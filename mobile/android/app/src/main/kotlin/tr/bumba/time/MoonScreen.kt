package tr.bumba.time

import android.icu.text.LocaleDisplayNames
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import tr.bumba.time.core.Nature
import java.time.Instant
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.clickable
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.floor

val MOON_PHASE_KEYS = listOf("new", "waxingCrescent", "firstQuarter", "waxingGibbous", "full", "waningGibbous", "lastQuarter", "waningCrescent")
val MOON_PHASE_EMOJI = listOf("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")

/** Yerin o günü (gece yarısından ertesi gece yarısına) içinde Ay'ın doğuşu ve batışı */
fun moonRiseSet(place: AppPlace, day: LocalDate): Nature.MoonRiseSet {
    val start = day.atStartOfDay(place.zone).toInstant().toEpochMilli()
    val end = day.plusDays(1).atStartOfDay(place.zone).toInstant().toEpochMilli()
    return Nature.moonRiseSet(start, place.lat, place.lon, end - start)
}

/** Ay doğuş/batış saati (dakikaya yuvarlanır) ya da "bugün doğmuyor/batmıyor" */
fun moonTime(ms: Long?, place: AppPlace, noneKey: String): String =
    ms?.let { Fmt.hm(Instant.ofEpochMilli((it + 30_000) / 60_000 * 60_000), place.zone) } ?: L(noneKey)

/** NASA tablosundaki tutulma (src/data/eclipses.json; ut: en büyük tutulma anı, UTC ms) */
data class Eclipse(val kind: String, val ut: Long, val type: String, val regions: List<String>, val path: List<String>) {
    val title: String get() = L("nature.eclipseTypes.$kind${type.replaceFirstChar { it.uppercase() }}")

    companion object {
        @Volatile private var cache: List<Eclipse>? = null

        fun all(c: android.content.Context): List<Eclipse> = cache ?: runCatching {
            val a = JSONArray(c.assets.open("eclipses.json").bufferedReader().use { it.readText() })
            fun list(o: org.json.JSONObject, k: String) = o.getJSONArray(k).let { x -> (0 until x.length()).map { x.getString(it) } }
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Eclipse(o.getString("kind"), o.getLong("ut"), o.getString("type"), list(o, "regions"), list(o, "path"))
            }
        }.getOrDefault(emptyList()).also { cache = it }

        /** Bölge ve ülke adları uygulamanın dilinde (UN M49 bölge kodları ve ISO ülke kodları) */
        fun names(codes: List<String>): String {
            val n = LocaleDisplayNames.getInstance(Strings.locale)
            return codes.joinToString(", ") { runCatching { n.regionDisplayName(it) }.getOrDefault(it) }
        }
    }
}

