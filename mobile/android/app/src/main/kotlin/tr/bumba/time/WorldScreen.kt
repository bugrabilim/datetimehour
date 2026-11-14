package tr.bumba.time

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
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
    val cities = keys.mapNotNull { k -> WorldEntry.of(k) }
    // Şehre dokununca ayrıntı (2. analiz #129)
    var detail by remember { mutableStateOf<WorldEntry?>(null) }
    var labelsVersion by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var shift by rememberSaveable { mutableStateOf(false) }
    var whenMs by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var picking by remember { mutableStateOf<String?>(null) }
    val now = if (shift) Instant.ofEpochMilli(whenMs) else live
    // Küre / harita / 24 saat kadranı (2. analiz #129, #131) ve şehir başına hava
    var view by remember { mutableStateOf(Prefs.store.getString("world.view", "globe") ?: "globe") }
    val wxVersion = rememberWorldWeather(cities)

    fun save(list: List<String>) = Prefs.saveWorld(list.joinToString(","))

    // Şehir satırı (tek sütunda ya da tablette iki sütunda; 2. analiz #145)
    @Composable
    fun cityRow(city: WorldEntry) {
        labelsVersion
        val t = now.atZone(city.zone)
        val diff = city.zone.rules.getOffset(now).totalSeconds - here.rules.getOffset(now).totalSeconds
        // Şehirde gün farklıysa ertesi/önceki gün işareti ve haftanın günü
        val dayShift = java.time.temporal.ChronoUnit.DAYS.between(now.atZone(here).toLocalDate(), t.toLocalDate())
        val dayNote = if (dayShift != 0L) (if (dayShift > 0) "+1 " else "−1 ") + Fmt.weekdayShort(t.toLocalDate()) else ""
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = !editing) { detail = city }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (editing) {
                    IconButton(onClick = { save(keys.filter { it != city.key }) }) {
                        Icon(Icons.Filled.RemoveCircle, L("world.remove"), tint = Theme.c.err)
                    }
                }
                Column(Modifier.weight(1f)) {
                    city.label?.let { Text(it, color = Theme.c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
                    Text((if (city.flag.isNotEmpty()) city.flag + " " else "") + city.name, color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        when {
                            diff == 0 -> L("client.diffSame")
                            diff > 0 -> T("client.diffAhead", Fmt.hm(diff))
                            else -> T("client.diffBehind", Fmt.hm(diff))
                        } + " · " + city.abbr(now),
                        color = Theme.c.muted, fontSize = 13.sp,
                    )
                    // Yaz saati uyarısı (rakip analizi #30): şehirde 7 gün içinde saat değişiyorsa
                    city.dstNote(now)?.let { Text("⟳ $it", color = Theme.c.accent, fontSize = 12.sp) }
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
                    // Küçük gök penceresi + kutusuz saat (doğal tasarım 4 Kasım 2026; önceden göz alan açık mavi kutu, 2. analiz #129)
                    val light = city.light(now)
                    val fg = Theme.c.text
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkyWindow(light)
                    Column(horizontalAlignment = Alignment.End) {
                        Ltr {
                            Text(Fmt.time(t) + (Fmt.ampm(t).let { if (it.isEmpty()) "" else " $it" }),
                                color = fg, fontSize = 26.sp, fontWeight = FontWeight.Light, style = Tnum)
                        }
                        if (dayNote.isNotEmpty()) Text(dayNote, color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        wxVersion
                        WorldWeather.cached(city.key)?.let {
                            // Hava verisi 3D simgeyle (8 Kasım 2026; metindeki emoji yerine)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WeatherIcon(WeatherCode.of(it.code), it.isDay, 16.dp)
                                Spacer(Modifier.width(3.dp))
                                Text(Units.temp(it.celsius), color = Theme.c.muted, fontSize = 12.sp)
                            }
                        }
                    }
                    }
                }
            }
            HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
        }
    }
    // Tablet ve katlanırda iki sütun (düzenlerken tek sütun)
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600 && !editing
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            WorldViewSwitch(view) { view = it; Prefs.store.edit().putString("world.view", it).apply() }
            when (view) {
                "map" -> WorldMapCard(cities, now, Modifier.padding(horizontal = 16.dp))
                "dial" -> WorldDial(cities, now, onShift = { ms -> shift = true; whenMs = ms; BadgeStore.hit(Badge.CEVIRICI) }, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                else -> GlobeView(Modifier.fillMaxWidth().height(340.dp))
            }
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
                    SoftSwitch(shift, {
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
        if (wide) items(cities.chunked(2), key = { it.first().key }) { pair ->
            Row(Modifier.fillMaxWidth()) {
                for (c in pair) Box(Modifier.weight(1f)) { cityRow(c) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        } else items(cities, key = { it.key }) { city -> cityRow(city) }
        item {
            if (cities.any { WorldWeather.cached(it.key) != null }) {
                Text(L("world.wxNote") + " · " + L("weather.attribution"), Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = Theme.c.muted, fontSize = 12.sp)
            }
            TextButton(onClick = { Prefs.saveWorld(City.DEFAULT_KEYS) }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(L("world.reset"), color = Theme.c.accent, fontSize = 13.sp)
            }
        }
    }

    if (adding) {
        // Şehir arama (2. analiz #128): 31 şehir, 1960 yer (places.json) ve kısaltmalar (EST, CET…)
        var q by remember { mutableStateOf("") }
        val results = remember(q, keys) { WorldEntry.search(q, keys.toSet()) }
        AlertDialog(
            onDismissRequest = { adding = false },
            confirmButton = { TextButton(onClick = { adding = false }) { Text(L("world.done")) } },
            title = { Text(L("world.addCity")) },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(q, { q = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text(L("world.searchHint")) })
                    LazyColumn(Modifier.heightIn(max = 420.dp).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(results, key = { it.key }) { c ->
                            Row(Modifier.fillMaxWidth().clickable {
                                save(keys + c.key)
                                BadgeStore.hit(Badge.DUNYA)
                                adding = false
                            }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text((if (c.flag.isNotEmpty()) c.flag + " " else "") + c.name, color = Theme.c.text)
                                    if (c.country.isNotEmpty()) Text(c.country, color = Theme.c.muted, fontSize = 12.sp)
                                }
                                Ltr { Text(Fmt.time(Instant.now().atZone(c.zone)), color = Theme.c.muted, fontSize = 13.sp, style = Tnum) }
                            }
                        }
                    }
                }
            },
        )
    }

    detail?.let { c -> WorldDetailDialog(c, now, onClose = { detail = null; labelsVersion++ }) }

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

/** Şehir ayrıntısı (2. analiz #129): yerel saat ve tarih, saat dilimi, UTC farkı, kısaltma, gün doğumu/batımı ve gün
 *  uzunluğu, Ay evresi, sıradaki saat değişimi; kişisel ad ve emoji ("Annem 👵") */
@Composable
private fun WorldDetailDialog(c: WorldEntry, now: Instant, onClose: () -> Unit) {
    var label by remember { mutableStateOf(c.label ?: "") }
    val t = now.atZone(c.zone)
    AlertDialog(
        onDismissRequest = { WorldLabels.set(c.key, label); onClose() },
        confirmButton = { TextButton(onClick = { WorldLabels.set(c.key, label); onClose() }) { Text(L("world.done")) } },
        title = { Text((if (c.flag.isNotEmpty()) c.flag + " " else "") + c.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (c.country.isNotEmpty()) Text(c.country, color = Theme.c.muted)
                Ltr { Text(Fmt.time(t) + (Fmt.ampm(t).let { if (it.isEmpty()) "" else " $it" }), color = Theme.c.text, fontSize = 40.sp, fontWeight = FontWeight.Light, style = Tnum) }
                Text(Fmt.longDate(t.toLocalDate()), color = Theme.c.text)
                Text(c.zone.id + " · " + c.abbr(now) + " · " + c.utcOffset(now), color = Theme.c.muted, fontSize = 13.sp)
                if (c.lat != null && c.lon != null) {
                    val d = t.toLocalDate()
                    val off = c.zone.rules.getOffset(d.atTime(12, 0).atZone(c.zone).toInstant()).totalSeconds / 3600.0
                    val sun = tr.bumba.time.core.PrayerTimes.sunAltitudeTimes(d.year, d.monthValue, d.dayOfMonth, c.lat, c.lon, off, -0.833)
                    fun hm(h: Double?) = h?.let { val m = Math.round(it * 60).toInt(); "%02d:%02d".format(java.util.Locale.ROOT, (m / 60 + 24) % 24, m % 60) } ?: "–"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DuoIcon("sun", 18.dp, Theme.c.gold)
                        Spacer(Modifier.width(6.dp))
                        Text("↑ ${hm(sun.rise)}   ↓ ${hm(sun.set)}", color = Theme.c.text, style = Tnum)
                    }
                    if (sun.rise != null && sun.set != null) {
                        val len = Math.round((sun.set!! - sun.rise!!) * 60).toInt()
                        Text(T("seasons.dayLengthValue", len / 60, len % 60), color = Theme.c.muted, fontSize = 13.sp)
                    }
                }
                val moon = tr.bumba.time.core.Nature.moon(now.toEpochMilli())
                Text(MOON_PHASE_EMOJI[moon.phaseIndex] + " " + L("nature.phases.${MOON_PHASE_KEYS[moon.phaseIndex]}"), color = Theme.c.text)
                c.dstNote(now, 60)?.let { Text("⟳ $it", color = Theme.c.accent, fontSize = 13.sp) }
                androidx.compose.material3.OutlinedTextField(label, { label = it.take(30) }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(L("world.label")) }, placeholder = { Text(L("world.labelHint")) })
            }
        },
    )
}

