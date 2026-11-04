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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
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
    // Ay takvimi alt sayfası (Ay kartına dokununca; Mevsimler'deki gibi)
    var moonPage by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (moonPage) {
        androidx.activity.compose.BackHandler { moonPage = false }
        Column(Modifier.fillMaxSize()) {
            SubBar(L("moon.calendarLink"), onBack = { moonPage = false })
            MoonScreen()
        }
        return
    }
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
                // Paylaşım kartı (2. analiz #105)
                val ctx = LocalContext.current
                TextButton(onClick = { Share.image(ctx, ShareCards.weather(place, d), "hava.png", L("share.card")) }) {
                    Text("📤 " + L("share.card"), color = Theme.c.accent)
                }
                HourlyChart(d)
                Hourly(d)
                // Dışarıdaki işlere uygunluk (2. analiz #113)
                ActivitiesCard(d)
                Details(d)
                // Hava kalitesi ve polen (2. analiz #111)
                AirCard(place)
                // Güneş yayı ve Ay (2. analiz #109)
                SunMoonArc(Instant.now(), place)
                WeatherMoonCard(place) { moonPage = true }
                Daily(d)
                // Meteogram ve model karşılaştırma (2. analiz #114)
                MeteogramCard(d)
                ModelsCard(place)
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

/** Üst kart: canlandırma açıksa (2. analiz #193) kart yok; hava sahnesi bütün sayfanın zemininde (MainActivity; 4 Kasım
 *  2026, önceden yalnız bu kartın içinde yağıyordu), yazılar koyu temanın açık renkleri */
