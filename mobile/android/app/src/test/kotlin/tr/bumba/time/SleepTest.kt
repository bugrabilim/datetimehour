package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/** Uyku planı (#17): yatma hatırlatmaları ve gün doğumu ışığı seçili alarm */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SleepTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    @Test fun bedtimeReminders() {
        // Kapalıyken hatırlatma yok
        SleepPlan.on = false
        assertEquals(0, SleepPlan.reminders(Instant.parse("2026-10-30T10:00:00Z"), 7, zone).size)
        // Hafta içi 07.00 kalkış, 8 saat uyku, 30 dk önce: önceki akşam 22.30
        SleepPlan.on = true
        SleepPlan.wake = 7 * 60
        SleepPlan.duration = 8 * 60
        SleepPlan.days = listOf(1, 2, 3, 4, 5)
        SleepPlan.remind = 30
        val now = Instant.parse("2026-10-30T10:00:00Z") // cuma 13.00
        val r = SleepPlan.reminders(now, 7, zone)
        // Cumartesi ve pazar kalkış yok: pazar akşamı (pazartesi kalkışı için) ilk hatırlatma
        assertEquals("2026-11-01T22:30+03:00[Europe/Istanbul]", r.first().at.atZone(zone).toString())
        assertEquals(5, r.size)
        assertTrue(r.all { it.id.startsWith("bed-") && it.tab == Tab.TOOLS })
        // Cuma kalkışı: perşembe 22.30 hatırlatması geçmiş, sayılmaz; yatma saatinde hatırlatma
        SleepPlan.remind = 0
        assertEquals("2026-11-01T23:00+03:00[Europe/Istanbul]", SleepPlan.reminders(now, 7, zone).first().at.atZone(zone).toString())
        SleepPlan.on = false
        listOf(SleepPlan.WAKE, SleepPlan.DURATION, SleepPlan.DAYS, SleepPlan.REMIND).forEach { Prefs.store.edit().remove(it).commit() }
    }

    @Test fun sunriseAlarm() {
        AlarmItem.save(listOf(AlarmItem(id = "a", hour = 7, minute = 0), AlarmItem(id = "b", hour = 6, minute = 30, sunrise = 20, enabled = false)))
        assertNull(AlarmItem.nextSunrise())
        val item = AlarmItem(id = "c", hour = 6, minute = 45, sunrise = 20)
        assertEquals(20, AlarmItem.fromJson(item.toJson()).sunrise)
        AlarmItem.save(listOf(item))
        val (at, window) = AlarmItem.nextSunrise()!!
        assertEquals(20, window)
        val t = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
        assertEquals(6, t.hour)
        assertEquals(45, t.minute)
        AlarmItem.save(emptyList())
    }
}
