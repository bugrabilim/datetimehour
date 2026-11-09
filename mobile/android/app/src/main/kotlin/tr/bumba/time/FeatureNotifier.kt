package tr.bumba.time

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import tr.bumba.time.core.Nature
import tr.bumba.time.core.PrayerTimes
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Özellik bildirimleri (30 Ekim 2026, sahibin isteği; iOS'taki FeatureNotifier ile aynı kural): her özelliğin bildirimi
 * varsayılan açık, kullanıcı Ayarlar › Bildirimler'den istemediğini kapatır. Namaz vakitleri ayrıca (PrayerNotifier).
 * Bildirimler telefonda hazırlanır; yalnız hava özeti için Open-Meteo'ya sorulur. AlarmManager'a sıradaki birkaç bildirim
 * kurulur, her bildirim geldiğinde plan yeniden kurulur (vakit bildirimlerindeki gibi).
 */
enum class NotifyFeature(val id: String, val tab: Tab) {
    CALENDAR("calendar", Tab.CALENDAR),
    SEASONS("seasons", Tab.SEASONS),
    MOON("moon", Tab.SEASONS),
    SUN("sun", Tab.SEASONS),
    WEATHER("weather", Tab.WEATHER),
    HOURLY("hourly", Tab.CLOCK),
    DST("dst", Tab.WORLD),
    BEDTIME("bedtime", Tab.TOOLS),
    /** Uyku alışkanlıkları (2. analiz #49): kafein, ekran, akşam rutini; yalnız uyku planı açıkken */
    HABITS("habits", Tab.TOOLS),
    /** Cuma hatırlatması (2. analiz #58): Cuma öğleden 45 dk önce */
    FRIDAY("friday", Tab.PRAYER),
    /** Günün sözü (2. analiz #16): her sabah 08.00'de takvim yaprağındaki söz */
    DAILY("daily", Tab.CALENDAR),
    /** Gökyüzü olayları (2. analiz #154): meteor yağmuru zirveleri, kavuşumlar, karşı konumlar; o gecenin akşamı 20.00 */
    SKY("sky", Tab.SEASONS);

    val key: String get() = "notify.$id"
    /** Varsayılan: saat başı kapalı, diğerleri açık (30 Ekim 2026, sahibin kararı) */
    val defaultOn: Boolean get() = this != HOURLY
    val title: String get() = L("notify.cat.$id")
    val note: String get() = L("notify.cat.$id.note")
}

/** Özellik bildirimi tercihleri ("prefs" dosyasında, iOS'takiyle aynı anahtarlar) */
object FeaturePrefs {
    private val sp get() = Prefs.store

    fun isOn(f: NotifyFeature): Boolean = sp.getBoolean(f.key, f.defaultOn)
    fun set(f: NotifyFeature, on: Boolean) = sp.edit().putBoolean(f.key, on).apply()

    /** Hava özetinin saati (gece yarısından dakika; varsayılan 07.30) */
    var weatherMinutes: Int
        get() = sp.getInt("notify.weatherTime", 450)
        set(v) = sp.edit().putInt("notify.weatherTime", v).apply()

    /** Saat başı bildiriminin ilk ve son saati (varsayılan 09–21) */
    var hourlyFrom: Int
        get() = sp.getInt("notify.hourlyFrom", 9)
        set(v) = sp.edit().putInt("notify.hourlyFrom", v).apply()
    var hourlyTo: Int
        get() = sp.getInt("notify.hourlyTo", 21)
        set(v) = sp.edit().putInt("notify.hourlyTo", v).apply()
}

object FeatureNotifier {
    const val CHANNEL = "features"
    /** AlarmManager'da aynı anda kurulu bildirim sayısı (istek kodları BASE…BASE+SLOTS-1) */
    private const val SLOTS = 12
    private const val BASE = 2000
    private const val ACTION_FIRE = "tr.bumba.time.FEATURE_ALERT"

    data class Planned(val id: String, val at: Instant, val title: String, val body: String, val tab: Tab)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val generation = AtomicInteger(0)

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL, L("settings.notifications"), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    /** Bekleyen özellik alarmlarını siler, planı yeniden kurar (hava için ağ beklenir); bitince `done` */
    fun reschedule(context: Context, done: () -> Unit = {}) {
        val app = context.applicationContext
        val mine = generation.incrementAndGet()
        scope.launch {
            try {
                val items = plan(app, Instant.now())
                if (mine == generation.get()) schedule(app, items)
            } finally {
                done()
            }
        }
    }

    private fun schedule(context: Context, items: List<Planned>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for (i in 0 until SLOTS) {
            PendingIntent.getBroadcast(context, BASE + i, fireIntent(context), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                ?.let { am.cancel(it); it.cancel() }
        }
        val exact = PrayerNotifier.exactAllowed(context)
        items.sortedBy { it.at }.take(SLOTS).forEachIndexed { i, p ->
            val pi = PendingIntent.getBroadcast(
                context, BASE + i,
                fireIntent(context).putExtra("id", p.id).putExtra("title", p.title).putExtra("body", p.body)
                    .putExtra("tab", p.tab.ordinal),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val t = p.at.toEpochMilli()
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        }
    }

    private fun fireIntent(context: Context) = Intent(context, FeatureAlarmReceiver::class.java).setAction(ACTION_FIRE)

    /** `now` sonrasındaki özellik bildirimleri (14 gün; mevsim ve Ay olayları daha ileriye bakar) */
    suspend fun plan(context: Context, now: Instant, days: Int = 14): List<Planned> {
        val place = AppPlace.current()
        val out = ArrayList<Planned>()
        if (FeaturePrefs.isOn(NotifyFeature.CALENDAR)) out += calendar(now, days)
        if (FeaturePrefs.isOn(NotifyFeature.SEASONS)) out += seasons(place, now)
        if (FeaturePrefs.isOn(NotifyFeature.MOON)) out += moon(context, place, now)
        if (FeaturePrefs.isOn(NotifyFeature.SUN)) out += sun(place, now, days)
        if (FeaturePrefs.isOn(NotifyFeature.DST)) out += dst(now) + worldDst(now)
        if (FeaturePrefs.isOn(NotifyFeature.HOURLY)) out += hourly(now)
        if (FeaturePrefs.isOn(NotifyFeature.WEATHER)) out += weather(context, place, now)
        out += CountdownItem.reminders(now) // kişisel geri sayımların hatırlatmaları (her biri kendi ayarıyla)
        if (FeaturePrefs.isOn(NotifyFeature.BEDTIME)) out += SleepPlan.reminders(now, days) // uyku planı kuruluysa
        if (FeaturePrefs.isOn(NotifyFeature.HABITS)) out += SleepPlan.habits(now, days)
        if (FeaturePrefs.isOn(NotifyFeature.FRIDAY)) out += friday(place, now, days)
        if (FeaturePrefs.isOn(NotifyFeature.DAILY)) out += daily(context, now, minOf(days, 7))
        if (FeaturePrefs.isOn(NotifyFeature.SKY)) out += sky(context, place, now, days)
        return out.filter { it.at.isAfter(now) }
    }

    /** Özel günler: o gün sabah 09.00'da (telefonun saatiyle), günün bütün olayları tek bildirimde */
    private fun calendar(now: Instant, days: Int): List<Planned> {
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        return (0 until days).mapNotNull { i ->
            val day = today.plusDays(i.toLong())
            val names = calEvents(day).map { it.name }.distinct()
            if (names.isEmpty()) return@mapNotNull null
            Planned("cal-$day", day.atTime(9, 0).atZone(zone).toInstant(), names.joinToString(" · "),
                T("notify.n.today", Fmt.longDate(day)), NotifyFeature.CALENDAR.tab)
        }
    }

    /** Ekinoks ve gündönümü anında; güney yarımkürede mevsim adları yer değiştirir */
    private fun seasons(place: AppPlace, now: Instant): List<Planned> {
        val names = if (place.lat < 0) listOf("autumn", "winter", "spring", "summer") else listOf("spring", "summer", "autumn", "winter")
        val year = now.atZone(place.zone).year
        return listOf(year, year + 1).flatMap { y ->
            val s = Nature.seasons(y)
            listOf(s.march, s.june, s.september, s.december).mapIndexedNotNull { i, ms ->
                val at = Instant.ofEpochMilli(ms)
                if (!at.isAfter(now) || at.isAfter(now.plusSeconds(400L * 86400))) return@mapIndexedNotNull null
                Planned("season-$ms", at, T("notify.n.season", L("nature.seasons.${names[i]}")), whenText(at, place), NotifyFeature.SEASONS.tab)
            }
        }
    }

    /** Dolunay ve yeni ay anında (yaklaşık iki ay ileri) */
    /** Ay olayları (2. analiz #152; `SunMoonPrefs`): seçilen olaylar (yeni ay, dolunay, dördünler, süper Ay, tutulmalar),
     *  istenirse olaydan önce (dk) */
    /** Gökyüzü olayı o gecenin akşamı 20.00'de (yerel öğleden önceki olay bir önceki akşama düşer) */
    private fun sky(context: Context, place: AppPlace, now: Instant, days: Int): List<Planned> =
        SkyEvent.all(context).filter { it.t > now.toEpochMilli() - 86_400_000 && it.t < now.toEpochMilli() + (days + 1) * 86_400_000L }.mapNotNull { e ->
            val local = Instant.ofEpochMilli(e.t).atZone(place.zone)
            val day = if (local.hour < 12) local.toLocalDate().minusDays(1) else local.toLocalDate()
            val at = day.atTime(20, 0).atZone(place.zone).toInstant()
            if (!at.isAfter(now)) null else Planned("sky-${e.t}-${e.k}", at, e.title, e.detail ?: whenText(Instant.ofEpochMilli(e.t), place), NotifyFeature.SKY.tab)
        }

    private fun moon(context: Context, place: AppPlace, now: Instant): List<Planned> {
        val ev = SunMoonPrefs.moonEvents
        val lead = SunMoonPrefs.moonLead * 60L
        val out = ArrayList<Planned>()
        fun add(id: String, ms: Long, title: String) {
            val at = Instant.ofEpochMilli(ms).minusSeconds(lead)
            if (at.isAfter(now) && out.none { it.id == id }) out += Planned(id, at, title, whenText(Instant.ofEpochMilli(ms), place), NotifyFeature.MOON.tab)
        }
        var t = now.toEpochMilli()
        repeat(3) {
            val m = Nature.moon(t)
            for ((ms, key) in listOf(m.nextNewMoon to "new", m.nextFullMoon to "full")) {
                if (key in ev) add("moon-$key-${ms / 60000}", ms, L("nature.phases.$key"))
            }
            t = minOf(m.nextNewMoon, m.nextFullMoon) + 3_600_000
        }
        if ("quarter" in ev) {
            val (fq, lq) = MoonDetails.quarters(now.toEpochMilli())
            add("moon-fq-${fq / 60000}", fq, L("nature.phases.firstQuarter"))
            add("moon-lq-${lq / 60000}", lq, L("nature.phases.lastQuarter"))
        }
        if ("super" in ev) {
            val ms = now.toEpochMilli()
            MoonDetails.superMoon(ms, MoonDetails.perigeeApogee(ms).first)?.let { add("moon-super-${it / 60000}", it, L("moon.ev.super")) }
        }
        if ("eclipse" in ev) {
            for (e in Eclipse.all(context).filter { it.ut > now.toEpochMilli() && it.ut < now.toEpochMilli() + 60L * 86_400_000 }) {
                add("moon-ecl-${e.ut / 60000}", e.ut, e.title)
            }
        }
        return out
    }

    /** Her gün Güneş doğarken ve batarken (vakit düzeltmeleri olmadan; Mevsimler'deki saatler) */
    /** Günün sözü (2. analiz #16): her gün 08.00'de (telefonun saatiyle) takvim yaprağındaki söz */
    private fun daily(context: Context, now: Instant, days: Int): List<Planned> {
        val zone = java.time.ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        return (0 until days).mapNotNull { i ->
            val day = today.plusDays(i.toLong())
            val text = Sayings.today(context, day) ?: return@mapNotNull null
            Planned("daily-$day", day.atTime(8, 0).atZone(zone).toInstant(), L("notify.n.daily"), text, NotifyFeature.DAILY.tab)
        }
    }

    /** Cuma hatırlatması (2. analiz #58): Cuma günü öğle vaktinden 45 dakika önce */
    private fun friday(place: AppPlace, now: Instant, days: Int): List<Planned> {
        val today = place.today(now)
        return (0 until days).mapNotNull { i ->
            val day = today.plusDays(i.toLong())
            if (day.dayOfWeek != java.time.DayOfWeek.FRIDAY) return@mapNotNull null
            val dh = place.entries(day).firstOrNull { it.prayer == tr.bumba.time.core.Prayer.DHUHR } ?: return@mapNotNull null
            Planned("fri-${dh.at.epochSecond / 60}", dh.at.minusSeconds(45 * 60), L("notify.n.friday"),
                T("notify.n.fridayBody", Fmt.hm(dh.at, place.zone), place.name), NotifyFeature.FRIDAY.tab)
        }
    }

    private fun sun(place: AppPlace, now: Instant, days: Int): List<Planned> {
        val p = PrayerTimes.Parameters(adjustments = PrayerTimes.Adjustments(sunrise = 0.0, dhuhr = 0.0, maghrib = 0.0))
        val today = place.today(now)
        val ev = SunMoonPrefs.sunEvents
        val lead = SunMoonPrefs.sunLead * 60L
        val dayset = SunMoonPrefs.sunDays
        return (0 until days).flatMap { i ->
            val day = today.plusDays(i.toLong())
            if (day.dayOfWeek.value !in dayset) return@flatMap emptyList()
            val t = PrayerTimes.times(day, place.lat, place.lon, place.zone, p)
            val start = day.atStartOfDay(place.zone).toInstant()
            fun at(h: Double?) = h?.let { start.plusSeconds((it * 3600).toLong()) }
            val off = place.zone.rules.getOffset(day.atTime(12, 0).atZone(place.zone).toInstant()).totalSeconds / 3600.0
            val light = if ("goldenAm" in ev || "goldenPm" in ev) PrayerTimes.lightHours(day.year, day.monthValue, day.dayOfMonth, place.lat, place.lon, off) else null
            listOfNotNull(
                if ("sunrise" in ev) at(t.sunrise)?.let { Triple("sunrise", it, L("nature.sunrise")) } else null,
                if ("sunset" in ev) at(t.maghrib)?.let { Triple("sunset", it, L("nature.sunset")) } else null,
                // Altın saat başlangıcı: sabah Güneş −4°'den çıkarken, akşam +6°'ye inerken
                if ("goldenAm" in ev) at(light?.goldenAm?.first)?.let { Triple("goldenam", it, L("nature.goldenAm")) } else null,
                if ("goldenPm" in ev) at(light?.goldenPm?.first)?.let { Triple("goldenpm", it, L("nature.goldenPm")) } else null,
            ).mapNotNull { (key, ev0, title) ->
                val fire = ev0.minusSeconds(lead)
                if (!fire.isAfter(now)) null
                else Planned("$key-${ev0.epochSecond / 60}", fire, title, "${place.name} · ${Fmt.hm(ev0, place.zone)}", NotifyFeature.SUN.tab)
            }
        }
    }

    /** Telefonun saat diliminde saatler değişmeden önceki akşam 20.00'de (60 gün içindeyse) */
    private fun dst(now: Instant): List<Planned> {
        val zone = ZoneId.systemDefault()
        val tr = zone.rules.nextTransition(now) ?: return emptyList()
        if (tr.instant.isAfter(now.plusSeconds(60L * 86400))) return emptyList()
        val delta = tr.offsetAfter.totalSeconds - tr.offsetBefore.totalSeconds
        if (delta == 0) return emptyList()
        val fire = tr.instant.atZone(zone).toLocalDate().minusDays(1).atTime(20, 0).atZone(zone).toInstant()
        val amount = if (abs(delta) == 3600) L("notify.n.oneHour") else T("notify.n.minutes", abs(delta) / 60)
        return listOf(
            Planned("dst-${tr.instant.epochSecond}", fire, T(if (delta > 0) "notify.n.dstForward" else "notify.n.dstBack", amount),
                Fmt.longDate(tr.instant.atZone(zone).toLocalDate()), NotifyFeature.DST.tab),
        )
    }

    /** Dünya saatlerindeki şehirlerde saat değişmeden önceki akşam 20.00'de (telefonun saatiyle; rakip analizi #30).
     *  Telefonun kendi değişimiyle aynı an olan şehir atlanır (o bildirim zaten var). */
    private fun worldDst(now: Instant): List<Planned> {
        val zone = ZoneId.systemDefault()
        val mine = zone.rules.nextTransition(now)?.instant
        val keys = Prefs.world.split(",")
        return keys.mapNotNull { k -> WorldEntry.of(k) }.mapNotNull { city ->
            val (at, _) = city.nextDst(now, 30) ?: return@mapNotNull null
            if (at == mine) return@mapNotNull null
            val fire = at.atZone(zone).toLocalDate().minusDays(1).atTime(20, 0).atZone(zone).toInstant()
            Planned("wdst-${city.key}-${at.epochSecond}", fire, T("world.dstTitle", city.displayName), city.dstNote(now, 30) ?: "", Tab.WORLD)
        }
    }

    /** Saat başı: seçili saatler arasında, önümüzdeki iki gün (her bildirimde plan ileri taşınır) */
    private fun hourly(now: Instant): List<Planned> {
        val zone = ZoneId.systemDefault()
        val from = FeaturePrefs.hourlyFrom
        val to = FeaturePrefs.hourlyTo
        val hours = if (from <= to) (from..to).toList() else (from..23).toList() + (0..to).toList()
        val today = now.atZone(zone).toLocalDate()
        return (0..1).flatMap { d ->
            hours.map { h ->
                val at = today.plusDays(d.toLong()).atTime(h, 0).atZone(zone)
                val label = listOf(Fmt.time(at), Fmt.ampm(at)).filter { it.isNotEmpty() }.joinToString(" ")
                Planned("hourly-${at.toEpochSecond()}", at.toInstant(), T("notify.n.hourly", label), "", NotifyFeature.HOURLY.tab)
            }
        }
    }

    /** Her sabah günün tahmini (önümüzdeki üç gün; liste her açılışta ve her bildirimde tazelenir) */
    private suspend fun weather(context: Context, place: AppPlace, now: Instant): List<Planned> {
        val w = withTimeoutOrNull(8000) { WeatherCache.weather(place.lat, place.lon) } ?: return emptyList()
        val percent = NumberFormat.getPercentInstance(context.resources.configuration.locales[0])
        val minutes = FeaturePrefs.weatherMinutes.toLong()
        return w.daily.take(3).map { day ->
            val at = day.date.atStartOfDay(place.zone).plusMinutes(minutes).toInstant()
            val body = T("notify.n.weatherBody", L("weather.codes.${WeatherCode.of(day.code).key}"), Units.temp(day.max),
                Units.temp(day.min), percent.format(day.rain / 100.0))
            Planned("weather-${day.date}", at, T("notify.n.weather", place.name), "$body\nOpen-Meteo.com", NotifyFeature.WEATHER.tab)
        } + if (Prefs.store.getBoolean("notify.weather.alerts", true)) weatherAlerts(w, place, now) else emptyList()
    }

    /** Hava uyarıları (2. analiz #110): akşam 19.00'da "yarın belirgin şekilde soğuk/sıcak" (en yüksek 5° ve fazla fark) ve
     *  önümüzdeki 24 saatte yağış ihtimali %60'ı geçen ilk saatten 15 dk önce "yağmur yaklaşıyor" (saatlik tahminden) */
    internal fun weatherAlerts(w: WeatherData, place: AppPlace, now: Instant): List<Planned> {
        val out = ArrayList<Planned>()
        val title = T("notify.n.weather", place.name)
        for (i in 0 until minOf(2, w.daily.size - 1)) {
            val a = w.daily[i]; val b = w.daily[i + 1]
            val s = tr.bumba.time.core.WeatherWords.tomorrowAlert(a.max, b.max) ?: continue
            val at = a.date.atTime(19, 0).atZone(place.zone).toInstant()
            if (at.isAfter(now)) out += Planned("weather-alert-${b.date}", at, title, T(s.key, Units.delta(s.n)) + "\nOpen-Meteo.com", NotifyFeature.WEATHER.tab)
        }
        w.hourly.firstOrNull { it.at.isAfter(now.plusSeconds(1800)) && it.at.isBefore(now.plusSeconds(86_400)) && it.rain >= 60 }?.let { h ->
            out += Planned("weather-rain-${h.at.epochSecond}", h.at.minusSeconds(900), title,
                T("weather.alert.rainSoon", Fmt.hm(h.at, place.zone)) + "\nOpen-Meteo.com", NotifyFeature.WEATHER.tab)
        }
        return out
    }

    /** "30 Ekim 2026 Cuma · 14:25" (yerin saatiyle) */
    private fun whenText(at: Instant, place: AppPlace): String {
        val z = at.atZone(place.zone)
        return "${Fmt.longDate(z.toLocalDate())} · ${Fmt.hm(at, place.zone)}"
    }

    /** Bildirimi gösterir; dokununca ilgili sekme açılır */
    fun deliver(context: Context, id: String, title: String, body: String, tab: Int) {
        if (!PrayerNotifier.notificationsAllowed(context)) return
        val open = PendingIntent.getActivity(
            context, 200 + tab,
            Intent(context, MainActivity::class.java).putExtra("startTab", tab)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(title)
            .apply { if (body.isNotEmpty()) setContentText(body.lineSequence().first()).setStyle(NotificationCompat.BigTextStyle().bigText(body)) }
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setGroup(id.substringBefore('-'))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify("feature", id.hashCode(), n) }
    }
}

/** Özellik alarmı geldi: bildirim, sonra plan yeniden kurulur (hava için kısa ağ beklemesi) */
class FeatureAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id")
        val title = intent.getStringExtra("title")
        val kind = id?.substringBefore('-')
        val feature = NotifyFeature.entries.firstOrNull { it.id == kind } ?: when (kind) {
            "cal" -> NotifyFeature.CALENDAR
            "season" -> NotifyFeature.SEASONS
            "sunrise", "sunset" -> NotifyFeature.SUN
            else -> null
        }
        if (id != null && title != null && (feature == null || FeaturePrefs.isOn(feature))) {
            FeatureNotifier.deliver(context, id, title, intent.getStringExtra("body").orEmpty(), intent.getIntExtra("tab", 0))
        }
        // Alıcının süresi sınırlı: plan bitmese de en geç 9 sn'de kapanır (sonraki açılışta yeniden kurulur)
        val pending = goAsync()
        val finished = java.util.concurrent.atomic.AtomicBoolean(false)
        val finish = { if (finished.compareAndSet(false, true)) pending.finish() }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 9000)
        FeatureNotifier.reschedule(context) { finish() }
    }
}

