package tr.bumba.time

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.DateRange
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Takvim görünümleri ve ülke seçimi (2. analiz #116, #119, #122, #126, #127; iOS CalendarViews.swift ile aynı):
 * ay / hafta / gün (zaman çizelgesi, bayram kartı) / ajanda (60 gün) / yıl (yoğunluk ısı haritası); hicri aylarla gezme;
 * ülke seçimi (30 ülkenin ulusal tatilleri, src/data/world-holidays.json) ve gösterilen gün türleri.
 * Kayıt: `cal.mode`, `cal.hijriNav`, `cal.country` (TR varsayılan), `cal.hidden` (virgülle tür).
 */
object WorldHol {
    private var loaded = false
    private var strs: List<String> = emptyList()
    private val days = HashMap<String, HashMap<LocalDate, MutableList<Pair<Int, Int>>>>()
    private var tx: Map<Int, Map<String, String>> = emptyMap()
    private var native: Map<String, Set<String>> = emptyMap()
    var countries: List<String> = emptyList()
        private set

    fun load(c: Context) {
        if (loaded) return
        loaded = true
        val o = JSONObject(c.assets.open("world-holidays.json").bufferedReader().use { it.readText() })
        val s = o.getJSONArray("s")
        strs = (0 until s.length()).map { s.getString(it) }
        val cs = o.getJSONObject("countries")
        countries = cs.keys().asSequence().toList()
        for (cc in countries) {
            val map = HashMap<LocalDate, MutableList<Pair<Int, Int>>>()
            val ys = cs.getJSONObject(cc)
            for (y in ys.keys()) {
                val arr = ys.getJSONArray(y)
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONArray(i)
                    map.getOrPut(LocalDate.parse(r.getString(0))) { mutableListOf() }.add(r.getInt(1) to r.getInt(2))
                }
            }
            days[cc] = map
        }
        val t = o.getJSONObject("tx")
        tx = t.keys().asSequence().associate { k -> k.toInt() to t.getJSONObject(k).let { j -> j.keys().asSequence().associateWith { j.getString(it) } } }
        val n = o.getJSONObject("native")
        native = n.keys().asSequence().associateWith { k -> n.getJSONArray(k).let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } }
    }

    /** Sitedeki holName kuralı: İngilizcede İngilizce ad, ülkenin dilinde yerel ad, diğerlerinde çeviri (yoksa İngilizce) */
    private fun name(cc: String, en: Int, local: Int): String {
        val lang = Strings.lang
        return when {
            lang == "en" -> strs[en]
            cc in native[lang].orEmpty() -> strs[local]
            else -> tx[en]?.get(lang) ?: strs[en]
        }
    }

    fun events(cc: String, d: LocalDate): List<CalEvent> =
        days[cc]?.get(d).orEmpty().mapIndexed { i, (en, lo) -> CalEvent(d, "w:$cc:$en:$i", "holiday", null, name(cc, en, lo)) }

    /** Ülke adı (telefonun dil verisinden) */
    fun countryName(cc: String): String = java.util.Locale.Builder().setRegion(cc).build().getDisplayCountry(Strings.locale).ifBlank { cc }
}

/** Takvimin seçili ülkesine ve türlerine göre bir günün özel günleri */
fun calEvents(d: LocalDate): List<CalEvent> {
    val base = AppData.events[d].orEmpty()
    val list = if (CalView.country == "TR") base
        else base.filter { it.type == "religious" } + WorldHol.events(CalView.country, d)
    return list.filter { it.type !in CalView.hidden }
}

