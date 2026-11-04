import SwiftUI
import UIKit

/*
 * Başlık yazısı (doğal tasarım, docs/dogal-tasarim bölüm 3.2; sahibin onayı 4 Kasım 2026): Literata (SIL OFL 1.1;
 * App/Resources/Fonts, Latin + Kiril alt kümesi, 500 ve 600; üretim mobile/shared/fonts/make-literata.py; Info.plist
 * UIAppFonts). Literata yalnız Latin ve Kiril yazılı dillerde seçilir; Arapça, Urduca, Farsça, Japonca, Korece, Çince,
 * Tayca ve Hintçede sistem yazısı. Dynamic Type ile büyür (relativeTo). Android'deki Fonts.kt ile aynı kural.
 */
enum AppFonts {
    private static let serifLangs: Set<String> = ["tr", "en", "de", "az", "id", "fr", "fil", "es", "pt", "ru"]

    /// Başlıkta Literata kullanılıyor mu (dilin yazısına göre)
    static var serif: Bool { serifLangs.contains(AppLocale.code) }

    /// Sayfa başlığı: Literata 30, 600 (diğer dillerde sistem 28, kalın)
    static var page: Font {
        serif ? .custom("Literata-SemiBold", size: 30, relativeTo: .largeTitle) : .system(size: 28, weight: .semibold)
    }

    /// Bölüm başlığı: Literata 19, 500 (diğer dillerde sistem 18, yarı kalın)
    static var section: Font {
        serif ? .custom("Literata-Medium", size: 19, relativeTo: .title3) : .title3.weight(.semibold)
    }

    /// Kart ya da boş durum başlığı: Literata 18, 500
    static var cardTitle: Font {
        serif ? .custom("Literata-Medium", size: 18, relativeTo: .headline) : .headline
    }

    /// Kur'an, dua, hadis ve Esma'nın Arapçası (doğal tasarım madde 16): Amiri (SIL OFL 1.1, klasik nesih; Arapça alt
    /// kümesi). Arayüzün Arapça metinleri sistem yazısında kalır.
    static func arabic(_ size: CGFloat) -> Font { .custom("Amiri-Regular", size: size, relativeTo: .title2) }

    /// Büyük sayı (tarih rakamı, sıcaklık): her dilde Latin rakamı olduğu için hep Literata
    static func numerals(_ size: CGFloat) -> Font { .custom("Literata-Medium", size: size, relativeTo: .largeTitle) }

    /// Gezinme çubuğu başlıkları (büyük ve satır içi): uygulama açılırken bir kez
    static func applyNavigationBar() {
        guard serif,
              let large = UIFont(name: "Literata-SemiBold", size: 32),
              let inline = UIFont(name: "Literata-Medium", size: 18) else { return }
        let bar = UINavigationBar.appearance()
        // Görünüm vekilindeki sözlükler isteğe bağlı (nil olabilir): var olanın üstüne yazı tipi eklenir
        var largeAttrs = bar.largeTitleTextAttributes ?? [:]
        largeAttrs[.font] = UIFontMetrics(forTextStyle: .largeTitle).scaledFont(for: large)
        bar.largeTitleTextAttributes = largeAttrs
        var inlineAttrs = bar.titleTextAttributes ?? [:]
        inlineAttrs[.font] = UIFontMetrics(forTextStyle: .headline).scaledFont(for: inline)
        bar.titleTextAttributes = inlineAttrs
    }

    /// Yumuşak bölmeli seçici (doğal tasarım madde 5): kenarlıksız `surface2` iz, seçili parça `surface`, ayraç çizgisi
    /// yok, seçili yazı orta kalın. SwiftUI'nin `.pickerStyle(.segmented)` seçicilerinin hepsi bu görünümü alır.
    static func applySegmentedControls() {
        let seg = UISegmentedControl.appearance()
        seg.selectedSegmentTintColor = UIColor(Theme.surface)
        seg.backgroundColor = UIColor(Theme.surface2)
        seg.setDividerImage(UIImage(), forLeftSegmentState: .normal, rightSegmentState: .normal, barMetrics: .default)
        seg.setTitleTextAttributes([.foregroundColor: UIColor(Theme.muted), .font: UIFont.systemFont(ofSize: 14)], for: .normal)
        seg.setTitleTextAttributes([.foregroundColor: UIColor(Theme.text),
                                    .font: UIFont.systemFont(ofSize: 14, weight: .medium)], for: .selected)
    }
}
