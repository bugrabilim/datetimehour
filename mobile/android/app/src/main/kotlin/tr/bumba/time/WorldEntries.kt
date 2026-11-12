package tr.bumba.time

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/*
 * Dünya saatleri kaydı (2. analiz #128, #129; 3 Kasım 2026). `world` listesindeki anahtar üç türden olabilir:
 * şehir anahtarı (cities.json, 31 şehir), "p:<enlem>:<boylam>" (places.json'daki 1960 yer; sıra değişse de koordinatla
 * bulunur) ya da "tz:<IANA dilimi>" (kısaltmayla aramadan, ör. EST). Kişisel ad ve emoji `world.labels` (JSON
 * {anahtar: ad}). iOS'taki WorldEntries.swift ile aynı biçim.
 */
data class WorldEntry(
    val key: String,
    val name: String,
    val country: String,
    val zone: ZoneId,
    val lat: Double?,
    val lon: Double?,
    /** ISO ülke kodu (bayrak için; yalnız 31 şehirde bilinir) */
    val cc: String? = null,
) {
    /** Kişisel ad ("Annem 👵"); yoksa null */
    val label: String? get() = WorldLabels.get(key)
    val flag: String get() = cc?.let { flagOf(it) } ?: ""
    /** Kısa alanlarda (saat ekranı, widget) gösterilen ad: kişisel ad varsa o */
    val displayName: String get() = label ?: name

    /** Saat dilimi kısaltması: platformun kısa adı; "GMT+3" gibi ise "UTC+3" */
    fun abbr(now: Instant): String {
        // O andaki özel kısa ad (yaz saatinde CEST, kışın CET); genel kısa ad ("BT") kullanılmaz
        val s = java.time.format.DateTimeFormatter.ofPattern("zzz", Locale.US).format(now.atZone(zone))
        if (s == "GMT" || (s.isNotEmpty() && !s.startsWith("GMT") && !s.contains('/') && s.length <= 5)) return s
        return utcOffset(now)
    }

    fun utcOffset(now: Instant): String {
        val sec = zone.rules.getOffset(now).totalSeconds
        if (sec == 0) return "UTC"
        val h = kotlin.math.abs(sec) / 3600
        val m = kotlin.math.abs(sec) % 3600 / 60
        return "UTC" + (if (sec > 0) "+" else "−") + h + (if (m > 0) ":%02d".format(Locale.ROOT, m) else "")
    }

    /** Gündüz (Güneş ufkun üstünde), alacakaranlık (0…−6°), gece; konumu bilinmeyen dilimde saate göre */
    fun light(now: Instant): String {
        if (lat == null || lon == null) {
            val h = now.atZone(zone).hour
            return when (h) { in 7..17 -> "day"; 6, 18, 19 -> "dusk"; else -> "night" }
        }
        val alt = SkyMath.sunAltitude(lat, lon, now.toEpochMilli())
        return when { alt > 0 -> "day"; alt > -6 -> "dusk"; else -> "night" }
    }

    /** Sıradaki saat değişimi (`days` gün içindeyse): an ve fark (sn) */
    fun nextDst(now: Instant, days: Long = 7): Pair<Instant, Int>? {
        val tr = zone.rules.nextTransition(now) ?: return null
        if (tr.instant.isAfter(now.plusSeconds(days * 86400))) return null
        val delta = tr.offsetAfter.totalSeconds - tr.offsetBefore.totalSeconds
        return if (delta == 0) null else tr.instant to delta
    }

    fun dstNote(now: Instant, days: Long = 7): String? {
        val (at, delta) = nextDst(now, days) ?: return null
        return T(if (delta > 0) "world.dstForward" else "world.dstBack", Fmt.dayMonth(at.atZone(zone).toLocalDate()), Fmt.hm(kotlin.math.abs(delta)))
    }

    companion object {
        /** 31 şehrin ülke kodu (bayrak) */
        val CITY_CC = mapOf(
            "istanbul" to "TR", "ankara" to "TR", "izmir" to "TR", "london" to "GB", "paris" to "FR", "berlin" to "DE", "moscow" to "RU",
            "dubai" to "AE", "baku" to "AZ", "cairo" to "EG", "new-york" to "US", "los-angeles" to "US", "tokyo" to "JP", "beijing" to "CN",
            "singapore" to "SG", "mumbai" to "IN", "sydney" to "AU", "riyadh" to "SA", "doha" to "QA", "amsterdam" to "NL", "rome" to "IT",
            "madrid" to "ES", "athens" to "GR", "vienna" to "AT", "kyiv" to "UA", "tehran" to "IR", "hong-kong" to "HK", "bangkok" to "TH",
            "toronto" to "CA", "sao-paulo" to "BR", "johannesburg" to "ZA",
        )

        /** Kısaltmayla arama (sitedeki saat dilimi kısaltmaları sayfasıyla aynı 25 kısaltma; temsilci dilim) */
        val ABBRS = linkedMapOf(
            "UTC" to "UTC", "GMT" to "Etc/GMT", "BST" to "Europe/London", "WET" to "Europe/Lisbon", "CET" to "Europe/Paris",
            "CEST" to "Europe/Paris", "EET" to "Europe/Athens", "EEST" to "Europe/Athens", "TRT" to "Europe/Istanbul", "MSK" to "Europe/Moscow",
            "GST" to "Asia/Dubai", "PKT" to "Asia/Karachi", "IST" to "Asia/Kolkata", "WIB" to "Asia/Jakarta", "WITA" to "Asia/Makassar", "WIT" to "Asia/Jayapura", "SGT" to "Asia/Singapore", "JST" to "Asia/Tokyo",
            "KST" to "Asia/Seoul", "AEST" to "Australia/Sydney", "AEDT" to "Australia/Sydney", "EST" to "America/New_York",
            "EDT" to "America/New_York", "CST" to "America/Chicago", "CDT" to "America/Chicago", "MST" to "America/Denver",
            "PST" to "America/Los_Angeles", "PDT" to "America/Los_Angeles",
        )

        fun flagOf(cc: String): String = cc.uppercase(Locale.ROOT).map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")

        fun placeKey(p: GlobePlace) = "p:%.2f:%.2f".format(Locale.ROOT, p.lat, p.lon)

        fun of(key: String): WorldEntry? = when {
            key.startsWith("p:") -> {
                val parts = key.split(":")
                val lat = parts.getOrNull(1)?.toDoubleOrNull()
                val lon = parts.getOrNull(2)?.toDoubleOrNull()
                if (lat == null || lon == null) null
                else places().minByOrNull { kotlin.math.abs(it.lat - lat) + kotlin.math.abs(it.lon - lon) }
                    ?.takeIf { kotlin.math.abs(it.lat - lat) < 0.05 && kotlin.math.abs(it.lon - lon) < 0.05 }
                    ?.let { WorldEntry(key, it.name, "", it.zone, it.lat, it.lon) }
            }
            key.startsWith("tz:") -> runCatching { ZoneId.of(key.removePrefix("tz:")) }.getOrNull()?.let { z ->
                val abbr = ABBRS.entries.firstOrNull { "tz:" + it.value == key }?.key
                val city = z.id.substringAfterLast('/').replace('_', ' ')
                WorldEntry(key, if (abbr != null && z.id != "UTC") "$abbr · $city" else city, z.id, z, null, null)
            }
            else -> City.all.firstOrNull { it.key == key }?.let { WorldEntry(it.key, it.displayName, it.displayCountry, it.zone, it.lat, it.lon, CITY_CC[it.key]) }
        }

        fun list(raw: String = Prefs.world): List<WorldEntry> = raw.split(",").filter { it.isNotEmpty() }.mapNotNull { of(it) }

        @Volatile private var placesCache: List<GlobePlace>? = null
        private fun places(): List<GlobePlace> = placesCache ?: GlobePlace.all(Strings.appContext).also { placesCache = it }

        /**
         * Arama: önce 31 şehir, sonra 1960 yer (nüfusa göre), sonra kısaltmalar; ad başta eşleşen öne. Boş aramada 31 şehir.
         * Sonuç: anahtar → kayıt (listede olanlar hariç).
         */
        fun search(q: String, exclude: Set<String>, limit: Int = 60): List<WorldEntry> {
            val f = District.fold(q.trim())
            val cities = City.all.filter { it.key !in exclude }
                .map { WorldEntry(it.key, it.displayName, it.displayCountry, it.zone, it.lat, it.lon, CITY_CC[it.key]) }
            if (f.isEmpty()) return cities
            fun score(name: String): Int { val n = District.fold(name); return when { n.startsWith(f) -> 0; n.contains(f) -> 1; else -> 9 } }
            val out = ArrayList<Pair<Int, WorldEntry>>()
            for (c in cities) { val s = minOf(score(c.name), score(c.country) + 1); if (s < 9) out += s to c }
            val cityKeys = out.map { it.second.key }.toSet()
            for (p in places()) {
                val k = placeKey(p)
                if (k in exclude || k in cityKeys) continue
                val s = score(p.name)
                if (s < 9) out += (s + 2) to WorldEntry(k, p.name, "", p.zone, p.lat, p.lon)
            }
            val up = q.trim().uppercase(Locale.ROOT)
            ABBRS[up]?.let { z -> val k = "tz:$z"; if (k !in exclude) of(k)?.let { out += 0 to it } }
            if (up.length >= 3) runCatching { ZoneId.getAvailableZoneIds().filter { District.fold(it).contains(f) && "tz:$it" !in exclude }.take(10) }
                .getOrDefault(emptyList()).forEach { z -> of("tz:$z")?.let { out += 5 to it } }
            return out.sortedBy { it.first }.map { it.second }.distinctBy { it.key }.take(limit)
        }
    }
}

/** Dünya saatlerinde kişisel ad ve emoji (`world.labels`: JSON {anahtar: ad}; yalnız cihazda) */
object WorldLabels {
    private const val KEY = "world.labels"

    private fun all(): JSONObject = runCatching { JSONObject(Prefs.store.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())

    fun get(key: String): String? = all().optString(key).takeIf { it.isNotEmpty() }

    fun set(key: String, label: String) {
        val o = all()
        if (label.isBlank()) o.remove(key) else o.put(key, label.trim().take(30))
        Prefs.store.edit().putString(KEY, o.toString()).apply()
    }
}
