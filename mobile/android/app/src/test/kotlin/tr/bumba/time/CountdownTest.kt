package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Geri sayımlar: yıllık tekrar, "Bugün!", eski tek hedefin taşınması, hatırlatmalar */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CountdownTest {
    private val zone = ZoneId.systemDefault()
    private fun ms(s: String) = LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()

    @Test fun yearlyAndToday() {
        val now = Instant.ofEpochMilli(ms("2026-10-30T12:00"))
        val bday = CountdownItem(title = "Doğum günü", emoji = "🎂", target = ms("2020-03-15T00:00"), yearly = true)
        assertEquals(ms("2027-03-15T00:00"), bday.effective(now).toEpochMilli())
        // Hedef günü boyunca bugünün hedefi kalır
        val todayItem = CountdownItem(title = "x", target = ms("2025-10-30T08:00"), yearly = true)
        assertEquals(ms("2026-10-30T08:00"), todayItem.effective(now).toEpochMilli())
        assertEquals(L("countdowns.today"), countdownText(todayItem.effective(now), now))
        // Tek seferlik geçmiş hedef ileri taşınmaz
        val once = CountdownItem(title = "y", target = ms("2026-10-01T00:00"))
        assertEquals(ms("2026-10-01T00:00"), once.effective(now).toEpochMilli())
        assertEquals(T("client.daysLeft", 136), countdownText(bday.effective(now), now))
    }

    @Test fun migrationAndReminders() {
        Prefs.store.edit().remove("countdowns").putLong("cd.target", ms("2026-11-05T10:00")).putString("cd.title", "Sınav").commit()
        val list = CountdownItem.load()
        assertEquals(1, list.size)
        assertEquals("Sınav", list[0].title)
        assertEquals(0L, Prefs.store.getLong("cd.target", 0L))
        val now = Instant.ofEpochMilli(ms("2026-10-30T12:00"))
        val r = CountdownItem.reminders(now)
        assertEquals(2, r.size)
        assertTrue(r.any { it.at.toEpochMilli() == ms("2026-11-04T09:00") })
        assertTrue(r.any { it.at.toEpochMilli() == ms("2026-11-05T10:00") })
        Prefs.store.edit().remove("countdowns").commit()
    }

    /** 2. analiz #73, #138: gösterim birimleri, tarih geçince ileri sayım, kaçıncı yıl, paylaşım bağlantısı ve geri ekleme */
    @Test fun unitsShareImport() {
        val now = Instant.ofEpochMilli(ms("2026-11-03T12:00"))
        val at = Instant.ofEpochMilli(ms("2026-11-20T18:30"))
        val base = CountdownItem(title = "Tatil", target = at.toEpochMilli())
        assertEquals(T("client.daysLeft", 17L), countdownDisplay(base.copy(unit = "days"), at, now))
        assertEquals(T("countdowns.weeksLeft", 2L, 3L), countdownDisplay(base.copy(unit = "weeks"), at, now))
        assertEquals(T("countdowns.hoursLeft", 414L), countdownDisplay(base.copy(unit = "hours"), at, now))
        // Geçmiş tek seferlik hedef ileri sayar
        val past = CountdownItem(title = "x", target = ms("2026-10-22T09:00"))
        assertEquals(T("countdowns.since", 12L), countdownDisplay(past, Instant.ofEpochMilli(past.target), now))
        // Yıllık: kaçıncı yıl
        val wed = CountdownItem(title = "Evlilik", target = ms("1990-05-01T00:00"), yearly = true)
        assertEquals(" · " + T("countdowns.ordinal", 37), wed.ordinalText(wed.effective(now)))
        // Paylaşım bağlantısı sitedeki ?t=&n= biçiminde ve uygulamaya geri eklenebilir
        val url = base.shareUrl(now)
        assertTrue(url, url.contains("t=2026-11-20T18:30") && url.contains("n=Tatil"))
        Prefs.store.edit().remove("countdowns").commit()
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("saattarih://countdown?t=2026-12-31T23:59&n=Y%C4%B1lba%C5%9F%C4%B1"))
        assertTrue(CountdownImport.handle(intent))
        assertTrue(CountdownImport.handle(intent))
        val list = CountdownItem.load()
        assertEquals(1, list.size)
        assertEquals("Yılbaşı", list[0].title)
        assertEquals(ms("2026-12-31T23:59"), list[0].target)
        // Başka adresler alınmaz
        assertTrue(!CountdownImport.handle(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com/?t=2026-12-31"))))
        Prefs.store.edit().remove("countdowns").commit()
    }
}
