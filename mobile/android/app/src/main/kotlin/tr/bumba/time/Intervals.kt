package tr.bumba.time

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

/**
 * Birden çok adlı zamanlayıcı ve aralık (HIIT) antrenmanı (30 Ekim 2026, rakip analizi #12), iOS'taki Intervals.swift ile
 * aynı kayıtlar: "timers" (JSON) ve "hiit.cfg" / "hiit.start" / "hiit.paused". Sesli duyuru telefonun metin okuma
 * motoruyla (dosya ya da internet yok). Aralık antrenmanının duyuruları HiitService'te (ekran kapalıyken de);
 * zamanlayıcının adı ekran açıkken söylenir. Bitişte her zamanlayıcı kendi alarmını çalar.
 */
data class TimerItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    /** Toplam süre (ms) */
    val total: Long,
    /** Çalışırken bitiş anı (ms; 0 = durmuş) */
    val end: Long = 0,
    /** Duraklatılmışken kalan (ms) */
    val left: Long = 0,
    /** 2. analiz #133: renk (0 = yok; geri sayım kartlarıyla aynı palet) ve bitince kendiliğinden yeniden başlama */
    val color: Int = 0,
    val repeats: Boolean = false,
) {
    val running: Boolean get() = end > 0
    fun remaining(now: Long): Long = if (running) maxOf(0, end - now) else left
    val alarmId: String get() = "timer-$id"
    val doneTitle: String get() = if (name.isBlank()) L("tools.countdown.finished") else T("timers.doneNamed", name)

    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name).put("total", total).put("end", end).put("left", left)
        .put("color", color).put("repeats", repeats)

    companion object {
        private const val KEY = "timers"

        /** Kilitli: eski kaydın taşınması iki kez yapılmasın (geri sayımlardaki gibi) */
        @Synchronized
        fun load(): List<TimerItem> {
            val sp = Prefs.store
            // Eski tek zamanlayıcı (tm.end / tm.left) bir kez listeye taşınır
            val oldEnd = sp.getLong("tm.end", 0L)
            val oldLeft = sp.getLong("tm.left", 0L)
            val list = runCatching {
                val a = JSONArray(sp.getString(KEY, "[]"))
                (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    TimerItem(o.getString("id"), o.optString("name"), o.getLong("total"), o.optLong("end"), o.optLong("left"),
                        o.optInt("color", 0), o.optBoolean("repeats", false))
                }
            }.getOrDefault(emptyList())
            if (oldEnd > 0 || oldLeft > 0) {
                val now = System.currentTimeMillis()
                val migrated = list + TimerItem(total = maxOf(oldLeft, oldEnd - now, 1000L), end = oldEnd, left = oldLeft)
                sp.edit().remove("tm.end").remove("tm.left").apply()
                save(migrated)
                return migrated
            }
            return list
        }

        fun save(items: List<TimerItem>) {
            Prefs.store.edit().putString(KEY, JSONArray(items.map { it.toJson() }).toString()).apply()
        }

        /** Arayüzsüz yeni zamanlayıcı (asistan ve başka uygulamalar, ClockIntentActivity): kayıt + bitiş alarmı */
        fun startNew(c: android.content.Context, ms: Long, name: String = ""): TimerItem {
            val e = System.currentTimeMillis() + ms
            val t = TimerItem(name = name.trim(), total = ms, end = e)
            AlarmScheduler.schedule(c, t.alarmId, e, t.doneTitle, AlarmScheduler.TIMER)
            save(load() + t)
            TimerHistory.record(t.name, ms)
            LiveNotifications.sync(c)
            return t
        }
    }
}

/**
 * Zamanlayıcı geçmişi (rakip analizi listesi #82): son başlatılan süreler, aynı ad + süre bir kez (en yenisi üstte,
 * en çok 12); kayıt `timers.history` (JSON [{n, t}], t ms; iOS'la aynı)
 */
