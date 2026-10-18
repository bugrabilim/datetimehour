import SwiftUI
import MapKit
import WeatherKit

/// Dönen dünya (sitedeki küre gibi): Apple Haritalar uydu görüntüsüyle küre, gündüz/gece gölgesi, Güneş ve Ay'ın
/// tepe noktası, 15°'lik saat dilimi meridyenleri (0° kırmızı), öne çıkan şehirlerin yerel saati ve hava durumu
/// (bulut + derece). Kendiliğinden döner (bir tur 2 dk); dokununca durur; yakınlaşınca 31 şehir ve o bölgedeki
/// yerler (sitedeki places.json, Natural Earth) yerel saatleriyle görünür. Hareketi azalt tercihinde dönmez.
///
/// Harita UIKit'teki MKMapView ile çizilir: dönüş her ekran karesinde kameranın küçük bir adımla
/// kaydırılmasıdır (CADisplayLink). SwiftUI `Map` kamera geçişlerinde süreyi dikkate almıyor, sık konum
/// değişikliğinde de bütün haritayı yeniden kuruyordu; küre takılıyordu (TestFlight geri bildirimi).
struct GlobeView: View {
    /// Sitedeki GLOBE_CITIES (build.mjs)
    static let featured = ["istanbul", "london", "moscow", "dubai", "mumbai", "singapore", "beijing", "tokyo", "sydney",
                           "cairo", "johannesburg", "sao-paulo", "new-york", "los-angeles"]

    /// Tam ekran kopyası: kapatma düğmesi gösterilir
    var fullscreen = false
    /// Saat ekranındaki küçük kopya: dokunuş haritaya gitmez (dokununca Dünya saatleri açılır), düğme yok
    var interactive = true

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dismiss) private var dismiss
    @State private var spinning = true
    @State private var zoomed = false
    @State private var resetToken = 0
    @State private var showFullscreen = false

    var body: some View {
        GlobeMap(spinning: $spinning, zoomed: $zoomed, resetToken: resetToken, reduceMotion: reduceMotion,
                 interactive: interactive)
            .overlay(alignment: .topTrailing) {
                if interactive {
                    Button {
                        if fullscreen { dismiss() } else { showFullscreen = true }
                    } label: {
                        Image(systemName: fullscreen ? "xmark" : "arrow.up.left.and.arrow.down.right")
                            .font(.footnote.weight(.bold))
                            .frame(width: 32, height: 32)
                            .background(.black.opacity(0.55), in: Circle())
                            .foregroundStyle(.white)
                    }
                    .accessibilityLabel(L(fullscreen ? "client.exitFullscreen" : "client.fullscreen"))
                    .padding(fullscreen ? 16 : 10)
                }
            }
            .overlay(alignment: .bottomTrailing) {
                if interactive, zoomed || (!spinning && !reduceMotion) {
                    Button {
                        resetToken += 1
                        spinning = !reduceMotion
                    } label: {
                        Label(L("globe.reset"), systemImage: "globe")
                    }
                    .font(.footnote.weight(.semibold))
                    .buttonStyle(.borderedProminent)
                    .tint(.black.opacity(0.55))
                    .padding(10)
                }
            }
            .fullScreenCover(isPresented: $showFullscreen) {
                GlobeView(fullscreen: true)
                    .ignoresSafeArea()
                    .statusBarHidden()
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(L("globe.label"))
    }

    /// Gece yarımküresi iki parça (her biri 180°'lik; harita 360°'lik tek çokgeni yanlış çizebilir).
    static func nightPolygons(at date: Date) -> [[Pair]] {
        let sun = GlobeSky.sun(date)
        var decl = sun.lat
        if abs(decl) < 0.5 { decl = decl < 0 ? -0.5 : 0.5 }
        let r = Double.pi / 180
        func termLat(_ lon: Double) -> Double {
            atan(-cos((lon - sun.lon) * r) / tan(decl * r)) / r
        }
        // Kuzey yazında gece güney kutbunu içerir
        let pole = decl > 0 ? -89.0 : 89.0
        return [(-180.0, 0.0), (0.0, 180.0)].map { from, to in
            let edge = stride(from: from, through: to, by: 5).map { Pair(termLat($0), $0) }
            return edge + [Pair(pole, to), Pair(pole, from)]
        }
    }

    struct Pair: Hashable {
        let lat: Double
        let lon: Double
        init(_ lat: Double, _ lon: Double) { self.lat = lat; self.lon = lon }
    }
}

