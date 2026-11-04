package tr.bumba.time.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
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
    /** Gürültüler ve doğa sesleri (2. analiz #30: dere, rüzgâr, şömine, cırcır böceği, tik-tak) */
    val KINDS = listOf("white", "pink", "brown", "rain", "waves", "stream", "wind", "fire", "crickets", "tick")
    const val RATE = 44100
    private const val RMS = 0.2

    fun seconds(kind: String) = if (kind == "waves") 10 else 8

    fun samples(kind: String, rate: Int = RATE, seed: Int = 0x5A17, secs: Int = seconds(kind)): FloatArray {
        val n = secs * rate
        val fade = rate / 2
        val raw = DoubleArray(n + fade)
        var s = if (seed == 0) 1 else seed
        fun white(): Double { // xorshift32: telefonda tekrarlanabilir
            s = s xor (s shl 13); s = s xor (s ushr 17); s = s xor (s shl 5)
            return (s.toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0 * 2 - 1
        }
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0; var last = 0.0
        val brownKind = kind == "brown" || kind == "wind" || kind == "fire"
        for (i in raw.indices) {
            val w = white()
            last = (last + 0.02 * w) / 1.02
            b0 = 0.99886 * b0 + w * 0.0555179; b1 = 0.99332 * b1 + w * 0.0750759; b2 = 0.969 * b2 + w * 0.153852
            b3 = 0.8665 * b3 + w * 0.3104856; b4 = 0.55 * b4 + w * 0.5329522; b5 = -0.7616 * b5 - w * 0.016898
            val p = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362) * 0.11
            b6 = w * 0.115926
            raw[i] = when {
                kind == "white" -> w * 0.5
                brownKind -> last * 3.5
                else -> p
            }
        }
        when (kind) {
            "rain" -> { biquad(raw, rate, 900.0, high = true); biquad(raw, rate, 7000.0, high = false) }
            "waves" -> biquad(raw, rate, 1200.0, high = false)
            "stream" -> { biquad(raw, rate, 300.0, high = true); biquad(raw, rate, 2500.0, high = false) }
            "wind" -> biquad(raw, rate, 500.0, high = false)
            "fire" -> biquad(raw, rate, 700.0, high = false)
        }
        // Döngü yeri: sondaki yarım saniye baştakiyle karıştırılır
        val out = DoubleArray(n)
        for (i in 0 until n) out[i] = if (i < fade) {
            val k = i.toDouble() / fade
            raw[i] * k + raw[n + i] * (1 - k)
        } else raw[i]
        when (kind) {
            "waves" -> for (i in 0 until n) {
                val e = 0.5 - 0.5 * cos(2 * PI * i / n)
                out[i] *= 0.25 + 0.75 * e.pow(1.5)
            }
            // Dere: döngüye oturan yavaş çalkantı
            "stream" -> for (i in 0 until n) out[i] *= 0.8 + 0.2 * sin(2 * PI * 3 * i / n) * sin(2 * PI * 5 * i / n)
            // Rüzgâr: döngüde iki esinti
            "wind" -> for (i in 0 until n) {
                val e = 0.5 - 0.5 * cos(2 * PI * 2 * i / n)
                out[i] *= 0.3 + 0.7 * e.pow(1.2)
            }
            // Şömine: alçak uğultu üstüne rastgele çıtırtılar (4 ms'de sönen kısa darbeler)
            "fire" -> {
                var r = (seed xor 0x2F1D).let { if (it == 0) 1 else it }
                fun rnd(): Double {
                    r = r xor (r shl 13); r = r xor (r ushr 17); r = r xor (r shl 5)
                    return (r.toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
                }
                var sq = 0.0
                for (v in out) sq += v * v
                val base = sqrt(sq / n).coerceAtLeast(1e-9)
                var amp = 0.0
                val decay = exp(-1.0 / (0.004 * rate))
                for (i in 0 until n) {
                    if (rnd() < 0.00035) amp = base * (3 + 6 * rnd())
                    out[i] = out[i] * 0.7 + amp * (rnd() * 2 - 1)
                    amp *= decay
                }
            }
            // Cırcır böceği: saniyede bir üç kısa cıvıltı (4,4 kHz), altta çok hafif uğultu
            "crickets" -> for (i in 0 until n) {
                val t = (i % rate).toDouble() / rate
                var c = 0.0
                for (k in 0 until 3) {
                    val u = (t - k * 0.06) / 0.035
                    if (u in 0.0..1.0) c += sin(PI * u) * sin(2 * PI * 4400 * i / rate)
                }
                out[i] = out[i] * 0.05 + c
            }
            // Tik-tak: saniyede bir tık; çift saniyede tiz (2,5 kHz), tekte pes (2 kHz)
            "tick" -> for (i in 0 until n) {
                val sec = i / rate
                val t = (i % rate).toDouble() / rate
                val f = if (sec % 2 == 0) 2500.0 else 2000.0
                out[i] = if (t < 0.03) exp(-t / 0.006) * sin(2 * PI * f * t) else 0.0
            }
        }
        val g = if (kind == "crickets" || kind == "tick") {
            0.7 / max(1e-9, out.maxOf { abs(it) })
        } else {
            var sq = 0.0
            for (v in out) sq += v * v
            RMS / max(1e-9, sqrt(sq / n))
        }
        return FloatArray(n) { max(-1.0, min(1.0, out[it] * g)).toFloat() }
    }

    /** Karışım tanımı ("rain=60;fire=30") → (tür, ses düzeyi) */
    fun parseMix(spec: String): List<Pair<String, Int>> = spec.split(';').mapNotNull { part ->
        val kv = part.split('=')
        val k = kv.getOrNull(0)?.trim() ?: return@mapNotNull null
        val v = kv.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
        if (k in KINDS && v > 0) k to min(100, v) else null
    }

    /** Birden çok sesin karışımı (2. analiz #30): her ses 8 sn'lik döngü, kendi ses düzeyiyle; tepe 0,95'i geçmez */
    fun mix(spec: String, rate: Int = RATE): FloatArray {
        val parts = parseMix(spec)
        val n = 8 * rate
        val sum = DoubleArray(n)
        parts.forEachIndexed { idx, (k, v) ->
            val x = samples(k, rate, seed = 0x5A17 + idx * 7919, secs = 8)
            for (i in 0 until n) sum[i] += x[i] * v / 100.0
        }
        val peak = sum.maxOfOrNull { abs(it) } ?: 0.0
        val g = if (peak > 0.95) 0.95 / peak else 1.0
        return FloatArray(n) { (sum[it] * g).toFloat() }
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

/**
 * Odak engelinin zaman çizelgesi (2. analiz #27; iOS'taki FocusSchedule ile aynı kural): gün ISO 1–7 (pazartesi 1),
 * dakika gece yarısından. [from, to) arası; from > to ise gece yarısını aşar ve başladığı günün seçimine bakılır.
 */
object FocusSchedule {
    fun active(days: Set<Int>, from: Int, to: Int, weekday: Int, minute: Int): Boolean {
        if (from == to) return false
        if (from < to) return weekday in days && minute >= from && minute < to
        val prev = (weekday + 5) % 7 + 1
        return (weekday in days && minute >= from) || (prev in days && minute < to)
    }

    /** Sıradaki sınıra (başlangıç ya da bitiş) kalan dakika: 1..1440 */
    fun minutesToNextBoundary(from: Int, to: Int, minute: Int): Int {
        fun ahead(t: Int) = ((t - minute) % 1440 + 1440) % 1440
        val a = ahead(from).let { if (it == 0) 1440 else it }
        val b = ahead(to).let { if (it == 0) 1440 else it }
        return minOf(a, b)
    }
}

/**
 * Nefes egzersizi (2. analiz #28; iOS'taki Breath ile aynı): kalıp saniyeleri sırayla nefes al, tut, ver, tut;
 * 0 saniyelik adımlar atlanır. `phase` geçen süreye göre adım (0 al, 1 tut, 2 ver, 3 tut), adımın ilerlemesi (0–1)
 * ve tamamlanan tur sayısı.
 */
object Breath {
    val PATTERNS = linkedMapOf(
        "478" to doubleArrayOf(4.0, 7.0, 8.0, 0.0),
        "box" to doubleArrayOf(4.0, 4.0, 4.0, 4.0),
        "calm" to doubleArrayOf(4.0, 0.0, 6.0, 0.0),
        "even" to doubleArrayOf(5.5, 0.0, 5.5, 0.0),
    )

    data class Phase(val step: Int, val progress: Double, val left: Double, val cycle: Int)

    fun cycleSeconds(p: DoubleArray) = p.sum()

    fun phase(p: DoubleArray, elapsed: Double): Phase {
        val total = cycleSeconds(p)
        val cycle = (elapsed / total).toInt()
        var t = elapsed - cycle * total
        for (i in p.indices) {
            if (p[i] <= 0.0) continue
            if (t < p[i]) return Phase(i, t / p[i], p[i] - t, cycle)
            t -= p[i]
        }
        return Phase(0, 0.0, p[0], cycle + 1)
    }

    /** Çemberin büyüklüğü (0 küçük, 1 büyük): alırken büyür, verirken küçülür, tutarken kalır */
    fun scale(ph: Phase): Double = when (ph.step) {
        0 -> ph.progress
        1 -> 1.0
        2 -> 1.0 - ph.progress
        else -> 0.0
    }
}
