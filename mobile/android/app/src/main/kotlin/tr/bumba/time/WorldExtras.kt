package tr.bumba.time

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/*
 * Dünya saatleri ekleri (2. analiz #129, #130, #131; 4 Kasım 2026):
 *  - Şehir başına hava: Open-Meteo'ya listedeki yerler için tek istek (koordinat 2 ondalık), saatte bir, cihazda
 *    "worldWx" anahtarında; konumu bilinmeyen (yalnız saat dilimi) girişlerde hava yok.
 *  - Gece gölgeli dünya haritası (eşdikdörtgen Natural Earth dokusu, Güneş'in alt noktasından terminatör).
 *  - 24 saatlik çok ibreli kadran: üst yarı gündüz (12.00 tepede), alt yarı gece; her şehir bir ibre; kadranı
 *    parmakla döndürmek saati kaydırır (360° = 24 saat).
 */

object WorldWeather {
    data class Entry(val celsius: Double, val code: Int, val isDay: Boolean)

    private const val KEY = "worldWx"
    private const val TTL = 3_600_000L
    private var at = 0L
    private var sig = ""
    private var items: Map<String, Entry> = emptyMap()
    private var loaded = false
    private var loading = false

    private fun restore() {
        if (loaded) return
        loaded = true
        runCatching {
            val o = JSONObject(Prefs.store.getString(KEY, null) ?: return)
            at = o.getLong("at")
            sig = o.optString("s")
            val c = o.getJSONObject("c")
            items = c.keys().asSequence().associateWith { k ->
                val a = c.getJSONArray(k)
                Entry(a.getDouble(0), a.getInt(1), a.getInt(2) == 1)
            }
        }
    }

    fun cached(key: String): Entry? {
        restore()
        return if (System.currentTimeMillis() - at < 6 * TTL) items[key] else null
    }

    private fun signature(list: List<WorldEntry>) = list.filter { it.lat != null }.joinToString("|") { it.key }

    /** Liste değiştiyse ya da süresi geçtiyse yeniden sorar; yeni veri geldiyse true */
    suspend fun refresh(list: List<WorldEntry>): Boolean {
        restore()
        val geo = list.filter { it.lat != null && it.lon != null }.take(40)
        if (loading || geo.isEmpty()) return false
        val s = signature(geo)
        if (s == sig && System.currentTimeMillis() - at < TTL) return false
        loading = true
        try {
            val fresh = withContext(Dispatchers.IO) { runCatching { fetch(geo) }.getOrNull() } ?: return false
            items = fresh
            at = System.currentTimeMillis()
            sig = s
            val c = JSONObject()
            for ((k, e) in fresh) c.put(k, JSONArray(listOf(e.celsius, e.code, if (e.isDay) 1 else 0)))
            Prefs.store.edit().putString(KEY, JSONObject().put("at", at).put("s", sig).put("c", c).toString()).apply()
            return true
        } finally {
            loading = false
        }
    }

    private fun fetch(list: List<WorldEntry>): Map<String, Entry> {
        fun f(v: Double) = String.format(Locale.ROOT, "%.2f", v)
        val url = "https://api.open-meteo.com/v1/forecast?latitude=" + list.joinToString(",") { f(it.lat!!) } +
            "&longitude=" + list.joinToString(",") { f(it.lon!!) } + "&current=temperature_2m,weather_code,is_day&timezone=GMT"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        val body = try {
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
        val t = body.trimStart()
        val arr = if (t.startsWith("[")) JSONArray(t) else JSONArray().put(JSONObject(t))
        val out = HashMap<String, Entry>()
        for (i in 0 until minOf(arr.length(), list.size)) {
            val cur = arr.getJSONObject(i).optJSONObject("current") ?: continue
            out[list[i].key] = Entry(cur.getDouble("temperature_2m"), cur.optInt("weather_code", 2), cur.optInt("is_day", 1) == 1)
        }
        return out
    }

    /** Testler için: ağsız örnek veri */
    fun seed(map: Map<String, Entry>) {
        loaded = true
        items = map
        at = System.currentTimeMillis()
    }
}

/** Haritada ve widget'ta kullanılan küçültülmüş doku (720×360) */
object WorldMapTexture {
    @Volatile private var bmp: android.graphics.Bitmap? = null

