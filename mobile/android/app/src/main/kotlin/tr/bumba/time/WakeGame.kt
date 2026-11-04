package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/**
 * Uyanma oyunu (3 Kasım 2026, 2. analiz #7; sahibin ekran görüntüleri): alarmı kapatınca civciv uyanır ve puan
 * kazanılır; puanla civciv büyür (yumurta → civciv → ibikli → atkılı → taçlı). Puan kayıttan hesaplanır, ayrıca
 * saklanmaz: her uyanış 10, ertelemeden kalkınca +10 (bir erteleme +3, iki ve üstü 0), kapatma görevi +5, üst üste
 * ertelemesiz her gün +2 (en çok +10). Kayıt `wake.events` (JSON `[{d, w, a, s, t}]`: gün, kalkış ve alarm dakikası,
 * erteleme sayısı, görev yapıldı mı; gün başına bir kayıt, en çok 400), yalnız telefonda. iOS'taki WakeGame.swift
 * ile aynı kurallar.
 */
object WakeGame {
    const val KEY = "wake.events"
    const val MAX = 400

    data class Event(val d: String, val w: Int, val a: Int, val s: Int, val t: Boolean)

    /** Puanlı gün (rapor satırı) */
    data class Scored(val e: Event, val points: Int, val streak: Int)

