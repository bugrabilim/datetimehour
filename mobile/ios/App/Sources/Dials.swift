import SwiftUI

/*
 * Kadran koleksiyonu (3 Kasım 2026, 2. analiz #1; sahibin ekran görüntüleri): rakamlı analog saatlerin kadranı.
 * 12 kadran: klasik, Roma rakamı, sade, noktalı, kronograf, çift halka, metal çerçeve, radar, Çin rakamı ve dokulu
 * zeminler (deniz, yaprak, mermer); lüm (2. analiz #11); 4 Kasım 2026'da sahibin isteğiyle ("yetersiz kadran var") 12 yeni:
 * Doğu Arap rakamı, pilot, Bauhaus, güneş ışını, eski cep saati, dalgıç, gökkuşağı (çocuklar), neon, ahşap, İznik çini,
 * geometrik (sekiz köşeli yıldız), sektör. Görsel dosya yok, hepsi çizim. Her kadranda 5 renk (ibre ve saniye rengi; "auto"
 * kadranın kendi rengi). Seçim `dial.face`, `dial.color`; galeri açılınca "Yeni" işareti ve saat ekranındaki tanıtım
 * kalkar (`dial.seen2`; yeni kadranlar için 4 Kasım 2026'da yeniden gösterilir). Widget'lar kendi görünümleriyle klasik kadranda kalır. Android'deki Dials.kt ile aynı adlar.
 */
enum DialFace: String, CaseIterable, Identifiable {
    case classic, roman, minimal, dots, chrono, double, metal, radar, chinese, ocean, leaf, marble
    /// Lüm (2. analiz #11): siyah kadranda işaretler ve ibreler seçilen renkte ışır (14 renk, `dial.lume`)
    case lume
    case eastern, pilot, bauhaus, sunburst, vintage, diver, rainbow, neon, wood, iznik, geometric, sector

    static let key = "dial.face"
    static let colorKey = "dial.color"
    static let seenKey = "dial.seen2"
    /// 4 Kasım 2026'da eklenenler: galeride "Yeni" işareti bunlarda
    static let added: Set<DialFace> = [.eastern, .pilot, .bauhaus, .sunburst, .vintage, .diver, .rainbow, .neon, .wood, .iznik, .geometric, .sector]

    var id: String { rawValue }
    var title: String { L("dial.\(rawValue)") }

    static var current: DialFace { DialFace(rawValue: UserDefaults.standard.string(forKey: key) ?? "") ?? .classic }

    /// quarters: yalnız 12, 3, 6, 9
    enum Numerals { case arabic, roman, chinese, eastern, quarters, none }
    /// none: kadran kendi işaretlerini çizer (dalgıç, gökkuşağı, İznik, geometrik)
    enum Ticks { case full, hours, dots, none }

    var numerals: Numerals {
        switch self {
        case .classic, .chrono, .double, .ocean, .marble, .lume, .pilot, .vintage, .rainbow, .iznik, .sector: .arabic
        case .roman, .wood: .roman
        case .chinese: .chinese
        case .eastern: .eastern
        case .bauhaus: .quarters
        default: .none
        }
    }

    var ticks: Ticks {
        switch self {
        case .minimal, .metal, .leaf, .lume, .sunburst, .neon, .wood: .hours
        case .dots: .dots
        case .rainbow, .iznik, .geometric, .diver: .none
        default: .full
        }
    }

