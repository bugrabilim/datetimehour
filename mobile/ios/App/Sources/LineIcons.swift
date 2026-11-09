import SwiftUI

/// Saat Tarih'in kendi çizgi simgeleri (2. analiz #97; 4 Kasım 2026): 24 simge, 24×24 alan, 2 pt çizgi, ana renk +
/// ikinci renk; temaya göre renk değişir. Tanımlar mobile/shared/icons/icons.json (gen-strings kopyalar; Android'deki
/// LineIcons.kt aynı dosyayı okur). Komutlar: M L C Z ve O (daire).
enum LineIcons {
    struct Part {
        let second: Bool
        let fill: Bool
        let d: String
    }

    static let all: [String: [Part]] = {
        guard let url = Bundle.main.url(forResource: "icons", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let raw = try? JSONSerialization.jsonObject(with: data) as? [String: [[String]]] else { return [:] }
        var out: [String: [Part]] = [:]
        for (k, parts) in raw {
            out[k] = parts.compactMap { p in
                guard p.count == 2 else { return nil }
                return Part(second: p[0].hasPrefix("c2"), fill: p[0].hasSuffix("f"), d: p[1])
            }
        }
        return out
    }()

    static let seasons = ["flower", "sun", "leaf", "snowman"]
    static let moods = ["mood1", "mood2", "mood3", "mood4", "mood5"]

    /// Yol metni → çizgi yolu ve daireler
    static func parse(_ d: String) -> (Path, [CGRect]) {
        var spaced = ""
        for ch in d {
            if "MLCZO".contains(ch) { spaced += " \(ch) " } else if ch == "," { spaced += " " } else { spaced.append(ch) }
        }
        let tok = spaced.split(whereSeparator: { $0 == " " }).map(String.init)
        var path = Path()
        var circles: [CGRect] = []
        var i = 0
        func num() -> CGFloat {
            let v = i < tok.count ? (Double(tok[i]) ?? 0) : 0
            i += 1
            return CGFloat(v)
        }
        func numeric() -> Bool { i < tok.count && Double(tok[i]) != nil }
        while i < tok.count {
            let c = tok[i]
            i += 1
            switch c {
            case "M":
                let x = num(), y = num()
                path.move(to: CGPoint(x: x, y: y))
            case "L":
                repeat {
                    let x = num(), y = num()
                    path.addLine(to: CGPoint(x: x, y: y))
                } while numeric()
            case "C":
                let x1 = num(), y1 = num(), x2 = num(), y2 = num(), x = num(), y = num()
                path.addCurve(to: CGPoint(x: x, y: y), control1: CGPoint(x: x1, y: y1), control2: CGPoint(x: x2, y: y2))
            case "Z":
                path.closeSubpath()
            case "O":
                while numeric() {
                    let cx = num(), cy = num(), r = num()
                    circles.append(CGRect(x: cx - r, y: cy - r, width: 2 * r, height: 2 * r))
                }
            default:
                break
            }
        }
        return (path, circles)
    }
}

/// Uygulamanın simgesi: 3D görsel (Icons3D, 4 Kasım 2026); görseli olmayan ad için eski çizgi çizimi. `c1`/`c2`
/// yalnız çizgi çizimde kullanılır; `dim`: gri ve soluk.
struct LineIcon: View {
    let name: String
    var size: CGFloat = 28
    var c1: Color = Theme.accent
    var c2: Color = Theme.accent2
    var dim = false

    var body: some View {
        if Icons3D.has(name) {
            Icon3D(name: name, size: size, dim: dim)
        } else if LineIcons.all[name] == nil, DuoIcons.has(name) {
            // Ne 3D ne çizgi çizimi olan ad (privacy, language…): iki tonlu çizilir, hiçbir simge kaybolmasın
            DuoIcon(name: name, size: size, color: dim ? Theme.muted : c1)
        } else {
            lines
        }
    }

    private var lines: some View {
        Canvas { ctx, sz in
            let k = min(sz.width, sz.height) / 24
            ctx.scaleBy(x: k, y: k)
            for p in LineIcons.all[name] ?? [] {
                let color = p.second ? c2 : c1
                let (path, circles) = LineIcons.parse(p.d)
                var all = path
                for r in circles { all.addEllipse(in: r) }
                if p.fill {
                    ctx.fill(all, with: .color(color))
                } else {
                    ctx.stroke(all, with: .color(color), style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
                }
            }
        }
        .frame(width: size, height: size)
    }
}

/// İki tonlu küçük simgeler (8 Kasım 2026, sahibin seçimi "B Karma"; mobile/shared/icons/DUOTONE.md): Phosphor duotone
/// (MIT), 24×24 alan, her ad iki katman (önce %20 saydam arka katman, sonra ön katman), ikisi de aynı renkle doldurulur
/// (nonzero). 3D yalnız hava verisinde, altı vakit satırında, büyük çizimlerde ve boş durumlarda kalır; alt çubuk, Tümü
/// kareleri, ayar/izin/bildirim satırları gibi bütün küçük simgeler iki tonludur. Veri App/Resources/duotone.json
/// (gen-strings kopyalar; yalnız uygulama hedefinde, widget ve saat hedeflerinde yok). Android'deki DuoIcons ile aynı adlar.
enum DuoIcons {
    struct Layer {
        let opacity: Double
        let path: Path
    }

