package tr.bumba.time

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Saat görünümü ekleri (2. analiz #10, #12, #21, #22; iOS'taki anahtarlarla aynı): saniye ibresinin hareketi
 * (`secMotion`: tick, mech, sweep), dijitalde küçük saniye (`secSmall`), tık sesi (`tickSound`), tam ekranda metni
 * tabela gibi büyük gösterme (`fs.banner`), ikili saat diliminin ikinci şehri (`dualCity`).
 */
object ClockLook {
    val MOTIONS = listOf("tick", "mech", "sweep")
    var motion by mutableStateOf("tick"); private set
    var secSmall by mutableStateOf(false); private set
    var tickSound by mutableStateOf(false); private set
    var banner by mutableStateOf(false); private set
    var dualCity by mutableStateOf(""); private set
    /** Rakam rengi (2. analiz #24): "theme" ya da renk kimliği (color.<id>); dış hatlı rakamlar */
    var digitColor by mutableStateOf("theme"); private set
    var outline by mutableStateOf(false); private set
    /** Büyük rakam için iki temada da okunur orta tonlar */
    val DIGIT_COLORS = linkedMapOf(
        "red" to 0xE03131L, "orange" to 0xF76707L, "amber" to 0xF59F00L, "green" to 0x2F9E44L,
        "aqua" to 0x1098ADL, "blue" to 0x1C7ED6L, "violet" to 0x7048E8L, "pink" to 0xD6336CL,
    )
    fun digitInk(c: AppColors): androidx.compose.ui.graphics.Color =
        DIGIT_COLORS[digitColor]?.let { androidx.compose.ui.graphics.Color(0xFF000000 or it) } ?: c.ink
    fun saveDigitColor(v: String) { digitColor = v; Prefs.store.edit().putString("look.digitColor", v).apply() }
    fun saveOutline(v: Boolean) { outline = v; Prefs.store.edit().putBoolean("look.outline", v).apply() }

    fun load() {
        val s = Prefs.store
        motion = s.getString("secMotion", "tick")!!
        secSmall = s.getBoolean("secSmall", false)
        tickSound = s.getBoolean("tickSound", false)
        banner = s.getBoolean("fs.banner", false)
        dualCity = s.getString("dualCity", "")!!
        digitColor = s.getString("look.digitColor", "theme")!!
        outline = s.getBoolean("look.outline", false)
    }

    fun saveMotion(v: String) { motion = v; Prefs.store.edit().putString("secMotion", v).apply() }
    fun saveSecSmall(v: Boolean) { secSmall = v; Prefs.store.edit().putBoolean("secSmall", v).apply() }
    fun saveTickSound(v: Boolean) { tickSound = v; Prefs.store.edit().putBoolean("tickSound", v).apply() }
    fun saveBanner(v: Boolean) { banner = v; Prefs.store.edit().putBoolean("fs.banner", v).apply() }
    fun saveDualCity(v: String) { dualCity = v; Prefs.store.edit().putString("dualCity", v).apply() }

    /** Ekranın yenilenme aralığı (ms; tıkta saniyede bir yeter) */
    fun frameMs(m: String = motion): Long = when (m) { "mech" -> 125; "sweep" -> 33; else -> 1000 }

    /** Dakikanın içindeki saniye (kesirli): tık tam saniye, mekanik 1/8 adımlarla, kayma akıcı */
    fun seconds(epochMs: Long, m: String = motion): Double {
        val s = ((epochMs / 1000) % 60).toDouble()
        val frac = (epochMs % 1000) / 1000.0
        return when (m) {
            "mech" -> s + kotlin.math.floor(frac * 8) / 8
            "sweep" -> s + frac
            else -> s
        }
    }

    /** İkinci şehir: seçim yoksa Londra (burası Londra ise New York) */
    fun dual(): City? {
        City.all.firstOrNull { it.key == dualCity }?.let { return it }
        val fallback = if (ZoneId.systemDefault().id == "Europe/London") "new-york" else "london"
        return City.all.firstOrNull { it.key == fallback } ?: City.all.firstOrNull()
    }
}

/**
 * Tabela metni (2. analiz #22): sözcükler bölünmesin diye satırlar sözcük sınırından seçilir; 1–3 satır arasından
 * yazının en büyük çıkacağı bölünme alınır (harf genişliği ~0,6 em varsayımıyla). `aspect` = genişlik / yükseklik.
 */
fun bannerLines(text: String, aspect: Double): String {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.size <= 1) return text.trim()
    var best = words.joinToString(" ")
    var bestSize = 0.0
    for (k in 1..minOf(3, words.size)) {
        // Sözcükleri k satıra en uzun satır en kısa olacak biçimde böl (dinamik programlama yerine tüm bölmeler; en çok 3 satır)
        val splits = if (k == 1) listOf(listOf(words.size)) else if (k == 2) (1 until words.size).map { listOf(it, words.size) }
            else (1 until words.size - 1).flatMap { a -> (a + 1 until words.size).map { b -> listOf(a, b, words.size) } }
        for (sp in splits) {
            var from = 0
            val lines = sp.map { to -> words.subList(from, to).joinToString(" ").also { from = to } }
            val longest = lines.maxOf { it.length }.toDouble()
            val size = minOf(aspect / (0.6 * longest), 1.0 / (1.05 * k))
            if (size > bestSize) { bestSize = size; best = lines.joinToString("\n") }
        }
    }
    return best
}

