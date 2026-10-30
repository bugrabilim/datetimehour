package tr.bumba.time

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.bumba.time.core.Prayer
import java.time.LocalDate
import java.time.YearMonth

/**
 * Vakitler (iOS'taki PrayerTab): üstte zil (vakit bildirimleri), konum, sıradaki vakte kalan süre, bugünün
 * vakitleri, aylık çizelge, kıble ve yakındaki camiler, sıradaki Ramazan'ın imsakiyesi.
 */
@Composable
fun PrayerScreen() {
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    val place = AppPlace.current()
    val ramadan = Imsakiye.nextRamadan(place.today())
    when (page) {
        "monthly" -> { BackHandler { page = null }; MonthlyPrayers { page = null }; return }
        "notify" -> { BackHandler { page = null }; NotificationSettingsScreen { page = null }; return }
        "qibla" -> { BackHandler { page = null }; QiblaScreen { page = null }; return }
        "imsakiye" -> if (ramadan != null) {
            BackHandler { page = null }
            ImsakiyeScreen(place, ramadan) { page = null }
            return
        }
    }
    val now = rememberNow()
    val today = place.today(now)
    val entries = place.entries(today)
    val next = place.next(now)
    NotifyPrefs.load()
    Column(Modifier.fillMaxSize()) {
        SubBar(L("tab.prayer"), actions = {
            IconButton(onClick = { page = "notify" }) {
                Icon(
                    if (NotifyPrefs.enabled) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    L("notify.heading"), tint = Theme.c.accent,
                )
            }
        })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlacePicker()
            SavedPlaceChips()
            // Bugünün miladi ve hicri tarihi (Diyanet takvimi)
            Hijri.date(today)?.let { Text(Fmt.longDate(today) + " · " + it.text, color = Theme.c.muted, fontSize = 13.sp) }
            if (next != null) {
                Text(T("prayer.nextIn", next.prayer.title), color = Theme.c.muted, fontSize = 15.sp)
                Text(
                    Fmt.countdown(next.at.epochSecond - now.epochSecond),
                    color = Theme.c.accent, fontSize = 52.sp, fontWeight = FontWeight.Bold, style = Tnum,
                )
            }
            AppCard {
                SectionTitle(L("prayer.today"))
                for (e in entries) {
                    val on = next?.prayer == e.prayer && next.at == e.at
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (on) Theme.c.accentSoft else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                    ) {
                        Text(e.prayer.title, Modifier.weight(1f), color = Theme.c.text, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 17.sp)
                        Text(Fmt.hm(e.at, place.zone), color = Theme.c.text, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 17.sp, style = Tnum)
                    }
                }
            }
            LinkCard(Icons.Filled.TableChart, L("prayer.monthly")) { page = "monthly" }
            LinkCard(Icons.Filled.Explore, L("qibla.link")) { page = "qibla" }
            if (ramadan != null) LinkCard(Icons.Filled.NightsStay, T("imsakiye.title", ramadan.start.year)) { page = "imsakiye" }
        }
    }
}

@Composable
private fun LinkCard(icon: ImageVector, title: String, onClick: () -> Unit) {
    AppCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Theme.c.accent)
            Text("  $title", Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
        }
    }
}

/** Aylık vakit çizelgesi: gün gün altı vakit; bugün vurgulu */
@Composable
fun MonthlyPrayers(onBack: () -> Unit) {
    val place = AppPlace.current()
    val today = place.today()
    var ym by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(ym)
    Column(Modifier.fillMaxSize()) {
        SubBar(L("prayer.monthly"), onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { ym = month.minusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("prayer.prevMonth"), tint = Theme.c.accent)
            }
            Text("${Fmt.month(month.monthValue)} ${month.year} · ${place.name}", Modifier.weight(1f), color = Theme.c.text,
                fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { ym = month.plusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("prayer.nextMonth"), tint = Theme.c.accent)
            }
        }
        val cols = listOf(L("prayer.colDay")) + Prayer.entries.map { it.title }
        val scroll = rememberScrollState()
        LazyColumn(Modifier.fillMaxSize().horizontalScroll(scroll).padding(horizontal = 12.dp)) {
            item { TableRow(cols, header = true, highlight = false) }
            items((1..month.lengthOfMonth()).toList()) { d ->
                val day = LocalDate.of(month.year, month.monthValue, d)
                val e = place.entries(day).associate { it.prayer to Fmt.hm(it.at, place.zone) }
                TableRow(
                    listOf("$d ${Fmt.weekdayShort(day)}") + Prayer.entries.map { e[it] ?: "–" },
                    header = false, highlight = day == today,
                )
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean, highlight: Boolean) {
    Row(
        Modifier.background(if (highlight) Theme.c.accentSoft else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp))
            .padding(vertical = 7.dp),
    ) {
        cells.forEachIndexed { i, s ->
            Text(
                s, Modifier.width(if (i == 0) 74.dp else 56.dp), fontSize = if (header) 12.sp else 14.sp,
                color = if (header) Theme.c.muted else Theme.c.text,
                fontWeight = if (header || highlight) FontWeight.SemiBold else FontWeight.Normal,
                style = if (i > 0 && !header) Tnum else TextStyle.Default,
            )
        }
    }
}
