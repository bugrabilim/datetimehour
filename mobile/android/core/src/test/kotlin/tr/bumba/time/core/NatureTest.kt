package tr.bumba.time.core

import java.time.Instant
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NatureTest {
    /** Sitedeki hesapla (src/js/nature-calc.js) aynı sonuç: mevsim anları, ay evreleri, Ay durumu */
    @Test
    fun matchesWebCalculation() {
        val file = Vectors.load("nature.json")
        val seasons = file.getJSONArray("seasons").objects()
        assertTrue(seasons.size > 40)
        for (s in seasons) {
            val got = Nature.seasons(s.getInt("y"))
            listOf(got.march to "march", got.june to "june", got.september to "sept", got.december to "dec").forEach { (g, key) ->
                assertTrue(abs(g - s.getLong(key)) <= 1, "mevsim ${s.getInt("y")} $key: $g ≠ ${s.getLong(key)}")
            }
        }
        for (p in file.getJSONArray("phases").objects()) {
            val k = p.getInt("k")
            assertTrue(abs(Nature.phaseMillis(k, false) - p.getLong("new")) <= 1, "yeni ay k=$k")
            assertTrue(abs(Nature.phaseMillis(k, true) - p.getLong("full")) <= 1, "dolunay k=$k")
        }
        for (m in file.getJSONArray("moon").objects()) {
            val ms = m.getLong("ms")
            val got = Nature.moon(ms)
            assertEquals(m.getDouble("age"), got.age, 1e-9, "Ay yaşı $ms")
            assertEquals(m.getDouble("frac"), got.fraction, 1e-12, "Ay döngüsü $ms")
            assertEquals(m.getDouble("illum"), got.illumination, 1e-12, "Ay aydınlanma $ms")
            assertEquals(m.getInt("idx"), got.phaseIndex, "Ay evresi $ms")
            assertTrue(abs(got.nextNewMoon - m.getLong("nextNew")) <= 1, "sonraki yeni ay $ms")
            assertTrue(abs(got.nextFullMoon - m.getLong("nextFull")) <= 1, "sonraki dolunay $ms")
        }
    }

    /** Ay'ın konumu ve doğuş/batışı sitedeki hesapla aynı (doğuş/batış ikiye bölme ile bulunduğu için ±1 sn) */
    @Test
    fun moonRiseSetMatchesWeb() {
        val file = Vectors.load("nature.json")
        for (p in file.getJSONArray("moonPos").objects()) {
            val got = Nature.moonPosition(p.getDouble("ms"))
            assertEquals(p.getDouble("ra"), got.rightAscension, 1e-9, "sağ açıklık")
            assertEquals(p.getDouble("dec"), got.declination, 1e-9, "dik açıklık")
            assertEquals(p.getDouble("par"), got.parallax, 1e-12, "paralaks")
        }
        val rises = file.getJSONArray("moonRise").objects()
        assertTrue(rises.size > 50)
        for (r in rises) {
            val got = Nature.moonRiseSet(r.getLong("start"), r.getDouble("lat"), r.getDouble("lon"))
            for ((key, g) in listOf("rise" to got.rise, "set" to got.set)) {
                val want = if (r.isNull(key)) null else r.getLong(key)
                assertEquals(want == null, g == null, "$key var mı ${r.getDouble("lat")} ${r.getLong("start")}")
                if (want != null && g != null) assertTrue(abs(g - want) <= 1000, "$key ${r.getDouble("lat")} ${r.getLong("start")}")
            }
        }
    }

    /** ABD Deniz Gözlemevi (USNO) ile: İstanbul 30 Ekim 2026 Ay doğuşu 21.29, batışı 12.36 (±1 dk) */
    @Test
    fun moonRiseSetMatchesUsno() {
        val start = Instant.parse("2026-10-29T21:00:00Z").toEpochMilli()
        val r = Nature.moonRiseSet(start, 41.01, 28.98)
        assertTrue(abs(r.rise!! - (start + (21 * 60 + 29) * 60_000L)) <= 90_000)
        assertTrue(abs(r.set!! - (start + (12 * 60 + 36) * 60_000L)) <= 90_000)
    }

    /** Yayımlanmış astronomik anlarla (±2 dk); scripts/test.mjs ile aynı denetim */
    @Test
    fun seasonsMatchPublishedInstants() {
        val want = mapOf(
            2025 to listOf("2025-03-20T09:01:00Z", "2025-06-21T02:42:00Z", "2025-09-22T18:19:00Z", "2025-12-21T15:03:00Z"),
            2026 to listOf("2026-03-20T14:46:00Z", "2026-06-21T08:24:00Z", "2026-09-23T00:06:00Z", "2026-12-21T20:50:00Z"),
        )
        for ((year, list) in want) {
            val s = Nature.seasons(year)
            listOf(s.march, s.june, s.september, s.december).zip(list).forEach { (got, text) ->
                assertTrue(abs(got - Instant.parse(text).toEpochMilli()) <= 2 * 60_000, "$year $text")
            }
        }
    }
}
