package tr.bumba.time

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import tr.bumba.time.core.Nature
import tr.bumba.time.core.Sky
import java.time.Instant
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/* ---------------- Fotoğraf dokulu Ay (2. analiz #103) ----------------
   NASA SVS CGI Moon Kit dokusu (lroc_color_2k.jpg, kamu malı, 1024×512; assets/moon.jpg). Her evre telefonda çizilir:
   diskin her noktası için yüzey normali → selenografik boylam/enlem → doku pikseli; aydınlık Güneş yönüyle (evre
   kesrinden), karanlık yarıda hafif yer ışığı; güney yarımkürede 180° döner. Sitedeki moon-render.js ile aynı kural. */
object MoonTexture {
    @Volatile private var pixels: IntArray? = null
    private var w = 0
    private var h = 0
    private val cache = object : android.util.LruCache<String, Bitmap>(48) {}

    private fun load(c: Context): Boolean {
        if (pixels != null) return true
        return runCatching {
            val b = c.assets.open("moon.jpg").use { BitmapFactory.decodeStream(it) }
            w = b.width; h = b.height
            val p = IntArray(w * h); b.getPixels(p, 0, w, 0, 0, w, h); pixels = p
            true
        }.getOrDefault(false)
    }

    /** frac: evre kesri (0 yeni ay, 0,5 dolunay); south: güney yarımküre; size: piksel */
    fun bitmap(c: Context, frac: Double, south: Boolean, size: Int): Bitmap? {
        if (!load(c)) return null
        val key = "${(frac * 200).roundToInt()}-$south-$size"
        cache.get(key)?.let { return it }
        val T = pixels ?: return null
        val th = 2 * PI * (frac * 200).roundToInt() / 200.0
        val sx = sin(th); val sz = -cos(th)
        val out = IntArray(size * size)
        val r = size / 2.0
        for (py in 0 until size) for (px in 0 until size) {
            var x = (px + 0.5 - r) / r; var y = (r - py - 0.5) / r
            val rr = x * x + y * y
            if (rr > 1) continue
            if (south) { x = -x; y = -y }
            val z = sqrt(1 - rr)
            val lon = atan2(x, z); val lat = Math.asin(y)
            val u = (((lon / PI + 1) / 2) * w).toInt().coerceIn(0, w - 1)
            val v = ((0.5 - lat / PI) * h).toInt().coerceIn(0, h - 1)
            val tp = T[v * w + u]
            val lit = x * sx + z * sz
            var k = if (lit > 0) min(1.0, 0.15 + lit * 1.1) else 0.0
            if (lit > -0.04 && lit <= 0.04) k = max(k, (lit + 0.04) / 0.08 * 0.2)
            k = max(k, 0.07)
            val edge = if (rr > 0.96) max(0.0, (1 - rr) / 0.04) else 1.0
            val rC = (((tp shr 16) and 0xff) * k).toInt(); val gC = (((tp shr 8) and 0xff) * k).toInt(); val bC = ((tp and 0xff) * k).toInt()
            out[py * size + px] = ((255 * edge).toInt() shl 24) or (rC shl 16) or (gC shl 8) or bC
        }
        return Bitmap.createBitmap(out, size, size, Bitmap.Config.ARGB_8888).also { cache.put(key, it) }
    }
}

/** Dokulu Ay görseli; doku yoksa evre emojisi */
@Composable
fun MoonImage(frac: Double, size: Dp, south: Boolean = AppPlace.current().lat < 0) {
    val c = LocalContext.current
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx().roundToInt().coerceIn(16, 512) }
    val bmp = remember(frac, south, px) { MoonTexture.bitmap(c, frac, south, px) }
    if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.size(size))
    else Text(MOON_PHASE_EMOJI[((frac * 8 + 0.5).toInt()) % 8], fontSize = (size.value * 0.8).sp)
}

/* ---------------- Gökyüzü olayları (2. analiz #154) ---------------- */
data class SkyEvent(val t: Long, val k: String, val id: String?, val p: String?, val a: String?, val b: String?, val zhr: Int?, val illum: Int?, val sep: Double?, val deg: Double?, val ev: Boolean) {
    val title: String
        get() {
            fun pn(x: String?) = L("sky.planets.${x ?: ""}")
            fun n1(v: Double?) = java.text.NumberFormat.getNumberInstance(Strings.numberLocale).apply { minimumFractionDigits = 1; maximumFractionDigits = 1 }.format(v ?: 0.0)
            return when (k) {
                "shower" -> T("sky.evShower", L("sky.showers.$id"))
                "opp" -> T("sky.evOpp", pn(p))
                "elong" -> T(if (ev) "sky.evElongE" else "sky.evElongW", pn(p), n1(deg))
                "conj" -> T("sky.evConj", pn(a), pn(b), n1(sep))
                else -> T("sky.evMoon", pn(p), n1(sep))
            }
        }
    val detail: String?
        get() = if (k != "shower") null else (if (zhr != null) T("sky.showerRate", zhr) else L("sky.showerVar")) + " · " + T("sky.moonLight", Fmt.percent(illum ?: 0))