/// Güneş ve Ay'ın tepe noktası (enlem/boylam; sitedeki globe.js ile aynı formüller).
/// Güneş ~0,01°, Ay düşük duyarlıklı (~1°; yalnız küredeki işaret için).
enum GlobeSky {
    private static let rad = Double.pi / 180

    private static func days(_ date: Date) -> Double {
        date.timeIntervalSince1970 / 86400 + 2440587.5 - 2451545.0
    }

    private static func wrap(_ x: Double) -> Double {
        var v = (x + 180).truncatingRemainder(dividingBy: 360)
        if v < 0 { v += 360 }
        return v - 180
    }

    static func sun(_ date: Date) -> (lat: Double, lon: Double) {
        let d = days(date)
        let g = (357.529 + 0.98560028 * d) * rad, q = 280.459 + 0.98564736 * d
        let l = (q + 1.915 * sin(g) + 0.02 * sin(2 * g)) * rad, e = (23.439 - 0.00000036 * d) * rad
        let ra = atan2(cos(e) * sin(l), cos(l)) / rad, dec = asin(sin(e) * sin(l)) / rad
        let eqt = wrap(q - ra)
        var ms = (date.timeIntervalSince1970 * 1000).truncatingRemainder(dividingBy: 86_400_000)
        if ms < 0 { ms += 86_400_000 }
        let utcH = ms / 3_600_000
        return (dec, wrap(-15 * (utcH - 12) - eqt))
    }

    /// elong: Güneş'ten açısal uzaklık (0 yeni ay, 180 dolunay)
    static func moon(_ date: Date) -> (lat: Double, lon: Double, elong: Double) {
        let d = days(date)
        let l = 218.316 + 13.176396 * d, m = (134.963 + 13.064993 * d) * rad, f = (93.272 + 13.22935 * d) * rad
        let lam = (l + 6.289 * sin(m)) * rad, bet = 5.128 * sin(f) * rad, e = 23.439 * rad
        let ra = atan2(sin(lam) * cos(e) - tan(bet) * sin(e), cos(lam)) / rad
        let dec = asin(sin(bet) * cos(e) + cos(bet) * sin(e) * sin(lam)) / rad
        let gmst = 280.46061837 + 360.98564736629 * d
        let sunL = 280.459 + 0.98564736 * d + 1.915 * sin((357.529 + 0.98560028 * d) * rad)
        var elong = (lam / rad - sunL).truncatingRemainder(dividingBy: 360)
        if elong < 0 { elong += 360 }
        return (dec, wrap(ra - gmst), elong)
    }
}

/// Yakınlaşınca gösterilen dünya yerleri (sitedeki src/data/places.json: Natural Earth 1:10m, nüfusu ≥200 bin
/// ya da başkent; 31 şehre çok yakın olanlar atlanır)
struct GlobePlace {
    let name: String
    let zone: TimeZone
    let lat: Double
    let lon: Double
    let population: Double

    private struct File: Decodable {
        let tz: [String]
        let p: [[Double]]
        let names: [String: [String]]
    }

    static let all: [GlobePlace] = {
        guard let url = Bundle.main.url(forResource: "places", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let f = try? JSONDecoder().decode(File.self, from: data) else { return [] }
        let names = f.names[AppLocale.code] ?? f.names["en"] ?? []
        let zones = f.tz.map { TimeZone(identifier: $0) }
        let cities = City.all
        return f.p.enumerated().compactMap { i, row in
            guard row.count >= 4, i < names.count, let zone = zones[Int(row[2])] else { return nil }
            let lat = row[0], lon = row[1]
            if cities.contains(where: { abs($0.lat - lat) < 0.35 && abs($0.lon - lon) < 0.35 }) { return nil }
            return GlobePlace(name: names[i], zone: zone, lat: lat, lon: lon, population: row[3])
        }
    }()
}

/// Küredeki öne çıkan şehirlerin güncel havası (simge + sıcaklık). WeatherKit çağrı hakkı idareli kullanılır:
/// her şehir en çok saatte bir sorulur, sonuç cihazda saklanır (uygulama yeniden açılsa da).
@MainActor
final class GlobeWeather {
    static let shared = GlobeWeather()

