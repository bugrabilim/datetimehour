package tr.bumba.time

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Kapsayıcı takvimler (9 Kasım 2026): uydurma bir takvimle (test kaynağı calendars-fixture.json, sözleşme v1 biçimi)
 * bölgeye göre varsayılan, `cal.extra` kaydı, calEvents'e eklenen olaylar (tür, ad, renk, çok günlü), ikinci tarih
 * satırı (artık ay, günlük kod, yıl etiketi) ve gün ayrıntısı + Takvimler bölümünün ekran görüntüleri (tr, ar).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class ExtraCalendarsTest {
    private val dir = System.getProperty("screens.dir") ?: "build/screens"
    private val fixture: String = javaClass.getResourceAsStream("/calendars-fixture.json")!!.bufferedReader().use { it.readText() }

    private val tr = mapOf(
        "cals.fixture.name" to "Deneme takvimi",
        "cals.fixture.desc" to "Testler için uydurma bir takvim: özel günler ve ikinci tarih.",
        "cals.fixture.ev.spring" to "Bahar günü",
        "cals.fixture.ev.fest" to "Üç günlük şenlik",
        "cals.fixture.month.1" to "Birinci ay", "cals.fixture.month.2" to "İkinci ay",
        "cals.fixture.month.3" to "Üçüncü ay", "cals.fixture.month.4" to "Dördüncü ay",
        "cals.fixture.leapFmt" to "%1\$s (artık)",
        "cals.fixture.fmt" to "%1\$s %2\$s %3\$s",
        "cals.fixture.yearFmt" to "Dönem %1\$s",
        "cals.fixture.daily.t5" to "5. gün", "cals.fixture.daily.n12" to "12. konak", "cals.fixture.daily.r3" to "Taian",
        "cals.fixture.note" to "Uydurma veri; yalnız testlerde kullanılır.",
    )
    private val ar = mapOf(
        "cals.fixture.name" to "تقويم تجريبي",
        "cals.fixture.desc" to "تقويم وهمي للاختبارات: مناسبات وتاريخ ثانٍ.",
        "cals.fixture.ev.spring" to "يوم الربيع",
        "cals.fixture.ev.fest" to "مهرجان ثلاثة أيام",
        "cals.fixture.month.1" to "الشهر الأول", "cals.fixture.month.2" to "الشهر الثاني",
        "cals.fixture.month.3" to "الشهر الثالث", "cals.fixture.month.4" to "الشهر الرابع",
        "cals.fixture.leapFmt" to "%1\$s (كبيس)",
        "cals.fixture.fmt" to "%1\$s %2\$s %3\$s",
        "cals.fixture.yearFmt" to "الحقبة %1\$s",
        "cals.fixture.daily.t5" to "اليوم 5", "cals.fixture.daily.n12" to "المنزلة 12", "cals.fixture.daily.r3" to "تايآن",
        "cals.fixture.note" to "بيانات وهمية للاختبارات فقط.",
    )

    private fun load(json: String = fixture, region: String = "TR", texts: Map<String, String> = tr) {
        Prefs.store.edit().remove(ExtraCals.KEY).commit()
        ExtraCals.reset()
        ExtraCals.regionOverride = region
        ExtraCals.textOverride = texts
        ExtraCals.loadJson(json)
    }

    @After fun tearDown() {
        Prefs.store.edit().remove(ExtraCals.KEY).commit()
        ExtraCals.reset()
    }

    @Test fun defaultsByRegion() {
        load(region = "DE")
        assertEquals(setOf("fixture"), ExtraCals.enabled)
        load(region = "TR")
        assertTrue(ExtraCals.enabled.isEmpty())
        load(region = "")
        assertTrue(ExtraCals.enabled.isEmpty())
        // Dinî takvim hiçbir bölgede kendiliğinden açılmaz
        load(fixture.replace("\"cultural\"", "\"religious\""), region = "DE")
        assertTrue(ExtraCals.enabled.isEmpty())
        // Varsayılan kayıt yazmaz
        assertFalse(Prefs.store.contains(ExtraCals.KEY))
    }

    @Test fun toggleWritesSelection() {
        load(region = "TR")
        ExtraCals.set("fixture", true)
        assertEquals("fixture", Prefs.store.getString(ExtraCals.KEY, null))
        assertTrue(ExtraCals.isOn("fixture"))
        ExtraCals.set("fixture", false)
        assertEquals("", Prefs.store.getString(ExtraCals.KEY, null))
        // Seçim kalıcıdır: bölgesinde de kapalı kalır
        ExtraCals.regionOverride = "DE"
        assertFalse(ExtraCals.isOn("fixture"))
        // Bilinmeyen (ileride gelecek) takvim kimliği korunur, açık sayılmaz
        Prefs.store.edit().putString(ExtraCals.KEY, "future,fixture").commit()
        ExtraCals.loadJson(fixture)
        assertEquals(setOf("fixture"), ExtraCals.enabled)
        ExtraCals.set("fixture", false)
        assertEquals("future", Prefs.store.getString(ExtraCals.KEY, null))
    }

    @Test fun eventsJoinCalEvents() {
        load(region = "DE")
        val spring = calEvents(LocalDate.parse("2027-03-28")).single { it.type.startsWith("cal:") }
        assertEquals("cal:fixture", spring.type)
        assertEquals("Bahar günü", spring.name)
        assertEquals(Color(0xFF7B5EA7), AppData.color(spring.type))
        assertEquals("Deneme takvimi", calendarTypeLabel(spring.type))
        // Çok günlü olay her gününde, başlangıç tarihiyle
        for (day in listOf("2027-04-10", "2027-04-11", "2027-04-12")) {
            val e = calEvents(LocalDate.parse(day)).single { it.type == "cal:fixture" }
            assertEquals("Üç günlük şenlik", e.name)
            assertEquals(LocalDate.parse("2027-04-10"), e.date)
            assertEquals(LocalDate.parse("2027-04-12"), e.end)
        }
        assertTrue(calEvents(LocalDate.parse("2027-04-13")).none { it.type == "cal:fixture" })
        // Yaklaşan günlerde bir kez
        assertEquals(1, AppData.upcoming(LocalDate.parse("2027-04-09"), 20).count { it.name == "Üç günlük şenlik" })
        // Kapalıyken görünmez
        ExtraCals.set("fixture", false)
        assertTrue(calEvents(LocalDate.parse("2027-03-28")).none { it.type == "cal:fixture" })
    }

    @Test fun secondDateLine() {
        load(region = "DE")
        val c = ExtraCals.find("fixture")!!
        assertEquals(ExtraCals.SecondDate(19, 1, 5787, false), ExtraCals.secondDate(c, LocalDate.parse("2027-03-28")))
        assertEquals("19 Birinci ay 5787 · 5. gün · 12. konak · Dönem 9", ExtraCals.dayLine(c, LocalDate.parse("2027-03-28")))
        // Artık (tekrar) ay
        assertEquals("2 İkinci ay (artık) 5787 · Taian · Dönem 9", ExtraCals.dayLine(c, LocalDate.parse("2027-04-10")))
        // Tablonun dışı: ilk satırdan önce ve son satır (yalnız bitiş sınırı)
        assertNull(ExtraCals.secondDate(c, LocalDate.parse("2027-03-09")))
        assertNull(ExtraCals.secondDate(c, LocalDate.parse("2027-06-07")))
        assertEquals(ExtraCals.SecondDate(30, 3, 5787, false), ExtraCals.secondDate(c, LocalDate.parse("2027-06-06")))
        // Ay adları 0'dan yazılmışsa (month.0 varsa) bir eksiği okunur; biçim yoksa "{d} {m} {y}"
        ExtraCals.textOverride = mapOf("cals.fixture.month.0" to "Sıfırıncı", "cals.fixture.month.1" to "Birinci")
        assertEquals("19 Sıfırıncı 5787", ExtraCals.dayLine(c, LocalDate.parse("2027-03-28")))
        assertEquals(listOf("fixture"), ExtraCals.dayLines(LocalDate.parse("2027-03-28")).map { it.first.id })
    }

    @Test fun locationHook() {
        // Konuma bağlı ekleme yalnız metni varsa: Yahudi takviminde cuma mum saati (gün batımı − 18 dk)
        Prefs.saveH12(false) // Robolectric'in yerel ayarı en-US: kayıt yoksa 12 saat olurdu
        load(fixture.replace("\"fixture\"", "\"jewish\""), region = "TR",
            texts = mapOf("cals.jewish.candles" to "Mum %1\$s", "cals.jewish.havdalah" to "Havdala %1\$s"))
        ExtraCals.set("jewish", true)
        val c = ExtraCals.find("jewish")!!
        val ist = AppPlace.of(Province.find("istanbul"))
        val fri = ExtraCals.dayLine(c, LocalDate.parse("2027-03-26"), ist)!!
        val t = Regex("Mum (\\d\\d):(\\d\\d)").find(fri)!!.groupValues
        val min = t[1].toInt() * 60 + t[2].toInt()
        assertTrue("$fri", min in 18 * 60 + 50..19 * 60 + 20) // İstanbul gün batımı ~19.22
        assertTrue(ExtraCals.dayLine(c, LocalDate.parse("2027-03-27"), ist)!!.contains("Havdala "))
        assertFalse(ExtraCals.dayLine(c, LocalDate.parse("2027-03-25"), ist)!!.contains("Mum"))
        ExtraCals.textOverride = emptyMap()
        assertFalse(ExtraCals.dayLine(c, LocalDate.parse("2027-03-26"), ist)!!.contains("Mum"))
    }

    @Test fun noDataNoFeature() {
        load("""{"v":1,"cals":{}}""", region = "DE")
        assertTrue(ExtraCals.all.isEmpty())
        assertTrue(calEvents(LocalDate.parse("2027-03-28")).none { it.type.startsWith("cal:") })
        assertTrue(ExtraCals.dayLines(LocalDate.parse("2027-03-28")).isEmpty())
    }

    private fun shot(name: String, lang: String, texts: Map<String, String>, day: String) {
        RuntimeEnvironment.setQualifiers("+$lang")
        Prefs.store.edit().putBoolean("notify.asked", true).putBoolean(DialFace.SEEN_KEY, true).commit()
        Prefs.saveAppearance("light")
        Prefs.saveAccent("amber")
        Onboarding.finish()
        Features.save(setOf(Tab.PRAYER))
        load(region = "DE", texts = texts)
        val d = LocalDate.parse(day)
        captureRoboImage("$dir/$name.png") {
            AppTheme {
                Column(Modifier.fillMaxSize().appBackdrop(Theme.c)) {
                    SubBar(L("calendars.title"), onBack = {})
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                        DayDetail(d, d, emptyList())
                        Spacer(Modifier.height(16.dp))
                        AppCard { ExtraCalsSection() }
                    }
                }
            }
        }
    }

    @Test fun screenshots() {
        shot("tr-takvimler", "tr", tr, "2027-04-10")
        shot("ar-takvimler", "ar", ar, "2027-04-10")
    }
}
