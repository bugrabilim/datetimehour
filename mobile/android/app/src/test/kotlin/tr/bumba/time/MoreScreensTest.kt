package tr.bumba.time

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import org.json.JSONObject
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

    /** İstanbul için örnek hava durumu (Open-Meteo yanıtı biçiminde) */
    private fun seedWeather() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val hours = (0 until 48).map { now.plusHours(it.toLong()) }
        val days = (0 until 7).map { LocalDate.now().plusDays(it.toLong()) }
        val codes = listOf(0, 1, 2, 3, 61, 80, 95)
        val o = JSONObject()
            .put("timezone", "Europe/Istanbul")
            .put("current", JSONObject().put("temperature_2m", 18.4).put("apparent_temperature", 17.2)
                .put("relative_humidity_2m", 64).put("wind_speed_10m", 14.0).put("wind_direction_10m", 40)
                .put("pressure_msl", 1016.0).put("uv_index", 3.1).put("weather_code", 2).put("is_day", 1).put("precipitation", 0))
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
    }
}
