package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/** Gece modu: açık/kapalı/otomatik (Güneş ufkun altında), kayma sınırları */
class NightModeTest {
    private fun ms(s: String) = ZonedDateTime.parse(s).toInstant().toEpochMilli()

    @Test fun active() {
        val (lat, lon) = 41.01 to 28.98 // İstanbul
        assertFalse(NightMode.active("off", lat, lon, ms("2026-10-30T23:00:00+03:00")))
        assertTrue(NightMode.active("on", lat, lon, ms("2026-10-30T12:00:00+03:00")))
        assertTrue(NightMode.active("auto", lat, lon, ms("2026-10-30T23:00:00+03:00")))
        assertFalse(NightMode.active("auto", lat, lon, ms("2026-10-30T12:00:00+03:00")))
        // 30 Ekim 2026 İstanbul gün batımı ~18.10: 18.00 gündüz, 18.20 gece
        assertFalse(NightMode.active("auto", lat, lon, ms("2026-10-30T18:00:00+03:00")))
        assertTrue(NightMode.active("auto", lat, lon, ms("2026-10-30T18:20:00+03:00")))
    }

    @Test fun shiftAndToggle() {
        val start = ms("2026-10-30T23:00:00+03:00")
        val seen = (0 until 200).map { NightMode.shift(start + it * 60_000L) }
        assertTrue(seen.all { (x, y) -> x in -6..6 && y in -6..6 })
        assertTrue(seen.toSet().size > 10)
        assertEquals(NightMode.shift(start), NightMode.shift(start + 59_000))
        assertEquals("off", NightMode.toggled(true))
        assertEquals("on", NightMode.toggled(false))
    }
}
