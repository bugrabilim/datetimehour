package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Sitedeki araçların uygulamadaki hesapları (4 Kasım 2026): beklenen değerler sitedeki betiklerden (Node ile) alındı */
class SiteToolsTest {
    @Test fun timeCalc() {
        assertEquals(9 * 60 + 5, TimeCalc.hm("09:05"))
        assertNull(TimeCalc.hm("24:00"))
        assertEquals(510 to false, TimeCalc.between(9 * 60, 17 * 60 + 30))
        assertEquals(420 to true, TimeCalc.between(22 * 60, 5 * 60))
        // 1:30 + 45 dk + 2:15:30 − 10 dk = 4:20:30
        val (total, bad) = TimeCalc.sum("1:30\n45\n2:15:30\n−10\nabc\n")
        assertEquals(4 * 3600 + 20 * 60 + 30L, total)
        assertEquals(listOf(5), bad)
    }

    @Test fun zodiac() {
        // src/js/more-tools.js zodiac() ile aynı boylamlar (±0,001°)
        val cases = listOf(
            Triple(648032400000.0, 112.6212, 21.5069),
            Triple(978307200000.0, 280.6369, 348.7112),
            Triple(480202200000.0, 0.2185, 353.5970),
            Triple(1793793600000.0, 222.0758, 165.3790),
        )
        for ((ms, sun, moon) in cases) {
            assertEquals(sun, Zodiac.sunLon(ms), 0.001)
            assertEquals(moon, Zodiac.moonLon(ms), 0.001)
        }
        assertEquals(3, Zodiac.sign(112.6))
        assertEquals(11, Zodiac.sign(359.9))
    }

    @Test fun retire() {
        // src/js/retire.js calc() sonuçları (bugün 4 Kasım 2026)
        val today = LocalDate.of(2026, 11, 4)
        data class Case(val f: Boolean, val birth: String, val start: String, val days: Int, val rate: Int, val group: String, val best: String?, val at: String?, val routes: String)
        val cases = listOf(
            Case(true, "1975-03-12", "1995-06-01", 6200, 360, "b", "full", "2026-11-04", "full:2026-11-04,partialPre:2033-03-12"),
            Case(false, "1970-01-20", "1988-02-15", 9000, 0, "b", "full", "2026-11-04", "full:2026-11-04,partialPre:2030-01-20"),
            Case(false, "1972-07-07", "1996-11-30", 4800, 180, "b", "partialPre", "2032-07-07", "full:2032-07-15,partialPre:2032-07-07"),
            Case(true, "1980-05-05", "2002-09-01", 5000, 360, "mid", "mid1", "2038-05-05", "mid1:2038-05-05,mid2:2038-05-05"),
            Case(false, "1985-10-10", "2010-03-01", 3100, 360, "post", "full", "2047-10-10", "full:2047-10-10,partialPost:2048-10-10"),
            Case(true, "1992-02-29", "2014-01-01", 1500, 0, "post", null, null, "full:null,partialPost:null"),
            Case(false, "1990-12-31", "2009-08-15", 4000, 180, "post", "partialPost", "2053-12-31", "full:2055-12-31,partialPost:2053-12-31"),
        )
        for (c in cases) {
            val r = Retire.calc(c.f, LocalDate.parse(c.birth), LocalDate.parse(c.start), c.days, c.rate, today)
            assertEquals(c.toString(), c.group, r.group)
            assertEquals(c.toString(), c.best, r.best?.key)
            assertEquals(c.toString(), c.at, r.best?.at?.toString())
            assertEquals(c.toString(), c.routes, r.routes.joinToString(",") { it.key + ":" + (it.at?.toString() ?: "null") })
        }
        // JS Date.UTC taşması: 29 Şubat + 1 yıl = 1 Mart
        assertEquals(LocalDate.of(1993, 3, 1), Retire.addYMD(LocalDate.of(1992, 2, 29), 1))
    }

    @Test fun classroomNames() {
        assertEquals(listOf("Ayşe", "Mehmet", "Zeynep"), Classroom.names("Ayşe, Mehmet\nZeynep;Ayşe\n\n"))
    }
}