object TimerHistory {
    data class Entry(val n: String, val t: Long)
    private const val KEY = "timers.history"
    fun load(): List<Entry> = runCatching {
        val a = org.json.JSONArray(Prefs.store.getString(KEY, "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Entry(o.optString("n"), o.getLong("t")) } }
    }.getOrDefault(emptyList())
    fun record(name: String, ms: Long) {
        val e = Entry(name, ms)
        val list = (listOf(e) + load().filter { it != e }).take(12)
        Prefs.store.edit().putString(KEY, org.json.JSONArray(list.map { org.json.JSONObject().put("n", it.n).put("t", it.t) }).toString()).apply()
    }
    fun clear() = Prefs.store.edit().remove(KEY).apply()
}

/** Aralık antrenmanı: ısınma, çalışma ve dinlenme süreleri (sn), tur sayısı */
data class HiitConfig(
    val warmup: Int = 10, val work: Int = 30, val rest: Int = 15, val rounds: Int = 8,
    /** Çok adımlı rutin (rakip analizi listesi #83): adlı adımlar her turda sırayla; boşsa klasik aralık */
    val name: String = "", val steps: List<Step> = emptyList(),
) {
    /** Rutin adımı: ad, süre (sn), dinlenme mi (renk ve sesli duyuru için) */
    data class Step(val n: String, val s: Int, val r: Boolean = false)

    val isRoutine: Boolean get() = steps.isNotEmpty()
    val totalSeconds: Int get() = if (isRoutine) rounds * steps.sumOf { it.s } else warmup + rounds * work + (rounds - 1) * rest
    override fun toString() = "$warmup,$work,$rest,$rounds"

    /** Geçen süreye (sn) göre evre: kind (warmup, work, rest, done), tur (1'den), evrenin bitişine kalan (sn); rutinde adım adı */
    data class Phase(val kind: String, val round: Int, val left: Int, val index: Int, val name: String = "")

    fun phase(elapsed: Int): Phase {
        var t = elapsed
        if (isRoutine) {
            for (r in 1..rounds) for ((i, st) in steps.withIndex()) {
                if (t < st.s) return Phase(if (st.r) "rest" else "work", r, st.s - t, (r - 1) * steps.size + i, st.n)
                t -= st.s
            }
            return Phase("done", rounds, 0, rounds * steps.size)
        }
        if (t < warmup) return Phase("warmup", 0, warmup - t, 0)
        t -= warmup
        for (r in 1..rounds) {
            if (t < work) return Phase("work", r, work - t, r * 2 - 1)
            t -= work
            if (r == rounds) break
            if (t < rest) return Phase("rest", r, rest - t, r * 2)
            t -= rest
        }
        return Phase("done", rounds, 0, rounds * 2 + 1)
    }

    /** Evrenin toplam süresi (sn) */
    fun duration(p: Phase): Int = when {
        p.kind == "done" -> 0
        isRoutine -> steps[p.index % steps.size].s
        p.kind == "warmup" -> warmup
        p.kind == "work" -> work
        else -> rest
    }

    /** Antrenmanın bölümleri (süresi 0 olanlar yok): başlangıç (sn) ve süre; sıradakiler listesi ve adım atlama için */
    data class Seg(val phase: Phase, val start: Int, val dur: Int)

    fun segments(): List<Seg> {
        val out = ArrayList<Seg>()
        var t = 0
        while (t < totalSeconds) {
            val p = phase(t)
            if (p.kind == "done") break
            out.add(Seg(p, t, p.left))
            t += p.left
        }
        return out
    }

    /** İsteğe bağlı duyurular (2. analiz #135): dinlenmede sıradaki adım, evrenin yarısı, son 10 saniye */
    data class Cues(val next: Boolean = false, val half: Boolean = false, val ten: Boolean = false) {
        companion object {
            fun load() = Cues(Prefs.store.getBoolean("hiit.cue.next", true), Prefs.store.getBoolean("hiit.cue.half", false), Prefs.store.getBoolean("hiit.cue.ten", false))
        }
    }

    /** Söylenecek: evre değişince (ya da ilk bakışta) evrenin adı (dinlenme ve ısınmada istenirse sıradaki adım), evre içinde
     *  son 3 saniyede sayı, istenirse son 10 saniye ve evrenin yarısı (yalnız 20 sn ve uzun evrelerde); yoksa null */
    fun cue(last: Phase?, p: Phase, cues: Cues = Cues()): String? {
        if (last == null || p.index != last.index) {
            val nx = if (cues.next && (p.kind == "rest" || p.kind == "warmup")) segments().firstOrNull { it.phase.index > p.index } else null
            return word(p) + (nx?.let { ". " + T("routine.next", title(it.phase)) } ?: "")
        }
        if (p.kind == "done") return null
        if (p.left in 1..3 && p.left != last.left) return p.left.toString()
        val d = duration(p)
        if (cues.ten && d >= 20 && last.left > 10 && p.left <= 10) return L("hiit.say.ten")
        if (cues.half && d >= 20 && last.left > d / 2 && p.left <= d / 2) return L("hiit.say.half")
        return null
    }

    /** Evrenin sesli adı: "Isın", "Çalış" (son turda "Son tur. Çalış"), "Dinlen", "Bitti"; rutinde adımın adı */
    fun word(p: Phase): String = when {
        p.kind == "done" -> L("hiit.say.done")
        isRoutine -> (if (p.round == rounds && rounds > 1 && p.index == (rounds - 1) * steps.size) L("hiit.say.last") + ". " else "") + p.name
        p.kind == "warmup" -> L("hiit.say.warmup")
        p.kind == "work" -> if (p.round == rounds && rounds > 1) L("hiit.say.last") + ". " + L("hiit.say.work") else L("hiit.say.work")
        else -> L("hiit.say.rest")
    }

    /** Ekrandaki ad */
    fun title(p: Phase): String = when {
        p.name.isNotEmpty() -> p.name
        p.kind == "warmup" -> L("hiit.warmup"); p.kind == "work" -> L("hiit.work"); p.kind == "rest" -> L("hiit.rest"); else -> L("hiit.done")
    }

    fun routineJson(): org.json.JSONObject = org.json.JSONObject().put("n", name).put("rounds", rounds)
        .put("steps", org.json.JSONArray(steps.map { org.json.JSONObject().put("n", it.n).put("s", it.s).put("r", it.r) }))

    companion object {
        fun fromRoutine(o: org.json.JSONObject): HiitConfig {
            val a = o.optJSONArray("steps") ?: org.json.JSONArray()
            val steps = (0 until a.length()).map { a.getJSONObject(it).let { s -> Step(s.getString("n"), s.getInt("s"), s.optBoolean("r")) } }
            return HiitConfig(rounds = o.optInt("rounds", 1).coerceAtLeast(1), name = o.optString("n"), steps = steps)
        }

        /** Etkin ayar: seçili rutin (hiit.routine) ya da klasik aralık (hiit.cfg) */
        fun load(): HiitConfig {
            Prefs.store.getString("hiit.routine", "")?.takeIf { it.isNotEmpty() }?.let { raw ->
                runCatching { return fromRoutine(org.json.JSONObject(raw)) }
            }
            return runCatching {
                val p = Prefs.store.getString("hiit.cfg", "")!!.split(",").map { it.toInt() }
                HiitConfig(p[0], p[1], p[2], p[3])
            }.getOrDefault(HiitConfig())
        }
    }
}

/** Rutin şablonları ve kullanıcının rutinleri (`routines`: [{id, n, rounds, steps}]; iOS'taki Routines ile aynı) */
object Routines {
    private const val KEY = "routines"
    private fun ex(k: String) = L("routine.ex.$k")
    private fun work(k: String, s: Int) = HiitConfig.Step(ex(k), s)
    private fun rest(s: Int) = HiitConfig.Step(L("hiit.rest"), s, true)

    /** Hazır şablonlar (kimlik, yapı) */
    fun templates(): List<Pair<String, HiitConfig>> = listOf(
        "tabata" to HiitConfig(rounds = 8, name = L("routine.tabata"), steps = listOf(HiitConfig.Step(L("hiit.work"), 20), rest(10))),
        "seven" to HiitConfig(rounds = 1, name = L("routine.seven"), steps = listOf("jacks", "wallsit", "pushup", "crunch", "stepup", "squat", "dips", "plank", "highknees", "lunge", "pushrot", "sideplank")
            .flatMapIndexed { i, k -> if (i < 11) listOf(work(k, 30), rest(10)) else listOf(work(k, 30)) }),
        "emom" to HiitConfig(rounds = 10, name = L("routine.emom"), steps = listOf(HiitConfig.Step(L("routine.minute"), 60))),
        "warmup" to HiitConfig(rounds = 1, name = L("routine.warmup"), steps = listOf("march", "arms", "hips", "knees", "jacks").map { work(it, 45) }),
        "stretch" to HiitConfig(rounds = 1, name = L("routine.stretch"), steps = listOf("neck", "shoulders", "hamstring", "quads", "back").map { work(it, 40) }),
    )

    fun mine(): List<Pair<String, HiitConfig>> = runCatching {
        val a = org.json.JSONArray(Prefs.store.getString(KEY, "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> o.getString("id") to HiitConfig.fromRoutine(o) } }
    }.getOrDefault(emptyList())

    fun save(list: List<Pair<String, HiitConfig>>) {
        Prefs.store.edit().putString(KEY, org.json.JSONArray(list.map { (id, c) -> c.routineJson().put("id", id) }).toString()).apply()
    }

    /** Etkin rutini seç (null: klasik aralığa dön) */
    fun select(c: HiitConfig?) {
        Prefs.store.edit().putString("hiit.routine", c?.routineJson()?.toString() ?: "").apply()
    }
}

/** Sesli duyuru: telefonun metin okuma motoru (Azericede Türkçe ses, iOS'taki gibi) */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    val on: Boolean get() = Prefs.store.getBoolean("tools.voice", true)

    /** Uygulamanın diline göre ses (Azericede Türkçe) */
    fun locale(): Locale = when (Strings.lang) {
        "en" -> Locale.US
        "de" -> Locale.GERMANY
        "ar" -> Locale.forLanguageTag("ar-SA")
        "id" -> Locale.forLanguageTag("id-ID")
        "ur" -> Locale.forLanguageTag("ur-PK")
        "fa" -> Locale.forLanguageTag("fa-IR")
        "fr" -> Locale.FRANCE
        "ru" -> Locale.forLanguageTag("ru-RU")
        "ja" -> Locale.JAPAN
        "ko" -> Locale.KOREA
        "zh" -> Locale.SIMPLIFIED_CHINESE
        "fil" -> Locale.forLanguageTag("fil-PH")
        "th" -> Locale.forLanguageTag("th-TH")
        else -> Locale.forLanguageTag("tr-TR")
    }

    fun prepare(c: Context) {
        if (tts != null) return
        tts = TextToSpeech(c.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts?.language = locale()
        }
    }

    fun say(text: String) {
        if (!on || !ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "say")
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}

@Composable
private fun VoiceToggle() {
    var on by remember { mutableStateOf(Speaker.on) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(L("tools.voice"), color = Theme.c.text, fontSize = 15.sp)
            Text(L("tools.voiceNote"), color = Theme.c.muted, fontSize = 12.sp)
        }
        Switch(on, { on = it; Prefs.store.edit().putBoolean("tools.voice", it).apply() },
            colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
    }
}

/** Sesli duyuru motoru ekran açıkken hazır tutulur */
@Composable
private fun SpeakerLifecycle() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        Speaker.prepare(context)
        onDispose { Speaker.shutdown() }
    }
}

/** Birden çok adlı zamanlayıcı: üstte çalışanlar, altta yenisi (ad, süre çarkları, hazır süreler) */
@Composable
fun TimersScreen() {
    val context = LocalContext.current
    val ask = rememberNotifyAsk()
    SpeakerLifecycle()
    var items by remember { mutableStateOf(TimerItem.load()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var h by rememberSaveable { mutableIntStateOf(0) }
    var m by rememberSaveable { mutableIntStateOf(5) }
    var s by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf(TimerHistory.load()) }
    // Kaldırılanı geri almak için (2. analiz #133); süre girişi tekerlek ya da tuş takımı (#134)
    var removed by remember { mutableStateOf<TimerItem?>(null) }
    var keypad by remember { mutableStateOf(Prefs.store.getBoolean("timers.keypad", false)) }
    var digits by rememberSaveable { mutableStateOf("") }
    // Görsel (rakamsız) zamanlayıcı tam ekranı (2. analiz #140)
    var visualId by remember { mutableStateOf<String?>(null) }
    fun update(list: List<TimerItem>) { items = list; TimerItem.save(list); LiveNotifications.sync(context) }
    fun start(t: TimerItem, ms: Long): TimerItem {
        val e = System.currentTimeMillis() + ms
        AlarmScheduler.schedule(context, t.alarmId, e, t.doneTitle, AlarmScheduler.TIMER)
        return t.copy(end = e, left = 0)
    }
    // Ekran açıkken biten zamanlayıcı sesli duyurulur (bir kez); "tekrarla" seçiliyse yeniden başlar (uygulama açıkken)
    val announced = remember { HashSet<String>() }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            for (t in items) if (t.running && t.end <= now && announced.add(t.id + t.end)) {
                Speaker.say(t.doneTitle)
                if (t.repeats) update(items.map { if (it.id == t.id) start(it, it.total) else it })
            }
            delay(250)
        }
    }
    /** Süreyi uzatır ya da kısaltır (sayarken bitiş, duraklatılmışken kalan; en az 1 sn) */
    fun adjust(t: TimerItem, rem: Long, delta: Long) {
        update(items.map {
            if (it.id != t.id) it
            else if (it.running && rem > 0) start(it.copy(total = maxOf(1000L, it.total + delta)), maxOf(1000L, rem + delta))
            else if (!it.running) it.copy(total = maxOf(1000L, it.total + delta), left = maxOf(1000L, it.left + delta))
            else it
        })
    }
    fun add(ms: Long, preset: String? = null) {
        if (ms <= 0) return
        BadgeStore.hit(Badge.ZAMANLAYICI)
        ask()
        val n = (preset ?: name).trim()
        update(items + start(TimerItem(name = n, total = ms), ms))
        TimerHistory.record(n, ms)
        history = TimerHistory.load()
        if (preset == null) name = ""
    }

    // Araç kalıbı (2. analiz #200): süren sayaçlar ve geçmiş ekranda, yeni sayaç kurma alt sayfada (altta tek ana düğme)
    var newSheet by rememberSaveable { mutableStateOf(false) }
    ToolFrame(
        primary = L("timers.new"), onPrimary = { newSheet = true },
        menu = if (history.isNotEmpty()) listOf(L("timers.history.clear") to { TimerHistory.clear(); history = emptyList() }) else emptyList(),
    ) {
        if (items.isEmpty() && removed == null) {
            EmptyState(Icons.Filled.HourglassBottom, L("timers.heading"), L("timers.multiNote"))
        }
        if (items.isNotEmpty()) {
            SectionTitle(L("timers.heading"))
            AppCard(padding = 4) {
                items.forEachIndexed { i, t ->
                    val rem = t.remaining(now)
                    val done = t.running && rem <= 0
                    val tint = if (t.color in 1 until CountdownItem.PALETTE.size) CountdownItem.PALETTE[t.color] else Theme.c.accent
                    var menu by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Kalan süre halkası (2. analiz #101)
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(
                                progress = { if (t.total > 0) (rem.toFloat() / t.total).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier.size(40.dp), color = tint, trackColor = Theme.c.surface2, strokeWidth = 5.dp,
                            )
                            if (t.repeats) Text("↻", color = tint, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name.ifBlank { clock(t.total / 1000) }, color = Theme.c.muted, fontSize = 13.sp, maxLines = 1)
                            Ltr {
                                Text(if (done) L("tools.countdown.finished") else clock((rem + 999) / 1000),
                                    color = if (done) tint else Theme.c.text, fontSize = 32.sp, fontWeight = FontWeight.Light, style = Tnum)
                            }
                            // Bitiş saati (2. analiz #133)
                            if (t.running && !done) Text("🕓 " + Fmt.time(java.time.Instant.ofEpochMilli(t.end).atZone(java.time.ZoneId.systemDefault())),
                                color = Theme.c.muted, fontSize = 12.sp)
                        }
                        // ±10 dk, +1 sa, tekrar, renk (2. analiz #133)
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, L("timers.more"), tint = Theme.c.accent) }
                            androidx.compose.material3.DropdownMenu(menu, { menu = false }) {
                                for ((label, d) in listOf("+10 " + L("tools.countdown.minutesShort") to 600_000L, "+1 " + L("tools.countdown.hoursShort") to 3_600_000L,
                                    "−1 " + L("tools.countdown.minutesShort") to -60_000L, "−10 " + L("tools.countdown.minutesShort") to -600_000L)) {
                                    androidx.compose.material3.DropdownMenuItem({ Text(label) }, { menu = false; adjust(t, rem, d) })
                                }
                                androidx.compose.material3.DropdownMenuItem({ Text(L("timers.visual")) }, { menu = false; visualId = t.id })
                                androidx.compose.material3.DropdownMenuItem({ Text((if (t.repeats) "✓ " else "") + L("timers.repeat")) }, {
                                    menu = false; update(items.map { if (it.id == t.id) it.copy(repeats = !it.repeats) else it })
                                })
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (ci in CountdownItem.PALETTE.indices) {
                                        Box(Modifier.size(24.dp).clip(CircleShape).background(if (ci == 0) Theme.c.surface2 else CountdownItem.PALETTE[ci])
                                            .then(if (ci == t.color) Modifier.border(2.dp, Theme.c.ink, CircleShape) else Modifier)
                                            .clickable { menu = false; update(items.map { if (it.id == t.id) it.copy(color = ci) else it }) })
                                    }
                                }
                            }
                        }
                        when {
                            done -> IconButton(onClick = { update(items.map { if (it.id == t.id) start(it, it.total) else it }) }) {
                                Icon(Icons.Filled.Refresh, L("timers.restart"), tint = Theme.c.accent)
                            }
                            t.running -> IconButton(onClick = {
                                AlarmScheduler.cancel(context, t.alarmId)
                                update(items.map { if (it.id == t.id) it.copy(end = 0, left = rem) else it })
                            }) { Icon(Icons.Filled.Pause, L("tools.countdown.pause"), tint = Theme.c.accent) }
                            else -> IconButton(onClick = { update(items.map { if (it.id == t.id) start(it, it.left) else it }) }) {
                                Icon(Icons.Filled.PlayArrow, L("tools.countdown.resume"), tint = Theme.c.accent)
                            }
                        }
                        // +1 dk (rakip analizi #26): sayarken bitişi, duraklatılmışken kalanı uzatır; bittiyse 1 dakikalık yeniden başlar
                        androidx.compose.material3.TextButton(onClick = {
                            update(items.map {
                                if (it.id != t.id) it
                                else if (done) start(it.copy(total = 60_000L), 60_000L)
                                else if (it.running) start(it.copy(total = it.total + 60_000L), rem + 60_000L)
                                else it.copy(total = it.total + 60_000L, left = it.left + 60_000L)
                            })
                        }) { Text(L("tools.countdown.addMinute"), color = Theme.c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                        IconButton(onClick = { AlarmScheduler.cancel(context, t.alarmId); update(items.filter { it.id != t.id }); removed = t }) {
                            Icon(Icons.Filled.Close, L("timers.remove"), tint = Theme.c.muted)
                        }
                    }
                    if (i < items.lastIndex) RowLine()
                }
            }
        }
        removed?.let { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(T("timers.removed", r.name.ifBlank { clock(r.total / 1000) }), Modifier.weight(1f), color = Theme.c.muted, fontSize = 13.sp)
                androidx.compose.material3.TextButton(onClick = {
                    if (r.running && r.end > System.currentTimeMillis()) AlarmScheduler.schedule(context, r.alarmId, r.end, r.doneTitle, AlarmScheduler.TIMER)
                    update(items + r); removed = null
                }) { Text(L("timers.undo"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (history.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SectionTitle(L("timers.history.heading")) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (e in history) {
                    val dur = clock(e.t / 1000)
                    Text(
                        if (e.n.isBlank()) dur else "${e.n} · $dur",
                        Modifier.border(1.dp, Theme.c.accent.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .clickable { add(e.t, e.n) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Theme.c.accent, fontSize = 14.sp,
                    )
                }
            }
        }
        Text(L("alarm.realNote"), color = Theme.c.muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
    }
    if (newSheet) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.ModalBottomSheet({ newSheet = false }, containerColor = Theme.c.bg) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle(L("timers.new"))
                OutlinedTextField(name, { name = it.take(30) }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(L("timers.name")) }, placeholder = { Text(L("timers.namePlaceholder")) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundButton(L("timers.input.wheel"), !keypad) { keypad = false; Prefs.store.edit().putBoolean("timers.keypad", false).apply() }
                    RoundButton(L("timers.input.keypad"), keypad) { keypad = true; Prefs.store.edit().putBoolean("timers.keypad", true).apply() }
                }
                if (keypad) DurationKeypad(digits, { digits = it }) { hh, mm, ss -> h = hh; m = mm; s = ss }
                else Ltr {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NumberWheel(h, 0..23, L("tools.countdown.hoursShort")) { h = it }
                        NumberWheel(m, 0..59, L("tools.countdown.minutesShort")) { m = it }
                        NumberWheel(s, 0..59, L("tools.countdown.secondsShort")) { s = it }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (n in listOf(1, 3, 5, 10, 15, 25, 30, 45, 60)) {
                        Text(
                            T("tools.countdown.presetMin", n),
                            Modifier.border(1.dp, Theme.c.accent.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .clickable { add(n * 60_000L); newSheet = false }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            color = Theme.c.accent, fontSize = 14.sp,
                        )
                    }
                }
                RoundButton(L("tools.countdown.start"), true, Modifier.fillMaxWidth()) { add((h * 3600L + m * 60L + s) * 1000); newSheet = false }
                VoiceToggle()
            }
        }
    }
    items.firstOrNull { it.id == visualId }?.let { t -> VisualTimerDialog(t, now) { visualId = null } }
}

/** Görsel zamanlayıcı (2. analiz #140): rakamsız pasta ya da kum saati; çocuk modunda düğmeler gizli, basılı tutunca açılır */
@Composable
private fun VisualTimerDialog(t: TimerItem, now: Long, onClose: () -> Unit) {
    var style by remember { mutableStateOf(Prefs.store.getString("timers.visual", "pie") ?: "pie") }
    var kids by remember { mutableStateOf(false) }
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !kids, dismissOnClickOutside = false)) {
        val rem = t.remaining(now)
        Column(Modifier.fillMaxSize().background(Color.White).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {
            Text(t.name.ifBlank { clock(t.total / 1000) }, color = Color(0xFF222222), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            VisualTimerContent(if (t.running) rem else t.left, t.total, style)
            if (kids) HoldToUnlock({ kids = false })
            else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (st in listOf("pie", "sand")) {
                        val on = st == style
                        Text(L("timers.style.$st"), Modifier.background(if (on) Color(0xFF222222) else Color(0xFFEEEEEE), RoundedCornerShape(50))
                            .clickable { style = st; Prefs.store.edit().putString("timers.visual", st).apply() }
                            .padding(horizontal = 16.dp, vertical = 8.dp), color = if (on) Color.White else Color(0xFF222222))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(onClick = { kids = true }) { Text("🧒 " + L("timers.kids"), color = Color(0xFF222222)) }
                    androidx.compose.material3.TextButton(onClick = onClose) { Text(L("common.done"), color = Color(0xFF222222)) }
                }
            }
        }
    }
}

@Composable
private fun RowLine() = Box(Modifier.fillMaxWidth().height(1.dp).background(Theme.c.border.copy(alpha = 0.5f)))

/** Süre girişi tuş takımı (2. analiz #134): rakamlar sağdan dolar (SS:DD:ss), ⌫ siler, 00 iki sıfır ekler */
@Composable
fun DurationKeypad(digits: String, onDigits: (String) -> Unit, onChange: (Int, Int, Int) -> Unit) {
    fun parts(d: String): Triple<Int, Int, Int> {
        val p = d.takeLast(6).padStart(6, '0')
        return Triple(p.substring(0, 2).toInt(), p.substring(2, 4).toInt(), p.substring(4, 6).toInt())
    }
    val p = parts(digits)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Ltr { Text("%02d:%02d:%02d".format(java.util.Locale.ROOT, p.first, p.second, p.third),
            color = if (digits.isEmpty()) Theme.c.muted else Theme.c.text, fontSize = 44.sp, fontWeight = FontWeight.Light, style = Tnum) }
        Ltr {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("00", "0", "⌫"))) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (k in row) {
                            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).background(Theme.c.surface2).clickable {
                                val d = if (k == "⌫") digits.dropLast(1) else if (digits.length < 6) (digits + k).trimStart('0').take(6) else digits
                                onDigits(d)
                                val q = parts(d)
                                // Dakika ve saniye 59'u aşarsa taşar (90 sn → 1:30)
                                val total = minOf(23 * 3600 + 59 * 60 + 59, q.first * 3600 + q.second * 60 + q.third)
                                onChange(total / 3600, total % 3600 / 60, total % 60)
                            }.semantics { contentDescription = if (k == "⌫") L("timers.input.delete") else k }, contentAlignment = Alignment.Center) {
                                Text(k, color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Aralık (HIIT) antrenmanı: ayarlar (ısınma, çalışma, dinlenme, tur) ya da sürerken evre, kalan, tur ve ilerleme */
@Composable
fun HiitScreen() {
    val context = LocalContext.current
    val ask = rememberNotifyAsk()
    var cfg by remember { mutableStateOf(HiitConfig.load()) }
    // Başlangıç anı (ms; duraklatılan süre kadar ileri kayar) ve duraklatıldığı an (0 = sürüyor)
    var start by remember { mutableLongStateOf(Prefs.store.getLong("hiit.start", 0L)) }
    var paused by remember { mutableLongStateOf(Prefs.store.getLong("hiit.paused", 0L)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val active = start > 0
    val elapsed = if (!active) 0 else (((if (paused > 0) paused else now) - start) / 1000).toInt()
    val phase = cfg.phase(elapsed)
    // Sürerken ekran açık kalır
    val view = LocalView.current
    DisposableEffect(active && paused == 0L) {
        view.keepScreenOn = active && paused == 0L
        onDispose { view.keepScreenOn = false }
    }

    // Durum kaydedilince hizmet başlar ya da durur (sesli duyurular ve bildirim hizmette, ekran kapalıyken de)
    fun saveState(s: Long, p: Long) {
        start = s; paused = p
        Prefs.store.edit().putLong("hiit.start", s).putLong("hiit.paused", p).apply()
        HiitService.sync(context)
    }
    fun scheduleEnd(from: Long) {
        AlarmScheduler.schedule(context, "hiit", from + cfg.totalSeconds * 1000L, L("hiit.done"), AlarmScheduler.TIMER)
    }

    LaunchedEffect(start, paused) {
        while (start > 0 && paused == 0L) {
            now = System.currentTimeMillis()
            if (cfg.phase(((now - start) / 1000).toInt()).kind == "done") { HealthLog.recordHiitIfDone(context); break }
            delay(200)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!active) {
            if (cfg.isRoutine) AppCard {
                Text(cfg.name, color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                for (st in cfg.steps) Row(Modifier.fillMaxWidth()) {
                    Text(st.n, Modifier.weight(1f), color = if (st.r) Theme.c.muted else Theme.c.text, fontSize = 15.sp)
                    Text(clock(st.s.toLong()), color = Theme.c.muted, fontSize = 15.sp, style = Tnum)
                }
                Stepper(L("hiit.rounds"), cfg.rounds, 1..50, 1, false) { cfg = cfg.copy(rounds = it) }
                Text(T("hiit.total", clock(cfg.totalSeconds.toLong())), color = Theme.c.muted, fontSize = 13.sp)
                TextButton(onClick = { Routines.select(null); cfg = HiitConfig.load() }) { Text(L("routine.classic"), color = Theme.c.accent) }
            } else AppCard {
                Stepper(L("hiit.warmup"), cfg.warmup, 0..600, 5, true) { cfg = cfg.copy(warmup = it) }
                Stepper(L("hiit.work"), cfg.work, 5..1800, 5, true) { cfg = cfg.copy(work = it) }
                Stepper(L("hiit.rest"), cfg.rest, 0..900, 5, true) { cfg = cfg.copy(rest = it) }
                Stepper(L("hiit.rounds"), cfg.rounds, 1..50, 1, false) { cfg = cfg.copy(rounds = it) }
                Text(T("hiit.total", clock(cfg.totalSeconds.toLong())), color = Theme.c.muted, fontSize = 13.sp)
            }
            RoutinePicker { cfg = it; Routines.select(it) }
            RoundButton(L("tools.countdown.start"), true, Modifier.fillMaxWidth()) {
                ask()
                if (cfg.isRoutine) Routines.select(cfg) else Prefs.store.edit().putString("hiit.cfg", cfg.toString()).apply()
                val s = System.currentTimeMillis()
                saveState(s, 0)
                scheduleEnd(s)
            }
        } else if (phase.kind == "done") {
            // Bitiş özeti (2. analiz #135)
            PhasePanel("done", 1f, Modifier.height(220.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🏁", fontSize = 44.sp)
                    Text(L("hiit.done"), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    if (cfg.isRoutine) Text(cfg.name, color = Color.White, fontSize = 16.sp)
                    Text(T("hiit.total", clock(cfg.totalSeconds.toLong())) + " · " + T("hiit.roundCount", cfg.rounds), color = Color.White, fontSize = 15.sp)
                }
            }
            var reminded by remember { mutableStateOf("") }
            if (reminded.isEmpty()) RoundButton(L("hiit.remindTomorrow"), false, Modifier.fillMaxWidth()) {
                val at = java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.systemDefault()).plusDays(1)
                AlarmItem.addNew(context, AlarmItem(hour = at.hour, minute = at.minute, label = (if (cfg.isRoutine) cfg.name else L("hiit.tab")).take(40), date = at.toLocalDate().toString()))
                reminded = T("alarm.quickSet", Fmt.time(at))
            } else Text(reminded, color = Theme.c.muted, fontSize = 14.sp)
            RoundButton(L("tools.countdown.reset"), true, Modifier.fillMaxWidth()) {
                AlarmScheduler.cancel(context, "hiit")
                saveState(0, 0)
            }
        } else {
            // Evre renkli pano: zemin evrenin rengi, alttan evrenin ilerlemesiyle dolar (2. analiz #101)
            val dur = cfg.duration(phase).coerceAtLeast(1)
            val segs = remember(cfg) { cfg.segments() }
            PhasePanel(phase.kind, 1f - phase.left.toFloat() / dur, Modifier.height(300.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(cfg.title(phase), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Ltr { Text(clock(phase.left.toLong()), color = Color.White, fontSize = 84.sp, fontWeight = FontWeight.Light, style = Tnum) }
                    if ((phase.kind == "work" || phase.kind == "rest") && cfg.rounds > 1) Text(T("hiit.round", phase.round, cfg.rounds), color = Color.White, fontSize = 17.sp)
                }
            }
            // Antrenmanın ilerlemesi
            val frac = (elapsed.toFloat() / cfg.totalSeconds).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(8.dp).background(Theme.c.surface2, RoundedCornerShape(4.dp))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(PhaseColors.of(phase.kind), RoundedCornerShape(4.dp)))
            }
            var locked by remember { mutableStateOf(false) }
            if (locked) HoldToUnlock({ locked = false })
            else {
                // Adım atlama: ⏮ evrenin başına (ilk 3 sn içindeyse bir öncekine), ⏭ sıradakine
                fun jump(toSec: Int) {
                    val ref = if (paused > 0) paused else System.currentTimeMillis()
                    val s = ref - toSec * 1000L
                    saveState(s, paused)
                    if (paused == 0L) scheduleEnd(s)
                }
                val cur = segs.indexOfFirst { it.phase.index == phase.index }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (cur >= 0) jump(if (elapsed - segs[cur].start >= 3 || cur == 0) segs[cur].start else segs[cur - 1].start)
                    }) { Icon(Icons.Filled.SkipPrevious, L("hiit.prev"), tint = Theme.c.accent) }
                    if (paused > 0) RoundButton(L("tools.countdown.resume"), true, Modifier.weight(1f)) {
                        val s = start + (System.currentTimeMillis() - paused)
                        saveState(s, 0)
                        scheduleEnd(s)
                    } else RoundButton(L("tools.countdown.pause"), true, Modifier.weight(1f)) {
                        AlarmScheduler.cancel(context, "hiit")
                        saveState(start, System.currentTimeMillis())
                    }
                    IconButton(onClick = { jump(segs.getOrNull(cur + 1)?.start ?: cfg.totalSeconds) }) {
                        Icon(Icons.Filled.SkipNext, L("hiit.skip"), tint = Theme.c.accent)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundButton(L("tools.countdown.reset"), false, Modifier.weight(1f)) {
                        AlarmScheduler.cancel(context, "hiit")
                        saveState(0, 0)
                    }
                    RoundButton("🔒 " + L("tools.lock"), false, Modifier.weight(1f)) { locked = true }
                }
            }
            // Sıradakiler ve bitiş saatleri (2. analiz #135)
            val upcoming = segs.filter { it.phase.index > phase.index }.take(4)
            if (upcoming.isNotEmpty()) AppCard {
                SectionTitle(L("hiit.upcoming"))
                val ref = if (paused > 0) paused else now
                for (sg in upcoming) Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(PhaseColors.of(sg.phase.kind)))
                    Spacer(Modifier.width(8.dp))
                    Text(cfg.title(sg.phase), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp, maxLines = 1)
                    Text(clock(sg.dur.toLong()), color = Theme.c.muted, fontSize = 14.sp, style = Tnum)
                    Spacer(Modifier.width(10.dp))
                    val endAt = java.time.Instant.ofEpochMilli(ref + (sg.start + sg.dur - elapsed) * 1000L).atZone(java.time.ZoneId.systemDefault())
                    Text("🕓 " + Fmt.time(endAt), color = Theme.c.muted, fontSize = 13.sp)
                }
            }
        }
        if (!active) HiitHistoryCard()
        HiitCueOptions()
        VoiceToggle()
        HealthToggle()
        Text(L("hiit.note"), color = Theme.c.muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
    }
}

/** Biten antrenmanlar (2. analiz #135): `hiit.log` [{d: yerel gün, n: ad, s: süre sn, r: tur}], en çok 200; yalnız cihazda */
object HiitLog {
    data class Entry(val d: String, val n: String, val s: Int, val r: Int)
    private const val KEY = "hiit.log"

    fun load(): List<Entry> = runCatching {
        val a = org.json.JSONArray(Prefs.store.getString(KEY, "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Entry(o.getString("d"), o.optString("n"), o.optInt("s"), o.optInt("r", 1)) } }
    }.getOrDefault(emptyList())

    fun add(cfg: HiitConfig, startMs: Long) {
        val d = java.time.Instant.ofEpochMilli(startMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        val list = (listOf(Entry(d, if (cfg.isRoutine) cfg.name else "", cfg.totalSeconds, cfg.rounds)) + load()).take(200)
        Prefs.store.edit().putString(KEY, org.json.JSONArray(list.map {
            org.json.JSONObject().put("d", it.d).put("n", it.n).put("s", it.s).put("r", it.r)
        }).toString()).apply()
    }

    fun clear() = Prefs.store.edit().remove(KEY).apply()

    /** Bu hafta (pazartesiden) biten antrenman sayısı */
    fun thisWeek(today: java.time.LocalDate = java.time.LocalDate.now()): Int {
        val mon = today.minusDays((today.dayOfWeek.value - 1).toLong()).toString()
        return load().count { it.d >= mon }
    }
}

@Composable
private fun HiitHistoryCard() {
    var log by remember { mutableStateOf(HiitLog.load()) }
    if (log.isEmpty()) return
    var confirm by remember { mutableStateOf(false) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(L("hiit.history"), Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = { confirm = true }) { Text(L("hiit.historyClear"), color = Theme.c.accent, fontSize = 13.sp) }
        }
        Text(T("hiit.week", HiitLog.thisWeek()), color = Theme.c.text, fontSize = 15.sp)
        for (e in log.take(6)) Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(Fmt.shortDate(java.time.LocalDate.parse(e.d)), color = Theme.c.muted, fontSize = 13.sp, modifier = Modifier.width(96.dp))
            Text(e.n.ifEmpty { L("hiit.tab") }, Modifier.weight(1f), color = Theme.c.text, fontSize = 14.sp, maxLines = 1)
            Text(clock(e.s.toLong()), color = Theme.c.muted, fontSize = 13.sp, style = Tnum)
        }
    }
    if (confirm) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(L("hiit.historyClear")) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { HiitLog.clear(); log = emptyList(); confirm = false }) { Text(L("hiit.historyClear")) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirm = false }) { Text(L("alarm.cancel")) } },
    )
}

