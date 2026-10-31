package tr.bumba.time

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import java.time.LocalDate

/*
 * Dokunulabilen widget'lar ve Hızlı Ayarlar kutucukları (30 Ekim 2026, rakip analizi listesi #77). Android'de widget'lar
 * uygulamanın kendi kayıtlarını kullanır: zikirmatik widget'ı uygulamadaki zikir sayacını (zikir.count/total/today),
 * kronometre widget'ı ve kutucuğu uygulamadaki kronometreyi (sw.start/sw.acc) sayar. iOS'ta App Group olmadığı için
 * widget'lar kendi sayaçlarını tutar.
 */
object ToolActions {
    const val ZIKIR_TAP = "tr.bumba.time.ZIKIR_TAP"
    const val ZIKIR_RESET = "tr.bumba.time.ZIKIR_RESET"
    const val SW_TOGGLE = "tr.bumba.time.SW_TOGGLE"
    const val SW_RESET = "tr.bumba.time.SW_RESET"

    fun pending(c: Context, action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        c, code, Intent(c, ToolWidgetReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Zikir: sayaç, toplam ve bugünkü sayı (ZikirScreen ile aynı anahtarlar) */
    fun zikirTap(c: Context) {
        val sp = Prefs.store
        val today = LocalDate.now().toString()
        val todayCount = sp.getString("zikir.today", "")?.split("|")?.takeIf { it.size == 2 && it[0] == today }?.get(1)?.toIntOrNull() ?: 0
        val count = sp.getInt("zikir.count", 0) + 1
        sp.edit().putInt("zikir.count", count).putInt("zikir.total", sp.getInt("zikir.total", 0) + 1)
            .putString("zikir.today", "$today|${todayCount + 1}").apply()
        val target = sp.getInt("zikir.target", 33)
        val strong = count % 33 == 0 || (target > 0 && count == target)
        if (Build.VERSION.SDK_INT >= 29) runCatching {
            c.getSystemService(Vibrator::class.java)?.vibrate(
                VibrationEffect.createPredefined(if (strong) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_TICK))
        }
    }

    fun zikirReset() = Prefs.store.edit().putInt("zikir.count", 0).apply()

    /** Kronometre (StopwatchScreen ile aynı: başlangıç Unix ms, 0 = durdu; acc ms) */
    fun swRunning() = Prefs.store.getLong("sw.start", 0L) > 0
    fun swElapsed(now: Long = System.currentTimeMillis()): Long {
        val s = Prefs.store.getLong("sw.start", 0L)
        return Prefs.store.getLong("sw.acc", 0L) + if (s > 0) maxOf(0, now - s) else 0
    }

    fun swToggle(c: Context) {
        val now = System.currentTimeMillis()
        val e = Prefs.store.edit()
        if (swRunning()) e.putLong("sw.acc", swElapsed(now)).putLong("sw.start", 0L)
        else { e.putLong("sw.start", now); BadgeStore.hit(Badge.KRONOMETRE) }
        e.apply()
        LiveNotifications.sync(c)
    }

    fun swReset(c: Context) {
        Prefs.store.edit().putLong("sw.start", 0L).putLong("sw.acc", 0L).putString("sw.laps", "").apply()
        LiveNotifications.sync(c)
    }

    /** Widget'lar ve kutucuk yeniden çizilir */
    fun refresh(c: Context) {
        val mgr = AppWidgetManager.getInstance(c)
        for ((cls, w) in listOf(ZikirWidget::class.java to ZikirWidget(), StopwatchToolWidget::class.java to StopwatchToolWidget())) {
            for (id in mgr.getAppWidgetIds(ComponentName(c, cls))) mgr.updateAppWidget(id, w.views(c, id))
        }
        runCatching { TileService.requestListeningState(c, ComponentName(c, StopwatchTileService::class.java)) }
    }
}

class ToolWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            ToolActions.ZIKIR_TAP -> ToolActions.zikirTap(c)
            ToolActions.ZIKIR_RESET -> ToolActions.zikirReset()
            ToolActions.SW_TOGGLE -> ToolActions.swToggle(c)
            ToolActions.SW_RESET -> ToolActions.swReset(c)
        }
        ToolActions.refresh(c)
    }
}

/** Zikirmatik: dokununca bir artar (uygulamadaki sayaç), 33'te ve hedefte güçlü titreşim; ↺ sıfırlar */
class ZikirWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_zikir)
        val sp = Prefs.store
        val count = sp.getInt("zikir.count", 0)
        val target = sp.getInt("zikir.target", 33)
        v.setTextViewText(R.id.title, L("zikir.kind.${sp.getString("zikir.kind", "subhanallah")}"))
        v.setTextViewText(R.id.count, "$count")
        v.setTextViewText(R.id.target, if (target > 0) "/ $target" else "")
        v.setOnClickPendingIntent(R.id.tap, ToolActions.pending(c, ToolActions.ZIKIR_TAP, 7100))
        v.setOnClickPendingIntent(R.id.reset, ToolActions.pending(c, ToolActions.ZIKIR_RESET, 7101))
        v.setContentDescription(R.id.reset, L("widget.reset"))
        return v
    }
}

/** Kronometre: başlat/durdur ve sıfırla; süre sistemin Chronometer'ıyla akar */
class StopwatchToolWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_stopwatch)
        val running = ToolActions.swRunning()
        val elapsed = ToolActions.swElapsed()
        v.setTextViewText(R.id.title, L("tools.hub.cards.stopwatch.name"))
        v.setChronometer(R.id.time, SystemClock.elapsedRealtime() - elapsed, null, running)
        v.setTextViewText(R.id.toggle, if (running) "❚❚" else "▶")
        v.setContentDescription(R.id.toggle, L(if (running) "tools.stopwatch.stop" else "tools.stopwatch.start"))
        v.setContentDescription(R.id.reset, L("widget.reset"))
        v.setOnClickPendingIntent(R.id.toggle, ToolActions.pending(c, ToolActions.SW_TOGGLE, 7102))
        v.setOnClickPendingIntent(R.id.reset, ToolActions.pending(c, ToolActions.SW_RESET, 7103))
        v.setOnClickPendingIntent(R.id.title, WidgetUpdater.openApp(c, Tab.TOOLS, id))
        return v
    }
}

/** Hızlı Ayarlar: tam ekran saati açar */
class ClockTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply { label = L("control.clock"); state = Tile.STATE_INACTIVE; updateTile() }
    }

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).putExtra("shortcut", "fullscreen")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 7104, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

/** Hızlı Ayarlar: kronometreyi başlatır/durdurur (uygulamadaki kronometre); çalışırken kutucuk etkin */
class StopwatchTileService : TileService() {
    override fun onStartListening() = draw()

    override fun onClick() {
        ToolActions.swToggle(this)
        ToolActions.refresh(this)
        draw()
    }

    private fun draw() {
        val running = ToolActions.swRunning()
        qsTile?.apply {
            label = L("tools.hub.cards.stopwatch.name")
            state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= 29) subtitle = L(if (running) "tile.stopwatchRunning" else "tile.stopwatchStopped")
            icon = Icon.createWithResource(this@StopwatchTileService, R.drawable.ic_tile_stopwatch)
            updateTile()
        }
    }
}
