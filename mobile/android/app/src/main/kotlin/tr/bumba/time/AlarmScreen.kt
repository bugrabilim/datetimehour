package tr.bumba.time

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.outlined.Alarm
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun AlarmScreen(onGeo: () -> Unit = {}) {
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
    // İzin öncesi kısa açıklama (2. analiz #80): bu ekranda bir kez
    var primer by remember { mutableStateOf(false) }
    var primed by remember { mutableStateOf(false) }

    fun update(list: List<AlarmItem>) {
        items = list.sortedWith(compareBy({ it.hour }, { it.minute }))
        AlarmItem.save(items)
        AlarmScheduler.sync(context, items)
        // Bildirim izni ilk alarm eklenirken istenir (Android 13+)
        if (!notify && !primed && Build.VERSION.SDK_INT >= 33 && items.any { it.enabled }) { primed = true; primer = true }
    }
    if (primer) PermissionPrimer(PermissionKind.ALARM, onAllow = {
        primer = false
        askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
    }, onNotNow = { primer = false })

    var quickMsg by remember { mutableStateOf("") }
    val nowMs = rememberNow().toEpochMilli()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Sıradaki alarm (2. analiz #51)
        NextAlarmCard(items, nowMs)
        // Hızlı alarm (rakip analizi #17): şu andan X dakika sonrasına bir kezlik alarm
        Text(L("alarm.quick"), color = Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (m in listOf(5, 10, 15, 20, 30, 45, 60)) {
                Box(
                    Modifier.background(Theme.c.surface2, RoundedCornerShape(50)).clickable {
                        val at = java.time.ZonedDateTime.now().plusMinutes(m.toLong())
                        update(items + AlarmItem(hour = at.hour, minute = at.minute, label = T("alarm.quickIn", m), preNotify = 0))
                        quickMsg = T("alarm.quickSet", Fmt.time(at))
                        BadgeStore.hit(Badge.ALARM)
                    }.padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Text(T("alarm.quickIn", m), color = Theme.c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (quickMsg.isNotEmpty()) Text(quickMsg, color = Theme.c.accent, fontSize = 13.sp)
        // Takvimdeki etkinliğe alarm ve konuma bağlı hatırlatıcı (rakip analizi listesi #93)
        var calLead by remember { mutableStateOf(Prefs.store.getInt(CalendarAlarm.LEAD_KEY, 15)) }
        var pickingEvent by remember { mutableStateOf(false) }
        AppCard(padding = 12) {
            ChoiceRow(L("alarm.calLead"), if (calLead == 0) L("alarm.calLead.zero") else T("alarm.preMin", calLead),
                CalendarAlarm.LEADS.map { it to if (it == 0) L("alarm.calLead.zero") else T("alarm.preMin", it) }) {
                calLead = it
                Prefs.store.edit().putInt(CalendarAlarm.LEAD_KEY, it).apply()
            }
            Text("📅 " + L("alarm.fromCalendar"), Modifier.fillMaxWidth().clickable {
                DeviceCalendar.reload(context)
                pickingEvent = true
            }.padding(vertical = 8.dp), color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("📍 " + L("geo.title"), Modifier.fillMaxWidth().clickable { onGeo() }.padding(vertical = 8.dp),
                color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(L("alarm.fromCalendarNote"), color = Theme.c.muted, fontSize = 12.sp)
        }
        if (pickingEvent) {
            CalendarEventDialog(onDismiss = { pickingEvent = false }) { title, start ->
                pickingEvent = false
                val item = CalendarAlarm.item(title, start, calLead)
                if (item == null) quickMsg = L("alarm.fromCalendarPast")
                else {
                    update(items + item)
                    quickMsg = T("alarm.quickSet", Fmt.longDate(start.toLocalDate()) + " " + item.timeText)
                    BadgeStore.hit(Badge.ALARM)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("tools.alarm.listHeading"), Modifier.weight(1f), color = Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = {
                val now = LocalTime.now()
                editing = AlarmItem(hour = now.hour, minute = now.minute)
            }) { Icon(Icons.Filled.Add, L("tools.alarm.add"), tint = Theme.c.accent) }
        }
        // Uyku planının kalkış alarmı ayrı başlıkta (2. analiz #52)
        val sleepItems = items.filter { it.sleepPlan }
        val list = items.filter { !it.sleepPlan }
        for ((heading, group) in listOf(L("alarm.sleepSection") to sleepItems, "" to list)) {
        if (heading.isNotEmpty() && group.isEmpty()) continue
        if (heading.isNotEmpty()) Text(heading, color = Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        AppCard(padding = 4) {
            // Boş durum (2. analiz #96)
            if (group.isEmpty()) EmptyState(
                Icons.Outlined.Alarm, L("tools.alarm.none").trimEnd('.', '。', '۔'), L("empty.alarm.text"),
                action = L("tools.alarm.add"),
            ) { val now = LocalTime.now(); editing = AlarmItem(hour = now.hour, minute = now.minute) }
            // Tablet ve katlanırda iki sütun (2. analiz #145)
            val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
            val rows = if (wide) group.chunked(2) else group.map { listOf(it) }
            rows.forEachIndexed { i, pair ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    for (item in pair) Box(Modifier.weight(1f)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { editing = item }.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Ltr {
                                    Text(item.timeText, color = if (item.enabled) Theme.c.text else Theme.c.muted, fontSize = 40.sp,
                                        fontWeight = FontWeight.Light, style = Tnum)
                                }
                                // Görev simgesi (2. analiz #51)
                                val icon = when (item.effectiveTask) {
                                    "math" -> "🧮 "; "shake" -> "📳 "; "steps" -> "🚶 "; "code" -> "🔳 "; "memory" -> "🧠 "; "typing" -> "⌨️ "; "photo" -> "📷 "
                                    "light" -> "💡 "; "random" -> "🎲 "
                                    else -> ""
                                } + (if (item.chain.isNotEmpty()) "+${item.chain.size} " else "")
                                // Şehir saatine ya da güneşe bağlı alarm (2. analiz #132, #153)
                                item.anchorText?.let { Text((if (item.sun.isNotEmpty()) "☀️ " else "🌐 ") + it, color = Theme.c.accent, fontSize = 13.sp) }
                                Text(icon + item.title + " · " + item.repeatText + (if (item.link.isNotEmpty()) " · ▶ " + (if (MusicLink.kind(item.link) == "spotify") "Spotify" else "YouTube") else "") +
                                    (if (item.smart > 0) " · " + L("alarm.smart") else "") + (if (item.preAlarm > 0) " · 🌧" else ""), color = Theme.c.muted, fontSize = 14.sp)
                                if (item.sleepPlan) Text(L("alarm.sleepLinked"), color = Theme.c.muted, fontSize = 12.sp)
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
                    }
                    if (wide && pair.size == 1) Spacer(Modifier.weight(1f))
                }
                if (i < rows.lastIndex) HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            }
        }
        }
        // Tatil modu (2. analiz #44)
        VacationCard()
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
                color = Theme.c.err, fontSize = 14.sp,
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
    // Esnek tekrar: belirli tarih, iki haftada bir, bitiş tarihi
    var onDate by remember { mutableStateOf(item.date) }
    var every by remember { mutableStateOf(item.every) }
    var anchor by remember { mutableStateOf(item.anchor) }
    var until by remember { mutableStateOf(item.until) }
    var pickDate by remember { mutableStateOf(0) }
    var snooze by remember { mutableStateOf(item.snooze) }
    var sound by remember { mutableStateOf(item.sound) }
    var vibrate by remember { mutableStateOf(item.vibrate) }
    var snoozeMax by remember { mutableStateOf(item.snoozeMax) }
    var rise by remember { mutableStateOf(item.rise) }
    var skipHolidays by remember { mutableStateOf(item.skipHolidays) }
    var sunrise by remember { mutableStateOf(item.sunrise) }
    var task by remember { mutableStateOf(item.task) }
    var taskLevel by remember { mutableStateOf(item.taskLevel) }
    var code by remember { mutableStateOf(item.code) }
    // Fotoğraf görevi: kayıtlı yerin özeti (fotoğraf saklanmaz)
    val takeRef = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) code = PhotoHash.encode(PhotoHash.of(bmp))
    }
    var wakeCheck by remember { mutableStateOf(item.wakeCheck) }
    var preNotify by remember { mutableStateOf(item.preNotify) }
    var speak by remember { mutableStateOf(item.speak) }
    // Müzik bağlantısı (Spotify/YouTube) ve akıllı alarm penceresi (31 Ekim 2026)
    var link by remember { mutableStateOf(item.link) }
    var smart by remember { mutableStateOf(item.smart) }
    var shakeSnooze by remember { mutableStateOf(item.shakeSnooze) }
    var preAlarm by remember { mutableStateOf(item.preAlarm) }
    var openApp by remember { mutableStateOf(item.openApp) }
    var wall by remember { mutableStateOf(item.wall) }
    var headset by remember { mutableStateOf(item.headset) }
    // 2. analiz #40, #132, #153, #34, #39
    var snoozeShrink by remember { mutableStateOf(item.snoozeShrink) }
    var snoozeEnd by remember { mutableStateOf(item.snoozeEnd) }
    var volume by remember { mutableStateOf(item.volume) }
    var tz by remember { mutableStateOf(item.tz) }
    var sun by remember { mutableStateOf(item.sun) }
    var sunOffset by remember { mutableStateOf(item.sunOffset) }
    var chain by remember { mutableStateOf(item.chain) }
    var snoozeTask by remember { mutableStateOf(item.snoozeTask) }
    var keyAction by remember { mutableStateOf(item.keyAction) }
    var powerAction by remember { mutableStateOf(item.powerAction) }
    var tryTask by remember { mutableStateOf(false) }
    val linkNorm = MusicLink.normalize(link)
    // Zil sesi seçici (sistem penceresi); "Sessiz" seçilirse varsayılana dönülür
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == android.app.Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            sound = uri?.takeIf { it != RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) }?.toString() ?: ""
        }
    }
    var advanced by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L(if (isNew) "alarm.new" else "tools.alarm.timeLabel")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sun.isEmpty()) Ltr {
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
                if (days.isEmpty()) {
                    FlexRow(L("alarm.onDate"), onDate.isNotEmpty(), AlarmItem.parse(onDate)?.let { Fmt.dayMonth(it) }, { pickDate = 1 }) { on ->
                        onDate = if (on) java.time.LocalDate.now().plusDays(1).toString() else ""
                    }
                } else {
                    FlexRow(L("alarm.biweekly"), every > 1, null, null) { on ->
                        every = if (on) 2 else 1
                        if (on && anchor.isEmpty()) anchor = java.time.LocalDate.now().let { it.minusDays(it.dayOfWeek.value - 1L) }.toString()
                    }
                    FlexRow(L("alarm.untilToggle"), until.isNotEmpty(), AlarmItem.parse(until)?.let { Fmt.dayMonth(it) }, { pickDate = 2 }) { on ->
                        until = if (on) java.time.LocalDate.now().plusDays(30).toString() else ""
                    }
                }
                Text(item.copy(days = days, date = onDate, every = every, anchor = anchor, until = until).repeatText, color = Theme.c.muted, fontSize = 13.sp)
                // Yalnız tekrarlayan alarmda: Türkiye'deki tam gün resmî tatillerde çalmaz
                if (days.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(L("alarm.skipHolidays"), color = Theme.c.text, fontSize = 15.sp)
                        Text(L("alarm.skipHolidaysNote"), color = Theme.c.muted, fontSize = 12.sp)
                    }
                    Switch(skipHolidays, { skipHolidays = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                ChoiceRow(L("alarm.snooze"), if (snooze == 0) L("alarm.snoozeOff") else T("alarm.snoozeMin", snooze),
                    AlarmItem.SNOOZE_CHOICES.map { it to (if (it == 0) L("alarm.snoozeOff") else T("alarm.snoozeMin", it)) }) { snooze = it }
                // Ses düzeyi (2. analiz #40): alarm ses düzeyinin yüzdesi
                fun volText(v: Int) = if (v == 0) L("alarm.volume.phone") else Fmt.percent(v)
                ChoiceRow(L("alarm.volume"), volText(volume), AlarmItem.VOLUME_CHOICES.map { it to volText(it) }) { volume = it }
                Text(L("alarm.volumeNote"), color = Theme.c.muted, fontSize = 12.sp)
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
                // Uygulamanın ses kitaplığı (2. analiz #3): telifsiz, kategorili sesler
                var toneLibrary by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().clickable { toneLibrary = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(L("tone.library"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Text(AlarmTones.id(sound)?.let { L("tone.$it") } ?: "›", color = Theme.c.accent, fontSize = 15.sp, maxLines = 1)
                }
                if (toneLibrary) AlarmToneDialog(sound, { sound = it; toneLibrary = false }, { toneLibrary = false })
                // Alarm ekranı duvar kâğıdı (2. analiz #2)
                var wallPicker by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().clickable { wallPicker = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(L("wall.title"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Text(AlarmWalls.title(wall), color = Theme.c.accent, fontSize = 15.sp, maxLines = 1)
                }
                if (wallPicker) AlarmWallDialog(wall, time.hour, time.minute, { wall = it; wallPicker = false }, { wallPicker = false })
                // Kulaklıkta çal (2. analiz #79): bağlantı kesilirse telefonda çalar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(L("alarm.headset"), color = Theme.c.text, fontSize = 15.sp)
                        Text(L("alarm.headsetNote"), color = Theme.c.muted, fontSize = 12.sp)
                    }
                    Switch(headset, { headset = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("alarm.vibrate"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Switch(vibrate, { vibrate = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                // Gelişmiş (2. analiz #201): kapalı gelir; saat, günler, ses ve erteleme üstte kalır
                Row(
                    Modifier.fillMaxWidth().clickable { advanced = !advanced }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(L("alarm.advanced"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (!advanced) Text(L("alarm.advanced.note"), color = Theme.c.muted, fontSize = 12.sp)
                    }
                    Icon(if (advanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Theme.c.muted)
                }
                if (advanced) {
                    // Saat neye göre (2. analiz #132 şehir saati, #153 gün doğumu/batımı ± dk)
                    val base = when { sun == "rise" -> 2; sun == "set" -> 3; tz.isNotEmpty() -> 1; else -> 0 }
                    val baseNames = listOf("phone", "city", "rise", "set")
                    ChoiceRow(L("alarm.base"), L("alarm.base.${baseNames[base]}"), baseNames.indices.map { it to L("alarm.base.${baseNames[it]}") }) { b ->
                        sun = when (b) { 2 -> "rise"; 3 -> "set"; else -> "" }
                        tz = if (b == 1) tz.ifEmpty { AlarmItem.TZ_CHOICES.firstOrNull { it != java.time.ZoneId.systemDefault().id } ?: "Europe/London" } else ""
                    }
                    if (base == 1) {
                        val cities = remember { City.all.distinctBy { it.zone.id }.sortedBy { it.displayName } }
                        val cur = cities.indexOfFirst { it.zone.id == tz }
                        ChoiceRow(L("alarm.city"), cities.getOrNull(cur)?.displayName ?: tz, cities.indices.map { it to cities[it].displayName }) { tz = cities[it].zone.id }
                        Text(L("alarm.tzNote"), color = Theme.c.muted, fontSize = 12.sp)
                    }
                    if (base >= 2) {
                        ChoiceRow(L("alarm.sunOffset"), AlarmItem.sunOffsetText(sunOffset), AlarmItem.SUN_OFFSETS.map { it to AlarmItem.sunOffsetText(it) }) { sunOffset = it }
                        val probe = item.copy(days = days, date = onDate, every = every, anchor = anchor, until = until, sun = sun, sunOffset = sunOffset, tz = "", skipUntil = 0)
                        val next = probe.nextAt()
                        Text(T("alarm.sunNote", AppPlace.current().name,
                            if (next == AlarmItem.NEVER) "—" else Fmt.dayMonth(java.time.Instant.ofEpochMilli(next).atZone(java.time.ZoneId.systemDefault()).toLocalDate()) + " " +
                                Fmt.time(java.time.Instant.ofEpochMilli(next).atZone(java.time.ZoneId.systemDefault()))),
                            color = Theme.c.muted, fontSize = 12.sp)
                    }
                    if (snooze > 0) {
                        fun maxText(n: Int) = if (n == 0) L("alarm.snoozeMax.none") else T("alarm.snoozeMax.n", n)
                        ChoiceRow(L("alarm.snoozeMax"), maxText(snoozeMax), AlarmItem.SNOOZE_MAX_CHOICES.map { it to maxText(it) }) { snoozeMax = it }
                        // 2. analiz #40: her ertelemede kısalan süre, belli saatten sonra erteleme yok
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(L("alarm.snoozeShrink"), color = Theme.c.text, fontSize = 15.sp)
                                Text(L("alarm.snoozeShrinkNote"), color = Theme.c.muted, fontSize = 12.sp)
                            }
                            Switch(snoozeShrink, { snoozeShrink = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                        }
                        val ends = remember(time.hour, time.minute) {
                            listOf("") + listOf(15, 30, 45, 60, 90, 120).map {
                                java.time.LocalTime.of(time.hour, time.minute).plusMinutes(it.toLong()).toString()
                            }
                        }
                        val endChoices = (if (snoozeEnd !in ends) ends + snoozeEnd else ends)
                        fun endText(e: String) = if (e.isEmpty()) L("alarm.snoozeEnd.none") else e
                        ChoiceRow(L("alarm.snoozeEnd"), endText(snoozeEnd), endChoices.indices.map { it to endText(endChoices[it]) }) { snoozeEnd = endChoices[it] }
                        // 2. analiz #39: ertelemek için görev
                        val snoozeKinds = listOf("none", "math", "memory", "typing", "shake", "random")
                        ChoiceRow(L("alarm.snoozeTask"), L("alarm.task.$snoozeTask"), snoozeKinds.indices.map { it to L("alarm.task.${snoozeKinds[it]}") }) { snoozeTask = snoozeKinds[it] }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(L("alarm.rise"), color = Theme.c.text, fontSize = 15.sp)
                            Text(L("alarm.riseNote"), color = Theme.c.muted, fontSize = 12.sp)
                        }
                        Switch(rise, { rise = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                    }
                    // Müzik bağlantısı: çalınca Spotify ya da YouTube'da açılır, alarm sesi 1 dk sonra (MusicLink)
                    OutlinedTextField(link, { link = it.take(300) }, Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(L("alarm.link")) }, placeholder = { Text(L("alarm.link.placeholder")) },
                        isError = linkNorm == null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri))
                    Text(L(if (linkNorm == null) "alarm.link.invalid" else "alarm.link.note"),
                        color = if (linkNorm == null) Theme.c.err else Theme.c.muted, fontSize = 12.sp)
                    if (!linkNorm.isNullOrEmpty()) TextButton(onClick = { MusicLink.open(context, linkNorm) }) {
                        Text("▶ " + L("alarm.link.try"), color = Theme.c.accent)
                    }
                    // Akıllı alarm (SmartSleep): Uyku ekranındaki oturum açıkken pencerede hafif uyku algılanırsa erken çalar
                    fun smartText(m: Int) = if (m == 0) L("alarm.snoozeOff") else T("alarm.smartMin", m)
                    ChoiceRow(L("alarm.smart"), smartText(smart), AlarmItem.SMART_CHOICES.map { it to smartText(it) }) { smart = it }
                    Text(L("alarm.smart.note"), color = Theme.c.muted, fontSize = 12.sp)
                    // Saati sesli söyleme (rakip analizi #20) ve yaklaşan alarm bildirimi (#19)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(L("alarm.speak"), color = Theme.c.text, fontSize = 15.sp)
                            Text(L("alarm.speakNote"), color = Theme.c.muted, fontSize = 12.sp)
                        }
                        Switch(speak, { speak = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                    }
                    fun preText(m: Int) = if (m == 0) L("alarm.preOff") else T("alarm.preMin", m)
                    ChoiceRow(L("alarm.pre"), preText(preNotify), AlarmItem.PRE_NOTIFY_CHOICES.map { it to preText(it) }) { preNotify = it }
                    Text(L("alarm.preNote"), color = Theme.c.muted, fontSize = 12.sp)
                    // Sakin ön alarm (2. analiz #42), sallayınca ertele (#41), kapanınca uygulama aç (#43)
                    ChoiceRow(L("alarm.preAlarm"), preText(preAlarm), AlarmItem.PRE_ALARM_CHOICES.map { it to preText(it) }) { preAlarm = it }
                    Text(L("alarm.preAlarmNote"), color = Theme.c.muted, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L("alarm.shakeSnooze"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                        Switch(shakeSnooze, { shakeSnooze = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                    }
                    // Ses tuşu ve güç tuşu (2. analiz #39)
                    val keyNames = AlarmItem.KEY_ACTIONS
                    ChoiceRow(L("alarm.keys"), L("alarm.key.$keyAction"), keyNames.indices.map { it to L("alarm.key.${keyNames[it]}") }) { keyAction = keyNames[it] }
                    ChoiceRow(L("alarm.power"), L("alarm.key.$powerAction"), keyNames.indices.map { it to L("alarm.key.${keyNames[it]}") }) { powerAction = keyNames[it] }
                    Text(L("alarm.keysNote"), color = Theme.c.muted, fontSize = 12.sp)
                    OpenAppRow(openApp) { openApp = it }
                    // Gün doğumu ışığı (rakip analizi #17): tam ekran saat açıkken alarmdan önce ekran yavaşça aydınlanır
                    fun sunriseText(m: Int) = if (m == 0) L("alarm.snoozeOff") else T("alarm.sunriseMin", m)
                    ChoiceRow(L("alarm.sunrise"), sunriseText(sunrise), AlarmItem.SUNRISE_CHOICES.map { it to sunriseText(it) }) { sunrise = it }
                    Text(L("alarm.sunriseNote"), color = Theme.c.muted, fontSize = 12.sp)
                    // Kapatma görevi ve uyanma denetimi (rakip analizi #13)
                    val hasLight = remember {
                        (context.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager)
                            ?.getDefaultSensor(android.hardware.Sensor.TYPE_LIGHT) != null
                    }
                    val kinds = tr.bumba.time.core.WakeTask.KINDS + (if (hasLight) listOf("light") else emptyList()) + "random"
                    ChoiceRow(L("alarm.task"), L("alarm.task.$task"), kinds.indices.map { it to L("alarm.task.${kinds[it]}") }) { task = kinds[it] }
                    // Görev zinciri (2. analiz #34): asıl görevden sonra en çok iki görev daha
                    if (task != "none") {
                        val chainKinds = kinds
                        for (i in 0..1) {
                            if (i > 0 && chain.getOrNull(0).let { it == null || it == "none" }) break
                            val cur = chain.getOrNull(i) ?: "none"
                            ChoiceRow(L("alarm.chain"), L("alarm.task.$cur"), chainKinds.indices.map { it to L("alarm.task.${chainKinds[it]}") }) { k ->
                                val list = chain.toMutableList()
                                while (list.size <= i) list += "none"
                                list[i] = chainKinds[k]
                                chain = list.take(if (list[0] == "none") 0 else 2).filterIndexed { j, t -> j == 0 || t != "none" }
                            }
                        }
                        Text(L("alarm.chainNote"), color = Theme.c.muted, fontSize = 12.sp)
                        // Önizleme (2. analiz #35): alarm çalmadan görevi dene
                        TextButton(onClick = { tryTask = true }) { Text("▶ " + L("alarm.taskTry"), color = Theme.c.accent) }
                    }
                    if (task == "math") {
                        ChoiceRow(L("alarm.task.level"), L("alarm.task.level.$taskLevel"), (1..3).map { it to L("alarm.task.level.$it") }) { taskLevel = it }
                    }
                    if (task == "code") {
                        TextButton(onClick = { CodeScan.scan(context, onCode = { code = it }, onFail = {}) }) {
                            Text(L(if (code.isEmpty()) "alarm.code.save" else "alarm.code.change"), color = Theme.c.accent)
                        }
                        Text(L(if (code.isEmpty()) "alarm.code.missing" else "alarm.code.saved"), color = Theme.c.muted, fontSize = 12.sp)
                        Text(L("alarm.code.hint"), color = Theme.c.muted, fontSize = 12.sp)
                    }
                    if (task == "memory") {
                        ChoiceRow(L("alarm.task.level"), L("alarm.task.level.$taskLevel"), (1..3).map { it to L("alarm.task.level.$it") }) { taskLevel = it }
                    }
                    if (task == "photo") {
                        val hasPhoto = code.startsWith(PhotoHash.PREFIX)
                        TextButton(onClick = { runCatching { takeRef.launch(null) } }) {
                            Text(L(if (hasPhoto) "alarm.photo.change" else "alarm.photo.save"), color = Theme.c.accent)
                        }
                        Text(L(if (hasPhoto) "alarm.photo.saved" else "alarm.photo.missing"), color = Theme.c.muted, fontSize = 12.sp)
                    }
                    if (task != "none") Text(L("alarm.task.note"), color = Theme.c.muted, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(L("alarm.wakeCheck"), color = Theme.c.text, fontSize = 15.sp)
                            Text(L("alarm.wakeCheck.note"), color = Theme.c.muted, fontSize = 12.sp)
                        }
                        Switch(wakeCheck, { wakeCheck = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                    }
                }
                if (!isNew) TextButton(onClick = onDelete) { Text(L("tools.alarm.delete"), color = Theme.c.err) }
            }
        },
        confirmButton = {
            TextButton(enabled = linkNorm != null, onClick = {
                onSave(item.copy(hour = time.hour, minute = time.minute, label = label.trim(), days = days, enabled = true,
                    link = linkNorm ?: "", smart = smart, shakeSnooze = shakeSnooze, preAlarm = preAlarm, openApp = openApp, wall = wall, headset = headset,
                    snooze = snooze, skipUntil = 0, sound = sound, vibrate = vibrate, snoozeMax = snoozeMax, rise = rise,
                    skipHolidays = skipHolidays && days.isNotEmpty(), sunrise = sunrise, task = task, taskLevel = taskLevel,
                    code = code, wakeCheck = wakeCheck, preNotify = preNotify, speak = speak,
                    date = if (days.isEmpty()) onDate else "", every = if (days.isEmpty()) 1 else every, anchor = anchor,
                    until = if (days.isEmpty()) "" else until,
                    snoozeShrink = snoozeShrink, snoozeEnd = snoozeEnd, volume = volume, tz = if (sun.isEmpty()) tz else "", sun = sun,
                    sunOffset = sunOffset, chain = if (task == "none") emptyList() else chain.filter { it != "none" },
                    snoozeTask = snoozeTask, keyAction = keyAction, powerAction = powerAction))
            }) { Text(L("alarm.save"), color = Theme.c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
    )
    if (pickDate > 0) {
        val current = AlarmItem.parse(if (pickDate == 1) onDate else until) ?: java.time.LocalDate.now()
        val state = androidx.compose.material3.rememberDatePickerState(current.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { pickDate = 0 },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                        if (pickDate == 1) onDate = d else until = d
                    }
                    pickDate = 0
                }) { Text(L("common.done"), color = Theme.c.accent) }
            },
        ) { androidx.compose.material3.DatePicker(state) }
    }
    // Görev önizlemesi (2. analiz #35): alarm çalmadan, ses olmadan
    if (tryTask) {
        var done by remember { mutableStateOf(false) }
        val probe = item.copy(task = task, taskLevel = taskLevel, code = code, chain = chain.filter { it != "none" })
        AlertDialog(
            onDismissRequest = { tryTask = false },
            title = { Text(L("alarm.taskTry")) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (done) Text(L("alarm.taskTryDone"), color = Theme.c.text, fontSize = 16.sp)
                    else WakeTaskPanel(probe) { done = true }
                }
            },
            confirmButton = { TextButton(onClick = { tryTask = false }) { Text(L(if (done) "common.done" else "alarm.cancel"), color = Theme.c.accent) } },
        )
    }
}

/** Başlık + seçili değer; dokununca açılır liste */
@Composable
internal fun ChoiceRow(title: String, value: String, options: List<Pair<Int, String>>, onPick: (Int) -> Unit) {
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

/** Esnek tekrar satırı: anahtar ve (açıksa) dokununca tarih seçilen değer */
@Composable
private fun FlexRow(title: String, on: Boolean, value: String?, onPick: (() -> Unit)?, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        if (on && value != null && onPick != null) TextButton(onClick = onPick) { Text(value, color = Theme.c.accent, fontSize = 15.sp) }
        androidx.compose.material3.Switch(on, onToggle, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
    }
}
