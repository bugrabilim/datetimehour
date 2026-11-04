package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.min

/**
 * Ölçülü süsler (doğal tasarım madde 15, 4 Kasım 2026; sahibin onayı): sekiz köşeli hatem yıldızı, mihrap kemeri ve ufukta
 * kubbe-minare silueti. Yalnız ibadet ekranlarında (vakit sayacı, Kur'an, dualar, kıble), %6–12 saydamlıkta; yazının
 * arkasında okumayı bozmayacak kadar soluk. iOS'taki Motifs.swift ile aynı biçimler.
 */
object Motifs {
    /** Hatem: iki kare (biri 45° dönük) ve ortada halka */
    fun DrawScope.hatem(center: Offset, r: Float, color: Color, stroke: Float) {
        val sq = Path().apply {
            val s = r * 0.7071f
            moveTo(center.x - s, center.y - s); lineTo(center.x + s, center.y - s)
            lineTo(center.x + s, center.y + s); lineTo(center.x - s, center.y + s); close()
        }
        drawPath(sq, color, style = Stroke(stroke))
        rotate(45f, center) { drawPath(sq, color, style = Stroke(stroke)) }
        drawCircle(color, r * 0.42f, center, style = Stroke(stroke))
        drawCircle(color, r * 0.12f, center)
    }

    /** Sivri mihrap kemeri: iki yanda sütun, üstte iki yayın buluştuğu uç */
    fun archPath(w: Float, h: Float, x: Float = 0f, y: Float = 0f): Path = Path().apply {
        val spring = y + h * 0.42f
        moveTo(x, y + h)
        lineTo(x, spring)
        cubicTo(x, y + h * 0.16f, x + w * 0.32f, y + h * 0.04f, x + w / 2, y)
        cubicTo(x + w * 0.68f, y + h * 0.04f, x + w, y + h * 0.16f, x + w, spring)
        lineTo(x + w, y + h)
    }

    /** Ufuk silueti: ortada kubbe ve alemi, iki yanda ince minare, altta yer çizgisi (dolu) */
    fun skylinePath(w: Float, h: Float): Path = Path().apply {
        val ground = h
        val cx = w / 2
        val domeW = min(w * 0.28f, h * 1.6f)
        val domeH = h * 0.55f
        moveTo(0f, ground)
        lineTo(0f, ground - h * 0.08f)
        // Sol minare
        val m1 = cx - domeW * 0.95f
        lineTo(m1 - h * 0.05f, ground - h * 0.08f); lineTo(m1 - h * 0.05f, ground - h * 0.78f)
        lineTo(m1, ground - h * 0.95f); lineTo(m1 + h * 0.05f, ground - h * 0.78f); lineTo(m1 + h * 0.05f, ground - h * 0.22f)
        // Kubbe
        lineTo(cx - domeW / 2, ground - h * 0.22f)
        cubicTo(cx - domeW / 2, ground - h * 0.22f - domeH, cx + domeW / 2, ground - h * 0.22f - domeH, cx + domeW / 2, ground - h * 0.22f)
        // Sağ minare
        val m2 = cx + domeW * 0.95f
        lineTo(m2 - h * 0.05f, ground - h * 0.22f); lineTo(m2 - h * 0.05f, ground - h * 0.78f)
        lineTo(m2, ground - h * 0.95f); lineTo(m2 + h * 0.05f, ground - h * 0.78f); lineTo(m2 + h * 0.05f, ground - h * 0.08f)
        lineTo(w, ground - h * 0.08f); lineTo(w, ground); close()
    }
}

/** Köşede hatem (vakit sayacı, kıble): kutunun içine çizilir, taşan kısmı kesilir */
@Composable
fun HatemMotif(modifier: Modifier, color: Color, alpha: Float = 0.08f) {
    Canvas(modifier) {
        val r = min(size.width, size.height) / 2
        with(Motifs) { hatem(Offset(size.width / 2, size.height / 2), r * 0.96f, color.copy(alpha = alpha), r * 0.05f) }
    }
}

/** Ufukta kubbe-minare silueti (%12; vakit sayacının altında) */
@Composable
fun SkylineMotif(modifier: Modifier, color: Color, alpha: Float = 0.12f) {
    Canvas(modifier) { drawPath(Motifs.skylinePath(size.width, size.height), color.copy(alpha = alpha)) }
}

/** Başlık şeridi (Kur'an, dualar): ortada mihrap kemeri, içinde küçük hatem, iki yanda ince çizgi */
@Composable
fun ArchBanner(modifier: Modifier, color: Color, alpha: Float = 0.10f) {
    Canvas(modifier) {
        val c = color.copy(alpha = alpha)
        val w = size.height * 0.75f
        val x = (size.width - w) / 2
        drawPath(Motifs.archPath(w, size.height, x, 0f), c, style = Stroke(size.height * 0.035f))
        with(Motifs) { hatem(Offset(size.width / 2, size.height * 0.58f), size.height * 0.2f, c, size.height * 0.025f) }
        val y = size.height * 0.95f
        drawLine(c, Offset(0f, y), Offset(x - size.height * 0.1f, y), size.height * 0.02f)
        drawLine(c, Offset(x + w + size.height * 0.1f, y), Offset(size.width, y), size.height * 0.02f)
    }
}
