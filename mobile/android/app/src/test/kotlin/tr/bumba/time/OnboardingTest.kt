package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import android.os.Looper

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

    @Test fun skipLeavesOnlyClockAndOffersResume() {
        Prefs.store.edit().clear().commit()
        Features.save(emptySet())
        Onboarding.load(fresh = true)
        // Bir soruya yanıt verip geçen kişi: yalnız Saat (ve Tümü) açık kalır, sorulan bölümler bildirim göndermez
        Onboarding.answer(Tab.PRAYER, true)
        Onboarding.skip()
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(Onboarding.pending)
        assertTrue(Onboarding.resume)
        assertEquals(listOf(Tab.CLOCK, Tab.TOOLS), Features.barTabs())
        assertFalse(Prefs.store.getBoolean(NotifyPrefs.ENABLED, true))
        // "Kuruluma devam et": hoş geldiniz atlanır, kurulum sayfasından açılır
        Onboarding.resumeSetup()
        assertTrue(Onboarding.pending)
        assertEquals(1, Onboarding.startPage)
        assertEquals(null, Onboarding.shown(Tab.PRAYER))
        Onboarding.skip()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(Onboarding.resume)
        // Ayarlardan bir değişiklik "Kuruluma devam et"i kaldırır
        Prefs.saveH12(true)
        shadowOf(Looper.getMainLooper()).idle()
        Onboarding.checkResume()
        assertFalse(Onboarding.resume)
        assertFalse(Prefs.store.contains(Onboarding.RESUME_KEY))
    }

    @Test fun upgradeSkipKeepsFeatures() {
        Prefs.store.edit().clear().commit()
        Features.save(setOf(Tab.PRAYER, Tab.WEATHER, Tab.TOOLS))
        Onboarding.load(fresh = false)
        Onboarding.begin()
        Onboarding.answer(Tab.PRAYER, false)
        Onboarding.skip()
        // Eski kullanıcı geçerse seçimi geri gelir, devam düğmesi çıkmaz
        assertTrue(Features.isOn(Tab.PRAYER))
        assertFalse(Onboarding.resume)
    }

    /** Vakitler sorusu dil grubuna göre (9 Kasım 2026, sahibin kararı): tr, ar, id, ur, fa başta, diğer dillerde sonda */
    @Test fun prayerQuestionOrderByLanguage() {
        for (l in listOf("tr", "ar", "in", "ur", "fa")) {
            org.robolectric.RuntimeEnvironment.setQualifiers("+$l")
            assertEquals(l, Tab.PRAYER, Onboarding.QUESTIONS.first())
            assertEquals(l, "q:prayer", Onboarding.PAGES[2])
        }
        // Diğer dillerde tanıtımda vakit/namaz hiç geçmez (9 Kasım 2026, sahibin kararı): soru ve tur yok
        for (l in listOf("en", "de", "ja", "hi", "es", "ru")) {
            org.robolectric.RuntimeEnvironment.setQualifiers("+$l")
            assertFalse(l, Tab.PRAYER in Onboarding.QUESTIONS)
            assertFalse(l, "tour:prayer" in Onboarding.PAGES)
            assertEquals(l, 4, Onboarding.QUESTIONS.size)
            assertEquals(l, "q:weather", Onboarding.PAGES[2])
            assertEquals(l, 6, Onboarding.SUMMARY)
            assertEquals(l, 16, Onboarding.PAGES.size)
        }
    }

    /** Vakitler sorulmayan dilde yeni kurulum: Vakitler kapalı, vakit bildirimleri de kapalı başlar (bitince ve geçince) */
    @Test fun noPrayerLanguageTurnsPrayerNotificationsOff() {
        org.robolectric.RuntimeEnvironment.setQualifiers("+en")
        for (skip in listOf(false, true)) {
            Prefs.store.edit().clear().commit()
            Features.save(emptySet())
            Onboarding.load(fresh = true)
            Onboarding.begin()
            for (t in Onboarding.QUESTIONS) Onboarding.answer(t, true)
            if (skip) Onboarding.skip() else Onboarding.finish()
            assertFalse(Features.isOn(Tab.PRAYER))
            assertFalse(Prefs.store.getBoolean(NotifyPrefs.ENABLED, true))
            assertFalse(Prefs.store.getBoolean("notify.friday", true))
            assertFalse(Onboarding.prayerShown)
        }
        org.robolectric.RuntimeEnvironment.setQualifiers("+tr")
    }
}
