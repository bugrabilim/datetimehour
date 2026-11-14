package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/** Endonezya önerileri (10 Kasım 2026): sünnet oruçlar, Rashdul Qiblat ve isbat bildirimi (Diyanet ve Kemenag takvimi) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SunnahFastsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val all = SunnahFasts.KINDS.toSet()
    private fun r(d: String) = SunnahFasts.reasons(LocalDate.parse(d), all)

    @Test fun reasons() {
        AppData.init(context)
        Prefs.store.edit().remove(Hijri.CAL).remove(Hijri.OFFSET).putString("province", "ankara").commit()
        assertEquals(listOf("fast.name.thursday"), r("2026-11-12"))
        assertEquals(listOf("fast.name.bidh"), r("2026-11-22")) // 13 Cemaziyelahir 1448, pazar
        assertEquals(listOf("fast.name.arafah"), r("2027-05-15"))
        assertEquals(emptyList<String>(), r("2027-05-16")) // Kurban Bayramı
        assertEquals(emptyList<String>(), r("2027-05-17")) // teşrik, pazartesi de olsa
        assertEquals(emptyList<String>(), r("2027-05-19")) // 13 Zilhicce: teşrik, eyyâm-ı bîd sayılmaz
        assertEquals(listOf("fast.name.bidh", "fast.name.thursday"), r("2027-05-20"))
        assertEquals(emptyList<String>(), r("2027-02-08")) // Ramazan'ın ilk günü, pazartesi
        assertEquals(emptyList<String>(), r("2027-03-09")) // Ramazan Bayramı
        assertEquals(listOf("fast.name.shawwal"), r("2027-03-10"))
        assertEquals(listOf("fast.name.tasua", "fast.name.monday"), r("2027-06-14"))
        assertEquals(listOf("fast.name.ashura"), r("2027-06-15"))
        // Yalnız seçilenler; Caferî/Tahran yönteminde varsayılan seçimde Aşure yok
        assertEquals(emptyList<String>(), SunnahFasts.reasons(LocalDate.parse("2026-11-12"), setOf("bidh")))
        assertEquals(all, SunnahFasts.kinds())
        Prefs.store.edit().putString(PrayerMethod.METHOD, "jafari").commit()
        assertEquals(all - "ashura", SunnahFasts.kinds())
        SunnahFasts.setKinds(setOf("ashura", "monthu"))
        assertEquals(setOf("monthu", "ashura"), SunnahFasts.kinds())
        Prefs.store.edit().remove(PrayerMethod.METHOD).remove(SunnahFasts.KINDS_KEY).commit()
    }

    @Test fun plans() {
        AppData.init(context)
        Prefs.store.edit().remove(Hijri.CAL).remove(Hijri.OFFSET).putString("province", "ankara").commit()
        val place = AppPlace.current()
        // Rashdul Qiblat 2026: 28 Mayıs 09.18 UTC (Ankara 12.18), bildirim 30 dk önce; Temmuz'daki de planda
        val rq = FeatureNotifier.rashdul(place, Instant.parse("2026-05-01T00:00:00Z"))
        assertEquals(Instant.parse("2026-05-28T08:48:00Z"), rq.first().at)
        assertEquals(T("notify.n.rashdul", "12:18"), rq.first().body)
        assertTrue(rq.any { it.at == Instant.parse("2026-07-15T08:57:00Z") })
        // Sünnet oruç: önceki akşam 20.00, gövdede hicri tarih
        val f = FeatureNotifier.fasting(place, Instant.parse("2026-11-10T00:00:00Z"), 14)
        val thu = f.first { it.id == "fast-2026-11-12" }
        assertEquals(Instant.parse("2026-11-11T17:00:00Z"), thu.at)
        assertEquals(T("fast.n.title", L("fast.name.thursday")), thu.title)
        assertTrue(thu.body.startsWith(HijriDate(1448, 6, 3).text))
        // İsbat: yalnız Kemenag takviminde; 29 Şaban 1448 (Kemenag) akşamı, beklenen 1 Ramazan gövdede
        val now = Instant.parse("2027-02-01T00:00:00Z")
        assertEquals(emptyList<FeatureNotifier.Planned>(), FeatureNotifier.isbat(place, now, 14))
        Prefs.store.edit().putString(Hijri.CAL, "kemenag").commit()
        val isb = FeatureNotifier.isbat(place, now, 14)
        assertEquals(1, isb.size)
        val first = Hijri.month(1448, 9)!!.first
        assertEquals(29, Hijri.date(isb[0].at.atZone(place.zone).toLocalDate())!!.day)
        assertEquals(T("notify.n.isbatBody", L("hijri.month.9"), Fmt.longDate(first)), isb[0].body)
        Prefs.store.edit().remove(Hijri.CAL).commit()
    }
}