/** Küçük gök penceresi (doğal tasarım 4 Kasım 2026): gündüz güneşli gök, alacakaranlıkta ufukta güneş, gecede hilal */
@Composable
fun SkyWindow(light: String, modifier: Modifier = Modifier) {
    val (top, bottom) = when (light) {
        "day" -> Color(0xFFA9C9DF) to Color(0xFFF2E6CC)
        "dusk" -> Color(0xFF46305A) to Color(0xFFE8A27C)
        else -> Color(0xFF1F2240) to Color(0xFF3B3762)
    }
    androidx.compose.foundation.Canvas(
        modifier.size(30.dp).clip(RoundedCornerShape(10.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(top, bottom))),
    ) {
        val w = size.width
        val sun = Color(0xFFF6C35B)
        when (light) {
            "day" -> drawCircle(sun, radius = w * 0.2f, center = androidx.compose.ui.geometry.Offset(w * 0.62f, w * 0.38f))
            "dusk" -> drawCircle(sun, radius = w * 0.24f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, w * 0.94f))
            else -> {
                drawCircle(Color(0xFFFFF8EC), radius = w * 0.19f, center = androidx.compose.ui.geometry.Offset(w * 0.56f, w * 0.42f))
                drawCircle(androidx.compose.ui.graphics.lerp(top, bottom, 0.35f), radius = w * 0.17f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.65f, w * 0.35f))
                drawCircle(Color(0xFFFFF8EC).copy(alpha = 0.7f), radius = w * 0.03f, center = androidx.compose.ui.geometry.Offset(w * 0.22f, w * 0.25f))
                drawCircle(Color(0xFFFFF8EC).copy(alpha = 0.5f), radius = w * 0.025f, center = androidx.compose.ui.geometry.Offset(w * 0.3f, w * 0.7f))
            }
        }
    }
}
