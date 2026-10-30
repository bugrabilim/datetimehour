package tr.bumba.time

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 81 il (sitedeki src/data/provinces.json) */
data class Province(val slug: String, val name: String, val lat: Double, val lon: Double, val ar: String) {
    val displayName: String get() = if (Strings.lang == "ar") ar else name

    companion object {
        val all: List<Province> get() = AppData.provinces
        fun find(slug: String): Province = all.firstOrNull { it.slug == slug } ?: all.first { it.slug == "istanbul" }
        fun nearest(lat: Double, lon: Double): Pair<Province, Double> =
            all.map { it to km(it.lat, it.lon, lat, lon) }.minBy { it.second }
    }
}

/** İlçe (src/data/districts.json, GeoNames CC BY 4.0): ad, ilçe merkezinin koordinatı, bağlı olduğu il */
data class District(val name: String, val lat: Double, val lon: Double, val province: String) {
    val id: String get() = "d:$province:$name"
    /** İl merkezi ilçesi (adı ilin adı; "Hakkâri" / "Hakkari" gibi şapkalı yazımlar da) */
    val isCentre: Boolean get() = fold(name) == fold(Province.find(province).name)
    val label: String get() = if (isCentre) Province.find(province).displayName else "$name, ${Province.find(province).displayName}"

    companion object {
        fun all(province: String): List<District> = AppData.districts[province].orEmpty()
        fun find(id: String): District? {
            val parts = id.split(":", limit = 3)
            if (parts.size != 3 || parts[0] != "d") return null
            return all(parts[1]).firstOrNull { it.name == parts[2] }
        }
        fun fold(s: String) = java.text.Normalizer.normalize(s.lowercase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "").replace("ı", "i")
    }
}

/** Dünya saatleri için 31 şehir (sitedeki src/data/cities.json) */
data class City(
    val key: String,
    val tz: String,
    val lat: Double,
    val lon: Double,
    private val names: Map<String, String>,
    private val countries: Map<String, String>,
) {
    val displayName: String get() = names[Strings.lang] ?: names["en"] ?: key
    val displayCountry: String get() = countries[Strings.lang] ?: countries["en"] ?: ""
    val zone: ZoneId get() = ZoneId.of(tz)

    /** Sıradaki saat değişimi (`days` gün içindeyse): an ve fark (sn; ileri pozitif) — rakip analizi #30 */
    fun nextDst(now: java.time.Instant, days: Long = 7): Pair<java.time.Instant, Int>? {
        val tr = zone.rules.nextTransition(now) ?: return null
        if (tr.instant.isAfter(now.plusSeconds(days * 86400))) return null
        val delta = tr.offsetAfter.totalSeconds - tr.offsetBefore.totalSeconds
        return if (delta == 0) null else tr.instant to delta
    }

    /** "2 Kasım: saatler 1 sa geri alınıyor" (şehrin yerel tarihiyle) */
    fun dstNote(now: java.time.Instant, days: Long = 7): String? {
        val (at, delta) = nextDst(now, days) ?: return null
        return T(if (delta > 0) "world.dstForward" else "world.dstBack", Fmt.dayMonth(at.atZone(zone).toLocalDate()), Fmt.hm(kotlin.math.abs(delta)))
    }

    companion object {
        val all: List<City> get() = AppData.cities
        /** Sitedeki varsayılan liste (Ankara ve İzmir İstanbul'la aynı dilimde olduğu için yok) */
        const val DEFAULT_KEYS = "istanbul,london,berlin,cairo,moscow,dubai,mumbai,beijing,tokyo,sydney,new-york,los-angeles"
    }
}

/** Takvim olayı: resmî tatil, arife, dini gün, özel gün, okul (sitedeki lib/events.mjs çıktısı) */
data class CalEvent(val date: LocalDate, val key: String, val type: String, val end: LocalDate?) {
    val name: String get() = L("calendar.events.$key")
}

object AppData {
    lateinit var provinces: List<Province>
        private set
    lateinit var cities: List<City>
        private set
    /** İl → ilçeleri (ada göre sıralı) */
    lateinit var districts: Map<String, List<District>>
        private set
    /** Gün → o günün olayları (birden çok güne yayılanlar her günde) */
    lateinit var events: Map<LocalDate, List<CalEvent>>
        private set
    lateinit var ramadans: List<ClosedRange<LocalDate>>
        private set
    lateinit var schoolBreaks: List<ClosedRange<LocalDate>>
        private set
    lateinit var projectedYears: Set<Int>
        private set

