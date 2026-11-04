package tr.bumba.time

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Tümü" sayfası (Seçenek B, 2. analiz #195–#197; iOS AllView ile aynı kimlikler ve kayıtlar):
 * üstte genel arama (araç, ayar, içerik), 4 karelik sık kullanılanlar (uzun basınca ekle/çıkar; boşsa son kullanılanlar),
 * sonra kategoriler (Süre ve alarm, Gün ve plan, Gök ve dünya, İbadet), sonda Rozetler. Ayarlar sağ üstteki dişliden.
 * Yalnız açık özelliklerin girişleri görünür. Kayıt: `all.favs`, `all.recent` (virgülle kimlik).
 */
enum class AllCat(val key: String) { TIME("all.cat.time"), PLAN("all.cat.plan"), SKY("all.cat.sky"), FAITH("worship.title"), MORE("tab.more") }

/**
 * Bir giriş: [tab] doluysa o özellik sekmesi açılır (çubukta değilse geri düğmeli sayfa), değilse Araçlar yolu [route].
 * [feature] kapalıysa giriş gösterilmez.
 */
data class AllEntry(
    val id: String, val titleKey: String, val icon: ImageVector, val cat: AllCat,
    val feature: Tab?, val tab: Tab? = null, val route: String? = null, val words: String = "",
) {
    val title: String get() = L(titleKey)
}

object AllPrefs {
    const val FAVS = "all.favs"
    const val RECENT = "all.recent"
    const val MAX = 4

