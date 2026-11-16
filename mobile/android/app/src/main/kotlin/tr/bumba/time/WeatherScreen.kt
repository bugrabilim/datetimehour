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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.zIndex
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
                val ctx = LocalContext.current
                var editing by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Paylaşım kartı (2. analiz #105)
                    TextButton(onClick = { Share.image(ctx, ShareCards.weather(place, d), "hava.png", L("share.card")) }) {
                        DuoIcon("share", 18.dp, Theme.c.accent); Spacer(Modifier.width(6.dp)); Text(L("share.card"), color = Theme.c.accent)
                    }
                    TextButton(onClick = { editing = true }) { Text(L("weather.cards"), color = Theme.c.accent) }
                }
                // Kartlar kişinin sırasıyla (16 Kasım 2026, sahibin isteği): saatlik ve 10 günlük tahmin varsayılan olarak en
                // üstte; bir kartı basılı tutup sürükleyerek ya da "Kartları sırala"dan yeri değişir
                var orderRaw by remember { mutableStateOf(Prefs.store.getString(WeatherCards.KEY, "") ?: "") }
                ReorderColumn(WeatherCards.order(orderRaw), onOrder = {
                    orderRaw = it.joinToString(","); Prefs.store.edit().putString(WeatherCards.KEY, orderRaw).apply()
                }) { id -> WeatherCardContent(id, d, place) { moonPage = true } }
                if (editing) WeatherCardsDialog(WeatherCards.order(orderRaw), onOrder = {
                    orderRaw = it.joinToString(","); Prefs.store.edit().putString(WeatherCards.KEY, orderRaw).apply()
                }, onDismiss = { editing = false })
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

/** Hava ekranındaki kartların sırası (16 Kasım 2026, sahibin isteği; iOS'ta aynı kimlikler ve anahtar, iOS'ta ek "alerts").
 *  Kayıtta olmayan yeni kart sona eklenir. */
object WeatherCards {
    const val KEY = "weather.order"
    val ALL = listOf("hourly", "daily", "now", "chart", "activities", "details", "air", "sun", "moon", "meteogram", "models", "report", "live")

    fun order(raw: String): List<String> {
        val out = raw.split(",").filter { it in ALL }.distinct().toMutableList()
        ALL.forEach { if (it !in out) out += it }
        return out
    }

    fun title(id: String): String = when (id) {
        "hourly" -> L("weather.hourlyHeading")
        "daily" -> L("weather.dailyHeading10")
        "now" -> L("weather.now")
        "chart" -> L("weather.card.chart")
        "activities" -> L("act.title")
        "details" -> L("weather.details")
        "air" -> L("aqi.title")
        "sun" -> L("weather.card.sun")
        "moon" -> L("nature.moonHeading")
        "meteogram" -> L("meteo.title")
        "models" -> L("models.title")
        "report" -> L("wxr.title")
        else -> L("live.weather.toggleAndroid")
    }
}

@Composable
private fun WeatherCardContent(id: String, d: WeatherData, place: AppPlace, openMoon: () -> Unit) {
    when (id) {
        "hourly" -> Hourly(d)
        "daily" -> Daily(d)
        "now" -> NowCard(d)
        "chart" -> HourlyChart(d)
        // Dışarıdaki işlere uygunluk (2. analiz #113)
        "activities" -> ActivitiesCard(d)
        "details" -> Details(d)
        // Hava kalitesi ve polen (2. analiz #111)
        "air" -> AirCard(place)
        // Güneş yayı ve Ay (2. analiz #109)
        "sun" -> SunMoonArc(Instant.now(), place)
        "moon" -> WeatherMoonCard(place, openMoon)
        // Meteogram ve model karşılaştırma (2. analiz #114)
        "meteogram" -> MeteogramCard(d)
        "models" -> ModelsCard(place)
        // Anonim hava bildirimi (üyelik aşama 2d; girişliyken, sunucuda açıksa)
        "report" -> WeatherReportCard(place.lat, place.lon)
        "live" -> LiveWeatherCard()
    }
}

/** Bildirim alanında ve kilit ekranında hava (4 Kasım 2026; iOS'taki Dynamic Island karşılığı, kolay aç/kapa) */
@Composable
private fun LiveWeatherCard() {
    val ctx = LocalContext.current
    var liveWeather by remember { mutableStateOf(Prefs.store.getBoolean(LiveNotifications.WEATHER_KEY, false)) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(L("live.weather.toggleAndroid"), color = Theme.c.text, fontSize = 15.sp)
                Text(L("live.weather.noteAndroid"), color = Theme.c.muted, fontSize = 12.sp)
            }
            SoftSwitch(liveWeather, {
                liveWeather = it
                LiveNotifications.setWeather(ctx, it)
            })
        }
    }
}

