package tr.bumba.time.core

/**
 * Kıble yönü, Kâbe'ye uzaklık ve kıble saati (30 Ekim 2026, rakip analizi #8). Sitedeki `src/js/prayer-calc.js`
 * `qibla`/`qiblaTime` işlevlerinin birebir karşılığıdır; sonuçlar `mobile/shared/testdata/qibla.json` ile doğrulanır.
 * Kıble saati Diyanet'in yayımladığı "kıble saati"yle (Güneş'in tam kıble yönünde olduğu an) İstanbul'da 32 günde ±1 dk.
 */
object Qibla {
    /** Kâbe (Mescid-i Haram) */
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206

    data class Direction(val bearing: Double, val km: Double)
    data class Sun(val alt: Double, val az: Double)

    /** Büyük daire başlangıç yönü (kuzeyden saat yönünde, derece) ve uzaklık (km) */
    fun direction(lat: Double, lon: Double): Direction {
        val dl = KAABA_LON - lon
        val bearing = fixAngle(datan2(dsin(dl), dcos(lat) * dtan(KAABA_LAT) - dsin(lat) * dcos(dl)))
        val c = dsin(lat) * dsin(KAABA_LAT) + dcos(lat) * dcos(KAABA_LAT) * dcos(dl)
        val km = StrictMath.acos(maxOf(-1.0, minOf(1.0, c))) * 6371.0088
        return Direction(bearing, km)
    }

    /** Güneş'in yüksekliği ve yönü (derece; yön kuzeyden saat yönünde). ut: o günün UTC saati (ondalık). */
    fun sun(year: Int, month: Int, day: Int, ut: Double, lat: Double, lon: Double): Sun {
        val p = PrayerTimes.sunPosition(PrayerTimes.julian(year, month, day) + ut / 24)
        val h = 15 * (ut + p.eqt + lon / 15 - 12)
        val alt = dasin(dsin(lat) * dsin(p.decl) + dcos(lat) * dcos(p.decl) * dcos(h))
        val az = fixAngle(datan2(dsin(h), dcos(h) * dsin(lat) - dtan(p.decl) * dcos(lat)) + 180)
        return Sun(alt, az)
    }

    /**
     * Kıble saati: Güneş'in kıble yönünden geçtiği an (yerel ondalık saat). Güneş o gün kıble yönüne gelmiyorsa
     * (ör. Kâbe'nin çok uzağında) `null`. Gün 5 dk adımla taranır, işaret değişen aralık ikiye bölünür.
     */
    fun time(year: Int, month: Int, day: Int, lat: Double, lon: Double, tz: Double): Double? {
        val q = direction(lat, lon).bearing
        fun diff(ut: Double): Pair<Double, Double> {
            val a = sun(year, month, day, ut, lat, lon)
            return a.alt to ((a.az - q + 540) % 360) - 180
        }
        val step = 5.0 / 60
        var prev = diff(-tz)
        var ut = -tz + step
        while (ut <= 24 - tz + 1e-9) {
            val cur = diff(ut)
            if (prev.first > 0 && cur.first > 0 && prev.second <= 0 && cur.second > 0 && cur.second - prev.second < 90) {
                var lo = ut - step
                var hi = ut
                repeat(30) {
                    val mid = (lo + hi) / 2
                    if (diff(mid).second <= 0) lo = mid else hi = mid
                }
                return (lo + hi) / 2 + tz
            }
            prev = cur
            ut += step
        }
        return null
    }
}
