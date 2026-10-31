package tr.bumba.time

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Sekme dışı ekranlar ve widget'lar: hava durumu (ağsız örnek veriyle), ayarlar, renk, hakkında, Google Takvimi
 * rehberi ve beş widget. Çıktı: app/build/screens/<ad>.png (ScreenshotTest gibi; karşılaştırma yapılmaz).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class MoreScreensTest {
    private val dir = System.getProperty("screens.dir") ?: "build/screens"

    private fun shot(name: String, qualifiers: String = "tr", dark: Boolean = false, imperial: Boolean = false, content: @Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers("+$qualifiers")
        Prefs.store.edit().putBoolean("notify.asked", true).commit()
        Prefs.saveAppearance(if (dark) "dark" else "light")
        Prefs.saveAccent("amber")
        // Robolectric'te bölge ABD: birimler açıkça metrik
        if (!imperial) Prefs.saveUnits("metric")
        captureRoboImage("$dir/$name.png") {
            AppTheme { Column(Modifier.fillMaxSize().background(Theme.c.bg)) { content() } }
        }
    }

    private fun page(name: String, title: String, dark: Boolean = false, content: @Composable () -> Unit) =
        shot(name, dark = dark) { SubBar(title, onBack = {}); content() }

    /** Paylaşım kartları (rakip analizi #16, #29): Canvas'la çizilen görseller doğrudan PNG olarak yazılır */
    @Test
    fun shareCards() {
        RuntimeEnvironment.setQualifiers("+tr")
        val place = AppPlace.current()
        val now = java.time.Instant.parse("2027-03-09T09:00:00Z") // Ramazan Bayramı 1. günü
        java.io.File(dir).mkdirs()
        java.io.FileOutputStream("$dir/tr-paylas-vakitler.png").use { ShareCards.prayer(place, now).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val cd = CountdownItem(title = "Yaz tatili", emoji = "🏖️", target = java.time.Instant.parse("2027-06-25T09:00:00Z").toEpochMilli())
        java.io.FileOutputStream("$dir/tr-paylas-geri-sayim.png").use { ShareCards.countdown(cd, now).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** İstanbul için örnek hava durumu (Open-Meteo yanıtı biçiminde) */
    private fun seedWeather(current: Int = 2) {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val hours = (0 until 48).map { now.plusHours(it.toLong()) }
        val days = (0 until 7).map { LocalDate.now().plusDays(it.toLong()) }
        val codes = listOf(0, 1, 2, 3, 61, 80, 95)
        val o = JSONObject()
            .put("timezone", "Europe/Istanbul")
            .put("current", JSONObject().put("temperature_2m", 18.4).put("apparent_temperature", 17.2)
                .put("relative_humidity_2m", 64).put("wind_speed_10m", 14.0).put("wind_direction_10m", 40)
                .put("pressure_msl", 1016.0).put("uv_index", 3.1).put("weather_code", current).put("is_day", 1).put("precipitation", 0))
            .put("hourly", JSONObject()
                .put("time", hours.map { it.toString() }.let { org.json.JSONArray(it) })
                .put("temperature_2m", org.json.JSONArray(hours.indices.map { 14 + 5 * kotlin.math.sin(it / 4.0) }))
                .put("weather_code", org.json.JSONArray(hours.indices.map { codes[it % codes.size] }))
                .put("precipitation_probability", org.json.JSONArray(hours.indices.map { (it * 7) % 60 }))
                .put("is_day", org.json.JSONArray(hours.map { if (it.hour in 7..18) 1 else 0 })))
            .put("daily", JSONObject()
                .put("time", org.json.JSONArray(days.map { it.toString() }))
                .put("weather_code", org.json.JSONArray(codes))
                .put("temperature_2m_max", org.json.JSONArray(listOf(20, 21, 19, 17, 15, 18, 22)))
                .put("temperature_2m_min", org.json.JSONArray(listOf(12, 13, 12, 10, 9, 11, 13)))
                .put("precipitation_probability_max", org.json.JSONArray(listOf(5, 10, 20, 40, 80, 60, 10))))
        val place = AppPlace.current()
        WeatherCache.seed(place.lat, place.lon, WeatherCache.parse(o))
    }

    @Test fun weather() {
        Prefs.saveProvince("istanbul")
        seedWeather()
        shot("tr-hava") { WeatherScreen() }
    }

    @Test fun weatherDarkImperial() {
        Prefs.saveProvince("istanbul")
        seedWeather()
        Prefs.saveUnits("imperial")
        shot("en-hava-koyu", "en", dark = true, imperial = true) { WeatherScreen() }
        Prefs.saveUnits("metric")
    }

    @Test fun settings() = page("tr-ayarlar", "Ayarlar") { SettingsScreen {} }
    @Test fun colors() = page("tr-renk", "Uygulama rengi") { ColorScreen() }
    @Test fun about() = page("tr-hakkinda", "Hakkında") { AboutScreen() }
    @Test fun aboutDark() = page("tr-hakkinda-koyu", "Hakkında", dark = true) { AboutScreen() }
    @Test fun google() = page("tr-google-takvim", "Google Takvimi") { GoogleCalendarGuide() }
    @Test fun notifications() = shot("tr-bildirimler") { NotificationsScreen {} }
    @Test fun countdowns() {
        val z = java.time.ZoneId.systemDefault()
        fun at(d: LocalDate, h: Int = 0) = d.atTime(h, 0).atZone(z).toInstant().toEpochMilli()
        // Fotoğraflı kart: sentez bir gün batımı görüntüsü (dosya uygulama dosyalarında "cd-a.jpg")
        val c = RuntimeEnvironment.getApplication()
        val bmp = android.graphics.Bitmap.createBitmap(400, 240, android.graphics.Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bmp).drawPaint(android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(0f, 0f, 0f, 240f, 0xFFFF8A3D.toInt(), 0xFF5B2A86.toInt(), android.graphics.Shader.TileMode.CLAMP)
        })
        java.io.File(c.filesDir, "cd-a.jpg").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
        CountdownItem.save(listOf(
            CountdownItem(id = "a", title = "Annemin doğum günü", emoji = "🎂", target = at(LocalDate.now().plusDays(23)), yearly = true, photo = 1),
            CountdownItem(id = "b", title = "Tatil", emoji = "🏖️", target = at(LocalDate.now().plusDays(61), 8)),
            CountdownItem(id = "c", title = "Sunum", emoji = "💼", target = System.currentTimeMillis() + 5 * 3_600_000L),
        ))
        page("tr-geri-sayimlar", "Zamanlayıcı") { CountdownsScreen() }
        CountdownItem.save(emptyList())
    }
    /** Gece modu: siyah zemin, kırmızı ve kısık saat (yıldızlı arka plan seçili olsa da düz siyah) */
    @Test fun nightMode() {
        android.provider.Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        Prefs.saveFsBg("stars")
        Prefs.saveNightMode("on")
        shot("tr-gece-modu") { FullscreenClock {} }
        Prefs.saveNightMode("off")
        Prefs.saveFsBg("plain")
    }

    /** Uyku (rakip analizi #17): plan açık (kalkış 07.00, 8 saat), döngüler, uyku sesleri */
    @Test fun sleep() {
        RuntimeEnvironment.setQualifiers("+tr")
        SleepPlan.on = true
        page("tr-uyku", L("sleep.title")) { SleepScreen() }
        SleepPlan.on = false
    }

    /** Alarm kapatma görevi (#13): matematik ve sallama */
    @Test fun wakeTasks() {
        RuntimeEnvironment.setQualifiers("+tr")
        shot("tr-alarm-gorevi", dark = true) {
            androidx.compose.foundation.layout.Column(Modifier.padding(24.dp)) {
                WakeTaskPanel(AlarmItem(id = "m", hour = 7, minute = 0, task = "math", taskLevel = 2)) {}
            }
        }
        shot("tr-alarm-gorevi-sallama") {
            androidx.compose.foundation.layout.Column(Modifier.padding(24.dp)) {
                WakeTaskPanel(AlarmItem(id = "s", hour = 7, minute = 0, task = "shake")) {}
            }
        }
    }

    /** Gün doğumu ışığı: alarmdan 8 dk önce (30 dk pencere), tam ekran saat sıcak renkle aydınlanmış */
    @Test fun sunriseLight() {
        val at = java.time.LocalDateTime.now().plusMinutes(8)
        AlarmItem.save(listOf(AlarmItem(id = "s", hour = at.hour, minute = at.minute, sunrise = 30)))
        shot("tr-gun-dogumu-isigi") { FullscreenClock {} }
        AlarmItem.save(emptyList())
    }

    /** Hareketli arka planlar (testte tek kare): gece gökyüzü ve renk akışıyla tam ekran saat */
    @Test fun animatedBackgrounds() {
        // "Animasyonları kapat" açık: arka plan durağan tek kare çizer (sonsuz kare döngüsü ekran görüntüsünü bekletmesin)
        android.provider.Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        Prefs.saveFsBg("stars")
        shot("tr-tam-ekran-yildizlar") { FullscreenClock {} }
        Prefs.saveFsBg("flow")
        shot("tr-tam-ekran-renk-akisi") { FullscreenClock {} }
        Prefs.saveFsBg("sky")
        page("tr-saat-gorunumu-gokyuzu", "Saat görünümü") { AppearanceScreen() }
        // Havaya göre: İstanbul'da yağmur ve kar (örnek veri)
        Prefs.saveProvince("istanbul")
        Prefs.saveFsBg("weather")
        seedWeather(61)
        shot("tr-tam-ekran-hava-yagmur") { FullscreenClock {} }
        seedWeather(73)
        shot("tr-tam-ekran-hava-kar") { FullscreenClock {} }
        Prefs.saveFsBg("plain")
    }
    @Test fun appearance() {
        Prefs.saveClockFont("serif")
        page("tr-saat-gorunumu", "Saat görünümü") { AppearanceScreen() }
        Prefs.saveClockFont("rounded")
    }
    @Test fun moon() {
        Prefs.saveProvince("istanbul")
        page("tr-ay-takvimi", "Ay takvimi ve tutulmalar") { MoonScreen() }
    }
    /** Kıble (Robolectric'te pusula sensörü yok: kuzey yukarıda sabit kadran) ve örnek verili yakındaki camiler */
    /** Android TV (#95): televizyon ekranı (yatay, 1080p oranı) */
    @Test fun tv() {
        Prefs.saveProvince("istanbul")
        shot("tr-tv", "tr-w960dp-h540dp-land", dark = true) { TvClock() }
        RuntimeEnvironment.setQualifiers("w412dp-h915dp-port")
        // Odakta engelleme kartı (#98) pomodoro sayfasında
        Prefs.store.edit().putBoolean(FocusBlock.ON, true).apply()
        FocusBlock.saveApps(setOf("com.example.video"))
        shot("tr-odak-engeli") { FocusBlockCard() }
        Prefs.store.edit().putBoolean(FocusBlock.ON, false).apply()
    }

    @Test fun qibla() {
        Prefs.saveProvince("istanbul")
        shot("tr-kible") { QiblaScreen {} }
        shot("ar-kible-koyu", "ar", dark = true) { QiblaScreen {} }
        val p = AppPlace.current()
        NearbyMosques.seed(p.lat, p.lon, listOf(
            Mosque("Süleymaniye Camii", 41.0162, 28.9640, 0.0),
            Mosque("Yeni Cami", 41.0171, 28.9706, 0.0),
            Mosque("Rüstem Paşa Camii", 41.0176, 28.9688, 0.0),
            Mosque("Sultanahmet Camii", 41.0054, 28.9768, 0.0),
        ))
        shot("tr-yakindaki-camiler") { NearbyMosquesScreen {} }
    }
    /** İbadet: Kur'an (liste, Fatiha, Arapça koyu), dualar, zikirmatik, namaz/oruç takibi, zekât */
    @Test fun worship() {
        Quran.bookmark = 18 to 10
        shot("tr-kuran") { QuranScreen {} }
        shot("tr-kuran-fatiha") { SuraScreen(1, 1) {} }
        shot("en-kuran-yasin", "en") { SuraScreen(36, 1) {} }
        shot("ar-kuran-koyu", "ar", dark = true) { SuraScreen(112, 1) {} }
        Quran.bookmark = null
        shot("tr-dualar") { DuasScreen {} }
        Prefs.store.edit().putString("zikir.kind", "subhanallah").putInt("zikir.target", 33).putBoolean("zikir.tesbihat", true)
            .putInt("zikir.count", 41).putInt("zikir.total", 1250).commit()
        shot("tr-zikirmatik") { ZikirScreen {} }
        Tracker.clear()
        val today = LocalDate.now()
        Tracker.setDayMask(today, 0b00111)
        for (i in 1..6) Tracker.setDayMask(today.minusDays(i.toLong()), 0b11111)
        Tracker.setKaza("fajr", 12); Tracker.setKaza("isha", 4); Tracker.setKaza("fast", 3)
        shot("tr-namaz-takibi") { TrackerScreen {} }
        Tracker.clear()
        Zakat.save(mapOf("cash" to 400_000.0, "goldGrams" to 20.0, "goldPrice" to 4_000.0, "debt" to 30_000.0))
        shot("tr-zekat") { ZakatScreen {} }
        Zakat.save(emptyMap())
    }
    @Test fun prayerExtras() {
        Prefs.store.edit().putString(PrayerMethod.METHOD, "mwl").putBoolean(PrayerMethod.HANAFI, true)
            .putInt(PrayerSilent.MINUTES, 30).putString(PrayerAlarms.KEY, "fajr,maghrib").commit()
        shot("tr-hesap-yontemi") { PrayerMethodScreen {} }
        shot("tr-vakit-alarmi") { NotificationSettingsScreen {} }
        Prefs.store.edit().remove(PrayerMethod.METHOD).remove(PrayerMethod.HANAFI).remove(PrayerSilent.MINUTES).remove(PrayerAlarms.KEY).commit()
        shot("tr-vakitler-yay") { PrayerScreen() }
    }
    @Test fun content() {
        shot("tr-esma") { EsmaScreen {} }
        shot("ar-esma", "ar") { EsmaScreen {} }
        shot("ur-esma", "ur") { EsmaScreen {} }
        shot("id-esma", "in") { EsmaScreen {} }
        shot("ru-hadis", "ru") { HadithScreen {} }
        shot("fa-namaz-rehberi", "fa") { PrayerGuideScreen({}) {} }
        shot("tr-hadis") { HadithScreen {} }
        shot("en-hadis-koyu", "en", dark = true) { HadithScreen {} }
        shot("tr-namaz-rehberi") { PrayerGuideScreen({}) {} }
        shot("tr-takvim-yapragi") { CalendarLeafScreen {} }
        shot("de-takvim-yapragi", "de") { CalendarLeafScreen {} }
    }
    @Test fun morning() {
        seedWeather()
        Prefs.store.edit().remove(Journal.KEY).commit()
        shot("tr-gunaydin") { MorningCard {} }
    }
    @Test fun journalAndPlanner() {
        // Bugün ve önceki günler (ruh hâli, uyku, niyet, not)
        val d = java.time.LocalDate.now()
        Journal.save(listOf(
            JournalEntry(d.toString(), 4, 3, "Yürüyüşe çıkmak"),
            JournalEntry(d.minusDays(1).toString(), 5, 5, "Kitap okumak", "Güzel bir gündü."),
            JournalEntry(d.minusDays(3).toString(), 2, 2, "", "Az uyudum."),
        ))
        shot("tr-sabah-gunlugu") { JournalScreen() }
        Prefs.store.edit().remove(Journal.KEY).commit()
        // Toplantı planlayıcı: Londra, New York, Tokyo
        Prefs.store.edit().putString("meet.cities", "london,new-york,tokyo").commit()
        shot("tr-toplanti-planlayici") { MeetingPlannerScreen() }
        Prefs.store.edit().remove("meet.cities").commit()
    }
    @Test fun badges() {
        // Örnek ilerleme: birkaç rozet kazanılmış, 6 model görülmüş, 2 gün seri
        BadgeStore.deleteAll()
        listOf(Badge.RENK, Badge.SANIYE, Badge.TAKVIM, Badge.KRONOMETRE).forEach { BadgeStore.hit(it) }
        (0 until 6).forEach { BadgeStore.sawModel(it, ClockModel.count) }
        BadgeStore.appOpened(LocalDate.now().minusDays(1)); BadgeStore.appOpened()
        page("tr-rozetler", "Rozetler") { BadgesScreen() }
        page("tr-rozetler-koyu", "Rozetler", dark = true) { BadgesScreen() }
        BadgeStore.deleteAll()
    }

    /** Widget'lar: RemoteViews gerçek görünüme uygulanıp çizilir (dp → px: xxhdpi = 3) */
    private fun widget(name: String, w: Int, h: Int, views: android.widget.RemoteViews) {
        val c = RuntimeEnvironment.getApplication()
        val host = FrameLayout(c)
        val v: View = views.apply(c, host)
        host.addView(v, ViewGroup.LayoutParams(w * 3, h * 3))
        host.measure(View.MeasureSpec.makeMeasureSpec(w * 3, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h * 3, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, w * 3, h * 3)
        // Activity'siz görünüm: doğrudan bitmap'e çizilir (gri zemin ana ekran gibi)
        val b = android.graphics.Bitmap.createBitmap(w * 3 + 60, h * 3 + 60, android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(b)
        cv.drawColor(0xFF6E7781.toInt())
        cv.translate(30f, 30f)
        host.draw(cv)
        java.io.File(dir).mkdirs()
        java.io.FileOutputStream("$dir/$name.png").use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun widgets() {
        RuntimeEnvironment.setQualifiers("+tr")
        Prefs.saveProvince("istanbul")
        val c = RuntimeEnvironment.getApplication()
        widget("widget-siradaki", 170, 170, NextPrayerWidget().views(c, 1))
        widget("widget-bugun", 360, 170, TodayPrayersWidget().views(c, 2))
        widget("widget-analog", 170, 170, AnalogClockWidget().views(c, 3))
        widget("widget-dijital", 170, 80, DigitalClockWidget().views(c, 4))
        widget("widget-saat-tarih", 170, 170, ClockDateWidget().views(c, 5))
        widget("widget-ay", 170, 170, MoonWidget().views(c, 6))
        widget("widget-hicri", 170, 170, HijriWidget().views(c, 7))
        widget("widget-saniyeli", 170, 170, SecondsClockWidget().views(c, 7))
        widget("widget-dunya", 360, 170, WorldClockWidget().views(c, 8))
        widget("widget-geri-sayim", 170, 170, CountdownWidget().views(c, 9))
        // Dokunulabilen widget'lar: zikirmatik (uygulamadaki sayaç) ve kronometre (durmuş, 2:05)
        Prefs.store.edit().putInt("zikir.count", 21).putInt("zikir.target", 33).putLong("sw.start", 0L).putLong("sw.acc", 125_000L).commit()
        widget("widget-zikir", 170, 170, ZikirWidget().views(c, 10))
        widget("widget-kronometre", 170, 170, StopwatchToolWidget().views(c, 11))
        ToolActions.zikirTap(c)
        assertEquals(22, Prefs.store.getInt("zikir.count", 0))
        ToolActions.swToggle(c)
        assertTrue(ToolActions.swRunning())
        ToolActions.swReset(c)
        assertEquals(0L, ToolActions.swElapsed())
        Prefs.store.edit().remove("zikir.count").remove("zikir.target").commit()
    }
}