    fun load(): List<Event> = runCatching {
        val arr = JSONArray(Prefs.store.getString(KEY, "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it) }.map { o ->
            Event(o.optString("d"), o.optInt("w"), o.optInt("a", -1), o.optInt("s"), o.optBoolean("t"))
        }
    }.getOrDefault(emptyList())

    fun save(l: List<Event>) {
        val arr = JSONArray()
        l.takeLast(MAX).forEach { arr.put(JSONObject().put("d", it.d).put("w", it.w).put("a", it.a).put("s", it.s).put("t", it.t)) }
        Prefs.store.edit().putString(KEY, arr.toString()).apply()
    }

    fun clear() = Prefs.store.edit().remove(KEY).apply()

    /** Günün kaydı: aynı gün ikinci kez yazılırsa birleştirilir (erteleme en büyüğü, görev ya da ile) */
    fun merge(log: List<Event>, e: Event): List<Event> {
        val old = log.firstOrNull { it.d == e.d } ?: return (log + e).sortedBy { it.d }.takeLast(MAX)
        val m = old.copy(s = max(old.s, e.s), t = old.t || e.t, a = if (old.a >= 0) old.a else e.a)
        return log.map { if (it.d == e.d) m else it }
    }

    fun record(day: LocalDate, wake: Int, alarm: Int, snoozes: Int, task: Boolean) {
        save(merge(load(), Event(day.toString(), wake, alarm, snoozes.coerceAtLeast(0), task)))
    }

    /** Bütün günler puanıyla (gün sırasıyla); seri yalnız art arda günlerde sürer */
    fun scored(log: List<Event>): List<Scored> {
        var streak = 0
        var prev: LocalDate? = null
        return log.sortedBy { it.d }.map { e ->
            val day = runCatching { LocalDate.parse(e.d) }.getOrNull()
            val consecutive = prev != null && day != null && prev!!.plusDays(1) == day
            streak = if (e.s == 0) (if (consecutive) streak + 1 else 1) else 0
            prev = day
            val base = 10 + when (e.s) { 0 -> 10; 1 -> 3; else -> 0 } + if (e.t) 5 else 0
            Scored(e, base + if (e.s == 0) min(10, 2 * (streak - 1)) else 0, streak)
        }
    }

    fun total(log: List<Event>): Int = scored(log).sumOf { it.points }

    /** Seviye: 1 → 0, 2 → 100, 3 → 300, 4 → 600 … (n·(n−1)/2 · 100), en çok 10 */
    fun level(points: Int): Int {
        var l = 1
        while (l < 10 && points >= threshold(l + 1)) l++
        return l
    }

    fun threshold(level: Int): Int = 100 * level * (level - 1) / 2

    /** Civcivin hâli: 0 yumurta, 1 civciv, 2 ibikli, 3 atkılı, 4 taçlı */
    fun stage(level: Int): Int = when {
        level <= 1 -> 0
        level <= 3 -> 1
        level <= 5 -> 2
        level <= 7 -> 3
        else -> 4
    }

    /** Bugün uyandı mı */
    fun awakeToday(log: List<Event>, today: LocalDate = LocalDate.now()) = log.any { it.d == today.toString() }

    /** En uzun ertelemesiz seri */
    fun bestStreak(log: List<Event>) = scored(log).maxOfOrNull { it.streak } ?: 0
}

/** Civciv çizimi: uyurken gözler kapalı ve "z", uyanıkken gözler açık; hâline göre ibik, atkı, taç */
@Composable
fun WakePet(stage: Int, awake: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val desc = L(if (awake) "wakegame.awake" else "wakegame.sleeping")
    Canvas(modifier.size(size).semantics { contentDescription = desc }) { drawPet(stage, awake) }
}

private val YELLOW = Color(0xFFFFD43B)
private val YELLOW_DARK = Color(0xFFF2B705)
private val ORANGE = Color(0xFFF08C00)
private val RED = Color(0xFFE03131)
private val INK = Color(0xFF2B2B2B)

private fun DrawScope.drawPet(stage: Int, awake: Boolean) {
    val w = size.width
    val h = size.height
    val cx = w / 2
    // Zemin gölgesi
    drawOval(Color.Black.copy(alpha = 0.10f), Offset(w * 0.2f, h * 0.86f), Size(w * 0.6f, h * 0.08f))
    if (stage == 0) {
        // Yumurta: uyanıkken çatlak ve gözler
        drawOval(Color(0xFFFFF4E0), Offset(w * 0.27f, h * 0.18f), Size(w * 0.46f, h * 0.7f))
        drawOval(Color(0xFFE9D8BA), Offset(w * 0.27f, h * 0.18f), Size(w * 0.46f, h * 0.7f), style = Stroke(w * 0.012f))
        if (awake) {
            val crack = Path().apply {
                moveTo(w * 0.29f, h * 0.48f); lineTo(w * 0.36f, h * 0.43f); lineTo(w * 0.42f, h * 0.5f)
                lineTo(w * 0.5f, h * 0.42f); lineTo(w * 0.57f, h * 0.5f); lineTo(w * 0.64f, h * 0.43f); lineTo(w * 0.71f, h * 0.48f)
            }
            drawPath(crack, Color(0xFFB08D57), style = Stroke(w * 0.015f))
            eyes(cx, h * 0.58f, w * 0.08f, w, true)
        } else zzz(w, h)
        return
    }
    val r = w * (0.24f + 0.02f * stage)
    val cy = h * 0.58f
    // Kanatlar
    drawOval(YELLOW_DARK, Offset(cx - r * 1.15f, cy - r * 0.1f), Size(r * 0.55f, r * 0.75f))
    drawOval(YELLOW_DARK, Offset(cx + r * 0.6f, cy - r * 0.1f), Size(r * 0.55f, r * 0.75f))
    // Ayaklar
    drawRoundRect(ORANGE, Offset(cx - r * 0.45f, cy + r * 0.85f), Size(r * 0.25f, r * 0.3f), CornerRadius(r * 0.1f))
    drawRoundRect(ORANGE, Offset(cx + r * 0.2f, cy + r * 0.85f), Size(r * 0.25f, r * 0.3f), CornerRadius(r * 0.1f))
    // Gövde
    drawCircle(YELLOW, r, Offset(cx, cy))
    // İbik (2+)
    if (stage >= 2) {
        listOf(-0.22f, 0f, 0.22f).forEach { dx -> drawCircle(RED, r * 0.16f, Offset(cx + r * dx, cy - r * 1.02f)) }
    }
    // Taç (4)
    if (stage >= 4) {
        val top = cy - r * 1.05f
        val crown = Path().apply {
            moveTo(cx - r * 0.45f, top); lineTo(cx - r * 0.45f, top - r * 0.38f); lineTo(cx - r * 0.22f, top - r * 0.18f)
            lineTo(cx, top - r * 0.45f); lineTo(cx + r * 0.22f, top - r * 0.18f); lineTo(cx + r * 0.45f, top - r * 0.38f)
            lineTo(cx + r * 0.45f, top); close()
        }
        drawPath(crown, Color(0xFFFAB005))
        drawPath(crown, Color(0xFFE67700), style = Stroke(r * 0.04f))
    }
    // Göz ve gaga
    eyes(cx, cy - r * 0.2f, r * 0.32f, w, awake)
    val beak = Path().apply {
        moveTo(cx - r * 0.14f, cy + r * 0.02f); lineTo(cx + r * 0.14f, cy + r * 0.02f); lineTo(cx, cy + r * 0.22f); close()
    }
    drawPath(beak, ORANGE)
    // Yanak
    drawCircle(Color(0xFFFF8787).copy(alpha = 0.5f), r * 0.1f, Offset(cx - r * 0.5f, cy + r * 0.05f))
    drawCircle(Color(0xFFFF8787).copy(alpha = 0.5f), r * 0.1f, Offset(cx + r * 0.5f, cy + r * 0.05f))
    // Atkı (3+)
    if (stage >= 3) {
        drawRoundRect(Color(0xFF1C7ED6), Offset(cx - r * 0.75f, cy + r * 0.38f), Size(r * 1.5f, r * 0.2f), CornerRadius(r * 0.1f))
        drawRoundRect(Color(0xFF1C7ED6), Offset(cx + r * 0.35f, cy + r * 0.45f), Size(r * 0.2f, r * 0.45f), CornerRadius(r * 0.08f))
    }
    if (!awake) zzz(w, h)
}

private fun DrawScope.eyes(cx: Float, y: Float, dx: Float, w: Float, open: Boolean) {
    listOf(-dx, dx).forEach { x ->
        if (open) {
            drawCircle(INK, w * 0.03f, Offset(cx + x, y))
            drawCircle(Color.White, w * 0.01f, Offset(cx + x + w * 0.01f, y - w * 0.01f))
        } else {
            drawArc(INK, 20f, 140f, false, Offset(cx + x - w * 0.035f, y - w * 0.03f), Size(w * 0.07f, w * 0.05f), style = Stroke(w * 0.012f))
        }
    }
}

private fun DrawScope.zzz(w: Float, h: Float) {
    listOf(Triple(0.74f, 0.3f, 0.07f), Triple(0.82f, 0.2f, 0.055f), Triple(0.88f, 0.12f, 0.04f)).forEach { (x, y, s) ->
        val p = Path().apply {
            moveTo(w * x, h * y); lineTo(w * (x + s), h * y); lineTo(w * x, h * y + w * s); lineTo(w * (x + s), h * y + w * s)
        }
        drawPath(p, Color(0xFF748FFC), style = Stroke(w * 0.012f))
    }
}

private fun hm(m: Int) = if (m < 0) "–" else String.format(java.util.Locale.ROOT, "%02d:%02d", (m / 60) % 24, m % 60)

/** Uyanma oyunu ekranı: civciv (Beni uyandır), Puanlarım, Rapor */
@Composable
fun WakeGameScreen(initialTab: String = "pet") {
    var log by remember { mutableStateOf(WakeGame.load()) }
    var tab by remember { mutableStateOf(initialTab) }
    var confirm by remember { mutableStateOf(false) }
    val total = WakeGame.total(log)
    val level = WakeGame.level(total)
    val awake = WakeGame.awakeToday(log)
    val scored = WakeGame.scored(log)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SoftSegmentedRow(Modifier.fillMaxWidth()) {
            listOf("pet", "points", "report").forEachIndexed { i, t ->
                SoftSegment(tab == t, { tab = t }) {
                    Text(L("wakegame.tab.$t"), fontSize = 13.sp, maxLines = 1)
                }
            }
        }
        when (tab) {
            "pet" -> AppCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WakePet(WakeGame.stage(level), awake, 200.dp)
                    Text(L(if (awake) "wakegame.awakeTitle" else "wakegame.wakeMe"), color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(L(if (awake) "wakegame.awakeText" else "wakegame.sleepText"), color = Theme.c.muted, fontSize = 15.sp)
                    LevelBar(total, level)
                }
            }
            "points" -> {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(L("wakegame.total"), color = Theme.c.muted, fontSize = 13.sp)
                        Text("$total", color = Theme.c.accent, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        LevelBar(total, level)
                    }
                }
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(L("wakegame.how"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(L("wakegame.howText"), color = Theme.c.muted, fontSize = 14.sp)
                    }
                }
                if (scored.isNotEmpty()) AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        scored.takeLast(10).reversed().forEach { s ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(Fmt.dayMonth(LocalDate.parse(s.e.d)), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                                Text(T("wakegame.earned", s.points), color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            else -> WakeReport(log, scored)
        }
        if (log.isEmpty()) Text(L("wakegame.empty"), color = Theme.c.muted, fontSize = 14.sp)
        else TextButton(onClick = { confirm = true }) { Text(L("wakegame.reset"), color = Color(0xFFE5484D)) }
        Text(L("wakegame.privacy"), color = Theme.c.muted, fontSize = 12.sp)
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(L("wakegame.resetConfirm")) },
            confirmButton = { TextButton(onClick = { WakeGame.clear(); log = emptyList(); confirm = false }) { Text(L("wakegame.reset"), color = Color(0xFFE5484D)) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
        )
    }
}

@Composable
private fun LevelBar(total: Int, level: Int) {
    val lo = WakeGame.threshold(level)
    val hi = WakeGame.threshold(level + 1)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(T("wakegame.level", level), Modifier.weight(1f), color = Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (level < 10) Text(T("wakegame.next", hi - total), color = Theme.c.muted, fontSize = 13.sp)
        }
        val frac = if (level >= 10) 1f else (total - lo).toFloat() / (hi - lo)
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Theme.c.surface2)) {
            Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(10.dp).clip(RoundedCornerShape(5.dp)).background(YELLOW_DARK))
        }
    }
}

