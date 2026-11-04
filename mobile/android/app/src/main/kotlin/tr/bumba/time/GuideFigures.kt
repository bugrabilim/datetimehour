package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

/**
 * Resimli namaz ve abdest rehberi (2. analiz #68; 4 Kasım 2026): adım adım (n/9) çizimler, erkek/kadın seçimi ve
 * Hanefî farkları. Duruşlar 100×100 kutuda eklem çizgileriyle tanımlı (iOS'taki GuideFigures ile aynı sayılar);
 * yüz sağa (kıbleye) dönük. Abdest adımları simgeyle. Kayıt yok.
 */
object GuideFigures {
    /** Namaz adımı → duruş (guide.json howto sırası) */
    val PRAYER = listOf("stand", "takbir", "fold", "ruku", "stand", "sajda", "fold", "sit", "salam")
    val WUDU = listOf("🤲", "✋", "👄", "👃", "🙂", "💪", "💆", "👂", "🦶")
    /** Erkek ve kadında farkı yazılan adımlar */
    val DIFF = setOf(1, 2, 3, 5, 7)

    data class Pose(val head: Pair<Float, Float>, val lines: List<FloatArray>)

    fun pose(id: String, female: Boolean): Pose = when (id) {
        "takbir" -> Pose(50f to 14f, body() + listOf(f(50, 26, 57, 26), f(57, 26, if (female) 58 else 56, if (female) 24 else 14)))
        "fold" -> Pose(50f to 14f, body() + listOf(f(50, 27, 56, if (female) 38 else 44), f(56, if (female) 38 else 44, 60, if (female) 33 else 48)))
        "ruku" -> if (female)
            Pose(70f to 38f, listOf(f(40, 56, 64, 44), f(40, 56, 42, 75), f(42, 75, 40, 95), f(60, 46, 48, 62), f(48, 62, 43, 70)))
        else Pose(78f to 53f, listOf(f(40, 56, 70, 55), f(40, 56, 40, 75), f(40, 75, 40, 95), f(66, 56, 54, 66), f(54, 66, 42, 73)))
        "sajda" -> Pose(74f to 86f, listOf(f(38, 74, 66, 82), f(38, 74, 46, 93), f(46, 93, 22, 94), f(62, 81, if (female) 64 else 70, if (female) 90 else 86),
            f(if (female) 64 else 70, if (female) 90 else 86, 78, 94)))
        "sit", "salam" -> if (female)
            Pose(46f to 42f, listOf(f(44, 84, 45, 50), f(44, 84, 66, 88), f(66, 88, 76, 94), f(45, 54, 56, 68), f(56, 68, 62, 80)))
        else Pose(46f to 42f, listOf(f(42, 82, 45, 50), f(42, 82, 64, 88), f(64, 88, 36, 93), f(36, 93, 30, 82), f(45, 54, 56, 68), f(56, 68, 62, 80)))
        else -> Pose(50f to 14f, body() + listOf(f(50, 27, 51, 40), f(51, 40, 52, 52)))
    }

    private fun body() = listOf(f(50, 22, 50, 55), f(50, 55, 51, 75), f(51, 75, 50, 95))
    private fun f(a: Int, b: Int, c: Int, d: Int) = floatArrayOf(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat())
}

@Composable
fun GuideFigure(id: String, female: Boolean, ink: Color, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        val ox = (size.width - 100f * s) / 2
        fun p(x: Float, y: Float) = Offset(ox + x * s, y * s)
        // Zemin (seccade)
        drawLine(accent.copy(alpha = 0.35f), p(12f, 96.5f), p(92f, 96.5f), strokeWidth = 5f * s, cap = StrokeCap.Round)
        val pose = GuideFigures.pose(id, female)
        for (l in pose.lines) drawLine(ink, p(l[0], l[1]), p(l[2], l[3]), strokeWidth = 4.2f * s, cap = StrokeCap.Round)
        val (hx, hy) = pose.head
        if (female) drawCircle(accent.copy(alpha = 0.55f), 9.5f * s, p(hx - 1f, hy))
        drawCircle(ink, 6.5f * s, p(hx, hy))
        if (id == "salam") {
            // Selam: baş önce sağa, sonra sola döner (ok)
            drawLine(accent, p(hx - 10f, hy - 14f), p(hx + 10f, hy - 14f), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
            drawLine(accent, p(hx + 10f, hy - 14f), p(hx + 6f, hy - 17f), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
            drawLine(accent, p(hx - 10f, hy - 14f), p(hx - 6f, hy - 11f), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
        }
    }
}

/** Rehberin başındaki resimli adım kartı */
@Composable
fun GuidePictureCard(data: JSONObject?) {
    var mode by rememberSaveable { mutableStateOf("prayer") }
    var female by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    val lang = Strings.lang
    val sections = data?.optJSONArray("sections")
    var steps: org.json.JSONArray? = null
    if (sections != null) for (i in 0 until sections.length()) {
        val s = sections.getJSONObject(i)
        if (s.optString("id") == (if (mode == "prayer") "howto" else "wudu")) steps = s.optJSONArray("steps")
    }
    val n = if (mode == "prayer") GuideFigures.PRAYER.size else GuideFigures.WUDU.size
    val i = step.coerceIn(0, n - 1)
    AppCard {
        SectionTitle(L("guide.pic.title"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(L("guide.pic.prayer"), mode == "prayer") { mode = "prayer"; step = 0 }
            Chip(L("guide.pic.wudu"), mode == "wudu") { mode = "wudu"; step = 0 }
            if (mode == "prayer") {
                Box(Modifier.size(12.dp))
                Chip(L("guide.pic.male"), !female) { female = false }
                Chip(L("guide.pic.female"), female) { female = true }
            }
        }
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            if (mode == "prayer") GuideFigure(GuideFigures.PRAYER[i], female, Theme.c.ink, Theme.c.accent, Modifier.size(200.dp))
            else Text(GuideFigures.WUDU[i], fontSize = 96.sp)
        }
        Text("${i + 1}/$n", Modifier.fillMaxWidth(), color = Theme.c.accent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        val text = steps?.optJSONObject(i)?.let { o -> o.optString(lang).ifEmpty { o.optString("en") } } ?: ""
        Text(text, color = Theme.c.text, fontSize = 16.sp)
        if (mode == "prayer" && i in GuideFigures.DIFF) {
            Text(L(if (female) "guide.pic.f.$i" else "guide.pic.m.$i"), color = Theme.c.accent, fontSize = 14.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { step = (i - 1).coerceAtLeast(0) }, enabled = i > 0) { Text("‹ " + L("hiit.prev")) }
            TextButton(onClick = { step = (i + 1).coerceAtMost(n - 1) }, enabled = i < n - 1) { Text(L("onboard.next") + " ›") }
        }
        if (mode == "prayer") Text(L("guide.pic.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
        Text(text, maxLines = 1, color = if (on) Theme.c.accent else Theme.c.muted, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
    }
}
