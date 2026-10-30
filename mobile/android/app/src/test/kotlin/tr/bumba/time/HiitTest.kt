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

/** Sesli duyurular: evre değişince adı, son 3 saniyede sayı (her saniye bir kez); sürdürünce o anki evre */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class HiitCueTest {
    @Test fun cues() {
        val c = HiitConfig(warmup = 4, work = 5, rest = 4, rounds = 2)
        var last: HiitConfig.Phase? = null
        val said = mutableListOf<String>()
        // Saniyede beş bakış (hizmetteki gibi 200 ms)
        for (tenth in 0..c.totalSeconds * 5) {
            val p = c.phase(tenth / 5)
            c.cue(last, p)?.let { said += it }
            last = p
        }
        val work = L("hiit.say.work")
        val expected = listOf(
            L("hiit.say.warmup"), "3", "2", "1",
            work, "3", "2", "1",
            L("hiit.say.rest"), "3", "2", "1",
            L("hiit.say.last") + ". " + work, "3", "2", "1",
            L("hiit.say.done"),
        )
        assertEquals(expected, said)
        // Duraklatıp sürdürünce (last = null) o anki evre yeniden söylenir
        assertEquals(work, c.cue(null, c.phase(6)))
    }
}
