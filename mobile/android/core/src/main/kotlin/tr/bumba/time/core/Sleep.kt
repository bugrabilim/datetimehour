package tr.bumba.time.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Uyku planı (30 Ekim 2026, rakip analizi #17): 90 dakikalık uyku döngüleri (sitedeki alarm sayfalarıyla aynı kural:
 * uykuya dalmak için 15 dk + döngüler) ve gün doğumu ışığı (alarmdan önce ekranın yavaşça aydınlanması).
 * Uyku takibi (mikrofon, hareket) bilerek yok.
 */
object Sleep {
    const val FALL_ASLEEP = 15
    const val CYCLE = 90
    val CYCLES = listOf(6, 5, 4)

    /** Günün dakikası (0–1439) */
    fun mod(m: Int) = ((m % 1440) + 1440) % 1440

    /** Kalkış saatine göre yatma saatleri (6, 5, 4 döngü) */
    fun bedtimes(wake: Int) = CYCLES.map { it to mod(wake - FALL_ASLEEP - it * CYCLE) }

    /** Şimdi yatılırsa kalkış saatleri (6, 5, 4 döngü) */
    fun wakeTimes(now: Int) = CYCLES.map { it to mod(now + FALL_ASLEEP + it * CYCLE) }

    /** Planın yatma saati: kalkıştan uyku süresi kadar önce */
    fun bedtime(wake: Int, duration: Int) = mod(wake - duration)
}

/**
 * Gün doğumu ışığı: alarmdan [window] dakika önce başlar, alarm anında tam aydınlık olur, 15 dk sonra söner.
 * Ekran yalnız tam ekran saat açıkken aydınlanır (telefonlar uygulama kapalıyken ekranı açamaz).
 */
object SunriseLight {
    const val AFTER_MS = 15 * 60_000L

    /** 0 (karanlık) – 1 (tam aydınlık); yumuşak başlangıçlı (ilk dakikalar çok kısık) */
    fun level(now: Long, alarmAt: Long, window: Int): Double {
        if (window <= 0) return 0.0
        val start = alarmAt - window * 60_000L
        if (now < start || now > alarmAt + AFTER_MS) return 0.0
        if (now >= alarmAt) return 1.0
        val x = (now - start).toDouble() / (alarmAt - start)
        return x * x * (3 - 2 * x)
    }

    private val STOPS = listOf(
        0.0 to intArrayOf(0, 0, 0),
        0.25 to intArrayOf(70, 10, 6),
        0.55 to intArrayOf(196, 72, 22),
        0.8 to intArrayOf(255, 160, 72),
        1.0 to intArrayOf(255, 236, 204),
    )

    /** Işığın rengi (koyu kırmızıdan turuncuya, sonra sıcak beyaza); 0xRRGGBB */
    fun color(level: Double): Int {
        val l = max(0.0, min(1.0, level))
        val i = STOPS.indexOfLast { it.first <= l }.coerceAtMost(STOPS.size - 2)
        val (a, ca) = STOPS[i]
        val (b, cb) = STOPS[i + 1]
        val k = (l - a) / (b - a)
        val c = IntArray(3) { (ca[it] + (cb[it] - ca[it]) * k).roundToInt() }
        return (c[0] shl 16) or (c[1] shl 8) or c[2]
    }

    /** Ekran parlaklığı (0,05 – 1) */
    fun brightness(level: Double) = 0.05 + 0.95 * max(0.0, min(1.0, level))
}
