import AppIntents
import SwiftUI
import UIKit
import WidgetKit

/// Şeffaf widget (2. analiz #76): widget'ın zemini, ana ekran duvar kâğıdının widget'ın arkasına denk gelen parçasıdır,
/// böylece widget şeffaf görünür. Kişi ana ekranın boş bir sayfasının ekran görüntüsünü uygulamada seçer; uygulama
/// her widget yeri için parçayı keser ve App Group klasörüne yazar (`group.tr.bumba.time`), widget oradan okur.
/// Uygulama ve widget hedeflerinde ortak dosya. Görüntü yalnız telefonda kalır.
enum WidgetSpot: String, AppEnum, CaseIterable {
    case topLeft, topRight, middleLeft, middleRight, bottomLeft, bottomRight

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "widget.spot"
    static let caseDisplayRepresentations: [WidgetSpot: DisplayRepresentation] = [
        .topLeft: "widget.spot.topLeft", .topRight: "widget.spot.topRight",
        .middleLeft: "widget.spot.middleLeft", .middleRight: "widget.spot.middleRight",
        .bottomLeft: "widget.spot.bottomLeft", .bottomRight: "widget.spot.bottomRight",
    ]

    /// Satır: 0 üst, 1 orta, 2 alt
    var row: Int {
        switch self {
        case .topLeft, .topRight: 0
        case .middleLeft, .middleRight: 1
        case .bottomLeft, .bottomRight: 2
        }
    }
    var isRight: Bool { self == .topRight || self == .middleRight || self == .bottomRight }
}

enum WallpaperStore {
    static let group = "group.tr.bumba.time"

