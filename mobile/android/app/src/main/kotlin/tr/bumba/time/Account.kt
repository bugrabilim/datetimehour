package tr.bumba.time

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Uygulamalardaki hesap (Bumba Genel Standartlar 5.15, madde 7 ve 13; 12 Kasım 2026): e-posta + şifre, üyelik sunucusu
 * hesap.time.bumba.tr (server/, Better Auth). Hesap isteğe bağlıdır; hiçbir özellik hesap istemez. Kayıt ekranı, onay
 * cümlesi, şifre kuralı ve hesap sayfası (10 bölüm) web'dekiyle aynıdır; metinler server/i18n/account.json (`account.*`).
 * Oturum anahtarı Android Keystore'daki anahtarla AES-GCM şifreli "account" dosyasında durur; bu dosya telefonun yedeğine
 * girmez (data_extraction_rules.xml). İstekler `X-Saat-Istemci: android` taşır (sunucuda 90 günlük uygulama oturumu).
 * iOS'taki Account.swift ile aynı uçlar ve hata eşlemesi.
 */
object Account {
    const val BASE = "https://hesap.time.bumba.tr"

    data class User(val id: String, val email: String, val lang: String)
    data class Resp(val code: Int, val body: String, val token: String?)

    /** Sonuç: [err] boşsa başarılı; doluysa `account.err.*` metninin anahtarı (ya da "account.bumba": Bumba üyesi) */
    data class Result(val err: String? = null, val data: String? = null) { val ok get() = err == null }

    var user by mutableStateOf<User?>(null)
        private set
    private var token: String? = null
    private var sp: SharedPreferences? = null

    /** Ağ katmanı (testlerde sahte yanıt verilir): yöntem, yol, JSON gövde, taşıyıcı belirteç → yanıt */
    var transport: (String, String, JSONObject?, String?) -> Resp = ::http

    val signedIn get() = user != null && token != null

    fun init(c: Context) {
        sp = c.getSharedPreferences("account", Context.MODE_PRIVATE)
        val p = sp ?: return
        token = p.getString("t", null)?.let { Crypt.open(it) }
        user = if (token != null) p.getString("u", null)?.let { parseUser(JSONObject(it)) } else null
        if (token == null && p.contains("t")) p.edit().remove("t").remove("u").apply() // anahtar açılamadı (yeni telefon): çıkış sayılır
    }

    private fun save(newToken: String?, u: User?) {
        token = newToken
        user = if (newToken != null) u else null
        val e = sp?.edit() ?: return
        if (newToken == null || u == null) e.remove("t").remove("u")
        else {
            val sealed = Crypt.seal(newToken)
            if (sealed != null) e.putString("t", sealed) else e.remove("t") // şifrelenemezse yalnız bellekte (uygulama kapanınca çıkış)
            e.putString("u", JSONObject().put("id", u.id).put("email", u.email).put("lang", u.lang).toString())
        }
        e.apply()
    }

    /** Bu cihazda kayıt olunan adres: ilk girişte cihazdaki rozetler sormadan hesaba geçer (madde 13, "Üye") */
    internal var createdHere: String?
        get() = sp?.getString("new", null)
        set(v) { sp?.edit()?.apply { if (v == null) remove("new") else putString("new", v) }?.apply() }
    /** Var olan hesaba girişte rozet sorusu her hesap için bir kez */
    internal fun badgesAsked(id: String) = sp?.getBoolean("asked.$id", false) == true
    internal fun setBadgesAsked(id: String) { sp?.edit()?.putBoolean("asked.$id", true)?.apply() }

    private fun parseUser(o: JSONObject) = User(o.optString("id"), o.optString("email"), o.optString("lang", "en"))

    private fun codeOf(r: Resp) = runCatching { JSONObject(r.body).let { it.optString("code").ifEmpty { it.optString("hata") } } }.getOrDefault("")

