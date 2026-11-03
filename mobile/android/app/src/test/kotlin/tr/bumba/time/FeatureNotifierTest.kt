package tr.bumba.time

import org.robolectric.RuntimeEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Özellik bildirimi planı: varsayılan açık kategoriler (hava kapalı, ağ yok), sıralama ve kapatma */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeatureNotifierTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun defaultsAndToggles() = runBlocking {
        FeaturePrefs.set(NotifyFeature.WEATHER, false)
        val now = Instant.parse("2026-10-30T10:00:00Z")
        val all = FeatureNotifier.plan(context, now)
        assertTrue(all.all { it.at.isAfter(now) })
        // 14 gün boyunca gün doğumu ve batımı, en az bir dolunay ve yeni ay; saat başı varsayılan kapalı
        assertTrue(all.count { it.id.startsWith("sunrise-") } >= 13)
        assertTrue(all.count { it.id.startsWith("sunset-") } >= 13)
        assertEquals(0, all.count { it.id.startsWith("hourly-") })
        FeaturePrefs.set(NotifyFeature.HOURLY, true)
        assertTrue(FeatureNotifier.plan(context, now).count { it.id.startsWith("hourly-") } in 13..26)
        assertTrue(all.any { it.id.startsWith("moon-full-") } && all.any { it.id.startsWith("moon-new-") })
        // Sonraki mevsim başlangıcı: 21 Aralık 2026 kış gündönümü
        assertTrue(all.any { it.id.startsWith("season-") && it.at.toString().startsWith("2026-12-21") })
        all.filter { it.id.startsWith("cal-") }.forEach { assertTrue(it.title.isNotBlank()) }

        NotifyFeature.entries.forEach { FeaturePrefs.set(it, false) }
        assertEquals(emptyList<FeatureNotifier.Planned>(), FeatureNotifier.plan(context, now))
        NotifyFeature.entries.forEach { Prefs.store.edit().remove(it.key).commit() }
    }

    /** Hava uyarıları (2. analiz #110): yarın 5° ve fazla soğuk/sıcak akşam 19.00'da, yağmurdan 15 dk önce */
    @Test fun weatherAlerts() {
        val zone = java.time.ZoneId.of("Europe/Istanbul")
        val now = java.time.LocalDateTime.of(2026, 11, 3, 10, 0).atZone(zone).toInstant()
        val day0 = java.time.LocalDate.of(2026, 11, 3)
        val w = WeatherData(
            zone,
            WeatherData.Current(15.0, 14.0, 60.0, 10.0, 0.0, 1015.0, 2.0, 2, true, 0.0),
            (0 until 24).map { WeatherData.Hour(now.plusSeconds(it * 3600L), 15.0, if (it == 5) 61 else 2, if (it == 5) 70 else 10, true) },
            listOf(18.0, 11.0, 12.0).mapIndexed { i, mx -> WeatherData.Day(day0.plusDays(i.toLong()), 2, mx, 8.0, 10, null, null, 2.0) },
        )
        Prefs.saveUnits("metric")
        val place = AppPlace.current()
        val out = FeatureNotifier.weatherAlerts(w, place, now)
        val colder = out.first { it.id == "weather-alert-2026-11-04" }
        assertEquals(java.time.LocalDateTime.of(2026, 11, 3, 19, 0).atZone(place.zone).toInstant(), colder.at)
        assertTrue(colder.body.startsWith(T("weather.alert.colder", 7)))
        val rain = out.first { it.id.startsWith("weather-rain-") }
        assertEquals(now.plusSeconds(5 * 3600L - 900), rain.at)
        // 11 → 12: fark 5'ten az, uyarı yok
        assertTrue(out.none { it.id == "weather-alert-2026-11-05" })
    }
}
