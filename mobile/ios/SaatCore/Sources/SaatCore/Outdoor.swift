import Foundation

/// Dışarıdaki işler için saatlik uygunluk (2. analiz #113) ve hava kalitesi / polen düzeyleri (#111).
/// Android core/Outdoor.kt ile birebir aynı kurallar. Puan: 2 iyi, 1 orta, 0 uygun değil; bileşenlerin en kötüsü alınır.
public enum Outdoor {
    public enum Activity: String, CaseIterable, Sendable {
        case run, bike, walk, garden
    }

    /// Bir saat: sıcaklık ve hissedilen °C, yağış ihtimali %, yağış mm, rüzgâr ve hamle km/sa, WMO kodu, gündüz mü, UV
    public struct Hour: Sendable {
        public let temp: Double
        public let feels: Double
        public let rain: Int
        public let mm: Double
        public let wind: Double
        public let gust: Double
        public let code: Int
        public let isDay: Bool
        public let uv: Double

        public init(temp: Double, feels: Double, rain: Int, mm: Double, wind: Double, gust: Double, code: Int, isDay: Bool, uv: Double = 0) {
            self.temp = temp
            self.feels = feels
            self.rain = rain
            self.mm = mm
            self.wind = wind
            self.gust = gust
            self.code = code
            self.isDay = isDay
            self.uv = uv
        }
    }

    /// Hissedilen sıcaklık aralıkları: iyi [a, b], orta [c, a) ve (b, d]
    static func tempBands(_ a: Activity) -> [Double] {
        switch a {
        case .run: return [5, 20, -5, 28]
        case .bike: return [10, 25, 3, 32]
        case .walk: return [10, 26, 0, 32]
        case .garden: return [10, 27, 5, 32]
        }
    }

    /// Rüzgâr sınırları: orta (ortalama), uygun değil (ortalama), uygun değil (hamle)
    static func windBands(_ a: Activity) -> [Double] {
        switch a {
        case .run: return [25, 40, 60]
        case .bike: return [20, 30, 45]
        case .walk: return [30, 45, 65]
        case .garden: return [25, 40, 60]
        }
    }

    public static func score(_ a: Activity, _ h: Hour) -> Int {
        // Gök gürültülü fırtına ve donan yağmur/çisenti her iş için uygun değil
        if (95...99).contains(h.code) || (56...57).contains(h.code) || (66...67).contains(h.code) { return 0 }
        var s = 2
        if h.rain >= 60 || h.mm >= 1.0 { return 0 }
        if h.rain >= 30 || h.mm >= 0.2 { s = min(s, 1) }
        let t = tempBands(a)
        let f = h.feels
        let ts: Int
        if f >= t[0] && f <= t[1] { ts = 2 } else if f >= t[2] && f <= t[3] { ts = 1 } else { ts = 0 }
        s = min(s, ts)
        let w = windBands(a)
        if h.wind > w[1] || (h.gust >= 0 && h.gust > w[2]) { return 0 }
        if h.wind > w[0] { s = min(s, 1) }
        if !h.isDay { s = a == .garden ? 0 : min(s, 1) }
        if h.uv >= 8 && a != .bike { s = min(s, 1) }
        return s
    }

    /// En iyi saatin sırası (en yüksek puan, eşitlikte en erken); hepsi uygun değilse nil
    public static func best(_ a: Activity, _ hours: [Hour]) -> Int? {
        var bi = -1
        var bs = 0
        for (i, h) in hours.enumerated() {
            let s = score(a, h)
            if s > bs { bs = s; bi = i }
        }
        return bi >= 0 ? bi : nil
    }

    /// Avrupa hava kalitesi endeksi (EAQI) düzeyi: 0 iyi … 5 son derece kötü
    public static func aqiLevel(_ eaqi: Double) -> Int {
        if eaqi < 20 { return 0 }
        if eaqi < 40 { return 1 }
        if eaqi < 60 { return 2 }
        if eaqi < 80 { return 3 }
        if eaqi < 100 { return 4 }
        return 5
    }

    /// Polen (tane/m³) düzeyi: 0 yok (< 1), 1 düşük (< 20), 2 orta (< 100), 3 yüksek
    public static func pollenLevel(_ grains: Double) -> Int {
        if grains < 1 { return 0 }
        if grains < 20 { return 1 }
        if grains < 100 { return 2 }
        return 3
    }
}
