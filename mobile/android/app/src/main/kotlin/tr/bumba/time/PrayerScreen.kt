package tr.bumba.time

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
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
        "reldays" -> { BackHandler { page = null }; ReligiousDaysScreen { page = null }; return }
        "greet" -> { BackHandler { page = null }; GreetingScreen { page = null }; return }
        "esma" -> { BackHandler { page = null }; EsmaScreen { page = null }; return }
        "hadith" -> { BackHandler { page = null }; HadithScreen { page = null }; return }
        "guide" -> { BackHandler { page = null }; PrayerGuideScreen({ page = null }) { page = "duas" }; return }
        // İbadet sayfası (2. analiz #199): bütün ibadet bölümleri tek listede
        "worship" -> { BackHandler { page = null }; WorshipHub({ page = null }) { page = it }; return }
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
            IconButton(onClick = { page = "notify" }) {
                Icon(
                    if (NotifyPrefs.enabled) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    L("notify.heading"), tint = Theme.c.accent,
                )
            }
            // Seyrek kullanılanlar "…" menüsünde (2. analiz #199): paylaş, aylık çizelge, hesap yöntemi, imsakiye
            Box {
                var menu by remember { mutableStateOf(false) }
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, L("common.more"), tint = Theme.c.accent) }
                androidx.compose.material3.DropdownMenu(menu, { menu = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L("share.prayer")) }, leadingIcon = { Icon(Icons.Filled.Share, null) },
                        onClick = {
                            menu = false
                            Share.image(shareCtx, ShareCards.prayer(place, java.time.Instant.now()), "vakitler-${place.today(now)}.png", L("share.prayer"))
                        },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L("prayer.monthly")) }, leadingIcon = { Icon(Icons.Filled.TableChart, null) },
                        onClick = { menu = false; page = "monthly" },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L("prayer.method.app") + if (PrayerMethod.id(place) != "diyanet" || PrayerMethod.hanafi(place)) " · " + PrayerMethod.name(PrayerMethod.id(place)) else "") },
                        leadingIcon = { Icon(Icons.Filled.Tune, null) }, onClick = { menu = false; page = "method" },
                    )
                    if (ramadan != null) androidx.compose.material3.DropdownMenuItem(
                        text = { Text(T("imsakiye.title", ramadan.start.year)) }, leadingIcon = { Icon(Icons.Filled.NightsStay, null) },
                        onClick = { menu = false; page = "imsakiye" },
                    )
                }
            }
        })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlacePicker()
            SavedPlaceChips()
            // Bugünün miladi ve hicri tarihi (Diyanet takvimi)
            Hijri.date(today)?.let { Text(Fmt.longDate(today) + " · " + it.text, color = Theme.c.muted, fontSize = 13.sp) }
            UpcomingReligiousLine(today) { page = "reldays" }
            if (next != null) SkyCountdown(entries, next, now)
            var trackV by remember { androidx.compose.runtime.mutableIntStateOf(0) }
            val inline = TrackX.inline
            AppCard {
                SectionTitle(L("prayer.today"))
                // Kemenag'ın ek satırları: ihtiyat imsakı sabahtan önce, duha güneşten sonra (yalnız o yöntemde)
                val extraRows = remember(today, place) { place.extraRows(today) }
                @Composable fun SubRow(label: String, at: java.time.Instant?) {
                    if (at == null) return
                    Row(Modifier.fillMaxWidth().padding(start = 42.dp, end = 10.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.weight(1f), color = Theme.c.muted, fontSize = 15.sp)
                        Text(Fmt.hm(at, place.zone), color = Theme.c.muted, fontSize = 15.sp, style = Tnum)
                    }
                }
                for (e in entries) {
                    if (e.prayer == Prayer.FAJR) SubRow(L("prayer.method.rowImsak"), extraRows?.first)
                    val on = next?.prayer == e.prayer && next.at == e.at
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (on) Theme.c.accentSoft else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp))
                            .padding(start = 10.dp, end = if (inline) 0.dp else 10.dp, top = 5.dp, bottom = 5.dp)
                            .heightIn(min = 34.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.padding(end = 10.dp)) {
                            val ic = if (on) Theme.c.accent else Theme.c.muted
                            LineIcon(LineIconMap.prayer(e.prayer), 22.dp, c1 = ic, c2 = ic)
                        }
                        Text(e.prayer.title, Modifier.weight(1f), color = Theme.c.text, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 17.sp)
                        Text(Fmt.hm(e.at, place.zone), color = Theme.c.text, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 17.sp, style = Tnum)
                        if (inline) PrayedMark(today, e, now, trackV) { trackV++ }
                    }
                    if (e.prayer == Prayer.SUNRISE) SubRow(L("prayer.method.rowDuha"), extraRows?.second)
                }
                if (inline) PrayedSummary(today, entries, now, trackV) { trackV++ }
            }
            PrayerVisual(now, place, entries)
            // Bayram namazı (Diyanet: güneş + 38 dk), ezani saat ve Türkiye Takvimi ek vakitleri yalnız Diyanet yönteminde
            val diyanet = PrayerMethod.isDiyanet(place)
            if (diyanet) BayramRow(today, now, place)
            if (diyanet) EzaniRow(today, now, place)
            RamadanCountdown(now, place)
            KerahatCard(entries, place)
            if (diyanet) ExtraTimesCard(today, place)
            // İbadet satırı (2. analiz #199): dört kare, hepsi "İbadet" sayfasında; kilit ekranı anahtarı Bildirimler'de
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(L("worship.title"), Modifier.weight(1f), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { page = "worship" }) { Text(L("countdowns.all") + " ›", color = Theme.c.accent) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorshipTile("compass", L("qibla.title"), Modifier.weight(1f)) { page = "qibla" }
                WorshipTile("book", L("quran.title"), Modifier.weight(1f)) { page = "quran" }
                WorshipTile("beads", L("zikir.title"), Modifier.weight(1f)) { page = "zikir" }
                WorshipTile("checklist", L("track.title"), Modifier.weight(1f)) { page = "track" }
            }
        }
    }
}

