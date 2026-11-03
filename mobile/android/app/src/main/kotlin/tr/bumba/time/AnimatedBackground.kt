package tr.bumba.time

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Tam ekran saatin arka planı (30 Ekim 2026, rakip analizi #26): düz, fotoğraf ya da hareketli (gökyüzü, renk akışı,
 * yıldızlar). Seçim `fsBg` (iOS ile aynı); kayıt yoksa fotoğraf varsa "photo", yoksa "plain". Hareketli olanlar Canvas
 * ile çizilir; sistemde animasyonlar kapalıysa ("hareketi azalt") durağan tek kare. Gökyüzü seçili yerde Güneş'in
 * yüksekliğine göre gündüz, alacakaranlık ya da gece olur. iOS'taki AnimatedBackground ile aynı renkler ve hareket.
 */
enum class FullscreenBackground(val id: String) {
    PLAIN("plain"), PHOTO("photo"), SKY("sky"), FLOW("flow"), STARS("stars"), WEATHER("weather");

    val title: String get() = L("look.bg.$id")
    val animated: Boolean get() = this != PLAIN && this != PHOTO

    companion object {
        const val KEY = "fsBg"

        fun of(raw: String?, hasPhoto: Boolean): FullscreenBackground =
            entries.firstOrNull { it.id == raw } ?: if (hasPhoto) PHOTO else PLAIN

        val current: FullscreenBackground get() = of(Prefs.fsBg, Prefs.fsPhoto > 0)
    }
}

/** Havaya göre arka planın türü (rakip analizi listesi #79; iOS'taki SkyWeather ile aynı): WMO kodundan */
enum class SkyWeather {
    CLEAR, CLOUDY, RAIN, SNOW, FOG, STORM, WINDY;

    companion object {
        fun of(code: Int): SkyWeather = when (code) {
            0, 1 -> CLEAR
            45, 48 -> FOG
            in 51..67, in 80..82 -> RAIN
            in 71..77, 85, 86 -> SNOW
            in 95..99 -> STORM
            else -> CLOUDY
        }
    }
}

/**
 * Ekrandaki hava (3 Kasım 2026, 2. analiz #193; sahibin isteği "hava durumu neyse ekran da öyle"): tür + yoğunluk.
 * Yağış (mm/sa) damla ve kar tanesi sayısını, rüzgâr (km/sa) bulut hızını, yağmurun eğimini ve kar savrulmasını,
 * bulut oranı (0–1) bulut sayısını belirler; 30 km/sa ve üstü rüzgârda açık ya da bulutlu hava "rüzgârlı" olur
 * (yapraklar savrulur). Açık gündüz Güneş parlar, gece yıldızlar. iOS'taki WeatherScene ve sitedeki weather-fx.js ile
 * aynı kurallar.
 */
data class WeatherScene(val kind: SkyWeather, val precip: Double = 0.0, val wind: Double = 0.0, val cloud: Double = -1.0) {
    companion object {
        const val WINDY_KMH = 30.0
        const val CLOCK_KEY = "clock.weatherBg"
        const val TAB_KEY = "weather.anim"

        fun of(kind: SkyWeather, precip: Double = 0.0, wind: Double = 0.0, cloud: Double = -1.0): WeatherScene {
            val k = if ((kind == SkyWeather.CLEAR || kind == SkyWeather.CLOUDY) && wind >= WINDY_KMH) SkyWeather.WINDY else kind
            return WeatherScene(k, precip.coerceAtLeast(0.0), wind.coerceAtLeast(0.0), cloud)
        }

        fun of(c: WeatherData.Current): WeatherScene =
            of(SkyWeather.of(c.code), c.precipitation, c.wind, if (c.cloud < 0) -1.0 else c.cloud / 100)
    }

