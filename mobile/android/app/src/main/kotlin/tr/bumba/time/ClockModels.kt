package tr.bumba.time

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle as JTextStyle

/**
 * Sitedeki saat sahnesinin 20 modeli (build.mjs STAGE_ORDER; iOS'taki ClockModel ile aynı sıra ve kimlikler).
 * Adlar sitedeki `stage.models.<kimlik>` metinleri. Hava durumlu (wx) modeller seçili konumun havasını alır.
 */
enum class ClockModel(val id: String) {
    CLASSIC("classic"), // 1. sırada: varsayılan saniyeli rakamlı analog (30 Ekim 2026, sahibin isteği)
    WX_DIGITAL_CAL_WEEK("wx-digital-cal-week"),
    ANALOG_DATE("analog-date"), STACK("stack"),
    WX_ANALOG("wx-analog"), WX_STACK("wx-stack"), WX_ANALOG_DATE("wx-analog-date"), WX_FULL("wx-full"),
    H_CLASSIC("h-classic"), H_ANALOG_DATE("h-analog-date"), H_STACK("h-stack"),
    H_WX_ANALOG("h-wx-analog"), H_WX_STACK("h-wx-stack"), H_WX_ANALOG_DATE("h-wx-analog-date"), H_WX_FULL("h-wx-full"),
    DIGITAL("digital"), DIGITAL_DATE("digital-date"), WX_DIGITAL("wx-digital"), WX_DIGITAL_CAL("wx-digital-cal"),
    FLIP("flip"), // 20: çevirmeli saat (30 Ekim 2026, rakip analizi #5)
    DUAL("dual"); // 21: ikili saat dilimi (2. analiz #12; yalnız uygulamalarda, sitede yok; sona eklendi, sıra numarası bozulmaz)

    val title: String get() = if (this == DUAL) L("dual.model") else L("stage.models.$id")
    val number: Int get() = ordinal + 1
    val horizontal: Boolean get() = id.startsWith("h-")

    companion object {
        val count: Int get() = entries.size
        fun at(i: Int): ClockModel = entries[i.coerceIn(0, entries.size - 1)]
    }
}

/** Ekran görüntüsü testleri için örnek hava verisi (ağ yok); uygulamada hep null */
internal var weatherOverride: WeatherData? = null

/** dp cinsinden ölçüyü yazı boyutuna çevirir (sistem yazı büyütmesi saat çizimini taşırmasın) */
@Composable
private fun Dp.asSp(): TextUnit = with(LocalDensity.current) { this@asSp.toSp() }