/** İkili saat dilimi için kısa şehir kodu ve fark: "LON +3" */
object ZoneCaption {
    private val KNOWN = mapOf(
        "new-york" to "NYC", "los-angeles" to "LAX", "san-francisco" to "SFO", "hong-kong" to "HKG", "mexico-city" to "MEX",
        "sao-paulo" to "SAO", "buenos-aires" to "BUE", "kuala-lumpur" to "KUL", "cape-town" to "CPT", "tel-aviv" to "TLV",
        "abu-dhabi" to "AUH", "new-delhi" to "DEL", "mumbai" to "BOM", "beijing" to "BJS", "tokyo" to "TYO", "seoul" to "SEL",
    )

    fun code(key: String): String = KNOWN[key] ?: key.replace("-", "").take(3).uppercase(java.util.Locale.ROOT)

    fun diff(zone: ZoneId, base: ZoneId, at: Instant): String {
        val d = zone.rules.getOffset(at).totalSeconds - base.rules.getOffset(at).totalSeconds
        if (d == 0) return "±0"
        val h = abs(d) / 3600
        val m = (abs(d) % 3600) / 60
        return (if (d > 0) "+" else "−") + (if (m == 0) "$h" else "%d:%02d".format(h, m))
    }
}

/**
 * Saniye tık sesi (2. analiz #10; sitedeki `sth-prefs.tick` gibi): saat ekranı açıkken her saniye kısa bir tık.
 * Ses telefonda üretilir (dosya yok); telefon sessiz ya da titreşimdeyken çalmaz.
 */
object TickSound {
    private var track: AudioTrack? = null
    private var last = 0L

    fun tick(c: Context, second: Long) {
        if (!ClockLook.tickSound || second == last) return
        last = second
        val am = c.getSystemService(AudioManager::class.java)
        if (am != null && am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val t = track ?: build()?.also { track = it } ?: return
        runCatching {
            t.stop()
            t.reloadStaticData()
            t.play()
        }
    }

    fun stop() {
        track?.let { runCatching { it.stop(); it.release() } }
        track = null
    }

    private fun build(): AudioTrack? = runCatching {
        val rate = 44100
        val n = 1323 // 30 ms
        val pcm = ShortArray(n)
        var seed = 12345L
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            seed = (seed * 1664525 + 1013904223) and 0xFFFFFFFFL
            val noise = (seed.toInt()).toDouble() / Int.MAX_VALUE
            val env = exp(-t / 0.004)
            pcm[i] = ((0.6 * sin(2 * PI * 2200 * t) + 0.4 * noise) * env * 0.35 * Short.MAX_VALUE).toInt().toShort()
        }
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(n * 2)
            .build().also { it.write(pcm, 0, n) }
    }.getOrNull()
}

/** Kayan ya da mekanik saniye için ekran karesi saati (tıkta null: dış saat yeter) */
@Composable
fun rememberFrameMillis(enabled: Boolean, everyMs: Long): Long? {
    if (!enabled) return null
    var ms by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(everyMs) {
        var lastDraw = 0L
        while (true) {
            withFrameMillis { }
            val now = System.currentTimeMillis()
            if (now - lastDraw >= everyMs) { ms = now; lastDraw = now }
        }
    }
    return ms
}

/**
 * 21. saat modeli (2. analiz #12; yalnız uygulamalarda): iki analog kadran; solda burası, sağda seçilen şehir
 * (kadranın içinde kısa kod ve fark). Şehrin adına dokununca şehir seçilir.
 */
