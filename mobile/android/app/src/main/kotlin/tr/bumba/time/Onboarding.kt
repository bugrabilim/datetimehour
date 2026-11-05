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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.BackHandler
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
 * İlk kurulum sihirbazı (3 Kasım 2026, 2. analiz #4). 5 Kasım 2026'dan beri (sahibin isteği) sorulu akış:
 * hoş geldiniz → beş kısa soru (Vakitler, Hava, Takvim, Dünya, Mevsimler; her biri Evet/Hayır, yanıt alt çubuğu hemen
 * değiştirir) → özet (oluşan alt çubuk) → üç ipucu (kaydırma, tam ekran, Tümü; "Geç" yalnız burada) → güven kartı, konum
 * ve bildirim. Sorular atlanamaz; "Hayır" denen bölüm gizlenir ve bildirimi kapanır. Yeni kurulumda ve tanıtımın bu
 * sürümünü görmemiş eski kullanıcıda bir kez açılır (`onboard.seen` < [VERSION]; 1.1'e güncelleyenler de görsün diye).
 * Ayarlar › Destek'ten yeniden gösterilir. iOS'taki Onboarding.swift ile aynı akış ve anahtarlar.
 */
object Onboarding {
    const val KEY = "onboard.pending"
    /** Görülen tanıtım sürümü; [VERSION]'dan küçükse (eski kullanıcı) bir kez daha açılır */
    const val SEEN_KEY = "onboard.seen"
    /** 2: sorulu akış ve güven kartı (1.1, 5 Kasım 2026) */
    const val VERSION = 2
    /** Sorulan özellikler, sırayla (alt çubuğa giriş sırası da budur) */
    val QUESTIONS = listOf(Tab.PRAYER, Tab.WEATHER, Tab.CALENDAR, Tab.WORLD, Tab.SEASONS)

    var pending by mutableStateOf(false)
        private set
    /** Yeni kurulum mu (sorular boş başlar; eski kullanıcıda açık özellikler işaretli gelir) */
    var fresh = false
        private set
    /** Bu tanıtımda verilen yanıtlar */
    val answers = mutableStateMapOf<Tab, Boolean>()

    /** Features.load'dan: yeni kurulumsa ya da tanıtımın bu sürümü görülmediyse işaretler */
    fun load(fresh: Boolean) {
        this.fresh = fresh
        val sp = Prefs.store
        if (fresh || sp.getInt(SEEN_KEY, 0) < VERSION) sp.edit().putBoolean(KEY, true).apply()
        pending = sp.getBoolean(KEY, false)
    }

    /** Ayarlar › Destek › Tanıtımı yeniden göster */
    fun replay() {
        answers.clear()
        pending = true
        Prefs.store.edit().putBoolean(KEY, true).apply()
    }

    /** Sorunun gösterilen yanıtı: verildiyse o, eski kullanıcıda açık/kapalı durumu, yeni kurulumda boş */
    fun shown(t: Tab): Boolean? = answers[t] ?: if (fresh) null else Features.isOn(t)

    /** Yanıt hemen uygulanır (özet sayfasındaki alt çubuk canlı değişir) */
    fun answer(t: Tab, yes: Boolean) {
        answers[t] = yes
        Features.set(t, yes)
    }

    /** "Hayır" denen bölümün bildirimleri (iOS'takiyle aynı anahtarlar) */
    fun notifyKeys(t: Tab): List<String> = when (t) {
        Tab.PRAYER -> listOf(NotifyPrefs.ENABLED, "notify.friday")
        Tab.WEATHER -> listOf("notify.weather")
        Tab.CALENDAR -> listOf("notify.calendar", "notify.daily")
        Tab.SEASONS -> listOf("notify.seasons", "notify.moon", "notify.sun", "notify.sky")
        else -> emptyList()
    }

    /**
     * Bitince: yanıtlar kalıcı olur. "Hayır" denen bölümün bildirimi kapanır; yeni kurulumda Vakitler'e "Evet" denirse
     * saatin altında sıradaki vakit görünür. Araçlar (Tümü'ndeki araçlar) her zaman açık kalır. Bildirim izni burada
     * sorulduğu için açılıştaki kısa açıklama bir daha çıkmaz.
     */
    fun finish() {
        val e = Prefs.store.edit()
        for ((t, yes) in answers) if (!yes) for (k in notifyKeys(t)) e.putBoolean(k, false)
        e.putBoolean(KEY, false).putInt(SEEN_KEY, VERSION).putBoolean("notify.asked", true).apply()
        if (fresh && answers[Tab.PRAYER] == true && !Prefs.store.contains("homeItems")) {
            val layout = HomeLayout(HomeItem.DEFAULT_RAW)
            layout.enabled += HomeItem.NEXT
            HomeItems.raw = layout.raw
        }
        if (fresh && !Features.isOn(Tab.TOOLS)) Features.set(Tab.TOOLS, true)
        answers.clear()
        pending = false
    }
}

/** Sayfa kimlikleri (testler ve CI başlangıç sayfasını sayıyla verir: 0 hoş geldiniz, 1–5 sorular, 6 özet, 7–9 ipuçları, 10 kurulum) */
private val ONBOARD_PAGES: List<String> =
    listOf("welcome") + Onboarding.QUESTIONS.map { "q:" + Features.id(it) } + listOf("summary", "tip:swipe", "tip:full", "tip:all", "setup")

@Composable
fun OnboardingScreen(startPage: Int = 0, onDone: () -> Unit) {
    val last = ONBOARD_PAGES.size - 1
    val pager = rememberPagerState(initialPage = startPage.coerceIn(0, last)) { last + 1 }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    val id = ONBOARD_PAGES[page]
    fun go(to: Int) { scope.launch { pager.animateScrollToPage(to.coerceIn(0, last)) } }
    BackHandler(enabled = page > 0) { go(page - 1) }
    Column(Modifier.fillMaxSize().appBackdrop(Theme.c).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) IconButton(onClick = { go(page - 1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, L("onboard.back"), tint = Theme.c.muted)
            }
            Spacer(Modifier.weight(1f))
            // "Geç" yalnız ipuçlarında: sorular atlanmaz, kurulum sayfasına geçilir
            if (id.startsWith("tip:")) TextButton(onClick = { go(last) }) {
                Text(L("onboard.skip"), color = Theme.c.muted, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), userScrollEnabled = false) { p ->
            val pid = ONBOARD_PAGES[p]
            when {
                pid == "welcome" -> WelcomePage()
                pid.startsWith("q:") -> QuestionPage(Onboarding.QUESTIONS[p - 1], p, Onboarding.QUESTIONS.size)
                pid == "summary" -> SummaryPage()
                pid == "tip:swipe" -> TipPage("onboard.swipe") { SwipeArt() }
                pid == "tip:full" -> TipPage("onboard.full") { FullscreenArt() }
                pid == "tip:all" -> TipPage("onboard.all") { AllArt() }
                else -> SetupPage()
            }
        }
        // Sayfa noktaları: seçili olan uzun çizgi
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(last + 1) { i ->
                val on = i == page
                Box(Modifier.padding(horizontal = 3.dp).size(if (on) 18.dp else 6.dp, 6.dp)
                    .background(if (on) Theme.c.accent else Theme.c.border, RoundedCornerShape(50)))
            }
        }
        val bottom = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)
        if (id.startsWith("q:")) {
            // Soru: Hayır / Evet; yanıt hemen uygulanır, sonraki soruya geçilir
            val t = Onboarding.QUESTIONS[page - 1]
            val shown = Onboarding.shown(t)
            Row(bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundButton((if (shown == false) "✓ " else "") + L("onboard.no"), false, Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Onboarding.answer(t, false); go(page + 1)
                }
                RoundButton((if (shown == true) "✓ " else "") + L("onboard.yes"), true, Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Onboarding.answer(t, true); go(page + 1)
                }
            }
        } else {
            val label = when (id) { "welcome" -> "onboard.go"; "setup" -> "onboard.start"; else -> "onboard.next" }
            RoundButton(L(label), true, bottom.heightIn(min = 52.dp)) { if (page < last) go(page + 1) else onDone() }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        AnalogClock(rememberNow().atZone(java.time.ZoneId.systemDefault()), Modifier.size(200.dp))
        Text(L("app.displayName"), color = Theme.c.text, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(L("onboard.welcome.title"), color = Theme.c.accent, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(L("onboard.welcome.text"), color = Theme.c.muted, fontSize = 16.sp, textAlign = TextAlign.Center)
        Text(L("onboard.welcome.hint"), color = Theme.c.text, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

/** Tek soru: büyük 3D simge, soru, ne değişeceği; altta Hayır / Evet (OnboardingScreen) */
@Composable
private fun QuestionPage(t: Tab, n: Int, total: Int) {
    val key = Features.id(t)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(T("onboard.q.step", n, total), color = Theme.c.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.size(150.dp).background(Theme.c.surface, CircleShape), contentAlignment = Alignment.Center) {
            LineIcon(LineIconMap.tab(t), 104.dp)
        }
        Text(L("onboard.q.$key.title"), color = Theme.c.text, fontSize = 25.sp, lineHeight = 31.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(L("onboard.q.$key.text"), color = Theme.c.muted, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
        Text(L("onboard.q.note"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
    }
}

/** Özet: yanıtlara göre oluşan alt çubuk ve açılan bölümler */
@Composable
private fun SummaryPage() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(L("onboard.summary.title"), color = Theme.c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(L("onboard.summary.text"), color = Theme.c.muted, fontSize = 15.sp)
        BarPreview(Modifier.paper(Theme.c.surface).padding(vertical = 12.dp))
        val on = Onboarding.QUESTIONS.filter { Features.isOn(it) }
        if (on.isEmpty()) Text(L("onboard.summary.none"), color = Theme.c.muted, fontSize = 15.sp)
        else AppCard(padding = 16) {
            Text(L("onboard.summary.on"), color = Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            for (t in on) Row(verticalAlignment = Alignment.CenterVertically) {
                LineIcon(LineIconMap.tab(t), 28.dp)
                Spacer(Modifier.width(12.dp))
                Text(L(t.key), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Filled.CheckCircle, null, tint = Theme.c.accent, modifier = Modifier.size(22.dp))
            }
        }
        Text(L("onboard.q.note"), color = Theme.c.muted, fontSize = 13.sp)
    }
}

/** Güven kartı: hesap/reklam/takip yok, verileriniz bize gelmez (kurulum sayfası ve Ayarlar › İzinler) */
@Composable
fun TrustCard() {
    AppCard(padding = 18) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineIcon("heart", 30.dp)
            Spacer(Modifier.width(10.dp))
            Text(L("onboard.trust.title"), color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(L("onboard.trust.text"), color = Theme.c.text, fontSize = 15.sp, lineHeight = 21.sp)
        Text(L("onboard.trust.services"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(L("onboard.setup.title"), color = Theme.c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        TrustCard()
        Column(Modifier.fillMaxWidth().paper(Theme.c.surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineIcon("pin", 26.dp)
                Spacer(Modifier.width(10.dp))
                Text(L("settings.location"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                PlacePicker(size = 16.sp)
            }
            Text(L("perms.location.why"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Column(
            Modifier.fillMaxWidth().paper(Theme.c.surface)
                .clickable(enabled = !notifyOn) {
                    if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else notifyOn = PrayerNotifier.notificationsAllowed(context)
                }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineIcon("bell", 26.dp)
                Spacer(Modifier.width(10.dp))
                Text(L(if (notifyOn) "onboard.notifyOn" else "onboard.notify"), Modifier.weight(1f),
                    color = if (notifyOn) Theme.c.accent else Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (notifyOn) Icon(Icons.Filled.CheckCircle, null, tint = Theme.c.accent)
            }
            Text(L("perms.notify.why"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Text(L("onboard.trust.later"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
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
    // 3D simgeler (4 Kasım 2026, sahibin seçimi)
    val tiles = listOf(
        "alarm" to c.terra, "hourglass" to c.terra, "calendar" to c.sage,
        "sun_cloud" to c.sky, "compass" to c.gold, "book" to c.gold,
        "globe" to c.sky, "moon" to c.dusk, "gear" to c.dusk,
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
                    ) { LineIcon(icon, 22.dp) }
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
