package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.bumba.time.core.Nature
import tr.bumba.time.core.PrayerTimes
import java.time.Instant
import java.time.temporal.ChronoUnit

private val SEASON_KEYS = listOf("spring", "summer", "autumn", "winter")
private val SEASON_EMOJI = listOf("🌸", "☀️", "🍂", "⛄")
private val SEASON_COLOR = listOf(Color(0xFF4CAF50), Color(0xFFFA9E1C), Color(0xFFCC6B29), Color(0xFF4087D9))

/** Yılın dört mevsim anı: (0 ilkbahar, 1 yaz, 2 sonbahar, 3 kış) */
private fun instants(year: Int): List<Pair<Int, Instant>> {
    val s = Nature.seasons(year)
    return listOf(s.march, s.june, s.september, s.december).mapIndexed { i, ms -> i to Instant.ofEpochMilli(ms) }
}

/**
 * Mevsimler (iOS'taki çocuklara yönelik sayfa gibi): şu anki mevsim ve ilerleyişi, neden mevsim olur, ışığın açısı,
 * "kış 1 Aralık'ta mı 21 Aralık'ta mı", güney yarımküre, ilginç bilgiler; Güneş, Ay ve mevsim başlangıçları.
 */
@Composable
fun SeasonsScreen() {
    // Ay takvimi ve tutulmalar alt sayfası (Ay kartına dokununca)
    var moonPage by rememberSaveable { mutableStateOf(false) }
    if (moonPage) {
        BackHandler { moonPage = false }
        Column(Modifier.fillMaxSize()) {
            SubBar(L("moon.calendarLink"), onBack = { moonPage = false })
            MoonScreen()
        }
        return
    }
    val now = Instant.now()
    val place = AppPlace.current()
    val year = now.atZone(place.zone).year
    val marks = (instants(year - 1) + instants(year) + instants(year + 1)).sortedBy { it.second }
    val current = marks.last { !it.second.isAfter(now) }
    val next = marks.first { it.second.isAfter(now) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Hero(current, next, now, place)
        Info("🌍", "kids.why.title", "kids.why.text", "kids.why.myth")
        Info("🔦", "kids.light.title", "kids.light.text")
        Info("📅", "kids.dates.title", "kids.dates.text")
        Info("🏖️", "kids.south.title", "kids.south.text")
        AppCard {
            Text("💡 " + L("kids.facts.title"), color = Theme.c.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            for (i in 1..5) Text("• " + L("kids.fact$i"), color = Theme.c.text, fontSize = 15.sp)
        }
        SunMoon(place, now) { moonPage = true }
        AppCard {
            SectionTitle(L("nature.tableHeading"))
            for ((i, at) in instants(year)) {
                Row2("${SEASON_EMOJI[i]} ${L("nature.seasons.${SEASON_KEYS[i]}")}", Fmt.shortDateTime(at, place.zone))
            }
            Text(L("nature.tableLead"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Hero(current: Pair<Int, Instant>, next: Pair<Int, Instant>, now: Instant, place: AppPlace) {
    val color = SEASON_COLOR[current.first]
    val total = (next.second.epochSecond - current.second.epochSecond).toFloat()
    val done = ((now.epochSecond - current.second.epochSecond) / total).coerceIn(0f, 1f)
    val days = ChronoUnit.DAYS.between(place.today(now), next.second.atZone(place.zone).toLocalDate())
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.08f))), RoundedCornerShape(20.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(SEASON_EMOJI[current.first], fontSize = 64.sp)
        Text(T("nature.current", L("nature.seasons.${SEASON_KEYS[current.first]}")), color = Theme.c.text, fontSize = 26.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(L("kids.progress"), color = Theme.c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().height(20.dp).background(Theme.c.surface2, RoundedCornerShape(10.dp))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(done.coerceAtLeast(0.05f)).background(color, RoundedCornerShape(10.dp)))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(Fmt.shortDate(current.second.atZone(place.zone).toLocalDate()), Modifier.weight(1f), color = Theme.c.muted, fontSize = 11.sp)
            Text(Fmt.shortDate(next.second.atZone(place.zone).toLocalDate()), color = Theme.c.muted, fontSize = 11.sp)
        }
        Text(T("nature.nextIn", L("nature.seasons.${SEASON_KEYS[next.first]}"), days), color = Theme.c.text,
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Info(emoji: String, title: String, vararg texts: String) {
    AppCard {
        Text("$emoji ${L(title)}", color = Theme.c.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        texts.forEachIndexed { i, t ->
            Text(
                L(t), color = Theme.c.text, fontSize = 15.sp,
                modifier = if (i > 0) Modifier.fillMaxWidth().background(Theme.c.accentSoft, RoundedCornerShape(12.dp)).padding(12.dp) else Modifier,
            )
        }
    }
}

@Composable
private fun SunMoon(place: AppPlace, now: Instant, openMoon: () -> Unit) {
    val day = place.today(now)
    // Güneşin doğuşu ve batışı (vakit düzeltmeleri olmadan)
    val p = PrayerTimes.Parameters(adjustments = PrayerTimes.Adjustments(sunrise = 0.0, dhuhr = 0.0, maghrib = 0.0))
    val t = PrayerTimes.times(day, place.lat, place.lon, place.zone, p)
    val moon = Nature.moon(now.toEpochMilli())
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppCard(Modifier.weight(1f)) {
            Text("🌅 " + L("nature.sunHeading"), color = Theme.c.text, fontWeight = FontWeight.SemiBold)
            fun hm(h: Double?) = h?.let { "%02d:%02d".format((it * 60).toInt() / 60 % 24, (it * 60).toInt() % 60) } ?: "–"
            Text("↑ ${hm(t.sunrise)}   ↓ ${hm(t.maghrib)}", color = Theme.c.text)
            if (t.sunrise != null && t.maghrib != null) {
                val len = ((t.maghrib!! - t.sunrise!!) * 60).toInt()
                Text(T("seasons.dayLengthValue", len / 60, len % 60), color = Theme.c.muted, fontSize = 12.sp)
            }
            Text(place.name, color = Theme.c.muted, fontSize = 11.sp)
        }
        AppCard(Modifier.weight(1f).clickable(onClick = openMoon)) {
            Text(L("nature.moonHeading"), color = Theme.c.text, fontWeight = FontWeight.SemiBold)
            Text("${MOON_PHASE_EMOJI[moon.phaseIndex]} ${L("nature.phases.${MOON_PHASE_KEYS[moon.phaseIndex]}")}", color = Theme.c.text)
            Text(T("nature.illumination", Math.round(moon.illumination * 100)), color = Theme.c.muted, fontSize = 12.sp)
            // Ay doğuşu ve batışı (yerin bugünü)
            val rs = moonRiseSet(place, day)
            Text("↑ ${moonTime(rs.rise, place, "nature.moonNoRise")}   ↓ ${moonTime(rs.set, place, "nature.moonNoSet")}", color = Theme.c.text)
            Text(L("moon.calendarLink") + " ›", color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    // Altın ve mavi saat (rakip analizi #15; sitedeki Mevsimler ile aynı: Güneş +6°/−4°/−6°)
    val off = place.zone.rules.getOffset(day.atTime(12, 0).atZone(place.zone).toInstant()).totalSeconds / 3600.0
    val light = PrayerTimes.lightHours(day.year, day.monthValue, day.dayOfMonth, place.lat, place.lon, off)
    fun hm(h: Double?): String = h?.let { val m = Math.round(it * 60).toInt(); "%02d:%02d".format(java.util.Locale.ROOT, (m / 60 + 24) % 24, m % 60) } ?: "–"
    fun span(r: Pair<Double?, Double?>) = if (r.first == null || r.second == null) "–" else hm(r.first) + "–" + hm(r.second)
    AppCard {
        for ((key, icon, r) in listOf(Triple("nature.blueAm", "🔵", light.blueAm), Triple("nature.goldenAm", "🟠", light.goldenAm),
                Triple("nature.goldenPm", "🟠", light.goldenPm), Triple("nature.bluePm", "🔵", light.bluePm))) {
            Row(Modifier.fillMaxWidth()) {
                Text("$icon ${L(key)}", Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                Text(span(r), color = Theme.c.text, fontSize = 15.sp, style = Tnum)
            }
        }
        Text(L("nature.lightNote"), color = Theme.c.muted, fontSize = 12.sp)
    }
}
