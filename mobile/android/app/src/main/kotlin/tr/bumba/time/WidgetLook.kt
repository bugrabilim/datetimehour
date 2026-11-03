package tr.bumba.time

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

/**
 * Saat widget'larının görünümü (2. analiz #17; iOS'taki WidgetStyle ile aynı adlar ve renkler): hazır renk takımları
 * ve çerçeve. Widget eklenirken ya da sonradan (uzun basıp "Widget'ı düzenle") WidgetStyleActivity ile seçilir;
 * seçim `widget.style.<id>`, `widget.frame.<id>`, iki saatte şehir `widget.city.<id>`. "app" sistemin açık/koyu
 * temasına uyan varsayılan renklerdir. Yazı tipi widget'ta seçilemez (RemoteViews yazı tipini değiştiremez).
 */
enum class WidgetStyle(val id: String, val bg: Int, val text: Int, val muted: Int, val face: Int, val border: Int, val accent: Int, val second: Int) {
    APP("app", 0, 0, 0, 0, 0, 0, 0),
    PAPER("paper", 0xF7F4EE, 0x1B1F27, 0x565C68, 0xEFEBE2, 0xD6CFBF, 0x9A3F00, 0x9A3F00),
    NIGHT("night", 0x000000, 0xEEF0F4, 0xA5ACB9, 0x15181E, 0x343B49, 0xF2A65A, 0xFF6B6B),
    MINT("mint", 0xE3F4EA, 0x12351F, 0x3F6B50, 0xCDEBD8, 0x9CCFB0, 0x1B7A43, 0xD84315),
    ROSE("rose", 0xFBE9EF, 0x3A1424, 0x7A4A5C, 0xF6D3DF, 0xE6A9BF, 0xAD1457, 0x8D6E63),
    OCEAN("ocean", 0x0B2540, 0xE6F1FF, 0x9FB8D6, 0x12345A, 0x2B5580, 0x5AA9FF, 0x4DD0E1),
    MONO("mono", 0xFFFFFF, 0x000000, 0x555555, 0xF2F2F2, 0x000000, 0x000000, 0x000000),
    NEON("neon", 0x0A0A12, 0xF0F0FF, 0x9A9AC0, 0x15152A, 0x3A1F6E, 0x39FF14, 0xFF2BD6);

    val title: String get() = L("widget.style.$id")

    companion object {
        fun of(id: Int): WidgetStyle = Prefs.store.getString("widget.style.$id", "app").let { s -> entries.firstOrNull { it.id == s } ?: APP }
    }
}

object WidgetLook {
    private fun argb(rgb: Int) = 0xFF000000.toInt() or rgb

    fun frame(id: Int) = Prefs.store.getBoolean("widget.frame.$id", false)

    /** Seçilen görünümün rengi: "app" ise sistemin teması (kaynaklardaki widget_* renkleri) */
    fun colors(c: Context, s: WidgetStyle): IntArray = if (s == WidgetStyle.APP) intArrayOf(
        c.getColor(R.color.widget_bg), c.getColor(R.color.widget_text), c.getColor(R.color.widget_muted), c.getColor(R.color.widget_face),
        c.getColor(R.color.widget_border), c.getColor(R.color.widget_accent), c.getColor(R.color.widget_accent),
    ) else intArrayOf(argb(s.bg), argb(s.text), argb(s.muted), argb(s.face), argb(s.border), argb(s.accent), argb(s.second))

    /** Zemin (ve çerçeve) ile yazı renklerini uygular */
    fun apply(c: Context, v: RemoteViews, id: Int, root: Int, texts: List<Int> = emptyList(), muted: List<Int> = emptyList()) {
        val s = WidgetStyle.of(id)
        val f = frame(id)
        if (s != WidgetStyle.APP || f) {
            val res = c.resources.getIdentifier("widget_bg_${s.id}${if (f) "_frame" else ""}", "drawable", c.packageName)
            if (res != 0) v.setInt(root, "setBackgroundResource", res)
        }
        if (s == WidgetStyle.APP) return
        val k = colors(c, s)
        texts.forEach { v.setTextColor(it, k[1]) }
        muted.forEach { v.setTextColor(it, k[2]) }
    }

    /** Analog kadran (Android 12+): rakamlı kadran ve ibre renkleri seçilen görünümden */
    fun analog(c: Context, v: RemoteViews, viewId: Int, id: Int) {
        if (Build.VERSION.SDK_INT < 31) return
        val s = WidgetStyle.of(id)
        v.setIcon(viewId, "setDial", Icon.createWithBitmap(dial(c, s)))
        if (s != WidgetStyle.APP) {
            val k = colors(c, s)
            v.setColorStateList(viewId, "setHourHandTintList", ColorStateList.valueOf(k[1]))
            v.setColorStateList(viewId, "setMinuteHandTintList", ColorStateList.valueOf(k[5]))
            v.setColorStateList(viewId, "setSecondHandTintList", ColorStateList.valueOf(k[6]))
        }
    }

