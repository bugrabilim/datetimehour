package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Dijital rakam görünümleri (2. analiz #9; 4 Kasım 2026): yedi bölütlü LED (sönük bölütler de görünür), LCD taklidi
 * (yeşilimsi zemin, koyu bölüt), nokta matris (5×7), silindir (önceki ve sonraki rakam soluk, üstte ve altta) ve dikey
 * (saat, dakika, saniye alt alta). Kayıt `digital.style`; iOS'taki DigitalStyle ile aynı kimlikler ve bölüt tablosu.
 */
object DigitalStyle {
    const val KEY = "digital.style"
    val ALL = listOf("plain", "led", "lcd", "matrix", "roll", "vertical")
    val current: String get() = Prefs.store.getString(KEY, "plain")?.takeIf { it in ALL } ?: "plain"
    fun save(v: String) = Prefs.store.edit().putString(KEY, v).apply()

    /** Yedi bölüt: a üst, b sağ üst, c sağ alt, d alt, e sol alt, f sol üst, g orta (bit 0..6) */
    val SEG = intArrayOf(0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F)

    /** 5×7 nokta matris rakamları (satır başına 5 bit, üstten alta) */
    val DOTS = arrayOf(
        intArrayOf(0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E), intArrayOf(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E),
        intArrayOf(0x0E, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1F), intArrayOf(0x1F, 0x02, 0x04, 0x02, 0x01, 0x11, 0x0E),
        intArrayOf(0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02), intArrayOf(0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E),
        intArrayOf(0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E), intArrayOf(0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08),
        intArrayOf(0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E), intArrayOf(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C),
    )

    /** Karakter genişliği (yüksekliğe oran) */
    fun widthOf(ch: Char, style: String): Float = when {
        ch == ':' -> 0.28f
        ch == ' ' -> 0.25f
        style == "matrix" -> 0.62f
        else -> 0.56f
    }
}

/** Biçimli rakamlar ("12:34:56"); yükseklik verilir, genişlik sığmazsa küçülür */
@Composable
fun StyledDigits(text: String, style: String, height: Dp, maxWidth: Dp, ink: Color, roll: Triple<String, String, Float>? = null) {
    val d = LocalDensity.current
    val natural = text.sumOf { DigitalStyle.widthOf(it, style).toDouble() }.toFloat() * height.value + (text.length - 1) * height.value * 0.06f
    val scale = min(1f, maxWidth.value / natural.coerceAtLeast(1f))
    val h = height * scale
    val w = (natural * scale).dp
    Canvas(Modifier.width(w).height(h)) {
        val hp = size.height
        if (style == "lcd") {
            drawRoundRect(Color(0xFFB9C7A2), size = size, cornerRadius = CornerRadius(hp * 0.08f))
        }
        var x = 0f
        val gap = hp * 0.06f
        val lit = if (style == "lcd") Color(0xFF1E2A18) else ink
        val ghost = lit.copy(alpha = if (style == "lcd") 0.10f else 0.09f)
        text.forEachIndexed { i, ch ->
            val cw = DigitalStyle.widthOf(ch, style) * hp
            when {
                ch == ':' -> {
                    val r = hp * 0.045f
                    drawCircle(lit, r, Offset(x + cw / 2, hp * 0.33f))
                    drawCircle(lit, r, Offset(x + cw / 2, hp * 0.67f))
                }
                ch.isDigit() -> {
                    val n = ch - '0'
                    when (style) {
                        "matrix" -> drawMatrix(n, x, cw, hp, lit, ghost)
                        "roll" -> drawRoll(ch, x, cw, hp, lit, d.density)
                        else -> drawSegments(n, x, cw, hp, lit, ghost)
                    }
                }
            }
            x += cw + gap
        }
    }
}

private fun DrawScope.drawSegments(n: Int, x0: Float, w: Float, h: Float, on: Color, off: Color) {
    val bits = DigitalStyle.SEG[n]
    val t = h * 0.1f // bölüt kalınlığı
    val inset = h * 0.04f
    val x = x0 + inset
    val ww = w - 2 * inset
    val hh = h - 2 * inset
    val y = inset
    // Altıgen bölüt: yatay ve dikey
    fun hSeg(cx: Float, cy: Float, len: Float) = Path().apply {
        moveTo(cx, cy); lineTo(cx + t / 2, cy - t / 2); lineTo(cx + len - t / 2, cy - t / 2); lineTo(cx + len, cy)
        lineTo(cx + len - t / 2, cy + t / 2); lineTo(cx + t / 2, cy + t / 2); close()
    }
    fun vSeg(cx: Float, cy: Float, len: Float) = Path().apply {
        moveTo(cx, cy); lineTo(cx + t / 2, cy + t / 2); lineTo(cx + t / 2, cy + len - t / 2); lineTo(cx, cy + len)
        lineTo(cx - t / 2, cy + len - t / 2); lineTo(cx - t / 2, cy + t / 2); close()
    }
    val half = hh / 2
    val segs = listOf(
        hSeg(x + t * 0.6f, y + t / 2, ww - t * 1.2f),               // a
        vSeg(x + ww - t / 2, y + t * 0.6f, half - t * 0.6f),        // b
        vSeg(x + ww - t / 2, y + half + t * 0.1f, half - t * 0.6f), // c
        hSeg(x + t * 0.6f, y + hh - t / 2, ww - t * 1.2f),          // d
        vSeg(x + t / 2, y + half + t * 0.1f, half - t * 0.6f),      // e
        vSeg(x + t / 2, y + t * 0.6f, half - t * 0.6f),             // f
        hSeg(x + t * 0.6f, y + half, ww - t * 1.2f),                // g
    )
    segs.forEachIndexed { i, p -> drawPath(p, if (bits and (1 shl i) != 0) on else off) }
}

private fun DrawScope.drawMatrix(n: Int, x0: Float, w: Float, h: Float, on: Color, off: Color) {
    val rows = DigitalStyle.DOTS[n]
    val step = min(w / 5f, h / 7f)
    val r = step * 0.38f
    val ox = x0 + (w - step * 5) / 2
    val oy = (h - step * 7) / 2
    for (row in 0 until 7) for (col in 0 until 5) {
        val litDot = rows[row] and (1 shl (4 - col)) != 0
        drawCircle(if (litDot) on else off, r, Offset(ox + step * (col + 0.5f), oy + step * (row + 0.5f)))
    }
}

private fun DrawScope.drawRoll(ch: Char, x0: Float, w: Float, h: Float, on: Color, density: Float) {
    val n = ch - '0'
    val prev = ((n + 9) % 10).toString()
    val next = ((n + 1) % 10).toString()
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textSize = h * 0.62f
    }
    val cx = x0 + w / 2
    // Silindir zemini: ortası açık, kenarları koyulaşan şerit
    drawRoundRect(on.copy(alpha = 0.06f), Offset(x0, 0f), Size(w, h), CornerRadius(w * 0.18f))
    fun text(s: String, y: Float, alpha: Float, scaleY: Float) {
        p.color = android.graphics.Color.argb((alpha * 255).toInt(), (on.red * 255).toInt(), (on.green * 255).toInt(), (on.blue * 255).toInt())
        val c = drawContext.canvas.nativeCanvas
        c.save()
        c.scale(1f, scaleY, cx, y)
        c.drawText(s, cx, y + p.textSize * 0.36f, p)
        c.restore()
    }
    text(prev, h * 0.08f, 0.22f, 0.45f)
    text(ch.toString(), h * 0.5f, 1f, 1f)
    text(next, h * 0.92f, 0.22f, 0.45f)
}

/** Dikey görünüm: saat, dakika (ve saniye) alt alta */
@Composable
fun VerticalDigits(hh: String, mm: String, ss: String?, height: Dp, ink: Color, fontFamily: androidx.compose.ui.text.font.FontFamily?) {
    val lines = listOfNotNull(hh, mm, ss)
    val each = height / lines.size
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        for ((i, l) in lines.withIndex()) {
            androidx.compose.material3.Text(
                l, color = if (i == 2) ink.copy(alpha = 0.6f) else ink, fontFamily = fontFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, style = Tnum,
                fontSize = with(LocalDensity.current) { (each * 0.86f).toSp() }, lineHeight = with(LocalDensity.current) { (each * 0.9f).toSp() },
            )
        }
    }
}
