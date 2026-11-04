package tr.bumba.time

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Özellik aç/kapa: yeni kurulumda yalnız Saat, önceki kullanıcıda hepsi açık (göç), barda geçici sekme */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeaturesTest {
    private fun prefs(name: String) = RuntimeEnvironment.getApplication().getSharedPreferences(name, Context.MODE_PRIVATE)

    @Test fun newInstallOnlyClock() {
        val sp = prefs("t-new")
        Features.load(sp)
        assertTrue(Features.enabled.isEmpty())
        assertEquals("", sp.getString(Features.KEY, null))
        assertEquals(listOf(Tab.CLOCK), Features.barTabs())
        // Kapalı bir sekme açılırsa (widget, bildirim, saat ekranındaki alan) bar değişmez, geri düğmeli sayfa açılır (#203)
        assertEquals(false, Features.isOn(Tab.PRAYER))
    }

    @Test fun existingUserKeepsAll() {
        val sp = prefs("t-old")
        sp.edit().putString("province", "ankara").commit()
        Features.load(sp)
        assertEquals(Features.OPTIONAL.toSet(), Features.enabled)
        assertEquals("weather,calendar,world,seasons,prayer,tools", sp.getString(Features.KEY, null))
        // Seçenek B göçü: çubukta Saat, Vakitler, Hava, Takvim ve Tümü (en çok 5)
        assertEquals("prayer,weather,calendar", sp.getString(Features.BAR_KEY, null))
        assertEquals(listOf(Tab.CLOCK, Tab.PRAYER, Tab.WEATHER, Tab.CALENDAR, Tab.TOOLS), Features.barTabs())
    }

    @Test fun saveKeepsOrder() {
        Features.save(setOf(Tab.TOOLS, Tab.WEATHER))
        assertEquals("weather,tools", Prefs.store.getString(Features.KEY, null))
        assertEquals(setOf(Tab.WEATHER, Tab.TOOLS), Features.parse("tools,weather,nope"))
        assertEquals(listOf(Tab.CLOCK, Tab.WEATHER, Tab.TOOLS), Features.barTabs())
        Features.set(Tab.WEATHER, false)
        assertEquals("tools", Prefs.store.getString(Features.KEY, null))
        // Yalnız Araçlar açık: Saat ve Tümü
        assertEquals(listOf(Tab.CLOCK, Tab.TOOLS), Features.barTabs())
    }

    @Test fun barChoice() {
        Features.save(emptySet())
        for (t in listOf(Tab.WORLD, Tab.SEASONS, Tab.PRAYER, Tab.CALENDAR)) Features.set(t, true)
        // İlk açılan 3 özellik kendiliğinden çubuğa girer, dördüncüsü Tümü'nde kalır
        assertEquals(listOf(Tab.WORLD, Tab.SEASONS, Tab.PRAYER), Features.bar)
        assertEquals(false, Features.toggleBar(Tab.CALENDAR))
        assertEquals(true, Features.toggleBar(Tab.SEASONS))
        assertEquals(true, Features.toggleBar(Tab.CALENDAR))
        Features.moveBar(Tab.CALENDAR, -1)
        assertEquals(listOf(Tab.WORLD, Tab.CALENDAR, Tab.PRAYER), Features.bar)
        assertEquals("world,calendar,prayer", Prefs.store.getString(Features.BAR_KEY, null))
        // Kapanan özellik çubuktan çıkar
        Features.set(Tab.WORLD, false)
        assertEquals(listOf(Tab.CLOCK, Tab.CALENDAR, Tab.PRAYER, Tab.TOOLS), Features.barTabs())
        // Araçlar çubuğa giremez (her zaman Tümü)
        Features.set(Tab.TOOLS, true)
        assertEquals(false, Features.toggleBar(Tab.TOOLS))
    }

    @Test fun allSearchAndFavorites() {
        Features.save(Features.OPTIONAL.toSet())
        Prefs.store.edit().remove(AllPrefs.FAVS).remove(AllPrefs.RECENT).commit()
        AllPrefs.reload()
        assertEquals(true, AllPrefs.toggleFav("alarm"))
        AllPrefs.toggleFav("quran"); AllPrefs.toggleFav("moon"); AllPrefs.toggleFav("timer")
        assertEquals(null, AllPrefs.toggleFav("zikir"))
        assertEquals(false, AllPrefs.toggleFav("moon"))
        assertEquals("alarm,quran,timer", Prefs.store.getString(AllPrefs.FAVS, null))
        AllPrefs.used("zikir"); AllPrefs.used("alarm"); AllPrefs.used("zikir")
        assertEquals(listOf("zikir", "alarm"), AllPrefs.recent)
        // Arama: araç ve ayar sayfası bulunur; kapalı özelliğin girişi çıkmaz
        assertTrue(AllPrefs.search(L("tools.hub.cards.stopwatch.name").uppercase()).any { it.first.id == "stopwatch" })
        assertTrue(AllPrefs.search(L("settings.notifications")).any { it.first.id == "s:notifications" })
        Features.set(Tab.PRAYER, false)
        assertTrue(AllPrefs.search(L("qibla.title")).none { it.first.id == "qibla" })
    }
}
