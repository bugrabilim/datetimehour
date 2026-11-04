package tr.bumba.time

import androidx.compose.runtime.key
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.cos
import kotlin.math.sin

/**
 * Kadran koleksiyonu (3 Kasım 2026, 2. analiz #1; sahibin ekran görüntüleri): rakamlı analog saatlerin kadranı.
 * 12 kadran: klasik, Roma rakamı, sade, noktalı, kronograf, çift halka, metal çerçeve, radar, Çin rakamı ve dokulu
 * zeminler (deniz, yaprak, mermer); lüm (2. analiz #11); 4 Kasım 2026'da sahibin isteğiyle ("yetersiz kadran var") 12 yeni:
 * Doğu Arap rakamı, pilot, Bauhaus, güneş ışını, eski cep saati, dalgıç, gökkuşağı (çocuklar), neon, ahşap, İznik çini,
 * geometrik (sekiz köşeli yıldız), sektör. Görsel dosya yok, hepsi çizim. Her kadranda 5 renk (ibre ve saniye; "auto"
 * kadranın kendi rengi). Seçim `dial.face`, `dial.color`; galeri açılınca "Yeni" işareti ve saat ekranındaki tanıtım
 * kalkar (`dial.seen2`; yeni kadranlar için 4 Kasım 2026'da yeniden gösterilir). iOS'taki Dials.swift ile aynı adlar ve ölçüler (200 birimlik kadran).
 */
enum class DialFace(val id: String) {
    CLASSIC("classic"), ROMAN("roman"), MINIMAL("minimal"), DOTS("dots"), CHRONO("chrono"), DOUBLE("double"),
    METAL("metal"), RADAR("radar"), CHINESE("chinese"), OCEAN("ocean"), LEAF("leaf"), MARBLE("marble"),
    /** Lüm (2. analiz #11): siyah kadranda işaretler ve ibreler seçilen renkte ışır (14 renk, `dial.lume`) */
    LUME("lume"),
    EASTERN("eastern"), PILOT("pilot"), BAUHAUS("bauhaus"), SUNBURST("sunburst"), VINTAGE("vintage"), DIVER("diver"),
    RAINBOW("rainbow"), NEON("neon"), WOOD("wood"), IZNIK("iznik"), GEOMETRIC("geometric"), SECTOR("sector");

    val title: String get() = L("dial.$id")

    /** QUARTERS: yalnız 12, 3, 6, 9 */
    enum class Numerals { ARABIC, ROMAN, CHINESE, EASTERN, QUARTERS, NONE }
    /** NONE: kadran kendi işaretlerini çizer (dalgıç, gökkuşağı, İznik, geometrik) */
    enum class Ticks { FULL, HOURS, DOTS, NONE }

    val numerals: Numerals get() = when (this) {
        CLASSIC, CHRONO, DOUBLE, OCEAN, MARBLE, LUME, PILOT, VINTAGE, RAINBOW, IZNIK, SECTOR -> Numerals.ARABIC
        ROMAN, WOOD -> Numerals.ROMAN
        CHINESE -> Numerals.CHINESE
        EASTERN -> Numerals.EASTERN
        BAUHAUS -> Numerals.QUARTERS
        else -> Numerals.NONE
    }

    val ticks: Ticks get() = when (this) {
        MINIMAL, METAL, LEAF, LUME, SUNBURST, NEON, WOOD -> Ticks.HOURS
        DOTS -> Ticks.DOTS
        RAINBOW, IZNIK, GEOMETRIC, DIVER -> Ticks.NONE
        else -> Ticks.FULL
    }

    /** Kadranın renkleri: yüz, çerçeve, büyük çizgi, küçük çizgi, rakam/akrep, yelkovan, saniye */
    data class Colors(val face: Color, val border: Color, val tick: Color, val minor: Color, val ink: Color, val hand: Color, val second: Color)

