package tr.bumba.time

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextClock
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Yüzen saat (rakip analizi listesi #80; iOS'ta resim içinde resim): başka uygulamaların üstünde küçük, sürüklenebilir
 * saat penceresi (TYPE_APPLICATION_OVERLAY, "Diğer uygulamaların üzerinde göster" izniyle). Saat sistemin TextClock'uyla
 * kendi işler; pencere ön plan hizmetinde durur (bildirimde "Kapat"). Konum `float.x`/`float.y`.
 */
class FloatingClockService : Service() {
    private var view: TextClock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        ensureChannel(this)
        val n = notification()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(ID, n)
        if (view == null) show()
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun show() {
        val wm = getSystemService(WindowManager::class.java) ?: return
        val dp = resources.displayMetrics.density
        val clock = TextClock(this).apply {
            format24Hour = "HH:mm:ss"
            format12Hour = "h:mm:ss a"
            timeZone = null
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            setPadding((14 * dp).toInt(), (6 * dp).toInt(), (14 * dp).toInt(), (6 * dp).toInt())
            background = GradientDrawable().apply { cornerRadius = 18 * dp; setColor(0xCC1B1F27.toInt()) }
            contentDescription = L("float.title")
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = Prefs.store.getInt("float.x", (16 * dp).toInt())
            y = Prefs.store.getInt("float.y", (120 * dp).toInt())
        }
        // Sürükleyerek taşınır; bırakınca konum saklanır
        var sx = 0f; var sy = 0f; var px = 0; var py = 0
        clock.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = params.x; py = params.y }
                MotionEvent.ACTION_MOVE -> {
                    params.x = px + (e.rawX - sx).toInt(); params.y = py + (e.rawY - sy).toInt()
                    runCatching { wm.updateViewLayout(clock, params) }
                }
                MotionEvent.ACTION_UP -> Prefs.store.edit().putInt("float.x", params.x).putInt("float.y", params.y).apply()
            }
            true
        }
        runCatching { wm.addView(clock, params); view = clock }
    }

    override fun onDestroy() {
        view?.let { v -> runCatching { getSystemService(WindowManager::class.java)?.removeView(v) } }
        view = null
        Prefs.store.edit().putBoolean(KEY, false).apply()
        super.onDestroy()
    }

    private fun notification(): Notification {
        val stop = PendingIntent.getService(this, 7201, Intent(this, FloatingClockService::class.java).setAction(STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile_clock)
            .setContentTitle(L("float.title"))
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, L("float.stop"), stop)
            .setContentIntent(stop)
            .build()
    }

    companion object {
        const val KEY = "float.on"
        private const val ID = 7202
        private const val CHANNEL = "float"
        private const val STOP = "tr.bumba.time.FLOAT_STOP"

        fun start(c: Context) {
            Prefs.store.edit().putBoolean(KEY, true).apply()
            runCatching { ContextCompat.startForegroundService(c, Intent(c, FloatingClockService::class.java)) }
        }

        fun stop(c: Context) {
            Prefs.store.edit().putBoolean(KEY, false).apply()
            c.stopService(Intent(c, FloatingClockService::class.java))
        }

        private fun ensureChannel(c: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = c.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL) != null) return
            nm.createNotificationChannel(NotificationChannel(CHANNEL, L("float.channel"), NotificationManager.IMPORTANCE_LOW))
        }
    }
}

/** Saat görünümü sayfasında: aç/kapat; izin yoksa sistemin izin ekranı açılır */
@Composable
fun FloatingClockCard() {
    val context = LocalContext.current
    var on by remember { mutableStateOf(Prefs.store.getBoolean(FloatingClockService.KEY, false)) }
    AppCard {
        SectionTitle(L("float.title"))
        Column(Modifier.fillMaxWidth()) {
            TextButton(onClick = {
                if (on) { FloatingClockService.stop(context); on = false }
                else if (!Settings.canDrawOverlays(context)) {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                } else { FloatingClockService.start(context); on = true }
            }) { Text(L(if (on) "float.stop" else "float.start"), color = Theme.c.accent) }
            Text(L("float.noteAndroid"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}
