import XCTest
@testable import SaatCore

final class MethodsCrescentTests: XCTestCase {
    /// Hesap yöntemleri ve Hanefî ikindisi: sitedeki methodParams ile aynı sonuç
    func testMethodsMatchWeb() throws {
        let file = try Vectors.load("prayer-methods.json", as: MethodVectors.self)
        XCTAssertGreaterThan(file.cases.count, 700)
        for c in file.cases {
            let p = PrayerTimes.Parameters.method(c.method, hanafi: c.hanafi)
            let day = PrayerTimes.times(year: c.y, month: c.m, day: c.d, latitude: c.lat, longitude: c.lon, utcOffset: c.tz, parameters: p)
            for (i, prayer) in Prayer.allCases.enumerated() {
                let label = "\(c.id) \(c.method) \(c.hanafi) \(c.m)-\(c.d) \(prayer.rawValue)"
                switch (c.t[i], day[prayer]) {
                case let (want?, got?): XCTAssertEqual(want, got, accuracy: 1e-6, label)
                case (nil, nil): break
                default: XCTFail("\(label): web \(String(describing: c.t[i])), Swift \(String(describing: day[prayer]))")
                }
            }
        }
        XCTAssertEqual(PrayerTimes.methodIDs.count, PrayerTimes.methods.count)
    }

    /// Hilal görünürlüğü (Yallop): sitedeki crescent ile aynı gün batımı, Ay batımı (±1 sn), q ve sınıf
    func testCrescentMatchesWeb() throws {
        let file = try Vectors.load("nature.json", as: NatureVectors.self)
        XCTAssertGreaterThan(file.crescent.count, 50)
        var groups: [String: [NatureVectors.Crescent]] = [:]
        var order: [String] = []
        for c in file.crescent {
            let k = "\(c.conj) \(c.lat) \(c.lon)"
            if groups[k] == nil { order.append(k) }
            groups[k, default: []].append(c)
        }
        for k in order {
            let list = groups[k]!
            let got = Nature.crescent(conjunction: list[0].conj, latitude: list[0].lat, longitude: list[0].lon, evenings: 3)
            XCTAssertEqual(list.count, got.count, "akşam sayısı \(k)")
            for (w, g) in zip(list, got) {
                XCTAssertLessThanOrEqual(abs(w.sunset - g.sunset), 1000, "gün batımı \(k)")
                if let ws = w.moonset, let gs = g.moonset { XCTAssertLessThanOrEqual(abs(ws - gs), 1000, "Ay batımı \(k)") } else { XCTAssertEqual(w.moonset == nil, g.moonset == nil, "Ay batımı var/yok \(k)") }
                if let wq = w.q, let gq = g.q { XCTAssertEqual(wq, gq, accuracy: 1e-4, "q \(k)") } else { XCTAssertEqual(w.q == nil, g.q == nil, "q var/yok \(k)") }
                XCTAssertEqual(w.cat, g.category, "sınıf \(k)")
            }
        }
    }
}
