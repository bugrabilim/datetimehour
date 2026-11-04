package tr.bumba.time

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Süren sayaçlar (iOS Live Activity / Dynamic Island karşılığı, rakip analizi #2): çalışan zamanlayıcılar, pomodoro,
 * kronometre ve isteğe bağlı "sıradaki vakit" (Ramazan'da iftar ve sahur) bildirim alanında ve kilit ekranında kalıcı
 * bildirim olarak; süre sistemin sayacıyla akar (Chronometer). Kayıtlı durumdan kurulur: araçlarda durum değişince,
 * uygulama açılınca ve bir sonraki bitişte/vakitte (LiveRefreshReceiver) yeniden çizilir, bitenler kalkar.
 * İsteğe bağlı hava durumu (4 Kasım 2026; iOS'taki kilit ekranı / Dynamic Island hava etkinliğinin karşılığı):
 * sıcaklık durum çubuğunda (Android 16 çipi) ve kilit ekranında; Open-Meteo'dan saatte bir yenilenir.
 */
object LiveNotifications {
    const val CHANNEL = "live"
    const val PRAYER_KEY = "live.prayer"
    /** Güneş sayacı (2. analiz #150): sıradaki gün doğumu ya da batımına kalan süre */
    const val SUN_KEY = "live.sun"
    /** Bildirim alanında ve kilit ekranında hava (varsayılan kapalı) */
    const val WEATHER_KEY = "live.weather"
    private const val WEATHER_SNAP = "live.weather.snap"
    private const val WEATHER_EVERY = 60 * 60_000L
    private const val IDS_KEY = "live.ids"

    private fun nid(key: String) = ("live-$key").hashCode()

    private class Item(val key: String, val title: String, val text: String, val whenMs: Long, val countDown: Boolean,
                       val tab: Tab, val tool: String?, val icon: Int,
                       /** Sayaçsız bildirim (hava): durum çubuğu çipinde bu yazı */
                       val chip: String? = null)

    fun sync(c: Context, now: Long = System.currentTimeMillis()) {
        val app = c.applicationContext
        // Zamanlayıcı ve odak widget'ları da süren sayaçlarla birlikte yenilenir (2. analiz #136)
        runCatching { TimeWidgets.refresh(app) }
        val nm = NotificationManagerCompat.from(app)
        val items = ArrayList<Item>()
        for (t in TimerItem.load()) if (t.running && t.end > now) {
            items += Item("timer-${t.id}", t.name.ifBlank { L("tools.timer") }, Fmt.duration(t.total / 1000), t.end, true,
                Tab.TOOLS, "timer", R.drawable.ic_stat_alarm)
        }
        val sp = Prefs.store
        val swStart = sp.getLong("sw.start", 0L)
        if (swStart > 0) {
            items += Item("stopwatch", L("tools.hub.cards.stopwatch.name"), "", swStart - sp.getLong("sw.acc", 0L), false,
                Tab.TOOLS, "stopwatch", R.drawable.ic_stat_alarm)
        }
        val pomoEnd = sp.getLong("pomo.end", 0L)
        // Arka plan sesi çalarken pomodoro bildirimini ses hizmeti çizer (tek bildirim)
        if (pomoEnd > now && SoundService.source != "pomo") {
            val phase = sp.getString("pomo.phase", "work")!!
            items += Item("pomodoro", L("tools.pomodoro.$phase"), T("tools.pomodoro.round", sp.getInt("pomo.round", 1), sp.getInt("pomo.rounds", 4)),
                pomoEnd, true, Tab.TOOLS, "pomodoro", R.drawable.ic_stat_alarm)
        }
        val prayer = if (sp.getBoolean(PRAYER_KEY, false)) prayerItem(now) else null
        prayer?.let { items += it.first }
        val sun = if (sp.getBoolean(SUN_KEY, false)) sunItem(now) else null
        sun?.let { items += it.first }
        val weather = if (sp.getBoolean(WEATHER_KEY, false)) weatherItem(now) else null
        weather?.let { items += it }

        val wanted = items.map { nid(it.key) }.toSet()
        for (id in sp.getStringSet(IDS_KEY, emptySet())!!.mapNotNull { it.toIntOrNull() }) if (id !in wanted) nm.cancel(id)
        if (items.isNotEmpty() && AlarmScheduler.canNotify(app)) {
            channel(app)
            for (it in items) runCatching { nm.notify(nid(it.key), build(app, it)) }
        }
        sp.edit().putStringSet(IDS_KEY, wanted.map { it.toString() }.toSet()).apply()

        // Bir sonraki değişim: en yakın bitiş ya da vakit (geçenler kalksın, vakit ileri taşınsın)
        // Hava açıksa en geç saatte bir yenilenir (alıcı Open-Meteo'dan yeni veri alır)
        val weatherNext = if (sp.getBoolean(WEATHER_KEY, false)) maxOf(now + 60_000, weatherBase() + WEATHER_EVERY) else null
        val next = (items.filter { it.countDown }.map { it.whenMs } + listOfNotNull(prayer?.second, sun?.second, weatherNext)).filter { it > now }.minOrNull()
        schedule(app, next)
    }

    private fun weatherAt(): Long = runCatching { JSONObject(Prefs.store.getString(WEATHER_SNAP, null)!!).getLong("at") }.getOrDefault(0L)

    /** Son alınan veri ya da son deneme (bağlantı yokken her dakika denenmesin; en geç saatte bir) */
    private fun weatherBase(): Long = maxOf(weatherAt(), Prefs.store.getLong("live.weather.try", 0L))

    /** Hava özeti eskidi mi (yenileme zamanı geldi mi) */
    fun weatherDue(now: Long = System.currentTimeMillis()): Boolean =
        Prefs.store.getBoolean(WEATHER_KEY, false) && now - weatherBase() >= WEATHER_EVERY - 5 * 60_000

    /** Seçili yerin güncel havası alınır ve son özet olarak saklanır (sayılar °C; yazı çizerken dile ve birime göre) */
    suspend fun refreshWeather(@Suppress("UNUSED_PARAMETER") c: Context) {
        Prefs.store.edit().putLong("live.weather.try", System.currentTimeMillis()).apply()
        val place = AppPlace.current()
        val d = WeatherCache.weather(place.lat, place.lon) ?: return
        val o = JSONObject().put("t", d.current.temp).put("c", d.current.code).put("d", d.current.isDay)
            .put("p", place.name).put("at", System.currentTimeMillis())
        d.today?.let { o.put("hi", it.max).put("lo", it.min) }
        Prefs.store.edit().putString(WEATHER_SNAP, o.toString()).apply()
    }

    /** Aç/kapa (Hava ekranı, bildirimdeki Kapat, Hızlı Ayarlar kutucuğu): açınca güncel hava alınıp bildirim çizilir */
    fun setWeather(c: Context, on: Boolean) {
        val app = c.applicationContext
        Prefs.store.edit().putBoolean(WEATHER_KEY, on).apply()
        sync(app)
        if (on) CoroutineScope(Dispatchers.Default).launch {
            withTimeoutOrNull(15_000) { refreshWeather(app) }
            sync(app)
        }
        runCatching {
            android.service.quicksettings.TileService.requestListeningState(app,
                android.content.ComponentName(app, WeatherTileService::class.java))
        }
    }

    /** Son hava özetinden bildirim (12 saatten eskiyse yok) */
    private fun weatherItem(now: Long): Item? {
        val o = runCatching { JSONObject(Prefs.store.getString(WEATHER_SNAP, null)!!) }.getOrNull() ?: return null
        val at = o.optLong("at")
        if (now - at > 12 * 3_600_000L) return null
        val temp = Units.temp(o.getDouble("t"))
        val code = WeatherCode.of(o.getInt("c"))
        val hi = o.optDouble("hi"); val lo = o.optDouble("lo")
        val text = listOfNotNull(o.optString("p").ifBlank { null },
            if (!hi.isNaN() && !lo.isNaN()) "↑${Units.temp(hi)} ↓${Units.temp(lo)}" else null, "Open-Meteo.com").joinToString(" · ")
        return Item("weather", "$temp · ${code.label}", text, at, false, Tab.WEATHER, null, R.drawable.ic_stat_weather, chip = temp)
    }

    /** Sıradaki vakit ve o vaktin anı */
    private fun prayerItem(now: Long): Pair<Item, Long>? {
        val place = AppPlace.current()
        val next = place.next(Instant.ofEpochMilli(now)) ?: return null
        val day = next.at.atZone(place.zone).toLocalDate()
        val ramadan = AppData.ramadans.any { day in it }
        val name = when {
            ramadan && next.prayer == tr.bumba.time.core.Prayer.MAGHRIB -> L("live.iftar")
            ramadan && next.prayer == tr.bumba.time.core.Prayer.FAJR -> L("live.sahur")
            else -> next.prayer.title
        }
        val at = next.at.toEpochMilli()
        return Item("prayer", name + " " + Fmt.time(next.at.atZone(place.zone)), place.name, at, true,
            Tab.PRAYER, null, R.drawable.ic_stat_prayer) to at
    }

    /** Sıradaki gün doğumu ya da batımı (vakit düzeltmesi olmadan; bugün ve yarın) */
    private fun sunItem(now: Long): Pair<Item, Long>? {
        val place = AppPlace.current()
        val p = tr.bumba.time.core.PrayerTimes.Parameters(adjustments = tr.bumba.time.core.PrayerTimes.Adjustments(sunrise = 0.0, dhuhr = 0.0, maghrib = 0.0))
        val today = place.today(Instant.ofEpochMilli(now))
        val events = listOf(today, today.plusDays(1)).flatMap { d ->
            val t = tr.bumba.time.core.PrayerTimes.times(d, place.lat, place.lon, place.zone, p)
            val start = d.atStartOfDay(place.zone).toInstant().toEpochMilli()
            listOfNotNull(t.sunrise?.let { (start + it * 3_600_000).toLong() to true }, t.maghrib?.let { (start + it * 3_600_000).toLong() to false })
        }.filter { it.first > now }.sortedBy { it.first }
        val (at, rise) = events.firstOrNull() ?: return null
        val title = L(if (rise) "nature.sunrise" else "nature.sunset") + " " + Fmt.hm(Instant.ofEpochMilli(at), place.zone)
        return Item("sun", title, place.name, at, true, Tab.SEASONS, null, R.drawable.ic_stat_prayer) to at
    }

    private fun channel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, L("live.channel"), NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
    }

    private fun build(c: Context, it: Item) = NotificationCompat.Builder(c, CHANNEL)
        .setSmallIcon(it.icon)
        .setContentTitle(it.title)
        .apply { if (it.text.isNotEmpty()) setContentText(it.text) }
        .setWhen(it.whenMs)
        .setShowWhen(it.chip == null)
        .setUsesChronometer(it.chip == null)
        .setChronometerCountDown(it.countDown)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(if (it.key == "prayer") NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_STOPWATCH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        // Android 16 canlı güncelleme (2. analiz #170): durum çubuğunda çip olarak öne çıkar; vakitte çipte vaktin adı
        .setRequestPromotedOngoing(true)
        .apply { if (it.key == "prayer") setShortCriticalText(it.title.take(12)) }
        // Hava: çipte sıcaklık, bildirimde "Kapat"
        .apply {
            it.chip?.let { chip ->
                setShortCriticalText(chip)
                addAction(0, L("common.close"), PendingIntent.getBroadcast(c, nid(it.key),
                    Intent(c, LiveWeatherOffReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
        }
        .setContentIntent(PendingIntent.getActivity(c, nid(it.key),
            Intent(c, MainActivity::class.java).putExtra("startTab", it.tab.ordinal).apply { it.tool?.let { t -> putExtra("tool", t) } }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .build()

    private fun schedule(c: Context, at: Long?) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(c, 0, Intent(c, LiveRefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (at == null) { am.cancel(pi); return }
        val t = at + 1000
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, t, 60_000L, pi)
    }
}

/** Bir sayaç bitince ya da vakit girince süren sayaç bildirimleri yeniden çizilir */
class LiveRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        if (!LiveNotifications.weatherDue()) return LiveNotifications.sync(c)
        // Hava özeti eskidiyse önce Open-Meteo'dan yenilenir (alıcıya tanınan sürenin içinde)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                withTimeoutOrNull(8_000) { LiveNotifications.refreshWeather(c) }
                LiveNotifications.sync(c)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Hava bildirimindeki "Kapat" */
class LiveWeatherOffReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) = LiveNotifications.setWeather(c, false)
}
