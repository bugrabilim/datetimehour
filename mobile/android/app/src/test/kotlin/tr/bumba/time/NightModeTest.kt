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

    /** Belirli saatler (2. analiz #11) ve ortam ışığı (#189) */
    @Test fun scheduleAndLight() {
        // 22.00 → 07.00 gece yarısını aşar
        assertTrue(NightMode.inSchedule(23 * 60, 22 * 60, 7 * 60))
        assertTrue(NightMode.inSchedule(3 * 60, 22 * 60, 7 * 60))
        assertFalse(NightMode.inSchedule(7 * 60, 22 * 60, 7 * 60))
        assertFalse(NightMode.inSchedule(12 * 60, 22 * 60, 7 * 60))
        // Aynı gün içinde: 13.00 → 15.00
        assertTrue(NightMode.inSchedule(14 * 60, 13 * 60, 15 * 60))
        assertFalse(NightMode.inSchedule(16 * 60, 13 * 60, 15 * 60))
        // Başlangıç = bitiş: hiç
        assertFalse(NightMode.inSchedule(10 * 60, 10 * 60, 10 * 60))
        // Ortam ışığı: yalnız "light" modunda sensöre bakılır
        assertTrue(NightMode.active("light", 41.0, 29.0, ms("2026-10-30T12:00:00+03:00"), dark = true))
        assertFalse(NightMode.active("light", 41.0, 29.0, ms("2026-10-30T23:00:00+03:00"), dark = false))
        assertFalse(NightMode.active("off", 41.0, 29.0, ms("2026-10-30T23:00:00+03:00"), dark = true))
        assertEquals(7, NightMode.COLORS.size)
        assertEquals(listOf("off", "on", "auto", "schedule", "light"), NightMode.choices(true))
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
