import SwiftUI

/*
 * Kadran koleksiyonu (3 Kasım 2026, 2. analiz #1; sahibin ekran görüntüleri): rakamlı analog saatlerin kadranı.
 * 12 kadran: klasik, Roma rakamı, sade, noktalı, kronograf, çift halka, metal çerçeve, radar, Çin rakamı ve dokulu
 * zeminler (deniz, yaprak, mermer). Görsel dosya yok, hepsi çizim. Her kadranda 5 renk (ibre ve saniye rengi; "auto"
 * kadranın kendi rengi). Seçim `dial.face`, `dial.color`; galeri açılınca "Yeni" işareti ve saat ekranındaki tanıtım
 * kalkar (`dial.seen`). Widget'lar kendi görünümleriyle klasik kadranda kalır. Android'deki Dials.kt ile aynı adlar.
 */
enum DialFace: String, CaseIterable, Identifiable {
    case classic, roman, minimal, dots, chrono, double, metal, radar, chinese, ocean, leaf, marble
    /// Lüm (2. analiz #11): siyah kadranda işaretler ve ibreler seçilen renkte ışır (14 renk, `dial.lume`)
    case lume

    static let key = "dial.face"
    static let colorKey = "dial.color"
    static let seenKey = "dial.seen"

    var id: String { rawValue }
    var title: String { L("dial.\(rawValue)") }

    static var current: DialFace { DialFace(rawValue: UserDefaults.standard.string(forKey: key) ?? "") ?? .classic }

    enum Numerals { case arabic, roman, chinese, none }
    enum Ticks { case full, hours, dots }

    var numerals: Numerals {
        switch self {
        case .classic, .chrono, .double, .ocean, .marble, .lume: .arabic
        case .roman: .roman
        case .chinese: .chinese
        default: .none
        }
    }

    var ticks: Ticks {
        switch self {
        case .minimal, .metal, .leaf, .lume: .hours
        case .dots: .dots
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
        default: return theme
        }
    }

    static let romanNumerals = ["XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"]
    static let chineseNumerals = ["十二", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一"]
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
            } else {
                Circle().stroke(k.border, lineWidth: 2 * u)
            }
        }
        .frame(width: 188 * u, height: 188 * u)
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
                ForEach(0..<60, id: \.self) { i in
                    let major = i % 5 == 0
                    Capsule().fill(major ? k.tick : k.minorTick)
                        .frame(width: (major ? 3 : 1.4) * u, height: (major ? 12 : 5) * u)
                        .offset(y: -(major ? 82 : 83.5) * u)
                        .rotationEffect(.degrees(Double(i) * 6))
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
            let radius: Double = face == .double ? 46 : face == .chrono ? 70 : 66
            let size: CGFloat = face == .chrono ? 12 : face == .double ? 13 : face == .roman || face == .chinese ? 14 : 15
            switch face.numerals {
            case .none: EmptyView()
            default:
                ForEach(0..<12, id: \.self) { i in
                    let n = i == 0 ? 12 : i
                    let a = Double(n) * 30 * .pi / 180
                    let text = face.numerals == .roman ? DialFace.romanNumerals[i] : face.numerals == .chinese ? DialFace.chineseNumerals[i] : String(n)
                    // Kronografta alt kadranların üstüne düşen rakamlar (3, 6, 9) yazılmaz
                    if !(face == .chrono && [3, 6, 9].contains(n)) {
                        Text(verbatim: text)
                            .font(face.numerals == .roman ? .system(size: size * u, weight: .semibold, design: .serif) : .system(size: size * u, weight: .semibold))
                            .foregroundStyle(k.ink)
                            .offset(x: radius * u * sin(a), y: -radius * u * cos(a))
                    }
                }
            }
        }
    }
}

/// 0xRRGGBB → renk (widget hedefinde de derlenir; AnimatedBackground'daki Color(hex:) uygulamada)
func dc(_ v: UInt32) -> Color {
    Color(red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255)
}
