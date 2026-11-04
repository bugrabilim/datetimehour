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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import tr.bumba.time.core.Chronotype
import tr.bumba.time.core.SleepStats
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.sqrt

/**
 * Tatil modu (2. analiz #44; iOS'taki AlarmPause ile aynı anahtarlar): `alarm.pause.from` / `.to` (ISO, ikisi de dahil)
 * arasında tekrarlayan alarmlar çalmaz; bir kezlik alarmlar etkilenmez.
 */
object AlarmPause {
    const val FROM = "alarm.pause.from"
    const val TO = "alarm.pause.to"

    fun range(): Pair<LocalDate, LocalDate>? {
        val a = AlarmItem.parse(Prefs.store.getString(FROM, "") ?: "") ?: return null
        val b = AlarmItem.parse(Prefs.store.getString(TO, "") ?: "") ?: return null
        return if (b.isBefore(a)) null else a to b
    }

    fun contains(d: LocalDate): Boolean = range()?.let { (a, b) -> !d.isBefore(a) && !d.isAfter(b) } ?: false

    /** Bitmiş tatil kendiliğinden kalkar */
    fun active(): Boolean = range()?.let { (_, b) -> !LocalDate.now().isAfter(b) } ?: false

    fun save(c: Context, from: LocalDate?, to: LocalDate?) {
        Prefs.store.edit().putString(FROM, from?.toString() ?: "").putString(TO, to?.toString() ?: "").apply()
        AlarmScheduler.sync(c)
    }
}

/**
 * Sakin ön alarm (2. analiz #42): alarmdan `preAlarm` dakika önce uygulamanın kendi yağmur sesi (NoiseGen) kısık çalar
 * (SoundService, kaynak "prealarm"); asıl alarm çalınca ya da alarm anında durur.
 */
object PreAlarm {
    private fun pending(c: Context, id: String, at: Long, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        c, ("prealarm-$id").hashCode(),
        Intent(c, PreAlarmReceiver::class.java).putExtra(AlarmScheduler.EXTRA_ID, id).putExtra("at", at),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    fun sync(c: Context, items: List<AlarmItem>, known: Set<String>) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        for (id in known + items.map { it.id }) pending(c, id, 0, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it) }
        val now = System.currentTimeMillis()
        for (it in items) {
            if (!it.enabled || it.preAlarm <= 0) continue
            val at = it.nextAt(now)
            if (at == AlarmItem.NEVER) continue
            val fire = at - it.preAlarm * 60_000L
            if (fire <= now) continue
            val pi = pending(c, it.id, at, PendingIntent.FLAG_UPDATE_CURRENT)!!
            if (android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pi)
        }
    }
}

class PreAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val at = intent.getLongExtra("at", 0L)
        if (at <= System.currentTimeMillis()) return
        // Uyku sesi çalıyorsa ona dokunulmaz
        if (SoundService.source.isNotEmpty() && SoundService.source != "prealarm") return
        SoundService.play(c, "rain", 30, at - 2000, "prealarm")
    }
}

/**
 * Uyku düzeni kaydı (2. analiz #47): kalkış alarmın kapatıldığı an, yatış uyku seslerinin ya da akıllı alarmın
 * başlatıldığı an (18.00–04.00 arası). Kayıt `sleep.log` (SleepStats.Entry), bekleyen yatış `sleep.bedAt` (ms).
 */
object SleepLog {
    private const val KEY = "sleep.log"
    private const val BED = "sleep.bedAt"

    fun load(): List<SleepStats.Entry> = runCatching {
        val a = JSONArray(Prefs.store.getString(KEY, "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> SleepStats.Entry(o.getString("d"), o.getInt("w"), o.optInt("b", -1)) } }
    }.getOrDefault(emptyList())

    fun save(l: List<SleepStats.Entry>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("d", it.d).put("w", it.w).put("b", it.b)) }
        Prefs.store.edit().putString(KEY, a.toString()).apply()
    }

    fun clear() = Prefs.store.edit().remove(KEY).remove(BED).apply()

    fun markBed(ms: Long = System.currentTimeMillis()) {
        val h = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).hour
        if (h >= 18 || h < 4) Prefs.store.edit().putLong(BED, ms).apply()
    }

    /** Kalkış: 03.00–13.00 arası; son 16 saatte yatış işaretlendiyse birlikte yazılır */
    fun recordWake(ms: Long = System.currentTimeMillis()) {
        val t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
        if (t.hour < 3 || t.hour >= 13) return
        val bedAt = Prefs.store.getLong(BED, 0L)
        val b = if (bedAt > 0 && ms - bedAt in 1..16 * 3_600_000L) Instant.ofEpochMilli(bedAt).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute } else -1
        save(SleepStats.record(load(), SleepStats.Entry(t.toLocalDate().toString(), t.hour * 60 + t.minute, b)))
        Prefs.store.edit().remove(BED).apply()
    }
}

