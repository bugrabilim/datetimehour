package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId

/** Konum hiç seçilmediyse varsayılan yer telefonun saat diliminden (Türkiye'de İstanbul ili) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultPlaceTest {
    @Test fun turkeyKeepsIstanbul() {
        assertNull(DefaultPlace.place(ZoneId.of("Europe/Istanbul")))
        assertNull(DefaultPlace.place(ZoneId.of("UTC")))
    }

    @Test fun cityOrPlaceByZone() {
        val berlin = DefaultPlace.place(ZoneId.of("Europe/Berlin"))!!
        assertEquals(AppPlace.AUTO, berlin.id)
        assertEquals(52.5, berlin.lat, 0.5)
        assertEquals("Europe/Berlin", berlin.zone.id)
        // Dünya şehirlerinde olmayan dilim: places.json'daki en kalabalık yer
        val chicago = DefaultPlace.place(ZoneId.of("America/Chicago"))!!
        assertEquals(41.9, chicago.lat, 0.5)
        // Eski/yeni ad eşlemesi
        assertEquals(50.45, DefaultPlace.place(ZoneId.of("Europe/Kyiv"))!!.lat, 0.5)
        // Verimizde olmayan dilim: aynı bölgede, aynı UTC farkında bir yer; saat yine telefonun diliminden
        val knox = DefaultPlace.place(ZoneId.of("America/Indiana/Knox"))
        assertNotNull(knox)
        assertEquals("America/Indiana/Knox", knox!!.zone.id)
        assertTrue(knox.lon < -30)
    }

    /** Konum penceresinde dünya yeri araması (Hindistan araştırması #4): 31 şehir + places.json, Türkiye hariç */
    @Test fun worldPlaceSearch() {
        val hits = WorldPlace.search("Mumbai")
        assertTrue(hits.isNotEmpty())
        assertEquals("Asia/Kolkata", hits.first().zone.id)
        // Türkiye'deki yerler aramada yok (il ve ilçe listesi)
        assertTrue(WorldPlace.search("Istanbul").none { it.zone.id == "Europe/Istanbul" })
        // İngilizce adla ve yerel adla arama, kimlikten geri okuma
        val lucknow = WorldPlace.search("lucknow").first()
        val back = AppPlace.byId(lucknow.id)
        assertEquals(lucknow.id, back.id)
        assertEquals("Asia/Kolkata", back.zone.id)
        assertEquals(lucknow.lat, back.lat, 0.01)
        assertTrue(WorldPlace.search("a").isEmpty())
    }
}
