package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Ders çalışma sayacı (3 Kasım 2026, 2. analiz #84; sahibin ekran görüntüsü): ders seçimi ("Genel" + kullanıcının
 * dersleri), yedi bölütlü sarı LED rakamlarla süre (sönük bölütler görünür), çözülen soru sayacı, oturum notu ve
 * istatistik; "Pomodoro" sekmesi uygulamanın pomodorosunu dersin adıyla açar. Kayıtlar yalnız telefonda:
 * `study.subjects` (JSON dizi), `study.subject` (seçili; boş = Genel), süren oturum `study.start`/`study.acc` (ms),
 * `study.q`, `study.note`, geçmiş `study.log` (JSON `[{d, s, m, q, n}]`, en çok 1000). iOS'taki Study.swift ile aynı.
 */
object Study {
    const val SUBJECTS = "study.subjects"
    const val SUBJECT = "study.subject"
    const val LOG = "study.log"
    const val MAX_LOG = 1000

    data class Entry(val d: String, val s: String, val m: Int, val q: Int, val n: String = "")

    /** Bir ders için toplam: dakika ve soru */
    data class Total(val s: String, val m: Int, val q: Int)

    fun subjects(): List<String> = runCatching {
        val a = JSONArray(Prefs.store.getString(SUBJECTS, "[]"))
        (0 until a.length()).map { a.getString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    fun saveSubjects(list: List<String>) {
        Prefs.store.edit().putString(SUBJECTS, JSONArray(list.distinct().take(30)).toString()).apply()
    }

    fun load(): List<Entry> = runCatching {
        val a = JSONArray(Prefs.store.getString(LOG, "[]"))
        (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
            Entry(o.optString("d"), o.optString("s"), o.optInt("m"), o.optInt("q"), o.optString("n"))
        }
    }.getOrDefault(emptyList())

    fun save(log: List<Entry>) {
        val a = JSONArray()
        log.takeLast(MAX_LOG).forEach { e ->
            a.put(JSONObject().put("d", e.d).put("s", e.s).put("m", e.m).put("q", e.q).apply { if (e.n.isNotEmpty()) put("n", e.n) })
        }
        Prefs.store.edit().putString(LOG, a.toString()).apply()
    }

    fun clear() { Prefs.store.edit().remove(LOG).apply() }

    fun name(s: String): String = s.ifEmpty { L("study.general") }

    /** Süren oturumun geçen süresi (ms) */
    fun elapsed(start: Long, acc: Long, now: Long): Long = acc + if (start > 0) maxOf(0L, now - start) else 0L

    /** Oturum kaydı: en az 1 dakika ya da en az 1 soru varsa yazılır; yazıldıysa true */
    fun record(log: List<Entry>, day: LocalDate, subject: String, ms: Long, q: Int, note: String): List<Entry>? {
        val m = ((ms + 30_000) / 60_000).toInt()
        if (m < 1 && q < 1) return null
        return (log + Entry(day.toString(), subject, m, q, note.trim().take(200))).takeLast(MAX_LOG)
    }

    /** Gün aralığındaki toplam (bugün dahil son `days` gün; days = 0 → bütün kayıtlar) */
    fun total(log: List<Entry>, today: LocalDate, days: Int): Total {
        val from = if (days > 0) today.minusDays(days - 1L).toString() else ""
        val sel = log.filter { it.d >= from && it.d <= today.toString() }
        return Total("", sel.sumOf { it.m }, sel.sumOf { it.q })
    }

    /** Derslere göre son `days` gün, en çok çalışılandan */
    fun bySubject(log: List<Entry>, today: LocalDate, days: Int = 7): List<Total> {
        val from = today.minusDays(days - 1L).toString()
        return log.filter { it.d >= from && it.d <= today.toString() }.groupBy { it.s }
            .map { (s, l) -> Total(s, l.sumOf { it.m }, l.sumOf { it.q }) }
            .sortedWith(compareByDescending<Total> { it.m }.thenByDescending { it.q })
    }
}

/** Sarı LED rengi ve sönük bölüt */
private val LED_ON = Color(0xFFFFC83D)
private val LED_PANEL = Color(0xFF14110A)

/** Yedi bölütlü rakam göstergesi: 0–9, ':' ve boşluk; sönük bölütler de çizilir */
@Composable
fun SevenSegment(text: String, height: Dp, modifier: Modifier = Modifier, color: Color = LED_ON) {
    val digitW = 0.56f
    val colonW = 0.26f
    val gapW = 0.14f
    val units = text.sumOf { (if (it == ':') colonW else digitW).toDouble() } + gapW * (text.length - 1).coerceAtLeast(0)
    Canvas(modifier.size(height * units.toFloat(), height).semantics { contentDescription = text }) {
        val h = size.height
        var x = 0f
        text.forEach { ch ->
            if (ch == ':') {
                val r = h * 0.055f
                listOf(0.32f, 0.68f).forEach { drawCircle(color, r, Offset(x + h * colonW / 2, h * it)) }
                x += h * colonW
            } else {
                drawDigit(ch, x, h * digitW, h, color)
                x += h * digitW
            }
            x += h * gapW
        }
    }
}

/** Bölütler a b c d e f g (saat yönünde üstten, ortadaki g) */
private val SEGMENTS = mapOf(
    '0' to "abcdef", '1' to "bc", '2' to "abged", '3' to "abgcd", '4' to "fgbc",
    '5' to "afgcd", '6' to "afgedc", '7' to "abc", '8' to "abcdefg", '9' to "abcdfg", '-' to "g",
)

private fun DrawScope.drawDigit(ch: Char, x: Float, w: Float, h: Float, on: Color) {
    val lit = SEGMENTS[ch].orEmpty()
    val t = h * 0.11f
    val g = t * 0.18f
    val mid = h / 2
    fun horiz(y: Float) = Path().apply {
        val x0 = x + g; val x1 = x + w - g
        moveTo(x0, y); lineTo(x0 + t / 2, y - t / 2); lineTo(x1 - t / 2, y - t / 2); lineTo(x1, y)
        lineTo(x1 - t / 2, y + t / 2); lineTo(x0 + t / 2, y + t / 2); close()
    }
    fun vert(cx: Float, y0: Float, y1: Float) = Path().apply {
        val a = y0 + g; val b = y1 - g
        moveTo(cx, a); lineTo(cx + t / 2, a + t / 2); lineTo(cx + t / 2, b - t / 2); lineTo(cx, b)
        lineTo(cx - t / 2, b - t / 2); lineTo(cx - t / 2, a + t / 2); close()
    }
    val top = t / 2; val bottom = h - t / 2; val left = x + t / 2; val right = x + w - t / 2
    val paths = mapOf(
        'a' to horiz(top), 'g' to horiz(mid), 'd' to horiz(bottom),
        'f' to vert(left, top, mid), 'b' to vert(right, top, mid),
        'e' to vert(left, mid, bottom), 'c' to vert(right, mid, bottom),
    )
    paths.forEach { (k, p) -> drawPath(p, if (k in lit) on else on.copy(alpha = 0.10f)) }
}

/** LED paneli: koyu zemin üstünde yedi bölütlü yazı ve küçük etiket */
@Composable
fun LedPanel(text: String, label: String, height: Dp, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(LED_PANEL).padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Ltr { SevenSegment(text, height) }
        Text(label, color = LED_ON.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** HH:MM:SS (LED için hep iki basamak) */
private fun ledTime(ms: Long): String {
    val s = ms / 1000
    return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudyScreen() {
    var mode by remember { mutableStateOf(ToolState.str("study.mode", "timer")) }
    var subjects by remember { mutableStateOf(Study.subjects()) }
    var subject by remember { mutableStateOf(ToolState.str(Study.SUBJECT)) }
    var adding by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftSegmentedRow(Modifier.fillMaxWidth()) {
                listOf("timer", "pomo").forEachIndexed { i, m ->
                    SoftSegment(mode == m, { mode = m; ToolState.putStr("study.mode", m) }) {
                        Text(L(if (m == "timer") "study.modeTimer" else "study.modePomo"))
                    }
                }
            }
            // Dersler: dokununca seçilir, uzun basınca silinir (Genel silinmez)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("") + subjects).forEach { s ->
                    val on = s == subject
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(if (on) Theme.c.accent else Theme.c.surface2)
                            .combinedClickable(onLongClick = { if (s.isNotEmpty()) removing = s }) {
                                subject = s; ToolState.putStr(Study.SUBJECT, s)
                                if (mode == "pomo") ToolState.putStr("pomo.task", Study.name(s))
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text(Study.name(s), color = if (on) Theme.c.bg else Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Theme.c.surface2).combinedClickable { adding = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) { Icon(Icons.Filled.Add, L("study.addSubject"), tint = Theme.c.accent, modifier = Modifier.size(20.dp)) }
            }
            if (mode == "pomo") Text(L("study.pomoNote"), color = Theme.c.muted, fontSize = 12.sp)
        }
        Box(Modifier.weight(1f)) {
            if (mode == "pomo") {
                // Pomodoro: görev adı seçili ders (pomodoro geçmişinde dersin adıyla görünür)
                remember(subject) { ToolState.putStr("pomo.task", Study.name(subject)); 0 }
                androidx.compose.runtime.key(subject) { PomodoroScreen() }
            } else {
                StudyTimer(subject)
            }
        }
    }
    if (adding) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(L("study.addSubject")) },
            text = { OutlinedTextField(name, { name = it.take(30) }, singleLine = true, label = { Text(L("study.subjectName")) }) },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.trim()
                    if (n.isNotEmpty() && n != L("study.general")) {
                        subjects = (subjects + n).distinct(); Study.saveSubjects(subjects)
                        subject = n; ToolState.putStr(Study.SUBJECT, n)
                    }
                    adding = false
                }, enabled = name.isNotBlank()) { Text(L("common.done"), color = Theme.c.accent) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
        )
    }
    removing?.let { s ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(T("study.removeSubject", s)) },
            text = { Text(L("study.removeNote"), color = Theme.c.muted) },
            confirmButton = {
                TextButton(onClick = {
                    subjects = subjects - s; Study.saveSubjects(subjects)
                    if (subject == s) { subject = ""; ToolState.putStr(Study.SUBJECT, "") }
                    removing = null
                }) { Text(L("study.remove"), color = Color(0xFFE5484D)) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
        )
    }
}

