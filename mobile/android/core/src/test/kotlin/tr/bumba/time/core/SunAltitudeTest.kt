package tr.bumba.time.core

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/** Altın/mavi saat (Güneş yüksekliği anları) ve kerahat vakitleri (30 Ekim 2026) */
class SunAltitudeTest {
    /** Sitedeki hesapla (src/js/prayer-calc.js sunAltTimes) aynı sonuç: 81 il, dünya şehirleri, dört tarih, üç yükseklik */
    @Test
    fun matchesWebCalculation() {
        val file = Vectors.load("sun-altitude.json")
        val alts = file.getJSONArray("altitudes").let { a -> List(a.length()) { a.getDouble(it) } }
        val cases = file.getJSONArray("cases").objects()
        assertTrue(cases.size > 400)
        var none = 0
        for (c in cases) {
            val id = c.getString("id")
            val t = c.getJSONArray("t")
            alts.forEachIndexed { i, alt ->
                val r = PrayerTimes.sunAltitudeTimes(c.getInt("y"), c.getInt("m"), c.getInt("d"), c.getDouble("lat"), c.getDouble("lon"), c.getDouble("tz"), alt)
                listOf(r.rise, r.set).forEachIndexed { j, got ->
                    val k = i * 2 + j
                    val want = if (t.isNull(k)) null else t.getDouble(k)
                    when {
                        want != null && got != null -> assertEquals(want, got, 1e-6, "$id $alt")
                        want == null && got == null -> none++
                        else -> fail("$id $alt: var/yok farkı (web: $want, Kotlin: $got)")
                    }
                }
            }
        }
        assertTrue(none > 0, "yüksek enlemde oluşmayan anlar test verisinde olmalı")
    }

    /** İstanbul 30 Ekim 2026: sabah altın saat 07.15–08.11, akşam 17.24–18.20; kerahat 45 dk */
    @Test
    fun istanbul() {
        val l = PrayerTimes.lightHours(2026, 10, 30, 41.01, 28.98, 3.0)
        fun m(x: Double?) = ((x ?: 0.0) * 60).roundToInt()
        assertEquals(7 * 60 + 15, m(l.goldenAm.first))
        assertEquals(8 * 60 + 11, m(l.goldenAm.second))
        assertEquals(17 * 60 + 24, m(l.goldenPm.first))
        assertEquals(18 * 60 + 31, m(l.bluePm.second))
        val day = PrayerTimes.times(2026, 10, 30, 41.01, 28.98, 3.0)
        val k = PrayerTimes.kerahat(day)
        assertNotNull(k.israk)
        assertEquals(0.75, k.israk!!.second - day.sunrise!!, 1e-12)
        assertEquals(0.75, day.maghrib!! - k.isfirar!!.first, 1e-12)
    }
}