/** Kartları basılı tutup sürükleyerek sıralayan sütun. Sürüklenen kartın görünür ortası başka bir kartın üstüne gelince
 *  sıra değişir; kart parmağın altında kalsın diye kaydırma, kartın yeni yerine göre hesaplanır. */
@Composable
fun ReorderColumn(items: List<String>, onOrder: (List<String>) -> Unit, content: @Composable (String) -> Unit) {
    var list by remember(items) { mutableStateOf(items) }
    val tops = remember { androidx.compose.runtime.mutableStateMapOf<String, Float>() }
    val heights = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var startTop by remember { mutableStateOf(0f) }
    var moved by remember { mutableStateOf(0f) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        list.forEach { id ->
            androidx.compose.runtime.key(id) {
                val isDragged = dragging == id
                Box(
                    Modifier
                        .onGloballyPositioned { tops[id] = it.positionInParent().y; heights[id] = it.size.height }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            if (isDragged) {
                                translationY = startTop + moved - (tops[id] ?: startTop)
                                scaleX = 1.02f; scaleY = 1.02f; alpha = 0.95f
                            }
                        }
                        .pointerInput(id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragging = id; startTop = tops[id] ?: 0f; moved = 0f
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    moved += amount.y
                                    val center = startTop + moved + (heights[id] ?: 0) / 2f
                                    val target = list.firstOrNull { o ->
                                        o != id && (tops[o] ?: return@firstOrNull false).let { t -> center >= t && center <= t + (heights[o] ?: 0) }
                                    }
                                    if (target != null) {
                                        val l = list.toMutableList()
                                        l.remove(id)
                                        l.add(list.indexOf(target).coerceAtMost(l.size), id)
                                        list = l
                                    }
                                },
                                onDragEnd = { dragging = null; moved = 0f; onOrder(list) },
                                onDragCancel = { dragging = null; moved = 0f; onOrder(list) },
                            )
                        },
                ) { content(id) }
            }
        }
    }
}

/** "Kartları sırala": yukarı/aşağı düğmeleriyle sıralama (ekran okuyucu ve sürüklemeyi sevmeyenler için) */
@Composable
private fun WeatherCardsDialog(order: List<String>, onOrder: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var list by remember { mutableStateOf(order) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onOrder(list); onDismiss() },
        title = { Text(L("weather.cards")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(L("weather.cardsHint"), color = Theme.c.muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                list.forEachIndexed { i, id ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(WeatherCards.title(id), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                        TextButton(onClick = { list = list.toMutableList().apply { add(i - 1, removeAt(i)) } }, enabled = i > 0) { Text("↑") }
                        TextButton(onClick = { list = list.toMutableList().apply { add(i + 1, removeAt(i)) } }, enabled = i < list.size - 1) { Text("↓") }
                    }
                }
                TextButton(onClick = { list = WeatherCards.ALL }) { Text(L("world.reset"), color = Theme.c.accent) }
            }
        },
        confirmButton = { TextButton({ onOrder(list); onDismiss() }) { Text(L("common.done"), color = Theme.c.accent) } },
    )
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
            // Simge ve sıcaklık yan yana (16 Kasım 2026): tahminler ilk ekrana sığsın
            Ltr {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    WeatherIcon(d.condition, c.isDay, 52.dp)
                    Text(Units.temp(c.temp), color = Theme.c.text, fontSize = 56.sp, fontFamily = AppFonts.numerals, fontWeight = FontWeight.Medium)
                }
            }
            Text(d.condition.label, color = Theme.c.text, fontSize = 20.sp)
            d.today?.let { Ltr { Text("↑ ${Units.temp(it.max)}   ↓ ${Units.temp(it.min)}", color = Theme.c.muted) } }
            // Sözlü günlük özet (2. analiz #108)
            val sum = WeatherSpeech.summary(d)
            if (sum.isNotEmpty()) Text(sum, color = Theme.c.text, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Şu anki ölçümler; 16 Kasım 2026'ya kadar üst kartın içindeydi */
@Composable
private fun NowCard(d: WeatherData) {
    val c = d.current
    AppCard {
        SectionTitle(L("weather.now"))
        val W = tr.bumba.time.core.WeatherWords
        Row(Modifier.fillMaxWidth()) {
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
                        WeatherIcon(WeatherCode.of(h.code), h.isDay, 24.dp)
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
                    Box(Modifier.width(32.dp)) { WeatherIcon(WeatherCode.of(day.code), size = 22.dp) }
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
