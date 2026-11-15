package tr.bumba.time

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Hata bildir (15 Kasım 2026): hesapsız gönderim, gövde alanları, kısa ileti ve hata eşlemesi (server/test/feedback.test.mjs) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeedbackTest {
    @After fun reset() = Account.resetForTest()

    @Test fun sendsWithoutAccount() = runBlocking {
        var sent: JSONObject? = null
        var auth: String? = "x"
        Account.transport = { _, p, b, t -> assertEquals("/v1/geri-bildirim", p); sent = b; auth = t; Account.Resp(200, """{"tamam":true}""", null) }
        assertNull(Feedback.send("Alarm sabah çalmadı.", " Kisi@Example.com ", "1.1 (31)"))
        assertNull(auth)
        assertEquals("android", sent!!.getString("kaynak"))
        assertEquals("kisi@example.com", sent!!.getString("eposta"))
        assertEquals("1.1 (31)", sent!!.getString("surum"))
        assertTrue(sent!!.getString("cihaz").contains("Android"))
    }

    @Test fun errors() = runBlocking {
        Account.transport = { _, _, _, _ -> error("istek gitmemeli") }
        assertEquals("account.fbShort", Feedback.send("kısa", "", "1"))
        assertEquals("account.err.email", Feedback.send("Yeterince uzun bir ileti", "yanlis", "1"))
        Account.transport = { _, _, _, _ -> Account.Resp(429, """{"hata":"sinir"}""", null) }
        assertEquals("account.err.tooMany", Feedback.send("Yeterince uzun bir ileti", "", "1"))
        Account.transport = { _, _, _, _ -> Account.Resp(0, "", null) }
        assertEquals("account.err.network", Feedback.send("Yeterince uzun bir ileti", "", "1"))
    }
}
