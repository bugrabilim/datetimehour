package tr.bumba.time

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId

/** Alarm günleri, atlama ve eski kayıtların okunması */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmItemTest {
    private val zone = ZoneId.systemDefault()
    private fun ms(s: String) = LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()

    @Test fun legacyRepeats() {
        fun legacy(r: String) = AlarmItem.fromJson(JSONObject().put("id", "x").put("hour", 7).put("minute", 0).put("repeats", r))
        assertEquals(emptyList<Int>(), legacy("once").days)
        assertEquals((1..7).toList(), legacy("every").days)
        assertEquals(listOf(1, 2, 3, 4, 5), legacy("weekdays").days)
        assertEquals(5, legacy("once").snooze)
        val round = AlarmItem.fromJson(AlarmItem(id = "y", hour = 6, minute = 30, days = listOf(2, 4), snooze = 15, vibrate = false).toJson())
        assertEquals(listOf(2, 4), round.days)
        assertEquals(15, round.snooze)
        assertEquals(false, round.vibrate)
    }

    /** Yaklaşan alarm bildirimi ve sesli saat (rakip analizi #19, #20): yeni alarmda 30 dk, eski kayıtta kapalı */
    @Test fun preNotifyAndSpeak() {
        val old = AlarmItem.fromJson(JSONObject().put("id", "x").put("hour", 7).put("minute", 0))
        assertEquals(0, old.preNotify)
        assertEquals(false, old.speak)
        assertEquals(30, AlarmItem(hour = 7, minute = 0).preNotify)
        val round = AlarmItem.fromJson(AlarmItem(id = "y", hour = 6, minute = 0, preNotify = 15, speak = true).toJson())
        assertEquals(15, round.preNotify)
        assertEquals(true, round.speak)
    }

    @Test fun skipHolidays() {
        val old = AlarmItem.isHoliday
        try {
            // 28 Ekim 2026 çarşamba 08.00; hafta içi 07.00 alarmı. 29 Ekim perşembe tatil (Cumhuriyet Bayramı)
            AlarmItem.isHoliday = { it == java.time.LocalDate.of(2026, 10, 29) }
            val now = ms("2026-10-28T08:00")
            val a = AlarmItem(hour = 7, minute = 0, days = AlarmItem.WEEKDAYS)
            assertEquals(ms("2026-10-29T07:00"), a.nextAt(now))
            val h = a.copy(skipHolidays = true)
            assertEquals(ms("2026-10-30T07:00"), h.nextAt(now))
            assertEquals(true, h.holidaySkipped(now))
            assertEquals(false, a.holidaySkipped(now))
            // Elle atlama tatilden sonraki çalmayı atlar → 2 Kasım pazartesi
            assertEquals(ms("2026-11-02T07:00"), h.copy(skipUntil = h.nextAt(now) + 60_000).nextAt(now))
            // Bir kezlik alarmda etkisiz
            assertEquals(ms("2026-10-29T07:00"), AlarmItem(hour = 7, minute = 0, skipHolidays = true).nextAt(now))
            // Bütün günler tatilse ilk çalma kalır (sonsuz döngü yok)
            AlarmItem.isHoliday = { true }
            assertEquals(ms("2026-10-29T07:00"), h.nextAt(now))
            // JSON gidiş-dönüş
            assertEquals(true, AlarmItem.fromJson(h.toJson()).skipHolidays)
            assertEquals(false, AlarmItem.fromJson(JSONObject().put("id", "x").put("hour", 7).put("minute", 0)).skipHolidays)
        } finally { AlarmItem.isHoliday = old }
    }

    @Test fun realHolidayData() {
        // Takvim verisiyle: 2027 Ramazan Bayramı (9–11 Mart 2027, arife 8 Mart yarım gün) hafta içi alarmı
        AppData.init(org.robolectric.RuntimeEnvironment.getApplication())
        val h = AlarmItem(hour = 7, minute = 0, days = AlarmItem.WEEKDAYS, skipHolidays = true)
        val now = ms("2027-03-07T12:00")
        // Arife (8 Mart pazartesi, yarım gün) çalar
        assertEquals(ms("2027-03-08T07:00"), h.nextAt(now))
        // 9–11 Mart tatil → 12 Mart cuma
        assertEquals(ms("2027-03-12T07:00"), h.nextAt(ms("2027-03-08T08:00")))
    }

    @Test fun daysAndSkip() {
        // 30 Ekim 2026 cuma 08.00; hafta içi 07.00 alarmı → 2 Kasım pazartesi
        val now = ms("2026-10-30T08:00")
        val a = AlarmItem(hour = 7, minute = 0, days = AlarmItem.WEEKDAYS)
        assertEquals(ms("2026-11-02T07:00"), a.nextAt(now))
        // Sonraki atlanır → salı
        val skipped = a.copy(skipUntil = a.occurrenceAfter(now) + 60_000)
        assertEquals(ms("2026-11-03T07:00"), skipped.nextAt(now))
        // Bir kezlik: aynı gün geçtiyse ertesi gün
        assertEquals(ms("2026-10-31T07:00"), AlarmItem(hour = 7, minute = 0).nextAt(now))
        // Son çalma (günaydın kartı)
        assertEquals(ms("2026-10-30T07:00"), a.occurrenceBefore(now))
        assertEquals(L("tools.alarm.weekdays"), a.repeatText)
        assertEquals(L("alarm.everyDay"), a.copy(days = (1..7).toList()).repeatText)
    }
}

