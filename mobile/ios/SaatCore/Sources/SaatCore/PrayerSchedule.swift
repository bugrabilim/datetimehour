import Foundation

/// Bir konumun vakitleri gerçek anlar olarak: o yerin saat dilimiyle, sitedeki gibi dakikaya yuvarlanmış.
public struct PrayerSchedule: Sendable {
    public struct Entry: Identifiable, Equatable, Sendable {
        public let prayer: Prayer
        public let at: Date
        public var id: String { prayer.rawValue }
    }

    public let latitude: Double
    public let longitude: Double
    public let timeZone: TimeZone

    public init(latitude: Double, longitude: Double, timeZone: TimeZone) {
        self.latitude = latitude
        self.longitude = longitude
        self.timeZone = timeZone
    }

    var calendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        return cal
    }

    /// Verilen günün vakitleri, sırayla (oluşmayan vakit listede yer almaz).
    public func entries(on date: Date) -> [Entry] {
        let cal = calendar
        let c = cal.dateComponents([.year, .month, .day], from: date)
        let day = PrayerTimes.times(year: c.year!, month: c.month!, day: c.day!,
                                    latitude: latitude, longitude: longitude, timeZone: timeZone)
        let start = cal.startOfDay(for: date)
        return Prayer.allCases.compactMap { p in
            day.minutes(p).map { Entry(prayer: p, at: start.addingTimeInterval(TimeInterval($0 * 60))) }
        }
    }

    /// Şu andan sonraki ilk vakit (bugün kalmadıysa yarının ilk vakti).
    public func next(after now: Date) -> Entry? {
        for add in 0...1 {
            guard let day = calendar.date(byAdding: .day, value: add, to: now) else { continue }
            if let e = entries(on: day).first(where: { $0.at > now }) { return e }
        }
        return nil
    }
}