    fun colors(c: AppColors): Colors {
        fun k(v: Long) = Color(0xFF000000 or v)
        return when (this) {
            METAL -> Colors(k(0x1E2228), k(0xADB5BD), k(0xE9ECEF), k(0x868E96), k(0xF1F3F5), k(0xF2A65A), k(0xFF6B6B))
            RADAR -> Colors(k(0x04140A), k(0x2B8A3E), k(0x51CF66), k(0x2B8A3E), k(0x8CE99A), k(0x51CF66), k(0xB2F2BB))
            OCEAN -> Colors(k(0x0B3D91), k(0x74C0FC), Color.White, k(0xA5D8FF), Color.White, k(0xFFD43B), k(0xFF8787))
            LEAF -> Colors(k(0x1E6B34), k(0x8CE99A), Color.White, k(0xB2F2BB), Color.White, k(0xFFD43B), k(0xFF8787))
            MARBLE -> Colors(k(0xF1EEE9), k(0xBDB5A6), k(0x343A40), k(0x868E96), k(0x212529), k(0x9A3F00), k(0xC92A2A))
            CHINESE -> Colors(c.surface2, c.border, c.text, c.muted, c.ink, c.accent, k(0xC92A2A))
            LUME -> Lume.current.let { g -> Colors(k(0x07080B), g.copy(alpha = 0.45f), g, g.copy(alpha = 0.55f), g, g, k(0xFF6B6B)) }
            PILOT -> Colors(k(0x15171A), k(0x3A3F45), k(0xF1F3F5), k(0x8D949B), k(0xF8F9FA), k(0xF8F9FA), k(0xFF922B))
            BAUHAUS -> Colors(k(0xF4F1EA), k(0x1A1A1A), k(0x1A1A1A), k(0x8C8C8C), k(0x1A1A1A), k(0x1A1A1A), k(0xE03131))
            SUNBURST -> Colors(k(0x2546A8), k(0xC7D2FE), k(0xF1F3F5), k(0xA5B4FC), k(0xF8F9FA), k(0xE9ECEF), k(0xFFD43B))
            VINTAGE -> Colors(k(0xF3E9D2), k(0x8C7A5B), k(0x3B2F1E), k(0x7A6A50), k(0x2B2116), k(0x1E3A8A), k(0x8B1E1E))
            DIVER -> Colors(k(0x0B1220), k(0x334155), k(0xE6F4EA), k(0x94A3B8), k(0xE6F4EA), k(0xE6F4EA), k(0xFF6B6B))
            RAINBOW -> Colors(k(0xFFFFFF), k(0x495057), k(0x343A40), k(0xADB5BD), k(0x212529), k(0x343A40), k(0xE03131))
            NEON -> Colors(k(0x0A0A14), k(0x3BF0FF), k(0xFF4FD8), k(0x7A3E8C), k(0x3BF0FF), k(0x3BF0FF), k(0xFF4FD8))
            WOOD -> Colors(k(0x7A4A24), k(0xE0B65A), k(0xF1D38A), k(0xC9A35A), k(0xF6E3B0), k(0xF1D38A), k(0xE8590C))
            IZNIK -> Colors(k(0xFBFBF8), k(0x1F4E9C), k(0x1F4E9C), k(0x6C8FC9), k(0x1F4E9C), k(0x173B78), k(0xC0392B))
            GEOMETRIC -> Colors(k(0x0F3B2E), k(0xD4AF37), k(0xD4AF37), k(0x8C7A3A), k(0xF3E3A6), k(0xD4AF37), k(0xF8F9FA))
            SECTOR -> Colors(k(0xEEE8DA), k(0x1B3A5C), k(0x1B3A5C), k(0x6B7F95), k(0x1B3A5C), k(0x1B3A5C), k(0xD9480F))
            else -> Colors(c.surface2, c.border, c.text, c.muted, c.ink, c.accent, c.accent2)
        }
    }

