package tr.bumba.time

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import tr.bumba.time.core.Prayer
import tr.bumba.time.core.PrayerTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.resume

/**
 * Uygulamanın konumu: bir il ya da "Otomatik" (telefonun konumundan; en yakın il 150 km içindeyse o il, değilse
 * tam konum ve cihazın saat dilimi; yakında dünya şehri varsa adı). iOS'taki AppPlace ile aynı kurallar.
 */
data class AppPlace(val id: String, val name: String, val lat: Double, val lon: Double, val zone: ZoneId) {
    val isAuto: Boolean get() = id == AUTO

    data class Entry(val prayer: Prayer, val at: Instant)

    /** Günün vakitleri, sırayla (oluşmayan vakit yok); sitedeki gibi dakikaya yuvarlanır */
    fun entries(day: LocalDate): List<Entry> {
        val t = PrayerTimes.times(day, lat, lon, zone, PrayerMethod.params())
        val start = day.atStartOfDay(zone).toInstant()
        return Prayer.entries.mapNotNull { p -> t.minutes(p)?.let { Entry(p, start.plusSeconds(it * 60L)) } }
    }

    /** Şu andan sonraki ilk vakit (bugün kalmadıysa yarının ilki) */
    fun next(now: Instant): Entry? {
        val today = now.atZone(zone).toLocalDate()
        return (entries(today) + entries(today.plusDays(1))).firstOrNull { it.at.isAfter(now) }
    }

    fun today(now: Instant = Instant.now()): LocalDate = now.atZone(zone).toLocalDate()

    companion object {
        const val AUTO = "auto"
        val turkey: ZoneId = ZoneId.of("Europe/Istanbul")

        fun of(p: Province, id: String = p.slug) = AppPlace(id, p.displayName, p.lat, p.lon, turkey)
        fun of(d: District) = AppPlace(d.id, d.label, d.lat, d.lon, turkey)

        /** Kayıtlı kimlikten yer: il, ilçe ("d:il:ilçe") ya da otomatik */
        fun byId(id: String): AppPlace = when {
            id == AUTO -> current()
            id.startsWith("d:") -> District.find(id)?.let { of(it) } ?: of(Province.find(id.split(":").getOrElse(1) { "istanbul" }))
            else -> of(Province.find(id))
        }

        /** Kayıtlı seçim (Prefs.province okunur; Compose'da değişince yeniden hesaplanır) */
        fun current(): AppPlace {
            if (!Prefs.provinceChosen) DefaultPlace.place()?.let { return it }
            val slug = Prefs.province
            if (slug.startsWith("d:")) District.find(slug)?.let { return of(it) }
            if (slug != AUTO) return of(Province.find(slug.removePrefix("d:").substringBefore(":")))
            val lat = Prefs.autoLat() ?: return of(Province.find("istanbul"), AUTO)
            val ref = Prefs.autoRef()
            if (ref.startsWith("p:")) return of(Province.find(ref.drop(2)), AUTO)
            val city = if (ref.startsWith("c:")) City.all.firstOrNull { it.key == ref.drop(2) } else null
            val zone = runCatching { ZoneId.of(Prefs.autoZone()) }.getOrDefault(ZoneId.systemDefault())
            return AppPlace(AUTO, city?.displayName ?: L("place.myLocation"), lat, Prefs.autoLon(), zone)
        }

        /** Konumu kaydeder: en yakın il (150 km), değilse yakındaki dünya şehri (100 km) ya da "Konumum" */
        fun save(lat: Double, lon: Double) {
            val (p, d) = Province.nearest(lat, lon)
            val ref = when {
                d <= 150 -> "p:" + p.slug
                else -> City.all.map { it to km(it.lat, it.lon, lat, lon) }.minByOrNull { it.second }
                    ?.takeIf { it.second <= 100 }?.let { "c:" + it.first.key } ?: ""
            }
            Prefs.saveAuto(Math.round(lat * 100) / 100.0, Math.round(lon * 100) / 100.0, ZoneId.systemDefault().id, ref)
            Prefs.saveProvince(AUTO)
        }

        /** Telefonun yaklaşık konumu (izin verilmişse); yoksa null */
        @SuppressLint("MissingPermission")
        suspend fun locate(context: Context): Location? {
            val lm = context.getSystemService(LocationManager::class.java) ?: return null
            val provider = when {
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> return null
            }
            if (Build.VERSION.SDK_INT >= 30) {
                val fresh = suspendCancellableCoroutine { cont ->
                    lm.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(context)) { cont.resume(it) }
                }
                if (fresh != null) return fresh
            }
            return lm.getLastKnownLocation(provider)
        }
    }
}

