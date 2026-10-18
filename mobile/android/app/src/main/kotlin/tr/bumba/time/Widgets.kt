package tr.bumba.time

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ana ekran widget'ları (iOS'taki widget'larla aynı beşli): Sıradaki vakit, Bugünün vakitleri, Analog saat, Dijital
 * saat, Saat ve tarih. Saatler sistemin TextClock/AnalogClock'uyla kendi kendine işler (uygulama uyanmaz, saniye yok);
 * kalan süre Chronometer ile geri sayar. Vakit geçince ve gece yarısı WidgetUpdater yeniden çizer. İnternet gerekmez.
 * Vakit widget'larında yer widget ayarındadır: uygulamadaki konum (varsayılan) ya da 81 il. Widget'lar varsayılan
 * temadadır (uygulama rengi geçmez; iOS'ta da öyle).
 */
abstract class BaseWidget : AppWidgetProvider() {
    abstract fun views(c: Context, id: Int): RemoteViews

    override fun onUpdate(c: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) mgr.updateAppWidget(id, views(c, id))
        WidgetUpdater.schedule(c)
    }

    override fun onAppWidgetOptionsChanged(c: Context, mgr: AppWidgetManager, id: Int, options: Bundle?) {
        mgr.updateAppWidget(id, views(c, id))
    }

    override fun onReceive(c: Context, intent: Intent) {
        super.onReceive(c, intent)
        // Saat, saat dilimi ya da dil değişince (manifestte kayıtlı; bu yayınlar arka planda da gelir)
        if (intent.action in setOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED)) {
            WidgetUpdater.updateAll(c)
        }
    }

    override fun onDeleted(c: Context, ids: IntArray) {
        val e = Prefs.store.edit()
        for (id in ids) e.remove(WidgetUpdater.placeKey(id))
        e.apply()
    }
}

class NextPrayerWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val place = WidgetUpdater.place(id)
        val now = Instant.now()
        val v = RemoteViews(c.packageName, R.layout.widget_next)
        v.setTextViewText(R.id.place, "📍 " + place.name)
        place.next(now)?.let { n ->
            v.setTextViewText(R.id.name, n.prayer.title)
            v.setTextViewText(R.id.time, Fmt.hm(n.at, place.zone))
            WidgetUpdater.countdown(v, R.id.left, n.at, now)
        }
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.PRAYER, id))
        return v
    }
}

class TodayPrayersWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val place = WidgetUpdater.place(id)
        val now = Instant.now()
        val v = RemoteViews(c.packageName, R.layout.widget_today)
        v.setTextViewText(R.id.place, "📍 " + place.name)
        val next = place.next(now)
        next?.let { WidgetUpdater.countdown(v, R.id.left, it.at, now) }
        val entries = place.entries(place.today(now))
        val cols = listOf(R.id.c0, R.id.c1, R.id.c2, R.id.c3, R.id.c4, R.id.c5)
        val names = listOf(R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5)
        val times = listOf(R.id.t0, R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5)
        for (i in cols.indices) {
            val e = entries.getOrNull(i)
            if (e == null) {
                v.setViewVisibility(cols[i], View.GONE)
                continue
            }
            v.setViewVisibility(cols[i], View.VISIBLE)
            val isNext = e.at == next?.at
            v.setTextViewText(names[i], e.prayer.title)
            v.setTextViewText(times[i], Fmt.hm(e.at, place.zone))
            v.setInt(cols[i], "setBackgroundResource", if (isNext) R.drawable.widget_soft else 0)
        }
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.PRAYER, id))
        return v
    }
}

class AnalogClockWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_analog)
        // Android 12+: uygulamadaki gibi rakamlı kadran (daha eskide rakamsız vektör kadran)
        if (Build.VERSION.SDK_INT >= 31) v.setIcon(R.id.analog, "setDial", Icon.createWithBitmap(WidgetUpdater.dial(c)))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.CLOCK, id))
        return v
    }
}

class DigitalClockWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_digital)
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.CLOCK, id))
        return v
    }
}

class ClockDateWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_clock_date)
        val today = LocalDate.now()
        v.setTextViewText(R.id.date, today.dayOfMonth.toString() + " " + Fmt.month(today.monthValue) + " " + today.year)
        v.setTextViewText(R.id.weekday, L("calendar.weekdays.${today.dayOfWeek.value - 1}"))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.CLOCK, id))
        return v
    }
}

/** Yenileme yayını: bir sonraki vakitte ya da gece yarısında gelir */
class WidgetRefreshReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) = WidgetUpdater.updateAll(c)
}

object WidgetUpdater {
    private val providers = listOf(
        NextPrayerWidget::class.java, TodayPrayersWidget::class.java, AnalogClockWidget::class.java,
        DigitalClockWidget::class.java, ClockDateWidget::class.java,
    )

    fun placeKey(id: Int) = "widget.place.$id"

    /** Widget'ın yeri: "app" (uygulamadaki konum; varsayılan) ya da il adı */
    fun place(id: Int): AppPlace {
        val v = Prefs.store.getString(placeKey(id), "app")!!
        return if (v == "app") AppPlace.current() else AppPlace.of(Province.find(v))
    }

