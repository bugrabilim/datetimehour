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

    /// Ay'ın yer merkezli konumu (derece): sağ açıklık, dik açıklık, yatay paralaks.
    public struct MoonPosition: Sendable, Equatable {
        public let rightAscension: Double
        public let declination: Double
        public let parallax: Double
    }

    /// Ay'ın doğuşu ve batışı (UTC milisaniye; o aralıkta olmuyorsa nil).
    public struct MoonRiseSet: Sendable, Equatable {
        public let rise: Int64?
        public let set: Int64?
    }

    /// Meeus bölüm 47, kısaltılmış seri: D, M, M', F, boylam (1e-6°), uzaklık (1e-3 km)
    static let moonLR: [(Double, Double, Double, Double, Double, Double)] = [
        (0, 0, 1, 0, 6288774, -20905355), (2, 0, -1, 0, 1274027, -3699111), (2, 0, 0, 0, 658314, -2955968), (0, 0, 2, 0, 213618, -569925),
        (0, 1, 0, 0, -185116, 48888), (0, 0, 0, 2, -114332, -3149), (2, 0, -2, 0, 58793, 246158), (2, -1, -1, 0, 57066, -152138),
        (2, 0, 1, 0, 53322, -170733), (2, -1, 0, 0, 45758, -204586), (0, 1, -1, 0, -40923, -129620), (1, 0, 0, 0, -34720, 108743),
        (0, 1, 1, 0, -30383, 104755), (2, 0, 0, -2, 15327, 10321), (0, 0, 1, 2, -12528, 0), (0, 0, 1, -2, 10980, 79661),
        (4, 0, -1, 0, 10675, -34782), (0, 0, 3, 0, 10034, -23210), (4, 0, -2, 0, 8548, -21636), (2, 1, -1, 0, -7888, 24208),
        (2, 1, 0, 0, -6766, 30824), (1, 0, -1, 0, -5163, -8379), (1, 1, 0, 0, 4987, -16675), (2, -1, 1, 0, 4036, -12831),
        (2, 0, 2, 0, 3994, -10445), (4, 0, 0, 0, 3861, -11650), (2, 0, -3, 0, 3665, 14403), (0, 1, -2, 0, -2689, -7003),
        (2, 0, -1, 2, -2602, 0), (2, -1, -2, 0, 2390, 10056), (1, 0, 1, 0, -2348, 6322), (2, -2, 0, 0, 2236, -9884),
    ]
    /// D, M, M', F, enlem (1e-6°)
    static let moonB: [(Double, Double, Double, Double, Double)] = [
        (0, 0, 0, 1, 5128122), (0, 0, 1, 1, 280602), (0, 0, 1, -1, 277693), (2, 0, 0, -1, 173237), (2, 0, -1, 1, 55413),
        (2, 0, -1, -1, 46271), (2, 0, 0, 1, 32573), (0, 0, 2, 1, 17198), (2, 0, 1, -1, 9266), (0, 0, 2, -1, 8822),
        (2, -1, 0, -1, 8216), (2, 0, -2, -1, 4324), (2, 0, 1, 1, 4200), (2, 1, 0, -1, -3359), (2, -1, -1, 1, 2463),
        (2, -1, 0, 1, 2211), (2, -1, -1, -1, 2065), (0, 1, -1, -1, -1870), (4, 0, -1, -1, 1828), (0, 1, 0, 1, -1794),
        (0, 0, 0, 3, -1749), (0, 1, -1, 1, -1565), (1, 0, 0, 1, -1491), (0, 1, 1, 1, -1475), (0, 1, 1, -1, -1410),
        (0, 1, 0, -1, -1344), (1, 0, 0, -1, -1335), (0, 0, 3, 1, 1107),
    ]

    /// ms anında Ay'ın konumu (sitedeki moonPos ile aynı işlem sırası)
    public static func moonPosition(atMilliseconds ms: Double) -> MoonPosition {
        let T = (ms / dayMs + unixJD + deltaT / 86400 - 2451545) / 36525
        let T2 = T * T, T3 = T2 * T, T4 = T3 * T
        let Lp = 218.3164477 + 481267.88123421 * T - 0.0015786 * T2 + T3 / 538841 - T4 / 65194000
        let D = 297.8501921 + 445267.1114034 * T - 0.0018819 * T2 + T3 / 545868 - T4 / 113065000
        let M = 357.5291092 + 35999.0502909 * T - 0.0001536 * T2 + T3 / 24490000
        let Mp = 134.9633964 + 477198.8675055 * T + 0.0087414 * T2 + T3 / 69699 - T4 / 14712000
        let F = 93.2720950 + 483202.0175233 * T - 0.0036539 * T2 - T3 / 3526000 + T4 / 863310000
        let A1 = 119.75 + 131.849 * T, A2 = 53.09 + 479264.290 * T, A3 = 313.45 + 481266.484 * T
        let E = 1 - 0.002516 * T - 0.0000074 * T2
        var sl = 0.0, sr = 0.0, sb = 0.0
        for t in moonLR {
            let arg = t.0 * D + t.1 * M + t.2 * Mp + t.3 * F
            let e = t.1 == 0 ? 1 : (t.1 == 2 || t.1 == -2 ? E * E : E)
            sl += t.4 * e * dsin(arg)
            sr += t.5 * e * dcos(arg)
        }
        for t in moonB {
            let arg = t.0 * D + t.1 * M + t.2 * Mp + t.3 * F
            let e = t.1 == 0 ? 1 : (t.1 == 2 || t.1 == -2 ? E * E : E)
            sb += t.4 * e * dsin(arg)
        }
        sl += 3958 * dsin(A1) + 1962 * dsin(Lp - F) + 318 * dsin(A2)
        sb += -2235 * dsin(Lp) + 382 * dsin(A3) + 175 * dsin(A1 - F) + 175 * dsin(A1 + F) + 127 * dsin(Lp - Mp) - 115 * dsin(Lp + Mp)
        let lam = Lp + sl / 1000000, bet = sb / 1000000, dist = 385000.56 + sr / 1000
        let eps = 23.4392911 - 0.0130042 * T
        let ra = atan2(dsin(lam) * dcos(eps) - (dsin(bet) / dcos(bet)) * dsin(eps), dcos(lam)) / deg2rad
        let dec = asin(dsin(bet) * dcos(eps) + dcos(bet) * dsin(eps) * dsin(lam)) / deg2rad
        return MoonPosition(rightAscension: ra, declination: dec, parallax: asin(6378.14 / dist) / deg2rad)
    }

    /// Ufka göre yükseklik eksi doğuş/batış eşiği (Meeus bölüm 15); 0'dan büyükse Ay ufkun üstünde
    public static func moonAltitude(_ ms: Double, lat: Double, lon: Double) -> Double {
        let p = moonPosition(atMilliseconds: ms)
        let d = ms / dayMs + unixJD - 2451545, T = d / 36525
        let gmst = 280.46061837 + 360.98564736629 * d + 0.000387933 * T * T
        let sh = dsin(lat) * dsin(p.declination) + dcos(lat) * dcos(p.declination) * dcos(gmst + lon - p.rightAscension)
        return asin(sh) / deg2rad - (0.7275 * p.parallax - 0.5667)
    }

    /// `start`tan başlayan `span` (varsayılan 24 saat) içinde Ay'ın ilk doğuşu ve batışı
    public static func moonRiseSet(startMilliseconds start: Int64, latitude lat: Double, longitude lon: Double,
                                   spanMilliseconds span: Int64 = 86_400_000) -> MoonRiseSet {
        let step = 600000.0, n = Int(jsRound(Double(span) / step))
        var rise: Int64?, set: Int64?
        var t0 = Double(start), f0 = moonAltitude(t0, lat: lat, lon: lon)
        if n > 0 {
            for i in 1...n {
                let t1 = Double(start) + Double(i) * step, f1 = moonAltitude(t1, lat: lat, lon: lon)
                if (f0 < 0) != (f1 < 0) {
                    let isRise = f0 < 0
                    if (isRise ? rise : set) == nil {
                        var a = t0, b = t1, fa = f0
                        for _ in 0..<12 {
                            let m = (a + b) / 2, fm = moonAltitude(m, lat: lat, lon: lon)
                            if (fa < 0) == (fm < 0) { a = m; fa = fm } else { b = m }
                        }
                        let at = Int64(jsRound((a + b) / 2))
                        if isRise { rise = at } else { set = at }
                    }
                }
                t0 = t1
                f0 = f1
            }
        }
        return MoonRiseSet(rise: rise, set: set)
    }
}

