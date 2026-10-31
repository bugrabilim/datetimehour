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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    private const val NAME = "fs-photo.jpg"

    fun image(c: Context): ImageBitmap? = PhotoFile.image(c, NAME, Prefs.fsPhoto)

    fun save(c: Context, uri: Uri): Boolean =
        PhotoFile.save(c, uri, NAME, 1600).also { if (it) Prefs.saveFsPhoto(System.currentTimeMillis()) }

    fun remove(c: Context) {
        PhotoFile.remove(c, NAME)
        Prefs.saveFsPhoto(0)
    }
}

/**
 * Tam ekran saat ekleri (rakip analizi #22, #24, #25; iOS'takiyle aynı anahtarlar): saatin altındaki kişisel metin
 * (`fs.text`), saat boyutu (`fs.scale`, iki parmakla da), karartma (`fs.dim`, ekranın kenarında yukarı-aşağı kaydırma),
 * yön kilidi (`fs.lock`) ve fotoğraf slaytı (ana fotoğrafa ek en çok 10 fotoğraf `fs-slide-<i>.jpg`, `fsSlideEvery` sn).
 */
object FsLook {
    const val MAX_SLIDES = 10
    val INTERVALS = listOf(10, 30, 60, 300)
    var text by mutableStateOf(""); private set
    var scale by mutableFloatStateOf(1f); private set
    var dim by mutableFloatStateOf(0f); private set
    var lock by mutableStateOf(false); private set
    var slides by mutableIntStateOf(0); private set
    var slideVersion by mutableLongStateOf(0L); private set
    var slideEvery by mutableIntStateOf(30); private set

    fun load() {
        val s = Prefs.store
        text = s.getString("fs.text", "")!!
        scale = s.getFloat("fs.scale", 1f)
        dim = s.getFloat("fs.dim", 0f)
        lock = s.getBoolean("fs.lock", false)
        slides = s.getInt("fsSlides", 0)
        slideVersion = s.getLong("fsSlideV", 0L)
        slideEvery = s.getInt("fsSlideEvery", 30)
    }

    fun saveText(v: String) { text = v.take(60); Prefs.store.edit().putString("fs.text", text).apply() }
    fun saveScale(v: Float) { scale = v.coerceIn(0.5f, 1f); Prefs.store.edit().putFloat("fs.scale", scale).apply() }
    fun saveDim(v: Float) { dim = v.coerceIn(0f, 1f); Prefs.store.edit().putFloat("fs.dim", dim).apply() }
    fun saveLock(v: Boolean) { lock = v; Prefs.store.edit().putBoolean("fs.lock", v).apply() }
    fun saveEvery(v: Int) { slideEvery = v; Prefs.store.edit().putInt("fsSlideEvery", v).apply() }

    private fun slideName(i: Int) = "fs-slide-$i.jpg"

    fun addSlides(c: Context, uris: List<Uri>): Int {
        var n = slides
        var added = 0
        for (u in uris) {
            if (n >= MAX_SLIDES) break
            if (PhotoFile.save(c, u, slideName(n), 1600)) { n++; added++ }
        }
        slides = n
        slideVersion = System.currentTimeMillis()
        Prefs.store.edit().putInt("fsSlides", n).putLong("fsSlideV", slideVersion).apply()
        return added
    }

    fun clearSlides(c: Context) {
        for (i in 0 until MAX_SLIDES) PhotoFile.remove(c, slideName(i))
        slides = 0
        slideVersion = System.currentTimeMillis()
        Prefs.store.edit().putInt("fsSlides", 0).putLong("fsSlideV", slideVersion).apply()
    }

    val hasAny: Boolean get() = Prefs.fsPhoto > 0 || slides > 0

    /** O anda gösterilecek fotoğraf (ana fotoğraf + slaytlar sırayla) */
    fun image(c: Context, nowMs: Long): ImageBitmap? {
        val list = buildList {
            if (Prefs.fsPhoto > 0) add("fs-photo.jpg" to Prefs.fsPhoto)
            for (i in 0 until slides) add(slideName(i) to slideVersion)
        }
        if (list.isEmpty()) return null
        val i = ((nowMs / 1000 / maxOf(5, slideEvery)) % list.size).toInt()
        return PhotoFile.image(c, list[i].first, list[i].second)
    }
}

/**
 * Telefonda saklanan küçültülmüş fotoğraflar (tam ekran saat, geri sayımlar): uygulama dosyalarında JPEG. Okunan görüntü
 * sürüm numarasıyla bellekte tutulur (her saniye yenilenen ekranlar dosyayı yeniden okumasın).
 */
object PhotoFile {
    private val cache = HashMap<String, Pair<Long, ImageBitmap>>()

    private fun file(c: Context, name: String) = File(c.filesDir, name)

    fun image(c: Context, name: String, version: Long): ImageBitmap? {
        if (version == 0L) return null
        cache[name]?.let { if (it.first == version) return it.second }
        val bmp = BitmapFactory.decodeFile(file(c, name).path) ?: return null
        return bmp.asImageBitmap().also { cache[name] = version to it }
    }

    /** En uzun kenarı `maxSide` piksele küçültüp JPEG olarak yazar */
    fun save(c: Context, uri: Uri, name: String, maxSide: Int): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bmp = c.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return false
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        file(c, name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        cache.remove(name)
        true
    }.getOrDefault(false)

