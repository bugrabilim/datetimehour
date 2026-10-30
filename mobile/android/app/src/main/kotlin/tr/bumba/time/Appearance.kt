package tr.bumba.time

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.time.ZonedDateTime

/** Saat yazı tipi (30 Ekim 2026, rakip analizi #26; iOS'taki ClockFont): dijital ve çevirmeli saat rakamları, `clockFont` */
enum class ClockFont(val id: String, val family: FontFamily, val weight: FontWeight) {
    ROUNDED("rounded", FontFamily.Default, FontWeight.Bold),
    STANDARD("standard", FontFamily.SansSerif, FontWeight.Medium),
    SERIF("serif", FontFamily.Serif, FontWeight.Bold),
    MONO("mono", FontFamily.Monospace, FontWeight.Bold),
    LIGHT("light", FontFamily.SansSerif, FontWeight.Thin);

    val title: String get() = L("look.font.$id")

    companion object {
        val current: ClockFont get() = entries.firstOrNull { it.id == Prefs.clockFont } ?: ROUNDED
    }
}

/** Tam ekran arka plan fotoğrafı: yalnız telefonda (uygulama dosyaları), en uzun kenar 1600 px JPEG */
object FullscreenPhoto {
    private fun file(c: Context) = File(c.filesDir, "fs-photo.jpg")
    private var cache: Pair<Long, ImageBitmap>? = null

    fun image(c: Context): ImageBitmap? {
        val v = Prefs.fsPhoto
        if (v == 0L) return null
        cache?.let { if (it.first == v) return it.second }
        val bmp = BitmapFactory.decodeFile(file(c).path) ?: return null
        return bmp.asImageBitmap().also { cache = v to it }
    }

    fun save(c: Context, uri: Uri): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
        val bmp = c.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return false
        val scale = minOf(1f, 1600f / maxOf(bmp.width, bmp.height))
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        file(c).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        Prefs.saveFsPhoto(System.currentTimeMillis())
        true
    }.getOrDefault(false)

    fun remove(c: Context) {
        file(c).delete()
        cache = null
        Prefs.saveFsPhoto(0)
    }
}

/** Fotoğraf arka planı (koyulaştırılmış) */
@Composable
fun PhotoBackground(img: ImageBitmap, modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
    }
}

/** Ayarlar › Saat görünümü: yazı tipi (canlı örnek) ve tam ekran fotoğrafı */
@Composable
fun AppearanceScreen() {
    val context = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) FullscreenPhoto.save(context, uri) }
    val now = rememberNow()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard {
            SectionTitle(L("look.font"))
            val f = ClockFont.current
            Ltr {
                Text(Fmt.time(now.atZone(java.time.ZoneId.systemDefault()), true), Modifier.fillMaxWidth(), color = Theme.c.ink,
                    fontSize = 52.sp, fontFamily = f.family, fontWeight = f.weight, style = Tnum, maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            for (c in ClockFont.entries) {
                Row(Modifier.fillMaxWidth().clickable { Prefs.saveClockFont(c.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(c == f, { Prefs.saveClockFont(c.id) }, colors = RadioButtonDefaults.colors(selectedColor = Theme.c.accent))
                    Text(c.title, color = Theme.c.text, fontSize = 16.sp, fontFamily = c.family, fontWeight = c.weight)
                }
            }
        }
        AppCard {
            SectionTitle(L("look.photo"))
            FullscreenPhoto.image(context)?.let { img ->
                PhotoBackground(img, Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)))
            }
            Row {
                TextButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(L("look.photo.pick"), color = Theme.c.accent)
                }
                if (Prefs.fsPhoto > 0) TextButton(onClick = { FullscreenPhoto.remove(context) }) {
                    Text(L("look.photo.remove"), color = Color(0xFFD32F2F))
                }
            }
            Text(L("look.photo.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}
