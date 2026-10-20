package tr.bumba.time

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

    private fun shot(name: String, tab: Tab, qualifiers: String = "tr", dark: Boolean = false, accent: String = "amber") {
        RuntimeEnvironment.setQualifiers("+$qualifiers")
        Prefs.saveAppearance(if (dark) "dark" else "light")
        Prefs.saveAccent(accent)
        captureRoboImage("$dir/$name.png") {
            AppTheme { Root(tab.ordinal) }
        }
    }

    @Test fun clock() = shot("tr-saat", Tab.CLOCK)
    @Test fun clockDark() = shot("tr-saat-koyu", Tab.CLOCK, dark = true)
    @Test fun calendar() = shot("tr-takvim", Tab.CALENDAR)
    @Test fun world() = shot("tr-dunya", Tab.WORLD)
    @Test fun seasons() = shot("tr-mevsimler", Tab.SEASONS)
    @Test fun prayer() = shot("tr-vakitler", Tab.PRAYER)
    @Test fun tools() = shot("tr-araclar", Tab.TOOLS)
    @Test fun english() = shot("en-saat", Tab.CLOCK, "en")
    @Test fun arabic() = shot("ar-vakitler", Tab.PRAYER, "ar")
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
}