    val clouds: Int get() = when (kind) {
        SkyWeather.CLEAR -> if (cloud < 0) 0 else min(2, Math.round(cloud * 3).toInt())
        SkyWeather.FOG -> 3
        else -> if (cloud < 0) 4 else Math.round(2 + cloud * 4).toInt().coerceIn(2, 6)
    }
    val cloudSpeed: Double get() = (if (kind == SkyWeather.FOG) 0.5 else 1.0) * (1 + wind / 12)
    val drops: Int get() = when (kind) {
        SkyWeather.RAIN -> if (precip <= 0) 90 else (40 + precip * 30).toInt().coerceIn(40, 240)
        SkyWeather.STORM -> (140 + precip * 20).toInt().coerceIn(140, 300)
        else -> 0
    }
    val slant: Double get() = min(0.9, 0.08 + wind / 80)
    val flakes: Int get() = if (kind != SkyWeather.SNOW) 0 else if (precip <= 0) 110 else (60 + precip * 40).toInt().coerceIn(60, 240)
    val leaves: Int get() = when (kind) {
        SkyWeather.WINDY -> (8 + ((wind - WINDY_KMH) / 3).toInt()).coerceIn(8, 30)
        SkyWeather.STORM, SkyWeather.RAIN -> if (wind >= WINDY_KMH) 10 else 0
        else -> 0
    }
}

object SkyMath {
    /** Seçili yerde Güneş'in yüksekliği (derece): alt güneş noktasından küresel üçgen */
    fun sunAltitude(lat: Double, lon: Double, ms: Long): Double {
        val s = GlobeSky.sun(ms)
        val r = Math.PI / 180
        val v = sin(lat * r) * sin(s.lat * r) + cos(lat * r) * cos(s.lat * r) * cos((lon - s.lon) * r)
        return asin(v.coerceIn(-1.0, 1.0)) / r
    }
}

/**
 * Hareketli arka plan. Hava türünde `scene` verilirse o çizilir (hava sekmesi), yoksa seçili yerin havası alınır;
 * `attribution` Open-Meteo atfını gösterir (hava sekmesinde atıf zaten sayfada).
 */
@Composable
fun AnimatedBackground(kind: FullscreenBackground, modifier: Modifier = Modifier, scene: WeatherScene? = null, attribution: Boolean = true) {
    val context = LocalContext.current
    val still = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
    var t by remember { mutableDoubleStateOf(0.0) }
    // Sonsuz animasyon karesi: testlerde (ekran görüntüsü) çalışmaz, tek kare çizilir
    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) withInfiniteAnimationFrameMillis { t = (it - start) / 1000.0 }
    }
    val place = AppPlace.current()
    val accent = Theme.c.accent
    val accent2 = Theme.c.accent2
    // Hava durumu arka planı: seçili yerin güncel havası (Open-Meteo, 15 dk önbellek)
    var fetched by remember { mutableStateOf(WeatherCache.cached(place.lat, place.lon)?.let { WeatherScene.of(it.current) }) }
    if (kind == FullscreenBackground.WEATHER && scene == null) LaunchedEffect(place.id) {
        while (true) {
            runCatching { WeatherCache.weather(place.lat, place.lon) }.getOrNull()?.let { fetched = WeatherScene.of(it.current) }
            kotlinx.coroutines.delay(900_000)
        }
    }
    val sky = scene ?: fetched
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            when (kind) {
                FullscreenBackground.SKY -> sky(t, SkyMath.sunAltitude(place.lat, place.lon, System.currentTimeMillis()))
                FullscreenBackground.WEATHER -> weather(t, SkyMath.sunAltitude(place.lat, place.lon, System.currentTimeMillis()), sky ?: WeatherScene(SkyWeather.CLEAR))
                FullscreenBackground.FLOW -> flow(t, listOf(accent, accent2, Color(0xFF6C4AB6)))
                else -> stars(t, listOf(Color(0xFF05070F), Color(0xFF111A3A)), 1f)
            }
        }
        // Veri Open-Meteo'dan: atıf (CC BY 4.0)
        if (kind == FullscreenBackground.WEATHER && sky != null && attribution) {
            WeatherAttribution(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp))
        }
    }
}

