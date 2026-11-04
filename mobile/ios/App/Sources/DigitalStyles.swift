import SwiftUI

/// Dijital rakam görünümleri (2. analiz #9; 4 Kasım 2026): yedi bölütlü LED (sönük bölütler de görünür), LCD taklidi,
/// nokta matris (5×7), silindir (önceki ve sonraki rakam soluk) ve dikey (saat, dakika, saniye alt alta).
/// Kayıt `digital.style`; Android'deki DigitalStyle ile aynı kimlikler ve tablolar.
enum DigitalStyle {
    static let key = "digital.style"
    static let all = ["plain", "led", "lcd", "matrix", "roll", "vertical"]
    /// a üst, b sağ üst, c sağ alt, d alt, e sol alt, f sol üst, g orta (bit 0..6)
    static let seg: [Int] = [0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F]
    static let dots: [[Int]] = [
        [0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E], [0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E],
        [0x0E, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1F], [0x1F, 0x02, 0x04, 0x02, 0x01, 0x11, 0x0E],
        [0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02], [0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E],
        [0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E], [0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08],
        [0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E], [0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C],
    ]

    static func width(_ ch: Character, _ style: String) -> CGFloat {
        if ch == ":" { return 0.28 }
        if ch == " " { return 0.25 }
        return style == "matrix" ? 0.62 : 0.56
    }

    static func naturalWidth(_ text: String, _ style: String, height: CGFloat) -> CGFloat {
        let sum: CGFloat = text.reduce(0) { $0 + width($1, style) }
        return sum * height + CGFloat(max(0, text.count - 1)) * height * 0.06
    }
}

/// Biçimli rakamlar ("12:34:56"); `height` yüksekliğinde, genişliğe sığmazsa küçülür
struct StyledDigits: View {
    let text: String
    let style: String
    let height: CGFloat
    let ink: Color

    var body: some View {
        let natural = DigitalStyle.naturalWidth(text, style, height: height)
        Canvas { ctx, size in
            let scale = min(1, size.width / max(1, natural))
            let h = height * scale
            let oy = (size.height - h) / 2
            if style == "lcd" {
                ctx.fill(Path(roundedRect: CGRect(x: 0, y: oy, width: natural * scale, height: h), cornerRadius: h * 0.08),
                         with: .color(Color(red: 0.725, green: 0.78, blue: 0.635)))
            }
            let lit: Color = style == "lcd" ? Color(red: 0.118, green: 0.165, blue: 0.094) : ink
            let ghost = lit.opacity(0.1)
            var x: CGFloat = 0
            for ch in text {
                let cw = DigitalStyle.width(ch, style) * h
                if ch == ":" {
                    let r = h * 0.045
                    ctx.fill(Path(ellipseIn: CGRect(x: x + cw / 2 - r, y: oy + h * 0.33 - r, width: 2 * r, height: 2 * r)), with: .color(lit))
                    ctx.fill(Path(ellipseIn: CGRect(x: x + cw / 2 - r, y: oy + h * 0.67 - r, width: 2 * r, height: 2 * r)), with: .color(lit))
                } else if let n = ch.wholeNumberValue {
                    let rect = CGRect(x: x, y: oy, width: cw, height: h)
                    switch style {
                    case "matrix": Self.matrix(&ctx, n, rect, lit, ghost)
                    case "roll": Self.roll(&ctx, n, rect, lit)
                    default: Self.segments(&ctx, n, rect, lit, ghost)
                    }
                }
                x += cw + h * 0.06
            }
        }
        .frame(height: height)
        .frame(maxWidth: natural)
        .accessibilityLabel(text)
    }

    private static func segments(_ ctx: inout GraphicsContext, _ n: Int, _ r: CGRect, _ on: Color, _ off: Color) {
        let bits = DigitalStyle.seg[n]
        let t = r.height * 0.1, inset = r.height * 0.04
        let x = r.minX + inset, y = r.minY + inset, ww = r.width - 2 * inset, hh = r.height - 2 * inset, half = hh / 2
        func hSeg(_ cx: CGFloat, _ cy: CGFloat, _ len: CGFloat) -> Path {
            var p = Path()
            p.move(to: CGPoint(x: cx, y: cy)); p.addLine(to: CGPoint(x: cx + t / 2, y: cy - t / 2))
            p.addLine(to: CGPoint(x: cx + len - t / 2, y: cy - t / 2)); p.addLine(to: CGPoint(x: cx + len, y: cy))
            p.addLine(to: CGPoint(x: cx + len - t / 2, y: cy + t / 2)); p.addLine(to: CGPoint(x: cx + t / 2, y: cy + t / 2))
            p.closeSubpath()
            return p
        }
        func vSeg(_ cx: CGFloat, _ cy: CGFloat, _ len: CGFloat) -> Path {
            var p = Path()
            p.move(to: CGPoint(x: cx, y: cy)); p.addLine(to: CGPoint(x: cx + t / 2, y: cy + t / 2))
            p.addLine(to: CGPoint(x: cx + t / 2, y: cy + len - t / 2)); p.addLine(to: CGPoint(x: cx, y: cy + len))
            p.addLine(to: CGPoint(x: cx - t / 2, y: cy + len - t / 2)); p.addLine(to: CGPoint(x: cx - t / 2, y: cy + t / 2))
            p.closeSubpath()
            return p
        }
        let segs: [Path] = [
            hSeg(x + t * 0.6, y + t / 2, ww - t * 1.2),
            vSeg(x + ww - t / 2, y + t * 0.6, half - t * 0.6),
            vSeg(x + ww - t / 2, y + half + t * 0.1, half - t * 0.6),
            hSeg(x + t * 0.6, y + hh - t / 2, ww - t * 1.2),
            vSeg(x + t / 2, y + half + t * 0.1, half - t * 0.6),
            vSeg(x + t / 2, y + t * 0.6, half - t * 0.6),
            hSeg(x + t * 0.6, y + half, ww - t * 1.2),
        ]
        for (i, p) in segs.enumerated() { ctx.fill(p, with: .color(bits & (1 << i) != 0 ? on : off)) }
    }

