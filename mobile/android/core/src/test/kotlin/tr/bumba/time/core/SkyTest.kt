package tr.bumba.time.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class SkyTest {
    /** Gezegen konumları ve tutulma ayrıntısı sitedeki hesapla aynı (src/js/sky-calc.js, nature-calc.js) */
    @Test
    fun matchesWebCalculation() {
        val file = Vectors.load("sky.json")
        val planets = file.getJSONArray("planets").objects()
        assertTrue(planets.size > 100)
        for (c in planets) {
            val p = Sky.planet(c.getString("name"), c.getLong("ms").toDouble())
            val id = c.getString("name") + " " + c.getLong("ms")
            assertEquals(c.getDouble("ra"), p.ra, 1e-7, id)
            assertEquals(c.getDouble("dec"), p.dec, 1e-7, id)
            assertEquals(c.getDouble("dist"), p.dist, 1e-9, id)
            assertEquals(c.getDouble("elong"), p.elong, 1e-7, id)
            assertEquals(c.getDouble("mag"), p.mag, 1e-7, id)
        }
        val ms = { o: org.json.JSONObject, k: String -> if (o.isNull(k)) null else o.getLong(k) }
        val close = { want: Long?, got: Long?, id: String -> if (want == null) assertNull(got, id) else if (got == null || kotlin.math.abs(want - got) > 2000) fail("$id: $want / $got") }
        for (c in file.getJSONArray("lunar").objects()) {
            val l = Sky.lunarContacts(c.getLong("ut").toDouble())
            val id = "Ay tutulması " + c.getLong("ut")
            for ((k, v) in listOf("p1" to l.p1, "u1" to l.u1, "u2" to l.u2, "max" to l.max, "u3" to l.u3, "u4" to l.u4, "p4" to l.p4)) close(ms(c, k), v, "$id $k")
            assertEquals(c.getDouble("umag"), l.umag, 1e-6, id)
        }
        for (c in file.getJSONArray("solar").objects()) {
            val got = Sky.solarLocal(c.getLong("ut").toDouble(), c.getDouble("lat"), c.getDouble("lon"))
            val id = "Güneş tutulması " + c.getLong("ut") + " " + c.getDouble("lat")
            if (c.isNull("r")) { assertNull(got, id); continue }
            val r = c.getJSONObject("r")
            if (got == null) fail("$id: görülmeli")
            close(ms(r, "c1"), got.c1, "$id c1"); close(ms(r, "c4"), got.c4, "$id c4"); close(r.getLong("max"), got.max, "$id max")
            assertEquals(r.getDouble("obsc"), got.obsc, 1e-4, id)
            assertEquals(r.getString("central"), got.central, id)
        }
    }
}
