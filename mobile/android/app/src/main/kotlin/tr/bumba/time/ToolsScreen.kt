package tr.bumba.time

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
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
        "breath" -> Page(L("breath.title"), onRoute) { BreathScreen() }
        "study" -> Page(L("study.title"), onRoute) { StudyScreen() }
        "wakeGame" -> Page(L("wakegame.title"), onRoute) { WakeGameScreen() }
        "planner" -> Page(L("planner.h1"), onRoute) { MeetingPlannerScreen() }
        "datecalc" -> Page(L("tools.hub.cards.datecalc.name"), onRoute) { DateCalcScreen() }
        "settings" -> Page(L("settings.title"), onRoute) { SettingsScreen(onRoute) }
        "color" -> Page(L("settings.color"), { onRoute("settings") }) { ColorScreen() }
        "appearance" -> Page(L("look.title"), { onRoute("settings") }) { AppearanceScreen { onRoute(it) } }
        "dials" -> Page(L("dial.title"), { onRoute("appearance") }) { DialGalleryScreen() }
        "widgets" -> Page(L("widgets.gallery"), { onRoute("appearance") }) { WidgetGalleryScreen() }
        "homeItems" -> Page(L("home.customize"), { onRoute("settings") }) { HomeItemsEditor() }
        "features" -> Page(L("features.title"), { onRoute("settings") }) { FeaturesScreen() }
        "permissions" -> Page(L("perms.title"), { onRoute("settings") }) { PermissionsScreen() }
        "about" -> Page(L("settings.about"), onRoute) { AboutScreen() }
        "shareQr" -> Page(L("support.qr"), { onRoute("settings") }) { ShareQrScreen() }
        "language" -> Page(L("lang.title"), { onRoute("settings") }) { LanguageScreen() }
        "badges" -> Page(L("badges.h1"), onRoute) { BadgesScreen() }
        "notifications" -> Page(L("settings.notifications"), { onRoute("settings") }) { NotificationsScreen(onRoute) }
        "prayerNotify" -> {
            BackHandler { onRoute("notifications") }
            NotificationSettingsScreen { onRoute("notifications") }
        }
        // Tümü'nden açılan ibadet, Ay ve takvim yaprağı sayfaları (kendi geri düğmeleriyle)
        "qibla" -> QiblaScreen { onRoute(null) }
        "quran" -> QuranScreen { onRoute(null) }
        "duas" -> DuasScreen { onRoute(null) }
        "zikir" -> ZikirScreen { onRoute(null) }
        "track" -> TrackerScreen { onRoute(null) }
        "zakat" -> ZakatScreen { onRoute(null) }
        "reldays" -> ReligiousDaysScreen { onRoute(null) }
        "greet" -> GreetingScreen { onRoute(null) }
        "esma" -> EsmaScreen { onRoute(null) }
        "hadith" -> HadithScreen { onRoute(null) }
        "guide" -> PrayerGuideScreen({ onRoute(null) }) { onRoute("duas") }
        "leaf" -> CalendarLeafScreen { onRoute(null) }
        "moon" -> Page(L("moon.calendarLink"), onRoute) { MoonScreen() }
        "sky" -> Page(L("sky.h1"), onRoute) { SkyScreen() }
        "sunpath" -> Page(L("sunpath.title"), onRoute) { SunPathScreen() }
        // Fotoğraf ışığı (9 Kasım 2026): altın/mavi saat, gün doğumu ve batımı tahmini
        "photo" -> Page(L("photoApp.title"), onRoute) { PhotoLightScreen() }
        // Panchang ve Rahu Kaal (10 Kasım 2026, Hindistan araştırması #9)
        "panchang" -> Page(L("panchang.h1"), onRoute) { PanchangScreen() }
        // Sitedeki dokuz araç (4 Kasım 2026)
        in SITE_TOOL_TITLES.keys -> Page(L(SITE_TOOL_TITLES.getValue(route!!)), onRoute) { SiteToolScreen(route) }
        else -> if (route?.startsWith("quran:") == true) QuranScreen(route.removePrefix("quran:").toIntOrNull()) { onRoute(null) }
            else AllScreen(onRoute)
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
        IconBox(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Theme.c.text, fontSize = 16.sp)
            if (note != null) Text(note, color = Theme.c.muted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
    }
    if (!last) HorizontalDivider(Modifier.padding(start = 62.dp), color = Theme.c.border.copy(alpha = 0.5f))
}

@Composable
fun RoundButton(title: String, prominent: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) =
    RoundButton(title, prominent, null, modifier, onClick)

