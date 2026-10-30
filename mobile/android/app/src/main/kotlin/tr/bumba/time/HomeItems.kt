package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import tr.bumba.time.core.PrayerTimes
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Saat ekranında saatin altında görünebilecek alanlar (iOS'taki HomeItem). Seçim ve sıra "homeItems" anahtarında:
 * bütün alanlar sırayla, kapalı olanın başında "-" ("date,week,next,-prayers,…").
 */
enum class HomeItem(val key: String, val icon: ImageVector) {
    DATE("date", Icons.Filled.CalendarMonth),
    WEEK("week", Icons.Filled.Numbers),
    NEXT("next", Icons.Filled.HourglassBottom),
    PRAYERS("prayers", Icons.Filled.NightsStay),
    UPCOMING("upcoming", Icons.Filled.Star),
    WORLD("world", Icons.Filled.Language),
    WEATHER("weather", Icons.Filled.WbSunny),
    SUN("sun", Icons.Filled.WbTwilight),
    GLOBE("globe", Icons.Filled.Public);

    val title: String get() = L("home.item.$key")

    /**
     * Alana dokununca açılacak sekme (iOS'taki Router.open); takvime gidenler günü de seçer.
     * Sekmeyi çağıran değiştirir (tam ekran önce kendini kapatır).
     */
    fun open(): Tab = when (this) {
        DATE, WEEK -> {
            Nav.calendarDay = LocalDate.now()
            Tab.CALENDAR
        }
        UPCOMING -> {
            val today = LocalDate.now(AppPlace.turkey)
            Nav.calendarDay = AppData.upcoming(today, 1).firstOrNull()?.date ?: today
            Tab.CALENDAR
        }
        NEXT, PRAYERS -> Tab.PRAYER
        WEATHER -> Tab.WEATHER
        WORLD, GLOBE -> Tab.WORLD
        SUN -> Tab.SEASONS
    }

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }

        /** Dönen dünya varsayılan olarak açık (18 Ekim 2026, sahibin isteği); "Düzenle" ile kapatılabilir */
        const val DEFAULT_RAW = "-date,-week,-next,-prayers,-upcoming,-world,-weather,-sun,-globe" // varsayılan: yalnız saat (30 Ekim 2026)
    }
}

class HomeLayout(raw: String) {
    val order: MutableList<HomeItem> = ArrayList()
    val enabled: MutableSet<HomeItem> = HashSet()

    init {
        for (part in raw.split(",")) {
            val off = part.startsWith("-")
            val item = HomeItem.of(if (off) part.drop(1) else part) ?: continue
            if (item in order) continue
            order += item
            if (!off) enabled += item
        }
        // Sonradan eklenen alanlar sona; varsayılanda açıksa açık gelir
        val added = HomeItem.entries.filter { it !in order }
        if (added.isNotEmpty()) {
            val defaults = HomeLayout(HomeItem.DEFAULT_RAW).enabled
            enabled += added.filter { it in defaults }
            order += added
        }
    }

    val raw: String get() = order.joinToString(",") { (if (it in enabled) "" else "-") + it.key }
    val visible: List<HomeItem> get() = order.filter { it in enabled }
}

/** "homeItems" tercihi (Compose durumu; değişince saat ekranı yeniden çizilir) */
object HomeItems {
    private val state by lazy { mutableStateOf(Prefs.store.getString("homeItems", null) ?: HomeItem.DEFAULT_RAW) }

    var raw: String
        get() = state.value
        set(v) {
            state.value = v
            Prefs.store.edit().putString("homeItems", v).apply()
        }

    val layout: HomeLayout get() = HomeLayout(raw)
}

