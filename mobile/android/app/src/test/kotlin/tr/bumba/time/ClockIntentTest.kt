package tr.bumba.time

import android.content.Intent
import android.provider.AlarmClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/** Asistan/uygulama istekleri (AlarmClock) ve simge kısayolları */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ClockIntentTest {
    private fun run(i: Intent) = Robolectric.buildActivity(ClockIntentActivity::class.java, i).create().get()

    @Test fun setTimer() {
        run(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, 300)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "Çay").putExtra(AlarmClock.EXTRA_SKIP_UI, true))
        val t = TimerItem.load().single()
        assertEquals(300_000L, t.total)
        assertEquals("Çay", t.name)
        assertTrue(t.running)
    }

    @Test fun setAlarm() {
        run(Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, 6).putExtra(AlarmClock.EXTRA_MINUTES, 30)
            .putExtra(AlarmClock.EXTRA_DAYS, arrayListOf(Calendar.MONDAY, Calendar.SUNDAY))
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true))
        val a = AlarmItem.load().single()
        assertEquals(6, a.hour)
        assertEquals(30, a.minute)
        assertEquals(listOf(1, 7), a.days)
        // Saat yoksa alarm eklenmez (ekran açılır)
        run(Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_SKIP_UI, true))
        assertEquals(1, AlarmItem.load().size)
    }

    @Test fun shortcuts() {
        Nav.handleShortcut(Intent().putExtra("shortcut", "timer"))
        assertEquals(Tab.TOOLS, Nav.tab)
        assertEquals("timer", Nav.toolsRoute)
        Nav.handleShortcut(Intent().putExtra("shortcut", "fullscreen").putExtra("night", true))
        assertEquals(Tab.CLOCK, Nav.tab)
        assertTrue(Nav.openFullscreen)
        assertEquals("on", Prefs.nightMode)
        Nav.openFullscreen = false
        Prefs.saveNightMode("off")
    }
}
