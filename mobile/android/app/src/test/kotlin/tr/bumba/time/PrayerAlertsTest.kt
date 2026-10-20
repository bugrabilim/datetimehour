package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.bumba.time.core.Prayer
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Bildirim planı: iOS'taki PrayerAlertsTests ile aynı kurallar (sıralı, gelecekte, seçili vakitler, öne alma, sınır) */
class PrayerAlertsTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val place = AppPlace("istanbul", "İstanbul", 41.0082, 28.9784, zone)
    private val now: Instant = ZonedDateTime.of(2026, 10, 9, 0, 0, 0, 0, zone).toInstant()

    @Test fun disabledGivesNothing() {
        assertTrue(PrayerAlerts.plan(place, PrayerAlerts.Settings(enabled = false), now).isEmpty())
    }

    @Test fun defaultFivePrayersSortedInFuture() {
        val plan = PrayerAlerts.plan(place, PrayerAlerts.Settings(enabled = true), now)
        assertEquals(63, plan.size)
        assertTrue(plan.none { it.prayer == Prayer.SUNRISE })
        assertTrue(plan.all { it.fireAt.isAfter(now) && it.fireAt == it.prayerAt })
        assertEquals(plan.sortedBy { it.fireAt }, plan)
        assertEquals(Prayer.FAJR, plan.first().prayer)
        // İlk gün beş vakit sırayla
        assertEquals(listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA), plan.take(5).map { it.prayer })
    }

    @Test fun leadMovesFireTimeAndSkipsPast() {
        val dhuhr = place.entries(now.atZone(zone).toLocalDate()).first { it.prayer == Prayer.DHUHR }.at
        val at = dhuhr.minusSeconds(5 * 60) // öğleye 5 dk kala: 10 dk öncesi geçmiş, sıradaki ikindi
        val plan = PrayerAlerts.plan(place, PrayerAlerts.Settings(true, setOf(Prayer.DHUHR, Prayer.ASR), 10), at, limit = 3)
        assertEquals(3, plan.size)
        assertEquals(Prayer.ASR, plan.first().prayer)
        assertTrue(plan.all { it.prayerAt.epochSecond - it.fireAt.epochSecond == 600L })
    }

    @Test fun preferencesRoundTrip() {
        val set = setOf(Prayer.ISHA, Prayer.FAJR)
        assertEquals("fajr,isha", NotifyPrefs.encode(set))
        assertEquals(set, NotifyPrefs.decode("isha,fajr,bogus"))
    }
}
