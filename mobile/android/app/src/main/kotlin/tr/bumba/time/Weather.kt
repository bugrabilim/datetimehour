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
        /** Bulut oranı % (bilinmiyorsa −1; 2. analiz #193 hava canlandırması) */
        val cloud: Double = -1.0,
        /** 2. analiz #106: hamle km/sa, görüş m (bilinmiyorsa −1), çiy noktası °C (bilinmiyorsa NaN) */
        val gust: Double = -1.0, val visibility: Double = -1.0, val dew: Double = Double.NaN,
    )
    data class Hour(val at: Instant, val temp: Double, val code: Int, val rain: Int, val isDay: Boolean, val wind: Double = 0.0)
    data class Day(
        val date: LocalDate, val code: Int, val max: Double, val min: Double, val rain: Int,
        val sunrise: Instant?, val sunset: Instant?, val uv: Double,
        /** Günlük toplam yağış (mm) */
        val precip: Double = 0.0,
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
            "&hourly=temperature_2m,precipitation_probability,weather_code,is_day,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,uv_index_max,precipitation_sum" +
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
        val hourly = (0 until ht.length()).map { i ->
            WeatherData.Hour(
                local(ht.getString(i)),
                h.getJSONArray("temperature_2m").optDouble(i, 0.0),
                h.getJSONArray("weather_code").optInt(i, 2),
                h.getJSONArray("precipitation_probability").optInt(i, 0),
                h.getJSONArray("is_day").optInt(i, 1) == 1,
                h.optJSONArray("wind_speed_10m")?.optDouble(i, 0.0) ?: 0.0,
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