    struct Entry: Codable {
        let at: Date
        let symbol: String
        let celsius: Double
    }

    private var cache: [String: Entry]
    private var loading = Set<String>()
    private static let key = "globeWx"
    private static let ttl: TimeInterval = 3600

    private init() {
        cache = UserDefaults.standard.data(forKey: Self.key)
            .flatMap { try? JSONDecoder().decode([String: Entry].self, from: $0) } ?? [:]
    }

    func cached(_ key: String) -> Entry? { cache[key] }

    /// Süresi geçenleri sırayla sorar; her yanıt gelince `done` çağrılır
    func refresh(_ cities: [City], done: @escaping @MainActor (String) -> Void) {
        let due = cities.filter { c in
            !loading.contains(c.key) && (cache[c.key].map { Date().timeIntervalSince($0.at) > Self.ttl } ?? true)
        }
        guard !due.isEmpty else { return }
        due.forEach { loading.insert($0.key) }
        Task {
            for c in due {
                defer { loading.remove(c.key) }
                guard let w = try? await WeatherService.shared.weather(
                    for: CLLocation(latitude: c.lat, longitude: c.lon), including: .current) else { continue }
                cache[c.key] = Entry(at: Date(), symbol: w.symbolName,
                                     celsius: w.temperature.converted(to: .celsius).value)
                if let data = try? JSONEncoder().encode(cache) { UserDefaults.standard.set(data, forKey: Self.key) }
                done(c.key)
            }
        }
    }
}

/// MKMapView sarmalayıcısı: kamera, katmanlar (meridyenler, gece), etiketler (şehirler, yerler, UTC, Güneş, Ay)
/// ve dönüş
private struct GlobeMap: UIViewRepresentable {
    @Binding var spinning: Bool
    @Binding var zoomed: Bool
    let resetToken: Int
    let reduceMotion: Bool
    let interactive: Bool

    static let farDistance: CLLocationDistance = 32_000_000
    static let centerLat = 20.0
    /// Derece/saniye: bir tur 2 dakika
    static let speed = 3.0

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.preferredConfiguration = MKImageryMapConfiguration(elevationStyle: .realistic)
        map.showsCompass = false
        map.isUserInteractionEnabled = interactive
        map.delegate = context.coordinator
        let c = context.coordinator
        c.map = map
        c.setCamera(animated: false)
        if interactive { c.installGestures() }
        c.refreshOverlays()
        c.refreshAnnotations(zoomed: false)
        c.refreshSky()
        c.loadWeather()
        c.startMinuteTimer()
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        let c = context.coordinator
        c.parent = self
        if resetToken != c.lastReset {
            c.lastReset = resetToken
            c.setCamera(animated: true)
        }
        c.setSpinning(spinning && !reduceMotion)
        c.refreshAnnotations(zoomed: zoomed)
    }

    static func dismantleUIView(_ map: MKMapView, coordinator: Coordinator) {
        coordinator.stop()
    }

    @MainActor
    final class Coordinator: NSObject, MKMapViewDelegate, UIGestureRecognizerDelegate {
        var parent: GlobeMap
        weak var map: MKMapView?
        var lastReset = 0
        private var lon = 30.0
        private var link: CADisplayLink?
        private var lastTime: CFTimeInterval = 0
        private var minuteTimer: Timer?
        private var shownZoomed: Bool?
        private let labelViews = NSHashTable<PlaceLabelView>.weakObjects()
        private var skyMarks: [SkyAnnotation] = []

        init(_ parent: GlobeMap) {
            self.parent = parent
        }

        // MARK: Kamera ve dönüş

        func setCamera(animated: Bool) {
            map?.setCamera(MKMapCamera(lookingAtCenter: CLLocationCoordinate2D(latitude: GlobeMap.centerLat, longitude: lon),
                                       fromDistance: GlobeMap.farDistance, pitch: 0, heading: 0),
                           animated: animated)
        }