/** Güneş ve Ay bildirimlerinin ayrıntısı (2. analiz #152; iOS'taki SunMoonPrefs ile aynı anahtarlar) */
object SunMoonPrefs {
    val MOON_EVENTS = listOf("new", "full", "quarter", "super", "eclipse")
    val SUN_EVENTS = listOf("sunrise", "sunset", "goldenAm", "goldenPm")
    val MOON_LEADS = listOf(0, 60, 180, 1440)
    val SUN_LEADS = listOf(0, 5, 10, 15, 30, 60)

    private fun set(key: String, def: String) = (Prefs.store.getString(key, def) ?: def).split(",").filter { it.isNotEmpty() }.toSet()

    var moonEvents: Set<String>
        get() = set("notify.moon.events", "new,full")
        set(v) = Prefs.store.edit().putString("notify.moon.events", MOON_EVENTS.filter { it in v }.joinToString(",")).apply()
    var sunEvents: Set<String>
        get() = set("notify.sun.events", "sunrise,sunset")
        set(v) = Prefs.store.edit().putString("notify.sun.events", SUN_EVENTS.filter { it in v }.joinToString(",")).apply()
    var moonLead: Int
        get() = Prefs.store.getInt("notify.moon.lead", 0)
        set(v) = Prefs.store.edit().putInt("notify.moon.lead", v).apply()
    var sunLead: Int
        get() = Prefs.store.getInt("notify.sun.lead", 0)
        set(v) = Prefs.store.edit().putInt("notify.sun.lead", v).apply()
    /** ISO hafta günleri (1 pazartesi … 7 pazar) */
    var sunDays: Set<Int>
        get() = set("notify.sun.days", "1,2,3,4,5,6,7").mapNotNull { it.toIntOrNull() }.toSet()
        set(v) = Prefs.store.edit().putString("notify.sun.days", (1..7).filter { it in v }.joinToString(",")).apply()

    fun leadLabel(min: Int): String = when {
        min == 0 -> L("notify.lead.now")
        min % 1440 == 0 -> T("notify.lead.days", min / 1440)
        min % 60 == 0 -> T("notify.lead.hours", min / 60)
        else -> T("notify.lead.minutes", min)
    }
}

