package tr.bumba.time

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import tr.bumba.time.core.FocusLog
import tr.bumba.time.core.NoiseGen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/* Pomodoro istatistikleri ve arka plan sesi (30 Ekim 2026, rakip analizi #16; sitedeki pomodoroyla aynı tasarım) ve
   uyku sesleri (#17) için ortak ses hizmeti. Kayıt "pomo.log" (JSON, sitedeki sth-pomo-log biçimi), yalnız telefonda. */

object PomoLog {
    const val KEY = "pomo.log"

    fun load(): List<FocusLog.Entry> = runCatching {
        val a = JSONArray(Prefs.store.getString(KEY, "[]"))
        (0 until a.length()).map { i -> a.getJSONObject(i).let { FocusLog.Entry(it.getString("d"), it.getInt("m"), it.optString("t")) } }
    }.getOrDefault(emptyList())

    fun save(log: List<FocusLog.Entry>) {
        val a = JSONArray()
        for (e in log) a.put(JSONObject().put("d", e.d).put("m", e.m).put("t", e.t))
        Prefs.store.edit().putString(KEY, a.toString()).apply()
    }

    /** Tamamlanan çalışma turu (çalışmanın bittiği yerel güne yazılır) */
    fun record(minutes: Int, task: String, atMs: Long) {
        val day = Instant.ofEpochMilli(atMs).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        save(FocusLog.add(load(), FocusLog.Entry(day, minutes, task)))
    }

    /** Son 7 günün anahtarları, eskiden yeniye */
    fun lastDays(today: LocalDate = LocalDate.now()) = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
}

/** Gürültü türünün adı */
fun noiseName(kind: String) = when (kind) {
    "white" -> L("tools.pomodoro.noiseWhite")
    "pink" -> L("tools.pomodoro.noisePink")
    "brown" -> L("tools.pomodoro.noiseBrown")
    "rain" -> L("tools.pomodoro.noiseRain")
    "waves" -> L("noise.waves")
    else -> L("tools.pomodoro.noiseOff")
}

/**
 * Telefonda üretilen gürültüyü döngüde çalan ön plan hizmeti (tür mediaPlayback; ekran kapalıyken de sürer). `until`
 * gelince durur, uyku seslerinde son 30 sn'de kısılarak. Aynı anda tek kaynak: pomodoro ("pomo") ya da uyku ("sleep").
 * Pomodoroda bildirim süren pomodoro bildiriminin yerini alır (LiveNotifications o sırada kendi bildirimini çizmez).
 */
class SoundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var track: AudioTrack? = null
    private var kind = ""
    private var until = 0L
    private var fade = false
    private var gain = 0.4f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            finish()
            return START_NOT_STICKY
        }
        val k = intent.getStringExtra(EXTRA_KIND) ?: ""
        val src = intent.getStringExtra(EXTRA_SOURCE) ?: "pomo"
        until = intent.getLongExtra(EXTRA_UNTIL, 0L)
        fade = intent.getBooleanExtra(EXTRA_FADE, false)
        gain = NoiseGen.gain(intent.getIntExtra(EXTRA_VOLUME, 40))
        if (k !in NoiseGen.KINDS || (until in 1..System.currentTimeMillis())) {
            finish()
            return START_NOT_STICKY
        }
        source = src
        val n = notification(this, src, k, until)
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(NID, n)
        }.onFailure { finish(); return START_NOT_STICKY }
        if (src == "pomo") LiveNotifications.sync(this) // pomodoronun ayrı bildirimi kalksın
        if (k != kind || track == null) play(k) else applyVolume()
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_NOT_STICKY
    }

    private fun play(k: String) {
        release()
        kind = k
        // Arabellek (8–10 sn) arka planda üretilir, sonra ana iş parçacığında çalınır
        Thread {
            val samples = NoiseGen.samples(k)
            val pcm = ShortArray(samples.size) { (samples[it] * 32767).toInt().toShort() }
            handler.post { if (kind == k && track == null && source.isNotEmpty()) start(pcm) }
        }.start()
    }

    private fun start(pcm: ShortArray) {
        track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(NoiseGen.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build().apply {
                    write(pcm, 0, pcm.size)
                    setLoopPoints(0, pcm.size, -1)
                    setVolume(gain)
                    play()
                }
        }.getOrNull()
        if (track == null) finish()
    }

    private val tick = object : Runnable {
        override fun run() {
            if (until > 0 && System.currentTimeMillis() >= until) { finish(); return }
            applyVolume()
            handler.postDelayed(this, 500)
        }
    }

    private fun applyVolume() {
        var v = gain
        if (fade && until > 0) {
            val left = until - System.currentTimeMillis()
            if (left < 30_000) v *= (left.coerceAtLeast(0) / 30_000f)
        }
        runCatching { track?.setVolume(v) }
    }

    private fun release() {
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
        kind = ""
    }

    private fun finish() {
        handler.removeCallbacksAndMessages(null)
        release()
        val wasPomo = source == "pomo"
        source = ""
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
        stopSelf()
        if (wasPomo) LiveNotifications.sync(this)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        release()
        source = ""
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "tr.bumba.time.SOUND_STOP"
        const val EXTRA_KIND = "kind"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_UNTIL = "until"
        const val EXTRA_FADE = "fade"
        const val EXTRA_VOLUME = "volume"
        private const val NID = 7301

        /** Çalan kaynak: "pomo", "sleep" ya da "" */
        @Volatile var source = ""
            private set

        fun play(c: Context, kind: String, volume: Int, until: Long, source: String, fade: Boolean = false) {
            val i = Intent(c, SoundService::class.java).putExtra(EXTRA_KIND, kind).putExtra(EXTRA_VOLUME, volume)
                .putExtra(EXTRA_UNTIL, until).putExtra(EXTRA_SOURCE, source).putExtra(EXTRA_FADE, fade)
            runCatching { androidx.core.content.ContextCompat.startForegroundService(c, i) }
        }

        /** Durdurur; kaynak verilirse yalnız o kaynak çalıyorsa */
        fun stop(c: Context, source: String? = null) {
            if (this.source.isEmpty() || (source != null && source != this.source)) return
            runCatching { c.startService(Intent(c, SoundService::class.java).setAction(ACTION_STOP)) }
        }

        private fun notification(c: Context, source: String, kind: String, until: Long): android.app.Notification {
            channel(c)
            val sp = Prefs.store
            val title = if (source == "pomo") L("tools.pomodoro.${sp.getString("pomo.phase", "work")}") + " · " + noiseName(kind)
            else L("sleep.sounds") + " · " + noiseName(kind)
            val text = if (source == "pomo") T("tools.pomodoro.round", sp.getInt("pomo.round", 1), sp.getInt("pomo.rounds", 4)) else ""
            val tool = if (source == "pomo") "pomodoro" else "sleep"
            val stop = PendingIntent.getService(c, NID, Intent(c, SoundService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return NotificationCompat.Builder(c, LiveNotifications.CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle(title)
                .apply { if (text.isNotEmpty()) setContentText(text) }
                .apply { if (until > 0) setWhen(until).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true) }
                .setOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(0, L("noise.stop"), stop)
                .setContentIntent(PendingIntent.getActivity(c, NID,
                    Intent(c, MainActivity::class.java).putExtra("startTab", Tab.TOOLS.ordinal).putExtra("tool", tool)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .build()
        }

        private fun channel(c: Context) {
            val nm = c.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(LiveNotifications.CHANNEL) != null) return
            nm.createNotificationChannel(android.app.NotificationChannel(LiveNotifications.CHANNEL, L("live.channel"),
                NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false); setSound(null, null) })
        }
    }
}

/** Pomodoro gürültüsü: çalışma süresi işlerken ve tür seçiliyse çalar, süre bitince durur */
object PomoNoise {
    fun sync(c: Context, now: Long = System.currentTimeMillis()) {
        val sp = Prefs.store
        val end = sp.getLong("pomo.end", 0L)
        val kind = sp.getString("pomo.noise", "off") ?: "off"
        if (end > now && sp.getString("pomo.phase", "work") == "work" && kind != "off") {
            SoundService.play(c, kind, sp.getInt("pomo.vol", 40), end, "pomo")
        } else {
            SoundService.stop(c, "pomo")
        }
    }
}

/** Gürültü türü seçici ve ses düzeyi (pomodoro ve uyku sesleri) */
@Composable
fun NoiseControls(kind: String, volume: Int, allowOff: Boolean = true, onKind: (String) -> Unit, onVolume: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (k in (if (allowOff) listOf("off") else emptyList()) + NoiseGen.KINDS) {
                val on = k == kind
                Text(noiseName(k),
                    Modifier
                        .background(if (on) Theme.c.accent.copy(alpha = 0.22f) else Theme.c.surface2, RoundedCornerShape(50))
                        .clickable { onKind(k) }
                        .semantics { selected = on }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    color = if (on) Theme.c.accent else Theme.c.text, fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, null, tint = Theme.c.muted)
            Slider(volume.toFloat(), { onVolume(it.toInt()) }, Modifier.weight(1f).semantics { contentDescription = L("tools.pomodoro.volume") },
                valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = Theme.c.accent, activeTrackColor = Theme.c.accent,
                    inactiveTrackColor = Theme.c.accent.copy(alpha = 0.2f), inactiveTickColor = Theme.c.accent))
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Theme.c.muted)
        }
    }
}

