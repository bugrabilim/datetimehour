package tr.bumba.time

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Health Connect (rakip analizi listesi #84; iOS'ta Apple Sağlık): biten aralık antrenmanı ya da rutin, kullanıcı açarsa
 * (`health.save`, varsayılan kapalı) egzersiz oturumu olarak yazılır (tür, başlangıç, süre). Yalnız WRITE_EXERCISE izni;
 * uygulama sağlık verisi okumaz. Aynı antrenman bir kez yazılır (`hiit.healthSaved` = başlangıç; iOS'la aynı anahtarlar).
 */
object HealthLog {
    const val KEY = "health.save"
    val PERMS = setOf(HealthPermission.getWritePermission(ExerciseSessionRecord::class))

    val enabled: Boolean get() = Prefs.store.getBoolean(KEY, false)
    fun setEnabled(v: Boolean) = Prefs.store.edit().putBoolean(KEY, v).apply()

    fun available(c: Context): Boolean = runCatching { HealthConnectClient.getSdkStatus(c) == HealthConnectClient.SDK_AVAILABLE }.getOrDefault(false)

    suspend fun granted(c: Context): Boolean = runCatching {
        HealthConnectClient.getOrCreate(c).permissionController.getGrantedPermissions().containsAll(PERMS)
    }.getOrDefault(false)

    /** Antrenman bittiyse (ve daha önce yazılmadıysa) yazar; hizmetten, aralık ekranından ve uygulama öne gelince */
    fun recordHiitIfDone(c: Context) {
        val start = Prefs.store.getLong("hiit.start", 0L)
        if (start == 0L || Prefs.store.getLong("hiit.paused", 0L) > 0) return
        val cfg = HiitConfig.load()
        val elapsed = ((System.currentTimeMillis() - start) / 1000).toInt()
        if (cfg.phase(elapsed).kind != "done" || Prefs.store.getLong("hiit.healthSaved", 0L) == start) return
        Prefs.store.edit().putLong("hiit.healthSaved", start).apply()
        if (!enabled || !available(c)) return
        val app = c.applicationContext
        val begin = Instant.ofEpochMilli(start)
        val end = begin.plusSeconds(cfg.totalSeconds.toLong())
        val title = if (cfg.isRoutine) cfg.name else L("hiit.tab")
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                if (!granted(app)) return@launch
                val zone = ZoneId.systemDefault().rules
                HealthConnectClient.getOrCreate(app).insertRecords(listOf(ExerciseSessionRecord(
                    startTime = begin, startZoneOffset = zone.getOffset(begin),
                    endTime = end, endZoneOffset = zone.getOffset(end),
                    metadata = Metadata.activelyRecorded(device = Device(type = Device.TYPE_PHONE), clientRecordId = "hiit-$start"),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                    title = title,
                )))
            }
        }
    }
}

/** Aralık ekranındaki anahtar: açınca Health Connect izni istenir */
@Composable
fun HealthToggle() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val available = remember { HealthLog.available(context) }
    var on by remember { mutableStateOf(HealthLog.enabled) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { got ->
        val ok = got.containsAll(HealthLog.PERMS)
        on = ok; denied = !ok; HealthLog.setEnabled(ok)
    }
    LaunchedEffect(Unit) { if (on && available && !HealthLog.granted(context)) { denied = true } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("health.saveAndroid"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
            Switch(on, { v ->
                if (!v) { on = false; HealthLog.setEnabled(false); return@Switch }
                scope.launch {
                    if (HealthLog.granted(context)) { on = true; denied = false; HealthLog.setEnabled(true) }
                    else runCatching { launcher.launch(HealthLog.PERMS) }.onFailure { denied = true }
                }
            }, enabled = available, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
        }
        Text(L(if (!available) "health.unavailable" else if (denied) "health.denied" else "health.noteAndroid"), color = Theme.c.muted, fontSize = 13.sp)
    }
}

/** Health Connect'in "neden bu izin" bağlantısı: gizlilik sayfasının Android bölümünü açar */
class HealthRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://time.bumba.tr/gizlilik/#android-uygulamasi"))) }
        finish()
    }
}
