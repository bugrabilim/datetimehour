package tr.bumba.time

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Dönen dünya (sitedeki küre ve iOS GlobeView gibi): Natural Earth dokulu küre, gündüz/gece gölgesi, Güneş ve Ay'ın
 * tepe noktası, 15°'lik saat dilimi meridyenleri (0° kırmızı), öne çıkan şehirlerin yerel saati ve hava durumu.
 * Kendiliğinden döner (bir tur 2 dk); dokununca durur, "Tüm dünya" ile yeniden döner; yakınlaşınca 31 şehir ve o
 * bölgedeki yerler (places.json) yerel saatleriyle görünür. Hareketi azalt (animasyonlar kapalı) tercihinde dönmez.
 *
 * Harita anahtarı gerekmez: yüzey, sitedeki yazılım yolu gibi piksel başına ters ortografik izdüşümle bir Bitmap'e
 * arka planda çizilir (geometri yalnız boyut, yakınlaştırma ya da enlem değişince yeniden hesaplanır; dönerken her
 * karede yalnız doku okunur). Çizgiler ve etiketler üstteki Canvas'ta.
 *
 * fullscreen: kapatma düğmesi (onClose); interactive = false: saat ekranındaki küçük kopya (dokunuş küreye gitmez,
 * düğme yok; dokunma çağıranındır).
 */
