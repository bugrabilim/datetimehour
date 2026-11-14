import Foundation

/// Fotoğraf ışığı (9 Kasım 2026, sahibin onayı): seçili yer ve gün için altın/mavi saat, gün doğumu/batımı ve Güneş'in
/// doğduğu/battığı yön, bulut tahmininden gün doğumu ve batımı fotoğrafı tahmini (iyi / orta / kötü).
/// Saatler mevcut Güneş yüksekliği işlevlerinden (Mevsimler'deki tanımlar: altın saat +6° ile −4°, mavi saat −4° ile −6°,
/// ufuk −0,833°); yeni gökbilim formülü yok. Tahmin kuralı sitedeki fotoğraf ışığı sayfası ve Android core/PhotoLight.kt
/// ile birebir aynı (testler iki tarafta aynı örneklerle).
public enum PhotoLight {
    public enum Quality: String, CaseIterable, Sendable {
        case good, fair, poor
    }

    /// Gün doğumu/batımı fotoğrafı tahmini. `low` alçak, `mid` orta, `high` yüksek bulut (%), `precip` yağış (mm/sa); gün
    /// doğumuna ya da batımına en yakın saatlik değer. Renkleri orta ve yüksek bulutlar yakalar; ufuktaki alçak bulut ve
    /// yağış Güneş'i kapatır; açık gökyüzü temiz ama sade.
    public static func quality(low: Double, mid: Double, high: Double, precip: Double) -> Quality {
        if precip >= 0.5 || low >= 75 { return .poor }
        let upper = max(mid, high)
        if low < 40 && upper >= 20 && upper <= 80 { return .good }
        if low < 60 { return .fair }
        return .poor
    }

    /// Saatlik bulut ve yağış örneği (t: UTC milisaniye)
    public struct Sample: Sendable, Equatable {
        public let t: Int64
        public let low: Double
        public let mid: Double
        public let high: Double
        public let precip: Double

        public init(t: Int64, low: Double, mid: Double, high: Double, precip: Double) {
            self.t = t
            self.low = low
            self.mid = mid
            self.high = high
            self.precip = precip
        }
    }

    /// `t` anına en yakın saatlik örnek; `maxGap` milisaniyeden uzaksa (tahmin aralığı dışında) nil
    public static func nearest(_ samples: [Sample], to t: Int64, maxGap: Int64 = 90 * 60_000) -> Sample? {
        guard let s = samples.min(by: { abs($0.t - t) < abs($1.t - t) }), abs(s.t - t) <= maxGap else { return nil }
        return s
    }

    /// `t` anı için tahmin (örnek yoksa nil)
    public static func quality(at t: Int64, in samples: [Sample]) -> Quality? {
        nearest(samples, to: t).map { quality(low: $0.low, mid: $0.mid, high: $0.high, precip: $0.precip) }
    }

    /// Gün doğumu ve batımı için ufuk yüksekliği (Mevsimler'deki gibi, vakit düzeltmesi yok)
    public static let horizon = -0.833

    /// Bir günün ışığı. Saatler yerel ondalık saat (24'ü geçebilir: ertesi gün; oluşmuyorsa nil), yönler derece (kuzeyden
    /// saat yönünde). `polar`: Güneş bütün gün ufkun üstünde 1 (gece yarısı güneşi), altında −1 (kutup gecesi), olağan 0.
    public struct Day: Sendable, Equatable {
        public let light: PrayerTimes.LightHours
        public let sunrise: Double?
        public let sunset: Double?
        public let riseAzimuth: Double?
        public let setAzimuth: Double?
        public let polar: Int
    }

    public static func day(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, utcOffset: Double) -> Day {
        let light = PrayerTimes.lightHours(year: year, month: month, day: day, latitude: latitude, longitude: longitude, utcOffset: utcOffset)
        let s = PrayerTimes.sunAltitudeTimes(year: year, month: month, day: day, latitude: latitude, longitude: longitude,
                                             utcOffset: utcOffset, altitude: horizon)
        func azimuth(_ h: Double?) -> Double? {
            h.map { Qibla.sun(year: year, month: month, day: day, ut: $0 - utcOffset, latitude: latitude, longitude: longitude).az }
        }
        let polar: Int
        if s.rise != nil || s.set != nil {
            polar = 0
        } else {
            // Yerel güneş öğlesinde (UTC 12 − boylam/15) Güneş ufkun üstündeyse bütün gün üstündedir
            let noon = Qibla.sun(year: year, month: month, day: day, ut: 12 - longitude / 15, latitude: latitude, longitude: longitude)
            polar = noon.alt > horizon ? 1 : -1
        }
        return Day(light: light, sunrise: s.rise, sunset: s.set, riseAzimuth: azimuth(s.rise), setAzimuth: azimuth(s.set), polar: polar)
    }
}
