package tr.bumba.time

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Halkalar (üyelik aşama 2b, 15 Kasım 2026; sahibin onayı "tüm önerilerin uygun"): ortak zikir ve hatim halkaları,
 * üyelik sunucusundaki `/v1/halka…` uçlarıyla (server/src/circles.mjs). Giriş ister; ilk kurma ya da katılmada ayrı açık
 * rıza (`circle.consent`). Halka adı ve görünen ad dışında kullanıcı içeriği yok; Bildir, Engelle, kurucu Çıkar.
 * Sunucuda kapalıyken (HALKALAR değişkeni yok) uçlar 404 verir: ekran "yakında" der. iOS'taki Circles.swift ile aynı.
 */
object Circles {
    const val LINK = "https://time.bumba.tr/davet/#"

    /** Davet bağlantısıyla gelen kod (aşama 2c): Halkalar › Kodla katıl bununla açılır */
    var pendingCode by mutableStateOf<String?>(null)

    /** Davet kodu biçimi: sunucudaki gibi 8 harf/rakam (karışan harfler yok) */
    fun cleanCode(s: String?): String? = s?.uppercase()?.filter { it.isLetterOrDigit() }?.takeIf { it.length == 8 }

    /** https://time.bumba.tr/davet/#KOD ya da saattarih://davet?kod=KOD (aşama 2c) */
    fun handle(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        val site = uri.host == "time.bumba.tr" && uri.path.orEmpty().startsWith("/davet")
        val app = uri.scheme == "saattarih" && uri.host == "davet"
        if (!site && !app) return false
        // Grup daveti "g:" önekiyle (…/davet/#g:KOD, saattarih://davet?kod=KOD&tur=grup)
        val raw = uri.getQueryParameter("kod") ?: uri.fragment
        val group = uri.getQueryParameter("tur") == "grup" || raw?.startsWith("g:") == true
        val code = cleanCode(raw?.removePrefix("g:")) ?: return false
        if (group) Groups.pendingCode = code else pendingCode = code
        return true
    }

    data class Member(val id: Long, val name: String?, val blocked: Boolean, val you: Boolean, val owner: Boolean, val count: Long)
    data class Juz(val n: Int, val state: String, val member: Long?, val you: Boolean, val gone: String?)
    data class Circle(
        val id: String, val kind: String, val name: String?, val owner: Boolean, val members: Int,
        val zikir: String? = null, val target: Long = 0, val total: Long = 0,
        val round: Int = 1, val khatms: Int = 0, val doneJuz: Int = 0,
        val code: String? = null, val memberList: List<Member> = emptyList(), val juz: List<Juz> = emptyList(), val ends: String? = null,
    )

    /** Sonuç: [err] doluysa metin anahtarı; "closed" sunucuda kapalı */
    data class Res<T>(val value: T? = null, val err: String? = null)

    private fun errOf(r: Account.Resp): String {
        if (r.code == 404 && r.body.contains("\"yok\"")) return "closed"
        val h = runCatching { JSONObject(r.body).optString("hata") }.getOrDefault("")
        return when (h) {
            "kod" -> "circle.err.code"
            "ad", "ad-icerik" -> "circle.err.name"
            "halka-sinir" -> "circle.err.limit"
            "alinmis", "senin-degil" -> "circle.err.taken"
            "bitti" -> "circle.err.ended"
            "dolu" -> "circle.full"
            "riza" -> "riza"
            else -> Account.errorOf(r)
        }
    }

