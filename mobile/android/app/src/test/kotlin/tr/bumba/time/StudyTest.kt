package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Ders çalışma sayacı (2. analiz #84): oturum kaydı, toplamlar, derslere göre sıralama */
class StudyTest {
    private val today = LocalDate.of(2026, 11, 3)

    @Test fun recordAndTotals() {
        // 20 sn ve sorusuz oturum yazılmaz; 29,5 dk yuvarlanınca 30 dk
        assertNull(Study.record(emptyList(), today, "", 20_000, 0, ""))
        var log = Study.record(emptyList(), today, "", 0, 3, "")!!
        log = Study.record(log, today, "Matematik", 1_770_000, 40, "  türev  ")!!
        log = Study.record(log, today.minusDays(3), "Fizik", 45 * 60_000L, 10, "")!!
        log = Study.record(log, today.minusDays(9), "Matematik", 60 * 60_000L, 5, "")!!
        assertEquals(listOf(0, 30, 45, 60), log.map { it.m })
        assertEquals("türev", log[1].n)
        assertEquals(Study.Total("", 30, 43), Study.total(log, today, 1))
        assertEquals(Study.Total("", 75, 53), Study.total(log, today, 7))
        assertEquals(Study.Total("", 135, 58), Study.total(log, today, 0))
        assertEquals(listOf("Fizik", "Matematik", ""), Study.bySubject(log, today).map { it.s })
        assertEquals(2 * 60_000L + 5_000, Study.elapsed(today.toEpochDay(), 60_000, today.toEpochDay() + 65_000))
        assertEquals(60_000L, Study.elapsed(0, 60_000, 999_999))
    }

    @Test fun logLimit() {
        var log = emptyList<Study.Entry>()
        repeat(Study.MAX_LOG + 5) { log = Study.record(log, today, "", 60_000, 0, "")!! }
        assertEquals(Study.MAX_LOG, log.size)
    }
}
