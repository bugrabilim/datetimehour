import Foundation

/// Uyku planı (30 Ekim 2026, rakip analizi #17): 90 dakikalık uyku döngüleri (sitedeki alarm sayfalarıyla aynı kural:
/// uykuya dalmak için 15 dk + döngüler). Uyku takibi (mikrofon, hareket) bilerek yok. Android'deki Sleep'in aynısı.
public enum Sleep {
    public static let fallAsleep = 15
    public static let cycle = 90
    public static let cycles = [6, 5, 4]

    /// Günün dakikası (0–1439)
    public static func mod(_ m: Int) -> Int { ((m % 1440) + 1440) % 1440 }

    /// Kalkış saatine göre yatma saatleri (6, 5, 4 döngü)
    public static func bedtimes(wake: Int) -> [(cycles: Int, minute: Int)] {
        cycles.map { ($0, mod(wake - fallAsleep - $0 * cycle)) }
    }

    /// Şimdi yatılırsa kalkış saatleri (6, 5, 4 döngü)
    public static func wakeTimes(now: Int) -> [(cycles: Int, minute: Int)] {
        cycles.map { ($0, mod(now + fallAsleep + $0 * cycle)) }
    }

    /// Planın yatma saati: kalkıştan uyku süresi kadar önce
    public static func bedtime(wake: Int, duration: Int) -> Int { mod(wake - duration) }
}

/// Gün doğumu ışığı: alarmdan `window` dakika önce başlar, alarm anında tam aydınlık olur, 15 dk sonra söner.
/// Ekran yalnız tam ekran saat açıkken aydınlanır (telefon uygulama kapalıyken ekranı açamaz).
public enum SunriseLight {
    public static let after: TimeInterval = 15 * 60

    /// 0 (karanlık) – 1 (tam aydınlık); yumuşak başlangıçlı
    public static func level(now: Date, alarmAt: Date, window: Int) -> Double {
        guard window > 0 else { return 0 }
        let start = alarmAt.addingTimeInterval(-Double(window) * 60)
        if now < start || now > alarmAt.addingTimeInterval(after) { return 0 }
        if now >= alarmAt { return 1 }
        let x = now.timeIntervalSince(start) / alarmAt.timeIntervalSince(start)
        return x * x * (3 - 2 * x)
    }

    private static let stops: [(Double, [Double])] = [
        (0, [0, 0, 0]),
        (0.25, [70, 10, 6]),
        (0.55, [196, 72, 22]),
        (0.8, [255, 160, 72]),
        (1, [255, 236, 204]),
    ]

    /// Işığın rengi (koyu kırmızıdan turuncuya, sonra sıcak beyaza); 0–255 RGB
    public static func rgb(_ level: Double) -> (r: Int, g: Int, b: Int) {
        let l = max(0, min(1, level))
        let i = min(stops.lastIndex { $0.0 <= l } ?? 0, stops.count - 2)
        let (a, ca) = stops[i], (b, cb) = stops[i + 1]
        let k = (l - a) / (b - a)
        let c = (0..<3).map { Int((ca[$0] + (cb[$0] - ca[$0]) * k).rounded()) }
        return (c[0], c[1], c[2])
    }

    /// Ekran parlaklığı (0,05 – 1)
    public static func brightness(_ level: Double) -> Double { 0.05 + 0.95 * max(0, min(1, level)) }
}
