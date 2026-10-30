package tr.bumba.time

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Zamanlayıcı (süre) ve tarihe geri sayım (iOS'taki TimerView) */
@Composable
fun TimerScreen() {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
            SegmentedButton(mode == 0, { mode = 0 }, SegmentedButtonDefaults.itemShape(0, 2), colors = segColors()) { Text(L("tools.countdown.tabTimer")) }
            SegmentedButton(mode == 1, { mode = 1 }, SegmentedButtonDefaults.itemShape(1, 2), colors = segColors()) { Text(L("tools.countdown.tabDate")) }
        }
        if (mode == 0) DurationTimer() else DateCountdown()
    }
}

/** Kalıcı küçük durum (uygulama kapansa da sürer): iOS'taki tm.* / cd.* / pomo.* anahtarları */
internal object ToolState {
    fun long(k: String) = Prefs.store.getLong(k, 0L)
    fun putLong(k: String, v: Long) = Prefs.store.edit().putLong(k, v).apply()
    fun str(k: String, d: String = "") = Prefs.store.getString(k, d) ?: d
    fun putStr(k: String, v: String) = Prefs.store.edit().putString(k, v).apply()
    fun int(k: String, d: Int) = Prefs.store.getInt(k, d)
    fun putInt(k: String, v: Int) = Prefs.store.edit().putInt(k, v).apply()
}

/** Saniyelik süre: "1:02:03" ya da "02:03" */
internal fun clock(seconds: Long): String = Fmt.duration(seconds)

