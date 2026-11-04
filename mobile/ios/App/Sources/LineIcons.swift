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
