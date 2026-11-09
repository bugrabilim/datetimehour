package tr.bumba.time

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/*
 * Destek ve paylaşım, yazı boyutu ve izin öncesi açıklama (3 Kasım 2026, 2. analiz #6, #77, #80, #81; sahibin ekran
 * görüntüleri). iOS'taki Support.swift ile aynı kayıt anahtarı (`fontScale`).
 */

// MARK: Destek ve paylaşım (#6, #81)

object Support {
    const val SITE = "https://time.bumba.tr"
    const val BUMBA = "https://bumbagroup.com"

    fun version(context: Context): String = runCatching {
        val i = context.packageManager.getPackageInfo(context.packageName, 0)
        "${i.versionName} (${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(i)})"
    }.getOrDefault("")

    fun shareText(): String = T("support.shareText", L("app.displayName")) + " " + SITE

    private fun open(context: Context, url: String) =
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    /** Play Store'daki sayfa (uygulama kuruluysa Play uygulamasında, değilse tarayıcıda) */
    fun rate(context: Context) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            open(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
        }
    }

    fun share(context: Context) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, L("app.displayName"))
            .putExtra(Intent.EXTRA_TEXT, shareText())
        context.startActivity(Intent.createChooser(send, L("support.share")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Geri bildirim iletisi: konu ve yalnız uygulama/sistem sürümü (kullanıcı göndermeden önce görür) */
    fun feedback(context: Context) {
        val body = "\n\n—\n${L("app.displayName")} ${version(context)} · Android ${Build.VERSION.RELEASE}"
        val uri = Uri.parse("mailto:bilgi@bumbagroup.com?subject=" + Uri.encode(T("support.feedbackSubject", L("app.displayName"))) +
            "&body=" + Uri.encode(body))
        runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun moreApps(context: Context) = open(context, BUMBA)

    /** QR kod (ZXing, telefonda üretilir): siyah kareler beyaz zemin üstünde */
    fun qr(text: String, size: Int = 720): Bitmap {
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1))
        val px = IntArray(m.width * m.height) { i -> if (m.get(i % m.width, i / m.width)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        return Bitmap.createBitmap(px, m.width, m.height, Bitmap.Config.ARGB_8888)
    }
}

/** Ayarlar'daki kart: puanla, paylaş, QR kod, geri bildirim, Bumba'nın diğer ürünleri, sürüm */
@Composable
fun SupportCard(onRoute: (String) -> Unit) {
    val context = LocalContext.current
    AppCard(padding = 4) {
        SectionTitle(L("support.title"), Modifier.padding(start = 12.dp, top = 10.dp))
        SupportRow("star", L("support.rate")) { Support.rate(context) }
        SupportRow("share", L("support.share")) { Support.share(context) }
        SupportRow("qr", L("support.qr")) { onRoute("shareQr") }
        SupportRow("mail", L("support.feedback")) { Support.feedback(context) }
        SupportRow("grid", L("support.moreApps")) { Support.moreApps(context) }
        HorizontalDivider(Modifier.padding(start = 12.dp), color = Theme.c.border.copy(alpha = 0.5f))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(L("support.version"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
            Text(Support.version(context), color = Theme.c.muted, fontSize = 15.sp)
        }
        Text(L("support.feedbackNote"), Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp), color = Theme.c.muted, fontSize = 12.sp)
    }
}

@Composable
private fun SupportRow(icon: String, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DuoChip(icon, Theme.c.gold)
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
    }
}

/** QR kodla paylaşma: kamerayla okutulunca site açılır; altında paylaş ve puanla */
@Composable
fun ShareQrScreen() {
    val context = LocalContext.current
    val bmp = remember { Support.qr(Support.SITE).asImageBitmap() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(L("app.displayName"), color = Theme.c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.background(Color.White, RoundedCornerShape(22.dp)).padding(18.dp)) {
            Image(bmp, L("support.qr"), Modifier.size(240.dp), filterQuality = FilterQuality.None)
        }
        Text("time.bumba.tr", color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
        Text(L("support.qrNote"), color = Theme.c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundButton(L("support.share"), true, Modifier.weight(1f).heightIn(min = 48.dp)) { Support.share(context) }
            RoundButton(L("support.rate"), false, Modifier.weight(1f).heightIn(min = 48.dp)) { Support.rate(context) }
        }
        TextButton(onClick = { Support.moreApps(context) }) { Text(L("support.moreApps"), color = Theme.c.accent) }
    }
}

// MARK: Yazı boyutu (#77)

/** Uygulamadaki yazılar %85–%145 (5'er adım); %100 telefonun ayarıdır (telefonun yazı ölçeğiyle çarpılır) */
object FontScale {
    const val KEY = "fontScale"
    val RANGE = 85..145
    var percent by mutableStateOf(100)
        private set
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        percent = Prefs.store.getInt(KEY, 100).coerceIn(RANGE)
    }

    fun save(v: Int) {
        percent = v.coerceIn(RANGE)
        Prefs.store.edit().putInt(KEY, percent).apply()
    }
}

/** AppTheme'in içinde: sp birimindeki bütün yazılar seçilen oranla büyür ya da küçülür */
@Composable
fun ProvideFontScale(content: @Composable () -> Unit) {
    FontScale.load()
    val d = LocalDensity.current
    if (FontScale.percent == 100) content()
    else CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * FontScale.percent / 100f), content = content)
}

