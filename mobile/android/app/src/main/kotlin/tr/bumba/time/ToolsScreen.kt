package tr.bumba.time

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Araçlar: kronometre, zamanlayıcı, tarih hesaplama; altta ayarlar ve hakkında (iOS'taki sırayla) */
@Composable
fun ToolsScreen(route: String?, onRoute: (String?) -> Unit) {
    when (route) {
        "stopwatch" -> Page(L("tools.hub.cards.stopwatch.name"), onRoute) { Stopwatch() }
        "timer" -> Page(L("tools.timer"), onRoute) { TimerTool() }
        "datecalc" -> Page(L("tools.hub.cards.datecalc.name"), onRoute) { DateCalc() }
        "settings" -> Page(L("settings.title"), onRoute) { SettingsScreen(onRoute) }
        "color" -> Page(L("settings.color"), { onRoute("settings") }) { ColorScreen() }
        "homeItems" -> Page(L("home.customize"), { onRoute("settings") }) { HomeItemsEditor() }
        "about" -> Page(L("settings.about"), onRoute) { AboutScreen() }
        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(L("tab.tools"), color = Theme.c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            AppCard(padding = 4) {
                ToolRow(Icons.Filled.Timer, L("tools.hub.cards.stopwatch.name")) { onRoute("stopwatch") }
                ToolRow(Icons.Filled.HourglassBottom, L("tools.timer")) { onRoute("timer") }
                ToolRow(Icons.Filled.DateRange, L("tools.hub.cards.datecalc.name"), last = true) { onRoute("datecalc") }
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
private fun ToolRow(icon: ImageVector, title: String, last: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Theme.c.accent)
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
    }
    if (!last) HorizontalDivider(Modifier.padding(start = 50.dp), color = Theme.c.border.copy(alpha = 0.5f))
}

@Composable
private fun BigTime(text: String) {
    Text(text, Modifier.fillMaxWidth(), color = Theme.c.text, fontSize = 60.sp, fontWeight = FontWeight.SemiBold,
        style = Tnum, textAlign = TextAlign.Center, maxLines = 1)
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

/** Kronometre: SystemClock.elapsedRealtime ile (cihaz saati değişse etkilenmez); turlar */
@Composable
private fun Stopwatch() {
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var acc by rememberSaveable { mutableLongStateOf(0L) }
    var running by rememberSaveable { mutableStateOf(false) }
    val laps = remember { mutableStateListOf<Long>() }
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) { while (running) { tick = SystemClock.elapsedRealtime(); delay(30) } }
    val total = acc + if (running) tick.coerceAtLeast(startedAt) - startedAt else 0
    fun fmt(ms: Long) = "%02d:%02d.%02d".format(ms / 60000, (ms / 1000) % 60, (ms / 10) % 100)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        BigTime(fmt(total))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundButton(L(if (running) "tools.stopwatch.lap" else "tools.stopwatch.reset"), false, Modifier.weight(1f)) {
                if (running) laps.add(0, total) else { acc = 0; laps.clear() }
            }
            RoundButton(L(if (running) "tools.stopwatch.stop" else if (acc > 0) "tools.stopwatch.resume" else "tools.stopwatch.start"), true, Modifier.weight(1f)) {
                if (running) { acc = total; running = false } else { startedAt = SystemClock.elapsedRealtime(); tick = startedAt; running = true }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            laps.forEachIndexed { i, t ->
                val prev = laps.getOrNull(i + 1) ?: 0
                Row2("${L("tools.stopwatch.lapNo")} ${laps.size - i}", "${fmt(t - prev)}   ${fmt(t)}")
                HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            }
        }
    }
}

/** Zamanlayıcı: süre seçilir, geri sayar; bitince titreşim/ses yerine ekranda uyarı (uygulama açıkken) */
@Composable
private fun TimerTool() {
    var minutes by rememberSaveable { mutableStateOf("5") }
    var endAt by rememberSaveable { mutableLongStateOf(0L) }
    var left by remember { mutableLongStateOf(0L) }
    LaunchedEffect(endAt) {
        while (endAt > 0) {
            left = ((endAt - SystemClock.elapsedRealtime()) / 1000).coerceAtLeast(0)
            if (left == 0L) break
            delay(200)
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        if (endAt > 0) {
            BigTime(Fmt.countdown(left))
            if (left == 0L) Text(L("tools.countdown.finished"), color = Theme.c.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            RoundButton(L("tools.countdown.reset"), false, Modifier.fillMaxWidth()) { endAt = 0 }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (m in listOf(1, 5, 10, 15, 25, 30)) {
                    Box(Modifier.background(Theme.c.surface2, RoundedCornerShape(10.dp)).clickable { minutes = m.toString() }.padding(10.dp)) {
                        Text("$m", color = Theme.c.text)
                    }
                }
            }
            OutlinedTextField(
                minutes, { minutes = it.filter(Char::isDigit).take(4) },
                label = { Text(L("tools.countdown.minutes")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
            )
            RoundButton(L("tools.countdown.start"), true, Modifier.fillMaxWidth()) {
                val m = minutes.toLongOrNull() ?: 0
                if (m > 0) endAt = SystemClock.elapsedRealtime() + m * 60_000
            }
        }
    }
}

/** Tarih hesaplama: iki tarih arası gün ve hafta */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateCalc() {
    var a by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var b by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(30).toString()) }
    var picking by remember { mutableStateOf<Int?>(null) }
    val from = LocalDate.parse(a)
    val to = LocalDate.parse(b)
    val days = ChronoUnit.DAYS.between(from, to)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppCard {
            SectionTitle(L("datecalc.diffHeading"))
            Row2(L("datecalc.startLabel"), Fmt.shortDate(from), Modifier.clickable { picking = 0 }, color = Theme.c.accent)
            Row2(L("datecalc.endLabel"), Fmt.shortDate(to), Modifier.clickable { picking = 1 }, color = Theme.c.accent)
            HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            Text(T("datecalc.diffDays", days), color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(T("datecalc.diffWeeks", days / 7, days % 7), color = Theme.c.muted)
        }
    }
    picking?.let { which ->
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (if (which == 0) from else to).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        if (which == 0) a = d else b = d
                    }
                    picking = null
                }) { Text(L("common.done")) }
            },
        ) { DatePicker(state) }
    }
}
