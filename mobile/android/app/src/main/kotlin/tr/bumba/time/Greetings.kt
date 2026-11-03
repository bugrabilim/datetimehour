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
 * Tebrik kartları (3 Kasım 2026, 2. analiz #74; iOS'taki Greetings.swift ile aynı): Cuma, Ramazan, Ramazan ve Kurban
 * bayramı, kandil ve hicri yılbaşı için telefonda çizilen kare görsel; üç desen (gece, zümrüt, altın). Dış görsel yok,
 * görsel yalnız kullanıcı paylaşınca sistemin paylaşım penceresiyle gider. iMessage çıkartma paketi iOS'ta ayrı bir
 * uzantı ve paket kimliği istediği için bu turda yok.
 */
object Greetings {
    val OCCASIONS = listOf("cuma", "ramadan", "rb", "kb", "kandil", "hijri")
    val STYLES = listOf("night", "emerald", "gold")
    private val AR = mapOf(
        "cuma" to "جمعة مباركة", "ramadan" to "رمضان كريم", "rb" to "عيد مبارك", "kb" to "عيد مبارك",
        "kandil" to "ليلة مباركة", "hijri" to "عام هجري سعيد",
    )

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

    fun render(occasion: String, style: String, today: LocalDate, size: Int = 1080): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val s = size.toFloat()
        val (top, bottom, ink, gold) = when (style) {
            "emerald" -> listOf(0xFF0B4D3B.toInt(), 0xFF06281F.toInt(), 0xFFFFFFFF.toInt(), 0xFFE9C46A.toInt())
            "gold" -> listOf(0xFFFBF3E0.toInt(), 0xFFEAD9B0.toInt(), 0xFF3B2A12.toInt(), 0xFFA8761E.toInt())
            else -> listOf(0xFF101A3F.toInt(), 0xFF2A1D52.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFD173.toInt())
        }
        c.drawRect(0f, 0f, s, s, Paint().apply { shader = LinearGradient(0f, 0f, 0f, s, top, bottom, Shader.TileMode.CLAMP) })
        val deco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold }
        when (style) {
            "night" -> {
                // Yıldızlar (sabit tohumlu) ve hilal
                // Yazı alanına (orta bant) yıldız düşmez
                val r = java.util.Random(7)
                var placed = 0
                while (placed < 60) {
                    val x = r.nextFloat() * s; val y = r.nextFloat() * s * 0.9f
                    val a = r.nextInt(150); val rad = 1.5f + r.nextFloat() * 3f
                    if (y > s * 0.31f && y < s * 0.8f && x > s * 0.1f && x < s * 0.9f) continue
                    deco.alpha = 90 + a; c.drawCircle(x, y, rad, deco); placed++
                }
                deco.alpha = 255
                val cx = s * 0.74f; val cy = s * 0.2f; val rr = s * 0.08f
                c.drawCircle(cx, cy, rr, deco)
                // Hilalin oyuğu zemin degradesinin o yükseklikteki rengiyle
                val bgAt = androidx.core.graphics.ColorUtils.blendARGB(top, bottom, (cy - rr * 0.18f) / s)
                c.drawCircle(cx + rr * 0.45f, cy - rr * 0.18f, rr * 0.88f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgAt })
            }
            "emerald" -> {
                // Sekiz köşeli yıldız deseni
                deco.style = Paint.Style.STROKE; deco.strokeWidth = s / 360f; deco.alpha = 70
                val step = s / 6
                for (i in 0..6) for (j in 0..6) star8(c, i * step, j * step, step * 0.34f, deco)
                deco.alpha = 255; deco.style = Paint.Style.FILL
            }
            else -> {
                // Kemer ve fener
                deco.style = Paint.Style.STROKE; deco.strokeWidth = s / 120f
                val p = Path().apply {
                    moveTo(s * 0.18f, s * 0.92f); lineTo(s * 0.18f, s * 0.42f)
                    quadTo(s * 0.18f, s * 0.12f, s * 0.5f, s * 0.08f); quadTo(s * 0.82f, s * 0.12f, s * 0.82f, s * 0.42f); lineTo(s * 0.82f, s * 0.92f)
                }
                c.drawPath(p, deco)
                deco.style = Paint.Style.FILL
                c.drawCircle(s * 0.5f, s * 0.19f, s * 0.025f, deco)
            }
        }
        // Yumuşak ışık
        c.drawCircle(s / 2, s * 0.52f, s * 0.45f, Paint().apply {
            shader = RadialGradient(s / 2, s * 0.52f, s * 0.45f, (gold and 0x00FFFFFF) or 0x22000000, 0, Shader.TileMode.CLAMP)
        })
        fun block(text: String, size: Float, color: Int, y: Float, bold: Boolean): Float {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT }
            val l = StaticLayout.Builder.obtain(text, 0, text.length, p, (s * 0.7f).toInt()).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            c.save(); c.translate(s * 0.15f, y); l.draw(c); c.restore()
            return y + l.height
        }
        var y = s * 0.36f
        if (Strings.lang != "ar") y = block(AR.getValue(occasion), s * 0.06f, gold, y, false) + s * 0.03f
        y = block(L("greet.$occasion"), s * 0.075f, ink, y, true) + s * 0.04f
        val sub = Hijri.date(today)?.text ?: Fmt.longDate(today)
        block(sub, s * 0.032f, ink and 0x00FFFFFF or 0xBB000000.toInt(), y, false)
        val sign = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = s * 0.028f; color = ink and 0x00FFFFFF or 0x99000000.toInt(); textAlign = Paint.Align.CENTER }
        c.drawText("Saat Tarih · time.bumba.tr", s / 2, s * 0.95f, sign)
        return b
    }

    private fun star8(c: Canvas, x: Float, y: Float, r: Float, p: Paint) {
        val path = Path()
        for (k in 0 until 16) {
            val a = Math.PI / 8 * k
            val rr = if (k % 2 == 0) r else r * 0.62f
            val px = x + (rr * cos(a)).toFloat(); val py = y + (rr * sin(a)).toFloat()
            if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        c.drawPath(path, p)
    }
}

@Composable
fun GreetingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var occasion by remember { mutableStateOf(Greetings.suggest(today)) }
    var style by remember { mutableStateOf("night") }
    val bmp = remember(occasion, style) { Greetings.render(occasion, style, today, 720) }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("greet.title"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(bmp.asImageBitmap(), L("greet.$occasion"), Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (o in Greetings.OCCASIONS) Chip(L("greet.$o"), o == occasion) { occasion = o }
            }
            Text(L("greet.style"), color = Theme.c.muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val names = mapOf("night" to L("quran.theme.night"), "emerald" to L("quran.theme.green"), "gold" to L("quran.theme.paper"))
                for (st in Greetings.STYLES) Chip(names.getValue(st), st == style) { style = st }
            }
            RoundButton(L("share.card"), true, Modifier.fillMaxWidth()) {
                Share.image(context, Greetings.render(occasion, style, today), "tebrik-$occasion.png", L("greet.title"))
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