/** Bir modelin çizimi; verilen alana sığacak şekilde ölçeklenir (sahne ve tam ekran) */
@Composable
fun ClockModelView(model: ClockModel, now: Instant, place: AppPlace, modifier: Modifier = Modifier) {
    val t = now.atZone(ZoneId.systemDefault())
    BoxWithConstraints(modifier.semantics { contentDescription = model.title }, contentAlignment = Alignment.Center) {
        val w = maxWidth
        val h = maxHeight
        val s = minOf(w, h)
        val center = Arrangement.spacedBy(s * 0.03f, Alignment.CenterVertically)
        when (model) {
            ClockModel.CLASSIC -> AnalogPart(t, s * 0.95f)
            ClockModel.DUAL -> DualClockPart(now, place, w, h)
            ClockModel.ANALOG_DATE -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s * 0.04f)) {
                AnalogPart(t, s * 0.78f)
                DatePart(t, s * 0.065f)
            }
            ClockModel.STACK -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = center) {
                AnalogPart(t, s * 0.66f)
                DigitalPart(t, s * 0.13f, w)
            }
            ClockModel.WX_ANALOG -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s * 0.04f)) {
                AnalogPart(t, s * 0.66f)
                WeatherPart(WxKind.LINE, place, s)
            }
            ClockModel.WX_STACK -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s * 0.025f)) {
                AnalogPart(t, s * 0.48f)
                DigitalPart(t, s * 0.1f, w)
                WeatherPart(WxKind.CARD, place, s)
            }
            ClockModel.WX_ANALOG_DATE -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = center) {
                AnalogPart(t, s * 0.58f)
                DatePart(t, s * 0.06f)
                WeatherPart(WxKind.LINE, place, s)
            }
            ClockModel.WX_FULL -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s * 0.025f)) {
                AnalogPart(t, s * 0.48f)
                DigitalPart(t, s * 0.1f, w)
                DatePart(t, s * 0.055f)
                WeatherPart(WxKind.LINE, place, s)
            }
            ClockModel.H_CLASSIC, ClockModel.H_ANALOG_DATE, ClockModel.H_STACK, ClockModel.H_WX_ANALOG, ClockModel.H_WX_STACK,
            ClockModel.H_WX_ANALOG_DATE, ClockModel.H_WX_FULL -> Horizontal(model, t, place, w, h)
            ClockModel.DIGITAL -> DigitalPart(t, minOf(w * 0.2f, h * 0.35f), w)
            ClockModel.DIGITAL_DATE -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s * 0.04f)) {
                DigitalPart(t, minOf(w * 0.19f, h * 0.3f), w)
                DatePart(t, s * 0.07f)
            }
            ClockModel.WX_DIGITAL -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s * 0.04f)) {
                DigitalPart(t, minOf(w * 0.18f, h * 0.26f), w)
                DatePart(t, s * 0.065f)
                WeatherPart(WxKind.LINE, place, s)
            }
            ClockModel.WX_DIGITAL_CAL -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(w * 0.04f)) {
                Column(Modifier.width(w * 0.42f), verticalArrangement = Arrangement.spacedBy(s * 0.03f)) {
                    DigitalPart(t, minOf(w * 0.085f, h * 0.16f), w * 0.42f)
                    DatePart(t, s * 0.05f, start = true)
                    WeatherPart(WxKind.LINE, place, s * 0.85f, start = true)
                }
                MonthPart(t.toLocalDate(), w * 0.54f)
            }
            ClockModel.FLIP -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s * 0.05f)) {
                FlipPart(t, w * 0.92f, h * 0.55f)
                DatePart(t, s * 0.065f)
            }
            ClockModel.WX_DIGITAL_CAL_WEEK -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = center) {
                DigitalPart(t, minOf(w * 0.19f, h * 0.2f), w)
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(w * 0.04f)) {
                    Column(Modifier.width(w * 0.42f), verticalArrangement = Arrangement.spacedBy(s * 0.025f)) {
                        DatePart(t, s * 0.048f, start = true)
                        WeatherPart(WxKind.WEEK, place, s * 0.85f, start = true)
                    }
                    MonthPart(t.toLocalDate(), w * 0.54f)
                }
            }
        }
    }
}

/** 9–15: solda analog, sağda bilgi */
@Composable
private fun Horizontal(model: ClockModel, t: ZonedDateTime, place: AppPlace, w: Dp, h: Dp) {
    val a = minOf(w * 0.5f, h * 0.9f)
    val s = minOf(w, h)
    val info = w - a - w * 0.05f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(w * 0.05f)) {
        AnalogPart(t, a)
        Column(Modifier.widthIn(max = info), verticalArrangement = Arrangement.spacedBy(s * 0.03f)) {
            when (model) {
                ClockModel.H_CLASSIC -> Text(zoneName(t), color = Theme.c.muted, fontSize = (s * 0.06f).asSp(), fontWeight = FontWeight.Medium)
                ClockModel.H_ANALOG_DATE -> DatePart(t, s * 0.065f, start = true)
                ClockModel.H_STACK -> DigitalPart(t, s * 0.12f, info)
                ClockModel.H_WX_ANALOG -> WeatherPart(WxKind.LINE, place, s, start = true)
                ClockModel.H_WX_STACK -> {
                    DigitalPart(t, s * 0.11f, info)
                    WeatherPart(WxKind.LINE, place, s, start = true)
                }
                ClockModel.H_WX_ANALOG_DATE -> {
                    DatePart(t, s * 0.055f, start = true)
                    WeatherPart(WxKind.LINE, place, s, start = true)
                }
                else -> {
                    DigitalPart(t, s * 0.1f, info)
                    DatePart(t, s * 0.05f, start = true)
                    WeatherPart(WxKind.LINE, place, s, start = true)
                }
            }
        }
    }
}

