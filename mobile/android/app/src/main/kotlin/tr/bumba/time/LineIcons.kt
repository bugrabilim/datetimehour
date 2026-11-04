package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * Saat Tarih'in kendi çizgi simgeleri (2. analiz #97; 4 Kasım 2026): 24 simge, 24×24 alan, 2 pt çizgi, ana renk +
 * ikinci renk; temaya göre renk değişir. Tanımlar mobile/shared/icons/icons.json (gen-strings kopyalar; iOS'taki
 * LineIcon aynı dosyayı okur). Komutlar: M L C Z ve O (daire).
 */
object LineIcons {
    class Part(val second: Boolean, val fill: Boolean, val d: String)

    private var all: Map<String, List<Part>>? = null

    fun get(name: String): List<Part> {
        val m = all ?: runCatching {
            val o = JSONObject(Strings.appContext.assets.open("icons.json").bufferedReader().use { it.readText() })
            o.keys().asSequence().associateWith { k ->
                val a = o.getJSONArray(k)
                (0 until a.length()).map { i -> a.getJSONArray(i).let { p -> Part(p.getString(0).startsWith("c2"), p.getString(0).endsWith("f"), p.getString(1)) } }
            }
        }.getOrDefault(emptyMap()).also { all = it }
        return m[name].orEmpty()
    }

    /** Yol metnini Compose yollarına çevirir (daireler ayrı) */
    fun parse(d: String): Pair<Path, List<Triple<Float, Float, Float>>> {
        val path = Path()
        val circles = ArrayList<Triple<Float, Float, Float>>()
        val tok = d.replace(",", " ").replace(Regex("([MLCZO])"), " $1 ").trim().split(Regex("\\s+"))
        var i = 0
        fun num() = tok[i++].toFloat()
        fun numeric() = i < tok.size && tok[i].first().let { it.isDigit() || it == '-' || it == '.' }
        while (i < tok.size) {
            when (tok[i++]) {
                "M" -> path.moveTo(num(), num())
                "L" -> { path.lineTo(num(), num()); while (numeric()) path.lineTo(num(), num()) }
                "C" -> path.cubicTo(num(), num(), num(), num(), num(), num())
                "Z" -> path.close()
                "O" -> while (numeric()) circles += Triple(num(), num(), num())
            }
        }
        return path to circles
    }
}

/** Çizgi simge; `label` verilirse ekran okuyucu için ad */
@Composable
fun LineIcon(name: String, size: Dp = 28.dp, c1: Color = Theme.c.accent, c2: Color = Theme.c.accent2, label: String? = null) {
    val parts = LineIcons.get(name)
    Canvas(Modifier.size(size).semantics { if (label != null) contentDescription = label }) {
        val k = this.size.minDimension / 24f
        scale(k, k, Offset.Zero) {
            for (p in parts) {
                val color = if (p.second) c2 else c1
                val (path, circles) = LineIcons.parse(p.d)
                val style = if (p.fill) Fill else Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(path, color, style = style)
                for ((cx, cy, r) in circles) drawCircle(color, r, Offset(cx, cy), style = style)
            }
        }
    }
}

/** Ses türü/ruh hâli gibi listeler için kimlikler */
object LineIconSets {
    val SEASONS = listOf("flower", "sun", "leaf", "snowman")
    val MOODS = listOf("mood1", "mood2", "mood3", "mood4", "mood5")
}
