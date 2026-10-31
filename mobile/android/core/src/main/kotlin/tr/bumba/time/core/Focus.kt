package tr.bumba.time.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pomodoro odak geçmişi (30 Ekim 2026, rakip analizi #16): sitedeki `sth-pomo-log` ile aynı kayıt
 * ({d: yerel gün "YYYY-MM-DD", m: dakika, t: görev}, en çok 1000). Kayıt yalnız telefonda durur.
 */
object FocusLog {
    const val MAX = 1000

    /** g: etiket (rakip analizi #27): work, study, personal, other; boşsa etiketsiz */
    data class Entry(val d: String, val m: Int, val t: String, val g: String = "")
    data class Sum(val n: Int, val m: Int)
    data class Stats(val today: Sum, val week: Sum, val total: Sum, val perDay: List<Int>, val top: List<Pair<String, Int>>)

    /** Yeni kaydı ekler, en eski kayıtları atar (en çok 1000); görev adı en çok 60 karakter */
    fun add(log: List<Entry>, e: Entry): List<Entry> = (log + e.copy(t = e.t.trim().take(60))).takeLast(MAX)

    /** Üst üste çalışılan gün sayısı: seri bugün ya da dün biten günlerden geriye sayılır (sitedeki tools.js ile aynı) */
    fun streak(log: List<Entry>, today: java.time.LocalDate): Int {
        val have = log.map { it.d }.toHashSet()
        var d = if (today.toString() in have) today else today.minusDays(1)
        var n = 0
        while (d.toString() in have) { n++; d = d.minusDays(1) }
        return n
    }

    /** [since] gününden bu yana etiketlere göre dakika (etiketsizler "none"), çoktan aza; eşitlikte ilk görülen önce */
    fun tags(log: List<Entry>, since: String): List<Pair<String, Int>> {
        val m = LinkedHashMap<String, Int>()
        for (e in log) if (e.d >= since) {
            val k = e.g.ifBlank { "none" }
            m[k] = (m[k] ?: 0) + e.m
        }
        return m.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    /** days: son 7 günün anahtarları, eskiden yeniye (sonuncusu bugün). Adsız görevler [noTask] adıyla toplanır. */
    fun stats(log: List<Entry>, days: List<String>, noTask: String): Stats {
        require(days.size == 7)
        fun sum(f: (Entry) -> Boolean) = log.filter(f).let { l -> Sum(l.size, l.sumOf { it.m }) }
        val first = days.first()
        val tasks = LinkedHashMap<String, Int>()
        for (e in log) if (e.d >= first) {
            val k = e.t.ifBlank { noTask }
            tasks[k] = (tasks[k] ?: 0) + e.m
        }
        return Stats(
            today = sum { it.d == days.last() },
            week = sum { it.d >= first },
            total = sum { true },
            perDay = days.map { d -> log.filter { it.d == d }.sumOf { it.m } },
            top = tasks.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value },
        )
    }
}

/**
 * Arka plan sesi (pomodoro ve uyku sesleri): sitedeki Web Audio gürültüsünün (tools.js noiseBuffer) karşılığı, telefonda
 * üretilir, dosya yok. Beyaz, pembe (Paul Kellet), kahverengi, yağmur (pembe gürültü 900 Hz yüksek geçiren + 7 kHz alçak
 * geçiren), dalga (pembe gürültü, 10 sn'lik yavaş kabarma). Arabellek döngüde çalınır; ucu başına yarım saniyede
 * karıştırıldığı için döngü yerinde tık sesi olmaz. Ses düzeyi türler arasında eşit (RMS 0,2).
 */
object NoiseGen {
    val KINDS = listOf("white", "pink", "brown", "rain", "waves")
    const val RATE = 44100
    private const val RMS = 0.2

    fun seconds(kind: String) = if (kind == "waves") 10 else 8

    fun samples(kind: String, rate: Int = RATE, seed: Int = 0x5A17): FloatArray {
        val n = seconds(kind) * rate
        val fade = rate / 2
        val raw = DoubleArray(n + fade)
        var s = if (seed == 0) 1 else seed
        fun white(): Double { // xorshift32: telefonda tekrarlanabilir
            s = s xor (s shl 13); s = s xor (s ushr 17); s = s xor (s shl 5)
            return (s.toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0 * 2 - 1
        }
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0; var last = 0.0
        for (i in raw.indices) {
            val w = white()
            raw[i] = when (kind) {
                "white" -> w * 0.5
                "brown" -> { last = (last + 0.02 * w) / 1.02; last * 3.5 }
                else -> {
                    b0 = 0.99886 * b0 + w * 0.0555179; b1 = 0.99332 * b1 + w * 0.0750759; b2 = 0.969 * b2 + w * 0.153852
                    b3 = 0.8665 * b3 + w * 0.3104856; b4 = 0.55 * b4 + w * 0.5329522; b5 = -0.7616 * b5 - w * 0.016898
                    val p = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362) * 0.11
                    b6 = w * 0.115926
                    p
                }
            }
        }
        when (kind) {
            "rain" -> { biquad(raw, rate, 900.0, high = true); biquad(raw, rate, 7000.0, high = false) }
            "waves" -> biquad(raw, rate, 1200.0, high = false)
        }
        // Döngü yeri: sondaki yarım saniye baştakiyle karıştırılır
        val out = DoubleArray(n)
        for (i in 0 until n) out[i] = if (i < fade) {
            val k = i.toDouble() / fade
            raw[i] * k + raw[n + i] * (1 - k)
        } else raw[i]
        if (kind == "waves") for (i in 0 until n) {
            val e = 0.5 - 0.5 * cos(2 * PI * i / n)
            out[i] *= 0.25 + 0.75 * e.pow(1.5)
        }
        var sq = 0.0
        for (v in out) sq += v * v
        val g = RMS / max(1e-9, sqrt(sq / n))
        return FloatArray(n) { max(-1.0, min(1.0, out[it] * g)).toFloat() }
    }

    /** RBJ ikinci derece süzgeç (Q = 0,7071), yerinde */
    private fun biquad(x: DoubleArray, rate: Int, f: Double, high: Boolean) {
        val w0 = 2 * PI * min(f, rate * 0.45) / rate
        val c = cos(w0)
        val alpha = sin(w0) / (2 * 0.7071)
        val a0 = 1 + alpha
        val b0 = (if (high) (1 + c) / 2 else (1 - c) / 2) / a0
        val b1 = (if (high) -(1 + c) else 1 - c) / a0
        val b2 = b0
        val a1 = -2 * c / a0
        val a2 = (1 - alpha) / a0
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
        for (i in x.indices) {
            val y = b0 * x[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x[i]; y2 = y1; y1 = y
            x[i] = y
        }
    }

    /** Ses düzeyi (0–100) → kazanç */
    fun gain(volume: Int) = max(0, min(100, volume)) / 100f
}
