package tr.bumba.time.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.floor

/** Namaz vakitleri ve güneş vakitleri. */
enum class Prayer(val key: String) {
    FAJR("fajr"), SUNRISE("sunrise"), DHUHR("dhuhr"), ASR("asr"), MAGHRIB("maghrib"), ISHA("isha"),
}

/**
 * Bir günün vakitleri: yerel saat, ondalık saat (5.5 = 05:30).
 * Kutup bölgelerinde vakit oluşmuyorsa `null` (ör. gece yarısı güneşinde güneş doğuşu).
 */
data class PrayerDay(
    val fajr: Double?,
    val sunrise: Double?,
    val dhuhr: Double?,
    val asr: Double?,
    val maghrib: Double?,
    val isha: Double?,
) {
    operator fun get(prayer: Prayer): Double? = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    /** Gece yarısından itibaren dakika; sitedeki gibi yuvarlanır (Math.round(saat × 60)). */
    fun minutes(prayer: Prayer): Int? = get(prayer)?.let { jsRound(it * 60).toInt() }
}

/**
 * Namaz vakti hesabı (astronomik, dış veri yok). Sitedeki `src/js/prayer-calc.js` dosyasının birebir karşılığıdır;
 * sonuçlar `mobile/shared/testdata/prayer-times.json` ile doğrulanır. Formül değişirse önce JS değişir, test verisi yeniden üretilir.
 * Yöntem: imsak 18°, yatsı 17° (Diyanet ile uyumlu), ikindi gölge katsayısı 1, resmî vakitlere uyum için dakika düzeltmeleri.
 * Yüksek enlemde imsak/yatsı açısı oluşmazsa gecenin yedide biri kuralı uygulanır.
 */
object PrayerTimes {
    /** Dakika cinsinden düzeltmeler (resmî vakitlerle karşılaştırılarak ayarlandı). */
    data class Adjustments(
        val fajr: Double = 0.0,
        val sunrise: Double = -7.1,
        val dhuhr: Double = 5.1,
        val asr: Double = 4.5,
        val maghrib: Double = 7.9,
        val isha: Double = 0.9,
    )

    data class Parameters(
        val fajrAngle: Double = 18.0,
        val ishaAngle: Double = 17.0,
        val sunriseAngle: Double = 0.833,
        val sunsetAngle: Double = 0.833,
        val asrFactor: Double = 1.0,
        val adjustments: Adjustments = Adjustments(),
    ) {
        companion object {
            /** Diyanet ile uyumlu varsayılanlar. */
            val DIYANET = Parameters()
        }
    }

    internal data class SunPosition(val decl: Double, val eqt: Double)

    /**
     * Bir gün için vakitler.
     * @param utcOffset o gün UTC'ye göre saat farkı (Türkiye için 3).
     */
    fun times(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        utcOffset: Double,
        parameters: Parameters = Parameters.DIYANET,
    ): PrayerDay {
        val lat = latitude
        val lon = longitude
        val p = parameters
        val jd = julian(year, month, day) - lon / 360
        fun midDay(t: Double): Double = fixHour(12 - sunPosition(jd + t).eqt)

        // alt: ufkun altındaki açı (derece, pozitif = altında)
        fun angleTime(alt: Double, t: Double, ccw: Boolean): Double {
            val decl = sunPosition(jd + t).decl
            val a = (-dsin(alt) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            if (a < -1 || a > 1) return Double.NaN
            return midDay(t) + (if (ccw) -1 else 1) * dacos(a) / 15
        }

        fun asrTime(t: Double): Double {
            val decl = sunPosition(jd + t).decl
            val altAbove = dacot(p.asrFactor + dtan(abs(lat - decl)))
            return angleTime(-altAbove, t, ccw = false)
        }

        var fajr = angleTime(p.fajrAngle, 5.0 / 24, ccw = true)
        var sunrise = angleTime(p.sunriseAngle, 6.0 / 24, ccw = true)
        var dhuhr = midDay(12.0 / 24)
        var asr = asrTime(13.0 / 24)
        var maghrib = angleTime(p.sunsetAngle, 18.0 / 24, ccw = false)
        var isha = angleTime(p.ishaAngle, 18.0 / 24, ccw = false)

        val off = utcOffset - lon / 15
        fajr += off; sunrise += off; dhuhr += off; asr += off; maghrib += off; isha += off

        // Yüksek enlemlerde (beyaz geceler) imsak/yatsı açısı hiç oluşmuyorsa: gecenin yedide biri kuralı
        if (!sunrise.isNaN() && !maghrib.isNaN()) {
            val night = 24 - (maghrib - sunrise)
            if (fajr.isNaN()) fajr = sunrise - night / 7
            if (isha.isNaN()) isha = maghrib + night / 7
        }
        val a = p.adjustments
        fajr += a.fajr / 60; sunrise += a.sunrise / 60; dhuhr += a.dhuhr / 60
        asr += a.asr / 60; maghrib += a.maghrib / 60; isha += a.isha / 60

        fun value(x: Double): Double? = if (x.isNaN()) null else x
        return PrayerDay(value(fajr), value(sunrise), value(dhuhr), value(asr), value(maghrib), value(isha))
    }

    /** Bir gün için vakitler; saat dilimi farkı sitedeki gibi o günün UTC öğlesindeki farktır. */
    fun times(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        parameters: Parameters = Parameters.DIYANET,
    ): PrayerDay {
        val noon = date.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC)
        val offset = zone.rules.getOffset(noon).totalSeconds / 3600.0
        return times(date.year, date.monthValue, date.dayOfMonth, latitude, longitude, offset, parameters)
    }

    // Güneş konumu (PrayTimes tarzı düşük duyarlıklı formüller)

    internal fun julian(year: Int, month: Int, day: Int): Double {
        var y = year.toDouble()
        var m = month.toDouble()
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    internal fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(datan2(dcos(e) * dsin(l), dcos(l)) / 15)
        return SunPosition(dasin(dsin(e) * dsin(l)), q / 15 - ra)
    }
}
