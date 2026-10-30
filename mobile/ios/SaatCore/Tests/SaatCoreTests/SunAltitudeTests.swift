import XCTest
@testable import SaatCore

/// Altın/mavi saat (Güneş yüksekliği anları) ve kerahat vakitleri (30 Ekim 2026)
final class SunAltitudeTests: XCTestCase {
    /// Sitedeki hesapla (src/js/prayer-calc.js sunAltTimes) aynı sonuç: 81 il, dünya şehirleri, dört tarih, üç yükseklik
    func testMatchesWebCalculation() throws {
        let file = try Vectors.load("sun-altitude.json", as: SunAltitudeVectors.self)
        XCTAssertGreaterThan(file.cases.count, 400)
        var none = 0
        for c in file.cases {
            for (i, alt) in file.altitudes.enumerated() {
                let r = PrayerTimes.sunAltitudeTimes(year: c.y, month: c.m, day: c.d, latitude: c.lat, longitude: c.lon,
                                                     utcOffset: c.tz, altitude: alt)
                for (j, got) in [r.rise, r.set].enumerated() {
                    switch (c.t[i * 2 + j], got) {
                    case let (want?, got?): XCTAssertEqual(got, want, accuracy: 1e-6, "\(c.id) \(c.m)/\(c.d) \(alt)")
                    case (nil, nil): none += 1
                    default: XCTFail("\(c.id) \(c.m)/\(c.d) \(alt): var/yok farkı")
                    }
                }
            }
        }
        XCTAssertGreaterThan(none, 0, "yüksek enlemde oluşmayan anlar test verisinde olmalı")
    }

    /// İstanbul 30 Ekim 2026: sabah altın saat 07.15–08.11, akşam 17.24–18.20; kerahat 45 dk
    func testIstanbul() throws {
        let l = PrayerTimes.lightHours(year: 2026, month: 10, day: 30, latitude: 41.01, longitude: 28.98, utcOffset: 3)
        func m(_ x: Double?) -> Int { Int(jsRound((x ?? 0) * 60)) }
        XCTAssertEqual(m(l.goldenAm.0), 7 * 60 + 15)
        XCTAssertEqual(m(l.goldenAm.1), 8 * 60 + 11)
        XCTAssertEqual(m(l.goldenPm.0), 17 * 60 + 24)
        XCTAssertEqual(m(l.bluePm.1), 18 * 60 + 31)
        let day = PrayerTimes.times(year: 2026, month: 10, day: 30, latitude: 41.01, longitude: 28.98, utcOffset: 3)
        let k = PrayerTimes.kerahat(day)
        let israk = try XCTUnwrap(k.israk)
        XCTAssertEqual(israk.upperBound - (day.sunrise ?? 0), 0.75, accuracy: 1e-12)
        let isfirar = try XCTUnwrap(k.isfirar)
        XCTAssertEqual((day.maghrib ?? 0) - isfirar.lowerBound, 0.75, accuracy: 1e-12)
    }
}
