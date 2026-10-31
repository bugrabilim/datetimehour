import XCTest
@testable import SaatCore

final class QiblaTests: XCTestCase {
    /// Sitedeki hesapla (src/js/prayer-calc.js) aynı sonuç: 81 il, dünya şehirleri, dört tarih
    func testMatchesWebCalculation() throws {
        let file = try Vectors.load("qibla.json", as: QiblaVectors.self)
        XCTAssertGreaterThan(file.cases.count, 100)
        var none = 0
        for c in file.cases {
            let q = Qibla.direction(latitude: c.lat, longitude: c.lon)
            XCTAssertEqual(q.bearing, c.bearing, accuracy: 1e-9, c.id)
            XCTAssertEqual(q.km, c.km, accuracy: 1e-6, c.id)
            for (i, d) in file.dates.enumerated() {
                let got = Qibla.time(year: d[0], month: d[1], day: d[2], latitude: c.lat, longitude: c.lon, utcOffset: c.tz[i])
                switch (c.time[i], got) {
                case let (want?, got?): XCTAssertEqual(got, want, accuracy: 1e-6, "\(c.id) \(d)")
                case (nil, nil): none += 1
                default: XCTFail("\(c.id) \(d): kıble saati var/yok farkı")
                }
            }
        }
        XCTAssertGreaterThan(none, 0, "kıble saati olmayan yerler test verisinde olmalı")
    }

    /// İstanbul: kıble 151,6°, Kâbe'ye ~2405 km; 28 Ekim 2026 kıble saati Diyanet'te 11.09
    func testIstanbul() throws {
        let q = Qibla.direction(latitude: 41.01, longitude: 28.98)
        XCTAssertEqual(q.bearing, 151.6, accuracy: 0.1)
        XCTAssertEqual(q.km, 2405, accuracy: 5)
        let t = try XCTUnwrap(Qibla.time(year: 2026, month: 10, day: 28, latitude: 41.01, longitude: 28.98, utcOffset: 3))
        XCTAssertLessThanOrEqual(abs(Int(jsRound(t * 60)) - (11 * 60 + 9)), 1)
    }
}
