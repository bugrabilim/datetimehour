package tr.bumba.time

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/** Alarm: saat, etiket, tekrar (bir kez / her gün / hafta içi); iOS'taki AlarmItem ile aynı alanlar */
data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "",
    /** "once", "every", "weekdays" */
    val repeats: String = "once",
    val enabled: Boolean = true,
) {
    val title: String get() = label.ifEmpty { L("tools.alarm.defaultLabel") }
    val timeText: String get() = Fmt.time(LocalDateTime.now().withHour(hour).withMinute(minute).atZone(ZoneId.systemDefault()))

    /** Şu andan sonraki ilk çalma anı (ms) */
    fun nextAt(nowMs: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        var t = LocalDateTime.now(zone).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        while (true) {
            val ms = t.atZone(zone).toInstant().toEpochMilli()
            val weekdayOk = repeats != "weekdays" || t.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            if (ms > nowMs && weekdayOk) return ms
            t = t.plusDays(1)
        }
    }

    fun toJson(): JSONObject = JSONObject().put("id", id).put("hour", hour).put("minute", minute).put("label", label)
        .put("repeats", repeats).put("enabled", enabled)

    companion object {
        private const val KEY = "alarms"

        fun fromJson(o: JSONObject) = AlarmItem(
            o.getString("id"), o.getInt("hour"), o.getInt("minute"), o.optString("label"),
            o.optString("repeats", "once"), o.optBoolean("enabled", true),
        )

        fun load(): List<AlarmItem> = runCatching {
            val a = JSONArray(Prefs.store.getString(KEY, "[]"))
            (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())

        fun save(items: List<AlarmItem>) {
            Prefs.store.edit().putString(KEY, JSONArray(items.map { it.toJson() }).toString()).apply()
        }
    }
}

/**
 * Alarm, zamanlayıcı ve pomodoro kurulumu (iOS'taki AlarmCenter). AlarmManager.setAlarmClock ile tam zamanında,
 * uygulama kapalıyken de çalar; telefon yeniden başlayınca ya da saat/dilim değişince yeniden kurulur
 * (RescheduleReceiver). Tam zamanlı alarm izni (Android 12+) yoksa yaklaşık zamanlı kurulur ve ekranda uyarı çıkar.
 * Çalma: alarm kanalında, alarm sesiyle ve sürekli (FLAG_INSISTENT) bildirim + kilit ekranında AlarmActivity.
 */
object AlarmScheduler {
    const val CHANNEL_ALARM = "alarm"
    const val CHANNEL_TOOLS = "tools"
    const val ACTION_FIRE = "tr.bumba.time.ALARM_FIRE"
    const val EXTRA_ID = "id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_KIND = "kind"
    /** Ertelenen alarmın kimlik öneki */
    const val SNOOZE = "snooze-"
    const val SNOOZE_MIN = 5

    /** Tek seferlik kayıtlar: zamanlayıcı ve pomodoro bitişi */
    const val TIMER = "timer"
    const val POMODORO = "pomodoro"

    fun canExact(c: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun canNotify(c: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ALARM) == null) {
            val ch = NotificationChannel(CHANNEL_ALARM, L("tools.hub.cards.alarm.name"), NotificationManager.IMPORTANCE_HIGH)
            ch.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            ch.enableVibration(true)
            ch.vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            ch.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(CHANNEL_TOOLS) == null) {
            val ch = NotificationChannel(CHANNEL_TOOLS, L("tab.tools"), NotificationManager.IMPORTANCE_HIGH)
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
    }

    private fun pending(c: Context, id: String, title: String, kind: String, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            c, id.hashCode(),
            Intent(c, AlarmReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_TITLE, title).putExtra(EXTRA_KIND, kind),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun set(c: Context, atMs: Long, pi: PendingIntent, clock: Boolean) {
        val am = c.getSystemService(AlarmManager::class.java)
        if (!canExact(c)) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            return
        }
        if (clock) {
            val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).putExtra("startTab", Tab.TOOLS.ordinal),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(atMs, show), pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    /** Alarm listesini kurar: kapalı ya da silinmiş olanlar iptal edilir */
    fun sync(c: Context, items: List<AlarmItem> = AlarmItem.load()) {
        val am = c.getSystemService(AlarmManager::class.java)
        val known = Prefs.store.getStringSet("alarms.scheduled", emptySet())!!
        for (id in known) pending(c, id, "", "alarm", PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it) }
        val now = System.currentTimeMillis()
        for (it in items.filter { it.enabled }) {
            set(c, it.nextAt(now), pending(c, it.id, it.title, "alarm", PendingIntent.FLAG_UPDATE_CURRENT)!!, clock = true)
        }
        Prefs.store.edit().putStringSet("alarms.scheduled", items.filter { it.enabled }.map { it.id }.toSet()).apply()
    }

    /** Tek seferlik çalma (zamanlayıcı, pomodoro, erteleme) */
    fun schedule(c: Context, id: String, atMs: Long, title: String, kind: String) {
        set(c, atMs, pending(c, id, title, kind, PendingIntent.FLAG_UPDATE_CURRENT)!!, clock = kind != POMODORO)
    }

    fun cancel(c: Context, id: String) {
        pending(c, id, "", "", PendingIntent.FLAG_NO_CREATE)?.let { c.getSystemService(AlarmManager::class.java).cancel(it) }
        NotificationManagerCompat.from(c).cancel(id.hashCode())
    }

    /** Uygulama ya da telefon yeniden başlayınca: alarmlar ve süren zamanlayıcı/pomodoro yeniden kurulur */
    fun restoreAll(c: Context) {
        sync(c)
        val now = System.currentTimeMillis()
        Prefs.store.getLong("tm.end", 0).takeIf { it > now }?.let { schedule(c, TIMER, it, L("tools.countdown.finished"), TIMER) }
        Prefs.store.getLong("pomo.end", 0).takeIf { it > now }?.let {
            val phase = Prefs.store.getString("pomo.phase", "work")!!
            schedule(c, POMODORO, it, T("tools.pomodoro.phaseDone", L("tools.pomodoro.$phase")), POMODORO)
        }
    }

    /** Çalan alarmın bildirimi: alarm sesi sürekli çalar; kilit ekranında AlarmActivity açılır */
    fun ring(c: Context, id: String, title: String, kind: String) {
        ensureChannels(c)
        if (!canNotify(c)) return
        val nid = id.hashCode()
        val full = PendingIntent.getActivity(
            c, nid,
            Intent(c, AlarmActivity::class.java).putExtra(EXTRA_ID, id).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_KIND, kind)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(a: String) = PendingIntent.getBroadcast(
            c, nid + a.hashCode(),
            Intent(c, AlarmReceiver::class.java).setAction(a).putExtra(EXTRA_ID, id).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_KIND, kind),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val time = Fmt.time(java.time.ZonedDateTime.now())
        val n = if (kind == POMODORO) {
            NotificationCompat.Builder(c, CHANNEL_TOOLS)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle(title)
                .setContentText(L("tools.hub.cards.pomodoro.name"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(c, nid,
                    Intent(c, MainActivity::class.java).putExtra("startTab", Tab.TOOLS.ordinal),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                .build()
        } else {
            val b = NotificationCompat.Builder(c, CHANNEL_ALARM)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle(title)
                .setContentText(time)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), android.media.AudioManager.STREAM_ALARM)
                .setFullScreenIntent(full, true)
                .setContentIntent(full)
                .setOngoing(true)
                .addAction(0, L("tools.alarm.stop"), action(ACTION_STOP))
            if (kind == "alarm") b.addAction(0, L("tools.alarm.snooze"), action(ACTION_SNOOZE))
            b.build().also { it.flags = it.flags or android.app.Notification.FLAG_INSISTENT }
        }
        runCatching { NotificationManagerCompat.from(c).notify(nid, n) }
    }

    const val ACTION_STOP = "tr.bumba.time.ALARM_STOP"
    const val ACTION_SNOOZE = "tr.bumba.time.ALARM_SNOOZE"

    fun stop(c: Context, id: String) {
        NotificationManagerCompat.from(c).cancel(id.hashCode())
    }

    fun snooze(c: Context, id: String, title: String) {
        stop(c, id)
        val sid = if (id.startsWith(SNOOZE)) id else SNOOZE + id
        schedule(c, sid, System.currentTimeMillis() + SNOOZE_MIN * 60_000L, title, "alarm")
    }
}

/** Alarm, zamanlayıcı ve pomodoro anı geldi; bildirim düğmeleri (kapat, ertele) */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: L("tools.alarm.defaultLabel")
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: "alarm"
        when (intent.action) {
            AlarmScheduler.ACTION_STOP -> AlarmScheduler.stop(c, id)
            AlarmScheduler.ACTION_SNOOZE -> AlarmScheduler.snooze(c, id, title)
            else -> {
                if (kind == "alarm" && !id.startsWith(AlarmScheduler.SNOOZE)) {
                    // Bir kezlik alarm kapanır; tekrarlı olanın sonraki çalması kurulur
                    val items = AlarmItem.load().map { if (it.id == id && it.repeats == "once") it.copy(enabled = false) else it }
                    AlarmItem.save(items)
                    AlarmScheduler.sync(c, items)
                }
                AlarmScheduler.ring(c, id, title, kind)
            }
        }
    }
}

/** Telefon açılınca, uygulama güncellenince, saat ya da saat dilimi değişince alarmlar yeniden kurulur */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        AlarmScheduler.restoreAll(c)
    }
}
