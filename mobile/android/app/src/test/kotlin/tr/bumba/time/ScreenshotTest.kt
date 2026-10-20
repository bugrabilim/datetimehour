package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
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
        model: Int = 0, items: String = HomeItem.DEFAULT_RAW, weather: Boolean = false,
    ) {
        RuntimeEnvironment.setQualifiers("+$qualifiers")
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
        AlarmItem.save(listOf(AlarmItem(id = "a", hour = 7, minute = 0, repeats = "weekdays"),
            AlarmItem(id = "b", hour = 9, minute = 30, label = "Spor", enabled = false)))
        tool("tr-alarm", "alarm")
    }
    @Test fun timer() = tool("tr-zamanlayici", "timer")
    @Test fun pomodoro() = tool("tr-pomodoro", "pomodoro")
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
    @Test fun clockAllItems() = shot("tr-saat-alanlar", Tab.CLOCK, model = 18, weather = true,
        items = "date,week,next,prayers,upcoming,world,weather,sun,-globe")
    @Test fun clockArabic() = shot("ar-saat", Tab.CLOCK, "ar", weather = true)
    @Test fun homeItemsEditor() = capture("tr-saat-duzenle") { HomeItemsEditor() }
    @Test fun fullscreen() = capture("tr-saat-tam-ekran") { FullscreenClock {} }
}