@Composable
fun GlobeView(modifier: Modifier = Modifier, fullscreen: Boolean = false, interactive: Boolean = true, onClose: () -> Unit = {}) {
    val context = LocalContext.current
    val reduce = remember {
        GlobeConst.TEST || Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val state = remember { GlobeState(reduce) }
    var showFullscreen by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier.clipToBounds()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1426), Color(0xFF04060C))))
            .semantics { contentDescription = L("globe.label") },
    ) {
        val density = LocalDensity.current
        val w = constraints.maxWidth.coerceAtLeast(1)
        val h = constraints.maxHeight.coerceAtLeast(1)
        GlobeSurface(state, w, h, interactive, density.density)

        if (interactive) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(if (fullscreen) 16.dp else 10.dp)
                    .then(if (fullscreen) Modifier.safeDrawingPadding() else Modifier)
                    .size(36.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .clickable { if (fullscreen) onClose() else showFullscreen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (fullscreen) Icons.Filled.Close else Icons.Filled.Fullscreen,
                    L(if (fullscreen) "client.exitFullscreen" else "client.fullscreen"),
                    tint = Color.White, modifier = Modifier.size(20.dp),
                )
            }
            if (state.zoom > 1.01f || (!state.spinning && !reduce)) {
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(10.dp)
                        .then(if (fullscreen) Modifier.safeDrawingPadding() else Modifier)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .clickable { state.reset() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Public, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(L("globe.reset"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            GlobeView(Modifier.fillMaxSize(), fullscreen = true, onClose = { showFullscreen = false })
        }
    }
}

internal object GlobeConst {
    /** Sitedeki GLOBE_CITIES (build.mjs) ve iOS GlobeView.featured; sıra öncelik sırasıdır (İstanbul en önde) */
    val FEATURED = listOf(
        "istanbul", "london", "moscow", "dubai", "mumbai", "singapore", "beijing", "tokyo", "sydney",
        "cairo", "johannesburg", "sao-paulo", "new-york", "los-angeles",
    )
    /** Derece/saniye: bir tur 2 dakika */
    const val SPEED = 3.0
    const val TILT0 = 20.0
    const val ZMAX = 6f
    /** Bu yakınlaştırmanın üstünde 31 şehir ve dünya yerleri görünür */
    const val ZPLACES = 1.25f
    /** Ekran görüntüsü testi (Robolectric): dönmez, ağ yok, kare eşzamanlı çizilir */
    val TEST = Build.FINGERPRINT == "robolectric"
    const val RAD = PI / 180
}

/** Kamera: merkez boylamı (lon0), merkez enlemi (tilt), yakınlaştırma; dönüyor mu */
internal class GlobeState(private val reduce: Boolean) {
    var lon0 by mutableStateOf(initialLon())
    var tilt by mutableStateOf(GlobeConst.TILT0)
    var zoom by mutableFloatStateOf(1f)
    var spinning by mutableStateOf(!reduce)

    fun reset() {
        zoom = 1f
        tilt = GlobeConst.TILT0
        spinning = !reduce
    }

    companion object {
        /** Başlangıçta cihazın bölgesi görünür (sitedeki gibi) */
        fun initialLon(): Double {
            val off = ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds
            return wrap(off / 240.0 - 10)
        }

        fun wrap(x: Double): Double {
            var v = (x + 180) % 360
            if (v < 0) v += 360
            return v - 180
        }
    }
}

/** Çizilmiş bir kare ve onu üreten kamera (çizgiler ve etiketler aynı kamerayla çizilir) */
internal class GlobeFrame(
    val bitmap: Bitmap?, val lon0: Double, val tilt: Double, val zoom: Float, val w: Int, val h: Int, val ms: Long,
) {
    val r0: Float = GlobeRenderer.radius(w, h)
    val r: Float = r0 * zoom
    private val sinT = sin(tilt * GlobeConst.RAD)
    private val cosT = cos(tilt * GlobeConst.RAD)

    /** Ortografik izdüşüm: x, y (piksel) ve z (> 0 ön yüz) */
    fun project(lat: Double, lon: Double): Triple<Float, Float, Double> {
        val f = lat * GlobeConst.RAD
        val l = (lon - lon0) * GlobeConst.RAD
        val cf = cos(f)
        val cl = cos(l)
        val x = w / 2f + (cf * sin(l) * r).toFloat()
        val y = h / 2f - ((cosT * sin(f) - sinT * cf * cl) * r).toFloat()
        return Triple(x, y, sinT * sin(f) + cosT * cf * cl)
    }
}

/**
 * Yüzey çizimi (sitedeki paintSurfaceCPU): ekran pikselleri küçültülmüş bir tampona; her piksel için enlem/boylam
 * önceden hesaplanır, karede doku çift doğrusal okunur ve Güneş'e göre gündüz/gece karışımı yapılır.
 */
internal class GlobeRenderer {
    private var key = ""
    private var bw = 0
    private var bh = 0
    private var n = 0
    private lateinit var idx: IntArray
    private lateinit var relLon: FloatArray
    private lateinit var lat: FloatArray
    private lateinit var sinLat: FloatArray
    private lateinit var cosLat: FloatArray
    private lateinit var cosL: FloatArray
    private lateinit var sinL: FloatArray
    private lateinit var shade: FloatArray
    private lateinit var alpha: IntArray
    // Çift tampon: biri ekranda çizilirken öteki doldurulur
    private val bitmaps = arrayOfNulls<Bitmap>(2)
    private var flip = 0
    private var buf = IntArray(0)

    companion object {
        /** Küre yarıçapı (piksel): alanın %94'ü (sitedeki R0 gibi; kenarda biraz boşluk) */
        fun radius(w: Int, h: Int): Float = min(w * 0.47f, h * 0.47f)

        /** Piksel bütçesi: ~240 bin; kürenin ekrandaki alanına göre ölçek (dönerken her karede bu kadar piksel) */
        private const val BUDGET = 240_000.0
    }

    private fun geometry(w: Int, h: Int, zoom: Float, tilt: Double) {
        val r = radius(w, h) * zoom
        val visible = min(PI * r * r, w.toDouble() * h)
        val k = min(1.0, sqrt(BUDGET / visible))
        val nbw = max(1, (w * k).roundToInt())
        val nbh = max(1, (h * k).roundToInt())
        val rs = r * k
        val newKey = "$nbw,$nbh,$rs,$tilt"
        if (newKey == key) return
        key = newKey
        bw = nbw
        bh = nbh
        val cx = bw / 2.0
        val cy = bh / 2.0
        val sinT = sin(tilt * GlobeConst.RAD)
        val cosT = cos(tilt * GlobeConst.RAD)
        val lim = 1 + 2 / rs
        val list = IntArray(bw * bh)
        var cnt = 0
        for (j in 0 until bh) {
            val y = (cy - (j + 0.5)) / rs
            for (i in 0 until bw) {
                val x = (i + 0.5 - cx) / rs
                if (x * x + y * y <= lim) list[cnt++] = j * bw + i
            }
        }
        n = cnt
        idx = list.copyOf(cnt)
        relLon = FloatArray(cnt); lat = FloatArray(cnt); sinLat = FloatArray(cnt); cosLat = FloatArray(cnt)
        cosL = FloatArray(cnt); sinL = FloatArray(cnt); shade = FloatArray(cnt); alpha = IntArray(cnt)
        for (q in 0 until cnt) {
            val p = idx[q]
            val jj = p / bw
            val ii = p - jj * bw
            var xx = (ii + 0.5 - cx) / rs
            var yy = (cy - (jj + 0.5)) / rs
            var rr = xx * xx + yy * yy
            val d = sqrt(rr)
            if (rr > 1) { xx /= d; yy /= d; rr = 1.0 }
            val z = sqrt(max(0.0, 1 - rr))
            val sl = z * sinT + yy * cosT
            val la = asin(sl.coerceIn(-1.0, 1.0))
            val lr = atan2(xx, z * cosT - yy * sinT)
            lat[q] = (la / GlobeConst.RAD).toFloat()
            relLon[q] = (lr / GlobeConst.RAD).toFloat()
            sinLat[q] = sin(la).toFloat(); cosLat[q] = cos(la).toFloat()
            cosL[q] = cos(lr).toFloat(); sinL[q] = sin(lr).toFloat()
            shade[q] = (0.72 + 0.28 * z).toFloat()
            alpha[q] = (((1 - d) * rs + 0.5).coerceIn(0.0, 1.0) * 255).toInt()
        }
        buf = IntArray(bw * bh)
        // Eski Bitmap'ler geri dönüştürülmez (ekranda hâlâ çizilebilir); yalnız bırakılır
        bitmaps[0] = null; bitmaps[1] = null
    }

    fun render(lon0: Double, tilt: Double, zoom: Float, w: Int, h: Int, ms: Long, tex: IntArray?, tw: Int, th: Int): GlobeFrame {
        geometry(w, h, zoom, tilt)
        val sun = GlobeSky.sun(ms)
        val sd = sin(sun.lat * GlobeConst.RAD).toFloat()
        val cd = cos(sun.lat * GlobeConst.RAD).toFloat()
        val dl = (lon0 - sun.lon) * GlobeConst.RAD
        val cD = cos(dl).toFloat()
        val sD = sin(dl).toFloat()
        // Yakınlaşınca gündüz/gece gölgesi yok (sitedeki gibi)
        val night = zoom <= 1.01f
        val base = (lon0 + 540).toFloat()
        val px = buf
        for (q in 0 until n) {
            var r: Float
            var g: Float
            var b: Float
            if (tex != null) {
                val u = ((relLon[q] + base) % 360f) / 360f * tw - 0.5f
                val v = (90f - lat[q]) / 180f * th - 0.5f
                var c0 = u.toInt().let { if (u < 0 && it.toFloat() != u) it - 1 else it }
                val fx = u - c0
                var r0 = v.toInt().let { if (v < 0 && it.toFloat() != v) it - 1 else it }
                var fy = v - r0
                c0 = ((c0 % tw) + tw) % tw
                val c1 = (c0 + 1) % tw
                if (r0 < 0) { r0 = 0; fy = 0f } else if (r0 >= th - 1) { r0 = th - 2; fy = 1f }
                val a0 = r0 * tw
                val a1 = a0 + tw
                val p00 = tex[a0 + c0]; val p01 = tex[a0 + c1]; val p10 = tex[a1 + c0]; val p11 = tex[a1 + c1]
                val w00 = (1 - fx) * (1 - fy); val w01 = fx * (1 - fy); val w10 = (1 - fx) * fy; val w11 = fx * fy
                r = ((p00 shr 16) and 255) * w00 + ((p01 shr 16) and 255) * w01 + ((p10 shr 16) and 255) * w10 + ((p11 shr 16) and 255) * w11
                g = ((p00 shr 8) and 255) * w00 + ((p01 shr 8) and 255) * w01 + ((p10 shr 8) and 255) * w10 + ((p11 shr 8) and 255) * w11
                b = (p00 and 255) * w00 + (p01 and 255) * w01 + (p10 and 255) * w10 + (p11 and 255) * w11
            } else {
                r = 110f; g = 160f; b = 200f
            }
            val t = if (night) {
                val cz = sinLat[q] * sd + cosLat[q] * cd * (cosL[q] * cD - sinL[q] * sD)
                val s = ((cz + 0.1f) / 0.16f).coerceIn(0f, 1f)
                s * s * (3 - 2 * s)
            } else 1f
            val sh = shade[q]
            val nr = r * 0.2f + 6
            val ng = g * 0.2f + 12
            val nb = b * 0.24f + 30
            val ir = ((nr + (r - nr) * t) * sh).toInt().coerceIn(0, 255)
            val ig = ((ng + (g - ng) * t) * sh).toInt().coerceIn(0, 255)
            val ib = ((nb + (b - nb) * t) * sh).toInt().coerceIn(0, 255)
            px[idx[q]] = (alpha[q] shl 24) or (ir shl 16) or (ig shl 8) or ib
        }
        flip = 1 - flip
        val bmp = bitmaps[flip] ?: Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888).also { bitmaps[flip] = it }
        bmp.setPixels(px, 0, bw, 0, 0, bw, bh)
        return GlobeFrame(bmp, lon0, tilt, zoom, w, h, ms)
    }
}

/** Yüzey (arka planda çizilen Bitmap) + çizgiler, Güneş/Ay ve etiketler; dokunuşla döndürme ve yakınlaştırma */
@Composable
private fun GlobeSurface(state: GlobeState, w: Int, h: Int, interactive: Boolean, density: Float) {
    val context = LocalContext.current
    val renderer = remember { GlobeRenderer() }
    var frame by remember { mutableStateOf<GlobeFrame?>(null) }
    var wxVersion by remember { mutableIntStateOf(0) }
    val now = rememberNow()
    val cities = remember { City.all }
    val featured = remember(cities) { GlobeConst.FEATURED.mapNotNull { k -> cities.firstOrNull { it.key == k } } }
    val places = remember(Strings.lang) { GlobePlace.all(context) }

    if (GlobeConst.TEST && frame == null) {
        // Ekran görüntüsü: ilk kare hemen (arka plan iş parçacığı beklenmez)
        val tex = GlobeTexture.load(context)
        frame = renderer.render(state.lon0, state.tilt, state.zoom, w, h, System.currentTimeMillis(), tex, GlobeTexture.width, GlobeTexture.height)
    }

    LaunchedEffect(w, h) {
        val tex = if (GlobeConst.TEST) GlobeTexture.load(context) else withContext(Dispatchers.IO) { GlobeTexture.load(context) }
        var last = System.nanoTime()
        var lastKey = ""
        var lastMs = 0L
        while (true) {
            val t = System.nanoTime()
            val dt = min(0.1, (t - last) / 1e9)
            last = t
            if (state.spinning && state.zoom < 1.2f) state.lon0 = GlobeState.wrap(state.lon0 - GlobeConst.SPEED * dt)
            val ms = System.currentTimeMillis()
            val key = "${state.lon0},${state.tilt},${state.zoom}"
            // Kamera değişince ya da yarım dakikada bir (gece gölgesi ilerler) yeniden çizilir
            if (key != lastKey || ms - lastMs > 30_000) {
                val lon0 = state.lon0
                val tilt = state.tilt
                val zoom = state.zoom
                frame = if (GlobeConst.TEST) renderer.render(lon0, tilt, zoom, w, h, ms, tex, GlobeTexture.width, GlobeTexture.height)
                else withContext(Dispatchers.Default) {
                    renderer.render(lon0, tilt, zoom, w, h, ms, tex, GlobeTexture.width, GlobeTexture.height)
                }
                lastKey = key
                lastMs = ms
            }
            delay(33)
        }
    }

    // Öne çıkan şehirlerin havası: saatte bir (tek istek)
    LaunchedEffect(Unit) {
        if (GlobeConst.TEST) return@LaunchedEffect
        while (true) {
            if (GlobeWeather.refresh(featured)) wxVersion++
            delay(60_000)
        }
    }

    val paints = remember(density) { GlobePaints(density) }
    val gestures = if (!interactive) Modifier else Modifier
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoomChange, _ ->
                state.spinning = false
                val r = GlobeRenderer.radius(w, h) * state.zoom
                state.lon0 = GlobeState.wrap(state.lon0 - pan.x / r / GlobeConst.RAD)
                state.tilt = (state.tilt + pan.y / r / GlobeConst.RAD).coerceIn(-80.0, 80.0)
                state.zoom = (state.zoom * zoomChange).coerceIn(1f, GlobeConst.ZMAX)
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { state.spinning = false },
                onDoubleTap = {
                    state.spinning = false
                    state.zoom = if (state.zoom >= GlobeConst.ZMAX - 0.1f) 1f else min(GlobeConst.ZMAX, state.zoom * 2)
                },
            )
        }

    Canvas(Modifier.fillMaxSize().then(gestures)) {
        val f = frame ?: return@Canvas
        wxVersion
        val bmp = f.bitmap
        if (bmp != null && !bmp.isRecycled) {
            // Atmosfer: kürenin çevresinde hafif mavi ışık (yalnız uzakken)
            if (f.zoom <= 1.01f) {
                drawCircle(
                    Brush.radialGradient(
                        0.88f to Color(0x5584B7FF), 1f to Color(0x0084B7FF),
                        center = Offset(size.width / 2, size.height / 2), radius = f.r * 1.09f,
                    ),
                    radius = f.r * 1.09f, center = Offset(size.width / 2, size.height / 2),
                )
            }
            drawImage(
                bmp.asImageBitmap(), srcOffset = IntOffset.Zero, srcSize = IntSize(bmp.width, bmp.height),
                dstOffset = IntOffset.Zero, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
        drawGlobeLines(f, paints)
        if (f.zoom <= 1.01f) drawSky(f, now.toEpochMilli(), paints)
        drawLabels(f, now, featured, cities, places, paints)
    }
}

/** Yazı boyaları (yerel Canvas: hızlı ölçüm, Arapça adlar için sistem metin düzeni) */
private class GlobePaints(val d: Float) {
    val name = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
    val time = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; typeface = Typeface.DEFAULT_BOLD }
    val utc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 10.5f * d
        textAlign = Paint.Align.CENTER
        setShadowLayer(2.5f * d, 0f, 0f, android.graphics.Color.BLACK)
    }
}