        func setSpinning(_ on: Bool) {
            if on, link == nil {
                lastTime = 0
                let l = CADisplayLink(target: self, selector: #selector(tick(_:)))
                // Saat ekranındaki küçük küre için 30 kare yeterli (pil)
                let fps: Float = parent.interactive ? 60 : 30
                l.preferredFrameRateRange = CAFrameRateRange(minimum: 30, maximum: fps, preferred: fps)
                l.add(to: .main, forMode: .common)
                link = l
            } else if !on, let l = link {
                l.invalidate()
                link = nil
            }
        }

        @objc private func tick(_ l: CADisplayLink) {
            guard map != nil else { return }
            defer { lastTime = l.timestamp }
            guard lastTime > 0 else { return }
            lon += GlobeMap.speed * min(l.timestamp - lastTime, 0.1)
            if lon > 180 { lon -= 360 }
            setCamera(animated: false)
        }

        func stop() {
            link?.invalidate()
            link = nil
            minuteTimer?.invalidate()
            minuteTimer = nil
        }

        // MARK: Dokunuş: dönüş durur

        func installGestures() {
            guard let map else { return }
            for g in [UIPanGestureRecognizer(), UIPinchGestureRecognizer(), UITapGestureRecognizer()] as [UIGestureRecognizer] {
                g.addTarget(self, action: #selector(userTouched(_:)))
                g.cancelsTouchesInView = false
                g.delegate = self
                map.addGestureRecognizer(g)
            }
        }

        @objc private func userTouched(_ g: UIGestureRecognizer) {
            guard g.state == .began || g.state == .ended, parent.spinning else { return }
            parent.spinning = false
        }

        nonisolated func gestureRecognizer(_ g: UIGestureRecognizer,
                                           shouldRecognizeSimultaneouslyWith other: UIGestureRecognizer) -> Bool { true }

        nonisolated func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
            MainActor.assumeIsolated {
                guard link == nil else { return } // dönerken her karede çağrılır; bir şey değişmez
                lon = mapView.centerCoordinate.longitude
                let z = mapView.camera.centerCoordinateDistance < GlobeMap.farDistance * 0.6
                if z != parent.zoomed {
                    DispatchQueue.main.async { self.parent.zoomed = z }
                }
            }
        }

        // MARK: Katmanlar

        func refreshOverlays() {
            guard let map else { return }
            map.removeOverlays(map.overlays)
            for h in -11...12 {
                var coords = stride(from: -80.0, through: 80.0, by: 10).map {
                    CLLocationCoordinate2D(latitude: $0, longitude: Double(h) * 15)
                }
                let line = MKPolyline(coordinates: &coords, count: coords.count)
                line.title = h == 0 ? "utc0" : "meridian"
                map.addOverlay(line, level: .aboveLabels)
            }
            for ring in GlobeView.nightPolygons(at: Date()) {
                var coords = ring.map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lon) }
                map.addOverlay(MKPolygon(coordinates: &coords, count: coords.count), level: .aboveRoads)
            }
        }

