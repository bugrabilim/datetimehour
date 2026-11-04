import SwiftUI
import SaatCore
import UIKit

// MARK: Fotoğraf dokulu Ay (2. analiz #103)

/// NASA SVS CGI Moon Kit dokusu (lroc_color_2k.jpg, kamu malı, 1024×512; Resources/moon.jpg). Her evre telefonda çizilir:
/// diskin her noktası için yüzey normali → selenografik boylam/enlem → doku pikseli; aydınlık Güneş yönüyle (evre kesrinden),
/// karanlık yarıda hafif yer ışığı; güney yarımkürede 180° döner. Sitedeki moon-render.js ile aynı kural. Widget'ta da kullanılır.
enum MoonTexture {
    nonisolated(unsafe) private static var pixels: [UInt8] = []
    nonisolated(unsafe) private static var width = 0
    nonisolated(unsafe) private static var height = 0
    nonisolated(unsafe) private static var cache: [String: CGImage] = [:]
    private static let lock = NSLock()

    private static func load() -> Bool {
        if !pixels.isEmpty { return true }
        guard let url = Bundle.main.url(forResource: "moon", withExtension: "jpg"),
              let data = try? Data(contentsOf: url),
              let src = UIImage(data: data)?.cgImage else { return false }
        let w = src.width, h = src.height
        var buf = [UInt8](repeating: 0, count: w * h * 4)
        let cs = CGColorSpaceCreateDeviceRGB()
        let ok: Bool = buf.withUnsafeMutableBytes { raw -> Bool in
            guard let ctx = CGContext(data: raw.baseAddress, width: w, height: h, bitsPerComponent: 8, bytesPerRow: w * 4, space: cs,
                                      bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return false }
            ctx.draw(src, in: CGRect(x: 0, y: 0, width: w, height: h))
            return true
        }
        if !ok { return false }
        pixels = buf; width = w; height = h
        return true
    }

    /// frac: evre kesri (0 yeni ay, 0,5 dolunay); south: güney yarımküre; size: piksel
    static func image(frac: Double, south: Bool, size: Int) -> CGImage? {
        lock.lock(); defer { lock.unlock() }
        guard load() else { return nil }
        let q = Int((frac * 200).rounded())
        let key = "\(q)-\(south)-\(size)"
        if let c = cache[key] { return c }
        let th = 2 * Double.pi * Double(q) / 200
        let sx = sin(th), sz = -cos(th)
        var out = [UInt8](repeating: 0, count: size * size * 4)
        let r = Double(size) / 2
        let W = width, H = height
        for py in 0..<size {
            for px in 0..<size {
                var x = (Double(px) + 0.5 - r) / r
                var y = (r - Double(py) - 0.5) / r
                let rr = x * x + y * y
                if rr > 1 { continue }
                if south { x = -x; y = -y }
                let z = (1 - rr).squareRoot()
                let lon = atan2(x, z), lat = asin(y)
                let u = min(W - 1, max(0, Int(((lon / Double.pi + 1) / 2) * Double(W))))
                let v = min(H - 1, max(0, Int((0.5 - lat / Double.pi) * Double(H))))
                let ti = (v * W + u) * 4
                let lit = x * sx + z * sz
                var k = lit > 0 ? min(1, 0.15 + lit * 1.1) : 0
                if lit > -0.04 && lit <= 0.04 { k = max(k, (lit + 0.04) / 0.08 * 0.2) }
                k = max(k, 0.07)
                let edge = rr > 0.96 ? max(0, (1 - rr) / 0.04) : 1
                let i = (py * size + px) * 4
                let a = edge
                out[i] = UInt8(Double(pixels[ti]) * k * a)
                out[i + 1] = UInt8(Double(pixels[ti + 1]) * k * a)
                out[i + 2] = UInt8(Double(pixels[ti + 2]) * k * a)
                out[i + 3] = UInt8(255 * a)
            }
        }
        let cs = CGColorSpaceCreateDeviceRGB()
        guard let provider = CGDataProvider(data: Data(out) as CFData),
              let img = CGImage(width: size, height: size, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: size * 4, space: cs,
                                bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue), provider: provider,
                                decode: nil, shouldInterpolate: true, intent: .defaultIntent) else { return nil }
        if cache.count > 64 { cache.removeAll() }
        cache[key] = img
        return img
    }
}

/// Dokulu Ay görseli; doku yoksa evre emojisi
struct MoonImage: View {
    let fraction: Double
    let size: CGFloat
    var south: Bool = false

    var body: some View {
        let px = Int(size * 3)
        if let img = MoonTexture.image(frac: fraction, south: south, size: max(16, min(512, px))) {
            Image(decorative: img, scale: 3).resizable().frame(width: size, height: size)
        } else {
            let idx = Int(fraction * 8 + 0.5) % 8
            Text(["🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"][idx]).font(.system(size: size * 0.8))
        }
    }
}

/// Sıradaki ana evreler (yeni ay, ilk dördün, dolunay, son dördün): evre kesrinin 0; 0,25; 0,5; 0,75'i geçtiği anlar
enum MoonPhases {
    static func next(from date: Date, count: Int = 4) -> [(key: String, date: Date)] {
        let keys = ["new", "firstQuarter", "full", "lastQuarter"]
        var out: [(key: String, date: Date)] = []
        var t = date.timeIntervalSince1970 * 1000
        var prev = Nature.moon(atMilliseconds: Int64(t)).fraction
        var guardN = 0
        while out.count < count && guardN < 24 * 60 {
            guardN += 1
            let t2 = t + 3_600_000
            let f = Nature.moon(atMilliseconds: Int64(t2)).fraction
            for (i, q) in [0.0, 0.25, 0.5, 0.75].enumerated() {
                let crossed: Bool = q == 0 ? f < prev : (prev < q && f >= q)
                if crossed { out.append((keys[i], Date(timeIntervalSince1970: t2 / 1000))) }
            }
            prev = f
            t = t2
        }
        return Array(out.prefix(count))
    }
}
