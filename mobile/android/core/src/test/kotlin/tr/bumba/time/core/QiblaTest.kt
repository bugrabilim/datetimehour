package tr.bumba.time.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class QiblaTest {
    /** Sitedeki hesapla (src/js/prayer-calc.js) aynı sonuç: 81 il, dünya şehirleri, dört tarih */
    @Test
    fun matchesWebCalculation() {
        val file = Vectors.load("qibla.json")
        val dates = file.getJSONArray("dates").let { a -> List(a.length()) { i -> a.getJSONArray(i).let { d -> Triple(d.getInt(0), d.getInt(1), d.getInt(2)) } } }
        val cases = file.getJSONArray("cases").objects()
        assertTrue(cases.size > 100)
        var none = 0
        for (c in cases) {
            val id = c.getString("id")
            val q = Qibla.direction(c.getDouble("lat"), c.getDouble("lon"))
            assertEquals(c.getDouble("bearing"), q.bearing, 1e-9, id)
            assertEquals(c.getDouble("km"), q.km, 1e-6, id)
            val tz = c.getJSONArray("tz")
            val time = c.getJSONArray("time")
            dates.forEachIndexed { i, (y, m, d) ->
                val got = Qibla.time(y, m, d, c.getDouble("lat"), c.getDouble("lon"), tz.getDouble(i))
                val want = if (time.isNull(i)) null else time.getDouble(i)
                when {
                    want != null && got != null -> assertEquals(want, got, 1e-6, "$id $y-$m-$d")
                    want == null && got == null -> none++
                    else -> fail("$id $y-$m-$d: kıble saati var/yok farkı (web: $want, Kotlin: $got)")
                }
            }
        }
        assertTrue(none > 0, "kıble saati olmayan yerler test verisinde olmalı")
    }

    /** İstanbul: kıble 151,6°, Kâbe'ye ~2405 km; 28 Ekim 2026 kıble saati Diyanet'te 11.09 */
    @Test
    fun istanbul() {
        val q = Qibla.direction(41.01, 28.98)
        assertEquals(151.6, q.bearing, 0.1)
        assertEquals(2405.0, q.km, 5.0)
        val t = assertNotNull(Qibla.time(2026, 10, 28, 41.01, 28.98, 3.0))
        assertTrue(abs(jsRound(t * 60).toInt() - (11 * 60 + 9)) <= 1)
    }
}
