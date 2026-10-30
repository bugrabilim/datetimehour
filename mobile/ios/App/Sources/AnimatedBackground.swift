import SwiftUI

/// Tam ekran saatin arka planı (30 Ekim 2026, rakip analizi #26): düz, fotoğraf ya da hareketli (gökyüzü, renk akışı,
/// yıldızlar). Seçim `fsBg`; kayıt yoksa fotoğraf varsa "photo", yoksa "plain". Hareketli olanlar Canvas ile çizilir;
/// "hareketi azalt" açıksa durağan tek kare. Gökyüzü, seçili yerde Güneş'in yüksekliğine göre (küredeki Güneş formülü)
/// gündüz, alacakaranlık ya da gece olur. Android'deki AnimatedBackground ile aynı renkler ve hareket.
enum FullscreenBackground: String, CaseIterable, Identifiable {
    case plain, photo, sky, flow, stars

    static let key = "fsBg"
    var id: String { rawValue }
    var title: String { L("look.bg.\(rawValue)") }
    var animated: Bool { self == .sky || self == .flow || self == .stars }

    static var current: FullscreenBackground {
        if let raw = UserDefaults.standard.string(forKey: key), let v = FullscreenBackground(rawValue: raw) { return v }
        return UserDefaults.standard.double(forKey: FullscreenPhoto.versionKey) > 0 ? .photo : .plain
    }
}

/// Hareketli arka plan; yazılar üstünde koyu temanın açık renkleriyle okunur
struct AnimatedBackground: View {
    let kind: FullscreenBackground
    var place: AppPlace = PlaceModel.shared.place
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30, paused: reduceMotion)) { context in
            let t = reduceMotion ? 0 : context.date.timeIntervalSinceReferenceDate
            Canvas { ctx, size in
                switch kind {
                case .sky: Self.sky(&ctx, size, t: t, alt: Self.sunAltitude(place: place, at: context.date))
                case .flow: Self.flow(&ctx, size, t: t)
                default: Self.stars(&ctx, size, t: t, base: [Color(hex: 0x05070F), Color(hex: 0x111A3A)], alpha: 1)
                }
            }
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }

    /// Seçili yerde Güneş'in yüksekliği (derece): alt güneş noktasından küresel üçgen
    static func sunAltitude(place: AppPlace, at date: Date) -> Double {
        let s = GlobeSky.sun(date)
        let r = Double.pi / 180
        let v = sin(place.lat * r) * sin(s.lat * r) + cos(place.lat * r) * cos(s.lat * r) * cos((place.lon - s.lon) * r)
        return asin(max(-1, min(1, v))) / r
    }

    // MARK: Çizimler

    private static func gradient(_ ctx: inout GraphicsContext, _ size: CGSize, _ colors: [Color]) {
        ctx.fill(Path(CGRect(origin: .zero, size: size)),
                 with: .linearGradient(Gradient(colors: colors), startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height)))
    }

    /// Gündüz mavi + süzülen bulutlar, alacakaranlıkta turuncu-mor, gece koyu lacivert + yıldızlar (geçişler yumuşak)
    static func sky(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double, alt: Double) {
        let night = [Color(hex: 0x0B1026), Color(hex: 0x1B2350)]
        let dusk = [Color(hex: 0x2B3A67), Color(hex: 0xC8607A), Color(hex: 0xF6A75F)]
        let day = [Color(hex: 0x2F6FCB), Color(hex: 0x8EC9F2)]
        if alt <= -6 {
            stars(&ctx, size, t: t, base: night, alpha: 1)
        } else if alt < 6 {
            gradient(&ctx, size, dusk)
            // Alacakaranlığın karanlık ucunda yıldızlar belirir
            if alt < 0 { stars(&ctx, size, t: t, base: nil, alpha: -alt / 6) }
        } else {
            gradient(&ctx, size, day)
            clouds(&ctx, size, t: t)
        }
        // Yazılar okunsun diye hafif koyulaştırma
        ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.black.opacity(alt >= 6 ? 0.2 : 0.1)))
    }

    private static func clouds(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double) {
        for i in 0..<4 {
            let w = size.width * (0.45 + 0.1 * Double(i % 2)), h = w * 0.28
            let speed = 6.0 + Double(i) * 3 // pt/sn
            let x = (Double(i) * size.width * 0.37 + t * speed).truncatingRemainder(dividingBy: size.width + w) - w
            let y = size.height * (0.12 + 0.17 * Double(i))
            let rect = CGRect(x: x, y: y, width: w, height: h)
            ctx.fill(Path(ellipseIn: rect), with: .radialGradient(
                Gradient(colors: [.white.opacity(0.55), .white.opacity(0)]),
                center: CGPoint(x: rect.midX, y: rect.midY), startRadius: 0, endRadius: w / 2))
        }
    }

    /// Koyu zemin üstünde dönerek kayan üç renk lekesi (uygulama renginden)
    static func flow(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double) {
        gradient(&ctx, size, [Color(hex: 0x0B1020), Color(hex: 0x141A33)])
        let colors = [Theme.accent, Theme.accent2, Color(hex: 0x6C4AB6)]
        let r = max(size.width, size.height) * 0.6
        for (k, c) in colors.enumerated() {
            let k = Double(k)
            let p = CGPoint(x: size.width * (0.5 + 0.35 * sin(t * 0.07 + k * 2.1)),
                            y: size.height * (0.5 + 0.35 * cos(t * 0.05 + k * 1.7)))
            ctx.fill(Path(ellipseIn: CGRect(x: p.x - r, y: p.y - r, width: 2 * r, height: 2 * r)), with: .radialGradient(
                Gradient(colors: [c.opacity(0.55), c.opacity(0)]), center: p, startRadius: 0, endRadius: r))
        }
    }

    /// Sabit tohumlu yıldızlar; her biri kendi hızıyla göz kırpar, gökyüzü çok yavaş kayar
    static func stars(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double, base: [Color]?, alpha: Double) {
        if let base { gradient(&ctx, size, base) }
        var seed: UInt64 = 0x5EED
        func rnd() -> Double {
            seed = seed &* 6364136223846793005 &+ 1442695040888963407
            return Double(seed >> 11) / Double(1 << 53)
        }
        let drift = t * 2 // pt/sn
        for _ in 0..<140 {
            let x = (rnd() * size.width + drift).truncatingRemainder(dividingBy: size.width)
            let y = rnd() * size.height
            let s = 0.6 + rnd() * 1.6
            let phase = rnd() * 6.28, speed = 0.5 + rnd() * 1.5
            let a = (0.35 + 0.65 * (0.5 + 0.5 * sin(t * speed + phase))) * alpha
            ctx.fill(Path(ellipseIn: CGRect(x: x - s / 2, y: y - s / 2, width: s, height: s)), with: .color(.white.opacity(a)))
        }
    }
}

extension Color {
    /// 0xRRGGBB
    init(hex: UInt32) {
        self.init(red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
    }
}