/** Cihazın saat diliminin adı ("Türkiye Saati" gibi) */
private fun zoneName(t: ZonedDateTime): String =
    t.zone.getDisplayName(JTextStyle.FULL, Strings.locale).takeIf { it.isNotBlank() } ?: t.zone.id

// Parçalar

/** Analog saat; dokununca analog saniye açılır/kapanır (sitedeki gibi: dokunulan saatin türü) */
@Composable
private fun AnalogPart(t: ZonedDateTime, size: Dp) {
    AnalogClock(t, Modifier.size(size))
}

/** Dijital saat; dokununca dijital saniye açılır/kapanır. Sığmazsa küçülür. */
@Composable
private fun DigitalPart(t: ZonedDateTime, size: Dp, maxWidth: Dp) {
    val max = size.asSp()
    // Dijital rakam görünümü (2. analiz #9): LED, LCD, nokta matris, silindir, dikey
    val style = DigitalStyle.current
    if (style != "plain") {
        val tap = Modifier.clickable(remember { MutableInteractionSource() }, null) { Prefs.saveSecD(!Prefs.secD); BadgeStore.hit(Badge.SANIYE) }
        Ltr {
            Row(tap.widthIn(max = maxWidth), verticalAlignment = Alignment.CenterVertically) {
                if (style == "vertical") {
                    val hh = Fmt.time(t).substringBefore(':')
                    val mm = "%02d".format(t.minute)
                    VerticalDigits(hh, mm, if (Prefs.secD) "%02d".format(t.second) else null, size * 1.6f, ClockLook.digitInk(Theme.c), ClockFont.current.family)
                } else {
                    StyledDigits(Fmt.time(t, Prefs.secD).filter { it.isDigit() || it == ':' }, style, size * 0.82f, maxWidth, ClockLook.digitInk(Theme.c))
                }
                if (Prefs.h12) Text(" " + Fmt.ampm(t), color = Theme.c.text, fontSize = (max.value * 0.3f).sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        return
    }
    Ltr {
        Row(
            Modifier.widthIn(max = maxWidth).clickable(remember { MutableInteractionSource() }, null) { Prefs.saveSecD(!Prefs.secD); BadgeStore.hit(Badge.SANIYE) },
            verticalAlignment = Alignment.Bottom,
        ) {
            // Saniye küçük ve tabana hizalı (2. analiz #21): tek metin, saniye kısmı küçük yazı
            val text = if (Prefs.secD && ClockLook.secSmall) androidx.compose.ui.text.buildAnnotatedString {
                append(Fmt.time(t))
                pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = androidx.compose.ui.unit.TextUnit(0.45f, androidx.compose.ui.unit.TextUnitType.Em)))
                append(":%02d".format(t.second))
                pop()
            } else androidx.compose.ui.text.AnnotatedString(Fmt.time(t, Prefs.secD))
            // Rakam rengi ve dış hatlı rakam (2. analiz #24)
            val stroke = with(androidx.compose.ui.platform.LocalDensity.current) { maxOf(1.5f, (size * 0.012f).toPx()) }
            Text(
                text, Modifier.weight(1f, fill = false), color = ClockLook.digitInk(Theme.c),
                fontFamily = ClockFont.current.family, fontWeight = ClockFont.current.weight,
                style = if (ClockLook.outline) Tnum.copy(drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(stroke)) else Tnum,
                maxLines = 1, softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = (max.value * 0.3f).sp, maxFontSize = max, stepSize = 1.sp),
            )
            if (Prefs.h12) {
                Text(" " + Fmt.ampm(t), color = Theme.c.text, fontSize = (max.value * 0.3f).sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1)
            }
        }
    }
}