@Composable
private fun WeatherHero(scene: WeatherScene?, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    if (scene == null) { AppCard(content = content); return }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun Current(d: WeatherData) {
    val c = d.current
    WeatherHero(if (Prefs.store.getBoolean(WeatherScene.TAB_KEY, true)) WeatherScene.of(c) else null) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(d.condition.icon(c.isDay), fontSize = 56.sp)
            Ltr { Text(Units.temp(c.temp), color = Theme.c.text, fontSize = 56.sp, fontWeight = FontWeight.SemiBold) }
            Text(d.condition.label, color = Theme.c.text, fontSize = 20.sp)
            d.today?.let { Ltr { Text("↑ ${Units.temp(it.max)}   ↓ ${Units.temp(it.min)}", color = Theme.c.muted) } }
            // Sözlü günlük özet (2. analiz #108)
            val sum = WeatherSpeech.summary(d)
            if (sum.isNotEmpty()) Text(sum, color = Theme.c.text, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
        val W = tr.bumba.time.core.WeatherWords
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Pill(L("weather.feelsLike"), Units.temp(c.feels), Modifier.weight(1f), W.feels(c.temp, c.feels)?.let { L(it) })
            Pill(L("weather.humidity"), Fmt.percent(Math.round(c.humidity).toInt()), Modifier.weight(1f), L(W.humidity(c.humidity)))
        }
        Row(Modifier.fillMaxWidth()) {
            Pill(L("weather.wind"), Units.wind(c.wind), Modifier.weight(1f), L(W.wind(c.wind)))
            Pill(L("weather.uv"), "${Math.round(c.uv)}", Modifier.weight(1f), L(W.uv(c.uv)))
        }
        Row(Modifier.fillMaxWidth()) {
            Pill(L("weather.rainChanceShort"), Fmt.percent(d.rainNow), Modifier.weight(1f))
            Pill(L("weather.pressure"), Units.pressure(c.pressure), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Pill(title: String, value: String, modifier: Modifier, note: String? = null) {
    Column(modifier) {
        Text(title, color = Theme.c.muted, fontSize = 12.sp)
        Ltr { Text(value, color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        if (note != null) Text(note, color = Theme.c.muted, fontSize = 12.sp, maxLines = 2)
    }
}

/** Ek ölçümler (2. analiz #106): rüzgâr yönü ve hamle, görüş, çiy noktası, bugünkü yağış, gün doğumu/batımı */
@Composable
private fun Details(d: WeatherData) {
    val c = d.current
    val W = tr.bumba.time.core.WeatherWords
    val compass = (0..7).map { L("weather.compass.$it") }
    AppCard {
        SectionTitle(L("weather.details"))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(L("weather.wind"), color = Theme.c.muted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ok rüzgârın gittiği yönü gösterir (gelen yön + 180°)
                    Text("↑", Modifier.rotate((c.windDir + 180).toFloat()), color = Theme.c.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Ltr { Text(Units.wind(c.wind), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                }
                Text(T("weather.windFrom", compass[W.compass8(c.windDir)]), color = Theme.c.muted, fontSize = 12.sp)
            }
            if (c.gust >= 0) Pill(L("weather.gusts"), Units.wind(c.gust), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            if (c.visibility >= 0) Pill(L("weather.visibility"), Units.distance(c.visibility), Modifier.weight(1f), L(W.visibility(c.visibility)))
            if (!c.dew.isNaN()) Pill(L("weather.dewPoint"), Units.temp(c.dew), Modifier.weight(1f), L(W.dew(c.dew)))
        }
        Row(Modifier.fillMaxWidth()) {
            Pill(L("weather.precipitation"), Units.precip(d.today?.precip ?: c.precipitation), Modifier.weight(1f))
            Pill(L("weather.rainChanceShort"), Fmt.percent(d.rainNow), Modifier.weight(1f))
        }
        d.today?.let { t ->
            Row(Modifier.fillMaxWidth()) {
                Pill(L("weather.sunrise"), t.sunrise?.let { Fmt.hm(it, d.zone) } ?: "–", Modifier.weight(1f))
                Pill(L("weather.sunset"), t.sunset?.let { Fmt.hm(it, d.zone) } ?: "–", Modifier.weight(1f))
            }
        }
    }
}

/** Ay kartı (2. analiz #109): evre, aydınlık yüzde, yeni aya ve dolunaya kalan gün, doğuş/batış; dokununca Ay takvimi */
@Composable
private fun WeatherMoonCard(place: AppPlace, onOpen: () -> Unit) {
    val now = Instant.now()
    val moon = tr.bumba.time.core.Nature.moon(now.toEpochMilli())
    val today = place.today(now)
    val rs = moonRiseSet(place, today)
    fun days(ms: Long) = Math.max(0, Math.round((ms - now.toEpochMilli()) / 86_400_000.0)).toInt()
    AppCard(Modifier.clickable(onClick = onOpen)) {
        SectionTitle(L("nature.moonHeading"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(MOON_PHASE_EMOJI[moon.phaseIndex], fontSize = 40.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(L("nature.phases.${MOON_PHASE_KEYS[moon.phaseIndex]}"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(T("nature.illumination", Math.round(moon.illumination * 100)), color = Theme.c.muted, fontSize = 13.sp)
                Text("↑ ${moonTime(rs.rise, place, "nature.moonNoRise")}   ↓ ${moonTime(rs.set, place, "nature.moonNoSet")}", color = Theme.c.text, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("🌑 " + T("weather.moonDays", days(moon.nextNewMoon)), color = Theme.c.muted, fontSize = 13.sp)
                Text("🌕 " + T("weather.moonDays", days(moon.nextFullMoon)), color = Theme.c.muted, fontSize = 13.sp)
            }
        }
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
                        if (h.rain >= 10) Text(Fmt.percent(h.rain), color = Color(0xFF1E88E5), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Daily(d: WeatherData) {
    val days = d.daily.take(14)
    // Güne dokununca ayrıntı (2. analiz #107)
    var open by remember { mutableStateOf<WeatherData.Day?>(null) }
    open?.let { DayDetailDialog(d, it) { open = null } }
    val lo = days.minOfOrNull { it.min } ?: 0.0
    val hi = days.maxOfOrNull { it.max } ?: 1.0
    AppCard {
        SectionTitle(L("weather.dailyHeading"))
        days.forEachIndexed { i, day ->
            Ltr {
                Row(Modifier.fillMaxWidth().height(36.dp).clickable { open = day }, verticalAlignment = Alignment.CenterVertically) {
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