public extension Nature {
    /// Güneş'in yer merkezli konumu (Meeus bölüm 25, düşük duyarlık): sağ açıklık, dik açıklık (derece), uzaklık (AB).
    struct SunPosition: Sendable, Equatable {
        public let rightAscension: Double
        public let declination: Double
        public let distance: Double
    }

    static func sunPosition(atMilliseconds ms: Double) -> SunPosition {
        let T = (ms / dayMs + unixJD + deltaT / 86400 - 2451545) / 36525
        let L0 = 280.46646 + 36000.76983 * T + 0.0003032 * T * T
        let M = 357.52911 + 35999.05029 * T - 0.0001537 * T * T
        let e = 0.016708634 - 0.000042037 * T
        let C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * dsin(M) + (0.019993 - 0.000101 * T) * dsin(2 * M) + 0.000289 * dsin(3 * M)
        let nu = M + C, R = 1.000001018 * (1 - e * e) / (1 + e * dcos(nu))
        let Om = 125.04 - 1934.136 * T, lam = L0 + C - 0.00569 - 0.00478 * dsin(Om)
        let eps = 23.4392911 - 0.0130042 * T + 0.00256 * dcos(Om)
        let ra = atan2(dcos(eps) * dsin(lam), dcos(lam)) / deg2rad
        let dec = asin(dsin(eps) * dsin(lam)) / deg2rad
        return SunPosition(rightAscension: ra, declination: dec, distance: R)
    }

