package tr.bumba.time

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Ortak gruplar (üyelik aşama 2d, 15 Kasım 2026; sahibin onayı "tüm önerilerin uygun"; docs/uyelik-ortak-ozellikler-2026-11.md):
 * ortak geri sayım, ortak dünya saati, toplantı oylaması, odak odası, alarm önerisi, gözlem sayacı. Üyelik sunucusundaki
 * `/v1/grup…` uçları (server/src/groups.mjs). Giriş ister; din verisi olmadığı için ayrı rıza yok. Halka adı/görünen ad
 * dışında kullanıcı metni yalnız grup adı, şehir adı ve alarm etiketi (sunucuda süzgeç); Bildir, Engelle, kurucu Çıkar.
 * Sunucuda kapalıyken (GRUPLAR) uçlar 404 verir: ekran "yakında" der. iOS'taki Groups.swift ile aynı.
 */
object Groups {
    /** Davet bağlantısı: halkalarla aynı sayfa, kod "g:" önekiyle (aşama 2c) */
    const val LINK = "https://time.bumba.tr/davet/#g:"
    val KINDS = listOf("geri-sayim", "dunya", "oylama", "odak", "alarm", "gozlem")
    val ICONS = listOf("🎉", "🎂", "💍", "🎓", "✈️", "🏖️", "🎄", "🌙", "⭐", "❤️", "🏠", "👶", "⚽", "🎵", "📚", "🕌")
    fun icon(kind: String) = when (kind) {
        "geri-sayim" -> "hourglass"; "dunya" -> "globe"; "oylama" -> "checklist"; "odak" -> "tomato"; "alarm" -> "alarm"; else -> "telescope"
    }

    var pendingCode by mutableStateOf<String?>(null)

    data class Res<T>(val value: T? = null, val err: String? = null)

    fun errOf(r: Account.Resp): String {
        if (r.code == 404 && r.body.contains("\"yok\"")) return "closed"
        return when (runCatching { JSONObject(r.body).optString("hata") }.getOrDefault("")) {
            "kod" -> "group.err.code"
            "ad", "ad-icerik" -> "circle.err.name"
            "sehir" -> "circle.err.name"
            "etiket" -> "circle.err.name"
            "grup-sinir" -> "group.err.limit"
            "dolu" -> "group.full"
            "alarm-sinir" -> "group.a.limit"
            "engelli" -> "group.a.blocked"
            "suruyor" -> "group.f.none"
            "bitti" -> "group.p.closed"
            else -> Account.errorOf(r)
        }
    }

    private suspend fun obj(method: String, path: String, body: JSONObject? = null): Res<JSONObject> {
        val r = Account.call(method, path, body, auth = true)
        return if (r.code == 200) Res(JSONObject(r.body)) else Res(err = errOf(r))
    }

    suspend fun list(): Res<List<JSONObject>> = obj("GET", "/v1/gruplar").let { r ->
        if (r.value == null) Res(err = r.err) else r.value.optJSONArray("gruplar").let { a -> Res((0 until (a?.length() ?: 0)).map { a!!.getJSONObject(it) }) }
    }
    suspend fun detail(id: String) = obj("GET", "/v1/grup/$id")
    suspend fun create(body: JSONObject) = obj("POST", "/v1/grup", body)
    suspend fun preview(code: String) = obj("POST", "/v1/grup/onizle", JSONObject().put("kod", code))
    suspend fun join(code: String) = obj("POST", "/v1/grup/katil", JSONObject().put("kod", code))
    suspend fun op(id: String, op: String, body: JSONObject = JSONObject()) = obj("POST", "/v1/grup/$id/$op", body)
    suspend fun weatherNear(lat: Double, lon: Double) = obj("GET", "/v1/hava-bildirim?enlem=$lat&boylam=$lon")
    suspend fun weatherReport(kind: String, lat: Double, lon: Double) =
        obj("POST", "/v1/hava-bildirim", JSONObject().put("tur", kind).put("enlem", lat).put("boylam", lon))

    fun name(o: JSONObject) = o.optString("ad").takeIf { !o.isNull("ad") && it.isNotEmpty() } ?: L("circle.nameHidden")
    fun kindTitle(kind: String) = L("group.k.$kind")

    /** Geri sayım: yıllık ise bir sonraki yıl dönümü */
    fun target(o: JSONObject, now: Instant = Instant.now()): Instant {
        var t = Instant.ofEpochMilli(o.optLong("hedef"))
        if (o.optBoolean("yillik")) {
            val z = ZoneId.systemDefault()
            var d = t.atZone(z)
            while (d.toInstant().plusSeconds(86_400).isBefore(now)) d = d.plusYears(1)
            t = d.toInstant()
        }
        return t
    }

