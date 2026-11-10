package tr.bumba.time

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

/*
 * Tebrik kartları (3 Kasım 2026, 2. analiz #74; 4 Kasım 2026'da sahibin isteğiyle genişledi; iOS'taki Greetings.swift
 * ile aynı): Cuma, Ramazan, Ramazan ve Kurban bayramı, kandil ve hicri yılbaşı için telefonda çizilen kare görsel.
 * Söz her açılışta değişir (vesileye özgü dilek 6 × dua 10 × kapanış 8, 18 dilde), kendi mesajını yazan da olur;
 * desen vesileye göre (fenerler, cami, lale, Kâbe, kandil…), renk ayrıca; paylaşımda uygulamanın bağlantısı da gider.
 * Dış görsel yok; görsel yalnız kullanıcı paylaşınca sistemin paylaşım penceresiyle gider.
 */
object Greetings {
    val RELIGIOUS = listOf("cuma", "ramadan", "rb", "kb", "kandil", "hijri")
    /** Nötr vesileler (10 Kasım 2026, Müslüman olmayanlar araştırması): "Kutlama kartları" yalnız bunları gösterir */
    val NEUTRAL = listOf("birthday", "newyear", "anniversary", "graduation", "newjob")
    val OCCASIONS = RELIGIOUS + NEUTRAL
    /** Nötr kartlarda dua içeren kapanış (2: "dualarımla") kullanılmaz */
    private val NEUTRAL_CLOSES = listOf(1, 3, 4, 5, 6, 7, 8)
    val STYLES = listOf("night", "emerald", "gold")
    private val AR = mapOf(
        "cuma" to "جمعة مباركة", "ramadan" to "رمضان كريم", "rb" to "عيد مبارك", "kb" to "عيد مبارك",
        "kandil" to "ليلة مباركة", "hijri" to "عام هجري سعيد",
    )
    /** Vesileye göre desenler (ilk desen varsayılan) */
    val PATTERNS = mapOf(
        "cuma" to listOf("mosque", "geo", "arch"),
        "ramadan" to listOf("lanterns", "crescent", "geo"),
        "rb" to listOf("tulips", "lights", "geo"),
        "kb" to listOf("kaaba", "tulips", "geo"),
        "kandil" to listOf("lamp", "stars", "arch"),
        "hijri" to listOf("crescent", "stars", "geo"),
        "birthday" to listOf("lights", "stars", "tulips"),
        "newyear" to listOf("stars", "lights", "geo"),
        "anniversary" to listOf("tulips", "lights", "stars"),
        "graduation" to listOf("stars", "geo", "lights"),
        "newjob" to listOf("geo", "tulips", "stars"),
    )
    /** Play sayfası (uygulama yayımlanınca açılır) */
    const val STORE = "https://play.google.com/store/apps/details?id=tr.bumba.time"

    /** Söz: vesileye özgü dilek + dua + kapanış; tohumdan seçilir (iOS'taki Greetings.message ile aynı kural) */
    fun message(occasion: String, seed: Long): String {
        val u = seed and 0x7FFFFFFFL
        if (occasion in NEUTRAL) {
            // Nötr: vesile sözü (3) + dilek (6, dua yok) + kapanış (7)
            val w = (u % 3).toInt() + 1; val b = ((u / 3) % 6).toInt() + 1; val c = NEUTRAL_CLOSES[((u / 18) % NEUTRAL_CLOSES.size).toInt()]
            return listOf(L("greet.m.$occasion.$w"), L("greet.wish.$b"), L("greet.close.$c")).joinToString("\n")
        }
        val w = (u % 6).toInt() + 1; val b = ((u / 6) % 10).toInt() + 1; val c = ((u / 60) % 8).toInt() + 1
        return listOf(L("greet.m.$occasion.$w"), L("greet.bless.$b"), L("greet.close.$c")).joinToString("\n")
    }

