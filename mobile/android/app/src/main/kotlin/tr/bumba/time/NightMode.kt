package tr.bumba.time

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

/**
 * Gece / yatak başı modu (rakip analizi #1): tam ekran saatte siyah zemin, kırmızı ve kısık yazı (gece görüşünü ve
 * uykuyu az bozar), ekran parlaklığı düşer, ekran yanmasın diye içerik dakikada bir birkaç nokta kayar.
 * Seçim `nightMode`: "off" (varsayılan), "on", "auto" (seçili yerde Güneş ufkun altındayken). iOS'taki NightMode ile aynı.
 */
object NightMode {
    const val KEY = "nightMode"
    val CHOICES = listOf("off", "on", "auto")
    /** Çarpım rengi: beyaz → kısık kırmızı, siyah → siyah (zemin tam siyah kalsın diye kısıklık saydamlıkla değil renkle) */
    val TINT = Color(0xFFB82016)

    /** Gün doğumu/batımı eşiği (kırılma + Güneş yarıçapı) */
    fun active(raw: String, lat: Double, lon: Double, ms: Long): Boolean =
        raw == "on" || (raw == "auto" && SkyMath.sunAltitude(lat, lon, ms) < -0.833)

    /** Dakikaya göre kayma (dp): her biri −6…6, iki platformda aynı */
    fun shift(ms: Long): Pair<Int, Int> {
        val k = ms / 60_000
        return ((k * 7) % 13 - 6).toInt() to ((k * 11) % 13 - 6).toInt()
    }

    /** Bir sonraki seçim (tam ekrandaki ay düğmesi): açıkken kapatır, kapalıyken açar */
    fun toggled(active: Boolean): String = if (active) "off" else "on"
}

/** İçeriği kırmızıya çevirir: ekran dışı katmanda siyah zemin + çarpım (beyaz yazı kırmızı, siyah siyah kalır) */
fun Modifier.nightTint(on: Boolean): Modifier = if (!on) this else this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawRect(Color.Black)
        drawContent()
        drawRect(NightMode.TINT, blendMode = BlendMode.Multiply)
    }

/** Gece modunda pencere parlaklığı düşer; çıkınca sistem ayarına döner */
@Composable
fun NightBrightness(on: Boolean) = ScreenBrightness(if (on) 0.05f else null)

/** Pencerenin ekran parlaklığı; null ise telefonun kendi ayarı (gece modu 0,05; gün doğumu ışığında yükselir) */
@Composable
fun ScreenBrightness(level: Float?) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    DisposableEffect(window, level) {
        window?.attributes = window?.attributes?.apply { screenBrightness = level ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
        onDispose {
            window?.attributes = window?.attributes?.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
        }
    }
}
