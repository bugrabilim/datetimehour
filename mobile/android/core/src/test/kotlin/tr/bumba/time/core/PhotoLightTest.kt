package tr.bumba.time.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Fotoğraf ışığı (9 Kasım 2026): SaatCore PhotoLightTests ile aynı örnekler; tahmin kuralının zorunlu durumları şartnameden */
class PhotoLightTest {
    /** (alçak, orta, yüksek bulut %, yağış mm/sa) → sonuç; sitedeki ve iOS'taki testlerle aynı 14 durum */
    @Test fun qualityRule() {
        val cases = listOf(
            doubleArrayOf(0.0, 0.0, 0.0, 0.0) to "fair",
            doubleArrayOf(10.0, 40.0, 10.0, 0.0) to "good",
            doubleArrayOf(10.0, 10.0, 60.0, 0.0) to "good",
            doubleArrayOf(30.0, 85.0, 90.0, 0.0) to "fair",
            doubleArrayOf(50.0, 50.0, 50.0, 0.0) to "fair",
            doubleArrayOf(65.0, 30.0, 30.0, 0.0) to "poor",
            doubleArrayOf(80.0, 0.0, 0.0, 0.0) to "poor",
            doubleArrayOf(5.0, 50.0, 50.0, 0.6) to "poor",
            doubleArrayOf(39.0, 20.0, 0.0, 0.0) to "good",
            doubleArrayOf(40.0, 20.0, 0.0, 0.0) to "fair",
            doubleArrayOf(0.0, 80.0, 0.0, 0.0) to "good",
            doubleArrayOf(0.0, 81.0, 0.0, 0.0) to "fair",
            doubleArrayOf(74.0, 0.0, 0.0, 0.0) to "poor",
            doubleArrayOf(59.0, 0.0, 0.0, 0.0) to "fair",
        )
        for ((c, want) in cases) {
            assertEquals(want, PhotoLight.quality(c[0], c[1], c[2], c[3]).key, c.joinToString())
        }
    }

    /** En yakın saatlik örnek; 90 dakikadan uzaksa yok */
    @Test fun nearestSample() {
        val h = 3_600_000L
        val samples = (0 until 6).map { PhotoLight.Sample(it * h, it * 10.0, 50.0, 0.0, 0.0) }
        assertEquals(2 * h, PhotoLight.nearest(samples, 2 * h + 29 * 60_000L)?.t)
        assertEquals(3 * h, PhotoLight.nearest(samples, 2 * h + 31 * 60_000L)?.t)
        assertEquals(5 * h, PhotoLight.nearest(samples, 5 * h + 80 * 60_000L)?.t)
        assertNull(PhotoLight.nearest(samples, 5 * h + 91 * 60_000L))
        assertNull(PhotoLight.nearest(emptyList(), 0))
        // 1. saat: alçak bulut 10, orta 50 → iyi; 5. saat: alçak 50 → orta
        assertEquals(PhotoLight.Quality.GOOD, PhotoLight.qualityAt(samples, h))
        assertEquals(PhotoLight.Quality.FAIR, PhotoLight.qualityAt(samples, 5 * h))
    }

    /** İstanbul 30 Ekim 2026: altın saat Mevsimler'deki saatler, gün doğumu −0,833°, yönler sitedeki sunAltAz ile aynı */
    @Test fun istanbul() {
        val d = PhotoLight.day(2026, 10, 30, 41.01, 28.98, 3.0)
        fun m(x: Double?) = Math.round(x!! * 60).toInt()
        assertEquals(7 * 60 + 15, m(d.light.goldenAm.first))
        assertEquals(8 * 60 + 11, m(d.light.goldenAm.second))
        assertEquals(17 * 60 + 24, m(d.light.goldenPm.first))
        assertEquals(18 * 60 + 31, m(d.light.bluePm.second))
        assertEquals(7.538731793789072, d.sunrise!!, 1e-6)
        assertEquals(18.04354430372772, d.sunset!!, 1e-6)
        assertEquals(107.62759669943475, d.riseAzimuth!!, 1e-6)
        assertEquals(252.1790509079625, d.setAzimuth!!, 1e-6)
        assertEquals(0, d.polar)
        // Gün doğumu sabah altın saatinin içinde, gün batımı akşam altın saatinin içinde
        assertTrue(d.sunrise!! > d.light.goldenAm.first!! && d.sunrise!! < d.light.goldenAm.second!!)
        assertTrue(d.sunset!! > d.light.goldenPm.first!! && d.sunset!! < d.light.goldenPm.second!!)
    }

    /** Tromsø: 21 Aralık kutup gecesi (altın saat yalnız başlıyor), 21 Haziran gece yarısı güneşi; Helsinki mavi saat gece yarısını geçer */
    @Test fun polar() {
        val night = PhotoLight.day(2026, 12, 21, 69.65, 18.96, 1.0)
        assertNull(night.sunrise)
        assertNull(night.sunset)
        assertNull(night.riseAzimuth)
        assertEquals(-1, night.polar)
        assertNotNull(night.light.goldenAm.first)
        assertNull(night.light.goldenAm.second)
        val day = PhotoLight.day(2026, 6, 21, 69.65, 18.96, 2.0)
        assertNull(day.sunset)
        assertEquals(1, day.polar)
        assertNull(day.light.bluePm.first)
        val helsinki = PhotoLight.day(2026, 6, 21, 60.17, 24.94, 3.0)
        assertEquals(0, helsinki.polar)
        assertEquals(24.705994483292148, helsinki.light.bluePm.second!!, 1e-6)
        assertEquals(34.41390618628495, helsinki.riseAzimuth!!, 1e-6)
    }
}
