package tr.bumba.time

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.invisibleToUser
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
 * (önceki sürümü kullananlar görmez; Features.load işaretler). 4 Kasım 2026'dan beri altı adımlık tanıtım (sahibin
 * isteği): hoş geldiniz (canlı saat), saati kaydırma, tam ekran ve gece modu, özellik seçimi, Tümü ve Ayarlar, konum ve
 * bildirimler. Çizimler kodla (telefon çerçevesi içinde kısa döngüler). Bekletme yok; "Geç" her sayfada; Ayarlar ›
 * Destek'ten yeniden gösterilir. iOS'taki Onboarding.swift ile aynı akış ve anahtar.
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

    /** Ayarlar › Destek › Tanıtımı yeniden göster */
    fun replay() {
        pending = true
        Prefs.store.edit().putBoolean(KEY, true).apply()
    }

    /** Bitince: bildirim izni burada sorulduğu için açılıştaki kısa açıklama bir daha çıkmaz */
    fun finish() {
        pending = false
        Prefs.store.edit().putBoolean(KEY, false).putBoolean("notify.asked", true).apply()
    }
}

@Composable
fun OnboardingScreen(startPage: Int = 0, onDone: () -> Unit) {
    val last = 5
    val pager = rememberPagerState(initialPage = startPage.coerceIn(0, last)) { last + 1 }
    val scope = rememberCoroutineScope()
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
                1 -> TipPage("onboard.swipe") { SwipeArt() }
                2 -> TipPage("onboard.full") { FullscreenArt() }
                3 -> FeaturesPage()
                4 -> TipPage("onboard.all") { AllArt() }
                else -> SetupPage()
            }
        }
        // Sayfa noktaları: seçili olan uzun çizgi
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(last + 1) { i ->
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
        // Seçtikçe değişen alt çubuk (Seçenek B; ilk 3 özellik çubuğa girer, gerisi Tümü'nde)
        BarPreview(Modifier.background(Theme.c.surface, RoundedCornerShape(14.dp)).padding(vertical = 10.dp))
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

/** Tanıtım adımı: üstte çizim, altında başlık ve kısa açıklama (anahtarlar `<key>.title`, `<key>.text`) */
@Composable
private fun TipPage(key: String, art: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(290.dp).semantics { invisibleToUser() }, contentAlignment = Alignment.Center) { Ltr { art() } }
        Text(L("$key.title"), color = Theme.c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(L("$key.text"), color = Theme.c.muted, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
    }
}

/** Telefon çerçevesi: yuvarlak köşeli ekran, üstte çentik */
@Composable
private fun PhoneFrame(modifier: Modifier = Modifier, screen: Color = Theme.c.surface, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier.size(150.dp, 280.dp).background(screen, shape).border(5.dp, Theme.c.text.copy(alpha = 0.82f), shape)
            .padding(5.dp).clip(RoundedCornerShape(23.dp)),
    ) {
        content()
        Box(Modifier.align(Alignment.TopCenter).padding(top = 7.dp).size(38.dp, 9.dp)
            .background(Theme.c.text.copy(alpha = 0.82f), RoundedCornerShape(50)))
    }
}

/** 0 → 1 → 0 döngüsü: bekle, yumuşak geç, bekle (hareketi azalt açıksa ilk karede durur) */
@Composable
private fun loopPhase(period: Int = 3200): Float {
    val t = rememberInfiniteTransition(label = "tip")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    val x = ((p - 0.3f) / 0.4f).coerceIn(0f, 1f)
    return x * x * (3 - 2 * x)
}