    companion object {
        @Volatile private var cache: List<SkyEvent>? = null
        fun all(c: Context): List<SkyEvent> = cache ?: runCatching {
            val a = JSONArray(c.assets.open("sky-events.json").bufferedReader().use { it.readText() })
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                fun s(k: String) = if (o.has(k) && !o.isNull(k)) o.getString(k) else null
                fun d(k: String) = if (o.has(k) && !o.isNull(k)) o.getDouble(k) else null
                SkyEvent(o.getLong("t"), o.getString("k"), s("id"), s("p"), s("a"), s("b"), if (o.has("zhr") && !o.isNull("zhr")) o.getInt("zhr") else null,
                    if (o.has("illum")) o.getInt("illum") else null, d("sep"), d("deg"), o.optBoolean("ev"))
            }
        }.getOrDefault(emptyList()).also { cache = it }
        fun upcoming(c: Context, now: Long = System.currentTimeMillis()) = all(c).filter { it.t > now - 12 * 3_600_000 }
    }
}

/* ---------------- Takımyıldızlar (d3-celestial, BSD-3) ---------------- */
object Constellations {
    class Con(val id: String, val rank: Int, val lines: List<List<Pair<Double, Double>>>, val at: Pair<Double, Double>?, val names: JSONObject?)
    @Volatile private var cache: List<Con>? = null
    fun all(c: Context): List<Con> = cache ?: runCatching {
        val o = JSONObject(c.assets.open("constellations.json").bufferedReader().use { it.readText() })
        o.keys().asSequence().map { id ->
            val x = o.getJSONObject(id)
            val ls = x.getJSONArray("lines")
            val lines = (0 until ls.length()).map { i -> val l = ls.getJSONArray(i); (0 until l.length()).map { j -> val p = l.getJSONArray(j); p.getDouble(0) to p.getDouble(1) } }
            val at = x.optJSONArray("at")?.let { it.getDouble(0) to it.getDouble(1) }
            Con(id, x.optInt("rank", 3), lines, at, x.optJSONObject("n"))
        }.toList()
    }.getOrDefault(emptyList()).also { cache = it }
    fun name(c: Con): String = c.names?.optString(Strings.lang, null) ?: c.names?.optString("en") ?: c.id
}

private val PLANET_COLORS = mapOf("mercury" to Color(0xFFB9A68D), "venus" to Color(0xFFF4E3A1), "mars" to Color(0xFFE8714C), "jupiter" to Color(0xFFE9C48F), "saturn" to Color(0xFFD9C27A))

private fun sunAA(ms: Double, p: AppPlace): Nature.AltAz { val s = Nature.sunPosition(ms); return Nature.altAz(ms, s.rightAscension, s.declination, p.lat, p.lon) }
private fun moonAA(ms: Double, p: AppPlace): Nature.AltAz {
    val m = Nature.moonPosition(ms); val a = Nature.altAz(ms, m.rightAscension, m.declination, p.lat, p.lon)
    return Nature.AltAz(a.alt - m.parallax * cos(Math.toRadians(a.alt)), a.az)
}
private fun planetAA(name: String, ms: Double, p: AppPlace): Nature.AltAz { val x = Sky.planet(name, ms); return Nature.altAz(ms, x.ra, x.dec, p.lat, p.lon) }

/** Bu gece: Güneş belirli yüksekliğin altındaki ilk aralık (10 dk adım) */
private fun darkWindow(t0: Long, place: AppPlace, lim: Double): Pair<Long, Long>? {
    val below = { t: Long -> sunAA(t.toDouble(), place).alt < lim }
    val step = 600_000L
    var a: Long? = null
    if (below(t0)) { for (i in 1..144) { val t = t0 - i * step; if (!below(t)) { a = t + step; break } }; if (a == null) return null }
    else { for (i in 1..144) { val t = t0 + i * step; if (below(t)) { a = t; break } }; if (a == null) return null }
    for (i in 1..144) { val t = a + i * step; if (!below(t)) return a to t }
    return a to a + 86_400_000
}

private class Visibility(val first: Long?, val last: Long?, val bestT: Long, val bestAlt: Double, val firstAz: Double)
private fun visibility(name: String, night: Pair<Long, Long>?, place: AppPlace): Visibility {
    var first: Long? = null; var last: Long? = null; var bt = 0L; var ba = -90.0; var faz = 0.0
    if (night != null) {
        var t = night.first
        while (t <= night.second) {
            if (sunAA(t.toDouble(), place).alt <= -6) {
                val a = planetAA(name, t.toDouble(), place)
                if (a.alt >= 5) { if (first == null) { first = t; faz = a.az }; last = t }
                if (a.alt > ba) { ba = a.alt; bt = t }
            }
            t += 600_000
        }
    }
    return Visibility(first, last, bt, ba, faz)
}

private fun dirName(az: Double): String = L("weather.compass.${((az / 45).roundToInt()) % 8}")

/**
 * Gece gökyüzü (2. analiz #157; sitedeki /gece-gokyuzu/): gökyüzü çizimi (yıldızlar, takımyıldızlar, gezegenler, dokulu
 * Ay), saat kaydırıcı ve oynatma, kırmızı ışık; bu gece özeti; gezegen tablosu; gökyüzü olayları (#154).
 */
