package tr.bumba.time.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class MethodsCrescentTest {
    /** Hesap yöntemleri ve Hanefî ikindisi: sitedeki methodParams ile aynı sonuç */
    @Test
    fun methodsMatchWeb() {
        val file = Vectors.load("prayer-methods.json")
        val ids = file.getJSONObject("methods").keys().asSequence().toList()
        assertEquals(PrayerTimes.METHOD_IDS.sorted(), ids.sorted())
        val cases = file.getJSONArray("cases").objects()
        assertTrue(cases.size > 700)
        for (c in cases) {
            val p = PrayerTimes.Parameters.method(c.getString("method"), c.getBoolean("hanafi"))
            val day = PrayerTimes.times(c.getInt("y"), c.getInt("m"), c.getInt("d"), c.getDouble("lat"), c.getDouble("lon"), c.getDouble("tz"), p)
            val t = c.getJSONArray("t")
            Prayer.entries.forEachIndexed { i, prayer ->
                val label = "${c.getString("id")} ${c.getString("method")} ${c.getBoolean("hanafi")} ${c.getInt("m")}-${c.getInt("d")} ${prayer.key}"
                val want = if (t.isNull(i)) null else t.getDouble(i)
                val got = day[prayer]
                when {
                    want != null && got != null -> assertEquals(want, got, 1e-6, label)
                    want == null && got == null -> {}
                    else -> fail("$label: web $want, Kotlin $got")
                }
            }
        }
        // Hanefî ikindisi her zaman daha geç
        val a = PrayerTimes.times(2026, 10, 30, 41.01, 28.98, 3.0)
        val h = PrayerTimes.times(2026, 10, 30, 41.01, 28.98, 3.0, PrayerTimes.Parameters.method("diyanet", true))
        assertTrue(h.asr!! - a.asr!! > 0.5)
    }

    /** Hilal görünürlüğü (Yallop): sitedeki crescent ile aynı gün batımı, Ay batımı (±1 sn), q ve sınıf */
    @Test
    fun crescentMatchesWeb() {
        val cases = Vectors.load("nature.json").getJSONArray("crescent").objects()
        assertTrue(cases.size > 50)
        val groups = cases.groupBy { Triple(it.getLong("conj"), it.getDouble("lat"), it.getDouble("lon")) }
        for ((key, list) in groups) {
            val got = Nature.crescent(key.first, key.second, key.third, 3)
            assertEquals(list.size, got.size, "akşam sayısı $key")
            list.zip(got).forEach { (w, g) ->
                assertTrue(abs(w.getLong("sunset") - g.sunset) <= 1000, "gün batımı $key")
                if (w.isNull("moonset")) assertEquals(null, g.moonset) else assertTrue(abs(w.getLong("moonset") - g.moonset!!) <= 1000, "Ay batımı $key")
                if (w.isNull("q")) assertEquals(null, g.q) else assertEquals(w.getDouble("q"), g.q!!, 1e-4, "q $key")
                assertEquals(w.getString("cat"), g.category, "sınıf $key")
            }
        }
    }
}
