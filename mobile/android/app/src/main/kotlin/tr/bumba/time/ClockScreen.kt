package tr.bumba.time

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Saat sekmesi (iOS'taki ClockTab): sitedeki 19 saat modeli (kaydırarak ya da sayaçtan seçilir), altında kullanıcının
 * seçtiği alanlar (Ayarlar › Saat ekranı ya da "Düzenle"; dokununca ilgili sayfa açılır), tam ekran ve Düzenle.
 * Varsayılan düzende sayfa ekrana tam oturur: dönen dünya kalan boşluğu doldurur.
 */
@Composable
fun ClockScreen(onFullscreen: () -> Unit, onOpen: (Tab) -> Unit, onSettings: (String) -> Unit = {}) {
    val now = rememberNow()
    val place = AppPlace.current()
    val items = HomeItems.layout.visible
    var customizing by remember { mutableStateOf(false) }
    val pager = rememberClockPager()
    // Saniye tık sesi (2. analiz #10; Ayarlar › Saat görünümü)
    val ctx = LocalContext.current
    LaunchedEffect(now.epochSecond) { TickSound.tick(ctx, now.epochSecond) }
    DisposableEffect(Unit) { onDispose { TickSound.stop() } }

    // Hava zemini açıkken (2. analiz #193) denetimler beyaz, atıf saatin altında akışta (4 Kasım 2026, sahibin ekran
    // görüntüsü: atıf saatin üstüne biniyordu, tema rengindeki yazılar yağmur zemininde soluk kalıyordu)
    val onScene = Prefs.store.getBoolean(WeatherScene.CLOCK_KEY, false)
    val ctrl = if (onScene) androidx.compose.ui.graphics.Color.White else Theme.c.accent
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Düğme satırı alt çubuğun hemen üstünde, başparmak bölgesinde (2. analiz #202)
        val actionBar = 56.dp
        val markRow = if (onScene) 30.dp else 0.dp
        val viewport = maxHeight - actionBar - markRow
        val globeHeight = rememberGlobeHeight()
        // Saat ekranında yalnız saat varsa (30 Ekim kararı) saat ve denetim satırı dikeyde ortalanır, saat genişliğe büyür
        // (2. analiz #94)
        val onlyClock = items.isEmpty() && FsLook.text.isEmpty()
        val stage = if (onlyClock) min((viewport - 150.dp).value, (minOf(maxWidth, 640.dp) * 0.95f).value).dp.coerceAtLeast(280.dp)
            else stageHeight(viewport)
        Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                    .then(if (onlyClock) Modifier.heightIn(min = viewport - 20.dp) else Modifier)
                    .fitGlobe(viewport, globeHeight, HomeItem.GLOBE in items),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (onlyClock) Arrangement.spacedBy(16.dp, Alignment.CenterVertically) else Arrangement.spacedBy(16.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalPager(pager, Modifier.fillMaxWidth().height(stage)) { page ->
                        ClockModelPage(page, now, place, Modifier.padding(horizontal = 2.dp))
                    }
                    StageControls(pager, ctrl)
                }
                // Saatin altındaki kişisel metin (rakip analizi #25)
                if (FsLook.text.isNotEmpty()) Text(FsLook.text, color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                for (item in items) {
                    HomeItemView(item, now, place, globeHeight.value) { onOpen(it.open()) }
                }
                // Yenilikler: yeni kadranlar bir kez tanıtılır (2. analiz #1)
                Box(Modifier.padding(horizontal = 16.dp)) { DialNewsCard { onSettings("dials") } }
                // Hiçbir özellik açık değilse (ilk açılış) özellikleri açmaya çağıran tek satır
                if (Features.enabled.isEmpty()) {
                    TextButton(onClick = { onSettings("features") }) {
                        Icon(Icons.Filled.Apps, null, tint = Theme.c.muted, modifier = Modifier.size(18.dp))
                        Text(" " + L("features.hint"), color = Theme.c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (onScene) WeatherAttribution(Modifier.fillMaxWidth().height(markRow).wrapContentHeight())
        Row(Modifier.fillMaxWidth().height(actionBar), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onFullscreen) {
                Icon(Icons.Filled.Fullscreen, null, tint = ctrl)
                Text(" " + L("clock.fullscreen"), color = ctrl, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { customizing = true }) {
                Icon(Icons.Filled.Tune, null, tint = ctrl)
                Text(" " + L("home.edit"), color = ctrl, fontWeight = FontWeight.SemiBold)
            }
        }
        }
    }

    if (customizing) {
        Dialog({ customizing = false }, DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = Theme.c.bg) {
                Column {
                    SubBar(L("home.customize"), actions = {
                        TextButton(onClick = { customizing = false }) { Text(L("common.done"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold) }
                    })
                    Box(Modifier.weight(1f)) { HomeItemsEditor() }
                }
            }
        }
    }
}

/** Seçili modelle eşlenen sayfa durumu (Prefs.clockModel; eski 3 modelli kayıtlar 19 içine sıkıştırılır) */
@Composable
private fun rememberClockPager(): PagerState {
    val pager = rememberPagerState(initialPage = Prefs.clockModel.coerceIn(0, ClockModel.count - 1)) { ClockModel.count }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(pager.currentPage) {
        Prefs.saveClockModel(pager.currentPage)
        // Rozetler: görülen modeller (açılıştaki dahil) ve ilk model değişimi
        BadgeStore.sawModel(pager.currentPage, ClockModel.count)
        if (first[0]) first[0] = false else BadgeStore.hit(Badge.ILK_MODEL)
    }
    return pager
}

/** Tam ekran: saat modelleri + iki yanda araç sayfaları; saat sayfasına geçilince model kaydedilir */
@Composable
private fun rememberFullscreenPager(): PagerState {
    val pager = rememberPagerState(initialPage = Prefs.clockModel.coerceIn(0, ClockModel.count - 1) + 1) { ClockModel.count + 2 }
    LaunchedEffect(pager.currentPage) {
        val m = pager.currentPage - 1
        if (m in 0 until ClockModel.count) {
            if (m != Prefs.clockModel) BadgeStore.hit(Badge.ILK_MODEL)
            Prefs.saveClockModel(m)
            BadgeStore.sawModel(m, ClockModel.count)
        }
    }
    return pager
}

@Composable
private fun rememberGlobeHeight() = remember { mutableStateOf(260.dp) }

/**
 * Dünya, sayfayı ekranın altına kadar dolduracak yüksekliği alır (en az 170 dp; daha fazla alan eklenmişse sayfa
 * kayar). İçerik yüksekliği ölçülür, dünya dışındaki kısım çıkarılır (iOS'taki fitGlobe).
 */
@Composable
private fun Modifier.fitGlobe(viewport: Dp, globe: androidx.compose.runtime.MutableState<Dp>, hasGlobe: Boolean): Modifier {
    val density = LocalDensity.current
    return onSizeChanged { size ->
        if (!hasGlobe) return@onSizeChanged
        val content = with(density) { size.height.toDp() }
        val target = maxOf(170.dp, viewport - (content - globe.value))
        if (kotlin.math.abs((target - globe.value).value) > 1f) globe.value = Dp(kotlin.math.floor(target.value))
    }
}

/** Sahnenin altında ‹ 1/20 ›: sayaca dokununca model listesi açılır */
@Composable
private fun StageControls(pager: PagerState, tint: androidx.compose.ui.graphics.Color = Theme.c.accent) {
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    val n = ClockModel.count
    Ltr {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage + n - 1) % n) } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("stage.prev"), tint = tint)
            }
            Box {
                // Model adı ve sırası (2. analiz #202): "Klasik analog · 1/20"; dokununca liste
                Text(
                    ClockModel.at(pager.currentPage).title + " · ${pager.currentPage + 1}/$n",
                    Modifier.widthIn(max = 260.dp).clickable { menu = true }.padding(horizontal = 8.dp, vertical = 6.dp),
                    color = tint, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, style = Tnum, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                DropdownMenu(menu, { menu = false }) {
                    for (m in ClockModel.entries) {
                        DropdownMenuItem(
                            text = {
                                Text("${m.number}. ${m.title}", color = if (m.ordinal == pager.currentPage) Theme.c.accent else Theme.c.text,
                                    fontWeight = if (m.ordinal == pager.currentPage) FontWeight.SemiBold else FontWeight.Normal)
                            },
                            onClick = {
                                menu = false
                                scope.launch { pager.scrollToPage(m.ordinal) }
                            },
                        )
                    }
                }
            }
            IconButton(onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage + 1) % n) } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("stage.next"), tint = tint)
            }
        }
    }
}

