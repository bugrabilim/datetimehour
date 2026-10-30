package tr.bumba.time

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Kişisel geri sayım listesi (30 Ekim 2026, rakip analizi #15), iOS'taki Countdowns.swift ile aynı kayıt: "countdowns"
 * (JSON; hedef Unix ms). Simge (emoji), her yıl tekrar (doğum günü, yıl dönümü), hatırlatma (bir gün önce 09.00 ve zamanı
 * gelince; özellik bildirimleriyle birlikte kurulur), telefon takviminden seçme. Eski tek hedef (cd.target) bir kez taşınır.
 */
data class CountdownItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "⏳",
    val target: Long,
    val yearly: Boolean = false,
    val remind: Boolean = true,
    /** Fotoğrafın sürümü (0 = yok); dosya "cd-<id>.jpg" (yalnız telefonda) */
    val photo: Long = 0,
) {
    val photoName: String get() = "cd-$id.jpg"

    /** Yıllık olanda hedef günü geçtiyse sıradaki yıl (hedef günü boyunca "Bugün!") */
    fun effective(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Instant {
        var t = Instant.ofEpochMilli(target).atZone(zone)
        if (yearly) while (t.toLocalDate().isBefore(now.atZone(zone).toLocalDate())) t = t.plusYears(1)
        return t.toInstant()
    }

    val label: String get() = "$emoji $title".trim()

    fun toJson(): JSONObject = JSONObject().put("id", id).put("title", title).put("emoji", emoji).put("target", target)
        .put("yearly", yearly).put("remind", remind).put("photo", photo)

    companion object {
        private const val KEY = "countdowns"
        val EMOJIS = listOf("⏳", "🎂", "🎉", "✈️", "🏖️", "🎓", "💍", "❤️", "🎄", "🌙", "⚽", "📚", "💼", "🏥", "👶", "🎁")

        /** Kilitli: bildirim planı arka planda da okur; eski kaydın taşınması iki kez yapılmasın */
        @Synchronized
        fun load(): List<CountdownItem> {
            val sp = Prefs.store
            val list = runCatching {
                val a = JSONArray(sp.getString(KEY, "[]"))
                (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    CountdownItem(o.getString("id"), o.optString("title"), o.optString("emoji", "⏳"), o.getLong("target"),
                        o.optBoolean("yearly"), o.optBoolean("remind", true), o.optLong("photo", 0))
                }
            }.getOrDefault(emptyList())
            val old = sp.getLong("cd.target", 0L)
            if (old > 0) {
                val migrated = list + CountdownItem(title = sp.getString("cd.title", "")!!, target = old)
                sp.edit().remove("cd.target").remove("cd.title").apply()
                save(migrated)
                return migrated
            }
            return list
        }

        fun save(items: List<CountdownItem>) {
            Prefs.store.edit().putString(KEY, JSONArray(items.map { it.toJson() }).toString()).apply()
        }

        /** En yakın gelecek hedef (widget) */
        fun nearest(now: Instant = Instant.now()): CountdownItem? =
            load().filter { !it.effective(now).isBefore(now) }.minByOrNull { it.effective(now) }

        /** Hatırlatmalar: bir gün önce 09.00 ve hedef anında (60 gün içinde) */
        fun reminders(now: Instant): List<FeatureNotifier.Planned> {
            val zone = ZoneId.systemDefault()
            return load().filter { it.remind }.flatMap { c ->
                val at = c.effective(now)
                if (at.isAfter(now.plus(60, ChronoUnit.DAYS))) return@flatMap emptyList()
                val dayBefore = at.atZone(zone).toLocalDate().minusDays(1).atTime(9, 0).atZone(zone).toInstant()
                listOf(
                    FeatureNotifier.Planned("cd-${c.id}-pre-${at.toEpochMilli()}", dayBefore, T("countdowns.remindTomorrow", c.label),
                        Fmt.longDate(at.atZone(zone).toLocalDate()), Tab.TOOLS),
                    FeatureNotifier.Planned("cd-${c.id}-now-${at.toEpochMilli()}", at, T("countdowns.remindNow", c.label), "", Tab.TOOLS),
                ).filter { it.at.isAfter(now) }
            }
        }
    }
}

/** Kalan: "Bugün!", son 24 saatte SS:DD:ss, değilse "N gün kaldı" */
fun countdownText(at: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val days = ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), at.atZone(zone).toLocalDate())
    val left = (at.toEpochMilli() - now.toEpochMilli() + 999) / 1000
    return when {
        left <= 0 && days == 0L -> L("countdowns.today")
        left <= 0 -> L("tools.countdown.finishedDate")
        left < 86_400 -> Fmt.countdown(left)
        else -> T("client.daysLeft", days)
    }
}