    /** Kutlama kartları açılışı: yılbaşına yakınsa (20 Aralık–7 Ocak) yeni yıl, değilse doğum günü */
    fun suggestNeutral(today: LocalDate): String =
        if ((today.monthValue == 12 && today.dayOfMonth >= 20) || (today.monthValue == 1 && today.dayOfMonth <= 7)) "newyear" else "birthday"

    /** Bugüne en uygun vesile: yakın bayram/kandil/hicri yılbaşı/Ramazan, yoksa Cuma */
    fun suggest(today: LocalDate): String {
        fun near(keys: Set<String>, days: Long) = RelDays.all.any { it.key in keys && ChronoUnit.DAYS.between(today, it.date) in -2..days }
        return when {
            near(setOf("rb1", "rbEve"), 3) -> "rb"
            near(setOf("kb1", "kbEve"), 3) -> "kb"
            near(setOf("regaib", "mirac", "berat", "kadir", "mawlid"), 1) -> "kandil"
            near(setOf("hijri"), 1) -> "hijri"
            AppData.ramadans.any { today in it } -> "ramadan"
            else -> "cuma"
        }
    }

    fun render(occasion: String, style: String, pattern: String, message: String, today: LocalDate, size: Int = 1080): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val s = size.toFloat()
        val (top, bottom, ink, gold) = when (style) {
            "emerald" -> listOf(0xFF0B4D3B.toInt(), 0xFF06281F.toInt(), 0xFFFFFFFF.toInt(), 0xFFE9C46A.toInt())
            "gold" -> listOf(0xFFFBF3E0.toInt(), 0xFFEAD9B0.toInt(), 0xFF3B2A12.toInt(), 0xFFA8761E.toInt())
            else -> listOf(0xFF101A3F.toInt(), 0xFF2A1D52.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFD173.toInt())
        }
        c.drawRect(0f, 0f, s, s, Paint().apply { shader = LinearGradient(0f, 0f, 0f, s, top, bottom, Shader.TileMode.CLAMP) })
        GreetMotifs.draw(pattern, c, s, gold, ink, top, bottom)
        // Yumuşak ışık
        c.drawCircle(s / 2, s * 0.5f, s * 0.45f, Paint().apply {
            shader = RadialGradient(s / 2, s * 0.5f, s * 0.45f, (gold and 0x00FFFFFF) or 0x22000000, 0, Shader.TileMode.CLAMP)
        })
        fun layout(text: String, size: Float, color: Int, bold: Boolean): StaticLayout {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT }
            return StaticLayout.Builder.obtain(text, 0, text.length, p, (s * 0.72f).toInt()).setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(s * 0.006f, 1f).build()
        }
        val gap = s * 0.022f
        // Yazı alanı üst ve alt desen şeritlerinin arasındadır (en çok 0,48 s; iOS'taki gibi): uzun sözde yazı küçülür
        var msgSize = s * 0.036f
        var blocks: List<StaticLayout>
        var total: Float
        while (true) {
            blocks = buildList {
                AR[occasion]?.let { if (Strings.lang != "ar") add(layout(it, s * 0.055f, gold, false)) }
                add(layout(L("greet.$occasion"), s * 0.066f, ink, true))
                add(layout(message, msgSize, ink and 0x00FFFFFF or 0xE6000000.toInt(), false))
                // Dini kartta hicri tarih, nötr kartta miladi tarih
                val dateText = if (occasion in NEUTRAL) Fmt.longDate(today) else Hijri.date(today)?.text ?: Fmt.longDate(today)
                add(layout(dateText, s * 0.028f, ink and 0x00FFFFFF or 0xB3000000.toInt(), false))
            }
            total = blocks.sumOf { it.height } + gap * (blocks.size - 1)
            if (total <= s * 0.48f || msgSize < s * 0.024f) break
            msgSize *= 0.92f
        }
        var y = (s * 0.495f - total / 2).coerceAtLeast(s * 0.24f)
        for (l in blocks) { c.save(); c.translate(s * 0.14f, y); l.draw(c); c.restore(); y += l.height + gap }
        val sign = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = s * 0.027f; color = ink and 0x00FFFFFF or 0xA6000000.toInt(); textAlign = Paint.Align.CENTER }
        c.drawText("Saat Tarih · time.bumba.tr", s / 2, s * 0.965f, sign)
        return b
    }
}

