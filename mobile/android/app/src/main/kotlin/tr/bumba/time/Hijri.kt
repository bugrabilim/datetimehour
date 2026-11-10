package tr.bumba.time

import org.json.JSONArray
import java.time.LocalDate
import tr.bumba.time.core.PrayerTimes

/** Hicri tarih: yıl, ay (1 Muharrem … 12 Zilhicce), gün */
data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = L("hijri.month.$month")
    /** "19 Cemaziyelevvel 1448" */
    val text: String get() = T("hijri.fmt", day.toString(), monthName, year.toString())
}

/**
 * Hicri tarih (rakip analizi #9). Ay başları Diyanet'e hizalı tablodan (assets/hijri.json; sitedeki lib/hijri.mjs üretir:
 * Diyanet vakit tablolarındaki hicri tarihler ve dini günler, dayanağı olmayan aylarda Umm al-Qura). Java'nın
 * HijrahDate'i (Umm al-Qura) bazı aylarda Diyanet'ten bir gün ayrıldığı için kullanılmaz. Gün gece yarısı değişir.
 * Tablonun dışında (şu an 21 Aralık 2025 – 13 Ocak 2032) tarih yoktur. iOS'taki Hijri.swift ile aynı kural.
 *
 * Endonezya takvimi (10 Kasım 2026; assets/hijri-id.json, lib/hijri.mjs hijriTableId: Kemenag takvimi, isbat kararları,
 * dışında MABIMS hesabı): `hijri.cal` "auto" (varsayılan; yer Endonezya'daysa Endonezya, değilse Diyanet), "diyanet" ya da
 * "kemenag". `hijri.offset` −1/0/+1: ay başı bir gün önce/sonra (Hindistan gibi hilalin bir gün geç görüldüğü yerler için).
 */
object Hijri {
    const val CAL = "hijri.cal"
    const val OFFSET = "hijri.offset"
    private var diyanet: List<Triple<Long, Int, Int>> = emptyList()
    private var kemenag: List<Triple<Long, Int, Int>> = emptyList()

    private fun parse(json: String): List<Triple<Long, Int, Int>> {
        val a = JSONArray(json)
        return (0 until a.length()).map { i ->
            val r = a.getJSONArray(i)
            Triple(LocalDate.parse(r.getString(0)).toEpochDay(), r.getInt(1), r.getInt(2))
        }
    }

    fun load(json: String, idJson: String? = null) {
        diyanet = parse(json)
        kemenag = idJson?.let { runCatching { parse(it) }.getOrNull() }.orEmpty()
        cacheKey = ""
    }

    val hasKemenag: Boolean get() = kemenag.isNotEmpty()

    /** Seçili yerin saat dilimi (AppPlace.current()'i kurmadan; takvim her hücrede soruyor) */
    private fun placeZone(): String = when {
        !Prefs.ready || !Prefs.provinceChosen -> java.time.ZoneId.systemDefault().id
        Prefs.province == AppPlace.AUTO -> Prefs.autoZone()
        else -> "Europe/Istanbul"
    }

    /** Ayardaki seçim: "auto", "diyanet", "kemenag" */
    val setting: String get() = if (Prefs.ready) Prefs.store.getString(CAL, "auto") ?: "auto" else "auto"
    /** Ay başı düzeltmesi (gün): −1, 0, +1 */
    val offset: Int get() = if (Prefs.ready) Prefs.store.getInt(OFFSET, 0).coerceIn(-1, 1) else 0
    /** Yerin bölgesi Endonezya mı (otomatik seçim için) */
    fun regionIsIndonesia(zone: String = placeZone()) = PrayerTimes.region(zone) == "ID"

    /** Kullanılan takvim: "diyanet" ya da "kemenag" */
    val calendar: String get() = when (setting) {
        "kemenag" -> if (hasKemenag) "kemenag" else "diyanet"
        "diyanet" -> "diyanet"
        else -> if (hasKemenag && regionIsIndonesia()) "kemenag" else "diyanet"
    }

    /** Takvim ve düzeltme anahtarı ("diyanet:0" varsayılan): önbellekler bununla yenilenir */
    val key: String get() = "$calendar:$offset"
    val isDefault: Boolean get() = key == "diyanet:0"

