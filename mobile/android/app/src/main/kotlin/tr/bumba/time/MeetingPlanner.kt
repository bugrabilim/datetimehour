package tr.bumba.time

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Toplantı planlayıcı (rakip analizi listesi #85; sitedeki /toplanti-planlayici/ ve iOS'taki MeetingPlannerView ile aynı kural):
 * bulunduğunuz saat dilimi ve en çok 5 şehir; seçilen günün 24 saati yan yana, mesai (09–18) yeşil, erken/geç (07–09, 18–22)
 * turuncu, gece boş. Satıra dokununca saat seçilir: takvime ekle ya da paylaş. Şehirler `meet.cities` (iOS'la aynı).
 */
object MeetingPlanner {
    data class Zone(val name: String, val zone: ZoneId)

    /** Varsayılan: dünya saatleri listesinden burayla aynı dilimde olmayan ilk iki şehir */
    fun defaultKeys(): List<String> {
        val now = Instant.now()
        val here = ZoneId.systemDefault().rules.getOffset(now)
        return Prefs.world.split(",").filter { k -> City.all.firstOrNull { it.key == k }?.let { it.zone.rules.getOffset(now) != here } ?: false }.take(2)
    }

    fun keys(): List<String> = Prefs.store.getString("meet.cities", "")!!.let { raw -> if (raw.isEmpty()) defaultKeys() else raw.split(",") }
    fun saveKeys(keys: List<String>) = Prefs.store.edit().putString("meet.cities", keys.joinToString(",")).apply()

    /** 0 mesai (başlangıç 09:00–17:00), 1 erken/geç (07–09, 18–22), 2 gece */
    fun kind(at: Instant, zone: ZoneId): Int {
        val t = at.atZone(zone)
        val m = t.hour * 60 + t.minute
        return when {
            m in 540..1020 -> 0
            m in 420 until 1320 -> 1
            else -> 2
        }
    }

    /** Şehirdeki gün buradaki güne göre: −1, 0, +1 */
    fun dayShift(at: Instant, zone: ZoneId): Int = at.atZone(zone).toLocalDate().compareTo(at.atZone(ZoneId.systemDefault()).toLocalDate()).coerceIn(-1, 1)

    fun color(kind: Int): Color = when (kind) { 0 -> Color(0xFF2E7D32).copy(alpha = 0.28f); 1 -> Color(0xFFEF6C00).copy(alpha = 0.22f); else -> Color.Transparent }

    fun summary(cols: List<Zone>, at: Instant, end: Instant? = null): String =
        (listOf(L("planner.meeting") + " · " + Fmt.longDate(at.atZone(ZoneId.systemDefault()).toLocalDate())) +
            cols.map { "${it.name}: ${Fmt.hm(at, it.zone)}" + (end?.let { e -> "–" + Fmt.hm(e, it.zone) } ?: "") })
            .joinToString("\n")

    // 2. analiz #72: hafta sonu, borsa saatleri, sütun başına 12/24 saat
    fun weekend(at: Instant, zone: ZoneId) = at.atZone(zone).dayOfWeek.let { it == java.time.DayOfWeek.SATURDAY || it == java.time.DayOfWeek.SUNDAY }
    val WEEKEND_COLOR = Color(0xFF9E9E9E).copy(alpha = 0.22f)

    /** Borsa: kısa ad, saat dilimi, seanslar (yerel dakika [başlangıç, bitiş)), işlem günleri (ISO 1–7) */
    data class Exchange(val code: String, val zone: ZoneId, val sessions: List<Pair<Int, Int>>, val days: Set<Int> = setOf(1, 2, 3, 4, 5))
    val EXCHANGES = listOf(
        Exchange("BIST", ZoneId.of("Europe/Istanbul"), listOf(600 to 1080)),
        Exchange("LSE", ZoneId.of("Europe/London"), listOf(480 to 990)),
        Exchange("Xetra", ZoneId.of("Europe/Berlin"), listOf(540 to 1050)),
        Exchange("NYSE", ZoneId.of("America/New_York"), listOf(570 to 960)),
        Exchange("TSE", ZoneId.of("Asia/Tokyo"), listOf(540 to 690, 750 to 930)),
        Exchange("HKEX", ZoneId.of("Asia/Hong_Kong"), listOf(570 to 720, 780 to 960)),
        Exchange("SSE", ZoneId.of("Asia/Shanghai"), listOf(570 to 690, 780 to 900)),
        Exchange("Tadawul", ZoneId.of("Asia/Riyadh"), listOf(600 to 900), setOf(7, 1, 2, 3, 4)),
    )

    fun isOpen(e: Exchange, at: Instant): Boolean {
        val t = at.atZone(e.zone)
        if (t.dayOfWeek.value !in e.days) return false
        val m = t.hour * 60 + t.minute
        return e.sessions.any { m >= it.first && m < it.second }
    }

    /** Saat diliminde (yarım saatlik adımla) en az bir an açık mı */
    fun openInHour(e: Exchange, start: Instant) = (0 until 60 step 15).any { isOpen(e, start.plusSeconds(it * 60L)) }

    fun h12Cols(): Set<String> = Prefs.store.getString("meet.h12", "")!!.split(",").filter { it.isNotBlank() }.toSet()
    fun toggleH12(id: String) {
        val s = h12Cols().toMutableSet(); if (!s.add(id)) s.remove(id)
        Prefs.store.edit().putString("meet.h12", s.joinToString(",")).apply()
    }

    fun hm(at: Instant, zone: ZoneId, h12: Boolean): String {
        if (!h12) return "%02d:%02d".format(java.util.Locale.ROOT, at.atZone(zone).hour, at.atZone(zone).minute)
        val t = at.atZone(zone)
        return "${(t.hour % 12).let { if (it == 0) 12 else it }}:%02d ${if (t.hour < 12) "AM" else "PM"}".format(java.util.Locale.ROOT, t.minute)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingPlannerScreen() {
    val context = LocalContext.current
    val here = ZoneId.systemDefault()
    var keys by remember { mutableStateOf(MeetingPlanner.keys()) }
    var day by remember { mutableStateOf(LocalDate.now()) }
    var picked by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var h12 by remember { mutableStateOf(MeetingPlanner.h12Cols()) }
    var showEx by remember { mutableStateOf(Prefs.store.getBoolean("meet.exchanges", false)) }
    remember { DeviceCalendar.reload(context); 0 }
    var adding by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    val cities = keys.mapNotNull { k -> City.all.firstOrNull { it.key == k } }
    val cols = listOf(MeetingPlanner.Zone(L("meet.here"), here)) + cities.map { MeetingPlanner.Zone(it.displayName, it.zone) }
    val colIds = listOf("here") + cities.map { it.key }
    val start = day.atStartOfDay(here).toInstant()
    fun at(h: Int): Instant = start.plusSeconds(h * 3600L)
    // Kendi takvimindeki etkinliklerin kapladığı saatler (izin verilmişse; tüm gün etkinlikler hariç)
    val busy: Set<Int> = remember(day, DeviceCalendar.version) {
        if (!DeviceCalendar.authorized) emptySet() else DeviceCalendar.events(context, java.time.YearMonth.from(day))[day].orEmpty()
            .filter { !it.allDay }.flatMap { e -> (0 until 24).filter { h -> e.begin.isBefore(at(h + 1)) && e.end.isAfter(at(h)) } }.toSet()
    }
    fun save(list: List<String>) { keys = list; MeetingPlanner.saveKeys(list); picked = null }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppCard {
            Row(Modifier.fillMaxWidth().clickable { pickingDate = true }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(L("planner.dateLabel"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                Text(Fmt.longDate(day), color = Theme.c.accent, fontSize = 15.sp)
            }
            for (c in cities) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.displayName, color = Theme.c.text, fontSize = 16.sp)
                    Text(c.displayCountry, color = Theme.c.muted, fontSize = 12.sp)
                }
                IconButton(onClick = { save(keys - c.key) }) { Icon(Icons.Filled.Close, L("planner.remove"), tint = Theme.c.muted) }
            }
            if (cities.size < 5) TextButton(onClick = { adding = true }) { Text("+ " + L("planner.addLabel"), color = Theme.c.accent) }
        }
        if (cities.isEmpty()) {
            Text(L("planner.minCities"), color = Theme.c.muted)
        } else {
            val common = (0 until 24).filter { h -> cols.all { MeetingPlanner.kind(at(h), it.zone) == 0 } }
            Text(if (common.isEmpty()) L("planner.noCommon") else T("planner.common", L("meet.here"), common.joinToString(", ") { Fmt.hm(at(it), here) }),
                color = Theme.c.text, fontSize = 14.sp)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((c, key) in listOf(MeetingPlanner.color(0) to "planner.legendWork", MeetingPlanner.color(1) to "planner.legendEdge",
                        MeetingPlanner.color(2) to "planner.legendNight", MeetingPlanner.WEEKEND_COLOR to "meet.weekend")) Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(c, RoundedCornerShape(3.dp)).border(1.dp, Theme.c.border, RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text(L(key), color = Theme.c.muted, fontSize = 11.sp)
                }
                if (busy.isNotEmpty()) Text("● " + L("meet.busy"), color = Theme.c.accent, fontSize = 11.sp)
            }
            Text(L("meet.rangeHint"), color = Theme.c.muted, fontSize = 12.sp)
            if (!DeviceCalendar.authorized) Text(L("meet.calendarHint"), color = Theme.c.muted, fontSize = 12.sp)
            Ltr {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for ((i, c) in cols.withIndex()) Text(c.name + if (colIds[i] in h12) " ·12" else "",
                            Modifier.weight(1f).clickable { MeetingPlanner.toggleH12(colIds[i]); h12 = MeetingPlanner.h12Cols() },
                            color = Theme.c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
                    }
                    for (h in 0 until 24) {
                        val sel = picked?.let { h in it.first..it.second } == true
                        Row(
                            Modifier.fillMaxWidth()
                                .border(2.dp, if (sel) Theme.c.accent else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable {
                                    val p = picked
                                    picked = when {
                                        p == null -> h to h
                                        p.first == p.second && h == p.first -> null
                                        p.first == p.second -> minOf(h, p.first) to maxOf(h, p.first)
                                        else -> h to h
                                    }
                                }
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for ((i, c) in cols.withIndex()) {
                                val shift = MeetingPlanner.dayShift(at(h), c.zone)
                                val kind = MeetingPlanner.kind(at(h), c.zone)
                                val bg = if (MeetingPlanner.weekend(at(h), c.zone) && kind != 2) MeetingPlanner.WEEKEND_COLOR else MeetingPlanner.color(kind)
                                Text((if (i == 0 && h in busy) "● " else "") + MeetingPlanner.hm(at(h), c.zone, colIds[i] in h12) + if (shift == 0) "" else if (shift > 0) " +1" else " −1",
                                    Modifier.weight(1f).background(bg, RoundedCornerShape(6.dp)).padding(vertical = 6.dp),
                                    color = if (i == 0 && h in busy) Theme.c.accent else Theme.c.text, fontSize = 13.sp, textAlign = TextAlign.Center, style = Tnum, maxLines = 1)
                            }
                        }
                    }
                }
            }
            // Borsa saatleri (burası saatiyle, 24 hücre)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("meet.exchanges"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                androidx.compose.material3.Switch(showEx, { showEx = it; Prefs.store.edit().putBoolean("meet.exchanges", it).apply() },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
            }
            if (showEx) Ltr {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (e in MeetingPlanner.EXCHANGES) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.code, Modifier.width(64.dp), color = Theme.c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            for (h in 0 until 24) Box(Modifier.weight(1f).heightIn(min = 14.dp).background(
                                if (MeetingPlanner.openInHour(e, at(h))) Theme.c.accent.copy(alpha = 0.75f) else Theme.c.surface2, RoundedCornerShape(2.dp)))
                        }
                    }
                    Row { Spacer(Modifier.width(64.dp)); for (h in listOf(0, 6, 12, 18)) Text("%02d".format(h), Modifier.weight(1f), color = Theme.c.muted, fontSize = 10.sp) }
                }
            }
            if (showEx) Text(L("meet.exchangesNote"), color = Theme.c.muted, fontSize = 11.sp)
            picked?.let { (a, b) ->
                val begin = at(a); val end = at(b + 1)
                AppCard {
                    Text(T("planner.picked", Fmt.hm(begin, here) + "–" + Fmt.hm(end, here), L("meet.here")), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (showEx) {
                        val open = MeetingPlanner.EXCHANGES.filter { e -> (a..b).any { MeetingPlanner.openInHour(e, at(it)) } }.map { it.code }
                        Text(if (open.isEmpty()) L("meet.noneOpen") else T("meet.openAt", open.joinToString(", ")), color = Theme.c.muted, fontSize = 13.sp)
                    }
                    Row {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin.toEpochMilli())
                                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.toEpochMilli())
                                    .putExtra(CalendarContract.Events.TITLE, L("planner.meeting"))
                                    .putExtra(CalendarContract.Events.DESCRIPTION, MeetingPlanner.summary(cols, begin, end)))
                            }
                        }) { Text(L("calendar.addToCalendar"), color = Theme.c.accent) }
                        TextButton(onClick = {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, MeetingPlanner.summary(cols, begin, end)), L("share.card")))
                        }) { Text(L("share.card"), color = Theme.c.accent) }
                    }
                }
            }
        }
    }

    if (adding) AlertDialog(
        onDismissRequest = { adding = false },
        confirmButton = { TextButton(onClick = { adding = false }) { Text(L("alarm.cancel")) } },
        title = { Text(L("planner.addLabel")) },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(City.all.filter { it.key !in keys }) { c ->
                    Column(Modifier.fillMaxWidth().clickable { save(keys + c.key); adding = false }.padding(vertical = 8.dp)) {
                        Text(c.displayName, color = Theme.c.text)
                        Text(c.displayCountry, color = Theme.c.muted, fontSize = 12.sp)
                    }
                }
            }
        },
    )
    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate(); picked = null }
                    pickingDate = false
                }) { Text(L("world.done")) }
            },
        ) { DatePicker(state) }
    }
}
