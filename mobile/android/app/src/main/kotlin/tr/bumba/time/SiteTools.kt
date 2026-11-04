package tr.bumba.time

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import tr.bumba.time.core.Nature
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/*
 * Sitedeki dokuz araç uygulamada (4 Kasım 2026, sahibin isteği: "satranç saati vardı, şimdi bulamadım"): satranç saati,
 * metronom, konuşan saat, burç hesaplama, saat hesaplama, köprü tatil, emeklilik (yalnız Türkçe), sınav zamanlayıcısı ve
 * sınıf araçları. Hesaplar sitedeki betiklerle aynı (src/js/more-tools.js, src/js/retire.js, lib/bridges.mjs); metinler
 * sitedeki anahtarlarla. iOS'taki SiteTools.swift ile aynı kayıt anahtarları ("exam", "classroom", "metro").
 */

/** Saat hesaplama (sitedeki timecalc): "SS:DD" okuma, iki saat arası, saate süre ekleme, süreleri toplama */
object TimeCalc {
    fun hm(v: String): Int? {
        val m = Regex("""^(\d{1,2}):(\d{2})""").find(v.trim()) ?: return null
        val h = m.groupValues[1].toInt()
        val mi = m.groupValues[2].toInt()
        return if (h > 23 || mi > 59) null else h * 60 + mi
    }

    /** İki saat arası (dakika) ve gece yarısı geçildi mi */
    fun between(a: Int, b: Int): Pair<Int, Boolean> {
        val d = b - a
        return if (d < 0) (d + 1440) to true else d to false
    }

    /** Toplam saniye ve okunamayan satır numaraları (1'den); satır: 45 (dk), 1:30, 2:15:30, başında − ya da + */
    fun sum(text: String): Pair<Long, List<Int>> {
        var total = 0L
        val bad = mutableListOf<Int>()
        text.split(Regex("\r?\n")).forEachIndexed { i, line ->
            var l = line.trim().replace(Regex("\\s+"), "")
            if (l.isEmpty()) return@forEachIndexed
            var sign = 1
            if (l[0] in "-−–") { sign = -1; l = l.substring(1) } else if (l[0] == '+') l = l.substring(1)
            val p = l.split(Regex("[:.]"))
            if (p.size > 3 || p.any { !it.matches(Regex("\\d+")) }) { bad.add(i + 1); return@forEachIndexed }
            val n = p.map { it.toLong() }
            total += sign * when (n.size) { 1 -> n[0] * 60; 2 -> n[0] * 3600 + n[1] * 60; else -> n[0] * 3600 + n[1] * 60 + n[2] }
        }
        return total to bad
    }

    /** Dakikayı günün saati olarak (uygulamanın 24/12 ayarıyla) */
    fun clock(min: Int): String {
        val x = ((min % 1440) + 1440) % 1440
        return LocalTime.of(x / 60, x % 60).format(DateTimeFormatter.ofPattern(if (Prefs.h12) "h:mm a" else "HH:mm", Locale.US))
    }

    fun decimal(x: Double): String = String.format(Strings.numberLocale, "%.2f", x).trimEnd('0').trimEnd('.', ',')
}

/** Burç (sitedeki zodiac): Güneş'in ve Ay'ın tutulum boylamı, tropikal zodyak */
object Zodiac {
    val SYM = listOf("♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓")

    fun sunLon(ms: Double): Double {
        val d = ms / 86400000 + 2440587.5 - 2451545
        val g = Math.toRadians(357.529 + 0.98560028 * d)
        val q = 280.459 + 0.98564736 * d
        return (((q + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360) + 360) % 360
    }

    fun moonLon(ms: Double): Double {
        val p = Nature.moonPosition(ms)
        val d = ms / 86400000 + 2440587.5 - 2451545
        val e = Math.toRadians(23.439 - 0.00000036 * d)
        val a = Math.toRadians(p.rightAscension)
        val dec = Math.toRadians(p.declination)
        return ((Math.toDegrees(atan2(sin(a) * cos(e) + tan(dec) * sin(e), cos(a))) % 360) + 360) % 360
    }

    fun sign(lon: Double): Int = floor(lon / 30).toInt() % 12
    fun label(i: Int): String = SYM[i] + "︎ " + L("zodiac.signs.$i")
}

/**
 * Emeklilik (sitedeki retire.js; yalnız 4/a, Türkiye mevzuatı). Kurallar ve tablolar aynı; tarih aritmetiği JS Date.UTC
 * taşma kuralıyla (yıl başı + ay + gün).
 */
object Retire {
    data class Cond(val kind: String, val n: Int, val at: LocalDate?)
    data class Route(val key: String, val conds: List<Cond>, val at: LocalDate?)
    data class Result(val group: String, val routes: List<Route>, val best: Route?)

    private val B_WOMEN = listOf(intArrayOf(18, 0, 0, 5000), intArrayOf(17, 0, 0, 5000), intArrayOf(16, 0, 0, 5075), intArrayOf(15, 0, 0, 5150),
        intArrayOf(14, 0, 0, 5225), intArrayOf(13, 0, 0, 5300), intArrayOf(12, 0, 0, 5375), intArrayOf(11, 0, 0, 5450), intArrayOf(10, 0, 0, 5525),
        intArrayOf(9, 0, 0, 5600), intArrayOf(8, 0, 0, 5675), intArrayOf(7, 0, 0, 5750), intArrayOf(6, 0, 0, 5825), intArrayOf(5, 0, 0, 5900),
        intArrayOf(4, 0, 0, 5975), intArrayOf(3, 0, 0, 5975), intArrayOf(2, 8, 15, 5975))
    private val B_MEN = listOf(intArrayOf(23, 0, 0, 5000), intArrayOf(21, 6, 0, 5000), intArrayOf(20, 0, 0, 5075), intArrayOf(18, 6, 0, 5150),
        intArrayOf(17, 0, 0, 5225), intArrayOf(15, 6, 0, 5300), intArrayOf(14, 0, 0, 5375), intArrayOf(12, 6, 0, 5450), intArrayOf(11, 0, 0, 5525),
        intArrayOf(9, 6, 0, 5600), intArrayOf(8, 0, 0, 5675), intArrayOf(6, 6, 0, 5750), intArrayOf(5, 0, 0, 5825), intArrayOf(3, 6, 0, 5900),
        intArrayOf(2, 8, 15, 5975))
    private val REF_B = LocalDate.of(2002, 5, 23)
    private val LAW_1999 = LocalDate.of(1999, 9, 8)
    private val LAW_2008 = LocalDate.of(2008, 5, 1)

