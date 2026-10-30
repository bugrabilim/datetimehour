package tr.bumba.time

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.Instant
import java.time.LocalDate

/**
 * Süren sayaçlar (iOS Live Activity / Dynamic Island karşılığı, rakip analizi #2): çalışan zamanlayıcılar, pomodoro,
 * kronometre ve isteğe bağlı "sıradaki vakit" (Ramazan'da iftar ve sahur) bildirim alanında ve kilit ekranında kalıcı
 * bildirim olarak; süre sistemin sayacıyla akar (Chronometer). Kayıtlı durumdan kurulur: araçlarda durum değişince,
 * uygulama açılınca ve bir sonraki bitişte/vakitte (LiveRefreshReceiver) yeniden çizilir, bitenler kalkar.
 */
object LiveNotifications {
    const val CHANNEL = "live"
    const val PRAYER_KEY = "live.prayer"
    private const val IDS_KEY = "live.ids"

    private fun nid(key: String) = ("live-$key").hashCode()

    private class Item(val key: String, val title: String, val text: String, val whenMs: Long, val countDown: Boolean,
                       val tab: Tab, val tool: String?, val icon: Int)

    fun sync(c: Context, now: Long = System.currentTimeMillis()) {
        val app = c.applicationContext
        val nm = NotificationManagerCompat.from(app)
        val items = ArrayList<Item>()
        for (t in TimerItem.load()) if (t.running && t.end > now) {
            items += Item("timer-${t.id}", t.name.ifBlank { L("tools.timer") }, Fmt.duration(t.total / 1000), t.end, true,
                Tab.TOOLS, "timer", R.drawable.ic_stat_alarm)
        }
        val sp = Prefs.store
        val swStart = sp.getLong("sw.start", 0L)
        if (swStart > 0) {
            items += Item("stopwatch", L("tools.hub.cards.stopwatch.name"), "", swStart - sp.getLong("sw.acc", 0L), false,
                Tab.TOOLS, "stopwatch", R.drawable.ic_stat_alarm)
        }
        val pomoEnd = sp.getLong("pomo.end", 0L)
        if (pomoEnd > now) {
            val phase = sp.getString("pomo.phase", "work")!!
            items += Item("pomodoro", L("tools.pomodoro.$phase"), T("tools.pomodoro.round", sp.getInt("pomo.round", 1), sp.getInt("pomo.rounds", 4)),
                pomoEnd, true, Tab.TOOLS, "pomodoro", R.drawable.ic_stat_alarm)
        }
        val prayer = if (sp.getBoolean(PRAYER_KEY, false)) prayerItem(now) else null
        prayer?.let { items += it.first }

        val wanted = items.map { nid(it.key) }.toSet()
        for (id in sp.getStringSet(IDS_KEY, emptySet())!!.mapNotNull { it.toIntOrNull() }) if (id !in wanted) nm.cancel(id)
        if (items.isNotEmpty() && AlarmScheduler.canNotify(app)) {
            channel(app)
            for (it in items) runCatching { nm.notify(nid(it.key), build(app, it)) }
        }
        sp.edit().putStringSet(IDS_KEY, wanted.map { it.toString() }.toSet()).apply()

        // Bir sonraki değişim: en yakın bitiş ya da vakit (geçenler kalksın, vakit ileri taşınsın)
        val next = (items.filter { it.countDown }.map { it.whenMs } + listOfNotNull(prayer?.second)).filter { it > now }.minOrNull()
        schedule(app, next)
    }

    /** Sıradaki vakit ve o vaktin anı */
    private fun prayerItem(now: Long): Pair<Item, Long>? {
        val place = AppPlace.current()
        val next = place.next(Instant.ofEpochMilli(now)) ?: return null
        val day = next.at.atZone(place.zone).toLocalDate()
        val ramadan = AppData.ramadans.any { day in it }
        val name = when {
            ramadan && next.prayer == tr.bumba.time.core.Prayer.MAGHRIB -> L("live.iftar")
            ramadan && next.prayer == tr.bumba.time.core.Prayer.FAJR -> L("live.sahur")
            else -> next.prayer.title
        }
        val at = next.at.toEpochMilli()
        return Item("prayer", name + " " + Fmt.time(next.at.atZone(place.zone)), place.name, at, true,
            Tab.PRAYER, null, R.drawable.ic_stat_prayer) to at
    }

    private fun channel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, L("live.channel"), NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
    }

    private fun build(c: Context, it: Item) = NotificationCompat.Builder(c, CHANNEL)
        .setSmallIcon(it.icon)
        .setContentTitle(it.title)
        .apply { if (it.text.isNotEmpty()) setContentText(it.text) }
        .setWhen(it.whenMs)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setChronometerCountDown(it.countDown)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(if (it.key == "prayer") NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_STOPWATCH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(PendingIntent.getActivity(c, nid(it.key),
            Intent(c, MainActivity::class.java).putExtra("startTab", it.tab.ordinal).apply { it.tool?.let { t -> putExtra("tool", t) } }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .build()

    private fun schedule(c: Context, at: Long?) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(c, 0, Intent(c, LiveRefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (at == null) { am.cancel(pi); return }
        val t = at + 1000
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, t, 60_000L, pi)
    }
}

/** Bir sayaç bitince ya da vakit girince süren sayaç bildirimleri yeniden çizilir */
class LiveRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) = LiveNotifications.sync(c)
}
