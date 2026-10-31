package tr.bumba.time.wear

import androidx.wear.compose.material.MaterialTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tr.bumba.time.core.Prayer
import java.time.Instant
import java.time.LocalDate

/** Wear OS saati: yer, sıradaki vakit, hicri tarih ve ekran görüntüleri (wear/build/screens) */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "tr-w227dp-h227dp-small-notlong-round-watch-xhdpi-keyshidden-nonav")
class WearTest {
    private val dir = System.getProperty("screens.dir") ?: "build/screens"
    private val at = Instant.parse("2026-10-30T09:14:00Z") // İstanbul 12.14

    @Test fun placeAndNext() {
        Wear.select("istanbul")
        val p = Wear.place()
        assertEquals("İstanbul", p.name)
        val next = p.next(at)!!
        assertEquals(Prayer.DHUHR, next.prayer)
        assertEquals("12:53", p.hm(next.at))
        // Konum: Kadıköy yakınları → İstanbul; uzak konum → "Konumum"
        Wear.select("auto")
        Wear.saveLocation(40.99, 29.03)
        assertEquals("İstanbul", Wear.place().name)
        Wear.saveLocation(52.52, 13.40)
        assertEquals(Wear.text("place.myLocation"), Wear.place().name)
    }

    @Test fun hijri() {
        assertEquals(Triple(1448, 5, 19), Wear.hijri(LocalDate.of(2026, 10, 30)))
        assertEquals("19 Cemaziyelevvel 1448", Wear.hijriText(LocalDate.of(2026, 10, 30)))
        assertNull(Wear.hijri(LocalDate.of(2025, 1, 1)))
        // Ramazan 1448'in ilk günü: akşam "İftar"
        Wear.select("istanbul")
        val p = Wear.place()
        val maghrib = p.entries(LocalDate.of(2027, 2, 8)).first { it.prayer == Prayer.MAGHRIB }
        assertEquals(Wear.text("live.iftar"), Wear.label(maghrib, p.zone))
    }

    @Test fun screens() {
        Wear.select("istanbul")
        captureRoboImage("$dir/wear-saat.png") { MaterialTheme { WearRoot(initialNow = at) } }
        RuntimeEnvironment.setQualifiers("+ar")
        captureRoboImage("$dir/wear-saat-ar.png") { MaterialTheme { WearRoot(initialNow = at) } }
    }
}
