package tr.bumba.time

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Araçlar (iOS'taki ToolsTab): alarm, zamanlayıcı, kronometre, pomodoro, tarih hesaplama;
 * altta ayarlar ve hakkında (gizlilik ve e-posta listesi Hakkında sayfasında).
 */
@Composable
fun ToolsScreen(route: String?, onRoute: (String?) -> Unit) {
    when (route) {
        "alarm" -> Page(L("tools.hub.cards.alarm.name"), onRoute) { AlarmScreen { onRoute("geo") } }
        "geo" -> Page(L("geo.title"), { onRoute("alarm") }) { GeoAlarmsScreen() }
        "timer" -> Page(L("tools.timer"), onRoute) { TimerScreen() }
        "stopwatch" -> Page(L("tools.hub.cards.stopwatch.name"), onRoute) { Stopwatch() }
        "pomodoro" -> Page(L("tools.hub.cards.pomodoro.name"), onRoute) { PomodoroScreen() }
        "sleep" -> Page(L("sleep.title"), onRoute) { SleepScreen() }
        "journal" -> Page(L("journal.title"), onRoute) { JournalScreen() }
        "planner" -> Page(L("planner.h1"), onRoute) { MeetingPlannerScreen() }
        "datecalc" -> Page(L("tools.hub.cards.datecalc.name"), onRoute) { DateCalcScreen() }
        "settings" -> Page(L("settings.title"), onRoute) { SettingsScreen(onRoute) }
        "color" -> Page(L("settings.color"), { onRoute("settings") }) { ColorScreen() }
        "appearance" -> Page(L("look.title"), { onRoute("settings") }) { AppearanceScreen() }
        "homeItems" -> Page(L("home.customize"), { onRoute("settings") }) { HomeItemsEditor() }
        "features" -> Page(L("features.title"), { onRoute("settings") }) { FeaturesScreen() }
        "about" -> Page(L("settings.about"), onRoute) { AboutScreen() }
        "badges" -> Page(L("badges.h1"), onRoute) { BadgesScreen() }
        "notifications" -> Page(L("settings.notifications"), { onRoute("settings") }) { NotificationsScreen(onRoute) }
        "prayerNotify" -> {
            BackHandler { onRoute("notifications") }
            NotificationSettingsScreen { onRoute("notifications") }
        }
        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(L("tab.tools"), color = Theme.c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            AppCard(padding = 4) {
                ToolRow(Icons.Filled.Alarm, L("tools.hub.cards.alarm.name")) { onRoute("alarm") }
                ToolRow(Icons.Filled.HourglassBottom, L("tools.timer")) { onRoute("timer") }
                ToolRow(Icons.Filled.Timer, L("tools.hub.cards.stopwatch.name")) { onRoute("stopwatch") }
                ToolRow(Icons.Filled.Spa, L("tools.hub.cards.pomodoro.name")) { onRoute("pomodoro") }
                ToolRow(Icons.Filled.Bedtime, L("sleep.title")) { onRoute("sleep") }
                ToolRow(Icons.Filled.WbTwilight, L("journal.title")) { onRoute("journal") }
                ToolRow(Icons.Filled.Groups, L("planner.h1")) { onRoute("planner") }
                ToolRow(Icons.Filled.EditCalendar, L("tools.hub.cards.datecalc.name"), last = true) { onRoute("datecalc") }
            }
            // Rozetler: girişin yanında bir satırlık not (Bumba standardı madde 13)
            AppCard(padding = 4) {
                ToolRow(Icons.Filled.MilitaryTech, L("badges.h1"), last = true, note = L("badges.note")) { onRoute("badges") }
            }
            AppCard(padding = 4) {
                ToolRow(Icons.Filled.Settings, L("settings.title")) { onRoute("settings") }
                ToolRow(Icons.Filled.Info, L("settings.about"), last = true) { onRoute("about") }
            }
        }
    }
}

@Composable
private fun Page(title: String, onRoute: (String?) -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubBar(title, onBack = { onRoute(null) })
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ToolRow(icon: ImageVector, title: String, last: Boolean = false, note: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Theme.c.accent)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Theme.c.text, fontSize = 16.sp)
            if (note != null) Text(note, color = Theme.c.muted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
    }
    if (!last) HorizontalDivider(Modifier.padding(start = 50.dp), color = Theme.c.border.copy(alpha = 0.5f))
}

