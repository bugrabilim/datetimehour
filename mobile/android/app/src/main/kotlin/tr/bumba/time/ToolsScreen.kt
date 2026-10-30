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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
        "alarm" -> Page(L("tools.hub.cards.alarm.name"), onRoute) { AlarmScreen() }
        "timer" -> Page(L("tools.timer"), onRoute) { TimerScreen() }
        "stopwatch" -> Page(L("tools.hub.cards.stopwatch.name"), onRoute) { Stopwatch() }
        "pomodoro" -> Page(L("tools.hub.cards.pomodoro.name"), onRoute) { PomodoroScreen() }
        "sleep" -> Page(L("sleep.title"), onRoute) { SleepScreen() }
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
    LaunchedEffect(start) { while (start > 0) { now = System.currentTimeMillis(); delay(30) } }
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
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (lapTimes.isEmpty()) Text(L("tools.stopwatch.empty"), color = Theme.c.muted)
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
        }
    }
}
