package tr.bumba.time

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Odakta engelleme (#98): yalnız açıkken, uygulama seçiliyken ve pomodoro çalışma süresi işlerken etkin */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FocusBlockTest {
    @Test fun activeOnlyDuringWork() {
        Prefs.init(ApplicationProvider.getApplicationContext())
        val now = 1_800_000_000_000L
        Prefs.store.edit().putString("pomo.phase", "work").putLong("pomo.end", now + 60_000).apply()
        assertFalse(FocusBlock.active(now)) // kapalı
        Prefs.store.edit().putBoolean(FocusBlock.ON, true).apply()
        assertFalse(FocusBlock.active(now)) // uygulama seçilmedi
        FocusBlock.saveApps(setOf("com.example.b", "com.example.a"))
        assertEquals(setOf("com.example.a", "com.example.b"), FocusBlock.apps())
        assertTrue(FocusBlock.active(now))
        assertFalse(FocusBlock.active(now + 61_000)) // süre bitti
        Prefs.store.edit().putString("pomo.phase", "short").apply()
        assertFalse(FocusBlock.active(now)) // mola
        Prefs.store.edit().clear().apply()
    }
}
