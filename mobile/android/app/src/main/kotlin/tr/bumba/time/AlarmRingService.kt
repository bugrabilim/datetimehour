package tr.bumba.time

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import kotlin.math.min

/**
 * Kademeli alarm sesi (30 Ekim 2026, rakip analizi #3): bildirim kanalının sesi sonradan yükseltilemediği için kademeli
 * seçili alarmda bildirim sessiz kanaldan gelir, alarmın sesi bu ön plan hizmetinde (tür mediaPlayback) alarm ses
 * düzeyiyle döngüde çalar ve ilk 30 sn'de kısıktan tam sese çıkar; titreşim de buradan. Kapat/ertele ya da 10 dk sonra
 * durur. Hizmet başlatılamazsa (izin, arka plan kısıtı) AlarmScheduler normal sesli bildirime döner.
 */
class AlarmRingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var started = 0L
    /** Saati sesli söyleme (rakip analizi #20): 20 sn'de bir, konuşurken zil kısılır */
    private var tts: android.speech.tts.TextToSpeech? = null
    private var rising = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(AlarmScheduler.EXTRA_ID)
        if (id == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: L("tools.alarm.defaultLabel")
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: "alarm"
        val n = AlarmScheduler.notification(this, id, title, kind, silent = true)
        if (Build.VERSION.SDK_INT >= 29) startForeground(id.hashCode(), n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(id.hashCode(), n)
        release()
        current = id
        val item = AlarmItem.find(id)
        val uri: Uri? = item?.sound?.takeIf { it.isNotEmpty() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(this@AlarmRingService, uri!!)
                setWakeMode(this@AlarmRingService, PowerManager.PARTIAL_WAKE_LOCK)
                isLooping = true
                rising = item?.rise == true
                setVolume(if (rising) MIN_VOLUME else 1f, if (rising) MIN_VOLUME else 1f)
                prepare()
                start()
            }
        }.getOrNull()
        if (item?.vibrate != false) vibrate()
        started = SystemClock.elapsedRealtime()
        if (rising) handler.post(ramp)
        if (item?.speak == true) startSpeech()
        // Kimse kapatmazsa 10 dk sonra susar (bildirim kalkar)
        handler.postDelayed({ AlarmScheduler.stop(this, id) }, 10 * 60_000L)
        return START_NOT_STICKY
    }

    private val ramp = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime() - started
            val v = volumeAt(elapsed)
            runCatching { player?.setVolume(v, v) }
            if (elapsed < RISE_MS) handler.postDelayed(this, 250)
        }
    }

    private fun startSpeech() {
        tts = android.speech.tts.TextToSpeech(this) { status ->
            val t = tts ?: return@TextToSpeech
            if (status != android.speech.tts.TextToSpeech.SUCCESS) return@TextToSpeech
            val ok = t.setLanguage(Strings.locale)
            // Azericede ses yoksa Türkçe ses (bildirim seslerindeki gibi)
            if (ok < android.speech.tts.TextToSpeech.LANG_AVAILABLE && Strings.lang == "az") t.setLanguage(java.util.Locale("tr"))
            t.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            handler.postDelayed(say, 1500)
        }
    }

    private val say = object : Runnable {
        override fun run() {
            val t = tts ?: return
            val now = java.time.ZonedDateTime.now()
            runCatching { player?.setVolume(0.15f, 0.15f) }
            t.speak(T("alarm.speakText", Fmt.time(now)), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "say")
            // Konuşma bitince zil eski düzeyine döner
            handler.postDelayed({
                val v = if (rising) volumeAt(SystemClock.elapsedRealtime() - started) else 1f
                runCatching { player?.setVolume(v, v) }
            }, 3000)
            handler.postDelayed(this, 20_000)
        }
    }

    private fun vibrate() {
        val v = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        vibrator = v
        val pattern = longArrayOf(0, 600, 400, 600, 400, 600, 1500)
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) v?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            else @Suppress("DEPRECATION") v?.vibrate(pattern, 0)
        }
    }

    private fun release() {
        handler.removeCallbacksAndMessages(null)
        runCatching { player?.stop() }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    override fun onDestroy() {
        release()
        current = null
        super.onDestroy()
    }

    companion object {
        const val RISE_MS = 30_000L
        const val MIN_VOLUME = 0.03f

        /** Şu an kademeli çalan alarmın kimliği */
        @Volatile var current: String? = null
            private set

        /** Geçen süreye göre ses düzeyi: 30 sn'de kısıktan tam sese (kulakta daha doğal artış için kare eğri) */
        fun volumeAt(elapsedMs: Long): Float {
            val x = min(1f, elapsedMs.coerceAtLeast(0) / RISE_MS.toFloat())
            return MIN_VOLUME + (1f - MIN_VOLUME) * x * x
        }

        fun start(c: Context, id: String, title: String, kind: String): Boolean = runCatching {
            ContextCompat.startForegroundService(c, Intent(c, AlarmRingService::class.java)
                .putExtra(AlarmScheduler.EXTRA_ID, id).putExtra(AlarmScheduler.EXTRA_TITLE, title).putExtra(AlarmScheduler.EXTRA_KIND, kind))
            true
        }.getOrDefault(false)

        /** Bu alarm kademeli çalıyorsa susturur */
        fun stop(c: Context, id: String) {
            if (current != null && current == id) c.stopService(Intent(c, AlarmRingService::class.java))
        }
    }
}