    /// Sabit renkli kadranlar temadan bağımsızdır; diğerleri uygulamanın renkleriyle
    func colors(theme: ClockColors) -> ClockColors {
        func c(_ v: UInt32) -> Color { dc(v) }
        switch self {
        case .metal: return ClockColors(face: c(0x1E2228), border: c(0xADB5BD), tick: c(0xE9ECEF), minorTick: c(0x868E96), ink: c(0xF1F3F5), hand: c(0xF2A65A), second: c(0xFF6B6B))
        case .radar: return ClockColors(face: c(0x04140A), border: c(0x2B8A3E), tick: c(0x51CF66), minorTick: c(0x2B8A3E), ink: c(0x8CE99A), hand: c(0x51CF66), second: c(0xB2F2BB))
        case .ocean: return ClockColors(face: c(0x0B3D91), border: c(0x74C0FC), tick: .white, minorTick: c(0xA5D8FF), ink: .white, hand: c(0xFFD43B), second: c(0xFF8787))
        case .leaf: return ClockColors(face: c(0x1E6B34), border: c(0x8CE99A), tick: .white, minorTick: c(0xB2F2BB), ink: .white, hand: c(0xFFD43B), second: c(0xFF8787))
        case .marble: return ClockColors(face: c(0xF1EEE9), border: c(0xBDB5A6), tick: c(0x343A40), minorTick: c(0x868E96), ink: c(0x212529), hand: c(0x9A3F00), second: c(0xC92A2A))
        case .chinese: var k = theme; k.second = c(0xC92A2A); return k
        case .lume:
            let g = Lume.current
            return ClockColors(face: c(0x07080B), border: g.opacity(0.45), tick: g, minorTick: g.opacity(0.55), ink: g, hand: g, second: c(0xFF6B6B))
        case .pilot: return ClockColors(face: c(0x15171A), border: c(0x3A3F45), tick: c(0xF1F3F5), minorTick: c(0x8D949B), ink: c(0xF8F9FA), hand: c(0xF8F9FA), second: c(0xFF922B))
        case .bauhaus: return ClockColors(face: c(0xF4F1EA), border: c(0x1A1A1A), tick: c(0x1A1A1A), minorTick: c(0x8C8C8C), ink: c(0x1A1A1A), hand: c(0x1A1A1A), second: c(0xE03131))
        case .sunburst: return ClockColors(face: c(0x2546A8), border: c(0xC7D2FE), tick: c(0xF1F3F5), minorTick: c(0xA5B4FC), ink: c(0xF8F9FA), hand: c(0xE9ECEF), second: c(0xFFD43B))
        case .vintage: return ClockColors(face: c(0xF3E9D2), border: c(0x8C7A5B), tick: c(0x3B2F1E), minorTick: c(0x7A6A50), ink: c(0x2B2116), hand: c(0x1E3A8A), second: c(0x8B1E1E))
        case .diver: return ClockColors(face: c(0x0B1220), border: c(0x334155), tick: c(0xE6F4EA), minorTick: c(0x94A3B8), ink: c(0xE6F4EA), hand: c(0xE6F4EA), second: c(0xFF6B6B))
        case .rainbow: return ClockColors(face: c(0xFFFFFF), border: c(0x495057), tick: c(0x343A40), minorTick: c(0xADB5BD), ink: c(0x212529), hand: c(0x343A40), second: c(0xE03131))
        case .neon: return ClockColors(face: c(0x0A0A14), border: c(0x3BF0FF), tick: c(0xFF4FD8), minorTick: c(0x7A3E8C), ink: c(0x3BF0FF), hand: c(0x3BF0FF), second: c(0xFF4FD8))
        case .wood: return ClockColors(face: c(0x7A4A24), border: c(0xE0B65A), tick: c(0xF1D38A), minorTick: c(0xC9A35A), ink: c(0xF6E3B0), hand: c(0xF1D38A), second: c(0xE8590C))
        case .iznik: return ClockColors(face: c(0xFBFBF8), border: c(0x1F4E9C), tick: c(0x1F4E9C), minorTick: c(0x6C8FC9), ink: c(0x1F4E9C), hand: c(0x173B78), second: c(0xC0392B))
        case .geometric: return ClockColors(face: c(0x0F3B2E), border: c(0xD4AF37), tick: c(0xD4AF37), minorTick: c(0x8C7A3A), ink: c(0xF3E3A6), hand: c(0xD4AF37), second: c(0xF8F9FA))
        case .sector: return ClockColors(face: c(0xEEE8DA), border: c(0x1B3A5C), tick: c(0x1B3A5C), minorTick: c(0x6B7F95), ink: c(0x1B3A5C), hand: c(0x1B3A5C), second: c(0xD9480F))
        default: return theme
        }
    }