    private var cacheKey = ""
    private var cacheTable: List<Triple<Long, Int, Int>> = emptyList()
    /** Kullanılan tablo: seçili takvim, ay başları düzeltme kadar kaydırılmış */
    private val table: List<Triple<Long, Int, Int>> get() {
        val k = key
        if (k != cacheKey) {
            val base = if (calendar == "kemenag") kemenag else diyanet
            val off = offset.toLong()
            cacheTable = if (off == 0L) base else base.map { Triple(it.first + off, it.second, it.third) }
            cacheKey = k
        }
        return cacheTable
    }

    private fun dateIn(t: List<Triple<Long, Int, Int>>, d: LocalDate): HijriDate? {
        val n = d.toEpochDay()
        for (i in t.size - 2 downTo 0) {
            val (start, y, m) = t[i]
            if (n >= start) return if (n < t[i + 1].first) HijriDate(y, m, (n - start + 1).toInt()) else null
        }
        return null
    }

    private fun startIn(t: List<Triple<Long, Int, Int>>, year: Int, month: Int): Pair<Long, Int>? {
        for (i in 0 until t.size - 1) {
            val (start, y, m) = t[i]
            if (y == year && m == month) return start to (t[i + 1].first - start).toInt()
        }
        return null
    }

    /** Diyanet tablosundaki hicri tarih (calendar.json'daki günlerin dayanağı) */
    fun baseDate(d: LocalDate): HijriDate? = dateIn(diyanet, d)

    /**
     * Diyanet'e göre verilmiş bir günün (calendar.json'daki dini günler, Ramazan) seçili takvimdeki karşılığı: aynı hicri
     * gün. Varsayılanda ya da tablo dışında aynı gün.
     */
    fun shift(d: LocalDate): LocalDate {
        if (isDefault) return d
        val h = dateIn(diyanet, d) ?: return d
        val (start, len) = startIn(table, h.year, h.month) ?: return d
        return LocalDate.ofEpochDay(start + minOf(h.day, len) - 1)
    }

    /** Hicri çevirici (rakip analizi #13): tabloda seçili takvim, dışında Java'nın Umm al-Qura takvimi (`outside`) */
    fun convert(d: LocalDate): Pair<HijriDate, Boolean>? {
        date(d)?.let { return it to false }
        return try {
            val h = java.time.chrono.HijrahDate.from(d)
            HijriDate(h.get(java.time.temporal.ChronoField.YEAR), h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR),
                h.get(java.time.temporal.ChronoField.DAY_OF_MONTH)) to true
        } catch (_: Exception) { null }
    }

    sealed interface Reverse {
        data class Date(val date: LocalDate, val outside: Boolean) : Reverse
        data class TooLong(val days: Int) : Reverse
        data object None : Reverse
    }

    /** Hicri (yıl, ay, gün) → miladi; ay o kadar gün çekmiyorsa ayın gün sayısı */
    fun gregorian(year: Int, month: Int, day: Int): Reverse {
        startIn(table, year, month)?.let { (start, len) ->
            return if (day > len) Reverse.TooLong(len) else Reverse.Date(LocalDate.ofEpochDay(start + day - 1), false)
        }
        return try {
            val first = java.time.chrono.HijrahDate.of(year, month, 1)
            val len = first.lengthOfMonth()
            if (day > len) Reverse.TooLong(len) else Reverse.Date(LocalDate.from(first.plus((day - 1).toLong(), java.time.temporal.ChronoUnit.DAYS)), true)
        } catch (_: Exception) { Reverse.None }
    }

    /** Hicri ayın ilk günü ve uzunluğu (takvimi hicri aylarla gezmek için; 2. analiz #119); tablo dışında null */
    fun month(year: Int, month: Int): Pair<LocalDate, Int>? =
        startIn(table, year, month)?.let { (start, len) -> LocalDate.ofEpochDay(start) to len }

    /** Takvim günü için hicri tarih; tablo dışında null */
    fun date(d: LocalDate): HijriDate? = dateIn(table, d)
}
