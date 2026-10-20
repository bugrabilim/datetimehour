package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Tarih hesaplama kuralları (iOS SaatCore DateCalc ve sitedeki dates.js ile aynı örnekler) */
class DateCalcTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun ymdMonthEnd() {
        assertEquals(DateCalc.Ymd(0, 1, 1), DateCalc.ymd(d("2026-01-31"), d("2026-03-01")))
        assertEquals(DateCalc.Ymd(1, 0, 0), DateCalc.ymd(d("2025-10-18"), d("2026-10-18")))
    }

    @Test fun workdays() {
        // 29 Ekim 2026 Perşembe resmî tatil, 28 Ekim arife yarım gün
        val off = mapOf(d("2026-10-29") to 0.0, d("2026-10-28") to 0.5)
        assertEquals(3.5, DateCalc.workdays(d("2026-10-26"), d("2026-10-31"), off), 1e-9)
        assertEquals(d("2026-11-02"), DateCalc.addWorkdays(d("2026-10-28"), 2, off))
        assertEquals(d("2026-10-27"), DateCalc.addWorkdays(d("2026-10-30"), -2, off))
    }

    @Test fun birthdayLeap() {
        assertEquals(d("2027-02-28"), DateCalc.nextBirthday(d("2000-02-29"), d("2026-10-18")))
        assertEquals(d("2026-10-18"), DateCalc.nextBirthday(d("1990-10-18"), d("2026-10-18")))
    }
}