@Composable
private fun WakeReport(log: List<WakeGame.Event>, scored: List<WakeGame.Scored>) {
    val today = LocalDate.now()
    val week = log.filter { it.d >= today.minusDays(6).toString() }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(L("wakegame.week"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            val avg = if (week.isEmpty()) -1 else week.sumOf { it.w } / week.size
            val onTime = if (week.isEmpty()) 0 else week.count { it.s == 0 } * 100 / week.size
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(L("wakegame.avgWake"), hm(avg), Modifier.weight(1f))
                Stat(L("wakegame.onTime"), Fmt.percent(onTime), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(L("wakegame.snoozes"), "${week.sumOf { it.s }}", Modifier.weight(1f))
                Stat(L("wakegame.bestStreak"), T("wakegame.days", WakeGame.bestStreak(log)), Modifier.weight(1f))
            }
        }
    }
    if (scored.isNotEmpty()) AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            scored.takeLast(14).reversed().forEach { s ->
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Text(Fmt.dayMonth(LocalDate.parse(s.e.d)), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Ltr { Text(hm(s.e.w), color = Theme.c.text, fontSize = 15.sp) }
                    }
                    val parts = buildList {
                        if (s.e.a >= 0) add(T("wakegame.alarmAt", hm(s.e.a)))
                        add(if (s.e.s == 0) L("wakegame.noSnooze") else T("wakegame.snoozeN", s.e.s))
                        if (s.e.t) add(L("wakegame.taskDone"))
                        add(T("wakegame.earned", s.points))
                    }
                    Text(parts.joinToString(" · "), color = Theme.c.muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Theme.c.surface2).padding(10.dp)) {
        Text(label, color = Theme.c.muted, fontSize = 12.sp, maxLines = 1)
        Text(value, color = Theme.c.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

/** Günaydın kartında: civciv uyandı ve kazanılan puan */
@Composable
fun WakeGameMorningCard() {
    val log = remember { WakeGame.load() }
    val today = WakeGame.scored(log).lastOrNull { it.e.d == LocalDate.now().toString() } ?: return
    val level = WakeGame.level(WakeGame.total(log))
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WakePet(WakeGame.stage(level), true, 72.dp)
            Column {
                Text(L("wakegame.awakeTitle"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(T("wakegame.earned", today.points) + " · " + T("wakegame.level", level), color = Theme.c.accent, fontSize = 15.sp)
            }
        }
    }
}
