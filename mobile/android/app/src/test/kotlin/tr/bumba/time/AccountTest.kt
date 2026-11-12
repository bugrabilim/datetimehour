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
 * Hesap (standart 5.15 madde 7 ve 13): sahte sunucuyla şifre kuralı, kayıt (yalnız e-posta ve şifre, Bumba sorusu),
 * giriş, dil ve saat dilimi eşitleme, oturum düşmesi, şifre değişimi, iki seçenekli silme, hata eşlemesi, kayıtların ve
 * rozetlerin hesapla eşitlenmesi. Yanıt biçimleri server/test klasöründeki testler'teki yanıtlarla aynı.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountTest {
    private val calls = mutableListOf<Triple<String, String, JSONObject?>>() // yöntem, yol, gövde

    private fun fake(handler: (String, String, JSONObject?, String?) -> Account.Resp) {
        Account.transport = { m, p, b, t -> calls += Triple(m, p, b); handler(m, p, b, t) }
    }

    private fun userJson(lang: String = "tr", id: String = "u1") = JSONObject().put("id", id).put("email", "ayse@example.com").put("name", "").put("lang", lang)
    private var uid = "u1"
    private fun signInResp() = Account.Resp(200, JSONObject().put("token", "body-token").put("user", userJson(id = uid)).toString(), "header-token")
    private val empty = Account.Resp(200, "{\"kayitlar\":{},\"rozetler\":[]}", null)

    @Before fun setUp() {
        RuntimeEnvironment.setQualifiers("+tr")
        val app = RuntimeEnvironment.getApplication()
        Prefs.init(app)
        Account.init(app)
        AccountSync.init(app)
        runBlocking { Account.signOut() }
        Account.createdHere = null
        BadgeStore.deleteAll()
        calls.clear()
    }

    @After fun tearDown() { Account.resetForTest(); BadgeStore.deleteAll() }

    @Test fun passwordRuleMatchesServer() {
        assertFalse(Account.validPassword("guclu-sifre-1")) // büyük harf yok
        assertFalse(Account.validPassword("Guclu sifre1!")) // boşluk
        assertFalse(Account.validPassword("Gs-1"))
        assertTrue(Account.validPassword("Guclu-sifre1"))
        assertEquals(listOf("pwLen", "pwDigit", "pwCase", "pwSymbol"), Account.passwordRules("").keys.toList())
        assertEquals(listOf(false, true, false, false), Account.passwordRules("1").values.toList())
    }

    @Test fun localValidationNeedsNoNetwork() = runBlocking {
        fake { _, _, _, _ -> error("ağ çağrılmamalı") }
        assertEquals("account.err.email", Account.signUp("ayse", "Guclu-sifre1").err)
        assertEquals("account.err.password", Account.signUp("ayse@example.com", "guclu-sifre-1").err)
        assertEquals("account.err.email", Account.forgot("  ").err)
        assertEquals("account.err.email", Account.lookup("x").err)
        assertTrue(calls.isEmpty())
    }

    @Test fun signUpSendsOnlyEmailPasswordAndRemembersDevice() = runBlocking {
        fake { _, p, _, _ ->
            when (p) {
                "/v1/uyelik/sorgula" -> Account.Resp(200, "{\"durum\":\"var\"}", null)
                "/api/auth/sign-up/email" -> Account.Resp(200, "{\"token\":null}", null)
                else -> Account.Resp(404, "", null)
            }
        }
        assertEquals("var", Account.lookup("Bumba@Example.com").data)
        assertEquals("bumba@example.com", calls.last().third!!.getString("eposta"))
        assertTrue(Account.signUp("Yeni@Example.com", "Guclu-sifre1").ok)
        val body = calls.last().third!!
        assertEquals(setOf("email", "password", "name", "lang", "timeZone"), body.keys().asSequence().toSet())
        assertEquals("", body.getString("name"))
        assertEquals("yeni@example.com", Account.createdHere)
        // Sunucu Bumba üyesi derse soru gösterilir
        fake { _, _, _, _ -> Account.Resp(400, "{\"code\":\"BUMBA_MEMBER\"}", null) }
        assertEquals("account.bumba", Account.signUp("bumba@example.com", "Guclu-sifre1").err)
    }

    @Test fun signInKeepsTokenAndSyncsLanguage() = runBlocking {
        RuntimeEnvironment.setQualifiers("+tr")
        fake { _, p, b, t ->
            when (p) {
                "/api/auth/sign-in/email" -> Account.Resp(200, JSONObject().put("token", "body-token").put("user", userJson("en")).toString(), "header-token")
                "/api/auth/update-user" -> { assertEquals("tr", b!!.getString("lang")); assertTrue(b.has("timeZone")); assertEquals("header-token", t); Account.Resp(200, "{\"status\":true}", null) }
                else -> empty
            }
        }
        assertTrue(Account.signIn(" ayse@example.com ", "Guclu-sifre1").ok)
        assertTrue(Account.signedIn)
        assertEquals("tr", Account.user!!.lang)
        assertEquals(listOf("/api/auth/sign-in/email", "/api/auth/update-user"), calls.map { it.second }.take(2))
        Unit
    }

    @Test fun linkWithBumbaMembership() = runBlocking {
        fake { _, p, b, _ -> if (p == "/v1/uyelik/bagla") { assertEquals("Eski-sifre1", b!!.getString("sifre")); signInResp() } else empty }
        assertTrue(Account.link("ayse@example.com", "Eski-sifre1").ok)
        assertTrue(Account.signedIn)
        fake { _, _, _, _ -> Account.Resp(401, "{\"hata\":\"bilgiler\"}", null) }
        Account.signOut()
        assertEquals("account.err.credentials", Account.link("ayse@example.com", "yanlis").err)
    }

    @Test fun refreshSignsOutWhenSessionIsGone() = runBlocking {
        fake { _, p, _, _ -> if (p.endsWith("sign-in/email")) signInResp() else if (p == "/api/auth/get-session") Account.Resp(200, "null", null) else empty }
        Account.signIn("ayse@example.com", "Guclu-sifre1")
        assertTrue(Account.signedIn)
        assertTrue(Account.refresh().ok)
        assertFalse(Account.signedIn)
        assertNull(Account.user)
    }

    @Test fun refreshKeepsSessionOffline() = runBlocking {
        fake { _, p, _, _ -> if (p.endsWith("sign-in/email")) signInResp() else Account.Resp(0, "", null) }
        Account.signIn("ayse@example.com", "Guclu-sifre1")
        assertEquals("account.err.network", Account.refresh().err)
        assertTrue(Account.signedIn) // bağlantı yokken çıkış yapılmaz
    }

    @Test fun changePasswordReplacesToken() = runBlocking {
        var lastToken: String? = null
        fake { _, p, _, t ->
            lastToken = t
            when (p) {
                "/api/auth/sign-in/email" -> signInResp()
                "/api/auth/change-password" -> Account.Resp(200, JSONObject().put("token", "yeni").toString(), "yeni")
                "/v1/verilerim" -> Account.Resp(200, "{\"hesap\":{}}", null)
                else -> empty
            }
        }
        Account.signIn("ayse@example.com", "Guclu-sifre1")
        assertTrue(Account.changePassword("Guclu-sifre1", "Daha-guclu2").ok)
        assertEquals("{\"hesap\":{}}", Account.download().data)
        assertEquals("yeni", lastToken)
        assertEquals("account.err.password", Account.changePassword("Guclu-sifre1", "kisa").err)
    }

    @Test fun deleteHasTwoScopes() = runBlocking {
        fake { _, p, _, _ ->
            when (p) {
                "/api/auth/sign-in/email" -> signInResp()
                "/v1/hesap/sil" -> if (calls.last().third!!.optString("sifre") == "yanlis") Account.Resp(401, "{\"hata\":\"sifre\"}", null) else Account.Resp(200, "{\"silindi\":true}", null)
                else -> empty
            }
        }
        Account.signIn("ayse@example.com", "Guclu-sifre1")
        assertEquals("account.err.wrongPassword", Account.delete("urun", "yanlis").err)
        assertTrue(Account.signedIn)
        assertTrue(Account.delete("hepsi", "").ok)
        assertEquals("hepsi", calls.last().third!!.getString("kapsam"))
        assertFalse(calls.last().third!!.has("sifre")) // son 24 saatte girişte şifre gerekmez
        assertFalse(Account.signedIn)
    }

    @Test fun errorMapping() {
        fun e(code: Int, body: String = "", wrong: Boolean = false) = Account.errorOf(Account.Resp(code, body, null), wrong)
        assertEquals("account.err.network", e(0))
        assertEquals("account.err.tooMany", e(429))
        assertEquals("account.err.unverified", e(403, "{\"code\":\"EMAIL_NOT_VERIFIED\"}"))
        assertEquals("account.err.suspended", e(403, "{\"code\":\"BANNED_USER\"}"))
        assertEquals("account.err.credentials", e(401, "{\"code\":\"INVALID_EMAIL_OR_PASSWORD\"}"))
        assertEquals("account.err.wrongPassword", e(400, "{\"code\":\"INVALID_PASSWORD\"}", wrong = true))
        assertEquals("account.err.password", e(400, "{\"code\":\"WEAK_PASSWORD\"}"))
        assertEquals("account.err.emailTaken", e(400, "{\"code\":\"EMAIL_TAKEN\"}"))
        assertEquals("account.err.field:telefon", e(400, "{\"hata\":\"gecersiz:telefon\"}"))
        assertEquals("account.err.generic", e(500, "<html>"))
        listOf("network", "tooMany", "unverified", "suspended", "credentials", "wrongPassword", "email", "password", "emailTaken", "generic")
            .forEach { assertTrue(it, L("account.err.$it").isNotEmpty() && L("account.err.$it") != "account.err.$it") }
        assertTrue(L("account.consent").contains("](https://bumbagroup.com/uyelik-aydinlatma)")) // Türkçe arayüzde Türkçe metin
    }

    @Test fun recordsSyncTypesAndLastWriter() = runBlocking {
        val st = Prefs.store
        st.edit().putInt("zikir.total", 33).putBoolean("h12", true).putString("world", "[\"london\"]").remove("pomo.goal").apply()
        // İlk eşitleme: sunucuda olan kazanır, olmayan gönderilir
        val pushed = mutableListOf<JSONObject>()
        fake { m, p, b, _ ->
            when {
                p.endsWith("sign-in/email") -> signInResp()
                p == "/v1/kayitlar" && m == "GET" -> Account.Resp(200, "{\"kayitlar\":{\"zikir.total\":{\"v\":\"99\",\"t\":5},\"pomo.goal\":{\"v\":\"6\",\"t\":5},\"fs.scale\":{\"v\":\"0.8\",\"t\":5}}}", null)
                p == "/v1/kayitlar" -> { pushed += b!!.getJSONObject("degisiklik"); Account.Resp(200, "{\"tamam\":true}", null) }
                else -> empty
            }
        }
        Account.signIn("ayse@example.com", "Guclu-sifre1")
        assertEquals(99, st.getInt("zikir.total", 0))
        assertEquals(6, st.getInt("pomo.goal", 0))
        assertEquals(0.8f, st.getFloat("fs.scale", 0f), 0.0001f)
        val first = pushed.single()
        assertEquals("true", first.getJSONObject("h12").getString("v"))
        assertEquals("\"[\\\"london\\\"]\"", first.getJSONObject("world").getString("v"))
        assertFalse(first.has("zikir.total"))
        // Sonraki eşitleme: yalnız değişen gönderilir
        pushed.clear()
        st.edit().putInt("zikir.total", 100).apply()
        assertTrue(AccountSync.sync())
        assertEquals(setOf("zikir.total"), pushed.single().keys().asSequence().toSet())
        assertEquals("100", pushed.single().getJSONObject("zikir.total").getString("v"))
        // Tür tutmayan bozuk kayıt yazılmaz (çökme yok)
        AccountSync.apply("h12", "\"evet\"")
        assertTrue(st.getBoolean("h12", false))
    }

    @Test fun badgesMoveToNewAccountAndAskOnExisting() = runBlocking {
        BadgeStore.hit(Badge.RENK)
        val added = mutableListOf<JSONObject>()
        fake { m, p, b, _ ->
            when {
                p.endsWith("sign-in/email") -> signInResp()
                p == "/v1/rozetler" && m == "POST" -> { added += b!!; Account.Resp(200, "{\"rozetler\":[{\"id\":\"renk\",\"gun\":\"2026-11-12\"}]}", null) }
                p == "/v1/rozetler" -> Account.Resp(200, "{\"rozetler\":[{\"id\":\"takvim\",\"gun\":\"2026-11-01\"}]}", null)
                else -> empty
            }
        }
        // Var olan hesap (başka cihazda açılmış): bir kez sorulur; "Hayır"dan sonra yeniden sorulmaz
        Account.signIn("ayse@example.com", "Guclu-sifre1")
        assertTrue(AccountSync.askBadges > 0)
        assertTrue(added.isEmpty())
        assertEquals("2026-11-01", BadgeStore.earned(Badge.TAKVIM)) // hesaptaki rozet cihaza gelir
        AccountSync.declineBadges()
        Account.signOut()
        Account.signIn("ayse@example.com", "Guclu-sifre1")
        assertEquals(0, AccountSync.askBadges)
        Account.signOut()
        // Bu cihazda açılan hesap: sormadan aktarılır
        uid = "u2"
        Account.createdHere = "ayse@example.com"
        Account.signIn("ayse@example.com", "Guclu-sifre1")
        assertEquals(1, added.size)
        assertNull(Account.createdHere)
        assertEquals(0, AccountSync.askBadges)
    }
}
