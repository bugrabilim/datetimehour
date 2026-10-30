package tr.bumba.time.core

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Alarm kapatma görevleri (30 Ekim 2026, rakip analizi #13): matematik, sallama, adım, kod (QR/barkod) okutma; ve
 * uyanma denetimi. Burada yalnız hesap var; sensör ve kamera uygulamada.
 */
object WakeTask {
    val KINDS = listOf("none", "math", "shake", "steps", "code", "memory", "typing", "photo")
    /** Matematik: üç işlem; sallama 30 kez; adım 20 */
    const val MATH_COUNT = 3
    const val SHAKES = 30
    const val STEPS = 20
    /** Uyanma denetimi: kapattıktan sonra sorulur, cevap gelmezse alarm yeniden çalar */
    const val CHECK_AFTER_MIN = 5
    const val CHECK_GRACE_MIN = 2
    /** Görev çözülmeden kapatılırsa alarm bir dakika sonra yeniden çalar; en çok 10 kez */
    const val REPEAT_MIN = 1
    const val REPEAT_MAX = 10

    data class Problem(val text: String, val answer: Int)

    /** Hafıza görevi (rakip analizi listesi #75): 3×3 ızgarada yanıp sönen karelerin sırası; uzunluk 4/5/6 (zorluk) */
    fun memorySequence(level: Int, rnd: Random): List<Int> {
        val n = 3 + level.coerceIn(1, 3)
        val out = mutableListOf<Int>()
        while (out.size < n) { val c = rnd.nextInt(9); if (out.lastOrNull() != c) out += c }
        return out
    }

    /** Yazma görevi: büyük/küçük harf, baştaki/sondaki boşluk ve noktalama farkı sayılmaz */
    fun typed(target: String, input: String): Boolean {
        fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() || it == ' ' }.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
        return norm(target).isNotEmpty() && norm(target) == norm(input)
    }

    /** Fotoğraf görevi: 9×8 gri görüntüden 64 bit fark özeti (dHash); kayıtlı yerle aynı sahne mi diye karşılaştırılır */
    fun dHash(gray: IntArray): Long {
        require(gray.size == 72)
        var h = 0L
        for (y in 0 until 8) for (x in 0 until 8) {
            h = (h shl 1) or (if (gray[y * 9 + x] > gray[y * 9 + x + 1]) 1L else 0L)
        }
        return h
    }

    /** Aynı sahne sayılan en büyük fark (64 bitten); ışık ve açı değişimine pay bırakır */
    const val PHOTO_MAX_DISTANCE = 18
    fun samePhoto(a: Long, b: Long): Boolean = java.lang.Long.bitCount(a xor b) <= PHOTO_MAX_DISTANCE

    /** level: 1 kolay (iki basamaklı toplama), 2 orta (çarpma + toplama), 3 zor (iki basamaklı çarpma + toplama) */
    fun problem(level: Int, rnd: Random): Problem = when (level) {
        1 -> {
            val a = rnd.nextInt(12, 90); val b = rnd.nextInt(12, 90)
            Problem("$a + $b", a + b)
        }
        2 -> {
            val a = rnd.nextInt(12, 30); val b = rnd.nextInt(3, 10); val c = rnd.nextInt(10, 100)
            Problem("$a × $b + $c", a * b + c)
        }
        else -> {
            val a = rnd.nextInt(13, 50); val b = rnd.nextInt(12, 30); val c = rnd.nextInt(100, 1000)
            Problem("$a × $b + $c", a * b + c)
        }
    }
}

/**
 * İvmeölçerden sayma (izin istemez): büyüklük (g) [high] eşiğini yukarı kestiğinde bir sayılır, yeniden sayılması için
 * önce [low] altına inmeli ve en az [gapMs] geçmeli. Sallama: 2 g; adım: 1,18 g (yürürken her adımda bir tepe).
 */
class MotionCounter(private val high: Double, private val low: Double, private val gapMs: Long) {
    private var armed = true
    private var lastAt = Long.MIN_VALUE / 2
    var count = 0
        private set

    /** x, y, z: g biriminde (yer çekimi dahil); sayıldıysa true */
    fun feed(tMs: Long, x: Double, y: Double, z: Double): Boolean {
        val m = sqrt(x * x + y * y + z * z)
        if (m < low) armed = true
        if (armed && m > high && tMs - lastAt >= gapMs) {
            armed = false
            lastAt = tMs
            count++
            return true
        }
        return false
    }

    companion object {
        fun shake() = MotionCounter(2.0, 1.3, 150)
        fun steps() = MotionCounter(1.18, 1.04, 280)
        const val G = 9.80665
    }
}