    companion object {
        const val KEY = "dial.face"
        const val COLOR_KEY = "dial.color"
        const val SEEN_KEY = "dial.seen2"
        val ROMAN_N = listOf("XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")
        val CHINESE_N = listOf("十二", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一")
        val EASTERN_N = listOf("١٢", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "١٠", "١١")
        /** Gökkuşağı kadranının saat dilimleri (pastel) */
        val RAINBOW_C = listOf(0xFFC9C9L, 0xFFD8A8L, 0xFFEC99L, 0xD8F5A2L, 0xB2F2BBL, 0x96F2D7L, 0x99E9F2L, 0xA5D8FFL, 0xBAC8FFL, 0xD0BFFFL, 0xEEBEFAL, 0xFCC2D7L)

        fun of(id: String?): DialFace = entries.firstOrNull { it.id == id } ?: CLASSIC
        val current: DialFace get() = of(Prefs.store.getString(KEY, null))
    }
}

/** Lüm kadranının ışıma renkleri (2. analiz #11): siyah üstünde parlak tonlar; adları `color.<id>` */
object Lume {
    const val KEY = "dial.lume"
    val COLORS = linkedMapOf(
        "green" to 0x7CFC8AL, "mint" to 0x98F5C9L, "aqua" to 0x5CE1E6L, "blue" to 0x6FA8FFL, "ice" to 0xCDEBFFL, "white" to 0xF2F2F2L,
        "lemon" to 0xF5F57AL, "yellow" to 0xFFD54FL, "orange" to 0xFFA94DL, "amber" to 0xFFBF47L, "red" to 0xFF6B6BL, "pink" to 0xFF8FC7L,
        "violet" to 0xB48CFFL, "lilac" to 0xD9B8FFL,
    )
    fun color(id: String?): Color = Color(0xFF000000 or (COLORS[id] ?: COLORS.getValue("green")))
    val id: String get() = Prefs.store.getString(KEY, "green")?.takeIf { it in COLORS } ?: "green"
    val current: Color get() = color(id)
}

/** İbre rengi: "auto" kadranın kendi rengi */
enum class DialColor(val id: String, val rgb: Long) {
    AUTO("auto", 0), RED("red", 0xE03131), BLUE("blue", 0x1C7ED6), GREEN("green", 0x2F9E44), GOLD("gold", 0xE8A317);

    val color: Color? get() = if (this == AUTO) null else Color(0xFF000000 or rgb)

    fun apply(k: DialFace.Colors): DialFace.Colors = color?.let { k.copy(hand = it, second = it) } ?: k

