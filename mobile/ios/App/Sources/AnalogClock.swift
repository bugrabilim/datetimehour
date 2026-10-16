import SwiftUI

/// Rakamlı analog saat; sitedeki çizimin (build.mjs analogSvg, 200×200 birim) aynısı.
struct AnalogClock: View {
    let date: Date
    var showSeconds = false

    var body: some View {
        Canvas { ctx, size in
            let s = min(size.width, size.height) / 200
            ctx.translateBy(x: (size.width - 200 * s) / 2, y: (size.height - 200 * s) / 2)
            ctx.scaleBy(x: s, y: s)
            let center = CGPoint(x: 100, y: 100)

            let face = Path(ellipseIn: CGRect(x: 6, y: 6, width: 188, height: 188))
            ctx.fill(face, with: .color(Theme.surface2))
            ctx.stroke(face, with: .color(Theme.border), lineWidth: 2)

            for i in 0..<60 {
                let major = i % 5 == 0
                var c = ctx
                c.translateBy(x: 100, y: 100)
                c.rotate(by: .degrees(Double(i) * 6))
                var p = Path()
                p.move(to: CGPoint(x: 0, y: major ? -88 : -86))
                p.addLine(to: CGPoint(x: 0, y: major ? -76 : -81))
                c.stroke(p, with: .color(major ? Theme.text : Theme.muted),
                         style: StrokeStyle(lineWidth: major ? 3 : 1.4, lineCap: .round))
            }
            for n in 1...12 {
                let a = Double(n) * 30 * .pi / 180
                let text = Text(verbatim: String(n)).font(.system(size: 15, weight: .semibold)).foregroundColor(Theme.text)
                ctx.draw(text, at: CGPoint(x: 100 + 66 * sin(a), y: 100 - 66 * cos(a)))
            }

            let c = Calendar.current.dateComponents([.hour, .minute, .second, .nanosecond], from: date)
            let sec = Double(c.second ?? 0)
            let min = Double(c.minute ?? 0) + sec / 60
            let hour = Double((c.hour ?? 0) % 12) + min / 60
            hand(ctx, center, angle: hour * 30, from: 4, to: 44, width: 6, color: Theme.text)
            hand(ctx, center, angle: min * 6, from: 6, to: 66, width: 4.5, color: Theme.text)
            if showSeconds { hand(ctx, center, angle: sec * 6, from: 16, to: 76, width: 2, color: Theme.accent) }
            ctx.fill(Path(ellipseIn: CGRect(x: 95, y: 95, width: 10, height: 10)), with: .color(Theme.accent))
        }
        .aspectRatio(1, contentMode: .fit)
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityElement()
        .accessibilityLabel(date.formatted(date: .omitted, time: showSeconds ? .standard : .shortened))
    }

    /// Merkezden geriye `from`, ileriye `to` birim uzanan ibre
    private func hand(_ ctx: GraphicsContext, _ center: CGPoint, angle: Double, from: CGFloat, to: CGFloat,
                      width: CGFloat, color: Color) {
        var c = ctx
        c.translateBy(x: center.x, y: center.y)
        c.rotate(by: .degrees(angle))
        var p = Path()
        p.move(to: CGPoint(x: 0, y: from))
        p.addLine(to: CGPoint(x: 0, y: -to))
        c.stroke(p, with: .color(color), style: StrokeStyle(lineWidth: width, lineCap: .round))
    }
}
