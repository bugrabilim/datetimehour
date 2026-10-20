package tr.bumba.time

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Güneş ve Ay'ın tepe noktası (enlem/boylam; sitedeki globe.js ve iOS GlobeSky ile aynı formüller).
 * Güneş ~0,01°, Ay düşük duyarlıklı (~1°; yalnız küredeki işaret için).
 */
object GlobeSky {
    private const val RAD = Math.PI / 180

    private fun days(ms: Long) = ms / 86_400_000.0 + 2440587.5 - 2451545.0

    private fun wrap(x: Double): Double {
        var v = (x + 180) % 360
        if (v < 0) v += 360
        return v - 180
    }

    data class Point(val lat: Double, val lon: Double, val elong: Double = 0.0)

    fun sun(ms: Long): Point {
        val d = days(ms)
        val g = (357.529 + 0.98560028 * d) * RAD
        val q = 280.459 + 0.98564736 * d
        val l = (q + 1.915 * sin(g) + 0.02 * sin(2 * g)) * RAD
        val e = (23.439 - 0.00000036 * d) * RAD
        val ra = atan2(cos(e) * sin(l), cos(l)) / RAD
        val dec = asin(sin(e) * sin(l)) / RAD
        val eqt = wrap(q - ra)
        var m = (ms % 86_400_000).toDouble()
        if (m < 0) m += 86_400_000
        val utcH = m / 3_600_000
        return Point(dec, wrap(-15 * (utcH - 12) - eqt))
    }

    /** elong: Güneş'ten açısal uzaklık (0 yeni ay, 180 dolunay) */
    fun moon(ms: Long): Point {
        val d = days(ms)
        val l = 218.316 + 13.176396 * d
        val m = (134.963 + 13.064993 * d) * RAD
        val f = (93.272 + 13.22935 * d) * RAD
        val lam = (l + 6.289 * sin(m)) * RAD
        val bet = 5.128 * sin(f) * RAD
        val e = 23.439 * RAD
        val ra = atan2(sin(lam) * cos(e) - tan(bet) * sin(e), cos(lam)) / RAD
        val dec = asin(sin(bet) * cos(e) + cos(bet) * sin(e) * sin(lam)) / RAD
        val gmst = 280.46061837 + 360.98564736629 * d
        val sunL = 280.459 + 0.98564736 * d + 1.915 * sin((357.529 + 0.98560028 * d) * RAD)
        var elong = (lam / RAD - sunL) % 360
        if (elong < 0) elong += 360
        return Point(dec, wrap(ra - gmst), elong)
    }
}

/** Küre dokusu: sitedeki Natural Earth görüntüsü (assets/earth.jpg, 2560×1280, kamu malı); bir kez çözülür */
object GlobeTexture {
    @Volatile var pixels: IntArray? = null
        private set
    var width = 0
        private set
    var height = 0
        private set

    @Synchronized
    fun load(c: Context): IntArray? {
        pixels?.let { return it }
        return runCatching {
            val bmp = c.assets.open("earth.jpg").use { BitmapFactory.decodeStream(it) } ?: return null
            val px = IntArray(bmp.width * bmp.height)
            bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            width = bmp.width
            height = bmp.height
            bmp.recycle()
            pixels = px
            px
        }.getOrNull()
    }
}

/**
 * Yakınlaşınca gösterilen dünya yerleri (sitedeki src/data/places.json: Natural Earth 1:10m, nüfusu ≥200 bin ya da
 * başkent; nüfusa göre sıralı). 31 şehre 0,35°'den yakın olanlar atlanır (iOS GlobePlace ile aynı).
 */
