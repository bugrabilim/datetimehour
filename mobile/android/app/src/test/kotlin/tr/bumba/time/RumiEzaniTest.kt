package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** 2. analiz #120: Rumi tarih (Türkiye Takvimi geleneği; Kasım günleri Rumi 26 Teşrîn-i evvel = miladi 8 Kasım) */
class RumiEzaniTest {
    @Test fun rumi() {
        assertEquals(Triple(26, 10, 1442), Rumi.parts(LocalDate.of(2026, 11, 8)))
        assertEquals(Triple(20, 5, 1440), Rumi.parts(LocalDate.of(2024, 6, 2)))
        assertEquals(Triple(1, 3, 1442), Rumi.parts(LocalDate.of(2026, 3, 14)))
        assertEquals(Triple(28, 2, 1441), Rumi.parts(LocalDate.of(2026, 3, 13)))
        // Hızır günleri: Rumi 23 Nisan = miladi 6 Mayıs
        assertEquals(Triple(23, 4, 1442), Rumi.parts(LocalDate.of(2026, 5, 6)))
    }
}
