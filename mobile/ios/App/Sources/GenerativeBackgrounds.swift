import SwiftUI
import UIKit

/// Üretken tam ekran zeminleri (2. analiz #23; 4 Kasım 2026): eş yükselti çizgileri, noktalı dünya haritası (karalar
/// Natural Earth dokusundan, gece yarısı sönük), grenli renk geçişi ve sıvı metal. Telefonda çizilir; Android'deki
/// GenerativeBackgrounds.kt ile aynı kurallar.
enum GenArt {
    static let cols = 120
    static let rows = 60

    /// Kara maskesi (120×60): dokuda mavi baskın değilse kara
    static let landMask: [Bool] = {
        guard let cg = WorldMath.earth?.cgImage else { return [] }
        let w = cols, h = rows
        var px = [UInt8](repeating: 0, count: w * h * 4)
        guard let ctx = CGContext(data: &px, width: w, height: h, bitsPerComponent: 8, bytesPerRow: w * 4,
                                  space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return [] }
        ctx.interpolationQuality = .medium
        ctx.draw(cg, in: CGRect(x: 0, y: 0, width: w, height: h))
        var out = [Bool](repeating: false, count: w * h)
        for i in 0..<(w * h) {
            let r = Int(px[i * 4]), g = Int(px[i * 4 + 1]), b = Int(px[i * 4 + 2])
            out[i] = !(b > r + 18 && b >= g)
        }
        return out
    }()

    private static func mix(_ a: (Double, Double, Double), _ b: (Double, Double, Double), _ f: Double) -> Color {
        Color(red: a.0 + (b.0 - a.0) * f, green: a.1 + (b.1 - a.1) * f, blue: a.2 + (b.2 - a.2) * f)
    }

    private static func rgb(_ c: Color) -> (Double, Double, Double) {
        let u = UIColor(c)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        u.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (Double(r), Double(g), Double(b))
    }

    static func topo(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double) {
        ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .linearGradient(
            Gradient(colors: [Color(hex: 0x0E1412), Color(hex: 0x16201C)]), startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height)))
        let centers: [(CGFloat, CGFloat)] = [(0.3, 0.3), (0.75, 0.55), (0.4, 0.85)]
        let big = max(size.width, size.height)
        let accent = Theme.accent
        for (ci, c) in centers.enumerated() {
            for k in 1...14 {
                let r0 = big * 0.035 * CGFloat(k)
                var path = Path()
                for i in 0...72 {
                    let a = 2 * Double.pi * Double(i) / 72
                    let w1 = sin(a * 3 + Double(k) * 0.5 + Double(ci) + t * 0.05) * 0.12
                    let w2 = sin(a * 5 - Double(k) * 0.3 + t * 0.03) * 0.06
                    let rr = r0 * CGFloat(1 + w1 + w2)
                    let p = CGPoint(x: c.0 * size.width + rr * CGFloat(cos(a)), y: c.1 * size.height + rr * CGFloat(sin(a)))
                    if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
                }
                path.closeSubpath()
                let major = k % 5 == 0
                ctx.stroke(path, with: .color(accent.opacity(major ? 0.35 : 0.16)), lineWidth: major ? 1.6 : 1)
            }
        }
    }

    static func dots(_ ctx: inout GraphicsContext, _ size: CGSize, date: Date) {
        ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .color(Color(hex: 0x070B14)))
        let mask = landMask
        guard mask.count == cols * rows else { return }
        let w = size.width, h = min(size.height, w / 2), top = (size.height - h) / 2
        let step = w / CGFloat(cols), r = step * 0.32
        let night = (0.149, 0.196, 0.302)
        let day = rgb(Theme.accent)
        for row in 0..<rows {
            for col in 0..<cols where mask[row * cols + col] {
                let lat = 90.0 - (Double(row) + 0.5) * 180.0 / Double(rows)
                let lon = -180.0 + (Double(col) + 0.5) * 360.0 / Double(cols)
                let alt = WorldMath.sunAltitude(lat: lat, lon: lon, at: date)
                let f = max(0, min(1, (alt + 6) / 12))
                let x = (CGFloat(col) + 0.5) * step, y = top + (CGFloat(row) + 0.5) * (h / CGFloat(rows))
                ctx.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: 2 * r, height: 2 * r)), with: .color(mix(night, day, f)))
            }
        }
    }

    static func grain(_ ctx: inout GraphicsContext, _ size: CGSize) {
        let base = (0.043, 0.043, 0.063)
        let c1 = mix(base, rgb(Theme.accent), 0.55), c2 = mix(base, rgb(Theme.accent2), 0.35)
        ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .linearGradient(
            Gradient(colors: [c1, c2, Color(hex: 0x0B0B10)]), startPoint: .zero, endPoint: CGPoint(x: size.width, y: size.height)))
        var seed: UInt64 = 0x6A1E
        func rnd() -> CGFloat {
            seed = seed &* 6364136223846793005 &+ 1442695040888963407
            return CGFloat(Double(seed >> 11) / Double(1 << 53))
        }
        let n = min(30000, Int(size.width * size.height / 6))
        for _ in 0..<n {
            let light = rnd() > 0.5
            let a = light ? 0.05 + Double(rnd()) * 0.05 : 0.08 + Double(rnd()) * 0.08
            let rect = CGRect(x: rnd() * size.width, y: rnd() * size.height, width: 1, height: 1)
            ctx.fill(Path(rect), with: .color(light ? Color.white.opacity(a) : Color.black.opacity(a)))
        }
    }

    static func liquid(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double) {
        ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .linearGradient(
            Gradient(colors: [Color(hex: 0x1A1C20), Color(hex: 0x2A2D33)]), startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height)))
        let big = max(size.width, size.height)
        let tones: [UInt32] = [0xE8EBF0, 0x9AA3B0, 0xC8CDD6, 0x6E7684, 0xF5F7FA]
        for (k, hex) in tones.enumerated() {
            let c = Color(hex: hex)
            let kd = Double(k)
            let p = CGPoint(x: size.width * CGFloat(0.5 + 0.38 * sin(t * 0.06 + kd * 1.3)),
                            y: size.height * CGFloat(0.5 + 0.38 * cos(t * 0.045 + kd * 2.1)))
            let rr = big * (0.22 + 0.06 * CGFloat(k))
            ctx.fill(Path(ellipseIn: CGRect(x: p.x - rr, y: p.y - rr, width: 2 * rr, height: 2 * rr)), with: .radialGradient(
                Gradient(colors: [c.opacity(0.55), c.opacity(0.18), c.opacity(0)]), center: p, startRadius: 0, endRadius: rr))
        }
    }
}
