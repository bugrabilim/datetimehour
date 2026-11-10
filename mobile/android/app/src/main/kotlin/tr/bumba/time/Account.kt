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
 * Uygulamalardaki hesap (üyelik aşama 1, 10 Kasım 2026; docs/uyelik-plani-2026-11.md): e-posta + şifre, hesap sunucusu
 * hesap.time.bumba.tr (server/, Better Auth). Hesap isteğe bağlıdır; hiçbir özellik hesap istemez (grup özellikleri
 * gelene kadar). Oturum anahtarı Android Keystore'daki anahtarla AES-GCM şifreli "account" dosyasında durur; bu dosya
 * telefonun yedeğine girmez (data_extraction_rules.xml). iOS'taki Account.swift ile aynı uçlar ve hata eşlemesi.
 */
object Account {
    const val BASE = "https://hesap.time.bumba.tr"

    data class User(val id: String, val email: String, val name: String, val lang: String)
    data class Resp(val code: Int, val body: String, val token: String?)

    /** Sonuç: [err] boşsa başarılı; doluysa `account.err.*` metninin anahtarı */
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
        if (token == null && p.contains("t")) p.edit().clear().apply() // anahtar açılamadı (yeni telefon): çıkış sayılır
    }

    private fun save(newToken: String?, u: User?) {
        token = newToken
        user = if (newToken != null) u else null
        val e = sp?.edit() ?: return
        if (newToken == null || u == null) e.clear()
        else {
            val sealed = Crypt.seal(newToken)
            if (sealed != null) e.putString("t", sealed) else e.remove("t") // şifrelenemezse yalnız bellekte (uygulama kapanınca çıkış)
            e.putString("u", JSONObject().put("id", u.id).put("email", u.email).put("name", u.name).put("lang", u.lang).toString())
        }
        e.apply()
    }

    private fun parseUser(o: JSONObject) = User(o.optString("id"), o.optString("email"), o.optString("name"), o.optString("lang", "en"))

    /** Better Auth hata kodlarını ve durumlarını uygulamanın metinlerine çevirir */
    internal fun errorOf(r: Resp, wrongPassword: Boolean = false): String {
        val code = runCatching { JSONObject(r.body).optString("code") }.getOrDefault("")
        return when {
            r.code == 0 -> "account.err.network"
            r.code == 429 -> "account.err.tooMany"
            code == "EMAIL_NOT_VERIFIED" -> "account.err.unverified"
            code == "INVALID_EMAIL_OR_PASSWORD" || (r.code == 401 && !wrongPassword) -> "account.err.credentials"
            code == "INVALID_PASSWORD" || wrongPassword && (r.code == 400 || r.code == 401) -> "account.err.wrongPassword"
            code == "INVALID_EMAIL" -> "account.err.email"
            code.startsWith("PASSWORD_TOO") -> "account.err.password"
            else -> "account.err.generic"
        }
    }

    private suspend fun call(method: String, path: String, body: JSONObject? = null, auth: Boolean = false): Resp =
        withContext(Dispatchers.IO) { transport(method, path, body, if (auth) token else null) }

    fun validEmail(e: String) = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(e.trim())
    fun validPassword(p: String) = p.length in 8..128

    suspend fun signUp(email: String, password: String, name: String): Result {
        if (!validEmail(email)) return Result("account.err.email")
        if (!validPassword(password)) return Result("account.err.password")
        val r = call("POST", "/api/auth/sign-up/email", JSONObject().put("email", email.trim()).put("password", password)
            .put("name", name.trim()).put("lang", Strings.lang))
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    suspend fun signIn(email: String, password: String): Result {
        if (!validEmail(email)) return Result("account.err.email")
        val r = call("POST", "/api/auth/sign-in/email", JSONObject().put("email", email.trim()).put("password", password))
        if (r.code != 200) return Result(errorOf(r))
        val o = JSONObject(r.body)
        val t = r.token ?: o.optString("token").takeIf { it.isNotEmpty() } ?: return Result("account.err.generic")
        save(t, parseUser(o.getJSONObject("user")))
        syncLang()
        return Result()
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
        syncLang()
        return Result()
    }

    /** Hesaba gelen e-postalar uygulamanın dilinde gitsin: dil değişince sunucuya yazılır */
    suspend fun syncLang() {
        val u = user ?: return
        if (u.lang == Strings.lang) return
        val r = call("POST", "/api/auth/update-user", JSONObject().put("lang", Strings.lang), auth = true)
        if (r.code == 200) save(token, u.copy(lang = Strings.lang))
    }

    suspend fun updateName(name: String): Result {
        val u = user ?: return Result("account.err.generic")
        val r = call("POST", "/api/auth/update-user", JSONObject().put("name", name.trim()), auth = true)
        if (r.code != 200) return Result(errorOf(r))
        save(token, u.copy(name = name.trim()))
        return Result()
    }

    suspend fun changeEmail(newEmail: String): Result {
        if (!validEmail(newEmail)) return Result("account.err.email")
        val r = call("POST", "/api/auth/change-email", JSONObject().put("newEmail", newEmail.trim()), auth = true)
        return if (r.code == 200) Result() else Result(errorOf(r))
    }

    /** Diğer oturumlar kapanır; sunucu bu cihaz için yeni belirteç verir */
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

    /** Verilerimi indir (KVKK md. 11): sunucunun JSON dökümü */
    suspend fun download(): Result {
        val r = call("GET", "/v1/verilerim", auth = true)
        return if (r.code == 200) Result(data = r.body) else Result(errorOf(r))
    }

    suspend fun signOut() {
        if (token != null) call("POST", "/api/auth/sign-out", JSONObject(), auth = true)
        save(null, null)
    }

    suspend fun delete(password: String): Result {
        val r = call("POST", "/api/auth/delete-user", JSONObject().put("password", password), auth = true)
        if (r.code != 200) return Result(errorOf(r, wrongPassword = true))
        save(null, null)
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
