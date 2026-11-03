package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.bumba.time.core.Nature

/** Ay verileri (2. analiz #147): dördünlerde Ay–Güneş açısı 90°, yerberi/yeröte uzaklık aralığında, sıra doğru */
class MoonDetailsTest {
    @Test fun quartersAndDistance() {
        val now = java.time.Instant.parse("2026-11-03T12:00:00Z").toEpochMilli()
        val (fq, lq) = MoonDetails.quarters(now)
        assertTrue(fq > now && lq > now)
        assertEquals(90.0, MoonDetails.elongation(fq), 0.01)
        assertEquals(90.0, MoonDetails.elongation(lq), 0.01)
        // İlk dördün yeni aydan sonra, dolunaydan önce
        val m = Nature.moon(fq)
        assertTrue(m.fraction in 0.15..0.35)
        val (per, apo) = MoonDetails.perigeeApogee(now)
        assertTrue(per != null && apo != null)
        assertTrue(per!!.km in 355_000.0..371_000.0)
        assertTrue(apo!!.km in 403_000.0..407_000.0)
        assertTrue(per.at < now + 32L * 86_400_000 && apo.at < now + 32L * 86_400_000)
        assertTrue(MoonDetails.distanceKm(now) in 355_000.0..407_000.0)
    }
}
