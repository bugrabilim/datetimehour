package tr.bumba.time

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/* Ortak hukuk metinleri (standart madde 7 "Hukuk"; sahibin kararı S2, 12 Kasım 2026): Türkçe arayüzden Türkçe metne,
   öbür dillerden ?dil=en metnine. Hesap silme bilgi sayfası sitededir (herkese açık, mağaza kuralı). */
internal fun legalSuffix() = if (Strings.lang == "tr") "" else "?dil=en"
internal fun accountNoticeUrl(): String = "https://bumbagroup.com/uyelik-aydinlatma" + legalSuffix()
internal fun accountTermsUrl(): String = "https://bumbagroup.com/uyelik-sozlesmesi" + legalSuffix()
internal fun accountDeleteUrl(): String = "https://time.bumba.tr" + if (Strings.lang == "tr") "/hesap-silme/" else "/${Strings.lang}/account-deletion/"

/** Profil seçenekleri (server/src/profile.mjs ile aynı) */
private val SOCIAL = listOf("instagram", "x", "facebook", "linkedin", "youtube", "tiktok", "threads", "bluesky", "github", "telegram", "web")
private val VISIBLE = listOf("ad" to "firstName", "soyad" to "lastName", "takmaAd" to "nickname", "foto" to "photo", "dogumTarihi" to "birthDate",
    "cinsiyet" to "gender", "telefon" to "phone", "ulke" to "country", "sehir" to "city", "meslek" to "job", "hakkimda" to "about", "sosyal" to "social")

private fun A(key: String) = L("account.$key")
private fun flag(cc: String) = if (cc.length == 2) cc.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("") else ""

/** Ülke kodları ve telefon kodları (server/data/countries.json'dan; gen-strings uygulamaya kopyalar) */
private object Countries {
    private var cache: List<Triple<String, String, String>>? = null
    fun list(context: android.content.Context): List<Triple<String, String, String>> = cache ?: run {
        val o = runCatching { JSONObject(context.assets.open("countries.json").bufferedReader().readText()).getJSONObject("ulkeler") }.getOrNull() ?: JSONObject()
        val loc = Strings.locale
        o.keys().asSequence().map { cc -> Triple(cc, o.optString(cc), Locale("", cc).getDisplayCountry(loc)) }
            .sortedWith(compareBy(java.text.Collator.getInstance(loc)) { it.third }).toList().also { cache = it }
    }
}

/** [metin](adres) bağlantılı metin (onay cümlesi, isteğe bağlı bilgiler notu) */
@Composable
private fun LinkedText(raw: String, size: Int = 13) {
    val c = Theme.c
    val text = remember(raw) {
        buildAnnotatedString {
            var last = 0
            for (m in Regex("\\[([^\\]]+)\\]\\((https://[^)\\s]+)\\)").findAll(raw)) {
                append(raw.substring(last, m.range.first))
                withLink(LinkAnnotation.Url(m.groupValues[2], TextLinkStyles(SpanStyle(color = c.accent, textDecoration = TextDecoration.Underline)))) { append(m.groupValues[1]) }
                last = m.range.last + 1
            }
            append(raw.substring(last))
        }
    }
    Text(text, color = c.muted, fontSize = size.sp)
}

/** Şifre kuralı: alanın altında dört satır, sağlandıkça işaretlenir */
@Composable
private fun PasswordRules(password: String) {
    val rules = Account.passwordRules(password)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(A("pwRuleTitle"), color = Theme.c.muted, fontSize = 13.sp)
        for ((k, ok) in rules) Text((if (ok) "✓  " else "○  ") + A(k), color = if (ok) Theme.c.ok else Theme.c.muted, fontSize = 13.sp)
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, type: ContentType? = null, password: Boolean = false, email: Boolean = false,
                  singleLine: Boolean = true, readOnly: Boolean = false, keyboard: KeyboardType? = null) {
    OutlinedTextField(
        value, onChange,
        Modifier.fillMaxWidth().then(if (type != null) Modifier.semantics { contentType = type } else Modifier),
        label = { Text(label) }, singleLine = singleLine, readOnly = readOnly, shape = RoundedCornerShape(14.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard ?: when { password -> KeyboardType.Password; email -> KeyboardType.Email; else -> KeyboardType.Text }),
    )
}

