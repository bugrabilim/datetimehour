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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
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
    CLEAR, CLOUDY, RAIN, SNOW, FOG, STORM;

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

object SkyMath {
    /** Seçili yerde Güneş'in yüksekliği (derece): alt güneş noktasından küresel üçgen */
    fun sunAltitude(lat: Double, lon: Double, ms: Long): Double {
        val s = GlobeSky.sun(ms)
        val r = Math.PI / 180
        val v = sin(lat * r) * sin(s.lat * r) + cos(lat * r) * cos(s.lat * r) * cos((lon - s.lon) * r)
        return asin(v.coerceIn(-1.0, 1.0)) / r
    }
}

@Composable
fun AnimatedBackground(kind: FullscreenBackground, modifier: Modifier = Modifier) {
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
    var sky by remember { mutableStateOf(WeatherCache.cached(place.lat, place.lon)?.let { SkyWeather.of(it.current.code) }) }
    if (kind == FullscreenBackground.WEATHER) LaunchedEffect(place.id) {
        while (true) {
            runCatching { WeatherCache.weather(place.lat, place.lon) }.getOrNull()?.let { sky = SkyWeather.of(it.current.code) }
            kotlinx.coroutines.delay(900_000)
        }
    }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            when (kind) {
                FullscreenBackground.SKY -> sky(t, SkyMath.sunAltitude(place.lat, place.lon, System.currentTimeMillis()))
                FullscreenBackground.WEATHER -> weather(t, SkyMath.sunAltitude(place.lat, place.lon, System.currentTimeMillis()), sky ?: SkyWeather.CLEAR)
                FullscreenBackground.FLOW -> flow(t, listOf(accent, accent2, Color(0xFF6C4AB6)))
                else -> stars(t, listOf(Color(0xFF05070F), Color(0xFF111A3A)), 1f)
            }
        }
        // Veri Open-Meteo'dan: atıf (CC BY 4.0)
        if (kind == FullscreenBackground.WEATHER && sky != null) {
            WeatherAttribution(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp))
        }
    }
}

/** Gökyüzü (Güneş'in yüksekliğine göre) + hava: bulut, yağmur, kar, sis, şimşek */
private fun DrawScope.weather(t: Double, alt: Double, w: SkyWeather) {
    sky(t, alt)
    val dp = density
    when (w) {
        SkyWeather.CLEAR -> Unit
        SkyWeather.CLOUDY -> { drawRect(Color(0xFF737373).copy(alpha = 0.25f)); clouds(t * 0.8) }
        SkyWeather.FOG -> { drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.45f)))); clouds(t * 0.4) }
        SkyWeather.RAIN, SkyWeather.STORM -> {
            drawRect(Color(0xFF333333).copy(alpha = 0.35f))
            clouds(t)
            if (w == SkyWeather.STORM) {
                // Arada bir kısa şimşek (12 sn'de bir, 0,15 sn)
                val p = t % 12
                if (p < 0.15) drawRect(Color.White.copy(alpha = (0.35 * (1 - p / 0.15)).toFloat()))
            }
            var seed = 0xA1A1L
            fun rnd(): Double { seed = seed * 6364136223846793005L + 1442695040888963407L; return (seed ushr 11).toDouble() / (1L shl 53).toDouble() }
            repeat(if (w == SkyWeather.STORM) 140 else 90) {
                val x0 = rnd() * size.width
                val speed = (380 + rnd() * 220) * dp
                val len = (10 + rnd() * 14) * dp
                val y = (rnd() * size.height + t * speed) % (size.height + len) - len
                var x = (x0 - (y + len) * 0.12) % size.width
                if (x < 0) x += size.width
                drawLine(Color.White.copy(alpha = 0.45f), Offset(x.toFloat(), y.toFloat()), Offset((x - len * 0.12).toFloat(), (y + len).toFloat()), strokeWidth = 1.2f * dp)
            }
        }
        SkyWeather.SNOW -> {
            drawRect(Color(0xFF999999).copy(alpha = 0.2f))
            var seed = 0x5E0L
            fun rnd(): Double { seed = seed * 6364136223846793005L + 1442695040888963407L; return (seed ushr 11).toDouble() / (1L shl 53).toDouble() }
            repeat(110) {
                val x0 = rnd() * size.width
                val speed = (25 + rnd() * 45) * dp
                val r = ((1.2 + rnd() * 2.6) * dp).toFloat()
                val ph = rnd() * 6.28
                val y = (rnd() * size.height + t * speed) % (size.height + 10 * dp) - 5 * dp
                val x = x0 + 12 * dp * sin(t * 0.8 + ph)
                drawCircle(Color.White.copy(alpha = 0.8f), r, Offset(x.toFloat(), y.toFloat()))
            }
        }
    }
}

private fun DrawScope.gradient(colors: List<Color>) = drawRect(Brush.verticalGradient(colors))

/** Gündüz mavi + süzülen bulutlar, alacakaranlıkta turuncu-mor, gece koyu lacivert + yıldızlar */
private fun DrawScope.sky(t: Double, alt: Double) {
    when {
        alt <= -6 -> stars(t, listOf(Color(0xFF0B1026), Color(0xFF1B2350)), 1f)
        alt < 6 -> {
            gradient(listOf(Color(0xFF2B3A67), Color(0xFFC8607A), Color(0xFFF6A75F)))
            if (alt < 0) stars(t, null, (-alt / 6).toFloat())
        }
        else -> {
            gradient(listOf(Color(0xFF2F6FCB), Color(0xFF8EC9F2)))
            clouds(t)
        }
    }
    drawRect(Color.Black.copy(alpha = if (alt >= 6) 0.2f else 0.1f))
}

private fun DrawScope.clouds(t: Double) {
    val dp = density
    for (i in 0 until 4) {
        val w = size.width * (0.45f + 0.1f * (i % 2))
        val h = w * 0.28f
        val speed = (6.0 + i * 3) * dp // iOS'taki pt/sn
        val x = ((i * size.width * 0.37 + t * speed) % (size.width + w) - w).toFloat()
        val y = size.height * (0.12f + 0.17f * i)
        drawOval(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f)), Offset(x + w / 2, y + h / 2), w / 2),
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
