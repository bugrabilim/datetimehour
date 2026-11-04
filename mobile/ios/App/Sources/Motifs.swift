import SwiftUI

/*
 * Ölçülü süsler (doğal tasarım madde 15, 4 Kasım 2026; sahibin onayı): sekiz köşeli hatem yıldızı, mihrap kemeri ve ufukta
 * kubbe-minare silueti. Yalnız ibadet ekranlarında (vakit sayacı, Kur'an, dualar, kıble), %6–12 saydamlıkta. Android'deki
 * Motifs.kt ile aynı biçimler.
 */

/// Hatem: iki kare (biri 45° dönük), ortada halka ve nokta
struct HatemMotif: View {
    var color: Color
    var opacity: Double = 0.08

    var body: some View {
        GeometryReader { g in
            let r = min(g.size.width, g.size.height) / 2 * 0.96
            let w = r * 0.05
            let side = r * 1.4142
            ZStack {
                Rectangle().stroke(color, lineWidth: w).frame(width: side, height: side)
                Rectangle().stroke(color, lineWidth: w).frame(width: side, height: side).rotationEffect(.degrees(45))
                Circle().stroke(color, lineWidth: w).frame(width: r * 0.84, height: r * 0.84)
                Circle().fill(color).frame(width: r * 0.24, height: r * 0.24)
            }
            .frame(width: g.size.width, height: g.size.height)
        }
        .opacity(opacity)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

/// Sivri mihrap kemeri (alt kenarı açık)
struct ArchShape: Shape {
    func path(in r: CGRect) -> Path {
        var p = Path()
        let spring = r.minY + r.height * 0.42
        p.move(to: CGPoint(x: r.minX, y: r.maxY))
        p.addLine(to: CGPoint(x: r.minX, y: spring))
        p.addCurve(to: CGPoint(x: r.midX, y: r.minY), control1: CGPoint(x: r.minX, y: r.minY + r.height * 0.16),
                   control2: CGPoint(x: r.minX + r.width * 0.32, y: r.minY + r.height * 0.04))
        p.addCurve(to: CGPoint(x: r.maxX, y: spring), control1: CGPoint(x: r.minX + r.width * 0.68, y: r.minY + r.height * 0.04),
                   control2: CGPoint(x: r.maxX, y: r.minY + r.height * 0.16))
        p.addLine(to: CGPoint(x: r.maxX, y: r.maxY))
        return p
    }
}

/// Ufuk silueti: ortada kubbe, iki yanda ince minare, altta yer (dolu)
struct SkylineShape: Shape {
    func path(in r: CGRect) -> Path {
        var p = Path()
        let w = r.width, h = r.height, x0 = r.minX, ground = r.maxY
        let cx = x0 + w / 2
        let domeW = min(w * 0.28, h * 1.6), domeH = h * 0.55
        func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x, y: y) }
        p.move(to: pt(x0, ground))
        p.addLine(to: pt(x0, ground - h * 0.08))
        let m1 = cx - domeW * 0.95
        p.addLine(to: pt(m1 - h * 0.05, ground - h * 0.08)); p.addLine(to: pt(m1 - h * 0.05, ground - h * 0.78))
        p.addLine(to: pt(m1, ground - h * 0.95)); p.addLine(to: pt(m1 + h * 0.05, ground - h * 0.78)); p.addLine(to: pt(m1 + h * 0.05, ground - h * 0.22))
        p.addLine(to: pt(cx - domeW / 2, ground - h * 0.22))
        p.addCurve(to: pt(cx + domeW / 2, ground - h * 0.22), control1: pt(cx - domeW / 2, ground - h * 0.22 - domeH),
                   control2: pt(cx + domeW / 2, ground - h * 0.22 - domeH))
        let m2 = cx + domeW * 0.95
        p.addLine(to: pt(m2 - h * 0.05, ground - h * 0.22)); p.addLine(to: pt(m2 - h * 0.05, ground - h * 0.78))
        p.addLine(to: pt(m2, ground - h * 0.95)); p.addLine(to: pt(m2 + h * 0.05, ground - h * 0.78)); p.addLine(to: pt(m2 + h * 0.05, ground - h * 0.08))
        p.addLine(to: pt(x0 + w, ground - h * 0.08)); p.addLine(to: pt(x0 + w, ground))
        p.closeSubpath()
        return p
    }
}

/// Başlık şeridi (Kur'an, dualar): ortada mihrap kemeri, içinde küçük hatem, iki yanda ince çizgi
struct ArchBanner: View {
    var color: Color = Theme.gold
    var opacity: Double = 0.22

    var body: some View {
        GeometryReader { g in
            let h = g.size.height, w = h * 0.75
            ZStack {
                ArchShape().stroke(color, lineWidth: h * 0.035).frame(width: w, height: h)
                HatemMotif(color: color, opacity: 1).frame(width: h * 0.4, height: h * 0.4).offset(y: h * 0.08)
                HStack(spacing: w + h * 0.2) {
                    Rectangle().fill(color).frame(height: h * 0.02)
                    Rectangle().fill(color).frame(height: h * 0.02)
                }
                .offset(y: h * 0.45)
            }
            .frame(width: g.size.width, height: h)
        }
        .opacity(opacity)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
