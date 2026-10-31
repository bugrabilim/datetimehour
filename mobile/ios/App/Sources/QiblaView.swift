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
                }
                facts(place: place, q: q, atKaaba: atKaaba)
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
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(L("qibla.title"))
        .navigationBarTitleDisplayMode(.inline)
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
        let color = aligned ? Theme.accent : Theme.ink
        return ZStack {
            Circle().fill(Theme.surface)
            Circle().stroke(Theme.border, lineWidth: 1)
            ZStack {
                ForEach(0..<72) { i in
                    Capsule()
                        .fill(i % 18 == 0 ? Theme.ink : Theme.muted.opacity(i % 6 == 0 ? 0.8 : 0.4))
                        .frame(width: i % 6 == 0 ? 2 : 1, height: i % 6 == 0 ? 12 : 6)
                        .offset(y: -128)
                        .rotationEffect(.degrees(Double(i) * 5))
                }
                ForEach(Array(L("qibla.cardinals").split(separator: " ").enumerated()), id: \.offset) { i, s in
                    Text(String(s))
                        .font(.headline.weight(i == 0 ? .bold : .semibold))
                        .foregroundStyle(i == 0 ? Color.red : Theme.muted)
                        .rotationEffect(.degrees(heading - Double(i) * 90))
                        .offset(y: -104)
                        .rotationEffect(.degrees(Double(i) * 90))
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
            }
        }
        .card()
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

    var body: some View {
        List {
            if let center {
                Section {
                    Map(position: $camera) {
                        UserAnnotation()
                        if usedPlace { Marker(place.name, systemImage: "mappin", coordinate: center).tint(.gray) }
                        ForEach(mosques) { m in
                            Marker(m.name, systemImage: "building.columns.fill", coordinate: m.item.placemark.coordinate)
                                .tint(Theme.accent)
                        }
                    }
                    .frame(height: 260)
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
                    }
                }
            } footer: {
                Text(L("qibla.mosquesNote"))
            }
        }
        .themedBackground()
        .navigationTitle(L("qibla.mosques"))
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        loading = true
        failed = false
        var coord = CLLocationCoordinate2D(latitude: place.lat, longitude: place.lon)
        if let loc = await OneShotLocation().fetch(requestPermission: true) {
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
