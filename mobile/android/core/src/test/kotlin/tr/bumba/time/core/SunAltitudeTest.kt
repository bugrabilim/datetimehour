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

    /** Bayram namazı ve ek vakitler (2. analiz #56, #59): sitedeki prayer-calc.js değerleri, İstanbul 3 Kasım 2026 */
    @Test
    fun extraAndBayram() {
        val day = PrayerTimes.times(2026, 11, 3, 41.0082, 28.9784, 3.0)
        val e = PrayerTimes.extraTimes(2026, 11, 3, 41.0082, 28.9784, 3.0, day)
        val want = listOf(
            e.israk to 8.17432610524963, e.duha to 12.07736148348695, e.isfirar to 17.405376001886776,
            e.istibak to 18.80257678714269, e.isaSani to 19.60495212576094, e.midnight to 24.07736148348695,
            e.tahajjud to 26.07218865697815, e.seher to 28.067015830469348,
        )
        want.forEach { (got, exp) -> assertEquals(exp, got!!, 1e-6) }
        assertEquals(8.13187983833751, PrayerTimes.bayram(day)!!, 1e-6)
        // Bursa 20 Mart 2026 (Ramazan Bayramı): Diyanet 07.38
        val bursa = PrayerTimes.times(2026, 3, 20, 40.1826, 29.0665, 3.0)
        assertEquals(7 * 60 + 38, (PrayerTimes.bayram(bursa)!! * 60).roundToInt())
    }

    /** Geleneksel Hint zaman dilimleri: sitedeki hesapla aynı; Delhi 8 Kasım 2026 Drik Panchang ile ±1 dk */
    @Test
    fun hinduKaal() {
        val cases = Vectors.load("kaal.json").getJSONArray("cases").objects()
        assertTrue(cases.size > 400)
        for (c in cases) {
            val k = PrayerTimes.hinduKaal(c.getInt("y"), c.getInt("m"), c.getInt("d"), c.getDouble("lat"), c.getDouble("lon"), c.getDouble("tz"))
            val id = c.getString("id")
            if (c.isNull("t")) { assertTrue(k == null, id); continue }
            val want = c.getJSONArray("t")
            val got = listOf(k!!.sunrise, k.sunset, k.rahu.first, k.rahu.second, k.yamaganda.first, k.yamaganda.second,
                k.gulika.first, k.gulika.second, k.abhijit.first, k.abhijit.second, k.brahma.first, k.brahma.second)
            got.forEachIndexed { i, v -> assertEquals(want.getDouble(i), v, 1e-6, id) }
        }
        val d = PrayerTimes.hinduKaal(2026, 11, 8, 28.6139, 77.209, 5.5)!!
        for ((h, want) in listOf(d.rahu.first to 16 * 60 + 10, d.yamaganda.first to 12 * 60 + 5, d.gulika.first to 14 * 60 + 48,
            d.abhijit.first to 11 * 60 + 43, d.brahma.first to 4 * 60 + 53)) {
            assertTrue(kotlin.math.abs(jsRound(h * 60).toInt() - want) <= 1)
        }
    }
}