    /// Bütün simgeler: dosya bir kez okunur, yollar bir kez ayrıştırılır (yollar uzun; her karede yeniden ayrıştırılmaz)
    static let all: [String: [Layer]] = {
        guard let url = Bundle.main.url(forResource: "duotone", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let raw = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [:] }
        var out: [String: [Layer]] = [:]
        for (k, v) in raw {
            guard let layers = v as? [[String: Any]] else { continue }
            let parsed: [Layer] = layers.compactMap { l in
                guard let d = l["d"] as? String else { return nil }
                let o = (l["o"] as? NSNumber)?.doubleValue ?? 1
                return Layer(opacity: o, path: LineIcons.parse(d).0)
            }
            if !parsed.isEmpty { out[k] = parsed }
        }
        return out
    }()

    /// Bu adla iki tonlu simge var mı
    static func has(_ name: String) -> Bool { all[name] != nil }
}

/// İki tonlu simge: katmanlar aynı renkle, katmanın saydamlığıyla doldurulur. Ad duotone.json'da yoksa eski LineIcon
/// (3D ya da çizgi) çizilir. Ekran okuyucudan gizli (yanındaki yazı okunur).
struct DuoIcon: View {
    let name: String
    var size: CGFloat = 24
    var color: Color = Theme.accent

    var body: some View {
        if let layers = DuoIcons.all[name] {
            Canvas { ctx, sz in
                let k = min(sz.width, sz.height) / 24
                ctx.scaleBy(x: k, y: k)
                for layer in layers {
                    ctx.fill(layer.path, with: .color(color.opacity(layer.opacity)))
                }
            }
            .frame(width: size, height: size)
            .accessibilityHidden(true)
        } else {
            LineIcon(name: name, size: size, c1: color, c2: color)
                .accessibilityHidden(true)
        }
    }
}

/// Dairede iki tonlu simge (eski çakıl taşı kutunun yerine): zemin, simge renginin açık tonu (açıkta %14, koyuda %22)
/// tema yüzeyinin üstünde. Ölçüler: satır 32/19, kare 42/25 (DUOTONE.md).
struct DuoChip: View {
    let name: String
    var color: Color = Theme.accent
    var box: CGFloat = 32
    var glyph: CGFloat = 19
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        ZStack {
            Circle().fill(Theme.surface)
            Circle().fill(color.opacity(scheme == .dark ? 0.22 : 0.14))
            DuoIcon(name: name, size: glyph, color: color)
        }
        .frame(width: box, height: box)
        .accessibilityHidden(true)
    }
}

/// SF Symbol'den kendi çizgi simgemize eşleme (doğal tasarım madde 4, 4 Kasım 2026): IconBox ve alt çubuk önce buraya
/// bakar, karşılığı yoksa SF Symbol çizilir. Android'deki LineIconMap ile aynı simgeler.
enum LineIconMap {
    private static let bySymbol: [String: String] = [
        "alarm": "alarm", "timer": "hourglass", "stopwatch": "stopwatch", "graduationcap": "cap", "bed.double": "bed",
        "bird": "chick", "wind": "wind", "checkerboard.rectangle": "knight", "metronome": "metronome", "speaker.wave.2": "speaker",
        "pencil.and.list.clipboard": "paper", "person.3": "people", "person.2.wave.2": "people", "clock.badge.questionmark": "calc",
        "calendar": "calendar", "calendar.badge.clock": "calendar", "calendar.badge.exclamationmark": "calendar", "doc.text": "leafcal",
        "sun.horizon": "sunrise", "beach.umbrella": "beach", "cloud.sun": "sun", "sun.max": "sun", "globe.europe.africa": "globe",
        "globe": "globe", "leaf": "leaf", "moon": "moon", "moon.stars": "night", "sparkles": "sparkles", "rosette": "medal", "gift": "gift",
        "location.north.line": "compass", "book": "book", "hand.tap": "beads", "checkmark.circle": "checklist", "lock.shield": "privacy", "hands.sparkles": "hands",
        "figure.mind.and.body": "mosque", "percent": "coin", "gearshape": "gear", "bell": "bell", "location": "pin",
        "paintpalette": "palette", "textformat.size": "textsize", "info.circle": "info", "qrcode": "qr", "slider.horizontal.3": "sliders",
        "icloud": "backup", "square.grid.3x3": "grid", "square.grid.2x2": "grid", "clock": "clock", "magnifyingglass": "search",
        "envelope": "mail", "square.and.arrow.up": "share", "star": "star", "heart": "heart",
        "arrow.up.left.and.arrow.down.right": "fullscreen", "thermometer.medium": "thermometer",
        // 3D simgelerle eklenenler (4 Kasım 2026)
        "wand.and.stars": "wand", "textformat.123": "numbers", "textformat": "textsize", "sunrise": "sunrise",
        "stopwatch.fill": "stopwatch", "square.and.pencil": "memo", "ruler": "ruler", "quote.opening": "speech",
        "quote.bubble": "speech", "moon.haze": "night", "mappin.and.ellipse": "pin", "list.bullet.rectangle": "clipboard",
        "list.bullet": "clipboard", "gauge": "gauge", "doc.on.doc": "paper", "circle.lefthalf.filled": "halfmoon",
        "cup.and.saucer": "coffee", "party.popper": "party", "lock.iphone": "qr",
        // İki tonlu simgelerle eklenenler (8 Kasım 2026, DUOTONE.md); çağrı yerleri adı ayrıca açıkça verir
        "location.north.circle": "compass", "checklist": "checklist", "text.quote": "speech", "number": "numbers",
        "battery.75percent": "battery", "cloud.rain": "umbrella", "clock.arrow.2.circlepath": "dst",
        "building.columns": "mosque", "moon.circle": "moon",
    ]