@Composable
fun RoundButton(title: String, prominent: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (prominent) Theme.c.accent else Theme.c.surface2,
            contentColor = if (prominent) Theme.c.bg else Theme.c.text,
        ),
    ) { Text(title, fontWeight = FontWeight.SemiBold) }
}

/** Ek kronometreler (rakip analizi listesi #81): her biri ayrı sayar; kayıt `stopwatches` (JSON, Unix ms; iOS'la aynı) */
data class StopwatchItem(val id: String = java.util.UUID.randomUUID().toString(), val name: String = "", val start: Long = 0, val acc: Long = 0) {
    fun elapsed(now: Long) = acc + if (start > 0) maxOf(0, now - start) else 0

    companion object {
        private const val KEY = "stopwatches"
        fun load(): List<StopwatchItem> = runCatching {
            val a = org.json.JSONArray(Prefs.store.getString(KEY, "[]"))
            (0 until a.length()).map { a.getJSONObject(it).let { o -> StopwatchItem(o.getString("id"), o.optString("name"), o.optLong("start"), o.optLong("acc")) } }
        }.getOrDefault(emptyList())
        fun save(list: List<StopwatchItem>) {
            Prefs.store.edit().putString(KEY, org.json.JSONArray(list.map {
                org.json.JSONObject().put("id", it.id).put("name", it.name).put("start", it.start).put("acc", it.acc)
            }).toString()).apply()
        }
    }
}

/**
 * Kronometre (iOS'taki StopwatchView): başlangıç anı, toplam ve turlar kalıcı (sw.start, sw.acc, sw.laps);
 * uygulama kapansa da süre kaybolmaz. En iyi ve en yavaş tur işaretlenir.
 */