/** 15°'lik saat dilimi meridyenleri (0° kırmızı), ekvator ve UTC farkları */
private fun DrawScope.drawGlobeLines(f: GlobeFrame, p: GlobePaints) {
    val d = p.d
    for (m in -12 until 12) {
        val zero = m == 0
        val path = Path()
        var pen = false
        var lat = -78.0
        while (lat <= 78.0) {
            val (x, y, z) = f.project(lat, m * 15.0)
            if (z > 0) { if (pen) path.lineTo(x, y) else path.moveTo(x, y); pen = true } else pen = false
            lat += 3.0
        }
        drawPath(path, if (zero) Color(0xFFE53935) else Color.White.copy(alpha = 0.42f), style = Stroke(if (zero) 2.2f * d else 1f * d))
    }
    val eq = Path()
    var on = false
    var lo = -180.0
    while (lo <= 180.0) {
        val (x, y, z) = f.project(0.0, lo)
        if (z > 0) { if (on) eq.lineTo(x, y) else eq.moveTo(x, y); on = true } else on = false
        lo += 4.0
    }
    drawPath(eq, Color.White.copy(alpha = 0.3f), style = Stroke(1f * d))
    // Kenar
    val c = Offset(size.width / 2, size.height / 2)
    if (f.r < hypot(c.x, c.y)) drawCircle(Color.White.copy(alpha = 0.22f), f.r, c, style = Stroke(1.2f * d))
    // UTC farkları (uzakken; iOS gibi "UTC", "+3")
    if (f.zoom > 1.01f) return
    drawIntoCanvas { cv ->
        for (hr in -11..12) {
            val (x, y, z) = f.project(-36.0, hr * 15.0)
            if (z < 0.3) continue
            p.utc.color = if (hr == 0) 0xFFFF5252.toInt() else android.graphics.Color.WHITE
            p.utc.alpha = (min(1.0, (z - 0.3) / 0.25) * 255).toInt()
            cv.nativeCanvas.drawText(if (hr == 0) "UTC" else (if (hr > 0) "+$hr" else "−${-hr}"), x, y, p.utc)
        }
    }
}

