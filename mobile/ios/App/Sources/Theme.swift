import SwiftUI
import UIKit

/// Sitedeki renk token'ları (src/styles.css): açık "kâğıt" ve koyu "gece" paleti.
enum Theme {
    static let bg = dynamic(light: 0xF7F4EE, dark: 0x11141A)
    static let surface = dynamic(light: 0xFFFFFF, dark: 0x1A1E26)
    static let text = dynamic(light: 0x1B1F27, dark: 0xEEF0F4)
    static let muted = dynamic(light: 0x565C68, dark: 0xA5ACB9)
    static let border = dynamic(light: 0xD6CFBF, dark: 0x343B49)
    static let accent = dynamic(light: 0x9A3F00, dark: 0xF2A65A)
    static let accentSoft = dynamic(light: 0xF6E5D6, dark: 0x3A2B1D)

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(rgb: dark) : UIColor(rgb: light) })
    }
}

private extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(red: CGFloat((rgb >> 16) & 0xFF) / 255, green: CGFloat((rgb >> 8) & 0xFF) / 255,
                  blue: CGFloat(rgb & 0xFF) / 255, alpha: 1)
    }
}