/** Kademeli ses eğrisi ve erteleme sınırı */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class AlarmRiseSnoozeTest {
    @Test fun volumeCurve() {
        assertEquals(AlarmRingService.MIN_VOLUME, AlarmRingService.volumeAt(0), 1e-6f)
        assertEquals(1f, AlarmRingService.volumeAt(AlarmRingService.RISE_MS), 1e-6f)
        assertEquals(1f, AlarmRingService.volumeAt(120_000), 1e-6f)
        // Yarı yolda dörtte bire yakın (kare eğri), hep artan
        val half = AlarmRingService.volumeAt(15_000)
        org.junit.Assert.assertTrue(half in 0.25f..0.3f)
        org.junit.Assert.assertTrue(AlarmRingService.volumeAt(10_000) < half && half < AlarmRingService.volumeAt(20_000))
    }

    @Test fun snoozeLimit() {
        val c = org.robolectric.RuntimeEnvironment.getApplication()
        val a = AlarmItem(id = "x", hour = 7, minute = 0, snooze = 5, snoozeMax = 2, rise = true)
        AlarmItem.save(listOf(a))
        assertEquals(a, AlarmItem.fromJson(a.toJson()))
        assertEquals(5, AlarmScheduler.snoozeMinutes("x", "alarm"))
        AlarmScheduler.snooze(c, "x", "t")
        assertEquals(5, AlarmScheduler.snoozeMinutes(AlarmScheduler.SNOOZE + "x", "alarm"))
        AlarmScheduler.snooze(c, AlarmScheduler.SNOOZE + "x", "t")
        // İki erteleme kullanıldı: üçüncü çalmada erteleme düğmesi yok
        assertEquals(0, AlarmScheduler.snoozeMinutes(AlarmScheduler.SNOOZE + "x", "alarm"))
        // Kapatınca sayaç sıfırlanır
        AlarmScheduler.stop(c, AlarmScheduler.SNOOZE + "x")
        assertEquals(5, AlarmScheduler.snoozeMinutes("x", "alarm"))
        // Sınırsız
        AlarmItem.save(listOf(a.copy(snoozeMax = 0)))
        repeat(6) { AlarmScheduler.snooze(c, AlarmScheduler.SNOOZE + "x", "t") }
        assertEquals(5, AlarmScheduler.snoozeMinutes(AlarmScheduler.SNOOZE + "x", "alarm"))
        AlarmItem.save(emptyList())
    }
}
