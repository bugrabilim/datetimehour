package tr.bumba.time

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Üretken tam ekran zeminleri (2. analiz #23; 4 Kasım 2026): eş yükselti çizgileri, noktalı dünya haritası (karalar
 * Natural Earth dokusundan, gece yarısı sönük; saate göre), grenli renk geçişi ve sıvı metal. Hepsi telefonda çizilir,
 * dosya ya da internet gerekmez. iOS'taki AnimatedBackground'daki topo/dots/grain/liquid ile aynı kurallar.
 */
object GenArt {
    const val COLS = 120
    const val ROWS = 60
    @Volatile private var mask: BooleanArray? = null

    /** Kara maskesi (120×60): dokuda mavi baskın değilse kara (buzullar da kara sayılır) */
    fun landMask(c: Context): BooleanArray? {
        mask?.let { return it }
        val bmp = WorldMapTexture.bitmap(c) ?: return null
        val out = BooleanArray(COLS * ROWS)
        for (r in 0 until ROWS) for (q in 0 until COLS) {
            val px = bmp.getPixel(((q + 0.5f) / COLS * bmp.width).toInt().coerceIn(0, bmp.width - 1), ((r + 0.5f) / ROWS * bmp.height).toInt().coerceIn(0, bmp.height - 1))
            val red = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            out[r * COLS + q] = !(b > red + 18 && b >= g)
        }
        mask = out
        return out
    }
}

/** Eş yükselti çizgileri: üç tepe çevresinde dalgalı halkalar, çok yavaş kayar */
fun DrawScope.topo(t: Double, accent: Color) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0E1412), Color(0xFF16201C))))
    val centers = listOf(0.3f to 0.3f, 0.75f to 0.55f, 0.4f to 0.85f)
    val big = max(size.width, size.height)
    centers.forEachIndexed { ci, (cx, cy) ->
        for (k in 1..14) {
            val r0 = big * 0.035f * k
            val path = Path()
            val steps = 72
            for (i in 0..steps) {
                val a = 2 * PI * i / steps
                val wob = sin(a * 3 + k * 0.5 + ci + t * 0.05) * 0.12 + sin(a * 5 - k * 0.3 + t * 0.03) * 0.06
                val rr = r0 * (1 + wob).toFloat()
                val x = cx * size.width + rr * cos(a).toFloat()
                val y = cy * size.height + rr * sin(a).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, accent.copy(alpha = if (k % 5 == 0) 0.35f else 0.16f), style = Stroke(width = (if (k % 5 == 0) 1.6f else 1f) * density))
        }
    }
}

/** Noktalı dünya haritası: karalar nokta; Güneş'in aydınlattığı yarı parlak, gece sönük */
fun DrawScope.dotMap(t: Double, mask: BooleanArray?, accent: Color) {
    drawRect(Color(0xFF070B14))
    val m = mask ?: return
    val w = size.width
    val h = min(size.height, w / 2)
    val top = (size.height - h) / 2
    val step = w / GenArt.COLS
    val r = step * 0.32f
    val now = System.currentTimeMillis() + (t * 0).toLong()
    for (row in 0 until GenArt.ROWS) for (col in 0 until GenArt.COLS) {
        if (!m[row * GenArt.COLS + col]) continue
        val lat = 90.0 - (row + 0.5) * 180.0 / GenArt.ROWS
        val lon = -180.0 + (col + 0.5) * 360.0 / GenArt.COLS
        val alt = SkyMath.sunAltitude(lat, lon, now)
        val day = ((alt + 6) / 12).coerceIn(0.0, 1.0).toFloat()
        drawCircle(lerpColor(Color(0xFF26324D), accent, day), r, Offset((col + 0.5f) * step, top + (row + 0.5f) * (h / GenArt.ROWS)))
    }
}

private fun lerpColor(a: Color, b: Color, f: Float) = Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, 1f)

/** Grenli renk geçişi: uygulama renklerinden koyu çapraz geçiş, üstte sabit tohumlu ince gren */
fun DrawScope.grain(accent: Color, accent2: Color) {
    drawRect(Brush.linearGradient(listOf(accent.copy(alpha = 1f).let { lerpColor(Color(0xFF0B0B10), it, 0.55f) },
        lerpColor(Color(0xFF0B0B10), accent2, 0.35f), Color(0xFF0B0B10)), Offset.Zero, Offset(size.width, size.height)))
    var seed = 0x6A1EL
    fun rnd(): Float {
        seed = seed * 6364136223846793005L + 1442695040888963407L
        return ((seed ushr 11).toDouble() / (1L shl 53).toDouble()).toFloat()
    }
    val n = (size.width * size.height / (6f * density * density)).toInt().coerceAtMost(30000)
    val d = density * 0.9f
    repeat(n) {
        val light = rnd() > 0.5f
        drawRect(if (light) Color.White.copy(alpha = 0.05f + rnd() * 0.05f) else Color.Black.copy(alpha = 0.08f + rnd() * 0.08f),
            Offset(rnd() * size.width, rnd() * size.height), androidx.compose.ui.geometry.Size(d, d))
    }
}

/** Sıvı metal: koyu gri zeminde yavaş akan gümüş ve parlak lekeler */
fun DrawScope.liquid(t: Double) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1C20), Color(0xFF2A2D33))))
    val big = max(size.width, size.height)
    val tones = listOf(Color(0xFFE8EBF0), Color(0xFF9AA3B0), Color(0xFFC8CDD6), Color(0xFF6E7684), Color(0xFFF5F7FA))
    tones.forEachIndexed { k, c ->
        val p = Offset(
            (size.width * (0.5 + 0.38 * sin(t * 0.06 + k * 1.3))).toFloat(),
            (size.height * (0.5 + 0.38 * cos(t * 0.045 + k * 2.1))).toFloat(),
        )
        val rr = big * (0.22f + 0.06f * k)
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.55f), c.copy(alpha = 0.18f), c.copy(alpha = 0f)), p, rr), rr, p)
    }
    // Parlama şeridi
    drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
        Offset(0f, (size.height * (0.3 + 0.2 * sin(t * 0.08))).toFloat()), Offset(size.width, (size.height * (0.6 + 0.2 * cos(t * 0.07))).toFloat())))
}
