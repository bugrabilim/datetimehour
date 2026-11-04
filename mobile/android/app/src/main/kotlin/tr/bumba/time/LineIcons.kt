package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import tr.bumba.time.core.Prayer

/**
 * Saat Tarih'in kendi çizgi simgeleri (2. analiz #97; 4 Kasım 2026): 24 simge, 24×24 alan, 2 pt çizgi, ana renk +
 * ikinci renk; temaya göre renk değişir. Tanımlar mobile/shared/icons/icons.json (gen-strings kopyalar; iOS'taki
 * LineIcon aynı dosyayı okur). Komutlar: M L C Z ve O (daire).
 */
object LineIcons {
    class Part(val second: Boolean, val fill: Boolean, val d: String)

    private var all: Map<String, List<Part>>? = null

    fun get(name: String): List<Part> {
        val m = all ?: runCatching {
            val o = JSONObject(Strings.appContext.assets.open("icons.json").bufferedReader().use { it.readText() })
            o.keys().asSequence().associateWith { k ->
                val a = o.getJSONArray(k)
                (0 until a.length()).map { i -> a.getJSONArray(i).let { p -> Part(p.getString(0).startsWith("c2"), p.getString(0).endsWith("f"), p.getString(1)) } }
            }
        }.getOrDefault(emptyMap()).also { all = it }
        return m[name].orEmpty()
    }

    /** Yol metnini Compose yollarına çevirir (daireler ayrı) */
    fun parse(d: String): Pair<Path, List<Triple<Float, Float, Float>>> {
        val path = Path()
        val circles = ArrayList<Triple<Float, Float, Float>>()
        val tok = d.replace(",", " ").replace(Regex("([MLCZO])"), " $1 ").trim().split(Regex("\\s+"))
        var i = 0
        fun num() = tok[i++].toFloat()
        fun numeric() = i < tok.size && tok[i].first().let { it.isDigit() || it == '-' || it == '.' }
        while (i < tok.size) {
            when (tok[i++]) {
                "M" -> path.moveTo(num(), num())
                "L" -> { path.lineTo(num(), num()); while (numeric()) path.lineTo(num(), num()) }
                "C" -> path.cubicTo(num(), num(), num(), num(), num(), num())
                "Z" -> path.close()
                "O" -> while (numeric()) circles += Triple(num(), num(), num())
            }
        }
        return path to circles
    }
}

/**
 * Gerçekçi 3D simgeler (4 Kasım 2026, sahibin seçimi: "çizgi simge istemiyorum, gerçekçi bulut gibi"): Microsoft Fluent
 * Emoji 3D (MIT), mobile/shared/icons3d/gen-icons3d.py üretir (res/drawable-nodpi/e3d_<ad>.png). Adlar çizgi
 * simgelerle aynı; hava için ek adlar: sun_small_cloud, sun_cloud, sun_rain.
 */
object Icons3D {
    private val cache = HashMap<String, Int>()

    /** Görselin kaynak kimliği (yoksa 0) */
    fun res(name: String): Int = cache.getOrPut(name) {
        runCatching {
            val c = Strings.appContext
            c.resources.getIdentifier("e3d_$name", "drawable", c.packageName)
        }.getOrDefault(0)
    }
}

/** Gri ve soluk (alt çubukta seçili olmayan sekme, bitmemiş pomodoro turu) */
private val GRAY = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Uygulamanın simgesi: 3D görsel (Icons3D); görseli olmayan ad için eski çizgi çizimi. `dim`: gri ve soluk.
 * `c1`/`c2` yalnız çizgi çizimde kullanılır. `label` verilirse ekran okuyucu için ad.
 */
@Composable
fun LineIcon(name: String, size: Dp = 28.dp, c1: Color = Theme.c.accent, c2: Color = Theme.c.accent2, label: String? = null,
             dim: Boolean = false) {
    val res = Icons3D.res(name)
    if (res != 0) {
        Image(painterResource(res), contentDescription = label,
            modifier = Modifier.size(size).graphicsLayer { alpha = if (dim) 0.45f else 1f },
            colorFilter = if (dim) GRAY else null)
        return
    }
    val parts = LineIcons.get(name)
    Canvas(Modifier.size(size).semantics { if (label != null) contentDescription = label }) {
        val k = this.size.minDimension / 24f
        scale(k, k, Offset.Zero) {
            for (p in parts) {
                val color = if (p.second) c2 else c1
                val (path, circles) = LineIcons.parse(p.d)
                val style = if (p.fill) Fill else Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(path, color, style = style)
                for ((cx, cy, r) in circles) drawCircle(color, r, Offset(cx, cy), style = style)
            }
        }
    }
}

