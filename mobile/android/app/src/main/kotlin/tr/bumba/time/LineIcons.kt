package tr.bumba.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
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

/**
 * Uygulamanın 3D simgesi (Icons3D; 8 Kasım 2026'dan beri yalnız hava, vakit satırları, büyük çizimler ve boş durumlar);
 * görseli olmayan ad için eski çizgi çizimi. `c1`/`c2` yalnız çizgi çizimde kullanılır. `label` verilirse ekran okuyucu için ad.
 * Küçük simgeler için [DuoIcon].
 */
@Composable
fun LineIcon(name: String, size: Dp = 28.dp, c1: Color = Theme.c.accent, c2: Color = Theme.c.accent2, label: String? = null) {
    val res = Icons3D.res(name)
    if (res != 0) {
        Image(painterResource(res), contentDescription = label, modifier = Modifier.size(size))
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
 * İki tonlu küçük simgeler (8 Kasım 2026, sahibin seçimi "B Karma"; mobile/shared/icons/DUOTONE.md): Phosphor duotone
 * (MIT), assets/duotone.json (gen-strings kopyalar). Her ad iki katman: önce `o` 0,2 arka katman, sonra `o` 1 ön katman;
 * ikisi aynı renkle doldurulur (nonzero). Yollar bir kez ayrıştırılıp önbellekte tutulur. 3D simgeler yalnız hava, altı
 * vakit satırı, büyük tek çizimler ve boş durumlarda kalır.
 */
object DuoIcons {
    class Layer(val o: Float, val path: Path)

    private var all: Map<String, List<Layer>>? = null

    private fun load(): Map<String, List<Layer>> = all ?: runCatching {
        val o = JSONObject(Strings.appContext.assets.open("duotone.json").bufferedReader().use { it.readText() })
        o.keys().asSequence().filterNot { it.startsWith("_") }.associateWith { k ->
            val a = o.getJSONArray(k)
            (0 until a.length()).map { i -> a.getJSONObject(i).let { l -> Layer(l.getDouble("o").toFloat(), LineIcons.parse(l.getString("d")).first) } }
        }
    }.getOrDefault(emptyMap()).also { all = it }

    fun has(name: String): Boolean = load().containsKey(name)

    fun get(name: String): List<Layer> = load()[name].orEmpty()
}

/**
 * İki tonlu simge: 24×24 glif [size] boyutunda, iki katman aynı [color] ile (arka katman %20). Ad duotone.json'da yoksa
 * eski simge (LineIcon) çizilir ki hiçbir şey kaybolmasın. [label] yoksa ekran okuyucudan gizlidir.
 */
@Composable
fun DuoIcon(name: String, size: Dp = 24.dp, color: Color = Theme.c.accent, label: String? = null, modifier: Modifier = Modifier) {
    val layers = DuoIcons.get(name)
    if (layers.isEmpty()) {
        Box(modifier) { LineIcon(name, size, c1 = color, c2 = color, label = label) }
        return
    }
    Canvas(modifier.size(size).semantics { if (label != null) contentDescription = label }) {
        val k = this.size.minDimension / 24f
        scale(k, k, Offset.Zero) {
            for (l in layers) drawPath(l.path, color.copy(alpha = color.alpha * l.o), style = Fill)
        }
    }
}

/**
 * İki tonlu simge dairede (ayar, izin, bildirim satırları: 32/19; kareler: 42/25): zemin simge renginin açık tonu
 * (açık temada %14, koyu temada %22, kâğıt zeminin üstünde), ortada [DuoIcon].
 */
@Composable
fun DuoChip(name: String, color: Color, modifier: Modifier = Modifier, box: Dp = 32.dp, glyph: Dp = 19.dp, label: String? = null) {
    val c = Theme.c
    Box(
        modifier.size(box).clip(CircleShape).background(color.copy(alpha = if (c.dark) 0.22f else 0.14f).compositeOver(c.surface)),
        contentAlignment = Alignment.Center,
    ) { DuoIcon(name, glyph, color, label) }
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
        "Filled.Fullscreen" to "fullscreen", "Filled.Backup" to "backup", "Filled.Thermostat" to "thermometer", "Filled.Favorite" to "heart",
        // 3D simgelerle eklenenler (4 Kasım 2026)
        "Filled.FormatQuote" to "speech", "Filled.LocalCafe" to "coffee", "Filled.TableChart" to "chart",
        "Filled.CheckCircle" to "checklist", "Filled.Straighten" to "ruler", "Filled.EditNote" to "memo", "Filled.Edit" to "memo",
        "Filled.Brightness6" to "halfmoon", "Filled.CardGiftcard" to "gift", "Filled.Numbers" to "numbers",
        // İki tonlu simgelerle eklenenler (8 Kasım 2026)
        "Filled.Bed" to "bed", "Filled.Update" to "dst", "Filled.Lock" to "privacy", "Filled.Shortcut" to "shortcut",
        "Filled.QrCode2" to "qr", "Filled.Umbrella" to "umbrella",
        // Boş durumlar (3D kalır)
        "Outlined.Alarm" to "alarm", "Outlined.HourglassEmpty" to "hourglass", "Outlined.EditNote" to "memo",
        "Outlined.WhereToVote" to "pin", "Filled.DateRange" to "calendar",
    )

    fun of(vectorName: String): String? =
        byVector[vectorName]?.takeIf { DuoIcons.has(it) || Icons3D.res(it) != 0 || LineIcons.get(it).isNotEmpty() }

    /** Yalnız 3D görseli olan eşleme (boş durumlar 3D kalır) */
    fun of3D(vectorName: String): String? = byVector[vectorName]?.takeIf { Icons3D.res(it) != 0 }

    /** Alt çubuktaki sekmeler */
    fun tab(t: Tab): String = when (t) {
        Tab.CLOCK -> "clock"; Tab.WEATHER -> "sun_cloud"; Tab.CALENDAR -> "calendar"; Tab.WORLD -> "globe"
        Tab.SEASONS -> "leaf"; Tab.PRAYER -> "mosque"; Tab.TOOLS -> "grid"
    }

    /** Sekmenin konu rengi (Özellikler satırları, ilk kurulum özeti; Tümü kategorileriyle aynı) */
    @Composable
    fun tabColor(t: Tab): Color = when (t) {
        Tab.CLOCK -> Theme.c.terra; Tab.CALENDAR -> Theme.c.sage; Tab.PRAYER -> Theme.c.gold; Tab.TOOLS -> Theme.c.dusk
        Tab.WEATHER, Tab.WORLD, Tab.SEASONS -> Theme.c.sky
    }

    /** Vakit satırları (3D): imsak dağlarda şafak, güneş doğuş, öğle güneş, ikindi bulutlu güneş, akşam batış, yatsı yıldızlı gece */
    fun prayer(p: Prayer): String = when (p) {
        Prayer.FAJR -> "dawn"; Prayer.SUNRISE -> "sunrise"; Prayer.DHUHR -> "noon"
        Prayer.ASR -> "afternoon"; Prayer.MAGHRIB -> "sunset"; Prayer.ISHA -> "night"
    }
}

/**
 * Alarm listesindeki görev simgesi (2. analiz #51; 8 Kasım 2026'dan beri emoji yerine iki tonlu): matematik hesap
 * makinesi, sallama titreşim dalgası, adım sayaç, kod QR, hafıza (renk sırası) palet, yazma kalem, fotoğraf kamera,
 * çömelme kalp, nesne büyüteç, konuşma mikrofon, ışık ampul, rastgele sihirli değnek.
 */
object AlarmTaskIcon {
    fun of(task: String): String? = when (task) {
        "math" -> "calc"; "shake" -> "waveform"; "steps" -> "gauge"; "code" -> "qr"; "memory" -> "palette"
        "typing" -> "memo"; "photo" -> "camera"; "squat" -> "heart"; "object" -> "search"; "speak" -> "mic"
        "light" -> "idea"; "random" -> "wand"
        else -> null
    }
}

/** Ses türü/ruh hâli gibi listeler için kimlikler */
object LineIconSets {
    val SEASONS = listOf("flower", "sun", "leaf", "snowman")
    val MOODS = listOf("mood1", "mood2", "mood3", "mood4", "mood5")
}
