package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import tr.bumba.time.core.Nature
import tr.bumba.time.core.PhotoLight
import tr.bumba.time.core.WeatherWords
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Fotoğraf ışığı (9 Kasım 2026, sahibin onayı; iOS PhotoLightView ve sitedeki fotoğraf ışığı sayfasıyla aynı içerik):
 * seçili yer ve gün için sabah/akşam mavi ve altın saat, gün doğumu/batımı saati ve yönü, Ay (doğuş, batış, evre), bulut
 * tahmininden bugün ve sonraki 6 günün gün doğumu/batımı fotoğrafı tahmini (iyi / orta / kötü) ve 7 günlük ışık tablosu.
 * Saatler telefonda (core PhotoLight; Mevsimler'deki tanımlar), bulut tahmini Open-Meteo'dan (yalnız yaklaşık konum).
 */

/** Tercihler ("prefs"; iOS'taki PhotoPrefs ile aynı anahtarlar) */
object PhotoPrefs {
    /** Ekran bir kez açıldı mı: bildirimler ancak o zaman kurulur (uyku planındaki yatma hatırlatması gibi) */
    const val USED = "photo.used"
    const val LEAD = "notify.photo.lead"
    const val EVENTS = "notify.photo.events"
    val EVENT_IDS = listOf("amBlue", "amGold", "pmGold", "pmBlue")
    val LEADS = listOf(10, 20, 30, 60)

    var used: Boolean
        get() = Prefs.store.getBoolean(USED, false)
        set(v) = Prefs.store.edit().putBoolean(USED, v).apply()
    var lead: Int
        get() = Prefs.store.getInt(LEAD, 20)
        set(v) = Prefs.store.edit().putInt(LEAD, v).apply()
    var events: Set<String>
        get() = (Prefs.store.getString(EVENTS, "pmGold") ?: "pmGold").split(",").filter { it.isNotEmpty() }.toSet()
        set(v) = Prefs.store.edit().putString(EVENTS, EVENT_IDS.filter { it in v }.joinToString(",")).apply()

    /** Olayın adı (Mevsimler'deki metinler) */
    fun label(e: String): String = L(when (e) {
        "amBlue" -> "nature.blueAm"; "amGold" -> "nature.goldenAm"; "pmGold" -> "nature.goldenPm"; else -> "nature.bluePm"
    })
}

/**
 * Bulut tahmini (Open-Meteo, anahtarsız): saatlik alçak/orta/yüksek bulut ve yağış, 7 gün. Koordinat 2 ondalık, aynı yer
 * 30 dk önbellekte (hava ekranındaki önbellekler gibi).
 */
object PhotoSky {
    private var cache: Triple<String, Long, List<PhotoLight.Sample>>? = null
    private fun key(lat: Double, lon: Double) = String.format(Locale.ROOT, "%.2f,%.2f", lat, lon)

    /** Ekran görüntüsü testleri için: ağ olmadan örnek veri */
    fun seed(lat: Double, lon: Double, samples: List<PhotoLight.Sample>) {
        cache = Triple(key(lat, lon), System.currentTimeMillis(), samples)
    }

    /** Önbellekteki son veri (bildirimler ağa çıkmaz, yalnız bunu kullanır) */
    fun cached(lat: Double, lon: Double): List<PhotoLight.Sample>? = cache?.takeIf { it.first == key(lat, lon) }?.third

    suspend fun get(lat: Double, lon: Double): List<PhotoLight.Sample>? {
        val k = key(lat, lon)
        cache?.let { if (it.first == k && System.currentTimeMillis() - it.second < 30 * 60_000) return it.third }
        val v = withContext(Dispatchers.IO) {
            runCatching {
                val (la, lo) = k.split(",")
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$la&longitude=$lo" +
                    "&hourly=cloud_cover_low,cloud_cover_mid,cloud_cover_high,precipitation&forecast_days=7&timezone=auto&timeformat=unixtime"
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 10_000; c.readTimeout = 15_000
                val body = try {
                    if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                    c.inputStream.bufferedReader().use { it.readText() }
                } finally { c.disconnect() }
                parse(JSONObject(body))
            }.getOrNull()
        }
        if (!v.isNullOrEmpty()) cache = Triple(k, System.currentTimeMillis(), v)
        return v?.takeIf { it.isNotEmpty() } ?: cached(lat, lon)
    }

    /** Open-Meteo yanıtı (timeformat=unixtime): eksik değerli saatler atlanır */
    fun parse(o: JSONObject): List<PhotoLight.Sample> {
        val h = o.getJSONObject("hourly")
        val t = h.getJSONArray("time")
        fun arr(n: String) = h.optJSONArray(n)
        val low = arr("cloud_cover_low"); val mid = arr("cloud_cover_mid"); val high = arr("cloud_cover_high"); val pr = arr("precipitation")
        return (0 until t.length()).mapNotNull { i ->
            if (low == null || mid == null || high == null || low.isNull(i) || mid.isNull(i) || high.isNull(i)) return@mapNotNull null
            PhotoLight.Sample(t.getLong(i) * 1000, low.getDouble(i), mid.getDouble(i), high.getDouble(i),
                pr?.takeIf { !it.isNull(i) }?.getDouble(i) ?: 0.0)
        }
    }
}

/** Bir yer ve günün ışığı; ondalık saatler o günün yerel öğlesinden sayılır (yaz saati geçişinde de doğru an) */
class PhotoDay(val date: LocalDate, val place: AppPlace) {
    private val noon: Instant = date.atTime(12, 0).atZone(place.zone).toInstant()
    private val offset: Double = place.zone.rules.getOffset(noon).totalSeconds / 3600.0
    val day: PhotoLight.Day = PhotoLight.day(date.year, date.monthValue, date.dayOfMonth, place.lat, place.lon, offset)

    /** Ondalık saat → an (dakikaya yuvarlanır; Mevsimler'deki saatlerle aynı) */
    fun at(h: Double?): Instant? = h?.let { noon.plusSeconds((Math.round(it * 60) - 12 * 60) * 60) }

    val sunrise: Instant? get() = at(day.sunrise)
    val sunset: Instant? get() = at(day.sunset)

    /** Herhangi bir eşik o gün oluşmuyor mu ("—" notu için) */
    val partial: Boolean get() = day.polar == 0 && listOf(day.light.blueAm, day.light.goldenAm, day.light.goldenPm, day.light.bluePm)
        .any { it.first == null || it.second == null }

    /** Olayın başladığı an: amBlue, amGold, pmGold, pmBlue */
    fun start(event: String): Instant? = at(when (event) {
        "amBlue" -> day.light.blueAm.first; "amGold" -> day.light.goldenAm.first
        "pmGold" -> day.light.goldenPm.first; else -> day.light.bluePm.first
    })

    /** Saat (kullanıcının 24/12 seçimiyle) */
    fun clock(i: Instant?): String = i?.let { listOf(Fmt.time(it, place.zone), Fmt.ampm(it.atZone(place.zone))).filter { s -> s.isNotEmpty() }.joinToString(" ") } ?: "—"

    /** "06:41–07:28" (12 saatte "6:41–7:28 AM"; öğleden önce ve sonra ayrıysa ikisi de) */
    fun span(r: Pair<Double?, Double?>, ampm: Boolean = true): String {
        val a = at(r.first); val b = at(r.second)
        if (a == null && b == null) return "—"
        if (a == null || b == null || !ampm) return listOf(a, b).joinToString(if (a == null || b == null) " – " else "–") { it?.let { x -> Fmt.time(x, place.zone) } ?: "—" }
        val pa = Fmt.ampm(a.atZone(place.zone)); val pb = Fmt.ampm(b.atZone(place.zone))
        val left = Fmt.time(a, place.zone) + if (pa.isNotEmpty() && pa != pb) " $pa" else ""
        return "$left–" + clock(b)
    }

    /** "108° Doğu" (kuzeyden saat yönünde; 8 yönün adı) */
    fun direction(az: Double?): String =
        az?.let { "${it.roundToInt() % 360}° " + L("weather.compass.${WeatherWords.compass8(it)}") } ?: ""
}

/** Ekran: yer seçici, gün gezintisi, sabah ve akşam ışığı, Ay, bulut tahmini, 7 günlük tablo */
@Composable
fun PhotoLightScreen() {
    val context = LocalContext.current
    val place = AppPlace.current()
    val today = place.today()
    var date by remember(place.id) { mutableStateOf(today) }
    // İlk açılış: bildirimler artık kurulabilir
    LaunchedEffect(Unit) {
        if (!PhotoPrefs.used) { PhotoPrefs.used = true; FeatureNotifier.reschedule(context) }
    }
    var samples by remember(place.lat, place.lon) { mutableStateOf(PhotoSky.cached(place.lat, place.lon)) }
    var failed by remember(place.lat, place.lon) { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(place.lat, place.lon, attempt) {
        failed = false
        val v = PhotoSky.get(place.lat, place.lon)
        if (v != null) samples = v else failed = samples == null
    }
    val p = remember(place.id, place.lat, place.lon, date) { PhotoDay(date, place) }
    val week = remember(place.id, place.lat, place.lon, today) { (0..6).map { PhotoDay(today.plusDays(it.toLong()), place) } }
    fun quality(i: Instant?) = i?.let { t -> samples?.let { PhotoLight.qualityAt(it, t.toEpochMilli()) } }
    val c = Theme.c

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(L("photoApp.subtitle"), color = c.muted, fontSize = 14.sp)
        // Uzun açıklamalar ekranda değil "Nasıl çalışır?" alt sayfasında (doğal tasarım madde 13)
        HowItWorks(photoHowSections())
        PlacePicker()
        // Gün gezintisi: önceki / sonraki gün, bugüne dön
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (date > today.minusDays(366)) date = date.minusDays(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("photoApp.prevDay"), tint = c.accent)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.longDate(date), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                if (date != today) Text(L("client.today"), Modifier.clip(RoundedCornerShape(50)).clickable { date = today }
                    .padding(horizontal = 10.dp, vertical = 2.dp), color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = { if (date < today.plusDays(366)) date = date.plusDays(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("photoApp.nextDay"), tint = c.accent)
            }
        }
        when (p.day.polar) {
            1 -> Text(L("photoApp.polarDay"), color = c.muted, fontSize = 13.sp)
            -1 -> Text(L("photoApp.polarNight"), color = c.muted, fontSize = 13.sp)
        }
        val light = p.day.light
        LightCard(L("photoApp.morning"), quality(p.sunrise)) {
            LightRow("sunrise", c.sky, L("photoApp.blue"), p.span(light.blueAm))
            LightRow("sunrise", c.gold, L("photoApp.golden"), p.span(light.goldenAm))
            LightRow("sun", c.terra, L("nature.sunrise"), p.sunrise?.let { p.clock(it) + " · " + p.direction(p.day.riseAzimuth) } ?: "—")
        }
        LightCard(L("photoApp.evening"), quality(p.sunset)) {
            LightRow("sunset", c.gold, L("photoApp.golden"), p.span(light.goldenPm))
            LightRow("sun", c.terra, L("nature.sunset"), p.sunset?.let { p.clock(it) + " · " + p.direction(p.day.setAzimuth) } ?: "—")
            LightRow("sunset", c.sky, L("photoApp.blue"), p.span(light.bluePm))
        }
        if (p.partial) Text(L("photoApp.partial"), color = c.muted, fontSize = 13.sp)
        MoonCard(p, date == today)
        ForecastCard(week, date, samples, failed, onPick = { date = it }, onRetry = { attempt++ })
        WeekCard(week, date) { date = it }
    }
}

/** "Nasıl çalışır?" bölümleri (iOS'takiyle aynı) */
fun photoHowSections(): List<Pair<String?, String>> = listOf(
    L("photoApp.subtitle") to L("nature.lightNote"),
    L("photoApp.how.forecastTitle") to T("photoApp.how.forecast", Fmt.percent(75), Fmt.percent(40), Fmt.percent(20), Fmt.percent(80), Fmt.percent(60)),
    L("photoApp.how.dirTitle") to L("photoApp.how.dir"),
    L("settings.privacy") to L("photoApp.how.privacy"),
)

@Composable
private fun LightCard(title: String, q: PhotoLight.Quality?, content: @Composable () -> Unit) {
    AppCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(title, Modifier.weight(1f))
            if (q != null) QualityPill(q)
        }
        content()
    }
}