/**
 * Material simgesinden kendi çizgi simgemize eşleme (doğal tasarım madde 4, 4 Kasım 2026): IconBox ve alt çubuk önce
 * buraya bakar, karşılığı yoksa Material simgesini çizer. Anahtar ImageVector.name ("Filled.Alarm" gibi).
 */
object LineIconMap {
    private val byVector = mapOf(
        "Filled.Alarm" to "alarm", "Filled.HourglassBottom" to "hourglass", "Filled.Timer" to "stopwatch", "Filled.Spa" to "tomato",
        "Filled.School" to "cap", "Filled.Bedtime" to "bed", "Filled.EmojiNature" to "chick", "Filled.Air" to "wind",
        "Filled.Games" to "knight", "Filled.MusicNote" to "metronome", "Filled.RecordVoiceOver" to "speaker", "Filled.Quiz" to "paper",
        "Filled.EmojiPeople" to "people", "Filled.Groups" to "people", "Filled.MoreTime" to "calc", "Filled.Calculate" to "calc",
        "Filled.CalendarMonth" to "calendar", "Filled.Event" to "calendar", "Filled.EditCalendar" to "calendar", "Filled.Article" to "leafcal",
        "Filled.WbTwilight" to "sunrise", "Filled.BeachAccess" to "beach", "Filled.Elderly" to "beach", "Filled.WbSunny" to "sun",
        "Filled.LightMode" to "sun", "Filled.Public" to "globe", "Filled.Language" to "globe", "Filled.Eco" to "leaf",
        "Filled.Brightness3" to "moon", "Filled.DarkMode" to "night", "Filled.NightsStay" to "night", "Filled.AutoAwesome" to "sparkles",
        "Filled.Stars" to "star", "Filled.Celebration" to "party", "Filled.MilitaryTech" to "medal", "Filled.Star" to "star",
        "Filled.Explore" to "compass", "AutoMirrored.Filled.MenuBook" to "book", "Filled.MenuBook" to "book", "Filled.TouchApp" to "beads",
        "Filled.TaskAlt" to "checklist", "Filled.VolunteerActivism" to "hands", "Filled.SelfImprovement" to "mosque",
        "Filled.Settings" to "gear", "Filled.Notifications" to "bell", "Filled.NotificationsActive" to "bell", "Outlined.Notifications" to "bell",
        "Filled.LocationOn" to "pin", "Filled.Palette" to "palette", "Filled.ColorLens" to "palette", "Filled.TextFields" to "textsize",
        "Filled.Tune" to "sliders", "Filled.Info" to "info", "Filled.Share" to "share", "Filled.QrCode" to "qr", "Filled.Email" to "mail",
        "Filled.Search" to "search", "Filled.Apps" to "grid", "Filled.Widgets" to "grid", "Filled.Watch" to "clock", "Filled.Schedule" to "clock",
        "Filled.Fullscreen" to "fullscreen", "Filled.Backup" to "cloud", "Filled.Thermostat" to "thermometer", "Filled.Favorite" to "heart",
        // 3D simgelerle eklenenler (4 Kasım 2026)
        "Filled.FormatQuote" to "speech", "Filled.LocalCafe" to "coffee", "Filled.TableChart" to "chart",
        "Filled.CheckCircle" to "checklist", "Filled.Straighten" to "ruler", "Filled.EditNote" to "memo", "Filled.Edit" to "memo",
        "Filled.Brightness6" to "halfmoon", "Filled.CardGiftcard" to "gift", "Filled.Numbers" to "numbers",
    )

    fun of(vectorName: String): String? = byVector[vectorName]?.takeIf { Icons3D.res(it) != 0 || LineIcons.get(it).isNotEmpty() }

    /** Alt çubuktaki sekmeler */
    fun tab(t: Tab): String = when (t) {
        Tab.CLOCK -> "clock"; Tab.WEATHER -> "sun_cloud"; Tab.CALENDAR -> "calendar"; Tab.WORLD -> "globe"
        Tab.SEASONS -> "leaf"; Tab.PRAYER -> "mosque"; Tab.TOOLS -> "grid"
    }

    /** Vakit satırları (3D): imsak dağlarda şafak, güneş doğuş, öğle güneş, ikindi bulutlu güneş, akşam batış, yatsı yıldızlı gece */
    fun prayer(p: Prayer): String = when (p) {
        Prayer.FAJR -> "dawn"; Prayer.SUNRISE -> "sunrise"; Prayer.DHUHR -> "noon"
        Prayer.ASR -> "afternoon"; Prayer.MAGHRIB -> "sunset"; Prayer.ISHA -> "night"
    }
}

/** Ses türü/ruh hâli gibi listeler için kimlikler */
object LineIconSets {
    val SEASONS = listOf("flower", "sun", "leaf", "snowman")
    val MOODS = listOf("mood1", "mood2", "mood3", "mood4", "mood5")
}
