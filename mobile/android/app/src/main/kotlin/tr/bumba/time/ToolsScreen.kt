package tr.bumba.time

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Araçlar: kronometre, zamanlayıcı, tarih hesaplama; altta ayarlar ve hakkında (iOS'taki sırayla) */
@Composable
fun ToolsScreen(route: String?, onRoute: (String?) -> Unit) {
    when (route) {
        "stopwatch" -> Page(L("tools.hub.cards.stopwatch.name"), onRoute) { Stopwatch() }
        "timer" -> Page(L("tools.timer"), onRoute) { TimerTool() }
        "datecalc" -> Page(L("tools.hub.cards.datecalc.name"), onRoute) { DateCalc() }
        "settings" -> Page(L("settings.title"), onRoute) { Settings() }
        "about" -> Page(L("settings.about"), onRoute) { About() }
        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(L("tab.tools"), color = Theme.c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            AppCard(padding = 4) {
                ToolRow(Icons.Filled.Timer, L("tools.hub.cards.stopwatch.name")) { onRoute("stopwatch") }
                ToolRow(Icons.Filled.HourglassBottom, L("tools.timer")) { onRoute("timer") }
                ToolRow(Icons.Filled.DateRange, L("tools.hub.cards.datecalc.name"), last = true) { onRoute("datecalc") }
            }
            AppCard(padding = 4) {
                ToolRow(Icons.Filled.Settings, L("settings.title")) { onRoute("settings") }
                ToolRow(Icons.Filled.Info, L("settings.about"), last = true) { onRoute("about") }
            }
        }
    }
}

@Composable
private fun Page(title: String, onRoute: (String?) -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubBar(title, onBack = { onRoute(null) })
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ToolRow(icon: ImageVector, title: String, last: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Theme.c.accent)
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
    }
    if (!last) HorizontalDivider(Modifier.padding(start = 50.dp), color = Theme.c.border.copy(alpha = 0.5f))
}

@Composable
private fun BigTime(text: String) {
    Text(text, Modifier.fillMaxWidth(), color = Theme.c.text, fontSize = 60.sp, fontWeight = FontWeight.SemiBold,
        style = Tnum, textAlign = TextAlign.Center, maxLines = 1)
}

@Composable
private fun RoundButton(title: String, prominent: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (prominent) Theme.c.accent else Theme.c.surface2,
            contentColor = if (prominent) Theme.c.bg else Theme.c.text,
        ),
    ) { Text(title, fontWeight = FontWeight.SemiBold) }
}

/** Kronometre: SystemClock.elapsedRealtime ile (cihaz saati değişse etkilenmez); turlar */
@Composable
private fun Stopwatch() {
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var acc by rememberSaveable { mutableLongStateOf(0L) }
    var running by rememberSaveable { mutableStateOf(false) }
    val laps = remember { mutableStateListOf<Long>() }
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) { while (running) { tick = SystemClock.elapsedRealtime(); delay(30) } }
    val total = acc + if (running) tick.coerceAtLeast(startedAt) - startedAt else 0
    fun fmt(ms: Long) = "%02d:%02d.%02d".format(ms / 60000, (ms / 1000) % 60, (ms / 10) % 100)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        BigTime(fmt(total))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundButton(L(if (running) "tools.stopwatch.lap" else "tools.stopwatch.reset"), false, Modifier.weight(1f)) {
                if (running) laps.add(0, total) else { acc = 0; laps.clear() }
            }
            RoundButton(L(if (running) "tools.stopwatch.stop" else if (acc > 0) "tools.stopwatch.resume" else "tools.stopwatch.start"), true, Modifier.weight(1f)) {
                if (running) { acc = total; running = false } else { startedAt = SystemClock.elapsedRealtime(); tick = startedAt; running = true }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            laps.forEachIndexed { i, t ->
                val prev = laps.getOrNull(i + 1) ?: 0
                Row2("${L("tools.stopwatch.lapNo")} ${laps.size - i}", "${fmt(t - prev)}   ${fmt(t)}")
                HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            }
        }
    }
}

