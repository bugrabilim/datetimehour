import XCTest
@testable import SaatCore

/// Android'deki core FocusSleepWakeTest ile aynı denetimler
final class FocusSleepWakeTests: XCTestCase {
    private let days = (24...30).map { "2026-10-\($0)" }

    func testFocusStats() {
        var log: [FocusLog.Entry] = [
            .init(d: "2026-10-01", m: 25, t: "eski"),
            .init(d: "2026-10-24", m: 25, t: "rapor"),
            .init(d: "2026-10-29", m: 50, t: "rapor"),
            .init(d: "2026-10-30", m: 25, t: ""),
        ]
        log = FocusLog.add(log, .init(d: "2026-10-30", m: 25, t: "  kod  "))
        let s = FocusLog.stats(log, days: days, noTask: "Adsız")
        XCTAssertEqual(s.today, .init(n: 2, m: 50))
        XCTAssertEqual(s.week, .init(n: 4, m: 125))
        XCTAssertEqual(s.total, .init(n: 5, m: 150))
        XCTAssertEqual(s.perDay, [25, 0, 0, 0, 0, 50, 50])
        XCTAssertEqual(s.top.map(\.name), ["rapor", "Adsız", "kod"])
        XCTAssertEqual(s.top.map(\.minutes), [75, 25, 25])
    }

    /// Odak serisi ve etiketler (rakip analizi #27): bugün ya da dün biten seri; etiketsizler "none"
    func testFocusStreakAndTags() {
        let log = [
            FocusLog.Entry(d: "2026-10-27", m: 25, t: "", g: "work"), FocusLog.Entry(d: "2026-10-28", m: 25, t: "", g: "work"),
            FocusLog.Entry(d: "2026-10-29", m: 25, t: "", g: "study"), FocusLog.Entry(d: "2026-10-29", m: 50, t: ""),
        ]
        let today = CalendarDay(2026, 10, 30)
        XCTAssertEqual(FocusLog.streak(log, today: today), 3)
        XCTAssertEqual(FocusLog.streak(log + [FocusLog.Entry(d: "2026-10-30", m: 25, t: "")], today: today), 4)
        XCTAssertEqual(FocusLog.streak(log, today: today.adding(days: 2)), 0)
        let tags = FocusLog.tags(log, since: "2026-10-24")
        XCTAssertEqual(tags.map(\.tag), ["work", "none", "study"]) // eşit dakikada ilk görülen önce
        XCTAssertEqual(tags.map(\.minutes), [50, 50, 25])
        // Etiketli kayıt JSON'da "g" ile; etiketsiz kayıtta alan yok (sitedeki sth-pomo-log ile aynı)
        let json = String(decoding: try! JSONEncoder().encode(log[3]), as: UTF8.self)
        XCTAssertFalse(json.contains("\"g\""))
        XCTAssertEqual(try! JSONDecoder().decode(FocusLog.Entry.self, from: Data(#"{"d":"2026-10-29","m":25,"t":"","g":"work"}"#.utf8)).g, "work")
    }

    func testFocusLogIsCapped() {
        var log: [FocusLog.Entry] = []
        for i in 0..<1005 { log = FocusLog.add(log, .init(d: "2026-10-30", m: i, t: String(repeating: "x", count: 80))) }
        XCTAssertEqual(log.count, 1000)
        XCTAssertEqual(log.first?.m, 5)
        XCTAssertEqual(log.last?.t.count, 60)
    }

    func testNoiseLevelsAndLoop() {
        for kind in NoiseGen.kinds {
            let s = NoiseGen.samples(kind, rate: 8000)
            XCTAssertEqual(s.count, NoiseGen.seconds(kind) * 8000, kind)
            let rms = (s.reduce(0.0) { $0 + Double($1) * Double($1) } / Double(s.count)).squareRoot()
            XCTAssert((0.17...0.21).contains(rms), "\(kind) rms \(rms)")
            XCTAssert(s.allSatisfy { (-1...1).contains($0) }, kind)
        }
        let b = NoiseGen.samples("brown", rate: 8000)
        XCTAssertLessThan(abs(b.last! - b.first!), 0.05)
        XCTAssertEqual(NoiseGen.samples("pink", rate: 8000), NoiseGen.samples("pink", rate: 8000))
    }

    func testSleepCycles() {
        XCTAssertEqual(Sleep.bedtimes(wake: 7 * 60).map(\.minute), [21 * 60 + 45, 23 * 60 + 15, 45])
        XCTAssertEqual(Sleep.wakeTimes(now: 23 * 60).map(\.minute), [8 * 60 + 15, 6 * 60 + 45, 5 * 60 + 15])
        XCTAssertEqual(Sleep.bedtime(wake: 7 * 60, duration: 8 * 60), 23 * 60)
    }

    func testSunrise() {
        let at = Date(timeIntervalSince1970: 1_800_000_000)
        func lv(_ min: Double) -> Double { SunriseLight.level(now: at.addingTimeInterval(min * 60), alarmAt: at, window: 30) }
        XCTAssertEqual(lv(-31), 0)
        XCTAssertEqual(lv(-15), 0.5, accuracy: 1e-9)
        XCTAssertEqual(lv(1), 1)
        XCTAssertEqual(lv(16), 0)
        XCTAssertEqual(SunriseLight.level(now: at, alarmAt: at, window: 0), 0)
        var prev = -1.0
        for m in stride(from: -30.0, through: 0, by: 1) {
            XCTAssertGreaterThanOrEqual(lv(m), prev)
            prev = lv(m)
        }
        XCTAssert(SunriseLight.rgb(0) == (0, 0, 0))
        XCTAssert(SunriseLight.rgb(1) == (255, 236, 204))
        XCTAssert(SunriseLight.rgb(0.8) == (255, 160, 72))
    }

    func testMathProblems() {
        var rng = SplitMix64(seed: 7)
        for level in 1...3 {
            for _ in 0..<50 {
                let p = WakeTask.problem(level: level, using: &rng)
                let parts = p.text.split(separator: " ").map(String.init)
                let v = parts[1] == "+" ? Int(parts[0])! + Int(parts[2])! : Int(parts[0])! * Int(parts[2])! + Int(parts[4])!
                XCTAssertEqual(v, p.answer, p.text)
            }
        }
    }

    func testMotionCounting() {
        func walk(_ c: inout MotionCounter) {
            for i in 0..<500 {
                let t = Double(i) * 0.02
                c.feed(t: t, x: 0, y: 0, z: 1 + 0.3 * sin(2 * .pi * 1.8 * t))
            }
        }
        var steps = MotionCounter.steps()
        walk(&steps)
        XCTAssertEqual(steps.count, 18)
        var still = MotionCounter.steps()
        var rng = SplitMix64(seed: 1)
        for i in 0..<500 {
            still.feed(t: Double(i) * 0.02, x: .random(in: -0.02...0.02, using: &rng), y: .random(in: -0.02...0.02, using: &rng),
                       z: 1 + .random(in: -0.02...0.02, using: &rng))
        }
        XCTAssertEqual(still.count, 0)
        var notShake = MotionCounter.shake()
        walk(&notShake)
        XCTAssertEqual(notShake.count, 0)
        var shake = MotionCounter.shake()
        for i in 0..<250 { shake.feed(t: Double(i) * 0.02, x: 2.5 * sin(2 * .pi * 2 * Double(i) * 0.02), y: 0, z: 1) }
        XCTAssert((18...22).contains(shake.count), "shake \(shake.count)")
    }
}
