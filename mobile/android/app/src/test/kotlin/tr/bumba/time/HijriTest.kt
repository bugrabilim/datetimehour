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

    /** Endonezya takvimi ve ±1 gün (10 Kasım 2026): Kemenag 1448 takvimi, SKB 2026 Idul Fitri 21 Mart */
    @Test fun kemenagAndOffset() {
        val app = RuntimeEnvironment.getApplication()
        Prefs.init(app)
        try {
            Prefs.store.edit().putString(Hijri.CAL, "kemenag").putInt(Hijri.OFFSET, 0).commit()
            assertEquals("kemenag:0", Hijri.key)
            // Cemaziyelahir 1448: Diyanet 10 Kasım, Kemenag 11 Kasım
            assertEquals(HijriDate(1448, 5, 30), h("2026-11-10"))
            assertEquals(HijriDate(1448, 6, 1), h("2026-11-11"))
            // Ramazan 1447 Endonezya'da da 19 Şubat; Şevval 21 Mart (Diyanet 20 Mart)
            assertEquals(LocalDate.parse("2026-02-19")..LocalDate.parse("2026-03-20"), AppData.ramadans.first())
            assertEquals(LocalDate.parse("2026-03-21"), RelDays.all.first { it.key == "rb1" && it.date.year == 2026 }.date)
            assertEquals(LocalDate.parse("2026-03-20"), RelDays.all.first { it.key == "rbEve" && it.date.year == 2026 }.date)
            // Türkiye'nin resmî tatili takvimde değişmez
            assert(AppData.events[LocalDate.parse("2026-03-20")].orEmpty().any { it.key == "rb1" })
            // Regaib her zaman perşembe
            val regaib = AppData.events.values.flatten().filter { it.key == "regaib" }
            assert(regaib.isNotEmpty() && regaib.all { it.date.dayOfWeek == java.time.DayOfWeek.THURSDAY })

            // Diyanet + 1 gün (Hindistan gibi): ay başları bir gün sonra
            Prefs.store.edit().putString(Hijri.CAL, "diyanet").putInt(Hijri.OFFSET, 1).commit()
            assertEquals(HijriDate(1448, 5, 29), h("2026-11-10"))
            assertEquals(HijriDate(1448, 6, 1), h("2026-11-11"))
            assertEquals(LocalDate.parse("2026-02-20"), AppData.ramadans.first().start)
            assertEquals(LocalDate.parse("2027-02-09"), AppData.events.entries.first { (_, l) -> l.any { it.key == "ramadanStart" && it.date.year == 2027 } }.key)
        } finally {
            Prefs.store.edit().remove(Hijri.CAL).remove(Hijri.OFFSET).commit()
        }
        assertEquals("diyanet:0", Hijri.key)
        assertEquals(HijriDate(1448, 6, 1), h("2026-11-10"))
    }
}