/**
 * Çevirmeli saatin görünümü (2. analiz #20): iki rakam tek kartta (`flip.pair`), köşede küçük saniye (`flip.smallSec`),
 * kart ortasındaki çizgi (`flip.hinge`, varsayılan açık), kart rengi (`flip.card`: tema, siyah, beyaz, kırmızı, mavi,
 * yeşil). iOS'taki FlipLook ile aynı anahtarlar.
 */
object FlipLook {
    const val PAIR_KEY = "flip.pair"
    const val SMALL_SEC_KEY = "flip.smallSec"
    const val HINGE_KEY = "flip.hinge"
    const val CARD_KEY = "flip.card"
    /** Kart rengi: kimlik → (kart, rakam); "theme" uygulamanın renkleri */
    val CARDS = linkedMapOf(
        "theme" to (0L to 0L), "black" to (0x1A1B1EL to 0xF5F5F5L), "white" to (0xF7F7F5L to 0x1A1B1EL),
        "red" to (0xA61E1EL to 0xFFFFFFL), "blue" to (0x1F4E9CL to 0xFFFFFFL), "green" to (0x1E6B34L to 0xFFFFFFL),
    )
    val pair: Boolean get() = Prefs.store.getBoolean(PAIR_KEY, false)
    val smallSec: Boolean get() = Prefs.store.getBoolean(SMALL_SEC_KEY, false)
    val hinge: Boolean get() = Prefs.store.getBoolean(HINGE_KEY, true)
    val card: String get() = Prefs.store.getString(CARD_KEY, "theme")?.takeIf { it in CARDS } ?: "theme"
    fun colors(id: String, c: AppColors): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> =
        if (id == "theme") c.surface2 to c.ink
        else CARDS.getValue(id).let { (bg, fg) -> androidx.compose.ui.graphics.Color(0xFF000000 or bg) to androidx.compose.ui.graphics.Color(0xFF000000 or fg) }
}

/**
 * Çevirmeli saat: her rakam (ya da iki rakam) bir kart, rakam değişince kart döner. Dokununca dijital saniye açılır/kapanır.
 * Sahne enine göre uzunsa (dik tam ekran) saat ve dakika kartları alt alta dizilir; 12 saat biçiminde ÖÖ/ÖS saat kartının
 * köşesinde, küçük saniye seçiliyse dakika kartının köşesinde yazar.
 */
@Composable
private fun FlipPart(t: ZonedDateTime, width: Dp, height: Dp) {
    val secs = Prefs.secD
    val pair = FlipLook.pair
    val small = secs && FlipLook.smallSec
    val hour = if (Prefs.h12) (t.hour % 12).let { if (it == 0) 12 else it } else t.hour
    val groups = listOf(hour, t.minute) + if (secs && !small) listOf(t.second) else emptyList()
    val vertical = height > width * 1.1f
    val perGroup = if (pair) 1.7f else 2.08f
    val card = if (vertical) minOf(width / (perGroup + 0.2f), height / (groups.size * 1.4f + (groups.size - 1) * 0.25f))
        else minOf(width / (groups.size * perGroup + (groups.size - 1) * 0.35f), height / 1.4f)
    val (bg, fg) = FlipLook.colors(FlipLook.card, Theme.c)
    val ampm = if (Prefs.h12) Fmt.ampm(t) else ""
    @Composable
    fun group(i: Int, v: Int) {
        val corner = when {
            i == 0 && ampm.isNotEmpty() -> ampm
            i == 1 && small -> "%02d".format(java.util.Locale.ROOT, t.second)
            else -> ""
        }
        if (pair) FlipCard("%02d".format(java.util.Locale.ROOT, v), card * 1.7f, card, "g$i", bg, fg, corner, i == 0)
        else Row(horizontalArrangement = Arrangement.spacedBy(card * 0.08f)) {
            FlipCard("${v / 10}", card, card, "g$i-0", bg, fg, if (i == 0) corner else "", true)
            FlipCard("${v % 10}", card, card, "g$i-1", bg, fg, if (i != 0) corner else "", false)
        }
    }
    Ltr {
        val mod = Modifier.clickable(remember { MutableInteractionSource() }, null) { Prefs.saveSecD(!secs); BadgeStore.hit(Badge.SANIYE) }
            .semantics(mergeDescendants = true) { contentDescription = Fmt.time(t, secs) }
        if (vertical) Column(mod, verticalArrangement = Arrangement.spacedBy(card * 0.25f), horizontalAlignment = Alignment.CenterHorizontally) {
            for ((i, v) in groups.withIndex()) group(i, v)
        } else Row(mod, horizontalArrangement = Arrangement.spacedBy(card * 0.35f), verticalAlignment = Alignment.CenterVertically) {
            for ((i, v) in groups.withIndex()) group(i, v)
        }
    }
}