@Composable
fun DualClockPart(now: Instant, place: AppPlace, width: Dp, height: Dp) {
    val city = ClockLook.dual()
    val zone = city?.zone ?: ZoneId.systemDefault()
    var picking by remember { mutableStateOf(false) }
    val d = minOf(width * 0.46f, height * 0.72f)
    Row(horizontalArrangement = Arrangement.spacedBy(width * 0.04f), verticalAlignment = Alignment.Top) {
        DualColumn(now, ZoneId.systemDefault(), d, place.name.ifEmpty { L("dual.here") }, null) {}
        DualColumn(now, zone, d, city?.displayName ?: "", city?.let { ZoneCaption.code(it.key) + " " + ZoneCaption.diff(zone, ZoneId.systemDefault(), now) }) {
            picking = true
        }
    }
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { picking = false }) { Text(L("common.done"), color = Theme.c.accent) } },
            title = { Text(L("dual.city")) },
            text = {
                LazyColumn {
                    items(City.all) { c ->
                        Row(Modifier.clickable { ClockLook.saveDualCity(c.key); picking = false }.width(280.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(c.displayName, Modifier.weight(1f), color = if (c.key == city?.key) Theme.c.accent else Theme.c.text,
                                fontSize = 16.sp)
                            Text(ZoneCaption.diff(c.zone, ZoneId.systemDefault(), Instant.now()), color = Theme.c.muted, style = Tnum)
                        }
                    }
                }
            },
            containerColor = Theme.c.surface,
        )
    }
}