/** Ayarlar'daki kart: kaydırıcı, yüzde ve canlı örnek metin */
@Composable
fun FontScaleCard() {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("fontScale.title"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
            Text(Fmt.percent(FontScale.percent), color = Theme.c.muted, fontSize = 15.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TextDecrease, null, tint = Theme.c.muted)
            Slider(
                FontScale.percent.toFloat(), { FontScale.save(Math.round(it / 5f) * 5) },
                Modifier.weight(1f).padding(horizontal = 8.dp).semantics { contentDescription = L("fontScale.title") },
                valueRange = FontScale.RANGE.first.toFloat()..FontScale.RANGE.last.toFloat(),
                steps = (FontScale.RANGE.last - FontScale.RANGE.first) / 5 - 1,
                colors = SliderDefaults.colors(thumbColor = Theme.c.accent, activeTrackColor = Theme.c.accent),
            )
            Icon(Icons.Filled.TextIncrease, null, tint = Theme.c.muted)
        }
        Text(
            L("fontScale.sample"),
            Modifier.fillMaxWidth().background(Theme.c.surface2, RoundedCornerShape(10.dp)).padding(10.dp),
            color = Theme.c.text, fontSize = 16.sp,
        )
        Text(L("fontScale.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

// MARK: İzin öncesi açıklama (#80)

/**
 * Sistemin izin penceresinden önce kısa açıklama: "Şimdi değil" ya da "İzin ver". "Şimdi değil"de sistem penceresi
 * açılmaz (izin daha sonra ilgili ayardan istenebilir).
 */
enum class PermissionKind(val key: String, val icon: String) {
    NOTIFY("notify", "bell"),
    ALARM("alarm", "alarm"),
    LOCATION("location", "pin"),
}

@Composable
fun PermissionPrimer(kind: PermissionKind, onAllow: () -> Unit, onNotNow: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNotNow,
        icon = {
            // İki tonlu simge dairede, İzinler sayfasındaki gibi adaçayı (8 Kasım 2026)
            DuoChip(kind.icon, Theme.c.sage, box = 72.dp, glyph = 40.dp)
        },
        title = { Text(L("perm.${kind.key}.title"), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        // "İzin verirseniz… vermezseniz…" ve nereden kapatılacağı (5 Kasım 2026, sahibin isteği)
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(L("perms.${kind.key}.why"), textAlign = TextAlign.Center, color = Theme.c.text, modifier = Modifier.fillMaxWidth())
                if (kind == PermissionKind.LOCATION) Text(L("onboard.trust.services"), textAlign = TextAlign.Center,
                    color = Theme.c.muted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                Text(L("onboard.trust.later"), textAlign = TextAlign.Center, color = Theme.c.muted, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            RoundButton(L("perm.allow"), true, Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = onAllow)
        },
        dismissButton = {
            TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) { Text(L("perm.notNow"), color = Theme.c.muted) }
        },
        containerColor = Theme.c.surface,
        titleContentColor = Theme.c.text,
    )
}
