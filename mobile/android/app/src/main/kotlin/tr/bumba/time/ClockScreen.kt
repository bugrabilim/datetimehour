package tr.bumba.time

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
fun ClockScreen(onFullscreen: () -> Unit, onOpen: (Tab) -> Unit) {
    val now = rememberNow()
    val place = AppPlace.current()
    val items = HomeItems.layout.visible
    var customizing by remember { mutableStateOf(false) }
    val pager = rememberClockPager()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewport = maxHeight
        val globeHeight = rememberGlobeHeight()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                    .fitGlobe(viewport, globeHeight, HomeItem.GLOBE in items),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalPager(pager, Modifier.fillMaxWidth().height(stageHeight(viewport))) { page ->
                        ClockModelPage(page, now, place, Modifier.padding(horizontal = 2.dp))
                    }
                    StageControls(pager)
                }
                for (item in items) {
                    HomeItemView(item, now, place, globeHeight.value) { onOpen(it.open()) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    TextButton(onClick = onFullscreen) {
                        Icon(Icons.Filled.Fullscreen, null, tint = Theme.c.accent)
                        Text(" " + L("clock.fullscreen"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { customizing = true }) {
                        Icon(Icons.Filled.Tune, null, tint = Theme.c.accent)
                        Text(" " + L("home.edit"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                    }
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

/** Sahnenin altında ‹ 1/19 ›: sayaca dokununca model listesi açılır */
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
                Text(
                    "${pager.currentPage + 1}/$n",
                    Modifier.clickable { menu = true }.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = tint, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, style = Tnum,
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
fun AnalogClock(t: java.time.ZonedDateTime, modifier: Modifier = Modifier) {
    val c = Theme.c
    val measurer = rememberTextMeasurer()
    val sec = t.second.toDouble()
    val min = t.minute + sec / 60
    val hour = (t.hour % 12) + min / 60
    Canvas(
        modifier.aspectRatio(1f).widthIn(max = 520.dp)
            .clickable(remember { MutableInteractionSource() }, null) { Prefs.saveSecA(!Prefs.secA); BadgeStore.hit(Badge.SANIYE) },
    ) {
        val u = min(size.width, size.height) / 200f
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(c.surface2, 94 * u, center)
        drawCircle(c.border, 94 * u, center, style = Stroke(2 * u))
        for (i in 0 until 60) {
            val major = i % 5 == 0
            rotate(i * 6f, center) {
                drawLine(
                    if (major) c.text else c.muted,
                    Offset(center.x, center.y - (if (major) 88 else 86) * u),
                    Offset(center.x, center.y - (if (major) 76 else 81) * u),
                    (if (major) 3f else 1.4f) * u, StrokeCap.Round,
                )
            }
        }
        val style = TextStyle(color = c.ink, fontSize = (15 * u / density / fontScale).sp, fontWeight = FontWeight.SemiBold)
        for (n in 1..12) {
            val a = n * 30 * Math.PI / 180
            val layout = measurer.measure(n.toString(), style)
            drawText(
                layout,
                topLeft = Offset(
                    center.x + 66 * u * sin(a).toFloat() - layout.size.width / 2f,
                    center.y - 66 * u * cos(a).toFloat() - layout.size.height / 2f,
                ),
            )
        }
        fun hand(angle: Double, from: Float, to: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
            rotate(angle.toFloat(), center) {
                drawLine(color, Offset(center.x, center.y + from * u), Offset(center.x, center.y - to * u), width * u, StrokeCap.Round)
            }
        }
        // Akrep yazı renginde, yelkovan temanın ana renginde, saniye ve orta temanın ikinci renginde
        hand(hour * 30, 4f, 44f, 6f, c.ink)
        hand(min * 6, 6f, 66f, 4.5f, c.accent)
        if (Prefs.secA) hand(sec * 6, 16f, 76f, 2f, c.accent2)
        drawCircle(c.accent2, 5 * u, center)
    }
}

/**
 * Tam ekran saat: 19 model yatay kaydırılır, altında saat ekranındaki alanlar (Düzenle ile seçilenler); ekran kapanmaz,
 * çift dokununca ya da düğmeyle çıkılır. Bir alana dokununca tam ekran kapanır, sayfası açılır.
 */
@Composable
fun FullscreenClock(onExit: () -> Unit) {
    val now = rememberNow()
    val place = AppPlace.current()
    val items = HomeItems.layout.visible
    val pager = rememberClockPager()
    HideSystemBars()
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Theme.c.bg).pointerInput(Unit) { detectTapGestures(onDoubleTap = { onExit() }) },
    ) {
        val viewport = maxHeight
        val globeHeight = rememberGlobeHeight()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .fitGlobe(viewport, globeHeight, HomeItem.GLOBE in items),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val stage = if (items.isEmpty()) maxOf(300.dp, viewport - 60.dp) else stageHeight(viewport * 1.08f)
                HorizontalPager(pager, Modifier.fillMaxWidth().height(stage)) { page ->
                    ClockModelPage(page, now, place, Modifier.padding(horizontal = 8.dp))
                }
                for (item in items) {
                    HomeItemView(item, now, place, globeHeight.value) {
                        onExit()
                        Nav.tab = it.open()
                    }
                }
                TextButton(onClick = onExit) {
                    Icon(Icons.Filled.FullscreenExit, null, tint = Theme.c.muted, modifier = Modifier.size(18.dp))
                    Text(" " + L("client.exitFullscreen"), color = Theme.c.muted, fontSize = 13.sp)
                }
            }
        }
    }
}

/** Tam ekranda durum ve gezinme çubukları gizlenir (kenardan kaydırınca geçici görünür) */
@Composable
private fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