    private fun parse(o: JSONObject): Circle {
        val h = o.optJSONObject("hatim")
        val ms = o.optJSONArray("uyeler") ?: JSONArray()
        val js = o.optJSONArray("cuzler") ?: JSONArray()
        return Circle(
            id = o.getString("id"), kind = o.optString("tur"), name = o.optString("ad").takeIf { !o.isNull("ad") && it.isNotEmpty() },
            owner = o.optBoolean("sahip"), members = o.optInt("uyeSayisi", ms.length()),
            zikir = o.optString("zikir").ifEmpty { null }, target = o.optLong("hedef"), total = o.optLong("toplam"),
            round = h?.optInt("tur", 1) ?: 1, khatms = h?.optInt("biten") ?: 0,
            doneJuz = h?.optInt("bitenCuz", -1)?.takeIf { it >= 0 } ?: (0 until js.length()).count { js.getJSONObject(it).optString("durum") == "bitti" },
            code = o.optString("kod").ifEmpty { null }, ends = o.optString("bitis").ifEmpty { null }.takeIf { !o.isNull("bitis") },
            memberList = (0 until ms.length()).map { i ->
                val m = ms.getJSONObject(i)
                Member(m.getLong("id"), m.optString("ad").takeIf { !m.isNull("ad") && it.isNotEmpty() }, m.optBoolean("engelli"),
                    m.optBoolean("sen"), m.optBoolean("sahip"), m.optLong("sayi"))
            },
            juz = (0 until js.length()).map { i ->
                val j = js.getJSONObject(i)
                Juz(j.getInt("cuz"), j.optString("durum"), if (j.isNull("uye")) null else j.optLong("uye"), j.optBoolean("sen"),
                    j.optString("eski").ifEmpty { null }.takeIf { !j.isNull("eski") })
            },
        )
    }

    suspend fun list(): Res<Pair<List<Circle>, Boolean>> {
        val r = Account.call("GET", "/v1/halkalar", auth = true)
        if (r.code != 200) return Res(err = errOf(r))
        val o = JSONObject(r.body)
        val a = o.optJSONArray("halkalar") ?: JSONArray()
        return Res((0 until a.length()).map { parse(a.getJSONObject(it)) } to o.optBoolean("riza"))
    }

    private suspend fun detailCall(method: String, path: String, body: JSONObject? = null): Res<Circle> {
        val r = Account.call(method, path, body, auth = true)
        return if (r.code == 200) Res(parse(JSONObject(r.body))) else Res(err = errOf(r))
    }

    suspend fun detail(id: String) = detailCall("GET", "/v1/halka/$id")
    suspend fun create(kind: String, name: String, zikir: String, target: Long, consent: Boolean) =
        detailCall("POST", "/v1/halka", JSONObject().put("tur", kind).put("ad", name).put("zikir", zikir).put("hedef", target).put("riza", consent))
    suspend fun join(code: String, consent: Boolean) = detailCall("POST", "/v1/halka/katil", JSONObject().put("kod", code).put("riza", consent))
    suspend fun juz(id: String, n: Int, op: String) = detailCall("POST", "/v1/halka/$id/cuz", JSONObject().put("cuz", n).put("islem", op))
    suspend fun kick(id: String, member: Long) = detailCall("POST", "/v1/halka/$id/cikar", JSONObject().put("uye", member))
    suspend fun block(id: String, member: Long, on: Boolean) = detailCall("POST", "/v1/halka/$id/engelle", JSONObject().put("uye", member).put("kaldir", !on))
    suspend fun edit(id: String, body: JSONObject) = detailCall("POST", "/v1/halka/$id/duzenle", body)

    /** Kodla önizleme: tür, ad, üye sayısı, dolu mu */
    suspend fun preview(code: String): Res<JSONObject> {
        val r = Account.call("POST", "/v1/halka/onizle", JSONObject().put("kod", code), auth = true)
        return if (r.code == 200) Res(JSONObject(r.body)) else Res(err = errOf(r))
    }

    suspend fun addZikir(id: String, n: Int): Res<Long> {
        val r = Account.call("POST", "/v1/halka/$id/zikir", JSONObject().put("ekle", n), auth = true)
        return if (r.code == 200) Res(JSONObject(r.body).optLong("toplam")) else Res(err = errOf(r))
    }

