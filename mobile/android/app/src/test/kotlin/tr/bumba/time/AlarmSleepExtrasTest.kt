package tr.bumba.time

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId

/** 2. analiz #41–#44, #47, #50, #52: yeni alarm alanları, tatil modu, uyku kaydı, günlük etiketleri */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmSleepExtrasTest {
    private val zone = ZoneId.systemDefault()
    private fun ms(s: String) = LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()

    @After fun clean() {
        Prefs.store.edit().remove(AlarmPause.FROM).remove(AlarmPause.TO).remove("sleep.log").remove("sleep.bedAt").commit()
    }

    @Test fun newFieldsRoundTrip() {
        val a = AlarmItem(hour = 7, minute = 0, shakeSnooze = true, preAlarm = 20, openApp = "com.example", sleepPlan = true)
        val b = AlarmItem.fromJson(a.toJson())
        assertEquals(a.shakeSnooze, b.shakeSnooze); assertEquals(20, b.preAlarm)
        assertEquals("com.example", b.openApp); assertTrue(b.sleepPlan)
    }

    @Test fun vacationPause() {
        Prefs.store.edit().putString(AlarmPause.FROM, "2026-11-09").putString(AlarmPause.TO, "2026-11-13").commit()
        val weekdays = AlarmItem(hour = 7, minute = 0, days = listOf(1, 2, 3, 4, 5))
        // Cuma 6 Kasım akşamından sonra: 9–13 Kasım duraklatıldı → 16 Kasım pazartesi
        assertEquals(ms("2026-11-16T07:00"), weekdays.nextAt(ms("2026-11-06T20:00")))
        // Bir kezlik alarm etkilenmez
        val once = AlarmItem(hour = 7, minute = 0, date = "2026-11-10")
        assertEquals(ms("2026-11-10T07:00"), once.nextAt(ms("2026-11-06T20:00")))
    }

    @Test fun sleepLog() {
        SleepLog.markBed(ms("2026-11-02T23:10"))
        SleepLog.recordWake(ms("2026-11-03T07:05"))
        val e = SleepLog.load().single()
        assertEquals("2026-11-03", e.d); assertEquals(425, e.w); assertEquals(1390, e.b)
        // Öğleden sonra kapatılan alarm kalkış sayılmaz
        SleepLog.recordWake(ms("2026-11-04T15:00"))
        assertEquals(1, SleepLog.load().size)
    }

    @Test fun journalTags() {
        assertEquals(setOf("kahve", "spor"), Journal.tags("Bugün #Kahve içtim, #spor yaptım. #kahve"))
    }
}
