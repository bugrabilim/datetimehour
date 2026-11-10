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
    /// Dakikaya yuvarlama (Kemenag: başlangıçlar yukarı, güneş aşağı; diğerleri en yakın dakika)
    public var rounding: PrayerTimes.Rounding = .nearest

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

    /// Gece yarısından itibaren dakika; sitedeki `toMin` gibi yuvarlanır (çoğu yöntemde Math.round(saat × 60)).
    public func minutes(_ prayer: Prayer) -> Int? {
        self[prayer].map { PrayerTimes.toMinutes($0, rounding, sunrise: prayer == .sunrise) }
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
        public static let zero: Adjustments = {
            var a = Adjustments()
            a.sunrise = 0; a.dhuhr = 0; a.asr = 0; a.maghrib = 0; a.isha = 0
            return a
        }()
    }

    /// Dakikaya yuvarlama: en yakın dakika ya da (Kemenag) başlangıçlar yukarı, güneş aşağı.
    public enum Rounding: Sendable, Equatable { case nearest, ceil }

    /// Ondalık saati gece yarısından dakikaya çevirir (sitedeki `toMin`; yukarı/aşağı yuvarlamada 1e-6 dk pay).
    public static func toMinutes(_ hours: Double, _ rounding: Rounding, sunrise: Bool = false) -> Int {
        let x = hours * 60
        switch rounding {
        case .nearest: return Int(jsRound(x))
        case .ceil: return Int(sunrise ? Foundation.floor(x + 1e-6) : Foundation.ceil(x - 1e-6))
        }
    }

    public struct Parameters: Sendable, Equatable {
        public var fajrAngle = 18.0
        public var ishaAngle = 17.0
        public var sunriseAngle = 0.833
        public var sunsetAngle = 0.833
        public var asrFactor = 1.0
        /// Yatsı akşamdan bu kadar dakika sonra (0 = açıyla; Ümmü'l-Kura, Körfez, Katar)
        public var ishaMinutes = 0.0
        public var adjustments = Adjustments()
        public var rounding = Rounding.nearest
        /// Ek satırlar (yalnız Kemenag): imsak uyarısı sabahtan bu kadar dakika önce (0 = yok), duha Güneş bu
        /// yükseklikteyken + bu kadar dakika.
        public var imsakMinutes = 0.0
        public var duhaAltitude = 0.0
        public var duhaMinutes = 0.0
        public init() {}
        /// Diyanet ile uyumlu varsayılanlar.
        public static let diyanet = Parameters()

        /// Hesap yöntemi ve ikindi seçimi (sitedeki `methodParams` ile aynı); bilinmeyen yöntem Diyanet sayılır.
        public static func method(_ id: String, hanafi: Bool) -> Parameters {
            guard let m = PrayerTimes.methods[id], id != "diyanet" else {
                var p = Parameters()
                if hanafi { p.asrFactor = 2 }
                return p
            }
            var p = Parameters()
            p.fajrAngle = m.fajr
            p.ishaAngle = m.isha
            p.ishaMinutes = m.ishaMinutes
            p.sunriseAngle = m.sunrise
            p.sunsetAngle = m.maghrib
            p.asrFactor = hanafi ? 2 : 1
            p.adjustments = m.adjustments ?? .zero
            p.rounding = m.rounding
            p.imsakMinutes = m.imsakMinutes
            p.duhaAltitude = m.duhaAltitude
            p.duhaMinutes = m.duhaMinutes
            return p
        }
    }

    /// Hesap yöntemleri (sitedeki METHODS ile aynı sıra ve değerler). Diyanet varsayılandır; Türkiye'de resmî vakit odur.
    public struct Method: Sendable, Equatable {
        public let fajr: Double, isha: Double, ishaMinutes: Double, maghrib: Double
        public var sunrise = 0.833
        public var adjustments: Adjustments? = nil
        public var rounding = Rounding.nearest
        public var imsakMinutes = 0.0, duhaAltitude = 0.0, duhaMinutes = 0.0
        public init(fajr: Double, isha: Double, ishaMinutes: Double, maghrib: Double) {
            self.fajr = fajr; self.isha = isha; self.ishaMinutes = ishaMinutes; self.maghrib = maghrib
        }
    }
    /// Kemenag (Endonezya; sitedeki METHODS.kemenag): 20°/18°, ufuk 1° altı, ihtiyat dakikaları, yukarı yuvarlama,
    /// imsak uyarısı (−10 dk) ve duha (+4,5°, +2 dk).
    static let kemenag: Method = {
        var m = Method(fajr: 20, isha: 18, ishaMinutes: 0, maghrib: 1)
        m.sunrise = 1
        var a = Adjustments()
        a.fajr = 2; a.sunrise = -2; a.dhuhr = 3; a.asr = 2; a.maghrib = 2; a.isha = 2
        m.adjustments = a
        m.rounding = .ceil
        m.imsakMinutes = 10; m.duhaAltitude = 4.5; m.duhaMinutes = 2
        return m
    }()
    public static let methodIDs = ["diyanet", "mwl", "isna", "egypt", "makkah", "karachi", "tehran", "jafari",
                                   "gulf", "kuwait", "qatar", "singapore", "kemenag", "france", "russia"]
    public static let methods: [String: Method] = [
        "diyanet": Method(fajr: 18, isha: 17, ishaMinutes: 0, maghrib: 0.833),
        "mwl": Method(fajr: 18, isha: 17, ishaMinutes: 0, maghrib: 0.833),
        "isna": Method(fajr: 15, isha: 15, ishaMinutes: 0, maghrib: 0.833),
        "egypt": Method(fajr: 19.5, isha: 17.5, ishaMinutes: 0, maghrib: 0.833),
        "makkah": Method(fajr: 18.5, isha: 0, ishaMinutes: 90, maghrib: 0.833),
        "karachi": Method(fajr: 18, isha: 18, ishaMinutes: 0, maghrib: 0.833),
        "tehran": Method(fajr: 17.7, isha: 14, ishaMinutes: 0, maghrib: 4.5),
        "jafari": Method(fajr: 16, isha: 14, ishaMinutes: 0, maghrib: 4),
        "gulf": Method(fajr: 19.5, isha: 0, ishaMinutes: 90, maghrib: 0.833),
        "kuwait": Method(fajr: 18, isha: 17.5, ishaMinutes: 0, maghrib: 0.833),
        "qatar": Method(fajr: 18, isha: 0, ishaMinutes: 90, maghrib: 0.833),
        "singapore": Method(fajr: 20, isha: 18, ishaMinutes: 0, maghrib: 0.833),
        "kemenag": kemenag,
        "france": Method(fajr: 12, isha: 12, ishaMinutes: 0, maghrib: 0.833),
        "russia": Method(fajr: 16, isha: 15, ishaMinutes: 0, maghrib: 0.833),
    ]

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
        // Yatsı akşamdan sabit dakika sonra (Ümmü'l-Kura, Körfez, Katar)
        if P.ishaMinutes != 0 { isha = maghrib + P.ishaMinutes / 60 }
        let a = P.adjustments
        fajr += a.fajr / 60; sunrise += a.sunrise / 60; dhuhr += a.dhuhr / 60
        asr += a.asr / 60; maghrib += a.maghrib / 60; isha += a.isha / 60

        func value(_ x: Double) -> Double? { x.isNaN ? nil : x }
        return PrayerDay(fajr: value(fajr), sunrise: value(sunrise), dhuhr: value(dhuhr),
                         asr: value(asr), maghrib: value(maghrib), isha: value(isha), rounding: P.rounding)
    }

    /// Yöntemin ek satırları (yalnız Kemenag; sitedeki `methodRows`): dakika olarak imsak uyarısı ve duha.
    public struct MethodRows: Sendable, Equatable {
        public var imsak: Int?
        public var duha: Int?
    }

    public static func methodRows(year: Int, month: Int, day: Int, latitude: Double, longitude: Double,
                                  utcOffset: Double, parameters P: Parameters, times: PrayerDay) -> MethodRows? {
        guard P.imsakMinutes != 0 else { return nil }
        let du = sunAltitudeTimes(year: year, month: month, day: day, latitude: latitude, longitude: longitude,
                                  utcOffset: utcOffset, altitude: P.duhaAltitude).rise
        return MethodRows(imsak: times.minutes(.fajr).map { $0 - Int(P.imsakMinutes) },
                          duha: du.map { toMinutes($0 + P.duhaMinutes / 60, P.rounding) })
    }

    /// Bölgeye göre varsayılan yöntem (sitedeki `defaultMethod`): kullanıcı seçmediyse yerin saat dilimine (Kerala için
    /// koordinata) bakılır. Endonezya Kemenag; Malezya, Singapur, Brunei Singapur; Pakistan, Hindistan, Bangladeş,
    /// Afganistan Karaçi + Hanefî (Kerala ve Lakşadvip'te standart ikindi); diğer her yer Diyanet.
    static let regionTimeZones: [String: String] = [
        "Asia/Jakarta": "ID", "Asia/Pontianak": "ID", "Asia/Makassar": "ID", "Asia/Jayapura": "ID", "Asia/Ujung_Pandang": "ID",
        "Asia/Kuala_Lumpur": "MY", "Asia/Kuching": "MY", "Asia/Singapore": "SG", "Singapore": "SG", "Asia/Brunei": "BN",
        "Asia/Karachi": "PK", "Asia/Kolkata": "IN", "Asia/Calcutta": "IN", "Asia/Dhaka": "BD", "Asia/Dacca": "BD", "Asia/Kabul": "AF",
        "Europe/Istanbul": "TR", "Asia/Istanbul": "TR", "Turkey": "TR",
    ]
    public static func region(timeZone id: String) -> String { regionTimeZones[id] ?? "" }
    public static func defaultMethod(timeZone id: String, latitude lat: Double, longitude lon: Double) -> (id: String, hanafi: Bool) {
        switch region(timeZone: id) {
        case "ID": return ("kemenag", false)
        case "MY", "SG", "BN": return ("singapore", false)
        case "PK", "IN", "BD", "AF":
            let shafii = region(timeZone: id) == "IN" && lat >= 8 && lat <= 12.8 && lon >= 71.5 && lon <= 77.4
            return ("karachi", !shafii)
        default: return ("diyanet", false)
        }
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

    /// Geleneksel Hint zaman dilimleri (10 Kasım 2026, Hindistan araştırması): yerel ondalık saat (başlangıç, bitiş).
    public struct HinduKaal: Sendable {
        public var sunrise: Double
        public var sunset: Double
        public var rahu: (Double, Double)
        public var yamaganda: (Double, Double)
        public var gulika: (Double, Double)
        public var abhijit: (Double, Double)
        public var brahma: (Double, Double)
    }
    /// Gündüzün 8 parçasından hangisi (pazar 0 … cumartesi 6)
    public static let kaalRahu = [8, 2, 7, 5, 6, 4, 3]
    public static let kaalYamaganda = [5, 4, 3, 2, 1, 7, 6]
    public static let kaalGulika = [7, 6, 5, 4, 3, 2, 1]

    /// Gün doğumu ve batımı Güneş −0,833° (düzeltmesiz); gündüz 8 parça (Rahu Kaal, Yamaganda, Gulika), Abhijit gündüzün
    /// 15 muhurtasının 8.si, Brahma muhurta önceki gecenin 15 muhurtasının 14.sü. Sitedeki `hinduKaal` ile birebir
    /// (test verisi `kaal.json`); Güneş doğmuyor ya da batmıyorsa nil.
    public static func hinduKaal(year: Int, month: Int, day: Int, latitude lat: Double, longitude lon: Double,
                                 utcOffset tz: Double) -> HinduKaal? {
        let t = sunAltitudeTimes(year: year, month: month, day: day, latitude: lat, longitude: lon, utcOffset: tz, altitude: -0.833)
        let p = CalendarDay(year, month, day).adding(days: -1)
        let prev = sunAltitudeTimes(year: p.year, month: p.month, day: p.day, latitude: lat, longitude: lon, utcOffset: tz, altitude: -0.833)
        guard let rise = t.rise, let set = t.set, let pset = prev.set else { return nil }
        let dow = Int((julian(year, month, day) + 1.5).rounded(.down)) % 7
        let part = (set - rise) / 8, mu = (set - rise) / 15, nmu = (rise + 24 - pset) / 15
        func seg(_ n: Int) -> (Double, Double) { (rise + Double(n - 1) * part, rise + Double(n) * part) }
        return HinduKaal(sunrise: rise, sunset: set, rahu: seg(kaalRahu[dow]), yamaganda: seg(kaalYamaganda[dow]),
                         gulika: seg(kaalGulika[dow]), abhijit: (rise + 7 * mu, rise + 8 * mu), brahma: (rise - 2 * nmu, rise - nmu))
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

    /// Bayram namazı vakti (Diyanet: güneş vaktinden 38 dk sonra; 2. analiz #56). Sitedeki `bayram` ile aynı.
    public static let bayramMinutes = 38
    public static func bayram(_ day: PrayerDay) -> Double? { day.sunrise.map { $0 + Double(bayramMinutes) / 60 } }

    /// Ek vakitler (Türkiye Takvimi tanımlarıyla yaklaşık; 2. analiz #59): işrak ve isfirar Güneş +5°, iştibak −10°,
    /// işa-i sani −19°, dahve-i kübra imsak ile akşamın ortası; gece akşamdan imsaka: gece yarısı ½, teheccüd ⅔, seher ⅚.
    /// Sitedeki `extraTimes` ile birebir; `day` Diyanet parametreleriyle hesaplanmış gün olmalı.
    public struct ExtraTimes: Sendable, Equatable {
        public var israk: Double?, duha: Double?, isfirar: Double?, istibak: Double?, isaSani: Double?
        public var midnight: Double?, tahajjud: Double?, seher: Double?
    }

    public static func extraTimes(year: Int, month: Int, day: Int, latitude: Double, longitude: Double,
                                  utcOffset: Double, times: PrayerDay) -> ExtraTimes {
        func t(_ a: Double) -> AltitudeTimes {
            sunAltitudeTimes(year: year, month: month, day: day, latitude: latitude, longitude: longitude, utcOffset: utcOffset, altitude: a)
        }
        let up = t(5), st = t(-10), is2 = t(-19)
        var duha: Double?, mid: Double?, tah: Double?, seh: Double?
        if let f = times.fajr, let m = times.maghrib {
            let night = f + 24 - m
            duha = (f + m) / 2; mid = m + night / 2; tah = m + night * 2 / 3; seh = m + night * 5 / 6
        }
        return ExtraTimes(israk: up.rise, duha: duha, isfirar: up.set, istibak: st.set, isaSani: is2.set,
                          midnight: mid, tahajjud: tah, seher: seh)
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
