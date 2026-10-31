package tr.bumba.time

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import tr.bumba.time.core.MotionCounter
import tr.bumba.time.core.SmartWake

/**
 * Akıllı alarm (31 Ekim 2026, sahibin kararı: uyku takibinden yalnız akıllı alarm; iOS'taki SmartSleep ile aynı kural).
 * Kullanıcı Uyku ekranında "Akıllı alarmla uyu"ya basınca, akıllı alarm penceresi seçili (alarm başına `smart` dk) sıradaki
 * alarm için oturum açılır. Ön plan hizmeti (tür health; önkoşul HIGH_SAMPLING_RATE_SENSORS, izin istemez) pencere başına
 * kadar uyur (tam zamanlı alarmla uyanır), pencerede telefonun ivmeölçerini saniyede ~10 kez okur (SmartWake) ve hafif uyku
 * belirtisi (iki dakika hareket) görünce alarmı hemen çaldırır; o alarmın asıl çalması atlanır. Hareket yoksa alarm her
 * zamanki gibi saatinde çalar (AlarmManager), oturum alarmdan 2 dk sonra kendiliğinden kapanır. Mikrofon ve kayıt yok;
 * hareket verisi yalnız bellekte, son 10 dakika.
 */
object SmartSleep {
    const val ACTION_START = "tr.bumba.time.SMART_START"
    const val ACTION_WINDOW = "tr.bumba.time.SMART_WINDOW"
    const val ACTION_STOP = "tr.bumba.time.SMART_STOP"
    const val CHANNEL = "smart"
    const val NID = 7301
    private const val KEY = "smart.session"

    data class Session(val id: String, val at: Long, val window: Int) {
        val windowStart: Long get() = at - window * 60_000L
    }

    /** Ekranda gösterilen: pencerede hareketli bulunan dakika sayısı ve pencerede izleniyor mu */
    @Volatile var movingMinutes = 0
    @Volatile var watching = false

    /** Akıllı alarm penceresi seçili sıradaki alarm ve çalma anı */
    fun next(now: Long = System.currentTimeMillis()): Pair<AlarmItem, Long>? =
        AlarmItem.load().filter { it.enabled && it.smart > 0 }.map { it to it.nextAt(now) }
            .filter { it.second != AlarmItem.NEVER }.minByOrNull { it.second }

    var session: Session?
        get() = Prefs.store.getString(KEY, null)?.split("|")?.takeIf { it.size == 3 }?.let {
            runCatching { Session(it[0], it[1].toLong(), it[2].toInt()) }.getOrNull()
        }?.takeIf { System.currentTimeMillis() < it.at + SmartWake.AFTER_MS }
        set(v) { Prefs.store.edit().apply { if (v == null) remove(KEY) else putString(KEY, "${v.id}|${v.at}|${v.window}") }.apply() }

    val running: Boolean get() = session != null

    /** Oturumu başlatır; akıllı alarm yoksa false */
    fun start(c: Context): Boolean {
        val (item, at) = next() ?: return false
        session = Session(item.id, at, item.smart)
        movingMinutes = 0
        watching = false
        return runCatching {
            ContextCompat.startForegroundService(c, Intent(c, SmartSleepService::class.java).setAction(ACTION_START))
            true
        }.getOrDefault(false).also { if (!it) session = null }
    }

    fun stop(c: Context) {
        session = null
        watching = false
        c.stopService(Intent(c, SmartSleepService::class.java))
        windowPending(c, PendingIntent.FLAG_NO_CREATE)?.let { c.getSystemService(AlarmManager::class.java).cancel(it) }
    }

    fun windowPending(c: Context, flags: Int): PendingIntent? = PendingIntent.getForegroundService(
        c, NID + 1, Intent(c, SmartSleepService::class.java).setAction(ACTION_WINDOW), flags or PendingIntent.FLAG_IMMUTABLE)

