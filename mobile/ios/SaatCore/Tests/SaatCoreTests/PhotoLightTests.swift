import XCTest
@testable import SaatCore

/// Fotoğraf ışığı (9 Kasım 2026): Android PhotoLightTest ile aynı örnekler; tahmin kuralının zorunlu durumları şartnameden
final class PhotoLightTests: XCTestCase {
    /// (alçak, orta, yüksek bulut %, yağış mm/sa) → sonuç; sitedeki ve Android'deki testlerle aynı 14 durum
    func testQualityRule() {
        let cases: [([Double], String)] = [
            ([0, 0, 0, 0], "fair"),
            ([10, 40, 10, 0], "good"),
            ([10, 10, 60, 0], "good"),
            ([30, 85, 90, 0], "fair"),
            ([50, 50, 50, 0], "fair"),
            ([65, 30, 30, 0], "poor"),
            ([80, 0, 0, 0], "poor"),
            ([5, 50, 50, 0.6], "poor"),
            ([39, 20, 0, 0], "good"),
            ([40, 20, 0, 0], "fair"),
            ([0, 80, 0, 0], "good"),
            ([0, 81, 0, 0], "fair"),
            ([74, 0, 0, 0], "poor"),
            ([59, 0, 0, 0], "fair"),
        ]
        for (c, want) in cases {
            XCTAssertEqual(PhotoLight.quality(low: c[0], mid: c[1], high: c[2], precip: c[3]).rawValue, want, "\(c)")
        }
    }

    /// En yakın saatlik örnek; 90 dakikadan uzaksa yok
    func testNearestSample() {
        let h: Int64 = 3_600_000
        let samples = (0..<6).map { PhotoLight.Sample(t: Int64($0) * h, low: Double($0) * 10, mid: 50, high: 0, precip: 0) }
        XCTAssertEqual(PhotoLight.nearest(samples, to: 2 * h + 29 * 60_000)?.t, 2 * h)
        XCTAssertEqual(PhotoLight.nearest(samples, to: 2 * h + 31 * 60_000)?.t, 3 * h)
        XCTAssertEqual(PhotoLight.nearest(samples, to: 5 * h + 80 * 60_000)?.t, 5 * h)
        XCTAssertNil(PhotoLight.nearest(samples, to: 5 * h + 91 * 60_000))
        XCTAssertNil(PhotoLight.nearest([], to: 0))
        // 1. saat: alçak bulut 10, orta 50 → iyi; 5. saat: alçak 50 → orta
        XCTAssertEqual(PhotoLight.quality(at: h, in: samples), .good)
        XCTAssertEqual(PhotoLight.quality(at: 5 * h, in: samples), .fair)
    }

    /// İstanbul 30 Ekim 2026: altın saat Mevsimler'deki saatler, gün doğumu −0,833°, yönler sitedeki sunAltAz ile aynı
    func testIstanbul() throws {
        let d = PhotoLight.day(year: 2026, month: 10, day: 30, latitude: 41.01, longitude: 28.98, utcOffset: 3)
        func m(_ x: Double?) -> Int { Int(jsRound((x ?? 0) * 60)) }
        XCTAssertEqual(m(d.light.goldenAm.0), 7 * 60 + 15)
        XCTAssertEqual(m(d.light.goldenAm.1), 8 * 60 + 11)
        XCTAssertEqual(m(d.light.goldenPm.0), 17 * 60 + 24)
        XCTAssertEqual(m(d.light.bluePm.1), 18 * 60 + 31)
        let rise = try XCTUnwrap(d.sunrise), set = try XCTUnwrap(d.sunset)
        XCTAssertEqual(rise, 7.538731793789072, accuracy: 1e-6)
        XCTAssertEqual(set, 18.04354430372772, accuracy: 1e-6)
        XCTAssertEqual(try XCTUnwrap(d.riseAzimuth), 107.62759669943475, accuracy: 1e-6)
        XCTAssertEqual(try XCTUnwrap(d.setAzimuth), 252.1790509079625, accuracy: 1e-6)
        XCTAssertEqual(d.polar, 0)
        // Gün doğumu sabah altın saatinin içinde, gün batımı akşam altın saatinin içinde
        XCTAssertTrue(rise > (d.light.goldenAm.0 ?? 99) && rise < (d.light.goldenAm.1 ?? 0))
        XCTAssertTrue(set > (d.light.goldenPm.0 ?? 99) && set < (d.light.goldenPm.1 ?? 0))
    }

    /// Tromsø: 21 Aralık kutup gecesi (altın saat yalnız başlıyor), 21 Haziran gece yarısı güneşi; Helsinki mavi saat gece yarısını geçer
    func testPolar() throws {
        let night = PhotoLight.day(year: 2026, month: 12, day: 21, latitude: 69.65, longitude: 18.96, utcOffset: 1)
        XCTAssertNil(night.sunrise)
        XCTAssertNil(night.sunset)
        XCTAssertNil(night.riseAzimuth)
        XCTAssertEqual(night.polar, -1)
        XCTAssertNotNil(night.light.goldenAm.0)
        XCTAssertNil(night.light.goldenAm.1)
        let day = PhotoLight.day(year: 2026, month: 6, day: 21, latitude: 69.65, longitude: 18.96, utcOffset: 2)
        XCTAssertNil(day.sunset)
        XCTAssertEqual(day.polar, 1)
        XCTAssertNil(day.light.bluePm.0)
        let helsinki = PhotoLight.day(year: 2026, month: 6, day: 21, latitude: 60.17, longitude: 24.94, utcOffset: 3)
        XCTAssertEqual(helsinki.polar, 0)
        XCTAssertEqual(try XCTUnwrap(helsinki.light.bluePm.1), 24.705994483292148, accuracy: 1e-6)
        XCTAssertEqual(try XCTUnwrap(helsinki.riseAzimuth), 34.41390618628495, accuracy: 1e-6)
    }
}
