package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/**
 * Sabah günlüğü (rakip analizi listesi #76): her gün için ruh hâli, uyku, günün niyeti ve not.
 * Kayıt `journal` (JSON [{d, m, s, i, n}], en yeni üstte, en çok 366 gün; iOS'la aynı); yalnız telefonda (ve Google yedeğinde).
 */
data class JournalEntry(val d: String, val m: Int = 0, val s: Int = 0, val i: String = "", val n: String = "") {
    val isEmpty: Boolean get() = m == 0 && s == 0 && i.isEmpty() && n.isEmpty()
}

object Journal {
    const val KEY = "journal"
    val moods = listOf("😞", "🙁", "😐", "🙂", "😄")

    fun load(): List<JournalEntry> = runCatching {
        val a = org.json.JSONArray(Prefs.store.getString(KEY, "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> JournalEntry(o.getString("d"), o.optInt("m"), o.optInt("s"), o.optString("i"), o.optString("n")) } }
    }.getOrDefault(emptyList())

    fun save(list: List<JournalEntry>) {
        val kept = list.filter { !it.isEmpty }.sortedByDescending { it.d }.take(366)
        Prefs.store.edit().putString(KEY, org.json.JSONArray(kept.map {
            org.json.JSONObject().put("d", it.d).put("m", it.m).put("s", it.s).put("i", it.i).put("n", it.n)
        }).toString()).apply()
    }

    val todayKey: String get() = LocalDate.now().toString()
    fun today(): JournalEntry = load().firstOrNull { it.d == todayKey } ?: JournalEntry(todayKey)
    fun put(e: JournalEntry) = save(listOf(e) + load().filter { it.d != e.d })
}

/** Bugünün ruh hâli, uykusu ve niyeti (günaydın kartında ve günlük sayfasında) */
@Composable
fun JournalQuick(entry: JournalEntry, showSleep: Boolean = true, onChange: (JournalEntry) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(L("journal.mood"), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth()) {
            for (v in 1..5) Text(
                Journal.moods[v - 1],
                Modifier.weight(1f).alpha(if (entry.m == 0 || entry.m == v) 1f else 0.35f)
                    .clickable { onChange(entry.copy(m = if (entry.m == v) 0 else v)) }
                    .semantics { contentDescription = L("journal.level.$v"); selected = entry.m == v },
                fontSize = 32.sp, textAlign = TextAlign.Center,
            )
        }
        if (showSleep) {
            Text(L("journal.sleep"), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (v in 1..5) {
                    val on = entry.s == v
                    Text(
                        L("journal.level.$v"),
                        Modifier.weight(1f)
                            .background(if (on) Theme.c.accent else Theme.c.surface2, RoundedCornerShape(8.dp))
                            .clickable { onChange(entry.copy(s = if (on) 0 else v)) }
                            .padding(vertical = 8.dp),
                        color = if (on) Theme.c.bg else Theme.c.text, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1,
                    )
                }
            }
        }
        OutlinedTextField(entry.i, { onChange(entry.copy(i = it.take(120))) }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(L("journal.intention")) }, placeholder = { Text(L("journal.intentionHint")) })
    }
}

@Composable
fun JournalScreen() {
    var entry by remember { mutableStateOf(Journal.today()) }
    var list by remember { mutableStateOf(Journal.load()) }
    var confirm by remember { mutableStateOf(false) }
    fun update(e: JournalEntry) { entry = e; Journal.put(e); list = Journal.load() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(Fmt.longDate(LocalDate.now()), color = Theme.c.muted, fontSize = 14.sp)
        AppCard {
            JournalQuick(entry) { update(it) }
            Spacer(Modifier.padding(4.dp))
            OutlinedTextField(entry.n, { update(entry.copy(n = it.take(2000))) }, Modifier.fillMaxWidth().heightIn(min = 110.dp),
                label = { Text(L("journal.note")) })
        }
        SectionTitle(L("journal.recent"))
        AppCard {
            val byDay = list.associateBy { it.d }
            Ltr {
                Row(Modifier.fillMaxWidth()) {
                    for (back in 13 downTo 0) {
                        val day = LocalDate.now().minusDays(back.toLong())
                        val e = byDay[day.toString()]
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(e?.let { if (it.m > 0) Journal.moods[it.m - 1] else "•" } ?: "·", fontSize = 18.sp, color = Theme.c.muted)
                            Text("${day.dayOfMonth}", color = Theme.c.muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        SectionTitle(L("journal.history"))
        val older = list.filter { it.d != Journal.todayKey }
        AppCard {
            if (older.isEmpty()) Text(L("journal.empty"), color = Theme.c.muted)
            older.take(60).forEachIndexed { idx, e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(if (e.m > 0) Journal.moods[e.m - 1] else "·", fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(runCatching { Fmt.longDate(LocalDate.parse(e.d)) }.getOrDefault(e.d), color = Theme.c.muted, fontSize = 12.sp)
                        if (e.i.isNotEmpty()) Text(e.i, color = Theme.c.text, fontSize = 15.sp)
                        if (e.n.isNotEmpty()) Text(e.n, color = Theme.c.muted, fontSize = 13.sp, maxLines = 3)
                        if (e.s > 0) Text(T("journal.sleepShort", L("journal.level.${e.s}")), color = Theme.c.muted, fontSize = 12.sp)
                    }
                }
                if (idx < older.take(60).lastIndex) HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            }
        }
        Text(L("journal.privacyAndroid"), color = Theme.c.muted, fontSize = 13.sp)
        if (list.isNotEmpty()) TextButton(onClick = { confirm = true }) { Text(L("journal.clear"), color = androidx.compose.ui.graphics.Color(0xFFD32F2F)) }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(L("journal.clearConfirm")) },
        confirmButton = {
            TextButton(onClick = {
                Prefs.store.edit().remove(Journal.KEY).apply()
                list = emptyList(); entry = JournalEntry(Journal.todayKey); confirm = false
            }) { Text(L("journal.clear"), color = androidx.compose.ui.graphics.Color(0xFFD32F2F)) }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(L("alarm.cancel")) } },
    )
}

/** Günaydın kartındaki bölüm: ruh hâli, uyku ve niyet (tamamı Araçlar › Sabah günlüğü'nde) */
@Composable
fun JournalMorningCard() {
    var entry by remember { mutableStateOf(Journal.today()) }
    AppCard {
        Text(L("journal.title"), color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        JournalQuick(entry) { entry = it; Journal.put(it) }
    }
}
