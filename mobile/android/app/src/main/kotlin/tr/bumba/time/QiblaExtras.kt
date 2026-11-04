package tr.bumba.time

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Kıble ekleri (2. analiz #67; 4 Kasım 2026): pusula görünümü seçimi (5 tasarım, `qibla.style`), haritada Kâbe'ye
 * büyük daire çizgisi (kendi Natural Earth dokumuzla, harita servisi yok) ve favori camiler (`qibla.favs`, yalnız
 * telefonda). Kamera görünümü yalnız iPhone'da.
 */
object QiblaPrefs {
    const val STYLE = "qibla.style"
    const val FAVS = "qibla.favs"
    val STYLES = listOf("classic", "minimal", "gold", "night", "green")
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206

    var style by mutableStateOf(load())
        private set

    private fun load(): String = Prefs.store.getString(STYLE, "classic")?.takeIf { it in STYLES } ?: "classic"

    fun saveStyle(s: String) {
        style = s
        Prefs.store.edit().putString(STYLE, s).apply()
    }

    /** Kadran renkleri: zemin, kenar, çentik, iğne (hizalı değilken), kuzey harfi */
    data class Look(val face: Color, val ring: Color, val tick: Color, val needle: Color, val north: Color, val faceInk: Color?)

    @androidx.compose.runtime.Composable
    fun look(s: String = style): Look = when (s) {
        "minimal" -> Look(Color.Transparent, Theme.c.border, Theme.c.muted, Theme.c.ink, Theme.c.ink, null)
        "gold" -> Look(Color(0xFF1F1A10), Color(0xFFC9A54A), Color(0xFFC9A54A), Color(0xFFE8C766), Color(0xFFE8C766), Color(0xFFE8D9AE))
        "night" -> Look(Color(0xFF0B1426), Color(0xFF2B3B5E), Color(0xFF7D8DB3), Color(0xFFE6ECFF), Color(0xFFFF6B6B), Color(0xFFB9C4E2))
        "green" -> Look(Color(0xFF0F3B2E), Color(0xFF3C8D6E), Color(0xFFA9D8C2), Color(0xFFFFFFFF), Color(0xFFFFD27A), Color(0xFFD8F0E4))
        else -> Look(Theme.c.surface, Theme.c.border, Theme.c.muted, Theme.c.ink, Color(0xFFE5484D), null)
    }

    var favVersion by mutableStateOf(0)
        private set

    fun favs(): List<Mosque> = runCatching {
        val a = JSONArray(Prefs.store.getString(FAVS, "[]"))
        (0 until a.length()).map { i -> a.getJSONObject(i).let { Mosque(it.getString("n"), it.getDouble("lat"), it.getDouble("lon"), 0.0) } }
    }.getOrDefault(emptyList())

    fun isFav(m: Mosque) = favs().any { same(it, m) }

    private fun same(a: Mosque, b: Mosque) = abs(a.lat - b.lat) < 1e-5 && abs(a.lon - b.lon) < 1e-5

    private fun raw(): List<JSONObject> = runCatching {
        val a = JSONArray(Prefs.store.getString(FAVS, "[]"))
        (0 until a.length()).map { a.getJSONObject(it) }
    }.getOrDefault(emptyList())

    private fun write(list: List<JSONObject>) {
        val a = JSONArray()
        list.take(30).forEach { a.put(it) }
        Prefs.store.edit().putString(FAVS, a.toString()).apply()
        favVersion++
    }

    private fun JSONObject.isAt(m: Mosque) = abs(optDouble("lat") - m.lat) < 1e-5 && abs(optDouble("lon") - m.lon) < 1e-5

    fun toggle(m: Mosque) {
        val list = raw().toMutableList()
        if (list.removeAll { it.isAt(m) }.not()) list.add(0, JSONObject().put("n", m.name).put("lat", m.lat).put("lon", m.lon))
        write(list)
    }

    /** Favori camiye kişisel not (alan `t`; yalnız telefonda; başkalarıyla paylaşım sunucu ister, ücretli sistemle
     *  birlikte ele alınacak, 4 Kasım 2026 sahibin kararı). Boş not silinir. */
    fun noteOf(m: Mosque): String? = raw().firstOrNull { it.isAt(m) }?.optString("t")?.takeIf { it.isNotEmpty() }

    fun setNote(m: Mosque, note: String) {
        val t = note.trim().take(500)
        write(raw().map { if (it.isAt(m)) it.apply { if (t.isEmpty()) remove("t") else put("t", t) } else it })
    }

    /** Büyük daire üzerinde n+1 nokta (enlem, boylam), boylam başlangıca göre kesintisiz */
    fun route(lat: Double, lon: Double, n: Int = 72): List<Pair<Double, Double>> {
        val r = Math.PI / 180
        fun vec(la: Double, lo: Double) = doubleArrayOf(cos(la * r) * cos(lo * r), cos(la * r) * sin(lo * r), sin(la * r))
        val a = vec(lat, lon)
        val b = vec(KAABA_LAT, KAABA_LON)
        val d = acos((a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).coerceIn(-1.0, 1.0))
        val out = ArrayList<Pair<Double, Double>>()
        var prevLon = lon
        for (i in 0..n) {
            val t = i.toDouble() / n
            val (p, q) = if (d < 1e-9) 1.0 to 0.0 else sin((1 - t) * d) / sin(d) to sin(t * d) / sin(d)
            val x = p * a[0] + q * b[0]
            val y = p * a[1] + q * b[1]
            val z = p * a[2] + q * b[2]
            val la = atan2(z, sqrt(x * x + y * y)) / r
            var lo = atan2(y, x) / r
            while (lo - prevLon > 180) lo -= 360
            while (lo - prevLon < -180) lo += 360
            prevLon = lo
            out += la to lo
        }
        return out
    }

    /** Çizginin sığdığı 2:1 kesit (batı, kuzey, boylam genişliği); dünya dışına taşmaz */
    fun frame(pts: List<Pair<Double, Double>>): Triple<Double, Double, Double> {
        var w = pts.minOf { it.second } - 8
        var e = pts.maxOf { it.second } + 8
        var s = pts.minOf { it.first } - 6
        var nn = pts.maxOf { it.first } + 6
        var lonSpan = e - w
        var latSpan = nn - s
        if (lonSpan < 2 * latSpan) { val add = 2 * latSpan - lonSpan; w -= add / 2; e += add / 2; lonSpan = e - w }
        else { val add = lonSpan / 2 - latSpan; s -= add / 2; nn += add / 2; latSpan = nn - s }
        if (lonSpan > 360 || latSpan > 180) return Triple(-180.0, 90.0, 360.0)
        if (nn > 90) { s -= nn - 90; nn = 90.0 }
        if (s < -90) { nn += -90 - s; s = -90.0 }
        if (w < -180 && e <= 180) { e += -180 - w; w = -180.0 }
        if (e > 180 && w >= -180) { w -= e - 180; e = 180.0 }
        if (w < -180 || e > 180) return Triple(-180.0, 90.0, 360.0)
        return Triple(w, nn, lonSpan)
    }

    /** Dokunun yalnız gereken kesiti (tam çözünürlükten) */
    fun region(c: Context, w: Double, n: Double, span: Double): ImageBitmap? = runCatching {
        c.assets.open("earth.jpg").use { input ->
            @Suppress("DEPRECATION")
            val dec = BitmapRegionDecoder.newInstance(input, false) ?: return null
            val tw = dec.width
            val th = dec.height
            val x0 = ((w + 180) / 360 * tw).toInt().coerceIn(0, tw - 1)
            val y0 = ((90 - n) / 180 * th).toInt().coerceIn(0, th - 1)
            val x1 = ((w + span + 180) / 360 * tw).toInt().coerceIn(x0 + 1, tw)
            val y1 = ((90 - n + span / 2) / 180 * th).toInt().coerceIn(y0 + 1, th)
            val opts = BitmapFactory.Options().apply { inSampleSize = max(1, (x1 - x0) / 900) }
            dec.decodeRegion(Rect(x0, y0, x1, y1), opts)?.asImageBitmap().also { dec.recycle() }
        }
    }.getOrNull()
}

/** Pusula görünümü seçimi: beş küçük kadran örneği */
@Composable
fun QiblaStylePicker() {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (s in QiblaPrefs.STYLES) {
            val look = QiblaPrefs.look(s)
            val on = QiblaPrefs.style == s
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { QiblaPrefs.saveStyle(s) }) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(if (look.face == Color.Transparent) Theme.c.bg else look.face)
                        .border(if (on) 3.dp else 1.dp, if (on) Theme.c.accent else look.ring, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(30.dp)) {
                        val cx = size.width / 2
                        val p = Path().apply { moveTo(cx, 0f); lineTo(cx + 6.dp.toPx(), size.height / 2); lineTo(cx - 6.dp.toPx(), size.height / 2); close() }
                        drawPath(p, look.needle)
                        drawCircle(look.north, 2.5.dp.toPx(), Offset(cx, size.height * 0.8f))
                    }
                }
                Text(L("qibla.style.$s"), color = if (on) Theme.c.accent else Theme.c.muted, fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** Haritada buradan Kâbe'ye en kısa yol (büyük daire); kendi dokumuzdan kesit, ağ yok */
@Composable
fun QiblaRouteMap(lat: Double, lon: Double, placeName: String) {
    val ctx = LocalContext.current
    val d = LocalDensity.current.density
    val pts = remember(lat, lon) { QiblaPrefs.route(lat, lon) }
    val fr = remember(pts) { QiblaPrefs.frame(pts) }
    val tex = remember(fr) { QiblaPrefs.region(ctx, fr.first, fr.second, fr.third) }
    val accent = Theme.c.accent
    Canvas(Modifier.fillMaxWidth().aspectRatio(2f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF0E1A2E))) {
        val (w0, n0, span) = fr
        val wpx = size.width
        val hpx = size.height
        if (tex != null) drawImage(tex, srcOffset = IntOffset.Zero, srcSize = IntSize(tex.width, tex.height), dstSize = IntSize(wpx.toInt(), hpx.toInt()))
        fun xy(la: Double, lo0: Double): Offset {
            var lo = lo0
            if (span >= 360) { while (lo > 180) lo -= 360; while (lo < -180) lo += 360 }
            return Offset(((lo - w0) / span * wpx).toFloat(), ((n0 - la) / (span / 2) * hpx).toFloat())
        }
        val path = Path()
        var prev: Offset? = null
        for ((la, lo) in pts) {
            val p = xy(la, lo)
            if (prev == null || abs(p.x - prev.x) > wpx / 2) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            prev = p
        }
        drawPath(path, Color.Black.copy(alpha = 0.5f), style = Stroke(5f * d, cap = StrokeCap.Round))
        drawPath(path, accent, style = Stroke(3f * d, cap = StrokeCap.Round))
        val start = xy(lat, lon)
        val end = xy(QiblaPrefs.KAABA_LAT, pts.last().second)
        drawCircle(Color.White, 5f * d, start)
        drawCircle(accent, 3.5f * d, start)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f * d
            color = android.graphics.Color.WHITE
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(3f * d, 0f, 0f, android.graphics.Color.BLACK)
        }
        val k = android.graphics.Paint(paint).apply { textSize = 20f * d; textAlign = android.graphics.Paint.Align.CENTER }
        drawContext.canvas.nativeCanvas.drawText("🕋", end.x, end.y + 7f * d, k)
        val label = placeName.take(18)
        val tw = paint.measureText(label)
        val lx = if (start.x + 8 * d + tw > wpx) start.x - 8 * d - tw else start.x + 8 * d
        drawContext.canvas.nativeCanvas.drawText(label, min(max(2f, lx), wpx - tw - 2f), start.y - 6 * d, paint)
    }
}
