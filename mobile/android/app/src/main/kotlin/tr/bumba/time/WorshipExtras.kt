package tr.bumba.time

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import tr.bumba.time.core.Prayer
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Vakit ve ibadet ekleri (3 Kasım 2026, 2. analiz #57, #61, #63, #64, #65, #66; iOS'taki WorshipExtras.swift ile aynı
 * kayıt anahtarları): dini günler listesi ve yaklaşan dini gün satırı, Vakitler'de "kıldım" işareti, kalan süreyi sözle
 * yazma, vakit simgeleri, 24 saat kadranı, vakte göre gökyüzü zemini, ibadet istatistikleri, ömür boyu kaza hesabı,
 * oruç kazasında sebep ve hayız/nifas döneminde takibi duraklatma. Kayıtlar yalnız telefonda; seri ve rozet bilerek yok.
 */

// MARK: Dini günler (#57)

object RelDays {
    /** Dini gün ve geceler, bayramların ilk günleri ve arifeleri */
    fun isReligious(e: CalEvent) = e.type == "religious" || e.key in setOf("rb1", "kb1", "rbEve", "kbEve")

    val all: List<CalEvent> by lazy {
        AppData.events.entries.flatMap { (d, l) -> l.filter { it.date == d && isReligious(it) } }.sortedBy { it.date }
    }

    fun next(today: LocalDate) = all.firstOrNull { !it.date.isBefore(today) }
    fun nextOf(key: String, today: LocalDate) = all.firstOrNull { it.key == key && !it.date.isBefore(today) }
    fun years() = all.map { it.date.year }.distinct().sorted()

    /** "Bugün", "Yarın", "3 gün kaldı", "Geçti" */
    fun whenText(d: LocalDate, today: LocalDate): String {
        val n = ChronoUnit.DAYS.between(today, d)
        return when {
            n == 0L -> L("client.today")
            n == 1L -> L("client.tomorrow")
            n < 0 -> L("client.passed")
            else -> T("client.daysLeft", n)
        }
    }

    /** Bayram namazı vakti (bayramın ilk günü) seçili yerde */
    fun bayramAt(d: LocalDate, place: AppPlace): Instant? {
        val day = tr.bumba.time.core.PrayerTimes.times(d, place.lat, place.lon, place.zone)
        val h = tr.bumba.time.core.PrayerTimes.bayram(day) ?: return null
        return d.atStartOfDay(place.zone).toInstant().plusSeconds((h * 3600).roundToLong())
    }

    private fun Double.roundToLong() = Math.round(this)
}

/** Vakitler'de: "Yaklaşan: Arefe · 3 gün kaldı"; dokununca dini günler listesi */
@Composable
fun UpcomingReligiousLine(today: LocalDate, onOpen: () -> Unit) {
    val e = RelDays.next(today) ?: return
    Text(
        T("relday.upcoming", e.name, RelDays.whenText(e.date, today)),
        Modifier.clickable(onClick = onOpen).padding(vertical = 2.dp),
        color = Theme.c.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
    )
}

