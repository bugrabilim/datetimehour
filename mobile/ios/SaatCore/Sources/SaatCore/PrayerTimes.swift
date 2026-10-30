import Foundation

/// Namaz vakitleri ve güneş vakitleri.
public enum Prayer: String, CaseIterable, Sendable, Codable {
    case fajr, sunrise, dhuhr, asr, maghrib, isha
}

/// Bir günün vakitleri: yerel saat, ondalık saat (5.5 = 05:30).
/// Kutup bölgelerinde vakit oluşmuyorsa `nil` (ör. gece yarısı güneşinde güneş doğuşu).
public struct PrayerDay: Sendable, Equatable {
    public var fajr: Double?
    public var sunrise: Double?
    public var dhuhr: Double?
    public var asr: Double?
    public var maghrib: Double?
    public var isha: Double?

    public subscript(prayer: Prayer) -> Double? {
        switch prayer {
        case .fajr: fajr
        case .sunrise: sunrise
        case .dhuhr: dhuhr
        case .asr: asr
        case .maghrib: maghrib
        case .isha: isha
        }
    }

    /// Gece yarısından itibaren dakika; sitedeki gibi yuvarlanır (Math.round(saat × 60)).
    public func minutes(_ prayer: Prayer) -> Int? {
        self[prayer].map { Int(jsRound($0 * 60)) }
    }
}

/// Namaz vakti hesabı (astronomik, dış veri yok). Sitedeki `src/js/prayer-calc.js` dosyasının birebir karşılığıdır;
/// sonuçlar `mobile/shared/testdata/prayer-times.json` ile doğrulanır. Formül değişirse önce JS değişir, test verisi yeniden üretilir.
/// Yöntem: imsak 18°, yatsı 17° (Diyanet ile uyumlu), ikindi gölge katsayısı 1, resmî vakitlere uyum için dakika düzeltmeleri.
/// Yüksek enlemde imsak/yatsı açısı oluşmazsa gecenin yedide biri kuralı uygulanır.
public enum PrayerTimes {
    /// Dakika cinsinden düzeltmeler (resmî vakitlerle karşılaştırılarak ayarlandı).
    public struct Adjustments: Sendable, Equatable {
        public var fajr = 0.0, sunrise = -7.1, dhuhr = 5.1, asr = 4.5, maghrib = 7.9, isha = 0.9
        public init() {}
    }

    public struct Parameters: Sendable, Equatable {
        public var fajrAngle = 18.0
        public var ishaAngle = 17.0
        public var sunriseAngle = 0.833
        public var sunsetAngle = 0.833
        public var asrFactor = 1.0
        public var adjustments = Adjustments()
        public init() {}
        /// Diyanet ile uyumlu varsayılanlar.
        public static let diyanet = Parameters()
    }