@Composable
fun SkyScreen() {
    val c = LocalContext.current
    val place = AppPlace.current()
    val now = System.currentTimeMillis()
    val step = 15 * 60_000L
    var base by remember { mutableStateOf(now / step * step) }
    var slot by rememberSaveable { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var red by rememberSaveable { mutableStateOf(false) }
    var showCon by rememberSaveable { mutableStateOf(true) }
    var shown by rememberSaveable { mutableIntStateOf(10) }
    val night = remember(place.id, base / 3_600_000) { darkWindow(base, place, -0.833) }
    LaunchedEffect(place.id) {
        val dark = sunAA(now.toDouble(), place).alt < -6
        slot = if (dark || night == null) 0 else ((night.first + 3_600_000 - base) / step).toInt().coerceIn(0, 96)
    }
    LaunchedEffect(playing) { while (playing) { delay(160); if (slot >= 96) playing = false else slot++ } }
    val ms = base + slot * step
    val tint = if (red) Color(0xFFB82016) else Color.Unspecified
    Column(Modifier.fillMaxSize().background(if (red) Color.Black else Color.Transparent).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PlacePicker()
        Box(Modifier.fillMaxWidth().aspectRatio(1f).nightTintIf(red)) { SkyChart(ms.toDouble(), place, showCon) }
        Text(Fmt.hm(Instant.ofEpochMilli(ms), place.zone) + " · " + Fmt.longDate(Instant.ofEpochMilli(ms).atZone(place.zone).toLocalDate()),
            Modifier.fillMaxWidth(), color = if (red) tint else Theme.c.text, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        Slider(slot.toFloat(), { slot = it.roundToInt(); playing = false }, valueRange = 0f..96f, steps = 95,
            colors = SliderDefaults.colors(thumbColor = Theme.c.accent, activeTrackColor = Theme.c.accent))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(playing, { playing = !playing }, { Text(L(if (playing) "sky.pause" else "sky.play")) })
            FilterChip(false, { playing = false; base = System.currentTimeMillis() / step * step; slot = 0 }, { Text(L("sky.now")) })
            FilterChip(showCon, { showCon = !showCon }, { Text(L("sky.constellations")) })
            FilterChip(red, { red = !red }, { Text(L("sky.red")) })
        }
        if (red) Text(L("sky.redHint"), color = tint, fontSize = 12.sp)
        TonightCard(place, night, base)
        SectionTitle(L("sky.tableHeading"))
        AppCard {
            for (name in Sky.PLANETS) {
                val p = Sky.planet(name, now.toDouble())
                val v = visibility(name, night, place)
                val whenTxt = if (v.first == null) L(if (p.elong < 15) "sky.tooClose" else "sky.below")
                else T("sky.window", Fmt.hm(Instant.ofEpochMilli(v.first), place.zone), Fmt.hm(Instant.ofEpochMilli(max(v.last ?: v.first, v.first + 600_000)), place.zone)) +
                    " · " + dirName(v.firstAz) + " · " + T("sky.altFmt", v.bestAlt.roundToInt(), Fmt.hm(Instant.ofEpochMilli(v.bestT), place.zone))
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(PLANET_COLORS[name] ?: Color.Gray, RoundedCornerShape(6.dp)))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(L("sky.planets.$name"), color = Theme.c.text, fontWeight = FontWeight.SemiBold)
                        Text(whenTxt, color = Theme.c.muted, fontSize = 13.sp)
                    }
                    Text((if (p.mag < 0) "−" else "") + String.format(Strings.numberLocale, "%.1f", kotlin.math.abs(p.mag)), color = Theme.c.muted, fontSize = 13.sp, style = Tnum)
                }
            }
        }
        SectionTitle(L("sky.eventsHeading"))
        Text(L("sky.eventsLead"), color = Theme.c.muted, fontSize = 13.sp)
        val evs = SkyEvent.upcoming(c)
        for (e in evs.take(shown)) {
            AppCard {
                Text(Fmt.shortDateTime(Instant.ofEpochMilli(e.t), place.zone), color = Theme.c.muted, fontSize = 13.sp)
                Text(e.title, color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                e.detail?.let { Text(it, color = Theme.c.muted, fontSize = 13.sp) }
            }
        }
        if (evs.size > shown) TextButton(onClick = { shown += 10 }) { Text(L("sky.evMore"), color = Theme.c.accent) }
        Text(L("sky.eventsNote"), color = Theme.c.muted, fontSize = 12.sp)
        Text(L("sky.note"), color = Theme.c.muted, fontSize = 12.sp)
        Text(L("sky.conSource"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

private fun Modifier.nightTintIf(on: Boolean): Modifier = this.nightTint(on)

/** Gökyüzü çizimi: ortası başucu, kenar ufuk; kuzey yukarıda, doğu solda */
@Composable
fun SkyChart(ms: Double, place: AppPlace, showCon: Boolean) {
    val c = LocalContext.current
    val cons = remember { Constellations.all(c) }
    val m = Nature.moon(ms.toLong())
    val moonBmp = remember(m.fraction, place.lat < 0) { MoonTexture.bitmap(c, m.fraction, place.lat < 0, 48) }
    val ink = Theme.c.muted
    Canvas(Modifier.fillMaxSize()) {
        val R = size.minDimension / 2 * 0.9f
        val cx = size.width / 2; val cy = size.height / 2
        fun proj(alt: Double, az: Double): Offset {
            val r = ((90 - alt) / 90 * R).toFloat(); val z = Math.toRadians(az)
            return Offset(cx - r * sin(z).toFloat(), cy - r * cos(z).toFloat())
        }
        val sa = sunAA(ms, place)
        val bg = when { sa.alt > 0 -> Color(0xFF7FB0DD); sa.alt > -6 -> Color(0xFF3A4E85); sa.alt > -12 -> Color(0xFF1D2A55); else -> Color(0xFF0D1730) }
        drawCircle(bg, R, Offset(cx, cy))
        for (alt in listOf(30.0, 60.0)) drawCircle(Color.White.copy(alpha = 0.18f), ((90 - alt) / 90 * R).toFloat(), Offset(cx, cy), style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))))
        val paint = android.graphics.Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx(); color = android.graphics.Color.WHITE }
        val nc = drawContext.canvas.nativeCanvas
        // Yönler
        val card = paint.apply { color = ink.hashCode(); textAlign = android.graphics.Paint.Align.CENTER; textSize = 13.sp.toPx(); isFakeBoldText = true }
        card.color = android.graphics.Color.argb(255, (ink.red * 255).toInt(), (ink.green * 255).toInt(), (ink.blue * 255).toInt())
        for ((i, az) in listOf(0.0, 90.0, 180.0, 270.0).withIndex()) {
            val o = proj(-6.0, az)
            nc.drawText(L("weather.compass.${i * 2}"), o.x, o.y + 5, card)
        }
        if (sa.alt < -3 && showCon) {
            val small = android.graphics.Paint().apply { isAntiAlias = true; textSize = 8.sp.toPx(); color = android.graphics.Color.argb(150, 170, 200, 255); textAlign = android.graphics.Paint.Align.CENTER }
            for (k in cons) {
                for (ln in k.lines) {
                    val path = Path(); var pen = false
                    for ((ra, dec) in ln) {
                        val a = Nature.altAz(ms, ra, dec, place.lat, place.lon)
                        if (a.alt < -2) { pen = false; continue }
                        val o = proj(a.alt, a.az)
                        if (pen) path.lineTo(o.x, o.y) else path.moveTo(o.x, o.y); pen = true
                    }
                    drawPath(path, Color(0x60A0C0FF), style = Stroke(1.2f))
                }
                if (k.rank <= 2) k.at?.let { (ra, dec) ->
                    val a = Nature.altAz(ms, ra, dec, place.lat, place.lon)
                    if (a.alt > 12) { val o = proj(a.alt, a.az); nc.drawText(Constellations.name(k), o.x, o.y, small) }
                }
            }
        }
        if (sa.alt < 0) {
            val sp = android.graphics.Paint().apply { isAntiAlias = true; textSize = 9.sp.toPx(); color = android.graphics.Color.argb(190, 255, 255, 255) }
            for ((name, rd, mag) in Sky.STARS) {
                val a = Nature.altAz(ms, rd.first, rd.second, place.lat, place.lon)
                if (a.alt <= 0) continue
                val o = proj(a.alt, a.az)
                drawCircle(Color.White.copy(alpha = 0.9f), max(1.2f, (3.4 - mag * 0.9).toFloat()), o)
                if (mag < 0.9) nc.drawText(name, o.x + 5, o.y + 3, sp)
            }
        }
        val label = android.graphics.Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx(); color = android.graphics.Color.WHITE; isFakeBoldText = true }
        val ma = moonAA(ms, place)
        if (ma.alt > -0.5) {
            val o = proj(ma.alt, ma.az)
            if (moonBmp != null) drawImage(moonBmp.asImageBitmap(), Offset(o.x - moonBmp.width / 2f, o.y - moonBmp.height / 2f))
            else drawCircle(Color(0xFFE9EDF2), 14f, o)
            nc.drawText(L("sky.moon"), o.x + 28, o.y + 4, label)
        }
        if (sa.alt > -0.833) { val o = proj(sa.alt, sa.az); drawCircle(Color(0xFFFFD34D), 16f, o); nc.drawText(L("sky.sun"), o.x + 20, o.y + 4, label) }
        for (name in Sky.PLANETS) {
            val p = Sky.planet(name, ms)
            val a = Nature.altAz(ms, p.ra, p.dec, place.lat, place.lon)
            if (a.alt <= 0) continue
            val o = proj(a.alt, a.az)
            drawCircle(PLANET_COLORS[name] ?: Color.White, max(5f, (10 - p.mag * 1.2).toFloat()), o)
            nc.drawText(L("sky.planets.$name"), o.x + 12, o.y - 8, label)
        }
    }
}