/** Rakamlı analog saat (sitedeki analogSvg ölçüleri, 200 birimlik kare); dokununca saniye */
@Composable
fun AnalogClock(
    t: java.time.ZonedDateTime, modifier: Modifier = Modifier, caption: String? = null,
    /** Kadran (2. analiz #1): verilmezse seçili kadran ve renk; galeride dokunuş saniyeyi açıp kapatmaz */
    face: DialFace? = null, dialColor: DialColor? = null, tap: Boolean = true,
) {
    val c = Theme.c
    val f = face ?: DialFace.current
    val k = (dialColor ?: DialColor.current).apply(f.colors(c))
    val measurer = rememberTextMeasurer()
    // Saniye ibresinin hareketi (2. analiz #10): mekanik ve kayan saniyede ibre ekran karesiyle çizilir
    val motion = ClockLook.motion
    val frame = rememberFrameMillis(Prefs.secA && motion != "tick", ClockLook.frameMs(motion))
    val tt = frame?.let { java.time.Instant.ofEpochMilli(it).atZone(t.zone) } ?: t
    val sec = if (frame != null) ClockLook.seconds(frame, motion) else tt.second.toDouble()
    val min = tt.minute + sec / 60
    val hour = (tt.hour % 12) + min / 60
    Canvas(
        modifier.aspectRatio(1f).widthIn(max = 520.dp)
            .then(if (tap) Modifier.clickable(remember { MutableInteractionSource() }, null) { Prefs.saveSecA(!Prefs.secA); BadgeStore.hit(Badge.SANIYE) } else Modifier),
    ) {
        val u = min(size.width, size.height) / 200f
        val center = Offset(size.width / 2, size.height / 2)
        drawDial(f, k, measurer, tt.hour + min / 60, min, sec, fontScale)
        if (caption != null) {
            val cap = measurer.measure(caption, TextStyle(color = k.minor, fontSize = (13 * u / density / fontScale).sp, fontWeight = FontWeight.Bold))
            drawText(cap, topLeft = Offset(center.x - cap.size.width / 2f, center.y + 34 * u - cap.size.height / 2f))
        }
        fun hand(angle: Double, from: Float, to: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
            rotate(angle.toFloat(), center) {
                drawLine(color, Offset(center.x, center.y + from * u), Offset(center.x, center.y - to * u), width * u, StrokeCap.Round)
            }
        }
        // Akrep yazı renginde, yelkovan temanın ana renginde, saniye ve orta temanın ikinci renginde
        hand(hour * 30, 4f, 44f, 6f, k.ink)
        hand(min * 6, 6f, 66f, 4.5f, k.hand)
        if (Prefs.secA || !tap) hand(sec * 6, 16f, 76f, 2f, k.second)
        drawCircle(k.second, 5 * u, center)
    }
}

