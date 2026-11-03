package tr.bumba.time.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 2. analiz #27 (odak çizelgesi) ve #28 (nefes); SaatCore'daki ScheduleBreathTests ile aynı değerler */
class ScheduleBreathTest {
    private val weekdays = setOf(1, 2, 3, 4, 5)

    @Test fun schedule() {
        assertTrue(FocusSchedule.active(weekdays, 480, 1020, 1, 480))
        assertFalse(FocusSchedule.active(weekdays, 480, 1020, 1, 1020))
        assertFalse(FocusSchedule.active(weekdays, 480, 1020, 6, 600))
        // Gece yarısını aşan: cuma 22:00 – cumartesi 06:00
        assertTrue(FocusSchedule.active(setOf(5), 1320, 360, 5, 1400))
        assertTrue(FocusSchedule.active(setOf(5), 1320, 360, 6, 100))
        assertFalse(FocusSchedule.active(setOf(5), 1320, 360, 1, 100))
        assertFalse(FocusSchedule.active(weekdays, 600, 600, 1, 600))
        assertEquals(60, FocusSchedule.minutesToNextBoundary(480, 1020, 420))
        assertEquals(1440 - 1020 + 480, FocusSchedule.minutesToNextBoundary(480, 1020, 1020))
    }

    @Test fun breath() {
        val p = Breath.PATTERNS["478"]!!
        assertEquals(19.0, Breath.cycleSeconds(p))
        val a = Breath.phase(p, 2.0)
        assertEquals(0, a.step); assertEquals(0.5, a.progress, 1e-9)
        val b = Breath.phase(p, 5.0)
        assertEquals(1, b.step); assertEquals(1.0, Breath.scale(b))
        val c = Breath.phase(p, 15.0)
        assertEquals(2, c.step); assertEquals(0.5, Breath.scale(c), 1e-9)
        val d = Breath.phase(p, 19.5)
        assertEquals(1, d.cycle); assertEquals(0, d.step)
        // 0 sn'lik tutma atlanır
        assertEquals(2, Breath.phase(Breath.PATTERNS["calm"]!!, 4.5).step)
    }
}
