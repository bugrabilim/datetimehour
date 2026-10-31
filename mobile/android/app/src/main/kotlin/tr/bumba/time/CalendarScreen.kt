package tr.bumba.time

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Takvim (iOS'taki CalendarTab gibi): ay ızgarası (resmî tatil, arife, dini gün, özel gün, okul tatili renkleri;
 * Ramazan gölgeli), seçili günün özel günleri ve telefondaki takvimlerin etkinlikleri. "+" sistemin takvim
 * uygulamasında etkinlik ekler; etkinliğe dokununca ayrıntısı açılır, uzun basınca silinir (onaylı).
 * Veri sitedeki takvimle aynı (calendar.json). Kaydırarak ay değişir.
 */
@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val today = LocalDate.now()
    var ym by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var sel by rememberSaveable { mutableStateOf(today.toString()) }
    var guide by rememberSaveable { mutableStateOf(false) }
    var leaf by rememberSaveable { mutableStateOf(false) }
    val month = YearMonth.parse(ym)
    val selected = LocalDate.parse(sel)
    fun go(d: LocalDate) {
        ym = YearMonth.from(d).toString()
        sel = d.toString()
    }
    fun move(d: Long) {
        val m = month.plusMonths(d)
        ym = m.toString()
        sel = (if (YearMonth.from(today) == m) today else m.atDay(1)).toString()
    }
    // Saat ekranından istenen gün
    LaunchedEffect(Nav.calendarDay) {
        Nav.calendarDay?.let { go(it); Nav.calendarDay = null }
    }
    LaunchedEffect(Unit) { DeviceCalendar.reload(context) }
    if (leaf) {
        BackHandler { leaf = false }
        CalendarLeafScreen { leaf = false }
        return
    }
    if (guide) {
        BackHandler { guide = false; DeviceCalendar.reload(context) }
        Column(Modifier.fillMaxSize()) {
            SubBar(L("google.title"), onBack = { guide = false; DeviceCalendar.reload(context) })
            GoogleCalendarGuide()
        }
        return
    }
    val device = remember(ym, DeviceCalendar.version, DeviceCalendar.authorized) { DeviceCalendar.events(context, month) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Ay ve yıl alt alta (uzun ay adlı dillerde, ör. Rusça/Fransızca, düğmelerle sığsın); "Bugün" simge düğmesi
            Column(Modifier.weight(1f)) {
                Text(Fmt.month(month.monthValue), color = Theme.c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("${month.year}", color = Theme.c.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            IconButton(onClick = { go(today) }) { Icon(Icons.Filled.Today, L("client.today"), tint = Theme.c.accent) }
            IconButton(onClick = { move(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("prayer.prevMonth"), tint = Theme.c.accent) }
            IconButton(onClick = { move(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("prayer.nextMonth"), tint = Theme.c.accent) }
            IconButton(onClick = { leaf = true }) { Icon(Icons.Filled.Article, L("leaf.title"), tint = Theme.c.accent) }
            IconButton(onClick = { DeviceCalendar.add(context, selected) }) { Icon(Icons.Filled.Add, L("calendar.add"), tint = Theme.c.accent) }
        }
        MonthGrid(month, today, selected, device, Modifier.pointerInput(ym) {
            var total = 0f
            detectHorizontalDragGestures(onDragEnd = {
                if (total < -60) move(if (Strings.isRtl) -1 else 1) else if (total > 60) move(if (Strings.isRtl) 1 else -1)
                total = 0f
            }) { _, d -> total += d }
        }) {
            sel = it.toString()
            BadgeStore.hit(Badge.TAKVIM)
        }
        Spacer(Modifier.height(12.dp))
        DayDetail(selected, today, device[selected].orEmpty())
        Spacer(Modifier.height(12.dp))
        Legend()
        TextButton(onClick = { guide = true }) {
            Icon(Icons.Filled.Link, null, tint = Theme.c.accent)
            Text("  " + L("google.button"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
        }
        if (month.year in AppData.projectedYears) {
            Text(L("calendar.projected"), color = Theme.c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Text(L("hijri.note"), color = Theme.c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth, today: LocalDate, selected: LocalDate, device: Map<LocalDate, List<DeviceCalendar.Event>>,
    modifier: Modifier, onPick: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1 // Pazartesi başlar
    val cells = lead + month.lengthOfMonth()
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            for (i in 0..6) {
                Text(L("calendar.weekdaysShort.$i"), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp,
                    color = if (i >= 5) Theme.c.accent else Theme.c.muted)
            }
        }
        for (row in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val n = row * 7 + col - lead + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (n in 1..month.lengthOfMonth()) {
                            val d = month.atDay(n)
                            DayCell(d, d == today, d == selected, device[d].orEmpty().isNotEmpty()) { onPick(d) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, isToday: Boolean, isSelected: Boolean, mine: Boolean, onClick: () -> Unit) {
    val ev = AppData.events[d].orEmpty()
    val holiday = ev.any { it.type == "holiday" }
    val ramadan = AppData.isRamadan(d)
    Column(
        Modifier.fillMaxSize().padding(2.dp)
            .background(if (ramadan) AppData.color("religious").copy(alpha = 0.10f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(34.dp).background(
                when {
                    isToday -> Theme.c.accent
                    isSelected -> Theme.c.accentSoft
                    else -> Color.Transparent
                }, CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                d.dayOfMonth.toString(), fontSize = 17.sp,
                color = when {
                    isToday -> Theme.c.bg
                    holiday -> AppData.color("holiday")
                    d.dayOfWeek.value >= 6 -> Theme.c.muted
                    else -> Theme.c.text
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (t in ev.map { it.type }.distinct().take(3)) {
                Box(Modifier.size(5.dp).background(AppData.color(t), CircleShape))
            }
            // Telefondaki etkinlikler gri nokta
            if (mine) Box(Modifier.size(5.dp).background(Theme.c.muted, CircleShape))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayDetail(d: LocalDate, today: LocalDate, device: List<DeviceCalendar.Event>) {
    val context = LocalContext.current
    val ev = AppData.events[d].orEmpty()
    var deleting by remember { mutableStateOf<DeviceCalendar.Event?>(null) }
    val ask = rememberCalendarPermission()
    AppCard {
        Text(Fmt.longDate(d), color = Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        val diff = ChronoUnit.DAYS.between(today, d)
        val rel = when {
            diff == 0L -> L("client.today")
            diff == 1L -> L("client.tomorrow")
            diff > 0 -> T("client.daysLeft", diff)
            else -> T("calendar.daysAgo", -diff)
        }
        Text(listOf(rel, T("info.week.big", Fmt.isoWeek(d)), T("calendar.dayOfYear", d.dayOfYear)).joinToString(" · "),
            color = Theme.c.muted, fontSize = 13.sp)
        // Hicri tarih (Diyanet takvimi)
        Hijri.date(d)?.let { Text("☾ " + it.text, color = Theme.c.accent, fontSize = 13.sp) }
        for (e in ev.distinctBy { it.key }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(34.dp).background(AppData.color(e.type), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.name, color = Theme.c.text, fontSize = 15.sp)
                    Text(L("calendar.types.${e.type}"), color = Theme.c.muted, fontSize = 12.sp)
                }
                // Özel günü telefonun takvimine ekle (tüm gün)
                IconButton(onClick = { DeviceCalendar.add(context, d, e.name) }) {
                    Icon(Icons.Filled.EditCalendar, L("calendar.addToCalendar"), tint = Theme.c.accent)
                }
            }
        }
        if (AppData.isRamadan(d)) TypeRow("religious", L("calendar.types.ramadan"))
        if (AppData.isSchoolBreak(d) && ev.none { it.type == "school" }) TypeRow("school", L("calendar.types.school"))
        for (e in device) {
            Row(
                Modifier.fillMaxWidth().combinedClickable(
                    onClick = { DeviceCalendar.open(context, e) },
                    onLongClick = { if (e.deletable) deleting = e },
                ).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Ltr {
                    Column(Modifier.width(54.dp), horizontalAlignment = Alignment.End) {
                        if (e.allDay) Text(L("calendar.allDay"), color = Theme.c.text, fontSize = 12.sp)
                        else {
                            Text(Fmt.time(e.begin, java.time.ZoneId.systemDefault()), color = Theme.c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, style = Tnum)
                            Text(Fmt.time(e.end, java.time.ZoneId.systemDefault()), color = Theme.c.muted, fontSize = 12.sp, style = Tnum)
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(4.dp).height(34.dp).background(e.color, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.title, color = Theme.c.text, fontSize = 15.sp, maxLines = 2)
                    Text(e.location.ifEmpty { e.calendar }, color = Theme.c.muted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        if (ev.isEmpty() && device.isEmpty() && !AppData.isRamadan(d)) Text(L("calendar.noDayEvents"), color = Theme.c.muted)
        if (!DeviceCalendar.authorized) {
            Column(Modifier.fillMaxWidth().clickable(onClick = ask).padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EventAvailable, null, tint = Theme.c.accent)
                    Text("  " + L("calendar.showDevice"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                }
                Text(L("calendar.showDeviceNote"), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
    }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(L("calendar.deleteConfirm")) },
            text = { Text(e.title) },
            confirmButton = {
                TextButton(onClick = { DeviceCalendar.remove(context, e); deleting = null }) {
                    Text(L("calendar.delete"), color = Color(0xFFD32F2F))
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(L("list.cancel")) } },
        )
    }
}

@Composable
private fun TypeRow(type: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(AppData.color(type), CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(text, color = AppData.color(type), fontSize = 14.sp)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (t in listOf("holiday", "half", "religious", "special", "school")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(AppData.color(t), CircleShape))
                Text(" " + L("calendar.types.$t"), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
    }
}