    val entries: List<AllEntry> = listOf(
        AllEntry("alarm", "tools.hub.cards.alarm.name", Icons.Filled.Alarm, AllCat.TIME, Tab.TOOLS, route = "alarm"),
        AllEntry("timer", "tools.timer", Icons.Filled.HourglassBottom, AllCat.TIME, Tab.TOOLS, route = "timer", words = "countdowns.heading hiit.tab"),
        AllEntry("stopwatch", "tools.hub.cards.stopwatch.name", Icons.Filled.Timer, AllCat.TIME, Tab.TOOLS, route = "stopwatch"),
        AllEntry("pomodoro", "tools.hub.cards.pomodoro.name", Icons.Filled.Spa, AllCat.TIME, Tab.TOOLS, route = "pomodoro"),
        AllEntry("study", "study.title", Icons.Filled.School, AllCat.TIME, Tab.TOOLS, route = "study"),
        AllEntry("sleep", "sleep.title", Icons.Filled.Bedtime, AllCat.TIME, Tab.TOOLS, route = "sleep"),
        AllEntry("wakeGame", "wakegame.title", Icons.Filled.EmojiNature, AllCat.TIME, Tab.TOOLS, route = "wakeGame"),
        AllEntry("breath", "breath.title", Icons.Filled.Air, AllCat.TIME, Tab.TOOLS, route = "breath"),
        // Sitedeki araçlar (4 Kasım 2026)
        AllEntry("chess", "chess.h1", Icons.Filled.Games, AllCat.TIME, Tab.TOOLS, route = "chess"),
        AllEntry("metronome", "metronome.h1", Icons.Filled.MusicNote, AllCat.TIME, Tab.TOOLS, route = "metronome", words = "metronome.bpm"),
        AllEntry("talking", "talking.h1", Icons.Filled.RecordVoiceOver, AllCat.TIME, Tab.TOOLS, route = "talking"),
        AllEntry("exam", "examTimer.h1", Icons.Filled.Quiz, AllCat.TIME, Tab.TOOLS, route = "exam"),
        AllEntry("classroom", "classroom.h1", Icons.Filled.EmojiPeople, AllCat.TIME, Tab.TOOLS, route = "classroom",
            words = "classroom.tabPicker classroom.tabTeams classroom.tabCounter"),
        AllEntry("timecalc", "timeCalc.h1", Icons.Filled.MoreTime, AllCat.TIME, Tab.TOOLS, route = "timecalc"),
        AllEntry("calendar", "tab.calendar", Icons.Filled.CalendarMonth, AllCat.PLAN, Tab.CALENDAR, tab = Tab.CALENDAR),
        AllEntry("leaf", "leaf.title", Icons.Filled.Article, AllCat.PLAN, Tab.CALENDAR, route = "leaf"),
        AllEntry("datecalc", "tools.hub.cards.datecalc.name", Icons.Filled.EditCalendar, AllCat.PLAN, Tab.TOOLS, route = "datecalc"),
        AllEntry("planner", "planner.h1", Icons.Filled.Groups, AllCat.PLAN, Tab.TOOLS, route = "planner"),
        AllEntry("journal", "journal.title", Icons.Filled.WbTwilight, AllCat.PLAN, Tab.TOOLS, route = "journal"),
        AllEntry("bridge", "bridge.h1", Icons.Filled.BeachAccess, AllCat.PLAN, Tab.TOOLS, route = "bridge"),
        AllEntry("retire", "retire.h1", Icons.Filled.Elderly, AllCat.PLAN, Tab.TOOLS, route = "retire"),
        AllEntry("weather", "tab.weather", Icons.Filled.WbSunny, AllCat.SKY, Tab.WEATHER, tab = Tab.WEATHER),
        AllEntry("world", "tab.world", Icons.Filled.Public, AllCat.SKY, Tab.WORLD, tab = Tab.WORLD),
        AllEntry("seasons", "tab.seasons", Icons.Filled.Eco, AllCat.SKY, Tab.SEASONS, tab = Tab.SEASONS),
        AllEntry("moon", "moon.calendarLink", Icons.Filled.Brightness3, AllCat.SKY, Tab.SEASONS, route = "moon"),
        AllEntry("sky", "sky.h1", Icons.Filled.AutoAwesome, AllCat.SKY, Tab.SEASONS, route = "sky", words = "sky.eventsHeading sky.constellations sky.tonightHeading"),
        AllEntry("sunpath", "sunpath.title", Icons.Filled.WbTwilight, AllCat.SKY, Tab.SEASONS, route = "sunpath"),
        AllEntry("zodiac", "zodiac.h1", Icons.Filled.Stars, AllCat.SKY, Tab.TOOLS, route = "zodiac"),
        AllEntry("prayer", "tab.prayer", Icons.Filled.DarkMode, AllCat.FAITH, Tab.PRAYER, tab = Tab.PRAYER),
        AllEntry("qibla", "qibla.title", Icons.Filled.Explore, AllCat.FAITH, Tab.PRAYER, route = "qibla"),
        AllEntry("quran", "quran.title", Icons.AutoMirrored.Filled.MenuBook, AllCat.FAITH, Tab.PRAYER, route = "quran"),
        AllEntry("zikir", "zikir.title", Icons.Filled.TouchApp, AllCat.FAITH, Tab.PRAYER, route = "zikir"),
        AllEntry("track", "track.title", Icons.Filled.TaskAlt, AllCat.FAITH, Tab.PRAYER, route = "track"),
        AllEntry("duas", "dua.title", Icons.Filled.VolunteerActivism, AllCat.FAITH, Tab.PRAYER, route = "duas"),
        AllEntry("esma", "esma.title", Icons.Filled.AutoAwesome, AllCat.FAITH, Tab.PRAYER, route = "esma"),
        AllEntry("hadith", "hadith.title", Icons.Filled.FormatQuote, AllCat.FAITH, Tab.PRAYER, route = "hadith"),
        AllEntry("guide", "guide.title", Icons.Filled.SelfImprovement, AllCat.FAITH, Tab.PRAYER, route = "guide"),
        AllEntry("reldays", "relday.title", Icons.Filled.Event, AllCat.FAITH, Tab.PRAYER, route = "reldays"),
        AllEntry("greet", "greet.title", Icons.Filled.Celebration, AllCat.FAITH, Tab.PRAYER, route = "greet"),
        AllEntry("zakat", "zakat.title", Icons.Filled.Calculate, AllCat.FAITH, Tab.PRAYER, route = "zakat"),
        AllEntry("settings", "settings.title", Icons.Filled.Settings, AllCat.MORE, null, route = "settings",
            words = "settings.color lang.title settings.notifications"),
        AllEntry("badges", "badges.h1", Icons.Filled.MilitaryTech, AllCat.MORE, null, route = "badges"),
    )