/** Gökyüzü (Güneş'in yüksekliğine göre) + hava: bulut, yağmur, kar, sis, şimşek, rüzgâr; yoğunluk sahneden */
private fun DrawScope.weather(t: Double, alt: Double, scene: WeatherScene) {
    val w = scene.kind
    sky(t, alt, drawClouds = false)
    val dp = density
    fun rng(s0: Long): () -> Double {
        var seed = s0
        return { seed = seed * 6364136223846793005L + 1442695040888963407L; (seed ushr 11).toDouble() / (1L shl 53).toDouble() }
    }
    when (w) {
        SkyWeather.CLEAR, SkyWeather.WINDY -> if (alt >= 6) {
            // Açık gündüz: Güneş parlaması (hafif nabız)
            val c = Offset(size.width * 0.82f, size.height * 0.12f)
            val r = size.width * 0.5f
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3C4).copy(alpha = (0.45 + 0.08 * sin(t * 0.5)).toFloat()), Color.Transparent), c, r), r, c)
        }
        SkyWeather.CLOUDY -> drawRect(Color(0xFF737373).copy(alpha = (0.15 + 0.15 * scene.cloud.coerceAtLeast(0.0)).toFloat()))
        SkyWeather.FOG -> drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.45f))))
        SkyWeather.RAIN, SkyWeather.STORM -> drawRect(Color(0xFF333333).copy(alpha = 0.35f))
        SkyWeather.SNOW -> drawRect(Color(0xFF999999).copy(alpha = 0.2f))
    }
    clouds(t, scene.clouds, scene.cloudSpeed, dark = w == SkyWeather.RAIN || w == SkyWeather.STORM)
    if (w == SkyWeather.STORM) {
        // Arada bir kısa şimşek (12 sn'de bir, 0,15 sn)
        val p = t % 12
        if (p < 0.15) drawRect(Color.White.copy(alpha = (0.35 * (1 - p / 0.15)).toFloat()))
    }
    if (scene.drops > 0) {
        val rnd = rng(0xA1A1L)
        val k = scene.slant
        repeat(scene.drops) {
            val x0 = rnd() * size.width
            val speed = (380 + rnd() * 220) * dp
            val len = (10 + rnd() * 14) * dp
            val y = (rnd() * size.height + t * speed) % (size.height + len) - len
            var x = (x0 - (y + len) * k) % size.width
            if (x < 0) x += size.width
            drawLine(Color.White.copy(alpha = 0.45f), Offset(x.toFloat(), y.toFloat()), Offset((x - len * k).toFloat(), (y + len).toFloat()), strokeWidth = 1.2f * dp)
        }
    }
    if (scene.flakes > 0) {
        val rnd = rng(0x5E0L)
        val drift = scene.wind * 1.2 * dp
        repeat(scene.flakes) {
            val x0 = rnd() * size.width
            val speed = (25 + rnd() * 45) * dp
            val r = ((1.2 + rnd() * 2.6) * dp).toFloat()
            val ph = rnd() * 6.28
            val y = (rnd() * size.height + t * speed) % (size.height + 10 * dp) - 5 * dp
            var x = (x0 + t * drift + 12 * dp * sin(t * 0.8 + ph)) % size.width
            if (x < 0) x += size.width
            drawCircle(Color.White.copy(alpha = 0.8f), r, Offset(x.toFloat(), y.toFloat()))
        }
    }
    if (scene.leaves > 0) {
        // Savrulan yapraklar: hız rüzgârla artar, kendi çevresinde döner
        val rnd = rng(0x1EAFL)
        val colors = listOf(Color(0xFFE8590C), Color(0xFF82C91E), Color(0xFFB07D2B), Color(0xFFF59F00))
        repeat(scene.leaves) { i ->
            val speed = (90 + scene.wind * 3) * (0.7 + rnd() * 0.6) * dp
            val y0 = rnd() * size.height * 0.9
            val ph = rnd() * 6.28
            val x = ((rnd() * (size.width + 40 * dp) + t * speed) % (size.width + 40 * dp) - 20 * dp).toFloat()
            val y = (y0 + 30 * dp * sin(t * 1.5 + ph)).toFloat()
            rotate(((t * 3 + ph) * 180 / Math.PI).toFloat(), Offset(x, y)) {
                drawOval(colors[i % colors.size].copy(alpha = 0.85f), Offset(x - 6 * dp, y - 3 * dp), Size(12 * dp, 6 * dp))
            }
        }
    }
}