/** Geri sayımlarım: liste, + ile yeni, dokununca düzenle; altta hazır özel günler */
@Composable
fun CountdownsScreen() {
    val context = LocalContext.current
    var items by remember { mutableStateOf(CountdownItem.load()) }
    var editing by remember { mutableStateOf<CountdownItem?>(null) }
    val now = rememberNow()
    val today = LocalDate.now()
    fun update(list: List<CountdownItem>) {
        // Silinen ya da fotoğrafı kaldırılan geri sayımların dosyaları silinir
        for (old in items) if (list.none { it.id == old.id && it.photo != 0L }) PhotoFile.remove(context, old.photoName)
        items = list
        CountdownItem.save(list)
        WidgetUpdater.updateAll(context)
        PrayerNotifier.reschedule(context) // hatırlatmalar özellik bildirimleriyle birlikte kurulur
    }
    val sorted = items.sortedBy { it.effective(now) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(L("countdowns.heading"), Modifier.weight(1f))
            IconButton(onClick = {
                val d = LocalDate.now().plusDays(7).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                editing = CountdownItem(title = "", target = d)
            }) { Icon(Icons.Filled.Add, L("countdowns.add"), tint = Theme.c.accent) }
        }
        if (sorted.isEmpty()) Text(L("countdowns.empty"), color = Theme.c.muted, fontSize = 14.sp)
        for (c in sorted) {
            val at = c.effective(now)
            // Fotoğraflı kart: fotoğraf koyulaştırılır, yazılar koyu temanın açık renklerine geçer
            val photo = PhotoFile.image(context, c.photoName, c.photo)
            val colors = if (photo != null) AccentPalette.find(Prefs.accent).colors(true) else Theme.c
            CompositionLocalProvider(LocalColors provides colors) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
            if (photo != null) PhotoBackground(photo, Modifier.matchParentSize())
            AppCard(Modifier.clickable { editing = c }.then(if (photo != null) Modifier.heightIn(min = 120.dp) else Modifier),
                background = if (photo != null) Color.Transparent else Theme.c.surface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.emoji, fontSize = 34.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.title.ifBlank { L("tools.hub.cards.countdown.name") }, color = Theme.c.text, fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Ltr { Text(countdownText(at, now), color = Theme.c.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, style = Tnum) }
                        val local = at.atZone(ZoneId.systemDefault())
                        Text(Fmt.longDate(local.toLocalDate()) + (if (local.hour + local.minute > 0) " " + Fmt.time(local) else "") +
                            if (c.yearly) " · ↻" else "", color = Theme.c.muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { update(items.filter { it.id != c.id }) }) {
                        Icon(Icons.Filled.Close, L("timers.remove"), tint = Theme.c.muted)
                    }
                }
            }
            }
            }
        }
        SectionTitle(L("tools.countdown.quickHeading"))
        AppCard(padding = 4) {
            for ((name, day) in quickTargets(today)) {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val ms = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        if (items.none { it.title == name && it.target == ms }) update(items + CountdownItem(title = name, emoji = "🎉", target = ms))
                    }.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Text(T("client.daysLeft", ChronoUnit.DAYS.between(today, day)), color = Theme.c.muted, fontSize = 12.sp)
                    Icon(Icons.Filled.Add, null, Modifier.padding(start = 8.dp).size(18.dp), tint = Theme.c.accent)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    editing?.let { item ->
        CountdownEditor(item, isNew = items.none { it.id == item.id }, onDismiss = { editing = null }) { saved ->
            update(items.filter { it.id != saved.id } + saved)
            editing = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownEditor(item: CountdownItem, isNew: Boolean, onDismiss: () -> Unit, onSave: (CountdownItem) -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    var title by remember { mutableStateOf(item.title) }
    var emoji by remember { mutableStateOf(item.emoji) }
    var at by remember { mutableStateOf(Instant.ofEpochMilli(item.target).atZone(zone).toLocalDateTime()) }
    var yearly by remember { mutableStateOf(item.yearly) }
    var remind by remember { mutableStateOf(item.remind) }
    var photo by remember { mutableLongStateOf(item.photo) }
    var picking by remember { mutableIntStateOf(0) } // 1 tarih, 2 saat, 3 takvim
    // Fotoğraf: Android'in fotoğraf seçicisi (izin istemez), küçültülmüş kopya uygulama dosyalarına
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && PhotoFile.save(context, uri, item.photoName, 1000)) photo = System.currentTimeMillis()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L(if (isNew) "countdowns.add" else "countdowns.edit")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(L("countdowns.title")) }, placeholder = { Text(L("countdowns.titlePlaceholder")) })
                Text(L("countdowns.emoji"), color = Theme.c.muted, fontSize = 13.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (e in CountdownItem.EMOJIS) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(if (e == emoji) Theme.c.accentSoft else Theme.c.surface2)
                                .then(if (e == emoji) Modifier.border(2.dp, Theme.c.accent, CircleShape) else Modifier)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 20.sp) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { picking = 1 }) { Text(Fmt.shortDate(at.toLocalDate()), color = Theme.c.accent) }
                    TextButton(onClick = { picking = 2 }) {
                        Text(Fmt.time(at.atZone(zone)), color = Theme.c.accent)
                    }
                }
                TextButton(onClick = { DeviceCalendar.reload(context); picking = 3 }) { Text("📅 " + L("countdowns.fromCalendar"), color = Theme.c.accent) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("countdowns.yearly"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Switch(yearly, { yearly = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("countdowns.remind"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Switch(remind, { remind = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                Text(L("countdowns.photo"), color = Theme.c.muted, fontSize = 13.sp)
                PhotoFile.image(context, item.photoName, photo)?.let {
                    PhotoBackground(it, Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)))
                }
                Row {
                    TextButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(L("look.photo.pick"), color = Theme.c.accent)
                    }
                    if (photo != 0L) TextButton(onClick = { photo = 0 }) { Text(L("look.photo.remove"), color = Color(0xFFD32F2F)) }
                }
                Text(L("countdowns.photoNote"), color = Theme.c.muted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(item.copy(title = title.trim(), emoji = emoji, target = at.atZone(zone).toInstant().toEpochMilli(), yearly = yearly,
                    remind = remind, photo = photo))
            }) { Text(L("alarm.save"), color = Theme.c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
    )
    when (picking) {
        1 -> {
            val state = rememberDatePickerState(at.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            DatePickerDialog(
                onDismissRequest = { picking = 0 },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { at = LocalDateTime.of(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate(), at.toLocalTime()) }
                        picking = 0
                    }) { Text(L("common.done")) }
                },
            ) { DatePicker(state) }
        }
        2 -> {
            val state = rememberTimePickerState(at.hour, at.minute, is24Hour = !Prefs.h12)
            AlertDialog(
                onDismissRequest = { picking = 0 },
                confirmButton = { TextButton(onClick = { at = at.withHour(state.hour).withMinute(state.minute); picking = 0 }) { Text(L("common.done")) } },
                text = { Ltr { TimePicker(state) } },
            )
        }
        3 -> {
            val events = remember { upcomingDeviceEvents(context) }
            AlertDialog(
                onDismissRequest = { picking = 0 },
                confirmButton = { TextButton(onClick = { picking = 0 }) { Text(L("alarm.cancel")) } },
                title = { Text(L("countdowns.fromCalendar")) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        when {
                            !DeviceCalendar.authorized -> Text(L("countdowns.calendarDenied"), color = Theme.c.muted)
                            events.isEmpty() -> Text(L("countdowns.calendarNone"), color = Theme.c.muted)
                            else -> for ((name, start) in events) {
                                Column(Modifier.fillMaxWidth().clickable {
                                    title = name.take(40)
                                    at = start
                                    picking = 0
                                }.padding(vertical = 10.dp)) {
                                    Text(name, color = Theme.c.text, fontSize = 15.sp, maxLines = 1)
                                    Text(Fmt.longDate(start.toLocalDate()) + if (start.toLocalTime() != LocalTime.MIDNIGHT) " " + Fmt.time(start.atZone(zone)) else "",
                                        color = Theme.c.muted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

/** Telefondaki takvimlerden önümüzdeki 60 günün etkinlikleri (tüm gün olanlar yerel gece yarısında) */
private fun upcomingDeviceEvents(c: Context): List<Pair<String, LocalDateTime>> {
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
    val seen = HashSet<String>()
    return (0..2).flatMap { DeviceCalendar.events(c, YearMonth.now().plusMonths(it.toLong())).values.flatten() }
        .mapNotNull { e ->
            val start = if (e.allDay) e.begin.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay() else e.begin.atZone(zone).toLocalDateTime()
            val key = "${e.id}-$start"
            if (!seen.add(key) || start.atZone(zone).toInstant().isBefore(now) || start.atZone(zone).toInstant().isAfter(now.plus(60, ChronoUnit.DAYS))) null
            else (e.title.ifBlank { "—" } to start)
        }
        .sortedBy { it.second }
}
