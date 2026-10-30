package tr.bumba.time

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tr.bumba.time.core.Prayer
import java.time.Instant

/**
 * Vakit bildirimlerini telefonda kurar (iOS'taki PrayerNotifier'ın karşılığı; sunucu yok). Plan PrayerAlerts'ten;
 * AlarmManager'a sıradaki birkaç vakit tam zamanlı kurulur, her bildirim geldiğinde plan şimdiden yeniden kurulur.
 * Uygulama açılınca, il ya da tercih değişince, telefon yeniden başlayınca, saat/saat dilimi değişince ve uygulama
 * güncellenince de baştan kurulur. Tam zamanlı alarm izni yoksa (Android 12+) yaklaşık zamanlı kurulur ve ayar
 * sayfasında izin satırı gösterilir.
 */
object PrayerNotifier {
    /** Sessiz kanal: zil/duyuru alıcıdan çalınır (NotificationSounds); "Telefonun sesi" seçiliyse sesli kanal */
    const val CHANNEL_QUIET = "prayer"
    const val CHANNEL_SOUND = "prayer-sound"
    /** AlarmManager'da aynı anda kurulu bildirim sayısı (istek kodları 0…SLOTS-1) */
    private const val SLOTS = 8
    private const val ACTION_FIRE = "tr.bumba.time.PRAYER_ALERT"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    /** Uygulama başlarken: kanallar, ilk kurulum ve konum/tercih değişikliklerini izleme */
    fun start(context: Context) {
        val app = context.applicationContext
        createChannels(app)
        FeatureNotifier.createChannel(app)
        // Konum (il ya da otomatik konum) değişince plan yeniden kurulur; Prefs.kt'ye dokunmadan izlenir
        val keys = setOf("province", "auto.lat", "auto.lon", "auto.ref")
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in keys) reschedule(app)
        }.also { Prefs.store.registerOnSharedPreferenceChangeListener(it) }
        reschedule(app)
    }

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val name = L("notify.heading")
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_QUIET, name, NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SOUND, name + " · " + L("notify.sound.system"), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun exactAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: false
    }

    /**
     * Bekleyen vakit alarmlarını siler, plana göre yeniden kurar; sesli duyuru dosyalarını arka planda hazırlar.
     * Özellik bildirimleri (FeatureNotifier) de aynı anlarda yeniden kurulur.
     */
    fun reschedule(context: Context) {
        val app = context.applicationContext
        FeatureNotifier.reschedule(app)
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        for (i in 0 until SLOTS) {
            PendingIntent.getBroadcast(app, i, fireIntent(app), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                ?.let { am.cancel(it); it.cancel() }
        }
        val settings = NotifyPrefs.current
        if (!settings.enabled) return
        val place = AppPlace.current()
        val alerts = PrayerAlerts.plan(place, settings, Instant.now(), limit = SLOTS)
        val exact = exactAllowed(app)
        alerts.forEachIndexed { i, a ->
            val pi = PendingIntent.getBroadcast(
                app, i,
                fireIntent(app).putExtra("prayer", a.prayer.key).putExtra("at", a.prayerAt.toEpochMilli())
                    .putExtra("lead", settings.leadMinutes),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val t = a.fireAt.toEpochMilli()
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        }
        if (NotifyPrefs.sound == "voice") {
            val prayers = alerts.map { it.prayer }.toSet()
            scope.launch { runCatching { NotificationSounds.prepare(app, prayers, settings.leadMinutes) } }
        }
    }

    private fun fireIntent(context: Context) = Intent(context, PrayerAlarmReceiver::class.java).setAction(ACTION_FIRE)

    /** Bildirimi gösterir; ses ayarına göre zil/duyuru çalar (bitince PendingResult kapanır) */
    fun deliver(context: Context, prayer: Prayer, prayerAt: Instant, lead: Int, done: () -> Unit) {
        val place = AppPlace.current()
        val mode = NotifyPrefs.sound
        if (notificationsAllowed(context)) {
            val name = prayer.title
            val title = if (lead > 0) T("notify.title.before", name, lead) else T("notify.title.now", name)
            val open = PendingIntent.getActivity(
                context, 100,
                Intent(context, MainActivity::class.java).putExtra("startTab", Tab.PRAYER.ordinal)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val n = NotificationCompat.Builder(context, if (mode == "system") CHANNEL_SOUND else CHANNEL_QUIET)
                .setSmallIcon(R.drawable.ic_stat_prayer)
                .setContentTitle(title)
                .setContentText("${place.name} · ${Fmt.hm(prayerAt, place.zone)}")
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(open)
                .setGroup("prayer")
                .build()
            runCatching { NotificationManagerCompat.from(context).notify("prayer", (prayerAt.epochSecond / 60).toInt(), n) }
            if (mode != "system" && NotificationSounds.canPlayNow(context)) {
                // Alıcının süresi sınırlı: en geç 9 sn'de kapanır
                val main = Handler(Looper.getMainLooper())
                var finished = false
                val finish = { if (!finished) { finished = true; done() } }
                main.postDelayed({ finish() }, 9000)
                NotificationSounds.play(context, NotificationSounds.filesFor(context, prayer, lead, mode)) { main.post { finish() } }
                return
            }
        }
        done()
    }
}

/** Vakit alarmı geldi: bildirim + ses, sonra plan yeniden kurulur */
class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = Prayer.entries.firstOrNull { it.key == intent.getStringExtra("prayer") }
        val at = intent.getLongExtra("at", 0)
        val pending = goAsync()
        PrayerNotifier.reschedule(context)
        if (prayer == null || at == 0L || !NotifyPrefs.current.enabled) {
            pending.finish()
            return
        }
        PrayerNotifier.deliver(context, prayer, Instant.ofEpochMilli(at), intent.getIntExtra("lead", 0)) { pending.finish() }
    }
}

/**
 * Telefon açılınca, saat ya da saat dilimi değişince, uygulama güncellenince ve tam zamanlı alarm izni değişince
 * plan yeniden kurulur (alarmlar yeniden başlatmada silinir). Uygulama sınıfı (SaatTarihApp) önce çalıştığı için
 * metinler ve tercihler hazırdır.
 */
class PrayerRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerNotifier.reschedule(context)
    }
}