/** Tek kart: metin (bir ya da iki rakam), köşe etiketi (ÖÖ/ÖS solda üstte, küçük saniye sağda altta) */
@Composable
private fun FlipCard(
    text: String, width: Dp, digit: Dp, key: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color,
    corner: String, cornerStart: Boolean,
) {
    val angle = remember(key) { Animatable(0f) }
    var last by remember(key) { mutableStateOf(text) }
    LaunchedEffect(text) {
        if (text != last) {
            last = text
            angle.snapTo(-80f)
            angle.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        }
    }
    val r = RoundedCornerShape(digit * 0.12f)
    Box(
        Modifier.size(width, digit * 1.4f).shadow(digit * 0.05f, r).clip(r).background(bg).border(1.dp, Theme.c.border.copy(alpha = 0.6f), r),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            Modifier.graphicsLayer { rotationX = angle.value; cameraDistance = 12f * density; alpha = if (angle.value == 0f) 1f else 0.4f },
            color = fg, fontSize = digit.asSp(), fontFamily = ClockFont.current.family, fontWeight = ClockFont.current.weight,
            style = Tnum, maxLines = 1,
        )
        if (FlipLook.hinge) Box(Modifier.fillMaxWidth().height(maxOf(1.dp, digit * 0.03f)).background(Theme.c.bg))
        if (corner.isNotEmpty()) Text(
            corner, Modifier.align(if (cornerStart) Alignment.TopStart else Alignment.BottomEnd).padding(digit * 0.07f),
            color = fg.copy(alpha = 0.75f), fontSize = (digit * 0.2f).asSp(), fontWeight = FontWeight.Bold, style = Tnum, maxLines = 1,
        )
    }
}

@Composable
private fun DatePart(t: ZonedDateTime, size: Dp, start: Boolean = false) {
    val f = size.asSp()
    Text(
        Fmt.longDate(t.toLocalDate()), color = Theme.c.text, fontWeight = FontWeight.SemiBold, maxLines = 2,
        textAlign = if (start) TextAlign.Start else TextAlign.Center,
        autoSize = TextAutoSize.StepBased(minFontSize = (f.value * 0.6f).sp, maxFontSize = f, stepSize = 0.5.sp),
    )
}

enum class WxKind { LINE, CARD, WEEK }

/**
 * Hava bloğu: LINE (simge, sıcaklık, durum · yer), CARD (+ hissedilen, en yüksek/düşük, nem, rüzgâr),
 * WEEK (+ önümüzdeki 6 gün). Veri gelene kadar sade bir yer tutucu (yükleme halkası yok).
 */
