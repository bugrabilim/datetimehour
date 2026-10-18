package tr.bumba.time

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZoneId

/** Dünya saatleri: seçili şehirler (sitedeki varsayılan 12 şehir), saat farkı; ekle, çıkar, varsayılana dön */
@Composable
fun WorldScreen() {
    val now = rememberNow()
    val here = ZoneId.systemDefault()
    val keys = Prefs.world.split(",").filter { it.isNotEmpty() }
    val cities = keys.mapNotNull { k -> City.all.firstOrNull { it.key == k } }
    var editing by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(L("world.h1"), Modifier.weight(1f), color = Theme.c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { editing = !editing }) {
                    Text(if (editing) L("world.done") else L("home.edit"), color = Theme.c.accent)
                }
                IconButton(onClick = { adding = true }) { Icon(Icons.Filled.Add, L("world.addCity"), tint = Theme.c.accent) }
            }
        }
        items(cities, key = { it.key }) { city ->
            val t = now.atZone(city.zone)
            val diff = city.zone.rules.getOffset(now).totalSeconds - here.rules.getOffset(now).totalSeconds
            val dayNote = if (t.toLocalDate() != now.atZone(here).toLocalDate()) Fmt.weekdayShort(t.toLocalDate()) else ""
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (editing) {
                    IconButton(onClick = { Prefs.saveWorld(keys.filter { it != city.key }.joinToString(",")) }) {
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
                Column(horizontalAlignment = Alignment.End) {
                    Text(Fmt.time(t) + (Fmt.ampm(t).let { if (it.isEmpty()) "" else " $it" }),
                        color = Theme.c.text, fontSize = 30.sp, style = Tnum)
                    if (dayNote.isNotEmpty()) Text(dayNote, color = Theme.c.muted, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
        }
        item {
            TextButton(onClick = { Prefs.saveWorld(City.DEFAULT_KEYS) }, modifier = Modifier.padding(vertical = 8.dp)) {
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
                            Prefs.saveWorld((keys + c.key).joinToString(","))
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
}