@Composable
fun ReligiousDaysScreen(onBack: () -> Unit) {
    val place = AppPlace.current()
    val today = place.today()
    val years = remember { RelDays.years() }
    var year by remember { mutableIntStateOf(today.year.coerceIn(years.firstOrNull() ?: today.year, years.lastOrNull() ?: today.year)) }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("relday.title"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RelDays.next(today)?.let { e ->
                AppCard(background = Theme.c.accentSoft) {
                    SectionTitle(L("relday.next"))
                    Text(e.name, color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(Fmt.longDate(e.date) + (Hijri.date(e.date)?.let { " · " + it.text } ?: ""), color = Theme.c.muted, fontSize = 14.sp)
                    Text(RelDays.whenText(e.date, today), color = Theme.c.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (e.key == "rb1" || e.key == "kb1") RelDays.bayramAt(e.date, place)?.let {
                        Text(T("relday.bayramPrayerAt", Fmt.hm(it, place.zone)), color = Theme.c.text, fontSize = 15.sp)
                    }
                }
            }
            val three = RelDays.nextOf("ucAylar", today)
            val ramadan = RelDays.nextOf("ramadanStart", today)
            if (three != null || ramadan != null) AppCard {
                three?.let { Text(T("relday.threeMonthsIn", ChronoUnit.DAYS.between(today, it.date)), color = Theme.c.text, fontSize = 15.sp) }
                ramadan?.let { Text(T("relday.ramadanIn", ChronoUnit.DAYS.between(today, it.date)), color = Theme.c.text, fontSize = 15.sp) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (y in years) {
                    val on = y == year
                    Text("$y", Modifier.background(if (on) Theme.c.accent else Theme.c.surface2, RoundedCornerShape(50))
                        .clickable { year = y }.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (on) Color.White else Theme.c.text, fontSize = 14.sp, style = Tnum)
                }
            }
            AppCard {
                for (e in RelDays.all.filter { it.date.year == year }) {
                    val past = e.date.isBefore(today)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, color = if (past) Theme.c.muted else Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(Fmt.longDate(e.date), color = Theme.c.muted, fontSize = 12.sp)
                        }
                        Text(RelDays.whenText(e.date, today), color = if (past) Theme.c.muted else Theme.c.accent, fontSize = 13.sp)
                    }
                }
                if (year in AppData.projectedYears) Text(L("calendar.projected"), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
    }
}

// MARK: Takip ekleri (#61, #63, #64)

object TrackX {
    private val sp get() = Prefs.store
    private fun obj(key: String) = runCatching { JSONObject(sp.getString(key, "{}")) }.getOrDefault(JSONObject())

    /** Vakitler ekranında "kıldım" işareti (varsayılan açık) */
    var inline: Boolean
        get() = sp.getBoolean("track.inline", true)
        set(v) = sp.edit().putBoolean("track.inline", v).apply()

    fun bit(p: Prayer): Int? = when (p) {
        Prayer.FAJR -> 0; Prayer.DHUHR -> 1; Prayer.ASR -> 2; Prayer.MAGHRIB -> 3; Prayer.ISHA -> 4; else -> null
    }

    /** Vakit girdikten kaç dakika sonra işaretlendi (yalnız aynı gün; -1 bilinmiyor) */
    fun whenMinutes(d: LocalDate): IntArray {
        val a = obj("track.when").optJSONArray(d.toString()) ?: return IntArray(5) { -1 }
        return IntArray(5) { a.optInt(it, -1) }
    }

    fun recordWhen(d: LocalDate, i: Int, minutes: Int?) {
        val o = obj("track.when")
        val cur = whenMinutes(d)
        cur[i] = minutes ?: -1
        if (cur.all { it < 0 }) o.remove(d.toString()) else o.put(d.toString(), JSONArray(cur.toList()))
        // Bir yıldan eski kayıtlar silinir
        val cut = d.minusDays(400).toString()
        o.keys().asSequence().toList().filter { it < cut }.forEach { o.remove(it) }
        sp.edit().putString("track.when", o.toString()).apply()
    }

    /** "Kıldım" işaretini değiştir; vakit içinde işaretlenirse kaç dakika sonra kılındığı da yazılır */
    fun toggle(d: LocalDate, p: Prayer, vakitAt: Instant?, now: Instant = Instant.now()) {
        val i = bit(p) ?: return
        val mask = Tracker.dayMask(d)
        val on = mask and (1 shl i) == 0
        Tracker.setDayMask(d, mask xor (1 shl i))
        val mins = if (on && vakitAt != null && !now.isBefore(vakitAt)) ChronoUnit.MINUTES.between(vakitAt, now).toInt().coerceAtMost(24 * 60) else null
        recordWhen(d, i, mins)
    }

    // Hayız/nifas duraklatması: [başlangıç, bitiş] aralıkları + süren duraklatma
    var pausedFrom: LocalDate?
        get() = sp.getString("track.pausedFrom", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        set(v) {
            val old = pausedFrom
            val e = sp.edit()
            if (v == null) {
                e.remove("track.pausedFrom")
                if (old != null) {
                    val a = pauses().toMutableList().apply { add(old to LocalDate.now()) }
                    e.putString("track.pauses", JSONArray(a.takeLast(200).map { JSONArray(listOf(it.first.toString(), it.second.toString())) }).toString())
                }
            } else e.putString("track.pausedFrom", v.toString())
            e.apply()
        }

    fun pauses(): List<Pair<LocalDate, LocalDate>> = runCatching {
        val a = JSONArray(sp.getString("track.pauses", "[]"))
        (0 until a.length()).map { val r = a.getJSONArray(it); LocalDate.parse(r.getString(0)) to LocalDate.parse(r.getString(1)) }
    }.getOrDefault(emptyList())

    fun isPaused(d: LocalDate): Boolean {
        pausedFrom?.let { if (!d.isBefore(it)) return true }
        return pauses().any { !d.isBefore(it.first) && !d.isAfter(it.second) }
    }

    // Kaza kılındıkça günlük sayaç (hız hesabı için)
    fun logKazaPrayed(d: LocalDate = LocalDate.now()) {
        val o = obj("track.kazaLog")
        o.put(d.toString(), o.optInt(d.toString(), 0) + 1)
        val cut = d.minusDays(60).toString()
        o.keys().asSequence().toList().filter { it < cut }.forEach { o.remove(it) }
        sp.edit().putString("track.kazaLog", o.toString()).apply()
    }

    fun kazaPerDay(today: LocalDate = LocalDate.now()): Double {
        val o = obj("track.kazaLog")
        return (0 until 30).sumOf { o.optInt(today.minusDays(it.toLong()).toString(), 0) } / 30.0
    }

    val FAST_REASONS = listOf("illness", "travel", "period", "other")

    /** Eski tek oruç kazası sayacı "Diğer" sebebine taşınır */
    fun migrate() {
        val old = Tracker.kaza("fast")
        if (old > 0) { Tracker.setKaza("fast.other", Tracker.kaza("fast.other") + old); Tracker.setKaza("fast", 0) }
    }

    fun fastTotal() = FAST_REASONS.sumOf { Tracker.kaza("fast.$it") }
    fun prayerKazaTotal() = Tracker.KAZA.sumOf { Tracker.kaza(it) }

    /** Gün sayısını yıl/ay/gün olarak */
    fun span(days: Double): String = when {
        days >= 365 -> T("track.unit.years", (days / 365.2425).let { if (it < 10) (it * 10).roundToInt() / 10.0 else it.roundToInt().toDouble() }.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it".replace(".", Strings.decimalPoint) })
        days >= 60 -> T("track.unit.months", (days / 30.44).roundToInt())
        else -> T("track.unit.days", days.roundToInt().coerceAtLeast(1))
    }

    /** Ömür boyu kaza: ergenlikten düzenli kılmaya başlanan yıla kadar her vakit için gün sayısı */
    fun lifetime(birth: Int, puberty: Int, start: Int, female: Boolean, periodDays: Int): Int {
        val years = (start - (birth + puberty)).coerceAtLeast(0)
        var days = years * 365.2425
        if (female) days -= years * 12.0 * periodDays.coerceIn(0, 15)
        return days.roundToInt().coerceAtLeast(0)
    }

    fun clearExtras() = sp.edit().remove("track.when").remove("track.pauses").remove("track.pausedFrom").remove("track.kazaLog").remove("track.life").apply()
}

/** "2/5" halkası */
@Composable
fun PrayedRing(n: Int, total: Int, size: Int = 34) {
    val accent = Theme.c.accent
    val track = Theme.c.surface2
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(width = size * 0.12f * density)
            drawArc(track, 0f, 360f, false, style = s)
            if (total > 0 && n > 0) drawArc(accent, -90f, 360f * n / total, false, style = s)
        }
        Text("$n", color = Theme.c.text, fontSize = (size * 0.36).sp, fontWeight = FontWeight.SemiBold, style = Tnum)
    }
}

/** Vakit simgesi (#65) */
fun prayerIcon(p: Prayer): ImageVector = when (p) {
    Prayer.FAJR -> Icons.Filled.WbTwilight
    Prayer.SUNRISE -> Icons.Filled.WbSunny
    Prayer.DHUHR -> Icons.Filled.LightMode
    Prayer.ASR -> Icons.Filled.Brightness5
    Prayer.MAGHRIB -> Icons.Filled.Nightlight
    Prayer.ISHA -> Icons.Filled.NightsStay
}

/**
 * Vakte göre gökyüzü zemini (#65; doğal tasarım 4 Kasım 2026, docs/dogal-tasarim bölüm 3.1): gece, şafak, sabah, öğle,
 * ikindi ve akşam gökleri; yazı rengi gökyüzünün parlaklığından (krem ya da mürekkep, en kötü 5,2:1). Koyu temada
 * gündüz gökleri karartılır, yazı krem kalır.
 */
data class Sky(val colors: List<Color>, val text: Color)

fun skyColors(entries: List<AppPlace.Entry>, now: Instant, dark: Boolean = false): Sky {
    fun at(p: Prayer) = entries.firstOrNull { it.prayer == p }?.at
    val f = at(Prayer.FAJR); val s = at(Prayer.SUNRISE); val d = at(Prayer.DHUHR); val a = at(Prayer.ASR); val m = at(Prayer.MAGHRIB); val i = at(Prayer.ISHA)
    fun after(x: Instant?) = x != null && !now.isBefore(x)
    val cream = Color(0xFFFFF8EC); val ink = Color(0xFF2B2520)
    fun sky(top: Long, bottom: Long, text: Color) = Sky(listOf(Color(0xFF000000 or top), Color(0xFF000000 or bottom)), text)
    return when {
        after(i) || !after(f) -> sky(0x1F2240, 0x3B3762, cream)
        after(m) -> sky(0x46305A, 0x8E4B4E, cream)
        after(a) -> if (dark) sky(0x4F3A22, 0x6E5638, cream) else sky(0xE0B577, 0xF5E3BF, ink)
        after(d) -> if (dark) sky(0x284866, 0x47627A, cream) else sky(0x9CC4E0, 0xE3EEF2, ink)
        after(s) -> if (dark) sky(0x2D4A63, 0x4F5E6E, cream) else sky(0xA9C9DF, 0xF2E6CC, ink)
        else -> sky(0x2E2C55, 0x8A5A78, cream)
    }
}

/** Kalan süre sözle: "1 sa 42 dk sonra İkindi" (#66) */
fun countdownWords(seconds: Long, name: String): String {
    val mins = ((seconds + 59) / 60).coerceAtLeast(1)
    val t = if (mins >= 60) T("prayer.wordsHM", mins / 60, mins % 60) else T("prayer.wordsM", mins)
    return T("prayer.inWords", t, name)
}

/** 24 saat kadranı (#65): gece yarısı üstte, vakit dilimleri renkli yay, noktalar ve şu anki saat ibresi */
@Composable
fun PrayerDial24(entries: List<AppPlace.Entry>, place: AppPlace, now: Instant) {
    val measurer = rememberTextMeasurer()
    val accent = Theme.c.accent; val text = Theme.c.text; val muted = Theme.c.muted; val surface2 = Theme.c.surface2
    val day = place.today(now)
    val start = day.atStartOfDay(place.zone).toInstant()
    fun frac(i: Instant) = ChronoUnit.SECONDS.between(start, i) / 86400.0
    val colors = listOf(Color(0xFF6C5BA8), Color(0xFFF2B65A), Color(0xFF5AA9E6), Color(0xFFE39A3B), Color(0xFFC0583F), Color(0xFF26245A))
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp).semantics { contentDescription = entries.joinToString { it.prayer.title + " " + Fmt.hm(it.at, place.zone) } }) {
        val r = size.minDimension / 2 * 0.78f
        val c = Offset(size.width / 2, size.height / 2)
        val sw = r * 0.16f
        drawCircle(surface2, r, c, style = Stroke(sw))
        fun ang(f: Double) = (f * 360 - 90).toFloat()
        for ((k, e) in entries.withIndex()) {
            val from = frac(e.at)
            val to = entries.getOrNull(k + 1)?.let { frac(it.at) } ?: (frac(entries.first().at) + 1)
            drawArc(colors[k % colors.size].copy(alpha = 0.85f), ang(from), ((to - from) * 360).toFloat(), false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r), style = Stroke(sw))
        }
        for (h in 0 until 24) {
            val a = Math.toRadians(h * 15.0 - 90)
            val o = r + sw * 0.75f
            val l = if (h % 6 == 0) sw * 0.6f else sw * 0.3f
            drawLine(muted, Offset(c.x + (o * cos(a)).toFloat(), c.y + (o * sin(a)).toFloat()), Offset(c.x + ((o + l) * cos(a)).toFloat(), c.y + ((o + l) * sin(a)).toFloat()), strokeWidth = 2f)
            if (h % 6 == 0) {
                val t = measurer.measure("%02d".format(h), TextStyle(fontSize = 11.sp, color = muted))
                val tr = o + l + 14f * density
                drawText(t, topLeft = Offset(c.x + (tr * cos(a)).toFloat() - t.size.width / 2, c.y + (tr * sin(a)).toFloat() - t.size.height / 2))
            }
        }
        for (e in entries) {
            val a = Math.toRadians(frac(e.at) * 360 - 90)
            val p = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
            drawCircle(Color.White, sw * 0.32f, p)
            val label = measurer.measure(e.prayer.title, TextStyle(fontSize = 11.sp, color = text, fontWeight = FontWeight.Medium))
            val ir = r - sw * 1.6f
            drawText(label, topLeft = Offset(c.x + (ir * cos(a)).toFloat() - label.size.width / 2, c.y + (ir * sin(a)).toFloat() - label.size.height / 2))
        }
        val a = Math.toRadians(frac(now) * 360 - 90)
        drawLine(accent, c, Offset(c.x + ((r + sw * 0.5f) * cos(a)).toFloat(), c.y + ((r + sw * 0.5f) * sin(a)).toFloat()), strokeWidth = 4f * density / 2)
        drawCircle(accent, 5f * density, c)
        val tl = measurer.measure(Fmt.hm(now, place.zone), TextStyle(fontSize = 20.sp, color = text, fontWeight = FontWeight.Bold))
        drawText(tl, topLeft = Offset(c.x - tl.size.width / 2, c.y + 10f * density))
    }
}

