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

/// Çizgi simge
struct LineIcon: View {
    let name: String
    var size: CGFloat = 28
    var c1: Color = Theme.accent
    var c2: Color = Theme.accent2

    var body: some View {
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

/// SF Symbol'den kendi çizgi simgemize eşleme (doğal tasarım madde 4, 4 Kasım 2026): IconBox ve alt çubuk önce buraya
/// bakar, karşılığı yoksa SF Symbol çizilir. Android'deki LineIconMap ile aynı simgeler.
enum LineIconMap {
    private static let bySymbol: [String: String] = [
        "alarm": "alarm", "timer": "hourglass", "stopwatch": "stopwatch", "graduationcap": "cap", "bed.double": "bed",
        "bird": "chick", "wind": "wind", "checkerboard.rectangle": "knight", "metronome": "metronome", "speaker.wave.2": "speaker",
        "pencil.and.list.clipboard": "paper", "person.3": "people", "person.2.wave.2": "people", "clock.badge.questionmark": "calc",
        "calendar": "calendar", "calendar.badge.clock": "calendar", "calendar.badge.exclamationmark": "calendar", "doc.text": "leafcal",
        "sun.horizon": "sunrise", "beach.umbrella": "beach", "cloud.sun": "sun", "sun.max": "sun", "globe.europe.africa": "globe",
        "globe": "globe", "leaf": "leaf", "moon": "moon", "moon.stars": "night", "sparkles": "star", "rosette": "star", "gift": "heart",
        "location.north.line": "compass", "book": "book", "hand.tap": "beads", "checkmark.circle": "checklist", "hands.sparkles": "hands",
        "figure.mind.and.body": "mosque", "percent": "coin", "gearshape": "gear", "bell": "bell", "location": "pin",
        "paintpalette": "palette", "textformat.size": "textsize", "info.circle": "info", "qrcode": "qr", "slider.horizontal.3": "sliders",
        "icloud": "cloud", "square.grid.3x3": "grid", "square.grid.2x2": "grid", "clock": "clock", "magnifyingglass": "search",
        "envelope": "mail", "square.and.arrow.up": "share", "star": "star", "heart": "heart",
        "arrow.up.left.and.arrow.down.right": "fullscreen", "thermometer.medium": "thermometer",
    ]

    static func of(_ symbol: String) -> String? {
        guard let n = bySymbol[symbol], LineIcons.all[n] != nil else { return nil }
        return n
    }

    /// Alt çubuktaki sekmeler
    static func tab(_ t: AppTab) -> String {
        switch t {
        case .clock: "clock"
        case .weather: "sun"
        case .calendar: "calendar"
        case .world: "globe"
        case .seasons: "leaf"
        case .prayer: "mosque"
        case .tools: "grid"
        }
    }

    /// Tümü girişleri (aynı SF Symbol'ü paylaşan girişler ayrışsın diye kimliğe göre)
    static func entry(_ id: String) -> String? {
        let m: [String: String] = [
            "alarm": "alarm", "timer": "hourglass", "stopwatch": "stopwatch", "pomodoro": "tomato", "study": "book",
            "sleep": "bed", "wakeGame": "chick", "breath": "wind", "chess": "knight", "metronome": "metronome",
            "talking": "speaker", "exam": "paper", "classroom": "people", "timecalc": "calc", "calendar": "calendar",
            "leaf": "leafcal", "datecalc": "calc", "planner": "people", "journal": "sunrise", "bridge": "bridge",
            "retire": "beach", "weather": "sun", "world": "globe", "seasons": "leaf", "moon": "moon", "sky": "star",
            "sunpath": "sunset", "zodiac": "star", "prayer": "mosque", "qibla": "compass", "quran": "book",
            "zikir": "beads", "track": "checklist", "duas": "hands", "esma": "star", "hadith": "book", "guide": "mosque",
            "reldays": "calendar", "greet": "heart", "zakat": "coin", "settings": "gear", "badges": "star",
        ]
        let key = id.hasPrefix("s:") ? String(id.dropFirst(2)) : id
        guard let n = m[key], LineIcons.all[n] != nil else { return nil }
        return n
    }
}
