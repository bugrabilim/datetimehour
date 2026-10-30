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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
            MoonMonth(ym, place, place.today(now))
        }
        val today = place.today(now)
        val rs = moonRiseSet(place, today)
        AppCard {
            Row2(L("nature.moonrise"), moonTime(rs.rise, place, "nature.moonNoRise"))
            Row2(L("nature.moonset"), moonTime(rs.set, place, "nature.moonNoSet"))
            Text(place.name, color = Theme.c.muted, fontSize = 12.sp)
        }
        CrescentCard(place, now)
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
private fun MoonMonth(ym: YearMonth, place: AppPlace, today: LocalDate) {
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
                                    ).padding(top = 3.dp),
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
