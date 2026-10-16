import SwiftUI

/// Rakamlı analog saat; sitedeki çizimin (build.mjs analogSvg, 200×200 birim) aynısı.
/// Şekillerle çizilir (Canvas değil), böylece widget'ta da çalışır.
struct AnalogClock: View {
    let date: Date
    var showSeconds = false

    var body: some View {
        GeometryReader { g in
            let u = min(g.size.width, g.size.height) / 200
            let c = Calendar.current.dateComponents([.hour, .minute, .second], from: date)
            let sec = Double(c.second ?? 0)
            let min = Double(c.minute ?? 0) + sec / 60
            let hour = Double((c.hour ?? 0) % 12) + min / 60
            ZStack {
                Circle().fill(Theme.surface2)
                    .overlay(Circle().stroke(Theme.border, lineWidth: 2 * u))
                    .frame(width: 188 * u, height: 188 * u)
                ForEach(0..<60, id: \.self) { i in
                    let major = i % 5 == 0
                    Capsule()
                        .fill(major ? Theme.text : Theme.muted)
                        .frame(width: (major ? 3 : 1.4) * u, height: (major ? 12 : 5) * u)
                        .offset(y: -(major ? 82 : 83.5) * u)
                        .rotationEffect(.degrees(Double(i) * 6))
                }
                ForEach(1...12, id: \.self) { n in
                    let a = Double(n) * 30 * .pi / 180
                    Text(verbatim: String(n))
                        .font(.system(size: 15 * u, weight: .semibold))
                        .foregroundStyle(Theme.text)
                        .offset(x: 66 * u * sin(a), y: -66 * u * cos(a))
                }
                hand(angle: hour * 30, from: 4, to: 44, width: 6, color: Theme.text, u: u)
                hand(angle: min * 6, from: 6, to: 66, width: 4.5, color: Theme.text, u: u)
                if showSeconds { hand(angle: sec * 6, from: 16, to: 76, width: 2, color: Theme.accent, u: u) }
                Circle().fill(Theme.accent).frame(width: 10 * u, height: 10 * u)
            }
            .frame(width: 200 * u, height: 200 * u)
            .position(x: g.size.width / 2, y: g.size.height / 2)
        }
        .aspectRatio(1, contentMode: .fit)
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityElement()
        .accessibilityLabel(date.formatted(date: .omitted, time: showSeconds ? .standard : .shortened))
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
