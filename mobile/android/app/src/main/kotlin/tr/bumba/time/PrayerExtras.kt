package tr.bumba.time

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import tr.bumba.time.core.Nature
import tr.bumba.time.core.Prayer
import tr.bumba.time.core.PrayerTimes
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Vakitler ekleri (30 Ekim 2026, rakip analizi listesi #65–#68, #73; iOS'taki PrayerExtras.swift ile aynı anahtarlar):
 * hesap yöntemi ve Hanefî ikindisi, Güneş/Ay yayı, vakitte gerçek alarm, vakitte telefonu sessize alma (yalnız Android),
 * hilal görünürlüğü kartı.
 */

// Hesap yöntemi

object PrayerMethod {
    const val METHOD = "prayer.method"
    const val HANAFI = "prayer.hanafi"
    val id: String get() = Prefs.store.getString(METHOD, "diyanet") ?: "diyanet"
    val hanafi: Boolean get() = Prefs.store.getBoolean(HANAFI, false)
    fun params(): PrayerTimes.Parameters = PrayerTimes.Parameters.method(id, hanafi)
    val isCustom: Boolean get() = id != "diyanet" || hanafi
    fun name(id: String) = L("prayer.method.names.$id")
}

@Composable
fun PrayerMethodScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var method by remember { mutableStateOf(PrayerMethod.id) }
    var hanafi by remember { mutableStateOf(PrayerMethod.hanafi) }
    fun save() {
        Prefs.store.edit().putString(PrayerMethod.METHOD, method).putBoolean(PrayerMethod.HANAFI, hanafi).apply()
        PrayerNotifier.reschedule(context)
    }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("prayer.method.app"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                SectionTitle(L("prayer.method.label"))
                for (id in PrayerTimes.METHOD_IDS) Row(Modifier.fillMaxWidth().clickable { method = id; save() }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(method == id, { method = id; save() }, colors = RadioButtonDefaults.colors(selectedColor = Theme.c.accent))
                    Text(PrayerMethod.name(id), color = Theme.c.text, fontSize = 15.sp)
                }
            }
            AppCard {
                SectionTitle(L("prayer.method.asrLabel"))
                for (h in listOf(false, true)) Row(Modifier.fillMaxWidth().clickable { hanafi = h; save() }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(hanafi == h, { hanafi = h; save() }, colors = RadioButtonDefaults.colors(selectedColor = Theme.c.accent))
                    Text(L(if (h) "prayer.method.asrHanafi" else "prayer.method.asrStd"), color = Theme.c.text, fontSize = 15.sp)
                }
            }
            Text(L("prayer.method.appNote"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}

// Güneş/Ay yayı

@Composable
fun SunMoonArc(now: Instant, place: AppPlace) {
    val today = place.today(now)
    fun sun(d: LocalDate): Pair<Instant, Instant>? {
        val e = place.entries(d)
        val r = e.firstOrNull { it.prayer == Prayer.SUNRISE }?.at ?: return null
        val s = e.firstOrNull { it.prayer == Prayer.MAGHRIB }?.at ?: return null
        return r to s
    }
    val t = sun(today) ?: return
    val day: Boolean
    val start: Instant
    val end: Instant
    val marks: List<Instant>
    when {
        !now.isBefore(t.first) && now.isBefore(t.second) -> {
            day = true; start = t.first; end = t.second
            marks = place.entries(today).filter { it.prayer == Prayer.DHUHR || it.prayer == Prayer.ASR }.map { it.at }
        }
        now.isBefore(t.first) -> {
            day = false; start = sun(today.minusDays(1))?.second ?: return; end = t.first
            marks = place.entries(today).filter { it.prayer == Prayer.FAJR }.map { it.at }
        }
        else -> {
            day = false; start = t.second; end = sun(today.plusDays(1))?.first ?: return
            marks = place.entries(today).filter { it.prayer == Prayer.ISHA }.map { it.at } +
                place.entries(today.plusDays(1)).filter { it.prayer == Prayer.FAJR }.map { it.at }
        }
    }
    val total = (end.toEpochMilli() - start.toEpochMilli()).toDouble()
    val f = ((now.toEpochMilli() - start.toEpochMilli()) / total).coerceIn(0.0, 1.0)
    val moon = Nature.moon(now.toEpochMilli())
    val muted = Theme.c.muted
    val accent = Theme.c.accent
    AppCard {
        SectionTitle(L("prayer.arc.title"))
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val left = 18.dp.toPx()
            val right = size.width - 18.dp.toPx()
            val base = size.height - 22.dp.toPx()
            val h = size.height - 40.dp.toPx()
            fun p(x: Double) = Offset((left + (right - left) * x).toFloat(), (base - h * sin(PI * x)).toFloat())
            drawLine(muted.copy(alpha = 0.5f), Offset(4f, base), Offset(size.width - 4f, base), 2f)
            val arc = Path().apply { for (i in 0..60) { val q = p(i / 60.0); if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) } }
            drawPath(arc, muted.copy(alpha = 0.6f), style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
            val done = Path().apply { for (i in 0..60) { if (i / 60.0 > f) break; val q = p(i / 60.0); if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) } }
            drawPath(done, if (day) Color(0xFFFF9500) else accent, style = Stroke(6f))
            for (m in marks) {
                val x = (m.toEpochMilli() - start.toEpochMilli()) / total
                if (x > 0 && x < 1) drawCircle(muted, 6f, p(x))
            }
            val c = p(f)
            if (day) drawCircle(Color(0xFFFF9500), 22f, c)
            else {
                val r = 20f
                drawCircle(Color(0xFFEBEBEB), r, c)
                val shift = (if (moon.fraction < 0.5) -1 else 1) * 2 * r * moon.illumination.toFloat()
                val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
                clipPath(clip) { drawCircle(Color(0xFF404040), r, Offset(c.x + shift, c.y)) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.hm(start, place.zone), color = Theme.c.muted, fontSize = 14.sp, style = Tnum)
            Spacer(Modifier.weight(1f))
            Text(T(if (day) "prayer.arc.day" else "prayer.arc.night", Fmt.hm(((end.toEpochMilli() - now.toEpochMilli()) / 1000).toInt())), color = Theme.c.muted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(Fmt.hm(end, place.zone), color = Theme.c.muted, fontSize = 14.sp, style = Tnum)
        }
    }
}

// Vakitte alarm: seçili her vaktin sıradaki anı için AlarmManager.setAlarmClock (alarm sesiyle, sessizde de çalar)

object PrayerAlarms {
    const val KEY = "prayer.alarm"
    const val KIND = "prayer"
    private const val PREFIX = "prayer-alarm-"

    var selected: Set<Prayer>
        get() = (Prefs.store.getString(KEY, "") ?: "").split(",").mapNotNull { k -> Prayer.entries.firstOrNull { it.key == k } }.toSet()
        set(v) = Prefs.store.edit().putString(KEY, Prayer.entries.filter { it in v }.joinToString(",") { it.key }).apply()

    fun sync(c: Context, now: Instant = Instant.now()) {
        for (p in Prayer.entries) AlarmScheduler.cancel(c, PREFIX + p.key)
        val sel = selected
        if (sel.isEmpty()) return
        val place = AppPlace.current()
        val today = place.today(now)
        for (p in sel) {
            val at = (0..2L).flatMap { place.entries(today.plusDays(it)) }.firstOrNull { it.prayer == p && it.at.isAfter(now) }?.at ?: continue
            AlarmScheduler.schedule(c, PREFIX + p.key, at.toEpochMilli(), T("prayer.alarm.title", p.title), KIND)
        }
    }
}

// Vakitte telefonu sessize alma (yalnız Android): vakit girince titreşim ya da Rahatsız Etmeyin, süre bitince önceki hâl

object PrayerSilent {
    const val MINUTES = "prayer.silent"
    const val MODE = "prayer.silent.mode"
    const val PRAYERS = "prayer.silent.prayers"
    val CHOICES = listOf(0, 15, 20, 30, 45, 60)
    private const val ACTION_START = "tr.bumba.time.SILENT_START"
    private const val ACTION_END = "tr.bumba.time.SILENT_END"
    private const val CHANNEL = "prayer-silent"
    private const val NOTE_ID = 7_331

    val minutes: Int get() = Prefs.store.getInt(MINUTES, 0)
    val dnd: Boolean get() = Prefs.store.getString(MODE, "vibrate") == "dnd"
    var prayers: Set<Prayer>
        get() = (Prefs.store.getString(PRAYERS, "fajr,dhuhr,asr,maghrib,isha") ?: "").split(",").mapNotNull { k -> Prayer.entries.firstOrNull { it.key == k } }.toSet()
        set(v) = Prefs.store.edit().putString(PRAYERS, Prayer.entries.filter { it in v }.joinToString(",") { it.key }).apply()

    fun hasPolicy(c: Context) = c.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    fun policyIntent() = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    private fun pi(c: Context, action: String, flags: Int) = PendingIntent.getBroadcast(
        c, action.hashCode(), Intent(c, PrayerSilentReceiver::class.java).setAction(action), flags or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun setAt(c: Context, at: Long, p: PendingIntent) {
        val am = c.getSystemService(AlarmManager::class.java)
        if (AlarmScheduler.canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }

    /** Sıradaki seçili vaktin başlangıcını kurar (bitiş başlangıçta kurulur) */
    fun sync(c: Context, now: Instant = Instant.now()) {
        pi(c, ACTION_START, PendingIntent.FLAG_NO_CREATE)?.let { c.getSystemService(AlarmManager::class.java).cancel(it) }
        if (minutes <= 0) return
        val sel = prayers
        val place = AppPlace.current()
        val next = (0..2L).flatMap { place.entries(place.today(now).plusDays(it)) }.firstOrNull { it.prayer in sel && it.at.isAfter(now) } ?: return
        setAt(c, next.at.toEpochMilli(), pi(c, ACTION_START, PendingIntent.FLAG_UPDATE_CURRENT)!!)
    }

    fun start(c: Context) {
        val am = c.getSystemService(AudioManager::class.java)
        val nm = c.getSystemService(NotificationManager::class.java)
        val store = Prefs.store
        if (!store.contains("prayer.silent.prev")) {
            store.edit().putInt("prayer.silent.prev", am.ringerMode).putInt("prayer.silent.prevFilter", nm.currentInterruptionFilter).apply()
        }
        runCatching {
            if (dnd && hasPolicy(c)) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            else if (am.ringerMode == AudioManager.RINGER_MODE_NORMAL) am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }
        val end = System.currentTimeMillis() + minutes * 60_000L
        setAt(c, end, pi(c, ACTION_END, PendingIntent.FLAG_UPDATE_CURRENT)!!)
        note(c, end)
        sync(c, Instant.ofEpochMilli(end))
    }

    fun end(c: Context) {
        val am = c.getSystemService(AudioManager::class.java)
        val nm = c.getSystemService(NotificationManager::class.java)
        val store = Prefs.store
        val prev = store.getInt("prayer.silent.prev", -1)
        val prevFilter = store.getInt("prayer.silent.prevFilter", -1)
        runCatching {
            if (prevFilter > 0 && hasPolicy(c) && nm.currentInterruptionFilter != prevFilter) nm.setInterruptionFilter(prevFilter)
            if (prev >= 0 && am.ringerMode != prev && (prev != AudioManager.RINGER_MODE_SILENT || hasPolicy(c))) am.ringerMode = prev
        }
        store.edit().remove("prayer.silent.prev").remove("prayer.silent.prevFilter").apply()
        NotificationManagerCompat.from(c).cancel(NOTE_ID)
        sync(c)
    }

    private fun note(c: Context, end: Long) {
        if (!AlarmScheduler.canNotify(c)) return
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = c.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) nm.createNotificationChannel(NotificationChannel(CHANNEL, L("prayer.silent.channel"), NotificationManager.IMPORTANCE_LOW))
        }
        val time = Instant.ofEpochMilli(end).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
        val n = NotificationCompat.Builder(c, CHANNEL).setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(T("prayer.silent.active", time)).setOngoing(true).setSilent(true)
            .addAction(0, L("common.done"), pi(c, ACTION_END, PendingIntent.FLAG_UPDATE_CURRENT)).build()
        runCatching { NotificationManagerCompat.from(c).notify(NOTE_ID, n) }
    }

    fun handle(c: Context, action: String?) {
        when (action) {
            ACTION_START -> start(c)
            ACTION_END -> end(c)
        }
    }
}

class PrayerSilentReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) = PrayerSilent.handle(c, intent.action)
}