    /** Better Auth ve sunucunun hata kodlarını uygulamanın metinlerine çevirir */
    internal fun errorOf(r: Resp, wrongPassword: Boolean = false): String {
        val code = codeOf(r)
        return when {
            r.code == 0 -> "account.err.network"
            r.code == 429 -> "account.err.tooMany"
            code == "EMAIL_NOT_VERIFIED" -> "account.err.unverified"
            code == "BANNED_USER" -> "account.err.suspended"
            code == "BUMBA_MEMBER" -> "account.bumba"
            code == "EMAIL_TAKEN" || code == "USER_ALREADY_EXISTS" -> "account.err.emailTaken"
            code == "WEAK_PASSWORD" || code.startsWith("PASSWORD_TOO") -> "account.err.password"
            code == "INVALID_PASSWORD" || code == "sifre" || wrongPassword && (r.code == 400 || r.code == 401) -> "account.err.wrongPassword"
            code == "INVALID_EMAIL_OR_PASSWORD" || code == "bilgiler" || r.code == 401 -> "account.err.credentials"
            code == "INVALID_EMAIL" || code == "eposta" -> "account.err.email"
            code.startsWith("gecersiz:") -> "account.err.field:" + code.removePrefix("gecersiz:")
            else -> "account.err.generic"
        }
    }

    internal suspend fun call(method: String, path: String, body: JSONObject? = null, auth: Boolean = false): Resp =
        withContext(Dispatchers.IO) { transport(method, path, body, if (auth) token else null) }

    fun validEmail(e: String) = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(e.trim())

    /** Şifre kuralı (standart madde 7; sunucudaki passwordChecks ile aynı): ekranda dört satır */
    fun passwordRules(p: String): Map<String, Boolean> = linkedMapOf(
        "pwLen" to (p.length in 8..128 && p.none { it.isWhitespace() }),
        "pwDigit" to p.any { it.isDigit() },
        "pwCase" to (p.any { it.isUpperCase() } && p.any { it.isLowerCase() }),
        "pwSymbol" to p.any { !it.isLetterOrDigit() && !it.isWhitespace() },
    )
    fun validPassword(p: String) = passwordRules(p).values.all { it }

    /** Kayıt ekranı: yazılan e-posta başka bir Bumba ürününde kayıtlı mı ("var" ise soru gösterilir) */
    suspend fun lookup(email: String): Result {
        if (!validEmail(email)) return Result("account.err.email")
        val r = call("POST", "/v1/uyelik/sorgula", JSONObject().put("eposta", email.trim().lowercase()))
        return if (r.code == 200) Result(data = JSONObject(r.body).optString("durum", "yok")) else Result(errorOf(r))
    }

