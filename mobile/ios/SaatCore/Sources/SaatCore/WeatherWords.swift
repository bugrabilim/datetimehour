import Foundation

/// Hava durumunun sözlü açıklaması (2. analiz #108, #110; 3 Kasım 2026): ölçüm kartlarındaki kısa açıklama ve günlük özet
/// cümlesi. Metin değil metin anahtarı döner (çeviriler uygulamada). Eşikler sitedeki weather-core.js ve Android'deki
/// core/WeatherWords.kt ile aynıdır.
public enum WeatherWords {
    /// Rüzgâr (km/sa): sakin, hafif esinti, rüzgârlı, kuvvetli, fırtına
    public static func wind(_ kmh: Double) -> String {
        "weather.wd." + (kmh < 6 ? "calm" : kmh < 20 ? "light" : kmh < 39 ? "breezy" : kmh < 62 ? "strong" : "gale")
    }

    /// UV indeksi (WHO ölçeği)
    public static func uv(_ uv: Double) -> String {
        "weather.uvw." + (uv < 3 ? "low" : uv < 6 ? "moderate" : uv < 8 ? "high" : uv < 11 ? "veryHigh" : "extreme")
    }

    /// Bağıl nem (%)
    public static func humidity(_ h: Double) -> String {
        "weather.hw." + (h < 30 ? "dry" : h < 60 ? "comfy" : h < 80 ? "humid" : "veryHumid")
    }

    /// Görüş uzaklığı (m)
    public static func visibility(_ m: Double) -> String {
        "weather.vw." + (m < 1000 ? "fog" : m < 4000 ? "haze" : m < 10000 ? "good" : "clear")
    }

    /// Çiy noktası (°C)
    public static func dew(_ c: Double) -> String {
        "weather.dw." + (c < 10 ? "dry" : c < 16 ? "comfy" : c < 20 ? "muggy" : "oppressive")
    }

    /// Hissedilen sıcaklık: 3° ve fazla farkta açıklama; yoksa nil
    public static func feels(_ temp: Double, _ feels: Double) -> String? {
        feels <= temp - 3 ? "weather.fw.colder" : feels >= temp + 3 ? "weather.fw.warmer" : nil
    }

    /// Önümüzdeki saatlerden biri: yerel saat (0–23), WMO kodu, yağış ihtimali (%), rüzgâr (km/sa)
    public struct Hour: Equatable, Sendable {
        public let hour: Int, code: Int, rain: Int
        public let wind: Double
        public init(hour: Int, code: Int, rain: Int, wind: Double) { self.hour = hour; self.code = code; self.rain = rain; self.wind = wind }
    }

    /// Özet: anahtar; `hour` saatli anahtarlarda yerel saat, `n` derece farkında
    public struct Summary: Equatable, Sendable {
        public let key: String
        public var hour = -1
        public var n = 0
        public init(_ key: String, hour: Int = -1, n: Int = 0) { self.key = key; self.hour = hour; self.n = n }
    }

    private static func isThunder(_ c: Int) -> Bool { (95...99).contains(c) }
    private static func isSnow(_ c: Int) -> Bool { (71...77).contains(c) || (85...86).contains(c) }

    /// Günlük özet cümlesi (Android'deki WeatherWords.summary ile aynı öncelik)
    public static func summary(_ next: [Hour], todayMax: Double, tomorrowMax: Double?) -> Summary {
        let h = Array(next.prefix(12))
        if let t = h.first(where: { isThunder($0.code) }) { return Summary("weather.sum.storm", hour: t.hour) }
        if let r = h.first(where: { $0.rain >= 60 }) { return Summary(isSnow(r.code) ? "weather.sum.snow" : "weather.sum.rain", hour: r.hour) }
        if h.contains(where: { (12...18).contains($0.hour) && $0.wind >= 30 }) { return Summary("weather.sum.windyAfternoon") }
        if let tm = tomorrowMax {
            let d = tm - todayMax
            if d <= -5 { return Summary("weather.sum.colder", n: Int((-d).rounded())) }
            if d >= 5 { return Summary("weather.sum.warmer", n: Int(d.rounded())) }
        }
        return Summary("weather.sum.default")
    }

    /// Akşam bildirimi (#110): yarın en yüksek sıcaklık bugünden 5° ve fazla farklıysa; yoksa nil
    public static func tomorrowAlert(todayMax: Double, tomorrowMax: Double) -> Summary? {
        let d = tomorrowMax - todayMax
        if d <= -5 { return Summary("weather.alert.colder", n: Int((-d).rounded())) }
        if d >= 5 { return Summary("weather.alert.warmer", n: Int(d.rounded())) }
        return nil
    }

    /// Rüzgâr yönü (geldiği yön, derece) → 8 yönden sırası (0 = kuzey, saat yönünde)
    public static func compass8(_ deg: Double) -> Int { ((Int((deg / 45).rounded()) % 8) + 8) % 8 }
}
