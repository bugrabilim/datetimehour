import SwiftUI

/// Saniye ibresinin hareketi (2. analiz #10): tık (saniyede bir atlar), mekanik (saniyede 8 adım, mekanik saat
/// gibi), kayma (akıcı). Seçim `secMotion`; tık dışında saat ekranı saniyede birden sık çizilir.
enum SecondsMotion: String, CaseIterable, Identifiable {
    case tick, mech, sweep
    static let key = "secMotion"
    var id: String { rawValue }
    /// Ekranın yenilenme aralığı (tıkta saniyede bir yeter)
    var interval: Double {
        switch self {
        case .tick: 1
        case .mech: 1.0 / 8
        case .sweep: 1.0 / 30
        }
    }

    /// Dakikanın içindeki saniye (kesirli)
    func seconds(_ date: Date, second: Int) -> Double {
        let t = date.timeIntervalSince1970
        let frac = t - t.rounded(.down)
        switch self {
        case .tick: return Double(second)
        case .mech: return Double(second) + (frac * 8).rounded(.down) / 8
        case .sweep: return Double(second) + frac
        }
    }
}

/// Saat çiziminin renkleri: uygulamada temadan, widget'ta seçilen görünümden (2. analiz #17)
struct ClockColors {
    var face: Color
    var border: Color
    var tick: Color
    var minorTick: Color
    var ink: Color
    var hand: Color
    var second: Color

    static var theme: ClockColors {
        ClockColors(face: Theme.surface2, border: Theme.border, tick: Theme.text, minorTick: Theme.muted,
                    ink: Theme.ink, hand: Theme.accent, second: Theme.accent2)
    }
}

/// Rakamlı analog saat; sitedeki çizimin (build.mjs analogSvg, 200×200 birim) aynısı.
/// Şekillerle çizilir (Canvas değil), böylece widget'ta da çalışır.
struct AnalogClock: View {
    let date: Date
    var showSeconds = false
    var motion: SecondsMotion = .tick
    var timeZone: TimeZone = .current
    var colors: ClockColors? = nil
    /// Kadranın içinde, altta küçük yazı (ikili saat dilimi kadranında şehir kodu ve fark)
    var caption: String? = nil
    /// Kadran (2. analiz #1): verilmezse seçili kadran; renk verilen widget görünümlerinde klasik
    var face: DialFace? = nil
    /// İbre rengi; verilmezse seçili renk
    var dialColor: DialColor? = nil

    var body: some View {
        GeometryReader { g in
            let u = min(g.size.width, g.size.height) / 200
            let f = face ?? (colors == nil ? DialFace.current : .classic)
            let k = colors ?? (dialColor ?? .current).apply(f.colors(theme: .theme))
            let c = calendar.dateComponents([.hour, .minute, .second], from: date)
            let sec = motion.seconds(date, second: c.second ?? 0)
            let min = Double(c.minute ?? 0) + sec / 60
            let hour = Double((c.hour ?? 0) % 12) + min / 60
            ZStack {
                DialFill(face: f, k: k, u: u, seconds: sec)
                DialMarks(face: f, k: k, u: u, hour: Double(c.hour ?? 0) + min / 60, minute: min, seconds: sec)
                if let caption {
                    Text(verbatim: caption)
                        .font(.system(size: 13 * u, weight: .bold, design: .rounded))
                        .foregroundStyle(k.minorTick)
                        .offset(y: 34 * u)
                }
                // Akrep yazı renginde, yelkovan temanın ana renginde, saniye ve orta temanın ikinci renginde.
                // Klasik kadranda ibreler sivrilir ve hafif gölgelidir, ortada altın kapak (doğal tasarım madde 9).
                if f == .classic {
                    taperedHand(angle: hour * 30, from: 6, to: 46, width: 7, color: k.ink, u: u)
                    taperedHand(angle: min * 6, from: 8, to: 68, width: 5.5, color: k.hand, u: u)
                } else {
                    hand(angle: hour * 30, from: 4, to: 44, width: 6, color: k.ink, u: u)
                    hand(angle: min * 6, from: 6, to: 66, width: 4.5, color: k.hand, u: u)
                }
                if showSeconds { hand(angle: sec * 6, from: 16, to: 76, width: 2, color: k.second, u: u) }
                if f == .classic {
                    Circle().fill(Color(red: 0.788, green: 0.647, blue: 0.29)).frame(width: 11 * u, height: 11 * u)
                    Circle().fill(k.ink).frame(width: 3.2 * u, height: 3.2 * u)
                } else {
                    Circle().fill(k.second).frame(width: 10 * u, height: 10 * u)
                }
            }
            .frame(width: 200 * u, height: 200 * u)
            .position(x: g.size.width / 2, y: g.size.height / 2)
        }
        .aspectRatio(1, contentMode: .fit)
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityElement()
        .accessibilityLabel((caption.map { $0 + ", " } ?? "") + date.formatted(Date.FormatStyle(date: .omitted, time: showSeconds ? .standard : .shortened, timeZone: timeZone)))
    }

