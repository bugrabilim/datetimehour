import XCTest
@testable import SaatCore

/// 2. analiz #27 (odak çizelgesi) ve #28 (nefes); Kotlin'deki ScheduleBreathTest ile aynı değerler
final class ScheduleBreathTests: XCTestCase {
    let weekdays: Set<Int> = [1, 2, 3, 4, 5]

    func testSchedule() {
        XCTAssertTrue(FocusSchedule.active(days: weekdays, from: 480, to: 1020, weekday: 1, minute: 480))
        XCTAssertFalse(FocusSchedule.active(days: weekdays, from: 480, to: 1020, weekday: 1, minute: 1020))
        XCTAssertFalse(FocusSchedule.active(days: weekdays, from: 480, to: 1020, weekday: 6, minute: 600))
        XCTAssertTrue(FocusSchedule.active(days: [5], from: 1320, to: 360, weekday: 5, minute: 1400))
        XCTAssertTrue(FocusSchedule.active(days: [5], from: 1320, to: 360, weekday: 6, minute: 100))
        XCTAssertFalse(FocusSchedule.active(days: [5], from: 1320, to: 360, weekday: 1, minute: 100))
        XCTAssertFalse(FocusSchedule.active(days: weekdays, from: 600, to: 600, weekday: 1, minute: 600))
        XCTAssertEqual(FocusSchedule.minutesToNextBoundary(from: 480, to: 1020, minute: 420), 60)
        XCTAssertEqual(FocusSchedule.minutesToNextBoundary(from: 480, to: 1020, minute: 1020), 1440 - 1020 + 480)
    }

    func testBreath() {
        let p = Breath.patterns["478"]!
        XCTAssertEqual(Breath.cycleSeconds(p), 19)
        let a = Breath.phase(p, elapsed: 2)
        XCTAssertEqual(a.step, 0); XCTAssertEqual(a.progress, 0.5, accuracy: 1e-9)
        let b = Breath.phase(p, elapsed: 5)
        XCTAssertEqual(b.step, 1); XCTAssertEqual(Breath.scale(b), 1)
        let c = Breath.phase(p, elapsed: 15)
        XCTAssertEqual(c.step, 2); XCTAssertEqual(Breath.scale(c), 0.5, accuracy: 1e-9)
        let d = Breath.phase(p, elapsed: 19.5)
        XCTAssertEqual(d.cycle, 1); XCTAssertEqual(d.step, 0)
        XCTAssertEqual(Breath.phase(Breath.patterns["calm"]!, elapsed: 4.5).step, 2)
    }
}
