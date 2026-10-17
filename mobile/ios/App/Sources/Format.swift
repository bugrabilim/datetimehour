import Foundation
import SwiftUI
import SaatCore

/// Uygulamanın görünen dili (tarih ve sayı biçimleri bununla; cihazın bölge ayarı farklı olabilir)
enum AppLocale {
    static let code = Bundle.main.preferredLocalizations.first ?? "tr"
    static let locale = Locale(identifier: code)
    static var isRTL: Bool { code == "ar" }
    /// Tarihler her dilde Gregoryen takvimle (Arapça bölge ayarı Hicrî takvim seçebilir; site de Gregoryen kullanır)
    static let gregorian = Calendar(identifier: .gregorian)
}

/// Metin kalıbı: "{n} gün kaldı" gibi yer tutuculu metinler sırayla doldurulur
func T(_ key: String, _ args: CustomStringConvertible...) -> String {
    String(format: L(key), arguments: args.map { "\($0)" as CVarArg })
}

/// Sayı, uygulama dilinde (Arapçada da Latin rakamı; saat ve vakitlerle tutarlı)
func num(_ n: Int) -> String {
    let f = NumberFormatter()
    f.locale = Locale(identifier: AppLocale.code == "ar" ? "en_US_POSIX" : AppLocale.code)
    f.numberStyle = .decimal
    return f.string(from: NSNumber(value: n)) ?? String(n)
}

enum Fmt {
    private static func make(_ format: String, _ tz: TimeZone? = nil, posix: Bool = true) -> DateFormatter {
        let f = DateFormatter()
        f.locale = posix ? Locale(identifier: "en_US_POSIX") : AppLocale.locale
        f.dateFormat = format
        if let tz { f.timeZone = tz }
        return f
    }

    /// Saat: 24 ya da 12 saat ("h12" ayarı), saniyeli ya da saniyesiz; rakamlar her dilde Latin
    static func time(_ date: Date, seconds: Bool = false, tz: TimeZone = .current) -> String {
        let h12 = UserDefaults.standard.bool(forKey: "h12")
        let f = make(h12 ? (seconds ? "h:mm:ss" : "h:mm") : (seconds ? "HH:mm:ss" : "HH:mm"), tz)
        return f.string(from: date)
    }

    /// 12 saatte ÖÖ/ÖS işareti (uygulama dilinde), 24 saatte boş
    static func ampm(_ date: Date, tz: TimeZone = .current) -> String {
        guard UserDefaults.standard.bool(forKey: "h12") else { return "" }
        return make("a", tz, posix: false).string(from: date)
    }

    /// "17 Ekim 2026 Cumartesi" (uygulama dilinde)
    static func longDate(_ date: Date, tz: TimeZone = .current) -> String {
        var s = Date.FormatStyle.dateTime.day().month(.wide).year().weekday(.wide).locale(AppLocale.locale)
        s.timeZone = tz
        s.calendar = AppLocale.gregorian
        return date.formatted(s)
    }

    /// "17 Ekim"
    static func dayMonth(_ date: Date, tz: TimeZone = .current) -> String {
        var s = Date.FormatStyle.dateTime.day().month(.wide).locale(AppLocale.locale)
        s.timeZone = tz
        s.calendar = AppLocale.gregorian
        return date.formatted(s)
    }

    /// "17 Eki 2026, 17:46" (tz'de)
    static func shortDateTime(_ date: Date, tz: TimeZone) -> String {
        var s = Date.FormatStyle.dateTime.day().month(.abbreviated).year().locale(AppLocale.locale)
        s.timeZone = tz
        s.calendar = AppLocale.gregorian
        return date.formatted(s) + ", " + make("HH:mm", tz).string(from: date)
    }

    static func day(_ d: CalendarDay) -> String { longDate(d.noon(in: .current)) }

    /// Süre: "2 sa 30 dk"
    static func hm(seconds: Int) -> String {
        let h = abs(seconds) / 3600, m = (abs(seconds) % 3600) / 60
        var parts: [String] = []
        if h > 0 { parts.append("\(h) " + L("client.hourShort")) }
        if m > 0 || h == 0 { parts.append("\(m) " + L("client.minuteShort")) }
        return parts.joined(separator: " ")
    }

    /// Geri sayım "05:13:17" (gün varsa "3 gün 05:13:17")
    static func countdown(_ seconds: Int) -> String {
        let s = max(0, seconds), d = s / 86400
        let hms = String(format: "%02d:%02d:%02d", (s % 86400) / 3600, (s % 3600) / 60, s % 60)
        return d > 0 ? "\(d) \(L("tools.countdown.days")) \(hms)" : hms
    }

    static func monthTitle(_ year: Int, _ month: Int) -> String { L("calendar.months.\(month - 1)") + " " + String(year) }
}

/// Bölüm başlığı ve kart görünümü (sitedeki sade, çerçevesiz düzen)
struct SectionTitle: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.headline)
            .foregroundStyle(Theme.text)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

extension View {
    func card() -> some View {
        padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: 14))
    }
}
