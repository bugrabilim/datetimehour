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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import android.media.RingtoneManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import java.time.LocalTime

/** Alarmlar (iOS'taki AlarmsView): liste, aç/kapat, dokununca düzenle, + ile yeni; uygulama kapalıyken de çalar */
@Composable
fun AlarmScreen() {
    val context = LocalContext.current
    var items by remember { mutableStateOf(AlarmItem.load()) }
    var editing by remember { mutableStateOf<AlarmItem?>(null) }
    // İzinler sayfaya her dönüşte yeniden okunur (Ayarlar'dan izin verilip dönülebilir)
    var exact by remember { mutableStateOf(AlarmScheduler.canExact(context)) }
    var notify by remember { mutableStateOf(AlarmScheduler.canNotify(context)) }
    LifecycleResumeEffect(Unit) {
        exact = AlarmScheduler.canExact(context)
        notify = AlarmScheduler.canNotify(context)
        items = AlarmItem.load()
        onPauseOrDispose { }
    }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notify = it }

    fun update(list: List<AlarmItem>) {
        items = list.sortedWith(compareBy({ it.hour }, { it.minute }))
        AlarmItem.save(items)
        AlarmScheduler.sync(context, items)
        // Bildirim izni ilk alarm eklenirken istenir (Android 13+)
        if (!notify && Build.VERSION.SDK_INT >= 33 && items.any { it.enabled }) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("tools.alarm.listHeading"), Modifier.weight(1f), color = Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = {
                val now = LocalTime.now()
                editing = AlarmItem(hour = now.hour, minute = now.minute)
            }) { Icon(Icons.Filled.Add, L("tools.alarm.add"), tint = Theme.c.accent) }
        }
        AppCard(padding = 4) {
            if (items.isEmpty()) Text(L("tools.alarm.none"), Modifier.padding(12.dp), color = Theme.c.muted)
            items.forEachIndexed { i, item ->
                Row(
                    Modifier.fillMaxWidth().clickable { editing = item }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Ltr {
                            Text(item.timeText, color = if (item.enabled) Theme.c.text else Theme.c.muted, fontSize = 40.sp,
                                fontWeight = FontWeight.Light, style = Tnum)
                        }
                        Text(item.title + " · " + item.repeatText, color = Theme.c.muted, fontSize = 14.sp)
                        // Sonraki alarm atlanacaksa hangi gün; dokununca geri alınır, değilse "atla"
                        if (item.enabled) {
                            val skipped = item.skipping
                            val now = System.currentTimeMillis()
                            fun day(ms: Long) = Fmt.longDate(java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                            // Atlanacak çalma: tatiller hesaba katılmış, elle atlama hariç sıradaki
                            val base = item.copy(skipUntil = 0).nextAt(now)
                            if (item.holidaySkipped(now)) Text(T("alarm.holidayNext", day(item.nextAt(now))), color = Theme.c.muted, fontSize = 13.sp)
                            Text(
                                if (skipped) T("alarm.skipped", day(base)) + " · " + L("alarm.unskip")
                                else L("alarm.skip"),
                                Modifier.clickable {
                                    val skip = if (skipped) 0L else base + 60_000L
                                    update(items.map { if (it.id == item.id) it.copy(skipUntil = skip) else it })
                                }.padding(vertical = 4.dp),
                                color = if (skipped) Theme.c.muted else Theme.c.accent, fontSize = 13.sp,
                            )
                        }
                    }
                    Switch(item.enabled, { on -> update(items.map { if (it.id == item.id) it.copy(enabled = on) else it }) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                if (i < items.lastIndex) HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            }
        }
        // İzin eksikse: dokununca ilgili ayar açılır
        if (items.any { it.enabled } && (!exact || !notify)) {
            Text(
                L("alarm.denied"),
                Modifier.clickable {
                    val intent = if (!exact && Build.VERSION.SDK_INT >= 31)
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                    else Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                }.padding(4.dp),
                color = Color(0xFFD32F2F), fontSize = 14.sp,
            )
        }
        Text(L("alarm.realNote"), Modifier.padding(horizontal = 4.dp), color = Theme.c.muted, fontSize = 13.sp)
    }

    editing?.let { item ->
        AlarmEditor(
            item,
            isNew = items.none { it.id == item.id },
            onDismiss = { editing = null },
            onDelete = { update(items.filter { it.id != item.id }); editing = null },
        ) { saved ->
            update(items.filter { it.id != saved.id } + saved)
            BadgeStore.hit(Badge.ALARM)
            editing = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditor(item: AlarmItem, isNew: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (AlarmItem) -> Unit) {
    val time = rememberTimePickerState(item.hour, item.minute, is24Hour = !Prefs.h12)
    var label by remember { mutableStateOf(item.label) }
    val context = LocalContext.current
    var days by remember { mutableStateOf(item.days.sorted()) }
    var snooze by remember { mutableStateOf(item.snooze) }
    var sound by remember { mutableStateOf(item.sound) }
    var vibrate by remember { mutableStateOf(item.vibrate) }
    var snoozeMax by remember { mutableStateOf(item.snoozeMax) }
    var rise by remember { mutableStateOf(item.rise) }
    var skipHolidays by remember { mutableStateOf(item.skipHolidays) }
    // Zil sesi seçici (sistem penceresi); "Sessiz" seçilirse varsayılana dönülür
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == android.app.Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            sound = uri?.takeIf { it != RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) }?.toString() ?: ""
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L(if (isNew) "alarm.new" else "tools.alarm.timeLabel")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Ltr {
                    TimePicker(time, colors = TimePickerDefaults.colors(
                        selectorColor = Theme.c.accent,
                        timeSelectorSelectedContainerColor = Theme.c.accentSoft,
                        timeSelectorSelectedContentColor = Theme.c.accent,
                    ))
                }
                OutlinedTextField(label, { label = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(L("tools.alarm.labelLabel")) }, placeholder = { Text(L("tools.alarm.labelPlaceholder")) })
                Text(L("alarm.days"), color = Theme.c.muted, fontSize = 13.sp)
                // Pazartesiden pazara yedi düğme; hiçbiri seçili değilse bir kez çalar
                Ltr {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (d in 1..7) {
                            val on = d in days
                            Box(
                                Modifier.weight(1f).height(36.dp).clip(CircleShape)
                                    .background(if (on) Theme.c.accent else Theme.c.surface2)
                                    .clickable { days = if (on) days - d else (days + d).sorted() }
                                    .semantics { selected = on },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(L("tools.alarm.dayNames.${d - 1}"), color = if (on) Color.White else Theme.c.text,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
                Text(item.copy(days = days).repeatText, color = Theme.c.muted, fontSize = 13.sp)
                ChoiceRow(L("alarm.snooze"), if (snooze == 0) L("alarm.snoozeOff") else T("alarm.snoozeMin", snooze),
                    AlarmItem.SNOOZE_CHOICES.map { it to (if (it == 0) L("alarm.snoozeOff") else T("alarm.snoozeMin", it)) }) { snooze = it }
                if (snooze > 0) {
                    fun maxText(n: Int) = if (n == 0) L("alarm.snoozeMax.none") else T("alarm.snoozeMax.n", n)
                    ChoiceRow(L("alarm.snoozeMax"), maxText(snoozeMax), AlarmItem.SNOOZE_MAX_CHOICES.map { it to maxText(it) }) { snoozeMax = it }
                }
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val current = sound.takeIf { it.isNotEmpty() }?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        runCatching {
                            pickSound.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current))
                        }
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(L("alarm.sound"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Text(AlarmScheduler.soundTitle(context, sound), color = Theme.c.accent, fontSize = 15.sp, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("alarm.vibrate"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Switch(vibrate, { vibrate = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(L("alarm.rise"), color = Theme.c.text, fontSize = 15.sp)
                        Text(L("alarm.riseNote"), color = Theme.c.muted, fontSize = 12.sp)
                    }
                    Switch(rise, { rise = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                // Yalnız tekrarlayan alarmda: Türkiye'deki tam gün resmî tatillerde çalmaz
                if (days.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(L("alarm.skipHolidays"), color = Theme.c.text, fontSize = 15.sp)
                        Text(L("alarm.skipHolidaysNote"), color = Theme.c.muted, fontSize = 12.sp)
                    }
                    Switch(skipHolidays, { skipHolidays = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                if (!isNew) TextButton(onClick = onDelete) { Text(L("tools.alarm.delete"), color = Color(0xFFD32F2F)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(item.copy(hour = time.hour, minute = time.minute, label = label.trim(), days = days, enabled = true,
                    snooze = snooze, skipUntil = 0, sound = sound, vibrate = vibrate, snoozeMax = snoozeMax, rise = rise,
                    skipHolidays = skipHolidays && days.isNotEmpty()))
            }) { Text(L("alarm.save"), color = Theme.c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
    )
}

/** Başlık + seçili değer; dokununca açılır liste */
@Composable
private fun ChoiceRow(title: String, value: String, options: List<Pair<Int, String>>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        Box {
            Text(value, color = Theme.c.accent, fontSize = 15.sp)
            DropdownMenu(open, { open = false }) {
                for ((v, label) in options) DropdownMenuItem(text = { Text(label) }, onClick = { open = false; onPick(v) })
            }
        }
    }
}
