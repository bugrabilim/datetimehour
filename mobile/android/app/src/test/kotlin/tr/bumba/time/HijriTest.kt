package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Hicri tarih: Diyanet'in vakit tablosundaki hicri tarihlerle (namazvakitleri.diyanet.gov.tr, 30 Ekim 2026) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HijriTest {
    @Before fun load() = AppData.init(RuntimeEnvironment.getApplication())

    private fun h(s: String) = Hijri.date(LocalDate.parse(s))

    @Test fun diyanet() {
        assertEquals(HijriDate(1448, 5, 19), h("2026-10-30"))
        // Cemaziyelahir 1448 Diyanet'te Umm al-Qura'dan bir gün önce başlar
        assertEquals(HijriDate(1448, 6, 1), h("2026-11-10"))
        assertEquals(HijriDate(1448, 6, 15), h("2026-11-24"))
        assertEquals(HijriDate(1448, 10, 1), h("2027-03-09")) // Ramazan Bayramı
        assertEquals(HijriDate(1448, 12, 10), h("2027-05-16")) // Kurban Bayramı
        assertEquals(HijriDate(1449, 8, 3), h("2027-12-31"))
        // Ramazan 1447 Diyanet'te 19 Şubat 2026'da başladı (Umm al-Qura 18 Şubat)
        assertEquals(HijriDate(1447, 8, 30), h("2026-02-18"))
        assertEquals(HijriDate(1447, 9, 1), h("2026-02-19"))
    }

    @Test fun outsideTable() {
        assertNull(h("2025-12-20"))
        assertEquals(HijriDate(1447, 7, 1), h("2025-12-21"))
        assertNull(h("2032-06-01"))
    }

    @Test fun text() {
        RuntimeEnvironment.setQualifiers("+tr")
        assertEquals("19 Cemaziyelevvel 1448", h("2026-10-30")!!.text)
    }
}
