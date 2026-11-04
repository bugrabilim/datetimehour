package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* Araç görselleri (2. analiz #101, #140; 3 Kasım 2026): kalan süre halkası, pomodoro tur domatesleri, evre renkleri,
   rakamsız pasta zamanlayıcı ve kum saati, kilit (çocuk modu). iOS'taki ToolVisuals.swift ile aynı çizim. */

object PhaseColors {
    /** Aralık antrenmanında evre rengi (doğal tasarım 4 Kasım 2026, yumuşak doğa tonları): çalış yaprak yeşili, dinlen
     *  kiremit, ısın hardal, bitti gök mavisi; üstündeki beyaz yazı ≥4,5:1 */
    fun of(kind: String): Color = when (kind) {
        "work" -> Color(0xFF3F7A4F)
        "rest" -> Color(0xFFB4532A)
        "warmup" -> Color(0xFF9A6A12)
        else -> Color(0xFF3A6889)
    }
}

/**
 * Kalan süre halkası (doğal tasarım madde 11): ince iz (`line`) + 6 dp ilerleme yayı (saat 12'den saat yönünde), yayın
 * ucunda küçük ışıklı nokta; içerik ortada
 */
@Composable
fun ProgressRing(progress: Float, color: Color, modifier: Modifier = Modifier, stroke: Dp = 6.dp, track: Color = Theme.c.line,
                 content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = stroke.toPx()
            val d = min(size.width, size.height) - w * 2
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            val p = progress.coerceIn(0f, 1f)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w * 0.5f))
            drawArc(color, -90f, 360f * p, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
            if (p > 0f) {
                val a = Math.toRadians(-90.0 + 360.0 * p)
                val end = Offset(size.width / 2 + (d / 2 * cos(a)).toFloat(), size.height / 2 + (d / 2 * sin(a)).toFloat())
                drawCircle(color.copy(alpha = 0.25f), w * 1.3f, end)
                drawCircle(color, w * 0.75f, end)
            }
        }
        content()
    }
}

/** Pomodoro turları: çizgi domatesler (doğal tasarım madde 11); biten dolu renk, sürmekte olan yarı saydam, kalan soluk */
@Composable
fun RoundDots(total: Int, done: Int, current: Int, label: String) {
    Row(Modifier.semantics { contentDescription = label }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..total) {
            val a = when { i <= done -> 1f; i == current -> 0.6f; else -> 0.25f }
            val col = (if (i <= done || i == current) Theme.c.terra else Theme.c.muted).copy(alpha = a)
            LineIcon("tomato", if (i == current) 26.dp else 22.dp, c1 = col, c2 = (if (i <= done || i == current) Theme.c.sage else Theme.c.muted).copy(alpha = a))
        }
    }
}

/** Rakamsız pasta zamanlayıcı ("Time Timer" tarzı): kalan süre kırmızı dilim, 12 çentikli kadran */
@Composable
fun PieTimer(left: Float, color: Color, modifier: Modifier = Modifier) {
    val face = Theme.c.surface
    val ink = Theme.c.text
    Canvas(modifier.aspectRatio(1f)) {
        val r = min(size.width, size.height) / 2
        val c = Offset(size.width / 2, size.height / 2)
        // Temaya uyan kadran (doğal tasarım madde 11): kâğıt yüz, mürekkep çentikler
        drawCircle(face, r, c)
        drawCircle(ink.copy(alpha = 0.5f), r, c, style = Stroke(r * 0.02f))
        val inner = r * 0.86f
        drawArc(color, -90f, 360f * left.coerceIn(0f, 1f), true, Offset(c.x - inner, c.y - inner), Size(inner * 2, inner * 2))
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90)
            val l = if (i % 5 == 0) r * 0.12f else r * 0.05f
            val o = r * 0.97f
            drawLine(ink, Offset(c.x + (cos(a) * o).toFloat(), c.y + (sin(a) * o).toFloat()),
                Offset(c.x + (cos(a) * (o - l)).toFloat(), c.y + (sin(a) * (o - l)).toFloat()), strokeWidth = if (i % 5 == 0) r * 0.025f else r * 0.01f)
        }
        drawCircle(ink, r * 0.06f, c)
    }
}