    companion object {
        fun of(id: String?): DialColor = entries.firstOrNull { it.id == id } ?: AUTO
        val current: DialColor get() = of(Prefs.store.getString(DialFace.COLOR_KEY, null))
    }
}

/**
 * Kadran (zemin, doku, çizgiler, rakamlar, süsler; ibreler hariç). `hour24` 0–24, `minute` ve `sec` kesirli.
 * Saat ekranındaki AnalogClock ve galeri bunu çizer.
 */
fun DrawScope.drawDial(face: DialFace, k: DialFace.Colors, measurer: TextMeasurer, hour24: Double, minute: Double, sec: Double, fontScale: Float) {
    val u = kotlin.math.min(size.width, size.height) / 200f
    val center = Offset(size.width / 2, size.height / 2)
    val r = 94 * u
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, r)) }
    fun k(v: Long) = Color(0xFF000000 or v)
    // Zemin ve doku (dairenin içine kırpılır)
    clipPath(circle) {
        when (face) {
            DialFace.OCEAN -> {
                drawRect(Brush.verticalGradient(listOf(k(0x1971C2), k(0x0B3D91)), center.y - r, center.y + r))
                for (i in 0 until 7) {
                    val y0 = center.y - r + 2 * r * (0.2f + 0.1f * i)
                    val p = Path()
                    var x = center.x - r
                    p.moveTo(x, y0)
                    while (x <= center.x + r) {
                        p.lineTo(x, y0 + (sin((x - center.x + r) / (2 * r) * 12 + i) * 2 * r * 0.015f).toFloat())
                        x += 4 * u
                    }
                    drawPath(p, Color.White.copy(alpha = 0.10f + 0.02f * (i % 3)), style = Stroke(2 * r * 0.008f))
                }
            }
            DialFace.LEAF -> {
                drawRect(Brush.linearGradient(listOf(k(0x2F9E44), k(0x1E6B34)), Offset(center.x - r, center.y - r), Offset(center.x + r, center.y + r)))
                val s = 2 * r
                val ox = center.x - r
                val oy = center.y - r
                val mid = Path().apply {
                    moveTo(ox + s * 0.15f, oy + s * 0.85f)
                    quadraticTo(ox + s * 0.4f, oy + s * 0.35f, ox + s * 0.85f, oy + s * 0.15f)
                }
                drawPath(mid, Color.White.copy(alpha = 0.16f), style = Stroke(s * 0.012f))
                for (i in 1 until 7) {
                    val t = i / 7.0
                    val bx = ox + s * (0.15f + 0.7f * t.toFloat())
                    val by = oy + s * (0.85f - 0.7f * t.toFloat()) - (s * 0.08f * sin(t * Math.PI)).toFloat()
                    for (side in listOf(-1f, 1f)) {
                        val v = Path().apply {
                            moveTo(bx, by)
                            quadraticTo(bx + side * s * 0.06f, by - s * 0.06f, bx + side * s * 0.16f, by - s * 0.02f - (if (side > 0) 0f else s * 0.1f))
                        }
                        drawPath(v, Color.White.copy(alpha = 0.10f), style = Stroke(s * 0.006f))
                    }
                }
            }
            DialFace.MARBLE -> {
                drawRect(k.face)
                var seed = 0x3A7B1EL
                fun rnd(): Double { seed = seed * 6364136223846793005L + 1442695040888963407L; return (seed ushr 11).toDouble() / (1L shl 53).toDouble() }
                val s = 2 * r
                val ox = center.x - r
                val oy = center.y - r
                for (i in 0 until 9) {
                    val p = Path()
                    var px = ox + (rnd() * s).toFloat()
                    var py = oy
                    p.moveTo(px, py)
                    repeat(6) {
                        val nx = px + ((rnd() - 0.45) * s * 0.25).toFloat()
                        val ny = py + s / 6
                        p.quadraticTo(px + ((rnd() - 0.5) * s * 0.3).toFloat(), py + s / 12, nx, ny)
                        px = nx; py = ny
                    }
                    drawPath(p, k(if (i % 3 == 0) 0xB8AE9C else 0xCFC8BD).copy(alpha = 0.7f), style = Stroke(s * if (i % 3 == 0) 0.008f else 0.004f))
                }
            }
            DialFace.SUNBURST -> {
                drawRect(Brush.radialGradient(listOf(k(0x4263EB), k(0x1C3584)), center, r))
                for (i in 0 until 120) rotate(i * 3f, center) {
                    drawLine(Color.White.copy(alpha = if (i % 2 == 0) 0.07f else 0.03f), Offset(center.x, center.y - 8 * u), Offset(center.x, center.y - r), 1.2f * u)
                }
            }
            DialFace.VINTAGE -> drawRect(Brush.radialGradient(listOf(k(0xF7EFDC), k(0xF3E9D2), k(0xDCC9A3)), center, r))
            DialFace.RAINBOW -> {
                drawRect(k.face)
                for (i in 0 until 12) drawArc(Color(0xFF000000 or DialFace.RAINBOW_C[i]), i * 30f - 105f, 30f, true,
                    Offset(center.x - r, center.y - r), Size(2 * r, 2 * r))
                drawCircle(Color.White, 34 * u, center)
            }
            DialFace.WOOD -> {
                drawRect(Brush.linearGradient(listOf(k(0x9C6634), k(0x5C3A1E)), Offset(center.x - r, center.y - r), Offset(center.x + r, center.y + r)))
                val s = 2 * r
                for (i in 0 until 16) {
                    val p = Path()
                    val y0 = center.y - r + s * (i + 0.5f) / 16
                    var x = center.x - r
                    p.moveTo(x, y0)
                    while (x <= center.x + r) {
                        val t = (x - center.x + r) / s
                        p.lineTo(x, y0 + (sin(t * (5 + i % 4) + i * 1.7) * s * 0.012 * (1 + i % 3)).toFloat())
                        x += 3 * u
                    }
                    drawPath(p, (if (i % 2 == 0) k(0x3E2410) else k(0xC08A52)).copy(alpha = if (i % 2 == 0) 0.28f else 0.16f), style = Stroke(s * 0.005f))
                }
                // Budak
                for (j in 0 until 4) drawOval(k(0x3E2410).copy(alpha = 0.25f), Offset(center.x - 52 * u - j * 4 * u, center.y + 30 * u - j * 2.5f * u),
                    Size((14 + j * 8) * u, (7 + j * 5) * u), style = Stroke(1.2f * u))
            }
            DialFace.GEOMETRIC -> drawRect(Brush.radialGradient(listOf(k(0x145A45), k(0x0B2E24)), center, r))
            DialFace.SECTOR -> { drawRect(k.face); drawCircle(k(0xE2D9C6), 50 * u, center) }
            else -> drawRect(k.face)
        }
        if (face == DialFace.RADAR) {
            for (rr in listOf(30f, 60f, 88f)) drawCircle(k.minor.copy(alpha = 0.6f), rr * u, center, style = Stroke(1 * u))
            drawLine(k.minor.copy(alpha = 0.5f), Offset(center.x, center.y - 88 * u), Offset(center.x, center.y + 88 * u), 1 * u)
            drawLine(k.minor.copy(alpha = 0.5f), Offset(center.x - 88 * u, center.y), Offset(center.x + 88 * u, center.y), 1 * u)
            // Tarayıcı: saniye ibresiyle döner
            rotate((sec * 6 - 90).toFloat(), center) {
                drawCircle(Brush.sweepGradient(listOf(k.tick.copy(alpha = 0f), k.tick.copy(alpha = 0f), k.tick.copy(alpha = 0.35f)), center), r, center)
            }
        }
    }
    if (face == DialFace.METAL) {
        drawCircle(Brush.sweepGradient(listOf(k(0xF8F9FA), k(0x868E96), k(0xDEE2E6), k(0x495057), k(0xF8F9FA)), center), r, center, style = Stroke(9 * u))
    } else if (face == DialFace.NEON) {
        // Işıma: geniş ve soluktan ince ve parlağa
        for ((w, a) in listOf(12f to 0.10f, 7f to 0.22f, 3.5f to 0.6f, 1.5f to 1f)) drawCircle(k.border.copy(alpha = a), r - 4 * u, center, style = Stroke(w * u))
        drawCircle(k.tick.copy(alpha = 0.35f), 58 * u, center, style = Stroke(1 * u))
    } else {
        drawCircle(k.border, r, center, style = Stroke(2 * u))
    }
    // Çizgiler
    when (face.ticks) {
        DialFace.Ticks.FULL -> for (i in 0 until 60) {
            val major = i % 5 == 0
            rotate(i * 6f, center) {
                drawLine(if (major) k.tick else k.minor, Offset(center.x, center.y - (if (major) 88 else 86) * u),
                    Offset(center.x, center.y - (if (major) 76 else 81) * u), (if (major) 3f else 1.4f) * u, StrokeCap.Round)
            }
        }
        DialFace.Ticks.HOURS -> for (i in 0 until 12) {
            val q = i % 3 == 0
            rotate(i * 30f, center) {
                drawLine(k.tick, Offset(center.x, center.y - 86 * u), Offset(center.x, center.y - (if (q) 68 else 73) * u), (if (q) 6f else 4f) * u, StrokeCap.Round)
            }
        }
        DialFace.Ticks.DOTS -> for (i in 0 until 12) {
            val a = i * 30 * Math.PI / 180
            drawCircle(k.tick, (if (i % 3 == 0) 5.5f else 3.5f) * u, Offset(center.x + 80 * u * sin(a).toFloat(), center.y - 80 * u * cos(a).toFloat()))
        }
        DialFace.Ticks.NONE -> {}
    }
    fun text(s: String, x: Float, y: Float, size: Float, color: Color, serif: Boolean = false, weight: FontWeight = FontWeight.SemiBold) {
        val l = measurer.measure(s, TextStyle(color = color, fontSize = (size * u / density / fontScale).sp, fontWeight = weight,
            fontFamily = if (serif) FontFamily.Serif else FontFamily.Default))
        drawText(l, topLeft = Offset(x - l.size.width / 2f, y - l.size.height / 2f))
    }
    if (face == DialFace.DOUBLE) {
        for (n in 1..12) {
            val a = n * 30 * Math.PI / 180
            text(String.format(java.util.Locale.ROOT, "%02d", n * 5), center.x + 70 * u * sin(a).toFloat(), center.y - 70 * u * cos(a).toFloat(), 8f, k.minor, weight = FontWeight.Medium)
        }
        drawCircle(k.minor.copy(alpha = 0.5f), 62 * u, center, style = Stroke(1 * u))
    }
    if (face == DialFace.CHRONO) {
        for ((x, y) in listOf(-40f to 0f, 40f to 0f, 0f to 40f)) {
            val sc = Offset(center.x + x * u, center.y + y * u)
            drawCircle(k.minor, 18 * u, sc, style = Stroke(1 * u))
            for (i in 0 until 12) rotate(i * 30f, sc) { drawLine(k.minor, Offset(sc.x, sc.y - 17 * u), Offset(sc.x, sc.y - 14 * u), 1 * u) }
            // Sol: 24 saat, sağ: dakika, alt: saniye
            val ang = if (x < 0) hour24 * 15 else if (x > 0) minute * 6 else sec * 6
            rotate(ang.toFloat(), sc) { drawLine(if (x == 0f) k.second else k.ink, sc, Offset(sc.x, sc.y - 14 * u), 1.6f * u, StrokeCap.Round) }
        }
    }
    fun at(rad: Float, deg: Double) = Offset(center.x + rad * u * sin(Math.toRadians(deg)).toFloat(), center.y - rad * u * cos(Math.toRadians(deg)).toFloat())
    when (face) {
        DialFace.PILOT -> {
            // 12'de üçgen ve iki nokta
            drawPath(Path().apply { moveTo(center.x - 7 * u, center.y - 87 * u); lineTo(center.x + 7 * u, center.y - 87 * u); lineTo(center.x, center.y - 72 * u); close() }, k.tick)
            for (dx in listOf(-11f, 11f)) drawCircle(k.tick, 1.8f * u, Offset(center.x + dx * u, center.y - 84 * u))
        }
        DialFace.DIVER -> {
            // Dönen çerçeve: ilk 15 dakika renkli, 10'ar dakikada rakam, sıfırda ışıklı üçgen
            drawCircle(k(0x1F2937), 87 * u, center, style = Stroke(14 * u))
            drawArc(k(0x2563EB), -90f, 90f, false, Offset(center.x - 87 * u, center.y - 87 * u), Size(174 * u, 174 * u), style = Stroke(14 * u))
            for (m in 0 until 60) if (m < 15 || m % 5 == 0) if (m % 10 != 0) rotate(m * 6f, center) {
                drawLine(Color.White.copy(alpha = 0.7f), Offset(center.x, center.y - 90 * u), Offset(center.x, center.y - (if (m % 5 == 0) 84 else 87) * u), 1.2f * u)
            }
            for (n in 1..5) at(87f, n * 60.0).let { text((n * 10).toString(), it.x, it.y, 8.5f, Color.White, weight = FontWeight.Bold) }
            drawPath(Path().apply { moveTo(center.x - 5 * u, center.y - 92 * u); lineTo(center.x + 5 * u, center.y - 92 * u); lineTo(center.x, center.y - 83 * u); close() }, k.tick)
            drawCircle(k.minor, 79.5f * u, center, style = Stroke(1 * u))
            // Işıklı saat işaretleri
            for (i in 0 until 12) {
                if (i % 3 == 0) rotate(i * 30f, center) {
                    drawLine(k.minor, Offset(center.x, center.y - 75 * u), Offset(center.x, center.y - 60 * u), 9 * u, StrokeCap.Round)
                    drawLine(k.tick, Offset(center.x, center.y - 75 * u), Offset(center.x, center.y - 60 * u), 6.5f * u, StrokeCap.Round)
                } else {
                    drawCircle(k.minor, 6 * u, at(68f, i * 30.0)); drawCircle(k.tick, 4.6f * u, at(68f, i * 30.0))
                }
            }
        }
        DialFace.IZNIK -> {
            // Kobalt mavisi bant, beyaz lale motifleri, aralarda mercan kırmızısı noktalar
            drawCircle(k.border, 86.5f * u, center, style = Stroke(15 * u))
            for (i in 0 until 12) {
                val d = i * 30.0
                rotate(d.toFloat(), center) {
                    val cx = center.x; val cy = center.y - 86.5f * u
                    drawOval(Color.White, Offset(cx - 2.2f * u, cy - 4.6f * u), Size(4.4f * u, 7.4f * u))
                    rotate(-28f, Offset(cx, cy + 2 * u)) { drawOval(Color.White, Offset(cx - 1.6f * u, cy - 3.6f * u), Size(3.2f * u, 6 * u)) }
                    rotate(28f, Offset(cx, cy + 2 * u)) { drawOval(Color.White, Offset(cx - 1.6f * u, cy - 3.6f * u), Size(3.2f * u, 6 * u)) }
                }
                drawCircle(k.second, 2.2f * u, at(86.5f, d + 15))
            }
            drawCircle(k(0x2A9D8F), 77 * u, center, style = Stroke(1.2f * u))
            for (i in 0 until 60) if (i % 5 != 0) drawCircle(k.minor, 0.9f * u, at(74f, i * 6.0))
            for (i in 0 until 8) rotate(i * 45f, center) { drawOval(k(0x2A9D8F).copy(alpha = 0.3f), Offset(center.x - 3 * u, center.y - 26 * u), Size(6 * u, 12 * u)) }
        }
        DialFace.GEOMETRIC -> {
            // Sekiz köşeli yıldız (iki kare) ve saatlerde küçük yıldızlar
            fun star(c: Offset, h: Float, color: Color, stroke: Float?) {
                for (rot in listOf(0f, 45f)) rotate(rot, c) {
                    if (stroke == null) drawRect(color, Offset(c.x - h, c.y - h), Size(2 * h, 2 * h))
                    else drawRect(color, Offset(c.x - h, c.y - h), Size(2 * h, 2 * h), style = Stroke(stroke))
                }
            }
            star(center, 40 * u, k.tick.copy(alpha = 0.55f), 1.4f * u)
            star(center, 27 * u, k.tick.copy(alpha = 0.3f), 1f * u)
            drawCircle(k.tick.copy(alpha = 0.45f), 56 * u, center, style = Stroke(1 * u))
            drawCircle(k.tick.copy(alpha = 0.45f), 88 * u, center, style = Stroke(1 * u))
            for (i in 0 until 12) star(at(76f, i * 30.0), (if (i % 3 == 0) 5.2f else 3.6f) * u, k.tick, null)
            for (i in 0 until 60) if (i % 5 != 0) drawCircle(k.minor, 1 * u, at(76f, i * 6.0))
        }
        DialFace.SECTOR -> {
            drawCircle(k.minor, 50 * u, center, style = Stroke(1 * u))
            drawCircle(k.minor, 74 * u, center, style = Stroke(1 * u))
            drawCircle(k.minor, 89.5f * u, center, style = Stroke(1 * u))
        }
        DialFace.VINTAGE -> {
            // Demiryolu dakika halkası
            drawCircle(k.minor, 80 * u, center, style = Stroke(0.8f * u))
            drawCircle(k.minor, 88 * u, center, style = Stroke(0.8f * u))
        }
        else -> {}
    }
    if (face.numerals != DialFace.Numerals.NONE) {
        val radius = when (face) {
            DialFace.DOUBLE -> 46f; DialFace.CHRONO -> 70f; DialFace.RAINBOW, DialFace.SECTOR -> 62f
            DialFace.PILOT, DialFace.BAUHAUS -> 64f; DialFace.VINTAGE, DialFace.IZNIK -> 63f; DialFace.WOOD -> 55f; else -> 66f
        }
        val size = when (face) {
            DialFace.CHRONO, DialFace.SECTOR -> 12f; DialFace.DOUBLE, DialFace.WOOD -> 13f; DialFace.ROMAN, DialFace.CHINESE -> 14f
            DialFace.EASTERN -> 16f; DialFace.PILOT, DialFace.BAUHAUS, DialFace.RAINBOW -> 18f; else -> 15f
        }
        val weight = when (face) { DialFace.PILOT, DialFace.RAINBOW -> FontWeight.Bold; DialFace.BAUHAUS -> FontWeight.Light; DialFace.VINTAGE -> FontWeight.Normal; else -> FontWeight.SemiBold }
        for (i in 0 until 12) {
            val n = if (i == 0) 12 else i
            if (face == DialFace.CHRONO && n in listOf(3, 6, 9)) continue
            if (face == DialFace.PILOT && n == 12) continue
            if (face.numerals == DialFace.Numerals.QUARTERS && n % 3 != 0) continue
            val a = n * 30 * Math.PI / 180
            val s = when (face.numerals) {
                DialFace.Numerals.ROMAN -> DialFace.ROMAN_N[i]; DialFace.Numerals.CHINESE -> DialFace.CHINESE_N[i]
                DialFace.Numerals.EASTERN -> DialFace.EASTERN_N[i]; else -> n.toString()
            }
            text(s, center.x + radius * u * sin(a).toFloat(), center.y - radius * u * cos(a).toFloat(), size, k.ink,
                serif = face.numerals == DialFace.Numerals.ROMAN || face == DialFace.VINTAGE, weight = weight)
        }
    }
}

