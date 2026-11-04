package tr.bumba.time

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

/*
 * Dünya ve araç widget'ları (2. analiz #130, #136; 4 Kasım 2026):
 *  - 8 şehir: kısa kod, ad, gündüz/gece işareti, TextClock (saatleri sistem işler; işaret 30 dk'da bir yenilenir)
 *  - Saat çakışması: şehirlerin 09–18 mesai saatleri 24 saatlik şeritlerde, ortak saatler çerçeveli
 *  - Dünya haritası: gece gölgesi, Güneş noktası ve şehirlerin saati (30 dk'da bir çizilir)
 *  - Zamanlayıcılar: süren en çok üç zamanlayıcı (Chronometer geri sayar) ve 5/10/15/30 dk hazır süreler
 *  - Odak: bugünkü pomodoro odak süresi, süren çalışma ve tek dokunuşla 25 dk
 */

private fun cityCode(e: WorldEntry): String {
    val n = e.displayName.trim()
    val latin = n.all { it.code < 0x0250 || !it.isLetter() }
    return if (latin) n.filter { it.isLetter() }.take(3).uppercase() else n.take(2)
}

class World8Widget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_world8)
        val list = WorldEntry.list().take(8)
        val now = Instant.now()
        val rows = listOf(R.id.row1, R.id.row2, R.id.row3, R.id.row4, R.id.row5, R.id.row6, R.id.row7, R.id.row8)
        val codes = listOf(R.id.c1, R.id.c2, R.id.c3, R.id.c4, R.id.c5, R.id.c6, R.id.c7, R.id.c8)
        val names = listOf(R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6, R.id.n7, R.id.n8)
        val marks = listOf(R.id.m1, R.id.m2, R.id.m3, R.id.m4, R.id.m5, R.id.m6, R.id.m7, R.id.m8)
        val times = listOf(R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5, R.id.t6, R.id.t7, R.id.t8)
        for (i in 0 until 8) {
            val e = list.getOrNull(i)
            v.setViewVisibility(rows[i], if (e == null) View.GONE else View.VISIBLE)
            if (e == null) continue
            v.setTextViewText(codes[i], cityCode(e))
            v.setTextViewText(names[i], e.displayName)
            v.setTextViewText(marks[i], when (e.light(now)) { "day" -> "☀"; "dusk" -> "◐"; else -> "☾" })
            v.setString(times[i], "setTimeZone", e.zone.id)
        }
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.WORLD, id))
        return v
    }
}

/** Saat çakışması çizimi: satır başına şehir, sütun başına buradaki saat (bugün 0–23) */
fun overlapBitmap(c: Context, list: List<WorldEntry>, now: Instant, dark: Boolean): Bitmap {
    val d = c.resources.displayMetrics.density
    val rows = list.take(6)
    val labelW = 78 * d
    val cell = 13 * d
    val rowH = 26 * d
    val top = 18 * d
    val w = (labelW + cell * 24 + 6 * d).toInt()
    val h = (top + rowH * max(1, rows.size) + 4 * d).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val ink = if (dark) 0xFFEDEAE3.toInt() else 0xFF1E1B16.toInt()
    val muted = if (dark) 0xFFA8A296.toInt() else 0xFF6B655A.toInt()
    val work = if (dark) 0xFF5FB98A.toInt() else 0xFF2E8B57.toInt()
    val edge = if (dark) 0xFFE0A84D.toInt() else 0xFFC98A1E.toInt()
    val night = if (dark) 0xFF2A2F3D.toInt() else 0xFFDCDFE8.toInt()
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11 * d; color = ink }
    val here = ZoneId.systemDefault()
    val day = now.atZone(here).toLocalDate()
    val start = day.atStartOfDay(here).toInstant()
    // Saat başlıkları (buradaki saat)
    tp.color = muted
    tp.textSize = 9 * d
    for (hr in 0 until 24 step 3) cv.drawText(hr.toString(), labelW + hr * cell + 1 * d, 12 * d, tp)
    val common = BooleanArray(24) { true }
    rows.forEachIndexed { r, e ->
        val y = top + r * rowH
        tp.color = ink
        tp.textSize = 11 * d
        val name = e.displayName.let { if (tp.measureText(it) > labelW - 6 * d) it.take(9) + "…" else it }
        cv.drawText(name, 0f, y + 15 * d, tp)
        for (hr in 0 until 24) {
            val local = start.plusSeconds(hr * 3600L + 1800).atZone(e.zone).hour
            val kind = when (local) { in 9..17 -> 0; 7, 8, 18, 19, 20, 21 -> 1; else -> 2 }
            if (kind != 0) common[hr] = false
            p.color = when (kind) { 0 -> work; 1 -> edge; else -> night }
            cv.drawRoundRect(RectF(labelW + hr * cell + 1 * d, y + 4 * d, labelW + (hr + 1) * cell - 1 * d, y + rowH - 6 * d), 3 * d, 3 * d, p)
        }
    }
    // Ortak mesai saatleri çerçeveli
    p.style = Paint.Style.STROKE
    p.strokeWidth = 2 * d
    p.color = ink
    var hr = 0
    while (hr < 24) {
        if (!common[hr]) { hr++; continue }
        val s = hr
        while (hr < 24 && common[hr]) hr++
        cv.drawRoundRect(RectF(labelW + s * cell, top + 1 * d, labelW + hr * cell, top + rowH * rows.size - 2 * d), 4 * d, 4 * d, p)
    }
    // Şimdi çizgisi
    val nowH = (now.epochSecond - start.epochSecond) / 3600.0
    p.color = 0xFFE4572E.toInt()
    p.strokeWidth = 2 * d
    val x = (labelW + nowH * cell).toFloat()
    cv.drawLine(x, top - 2 * d, x, top + rowH * rows.size, p)
    return bmp
}

class OverlapWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_image)
        val dark = (c.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        v.setImageViewBitmap(R.id.img, overlapBitmap(c, WorldEntry.list(), Instant.now(), dark))
        v.setContentDescription(R.id.img, L("widget.overlap.name"))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.WORLD, id))
        return v
    }
}

/** Dünya haritası çizimi (Compose dışı; uygulamadaki harita kartıyla aynı kural) */
fun worldMapBitmap(c: Context, list: List<WorldEntry>, now: Instant): Bitmap {
    val d = c.resources.displayMetrics.density
    val w = (320 * d).toInt()
    val h = w / 2
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    cv.drawColor(0xFF0E1A2E.toInt())
    WorldMapTexture.bitmap(c)?.let { cv.drawBitmap(it, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG)) }
    val poly = nightPolygon(now.toEpochMilli(), w.toFloat(), h.toFloat())
    val path = Path().apply {
        moveTo(poly[0].first, poly[0].second)
        for (pt in poly.drop(1)) lineTo(pt.first, pt.second)
        close()
    }
    cv.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC050A1A.toInt() })
    val sun = GlobeSky.sun(now.toEpochMilli())
    val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFC94D.toInt() }
    cv.drawCircle(((sun.lon + 180) / 360 * w).toFloat(), ((90 - sun.lat) / 180 * h).toFloat(), 5 * d, sp)
    val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9.5f * d
        color = android.graphics.Color.WHITE
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(2.5f * d, 0f, 0f, android.graphics.Color.BLACK)
    }
    val boxes = ArrayList<RectF>()
    for (e in list.take(8)) {
        val la = e.lat ?: continue
        val lo = e.lon ?: continue
        val x = ((lo + 180) / 360 * w).toFloat()
        val y = ((90 - la) / 180 * h).toFloat()
        dot.color = if (e.light(now) == "day") 0xFFFFE08A.toInt() else 0xFF9DB7FF.toInt()
        cv.drawCircle(x, y, 3 * d, dot)
        val text = e.displayName.take(12) + " " + Fmt.time(now.atZone(e.zone))
        val tw = tp.measureText(text)
        var lx = x + 5 * d
        if (lx + tw > w - 2) lx = x - 5 * d - tw
        val box = RectF(lx, y - 7 * d, lx + tw, y + 4 * d)
        if (boxes.any { RectF.intersects(it, box) }) continue
        boxes += box
        cv.drawText(text, lx, y + 3.5f * d, tp)
    }
    return bmp
}

class WorldMapWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_image)
        v.setImageViewBitmap(R.id.img, worldMapBitmap(c, WorldEntry.list(), Instant.now()))
        v.setContentDescription(R.id.img, L("widget.worldMap.name"))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.WORLD, id))
        return v
    }
}

object TimeWidgets {
    const val TIMER = "tr.bumba.time.WIDGET_TIMER"
    const val FOCUS = "tr.bumba.time.WIDGET_FOCUS"
    val PRESETS = listOf(5, 10, 15, 30)

