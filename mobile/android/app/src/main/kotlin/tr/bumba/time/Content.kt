package tr.bumba.time

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import tr.bumba.time.core.Prayer
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.IsoFields

/*
 * Esmaü'l-Hüsna, günün hadisi, namaz rehberi ve takvim yaprağı (30 Ekim 2026, rakip analizi listesi #69–#72).
 * Veri mobile/shared/religion/ → gen-religion.mjs (esma.json, hadith.json, guide.json, sayings.json). iOS'taki
 * Content.swift ile aynı içerik ve sıra. Kayıt tutulmaz.
 */

private fun asset(c: Context, name: String) = runCatching { c.assets.open(name).bufferedReader().use { it.readText() } }.getOrNull()

@Composable
private fun Rtl(text: String, size: Int) {
    Text(text, Modifier.fillMaxWidth(), color = Theme.c.ink, fontSize = size.sp, lineHeight = (size * 1.7).sp,
        textAlign = TextAlign.Right, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl))
}

private val turkic get() = Strings.lang == "tr" || Strings.lang == "az"

// Esmaü'l-Hüsna

object Esma {
    class Name(val no: Int, val ar: String, val read: String, val meaning: String)

    fun load(c: Context): List<Name> = runCatching {
        val a = JSONArray(asset(c, "esma.json") ?: "[]")
        // Sütunlar: [ar, Türkçe okunuş, Latin okunuş, tr, en, de, az, id, ur, fa, fr, ru, ja, ko, zh, fil, th]; Farsça ve Urducada okunuş yok (ad Arapça yazıyla okunur)
        val col = when (Strings.lang) { "tr" -> 3; "en" -> 4; "de" -> 5; "az" -> 6; "id" -> 7; "ur" -> 8; "fa" -> 9; "fr" -> 10; "ru" -> 11; "ja" -> 12; "ko" -> 13; "zh" -> 14; "fil" -> 15; "th" -> 16; else -> -1 }
        (0 until a.length()).map { i ->
            val r = a.getJSONArray(i)
            val read = when (Strings.lang) { "tr", "az" -> r.getString(1); "ur", "fa" -> ""; else -> r.getString(2) }
            Name(i + 1, r.getString(0), read, if (col < 0 || col >= r.length()) "" else r.getString(col))
        }
    }.getOrDefault(emptyList())
}

@Composable
fun EsmaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val all = remember { Esma.load(context) }
    var q by rememberSaveable { mutableStateOf("") }
    // Dokununca hat kartı ve sesli okuma (2. analiz #85)
    var detail by remember { mutableStateOf<Esma.Name?>(null) }
    val list = q.trim().lowercase().let { s -> if (s.isEmpty()) all else all.filter { it.read.lowercase().contains(s) || it.meaning.lowercase().contains(s) || it.ar.contains(s) } }
    detail?.let { n -> EsmaCard(n) { detail = null } }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("esma.title"), onBack = onBack)
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text(L("esma.search")) }, singleLine = true)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(list, key = { it.no }) { n ->
                Row(Modifier.fillMaxWidth().clickable { detail = n }.padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
                    Text("${n.no}", Modifier.width(30.dp), color = Theme.c.muted, fontSize = 12.sp, style = Tnum)
                    Column(Modifier.weight(1f)) {
                        if (Strings.lang != "ar") {
                            if (n.read.isNotEmpty()) Text(n.read, color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(n.meaning, color = if (n.read.isEmpty()) Theme.c.text else Theme.c.muted, fontSize = if (n.read.isEmpty()) 16.sp else 14.sp)
                        }
                    }
                    Text(n.ar, color = Theme.c.ink, fontSize = 24.sp)
                }
            }
            item { Text(L("esma.note"), Modifier.padding(vertical = 12.dp), color = Theme.c.muted, fontSize = 12.sp) }
        }
    }
}

// Hadis

object Hadiths {
    class Hadith(val no: Int, val ar: String, val narrator: String, val book: String, val text: String) {
        val source: String get() = T("hadith.source", narrator, book, no.toString())
    }

    private var cache: List<Hadith>? = null
    fun load(c: Context): List<Hadith> = cache ?: runCatching {
        val a = JSONArray(asset(c, "hadith.json") ?: "[]")
        val lang = Strings.lang
        fun JSONObject.l(k: String) = getJSONObject(k).let { o -> o.optString(lang).ifEmpty { o.optString("en") } }
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Hadith(o.getInt("n"), o.getString("ar"), o.l("r"), o.l("s"), if (lang == "ar") "" else o.l("t"))
        }
    }.getOrDefault(emptyList()).also { cache = it }

    /** Günün hadisi: yılın gününe göre sırayla (iOS ile aynı: epoch günü mod sayı) */
    fun today(c: Context, day: LocalDate = LocalDate.now()): Hadith? = load(c).let { if (it.isEmpty()) null else it[Math.floorMod(day.toEpochDay(), it.size.toLong()).toInt()] }
}

