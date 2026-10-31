package tr.bumba.time

import org.robolectric.RuntimeEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Özellik bildirimi planı: varsayılan açık kategoriler (hava kapalı, ağ yok), sıralama ve kapatma */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeatureNotifierTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun defaultsAndToggles() = runBlocking {
        FeaturePrefs.set(NotifyFeature.WEATHER, false)
        val now = Instant.parse("2026-10-30T10:00:00Z")
        val all = FeatureNotifier.plan(context, now)
        assertTrue(all.all { it.at.isAfter(now) })
        // 14 gün boyunca gün doğumu ve batımı, en az bir dolunay ve yeni ay; saat başı varsayılan kapalı
        assertTrue(all.count { it.id.startsWith("sunrise-") } >= 13)
        assertTrue(all.count { it.id.startsWith("sunset-") } >= 13)
        assertEquals(0, all.count { it.id.startsWith("hourly-") })
        FeaturePrefs.set(NotifyFeature.HOURLY, true)
        assertTrue(FeatureNotifier.plan(context, now).count { it.id.startsWith("hourly-") } in 13..26)
        assertTrue(all.any { it.id.startsWith("moon-full-") } && all.any { it.id.startsWith("moon-new-") })
        // Sonraki mevsim başlangıcı: 21 Aralık 2026 kış gündönümü
        assertTrue(all.any { it.id.startsWith("season-") && it.at.toString().startsWith("2026-12-21") })
        all.filter { it.id.startsWith("cal-") }.forEach { assertTrue(it.title.isNotBlank()) }

        NotifyFeature.entries.forEach { FeaturePrefs.set(it, false) }
        assertEquals(emptyList<FeatureNotifier.Planned>(), FeatureNotifier.plan(context, now))
        NotifyFeature.entries.forEach { Prefs.store.edit().remove(it.key).commit() }
    }
}
