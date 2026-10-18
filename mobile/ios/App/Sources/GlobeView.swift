import SwiftUI
import MapKit

/// Dönen dünya (sitedeki küre gibi): Apple Haritalar uydu görüntüsüyle küre, gündüz/gece gölgesi, 15°'lik
/// saat dilimi meridyenleri (0° kırmızı) ve şehirlerin yerel saatleri. Kendiliğinden döner (bir tur 2 dk);
/// dokununca durur, yakınlaşınca bütün şehirler görünür. Hareketi azalt tercihinde dönmez. Tam ekrana alınabilir.
struct GlobeView: View {
    /// Tam ekran kopyası: kapatma düğmesi gösterilir
    var fullscreen = false
    /// Sitedeki GLOBE_CITIES (build.mjs)
    static let featured = ["istanbul", "london", "moscow", "dubai", "mumbai", "singapore", "beijing", "tokyo", "sydney",
                           "cairo", "johannesburg", "sao-paulo", "new-york", "los-angeles"]
    private static let farDistance: Double = 32_000_000
    private static let centerLat: Double = 20

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var lon: Double = 30
    @State private var position: MapCameraPosition = .camera(MapCamera(
        centerCoordinate: CLLocationCoordinate2D(latitude: GlobeView.centerLat, longitude: 30), distance: GlobeView.farDistance))
    @State private var spinning = true
    @State private var zoomed = false
    @State private var showFullscreen = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        TimelineView(.everyMinute) { context in
            let now = context.date
            Map(position: $position, interactionModes: .all) {
                ForEach(Self.nightPolygons(at: now), id: \.self) { ring in
                    MapPolygon(coordinates: ring.map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lon) })
                        .foregroundStyle(.black.opacity(0.38))
                }
                ForEach(-11...12, id: \.self) { h in
                    MapPolyline(coordinates: stride(from: -80.0, through: 80.0, by: 10).map {
                        CLLocationCoordinate2D(latitude: $0, longitude: Double(h) * 15)
                    })
                    .stroke(h == 0 ? Color.red : Color.white.opacity(0.45), lineWidth: h == 0 ? 2 : 1)
                    if !zoomed {
                        Annotation("UTC", coordinate: CLLocationCoordinate2D(latitude: -48, longitude: Double(h) * 15)) {
                            Text(h == 0 ? "UTC" : String(format: "%+d", h))
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(h == 0 ? Color.red : Color.white)
                                .shadow(radius: 2)
                        }
                        .annotationTitles(.hidden)
                    }
                }
                ForEach(zoomed ? City.all : City.all.filter { Self.featured.contains($0.key) }) { c in
                    Annotation(c.displayName, coordinate: CLLocationCoordinate2D(latitude: c.lat, longitude: c.lon)) {
                        VStack(spacing: 0) {
                            Text(c.displayName).font(.system(size: 10, weight: .medium))
                            Text(Fmt.time(now, tz: c.zone))
                                .font(.system(size: 13, weight: .bold).monospacedDigit())
                                .environment(\.layoutDirection, .leftToRight)
                        }
                        .padding(.horizontal, 5)
                        .padding(.vertical, 2)
                        .background(.black.opacity(0.55), in: RoundedRectangle(cornerRadius: 5))
                        .foregroundStyle(.white)
                    }
                    .annotationTitles(.hidden)
                }
            }
            .mapStyle(.imagery(elevation: .realistic))
            .onMapCameraChange(frequency: .onEnd) { ctx in
                zoomed = ctx.camera.distance < Self.farDistance * 0.6
                if !spinning { lon = ctx.camera.centerCoordinate.longitude }
                if zoomed { spinning = false }
            }
            .simultaneousGesture(DragGesture(minimumDistance: 4).onChanged { _ in spinning = false })
        }
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
        .fullScreenCover(isPresented: $showFullscreen) {
            GlobeView(fullscreen: true)
                .ignoresSafeArea()
                .statusBarHidden()
        }
        .overlay(alignment: .bottomTrailing) {
            HStack(spacing: 10) {
                if zoomed || !spinning {
                    Button {
                        zoomed = false
                        spinning = !reduceMotion
                        withAnimation(.easeInOut(duration: 0.8)) { setCamera() }
                    } label: {
                        Label(L("globe.reset"), systemImage: "globe")
                    }
                }
            }
            .font(.footnote.weight(.semibold))
            .buttonStyle(.borderedProminent)
            .tint(.black.opacity(0.55))
            .padding(10)
        }
        // Dönüş: her 10 saniyede 30° (bir tur 2 dk), MapKit'in kendi doğrusal kamera geçişiyle; kareleri
        // tek tek kaydırmak her adımda bütün haritayı yeniden çizdirip takılmaya yol açıyordu.
        .task(id: spinning) {
            guard spinning, !reduceMotion else {
                if reduceMotion { spinning = false }
                return
            }
            while !Task.isCancelled && spinning {
                // 180° doğrultusunu geçerken kamera ters yönden dolanmasın: 180'e kadar git, aynı meridyen
                // olan -180'e geçişsiz atla, sonra devam et
                if lon >= 180 {
                    lon = -180
                    setCamera()
                }
                let step = min(30, 180 - lon)
                lon += step
                let seconds = 10 * step / 30
                withAnimation(.linear(duration: seconds)) { setCamera() }
                try? await Task.sleep(for: .seconds(seconds))
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(L("globe.label"))
    }

    private func setCamera() {
        position = .camera(MapCamera(centerCoordinate: CLLocationCoordinate2D(latitude: Self.centerLat, longitude: lon),
                                     distance: Self.farDistance))
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
