package tr.bumba.time.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Uyku planı (30 Ekim 2026, rakip analizi #17): 90 dakikalık uyku döngüleri (sitedeki alarm sayfalarıyla aynı kural:
 * uykuya dalmak için 15 dk + döngüler) ve gün doğumu ışığı (alarmdan önce ekranın yavaşça aydınlanması).
 * Uyku takibi (mikrofon, ses kaydı, uyku evreleri) bilerek yok; yalnız akıllı alarm için hareket (SmartWake, aşağıda).
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

/**
 * Akıllı alarm (31 Ekim 2026, sahibin kararı: uyku takibinden yalnız akıllı alarm). Telefon yataktayken ivmeölçerin
 * büyüklüğü (g, yön değişse de aynı kalır) yavaş izlenen tabana göre [threshold]'tan fazla saparsa o örnek "hareket" sayılır;
 * bir dakikada en az [minSamples] hareketli örnek varsa o dakika hareketlidir. Alarm penceresi içinde son [span] dakikanın
 * en az [need]'si hareketliyse (hafif uyku ya da uyanma belirtisi) erken çalınır. Kayıt yok; yalnız bellekte son 10 dakika.
 * iOS SaatCore'daki SmartWake ile aynı kural ve aynı sayılar.
 */
class SmartWake(
    private val threshold: Double = 0.025,
    private val minSamples: Int = 3,
    private val need: Int = 2,
    private val span: Int = 3,
) {
    private var base = Double.NaN
    private val counts = HashMap<Long, Int>()
    /** Pencere içinde hareketli bulunan dakika sayısı (ekranda) */
    var movingMinutes = 0
        private set
    private var lastCounted = Long.MIN_VALUE

    /** x, y, z: g biriminde (yer çekimi dahil) */
    fun add(tMs: Long, x: Double, y: Double, z: Double) {
        val m = kotlin.math.sqrt(x * x + y * y + z * z)
        if (base.isNaN()) { base = m; return }
        val d = kotlin.math.abs(m - base)
        base += 0.05 * (m - base)
        if (d <= threshold) return
        val e = Math.floorDiv(tMs, EPOCH_MS)
        val n = (counts[e] ?: 0) + 1
        counts[e] = n
        if (n == minSamples && e != lastCounted) { movingMinutes++; lastCounted = e }
        counts.keys.removeAll { it < e - 10 }
    }

    /** Son [span] dakikadan (şimdiki dahil) kaçı hareketli */
    fun movingEpochs(nowMs: Long): Int {
        val e = Math.floorDiv(nowMs, EPOCH_MS)
        return (0 until span).count { (counts[e - it] ?: 0) >= minSamples }
    }

    /** Pencerede (alarmdan [windowMin] dk önce – alarm) ve yeterli hareket varsa true */
    fun shouldWake(nowMs: Long, alarmMs: Long, windowMin: Int): Boolean =
        windowMin > 0 && nowMs >= alarmMs - windowMin * 60_000L && nowMs < alarmMs && movingEpochs(nowMs) >= need

    companion object {
        const val EPOCH_MS = 60_000L
        /** Alarmdan bu kadar sonra oturum kendiliğinden kapanır (alarm zaten çalmıştır) */
        const val AFTER_MS = 2 * 60_000L
    }
}