    /** Rakamlı kadran (uygulamadaki AnalogClock ölçüleri; 200 birim) */
    fun dial(c: Context, s: WidgetStyle): Bitmap {
        if (s == WidgetStyle.APP) return WidgetUpdater.dial(c)
        val k = colors(c, s)
        val px = 400
        val u = px / 200f
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = k[3]
        cv.drawCircle(100 * u, 100 * u, 94 * u, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2 * u
        p.color = k[4]
        cv.drawCircle(100 * u, 100 * u, 94 * u, p)
        p.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 60) {
            val major = i % 5 == 0
            val a = Math.toRadians(i * 6.0)
            val r1 = if (major) 88f else 86f
            val r2 = if (major) 76f else 81f
            p.strokeWidth = (if (major) 3f else 1.4f) * u
            p.color = if (major) k[1] else k[2]
            cv.drawLine((100 + r1 * sin(a)).toFloat() * u, (100 - r1 * cos(a)).toFloat() * u,
                (100 + r2 * sin(a)).toFloat() * u, (100 - r2 * cos(a)).toFloat() * u, p)
        }
        p.style = Paint.Style.FILL
        p.color = k[1]
        p.textSize = 15 * u
        p.isFakeBoldText = true
        p.textAlign = Paint.Align.CENTER
        for (n in 1..12) {
            val a = Math.toRadians(n * 30.0)
            cv.drawText(n.toString(), (100 + 66 * sin(a)).toFloat() * u, (100 - 66 * cos(a)).toFloat() * u - (p.ascent() + p.descent()) / 2, p)
        }
        return b
    }

    /** İki saatte ikinci şehir: widget ayarındaki şehir, yoksa uygulamadaki ikili saat seçimi */
    fun city(id: Int): City? = Prefs.store.getString("widget.city.$id", null)?.let { k -> City.all.firstOrNull { it.key == k } } ?: ClockLook.dual()

    fun forget(e: android.content.SharedPreferences.Editor, id: Int) {
        e.remove("widget.style.$id").remove("widget.frame.$id").remove("widget.city.$id")
    }
}

/** İki saat (2. analiz #12): solda burası, sağda seçilen şehir (kısa kod ve fark) */
class DualClockWidget : BaseWidget() {
    override fun views(c: Context, id: Int): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_dual)
        val city = WidgetLook.city(id)
        val zone = city?.zone ?: ZoneId.systemDefault()
        val now = Instant.now()
        WidgetLook.analog(c, v, R.id.analog1, id)
        if (Build.VERSION.SDK_INT >= 31) {
            WidgetLook.analog(c, v, R.id.analog2, id)
            v.setString(R.id.analog2, "setTimeZone", zone.id)
        } else {
            // Android 12 öncesinde AnalogClock saat dilimi almaz: ikinci şehir dijital
            v.setViewVisibility(R.id.analog2, View.GONE)
            v.setViewVisibility(R.id.digital2, View.VISIBLE)
            v.setString(R.id.digital2, "setTimeZone", zone.id)
        }
        v.setTextViewText(R.id.label1, AppPlace.current().name.ifEmpty { L("dual.here") })
        v.setTextViewText(R.id.label2, city?.let { ZoneCaption.code(it.key) + " " + ZoneCaption.diff(zone, ZoneId.systemDefault(), now) } ?: "")
        WidgetLook.apply(c, v, id, R.id.root, texts = listOf(R.id.label1, R.id.label2, R.id.digital2))
        v.setOnClickPendingIntent(R.id.root, WidgetUpdater.openApp(c, Tab.CLOCK, id))
        return v
    }
}

/** Saat widget'larının görünüm seçimi (renk takımı galerisi, çerçeve; iki saatte şehir) */
class WidgetStyleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Seçim isteğe bağlı: vazgeçilse de widget eklenir (varsayılan görünümle)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val mgr = AppWidgetManager.getInstance(this)
        val provider = mgr.getAppWidgetInfo(id)?.provider?.className ?: ""
        val dual = provider.endsWith("DualClockWidget")
        fun done() {
            WidgetUpdater.updateAll(this)
            finish()
        }
        setContent {
            AppTheme {
                var style by remember { mutableStateOf(WidgetStyle.of(id)) }
                var frame by remember { mutableStateOf(WidgetLook.frame(id)) }
                var city by remember { mutableStateOf(WidgetLook.city(id)?.key ?: "") }
                fun save() {
                    Prefs.store.edit().putString("widget.style.$id", style.id).putBoolean("widget.frame.$id", frame)
                        .apply { if (dual && city.isNotEmpty()) putString("widget.city.$id", city) }.apply()
                }
                Column(Modifier.fillMaxSize().background(Theme.c.bg).statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(L("widget.style"), Modifier.weight(1f), color = Theme.c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { save(); done() }) { Text(L("common.done"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold) }
                    }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                        items(WidgetStyle.entries) { s ->
                            val k = WidgetLook.colors(this@WidgetStyleActivity, s)
                            Row(Modifier.fillMaxWidth().clickable { style = s }.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(width = 64.dp, height = 40.dp).background(Color(k[0]), RoundedCornerShape(10.dp))
                                    .border(if (frame) 3.dp else 1.dp, Color(if (frame) k[5] else k[4]), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center) {
                                    Text("12:00", color = Color(k[1]), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(s.title, Modifier.weight(1f), color = if (s == style) Theme.c.accent else Theme.c.text,
                                    fontWeight = if (s == style) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp)
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth().clickable { frame = !frame }.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(L("widget.frame"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                                Switch(frame, { frame = it }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                            }
                            HorizontalDivider(color = Theme.c.border.copy(alpha = 0.4f))
                            if (dual) Text(L("widget.city"), Modifier.padding(16.dp), color = Theme.c.muted, fontSize = 14.sp)
                        }
                        if (dual) items(City.all) { cty ->
                            Text(cty.displayName + "  " + ZoneCaption.diff(cty.zone, ZoneId.systemDefault(), Instant.now()),
                                Modifier.fillMaxWidth().clickable { city = cty.key }.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = if (cty.key == city) Theme.c.accent else Theme.c.text,
                                fontWeight = if (cty.key == city) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