    private static func matrix(_ ctx: inout GraphicsContext, _ n: Int, _ r: CGRect, _ on: Color, _ off: Color) {
        let rows = DigitalStyle.dots[n]
        let step = min(r.width / 5, r.height / 7), rad = step * 0.38
        let ox = r.minX + (r.width - step * 5) / 2, oy = r.minY + (r.height - step * 7) / 2
        for row in 0..<7 {
            for col in 0..<5 {
                let lit = rows[row] & (1 << (4 - col)) != 0
                let c = CGPoint(x: ox + step * (CGFloat(col) + 0.5), y: oy + step * (CGFloat(row) + 0.5))
                ctx.fill(Path(ellipseIn: CGRect(x: c.x - rad, y: c.y - rad, width: 2 * rad, height: 2 * rad)), with: .color(lit ? on : off))
            }
        }
    }

    private static func roll(_ ctx: inout GraphicsContext, _ n: Int, _ r: CGRect, _ on: Color) {
        ctx.fill(Path(roundedRect: r, cornerRadius: r.width * 0.18), with: .color(on.opacity(0.06)))
        let font = Font.system(size: r.height * 0.62, weight: .bold, design: .rounded)
        let prev = ctx.resolve(Text("\((n + 9) % 10)").font(font).foregroundColor(on.opacity(0.22)))
        let cur = ctx.resolve(Text("\(n)").font(font).foregroundColor(on))
        let next = ctx.resolve(Text("\((n + 1) % 10)").font(font).foregroundColor(on.opacity(0.22)))
        ctx.draw(cur, at: CGPoint(x: r.midX, y: r.midY))
        var top = ctx
        top.translateBy(x: r.midX, y: r.minY + r.height * 0.08)
        top.scaleBy(x: 1, y: 0.45)
        top.draw(prev, at: .zero)
        var bottom = ctx
        bottom.translateBy(x: r.midX, y: r.minY + r.height * 0.92)
        bottom.scaleBy(x: 1, y: 0.45)
        bottom.draw(next, at: .zero)
    }
}

/// Dikey görünüm: saat, dakika (ve saniye) alt alta
struct VerticalDigits: View {
    let lines: [String]
    let height: CGFloat
    let ink: Color
    let font: ClockFont

    var body: some View {
        let each = height / CGFloat(max(1, lines.count))
        VStack(spacing: -each * 0.12) {
            ForEach(Array(lines.enumerated()), id: \.offset) { i, l in
                Text(l).font(font.font(each * 0.86)).monospacedDigit()
                    .foregroundStyle(i == 2 ? ink.opacity(0.6) : ink)
            }
        }
        .environment(\.layoutDirection, .leftToRight)
    }
}

/// Görünüm seçici (Ayarlar › Saat görünümü)
struct DigitalStylePicker: View {
    @AppStorage(DigitalStyle.key) private var style = "plain"
    @AppStorage("clockFont") private var clockFont = ClockFont.rounded.rawValue

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { ctx in
            let t = Fmt.time(ctx.date, seconds: true)
            Group {
                if style == "vertical" {
                    let parts = t.split(separator: ":").map(String.init)
                    VerticalDigits(lines: parts, height: 150, ink: Theme.ink, font: ClockFont(rawValue: clockFont) ?? .rounded)
                } else if style == "plain" {
                    Text(t).font(.system(size: 48, weight: .semibold, design: .rounded)).monospacedDigit().foregroundStyle(Theme.ink)
                } else {
                    StyledDigits(text: t.filter { $0.isNumber || $0 == ":" }, style: style, height: 64, ink: Theme.ink)
                }
            }
            .frame(maxWidth: .infinity)
            .environment(\.layoutDirection, .leftToRight)
        }
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(DigitalStyle.all, id: \.self) { id in
                    Button { style = id } label: {
                        Text(L("digital.style.\(id)"))
                            .font(.subheadline)
                            .padding(.horizontal, 14).padding(.vertical, 8)
                            .background(id == style ? Theme.accent : Theme.surface2, in: Capsule())
                            .foregroundStyle(id == style ? Color.white : Theme.text)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(4)
        }
        Text(L("digital.styleNote")).font(.footnote).foregroundStyle(Theme.muted)
    }
}