/** Ay / hafta / gün / ajanda / yıl seçimi */
@Composable
fun CalModeBar() {
    val modes = listOf("month", "week", "day", "agenda", "year")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (m in modes) {
            val on = m == CalView.mode
            Text(L("cal.mode.$m"),
                Modifier.background(if (on) Theme.c.accentSoft else Theme.c.surface2, RoundedCornerShape(50))
                    .clickable { CalView.mode = m; CalView.save() }.padding(horizontal = 14.dp, vertical = 7.dp),
                color = if (on) Theme.c.accent else Theme.c.text, fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

/** Görünüm menüsüne ek: hicri aylarla gezme, ülke ve gösterilen türler */
@Composable
fun CalExtraMenuItems(onCountry: () -> Unit) {
    DropdownMenuItem(
        text = { Text(L("cal.hijriNav")) },
        trailingIcon = { Checkbox(checked = CalView.hijriNav, onCheckedChange = null) },
        onClick = { CalView.hijriNav = !CalView.hijriNav; CalView.save() },
    )
    DropdownMenuItem(
        text = { Text(L("cal.country") + ": " + (if (CalView.country == "TR") WorldHol.countryName("TR") else WorldHol.countryName(CalView.country))) },
        onClick = onCountry,
    )
}

/** Ülke ve tür seçim penceresi */
@Composable
fun CalCountryDialog(onClose: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text(L("common.done"), color = Theme.c.accent) } },
        title = { Text(L("cal.country")) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text(L("cal.shownTypes"), color = Theme.c.text, fontSize = if (AppFonts.serif) 19.sp else 18.sp, fontFamily = AppFonts.heading, fontWeight = FontWeight.Medium)
                val types = if (CalView.country == "TR") listOf("holiday", "half", "religious", "special", "school") else listOf("holiday", "religious")
                for (t in types) {
                    Row(Modifier.fillMaxWidth().clickable {
                        CalView.hidden = if (t in CalView.hidden) CalView.hidden - t else CalView.hidden + t
                        CalView.save()
                    }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = t !in CalView.hidden, onCheckedChange = null)
                        Box(Modifier.size(8.dp).background(AppData.color(t), CircleShape))
                        Text("  " + calendarTypeLabel(t), color = Theme.c.text, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Theme.c.border)
                val all = listOf("TR") + WorldHol.countries.sortedBy { WorldHol.countryName(it) }
                for (cc in all) {
                    val on = cc == CalView.country
                    Text((if (on) "✓ " else "    ") + WorldHol.countryName(cc),
                        Modifier.fillMaxWidth().clickable { CalView.country = cc; CalView.save() }.padding(vertical = 8.dp),
                        color = if (on) Theme.c.accent else Theme.c.text, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
                Text(L("cal.countryNote"), color = Theme.c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        },
        containerColor = Theme.c.bg,
    )
}

/** Hafta: seçili günün haftası (pazartesi başlar), her gün bir satır */
@Composable
fun WeekView(selected: LocalDate, today: LocalDate, device: Map<LocalDate, List<DeviceCalendar.Event>>, onPick: (LocalDate) -> Unit) {
    val start = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    AppCard(padding = 4) {
        for (i in 0..6) {
            val d = start.plusDays(i.toLong())
            val ev = calEvents(d)
            val mine = device[d].orEmpty()
            Row(Modifier.fillMaxWidth().clickable { onPick(d) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.width(54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(L("calendar.weekdaysShort.$i"), color = if (i >= 5) Theme.c.accent else Theme.c.muted, fontSize = 12.sp)
                    Box(Modifier.size(32.dp).background(if (d == today) Theme.c.accent else if (d == selected) Theme.c.accentSoft else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text("${d.dayOfMonth}", color = if (d == today) Theme.c.bg else Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (e in ev.distinctBy { it.key }) EventChip(e.name, AppData.color(e.type))
                    for (e in mine) EventChip((if (e.allDay) "" else Fmt.time(e.begin, ZoneId.systemDefault()) + " ") + e.title, e.color)
                    if (ev.isEmpty() && mine.isEmpty()) Text("–", color = Theme.c.border)
                }
            }
            if (i < 6) HorizontalDivider(Modifier.padding(start = 66.dp), color = Theme.c.border.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun EventChip(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).background(color, RoundedCornerShape(2.dp)))
        Text("  $text", color = Theme.c.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Gün: tüm gün olanlar üstte (bayramda çizimli kart), altında 24 saatlik zaman çizelgesi (etkinlikler ve namaz vakitleri) */
@Composable
fun DayTimeline(d: LocalDate, device: List<DeviceCalendar.Event>) {
    val ev = calEvents(d)
    val zone = ZoneId.systemDefault()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ev.any { it.key.startsWith("rb") || it.key.startsWith("kb") } && ev.none { it.key.endsWith("Eve") }) BayramCard(ev.first { it.key.startsWith("rb") || it.key.startsWith("kb") }.name)
        val allDay = ev.map { it.name to AppData.color(it.type) } + device.filter { it.allDay }.map { it.title to it.color }
        if (allDay.isNotEmpty()) AppCard {
            Text(L("calendar.allDay"), color = Theme.c.muted, fontSize = 12.sp)
            for ((n, c) in allDay) EventChip(n, c)
        }
        // Namaz vakitleri zaman çizelgesinde (Vakitler açıksa)
        val prayers = if (Features.isOn(Tab.PRAYER)) AppPlace.current().entries(d).map { it.at.atZone(zone) to it.prayer.title } else emptyList()
        val timed = device.filter { !it.allDay }
        AppCard(padding = 4) {
            for (h in 0..23) {
                val items = timed.filter { it.begin.atZone(zone).toLocalDate() == d && it.begin.atZone(zone).hour == h }
                val pr = prayers.filter { it.first.hour == h }
                Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Ltr { Text("%02d:00".format(h), Modifier.width(52.dp), color = Theme.c.muted, fontSize = 12.sp, style = Tnum) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        for ((t, name) in pr) Text("☾ $name · " + Fmt.time(t), color = Theme.c.accent, fontSize = 13.sp)
                        for (e in items) {
                            Box(Modifier.fillMaxWidth().background(e.color.copy(alpha = 0.18f), RoundedCornerShape(6.dp)).padding(6.dp)) {
                                Text(Fmt.time(e.begin, zone) + "–" + Fmt.time(e.end, zone) + "  " + e.title, color = Theme.c.text, fontSize = 13.sp)
                            }
                        }
                    }
                }
                if (h < 23) HorizontalDivider(color = Theme.c.border.copy(alpha = 0.35f))
            }
        }
    }
}

/** Bayram kartı: telefonda çizilen hilal, yıldız ve fener; altında tebrik */
@Composable
fun BayramCard(name: String) {
    Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.linearGradient(listOf(Color(0xFF15324F), Color(0xFF2E5C7A), Color(0xFFB8860B))), RoundedCornerShape(16.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Hilal
            val c = Offset(w * 0.82f, h * 0.32f)
            drawCircle(Color(0xFFFFE9A8), radius = h * 0.2f, center = c)
            drawCircle(Color(0xFF214566), radius = h * 0.17f, center = Offset(c.x + h * 0.08f, c.y - h * 0.04f))
            // Yıldızlar
            for ((x, y) in listOf(0.12f to 0.2f, 0.3f to 0.12f, 0.5f to 0.25f, 0.65f to 0.1f, 0.2f to 0.45f)) drawCircle(Color.White.copy(alpha = 0.8f), 2.5f, Offset(w * x, h * y))
            // Fener
            val fx = w * 0.12f
            drawLine(Color(0xFFFFE9A8), Offset(fx, 0f), Offset(fx, h * 0.35f), strokeWidth = 2f)
            val p = Path().apply {
                moveTo(fx - 14f, h * 0.38f); lineTo(fx + 14f, h * 0.38f); lineTo(fx + 20f, h * 0.62f); lineTo(fx, h * 0.72f); lineTo(fx - 20f, h * 0.62f); close()
            }
            drawPath(p, Color(0xFFFFC857))
            drawCircle(Color(0xFFFFF4C2), 7f, Offset(fx, h * 0.53f))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(L("cal.bayram"), color = Color(0xFFFFE9A8), fontSize = 15.sp)
        }
    }
}

/** Ajanda: seçili günden başlayarak 60 gün, özel gün ya da etkinlik olan günler */
@Composable
fun AgendaView(from: LocalDate, device: (LocalDate) -> List<DeviceCalendar.Event>, onPick: (LocalDate) -> Unit) {
    val days = remember(from, CalView.country, CalView.hidden) { (0L until 60L).map { from.plusDays(it) } }
    val shown = days.filter { calEvents(it).isNotEmpty() || device(it).isNotEmpty() }
    if (shown.isEmpty()) { EmptyState(androidx.compose.material.icons.Icons.Filled.DateRange, L("cal.agendaEmpty"), null); return }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (d in shown) {
            AppCard(Modifier.clickable { onPick(d) }) {
                Text(Fmt.longDate(d), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                for (e in calEvents(d).distinctBy { it.key }) EventChip(e.name, AppData.color(e.type))
                for (e in device(d)) EventChip((if (e.allDay) "" else Fmt.time(e.begin, ZoneId.systemDefault()) + " ") + e.title, e.color)
            }
        }
    }
}

/** Yıl: 12 küçük ay, her gün özel gün ve etkinlik sayısına göre koyulaşır (yoğunluk ısı haritası) */
@Composable
fun YearView(year: Int, today: LocalDate, deviceCount: (LocalDate) -> Int, onMonth: (YearMonth) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in 0..3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (col in 0..2) {
                    val ym = YearMonth.of(year, row * 3 + col + 1)
                    Column(Modifier.weight(1f).clickable { onMonth(ym) }) {
                        Text(Fmt.month(ym.monthValue), color = Theme.c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        val lead = ym.atDay(1).dayOfWeek.value - 1
                        val cells = lead + ym.lengthOfMonth()
                        for (r in 0 until (cells + 6) / 7) {
                            Row {
                                for (c in 0..6) {
                                    val n = r * 7 + c - lead + 1
                                    Box(Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp), contentAlignment = Alignment.Center) {
                                        if (n in 1..ym.lengthOfMonth()) {
                                            val d = ym.atDay(n)
                                            val k = calEvents(d).size + deviceCount(d)
                                            val bg = if (k == 0) Color.Transparent else Theme.c.accent.copy(alpha = (0.18f + 0.22f * minOf(k, 4)).coerceAtMost(0.95f))
                                            Box(Modifier.fillMaxSize().background(bg, RoundedCornerShape(2.dp))
                                                .then(if (d == today) Modifier.background(Color.Transparent).padding(0.dp) else Modifier), contentAlignment = Alignment.Center) {
                                                Text("$n", fontSize = 7.sp, color = if (d == today) Theme.c.accent else if (k >= 3) Theme.c.bg else Theme.c.text,
                                                    fontWeight = if (d == today) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(L("cal.density"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/** Hicri ay ızgarası: hicri günler, altında küçük miladi gün; ay başı tablodan (Diyanet) */
@Composable
fun HijriMonthGrid(hy: Int, hm: Int, today: LocalDate, selected: LocalDate, onPick: (LocalDate) -> Unit) {
    val m = Hijri.month(hy, hm) ?: return
    val (first, len) = m
    val lead = first.dayOfWeek.value - 1
    val cells = lead + len
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            for (i in 0..6) Text(L("calendar.weekdaysShort.$i"), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp,
                color = if (i >= 5) Theme.c.accent else Theme.c.muted)
        }
        for (row in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val n = row * 7 + col - lead + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (n in 1..len) {
                            val d = first.plusDays((n - 1).toLong())
                            val ev = calEvents(d)
                            Column(
                                Modifier.fillMaxSize().background(
                                    if (d == today) Theme.c.accent else if (d == selected) Theme.c.accentSoft else Color.Transparent, RoundedCornerShape(10.dp),
                                ).clickable { onPick(d) },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                            ) {
                                Text("$n", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (d == today) Theme.c.bg else if (ev.any { it.type == "holiday" }) AppData.color("holiday") else Theme.c.text)
                                Text("${d.dayOfMonth}" + if (d.dayOfMonth == 1 || n == 1) " " + Fmt.month(d.monthValue).take(3) else "",
                                    fontSize = 9.sp, color = if (d == today) Theme.c.bg else Theme.c.muted, maxLines = 1)
                                if (ev.isNotEmpty()) Box(Modifier.size(5.dp).background(AppData.color(ev.first().type), CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}