/** Bildirim izni (Android 13+): süre başlatılırken bir kez istenir */
@Composable
internal fun rememberNotifyAsk(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (Build.VERSION.SDK_INT >= 33 && !AlarmScheduler.canNotify(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun DurationTimer() {
    val context = LocalContext.current
    val ask = rememberNotifyAsk()
    // Çalışırken bitiş anı (ms), duraklatılmışken kalan (ms)
    var end by remember { mutableLongStateOf(ToolState.long("tm.end")) }
    var left by remember { mutableLongStateOf(ToolState.long("tm.left")) }
    var h by rememberSaveable { mutableIntStateOf(0) }
    var m by rememberSaveable { mutableIntStateOf(5) }
    var s by rememberSaveable { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) { while (end > 0) { now = System.currentTimeMillis(); delay(250) } }
    val running = end > 0
    val paused = end == 0L && left > 0
    val remaining = if (running) maxOf(0, end - now) else left

    fun save(e: Long, l: Long) { end = e; left = l; ToolState.putLong("tm.end", e); ToolState.putLong("tm.left", l) }
    fun start(ms: Long) {
        if (ms <= 0) return
        BadgeStore.hit(Badge.ZAMANLAYICI)
        ask()
        val e = System.currentTimeMillis() + ms
        now = System.currentTimeMillis()
        save(e, 0)
        AlarmScheduler.schedule(context, AlarmScheduler.TIMER, e, L("tools.countdown.finished"), AlarmScheduler.TIMER)
    }
    fun reset() { AlarmScheduler.cancel(context, AlarmScheduler.TIMER); save(0, 0) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (running || paused) {
            Spacer(Modifier.height(20.dp))
            BigTime(if (remaining <= 0) L("tools.countdown.finished") else clock((remaining + 999) / 1000))
        } else {
            Ltr {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Wheel(h, 0..23, L("tools.countdown.hoursShort")) { h = it }
                    Wheel(m, 0..59, L("tools.countdown.minutesShort")) { m = it }
                    Wheel(s, 0..59, L("tools.countdown.secondsShort")) { s = it }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in listOf(1, 3, 5, 10, 15, 25, 30, 45, 60)) {
                    Text(
                        T("tools.countdown.presetMin", n),
                        Modifier.border(1.dp, Theme.c.accent.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .clickable { h = n / 60; m = n % 60; s = 0; start(n * 60_000L) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Theme.c.accent, fontSize = 14.sp,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                running -> {
                    RoundButton(L("tools.countdown.reset"), false, Modifier.weight(1f)) { reset() }
                    RoundButton(L("tools.countdown.pause"), true, Modifier.weight(1f)) {
                        AlarmScheduler.cancel(context, AlarmScheduler.TIMER)
                        save(0, remaining)
                    }
                }
                paused -> {
                    RoundButton(L("tools.countdown.reset"), false, Modifier.weight(1f)) { reset() }
                    RoundButton(L("tools.countdown.resume"), true, Modifier.weight(1f)) { start(left) }
                }
                else -> RoundButton(L("tools.countdown.start"), true, Modifier.fillMaxWidth()) { start((h * 3600L + m * 60L + s) * 1000) }
            }
        }
        if (running && remaining <= 0) Text(L("tools.countdown.finished"), color = Theme.c.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(L("alarm.realNote"), color = Theme.c.muted, fontSize = 13.sp)
    }
}

/** Basit sayı çarkı: yukarı/aşağı oklarla (basılı tutmadan) */
@Composable
private fun Wheel(value: Int, range: IntRange, unit: String, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onChange(if (value >= range.last) range.first else value + 1) }) {
            Icon(Icons.Filled.KeyboardArrowUp, null, tint = Theme.c.accent)
        }
        Text("%02d".format(value), color = Theme.c.text, fontSize = 44.sp, fontWeight = FontWeight.SemiBold, style = Tnum)
        Text(unit, color = Theme.c.muted, fontSize = 13.sp)
        IconButton(onClick = { onChange(if (value <= range.first) range.last else value - 1) }) {
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Theme.c.accent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateCountdown() {
    var target by remember { mutableLongStateOf(ToolState.long("cd.target")) }
    var title by remember { mutableStateOf(ToolState.str("cd.title")) }
    var pickedDay by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var pickedTime by rememberSaveable { mutableStateOf("09:00") }
    var picking by remember { mutableIntStateOf(0) } // 1 tarih, 2 saat
    val now = rememberNow()
    val today = LocalDate.now()
    fun set(ms: Long, t: String) { target = ms; title = t; ToolState.putLong("cd.target", ms); ToolState.putStr("cd.title", t) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (target > 0) {
            val left = (target - now.toEpochMilli() + 999) / 1000
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (title.isNotEmpty()) Text(title, color = Theme.c.accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                BigTime(if (left <= 0) L("tools.countdown.finishedDate") else daysCountdown(left))
                val at = Instant.ofEpochMilli(target).atZone(ZoneId.systemDefault())
                Text(Fmt.longDate(at.toLocalDate()) + " " + Fmt.time(at), color = Theme.c.muted, fontSize = 14.sp)
            }
        }
        AppCard {
            SectionTitle(L("tools.countdown.targetLabel"))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(Fmt.longDate(LocalDate.parse(pickedDay)), Modifier.weight(1f).clickable { picking = 1 }.padding(vertical = 10.dp),
                    color = Theme.c.accent, fontSize = 16.sp)
                Ltr {
                    Text(pickedTime, Modifier.clickable { picking = 2 }.padding(10.dp), color = Theme.c.accent, fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, style = Tnum)
                }
            }
            RoundButton(L("tools.countdown.targetSet"), true, Modifier.fillMaxWidth()) {
                val at = LocalDateTime.of(LocalDate.parse(pickedDay), LocalTime.parse(pickedTime)).atZone(ZoneId.systemDefault())
                set(at.toInstant().toEpochMilli(), "")
            }
        }
        SectionTitle(L("tools.countdown.quickHeading"))
        for ((name, day) in quickTargets(today)) {
            Row(
                Modifier.fillMaxWidth().background(Theme.c.surface, RoundedCornerShape(14.dp))
                    .border(1.dp, Theme.c.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { set(day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), name) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, Modifier.weight(1f), color = Theme.c.text)
                Text(T("client.daysLeft", ChronoUnit.DAYS.between(today, day)), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (picking == 1) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(pickedDay).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = 0 },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { pickedDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    picking = 0
                }) { Text(L("common.done")) }
            },
        ) { DatePicker(state) }
    } else if (picking == 2) {
        val t = LocalTime.parse(pickedTime)
        val state = rememberTimePickerState(t.hour, t.minute, is24Hour = !Prefs.h12)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { picking = 0 },
            confirmButton = {
                TextButton(onClick = { pickedTime = "%02d:%02d".format(state.hour, state.minute); picking = 0 }) { Text(L("common.done")) }
            },
            text = { Ltr { TimePicker(state) } },
        )
    }
}

/** "12 gün 03:04:05" */
private fun daysCountdown(seconds: Long): String {
    val d = seconds / 86400
    val rest = Fmt.countdown(seconds % 86400)
    return if (d > 0) "$d ${L("tools.countdown.days")} $rest" else rest
}

/** Yeni yıl, Ramazan, bayramlar, 23 Nisan, 19 Mayıs, 30 Ağustos, 29 Ekim: her birinin sıradaki tarihi (iOS ile aynı) */
private fun quickTargets(today: LocalDate): List<Pair<String, LocalDate>> {
    val keys = listOf("newYear", "ramadanStart", "rb1", "kb1", "apr23", "may19", "aug30", "oct29")
    val all = AppData.events.entries.flatMap { (d, list) -> list.filter { it.date == d } }.sortedBy { it.date }
    return keys.mapNotNull { k ->
        all.firstOrNull { it.key == k && it.date.isAfter(today) }?.let { e ->
            (if (k == "newYear") L("tools.countdown.quickNewYear") else e.name) to e.date
        }
    }.sortedBy { it.second }
}

/** Bölmeli düğmelerde seçili olan uygulama renginde */
@Composable
internal fun segColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = Theme.c.accentSoft, activeContentColor = Theme.c.accent, activeBorderColor = Theme.c.border,
    inactiveContainerColor = Theme.c.surface, inactiveContentColor = Theme.c.text, inactiveBorderColor = Theme.c.border,
)

/** Büyük süre göstergesi */
@Composable
internal fun BigTime(text: String) {
    Ltr {
        Text(text, Modifier.fillMaxWidth(), color = Theme.c.text, fontSize = if (text.length > 9) 44.sp else 60.sp,
            fontWeight = FontWeight.SemiBold, style = Tnum, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
    }
}