/** Simgeli ana/ikincil düğme: [icon] iki tonlu simgenin adı (8 Kasım 2026; metindeki emojinin yerine) */
@Composable
fun RoundButton(title: String, prominent: Boolean, icon: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val content = if (prominent) Theme.c.onAccent else Theme.c.text
    Button(
        onClick = onClick, modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (prominent) Theme.c.accent else Theme.c.surface2, contentColor = content),
    ) {
        if (icon != null) { DuoIcon(icon, 18.dp, content); androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp)) }
        Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    }
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

    // Tam ekran ve yatayda dev rakamlar (2. analiz #104)
    var full by remember { mutableStateOf(false) }
    if (full) StopwatchFullscreen({ fmt(total) }, running = start > 0, onLap = { save(start, acc, (laps + total).joinToString(",")) },
        onToggle = { if (start > 0) save(0, total, lapsRaw) else { now = System.currentTimeMillis(); save(now, acc, lapsRaw) } }) { full = false }

    // Araç kalıbı (2. analiz #200): altta tur/sıfırla (çerçeveli) ve başlat/durdur (tek dolu düğme); tam ekran, CSV ve kopyala "…" menüsünde
    val lapTimes = laps.mapIndexed { i, v -> v - (if (i > 0) laps[i - 1] else 0) }
    val best = if (lapTimes.size > 1) lapTimes.min() else null
    val worst = if (lapTimes.size > 1) lapTimes.max() else null
    // (context yukarıda)
    val menu = buildList<Pair<String, () -> Unit>> {
        add(L("clock.fullscreen") to { full = true })
        if (lapTimes.isNotEmpty()) {
            // Turları CSV olarak paylaş (rakip analizi #28)
            add(L("csv.laps") to {
                Share.csv(context, "laps-${java.time.LocalDate.now()}.csv",
                    listOf(listOf(L("tools.stopwatch.lapNo"), L("tools.stopwatch.lapTime"), L("tools.stopwatch.total"))) +
                        lapTimes.indices.map { listOf("${it + 1}", fmt(lapTimes[it]), fmt(laps[it])) }, L("csv.laps"))
            })
            // Turları metin olarak kopyala (2. analiz #104)
            add(L("sw.copyLaps") to {
                val text = lapTimes.indices.joinToString("\n") { "${L("tools.stopwatch.lapNo")} ${it + 1}\t${fmt(lapTimes[it])}\t${fmt(laps[it])}" }
                (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                    .setPrimaryClip(android.content.ClipData.newPlainText(L("sw.copyLaps"), text))
                android.widget.Toast.makeText(context, L("sw.copied"), android.widget.Toast.LENGTH_SHORT).show()
            })
        }
    }
    val running = start > 0
    ToolFrame(
        primary = L(if (running) "tools.stopwatch.stop" else if (total > 0) "tools.stopwatch.resume" else "tools.stopwatch.start"),
        onPrimary = {
            if (running) save(0, total, lapsRaw)
            else {
                now = System.currentTimeMillis()
                save(now, acc, lapsRaw)
                BadgeStore.hit(Badge.KRONOMETRE)
            }
        },
        secondary = if (running) L("tools.stopwatch.lap") else if (total > 0) L("tools.stopwatch.reset") else null,
        onSecondary = { if (running) save(start, acc, (laps + total).joinToString(",")) else save(0, 0, "") },
        menu = menu,
    ) {
        Spacer(Modifier.height(8.dp))
        BigTime(fmt(total))
        Column(Modifier.fillMaxWidth()) {
            if (lapTimes.isEmpty()) Text(L("tools.stopwatch.empty"), color = Theme.c.muted)
            for (i in lapTimes.indices.reversed()) {
                val lap = lapTimes[i]
                Ltr {
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${L("tools.stopwatch.lapNo")} ${i + 1}", color = Theme.c.text)
                        if (lap == best) Text("  " + L("tools.stopwatch.best"), color = Theme.c.ok, fontSize = 12.sp)
                        if (lap == worst) Text("  " + L("tools.stopwatch.worst"), color = Theme.c.err, fontSize = 12.sp)
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

/** Kronometre tam ekranı: siyah zemin, genişliğe sığan dev rakamlar (yatayda ekranı doldurur), tur ve dur/başlat; ekran açık kalır */
@Composable
private fun StopwatchFullscreen(text: () -> String, running: Boolean, onLap: () -> Unit, onToggle: () -> Unit, onClose: () -> Unit) {
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.FullscreenExit, L("common.done"), tint = Color.White) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Ltr {
                    androidx.compose.foundation.text.BasicText(text(), maxLines = 1,
                        style = Tnum.copy(color = Color.White, fontWeight = FontWeight.Light, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 400.sp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (running) RoundButton(L("tools.stopwatch.lap"), false, Modifier.weight(1f), onLap)
                RoundButton(L(if (running) "tools.stopwatch.stop" else "tools.stopwatch.resume"), true, Modifier.weight(1f), onToggle)
            }
        }
    }
}
