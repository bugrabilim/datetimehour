package tr.bumba.time

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Ayarlar (iOS'taki SettingsView): konum; uygulama rengi ve birimler; saat biçimi, ekran açık, görünüm, saat ekranı */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onRoute: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("settings.location"), Modifier.weight(1f), color = Theme.c.text)
                PlacePicker(size = 16.sp)
            }
        }
        AppCard(padding = 4) {
            Row(
                Modifier.fillMaxWidth().clickable { onRoute("color") }.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Palette, null, tint = Theme.c.accent)
                Spacer(Modifier.width(14.dp))
                Text(L("settings.color"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                Box(Modifier.size(18.dp).background(Theme.c.accent, CircleShape))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
            }
            HorizontalDivider(Modifier.padding(start = 50.dp), color = Theme.c.border.copy(alpha = 0.5f))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(L("settings.units"))
                val opts = listOf("metric", "imperial")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opts.forEachIndexed { i, o ->
                        SegmentedButton(Prefs.units == o, { Prefs.saveUnits(o) }, SegmentedButtonDefaults.itemShape(i, opts.size)) {
                            Text(L("settings.units.$o"), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        AppCard {
            SectionTitle(L("client.formatLabel"))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(!Prefs.h12, { Prefs.saveH12(false) }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(L("client.format24")) }
                SegmentedButton(Prefs.h12, { Prefs.saveH12(true) }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(L("client.format12")) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("settings.keepAwake"), Modifier.weight(1f), color = Theme.c.text)
                Switch(Prefs.keepAwake, Prefs::saveKeepAwake, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
            }
            SectionTitle(L("settings.appearance"))
            val opts = listOf("system", "light", "dark")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                opts.forEachIndexed { i, o ->
                    SegmentedButton(Prefs.appearance == o, { Prefs.saveAppearance(o) }, SegmentedButtonDefaults.itemShape(i, opts.size)) {
                        Text(L("settings.$o"))
                    }
                }
            }
        }
        AppCard(padding = 4) {
            Row(
                Modifier.fillMaxWidth().clickable { onRoute("homeItems") }.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Tune, null, tint = Theme.c.accent)
                Spacer(Modifier.width(14.dp))
                Text(L("home.customize"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
            }
        }
    }
}

/** Uygulama rengi: dört grupta 24 renk (genel, çocuklar, zarif, iş); altta canlı saat örneği */
@Composable
fun ColorScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (g in AccentPalette.GROUPS) {
            AppCard {
                SectionTitle(L("settings.color.$g"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AccentPalette.ALL.filter { it.group == g }.forEachIndexed { i, p ->
                        Swatch(p, L("settings.color.$g") + " ${i + 1}")
                    }
                }
            }
        }
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalogClock(rememberNow().atZone(java.time.ZoneId.systemDefault()), Modifier.size(64.dp))
                Text(L("app.displayName"), Modifier.weight(1f), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                Text(
                    L("settings.color.sample"),
                    Modifier.background(Theme.c.accent, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Theme.c.bg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        TextButton(onClick = { Prefs.saveAccent(AccentPalette.DEFAULT_ID) }) { Text(L("world.reset"), color = Theme.c.accent) }
    }
}

/** Üç renkli daire: ana renk, ikinci renk ve kadran tonu */
@Composable
private fun Swatch(p: AccentPalette, label: String) {
    val dark = Theme.c.dark
    val on = Prefs.accent == p.id
    Box(
        Modifier.size(48.dp).border(2.dp, if (on) Theme.c.text.copy(alpha = 0.6f) else Color.Transparent, CircleShape).padding(4.dp)
            .clip(CircleShape).clickable { Prefs.saveAccent(p.id) }.semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxSize().background(rgb(if (dark) p.dark else p.light)))
            Column(Modifier.weight(1f).fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().background(rgb(if (dark) p.secondDark else p.secondLight)))
                Box(Modifier.weight(1f).fillMaxWidth().background(p.face(dark)))
            }
        }
        if (on) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Hakkında: ad, sürüm, web sitesi, iletişim; iletişimin altında sağda Bumba Life rozeti; e-posta listesi ve gizlilik */
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    val version = runCatching {
        val i = context.packageManager.getPackageInfo(context.packageName, 0)
        "${i.versionName} (${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(i)})"
    }.getOrDefault("")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Schedule, null, tint = Theme.c.accent, modifier = Modifier.size(48.dp))
                Text(L("app.displayName"), color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(version, color = Theme.c.muted, fontSize = 13.sp)
            }
        }
        AppCard {
            Row2(L("settings.website"), "time.bumba.tr", Modifier.clickable { open("https://time.bumba.tr") })
            Row2(L("about.contact"), "bilgi@bumbagroup.com", Modifier.clickable { open("mailto:bilgi@bumbagroup.com") })
        }
        // Sitedeki Bumba Life rozeti (açık/koyu), iletişimin hemen altında sağda
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Image(
                painterResource(if (Theme.c.dark) R.drawable.bumba_life_dark else R.drawable.bumba_life),
                contentDescription = L("brand.product"),
                modifier = Modifier.size(164.dp, 28.dp).clickable { open("https://bumbagroup.com/life") },
            )
        }
        ListSignup()
        // Gizlilik açık hâlde Hakkında'nın altında
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
