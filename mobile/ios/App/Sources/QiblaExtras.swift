import AVFoundation
import MapKit
import SwiftUI

/// Kıble ekleri (2. analiz #67; 4 Kasım 2026): pusula görünümü seçimi (5 tasarım, `qibla.style`; Android'deki
/// QiblaPrefs ile aynı kimlikler), haritada Kâbe'ye büyük daire çizgisi (Apple Haritalar, MKGeodesicPolyline),
/// favori camiler (`qibla.favs`, yalnız telefonda) ve kamera görünümü (görüntü telefonda kalır, kaydedilmez).
enum QiblaStyle: String, CaseIterable {
    case classic, minimal, gold, night, green

    static let key = "qibla.style"
    static let kaaba = CLLocationCoordinate2D(latitude: 21.422487, longitude: 39.826206)

    static var current: QiblaStyle { QiblaStyle(rawValue: UserDefaults.standard.string(forKey: key) ?? "") ?? .classic }

    var label: String { L("qibla.style." + rawValue) }

    private static func rgb(_ v: Int) -> Color {
        Color(red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255)
    }

    var face: Color {
        switch self {
        case .classic: return Theme.surface
        case .minimal: return .clear
        case .gold: return Self.rgb(0x1F1A10)
        case .night: return Self.rgb(0x0B1426)
        case .green: return Self.rgb(0x0F3B2E)
        }
    }

    var ring: Color {
        switch self {
        case .classic, .minimal: return Theme.border
        case .gold: return Self.rgb(0xC9A54A)
        case .night: return Self.rgb(0x2B3B5E)
        case .green: return Self.rgb(0x3C8D6E)
        }
    }

    var tick: Color {
        switch self {
        case .classic: return Theme.ink
        case .minimal: return Theme.muted
        case .gold: return Self.rgb(0xC9A54A)
        case .night: return Self.rgb(0x7D8DB3)
        case .green: return Self.rgb(0xA9D8C2)
        }
    }

    var faceInk: Color {
        switch self {
        case .classic, .minimal: return Theme.muted
        case .gold: return Self.rgb(0xE8D9AE)
        case .night: return Self.rgb(0xB9C4E2)
        case .green: return Self.rgb(0xD8F0E4)
        }
    }

    var needle: Color {
        switch self {
        case .classic, .minimal: return Theme.ink
        case .gold: return Self.rgb(0xE8C766)
        case .night: return Self.rgb(0xE6ECFF)
        case .green: return .white
        }
    }

    var north: Color {
        switch self {
        case .classic: return .red
        case .minimal: return Theme.ink
        case .gold: return Self.rgb(0xE8C766)
        case .night: return Self.rgb(0xFF6B6B)
        case .green: return Self.rgb(0xFFD27A)
        }
    }
}

/// Beş kadran örneği
struct QiblaStylePicker: View {
    @AppStorage(QiblaStyle.key) private var style = QiblaStyle.classic.rawValue

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(QiblaStyle.allCases, id: \.rawValue) { s in
                    let on = style == s.rawValue
                    Button { style = s.rawValue } label: {
                        VStack(spacing: 4) {
                            ZStack {
                                Circle().fill(s == .minimal ? Theme.bg : s.face)
                                Circle().stroke(on ? Theme.accent : s.ring, lineWidth: on ? 3 : 1)
                                Image(systemName: "location.north.fill").foregroundStyle(s.needle)
                                Circle().fill(s.north).frame(width: 5, height: 5).offset(y: 14)
                            }
                            .frame(width: 50, height: 50)
                            Text(s.label).font(.caption).foregroundStyle(on ? Theme.accent : Theme.muted)
                        }
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(on ? .isSelected : [])
                }
            }
            .padding(.horizontal, 2)
        }
    }
}

/// Haritada buradan Kâbe'ye en kısa yol
struct QiblaRouteMap: View {
    let place: AppPlace

