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
    @Test fun worldDark() = shot("tr-dunya-koyu", Tab.WORLD, dark = true)
    @Test fun seasons() = shot("tr-mevsimler", Tab.SEASONS)
    @Test fun prayer() = shot("tr-vakitler", Tab.PRAYER)
    @Test fun tools() = shot("tr-araclar", Tab.TOOLS)
    @Test fun english() = shot("en-saat", Tab.CLOCK, "en")
    @Test fun arabic() = shot("ar-vakitler", Tab.PRAYER, "ar")
    @Test fun kidsColor() = shot("tr-saat-renk", Tab.CLOCK, accent = "kid-purple")
}