/** Zamanlayıcı: süre seçilir, geri sayar; bitince titreşim/ses yerine ekranda uyarı (uygulama açıkken) */
@Composable
private fun TimerTool() {
    var minutes by rememberSaveable { mutableStateOf("5") }
    var endAt by rememberSaveable { mutableLongStateOf(0L) }
    var left by remember { mutableLongStateOf(0L) }
    LaunchedEffect(endAt) {
        while (endAt > 0) {
            left = ((endAt - SystemClock.elapsedRealtime()) / 1000).coerceAtLeast(0)
            if (left == 0L) break
            delay(200)
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        if (endAt > 0) {
            BigTime(Fmt.countdown(left))
            if (left == 0L) Text(L("tools.countdown.finished"), color = Theme.c.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            RoundButton(L("tools.countdown.reset"), false, Modifier.fillMaxWidth()) { endAt = 0 }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (m in listOf(1, 5, 10, 15, 25, 30)) {
                    Box(Modifier.background(Theme.c.surface2, RoundedCornerShape(10.dp)).clickable { minutes = m.toString() }.padding(10.dp)) {
                        Text("$m", color = Theme.c.text)
                    }
                }
            }
            OutlinedTextField(
                minutes, { minutes = it.filter(Char::isDigit).take(4) },
                label = { Text(L("tools.countdown.minutes")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
            )
            RoundButton(L("tools.countdown.start"), true, Modifier.fillMaxWidth()) {
                val m = minutes.toLongOrNull() ?: 0
                if (m > 0) endAt = SystemClock.elapsedRealtime() + m * 60_000
            }
        }
    }
}

/** Tarih hesaplama: iki tarih arası gün ve hafta */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateCalc() {
    var a by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var b by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(30).toString()) }
    var picking by remember { mutableStateOf<Int?>(null) }
    val from = LocalDate.parse(a)
    val to = LocalDate.parse(b)
    val days = ChronoUnit.DAYS.between(from, to)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppCard {
            SectionTitle(L("datecalc.diffHeading"))
            Row2(L("datecalc.startLabel"), Fmt.shortDate(from), Modifier.clickable { picking = 0 }, color = Theme.c.accent)
            Row2(L("datecalc.endLabel"), Fmt.shortDate(to), Modifier.clickable { picking = 1 }, color = Theme.c.accent)
            HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            Text(T("datecalc.diffDays", days), color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(T("datecalc.diffWeeks", days / 7, days % 7), color = Theme.c.muted)
        }
    }
    picking?.let { which ->
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (if (which == 0) from else to).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        if (which == 0) a = d else b = d
                    }
                    picking = null
                }) { Text(L("common.done")) }
            },
        ) { DatePicker(state) }
    }
}

/** Ayarlar: konum, uygulama rengi, görünüm, saat biçimi, ekranı açık tutma */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Settings() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("settings.location"), Modifier.weight(1f), color = Theme.c.text)
                PlacePicker(size = 16.sp)
            }
        }
        AppCard {
            SectionTitle(L("settings.appearance"))
            val opts = listOf("system", "light", "dark")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                opts.forEachIndexed { i, o ->
                    SegmentedButton(Prefs.appearance == o, { Prefs.saveAppearance(o) }, SegmentedButtonDefaults.itemShape(i, opts.size)) {
                        Text(L("settings.$o"))
                    }
                }
            }
            SectionTitle(L("client.formatLabel"))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(!Prefs.h12, { Prefs.saveH12(false) }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(L("client.format24")) }
                SegmentedButton(Prefs.h12, { Prefs.saveH12(true) }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(L("client.format12")) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("settings.keepAwake"), Modifier.weight(1f), color = Theme.c.text)
                Switch(Prefs.keepAwake, Prefs::saveKeepAwake, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
            }
        }
        AppCard {
            SectionTitle(L("settings.color"))
            for (g in AccentPalette.GROUPS) {
                Text(L("settings.color.$g"), color = Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (p in AccentPalette.ALL.filter { it.group == g }) Swatch(p)
                }
            }
            TextButton(onClick = { Prefs.saveAccent(AccentPalette.DEFAULT_ID) }) { Text(L("world.reset"), color = Theme.c.accent) }
        }
    }
}

