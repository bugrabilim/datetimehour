import Foundation

/// Vakit bildirimi planı. Bildirimi işletim sistemi kurar (iOS'ta en çok 64 bekleyen yerel bildirim);
/// burada yalnız hangi anlarda ne bildirileceği hesaplanır, böylece plan her platformda aynı ve test edilebilir.
public enum PrayerAlerts {
    public struct Settings: Equatable, Sendable {
        public var enabled: Bool
        public var prayers: Set<Prayer>
        /// Vakitten kaç dakika önce (0 = vaktinde)
        public var leadMinutes: Int
        /// Vakte özel "önce" süresi (2. analiz #54); yoksa genel süre
        public var perLead: [Prayer: Int]
        /// Vakte özel günler (ISO 1 pazartesi … 7 pazar); yoksa her gün
        public var days: [Prayer: Set<Int>]

        public init(enabled: Bool = false, prayers: Set<Prayer> = Self.defaultPrayers, leadMinutes: Int = 0,
                    perLead: [Prayer: Int] = [:], days: [Prayer: Set<Int>] = [:]) {
            self.enabled = enabled
            self.prayers = prayers
            self.leadMinutes = leadMinutes
            self.perLead = perLead
            self.days = days
        }

        public func lead(_ p: Prayer) -> Int { max(0, perLead[p] ?? leadMinutes) }

        /// Varsayılan: beş vakit (güneş doğuşu kapalı)
        public static let defaultPrayers: Set<Prayer> = [.fajr, .dhuhr, .asr, .maghrib, .isha]
        public static let leadChoices = [0, 5, 10, 15, 30, 45]
    }

    public struct Alert: Equatable, Sendable {
        public let prayer: Prayer
        /// Vaktin kendisi
        public let prayerAt: Date
        /// Bildirimin geleceği an
        public let fireAt: Date
    }

    /// `now` sonrasındaki bildirimler, zamana göre sıralı; en çok `limit` tane ve `days` gün.
    public static func plan(schedule: PrayerSchedule, settings: Settings, now: Date,
                            limit: Int = 63, days: Int = 30) -> [Alert] {
        guard settings.enabled, !settings.prayers.isEmpty, limit > 0 else { return [] }
        let cal = schedule.calendar
        var out: [Alert] = []
        for add in 0..<days {
            guard let day = cal.date(byAdding: .day, value: add, to: now) else { continue }
            let wd = cal.component(.weekday, from: day)
            let iso = wd == 1 ? 7 : wd - 1
            for e in schedule.entries(on: day) where settings.prayers.contains(e.prayer) {
                if let only = settings.days[e.prayer], !only.isEmpty, !only.contains(iso) { continue }
                let fire = e.at.addingTimeInterval(-TimeInterval(settings.lead(e.prayer) * 60))
                guard fire > now else { continue }
                out.append(Alert(prayer: e.prayer, prayerAt: e.at, fireAt: fire))
                if out.count == limit { return out }
            }
        }
        return out
    }
}
