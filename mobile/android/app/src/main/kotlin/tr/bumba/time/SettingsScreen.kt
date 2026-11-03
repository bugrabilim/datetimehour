package tr.bumba.time

import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

/**
 * Ayarlar (iOS'taki SettingsView): özellikler; konum ve bildirimler; uygulama rengi ve birimler; saat biçimi, ekran açık,
 * görünüm, saat ekranı. Araçlar kapalıyken rozetler ve hakkında da buradadır (başka yoldan açılmazlar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onRoute: (String) -> Unit) {
    // Beş başlık (2. analiz #198): Genel, Renk ve görünüm, Saat, Yedek ve kısayollar, Destek; satırda şimdiki değer,
    // uzun açıklamalar alt sayfada (penceresinde), en üstte arama
    var query by rememberSaveable { mutableStateOf("") }
    var note by remember { mutableStateOf<Pair<String, String>?>(null) }
    fun shows(vararg keys: String): Boolean {
        val q = District.fold(query.trim())
        return q.isEmpty() || keys.any { District.fold(L(it)).contains(q) }
    }
    val general = arrayOf("features.title", "settings.location", "settings.notifications", "lang.title", "settings.units",
        "settings.pressureUnit", "client.formatLabel", "settings.keepAwake")
    val look = arrayOf("settings.color", "settings.appearance", "fontScale.title")
    val clock = arrayOf("look.title", "home.customize", "dial.title", "night.title")
    val backup = arrayOf("sync.androidTitle", "android.shortcuts.title")
    val support = arrayOf("support.title", "support.rate", "support.share", "support.qr", "support.feedback", "support.moreApps",
        "support.version", "badges.h1", "settings.about")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text(L("settings.search")) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Theme.c.muted) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, L("common.done"), tint = Theme.c.muted) } },
            shape = RoundedCornerShape(14.dp),
        )
        // Genel
        if (shows(*general)) {
            SectionTitle(L("settings.color.general"), Modifier.padding(top = 6.dp))
            AppCard(padding = 4) {
                if (shows("features.title")) SettingsRow(Icons.Filled.Apps, L("features.title")) { onRoute("features") }
                if (shows("settings.location")) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBox(Icons.Filled.LocationOn)
                    Spacer(Modifier.width(14.dp))
                    Text(L("settings.location"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                    PlacePicker(size = 15.sp)
                }
                if (shows("settings.notifications")) SettingsRow(Icons.Filled.Notifications, L("settings.notifications")) { onRoute("notifications") }
                if (shows("settings.keepAwake")) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBox(Icons.Filled.LightMode)
                    Spacer(Modifier.width(14.dp))
                    Text(L("settings.keepAwake"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                    Switch(Prefs.keepAwake, Prefs::saveKeepAwake, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
            }
            // Dil (2. analiz #5): telefonun ayarlarına gitmeden
            if (shows("lang.title")) LanguageSettingsRow { onRoute("language") }
            if (shows("settings.units", "settings.pressureUnit", "client.formatLabel")) AppCard {
                if (shows("client.formatLabel")) {
                    SectionTitle(L("client.formatLabel"))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(!Prefs.h12, { Prefs.saveH12(false) }, SegmentedButtonDefaults.itemShape(0, 2), colors = segColors()) { Text(L("client.format24")) }
                        SegmentedButton(Prefs.h12, { Prefs.saveH12(true) }, SegmentedButtonDefaults.itemShape(1, 2), colors = segColors()) { Text(L("client.format12")) }
                    }
                }
                if (shows("settings.units")) {
                    SectionTitle(L("settings.units"))
                    val opts = listOf("metric", "imperial")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        opts.forEachIndexed { i, o ->
                            SegmentedButton(Prefs.units == o, { Prefs.saveUnits(o) }, SegmentedButtonDefaults.itemShape(i, opts.size), colors = segColors()) {
                                Text(L("settings.units.$o"), fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                }
                // Basınç birimi (2. analiz #106)
                if (shows("settings.pressureUnit")) {
                    SectionTitle(L("settings.pressureUnit"))
                    var pu by remember { mutableStateOf(Units.pressureUnit) }
                    val popts = listOf("auto", "hPa", "mmHg", "inHg")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        popts.forEachIndexed { i, o ->
                            SegmentedButton(pu == o, { pu = o; Prefs.store.edit().putString("units.pressure", o).apply() },
                                SegmentedButtonDefaults.itemShape(i, popts.size), colors = segColors()) {
                                Text(if (o == "auto") L("countdowns.unit.auto") else o, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        // Renk ve görünüm
        if (shows(*look)) {
            SectionTitle(L("settings.group.look"), Modifier.padding(top = 6.dp))
            if (shows("settings.color")) AppCard(padding = 4) {
                Row(
                    Modifier.fillMaxWidth().clickable { onRoute("color") }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBox(Icons.Filled.Palette)
                    Spacer(Modifier.width(14.dp))
                    Text(L("settings.color"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                    Box(Modifier.size(18.dp).background(Theme.c.accent, CircleShape))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
                }
            }
            if (shows("settings.appearance")) AppCard {
                SectionTitle(L("settings.appearance"))
                val opts = listOf("system", "light", "dark")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opts.forEachIndexed { i, o ->
                        SegmentedButton(Prefs.appearance == o, { Prefs.saveAppearance(o) }, SegmentedButtonDefaults.itemShape(i, opts.size), colors = segColors()) {
                            Text(L("settings.$o"))
                        }
                    }
                }
            }
            // Yazı boyutu (2. analiz #77): uygulamanın bütün yazıları, %100 telefonun ayarı
            if (shows("fontScale.title")) FontScaleCard()
        }
        // Saat
        if (shows(*clock)) {
            SectionTitle(L("tab.clock"), Modifier.padding(top = 6.dp))
            AppCard(padding = 4) {
                if (shows("look.title", "dial.title", "night.title")) SettingsRow(Icons.Filled.TextFields, L("look.title")) { onRoute("appearance") }
                if (shows("home.customize")) SettingsRow(Icons.Filled.Tune, L("home.customize")) { onRoute("homeItems") }
            }
        }
        // Yedek ve kısayollar: açıklamalar pencerede
        if (shows(*backup)) {
            SectionTitle(L("settings.group.backup"), Modifier.padding(top = 6.dp))
            AppCard(padding = 4) {
                // Sunucusuz yedek: telefonun Google yedeği (res/xml/data_extraction_rules.xml); hesap ve sunucu yok
                if (shows("sync.androidTitle")) SettingsRow(Icons.Filled.Backup, L("sync.androidTitle")) { note = L("sync.androidTitle") to L("sync.androidNote") }
                // Simge kısayolları, asistan/uygulamalardan zamanlayıcı-alarm, şarjda ekran koruyucu (rakip analizi #7)
                if (shows("android.shortcuts.title")) SettingsRow(Icons.Filled.Shortcut, L("android.shortcuts.title")) { note = L("android.shortcuts.title") to L("android.shortcuts.note") }
            }
        }
        // Destek: puanla, paylaş, QR kod, geri bildirim, Bumba'nın diğer ürünleri, sürüm (2. analiz #6, #81)
        if (shows(*support)) {
            SupportCard(onRoute)
            if (!Features.isOn(Tab.TOOLS)) {
                AppCard(padding = 4) {
                    SettingsRow(Icons.Filled.MilitaryTech, L("badges.h1")) { onRoute("badges") }
                    SettingsRow(Icons.Filled.Info, L("settings.about")) { onRoute("about") }
                }
            }
        }
        if (!shows(*general, *look, *clock, *backup, *support)) {
            EmptyState(Icons.Filled.Search, L("settings.noResult"), null)
        }
    }
    note?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { note = null },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { note = null }) { Text(L("common.done")) } },
            containerColor = Theme.c.surface,
        )
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon)
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
    }
}

/** Uygulama rengi: dört grupta 24 renk (genel, çocuklar, zarif, iş); altta canlı saat örneği */
@Composable
fun ColorScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Adlı atmosferler (2. analiz #78): yana kaydırmalı büyük kartlar; seçili kart ışır, köşesinde onay
        AppCard {
            SectionTitle(L("atmo.title"))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AtmosphereCard(null)
                Atmosphere.ALL.forEach { AtmosphereCard(it) }
            }
            Text(L("atmo.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
        // Duvar kâğıdı renkleri (Material You; Android 12+, 2. analiz #167)
        if (AccentPalette.hasDynamic) AppCard {
            val on = Prefs.accent == AccentPalette.DYNAMIC_ID
            Row(Modifier.fillMaxWidth().clickable { Prefs.saveAccent(AccentPalette.DYNAMIC_ID) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Swatch(AccentPalette.find(AccentPalette.DYNAMIC_ID), L("theme.dynamic"))
                Column(Modifier.weight(1f)) {
                    Text(L("theme.dynamic"), color = if (on) Theme.c.accent else Theme.c.text, fontSize = 16.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                    Text(L("theme.dynamicNote"), color = Theme.c.muted, fontSize = 12.sp)
                }
            }
        }
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

@Composable
private fun AtmosphereCard(a: Atmosphere?) {
    val c = Theme.c
    val on = (a?.id ?: "") == Atmosphere.selected
    val name = a?.let { L("atmo.${it.id}") } ?: L("atmo.none")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.size(112.dp, 150.dp)
            .shadow(if (on) 10.dp else 0.dp, shape, ambientColor = c.accent, spotColor = c.accent)
            .clip(shape)
            .then(if (a != null) Modifier.background(Brush.verticalGradient(listOf(a.top(c.dark), a.bottom(c.dark)))) else Modifier.background(AccentPalette.find(Prefs.accent).colors(c.dark).bg))
            .border(if (on) 2.dp else 1.dp, if (on) c.accent else c.border, shape)
            .clickable { Atmosphere.save(a?.id ?: "") }
            .semantics { contentDescription = name }
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(70.dp, 26.dp).background(c.surface, RoundedCornerShape(6.dp)))
            Box(Modifier.size(40.dp, 8.dp).background(c.accent, RoundedCornerShape(50)))
        }
        Text(name, Modifier.align(Alignment.BottomStart), color = c.text, fontSize = 13.sp,
            fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, maxLines = 2)
        if (on) Icon(Icons.Filled.CheckCircle, null, tint = c.accent, modifier = Modifier.align(Alignment.TopEnd).size(22.dp))
    }
}

/** Üç renkli daire: ana renk, ikinci renk ve kadran tonu */
@Composable
private fun Swatch(p: AccentPalette, label: String) {
    val dark = Theme.c.dark
    val on = Prefs.accent == p.id
    Box(
        Modifier.size(48.dp).border(2.dp, if (on) Theme.c.text.copy(alpha = 0.6f) else Color.Transparent, CircleShape).padding(4.dp)
            .clip(CircleShape).clickable { if (Prefs.accent != p.id) BadgeStore.hit(Badge.RENK); Prefs.saveAccent(p.id) }.semantics { contentDescription = label },
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

internal fun privacyUrl(): String {
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
        result?.let { Text(L(if (it) "list.ok" else "list.err"), color = if (it) Theme.c.ok else Theme.c.err, fontSize = 13.sp) }
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
