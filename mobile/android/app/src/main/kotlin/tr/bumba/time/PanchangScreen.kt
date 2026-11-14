package tr.bumba.time

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.bumba.time.core.PrayerTimes
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/*
 * Panchang ve Rahu Kaal (10 Kasım 2026, Hindistan araştırması #9; sitedeki /panchang/ sayfası ve iOS PanchangView ile aynı):
 * seçili yer ve gün için gün doğumu/batımı, geleneksel olarak kaçınılan (Rahu Kaal, Yamaganda, Gulika) ve uygun sayılan
 * (Abhijit, Brahma muhurta) dilimler, yedi günün Rahu Kaal saatleri. Hesap telefonda (core PrayerTimes.hinduKaal).
 * Hindu takviminin günlük satırı (tithi, nakşatra, ay, Vikram Samvat) Yeni Delhi gün doğumuna göre calendars.json'dan;
 * Hindu takvimi Takvim'de açık olmasa da gösterilir. Yorum yok: dilimler gelenek olarak, tarafsız sözlerle yazılır.
 */

/** Bir yer ve günün dilimleri; ondalık saatler o günün yerel öğlesinden sayılır (yaz saati geçişinde de doğru an) */
class PanchangDay(val date: LocalDate, val place: AppPlace) {
    private val noon: Instant = date.atTime(12, 0).atZone(place.zone).toInstant()
    private val offset: Double = place.zone.rules.getOffset(noon).totalSeconds / 3600.0
    val kaal: PrayerTimes.HinduKaal? = PrayerTimes.hinduKaal(date.year, date.monthValue, date.dayOfMonth, place.lat, place.lon, offset)

    fun at(h: Double): Instant = noon.plusSeconds((Math.round(h * 60) - 12 * 60) * 60)

    /** Saat (kullanıcının 24/12 seçimiyle) */
    fun clock(h: Double?): String = h?.let { at(it) }?.let { listOf(Fmt.time(it, place.zone), Fmt.ampm(it.atZone(place.zone))).filter { s -> s.isNotEmpty() }.joinToString(" ") } ?: "—"
    fun span(r: Pair<Double, Double>?): String = r?.let { clock(it.first) + "–" + clock(it.second) } ?: "—"
}

/** Hindu takviminin o günkü satırı (calendars.json; Yeni Delhi gün doğumu); veri yoksa null */
fun hinduLine(d: LocalDate): String? {
    ExtraCals.ensure()
    val c = ExtraCals.find("hindu") ?: return null
    return ExtraCals.dailyText(c, d)
}

@Composable
fun PanchangScreen() {
    val place = AppPlace.current()
    val today = place.today()
    var date by remember(place.id) { mutableStateOf(today) }
    val p = remember(place.id, place.lat, place.lon, date) { PanchangDay(date, place) }
    val week = remember(place.id, place.lat, place.lon, date) { (0..6).map { PanchangDay(date.plusDays(it.toLong()), place) } }
    val c = Theme.c
    val k = p.kaal

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Uzun açıklamalar ekranda değil "Nasıl çalışır?" alt sayfasında (doğal tasarım madde 13)
        HowItWorks(panchangHowSections())
        PlacePicker()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (date > today.minusDays(366)) date = date.minusDays(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("photoApp.prevDay"), tint = c.accent)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.longDate(date), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                if (date != today) Text(L("client.today"), Modifier.clip(RoundedCornerShape(50)).clickable { date = today }
                    .padding(horizontal = 10.dp, vertical = 2.dp), color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = { if (date < today.plusDays(366)) date = date.plusDays(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("photoApp.nextDay"), tint = c.accent)
            }
        }
        if (k == null) Text(L("panchang.polar"), color = c.muted, fontSize = 13.sp)
        AppCard {
            SectionTitle(L("panchang.hindu"))
            Text(hinduLine(date) ?: L("panchang.hinduNone"), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            KaalRow("sunrise", c.terra, L("nature.sunrise"), p.clock(k?.sunrise))
            KaalRow("sunset", c.terra, L("nature.sunset"), p.clock(k?.sunset))
            Text(L("panchang.hinduNote"), color = c.muted, fontSize = 13.sp)
        }
        AppCard {
            SectionTitle(L("panchang.avoid"))
            KaalRow("hourglass", c.dusk, L("panchang.rahu"), p.span(k?.rahu))
            KaalRow("hourglass", c.dusk, L("panchang.yama"), p.span(k?.yamaganda))
            KaalRow("hourglass", c.dusk, L("panchang.gulika"), p.span(k?.gulika))
        }
        AppCard {
            SectionTitle(L("panchang.good"))
            KaalRow("sun", c.gold, L("panchang.abhijit"), p.span(k?.abhijit))
            KaalRow("sunrise", c.gold, L("panchang.brahma"), p.span(k?.brahma))
            if (date.dayOfWeek == DayOfWeek.WEDNESDAY) Text(L("panchang.abhijit") + ": " + L("panchang.wed"), color = c.muted, fontSize = 13.sp)
        }
        AppCard {
            SectionTitle(L("panchang.week"))
            for (d in week) Row(Modifier.fillMaxWidth().heightIn(min = 32.dp).clickable { date = d.date }, verticalAlignment = Alignment.CenterVertically) {
                Text(Fmt.weekdayShort(d.date) + " " + Fmt.shortDate(d.date), Modifier.weight(1f),
                    color = if (d.date == date) c.accent else c.text, fontSize = 15.sp, fontWeight = if (d.date == date) FontWeight.SemiBold else FontWeight.Normal)
                Text(d.span(d.kaal?.rahu), color = c.text, fontSize = 15.sp, style = Tnum)
            }
        }
        Text(L("panchang.note"), color = c.muted, fontSize = 13.sp)
    }
}

/** "Nasıl çalışır?" bölümleri (iOS'takiyle aynı) */
fun panchangHowSections(): List<Pair<String?, String>> = listOf(
    L("panchang.defsHeading") to L("panchang.defs.0"),
    null to L("panchang.defs.1"),
    null to L("panchang.defs.2"),
    L("panchang.hindu") to L("panchang.hinduNote"),
)

@Composable
private fun KaalRow(icon: String, tint: Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 34.dp), verticalAlignment = Alignment.CenterVertically) {
        DuoIcon(icon, 18.dp, tint)
        Spacer(Modifier.width(8.dp))
        Text(label, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        Text(value, Modifier.padding(start = 8.dp), color = Theme.c.text, fontSize = 15.sp, style = Tnum, textAlign = TextAlign.End)
    }
}