    suspend fun simple(path: String, body: JSONObject = JSONObject()): String? {
        val r = Account.call("POST", path, body, auth = true)
        return if (r.code == 200) null else errOf(r)
    }

    suspend fun report(id: String, member: Long?): String? = simple("/v1/halka/$id/bildir", if (member != null) JSONObject().put("uye", member) else JSONObject())

    fun num(n: Long): String = java.text.NumberFormat.getIntegerInstance(Strings.numberLocale).format(n)
    fun num(n: Int): String = num(n.toLong())

    fun kindTitle(c: Circle) = L(if (c.kind == "hatim") "circle.kindHatim" else "circle.kindZikir")
    fun title(c: Circle) = c.name ?: L("circle.nameHidden")
    fun progress(c: Circle): String = if (c.kind == "hatim") T("circle.khatm", c.round) + " · " + T("circle.juzDone", c.doneJuz)
        else num(c.total) + " / " + num(c.target)
}

/** Halkalar: liste, kur, kodla katıl; seçilince ayrıntı */
@Composable
fun CirclesScreen(onBack: () -> Unit, onAccount: () -> Unit) {
    val scope = rememberCoroutineScope()
    var circles by remember { mutableStateOf<List<Circles.Circle>?>(null) }
    var consent by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) } // "create" | "join"
    var reload by remember { mutableIntStateOf(0) }
    var askWithdraw by remember { mutableStateOf(false) }
    // Davet bağlantısı ekran açıkken gelirse de katılma sayfası açılır
    LaunchedEffect(Circles.pendingCode, Account.signedIn) { if (Circles.pendingCode != null && Account.signedIn) sheet = "join" }
    LaunchedEffect(reload, Account.signedIn) {
        if (!Account.signedIn) return@LaunchedEffect
        val r = Circles.list()
        err = r.err
        r.value?.let { circles = it.first; consent = it.second }
    }
    open?.let { id ->
        BackHandler { open = null; reload++ }
        CircleDetail(id) { open = null; reload++ }
        return
    }
    when (sheet) {
        "create" -> { BackHandler { sheet = null }; CircleCreate(consent, { sheet = null }) { c -> sheet = null; open = c.id }; return }
        "join" -> { BackHandler { sheet = null }; CircleJoin(consent, { sheet = null }) { c -> sheet = null; open = c.id }; return }
    }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("circle.title"), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(L("circle.intro"), color = Theme.c.muted, fontSize = 15.sp, lineHeight = 21.sp)
            when {
                !Account.signedIn -> AppCard {
                    Text(L("circle.signIn"), color = Theme.c.text, fontSize = 16.sp)
                    RoundButton(L("circle.signInButton"), true, Modifier.fillMaxWidth()) { onAccount() }
                }
                err == "closed" -> EmptyState(Icons.Filled.Groups, L("circle.closed"), null)
                err != null -> Text(L(err!!), color = Theme.c.err, fontSize = 15.sp)
                circles == null -> {}
                else -> {
                    if (circles!!.isEmpty()) EmptyState(Icons.Filled.Groups, L("circle.empty"), null)
                    for (c in circles!!) CircleCard(c) { open = c.id }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RoundButton(L("circle.join"), false, Modifier.weight(1f)) { sheet = "join" }
                        RoundButton(L("circle.create"), true, Modifier.weight(1f)) { sheet = "create" }
                    }
                    Text(L("circle.privacyNote"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
                    if (consent) TextButton(onClick = { askWithdraw = true }) { Text(L("circle.withdraw"), color = Theme.c.err) }
                }
            }
        }
    }
    if (askWithdraw) AlertDialog(
        onDismissRequest = { askWithdraw = false },
        text = { Text(L("circle.withdrawConfirm")) },
        confirmButton = {
            TextButton(onClick = {
                askWithdraw = false
                scope.launch { Circles.simple("/v1/halka/riza/geri-al"); reload++ }
            }) { Text(L("circle.withdraw"), color = Theme.c.err) }
        },
        dismissButton = { TextButton(onClick = { askWithdraw = false }) { Text(L("common.cancel")) } },
    )
}