    /** Uygulamada bir şey değişince (konum, dil) ya da yenileme anında: bütün widget'lar yeniden çizilir */
    fun updateAll(c: Context) {
        val mgr = AppWidgetManager.getInstance(c)
        for (p in providers) {
            val ids = mgr.getAppWidgetIds(ComponentName(c, p))
            if (ids.isEmpty()) continue
            val w = p.getDeclaredConstructor().newInstance()
            for (id in ids) mgr.updateAppWidget(id, w.views(c, id))
        }
        schedule(c)
    }

    /** Sonraki yenileme: vakit widget'larındaki en yakın vakit ya da gece yarısı (hangisi önceyse) */
    fun schedule(c: Context) {
        val mgr = AppWidgetManager.getInstance(c)
        val now = Instant.now()
        var next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().plusSeconds(5)
        for (p in listOf(NextPrayerWidget::class.java, TodayPrayersWidget::class.java)) {
            for (id in mgr.getAppWidgetIds(ComponentName(c, p))) {
                place(id).next(now)?.at?.let { if (it.isBefore(next)) next = it.plusSeconds(1) }
            }
        }
        val any = providers.any { mgr.getAppWidgetIds(ComponentName(c, it)).isNotEmpty() }
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            c, 0, Intent(c, WidgetRefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (!any) { am.cancel(pi); return }
        val at = next.toEpochMilli()
        // Tam zamanlı alarm izni varsa vaktinde, yoksa birkaç dakikalık pencereyle
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, at, 5 * 60_000L, pi)
    }

    /** Kalan süre: Chronometer sistemin saatiyle geri sayar */
    fun countdown(v: RemoteViews, id: Int, at: Instant, now: Instant) {
        val base = SystemClock.elapsedRealtime() + (at.toEpochMilli() - now.toEpochMilli())
        v.setChronometer(id, base, null, true)
        v.setChronometerCountDown(id, true)
    }

    fun openApp(c: Context, tab: Tab, id: Int): PendingIntent = PendingIntent.getActivity(
        c, id, Intent(c, MainActivity::class.java).putExtra("startTab", tab.ordinal)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Rakamlı kadran (uygulamadaki AnalogClock ölçüleri; 200 birim); renkler sistemin açık/koyu temasından */
    fun dial(c: Context): Bitmap {
        val px = 400
        val u = px / 200f
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = c.getColor(R.color.widget_face)
        cv.drawCircle(100 * u, 100 * u, 94 * u, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2 * u
        p.color = c.getColor(R.color.widget_border)
        cv.drawCircle(100 * u, 100 * u, 94 * u, p)
        p.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 60) {
            val major = i % 5 == 0
            val a = Math.toRadians(i * 6.0)
            val r1 = if (major) 88f else 86f
            val r2 = if (major) 76f else 81f
            p.strokeWidth = (if (major) 3f else 1.4f) * u
            p.color = c.getColor(if (major) R.color.widget_text else R.color.widget_muted)
            cv.drawLine(
                (100 + r1 * sin(a)).toFloat() * u, (100 - r1 * cos(a)).toFloat() * u,
                (100 + r2 * sin(a)).toFloat() * u, (100 - r2 * cos(a)).toFloat() * u, p,
            )
        }
        p.style = Paint.Style.FILL
        p.color = c.getColor(R.color.widget_text)
        p.textSize = 15 * u
        p.isFakeBoldText = true
        p.textAlign = Paint.Align.CENTER
        for (n in 1..12) {
            val a = Math.toRadians(n * 30.0)
            val x = (100 + 66 * sin(a)).toFloat() * u
            val y = (100 - 66 * cos(a)).toFloat() * u - (p.ascent() + p.descent()) / 2
            cv.drawText(n.toString(), x, y, p)
        }
        return b
    }
}

/** Vakit widget'ının yeri: uygulamadaki konum ya da 81 ilden biri (widget eklenirken ya da sonradan) */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Vazgeçilirse widget eklenmez (Android 12 öncesi); seçim yapılınca tamam
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        fun pick(v: String) {
            Prefs.store.edit().putString(WidgetUpdater.placeKey(id), v).apply()
            WidgetUpdater.updateAll(this)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
            finish()
        }
        val current = Prefs.store.getString(WidgetUpdater.placeKey(id), "app")
        setContent {
            AppTheme {
                Column(Modifier.fillMaxSize().background(Theme.c.bg).statusBarsPadding()) {
                    Text(L("prayer.province"), Modifier.padding(16.dp), color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Row(L("widget.province.app") + " · " + AppPlace.current().name, current == "app") { pick("app") }
                        }
                        items(Province.all.sortedWith(compareBy(java.text.Collator.getInstance(Strings.locale)) { it.displayName })) { p ->
                            Row(p.displayName, current == p.slug) { pick(p.slug) }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Row(title: String, selected: Boolean, onClick: () -> Unit) {
        Text(
            title, Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
            color = if (selected) Theme.c.accent else Theme.c.text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp,
        )
        HorizontalDivider(color = Theme.c.border.copy(alpha = 0.4f))
    }
}