data class GlobePlace(val name: String, val zone: ZoneId, val lat: Double, val lon: Double, val population: Double) {
    companion object {
        @Volatile private var cache: Pair<String, List<GlobePlace>>? = null

        fun all(c: Context): List<GlobePlace> {
            val lang = Strings.lang
            cache?.let { if (it.first == lang) return it.second }
            val list = runCatching {
                val o = JSONObject(c.assets.open("places.json").bufferedReader().use { it.readText() })
                val tz = o.getJSONArray("tz")
                val zones = (0 until tz.length()).map { runCatching { ZoneId.of(tz.getString(it)) }.getOrNull() }
                val namesObj = o.getJSONObject("names")
                val names = namesObj.optJSONArray(lang) ?: namesObj.getJSONArray("en")
                val p = o.getJSONArray("p")
                val cities = City.all
                (0 until p.length()).mapNotNull { i ->
                    val row = p.getJSONArray(i)
                    if (row.length() < 4 || i >= names.length()) return@mapNotNull null
                    val zone = zones.getOrNull(row.getInt(2)) ?: return@mapNotNull null
                    val lat = row.getDouble(0)
                    val lon = row.getDouble(1)
                    if (cities.any { abs(it.lat - lat) < 0.35 && abs(it.lon - lon) < 0.35 }) return@mapNotNull null
                    GlobePlace(names.getString(i), zone, lat, lon, row.getDouble(3))
                }
            }.getOrDefault(emptyList())
            cache = lang to list
            return list
        }
    }
}

/**
 * Küredeki öne çıkan şehirlerin güncel havası (simge + sıcaklık). Open-Meteo'ya bütün şehirler için tek istek gider
 * (koordinatlar virgülle; yanıt dizi); sonuç saatte bir yenilenir ve cihazda saklanır (Prefs "globeWx", iOS gibi).
 */
object GlobeWeather {
    data class Entry(val celsius: Double, val code: Int, val isDay: Boolean)

    private const val KEY = "globeWx"
    private const val TTL = 3_600_000L
    private var at = 0L
    private var items: Map<String, Entry> = emptyMap()
    private var loaded = false
    private var loading = false

    private fun restore() {
        if (loaded) return
        loaded = true
        runCatching {
            val o = JSONObject(Prefs.store.getString(KEY, null) ?: return)
            at = o.getLong("at")
            val c = o.getJSONObject("c")
            items = c.keys().asSequence().associateWith { k ->
                val a = c.getJSONArray(k)
                Entry(a.getDouble(0), a.getInt(1), a.getInt(2) == 1)
            }
        }
    }

    /** 6 saatten eski veri gösterilmez */
    fun cached(key: String): Entry? {
        restore()
        return if (System.currentTimeMillis() - at < 6 * TTL) items[key] else null
    }

    /** Süresi geçtiyse yeniden sorar; yeni veri geldiyse true */
    suspend fun refresh(cities: List<City>): Boolean {
        restore()
        if (loading || cities.isEmpty() || System.currentTimeMillis() - at < TTL) return false
        loading = true
        try {
            val fresh = withContext(Dispatchers.IO) { runCatching { fetch(cities) }.getOrNull() } ?: return false
            items = fresh
            at = System.currentTimeMillis()
            val c = JSONObject()
            for ((k, e) in fresh) c.put(k, JSONArray(listOf(e.celsius, e.code, if (e.isDay) 1 else 0)))
            Prefs.store.edit().putString(KEY, JSONObject().put("at", at).put("c", c).toString()).apply()
            return true
        } finally {
            loading = false
        }
    }

    private fun fetch(cities: List<City>): Map<String, Entry> {
        fun f(v: Double) = String.format(Locale.ROOT, "%.2f", v)
        val url = "https://api.open-meteo.com/v1/forecast?latitude=" + cities.joinToString(",") { f(it.lat) } +
            "&longitude=" + cities.joinToString(",") { f(it.lon) } + "&current=temperature_2m,weather_code,is_day&timezone=GMT"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        val body = try {
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
        val trimmed = body.trimStart()
        val arr = if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONArray().put(JSONObject(trimmed))
        val out = HashMap<String, Entry>()
        for (i in 0 until minOf(arr.length(), cities.size)) {
            val cur = arr.getJSONObject(i).optJSONObject("current") ?: continue
            out[cities[i].key] = Entry(cur.getDouble("temperature_2m"), cur.optInt("weather_code", 2), cur.optInt("is_day", 1) == 1)
        }
        return out
    }
}