    static let romanNumerals = ["XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"]
    static let chineseNumerals = ["十二", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一"]
    static let easternNumerals = ["١٢", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "١٠", "١١"]
    /// Gökkuşağı kadranının saat dilimleri (pastel)
    static let rainbowColors: [UInt32] = [0xFFC9C9, 0xFFD8A8, 0xFFEC99, 0xD8F5A2, 0xB2F2BB, 0x96F2D7, 0x99E9F2, 0xA5D8FF, 0xBAC8FF, 0xD0BFFF, 0xEEBEFA, 0xFCC2D7]
}

/// Lüm kadranının ışıma renkleri (2. analiz #11): siyah üstünde parlak tonlar; adları `color.<id>` (Android'deki Lume ile aynı)
enum Lume {
    static let key = "dial.lume"
    static let colors: [(id: String, rgb: UInt32)] = [
        ("green", 0x7CFC8A), ("mint", 0x98F5C9), ("aqua", 0x5CE1E6), ("blue", 0x6FA8FF), ("ice", 0xCDEBFF), ("white", 0xF2F2F2),
        ("lemon", 0xF5F57A), ("yellow", 0xFFD54F), ("orange", 0xFFA94D), ("amber", 0xFFBF47), ("red", 0xFF6B6B), ("pink", 0xFF8FC7),
        ("violet", 0xB48CFF), ("lilac", 0xD9B8FF),
    ]
    static func color(_ id: String) -> Color { dc(colors.first { $0.id == id }?.rgb ?? 0x7CFC8A) }
    static var id: String {
        let v = UserDefaults.standard.string(forKey: key) ?? "green"
        return colors.contains { $0.id == v } ? v : "green"
    }
    static var current: Color { color(id) }
    /// Seçicideki satır (0: ilk 7, 1: son 7)
    static func ids(_ row: Int) -> [String] { Array(colors.map(\.id)[(row * 7)..<(row * 7 + 7)]) }
}

/// İbre rengi: "auto" kadranın kendi rengi
enum DialColor: String, CaseIterable, Identifiable {
    case auto, red, blue, green, gold
    var id: String { rawValue }
    var color: Color? {
        switch self {
        case .auto: nil
        case .red: dc(0xE03131)
        case .blue: dc(0x1C7ED6)
        case .green: dc(0x2F9E44)
        case .gold: dc(0xE8A317)
        }
    }
    static var current: DialColor { DialColor(rawValue: UserDefaults.standard.string(forKey: DialFace.colorKey) ?? "") ?? .auto }

    func apply(_ k: ClockColors) -> ClockColors {
        guard let c = color else { return k }
        var out = k
        out.hand = c
        out.second = c
        return out
    }
}

/// Kadranın zemini (dokular dahil) ve çerçevesi; 200 birimlik kadran ölçüleri
struct DialFill: View {
    let face: DialFace
    let k: ClockColors
    let u: CGFloat
    /// Radarın tarayıcısı için saniye
    var seconds: Double = 0