/** Alarm ekranında el feneri (2. analiz #41): arka kameranın flaşı, izin istemez */
object Torch {
    private fun id(c: Context): String? {
        val cm = c.getSystemService(CameraManager::class.java) ?: return null
        return runCatching { cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } }.getOrNull()
    }

    fun available(c: Context) = id(c) != null

    fun set(c: Context, on: Boolean) {
        val cm = c.getSystemService(CameraManager::class.java) ?: return
        id(c)?.let { runCatching { cm.setTorchMode(it, on) } }
    }
}

/** Çalarken sallayınca erteleme (2. analiz #41): 1 sn içinde iki kez 2,2 g üstü sarsıntı */
@Composable
fun ShakeToSnooze(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val sm = context.getSystemService(SensorManager::class.java)
        val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last = 0L
        var fired = false
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / SensorManager.GRAVITY_EARTH
                if (g < 2.2f || fired) return
                val now = System.currentTimeMillis()
                if (now - last in 120..1000) { fired = true; onShake() }
                last = now
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (enabled && acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(l) }
    }
}

/** Listede büyük "sıradaki alarm X sa Y dk sonra" kartı (2. analiz #51) */
@Composable
fun NextAlarmCard(items: List<AlarmItem>, nowMs: Long) {
    val next = items.filter { it.enabled }.map { it.nextAt(nowMs) }.filter { it != AlarmItem.NEVER }.minOrNull() ?: return
    AppCard {
        val mins = ((next - nowMs + 59_999) / 60_000).toInt()
        Text(T("alarm.nextIn", Fmt.hm(mins * 60)), color = Theme.c.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(Fmt.time(Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault())) + " · " + Fmt.longDate(Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault()).toLocalDate()),
            color = Theme.c.muted, fontSize = 14.sp)
    }
}