        nonisolated func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let line = overlay as? MKPolyline {
                let r = MKPolylineRenderer(polyline: line)
                let zero = line.title == "utc0"
                r.strokeColor = zero ? .systemRed : UIColor.white.withAlphaComponent(0.45)
                r.lineWidth = zero ? 2 : 1
                return r
            }
            if let poly = overlay as? MKPolygon {
                let r = MKPolygonRenderer(polygon: poly)
                r.fillColor = UIColor.black.withAlphaComponent(0.38)
                return r
            }
            return MKOverlayRenderer(overlay: overlay)
        }

        // MARK: Etiketler

        /// Uzakta: öne çıkan şehirler + UTC farkları + Güneş ve Ay. Yakında: 31 şehir ve dünya yerleri
        /// (MapKit çakışanları önceliğe göre gizler: önce şehirler, sonra nüfusu büyük yerler).
        func refreshAnnotations(zoomed: Bool) {
            guard let map, shownZoomed != zoomed else { return }
            shownZoomed = zoomed
            map.removeAnnotations(map.annotations)
            let cities = zoomed ? City.all : City.all.filter { GlobeView.featured.contains($0.key) }
            map.addAnnotations(cities.map { PlaceAnnotation(city: $0) })
            if zoomed {
                map.addAnnotations(GlobePlace.all.map { PlaceAnnotation(place: $0) })
            } else {
                map.addAnnotations((-11...12).map(UTCAnnotation.init))
                map.addAnnotations(skyMarks)
            }
        }

        /// Güneş ve Ay işaretleri dakikada bir yeni konuma taşınır
        func refreshSky() {
            guard let map else { return }
            let now = Date()
            let sun = GlobeSky.sun(now), moon = GlobeSky.moon(now)
            let fresh = [SkyAnnotation(kind: .sun, lat: sun.lat, lon: sun.lon, elong: 0),
                         SkyAnnotation(kind: .moon, lat: moon.lat, lon: moon.lon, elong: moon.elong)]
            map.removeAnnotations(skyMarks)
            skyMarks = fresh
            if shownZoomed == false { map.addAnnotations(fresh) }
        }

        func loadWeather() {
            let featured = City.all.filter { GlobeView.featured.contains($0.key) }
            GlobeWeather.shared.refresh(featured) { [weak self] key in
                guard let self else { return }
                for v in self.labelViews.allObjects where v.key == key { v.refresh(at: Date()) }
            }
        }

        nonisolated func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            MainActor.assumeIsolated {
                if let a = annotation as? PlaceAnnotation {
                    let id = a.isCity ? "city" : "place"
                    let v = (mapView.dequeueReusableAnnotationView(withIdentifier: id) as? PlaceLabelView)
                        ?? PlaceLabelView(annotation: a, reuseIdentifier: id)
                    v.annotation = a
                    v.show(a, at: Date())
                    labelViews.add(v)
                    return v
                }
                if let a = annotation as? UTCAnnotation {
                    let v = (mapView.dequeueReusableAnnotationView(withIdentifier: "utc") as? UTCLabelView)
                        ?? UTCLabelView(annotation: a, reuseIdentifier: "utc")
                    v.annotation = a
                    v.show(a.hour)
                    return v
                }
                if let a = annotation as? SkyAnnotation {
                    let v = MKAnnotationView(annotation: a, reuseIdentifier: nil)
                    v.image = a.kind == .sun ? SkyAnnotation.sunImage : SkyAnnotation.moonImage(elong: a.elong)
                    v.displayPriority = .required
                    v.accessibilityLabel = L(a.kind == .sun ? "globe.sun" : "globe.moon")
                    return v
                }
                return nil
            }
        }

        /// Dakika başında şehir saatleri, gece gölgesi, Güneş ve Ay yenilenir; süresi geçen hava yeniden sorulur
        func startMinuteTimer() {
            let now = Date()
            let next = (now.timeIntervalSinceReferenceDate / 60).rounded(.down) * 60 + 60
            let t = Timer(fire: Date(timeIntervalSinceReferenceDate: next), interval: 60, repeats: true) { [weak self] _ in
                MainActor.assumeIsolated {
                    guard let self else { return }
                    for v in self.labelViews.allObjects { v.refresh(at: Date()) }
                    self.refreshOverlays()
                    self.refreshSky()
                    self.loadWeather()
                }
            }
            RunLoop.main.add(t, forMode: .common)
            minuteTimer = t
        }
    }
}

/// Şehir (cities.json) ya da dünya yeri (places.json)
private final class PlaceAnnotation: NSObject, MKAnnotation {
    let key: String
    let name: String
    let zone: TimeZone
    let isCity: Bool
    let priority: MKFeatureDisplayPriority
    let coordinate: CLLocationCoordinate2D

    /// Şehirler yerlerden önce gelir; çakışan şehirlerden önce öne çıkanlar (sırayla, İstanbul en başta), sonra
    /// diğerleri görünür. `.required` değil: MapKit üst üste binen etiketlerden düşük öncelikliyi gizler.
    init(city: City) {
        key = city.key
        name = city.displayName
        zone = city.zone
        isCity = true
        let rank = GlobeView.featured.firstIndex(of: city.key)
        priority = MKFeatureDisplayPriority(rawValue: rank.map { 990 - Float($0) * 5 } ?? 900)
        coordinate = CLLocationCoordinate2D(latitude: city.lat, longitude: city.lon)
    }

