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

val Prayer.title: String get() = L("prayer.$key")
