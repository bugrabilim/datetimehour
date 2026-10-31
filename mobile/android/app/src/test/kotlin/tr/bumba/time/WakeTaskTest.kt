package tr.bumba.time

import android.app.AlarmManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Alarm kapatma görevi ve uyanma denetimi (#13) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WakeTaskTest {
    private val c get() = RuntimeEnvironment.getApplication()

    @Test fun fieldsAndEffectiveTask() {
        val a = AlarmItem(id = "a", hour = 7, minute = 0, task = "shake", taskLevel = 3, code = "X1", wakeCheck = true)
        val b = AlarmItem.fromJson(a.toJson())
        assertEquals(listOf("shake", 3, "X1", true), listOf(b.task, b.taskLevel, b.code, b.wakeCheck))
        // Eski kayıt: görev yok
        val old = AlarmItem.fromJson(org.json.JSONObject("""{"id":"o","hour":6,"minute":0}"""))
        assertEquals("none", old.effectiveTask)
        assertFalse(old.wakeCheck)
        // Kod kaydedilmediyse matematik
        assertEquals("math", a.copy(task = "code", code = "").effectiveTask)
        assertEquals("code", a.copy(task = "code").effectiveTask)
    }

    @Test fun taskAlarmCannotBeStoppedFromNotification() {
        AlarmItem.save(listOf(AlarmItem(id = "t", hour = 7, minute = 0, task = "math"), AlarmItem(id = "p", hour = 8, minute = 0)))
        val stop = L("tools.alarm.stop")
        val withTask = AlarmScheduler.notification(c, "t", "Kalkış", "alarm", silent = true).actions.map { it.title.toString() }
        assertFalse(stop in withTask)
        assertTrue(L("wake.title") in withTask)
        val plain = AlarmScheduler.notification(c, "p", "Kalkış", "alarm", silent = true).actions.map { it.title.toString() }
        assertTrue(stop in plain)
        // Ertelenmiş çalmada da görev sürer
        assertFalse(stop in AlarmScheduler.notification(c, AlarmScheduler.SNOOZE + "t", "Kalkış", "alarm", silent = true).actions.map { it.title.toString() })
        AlarmItem.save(emptyList())
    }

    @Test fun wakeCheckSchedulesAndCancels() {
        AlarmItem.save(listOf(AlarmItem(id = "w", hour = 7, minute = 0, wakeCheck = true)))
        val am = shadowOf(c.getSystemService(AlarmManager::class.java))
        // Başka planlar (bildirimler) arka planda kurulabildiği için sayı değil kimlik denetlenir
        fun ids() = am.scheduledAlarms.mapNotNull { a -> a.operation?.let { shadowOf(it).savedIntent.getStringExtra(AlarmScheduler.EXTRA_ID) } }
        AlarmScheduler.stop(c, "w")
        // Soru (5 dk) ve yedek alarm (7 dk)
        assertTrue("wakeask-w" in ids() && "wakecheck-w" in ids())
        WakeCheck.awake(c, "w")
        assertFalse("wakeask-w" in ids() || "wakecheck-w" in ids())
        AlarmItem.save(emptyList())
    }
}
