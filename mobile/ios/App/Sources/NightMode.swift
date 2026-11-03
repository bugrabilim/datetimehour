import SwiftUI
import UIKit

/// Gece / yatak başı modu (rakip analizi #1): tam ekran saatte siyah zemin, kısık kırmızı yazı (gece görüşünü ve uykuyu
/// az bozar), ekran parlaklığı düşer, ekran yanmasın diye içerik dakikada bir birkaç nokta kayar.
/// Seçim `nightMode`: "off" (varsayılan), "on", "auto" (seçili yerde Güneş ufkun altındayken). Android'deki NightMode ile aynı.
enum NightMode {
    static let key = "nightMode"
    static let colorKey = "night.color"
    static let fromKey = "night.from"
    static let toKey = "night.to"
    /// Seçenekler: kapalı, açık, Güneş batınca, belirli saatlerde (2. analiz #11). Ortam ışığına göre seçenek yalnız
    /// Android'de (iOS ışık sensörünü uygulamalara açmaz).
    static let choices = ["off", "on", "auto", "schedule"]

    /// Gece yazı rengi (2. analiz #189): çarpım rengi, beyaz → kısık renk, siyah → siyah. Android'deki COLORS ile aynı.
    static let colors: [(id: String, rgb: UInt32)] = [
        ("red", 0xB82016), ("amber", 0xB87A16), ("orange", 0xB85016), ("green", 0x2E8B3A),
        ("aqua", 0x1E8C8C), ("blue", 0x2A5BB8), ("white", 0x8A8A8A),
    ]
    static func color(_ id: String) -> Color {
        let v = colors.first { $0.id == id }?.rgb ?? 0xB82016
        return Color(red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255)
    }
    static var tint: Color { color(UserDefaults.standard.string(forKey: colorKey) ?? "red") }

    /// Belirli saatler (gece yarısından dakika): varsayılan 22.00 → 07.00
    static var from: Int { UserDefaults.standard.object(forKey: fromKey) as? Int ?? 22 * 60 }
    static var to: Int { UserDefaults.standard.object(forKey: toKey) as? Int ?? 7 * 60 }
    static func inSchedule(_ minute: Int, from: Int, to: Int) -> Bool {
        if from == to { return false }
        return from < to ? (minute >= from && minute < to) : (minute >= from || minute < to)
    }

    /// Gün doğumu/batımı eşiği (kırılma + Güneş yarıçapı)
    static func active(_ raw: String, place: AppPlace, at date: Date) -> Bool {
        switch raw {
        case "on": return true
        case "auto": return AnimatedBackground.sunAltitude(place: place, at: date) < -0.833
        case "schedule":
            let c = Calendar.current.dateComponents([.hour, .minute], from: date)
            return inSchedule((c.hour ?? 0) * 60 + (c.minute ?? 0), from: from, to: to)
        default: return false
        }
    }

    /// Dakikaya göre kayma (pt): her biri −6…6, iki platformda aynı
    static func shift(_ date: Date) -> CGSize {
        let k = Int(floor(date.timeIntervalSince1970 / 60))
        return CGSize(width: CGFloat((k * 7) % 13 - 6), height: CGFloat((k * 11) % 13 - 6))
    }

    /// Tam ekrandaki ay düğmesi: açıkken kapatır, kapalıyken açar
    static func toggled(_ active: Bool) -> String { active ? "off" : "on" }
}

/// Gece modunda ekran parlaklığı düşer; kapanınca ya da tam ekrandan çıkınca önceki değere döner
@MainActor
struct NightBrightness: ViewModifier {
    /// İstenen ekran parlaklığı; nil ise telefonun kendi ayarı (gece modu 0,05; gün doğumu ışığında yükselir)
    let target: CGFloat?
    @State private var saved: CGFloat?

    init(on: Bool) { target = on ? 0.05 : nil }
    init(target: CGFloat?) { self.target = target }

    func body(content: Content) -> some View {
        content
            .onAppear { apply(target) }
            .onChange(of: target) { _, v in apply(v) }
            .onDisappear { apply(nil) }
    }

    private var screen: UIScreen? {
        UIApplication.shared.connectedScenes.compactMap { ($0 as? UIWindowScene)?.screen }.first
    }

    private func apply(_ v: CGFloat?) {
        guard let screen else { return }
        if let v {
            if saved == nil { saved = screen.brightness }
            screen.brightness = v
        } else if let s = saved {
            screen.brightness = s
            saved = nil
        }
    }
}

/// Tam ekran saatte uyku zamanlayıcısı: bitiş anı (1970'ten saniye, 0 = kapalı); tam ekrandan çıkınca silinir
enum SleepTimer {
    static let key = "night.sleepUntil"
    static let choices = [0, 15, 30, 60, 90]
}
