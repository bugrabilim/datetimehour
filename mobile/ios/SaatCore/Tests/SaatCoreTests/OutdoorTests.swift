import XCTest
@testable import SaatCore

/// Android OutdoorTest ile aynı örnekler
final class OutdoorTests: XCTestCase {
    private func h(_ feels: Double, rain: Int = 0, mm: Double = 0, wind: Double = 8, gust: Double = 15, code: Int = 1,
                   day: Bool = true, uv: Double = 3) -> Outdoor.Hour {
        Outdoor.Hour(temp: feels, feels: feels, rain: rain, mm: mm, wind: wind, gust: gust, code: code, isDay: day, uv: uv)
    }

    func testScores() {
        XCTAssertEqual(Outdoor.score(.run, h(14)), 2)
        XCTAssertEqual(Outdoor.score(.run, h(24)), 1)
        XCTAssertEqual(Outdoor.score(.run, h(31)), 0)
        XCTAssertEqual(Outdoor.score(.walk, h(18, rain: 70)), 0)
        XCTAssertEqual(Outdoor.score(.walk, h(18, rain: 40)), 1)
        XCTAssertEqual(Outdoor.score(.bike, h(18, wind: 35)), 0)
        XCTAssertEqual(Outdoor.score(.bike, h(18, wind: 22)), 1)
        XCTAssertEqual(Outdoor.score(.bike, h(18, gust: 50)), 0)
        XCTAssertEqual(Outdoor.score(.garden, h(18, day: false)), 0)
        XCTAssertEqual(Outdoor.score(.run, h(18, day: false)), 1)
        XCTAssertEqual(Outdoor.score(.walk, h(18, code: 95)), 0)
        XCTAssertEqual(Outdoor.score(.garden, h(18, uv: 9)), 1)
        XCTAssertEqual(Outdoor.score(.bike, h(18, uv: 9)), 2)
    }

    func testBest() {
        XCTAssertEqual(Outdoor.best(.run, [h(30), h(24), h(15), h(16)]), 2)
        XCTAssertNil(Outdoor.best(.run, [h(35), h(14, rain: 80)]))
    }

    func testLevels() {
        XCTAssertEqual([10, 25, 45, 70, 90, 140].map { Outdoor.aqiLevel($0) }, [0, 1, 2, 3, 4, 5])
        XCTAssertEqual([0.2, 5, 40, 300].map { Outdoor.pollenLevel($0) }, [0, 1, 2, 3])
    }
}