    static var dir: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group)?
            .appendingPathComponent("wallpaper", isDirectory: true)
    }

    /// Kesilen parçaların bilgisi: her yerin parlaklığı (yazı rengi için), yazı rengi seçimi, ince ayar (piksel)
    struct Meta: Codable {
        var tones: [String: Double] = [:]
        var text = "auto"  // auto | light | dark
        var dx = 0
        var dy = 0
        var estimated = false
        var width = 0
        var height = 0
    }

    static func meta() -> Meta? {
        guard let u = dir?.appendingPathComponent("meta.json"), let d = try? Data(contentsOf: u) else { return nil }
        return try? JSONDecoder().decode(Meta.self, from: d)
    }

    /// Widget boyutu ve yerine göre dosya adı. Geniş widget satırın tamamını, büyük widget üst ya da alt yarıyı kaplar.
    static func fileName(_ family: WidgetFamily, _ spot: WidgetSpot) -> String? {
        switch family {
        case .systemSmall: "small-\(spot.rawValue).jpg"
        case .systemMedium: "medium-\(spot.row).jpg"
        case .systemLarge: "large-\(spot.row == 0 ? 0 : 1).jpg"
        default: nil
        }
    }

    static func image(_ family: WidgetFamily, _ spot: WidgetSpot) -> UIImage? {
        guard let name = fileName(family, spot), let u = dir?.appendingPathComponent(name) else { return nil }
        return UIImage(contentsOfFile: u.path)
    }

    /// Açık zeminde koyu, koyu zeminde açık yazı (ya da kişinin seçtiği)
    static func darkText(_ spot: WidgetSpot) -> Bool {
        guard let m = meta() else { return false }
        switch m.text {
        case "light": return false
        case "dark": return true
        default: return (m.tones[spot.rawValue] ?? 0) > 0.6
        }
    }

    static func textColor(_ spot: WidgetSpot) -> Color { darkText(spot) ? Color(white: 0.08) : .white }

    static func clockColors(_ spot: WidgetSpot) -> ClockColors {
        let t = textColor(spot)
        return ClockColors(face: t.opacity(0.10), border: t.opacity(0.35), tick: t, minorTick: t.opacity(0.6),
                           ink: t, hand: t, second: Color(red: 0.95, green: 0.42, blue: 0.2))
    }

    // MARK: Kesme (yalnız uygulama kullanır)

    /// Ana ekrandaki widget yerleri (dikey ekran görüntüsünde piksel): kare kenarı, geniş ve büyük widget boyu,
    /// sol/sağ sütun x, üst/orta/alt satır y. Kaynak: topluluğun ölçtüğü tablolar (Scriptable "transparent widget"
    /// betikleri); 2622 ve 2868 (iPhone 16 Pro / Pro Max ve sonrası) komşu ekrandan ölçeklenmiş tahmin. Kenarlar
    /// tutmazsa uygulamadaki ince ayar kullanılır.
    struct Layout {
        let small, medium, large, left, right, top, middle, bottom: Double
    }

    static let layouts: [Int: (width: Int, layout: Layout)] = [
        2868: (1320, Layout(small: 522, medium: 1117, large: 1176, left: 101, right: 697, top: 289, middle: 942, bottom: 1594)),
        2796: (1290, Layout(small: 510, medium: 1092, large: 1146, left: 99, right: 681, top: 282, middle: 918, bottom: 1554)),
        2778: (1284, Layout(small: 510, medium: 1092, large: 1146, left: 96, right: 678, top: 246, middle: 882, bottom: 1518)),
        2688: (1242, Layout(small: 507, medium: 1080, large: 1137, left: 81, right: 654, top: 228, middle: 858, bottom: 1488)),
        2622: (1206, Layout(small: 485, medium: 1040, large: 1089, left: 84, right: 639, top: 277, middle: 880, bottom: 1483)),
        2556: (1179, Layout(small: 474, medium: 1017, large: 1062, left: 82, right: 625, top: 270, middle: 858, bottom: 1446)),
        2532: (1170, Layout(small: 474, medium: 1014, large: 1062, left: 78, right: 618, top: 231, middle: 819, bottom: 1407)),
        2436: (1125, Layout(small: 465, medium: 987, large: 1035, left: 69, right: 591, top: 213, middle: 783, bottom: 1353)),
        2208: (1242, Layout(small: 471, medium: 1044, large: 1071, left: 99, right: 672, top: 114, middle: 696, bottom: 1278)),
        1792: (828, Layout(small: 338, medium: 720, large: 758, left: 55, right: 437, top: 159, middle: 579, bottom: 999)),
        1334: (750, Layout(small: 296, medium: 642, large: 648, left: 54, right: 400, top: 60, middle: 412, bottom: 764)),
        1136: (640, Layout(small: 282, medium: 584, large: 622, left: 30, right: 332, top: 59, middle: 399, bottom: 399)),
    ]
    static let estimatedHeights: Set<Int> = [2622, 2868]

    /// 12 mini ve 13 mini aynı ekran boyunda ama satırlar 18 piksel aşağıda
    private static var isMini: Bool {
        var info = utsname()
        uname(&info)
        let id = withUnsafeBytes(of: &info.machine) { bytes in String(decoding: bytes.prefix { byte in byte != 0 }, as: UTF8.self) }
        return id == "iPhone13,1" || id == "iPhone14,4"
    }

    /// Ekran görüntüsünün boyutuna göre yerleşim; tabloda yoksa en yakın oranlı ekrandan ölçeklenir (tahmin)
    static func layout(width w: Int, height h: Int) -> (Layout, estimated: Bool) {
        if let e = layouts[h], e.width == w {
            let l = e.layout
            if h == 2436 && isMini {
                return (Layout(small: l.small, medium: l.medium, large: l.large, left: l.left, right: l.right,
                               top: l.top + 18, middle: l.middle + 18, bottom: l.bottom + 18), false)
            }
            return (l, estimatedHeights.contains(h))
        }
        let ratio = Double(h) / Double(w)
        let best = layouts.min { abs(Double($0.key) / Double($0.value.width) - ratio) < abs(Double($1.key) / Double($1.value.width) - ratio) }!
        let sx = Double(w) / Double(best.value.width), sy = Double(h) / Double(best.key)
        let l = best.value.layout
        return (Layout(small: l.small * sx, medium: l.medium * sx, large: l.large * sy, left: l.left * sx, right: l.right * sx,
                       top: l.top * sy, middle: l.middle * sy, bottom: l.bottom * sy), true)
    }

    static func rect(_ l: Layout, _ family: WidgetFamily, _ spot: WidgetSpot, dx: Double, dy: Double) -> CGRect {
        let y = [l.top, l.middle, l.bottom][spot.row]
        switch family {
        case .systemMedium: return CGRect(x: l.left + dx, y: y + dy, width: l.medium, height: l.small)
        case .systemLarge: return CGRect(x: l.left + dx, y: (spot.row == 0 ? l.top : l.middle) + dy, width: l.medium, height: l.large)
        default: return CGRect(x: (spot.isRight ? l.right : l.left) + dx, y: y + dy, width: l.small, height: l.small)
        }
    }

    enum BuildResult { case ok(estimated: Bool), wrongSize, unavailable }

    /// Ekran görüntüsünü kaydeder ve bütün parçaları keser. `screen` dikey ekranın piksel boyutu.
    static func build(from source: UIImage, screen: CGSize, text: String, dx: Int, dy: Int) -> BuildResult {
        guard let dir else { return .unavailable }
        guard let cg = upright(source) else { return .wrongSize }
        let w = cg.width, h = cg.height
        guard w == Int(screen.width.rounded()), h == Int(screen.height.rounded()), h > w else { return .wrongSize }
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        guard let data = UIImage(cgImage: cg).jpegData(compressionQuality: 0.92),
              (try? data.write(to: dir.appendingPathComponent("source.jpg"), options: .atomic)) != nil else { return .unavailable }
        return cut(cg, text: text, dx: dx, dy: dy)
    }

    /// Kayıtlı ekran görüntüsünden parçaları yeniden keser (ince ayar ya da yazı rengi değişince)
    static func rebuild(text: String, dx: Int, dy: Int) -> BuildResult {
        guard let dir, let img = UIImage(contentsOfFile: dir.appendingPathComponent("source.jpg").path), let cg = img.cgImage else {
            return .unavailable
        }
        return cut(cg, text: text, dx: dx, dy: dy)
    }

    private static func cut(_ cg: CGImage, text: String, dx: Int, dy: Int) -> BuildResult {
        guard let dir else { return .unavailable }
        let (l, estimated) = layout(width: cg.width, height: cg.height)
        var meta = Meta(text: text, dx: dx, dy: dy, estimated: estimated, width: cg.width, height: cg.height)
        let bounds = CGRect(x: 0, y: 0, width: cg.width, height: cg.height)
        for family in [WidgetFamily.systemSmall, .systemMedium, .systemLarge] {
            for spot in WidgetSpot.allCases {
                guard let name = fileName(family, spot) else { continue }
                let r = rect(l, family, spot, dx: Double(dx), dy: Double(dy)).integral.intersection(bounds)
                guard !r.isEmpty, let part = cg.cropping(to: r) else { continue }
                if family == .systemSmall { meta.tones[spot.rawValue] = luminance(part) }
                try? UIImage(cgImage: part).jpegData(compressionQuality: 0.9)?.write(to: dir.appendingPathComponent(name), options: .atomic)
            }
        }
        if let d = try? JSONEncoder().encode(meta) { try? d.write(to: dir.appendingPathComponent("meta.json"), options: .atomic) }
        WidgetCenter.shared.reloadAllTimelines()
        return .ok(estimated: estimated)
    }

    static func remove() {
        if let dir { try? FileManager.default.removeItem(at: dir) }
        WidgetCenter.shared.reloadAllTimelines()
    }

    /// Fotoğraflardan gelen görüntü dik değilse dik çizilir (ekran görüntüleri zaten diktir)
    private static func upright(_ img: UIImage) -> CGImage? {
        if img.imageOrientation == .up, let cg = img.cgImage { return cg }
        let f = UIGraphicsImageRendererFormat()
        f.scale = 1
        return UIGraphicsImageRenderer(size: CGSize(width: img.size.width * img.scale, height: img.size.height * img.scale), format: f)
            .image { _ in img.draw(in: CGRect(x: 0, y: 0, width: img.size.width * img.scale, height: img.size.height * img.scale)) }
            .cgImage
    }

    /// Ortalama parlaklık (0 koyu – 1 açık)
    private static func luminance(_ img: CGImage) -> Double {
        var px = [UInt8](repeating: 0, count: 4)
        let ok = px.withUnsafeMutableBytes { buf -> Bool in
            guard let ctx = CGContext(data: buf.baseAddress, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4,
                                      space: CGColorSpaceCreateDeviceRGB(),
                                      bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return false }
            ctx.interpolationQuality = .medium
            ctx.draw(img, in: CGRect(x: 0, y: 0, width: 1, height: 1))
            return true
        }
        guard ok else { return 0.5 }
        return (0.2126 * Double(px[0]) + 0.7152 * Double(px[1]) + 0.0722 * Double(px[2])) / 255
    }
}
