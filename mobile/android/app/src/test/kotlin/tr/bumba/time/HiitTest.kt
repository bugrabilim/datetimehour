package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Test

/** Aralık antrenmanı evreleri: ısınma, çalışma/dinlenme turları, son turda dinlenme yok */
class HiitTest {
    @Test fun phases() {
        val c = HiitConfig(warmup = 10, work = 30, rest = 15, rounds = 3)
        assertEquals(10 + 3 * 30 + 2 * 15, c.totalSeconds)
        assertEquals(HiitConfig.Phase("warmup", 0, 10, 0), c.phase(0))
        assertEquals(HiitConfig.Phase("work", 1, 30, 1), c.phase(10))
        assertEquals(HiitConfig.Phase("work", 1, 1, 1), c.phase(39))
        assertEquals(HiitConfig.Phase("rest", 1, 15, 2), c.phase(40))
        assertEquals(HiitConfig.Phase("work", 2, 30, 3), c.phase(55))
        assertEquals(HiitConfig.Phase("work", 3, 30, 5), c.phase(100))
        assertEquals("done", c.phase(130).kind)
        // Isınmasız
        assertEquals("work", HiitConfig(warmup = 0, work = 20, rest = 10, rounds = 2).phase(0).kind)
    }
}
