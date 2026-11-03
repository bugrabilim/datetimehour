package tr.bumba.time

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    const val COLOR_KEY = "night.color"
    const val FROM_KEY = "night.from"
    const val TO_KEY = "night.to"
    /** Seçenekler: kapalı, açık, Güneş batınca, belirli saatlerde (2. analiz #11); ışık sensörü varsa ortam karanlıkken (#189) */
    fun choices(hasLight: Boolean) = listOf("off", "on", "auto", "schedule") + if (hasLight) listOf("light") else emptyList()

    /**
     * Gece yazı rengi (2. analiz #189): çarpım rengi, beyaz → kısık renk, siyah → siyah (zemin tam siyah kalsın diye
     * kısıklık saydamlıkla değil renkle). Varsayılan kırmızı (gece görüşünü en az bozan). iOS'taki NightMode ile aynı.
     */
    val COLORS = linkedMapOf(
        "red" to 0xB82016L, "amber" to 0xB87A16L, "orange" to 0xB85016L, "green" to 0x2E8B3AL,
        "aqua" to 0x1E8C8CL, "blue" to 0x2A5BB8L, "white" to 0x8A8A8AL,
    )
    val colorId: String get() = Prefs.store.getString(COLOR_KEY, "red")?.takeIf { it in COLORS } ?: "red"
    val TINT: Color get() = Color(0xFF000000 or COLORS.getValue(colorId))

    /** Belirli saatler (gece yarısından dakika): varsayılan 22.00 → 07.00 */
    val from: Int get() = Prefs.store.getInt(FROM_KEY, 22 * 60)
    val to: Int get() = Prefs.store.getInt(TO_KEY, 7 * 60)
    fun inSchedule(minute: Int, from: Int, to: Int): Boolean =
        if (from == to) false else if (from < to) minute in from until to else minute >= from || minute < to

    /** Gün doğumu/batımı eşiği (kırılma + Güneş yarıçapı); "light" modunda ortam ışığı (dark) belirler */
    fun active(raw: String, lat: Double, lon: Double, ms: Long, dark: Boolean = false): Boolean = when (raw) {
        "on" -> true
        "auto" -> SkyMath.sunAltitude(lat, lon, ms) < -0.833
        "schedule" -> java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).let { inSchedule(it.hour * 60 + it.minute, from, to) }
        "light" -> dark
        else -> false
    }

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

/** Tam ekran saatte uyku zamanlayıcısı: bitiş anı (Unix ms, 0 = kapalı); tam ekrandan çıkınca silinir */
object SleepTimer {
    const val KEY = "night.sleepUntil"
    val CHOICES = listOf(0, 15, 30, 60, 90)
}

/**
 * Ortam ışığı (2. analiz #189): ışık sensörü 5 lux'un altına inince karanlık, 15 lux'u geçince aydınlık sayılır
 * (gidip gelmesin diye iki eşik). Yalnız "light" seçiliyken ve tam ekran açıkken dinlenir.
 */
@Composable
fun rememberAmbientDark(enabled: Boolean): Boolean {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var dark by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    DisposableEffect(enabled) {
        val sm = ctx.getSystemService(android.hardware.SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(android.hardware.Sensor.TYPE_LIGHT)
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(e: android.hardware.SensorEvent) {
                val lux = e.values.firstOrNull() ?: return
                dark = if (dark) lux < 15f else lux < 5f
            }
            override fun onAccuracyChanged(s: android.hardware.Sensor?, a: Int) {}
        }
        if (enabled && sensor != null) sm.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm?.unregisterListener(listener) }
    }
    return dark
}

fun hasLightSensor(ctx: android.content.Context): Boolean =
    ctx.getSystemService(android.hardware.SensorManager::class.java)?.getDefaultSensor(android.hardware.Sensor.TYPE_LIGHT) != null