/** İbadet satırındaki kare: simge kutusu ve kısa ad */
@Composable
private fun WorshipTile(icon: String, title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.aspectRatio(1f).paper(radius = 16).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DuoChip(icon, Theme.c.gold, box = 42.dp, glyph = 25.dp)
        Spacer(Modifier.height(5.dp))
        Text(title, color = Theme.c.text, fontSize = 12.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 14.sp, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** İbadet sayfası (2. analiz #199): Vakitler'deki dört karenin "Tümü"sü */
@Composable
private fun WorshipHub(onBack: () -> Unit, open: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubBar(L("worship.title"), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LinkCard("compass", L("qibla.link")) { open("qibla") }
            LinkCard("book", L("quran.title")) { open("quran") }
            LinkCard("hands", L("dua.title")) { open("duas") }
            LinkCard("sparkles", L("esma.title")) { open("esma") }
            LinkCard("scroll", L("hadith.title")) { open("hadith") }
            LinkCard("guide", L("guide.title")) { open("guide") }
            LinkCard("beads", L("zikir.title")) { open("zikir") }
            LinkCard("checklist", L("track.title")) { open("track") }
            LinkCard("star_crescent", L("relday.title")) { open("reldays") }
            LinkCard("love_letter", L("greet.title")) { open("greet") }
            LinkCard("coin", L("zakat.title")) { open("zakat") }
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
            HowItWorks(listOf(L("prayer.kerahatHeading") to L("prayer.kerahatNote")))
        }
    }
}

/** Ezani (alaturka) saat (2. analiz #121): son akşam vakti 12.00 sayılır (sitedeki namaz sayfasıyla aynı) */
internal fun ezaniText(today: java.time.LocalDate, now: java.time.Instant, place: AppPlace): String? {
    fun maghrib(d: java.time.LocalDate): java.time.Instant? =
        tr.bumba.time.core.PrayerTimes.times(d, place.lat, place.lon, place.zone).maghrib?.let { d.atStartOfDay(place.zone).toInstant().plusSeconds(Math.round(it * 60) * 60) }
    var ref = maghrib(today)
    if (ref != null && now.isBefore(ref)) ref = maghrib(today.minusDays(1))
    ref ?: return null
    val m = maxOf(0L, java.time.Duration.between(ref, now).toMinutes())
    val h = (m / 60) % 12
    return T("prayer.ezani", "%02d:%02d".format(java.util.Locale.ROOT, if (h == 0L) 12L else h, m % 60))
}

/** Ramazan'da ikinci sayaç (2. analiz #55): iftara ya da sahura (imsak) kalan süre */
@Composable
private fun RamadanCountdown(now: java.time.Instant, place: AppPlace) {
    val today = place.today(now)
    val next = (place.entries(today) + place.entries(today.plusDays(1)))
        .firstOrNull { (it.prayer == Prayer.MAGHRIB || it.prayer == Prayer.FAJR) && it.at.isAfter(now) } ?: return
    if (Hijri.date(next.at.atZone(place.zone).toLocalDate())?.month != 9) return
    AppCard {
        Text(T("prayer.ramadanLeft", next.prayer.ramadanTitle(next.at, place.zone), Fmt.countdown(java.time.Duration.between(now, next.at).seconds)),
            color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, style = Tnum)
    }
}

@Composable
private fun EzaniRow(today: java.time.LocalDate, now: java.time.Instant, place: AppPlace) {
    val text = ezaniText(today, now, place) ?: return
    Text(text, color = Theme.c.muted, fontSize = 13.sp)
}

/** Bayram namazı (2. analiz #56): bayramın ilk günü öğleye kadar, arefe günü "yarın"; Diyanet kuralı güneş + 38 dk */
@Composable
private fun BayramRow(today: java.time.LocalDate, now: java.time.Instant, place: AppPlace) {
    fun isBayram(d: java.time.LocalDate) = AppData.events[d].orEmpty().any { (it.key == "rb1" || it.key == "kb1") && it.date == d }
    fun at(d: java.time.LocalDate): java.time.Instant? {
        val day = tr.bumba.time.core.PrayerTimes.times(d, place.lat, place.lon, place.zone)
        val h = tr.bumba.time.core.PrayerTimes.bayram(day) ?: return null
        return d.atStartOfDay(place.zone).toInstant().plusSeconds((h * 3600).toLong())
    }
    val text = when {
        isBayram(today) -> {
            val dh = tr.bumba.time.core.PrayerTimes.times(today, place.lat, place.lon, place.zone).dhuhr
            val noon = dh?.let { today.atStartOfDay(place.zone).toInstant().plusSeconds((it * 3600).toLong()) }
            if (noon != null && now.isBefore(noon)) at(today)?.let { L("prayer.bayram") + ": " + Fmt.hm(it, place.zone) } else null
        }
        isBayram(today.plusDays(1)) -> at(today.plusDays(1))?.let { T("prayer.bayramTomorrow", Fmt.hm(it, place.zone)) }
        else -> null
    } ?: return
    AppCard {
        Text(text, color = Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        HowItWorks(listOf(L("prayer.bayram") to L("prayer.bayramNote")))
    }
}

/** Ek vakitler (2. analiz #59; Türkiye Takvimi tanımlarıyla yaklaşık, sitedeki namaz sayfasıyla aynı): açılır kart */
@Composable
private fun ExtraTimesCard(today: java.time.LocalDate, place: AppPlace) {
    var open by remember { mutableStateOf(false) }
    AppCard(Modifier.clickable { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("prayer.extraHeading"), Modifier.weight(1f), color = Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Theme.c.muted)
        }
        if (open) {
            val pt = tr.bumba.time.core.PrayerTimes
            val noon = today.atTime(java.time.LocalTime.NOON).toInstant(java.time.ZoneOffset.UTC)
            val off = place.zone.rules.getOffset(noon).totalSeconds / 3600.0
            val day = pt.times(today.year, today.monthValue, today.dayOfMonth, place.lat, place.lon, off)
            val e = pt.extraTimes(today.year, today.monthValue, today.dayOfMonth, place.lat, place.lon, off, day)
            val asr2 = pt.times(today.year, today.monthValue, today.dayOfMonth, place.lat, place.lon, off, tr.bumba.time.core.PrayerTimes.Parameters.method("diyanet", true)).asr
            val rows = listOf(
                "israk" to e.israk, "duha" to e.duha, "asrSani" to asr2, "isfirar" to e.isfirar, "istibak" to e.istibak,
                "isaSani" to e.isaSani, "midnight" to e.midnight, "tahajjud" to e.tahajjud, "seher" to e.seher,
            )
            val start = today.atStartOfDay(place.zone).toInstant()
            for ((key, h) in rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(L("prayer.extraNames.$key"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Text(h?.let { Fmt.hm(start.plusSeconds(Math.round(it * 60) * 60), place.zone) } ?: "–", color = Theme.c.text, fontSize = 15.sp, style = Tnum)
                }
            }
            HowItWorks(listOf(L("prayer.extraHeading") to L("prayer.extraNote")))
        }
    }
}

@Composable
private fun LinkCard(icon: String, title: String, onClick: () -> Unit) {
    AppCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DuoChip(icon, Theme.c.gold)
            Spacer(Modifier.width(14.dp))
            Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
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
        // Kemenag'da ihtiyat imsakı ve duha sütunları da var
        val sub = place.extraRows(today) != null
        val cols = listOf(L("prayer.colDay")) + Prayer.entries.flatMap {
            when {
                sub && it == Prayer.FAJR -> listOf(L("prayer.method.rowImsak"), it.title)
                sub && it == Prayer.SUNRISE -> listOf(it.title, L("prayer.method.rowDuha"))
                else -> listOf(it.title)
            }
        }
        val scroll = rememberScrollState()
        LazyColumn(Modifier.fillMaxSize().horizontalScroll(scroll).padding(horizontal = 12.dp)) {
            item { TableRow(cols, header = true, highlight = false) }
            items((1..month.lengthOfMonth()).toList()) { d ->
                val day = LocalDate.of(month.year, month.monthValue, d)
                val e = place.entries(day).associate { it.prayer to Fmt.hm(it.at, place.zone) }
                val x = if (sub) place.extraRows(day) else null
                fun hm(at: java.time.Instant?) = at?.let { Fmt.hm(it, place.zone) } ?: "–"
                TableRow(
                    listOf("$d ${Fmt.weekdayShort(day)}") + Prayer.entries.flatMap {
                        when {
                            sub && it == Prayer.FAJR -> listOf(hm(x?.first), e[it] ?: "–")
                            sub && it == Prayer.SUNRISE -> listOf(e[it] ?: "–", hm(x?.second))
                            else -> listOf(e[it] ?: "–")
                        }
                    },
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
