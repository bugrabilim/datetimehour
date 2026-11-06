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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * İlk kurulum sihirbazı (3 Kasım 2026, 2. analiz #4). 5 Kasım 2026'dan beri (sahibin isteği) sorulu akış:
 * hoş geldiniz → güven kartı, konum ve bildirim → beş kısa soru (Vakitler, Hava, Takvim, Dünya, Mevsimler; her biri Evet/Hayır,
 * yanıt alt çubuğu hemen değiştirir) → özet → "Başka neler var, öğrenmek ister misiniz?" (Evet: özellikler tek tek; Hayır:
 * doğrudan son sayfa) → son sayfa "Diğer her şey Tümü'nde". "Geç" baştan itibaren her sayfada: soruların sonuna kadar geçilirse
 * yeni kurulumda yalnız Saat açık kalır ve alt çubuğun ortasında "Kuruluma devam et" çıkar (Tümü sağda); kişi ayarlardan bir şey
 * değiştirirse bu düğme kalkar. Özetten sonra "Geç" son sayfaya gider. Yeni kurulumda ve tanıtımın bu sürümünü görmemiş eski
 * kullanıcıda bir kez açılır (`onboard.seen` < [VERSION]). Ayarlar › Destek'ten yeniden gösterilir. iOS'taki Onboarding.swift ile
 * aynı akış ve anahtarlar.
 */
object Onboarding {
    const val KEY = "onboard.pending"
    /** Görülen tanıtım sürümü; [VERSION]'dan küçükse (eski kullanıcı) bir kez daha açılır */
    const val SEEN_KEY = "onboard.seen"
    /** 2: sorulu akış ve güven kartı (1.1, 5 Kasım 2026) */
    const val VERSION = 2
    /** Geçilen kurulum: alt çubukta "Kuruluma devam et" (yalnız yeni kurulumda) */
    const val RESUME_KEY = "onboard.resume"
    private const val SIG_KEY = "onboard.resume.sig"
    /** Yeni kurulum işareti (bitince ya da eski kullanıcıda yok): sorular boş başlar, geçilirse yalnız Saat açık kalır */
    private const val FRESH_KEY = "onboard.fresh"
    /** Bunlardan biri değişirse (kişi ayarlardan bir şey değiştirdi) "Kuruluma devam et" kalkar */
    private val SETTING_KEYS = listOf(
        Features.KEY, Features.BAR_KEY, "province", NotifyPrefs.ENABLED, "accent", "appearance", Atmosphere.KEY, "h12", "units",
        "fontScale", "homeItems", AppLanguage.KEY,
    )
    /** Sorulan özellikler, sırayla (alt çubuğa giriş sırası da budur) */
    val QUESTIONS = listOf(Tab.PRAYER, Tab.WEATHER, Tab.CALENDAR, Tab.WORLD, Tab.SEASONS)
    /** Özellik turu (başka neler var sorusuna Evet denince) */
    val TOURS = listOf("alarm", "prayer", "weather", "tools", "look", "widgets")
    /** Sayfa kimlikleri (testler ve CI başlangıç sayfasını sayıyla verir: 0 hoş geldiniz, 1 kurulum, 2–6 sorular, 7 özet,
     *  8 "başka neler var", 9–10 ipuçları, 11–16 özellik turu, 17 son sayfa "Diğer her şey Tümü'nde") */
    val PAGES: List<String> = listOf("welcome", "setup") + QUESTIONS.map { "q:" + Features.id(it) } +
        listOf("summary", "more", "tip:swipe", "tip:full") + TOURS.map { "tour:$it" } + listOf("tip:all")
    val SUMMARY = PAGES.indexOf("summary")
    val MORE = PAGES.indexOf("more")

    var pending by mutableStateOf(false)
        private set
    /** Alt çubukta "Kuruluma devam et" */
    var resume by mutableStateOf(false)
        private set
    /** Sihirbazın açılacağı sayfa (kuruluma devam edilince kurulum sayfası) */
    var startPage = 0
        private set
    /** Yeni kurulum mu (sorular boş başlar; eski kullanıcıda açık özellikler işaretli gelir) */
    var fresh = false
        private set
    /** Bu tanıtımda verilen yanıtlar */
    val answers = mutableStateMapOf<Tab, Boolean>()
    /** Sihirbaz açılırken özellikler; eski kullanıcı geçerse seçimi geri gelir */
    private var before: Pair<Set<Tab>, List<Tab>>? = null
    private val watcher = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (resume && !pending && key in SETTING_KEYS) checkResume()
    }

    /** Features.load'dan: yeni kurulumsa ya da tanıtımın bu sürümü görülmediyse işaretler */
    fun load(fresh: Boolean) {
        val sp = Prefs.store
        if (fresh) sp.edit().putBoolean(FRESH_KEY, true).apply()
        this.fresh = sp.getBoolean(FRESH_KEY, false)
        if (fresh || sp.getInt(SEEN_KEY, 0) < VERSION) sp.edit().putBoolean(KEY, true).apply()
        pending = sp.getBoolean(KEY, false)
        resume = sp.getBoolean(RESUME_KEY, false)
        sp.registerOnSharedPreferenceChangeListener(watcher)
        checkResume()
    }

    /** Ayarlar › Destek › Tanıtımı yeniden göster */
    fun replay() {
        answers.clear()
        startPage = 0
        before = null
        pending = true
        Prefs.store.edit().putBoolean(KEY, true).apply()
    }

    /** Alt çubuktaki "Kuruluma devam et": hoş geldiniz sayfası atlanır, kurulum sayfasından başlar */
    fun resumeSetup() {
        answers.clear()
        startPage = 1
        before = null
        pending = true
        Prefs.store.edit().putBoolean(KEY, true).apply()
    }

    /** Sihirbaz açılınca: o anki özellikler (eski kullanıcı geçerse geri yüklenir) */
    fun begin() {
        if (before == null) before = Features.enabled to Features.bar
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
     * Sorular bitmeden "Geç": yeni kurulumda yalnız Saat (ve Tümü) açık kalır, soruların bölümleri bildirim göndermez ve alt
     * çubuğun ortasında "Kuruluma devam et" çıkar; eski kullanıcının seçimi geri gelir (onda devam düğmesi yok).
     */
    fun skip() {
        val e = Prefs.store.edit()
        if (fresh) {
            Features.save(emptySet())
            for (t in QUESTIONS) for (k in notifyKeys(t)) e.putBoolean(k, false)
        } else before?.let { (on, bar) -> Features.save(on); Features.saveBar(bar) }
        e.putBoolean(KEY, false).putInt(SEEN_KEY, VERSION).putBoolean("notify.asked", true).remove(RESUME_KEY).remove(SIG_KEY)
        e.apply()
        // Ayarların izi yukarıdaki değişikliklerden sonra alınır (yoksa kendi yazdığımız kapatır)
        if (fresh) Prefs.store.edit().putBoolean(RESUME_KEY, true).putString(SIG_KEY, signature()).apply()
        resume = fresh
        answers.clear()
        before = null
        pending = false
    }

    /**
     * Bitince: yanıtlar kalıcı olur. "Hayır" denen bölümün bildirimi kapanır; yeni kurulumda Vakitler'e "Evet" denirse
     * saatin altında sıradaki vakit görünür. Araçlar (Tümü'ndeki araçlar) her zaman açık kalır. Bildirim izni burada
     * sorulduğu için açılıştaki kısa açıklama bir daha çıkmaz.
     */
    fun finish() {
        val e = Prefs.store.edit()
        for ((t, yes) in answers) if (!yes) for (k in notifyKeys(t)) e.putBoolean(k, false)
        e.putBoolean(KEY, false).putInt(SEEN_KEY, VERSION).putBoolean("notify.asked", true).remove(RESUME_KEY).remove(SIG_KEY)
            .remove(FRESH_KEY).apply()
        if (fresh && answers[Tab.PRAYER] == true && !Prefs.store.contains("homeItems")) {
            val layout = HomeLayout(HomeItem.DEFAULT_RAW)
            layout.enabled += HomeItem.NEXT
            HomeItems.raw = layout.raw
        }
        if (fresh && !Features.isOn(Tab.TOOLS)) Features.set(Tab.TOOLS, true)
        answers.clear()
        before = null
        resume = false
        fresh = false
        pending = false
    }

    /** Ayarların izi: geçildiği andaki ayarlarla karşılaştırılır */
    private fun signature(): String {
        val all = Prefs.store.all
        return SETTING_KEYS.joinToString("|") { "${all[it] ?: ""}" }
    }

    /** Kişi ayarlardan bir şey değiştirdiyse "Kuruluma devam et" kalkar */
    fun checkResume() {
        if (!resume) return
        val sp = Prefs.store
        if (sp.getString(SIG_KEY, null) != signature()) {
            sp.edit().remove(RESUME_KEY).remove(SIG_KEY).remove(FRESH_KEY).apply()
            resume = false
            fresh = false
        }
    }
}

@Composable
fun OnboardingScreen(startPage: Int = Onboarding.startPage, onDone: () -> Unit) {
    val pages = Onboarding.PAGES
    val last = pages.size - 1
    val pager = rememberPagerState(initialPage = startPage.coerceIn(0, last)) { last + 1 }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    val id = pages[page]
    // Geri düğmesi gelinen sayfaya döner ("başka neler var" sorusuna Hayır denip son sayfaya atlandıysa oraya)
    val history = remember { mutableStateListOf<Int>() }
    LaunchedEffect(Unit) { Onboarding.begin() }
    fun go(to: Int) { history.add(page); scope.launch { pager.animateScrollToPage(to.coerceIn(0, last)) } }
    fun back() {
        val to = history.removeLastOrNull() ?: (page - 1)
        scope.launch { pager.animateScrollToPage(to.coerceIn(0, last)) }
    }
    BackHandler(enabled = page > 0) { back() }
    Column(Modifier.fillMaxSize().appBackdrop(Theme.c).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) IconButton(onClick = { back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, L("onboard.back"), tint = Theme.c.muted)
            }
            Spacer(Modifier.weight(1f))
            // "Geç" baştan itibaren: sorular bitmeden geçilirse yalnız Saat açık kalır; özetten sonra son sayfaya gider
            if (page < last) TextButton(onClick = { if (page < Onboarding.SUMMARY) Onboarding.skip() else go(last) }) {
                Text(L("onboard.skip"), color = Theme.c.muted, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), userScrollEnabled = false) { p ->
            val pid = pages[p]
            when {
                pid == "welcome" -> WelcomePage()
                pid == "setup" -> SetupPage()
                pid.startsWith("q:") -> {
                    val i = Onboarding.QUESTIONS.indexOfFirst { "q:" + Features.id(it) == pid }
                    QuestionPage(Onboarding.QUESTIONS[i], i + 1, Onboarding.QUESTIONS.size)
                }
                pid == "summary" -> SummaryPage()
                pid == "more" -> MorePage()
                pid == "tip:swipe" -> TipPage("onboard.swipe") { SwipeArt() }
                pid == "tip:full" -> TipPage("onboard.full") { FullscreenArt() }
                pid.startsWith("tour:") -> TourPage(pid.removePrefix("tour:"))
                else -> TipPage("onboard.all") { AllArt() }
            }
        }
        // Sayfa noktaları: seçili olan uzun çizgi
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(last + 1) { i ->
                val on = i == page
                Box(Modifier.padding(horizontal = 2.dp).size(if (on) 16.dp else 5.dp, 5.dp)
                    .background(if (on) Theme.c.accent else Theme.c.border, RoundedCornerShape(50)))
            }
        }
        val bottom = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)
        when {
            id.startsWith("q:") -> {
                // Soru: Hayır / Evet; yanıt hemen uygulanır, sonraki soruya geçilir
                val t = Onboarding.QUESTIONS.first { "q:" + Features.id(it) == id }
                val shown = Onboarding.shown(t)
                Row(bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundButton((if (shown == false) "✓ " else "") + L("onboard.no"), false, Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Onboarding.answer(t, false); go(page + 1)
                    }
                    RoundButton((if (shown == true) "✓ " else "") + L("onboard.yes"), true, Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Onboarding.answer(t, true); go(page + 1)
                    }
                }
            }
            id == "more" -> Row(bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Hayır: doğrudan son sayfa ("Diğer her şey Tümü'nde"); Evet: özellikler tek tek
                RoundButton(L("onboard.no"), false, Modifier.weight(1f).heightIn(min = 52.dp)) { go(last) }
                RoundButton(L("onboard.yes"), true, Modifier.weight(1f).heightIn(min = 52.dp)) { go(page + 1) }
            }
            else -> {
                val label = when (id) { "welcome" -> "onboard.go"; "tip:all" -> "onboard.start"; else -> "onboard.next" }
                RoundButton(L(label), true, bottom.heightIn(min = 52.dp)) { if (page < last) go(page + 1) else onDone() }
            }
        }
    }
}

