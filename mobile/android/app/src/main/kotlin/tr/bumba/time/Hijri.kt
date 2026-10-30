package tr.bumba.time

import org.json.JSONArray
import java.time.LocalDate

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
 */
object Hijri {
    /** (ay başının epoch günü, hicri yıl, ay); son satır tablonun sonu */
    private var table: List<Triple<Long, Int, Int>> = emptyList()

    fun load(json: String) {
        val a = JSONArray(json)
        table = (0 until a.length()).map { i ->
            val r = a.getJSONArray(i)
            Triple(LocalDate.parse(r.getString(0)).toEpochDay(), r.getInt(1), r.getInt(2))
        }
    }

    /** Hicri çevirici (rakip analizi #13): tabloda Diyanet, dışında Java'nın Umm al-Qura takvimi (`outside`) */
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
        for (i in 0 until table.size - 1) {
            val (start, y, m) = table[i]
            if (y == year && m == month) {
                val len = (table[i + 1].first - start).toInt()
                return if (day > len) Reverse.TooLong(len) else Reverse.Date(LocalDate.ofEpochDay(start + day - 1), false)
            }
        }
        return try {
            val first = java.time.chrono.HijrahDate.of(year, month, 1)
            val len = first.lengthOfMonth()
            if (day > len) Reverse.TooLong(len) else Reverse.Date(LocalDate.from(first.plus((day - 1).toLong(), java.time.temporal.ChronoUnit.DAYS)), true)
        } catch (_: Exception) { Reverse.None }
    }

    /** Takvim günü için hicri tarih; tablo dışında null */
    fun date(d: LocalDate): HijriDate? {
        val n = d.toEpochDay()
        for (i in table.size - 2 downTo 0) {
            val (start, y, m) = table[i]
            if (n >= start) return if (n < table[i + 1].first) HijriDate(y, m, (n - start + 1).toInt()) else null
        }
        return null
    }
}