    /** Aramada ayar sayfaları da çıkar (Ayarlar'ın alt sayfaları) */
    val settings: List<AllEntry> = listOf(
        AllEntry("s:settings", "settings.title", Icons.Filled.Settings, AllCat.MORE, null, route = "settings"),
        AllEntry("s:appearance", "look.title", Icons.Filled.Watch, AllCat.MORE, null, route = "appearance"),
        AllEntry("s:color", "settings.color", Icons.Filled.Palette, AllCat.MORE, null, route = "color"),
        AllEntry("s:dials", "dial.title", Icons.Filled.ColorLens, AllCat.MORE, null, route = "dials"),
        AllEntry("s:widgets", "widgets.gallery", Icons.Filled.Widgets, AllCat.MORE, null, route = "widgets"),
        AllEntry("s:homeItems", "home.customize", Icons.Filled.Apps, AllCat.MORE, null, route = "homeItems"),
        AllEntry("s:features", "features.title", Icons.Filled.Apps, AllCat.MORE, null, route = "features", words = "features.barTitle"),
        AllEntry("s:notifications", "settings.notifications", Icons.Filled.Notifications, AllCat.MORE, null, route = "notifications"),
        AllEntry("s:language", "lang.title", Icons.Filled.Language, AllCat.MORE, null, route = "language"),
        AllEntry("s:about", "settings.about", Icons.Filled.Info, AllCat.MORE, null, route = "about"),
        AllEntry("s:shareQr", "support.qr", Icons.Filled.QrCode2, AllCat.MORE, null, route = "shareQr"),
    )

    /** Emeklilik hesabı yalnız Türkçe arayüzde (Türkiye mevzuatı, metni Türkçe) */
    fun visible(e: AllEntry) = (e.feature == null || Features.isOn(e.feature)) && (e.id != "retire" || Strings.lang == "tr")

    fun byId(id: String) = (entries + settings).firstOrNull { it.id == id }

    private fun list(key: String) = Prefs.store.getString(key, "")!!.split(",").filter { it.isNotEmpty() }

    var favs by mutableStateOf(list(FAVS))
        private set
    var recent by mutableStateOf(list(RECENT))
        private set

    fun reload() { favs = list(FAVS); recent = list(RECENT) }

    /** Ekler ya da çıkarır; 4 doluysa null döner */
    fun toggleFav(id: String): Boolean? {
        val v = if (id in favs) favs - id else if (favs.size >= MAX) return null else favs + id
        favs = v
        Prefs.store.edit().putString(FAVS, v.joinToString(",")).apply()
        return id in v
    }

    fun used(id: String) {
        val v = (listOf(id) + recent.filter { it != id }).take(MAX)
        recent = v
        Prefs.store.edit().putString(RECENT, v.joinToString(",")).apply()
    }

    /** Arama: ad, kategori ve ek sözcükler; Türkçe ve diğer dillerde aksan/büyük harf duyarsız (District.fold) */
    fun search(q: String, extra: List<Pair<AllEntry, String>> = emptyList()): List<Pair<AllEntry, String>> {
        val f = District.fold(q.trim())
        if (f.isEmpty()) return emptyList()
        val words = f.split(" ").filter { it.isNotEmpty() }
        fun hit(text: String) = District.fold(text).let { t -> words.all { it in t } }
        val out = mutableListOf<Pair<AllEntry, String>>()
        for (e in entries.filter { visible(it) } + settings) {
            val hay = e.title + " " + L(e.cat.key) + " " + e.words.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { L(it) }
            if (hit(hay)) out += e to L(e.cat.key)
        }
        for ((e, label) in extra) if (hit(label)) out += e to label
        return out
    }