    /** JS Date.UTC(y + dy, m + dm, d + dd) gibi taşmalı ekleme */
    fun addYMD(t: LocalDate, y: Int, mo: Int = 0, d: Int = 0): LocalDate =
        LocalDate.of(t.year + y, 1, 1).plusMonths((t.monthValue - 1 + mo).toLong()).plusDays((t.dayOfMonth - 1 + d).toLong())

    private fun postAge(t: LocalDate, female: Boolean): Int {
        val tbl = listOf(intArrayOf(2036, 58, 60), intArrayOf(2038, 59, 61), intArrayOf(2040, 60, 62), intArrayOf(2042, 61, 63),
            intArrayOf(2044, 62, 64), intArrayOf(2046, 63, 65), intArrayOf(2048, 64, 65), intArrayOf(99999, 65, 65))
        for (r in tbl) if (t.year < r[0]) return if (female) r[1] else r[2]
        return 65
    }

    fun calc(female: Boolean, birth: LocalDate, start: LocalDate, days: Int, rate: Int, today: LocalDate): Result {
        fun dayDate(need: Int): LocalDate? = when {
            days >= need -> today
            rate <= 0 -> null
            else -> today.plusDays(ceil((need - days) * 365.25 / rate).toLong())
        }
        fun route(key: String, conds: List<Cond>): Route =
            Route(key, conds, if (conds.any { it.at == null }) null else conds.maxOf { it.at!! })
        fun cDays(n: Int) = Cond("days", n, dayDate(n))
        fun cYears(n: Int) = Cond("years", n, addYMD(start, n))
        fun cAge(n: Int) = Cond("age", n, addYMD(birth, n))
        val routes = mutableListOf<Route>()
        val group: String
        if (start < LAW_1999) {
            val old = start <= addYMD(LAW_1999, if (female) -18 else -23)
            var need = 5975
            if (old) need = 5000
            else for (r in if (female) B_WOMEN else B_MEN) if (start <= addYMD(REF_B, -r[0], -r[1], -r[2])) { need = r[3]; break }
            group = if (old) "a" else "b"
            routes.add(route("full", listOf(cDays(need), cYears(if (female) 20 else 25), Cond("noAge", 0, start))))
            routes.add(route("partialPre", listOf(cDays(3600), cYears(15), cAge(if (old) (if (female) 50 else 55) else if (female) 58 else 60))))
        } else if (start < LAW_2008) {
            group = "mid"
            routes.add(route("mid1", listOf(cDays(7000), cAge(if (female) 58 else 60))))
            routes.add(route("mid2", listOf(cDays(4500), cYears(25), cAge(if (female) 58 else 60))))
        } else {
            group = "post"
            val d72 = dayDate(7200)
            val d54 = dayDate(5400)
            val a1 = d72?.let { postAge(it, female) } ?: if (female) 58 else 60
            val a2 = minOf(65, (d54?.let { postAge(it, female) } ?: if (female) 58 else 60) + 3)
            routes.add(route("full", listOf(cDays(7200), cAge(a1))))
            routes.add(route("partialPost", listOf(cDays(5400), cAge(a2))))
        }
        val best = routes.filter { it.at != null }.minByOrNull { it.at!! }
        return Result(group, routes, best)
    }

    /** "x yıl y ay" (sitedeki diffText) */
    fun diffText(a: LocalDate, b: LocalDate): String {
        var m = (b.year - a.year) * 12 + (b.monthValue - a.monthValue)
        if (b.dayOfMonth < a.dayOfMonth) m--
        val parts = mutableListOf<String>()
        if (m >= 12) parts.add(T("retire.years", m / 12))
        if (m % 12 != 0) parts.add(T("retire.months", m % 12))
        return parts.joinToString(" ").ifEmpty { T("retire.months", 0) }
    }

    fun ageAt(birth: LocalDate, at: LocalDate): Int {
        var age = at.year - birth.year
        if (addYMD(birth, age) > at) age--
        return age
    }
}

/** Sınıf araçlarının adları (sitedeki classroom): satır, virgül ya da noktalı virgülle; tekrarsız, en çok 300 */
object Classroom {
    const val KEY = "classroom"
    fun names(text: String): List<String> =
        text.split(Regex("[\\n,،、，;]+")).map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(300)
}

/** Köprü tatil verisi (gen-strings; sitedeki /kopru-tatil/ ile aynı hesap) */
object Bridges {
    data class Item(val groups: List<String>, val from: LocalDate, val to: LocalDate, val days: Int, val leave: Double,
                    val leaveDays: List<LocalDate>, val halfDays: List<LocalDate>, val best: Boolean)
    data class Year(val projected: Boolean, val list: List<Item>)

    val years: Map<Int, Year> by lazy {
        runCatching {
            val o = JSONObject(Strings.appContext.assets.open("bridges.json").bufferedReader().readText())
            o.keys().asSequence().associate { y ->
                val v = o.getJSONObject(y)
                val a = v.getJSONArray("list")
                y.toInt() to Year(v.optBoolean("projected"), (0 until a.length()).map { i ->
                    val b = a.getJSONObject(i)
                    fun dates(k: String) = b.getJSONArray(k).let { d -> (0 until d.length()).map { LocalDate.parse(d.getString(it)) } }
                    Item((0 until b.getJSONArray("g").length()).map { b.getJSONArray("g").getString(it) }, LocalDate.parse(b.getString("from")),
                        LocalDate.parse(b.getString("to")), b.getInt("days"), b.getDouble("leave"), dates("leaveDays"), dates("halfDays"), b.optBoolean("best"))
                })
            }
        }.getOrDefault(emptyMap())
    }

    fun groupName(g: String): String = if (g == "rb" || g == "kb") L("bridge.groups.$g") else L("calendar.events.$g")
}

/** Kısa bip sesleri (satranç, sınav, sınıf): AudioTrack ile üretilir, dosya yok */
private object Beep {
    fun play(freq: Double, ms: Int, vol: Float = 0.35f, repeat: Int = 1, gapMs: Int = 120) {
        Thread {
            runCatching {
                val rate = 44100
                val one = rate * ms / 1000
                val gap = rate * gapMs / 1000
                val buf = ShortArray((one + gap) * repeat)
                for (r in 0 until repeat) for (i in 0 until one) {
                    val env = minOf(1.0, i / 200.0) * minOf(1.0, (one - i) / 400.0)
                    buf[r * (one + gap) + i] = (sin(2 * Math.PI * freq * i / rate) * env * vol * Short.MAX_VALUE).toInt().toShort()
                }
                val t = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(buf.size * 2).build()
                t.write(buf, 0, buf.size)
                t.play()
                Thread.sleep((buf.size * 1000L / rate) + 100)
                t.release()
            }
        }.start()
    }

