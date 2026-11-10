import SwiftUI
import MapKit
import CoreLocation
import SaatCore

/// Telefonun pusulası (30 Ekim 2026, rakip analizi #8). Konum izni varsa gerçek kuzey (trueHeading; iOS bunun için
/// konum güncellemesi ister, kilometre doğruluğu yeter), yoksa manyetik kuzey. Konum hiçbir yere gönderilmez.
@MainActor
final class Compass: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published private(set) var heading: Double?
    @Published private(set) var accuracy: Double = -1
    @Published private(set) var isTrueNorth = false
    let available = CLLocationManager.headingAvailable()
    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        manager.headingFilter = 1
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    func start() {
        guard available else { return }
        let status = manager.authorizationStatus
        if status == .authorizedWhenInUse || status == .authorizedAlways { manager.startUpdatingLocation() }
        manager.startUpdatingHeading()
    }

    func stop() {
        manager.stopUpdatingHeading()
        manager.stopUpdatingLocation()
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        let trueH = newHeading.trueHeading, magnetic = newHeading.magneticHeading, acc = newHeading.headingAccuracy
        MainActor.assumeIsolated {
            isTrueNorth = trueH >= 0
            heading = trueH >= 0 ? trueH : magnetic
            accuracy = acc
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {}
    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {}
    nonisolated func locationManagerShouldDisplayHeadingCalibration(_ manager: CLLocationManager) -> Bool { true }
}

/// Bir ondalıklı sayı, uygulama dilinde (Arapçada da Latin rakamı)
private func oneDecimal(_ x: Double) -> String {
    let f = NumberFormatter()
    f.locale = AppLocale.numberLocale
    f.minimumFractionDigits = 1
    f.maximumFractionDigits = 1
    return f.string(from: NSNumber(value: x)) ?? String(format: "%.1f", x)
}

/// Derece, uygulama dilinde bir ondalıkla ("151,6°")
func degrees(_ x: Double) -> String { oneDecimal(x) + "°" }

/// Uzaklık: 1 km'den azsa metre, 10 km'den azsa bir ondalıklı, yoksa tam kilometre (binlik ayraçlı)
func distanceText(_ km: Double) -> String {
    if km < 1 { return T("qibla.m", num(Int((km * 1000).rounded()))) }
    if km < 10 { return T("qibla.km", oneDecimal(km)) }
    return T("qibla.km", num(Int(km.rounded())))
}

/// Kıble pusulası: seçili yere göre kıble açısı, Kâbe'ye uzaklık, bugünün kıble saati; altta yakındaki camiler
struct QiblaView: View {
    @StateObject private var compass = Compass()
    @State private var aligned = false
    @AppStorage(QiblaStyle.key) private var styleRaw = QiblaStyle.classic.rawValue

    var body: some View {
        let place = PlaceModel.shared.place
        let q = Qibla.direction(latitude: place.lat, longitude: place.lon)
        let atKaaba = q.km < 1
        ScrollView {
            VStack(spacing: 20) {
                PlacePicker()
                if atKaaba {
                    Text(L("qibla.atKaaba"))
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Theme.accent)
                        .padding(.vertical, 40)
                } else {
                    dial(bearing: q.bearing)
                    status(bearing: q.bearing)
                    QiblaStylePicker()
                }
                facts(place: place, q: q, atKaaba: atKaaba)
                if !atKaaba {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(L("qibla.map")).font(.headline).foregroundStyle(Theme.text)
                        QiblaRouteMap(place: place)
                        Text(L("qibla.mapNoteIos")).font(.footnote).foregroundStyle(Theme.muted)
                    }
                    .card()
                    if compass.available {
                        NavigationLink {
                            QiblaCameraView(bearing: q.bearing)
                        } label: {
                            HStack {
                                Label(L("qibla.camera"), systemImage: "camera.viewfinder").foregroundStyle(Theme.text)
                                Spacer()
                                Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(Theme.muted)
                            }
                            .card()
                        }
                    }
                }
                NavigationLink {
                    NearbyMosquesView(place: place)
                } label: {
                    HStack {
                        Label(L("qibla.mosques"), systemImage: "building.columns")
                            .foregroundStyle(Theme.text)
                        Spacer()
                        Image(systemName: "chevron.forward")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(Theme.muted)
                    }
                    .card()
                }
                Text(L("qibla.method"))
                    .font(.footnote)
                    .foregroundStyle(Theme.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(20)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("qibla.title"))
        .navigationBarTitleDisplayMode(.large)
        .onAppear { compass.start() }
        .onDisappear { compass.stop() }
        .onChange(of: compass.heading) { _, h in
            guard let h else { return }
            aligned = abs(angleDiff(q.bearing, h)) < 3
        }
        .sensoryFeedback(.success, trigger: aligned) { _, now in now }
    }

    /// İki yön arasındaki işaretli fark (−180…180)
    private func angleDiff(_ a: Double, _ b: Double) -> Double {
        (a - b + 540).truncatingRemainder(dividingBy: 360) - 180
    }

    /// Kadran telefonun yönüne göre döner (kuzey her zaman gerçek kuzeyi gösterir); Kâbe işareti kıble açısında durur.
    /// Ortadaki ok telefonun üstüne göre kıbleyi gösterir. Pusula yoksa kuzey yukarıda sabit kadran.
    private func dial(bearing: Double) -> some View {
        let heading = compass.heading ?? 0
        let look = QiblaStyle(rawValue: styleRaw) ?? .classic
        let color = aligned ? Theme.accent : look.needle
        return ZStack {
            Circle().fill(look.face)
            Circle().stroke(look.ring, lineWidth: look == .gold ? 3 : 1)
            // Kadranın zemininde soluk hatem (doğal tasarım madde 15; %6)
            HatemMotif(color: look.ring, opacity: 0.06).padding(30).rotationEffect(.degrees(-heading))
            ZStack {
                ForEach(0..<72) { i in
                    Capsule()
                        .fill(i % 18 == 0 ? look.tick : look.faceInk.opacity(i % 6 == 0 ? 0.8 : 0.4))
                        .frame(width: i % 6 == 0 ? 2 : 1, height: i % 6 == 0 ? 12 : 6)
                        .offset(y: -128)
                        .rotationEffect(.degrees(Double(i) * 5))
                }
                ForEach(Array(L("qibla.cardinals").split(separator: " ").enumerated()), id: \.offset) { i, s in
                    Text(String(s))
                        .font(.headline.weight(i == 0 ? .bold : .semibold))
                        .foregroundStyle(i == 0 ? look.north : look.faceInk)
                        .rotationEffect(.degrees(heading - Double(i) * 90))
                        .offset(y: -104)
                        .rotationEffect(.degrees(Double(i) * 90))
                }
                // Derece rakamları (4 Kasım 2026, sahibin isteği): yön harfleri dışındaki her 30°'de küçük, Latin rakamla
                ForEach([30, 60, 120, 150, 210, 240, 300, 330], id: \.self) { d in
                    Text(verbatim: String(d))
                        .font(.system(size: 10, weight: .medium).monospacedDigit())
                        .foregroundStyle(look.faceInk.opacity(0.75))
                        .rotationEffect(.degrees(heading - Double(d)))
                        .offset(y: -108)
                        .rotationEffect(.degrees(Double(d)))
                }
                Text("🕋")
                    .font(.system(size: 30))
                    .rotationEffect(.degrees(heading - bearing))
                    .offset(y: -150)
                    .rotationEffect(.degrees(bearing))
            }
            .rotationEffect(.degrees(-heading))
            .animation(.easeOut(duration: 0.25), value: heading)
            Image(systemName: "location.north.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 54, height: 120)
                .foregroundStyle(color)
                .rotationEffect(.degrees(bearing - heading))
                .animation(.easeOut(duration: 0.25), value: heading)
            Circle().fill(Theme.surface2).frame(width: 18, height: 18)
        }
        .frame(width: 280, height: 280)
        .padding(.vertical, 24)
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(L("qibla.angle") + " " + degrees(bearing))
    }

    @ViewBuilder private func status(bearing: Double) -> some View {
        if !compass.available {
            Text(L("qibla.noCompass")).font(.subheadline).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
        } else {
            VStack(spacing: 6) {
                Text(aligned ? L("qibla.aligned") : L("qibla.turn"))
                    .font(.headline)
                    .foregroundStyle(aligned ? Theme.accent : Theme.text)
                    .multilineTextAlignment(.center)
                if compass.accuracy < 0 || compass.accuracy > 20 {
                    Text(L("qibla.calibrate")).font(.footnote).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
                }
                if compass.heading != nil && !compass.isTrueNorth {
                    Text(L("qibla.magnetic")).font(.footnote).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
                }
            }
        }
    }

    private func facts(place: AppPlace, q: (bearing: Double, km: Double), atKaaba: Bool) -> some View {
        let today = place.today
        let noon = today.noon(in: place.zone)
        let tz = Double(place.zone.secondsFromGMT(for: noon)) / 3600
        let t = Qibla.time(year: today.year, month: today.month, day: today.day, latitude: place.lat, longitude: place.lon, utcOffset: tz)
        return VStack(spacing: 0) {
            if !atKaaba {
                row(L("qibla.angle"), degrees(q.bearing), note: L("qibla.angleNote"))
                Divider()
            }
            row(L("qibla.distance"), distanceText(q.km))
            if !atKaaba {
                Divider()
                if let t {
                    // Namaz vakitleri gibi dakikaya yuvarlanır (Diyanet'in kıble saatiyle aynı)
                    row(L("qibla.time"), place.hm(place.startOfDay(today).addingTimeInterval((t * 60).rounded() * 60)), note: L("qibla.timeNote"))
                } else {
                    row(L("qibla.time"), "–", note: L("qibla.noTime"))
                }
                // Rashdul Qiblat (10 Kasım 2026): Güneş'in Kâbe'nin üstünden geçtiği sıradaki an, yerin saatiyle
                let rq = rashdul(place: place)
                Divider()
                row(L("qibla.rashdul"), Fmt.shortDateTime(rq.at, tz: place.zone), note: L(rq.sunUp ? "qibla.rashdulNote" : "qibla.rashdulNight"))
            }
        }
        .card()
    }

    /// Sıradaki Rashdul Qiblat anı (dakikaya yuvarlı; bir saat sonrasına kadar gösterilir) ve Güneş o anda yerde ufkun üstünde mi
    private func rashdul(place: AppPlace) -> (at: Date, sunUp: Bool) {
        let now = Date(), y = place.today.year
        let all = (Qibla.rashdul(year: y) + Qibla.rashdul(year: y + 1)).map { Date(timeIntervalSince1970: ($0 / 60000).rounded() * 60) }
        let at = all.first { $0 > now.addingTimeInterval(-3600) } ?? all[all.count - 1]
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        let c = utc.dateComponents([.year, .month, .day, .hour, .minute], from: at)
        let sun = Qibla.sun(year: c.year ?? y, month: c.month ?? 1, day: c.day ?? 1, ut: Double(c.hour ?? 0) + Double(c.minute ?? 0) / 60,
                            latitude: place.lat, longitude: place.lon)
        return (at, sun.alt > 0)
    }

    private func row(_ title: String, _ value: String, note: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title).foregroundStyle(Theme.text)
                Spacer()
                Text(value)
                    .font(.body.weight(.semibold).monospacedDigit())
                    .foregroundStyle(Theme.accent)
                    .environment(\.layoutDirection, .leftToRight)
            }
            if let note {
                Text(note).font(.footnote).foregroundStyle(Theme.muted)
            }
        }
        .padding(.vertical, 10)
    }
}

