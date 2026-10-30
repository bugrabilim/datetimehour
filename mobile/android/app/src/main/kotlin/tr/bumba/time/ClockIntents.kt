package tr.bumba.time

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.service.dreams.DreamService
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/*
 * Asistan, kısayollar ve ekran koruyucu (rakip analizi #7; iOS'taki Siri/Kısayollar karşılığı).
 * Android'in saat uygulaması istekleri (AlarmClock.ACTION_SET_TIMER / ACTION_SET_ALARM / SHOW_*): Google Asistan ve
 * başka uygulamalar Saat Tarih'e zamanlayıcı ya da alarm kurdurabilir. Çağıran uygulamanın SET_ALARM izni olmalı
 * (AOSP saat uygulamasındaki gibi). EXTRA_SKIP_UI doğruysa ekran açılmaz, kısa bir bildirim yazısı çıkar.
 */
class ClockIntentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        finish()
    }

    private fun handle(i: Intent) {
        val skipUi = i.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false)
        val message = i.getStringExtra(AlarmClock.EXTRA_MESSAGE).orEmpty()
        when (i.action) {
            AlarmClock.ACTION_SET_TIMER -> {
                val sec = i.getIntExtra(AlarmClock.EXTRA_LENGTH, 0)
                if (sec > 0) {
                    TimerItem.startNew(this, sec * 1000L, message)
                    BadgeStore.hit(Badge.ZAMANLAYICI)
                    Toast.makeText(this, T("intent.timer.done", (sec + 59) / 60), Toast.LENGTH_SHORT).show()
                }
                if (!skipUi || sec <= 0) open("timer")
            }
            AlarmClock.ACTION_SET_ALARM -> {
                val hour = i.getIntExtra(AlarmClock.EXTRA_HOUR, -1)
                if (hour in 0..23) {
                    val minute = i.getIntExtra(AlarmClock.EXTRA_MINUTES, 0).coerceIn(0, 59)
                    val item = AlarmItem(hour = hour, minute = minute, label = message.take(40), days = isoDays(i))
                    AlarmItem.addNew(this, item)
                    BadgeStore.hit(Badge.ALARM)
                    Toast.makeText(this, T("intent.alarm.done", item.timeText), Toast.LENGTH_SHORT).show()
                }
                if (!skipUi || hour !in 0..23) open("alarm")
            }
            AlarmClock.ACTION_SHOW_ALARMS -> open("alarm")
            AlarmClock.ACTION_SHOW_TIMERS -> open("timer")
        }
    }

    private fun open(tool: String) = startActivity(
        Intent(this, MainActivity::class.java).putExtra("shortcut", tool)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
    )

    companion object {
        /** AlarmClock.EXTRA_DAYS (Calendar: pazar 1 … cumartesi 7) → ISO (pazartesi 1 … pazar 7) */
        fun isoDays(i: Intent): List<Int> =
            i.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS).orEmpty().map { if (it == 1) 7 else it - 1 }.filter { it in 1..7 }.distinct().sorted()
    }
}

/**
 * Ekran koruyucu (Daydream): şarjdayken ya da yuvadayken tam ekran saat. Telefon Ayarları › Ekran › Ekran koruyucu ›
 * Saat Tarih. Gece modu ayarı (Ayarlar › Saat görünümü) burada da geçerlidir; dokununca ekran koruyucu kapanır.
 */
class ClockDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    override fun onCreate() {
        super.onCreate()
        saved.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ClockDreamService)
            setViewTreeSavedStateRegistryOwner(this@ClockDreamService)
            setViewTreeViewModelStoreOwner(this@ClockDreamService)
            setContent { AppTheme { FullscreenClock(controls = false) { finish() } } }
        }
        window?.decorView?.let {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
            it.setViewTreeViewModelStoreOwner(this)
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        super.onDetachedFromWindow()
    }
}