    fun chime() { play(880.0, 250); Handler(Looper.getMainLooper()).postDelayed({ play(1318.5, 400, 0.3f) }, 180) }
}

@Composable
private fun KeepOn(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun Pill(text: String, on: Boolean, onClick: () -> Unit) {
    Text(text, Modifier.clip(RoundedCornerShape(50)).background(if (on) Theme.c.accent else Theme.c.surface2).clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp), color = if (on) Theme.c.bg else Theme.c.text, fontSize = 14.sp)
}

@Composable
private fun Note(text: String) = Text(text, color = Theme.c.muted, fontSize = 12.sp)

/** Saati seçen düğme (TimePicker) */
@Composable
private fun TimeButton(label: String, minutes: Int?, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    val context = LocalContext.current
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Theme.c.surface2).clickable {
        val v = minutes ?: (LocalTime.now().hour * 60)
        android.app.TimePickerDialog(context, { _, h, m -> onPick(h * 60 + m) }, v / 60, v % 60, !Prefs.h12).show()
    }.padding(12.dp)) {
        Text(label, color = Theme.c.muted, fontSize = 12.sp)
        Text(minutes?.let { TimeCalc.clock(it) } ?: "–", color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Tarihi seçen düğme (DatePicker) */
@Composable
private fun DateButton(label: String, date: LocalDate?, modifier: Modifier = Modifier, fallback: LocalDate = LocalDate.of(1990, 1, 1), onPick: (LocalDate) -> Unit) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Theme.c.surface2).clickable {
        val d = date ?: fallback
        android.app.DatePickerDialog(context, { _, y, m, dd -> onPick(LocalDate.of(y, m + 1, dd)) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
    }.padding(12.dp)) {
        Text(label, color = Theme.c.muted, fontSize = 12.sp)
        Text(date?.let { Fmt.longDate(it) } ?: "–", color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

/* ---------- Satranç saati ---------- */

@Composable
fun ChessClockScreen() {
    val presets = listOf(1 to 0, 3 to 0, 3 to 2, 5 to 0, 5 to 3, 10 to 0, 15 to 10, 30 to 0)
    var mins by remember { mutableIntStateOf(5) }
    var inc by remember { mutableIntStateOf(0) }
    var custom by remember { mutableStateOf(false) }
    val time = remember { mutableStateListOf(5 * 60_000L, 5 * 60_000L) } // 0 beyaz, 1 siyah
    val moves = remember { mutableStateListOf(0, 0) }
    var turn by remember { mutableIntStateOf(-1) }
    var running by remember { mutableStateOf(false) }
    var flag by remember { mutableIntStateOf(-1) }
    var last by remember { mutableLongStateOf(0L) }
    fun setup() {
        time[0] = mins * 60_000L; time[1] = mins * 60_000L; moves[0] = 0; moves[1] = 0
        turn = -1; running = false; flag = -1
    }
    fun step() {
        if (!running || turn < 0) return
        val now = System.nanoTime() / 1_000_000
        time[turn] -= now - last
        last = now
        if (time[turn] <= 0) { time[turn] = 0; flag = turn; running = false; Beep.play(988.0, 250, repeat = 3, gapMs = 100) }
    }
    fun press(side: Int) {
        if (flag >= 0) return
        if (turn < 0) { turn = 1 - side; running = true; last = System.nanoTime() / 1_000_000; Beep.play(1200.0, 50, 0.2f); return }
        if (!running || turn != side) return
        step(); if (flag >= 0) return
        time[side] += inc * 1000L; moves[side]++; turn = 1 - side; last = System.nanoTime() / 1_000_000
        Beep.play(1200.0, 50, 0.2f)
    }
    LaunchedEffect(running) { while (running) { step(); delay(50) } }
    KeepOn(running)
    fun fmt(ms: Long): String {
        val v = maxOf(0L, ms)
        if (v < 10_000) return String.format(Locale.US, "%.1f", floor(v / 100.0) / 10)
        val s = ceil(v / 1000.0).toLong()
        val h = s / 3600
        val m = (s % 3600) / 60
        return (if (h > 0) "$h:%02d".format(Locale.US, m) else "%02d".format(Locale.US, m)) + ":%02d".format(Locale.US, s % 60)
    }
    @Composable
    fun Side(side: Int, modifier: Modifier) {
        val active = turn == side && flag < 0
        val bg = when { flag == side -> Color(0xFFC92A2A); active -> Theme.c.accent; else -> Theme.c.surface2 }
        val fg = if (active || flag == side) Theme.c.bg else Theme.c.text
        Box(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(bg).clickable { press(side) }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(L(if (side == 0) "chess.white" else "chess.black"), color = fg, fontSize = 15.sp)
                Text(fmt(time[side]), color = fg, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                Text(if (moves[side] > 0) T("chess.moves", moves[side]) else " ", color = fg.copy(alpha = 0.8f), fontSize = 13.sp)
                if (flag == side) Text(T("chess.flag", L(if (side == 0) "chess.white" else "chess.black")), color = fg, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Karşıdaki oyuncu için siyahın saati ters döner
        Side(1, Modifier.weight(1f).rotate(180f))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            for ((m, i) in presets) Pill("$m + $i", !custom && mins == m && inc == i) { custom = false; mins = m; inc = i; setup() }
            Pill(L("chess.custom"), custom) { custom = true }
        }
        if (custom) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(mins.toString(), { v -> v.toIntOrNull()?.let { mins = it.coerceIn(1, 180); setup() } }, Modifier.weight(1f),
                label = { Text(L("chess.minutes")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(inc.toString(), { v -> v.toIntOrNull()?.let { inc = it.coerceIn(0, 60) } }, Modifier.weight(1f),
                label = { Text(L("chess.increment")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(L(if (running) "chess.pause" else "chess.resume"), false, Modifier.weight(1f)) {
                if (turn < 0 || flag >= 0) return@RoundButton
                if (running) { step(); running = false } else { last = System.nanoTime() / 1_000_000; running = true }
            }
            RoundButton(L("chess.reset"), false, Modifier.weight(1f)) { setup() }
        }
        Side(0, Modifier.weight(1f))
        if (turn < 0) Note(L("chess.appHint"))
    }
}

/* ---------- Metronom ---------- */

/** Metronom sesi: ayrı iş parçacığında AudioTrack akışına örnek hassasiyetinde tık yazılır (sitedeki Web Audio planlayıcısı gibi) */
private object MetronomeEngine {
    @Volatile var bpm = 100
    @Volatile var beats = 4
    @Volatile var sub = 1
    @Volatile var accent = true
    @Volatile var vol = 70
    @Volatile private var running = false
    private var thread: Thread? = null
    var onBeat: ((Int) -> Unit)? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            val rate = 44100
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(rate / 10 * 2).build()
            track.play()
            val handler = Handler(Looper.getMainLooper())
            val chunk = ShortArray(rate / 50)
            var pos = 0L
            var next = (rate * 0.05).toLong()
            var idx = 0
            var clickLeft = 0
            var freq = 0.0
            var amp = 0.0
            var phase = 0.0
            while (running) {
                for (i in chunk.indices) {
                    if (pos == next) {
                        val main = idx % sub == 0
                        val beat = (idx / sub) % beats
                        val acc = main && beat == 0 && accent && beats > 1
                        val v = vol / 100.0 * 0.6
                        freq = if (!main) 784.0 else if (acc) 1568.0 else 1046.5
                        amp = if (!main) v * 0.45 else if (acc) v else v * 0.8
                        clickLeft = rate * (if (main) 50 else 30) / 1000
                        phase = 0.0
                        if (main) handler.post { onBeat?.invoke(beat) }
                        next += (rate * 60.0 / bpm / sub).toLong()
                        idx++
                    }
                    chunk[i] = if (clickLeft > 0) {
                        clickLeft--
                        phase += freq / rate
                        // Kare dalga, sönümlü
                        ((if (phase % 1 < 0.5) 1 else -1) * amp * minOf(1.0, clickLeft / 300.0) * Short.MAX_VALUE * 0.5).toInt().toShort()
                    } else 0
                    pos++
                }
                track.write(chunk, 0, chunk.size)
            }
            track.stop(); track.release()
        }.also { it.start() }
    }

    fun stop() { running = false; thread = null; handlerReset() }
    private fun handlerReset() { onBeat?.invoke(-1) }
}

@Composable
fun MetronomeScreen() {
    val st = remember { runCatching { JSONObject(Prefs.store.getString("metro", "{}")!!) }.getOrDefault(JSONObject()) }
    var bpm by remember { mutableIntStateOf(st.optInt("bpm", 100)) }
    var beats by remember { mutableIntStateOf(st.optInt("beats", 4)) }
    var sub by remember { mutableIntStateOf(st.optInt("sub", 1)) }
    var accent by remember { mutableStateOf(st.optBoolean("accent", true)) }
    var vol by remember { mutableIntStateOf(st.optInt("vol", 70)) }
    var playing by remember { mutableStateOf(false) }
    var lit by remember { mutableIntStateOf(-1) }
    val taps = remember { mutableListOf<Long>() }
    fun persist() {
        Prefs.store.edit().putString("metro", JSONObject().put("bpm", bpm).put("beats", beats).put("sub", sub).put("accent", accent).put("vol", vol).toString()).apply()
        MetronomeEngine.bpm = bpm; MetronomeEngine.beats = beats; MetronomeEngine.sub = sub; MetronomeEngine.accent = accent; MetronomeEngine.vol = vol
    }
    fun setBpm(v: Int) { bpm = v.coerceIn(30, 250); persist() }
    DisposableEffect(Unit) {
        persist()
        MetronomeEngine.onBeat = { lit = it }
        onDispose { MetronomeEngine.stop(); MetronomeEngine.onBeat = null }
    }
    KeepOn(playing)
    val names = listOf(40 to "Grave", 60 to "Largo", 66 to "Larghetto", 76 to "Adagio", 108 to "Andante", 120 to "Moderato", 156 to "Allegro",
        176 to "Vivace", 200 to "Presto", 999 to "Prestissimo")
    ToolFrame(L(if (playing) "metronome.stop" else "metronome.start"), {
        playing = !playing
        if (playing) MetronomeEngine.start() else MetronomeEngine.stop()
    }, center = true) {
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until beats) Box(Modifier.size(if (beats > 8) 18.dp else 24.dp).clip(CircleShape)
                .background(if (lit == i) Theme.c.accent else if (i == 0 && accent) Theme.c.accent.copy(alpha = 0.35f) else Theme.c.surface2))
        }
        Text("$bpm", color = Theme.c.text, fontSize = 72.sp, fontWeight = FontWeight.Bold)
        Text(names.first { bpm < it.first }.second, color = Theme.c.muted, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (d in listOf(-5, -1, 1, 5)) Text(if (d < 0) "−${-d}" else "+$d",
                Modifier.size(56.dp).clip(CircleShape).background(Theme.c.surface2).clickable { setBpm(bpm + d) }.padding(top = 16.dp),
                color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        Slider(bpm.toFloat(), { setBpm(it.toInt()) }, valueRange = 30f..250f,
            colors = SliderDefaults.colors(thumbColor = Theme.c.accent, activeTrackColor = Theme.c.accent, inactiveTrackColor = Theme.c.surface2))
        RoundButton(L("metronome.tap"), false, Modifier.fillMaxWidth()) {
            val now = System.currentTimeMillis()
            if (taps.isNotEmpty() && now - taps.last() > 2000) taps.clear()
            taps.add(now); if (taps.size > 6) taps.removeAt(0)
            if (taps.size >= 2) setBpm(Math.round(60000.0 / ((taps.last() - taps.first()).toDouble() / (taps.size - 1))).toInt())
        }
        AppCard {
            SectionTitle(L("metronome.beats"))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (n in 1..12) Pill("$n", beats == n) { beats = n; persist() }
            }
            SectionTitle(L("metronome.sub"))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (k in 1..4) Pill(L("metronome.subs.$k"), sub == k) { sub = k; persist() }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("metronome.accent"), Modifier.weight(1f), color = Theme.c.text)
                SoftSwitch(accent, { accent = it; persist() }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
            }
            SectionTitle(L("metronome.volume"))
            Slider(vol.toFloat(), { vol = it.toInt(); persist() }, valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = Theme.c.accent, activeTrackColor = Theme.c.accent, inactiveTrackColor = Theme.c.surface2))
        }
        Note(L("metronome.tempoNote"))
    }
}

/* ---------- Konuşan saat ---------- */

@Composable
fun TalkingClockScreen() {
    val context = LocalContext.current
    var every by remember { mutableStateOf(Prefs.store.getString("talking.every", "off")!!) }
    var chime by remember { mutableStateOf(Prefs.store.getBoolean("talking.chime", true)) }
    var ready by remember { mutableStateOf(false) }
    val tts = remember {
        arrayOfNulls<TextToSpeech>(1)
    }
    DisposableEffect(Unit) {
        tts[0] = TextToSpeech(context.applicationContext) { ok -> ready = ok == TextToSpeech.SUCCESS; if (ready) tts[0]?.language = Strings.locale }
        onDispose { tts[0]?.shutdown(); tts[0] = null }
    }
    fun sayNow() {
        val now = LocalTime.now()
        val text = T("alarm.speakText", TimeCalc.clock(now.hour * 60 + now.minute))
        if (chime) {
            Beep.chime()
            Handler(Looper.getMainLooper()).postDelayed({ tts[0]?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "talk") }, 700)
        } else tts[0]?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "talk")
    }
    val now = rememberNow().atZone(ZoneId.systemDefault())
    var lastKey by remember { mutableStateOf("") }
    LaunchedEffect(now.minute, every) {
        if (every == "off" || !ready) return@LaunchedEffect
        val key = "${now.hour}:${now.minute}"
        if (key == lastKey || now.second > 2) return@LaunchedEffect
        val n = every.toIntOrNull() ?: return@LaunchedEffect
        if (if (n == 60) now.minute == 0 else now.minute % n == 0) { lastKey = key; sayNow() }
    }
    KeepOn(every != "off")
    ToolFrame(L("talking.say"), { sayNow() }, center = true) {
        Text(Fmt.time(now, seconds = true), color = Theme.c.text, fontSize = 64.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
        Text(Fmt.longDate(now.toLocalDate()), color = Theme.c.muted, fontSize = 15.sp)
        AppCard {
            SectionTitle(L("talking.auto"))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (k in listOf("off", "1", "5", "15", "30", "60")) Pill(L("talking.every.$k"), every == k) {
                    every = k; Prefs.store.edit().putString("talking.every", k).apply()
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("talking.chime"), Modifier.weight(1f), color = Theme.c.text)
                SoftSwitch(chime, { chime = it; Prefs.store.edit().putBoolean("talking.chime", it).apply() }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
            }
        }
        Note(L("talking.appNote"))
    }
}

/* ---------- Burç hesaplama ---------- */

@Composable
fun ZodiacScreen() {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var time by remember { mutableStateOf<Int?>(null) }
    val zone = ZoneId.systemDefault()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DateButton(L("zodiac.dateLabel"), date) { date = it }
        TimeButton(L("zodiac.timeLabel"), time, Modifier.fillMaxWidth()) { time = it }
        val d = date
        if (d != null) {
            val tm = time ?: 720
            val at = d.atTime(tm / 60, tm % 60).atZone(zone).toInstant().toEpochMilli().toDouble()
            val s0 = Zodiac.sign(Zodiac.sunLon(d.atStartOfDay(zone).toInstant().toEpochMilli().toDouble()))
            val s1 = Zodiac.sign(Zodiac.sunLon(d.atTime(23, 59).atZone(zone).toInstant().toEpochMilli().toDouble()))
            AppCard {
                Row2(L("zodiac.sun"), Zodiac.label(Zodiac.sign(Zodiac.sunLon(at))), bold = true)
                Row2(L("zodiac.moon"), Zodiac.label(Zodiac.sign(Zodiac.moonLon(at))), bold = true)
                if (s0 != s1) Note(L("zodiac.cusp"))
                if (time == null) Note(L("zodiac.noTime"))
                Note(L("zodiac.tzNote"))
            }
        }
        AppCard {
            SectionTitle(L("zodiac.tableHeading"))
            // Güneş'in burca girdiği yaklaşık günler (sitedeki tablo; derleme yılından bağımsız ortalama)
            val starts = listOf("21.03", "20.04", "21.05", "21.06", "23.07", "23.08", "23.09", "23.10", "22.11", "22.12", "20.01", "19.02")
            for (i in 0 until 12) {
                val (dd, mm) = starts[i].split(".").map { it.toInt() }
                val (ed, em) = starts[(i + 1) % 12].split(".").map { it.toInt() }
                val end = LocalDate.of(2025, em, ed).minusDays(1)
                Row2(Zodiac.label(i), "${Fmt.dayMonth(LocalDate.of(2025, mm, dd))} – ${Fmt.dayMonth(end)}")
            }
        }
        Note(L("zodiac.fun"))
    }
}

/* ---------- Saat hesaplama ---------- */

@Composable
fun TimeCalcScreen() {
    var s by remember { mutableStateOf<Int?>(9 * 60) }
    var e by remember { mutableStateOf<Int?>(17 * 60 + 30) }
    var t by remember { mutableStateOf<Int?>(8 * 60) }
    var sign by remember { mutableIntStateOf(1) }
    var h by remember { mutableStateOf("2") }
    var m by remember { mutableStateOf("30") }
    var sum by remember { mutableStateOf("1:30\n45\n2:15:30") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard {
            SectionTitle(L("timeCalc.betweenHeading"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeButton(L("timeCalc.startLabel"), s, Modifier.weight(1f)) { s = it }
                TimeButton(L("timeCalc.endLabel"), e, Modifier.weight(1f)) { e = it }
            }
            val a = s; val b = e
            if (a != null && b != null) {
                val (d, over) = TimeCalc.between(a, b)
                Text(T("timeCalc.duration", d / 60, d % 60) + " · " + T("timeCalc.decimal", TimeCalc.decimal(d / 60.0)),
                    color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Note(if (over) L("timeCalc.overnight") else T("timeCalc.minutesTotal", d))
            }
        }
        AppCard {
            SectionTitle(L("timeCalc.addHeading"))
            TimeButton(L("timeCalc.timeLabel"), t, Modifier.fillMaxWidth()) { t = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(L("timeCalc.opAdd"), sign == 1) { sign = 1 }
                Pill(L("timeCalc.opSub"), sign == -1) { sign = -1 }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(h, { h = it.filter(Char::isDigit).take(4) }, Modifier.weight(1f), label = { Text(L("timeCalc.hoursLabel")) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(m, { m = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), label = { Text(L("timeCalc.minutesLabel")) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            t?.let { base ->
                val tot = base + ((h.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0)) * sign
                val days = floor(tot / 1440.0).toInt()
                val extra = when { days == 1 -> L("timeCalc.nextDay"); days == -1 -> L("timeCalc.prevDay")
                    days > 1 -> T("timeCalc.daysLater", days); days < -1 -> T("timeCalc.daysEarlier", -days); else -> "" }
                Text(T("timeCalc.resultTime", TimeCalc.clock(tot)) + if (extra.isNotEmpty()) " ($extra)" else "",
                    color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        AppCard {
            SectionTitle(L("timeCalc.sumHeading"))
            OutlinedTextField(sum, { sum = it.take(2000) }, Modifier.fillMaxWidth().height(150.dp), label = { Text(L("timeCalc.sumLabel")) })
            Note(L("timeCalc.sumHint"))
            val (total, bad) = TimeCalc.sum(sum)
            val a = kotlin.math.abs(total)
            val neg = if (total < 0) "−" else ""
            val txt = neg + "${a / 3600}:%02d".format(Locale.US, (a % 3600) / 60) + if (a % 60 != 0L) ":%02d".format(Locale.US, a % 60) else ""
            Text(T("timeCalc.sumResult", txt) + " · " + T("timeCalc.decimal", neg + TimeCalc.decimal(a / 3600.0)),
                color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (bad.isNotEmpty()) Note(T("timeCalc.sumBad", bad.joinToString(", ")))
        }
    }
}

/* ---------- Köprü tatil ---------- */

@Composable
fun BridgeScreen() {
    val data = Bridges.years
    val thisYear = LocalDate.now().year
    var year by remember { mutableIntStateOf(if (thisYear in data) thisYear else data.keys.minOrNull() ?: thisYear) }
    var max by remember { mutableIntStateOf(9) }
    val num = { x: Double -> if (x % 1.0 == 0.0) x.toInt().toString() else String.format(Strings.numberLocale, "%.1f", x) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(L("bridge.yearLabel"))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (y in data.keys.sorted()) Pill("$y", y == year) { year = y }
        }
        SectionTitle(L("bridge.maxLabel"))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(L("bridge.maxAll"), max == 9) { max = 9 }
            for (n in 1..4) Pill(T("bridge.maxN", n), max == n) { max = n }
        }
        val yd = data[year]
        if (yd?.projected == true) Note(L("bridge.projected"))
        for (b in yd?.list.orEmpty().filter { it.leave <= max }) {
            AppCard(background = if (b.best) Theme.c.accent.copy(alpha = 0.12f) else Theme.c.surface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.groups.joinToString(" + ") { Bridges.groupName(it) }, Modifier.weight(1f), color = Theme.c.text, fontWeight = FontWeight.SemiBold)
                    if (b.best) Text(L("bridge.best"), Modifier.clip(RoundedCornerShape(50)).background(Theme.c.accent).padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Theme.c.bg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("${Fmt.shortDate(b.from)} – ${Fmt.shortDate(b.to)}", color = Theme.c.muted, fontSize = 13.sp)
                Row {
                    Text(T("bridge.totalN", b.days), Modifier.weight(1f), color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(if (b.leave > 0) T("bridge.leaveN", num(b.leave)) else L("bridge.noLeave"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                }
                val days = (b.leaveDays.map { it to Fmt.shortDate(it) } + b.halfDays.map { it to T("bridge.halfNote", Fmt.shortDate(it)) })
                    .sortedBy { it.first }.map { it.second }
                Text(if (days.isEmpty()) L("bridge.none") else days.joinToString(", "), color = Theme.c.muted, fontSize = 13.sp)
            }
        }
        Note(L("bridge.note"))
    }
}

/* ---------- Emeklilik (yalnız Türkçe) ---------- */

@Composable
fun RetireScreen() {
    var female by remember { mutableStateOf(true) }
    var birth by remember { mutableStateOf<LocalDate?>(null) }
    var start by remember { mutableStateOf<LocalDate?>(null) }
    var days by remember { mutableStateOf("") }
    var rate by remember { mutableIntStateOf(360) }
    var result by remember { mutableStateOf<Retire.Result?>(null) }
    val today = LocalDate.now(ZoneId.of("Europe/Istanbul"))
    val tr = Locale.forLanguageTag("tr-TR")
    fun condLabel(c: Retire.Cond) = when (c.kind) {
        "days" -> T("retire.condDays", String.format(tr, "%,d", c.n)); "years" -> T("retire.condYears", c.n)
        "age" -> T("retire.condAge", c.n); else -> L("retire.condNoAge")
    }
    ToolFrame(L("retire.calc"), {
        val b = birth; val s = start; val d = days.toIntOrNull()
        if (b != null && s != null && d != null && d >= 0 && s >= b) result = Retire.calc(female, b, s, d, rate, today)
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(L("retire.gender"), color = Theme.c.muted, fontSize = 13.sp)
            Pill(L("retire.women"), female) { female = true; result = null }
            Pill(L("retire.men"), !female) { female = false; result = null }
        }
        DateButton(L("retire.birth"), birth, fallback = LocalDate.of(1980, 1, 1)) { birth = it; result = null }
        DateButton(L("retire.start"), start, fallback = LocalDate.of(2005, 1, 1)) { start = it; result = null }
        OutlinedTextField(days, { days = it.filter(Char::isDigit).take(5); result = null }, Modifier.fillMaxWidth(), label = { Text(L("retire.days")) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Note(L("retire.daysHint"))
        SectionTitle(L("retire.rate"))
        for ((v, k) in listOf(360 to "retire.rateFull", 180 to "retire.rateHalf", 0 to "retire.rateNone")) Pill(L(k), rate == v) { rate = v; result = null }
        result?.let { r ->
            AppCard(background = Theme.c.accent.copy(alpha = 0.10f)) {
                val best = r.best
                if (best?.at == null) Text(L("retire.resultNever"), color = Theme.c.text, fontWeight = FontWeight.SemiBold)
                else {
                    Text(T("retire.result", Fmt.longDate(best.at)), color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(T("retire.resultAge", Retire.ageAt(birth!!, best.at)) + " " +
                        if (best.at <= today) L("retire.resultPast") else T("retire.resultLeft", Retire.diffText(today, best.at)), color = Theme.c.text)
                }
                Note(T("retire.group", L("retire.groups.${r.group}")))
            }
            for (rt in r.routes) AppCard(background = if (rt == r.best) Theme.c.accent.copy(alpha = 0.06f) else Theme.c.surface) {
                Text(L("retire.routes.${rt.key}"), color = Theme.c.text, fontWeight = FontWeight.SemiBold)
                for (c in rt.conds) Row2(condLabel(c), when { c.at == null -> L("retire.never"); c.at <= today -> L("retire.done"); else -> Fmt.shortDate(c.at) })
                Row2("=", rt.at?.let { Fmt.shortDate(maxOf(it, today)) } ?: L("retire.never"), bold = true)
            }
        }
        Note(L("retire.noteScope"))
        Note(L("retire.noteLaw"))
        Note(L("tools.localOnly"))
    }
}

/* ---------- Sınav zamanlayıcısı ---------- */

@Composable
fun ExamTimerScreen() {
    val st = remember { runCatching { JSONObject(Prefs.store.getString("exam", "{}")!!) }.getOrDefault(JSONObject()) }
    var name by remember { mutableStateOf(st.optString("name")) }
    var room by remember { mutableStateOf(st.optString("room")) }
    var read by remember { mutableStateOf(st.optInt("read", 10).toString()) }
    var dur by remember { mutableStateOf(st.optInt("dur", 40).toString()) }
    var start by remember { mutableLongStateOf(st.optLong("start")) }
    var pauseAt by remember { mutableLongStateOf(st.optLong("pauseAt")) }
    var lastPhase by remember { mutableStateOf("") }
    fun save() {
        Prefs.store.edit().putString("exam", JSONObject().put("name", name).put("room", room).put("read", read.toIntOrNull() ?: 0)
            .put("dur", dur.toIntOrNull() ?: 1).put("start", start).put("pauseAt", pauseAt).toString()).apply()
    }
    val nowI = rememberNow()
    val now = nowI.toEpochMilli()
    val r = (read.toIntOrNull() ?: 0).coerceIn(0, 60) * 60_000L
    val d = (dur.toIntOrNull() ?: 1).coerceIn(1, 600) * 60_000L
    val e = if (start == 0L) 0L else (if (pauseAt > 0) pauseAt else now) - start
    val (ph, rem, frac) = when {
        start == 0L -> Triple("", r + d, 0f)
        e < r -> Triple("read", r - e, e.toFloat() / r)
        e < r + d -> Triple("exam", r + d - e, (e - r).toFloat() / d)
        else -> Triple("done", 0L, 1f)
    }
    LaunchedEffect(ph) { if (start > 0 && lastPhase.isNotEmpty() && ph != lastPhase && ph.isNotEmpty()) Beep.chime(); lastPhase = ph }
    val warn = if (ph == "exam" && rem <= 5 * 60_000) 5 else if (ph == "exam" && rem <= 15 * 60_000) 15 else 0
    val running = start > 0 && pauseAt == 0L && ph != "done"
    KeepOn(running)
    fun mmss(ms: Long): String {
        val s = maxOf(0L, ceil(ms / 1000.0).toLong())
        val h = s / 3600
        val m = (s % 3600) / 60
        return (if (h > 0) "$h:%02d".format(Locale.US, m) else "%02d".format(Locale.US, m)) + ":%02d".format(Locale.US, s % 60)
    }
    val color = when { ph == "done" -> Color(0xFFC92A2A); warn == 5 -> Color(0xFFE8590C); warn == 15 -> Color(0xFFE8A317); else -> Theme.c.text }
    ToolFrame(
        primary = when { start == 0L -> L("examTimer.start"); ph == "done" -> L("examTimer.reset"); pauseAt > 0 -> L("examTimer.resume"); else -> L("examTimer.pause") },
        onPrimary = {
            when {
                start == 0L -> { start = System.currentTimeMillis(); pauseAt = 0; lastPhase = "" }
                ph == "done" -> { start = 0; pauseAt = 0; lastPhase = "" }
                pauseAt > 0 -> { start += System.currentTimeMillis() - pauseAt; pauseAt = 0 }
                else -> pauseAt = System.currentTimeMillis()
            }
            save()
        },
        secondary = if (start > 0 && ph != "done") L("examTimer.reset") else null,
        onSecondary = { start = 0; pauseAt = 0; lastPhase = ""; save() },
        center = true,
    ) {
        if (name.isNotBlank()) Text(name, color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (room.isNotBlank()) Text(room, color = Theme.c.muted, fontSize = 16.sp)
        Text(when (ph) { "read" -> L("examTimer.phaseRead"); "exam" -> L("examTimer.phaseExam"); "done" -> L("examTimer.phaseDone")
            else -> if (r > 0) L("examTimer.phaseRead") else L("examTimer.phaseExam") }, color = color, fontSize = 16.sp)
        Text(mmss(if (start > 0) rem else d), color = color, fontSize = 76.sp, fontWeight = FontWeight.Bold)
        LinearProgressIndicator({ frac }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = color, trackColor = Theme.c.surface2)
        if (warn > 0) Text(L("examTimer.warn$warn"), color = color, fontWeight = FontWeight.Bold)
        if (running) {
            val end = java.time.Instant.ofEpochMilli(start + r + d).atZone(ZoneId.systemDefault())
            Text(T("examTimer.endsAt", TimeCalc.clock(end.hour * 60 + end.minute)), color = Theme.c.muted)
        }
        if (start == 0L) AppCard {
            OutlinedTextField(name, { name = it.take(60); save() }, Modifier.fillMaxWidth(), label = { Text(L("examTimer.nameLabel")) },
                placeholder = { Text(L("examTimer.namePh")) }, singleLine = true)
            OutlinedTextField(room, { room = it.take(60); save() }, Modifier.fillMaxWidth(), label = { Text(L("examTimer.roomLabel")) },
                placeholder = { Text(L("examTimer.roomPh")) }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(read, { read = it.filter(Char::isDigit).take(2); save() }, Modifier.weight(1f), label = { Text(L("examTimer.readLabel")) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(dur, { dur = it.filter(Char::isDigit).take(3); save() }, Modifier.weight(1f), label = { Text(L("examTimer.durLabel")) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        Note(L("examTimer.appNote"))
    }
}

/* ---------- Sınıf araçları ---------- */

@Composable
fun ClassroomScreen() {
    val st = remember { runCatching { JSONObject(Prefs.store.getString(Classroom.KEY, "{}")!!) }.getOrDefault(JSONObject()) }
    var tab by remember { mutableStateOf(st.optString("tab", "picker").ifEmpty { "picker" }) }
    var names by remember { mutableStateOf(st.optString("names")) }
    var remove by remember { mutableStateOf(st.optBoolean("remove", true)) }
    var pool by remember { mutableStateOf(st.optJSONArray("pool")?.let { a -> (0 until a.length()).map { a.getString(it) } }) }
    var picked by remember { mutableStateOf(st.optString("picked")) }
    var teams by remember { mutableIntStateOf(st.optInt("teams", 2)) }
    var teamList by remember { mutableStateOf(st.optJSONArray("teamList")?.let { a -> (0 until a.length()).map { i -> a.getJSONArray(i).let { t -> (0 until t.length()).map { t.getString(it) } } } } ?: emptyList()) }
    val counters = remember { mutableStateListOf<Pair<String, Int>>().also { l -> st.optJSONArray("counters")?.let { a -> for (i in 0 until a.length()) a.getJSONObject(i).let { l.add(it.optString("n") to it.optInt("v")) } } } }
    var cname by remember { mutableStateOf("") }
    var empty by remember { mutableStateOf(false) }
    val rnd = remember { java.security.SecureRandom() }
    fun save() {
        val o = JSONObject().put("tab", tab).put("names", names).put("remove", remove).put("picked", picked).put("teams", teams)
            .put("teamList", JSONArray(teamList.map { JSONArray(it) }))
            .put("counters", JSONArray(counters.map { JSONObject().put("n", it.first).put("v", it.second) }))
        pool?.let { o.put("pool", JSONArray(it)) }
        Prefs.store.edit().putString(Classroom.KEY, o.toString()).apply()
    }
    val all = Classroom.names(names)
    val left = pool?.filter { it in all } ?: all
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((k, label) in listOf("picker" to "classroom.tabPicker", "teams" to "classroom.tabTeams", "counter" to "classroom.tabCounter"))
                Pill(L(label), tab == k) { tab = k; empty = false; save() }
        }
        if (tab != "counter") {
            OutlinedTextField(names, { names = it.take(4000); pool = null; empty = false; save() }, Modifier.fillMaxWidth().height(140.dp),
                label = { Text(L("classroom.namesLabel")) }, placeholder = { Text(L("classroom.namesPh")) })
            if (empty) Text(L("classroom.empty"), color = Color(0xFFC92A2A))
        }
        when (tab) {
            "picker" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("classroom.removePicked"), Modifier.weight(1f), color = Theme.c.text)
                    SoftSwitch(remove, { remove = it; pool = null; save() }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                AppCard(background = Theme.c.surface2) {
                    SectionTitle(L("classroom.picked"))
                    Text(picked.ifEmpty { "–" }, Modifier.fillMaxWidth(), color = Theme.c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (remove && all.isNotEmpty()) Note(T("classroom.left", left.size))
                }
                RoundButton(L("classroom.pick"), true, Modifier.fillMaxWidth()) {
                    if (all.isEmpty()) { empty = true; return@RoundButton }
                    val list = (if (remove) left else all).ifEmpty { all }
                    val choice = list[rnd.nextInt(list.size)]
                    if (remove) pool = list.filter { it != choice }
                    picked = choice; save(); Beep.play(988.0, 150, 0.2f)
                }
                RoundButton(L("classroom.restore"), false, Modifier.fillMaxWidth()) { pool = null; picked = ""; save() }
            }
            "teams" -> {
                SectionTitle(L("classroom.teamCount"))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (n in 2..12) Pill("$n", teams == n) { teams = n; save() }
                }
                RoundButton(L("classroom.makeTeams"), true, Modifier.fillMaxWidth()) {
                    if (all.isEmpty()) { empty = true; return@RoundButton }
                    val k = minOf(teams, all.size).coerceAtLeast(2).coerceAtMost(maxOf(2, all.size))
                    val sh = all.toMutableList().also { l -> for (i in l.size - 1 downTo 1) { val j = rnd.nextInt(i + 1); val x = l[i]; l[i] = l[j]; l[j] = x } }
                    teamList = (0 until k).map { t -> sh.filterIndexed { i, _ -> i % k == t } }
                    save()
                }
                teamList.forEachIndexed { i, t ->
                    AppCard {
                        Text(T("classroom.team", i + 1), color = Theme.c.accent, fontWeight = FontWeight.Bold)
                        t.forEachIndexed { j, n -> Text("${j + 1}. $n", color = Theme.c.text) }
                    }
                }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(cname, { cname = it.take(30) }, Modifier.weight(1f), label = { Text(L("classroom.counterName")) }, singleLine = true)
                    RoundButton(L("classroom.addCounter"), true) {
                        if (counters.size < 24) {
                            counters.add(cname.trim().ifEmpty { L("classroom.counterDefault") + " " + (counters.size + 1) } to 0)
                            cname = ""; save()
                        }
                    }
                }
                counters.forEachIndexed { i, c ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.c.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(c.first, Modifier.weight(1f), color = Theme.c.text, fontWeight = FontWeight.SemiBold)
                        RoundButton("−", false, Modifier.width(56.dp)) { counters[i] = c.first to c.second - 1; save() }
                        Text("${c.second}", Modifier.width(56.dp), color = Theme.c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        RoundButton("+", true, Modifier.width(56.dp)) { counters[i] = c.first to c.second + 1; save() }
                        Text("×", Modifier.clickable { counters.removeAt(i); save() }.padding(8.dp), color = Theme.c.muted, fontSize = 20.sp)
                    }
                }
                if (counters.isNotEmpty()) RoundButton(L("classroom.reset"), false, Modifier.fillMaxWidth()) {
                    for (i in counters.indices) counters[i] = counters[i].first to 0
                    save()
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Note(L("classroom.appNote"))
    }
}

/** Tümü'ndeki dokuz aracın sayfa adları (rota → başlık anahtarı) */
val SITE_TOOL_TITLES = mapOf(
    "chess" to "chess.h1", "metronome" to "metronome.h1", "talking" to "talking.h1", "zodiac" to "zodiac.h1", "timecalc" to "timeCalc.h1",
    "bridge" to "bridge.h1", "retire" to "retire.h1", "exam" to "examTimer.h1", "classroom" to "classroom.h1",
)

@Composable
fun SiteToolScreen(route: String) {
    when (route) {
        "chess" -> ChessClockScreen()
        "metronome" -> MetronomeScreen()
        "talking" -> TalkingClockScreen()
        "zodiac" -> ZodiacScreen()
        "timecalc" -> TimeCalcScreen()
        "bridge" -> BridgeScreen()
        "retire" -> RetireScreen()
        "exam" -> ExamTimerScreen()
        "classroom" -> ClassroomScreen()
    }
}
