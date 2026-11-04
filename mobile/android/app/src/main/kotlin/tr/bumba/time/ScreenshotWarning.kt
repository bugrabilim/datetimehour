package tr.bumba.time

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay

/**
 * Ekran görüntüsü alınınca üstte kısa uyarı (2. rakip analizi #82).
 * Yalnız kişisel kayıt içeren ekranlarda çağrılır: namaz ve oruç takibi, zekât, sabah günlüğü.
 * Android 14 (API 34) ve sonrasında sistemin ekran görüntüsü bildirimi kullanılır
 * (DETECT_SCREEN_CAPTURE, normal izin); daha eski sürümlerde güvenilir bir yol olmadığı için uyarı çıkmaz.
 * Görüntü uygulamaya gelmez; hiçbir şey kaydedilmez ya da gönderilmez.
 */
@Composable
fun ScreenshotWarning() {
    val activity = LocalContext.current.findActivity()
    var shownAt by remember { mutableIntStateOf(0) }

    if (Build.VERSION.SDK_INT >= 34 && activity != null) {
        DisposableEffect(activity) {
            val callback = Activity.ScreenCaptureCallback { shownAt++ }
            val ok = runCatching { activity.registerScreenCaptureCallback(activity.mainExecutor, callback) }.isSuccess
            onDispose { if (ok) runCatching { activity.unregisterScreenCaptureCallback(callback) } }
        }
    }

    var visible by remember { mutableIntStateOf(0) }
    LaunchedEffect(shownAt) {
        if (shownAt == 0) return@LaunchedEffect
        visible = shownAt
        delay(5000)
        visible = 0
    }
    if (visible == 0) return

    val c = Theme.c
    Popup(alignment = Alignment.TopCenter, onDismissRequest = { visible = 0 }) {
        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(c.surface, RoundedCornerShape(16.dp))
                .border(1.dp, c.accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable { visible = 0 }
                .padding(14.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text("👁", fontSize = 16.sp)
            Text(L("screenshot.warn"), color = c.text, fontSize = 14.sp)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
