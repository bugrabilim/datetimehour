import SwiftUI
import MapKit

/// Dönen dünya (sitedeki küre gibi): Apple Haritalar uydu görüntüsüyle küre, gündüz/gece gölgesi, 15°'lik
/// saat dilimi meridyenleri (0° kırmızı) ve şehirlerin yerel saatleri. Kendiliğinden döner (bir tur 2 dk);
/// dokununca durur, yakınlaşınca bütün şehirler görünür. Hareketi azalt tercihinde dönmez. Tam ekrana alınabilir.
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

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dismiss) private var dismiss
    @State private var spinning = true
    @State private var zoomed = false
    @State private var resetToken = 0
    @State private var showFullscreen = false

    var body: some View {
        GlobeMap(spinning: $spinning, zoomed: $zoomed, resetToken: resetToken, reduceMotion: reduceMotion)
            .overlay(alignment: .topTrailing) {
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
            .overlay(alignment: .bottomTrailing) {
                if zoomed || (!spinning && !reduceMotion) {
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
    /// Güneşin tepe noktası yaklaşık hesapla (gölge için yeterli).
    static func nightPolygons(at date: Date) -> [[Pair]] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "UTC")!
        let doy = Double(cal.ordinality(of: .day, in: .year, for: date) ?? 1)
        let c = cal.dateComponents([.hour, .minute], from: date)
        let utcHours = Double(c.hour ?? 0) + Double(c.minute ?? 0) / 60
        var decl = -23.44 * cos(2 * .pi / 365 * (doy + 10))
        if abs(decl) < 0.5 { decl = decl < 0 ? -0.5 : 0.5 }
        let sunLon = -15 * (utcHours - 12)
        let r = Double.pi / 180
        func termLat(_ lon: Double) -> Double {
            atan(-cos((lon - sunLon) * r) / tan(decl * r)) / r
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

/// MKMapView sarmalayıcısı: kamera, katmanlar (meridyenler, gece), etiketler (şehirler, UTC) ve dönüş
private struct GlobeMap: UIViewRepresentable {
    @Binding var spinning: Bool
    @Binding var zoomed: Bool
    let resetToken: Int
    let reduceMotion: Bool

    static let farDistance: CLLocationDistance = 32_000_000
    static let centerLat = 20.0
    /// Derece/saniye: bir tur 2 dakika
    static let speed = 3.0

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.preferredConfiguration = MKImageryMapConfiguration(elevationStyle: .realistic)
        map.showsCompass = false
        map.delegate = context.coordinator
        let c = context.coordinator
        c.map = map
        c.setCamera(animated: false)
        c.installGestures()
        c.refreshOverlays()
        c.refreshAnnotations(zoomed: false)
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
        private var cityViews: [String: CityLabelView] = [:]

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
                l.preferredFrameRateRange = CAFrameRateRange(minimum: 30, maximum: 60, preferred: 60)
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

        func refreshAnnotations(zoomed: Bool) {
            guard let map, shownZoomed != zoomed else { return }
            shownZoomed = zoomed
            map.removeAnnotations(map.annotations)
            cityViews = [:]
            let cities = zoomed ? City.all : City.all.filter { GlobeView.featured.contains($0.key) }
            map.addAnnotations(cities.map(CityAnnotation.init))
            if !zoomed {
                map.addAnnotations((-11...12).map(UTCAnnotation.init))
            }
        }

        nonisolated func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            MainActor.assumeIsolated {
                if let a = annotation as? CityAnnotation {
                    let v = (mapView.dequeueReusableAnnotationView(withIdentifier: "city") as? CityLabelView)
                        ?? CityLabelView(annotation: a, reuseIdentifier: "city")
                    v.annotation = a
                    v.show(a.city, at: Date())
                    cityViews[a.city.key] = v
                    return v
                }
                if let a = annotation as? UTCAnnotation {
                    let v = (mapView.dequeueReusableAnnotationView(withIdentifier: "utc") as? UTCLabelView)
                        ?? UTCLabelView(annotation: a, reuseIdentifier: "utc")
                    v.annotation = a
                    v.show(a.hour)
                    return v
                }
                return nil
            }
        }

        /// Dakika başında şehir saatleri ve gece gölgesi yenilenir
        func startMinuteTimer() {
            let now = Date()
            let next = (now.timeIntervalSinceReferenceDate / 60).rounded(.down) * 60 + 60
            let t = Timer(fire: Date(timeIntervalSinceReferenceDate: next), interval: 60, repeats: true) { [weak self] _ in
                MainActor.assumeIsolated {
                    guard let self else { return }
                    for v in self.cityViews.values { v.refresh(at: Date()) }
                    self.refreshOverlays()
                }
            }
            RunLoop.main.add(t, forMode: .common)
            minuteTimer = t
        }
    }
}

private final class CityAnnotation: NSObject, MKAnnotation {
    let city: City
    var coordinate: CLLocationCoordinate2D { CLLocationCoordinate2D(latitude: city.lat, longitude: city.lon) }
    init(_ city: City) { self.city = city }
}

private final class UTCAnnotation: NSObject, MKAnnotation {
    let hour: Int
    var coordinate: CLLocationCoordinate2D { CLLocationCoordinate2D(latitude: -48, longitude: Double(hour) * 15) }
    init(_ hour: Int) { self.hour = hour }
}

/// Şehir adı ve yerel saat: yarı saydam koyu kutuda iki satır
private final class CityLabelView: MKAnnotationView {
    private let label = UILabel()
    private var city: City?

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        backgroundColor = UIColor.black.withAlphaComponent(0.55)
        layer.cornerRadius = 5
        label.numberOfLines = 2
        addSubview(label)
        displayPriority = .required
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) kullanılmaz") }

    func show(_ city: City, at date: Date) {
        self.city = city
        refresh(at: date)
    }

    func refresh(at date: Date) {
        guard let city else { return }
        let p = NSMutableParagraphStyle()
        p.alignment = .center
        let text = NSMutableAttributedString(string: city.displayName + "\n",
                                             attributes: [.font: UIFont.systemFont(ofSize: 10, weight: .medium),
                                                          .foregroundColor: UIColor.white, .paragraphStyle: p])
        text.append(NSAttributedString(string: Fmt.time(date, tz: city.zone),
                                       attributes: [.font: UIFont.monospacedDigitSystemFont(ofSize: 13, weight: .bold),
                                                    .foregroundColor: UIColor.white, .paragraphStyle: p]))
        label.attributedText = text
        let size = label.sizeThatFits(CGSize(width: 200, height: 60))
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