    fun init(c: Context) {
        fun read(name: String) = c.assets.open(name).bufferedReader().use { it.readText() }

        val p = JSONArray(read("provinces.json"))
        provinces = (0 until p.length()).map { i ->
            val o = p.getJSONObject(i)
            Province(o.getString("slug"), o.getString("name"), o.getDouble("lat"), o.getDouble("lon"), o.getString("ar"))
        }

        val dj = JSONObject(read("districts.json"))
        districts = dj.keys().asSequence().associateWith { slug ->
            val arr = dj.getJSONArray(slug)
            (0 until arr.length()).map { i -> arr.getJSONArray(i).let { District(it.getString(0), it.getDouble(1), it.getDouble(2), slug) } }
        }

        val ci = JSONArray(read("cities.json"))
        cities = (0 until ci.length()).map { i ->
            val o = ci.getJSONObject(i)
            City(o.getString("key"), o.getString("tz"), o.getDouble("lat"), o.getDouble("lon"),
                 o.getJSONObject("name").toMap(), o.getJSONObject("country").toMap())
        }

        Hijri.load(read("hijri.json"))
        Fitre.load(read("fitre.json"))

        val cal = JSONObject(read("calendar.json"))
        val years = cal.getJSONObject("years")
        val byDay = HashMap<LocalDate, MutableList<CalEvent>>()
        val school = ArrayList<ClosedRange<LocalDate>>()
        val projected = HashSet<Int>()
        for (y in years.keys()) {
            val yo = years.getJSONObject(y)
            if (yo.optBoolean("projected")) projected += y.toInt()
            val ev = yo.getJSONArray("events")
            for (i in 0 until ev.length()) {
                val o = ev.getJSONObject(i)
                val start = LocalDate.parse(o.getString("date"))
                val end = if (o.has("end") && !o.isNull("end")) LocalDate.parse(o.getString("end")) else null
                val e = CalEvent(start, o.getString("key"), o.getString("type"), end)
                var d = start
                while (!d.isAfter(end ?: start)) {
                    byDay.getOrPut(d) { ArrayList() }.add(e)
                    d = d.plusDays(1)
                }
            }
            val sr = yo.getJSONArray("schoolRanges")
            for (i in 0 until sr.length()) {
                val o = sr.getJSONObject(i)
                school += LocalDate.parse(o.getString("from"))..LocalDate.parse(o.getString("to"))
            }
        }
        events = byDay
        schoolBreaks = school
        projectedYears = projected
        val r = cal.getJSONArray("ramadans")
        // Son Ramazan'ın bitişi henüz belli olmayabilir (veride null): 29 gün sayılır
        ramadans = (0 until r.length()).map { i ->
            val o = r.getJSONObject(i)
            val from = LocalDate.parse(o.getString("from"))
            from..(if (o.isNull("to")) from.plusDays(29) else LocalDate.parse(o.getString("to")))
        }
    }

    fun isRamadan(d: LocalDate) = ramadans.any { d in it }
    fun isSchoolBreak(d: LocalDate) = schoolBreaks.any { d in it }

    /** Bugünden sonraki özel günler (okul hariç) */
    fun upcoming(from: LocalDate, limit: Int): List<CalEvent> =
        events.entries.filter { !it.key.isBefore(from) }.sortedBy { it.key }
            .flatMap { e -> e.value.filter { it.type != "school" && it.date == e.key } }
            .take(limit)

    /** Olay türünün rengi (sitedeki takvim renkleri) */
    fun color(type: String): Color = when (type) {
        "holiday" -> Color(0xFFD32F2F)
        "half" -> Color(0xFFF57C00)
        "religious" -> Color(0xFF2E7D32)
        "school" -> Color(0xFF7B1FA2)
        else -> Color(0xFF1976D2)
    }
}

private fun JSONObject.toMap(): Map<String, String> = keys().asSequence().associateWith { getString(it) }

/** İki nokta arası kilometre (haversine) */
fun km(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
    val r = Math.PI / 180
    val dLat = (bLat - aLat) * r
    val dLon = (bLon - aLon) * r
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(aLat * r) * cos(bLat * r) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * 6371 * asin(sqrt(h))
}
