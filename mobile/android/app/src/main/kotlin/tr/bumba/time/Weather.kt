package tr.bumba.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
    )
    data class Hour(val at: Instant, val temp: Double, val code: Int, val rain: Int, val isDay: Boolean)
    data class Day(
        val date: LocalDate, val code: Int, val max: Double, val min: Double, val rain: Int,
        val sunrise: Instant?, val sunset: Instant?, val uv: Double,
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
            "wind_direction_10m,pressure_msl,is_day,uv_index" +
            "&hourly=temperature_2m,precipitation_probability,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,uv_index_max" +
            "&timezone=auto&forecast_days=7"
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
        )
        val h = o.getJSONObject("hourly")
        val ht = h.getJSONArray("time")
        val hourly = (0 until ht.length()).map { i ->
            WeatherData.Hour(
                local(ht.getString(i)),
                h.getJSONArray("temperature_2m").optDouble(i, 0.0),
                h.getJSONArray("weather_code").optInt(i, 2),
                h.getJSONArray("precipitation_probability").optInt(i, 0),
                h.getJSONArray("is_day").optInt(i, 1) == 1,
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

    fun pressure(hpa: Double): String =
        if (imperial) String.format(Locale.ROOT, "%.2f inHg", hpa * 0.0295299830714) else "${Math.round(hpa)} hPa"
}
