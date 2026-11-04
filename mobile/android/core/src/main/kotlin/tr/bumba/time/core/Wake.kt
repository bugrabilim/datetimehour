package tr.bumba.time.core

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Alarm kapatma görevleri (30 Ekim 2026, rakip analizi #13): matematik, sallama, adım, kod (QR/barkod) okutma; ve
 * uyanma denetimi. Burada yalnız hesap var; sensör ve kamera uygulamada.
 */
object WakeTask {
    val KINDS = listOf("none", "math", "shake", "steps", "code", "memory", "typing", "photo", "squat", "object", "speak")
    /** Squat (2. analiz #36): telefon elde 15 çömelme; ivme büyüklüğü 1,3 g'yi aşıp 0,95 g altına iner, en az 0,9 sn arayla */
    const val SQUATS = 15
    /** Nesne görevi (#36): kameradaki görüntü telefonda sınıflandırılır (iOS Vision, Android ML Kit); hedefin sözcüklerinden
     *  biri en az [OBJECT_MIN] güvenle etiketlerde geçerse çözülür. Etiketler İngilizce, küçük harfe çevrilerek aranır. */
    val OBJECTS = linkedMapOf(
        "cup" to listOf("cup", "mug", "coffee", "tea", "drinking glass", "teapot", "tableware", "drinkware", "glass"),
        "toothbrush" to listOf("toothbrush", "toothpaste", "sink", "bathroom", "faucet", "tap"),
        "sky" to listOf("sky", "cloud", "sunrise", "sunset", "outdoor"),
        "bed" to listOf("bed", "bedroom", "pillow", "blanket", "duvet", "bedding", "mattress", "quilt"),
        "plant" to listOf("plant", "flower", "houseplant", "flowerpot", "leaf", "tree"),
        "shoe" to listOf("shoe", "sneaker", "footwear", "boot", "sandal", "slipper"),
    )
    const val OBJECT_MIN = 0.3f

    /** Tam sözcük eşleşmesi ("bed" "flowerbed"de, "tree" "street"te geçmez); sondaki çoğul -s kabul edilir */
    fun objectMatch(target: String, labels: List<Pair<String, Float>>): Boolean {
        val words = OBJECTS[target] ?: return false
        return labels.any { (name, conf) ->
            val n = " " + name.lowercase().map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("").trim().replace(Regex(" +"), " ") + " "
            conf >= OBJECT_MIN && words.any { w -> n.contains(" $w ") || n.contains(" ${w}s ") || n.contains(" ${w}es ") }
        }
    }

    /** Sesli okuma (#37): duyulan sözcüklerin en az %70'i hedefte sırayla geçiyorsa (büyük/küçük harf, noktalama yok sayılır) */
    fun spoken(target: String, heard: String): Boolean {
        // Sözcükleri boşlukla ayrılmayan yazılar (Japonca, Çince, Tayca): harf düzeyinde, en uzun ortak alt dizi ≥ %70
        if (target.any { it in '\u0E00'..'\u0E7F' || it in '\u3040'..'\u30FF' || it in '\u4E00'..'\u9FFF' }) {
            fun chars(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
            val a = chars(target)
            val b = chars(heard)
            if (a.isEmpty() || b.isEmpty()) return false
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 1..a.length) for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1] + 1 else maxOf(dp[i - 1][j], dp[i][j - 1])
            }
            return dp[a.length][b.length] >= kotlin.math.ceil(a.length * 0.7)
        }
        fun words(s: String) = s.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val t = words(target)
        val h = words(heard)
        if (t.isEmpty() || h.isEmpty()) return false
        var i = 0
        var hit = 0
        for (w in t) {
            val j = h.subList(i, h.size).indexOf(w)
            if (j >= 0) { hit++; i += j + 1 }
        }
        return hit >= kotlin.math.ceil(t.size * 0.7)
    }
    /** Işık görevi (2. analiz #38; yalnız Android): en az bu kadar lüks (ve başlangıcın 3 katı) 3 sn boyunca */
    const val LIGHT_LUX = 100f
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
        fun squat() = MotionCounter(1.3, 0.95, 900)
        const val G = 9.80665
    }
}
