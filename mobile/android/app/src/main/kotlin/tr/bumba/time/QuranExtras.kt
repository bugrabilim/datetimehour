package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/*
 * Kur'an ekleri (3 Kasım 2026, 2. analiz #62, #71; iOS'taki QuranExtras.swift ile aynı kayıt anahtarları): hatim takibi
 * (kaldığım yerden ilerleme, 30 cüz halkası, tahmini bitiş, tamamlanan hatim sayısı; `quran.hatim`), okuma zemini
 * (`quran.theme`: uygulama, kâğıt, sepya, yeşil, gül, gece mavisi, koyu) ve günlük ayet hedefi (`quran.goal`, okunan
 * ayetler `quran.read`: kaldığım yer işareti ileri alındıkça; tek seferde 50 ayetten uzun atlama okuma sayılmaz).
 */
object QuranPlan {
    const val TOTAL = 6236
    private val sp get() = Prefs.store

    fun index(s: Int, a: Int) = Quran.suras[s - 1].start + a - 1
    fun bookmarkIndex(): Int? = Quran.bookmark?.let { index(it.first, it.second) }

    /** Ayetin cüzü (1–30) */
    fun juzOf(i: Int): Int = Quran.juz.indexOfLast { (s, a) -> index(s, a) <= i } + 1

    private fun hatim() = runCatching { JSONObject(sp.getString("quran.hatim", "{}")) }.getOrDefault(JSONObject())
    fun completed() = hatim().optInt("n", 0)

    /** Başlangıç günü ve ayeti (yoksa şimdiki yer bugünden başlamış sayılır) */
    fun start(): Pair<LocalDate, Int> {
        val h = hatim()
        val d = runCatching { LocalDate.parse(h.getString("s")) }.getOrNull()
        if (d != null) return d to h.optInt("i", 0)
        val now = LocalDate.now() to (bookmarkIndex() ?: 0)
        sp.edit().putString("quran.hatim", JSONObject().put("s", now.first.toString()).put("i", now.second).put("n", 0).toString()).apply()
        return now
    }

    /** Tahmini bitiş (en az bir gün ve bir ayet ilerleme gerekir) */
    fun eta(today: LocalDate = LocalDate.now()): LocalDate? {
        val i = bookmarkIndex() ?: return null
        val (d, i0) = start()
        val days = ChronoUnit.DAYS.between(d, today).coerceAtLeast(1)
        val read = i - i0
        if (read <= 0 || ChronoUnit.DAYS.between(d, today) < 1) return null
        val left = TOTAL - 1 - i
        return today.plusDays(Math.ceil(left / (read.toDouble() / days)).toLong())
    }

    fun newHatim() {
        val done = (bookmarkIndex() ?: 0) >= TOTAL - 70
        sp.edit().putString("quran.hatim", JSONObject().put("s", LocalDate.now().toString()).put("i", 0).put("n", completed() + if (done) 1 else 0).toString()).apply()
        Quran.bookmark = 1 to 1
    }

    // Günlük hedef
    var goal: Int
        get() = sp.getInt("quran.goal", 0)
        set(v) = sp.edit().putInt("quran.goal", v).apply()
    val GOALS = listOf(0, 5, 10, 20, 50, 100)

    fun readToday(today: LocalDate = LocalDate.now()) = runCatching { JSONObject(sp.getString("quran.read", "{}")).optInt(today.toString(), 0) }.getOrDefault(0)

    /** Kaldığım yeri değiştir; ileri doğru ve 50 ayetten kısa adımlar bugünün okumasına eklenir */
    fun setBookmark(s: Int, a: Int) {
        val old = bookmarkIndex()
        Quran.bookmark = s to a
        val new = index(s, a)
        if (old != null && new > old && new - old <= 50) {
            val o = runCatching { JSONObject(sp.getString("quran.read", "{}")) }.getOrDefault(JSONObject())
            val k = LocalDate.now().toString()
            o.put(k, o.optInt(k, 0) + (new - old))
            val cut = LocalDate.now().minusDays(60).toString()
            o.keys().asSequence().toList().filter { it < cut }.forEach { o.remove(it) }
            sp.edit().putString("quran.read", o.toString()).apply()
        }
        start()
    }

    // Okuma zemini
    data class Palette(val bg: Color, val card: Color, val ink: Color, val muted: Color, val accentSoft: Color)
    val THEMES = listOf("app", "paper", "sepia", "green", "rose", "night", "dark")
    var theme: String
        get() = sp.getString("quran.theme", "app") ?: "app"
        set(v) = sp.edit().putString("quran.theme", v).apply()