/**
 * Konum hiç seçilmediyse varsayılan yer (10 Kasım 2026, sahibin onayı; önceden her yerde İstanbul'du): telefonun saat
 * dilimi Türkiye'ninse İstanbul (il, Diyanet vakitleri; null döner), değilse o saat dilimindeki dünya şehri (cities.json)
 * ya da en kalabalık yer (places.json); dilim listede yoksa aynı bölge önekli ve şu an aynı UTC farkındaki en kalabalık yer.
 * Saat telefonun saat diliminden. Kaydedilmez: kişi bir yer seçene kadar geçerlidir. iOS'taki DefaultPlace ile aynı kural.
 */
object DefaultPlace {
    private val TURKEY = setOf("Europe/Istanbul", "Asia/Istanbul", "Turkey")
    /** Verideki eski adlar (telefon yeni adı verebilir) */
    private val ALIASES = mapOf(
        "Europe/Kyiv" to "Europe/Kiev", "Asia/Calcutta" to "Asia/Kolkata", "Asia/Saigon" to "Asia/Ho_Chi_Minh",
        "Asia/Katmandu" to "Asia/Kathmandu", "Asia/Rangoon" to "Asia/Yangon", "Asia/Yangon" to "Asia/Rangoon",
        "America/Buenos_Aires" to "America/Argentina/Buenos_Aires", "Asia/Dacca" to "Asia/Dhaka",
    )
    @Volatile private var cache: Triple<String, String, AppPlace?>? = null

    fun inTurkey(zone: ZoneId = ZoneId.systemDefault()) = zone.id in TURKEY

    fun place(zone: ZoneId = ZoneId.systemDefault()): AppPlace? {
        val lang = Strings.lang
        cache?.let { if (it.first == zone.id && it.second == lang) return it.third }
        val p = if (inTurkey(zone)) null else runCatching { find(zone) }.getOrNull()
        cache = Triple(zone.id, lang, p)
        return p
    }

    private fun find(zone: ZoneId): AppPlace? {
        val ids = setOfNotNull(zone.id, ALIASES[zone.id])
        fun mk(name: String, lat: Double, lon: Double) = AppPlace(AppPlace.AUTO, name, lat, lon, zone)
        City.all.firstOrNull { it.tz in ids }?.let { return mk(it.displayName, it.lat, it.lon) }
        val places = GlobePlace.all(Strings.appContext)
        places.filter { it.zone.id in ids }.maxByOrNull { it.population }?.let { return mk(it.name, it.lat, it.lon) }
        // Dilim verimizde yoksa: aynı bölgede (Europe/, America/…) şu an aynı UTC farkındaki en kalabalık yer
        val now = Instant.now()
        val off = zone.rules.getOffset(now)
        val region = zone.id.substringBefore('/')
        if (region == zone.id) return null
        return places.filter { it.zone.id.substringBefore('/') == region && it.zone.rules.getOffset(now) == off }
            .maxByOrNull { it.population }?.let { mk(it.name, it.lat, it.lon) }
    }
}

val Prayer.title: String get() = L("prayer.$key")

/** Ramazan'da (Diyanet hicri takvimine göre 9. ay) akşam "İftar", imsak "Sahur (imsak)" (2. analiz #55) */
fun Prayer.ramadanTitle(at: java.time.Instant, zone: java.time.ZoneId): String {
    if (this != Prayer.MAGHRIB && this != Prayer.FAJR) return title
    if (Hijri.date(at.atZone(zone).toLocalDate())?.month != 9) return title
    return L(if (this == Prayer.MAGHRIB) "live.iftar" else "live.sahur")
}