    var body: some View {
        ZStack {
            switch face {
            case .ocean:
                Circle().fill(LinearGradient(colors: [dc(0x1971C2), dc(0x0B3D91)], startPoint: .top, endPoint: .bottom))
                texture { ctx, s in
                    for i in 0..<7 {
                        var p = Path()
                        let y0 = s.height * (0.2 + 0.1 * Double(i))
                        p.move(to: CGPoint(x: 0, y: y0))
                        for x in stride(from: 0.0, through: s.width, by: 4) {
                            p.addLine(to: CGPoint(x: x, y: y0 + sin(x / s.width * 12 + Double(i)) * s.height * 0.015))
                        }
                        ctx.stroke(p, with: .color(.white.opacity(0.10 + 0.02 * Double(i % 3))), lineWidth: s.width * 0.008)
                    }
                }
            case .leaf:
                Circle().fill(LinearGradient(colors: [dc(0x2F9E44), dc(0x1E6B34)], startPoint: .topLeading, endPoint: .bottomTrailing))
                texture { ctx, s in
                    var mid = Path()
                    mid.move(to: CGPoint(x: s.width * 0.15, y: s.height * 0.85))
                    mid.addQuadCurve(to: CGPoint(x: s.width * 0.85, y: s.height * 0.15), control: CGPoint(x: s.width * 0.4, y: s.height * 0.35))
                    ctx.stroke(mid, with: .color(.white.opacity(0.16)), lineWidth: s.width * 0.012)
                    for i in 1..<7 {
                        let t = Double(i) / 7
                        let base = CGPoint(x: s.width * (0.15 + 0.7 * t), y: s.height * (0.85 - 0.7 * t) - s.height * 0.08 * sin(t * .pi))
                        for side in [-1.0, 1.0] {
                            var v = Path()
                            v.move(to: base)
                            v.addQuadCurve(to: CGPoint(x: base.x + side * s.width * 0.16, y: base.y - s.height * 0.02 - (side > 0 ? 0 : s.height * 0.1)),
                                           control: CGPoint(x: base.x + side * s.width * 0.06, y: base.y - s.height * 0.06))
                            ctx.stroke(v, with: .color(.white.opacity(0.10)), lineWidth: s.width * 0.006)
                        }
                    }
                }
            case .marble:
                Circle().fill(k.face)
                texture { ctx, s in
                    var seed: UInt64 = 0x3A7B1E
                    func rnd() -> Double { seed = seed &* 6364136223846793005 &+ 1442695040888963407; return Double(seed >> 11) / Double(1 << 53) }
                    for i in 0..<9 {
                        var p = Path()
                        var pt = CGPoint(x: rnd() * s.width, y: 0)
                        p.move(to: pt)
                        for _ in 0..<6 {
                            let nx = pt.x + (rnd() - 0.45) * s.width * 0.25, ny = pt.y + s.height / 6
                            p.addQuadCurve(to: CGPoint(x: nx, y: ny), control: CGPoint(x: pt.x + (rnd() - 0.5) * s.width * 0.3, y: pt.y + s.height / 12))
                            pt = CGPoint(x: nx, y: ny)
                        }
                        ctx.stroke(p, with: .color(dc(i % 3 == 0 ? 0xB8AE9C : 0xCFC8BD).opacity(0.7)), lineWidth: s.width * (i % 3 == 0 ? 0.008 : 0.004))
                    }
                }
            case .sunburst:
                Circle().fill(RadialGradient(colors: [dc(0x4263EB), dc(0x1C3584)], center: .center, startRadius: 0, endRadius: 94 * u))
                texture { ctx, s in
                    let c = CGPoint(x: s.width / 2, y: s.height / 2), r = s.width / 2
                    for i in 0..<120 {
                        let a = Double(i) * 3 * .pi / 180
                        var p = Path()
                        p.move(to: CGPoint(x: c.x + 8 * u * sin(a), y: c.y - 8 * u * cos(a)))
                        p.addLine(to: CGPoint(x: c.x + r * sin(a), y: c.y - r * cos(a)))
                        ctx.stroke(p, with: .color(.white.opacity(i % 2 == 0 ? 0.07 : 0.03)), lineWidth: 1.2 * u)
                    }
                }
            case .vintage:
                Circle().fill(RadialGradient(colors: [dc(0xF7EFDC), dc(0xF3E9D2), dc(0xDCC9A3)], center: .center, startRadius: 0, endRadius: 94 * u))
            case .rainbow:
                Circle().fill(k.face)
                texture { ctx, s in
                    let c = CGPoint(x: s.width / 2, y: s.height / 2), r = s.width / 2
                    for i in 0..<12 {
                        var p = Path()
                        p.move(to: c)
                        p.addArc(center: c, radius: r, startAngle: .degrees(Double(i) * 30 - 105), endAngle: .degrees(Double(i) * 30 - 75), clockwise: false)
                        p.closeSubpath()
                        ctx.fill(p, with: .color(dc(DialFace.rainbowColors[i])))
                    }
                    ctx.fill(Path(ellipseIn: CGRect(x: c.x - 34 * u, y: c.y - 34 * u, width: 68 * u, height: 68 * u)), with: .color(.white))
                }
            case .wood:
                Circle().fill(LinearGradient(colors: [dc(0x9C6634), dc(0x5C3A1E)], startPoint: .topLeading, endPoint: .bottomTrailing))
                texture { ctx, s in
                    for i in 0..<16 {
                        var p = Path()
                        let y0 = s.height * (Double(i) + 0.5) / 16
                        p.move(to: CGPoint(x: 0, y: y0))
                        for x in stride(from: 0.0, through: s.width, by: 3 * u) {
                            let t = x / s.width
                            p.addLine(to: CGPoint(x: x, y: y0 + sin(t * Double(5 + i % 4) + Double(i) * 1.7) * s.height * 0.012 * Double(1 + i % 3)))
                        }
                        ctx.stroke(p, with: .color(i % 2 == 0 ? dc(0x3E2410).opacity(0.28) : dc(0xC08A52).opacity(0.16)), lineWidth: s.width * 0.005)
                    }
                    // Budak
                    let cx = s.width / 2, cy = s.height / 2
                    for j in 0..<4 {
                        let j = Double(j)
                        ctx.stroke(Path(ellipseIn: CGRect(x: cx - (52 + j * 4) * u, y: cy + (30 - j * 2.5) * u, width: (14 + j * 8) * u, height: (7 + j * 5) * u)),
                                   with: .color(dc(0x3E2410).opacity(0.25)), lineWidth: 1.2 * u)
                    }
                }
            case .geometric:
                Circle().fill(RadialGradient(colors: [dc(0x145A45), dc(0x0B2E24)], center: .center, startRadius: 0, endRadius: 94 * u))
            case .sector:
                Circle().fill(k.face)
                Circle().fill(dc(0xE2D9C6)).frame(width: 100 * u, height: 100 * u)
            case .classic:
                // Klasik kadran (doğal tasarım madde 9): ortası hafif aydınlık radyal geçiş
                Circle().fill(k.face)
                Circle().fill(RadialGradient(colors: [.white.opacity(0.22), .white.opacity(0)], center: .center, startRadius: 0, endRadius: 94 * u))
            default:
                Circle().fill(k.face)
            }
            if face == .radar {
                ForEach([30.0, 60.0, 88.0], id: \.self) { r in
                    Circle().stroke(k.minorTick.opacity(0.6), lineWidth: 1 * u).frame(width: 2 * r * u, height: 2 * r * u)
                }
                Rectangle().fill(k.minorTick.opacity(0.5)).frame(width: 1 * u, height: 176 * u)
                Rectangle().fill(k.minorTick.opacity(0.5)).frame(width: 176 * u, height: 1 * u)
                // Tarayıcı: saniye ibresiyle döner
                Circle()
                    .fill(AngularGradient(colors: [k.tick.opacity(0), k.tick.opacity(0), k.tick.opacity(0.35)], center: .center))
                    .rotationEffect(.degrees(seconds * 6 - 90))
            }
        }
        .frame(width: 188 * u, height: 188 * u)
        .clipShape(Circle())
        .overlay {
            if face == .metal {
                Circle().stroke(AngularGradient(colors: [dc(0xF8F9FA), dc(0x868E96), dc(0xDEE2E6), dc(0x495057), dc(0xF8F9FA)], center: .center),
                                lineWidth: 9 * u)
            } else if face == .neon {
                // Işıma: geniş ve soluktan ince ve parlağa
                ZStack {
                    ForEach([(12.0, 0.10), (7.0, 0.22), (3.5, 0.6), (1.5, 1.0)], id: \.0) { w in
                        Circle().stroke(k.border.opacity(w.1), lineWidth: w.0 * u).frame(width: 180 * u, height: 180 * u)
                    }
                    Circle().stroke(k.tick.opacity(0.35), lineWidth: 1 * u).frame(width: 116 * u, height: 116 * u)
                }
            } else if face == .classic {
                // İnce dış halka
                Circle().stroke(k.border.opacity(0.8), lineWidth: 1.2 * u)
            } else {
                Circle().stroke(k.border, lineWidth: 2 * u)
            }
        }
        .frame(width: 188 * u, height: 188 * u)
        // Klasik kadranın altında yumuşak gölge
        .shadow(color: face == .classic ? Color(red: 0.29, green: 0.2, blue: 0.12).opacity(0.12) : .clear, radius: 4 * u, y: 3 * u)
    }

