package tr.bumba.time

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tr.bumba.time.core.Breath
import tr.bumba.time.core.FocusLog
import tr.bumba.time.core.FocusSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

// Odak engelinin zaman çizelgesi (2. analiz #27; iOS'taki FocusScheduleStore ile aynı anahtarlar)

object FocusScheduleStore {
    const val ON = "focus.sched"
    const val DAYS = "focus.sched.days"
    const val FROM = "focus.sched.from"
    const val TO = "focus.sched.to"

    val on: Boolean get() = Prefs.store.getBoolean(ON, false)
    val days: Set<Int> get() = Prefs.store.getString(DAYS, "1,2,3,4,5")!!.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    val from: Int get() = Prefs.store.getInt(FROM, 480)
    val to: Int get() = Prefs.store.getInt(TO, 1020)

    fun active(now: LocalDateTime = LocalDateTime.now()): Boolean =
        on && FocusSchedule.active(days, from, to, now.dayOfWeek.value, now.hour * 60 + now.minute)

    /** Çizelge açıksa sıradaki sınırda (başlangıç/bitiş) engeli yeniden değerlendiren alarm kurulur */
    fun scheduleNext(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(c, 3300, Intent(c, FocusScheduleReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (!on || !FocusBlock.on) { am.cancel(pi); return }
        val now = LocalDateTime.now()
        val mins = FocusSchedule.minutesToNextBoundary(from, to, now.hour * 60 + now.minute)
        val at = System.currentTimeMillis() - (now.second * 1000L + now.nano / 1_000_000) + mins * 60_000L + 2000
        am.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, pi)
    }
}

class FocusScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        FocusBlock.sync(c)
    }
}

