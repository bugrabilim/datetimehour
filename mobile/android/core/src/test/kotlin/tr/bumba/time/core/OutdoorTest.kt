package tr.bumba.time.core

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

/** SaatCore OutdoorTests ile aynı örnekler */
class OutdoorTest {
    private fun h(feels: Double, rain: Int = 0, mm: Double = 0.0, wind: Double = 8.0, gust: Double = 15.0, code: Int = 1, day: Boolean = true, uv: Double = 3.0) =
        Outdoor.Hour(feels, feels, rain, mm, wind, gust, code, day, uv)

    @Test fun scores() {
        assertEquals(2, Outdoor.score(Outdoor.Activity.RUN, h(14.0)))
        assertEquals(1, Outdoor.score(Outdoor.Activity.RUN, h(24.0)))
        assertEquals(0, Outdoor.score(Outdoor.Activity.RUN, h(31.0)))
        assertEquals(0, Outdoor.score(Outdoor.Activity.WALK, h(18.0, rain = 70)))
        assertEquals(1, Outdoor.score(Outdoor.Activity.WALK, h(18.0, rain = 40)))
        assertEquals(0, Outdoor.score(Outdoor.Activity.BIKE, h(18.0, wind = 35.0)))
        assertEquals(1, Outdoor.score(Outdoor.Activity.BIKE, h(18.0, wind = 22.0)))
        assertEquals(0, Outdoor.score(Outdoor.Activity.BIKE, h(18.0, gust = 50.0)))
        assertEquals(0, Outdoor.score(Outdoor.Activity.GARDEN, h(18.0, day = false)))
        assertEquals(1, Outdoor.score(Outdoor.Activity.RUN, h(18.0, day = false)))
        assertEquals(0, Outdoor.score(Outdoor.Activity.WALK, h(18.0, code = 95)))
        assertEquals(1, Outdoor.score(Outdoor.Activity.GARDEN, h(18.0, uv = 9.0)))
        assertEquals(2, Outdoor.score(Outdoor.Activity.BIKE, h(18.0, uv = 9.0)))
    }

    @Test fun best() {
        val hours = listOf(h(30.0), h(24.0), h(15.0), h(16.0))
        assertEquals(2, Outdoor.best(Outdoor.Activity.RUN, hours))
        assertNull(Outdoor.best(Outdoor.Activity.RUN, listOf(h(35.0), h(14.0, rain = 80))))
    }

    @Test fun levels() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5), listOf(10.0, 25.0, 45.0, 70.0, 90.0, 140.0).map { Outdoor.aqiLevel(it) })
        assertEquals(listOf(0, 1, 2, 3), listOf(0.2, 5.0, 40.0, 300.0).map { Outdoor.pollenLevel(it) })
    }
}