    private func texture(_ draw: @escaping (inout GraphicsContext, CGSize) -> Void) -> some View {
        Canvas { ctx, s in draw(&ctx, s) }.allowsHitTesting(false)
    }
}

/// Kadranın çizgileri, rakamları ve süsleri (ibreler hariç)
struct DialMarks: View {
    let face: DialFace
    let k: ClockColors
    let u: CGFloat
    var hour: Double = 10
    var minute: Double = 10
    var seconds: Double = 0

    var body: some View {
        ZStack {
            switch face.ticks {
            case .full:
                // Klasik: dakikalar %40 noktalar, saatler kısa yuvarlak uçlu çizgiler (doğal tasarım madde 9)
                ForEach(0..<60, id: \.self) { i in
                    let major = i % 5 == 0
                    if face == .classic && !major {
                        Circle().fill(k.minorTick.opacity(0.4))
                            .frame(width: 2.2 * u, height: 2.2 * u)
                            .offset(y: -85 * u)
                            .rotationEffect(.degrees(Double(i) * 6))
                    } else {
                        Capsule().fill(major ? k.tick : k.minorTick)
                            .frame(width: (major ? (face == .classic ? 3.2 : 3) : 1.4) * u, height: (major ? (face == .classic ? 8 : 12) : 5) * u)
                            .offset(y: -(major ? (face == .classic ? 83 : 82) : 83.5) * u)
                            .rotationEffect(.degrees(Double(i) * 6))
                    }
                }
            case .hours:
                ForEach(0..<12, id: \.self) { i in
                    Capsule().fill(k.tick)
                        .frame(width: (i % 3 == 0 ? 6 : 4) * u, height: (i % 3 == 0 ? 18 : 13) * u)
                        .offset(y: -(i % 3 == 0 ? 77 : 79.5) * u)
                        .rotationEffect(.degrees(Double(i) * 30))
                }
            case .dots:
                ForEach(0..<12, id: \.self) { i in
                    Circle().fill(k.tick).frame(width: (i % 3 == 0 ? 11 : 7) * u, height: (i % 3 == 0 ? 11 : 7) * u)
                        .offset(y: -80 * u)
                        .rotationEffect(.degrees(Double(i) * 30))
                }
            case .none:
                EmptyView()
            }
            if DialOrnaments.faces.contains(face) {
                Canvas { ctx, size in DialOrnaments.draw(face, &ctx, size, k: k) }
                    .frame(width: 200 * u, height: 200 * u)
                    .allowsHitTesting(false)
            }
            if face == .double {
                // Dış halka dakika (05…60), iç halka saat
                ForEach(1...12, id: \.self) { n in
                    let a = Double(n) * 30 * .pi / 180
                    Text(verbatim: String(format: "%02d", n * 5))
                        .font(.system(size: 8 * u, weight: .medium, design: .rounded)).foregroundStyle(k.minorTick)
                        .offset(x: 70 * u * sin(a), y: -70 * u * cos(a))
                }
                Circle().stroke(k.minorTick.opacity(0.5), lineWidth: 1 * u).frame(width: 124 * u, height: 124 * u)
            }
            if face == .chrono {
                ForEach([(-40.0, 0.0), (40.0, 0.0), (0.0, 40.0)], id: \.0) { p in
                    let x = p.0
                    let y = p.1
                    ZStack {
                        Circle().stroke(k.minorTick, lineWidth: 1 * u)
                        ForEach(0..<12, id: \.self) { i in
                            Capsule().fill(k.minorTick).frame(width: 1 * u, height: 3 * u).offset(y: -15 * u).rotationEffect(.degrees(Double(i) * 30))
                        }
                        // Sol: 24 saat, sağ: dakika, alt: saniye
                        let ang = x < 0 ? hour * 15 : x > 0 ? minute * 6 : seconds * 6
                        Capsule().fill(x == 0 && y > 0 ? k.second : k.ink).frame(width: 1.6 * u, height: 14 * u).offset(y: -7 * u).rotationEffect(.degrees(ang))
                    }
                    .frame(width: 36 * u, height: 36 * u)
                    .offset(x: x * u, y: y * u)
                }
            }
            let radius = numeralRadius
            let size = numeralSize
            let weight = numeralWeight
            let serif = face.numerals == .roman || face == .vintage
            switch face.numerals {
            case .none: EmptyView()
            default:
                ForEach(0..<12, id: \.self) { i in
                    let n = i == 0 ? 12 : i
                    let a = Double(n) * 30 * .pi / 180
                    let text = numeral(i)
                    // Kronografta alt kadranların üstüne düşen rakamlar (3, 6, 9), pilotta 12 (üçgen) yazılmaz; Bauhaus'ta yalnız 12, 3, 6, 9
                    if !(face == .chrono && [3, 6, 9].contains(n)) && !(face == .pilot && n == 12) && !(face.numerals == .quarters && n % 3 != 0) {
                        Text(verbatim: text)
                            .font(.system(size: size * u, weight: weight, design: serif ? .serif : .default))
                            .foregroundStyle(k.ink)
                            .offset(x: radius * u * sin(a), y: -radius * u * cos(a))
                    }
                }
            }
        }
    }
}

extension DialMarks {
    var numeralRadius: Double {
        switch face {
        case .double: return 46
        case .chrono: return 70
        case .rainbow, .sector: return 62
        case .pilot, .bauhaus: return 64
        case .vintage, .iznik: return 63
        case .wood: return 55
        default: return 66
        }
    }