private fun DrawScope.gradient(colors: List<Color>) = drawRect(Brush.verticalGradient(colors))

/** Gündüz mavi + süzülen bulutlar, alacakaranlıkta turuncu-mor, gece koyu lacivert + yıldızlar */
private fun DrawScope.sky(t: Double, alt: Double, drawClouds: Boolean = true) {
    when {
        alt <= -6 -> stars(t, listOf(Color(0xFF0B1026), Color(0xFF1B2350)), 1f)
        alt < 6 -> {
            gradient(listOf(Color(0xFF2B3A67), Color(0xFFC8607A), Color(0xFFF6A75F)))
            if (alt < 0) stars(t, null, (-alt / 6).toFloat())
        }
        else -> {
            gradient(listOf(Color(0xFF2F6FCB), Color(0xFF8EC9F2)))
            if (drawClouds) clouds(t)
        }
    }
    drawRect(Color.Black.copy(alpha = if (alt >= 6) 0.2f else 0.1f))
}

/** Süzülen bulutlar: sayı bulut oranından, hız rüzgârdan; yağmurda koyu */
private fun DrawScope.clouds(t: Double, count: Int = 4, speedK: Double = 1.0, dark: Boolean = false) {
    val dp = density
    val c = if (dark) Color(0xFFBFBFBF) else Color.White
    for (i in 0 until count) {
        val w = size.width * (0.45f + 0.1f * (i % 2))
        val h = w * 0.28f
        val speed = (6.0 + (i % 4) * 3) * speedK * dp // iOS'taki pt/sn
        val x = ((i * size.width * 0.37 + t * speed) % (size.width + w) - w).toFloat()
        val y = size.height * (0.12f + 0.17f * (i % 4) + 0.08f * (i / 4))
        drawOval(
            Brush.radialGradient(listOf(c.copy(alpha = 0.55f), c.copy(alpha = 0f)), Offset(x + w / 2, y + h / 2), w / 2),
            Offset(x, y), Size(w, h),
        )
    }
}

/** Koyu zemin üstünde dönerek kayan üç renk lekesi (uygulama renginden) */
private fun DrawScope.flow(t: Double, colors: List<Color>) {
    gradient(listOf(Color(0xFF0B1020), Color(0xFF141A33)))
    val r = max(size.width, size.height) * 0.6f
    colors.forEachIndexed { k, c ->
        val p = Offset(
            (size.width * (0.5 + 0.35 * sin(t * 0.07 + k * 2.1))).toFloat(),
            (size.height * (0.5 + 0.35 * cos(t * 0.05 + k * 1.7))).toFloat(),
        )
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.55f), c.copy(alpha = 0f)), p, r), r, p)
    }
}

/** Sabit tohumlu yıldızlar; her biri kendi hızıyla göz kırpar, gökyüzü çok yavaş kayar */
private fun DrawScope.stars(t: Double, base: List<Color>?, alpha: Float) {
    if (base != null) gradient(base)
    var seed = 0x5EEDL
    fun rnd(): Double {
        seed = seed * 6364136223846793005L + 1442695040888963407L
        return (seed ushr 11).toDouble() / (1L shl 53).toDouble()
    }
    val dp = density
    val drift = t * 2 * dp
    repeat(140) {
        val x = ((rnd() * size.width + drift) % size.width).toFloat()
        val y = (rnd() * size.height).toFloat()
        val s = ((0.6 + rnd() * 1.6) * dp).toFloat()
        val phase = rnd() * 6.28
        val speed = 0.5 + rnd() * 1.5
        val a = ((0.35 + 0.65 * (0.5 + 0.5 * sin(t * speed + phase))) * alpha).toFloat()
        drawCircle(Color.White.copy(alpha = a.coerceIn(0f, 1f)), s / 2, Offset(x, y))
    }
}
