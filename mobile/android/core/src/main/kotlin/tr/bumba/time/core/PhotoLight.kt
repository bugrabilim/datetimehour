package tr.bumba.time.core

import kotlin.math.abs
import kotlin.math.max

/**
 * Fotoğraf ışığı (9 Kasım 2026, sahibin onayı): seçili yer ve gün için altın/mavi saat, gün doğumu/batımı ve Güneş'in
 * doğduğu/battığı yön, bulut tahmininden gün doğumu ve batımı fotoğrafı tahmini (iyi / orta / kötü).
 * Saatler mevcut Güneş yüksekliği işlevlerinden (Mevsimler'deki tanımlar: altın saat +6° ile −4°, mavi saat −4° ile −6°,
 * ufuk −0,833°); yeni gökbilim formülü yok. Tahmin kuralı sitedeki fotoğraf ışığı sayfası ve SaatCore/PhotoLight.swift
 * ile birebir aynı (testler iki tarafta aynı örneklerle).
 */
object PhotoLight {
    enum class Quality(val key: String) { GOOD("good"), FAIR("fair"), POOR("poor") }

    /**
     * Gün doğumu/batımı fotoğrafı tahmini. [low] alçak, [mid] orta, [high] yüksek bulut (%), [precip] yağış (mm/sa); gün
     * doğumuna ya da batımına en yakın saatlik değer. Renkleri orta ve yüksek bulutlar yakalar; ufuktaki alçak bulut ve
     * yağış Güneş'i kapatır; açık gökyüzü temiz ama sade.
     */
    fun quality(low: Double, mid: Double, high: Double, precip: Double): Quality {
        if (precip >= 0.5 || low >= 75) return Quality.POOR
        val upper = max(mid, high)
        if (low < 40 && upper >= 20 && upper <= 80) return Quality.GOOD
        if (low < 60) return Quality.FAIR
        return Quality.POOR
    }

    /** Saatlik bulut ve yağış örneği (t: UTC milisaniye) */
    data class Sample(val t: Long, val low: Double, val mid: Double, val high: Double, val precip: Double)

    /** [t] anına en yakın saatlik örnek; [maxGap] milisaniyeden uzaksa (tahmin aralığı dışında) null */
    fun nearest(samples: List<Sample>, t: Long, maxGap: Long = 90 * 60_000L): Sample? =
        samples.minByOrNull { abs(it.t - t) }?.takeIf { abs(it.t - t) <= maxGap }

    /** [t] anı için tahmin (örnek yoksa null) */
    fun qualityAt(samples: List<Sample>, t: Long): Quality? =
        nearest(samples, t)?.let { quality(it.low, it.mid, it.high, it.precip) }

    /** Gün doğumu ve batımı için ufuk yüksekliği (Mevsimler'deki gibi, vakit düzeltmesi yok) */
    const val HORIZON = -0.833

    /**
     * Bir günün ışığı. Saatler yerel ondalık saat (24'ü geçebilir: ertesi gün; oluşmuyorsa null), yönler derece (kuzeyden
     * saat yönünde). [polar]: Güneş bütün gün ufkun üstünde 1 (gece yarısı güneşi), altında −1 (kutup gecesi), olağan 0.
     */
    data class Day(
        val light: PrayerTimes.LightHours,
        val sunrise: Double?,
        val sunset: Double?,
        val riseAzimuth: Double?,
        val setAzimuth: Double?,
        val polar: Int,
    )

    fun day(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, utcOffset: Double): Day {
        val light = PrayerTimes.lightHours(year, month, day, latitude, longitude, utcOffset)
        val s = PrayerTimes.sunAltitudeTimes(year, month, day, latitude, longitude, utcOffset, HORIZON)
        fun azimuth(h: Double?): Double? = h?.let { Qibla.sun(year, month, day, it - utcOffset, latitude, longitude).az }
        val polar = when {
            s.rise != null || s.set != null -> 0
            // Yerel güneş öğlesinde (UTC 12 − boylam/15) Güneş ufkun üstündeyse bütün gün üstündedir
            Qibla.sun(year, month, day, 12 - longitude / 15, latitude, longitude).alt > HORIZON -> 1
            else -> -1
        }
        return Day(light, s.rise, s.set, azimuth(s.rise), azimuth(s.set), polar)
    }
}
