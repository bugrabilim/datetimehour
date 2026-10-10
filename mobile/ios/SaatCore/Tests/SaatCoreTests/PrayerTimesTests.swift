import XCTest
@testable import SaatCore

final class PrayerTimesTests: XCTestCase {
    /// Sitedeki hesapla (src/js/prayer-calc.js) aynı sonuç: 81 il, 30 dünya şehri, kutup bölgeleri
    func testMatchesWebCalculation() throws {
        let file = try Vectors.load("prayer-times.json", as: PrayerVectors.self)
        XCTAssertEqual(file.keys, Prayer.allCases.map(\.rawValue))
        XCTAssertGreaterThan(file.cases.count, 1500)
        var worst = 0.0, missing = 0
        for c in file.cases {
            let day = PrayerTimes.times(year: c.y, month: c.m, day: c.d, latitude: c.lat, longitude: c.lon, utcOffset: c.tz)
            for (i, prayer) in Prayer.allCases.enumerated() {
                let label = "\(c.id) \(c.y)-\(c.m)-\(c.d) \(prayer.rawValue)"
                switch (c.t[i], day[prayer]) {
                case let (want?, got?):
                    worst = max(worst, abs(want - got))
                    XCTAssertEqual(got, want, accuracy: 1e-6, label)
                case (nil, nil):
                    missing += 1
                default:
                    XCTFail("\(label): vakit var/yok farkı (web: \(String(describing: c.t[i])), Swift: \(String(describing: day[prayer])))")
                }
            }
        }
        XCTAssertGreaterThan(missing, 0, "kutup bölgesi durumları test verisinde olmalı")
        print("SaatCore namaz: \(file.cases.count) gün, en büyük fark \(worst) saat")
    }

    /// İstanbul 9 Ekim 2026: Diyanet'in yayımladığı vakitler (±1 dk); scripts/test.mjs ile aynı denetim
    func testIstanbulMatchesDiyanet() throws {
        let day = PrayerTimes.times(year: 2026, month: 10, day: 9, latitude: 41.01, longitude: 28.98, utcOffset: 3)
        let want: [Prayer: (Int, Int)] = [.fajr: (5, 37), .sunrise: (7, 2), .dhuhr: (12, 57), .asr: (16, 7), .maghrib: (18, 41), .isha: (20, 0)]
        for (prayer, hm) in want {
            let got = try XCTUnwrap(day.minutes(prayer))
            XCTAssertLessThanOrEqual(abs(got - (hm.0 * 60 + hm.1)), 1, prayer.rawValue)
        }
    }

    /// Saat dilimi nesnesiyle çağrı, sabit farkla aynı sonucu verir
    func testTimeZoneVariant() {
        let a = PrayerTimes.times(year: 2026, month: 10, day: 9, latitude: 41.01, longitude: 28.98, utcOffset: 3)
        let b = PrayerTimes.times(year: 2026, month: 10, day: 9, latitude: 41.01, longitude: 28.98, timeZone: TimeZone(secondsFromGMT: 3 * 3600)!)
        XCTAssertEqual(a, b)
    }

    /// Reykjavik yaz gündönümü: imsak/yatsı açısı oluşmaz, yedide bir kuralıyla bütün vakitler dolu
    func testHighLatitudeRule() {
        let day = PrayerTimes.times(year: 2027, month: 6, day: 21, latitude: 64.1, longitude: -21.9, utcOffset: 0)
        for prayer in Prayer.allCases { XCTAssertNotNil(day[prayer], prayer.rawValue) }
    }
}