    /** Erken çalma: asıl çalma atlanır (bir kezlik alarm kapanır), alarm şimdi çalar */
    fun fireEarly(c: Context, s: Session) {
        val item = AlarmItem.find(s.id) ?: return
        val items = AlarmItem.load().map {
            if (it.id != s.id) it else if (it.days.isEmpty()) it.copy(enabled = false, skipUntil = 0) else it.copy(skipUntil = s.at + 60_000L)
        }
        AlarmItem.save(items)
        AlarmScheduler.sync(c, items)
        Prefs.store.edit().remove("alarm.snoozed.${s.id}").apply()
        AlarmScheduler.ring(c, s.id, item.title, "alarm")
    }

    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, L("alarm.smart"), NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        }
    }
}

class SmartSleepService : Service(), SensorEventListener {
    private val handler = Handler(Looper.getMainLooper())
    private var detector = SmartWake()
    private var lock: PowerManager.WakeLock? = null
    private var listening = false

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(s: SmartSleep.Session): android.app.Notification {
        SmartSleep.ensureChannel(this)
        val from = Fmt.time(java.time.Instant.ofEpochMilli(s.windowStart).atZone(java.time.ZoneId.systemDefault()))
        val to = Fmt.time(java.time.Instant.ofEpochMilli(s.at).atZone(java.time.ZoneId.systemDefault()))
        val open = PendingIntent.getActivity(this, SmartSleep.NID,
            Intent(this, MainActivity::class.java).putExtra("startTab", Tab.TOOLS.ordinal).putExtra("tool", "sleep")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, SmartSleep.NID + 2,
            Intent(this, SmartSleepService::class.java).setAction(SmartSleep.ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, SmartSleep.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(L("smart.notif"))
            .setContentText(T("smart.notifText", from, to))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, L("smart.stop"), stop)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val s = SmartSleep.session
        if (intent?.action == SmartSleep.ACTION_STOP) {
            SmartSleep.session = null
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        // startForegroundService ile gelindiyse önce ön plana geçilir (oturum bitmiş olsa da), sonra gerekirse durulur
        val n = notification(s ?: SmartSleep.Session("", System.currentTimeMillis(), 0))
        if (Build.VERSION.SDK_INT >= 34) startForeground(SmartSleep.NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        else startForeground(SmartSleep.NID, n)
        if (s == null) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        val now = System.currentTimeMillis()
        handler.removeCallbacksAndMessages(null)
        // Alarm çaldıktan sonra oturum kapanır
        handler.postDelayed({ SmartSleep.stop(this) }, (s.at + SmartWake.AFTER_MS - now).coerceAtLeast(1000L))
        if (now >= s.windowStart) listen(s) else waitForWindow(s)
        return START_NOT_STICKY
    }

    /** Pencere başına kadar uyur: tam zamanlı alarm hizmeti yeniden başlatır */
    private fun waitForWindow(s: SmartSleep.Session) {
        val am = getSystemService(AlarmManager::class.java)
        val pi = SmartSleep.windowPending(this, PendingIntent.FLAG_UPDATE_CURRENT)!!
        if (AlarmScheduler.canExact(this)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.windowStart, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.windowStart, pi)
    }

    private fun listen(s: SmartSleep.Session) {
        if (listening) return
        val sm = getSystemService(SensorManager::class.java)
        val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        val pm = getSystemService(PowerManager::class.java)
        lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SaatTarih:smart").apply {
            setReferenceCounted(false)
            acquire((s.at + SmartWake.AFTER_MS - System.currentTimeMillis()).coerceIn(60_000L, 2 * 3600_000L))
        }
        detector = SmartWake()
        listening = sm.registerListener(this, acc, 100_000)
        SmartSleep.watching = listening
    }

    override fun onSensorChanged(e: SensorEvent) {
        val s = SmartSleep.session ?: return
        val now = System.currentTimeMillis()
        val g = MotionCounter.G
        detector.add(now, e.values[0] / g, e.values[1] / g, e.values[2] / g)
        SmartSleep.movingMinutes = detector.movingMinutes
        if (detector.shouldWake(now, s.at, s.window)) {
            SmartSleep.session = null
            SmartSleep.fireEarly(this, s)
            SmartSleep.stop(this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (listening) getSystemService(SensorManager::class.java)?.unregisterListener(this)
        listening = false
        SmartSleep.watching = false
        runCatching { lock?.release() }
        lock = null
        super.onDestroy()
    }
}