/** Kadran galerisi (Saat görünümü › Kadranlar): küçük canlı kadranlar; dokununca önizleme, renk ve "Kullan" */
@Composable
fun DialGalleryScreen() {
    var face by remember { mutableStateOf(DialFace.current) }
    var color by remember { mutableStateOf(DialColor.current) }
    val newOnOpen = remember { !Prefs.store.getBoolean(DialFace.SEEN_KEY, false) }
    var preview by remember { mutableStateOf<DialFace?>(null) }
    remember { Prefs.store.edit().putBoolean(DialFace.SEEN_KEY, true).apply(); 0 }
    val now = rememberNow().atZone(java.time.ZoneId.systemDefault())
    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(DialFace.entries) { f ->
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(Theme.c.surface).clickable { preview = f }.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        AnalogClock(now, Modifier.fillMaxWidth(), face = f, dialColor = if (f == face) color else DialColor.AUTO, tap = false)
                        if (newOnOpen && f.ordinal >= DialFace.EASTERN.ordinal) {
                            Text(L("dial.new"), Modifier.align(Alignment.TopEnd).background(Theme.c.accent, RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Theme.c.bg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (f == face) Icon(Icons.Filled.CheckCircle, null, tint = Theme.c.accent, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                        Text(f.title, color = Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(L("dial.note"), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
    }
    preview?.let { f ->
        var pick by remember(f) { mutableStateOf(if (f == face) color else DialColor.AUTO) }
        Dialog(onDismissRequest = { preview = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(Theme.c.bg).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(f.title, Modifier.weight(1f), color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { preview = null }) { Icon(Icons.Filled.Close, L("common.close"), tint = Theme.c.text) }
                }
                var lume by remember { mutableStateOf(Lume.id) }
                key(lume) { AnalogClock(now, Modifier.widthIn(max = 340.dp).fillMaxWidth(), face = f, dialColor = pick, tap = false) }
                // Lüm kadranında ışıma rengi (14 renk)
                if (f == DialFace.LUME) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(L("dial.lumeColor"), color = Theme.c.muted, fontSize = 13.sp)
                    for (row in Lume.COLORS.keys.chunked(7)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (id in row) Box(
                            Modifier.size(34.dp).border(if (id == lume) 3.dp else 0.dp, Theme.c.text, CircleShape).padding(4.dp)
                                .background(androidx.compose.ui.graphics.Color.Black, CircleShape).padding(3.dp)
                                .background(Lume.color(id), CircleShape)
                                .clickable(onClickLabel = L("color.$id")) { lume = id; Prefs.store.edit().putString(Lume.KEY, id).apply() },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DialColor.entries.forEach { c ->
                        Box(Modifier.size(38.dp).border(if (c == pick) 3.dp else 0.dp, Theme.c.text, CircleShape).padding(4.dp)
                            .background(c.color ?: f.colors(Theme.c).hand, CircleShape).clickable { pick = c })
                    }
                }
                val inUse = face == f && color == pick
                RoundButton(L(if (inUse) "dial.inUse" else "dial.use"), true, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Prefs.store.edit().putString(DialFace.KEY, f.id).putString(DialFace.COLOR_KEY, pick.id).apply()
                    face = f; color = pick; preview = null
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Saat ekranında bir kez "Yeni kadranlar" tanıtımı (galeri açılınca ya da kapatılınca kalkar) */
@Composable
fun DialNewsCard(onOpen: () -> Unit) {
    var seen by remember { mutableStateOf(Prefs.store.getBoolean(DialFace.SEEN_KEY, false)) }
    if (seen) return
    val now = rememberNow().atZone(java.time.ZoneId.systemDefault())
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.c.surface).clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnalogClock(now, Modifier.size(54.dp), face = DialFace.CHRONO, tap = false)
        Column(Modifier.weight(1f)) {
            Text(L("dial.newTitle"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(L("dial.newText"), color = Theme.c.muted, fontSize = 12.sp, maxLines = 3)
        }
        IconButton(onClick = { seen = true; Prefs.store.edit().putBoolean(DialFace.SEEN_KEY, true).apply() }) {
            Icon(Icons.Filled.Close, L("common.close"), tint = Theme.c.muted)
        }
    }
}