/// Yakındaki camiler: Apple Haritalar araması (uygulamanın dilinde "cami"), harita ve uzaklığa göre liste.
/// Konum izni varsa telefonun konumu (bu ekranı açmak açık bir istektir, izin burada sorulabilir), yoksa seçili yer.
struct NearbyMosquesView: View {
    let place: AppPlace

    struct Mosque: Identifiable {
        let id = UUID()
        let item: MKMapItem
        let meters: Double
        var name: String { item.name ?? L("qibla.mosqueUnnamed") }
    }

    @State private var mosques: [Mosque] = []
    @State private var center: CLLocationCoordinate2D?
    @State private var usedPlace = false
    @State private var loading = true
    @State private var failed = false
    @State private var camera: MapCameraPosition = .automatic
    @State private var favs = QiblaFavs.load()
    /// Harita tam ekran (4 Kasım 2026, sahibin isteği: büyütülebilsin)
    @State private var bigMap = false
    /// Not düzenlenen favori ve yazılan metin
    @State private var noteFav: QiblaFavs.Fav?
    @State private var noteText = ""

    var body: some View {
        List {
            if !favs.isEmpty {
                Section {
                    ForEach(favs, id: \.self) { f in
                        HStack {
                            Button { QiblaFavs.open(f) } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(f.n).foregroundStyle(Theme.text)
                                        if let t = f.t {
                                            Text(t).font(.caption).foregroundStyle(Theme.muted).lineLimit(3)
                                        }
                                    }
                                    Spacer()
                                    Text(distanceText(CLLocation(latitude: place.lat, longitude: place.lon)
                                        .distance(from: CLLocation(latitude: f.lat, longitude: f.lon)) / 1000))
                                        .font(.subheadline.monospacedDigit()).foregroundStyle(Theme.muted)
                                }
                            }
                            .buttonStyle(.borderless)
                            Button {
                                noteText = f.t ?? ""
                                noteFav = f
                            } label: {
                                Image(systemName: f.t == nil ? "square.and.pencil" : "note.text").foregroundStyle(Theme.accent)
                            }
                            .buttonStyle(.borderless)
                            .accessibilityLabel(L("qibla.noteAdd"))
                            starButton(name: f.n, lat: f.lat, lon: f.lon)
                        }
                    }
                } header: {
                    Text(L("qibla.favs"))
                } footer: {
                    Text(L("qibla.noteNote"))
                }
            }
            if let center {
                Section {
                    mosqueMap(center)
                        .frame(height: 260)
                        .overlay(alignment: .topTrailing) {
                            Button { bigMap = true } label: {
                                Image(systemName: "arrow.up.left.and.arrow.down.right")
                                    .font(.subheadline.weight(.semibold))
                                    .padding(9)
                                    .background(.regularMaterial, in: Circle())
                            }
                            .padding(10)
                            .accessibilityLabel(L("qibla.mapLarge"))
                        }
                        .listRowInsets(EdgeInsets())
                } footer: {
                    if usedPlace { Text(T("qibla.mosquesNear", place.name)) }
                }
            }
            Section {
                if loading {
                    HStack { Spacer(); ProgressView(); Spacer() }
                } else if failed {
                    Text(L("qibla.mosquesError")).foregroundStyle(Theme.muted)
                    Button(L("weather.retry")) { Task { await load() } }
                } else if mosques.isEmpty {
                    Text(L("qibla.mosquesNone")).foregroundStyle(Theme.muted)
                } else {
                    ForEach(mosques) { m in
                        HStack {
                            Button {
                                m.item.openInMaps()
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(m.name).foregroundStyle(Theme.text)
                                        if let street = m.item.placemark.thoroughfare {
                                            Text(street).font(.caption).foregroundStyle(Theme.muted)
                                        }
                                    }
                                    Spacer()
                                    Text(distanceText(m.meters / 1000))
                                        .font(.subheadline.monospacedDigit())
                                        .foregroundStyle(Theme.muted)
                                }
                            }
                            .buttonStyle(.borderless)
                            starButton(name: m.name, lat: m.item.placemark.coordinate.latitude, lon: m.item.placemark.coordinate.longitude)
                        }
                    }
                }
            } footer: {
                Text(L("qibla.mosquesNote"))
            }
        }
        .themedBackground()
        .navigationTitle(L("qibla.mosques"))
        .navigationBarTitleDisplayMode(.large)
        .task { await load() }
        .fullScreenCover(isPresented: $bigMap) {
            if let center {
                mosqueMap(center)
                    .ignoresSafeArea(edges: .bottom)
                    .overlay(alignment: .topTrailing) {
                        Button { bigMap = false } label: {
                            Image(systemName: "xmark")
                                .font(.headline)
                                .padding(12)
                                .background(.regularMaterial, in: Circle())
                        }
                        .padding(16)
                        .accessibilityLabel(L("common.close"))
                    }
            }
        }
        .alert(L("qibla.noteAdd"), isPresented: Binding(get: { noteFav != nil }, set: { if !$0 { noteFav = nil } })) {
            TextField(L("qibla.note"), text: $noteText, axis: .vertical)
            Button(L("common.save")) {
                if let f = noteFav { QiblaFavs.setNote(f, noteText) }
                favs = QiblaFavs.load()
                noteFav = nil
            }
            Button(L("common.cancel"), role: .cancel) { noteFav = nil }
        } message: {
            Text(noteFav?.n ?? "")
        }
    }

    /// Camiler haritası: liste içinde 260 nokta, tam ekranda bütün ekran; yakınlaştırma ve kaydırma açık
    private func mosqueMap(_ center: CLLocationCoordinate2D) -> some View {
        Map(position: $camera) {
            UserAnnotation()
            if usedPlace { Marker(place.name, systemImage: "mappin", coordinate: center).tint(.gray) }
            ForEach(mosques) { m in
                Marker(m.name, systemImage: "building.columns.fill", coordinate: m.item.placemark.coordinate)
                    .tint(Theme.accent)
            }
            ForEach(favs, id: \.self) { f in
                Marker(f.n, systemImage: "star.fill", coordinate: CLLocationCoordinate2D(latitude: f.lat, longitude: f.lon))
                    .tint(.yellow)
            }
        }
        .mapControls { MapCompass(); MapScaleView() }
    }

    private func starButton(name: String, lat: Double, lon: Double) -> some View {
        let on = favs.contains { abs($0.lat - lat) < 1e-5 && abs($0.lon - lon) < 1e-5 }
        return Button {
            QiblaFavs.toggle(name: name, lat: lat, lon: lon)
            favs = QiblaFavs.load()
        } label: {
            Image(systemName: on ? "star.fill" : "star").foregroundStyle(on ? Color.yellow : Theme.muted)
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(on ? L("qibla.favRemove") : L("qibla.favAdd"))
    }

    private func load() async {
        loading = true
        failed = false
        var coord = CLLocationCoordinate2D(latitude: place.lat, longitude: place.lon)
        if await PermissionPrimer.location(), let loc = await OneShotLocation().fetch(requestPermission: true) {
            coord = loc.coordinate
            usedPlace = false
        } else {
            usedPlace = true
        }
        center = coord
        let request = MKLocalSearch.Request()
        request.naturalLanguageQuery = L("qibla.mosqueQuery")
        request.resultTypes = .pointOfInterest
        // Apple'a giden bölge merkezi iki ondalığa yuvarlanır (~1 km); uzaklıklar telefonda tam konumla hesaplanır
        let rounded = CLLocationCoordinate2D(latitude: (coord.latitude * 100).rounded() / 100, longitude: (coord.longitude * 100).rounded() / 100)
        request.region = MKCoordinateRegion(center: rounded, latitudinalMeters: 6000, longitudinalMeters: 6000)
        do {
            let response = try await MKLocalSearch(request: request).start()
            let here = CLLocation(latitude: coord.latitude, longitude: coord.longitude)
            mosques = response.mapItems.map { item in
                let c = item.placemark.coordinate
                return Mosque(item: item, meters: here.distance(from: CLLocation(latitude: c.latitude, longitude: c.longitude)))
            }
            .sorted { $0.meters < $1.meters }
            .prefix(25)
            .map { $0 }
            camera = .region(MKCoordinateRegion(center: coord, latitudinalMeters: 3000, longitudinalMeters: 3000))
        } catch {
            // Sonuç yoksa MapKit hata verir (MKError.placemarkNotFound): boş liste
            if (error as? MKError)?.code == .placemarkNotFound { mosques = [] } else { failed = true }
        }
        loading = false
    }
}