    /** Bir girişi açar: özellik sekmesi ya da Araçlar yolu; son kullanılanlara yazılır */
    fun open(e: AllEntry, onRoute: (String?) -> Unit, route: String? = null) {
        if (!e.id.startsWith("s:")) used(e.id)
        when {
            e.tab != null -> Nav.tab = e.tab
            else -> onRoute(route ?: e.route)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllScreen(onRoute: (String?) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var query by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) { if (toast != null) { kotlinx.coroutines.delay(2200); toast = null } }
    fun longPress(e: AllEntry) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        toast = when (AllPrefs.toggleFav(e.id)) { true -> L("all.favAdded"); false -> L("all.favRemoved"); null -> L("all.favFull") }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { PageTitle(L("tab.all")) }
                IconButton(onClick = { onRoute("settings") }) { Icon(Icons.Filled.Settings, L("settings.title"), tint = Theme.c.accent) }
            }
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp), singleLine = true,
                placeholder = { Text(L("all.search"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Theme.c.muted) },
                trailingIcon = if (query.isNotEmpty()) { { IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, L("all.clear"), tint = Theme.c.muted) } } } else null,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Theme.c.accent, unfocusedBorderColor = Theme.c.border),
            )
            if (query.isNotBlank()) {
                // İçerik: sure adları (Kur'an açılınca o sure)
                val extra = remember(query) {
                    if (!Features.isOn(Tab.PRAYER)) emptyList() else runCatching {
                        Quran.load(context)
                        val q = AllPrefs.byId("quran")!!
                        Quran.suras.mapIndexed { i, s -> q.copy(id = "quran", route = "quran:${i + 1}") to "${i + 1}. ${s.title}" }
                    }.getOrDefault(emptyList())
                }
                val results = AllPrefs.search(query, extra)
                if (results.isEmpty()) EmptyState(Icons.Filled.Search, L("settings.noResult"), null)
                AppCard(padding = 4) {
                    for ((i, r) in results.take(40).withIndex()) {
                        val (e, sub) = r
                        Row(
                            Modifier.fillMaxWidth().combinedClickable(onClick = { AllPrefs.open(e, onRoute, e.route) }, onLongClick = { if (!e.id.startsWith("s:")) longPress(e) })
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBox(e.icon)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (e.route?.startsWith("quran:") == true) sub else e.title, color = Theme.c.text, fontSize = 16.sp)
                                Text(if (e.route?.startsWith("quran:") == true) e.title else sub, color = Theme.c.muted, fontSize = 12.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
                        }
                        if (i < results.size - 1) androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 60.dp), color = Theme.c.border.copy(alpha = 0.5f))
                    }
                }
                return@Column
            }
            // Sık kullanılanlar; boşsa son kullanılanlar
            val favs = AllPrefs.favs.mapNotNull { AllPrefs.byId(it) }.filter { AllPrefs.visible(it) }
            val recent = AllPrefs.recent.mapNotNull { AllPrefs.byId(it) }.filter { AllPrefs.visible(it) }
            val top = favs.ifEmpty { recent }
            SectionTitle(if (favs.isNotEmpty() || recent.isEmpty()) L("all.favs") else L("all.recent"))
            if (top.isEmpty()) Text(L("all.favHint"), color = Theme.c.muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
            else {
                TileGrid(top, columns = 4, fav = favs.isNotEmpty(), onOpen = { AllPrefs.open(it, onRoute) }, onLong = { longPress(it) })
                if (favs.isEmpty()) Text(L("all.favHint"), color = Theme.c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            for (cat in AllCat.entries) {
                val list = AllPrefs.entries.filter { it.cat == cat && AllPrefs.visible(it) }
                if (list.isEmpty()) continue
                Spacer(Modifier.height(10.dp))
                SectionTitle(L(cat.key))
                TileGrid(list, columns = 4, fav = false, onOpen = { AllPrefs.open(it, onRoute) }, onLong = { longPress(it) })
            }
            Spacer(Modifier.height(14.dp))
            Text(
                L("all.editFeatures") + " ›", color = Theme.c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).combinedClickable(onClick = { onRoute("features") }).padding(vertical = 8.dp),
            )
        }
        toast?.let {
            Text(
                it, color = Theme.c.bg, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).background(Theme.c.text.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
}

/** Kategorideki kareler: simge kutusu + ad; uzun basınca sık kullanılanlara eklenir/çıkar */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileGrid(list: List<AllEntry>, columns: Int, fav: Boolean, onOpen: (AllEntry) -> Unit, onLong: (AllEntry) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Tablette daha çok sütun
        val cols = if (maxWidth >= 560.dp) columns + 2 else columns
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in list.chunked(cols)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (e in row) {
                        Column(
                            Modifier.weight(1f).heightIn(min = 92.dp).clip(RoundedCornerShape(14.dp)).background(Theme.c.surface)
                                .combinedClickable(onClickLabel = e.title, onClick = { onOpen(e) }, onLongClick = { onLong(e) })
                                .padding(horizontal = 4.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box {
                                IconBox(e.icon)
                                if (fav) Icon(Icons.Filled.Star, null, tint = Theme.c.accent, modifier = Modifier.size(12.dp).align(Alignment.TopEnd))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(e.title, color = Theme.c.text, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2,
                                overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                        }
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
