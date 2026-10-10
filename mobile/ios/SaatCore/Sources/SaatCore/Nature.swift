import Foundation

/// Doğa hesapları (dış veri yok): ekinoks/gündönümü anları (Meeus, Astronomical Algorithms bölüm 27)
/// ve yeni ay/dolunay anları (bölüm 49). Sitedeki `src/js/nature-calc.js` dosyasının birebir karşılığıdır;
/// sonuçlar `mobile/shared/testdata/nature.json` ile doğrulanır. Zamanlar UTC milisaniyedir (Unix epoch).
public enum Nature {
    /// Yılın dört mevsim anı (UTC milisaniye).
    public struct Seasons: Sendable, Equatable {
        public let march: Int64
        public let june: Int64
        public let september: Int64
        public let december: Int64
    }

    /// Bir andaki Ay durumu.
    public struct Moon: Sendable, Equatable {
        /// Son yeni aydan bu yana geçen gün
        public let age: Double
        /// Döngüde konum (0 = yeni ay, 0,5 = dolunay)
        public let fraction: Double
        /// Aydınlanan kısım (0…1)
        public let illumination: Double
        /// Evre: 0 yeni ay, 1 büyüyen hilal, 2 ilk dördün, 3 büyüyen şişkin, 4 dolunay, 5 küçülen şişkin, 6 son dördün, 7 küçülen hilal
        public let phaseIndex: Int
        public let nextNewMoon: Int64
        public let nextFullMoon: Int64
    }

    static let unixJD = 2440587.5
    /// saniye (2026 dolayı; yalnız TT→UT için)
    static let deltaT = 69.0
    static let dayMs = 86400000.0

    static func jdToMs(_ jd: Double) -> Double { (jd - unixJD) * 86400000 }

    static let seasonTerms: [(Double, Double, Double)] = [
        (485, 324.96, 1934.136), (203, 337.23, 32964.467), (199, 342.08, 20.186), (182, 27.85, 445267.112),
        (156, 73.14, 45036.886), (136, 171.52, 22518.443), (77, 222.54, 65928.934), (74, 296.72, 3034.906),
        (70, 243.58, 9037.513), (58, 119.81, 33718.147), (52, 297.17, 150.678), (50, 21.02, 2281.226),
        (45, 247.54, 29929.562), (44, 325.15, 31555.956), (29, 60.93, 4443.417), (18, 155.12, 67555.328),
        (17, 288.79, 4562.452), (16, 198.04, 62894.029), (14, 199.76, 31436.921), (12, 95.39, 14577.848),
        (12, 287.11, 31931.756), (12, 320.81, 34777.259), (9, 227.73, 1222.114), (8, 15.45, 16859.074),
    ]

    /// Verilen yılın dört mevsim anı.
    public static func seasons(year: Int) -> Seasons {
        let y = Double(year - 2000) / 1000
        let bases = [
            2451623.80984 + 365242.37404 * y + 0.05169 * y * y - 0.00411 * y * y * y - 0.00057 * y * y * y * y,
            2451716.56767 + 365241.62603 * y + 0.00325 * y * y + 0.00888 * y * y * y - 0.00030 * y * y * y * y,
            2451810.21715 + 365242.01767 * y - 0.11575 * y * y + 0.00337 * y * y * y + 0.00078 * y * y * y * y,
            2451900.05952 + 365242.74049 * y - 0.06223 * y * y - 0.00823 * y * y * y + 0.00032 * y * y * y * y,
        ]
        let out = bases.map { jde0 -> Int64 in
            let T = (jde0 - 2451545.0) / 36525
            let W = 35999.373 * T - 2.47
            let dl = 1 + 0.0334 * dcos(W) + 0.0007 * dcos(2 * W)
            var S = 0.0
            for t in seasonTerms { S += t.0 * dcos(t.1 + t.2 * T) }
            let jde = jde0 + (0.00001 * S) / dl
            return Int64(jsRound(jdToMs(jde) - deltaT * 1000))
        }
        return Seasons(march: out[0], june: out[1], september: out[2], december: out[3])
    }