    fun pending(c: Context, action: String, code: Int, minutes: Int = 0): android.app.PendingIntent =
        android.app.PendingIntent.getBroadcast(
            c, code, Intent(c, TimeWidgetReceiver::class.java).setAction(action).putExtra("min", minutes),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

    /** Pomodoro çalışma süresini uygulamayı açmadan başlatır (PomodoroScreen'deki start ile aynı kayıtlar) */
    fun startFocus(c: Context) {
        val work = ToolState.int("pomo.work", 25)
        val end = System.currentTimeMillis() + work * 60_000L
        ToolState.putStr("pomo.phase", "work")
        ToolState.putLong("pomo.end", end)
        ToolState.putLong("pomo.left", 0L)
        AlarmScheduler.schedule(c, AlarmScheduler.POMODORO, end, T("tools.pomodoro.phaseDone", L("tools.pomodoro.work")), AlarmScheduler.POMODORO)
        PomoNoise.sync(c)
        FocusBlock.sync(c)
        LiveNotifications.sync(c)
    }

    /** Süren sayaç değişince (LiveNotifications.sync çağırır) */
    fun refresh(c: Context) {
        val mgr = AppWidgetManager.getInstance(c) ?: return
        for ((cls, w) in listOf(TimersWidget::class.java to TimersWidget(), FocusWidget::class.java to FocusWidget())) {
            val ids = runCatching { mgr.getAppWidgetIds(ComponentName(c, cls)) }.getOrDefault(IntArray(0))
            for (id in ids) mgr.updateAppWidget(id, w.views(c, id))
        }
    }
}

class TimeWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            TimeWidgets.TIMER -> {
                val m = intent.getIntExtra("min", 0)
                if (m > 0) TimerItem.startNew(c, m * 60_000L, T("watch.minutes", m))
            }
            TimeWidgets.FOCUS -> TimeWidgets.startFocus(c)
        }
        TimeWidgets.refresh(c)
    }
}

class TimersWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_timers)
        v.setTextViewText(R.id.title, L("widget.timers.name"))
        val now = System.currentTimeMillis()
        val running = TimerItem.load().filter { it.running && it.end > now }.sortedBy { it.end }.take(3)
        val rows = listOf(R.id.tr1, R.id.tr2, R.id.tr3)
        val names = listOf(R.id.tn1, R.id.tn2, R.id.tn3)
        val chs = listOf(R.id.tc1, R.id.tc2, R.id.tc3)
        v.setViewVisibility(R.id.empty, if (running.isEmpty()) View.VISIBLE else View.GONE)
        v.setTextViewText(R.id.empty, L("widget.timers.none"))
        for (i in 0..2) {
            val t = running.getOrNull(i)
            v.setViewVisibility(rows[i], if (t == null) View.GONE else View.VISIBLE)
            if (t == null) continue
            v.setTextViewText(names[i], t.name.ifBlank { L("watch.timer") })
            v.setChronometer(chs[i], SystemClock.elapsedRealtime() + (t.end - now), null, true)
            v.setChronometerCountDown(chs[i], true)
        }
        val btns = listOf(R.id.p1, R.id.p2, R.id.p3, R.id.p4)
        TimeWidgets.PRESETS.forEachIndexed { i, m ->
            v.setTextViewText(btns[i], T("watch.minutes", m))
            v.setOnClickPendingIntent(btns[i], TimeWidgets.pending(c, TimeWidgets.TIMER, 7200 + i, m))
        }
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.TOOLS, id))
        return v
    }
}

class FocusWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_focus)
        v.setTextViewText(R.id.title, L("widget.focus.name"))
        val today = LocalDate.now().toString()
        val minutes = PomoLog.load().filter { it.d == today }.sumOf { it.m }
        v.setTextViewText(R.id.today, T("widget.focus.today", if (minutes < 60) T("watch.minutes", minutes) else Fmt.hm(minutes * 60)))
        val end = ToolState.long("pomo.end")
        val now = System.currentTimeMillis()
        val running = end > now && ToolState.str("pomo.phase", "work") == "work"
        v.setViewVisibility(R.id.run, if (running) View.VISIBLE else View.GONE)
        v.setViewVisibility(R.id.start, if (running) View.GONE else View.VISIBLE)
        if (running) {
            v.setChronometer(R.id.run, SystemClock.elapsedRealtime() + (end - now), null, true)
            v.setChronometerCountDown(R.id.run, true)
        }
        v.setTextViewText(R.id.start, L("widget.focus.start").let { s -> val w = ToolState.int("pomo.work", 25); if (w == 25) s else T("watch.minutes", w) + " ▶" })
        v.setOnClickPendingIntent(R.id.start, TimeWidgets.pending(c, TimeWidgets.FOCUS, 7210))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.TOOLS, id))
        return v
    }
}

/** Güneş yayı ve 24 saat halka widget'ı (2. analiz #150; 4 Kasım 2026): solda doğuştan batışa yay ve Güneş'in yeri,
 *  sağda günün 24 saati (gündüz sarı, gece lacivert) ve şimdi ibresi. Yer: widget ayarı ya da uygulamadaki konum. */