/** Tatil modu kartı: başlangıç ve bitiş tarihi, kaldır */
@Composable
fun VacationCard() {
    val context = LocalContext.current
    var range by remember { mutableStateOf(AlarmPause.range()) }
    var on by remember { mutableStateOf(AlarmPause.active()) }
    fun pick(start: LocalDate, set: (LocalDate) -> Unit) {
        android.app.DatePickerDialog(context, { _, y, m, d -> set(LocalDate.of(y, m + 1, d)) }, start.year, start.monthValue - 1, start.dayOfMonth).show()
    }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("alarm.pause"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
            SoftSwitch(on, { v ->
                on = v
                if (v) {
                    val a = LocalDate.now()
                    AlarmPause.save(context, a, a.plusDays(6)); range = a to a.plusDays(6)
                } else { AlarmPause.save(context, null, null); range = null }
            }, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
        }
        val r = range
        if (on && r != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { pick(r.first) { d -> val b = if (r.second.isBefore(d)) d else r.second; AlarmPause.save(context, d, b); range = d to b } }) {
                    Text(L("focus.schedFrom") + "  " + Fmt.dayMonth(r.first), color = Theme.c.accent)
                }
                TextButton(onClick = { pick(r.second) { d -> val a = if (d.isBefore(r.first)) d else r.first; AlarmPause.save(context, a, d); range = a to d } }) {
                    Text(L("focus.schedTo") + "  " + Fmt.dayMonth(r.second), color = Theme.c.accent)
                }
            }
            Text(T("alarm.pausedRange", Fmt.dayMonth(r.first), Fmt.dayMonth(r.second)), color = Theme.c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(L("alarm.pauseNote"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/** Uyku düzeni kartı (2. analiz #47) */
@Composable
fun SleepStatsCard() {
    var log by remember { mutableStateOf(SleepLog.load()) }
    val goal = Prefs.store.getInt("sleep.dur", 480)
    val s = SleepStats.summary(log, LocalDate.now().toString(), goal)
    AppCard {
        SectionTitle(L("sleepstats.title"))
        if (s.weekWake == null && s.spread == null) {
            Text(L("sleepstats.empty"), color = Theme.c.muted, fontSize = 14.sp)
        } else {
            s.weekWake?.let { w ->
                Row2(L("sleepstats.avgWake"), "%02d:%02d".format(w / 60, w % 60))
                s.prevWake?.let { p ->
                    val d = w - p
                    Text(when {
                        d < 0 -> T("sleepstats.earlier", -d)
                        d > 0 -> T("sleepstats.later", d)
                        else -> L("sleepstats.same")
                    }, color = Theme.c.muted, fontSize = 13.sp)
                }
            }
            s.spread?.let { Text(T("sleepstats.spread", it), color = Theme.c.text, fontSize = 14.sp) }
            s.debt?.let { Text(if (it > 0) T("sleepstats.debt", Fmt.hm(it * 60)) else L("sleepstats.noDebt"), color = Theme.c.text, fontSize = 14.sp) }
            TextButton(onClick = { SleepLog.clear(); log = emptyList() }) { Text(L("sleepstats.clear"), color = Theme.c.err) }
        }
        Text(L("sleepstats.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/** Kronotip testi kartı (2. analiz #48); girdiler kaydedilmez */
@Composable
fun ChronotypeCard() {
    val context = LocalContext.current
    var times by remember { mutableStateOf(listOf(1380, 420, 30, 540)) }
    fun pick(i: Int) {
        val v = times[i]
        android.app.TimePickerDialog(context, { _, h, m -> times = times.toMutableList().also { it[i] = h * 60 + m } }, v / 60, v % 60, !Prefs.h12).show()
    }
    val m = Chronotype.msfsc(times[0], times[1], times[2], times[3])
    val c = Chronotype.category(m)
    AppCard {
        SectionTitle(L("chrono.title"))
        listOf("chrono.workBed", "chrono.workWake", "chrono.freeBed", "chrono.freeWake").forEachIndexed { i, k ->
            Row(Modifier.fillMaxWidth().clickable { pick(i) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(L(k), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                Text("%02d:%02d".format(times[i] / 60, times[i] % 60), color = Theme.c.accent, fontSize = 15.sp, style = Tnum)
            }
        }
        val mm = (m + 1440) % 1440
        Text(L("chrono.c$c"), color = Theme.c.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(T("chrono.result", "%02d:%02d".format(mm / 60, mm % 60)), color = Theme.c.text, fontSize = 14.sp)
        Text(L("chrono.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/**
 * Enerji blokları (2. analiz #49): kalkıştan sonra yaklaşık sabah zirvesi (+2,5–5 sa), öğleden sonra düşüş (+7–9 sa) ve
 * akşam zirvesi (+10–12 sa); "Takvime ekle" sistemin takvim uygulamasında yeni etkinlik açar (izin gerekmez).
 */
@Composable
fun EnergyBlocksCard() {
    val context = LocalContext.current
    val wake = Prefs.store.getInt("sleep.wake", 420)
    val blocks = listOf(Triple("energy.morning", 150, 300), Triple("energy.dip", 420, 540), Triple("energy.evening", 600, 720))
    AppCard {
        SectionTitle(L("energy.title"))
        for ((k, a, b) in blocks) {
            val from = (wake + a) % 1440
            val to = (wake + b) % 1440
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(L(k), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("%02d:%02d – %02d:%02d".format(from / 60, from % 60, to / 60, to % 60), color = Theme.c.muted, fontSize = 13.sp, style = Tnum)
                }
                TextButton(onClick = {
                    val start = LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay().plusMinutes(from.toLong())
                    val ms = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ms)
                            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, ms + (b - a) * 60_000L)
                            .putExtra(CalendarContract.Events.TITLE, L(k)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }) { Text(L("energy.add"), color = Theme.c.accent) }
            }
        }
        Text(L("energy.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/** Uygulama seçici (alarm kapanınca açılacak uygulama, 2. analiz #43): ana ekranda simgesi olanlar */
@Composable
fun OpenAppRow(pkg: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val apps = remember { FocusBlock.launchable(context) }
    val name = apps.firstOrNull { it.first == pkg }?.second ?: L("alarm.openAppNone")
    Row(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(L("alarm.openApp"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        Text(name, color = Theme.c.accent, fontSize = 15.sp, maxLines = 1)
    }
    if (open) androidx.compose.material3.AlertDialog(
        onDismissRequest = { open = false },
        confirmButton = {},
        title = { Text(L("alarm.openApp")) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn {
                item { Text(L("alarm.openAppNone"), Modifier.fillMaxWidth().clickable { onChange(""); open = false }.padding(vertical = 10.dp), color = Theme.c.text) }
                items(apps.size) { i ->
                    Text(apps[i].second, Modifier.fillMaxWidth().clickable { onChange(apps[i].first); open = false }.padding(vertical = 10.dp),
                        color = if (apps[i].first == pkg) Theme.c.accent else Theme.c.text)
                }
            }
        },
        containerColor = Theme.c.surface,
    )
}

/** Alarm kapanınca seçilen uygulamayı açar */
fun launchApp(c: Context, pkg: String): Boolean {
    if (pkg.isEmpty()) return false
    val i = c.packageManager.getLaunchIntentForPackage(pkg) ?: return false
    return runCatching { c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}

/** Uyku planının kalkış alarmı (2. analiz #52): tek alarm, `sleepPlan` işaretli; plan değişince saati ve günleri güncellenir */
object SleepPlanAlarm {
    fun set(c: Context, wake: Int, days: List<Int>) {
        val items = AlarmItem.load()
        val existing = items.firstOrNull { it.sleepPlan }
        val list = if (existing != null) items.map { if (it.id == existing.id) it.copy(hour = wake / 60, minute = wake % 60, days = days, enabled = true) else it }
        else items + AlarmItem(hour = wake / 60, minute = wake % 60, label = L("sleep.alarmLabel"), days = days, sleepPlan = true)
        AlarmItem.save(list.sortedWith(compareBy({ it.hour }, { it.minute })))
        AlarmScheduler.sync(c)
    }

    fun update(c: Context, wake: Int, days: List<Int>) {
        val items = AlarmItem.load()
        val existing = items.firstOrNull { it.sleepPlan } ?: return
        if (existing.hour * 60 + existing.minute == wake && existing.days == days) return
        set(c, wake, days)
    }
}
