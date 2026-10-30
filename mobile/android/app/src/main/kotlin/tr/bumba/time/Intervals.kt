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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
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
) {
    val running: Boolean get() = end > 0
    fun remaining(now: Long): Long = if (running) maxOf(0, end - now) else left
    val alarmId: String get() = "timer-$id"
    val doneTitle: String get() = if (name.isBlank()) L("tools.countdown.finished") else T("timers.doneNamed", name)

    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name).put("total", total).put("end", end).put("left", left)

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
                    TimerItem(o.getString("id"), o.optString("name"), o.getLong("total"), o.optLong("end"), o.optLong("left"))
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
            return t
        }
    }
}

/** Aralık antrenmanı: ısınma, çalışma ve dinlenme süreleri (sn), tur sayısı */
data class HiitConfig(val warmup: Int = 10, val work: Int = 30, val rest: Int = 15, val rounds: Int = 8) {
    val totalSeconds: Int get() = warmup + rounds * work + (rounds - 1) * rest
    override fun toString() = "$warmup,$work,$rest,$rounds"

    /** Geçen süreye (sn) göre evre: kind (warmup, work, rest, done), tur (1'den), evrenin bitişine kalan (sn) */
    data class Phase(val kind: String, val round: Int, val left: Int, val index: Int)

    fun phase(elapsed: Int): Phase {
        var t = elapsed
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

    /** Söylenecek: evre değişince (ya da ilk bakışta) evrenin adı, evre içinde son 3 saniyede sayı; yoksa null */
    fun cue(last: Phase?, p: Phase): String? = when {
        last == null || p.index != last.index -> word(p)
        p.kind != "done" && p.left in 1..3 && p.left != last.left -> p.left.toString()
        else -> null
    }

    /** Evrenin sesli adı: "Isın", "Çalış" (son turda "Son tur. Çalış"), "Dinlen", "Bitti" */
    fun word(p: Phase): String = when (p.kind) {
        "warmup" -> L("hiit.say.warmup")
        "work" -> if (p.round == rounds && rounds > 1) L("hiit.say.last") + ". " + L("hiit.say.work") else L("hiit.say.work")
        "rest" -> L("hiit.say.rest")
        else -> L("hiit.say.done")
    }

    /** Ekrandaki ad */
    fun title(p: Phase): String = when (p.kind) {
        "warmup" -> L("hiit.warmup"); "work" -> L("hiit.work"); "rest" -> L("hiit.rest"); else -> L("hiit.done")
    }

    companion object {
        fun load(): HiitConfig = runCatching {
            val p = Prefs.store.getString("hiit.cfg", "")!!.split(",").map { it.toInt() }
            HiitConfig(p[0], p[1], p[2], p[3])
        }.getOrDefault(HiitConfig())
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
    // Ekran açıkken biten zamanlayıcı sesli duyurulur (bir kez)
    val announced = remember { HashSet<String>() }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            for (t in items) if (t.running && t.end <= now && announced.add(t.id + t.end)) Speaker.say(t.doneTitle)
            delay(250)
        }
    }