/** Duyuru seçenekleri (2. analiz #135) */
@Composable
private fun HiitCueOptions() {
    AppCard {
        SectionTitle(L("hiit.cues"))
        for ((key, label, def) in listOf(Triple("hiit.cue.next", "hiit.cue.nextLabel", true), Triple("hiit.cue.half", "hiit.cue.halfLabel", false),
            Triple("hiit.cue.ten", "hiit.cue.tenLabel", false))) {
            var on by remember { mutableStateOf(Prefs.store.getBoolean(key, def)) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(L(label), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                Switch(on, { on = it; Prefs.store.edit().putBoolean(key, it).apply() }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
            }
        }
    }
}

/** Rutin seçimi: hazır şablonlar ve kullanıcının rutinleri; yeni rutin oluşturma */
@Composable
internal fun RoutinePicker(onPick: (HiitConfig) -> Unit) {
    var mine by remember { mutableStateOf(Routines.mine()) }
    var editing by remember { mutableStateOf(false) }
    AppCard {
        SectionTitle(L("routine.heading"))
        for ((id, c) in Routines.templates() + mine) Row(Modifier.fillMaxWidth().clickable { onPick(c) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.name, color = Theme.c.text, fontSize = 15.sp)
                Text(T("hiit.total", clock(c.totalSeconds.toLong())), color = Theme.c.muted, fontSize = 12.sp)
            }
            if (mine.any { it.first == id }) TextButton(onClick = { mine = mine.filter { it.first != id }; Routines.save(mine) }) {
                Text("×", color = Theme.c.muted, fontSize = 18.sp)
            }
        }
        TextButton(onClick = { editing = true }) { Text(L("routine.new"), color = Theme.c.accent) }
    }
    if (editing) RoutineEditor(onDismiss = { editing = false }) { c ->
        mine = mine + (java.util.UUID.randomUUID().toString() to c)
        Routines.save(mine)
        editing = false
        onPick(c)
    }
}

/** Yeni rutin: ad, adımlar (ad, süre, dinlenme), tur */
@Composable
private fun RoutineEditor(onDismiss: () -> Unit, onSave: (HiitConfig) -> Unit) {
    var name by remember { mutableStateOf("") }
    var rounds by remember { mutableIntStateOf(1) }
    var steps by remember { mutableStateOf(listOf(HiitConfig.Step(L("hiit.work"), 30), HiitConfig.Step(L("hiit.rest"), 10, true))) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L("routine.new")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(L("routine.name")) })
                for ((i, st) in steps.withIndex()) AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.OutlinedTextField(st.n, { v -> steps = steps.toMutableList().also { it[i] = st.copy(n = v.take(30)) } },
                            Modifier.weight(1f), singleLine = true)
                        TextButton(onClick = { steps = steps.filterIndexed { j, _ -> j != i } }, enabled = steps.size > 1) { Text("×", color = Theme.c.muted) }
                    }
                    Stepper(L("routine.seconds"), st.s, 5..1800, 5, true) { v -> steps = steps.toMutableList().also { it[i] = st.copy(s = v) } }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L("hiit.rest"), Modifier.weight(1f), color = Theme.c.text, fontSize = 14.sp)
                        androidx.compose.material3.Switch(st.r, { v -> steps = steps.toMutableList().also { it[i] = st.copy(r = v) } },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                    }
                }
                TextButton(onClick = { if (steps.size < 30) steps = steps + HiitConfig.Step(L("hiit.work"), 30) }) { Text(L("routine.addStep"), color = Theme.c.accent) }
                Stepper(L("hiit.rounds"), rounds, 1..50, 1, false) { rounds = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(HiitConfig(rounds = rounds, name = name.trim().ifEmpty { L("routine.untitled") }, steps = steps.filter { it.s > 0 })) },
                enabled = steps.isNotEmpty()) { Text(L("alarm.save"), color = Theme.c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
    )
}

/** Değer adımlayıcı: − değer + (saniyede "1:30" gibi) */
@Composable
private fun Stepper(title: String, value: Int, range: IntRange, step: Int, seconds: Boolean, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
        IconButton(onClick = { onChange((value - step).coerceIn(range)) }, enabled = value > range.first) {
            Icon(Icons.Filled.Remove, null, tint = Theme.c.accent)
        }
        Ltr {
            Text(if (seconds) clock(value.toLong()) else value.toString(), Modifier.width(64.dp), color = Theme.c.text,
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, style = Tnum, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        IconButton(onClick = { onChange((value + step).coerceIn(range)) }, enabled = value < range.last) {
            Icon(Icons.Filled.Add, null, tint = Theme.c.accent)
        }
    }
}