    @Composable
    fun palette(id: String = theme): Palette = when (id) {
        "paper" -> Palette(Color(0xFFFBF8F1), Color(0xFFFFFFFF), Color(0xFF1B1B1B), Color(0xFF6B6B6B), Color(0xFFF1E6C9))
        "sepia" -> Palette(Color(0xFFF3E7CF), Color(0xFFF8EEDB), Color(0xFF3B2A12), Color(0xFF7A6346), Color(0xFFE8D3A6))
        "green" -> Palette(Color(0xFFE6F2EA), Color(0xFFF2F9F4), Color(0xFF10301F), Color(0xFF4F6F5C), Color(0xFFC9E4D2))
        "rose" -> Palette(Color(0xFFF8E9EC), Color(0xFFFDF3F5), Color(0xFF3A1A22), Color(0xFF7A5560), Color(0xFFF0D0D8))
        "night" -> Palette(Color(0xFF0F1A2E), Color(0xFF17243D), Color(0xFFE8EEF8), Color(0xFF9AA8C0), Color(0xFF263A5E))
        "dark" -> Palette(Color(0xFF000000), Color(0xFF121212), Color(0xFFEDEDED), Color(0xFF9E9E9E), Color(0xFF2A2A2A))
        else -> Palette(Theme.c.bg, Theme.c.surface, Theme.c.ink, Theme.c.muted, Theme.c.accentSoft)
    }
}

/** Hatim kartı: 30 cüz halkası, ilerleme, tahmini bitiş, yeni hatim */
@Composable
fun HatimCard() {
    var v by remember { mutableStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    @Suppress("UNUSED_EXPRESSION") v
    val i = QuranPlan.bookmarkIndex() ?: 0
    val p = (i + 1).toDouble() / QuranPlan.TOTAL
    val juz = QuranPlan.juzOf(i)
    val accent = Theme.c.accent; val track = Theme.c.surface2
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).semantics { contentDescription = T("quran.hatimProgress", Fmt.percent((p * 100).roundToInt()), juz) }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 9f * density / 2
                    for (k in 0 until 30) {
                        val filled = k < juz - 1 || (k == juz - 1 && p >= 0.999)
                        drawArc(if (filled) accent else if (k == juz - 1) accent.copy(alpha = 0.45f) else track, -90f + k * 12f + 1.2f, 9.6f, false, style = Stroke(sw))
                    }
                }
                Text("$juz", color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, style = Tnum)
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(L("quran.hatim"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(T("quran.hatimProgress", Fmt.percent((p * 100).roundToInt()), juz), color = Theme.c.muted, fontSize = 13.sp)
                QuranPlan.eta()?.let { Text(T("quran.hatimEta", Fmt.longDate(it)), color = Theme.c.muted, fontSize = 13.sp) }
                if (QuranPlan.completed() > 0) Text(T("quran.hatimCount", QuranPlan.completed()), color = Theme.c.muted, fontSize = 13.sp)
            }
        }
        val goal = QuranPlan.goal
        if (goal > 0) {
            val n = QuranPlan.readToday()
            Text(if (n >= goal) L("quran.goalDone") else T("quran.todayRead", n, goal), color = if (n >= goal) accent else Theme.c.text, fontSize = 14.sp)
        }
        TextButton(onClick = { confirm = true }) { Text(L("quran.hatimNew"), color = accent) }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        text = { Text(L("quran.hatimConfirm")) },
        confirmButton = { TextButton(onClick = { QuranPlan.newHatim(); confirm = false; v++ }) { Text(L("quran.hatimNew"), color = accent) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(L("badges.deleteNo")) } },
    )
}

/** Sure ekranının üstünde zemin ve günlük hedef seçimi */
@Composable
fun QuranReadingOptions(onTheme: (String) -> Unit) {
    var goal by remember { mutableStateOf(QuranPlan.goal) }
    var theme by remember { mutableStateOf(QuranPlan.theme) }
    val pal = QuranPlan.palette(theme)
    Text(L("quran.theme"), color = pal.muted, fontSize = 13.sp)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (t in QuranPlan.THEMES) {
            val p = QuranPlan.palette(t)
            val on = t == theme
            Row(Modifier.background(p.card, RoundedCornerShape(50)).clickable { theme = t; QuranPlan.theme = t; onTheme(t) }
                .padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(if (on) Theme.c.accent else p.bg, RoundedCornerShape(50)))
                Text(" " + L("quran.theme.$t"), color = p.ink, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
    ChoiceRow(L("quran.goal"), if (goal == 0) L("quran.goalOff") else T("quran.goalAyas", goal),
        QuranPlan.GOALS.map { it to if (it == 0) L("quran.goalOff") else T("quran.goalAyas", it) }) { goal = it; QuranPlan.goal = it }
    if (goal > 0) {
        val n = QuranPlan.readToday()
        Text(if (n >= goal) L("quran.goalDone") else T("quran.todayRead", n, goal), color = if (n >= goal) Theme.c.accent else pal.ink, fontSize = 14.sp)
        Text(L("quran.goalNote"), color = pal.muted, fontSize = 12.sp)
    }
}
