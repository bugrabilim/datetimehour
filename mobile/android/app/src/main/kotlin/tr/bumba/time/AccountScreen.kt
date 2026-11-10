package tr.bumba.time

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Sitedeki aydınlatma metni (gizlilik › hesap bölümü) ve hesap silme sayfası, uygulamanın dilinde */
internal fun accountNoticeUrl(): String = "https://time.bumba.tr" + if (Strings.lang == "tr") "/gizlilik/#hesap" else "/${Strings.lang}/privacy/#account"
internal fun accountDeleteUrl(): String = "https://time.bumba.tr" + if (Strings.lang == "tr") "/hesap-silme/" else "/${Strings.lang}/account-deletion/"

/**
 * Ayarlar › Hesap (üyelik aşama 1): girişsizken giriş / kayıt / şifremi unuttum; girişliyken görünen ad, dil ve tema
 * (uygulamanın ayarları), e-posta listesi, e-posta ve şifre değiştirme, verilerimi indir, çıkış, hesabı silme.
 * iOS'taki AccountView ile aynı akış ve metinler (`account.*`).
 */
@Composable
fun AccountScreen(onRoute: (String) -> Unit) {
    LaunchedEffect(Unit) { if (Account.signedIn) Account.refresh() }
    var notice by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (Account.signedIn) SignedIn(onRoute) { notice = it } else SignedOut(notice)
        AccountLinks()
    }
}

/** Ayarlar'ın en üstündeki satır: hesap simgesi, "Hesap", sağda e-posta ya da girişsiz notu */
@Composable
fun AccountSettingsRow(onClick: () -> Unit) {
    val u = Account.user
    AppCard(padding = 4) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            DuoChip("account", Theme.c.sage)
            Spacer(Modifier.width(14.dp))
            Text(L("account.title"), color = Theme.c.text, fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Text(u?.email ?: L("account.rowSignedOut"), Modifier.weight(1f), color = Theme.c.muted, fontSize = 14.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, type: ContentType? = null, password: Boolean = false, email: Boolean = false) {
    OutlinedTextField(
        value, onChange,
        Modifier.fillMaxWidth().then(if (type != null) Modifier.semantics { contentType = type } else Modifier),
        label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(14.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = when { password -> KeyboardType.Password; email -> KeyboardType.Email; else -> KeyboardType.Text }),
    )
}

@Composable
private fun Message(key: String?, ok: Boolean = false) {
    if (key != null) Text(L(key), color = if (ok) Theme.c.ok else Theme.c.err, fontSize = 14.sp)
}

@Composable
private fun SignedOut(initialNotice: String?) {
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf("in") } // in, up, forgot
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf(initialNotice) }
    var canResend by remember { mutableStateOf(false) }
    fun run(block: suspend () -> Account.Result, onOk: () -> Unit = {}) {
        if (busy) return
        busy = true; err = null; info = null
        scope.launch {
            val r = block()
            busy = false
            if (r.ok) onOk() else { err = r.err; canResend = r.err == "account.err.unverified" }
        }
    }
    Text(L("account.intro"), color = Theme.c.muted, fontSize = 14.sp)
    if (mode != "forgot") SoftSegmentedRow(Modifier.fillMaxWidth()) {
        SoftSegment(mode == "in", { mode = "in"; err = null }) { Text(L("account.signIn")) }
        SoftSegment(mode == "up", { mode = "up"; err = null }) { Text(L("account.signUp")) }
    }
    AppCard {
        when (mode) {
            "forgot" -> {
                SectionTitle(L("account.forgot"))
                Text(L("account.forgotLead"), color = Theme.c.text, fontSize = 14.sp)
                Field(email, { email = it.trim() }, L("account.email"), ContentType.EmailAddress, email = true)
                RoundButton(L("account.forgotSend"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    run({ Account.forgot(email) }) { info = "account.forgotSent" }
                }
                TextButton({ mode = "in"; err = null; info = null }) { Text(L("account.backToSignIn"), color = Theme.c.accent) }
            }
            "up" -> {
                Field(name, { name = it.take(60) }, L("account.nameOptional"), ContentType.PersonFullName)
                Field(email, { email = it.trim() }, L("account.email"), ContentType.EmailAddress, email = true)
                Field(password, { password = it }, L("account.password"), ContentType.NewPassword, password = true)
                Text(L("account.passwordHint"), color = Theme.c.muted, fontSize = 13.sp)
                RoundButton(L("account.signUpButton"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    run({ Account.signUp(email, password, name) }) { info = "account.signedUp"; canResend = true; password = ""; mode = "in" }
                }
                NoticeLink()
            }
            else -> {
                Field(email, { email = it.trim() }, L("account.email"), ContentType.EmailAddress, email = true)
                Field(password, { password = it }, L("account.password"), ContentType.Password, password = true)
                RoundButton(L("account.signInButton"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    run({ Account.signIn(email, password) }) { password = "" }
                }
                TextButton({ mode = "forgot"; err = null; info = null }) { Text(L("account.forgot"), color = Theme.c.accent) }
            }
        }
        Message(err)
        Message(info, ok = true)
        if (canResend && mode == "in") TextButton({ run({ Account.resendVerification(email) }) { info = "account.resent" } }) {
            Text(L("account.resend"), color = Theme.c.accent)
        }
    }
}

@Composable
private fun NoticeLink() {
    val context = LocalContext.current
    Text(L("account.noticeLead") + " " + L("account.noticeLink"), Modifier.clickable {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(accountNoticeUrl())))
    }, color = Theme.c.accent, fontSize = 13.sp)
}

@Composable
private fun AccountLinks() {
    val context = LocalContext.current
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(L("account.noticeLink"), Modifier.clickable { open(accountNoticeUrl()) }, color = Theme.c.accent, fontSize = 13.sp)
        Text(L("account.deletePage"), Modifier.clickable { open(accountDeleteUrl()) }, color = Theme.c.accent, fontSize = 13.sp)
    }
}

