import Foundation

/// Vakit bildirimi planı. Bildirimi işletim sistemi kurar (iOS'ta en çok 64 bekleyen yerel bildirim);
/// burada yalnız hangi anlarda ne bildirileceği hesaplanır, böylece plan her platformda aynı ve test edilebilir.
public enum PrayerAlerts {
    public struct Settings: Equatable, Sendable {
        public var enabled: Bool
        public var prayers: Set<Prayer>
        /// Vakitten kaç dakika önce (0 = vaktinde)
        public var leadMinutes: Int

        public init(enabled: Bool = false, prayers: Set<Prayer> = Self.defaultPrayers, leadMinutes: Int = 0) {
            self.enabled = enabled
            self.prayers = prayers
            self.leadMinutes = leadMinutes
        }

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
        let lead = TimeInterval(max(0, settings.leadMinutes) * 60)
        var out: [Alert] = []
        for add in 0..<days {
            guard let day = cal.date(byAdding: .day, value: add, to: now) else { continue }
            for e in schedule.entries(on: day) where settings.prayers.contains(e.prayer) {
                let fire = e.at.addingTimeInterval(-lead)
                guard fire > now else { continue }
                out.append(Alert(prayer: e.prayer, prayerAt: e.at, fireAt: fire))
                if out.count == limit { return out }
            }
        }
        return out
    }
}