    /// Yerel yükseklik ve azimut (derece; azimut kuzeyden saat yönünde), havasız, yer merkezli.
    static func altAz(_ ms: Double, ra: Double, dec: Double, lat: Double, lon: Double) -> (alt: Double, az: Double) {
        let d = ms / dayMs + unixJD - 2451545, T = d / 36525
        let H = 280.46061837 + 360.98564736629 * d + 0.000387933 * T * T + lon - ra
        let alt = asin(dsin(lat) * dsin(dec) + dcos(lat) * dcos(dec) * dcos(H)) / deg2rad
        let az = atan2(dsin(H), dcos(H) * dsin(lat) - tan(dec * deg2rad) * dcos(lat)) / deg2rad + 180
        return (alt, (az.truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360))
    }

    internal static func sunAltitude(_ ms: Double, lat: Double, lon: Double) -> Double {
        let p = sunPosition(atMilliseconds: ms)
        return altAz(ms, ra: p.rightAscension, dec: p.declination, lat: lat, lon: lon).alt + 0.8333
    }

    /// `start`tan sonraki `span` içinde f'nin ilk + → − geçişi (batış); 10 dk adım, 12 kez ikiye bölme.
    internal static func firstSet(_ f: (Double, Double, Double) -> Double, start: Double, span: Double, lat: Double, lon: Double) -> Int64? {
        let step = 600000.0
        var t0 = start, f0 = f(t0, lat, lon)
        var t1 = start + step
        while t1 <= start + span {
            let f1 = f(t1, lat, lon)
            if f0 >= 0 && f1 < 0 {
                var a = t0, b = t1
                for _ in 0..<12 {
                    let m = (a + b) / 2
                    if f(m, lat, lon) >= 0 { a = m } else { b = m }
                }
                return Int64(jsRound((a + b) / 2))
            }
            t0 = t1
            f0 = f1
            t1 += step
        }
        return nil
    }

    /// Hilal görünürlüğünde bir akşam (Yallop q ölçütü; sitedeki `crescent` ile aynı).
    struct CrescentEvening: Sendable, Equatable {
        public let sunset: Int64
        public let moonset: Int64?
        /// Gün batımında Ay'ın yaşı (saat; kavuşumdan beri)
        public let age: Double
        /// Ay batımı − gün batımı (dakika)
        public let lag: Double?
        /// Gün batımında Ay'ın yerel yüksekliği (derece)
        public let altitude: Double
        /// Güneş–Ay uzanımı (derece)
        public let elongation: Double
        public let width: Double?
        public let q: Double?
        /// A–F
        public let category: String
    }

