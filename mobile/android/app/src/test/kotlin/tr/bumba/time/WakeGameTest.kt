package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Test

/** Uyanma oyunu (2. analiz #7): puan, seri, seviye, gün birleştirme */
class WakeGameTest {
    private fun e(d: String, s: Int = 0, t: Boolean = false) = WakeGame.Event(d, 7 * 60, 7 * 60, s, t)

    @Test fun scoring() {
        val log = listOf(
            e("2026-11-01"), // 20 (seri 1)
            e("2026-11-02", t = true), // 20 + 5 + 2 (seri 2)
            e("2026-11-03"), // 20 + 4 (seri 3)
            e("2026-11-04", s = 1), // 13, seri biter
            e("2026-11-05", s = 3), // 10
            e("2026-11-07"), // 20 (gün atlandı, seri 1)
        )
        assertEquals(listOf(20, 27, 24, 13, 10, 20), WakeGame.scored(log).map { it.points })
        assertEquals(114, WakeGame.total(log))
        assertEquals(3, WakeGame.bestStreak(log))
        // Seri bonusu en çok +10
        val long = (1..9).map { e("2026-10-%02d".format(it)) }
        assertEquals(30, WakeGame.scored(long).last().points)
    }

    @Test fun levels() {
        assertEquals(listOf(0, 100, 300, 600, 1000), (1..5).map { WakeGame.threshold(it) })
        assertEquals(1, WakeGame.level(99))
        assertEquals(2, WakeGame.level(100))
        assertEquals(4, WakeGame.level(999))
        assertEquals(10, WakeGame.level(100_000))
        assertEquals(listOf(0, 1, 1, 2, 2, 3, 3, 4, 4, 4), (1..10).map { WakeGame.stage(it) })
    }

    @Test fun mergeSameDay() {
        var log = WakeGame.merge(emptyList(), e("2026-11-03", s = 1))
        log = WakeGame.merge(log, e("2026-11-03", s = 0, t = true))
        log = WakeGame.merge(log, e("2026-11-02"))
        assertEquals(listOf("2026-11-02", "2026-11-03"), log.map { it.d })
        assertEquals(1, log[1].s)
        assertEquals(true, log[1].t)
    }
}