@Composable
fun PrayerAlarmCard() {
    val context = LocalContext.current
    var sel by remember { mutableStateOf(PrayerAlarms.selected) }
    AppCard {
        SectionTitle(L("prayer.alarm.heading"))
        for (p in Prayer.entries) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.title, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
            Switch(p in sel, { on ->
                sel = if (on) sel + p else sel - p
                PrayerAlarms.selected = sel
                PrayerAlarms.sync(context)
            }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
        }
        Text(L("prayer.alarm.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

@Composable
fun PrayerSilentCard() {
    val context = LocalContext.current
    var minutes by remember { mutableStateOf(PrayerSilent.minutes) }
    var dnd by remember { mutableStateOf(PrayerSilent.dnd) }
    var sel by remember { mutableStateOf(PrayerSilent.prayers) }
    fun changed() = PrayerSilent.sync(context)
    AppCard {
        SectionTitle(L("prayer.silent.heading"))
        Text(L("prayer.silent.minutes"), color = Theme.c.muted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (m in PrayerSilent.CHOICES) Chip(if (m == 0) L("prayer.silent.off") else T("prayer.silent.min", m), minutes == m) {
                minutes = m; Prefs.store.edit().putInt(PrayerSilent.MINUTES, m).apply(); changed()
            }
        }
        if (minutes > 0) {
            Text(L("prayer.silent.mode"), color = Theme.c.muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Chip(L("prayer.silent.vibrate"), !dnd) { dnd = false; Prefs.store.edit().putString(PrayerSilent.MODE, "vibrate").apply() }
                Chip(L("prayer.silent.silent"), dnd) { dnd = true; Prefs.store.edit().putString(PrayerSilent.MODE, "dnd").apply() }
            }
            if (dnd && !PrayerSilent.hasPolicy(context)) TextButton(onClick = { runCatching { context.startActivity(PrayerSilent.policyIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) {
                Text(L("prayer.silent.grant"), color = Theme.c.accent)
            }
            for (p in Prayer.entries.filter { it != Prayer.SUNRISE }) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.title, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                Switch(p in sel, { on -> sel = if (on) sel + p else sel - p; PrayerSilent.prayers = sel; changed() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
            }
        }
        Text(L("prayer.silent.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
        Text(text, maxLines = 1, color = if (on) Theme.c.accent else Theme.c.muted, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
    }
}

// Hilal görünürlüğü

@Composable
fun CrescentCard(place: AppPlace, now: Instant) {
    val m = Nature.moon(now.toEpochMilli())
    val conj = if (m.age < 3) (now.toEpochMilli() - (m.age * 86_400_000).toLong()) else m.nextNewMoon
    val evenings = remember(conj, place.lat, place.lon) { Nature.crescent(conj, place.lat, place.lon) }
    SectionTitle(L("nature.crescentHeading"))
    for (e in evenings) AppCard {
        Text(Fmt.dayMonth(java.time.Instant.ofEpochMilli(e.sunset).atZone(place.zone).toLocalDate()), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(L("nature.crescentCats.${e.category}"), color = if (e.category == "A" || e.category == "B") Theme.c.accent else Theme.c.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        val parts = mutableListOf("☀ " + Fmt.hm(Instant.ofEpochMilli(e.sunset), place.zone))
        e.moonset?.let { ms ->
            val lag = e.lag ?: 0.0
            parts += "☾ " + Fmt.hm(Instant.ofEpochMilli(ms), place.zone) + " (" + (if (lag > 0) T("nature.crescentLag", lag.roundToInt()) else L("nature.crescentBefore")) + ")"
        }
        if (e.age > 0) parts += T("nature.crescentAge", e.age.roundToInt())
        Text(parts.joinToString(" · "), color = Theme.c.muted, fontSize = 12.sp)
    }
    Text(T("nature.crescentLead", place.name, Fmt.hm(Instant.ofEpochMilli(conj), place.zone)) + " " + L("nature.crescentNote"), color = Theme.c.muted, fontSize = 12.sp)
}