    private var calendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        return cal
    }

    /// Sivrilen ibre: dipte `width`, uçta onun üçte biri, ucu yuvarlak; gölgesi hep aynı yöne düşer
    private func taperedHand(angle: Double, from: CGFloat, to: CGFloat, width: CGFloat, color: Color, u: CGFloat) -> some View {
        TaperedHand(tip: 0.32)
            .fill(color)
            .frame(width: width * u, height: (from + to) * u)
            .offset(y: (from - to) / 2 * u)
            .rotationEffect(.degrees(angle))
            .shadow(color: .black.opacity(0.13), radius: 1.2 * u, x: 1.2 * u, y: 2 * u)
    }

    /// Merkezden geriye `from`, ileriye `to` birim uzanan ibre
    private func hand(angle: Double, from: CGFloat, to: CGFloat, width: CGFloat, color: Color, u: CGFloat) -> some View {
        Capsule()
            .fill(color)
            .frame(width: width * u, height: (from + to) * u)
            .offset(y: (from - to) / 2 * u)
            .rotationEffect(.degrees(angle))
    }
}

/// Sivrilen ibre biçimi (dikey; üst uç dar ve yuvarlak)
struct TaperedHand: Shape {
    var tip: CGFloat

    func path(in r: CGRect) -> Path {
        var p = Path()
        let half = r.width * tip / 2
        p.move(to: CGPoint(x: r.minX, y: r.maxY))
        p.addLine(to: CGPoint(x: r.midX - half, y: r.minY + half))
        p.addQuadCurve(to: CGPoint(x: r.midX + half, y: r.minY + half), control: CGPoint(x: r.midX, y: r.minY - half))
        p.addLine(to: CGPoint(x: r.maxX, y: r.maxY))
        p.closeSubpath()
        return p
    }
}

/// İkili saat dilimi için kısa şehir kodu ve fark: "LON −3" (2. analiz #12)
enum ZoneCaption {
    /// Şehir anahtarından kısa kod (bilinen şehirlerde havalimanı/şehir kodu, yoksa ilk üç harf)
    static func code(_ key: String) -> String {
        let known = ["new-york": "NYC", "los-angeles": "LAX", "san-francisco": "SFO", "hong-kong": "HKG", "mexico-city": "MEX",
                     "sao-paulo": "SAO", "buenos-aires": "BUE", "kuala-lumpur": "KUL", "cape-town": "CPT", "tel-aviv": "TLV",
                     "abu-dhabi": "AUH", "new-delhi": "DEL", "mumbai": "BOM", "beijing": "BJS", "tokyo": "TYO", "seoul": "SEL"]
        if let k = known[key] { return k }
        return String(key.replacingOccurrences(of: "-", with: "").prefix(3)).uppercased()
    }

    /// İki dilim arasındaki fark: "+3", "−5:30", "±0"
    static func diff(_ zone: TimeZone, from base: TimeZone, at date: Date) -> String {
        let d = zone.secondsFromGMT(for: date) - base.secondsFromGMT(for: date)
        if d == 0 { return "±0" }
        let h = abs(d) / 3600, m = (abs(d) % 3600) / 60
        return (d > 0 ? "+" : "−") + (m == 0 ? "\(h)" : String(format: "%d:%02d", h, m))
    }
}