/** Saati kaydırma: analog saat sola kayar, dijital saat gelir; altta kayan parmak */
@Composable
private fun SwipeArt() {
    val now = rememberNow().atZone(java.time.ZoneId.systemDefault())
    val k = loopPhase()
    PhoneFrame {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            Box(Modifier.fillMaxSize().offset(x = -w * k), contentAlignment = Alignment.Center) {
                AnalogClock(now, Modifier.size(112.dp), tap = false)
            }
            Box(Modifier.fillMaxSize().offset(x = w * (1 - k)), contentAlignment = Alignment.Center) {
                Text(String.format(java.util.Locale.ROOT, "%02d:%02d", now.hour, now.minute), color = Theme.c.ink,
                    fontSize = 38.sp, fontWeight = FontWeight.Light, style = Tnum)
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp).offset(x = (28 - 56 * k).dp).size(30.dp)
            .background(Theme.c.accent.copy(alpha = 0.25f), CircleShape).border(2.dp, Theme.c.accent, CircleShape))
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(2) { i ->
                val on = if (i == 0) k < 0.5f else k >= 0.5f
                Box(Modifier.size(if (on) 14.dp else 5.dp, 5.dp).background(if (on) Theme.c.accent else Theme.c.border, RoundedCornerShape(50)))
            }
        }
    }
}

/** Tam ekran ve gece: telefon yan döner, ekran kararır, saat kısık kırmızı olur */
@Composable
private fun FullscreenArt() {
    val now = rememberNow().atZone(java.time.ZoneId.systemDefault())
    val k = loopPhase(3600)
    val screen = androidx.compose.ui.graphics.lerp(Theme.c.surface, Color.Black, k)
    val digits = androidx.compose.ui.graphics.lerp(Theme.c.ink, Color(0xFFE0402A), k)
    PhoneFrame(Modifier.graphicsLayer { rotationZ = -90f * k }, screen = screen) {
        Column(Modifier.fillMaxSize().graphicsLayer { rotationZ = 90f * k }, horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(String.format(java.util.Locale.ROOT, "%02d:%02d", now.hour, now.minute), color = digits,
                fontSize = (34 + 14 * k).sp, fontWeight = FontWeight.Light, style = Tnum, maxLines = 1)
            Icon(Icons.Filled.Bedtime, null, tint = digits.copy(alpha = k), modifier = Modifier.padding(top = 6.dp).size(18.dp))
        }
    }
}

/** Tümü: arama, renkli araç kutuları, alt çubukta vurgulanan "Tümü" ve uzun basılan kutunun halkası */
@Composable
private fun AllArt() {
    val c = Theme.c
    val k = loopPhase(2800)
    val tiles = listOf(
        Icons.Filled.Alarm to c.terra, Icons.Filled.Timer to c.terra, Icons.Filled.CalendarMonth to c.sage,
        Icons.Filled.WbSunny to c.sky, Icons.Filled.Explore to c.gold, Icons.Filled.MenuBook to c.gold,
        Icons.Filled.Public to c.sky, Icons.Filled.Bedtime to c.dusk, Icons.Filled.Settings to c.dusk,
    )
    PhoneFrame {
        Column(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().height(20.dp).background(c.surface2, RoundedCornerShape(50)).padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = c.muted, modifier = Modifier.size(12.dp))
            }
            for (r in 0 until 3) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until 3) {
                    val (icon, col) = tiles[r * 3 + i]
                    val pressed = r == 0 && i == 0
                    Box(
                        Modifier.size(34.dp).background(col.copy(alpha = if (c.dark) 0.22f else 0.14f), RoundedCornerShape(11.dp))
                            .then(if (pressed) Modifier.border((1 + 2 * k).dp, c.accent.copy(alpha = k), RoundedCornerShape(11.dp)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, null, tint = col, modifier = Modifier.size(18.dp)) }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(34.dp).background(c.surface2).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, tint = c.muted, modifier = Modifier.size(15.dp))
            Icon(Icons.Filled.Bedtime, null, tint = c.muted, modifier = Modifier.size(15.dp))
            Icon(Icons.Filled.WbSunny, null, tint = c.muted, modifier = Modifier.size(15.dp))
            Box(Modifier.background(c.accent.copy(alpha = 0.12f + 0.2f * k), RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                Icon(Icons.Filled.Apps, null, tint = c.accent, modifier = Modifier.size(15.dp))
            }
        }
    }
}
