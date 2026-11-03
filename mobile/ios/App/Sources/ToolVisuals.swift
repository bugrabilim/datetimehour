import SwiftUI

/* Araç görselleri (2. analiz #101, #140; 3 Kasım 2026): kalan süre halkası, pomodoro tur domatesleri, evre renkleri,
   rakamsız pasta zamanlayıcı ve kum saati, kilit (çocuk modu). Android'deki ToolVisuals.kt ile aynı çizim. */

enum PhaseColors {
    /// Aralık antrenmanında evre rengi: çalış yeşil, dinlen kırmızı, ısın turuncu, bitti mavi
    static func of(_ kind: String) -> Color {
        switch kind {
        case "work": return Color(red: 0.18, green: 0.62, blue: 0.36)
        case "rest": return Color(red: 0.85, green: 0.27, blue: 0.23)
        case "warmup": return Color(red: 0.88, green: 0.54, blue: 0.12)
        default: return Color(red: 0.16, green: 0.47, blue: 0.85)
        }
    }

    static func dark(_ kind: String) -> Color {
        switch kind {
        case "work": return Color(red: 0.12, green: 0.40, blue: 0.23)
        case "rest": return Color(red: 0.55, green: 0.17, blue: 0.15)
        case "warmup": return Color(red: 0.57, green: 0.35, blue: 0.08)
        default: return Color(red: 0.10, green: 0.31, blue: 0.55)
        }
    }
}

/// Kalan süre halkası: iz + ilerleme yayı (saat 12'den saat yönünde); içerik ortada
struct ProgressRing<Content: View>: View {
    let progress: Double
    let color: Color
    var lineWidth: CGFloat = 12
    @ViewBuilder let content: () -> Content

    var body: some View {
        ZStack {
            Circle().stroke(Theme.surface2, lineWidth: lineWidth)
            Circle().trim(from: 0, to: max(0, min(1, progress)))
                .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
            content().padding(lineWidth * 2)
        }
        .aspectRatio(1, contentMode: .fit)
    }
}

/// Pomodoro turları: biten turlar dolu domates, sürmekte olan yarı saydam, kalanlar soluk
struct RoundDots: View {
    let total: Int
    let done: Int
    let current: Int
    let label: String

    var body: some View {
        HStack(spacing: 6) {
            ForEach(1...max(1, total), id: \.self) { i in
                Text("🍅")
                    .font(.system(size: i == current ? 24 : 20))
                    .opacity(i <= done ? 1 : i == current ? 0.6 : 0.18)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
    }
}

/// Rakamsız pasta zamanlayıcı ("Time Timer" tarzı): kalan süre kırmızı dilim, 60 çentikli kadran
struct PieTimer: View {
    let left: Double
    var color = Color(red: 0.85, green: 0.27, blue: 0.23)

    var body: some View {
        Canvas { g, size in
            let r = min(size.width, size.height) / 2
            let c = CGPoint(x: size.width / 2, y: size.height / 2)
            let ink = Color(white: 0.2)
            g.fill(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)), with: .color(.white))
            g.stroke(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)), with: .color(ink), lineWidth: r * 0.03)
            let inner = r * 0.86
            var pie = Path()
            pie.move(to: c)
            pie.addArc(center: c, radius: inner, startAngle: .degrees(-90), endAngle: .degrees(-90 + 360 * max(0, min(1, left))), clockwise: false)
            pie.closeSubpath()
            g.fill(pie, with: .color(color))
            for i in 0..<60 {
                let a = (Double(i) * 6 - 90) * .pi / 180
                let l = i % 5 == 0 ? r * 0.12 : r * 0.05
                let o = r * 0.97
                var p = Path()
                p.move(to: CGPoint(x: c.x + cos(a) * o, y: c.y + sin(a) * o))
                p.addLine(to: CGPoint(x: c.x + cos(a) * (o - l), y: c.y + sin(a) * (o - l)))
                g.stroke(p, with: .color(ink), lineWidth: i % 5 == 0 ? r * 0.025 : r * 0.01)
            }
            g.fill(Path(ellipseIn: CGRect(x: c.x - r * 0.06, y: c.y - r * 0.06, width: r * 0.12, height: r * 0.12)), with: .color(ink))
        }
        .aspectRatio(1, contentMode: .fit)
        .accessibilityHidden(true)
    }
}