    @Synchronized
    fun bitmap(c: Context): android.graphics.Bitmap? {
        bmp?.let { return it }
        return runCatching {
            val o = BitmapFactory.Options().apply { inSampleSize = 4 }
            c.assets.open("earth.jpg").use { BitmapFactory.decodeStream(it, null, o) }
        }.getOrNull()?.also { bmp = it }
    }
}

/** Gece bölgesinin eşdikdörtgen haritadaki çokgeni (0..w, 0..h); Güneş'in alt noktasına göre */
fun nightPolygon(ms: Long, w: Float, h: Float): List<Pair<Float, Float>> {
    val s = GlobeSky.sun(ms)
    val dec = if (kotlin.math.abs(s.lat) < 0.1) 0.1 else s.lat
    val r = PI / 180
    val pts = ArrayList<Pair<Float, Float>>()
    var lon = -180.0
    while (lon <= 180.0) {
        // Terminatör enlemi: tan φ = −cos(λ − λs) / tan δ
        val lat = atan(-cos((lon - s.lon) * r) / tan(dec * r)) / r
        pts += ((lon + 180) / 360 * w).toFloat() to ((90 - lat) / 180 * h).toFloat()
        lon += 3.0
    }
    // Kuzey yazında gece güneyde (alt kenara kapat), kışın kuzeyde
    val closeY = if (dec > 0) h else 0f
    pts += w to closeY
    pts += 0f to closeY
    return pts
}

@Composable
fun WorldMapCard(entries: List<WorldEntry>, now: Instant, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val tex: ImageBitmap? = remember { WorldMapTexture.bitmap(ctx)?.asImageBitmap() }
    val density = LocalDensity.current
    val ink = Theme.c.text
    Canvas(modifier.fillMaxWidth().aspectRatio(2f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF0E1A2E))) {
        drawWorldMap(tex, entries, now, density.density, ink)
    }
}

fun DrawScope.drawWorldMap(tex: ImageBitmap?, entries: List<WorldEntry>, now: Instant, d: Float, ink: Color, labels: Boolean = true) {
    val w = size.width
    val h = size.height
    if (tex != null) drawImage(tex, srcOffset = IntOffset.Zero, srcSize = IntSize(tex.width, tex.height), dstSize = IntSize(w.toInt(), h.toInt()))
    // Gece gölgesi
    val poly = nightPolygon(now.toEpochMilli(), w, h)
    val path = Path().apply {
        moveTo(poly[0].first, poly[0].second)
        for (p in poly.drop(1)) lineTo(p.first, p.second)
        close()
    }
    drawPath(path, Color(0xCC050A1A))
    // Güneş'in alt noktası
    val sun = GlobeSky.sun(now.toEpochMilli())
    val sx = ((sun.lon + 180) / 360 * w).toFloat()
    val sy = ((90 - sun.lat) / 180 * h).toFloat()
    drawCircle(Color(0xFFFFC94D), radius = 6f * d, center = Offset(sx, sy))
    drawCircle(Color(0x55FFC94D), radius = 12f * d, center = Offset(sx, sy))
    // Şehirler
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * d
        color = android.graphics.Color.WHITE
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(3f * d, 0f, 0f, android.graphics.Color.BLACK)
    }
    val boxes = ArrayList<android.graphics.RectF>()
    for (e in entries) {
        val la = e.lat ?: continue
        val lo = e.lon ?: continue
        val x = ((lo + 180) / 360 * w).toFloat()
        val y = ((90 - la) / 180 * h).toFloat()
        val day = e.light(now) == "day"
        drawCircle(if (day) Color(0xFFFFE08A) else Color(0xFF9DB7FF), radius = 3.5f * d, center = Offset(x, y))
        drawCircle(Color.Black.copy(alpha = 0.6f), radius = 3.5f * d, center = Offset(x, y), style = Stroke(1f * d))
        if (!labels) continue
        val text = e.displayName.take(14) + " " + Fmt.time(now.atZone(e.zone))
        val tw = paint.measureText(text)
        var lx = x + 6 * d
        if (lx + tw > w - 2) lx = x - 6 * d - tw
        val box = android.graphics.RectF(lx, y - 7 * d, lx + tw, y + 5 * d)
        if (boxes.any { android.graphics.RectF.intersects(it, box) }) continue
        boxes += box
        drawContext.canvas.nativeCanvas.drawText(text, lx, y + 4 * d, paint)
    }
}

/** 24 saatlik kadran: renkli ibreler (uygulama renginden başlayan sabit palet) */
private val HAND_COLORS = listOf(0xFFE4572E, 0xFF2E86AB, 0xFF3BB273, 0xFFF4B400, 0xFF8E44AD, 0xFFE91E63, 0xFF00ACC1, 0xFF795548)

