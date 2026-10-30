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