@Composable
private fun CircleCard(c: Circles.Circle, onClick: () -> Unit) {
    AppCard(Modifier.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DuoChip(if (c.kind == "hatim") "book" else "beads", Theme.c.gold)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(Circles.title(c), color = if (c.name == null) Theme.c.muted else Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(Circles.kindTitle(c) + " · " + T("circle.members", c.members), color = Theme.c.muted, fontSize = 13.sp)
            }
        }
        Text(Circles.progress(c), color = Theme.c.text, fontSize = 15.sp, style = Tnum)
        if (c.kind == "zikir" && c.target > 0) LinearProgressIndicator(
            progress = { (c.total.toFloat() / c.target).coerceIn(0f, 1f) }, Modifier.fillMaxWidth(),
            color = Theme.c.accent, trackColor = Theme.c.surface2,
        )
    }
}

/** Açık rıza: metin + onay kutusu (sunucu ilk kurma/katılmada ister) */
@Composable
private fun ConsentBlock(checked: Boolean, onChange: (Boolean) -> Unit) {
    AppCard(background = Theme.c.surface2) {
        Text(L("circle.consent"), color = Theme.c.text, fontSize = 14.sp, lineHeight = 20.sp)
        Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, onChange, colors = CheckboxDefaults.colors(checkedColor = Theme.c.accent))
            Text(L("circle.consentCheck"), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun CircleCreate(hasConsent: Boolean, onBack: () -> Unit, onDone: (Circles.Circle) -> Unit) {
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf("zikir") }
    var name by remember { mutableStateOf("") }
    var zikir by remember { mutableStateOf("subhanallah") }
    var target by remember { mutableStateOf("1000") }
    var agree by remember { mutableStateOf(hasConsent) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("circle.create"), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftSegmentedRow {
                SoftSegment(kind == "zikir", { kind = "zikir" }) { Text(L("circle.kindZikir"), maxLines = 1) }
                SoftSegment(kind == "hatim", { kind = "hatim" }) { Text(L("circle.kindHatim"), maxLines = 1) }
            }
            OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), label = { Text(L("circle.name")) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Theme.c.accent, unfocusedBorderColor = Theme.c.border))
            if (kind == "zikir") {
                val kinds = listOf("subhanallah", "elhamdulillah", "allahuekber", "tevhid", "istigfar", "salavat")
                ChoiceRow(L("zikir.title"), L("zikir.kind.$zikir"), kinds.mapIndexed { i, k -> i to L("zikir.kind.$k") }) { zikir = kinds[it] }
                OutlinedTextField(target, { v -> target = v.filter { it.isDigit() }.take(8) }, Modifier.fillMaxWidth(),
                    label = { Text(L("circle.target")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Theme.c.accent, unfocusedBorderColor = Theme.c.border))
            }
            if (!hasConsent) ConsentBlock(agree) { agree = it }
            err?.let { Text(L(it), color = Theme.c.err, fontSize = 14.sp) }
        }
        RoundButton(L("circle.create"), true, Modifier.fillMaxWidth().padding(16.dp)) {
            if (busy || name.isBlank() || !agree) return@RoundButton
            busy = true
            scope.launch {
                val r = Circles.create(kind, name.trim(), zikir, target.toLongOrNull() ?: 0, agree)
                busy = false
                r.value?.let(onDone)
                err = r.err?.let { if (it == "riza") null else it }
            }
        }
    }
}