/** "Bu gece" özeti (2. analiz #155): tam karanlık, Ay'ın etkisi, görülebilen gezegenler, öneriler */
@Composable
private fun TonightCard(place: AppPlace, night: Pair<Long, Long>?, base: Long) {
    val ref = night?.first?.plus(3_600_000) ?: base
    val dark = remember(place.id, ref / 3_600_000) { darkWindow(ref, place, -18.0) }
    val m = Nature.moon(dark?.first ?: ref)
    val up = ArrayList<Long>()
    if (dark != null) { var t = dark.first; while (t <= dark.second) { if (moonAA(t.toDouble(), place).alt > 0) up.add(t); t += 600_000 } }
    val impact = if (dark != null) up.size * 600_000.0 / max(1L, dark.second - dark.first) * m.illumination else m.illumination
    val vis = Sky.PLANETS.map { it to visibility(it, night, place) }.filter { it.second.first != null }
    val score = when { dark == null || impact > 0.6 -> "poor"; impact < 0.25 && dark.second - dark.first >= 3 * 3_600_000 -> "good"; else -> "fair" }
    SectionTitle(L("sky.tonightHeading"))
    AppCard {
        Text(L("sky.score.$score"), color = when (score) { "good" -> Theme.c.ok; "poor" -> Theme.c.err; else -> Theme.c.text }, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Row2(L("sky.darkLabel"), dark?.let { T("sky.window", Fmt.hm(Instant.ofEpochMilli(it.first), place.zone), Fmt.hm(Instant.ofEpochMilli(it.second), place.zone)) } ?: L("sky.darkNone"))
        val il = Fmt.percent((m.illumination * 100).roundToInt())
        Row2(L("sky.moon"), if (up.isNotEmpty()) T("sky.moonUpDark", il, Fmt.hm(Instant.ofEpochMilli(up.first()), place.zone), Fmt.hm(Instant.ofEpochMilli(up.last()), place.zone)) else T("sky.moonDownDark", il))
        Row2(L("sky.planetsLabel"), if (vis.isEmpty()) L("sky.planetsNone") else vis.joinToString(", ") { L("sky.planets.${it.first}") })
        if (m.illumination > 0.5 && up.isNotEmpty()) Text("• " + L("sky.tipMoon"), color = Theme.c.text, fontSize = 13.sp)
        else if (dark != null && up.isEmpty()) Text("• " + L("sky.tipDark"), color = Theme.c.text, fontSize = 13.sp)
        for ((name, v) in vis.sortedByDescending { it.second.bestAlt }.take(3))
            Text("• " + T("sky.tipBest", L("sky.planets.$name"), Fmt.hm(Instant.ofEpochMilli(v.bestT), place.zone), v.bestAlt.roundToInt()), color = Theme.c.text, fontSize = 13.sp)
        SkyGraph(place, night, dark)
        Text(L("sky.graphCaption"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

@Composable
private fun SkyGraph(place: AppPlace, night: Pair<Long, Long>?, dark: Pair<Long, Long>?) {
    val t0 = (night?.first ?: System.currentTimeMillis()) - 3_600_000
    val t1 = (night?.second ?: (t0 + 12 * 3_600_000)) + 3_600_000
    val grid = Theme.c.border
    val muted = Theme.c.muted
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val x = { t: Long -> ((t - t0).toFloat() / (t1 - t0)) * size.width }
        val y = { a: Double -> ((1 - max(0.0, a) / 90) * size.height).toFloat() }
        if (dark != null) drawRect(Color(0x302040A0), Offset(x(max(t0, dark.first)), 0f), androidx.compose.ui.geometry.Size(x(min(t1, dark.second)) - x(max(t0, dark.first)), size.height))
        for (a in listOf(30.0, 60.0)) drawLine(grid, Offset(0f, y(a)), Offset(size.width, y(a)), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
        val series = Sky.PLANETS.map { n -> (PLANET_COLORS[n] ?: Color.White) to { t: Long -> planetAA(n, t.toDouble(), place).alt } } +
            listOf(Color(0xFF9AA4B0) to { t: Long -> moonAA(t.toDouble(), place).alt })
        for ((col, f) in series) {
            val path = Path(); var pen = false; var t = t0
            while (t <= t1) {
                val a = f(t)
                if (a <= 0) pen = false else { if (pen) path.lineTo(x(t), y(a)) else path.moveTo(x(t), y(a)); pen = true }
                t += 900_000
            }
            drawPath(path, col, style = Stroke(4f))
        }
        val p = android.graphics.Paint().apply { isAntiAlias = true; textSize = 10.sp.toPx(); color = android.graphics.Color.argb(255, (muted.red * 255).toInt(), (muted.green * 255).toInt(), (muted.blue * 255).toInt()) }
        var h = (t0 / 3_600_000 + 1) * 3_600_000
        while (h < t1) { drawContext.canvas.nativeCanvas.drawText(Fmt.hm(Instant.ofEpochMilli(h), place.zone), x(h) - 14, size.height - 2, p); h += 3 * 3_600_000 }
    }
}

/* ---------------- Güneş yolu (2. analiz #158) ---------------- */
@Composable
fun SunPathScreen() {
    val c = LocalContext.current
    val place = AppPlace.current()
    val compass = remember(place.lat, place.lon) { CompassState(c, place.lat, place.lon) }
    DisposableEffect(compass) { compass.start(); onDispose { compass.stop() } }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); tick++ } }
    val now = System.currentTimeMillis() + tick * 0
    val today = place.today()
    val year = today.year
    val nowAA = sunAA(now.toDouble(), place)
    fun dayPath(d: LocalDate): List<Pair<Long, Nature.AltAz>> {
        val start = d.atStartOfDay(place.zone).toInstant().toEpochMilli()
        return (0..288).map { i -> val t = start + i * 300_000L; t to sunAA(t.toDouble(), place) }
    }
    val paths = remember(place.id, today) { listOf(dayPath(today), dayPath(LocalDate.of(year, 6, 21)), dayPath(LocalDate.of(year, 12, 21))) }
    fun crossing(list: List<Pair<Long, Nature.AltAz>>, rise: Boolean): Nature.AltAz? {
        for (i in 1 until list.size) {
            val a = list[i - 1].second.alt + 0.833; val b = list[i].second.alt + 0.833
            if (rise && a < 0 && b >= 0) return list[i].second
            if (!rise && a >= 0 && b < 0) return list[i - 1].second
        }
        return null
    }
    val todayPath = paths[0]
    val noon = todayPath.maxBy { it.second.alt }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PlacePicker()
        val heading = compass.heading
        val cols = listOf(Color(0xFFF2B705), Color(0xFFE8714C), Color(0xFF4A7BD0))
        val ink = Theme.c.muted
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Canvas(Modifier.fillMaxSize()) {
                val R = size.minDimension / 2 * 0.86f
                val ctr = Offset(size.width / 2, size.height / 2)
                rotate(-(heading ?: 0.0).toFloat(), ctr) {
                    drawCircle(ink.copy(alpha = 0.08f), R, ctr)
                    drawCircle(ink.copy(alpha = 0.5f), R, ctr, style = Stroke(2f))
                    for (alt in listOf(30.0, 60.0)) drawCircle(ink.copy(alpha = 0.3f), ((90 - alt) / 90 * R).toFloat(), ctr, style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))))
                    fun proj(a: Nature.AltAz): Offset { val r = ((90 - a.alt) / 90 * R).toFloat(); val z = Math.toRadians(a.az); return Offset(ctr.x + r * sin(z).toFloat(), ctr.y - r * cos(z).toFloat()) }
                    for ((i, pth) in paths.withIndex().reversed()) {
                        val path = Path(); var pen = false
                        for ((_, a) in pth) { if (a.alt < 0) { pen = false; continue }; val o = proj(a); if (pen) path.lineTo(o.x, o.y) else path.moveTo(o.x, o.y); pen = true }
                        drawPath(path, cols[i], style = Stroke(if (i == 0) 6f else 3f))
                    }
                    val p = android.graphics.Paint().apply { isAntiAlias = true; textSize = 14.sp.toPx(); isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER; color = android.graphics.Color.argb(255, (ink.red * 255).toInt(), (ink.green * 255).toInt(), (ink.blue * 255).toInt()) }
                    for ((i, az) in listOf(0.0, 90.0, 180.0, 270.0).withIndex()) {
                        val o = proj(Nature.AltAz(-8.0, az)); drawContext.canvas.nativeCanvas.drawText(L("weather.compass.${i * 2}"), o.x, o.y + 5, p)
                    }
                    if (nowAA.alt > 0) drawCircle(Color(0xFFFFC21A), 16f, proj(nowAA))
                }
            }
        }
        Text(L(if (heading != null) "sunpath.compass" else "qibla.noCompass"), color = Theme.c.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        AppCard {
            Text(if (nowAA.alt > 0) T("sunpath.now", nowAA.alt.roundToInt(), dirName(nowAA.az), nowAA.az.roundToInt()) else L("sunpath.below"),
                color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            crossing(todayPath, true)?.let { Row2(L("sunpath.rise"), "${dirName(it.az)} · ${it.az.roundToInt()}°") }
            crossing(todayPath, false)?.let { Row2(L("sunpath.set"), "${dirName(it.az)} · ${it.az.roundToInt()}°") }
            Row2(L("sunpath.noon"), "${noon.second.alt.roundToInt()}° · ${Fmt.hm(Instant.ofEpochMilli(noon.first), place.zone)}")
            Text(if (nowAA.alt > 0.5) T("sunpath.shadow", String.format(Strings.numberLocale, "%.1f", min(99.0, 1 / tan(Math.toRadians(nowAA.alt))))) else "", color = Theme.c.text, fontSize = 14.sp)
        }
        Text(L("sunpath.legend"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/* ---------------- Tutulma ayrıntısı (2. analiz #159) ---------------- */
@Composable
fun EclipseDetail(e: Eclipse, place: AppPlace) {
    if (e.kind == "lunar") {
        val L0 = remember(e.ut) { Sky.lunarContacts(e.ut.toDouble()) }
        val muted = Theme.c.muted
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val path = L0.path.filter { p -> (L0.p1 == null || p.t >= L0.p1!! - 600_000) && (L0.p4 == null || p.t <= L0.p4!! + 600_000) }
            var ext = L0.rP
            for (p in path) ext = max(ext, max(kotlin.math.abs(p.x), kotlin.math.abs(p.y)) + L0.rM)
            val sc = (size.minDimension / 2 * 0.95f / ext).toFloat()
            val ctr = Offset(size.width / 2, size.height / 2)
            drawCircle(Color(0x30787890), (L0.rP * sc).toFloat(), ctr)
            drawCircle(Color(0x5A96281E), (L0.rU * sc).toFloat(), ctr)
            val pp = Path()
            path.forEachIndexed { i, p -> val o = Offset(ctr.x - (p.x * sc).toFloat(), ctr.y - (p.y * sc).toFloat()); if (i == 0) pp.moveTo(o.x, o.y) else pp.lineTo(o.x, o.y) }
            drawPath(pp, muted, style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))))
            for ((k, t) in listOf("p1" to L0.p1, "u1" to L0.u1, "max" to L0.max, "u4" to L0.u4, "p4" to L0.p4)) {
                val tt = t ?: continue
                val q = path.minByOrNull { kotlin.math.abs(it.t - tt) } ?: continue
                val o = Offset(ctr.x - (q.x * sc).toFloat(), ctr.y - (q.y * sc).toFloat())
                drawCircle(if (k == "max") Color(0xC0BE4628) else Color(0x8CE9EDF2), (L0.rM * sc).toFloat(), o)
                drawCircle(muted, (L0.rM * sc).toFloat(), o, style = Stroke(1f))
            }
        }
        for ((k, t) in listOf("p1" to L0.p1, "u1" to L0.u1, "u2" to L0.u2, "max" to L0.max, "u3" to L0.u3, "u4" to L0.u4, "p4" to L0.p4)) {
            val tt = t ?: continue
            val up = Nature.moonAltitude(tt.toDouble(), place.lat, place.lon) > 0
            Row(Modifier.fillMaxWidth()) {
                Text(L("nature.eclContacts.$k"), Modifier.weight(1f), color = if (up) Theme.c.text else Theme.c.muted.copy(alpha = 0.6f), fontSize = 14.sp)
                Text(Fmt.hm(Instant.ofEpochMilli((tt + 30_000) / 60_000 * 60_000), place.zone), color = if (up) Theme.c.text else Theme.c.muted.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, style = Tnum)
            }
        }
        if (L0.umag > 0) Text(T("nature.eclMag", String.format(Strings.numberLocale, "%.2f", L0.umag)), color = Theme.c.muted, fontSize = 13.sp)
    } else {
        val r = remember(e.ut, place.id) { Sky.solarLocal(e.ut.toDouble(), place.lat, place.lon) }
        fun hm(t: Long?) = t?.let { Fmt.hm(Instant.ofEpochMilli((it + 30_000) / 60_000 * 60_000), place.zone) } ?: "–"
        val txt = when {
            r == null -> T("nature.eclLocalNone", place.name)
            r.altMax < 0 -> T("nature.eclLocalLow", place.name)
            r.central == "total" -> T("nature.eclLocalTotal", place.name, hm(r.c1), hm(r.c4), hm(r.max))
            r.central == "annular" -> T("nature.eclLocalAnnular", place.name, hm(r.c1), hm(r.c4), hm(r.max))
            else -> T("nature.eclLocal", place.name, Fmt.percent((r.obsc * 100).roundToInt()), hm(r.c1), hm(r.c4), hm(r.max))
        }
        Text(txt, color = if (r != null && r.altMax >= 0) Theme.c.accent else Theme.c.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (r != null && r.altMax >= 0) {
            // Örtülme çizimi: en büyük anda Güneş diski ve Ay
            val d = Sky.solarDisks(r.max.toDouble(), place.lat, place.lon)
            Canvas(Modifier.size(120.dp).padding(8.dp)) {
                val sc = size.minDimension / 2 / (d.rs * 1.6).toFloat()
                val ctr = Offset(size.width / 2, size.height / 2)
                drawCircle(Color(0xFFFFC21A), (d.rs * sc).toFloat(), ctr)
                drawCircle(Color(0xFF222630), (d.rm * sc).toFloat(), Offset(ctr.x + (d.d * sc).toFloat() * 0.7f, ctr.y - (d.d * sc).toFloat() * 0.7f))
            }
        }
        EclipseMap(e, place)
    }
    Text(L("nature.eclApprox"), color = Theme.c.muted, fontSize = 12.sp)
}

/** Tutulma haritası (sitedeki eclipse-map.js): 3° ızgarada yerel en büyük örtülme, eşdikdörtgen Dünya dokusu üstünde */
@Composable
private fun EclipseMap(e: Eclipse, place: AppPlace) {
    val c = LocalContext.current
    var cells by remember(e.ut) { mutableStateOf<List<FloatArray>?>(null) }
    LaunchedEffect(e.ut) {
        cells = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val out = ArrayList<FloatArray>()
            val steps = (0..84).map { e.ut - 210 * 60_000L + it * 5 * 60_000L }
            var lat = -88.5
            while (lat < 90) {
                var lon = -178.5
                while (lon < 180) {
                    var best = 0.0
                    for (t in steps) { val x = Sky.solarDisks(t.toDouble(), lat, lon); if (x.alt > 0 && x.d < x.rs + x.rm) best = max(best, (x.rs + x.rm - x.d) / (2 * x.rs)) }
                    if (best > 0) out.add(floatArrayOf(lat.toFloat(), lon.toFloat(), best.toFloat()))
                    lon += 3.0
                }
                lat += 3.0
            }
            out
        }
    }
    val earth = remember { runCatching { c.assets.open("earth.jpg").use { BitmapFactory.decodeStream(it) } }.getOrNull() }
    SectionTitle(L("nature.eclMapHeading"))
    Box(Modifier.fillMaxWidth().aspectRatio(2f)) {
        if (earth != null) Image(earth.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillBounds)
        Canvas(Modifier.fillMaxSize()) {
            val list = cells ?: return@Canvas
            val cw = size.width / 120; val ch = size.height / 60
            for (q in list) {
                val m = q[2]
                val col = when { m > 0.75 -> Color(0xFFC2410C); m > 0.5 -> Color(0xFFEA7A24); m > 0.25 -> Color(0xFFF4A65A); else -> Color(0xFFF9CF9C) }
                drawRect(col.copy(alpha = 0.55f), Offset((q[1] + 180 - 1.5f) / 360 * size.width, (90 - q[0] - 1.5f) / 180 * size.height), androidx.compose.ui.geometry.Size(cw + 1, ch + 1))
            }
            drawCircle(Color.Black, 5f, Offset(((place.lon + 180) / 360 * size.width).toFloat(), ((90 - place.lat) / 180 * size.height).toFloat()))
        }
    }
    Text(L("nature.eclMapSolar"), color = Theme.c.muted, fontSize = 12.sp)
}

