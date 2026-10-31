package tr.bumba.time.wear

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import tr.bumba.time.core.Prayer
import tr.bumba.time.core.PrayerTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class WearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Wear.init(this)
    }
}

/** İl (provinces.json; telefondaki listeyle aynı) */
data class WProvince(val slug: String, val name: String, val lat: Double, val lon: Double, val ar: String, val ru: String = "", val fa: String = "") {
    /** Arapça, Rusça ve Farsçada kendi yazısıyla */
    val displayName: String get() = when (Wear.lang) { "ar" -> ar; "ru" -> ru.ifEmpty { name }; "fa" -> fa.ifEmpty { name }; else -> name }
}

/** Vakitlerin hesaplandığı yer */
data class WPlace(val id: String, val name: String, val lat: Double, val lon: Double, val zone: ZoneId) {
    data class Entry(val prayer: Prayer, val at: Instant)

    fun entries(day: LocalDate): List<Entry> {
        val t = PrayerTimes.times(day, lat, lon, zone)
        val start = day.atStartOfDay(zone).toInstant()
        return Prayer.entries.mapNotNull { p -> t.minutes(p)?.let { Entry(p, start.plusSeconds(it * 60L)) } }
    }

    fun next(now: Instant): Entry? {
        val today = now.atZone(zone).toLocalDate()
        return (entries(today) + entries(today.plusDays(1))).firstOrNull { it.at.isAfter(now) }
    }

    fun previous(now: Instant): Entry? {
        val today = now.atZone(zone).toLocalDate()
        return (entries(today.minusDays(1)) + entries(today)).lastOrNull { !it.at.isAfter(now) }
    }

    fun hm(at: Instant): String = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).format(at.atZone(zone))
}

/**
 * Saatin verileri ve tercihleri: il listesi, hicri ay başları (hijri.json), metinler (telefon uygulamasıyla aynı anahtarlar),
 * seçili yer ("place": il adı ya da "auto"; otomatikte son bulunan konum "auto.lat/lon/name").
 */
object Wear {
    private lateinit var app: Context
    lateinit var provinces: List<WProvince>
        private set
    private var hijri: List<Triple<Long, Int, Int>> = emptyList()
    val turkey: ZoneId = ZoneId.of("Europe/Istanbul")

    fun init(c: Context) {
        app = c.applicationContext
        fun read(n: String) = app.assets.open(n).bufferedReader().use { it.readText() }
        val p = JSONArray(read("provinces.json"))
        provinces = (0 until p.length()).map { i ->
            val o = p.getJSONObject(i)
            WProvince(o.getString("slug"), o.getString("name"), o.getDouble("lat"), o.getDouble("lon"), o.getString("ar"), o.optString("ru"), o.optString("fa"))
        }.sortedWith(compareBy(java.text.Collator.getInstance(Locale.forLanguageTag("tr"))) { it.name })
        val h = JSONArray(read("hijri.json"))
        hijri = (0 until h.length()).map { i -> h.getJSONArray(i).let { Triple(LocalDate.parse(it.getString(0)).toEpochDay(), it.getInt(1), it.getInt(2)) } }
    }

    val prefs: SharedPreferences get() = app.getSharedPreferences("wear", Context.MODE_PRIVATE)

    val lang: String
        get() {
            val l = app.resources.configuration.locales[0].language.let { if (it == "in") "id" else it }
            return if (l in listOf("tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru")) l else "tr"
        }

    @SuppressLint("DiscouragedApi")
    fun text(key: String, vararg args: Any): String {
        val id = app.resources.getIdentifier(key.replace(Regex("[^A-Za-z0-9_]"), "_"), "string", app.packageName)
        if (id == 0) return key
        return if (args.isEmpty()) app.getString(id) else app.getString(id, *args.map { it.toString() }.toTypedArray())
    }

    fun province(slug: String) = provinces.firstOrNull { it.slug == slug } ?: provinces.first { it.slug == "istanbul" }

    /** Seçili yer: il ya da otomatik (son bulunan konum; yoksa İstanbul) */
    fun place(): WPlace {
        val sel = prefs.getString("place", "auto")!!
        if (sel != "auto") return province(sel).let { WPlace(it.slug, it.displayName, it.lat, it.lon, turkey) }
        val sp = prefs
        if (!sp.contains("auto.lat")) return province("istanbul").let { WPlace("auto", it.displayName, it.lat, it.lon, turkey) }
        return WPlace("auto", sp.getString("auto.name", "")!!, sp.getFloat("auto.lat", 0f).toDouble(), sp.getFloat("auto.lon", 0f).toDouble(),
            runCatching { ZoneId.of(sp.getString("auto.tz", "")!!) }.getOrDefault(ZoneId.systemDefault()))
    }

    fun select(sel: String) = prefs.edit().putString("place", sel).apply()

    /** Konum bulununca: en yakın il 150 km içindeyse o il, değilse tam konum ve saatin saat dilimi */
    fun saveLocation(lat: Double, lon: Double) {
        fun km(p: WProvince): Double {
            val r = Math.PI / 180
            val dLat = (p.lat - lat) * r
            val dLon = (p.lon - lon) * r
            val a = Math.sin(dLat / 2).let { it * it } + Math.cos(lat * r) * Math.cos(p.lat * r) * Math.sin(dLon / 2).let { it * it }
            return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        }
        val near = provinces.minBy { km(it) }
        val e = prefs.edit()
        if (km(near) <= 150) e.putFloat("auto.lat", near.lat.toFloat()).putFloat("auto.lon", near.lon.toFloat())
            .putString("auto.name", near.displayName).putString("auto.tz", turkey.id)
        else e.putFloat("auto.lat", lat.toFloat()).putFloat("auto.lon", lon.toFloat())
            .putString("auto.name", text("place.myLocation")).putString("auto.tz", ZoneId.systemDefault().id)
        e.apply()
    }

    fun prayerName(p: Prayer) = text("prayer.${p.key}")

    /** Ramazan'da akşam "İftar", imsak "Sahur" (Ramazan günleri hicri tablodan: 9. ay) */
    fun label(e: WPlace.Entry, zone: ZoneId): String {
        val h = hijri(e.at.atZone(zone).toLocalDate())
        val ramadan = h?.second == 9
        return when {
            ramadan && e.prayer == Prayer.MAGHRIB -> text("live.iftar")
            ramadan && e.prayer == Prayer.FAJR -> text("live.sahur")
            else -> prayerName(e.prayer)
        }
    }

    /** Hicri tarih: (yıl, ay, gün); tablo dışında null */
    fun hijri(d: LocalDate): Triple<Int, Int, Int>? {
        val n = d.toEpochDay()
        for (i in hijri.size - 2 downTo 0) {
            val (start, y, m) = hijri[i]
            if (n >= start) return if (n < hijri[i + 1].first) Triple(y, m, (n - start + 1).toInt()) else null
        }
        return null
    }

    fun hijriText(d: LocalDate): String? = hijri(d)?.let { (y, m, day) -> text("hijri.fmt", day, text("hijri.month.$m"), y) }

    /** "30 Ekim Cuma" (Gregoryen) */
    fun dayText(z: ZonedDateTime): String {
        val loc = Locale.forLanguageTag(lang)
        return "${z.dayOfMonth} ${z.month.getDisplayName(TextStyle.FULL, loc)} ${z.dayOfWeek.getDisplayName(TextStyle.FULL, loc)}"
    }
}