/** Güneş ve Ay'ın tepe noktası (Ay evresine göre aydınlık bölümüyle; sitedeki drawMoon) */
private fun DrawScope.drawSky(f: GlobeFrame, ms: Long, p: GlobePaints) {
    val d = p.d
    val mo = GlobeSky.moon(ms)
    val (mx, my, mz) = f.project(mo.lat, mo.lon)
    if (mz > 0) {
        val r = 8f * d
        val c = Offset(mx, my)
        drawCircle(Brush.radialGradient(listOf(Color(0x73DCE2F0), Color(0x00DCE2F0)), c, r * 2.6f), r * 2.6f, c)
        drawCircle(Color(0xFF3A3F4B), r, c)
        val k = cos(mo.elong * GlobeConst.RAD).toFloat()
        val waxing = mo.elong < 180
        val lit = Path()
        // Aydınlık yarım daire (büyürken sağı), sonra sonlandırıcı elips
        lit.arcTo(Rect(c, r), -90f, if (waxing) 180f else -180f, true)
        val ew = abs(k) * r
        lit.arcTo(Rect(c.x - ew, c.y - r, c.x + ew, c.y + r), 90f, if ((k > 0) == waxing) -180f else 180f, false)
        lit.close()
        drawPath(lit, Color(0xFFEEF1F7))
        drawCircle(Color.White.copy(alpha = 0.7f), r, c, style = Stroke(1.2f * d))
    }
    val sun = GlobeSky.sun(ms)
    val (sx, sy, sz) = f.project(sun.lat, sun.lon)
    if (sz > 0) {
        val c = Offset(sx, sy)
        drawCircle(
            Brush.radialGradient(
                0f to Color(0xF2FFD65A), 0.35f to Color(0x8CFFC83C), 1f to Color(0x00FFC83C), center = c, radius = 30f * d,
            ),
            30f * d, c,
        )
        drawCircle(Color(0xFFFFD23F), 8f * d, c)
        drawCircle(Color(0xFFE08A00), 8f * d, c, style = Stroke(2f * d))
    }
}

