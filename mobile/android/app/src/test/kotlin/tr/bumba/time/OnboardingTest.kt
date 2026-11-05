package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Sorulu ilk kurulum (5 Kasım 2026): yanıtlar alt çubuğu ve bildirimleri değiştirir; 1.1'e geçen eski kullanıcı bir kez görür */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingTest {
    @Test fun answersShapeBarAndNotifications() {
        Prefs.store.edit().clear().commit()
        Features.save(emptySet())
        Onboarding.load(fresh = true)
        assertTrue(Onboarding.pending)
        // Yeni kurulumda hiçbir yanıt önceden işaretli değil
        assertEquals(null, Onboarding.shown(Tab.PRAYER))
        Onboarding.answer(Tab.PRAYER, true)
        Onboarding.answer(Tab.WEATHER, false)
        Onboarding.answer(Tab.CALENDAR, true)
        Onboarding.answer(Tab.WORLD, false)
        Onboarding.answer(Tab.SEASONS, false)
        // Alt çubuk yanıtlarla hemen oluşur: Saat, Vakitler, Takvim, Tümü
        assertEquals(listOf(Tab.CLOCK, Tab.PRAYER, Tab.CALENDAR, Tab.TOOLS), Features.barTabs())
        Onboarding.finish()
        assertFalse(Onboarding.pending)
        assertEquals(Onboarding.VERSION, Prefs.store.getInt(Onboarding.SEEN_KEY, 0))
        // "Hayır" denenlerin bildirimi kapalı, "Evet" denenlere dokunulmaz
        assertFalse(Prefs.store.getBoolean("notify.weather", true))
        assertFalse(Prefs.store.getBoolean("notify.moon", true))
        assertTrue(Prefs.store.getBoolean(NotifyPrefs.ENABLED, true))
        assertTrue(Prefs.store.getBoolean("notify.calendar", true))
        // Yeni kurulumda Vakitler'e evet: saatin altında sıradaki vakit; araçlar her zaman açık
        assertTrue(HomeItem.NEXT in HomeItems.layout.enabled)
        assertTrue(Features.isOn(Tab.TOOLS))
    }

    @Test fun upgradeShowsTourOnce() {
        Prefs.store.edit().clear().putString("province", "ankara").commit()
        Features.save(setOf(Tab.PRAYER, Tab.WEATHER))
        // 1.1 öncesi kullanıcı: tanıtımın bu sürümünü görmedi
        Onboarding.load(fresh = false)
        assertTrue(Onboarding.pending)
        // Eski kullanıcıda sorular açık özelliklerle işaretli gelir
        assertEquals(true, Onboarding.shown(Tab.PRAYER))
        assertEquals(false, Onboarding.shown(Tab.WORLD))
        Onboarding.finish()
        // Saat ekranına dokunulmaz (yalnız yeni kurulumda)
        assertFalse(Prefs.store.contains("homeItems"))
        Onboarding.load(fresh = false)
        assertFalse(Onboarding.pending)
        // Ayarlar › Destek › Tanıtımı yeniden göster
        Onboarding.replay()
        assertTrue(Onboarding.pending)
        Onboarding.finish()
    }
}