    var numeralSize: CGFloat {
        switch face {
        case .chrono, .sector: return 12
        case .double, .wood: return 13
        case .roman, .chinese: return 14
        case .eastern: return 16
        case .pilot, .bauhaus, .rainbow: return 18
        default: return 15
        }
    }

    var numeralWeight: Font.Weight {
        switch face {
        case .pilot, .rainbow: return .bold
        case .bauhaus: return .light
        case .vintage: return .regular
        default: return .semibold
        }
    }

    func numeral(_ i: Int) -> String {
        switch face.numerals {
        case .roman: return DialFace.romanNumerals[i]
        case .chinese: return DialFace.chineseNumerals[i]
        case .eastern: return DialFace.easternNumerals[i]
        default: return String(i == 0 ? 12 : i)
        }
    }
}

/// Yeni kadranların süsleri (4 Kasım 2026): 200 birimlik kadranda, Android'deki drawDial ile aynı ölçüler
enum DialOrnaments {
    static let faces: Set<DialFace> = [.pilot, .diver, .iznik, .geometric, .sector, .vintage]

    static func draw(_ face: DialFace, _ ctx: inout GraphicsContext, _ size: CGSize, k: ClockColors) {
        let u = min(size.width, size.height) / 200
        let c = CGPoint(x: size.width / 2, y: size.height / 2)
        func at(_ r: Double, _ deg: Double) -> CGPoint {
            CGPoint(x: c.x + r * u * sin(deg * .pi / 180), y: c.y - r * u * cos(deg * .pi / 180))
        }
        func circle(_ p: CGPoint, _ r: Double) -> Path { Path(ellipseIn: CGRect(x: p.x - r * u, y: p.y - r * u, width: 2 * r * u, height: 2 * r * u)) }
        func ring(_ r: Double, _ color: Color, _ w: Double) { ctx.stroke(circle(c, r), with: .color(color), lineWidth: w * u) }
        func radial(_ deg: Double, _ r1: Double, _ r2: Double) -> Path {
            var p = Path(); p.move(to: at(r1, deg)); p.addLine(to: at(r2, deg)); return p
        }
        switch face {
        case .pilot:
            // 12'de üçgen ve iki nokta
            var t = Path()
            t.move(to: CGPoint(x: c.x - 7 * u, y: c.y - 87 * u)); t.addLine(to: CGPoint(x: c.x + 7 * u, y: c.y - 87 * u))
            t.addLine(to: CGPoint(x: c.x, y: c.y - 72 * u)); t.closeSubpath()
            ctx.fill(t, with: .color(k.tick))
            for dx in [-11.0, 11.0] { ctx.fill(circle(CGPoint(x: c.x + dx * u, y: c.y - 84 * u), 1.8), with: .color(k.tick)) }
        case .diver:
            // Dönen çerçeve: ilk 15 dakika renkli, 10'ar dakikada rakam, sıfırda ışıklı üçgen
            ring(87, dc(0x1F2937), 14)
            var arc = Path()
            arc.addArc(center: c, radius: 87 * u, startAngle: .degrees(-90), endAngle: .degrees(0), clockwise: false)
            ctx.stroke(arc, with: .color(dc(0x2563EB)), lineWidth: 14 * u)
            for m in 0..<60 where (m < 15 || m % 5 == 0) && m % 10 != 0 {
                ctx.stroke(radial(Double(m) * 6, 90, m % 5 == 0 ? 84 : 87), with: .color(.white.opacity(0.7)), lineWidth: 1.2 * u)
            }
            for n in 1...5 {
                ctx.draw(Text(verbatim: String(n * 10)).font(.system(size: 8.5 * u, weight: .bold)).foregroundColor(.white), at: at(87, Double(n) * 60))
            }
            var t = Path()
            t.move(to: CGPoint(x: c.x - 5 * u, y: c.y - 92 * u)); t.addLine(to: CGPoint(x: c.x + 5 * u, y: c.y - 92 * u))
            t.addLine(to: CGPoint(x: c.x, y: c.y - 83 * u)); t.closeSubpath()
            ctx.fill(t, with: .color(k.tick))
            ring(79.5, k.minorTick, 1)
            // Işıklı saat işaretleri
            for i in 0..<12 {
                let d = Double(i) * 30
                if i % 3 == 0 {
                    ctx.stroke(radial(d, 75, 60), with: .color(k.minorTick), style: StrokeStyle(lineWidth: 9 * u, lineCap: .round))
                    ctx.stroke(radial(d, 75, 60), with: .color(k.tick), style: StrokeStyle(lineWidth: 6.5 * u, lineCap: .round))
                } else {
                    ctx.fill(circle(at(68, d), 6), with: .color(k.minorTick))
                    ctx.fill(circle(at(68, d), 4.6), with: .color(k.tick))
                }
            }
        case .iznik:
            // Kobalt mavisi bant, beyaz lale motifleri, aralarda mercan kırmızısı noktalar
            ring(86.5, k.border, 15)
            for i in 0..<12 {
                let d = Double(i) * 30
                var g = ctx
                let p = at(86.5, d)
                g.translateBy(x: p.x, y: p.y)
                g.rotate(by: .degrees(d))
                g.fill(Path(ellipseIn: CGRect(x: -2.2 * u, y: -4.6 * u, width: 4.4 * u, height: 7.4 * u)), with: .color(.white))
                for side in [-28.0, 28.0] {
                    var h = g
                    h.translateBy(x: 0, y: 2 * u)
                    h.rotate(by: .degrees(side))
                    h.fill(Path(ellipseIn: CGRect(x: -1.6 * u, y: -5.6 * u, width: 3.2 * u, height: 6 * u)), with: .color(.white))
                }
                ctx.fill(circle(at(86.5, d + 15), 2.2), with: .color(k.second))
            }
            ring(77, dc(0x2A9D8F), 1.2)
            for i in 0..<60 where i % 5 != 0 { ctx.fill(circle(at(74, Double(i) * 6), 0.9), with: .color(k.minorTick)) }
            for i in 0..<8 {
                var g = ctx
                g.translateBy(x: c.x, y: c.y)
                g.rotate(by: .degrees(Double(i) * 45))
                g.fill(Path(ellipseIn: CGRect(x: -3 * u, y: -26 * u, width: 6 * u, height: 12 * u)), with: .color(dc(0x2A9D8F).opacity(0.3)))
            }
        case .geometric:
            // Sekiz köşeli yıldız (iki kare) ve saatlerde küçük yıldızlar
            func star(_ p: CGPoint, _ h: Double, _ color: Color, stroke: Double?) {
                for rot in [0.0, 45.0] {
                    var g = ctx
                    g.translateBy(x: p.x, y: p.y)
                    g.rotate(by: .degrees(rot))
                    let rect = Path(CGRect(x: -h * u, y: -h * u, width: 2 * h * u, height: 2 * h * u))
                    if let stroke { g.stroke(rect, with: .color(color), lineWidth: stroke * u) } else { g.fill(rect, with: .color(color)) }
                }
            }
            star(c, 40, k.tick.opacity(0.55), stroke: 1.4)
            star(c, 27, k.tick.opacity(0.3), stroke: 1)
            ring(56, k.tick.opacity(0.45), 1)
            ring(88, k.tick.opacity(0.45), 1)
            for i in 0..<12 { star(at(76, Double(i) * 30), i % 3 == 0 ? 5.2 : 3.6, k.tick, stroke: nil) }
            for i in 0..<60 where i % 5 != 0 { ctx.fill(circle(at(76, Double(i) * 6), 1), with: .color(k.minorTick)) }
        case .sector:
            ring(50, k.minorTick, 1)
            ring(74, k.minorTick, 1)
            ring(89.5, k.minorTick, 1)
        case .vintage:
            // Demiryolu dakika halkası
            ring(80, k.minorTick, 0.8)
            ring(88, k.minorTick, 0.8)
        default:
            break
        }
    }
}

/// 0xRRGGBB → renk (widget hedefinde de derlenir; AnimatedBackground'daki Color(hex:) uygulamada)
func dc(_ v: UInt32) -> Color {
    Color(red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255)
}