@Composable
private fun Stopwatch() {
    var start by remember { mutableLongStateOf(ToolState.long("sw.start")) }
    var acc by remember { mutableLongStateOf(ToolState.long("sw.acc")) }
    var lapsRaw by remember { mutableStateOf(ToolState.str("sw.laps")) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var extra by remember { mutableStateOf(StopwatchItem.load()) }
    val anyRunning = start > 0 || extra.any { it.start > 0 }
    LaunchedEffect(anyRunning) { while (anyRunning) { now = System.currentTimeMillis(); delay(30) } }
    fun saveExtra(list: List<StopwatchItem>) { extra = list; StopwatchItem.save(list) }
    val total = acc + if (start > 0) maxOf(0, now - start) else 0
    val laps = lapsRaw.split(",").mapNotNull { it.toLongOrNull() }
    val context = androidx.compose.ui.platform.LocalContext.current
    fun save(s: Long, a: Long, l: String) {
        start = s; acc = a; lapsRaw = l
        ToolState.putLong("sw.start", s); ToolState.putLong("sw.acc", a); ToolState.putStr("sw.laps", l)
        LiveNotifications.sync(context)
    }
    fun fmt(ms: Long) = clock(ms / 1000) + ".%02d".format((ms / 10) % 100)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        BigTime(fmt(total))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (start > 0) {
                RoundButton(L("tools.stopwatch.lap"), false, Modifier.weight(1f)) { save(start, acc, (laps + total).joinToString(",")) }
                RoundButton(L("tools.stopwatch.stop"), true, Modifier.weight(1f)) { save(0, total, lapsRaw) }
            } else {
                RoundButton(L("tools.stopwatch.reset"), false, Modifier.weight(1f)) { save(0, 0, "") }
                RoundButton(L(if (total > 0) "tools.stopwatch.resume" else "tools.stopwatch.start"), true, Modifier.weight(1f)) {
                    now = System.currentTimeMillis()
                    save(now, acc, lapsRaw)
                    BadgeStore.hit(Badge.KRONOMETRE)
                }
            }
        }
        val lapTimes = laps.mapIndexed { i, v -> v - (if (i > 0) laps[i - 1] else 0) }
        val best = if (lapTimes.size > 1) lapTimes.min() else null
        val worst = if (lapTimes.size > 1) lapTimes.max() else null
        val context = androidx.compose.ui.platform.LocalContext.current
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (lapTimes.isEmpty()) Text(L("tools.stopwatch.empty"), color = Theme.c.muted)
            // Turları CSV olarak paylaş (rakip analizi #28)
            else androidx.compose.material3.TextButton(onClick = {
                Share.csv(context, "laps-${java.time.LocalDate.now()}.csv",
                    listOf(listOf(L("tools.stopwatch.lapNo"), L("tools.stopwatch.lapTime"), L("tools.stopwatch.total"))) +
                        lapTimes.indices.map { listOf("${it + 1}", fmt(lapTimes[it]), fmt(laps[it])) }, L("csv.laps"))
            }) { Text(L("csv.laps"), color = Theme.c.accent) }
            for (i in lapTimes.indices.reversed()) {
                val lap = lapTimes[i]
                Ltr {
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${L("tools.stopwatch.lapNo")} ${i + 1}", color = Theme.c.text)
                        if (lap == best) Text("  " + L("tools.stopwatch.best"), color = Color(0xFF2E7D32), fontSize = 12.sp)
                        if (lap == worst) Text("  " + L("tools.stopwatch.worst"), color = Color(0xFFD32F2F), fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(fmt(lap), color = Theme.c.text, style = Tnum)
                        Text("   " + fmt(laps[i]), color = Theme.c.muted, style = Tnum)
                    }
                }
                HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            }
            // Ek kronometreler
            Spacer(Modifier.height(16.dp))
            SectionTitle(L("sw.multi.heading"))
            extra.forEachIndexed { i, w ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        androidx.compose.foundation.text.BasicTextField(w.name, { v -> saveExtra(extra.map { if (it.id == w.id) it.copy(name = v.take(30)) else it }) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Theme.c.muted, fontSize = 13.sp), singleLine = true,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Theme.c.accent),
                            decorationBox = { inner -> if (w.name.isEmpty()) Text(T("sw.multi.default", i + 2), color = Theme.c.muted, fontSize = 13.sp); inner() })
                        Ltr { Text(fmt(w.elapsed(now)), color = Theme.c.text, fontSize = 28.sp, fontWeight = FontWeight.Light, style = Tnum) }
                    }
                    if (w.start == 0L && w.acc > 0) IconButton(onClick = { saveExtra(extra.map { if (it.id == w.id) it.copy(acc = 0) else it }) }) {
                        Icon(Icons.Filled.Refresh, L("tools.stopwatch.reset"), tint = Theme.c.accent)
                    }
                    IconButton(onClick = {
                        val t = System.currentTimeMillis()
                        now = t
                        saveExtra(extra.map { if (it.id != w.id) it else if (it.start > 0) it.copy(acc = it.elapsed(t), start = 0) else it.copy(start = t) })
                    }) {
                        Icon(if (w.start > 0) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            L(if (w.start > 0) "tools.stopwatch.stop" else "tools.stopwatch.start"), tint = Theme.c.accent)
                    }
                    IconButton(onClick = { saveExtra(extra.filter { it.id != w.id }) }) { Icon(Icons.Filled.Close, L("timers.remove"), tint = Theme.c.muted) }
                }
                HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            }
            if (extra.size < 10) androidx.compose.material3.TextButton(onClick = {
                now = System.currentTimeMillis()
                saveExtra(extra + StopwatchItem(start = now))
            }) { Text("+ " + L("sw.multi.add"), color = Theme.c.accent) }
            Text(L("sw.multi.note"), color = Theme.c.muted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
}
