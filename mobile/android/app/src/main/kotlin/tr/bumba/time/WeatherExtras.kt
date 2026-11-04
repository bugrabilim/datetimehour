package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.bumba.time.core.Outdoor
import java.time.Instant

/**
 * Hava sekmesine ekler (2. analiz #107, #111, #113, #114): saatlik eğri (sıcaklık, hissedilen, yağış, rüzgâr; dokununca
 * saat ayrıntısı), gün ayrıntısı penceresi, dışarıdaki işlere uygunluk, 3 saatlik meteogram, model karşılaştırma,
 * hava kalitesi ve polen. Veriler Open-Meteo'dan (Weather.kt); kurallar ortak çekirdekte (core/Outdoor.kt).
 */
private val RAIN_BLUE = Color(0xFF1E88E5)

/** Saatlik eğri: çiplerle ölçü seçilir, eğriye dokununca o saatin ayrıntısı */
@Composable
fun HourlyChart(d: WeatherData) {
    val now = Instant.now().minusSeconds(3600)
    val items = d.hourly.filter { it.at.isAfter(now) }.take(24)
    if (items.size < 2) return
    var metric by remember { mutableIntStateOf(0) }
    var sel by remember { mutableIntStateOf(0) }
    val labels = listOf(L("wx.temp"), L("weather.feelsLike"), L("weather.precipitation"), L("weather.wind"))
    fun value(h: WeatherData.Hour): Double = when (metric) {
        0 -> h.temp
        1 -> h.feels
        2 -> h.rain.toDouble()
        else -> h.wind
    }
    val color = when (metric) { 2 -> RAIN_BLUE; 3 -> Theme.c.muted; else -> Theme.c.accent }
    AppCard {
        SectionTitle(L("weather.hourlyHeading"))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { i, t ->
                val on = i == metric
                Text(t, Modifier.background(if (on) Theme.c.accentSoft else Theme.c.surface2, RoundedCornerShape(50))
                    .clickable { metric = i }.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (on) Theme.c.accent else Theme.c.text, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
        val values = items.map(::value)
        val lineColor = Theme.c.border
        val lo = if (metric == 2) 0.0 else values.min() - 1
        val hi = if (metric == 2) 100.0 else values.max() + 1
        Ltr {
            Canvas(Modifier.fillMaxWidth().height(120.dp).padding(vertical = 6.dp).pointerInput(items.size) {
                detectTapGestures { p -> sel = ((p.x / size.width) * (items.size - 1)).toInt().coerceIn(0, items.size - 1) }
            }) {
                val w = size.width
                val h = size.height
                fun x(i: Int) = w * i / (items.size - 1)
                fun y(v: Double) = (h - (v - lo) / (hi - lo) * h).toFloat()
                if (metric == 2) {
                    // Yağış ihtimali çubukları
                    val bw = w / items.size * 0.7f
                    values.forEachIndexed { i, v ->
                        drawRect(color.copy(alpha = if (i == sel) 0.9f else 0.5f), Offset(x(i) - bw / 2, y(v)), androidx.compose.ui.geometry.Size(bw, h - y(v)))
                    }
                } else {
                    val path = Path()
                    values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
                    val fill = Path().apply { addPath(path); lineTo(w, h); lineTo(0f, h); close() }
                    drawPath(fill, color.copy(alpha = 0.12f))
                    drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
                }
                drawLine(lineColor, Offset(x(sel), 0f), Offset(x(sel), h), strokeWidth = 1.dp.toPx())
                drawCircle(color, 5.dp.toPx(), Offset(x(sel), y(values[sel])))
            }
            Row(Modifier.fillMaxWidth()) {
                for (i in items.indices step 6) {
                    Text(Fmt.time(items[i].at, d.zone), Modifier.weight(1f), color = Theme.c.muted, fontSize = 11.sp)
                }
            }
        }
        // Seçili saatin ayrıntısı
        val h = items[sel]
        val compass = (0..7).map { L("weather.compass.$it") }
        AppCard(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(WeatherCode.of(h.code).icon(h.isDay), fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(Fmt.time(h.at, d.zone) + " · " + WeatherCode.of(h.code).label + " · " + L(if (h.isDay) "wx.day" else "wx.night"),
                        color = Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Ltr {
                        Text(
                            Units.temp(h.temp) + " · " + L("weather.feelsLike") + " " + Units.temp(h.feels) + " · " +
                                Fmt.percent(h.rain) + (if (h.mm > 0) " (" + Units.precip(h.mm) + ")" else ""),
                            color = Theme.c.muted, fontSize = 13.sp,
                        )
                    }
                    Text(
                        L("weather.wind") + " " + Units.wind(h.wind) + " · " + T("weather.windFrom", compass[tr.bumba.time.core.WeatherWords.compass8(h.dir)]) +
                            (if (h.humidity >= 0) " · " + L("weather.humidity") + " " + Fmt.percent(Math.round(h.humidity).toInt()) else ""),
                        color = Theme.c.muted, fontSize = 13.sp,
                    )
                }
            }
        }
        Text(L("wx.tapHint"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/** Gün ayrıntısı: gündüz/gece, en yüksek/en düşük, yağış, UV, gün doğumu/batımı, rüzgâr, nem aralığı */
@Composable
fun DayDetailDialog(d: WeatherData, day: WeatherData.Day, onClose: () -> Unit) {
    val hours = d.hourly.filter { it.at.atZone(d.zone).toLocalDate() == day.date }
    val dayHours = hours.filter { it.isDay }
    val nightHours = hours.filter { !it.isDay }
    val hum = hours.mapNotNull { it.humidity.takeIf { v -> v >= 0 } }
    val compass = (0..7).map { L("weather.compass.$it") }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text(L("common.done"), color = Theme.c.accent) } },
        title = { Text(Fmt.longDate(day.date), color = Theme.c.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(WeatherCode.of(day.code).icon() + "  " + WeatherCode.of(day.code).label, color = Theme.c.text, fontSize = 16.sp)
                Ltr { Text("↑ ${Units.temp(day.max)}   ↓ ${Units.temp(day.min)}", color = Theme.c.text, fontSize = 15.sp) }
                if (dayHours.isNotEmpty()) {
                    val c = dayHours.groupingBy { it.code }.eachCount().maxBy { it.value }.key
                    Text(L("wx.day") + ": " + WeatherCode.of(c).icon(true) + " " + WeatherCode.of(c).label + " · " + Units.temp(dayHours.maxOf { it.temp }),
                        color = Theme.c.muted, fontSize = 14.sp)
                }
                if (nightHours.isNotEmpty()) {
                    val c = nightHours.groupingBy { it.code }.eachCount().maxBy { it.value }.key
                    Text(L("wx.night") + ": " + WeatherCode.of(c).icon(false) + " " + WeatherCode.of(c).label + " · " + Units.temp(nightHours.minOf { it.temp }),
                        color = Theme.c.muted, fontSize = 14.sp)
                }
                Text(L("weather.rainChanceShort") + ": " + Fmt.percent(day.rain) + " · " + L("weather.precipitation") + ": " + Units.precip(day.precip),
                    color = Theme.c.muted, fontSize = 14.sp)
                if (day.windMax > 0) Text(L("wx.windMax") + ": " + Units.wind(day.windMax) + " · " + T("weather.windFrom", compass[tr.bumba.time.core.WeatherWords.compass8(day.windDir)]),
                    color = Theme.c.muted, fontSize = 14.sp)
                if (hum.isNotEmpty()) Text(L("weather.humidity") + ": " + Fmt.percent(Math.round(hum.min()).toInt()) + "–" + Fmt.percent(Math.round(hum.max()).toInt()),
                    color = Theme.c.muted, fontSize = 14.sp)
                Text(L("weather.uv") + ": " + Math.round(day.uv), color = Theme.c.muted, fontSize = 14.sp)
                Text(L("weather.sunrise") + " " + (day.sunrise?.let { Fmt.hm(it, d.zone) } ?: "–") + " · " +
                    L("weather.sunset") + " " + (day.sunset?.let { Fmt.hm(it, d.zone) } ?: "–"), color = Theme.c.muted, fontSize = 14.sp)
            }
        },
        containerColor = Theme.c.bg,
    )
}

/** Dışarıdaki işlere uygunluk: önümüzdeki 12 saat, her saat bir kare (yeşil iyi, sarı orta, gri uygun değil) ve en iyi saat */
@Composable
fun ActivitiesCard(d: WeatherData) {
    val now = Instant.now().minusSeconds(3600)
    val items = d.hourly.filter { it.at.isAfter(now) }.take(12)
    if (items.isEmpty()) return
    val hours = items.map { Outdoor.Hour(it.temp, it.feels, it.rain, it.mm, it.wind, it.gust, it.code, it.isDay, it.uv) }
    val colors = listOf(Theme.c.surface2, Color(0xFFF2B33D), Theme.c.ok)
    AppCard {
        SectionTitle(L("act.title"))
        for (a in Outdoor.Activity.entries) {
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("act.${a.key}"), Modifier.width(96.dp), color = Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Ltr {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            hours.forEach { h ->
                                Box(Modifier.weight(1f).height(16.dp).background(colors[Outdoor.score(a, h)], RoundedCornerShape(3.dp)))
                            }
                        }
                    }
                }
                val best = Outdoor.best(a, hours)
                Text(if (best != null) T("act.best", Fmt.time(items[best].at, d.zone)) + " · " + L("act.lvl.${Outdoor.score(a, hours[best])}") else L("act.none"),
                    Modifier.padding(start = 96.dp), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
        Ltr {
            Row(Modifier.fillMaxWidth().padding(start = 96.dp)) {
                for (i in items.indices step 3) Text(Fmt.time(items[i].at, d.zone), Modifier.weight(1f), color = Theme.c.muted, fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (lv in 2 downTo 0) Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(colors[lv], CircleShape))
                Text(" " + L("act.lvl.$lv"), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
        Text(L("act.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/** Meteogram: önümüzdeki 48 saat, 3 saatte bir satır (saat, simge, sıcaklık, yağış, rüzgâr, hamle, yön) */
@Composable
fun MeteogramCard(d: WeatherData) {
    val now = Instant.now().minusSeconds(3600)
    val items = d.hourly.filter { it.at.isAfter(now) }.take(48).filterIndexed { i, _ -> i % 3 == 0 }
    if (items.isEmpty()) return
    AppCard {
        SectionTitle(L("meteo.title"))
        Ltr {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    for ((t, w) in listOf("" to 1.3f, "" to 0.8f, L("wx.temp") to 1f, "mm" to 0.9f, L("weather.wind") + "\n" + Units.windUnit to 1.1f, L("weather.gusts") + "\n" + Units.windUnit to 1.1f, L("meteo.dir") to 0.7f)) {
                        Text(t, Modifier.weight(w), color = Theme.c.muted, fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 13.sp)
                    }
                }
                var lastDay = -1
                for (h in items) {
                    val z = h.at.atZone(d.zone)
                    if (z.dayOfYear != lastDay) {
                        lastDay = z.dayOfYear
                        Text(Fmt.weekdayShort(z.toLocalDate()) + " " + z.dayOfMonth, color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp))
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Fmt.time(h.at, d.zone), Modifier.weight(1.3f), color = Theme.c.text, fontSize = 13.sp)
                        Text(WeatherCode.of(h.code).icon(h.isDay), Modifier.weight(0.8f), fontSize = 16.sp, textAlign = TextAlign.Center)
                        Text(Units.temp(h.temp), Modifier.weight(1f), color = Theme.c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        Text(if (h.mm > 0) (Math.round(h.mm * 10) / 10.0).toString().replace(".", Strings.decimalPoint) else "–",
                            Modifier.weight(0.9f), color = if (h.mm > 0) RAIN_BLUE else Theme.c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Text(Units.windNumber(h.wind), Modifier.weight(1.1f), color = Theme.c.text, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Text(if (h.gust >= 0) Units.windNumber(h.gust) else "–", Modifier.weight(1.1f), color = Theme.c.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Box(Modifier.weight(0.7f), contentAlignment = Alignment.Center) {
                            Text("↑", Modifier.rotate((h.dir + 180).toFloat()), color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Model karşılaştırma (ECMWF, GFS, ICON): 5 gün, her model için en yüksek/en düşük ve yağış */
@Composable
fun ModelsCard(place: AppPlace) {
    val data by produceState<Map<String, List<ModelDay>>?>(null, place.lat, place.lon) { value = ModelCache.get(place.lat, place.lon) }
    val m = data ?: return
    if (m.isEmpty()) return
    val dates = m.values.first().map { it.date }.take(5)
    AppCard {
        SectionTitle(L("models.title"))
        Ltr {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    Text("", Modifier.weight(1f))
                    for (name in m.keys) Text(name, Modifier.weight(1.4f), color = Theme.c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
                for (date in dates) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Fmt.weekdayShort(date), Modifier.weight(1f), color = Theme.c.text, fontSize = 13.sp)
                        for (list in m.values) {
                            val x = list.firstOrNull { it.date == date }
                            Column(Modifier.weight(1.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (x != null) Units.temp(x.max) + " / " + Units.temp(x.min) else "–", color = Theme.c.text, fontSize = 13.sp)
                                if (x != null && x.mm >= 0.1) Text(Units.precip(x.mm), color = RAIN_BLUE, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        Text(L("models.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

/** Avrupa hava kalitesi endeksi renkleri (EEA): iyi … son derece kötü */
val AQI_COLORS = listOf(Color(0xFF50F0E6), Color(0xFF50CCAA), Color(0xFFF0E641), Color(0xFFFF5050), Color(0xFF960032), Color(0xFF7D2181))

/** Hava kalitesi ve polen kartı */
@Composable
fun AirCard(place: AppPlace) {
    val data by produceState<AirData?>(null, place.lat, place.lon) { value = AirCache.get(place.lat, place.lon) }
    val a = data ?: return
    val lv = Outdoor.aqiLevel(a.eaqi)
    AppCard {
        SectionTitle(L("aqi.title"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(AQI_COLORS[lv], CircleShape), contentAlignment = Alignment.Center) {
                Text("${Math.round(a.eaqi)}", color = if (lv >= 4) Color.White else Color(0xFF1A1A1A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(L("aqi.lvl.$lv"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(L("aqi.index"), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
        Ltr {
            Row(Modifier.fillMaxWidth()) {
                for ((n, v) in listOf("PM2.5" to a.pm25, "PM10" to a.pm10, "O₃" to a.ozone, "NO₂" to a.no2)) {
                    Column(Modifier.weight(1f)) {
                        Text(n, color = Theme.c.muted, fontSize = 12.sp)
                        Text(if (v.isNaN()) "–" else "${Math.round(v)}", color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("µg/m³", color = Theme.c.muted, fontSize = 10.sp)
                    }
                }
            }
        }
        val pol = a.pollen.filterValues { Outdoor.pollenLevel(it) > 0 }
        Text(L("aqi.pollen"), color = Theme.c.text, fontSize = if (AppFonts.serif) 19.sp else 18.sp, fontFamily = AppFonts.heading, fontWeight = FontWeight.Medium)
        if (pol.isEmpty()) Text(L("pollen.none"), color = Theme.c.muted, fontSize = 12.sp)
        else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((k, v) in pol) {
                val pl = Outdoor.pollenLevel(v)
                Text(L("pollen.$k") + " · " + L("pollen.lvl.$pl"),
                    Modifier.background(listOf(Theme.c.surface2, AQI_COLORS[1], AQI_COLORS[2], AQI_COLORS[3])[pl].copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Theme.c.text, fontSize = 13.sp)
            }
        }
        Text(L("aqi.source"), color = Theme.c.muted, fontSize = 11.sp)
    }
}