    static let yallop: [(Double, String)] = [(0.216, "A"), (-0.014, "B"), (-0.160, "C"), (-0.232, "D"), (-0.293, "E")]

    internal static func separation(_ a1: Double, _ d1: Double, _ a2: Double, _ d2: Double) -> Double {
        let c = dsin(d1) * dsin(d2) + dcos(d1) * dcos(d2) * dcos(a1 - a2)
        return acos(max(-1, min(1, c))) / deg2rad
    }

    internal static func crescentEvening(conjunction conj: Int64, sunset ss: Int64, lat: Double, lon: Double) -> CrescentEvening {
        let sunsetMs = Double(ss)
        let age = Double(ss - conj) / 3600000
        let moonset = firstSet({ moonAltitude($0, lat: $1, lon: $2) }, start: sunsetMs - 6 * 3600000, span: 12 * 3600000, lat: lat, lon: lon)
        let ms0 = moonPosition(atMilliseconds: sunsetMs), s0 = sunPosition(atMilliseconds: sunsetMs)
        let mh = altAz(sunsetMs, ra: ms0.rightAscension, dec: ms0.declination, lat: lat, lon: lon)
        let alt = mh.alt - ms0.parallax * dcos(mh.alt)
        var arcl = separation(ms0.rightAscension, ms0.declination, s0.rightAscension, s0.declination)
        let lag = moonset.map { Double($0 - ss) / 60000 }
        guard age > 0, let lag, lag > 0 else {
            return CrescentEvening(sunset: ss, moonset: moonset, age: age, lag: lag, altitude: alt, elongation: arcl, width: nil, q: nil, category: "F")
        }
        let tb = sunsetMs + (4.0 / 9.0) * lag * 60000
        let m = moonPosition(atMilliseconds: tb), s = sunPosition(atMilliseconds: tb)
        let mp = altAz(tb, ra: m.rightAscension, dec: m.declination, lat: lat, lon: lon)
        let sp = altAz(tb, ra: s.rightAscension, dec: s.declination, lat: lat, lon: lon)
        let arcv = mp.alt - sp.alt, daz = sp.az - mp.az
        let altT = mp.alt - m.parallax * dcos(mp.alt)
        let arclT = acos(max(-1, min(1, dcos(altT - sp.alt) * dcos(daz)))) / deg2rad
        let sd = 0.27245 * m.parallax * 60 * (1 + dsin(altT) * dsin(m.parallax))
        let w = sd * (1 - dcos(arclT))
        let q = (arcv - (11.8371 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w * w * w)) / 10
        arcl = separation(m.rightAscension, m.declination, s.rightAscension, s.declination)
        let cat = yallop.first { q > $0.0 }?.1 ?? "F"
        return CrescentEvening(sunset: ss, moonset: moonset, age: age, lag: lag, altitude: alt, elongation: arcl, width: w, q: q, category: cat)
    }

    /// Kavuşum (yeni ay) sonrasındaki ilk `evenings` akşam için hilal görünürlüğü; gün batımı yoksa (kutup) kısalır.
    static func crescent(conjunction conj: Int64, latitude lat: Double, longitude lon: Double, evenings n: Int = 3) -> [CrescentEvening] {
        var list: [CrescentEvening] = []
        var t = Double(conj)
        for _ in 0..<n {
            guard let ss = firstSet({ sunAltitude($0, lat: $1, lon: $2) }, start: t, span: 30 * 3600000, lat: lat, lon: lon) else { break }
            list.append(crescentEvening(conjunction: conj, sunset: ss, lat: lat, lon: lon))
            t = Double(ss) + 3600000
        }
        return list
    }
}

public extension Date {
    /// UTC milisaniyeden tarih.
    init(unixMilliseconds ms: Int64) { self.init(timeIntervalSince1970: Double(ms) / 1000) }
}
