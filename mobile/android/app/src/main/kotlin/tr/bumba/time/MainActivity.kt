package tr.bumba.time

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    /** Android 13 öncesinde uygulama içi dil (AppLanguage); sonrasında sistemin uygulama dili */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ekran görüntüsü için: adb shell am start … --ei startTab 4
        val start = intent.getIntExtra("startTab", -1)
        intent.getStringExtra("tool")?.let { Nav.toolsRoute = it }
        Nav.handleShortcut(intent)
        setContent {
            AppTheme { Root(start) }
        }
    }

    /** Widget'tan ya da bildirimden açılınca istenen sekme */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        Tab.entries.getOrNull(intent.getIntExtra("startTab", -1))?.let { Nav.tab = it; Nav.toolsRoute = intent.getStringExtra("tool") }
        Nav.handleShortcut(intent)
    }

    /** Uygulama öne gelince rozetlerin gün serisi (yalnız son gün ve gün sayısı) */
    override fun onResume() {
        super.onResume()
        BadgeStore.appOpened()
        AccentPalette.initDynamic(this) // duvar kâğıdı değişmiş olabilir (Material You)
        // Aralık antrenmanı sürüyorsa sesli duyuru hizmeti çalışsın (uygulama zorla kapatılmış olabilir)
        HiitService.sync(this)
        // Biten antrenman Health Connect'e (açıksa ve daha önce yazılmadıysa)
        HealthLog.recordHiitIfDone(this)
        // Odakta engelleme: pomodoro sürüyorsa izleme hizmeti çalışsın, bittiyse dursun (#98)
        FocusBlock.sync(this)
    }

    /** Uygulamadan çıkarken widget'lar güncel konum ve dille yeniden çizilir */
    override fun onStop() {
        super.onStop()
        WidgetUpdater.updateAll(this)
    }
}

/**
 * Sekmeler (iOS'taki sırayla): Saat, Hava, Takvim, Dünya, Mevsimler, Vakitler, Araçlar.
 * Seçili sekmeye yeniden dokununca Araçlar'da açık sayfadan listeye dönülür.
 */
