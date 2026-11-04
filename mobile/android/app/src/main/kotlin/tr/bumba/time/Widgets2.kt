package tr.bumba.time

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/*
 * 2. analiz K-4 widget'ları (3 Kasım 2026): yıl ilerlemesi (#165), yazıyla tarih (#172), gün doğumu/batımı, hava,
 * takvim etkinlikleri ve fotoğraf (#15), günün içeriği (#16: söz, ayet, esma, hadis; dokunarak değişir). iOS'taki
 * DailyWidgets.swift ile aynı hesaplar. Hava Open-Meteo'dan (uygulamadaki gibi), diğerleri telefonda hesaplanır.
 */

/** Yıl ilerlemesi: "307/365", yüzde, kalan gün ve her güne bir nokta */
class YearWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_year)
        val today = LocalDate.now()
        val day = today.dayOfYear
        val total = today.lengthOfYear()
        v.setTextViewText(R.id.year, today.year.toString())
        v.setTextViewText(R.id.count, "$day/$total")
        v.setTextViewText(R.id.pct, Fmt.percent(day * 100 / total))
        v.setTextViewText(R.id.left, T("client.daysLeft", total - day))
        v.setImageViewBitmap(R.id.dots, yearDots(c, day, total))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.CALENDAR, id))
        return v
    }

    companion object {
        /** 25 sütunlu nokta ızgarası; geçen günler vurgu renginde */
        fun yearDots(c: Context, day: Int, total: Int, cols: Int = 25): Bitmap {
            val cell = 16
            val rows = (total + cols - 1) / cols
            val b = Bitmap.createBitmap(cols * cell, rows * cell, Bitmap.Config.ARGB_8888)
            val cv = Canvas(b)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val on = c.getColor(R.color.widget_accent)
            val off = c.getColor(R.color.widget_soft)
            for (i in 0 until total) {
                p.color = if (i < day) on else off
                cv.drawCircle((i % cols + 0.5f) * cell, (i / cols + 0.5f) * cell, cell * 0.34f, p)
            }
            return b
        }
    }
}

/** Yazıyla tarih: gün adı, günün sayısı yazıyla (ICU), ay büyük harfle; gece yarısı yenilenir */
class WordsWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_words)
        val today = LocalDate.now()
        v.setTextViewText(R.id.weekday, L("calendar.weekdays.${today.dayOfWeek.value - 1}"))
        v.setTextViewText(R.id.day, spell(today.dayOfMonth))
        v.setTextViewText(R.id.month, Fmt.month(today.monthValue).uppercase(Strings.locale))
        v.setTextViewText(R.id.year, spell(today.year))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.CALENDAR, id))
        return v
    }

    companion object {
        /** Sayıyı arayüz dilinde yazıyla (ICU kuralları; yoksa rakam) */
        fun spell(n: Int): String = runCatching {
            android.icu.text.MessageFormat("{0,spellout}", Strings.locale).format(arrayOf<Any>(n))
        }.getOrDefault(n.toString())
    }
}

/** Gün doğumu ve batımı (seçili yerde; vakit düzeltmeleri olmadan) */
class SunWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_sun)
        val place = WidgetUpdater.place(id)
        val today = place.today(Instant.now())
        val rise = AlarmItem.sunMinutes(today, place.lat, place.lon, place.zone, true)
        val set = AlarmItem.sunMinutes(today, place.lat, place.lon, place.zone, false)
        fun hm(m: Int?) = m?.let { "%02d:%02d".format(java.util.Locale.ROOT, (it / 60) % 24, it % 60) } ?: "—"
        v.setTextViewText(R.id.place, "📍 " + place.name)
        v.setTextViewText(R.id.riseLabel, L("nature.sunrise"))
        v.setTextViewText(R.id.rise, hm(rise))
        v.setTextViewText(R.id.setLabel, L("nature.sunset"))
        v.setTextViewText(R.id.set, hm(set))
        val len = if (rise != null && set != null && set > rise) T("seasons.dayLengthValue", (set - rise) / 60, (set - rise) % 60) else "—"
        v.setTextViewText(R.id.length, L("nature.dayLength") + ": " + len)
        // Sıradaki ekinoks ya da gündönümüne kalan gün (2. analiz #150)
        v.setTextViewText(R.id.turn, SeasonTurn.next(place.lat, Instant.now()).let { (season, days) -> T("widget.sun.turnIn", L("nature.seasons.$season"), days) })
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.SEASONS, id))
        return v
    }
}