    /** Gözlem sayacının olay anahtarı (sunucu: [a-z0-9-]{3,60}) */
    fun eventKey(e: SkyEvent): String {
        val d = Instant.ofEpochMilli(e.t).atZone(ZoneId.of("UTC")).toLocalDate()
        val who = (e.id ?: e.p ?: listOfNotNull(e.a, e.b).joinToString("-")).lowercase().replace(Regex("[^a-z0-9-]"), "")
        return "${e.k}-$d-$who".lowercase().replace(Regex("[^a-z0-9-]"), "").take(60)
    }

    /** Alarm önerisinin günleri: bit i → ISO gün i+1 */
    fun daysOf(mask: Int) = (0..6).filter { mask and (1 shl it) != 0 }.map { it + 1 }
}

/** Tarih ve saat seçici (sistem pencereleri) */
internal fun pickDateTime(context: Context, start: ZonedDateTime, onPick: (ZonedDateTime) -> Unit) {
    android.app.DatePickerDialog(context, { _, y, m, d ->
        android.app.TimePickerDialog(context, { _, h, min -> onPick(ZonedDateTime.of(y, m + 1, d, h, min, 0, 0, start.zone)) },
            start.hour, start.minute, !Prefs.h12).show()
    }, start.year, start.monthValue - 1, start.dayOfMonth).show()
}

/** "1:02:03" ya da "02:03" */
private fun hms(sec: Long): String { val s = maxOf(0L, sec); return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60) else "%02d:%02d".format(s / 60, s % 60) }