    var body: some View {
        let here = CLLocationCoordinate2D(latitude: place.lat, longitude: place.lon)
        let coords = [here, QiblaStyle.kaaba]
        let line = MKGeodesicPolyline(coordinates: coords, count: coords.count)
        let rect = line.boundingMapRect.insetBy(dx: -line.boundingMapRect.width * 0.25 - 200_000, dy: -line.boundingMapRect.height * 0.25 - 200_000)
        Map(initialPosition: .rect(rect), interactionModes: [.pan, .zoom]) {
            MapPolyline(line).stroke(Theme.accent, lineWidth: 4)
            Marker(place.name, systemImage: "mappin", coordinate: here).tint(Theme.accent)
            Annotation(L("qibla.kaabaName"), coordinate: QiblaStyle.kaaba) { Text("🕋").font(.title) }
        }
        .mapStyle(.hybrid(elevation: .flat))
        .frame(height: 240)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

/// Favori camiler (ad + koordinat + isteğe bağlı not, en çok 30). Not yalnız bu telefonda kalır; başka kullanıcılarla
/// paylaşımı sunucu ister, ücretli sistemle birlikte ele alınacak (4 Kasım 2026, sahibin kararı)
enum QiblaFavs {
    static let key = "qibla.favs"

    struct Fav: Codable, Hashable {
        let n: String
        let lat: Double
        let lon: Double
        /// Kişisel not (eski kayıtlarda yok)
        var t: String? = nil
    }

    private static func save(_ list: [Fav]) {
        if let data = try? JSONEncoder().encode(Array(list.prefix(30))) {
            UserDefaults.standard.set(String(data: data, encoding: .utf8), forKey: key)
        }
    }

    static func setNote(_ f: Fav, _ note: String) {
        var list = load()
        guard let i = list.firstIndex(where: { abs($0.lat - f.lat) < 1e-5 && abs($0.lon - f.lon) < 1e-5 }) else { return }
        let t = note.trimmingCharacters(in: .whitespacesAndNewlines)
        list[i].t = t.isEmpty ? nil : String(t.prefix(500))
        save(list)
    }

    static func load() -> [Fav] {
        guard let data = UserDefaults.standard.string(forKey: key)?.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([Fav].self, from: data)) ?? []
    }

    static func contains(lat: Double, lon: Double) -> Bool {
        load().contains { abs($0.lat - lat) < 1e-5 && abs($0.lon - lon) < 1e-5 }
    }

    static func toggle(name: String, lat: Double, lon: Double) {
        var list = load()
        if let i = list.firstIndex(where: { abs($0.lat - lat) < 1e-5 && abs($0.lon - lon) < 1e-5 }) {
            list.remove(at: i)
        } else {
            list.insert(Fav(n: name, lat: lat, lon: lon), at: 0)
        }
        save(list)
    }

    static func open(_ f: Fav) {
        let item = MKMapItem(placemark: MKPlacemark(coordinate: CLLocationCoordinate2D(latitude: f.lat, longitude: f.lon)))
        item.name = f.n
        item.openInMaps()
    }
}

/// Kamera görünümü: önde kamera, üstte kıble oku; Kâbe işareti açı farkına göre yatayda kayar
struct QiblaCameraView: View {
    let bearing: Double
    @StateObject private var compass = Compass()
    @State private var allowed: Bool?

    private func diff(_ a: Double, _ b: Double) -> Double {
        (a - b + 540).truncatingRemainder(dividingBy: 360) - 180
    }

    var body: some View {
        ZStack {
            if allowed == true {
                CameraPreview().ignoresSafeArea()
            } else {
                Color.black.ignoresSafeArea()
            }
            if allowed == false {
                Text(L("qibla.cameraDenied")).foregroundStyle(.white).padding()
            } else {
                overlay
            }
        }
        .navigationTitle(L("qibla.camera"))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized: allowed = true
            case .notDetermined: allowed = await AVCaptureDevice.requestAccess(for: .video)
            default: allowed = false
            }
        }
        .onAppear { compass.start() }
        .onDisappear { compass.stop() }
    }

    private var overlay: some View {
        let d = diff(bearing, compass.heading ?? 0)
        let aligned = abs(d) < 3
        return GeometryReader { geo in
            ZStack {
                // Görüş alanı ~60°: fark ±30° içindeyse Kâbe işareti ekranda
                if abs(d) < 32 {
                    Text("🕋")
                        .font(.system(size: aligned ? 90 : 64))
                        .shadow(color: aligned ? .yellow : .black, radius: aligned ? 18 : 4)
                        .position(x: geo.size.width / 2 + CGFloat(d / 30) * geo.size.width / 2, y: geo.size.height * 0.42)
                }
                Image(systemName: "arrow.up")
                    .font(.system(size: 54, weight: .bold))
                    .foregroundStyle(aligned ? Color.green : Color.white)
                    .shadow(radius: 4)
                    .rotationEffect(.degrees(d))
                    .position(x: geo.size.width / 2, y: geo.size.height * 0.72)
                VStack {
                    Spacer()
                    Text(aligned ? L("qibla.aligned") : L("qibla.turn"))
                        .font(.headline).foregroundStyle(.white).shadow(radius: 3)
                    Text(L("qibla.cameraNote"))
                        .font(.footnote).foregroundStyle(.white.opacity(0.85)).multilineTextAlignment(.center)
                        .padding(.horizontal, 24).padding(.bottom, 24).shadow(radius: 3)
                }
            }
        }
        .sensoryFeedback(.success, trigger: aligned) { _, now in now }
    }
}

/// Arka kamera önizlemesi (AVCaptureVideoPreviewLayer); görüntü kaydedilmez
private struct CameraPreview: UIViewRepresentable {
    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var preview: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
        let session = AVCaptureSession()

        func start() {
            guard session.inputs.isEmpty,
                  let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                  let input = try? AVCaptureDeviceInput(device: device), session.canAddInput(input)
            else { return }
            session.addInput(input)
            preview.session = session
            preview.videoGravity = .resizeAspectFill
            let s = session
            DispatchQueue.global(qos: .userInitiated).async { s.startRunning() }
        }

        override func removeFromSuperview() {
            let s = session
            DispatchQueue.global(qos: .userInitiated).async { s.stopRunning() }
            super.removeFromSuperview()
        }
    }

    func makeUIView(context: Context) -> PreviewView {
        let v = PreviewView()
        v.start()
        return v
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {}
}