fun sunArcBitmap(c: Context, place: AppPlace, now: Instant, dark: Boolean): Bitmap {
    val d = c.resources.displayMetrics.density
    val w = (320 * d).toInt()
    val h = (150 * d).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val bg = if (dark) 0xFF161A22.toInt() else 0xFFFFFBF2.toInt()
    val ink = if (dark) 0xFFE8ECF4.toInt() else 0xFF1B2330.toInt()
    val muted = if (dark) 0xFF8C97AA.toInt() else 0xFF6B7280.toInt()
    val sunC = 0xFFF4B400.toInt()
    val nightC = 0xFF2B3B5E.toInt()
    cv.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 18 * d, 18 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg })
    val today = place.today(now)
    val rise = AlarmItem.sunMinutes(today, place.lat, place.lon, place.zone, true)
    val set = AlarmItem.sunMinutes(today, place.lat, place.lon, place.zone, false)
    val z = now.atZone(place.zone)
    val nowMin = z.hour * 60 + z.minute
    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11 * d; color = muted; textAlign = Paint.Align.CENTER }
    fun hm(m: Int?) = m?.let { "%02d:%02d".format(java.util.Locale.ROOT, (it / 60) % 24, it % 60) } ?: "—"
    // Sol: yay
    val ax = w * 0.27f
    val ay = h * 0.70f
    val r = w * 0.20f
    line.color = muted; line.strokeWidth = 1.5f * d
    cv.drawLine(ax - r - 8 * d, ay, ax + r + 8 * d, ay, line)
    line.color = sunC; line.strokeWidth = 3 * d
    line.pathEffect = android.graphics.DashPathEffect(floatArrayOf(5 * d, 5 * d), 0f)
    cv.drawArc(RectF(ax - r, ay - r, ax + r, ay + r), 180f, 180f, false, line)
    line.pathEffect = null
    if (rise != null && set != null && set > rise) {
        val f = ((nowMin - rise).toFloat() / (set - rise)).coerceIn(0f, 1f)
        val a = Math.PI * (1 - f)
        val sx = ax + (r * Math.cos(a)).toFloat()
        val sy = ay - (r * Math.sin(a)).toFloat()
        val up = nowMin in rise..set
        cv.drawCircle(sx, sy, 7 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (up) sunC else muted })
    }
    text.textAlign = Paint.Align.CENTER
    cv.drawText("↑ " + hm(rise), ax - r, ay + 18 * d, text)
    cv.drawText("↓ " + hm(set), ax + r, ay + 18 * d, text)
    text.color = ink; text.textSize = 12 * d; text.typeface = android.graphics.Typeface.DEFAULT_BOLD
    cv.drawText(place.name.take(18), ax, 18 * d, text)
    // Sağ: 24 saat halka (0 üstte, saat yönünde)
    val cx = w * 0.75f
    val cy = h * 0.52f
    val rr = h * 0.32f
    line.strokeWidth = 10 * d; line.strokeCap = Paint.Cap.BUTT
    line.color = nightC
    cv.drawCircle(cx, cy, rr, line)
    if (rise != null && set != null && set > rise) {
        line.color = sunC
        cv.drawArc(RectF(cx - rr, cy - rr, cx + rr, cy + rr), rise / 1440f * 360f - 90f, (set - rise) / 1440f * 360f, false, line)
    }
    val ang = Math.toRadians(nowMin / 1440.0 * 360 - 90)
    line.color = ink; line.strokeWidth = 2.5f * d; line.strokeCap = Paint.Cap.ROUND
    val inner = 25 * d
    cv.drawLine(cx + (inner * Math.cos(ang)).toFloat(), cy + (inner * Math.sin(ang)).toFloat(),
        cx + (rr * 0.82f * Math.cos(ang)).toFloat(), cy + (rr * 0.82f * Math.sin(ang)).toFloat(), line)
    text.textSize = 9 * d; text.typeface = android.graphics.Typeface.DEFAULT; text.color = muted
    for ((hh, lab) in listOf(0 to "0", 6 to "6", 12 to "12", 18 to "18")) {
        val a = Math.toRadians(hh / 24.0 * 360 - 90)
        cv.drawText(lab, cx + ((rr + 12 * d) * Math.cos(a)).toFloat(), cy + ((rr + 12 * d) * Math.sin(a)).toFloat() + 3 * d, text)
    }
    text.textSize = 13 * d; text.color = ink; text.typeface = android.graphics.Typeface.DEFAULT_BOLD
    cv.drawText(hm(nowMin), cx, cy + 5 * d, text)
    return bmp
}

class SunArcWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_image)
        val dark = (c.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        v.setImageViewBitmap(R.id.img, sunArcBitmap(c, WidgetUpdater.place(id), Instant.now(), dark))
        v.setContentDescription(R.id.img, L("widget.sunArc.name"))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.SEASONS, id))
        return v
    }
}
