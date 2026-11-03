package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Test

/** Hava neyse ekran öyle (2. analiz #193): tür ve yoğunluk kuralları (iOS ve sitedeki weather-fx.js ile aynı) */
class WeatherSceneTest {
    @Test fun windyAndIntensity() {
        // 30 km/sa ve üstü rüzgârda açık/bulutlu hava rüzgârlı olur; yağmur yağmur kalır
        assertEquals(SkyWeather.WINDY, WeatherScene.of(SkyWeather.CLEAR, wind = 30.0).kind)
        assertEquals(SkyWeather.CLOUDY, WeatherScene.of(SkyWeather.CLOUDY, wind = 29.0).kind)
        assertEquals(SkyWeather.RAIN, WeatherScene.of(SkyWeather.RAIN, wind = 60.0).kind)
        // Yağmur damlası yağışla artar (40–240), fırtına en az 140
        assertEquals(90, WeatherScene.of(SkyWeather.RAIN).drops)
        assertEquals(70, WeatherScene.of(SkyWeather.RAIN, precip = 1.0).drops)
        assertEquals(240, WeatherScene.of(SkyWeather.RAIN, precip = 50.0).drops)
        assertEquals(140, WeatherScene.of(SkyWeather.STORM).drops)
        // Eğim rüzgârla (en çok 0,9); bulut sayısı bulut oranıyla
        assertEquals(0.08, WeatherScene.of(SkyWeather.RAIN).slant, 1e-9)
        assertEquals(0.9, WeatherScene.of(SkyWeather.RAIN, wind = 200.0).slant, 1e-9)
        assertEquals(6, WeatherScene.of(SkyWeather.CLOUDY, cloud = 1.0).clouds)
        assertEquals(0, WeatherScene.of(SkyWeather.CLEAR).clouds)
        assertEquals(4, WeatherScene.of(SkyWeather.CLOUDY).clouds)
        // Kar ve yapraklar
        assertEquals(140, WeatherScene.of(SkyWeather.SNOW, precip = 2.0).flakes)
        assertEquals(12, WeatherScene.of(SkyWeather.CLEAR, wind = 42.0).leaves)
        assertEquals(10, WeatherScene.of(SkyWeather.RAIN, wind = 40.0).leaves)
        assertEquals(0, WeatherScene.of(SkyWeather.RAIN, wind = 10.0).leaves)
    }
}
