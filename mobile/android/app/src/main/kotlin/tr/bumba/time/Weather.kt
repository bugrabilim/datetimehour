package tr.bumba.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Hava durumu (Android): Open-Meteo (anahtarsız; sitedeki weather-core.js ile aynı istek ve WMO kod eşlemesi).
 * Koordinat 2 ondalığa yuvarlanır; aynı yer 15 dakika içinde yeniden sorulmaz, aynı anda gelen istekler birleşir
 * (iOS'taki WeatherCache gibi). iOS uygulaması Apple WeatherKit kullanır; sağlayıcıya özgü kod yalnız bu dosyada.
 * Lisans: Open-Meteo ücretsiz katmanı ticari olmayan kullanım içindir (sitedeki gibi açık karar; CLAUDE.md).
 */
data class WeatherData(
    val zone: ZoneId,
    val current: Current,
    val hourly: List<Hour>,
    val daily: List<Day>,
) {
    /** Sıcaklıklar °C, rüzgâr km/sa, basınç hPa, oranlar 0–100 */
    data class Current(
        val temp: Double, val feels: Double, val humidity: Double, val wind: Double, val windDir: Double,
        val pressure: Double, val uv: Double, val code: Int, val isDay: Boolean, val precipitation: Double,
        /** Bulut oranı % (bilinmiyorsa −1; 2. analiz #193 hava canlandırması) */
        val cloud: Double = -1.0,
        /** 2. analiz #106: hamle km/sa, görüş m (bilinmiyorsa −1), çiy noktası °C (bilinmiyorsa NaN) */
        val gust: Double = -1.0, val visibility: Double = -1.0, val dew: Double = Double.NaN,
    )
    /** Saatlik: 2. analiz #107, #113, #114 için hissedilen, yağış mm, hamle, yön, nem ve UV (bilinmiyorsa varsayılan) */
    data class Hour(
        val at: Instant, val temp: Double, val code: Int, val rain: Int, val isDay: Boolean, val wind: Double = 0.0,
        val feels: Double = temp, val mm: Double = 0.0, val gust: Double = -1.0, val dir: Double = 0.0,
        val humidity: Double = -1.0, val uv: Double = 0.0,
    )
    data class Day(
        val date: LocalDate, val code: Int, val max: Double, val min: Double, val rain: Int,
        val sunrise: Instant?, val sunset: Instant?, val uv: Double,
        /** Günlük toplam yağış (mm) */
        val precip: Double = 0.0,
        /** En güçlü rüzgâr (km/sa) ve baskın yön (derece; gün ayrıntısı, 2. analiz #107) */
        val windMax: Double = 0.0, val windDir: Double = 0.0,
    )

    val condition: WeatherCode get() = WeatherCode.of(current.code)
    val today: Day? get() = daily.firstOrNull()
    /** Yağış ihtimali: içinde bulunulan saatin */
    val rainNow: Int get() = hourly.firstOrNull { it.at.isAfter(Instant.now().minusSeconds(3600)) }?.rain ?: 0
}

/** WMO hava kodu → sitedeki anahtar ("weather.codes.<key>") ve simge (gündüz, gece) */
enum class WeatherCode(val key: String, val day: String, val night: String) {
    CLEAR("clear", "☀️", "🌙"),
    MOSTLY_CLEAR("mostlyClear", "🌤️", "🌙"),
    PARTLY("partly", "⛅", "☁️"),
    OVERCAST("overcast", "☁️", "☁️"),
    FOG("fog", "🌫️", "🌫️"),
    DRIZZLE("drizzle", "🌦️", "🌧️"),
    FREEZING_DRIZZLE("freezingDrizzle", "🌧️", "🌧️"),
    RAIN("rain", "🌧️", "🌧️"),
    FREEZING_RAIN("freezingRain", "🌧️", "🌧️"),
    SNOW("snow", "🌨️", "🌨️"),
    SNOW_GRAINS("snowGrains", "❄️", "❄️"),
    SHOWERS("showers", "🌦️", "🌧️"),
    SNOW_SHOWERS("snowShowers", "🌨️", "🌨️"),
    THUNDER("thunder", "⛈️", "⛈️"),
    THUNDER_HAIL("thunderHail", "⛈️", "⛈️");

    val label: String get() = L("weather.codes.$key")
    fun icon(isDay: Boolean = true) = if (isDay) day else night

    /** Kendi çizgi simgemiz (doğal tasarım madde 10 ve 19): açık gündüz güneş, gece ay; bulutlu, yağmurlu, karlı, sisli, fırtınalı */
    fun line(isDay: Boolean = true): String = when (this) {
        CLEAR, MOSTLY_CLEAR -> if (isDay) "sun" else "moon"
        PARTLY, OVERCAST -> "cloud"
        FOG -> "fog"
        DRIZZLE, FREEZING_DRIZZLE, RAIN, FREEZING_RAIN, SHOWERS -> "rain"
        SNOW, SNOW_GRAINS, SNOW_SHOWERS -> "snow"
        THUNDER, THUNDER_HAIL -> "storm"
    }

    /** Widget'taki karşılığı (gen-strings icons.json'dan üretir: res/drawable/wi_*.xml) */
    fun widgetIcon(isDay: Boolean = true): Int = when (line(isDay)) {
        "sun" -> R.drawable.wi_sun
        "moon" -> R.drawable.wi_moon
        "fog" -> R.drawable.wi_fog
        "rain" -> R.drawable.wi_rain
        "snow" -> R.drawable.wi_snow
        "storm" -> R.drawable.wi_storm
        else -> R.drawable.wi_cloud
    }

    companion object {
        fun of(c: Int): WeatherCode = when (c) {
            0 -> CLEAR
            1 -> MOSTLY_CLEAR
            2 -> PARTLY
            3 -> OVERCAST
            45, 48 -> FOG
            in 51..55 -> DRIZZLE
            56, 57 -> FREEZING_DRIZZLE
            in 61..65 -> RAIN
            66, 67 -> FREEZING_RAIN
            in 71..75 -> SNOW
            77 -> SNOW_GRAINS
            in 80..82 -> SHOWERS
            85, 86 -> SNOW_SHOWERS
            95 -> THUNDER
            96, 99 -> THUNDER_HAIL
            else -> PARTLY
        }
    }
}

object WeatherCache {
    private const val API = "https://api.open-meteo.com/v1/forecast"
    private var cache: Triple<String, Long, WeatherData>? = null
    private var inflight: Pair<String, CompletableDeferred<WeatherData?>>? = null

    private fun key(lat: Double, lon: Double) = String.format(Locale.ROOT, "%.2f,%.2f", lat, lon)

    /** Ekran görüntüsü testleri için: ağ olmadan örnek veri */
    fun seed(lat: Double, lon: Double, data: WeatherData) {
        cache = Triple(key(lat, lon), System.currentTimeMillis(), data)
    }

    /** Önbellekteki son veri (ekran açılırken hemen göstermek için) */
    fun cached(lat: Double, lon: Double): WeatherData? = cache?.takeIf { it.first == key(lat, lon) }?.third

    /** Hata ya da bağlantı yoksa null */
    suspend fun weather(lat: Double, lon: Double, force: Boolean = false): WeatherData? {
        val key = key(lat, lon)
        cache?.let { if (!force && it.first == key && System.currentTimeMillis() - it.second < 15 * 60_000) return it.third }
        inflight?.let { if (it.first == key) return it.second.await() }
        val deferred = CompletableDeferred<WeatherData?>()
        inflight = key to deferred
        val value = withContext(Dispatchers.IO) { runCatching { fetch(key) }.getOrNull() }
        if (value != null) cache = Triple(key, System.currentTimeMillis(), value)
        if (inflight?.first == key) inflight = null
        deferred.complete(value)
        return value
    }

    private fun fetch(key: String): WeatherData {
        val (lat, lon) = key.split(",")
        val url = "$API?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m," +
            "wind_direction_10m,pressure_msl,is_day,uv_index,cloud_cover,wind_gusts_10m,visibility,dew_point_2m" +
            "&hourly=temperature_2m,precipitation_probability,weather_code,is_day,wind_speed_10m," +
            "apparent_temperature,precipitation,wind_gusts_10m,wind_direction_10m,relative_humidity_2m,uv_index" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,uv_index_max,precipitation_sum," +
            "wind_speed_10m_max,wind_direction_10m_dominant" +
            "&timezone=auto&forecast_days=14"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        val body = try {
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
        return parse(JSONObject(body))
    }

    fun parse(o: JSONObject): WeatherData {
        val zone = runCatching { ZoneId.of(o.getString("timezone")) }.getOrDefault(ZoneId.systemDefault())
        fun local(s: String): Instant = LocalDateTime.parse(s).atZone(zone).toInstant()
        val cu = o.getJSONObject("current")
        val current = WeatherData.Current(
            temp = cu.getDouble("temperature_2m"),
            feels = cu.optDouble("apparent_temperature", cu.getDouble("temperature_2m")),
            humidity = cu.optDouble("relative_humidity_2m", 0.0),
            wind = cu.optDouble("wind_speed_10m", 0.0),
            windDir = cu.optDouble("wind_direction_10m", 0.0),
            pressure = cu.optDouble("pressure_msl", 0.0),
            uv = cu.optDouble("uv_index", 0.0),
            code = cu.optInt("weather_code", 2),
            isDay = cu.optInt("is_day", 1) == 1,
            precipitation = cu.optDouble("precipitation", 0.0),
            cloud = cu.optDouble("cloud_cover", -1.0),
            gust = cu.optDouble("wind_gusts_10m", -1.0),
            visibility = cu.optDouble("visibility", -1.0),
            dew = cu.optDouble("dew_point_2m", Double.NaN),
        )
        val h = o.getJSONObject("hourly")
        val ht = h.getJSONArray("time")
        fun hd(name: String, i: Int, def: Double) = h.optJSONArray(name)?.optDouble(i, def)?.takeIf { !it.isNaN() } ?: def
        val hourly = (0 until ht.length()).map { i ->
            val temp = h.getJSONArray("temperature_2m").optDouble(i, 0.0)
            WeatherData.Hour(
                local(ht.getString(i)),
                temp,
                h.getJSONArray("weather_code").optInt(i, 2),
                h.getJSONArray("precipitation_probability").optInt(i, 0),
                h.getJSONArray("is_day").optInt(i, 1) == 1,
                hd("wind_speed_10m", i, 0.0),
                feels = hd("apparent_temperature", i, temp),
                mm = hd("precipitation", i, 0.0),
                gust = hd("wind_gusts_10m", i, -1.0),
                dir = hd("wind_direction_10m", i, 0.0),
                humidity = hd("relative_humidity_2m", i, -1.0),
                uv = hd("uv_index", i, 0.0),
            )
        }
        val d = o.getJSONObject("daily")
        val dt = d.getJSONArray("time")
        val daily = (0 until dt.length()).map { i ->
            fun sun(name: String) = d.optJSONArray(name)?.optString(i)?.takeIf { it.isNotEmpty() && it != "null" }?.let(::local)
            WeatherData.Day(
                LocalDate.parse(dt.getString(i)),
                d.getJSONArray("weather_code").optInt(i, 2),
                d.getJSONArray("temperature_2m_max").optDouble(i, 0.0),
                d.getJSONArray("temperature_2m_min").optDouble(i, 0.0),
                d.optJSONArray("precipitation_probability_max")?.optInt(i, 0) ?: 0,
                sun("sunrise"), sun("sunset"),
                d.optJSONArray("uv_index_max")?.optDouble(i, 0.0) ?: 0.0,
                d.optJSONArray("precipitation_sum")?.optDouble(i, 0.0) ?: 0.0,
                d.optJSONArray("wind_speed_10m_max")?.optDouble(i, 0.0) ?: 0.0,
                d.optJSONArray("wind_direction_10m_dominant")?.optDouble(i, 0.0) ?: 0.0,
            )
        }
        return WeatherData(zone, current, hourly, daily)
    }
}

/** Ekranlar için: önbellekte varsa hemen, yoksa yüklenince; konum değişince yeniden */
@Composable
fun rememberWeather(lat: Double, lon: Double): WeatherData? {
    val data by produceState(WeatherCache.cached(lat, lon), lat, lon) {
        value = WeatherCache.weather(lat, lon) ?: value
    }
    return data
}

/** Ölçü birimleri (Ayarlar › Birimler, `units`): metrik (°C, km/sa, hPa) ya da Amerikan (°F, mph, inHg) */
object Units {
    val imperial: Boolean get() = Prefs.units == "imperial"

    /** Seçim yoksa bölgeye göre (ABD, Liberya, Myanmar Amerikan birimleri kullanır) */
    val systemDefault: String
        get() = if (Locale.getDefault().country in setOf("US", "LR", "MM")) "imperial" else "metric"

    fun temp(c: Double): String = "${Math.round(if (imperial) c * 9 / 5 + 32 else c)}°"

    fun wind(kmh: Double): String =
        if (imperial) "${Math.round(kmh / 1.609344)} mph" else T("weather.km", Math.round(kmh))

    /** Yalnız sayı (tablolar; birim başlıkta: windUnit) */
    fun windNumber(kmh: Double): String = "${Math.round(if (imperial) kmh / 1.609344 else kmh)}"
    val windUnit: String get() = if (imperial) "mph" else T("weather.km", "").trim()

    /** Basınç birimi (2. analiz #106; Ayarlar › Birimler, `units.pressure`): auto (birime göre), hPa, mmHg, inHg */
    val pressureUnit: String get() = Prefs.store.getString("units.pressure", "auto") ?: "auto"

    fun pressure(hpa: Double): String = when (pressureUnit.takeIf { it != "auto" } ?: if (imperial) "inHg" else "hPa") {
        "inHg" -> String.format(Locale.ROOT, "%.2f inHg", hpa * 0.0295299830714)
        "mmHg" -> "${Math.round(hpa * 0.750061683)} mmHg"
        else -> "${Math.round(hpa)} hPa"
    }

    /** Görüş: 10 km'den uzaksa tam sayı, yakınsa bir ondalık (Amerikan biriminde mil) */
    fun distance(m: Double): String {
        val v = if (imperial) m / 1609.344 else m / 1000
        val s = if (v >= 10) Math.round(v).toString() else (Math.round(v * 10) / 10.0).toString().replace(".", Strings.decimalPoint)
        return s + if (imperial) " mi" else " km"
    }

    /** Yağış miktarı (mm ya da inç) */
    fun precip(mm: Double): String = if (imperial) String.format(Locale.ROOT, "%.2f in", mm / 25.4).replace(".", Strings.decimalPoint)
        else (Math.round(mm * 10) / 10.0).toString().replace(".", Strings.decimalPoint) + " mm"

    /** Derece farkı (yarın uyarısı; Amerikan biriminde °F farkı) */
    fun delta(c: Int): Int = if (imperial) Math.round(c * 1.8).toInt() else c
}

/** Hava durumunun sözlü özeti ve açıklamaları (2. analiz #108; kural core/WeatherWords, sitedeki weather-core.js ile aynı) */
object WeatherSpeech {
    fun next(d: WeatherData, now: Instant = Instant.now()): List<tr.bumba.time.core.WeatherWords.Hour> =
        d.hourly.filter { it.at.isAfter(now.minusSeconds(3600)) }.take(12)
            .map { tr.bumba.time.core.WeatherWords.Hour(it.at.atZone(d.zone).hour, it.code, it.rain, it.wind) }

    fun summary(d: WeatherData, now: Instant = Instant.now()): String {
        val today = d.daily.firstOrNull() ?: return ""
        val s = tr.bumba.time.core.WeatherWords.summary(next(d, now), today.max, d.daily.getOrNull(1)?.max)
        return when (s.key) {
            "weather.sum.default" -> T(s.key, d.condition.label, Units.temp(today.max), Units.temp(today.min))
            "weather.sum.colder", "weather.sum.warmer" -> T(s.key, Units.delta(s.n))
            else -> T(s.key, Fmt.hm(today.date.atTime(s.hour, 0).atZone(d.zone).toInstant(), d.zone))
        }
    }
}


/**
 * Hava kalitesi ve polen (2. analiz #111): Open-Meteo Air Quality API (CAMS, Copernicus; anahtarsız). Avrupa hava kalitesi
 * endeksi (EAQI), PM2,5, PM10, ozon, azot dioksit ve polen (polen yalnız CAMS Avrupa alanında; yoksa gösterilmez).
 * Koordinat 2 ondalık, aynı yer 30 dk önbellekte.
 */
data class AirData(
    val eaqi: Double, val pm25: Double, val pm10: Double, val ozone: Double, val no2: Double,
    /** alder, birch, grass, mugwort, olive, ragweed → tane/m³ (yoksa listede yok) */
    val pollen: Map<String, Double>,
)

object AirCache {
    private const val API = "https://air-quality-api.open-meteo.com/v1/air-quality"
    val POLLEN = listOf("alder", "birch", "grass", "mugwort", "olive", "ragweed")
    private var cache: Triple<String, Long, AirData>? = null

    fun seed(lat: Double, lon: Double, data: AirData) { cache = Triple(key(lat, lon), System.currentTimeMillis(), data) }
    private fun key(lat: Double, lon: Double) = String.format(Locale.ROOT, "%.2f,%.2f", lat, lon)

    suspend fun get(lat: Double, lon: Double): AirData? {
        val k = key(lat, lon)
        cache?.let { if (it.first == k && System.currentTimeMillis() - it.second < 30 * 60_000) return it.third }
        val v = withContext(Dispatchers.IO) {
            runCatching {
                val (la, lo) = k.split(",")
                val url = "$API?latitude=$la&longitude=$lo&timezone=auto&current=european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide," +
                    POLLEN.joinToString(",") { "${it}_pollen" }
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 10_000; c.readTimeout = 15_000
                val body = try {
                    if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                    c.inputStream.bufferedReader().use { it.readText() }
                } finally { c.disconnect() }
                parse(JSONObject(body))
            }.getOrNull()
        }
        if (v != null) cache = Triple(k, System.currentTimeMillis(), v)
        return v ?: cache?.takeIf { it.first == k }?.third
    }

    fun parse(o: JSONObject): AirData? {
        val c = o.optJSONObject("current") ?: return null
        fun d(n: String) = if (c.isNull(n)) Double.NaN else c.optDouble(n, Double.NaN)
        val eaqi = d("european_aqi")
        if (eaqi.isNaN()) return null
        val pollen = POLLEN.mapNotNull { p -> d("${p}_pollen").takeIf { !it.isNaN() }?.let { p to it } }.toMap()
        return AirData(eaqi, d("pm2_5"), d("pm10"), d("ozone"), d("nitrogen_dioxide"), pollen)
    }
}

/** Model karşılaştırma (2. analiz #114): ECMWF, GFS ve ICON'un günlük en yüksek/en düşük sıcaklık ve yağış tahmini */
data class ModelDay(val date: LocalDate, val max: Double, val min: Double, val mm: Double)

object ModelCache {
    val MODELS = listOf("ecmwf_ifs025" to "ECMWF", "gfs_seamless" to "GFS", "icon_seamless" to "ICON")
    private var cache: Triple<String, Long, Map<String, List<ModelDay>>>? = null

    fun seed(lat: Double, lon: Double, data: Map<String, List<ModelDay>>) {
        cache = Triple(String.format(Locale.ROOT, "%.2f,%.2f", lat, lon), System.currentTimeMillis(), data)
    }

    suspend fun get(lat: Double, lon: Double): Map<String, List<ModelDay>>? {
        val k = String.format(Locale.ROOT, "%.2f,%.2f", lat, lon)
        cache?.let { if (it.first == k && System.currentTimeMillis() - it.second < 60 * 60_000) return it.third }
        val v = withContext(Dispatchers.IO) {
            runCatching {
                val (la, lo) = k.split(",")
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$la&longitude=$lo&timezone=auto&forecast_days=7" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum&models=" + MODELS.joinToString(",") { it.first }
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 10_000; c.readTimeout = 15_000
                val body = try {
                    if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                    c.inputStream.bufferedReader().use { it.readText() }
                } finally { c.disconnect() }
                parse(JSONObject(body))
            }.getOrNull()
        }
        if (v != null) cache = Triple(k, System.currentTimeMillis(), v)
        return v
    }

    fun parse(o: JSONObject): Map<String, List<ModelDay>> {
        val d = o.getJSONObject("daily")
        val t = d.getJSONArray("time")
        return MODELS.associate { (id, name) ->
            fun arr(n: String) = d.optJSONArray("${n}_$id")
            val mx = arr("temperature_2m_max"); val mn = arr("temperature_2m_min"); val pr = arr("precipitation_sum")
            name to (0 until t.length()).mapNotNull { i ->
                if (mx == null || mn == null || mx.isNull(i) || mn.isNull(i)) null
                else ModelDay(LocalDate.parse(t.getString(i)), mx.getDouble(i), mn.getDouble(i), pr?.optDouble(i, 0.0)?.takeIf { !it.isNaN() } ?: 0.0)
            }
        }.filterValues { it.isNotEmpty() }
    }
}

/**
 * Hava çizgi simgesi (doğal tasarım madde 10, 4 Kasım 2026): emoji yerine kendi simgemiz. Güneş ve ay altın, yağmur, kar
 * ve sis ayrıntısı gök mavisi, gövde yazı renginde. Ekran okuyucuya durumun adı söylenir.
 */
@Composable
fun WeatherIcon(code: WeatherCode, isDay: Boolean = true, size: androidx.compose.ui.unit.Dp = 28.dp,
                color: androidx.compose.ui.graphics.Color = Theme.c.text) {
    val name = code.line(isDay)
    val c = Theme.c
    val (c1, c2) = when (name) {
        "sun" -> c.gold to c.gold
        "moon" -> c.gold to c.gold
        "storm" -> color to c.gold
        "rain", "snow", "fog" -> color to c.sky
        else -> color to color
    }
    LineIcon(name, size, c1 = c1, c2 = c2, label = code.label)
}