/** Günün içeriği: söz, ayet, esma, hadis; dokununca sıradaki türe geçer (seçim `widget.daily.<id>`) */
class DailyWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_daily)
        val e = DailyContent.entry(c, kind(id), LocalDate.now())
        v.setTextViewText(R.id.title, e.title)
        v.setTextViewText(R.id.arabic, e.arabic)
        v.setViewVisibility(R.id.arabic, if (e.arabic.isEmpty()) View.GONE else View.VISIBLE)
        v.setTextViewText(R.id.text, e.text)
        v.setViewVisibility(R.id.text, if (e.text.isEmpty()) View.GONE else View.VISIBLE)
        v.setTextViewText(R.id.source, e.source)
        val tap = PendingIntent.getBroadcast(c, 7000 + id, Intent(c, DailyWidget::class.java).setAction(ACTION_NEXT)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        v.setOnClickPendingIntent(R.id.root, tap)
        return v
    }

    override fun onReceive(c: Context, intent: Intent) {
        if (intent.action == ACTION_NEXT) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (id >= 0) {
                val next = KINDS[(KINDS.indexOf(kind(id)) + 1) % KINDS.size]
                Prefs.store.edit().putString(key(id), next).apply()
                runCatching { AppWidgetManager.getInstance(c).updateAppWidget(id, views(c, id)) }
            }
            return
        }
        super.onReceive(c, intent)
    }

    companion object {
        const val ACTION_NEXT = "tr.bumba.time.DAILY_NEXT"
        val KINDS = listOf("saying", "verse", "esma", "hadith")
        fun key(id: Int) = "widget.daily.$id"
        fun kind(id: Int) = Prefs.store.getString(key(id), "saying") ?: "saying"
    }
}

/** Günün içeriği (iOS DailyContent ile aynı sıra: epoch günü mod sayı) */
object DailyContent {
    class Entry(val title: String, val arabic: String, val text: String, val source: String)

    fun entry(c: Context, kind: String, day: LocalDate): Entry = when (kind) {
        "verse" -> verse(c, day)
        "esma" -> Esma.load(c).let { all ->
            if (all.isEmpty()) Entry(L("widget.daily.esma"), "", "", "")
            else all[Math.floorMod(day.toEpochDay(), all.size.toLong()).toInt()].let { n ->
                Entry(L("widget.daily.esma"), n.ar, listOf(n.read, n.meaning).filter { it.isNotEmpty() }.joinToString(" — "), "")
            }
        }
        "hadith" -> Hadiths.today(c, day)?.let { Entry(L("hadith.today"), it.ar, it.text, it.source) } ?: Entry(L("hadith.today"), "", "", "")
        else -> Entry(L("widget.daily.saying"), "", Sayings.today(c, day) ?: "", "")
    }

    /** Günün ayeti: kısa ayetlerden (en çok 120 harf); İngilizce ve Almanca arayüzde Pickthall meali (Türkçe meal yok) */
    private fun verse(c: Context, day: LocalDate): Entry {
        val count = 6236
        val ok = runCatching { Quran.load(c) }.isSuccess
        if (!ok) return Entry(L("widget.daily.verse"), "", "", "")
        var i = Math.floorMod(day.toEpochDay() * 211, count.toLong()).toInt()
        repeat(count) { if (text(i).length > 120) i = (i + 1) % count }
        val sura = Quran.suras.last { it.start <= i }
        val aya = i - sura.start + 1
        val meal = if (Strings.lang == "en" || Strings.lang == "de") Quran.en(sura.no, aya) else ""
        return Entry(L("widget.daily.verse"), Quran.ar(sura.no, aya), meal, "${sura.title} ${sura.no}:$aya")
    }

    private fun text(i: Int): String {
        val s = Quran.suras.last { it.start <= i }
        return Quran.ar(s.no, i - s.start + 1)
    }
}

/** Fotoğraf: tam ekran saat için seçilen fotoğraf, üstünde saat ve tarih (fotoğraf yoksa nasıl seçileceği) */
class PhotoWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_photo)
        val f = File(c.filesDir, "fs-photo.jpg")
        val bmp = if (f.exists()) runCatching {
            val o = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeFile(f.path, o)
        }.getOrNull() else null
        if (bmp != null) {
            v.setImageViewBitmap(R.id.photo, bmp)
            v.setViewVisibility(R.id.empty, View.GONE)
        } else {
            v.setTextViewText(R.id.empty, L("widget.photo.none"))
            v.setViewVisibility(R.id.empty, View.VISIBLE)
        }
        val today = LocalDate.now()
        v.setTextViewText(R.id.date, today.dayOfMonth.toString() + " " + Fmt.month(today.monthValue))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.CLOCK, id))
        return v
    }
}

