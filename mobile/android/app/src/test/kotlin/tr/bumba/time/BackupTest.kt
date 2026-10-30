package tr.bumba.time

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Google yedeğinden geri yüklenen kayıtlarda cihaza özgü anahtarlar ilk açılışta bir kez silinir */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupTest {
    @Test fun restoredDeviceKeysCleared() {
        val c = ApplicationProvider.getApplicationContext<Context>()
        val marker = File(c.noBackupFilesDir, "installed")
        // Geri yükleme benzetimi: kayıtlar var, kurulum işareti yok
        marker.delete()
        Prefs.store.edit().putBoolean("notify.asked", true).putString("widget.place.7", "ankara")
            .putInt("alarm.snoozed.a1", 2).putString("world", "london,tokyo").commit()
        Prefs.init(c)
        assertFalse(Prefs.store.contains("notify.asked"))
        assertFalse(Prefs.store.contains("widget.place.7"))
        assertFalse(Prefs.store.contains("alarm.snoozed.a1"))
        assertTrue(Prefs.store.contains("world")) // kişisel kayıtlar kalır
        assertTrue(marker.exists())
        // Sonraki açılışlarda dokunulmaz
        Prefs.store.edit().putBoolean("notify.asked", true).commit()
        Prefs.init(c)
        assertTrue(Prefs.store.getBoolean("notify.asked", false))
    }
}
