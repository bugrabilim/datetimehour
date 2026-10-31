package tr.bumba.time

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import tr.bumba.time.core.Qibla
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val numberLocale: Locale get() = Strings.numberLocale

/** Bir ondalıklı sayı, uygulama dilinde (Arapçada da Latin rakamı) */
private fun oneDecimal(x: Double): String = NumberFormat.getInstance(numberLocale).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}.format(x)

/** Derece, uygulama dilinde bir ondalıkla ("151,6°") */
fun degrees(x: Double): String = oneDecimal(x) + "°"

/** Uzaklık: 1 km'den azsa metre, 10 km'den azsa bir ondalıklı, yoksa tam kilometre (binlik ayraçlı) */
fun distanceText(km: Double): String {
    val f = NumberFormat.getIntegerInstance(numberLocale)
    return when {
        km < 1 -> T("qibla.m", f.format((km * 1000).roundToInt()))
        km < 10 -> T("qibla.km", oneDecimal(km))
        else -> T("qibla.km", f.format(km.roundToInt()))
    }
}

/** İki yön arasındaki işaretli fark (−180…180) */
private fun angleDiff(a: Double, b: Double): Double = ((a - b + 540) % 360) - 180

/**
 * Telefonun pusulası (30 Ekim 2026, rakip analizi #8): dönüş vektörü sensöründen manyetik yön, seçili yerin
 * manyetik sapmasıyla (GeomagneticField) gerçek kuzeye çevrilir; ekran yönüne göre eksenler yeniden eşlenir.
 * Sensör yoksa (emülatör, bazı tabletler) heading null kalır. Sonuçlar sin/cos ortalamasıyla yumuşatılır.
 */