/** Ay takvimi (her gün öğle saatinde evre; yeni ay ve dolunay günleri adıyla) ve yaklaşan tutulmalar */
@Composable
fun MoonScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val place = AppPlace.current()
    val now = Instant.now()
    var month by rememberSaveable { mutableStateOf(YearMonth.from(place.today(now)).toString()) }
    val ym = YearMonth.parse(month)
    // Tarih kaydırıcı (2. analiz #146): bugünden −60…+60 gün; takvimde güne dokunmak da seçer
    var offset by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MoonScrubber(place, place.today(now), offset) { offset = it.coerceIn(-60, 60) }
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = ym.minusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("prayer.prevMonth"), tint = Theme.c.accent)
                }
                Text("${Fmt.month(ym.monthValue)} ${ym.year}", Modifier.weight(1f), color = Theme.c.text, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                IconButton(onClick = { month = ym.plusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("prayer.nextMonth"), tint = Theme.c.accent)
                }
            }
            MoonMonth(ym, place, place.today(now)) { d ->
                offset = java.time.temporal.ChronoUnit.DAYS.between(place.today(now), d).toInt().coerceIn(-60, 60)
            }
        }
        MoonDataCard(place, now)
        val today = place.today(now)
        val rs = moonRiseSet(place, today)
        AppCard {
            Row2(L("nature.moonrise"), moonTime(rs.rise, place, "nature.moonNoRise"))
            Row2(L("nature.moonset"), moonTime(rs.set, place, "nature.moonNoSet"))
            Text(place.name, color = Theme.c.muted, fontSize = 12.sp)
        }
        CrescentCard(place, now)
        YearMoonsCard(place, now)
        SectionTitle(L("nature.eclipseHeading"))
        Text(T("nature.eclipseLead", place.name), color = Theme.c.muted, fontSize = 13.sp)
        val f = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Strings.locale)
        for (e in Eclipse.all(context).filter { it.ut > now.toEpochMilli() - 6 * 3_600_000 }.take(6)) {
            val at = Instant.ofEpochMilli(e.ut)
            AppCard {
                Text((if (e.kind == "solar") "☀️ " else "🌕 ") + e.title, color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${at.atZone(place.zone).toLocalDate().format(f)} · ${Fmt.hm(Instant.ofEpochMilli((e.ut + 30_000) / 60_000 * 60_000), place.zone)}",
                    color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(T("nature.eclipseRegions", Eclipse.names(e.regions)), color = Theme.c.muted, fontSize = 13.sp)
                if (e.path.isNotEmpty()) Text(T("nature.eclipsePath", Eclipse.names(e.path)), color = Theme.c.muted, fontSize = 13.sp)
                if (e.kind == "lunar") {
                    // Ay o anda yerin ufkunun üstündeyse tutulma buradan izlenebilir
                    val up = Nature.moonAltitude(e.ut.toDouble(), place.lat, place.lon) > 0
                    Text(T(if (up) "nature.eclipseHere" else "nature.eclipseNotHere", place.name),
                        color = if (up) Theme.c.accent else Theme.c.muted, fontSize = 13.sp, fontWeight = if (up) FontWeight.SemiBold else null)
                }
            }
        }
        Text(L("nature.eclipseSafety"), color = Theme.c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(L("nature.eclipseSource"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/** Ayın günleri: evre emojisi, gün, yeni ay ve dolunay adı (hesaplanan anın yerin saatiyle düştüğü gün) */
@Composable
private fun MoonMonth(ym: YearMonth, place: AppPlace, today: LocalDate, onPick: (LocalDate) -> Unit = {}) {
    val first = ym.atDay(1)
    val startMs = first.atStartOfDay(place.zone).toInstant().toEpochMilli()
    val marks = HashMap<LocalDate, String>()
    val k0 = floor((startMs / 86_400_000.0 + 2440587.5 - 2451550.09766) / 29.530588861).toInt() - 1
    for (k in k0..k0 + 3) {
        for ((ms, key) in listOf(Nature.phaseMillis(k, false) to "new", Nature.phaseMillis(k, true) to "full")) {
            val d = Instant.ofEpochMilli(ms).atZone(place.zone).toLocalDate()
            if (YearMonth.from(d) == ym) marks[d] = key
        }
    }
    val lead = first.dayOfWeek.value - 1
    val cells = List(lead) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
    Ltr {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                for (i in 0..6) Text(L("calendar.weekdaysShort.$i"), Modifier.weight(1f), color = Theme.c.muted, fontSize = 12.sp,
                    textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            }
            for (week in cells.chunked(7)) {
                Row {
                    for (i in 0..6) {
                        val d = week.getOrNull(i)
                        Box(Modifier.weight(1f).aspectRatio(0.72f).padding(1.dp), contentAlignment = Alignment.TopCenter) {
                            if (d != null) {
                                val noon = d.atTime(12, 0).atZone(place.zone).toInstant().toEpochMilli()
                                val m = Nature.moon(noon)
                                Column(
                                    Modifier.fillMaxSize().then(
                                        if (d == today) Modifier.background(Theme.c.accentSoft, RoundedCornerShape(8.dp)) else Modifier,
                                    ).clickable { onPick(d) }.padding(top = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(MOON_PHASE_EMOJI[m.phaseIndex], fontSize = 20.sp)
                                    Text("${d.dayOfMonth}", color = Theme.c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    marks[d]?.let {
                                        Text(L("nature.phases.$it"), color = Theme.c.accent, fontSize = 9.sp, lineHeight = 10.sp,
                                            textAlign = TextAlign.Center, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.padding(2.dp))
}

/** Yılın dolunayları (2. analiz #148, #161): sayı, Mavi Ay, Kuzey Amerika adları; Hasat Ayı ekinoksa en yakın dolunay */
object YearMoons {
    data class Item(val ms: Long, val name: String, val blue: Boolean)

    fun list(year: Int, zone: java.time.ZoneId): List<Item> {
        val jan1 = java.time.LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli().toDouble()
        val k0 = Math.floor((jan1 / 86_400_000.0 + 2440587.5 - 2451550.09766) / 29.530588861).toInt() - 2
        val raw = (k0..k0 + 16).map { Nature.phaseMillis(it, true) }
            .map { it to Instant.ofEpochMilli(it).atZone(zone) }
            .filter { it.second.year == year }
        if (raw.isEmpty()) return emptyList()
        val eq = Nature.seasons(year).september
        val hv = raw.indices.minBy { Math.abs(raw[it].first - eq) }
        val names = raw.map { L("nature.ym.names.${it.second.monthValue - 1}") }.toMutableList()
        val blue = BooleanArray(raw.size)
        for (i in 1 until raw.size) if (raw[i - 1].second.monthValue == raw[i].second.monthValue) { blue[i] = true; names[i] = L("nature.ym.blueName") }
        names[hv] = L("nature.ym.harvest")
        if (raw[hv].second.monthValue == 10 && hv + 1 < raw.size) names[hv + 1] = L("nature.ym.names.9")
        return raw.indices.map { Item(raw[it].first, names[it], blue[it]) }
    }
}

@Composable
private fun YearMoonsCard(place: AppPlace, now: Instant) {
    val year = place.today(now).year
    val list = YearMoons.list(year, place.zone)
    val blue = list.firstOrNull { it.blue }
    SectionTitle(T("nature.ym.heading", year.toString()))
    AppCard {
        Text(T("nature.ym.count", year.toString(), list.size.toString()) + " " +
            (blue?.let { T("nature.ym.blue", Fmt.dayMonth(Instant.ofEpochMilli(it.ms).atZone(place.zone).toLocalDate())) } ?: L("nature.ym.noBlue")),
            color = Theme.c.text, fontSize = 14.sp)
        for (m in list) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text((if (m.blue) "🔵 " else "🌕 ") + m.name, Modifier.weight(1f), color = if (m.ms < now.toEpochMilli()) Theme.c.muted else Theme.c.text, fontSize = 15.sp)
                Text(Fmt.shortDateTime(Instant.ofEpochMilli(m.ms), place.zone), color = Theme.c.muted, fontSize = 14.sp, style = Tnum)
            }
        }
        Text(L("nature.ym.namesNote"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/** Seçili günün Ay'ı (2. analiz #146): büyük evre, aydınlık yüzde, doğuş/batış; çentikli kaydırıcı ve ‹ › ile gün gün */
@Composable
private fun MoonScrubber(place: AppPlace, today: LocalDate, offset: Int, onChange: (Int) -> Unit) {
    val day = today.plusDays(offset.toLong())
    val noon = day.atTime(if (offset == 0) java.time.LocalTime.now(place.zone).hour else 21, 0).atZone(place.zone).toInstant().toEpochMilli()
    val m = Nature.moon(noon)
    val rs = moonRiseSet(place, day)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(offset - 1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("prayer.prevMonth"), tint = Theme.c.accent) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(MOON_PHASE_EMOJI[m.phaseIndex], fontSize = 64.sp)
                Text(L("nature.phases.${MOON_PHASE_KEYS[m.phaseIndex]}"), color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(T("nature.illumination", Math.round(m.illumination * 100)), color = Theme.c.muted, fontSize = 13.sp)
                Text(Fmt.longDate(day) + "  ·  " + when {
                    offset == 0 -> L("client.today")
                    offset > 0 -> T("moon.daysAfter", offset)
                    else -> T("moon.daysBefore", -offset)
                }, color = Theme.c.text, fontSize = 13.sp, textAlign = TextAlign.Center)
                Text("↑ ${moonTime(rs.rise, place, "nature.moonNoRise")}   ↓ ${moonTime(rs.set, place, "nature.moonNoSet")}", color = Theme.c.text, fontSize = 13.sp)
            }
            IconButton(onClick = { onChange(offset + 1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("prayer.nextMonth"), tint = Theme.c.accent) }
        }
        androidx.compose.material3.Slider(offset.toFloat(), { onChange(Math.round(it)) }, valueRange = -60f..60f, steps = 119,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Theme.c.accent, activeTrackColor = Theme.c.accent))
        Text(L("moon.scrubHint"), color = Theme.c.muted, fontSize = 12.sp)
        // Paylaşım kartı (2. analiz #105)
        val ctx = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.material3.TextButton(onClick = { Share.image(ctx, ShareCards.moon(place, day), "ay.png", L("share.card")) }) {
            Text("📤 " + L("share.card"), color = Theme.c.accent)
        }
    }
}

/** Ay verileri (2. analiz #147): doğuş/batışa kalan, üst geçiş, uzaklık, yerberi/yeröte, süper Ay, dördünler */
@Composable
private fun MoonDataCard(place: AppPlace, now: Instant) {
    val ms = now.toEpochMilli()
    val data = remember(ms / 600_000, place.id) {
        val (per, apo) = MoonDetails.perigeeApogee(ms)
        Triple(per to apo, MoonDetails.superMoon(ms, per), MoonDetails.quarters(ms))
    }
    val today = place.today(now)
    fun dt(t: Long) = Fmt.dayMonth(Instant.ofEpochMilli(t).atZone(place.zone).toLocalDate()) + " " + Fmt.hm(Instant.ofEpochMilli(t), place.zone)
    fun km(v: Double) = T("sky.km", java.text.NumberFormat.getIntegerInstance(Strings.numberLocale).format(Math.round(v)))
    AppCard {
        SectionTitle(L("moon.data"))
        // Doğuşa ya da batışa kalan süre (bugün ve yarın içinde ilk olan)
        val events = listOf(today, today.plusDays(1)).flatMap { d -> val r = moonRiseSet(place, d); listOfNotNull(r.rise?.let { it to true }, r.set?.let { it to false }) }
            .filter { it.first > ms }.sortedBy { it.first }
        events.firstOrNull()?.let { (t, rise) ->
            Text(T(if (rise) "moon.riseIn" else "moon.setIn", Fmt.hm(((t - ms) / 1000).toInt())), color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        val (tr, alt) = MoonDetails.transit(place, today)
        Row2(L("moon.transit"), Fmt.hm(Instant.ofEpochMilli(tr), place.zone) + " · " + Math.round(alt) + "°")
        Row2(L("sky.moonNow"), km(MoonDetails.distanceKm(ms)))
        data.first.first?.let { Row2(L("sky.perigee"), dt(it.at) + " · " + km(it.km)) }
        data.first.second?.let { Row2(L("sky.apogee"), dt(it.at) + " · " + km(it.km)) }
        Row2(L("moon.firstQuarter"), dt(data.third.first))
        Row2(L("moon.lastQuarter"), dt(data.third.second))
        data.second?.let { Text("🌕 " + T("sky.superMoon", Fmt.longDate(Instant.ofEpochMilli(it).atZone(place.zone).toLocalDate())), color = Theme.c.accent, fontSize = 13.sp) }
    }
}
