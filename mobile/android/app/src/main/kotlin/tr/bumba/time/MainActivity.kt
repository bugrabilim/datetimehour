package tr.bumba.time

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ekran görüntüsü için: adb shell am start … --ei startTab 4
        val start = intent.getIntExtra("startTab", -1)
        setContent {
            AppTheme { Root(start) }
        }
    }

    /** Widget'tan ya da bildirimden açılınca istenen sekme */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        Tab.entries.getOrNull(intent.getIntExtra("startTab", -1))?.let { Nav.tab = it; Nav.toolsRoute = null }
    }

    /** Uygulama öne gelince rozetlerin gün serisi (yalnız son gün ve gün sayısı) */
    override fun onResume() {
        super.onResume()
        BadgeStore.appOpened()
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
    WORLD("tab.world", Icons.Filled.Public, Icons.Outlined.Public),
    SEASONS("tab.seasons", Icons.Filled.Eco, Icons.Outlined.Eco),
    PRAYER("tab.prayer", Icons.Filled.NightsStay, Icons.Outlined.NightsStay),
    TOOLS("tab.tools", Icons.Filled.Timer, Icons.Outlined.Timer),
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

    fun tool(route: String) {
        toolsRoute = route
        tab = Tab.TOOLS
    }
}

@Composable
fun Root(start: Int) {
    // Ekran görüntüsü ya da bildirimden: istenen sekmede açılır
    remember(start) { Tab.entries.getOrNull(start)?.let { Nav.tab = it; Nav.toolsRoute = null } }
    val tab = Nav.tab
    val toolsRoute = Nav.toolsRoute
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    RefreshAutoPlace()

    // Saat ekranında ekran kapanmaz (Ayarlar'dan kapatılabilir); tam ekranda her zaman açık
    LocalView.current.keepScreenOn = fullscreen || (tab == Tab.CLOCK && Prefs.keepAwake)

    if (fullscreen) {
        BackHandler { fullscreen = false }
        Box(Modifier.fillMaxSize()) {
            FullscreenClock { fullscreen = false }
            BadgeToast(Modifier.align(Alignment.TopCenter).statusBarsPadding())
        }
        return
    }
    BackHandler(enabled = tab == Tab.TOOLS && toolsRoute != null) { Nav.toolsRoute = null }
    BackHandler(enabled = tab != Tab.CLOCK && !(tab == Tab.TOOLS && toolsRoute != null)) { Nav.tab = Tab.CLOCK }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Theme.c.bg)) {
        Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
            when (tab) {
                Tab.CLOCK -> ClockScreen(onFullscreen = { fullscreen = true; BadgeStore.hit(Badge.TAM_EKRAN) }, onOpen = { Nav.tab = it })
                Tab.WEATHER -> WeatherScreen()
                Tab.CALENDAR -> CalendarScreen()
                Tab.WORLD -> WorldScreen()
                Tab.SEASONS -> SeasonsScreen()
                Tab.PRAYER -> PrayerScreen()
                Tab.TOOLS -> ToolsScreen(toolsRoute) { Nav.toolsRoute = it }
            }
        }
        TabBar(tab) { t ->
            if (t == tab && t == Tab.TOOLS) Nav.toolsRoute = null
            Nav.tab = t
        }
    }
    BadgeToast(Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Theme.c.surface).navigationBarsPadding()) {
        HorizontalDivider(color = Theme.c.border.copy(alpha = 0.6f))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (t in Tab.entries) {
                val on = t == selected
                val color = if (on) Theme.c.accent else Theme.c.muted
                Column(
                    Modifier.weight(1f).clickable { onSelect(t) }.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(if (on) t.on else t.off, contentDescription = L(t.key), tint = color, modifier = Modifier.size(24.dp))
                    Text(
                        L(t.key), color = color, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