/** Ayarlar › Saat ekranı: alanları aç/kapat, tutamaçtan sürükleyerek sırala */
@Composable
fun HomeItemsEditor() {
    val layout = HomeItems.layout
    val rowH = 56.dp
    val rowPx = with(LocalDensity.current) { rowH.toPx() }
    var dragging by remember { mutableStateOf<HomeItem?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard(padding = 4) {
            Column {
                layout.order.forEachIndexed { i, item ->
                    val isDragged = dragging == item
                    Row(
                        Modifier.fillMaxWidth().height(rowH)
                            .zIndex(if (isDragged) 1f else 0f)
                            .offset { IntOffset(0, if (isDragged) dragY.roundToInt() else 0) }
                            .background(if (isDragged) Theme.c.surface2 else Theme.c.surface, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(item.icon, null, tint = Theme.c.accent)
                        Spacer(Modifier.width(14.dp))
                        Text(item.title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Switch(
                            item in layout.enabled,
                            { on ->
                                val l = HomeItems.layout
                                if (on) l.enabled += item else l.enabled -= item
                                HomeItems.raw = l.raw
                                BadgeStore.hit(Badge.DUZENLE)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent),
                        )
                        Icon(
                            Icons.Filled.DragHandle, L("home.customize"), tint = Theme.c.muted,
                            modifier = Modifier.padding(start = 8.dp).size(28.dp).pointerInput(item) {
                                detectDragGestures(
                                    onDragStart = { dragging = item; dragY = 0f },
                                    onDragEnd = { dragging = null; dragY = 0f },
                                    onDragCancel = { dragging = null; dragY = 0f },
                                ) { change, amount ->
                                    change.consume()
                                    dragY += amount.y
                                    val l = HomeItems.layout
                                    val at = l.order.indexOf(item)
                                    // Komşu satırın yarısı geçilince yer değiştirir
                                    if (dragY > rowPx / 2 && at < l.order.size - 1) {
                                        l.order.add(at + 1, l.order.removeAt(at))
                                        HomeItems.raw = l.raw
                                        dragY -= rowPx
                                    } else if (dragY < -rowPx / 2 && at > 0) {
                                        l.order.add(at - 1, l.order.removeAt(at))
                                        HomeItems.raw = l.raw
                                        dragY += rowPx
                                    }
                                }
                            },
                        )
                    }
                    if (i < layout.order.size - 1) HorizontalDivider(Modifier.padding(start = 50.dp), color = Theme.c.border.copy(alpha = 0.5f))
                }
            }
        }
        Text(L("home.customizeNote"), color = Theme.c.muted, fontSize = 13.sp)
        TextButton(onClick = { HomeItems.raw = HomeItem.DEFAULT_RAW }) { Text(L("world.reset"), color = Theme.c.accent) }
    }
}

/** Saat ekranındaki bir alan; dokununca ilgili sayfa açılır */
@Composable
fun HomeItemView(item: HomeItem, now: Instant, place: AppPlace, globeHeight: Dp, onOpen: (HomeItem) -> Unit) {
    val tap = Modifier.clickable { onOpen(item) }
    when (item) {
        HomeItem.DATE -> Text(Fmt.longDate(place.today(now)), tap, color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        HomeItem.WEEK -> {
            val d = now.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            Text(T("clock.weekDay", Fmt.isoWeek(d), d.dayOfYear), tap, color = Theme.c.muted, fontSize = 14.sp)
        }
        HomeItem.NEXT -> NextPrayerCard(place, now, tap)
        HomeItem.PRAYERS -> TodayPrayersCard(place, now, tap)
        HomeItem.UPCOMING -> UpcomingDayCard(now, tap)
        HomeItem.WORLD -> WorldMiniCard(now, tap)
        HomeItem.WEATHER -> WeatherMiniCard(place, tap)
        HomeItem.SUN -> SunCard(place, now, tap)
        HomeItem.GLOBE -> Box(Modifier.fillMaxWidth().height(globeHeight).clip(RoundedCornerShape(16.dp))) {
            GlobeView(Modifier.fillMaxSize(), interactive = false)
            // Küçük kopyada dokunuş haritaya gitmez; Dünya sekmesi açılır
            Box(Modifier.matchParentSize().clickable { onOpen(item) })
        }
    }
}

/** Seçili ilin sıradaki vakti ve kalan süre */
@Composable
fun NextPrayerCard(place: AppPlace, now: Instant, modifier: Modifier = Modifier) {
    val next = place.next(now) ?: return
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(L("home.item.next"), color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${next.prayer.title} ${Fmt.hm(next.at, place.zone)}", color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Ltr {
                Text(Fmt.countdown(next.at.epochSecond - now.epochSecond), color = Theme.c.accent, fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold, style = Tnum)
            }
        }
    }
}

/** Bugünün altı vakti tek satırda; sıradaki vurgulu */
@Composable
private fun TodayPrayersCard(place: AppPlace, now: Instant, modifier: Modifier) {
    val entries = place.entries(place.today(now))
    val next = place.next(now)
    AppCard(modifier) {
        Row(Modifier.fillMaxWidth()) {
            for (e in entries) {
                val isNext = next != null && e.prayer == next.prayer && e.at == next.at
                val color = if (isNext) Theme.c.accent else Theme.c.text
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(e.prayer.title, color = color, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Ltr { Text(Fmt.hm(e.at, place.zone), color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, style = Tnum) }
                }
            }
        }
    }
}

/** "Bugün", "Yarın", "5 gün kaldı", "3 gün önce" */
fun relativeDay(d: LocalDate, today: LocalDate): String {
    val n = ChronoUnit.DAYS.between(today, d)
    return when {
        n == 0L -> L("client.today")
        n == 1L -> L("client.tomorrow")
        n > 0 -> T("client.daysLeft", n)
        else -> T("calendar.daysAgo", -n)
    }
}

/** Yaklaşan ilk özel gün (resmî tatil, dini gün…) */
@Composable
private fun UpcomingDayCard(now: Instant, modifier: Modifier) {
    val today = now.atZone(AppPlace.turkey).toLocalDate()
    val e = AppData.upcoming(today, 1).firstOrNull() ?: return
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(AppData.color(e.type), CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(e.name, color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(Fmt.longDate(e.date), color = Theme.c.muted, fontSize = 12.sp)
            }
            Text(relativeDay(e.date, today), color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Dünya saatleri listesinin ilk dört şehri */
@Composable
private fun WorldMiniCard(now: Instant, modifier: Modifier) {
    val cities = Prefs.world.split(",").mapNotNull { k -> City.all.firstOrNull { it.key == k } }.take(4)
    AppCard(modifier) {
        for (pair in cities.chunked(2)) {
            Row(Modifier.fillMaxWidth()) {
                for (c in pair) {
                    val t = now.atZone(c.zone)
                    Column(Modifier.weight(1f)) {
                        Text(c.displayName, color = Theme.c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Ltr {
                            Text(Fmt.time(t) + (if (Prefs.h12) " " + Fmt.ampm(t) else ""), color = Theme.c.text, fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold, style = Tnum)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Seçili yerde şu anki hava (15 dk önbellek) */
@Composable
private fun WeatherMiniCard(place: AppPlace, modifier: Modifier) {
    val d = weatherOverride ?: rememberWeather(place.lat, place.lon)
    Prefs.units
    AppCard(modifier) {
        if (d == null) {
            Text(L("weather.loading"), color = Theme.c.muted, fontSize = 14.sp)
            return@AppCard
        }
        val c = d.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.condition.icon(c.isDay), fontSize = 32.sp)
            Spacer(Modifier.width(12.dp))
            Text(d.condition.label, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
            Ltr {
                Column(horizontalAlignment = Alignment.End) {
                    Text(Units.temp(c.temp), color = Theme.c.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    d.today?.let { Text("↑${Units.temp(it.max)} ↓${Units.temp(it.min)}", color = Theme.c.muted, fontSize = 12.sp) }
                }
            }
        }
        Text("Open-Meteo.com", color = Theme.c.muted, fontSize = 10.sp)
    }
}

/** Güneşin doğuşu, batışı ve gün uzunluğu (vakit düzeltmeleri olmadan; Mevsimler'deki gibi) */
@Composable
private fun SunCard(place: AppPlace, now: Instant, modifier: Modifier) {
    val p = PrayerTimes.Parameters(adjustments = PrayerTimes.Adjustments(sunrise = 0.0, dhuhr = 0.0, maghrib = 0.0))
    val t = PrayerTimes.times(place.today(now), place.lat, place.lon, place.zone, p)
    val rise = t.sunrise ?: return
    val set = t.maghrib ?: return
    fun hm(h: Double) = "%02d:%02d".format(java.util.Locale.ROOT, (h * 60).toInt() / 60 % 24, (h * 60).toInt() % 60)
    val len = ((set - rise) * 60).toInt()
    AppCard(modifier) {
        Row(Modifier.fillMaxWidth()) {
            SunColumn(L("nature.sunrise"), hm(rise), "🌅", Modifier.weight(1f))
            SunColumn(L("nature.sunset"), hm(set), "🌇", Modifier.weight(1f))
            SunColumn(L("nature.dayLength"), T("seasons.dayLengthValue", len / 60, len % 60), "☀️", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SunColumn(title: String, value: String, icon: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(icon, fontSize = 18.sp)
        Ltr { Text(value, color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, style = Tnum) }
        Text(title, color = Theme.c.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
