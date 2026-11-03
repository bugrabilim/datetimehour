package tr.bumba.time

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * İlk kurulum sihirbazı (3 Kasım 2026, 2. analiz #4; sahibin ekran görüntüleri): yalnız yeni kurulumda bir kez açılır
 * (önceki sürümü kullananlar görmez; Features.load işaretler). Üç sayfa: hoş geldiniz (canlı saat), özellik seçimi,
 * konum ve bildirimler. Bekletme yok; "Geç" her sayfada. iOS'taki Onboarding.swift ile aynı akış ve anahtar.
 */
object Onboarding {
    const val KEY = "onboard.pending"
    var pending by mutableStateOf(false)
        private set

    /** Features.load'dan: yeni kurulumsa işaretler, değilse kayıttakini okur */
    fun load(fresh: Boolean) {
        if (fresh) Prefs.store.edit().putBoolean(KEY, true).apply()
        pending = Prefs.store.getBoolean(KEY, false)
    }

    /** Bitince: bildirim izni burada sorulduğu için açılıştaki kısa açıklama bir daha çıkmaz */
    fun finish() {
        pending = false
        Prefs.store.edit().putBoolean(KEY, false).putBoolean("notify.asked", true).apply()
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val last = 2
    Column(Modifier.fillMaxSize().appBackdrop(Theme.c).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            if (pager.currentPage < last) TextButton(onClick = onDone) {
                Text(L("onboard.skip"), color = Theme.c.muted, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> FeaturesPage()
                else -> SetupPage()
            }
        }
        // Sayfa noktaları: seçili olan uzun çizgi
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(3) { i ->
                val on = i == pager.currentPage
                Box(Modifier.padding(horizontal = 4.dp).size(if (on) 22.dp else 8.dp, 8.dp)
                    .background(if (on) Theme.c.accent else Theme.c.border, RoundedCornerShape(50)))
            }
        }
        RoundButton(L(if (pager.currentPage < last) "onboard.next" else "onboard.start"), true,
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).heightIn(min = 52.dp)) {
            if (pager.currentPage < last) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onDone()
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        AnalogClock(rememberNow().atZone(java.time.ZoneId.systemDefault()), Modifier.size(200.dp))
        Text(L("app.displayName"), color = Theme.c.text, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(L("onboard.welcome.title"), color = Theme.c.accent, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(L("onboard.welcome.text"), color = Theme.c.muted, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FeaturesPage() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(L("onboard.features.title"), color = Theme.c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(L("onboard.features.text"), color = Theme.c.muted, fontSize = 14.sp)
        Features.OPTIONAL.forEach { t ->
            val on = Features.isOn(t)
            Row(
                Modifier.fillMaxWidth()
                    .background(Theme.c.surface, RoundedCornerShape(14.dp))
                    .border(1.5.dp, if (on) Theme.c.accent else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable { Features.set(t, !on) }
                    .semantics { role = Role.Checkbox; selected = on }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(t.on, null, tint = Theme.c.accent, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(L(t.key), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(L("features.${Features.id(t)}.note"), color = Theme.c.muted, fontSize = 12.sp)
                }
                Icon(if (on) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
                    tint = if (on) Theme.c.accent else Theme.c.border, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun SetupPage() {
    val context = LocalContext.current
    var notifyOn by remember { mutableStateOf(PrayerNotifier.notificationsAllowed(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifyOn = PrayerNotifier.notificationsAllowed(context)
        PrayerNotifier.reschedule(context)
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(L("onboard.setup.title"), color = Theme.c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(L("onboard.setup.text"), color = Theme.c.muted, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().background(Theme.c.surface, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, null, tint = Theme.c.accent)
            Spacer(Modifier.width(10.dp))
            Text(L("settings.location"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
            PlacePicker(size = 16.sp)
        }
        Row(
            Modifier.fillMaxWidth().background(Theme.c.surface, RoundedCornerShape(14.dp))
                .clickable(enabled = !notifyOn) {
                    if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else notifyOn = PrayerNotifier.notificationsAllowed(context)
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (notifyOn) Icons.Filled.NotificationsActive else Icons.Filled.Notifications, null, tint = Theme.c.accent)
            Spacer(Modifier.width(10.dp))
            Text(L(if (notifyOn) "onboard.notifyOn" else "onboard.notify"), Modifier.weight(1f),
                color = if (notifyOn) Theme.c.accent else Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (notifyOn) Icon(Icons.Filled.CheckCircle, null, tint = Theme.c.accent)
        }
    }
}
