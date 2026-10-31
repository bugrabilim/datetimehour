package tr.bumba.time

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/*
 * İbadet bölümü (30 Ekim 2026, sahibin isteği: rakip analizinde "bilerek yok" denenleri yap): Kur'an (Tanzil Arapça
 * metni + İngilizce Pickthall meali, Türkçe için Diyanet meal sitesine bağlantı), dualar, zikirmatik, namaz ve oruç
 * takibi, zekât hesaplama. Vakitler › İbadet. Veriler yalnız telefonda; rozet bilerek yok (inanç ele vermemek için).
 * iOS'taki Religion.swift ile aynı anahtarlar ve biçimler.
 */

object Quran {
    class Sura(val no: Int, val ar: String, val tname: String, val en: String, val tr: String, val mecca: Boolean, val ayas: Int, val start: Int) {
        /** Arayüz dilinde ad */
        val title: String get() = when (Strings.lang) { "tr", "az" -> tr; "ar", "fa", "ur" -> ar; else -> tname }
    }

    lateinit var suras: List<Sura>
        private set
    lateinit var juz: List<Pair<Int, Int>>
        private set
    private var arabic: List<String> = emptyList()
    private var english: List<String> = emptyList()

    fun load(c: Context) {
        if (::suras.isInitialized) return
        fun read(name: String) = c.assets.open(name).bufferedReader().use { it.readText() }
        val meta = JSONObject(read("quran-meta.json"))
        val s = meta.getJSONArray("suras")
        suras = (0 until s.length()).map { i ->
            val r = s.getJSONArray(i)
            Sura(i + 1, r.getString(0), r.getString(1), r.getString(2), r.getString(3), r.getString(4) == "M", r.getInt(5), r.getInt(6))
        }
        val j = meta.getJSONArray("juz")
        juz = (0 until j.length()).map { j.getJSONArray(it).let { r -> r.getInt(0) to r.getInt(1) } }
        arabic = read("quran-ar.txt").lineSequence().take(6236).toList()
        english = read("quran-en.txt").lineSequence().take(6236).toList()
    }

    fun ar(sura: Int, aya: Int) = arabic[suras[sura - 1].start + aya - 1]
    fun en(sura: Int, aya: Int) = english[suras[sura - 1].start + aya - 1]

    /** Diyanet'in meal sitesinde ayet */
    fun diyanetUrl(sura: Int, aya: Int) = "https://kuran.diyanet.gov.tr/kuran?sure=$sura&ayet=$aya&mode=kuran-meal"

    /** Kaldığım yer "sure:ayet" */
    var bookmark: Pair<Int, Int>?
        get() = Prefs.store.getString("quran.bookmark", null)?.split(":")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 2 }?.let { it[0] to it[1] }
        set(v) = Prefs.store.edit().putString("quran.bookmark", v?.let { "${it.first}:${it.second}" }).apply()
}

private fun openUrl(c: Context, url: String) {
    runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
private fun RtlText(text: String, size: Int, modifier: Modifier = Modifier) {
    Text(text, modifier.fillMaxWidth(), color = Theme.c.ink, fontSize = size.sp, lineHeight = (size * 1.9).sp,
        textAlign = TextAlign.Right, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl))
}

