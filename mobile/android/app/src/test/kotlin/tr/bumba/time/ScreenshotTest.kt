package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Sekmelerin ekran görüntüleri (emülatörsüz): her sekme Türkçe açık, birkaç tanesi koyu, İngilizce ve Arapça.
 * Çıktı: app/build/screens/<ad>.png. Görsel karşılaştırma yapılmaz; görüntüler CI'da incelemek için saklanır.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class ScreenshotTest {
    private val dir = System.getProperty("screens.dir") ?: "build/screens"

    private fun shot(
        name: String, tab: Tab, qualifiers: String = "tr", dark: Boolean = false, accent: String = "amber",
        model: Int = 0, items: String = HomeItem.DEFAULT_RAW, weather: Boolean = false, features: Set<Tab> = Features.OPTIONAL.toSet(),
    ) {
        RuntimeEnvironment.setQualifiers("+$qualifiers")
        Prefs.store.edit().putBoolean("notify.asked", true).commit()
        // Sekme görüntüleri bütün özellikler açıkken (yeni kurulumda yalnız Saat açıktır; firstLaunch görüntüsü)
        Features.save(features)
        Prefs.saveAppearance(if (dark) "dark" else "light")
        Prefs.saveAccent(accent)
        Prefs.saveClockModel(model)
        Prefs.saveUnits("metric")
        HomeItems.raw = items
        weatherOverride = if (weather) sampleWeather() else null
        captureRoboImage("$dir/$name.png") {
            AppTheme { Root(tab.ordinal) }
        }
        weatherOverride = null
        HomeItems.raw = HomeItem.DEFAULT_RAW
    }

    /** Ağ olmadığı için örnek hava verisi (İstanbul, parçalı bulutlu) */
    private fun sampleWeather(): WeatherData {
        val zone = java.time.ZoneId.of("Europe/Istanbul")
        val now = java.time.Instant.now()
        val today = java.time.LocalDate.now(zone)
        return WeatherData(
            zone,
            WeatherData.Current(19.4, 18.0, 64.0, 14.0, 40.0, 1016.0, 3.0, 2, true, 0.0),
            (0 until 48).map { WeatherData.Hour(now.plusSeconds(it * 3600L), 18.0 + it % 5, listOf(1, 2, 3, 61)[it % 4], it % 4 * 10, true) },
            (0 until 7).map { WeatherData.Day(today.plusDays(it.toLong()), listOf(2, 0, 61, 3, 1, 80, 0)[it], 22.0 + it % 3, 13.0 + it % 2, 20, null, null, 4.0) },
        )
    }

    private fun capture(name: String, qualifiers: String = "tr", content: @androidx.compose.runtime.Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers("+$qualifiers")
        Prefs.store.edit().putBoolean("notify.asked", true).commit()
        Prefs.saveAppearance("light")
        Prefs.saveAccent("amber")
        captureRoboImage("$dir/$name.png") {
            AppTheme { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.background(Theme.c.bg)) { content() } }
        }
    }

    @Test fun clock() = shot("tr-saat", Tab.CLOCK)
    @Test fun clockDark() = shot("tr-saat-koyu", Tab.CLOCK, dark = true)
    @Test fun calendar() = shot("tr-takvim", Tab.CALENDAR)
    @Test fun world() = shot("tr-dunya", Tab.WORLD)
    @Test fun worldDark() = shot("tr-dunya-koyu", Tab.WORLD, dark = true)
    @Test fun seasons() = shot("tr-mevsimler", Tab.SEASONS)
    @Test fun prayer() = shot("tr-vakitler", Tab.PRAYER)
    @Test fun tools() = shot("tr-araclar", Tab.TOOLS)
    @Test fun english() = shot("en-saat", Tab.CLOCK, "en")
    @Test fun arabic() = shot("ar-vakitler", Tab.PRAYER, "ar")
    // İlk dalga yeni diller (31 Ekim 2026): Endonezce (Android kaynak kodu "in"), Urduca ve Farsça (sağdan sola), Fransızca, Rusça
    @Test fun indonesian() = shot("id-vakitler", Tab.PRAYER, "in")
    @Test fun urdu() = shot("ur-saat", Tab.CLOCK, "ur")
    @Test fun persian() = shot("fa-vakitler", Tab.PRAYER, "fa")
    @Test fun french() = shot("fr-araclar", Tab.TOOLS, "fr")
    @Test fun russian() = shot("ru-takvim", Tab.CALENDAR, "ru")
    // Araçlar alt sayfaları
    private fun tool(name: String, route: String) {
        RuntimeEnvironment.setQualifiers("+tr")
        Prefs.saveAppearance("light")
        Prefs.saveAccent("amber")
        captureRoboImage("$dir/$name.png") {
            AppTheme {
                Box(Modifier.background(Theme.c.bg)) { ToolsScreen(route) {} }
            }
        }
    }

    @Test fun alarm() {
        AlarmItem.save(listOf(AlarmItem(id = "a", hour = 7, minute = 0, days = AlarmItem.WEEKDAYS, snooze = 10),
            AlarmItem(id = "b", hour = 9, minute = 30, label = "Spor", enabled = false)))
        tool("tr-alarm", "alarm")
    }
    @Test fun stopwatches() {
        // Ana kronometre durmuş, iki ek kronometre: biri sayıyor (Fırın), biri durmuş
        val now = System.currentTimeMillis()
        Prefs.store.edit().putLong("sw.start", 0).putLong("sw.acc", 95_400).putString("sw.laps", "41000,95400").commit()
        StopwatchItem.save(listOf(StopwatchItem(id = "a", name = "Fırın", start = now - 754_000), StopwatchItem(id = "b", acc = 62_250)))
        tool("tr-kronometreler", "stopwatch")
        StopwatchItem.save(emptyList())
        Prefs.store.edit().remove("sw.acc").remove("sw.laps").commit()
    }
    @Test fun timer() {
        TimerHistory.clear()
        TimerHistory.record("", 600_000)
        TimerHistory.record("Çay", 300_000)
        // İki zamanlayıcı: biri çalışıyor (Çay), biri duraklatılmış
        val now = System.currentTimeMillis()
        TimerItem.save(listOf(TimerItem(id = "a", name = "Çay", total = 300_000, end = now + 185_000),
            TimerItem(id = "b", name = "", total = 900_000, left = 420_000)))
        tool("tr-zamanlayici", "timer")
    }
    @Test fun hiit() {
        RuntimeEnvironment.setQualifiers("+tr")
        Prefs.saveAppearance("dark")
        Prefs.saveAccent("amber")
        // 50. saniyede: ısınma 10 sn + çalışma 30 sn bitti, ilk dinlenmenin 10. saniyesi
        Prefs.store.edit().putString("hiit.cfg", "10,30,15,8").putLong("hiit.start", System.currentTimeMillis() - 50_000)
            .putLong("hiit.paused", System.currentTimeMillis()).commit()
        captureRoboImage("$dir/tr-hiit.png") { AppTheme { Box(Modifier.background(Theme.c.bg)) { HiitScreen() } } }
        Prefs.store.edit().remove("hiit.start").remove("hiit.paused").commit()
        Prefs.saveAppearance("light")
    }
    @Test fun routine() {
        RuntimeEnvironment.setQualifiers("+tr")
        // 7 dakika antrenmanı, 75. saniye: üçüncü hareket (şınav)
        Routines.select(Routines.templates().first { it.first == "seven" }.second)
        Prefs.store.edit().putLong("hiit.start", System.currentTimeMillis() - 85_000).putLong("hiit.paused", System.currentTimeMillis()).commit()
        captureRoboImage("$dir/tr-rutin.png") { AppTheme { Box(Modifier.background(Theme.c.bg)) { HiitScreen() } } }
        Prefs.store.edit().remove("hiit.start").remove("hiit.paused").commit()
        Routines.select(null)
        captureRoboImage("$dir/tr-rutinler.png") { AppTheme { Box(Modifier.background(Theme.c.bg)) { RoutinePicker(onPick = {}) } } }
    }
    @Test fun pomodoro() = tool("tr-pomodoro", "pomodoro")
    /** Pomodoro istatistikleri (son 7 gün, görevler) ve arka plan sesi seçimi */
    @Test fun pomodoroStats() {
        val today = java.time.LocalDate.now()
        val log = listOf(6 to 2, 5 to 4, 3 to 1, 2 to 6, 1 to 3, 0 to 2).flatMap { (ago, n) ->
            (0 until n).map { i -> tr.bumba.time.core.FocusLog.Entry(today.minusDays(ago.toLong()).toString(), 25, listOf("Rapor", "Kod", "")[i % 3]) }
        }
        capture("tr-pomodoro-istatistik") {
            androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                AppCard {
                    SectionTitle(L("tools.pomodoro.noise"))
                    NoiseControls("rain", 40, onKind = {}, onVolume = {})
                }
                FocusStatsCard(log) {}
            }
        }
    }
    @Test fun dateCalc() = tool("tr-tarih-hesaplama", "datecalc")
    /** Vakit bildirimleri ayarı (açık hâlde: vakitler, zaman, ses) */
    @Test fun notifySettings() {
        RuntimeEnvironment.setQualifiers("+tr")
        Prefs.saveAppearance("light")
        Prefs.saveAccent("amber")
        NotifyPrefs.saveEnabled(true)
        captureRoboImage("$dir/tr-bildirim-ayarlari.png") { AppTheme { NotificationSettingsScreen {} } }
        NotifyPrefs.saveEnabled(false)
    }

    /** Sıradaki Ramazan'ın imsakiyesi (İstanbul): sayfa ve paylaşılan görselin kendisi */
    @Test fun imsakiye() {
        RuntimeEnvironment.setQualifiers("+tr")
        Prefs.saveAppearance("light")
        Prefs.saveProvince("istanbul")
        val place = AppPlace.current()
        val ramadan = Imsakiye.nextRamadan(java.time.LocalDate.of(2026, 10, 18)) ?: return
        captureRoboImage("$dir/tr-imsakiye.png") { AppTheme { ImsakiyeScreen(place, ramadan) {} } }
        java.io.File("$dir/tr-imsakiye-gorsel.png").outputStream().use {
            Imsakiye.draw(place, ramadan).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun kidsColor() = shot("tr-saat-renk", Tab.CLOCK, accent = "kid-purple")
    @Test fun clockWeather() = shot("tr-saat-hava", Tab.CLOCK, weather = true)
    @Test fun clockClassic() = shot("tr-saat-analog", Tab.CLOCK, model = 1)
    @Test fun clockHorizontal() = shot("tr-saat-yatay", Tab.CLOCK, model = 14, weather = true, dark = true)
    @Test fun clockWxCard() = shot("tr-saat-hava-kart", Tab.CLOCK, model = 5, weather = true)
    @Test fun clockFlip() = shot("tr-saat-cevirmeli", Tab.CLOCK, model = ClockModel.FLIP.ordinal, dark = true)
    @Test fun clockAllItems() = shot("tr-saat-alanlar", Tab.CLOCK, model = 18, weather = true,
        items = "date,week,hijri,next,prayers,upcoming,world,weather,sun,-globe")
    @Test fun clockArabic() = shot("ar-saat", Tab.CLOCK, "ar", weather = true)
    // Yan çevrilmiş: saat sekmesi tam ekran açılır; diğer sekmelerde alt bar ve sistem çubukları gizli
    @Test fun landscapeClock() = shot("tr-yatay-saat", Tab.CLOCK, "tr-land")
    @Test fun landscapeWeather() = shot("tr-yatay-hava", Tab.WEATHER, "tr-land", weather = true)
    @Test fun landscapePrayer() = shot("tr-yatay-vakitler", Tab.PRAYER, "tr-land", dark = true)
    // İlk açılış: yalnız Saat (alt bar yok; dişli ve özellikleri açma satırı); bir özellik açıkken iki sekmeli bar
    @Test fun firstLaunch() = shot("tr-ilk-acilis", Tab.CLOCK, features = emptySet())
    @Test fun twoFeatures() = shot("tr-iki-ozellik", Tab.PRAYER, features = setOf(Tab.PRAYER, Tab.WEATHER))
    @Test fun featuresPage() = tool("tr-ozellikler", "features")
    @Test fun homeItemsEditor() = capture("tr-saat-duzenle") { HomeItemsEditor() }
    @Test fun fullscreen() = capture("tr-saat-tam-ekran") { FullscreenClock {} }
}