@Composable
private fun CircleJoin(hasConsent: Boolean, onBack: () -> Unit, onDone: (Circles.Circle) -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(Circles.pendingCode ?: "") }
    var preview by remember { mutableStateOf<JSONObject?>(null) }
    var agree by remember { mutableStateOf(hasConsent) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(code) {
        Circles.pendingCode = null
        preview = null
        if (code.length == 8) {
            val r = Circles.preview(code)
            preview = r.value
            err = r.err
        } else err = null
    }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("circle.join"), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(code, { v -> code = v.uppercase().filter { it.isLetterOrDigit() }.take(8) }, Modifier.fillMaxWidth(),
                label = { Text(L("circle.codeField")) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Theme.c.accent, unfocusedBorderColor = Theme.c.border))
            preview?.let { p ->
                AppCard {
                    Text(p.optString("ad").takeIf { !p.isNull("ad") && it.isNotEmpty() } ?: L("circle.nameHidden"),
                        color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(L(if (p.optString("tur") == "hatim") "circle.kindHatim" else "circle.kindZikir") + " · " + T("circle.members", p.optInt("uyeSayisi")),
                        color = Theme.c.muted, fontSize = 14.sp)
                    if (p.optBoolean("dolu")) Text(L("circle.full"), color = Theme.c.err, fontSize = 14.sp)
                }
                if (!hasConsent) ConsentBlock(agree) { agree = it }
            }
            err?.let { Text(L(it), color = Theme.c.err, fontSize = 14.sp) }
        }
        val ready = preview != null && !preview!!.optBoolean("dolu") && agree
        RoundButton(L("circle.joinButton"), ready, Modifier.fillMaxWidth().padding(16.dp)) {
            if (!ready) return@RoundButton
            scope.launch {
                val r = Circles.join(code, agree)
                r.value?.let(onDone)
                err = r.err?.let { if (it == "riza") null else it }
            }
        }
    }
}