/** Kur'an: sure listesi, cüz, kaldığım yerden devam */
@Composable
fun QuranScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    remember { Quran.load(context) }
    var open by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    open?.let { (s, a) ->
        BackHandler { open = null }
        SuraScreen(s, a) { open = null }
        return
    }
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("quran.title"), onBack = onBack)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Quran.bookmark?.let { (s, a) ->
                    AppCard(Modifier.clickable { open = s to a }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bookmark, null, tint = Theme.c.accent)
                            Text("  " + T("quran.continue", Quran.suras[s - 1].title, a), Modifier.weight(1f), color = Theme.c.text)
                        }
                    }
                }
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 6.dp), singleLine = true,
                    placeholder = { Text(L("quran.search")) })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((i, j) in Quran.juz.withIndex()) {
                        Text(T("quran.juz", i + 1),
                            Modifier.background(Theme.c.surface2, RoundedCornerShape(50)).clickable { open = j }.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Theme.c.text, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            val q = query.trim().lowercase()
            val list = Quran.suras.filter { q.isEmpty() || it.tr.lowercase().contains(q) || it.tname.lowercase().contains(q) || it.ar.contains(q) || it.no.toString() == q }
            itemsIndexed(list) { _, s ->
                Row(
                    Modifier.fillMaxWidth().background(Theme.c.surface, RoundedCornerShape(12.dp)).clickable { open = s.no to 1 }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Ltr { Text("${s.no}", Modifier.width(34.dp), color = Theme.c.accent, fontWeight = FontWeight.SemiBold, style = Tnum) }
                    Column(Modifier.weight(1f)) {
                        Text(s.title, color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(T("quran.suraInfo", s.ayas, L(if (s.mecca) "quran.mecca" else "quran.medina")), color = Theme.c.muted, fontSize = 12.sp)
                    }
                    Text(s.ar, color = Theme.c.ink, fontSize = 20.sp)
                }
            }
            item { QuranCredits(); Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun QuranCredits() {
    val context = LocalContext.current
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(L("quran.credit"), color = Theme.c.muted, fontSize = 12.sp)
        Text("tanzil.net", Modifier.clickable { openUrl(context, "https://tanzil.net") }, color = Theme.c.accent, fontSize = 12.sp)
        Text("kuran.diyanet.gov.tr", Modifier.clickable { openUrl(context, "https://kuran.diyanet.gov.tr") }, color = Theme.c.accent, fontSize = 12.sp)
    }
}

/** Sure okuma: Arapça metin, isteğe bağlı İngilizce meal, Diyanet meal bağlantısı; ayete dokununca kaldığım yer */
@Composable
fun SuraScreen(sura: Int, startAya: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = Quran.suras[sura - 1]
    var size by remember { mutableIntStateOf(Prefs.store.getInt("quran.size", 24)) }
    var showEn by remember { mutableStateOf(Prefs.store.getBoolean("quran.en", Strings.lang == "en" || Strings.lang == "de")) }
    var mark by remember { mutableStateOf(Quran.bookmark) }
    val state = rememberLazyListState()
    LaunchedEffect(sura) { if (startAya > 1) state.scrollToItem(startAya) }
    Column(Modifier.fillMaxSize()) {
        SubBar("${s.no}. ${s.title}", onBack = onBack, actions = {
            IconButton(onClick = { size = (size - 2).coerceAtLeast(16); Prefs.store.edit().putInt("quran.size", size).apply() }) {
                Icon(Icons.Filled.TextDecrease, L("quran.smaller"), tint = Theme.c.accent)
            }
            IconButton(onClick = { size = (size + 2).coerceAtMost(40); Prefs.store.edit().putInt("quran.size", size).apply() }) {
                Icon(Icons.Filled.TextIncrease, L("quran.larger"), tint = Theme.c.accent)
            }
        })
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), state = state, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("quran.showEn"), Modifier.weight(1f), color = Theme.c.text, fontSize = 14.sp)
                    Switch(showEn, { showEn = it; Prefs.store.edit().putBoolean("quran.en", it).apply() },
                        colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                if (Strings.lang == "tr" || Strings.lang == "az") {
                    TextButton(onClick = { openUrl(context, Quran.diyanetUrl(sura, 1)) }) {
                        Text(L("quran.diyanet"), color = Theme.c.accent)
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Theme.c.accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
            items(s.ayas) { i ->
                val a = i + 1
                val marked = mark == (sura to a)
                Column(
                    Modifier.fillMaxWidth()
                        .background(if (marked) Theme.c.accentSoft else Theme.c.surface, RoundedCornerShape(12.dp))
                        .clickable { Quran.bookmark = sura to a; mark = sura to a }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Ltr { Text("$sura:$a", color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, style = Tnum) }
                        if (marked) Icon(Icons.Filled.Bookmark, L("quran.bookmark"), tint = Theme.c.accent, modifier = Modifier.padding(start = 6.dp).size(14.dp))
                        Spacer(Modifier.weight(1f))
                        if (Strings.lang == "tr" || Strings.lang == "az") {
                            Text(L("quran.meal"), Modifier.clickable { openUrl(context, Quran.diyanetUrl(sura, a)) }, color = Theme.c.accent, fontSize = 12.sp)
                        }
                    }
                    RtlText(Quran.ar(sura, a), size)
                    if (showEn) Text(Quran.en(sura, a), color = Theme.c.muted, fontSize = 14.sp)
                }
            }
            item { QuranCredits() }
        }
    }
}

/** Dualar (Kur'an'dan olanlar Tanzil metninden) */
object Duas {
    class Dua(val id: String, val title: String, val whenText: String, val ar: String, val okunus: String, val mean: String?, val refs: List<String>)

    fun load(c: Context): List<Dua> = runCatching {
        val a = JSONArray(c.assets.open("duas.json").bufferedReader().use { it.readText() })
        val lang = Strings.lang
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val mean = o.getJSONObject("mean")
            val refs = o.optJSONArray("refs")?.let { r -> (0 until r.length()).map { r.getString(it) } } ?: emptyList()
            // Anlam: arayüz dilinde; Kur'an'dan olanlarda İngilizce meal (tr/az'da Diyanet bağlantısı), Arapça yazılı dillerde (ar, fa, ur) yok
            val m = when {
                lang == "ar" -> null
                refs.isNotEmpty() -> if (lang in listOf("en", "de", "id", "fr", "ru")) mean.optString("en") else null
                else -> mean.optString(lang).ifEmpty { mean.optString("en") }
            }
            Dua(o.getString("id"), o.getJSONObject("title").getString(lang), o.getJSONObject("when").getString(lang),
                o.getString("ar"), o.optString("okunus"), m, refs)
        }
    }.getOrDefault(emptyList())
}

@Composable
fun DuasScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val duas = remember { Duas.load(context) }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("dua.title"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (d in duas) AppCard {
                Text(d.title, color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(d.whenText, color = Theme.c.muted, fontSize = 12.sp)
                RtlText(d.ar, 22)
                if (Strings.lang == "tr" || Strings.lang == "az") Text(d.okunus, color = Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                d.mean?.let { Text(it, color = Theme.c.muted, fontSize = 14.sp) }
                if (d.refs.isNotEmpty() && (Strings.lang == "tr" || Strings.lang == "az")) {
                    for (r in d.refs) {
                        val (s, a) = r.split(":").map { it.toInt() }
                        Text(T("dua.diyanetRef", r), Modifier.clickable { openUrl(context, Quran.diyanetUrl(s, a)) }, color = Theme.c.accent, fontSize = 13.sp)
                    }
                }
            }
            Text(L("dua.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}

/** Zikirmatik: sayaç, hedef, namaz sonrası tesbihat (33 + 33 + 33) */
object Zikir {
    class Kind(val id: String, val ar: String)
    val KINDS = listOf(
        Kind("subhanallah", "سُبْحَانَ اللَّهِ"), Kind("elhamdulillah", "الْحَمْدُ لِلَّهِ"), Kind("allahuekber", "اللَّهُ أَكْبَرُ"),
        Kind("tevhid", "لَا إِلَهَ إِلَّا اللَّهُ"), Kind("istigfar", "أَسْتَغْفِرُ اللَّهَ"), Kind("salavat", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"),
        Kind("free", ""),
    )
    val TARGETS = listOf(33, 99, 100, 500, 1000, 0)
    /** Tesbihat sırası: her biri 33 */
    val TESBIHAT = listOf("subhanallah", "elhamdulillah", "allahuekber")

    fun name(id: String) = L("zikir.kind.$id")

    /** Tesbihatta sayaca göre o anki zikir ve bitip bitmediği (99'da biter) */
    fun tesbihatStep(count: Int): Pair<String, Boolean> =
        TESBIHAT[minOf(count / 33, 2)] to (count >= 99)
}

@Composable
fun ZikirScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sp = Prefs.store
    var kind by remember { mutableStateOf(sp.getString("zikir.kind", "subhanallah") ?: "subhanallah") }
    var target by remember { mutableIntStateOf(sp.getInt("zikir.target", 33)) }
    var tesbihat by remember { mutableStateOf(sp.getBoolean("zikir.tesbihat", false)) }
    var count by remember { mutableIntStateOf(sp.getInt("zikir.count", 0)) }
    var total by remember { mutableIntStateOf(sp.getInt("zikir.total", 0)) }
    val today = LocalDate.now().toString()
    var todayCount by remember { mutableIntStateOf(sp.getString("zikir.today", "")?.split("|")?.takeIf { it.size == 2 && it[0] == today }?.get(1)?.toIntOrNull() ?: 0) }
    var confirm by remember { mutableStateOf(false) }
    fun save() {
        sp.edit().putString("zikir.kind", kind).putInt("zikir.target", target).putBoolean("zikir.tesbihat", tesbihat)
            .putInt("zikir.count", count).putInt("zikir.total", total).putString("zikir.today", "$today|$todayCount").apply()
    }
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    fun buzz(strong: Boolean) {
        if (Build.VERSION.SDK_INT >= 29) runCatching {
            vibrator?.vibrate(VibrationEffect.createPredefined(if (strong) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_TICK))
        }
    }
    val (stepKind, done) = if (tesbihat) Zikir.tesbihatStep(count) else kind to (target > 0 && count >= target)
    val shown = Zikir.KINDS.first { it.id == stepKind }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("zikir.title"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("zikir.tesbihat"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                Switch(tesbihat, { tesbihat = it; count = 0; save() }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
            }
            if (!tesbihat) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (k in Zikir.KINDS) {
                        val on = k.id == kind
                        Text(Zikir.name(k.id),
                            Modifier.background(if (on) Theme.c.accent.copy(alpha = 0.22f) else Theme.c.surface2, RoundedCornerShape(50))
                                .clickable { kind = k.id; count = 0; save() }.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = if (on) Theme.c.accent else Theme.c.text, fontSize = 14.sp)
                    }
                }
                ChoiceRow(L("zikir.target"), if (target == 0) L("zikir.noTarget") else "$target",
                    Zikir.TARGETS.map { it to (if (it == 0) L("zikir.noTarget") else "$it") }) { target = it; save() }
            }
            if (shown.ar.isNotEmpty()) Text(shown.ar, color = Theme.c.ink, fontSize = 30.sp, textAlign = TextAlign.Center)
            Text(Zikir.name(shown.id), color = Theme.c.muted, fontSize = 15.sp)
            // Büyük dokunma alanı
            Box(
                Modifier.size(240.dp).background(if (done) Theme.c.accent else Theme.c.accentSoft, CircleShape)
                    .clickable {
                        count++; total++; todayCount++
                        val (_, finished) = if (tesbihat) Zikir.tesbihatStep(count) else kind to (target > 0 && count >= target)
                        val stepEnd = tesbihat && count % 33 == 0
                        buzz(finished || stepEnd)
                        save()
                    }
                    .semantics { contentDescription = L("zikir.tap") },
                contentAlignment = Alignment.Center,
            ) {
                Ltr {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$count", color = if (done) Color.White else Theme.c.ink, fontSize = 64.sp, fontWeight = FontWeight.Bold, style = Tnum)
                        val goal = if (tesbihat) 99 else target
                        if (goal > 0) Text("/ $goal", color = if (done) Color.White else Theme.c.muted, fontSize = 16.sp, style = Tnum)
                    }
                }
            }
            if (done) Text(L("zikir.done"), color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundButton("−1", false, Modifier.width(110.dp)) { if (count > 0) { count--; total = maxOf(0, total - 1); todayCount = maxOf(0, todayCount - 1); save() } }
                RoundButton(L("zikir.reset"), false, Modifier.width(140.dp)) { confirm = true }
            }
            Row2(L("zikir.today"), "$todayCount")
            Row2(L("zikir.total"), "$total")
            Text(L("zikir.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        text = { Text(L("zikir.resetConfirm")) },
        confirmButton = { TextButton(onClick = { count = 0; save(); confirm = false }) { Text(L("zikir.reset"), color = DangerRed) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(L("badges.deleteNo")) } },
    )
}

/** Namaz ve oruç takibi: kılınan vakitler (gün gün), kaza sayaçları, Ramazan orucu */
object Tracker {
    val PRAYERS = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")
    val KAZA = PRAYERS + "witr"

    private fun obj(key: String) = runCatching { JSONObject(Prefs.store.getString(key, "{}")) }.getOrDefault(JSONObject())

    fun dayMask(d: LocalDate): Int = obj("track.prayer").optInt(d.toString(), 0)
    fun setDayMask(d: LocalDate, mask: Int) {
        val o = obj("track.prayer")
        if (mask == 0) o.remove(d.toString()) else o.put(d.toString(), mask)
        Prefs.store.edit().putString("track.prayer", o.toString()).apply()
    }

    fun kaza(id: String): Int = obj("track.kaza").optInt(id, 0)
    fun setKaza(id: String, n: Int) {
        val o = obj("track.kaza").put(id, maxOf(0, n))
        Prefs.store.edit().putString("track.kaza", o.toString()).apply()
    }

    fun fasted(d: LocalDate): Boolean = obj("track.fast").optBoolean(d.toString(), false)
    fun setFasted(d: LocalDate, on: Boolean) {
        val o = obj("track.fast")
        if (on) o.put(d.toString(), true) else o.remove(d.toString())
        Prefs.store.edit().putString("track.fast", o.toString()).apply()
    }

    /** Son 7 günde kılınan vakit sayısı */
    fun weekCount(today: LocalDate) = (0..6).sumOf { Integer.bitCount(dayMask(today.minusDays(it.toLong()))) }

    fun clear() = Prefs.store.edit().remove("track.prayer").remove("track.kaza").remove("track.fast").apply()
}

@Composable
fun TrackerScreen(onBack: () -> Unit) {
    val today = LocalDate.now()
    var day by remember { mutableStateOf(today) }
    var version by remember { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    val ramadan = remember { AppData.ramadans.firstOrNull { today in it } ?: AppData.ramadans.lastOrNull { it.endInclusive < today } ?: AppData.ramadans.firstOrNull() }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("track.title"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            key(version) {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { day = day.minusDays(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("stage.prev"), tint = Theme.c.accent) }
                        Text(Fmt.longDate(day), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        IconButton(onClick = { if (day < today) day = day.plusDays(1) }, enabled = day < today) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("stage.next"), tint = Theme.c.accent)
                        }
                    }
                    val mask = Tracker.dayMask(day)
                    for ((i, p) in Tracker.PRAYERS.withIndex()) {
                        val on = mask and (1 shl i) != 0
                        Row(Modifier.fillMaxWidth().clickable { Tracker.setDayMask(day, mask xor (1 shl i)); version++ }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(on, { Tracker.setDayMask(day, mask xor (1 shl i)); version++ },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Theme.c.accent))
                            Text(L("track.prayer.$p"), color = Theme.c.text, fontSize = 16.sp)
                        }
                    }
                    Text(T("track.week", Tracker.weekCount(today)), color = Theme.c.muted, fontSize = 13.sp)
                }
                AppCard {
                    SectionTitle(L("track.kaza"))
                    for (k in Tracker.KAZA) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L("track.prayer.$k"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                        IconButton(onClick = { Tracker.setKaza(k, Tracker.kaza(k) - 1); version++ }) { Icon(Icons.Filled.Remove, L("track.kazaPrayed"), tint = Theme.c.accent) }
                        Ltr { Text("${Tracker.kaza(k)}", Modifier.width(44.dp), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, style = Tnum) }
                        IconButton(onClick = { Tracker.setKaza(k, Tracker.kaza(k) + 1); version++ }) { Icon(Icons.Filled.Add, L("track.kazaAdd"), tint = Theme.c.accent) }
                    }
                    Text(L("track.kazaNote"), color = Theme.c.muted, fontSize = 12.sp)
                }
                AppCard {
                    SectionTitle(L("track.fast"))
                    if (ramadan != null) {
                        val days = generateSequence(ramadan.start) { it.plusDays(1) }.takeWhile { it <= ramadan.endInclusive && it <= today }.toList()
                        if (days.isEmpty()) Text(T("track.fastUpcoming", Fmt.longDate(ramadan.start)), color = Theme.c.muted, fontSize = 14.sp)
                        else {
                            Text(T("track.fastCount", days.count { Tracker.fasted(it) }, days.size), color = Theme.c.text, fontSize = 15.sp)
                            Ltr {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for ((i, d) in days.withIndex()) {
                                        val on = Tracker.fasted(d)
                                        Box(Modifier.size(34.dp).background(if (on) Theme.c.accent else Theme.c.surface2, CircleShape)
                                            .clickable { Tracker.setFasted(d, !on); version++ }
                                            .semantics { contentDescription = Fmt.longDate(d) },
                                            contentAlignment = Alignment.Center) {
                                            Text("${i + 1}", color = if (on) Color.White else Theme.c.text, fontSize = 12.sp, style = Tnum)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L("track.kazaFast"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                        IconButton(onClick = { Tracker.setKaza("fast", Tracker.kaza("fast") - 1); version++ }) { Icon(Icons.Filled.Remove, null, tint = Theme.c.accent) }
                        Ltr { Text("${Tracker.kaza("fast")}", Modifier.width(44.dp), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, style = Tnum) }
                        IconButton(onClick = { Tracker.setKaza("fast", Tracker.kaza("fast") + 1); version++ }) { Icon(Icons.Filled.Add, null, tint = Theme.c.accent) }
                    }
                }
            }
            Text(L("track.privacy"), color = Theme.c.muted, fontSize = 12.sp)
            TextButton(onClick = { confirm = true }) { Text(L("track.clear"), color = DangerRed) }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        text = { Text(L("track.clearConfirm")) },
        confirmButton = { TextButton(onClick = { Tracker.clear(); version++; confirm = false }) { Text(L("track.clear"), color = DangerRed) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(L("badges.deleteNo")) } },
    )
}

/** Zekât hesaplama: nisab 80,18 g altın (Diyanet), oran 1/40; altın fiyatını kullanıcı girer (dış servis yok) */
object Zakat {
    const val NISAB_GRAMS = 80.18
    val FIELDS = listOf("cash", "goldGrams", "goldPrice", "silverValue", "foreign", "trade", "receivable", "debt")

    data class Result(val net: Double, val nisab: Double?, val due: Boolean, val zakat: Double)

    fun calc(v: Map<String, Double>): Result {
        fun g(k: String) = v[k] ?: 0.0
        val goldPrice = g("goldPrice")
        val assets = g("cash") + g("goldGrams") * goldPrice + g("silverValue") + g("foreign") + g("trade") + g("receivable")
        val net = maxOf(0.0, assets - g("debt"))
        val nisab = if (goldPrice > 0) NISAB_GRAMS * goldPrice else null
        val due = nisab != null && net >= nisab
        return Result(net, nisab, due, if (due) net / 40 else 0.0)
    }

    fun load(): Map<String, Double> = runCatching {
        val o = JSONObject(Prefs.store.getString("zakat.values", "{}"))
        FIELDS.filter { o.has(it) }.associateWith { o.getDouble(it) }
    }.getOrDefault(emptyMap())

    fun save(v: Map<String, Double>) {
        val o = JSONObject()
        for ((k, x) in v) o.put(k, x)
        Prefs.store.edit().putString("zakat.values", o.toString()).apply()
    }
}

@Composable
fun ZakatScreen(onBack: () -> Unit) {
    var values by remember { mutableStateOf(Zakat.load()) }
    val texts = remember { mutableStateOf(values.mapValues { fmtNum(it.value) }) }
    val r = Zakat.calc(values)
    Column(Modifier.fillMaxSize()) {
        SubBar(L("zakat.title"), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(L("zakat.intro"), color = Theme.c.muted, fontSize = 13.sp)
            for (f in Zakat.FIELDS) {
                OutlinedTextField(texts.value[f] ?: "", { s ->
                    val clean = s.filter { it.isDigit() || it == ',' || it == '.' }.take(14)
                    texts.value = texts.value + (f to clean)
                    val num = clean.replace(",", ".").toDoubleOrNull()
                    values = if (num == null) values - f else values + (f to num)
                    Zakat.save(values)
                }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(L("zakat.f.$f")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            AppCard {
                Row2(L("zakat.net"), fmtMoney(r.net), bold = true)
                Row2(T("zakat.nisab", fmtNum(Zakat.NISAB_GRAMS)), r.nisab?.let { fmtMoney(it) } ?: "–")
                Text(
                    when {
                        r.nisab == null -> L("zakat.needGold")
                        r.due -> L("zakat.due")
                        else -> L("zakat.notDue")
                    },
                    color = if (r.due) Theme.c.accent else Theme.c.muted, fontSize = 14.sp,
                )
                if (r.due) Row2(L("zakat.amount"), fmtMoney(r.zakat), bold = true, color = Theme.c.accent)
            }
            Text(L("zakat.note"), color = Theme.c.muted, fontSize = 12.sp)
            // Fitre (rakip analizi #14): Diyanet'in açıkladığı kişi başı tutar (assets/fitre.json)
            Fitre.info?.let { f ->
                var people by remember { mutableStateOf(1) }
                AppCard {
                    SectionTitle(L("zakat.fitreHeading"))
                    Text(T("zakat.fitreText", f.year, f.amount, Fmt.longDate(f.date)), color = Theme.c.text, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L("zakat.fitrePeople"), Modifier.weight(1f), color = Theme.c.text, fontSize = 14.sp)
                        IconButton(onClick = { people = (people - 1).coerceIn(1, 99) }) { Icon(Icons.Filled.Remove, null, tint = Theme.c.accent) }
                        Text(people.toString(), color = Theme.c.text, fontSize = 17.sp, style = Tnum)
                        IconButton(onClick = { people = (people + 1).coerceIn(1, 99) }) { Icon(Icons.Filled.Add, null, tint = Theme.c.accent) }
                    }
                    Row2(L("zakat.fitreTotal"), fmtMoney((f.amount * people).toDouble()) + " TL", bold = true, color = Theme.c.accent)
                    Text(L("zakat.fitreNote"), color = Theme.c.muted, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Fitre tutarı (gen-strings, src/data/fitre.json): en son açıklanan yıl */
object Fitre {
    data class Info(val year: Int, val amount: Int, val date: java.time.LocalDate)
    var info: Info? = null
        private set

    fun load(json: String) {
        info = try {
            val o = org.json.JSONObject(json)
            Info(o.getInt("year"), o.getInt("amount"), java.time.LocalDate.parse(o.getString("date")))
        } catch (_: Exception) { null }
    }
}

private fun fmtNum(x: Double) = if (x == Math.floor(x)) x.toLong().toString() else x.toString().replace(".", Strings.decimalPoint)

private fun fmtMoney(x: Double): String {
    val loc = Strings.numberLocale
    return java.text.NumberFormat.getNumberInstance(loc).apply { maximumFractionDigits = 2; minimumFractionDigits = 2 }.format(x)
}