/** Güneş/Ay yayı ile 24 saat kadranı arasında dokunarak geçiş (seçim `prayer.visual`) */
@Composable
fun PrayerVisual(now: Instant, place: AppPlace, entries: List<AppPlace.Entry>) {
    var dial by remember { mutableStateOf(Prefs.store.getString("prayer.visual", "arc") == "dial") }
    Box(Modifier.fillMaxWidth().clickable {
        dial = !dial; Prefs.store.edit().putString("prayer.visual", if (dial) "dial" else "arc").apply()
    }) {
        if (dial) AppCard { PrayerDial24(entries, place, now) } else SunMoonArc(now, place)
    }
}

// MARK: İstatistik (#61)

@Composable
fun TrackerStatsCard(today: LocalDate, version: Int) {
    @Suppress("UNUSED_EXPRESSION") version
    val accent = Theme.c.accent; val surface2 = Theme.c.surface2
    AppCard {
        SectionTitle(L("track.stats"))
        // Son 7 gün halkaları
        Text(L("track.last7"), color = Theme.c.muted, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 6 downTo 0) {
                val d = today.minusDays(i.toLong())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (TrackX.isPaused(d)) Box(Modifier.size(34.dp).background(surface2, CircleShape))
                    else PrayedRing(Integer.bitCount(Tracker.dayMask(d)), 5)
                    Text(Fmt.weekdayShort(d), color = Theme.c.muted, fontSize = 11.sp)
                }
            }
        }
        // Son 12 ay: kılınan / kılınabilecek
        Text(L("track.monthly"), Modifier.padding(top = 6.dp), color = Theme.c.muted, fontSize = 12.sp)
        val months = (11 downTo 0).map { YearMonth.from(today).minusMonths(it.toLong()) }
        val ratios = months.map { ym ->
            val days = (1..ym.lengthOfMonth()).map { ym.atDay(it) }.filter { !it.isAfter(today) && !TrackX.isPaused(it) }
            if (days.isEmpty()) 0.0 else days.sumOf { Integer.bitCount(Tracker.dayMask(it)) } / (days.size * 5.0)
        }
        Ltr {
            Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                for ((k, ym) in months.withIndex()) Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height((4 + 66 * ratios[k]).dp).background(if (ratios[k] > 0) accent else surface2, RoundedCornerShape(4.dp))
                        .semantics { contentDescription = Fmt.month(ym.monthValue) + " " + Fmt.percent((ratios[k] * 100).roundToInt()) })
                    Text(Fmt.month(ym.monthValue).take(1), color = Theme.c.muted, fontSize = 10.sp)
                }
            }
        }
        // Son bir yıl ısı haritası (haftalar sütun, pazartesiden pazara satır)
        Text(L("track.heatmap"), Modifier.padding(top = 6.dp), color = Theme.c.muted, fontSize = 12.sp)
        val startMonday = today.minusDays((today.dayOfWeek.value - 1).toLong()).minusWeeks(52)
        Ltr {
            Row(Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (w in 0..52) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (dow in 0 until 7) {
                        val d = startMonday.plusWeeks(w.toLong()).plusDays(dow.toLong())
                        val n = if (d.isAfter(today)) -1 else Integer.bitCount(Tracker.dayMask(d))
                        Box(Modifier.size(10.dp).background(
                            when { n < 0 -> Color.Transparent; n == 0 -> surface2; else -> accent.copy(alpha = 0.25f + 0.15f * n) },
                            RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(L("track.less"), color = Theme.c.muted, fontSize = 11.sp)
            for (n in 0..5) Box(Modifier.size(10.dp).background(if (n == 0) surface2 else accent.copy(alpha = 0.25f + 0.15f * n), RoundedCornerShape(2.dp)))
            Text(L("track.more"), color = Theme.c.muted, fontSize = 11.sp)
        }
        // Ne zaman kılındı (ortalama)
        val avg = (0 until 5).map { i ->
            val v = (0 until 90).mapNotNull { TrackX.whenMinutes(today.minusDays(it.toLong()))[i].takeIf { m -> m >= 0 } }
            if (v.isEmpty()) null else v.average().roundToInt()
        }
        if (avg.any { it != null }) {
            Text(L("track.whenTitle"), Modifier.padding(top = 6.dp), color = Theme.c.muted, fontSize = 12.sp)
            for ((i, p) in Tracker.PRAYERS.withIndex()) avg[i]?.let { Row2(L("track.prayer.$p"), T("track.avgAfter", it)) }
            Text(L("track.whenNote"), color = Theme.c.muted, fontSize = 11.sp)
        }
    }
}

// MARK: Ömür boyu kaza (#63)

@Composable
fun LifetimeKazaCard(onApplied: () -> Unit) {
    val sp = Prefs.store
    val saved = remember { runCatching { JSONObject(sp.getString("track.life", "{}")) }.getOrDefault(JSONObject()) }
    var open by remember { mutableStateOf(false) }
    var birth by remember { mutableStateOf(saved.optInt("b", 0).takeIf { it > 0 }?.toString() ?: "") }
    var puberty by remember { mutableIntStateOf(saved.optInt("p", 15)) }
    var start by remember { mutableStateOf(saved.optInt("s", 0).takeIf { it > 0 }?.toString() ?: "") }
    var female by remember { mutableStateOf(saved.optBoolean("f", false)) }
    var period by remember { mutableIntStateOf(saved.optInt("d", 6)) }
    AppCard(Modifier.clickable { open = !open }) {
        Text(L("track.life.title"), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (open) {
            fun save() = sp.edit().putString("track.life", JSONObject().put("b", birth.toIntOrNull() ?: 0).put("p", puberty)
                .put("s", start.toIntOrNull() ?: 0).put("f", female).put("d", period).toString()).apply()
            OutlinedTextField(birth, { birth = it.filter(Char::isDigit).take(4); save() }, Modifier.fillMaxWidth(),
                label = { Text(L("track.life.birth")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            ChoiceRow(L("track.life.puberty"), "$puberty", (9..15).map { it to "$it" }) { puberty = it; save() }
            OutlinedTextField(start, { start = it.filter(Char::isDigit).take(4); save() }, Modifier.fillMaxWidth(),
                label = { Text(L("track.life.start")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("track.life.female"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                SoftSwitch(female, { female = it; save() }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
            }
            if (female) ChoiceRow(L("track.life.period"), "$period", (3..10).map { it to "$it" }) { period = it; save() }
            val b = birth.toIntOrNull(); val s = start.toIntOrNull()
            if (b != null && s != null && b in 1900..2100 && s in 1900..2100) {
                val n = TrackX.lifetime(b, puberty, s, female, period)
                Text(T("track.life.result", n), color = Theme.c.accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (n > 0) RoundButton(L("track.life.apply"), true) {
                    for (k in Tracker.KAZA) Tracker.setKaza(k, Tracker.kaza(k) + n)
                    onApplied()
                }
            }
            Text(L("track.life.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}

/** Kaza bitiş süresi: son 30 günün hızıyla ve günde 5 kazayla */
@Composable
fun KazaPace() {
    val remaining = TrackX.prayerKazaTotal()
    if (remaining <= 0) return
    val rate = TrackX.kazaPerDay()
    if (rate > 0) Text(T("track.pace", TrackX.span(remaining / rate)), color = Theme.c.text, fontSize = 14.sp)
    Text(T("track.paceFive", TrackX.span(remaining / 5.0)), color = Theme.c.muted, fontSize = 13.sp)
}

/** Oruç kazası sebeplere göre (#63) */
@Composable
fun FastKazaRows(onChange: () -> Unit) {
    var pick by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(L("track.kazaFast"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        Ltr { Text("${TrackX.fastTotal()}", Modifier.width(44.dp), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, style = Tnum) }
        IconButton(onClick = { pick = !pick }) { Icon(Icons.Filled.Add, L("track.fastReason"), tint = Theme.c.accent) }
    }
    if (pick) {
        Text(L("track.fastReason"), color = Theme.c.muted, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in TrackX.FAST_REASONS) Text(L("track.reason.$r"),
                Modifier.background(Theme.c.surface2, RoundedCornerShape(50)).clickable {
                    Tracker.setKaza("fast.$r", Tracker.kaza("fast.$r") + 1); pick = false; onChange()
                }.padding(horizontal = 12.dp, vertical = 7.dp), color = Theme.c.text, fontSize = 13.sp)
        }
    }
    for (r in TrackX.FAST_REASONS) {
        val n = Tracker.kaza("fast.$r")
        if (n > 0) Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(L("track.reason.$r"), Modifier.weight(1f), color = Theme.c.muted, fontSize = 14.sp)
            IconButton(onClick = { Tracker.setKaza("fast.$r", n - 1); onChange() }) { Icon(Icons.Filled.Remove, L("track.kazaPrayed"), tint = Theme.c.accent) }
            Ltr { Text("$n", Modifier.width(36.dp), color = Theme.c.text, fontSize = 15.sp, textAlign = TextAlign.Center, style = Tnum) }
        }
    }
}

/** Hayız/nifas duraklatması (#63) */
@Composable
fun PauseRow(onChange: () -> Unit) {
    val from = TrackX.pausedFrom
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(L("track.pause"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        SoftSwitch(from != null, { TrackX.pausedFrom = if (it) LocalDate.now() else null; onChange() },
            colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
    }
    from?.let { Text(T("track.pausedSince", Fmt.longDate(it)), color = Theme.c.muted, fontSize = 12.sp) }
}

/** Vakitler'de "kıldım" satır sonu düğmesi */
@Composable
fun PrayedMark(day: LocalDate, e: AppPlace.Entry, now: Instant, version: Int, onChange: () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") version
    val i = TrackX.bit(e.prayer)
    if (i == null || e.at.isAfter(now) || TrackX.isPaused(day)) { Box(Modifier.size(40.dp)); return }
    val on = Tracker.dayMask(day) and (1 shl i) != 0
    IconButton(onClick = { TrackX.toggle(day, e.prayer, e.at, now); onChange() }, modifier = Modifier.size(40.dp)) {
        Icon(if (on) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, T("track.markPrayed", e.prayer.title),
            tint = if (on) Theme.c.accent else Theme.c.muted)
    }
}

/** Bugünün "2/5 kılındı" özeti ve "Hepsini işaretle" */
@Composable
fun PrayedSummary(day: LocalDate, entries: List<AppPlace.Entry>, now: Instant, version: Int, onChange: () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") version
    if (TrackX.isPaused(day)) { TrackX.pausedFrom?.let { Text(T("track.pausedSince", Fmt.longDate(it)), color = Theme.c.muted, fontSize = 12.sp) }; return }
    val n = Integer.bitCount(Tracker.dayMask(day))
    val started = entries.filter { TrackX.bit(it.prayer) != null && !it.at.isAfter(now) }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PrayedRing(n, 5, 30)
        Text(T("track.prayedOf", n, 5), Modifier.weight(1f), color = Theme.c.text, fontSize = 14.sp)
        if (started.any { Tracker.dayMask(day) and (1 shl TrackX.bit(it.prayer)!!) == 0 }) TextButton(onClick = {
            for (e in started) {
                val b = TrackX.bit(e.prayer)!!
                if (Tracker.dayMask(day) and (1 shl b) == 0) TrackX.toggle(day, e.prayer, e.at, now)
            }
            onChange()
        }) { Text(L("track.markAll"), color = Theme.c.accent) }
    }
}

/** Gökyüzü zeminli kalan süre bloğu (#65, #66): dokununca sözle/saat olarak */
@Composable
fun SkyCountdown(entries: List<AppPlace.Entry>, next: AppPlace.Entry, now: Instant) {
    var words by remember { mutableStateOf(Prefs.store.getBoolean("prayer.words", false)) }
    val left = next.at.epochSecond - now.epochSecond
    val sky = skyColors(entries, now, Theme.c.dark)
    // Köşede hatem, altta kubbe-minare silueti (doğal tasarım madde 15; %10 ve %12)
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(sky.colors))
            .clickable { words = !words; Prefs.store.edit().putBoolean("prayer.words", words).apply() }
            .semantics { contentDescription = countdownWords(left, next.prayer.title) + ". " + L("prayer.wordsHint") },
    ) {
    HatemMotif(Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-28).dp).size(110.dp), sky.text, 0.10f)
    SkylineMotif(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(34.dp), sky.text, 0.12f)
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 22.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LineIcon(LineIconMap.prayer(next.prayer), 30.dp, c1 = sky.text, c2 = sky.text)
        if (words) {
            Text(countdownWords(left, next.prayer.title), color = sky.text, fontSize = 30.sp, fontFamily = AppFonts.heading,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        } else {
            Text(T("prayer.nextIn", next.prayer.title), color = sky.text.copy(alpha = 0.88f), fontSize = 15.sp)
            // Büyük sayaç: ince (300) ve tablo rakamlı (doğal tasarım bölüm 3.2)
            Text(Fmt.countdown(left), color = sky.text, fontSize = 56.sp, fontWeight = FontWeight.Light, style = Tnum)
        }
    }
    }
}