class CompassState(private val context: Context, private val lat: Double, private val lon: Double) : SensorEventListener {
    private val sm = context.getSystemService(SensorManager::class.java)
    private val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val available = sensor != null
    var heading by mutableStateOf<Double?>(null)
        private set
    var accuracy by mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE)
        private set
    private val declination = GeomagneticField(lat.toFloat(), lon.toFloat(), 0f, System.currentTimeMillis()).declination.toDouble()
    private val r = FloatArray(9)
    private val out = FloatArray(9)
    private val o = FloatArray(3)
    private var sx = 0.0
    private var sy = 0.0

    fun start() {
        sensor?.let { sm?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() = sm?.unregisterListener(this)

    private fun rotation(): Int = runCatching {
        if (Build.VERSION.SDK_INT >= 30) context.display?.rotation ?: Surface.ROTATION_0
        else @Suppress("DEPRECATION") context.getSystemService(WindowManager::class.java).defaultDisplay.rotation
    }.getOrDefault(Surface.ROTATION_0)

    override fun onSensorChanged(e: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(r, e.values)
        val (x, y) = when (rotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(r, x, y, out)
        SensorManager.getOrientation(out, o)
        val az = Math.toRadians(Math.toDegrees(o[0].toDouble()) + declination)
        // Yumuşatma: açının sin/cos'u ayrı ayrı süzülür (359° → 0° geçişinde sıçramasın)
        sx = if (heading == null) sin(az) else sx * 0.8 + sin(az) * 0.2
        sy = if (heading == null) cos(az) else sy * 0.8 + cos(az) * 0.2
        heading = (Math.toDegrees(atan2(sx, sy)) + 360) % 360
        if (e.accuracy != accuracy) accuracy = e.accuracy
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {
        accuracy = a
    }
}

/**
 * Kıble (iOS'taki QiblaView): seçili yere göre kıble açısı, Kâbe'ye uzaklık, bugünün kıble saati, pusula;
 * altta yakındaki camiler. Hesap çekirdekte (`tr.bumba.time.core.Qibla`, sitedeki prayer-calc.js ile aynı).
 */
@Composable
fun QiblaScreen(onBack: () -> Unit) {
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    if (page == "mosques") {
        BackHandler { page = null }
        NearbyMosquesScreen { page = null }
        return
    }
    val context = LocalContext.current
    val place = AppPlace.current()
    val q = remember(place.lat, place.lon) { Qibla.direction(place.lat, place.lon) }
    val atKaaba = q.km < 1
    val compass = remember(place.lat, place.lon) { CompassState(context, place.lat, place.lon) }
    DisposableEffect(compass) {
        compass.start()
        onDispose { compass.stop() }
    }
    val heading = compass.heading
    val aligned = heading != null && abs(angleDiff(q.bearing, heading)) < 3
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(aligned) { if (aligned) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    Column(Modifier.fillMaxSize()) {
        SubBar(L("qibla.title"), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlacePicker()
            if (atKaaba) {
                Text(L("qibla.atKaaba"), Modifier.padding(vertical = 40.dp), color = Theme.c.accent, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            } else {
                QiblaDial(q.bearing, heading ?: 0.0, aligned)
                if (!compass.available) {
                    Note(L("qibla.noCompass"))
                } else {
                    Text(
                        if (aligned) L("qibla.aligned") else L("qibla.turn"),
                        color = if (aligned) Theme.c.accent else Theme.c.text, fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    )
                    if (compass.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) Note(L("qibla.calibrate"))
                }
            }
            AppCard {
                val today = place.today()
                val tz = place.zone.rules.getOffset(today.atTime(12, 0).atZone(place.zone).toInstant()).totalSeconds / 3600.0
                val t = Qibla.time(today.year, today.monthValue, today.dayOfMonth, place.lat, place.lon, tz)
                if (!atKaaba) {
                    Fact(L("qibla.angle"), degrees(q.bearing), L("qibla.angleNote"))
                    HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
                }
                Fact(L("qibla.distance"), distanceText(q.km))
                if (!atKaaba) {
                    HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
                    if (t != null) {
                        // Namaz vakitleri gibi dakikaya yuvarlanır (Diyanet'in kıble saatiyle aynı)
                        val at = today.atStartOfDay(place.zone).toInstant().plusSeconds((t * 60).roundToInt() * 60L)
                        Fact(L("qibla.time"), Fmt.hm(at, place.zone), L("qibla.timeNote"))
                    } else {
                        Fact(L("qibla.time"), "–", L("qibla.noTime"))
                    }
                }
            }
            AppCard(Modifier.clickable { page = "mosques" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Mosque, null, tint = Theme.c.accent)
                    Text("  " + L("qibla.mosques"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
                }
            }
            Note(L("qibla.method"), TextAlign.Start)
        }
    }
}

@Composable
private fun Note(text: String, align: TextAlign = TextAlign.Center) {
    Text(text, Modifier.fillMaxWidth(), color = Theme.c.muted, fontSize = 13.sp, textAlign = align)
}

@Composable
private fun Fact(title: String, value: String, note: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(value, color = Theme.c.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, style = Tnum)
            }
        }
        if (note != null) Text(note, color = Theme.c.muted, fontSize = 13.sp)
    }
}

/**
 * Kadran telefonun yönüne göre döner (kuzey her zaman kuzeyi gösterir); Kâbe işareti kıble açısında durur.
 * Ortadaki ok telefonun üstüne göre kıbleyi gösterir. Pusula yoksa kuzey yukarıda sabit kadran.
 */
@Composable
private fun QiblaDial(bearing: Double, heading: Double, aligned: Boolean) {
    // Dönüş animasyonu en kısa yoldan: hedef açı bir önceki değere göre −180…180 içinde tutulur
    var target by remember { mutableFloatStateOf(-heading.toFloat()) }
    target += angleDiff(-heading, target.toDouble()).toFloat()
    val dial by animateFloatAsState(target, tween(250), label = "dial")
    val ink = Theme.c.ink
    val muted = Theme.c.muted
    val arrowColor = if (aligned) Theme.c.accent else ink
    val cardinals = L("qibla.cardinals").split(" ")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier.padding(vertical = 20.dp).size(290.dp)
                .semantics { contentDescription = L("qibla.angle") + " " + degrees(bearing) },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(270.dp).background(Theme.c.surface, CircleShape).border(1.dp, Theme.c.border, CircleShape))
            Box(Modifier.fillMaxSize().rotate(dial), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(270.dp)) {
                    val c = Offset(size.width / 2, size.height / 2)
                    val rOut = size.width / 2 - 6.dp.toPx()
                    for (i in 0 until 72) {
                        val a = Math.toRadians(i * 5.0)
                        val major = i % 6 == 0
                        val len = if (major) 12.dp.toPx() else 6.dp.toPx()
                        val dir = Offset(sin(a).toFloat(), -cos(a).toFloat())
                        drawLine(
                            if (i % 18 == 0) ink else muted.copy(alpha = if (major) 0.8f else 0.4f),
                            c + dir * (rOut - len), c + dir * rOut, strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                        )
                    }
                }
                cardinals.forEachIndexed { i, s ->
                    Box(Modifier.size(270.dp).rotate(i * 90f), contentAlignment = Alignment.TopCenter) {
                        Text(
                            s, Modifier.padding(top = 24.dp).rotate(-dial - i * 90f),
                            color = if (i == 0) Color(0xFFE5484D) else muted, fontSize = 17.sp,
                            fontWeight = if (i == 0) FontWeight.Bold else FontWeight.SemiBold,
                        )
                    }
                }
                Box(Modifier.fillMaxSize().rotate(bearing.toFloat()), contentAlignment = Alignment.TopCenter) {
                    Text("🕋", Modifier.rotate(-dial - bearing.toFloat()), fontSize = 26.sp)
                }
            }
            // Kıble iğnesi: uç kıbleyi, soluk kuyruk ters yönü gösterir
            val tail = muted.copy(alpha = 0.35f)
            Canvas(Modifier.size(170.dp).rotate(dial + bearing.toFloat())) {
                val cx = size.width / 2
                val cy = size.height / 2
                val w = 14.dp.toPx()
                val head = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, 0f); lineTo(cx + w, cy); lineTo(cx - w, cy); close()
                }
                val back = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - w, cy); lineTo(cx + w, cy); lineTo(cx, size.height * 0.85f); close()
                }
                drawPath(back, tail)
                drawPath(head, arrowColor)
            }
            Box(Modifier.size(16.dp).background(Theme.c.surface2, CircleShape))
        }
    }
}