/** Kart desenleri: üst şerit (0–0,27) ve alt şerit (0,74–0,93) yazı alanının dışında; geometrik desen bütün kartta soluk */
private object GreetMotifs {
    private fun paint(color: Int, alpha: Float = 1f, stroke: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        if (stroke > 0f) { style = Paint.Style.STROKE; strokeWidth = stroke }
    }

    private fun glow(c: Canvas, x: Float, y: Float, r: Float, color: Int, alpha: Float) {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        c.drawCircle(x, y, r, Paint().apply { shader = RadialGradient(x, y, r, (color and 0x00FFFFFF) or (a shl 24), 0, Shader.TileMode.CLAMP) })
    }

    fun draw(p: String, c: Canvas, s: Float, gold: Int, ink: Int, top: Int, bottom: Int) {
        when (p) {
            "arch" -> arch(c, s, gold)
            "stars" -> stars(c, s, gold, null)
            "crescent" -> stars(c, s, gold, top to bottom)
            "lanterns" -> lanterns(c, s, gold)
            "mosque" -> mosque(c, s, gold)
            "tulips" -> tulips(c, s, gold)
            "lights" -> lights(c, s, gold, ink)
            "kaaba" -> kaaba(c, s, gold)
            "lamp" -> lamp(c, s, gold)
            else -> geo(c, s, gold)
        }
    }

