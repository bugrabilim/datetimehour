package tr.bumba.time

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Kur'an verisi, dualar, zikirmatik tesbihatı, namaz/oruç takibi ve zekât hesabı */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReligionTest {
    private val c get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun quranData() {
        Quran.load(c)
        assertEquals(114, Quran.suras.size)
        assertEquals(30, Quran.juz.size)
        assertEquals(6236, Quran.suras.sumOf { it.ayas })
        assertEquals(7, Quran.suras[0].ayas)
        assertTrue(Quran.suras[0].mecca)
        assertFalse(Quran.suras[1].mecca)
        // Tanzil metni değiştirilmeden: ilk ayet besmele (harekelerin sırası kaynaktaki gibi kalır)
        assertEquals(4, Quran.ar(1, 1).split(" ").size)
        assertTrue(Quran.ar(1, 1).startsWith("بِسْمِ"))
        assertTrue(Quran.en(112, 1).startsWith("Say: He is Allah"))
        assertTrue(Quran.ar(114, 6).isNotBlank())
        assertEquals("https://kuran.diyanet.gov.tr/kuran?sure=2&ayet=255&mode=kuran-meal", Quran.diyanetUrl(2, 255))
        Quran.bookmark = 18 to 10
        assertEquals(18 to 10, Quran.bookmark)
        Quran.bookmark = null
        assertNull(Quran.bookmark)
    }

    @Test fun duas() {
        val d = Duas.load(c)
        assertEquals(12, d.size)
        val kursi = d.first { it.id == "ayetelkursi" }
        Quran.load(c)
        assertEquals(Quran.ar(2, 255), kursi.ar)
        assertEquals(listOf("2:255"), kursi.refs)
    }

    @Test fun tesbihat() {
        assertEquals("subhanallah" to false, Zikir.tesbihatStep(0))
        assertEquals("elhamdulillah" to false, Zikir.tesbihatStep(33))
        assertEquals("allahuekber" to false, Zikir.tesbihatStep(98))
        assertEquals("allahuekber" to true, Zikir.tesbihatStep(99))
    }

    @Test fun tracker() {
        Tracker.clear()
        val today = LocalDate.of(2026, 10, 30)
        Tracker.setDayMask(today, 0b11111)
        Tracker.setDayMask(today.minusDays(1), 0b00101)
        Tracker.setDayMask(today.minusDays(7), 0b11111) // 7 günün dışında
        assertEquals(7, Tracker.weekCount(today))
        Tracker.setKaza("fajr", 3)
        Tracker.setKaza("fajr", Tracker.kaza("fajr") - 5)
        assertEquals(0, Tracker.kaza("fajr"))
        Tracker.setFasted(today, true)
        assertTrue(Tracker.fasted(today))
        Tracker.clear()
        assertEquals(0, Tracker.dayMask(today))
        assertFalse(Tracker.fasted(today))
    }

    @Test fun zakat() {
        // Altın fiyatı yoksa nisab bilinmez
        val none = Zakat.calc(mapOf("cash" to 100_000.0))
        assertNull(none.nisab)
        assertFalse(none.due)
        // 80,18 g × 4.000 = 320.720 nisab; 400.000 + 20 g × 4.000 − 30.000 = 450.000 → 11.250
        val r = Zakat.calc(mapOf("cash" to 400_000.0, "goldGrams" to 20.0, "goldPrice" to 4000.0, "debt" to 30_000.0))
        assertEquals(450_000.0, r.net, 1e-6)
        assertEquals(320_720.0, r.nisab!!, 1e-6)
        assertTrue(r.due)
        assertEquals(11_250.0, r.zakat, 1e-6)
        val low = Zakat.calc(mapOf("cash" to 300_000.0, "goldPrice" to 4000.0))
        assertFalse(low.due)
        assertEquals(0.0, low.zakat, 0.0)
        // Borç varlıktan büyükse net 0
        assertEquals(0.0, Zakat.calc(mapOf("cash" to 10.0, "debt" to 50.0)).net, 0.0)
    }
}