/**
 * Tam ekran saat: 20 model yatay kaydırılır, altında saat ekranındaki alanlar (Düzenle ile seçilenler); ekran kapanmaz,
 * çift dokununca ya da düğmeyle çıkılır. Bir alana dokununca tam ekran kapanır, sayfası açılır.
 */
@Composable
fun FullscreenClock(controls: Boolean = true, onExit: () -> Unit) {
    val now = rememberNow()
    val place = AppPlace.current()
    val items = HomeItems.layout.visible
    // Saat modellerinin iki yanında araç sayfaları (2. analiz #26): 0 = pomodoro, son = kronometre
    val pager = rememberFullscreenPager()
    HideSystemBars()
    // Arka plan: fotoğraf (koyulaştırılmış) ya da hareketli; ikisinde de yazılar koyu temanın açık renklerine geçer
    // Gece modunda siyah zemin, kırmızı ve kısık yazı, düşük parlaklık; arka plan fotoğrafı ve hareket yok
    // Gün doğumu ışığı (rakip analizi #17): alarmdan önce zemin siyahtan sıcak beyaza döner, parlaklık artar
    val sunrise = AlarmItem.nextSunrise(now.toEpochMilli())
        ?.let { (at, window) -> tr.bumba.time.core.SunriseLight.level(now.toEpochMilli(), at, window) } ?: 0.0
    val ambientDark = rememberAmbientDark(Prefs.nightMode == "light")
    val night = sunrise == 0.0 && NightMode.active(Prefs.nightMode, place.lat, place.lon, now.toEpochMilli(), ambientDark)
    // Uyku zamanlayıcısı (rakip analizi #1): süre dolunca ekran kararır, telefon kendini kilitleyebilir, uyku sesleri durur
    val context = LocalContext.current
    var sleepUntil by remember { mutableLongStateOf(Prefs.store.getLong(SleepTimer.KEY, 0L)) }
    val asleep = sleepUntil > 0 && now.toEpochMilli() >= sleepUntil
    LaunchedEffect(now.epochSecond) { if (!asleep) TickSound.tick(context, now.epochSecond) }
    val view = LocalView.current
    SideEffect { if (asleep) view.keepScreenOn = false }
    LaunchedEffect(asleep) { if (asleep) SleepSounds.stop(context) }
    DisposableEffect(Unit) { onDispose { Prefs.store.edit().remove(SleepTimer.KEY).apply() } }
    fun setSleep(minutes: Int) {
        sleepUntil = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else 0L
        Prefs.store.edit().putLong(SleepTimer.KEY, sleepUntil).apply()
        view.keepScreenOn = true
    }
    ScreenBrightness(when {
        asleep -> 0.01f
        sunrise > 0 -> tr.bumba.time.core.SunriseLight.brightness(sunrise).toFloat()
        night -> 0.05f
        else -> null
    })
    val bg = if (night || sunrise > 0) FullscreenBackground.PLAIN else FullscreenBackground.current
    // Fotoğraf: ana fotoğraf ve slaytlar sırayla (rakip analizi #24)
    val photo = if (bg == FullscreenBackground.PHOTO) FsLook.image(LocalContext.current, now.toEpochMilli()) else null
    // Yön kilidi (rakip analizi #22): açıksa tam ekran o anki yöne kilitlenir; çıkınca bırakılır
    val activity = LocalContext.current as? Activity
    DisposableEffect(FsLook.lock) {
        if (FsLook.lock) activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
        onDispose { activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    var dimming by remember { mutableStateOf(false) }
    // Düğmeler dokununca belirir, 4 sn sonra kaybolur (sitedeki tam ekran gibi; 2. analiz #204)
    var shown by remember { mutableStateOf(true) }
    var touched by remember { mutableLongStateOf(0L) }
    LaunchedEffect(touched) { kotlinx.coroutines.delay(4000); shown = false }
    val controlsAlpha by androidx.compose.animation.core.animateFloatAsState(if (shown) 1f else 0f, label = "fsControls")
    val colors = when {
        sunrise > 0.6 -> AccentPalette.find(Prefs.accent).colors(false)
        night || sunrise > 0 || photo != null || bg.animated -> AccentPalette.find(Prefs.accent).colors(true)
        else -> Theme.c
    }
    val sunriseColor = Color(0xFF000000 or tr.bumba.time.core.SunriseLight.color(sunrise).toLong())
    CompositionLocalProvider(LocalColors provides colors) {
    BoxWithConstraints(
        Modifier.fillMaxSize().nightTint(night).then(if (sunrise > 0) Modifier.background(sunriseColor) else if (night) Modifier.background(Color.Black) else Modifier.appBackdrop(Theme.c))
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onExit() }) }
            // Her dokunuşta düğmeler belirir (dokunuş tüketilmez; kaydırma ve sayfalama bozulmaz)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    shown = true
                    touched = System.nanoTime()
                }
            }
            // İki parmakla saat boyutu (çocuklardan önce yakalanır; tek parmak kaydırma ve sayfalama bozulmaz)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) FsLook.saveScale(FsLook.scale * zoom)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        if (photo != null) PhotoBackground(photo, Modifier.fillMaxSize())
        else if (bg.animated) AnimatedBackground(bg, Modifier.fillMaxSize())
        // Ekran yanmasın diye dakikada bir birkaç nokta kayar
        val (dx, dy) = if (night) NightMode.shift(now.toEpochMilli()) else 0 to 0
        val viewport = maxHeight
        // Yatayda saat ekranı doldurur, alanlar kaydırınca görünür
        val landscape = maxWidth > maxHeight
        val bannerAspect = (maxWidth.value - 32) / (maxHeight.value * 0.5)
        val globeHeight = rememberGlobeHeight()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.offset(dx.dp, dy.dp).widthIn(max = if (landscape && FsDuo.current != "off") 1400.dp else 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .fitGlobe(viewport, globeHeight, HomeItem.GLOBE in items),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Tabela (2. analiz #22): metin ekranın üst yarısını doldurur, saat altında küçülür
                val banner = ClockLook.banner && FsLook.text.isNotEmpty()
                if (banner) Box(Modifier.fillMaxWidth().height(viewport * 0.5f), contentAlignment = Alignment.Center) {
                    Text(bannerLines(FsLook.text, bannerAspect.toDouble()), Modifier.fillMaxWidth(),
                    color = Theme.c.text, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 3, softWrap = false,
                    style = androidx.compose.ui.text.TextStyle(lineHeight = androidx.compose.ui.unit.TextUnit(1.05f, androidx.compose.ui.unit.TextUnitType.Em)),
                    autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 400.sp, stepSize = 2.sp))
                }
                val stage = when {
                    banner -> maxOf(180.dp, viewport * 0.4f)
                    landscape -> maxOf(220.dp, viewport - 48.dp)
                    items.isEmpty() -> maxOf(300.dp, viewport - 60.dp)
                    else -> stageHeight(viewport * 1.08f)
                }
                // Yatayda saatin yanında ikinci alan (2. analiz #169, `fs.duo`): takvim, vakitler ya da hava
                val duo = if (landscape && !banner) FsDuo.current else "off"
                val pagerView: @Composable (Modifier) -> Unit = { m ->
                    HorizontalPager(pager, m.height(stage)) { page ->
                        when (page) {
                            0 -> FullscreenToolPage(FullscreenTool.POMODORO)
                            ClockModel.count + 1 -> FullscreenToolPage(FullscreenTool.STOPWATCH)
                            else -> ClockModelPage(page - 1, now, place, Modifier.padding(horizontal = 8.dp).graphicsLayer(scaleX = FsLook.scale, scaleY = FsLook.scale))
                        }
                    }
                }
                if (duo != "off") Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    pagerView(Modifier.weight(1.3f))
                    Box(Modifier.weight(1f).height(stage), contentAlignment = Alignment.Center) {
                        FsDuoPanel(duo, now, place, stage) { onExit(); Nav.tab = it.open() }
                    }
                } else pagerView(Modifier.fillMaxWidth())
                if (FsLook.text.isNotEmpty() && !banner) Text(FsLook.text, color = Theme.c.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                for (item in items) {
                    HomeItemView(item, now, place, globeHeight.value) {
                        onExit()
                        Nav.tab = it.open()
                    }
                }
                // Ekran koruyucuda (ClockDreamService) düğme yok: dokunuş ekran koruyucuyu kapatır
                if (controls) Row(Modifier.graphicsLayer { alpha = controlsAlpha }.then(
                    if (shown) Modifier else Modifier.pointerInput(Unit) { awaitEachGesture { awaitFirstDown(pass = PointerEventPass.Initial).consume() } },
                )) {
                    // Gece modu: açıkken kapatır, kapalıyken açar (otomatik seçim Ayarlar › Saat görünümü'nde)
                    TextButton(onClick = { Prefs.saveNightMode(NightMode.toggled(night)) }) {
                        Icon(Icons.Filled.DarkMode, null, tint = if (night) Theme.c.accent else Theme.c.muted, modifier = Modifier.size(18.dp))
                        Text(" " + L("night.title"), color = Theme.c.muted, fontSize = 13.sp)
                    }
                    // Uyku zamanlayıcısı: seçilen süre sonra ekran kararır
                    Box {
                        var open by remember { mutableStateOf(false) }
                        TextButton(onClick = { open = true }) {
                            Icon(Icons.Filled.Bedtime, null, tint = if (sleepUntil > 0) Theme.c.accent else Theme.c.muted, modifier = Modifier.size(18.dp))
                            val left = (sleepUntil - now.toEpochMilli() + 999) / 1000
                            Text(" " + if (sleepUntil > 0 && left > 0) clock(left) else L("night.sleepTimer"), color = Theme.c.muted, fontSize = 13.sp, style = Tnum)
                        }
                        DropdownMenu(open, { open = false }) {
                            for (m in SleepTimer.CHOICES) DropdownMenuItem(
                                text = { Text(if (m == 0) L("alarm.snoozeOff") else T("alarm.snoozeMin", m)) },
                                onClick = { open = false; setSleep(m) })
                        }
                    }
                    // Yön kilidi: şu anki yöne kilitler
                    IconButton(onClick = { FsLook.saveLock(!FsLook.lock) }) {
                        Icon(if (FsLook.lock) Icons.Filled.ScreenLockRotation else Icons.Filled.ScreenRotation,
                            L(if (FsLook.lock) "fs.unlock" else "fs.lock"), tint = if (FsLook.lock) Theme.c.accent else Theme.c.muted, modifier = Modifier.size(20.dp))
                    }
                    TextButton(onClick = onExit) {
                        Icon(Icons.Filled.FullscreenExit, null, tint = Theme.c.muted, modifier = Modifier.size(18.dp))
                        Text(" " + L("client.exitFullscreen"), color = Theme.c.muted, fontSize = 13.sp)
                    }
                }
            }
            // Uyku zamanlayıcısı doldu: siyah ekran; dokununca saat geri gelir
            if (asleep) Box(Modifier.fillMaxSize().background(Color.Black).clickable { setSleep(0) })
        }
        // Karartma (rakip analizi #22): ekranın kenarında yukarı-aşağı kaydırma
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = FsLook.dim * 0.85f)))
        Box(
            Modifier.align(Alignment.CenterStart).width(44.dp).fillMaxHeight()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(onDragStart = { dimming = true }, onDragEnd = { dimming = false }, onDragCancel = { dimming = false }) { _, dy ->
                        FsLook.saveDim(FsLook.dim + dy / 1200f)
                    }
                },
        )
        if (dimming) Text(T("fs.dim", Fmt.percent(Math.round(FsLook.dim * 100))), Modifier.align(Alignment.Center)
            .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 8.dp),
            color = Color.White, fontSize = 17.sp, style = Tnum)
    }
    }
}