enum class Tab(val key: String, val on: ImageVector, val off: ImageVector) {
    CLOCK("tab.clock", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    WEATHER("tab.weather", Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
    CALENDAR("tab.calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    // Çizgi hâlleri dolu göründüğü için seçili değilken farklı çizgi simgeler (2. analiz #95)
    WORLD("tab.world", Icons.Filled.Public, Icons.Outlined.Language),
    SEASONS("tab.seasons", Icons.Filled.Eco, Icons.Outlined.Eco),
    PRAYER("tab.prayer", Icons.Filled.DarkMode, Icons.Outlined.DarkMode),
    TOOLS("tab.tools", Icons.Filled.Apps, Icons.Outlined.Apps),
}

/**
 * Sekme ve Araçlar gezintisi (iOS'taki Router): saat ekranındaki alanlar dokununca ilgili sayfayı açar.
 * Araçlar yolları: alarm, timer, stopwatch, pomodoro, datecalc, settings, color, homeItems, about.
 */
object Nav {
    var tab by mutableStateOf(Tab.CLOCK)
    var toolsRoute by mutableStateOf<String?>(null)
    /** Takvim sekmesi bu günü açar (saat ekranından) */
    var calendarDay by mutableStateOf<LocalDate?>(null)
    /** Saat ekranındaki dişliden açılan ayarlar (alt bar ve Araçlar'dan bağımsız; "settings", "features"…) */
    var overlay by mutableStateOf<String?>(null)
    /** Simge kısayolu "Tam ekran saat": saat sekmesi tam ekran açılır */
    var openFullscreen by mutableStateOf(false)

    /**
     * Simge kısayolları (res/xml/shortcuts.xml, "shortcut" ekstrası): fullscreen, timer, alarm, prayer.
     * "night" doğruysa gece modu açılır (şarj otomasyonu gibi).
     */
    fun handleShortcut(intent: android.content.Intent?) {
        // Paylaşılan geri sayım bağlantısı (2. analiz #138): listeye eklenir, geri sayımlar açılır
        if (CountdownImport.handle(intent)) { overlay = null; tool("timer"); return }
        if (intent?.getBooleanExtra("night", false) == true) Prefs.saveNightMode("on")
        when (intent?.getStringExtra("shortcut")) {
            "fullscreen" -> { overlay = null; tab = Tab.CLOCK; openFullscreen = true }
            "timer" -> { overlay = null; tool("timer") }
            "alarm" -> { overlay = null; tool("alarm") }
            "stopwatch" -> { overlay = null; tool("stopwatch") }
            "pomodoro" -> { overlay = null; tool("pomodoro") }
            "prayer" -> { overlay = null; tab = Tab.PRAYER }
        }
    }

    fun tool(route: String) {
        toolsRoute = route
        tab = Tab.TOOLS
    }
}

@Composable
fun Root(start: Int) {
    // Ekran görüntüsü ya da bildirimden: istenen sekmede açılır
    // (Araçlar'da bildirimden gelen sayfa, ör. aralık antrenmanı, onCreate'te Nav.toolsRoute'a yazılmıştır)
    remember(start) { Tab.entries.getOrNull(start)?.let { Nav.tab = it; if (it != Tab.TOOLS) Nav.toolsRoute = null } }
    val tab = Nav.tab
    val toolsRoute = Nav.toolsRoute
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    // Yan çevirince (30 Ekim 2026, sahibin isteği): saat sekmesinde tam ekran açılır, dikeye dönünce kapanır;
    // diğer sekmelerde alt bar ve sistem çubukları gizlenir ki içerik sıkışmasın
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var autoFull by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(landscape, tab) {
        if (landscape && tab == Tab.CLOCK && !fullscreen) {
            fullscreen = true
            autoFull = true
        } else if (!landscape && autoFull) {
            fullscreen = false
            autoFull = false
        }
    }
    // Yeni kurulumda ilk açılış sihirbazı (2. analiz #4); bitince ana ekran
    if (Onboarding.pending) {
        BackHandler { Onboarding.finish() }
        OnboardingScreen { Onboarding.finish() }
        return
    }
    RefreshAutoPlace()
    AskNotificationsOnce()
    // Simge kısayolundan "Tam ekran saat"
    LaunchedEffect(Nav.openFullscreen) {
        if (Nav.openFullscreen) {
            Nav.openFullscreen = false
            fullscreen = true
        }
    }

    // Saat ekranında ekran kapanmaz (Ayarlar'dan kapatılabilir); tam ekranda her zaman açık
    LocalView.current.keepScreenOn = fullscreen || (tab == Tab.CLOCK && Prefs.keepAwake)

    if (fullscreen) {
        BackHandler { fullscreen = false; autoFull = false }
        Box(Modifier.fillMaxSize()) {
            FullscreenClock { fullscreen = false; autoFull = false }
            BadgeToast(Modifier.align(Alignment.TopCenter).statusBarsPadding())
        }
        return
    }
    // Ayarlar katmanı: geri gelince alt sayfadan ayarlara, ayarlardan saat ekranına dönülür
    Nav.overlay?.let { route ->
        val back: (String?) -> Unit = { r -> Nav.overlay = r ?: if (route == "settings" || route == "features") null else "settings" }
        BackHandler { back(null) }
        Box(Modifier.fillMaxSize().appBackdrop(Theme.c).statusBarsPadding().navigationBarsPadding().windowInsetsPadding(WindowInsets.displayCutout)) {
            ToolsScreen(route, back)
            BadgeToast(Modifier.align(Alignment.TopCenter))
        }
        return
    }
    // Alt barda yalnız açık özellikler; kapalı bir özellik açılırsa bar değişmez, sayfa geri düğmesiyle açılır (2. analiz #203)
    val tabs = Features.barTabs()
    val offBar = tab !in tabs
    var backTab by remember { mutableStateOf(Tab.CLOCK) }
    LaunchedEffect(tab) { if (!offBar) backTab = tab }
    BackHandler(enabled = tab == Tab.TOOLS && toolsRoute != null) { Nav.toolsRoute = null }
    BackHandler(enabled = tab != Tab.CLOCK && !(tab == Tab.TOOLS && toolsRoute != null)) { Nav.tab = if (offBar) backTab else Tab.CLOCK }

    if (landscape) HideSystemBars()
    // Hava neyse ekran öyle (2. analiz #193): seçilirse saat ekranının zemini seçili yerin havası, yazılar koyu temanın renkleri
    val clockWx = tab == Tab.CLOCK && Prefs.store.getBoolean(WeatherScene.CLOCK_KEY, false)
    Box(Modifier.fillMaxSize()) {
    if (clockWx) AnimatedBackground(FullscreenBackground.WEATHER, Modifier.fillMaxSize())
    CompositionLocalProvider(LocalColors provides if (clockWx) AccentPalette.find(Prefs.accent).colors(true) else Theme.c) {
    Column(Modifier.fillMaxSize().then(if (clockWx) Modifier else Modifier.appBackdrop(Theme.c))) {
        Column(Modifier.weight(1f).fillMaxWidth().statusBarsPadding().windowInsetsPadding(WindowInsets.displayCutout)) {
        if (offBar && !(tab == Tab.TOOLS && toolsRoute != null)) BackRow(backTab) { Nav.tab = backTab }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                Tab.CLOCK -> ClockScreen(
                    onFullscreen = { fullscreen = true; BadgeStore.hit(Badge.TAM_EKRAN) }, onOpen = { Nav.tab = it },
                    onSettings = { Nav.overlay = it },
                )
                Tab.WEATHER -> WeatherScreen()
                Tab.CALENDAR -> CalendarScreen()
                Tab.WORLD -> WorldScreen()
                Tab.SEASONS -> SeasonsScreen()
                Tab.PRAYER -> PrayerScreen()
                Tab.TOOLS -> ToolsScreen(toolsRoute) { Nav.toolsRoute = it }
            }
        }
        }
        // Yalnız Saat açıksa bar yok
        if (!landscape && tabs.size > 1) {
            TabBar(tabs, tab) { t ->
                if (t == tab && t == Tab.TOOLS) Nav.toolsRoute = null
                Nav.tab = t
            }
        }
    }
    }
    BadgeToast(Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

@Composable
private fun TabBar(tabs: List<Tab>, selected: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Theme.c.surface).navigationBarsPadding()) {
        HorizontalDivider(color = Theme.c.border.copy(alpha = 0.6f))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (t in tabs) {
                val on = t == selected
                val color = if (on) Theme.c.accent else Theme.c.muted
                Column(
                    Modifier.weight(1f).clickable { onSelect(t) }.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Seçili sekmede simgenin arkasında ana rengin açık tonunda hap (sitedeki .nav gibi; 2. analiz #95)
                    Box(
                        Modifier.size(width = 56.dp, height = 30.dp).clip(RoundedCornerShape(15.dp))
                            .background(if (on) Theme.c.accentSoft else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (on) t.on else t.off, contentDescription = Features.label(t), tint = color, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        Features.label(t), color = color, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/** Alt barda olmayan bir sayfa açıldığında üstte geri düğmesi: döneceği sekmenin adıyla (2. analiz #203) */
@Composable
private fun BackRow(to: Tab, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = Features.label(to)) { onBack() }.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Theme.c.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(Features.label(to), color = Theme.c.accent, fontSize = 16.sp)
    }
}
