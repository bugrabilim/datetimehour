import XCTest
@testable import SaatCore

final class NatureTests: XCTestCase {
    /// Sitedeki hesapla (src/js/nature-calc.js) aynı sonuç: mevsim anları, ay evreleri, Ay durumu
    func testMatchesWebCalculation() throws {
        let file = try Vectors.load("nature.json", as: NatureVectors.self)
        XCTAssertGreaterThan(file.seasons.count, 40)
        for s in file.seasons {
            let got = Nature.seasons(year: s.y)
            for (g, w, name) in [(got.march, s.march, "mart"), (got.june, s.june, "haziran"), (got.september, s.sept, "eylül"), (got.december, s.dec, "aralık")] {
                XCTAssertLessThanOrEqual(abs(g - w), 1, "mevsim \(s.y) \(name)")
            }
        }
        for p in file.phases {
            XCTAssertLessThanOrEqual(abs(Nature.phaseMilliseconds(k: p.k, full: false) - p.new), 1, "yeni ay k=\(p.k)")
            XCTAssertLessThanOrEqual(abs(Nature.phaseMilliseconds(k: p.k, full: true) - p.full), 1, "dolunay k=\(p.k)")
        }
        for m in file.moon {
            let got = Nature.moon(atMilliseconds: m.ms)
            XCTAssertEqual(got.age, m.age, accuracy: 1e-9, "Ay yaşı \(m.ms)")
            XCTAssertEqual(got.fraction, m.frac, accuracy: 1e-12, "Ay döngüsü \(m.ms)")
            XCTAssertEqual(got.illumination, m.illum, accuracy: 1e-12, "Ay aydınlanma \(m.ms)")
            XCTAssertEqual(got.phaseIndex, m.idx, "Ay evresi \(m.ms)")
            XCTAssertLessThanOrEqual(abs(got.nextNewMoon - m.nextNew), 1, "sonraki yeni ay \(m.ms)")
            XCTAssertLessThanOrEqual(abs(got.nextFullMoon - m.nextFull), 1, "sonraki dolunay \(m.ms)")
        }
    }

    /// Yayımlanmış astronomik anlarla (±2 dk); scripts/test.mjs ile aynı denetim
    func testSeasonsMatchPublishedInstants() {
        let want: [Int: [String]] = [
            2025: ["2025-03-20T09:01:00Z", "2025-06-21T02:42:00Z", "2025-09-22T18:19:00Z", "2025-12-21T15:03:00Z"],
            2026: ["2026-03-20T14:46:00Z", "2026-06-21T08:24:00Z", "2026-09-23T00:06:00Z", "2026-12-21T20:50:00Z"],
        ]
        let iso = ISO8601DateFormatter()
        for (year, list) in want {
            let s = Nature.seasons(year: year)
            for (got, text) in zip([s.march, s.june, s.september, s.december], list) {
                let ms = Int64(iso.date(from: text)!.timeIntervalSince1970 * 1000)
                XCTAssertLessThanOrEqual(abs(got - ms), 2 * 60_000, "\(year) \(text)")
            }
        }
    }
}
