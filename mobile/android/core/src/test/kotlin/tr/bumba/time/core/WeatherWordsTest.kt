package tr.bumba.time.core

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

/** Hava açıklamaları ve özet: sitedeki npm test ve Swift WeatherWordsTests ile aynı girdiler */
class WeatherWordsTest {
    @Test fun words() {
        assertEquals(listOf("calm", "light", "breezy", "strong", "gale"), listOf(3.0, 12.0, 25.0, 45.0, 80.0).map { WeatherWords.wind(it).substringAfterLast('.') })
        assertEquals(listOf("low", "moderate", "high", "veryHigh", "extreme"), listOf(1.0, 4.0, 7.0, 9.5, 12.0).map { WeatherWords.uv(it).substringAfterLast('.') })
        assertEquals(listOf("dry", "comfy", "humid", "veryHumid"), listOf(20.0, 45.0, 70.0, 90.0).map { WeatherWords.humidity(it).substringAfterLast('.') })
        assertEquals(listOf("fog", "haze", "good", "clear"), listOf(500.0, 2500.0, 8000.0, 24000.0).map { WeatherWords.visibility(it).substringAfterLast('.') })
        assertEquals(listOf("dry", "comfy", "muggy", "oppressive"), listOf(5.0, 12.0, 18.0, 22.0).map { WeatherWords.dew(it).substringAfterLast('.') })
        assertEquals("weather.fw.colder", WeatherWords.feels(10.0, 6.0))
        assertEquals("weather.fw.warmer", WeatherWords.feels(30.0, 34.0))
        assertNull(WeatherWords.feels(20.0, 21.0))
        assertEquals(listOf(0, 1, 2, 4, 7, 0), listOf(10.0, 50.0, 80.0, 190.0, 300.0, 350.0).map { WeatherWords.compass8(it) })
    }

    @Test fun summary() {
        val calm = (8..19).map { WeatherWords.Hour(it, 1, 10, 10.0) }
        assertEquals(WeatherWords.Summary("weather.sum.default"), WeatherWords.summary(calm, 20.0, 22.0))
        assertEquals(WeatherWords.Summary("weather.sum.colder", n = 7), WeatherWords.summary(calm, 20.0, 13.0))
        assertEquals(WeatherWords.Summary("weather.sum.warmer", n = 6), WeatherWords.summary(calm, 20.0, 25.6))
        val windy = calm.map { if (it.hour == 15) it.copy(wind = 35.0) else it }
        assertEquals(WeatherWords.Summary("weather.sum.windyAfternoon"), WeatherWords.summary(windy, 20.0, 13.0))
        val rain = windy.map { if (it.hour == 11) it.copy(code = 61, rain = 70) else it }
        assertEquals(WeatherWords.Summary("weather.sum.rain", 11), WeatherWords.summary(rain, 20.0, 13.0))
        val snow = rain.map { if (it.hour == 11) it.copy(code = 73) else it }
        assertEquals(WeatherWords.Summary("weather.sum.snow", 11), WeatherWords.summary(snow, 20.0, 13.0))
        val storm = snow.map { if (it.hour == 17) it.copy(code = 95) else it }
        assertEquals(WeatherWords.Summary("weather.sum.storm", 17), WeatherWords.summary(storm, 20.0, 13.0))
        // 12 saatten sonrası bakılmaz
        val late = (8..23).map { WeatherWords.Hour(it, if (it == 21) 95 else 1, 0, 0.0) }
        assertEquals(WeatherWords.Summary("weather.sum.default"), WeatherWords.summary(late, 20.0, null))
        assertEquals(WeatherWords.Summary("weather.alert.colder", n = 8), WeatherWords.tomorrowAlert(18.0, 10.0))
        assertNull(WeatherWords.tomorrowAlert(18.0, 15.0))
    }
}
