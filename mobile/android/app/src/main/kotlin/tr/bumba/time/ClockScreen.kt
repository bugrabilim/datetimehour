package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Saat modelleri: sitedeki ilk modellerden telefonda en çok kullanılanlar (iOS'ta 19 model var; Android'de sırayla eklenecek) */
private const val MODELS = 3

/**
 * Saat sekmesi: model sayfaları (dijital, rakamlı analog, analog + dijital; kaydırarak), altında tarih, hafta ve
 * sıradaki vakit. Saate dokununca saniye açılır/kapanır (dijitalde varsayılan açık, analogda kapalı).
 */
@Composable
fun ClockScreen(onFullscreen: () -> Unit, onOpen: (Tab) -> Unit) {
    val now = rememberNow()
    val zone = ZoneId.systemDefault()
    val place = AppPlace.current()
    val pager = rememberPagerState(initialPage = Prefs.clockModel.coerceIn(0, MODELS - 1)) { MODELS }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { Prefs.saveClockModel(pager.currentPage) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val stageH = (maxHeight.value * 0.48f).coerceIn(260f, 420f).dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HorizontalPager(pager, Modifier.fillMaxWidth().height(stageH)) { page ->
                ClockModel(page, now, zone, Modifier.fillMaxSize())
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage + MODELS - 1) % MODELS) } }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L("stage.prev"), tint = Theme.c.accent)
                    }
                    Text("${pager.currentPage + 1}/$MODELS", color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage + 1) % MODELS) } }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L("stage.next"), tint = Theme.c.accent)
                    }
                }
            }
            val today = now.atZone(zone).toLocalDate()
            Text(Fmt.longDate(today), color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(T("clock.weekDay", Fmt.isoWeek(today), today.dayOfYear), color = Theme.c.muted, fontSize = 14.sp)
            NextPrayerCard(place, now, Modifier.clickable { onOpen(Tab.PRAYER) })
            TextButton(onClick = onFullscreen) {
                Icon(Icons.Filled.Fullscreen, null, tint = Theme.c.accent)
                Text(" " + L("clock.fullscreen"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Sıradaki vakit ve kalan süre */
@Composable
fun NextPrayerCard(place: AppPlace, now: Instant, modifier: Modifier = Modifier) {
    val next = place.next(now) ?: return
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(L("home.item.next"), color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${next.prayer.title} ${Fmt.hm(next.at, place.zone)}", color = Theme.c.text, fontSize = 17.sp)
            }
            Text(
                Fmt.countdown(next.at.epochSecond - now.epochSecond),
                color = Theme.c.accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, style = Tnum,
            )
        }
    }
}

@Composable
fun ClockModel(model: Int, now: Instant, zone: ZoneId, modifier: Modifier = Modifier) {
    val t = now.atZone(zone)
    Box(modifier, contentAlignment = Alignment.Center) {
        when (model) {
            0 -> DigitalClock(t, 1f)
            1 -> AnalogClock(t, Modifier.fillMaxSize(0.95f))
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalogClock(t, Modifier.weight(1f).aspectRatio(1f))
                DigitalClock(t, 0.45f)
            }
        }
    }
}

/** Büyük dijital saat; dokununca saniye açılır/kapanır */
@Composable
fun DigitalClock(t: java.time.ZonedDateTime, scale: Float) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val text = Fmt.time(t, Prefs.secD)
        val size = min(maxWidth.value / (text.length * 0.62f), 140f) * scale
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.clickable(remember { MutableInteractionSource() }, null) { Prefs.saveSecD(!Prefs.secD) },
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(text, color = Theme.c.ink, fontSize = size.sp, fontWeight = FontWeight.Bold, style = Tnum)
                if (Prefs.h12) Text(" " + Fmt.ampm(t), color = Theme.c.muted, fontSize = (size * 0.3f).sp, modifier = Modifier.padding(bottom = (size * 0.15f).dp))
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
            .clickable(remember { MutableInteractionSource() }, null) { Prefs.saveSecA(!Prefs.secA) },
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
        val style = TextStyle(color = c.ink, fontSize = (15 * u / density).sp, fontWeight = FontWeight.SemiBold)
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

/** Tam ekran: seçili model ekranı kaplar; dokununca çıkılır, ekran kapanmaz */
@Composable
fun FullscreenClock(onExit: () -> Unit) {
    val now = rememberNow()
    Box(Modifier.fillMaxSize().background(Theme.c.bg).padding(16.dp)) {
        ClockModel(Prefs.clockModel, now, ZoneId.systemDefault(), Modifier.fillMaxSize().padding(bottom = 40.dp))
        TextButton(onClick = onExit, modifier = Modifier.align(Alignment.BottomCenter)) {
            Icon(Icons.Filled.FullscreenExit, null, tint = Theme.c.muted, modifier = Modifier.size(18.dp))
            Text(" " + L("client.exitFullscreen"), color = Theme.c.muted)
        }
    }
}