    fun update(list: List<TimerItem>) { items = list; TimerItem.save(list) }
    fun start(t: TimerItem, ms: Long): TimerItem {
        val e = System.currentTimeMillis() + ms
        AlarmScheduler.schedule(context, t.alarmId, e, t.doneTitle, AlarmScheduler.TIMER)
        return t.copy(end = e, left = 0)
    }
    fun add(ms: Long) {
        if (ms <= 0) return
        BadgeStore.hit(Badge.ZAMANLAYICI)
        ask()
        update(items + start(TimerItem(name = name.trim(), total = ms), ms))
        name = ""
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (items.isNotEmpty()) {
            SectionTitle(L("timers.heading"))
            AppCard(padding = 4) {
                items.forEachIndexed { i, t ->
                    val rem = t.remaining(now)
                    val done = t.running && rem <= 0
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name.ifBlank { clock(t.total / 1000) }, color = Theme.c.muted, fontSize = 13.sp, maxLines = 1)
                            Ltr {
                                Text(if (done) L("tools.countdown.finished") else clock((rem + 999) / 1000),
                                    color = if (done) Theme.c.accent else Theme.c.text, fontSize = 32.sp, fontWeight = FontWeight.Light, style = Tnum)
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
                        IconButton(onClick = { AlarmScheduler.cancel(context, t.alarmId); update(items.filter { it.id != t.id }) }) {
                            Icon(Icons.Filled.Close, L("timers.remove"), tint = Theme.c.muted)
                        }
                    }
                    if (i < items.lastIndex) RowLine()
                }
            }
        }
        SectionTitle(L("timers.new"))
        OutlinedTextField(name, { name = it.take(30) }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(L("timers.name")) }, placeholder = { Text(L("timers.namePlaceholder")) })
        Ltr {
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
                        .clickable { add(n * 60_000L) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Theme.c.accent, fontSize = 14.sp,
                )
            }
        }
        RoundButton(L("tools.countdown.start"), true, Modifier.fillMaxWidth()) { add((h * 3600L + m * 60L + s) * 1000) }
        VoiceToggle()
        Text(L("timers.multiNote"), color = Theme.c.muted, fontSize = 13.sp)
        Text(L("alarm.realNote"), color = Theme.c.muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RowLine() = Box(Modifier.fillMaxWidth().height(1.dp).background(Theme.c.border.copy(alpha = 0.5f)))

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
            if (cfg.phase(((now - start) / 1000).toInt()).kind == "done") break
            delay(200)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!active) {
            AppCard {
                Stepper(L("hiit.warmup"), cfg.warmup, 0..600, 5, true) { cfg = cfg.copy(warmup = it) }
                Stepper(L("hiit.work"), cfg.work, 5..1800, 5, true) { cfg = cfg.copy(work = it) }
                Stepper(L("hiit.rest"), cfg.rest, 0..900, 5, true) { cfg = cfg.copy(rest = it) }
                Stepper(L("hiit.rounds"), cfg.rounds, 1..50, 1, false) { cfg = cfg.copy(rounds = it) }
                Text(T("hiit.total", clock(cfg.totalSeconds.toLong())), color = Theme.c.muted, fontSize = 13.sp)
            }
            RoundButton(L("tools.countdown.start"), true, Modifier.fillMaxWidth()) {
                ask()
                Prefs.store.edit().putString("hiit.cfg", cfg.toString()).apply()
                val s = System.currentTimeMillis()
                saveState(s, 0)
                scheduleEnd(s)
            }
        } else {
            val color = when (phase.kind) { "work" -> Theme.c.accent; "rest" -> Color(0xFF2E7D32); else -> Theme.c.muted }
            Spacer(Modifier.height(10.dp))
            Text(cfg.title(phase), color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (phase.kind != "done") BigTime(clock(phase.left.toLong()))
            if (phase.kind == "work" || phase.kind == "rest") Text(T("hiit.round", phase.round, cfg.rounds), color = Theme.c.text, fontSize = 17.sp)
            // Antrenmanın ilerlemesi
            val frac = (elapsed.toFloat() / cfg.totalSeconds).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(10.dp).background(Theme.c.surface2, RoundedCornerShape(5.dp))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(color, RoundedCornerShape(5.dp)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundButton(L("tools.countdown.reset"), false, Modifier.weight(1f)) {
                    AlarmScheduler.cancel(context, "hiit")
                    saveState(0, 0)
                }
                if (phase.kind != "done") {
                    if (paused > 0) RoundButton(L("tools.countdown.resume"), true, Modifier.weight(1f)) {
                        val s = start + (System.currentTimeMillis() - paused)
                        saveState(s, 0)
                        scheduleEnd(s)
                    } else RoundButton(L("tools.countdown.pause"), true, Modifier.weight(1f)) {
                        AlarmScheduler.cancel(context, "hiit")
                        saveState(start, System.currentTimeMillis())
                    }
                }
            }
        }
        VoiceToggle()
        Text(L("hiit.note"), color = Theme.c.muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
    }
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
