package tr.bumba.time

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject

/*
 * Hata bildir (sahibin isteği, 15 Kasım 2026): Ayarlar › Destek. Hesap gerekmez; bildirim hesap sunucusuna
 * (`POST /v1/geri-bildirim`) gider, oradan yalnız e-postayla bilgi@bumbagroup.com'a iletilir. Gönderilen: metin,
 * isteğe bağlı e-posta, uygulama sürümü, cihaz türü (marka, model, Android sürümü) ve dil. iOS'ta FeedbackView aynı.
 */
object Feedback {
    const val MIN = 10
    const val MAX = 4000
    fun privacyUrl(): String = Support.SITE + if (Strings.lang == "tr") "/gizlilik/#hata-bildirimi" else "/${Strings.lang}/privacy/#report-problem"

    /** null: gönderildi; değilse gösterilecek metnin anahtarı */
    suspend fun send(message: String, email: String, version: String): String? {
        val msg = message.trim()
        val mail = email.trim().lowercase()
        if (msg.length < MIN) return "account.fbShort"
        if (mail.isNotEmpty() && !Account.validEmail(mail)) return "account.err.email"
        val body = JSONObject().put("mesaj", msg.take(MAX)).put("eposta", mail).put("kaynak", "android")
            .put("surum", version).put("cihaz", "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
            .put("dil", Strings.lang)
        val r = Account.call("POST", "/v1/geri-bildirim", body)
        return when {
            r.code == 200 -> null
            r.code == 0 -> "account.err.network"
            r.code == 429 -> "account.err.tooMany"
            r.body.contains("\"kisa\"") -> "account.fbShort"
            r.body.contains("\"eposta\"") -> "account.err.email"
            else -> "account.err.generic"
        }
    }
}

@Composable
fun FeedbackScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) } // "ok" ya da hata anahtarı
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(L("account.fbIntro"), color = Theme.c.text, fontSize = 15.sp)
        AppCard {
            OutlinedTextField(
                message, { if (it.length <= Feedback.MAX) message = it },
                Modifier.fillMaxWidth().heightIn(min = 160.dp),
                label = { Text(L("account.fbMessage")) }, shape = RoundedCornerShape(14.dp), minLines = 5,
            )
            OutlinedTextField(
                email, { email = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text(L("account.fbEmail")) }, singleLine = true, shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            result?.let {
                if (it == "ok") Text(L("account.fbSent"), color = Theme.c.ok, fontSize = 14.sp)
                else Text(L(it), color = Theme.c.err, fontSize = 14.sp)
            }
            RoundButton(L("account.fbSend"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (busy) return@RoundButton
                busy = true; result = null
                scope.launch {
                    val err = Feedback.send(message, email, Support.version(context))
                    busy = false
                    if (err == null) { message = ""; result = "ok" } else result = err
                }
            }
        }
        Text(L("account.fbPrivacy"), color = Theme.c.muted, fontSize = 12.sp)
        TextButton({ Support.open(context, Feedback.privacyUrl()) }) { Text(L("account.fbPrivacyLink"), color = Theme.c.accent) }
    }
}