@Composable
private fun LightRow(icon: String, tint: Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 34.dp), verticalAlignment = Alignment.CenterVertically) {
        DuoIcon(icon, 18.dp, tint)
        Spacer(Modifier.width(8.dp))
        Text(label, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        Text(value, Modifier.padding(start = 8.dp), color = Theme.c.text, fontSize = 15.sp, style = Tnum, textAlign = TextAlign.End)
    }
}

@Composable
private fun qualityColor(q: PhotoLight.Quality): Color = when (q) {
    PhotoLight.Quality.GOOD -> Theme.c.sage
    PhotoLight.Quality.FAIR -> Theme.c.gold
    PhotoLight.Quality.POOR -> Theme.c.muted
}

/** Tahmin hapı: renkli nokta ve ad (renk tek başına anlam taşımaz) */
@Composable
private fun QualityPill(q: PhotoLight.Quality?, modifier: Modifier = Modifier) {
    if (q == null) {
        Text("—", modifier, color = Theme.c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        return
    }
    val col = qualityColor(q)
    Row(
        modifier.clip(RoundedCornerShape(50)).background(col.copy(alpha = if (Theme.c.dark) 0.22f else 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(col))
        Spacer(Modifier.width(6.dp))
        Text(L("photoApp.q.${q.key}"), color = Theme.c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Ay: evre ve aydınlık yüzdesi (bugünse şimdiki an, değilse o günün öğlesi), o günün doğuş ve batışı */
@Composable
private fun MoonCard(p: PhotoDay, isToday: Boolean) {
    val at = if (isToday) Instant.now() else p.date.atTime(12, 0).atZone(p.place.zone).toInstant()
    val m = Nature.moon(at.toEpochMilli())
    val rs = moonRiseSet(p.place, p.date)
    fun t(ms: Long?) = ms?.let { p.clock(Instant.ofEpochMilli((it + 30_000) / 60_000 * 60_000)) } ?: "—"
    AppCard {
        SectionTitle(L("nature.moonHeading"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(MOON_PHASE_EMOJI[m.phaseIndex], fontSize = 30.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(L("nature.phases.${MOON_PHASE_KEYS[m.phaseIndex]}"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(T("photoApp.lit", Fmt.percent((m.illumination * 100).roundToInt())), color = Theme.c.muted, fontSize = 13.sp)
            }
        }
        LightRow("moon", Theme.c.dusk, L("nature.moonrise"), t(rs.rise))
        LightRow("moon", Theme.c.dusk, L("nature.moonset"), t(rs.set))
    }
}

/** Bugün ve sonraki 6 günün gün doğumu ve batımı tahmini; satıra dokununca o gün seçilir */
@Composable
private fun ForecastCard(
    week: List<PhotoDay>, selected: LocalDate, samples: List<PhotoLight.Sample>?, failed: Boolean,
    onPick: (LocalDate) -> Unit, onRetry: () -> Unit,
) {
    val c = Theme.c
    AppCard {
        SectionTitle(L("photoApp.forecast"))
        Text(L("photoApp.forecastNote"), color = c.muted, fontSize = 12.sp)
        when {
            samples != null -> {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.widthIn(min = 96.dp), horizontalArrangement = Arrangement.Center) {
                        IconText("sunrise", L("nature.sunrise"), c.muted, fontSize = 12.sp, iconColor = c.gold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(Modifier.widthIn(min = 96.dp), horizontalArrangement = Arrangement.Center) {
                        IconText("sunset", L("nature.sunset"), c.muted, fontSize = 12.sp, iconColor = c.gold)
                    }
                }
                for (d in week) {
                    val on = d.date == selected
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (on) c.accentSoft else Color.Transparent)
                            .clickable { onPick(d.date) }.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(dayLabel(d.date), Modifier.weight(1f), color = c.text, fontSize = 15.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                        Box(Modifier.widthIn(min = 96.dp), contentAlignment = Alignment.Center) {
                            QualityPill(d.sunrise?.let { PhotoLight.qualityAt(samples, it.toEpochMilli()) })
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.widthIn(min = 96.dp), contentAlignment = Alignment.Center) {
                            QualityPill(d.sunset?.let { PhotoLight.qualityAt(samples, it.toEpochMilli()) })
                        }
                    }
                }
                if (week.none { it.date == selected }) Text(L("photoApp.noForecast"), color = c.muted, fontSize = 12.sp)
            }
            failed -> {
                Text(L("weather.error"), color = c.muted, fontSize = 14.sp)
                TextButton(onClick = onRetry) { Text(L("weather.retry"), color = c.accent) }
            }
            else -> Text(L("weather.loading"), color = c.muted, fontSize = 14.sp)
        }
        Text(L("weather.attribution"), color = c.muted, fontSize = 11.sp)
    }
}

/** 7 günlük tablo: sabah altın, akşam altın, akşam mavi saat (sütun başlıkları sabah/akşamı söylediği için ÖÖ/ÖS yazılmaz) */
@Composable
private fun WeekCard(week: List<PhotoDay>, selected: LocalDate, onPick: (LocalDate) -> Unit) {
    val c = Theme.c
    AppCard {
        SectionTitle(L("photoApp.week"))
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.Bottom) {
            Spacer(Modifier.weight(0.8f))
            for (k in listOf("photoApp.colAmGold", "photoApp.colPmGold", "photoApp.colPmBlue")) {
                Text(L(k), Modifier.weight(1f), color = c.muted, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
            }
        }
        for (d in week) {
            val on = d.date == selected
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (on) c.accentSoft else Color.Transparent)
                    .clickable { onPick(d.date) }.padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dayLabel(d.date), Modifier.weight(0.8f), color = c.text, fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                val l = d.day.light
                for (r in listOf(l.goldenAm, l.goldenPm, l.bluePm)) {
                    Text(d.span(r, ampm = false), Modifier.weight(1f), color = c.text, fontSize = 13.sp, style = Tnum, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** "Pzt 9" */
private fun dayLabel(d: LocalDate) = "${Fmt.weekdayShort(d)} ${d.dayOfMonth}"
