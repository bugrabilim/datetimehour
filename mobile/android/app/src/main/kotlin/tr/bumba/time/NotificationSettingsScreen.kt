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
            // Bildirim alanında ve kilit ekranında sıradaki vakit (iOS'taki Live Activity karşılığı; 2. analiz #199 ile Vakitler'den buraya)
            var liveOn by remember { mutableStateOf(Prefs.store.getBoolean(LiveNotifications.PRAYER_KEY, false)) }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("live.prayer.toggle"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                    Switch(liveOn, { on ->
                        liveOn = on
                        Prefs.store.edit().putBoolean(LiveNotifications.PRAYER_KEY, on).apply()
                        LiveNotifications.sync(context)
                    }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                Text(L("live.prayer.noteAndroid"), color = Theme.c.muted, fontSize = 13.sp)
            }
            PrayerAlarmCard()
            PrayerSilentCard()
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
                PerPrayerCard(changed = { changed() })
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
                    if (NotifyPrefs.sound == "adhan") {
                        SectionTitle(L("notify.adhanVoice"))
                        for (v in NotifyPrefs.ADHAN_VOICES) {
                            Choice(L("notify.adhan.$v"), NotifyPrefs.adhanVoice == v) {
                                NotifyPrefs.saveAdhanVoice(v)
                                changed()
                            }
                        }
                    }
                    TextButton(onClick = { scope.launch { NotificationSounds.preview(context, NotifyPrefs.sound, NotifyPrefs.lead) } }) {
                        Icon(Icons.Filled.PlayCircle, null, tint = Theme.c.accent)
                        Text("  " + L("notify.sound.preview"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                    }
                    Text(L("notify.sound.note"), color = Theme.c.muted, fontSize = 13.sp)
                    if (NotifyPrefs.sound == "adhan") {
                        Text(L("notify.sound.adhanNote"), color = Theme.c.muted, fontSize = 13.sp)
                        val credit = if (NotifyPrefs.adhanVoice == "azeez") "notify.sound.adhanCredit" else "notify.adhan.credit.${NotifyPrefs.adhanVoice}"
                        Text(L(credit), color = Theme.c.muted, fontSize = 11.sp)
                    }
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

/** Vakte göre ayar (2. analiz #54): her vakit için ayrı önceden bildirim süresi, ses ve günler; boşsa genel ayar */
@Composable
private fun PerPrayerCard(changed: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = NotifyPrefs.perVersion
    var open by remember { mutableStateOf<Prayer?>(null) }
    AppCard {
        SectionTitle(L("notify.perPrayer"))
        Text(L("notify.perPrayer.note"), color = Theme.c.muted, fontSize = 13.sp)
        for (p in NotifyPrefs.prayers.sortedBy { it.ordinal }) {
            val lead = NotifyPrefs.leadFor(p)
            val snd = NotifyPrefs.rawSoundFor(p)
            val days = NotifyPrefs.days(p)
            val custom = lead >= 0 || snd.isNotEmpty() || days.isNotEmpty()
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { open = if (open == p) null else p },
                verticalAlignment = Alignment.CenterVertically) {
                Text(p.title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                Text(if (custom) L("notify.perPrayer.custom") else L("notify.perPrayer.general"),
                    color = if (custom) Theme.c.accent else Theme.c.muted, fontSize = 13.sp)
            }
            if (open == p) {
                val leads = listOf(-1) + PrayerAlerts.LEAD_CHOICES
                ChoiceRow(L("notify.lead"), leadLabel(lead), leads.mapIndexed { i, m -> i to leadLabel(m) }) { i ->
                    NotifyPrefs.saveLeadFor(p, leads[i]); changed()
                }
                val sounds = listOf("") + NotifyPrefs.SOUNDS
                ChoiceRow(L("notify.sound"), soundLabel(snd), sounds.mapIndexed { i, s -> i to soundLabel(s) }) { i ->
                    NotifyPrefs.saveSoundFor(p, sounds[i]); changed()
                }
                Text(L("notify.perPrayer.days"), color = Theme.c.muted, fontSize = 13.sp)
                Ltr {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (d in 1..7) {
                            val on = d in days
                            androidx.compose.foundation.layout.Box(
                                Modifier.weight(1f).heightIn(min = 34.dp)
                                    .then(Modifier.clickable { NotifyPrefs.saveDays(p, if (on) days - d else days + d); changed() })
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(L("tools.alarm.dayNames.${d - 1}"), color = if (on) Theme.c.accent else Theme.c.muted,
                                    fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun leadLabel(m: Int): String = when {
    m < 0 -> L("notify.perPrayer.general")
    m == 0 -> L("notify.lead.zero")
    else -> T("notify.lead.minutes", m)
}

private fun soundLabel(s: String): String = if (s.isEmpty()) L("notify.perPrayer.general") else L("notify.sound.$s")
