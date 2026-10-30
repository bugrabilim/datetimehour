package tr.bumba.time

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Aralık antrenmanı arka planda (30 Ekim 2026, sahibin isteği: "uygulama kapalıyken de çalışması şart"): antrenman
 * sürerken ön plan hizmeti çalışır; ekran kapalıyken ya da başka uygulamadayken de evre geçişlerini ("Çalış",
 * "Dinlen", "Son tur") ve son 3 saniyeyi telefonun konuşma motoruyla söyler, bildirimde evre, tur ve kalan süre durur.
 * Durum Prefs'teki hiit.start / hiit.paused / hiit.cfg kayıtlarıdır; duraklatınca, sıfırlayınca ya da bitince durur.
 * Bitişte ayrıca "hiit" alarmı çalar (AlarmScheduler). Tür specialUse: Play Console'da açıklama ister.
 */
class HiitService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    /** Konuşma motoru hazır olmadan gelen son duyuru */
    private var pending: String? = null
    private var last: HiitConfig.Phase? = null
    private var wake: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Speaker.locale()
                pending?.let { say(it) }
            }
            pending = null
        }
        // Ekran kapalıyken saniye sayacı uyumasın (en çok 3 saat; antrenman bitince bırakılır)
        wake = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SaatTarih:hiit")
            ?.apply { acquire(3 * 3600_000L) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        val cfg = HiitConfig.load()
        val start = Prefs.store.getLong("hiit.start", 0L)
        val n = notification(cfg, cfg.phase(elapsed(start)), start)
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ID, n)
        // Sürdürünce o anki evre yeniden söylenir
        last = null
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_NOT_STICKY
    }

    private val tick = object : Runnable {
        override fun run() {
            val start = Prefs.store.getLong("hiit.start", 0L)
            if (start == 0L || Prefs.store.getLong("hiit.paused", 0L) > 0) {
                stopSelf()
                return
            }
            val cfg = HiitConfig.load()
            val p = cfg.phase(elapsed(start))
            cfg.cue(last, p)?.let { say(it) }
            if (last?.index != p.index) {
                getSystemService(NotificationManager::class.java)?.notify(ID, notification(cfg, p, start))
            }
            last = p
            if (p.kind == "done") {
                // "Bitti" söylensin, sonra hizmet kapanır (bitiş alarmı ayrıca çalar)
                handler.postDelayed({ stopSelf() }, 4000)
                return
            }
            handler.postDelayed(this, 200)
        }
    }

    private fun say(text: String) {
        if (!Speaker.on) return
        if (!ready) { pending = text; return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hiit")
    }

    /** Evre ve tur; kalan süre bildirimde geri sayan saat olarak (her saniye yenilenmesi gerekmez) */
    private fun notification(cfg: HiitConfig, p: HiitConfig.Phase, start: Long): Notification {
        val phaseEnd = start + (elapsed(start) + p.left) * 1000L
        val open = PendingIntent.getActivity(
            this, ID,
            Intent(this, MainActivity::class.java).putExtra("startTab", Tab.TOOLS.ordinal).putExtra("tool", "timer")
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val round = if (p.kind == "work" || p.kind == "rest") T("hiit.round", p.round, cfg.rounds) else null
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(listOfNotNull(cfg.title(p), round).joinToString(" · "))
            .setContentText(L("hiit.channel"))
            .setWhen(phaseEnd)
            .setShowWhen(p.kind != "done")
            .setUsesChronometer(p.kind != "done")
            .setChronometerCountDown(true)
            .setOngoing(p.kind != "done")
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts?.shutdown()
        tts = null
        wake?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    companion object {
        private const val ID = 7301
        private const val CHANNEL = "hiit"

        private fun elapsed(start: Long) = ((System.currentTimeMillis() - start) / 1000).toInt()

        /** Antrenman sürüyorsa hizmeti başlatır, sürmüyorsa durdurur (ekrandan ve uygulama öne gelince) */
        fun sync(c: Context) {
            val start = Prefs.store.getLong("hiit.start", 0L)
            val running = start > 0 && Prefs.store.getLong("hiit.paused", 0L) == 0L &&
                HiitConfig.load().phase(elapsed(start)).kind != "done"
            val intent = Intent(c, HiitService::class.java)
            if (running) runCatching { ContextCompat.startForegroundService(c, intent) } else c.stopService(intent)
        }

        private fun ensureChannel(c: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = c.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL) != null) return
            nm.createNotificationChannel(NotificationChannel(CHANNEL, L("hiit.channel"), NotificationManager.IMPORTANCE_LOW))
        }
    }
}