/** İlk sayfadaki özellik ızgarası (sahibin isteği, 6 Kasım 2026): metin anahtarı → 3D simge, sırası sabit */
private val WELCOME_FEATURES = listOf(
    "clock" to "clock", "calendar" to "calendar", "weather" to "sun_cloud",
    "prayer" to "mosque", "alarm" to "alarm", "world" to "globe",
    "qibla" to "kaaba", "sky" to "telescope", "pomodoro" to "tomato",
)

@Composable
private fun WelcomePage() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        AnalogClock(rememberNow().atZone(java.time.ZoneId.systemDefault()), Modifier.size(116.dp))
        Text(L("app.displayName"), color = Theme.c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        // Sahibin metni (6 Kasım 2026): vurgulu iki satırlık başlık, ne yaptık, özellikler, "Neden ücretsiz?"
        Text(L("onboard.welcome.title"), color = Theme.c.accent, fontSize = 26.sp, lineHeight = 32.sp,
            fontFamily = AppFonts.heading, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(L("onboard.welcome.text"), color = Theme.c.muted, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
        WelcomeFeatures()
        Text(L("onboard.welcome.whyTitle"), color = Theme.c.text, fontSize = 20.sp, lineHeight = 26.sp,
            fontFamily = AppFonts.heading, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp).semantics { heading() })
        Text(L("onboard.welcome.why"), color = Theme.c.muted, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
        Text(L("onboard.welcome.hint"), color = Theme.c.muted, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp))
    }
}