@Composable
private fun WeatherPart(kind: WxKind, place: AppPlace, size: Dp, start: Boolean = false) {
    val d = weatherOverride ?: rememberWeather(place.lat, place.lon)
    Prefs.units
    val f = maxOf(11.dp, size * 0.042f).asSp()
    val align = if (start) Alignment.Start else Alignment.CenterHorizontally
    Column(horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (d == null) {
            Text("— °", color = Theme.c.muted, fontSize = f * 1.7f, fontWeight = FontWeight.SemiBold)
            Text(L("weather.loading"), color = Theme.c.muted, fontSize = f * 0.9f, maxLines = 1)
            return@Column
        }
        val c = d.current
        Ltr {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(d.condition.icon(c.isDay) + " ", fontSize = f * 1.5f)
                Text(Units.temp(c.temp), color = Theme.c.text, fontSize = f * 1.7f, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(d.condition.label + " · " + place.name, color = Theme.c.muted, fontSize = f, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (kind == WxKind.CARD) {
            val today = d.today
            Text(L("weather.feelsLike") + " " + Units.temp(c.feels) + (today?.let { " · " + Units.temp(it.max) + " / " + Units.temp(it.min) } ?: ""),
                color = Theme.c.muted, fontSize = f * 0.9f, maxLines = 1)
            Text(L("weather.humidity") + " " + Fmt.percent(Math.round(c.humidity).toInt()) + " · " + L("weather.wind") + " " + Units.wind(c.wind),
                color = Theme.c.muted, fontSize = f * 0.9f, maxLines = 1)
        }
        if (kind == WxKind.WEEK) {
            for (day in d.daily.drop(1).take(6)) {
                Ltr {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Fmt.weekdayShort(day.date), Modifier.width(f.value.dp * 2.8f), color = Theme.c.text, fontSize = f, maxLines = 1)
                        Text(WeatherCode.of(day.code).icon(), Modifier.width(f.value.dp * 1.9f), fontSize = f)
                        Text(Units.temp(day.max) + " ", color = Theme.c.text, fontSize = f, fontWeight = FontWeight.SemiBold)
                        Text(Units.temp(day.min), color = Theme.c.muted, fontSize = f)
                    }
                }
            }
        }
        // Open-Meteo atfı (CC BY 4.0)
        Text("Open-Meteo.com", color = Theme.c.muted.copy(alpha = 0.8f), fontSize = f * 0.7f, maxLines = 1)
    }
}

/** Aylık takvim (pazartesi başlar; bugün vurgulu, tatiller renkli). Güne dokununca Takvim sekmesinde açılır. */
@Composable
private fun MonthPart(today: LocalDate, width: Dp) {
    val month = YearMonth.from(today)
    val lead = month.atDay(1).dayOfWeek.value - 1
    val cell = width / 7
    val f = (cell * 0.42f).asSp()
    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(cell * 0.08f)) {
        Text("${Fmt.month(month.monthValue)} ${month.year}", color = Theme.c.text, fontSize = f * 1.15f, fontWeight = FontWeight.SemiBold)
        Row {
            for (i in 0..6) {
                Text(L("calendar.weekdaysShort.$i"), Modifier.width(cell), color = if (i >= 5) Theme.c.accent else Theme.c.muted,
                    fontSize = f * 0.8f, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
        val rows = (lead + month.lengthOfMonth() + 6) / 7
        for (r in 0 until rows) {
            Row {
                for (c in 0..6) {
                    val n = r * 7 + c - lead + 1
                    Box(Modifier.size(cell, cell * 0.92f), contentAlignment = Alignment.Center) {
                        if (n in 1..month.lengthOfMonth()) {
                            val d = month.atDay(n)
                            val isToday = d == today
                            val holiday = AppData.events[d].orEmpty().any { it.type == "holiday" }
                            Box(
                                Modifier.size(cell * 0.86f)
                                    .background(if (isToday) Theme.c.accent else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                                    .clickable {
                                        Nav.calendarDay = d
                                        Nav.tab = Tab.CALENDAR
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$n", style = Tnum, fontSize = f, maxLines = 1,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isToday -> Theme.c.surface
                                        holiday -> AppData.color("holiday")
                                        else -> Theme.c.text
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Sahne yüksekliği ekrana göre (küçük telefonda da saat, alanlar ve dünya bir ekrana sığsın; iOS'taki gibi) */
fun stageHeight(viewport: Dp): Dp = (viewport * 0.46f).coerceIn(280.dp, 380.dp)

/** Saat ekranında kullanılan ortak model sayfası (pager içinde) */
@Composable
fun ClockModelPage(index: Int, now: Instant, place: AppPlace, modifier: Modifier = Modifier) {
    ClockModelView(ClockModel.at(index), now, place, modifier.fillMaxSize())
}
