package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Geri sayımlar: yıllık tekrar, "Bugün!", eski tek hedefin taşınması, hatırlatmalar */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CountdownTest {
    private val zone = ZoneId.systemDefault()
    private fun ms(s: String) = LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()

    @Test fun yearlyAndToday() {
        val now = Instant.ofEpochMilli(ms("2026-10-30T12:00"))
        val bday = CountdownItem(title = "Doğum günü", emoji = "🎂", target = ms("2020-03-15T00:00"), yearly = true)
        assertEquals(ms("2027-03-15T00:00"), bday.effective(now).toEpochMilli())
        // Hedef günü boyunca bugünün hedefi kalır
        val todayItem = CountdownItem(title = "x", target = ms("2025-10-30T08:00"), yearly = true)
        assertEquals(ms("2026-10-30T08:00"), todayItem.effective(now).toEpochMilli())
        assertEquals(L("countdowns.today"), countdownText(todayItem.effective(now), now))
        // Tek seferlik geçmiş hedef ileri taşınmaz
        val once = CountdownItem(title = "y", target = ms("2026-10-01T00:00"))
        assertEquals(ms("2026-10-01T00:00"), once.effective(now).toEpochMilli())
        assertEquals(T("client.daysLeft", 136), countdownText(bday.effective(now), now))
    }

    @Test fun migrationAndReminders() {
        Prefs.store.edit().remove("countdowns").putLong("cd.target", ms("2026-11-05T10:00")).putString("cd.title", "Sınav").commit()
        val list = CountdownItem.load()
        assertEquals(1, list.size)
        assertEquals("Sınav", list[0].title)
        assertEquals(0L, Prefs.store.getLong("cd.target", 0L))
        val now = Instant.ofEpochMilli(ms("2026-10-30T12:00"))
        val r = CountdownItem.reminders(now)
        assertEquals(2, r.size)
        assertTrue(r.any { it.at.toEpochMilli() == ms("2026-11-04T09:00") })
        assertTrue(r.any { it.at.toEpochMilli() == ms("2026-11-05T10:00") })
        Prefs.store.edit().remove("countdowns").commit()
    }
}