private fun calendarGranted(c: Context) = androidx.core.content.ContextCompat.checkSelfPermission(c, android.Manifest.permission.READ_CALENDAR) ==
    android.content.pm.PackageManager.PERMISSION_GRANTED

/** Takvim: bugünün ve yarının özel günleri ile telefondaki takvim etkinlikleri (izin uygulamada verilir) */
class EventsWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_events)
        val today = LocalDate.now()
        v.setTextViewText(R.id.title, Fmt.longDate(today))
        val zone = ZoneId.systemDefault()
        val lines = mutableListOf<String>()
        for (d in listOf(today, today.plusDays(1))) {
            val prefix = if (d == today) "" else L("client.tomorrow") + " · "
            AppData.events[d].orEmpty().map { it.name }.distinct().forEach { lines += "★ $prefix$it" }
            if (calendarGranted(c)) {
                DeviceCalendar.events(c, YearMonth.from(d))[d].orEmpty().forEach { e ->
                    lines += "• $prefix" + (if (e.allDay) "" else Fmt.hm(e.begin, zone) + " ") + e.title
                }
            }
        }
        val rows = listOf(R.id.e0, R.id.e1, R.id.e2, R.id.e3, R.id.e4)
        for ((i, r) in rows.withIndex()) {
            val t = lines.getOrNull(i)
            v.setTextViewText(r, t ?: "")
            v.setViewVisibility(r, if (t == null) View.GONE else View.VISIBLE)
        }
        v.setTextViewText(R.id.empty, L(if (calendarGranted(c)) "widget.events.none" else "widget.events.permission"))
        v.setViewVisibility(R.id.empty, if (lines.isEmpty()) View.VISIBLE else View.GONE)
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.CALENDAR, id))
        return v
    }
}

/** Hava: uygulamadaki konumun güncel havası (Open-Meteo, 15 dk önbellek); ekleme ve yenilemede arka planda sorulur */
class WeatherWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_weather)
        val place = AppPlace.current()
        v.setTextViewText(R.id.place, "📍 " + place.name)
        val w = WeatherCache.cached(place.lat, place.lon)
        if (w != null) {
            val code = WeatherCode.of(w.current.code)
            v.setTextViewText(R.id.icon, code.icon(w.current.isDay))
            v.setTextViewText(R.id.temp, Units.temp(w.current.temp))
            v.setTextViewText(R.id.cond, code.label)
            v.setTextViewText(R.id.range, w.today?.let { "↑${Units.temp(it.max)} ↓${Units.temp(it.min)}" } ?: "")
        } else {
            v.setTextViewText(R.id.icon, "…")
            v.setTextViewText(R.id.temp, "")
            v.setTextViewText(R.id.cond, L("weather.loading"))
            v.setTextViewText(R.id.range, "")
        }
        v.setTextViewText(R.id.attribution, "Open-Meteo.com")
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.WEATHER, id))
        return v
    }

    override fun onUpdate(c: Context, mgr: AppWidgetManager, ids: IntArray) {
        super.onUpdate(c, mgr, ids)
        val pending = goAsync()
        Thread {
            runCatching {
                val p = AppPlace.current()
                kotlinx.coroutines.runBlocking { WeatherCache.weather(p.lat, p.lon) }
                for (id in ids) mgr.updateAppWidget(id, views(c, id))
            }
            pending.finish()
        }.start()
    }
}

/** Uygulama içi widget galerisi (2. analiz #166): bütün widget'lar, canlı önizleme ve "Ana ekrana ekle" */
object WidgetCatalog {
    class Item(val cls: Class<out BaseWidget>, val name: String, val desc: String)