/** Hata ya da başarı iletisi; "account.err.field:alan" biçimi alan adını yazar */
@Composable
private fun Message(key: String?, ok: Boolean = false) {
    if (key == null) return
    val text = if (key.startsWith("account.err.field:")) T("account.err.field", key.substringAfter(':')) else L(key)
    Text(text, color = if (ok) Theme.c.ok else Theme.c.err, fontSize = 14.sp)
}

/** Seçim kutusu: ekrandaki değer + açılır liste */
@Composable
private fun Picker(label: String, value: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(role = Role.DropdownList) { open = true }.padding(vertical = 6.dp)) {
            Text(label, color = Theme.c.muted, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(options.firstOrNull { it.first == value }?.second ?: A("none"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Filled.ArrowDropDown, null, tint = Theme.c.muted)
            }
        }
        DropdownMenu(open, { open = false }) {
            for ((k, v) in options) DropdownMenuItem({ Text(v) }, { onPick(k); open = false })
        }
    }
}

/**
 * Ayarlar › Hesap (standart 5.15, madde 7): girişsizken giriş, kayıt (yalnız e-posta ve şifre, Bumba üyeliği sorusu),
 * şifremi unuttum; girişliyken web'deki hesap sayfasının 10 bölümü, aynı sırayla. iOS'taki AccountView ile aynı akış.
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
            Text(A("title"), color = Theme.c.text, fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Text(u?.email ?: A("rowSignedOut"), Modifier.weight(1f), color = Theme.c.muted, fontSize = 14.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
        }
    }
}

@Composable
private fun SignedOut(initialNotice: String?) {
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf("in") } // in, up, forgot, link (Bumba üyeliğiyle giriş), done (kayıt bitti), separate
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var bumba by rememberSaveable { mutableStateOf(false) } // kayıtta e-posta başka bir Bumba ürününde kayıtlı
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf(initialNotice) }
    var canResend by remember { mutableStateOf(false) }
    fun run(block: suspend () -> Account.Result, onOk: (Account.Result) -> Unit = {}) {
        if (busy) return
        busy = true; err = null; info = null
        scope.launch {
            val r = block()
            busy = false
            if (r.ok) onOk(r)
            else if (r.err == "account.bumba") bumba = true
            else { err = r.err; canResend = r.err == "account.err.unverified" }
        }
    }
    // Girişten sonra: aynı e-postayla başka bir Bumba ürününde ayrı hesap varsa birleştirme sorulur
    fun signedIn() { scope.launch { if (Account.separateAccount()) { mode = "separate" } } }

    Text(A("intro"), color = Theme.c.muted, fontSize = 14.sp)
    if (mode == "in" || mode == "up") SoftSegmentedRow(Modifier.fillMaxWidth()) {
        SoftSegment(mode == "in", { mode = "in"; err = null; bumba = false }) { Text(A("signIn")) }
        SoftSegment(mode == "up", { mode = "up"; err = null; info = null }) { Text(A("signUp")) }
    }
    AppCard {
        when (mode) {
            "forgot" -> {
                SectionTitle(A("forgot"))
                Text(A("forgotLead"), color = Theme.c.text, fontSize = 14.sp)
                Field(email, { email = it.trim() }, A("email"), ContentType.EmailAddress, email = true)
                RoundButton(A("forgotSend"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { run({ Account.forgot(email) }) { info = "account.forgotSent" } }
                TextButton({ mode = "in"; err = null; info = null }) { Text(A("back"), color = Theme.c.accent) }
            }
            "up" -> if (bumba) {
                Text(A("bumbaQ"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(A("bumbaNote"), color = Theme.c.muted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundButton(A("yes"), true, Modifier.weight(1f)) { bumba = false; password = ""; mode = "link" }
                    RoundButton(A("no"), false, Modifier.weight(1f)) { bumba = false; email = ""; info = null; err = "account.bumbaNo" }
                }
            } else {
                Field(email, { email = it.trim() }, A("email"), ContentType.EmailAddress, email = true)
                Field(password, { password = it }, A("password"), ContentType.NewPassword, password = true)
                PasswordRules(password)
                RoundButton(A("signUp"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    run({
                        val q = Account.lookup(email)
                        when {
                            !q.ok -> q
                            q.data == "var" -> Account.Result("account.bumba")
                            else -> Account.signUp(email, password)
                        }
                    }) { password = ""; mode = "done"; canResend = true }
                }
                LinkedText(A("consent"))
            }
            "link" -> {
                SectionTitle(A("bumbaSignIn"))
                Text(A("bumbaNote"), color = Theme.c.muted, fontSize = 14.sp)
                Field(email, {}, A("email"), ContentType.EmailAddress, email = true, readOnly = true)
                Field(password, { password = it }, A("password"), ContentType.Password, password = true)
                RoundButton(A("signIn"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { run({ Account.link(email, password) }) { password = "" } }
                TextButton({ mode = "forgot"; err = null }) { Text(A("forgot"), color = Theme.c.accent) }
            }
            "done" -> {
                Message("account.signedUp", ok = true)
                TextButton({ mode = "in"; info = null }) { Text(A("signIn"), color = Theme.c.accent) }
            }
            else -> {
                Field(email, { email = it.trim() }, A("email"), ContentType.EmailAddress, email = true)
                Field(password, { password = it }, A("password"), ContentType.Password, password = true)
                RoundButton(A("signIn"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    run({ Account.signIn(email, password) }) { password = ""; signedIn() }
                }
                TextButton({ mode = "forgot"; err = null; info = null }) { Text(A("forgot"), color = Theme.c.accent) }
            }
        }
        Message(err)
        Message(info, ok = true)
        if (canResend && (mode == "in" || mode == "done")) TextButton({ run({ Account.resendVerification(email) }) { info = "account.resent" } }) {
            Text(A("resend"), color = Theme.c.accent)
        }
    }
}

@Composable
private fun AccountLinks() {
    val context = LocalContext.current
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    FlowRow(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(A("notice"), Modifier.clickable { open(accountNoticeUrl()) }, color = Theme.c.accent, fontSize = 13.sp)
        Text(A("terms"), Modifier.clickable { open(accountTermsUrl()) }, color = Theme.c.accent, fontSize = 13.sp)
        Text(A("deletionInfo"), Modifier.clickable { open(accountDeleteUrl()) }, color = Theme.c.accent, fontSize = 13.sp)
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
private fun Numbered(n: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(n.toString(), Modifier.width(24.dp), color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        SectionTitle(title)
    }
}

@Composable
private fun CheckLine(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange, colors = CheckboxDefaults.colors(checkedColor = Theme.c.accent))
        Text(label, color = Theme.c.text, fontSize = 15.sp)
    }
}

/** Fotoğraf: kare kırpılıp en çok 512 piksele küçültülür, WebP (≤ ~190 KB); yeniden kodlandığı için EXIF ve konum düşer */
private fun photoDataUrl(context: android.content.Context, uri: Uri): String? = runCatching {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    var sample = 1
    while (minOf(opts.outWidth, opts.outHeight) / (sample * 2) >= 512) sample *= 2
    val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
    val side = minOf(src.width, src.height)
    val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    val size = minOf(512, side)
    val out = Bitmap.createScaledBitmap(square, size, size, true)
    @Suppress("DEPRECATION")
    val fmt = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
    var q = 85
    var bytes: ByteArray
    do {
        bytes = ByteArrayOutputStream().also { out.compress(fmt, q, it) }.toByteArray()
        q -= 10
    } while (bytes.size > 190 * 1024 && q >= 35)
    "data:image/webp;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}.getOrNull()

private fun photoBitmap(dataUrl: String?): androidx.compose.ui.graphics.ImageBitmap? = runCatching {
    val b = Base64.decode(dataUrl!!.substringAfter(","), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(b, 0, b.size).asImageBitmap()
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SignedIn(onRoute: (String) -> Unit, onSignedOut: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val u = Account.user ?: return
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var loadErr by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) } // (bölüm, metin anahtarı, başarılı mı)
    var busy by remember { mutableStateOf(false) }
    var separate by remember { mutableStateOf(false) }
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

    LaunchedEffect(u.id) {
        val r = Account.load()
        if (r.ok) data = JSONObject(r.data!!) else loadErr = r.err
        separate = Account.separateAccount()
        AccountSync.sync()
    }
    val d = data
    if (d == null) { AppCard { Text(loadErr?.let { L(it) } ?: "…", color = Theme.c.muted) }; return }
    val p = d.optJSONObject("profil") ?: JSONObject()
    fun ps(k: String) = if (p.isNull(k)) "" else p.optString(k)

    // Profil alanları
    var ad by rememberSaveable { mutableStateOf(ps("ad")) }
    var soyad by rememberSaveable { mutableStateOf(ps("soyad")) }
    var takma by rememberSaveable { mutableStateOf(ps("takmaAd")) }
    var dogum by rememberSaveable { mutableStateOf(ps("dogumTarihi")) }
    var cinsiyet by rememberSaveable { mutableStateOf(ps("cinsiyet")) }
    var ulke by rememberSaveable { mutableStateOf(ps("ulke")) }
    val tel = p.optJSONObject("telefon")
    var kod by rememberSaveable { mutableStateOf(tel?.optString("ulkeKodu") ?: "") }
    var numara by rememberSaveable { mutableStateOf(tel?.optString("numara") ?: "") }
    var sehir by rememberSaveable { mutableStateOf(ps("sehir")) }
    var meslek by rememberSaveable { mutableStateOf(ps("meslek")) }
    var hakkimda by rememberSaveable { mutableStateOf(ps("hakkimda")) }
    val sosyal = remember { mutableStateListOf<Pair<String, String>>().apply {
        p.optJSONArray("sosyal")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { add(it.optString("platform") to it.optString("deger")) } }
    } }
    var foto by remember { mutableStateOf(if (p.isNull("foto")) null else p.optString("foto").ifEmpty { null }) }
    val gor = p.optJSONObject("gorunurluk")
    var acik by rememberSaveable { mutableStateOf(gor?.optBoolean("acik") == true) }
    val alanlar = remember { mutableStateListOf<String>().apply { gor?.optJSONArray("alanlar")?.let { a -> for (i in 0 until a.length()) add(a.getString(i)) } } }
    var picking by remember { mutableStateOf(false) }

    fun profileJson() = JSONObject().apply {
        fun s(k: String, v: String) = put(k, v.trim().ifEmpty { JSONObject.NULL })
        s("ad", ad); s("soyad", soyad); s("takmaAd", takma); s("dogumTarihi", dogum); s("cinsiyet", cinsiyet); s("ulke", ulke)
        s("sehir", sehir); s("meslek", meslek); s("hakkimda", hakkimda)
        val n = numara.filter { it.isDigit() }
        put("telefon", if (kod.isNotEmpty() && n.isNotEmpty()) JSONObject().put("ulkeKodu", kod).put("numara", n) else JSONObject.NULL)
        put("sosyal", JSONArray(sosyal.filter { it.second.isNotBlank() }.map { JSONObject().put("platform", it.first).put("deger", it.second.trim()) }))
        put("gorunurluk", JSONObject().put("acik", acik).put("alanlar", JSONArray(alanlar.toList())))
    }
    fun saveProfile(card: String) = run(card, { Account.saveProfile(profileJson()) }, "account.saved")

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val url = withContext(Dispatchers.Default) { photoDataUrl(context, uri) }
            if (url == null) { msg = Triple("profile", "account.err.generic", false); return@launch }
            val r = Account.savePhoto(url)
            if (r.ok) foto = url else msg = Triple("profile", r.err!!, false)
        }
    }
    val countries = remember { Countries.list(context) }
    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Strings.locale) }

    // Üst: e-posta, rozetlerim ve kayıtlarım, çıkış
    AppCard {
        SectionTitle(u.email)
        Text(A("signedInNote"), color = Theme.c.muted, fontSize = 13.sp)
        RoundButton(A("signOut"), false, Modifier.fillMaxWidth()) { scope.launch { Account.signOut(); onSignedOut("account.signedOut") } }
    }
    AppCard {
        SectionTitle(A("badgesTitle"))
        Text(T("account.badgesCount", maxOf(AccountSync.accountBadges, 0)), color = Theme.c.text, fontSize = 15.sp)
        Text(A("syncNote"), color = Theme.c.muted, fontSize = 13.sp)
        if (BadgeStore.saved.r.isNotEmpty()) RoundButton(A("badgesAdd"), false, Modifier.fillMaxWidth()) {
            run("badges", { if (AccountSync.addBadges()) Account.Result() else Account.Result("account.err.generic") }, "account.badgesAdded")
        }
        RoundButton(A("sync"), false, Modifier.fillMaxWidth()) {
            run("badges", { if (AccountSync.sync()) Account.Result() else Account.Result("account.err.network") }, "account.saved")
        }
        Text(if (AccountSync.records > 0) T("account.recordsCount", AccountSync.records) else A("recordsNone"), color = Theme.c.muted, fontSize = 13.sp)
        Text(A("badgesNote"), color = Theme.c.muted, fontSize = 13.sp)
        CardMessage("badges")
    }

    // 1. Profil
    AppCard {
        Numbered(1, A("profile"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bmp = remember(foto) { photoBitmap(foto) }
            if (bmp != null) Image(bmp, A("photo"), Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                TextButton({ pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(A("photoChoose"), color = Theme.c.accent) }
                if (foto != null) TextButton({ scope.launch { if (Account.savePhoto(null).ok) foto = null } }) { Text(A("photoRemove"), color = Theme.c.accent) }
            }
        }
        Text(A("photoNote"), color = Theme.c.muted, fontSize = 12.sp)
        Field(ad, { ad = it.take(100) }, A("firstName"), ContentType.PersonFirstName)
        Field(soyad, { soyad = it.take(100) }, A("lastName"), ContentType.PersonLastName)
        Field(takma, { takma = it.take(60) }, A("nickname"))
        Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { picking = true }.padding(vertical = 6.dp)) {
            Text(A("birthDate"), color = Theme.c.muted, fontSize = 13.sp)
            Text(dogum.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull() } ?: A("none"), color = Theme.c.text, fontSize = 16.sp)
        }
        Picker(A("gender"), cinsiyet, listOf("" to A("none"), "kadin" to A("genderF"), "erkek" to A("genderM"), "belirtmek-istemiyorum" to A("genderN"))) { cinsiyet = it }
        Picker(A("country"), ulke, listOf("" to A("none")) + countries.map { it.first to "${flag(it.first)} ${it.third}" }) { cc ->
            ulke = cc
            if (kod.isEmpty()) countries.firstOrNull { it.first == cc }?.second?.takeIf { it.isNotEmpty() }?.let { kod = "+$it" }
        }
        Text(A("phone"), color = Theme.c.muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(0.45f)) {
                Picker(A("phoneCode"), kod, listOf("" to A("phoneCode")) + countries.filter { it.second.isNotEmpty() }
                    .map { "+${it.second}" to "${flag(it.first)} +${it.second}" }.distinctBy { it.first }) { kod = it }
            }
            Box(Modifier.weight(0.55f)) { Field(numara, { numara = it.filter { c -> c.isDigit() || c == ' ' }.take(20) }, A("phoneNumber"), ContentType.PhoneNumber, keyboard = KeyboardType.Phone) }
        }
        Field(sehir, { sehir = it.take(100) }, A("city"), ContentType.AddressLocality)
        Field(meslek, { meslek = it.take(100) }, A("job"))
        Field(hakkimda, { hakkimda = it.take(160) }, A("about"), singleLine = false)
        Text(A("aboutHint") + " · ${hakkimda.length}/160", color = Theme.c.muted, fontSize = 12.sp)
        Text(A("social"), color = Theme.c.muted, fontSize = 13.sp)
        sosyal.forEachIndexed { i, (pl, v) ->
            Picker(A("social"), pl, SOCIAL.map { it to if (it == "web") A("socialWeb") else it.replaceFirstChar { c -> c.uppercase() } }) { sosyal[i] = it to v }
            Field(v, { sosyal[i] = pl to it.take(300) }, A("socialValue"))
            TextButton({ sosyal.removeAt(i) }) { Text(A("socialRemove"), color = Theme.c.accent) }
        }
        if (sosyal.size < 8) TextButton({ sosyal.add(SOCIAL[0] to "") }) { Text(A("socialAdd"), color = Theme.c.accent) }
        LinkedText(A("optionalNote"))
        RoundButton(A("save"), true, Modifier.fillMaxWidth()) { saveProfile("profile") }
        CardMessage("profile")
    }

    // 2. Görünürlük
    AppCard {
        Numbered(2, A("visibility"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(A("showProfile"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
            SoftSwitch(acik, { acik = it })
        }
        Text(A("visibleFields"), color = Theme.c.muted, fontSize = 13.sp)
        for ((field, label) in VISIBLE) CheckLine(field in alanlar, A(label)) { on -> if (on) alanlar.add(field) else alanlar.remove(field) }
        Text(A("emailNever"), color = Theme.c.muted, fontSize = 13.sp)
        val shown = when {
            acik && "takmaAd" in alanlar && takma.isNotBlank() -> takma
            acik && "ad" in alanlar && ad.isNotBlank() -> if ("soyad" in alanlar && soyad.isNotBlank()) "$ad $soyad" else ad
            else -> T("account.member", if (d.isNull("uyeNo")) "" else d.optLong("uyeNo").toString())
        }
        Text(T("account.displayName", shown), color = Theme.c.muted, fontSize = 13.sp)
        RoundButton(A("save"), true, Modifier.fillMaxWidth()) { saveProfile("visibility") }
        CardMessage("visibility")
    }

    // 3. E-posta
    var newEmail by rememberSaveable { mutableStateOf("") }
    AppCard {
        Numbered(3, A("emailSection"))
        Text(d.optString("eposta"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Field(newEmail, { newEmail = it.trim() }, A("newEmail"), ContentType.EmailAddress, email = true)
        RoundButton(A("changeEmail"), false, Modifier.fillMaxWidth()) { run("email", { Account.changeEmail(newEmail) }, "account.changeEmailSent") { newEmail = "" } }
        CardMessage("email")
    }

    // 4. Şifre
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    AppCard {
        Numbered(4, A("passwordSection"))
        Field(current, { current = it }, A("passwordCurrent"), ContentType.Password, password = true)
        Field(next, { next = it }, A("newPassword"), ContentType.NewPassword, password = true)
        PasswordRules(next)
        RoundButton(A("changePassword"), false, Modifier.fillMaxWidth()) {
            run("password", { Account.changePassword(current, next) }, "account.passwordChanged") { current = ""; next = "" }
        }
        CardMessage("password")
    }

    // 5. Dil, tema ve saat dilimi (uygulamada dil ve tema uygulamanın ayarlarıdır; saat dilimi telefonunki)
    AppCard(padding = 12) {
        Box(Modifier.padding(horizontal = 8.dp)) { Numbered(5, A("prefs")) }
        LinkRow("globe", A("language")) { onRoute("language") }
        LinkRow("palette", A("theme")) { onRoute("color") }
        Text(A("timeZone") + ": " + java.util.TimeZone.getDefault().id.replace('_', ' '), Modifier.padding(horizontal = 8.dp), color = Theme.c.text, fontSize = 15.sp)
        Text(A("prefsApp"), Modifier.padding(horizontal = 8.dp), color = Theme.c.muted, fontSize = 13.sp)
    }

    // 6. E-postalar
    var celebrate by remember { mutableStateOf(d.optBoolean("kutlama", true)) }
    AppCard {
        Numbered(6, A("emails"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(A("celebrate"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
            SoftSwitch(celebrate, { on -> celebrate = on; scope.launch { if (!Account.setCelebrate(on).ok) celebrate = !on } })
        }
        Text(A("celebrateNote"), color = Theme.c.muted, fontSize = 13.sp)
        Text(A("list"), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(A("listNote"), color = Theme.c.muted, fontSize = 13.sp)
        TextButton({ onRoute("about") }) { Text(A("listJoin"), color = Theme.c.accent) }
    }

    // 7. Açık oturumlar
    val sessions = remember { mutableStateListOf<JSONObject>() }
    suspend fun loadSessions() {
        val r = Account.sessions()
        sessions.clear()
        if (r.ok) runCatching { JSONArray(r.data) }.getOrNull()?.let { a -> for (i in 0 until a.length()) sessions.add(a.getJSONObject(i)) }
    }
    LaunchedEffect(u.id) { loadSessions() }
    val dt = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Strings.locale) }
    fun fmt(iso: String) = runCatching { Instant.parse(iso).atZone(java.time.ZoneId.systemDefault()).format(dt) }.getOrDefault(iso)
    AppCard {
        Numbered(7, A("sessions"))
        for (s in sessions) {
            val tok = s.optString("token")
            val dev = when (s.optString("client")) { "ios" -> A("deviceIos"); "android" -> A("deviceAndroid"); "" -> A("deviceWeb"); else -> A("deviceApp") }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(dev + if (Account.isCurrent(tok)) " · " + A("thisSession") else "", color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(T("account.opened", fmt(s.optString("createdAt"))) + " · " + T("account.lastUsed", fmt(s.optString("updatedAt"))), color = Theme.c.muted, fontSize = 12.sp)
                }
                if (!Account.isCurrent(tok)) TextButton({ scope.launch { Account.closeSession(tok); loadSessions() } }) { Text(A("close"), color = Theme.c.accent) }
            }
        }
        RoundButton(A("closeOthers"), false, Modifier.fillMaxWidth()) { scope.launch { Account.closeOtherSessions(); loadSessions() } }
    }

    // 8. Üyelik ve öbür Bumba ürünleri
    val products = remember { mutableStateListOf<JSONObject>() }
    var productsErr by remember { mutableStateOf(false) }
    var askOpen by remember { mutableStateOf<List<String>?>(null) }
    var askClose by remember { mutableStateOf<List<String>?>(null) }
    suspend fun loadProducts() {
        val r = Account.products()
        products.clear(); productsErr = !r.ok
        if (r.ok) runCatching { JSONArray(r.data) }.getOrNull()?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { if (it.optString("site").isNotEmpty()) products.add(it) } }
    }
    LaunchedEffect(u.id) { loadProducts() }
    AppCard {
        Numbered(8, A("membership"))
        Text(A("memberNo") + ": " + (if (d.isNull("uyeNo")) "—" else d.optLong("uyeNo").toString()), color = Theme.c.text, fontSize = 15.sp)
        val since = runCatching { Instant.parse(d.optString("uyelikTarihi")).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFmt) }.getOrDefault("")
        Text(A("memberSince") + ": " + since, color = Theme.c.text, fontSize = 15.sp)
        Text(A("products"), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(A("productsNote"), color = Theme.c.muted, fontSize = 13.sp)
        if (productsErr) Text(A("productsUnavailable"), color = Theme.c.muted, fontSize = 13.sp)
        for (pr in products) {
            val site = pr.optString("site"); val on = pr.optBoolean("acik"); val self = site == "saat-tarih" || pr.optBoolean("kendisi")
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(pr.optString("ad", site) + if (self) " · " + A("productThis") else "", Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                if (self) Text(A("productOn"), color = Theme.c.muted, fontSize = 14.sp)
                else SoftSwitch(on, { want -> if (want) askOpen = listOf(site) else askClose = listOf(site) })
            }
        }
        val others = products.filter { it.optString("site") != "saat-tarih" && !it.optBoolean("kendisi") }
        if (others.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundButton(A("openAll"), false, Modifier.weight(1f)) { others.filter { !it.optBoolean("acik") }.map { it.optString("site") }.takeIf { it.isNotEmpty() }?.let { askOpen = it } }
            RoundButton(A("closeAll"), false, Modifier.weight(1f)) { others.filter { it.optBoolean("acik") }.map { it.optString("site") }.takeIf { it.isNotEmpty() }?.let { askClose = it } }
        }
        CardMessage("products")
    }
    askOpen?.let { sites ->
        var pw by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { askOpen = null },
            text = { Column { Text(A("confirmOpen")); Field(pw, { pw = it }, A("password"), ContentType.Password, password = true) } },
            confirmButton = { TextButton({ askOpen = null; run("products", { Account.openProducts(sites, pw) }, "account.saved") { scope.launch { loadProducts() } } }) { Text(A("open")) } },
            dismissButton = { TextButton({ askOpen = null }) { Text(A("cancel")) } })
    }
    askClose?.let { sites ->
        val names = sites.joinToString(", ") { s -> products.firstOrNull { it.optString("site") == s }?.optString("ad", s) ?: s }
        AlertDialog(onDismissRequest = { askClose = null },
            text = { Text(T("account.confirmClose", names)) },
            confirmButton = { TextButton({ askClose = null; run("products", { Account.closeProducts(sites) }, "account.saved") { scope.launch { loadProducts() } } }) { Text(A("close"), color = Theme.c.err) } },
            dismissButton = { TextButton({ askClose = null }) { Text(A("cancel")) } })
    }

    // 9. Verilerimi indir
    AppCard {
        Numbered(9, A("download"))
        Text(A("downloadNote"), color = Theme.c.text, fontSize = 14.sp)
        RoundButton(A("download"), false, Modifier.fillMaxWidth()) {
            run("data", { Account.download() }, null) { r -> Share.json(context, "saat-tarih-hesap.json", r.data ?: "{}", A("download")) }
        }
        CardMessage("data")
    }

    // 10. Hesabı sil
    var scopeAll by rememberSaveable { mutableStateOf(false) }
    var delPw by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    AppCard {
        Numbered(10, A("delete"))
        Text(A("deleteLead"), color = Theme.c.text, fontSize = 14.sp)
        for ((all, label) in listOf(false to A("deleteScopeProduct"), true to A("deleteScopeAll"))) {
            Row(Modifier.fillMaxWidth().selectable(scopeAll == all, role = Role.RadioButton) { scopeAll = all }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(scopeAll == all, null, colors = RadioButtonDefaults.colors(selectedColor = Theme.c.accent))
                Text(label, color = Theme.c.text, fontSize = 15.sp)
            }
        }
        Field(delPw, { delPw = it }, A("deletePassword"), ContentType.Password, password = true)
        Text(A("deviceKeeps"), color = Theme.c.muted, fontSize = 13.sp)
        RoundButton(A("delete"), false, Modifier.fillMaxWidth()) { confirm = true }
        CardMessage("delete")
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        text = { Text(A("deleteConfirm")) },
        confirmButton = {
            TextButton({
                confirm = false
                run("delete", { Account.delete(if (scopeAll) "hepsi" else "urun", delPw) }, null) { delPw = ""; onSignedOut("account.deleted") }
            }) { Text(A("delete"), color = Theme.c.err) }
        },
        dismissButton = { TextButton({ confirm = false }) { Text(A("cancel")) } },
    )

    // Doğum tarihi: yalnız 16 ile 90 yaş arası seçilebilir (sunucu da denetler)
    if (picking) {
        val today = LocalDate.now()
        val latest = today.minusYears(16); val earliest = today.minusYears(90).plusDays(1)
        val ms = { x: LocalDate -> x.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dogum.takeIf { it.isNotEmpty() }?.let { runCatching { ms(LocalDate.parse(it)) }.getOrNull() } ?: ms(latest),
            yearRange = earliest.year..latest.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in ms(earliest)..ms(latest)
            },
        )
        DatePickerDialog(onDismissRequest = { picking = false },
            confirmButton = { TextButton({ state.selectedDateMillis?.let { dogum = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; picking = false }) { Text(L("common.done")) } },
            dismissButton = { TextButton({ dogum = ""; picking = false }) { Text(A("none")) } }) { DatePicker(state) }
    }

    // Girişten sonra sorular: ayrı hesap birleştirme ve (var olan hesaba girişte bir kez) bu cihazdaki rozetler
    if (separate) {
        var pw by remember { mutableStateOf("") }
        var e by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { separate = false },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(A("ayriQ")); Text(A("ayriNote"), fontSize = 13.sp); Field(pw, { pw = it }, A("password"), ContentType.Password, password = true); Message(e) } },
            confirmButton = { TextButton({ scope.launch { val r = Account.merge(pw); if (r.ok) { separate = false; msg = Triple("badges", "account.ayriDone", true); Account.load().data?.let { data = JSONObject(it) } } else e = r.err } }) { Text(A("yes")) } },
            dismissButton = { TextButton({ separate = false }) { Text(A("no")) } })
    }
    if (AccountSync.askBadges > 0) AlertDialog(onDismissRequest = { AccountSync.declineBadges() },
        text = { Text(T("account.badgesAsk", AccountSync.askBadges)) },
        confirmButton = { TextButton({ scope.launch { AccountSync.addBadges() } }) { Text(A("yes")) } },
        dismissButton = { TextButton({ AccountSync.declineBadges() }) { Text(A("no")) } })
}