/** 3 × 3 kâğıt kareler: 3D simge ve ad (satırdaki kareler aynı boyda) */
@Composable
private fun WelcomeFeatures() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WELCOME_FEATURES.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (key, icon) ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight().paper(Theme.c.surface, 16).padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                    ) {
                        LineIcon(icon, 34.dp)
                        Text(L("onboard.welcome.f.$key"), color = Theme.c.text, fontSize = 13.sp, lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
        }
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

/** "Başka neler var, öğrenmek ister misiniz?": altta Hayır (son sayfaya) / Evet (özellikler tek tek) */
@Composable
private fun MorePage() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.semantics { invisibleToUser() }) { IconCluster(listOf("sparkles", "books", "star")) }
        Text(L("onboard.more.title"), color = Theme.c.text, fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Text(L("onboard.more.text"), color = Theme.c.muted, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
    }
}

/** Özellik turu sayfası: üç 3D simge, başlık ve kısa açıklama (`onboard.tour.<ad>.title/text`) */
@Composable
private fun TourPage(name: String) {
    val icons = when (name) {
        "alarm" -> listOf("bed", "alarm", "moon")
        "prayer" -> listOf("kaaba", "mosque", "beads")
        "weather" -> listOf("telescope", "sun_cloud", "moon")
        "tools" -> listOf("metronome", "tomato", "knight")
        "look" -> listOf("sparkles", "palette", "clock")
        else -> listOf("sun_cloud", "clock", "star_crescent")
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.semantics { invisibleToUser() }) { IconCluster(icons) }
        Text(L("onboard.tour.$name.title"), color = Theme.c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(L("onboard.tour.$name.text"), color = Theme.c.muted, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
    }
}

/** Üç 3D simge: ortada büyük, iki yanda küçük; arkada yumuşak daire */
@Composable
private fun IconCluster(icons: List<String>) {
    Box(Modifier.size(260.dp, 220.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(190.dp).background(Theme.c.surface, CircleShape))
        Box(Modifier.offset(x = (-78).dp, y = 44.dp).size(84.dp).background(Theme.c.surface2, CircleShape), contentAlignment = Alignment.Center) {
            LineIcon(icons[0], 58.dp)
        }
        Box(Modifier.offset(x = 80.dp, y = (-46).dp).size(76.dp).background(Theme.c.surface2, CircleShape), contentAlignment = Alignment.Center) {
            LineIcon(icons[2], 52.dp)
        }
        LineIcon(icons[1], 118.dp)
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