/** Halka ayrıntısı: zikir sayacı ya da 30 cüz, üyeler, davet ve kurucu işleri */
@Composable
internal fun CircleDetail(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var c by remember { mutableStateOf<Circles.Circle?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableIntStateOf(0) }
    var ask by remember { mutableStateOf<String?>(null) } // "leave" | "delete" | "rename"
    var newName by remember { mutableStateOf("") }
    LaunchedEffect(id) { Circles.detail(id).let { c = it.value; err = it.err } }
    fun apply(r: Circles.Res<Circles.Circle>) { r.value?.let { c = it }; err = r.err }
    val cur = c
    val menu = buildList<Pair<String, () -> Unit>> {
        if (cur != null) {
            add(L("circle.reportName") to { scope.launch { note = Circles.report(id, null) ?: "circle.reported" } })
            if (cur.owner) {
                add(L("circle.rename") to { newName = cur.name ?: ""; ask = "rename" })
                add(L("circle.renewCode") to { scope.launch { apply(Circles.edit(id, JSONObject().put("kodYenile", true))) } })
                add(L("circle.delete") to { ask = "delete" })
            }
            add(L("circle.leave") to { ask = "leave" })
        }
    }
    Column(Modifier.fillMaxSize()) {
        SubBar(cur?.let { Circles.title(it) } ?: L("circle.title"), onBack) { if (menu.isNotEmpty()) MoreMenu(menu) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            err?.let { Text(L(it), color = Theme.c.err, fontSize = 14.sp) }
            note?.let { Text(L(it), color = Theme.c.ok, fontSize = 14.sp) }
            if (cur == null) return@Column
            Text(Circles.kindTitle(cur) + " · " + T("circle.members", cur.memberList.size), color = Theme.c.muted, fontSize = 14.sp)
            if (cur.kind == "zikir") {
                AppCard {
                    Text(L("zikir.kind.${cur.zikir ?: "subhanallah"}"), color = Theme.c.muted, fontSize = 14.sp)
                    Text(Circles.progress(cur), color = Theme.c.text, fontSize = 30.sp, fontWeight = FontWeight.Light, style = Tnum)
                    if (cur.target > 0) LinearProgressIndicator(progress = { (cur.total.toFloat() / cur.target).coerceIn(0f, 1f) },
                        Modifier.fillMaxWidth(), color = Theme.c.accent, trackColor = Theme.c.surface2)
                    val mine = cur.memberList.firstOrNull { it.you }?.count ?: 0
                    Text(L("circle.yourShare") + ": " + Circles.num(mine), color = Theme.c.muted, fontSize = 14.sp)
                }
                // Dokunarak sayma: bir dokunuş bir; "Gönder" birikeni ekler
                Box(Modifier.fillMaxWidth().aspectRatio(2.2f).clip(RoundedCornerShape(22.dp)).background(Theme.c.accentSoft)
                    .clickable { pending++ }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Circles.num(pending), color = Theme.c.text, fontSize = 44.sp, fontWeight = FontWeight.Light, style = Tnum)
                        Text(L("zikir.tap"), color = Theme.c.muted, fontSize = 14.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (n in listOf(33, 100)) RoundButton("+$n", false, Modifier.weight(1f)) { pending += n }
                    RoundButton(T("circle.send", pending), pending > 0, Modifier.weight(1.6f)) {
                        val n = pending.coerceAtMost(10_000)
                        if (n <= 0) return@RoundButton
                        scope.launch {
                            val r = Circles.addZikir(id, n)
                            if (r.err == null) { pending -= n; apply(Circles.detail(id)) } else err = r.err
                        }
                    }
                }
            } else {
                AppCard {
                    Text(T("circle.khatm", cur.round) + " · " + T("circle.juzDone", cur.juz.count { it.state == "bitti" }),
                        color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    JuzGrid(cur) { j, op -> scope.launch { apply(Circles.juz(id, j, op)) } }
                }
            }
            // Davet
            cur.code?.let { code ->
                AppCard {
                    Text(L("circle.code"), color = Theme.c.muted, fontSize = 13.sp)
                    Text(code, color = Theme.c.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, style = Tnum, letterSpacing = 3.sp)
                    RoundButton(L("circle.share"), false, Modifier.fillMaxWidth()) {
                        val text = T("circle.shareText", Circles.title(cur), code, Circles.LINK + code)
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), L("circle.share")))
                    }
                }
            }
            SectionTitle(L("circle.membersTitle"))
            AppCard(padding = 8) {
                for (m in cur.memberList) MemberRow(cur, m) { action ->
                    scope.launch {
                        when (action) {
                            "report" -> note = Circles.report(id, m.id) ?: "circle.reported"
                            "block" -> apply(Circles.block(id, m.id, true))
                            "unblock" -> apply(Circles.block(id, m.id, false))
                            "kick" -> apply(Circles.kick(id, m.id))
                        }
                    }
                }
            }
            Text(L("circle.privacyNote"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
    when (ask) {
        "leave", "delete" -> AlertDialog(
            onDismissRequest = { ask = null },
            text = { Text(L(if (ask == "leave") "circle.leaveConfirm" else "circle.deleteConfirm")) },
            confirmButton = {
                val what = ask
                TextButton(onClick = {
                    ask = null
                    scope.launch {
                        val e = Circles.simple("/v1/halka/$id/" + if (what == "leave") "ayril" else "sil")
                        if (e == null) onBack() else err = e
                    }
                }) { Text(L(if (what == "leave") "circle.leave" else "circle.delete"), color = Theme.c.err) }
            },
            dismissButton = { TextButton(onClick = { ask = null }) { Text(L("common.cancel")) } },
        )
        "rename" -> AlertDialog(
            onDismissRequest = { ask = null },
            title = { Text(L("circle.rename")) },
            text = { OutlinedTextField(newName, { newName = it.take(40) }, singleLine = true, label = { Text(L("circle.name")) }) },
            confirmButton = {
                TextButton(onClick = {
                    ask = null
                    scope.launch { apply(Circles.edit(id, JSONObject().put("ad", newName.trim()))) }
                }) { Text(L("common.save"), color = Theme.c.accent) }
            },
            dismissButton = { TextButton(onClick = { ask = null }) { Text(L("common.cancel")) } },
        )
    }
}

/** 30 cüz: boş, alındı, sizde, bitti; dokununca işler */
@Composable
private fun JuzGrid(c: Circles.Circle, onAction: (Int, String) -> Unit) {
    var menuFor by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in c.juz.chunked(6)) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (j in row) {
                val bg = when {
                    j.state == "bitti" -> Theme.c.sage.copy(alpha = 0.35f)
                    j.you -> Theme.c.accent
                    j.state == "alindi" -> Theme.c.gold.copy(alpha = 0.35f)
                    else -> Theme.c.surface2
                }
                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(bg).clickable { menuFor = j.n },
                    contentAlignment = Alignment.Center) {
                    Text("${j.n}", color = if (j.you && j.state != "bitti") Theme.c.onAccent else Theme.c.text, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, style = Tnum)
                    DropdownMenu(menuFor == j.n, { menuFor = null }) {
                        val label = when {
                            j.state == "bitti" -> L("circle.juzDoneLabel")
                            j.you -> L("circle.juzMine")
                            j.state == "alindi" -> L("circle.juzTaken")
                            else -> L("circle.juzFree")
                        }
                        DropdownMenuItem({ Text("${j.n} · $label", color = Theme.c.muted) }, {}, enabled = false)
                        if (j.state == "bos") {
                            DropdownMenuItem({ Text(L("circle.juzTake")) }, { menuFor = null; onAction(j.n, "al") })
                            DropdownMenuItem({ Text(L("circle.juzFinish")) }, { menuFor = null; onAction(j.n, "bitir") })
                        }
                        if (j.you && j.state == "alindi") {
                            DropdownMenuItem({ Text(L("circle.juzFinish")) }, { menuFor = null; onAction(j.n, "bitir") })
                            DropdownMenuItem({ Text(L("circle.juzRelease")) }, { menuFor = null; onAction(j.n, "birak") })
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for ((color, key) in listOf(Theme.c.surface2 to "circle.juzFree", Theme.c.gold.copy(alpha = 0.35f) to "circle.juzTaken",
                Theme.c.accent to "circle.juzMine", Theme.c.sage.copy(alpha = 0.35f) to "circle.juzDoneLabel")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(12.dp).aspectRatio(1f).clip(RoundedCornerShape(3.dp)).background(color))
                    Text(" " + L(key), color = Theme.c.muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MemberRow(c: Circles.Circle, m: Circles.Member, onAction: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = !m.you) { menu = true }
        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (m.blocked) L("circle.blocked") else m.name ?: L("circle.nameHidden"),
                color = if (m.blocked || m.name == null) Theme.c.muted else Theme.c.text, fontSize = 16.sp)
            val tags = listOfNotNull(if (m.you) L("circle.you") else null, if (m.owner) L("circle.owner") else null)
            if (tags.isNotEmpty()) Text(tags.joinToString(" · "), color = Theme.c.muted, fontSize = 12.sp)
        }
        if (c.kind == "zikir") Text(Circles.num(m.count), color = Theme.c.text, fontSize = 15.sp, style = Tnum, textAlign = TextAlign.End)
        else {
            val n = c.juz.count { it.member == m.id && it.state == "bitti" }
            if (n > 0) Text("$n", color = Theme.c.text, fontSize = 15.sp, style = Tnum)
        }
        Box {
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text(L("circle.report")) }, { menu = false; onAction("report") })
                DropdownMenuItem({ Text(L(if (m.blocked) "circle.unblock" else "circle.block")) }, { menu = false; onAction(if (m.blocked) "unblock" else "block") })
                if (c.owner) DropdownMenuItem({ Text(L("circle.kick"), color = Theme.c.err) }, { menu = false; onAction("kick") })
            }
        }
    }
}
