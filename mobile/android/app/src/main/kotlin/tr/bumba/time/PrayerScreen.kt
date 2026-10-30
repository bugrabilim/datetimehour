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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
        "method" -> { BackHandler { page = null }; PrayerMethodScreen { page = null }; return }
        // İbadet bölümü (Religion.kt)
        "quran" -> { BackHandler { page = null }; QuranScreen { page = null }; return }
        "duas" -> { BackHandler { page = null }; DuasScreen { page = null }; return }
        "zikir" -> { BackHandler { page = null }; ZikirScreen { page = null }; return }
        "track" -> { BackHandler { page = null }; TrackerScreen { page = null }; return }
        "zakat" -> { BackHandler { page = null }; ZakatScreen { page = null }; return }
        "esma" -> { BackHandler { page = null }; EsmaScreen { page = null }; return }
        "hadith" -> { BackHandler { page = null }; HadithScreen { page = null }; return }
        "guide" -> { BackHandler { page = null }; PrayerGuideScreen({ page = null }) { page = "duas" }; return }
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
        val shareCtx = androidx.compose.ui.platform.LocalContext.current
        SubBar(L("tab.prayer"), actions = {
            // Bugünün vakitlerini görsel kart olarak paylaş (rakip analizi #16)
            IconButton(onClick = {
                Share.image(shareCtx, ShareCards.prayer(place, java.time.Instant.now()), "vakitler-${place.today(now)}.png", L("share.prayer"))
            }) { Icon(Icons.Filled.Share, L("share.prayer"), tint = Theme.c.accent) }
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
            SunMoonArc(now, place)
            KerahatCard(entries, place)
            // Bildirim alanında ve kilit ekranında sıradaki vakit (iOS'taki Live Activity karşılığı)
            val context = androidx.compose.ui.platform.LocalContext.current
            var liveOn by remember { mutableStateOf(Prefs.store.getBoolean(LiveNotifications.PRAYER_KEY, false)) }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("live.prayer.toggle"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    androidx.compose.material3.Switch(liveOn, { on ->
                        liveOn = on
                        Prefs.store.edit().putBoolean(LiveNotifications.PRAYER_KEY, on).apply()
                        LiveNotifications.sync(context)
                    }, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                Text(L("live.prayer.noteAndroid"), color = Theme.c.muted, fontSize = 12.sp)
            }
            LinkCard(Icons.Filled.TableChart, L("prayer.monthly")) { page = "monthly" }
            LinkCard(Icons.Filled.Tune, L("prayer.method.app") + if (PrayerMethod.isCustom) " · " + PrayerMethod.name(PrayerMethod.id) else "") { page = "method" }
            LinkCard(Icons.Filled.Explore, L("qibla.link")) { page = "qibla" }
            if (ramadan != null) LinkCard(Icons.Filled.NightsStay, T("imsakiye.title", ramadan.start.year)) { page = "imsakiye" }
            Text(L("worship.title"), Modifier.fillMaxWidth().padding(top = 6.dp), color = Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            LinkCard(Icons.AutoMirrored.Filled.MenuBook, L("quran.title")) { page = "quran" }
            LinkCard(Icons.Filled.VolunteerActivism, L("dua.title")) { page = "duas" }
            LinkCard(Icons.Filled.AutoAwesome, L("esma.title")) { page = "esma" }
            LinkCard(Icons.Filled.FormatQuote, L("hadith.title")) { page = "hadith" }
            LinkCard(Icons.Filled.SelfImprovement, L("guide.title")) { page = "guide" }
            LinkCard(Icons.Filled.TouchApp, L("zikir.title")) { page = "zikir" }
            LinkCard(Icons.Filled.TaskAlt, L("track.title")) { page = "track" }
            LinkCard(Icons.Filled.Calculate, L("zakat.title")) { page = "zakat" }
        }
    }
}

/** Kerahat vakitleri (rakip analizi #12; sitedeki namaz sayfasıyla aynı): yaklaşık 45 dk, açılır kart */
@Composable
private fun KerahatCard(entries: List<AppPlace.Entry>, place: AppPlace) {
    fun at(p: Prayer) = entries.firstOrNull { it.prayer == p }?.at
    val sr = at(Prayer.SUNRISE) ?: return
    val dh = at(Prayer.DHUHR) ?: return
    val mg = at(Prayer.MAGHRIB) ?: return
    val k = tr.bumba.time.core.PrayerTimes.KERAHAT_MIN * 60L
    var open by remember { mutableStateOf(false) }
    AppCard(Modifier.clickable { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("prayer.kerahatHeading"), Modifier.weight(1f), color = Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Theme.c.muted)
        }
        if (open) {
            for ((key, a, b) in listOf(Triple("israk", sr, sr.plusSeconds(k)), Triple("istiva", dh.minusSeconds(k), dh), Triple("isfirar", mg.minusSeconds(k), mg))) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(L("prayer.kerahatNames.$key"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Text(Fmt.hm(a, place.zone) + "–" + Fmt.hm(b, place.zone), color = Theme.c.text, fontSize = 15.sp, style = Tnum)
                }
            }
            Text(L("prayer.kerahatNote"), color = Theme.c.muted, fontSize = 12.sp)
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
