package tr.bumba.time.wear

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/*
 * Saatte zaman araçları (2. analiz #137; 4 Kasım 2026): zamanlayıcı (5/10/15/20/30 dk), kronometre, pomodoro (25/5),
 * dünya saati (6 büyük şehir) ve zikirmatik. Süre bitince saat kendi bildirimini titreşimle verir (uygulama kapalıyken
 * de; tam zamanlı alarm izni yoksa birkaç dakikalık pencereyle). Kayıtlar yalnız saatte ("wear" tercihleri).
 * Telefonun alarmları zaten saate yansıdığı için saatte ayrı alarm listesi yok.
 */
object WearTools {
    private const val CHANNEL = "tools"

    fun schedule(c: Context, code: Int, atMs: Long, title: String) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(c, code, Intent(c, WearToolReceiver::class.java).putExtra("title", title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, atMs, 60_000L, pi)
    }

    fun cancel(c: Context, code: Int) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(c, code, Intent(c, WearToolReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        am.cancel(pi)
    }

    fun notify(c: Context, title: String) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, Wear.text("watch.tools"), NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
        })
        val open = PendingIntent.getActivity(c, 0, Intent(c, WearActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = android.app.Notification.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { nm.notify(9100, n) }
        buzz(c, strong = true)
    }

    fun buzz(c: Context, strong: Boolean = false) {
        runCatching {
            val v = c.getSystemService(Vibrator::class.java) ?: return
            v.vibrate(if (strong) VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1)
                else VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }

    fun minutes(m: Int) = Wear.text("watch.minutes", m)
}

class WearToolReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        Wear.init(c)
        WearTools.notify(c, intent.getStringExtra("title") ?: Wear.text("watch.done"))
    }
}

@Composable
private fun rememberTick(ms: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ms) { while (true) { now = System.currentTimeMillis(); delay(ms - now % ms) } }
    return now
}

private fun clock(sec: Long): String {
    val s = maxOf(0L, sec)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
}

