package tr.bumba.time

import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Süren sayaçlar bildirimi: zamanlayıcı, kronometre ve sıradaki vakit; bitenler kalkar */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class LiveNotificationsTest {
    private val c = RuntimeEnvironment.getApplication()
    private fun active() = shadowOf(c.getSystemService(NotificationManager::class.java)).allNotifications
        .filter { it.channelId == LiveNotifications.CHANNEL }

    @Test fun timersStopwatchPrayer() {
        val now = System.currentTimeMillis()
        TimerItem.save(listOf(TimerItem(id = "a", name = "Çay", total = 300_000, end = now + 200_000), TimerItem(id = "b", total = 60_000, left = 30_000)))
        Prefs.store.edit().putLong("sw.start", now - 5_000).putLong("sw.acc", 1_000).apply()
        LiveNotifications.sync(c, now)
        val n = active()
        assertEquals(2, n.size) // duraklatılan zamanlayıcı yok
        assertTrue(n.any { it.extras.getString("android.title") == "Çay" && it.`when` == now + 200_000 })
        // Kronometre: başlangıç duraklamalar düşülerek
        assertTrue(n.any { it.`when` == now - 6_000 })
        // Sıradaki vakit açılınca eklenir
        Prefs.store.edit().putBoolean(LiveNotifications.PRAYER_KEY, true).apply()
        LiveNotifications.sync(c, now)
        assertEquals(3, active().size)
        // Kronometre durunca ve zamanlayıcı bitince kalkar
        Prefs.store.edit().putLong("sw.start", 0).putBoolean(LiveNotifications.PRAYER_KEY, false).apply()
        LiveNotifications.sync(c, now + 300_000)
        assertEquals(0, active().size)
    }
}
