package tr.bumba.time

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant

/**
 * Hava durumu sekmesi (iOS'taki WeatherView): seçili konum için güncel durum, saatlik (24 saat) ve 7 günlük tahmin.
 * Üstteki konum adı konum seçicidir. Veri Open-Meteo'dan (Weather.kt); atıf ekranın altında.
 */
@Composable
fun WeatherScreen() {
    val place = AppPlace.current()
    var data by remember(place.lat, place.lon) { mutableStateOf(WeatherCache.cached(place.lat, place.lon)) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    // Birim değişince yeniden çizilir
    Prefs.units
    LaunchedEffect(place.lat, place.lon, attempt) {
        failed = false
        val w = WeatherCache.weather(place.lat, place.lon, force = attempt > 0)
        if (w != null) data = w else failed = data == null
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlacePicker()
        val d = data
        when {
            d != null -> {
                Current(d)
                Hourly(d)
                Daily(d)
            }
            failed -> {
                Text(L("weather.error"), color = Theme.c.muted, textAlign = TextAlign.Center)
                TextButton(onClick = { attempt++ }) { Text(L("weather.retry"), color = Theme.c.accent) }
            }
            else -> {
                CircularProgressIndicator(Modifier.padding(top = 40.dp), color = Theme.c.accent)
                Text(L("weather.loading"), color = Theme.c.muted)
            }
        }
        WeatherAttribution()
    }
}

/** Open-Meteo atfı (CC BY 4.0; sitedeki metin), dokununca open-meteo.com */
@Composable
fun WeatherAttribution(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Text(
        L("weather.attribution"),
        modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"))) },
        color = Theme.c.muted, fontSize = 12.sp, textAlign = TextAlign.Center,
    )
}

@Composable
private fun Current(d: WeatherData) {
    val c = d.current
    AppCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(d.condition.icon(c.isDay), fontSize = 56.sp)
            Ltr { Text(Units.temp(c.temp), color = Theme.c.text, fontSize = 56.sp, fontWeight = FontWeight.SemiBold) }
            Text(d.condition.label, color = Theme.c.text, fontSize = 20.sp)
            d.today?.let { Ltr { Text("↑ ${Units.temp(it.max)}   ↓ ${Units.temp(it.min)}", color = Theme.c.muted) } }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Pill(L("weather.feelsLike"), Units.temp(c.feels), Modifier.weight(1f))
            Pill(L("weather.humidity"), "%${Math.round(c.humidity)}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            Pill(L("weather.wind"), Units.wind(c.wind), Modifier.weight(1f))
            Pill(L("weather.uv"), "${Math.round(c.uv)}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            Pill(L("weather.rainChanceShort"), "%${d.rainNow}", Modifier.weight(1f))
            Pill(L("weather.pressure"), Units.pressure(c.pressure), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Pill(title: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(title, color = Theme.c.muted, fontSize = 12.sp)
        Ltr { Text(value, color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun Hourly(d: WeatherData) {
    val now = Instant.now().minusSeconds(3600)
    val items = d.hourly.filter { it.at.isAfter(now) }.take(24)
    AppCard {
        SectionTitle(L("weather.hourlyHeading"))
        Ltr {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (h in items) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(Fmt.time(h.at, d.zone), color = Theme.c.muted, fontSize = 12.sp)
                        Text(WeatherCode.of(h.code).icon(h.isDay), fontSize = 20.sp)
                        Text(Units.temp(h.temp), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        if (h.rain >= 10) Text("%${h.rain}", color = Color(0xFF1E88E5), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Daily(d: WeatherData) {
    val days = d.daily.take(7)
    val lo = days.minOfOrNull { it.min } ?: 0.0
    val hi = days.maxOfOrNull { it.max } ?: 1.0
    AppCard {
        SectionTitle(L("weather.dailyHeading"))
        days.forEachIndexed { i, day ->
            Ltr {
                Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (i == 0) L("client.today") else Fmt.weekdayShort(day.date), Modifier.width(56.dp), color = Theme.c.text)
                    Text(WeatherCode.of(day.code).icon(), Modifier.width(32.dp), fontSize = 18.sp)
                    Text(Units.temp(day.min), Modifier.width(42.dp), color = Theme.c.muted, textAlign = TextAlign.Center)
                    BoxWithConstraints(Modifier.weight(1f).height(6.dp)) {
                        val w = maxWidth.value
                        val span = maxOf(1.0, hi - lo)
                        val x0 = ((day.min - lo) / span * w).toFloat()
                        val x1 = ((day.max - lo) / span * w).toFloat()
                        Box(Modifier.fillMaxSize().background(Theme.c.surface2, RoundedCornerShape(3.dp)))
                        Box(
                            Modifier.offset(x = x0.dp).width(maxOf(6f, x1 - x0).dp).fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFFFB8C00))), RoundedCornerShape(3.dp)),
                        )
                    }
                    Text(Units.temp(day.max), Modifier.width(42.dp), color = Theme.c.text, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Sayılar ve saatler Arapçada da soldan sağa */
@Composable
fun Ltr(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}