/** Araçlar listesi ve seçilen araç */
@Composable
fun WearToolsScreen(onBack: () -> Unit) {
    var tool by remember { mutableStateOf<String?>(null) }
    // Bildirim izni (Wear OS 4+): araçlar ilk açılınca bir kez
    val ctx = LocalContext.current
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED && !Wear.prefs.getBoolean("w.notifyAsked", false)) {
            Wear.prefs.edit().putBoolean("w.notifyAsked", true).apply()
            ask.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    androidx.activity.compose.BackHandler { if (tool != null) tool = null else onBack() }
    when (tool) {
        "timer" -> TimerTool()
        "stopwatch" -> StopwatchTool()
        "pomodoro" -> PomodoroTool()
        "world" -> WorldTool()
        "zikir" -> ZikirTool()
        else -> {
            val state = rememberScalingLazyListState()
            Scaffold(positionIndicator = { PositionIndicator(state) }) {
                ScalingLazyColumn(Modifier.fillMaxWidth(), state = state) {
                    item { Text(Wear.text("watch.tools"), color = WColors.muted, fontSize = 13.sp) }
                    for ((k, key) in listOf("timer" to "watch.timer", "stopwatch" to "watch.stopwatch", "pomodoro" to "watch.pomodoro",
                        "world" to "watch.world", "zikir" to "watch.zikir")) item {
                        Chip(onClick = { tool = k }, label = { Text(Wear.text(key), maxLines = 1) },
                            colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerTool() {
    val c = LocalContext.current
    val p = Wear.prefs
    var end by remember { mutableLongStateOf(p.getLong("w.timer.end", 0L)) }
    val now = rememberTick()
    var buzzed by remember { mutableStateOf(false) }
    val state = rememberScalingLazyListState()
    Scaffold(positionIndicator = { PositionIndicator(state) }) {
        ScalingLazyColumn(Modifier.fillMaxWidth(), state = state, horizontalAlignment = Alignment.CenterHorizontally) {
            item { Text(Wear.text("watch.timer"), color = WColors.muted, fontSize = 13.sp) }
            if (end > now) {
                item { Text(clock((end - now + 999) / 1000), color = WColors.accent, fontSize = 36.sp, fontWeight = FontWeight.Bold) }
                item {
                    Chip(onClick = { end = 0; p.edit().putLong("w.timer.end", 0).apply(); WearTools.cancel(c, 9001) },
                        label = { Text(Wear.text("watch.reset")) }, colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
                }
            } else {
                if (end > 0) item {
                    if (!buzzed) LaunchedEffect(end) { buzzed = true; WearTools.buzz(c, strong = true) }
                    Text(Wear.text("watch.done"), color = WColors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                for (m in listOf(5, 10, 15, 20, 30)) item {
                    Chip(onClick = {
                        end = System.currentTimeMillis() + m * 60_000L
                        buzzed = false
                        p.edit().putLong("w.timer.end", end).apply()
                        WearTools.schedule(c, 9001, end, Wear.text("watch.done"))
                    }, label = { Text(WearTools.minutes(m)) }, colors = ChipDefaults.primaryChipColors(backgroundColor = WColors.accent),
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun StopwatchTool() {
    val c = LocalContext.current
    val p = Wear.prefs
    var start by remember { mutableLongStateOf(p.getLong("w.sw.start", 0L)) }
    var acc by remember { mutableLongStateOf(p.getLong("w.sw.acc", 0L)) }
    val now = rememberTick(100)
    val elapsed = acc + if (start > 0) now - start else 0
    fun save() = p.edit().putLong("w.sw.start", start).putLong("w.sw.acc", acc).apply()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Wear.text("watch.stopwatch"), color = WColors.muted, fontSize = 13.sp)
        val cs = elapsed / 100 % 10
        Text(clock(elapsed / 1000) + "." + cs, color = WColors.text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(onClick = { start = 0; acc = 0; save() }, label = { Text(Wear.text("watch.reset")) }, colors = ChipDefaults.secondaryChipColors())
            Chip(onClick = {
                val t = System.currentTimeMillis()
                if (start > 0) { acc += t - start; start = 0 } else start = t
                save(); WearTools.buzz(c)
            }, label = { Text(if (start > 0) Wear.text("watch.pause") else Wear.text("watch.start")) },
                colors = ChipDefaults.primaryChipColors(backgroundColor = WColors.accent))
        }
    }
}

@Composable
private fun PomodoroTool() {
    val c = LocalContext.current
    val p = Wear.prefs
    var end by remember { mutableLongStateOf(p.getLong("w.pomo.end", 0L)) }
    var phase by remember { mutableStateOf(p.getString("w.pomo.phase", "work") ?: "work") }
    var done by remember { mutableIntStateOf(p.getInt("w.pomo.done", 0)) }
    val now = rememberTick()
    val minutes = if (phase == "work") 25 else 5
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Wear.text(if (phase == "work") "watch.work" else "watch.break"), color = WColors.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(if (end > now) clock((end - now + 999) / 1000) else WearTools.minutes(minutes), color = WColors.text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Chip(onClick = {
            if (end > now) { end = 0; WearTools.cancel(c, 9002) } else {
                if (end > 0) { if (phase == "work") done++; phase = if (phase == "work") "break" else "work" }
                end = System.currentTimeMillis() + (if (phase == "work") 25 else 5) * 60_000L
                WearTools.schedule(c, 9002, end, Wear.text(if (phase == "work") "desk.pomoDone" else "desk.breakDone"))
            }
            p.edit().putLong("w.pomo.end", end).putString("w.pomo.phase", phase).putInt("w.pomo.done", done).apply()
        }, label = { Text(if (end > now) Wear.text("watch.reset") else Wear.text("watch.start")) },
            colors = ChipDefaults.primaryChipColors(backgroundColor = WColors.accent), modifier = Modifier.padding(top = 8.dp))
        Text("🍅 $done", color = WColors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun WorldTool() {
    val now = Instant.ofEpochMilli(rememberTick(60_000))
    val zones = listOf("Europe/London", "America/New_York", "Asia/Tokyo", "Asia/Dubai", "Europe/Berlin", "Australia/Sydney", "Europe/Istanbul")
    val state = rememberScalingLazyListState()
    Scaffold(positionIndicator = { PositionIndicator(state) }) {
        ScalingLazyColumn(Modifier.fillMaxWidth(), state = state) {
            item { Text(Wear.text("watch.world"), color = WColors.muted, fontSize = 13.sp) }
            for (id in zones) item {
                val z = now.atZone(ZoneId.of(id))
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(id.substringAfterLast('/').replace('_', ' '), color = WColors.text, fontSize = 14.sp, maxLines = 1)
                    Text("%02d:%02d".format(z.hour, z.minute), color = WColors.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ZikirTool() {
    val c = LocalContext.current
    val p = Wear.prefs
    var count by remember { mutableIntStateOf(p.getInt("w.zikir.count", 0)) }
    val target = 33
    Column(Modifier.fillMaxSize().clickable {
        count++
        p.edit().putInt("w.zikir.count", count).apply()
        WearTools.buzz(c, strong = count % target == 0)
    }, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Wear.text("watch.zikir"), color = WColors.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("$count", color = WColors.text, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Text("/ $target · " + Wear.text("watch.tap"), color = WColors.muted, fontSize = 12.sp)
        Chip(onClick = { count = 0; p.edit().putInt("w.zikir.count", 0).apply() }, label = { Text(Wear.text("watch.reset")) },
            colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.padding(top = 8.dp))
    }
}
