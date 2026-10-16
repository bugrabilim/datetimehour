import XCTest
@testable import SaatCore

final class PrayerAlertsTests: XCTestCase {
    /// Türkiye UTC+3 (2016'dan beri yaz saati yok); Linux'ta saat dilimi verisine bağlı kalmamak için sabit fark
    let zone = TimeZone(secondsFromGMT: 3 * 3600)!
    var istanbul: PrayerSchedule { PrayerSchedule(latitude: 41.01, longitude: 28.98, timeZone: zone) }

    func date(_ y: Int, _ m: Int, _ d: Int, _ h: Int, _ min: Int) -> Date {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = zone
        return cal.date(from: DateComponents(year: y, month: m, day: d, hour: h, minute: min))!
    }

    func testEntriesMatchCalculation() {
        let entries = istanbul.entries(on: date(2026, 10, 9, 12, 0))
        let day = PrayerTimes.times(year: 2026, month: 10, day: 9, latitude: 41.01, longitude: 28.98, utcOffset: 3)
        XCTAssertEqual(entries.map(\.prayer), Prayer.allCases)
        for e in entries {
            XCTAssertEqual(e.at, date(2026, 10, 9, 0, 0).addingTimeInterval(TimeInterval(day.minutes(e.prayer)! * 60)), e.prayer.rawValue)
        }
    }

    func testNextAfterIshaIsTomorrowsFajr() throws {
        let next = try XCTUnwrap(istanbul.next(after: date(2026, 10, 9, 23, 0)))
        XCTAssertEqual(next.prayer, .fajr)
        XCTAssertEqual(next.at, istanbul.entries(on: date(2026, 10, 10, 12, 0))[0].at)
    }

    func testPlanRespectsLeadSelectionAndLimit() throws {
        let now = date(2026, 10, 9, 13, 0)
        let settings = PrayerAlerts.Settings(enabled: true, leadMinutes: 10)
        let plan = PrayerAlerts.plan(schedule: istanbul, settings: settings, now: now)
        XCTAssertEqual(plan.count, 63)
        let first = try XCTUnwrap(plan.first)
        XCTAssertEqual(first.prayer, .asr) // öğle geçti
        XCTAssertEqual(first.fireAt, first.prayerAt.addingTimeInterval(-600))
        XCTAssertFalse(plan.contains { $0.prayer == .sunrise })
        XCTAssertTrue(plan.allSatisfy { $0.fireAt > now })
        XCTAssertEqual(plan.map(\.fireAt), plan.map(\.fireAt).sorted())
        XCTAssertEqual(Set(plan.map(\.fireAt)).count, plan.count)
    }

    func testPlanSkipsAlertWhoseLeadTimeHasPassed() {
        // İkindi 16:07 civarı; 16:00'da 15 dk önceki bildirim geçmiştir, ilk bildirim akşamınki olur
        let settings = PrayerAlerts.Settings(enabled: true, leadMinutes: 15)
        let plan = PrayerAlerts.plan(schedule: istanbul, settings: settings, now: date(2026, 10, 9, 16, 0), days: 1)
        XCTAssertEqual(plan.map(\.prayer), [.maghrib, .isha])
    }

    func testDisabledOrEmptyPlansNothing() {
        let now = date(2026, 10, 9, 13, 0)
        XCTAssertTrue(PrayerAlerts.plan(schedule: istanbul, settings: .init(enabled: false), now: now).isEmpty)
        XCTAssertTrue(PrayerAlerts.plan(schedule: istanbul, settings: .init(enabled: true, prayers: []), now: now).isEmpty)
    }

    func testDaysBoundSinglePrayer() {
        let settings = PrayerAlerts.Settings(enabled: true, prayers: [.fajr])
        let plan = PrayerAlerts.plan(schedule: istanbul, settings: settings, now: date(2026, 10, 9, 13, 0))
        XCTAssertEqual(plan.count, 29) // bugünün imsakı geçti, sonraki 29 gün
        XCTAssertTrue(plan.allSatisfy { $0.prayer == .fajr })
    }
}
