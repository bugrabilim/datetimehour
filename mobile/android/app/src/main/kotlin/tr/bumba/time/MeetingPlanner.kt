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

    fun summary(cols: List<Zone>, at: Instant): String =
        (listOf(L("planner.meeting") + " · " + Fmt.longDate(at.atZone(ZoneId.systemDefault()).toLocalDate())) + cols.map { "${it.name}: ${Fmt.hm(at, it.zone)}" })
            .joinToString("\n")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingPlannerScreen() {
    val context = LocalContext.current
    val here = ZoneId.systemDefault()
    var keys by remember { mutableStateOf(MeetingPlanner.keys()) }
    var day by remember { mutableStateOf(LocalDate.now()) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    val cities = keys.mapNotNull { k -> City.all.firstOrNull { it.key == k } }
    val cols = listOf(MeetingPlanner.Zone(L("meet.here"), here)) + cities.map { MeetingPlanner.Zone(it.displayName, it.zone) }
    val start = day.atStartOfDay(here).toInstant()
    fun at(h: Int): Instant = start.plusSeconds(h * 3600L)
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for ((k, key) in listOf(0 to "planner.legendWork", 1 to "planner.legendEdge", 2 to "planner.legendNight")) Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(MeetingPlanner.color(k), RoundedCornerShape(3.dp)).border(1.dp, Theme.c.border, RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text(L(key), color = Theme.c.muted, fontSize = 11.sp)
                }
            }
            Text(L("meet.pickHint"), color = Theme.c.muted, fontSize = 12.sp)
            Ltr {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in cols) Text(c.name, Modifier.weight(1f), color = Theme.c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                    for (h in 0 until 24) {
                        val sel = picked == h
                        Row(
                            Modifier.fillMaxWidth()
                                .border(2.dp, if (sel) Theme.c.accent else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { picked = if (sel) null else h }
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for (c in cols) {
                                val shift = MeetingPlanner.dayShift(at(h), c.zone)
                                Text(Fmt.hm(at(h), c.zone) + if (shift == 0) "" else if (shift > 0) " +1" else " −1",
                                    Modifier.weight(1f).background(MeetingPlanner.color(MeetingPlanner.kind(at(h), c.zone)), RoundedCornerShape(6.dp)).padding(vertical = 6.dp),
                                    color = Theme.c.text, fontSize = 13.sp, textAlign = TextAlign.Center, style = Tnum)
                            }
                        }
                    }
                }
            }
            picked?.let { h ->
                AppCard {
                    Text(T("planner.picked", Fmt.hm(at(h), here), L("meet.here")), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Row {
                        TextButton(onClick = {
                            val begin = at(h).toEpochMilli()
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + 3_600_000L)
                                    .putExtra(CalendarContract.Events.TITLE, L("planner.meeting"))
                                    .putExtra(CalendarContract.Events.DESCRIPTION, MeetingPlanner.summary(cols, at(h))))
                            }
                        }) { Text(L("calendar.addToCalendar"), color = Theme.c.accent) }
                        TextButton(onClick = {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, MeetingPlanner.summary(cols, at(h))), L("share.card")))
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
