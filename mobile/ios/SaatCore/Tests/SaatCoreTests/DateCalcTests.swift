import XCTest
@testable import SaatCore

final class DateCalcTests: XCTestCase {
    func testDayNumberRoundTrip() {
        XCTAssertEqual(CalendarDay(1970, 1, 1).dayNumber, 0)
        XCTAssertEqual(CalendarDay(2026, 10, 17).dayNumber, 20743)
        for n in stride(from: -50000, through: 50000, by: 37) {
            XCTAssertEqual(CalendarDay(dayNumber: n).dayNumber, n)
        }
        XCTAssertEqual(CalendarDay(iso: "2024-02-29")?.iso, "2024-02-29")
        XCTAssertNil(CalendarDay(iso: "2026-02-29"))
    }

    func testWeekdayAndIsoWeek() {
        XCTAssertEqual(CalendarDay(2026, 10, 17).weekday, 5) // cumartesi
        XCTAssertEqual(CalendarDay(2026, 10, 17).isoWeek.week, 42)
        XCTAssertEqual(CalendarDay(2026, 1, 1).isoWeek.week, 1) // perşembe
        XCTAssertEqual(CalendarDay(2027, 1, 1).isoWeek.week, 53) // cuma: 2026'nın 53. haftası
        XCTAssertEqual(CalendarDay(2027, 1, 1).isoWeek.year, 2026)
        XCTAssertEqual(CalendarDay(2024, 12, 30).isoWeek.week, 1)
        XCTAssertEqual(CalendarDay(2024, 12, 30).isoWeek.year, 2025)
    }

    func testAddMonthsClampsToMonthEnd() {
        XCTAssertEqual(CalendarDay(2026, 1, 31).adding(months: 1), CalendarDay(2026, 2, 28))
        XCTAssertEqual(CalendarDay(2024, 3, 31).adding(months: -1), CalendarDay(2024, 2, 29))
        XCTAssertEqual(CalendarDay(2026, 1, 15).adding(months: -13), CalendarDay(2024, 12, 15))
    }

    func testYmd() {
        let r = DateCalc.ymd(from: CalendarDay(1990, 5, 20), to: CalendarDay(2026, 10, 17))
        XCTAssertEqual(r.years, 36); XCTAssertEqual(r.months, 4); XCTAssertEqual(r.days, 27)
        let s = DateCalc.ymd(from: CalendarDay(2026, 1, 31), to: CalendarDay(2026, 3, 1))
        XCTAssertEqual(s.years, 0); XCTAssertEqual(s.months, 1); XCTAssertEqual(s.days, 1)
    }

    func testWorkdaysWithHolidays() {
        // 26–30 Ekim 2026: 28 Ekim yarım gün, 29 Ekim tatil
        let off = ["2026-10-28": 0.5, "2026-10-29": 0.0]
        XCTAssertEqual(DateCalc.workdays(from: CalendarDay(2026, 10, 26), to: CalendarDay(2026, 11, 2), offDays: off), 3.5)
        XCTAssertEqual(DateCalc.adding(workdays: 3, to: CalendarDay(2026, 10, 27), offDays: off), CalendarDay(2026, 11, 2))
        XCTAssertEqual(DateCalc.adding(workdays: -1, to: CalendarDay(2026, 10, 26), offDays: off), CalendarDay(2026, 10, 23))
    }

    func testNextBirthday() {
        XCTAssertEqual(DateCalc.nextBirthday(CalendarDay(1990, 10, 17), today: CalendarDay(2026, 10, 17)), CalendarDay(2026, 10, 17))
        XCTAssertEqual(DateCalc.nextBirthday(CalendarDay(1990, 5, 20), today: CalendarDay(2026, 10, 17)), CalendarDay(2027, 5, 20))
        XCTAssertEqual(DateCalc.nextBirthday(CalendarDay(2000, 2, 29), today: CalendarDay(2026, 3, 1)), CalendarDay(2027, 2, 28))
    }
}
