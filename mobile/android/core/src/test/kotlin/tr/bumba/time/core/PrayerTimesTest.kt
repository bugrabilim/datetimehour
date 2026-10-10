package tr.bumba.time.core

import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class PrayerTimesTest {
    /** Sitedeki hesapla (src/js/prayer-calc.js) aynı sonuç: 81 il, 30 dünya şehri, kutup bölgeleri */
    @Test
    fun matchesWebCalculation() {
        val file = Vectors.load("prayer-times.json")
        val keys = file.getJSONArray("keys").let { a -> List(a.length()) { a.getString(it) } }
        assertEquals(Prayer.entries.map { it.key }, keys)
        val cases = file.getJSONArray("cases").objects()
        assertTrue(cases.size > 1500)
        var worst = 0.0
        var missing = 0
        for (c in cases) {
            val day = PrayerTimes.times(c.getInt("y"), c.getInt("m"), c.getInt("d"), c.getDouble("lat"), c.getDouble("lon"), c.getDouble("tz"))
            val t = c.getJSONArray("t")
            Prayer.entries.forEachIndexed { i, prayer ->
                val label = "${c.getString("id")} ${c.getInt("y")}-${c.getInt("m")}-${c.getInt("d")} ${prayer.key}"
                val want = if (t.isNull(i)) null else t.getDouble(i)
                val got = day[prayer]
                when {
                    want != null && got != null -> {
                        worst = maxOf(worst, abs(want - got))
                        assertEquals(want, got, 1e-6, label)
                    }
                    want == null && got == null -> missing++
                    else -> fail("$label: vakit var/yok farkı (web: $want, Kotlin: $got)")
                }
            }
        }
        assertTrue(missing > 0, "kutup bölgesi durumları test verisinde olmalı")
        println("core namaz: ${cases.size} gün, en büyük fark $worst saat")
    }

    /** İstanbul 9 Ekim 2026: Diyanet'in yayımladığı vakitler (±1 dk); scripts/test.mjs ile aynı denetim */
    @Test
    fun istanbulMatchesDiyanet() {
        val day = PrayerTimes.times(2026, 10, 9, 41.01, 28.98, 3.0)
        val want = mapOf(
            Prayer.FAJR to "05:37", Prayer.SUNRISE to "07:02", Prayer.DHUHR to "12:57",
            Prayer.ASR to "16:07", Prayer.MAGHRIB to "18:41", Prayer.ISHA to "20:00",
        )
        for ((prayer, hm) in want) {
            val (h, m) = hm.split(":").map { it.toInt() }
            val got = assertNotNull(day.minutes(prayer), prayer.key)
            assertTrue(abs(got - (h * 60 + m)) <= 1, "${prayer.key}: $got dk ≠ $hm")
        }
    }

    /** Saat dilimi nesnesiyle çağrı, sabit farkla aynı sonucu verir */
    @Test
    fun zoneVariant() {
        val a = PrayerTimes.times(2026, 10, 9, 41.01, 28.98, 3.0)
        val b = PrayerTimes.times(LocalDate.of(2026, 10, 9), 41.01, 28.98, ZoneOffset.ofHours(3))
        assertEquals(a, b)
    }

    /** Reykjavik yaz gündönümü: imsak/yatsı açısı oluşmaz, yedide bir kuralıyla bütün vakitler dolu */
    @Test
    fun highLatitudeRule() {
        val day = PrayerTimes.times(2027, 6, 21, 64.1, -21.9, 0.0)
        for (prayer in Prayer.entries) assertNotNull(day[prayer], prayer.key)
    }
}
