import XCTest
@testable import SaatCore

/// 2. analiz #47 (uyku düzeni) ve #48 (kronotip); Kotlin'deki SleepStatsTest ile aynı değerler
final class SleepStatsTests: XCTestCase {
    func testRecord() {
        var log = SleepStats.record([], SleepStats.Entry(d: "2026-11-02", w: 420))
        log = SleepStats.record(log, SleepStats.Entry(d: "2026-11-02", w: 430, b: 1380))
        XCTAssertEqual(log, [SleepStats.Entry(d: "2026-11-02", w: 420, b: 1380)])
        XCTAssertEqual(SleepStats.duration(log[0]), 480)
    }

    func testSummary() {
        let log = [
            SleepStats.Entry(d: "2026-10-24", w: 450), SleepStats.Entry(d: "2026-10-25", w: 470),
            SleepStats.Entry(d: "2026-10-30", w: 420, b: 1380), SleepStats.Entry(d: "2026-10-31", w: 440, b: 0), SleepStats.Entry(d: "2026-11-01", w: 400, b: 1350),
        ]
        let s = SleepStats.summary(log, today: "2026-11-02", goal: 480)
        XCTAssertEqual(s.weekWake, 420)
        XCTAssertEqual(s.prevWake, 460)
        XCTAssertEqual(s.nights, 3)
        XCTAssertEqual(s.debt, 40)
        XCTAssertEqual(s.spread, 24)
        XCTAssertNil(SleepStats.summary([], today: "2026-11-02", goal: 480).weekWake)
    }

    func testChronotype() {
        let m = Chronotype.msfsc(workBed: 1410, workWake: 420, freeBed: 60, freeWake: 570)
        XCTAssertEqual(m, 294)
        XCTAssertEqual(Chronotype.category(m), 3)
        XCTAssertEqual(Chronotype.category(Chronotype.msfsc(workBed: 1290, workWake: 330, freeBed: 1320, freeWake: 360)), 0)
    }
}