/* ---------------- Çocuklar için Ay (2. analiz #160) ---------------- */
@Composable
fun KidsMoonCard() {
    var day by rememberSaveable { mutableFloatStateOf(7.4f) }
    val frac = day / 29.53
    SectionTitle(L("kidsmoon.title"))
    AppCard {
        Text(L("kidsmoon.text"), color = Theme.c.text, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(L("kidsmoon.space"), color = Theme.c.muted, fontSize = 12.sp)
                val muted = Theme.c.muted
                Canvas(Modifier.size(150.dp)) {
                    val ctr = Offset(size.width / 2, size.height / 2)
                    val R = size.minDimension / 2 * 0.72f
                    // Güneş ışığı soldan gelir (sağa doğru oklar)
                    for (i in 0..3) { val y = size.height * (0.2f + i * 0.2f); drawLine(Color(0xFFFFC21A), Offset(0f, y), Offset(size.width * 0.12f, y), 3f) }
                    drawCircle(muted.copy(alpha = 0.4f), R, ctr, style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f))))
                    drawCircle(Color(0xFF3F7FD0), 16f, ctr)
                    drawArc(Color(0xFF1B2A44), -90f, 180f, true, Offset(ctr.x - 16f, ctr.y - 16f), androidx.compose.ui.geometry.Size(32f, 32f))
                    // Ay: yeni ayda Güneş yönünde (solda), dolunayda sağda; saat yönünün tersine döner
                    val ang = PI + 2 * PI * frac
                    val mo = Offset(ctr.x + (R * cos(ang)).toFloat(), ctr.y - (R * sin(ang)).toFloat())
                    drawCircle(Color(0xFFE9EDF2), 11f, mo)
                    drawArc(Color(0xFF2A2F3A), -90f, 180f, true, Offset(mo.x - 11f, mo.y - 11f), androidx.compose.ui.geometry.Size(22f, 22f))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(L("kidsmoon.earth"), color = Theme.c.muted, fontSize = 12.sp)
                MoonImage(frac.toDouble(), 110.dp, south = false)
            }
        }
        Slider(day, { day = it }, valueRange = 0f..29.5f, colors = SliderDefaults.colors(thumbColor = Theme.c.accent, activeTrackColor = Theme.c.accent))
        val idx = ((frac * 8 + 0.5).toInt()) % 8
        Text(T("kidsmoon.day", day.roundToInt()) + " · " + L("nature.phases.${MOON_PHASE_KEYS[idx]}"), color = Theme.c.text, fontWeight = FontWeight.SemiBold)
        Text(L("kidsmoon.sunlight") + " ⟶", color = Color(0xFFB8860B), fontSize = 12.sp)
    }
    KidsMoonQuiz()
}

