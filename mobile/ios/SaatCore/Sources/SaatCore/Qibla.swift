import Foundation

/// Kıble yönü, Kâbe'ye uzaklık ve kıble saati (30 Ekim 2026, rakip analizi #8). Sitedeki `src/js/prayer-calc.js`
/// `qibla`/`qiblaTime` işlevlerinin birebir karşılığıdır; sonuçlar `mobile/shared/testdata/qibla.json` ile doğrulanır.
/// Kıble saati Diyanet'in yayımladığı "kıble saati"yle (Güneş'in tam kıble yönünde olduğu an) İstanbul'da 32 günde ±1 dk.
public enum Qibla {
    /// Kâbe (Mescid-i Haram)
    public static let kaabaLatitude = 21.422487
    public static let kaabaLongitude = 39.826206

    /// Büyük daire başlangıç yönü (kuzeyden saat yönünde, derece) ve uzaklık (km)
    public static func direction(latitude lat: Double, longitude lon: Double) -> (bearing: Double, km: Double) {
        let dl = kaabaLongitude - lon
        let bearing = fixAngle(datan2(dsin(dl), dcos(lat) * dtan(kaabaLatitude) - dsin(lat) * dcos(dl)))
        let c = dsin(lat) * dsin(kaabaLatitude) + dcos(lat) * dcos(kaabaLatitude) * dcos(dl)
        let km = acos(max(-1, min(1, c))) * 6371.0088
        return (bearing, km)
    }

    /// Güneş'in yüksekliği ve yönü (derece; yön kuzeyden saat yönünde). ut: o günün UTC saati (ondalık).
    public static func sun(year: Int, month: Int, day: Int, ut: Double, latitude lat: Double, longitude lon: Double) -> (alt: Double, az: Double) {
        let p = PrayerTimes.sunPosition(PrayerTimes.julian(year, month, day) + ut / 24)
        let H = 15 * (ut + p.eqt + lon / 15 - 12)
        let alt = dasin(dsin(lat) * dsin(p.decl) + dcos(lat) * dcos(p.decl) * dcos(H))
        let az = fixAngle(datan2(dsin(H), dcos(H) * dsin(lat) - dtan(p.decl) * dcos(lat)) + 180)
        return (alt, az)
    }

    /// Kıble saati: Güneş'in kıble yönünden geçtiği an (yerel ondalık saat). Güneş o gün kıble yönüne gelmiyorsa
    /// (ör. Kâbe'nin çok uzağında) `nil`. Gün 5 dk adımla taranır, işaret değişen aralık ikiye bölünür.
    public static func time(year: Int, month: Int, day: Int, latitude lat: Double, longitude lon: Double, utcOffset tz: Double) -> Double? {
        let q = direction(latitude: lat, longitude: lon).bearing
        func diff(_ ut: Double) -> (alt: Double, f: Double) {
            let a = sun(year: year, month: month, day: day, ut: ut, latitude: lat, longitude: lon)
            return (a.alt, (a.az - q + 540).truncatingRemainder(dividingBy: 360) - 180)
        }
        let step = 5.0 / 60
        var prev = diff(-tz)
        var ut = -tz + step
        while ut <= 24 - tz + 1e-9 {
            let cur = diff(ut)
            if prev.alt > 0 && cur.alt > 0 && prev.f <= 0 && cur.f > 0 && cur.f - prev.f < 90 {
                var lo = ut - step, hi = ut
                for _ in 0..<30 {
                    let mid = (lo + hi) / 2
                    if diff(mid).f <= 0 { lo = mid } else { hi = mid }
                }
                return (lo + hi) / 2 + tz
            }
            prev = cur
            ut += step
        }
        return nil
    }

    /// Rashdul Qiblat (10 Kasım 2026): Güneş'in Kâbe'nin tam üstünden geçtiği iki an, [Mayıs, Temmuz] UTC ms. Mayıs 15–31 ve
    /// Temmuz 5–31 arasında Kâbe'de öğle anında Güneş'i başucuna en yakın getiren gün; sitedeki `rashdulQiblat` ile aynı.
    public static func rashdul(year y: Int) -> [Double] {
        func noon(_ m: Int, _ d: Int) -> (jd: Double, t: Double, zd: Double) {
            let jd = PrayerTimes.julian(y, m, d)
            var t = 12 - kaabaLongitude / 15
            for _ in 0..<3 { t = 12 - PrayerTimes.sunPosition(jd + t / 24).eqt - kaabaLongitude / 15 }
            return (jd, t, abs(PrayerTimes.sunPosition(jd + t / 24).decl - kaabaLatitude))
        }
        return [(5, 15, 31), (7, 5, 31)].map { (r: (Int, Int, Int)) -> Double in
            var best: (jd: Double, t: Double, zd: Double)?
            for d in r.1...r.2 {
                let n = noon(r.0, d)
                if best == nil || n.zd < best!.zd { best = n }
            }
            return jsRound((best!.jd - 2440587.5) * 86400000 + best!.t * 3600000)
        }
    }
}
