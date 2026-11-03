package tr.bumba.time.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 2. analiz #47 (uyku düzeni) ve #48 (kronotip); SaatCore'daki SleepStatsTests ile aynı değerler */
class SleepStatsTest {
    @Test fun record() {
        var log = SleepStats.record(emptyList(), SleepStats.Entry("2026-11-02", 420))
        log = SleepStats.record(log, SleepStats.Entry("2026-11-02", 430, 1380))
        assertEquals(listOf(SleepStats.Entry("2026-11-02", 420, 1380)), log)
        assertEquals(480, SleepStats.duration(log[0]))
    }

    @Test fun summary() {
        val log = listOf(
            SleepStats.Entry("2026-10-24", 450), SleepStats.Entry("2026-10-25", 470),
            SleepStats.Entry("2026-10-30", 420, 1380), SleepStats.Entry("2026-10-31", 440, 0), SleepStats.Entry("2026-11-01", 400, 1350),
        )
        val s = SleepStats.summary(log, "2026-11-02", 480)
        assertEquals(420, s.weekWake)
        assertEquals(460, s.prevWake)
        assertEquals(3, s.nights)
        assertEquals(0 + 40 + 0, s.debt)
        assertEquals(24, s.spread)
        assertNull(SleepStats.summary(emptyList(), "2026-11-02", 480).weekWake)
    }

    @Test fun chronotype() {
        // Hafta içi 23:30–07:00, hafta sonu 01:00–09:30: MSF 05:15, düzeltilmiş ~04:54 → akşamcıl (3)
        val m = Chronotype.msfsc(1410, 420, 60, 570)
        assertEquals(294, m)
        assertEquals(3, Chronotype.category(m))
        assertEquals(0, Chronotype.category(Chronotype.msfsc(1290, 330, 1320, 360)))
    }
}