    fun all(): List<Item> = listOf(
        Item(NextPrayerWidget::class.java, L("widget.next.name"), L("widget.next.desc")),
        Item(TodayPrayersWidget::class.java, L("prayer.today"), L("widget.today.desc")),
        Item(AnalogClockWidget::class.java, L("widget.analog.name"), L("widget.analog.desc")),
        Item(DigitalClockWidget::class.java, L("widget.digital.name"), L("widget.digital.desc")),
        Item(ClockDateWidget::class.java, L("widget.clockDate.name"), L("widget.clockDate.desc")),
        Item(SecondsClockWidget::class.java, L("widget.seconds.name"), L("widget.seconds.desc")),
        Item(DualClockWidget::class.java, L("widget.dual.name"), L("widget.dual.desc")),
        Item(WorldClockWidget::class.java, L("world.h1"), L("widget.world.desc")),
        Item(WordsWidget::class.java, L("widget.words.name"), L("widget.words.desc")),
        Item(YearWidget::class.java, L("widget.year.name"), L("widget.year.desc")),
        Item(SunWidget::class.java, L("widget.sun.name"), L("widget.sun.desc")),
        Item(WeatherWidget::class.java, L("widget.weather.name"), L("widget.weather.desc")),
        Item(EventsWidget::class.java, L("widget.events.name"), L("widget.events.desc")),
        Item(PhotoWidget::class.java, L("widget.photo.name"), L("widget.photo.desc")),
        Item(DailyWidget::class.java, L("widget.daily.name"), L("widget.daily.desc")),
        Item(MoonWidget::class.java, L("widget.moon.name"), L("widget.moon.desc")),
        Item(MoonCalWidget::class.java, L("widget.moonCal.name"), L("widget.moonCal.desc")),
        Item(HijriWidget::class.java, L("widget.hijri.name"), L("widget.hijri.desc")),
        Item(CountdownWidget::class.java, L("tools.hub.cards.countdown.name"), L("widget.countdown.desc")),
    )

    fun canPin(c: Context): Boolean = AppWidgetManager.getInstance(c).isRequestPinAppWidgetSupported

    fun pin(c: Context, cls: Class<*>): Boolean = runCatching {
        AppWidgetManager.getInstance(c).requestPinAppWidget(ComponentName(c, cls), null, null)
    }.getOrDefault(false)
}

/** Widget galerisi ekranı: her widget'ın canlı önizlemesi (widget'ın kendi RemoteViews'u), adı, açıklaması ve
 *  "Ana ekrana ekle" (başlatıcı destekliyorsa; yoksa nasıl ekleneceği yazar) */
@androidx.compose.runtime.Composable
fun WidgetGalleryScreen() {
    val c = androidx.compose.ui.platform.LocalContext.current
    val canPin = androidx.compose.runtime.remember { WidgetCatalog.canPin(c) }
    androidx.compose.foundation.layout.Column(
        androidx.compose.ui.Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
    ) {
        androidx.compose.material3.Text(L("widgets.howAndroid"), color = Theme.c.muted, fontSize = 13.sp)
        for (w in androidx.compose.runtime.remember { WidgetCatalog.all() }) {
            AppCard {
                androidx.compose.material3.Text(w.name, color = Theme.c.text, fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                androidx.compose.material3.Text(w.desc, color = Theme.c.muted, fontSize = 13.sp)
                // Önizleme: widget'ın gerçek görünümü (örnek kimlikle; ayarı yoksa varsayılan)
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.FrameLayout(ctx).apply {
                            runCatching {
                                val rv = w.cls.getDeclaredConstructor().newInstance().views(ctx, PREVIEW_ID)
                                addView(rv.apply(ctx, this))
                            }
                        }
                    },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp),
                )
                if (canPin) androidx.compose.material3.TextButton(onClick = { WidgetCatalog.pin(c, w.cls) }) {
                    androidx.compose.material3.Text("＋ " + L("widgets.add"), color = Theme.c.accent)
                }
            }
        }
    }
}

/** Önizlemede kullanılan widget kimliği (gerçek widget'larla karışmaz) */
private const val PREVIEW_ID = Int.MAX_VALUE - 1

/** Sıradaki mevsim başlangıcı (ekinoks ya da gündönümü): mevsim anahtarı ve kalan gün; güney yarımkürede adlar yer değiştirir */
object SeasonTurn {
    fun next(lat: Double, now: Instant): Pair<String, Long> {
        val y = now.atZone(java.time.ZoneOffset.UTC).year
        val north = listOf("spring", "summer", "autumn", "winter")
        val names = if (lat < 0) listOf("autumn", "winter", "spring", "summer") else north
        for (yy in y..y + 1) {
            val s = tr.bumba.time.core.Nature.seasons(yy)
            listOf(s.march, s.june, s.september, s.december).forEachIndexed { i, ms ->
                if (ms > now.toEpochMilli()) return names[i] to Math.ceil((ms - now.toEpochMilli()) / 86_400_000.0).toLong()
            }
        }
        return names[0] to 0L
    }
}

