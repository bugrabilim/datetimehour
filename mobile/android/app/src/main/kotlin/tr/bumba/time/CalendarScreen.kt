package tr.bumba.time

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
    /** Hicri aylarla gezme (2. analiz #119): seçili günün hicri ayından bir önceki/sonraki ayın ilk günü */
    fun moveHijri(d: Int) {
        val h = Hijri.date(selected) ?: return
        var y = h.year
        var m = h.month + d
        if (m < 1) { m = 12; y-- } else if (m > 12) { m = 1; y++ }
        val first = Hijri.month(y, m)?.first ?: return
        go(if (Hijri.date(today)?.let { it.year == y && it.month == m } == true) today else first)
    }
    /** Önceki/sonraki: görünüme göre gün, hafta, ay (hicri ay), yıl */
    fun step(d: Int) {
        when (CalView.mode) {
            "week" -> go(selected.plusWeeks(d.toLong()))
            "day" -> go(selected.plusDays(d.toLong()))
            "year" -> go(selected.plusYears(d.toLong()))
            else -> if (CalView.hijriNav && CalView.mode == "month") moveHijri(d) else move(d.toLong())
        }
    }
    // Saat ekranından istenen gün
    LaunchedEffect(Nav.calendarDay) {
        Nav.calendarDay?.let { go(it); Nav.calendarDay = null }
    }
    LaunchedEffect(Unit) { DeviceCalendar.reload(context) }
    CalView.load()
    remember { WorldHol.load(context) }
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
                Text(Fmt.month(month.monthValue), color = Theme.c.text, fontSize = 28.sp, fontFamily = AppFonts.heading,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("${month.year}", color = Theme.c.accent, fontSize = 18.sp, fontFamily = AppFonts.heading, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            // Başlıkta az simge (doğal tasarım, madde 13): Bugün ve takvim yaprağı "…" menüsünde
            IconButton(onClick = { step(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("prayer.prevMonth"), tint = Theme.c.accent) }
            IconButton(onClick = { step(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("prayer.nextMonth"), tint = Theme.c.accent) }
            CalViewMenu(onToday = { go(today) }, onLeaf = { leaf = true })
            IconButton(onClick = { DeviceCalendar.add(context, selected) }) { Icon(Icons.Filled.Add, L("calendar.add"), tint = Theme.c.accent) }
        }
        // Görünüm: ay, hafta, gün, ajanda, yıl (2. analiz #116)
        CalModeBar()
        val swipe = Modifier.pointerInput(ym, CalView.mode, CalView.hijriNav) {
            var total = 0f
            detectHorizontalDragGestures(onDragEnd = {
                val dir = if (total < -60) 1 else if (total > 60) -1 else 0
                if (dir != 0) when {
                    CalView.mode == "week" -> go(selected.plusWeeks(dir.toLong() * if (Strings.isRtl) -1 else 1))
                    CalView.mode == "day" -> go(selected.plusDays(dir.toLong() * if (Strings.isRtl) -1 else 1))
                    CalView.mode == "year" -> go(selected.plusYears(dir.toLong() * if (Strings.isRtl) -1 else 1))
                    CalView.mode == "month" && CalView.hijriNav -> moveHijri(dir * if (Strings.isRtl) -1 else 1)
                    else -> move((dir * if (Strings.isRtl) -1 else 1).toLong())
                }
                total = 0f
            }) { _, d -> total += d }
        }
        val pick: (LocalDate) -> Unit = { sel = it.toString(); ym = YearMonth.from(it).toString(); BadgeStore.hit(Badge.TAKVIM) }
        when (CalView.mode) {
            "week" -> Box(swipe) { WeekView(selected, today, DeviceCalendar.events(context, YearMonth.from(selected)) + DeviceCalendar.events(context, YearMonth.from(selected.plusDays(6)))) { pick(it); CalView.mode = "day"; CalView.save() } }
            "day" -> {
                Text(Fmt.longDate(selected), color = Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(vertical = 6.dp))
                Box(swipe) { DayTimeline(selected, device[selected].orEmpty()) }
            }
            "agenda" -> AgendaView(selected, { d -> DeviceCalendar.events(context, YearMonth.from(d))[d].orEmpty() }) { pick(it); CalView.mode = "day"; CalView.save() }
            "year" -> Box(swipe) {
                val counts = remember(selected.year, DeviceCalendar.version, DeviceCalendar.authorized) {
                    (1..12).flatMap { DeviceCalendar.events(context, YearMonth.of(selected.year, it)).entries }.associate { it.key to it.value.size }
                }
                YearView(selected.year, today, { counts[it] ?: 0 }) { m -> ym = m.toString(); sel = m.atDay(1).toString(); CalView.mode = "month"; CalView.save() }
            }
            else -> {
                if (CalView.hijriNav) {
                    val h = Hijri.date(selected)
                    if (h != null) {
                        Text(h.monthName + " " + h.year, color = Theme.c.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                        Box(swipe) { HijriMonthGrid(h.year, h.month, today, selected, pick) }
                    } else Text(L("hijri.note"), color = Theme.c.muted)
                } else MonthGrid(month, today, selected, device, swipe) { pick(it) }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (CalView.mode == "month") DayDetail(selected, today, device[selected].orEmpty())
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
    val moons = remember(month, CalView.moon) { if (CalView.moon) CalView.moonMarks(month) else emptyMap() }
    val dst = remember(month, CalView.dst) { if (CalView.dst) CalView.dstDays(month) else emptySet() }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            if (CalView.week) Text(L("cal.wk"), Modifier.width(26.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Theme.c.muted)
            for (i in 0..6) {
                Text(L("calendar.weekdaysShort.$i"), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp,
                    color = if (i >= 5) Theme.c.accent else Theme.c.muted)
            }
        }
        for (row in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (CalView.week) {
                    // Satırın herhangi bir günü aynı ISO haftasındadır (pazartesi başlar)
                    val any = month.atDay(maxOf(1, minOf(month.lengthOfMonth(), row * 7 - lead + 1)))
                    Text(Fmt.isoWeek(any).toString(), Modifier.width(26.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Theme.c.muted, style = Tnum)
                }
                for (col in 0..6) {
                    val n = row * 7 + col - lead + 1
                    Box(Modifier.weight(1f).then(if (CalView.names) Modifier.height(80.dp) else Modifier.aspectRatio(1f)), contentAlignment = Alignment.Center) {
                        if (n in 1..month.lengthOfMonth()) {
                            val d = month.atDay(n)
                            DayCell(d, d == today, d == selected, device[d].orEmpty().isNotEmpty(), moons[n], n in dst) { onPick(d) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, isToday: Boolean, isSelected: Boolean, mine: Boolean, moon: String?, dst: Boolean, onClick: () -> Unit) {
    val ev = calEvents(d)
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
        if (CalView.hijri) Hijri.date(d)?.let { Text(it.day.toString(), fontSize = 9.sp, color = Theme.c.muted, style = Tnum) }
        val first = ev.firstOrNull()
        if (CalView.names && first != null) {
            // Etkinlik adı; aynı rengin art arda günleri boşluksuz şerit gibi birleşir (bayram tatili gibi)
            Text(first.name, Modifier.fillMaxWidth().background(AppData.color(first.type).copy(alpha = 0.22f)).padding(vertical = 1.dp),
                fontSize = 8.sp, maxLines = 1, color = Theme.c.text, textAlign = TextAlign.Center,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        } else {
            Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (t in ev.map { it.type }.distinct().take(3)) {
                    Box(Modifier.size(5.dp).background(AppData.color(t), CircleShape))
                }
                // Telefondaki etkinlikler gri nokta
                if (mine) Box(Modifier.size(5.dp).background(Theme.c.muted, CircleShape))
            }
        }
    }
    // Ay evresi ve saat değişimi işareti sağ üst köşede (hücrenin yüksekliğini değiştirmez)
    if (moon != null || dst) Box(Modifier.fillMaxSize().padding(2.dp), contentAlignment = Alignment.TopEnd) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Saat değişimi günü iki tonlu simgeyle (8 Kasım 2026; "⏱" yerine)
            if (dst) DuoIcon("dst", 10.dp, Theme.c.terra)
            if (moon != null) Text(moon, fontSize = 10.sp)
        }
    }
}

/** Takvim görünüm seçenekleri (2. analiz #123, #119, #117): hafta no, Ay evreleri, saat değişimi, hicri gün, etkinlik adları */
object CalView {
    var week by androidx.compose.runtime.mutableStateOf(true)
    var moon by androidx.compose.runtime.mutableStateOf(true)
    var dst by androidx.compose.runtime.mutableStateOf(true)
    var hijri by androidx.compose.runtime.mutableStateOf(false)
    var names by androidx.compose.runtime.mutableStateOf(false)
    /** 2. analiz #116, #119, #122: görünüm, hicri aylarla gezme, ülke, gizli türler */
    var mode by androidx.compose.runtime.mutableStateOf("month")
    var hijriNav by androidx.compose.runtime.mutableStateOf(false)
    var country by androidx.compose.runtime.mutableStateOf("TR")
    var hidden by androidx.compose.runtime.mutableStateOf(emptySet<String>())
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        val sp = Prefs.store
        week = sp.getBoolean("cal.weekNo", true); moon = sp.getBoolean("cal.moon", true); dst = sp.getBoolean("cal.dst", true)
        hijri = sp.getBoolean("cal.hijri", false); names = sp.getBoolean("cal.names", false)
        mode = sp.getString("cal.mode", "month") ?: "month"; hijriNav = sp.getBoolean("cal.hijriNav", false)
        country = sp.getString("cal.country", "TR") ?: "TR"
        hidden = (sp.getString("cal.hidden", "") ?: "").split(",").filter { it.isNotEmpty() }.toSet()
    }

    fun save() {
        Prefs.store.edit().putBoolean("cal.weekNo", week).putBoolean("cal.moon", moon).putBoolean("cal.dst", dst)
            .putBoolean("cal.hijri", hijri).putBoolean("cal.names", names)
            .putString("cal.mode", mode).putBoolean("cal.hijriNav", hijriNav).putString("cal.country", country)
            .putString("cal.hidden", hidden.joinToString(",")).apply()
    }

    /** Ayın yeni ay ve dolunay günleri (telefonun saat diliminde): gün → emoji */
    fun moonMarks(month: YearMonth): Map<Int, String> {
        val zone = java.time.ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli().toDouble()
        val k0 = Math.floor((start / 86_400_000.0 + 2440587.5 - 2451550.09766) / 29.530588861).toInt() - 1
        val out = HashMap<Int, String>()
        for (k in k0..k0 + 3) for (full in listOf(false, true)) {
            val d = java.time.Instant.ofEpochMilli(tr.bumba.time.core.Nature.phaseMillis(k, full)).atZone(zone).toLocalDate()
            if (YearMonth.from(d) == month) out[d.dayOfMonth] = if (full) "🌕" else "🌑"
        }
        return out
    }

    /** Telefonun saat diliminde saat değişimi (yaz saati) olan günler */
    fun dstDays(month: YearMonth): Set<Int> {
        val zone = java.time.ZoneId.systemDefault()
        val rules = zone.rules
        var t = month.atDay(1).atStartOfDay(zone).toInstant().minusSeconds(1)
        val end = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant()
        val out = HashSet<Int>()
        while (true) {
            val tr = rules.nextTransition(t) ?: break
            if (!tr.instant.isBefore(end)) break
            out.add(tr.instant.atZone(zone).toLocalDate().dayOfMonth)
            t = tr.instant
        }
        return out
    }
}

@Composable
private fun CalViewMenu(onToday: () -> Unit, onLeaf: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    var countryOpen by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        IconButton(onClick = { open = true }) { Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, L("cal.view"), tint = Theme.c.accent) }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(L("client.today")) },
                leadingIcon = { Icon(Icons.Filled.Today, null, tint = Theme.c.accent) },
                onClick = { open = false; onToday() },
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(L("leaf.title")) },
                leadingIcon = { Icon(Icons.Filled.Article, null, tint = Theme.c.accent) },
                onClick = { open = false; onLeaf() },
            )
            androidx.compose.material3.HorizontalDivider(color = Theme.c.line)
            @Composable
            fun item(key: String, on: Boolean, set: (Boolean) -> Unit) = androidx.compose.material3.DropdownMenuItem(
                text = { Text(L(key)) },
                trailingIcon = { androidx.compose.material3.Checkbox(checked = on, onCheckedChange = null) },
                onClick = { set(!on); CalView.save() },
            )
            item("cal.weekNo", CalView.week) { CalView.week = it }
            item("cal.moon", CalView.moon) { CalView.moon = it }
            item("cal.dst", CalView.dst) { CalView.dst = it }
            item("cal.hijriDays", CalView.hijri) { CalView.hijri = it }
            item("cal.names", CalView.names) { CalView.names = it }
            CalExtraMenuItems { open = false; countryOpen = true }
        }
    }
    if (countryOpen) CalCountryDialog { countryOpen = false }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayDetail(d: LocalDate, today: LocalDate, device: List<DeviceCalendar.Event>) {
    val context = LocalContext.current
    val ev = calEvents(d)
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
        // O günün havası (2. analiz #118): Open-Meteo günlük tahmininde varsa; yalnız Hava özelliği açıkken
        if (Features.isOn(Tab.WEATHER)) {
            val place = AppPlace.current()
            val w = rememberWeather(place.lat, place.lon)
            w?.daily?.firstOrNull { it.date == d }?.let { day ->
                Text(T("cal.weather", WeatherCode.of(day.code).icon() + " " + Units.temp(day.max) + " / " + Units.temp(day.min)) + " · " + L("weather.attribution"),
                    color = Theme.c.text, fontSize = 13.sp)
            }
        }
        for (e in ev.distinctBy { it.key }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(34.dp).background(AppData.color(e.type), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.name, color = Theme.c.text, fontSize = 15.sp)
                    Text(calendarTypeLabel(e.type), color = Theme.c.muted, fontSize = 12.sp)
                }
                // Özel günü telefonun takvimine ekle (tüm gün)
                IconButton(onClick = { DeviceCalendar.add(context, d, e.name) }) {
                    Icon(Icons.Filled.EditCalendar, L("calendar.addToCalendar"), tint = Theme.c.accent)
                }
            }
        }
        if (AppData.isRamadan(d)) TypeRow("religious", L("calendar.types.ramadan"))
        if (CalView.country == "TR" && "school" !in CalView.hidden && AppData.isSchoolBreak(d) && ev.none { it.type == "school" }) TypeRow("school", L("calendar.types.school"))
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
                    Text(L("calendar.delete"), color = Theme.c.err)
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
        for (t in (if (CalView.country == "TR") listOf("holiday", "half", "religious", "special", "school") else listOf("holiday", "religious")).filter { it !in CalView.hidden }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(AppData.color(t), CircleShape))
                Text(" " + calendarTypeLabel(t), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Takvim türünün adı. Türkçe dışındaki arayüzde resmî tatil, arife ve okul türlerine ülke adı eklenir
 * ("Public holiday · Türkiye"): takvimdeki tatiller Türkiye'nindir (2. analiz #92).
 */
fun calendarTypeLabel(t: String): String {
    val base = L("calendar.types.$t")
    // Takvimde başka ülke seçiliyse (2. analiz #122) resmî tatil o ülkenindir
    val cc = if (t == "holiday" && CalView.country != "TR") CalView.country else "TR"
    if ((Strings.lang == "tr" && cc == "TR") || t !in setOf("holiday", "half", "school")) return base
    val country = java.util.Locale.Builder().setRegion(cc).build().getDisplayCountry(Strings.locale)
    return if (country.isBlank()) base else "$base · $country"
}
