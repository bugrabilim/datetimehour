package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Takvim varsayılanları (9 Kasım 2026, Müslüman olmayanlar raporu A4): dini günler yalnız Vakitler açıkken, seçim yazılınca kalıcı */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CalendarDefaultsTest {
    private fun religiousDay(): LocalDate =
        AppData.events.entries.filter { e -> e.value.any { it.type == "religious" && it.date == e.key } }.minByOrNull { it.key }!!.key

    @Test fun religiousDaysFollowPrayerFeature() {
        CalView.load()
        CalView.clearChoices()
        val d = religiousDay()
        Features.save(emptySet())
        assertFalse(CalView.religious)
        assertTrue(calEvents(d).none { it.type == "religious" })
        Features.save(setOf(Tab.PRAYER))
        assertTrue(CalView.religious)
        assertTrue(calEvents(d).any { it.type == "religious" })
        // Kullanıcı türleri seçince seçim kalıcıdır, Vakitler'e bağlı değildir
        CalView.hidden = emptySet(); CalView.save()
        Features.save(emptySet())
        assertTrue(CalView.religious)
        assertEquals("", Prefs.store.getString("cal.hidden", null))
        CalView.clearChoices()
    }

    @Test fun countryOnlySavedWhenChosen() {
        CalView.load()
        CalView.clearChoices()
        CalView.mode = "week"; CalView.save()
        assertFalse(Prefs.store.contains("cal.country"))
        assertFalse(Prefs.store.contains("cal.hidden"))
        CalView.country = "DE"; CalView.save()
        assertEquals("DE", Prefs.store.getString("cal.country", null))
        CalView.clearChoices()
        CalView.mode = "month"; CalView.save()
    }
}