@Composable
private fun DualColumn(now: Instant, zone: ZoneId, size: Dp, title: String, caption: String?, onTitle: () -> Unit) {
    val t = now.atZone(zone)
    Column(Modifier.width(size), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(size * 0.05f)) {
        AnalogClock(t, Modifier.size(size), caption = caption)
        Text(title, Modifier.clickable(onClick = onTitle), color = Theme.c.text, fontSize = (size.value * 0.09f).sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
        Ltr {
            Text(Fmt.time(t) + Fmt.ampm(t).let { if (it.isEmpty()) "" else " $it" }, color = Theme.c.muted,
                fontSize = (size.value * 0.085f).sp, style = Tnum)
        }
    }
}

/**
 * Tam ekranda saat modellerinin iki yanındaki araç sayfaları (2. analiz #26): ilk saatten geriye pomodoro, son
 * saatten sonra kronometre. Durum araçların kendi kayıtlarıdır (`pomo.*`, `sw.*`).
 */
enum class FullscreenTool { POMODORO, STOPWATCH }

/** Pomodoro durumunu tam ekran sayfasından değiştiren işlemler (PomodoroScreen'deki kurallarla aynı) */
object PomoEngine {
    fun phase() = ToolState.str("pomo.phase", "work")
    fun phaseMs(): Long = when (phase()) {
        "work" -> ToolState.int("pomo.work", 25)
        "short" -> ToolState.int("pomo.short", 5)
        else -> ToolState.int("pomo.long", 15)
    } * 60_000L

    fun remaining(now: Long): Long {
        val end = ToolState.long("pomo.end")
        val left = ToolState.long("pomo.left")
        return if (end > 0) maxOf(0, end - now) else if (left > 0) left else phaseMs()
    }

    private fun sync(c: Context) {
        PomoNoise.sync(c)
        LiveNotifications.sync(c)
        FocusBlock.sync(c)
    }

    fun start(c: Context) {
        val left = ToolState.long("pomo.left")
        val end = System.currentTimeMillis() + (if (left > 0) left else phaseMs())
        ToolState.putLong("pomo.end", end)
        ToolState.putLong("pomo.left", 0)
        sync(c)
        AlarmScheduler.schedule(c, AlarmScheduler.POMODORO, end, T("tools.pomodoro.phaseDone", L("tools.pomodoro.${phase()}")), AlarmScheduler.POMODORO)
    }

    fun pause(c: Context) {
        ToolState.putLong("pomo.left", remaining(System.currentTimeMillis()))
        ToolState.putLong("pomo.end", 0)
        AlarmScheduler.cancel(c, AlarmScheduler.POMODORO)
        sync(c)
    }

    fun advance(c: Context, completed: Boolean) {
        if (!completed) AlarmScheduler.cancel(c, AlarmScheduler.POMODORO)
        val end = ToolState.long("pomo.end")
        var round = ToolState.int("pomo.round", 1)
        if (phase() == "work") {
            if (completed) {
                ToolState.putInt("pomo.done", ToolState.int("pomo.done", 0) + 1)
                BadgeStore.hit(Badge.POMODORO)
                val task = ToolState.str("pomo.task")
                PomoLog.record(ToolState.int("pomo.work", 25), task, if (end > 0) end else System.currentTimeMillis(), ToolState.str("pomo.tag", "none"))
                PomoTodo.save(PomoTodo.credit(PomoTodo.load(), task))
            }
            ToolState.putStr("pomo.phase", if (round >= ToolState.int("pomo.rounds", 4)) "long" else "short")
        } else {
            if (phase() == "long") round = 1 else round += 1
            ToolState.putInt("pomo.round", round)
            ToolState.putStr("pomo.phase", "work")
        }
        ToolState.putLong("pomo.end", 0)
        ToolState.putLong("pomo.left", 0)
        sync(c)
    }
}

@Composable
fun FullscreenToolPage(tool: FullscreenTool, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var version by remember { mutableIntStateOf(0) } // durum değişince yeniden okunur
    LaunchedEffect(tool) {
        while (true) {
            now = System.currentTimeMillis()
            if (tool == FullscreenTool.POMODORO) {
                val end = ToolState.long("pomo.end")
                if (end > 0 && now >= end) { PomoEngine.advance(context, completed = true); version++ }
            }
            delay(if (tool == FullscreenTool.STOPWATCH) 30 else 250)
        }
    }
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val big = minOf(maxWidth * 0.2f, maxHeight * 0.3f)
        @Suppress("UNUSED_EXPRESSION") version
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(big * 0.2f)) {
            when (tool) {
                FullscreenTool.POMODORO -> {
                    val phase = PomoEngine.phase()
                    val running = ToolState.long("pomo.end") > 0
                    Text(L("tools.pomodoro.$phase"), color = if (phase == "work") Theme.c.accent else AppData.color("religious"),
                        fontSize = (big.value * 0.28f).sp, fontWeight = FontWeight.SemiBold)
                    Ltr {
                        Text(clock((PomoEngine.remaining(now) + 999) / 1000), color = Theme.c.ink, fontSize = big.value.sp,
                            fontFamily = ClockFont.current.family, fontWeight = ClockFont.current.weight, style = Tnum, maxLines = 1)
                    }
                    Text(T("tools.pomodoro.round", ToolState.int("pomo.round", 1), ToolState.int("pomo.rounds", 4)), color = Theme.c.muted,
                        fontSize = (big.value * 0.2f).sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { PomoEngine.advance(context, completed = false); version++ }) {
                            Icon(Icons.Filled.SkipNext, L("tools.pomodoro.skip"), tint = Theme.c.accent)
                        }
                        IconButton(onClick = { if (running) PomoEngine.pause(context) else PomoEngine.start(context); version++ },
                            Modifier.size(big * 0.6f)) {
                            Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                L(if (running) "tools.pomodoro.pause" else "tools.pomodoro.start"), tint = Theme.c.accent, modifier = Modifier.size(big * 0.5f))
                        }
                    }
                }
                FullscreenTool.STOPWATCH -> {
                    val start = ToolState.long("sw.start")
                    val acc = ToolState.long("sw.acc")
                    val total = acc + if (start > 0) maxOf(0, now - start) else 0
                    Text(L("tools.hub.cards.stopwatch.name"), color = Theme.c.accent, fontSize = (big.value * 0.28f).sp, fontWeight = FontWeight.SemiBold)
                    Ltr {
                        Text(clock(total / 1000) + ".%02d".format((total / 10) % 100), color = Theme.c.ink, fontSize = big.value.sp,
                            fontFamily = ClockFont.current.family, fontWeight = ClockFont.current.weight, style = Tnum, maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            ToolState.putLong("sw.acc", 0)
                            ToolState.putLong("sw.start", if (start > 0) System.currentTimeMillis() else 0)
                            ToolState.putStr("sw.laps", "")
                            LiveNotifications.sync(context); version++
                        }) { Icon(Icons.Filled.Refresh, L("tools.stopwatch.reset"), tint = Theme.c.accent) }
                        IconButton(onClick = {
                            if (start > 0) {
                                ToolState.putLong("sw.acc", total); ToolState.putLong("sw.start", 0)
                            } else {
                                ToolState.putLong("sw.start", System.currentTimeMillis()); BadgeStore.hit(Badge.KRONOMETRE)
                            }
                            LiveNotifications.sync(context); version++
                        }, Modifier.size(big * 0.6f)) {
                            Icon(if (start > 0) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                L(if (start > 0) "tools.stopwatch.stop" else "tools.stopwatch.start"), tint = Theme.c.accent, modifier = Modifier.size(big * 0.5f))
                        }
                    }
                }
            }
        }
    }
}