/** Üç renkli daire: ana renk, ikinci renk ve kadran tonu */
@Composable
private fun Swatch(p: AccentPalette) {
    val dark = Theme.c.dark
    val on = Prefs.accent == p.id
    Box(
        Modifier.size(44.dp).border(2.dp, if (on) Theme.c.text else Color.Transparent, CircleShape).padding(4.dp)
            .clickable { Prefs.saveAccent(p.id) },
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.fillMaxSize().background(Color.Transparent, CircleShape).clip(CircleShape)) {
            Box(Modifier.weight(1f).fillMaxSize().background(rgb(if (dark) p.dark else p.light)))
            Column(Modifier.weight(1f).fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().background(rgb(if (dark) p.secondDark else p.secondLight)))
                Box(Modifier.weight(1f).fillMaxWidth().background(p.face(dark)))
            }
        }
        if (on) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/** Hakkında: ad, sürüm, web sitesi, iletişim, Bumba Life rozeti, e-posta listesi ve gizlilik */
@Composable
private fun About() {
    val context = LocalContext.current
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AnalogClock(java.time.ZonedDateTime.now(), Modifier.size(64.dp))
                Text(L("app.displayName"), color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(version, color = Theme.c.muted, fontSize = 13.sp)
            }
        }
        AppCard {
            Row2(L("settings.website"), "time.bumba.tr", Modifier.clickable { open("https://time.bumba.tr") })
            Row2(L("about.contact"), "bilgi@bumbagroup.com", Modifier.clickable { open("mailto:bilgi@bumbagroup.com") })
        }
        // Sitedeki Bumba Life rozeti, iletişimin altında sağda
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text(
                L("brand.product"),
                Modifier.border(1.dp, Theme.c.border, RoundedCornerShape(8.dp)).clickable { open("https://bumbagroup.com/life") }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = Theme.c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        ListSignup()
        AppCard {
            SectionTitle(L("settings.privacy"))
            Text(L("settings.privacyNoteAndroid"), color = Theme.c.text, fontSize = 14.sp)
            Text(L("privacy.full"), Modifier.clickable { open(privacyUrl()) }, color = Theme.c.accent)
        }
    }
}

private fun privacyUrl(): String {
    val l = Strings.lang
    return "https://time.bumba.tr" + if (l == "tr") "/gizlilik/" else "/$l/privacy/"
}

/** Ortak Bumba listesine katılım (sitedeki formla aynı alanlar; onay anahtarı kapalı gelir) */
@Composable
private fun ListSignup() {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    val lang = if (Strings.lang in listOf("tr", "en")) Strings.lang else "en"
    AppCard {
        SectionTitle(L("list.heading"))
        Text(L("list.lead"), color = Theme.c.text, fontSize = 14.sp)
        OutlinedTextField(email, { email = it.trim() }, Modifier.fillMaxWidth(), label = { Text(L("list.emailLabel")) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("list.consentText"), Modifier.weight(1f), color = Theme.c.text, fontSize = 13.sp)
            Switch(consent, { consent = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
        }
        Text(L("list.readNote"), Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bumbagroup.com/liste/aydinlatma" + if (lang == "tr") "" else "?dil=en")))
        }, color = Theme.c.accent, fontSize = 13.sp)
        RoundButton(L("list.submit"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            if (!consent || !email.contains("@") || sending) return@RoundButton
            sending = true
            scope.launch {
                result = withContext(Dispatchers.IO) { joinList(email, lang) }
                sending = false
                if (result == true) { email = ""; consent = false }
            }
        }
        result?.let { Text(L(if (it) "list.ok" else "list.err"), color = if (it) Theme.c.accent else Color(0xFFD32F2F), fontSize = 13.sp) }
        Text(L("list.popupText"), color = Theme.c.muted, fontSize = 11.sp)
    }
}

/** Servis sonuçta dönüş adresine yönlendirir: "#liste-tamam" ya da "#liste-hata" */
private fun joinList(email: String, lang: String): Boolean = runCatching {
    val source = "https://time.bumba.tr/android"
    val body = mapOf("site" to "saat-tarih", "eposta" to email, "dil" to lang, "riza" to "on",
        "kaynak" to source, "donus" to source, "web_sitesi" to "")
        .entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
    val c = URL("https://bumbagroup.com/api/liste/katil").openConnection() as HttpURLConnection
    c.instanceFollowRedirects = false
    c.requestMethod = "POST"
    c.doOutput = true
    c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    c.outputStream.use { it.write(body.toByteArray()) }
    c.responseCode
    val ok = (c.getHeaderField("Location") ?: "").endsWith("#liste-tamam")
    c.disconnect()
    ok
}.getOrDefault(false)