    /// Bir gün için vakitler.
    /// - Parameter utcOffset: o gün UTC'ye göre saat farkı (Türkiye için 3).
    public static func times(year: Int, month: Int, day: Int, latitude lat: Double, longitude lon: Double,
                             utcOffset tz: Double, parameters P: Parameters = .diyanet) -> PrayerDay {
        let jd = julian(year, month, day) - lon / 360
        func midDay(_ t: Double) -> Double { fixHour(12 - sunPosition(jd + t).eqt) }
        // alt: ufkun altındaki açı (derece, pozitif = altında)
        func angleTime(_ alt: Double, _ t: Double, ccw: Bool) -> Double {
            let decl = sunPosition(jd + t).decl
            let a = (-dsin(alt) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            if a < -1 || a > 1 { return .nan }
            return midDay(t) + (ccw ? -1 : 1) * dacos(a) / 15
        }
        func asrTime(_ t: Double) -> Double {
            let decl = sunPosition(jd + t).decl
            let altAbove = dacot(P.asrFactor + dtan(abs(lat - decl)))
            return angleTime(-altAbove, t, ccw: false)
        }

        var fajr = angleTime(P.fajrAngle, 5.0 / 24, ccw: true)
        var sunrise = angleTime(P.sunriseAngle, 6.0 / 24, ccw: true)
        var dhuhr = midDay(12.0 / 24)
        var asr = asrTime(13.0 / 24)
        var maghrib = angleTime(P.sunsetAngle, 18.0 / 24, ccw: false)
        var isha = angleTime(P.ishaAngle, 18.0 / 24, ccw: false)

        let off = tz - lon / 15
        fajr += off; sunrise += off; dhuhr += off; asr += off; maghrib += off; isha += off

        // Yüksek enlemlerde (beyaz geceler) imsak/yatsı açısı hiç oluşmuyorsa: gecenin yedide biri kuralı
        if !sunrise.isNaN && !maghrib.isNaN {
            let night = 24 - (maghrib - sunrise)
            if fajr.isNaN { fajr = sunrise - night / 7 }
            if isha.isNaN { isha = maghrib + night / 7 }
        }
        let a = P.adjustments
        fajr += a.fajr / 60; sunrise += a.sunrise / 60; dhuhr += a.dhuhr / 60
        asr += a.asr / 60; maghrib += a.maghrib / 60; isha += a.isha / 60

        func value(_ x: Double) -> Double? { x.isNaN ? nil : x }
        return PrayerDay(fajr: value(fajr), sunrise: value(sunrise), dhuhr: value(dhuhr),
                         asr: value(asr), maghrib: value(maghrib), isha: value(isha))
    }

    /// Bir gün için vakitler; saat dilimi farkı sitedeki gibi o günün UTC öğlesindeki farktır.
    public static func times(year: Int, month: Int, day: Int, latitude: Double, longitude: Double,
                             timeZone: TimeZone, parameters: Parameters = .diyanet) -> PrayerDay {
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(secondsFromGMT: 0)!
        let noon = utc.date(from: DateComponents(year: year, month: month, day: day, hour: 12)) ?? Date()
        let offset = Double(timeZone.secondsFromGMT(for: noon)) / 3600
        return times(year: year, month: month, day: day, latitude: latitude, longitude: longitude,
                     utcOffset: offset, parameters: parameters)
    }

    /// Güneş'in bir yüksekliğe sabah çıktığı ve akşam indiği an (yerel ondalık saat; oluşmuyorsa nil).
    public struct AltitudeTimes: Sendable, Equatable {
        public var rise: Double?
        public var set: Double?
    }

    /// Güneş'in `altitude` dereceye (ufkun üstü pozitif) sabah çıktığı ve akşam indiği an; düzeltmesiz, ikinci
    /// yaklaşımla. Sitedeki `sunAltTimes` ile birebir (test verisi `sun-altitude.json`).
    public static func sunAltitudeTimes(year: Int, month: Int, day: Int, latitude lat: Double, longitude lon: Double,
                                        utcOffset tz: Double, altitude alt: Double) -> AltitudeTimes {
        let jd = julian(year, month, day) - lon / 360
        func at(_ t: Double, ccw: Bool) -> Double {
            let p = sunPosition(jd + t)
            let a = (dsin(alt) - dsin(p.decl) * dsin(lat)) / (dcos(p.decl) * dcos(lat))
            if a < -1 || a > 1 { return .nan }
            return fixHour(12 - p.eqt) + (ccw ? -1 : 1) * dacos(a) / 15
        }
        var rise = at(6.0 / 24, ccw: true)
        var set = at(18.0 / 24, ccw: false)
        if !rise.isNaN { rise = at(rise / 24, ccw: true) }
        if !set.isNaN { set = at(set / 24, ccw: false) }
        let off = tz - lon / 15
        return AltitudeTimes(rise: rise.isNaN ? nil : rise + off, set: set.isNaN ? nil : set + off)
    }

    /// Altın saat (Güneş +6° ile −4° arası) ve mavi saat (−4° ile −6° arası), sabah ve akşam (başlangıç, bitiş).
    public struct LightHours: Sendable, Equatable {
        public var blueAm: (Double?, Double?)
        public var goldenAm: (Double?, Double?)
        public var goldenPm: (Double?, Double?)
        public var bluePm: (Double?, Double?)
        public static func == (l: Self, r: Self) -> Bool {
            l.blueAm == r.blueAm && l.goldenAm == r.goldenAm && l.goldenPm == r.goldenPm && l.bluePm == r.bluePm
        }
    }

    public static func lightHours(year: Int, month: Int, day: Int, latitude: Double, longitude: Double,
                                  utcOffset: Double) -> LightHours {
        func t(_ a: Double) -> AltitudeTimes {
            sunAltitudeTimes(year: year, month: month, day: day, latitude: latitude, longitude: longitude, utcOffset: utcOffset, altitude: a)
        }
        let g = t(6), e = t(-4), b = t(-6)
        return LightHours(blueAm: (b.rise, e.rise), goldenAm: (e.rise, g.rise), goldenPm: (g.set, e.set), bluePm: (e.set, b.set))
    }

    /// Kerahat vakitleri (yaklaşık; Diyanet'in yayımladığı dakika ölçüsü yok, takvimlerde yaygın 45 dk): işrak güneş
    /// vaktinden 45 dk sonrasına, istiva öğleden 45 dk önceden öğleye, isfirar akşamdan 45 dk önceden akşama.
    public static let kerahatMinutes = 45
    public struct Kerahat: Sendable, Equatable {
        public var israk: ClosedRange<Double>?
        public var istiva: ClosedRange<Double>?
        public var isfirar: ClosedRange<Double>?
    }

    public static func kerahat(_ day: PrayerDay) -> Kerahat {
        let k = Double(kerahatMinutes) / 60
        return Kerahat(israk: day.sunrise.map { $0...($0 + k) }, istiva: day.dhuhr.map { ($0 - k)...$0 },
                       isfirar: day.maghrib.map { ($0 - k)...$0 })
    }

    // MARK: Güneş konumu (PrayTimes tarzı düşük duyarlıklı formüller)

    static func julian(_ year: Int, _ month: Int, _ day: Int) -> Double {
        var y = Double(year), m = Double(month)
        if m <= 2 { y -= 1; m += 12 }
        let A = floor(y / 100), B = 2 - A + floor(A / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + Double(day) + B - 1524.5
    }

    static func sunPosition(_ jd: Double) -> (decl: Double, eqt: Double) {
        let D = jd - 2451545.0
        let g = fixAngle(357.529 + 0.98560028 * D)
        let q = fixAngle(280.459 + 0.98564736 * D)
        let L = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        let e = 23.439 - 0.00000036 * D
        let RA = fixHour(datan2(dcos(e) * dsin(L), dcos(L)) / 15)
        return (dasin(dsin(e) * dsin(L)), q / 15 - RA)
    }
}
