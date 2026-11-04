package tr.bumba.time

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

    private fun shot(
        name: String, qualifiers: String = "tr", dark: Boolean = false, imperial: Boolean = false,
        atmo: String = "", fontScale: Int = 100, content: @Composable () -> Unit,
    ) {
        RuntimeEnvironment.setQualifiers("+$qualifiers")
        Prefs.store.edit().putBoolean("notify.asked", true).putBoolean(DialFace.SEEN_KEY, true).commit()
        Prefs.saveAppearance(if (dark) "dark" else "light")
        Prefs.saveAccent("amber")
        Atmosphere.save(atmo)
        FontScale.save(fontScale)
        // Kurulum sihirbazı geçilmiş sayılır (yeni kurulum işareti sonraki testlere sızmasın)
        Onboarding.finish()
        // Hareketli arka planlar durağan tek kare çizer (sonsuz kare döngüsü ekran görüntüsünü bekletmesin)
        android.provider.Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        // Robolectric'te bölge ABD: birimler açıkça metrik
        if (!imperial) Prefs.saveUnits("metric")
        captureRoboImage("$dir/$name.png") {
            AppTheme { Column(Modifier.fillMaxSize().appBackdrop(Theme.c)) { content() } }
        }
        Atmosphere.save("")
        FontScale.save(100)
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
    private fun seedWeather(current: Int = 2, wind: Double = 14.0, precip: Double = 0.0, cloud: Int = 60) {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val hours = (0 until 48).map { now.plusHours(it.toLong()) }
        val days = (0 until 7).map { LocalDate.now().plusDays(it.toLong()) }
        val codes = listOf(0, 1, 2, 3, 61, 80, 95)
        val o = JSONObject()
            .put("timezone", "Europe/Istanbul")
            .put("current", JSONObject().put("temperature_2m", 18.4).put("apparent_temperature", 17.2)
                .put("relative_humidity_2m", 64).put("wind_speed_10m", wind).put("wind_direction_10m", 40).put("cloud_cover", cloud)
                .put("pressure_msl", 1016.0).put("uv_index", 3.1).put("weather_code", current).put("is_day", 1).put("precipitation", precip)
                .put("wind_gusts_10m", wind * 1.8).put("visibility", 24000).put("dew_point_2m", 11.2))
            .put("hourly", JSONObject()
                .put("time", hours.map { it.toString() }.let { org.json.JSONArray(it) })
                .put("temperature_2m", org.json.JSONArray(hours.indices.map { 14 + 5 * kotlin.math.sin(it / 4.0) }))
                .put("weather_code", org.json.JSONArray(hours.indices.map { codes[it % codes.size] }))
                .put("precipitation_probability", org.json.JSONArray(hours.indices.map { (it * 7) % 60 }))
                .put("is_day", org.json.JSONArray(hours.map { if (it.hour in 7..18) 1 else 0 }))
                .put("wind_speed_10m", org.json.JSONArray(hours.indices.map { wind + (it % 5) * 2 }))
                .put("apparent_temperature", org.json.JSONArray(hours.indices.map { 13 + 5 * kotlin.math.sin(it / 4.0) }))
                .put("precipitation", org.json.JSONArray(hours.indices.map { if (it % 7 == 4) 1.4 else 0.0 }))
                .put("wind_gusts_10m", org.json.JSONArray(hours.indices.map { wind * 1.8 + it % 4 }))
                .put("wind_direction_10m", org.json.JSONArray(hours.indices.map { (it * 25) % 360 }))
                .put("relative_humidity_2m", org.json.JSONArray(hours.indices.map { 55 + (it % 6) * 5 }))
                .put("uv_index", org.json.JSONArray(hours.map { if (it.hour in 10..15) 5 else 0 })))
            .put("daily", JSONObject()
                .put("time", org.json.JSONArray(days.map { it.toString() }))
                .put("weather_code", org.json.JSONArray(codes))
                .put("temperature_2m_max", org.json.JSONArray(listOf(20, 21, 19, 17, 15, 18, 22)))
                .put("temperature_2m_min", org.json.JSONArray(listOf(12, 13, 12, 10, 9, 11, 13)))
                .put("precipitation_probability_max", org.json.JSONArray(listOf(5, 10, 20, 40, 80, 60, 10)))
                .put("precipitation_sum", org.json.JSONArray(listOf(0, 0.4, 1.2, 3.5, 12.0, 6.1, 0))))
        val place = AppPlace.current()
        WeatherCache.seed(place.lat, place.lon, WeatherCache.parse(o))
    }

    @Test fun weather() {
        Prefs.saveProvince("istanbul")
        seedWeather()
        shot("tr-hava") { WeatherScreen() }
    }

    /** 2. analiz #107, #111, #113, #114: saatlik eğri, etkinlik uygunluğu, hava kalitesi ve polen, meteogram, modeller */
    @Test fun weatherExtras() {
        Prefs.saveProvince("istanbul")
        seedWeather()
        val place = AppPlace.current()
        AirCache.seed(place.lat, place.lon, AirData(34.0, 12.4, 21.0, 61.0, 18.0, mapOf("grass" to 32.0, "olive" to 8.0, "birch" to 0.0)))
        val days = (0 until 7).map { LocalDate.now().plusDays(it.toLong()) }
        ModelCache.seed(place.lat, place.lon, mapOf(
            "ECMWF" to days.mapIndexed { i, d -> ModelDay(d, 20.0 + i % 3, 12.0, if (i == 3) 4.2 else 0.0) },
            "GFS" to days.mapIndexed { i, d -> ModelDay(d, 21.0 + i % 2, 11.0, if (i == 3) 7.5 else 0.2) },
            "ICON" to days.mapIndexed { i, d -> ModelDay(d, 19.0 + i % 4, 12.0, 0.0) },
        ))
        shot("tr-hava-grafik-uygunluk") {
            androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                val d = WeatherCache.cached(place.lat, place.lon)!!
                HourlyChart(d)
                ActivitiesCard(d)
                AirCard(place)
            }
        }
        shot("tr-hava-meteogram") {
            androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                val d = WeatherCache.cached(place.lat, place.lon)!!
                ModelsCard(place)
                MeteogramCard(d)
            }
        }
    }

    /** Hava neyse ekran öyle (2. analiz #193): rüzgârlı yağmur, karlı, rüzgârlı açık hava */
    @Test fun liveWeather() {
        Prefs.saveProvince("istanbul")
        seedWeather(current = 63, wind = 48.0, precip = 6.0, cloud = 95)
        shot("tr-hava-yagmur-ruzgar") { WeatherScreen() }
        seedWeather(current = 73, wind = 12.0, precip = 2.0, cloud = 100)
        shot("tr-hava-kar", dark = true) { WeatherScreen() }
        seedWeather(current = 1, wind = 42.0, precip = 0.0, cloud = 10)
        shot("tr-hava-ruzgar") { WeatherScreen() }
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
        // Akıllı alarm penceresi seçili alarm varken Uyku ekranındaki akıllı alarm kartı (31 Ekim 2026)
        val saved = AlarmItem.load()
        AlarmItem.save(listOf(AlarmItem(id = "smart", hour = 7, minute = 0, days = (1..7).toList(), smart = 30)))
        page("tr-akilli-alarm", L("sleep.title")) { SleepScreen() }
        AlarmItem.save(saved)
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
        // Yeni görevler (2. analiz #36, #37): squat, nesne (bardak), sesli okuma
        shot("tr-alarm-gorevi-yeni") {
            androidx.compose.foundation.layout.Column(Modifier.padding(24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(28.dp)) {
                WakeTaskPanel(AlarmItem(id = "q", hour = 7, minute = 0, task = "squat")) {}
                WakeTaskPanel(AlarmItem(id = "o", hour = 7, minute = 0, task = "object", code = "obj:cup")) {}
                WakeTaskPanel(AlarmItem(id = "v", hour = 7, minute = 0, task = "speak")) {}
            }
        }
        // Alarm duvar kâğıdı (2. analiz #2): gün batımı zemini, yazılar koyu temanın açık renkleri
        shot("tr-alarm-gorevi-duvar") {
            AlarmWallScreen("sunset") {
                androidx.compose.foundation.layout.Column(Modifier.padding(24.dp)) {
                    WakeTaskPanel(AlarmItem(id = "w", hour = 7, minute = 0, task = "math", wall = "sunset")) {}
                }
            }
        }
    }

    /** 2. analiz #34 görev zinciri (1/2), #45 uyanma denetimi halkası */
    @Test fun wakeChainAndCheck() {
        RuntimeEnvironment.setQualifiers("+tr")
        shot("tr-alarm-gorev-zinciri") {
            androidx.compose.foundation.layout.Column(Modifier.padding(24.dp)) {
                WakeTaskPanel(AlarmItem(id = "c", hour = 7, minute = 0, task = "math", chain = listOf("typing"))) {}
            }
        }
        shot("tr-uyanma-denetimi") {
            WakeCheckScreen(System.currentTimeMillis() + 75_000L, {}, {})
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
    /** Gece gökyüzü (#155–#157), Güneş yolu (#158), çocuklar için Ay (#160) */
    /** Üretken zeminler (2. analiz #23) */
    @Test fun generativeBackgrounds() {
        RuntimeEnvironment.setQualifiers("+tr")
        for (b in listOf(FullscreenBackground.TOPO, FullscreenBackground.DOTS, FullscreenBackground.GRAIN, FullscreenBackground.LIQUID)) {
            shot("tr-zemin-${b.id}") { AnimatedBackground(b, androidx.compose.ui.Modifier.fillMaxSize()) }
        }
    }

    /** Dijital rakam görünümleri (2. analiz #9) */
    @Test fun digitalStyles() {
        RuntimeEnvironment.setQualifiers("+tr")
        shot("tr-dijital-gorunumler") {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
                for (st in listOf("led", "lcd", "matrix", "roll")) StyledDigits("12:34:56", st, 64.dp, 380.dp, Theme.c.ink)
                VerticalDigits("12", "34", "56", 160.dp, Theme.c.ink, null)
            }
        }
    }

    @Test fun worldExtras() {
        RuntimeEnvironment.setQualifiers("+tr")
        Prefs.saveProvince("istanbul")
        val now = java.time.Instant.parse("2026-11-04T09:30:00Z")
        val list = WorldEntry.list()
        WorldWeather.seed(list.associate { it.key to WorldWeather.Entry(18.0, 2, true) })
        page("tr-dunya-kadran", "24 saat kadranı") { WorldDial(list, now, onShift = {}) }
        page("tr-dunya-harita", "Harita") { WorldMapCard(list, now) }
        val c = RuntimeEnvironment.getApplication()
        widget("widget-dunya-8", 340, 300, World8Widget().views(c, 21))
        widget("widget-saat-cakismasi", 360, 220, OverlapWidget().views(c, 22))
        widget("widget-dunya-haritasi", 360, 190, WorldMapWidget().views(c, 23))
        TimerItem.save(listOf(TimerItem(name = "Çay", total = 300_000L, end = System.currentTimeMillis() + 240_000L)))
        widget("widget-zamanlayicilar", 340, 170, TimersWidget().views(c, 24))
        widget("widget-odak", 170, 170, FocusWidget().views(c, 25))
        TimerItem.save(emptyList())
    }

    @Test fun sky() {
        Prefs.saveProvince("istanbul")
        page("tr-gece-gokyuzu", "Gece gökyüzü") { SkyScreen() }
        page("tr-gunes-yolu", "Güneş yolu") { SunPathScreen() }
        shot("tr-cocuklar-ay") { androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(16.dp)) { KidsMoonCard() } }
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
        QiblaPrefs.toggle(Mosque("Süleymaniye Camii", 41.0162, 28.9640, 0.0))
        shot("tr-yakindaki-camiler") { NearbyMosquesScreen {} }
        QiblaPrefs.saveStyle("gold")
        shot("tr-kible-altin", dark = true) { QiblaScreen {} }
        shot("tr-kible-harita") { androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(16.dp)) { QiblaRouteMap(41.0, 29.0, "İstanbul"); QiblaRouteMap(40.7, -74.0, "New York") } }
        QiblaPrefs.saveStyle("classic")
        // Büyük daire: İstanbul'dan Kâbe'ye çizgi başta İstanbul'da, sonda Kâbe'de; kesit 2:1
        val route = QiblaPrefs.route(41.0, 29.0)
        assertEquals(41.0, route.first().first, 1e-6)
        assertEquals(QiblaPrefs.KAABA_LON, route.last().second, 1e-6)
        val fr = QiblaPrefs.frame(route)
        assertTrue(fr.third in 30.0..90.0)
        // Samoa: yol tarih değiştirme çizgisini geçiyorsa tüm dünya gösterilir, sınır dışına çıkılmaz
        val f2 = QiblaPrefs.frame(QiblaPrefs.route(-13.8, -171.8))
        assertTrue(f2.first >= -180.0 && f2.first + f2.third <= 180.0)
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
    /** 2. analiz 1. dalga D: dini günler, tebrik kartı, ibadet istatistiği, tesbih görünümü, hatim, Kur'an zemini, 24 saat kadranı */
    @Test fun worshipExtras() {
        shot("tr-dini-gunler") { ReligiousDaysScreen {} }
        shot("tr-tebrik") { GreetingScreen {} }
        Tracker.clear()
        val today = LocalDate.now()
        val masks = listOf(0b11111, 0b01111, 0b10111, 0b11111, 0b00011, 0b11101, 0)
        for (i in 0..200) Tracker.setDayMask(today.minusDays(i.toLong()), masks[i % masks.size])
        for (i in 1..10) { TrackX.recordWhen(today.minusDays(i.toLong()), 0, 10 + i); TrackX.recordWhen(today.minusDays(i.toLong()), 1, 30 + i * 2) }
        for (k in Tracker.KAZA) Tracker.setKaza(k, 900)
        Tracker.setKaza("fast.illness", 2); Tracker.setKaza("fast.travel", 1)
        repeat(60) { TrackX.logKazaPrayed(today.minusDays((it % 20).toLong())) }
        shot("tr-ibadet-istatistik") { TrackerScreen {} }
        Tracker.clear()
        Prefs.store.edit().putBoolean("zikir.beads", true).putBoolean("zikir.tesbihat", false).putString("zikir.kind", "elhamdulillah")
            .putInt("zikir.target", 33).putInt("zikir.count", 21).commit()
        shot("tr-zikir-tesbih") { ZikirScreen {} }
        Prefs.store.edit().remove("zikir.beads").remove("zikir.count").commit()
        Quran.bookmark = 36 to 1
        Prefs.store.edit().putString("quran.theme", "sepia").putInt("quran.goal", 20)
            .putString("quran.hatim", org.json.JSONObject().put("s", today.minusDays(40).toString()).put("i", 0).put("n", 2).toString()).commit()
        shot("tr-kuran-hatim") { QuranScreen {} }
        shot("tr-kuran-sepya") { SuraScreen(36, 1) {} }
        Quran.bookmark = null
        Prefs.store.edit().remove("quran.theme").remove("quran.goal").remove("quran.hatim").commit()
        Prefs.store.edit().putString("prayer.visual", "dial").putBoolean("prayer.words", true).commit()
        shot("tr-vakitler-kadran") { PrayerScreen() }
        Prefs.store.edit().remove("prayer.visual").remove("prayer.words").commit()
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
        shot("ja-esma", "ja") { EsmaScreen {} }
        shot("th-esma", "th") { EsmaScreen {} }
        shot("zh-hadis", "zh") { HadithScreen {} }
        shot("ko-namaz-rehberi", "ko") { PrayerGuideScreen({}) {} }
        shot("fil-hadis", "b+fil") { HadithScreen {} }
        shot("tr-hadis") { HadithScreen {} }
        shot("en-hadis-koyu", "en", dark = true) { HadithScreen {} }
        shot("tr-namaz-rehberi") { PrayerGuideScreen({}) {} }
        // Resimli rehber (2. analiz #68): bütün duruşlar, erkek ve kadın
        shot("tr-namaz-rehberi-cizimler") {
            androidx.compose.foundation.layout.Column(Modifier.padding(8.dp)) {
                for (female in listOf(false, true)) androidx.compose.foundation.layout.Row {
                    for (id in listOf("stand", "takbir", "fold", "ruku", "sajda", "sit", "salam"))
                        GuideFigure(id, female, Theme.c.ink, Theme.c.accent, Modifier.size(56.dp))
                }
            }
        }
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
        Prefs.store.edit().putString("meet.cities", "london,new-york,tokyo").putBoolean("meet.exchanges", true).putString("meet.h12", "new-york").commit()
        shot("tr-toplanti-planlayici") { MeetingPlannerScreen() }
        Prefs.store.edit().remove("meet.cities").remove("meet.exchanges").remove("meet.h12").commit()
    }
    /** Sahibin ekran görüntülerinden (2. analiz #6, #77, #78, #81) */
    /** Ders çalışma sayacı (2. analiz #84): LED süre ve soru sayacı, istatistik */
    @Test fun study() {
        val today = LocalDate.now()
        var log = Study.record(emptyList(), today.minusDays(2), "Fizik", 50 * 60_000L, 24, "Optik")!!
        log = Study.record(log, today, "Matematik", 40 * 60_000L, 35, "Türev tekrarı")!!
        log = Study.record(log, today, "", 25 * 60_000L, 12, "")!!
        Study.save(log)
        Study.saveSubjects(listOf("Matematik", "Fizik", "Kimya"))
        Prefs.store.edit().putString(Study.SUBJECT, "Matematik").putString("study.mode", "timer")
            .putLong("study.start", 0).putLong("study.acc", (1 * 3600 + 12 * 60 + 5) * 1000L).putInt("study.q", 27).commit()
        page("tr-ders-sayaci", "Ders çalışma") { StudyScreen() }
        page("tr-ders-sayaci-koyu", "Ders çalışma", dark = true) { StudyScreen() }
        Study.clear(); Study.saveSubjects(emptyList())
        Prefs.store.edit().remove("study.acc").remove("study.q").remove(Study.SUBJECT).commit()
    }

    /** Uyanma oyunu (2. analiz #7): civciv uyuyor / uyandı, rapor */
    @Test fun wakeGame() {
        val today = LocalDate.now()
        val log = (12 downTo 1).map { WakeGame.Event(today.minusDays(it.toLong()).toString(), 7 * 60 + it % 4 * 5, 7 * 60, if (it % 5 == 0) 1 else 0, it % 3 == 0) }
        WakeGame.save(log)
        page("tr-uyanma-oyunu-uyuyor", "Uyanma oyunu") { WakeGameScreen() }
        WakeGame.save(log + WakeGame.Event(today.toString(), 6 * 60 + 58, 7 * 60, 0, true))
        page("tr-uyanma-oyunu", "Uyanma oyunu", dark = true) { WakeGameScreen() }
        page("tr-uyanma-raporu", "Uyanma oyunu") { WakeGameScreen("report") }
        page("tr-uyanma-puanlar", "Uyanma oyunu") { WakeGameScreen("points") }
        shot("tr-gunaydin-civciv") { Column(Modifier.padding(16.dp)) { WakeGameMorningCard() } }
        WakeGame.clear()
    }

    /** Kadran koleksiyonu (2. analiz #1): galeri ve kadranlar büyük */
    @Test fun dials() {
        Prefs.store.edit().putBoolean(DialFace.SEEN_KEY, false).commit()
        val t = java.time.ZonedDateTime.of(2026, 11, 3, 10, 9, 36, 0, java.time.ZoneId.of("Europe/Istanbul"))
        shot("tr-kadranlar") { DialGalleryScreen() }
        shot("tr-kadranlar-koyu", dark = true) { DialGalleryScreen() }
        listOf(DialFace.CHRONO to DialColor.AUTO, DialFace.RADAR to DialColor.AUTO, DialFace.MARBLE to DialColor.BLUE, DialFace.ROMAN to DialColor.GOLD)
            .forEach { (f, c) -> shot("kadran-${f.id}") { AnalogClock(t, Modifier.padding(24.dp), face = f, dialColor = c, tap = false) } }
    }

    @Test fun ownerScreens() {
        shot("tr-destek") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { FontScaleCard(); SupportCard {} }
        }
        shot("tr-qr-paylas") { ShareQrScreen() }
        shot("tr-atmosfer", atmo = "lavender") { ColorScreen() }
        shot("tr-atmosfer-koyu", atmo = "ocean", dark = true) { SettingsScreen {} }
        shot("tr-yazi-boyutu-130", fontScale = 130) { SettingsScreen {} }
        shot("ar-atmosfer-gun-batimi", qualifiers = "ar", atmo = "sunset") { PrayerScreen() }
        page("tr-dil", "Dil") { LanguageScreen() }
        shot("tr-kurulum") { OnboardingScreen {} }
        Onboarding.finish()
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
        widget("widget-ay-takvimi", 340, 170, MoonCalWidget().views(c, 6))
        widget("widget-hicri", 170, 170, HijriWidget().views(c, 7))
        widget("widget-saniyeli", 170, 170, SecondsClockWidget().views(c, 7))
        widget("widget-dunya", 360, 170, WorldClockWidget().views(c, 8))
        widget("widget-geri-sayim", 170, 170, CountdownWidget().views(c, 9))
        // Görünüm (2. analiz #17) ve iki saat (#12)
        Prefs.store.edit().putString("widget.style.12", "ocean").putBoolean("widget.frame.12", true)
            .putString("widget.style.13", "paper").putString("widget.city.13", "tokyo").commit()
        widget("widget-dijital-okyanus", 170, 80, DigitalClockWidget().views(c, 12))
        widget("widget-iki-saat", 260, 170, DualClockWidget().views(c, 13))
        // Şeffaf ve yarı saydam (2. analiz #76): gri zemin ana ekran duvar kâğıdı yerine
        Prefs.store.edit().putString("widget.style.14", "clear").putString("widget.style.15", "glass").putBoolean("widget.frame.15", true).commit()
        widget("widget-analog-seffaf", 170, 170, AnalogClockWidget().views(c, 14))
        widget("widget-saat-tarih-yari-saydam", 170, 170, ClockDateWidget().views(c, 15))
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

    /** 2. analiz K-4: yıl ilerlemesi, yazıyla tarih, gün doğumu, günün içeriği (dokununca tür değişir), takvim, hava,
     *  fotoğraf; Material You görünümü ve vakit widget'larındaki ilerleme çubuğu */
    @Test fun widgetsK4() {
        RuntimeEnvironment.setQualifiers("+tr")
        Prefs.saveProvince("istanbul")
        val c = RuntimeEnvironment.getApplication()
        widget("widget-yil", 170, 170, YearWidget().views(c, 31))
        widget("widget-yaziyla", 170, 170, WordsWidget().views(c, 32))
        widget("widget-gunes", 170, 170, SunWidget().views(c, 33))
        widget("widget-gunes-yayi", 320, 150, SunArcWidget().views(c, 41))
        widget("widget-gunun-sozu", 260, 170, DailyWidget().views(c, 34))
        Prefs.store.edit().putString(DailyWidget.key(35), "esma").putString(DailyWidget.key(36), "verse").commit()
        widget("widget-esma", 260, 170, DailyWidget().views(c, 35))
        widget("widget-ayet", 260, 170, DailyWidget().views(c, 36))
        widget("widget-takvim", 260, 170, EventsWidget().views(c, 37))
        widget("widget-hava", 170, 170, WeatherWidget().views(c, 38))
        widget("widget-fotograf", 170, 170, PhotoWidget().views(c, 39))
        Prefs.store.edit().putString("widget.style.40", "you").commit()
        widget("widget-saat-tarih-you", 170, 170, ClockDateWidget().views(c, 40))
        // Dokununca sıradaki tür
        DailyWidget().onReceive(c, android.content.Intent(c, DailyWidget::class.java).setAction(DailyWidget.ACTION_NEXT)
            .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, 34))
        assertEquals("verse", DailyWidget.kind(34))
        // Yazıyla sayı (ICU)
        assertEquals("yirmi iki", WordsWidget.spell(22).lowercase())
    }
}