/** Beş soruluk Ay bilgi yarışması (cevaplar cihazda, kaydedilmez) */
@Composable
private fun KidsMoonQuiz() {
    val answers = listOf(1, 1, 1, 0, 0)
    var q by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableIntStateOf(-1) }
    AppCard {
        Text(L("kidsmoon.quiz"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (q >= answers.size) {
            Text(T("kidsmoon.score", score, answers.size), color = Theme.c.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { q = 0; score = 0; picked = -1 }) { Text(L("kidsmoon.again"), color = Theme.c.accent) }
        } else {
            Text("${q + 1}/${answers.size}  " + L("kidsmoon.q${q + 1}"), color = Theme.c.text, fontSize = 15.sp)
            L("kidsmoon.q${q + 1}a").split("|").forEachIndexed { i, opt ->
                val right = i == answers[q]
                val solved = picked == answers[q]
                val bg = when { picked < 0 -> Theme.c.surface2; i == picked && right -> Theme.c.ok.copy(alpha = 0.18f); i == picked -> Theme.c.err.copy(alpha = 0.15f); else -> Theme.c.surface2 }
                Text(opt, Modifier.fillMaxWidth().padding(vertical = 3.dp).background(bg, RoundedCornerShape(10.dp)).clickable(enabled = !solved) {
                    if (picked < 0 && right) score++ // yalnız ilk denemede doğru olan sayılır
                    picked = i
                }.padding(12.dp), color = Theme.c.text)
            }
            if (picked >= 0) {
                Text(L(if (picked == answers[q]) "kidsmoon.correct" else "kidsmoon.wrong"), color = if (picked == answers[q]) Theme.c.ok else Theme.c.err, fontWeight = FontWeight.SemiBold)
                if (picked == answers[q]) TextButton(onClick = { q++; picked = -1 }) { Text("→", color = Theme.c.accent, fontSize = 18.sp) }
            }
        }
    }
}