private fun dateTimeText(t: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    Fmt.longDate(t.atZone(zone).toLocalDate()) + " · " + Fmt.time(t, zone)

/** Gruplar: liste, kur, kodla katıl; seçilince ayrıntı */
@Composable
fun GroupsScreen(onBack: () -> Unit, onAccount: () -> Unit) {
    var groups by remember { mutableStateOf<List<JSONObject>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload, Account.signedIn) {
        if (!Account.signedIn) return@LaunchedEffect
        val r = Groups.list()
        err = r.err
        r.value?.let { groups = it }
    }
    LaunchedEffect(Groups.pendingCode, Account.signedIn) { if (Groups.pendingCode != null && Account.signedIn) sheet = "join" }
    open?.let { id ->
        BackHandler { open = null; reload++ }
        GroupDetail(id) { open = null; reload++ }
        return
    }
    when (sheet) {
        "create" -> { BackHandler { sheet = null }; GroupCreate({ sheet = null }) { id -> sheet = null; open = id }; return }
        "join" -> { BackHandler { sheet = null }; GroupJoin({ sheet = null }) { id -> sheet = null; open = id }; return }
    }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("group.title"), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(L("group.intro"), color = Theme.c.muted, fontSize = 15.sp, lineHeight = 21.sp)
            when {
                !Account.signedIn -> AppCard {
                    Text(L("group.signIn"), color = Theme.c.text, fontSize = 16.sp)
                    RoundButton(L("circle.signInButton"), true, Modifier.fillMaxWidth()) { onAccount() }
                }
                err == "closed" -> EmptyState(Icons.Filled.Groups, L("group.closed"), null)
                err != null -> Text(L(err!!), color = Theme.c.err, fontSize = 15.sp)
                groups == null -> {}
                else -> {
                    if (groups!!.isEmpty()) EmptyState(Icons.Filled.Groups, L("group.empty"), null)
                    for (g in groups!!) GroupCard(g) { open = g.getString("id") }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RoundButton(L("circle.join"), false, Modifier.weight(1f)) { sheet = "join" }
                        RoundButton(L("group.create"), true, Modifier.weight(1f)) { sheet = "create" }
                    }
                    Text(L("group.privacyNote"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun GroupCard(g: JSONObject, onClick: () -> Unit) {
    val kind = g.optString("tur")
    val now = rememberNow()
    AppCard(Modifier.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DuoChip(Groups.icon(kind), Theme.c.sky)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(Groups.name(g), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(Groups.kindTitle(kind) + " · " + T("circle.members", g.optInt("uyeSayisi")), color = Theme.c.muted, fontSize = 13.sp)
            }
        }
        val line = when (kind) {
            "geri-sayim" -> {
                val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), Groups.target(g, now).atZone(ZoneId.systemDefault()).toLocalDate())
                g.optString("simge") + " " + if (days <= 0) L("group.cd.today") else T("group.cd.days", days)
            }
            "odak" -> if (g.has("oturum")) L("group.f.left") + ": " + hms((g.getJSONObject("oturum").getLong("bas") + g.getJSONObject("oturum").getInt("dk") * 60_000L - now.toEpochMilli()) / 1000) else null
            "alarm" -> g.optInt("bekleyen").takeIf { it > 0 }?.let { L("group.a.incoming") + ": " + it }
            else -> null
        }
        line?.let { Text(it, color = Theme.c.text, fontSize = 15.sp, style = Tnum) }
    }
}

@Composable
internal fun GroupCreate(onBack: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var kind by remember { mutableStateOf("geri-sayim") }
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(ZonedDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0)) }
    var yearly by remember { mutableStateOf(false) }
    var icon by remember { mutableStateOf("🎉") }
    var slots by remember { mutableStateOf(listOf<ZonedDateTime>()) }
    var duration by remember { mutableIntStateOf(60) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        SubBar(L("group.create"), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(L("group.kindTitle"))
            AppCard(padding = 8) {
                for (k in Groups.KINDS) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { kind = k }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(kind == k, { kind = k }, colors = RadioButtonDefaults.colors(selectedColor = Theme.c.accent))
                    Column(Modifier.weight(1f)) {
                        Text(L("group.k.$k"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(L("group.d.$k"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 17.sp)
                    }
                }
            }
            OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), label = { Text(L("group.name")) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Theme.c.accent, unfocusedBorderColor = Theme.c.border))
            if (kind == "geri-sayim") AppCard {
                Row(Modifier.fillMaxWidth().clickable { pickDateTime(context, target) { target = it } }.padding(vertical = 8.dp)) {
                    Text(L("group.cd.target"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Text(dateTimeText(target.toInstant()), color = Theme.c.accent, fontSize = 15.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("group.cd.yearly"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    SoftSwitch(yearly, { yearly = it }, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
                Text(L("group.cd.icon"), color = Theme.c.muted, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (e in Groups.ICONS) Box(Modifier.clip(RoundedCornerShape(10.dp)).clickable { icon = e }
                        .then(if (e == icon) Modifier.paper(Theme.c.accentSoft, 10) else Modifier).padding(8.dp)) { Text(e, fontSize = 22.sp) }
                }
            }
            if (kind == "oylama") AppCard {
                ChoiceRow(T("group.p.duration", duration), "",
                    listOf(15, 30, 45, 60, 90, 120).map { it to T("group.p.duration", it) }) { duration = it }
                Text(L("group.p.slots"), color = Theme.c.muted, fontSize = 13.sp)
                for ((i, s) in slots.withIndex()) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateTimeText(s.toInstant()), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    TextButton(onClick = { slots = slots.filterIndexed { j, _ -> j != i } }) { Text("×", color = Theme.c.muted, fontSize = 18.sp) }
                }
                if (slots.size < 12) RoundButton(L("group.p.add"), false, Modifier.fillMaxWidth()) {
                    pickDateTime(context, slots.lastOrNull()?.plusHours(1) ?: ZonedDateTime.now().plusDays(1).withHour(10).withMinute(0)) { p ->
                        slots = (slots + p.withSecond(0).withNano(0)).distinct().sortedBy { it.toInstant() }
                    }
                }
            }
            err?.let { Text(L(it), color = Theme.c.err, fontSize = 14.sp) }
        }
        val ready = name.isNotBlank() && (kind != "oylama" || slots.isNotEmpty())
        RoundButton(L("group.create"), ready, Modifier.fillMaxWidth().padding(16.dp)) {
            if (busy || !ready) return@RoundButton
            busy = true
            val b = JSONObject().put("tur", kind).put("ad", name.trim())
            if (kind == "geri-sayim") b.put("hedef", target.toInstant().toEpochMilli()).put("yillik", yearly).put("simge", icon)
            if (kind == "oylama") b.put("sure", duration).put("saatler", JSONArray(slots.map { it.toEpochSecond() / 60 }))
            scope.launch {
                val r = Groups.create(b)
                busy = false
                err = r.err
                r.value?.let { onDone(it.getString("id")) }
            }
        }
    }
}

@Composable
private fun GroupJoin(onBack: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(Groups.pendingCode ?: "") }
    var preview by remember { mutableStateOf<JSONObject?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(code) {
        Groups.pendingCode = null
        preview = null
        if (code.length == 8) Groups.preview(code).let { preview = it.value; err = it.err } else err = null
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
                    Text(Groups.name(p), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(Groups.kindTitle(p.optString("tur")) + " · " + T("circle.members", p.optInt("uyeSayisi")), color = Theme.c.muted, fontSize = 14.sp)
                    if (p.optBoolean("dolu")) Text(L("group.full"), color = Theme.c.err, fontSize = 14.sp)
                }
            }
            err?.let { Text(L(it), color = Theme.c.err, fontSize = 14.sp) }
        }
        val ready = preview != null && !preview!!.optBoolean("dolu")
        RoundButton(L("circle.joinButton"), ready, Modifier.fillMaxWidth().padding(16.dp)) {
            if (!ready) return@RoundButton
            scope.launch { Groups.join(code).let { r -> err = r.err; r.value?.let { onDone(it.getString("id")) } } }
        }
    }
}

/** Grup ayrıntısı: türüne göre bölüm, davet, üyeler ve kurucu işleri */
@Composable
internal fun GroupDetail(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var g by remember { mutableStateOf<JSONObject?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var ask by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    LaunchedEffect(id) { Groups.detail(id).let { g = it.value; err = it.err } }
    fun run(op: String, body: JSONObject = JSONObject(), ok: String? = null) = scope.launch {
        val r = Groups.op(id, op, body)
        r.value?.let { g = it; note = ok }
        err = r.err
    }
    val cur = g
    val menu = buildList<Pair<String, () -> Unit>> {
        if (cur != null) {
            add(L("group.reportName") to { scope.launch { val r = Groups.op(id, "bildir"); note = if (r.err == null) "circle.reported" else null; err = r.err } })
            if (cur.optBoolean("sahip")) {
                add(L("circle.rename") to { newName = cur.optString("ad"); ask = "rename" })
                add(L("circle.renewCode") to { run("duzenle", JSONObject().put("kodYenile", true)) })
                add(L("group.delete") to { ask = "delete" })
            }
            add(L("group.leave") to { ask = "leave" })
        }
    }
    Column(Modifier.fillMaxSize()) {
        SubBar(cur?.let { Groups.name(it) } ?: L("group.title"), onBack) { if (menu.isNotEmpty()) MoreMenu(menu) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            err?.let { Text(L(it), color = Theme.c.err, fontSize = 14.sp) }
            note?.let { Text(L(it), color = Theme.c.ok, fontSize = 14.sp) }
            if (cur == null) return@Column
            val kind = cur.optString("tur")
            val members = cur.optJSONArray("uyeler") ?: JSONArray()
            val ms = (0 until members.length()).map { members.getJSONObject(it) }
            Text(Groups.kindTitle(kind) + " · " + T("circle.members", ms.size), color = Theme.c.muted, fontSize = 14.sp)
            when (kind) {
                "geri-sayim" -> CountdownSection(cur, { note = "group.cd.added" }) { t -> run("duzenle", JSONObject().put("hedef", t)) }
                "dunya" -> WorldSection(ms) { body -> run("yerim", body) }
                "oylama" -> PollSection(cur, ms, { v -> run("oy", JSONObject().put("oylar", JSONArray(v)), "group.p.saved") }) { run("duzenle", JSONObject().put("kapali", true)) }
                "odak" -> FocusSection(cur, ms) { op, dk -> run("odak", JSONObject().put("islem", op).put("dk", dk)) }
                "alarm" -> AlarmSection(cur, ms, { body -> run("alarm", body, "group.a.sent") }, { req, ok ->
                    if (ok) {
                        val days = Groups.daysOf(req.optInt("gunler"))
                        val (h, m) = req.getString("saat").split(":").map { it.toInt() }
                        AlarmItem.addNew(context, AlarmItem(hour = h, minute = m, label = req.optString("etiket").takeIf { !req.isNull("etiket") } ?: "", days = days))
                    }
                    run("alarm-yanit", JSONObject().put("istek", req.getInt("id")).put("kabul", ok), if (ok) "group.a.installed" else null)
                }) { req -> scope.launch { val r = Groups.op(id, "bildir", JSONObject().put("istek", req.getInt("id"))); note = if (r.err == null) "circle.reported" else null; Groups.detail(id).value?.let { g = it } } }
                "gozlem" -> WatchSection(cur) { key, on -> run("izledim", JSONObject().put("olay", key).put("kaldir", !on)) }
            }
            // Davet
            cur.optString("kod").takeIf { it.isNotEmpty() }?.let { code ->
                AppCard {
                    Text(L("circle.code"), color = Theme.c.muted, fontSize = 13.sp)
                    Text(code, color = Theme.c.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, style = Tnum, letterSpacing = 3.sp)
                    RoundButton(L("circle.share"), false, Modifier.fillMaxWidth()) {
                        val text = T("group.shareText", Groups.name(cur), code, Groups.LINK + code)
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), L("circle.share")))
                    }
                }
            }
            SectionTitle(L("circle.membersTitle"))
            AppCard(padding = 8) {
                for (m in ms) GroupMemberRow(cur, m) { action ->
                    when (action) {
                        "report" -> scope.launch { val r = Groups.op(id, "bildir", JSONObject().put("uye", m.getLong("id"))); note = if (r.err == null) "circle.reported" else null; err = r.err }
                        "block" -> run("engelle", JSONObject().put("uye", m.getLong("id")))
                        "unblock" -> run("engelle", JSONObject().put("uye", m.getLong("id")).put("kaldir", true))
                        "kick" -> run("cikar", JSONObject().put("uye", m.getLong("id")))
                    }
                }
            }
            Text(L("group.privacyNote"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
    when (ask) {
        "leave", "delete" -> AlertDialog(
            onDismissRequest = { ask = null },
            text = { Text(L(if (ask == "leave") "group.leaveConfirm" else "group.deleteConfirm")) },
            confirmButton = {
                val what = ask
                TextButton(onClick = {
                    ask = null
                    scope.launch { val r = Groups.op(id, if (what == "leave") "ayril" else "sil"); if (r.err == null) onBack() else err = r.err }
                }) { Text(L(if (what == "leave") "group.leave" else "group.delete"), color = Theme.c.err) }
            },
            dismissButton = { TextButton(onClick = { ask = null }) { Text(L("common.cancel")) } },
        )
        "rename" -> AlertDialog(
            onDismissRequest = { ask = null },
            title = { Text(L("circle.rename")) },
            text = { OutlinedTextField(newName, { newName = it.take(40) }, singleLine = true, label = { Text(L("group.name")) }) },
            confirmButton = { TextButton(onClick = { ask = null; run("duzenle", JSONObject().put("ad", newName.trim())) }) { Text(L("common.save"), color = Theme.c.accent) } },
            dismissButton = { TextButton(onClick = { ask = null }) { Text(L("common.cancel")) } },
        )
    }
}

@Composable
private fun CountdownSection(g: JSONObject, onAdded: () -> Unit, onEdit: (Long) -> Unit) {
    val context = LocalContext.current
    val now = rememberNow()
    val t = Groups.target(g, now)
    val left = maxOf(0L, (t.toEpochMilli() - now.toEpochMilli()) / 1000)
    AppCard {
        Text(g.optString("simge", "🎉"), fontSize = 34.sp)
        Text(if (left == 0L) L("group.cd.today") else T("group.cd.days", left / 86_400), color = Theme.c.text, fontSize = 28.sp, fontWeight = FontWeight.Light, style = Tnum)
        if (left > 0) Text(hms(left % 86_400), color = Theme.c.muted, fontSize = 16.sp, style = Tnum)
        Text(dateTimeText(t), color = Theme.c.muted, fontSize = 14.sp)
        RoundButton(L("group.cd.add"), true, Modifier.fillMaxWidth()) {
            val list = CountdownItem.load()
            val title = Groups.name(g)
            if (list.none { it.target == t.toEpochMilli() && it.title == title })
                CountdownItem.save(list + CountdownItem(title = title, emoji = g.optString("simge", "🎉"), target = g.optLong("hedef"), yearly = g.optBoolean("yillik")))
            CountdownImport.version++
            onAdded()
        }
        if (g.optBoolean("sahip")) TextButton(onClick = {
            pickDateTime(context, t.atZone(ZoneId.systemDefault())) { onEdit(it.toInstant().toEpochMilli()) }
        }) { Text(L("group.cd.target"), color = Theme.c.accent) }
    }
}

@Composable
private fun WorldSection(ms: List<JSONObject>, onSet: (JSONObject) -> Unit) {
    val now = rememberNow()
    val me = ms.firstOrNull { it.optBoolean("sen") }
    var from by remember { mutableIntStateOf(me?.optJSONArray("mesai")?.optInt(0) ?: 9) }
    var to by remember { mutableIntStateOf(me?.optJSONArray("mesai")?.optInt(1) ?: 18) }
    val myZone = ZoneId.systemDefault()
    AppCard(padding = 8) {
        for (m in ms) {
            val tz = m.optString("tz").takeIf { it.isNotEmpty() }?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (m.optBoolean("engelli")) L("circle.blocked") else m.optString("ad").takeIf { !m.isNull("ad") } ?: L("circle.nameHidden"),
                        color = Theme.c.text, fontSize = 16.sp)
                    val sub = if (tz == null) L("group.w.none") else listOfNotNull(m.optString("sehir").takeIf { !m.isNull("sehir") && it.isNotEmpty() },
                        m.optJSONArray("mesai")?.let { w ->
                            val h = now.atZone(tz).hour
                            val a = w.getInt(0); val b = w.getInt(1)
                            L(if (if (a < b) h in a until b else h >= a || h < b) "group.w.working" else "group.w.off")
                        }).joinToString(" · ")
                    Text(sub, color = Theme.c.muted, fontSize = 13.sp)
                }
                if (tz != null) Column(horizontalAlignment = Alignment.End) {
                    Text(Fmt.time(now, tz), color = Theme.c.text, fontSize = 20.sp, style = Tnum)
                    val diff = (tz.rules.getOffset(now).totalSeconds - myZone.rules.getOffset(now).totalSeconds) / 60
                    val ad = kotlin.math.abs(diff)
                    if (diff != 0) Text((if (diff > 0) "+" else "−") + (ad / 60) + (if (ad % 60 != 0) ":%02d".format(ad % 60) else ""),
                        color = Theme.c.muted, fontSize = 12.sp, style = Tnum)
                }
            }
        }
    }
    AppCard {
        ChoiceRow(L("group.w.workFrom"), "%02d:00".format(from), (0..23).map { it to "%02d:00".format(it) }) { from = it }
        ChoiceRow(L("group.w.workTo"), "%02d:00".format(to), (1..24).map { it to "%02d:00".format(it) }) { to = it }
        RoundButton(L("group.w.share"), true, Modifier.fillMaxWidth()) {
            val p = AppPlace.current()
            onSet(JSONObject().put("sehir", p.name.take(40)).put("tz", p.zone.id).apply { if (from != to) put("mesaiBas", from).put("mesaiSon", to) })
        }
        if (me?.has("tz") == true) TextButton(onClick = { onSet(JSONObject()) }) { Text(L("group.w.remove"), color = Theme.c.err) }
    }
}

@Composable
private fun PollSection(g: JSONObject, ms: List<JSONObject>, onVote: (List<Int>) -> Unit, onClose: () -> Unit) {
    val slots = g.optJSONArray("saatler") ?: JSONArray()
    val tally = g.optJSONArray("sonuc") ?: JSONArray()
    val best = if (g.isNull("enIyi")) -1 else g.optInt("enIyi", -1)
    val closed = g.optBoolean("kapali")
    val mine = ms.firstOrNull { it.optBoolean("sen") }?.optJSONArray("oy")
    var votes by remember(g.toString()) { mutableStateOf((0 until slots.length()).map { mine?.optInt(it) ?: 0 }) }
    AppCard {
        Text(T("group.p.duration", g.optInt("sure")), color = Theme.c.muted, fontSize = 13.sp)
        for (i in 0 until slots.length()) {
            val at = Instant.ofEpochSecond(slots.getLong(i) * 60)
            val t = tally.optJSONObject(i)
            Column(Modifier.fillMaxWidth().then(if (i == best) Modifier.paper(Theme.c.accentSoft, 14) else Modifier).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateTimeText(at), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp, fontWeight = if (i == best) FontWeight.SemiBold else FontWeight.Normal)
                    Text(T("group.p.count", t?.optInt("uygun") ?: 0, t?.optInt("olabilir") ?: 0), color = Theme.c.muted, fontSize = 12.sp)
                }
                if (!closed) SoftSegmentedRow {
                    for ((v, k) in listOf(2 to "group.p.yes", 1 to "group.p.maybe", 0 to "group.p.no"))
                        SoftSegment(votes[i] == v, { votes = votes.toMutableList().also { it[i] = v } }) { Text(L(k), maxLines = 1, fontSize = 13.sp) }
                }
            }
        }
        if (best >= 0) Text(L("group.p.best") + ": " + dateTimeText(Instant.ofEpochSecond(slots.getLong(best) * 60)), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (closed) Text(L("group.p.closed"), color = Theme.c.muted, fontSize = 14.sp)
        else {
            RoundButton(L("group.p.save"), true, Modifier.fillMaxWidth()) { onVote(votes) }
            if (g.optBoolean("sahip")) TextButton(onClick = onClose) { Text(L("group.p.close"), color = Theme.c.err) }
        }
    }
}

@Composable
private fun FocusSection(g: JSONObject, ms: List<JSONObject>, onOp: (String, Int) -> Unit) {
    val now = rememberNow()
    val s = g.optJSONObject("oturum")
    AppCard {
        if (s == null) {
            Text(L("group.f.none"), color = Theme.c.muted, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (dk in listOf(25, 50)) RoundButton(T("group.f.start", dk), dk == 25, Modifier.weight(1f)) { onOp("baslat", dk) }
            }
        } else {
            val left = maxOf(0L, (s.getLong("bas") + s.getInt("dk") * 60_000L - now.toEpochMilli()) / 1000)
            Text(L("group.f.left"), color = Theme.c.muted, fontSize = 13.sp)
            Text(hms(left), color = Theme.c.text, fontSize = 40.sp, fontWeight = FontWeight.Light, style = Tnum)
            val me = ms.firstOrNull { it.optBoolean("sen") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (me?.optBoolean("katildi") != true) RoundButton(L("group.f.join"), true, Modifier.weight(1f)) { onOp("katil", 0) }
                else if (me.optBoolean("bitti") != true) RoundButton(L("group.f.done"), true, Modifier.weight(1f)) { onOp("bitti", 0) }
            }
            for (m in ms.filter { it.optBoolean("katildi") }) Text(
                (m.optString("ad").takeIf { !m.isNull("ad") } ?: L("circle.blocked")) + " · " + L(if (m.optBoolean("bitti")) "group.f.finished" else "group.f.joined"),
                color = Theme.c.text, fontSize = 14.sp)
        }
        Text(L("group.f.note"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun AlarmSection(g: JSONObject, ms: List<JSONObject>, onSend: (JSONObject) -> Unit, onAnswer: (JSONObject, Boolean) -> Unit, onReport: (JSONObject) -> Unit) {
    val context = LocalContext.current
    val reqs = g.optJSONArray("istekler")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()
    val byId = ms.associateBy { it.getLong("id") }
    fun who(id: Long?) = id?.let { byId[it] }?.let { if (it.optBoolean("engelli")) L("circle.blocked") else it.optString("ad").takeIf { _ -> !it.isNull("ad") } } ?: L("circle.formerMember")
    fun days(mask: Int) = if (mask == 0) L("group.a.once") else Groups.daysOf(mask).joinToString(" ") { L("calendar.weekdaysShort.${it - 1}") }
    var composing by remember { mutableStateOf(false) }
    val incoming = reqs.filter { it.optBoolean("gelen") }
    if (incoming.isNotEmpty()) {
        SectionTitle(L("group.a.incoming"))
        for (r in incoming) AppCard {
            Text(r.getString("saat") + "  " + days(r.optInt("gunler")), color = Theme.c.text, fontSize = 20.sp, style = Tnum)
            r.optString("etiket").takeIf { !r.isNull("etiket") && it.isNotEmpty() }?.let { Text(it, color = Theme.c.text, fontSize = 15.sp) }
            Text(T("group.a.from", who(if (r.isNull("kimden")) null else r.getLong("kimden"))), color = Theme.c.muted, fontSize = 13.sp)
            if (r.optString("durum") == "bekliyor") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundButton(L("group.a.decline"), false, Modifier.weight(1f)) { onAnswer(r, false) }
                RoundButton(L("group.a.accept"), true, Modifier.weight(1f)) { onAnswer(r, true) }
            } else Text(L(if (r.optString("durum") == "kuruldu") "group.a.set" else "group.a.declined"), color = Theme.c.muted, fontSize = 13.sp)
            if (!r.isNull("etiket")) TextButton(onClick = { onReport(r) }) { Text(L("circle.report"), color = Theme.c.muted, fontSize = 13.sp) }
        }
    }
    AppCard {
        Text(L("group.a.note"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
        if (ms.count { !it.optBoolean("sen") } > 0) RoundButton(L("group.a.send"), true, Modifier.fillMaxWidth()) { composing = true }
    }
    val outgoing = reqs.filter { !it.optBoolean("gelen") }
    if (outgoing.isNotEmpty()) {
        SectionTitle(L("group.a.outgoing"))
        AppCard(padding = 12) {
            for (r in outgoing) Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(r.getString("saat") + " · " + who(if (r.isNull("kime")) null else r.getLong("kime")), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                Text(L(when (r.optString("durum")) { "kuruldu" -> "group.a.set"; "reddedildi" -> "group.a.declined"; else -> "group.a.pending" }), color = Theme.c.muted, fontSize = 13.sp)
            }
        }
    }
    if (composing) {
        val others = ms.filter { !it.optBoolean("sen") && !it.optBoolean("engelli") }
        var to by remember { mutableStateOf(others.firstOrNull()?.getLong("id")) }
        var at by remember { mutableIntStateOf(7 * 60) }
        var mask by remember { mutableIntStateOf(0) }
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { composing = false },
            title = { Text(L("group.a.send")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceRow(L("group.a.to"), others.firstOrNull { it.getLong("id") == to }?.let { who(it.getLong("id")) } ?: "",
                        others.map { it.getLong("id").toInt() to who(it.getLong("id")) }) { to = it.toLong() }
                    Row(Modifier.fillMaxWidth().clickable {
                        android.app.TimePickerDialog(context, { _, h, m -> at = h * 60 + m }, at / 60, at % 60, !Prefs.h12).show()
                    }.padding(vertical = 8.dp)) {
                        Text(L("group.a.time"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                        Text("%02d:%02d".format(at / 60, at % 60), color = Theme.c.accent, fontSize = 15.sp, style = Tnum)
                    }
                    Text(L("group.a.days"), color = Theme.c.muted, fontSize = 13.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (d in 0..6) Box(Modifier.clip(RoundedCornerShape(10.dp)).clickable { mask = mask xor (1 shl d) }
                            .paper(if (mask and (1 shl d) != 0) Theme.c.accentSoft else Theme.c.surface2, 10).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(L("calendar.weekdaysShort.$d"), color = Theme.c.text, fontSize = 13.sp)
                        }
                    }
                    OutlinedTextField(label, { label = it.take(40) }, singleLine = true, label = { Text(L("group.a.label")) })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    composing = false
                    to?.let { onSend(JSONObject().put("uye", it).put("saat", "%02d:%02d".format(at / 60, at % 60)).put("gunler", mask).apply { if (label.isNotBlank()) put("etiket", label.trim()) }) }
                }) { Text(L("group.a.send"), color = Theme.c.accent) }
            },
            dismissButton = { TextButton(onClick = { composing = false }) { Text(L("common.cancel")) } },
        )
    }
}

@Composable
private fun WatchSection(g: JSONObject, onToggle: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val events = remember { SkyEvent.all(context).filter { it.t > now - 7 * 86_400_000L && it.t < now + 30 * 86_400_000L } }
    val counts = g.optJSONObject("sayilar") ?: JSONObject()
    val mine = g.optJSONArray("benim")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()
    AppCard(padding = 8) {
        Text(L("group.o.note"), Modifier.padding(8.dp), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
        if (events.isEmpty()) Text(L("group.o.none"), Modifier.padding(8.dp), color = Theme.c.muted, fontSize = 15.sp)
        for (e in events) {
            val key = Groups.eventKey(e)
            val on = key in mine
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onToggle(key, !on) }.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Checkbox(on, { onToggle(key, it) }, colors = CheckboxDefaults.colors(checkedColor = Theme.c.accent))
                Column(Modifier.weight(1f)) {
                    Text(e.title, color = Theme.c.text, fontSize = 15.sp)
                    Text(Fmt.longDate(Instant.ofEpochMilli(e.t).atZone(ZoneId.systemDefault()).toLocalDate()), color = Theme.c.muted, fontSize = 12.sp)
                }
                counts.optInt(key).takeIf { it > 0 }?.let { Text(T("group.o.count", it), color = Theme.c.muted, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun GroupMemberRow(g: JSONObject, m: JSONObject, onAction: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val you = m.optBoolean("sen")
    val blocked = m.optBoolean("engelli")
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = !you) { menu = true }
        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (blocked) L("circle.blocked") else m.optString("ad").takeIf { !m.isNull("ad") } ?: L("circle.nameHidden"),
                color = if (blocked || m.isNull("ad")) Theme.c.muted else Theme.c.text, fontSize = 16.sp)
            val tags = listOfNotNull(if (you) L("circle.you") else null, if (m.optBoolean("sahip")) L("circle.owner") else null)
            if (tags.isNotEmpty()) Text(tags.joinToString(" · "), color = Theme.c.muted, fontSize = 12.sp)
        }
        Box {
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text(L("circle.report")) }, { menu = false; onAction("report") })
                DropdownMenuItem({ Text(L(if (blocked) "circle.unblock" else "circle.block")) }, { menu = false; onAction(if (blocked) "unblock" else "block") })
                if (g.optBoolean("sahip")) DropdownMenuItem({ Text(L("group.kick"), color = Theme.c.err) }, { menu = false; onAction("kick") })
            }
        }
    }
}

/** Hava ekranındaki anonim hava bildirimi kartı (girişliyken; sunucuda kapalıysa görünmez) */
@Composable
fun WeatherReportCard(lat: Double, lon: Double) {
    if (!Account.signedIn) return
    val scope = rememberCoroutineScope()
    var counts by remember { mutableStateOf<JSONObject?>(null) }
    var hidden by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lat, lon) {
        val r = Groups.weatherNear(lat, lon)
        if (r.err == "closed") hidden = true
        counts = r.value?.optJSONObject("sayilar")
    }
    if (hidden) return
    AppCard {
        Text(L("wxr.title"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (k in listOf("acik", "bulutlu", "yagmur", "saganak", "kar", "dolu", "sis", "ruzgar", "firtina")) {
                Box(Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                    scope.launch {
                        val r = Groups.weatherReport(k, lat, lon)
                        if (r.value != null) { counts = r.value.optJSONObject("sayilar"); msg = "wxr.thanks" }
                        else msg = if (r.err == "account.err.tooMany") "wxr.wait" else r.err
                    }
                }.paper(Theme.c.surface2, 16).padding(horizontal = 12.dp, vertical = 7.dp)) { Text(L("wxr.k.$k"), color = Theme.c.text, fontSize = 14.sp) }
            }
        }
        msg?.let { Text(L(it), color = if (it == "wxr.thanks") Theme.c.ok else Theme.c.muted, fontSize = 13.sp) }
        val c = counts
        if (c != null && c.length() > 0) Text(L("wxr.near") + " " + c.keys().asSequence().sortedByDescending { c.getInt(it) }.joinToString(", ") { L("wxr.k.$it") + " " + c.getInt(it) },
            color = Theme.c.text, fontSize = 14.sp)
        else if (c != null) Text(L("wxr.none"), color = Theme.c.muted, fontSize = 14.sp)
        Text(L("wxr.note"), color = Theme.c.muted, fontSize = 12.sp, lineHeight = 16.sp)
    }
}