/// Kum saati: üst hazne kalan süreyle boşalır, alt hazne dolar, ortadan ince akış
struct Hourglass: View {
    let left: Double
    var color = Color(red: 0.88, green: 0.69, blue: 0.29)

    var body: some View {
        Canvas { g, size in
            let w = size.width, h = size.height
            let mid = h / 2, neck = w * 0.06, top = h * 0.06, bot = h * 0.94
            let glass = Color.gray.opacity(0.2)
            var upper = Path()
            upper.addLines([CGPoint(x: w * 0.1, y: top), CGPoint(x: w * 0.9, y: top), CGPoint(x: w / 2 + neck, y: mid), CGPoint(x: w / 2 - neck, y: mid)])
            upper.closeSubpath()
            var lower = Path()
            lower.addLines([CGPoint(x: w / 2 - neck, y: mid), CGPoint(x: w / 2 + neck, y: mid), CGPoint(x: w * 0.9, y: bot), CGPoint(x: w * 0.1, y: bot)])
            lower.closeSubpath()
            g.fill(upper, with: .color(glass))
            g.fill(lower, with: .color(glass))
            let f = max(0, min(1, left))
            if f > 0 {
                let hh = (mid - top) * sqrt(f)
                let y = mid - hh
                let half = neck + (w * 0.4 - neck) * (hh / (mid - top))
                var p = Path()
                p.addLines([CGPoint(x: w / 2 - half, y: y), CGPoint(x: w / 2 + half, y: y), CGPoint(x: w / 2 + neck, y: mid), CGPoint(x: w / 2 - neck, y: mid)])
                p.closeSubpath()
                g.fill(p, with: .color(color))
                var s = Path()
                s.move(to: CGPoint(x: w / 2, y: mid))
                s.addLine(to: CGPoint(x: w / 2, y: bot))
                g.stroke(s, with: .color(color), lineWidth: w * 0.015)
            }
            let gsand = 1 - f
            if gsand > 0 {
                let sq = sqrt(1 - gsand)
                let y = mid + (bot - mid) * sq
                let half = neck + (w * 0.4 - neck) * sq
                var p = Path()
                p.addLines([CGPoint(x: w * 0.1, y: bot), CGPoint(x: w * 0.9, y: bot), CGPoint(x: w / 2 + half, y: y), CGPoint(x: w / 2 - half, y: y)])
                p.closeSubpath()
                g.fill(p, with: .color(color))
            }
            let frame = Color(red: 0.42, green: 0.31, blue: 0.16)
            g.fill(Path(CGRect(x: 0, y: 0, width: w, height: top)), with: .color(frame))
            g.fill(Path(CGRect(x: 0, y: bot, width: w, height: h - bot)), with: .color(frame))
        }
        .aspectRatio(0.62, contentMode: .fit)
        .accessibilityHidden(true)
    }
}

/// Kilit (aralık antrenmanı ve çocuk modu): kilitliyken yalnız bu çip görünür, basılı tutunca açılır
struct HoldToUnlock: View {
    let onUnlock: () -> Void
    var dark = false

    var body: some View {
        Text("🔒 " + L("tools.holdToUnlock"))
            .font(.subheadline)
            .padding(.horizontal, 18).padding(.vertical, 10)
            .background(dark ? Color.white.opacity(0.2) : Theme.surface2, in: Capsule())
            .foregroundStyle(dark ? Color.white : Theme.text)
            .onLongPressGesture(minimumDuration: 0.8) { onUnlock() }
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { onUnlock() }
    }
}

/// Evre panosu: koyu evre rengi zemin, alttan evrenin ilerlemesiyle dolan açık ton, beyaz yazı
struct PhasePanel<Content: View>: View {
    let kind: String
    let progress: Double
    var height: CGFloat = 300
    @ViewBuilder let content: () -> Content

    var body: some View {
        ZStack {
            PhaseColors.dark(kind)
            GeometryReader { geo in
                PhaseColors.of(kind)
                    .frame(height: geo.size.height * max(0, min(1, progress)))
                    .frame(maxHeight: .infinity, alignment: .bottom)
            }
            content().foregroundStyle(.white)
        }
        .frame(height: height)
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}