@Composable
fun FocusScheduleSection(onChange: () -> Unit) {
    var on by remember { mutableStateOf(FocusScheduleStore.on) }
    var days by remember { mutableStateOf(FocusScheduleStore.days) }
    var from by remember { mutableIntStateOf(FocusScheduleStore.from) }
    var to by remember { mutableIntStateOf(FocusScheduleStore.to) }
    fun save() {
        Prefs.store.edit().putBoolean(FocusScheduleStore.ON, on).putString(FocusScheduleStore.DAYS, days.sorted().joinToString(","))
            .putInt(FocusScheduleStore.FROM, from).putInt(FocusScheduleStore.TO, to).apply()
        onChange()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(L("focus.sched"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        SoftSwitch(on, { on = it; save() }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
    }
    if (!on) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (d in 1..7) {
            val sel = d in days
            Box(Modifier.weight(1f).height(32.dp).background(if (sel) Theme.c.accent else Theme.c.surface2, RoundedCornerShape(8.dp))
                .clickable { days = if (sel) days - d else days + d; save() }, contentAlignment = Alignment.Center) {
                Text(L("calendar.weekdaysShort.${d - 1}"), color = if (sel) Theme.c.bg else Theme.c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    val context = LocalContext.current
    fun pick(start: Int, set: (Int) -> Unit) {
        android.app.TimePickerDialog(context, { _, h, m -> set(h * 60 + m); save() }, start / 60, start % 60, !Prefs.h12).show()
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { pick(from) { from = it } }) { Text(L("focus.schedFrom") + "  " + "%02d:%02d".format(from / 60, from % 60), color = Theme.c.accent) }
        TextButton(onClick = { pick(to) { to = it } }) { Text(L("focus.schedTo") + "  " + "%02d:%02d".format(to / 60, to % 60), color = Theme.c.accent) }
    }
    if (FocusScheduleStore.active()) Text("🔒 " + L("focus.schedOn"), color = Theme.c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

// Nefes egzersizi (2. analiz #28)

@Composable
fun BreathScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    var pattern by remember { mutableStateOf(Prefs.store.getString("breath.pattern", "478")!!) }
    var minutes by remember { mutableIntStateOf(Prefs.store.getInt("breath.minutes", 3)) }
    var start by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var finished by remember { mutableStateOf<Int?>(null) }
    var lastStep by remember { mutableIntStateOf(-1) }
    val steps = Breath.PATTERNS[pattern] ?: Breath.PATTERNS.getValue("478")
    LaunchedEffect(start) {
        while (start > 0) {
            now = System.currentTimeMillis()
            val el = (now - start) / 1000.0
            if (el >= minutes * 60) { finished = Breath.phase(steps, el).cycle; start = 0; break }
            delay(33)
        }
    }
    DisposableEffect(start) {
        view.keepScreenOn = start > 0
        onDispose { view.keepScreenOn = false }
    }
    val ph = Breath.phase(steps, if (start > 0) (now - start) / 1000.0 else 0.0)
    LaunchedEffect(ph.step, start) {
        if (start > 0 && ph.step != lastStep) {
            lastStep = ph.step
            context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(30, 80))
        }
    }
    val target = if (start > 0) 0.35f + 0.65f * Breath.scale(ph).toFloat() else 0.35f
    val scale by animateFloatAsState(target, label = "breath")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            val accent = Theme.c.accent
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(accent.copy(alpha = 0.12f), size.minDimension / 2 * 0.93f)
                drawCircle(accent.copy(alpha = 0.55f), size.minDimension / 2 * 0.93f * scale)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (start == 0L) L("breath.title") else L(listOf("breath.in", "breath.hold", "breath.out", "breath.hold")[ph.step]),
                    color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                if (start > 0) Text("${kotlin.math.ceil(ph.left).toInt()}", color = Theme.c.text, fontSize = 44.sp, fontWeight = FontWeight.Bold, style = Tnum)
            }
        }
        finished?.let { Text(T("breath.done", it), color = Theme.c.accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        RoundButton(L(if (start == 0L) "breath.start" else "breath.stop"), true, Modifier.fillMaxWidth()) {
            if (start == 0L) { start = System.currentTimeMillis(); finished = null; lastStep = -1 } else start = 0
        }
        AppCard {
            for (k in Breath.PATTERNS.keys) {
                Row(Modifier.fillMaxWidth().clickable(enabled = start == 0L) { pattern = k; Prefs.store.edit().putString("breath.pattern", k).apply() },
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(pattern == k, null, enabled = start == 0L, colors = RadioButtonDefaults.colors(selectedColor = Theme.c.accent))
                    Text(L("breath.pattern.$k"), color = Theme.c.text, fontSize = 15.sp)
                }
            }
            Stepper(L("breath.minutes") + " (" + L("client.minuteShort") + ")", minutes, 1..10, 1) {
                if (start == 0L) { minutes = it; Prefs.store.edit().putInt("breath.minutes", it).apply() }
            }
        }
        Text(L("breath.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

// Telefon yüzüstüyken odak (2. analiz #29)

/**
 * Pomodoro sayfası açıkken ivmeölçerle telefonun yüzüstü olup olmadığına bakar: çalışma sırası beklerken telefon 2 sn
 * yüzüstü kalırsa pomodoro başlar; çalışırken 3 sn kaldırılırsa durur ve titreşir. İzin istemez; seçim `pomo.flip`.
 */
@Composable
fun FlipFocusCard(onChange: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var on by remember { mutableStateOf(Prefs.store.getBoolean("pomo.flip", false)) }
    var paused by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(ToolState.long("pomo.end") > 0) }
    DisposableEffect(on) {
        view.keepScreenOn = on
        val sm = context.getSystemService(SensorManager::class.java)
        val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var down = false
        var since = System.currentTimeMillis()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // Ekran aşağı bakarken yerçekimi z ekseninde −9,8
                val d = e.values[2] < -7.8f
                val now = System.currentTimeMillis()
                if (d != down) { down = d; since = now }
                if (PomoEngine.phase() != "work") return
                val isRunning = ToolState.long("pomo.end") > 0
                if (down && !isRunning && now - since >= 2000) {
                    PomoEngine.start(context); paused = false; running = true; onChange()
                    context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(60, 120))
                } else if (!down && isRunning && now - since >= 3000) {
                    PomoEngine.pause(context); paused = true; running = false; onChange()
                    context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 120, 200), -1))
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (on && acc != null) sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose {
            sm?.unregisterListener(listener)
            view.keepScreenOn = false
        }
    }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("flip.toggle"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
            SoftSwitch(on, { on = it; Prefs.store.edit().putBoolean("pomo.flip", it).apply() }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
        }
        if (on && !running) Text(if (paused) "⚠️ " + L("flip.paused") else "📱 " + L("flip.waiting"),
            color = if (paused) androidx.compose.ui.graphics.Color(0xFFE65100) else Theme.c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(L("flip.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

// Odak istatistiği: 7 gün / 30 gün / 12 ay ve odak ormanı (2. analiz #31)

object FocusRange {
    val ALL = listOf("week", "month", "year")

    fun series(log: List<FocusLog.Entry>, range: String, today: LocalDate = LocalDate.now()): List<Pair<String, Int>> = when (range) {
        "year" -> {
            val sums = log.groupBy { it.d.take(7) }.mapValues { e -> e.value.sumOf { it.m } }
            (11 downTo 0).map { i ->
                val ym = YearMonth.from(today).minusMonths(i.toLong())
                L("calendar.months.${ym.monthValue - 1}").take(3) to (sums[ym.toString()] ?: 0)
            }
        }
        else -> {
            val n = if (range == "month") 30 else 7
            val sums = log.groupBy { it.d }.mapValues { e -> e.value.sumOf { it.m } }
            (n - 1 downTo 0).map { i ->
                val d = today.minusDays(i.toLong())
                (if (range == "week") L("calendar.weekdaysShort.${d.dayOfWeek.value - 1}") else d.dayOfMonth.toString()) to (sums[d.toString()] ?: 0)
            }
        }
    }
}

@Composable
fun FocusRangeChart(log: List<FocusLog.Entry>) {
    var range by remember { mutableStateOf(Prefs.store.getString("pomo.statsRange", "week")!!) }
    val s = FocusRange.series(log, range)
    val top = maxOf(1, s.maxOf { it.second })
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in FocusRange.ALL) {
            val on = r == range
            Box(Modifier.weight(1f).background(if (on) Theme.c.accent else Theme.c.surface2, RoundedCornerShape(50))
                .clickable { range = r; Prefs.store.edit().putString("pomo.statsRange", r).apply() }.padding(vertical = 6.dp),
                contentAlignment = Alignment.Center) {
                Text(L("stats.range.$r"), color = if (on) Theme.c.bg else Theme.c.text, fontSize = 13.sp)
            }
        }
    }
    Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(if (range == "month") 2.dp else 6.dp),
        verticalAlignment = Alignment.Bottom) {
        s.forEachIndexed { i, (label, m) ->
            Column(Modifier.weight(1f).fillMaxHeight().clearAndSetSemantics { contentDescription = "$label: $m ${L("tools.pomodoro.minUnit")}" },
                horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(maxOf(2f, 90f * m / top).dp)
                    .background(if (i == s.size - 1) Theme.c.accent else Theme.c.accent.copy(alpha = 0.55f), RoundedCornerShape(if (range == "month") 2.dp else 4.dp)))
                Spacer(Modifier.height(3.dp))
                Text(if (range != "month" || i % 5 == 4) label else " ", color = Theme.c.muted, fontSize = 9.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Bu ayın odak ormanı: biten her çalışma turu bir ağaç (25 dk altı fidan, 50 dk'ya kadar ağaç, daha uzunu ulu ağaç) */
@Composable
fun FocusForest(log: List<FocusLog.Entry>) {
    val month = YearMonth.now().toString()
    val trees = log.filter { it.d.startsWith(month) }.map { if (it.m < 25) "🌱" else if (it.m <= 50) "🌳" else "🌲" }
    AppCard {
        SectionTitle(L("forest.title"))
        if (trees.isEmpty()) Text(L("forest.empty"), color = Theme.c.muted, fontSize = 14.sp)
        else {
            Text(T("forest.count", trees.size), color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(trees.take(120).joinToString(""), fontSize = 24.sp, modifier = Modifier.clearAndSetSemantics { })
        }
        Text(L("forest.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}