/** Kum saati: üst hazne kalan süreyle boşalır, alt hazne dolar, ortadan ince akış */
@Composable
fun Hourglass(left: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(0.62f)) {
        val w = size.width; val h = size.height
        val frame = Color(0xFF6B4F2A)
        val glass = Color(0x33808080)
        val mid = h / 2; val neck = w * 0.06f; val top = h * 0.06f; val bot = h * 0.94f
        val upper = Path().apply { moveTo(w * 0.1f, top); lineTo(w * 0.9f, top); lineTo(w / 2 + neck, mid); lineTo(w / 2 - neck, mid); close() }
        val lower = Path().apply { moveTo(w / 2 - neck, mid); lineTo(w / 2 + neck, mid); lineTo(w * 0.9f, bot); lineTo(w * 0.1f, bot); close() }
        drawPath(upper, glass); drawPath(lower, glass)
        val f = left.coerceIn(0f, 1f)
        // Üst hazne: kum düzeyi aşağıdan (boğaz) yukarı f oranında (üçgen alanı yüksekliğin karesiyle ölçeklenir)
        if (f > 0f) {
            val hh = (mid - top) * kotlin.math.sqrt(f)
            val y = mid - hh
            val half = neck + (w * 0.4f - neck) * (hh / (mid - top))
            drawPath(Path().apply { moveTo(w / 2 - half, y); lineTo(w / 2 + half, y); lineTo(w / 2 + neck, mid); lineTo(w / 2 - neck, mid); close() }, color)
            drawLine(color, Offset(w / 2, mid), Offset(w / 2, bot), strokeWidth = w * 0.015f)
        }
        // Alt hazne: dipten yığılan kum (1 − f)
        val g = 1f - f
        if (g > 0f) {
            val hh = (bot - mid) * (1f - kotlin.math.sqrt(1f - g))
            val y = bot - hh
            val half = w * 0.4f - (w * 0.4f - neck) * ((bot - y) / (bot - mid))
            drawPath(Path().apply { moveTo(w * 0.1f, bot); lineTo(w * 0.9f, bot); lineTo(w / 2 + half, y); lineTo(w / 2 - half, y); close() }, color)
        }
        drawRect(frame, Offset(0f, 0f), Size(w, top))
        drawRect(frame, Offset(0f, bot), Size(w, h - bot))
    }
}

/** Kilit (aralık antrenmanı ve çocuk modu): kilitliyken yalnız bu çip görünür, basılı tutunca açılır */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HoldToUnlock(onUnlock: () -> Unit, dark: Boolean = false) {
    Box(Modifier.background(if (dark) Color(0x33FFFFFF) else Theme.c.surface2, RoundedCornerShape(50))
        .combinedClickable(onClick = {}, onLongClick = onUnlock).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text(L("tools.holdToUnlock"), color = if (dark) Color.White else Theme.c.text, fontSize = 14.sp)
    }
}

/** Görsel zamanlayıcı: pasta ya da kum saati (rakamsız) */
@Composable
fun VisualTimerContent(leftMs: Long, totalMs: Long, style: String, modifier: Modifier = Modifier) {
    val f = if (totalMs > 0) leftMs.toFloat() / totalMs else 0f
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        if (style == "sand") Hourglass(f, Color(0xFFE0B04A), Modifier.fillMaxWidth(0.6f))
        else PieTimer(f, Color(0xFFD9443A), Modifier.fillMaxWidth())
    }
}

/** Evre panosu: evre rengi zemin, alttan evrenin ilerlemesiyle dolan açık ton, beyaz yazı */
@Composable
fun PhasePanel(kind: String, progress: Float, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = PhaseColors.of(kind)
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.darken(0.35f)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val hh = size.height * progress.coerceIn(0f, 1f)
            drawRect(c, Offset(0f, size.height - hh), Size(size.width, hh))
        }
        content()
    }
}

private fun Color.darken(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
