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
    /** Yeni alarm görevleri (#75): hafıza dizisi, yazma denetimi, fotoğraf özeti */
    @Test
    fun moreWakeTasks() {
        val seq = WakeTask.memorySequence(2, kotlin.random.Random(7))
        kotlin.test.assertEquals(5, seq.size)
        kotlin.test.assertTrue(seq.all { it in 0..8 } && seq.zipWithNext().none { it.first == it.second })
        kotlin.test.assertTrue(WakeTask.typed("Damlaya damlaya göl olur.", "  damlaya DAMLAYA göl olur "))
        kotlin.test.assertFalse(WakeTask.typed("Damlaya damlaya göl olur.", "damlaya göl olur"))
        val img = IntArray(72) { (it * 37) % 255 }
        val same = img.map { (it + 6).coerceAtMost(255) }.toIntArray() // biraz daha aydınlık: özet aynı kalır
        kotlin.test.assertTrue(WakeTask.samePhoto(WakeTask.dHash(img), WakeTask.dHash(same)))
        val other = IntArray(72) { 255 - (it * 37) % 255 }
        kotlin.test.assertFalse(WakeTask.samePhoto(WakeTask.dHash(img), WakeTask.dHash(other)))
    }

    /** Odak serisi ve etiketler (rakip analizi #27): bugün ya da dün biten seri; etiketsizler "none" */
    @Test
    fun focusStreakAndTags() {
        val log = listOf(
            FocusLog.Entry("2026-10-27", 25, "", "work"), FocusLog.Entry("2026-10-28", 25, "", "work"),
            FocusLog.Entry("2026-10-29", 25, "", "study"), FocusLog.Entry("2026-10-29", 50, ""),
        )
        val today = java.time.LocalDate.of(2026, 10, 30)
        assertEquals(3, FocusLog.streak(log, today)) // bugün yok, dünden geriye 3 gün
        assertEquals(4, FocusLog.streak(log + FocusLog.Entry("2026-10-30", 25, ""), today))
        assertEquals(0, FocusLog.streak(log, today.plusDays(2)))
        assertEquals(listOf("work" to 50, "none" to 50, "study" to 25), FocusLog.tags(log, "2026-10-24")) // eşit dakikada ilk görülen önce
        assertEquals(listOf("study" to 25, "none" to 50).sortedByDescending { it.second }, FocusLog.tags(log, "2026-10-29"))
    }

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

    /** Akıllı alarm: durgunlukta erken çalmaz; pencerede iki dakika hareket olunca çalar; tek kıpırtı ya da pencere öncesi
     *  hareket yetmez (iOS SmartWakeTests ile aynı senaryo ve aynı sözde rastgele gürültü) */
    @Test
    fun smartWake() {
        val alarm = 1_800_000_000_000L - 1_800_000_000_000L % 60_000L
        fun run(bursts: Set<Int>): Long? {
            val w = SmartWake()
            var seed = 12345L
            var t = alarm - 50 * 60_000L
            while (t < alarm) {
                seed = (seed * 1103515245L + 12345L) % 2147483648L
                val noise = (seed / 2147483648.0 - 0.5) * 0.01
                val minute = ((alarm - t + 59_999) / 60_000).toInt() // alarma kalan dakika (yukarı yuvarlı)
                val inBurst = minute in bursts && (t / 100) % 600 < 20
                val z = 1.0 + noise + if (inBurst) 0.1 * sin(t / 100.0) else 0.0
                w.add(t, 0.0, 0.0, z)
                if (w.shouldWake(t, alarm, 30)) return t
                t += 100
            }
            return null
        }
        assertEquals(null, run(emptySet()), "durgunlukta erken çalmamalı")
        val at = run(setOf(20, 19))
        assertTrue(at != null && at >= alarm - 19 * 60_000L && at < alarm - 18 * 60_000L, "iki dakika hareket: 19. dakikada çalmalı ($at)")
        assertEquals(null, run(setOf(20)), "tek dakikalık kıpırtı yetmemeli")
        assertEquals(null, run(setOf(45, 44)), "pencere öncesi hareket sayılmamalı")
        assertEquals(null, SmartWake().let { it.add(0, 0.0, 0.0, 1.0); if (it.shouldWake(alarm - 60_000L, alarm, 0)) 1 else null }, "pencere 0 ise kapalı")
    }
}
