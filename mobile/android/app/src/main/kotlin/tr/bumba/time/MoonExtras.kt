package tr.bumba.time

import tr.bumba.time.core.Nature
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Ay verileri (2. analiz #147; 3 Kasım 2026): uzaklık, sıradaki yerberi/yeröte ve süper Ay (sitedeki gece gökyüzü
 * sayfasıyla aynı kural: saatlik tarama + parabol; dolunay yerberiye 1,5 günden yakın ve 362.000 km'den yakınsa),
 * dördünler (Ay–Güneş açısının 90° olduğu an, ikiye bölme), üst geçiş (yerde en yüksek an). iOS'taki MoonExtras.swift ile aynı.
 */
object MoonDetails {
    private const val HOUR = 3_600_000L

    fun distanceKm(ms: Long): Double = 6378.14 / sin(Math.toRadians(Nature.moonPosition(ms.toDouble()).parallax))

    data class Extreme(val at: Long, val km: Double)

    /** Sıradaki yerberi ve yeröte (32 gün içinde) */
    fun perigeeApogee(now: Long): Pair<Extreme?, Extreme?> {
        var per: Extreme? = null
        var apo: Extreme? = null
        var prev = distanceKm(now - HOUR)
        var cur = distanceKm(now)
        var i = 1
        while (i <= 32 * 24 && (per == null || apo == null)) {
            val t = now + i * HOUR
            val nx = distanceKm(t)
            fun refine(): Extreme {
                val den = prev - 2 * cur + nx
                val off = if (den != 0.0) 0.5 * (prev - nx) / den else 0.0
                val tt = (t - HOUR + off * HOUR).toLong()
                return Extreme(tt, distanceKm(tt))
            }
            if (per == null && cur < prev && cur <= nx) per = refine()
            if (apo == null && cur > prev && cur >= nx) apo = refine()
            prev = cur; cur = nx; i++
        }
        return per to apo
    }

    /** Sıradaki dolunay süper Ay ise anı; değilse null */
    fun superMoon(now: Long, perigee: Extreme?): Long? {
        val full = Nature.moon(now).nextFullMoon
        return if (perigee != null && abs(full - perigee.at) < 1.5 * 86_400_000 && distanceKm(full) < 362_000) full else null
    }

    /** Ay ile Güneş arasındaki açı (derece, 0 yeni ay … 180 dolunay) */
    fun elongation(ms: Long): Double {
        val m = Nature.moonPosition(ms.toDouble())
        val s = Nature.sunPosition(ms.toDouble())
        val r = Math.PI / 180
        val c = sin(m.declination * r) * sin(s.declination * r) + cos(m.declination * r) * cos(s.declination * r) * cos((m.rightAscension - s.rightAscension) * r)
        return acos(c.coerceIn(-1.0, 1.0)) / r
    }

    private fun bisect(a0: Long, b0: Long, rising: Boolean): Long {
        var a = a0; var b = b0
        repeat(40) {
            val mid = (a + b) / 2
            val above = elongation(mid) > 90
            if (above == rising) b = mid else a = mid
        }
        return (a + b) / 2
    }

    /** Sıradaki ilk dördün ve son dördün anları */
    fun quarters(now: Long): Pair<Long, Long> {
        val k0 = floor((now / 86_400_000.0 + 2440587.5 - 2451550.09766) / 29.530588861).toInt() - 1
        var first = 0L
        var last = 0L
        for (k in k0..k0 + 3) {
            val new = Nature.phaseMillis(k, false)
            val full = Nature.phaseMillis(k, true)
            val next = Nature.phaseMillis(k + 1, false)
            val fq = bisect(new, full, true)
            val lq = bisect(full, next, false)
            if (first == 0L && fq > now) first = fq
            if (last == 0L && lq > now) last = lq
        }
        return first to last
    }

    /** O gün Ay'ın en yüksek olduğu an (10 dk tarama + üçlü arama); ufkun altındaysa da döner, yükseklikle */
    fun transit(place: AppPlace, day: LocalDate): Pair<Long, Double> {
        val start = day.atStartOfDay(place.zone).toInstant().toEpochMilli()
        var best = start
        var bestAlt = -90.0
        var t = start
        while (t < start + 86_400_000) {
            val a = Nature.moonAltitude(t.toDouble(), place.lat, place.lon)
            if (a > bestAlt) { bestAlt = a; best = t }
            t += 600_000
        }
        var lo = best - 600_000
        var hi = best + 600_000
        repeat(30) {
            val m1 = lo + (hi - lo) / 3
            val m2 = hi - (hi - lo) / 3
            if (Nature.moonAltitude(m1.toDouble(), place.lat, place.lon) < Nature.moonAltitude(m2.toDouble(), place.lat, place.lon)) lo = m1 else hi = m2
        }
        val at = (lo + hi) / 2
        return at to Nature.moonAltitude(at.toDouble(), place.lat, place.lon)
    }
}
