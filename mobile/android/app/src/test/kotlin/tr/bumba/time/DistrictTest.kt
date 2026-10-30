package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** İlçeler: 973 kayıt, kimlik, il merkezi, seçili yer ve kayıtlı yerler */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DistrictTest {
    @Test fun data() {
        assertEquals(973, AppData.districts.values.sumOf { it.size })
        assertEquals(39, District.all("istanbul").size)
        val k = District.find("d:istanbul:Kadıköy")
        assertNotNull(k)
        assertTrue(k!!.label.startsWith("Kadıköy"))
        // "Hakkâri" il merkezi sayılır (şapkalı yazım)
        assertTrue(District.all("hakkari").single { it.isCentre }.name.startsWith("Hakk"))
    }

    @Test fun selectedAndSaved() {
        Prefs.saveProvince("d:hakkari:Yüksekova")
        val p = AppPlace.current()
        assertEquals("d:hakkari:Yüksekova", p.id)
        assertEquals(37.5736, p.lat, 1e-4)
        Prefs.saveSavedPlaces(listOf("d:hakkari:Yüksekova", "ankara", "d:hakkari:Yüksekova"))
        assertEquals(listOf("d:hakkari:Yüksekova", "ankara"), Prefs.savedPlaces)
        Prefs.saveProvince("istanbul")
        Prefs.saveSavedPlaces(emptyList())
    }
}