@Composable
private fun LinkRow(icon: String, title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        DuoChip(icon, Theme.c.sage)
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
    }
}

@Composable
private fun SignedIn(onRoute: (String) -> Unit, onSignedOut: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val u = Account.user ?: return
    var name by rememberSaveable(u.id) { mutableStateOf(u.name) }
    var newEmail by rememberSaveable { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var delPw by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    // Her kartın kendi iletisi: (kart, metin anahtarı, başarılı mı)
    var msg by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }
    fun run(card: String, block: suspend () -> Account.Result, okKey: String?, onOk: (Account.Result) -> Unit = {}) {
        if (busy) return
        busy = true; msg = null
        scope.launch {
            val r = block()
            busy = false
            if (r.ok) { onOk(r); if (okKey != null) msg = Triple(card, okKey, true) } else msg = Triple(card, r.err!!, false)
        }
    }
    @Composable fun CardMessage(card: String) { msg?.takeIf { it.first == card }?.let { Message(it.second, it.third) } }

    AppCard {
        SectionTitle(u.name.ifEmpty { u.email })
        if (u.name.isNotEmpty()) Text(u.email, color = Theme.c.muted, fontSize = 14.sp)
        Text(L("account.signedInNote"), color = Theme.c.muted, fontSize = 13.sp)
    }
    AppCard {
        SectionTitle(L("account.nameLabel"))
        Field(name, { name = it.take(60) }, L("account.nameLabel"), ContentType.PersonFullName)
        RoundButton(L("account.save"), false, Modifier.fillMaxWidth()) { run("name", { Account.updateName(name) }, "account.saved") }
        CardMessage("name")
    }
    AppCard(padding = 12) {
        Text(L("account.prefs"), Modifier.padding(horizontal = 8.dp), color = Theme.c.muted, fontSize = 13.sp)
        LinkRow("globe", L("lang.title")) { onRoute("language") }
        LinkRow("palette", L("settings.color")) { onRoute("color") }
        LinkRow("mail", L("list.heading")) { onRoute("about") }
    }
    AppCard {
        SectionTitle(L("account.changeEmail"))
        Field(newEmail, { newEmail = it.trim() }, L("account.newEmail"), ContentType.EmailAddress, email = true)
        RoundButton(L("account.changeEmail"), false, Modifier.fillMaxWidth()) {
            run("email", { Account.changeEmail(newEmail) }, "account.changeEmailSent") { newEmail = "" }
        }
        CardMessage("email")
    }
    AppCard {
        SectionTitle(L("account.changePassword"))
        Field(current, { current = it }, L("account.passwordCurrent"), ContentType.Password, password = true)
        Field(next, { next = it }, L("account.passwordNew"), ContentType.NewPassword, password = true)
        Text(L("account.passwordHint"), color = Theme.c.muted, fontSize = 13.sp)
        RoundButton(L("account.changePassword"), false, Modifier.fillMaxWidth()) {
            run("password", { Account.changePassword(current, next) }, "account.passwordChanged") { current = ""; next = "" }
        }
        CardMessage("password")
    }
    AppCard {
        SectionTitle(L("account.download"))
        Text(L("account.downloadNote"), color = Theme.c.text, fontSize = 14.sp)
        // Döküm paylaşım penceresiyle kaydedilir (Dosyalar'a, e-postaya…)
        RoundButton(L("account.download"), false, Modifier.fillMaxWidth()) {
            run("data", { Account.download() }, null) { r -> Share.json(context, "saat-tarih-hesap.json", r.data ?: "{}", L("account.download")) }
        }
        CardMessage("data")
    }
    RoundButton(L("account.signOut"), false, Modifier.fillMaxWidth()) {
        scope.launch { Account.signOut(); onSignedOut("account.signedOut") }
    }
    AppCard {
        SectionTitle(L("account.delete"))
        Text(L("account.deleteLead"), color = Theme.c.text, fontSize = 14.sp)
        Field(delPw, { delPw = it }, L("account.password"), ContentType.Password, password = true)
        RoundButton(L("account.delete"), false, Modifier.fillMaxWidth()) { if (delPw.isNotEmpty()) confirm = true }
        CardMessage("delete")
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        text = { Text(L("account.deleteConfirm")) },
        confirmButton = {
            TextButton({
                confirm = false
                run("delete", { Account.delete(delPw) }, null) { delPw = ""; onSignedOut("account.deleted") }
            }) { Text(L("account.delete"), color = Theme.c.err) }
        },
        dismissButton = { TextButton({ confirm = false }) { Text(L("common.cancel")) } },
    )
}