    /// Yeni ay / dolunay JDE değeri: k tam sayı = yeni ay, k + 0,5 = dolunay
    static func phaseJDE(_ k: Double, full: Bool) -> Double {
        let T = k / 1236.85
        let jde = 2451550.09766 + 29.530588861 * k + 0.00015437 * T * T - 0.00000015 * T * T * T + 0.00000000073 * T * T * T * T
        let E = 1 - 0.002516 * T - 0.0000074 * T * T
        let M = 2.5534 + 29.10535670 * k - 0.0000014 * T * T - 0.00000011 * T * T * T
        let Mp = 201.5643 + 385.81693528 * k + 0.0107582 * T * T + 0.00001238 * T * T * T - 0.000000058 * T * T * T * T
        let F = 160.7108 + 390.67050284 * k - 0.0016118 * T * T - 0.00000227 * T * T * T + 0.000000011 * T * T * T * T
        let Om = 124.7746 - 1.56375588 * k + 0.0020672 * T * T + 0.00000215 * T * T * T
        // Düzeltme terimleri JS'teki sırayla soldan sağa toplanır (x - y ile x + (-y) aynı sonucu verir)
        let a: [Double] = full
            ? [-0.40614, 0.17302, 0.01614, 0.01043, 0.00734, -0.00515, 0.00209]
            : [-0.40720, 0.17241, 0.01608, 0.01039, 0.00739, -0.00514, 0.00208]
        let terms: [Double] = [
            a[0] * dsin(Mp), a[1] * E * dsin(M), a[2] * dsin(2 * Mp), a[3] * dsin(2 * F),
            a[4] * E * dsin(Mp - M), a[5] * E * dsin(Mp + M), a[6] * E * E * dsin(2 * M),
            -0.00111 * dsin(Mp - 2 * F), -0.00057 * dsin(Mp + 2 * F), 0.00056 * E * dsin(2 * Mp + M),
            -0.00042 * dsin(3 * Mp), 0.00042 * E * dsin(M + 2 * F), 0.00038 * E * dsin(M - 2 * F),
            -0.00024 * E * dsin(2 * Mp - M), -0.00017 * dsin(Om),
        ]
        var c = 0.0
        for t in terms { c += t }
        return jde + c
    }

    /// k. yeni ayın (ya da dolunayın) anı, UTC milisaniye.
    public static func phaseMilliseconds(k: Int, full: Bool) -> Int64 {
        Int64(jsRound(jdToMs(phaseJDE(Double(k) + (full ? 0.5 : 0), full: full)) - deltaT * 1000))
    }

    static func k(for ms: Int64) -> Int {
        Int(floor((Double(ms) / dayMs + unixJD - 2451550.09766) / 29.530588861))
    }

    /// Verilen andaki Ay durumu.
    public static func moon(atMilliseconds ms: Int64) -> Moon {
        var k = k(for: ms) - 1
        var prevNew: Int64 = 0, nextNew: Int64 = 0
        for _ in 0..<4 {
            let t = phaseMilliseconds(k: k, full: false)
            if t <= ms { prevNew = t } else { nextNew = t; break }
            k += 1
        }
        let len = Double(nextNew - prevNew), frac = Double(ms - prevNew) / len
        let illum = (1 - cos(2 * Double.pi * frac)) / 2
        let idx = Int(floor(frac * 8 + 0.5)) % 8
        var kk = Self.k(for: ms) - 1
        var nextFull: Int64 = 0
        for _ in 0..<4 {
            let f = phaseMilliseconds(k: kk, full: true)
            if f > ms { nextFull = f; break }
            kk += 1
        }
        return Moon(age: Double(ms - prevNew) / dayMs, fraction: frac, illumination: illum, phaseIndex: idx,
                    nextNewMoon: nextNew, nextFullMoon: nextFull)
    }

    public static func moon(at date: Date) -> Moon {
        moon(atMilliseconds: Int64(jsRound(date.timeIntervalSince1970 * 1000)))
    }
}

public extension Date {
    /// UTC milisaniyeden tarih.
    init(unixMilliseconds ms: Int64) { self.init(timeIntervalSince1970: Double(ms) / 1000) }
}
