import Foundation
import SaatCore

/// Uygulama metni (Localizable.xcstrings); anahtar çalışma anında oluşuyorsa kullanılır.
func L(_ key: String) -> String {
    Bundle.main.localizedString(forKey: key, value: nil, table: nil)
}

extension PrayerSchedule {
    /// İller Türkiye saatiyle
    static let zone = TimeZone(identifier: "Europe/Istanbul")!

    init(province: Province) {
        self.init(latitude: province.lat, longitude: province.lon, timeZone: Self.zone)
    }

    /// Vakit gösterimi: 24 saat, her dilde Latin rakamı (sitedeki gibi)
    static let hm: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "HH:mm"
        f.timeZone = zone
        return f
    }()

    /// Bu yerin saat diliminde "HH:mm" (yurt dışı konumda Türkiye saati değil)
    func clock(_ date: Date) -> String {
        if timeZone == Self.zone { return Self.hm.string(from: date) }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "HH:mm"
        f.timeZone = timeZone
        return f.string(from: date)
    }
}

extension Prayer {
    var localizedName: String { L("prayer." + rawValue) }
}
