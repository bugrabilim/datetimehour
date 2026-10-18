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
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
                        Text(item.title + " · " + L("tools.alarm.${item.repeats}"), color = Theme.c.muted, fontSize = 14.sp)
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
            editing = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditor(item: AlarmItem, isNew: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (AlarmItem) -> Unit) {
    val time = rememberTimePickerState(item.hour, item.minute, is24Hour = !Prefs.h12)
    var label by remember { mutableStateOf(item.label) }
    var repeats by remember { mutableStateOf(item.repeats) }
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
                Text(L("tools.alarm.repeatLabel"), color = Theme.c.muted, fontSize = 13.sp)
                val opts = listOf("once", "every", "weekdays")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opts.forEachIndexed { i, o ->
                        SegmentedButton(repeats == o, { repeats = o }, SegmentedButtonDefaults.itemShape(i, opts.size), colors = segColors()) {
                            Text(L("tools.alarm.$o"), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                if (!isNew) TextButton(onClick = onDelete) { Text(L("tools.alarm.delete"), color = Color(0xFFD32F2F)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(item.copy(hour = time.hour, minute = time.minute, label = label.trim(), repeats = repeats, enabled = true))
            }) { Text(L("alarm.save"), color = Theme.c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
    )
}
