package tr.bumba.time

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Hesap (üyelik aşama 1): sahte sunucuyla giriş, belirteç, dil eşitleme, oturum düşmesi, şifre değişimi ve
 * hata eşlemesi. Yanıt biçimleri server/test/auth.test.mjs'teki Better Auth yanıtlarıyla aynı.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountTest {
    private val calls = mutableListOf<Triple<String, String, String?>>() // yöntem, yol, taşıyıcı belirteç

    private fun fake(handler: (String, String, JSONObject?, String?) -> Account.Resp) {
        Account.transport = { m, p, b, t -> calls += Triple(m, p, t); handler(m, p, b, t) }
    }

    private fun userJson(lang: String = "tr") =
        JSONObject().put("id", "u1").put("email", "ayse@example.com").put("name", "Ayşe").put("lang", lang)

    @Before fun setUp() {
        RuntimeEnvironment.setQualifiers("+tr")
        runBlocking { Account.signOut() }
        calls.clear()
    }

    @After fun tearDown() { Account.resetForTest() }

    @Test fun localValidationNeedsNoNetwork() = runBlocking {
        fake { _, _, _, _ -> error("ağ çağrılmamalı") }
        assertEquals("account.err.email", Account.signUp("ayse", "guclu-sifre-1", "").err)
        assertEquals("account.err.password", Account.signUp("ayse@example.com", "kisa", "").err)
        assertEquals("account.err.email", Account.forgot("  ").err)
        assertTrue(calls.isEmpty())
    }

    @Test fun signInKeepsTokenAndSyncsLanguage() = runBlocking {
        fake { m, p, b, t ->
            when (p) {
                "/api/auth/sign-in/email" -> Account.Resp(200, JSONObject().put("token", "body-token").put("user", userJson("en")).toString(), "header-token")
                "/api/auth/update-user" -> { assertEquals("tr", b!!.getString("lang")); assertEquals("header-token", t); Account.Resp(200, "{\"status\":true}", null) }
                else -> Account.Resp(404, "", null)
            }
        }
        val r = Account.signIn(" ayse@example.com ", "guclu-sifre-1")
        assertTrue(r.ok)
        assertTrue(Account.signedIn)
        assertEquals("ayse@example.com", Account.user!!.email)
        // Sunucudaki dil uygulamanın diline çekildi (e-postalar Türkçe gitsin)
        assertEquals("tr", Account.user!!.lang)
        assertEquals(listOf("/api/auth/sign-in/email", "/api/auth/update-user"), calls.map { it.second })
        Unit
    }

    @Test fun refreshSignsOutWhenSessionIsGone() = runBlocking {
        fake { _, p, _, _ -> if (p.endsWith("sign-in/email")) Account.Resp(200, JSONObject().put("token", "t1").put("user", userJson()).toString(), null) else Account.Resp(200, "null", null) }
        Account.signIn("ayse@example.com", "guclu-sifre-1")
        assertTrue(Account.signedIn)
        // Hesap başka cihazdan silindi: get-session boş döner
        assertTrue(Account.refresh().ok)
        assertFalse(Account.signedIn)
        assertNull(Account.user)
    }

    @Test fun refreshKeepsSessionOffline() = runBlocking {
        fake { _, p, _, _ -> if (p.endsWith("sign-in/email")) Account.Resp(200, JSONObject().put("token", "t1").put("user", userJson()).toString(), null) else Account.Resp(0, "", null) }
        Account.signIn("ayse@example.com", "guclu-sifre-1")
        assertEquals("account.err.network", Account.refresh().err)
        assertTrue(Account.signedIn) // bağlantı yokken çıkış yapılmaz
    }

    @Test fun changePasswordReplacesToken() = runBlocking {
        fake { _, p, _, t ->
            when (p) {
                "/api/auth/sign-in/email" -> Account.Resp(200, JSONObject().put("token", "eski").put("user", userJson()).toString(), null)
                "/api/auth/change-password" -> Account.Resp(200, JSONObject().put("token", "yeni").toString(), "yeni")
                "/v1/verilerim" -> { assertEquals("yeni", t); Account.Resp(200, "{\"hesap\":{}}", null) }
                else -> Account.Resp(404, "", null)
            }
        }
        Account.signIn("ayse@example.com", "guclu-sifre-1")
        assertTrue(Account.changePassword("guclu-sifre-1", "daha-guclu-2").ok)
        assertEquals("{\"hesap\":{}}", Account.download().data)
        assertEquals("account.err.password", Account.changePassword("guclu-sifre-1", "kisa").err)
    }

    @Test fun deleteWithWrongPasswordStaysSignedIn() = runBlocking {
        fake { _, p, _, _ ->
            when (p) {
                "/api/auth/sign-in/email" -> Account.Resp(200, JSONObject().put("token", "t1").put("user", userJson()).toString(), null)
                "/api/auth/delete-user" -> Account.Resp(400, "{\"code\":\"INVALID_PASSWORD\",\"message\":\"Invalid password\"}", null)
                else -> Account.Resp(404, "", null)
            }
        }
        Account.signIn("ayse@example.com", "guclu-sifre-1")
        assertEquals("account.err.wrongPassword", Account.delete("yanlis").err)
        assertTrue(Account.signedIn)
    }

    @Test fun errorMapping() {
        fun e(code: Int, body: String = "", wrong: Boolean = false) = Account.errorOf(Account.Resp(code, body, null), wrong)
        assertEquals("account.err.network", e(0))
        assertEquals("account.err.tooMany", e(429))
        assertEquals("account.err.unverified", e(403, "{\"code\":\"EMAIL_NOT_VERIFIED\"}"))
        assertEquals("account.err.credentials", e(401, "{\"code\":\"INVALID_EMAIL_OR_PASSWORD\"}"))
        assertEquals("account.err.wrongPassword", e(400, "{\"code\":\"INVALID_PASSWORD\"}", wrong = true))
        assertEquals("account.err.password", e(400, "{\"code\":\"PASSWORD_TOO_SHORT\"}"))
        assertEquals("account.err.generic", e(500, "<html>"))
        // Her anahtarın metni var (19 dil gen-strings ile)
        listOf("network", "tooMany", "unverified", "credentials", "wrongPassword", "email", "password", "generic")
            .forEach { assertTrue(it, L("account.err.$it").isNotEmpty() && L("account.err.$it") != "account.err.$it") }
    }
}
