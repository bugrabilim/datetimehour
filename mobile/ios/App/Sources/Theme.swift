import SwiftUI
import UIKit

/// Sitedeki renk token'ları (src/styles.css): açık "kâğıt" ve koyu "gece" paleti.
/// Vurgu rengi kullanıcı seçimidir (Ayarlar › Uygulama rengi, `accent` anahtarı); widget'lar varsayılanı kullanır.
enum Theme {
    static let bg = dynamic(light: 0xF7F4EE, dark: 0x11141A)
    static let surface = dynamic(light: 0xFFFFFF, dark: 0x1A1E26)
    static let surface2 = dynamic(light: 0xEFEBE2, dark: 0x232834)
    static let text = dynamic(light: 0x1B1F27, dark: 0xEEF0F4)
    static let muted = dynamic(light: 0x565C68, dark: 0xA5ACB9)
    static let border = dynamic(light: 0xD6CFBF, dark: 0x343B49)
    static var accent: Color { AccentPalette.current.color }
    static var accentSoft: Color { AccentPalette.current.color.opacity(0.16) }

    fileprivate static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(rgb: dark) : UIColor(rgb: light) })
    }
}

/// Uygulama renkleri: dört grup, her grupta altı renk. Açık temada koyu, koyu temada açık tonu kullanılır
/// (iki temada da yazı olarak okunur).
struct AccentPalette: Identifiable {
    enum Group: String, CaseIterable { case general, kids, elegant, business }

    let id: String
    let group: Group
    let light: UInt32
    let dark: UInt32
    var color: Color { Theme.dynamic(light: light, dark: dark) }

    static let defaultID = "amber"

    static let all: [AccentPalette] = [
        // Genel
        .init(id: "amber", group: .general, light: 0x9A3F00, dark: 0xF2A65A),
        .init(id: "blue", group: .general, light: 0x0A66C2, dark: 0x5AA9FF),
        .init(id: "green", group: .general, light: 0x1B7A43, dark: 0x5BD38A),
        .init(id: "indigo", group: .general, light: 0x3949AB, dark: 0x9FA8DA),
        .init(id: "orange", group: .general, light: 0xB85400, dark: 0xFFB74D),
        .init(id: "cyan", group: .general, light: 0x00708A, dark: 0x4DD0E1),
        // Çocuklar: canlı renkler
        .init(id: "kid-red", group: .kids, light: 0xC62828, dark: 0xFF6B6B),
        .init(id: "kid-orange", group: .kids, light: 0xD84315, dark: 0xFF9E5E),
        .init(id: "kid-yellow", group: .kids, light: 0x8A6D00, dark: 0xFFD54F),
        .init(id: "kid-green", group: .kids, light: 0x2E7D32, dark: 0x7BD88F),
        .init(id: "kid-blue", group: .kids, light: 0x1565C0, dark: 0x64B5F6),
        .init(id: "kid-purple", group: .kids, light: 0x7B1FA2, dark: 0xCE93D8),
        // Zarif: gül, mercan, leylak tonları
        .init(id: "rose", group: .elegant, light: 0xAD1457, dark: 0xF48FB1),
        .init(id: "coral", group: .elegant, light: 0xB4442E, dark: 0xFF8A75),
        .init(id: "mauve", group: .elegant, light: 0x8E4A6E, dark: 0xE0A3C4),
        .init(id: "lavender", group: .elegant, light: 0x6A4FA3, dark: 0xB9A5F0),
        .init(id: "plum", group: .elegant, light: 0x6D2E5B, dark: 0xD69ACB),
        .init(id: "rosegold", group: .elegant, light: 0x9C5A4A, dark: 0xE8B4A0),
        // İş: sakin, koyu tonlar
        .init(id: "navy", group: .business, light: 0x1F3A68, dark: 0x8AB4F8),
        .init(id: "slate", group: .business, light: 0x3E4C5E, dark: 0xA9B7C8),
        .init(id: "graphite", group: .business, light: 0x333333, dark: 0xD0D0D0),
        .init(id: "teal", group: .business, light: 0x00695C, dark: 0x4DB6AC),
        .init(id: "burgundy", group: .business, light: 0x7A1F2B, dark: 0xE08A95),
        .init(id: "forest", group: .business, light: 0x2F5233, dark: 0x8FC79A),
    ]

    static func find(_ id: String?) -> AccentPalette {
        all.first { $0.id == id } ?? all[0]
    }

    static var current: AccentPalette { find(UserDefaults.standard.string(forKey: "accent")) }
}

private extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(red: CGFloat((rgb >> 16) & 0xFF) / 255, green: CGFloat((rgb >> 8) & 0xFF) / 255,
                  blue: CGFloat(rgb & 0xFF) / 255, alpha: 1)
    }
}