/** İki nokta arası uzaklık (m; haversine) */
private fun meters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = Math.PI / 180
    val a = sin((lat2 - lat1) * r / 2).let { it * it } + cos(lat1 * r) * cos(lat2 * r) * sin((lon2 - lon1) * r / 2).let { it * it }
    return 2 * 6_371_008.8 * kotlin.math.asin(kotlin.math.sqrt(a))
}

/** OpenStreetMap'teki bir cami (Overpass yanıtından) */
data class Mosque(val name: String, val lat: Double, val lon: Double, val meters: Double)

/**
 * Yakındaki camiler: OpenStreetMap verisinde "amenity=place_of_worship + religion=muslim" (Overpass API, anahtarsız).
 * Servise yalnız iki ondalığa yuvarlanmış bölge merkezi gider (~1 km); uzaklıklar telefonda tam konumla hesaplanır.
 */
object NearbyMosques {
    private const val API = "https://overpass-api.de/api/interpreter"
    @Volatile private var cache: Pair<String, List<Mosque>>? = null

    /** Testler için: ağsız örnek veri */
    fun seed(lat: Double, lon: Double, list: List<Mosque>) {
        cache = key(lat, lon) to list
    }

    private fun key(lat: Double, lon: Double) = String.format(Locale.ROOT, "%.2f,%.2f", lat, lon)

    /** Önbellekteki liste, verilen yere göre uzaklıkla sıralı */
    fun cached(lat: Double, lon: Double): List<Mosque>? = cache?.takeIf { it.first == key(lat, lon) }?.second?.let { recompute(it, lat, lon) }

    suspend fun load(lat: Double, lon: Double): List<Mosque> {
        cached(lat, lon)?.let { return it }
        val k = key(lat, lon)
        val list = withContext(Dispatchers.IO) { fetch(k) }
        cache = k to list
        return recompute(list, lat, lon)
    }

