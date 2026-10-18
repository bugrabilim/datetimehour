import SwiftUI
import UIKit

/// Sitedeki renk token'ları (src/styles.css): açık "kâğıt" ve koyu "gece" paleti.
/// Uygulama teması kullanıcı seçimidir (Ayarlar › Uygulama rengi, `accent` anahtarı): her tema bir ana renk,
/// bir ikinci renk ve bir ton gücüyle gelir; zemin, kartlar, saat kadranı ve ibreler buna göre boyanır.
/// Varsayılan temada ton yoktur (sitedeki renkler). Widget'lar varsayılanı kullanır.
enum Theme {
    static var bg: Color { tinted(0xF7F4EE, 0x11141A, light: 0.09, dark: 0.11) }
    static var surface: Color { tinted(0xFFFFFF, 0x1A1E26, light: 0.04, dark: 0.07) }
    /// Saat kadranı ve ikincil yüzeyler
    static var surface2: Color { tinted(0xEFEBE2, 0x232834, light: 0.18, dark: 0.20) }
    static let text = dynamic(light: 0x1B1F27, dark: 0xEEF0F4)
    static let muted = dynamic(light: 0x565C68, dark: 0xA5ACB9)
    static var border: Color { tinted(0xD6CFBF, 0x343B49, light: 0.35, dark: 0.35) }
    static var accent: Color { AccentPalette.current.color }
    static var accentSoft: Color { AccentPalette.current.color.opacity(0.16) }
    /// İkinci renk: saniye ibresi, saat ortası, vurgu ayrıntıları
    static var accent2: Color { AccentPalette.current.secondColor }
    /// Saat rakamları ve akrep: yazı rengi, temanın ana rengine doğru çekilmiş
    static var ink: Color { tinted(0x1B1F27, 0xEEF0F4, light: 0.45, dark: 0.30) }

    fileprivate static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(rgb: dark) : UIColor(rgb: light) })
    }

    /// Temel rengi seçili temanın ana rengine doğru karıştırır (oran × temanın ton gücü)
    private static func tinted(_ light: UInt32, _ dark: UInt32, light lt: Double, dark dt: Double) -> Color {
        let p = AccentPalette.current
        let k = p.tint
        return Color(UIColor { tc in
            tc.userInterfaceStyle == .dark
                ? UIColor(rgb: mix(dark, p.dark, dt * k))
                : UIColor(rgb: mix(light, p.light, lt * k))
        })
    }

    static func mix(_ a: UInt32, _ b: UInt32, _ t: Double) -> UInt32 {
        func ch(_ v: UInt32, _ s: UInt32) -> Double { Double((v >> s) & 0xFF) }
        func m(_ s: UInt32) -> UInt32 { UInt32((ch(a, s) * (1 - t) + ch(b, s) * t).rounded()) << s }
        return m(16) | m(8) | m(0)
    }
}

/// Uygulama temaları: dört grup, her grupta altı tema. Ana ve ikinci renk açık temada koyu, koyu temada açık
/// tonla verilir (iki temada da okunur); `tint` zeminin ve kadranın ne kadar boyanacağı.
struct AccentPalette: Identifiable {
    enum Group: String, CaseIterable { case general, kids, elegant, business }

    let id: String
    let group: Group
    let light: UInt32
    let dark: UInt32
    let second: (light: UInt32, dark: UInt32)
    let tint: Double

    var color: Color { Theme.dynamic(light: light, dark: dark) }
    var secondColor: Color { Theme.dynamic(light: second.light, dark: second.dark) }
    /// Bu temada saat kadranının rengi (seçicideki örnek için)
    var faceColor: Color {
        Theme.dynamic(light: Theme.mix(0xEFEBE2, light, 0.18 * tint), dark: Theme.mix(0x232834, dark, 0.20 * tint))
    }

    static let defaultID = "amber"

    private init(_ id: String, _ group: Group, _ light: UInt32, _ dark: UInt32, second: (UInt32, UInt32), tint: Double) {
        self.id = id
        self.group = group
        self.light = light
        self.dark = dark
        self.second = second
        self.tint = tint
    }