    fun remove(c: Context, name: String) {
        file(c, name).delete()
        cache.remove(name)
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
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && FullscreenPhoto.save(context, uri)) Prefs.saveFsBg(FullscreenBackground.PHOTO.id)
    }
    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(FsLook.MAX_SLIDES)) { uris ->
        if (uris.isNotEmpty() && FsLook.addSlides(context, uris) > 0) Prefs.saveFsBg(FullscreenBackground.PHOTO.id)
    }
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
            // Canlı örnek: seçili arka plan ve saat
            val bg = FullscreenBackground.current
            val photo = if (bg == FullscreenBackground.PHOTO) FullscreenPhoto.image(context) else null
            val colors = if (photo != null || bg.animated) AccentPalette.find(Prefs.accent).colors(true) else Theme.c
            androidx.compose.runtime.CompositionLocalProvider(LocalColors provides colors) {
                Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).background(Theme.c.bg), contentAlignment = Alignment.Center) {
                    if (photo != null) PhotoBackground(photo, Modifier.fillMaxSize())
                    else if (bg.animated) AnimatedBackground(bg, Modifier.fillMaxSize())
                    Ltr {
                        Text(Fmt.time(now.atZone(java.time.ZoneId.systemDefault())), color = Theme.c.ink, fontSize = 44.sp,
                            fontFamily = ClockFont.current.family, fontWeight = ClockFont.current.weight, style = Tnum)
                    }
                }
            }
            for (b in FullscreenBackground.entries) {
                Row(Modifier.fillMaxWidth().clickable { Prefs.saveFsBg(b.id) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(b == bg, { Prefs.saveFsBg(b.id) }, colors = RadioButtonDefaults.colors(selectedColor = Theme.c.accent))
                    Text(b.title, color = Theme.c.text, fontSize = 15.sp)
                }
            }
            Row {
                TextButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(L("look.photo.pick"), color = Theme.c.accent)
                }
                if (Prefs.fsPhoto > 0) TextButton(onClick = {
                    FullscreenPhoto.remove(context)
                    if (bg == FullscreenBackground.PHOTO && FsLook.slides == 0) Prefs.saveFsBg(FullscreenBackground.PLAIN.id)
                }) {
                    Text(L("look.photo.remove"), color = Color(0xFFD32F2F))
                }
            }
            Text(L("look.photo.note") + " " + L("look.bg.note") + " " + L("look.bg.weatherNote"), color = Theme.c.muted, fontSize = 12.sp)
        }
        FloatingClockCard()
        // Fotoğraf slaytı: ana fotoğrafa ek en çok 10 fotoğraf sırayla
        AppCard {
            SectionTitle(L("look.slideshow"))
            if (FsLook.slides < FsLook.MAX_SLIDES) TextButton(onClick = {
                pickMany.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(L("look.slideAdd"), color = Theme.c.accent) }
            if (FsLook.slides > 0) {
                Text(T("look.slideCount", FsLook.slides), color = Theme.c.muted, fontSize = 14.sp)
                Text(L("look.slideEvery"), color = Theme.c.text, fontSize = 14.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (s in FsLook.INTERVALS) {
                        val on = s == FsLook.slideEvery
                        Box(
                            Modifier.background(if (on) Theme.c.accent else Theme.c.surface2, RoundedCornerShape(50))
                                .clickable { FsLook.saveEvery(s) }.padding(horizontal = 14.dp, vertical = 8.dp),
                        ) { Text(if (s < 60) "$s ${L("common.secondsShort")}" else "${s / 60} ${L("client.minuteShort")}", color = if (on) Theme.c.bg else Theme.c.text, fontSize = 14.sp) }
                    }
                }
                TextButton(onClick = {
                    FsLook.clearSlides(context)
                    if (FullscreenBackground.current == FullscreenBackground.PHOTO && Prefs.fsPhoto == 0L) Prefs.saveFsBg(FullscreenBackground.PLAIN.id)
                }) { Text(L("look.slideClear"), color = Color(0xFFD32F2F)) }
            }
            Text(L("look.slideNote"), color = Theme.c.muted, fontSize = 12.sp)
        }
        // Saatin altındaki kişisel metin ve tam ekranda saat boyutu
        AppCard {
            SectionTitle(L("look.text"))
            androidx.compose.material3.OutlinedTextField(FsLook.text, { FsLook.saveText(it) }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text(L("look.textPlaceholder")) })
            Text(L("look.textNote"), color = Theme.c.muted, fontSize = 12.sp)
        }
        AppCard {
            SectionTitle(L("look.size"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Slider(FsLook.scale, { FsLook.saveScale(it) }, Modifier.weight(1f), valueRange = 0.5f..1f, steps = 9,
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Theme.c.accent, activeTrackColor = Theme.c.accent))
                Text(" " + Fmt.percent(Math.round(FsLook.scale * 100)), color = Theme.c.muted, style = Tnum)
            }
            Text(L("look.fsNote"), color = Theme.c.muted, fontSize = 12.sp)
        }
        // Gece / yatak başı modu (tam ekran saat)
        AppCard {
            SectionTitle(L("night.title"))
            for (m in NightMode.CHOICES) {
                Row(Modifier.fillMaxWidth().clickable { Prefs.saveNightMode(m) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(Prefs.nightMode == m, { Prefs.saveNightMode(m) }, colors = RadioButtonDefaults.colors(selectedColor = Theme.c.accent))
                    Text(L("night.$m"), color = Theme.c.text, fontSize = 15.sp)
                }
            }
            Text(L("night.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}
