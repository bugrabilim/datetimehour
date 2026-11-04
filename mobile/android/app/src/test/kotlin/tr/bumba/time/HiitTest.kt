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

    /** İsteğe bağlı duyurular (2. analiz #135): dinlenmede sıradaki adım, yarısı (20 sn ve uzun evrelerde), son 10 saniye */
    @Test fun cueOptions() {
        val c = HiitConfig(warmup = 4, work = 24, rest = 20, rounds = 2)
        val o = HiitConfig.Cues(next = true, half = true, ten = true)
        var last: HiitConfig.Phase? = null
        val said = mutableListOf<String>()
        for (tenth in 0..c.totalSeconds * 5) {
            val p = c.phase(tenth / 5)
            c.cue(last, p, o)?.let { said += it }
            last = p
        }
        val work = L("hiit.say.work")
        val next = ". " + T("routine.next", L("hiit.work"))
        val expected = listOf(
            L("hiit.say.warmup") + next, "3", "2", "1",
            work, L("hiit.say.half"), L("hiit.say.ten"), "3", "2", "1",
            L("hiit.say.rest") + next, L("hiit.say.ten"), "3", "2", "1",
            L("hiit.say.last") + ". " + work, L("hiit.say.half"), L("hiit.say.ten"), "3", "2", "1",
            L("hiit.say.done"),
        )
        assertEquals(expected, said)
        // Bölümler: başlangıçlar ve süreler (adım atlama ve sıradakiler listesi)
        assertEquals(listOf(0 to 4, 4 to 24, 28 to 20, 48 to 24), c.segments().map { it.start to it.dur })
        assertEquals(72, c.totalSeconds)
        // Isınma ve dinlenme 0 olunca bölüm yok
        assertEquals(listOf(0 to 10, 10 to 10), HiitConfig(0, 10, 0, 2).segments().map { it.start to it.dur })
    }

    /** Antrenman geçmişi: bu hafta sayısı, en yeni başta */
    @Test fun history() {
        HiitLog.clear()
        val cfg = HiitConfig(10, 30, 15, 8)
        val mon = java.time.LocalDate.of(2026, 11, 2)
        HiitLog.add(cfg, mon.minusDays(2).atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        HiitLog.add(cfg, mon.plusDays(1).atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertEquals(1, HiitLog.thisWeek(mon.plusDays(2)))
        assertEquals("2026-11-03", HiitLog.load().first().d)
        assertEquals(cfg.totalSeconds, HiitLog.load().first().s)
        HiitLog.clear()
    }

    /** Çok adımlı rutin: adımlar her turda sırayla, ad evreye geçer, JSON gidiş-dönüş */
    @Test fun routine() {
        val c = HiitConfig(rounds = 2, name = "R", steps = listOf(HiitConfig.Step("A", 20), HiitConfig.Step("B", 10, true), HiitConfig.Step("C", 30)))
        assertEquals(2 * 60, c.totalSeconds)
        assertEquals(HiitConfig.Phase("work", 1, 20, 0, "A"), c.phase(0))
        assertEquals(HiitConfig.Phase("rest", 1, 10, 1, "B"), c.phase(20))
        assertEquals(HiitConfig.Phase("work", 1, 1, 2, "C"), c.phase(59))
        assertEquals(HiitConfig.Phase("work", 2, 20, 3, "A"), c.phase(60))
        assertEquals("done", c.phase(120).kind)
        assertEquals(c, HiitConfig.fromRoutine(c.routineJson()))
    }
}
