package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Tam ekran hava durumlu modelle açılmaz (16 Kasım 2026, sahibin isteği); kendi seçimi saat ekranını değiştirmez */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FullscreenModelTest {
    @Test fun startsWithoutWeather() {
        Prefs.init(RuntimeEnvironment.getApplication())
        Prefs.store.edit().remove(ClockModel.FULLSCREEN_KEY).commit()
        Prefs.saveClockModel(ClockModel.WX_DIGITAL_CAL_WEEK.ordinal)
        assertEquals(ClockModel.CLASSIC.ordinal, ClockModel.fullscreenStart())
        Prefs.saveClockModel(ClockModel.FLIP.ordinal)
        assertEquals(ClockModel.FLIP.ordinal, ClockModel.fullscreenStart())
        // Tam ekranda seçilen model (hava durumlu da olsa) korunur, saat ekranının seçimi değişmez
        ClockModel.saveFullscreen(ClockModel.WX_FULL.ordinal)
        assertEquals(ClockModel.WX_FULL.ordinal, ClockModel.fullscreenStart())
        assertEquals(ClockModel.FLIP.ordinal, Prefs.clockModel)
        Prefs.store.edit().remove(ClockModel.FULLSCREEN_KEY).commit()
        Prefs.saveClockModel(ClockModel.CLASSIC.ordinal)
    }
}