    private fun recompute(list: List<Mosque>, lat: Double, lon: Double) =
        list.map { it.copy(meters = meters(lat, lon, it.lat, it.lon)) }.sortedBy { it.meters }.take(30)

    private fun fetch(key: String): List<Mosque> {
        val (lat, lon) = key.split(",")
        val query = "[out:json][timeout:20];nwr[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:4000,$lat,$lon);out center 60;"
        val c = URL("$API?data=" + URLEncoder.encode(query, "UTF-8")).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 25_000
        c.setRequestProperty("User-Agent", "SaatTarih/1.0 (https://time.bumba.tr)")
        val body = try {
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
        return parse(JSONObject(body))
    }

    fun parse(o: JSONObject): List<Mosque> {
        val els = o.optJSONArray("elements") ?: return emptyList()
        val lang = Strings.locale.language
        return (0 until els.length()).mapNotNull { i ->
            val e = els.getJSONObject(i)
            val ll = if (e.has("lat")) e else e.optJSONObject("center") ?: return@mapNotNull null
            val tags = e.optJSONObject("tags")
            val name = tags?.optString("name:$lang")?.takeIf { it.isNotEmpty() }
                ?: tags?.optString("name")?.takeIf { it.isNotEmpty() }
                ?: L("qibla.mosqueUnnamed")
            Mosque(name, ll.getDouble("lat"), ll.getDouble("lon"), 0.0)
        }
    }
}

/** Yakındaki camiler ekranı: konum izni varsa telefonun konumu (bu ekranı açmak açık bir istektir), yoksa seçili yer */
@Composable
fun NearbyMosquesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val place = AppPlace.current()
    val scope = rememberCoroutineScope()
    var center by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var usedPlace by remember { mutableStateOf(false) }
    var list by remember { mutableStateOf(NearbyMosques.cached(place.lat, place.lon)) }
    var failed by remember { mutableStateOf(false) }
    var asked by remember { mutableStateOf(false) }

    fun load(useLocation: Boolean) {
        scope.launch {
            failed = false
            val loc = if (useLocation) AppPlace.locate(context) else null
            val (lat, lon) = loc?.let { it.latitude to it.longitude } ?: (place.lat to place.lon)
            usedPlace = loc == null
            center = lat to lon
            list = runCatching { NearbyMosques.load(lat, lon) }.getOrElse { failed = true; null }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> load(ok) }
    LaunchedEffect(Unit) {
        if (asked) return@LaunchedEffect
        asked = true
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        when {
            list != null -> { usedPlace = !granted; center = place.lat to place.lon }
            granted -> load(true)
            else -> permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun openMaps(uri: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }

    Column(Modifier.fillMaxSize()) {
        SubBar(L("qibla.mosques"), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (usedPlace) Note(T("qibla.mosquesNear", place.name), TextAlign.Start)
            AppCard {
                val l = list
                when {
                    failed -> {
                        Text(L("qibla.mosquesError"), color = Theme.c.muted)
                        TextButton(onClick = { load(!usedPlace) }) { Text(L("weather.retry"), color = Theme.c.accent) }
                    }
                    l == null -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Theme.c.accent)
                    }
                    l.isEmpty() -> Text(L("qibla.mosquesNone"), color = Theme.c.muted)
                    else -> l.forEachIndexed { i, m ->
                        if (i > 0) HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                openMaps("geo:${m.lat},${m.lon}?q=${m.lat},${m.lon}(${Uri.encode(m.name)})")
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Mosque, null, tint = Theme.c.accent, modifier = Modifier.size(20.dp))
                            Text("  " + m.name, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                            Text(distanceText(m.meters / 1000), color = Theme.c.muted, fontSize = 14.sp, style = Tnum)
                        }
                    }
                }
            }
            center?.let { (lat, lon) ->
                TextButton(onClick = { openMaps("geo:$lat,$lon?q=" + Uri.encode(L("qibla.mosqueQuery"))) }) {
                    Text(L("qibla.openMaps"), color = Theme.c.accent)
                }
            }
            Note(L("qibla.mosquesNoteAndroid"), TextAlign.Start)
        }
    }
}
