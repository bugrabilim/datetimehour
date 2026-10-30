package tr.bumba.time

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Dünya saatleri (iOS'taki WorldClockView): üstte dönen dünya; konum ve "başka bir zamana bak"; seçili şehirler
 * (sitedeki varsayılan 12 şehir) ve saat farkı. Düzenle ve + şehir listesinin başında; düzenlerken çıkar ve sırala.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldScreen() {
    val live = rememberNow()
    val here = ZoneId.systemDefault()
    val keys = Prefs.world.split(",").filter { it.isNotEmpty() }
    val cities = keys.mapNotNull { k -> City.all.firstOrNull { it.key == k } }
    var editing by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var shift by rememberSaveable { mutableStateOf(false) }
    var whenMs by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var picking by remember { mutableStateOf<String?>(null) }
    val now = if (shift) Instant.ofEpochMilli(whenMs) else live

    fun save(list: List<String>) = Prefs.saveWorld(list.joinToString(","))

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            GlobeView(Modifier.fillMaxWidth().height(340.dp))
        }
        item {
            AppCard(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("settings.location"), Modifier.weight(1f), color = Theme.c.text)
                    PlacePicker(size = 16.sp)
                }
                HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("world.changeTime"), Modifier.weight(1f), color = Theme.c.text)
                    Switch(shift, {
                        shift = it
                        if (it) whenMs = System.currentTimeMillis()
                    }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                if (shift) {
                    val t = Instant.ofEpochMilli(whenMs).atZone(here)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(L("converter.whenLabel"), Modifier.weight(1f), color = Theme.c.text)
                        Text(Fmt.shortDate(t.toLocalDate()), Modifier.clickable { picking = "date" }, color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                        Ltr {
                            Text(Fmt.time(t), Modifier.clickable { picking = "time" }, color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            // Düzenle ve + şehir listesinin başında (iOS gibi)
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(L("world.h1"), Modifier.weight(1f), color = Theme.c.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { editing = !editing }) {
                    Text(if (editing) L("world.done") else L("home.edit"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = { adding = true }) { Icon(Icons.Filled.Add, L("world.addCity"), tint = Theme.c.accent) }
            }
        }
        items(cities, key = { it.key }) { city ->
            val t = now.atZone(city.zone)
            val diff = city.zone.rules.getOffset(now).totalSeconds - here.rules.getOffset(now).totalSeconds
            // Şehirde gün farklıysa haftanın günü gösterilir
            val dayNote = if (t.toLocalDate() != now.atZone(here).toLocalDate()) Fmt.weekdayShort(t.toLocalDate()) else ""
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (editing) {
                        IconButton(onClick = { save(keys.filter { it != city.key }) }) {
                            Icon(Icons.Filled.RemoveCircle, L("world.remove"), tint = Color(0xFFD32F2F))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(city.displayName, color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                diff == 0 -> L("client.diffSame")
                                diff > 0 -> T("client.diffAhead", Fmt.hm(diff))
                                else -> T("client.diffBehind", Fmt.hm(diff))
                            },
                            color = Theme.c.muted, fontSize = 13.sp,
                        )
                    }
                    if (editing) {
                        val i = keys.indexOf(city.key)
                        IconButton(onClick = { if (i > 0) save(keys.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, null, tint = if (i > 0) Theme.c.accent else Theme.c.muted)
                        }
                        IconButton(onClick = { if (i < keys.size - 1) save(keys.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < keys.size - 1) {
                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = if (i < keys.size - 1) Theme.c.accent else Theme.c.muted)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Ltr {
                                Text(Fmt.time(t) + (Fmt.ampm(t).let { if (it.isEmpty()) "" else " $it" }),
                                    color = Theme.c.text, fontSize = 30.sp, fontWeight = FontWeight.Light, style = Tnum)
                            }
                            if (dayNote.isNotEmpty()) Text(dayNote, color = Theme.c.accent, fontSize = 12.sp)
                        }
                    }
                }
                HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            }
        }
        item {
            TextButton(onClick = { Prefs.saveWorld(City.DEFAULT_KEYS) }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(L("world.reset"), color = Theme.c.accent, fontSize = 13.sp)
            }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            confirmButton = { TextButton(onClick = { adding = false }) { Text(L("world.done")) } },
            title = { Text(L("world.addCity")) },
            text = {
                LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(City.all.filter { it.key !in keys }) { c ->
                        Column(Modifier.fillMaxWidth().clickable {
                            save(keys + c.key)
                            BadgeStore.hit(Badge.DUNYA)
                            adding = false
                        }.padding(vertical = 8.dp)) {
                            Text(c.displayName, color = Theme.c.text)
                            Text(c.displayCountry, color = Theme.c.muted, fontSize = 12.sp)
                        }
                    }
                }
            },
        )
    }

    // Tarih ve saat seçimi ("başka bir zamana bak"): cihazın saat diliminde
    when (picking) {
        "date" -> {
            val cur = Instant.ofEpochMilli(whenMs).atZone(here)
            val state = rememberDatePickerState(initialSelectedDateMillis = cur.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            DatePickerDialog(
                onDismissRequest = { picking = null },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let {
                            val day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            whenMs = LocalDateTime.of(day, cur.toLocalTime()).atZone(here).toInstant().toEpochMilli()
                            BadgeStore.hit(Badge.CEVIRICI)
                        }
                        picking = null
                    }) { Text(L("common.done")) }
                },
            ) { DatePicker(state) }
        }
        "time" -> {
            val cur = Instant.ofEpochMilli(whenMs).atZone(here)
            val state = rememberTimePickerState(cur.hour, cur.minute, is24Hour = !Prefs.h12)
            AlertDialog(
                onDismissRequest = { picking = null },
                confirmButton = {
                    TextButton(onClick = {
                        whenMs = cur.withHour(state.hour).withMinute(state.minute).withSecond(0).toInstant().toEpochMilli()
                        BadgeStore.hit(Badge.CEVIRICI)
                        picking = null
                    }) { Text(L("common.done")) }
                },
                text = { TimePicker(state) },
            )
        }
    }
}
