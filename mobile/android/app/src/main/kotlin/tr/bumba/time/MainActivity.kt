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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
}

/**
 * Sekmeler (iOS'taki sırayla; hava durumu Android'de henüz yok): Saat, Takvim, Dünya, Mevsimler, Vakitler, Araçlar.
 * Seçili sekmeye yeniden dokununca Araçlar'da açık sayfadan listeye dönülür.
 */
enum class Tab(val key: String, val on: ImageVector, val off: ImageVector) {
    CLOCK("tab.clock", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    CALENDAR("tab.calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    WORLD("tab.world", Icons.Filled.Public, Icons.Outlined.Public),
    SEASONS("tab.seasons", Icons.Filled.Eco, Icons.Outlined.Eco),
    PRAYER("tab.prayer", Icons.Filled.NightsStay, Icons.Outlined.NightsStay),
    TOOLS("tab.tools", Icons.Filled.Timer, Icons.Outlined.Timer),
}

@Composable
fun Root(start: Int) {
    var tab by rememberSaveable { mutableStateOf(Tab.entries.getOrElse(start) { Tab.CLOCK }) }
    var toolsRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    RefreshAutoPlace()

    // Saat ekranında ekran kapanmaz (Ayarlar'dan kapatılabilir); tam ekranda her zaman açık
    LocalView.current.keepScreenOn = fullscreen || (tab == Tab.CLOCK && Prefs.keepAwake)

    if (fullscreen) {
        BackHandler { fullscreen = false }
        FullscreenClock { fullscreen = false }
        return
    }
    BackHandler(enabled = tab == Tab.TOOLS && toolsRoute != null) { toolsRoute = null }
    BackHandler(enabled = tab != Tab.CLOCK && !(tab == Tab.TOOLS && toolsRoute != null)) { tab = Tab.CLOCK }

    Column(Modifier.fillMaxSize().background(Theme.c.bg)) {
        Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
            when (tab) {
                Tab.CLOCK -> ClockScreen(onFullscreen = { fullscreen = true }, onOpen = { tab = it })
                Tab.CALENDAR -> CalendarScreen()
                Tab.WORLD -> WorldScreen()
                Tab.SEASONS -> SeasonsScreen()
                Tab.PRAYER -> PrayerScreen()
                Tab.TOOLS -> ToolsScreen(toolsRoute) { toolsRoute = it }
            }
        }
        TabBar(tab) { t ->
            if (t == tab && t == Tab.TOOLS) toolsRoute = null
            tab = t
        }
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
