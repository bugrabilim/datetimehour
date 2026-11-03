package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Dünya saatleri (2. analiz #128, #129): 1960 yerde ve kısaltmayla arama, anahtar gidiş-dönüşü, kişisel ad, gündüz/gece */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class WorldEntryTest {
    @Test fun searchAndKeys() {
        // Kısaltma: EST → New York dilimi
        val est = WorldEntry.search("EST", emptySet())
        assertEquals("tz:America/New_York", est.first().key)
        // 31 şehirden biri: anahtarı aynı, bayrağı var
        val ber = WorldEntry.search("Berl", emptySet()).first()
        assertEquals("berlin", ber.key)
        assertEquals("🇩🇪", ber.flag)
        // places.json'dan bir yer: koordinatlı anahtar ve geri çözme
        val delhi = WorldEntry.search("Delhi", emptySet()).first { it.key.startsWith("p:") }
        val back = WorldEntry.of(delhi.key)
        assertNotNull(back)
        assertEquals("Asia/Kolkata", back!!.zone.id)
        // Listede olan bir daha önerilmez
        assertTrue(WorldEntry.search("Berl", setOf("berlin")).none { it.key == "berlin" })
        // Kişisel ad
        WorldLabels.set("berlin", "Annem 👵")
        assertEquals("Annem 👵", WorldEntry.of("berlin")!!.displayName)
        WorldLabels.set("berlin", "")
        assertEquals(ber.name, WorldEntry.of("berlin")!!.displayName)
        // Gündüz/gece: Tokyo'da 3 Kasım 2026 03:00 UTC = yerel 12:00 gündüz, 15:00 UTC = 00:00 gece
        val tokyo = WorldEntry.of("tokyo")!!
        assertEquals("day", tokyo.light(Instant.parse("2026-11-03T03:00:00Z")))
        assertEquals("night", tokyo.light(Instant.parse("2026-11-03T15:00:00Z")))
        assertEquals("UTC+9", tokyo.utcOffset(Instant.parse("2026-11-03T03:00:00Z")))
    }
}