@Composable
private fun StudyTimer(subject: String) {
    var start by remember { mutableLongStateOf(ToolState.long("study.start")) }
    var acc by remember { mutableLongStateOf(ToolState.long("study.acc")) }
    var q by remember { mutableIntStateOf(ToolState.int("study.q", 0)) }
    var note by remember { mutableStateOf(ToolState.str("study.note")) }
    var log by remember { mutableStateOf(Study.load()) }
    var confirmClear by remember { mutableStateOf(false) }
    val now = rememberNow().toEpochMilli()
    val ms = Study.elapsed(start, acc, now)
    val running = start > 0
    fun persist() {
        ToolState.putLong("study.start", start); ToolState.putLong("study.acc", acc); ToolState.putInt("study.q", q)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LedPanel(ledTime(ms), Study.name(subject), 56.dp, Modifier.fillMaxWidth())
        // Soru sayacı
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledIconButton(onClick = { if (q > 0) { q--; persist() } }, Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Theme.c.surface2, contentColor = Theme.c.text)) {
                Icon(Icons.Filled.Remove, L("study.minus"))
            }
            LedPanel(String.format("%03d", q.coerceAtMost(999)), L("study.questions"), 44.dp, Modifier.weight(1f))
            FilledIconButton(onClick = { q++; persist() }, Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Theme.c.accent, contentColor = Theme.c.bg)) {
                Icon(Icons.Filled.Add, L("study.plus"), Modifier.size(34.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(
                L(if (running) "tools.stopwatch.stop" else if (ms > 0) "tools.stopwatch.resume" else "tools.stopwatch.start"),
                true, Modifier.weight(1f),
            ) {
                val t = System.currentTimeMillis()
                if (running) { acc += t - start; start = 0 } else start = t
                persist()
            }
            RoundButton(L("study.finish"), false, Modifier.weight(1f)) {
                val t = System.currentTimeMillis()
                Study.record(log, LocalDate.now(), subject, Study.elapsed(start, acc, t), q, note)?.let { log = it; Study.save(it) }
                start = 0; acc = 0; q = 0; note = ""
                persist(); ToolState.putStr("study.note", "")
            }
        }
        OutlinedTextField(note, { note = it.take(200); ToolState.putStr("study.note", note) }, Modifier.fillMaxWidth(),
            label = { Text(L("study.note")) }, placeholder = { Text(L("study.notePlaceholder")) }, minLines = 2)
        StudyStats(log) { confirmClear = true }
        Text(L("study.privacy"), color = Theme.c.muted, fontSize = 12.sp)
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(L("study.clearConfirm")) },
            confirmButton = { TextButton(onClick = { Study.clear(); log = emptyList(); confirmClear = false }) { Text(L("study.clear"), color = Color(0xFFE5484D)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
        )
    }
}

@Composable
private fun StudyStats(log: List<Study.Entry>, onClear: () -> Unit) {
    val today = LocalDate.now()
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(L("study.stats"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (log.isEmpty()) {
                Text(L("study.empty"), color = Theme.c.muted)
                return@Column
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("study.today" to 1, "study.week" to 7, "study.total" to 0).forEach { (k, d) ->
                    val t = Study.total(log, today, d)
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Theme.c.surface2).padding(10.dp)) {
                        Text(L(k), color = Theme.c.muted, fontSize = 12.sp)
                        Text(T("study.min", t.m), color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(T("study.qCount", t.q), color = Theme.c.muted, fontSize = 12.sp)
                    }
                }
            }
            val subjects = Study.bySubject(log, today)
            if (subjects.isNotEmpty()) {
                Text(L("study.bySubject"), color = Theme.c.muted, fontSize = 13.sp)
                val max = subjects.maxOf { it.m }.coerceAtLeast(1)
                subjects.take(8).forEach { s ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row {
                            Text(Study.name(s.s), Modifier.weight(1f), color = Theme.c.text, fontSize = 14.sp, maxLines = 1)
                            Text("${T("study.min", s.m)} · ${T("study.qCount", s.q)}", color = Theme.c.muted, fontSize = 13.sp)
                        }
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Theme.c.surface2)) {
                            Box(Modifier.fillMaxWidth(s.m.toFloat() / max).height(8.dp).clip(RoundedCornerShape(4.dp)).background(LED_ON))
                        }
                    }
                }
            }
            Text(L("study.recent"), color = Theme.c.muted, fontSize = 13.sp)
            log.takeLast(5).reversed().forEach { e ->
                Column(Modifier.fillMaxWidth()) {
                    Row {
                        Text("${Fmt.dayMonth(LocalDate.parse(e.d))} · ${Study.name(e.s)}", Modifier.weight(1f), color = Theme.c.text, fontSize = 14.sp, maxLines = 1)
                        Text("${T("study.min", e.m)} · ${T("study.qCount", e.q)}", color = Theme.c.muted, fontSize = 13.sp)
                    }
                    if (e.n.isNotEmpty()) Text(e.n, color = Theme.c.muted, fontSize = 13.sp, maxLines = 2)
                }
            }
            Spacer(Modifier.width(1.dp))
            TextButton(onClick = onClear) { Text(L("study.clear"), color = Color(0xFFE5484D)) }
        }
    }
}
