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
    /** Dakikaya yuvarlama (Kemenag: başlangıçlar yukarı, güneş aşağı; diğerleri en yakın dakika) */
    val rounding: PrayerTimes.Rounding = PrayerTimes.Rounding.NEAREST,
) {
    operator fun get(prayer: Prayer): Double? = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    /** Gece yarısından itibaren dakika; sitedeki `toMin` gibi yuvarlanır (çoğu yöntemde Math.round(saat × 60)). */
    fun minutes(prayer: Prayer): Int? = get(prayer)?.let { PrayerTimes.toMinutes(it, rounding, sunrise = prayer == Prayer.SUNRISE) }
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
    ) {
        companion object {
            val ZERO = Adjustments(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
    }

    /** Dakikaya yuvarlama: en yakın dakika ya da (Kemenag) başlangıçlar yukarı, güneş aşağı. */
    enum class Rounding { NEAREST, CEIL }

    /** Ondalık saati gece yarısından dakikaya çevirir (sitedeki `toMin`; yukarı/aşağı yuvarlamada 1e-6 dk pay). */
    fun toMinutes(hours: Double, rounding: Rounding, sunrise: Boolean = false): Int {
        val x = hours * 60
        return when (rounding) {
            Rounding.NEAREST -> jsRound(x).toInt()
            Rounding.CEIL -> (if (sunrise) floor(x + 1e-6) else kotlin.math.ceil(x - 1e-6)).toInt()
        }
    }

    data class Parameters(
        val fajrAngle: Double = 18.0,
        val ishaAngle: Double = 17.0,
        val sunriseAngle: Double = 0.833,
        val sunsetAngle: Double = 0.833,
        val asrFactor: Double = 1.0,
        /** Yatsı akşamdan bu kadar dakika sonra (0 = açıyla; Ümmü'l-Kura, Körfez, Katar) */
        val ishaMinutes: Double = 0.0,
        val adjustments: Adjustments = Adjustments(),
        val rounding: Rounding = Rounding.NEAREST,
        /** Ek satırlar (yalnız Kemenag): imsak uyarısı sabahtan bu kadar dakika önce (0 = yok), duha Güneş bu yükseklikteyken + bu kadar dakika. */
        val imsakMinutes: Double = 0.0,
        val duhaAltitude: Double = 0.0,
        val duhaMinutes: Double = 0.0,
    ) {
        companion object {
            /** Diyanet ile uyumlu varsayılanlar. */
            val DIYANET = Parameters()

            /** Hesap yöntemi ve ikindi seçimi (sitedeki `methodParams` ile aynı); bilinmeyen yöntem Diyanet sayılır. */
            fun method(id: String, hanafi: Boolean): Parameters {
                val m = METHODS[id]
                if (m == null || id == "diyanet") return if (hanafi) Parameters(asrFactor = 2.0) else DIYANET
                return Parameters(
                    fajrAngle = m.fajr, ishaAngle = m.isha, sunriseAngle = m.sunrise, sunsetAngle = m.maghrib, ishaMinutes = m.ishaMinutes,
                    asrFactor = if (hanafi) 2.0 else 1.0, adjustments = m.adjustments ?: Adjustments.ZERO, rounding = m.rounding,
                    imsakMinutes = m.imsakMinutes, duhaAltitude = m.duhaAltitude, duhaMinutes = m.duhaMinutes,
                )
            }
        }
    }

    /** Hesap yöntemleri (sitedeki METHODS ile aynı sıra ve değerler). Diyanet varsayılandır; Türkiye'de resmî vakit odur. */
    data class Method(
        val fajr: Double, val isha: Double, val ishaMinutes: Double = 0.0, val maghrib: Double = 0.833,
        val sunrise: Double = 0.833, val adjustments: Adjustments? = null, val rounding: Rounding = Rounding.NEAREST,
        val imsakMinutes: Double = 0.0, val duhaAltitude: Double = 0.0, val duhaMinutes: Double = 0.0,
    )

    val METHOD_IDS = listOf("diyanet", "mwl", "isna", "egypt", "makkah", "karachi", "tehran", "jafari", "gulf", "kuwait", "qatar", "singapore", "kemenag", "france", "russia")
    val METHODS: Map<String, Method> = mapOf(
        "diyanet" to Method(18.0, 17.0),
        "mwl" to Method(18.0, 17.0),
        "isna" to Method(15.0, 15.0),
        "egypt" to Method(19.5, 17.5),
        "makkah" to Method(18.5, 0.0, ishaMinutes = 90.0),
        "karachi" to Method(18.0, 18.0),
        "tehran" to Method(17.7, 14.0, maghrib = 4.5),
        "jafari" to Method(16.0, 14.0, maghrib = 4.0),
        "gulf" to Method(19.5, 0.0, ishaMinutes = 90.0),
        "kuwait" to Method(18.0, 17.5),
        "qatar" to Method(18.0, 0.0, ishaMinutes = 90.0),
        "singapore" to Method(20.0, 18.0),
        // Kemenag (Endonezya; sitedeki METHODS.kemenag): 20°/18°, ufuk 1° altı, ihtiyat dakikaları, yukarı yuvarlama,
        // imsak uyarısı (−10 dk) ve duha (+4,5°, +2 dk)
        "kemenag" to Method(
            20.0, 18.0, maghrib = 1.0, sunrise = 1.0, adjustments = Adjustments(2.0, -2.0, 3.0, 2.0, 2.0, 2.0),
            rounding = Rounding.CEIL, imsakMinutes = 10.0, duhaAltitude = 4.5, duhaMinutes = 2.0,
        ),
        "france" to Method(12.0, 12.0),
        "russia" to Method(16.0, 15.0),
    )

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
        // Yatsı akşamdan sabit dakika sonra (Ümmü'l-Kura, Körfez, Katar)
        if (p.ishaMinutes != 0.0) isha = maghrib + p.ishaMinutes / 60
        val a = p.adjustments
        fajr += a.fajr / 60; sunrise += a.sunrise / 60; dhuhr += a.dhuhr / 60
        asr += a.asr / 60; maghrib += a.maghrib / 60; isha += a.isha / 60

        fun value(x: Double): Double? = if (x.isNaN()) null else x
        return PrayerDay(value(fajr), value(sunrise), value(dhuhr), value(asr), value(maghrib), value(isha), p.rounding)
    }

    /** Yöntemin ek satırları (yalnız Kemenag; sitedeki `methodRows`): dakika olarak imsak uyarısı ve duha. */
    data class MethodRows(val imsak: Int?, val duha: Int?)

    fun methodRows(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, utcOffset: Double, parameters: Parameters, times: PrayerDay): MethodRows? {
        if (parameters.imsakMinutes == 0.0) return null
        val du = sunAltitudeTimes(year, month, day, latitude, longitude, utcOffset, parameters.duhaAltitude).rise
        return MethodRows(
            imsak = times.minutes(Prayer.FAJR)?.let { it - parameters.imsakMinutes.toInt() },
            duha = du?.let { toMinutes(it + parameters.duhaMinutes / 60, parameters.rounding) },
        )
    }

    /** Bir gün için ek satırlar; saat dilimi farkı o günün UTC öğlesindeki farktır. */
    fun methodRows(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId, parameters: Parameters, times: PrayerDay): MethodRows? {
        val off = zone.rules.getOffset(date.atTime(12, 0).toInstant(ZoneOffset.UTC)).totalSeconds / 3600.0
        return methodRows(date.year, date.monthValue, date.dayOfMonth, latitude, longitude, off, parameters, times)
    }

    /**
     * Bölgeye göre varsayılan yöntem (sitedeki `defaultMethod`): kullanıcı seçmediyse yerin saat dilimine (Kerala için
     * koordinata) bakılır. Endonezya Kemenag; Malezya, Singapur, Brunei Singapur; Pakistan, Hindistan, Bangladeş,
     * Afganistan Karaçi + Hanefî (Kerala ve Lakşadvip'te standart ikindi); diğer her yer Diyanet.
     */
    private val REGION_TZ = mapOf(
        "Asia/Jakarta" to "ID", "Asia/Pontianak" to "ID", "Asia/Makassar" to "ID", "Asia/Jayapura" to "ID", "Asia/Ujung_Pandang" to "ID",
        "Asia/Kuala_Lumpur" to "MY", "Asia/Kuching" to "MY", "Asia/Singapore" to "SG", "Singapore" to "SG", "Asia/Brunei" to "BN",
        "Asia/Karachi" to "PK", "Asia/Kolkata" to "IN", "Asia/Calcutta" to "IN", "Asia/Dhaka" to "BD", "Asia/Dacca" to "BD", "Asia/Kabul" to "AF",
        "Europe/Istanbul" to "TR", "Asia/Istanbul" to "TR", "Turkey" to "TR",
    )
    fun region(zoneId: String): String = REGION_TZ[zoneId] ?: ""
    data class DefaultMethod(val id: String, val hanafi: Boolean)
    fun defaultMethod(zoneId: String, lat: Double, lon: Double): DefaultMethod = when (val r = region(zoneId)) {
        "ID" -> DefaultMethod("kemenag", false)
        "MY", "SG", "BN" -> DefaultMethod("singapore", false)
        "PK", "IN", "BD", "AF" -> DefaultMethod("karachi", !(r == "IN" && lat >= 8 && lat <= 12.8 && lon >= 71.5 && lon <= 77.4))
        else -> DefaultMethod("diyanet", false)
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

    /** Güneş'in bir yüksekliğe sabah çıktığı ve akşam indiği an (yerel ondalık saat; oluşmuyorsa null). */
    data class AltitudeTimes(val rise: Double?, val set: Double?)

    /**
     * Güneş'in [altitude] dereceye (ufkun üstü pozitif) sabah çıktığı ve akşam indiği an; düzeltmesiz, ikinci
     * yaklaşımla. Sitedeki `sunAltTimes` ile birebir (test verisi `sun-altitude.json`).
     */
    fun sunAltitudeTimes(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, utcOffset: Double, altitude: Double): AltitudeTimes {
        val jd = julian(year, month, day) - longitude / 360
        fun at(t: Double, ccw: Boolean): Double {
            val p = sunPosition(jd + t)
            val a = (dsin(altitude) - dsin(p.decl) * dsin(latitude)) / (dcos(p.decl) * dcos(latitude))
            if (a < -1 || a > 1) return Double.NaN
            return fixHour(12 - p.eqt) + (if (ccw) -1 else 1) * dacos(a) / 15
        }
        var rise = at(6.0 / 24, true)
        var set = at(18.0 / 24, false)
        if (!rise.isNaN()) rise = at(rise / 24, true)
        if (!set.isNaN()) set = at(set / 24, false)
        val off = utcOffset - longitude / 15
        return AltitudeTimes(if (rise.isNaN()) null else rise + off, if (set.isNaN()) null else set + off)
    }

    /** Altın saat (Güneş +6° ile −4° arası) ve mavi saat (−4° ile −6° arası), sabah ve akşam [başlangıç, bitiş]. */
    data class LightHours(
        val blueAm: Pair<Double?, Double?>, val goldenAm: Pair<Double?, Double?>,
        val goldenPm: Pair<Double?, Double?>, val bluePm: Pair<Double?, Double?>,
    )

    fun lightHours(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, utcOffset: Double): LightHours {
        val g = sunAltitudeTimes(year, month, day, latitude, longitude, utcOffset, 6.0)
        val e = sunAltitudeTimes(year, month, day, latitude, longitude, utcOffset, -4.0)
        val b = sunAltitudeTimes(year, month, day, latitude, longitude, utcOffset, -6.0)
        return LightHours(b.rise to e.rise, e.rise to g.rise, g.set to e.set, e.set to b.set)
    }

    /**
     * Kerahat vakitleri (yaklaşık; Diyanet'in yayımladığı dakika ölçüsü yok, takvimlerde yaygın 45 dk): işrak güneş
     * vaktinden 45 dk sonrasına, istiva öğleden 45 dk önceden öğleye, isfirar akşamdan 45 dk önceden akşama.
     */
    const val KERAHAT_MIN = 45
    data class Kerahat(val israk: Pair<Double, Double>?, val istiva: Pair<Double, Double>?, val isfirar: Pair<Double, Double>?)

    fun kerahat(day: PrayerDay): Kerahat {
        val k = KERAHAT_MIN / 60.0
        return Kerahat(
            day.sunrise?.let { it to it + k },
            day.dhuhr?.let { it - k to it },
            day.maghrib?.let { it - k to it },
        )
    }

    /** Bayram namazı vakti (Diyanet: güneş vaktinden 38 dk sonra; 2. analiz #56). Sitedeki `bayram` ile aynı. */
    const val BAYRAM_MIN = 38
    fun bayram(day: PrayerDay): Double? = day.sunrise?.let { it + BAYRAM_MIN / 60.0 }

    /**
     * Ek vakitler (Türkiye Takvimi tanımlarıyla yaklaşık; 2. analiz #59): işrak ve isfirar Güneş +5°, iştibak −10°,
     * işa-i sani −19°, dahve-i kübra imsak ile akşamın ortası; gece akşamdan imsaka: gece yarısı ½, teheccüd ⅔, seher ⅚.
     * Sitedeki `extraTimes` ile birebir; `day` Diyanet parametreleriyle hesaplanmış gün olmalı.
     */
    data class ExtraTimes(
        val israk: Double?, val duha: Double?, val isfirar: Double?, val istibak: Double?, val isaSani: Double?,
        val midnight: Double?, val tahajjud: Double?, val seher: Double?,
    )

    fun extraTimes(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, utcOffset: Double, times: PrayerDay): ExtraTimes {
        val up = sunAltitudeTimes(year, month, day, latitude, longitude, utcOffset, 5.0)
        val st = sunAltitudeTimes(year, month, day, latitude, longitude, utcOffset, -10.0)
        val is2 = sunAltitudeTimes(year, month, day, latitude, longitude, utcOffset, -19.0)
        val f = times.fajr; val m = times.maghrib
        val night = if (f != null && m != null) f + 24 - m else null
        return ExtraTimes(
            up.rise, if (f != null && m != null) (f + m) / 2 else null, up.set, st.set, is2.set,
            night?.let { m!! + it / 2 }, night?.let { m!! + it * 2 / 3 }, night?.let { m!! + it * 5 / 6 },
        )
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
