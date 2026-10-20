package tr.bumba.time

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import tr.bumba.time.core.Prayer

/**
 * Vakit bildirimleri ayarı (iOS'taki NotificationSettingsView): aç/kapat, hangi vakitler, kaç dakika önce, ses
 * (sesli duyuru, kısa zil, telefonun sesi) ve "Dinle". Bildirim izni yalnız açarken istenir (Android 13+);
 * tam zamanlı alarm izni yoksa (Android 12+) izin satırı gösterilir.
 */
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    NotifyPrefs.load()
    val enabled = NotifyPrefs.enabled
    var denied by remember { mutableStateOf(false) }
    var exact by remember { mutableStateOf(PrayerNotifier.exactAllowed(context)) }
    var voice by remember { mutableStateOf<NotificationSounds.VoiceStatus?>(null) }
    // Ayarlardan dönünce izinler yeniden okunur
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumed++
        onPauseOrDispose {}
    }
    LaunchedEffect(resumed) {
        exact = PrayerNotifier.exactAllowed(context)
        denied = enabled && !PrayerNotifier.notificationsAllowed(context)
        if (enabled) PrayerNotifier.reschedule(context)
    }
    LaunchedEffect(NotifyPrefs.sound, enabled) {
        if (enabled && NotifyPrefs.sound == "voice" && voice == null) voice = runCatching { NotificationSounds.voiceStatus(context) }.getOrNull()
    }
    fun changed() = PrayerNotifier.reschedule(context)

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        NotifyPrefs.saveEnabled(ok)
        denied = !ok
        changed()
    }
    fun setEnabled(on: Boolean) {
        if (!on) {
            NotifyPrefs.saveEnabled(false)
            denied = false
            changed()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !PrayerNotifier.notificationsAllowed(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val ok = PrayerNotifier.notificationsAllowed(context)
        NotifyPrefs.saveEnabled(ok)
        denied = !ok
        changed()
    }

    Column(Modifier.fillMaxSize()) {
        SubBar(L("notify.heading"), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("notify.enable"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                    Switch(enabled, ::setEnabled, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                Text(L("notify.note"), color = Theme.c.muted, fontSize = 13.sp)
            }
            if (denied) {
                AppCard {
                    Text(L("notify.denied"), color = Theme.c.text, fontSize = 14.sp)
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }) { Text(L("notify.openSettings"), color = Theme.c.accent) }
                }
            }
            if (enabled && !exact && Build.VERSION.SDK_INT >= 31) {
                AppCard {
                    Text(L("notify.exact"), color = Theme.c.text, fontSize = 14.sp)
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName)),
                        )
                    }) { Text(L("notify.exactAllow"), color = Theme.c.accent) }
                }
            }
            if (enabled) {
                AppCard {
                    SectionTitle(L("notify.prayers"))
                    val selected = NotifyPrefs.prayers
                    for (p in Prayer.entries) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(p.title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                            Switch(p in selected, { on ->
                                NotifyPrefs.savePrayers(if (on) selected + p else selected - p)
                                changed()
                            }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                        }
                    }
                }
                AppCard {
                    SectionTitle(L("notify.lead"))
                    for (m in PrayerAlerts.LEAD_CHOICES) {
                        Choice(if (m == 0) L("notify.lead.zero") else T("notify.lead.minutes", m), NotifyPrefs.lead == m) {
                            NotifyPrefs.saveLead(m)
                            changed()
                        }
                    }
                }
                AppCard {
                    SectionTitle(L("notify.sound"))
                    for (s in NotifyPrefs.SOUNDS) {
                        Choice(L("notify.sound.$s"), NotifyPrefs.sound == s) {
                            NotifyPrefs.saveSound(s)
                            changed()
                        }
                    }
                    TextButton(onClick = { scope.launch { NotificationSounds.preview(context, NotifyPrefs.sound, NotifyPrefs.lead) } }) {
                        Icon(Icons.Filled.PlayCircle, null, tint = Theme.c.accent)
                        Text("  " + L("notify.sound.preview"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                    }
                    Text(L("notify.sound.note"), color = Theme.c.muted, fontSize = 13.sp)
                    if (NotifyPrefs.sound == "voice" && voice != null && voice != NotificationSounds.VoiceStatus.GOOD) {
                        Text(L("notify.sound.betterAndroid"), color = Theme.c.muted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Choice(title: String, on: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
        RadioButton(on, onClick, colors = RadioButtonDefaults.colors(selectedColor = Theme.c.accent))
    }
}