    static let all: [AccentPalette] = [
        // Genel (varsayılan: sitedeki renkler, ton yok)
        .init("amber", .general, 0x9A3F00, 0xF2A65A, second: (0x9A3F00, 0xF2A65A), tint: 0),
        .init("blue", .general, 0x0A66C2, 0x5AA9FF, second: (0xC2410A, 0xFF9E5A), tint: 0.8),
        .init("green", .general, 0x1B7A43, 0x5BD38A, second: (0xA15C00, 0xF5C04A), tint: 0.8),
        .init("indigo", .general, 0x3949AB, 0x9FA8DA, second: (0xC2185B, 0xF48FB1), tint: 0.8),
        .init("orange", .general, 0xB85400, 0xFFB74D, second: (0x00695C, 0x4DB6AC), tint: 0.8),
        .init("cyan", .general, 0x00708A, 0x4DD0E1, second: (0xAD1457, 0xF06292), tint: 0.8),
        // Çocuklar için: canlı ve iki renkli
        .init("kid-red", .kids, 0xC62828, 0xFF6B6B, second: (0x1565C0, 0x64B5F6), tint: 1),
        .init("kid-orange", .kids, 0xD84315, 0xFF9E5E, second: (0x6A1B9A, 0xCE93D8), tint: 1),
        .init("kid-yellow", .kids, 0x8A6D00, 0xFFD54F, second: (0xC62828, 0xFF6B6B), tint: 1),
        .init("kid-green", .kids, 0x2E7D32, 0x7BD88F, second: (0xD84315, 0xFF9E5E), tint: 1),
        .init("kid-blue", .kids, 0x1565C0, 0x64B5F6, second: (0x8A6D00, 0xFFD54F), tint: 1),
        .init("kid-purple", .kids, 0x7B1FA2, 0xCE93D8, second: (0x2E7D32, 0x7BD88F), tint: 1),
        // Zarif: gül, mercan, leylak tonları
        .init("rose", .elegant, 0xAD1457, 0xF48FB1, second: (0x8D6E63, 0xD7B8A8), tint: 0.9),
        .init("coral", .elegant, 0xB4442E, 0xFF8A75, second: (0x00796B, 0x80CBC4), tint: 0.9),
        .init("mauve", .elegant, 0x8E4A6E, 0xE0A3C4, second: (0x5C6BC0, 0xB3BCF5), tint: 0.9),
        .init("lavender", .elegant, 0x6A4FA3, 0xB9A5F0, second: (0xAD1457, 0xF48FB1), tint: 0.9),
        .init("plum", .elegant, 0x6D2E5B, 0xD69ACB, second: (0xA1887F, 0xE6C9B8), tint: 0.9),
        .init("rosegold", .elegant, 0x9C5A4A, 0xE8B4A0, second: (0x6D4C41, 0xD7B8A8), tint: 0.9),
        // İş: sakin, koyu tonlar
        .init("navy", .business, 0x1F3A68, 0x8AB4F8, second: (0xA67C00, 0xE6C35C), tint: 0.6),
        .init("slate", .business, 0x3E4C5E, 0xA9B7C8, second: (0x0A66C2, 0x5AA9FF), tint: 0.6),
        .init("graphite", .business, 0x333333, 0xD0D0D0, second: (0xC62828, 0xFF6B6B), tint: 0.5),
        .init("teal", .business, 0x00695C, 0x4DB6AC, second: (0x37474F, 0xB0BEC5), tint: 0.6),
        .init("burgundy", .business, 0x7A1F2B, 0xE08A95, second: (0x8D6E63, 0xD7B8A8), tint: 0.6),
        .init("forest", .business, 0x2F5233, 0x8FC79A, second: (0x8A6D00, 0xE6C35C), tint: 0.6),
    ]

    static func find(_ id: String?) -> AccentPalette {
        all.first { $0.id == id } ?? all[0]
    }

    static var current: AccentPalette { find(UserDefaults.standard.string(forKey: "accent")) }
}

/// Liste ve form sayfalarının zemini temanın zemin rengidir; tema değişince sayfa yeniden boyanır
struct ThemedBackground: ViewModifier {
    @AppStorage("accent") private var accent = AccentPalette.defaultID

    func body(content: Content) -> some View {
        content
            .scrollContentBackground(.hidden)
            .background(Theme.bg.ignoresSafeArea())
    }
}

extension View {
    func themedBackground() -> some View { modifier(ThemedBackground()) }
}

private extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(red: CGFloat((rgb >> 16) & 0xFF) / 255, green: CGFloat((rgb >> 8) & 0xFF) / 255,
                  blue: CGFloat(rgb & 0xFF) / 255, alpha: 1)
    }
}
