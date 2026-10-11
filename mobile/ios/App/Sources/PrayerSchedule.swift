import Foundation
import SaatCore

/// Seçili ilin vakitleri: Türkiye saatiyle (Europe/Istanbul), sitedeki gibi dakikaya yuvarlanmış.
struct PrayerSchedule {
    static let zone = TimeZone(identifier: "Europe/Istanbul")!

    struct Entry: Identifiable {
        let prayer: Prayer
        let at: Date
        var id: String { prayer.rawValue }
    }

    let province: Province

    private var calendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = Self.zone
        return cal
    }

    /// Verilen günün vakitleri (oluşmayan vakit listede yer almaz).
    func entries(on date: Date) -> [Entry] {
        let cal = calendar
        let c = cal.dateComponents([.year, .month, .day], from: date)
        let day = PrayerTimes.times(year: c.year!, month: c.month!, day: c.day!,
                                    latitude: province.lat, longitude: province.lon, timeZone: Self.zone)
        let start = cal.startOfDay(for: date)
        return Prayer.allCases.compactMap { p in
            day.minutes(p).map { Entry(prayer: p, at: start.addingTimeInterval(TimeInterval($0 * 60))) }
        }
    }

    /// Şu andan sonraki ilk vakit (bugün kalmadıysa yarının imsakı).
    func next(after now: Date) -> Entry? {
        for add in 0...1 {
            guard let day = calendar.date(byAdding: .day, value: add, to: now) else { continue }
            if let e = entries(on: day).first(where: { $0.at > now }) { return e }
        }
        return nil
    }
}

extension Prayer {
    var localizedName: String {
        Bundle.main.localizedString(forKey: "prayer." + rawValue, value: nil, table: nil)
    }
}