    /// Nüfus büyüdükçe öncelik artar (200 bin → ~360, 20 milyon → ~740); çakışınca küçük olan gizlenir
    init(place: GlobePlace) {
        key = ""
        name = place.name
        zone = place.zone
        isCity = false
        let v = log10(max(place.population, 100_000)) - 5
        priority = MKFeatureDisplayPriority(rawValue: Float(min(740, 300 + v * 190)))
        coordinate = CLLocationCoordinate2D(latitude: place.lat, longitude: place.lon)
    }
}

private final class UTCAnnotation: NSObject, MKAnnotation {
    let hour: Int
    var coordinate: CLLocationCoordinate2D { CLLocationCoordinate2D(latitude: -48, longitude: Double(hour) * 15) }
    init(_ hour: Int) { self.hour = hour }
}

/// Güneş ya da Ay'ın tepe noktası (o an başucunda olduğu yer)
private final class SkyAnnotation: NSObject, MKAnnotation {
    enum Kind { case sun, moon }
    let kind: Kind
    let elong: Double
    let coordinate: CLLocationCoordinate2D

    init(kind: Kind, lat: Double, lon: Double, elong: Double) {
        self.kind = kind
        self.elong = elong
        coordinate = CLLocationCoordinate2D(latitude: lat, longitude: lon)
    }

    static let sunImage: UIImage = UIGraphicsImageRenderer(size: CGSize(width: 44, height: 44)).image { ctx in
        let c = ctx.cgContext
        let colors = [UIColor(red: 1, green: 0.85, blue: 0.3, alpha: 0.75).cgColor,
                      UIColor(red: 1, green: 0.75, blue: 0.2, alpha: 0).cgColor] as CFArray
        if let g = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1]) {
            c.drawRadialGradient(g, startCenter: CGPoint(x: 22, y: 22), startRadius: 0,
                                 endCenter: CGPoint(x: 22, y: 22), endRadius: 22, options: [])
        }
        UIColor(red: 1, green: 0.84, blue: 0.25, alpha: 1).setFill()
        c.fillEllipse(in: CGRect(x: 13, y: 13, width: 18, height: 18))
        UIColor.white.withAlphaComponent(0.9).setStroke()
        c.setLineWidth(1.5)
        c.strokeEllipse(in: CGRect(x: 13, y: 13, width: 18, height: 18))
    }

    /// Evresine göre aydınlık bölümüyle Ay (kuzey yarımküreden görünüş: büyürken sağı aydınlık)
    static func moonImage(elong: Double) -> UIImage {
        UIGraphicsImageRenderer(size: CGSize(width: 40, height: 40)).image { ctx in
            let c = ctx.cgContext
            let center = CGPoint(x: 20, y: 20), r: CGFloat = 8
            let colors = [UIColor(red: 0.86, green: 0.89, blue: 0.94, alpha: 0.5).cgColor,
                          UIColor(red: 0.86, green: 0.89, blue: 0.94, alpha: 0).cgColor] as CFArray
            if let g = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1]) {
                c.drawRadialGradient(g, startCenter: center, startRadius: 0, endCenter: center, endRadius: 20, options: [])
            }
            UIColor(red: 0.23, green: 0.25, blue: 0.29, alpha: 1).setFill()
            c.fillEllipse(in: CGRect(x: center.x - r, y: center.y - r, width: 2 * r, height: 2 * r))
            // Aydınlık kısım: yarım daire + sonlandırıcı elips (UIKit'te y aşağı doğru)
            let k = CGFloat(cos(elong * .pi / 180)), waxing = elong < 180
            let lit = UIBezierPath()
            lit.addArc(withCenter: center, radius: r, startAngle: -.pi / 2, endAngle: .pi / 2, clockwise: waxing)
            let w = abs(k) * r
            // elipsin yarısı alttan üste: hilalde aydınlık yana (ince dilim kalır), şişkin evrede karanlık yana
            let sideSign: CGFloat = ((k > 0) == waxing) ? 1 : -1
            lit.addCurve(to: CGPoint(x: center.x, y: center.y - r),
                         controlPoint1: CGPoint(x: center.x + sideSign * w * 1.333, y: center.y + r),
                         controlPoint2: CGPoint(x: center.x + sideSign * w * 1.333, y: center.y - r))
            lit.close()
            UIColor(red: 0.93, green: 0.95, blue: 0.97, alpha: 1).setFill()
            lit.fill()
            UIColor.white.withAlphaComponent(0.7).setStroke()
            c.setLineWidth(1.2)
            c.strokeEllipse(in: CGRect(x: center.x - r, y: center.y - r, width: 2 * r, height: 2 * r))
        }
    }
}

