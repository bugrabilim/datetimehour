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
        assertEquals(listOf(Tab.CLOCK), Features.barTabs(Tab.CLOCK))
        // Kapalı bir sekme açılırsa (widget, bildirim, saat ekranındaki alan) geçici olarak barda
        assertEquals(listOf(Tab.CLOCK, Tab.PRAYER), Features.barTabs(Tab.PRAYER))
    }

    @Test fun existingUserKeepsAll() {
        val sp = prefs("t-old")
        sp.edit().putString("province", "ankara").commit()
        Features.load(sp)
        assertEquals(Features.OPTIONAL.toSet(), Features.enabled)
        assertEquals("weather,calendar,world,seasons,prayer,tools", sp.getString(Features.KEY, null))
        assertEquals(Tab.entries.toList(), Features.barTabs(Tab.CLOCK))
    }

    @Test fun saveKeepsOrder() {
        Features.save(setOf(Tab.TOOLS, Tab.WEATHER))
        assertEquals("weather,tools", Prefs.store.getString(Features.KEY, null))
        assertEquals(setOf(Tab.WEATHER, Tab.TOOLS), Features.parse("tools,weather,nope"))
        assertEquals(listOf(Tab.CLOCK, Tab.WEATHER, Tab.TOOLS), Features.barTabs(Tab.CLOCK))
        Features.set(Tab.WEATHER, false)
        assertEquals("tools", Prefs.store.getString(Features.KEY, null))
    }
}