    private fun geo(c: Canvas, s: Float, gold: Int) {
        val pt = paint(gold, 0.28f, s / 360f)
        val step = s / 6
        for (i in 0..6) for (j in 0..6) {
            val path = Path()
            for (k in 0 until 16) {
                val a = Math.PI / 8 * k
                val r = if (k % 2 == 0) step * 0.34f else step * 0.21f
                val px = i * step + (r * cos(a)).toFloat(); val py = j * step + (r * sin(a)).toFloat()
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close(); c.drawPath(path, pt)
        }
    }

    private fun arch(c: Canvas, s: Float, gold: Int) {
        val p = Path().apply {
            moveTo(s * 0.12f, s * 0.92f); lineTo(s * 0.12f, s * 0.4f)
            quadTo(s * 0.12f, s * 0.1f, s * 0.5f, s * 0.06f); quadTo(s * 0.88f, s * 0.1f, s * 0.88f, s * 0.4f); lineTo(s * 0.88f, s * 0.92f)
        }
        c.drawPath(p, paint(gold, 1f, s / 120f))
        c.drawCircle(s * 0.5f, s * 0.17f, s * 0.025f, paint(gold))
    }

    /** Yıldızlar (sabit tohumlu; yazı alanına düşmez); bg verilirse hilal (oyuğu zeminin o yükseklikteki rengiyle) */
    private fun stars(c: Canvas, s: Float, gold: Int, bg: Pair<Int, Int>?) {
        val r = java.util.Random(7)
        var placed = 0
        val pt = paint(gold)
        while (placed < 60) {
            val x = r.nextFloat() * s; val y = r.nextFloat() * s * 0.92f
            val a = r.nextInt(150); val rad = (1f + r.nextFloat() * 2.2f) * s / 360f
            if (y > s * 0.27f && y < s * 0.8f && x > s * 0.08f && x < s * 0.92f) continue
            pt.alpha = 90 + a; c.drawCircle(x, y, rad, pt); placed++
        }
        if (bg != null) {
            val cx = s * 0.76f; val cy = s * 0.15f; val rr = s * 0.08f
            c.drawCircle(cx, cy, rr, paint(gold))
            val bgAt = androidx.core.graphics.ColorUtils.blendARGB(bg.first, bg.second, (cy - rr * 0.18f) / s)
            c.drawCircle(cx + rr * 0.45f, cy - rr * 0.18f, rr * 0.88f, paint(bgAt))
        }
    }

    private fun lanterns(c: Canvas, s: Float, gold: Int) {
        for ((x, len, k) in listOf(Triple(0.1f, 0.14f, 1f), Triple(0.26f, 0.07f, 0.8f), Triple(0.74f, 0.09f, 0.85f), Triple(0.9f, 0.15f, 1f))) {
            val cx = s * x; val top = s * len; val w = s * 0.07f * k; val h = s * 0.11f * k
            c.drawLine(cx, 0f, cx, top, paint(gold, 0.7f, s / 400f))
            glow(c, cx, top + h * 0.55f, w * 1.4f, gold, 0.35f)
            c.drawPath(Path().apply { moveTo(cx, top); lineTo(cx + w * 0.45f, top + h * 0.18f); lineTo(cx - w * 0.45f, top + h * 0.18f); close() }, paint(gold))
            c.drawPath(Path().apply {
                moveTo(cx - w * 0.45f, top + h * 0.18f); lineTo(cx + w * 0.45f, top + h * 0.18f); lineTo(cx + w * 0.5f, top + h * 0.75f)
                lineTo(cx, top + h); lineTo(cx - w * 0.5f, top + h * 0.75f); close()
            }, paint(gold, 0.85f))
            val ln = paint(0xFF000000.toInt(), 0.25f, s / 500f)
            for (f in listOf(-0.2f, 0f, 0.2f)) c.drawLine(cx + w * f, top + h * 0.22f, cx + w * f, top + h * 0.72f, ln)
        }
    }

    private fun mosque(c: Canvas, s: Float, gold: Int) {
        val base = s * 0.93f
        val p = Path()
        p.addRect(s * 0.3f, s * 0.84f, s * 0.7f, base, Path.Direction.CW)
        p.addOval(s * 0.37f, s * 0.75f, s * 0.63f, s * 0.94f, Path.Direction.CW)
        p.addRect(s * 0.497f, s * 0.71f, s * 0.503f, s * 0.76f, Path.Direction.CW)
        for (x in listOf(0.19f, 0.81f)) {
            p.addRect(s * x - s * 0.017f, s * 0.72f, s * x + s * 0.017f, base, Path.Direction.CW)
            p.addPath(Path().apply { moveTo(s * x - s * 0.021f, s * 0.72f); lineTo(s * x, s * 0.65f); lineTo(s * x + s * 0.021f, s * 0.72f); close() })
            p.addRect(s * x - s * 0.025f, s * 0.78f, s * x + s * 0.025f, s * 0.791f, Path.Direction.CW)
        }
        for (x in listOf(0.31f, 0.69f)) p.addOval(s * x - s * 0.05f, s * 0.82f, s * x + s * 0.05f, s * 0.88f, Path.Direction.CW)
        p.fillType = Path.FillType.WINDING
        c.drawPath(p, paint(gold, 0.3f))
    }

    private fun tulips(c: Canvas, s: Float, gold: Int) {
        listOf(0.08f, 0.2f, 0.32f, 0.68f, 0.8f, 0.92f).forEachIndexed { i, x ->
            val cx = s * x; val fy = s * (0.78f + (i % 2) * 0.03f); val bottom = s * 0.93f
            c.drawPath(Path().apply { moveTo(cx, bottom); quadTo(cx + s * 0.015f, (bottom + fy) / 2, cx, fy + s * 0.05f) }, paint(gold, 0.6f, s / 220f))
            c.drawPath(Path().apply {
                moveTo(cx, bottom - s * 0.02f); quadTo(cx - s * 0.05f, bottom - s * 0.02f, cx - s * 0.045f, bottom - s * 0.09f)
                quadTo(cx - s * 0.005f, bottom - s * 0.08f, cx, bottom - s * 0.02f); close()
            }, paint(gold, 0.45f))
            val w = s * 0.035f; val h = s * 0.055f
            c.drawPath(Path().apply {
                moveTo(cx - w, fy); lineTo(cx - w * 0.5f, fy + h * 0.35f); lineTo(cx, fy); lineTo(cx + w * 0.5f, fy + h * 0.35f); lineTo(cx + w, fy)
                quadTo(cx + w * 1.05f, fy + h, cx, fy + h); quadTo(cx - w * 1.05f, fy + h, cx - w, fy); close()
            }, paint(gold, 0.85f))
        }
    }

    private fun lights(c: Canvas, s: Float, gold: Int, ink: Int) {
        for (row in 0..1) {
            val y0 = s * (0.04f + row * 0.07f); val dip = s * 0.06f
            val ax = -s * 0.02f; val bx = s * 1.02f; val cx = s * 0.5f; val cy = y0 + 2 * dip
            c.drawPath(Path().apply { moveTo(ax, y0); quadTo(cx, cy, bx, y0) }, paint(gold, 0.5f, s / 500f))
            for (i in 0..11) {
                val t = i / 11f
                val x = (1 - t) * (1 - t) * ax + 2 * (1 - t) * t * cx + t * t * bx
                val y = (1 - t) * (1 - t) * y0 + 2 * (1 - t) * t * cy + t * t * y0
                val r = s * 0.012f; val col = if ((i + row) % 3 == 0) ink else gold
                glow(c, x, y + r, r * 2.4f, col, 0.35f)
                c.drawOval(x - r * 0.7f, y, x + r * 0.7f, y + r * 1.9f, paint(col, 0.9f))
            }
        }
    }

    private fun kaaba(c: Canvas, s: Float, gold: Int) {
        glow(c, s * 0.5f, s * 0.84f, s * 0.3f, gold, 0.25f)
        val w = s * 0.17f; val h = s * 0.14f; val x = s * 0.5f - w / 2 - s * 0.02f; val y = s * 0.92f - h; val d = s * 0.04f
        c.drawPath(Path().apply { moveTo(x + w, y); lineTo(x + w + d, y - d * 0.5f); lineTo(x + w + d, y + h - d * 0.5f); lineTo(x + w, y + h); close() }, paint(0xFF0D0D0D.toInt()))
        c.drawPath(Path().apply { moveTo(x, y); lineTo(x + d, y - d * 0.5f); lineTo(x + w + d, y - d * 0.5f); lineTo(x + w, y); close() }, paint(0xFF2B2B2B.toInt()))
        c.drawRect(x, y, x + w, y + h, paint(0xFF1A1A1A.toInt()))
        val by = y + h * 0.22f; val bh = h * 0.09f
        c.drawRect(x, by, x + w, by + bh, paint(gold))
        c.drawPath(Path().apply { moveTo(x + w, by); lineTo(x + w + d, by - d * 0.5f); lineTo(x + w + d, by - d * 0.5f + bh); lineTo(x + w, by + bh); close() }, paint(gold, 0.8f))
        c.drawRect(x + w * 0.62f, y + h * 0.45f, x + w * 0.78f, y + h * 0.85f, paint(gold, 0.9f))
    }

    private fun lamp(c: Canvas, s: Float, gold: Int) {
        val cx = s * 0.5f; val by = s * 0.9f
        val fx = cx + s * 0.075f; val fy = by - s * 0.1f
        glow(c, fx, fy, s * 0.16f, gold, 0.45f)
        c.drawPath(Path().apply {
            moveTo(cx - s * 0.1f, by - s * 0.04f)
            quadTo(cx - s * 0.02f, by + s * 0.03f, cx + s * 0.06f, by - s * 0.045f)
            lineTo(cx + s * 0.08f, by - s * 0.068f); lineTo(cx + s * 0.05f, by - s * 0.062f)
            quadTo(cx - s * 0.03f, by - s * 0.09f, cx - s * 0.1f, by - s * 0.04f); close()
        }, paint(gold))
        c.drawArc(cx - s * 0.125f, by - s * 0.075f, cx - s * 0.075f, by - s * 0.025f, 90f, 180f, false, paint(gold, 1f, s / 150f))
        c.drawRoundRect(cx - s * 0.05f, by - s * 0.005f, cx + s * 0.03f, by + s * 0.013f, s * 0.006f, s * 0.006f, paint(gold, 0.85f))
        c.drawPath(Path().apply {
            moveTo(fx, fy - s * 0.035f); quadTo(fx + s * 0.03f, fy + s * 0.01f, fx, fy + s * 0.03f); quadTo(fx - s * 0.03f, fy + s * 0.01f, fx, fy - s * 0.035f); close()
        }, paint(0xFFFFD173.toInt()))
        stars(c, s, (gold and 0x00FFFFFF) or 0xB3000000.toInt(), null)
    }
}

@Composable
fun GreetingScreen(neutral: Boolean = false, onBack: () -> Unit) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    // Kutlama kartları (nötr) yalnız nötr vesileleri, İbadet'teki tebrik kartları önce dini sonra nötr vesileleri gösterir
    val occasions = if (neutral) Greetings.NEUTRAL else Greetings.OCCASIONS
    var occasion by remember { mutableStateOf(if (neutral) Greetings.suggestNeutral(today) else Greetings.suggest(today)) }
    var style by remember { mutableStateOf("night") }
    var pattern by remember { mutableStateOf("") }
    // Her girişte yeni söz (4 Kasım 2026, sahibin isteği); "Başka söz" yenisini seçer
    var seed by remember { mutableStateOf(kotlin.random.Random.nextLong()) }
    var own by remember { mutableStateOf("") }
    val pat = pattern.ifEmpty { Greetings.PATTERNS[occasion]?.first() ?: "geo" }
    val message = own.trim().ifEmpty { Greetings.message(occasion, seed) }
    val bmp = remember(occasion, style, pat, message) { Greetings.render(occasion, style, pat, message, today, 720) }
    Column(Modifier.fillMaxSize()) {
        SubBar(L(if (neutral) "greet.titleN" else "greet.title"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(bmp.asImageBitmap(), L("greet.$occasion"), Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (o in occasions) Chip(L("greet.$o"), o == occasion) { occasion = o; pattern = "" }
            }
            Chip("↻ " + L("greet.shuffle"), false) { seed = kotlin.random.Random.nextLong(); own = "" }
            androidx.compose.material3.OutlinedTextField(own, { own = it.take(300) }, Modifier.fillMaxWidth(),
                placeholder = { Text(L("greet.own"), fontSize = 14.sp) }, maxLines = 4)
            Text(L("greet.style"), color = Theme.c.muted, fontSize = 13.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (p in Greetings.PATTERNS[occasion] ?: listOf("geo")) Chip(L("greet.pat.$p"), p == pat) { pattern = p }
            }
            Text(L("greet.palette"), color = Theme.c.muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val names = mapOf("night" to L("quran.theme.night"), "emerald" to L("quran.theme.green"), "gold" to L("quran.theme.paper"))
                for (st in Greetings.STYLES) Chip(names.getValue(st), st == style) { style = st }
            }
            RoundButton(L("share.card"), true, Modifier.fillMaxWidth()) {
                Share.image(context, Greetings.render(occasion, style, pat, message, today), "tebrik-$occasion.png", L(if (neutral) "greet.titleN" else "greet.title"),
                    T("greet.shareText", L("greet.$occasion"), Greetings.STORE))
            }
            Text(L("greet.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Chip(title: String, on: Boolean, onClick: () -> Unit) {
    Text(title, Modifier.background(if (on) Theme.c.accent else Theme.c.surface2, RoundedCornerShape(50)).clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp), color = if (on) Color.White else Theme.c.text, fontSize = 14.sp)
}