/** Pomodoro istatistikleri: bugün, son 7 gün, toplam; 7 günlük çubuk grafik; en çok çalışılan 5 görev; geçmişi sil */
@Composable
fun FocusStatsCard(log: List<FocusLog.Entry>, onClear: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val days = PomoLog.lastDays()
    val s = FocusLog.stats(log, days, L("tools.pomodoro.noTask"))
    AppCard {
        SectionTitle(L("tools.pomodoro.statsHeading"))
        if (log.isEmpty()) {
            Text(L("tools.pomodoro.empty"), color = Theme.c.muted, fontSize = 14.sp)
        } else {
            Row2(L("tools.pomodoro.today"), T("tools.pomodoro.statLine", s.today.n, s.today.m))
            Row2(L("tools.pomodoro.week"), T("tools.pomodoro.statLine", s.week.n, s.week.m))
            Row2(L("tools.pomodoro.total"), T("tools.pomodoro.statLine", s.total.n, s.total.m))
            Text(L("tools.pomodoro.chartCaption"), color = Theme.c.muted, fontSize = 12.sp)
            val top = maxOf(1, s.perDay.max())
            Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                days.forEachIndexed { i, d ->
                    val label = L("calendar.weekdaysShort.${LocalDate.parse(d).dayOfWeek.value - 1}")
                    Column(Modifier.weight(1f).fillMaxHeight().clearAndSetSemantics { contentDescription = "$label: ${s.perDay[i]} ${L("tools.pomodoro.minUnit")}" },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.fillMaxWidth().height(maxOf(3f, 90f * s.perDay[i] / top).dp)
                            .background(if (i == 6) Theme.c.accent else Theme.c.accent.copy(alpha = 0.55f), RoundedCornerShape(4.dp)))
                        Spacer(Modifier.height(4.dp))
                        Text(label, color = Theme.c.muted, fontSize = 11.sp)
                    }
                }
            }
            if (s.top.isNotEmpty()) {
                Text(L("tools.pomodoro.topTasks"), color = Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                for ((name, m) in s.top) Row2(name, "$m ${L("tools.pomodoro.minUnit")}")
            }
            TextButton(onClick = { confirm = true }) { Text(L("tools.pomodoro.clear"), color = DangerRed) }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            text = { Text(L("tools.pomodoro.clearConfirm")) },
            confirmButton = { TextButton(onClick = { confirm = false; onClear() }) { Text(L("tools.pomodoro.clear"), color = DangerRed) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(L("badges.deleteNo")) } },
        )
    }
}