/** Tam ekranda ve yatayda durum ve gezinme çubukları gizlenir (kenardan kaydırınca geçici görünür) */
@Composable
fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/** Yatay tam ekranda saatin yanındaki alan (2. analiz #169): kayıt `fs.duo` (off, calendar, prayer, weather) */
object FsDuo {
    const val KEY = "fs.duo"
    val ALL = listOf("off", "calendar", "prayer", "weather")
    val current: String get() = Prefs.store.getString(KEY, "off")?.takeIf { it in ALL } ?: "off"
    fun save(v: String) = Prefs.store.edit().putString(KEY, v).apply()
}

@Composable
fun FsDuoPanel(kind: String, now: java.time.Instant, place: AppPlace, height: androidx.compose.ui.unit.Dp, onOpen: (HomeItem) -> Unit) {
    when (kind) {
        "calendar" -> MonthPart(now.atZone(java.time.ZoneId.systemDefault()).toLocalDate(), minOf(height * 0.92f, 420.dp))
        "prayer" -> Column(Modifier.widthIn(max = 420.dp)) {
            HomeItemView(HomeItem.NEXT, now, place, 0.dp) { onOpen(it) }
            HomeItemView(HomeItem.PRAYERS, now, place, 0.dp) { onOpen(it) }
        }
        "weather" -> Column(Modifier.widthIn(max = 420.dp)) { HomeItemView(HomeItem.WEATHER, now, place, 0.dp) { onOpen(it) } }
    }
}