/// Ad ve yerel saat (öne çıkan şehirlerde hava simgesi ve sıcaklık): yarı saydam koyu kutuda iki satır
private final class PlaceLabelView: MKAnnotationView {
    private let label = UILabel()
    private var item: PlaceAnnotation?
    var key: String { item?.key ?? "" }

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        backgroundColor = UIColor.black.withAlphaComponent(0.55)
        layer.cornerRadius = 5
        label.numberOfLines = 2
        addSubview(label)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) kullanılmaz") }

    func show(_ item: PlaceAnnotation, at date: Date) {
        self.item = item
        displayPriority = item.priority
        collisionMode = .rectangle
        refresh(at: date)
    }

    func refresh(at date: Date) {
        guard let item else { return }
        let big = item.isCity
        let p = NSMutableParagraphStyle()
        p.alignment = .center
        let text = NSMutableAttributedString(string: item.name + "\n",
                                             attributes: [.font: UIFont.systemFont(ofSize: big ? 10 : 9, weight: .medium),
                                                          .foregroundColor: UIColor.white, .paragraphStyle: p])
        text.append(NSAttributedString(string: Fmt.time(date, tz: item.zone),
                                       attributes: [.font: UIFont.monospacedDigitSystemFont(ofSize: big ? 13 : 11, weight: .bold),
                                                    .foregroundColor: UIColor.white, .paragraphStyle: p]))
        // Saatin hemen yanında hava simgesi (bulut, güneş…) ve derece. Simge renkli ve sabit çizilir: şablon
        // simge etikette görünmüyordu (TestFlight geri bildirimi: "dakika ile derece arasında boşluk var").
        if big, let w = GlobeWeather.shared.cached(item.key), Date().timeIntervalSince(w.at) < 6 * 3600 {
            let config = UIImage.SymbolConfiguration(pointSize: 11, weight: .semibold)
                .applying(UIImage.SymbolConfiguration(paletteColors: [.white, .systemYellow, .systemCyan]))
            if let img = UIImage(systemName: w.symbol, withConfiguration: config)?.withRenderingMode(.alwaysOriginal) {
                let att = NSTextAttachment(image: img)
                att.bounds = CGRect(x: 0, y: -2, width: img.size.width, height: img.size.height)
                text.append(NSAttributedString(string: " "))
                text.append(NSAttributedString(attachment: att))
            }
            text.append(NSAttributedString(string: Units.temp(Measurement(value: w.celsius, unit: .celsius)),
                                           attributes: [.font: UIFont.monospacedDigitSystemFont(ofSize: 12, weight: .semibold),
                                                        .foregroundColor: UIColor.white]))
        }
        label.attributedText = text
        let size = label.sizeThatFits(CGSize(width: 220, height: 60))
        frame.size = CGSize(width: size.width + 10, height: size.height + 4)
        label.frame = CGRect(x: 5, y: 2, width: size.width, height: size.height)
    }
}

/// Meridyen altındaki UTC farkı ("UTC", "+3")
private final class UTCLabelView: MKAnnotationView {
    private let label = UILabel()

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        label.font = .systemFont(ofSize: 10, weight: .semibold)
        label.layer.shadowColor = UIColor.black.cgColor
        label.layer.shadowRadius = 2
        label.layer.shadowOpacity = 0.8
        label.layer.shadowOffset = .zero
        addSubview(label)
        displayPriority = .required
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) kullanılmaz") }

    func show(_ hour: Int) {
        label.text = hour == 0 ? "UTC" : String(format: "%+d", hour)
        label.textColor = hour == 0 ? .systemRed : .white
        label.sizeToFit()
        frame.size = label.bounds.size
        label.frame.origin = .zero
    }
}