private data class Label(val name: String, val zone: ZoneId, val lat: Double, val lon: Double, val city: Boolean, val key: String)

/**
 * Etiketler: uzakken öne çıkan şehirler (saat + hava), yakınken 31 şehir ve dünya yerleri (nüfus sırasıyla, bütçeli).
 * Yarı saydam koyu kutuda iki satır (iOS gibi); çakışan düşük öncelikli etiket çizilmez.
 */
private fun DrawScope.drawLabels(
    f: GlobeFrame, now: Instant, featured: List<City>, cities: List<City>, places: List<GlobePlace>, p: GlobePaints,
) {
    val d = p.d
    val near = f.zoom > GlobeConst.ZPLACES
    val list = ArrayList<Label>()
    for (c in featured) list += Label(c.displayName, c.zone, c.lat, c.lon, true, c.key)
    if (near) {
        for (c in cities) if (c.key !in GlobeConst.FEATURED) list += Label(c.displayName, c.zone, c.lat, c.lon, true, c.key)
        for (pl in places) list += Label(pl.name, pl.zone, pl.lat, pl.lon, false, "")
    }
    val budget = if (near) (8 + min(24f, f.zoom * 2.2f) * (min(size.width, size.height) / d / 270f)).toInt() else 0
    val boxes = ArrayList<Rect>()
    var shownPlaces = 0
    val pad = 5f * d
    drawIntoCanvas { cv ->
        val canvas = cv.nativeCanvas
        for (l in list) {
            if (!l.city && shownPlaces >= budget) break
            val (x, y, z) = f.project(l.lat, l.lon)
            if (z < 0.12) continue
            if (x < 0 || x > size.width || y < 0 || y > size.height) continue
            p.name.textSize = (if (l.city) 10.5f else 9.5f) * d
            p.time.textSize = (if (l.city) 13f else 11f) * d
            var line2 = Fmt.time(now, l.zone)
            if (l.city) GlobeWeather.cached(l.key)?.let {
                line2 += "  " + WeatherCode.of(it.code).icon(it.isDay) + " " + Units.temp(it.celsius)
            }
            val w1 = p.name.measureText(l.name)
            val w2 = p.time.measureText(line2)
            val bw = max(w1, w2) + 2 * pad
            val h1 = p.name.textSize * 1.15f
            val h2 = p.time.textSize * 1.2f
            val bh = h1 + h2 + 4f * d
            val box = Rect(x - bw / 2, y - bh / 2, x + bw / 2, y + bh / 2)
            if (box.left < 2 || box.right > size.width - 2 || box.top < 2 || box.bottom > size.height - 2) continue
            if (boxes.any { it.overlaps(box) }) continue
            boxes += box
            if (!l.city) shownPlaces++
            val fade = min(1.0, (z - 0.12) / 0.2).toFloat()
            drawRoundRect(Color.Black.copy(alpha = 0.55f * fade), box.topLeft, Size(bw, bh), androidx.compose.ui.geometry.CornerRadius(5f * d))
            p.name.alpha = (fade * 255).toInt()
            p.time.alpha = (fade * 255).toInt()
            canvas.drawText(l.name, x - w1 / 2, box.top + 2f * d + p.name.textSize, p.name)
            canvas.drawText(line2, x - w2 / 2, box.top + 2f * d + h1 + p.time.textSize, p.time)
        }
    }
}