@Composable
private fun HadithCard(h: Hadiths.Hadith) {
    AppCard {
        if (h.text.isNotEmpty()) Text(h.text, color = Theme.c.text, fontSize = 16.sp)
        Rtl(h.ar, 18)
        Text(h.source, color = Theme.c.muted, fontSize = 12.sp)
    }
}

@Composable
fun HadithScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val all = remember { Hadiths.load(context) }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("hadith.title"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Hadiths.today(context)?.let { SectionTitle(L("hadith.today")); HadithCard(it) }
            SectionTitle(L("hadith.title"))
            for (h in all) HadithCard(h)
            Text(L("hadith.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}

// Namaz rehberi

@Composable
fun PrayerGuideScreen(onBack: () -> Unit, onDuas: () -> Unit) {
    val context = LocalContext.current
    val data = remember { runCatching { JSONObject(asset(context, "guide.json") ?: "{}") }.getOrNull() }
    val lang = Strings.lang
    fun JSONObject.tx() = optString(lang).ifEmpty { optString("en") }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("guide.title"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val secs = data?.optJSONArray("sections") ?: JSONArray()
            for (i in 0 until secs.length()) {
                val s = secs.getJSONObject(i)
                AppCard {
                    Text(s.getJSONObject("title").tx(), color = Theme.c.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.getJSONObject("intro").tx(), color = Theme.c.muted, fontSize = 14.sp)
                    s.optJSONArray("steps")?.let { st ->
                        for (j in 0 until st.length()) Row(Modifier.padding(top = 4.dp)) {
                            Text("${j + 1}", Modifier.width(24.dp), color = Theme.c.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(st.getJSONObject(j).tx(), color = Theme.c.text, fontSize = 14.sp)
                        }
                    }
                    s.optJSONArray("rows")?.let { rows ->
                        for (j in 0 until rows.length()) {
                            val r = rows.getJSONArray(j)
                            Row(Modifier.padding(top = 4.dp)) {
                                Text(L("prayer.${r.getString(0)}"), Modifier.width(96.dp), color = Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(r.getJSONObject(1).tx(), color = Theme.c.text, fontSize = 14.sp)
                            }
                        }
                    }
                    if ((s.optJSONArray("duas")?.length() ?: 0) > 0) {
                        TextButton(onClick = onDuas) { Text(L("guide.duas"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
            data?.optJSONObject("note")?.let { Text(it.tx(), color = Theme.c.muted, fontSize = 12.sp) }
        }
    }
}

// Takvim yaprağı ve günün sözü

object Sayings {
    private var cache: JSONObject? = null
    fun today(c: Context, day: LocalDate = LocalDate.now()): String? {
        val all = cache ?: runCatching { JSONObject(asset(c, "sayings.json") ?: "{}") }.getOrDefault(JSONObject()).also { cache = it }
        val list = all.optJSONArray(Strings.lang) ?: all.optJSONArray("en") ?: return null
        if (list.length() == 0) return null
        return list.getString(Math.floorMod(day.toEpochDay(), list.length().toLong()).toInt())
    }

    /** Rastgele bir söz (alarm yazma görevi) */
    fun random(c: Context): String? {
        today(c) // listeyi yükler
        val list = cache?.optJSONArray(Strings.lang) ?: return null
        return if (list.length() == 0) null else list.getString(kotlin.random.Random.nextInt(list.length()))
    }

    /** Hızır (6 Mayıs) ve Kasım (8 Kasım) günleri: geleneksel takvim yapraklarındaki gün sayısı */
    fun hizirKasim(d: LocalDate): Pair<Boolean, Int> {
        val hizir = LocalDate.of(d.year, 5, 6)
        val kasim = LocalDate.of(d.year, 11, 8)
        if (!d.isBefore(hizir) && d.isBefore(kasim)) return false to (d.toEpochDay() - hizir.toEpochDay() + 1).toInt()
        val start = if (d.isBefore(hizir)) LocalDate.of(d.year - 1, 11, 8) else kasim
        return true to (d.toEpochDay() - start.toEpochDay() + 1).toInt()
    }
}

@Composable
fun CalendarLeafScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val place = AppPlace.current()
    var iso by rememberSaveable { mutableStateOf(place.today().toString()) }
    val day = LocalDate.parse(iso)
    val entries = place.entries(day)
    val events = AppData.events[day].orEmpty()
    val locale = Strings.locale
    Column(Modifier.fillMaxSize()) {
        SubBar(L("leaf.title"), onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).pointerInput(iso) {
                var total = 0f
                detectHorizontalDragGestures(onDragEnd = {
                    if (total < -60) iso = day.plusDays(1).toString() else if (total > 60) iso = day.minusDays(1).toString()
                    total = 0f
                }) { _, dx -> total += dx }
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().background(Theme.c.accent, RoundedCornerShape(18.dp)).padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(Fmt.month(day.monthValue) + " " + day.year, color = Color.White.copy(alpha = 0.9f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("${day.dayOfMonth}", color = Color.White, fontSize = 104.sp, fontWeight = FontWeight.Bold, style = Tnum)
                Text(day.dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Hijri.date(day)?.let { Text("☾ " + it.text, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp) }
                Text(Rumi.text(day), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }
            Text(T("leaf.dayOfYear", day.dayOfYear.toString(), day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString()), color = Theme.c.muted, fontSize = 14.sp)
            if (turkic) {
                val (kasim, n) = Sayings.hizirKasim(day)
                Text(T(if (kasim) "leaf.kasim" else "leaf.hizir", n.toString()), color = Theme.c.muted, fontSize = 13.sp)
            }
            if (events.isNotEmpty()) AppCard {
                SectionTitle(L("leaf.special"))
                for (e in events) Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(10.dp).background(AppData.color(e.type), CircleShape))
                    Text("  " + e.name, color = Theme.c.text, fontSize = 15.sp)
                }
            }
            val rise = entries.firstOrNull { it.prayer == Prayer.SUNRISE }
            val set = entries.firstOrNull { it.prayer == Prayer.MAGHRIB }
            if (rise != null && set != null) AppCard {
                SectionTitle(L("leaf.sun") + " · " + place.name)
                Text(T("leaf.sunTimes", Fmt.hm(rise.at, place.zone), Fmt.hm(set.at, place.zone)), color = Theme.c.text, fontSize = 15.sp)
                if (Features.isOn(Tab.PRAYER)) Text(entries.joinToString(" · ") { it.prayer.title + " " + Fmt.hm(it.at, place.zone) }, color = Theme.c.muted, fontSize = 13.sp)
            }
            Sayings.today(context, day)?.let { s ->
                AppCard {
                    SectionTitle(L("leaf.saying"))
                    Text("“$s”", color = Theme.c.text, fontSize = 19.sp, fontStyle = FontStyle.Italic)
                }
            }
            // Günün hadisi (2. analiz #124; İbadet içeriği, Vakitler açıkken)
            if (Features.isOn(Tab.PRAYER)) Hadiths.today(context, day)?.let { h ->
                SectionTitle(L("hadith.today"))
                HadithCard(h)
            }
            // Ayın adının kökeni (2. analiz #125)
            AppCard {
                SectionTitle(L("calendar.monthOrigin.heading"))
                Text(L("calendar.monthOrigin.items.${day.monthValue - 1}"), color = Theme.c.text, fontSize = 15.sp)
                Text(L("calendar.monthOrigin.source"), color = Theme.c.muted, fontSize = 11.sp)
            }
            // Tarihte bugün (2. analiz #124): Wikidata (CC0)
            val otd = OnThisDay.items(context, day)
            if (otd.isNotEmpty()) AppCard {
                SectionTitle(L("otd.h1"))
                for (it in otd) {
                    Text("${it.icon} ${it.year} · ${it.title}", color = Theme.c.text, fontSize = 15.sp)
                    if (it.desc.isNotEmpty()) Text(it.desc, color = Theme.c.muted, fontSize = 12.sp)
                }
                Text("Wikidata (CC0)", color = Theme.c.muted, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { iso = day.minusDays(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Theme.c.accent); Text(L("leaf.prev"), color = Theme.c.accent)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { iso = day.plusDays(1).toString() }) {
                    Text(L("leaf.next"), color = Theme.c.accent); Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.accent)
                }
            }
        }
    }
}

/** Rumi tarih (2. analiz #120): Jülyen takvimi, yılbaşı 1 Mart, yıl = Jülyen yıl − 584 (Mart–Aralık) ya da − 585 */
object Rumi {
    val months = listOf("Kânûn-ı sânî", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül",
        "Teşrîn-i evvel", "Teşrîn-i sânî", "Kânûn-ı evvel")

    /** (gün, ay, Rumi yıl) */
    fun parts(d: LocalDate): Triple<Int, Int, Int> {
        val jdn = d.toEpochDay() + 2440588
        val c = jdn + 32082
        val dd = (4 * c + 3) / 1461
        val e = c - 1461 * dd / 4
        val mm = (5 * e + 2) / 153
        val day = (e - (153 * mm + 2) / 5 + 1).toInt()
        val month = (mm + 3 - 12 * (mm / 10)).toInt()
        val year = (dd - 4800 + mm / 10).toInt()
        return Triple(day, month, year - if (month >= 3) 584 else 585)
    }

    fun text(d: LocalDate): String { val (dy, m, y) = parts(d); return T("hijriConv.rumi", "$dy ${months[m - 1]} $y") }
}

/** Tarihte bugün (2. analiz #124): Wikidata (CC0) kayıtları, dile göre assets/otd-<dil>.json (gen-strings.mjs) */
object OnThisDay {
    data class Item(val year: Int, val kind: String, val name: String, val desc: String) {
        val icon: String get() = when (kind) { "f" -> "🏛️"; "b" -> "🎂"; "d" -> "🕯️"; else -> "📜" }
        val title: String get() = if (kind == "f") T("otd.foundedFmt", name) else name
    }
    private var cache: Pair<String, Map<String, List<Item>>>? = null

    fun items(c: Context, d: LocalDate): List<Item> {
        val lang = Strings.lang
        if (cache?.first != lang) cache = lang to load(c, lang)
        return cache?.second?.get("%02d-%02d".format(java.util.Locale.ROOT, d.monthValue, d.dayOfMonth)).orEmpty()
    }

    private fun load(c: Context, lang: String): Map<String, List<Item>> = runCatching {
        val text = runCatching { c.assets.open("otd-$lang.json") }.getOrElse { c.assets.open("otd-en.json") }.bufferedReader().use { it.readText() }
        val o = org.json.JSONObject(text)
        o.keys().asSequence().associateWith { md ->
            val a = o.getJSONArray(md)
            (0 until a.length()).map { i -> val r = a.getJSONArray(i); Item(r.getInt(0), r.getString(1), r.getString(2), r.optString(3)) }
        }
    }.getOrDefault(emptyMap())
}

/**
 * Esma kartı (2. analiz #85): ismin Arapçası büyük, çini desenli çerçevede (telefonda çizilir; lisanslı hat görseli
 * kullanılmaz), okunuşu ve anlamı; "Dinle" telefonun kendi Arapça konuşma sesiyle (TextToSpeech) okur.
 */
@Composable
fun EsmaCard(n: Esma.Name, onClose: () -> Unit) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    var noVoice by remember { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        var engine: android.speech.tts.TextToSpeech? = null
        engine = android.speech.tts.TextToSpeech(context) { ok ->
            val r = if (ok == android.speech.tts.TextToSpeech.SUCCESS) engine?.setLanguage(java.util.Locale("ar")) else android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
            noVoice = r == null || r < 0
        }
        tts = engine
        onDispose { engine.shutdown() }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text(L("common.done")) } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                val accent = Theme.c.accent
                val accent2 = Theme.c.accent2
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        // Sekiz köşeli yıldız ve çift çerçeve
                        val c = center
                        val r = size.minDimension / 2 - 4
                        for (k in 0 until 2) {
                            val path = androidx.compose.ui.graphics.Path()
                            for (i in 0 until 16) {
                                val a = Math.PI / 8 * i - Math.PI / 2
                                val rr = if (i % 2 == 0) r - k * 10 else (r - k * 10) * 0.82f
                                val x = c.x + (rr * kotlin.math.cos(a)).toFloat()
                                val y = c.y + (rr * kotlin.math.sin(a)).toFloat()
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            path.close()
                            drawPath(path, if (k == 0) accent else accent2, style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (k == 0) 3f else 1.5f))
                        }
                        drawCircle(accent.copy(alpha = 0.08f), r * 0.62f, c)
                    }
                    Text(n.ar, color = Theme.c.ink, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
                }
                if (Strings.lang != "ar" && n.read.isNotEmpty()) Text(n.read, color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                if (Strings.lang != "ar") Text(n.meaning, color = Theme.c.muted, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                androidx.compose.material3.TextButton(onClick = { tts?.speak(n.ar, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "esma-${n.no}") }, enabled = !noVoice) {
                    Text("▶ " + L("esma.listen"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                }
                Text(L(if (noVoice) "esma.noVoice" else "esma.voiceNote"), color = Theme.c.muted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        },
    )
}