@Composable
fun WorldDial(entries: List<WorldEntry>, now: Instant, onShift: (Long) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val cur by rememberUpdatedState(now)
    val shiftCb by rememberUpdatedState(onShift)
    val ink = Theme.c.text
    val muted = Theme.c.muted
    val accent = Theme.c.accent
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f).pointerInput(Unit) {
                var last = 0.0
                detectDragGestures(
                    onDragStart = { p -> last = atan2((p.y - size.height / 2f).toDouble(), (p.x - size.width / 2f).toDouble()) },
                ) { change, _ ->
                    val p = change.position
                    val a = atan2((p.y - size.height / 2f).toDouble(), (p.x - size.width / 2f).toDouble())
                    var da = a - last
                    if (da > PI) da -= 2 * PI
                    if (da < -PI) da += 2 * PI
                    last = a
                    // Saat yönünde döndürmek ileri götürür: 2π = 24 saat
                    val minutes = da / (2 * PI) * 1440
                    shiftCb(cur.toEpochMilli() + (minutes * 60_000).toLong())
                }
            },
        ) {
            val d = density.density
            val c = Offset(size.width / 2, size.height / 2)
            val r = min(size.width, size.height) / 2 - 4 * d
            // Üst yarı gündüz, alt yarı gece
            drawArc(Color(0xFFD5E8FF), 180f, 180f, true, topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r))
            drawArc(Color(0xFF1B2440), 0f, 180f, true, topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r))
            drawCircle(ink.copy(alpha = 0.25f), r, c, style = Stroke(1.5f * d))
            val tp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 11f * d
                textAlign = android.graphics.Paint.Align.CENTER
            }
            // Saat çizgileri ve rakamlar (0 altta, 12 üstte)
            for (hr in 0 until 24) {
                val ang = (hr / 24.0 * 360 + 90) * PI / 180 // 0 saat altta
                val cs = cos(ang).toFloat()
                val sn = sin(ang).toFloat()
                val dayHalf = hr in 6..18
                val major = hr % 3 == 0
                val col = if (dayHalf) Color(0xFF14233A) else Color(0xFFE8ECF8)
                drawLine(col.copy(alpha = 0.7f), Offset(c.x + cs * r * 0.9f, c.y + sn * r * 0.9f), Offset(c.x + cs * r * (if (major) 0.8f else 0.86f), c.y + sn * r * (if (major) 0.8f else 0.86f)), strokeWidth = (if (major) 2f else 1f) * d)
                if (major) {
                    tp.color = android.graphics.Color.argb(220, (col.red * 255).toInt(), (col.green * 255).toInt(), (col.blue * 255).toInt())
                    // 6 ve 18 gündüz/gece sınırında: yazı sınırın üstüne alınır
                    val dy = if (hr == 6 || hr == 18) -8 * d else 4 * d
                    if (hr == 6 || hr == 18) tp.color = android.graphics.Color.argb(220, 20, 35, 58)
                    drawContext.canvas.nativeCanvas.drawText(hr.toString(), c.x + cs * r * 0.7f, c.y + sn * r * 0.7f + dy, tp)
                }
            }
            // İbreler
            val lp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 10.5f * d
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(3f * d, 0f, 0f, android.graphics.Color.BLACK)
                color = android.graphics.Color.WHITE
            }
            entries.take(8).forEachIndexed { i, e ->
                val t = cur.atZone(e.zone)
                val hrs = t.hour + t.minute / 60.0
                val ang = (hrs / 24.0 * 360 + 90) * PI / 180
                val cs = cos(ang).toFloat()
                val sn = sin(ang).toFloat()
                val col = Color(HAND_COLORS[i % HAND_COLORS.size])
                val len = r * (0.62f - (i % 4) * 0.08f)
                drawLine(col, c, Offset(c.x + cs * len, c.y + sn * len), strokeWidth = 3.5f * d, cap = StrokeCap.Round)
                drawCircle(col, 4.5f * d, Offset(c.x + cs * len, c.y + sn * len))
                val lx = c.x + cs * (len + 14 * d)
                val ly = c.y + sn * (len + 14 * d)
                drawContext.canvas.nativeCanvas.drawText(e.displayName.take(10), lx, ly + 4 * d, lp)
            }
            drawCircle(accent, 6f * d, c)
        }
        Text(L("world.dialHint"), color = muted, fontSize = 12.sp)
        // İbre renkleri ve saatler
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.take(8).forEachIndexed { i, e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.padding(end = 4.dp).size(10.dp)) { drawCircle(Color(HAND_COLORS[i % HAND_COLORS.size])) }
                    Text(e.displayName + " " + Fmt.time(cur.atZone(e.zone)), color = ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Küre / harita / kadran seçimi (`world.view`) */
@Composable
fun WorldViewSwitch(view: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        for (k in listOf("globe", "map", "dial")) {
            val on = k == view
            Text(
                L("world.view.$k"),
                Modifier.clip(RoundedCornerShape(50)).background(if (on) Theme.c.accent else Theme.c.surface)
                    .clickable { onChange(k) }.padding(horizontal = 14.dp, vertical = 8.dp),
                color = if (on) (if (Theme.c.dark) Color.Black else Color.White) else Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            )
        }
    }
}

/** Liste değişince şehir havasını tazeler; sürüm sayacı döner (yeniden çizim için) */
@Composable
fun rememberWorldWeather(entries: List<WorldEntry>): Int {
    var version by remember { mutableIntStateOf(0) }
    val keys = entries.joinToString("|") { it.key }
    LaunchedEffect(keys) {
        if (WorldWeather.refresh(entries)) version++
    }
    return version
}