    /** Kayıt: yalnız e-posta ve şifre (onay kaydını sunucu tutar); dil ve saat dilimi e-postalar için */
    suspend fun signUp(email: String, password: String): Result {
        if (!validEmail(email)) return Result("account.err.email")
        if (!validPassword(password)) return Result("account.err.password")
        val r = call("POST", "/api/auth/sign-up/email", JSONObject().put("email", email.trim().lowercase()).put("password", password)
            .put("name", "").put("lang", Strings.lang).put("timeZone", java.util.TimeZone.getDefault().id))
        if (r.code == 200) createdHere = email.trim().lowercase()
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    private fun keep(r: Resp): Result {
        val o = runCatching { JSONObject(r.body) }.getOrNull() ?: return Result("account.err.generic")
        val t = r.token ?: o.optString("token").takeIf { it.isNotEmpty() } ?: return Result("account.err.generic")
        save(t, parseUser(o.getJSONObject("user")))
        return Result()
    }

    suspend fun signIn(email: String, password: String): Result {
        if (!validEmail(email)) return Result("account.err.email")
        val r = call("POST", "/api/auth/sign-in/email", JSONObject().put("email", email.trim()).put("password", password))
        if (r.code != 200) return Result(errorOf(r))
        return keep(r).also { if (it.ok) afterSignIn() }
    }

    /** "Evet": başka bir Bumba ürünündeki üyelikle giriş; hesap burada açılır, numara aynı kalır */
    suspend fun link(email: String, password: String): Result {
        val r = call("POST", "/v1/uyelik/bagla", JSONObject().put("eposta", email.trim().lowercase()).put("sifre", password).put("dil", Strings.lang))
        if (r.code != 200) return Result(errorOf(r))
        return keep(r).also { if (it.ok) afterSignIn() }
    }

    private suspend fun afterSignIn() {
        syncPrefs()
        AccountSync.sync(first = true)
    }

    /** Girişten sonra: aynı e-postayla başka bir Bumba ürününde ayrı hesap var mı */
    suspend fun separateAccount(): Boolean {
        val r = call("GET", "/v1/uyelik/durum", auth = true)
        return r.code == 200 && runCatching { JSONObject(r.body).optBoolean("ayriHesap") }.getOrDefault(false)
    }

    suspend fun merge(password: String): Result {
        val r = call("POST", "/v1/uyelik/birlestir", JSONObject().put("sifre", password), auth = true)
        return if (r.code == 200) Result() else Result(errorOf(r, wrongPassword = true))
    }

    /** Sunucudaki güncel bilgiler; oturum geçersizse (silinmiş, şifre başka yerde değişmiş) çıkış */
    suspend fun refresh(): Result {
        if (token == null) return Result()
        val r = call("GET", "/api/auth/get-session", auth = true)
        if (r.code == 0) return Result("account.err.network")
        val o = runCatching { JSONObject(r.body) }.getOrNull()
        if (r.code == 401 || (r.code == 200 && (o == null || !o.has("user")))) { save(null, null); return Result() }
        if (r.code != 200 || o == null) return Result(errorOf(r))
        save(token, parseUser(o.getJSONObject("user")))
        syncPrefs()
        return Result()
    }

    /** Hesaba gelen e-postalar uygulamanın dilinde gitsin; saat dilimi telefonunki (5. bölüm) */
    suspend fun syncPrefs() {
        val u = user ?: return
        val tz = java.util.TimeZone.getDefault().id
        if (u.lang == Strings.lang && sp?.getString("tz", null) == tz) return
        val r = call("POST", "/api/auth/update-user", JSONObject().put("lang", Strings.lang).put("timeZone", tz), auth = true)
        if (r.code == 200) { save(token, u.copy(lang = Strings.lang)); sp?.edit()?.putString("tz", tz)?.apply() }
    }

    /** Hesap sayfasının verisi: e-posta, numara, üyelik tarihi, kutlama, profil (JSON) */
    suspend fun load(): Result {
        val r = call("GET", "/v1/hesap", auth = true)
        if (r.code == 401) { save(null, null); return Result("account.err.generic") }
        return if (r.code == 200) Result(data = r.body) else Result(errorOf(r))
    }

    suspend fun saveProfile(profile: JSONObject): Result {
        val r = call("POST", "/v1/profil", JSONObject().put("profil", profile), auth = true)
        return if (r.code == 200) Result(data = r.body) else Result(errorOf(r))
    }

    /** Fotoğraf: `data:image/webp;base64,…` (uygulamada 512 piksele küçültülür) ya da null (kaldır) */
    suspend fun savePhoto(dataUrl: String?): Result {
        val r = call("POST", "/v1/profil/foto", JSONObject().put("foto", dataUrl ?: JSONObject.NULL), auth = true)
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    suspend fun setCelebrate(on: Boolean): Result {
        val r = call("POST", "/v1/kutlama", JSONObject().put("acik", on), auth = true)
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    suspend fun changeEmail(newEmail: String): Result {
        if (!validEmail(newEmail)) return Result("account.err.email")
        val r = call("POST", "/api/auth/change-email", JSONObject().put("newEmail", newEmail.trim().lowercase()), auth = true)
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    /** Öbür oturumlar kapanır; sunucu bu cihaz için yeni belirteç verir */
    suspend fun changePassword(current: String, new: String): Result {
        if (!validPassword(new)) return Result("account.err.password")
        val r = call("POST", "/api/auth/change-password", JSONObject().put("currentPassword", current).put("newPassword", new)
            .put("revokeOtherSessions", true), auth = true)
        if (r.code != 200) return Result(errorOf(r, wrongPassword = true))
        val t = r.token ?: runCatching { JSONObject(r.body).optString("token") }.getOrNull()?.takeIf { it.isNotEmpty() }
        if (t != null) save(t, user)
        return Result()
    }

    /** Yanıt hesabın varlığını ele vermez (sunucu her durumda aynı döner) */
    suspend fun forgot(email: String): Result {
        if (!validEmail(email)) return Result("account.err.email")
        val r = call("POST", "/api/auth/request-password-reset", JSONObject().put("email", email.trim()))
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    suspend fun resendVerification(email: String): Result {
        if (!validEmail(email)) return Result("account.err.email")
        val r = call("POST", "/api/auth/send-verification-email", JSONObject().put("email", email.trim()))
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    /** Açık oturumlar (7. bölüm): JSON dizi; bu oturumun belirteci [token] ile karşılaştırılır */
    suspend fun sessions(): Result {
        val r = call("GET", "/api/auth/list-sessions", auth = true)
        return if (r.code == 200) Result(data = r.body) else Result(errorOf(r))
    }
    fun isCurrent(sessionToken: String) = token != null && (token == sessionToken || token!!.substringBefore('.') == sessionToken)
    suspend fun closeSession(sessionToken: String): Result {
        val r = call("POST", "/api/auth/revoke-session", JSONObject().put("token", sessionToken), auth = true)
        return if (r.code == 200) Result() else Result(errorOf(r))
    }
    suspend fun closeOtherSessions(): Result {
        val r = call("POST", "/api/auth/revoke-other-sessions", JSONObject(), auth = true)
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    /** Öbür Bumba ürünlerinde üyeliğim (8. bölüm): [{site, ad, adres, acik, kendisi}] */
    suspend fun products(): Result {
        val r = call("GET", "/v1/uyelik/urunler", auth = true)
        return if (r.code == 200) Result(data = JSONObject(r.body).optJSONArray("urunler")?.toString() ?: "[]") else Result(errorOf(r))
    }
    suspend fun openProducts(sites: List<String>, password: String): Result {
        val r = call("POST", "/v1/uyelik/ac", JSONObject().put("hedefler", org.json.JSONArray(sites)).put("sifre", password), auth = true)
        return if (r.code == 200) Result() else Result(errorOf(r, wrongPassword = true))
    }
    suspend fun closeProducts(sites: List<String>): Result {
        val r = call("POST", "/v1/uyelik/kapat", JSONObject().put("hedefler", org.json.JSONArray(sites)), auth = true)
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    /** Verilerimi indir (KVKK md. 11): sunucunun JSON dökümü */
    suspend fun download(): Result {
        val r = call("GET", "/v1/verilerim", auth = true)
        return if (r.code == 200) Result(data = r.body) else Result(errorOf(r))
    }

    suspend fun signOut() {
        if (token != null) call("POST", "/api/auth/sign-out", JSONObject(), auth = true)
        save(null, null)
        AccountSync.forget()
    }

    /** Hesabı sil: "urun" yalnız Saat Tarih'ten, "hepsi" bütün Bumba ürünlerinden. Şifre son 24 saatte girişte gerekmez. */
    suspend fun delete(scope: String, password: String): Result {
        val body = JSONObject().put("kapsam", scope)
        if (password.isNotEmpty()) body.put("sifre", password)
        val r = call("POST", "/v1/hesap/sil", body, auth = true)
        if (r.code != 200) return Result(errorOf(r, wrongPassword = true))
        save(null, null)
        AccountSync.forget()
        return Result()
    }

    /** Testler: bellek dışı kayıt yok sayılır */
    internal fun resetForTest() { token = null; user = null; sp = null; transport = ::http }

    private fun http(method: String, path: String, body: JSONObject?, bearer: String?): Resp = try {
        val c = URL(BASE + path).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.instanceFollowRedirects = false
        c.setRequestProperty("Accept", "application/json")
        c.setRequestProperty("User-Agent", "SaatTarih-Android")
        c.setRequestProperty("X-Saat-Istemci", "android")
        if (bearer != null) c.setRequestProperty("Authorization", "Bearer $bearer")
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toString().toByteArray()) }
        }
        val code = c.responseCode
        val text = (if (code < 400) c.inputStream else c.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
        val tok = c.getHeaderField("set-auth-token")
        c.disconnect()
        Resp(code, text, tok)
    } catch (e: IOException) {
        Resp(0, "", null)
    }

    /** Android Keystore'da üretilen, telefondan çıkmayan AES anahtarıyla şifreleme (yeni telefona geçmez) */
    private object Crypt {
        private const val ALIAS = "saat-account"
        private fun key(): SecretKey? = runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (ks.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
        }.getOrNull()

        fun seal(plain: String): String? = runCatching {
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key() ?: return null) }
            Base64.encodeToString(c.iv, Base64.NO_WRAP) + "." + Base64.encodeToString(c.doFinal(plain.toByteArray()), Base64.NO_WRAP)
        }.getOrNull()

        fun open(sealed: String): String? = runCatching {
            val (iv, data) = sealed.split(".").let { it[0] to it[1] }
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key() ?: return null, GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            }
            String(c.doFinal(Base64.decode(data, Base64.NO_WRAP)))
        }.getOrNull()
    }
}