    static func of(_ symbol: String) -> String? {
        guard let n = bySymbol[symbol], Icons3D.has(n) || LineIcons.all[n] != nil || DuoIcons.has(n) else { return nil }
        return n
    }

    /// Alt çubuktaki sekmeler
    static func tab(_ t: AppTab) -> String {
        switch t {
        case .clock: "clock"
        case .weather: "sun_cloud"
        case .calendar: "calendar"
        case .world: "globe"
        case .seasons: "leaf"
        case .prayer: "mosque"
        case .tools: "grid"
        }
    }

    /// Sekmenin konu rengi (DUOTONE.md): Saat kiremit, Takvim adaçayı, Hava/Dünya/Mevsimler mavi, Vakitler altın, Tümü mor
    static func tabColor(_ t: AppTab) -> Color {
        switch t {
        case .clock: Theme.terra
        case .calendar: Theme.sage
        case .weather, .world, .seasons: Theme.sky
        case .prayer: Theme.gold
        case .tools: Theme.dusk
        }
    }

    /// Tümü girişleri (aynı SF Symbol'ü paylaşan girişler ayrışsın diye kimliğe göre)
    static func entry(_ id: String) -> String? {
        let m: [String: String] = [
            "alarm": "alarm", "timer": "hourglass", "stopwatch": "stopwatch", "pomodoro": "tomato", "study": "books",
            "sleep": "bed", "wakeGame": "chick", "breath": "wind", "chess": "knight", "metronome": "metronome",
            "talking": "speaker", "exam": "paper", "classroom": "school", "timecalc": "calc", "calendar": "calendar",
            "leaf": "leafcal", "datecalc": "date", "planner": "people", "journal": "notebook", "bridge": "bridge",
            "retire": "beach", "weather": "sun_cloud", "world": "globe", "seasons": "leaf", "moon": "moon",
            "sky": "telescope", "sunpath": "sunset", "zodiac": "milkyway", "prayer": "mosque", "qibla": "compass",
            "quran": "book", "zikir": "beads", "track": "checklist", "duas": "hands", "esma": "sparkles",
            "hadith": "scroll", "guide": "guide", "reldays": "star_crescent", "greet": "love_letter", "zakat": "coin",
            "settings": "gear", "badges": "medal",
            // Ayar arama girdileri (`s:` öneki atılır)
            "appearance": "clock", "color": "palette", "dials": "dial", "widgets": "widgets", "homeItems": "homeitems",
            "features": "features", "notifications": "bell", "language": "language", "about": "info", "shareQr": "qr",
        ]
        let key = id.hasPrefix("s:") ? String(id.dropFirst(2)) : id
        guard let n = m[key], Icons3D.has(n) || LineIcons.all[n] != nil || DuoIcons.has(n) else { return nil }
        return n
    }
}
