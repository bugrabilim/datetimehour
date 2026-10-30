package tr.bumba.time.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FocusSleepWakeTest {
    private val days = (24..30).map { "2026-10-$it" }

    @Test
    fun focusStats() {
        var log = listOf(
            FocusLog.Entry("2026-10-01", 25, "eski"),
            FocusLog.Entry("2026-10-24", 25, "rapor"),
            FocusLog.Entry("2026-10-29", 50, "rapor"),
            FocusLog.Entry("2026-10-30", 25, ""),
        )
        log = FocusLog.add(log, FocusLog.Entry("2026-10-30", 25, "  kod  "))
        val s = FocusLog.stats(log, days, "Adsız")
        assertEquals(FocusLog.Sum(2, 50), s.today)
        assertEquals(FocusLog.Sum(4, 125), s.week)
        assertEquals(FocusLog.Sum(5, 150), s.total)
        assertEquals(listOf(25, 0, 0, 0, 0, 50, 50), s.perDay)
        assertEquals(listOf("rapor" to 75, "Adsız" to 25, "kod" to 25), s.top)
    }

    @Test
    fun focusLogIsCapped() {
        var log = emptyList<FocusLog.Entry>()
        repeat(1005) { log = FocusLog.add(log, FocusLog.Entry("2026-10-30", it, "x".repeat(80))) }
        assertEquals(1000, log.size)
        assertEquals(5, log.first().m)
        assertEquals(60, log.last().t.length)
    }

    @Test
    fun noiseLevelsAndLoop() {
        for (kind in NoiseGen.KINDS) {
            val s = NoiseGen.samples(kind, rate = 8000)
            assertEquals(NoiseGen.seconds(kind) * 8000, s.size, kind)
            val rms = sqrt(s.sumOf { it.toDouble() * it } / s.size)
            assertTrue(rms in 0.17..0.21, "$kind rms $rms")
            assertTrue(s.all { it in -1f..1f }, kind)
        }
        // Kahverengi gürültü yavaş değişir: döngü yerinde sıçrama olmamalı
        val b = NoiseGen.samples("brown", rate = 8000)
        assertTrue(abs(b.last() - b.first()) < 0.05f, "brown seam ${b.last()} ${b.first()}")
        // Aynı tohum aynı ses
        assertTrue(NoiseGen.samples("pink", rate = 8000).contentEquals(NoiseGen.samples("pink", rate = 8000)))
    }

    @Test
    fun sleepCycles() {
        // 07:00 kalkış: 21:45, 23:15, 00:45 (site alarm sayfalarıyla aynı)
        assertEquals(listOf(6 to 21 * 60 + 45, 5 to 23 * 60 + 15, 4 to 45), Sleep.bedtimes(7 * 60))
        assertEquals(listOf(6 to 8 * 60 + 15, 5 to 6 * 60 + 45, 4 to 5 * 60 + 15), Sleep.wakeTimes(23 * 60))
        assertEquals(23 * 60, Sleep.bedtime(7 * 60, 8 * 60))
    }

    @Test
    fun sunrise() {
        val at = 1_800_000_000_000L
        assertEquals(0.0, SunriseLight.level(at - 31 * 60_000L, at, 30))
        assertEquals(0.5, SunriseLight.level(at - 15 * 60_000L, at, 30), 1e-9)
        assertEquals(1.0, SunriseLight.level(at + 60_000L, at, 30))
        assertEquals(0.0, SunriseLight.level(at + 16 * 60_000L, at, 30))
        assertEquals(0.0, SunriseLight.level(at - 60_000L, at, 0))
        var prev = -1.0
        for (m in 30 downTo 0) {
            val l = SunriseLight.level(at - m * 60_000L, at, 30)
            assertTrue(l >= prev); prev = l
        }
        assertEquals(0x000000, SunriseLight.color(0.0))
        assertEquals(0xFFECCC, SunriseLight.color(1.0))
        assertEquals(0xFFA048, SunriseLight.color(0.8))
    }

    @Test
    fun mathProblems() {
        val r = Random(7)
        for (level in 1..3) repeat(50) {
            val p = WakeTask.problem(level, r)
            val parts = p.text.split(" ")
            val v = if (parts[1] == "+") parts[0].toInt() + parts[2].toInt() else parts[0].toInt() * parts[2].toInt() + parts[4].toInt()
            assertEquals(v, p.answer, p.text)
        }
    }

    @Test
    fun motionCounting() {
        // Yürüyüş: saniyede 1,8 adım, 10 sn (50 Hz örnek)
        val steps = MotionCounter.steps()
        for (i in 0 until 500) {
            val t = i * 20L
            steps.feed(t, 0.0, 0.0, 1.0 + 0.3 * sin(2 * PI * 1.8 * t / 1000))
        }
        assertEquals(18, steps.count)
        // Masada duran telefon: hiç sayılmaz
        val still = MotionCounter.steps()
        val r = Random(1)
        for (i in 0 until 500) still.feed(i * 20L, r.nextDouble(-0.02, 0.02), r.nextDouble(-0.02, 0.02), 1.0 + r.nextDouble(-0.02, 0.02))
        assertEquals(0, still.count)
        assertEquals(0, MotionCounter.shake().also { s -> for (i in 0 until 500) s.feed(i * 20L, 0.0, 0.0, 1.0 + 0.3 * sin(2 * PI * 1.8 * i * 20 / 1000)) }.count)
        // Sallama: saniyede 2 kez ileri geri (4 tepe), 5 sn
        val shake = MotionCounter.shake()
        for (i in 0 until 250) shake.feed(i * 20L, 2.5 * sin(2 * PI * 2 * i * 20 / 1000), 0.0, 1.0)
        assertTrue(shake.count in 18..22, "shake ${shake.count}")
    }
}
