import SwiftUI
import UIKit

/// Gece / yatak başı modu (rakip analizi #1): tam ekran saatte siyah zemin, kısık kırmızı yazı (gece görüşünü ve uykuyu
/// az bozar), ekran parlaklığı düşer, ekran yanmasın diye içerik dakikada bir birkaç nokta kayar.
/// Seçim `nightMode`: "off" (varsayılan), "on", "auto" (seçili yerde Güneş ufkun altındayken). Android'deki NightMode ile aynı.
enum NightMode {
    static let key = "nightMode"
    static let choices = ["off", "on", "auto"]
    /// Çarpım rengi: beyaz → kısık kırmızı, siyah → siyah (Android 0xB82016)
    static let tint = Color(red: 0xB8 / 255.0, green: 0x20 / 255.0, blue: 0x16 / 255.0)

    /// Gün doğumu/batımı eşiği (kırılma + Güneş yarıçapı)
    static func active(_ raw: String, place: AppPlace, at date: Date) -> Bool {
        raw == "on" || (raw == "auto" && AnimatedBackground.sunAltitude(place: place, at: date) < -0.833)
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
