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

/**
 * Uyku düzeni (2. analiz #47; uyku takibi olmadan): kalkış anları alarmın kapatıldığı ya da çaldığı andan, yatış anları
 * uyku sesleri / akıllı alarm oturumu / tam ekran uyku zamanlayıcısının başladığı andan gelir. Kayıt `sleep.log`
 * ([{d: kalkış günü ISO, w: kalkış dakikası, b: yatış dakikası ya da −1}], en çok 120; iOS'taki SleepStats ile aynı).
 */
object SleepStats {
    data class Entry(val d: String, val w: Int, val b: Int = -1)
    data class Summary(val weekWake: Int?, val prevWake: Int?, val spread: Int?, val debt: Int?, val nights: Int)

    const val MAX = 120

    /** Günün ilk kalkışı kalır (erteleme sonrası kapatma ya da ikinci alarm aynı günü bozmasın); yatış sonradan eklenebilir */
    fun record(log: List<Entry>, e: Entry): List<Entry> {
        val old = log.firstOrNull { it.d == e.d }
        val merged = if (old == null) e else Entry(e.d, minOf(old.w, e.w), if (old.b >= 0) old.b else e.b)
        return (log.filter { it.d != e.d } + merged).sortedBy { it.d }.takeLast(MAX)
    }

    /** Yatıştan kalkışa süre (dakika); yatış yoksa null */
    fun duration(e: Entry): Int? = if (e.b < 0) null else ((e.w - e.b) % 1440 + 1440) % 1440

    fun summary(log: List<Entry>, today: String, goal: Int): Summary {
        val t = java.time.LocalDate.parse(today)
        val w0 = t.minusDays(7).toString()
        val w1 = t.minusDays(14).toString()
        val week = log.filter { it.d > w0 && it.d <= today }
        val prev = log.filter { it.d > w1 && it.d <= w0 }
        fun avg(l: List<Entry>) = if (l.isEmpty()) null else (l.sumOf { it.w }.toDouble() / l.size).roundToInt()
        val recent = log.filter { it.d > w1 && it.d <= today }
        val spread = if (recent.size < 3) null else {
            val m = recent.sumOf { it.w }.toDouble() / recent.size
            kotlin.math.sqrt(recent.sumOf { (it.w - m) * (it.w - m) } / recent.size).roundToInt()
        }
        val durs = week.mapNotNull { duration(it) }
        val debt = if (durs.isEmpty()) null else durs.sumOf { max(0, goal - it) }
        return Summary(avg(week), avg(prev), spread, debt, week.size)
    }
}

/**
 * Kronotip tahmini (2. analiz #48): serbest günlerdeki uyku ortası, hafta içi uyku açığına göre düzeltilir (MCTQ'daki
 * MSFsc fikri; anketin kendisi kullanılmaz). Dakika, gece yarısına göre −720…720; kategori 0 belirgin sabahçıl …
 * 4 belirgin akşamcıl. Bilimsel ölçek değil, kısa bir tahmindir.
 */
object Chronotype {
    private fun dur(bed: Int, wake: Int) = ((wake - bed) % 1440 + 1440) % 1440

    fun msfsc(workBed: Int, workWake: Int, freeBed: Int, freeWake: Int, workDays: Int = 5): Int {
        val sdw = dur(workBed, workWake)
        val sdf = dur(freeBed, freeWake)
        val msf = freeBed + sdf / 2.0
        val week = (sdw * workDays + sdf * (7 - workDays)) / 7.0
        val m = if (sdf > sdw) msf - (sdf - week) / 2 else msf
        val r = ((m.roundToInt() % 1440) + 1440) % 1440
        return if (r >= 720) r - 1440 else r
    }

    fun category(msfsc: Int): Int = when {
        msfsc < 150 -> 0
        msfsc < 210 -> 1
        msfsc < 270 -> 2
        msfsc < 330 -> 3
        else -> 4
    }
}
