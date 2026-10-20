package tr.bumba.time

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import tr.bumba.time.core.Prayer
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * İmsakiye: sıradaki Ramazan boyunca günlük vakitler, sitedeki gibi renkli görsel olarak çizilir (src/js/imsakiye.js:
 * yıldız dokulu zemin, fener, şamdan, hilal, Bumba Group imzası; dış görsel yok). Sayfada gösterilir, PNG olarak
 * paylaşılır (FileProvider). Ramazan tarihleri calendar.json'dan (holidays.json).
 */
object Imsakiye {
    /** Bugün ya da sonra biten ilk Ramazan */
    fun nextRamadan(today: LocalDate): ClosedRange<LocalDate>? =
        AppData.ramadans.firstOrNull { !it.endInclusive.isBefore(today) }

    private const val GOLD = 0xFFE0BB62.toInt()
    private const val GOLD2 = 0xFFA8802F.toInt()
    private const val CREAM = 0xFFFFF4D6.toInt()
    private const val LIGHT = 0xFFFFF0B8.toInt()
    private const val BG1 = 0xFF0B4A43.toInt()
    private const val BG2 = 0xFF061F2B.toInt()

    private fun argb(a: Double, rgb: Int) = ((a * 255).toInt() shl 24) or (rgb and 0xFFFFFF)

    private class Row(val n: Int, val date: LocalDate, val times: List<String>)

    private fun rows(place: AppPlace, ramadan: ClosedRange<LocalDate>): List<Row> {
        val out = ArrayList<Row>()
        var d = ramadan.start
        var n = 1
        while (!d.isAfter(ramadan.endInclusive)) {
            val e = place.entries(d).associate { it.prayer to Fmt.hm(it.at, place.zone) }
            out += Row(n++, d, Prayer.entries.map { e[it] ?: "–" })
            d = d.plusDays(1)
        }
        return out
    }

    private fun dateFull(d: LocalDate) = "${d.dayOfMonth} ${Fmt.month(d.monthValue)} ${d.year}"

    /** 1080 piksel genişliğinde görsel (sitedeki ölçüler) */
    fun draw(place: AppPlace, ramadan: ClosedRange<LocalDate>): Bitmap {
        val data = rows(place, ramadan)
        val w = 1080f
        val head = 400f
        val th = 84f
        val rh = 46f
        val foot = 330f
        val h = head + th + data.size * rh + foot
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        p.shader = LinearGradient(0f, 0f, 0f, h, BG1, BG2, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        pattern(c, w, h)
        glow(c, w / 2, 130f, 330f, argb(0.22, 0xFFC450))
        // çerçeve
        stroke(p, GOLD, 4f)
        c.drawRoundRect(RectF(20f, 20f, w - 20, h - 20), 26f, 26f, p)
        stroke(p, argb(0.45, GOLD), 1.5f)
        c.drawRoundRect(RectF(34f, 34f, w - 34, h - 34), 18f, 18f, p)
        // süsler
        lantern(c, 150f, 44f, 0.78f)
        lantern(c, w - 150, 44f, 0.78f)
        crescent(c, w / 2, 128f, 64f)
        fill(p, GOLD)
        c.drawPath(star8(w / 2 + 62, 118f, 15f), p)
        c.drawPath(star8(w / 2 - 96, 80f, 9f), p)
        c.drawPath(star8(w / 2 + 100, 66f, 7f), p)
        // başlık
        val t = text(Typeface.BOLD)
        val title = T("prayer.imTitle", ramadan.start.year).uppercase(Strings.locale)
        fit(t, title, 56f, 760f)
        t.color = CREAM
        c.drawText(title, w / 2, 266f, t)
        fit(t, place.name, 40f, 700f)
        t.color = GOLD
        c.drawText(place.name, w / 2, 318f, t)
        val range = dateFull(ramadan.start) + " – " + dateFull(ramadan.endInclusive)
        val tn = text(Typeface.NORMAL)
        fit(tn, range, 26f, 860f)
        tn.color = argb(0.85, CREAM)
        c.drawText(range, w / 2, 362f, tn)
        // tablo başlığı
        val x0 = 50f
        val cols = floatArrayOf(104f, 180f, 116f, 116f, 116f, 116f, 116f, 116f)
        val xs = FloatArray(cols.size)
        var x = x0
        for (i in cols.indices) { xs[i] = x; x += cols[i] }
        val ty = head + 6
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, ty, 0f, ty + th - 10, argb(0.34, GOLD), argb(0.14, GOLD), Shader.TileMode.CLAMP)
        val hr = RectF(x0, ty, x0 + 980, ty + th - 10)
        c.drawRoundRect(hr, 14f, 14f, p)
        p.shader = null
        stroke(p, argb(0.6, GOLD), 1.5f)
        c.drawRoundRect(hr, 14f, 14f, p)
        fun cell(s: String, i: Int, y: Float, size: Float, style: Int, color: Int) {
            val tp = text(style)
            tp.textSize = size
            tp.color = color
            // Sütuna sığmayan ad küçültülür
            while (tp.measureText(s) > cols[i] - 6 && tp.textSize > 12) tp.textSize -= 1
            c.drawText(s, xs[i] + cols[i] / 2, y, tp)
        }
        cell(L("prayer.imDay"), 0, ty + 44, 19f, Typeface.BOLD, CREAM)
        cell(L("prayer.imDate"), 1, ty + 44, 22f, Typeface.BOLD, CREAM)
        Prayer.entries.forEachIndexed { i, pr ->
            val sp = when (pr) {
                Prayer.FAJR -> L("prayer.imSahur")
                Prayer.MAGHRIB -> L("prayer.imIftar")
                else -> ""
            }
            cell(pr.title, i + 2, if (sp.isNotEmpty()) ty + 36 else ty + 44, 22f, Typeface.BOLD, if (sp.isNotEmpty()) GOLD else CREAM)
            if (sp.isNotEmpty()) cell("($sp)", i + 2, ty + 58, 17f, Typeface.BOLD, GOLD)
        }
        // satırlar (cuma vurgulu)
        data.forEachIndexed { j, r ->
            val y = head + th + j * rh
            val rr = RectF(x0, y + 2, x0 + 980, y + rh - 2)
            p.style = Paint.Style.FILL
            if (r.date.dayOfWeek.value == 5) { p.color = argb(0.16, GOLD); c.drawRoundRect(rr, 10f, 10f, p) }
            else if (j % 2 == 0) { p.color = argb(0.06, 0xFFFFFF); c.drawRoundRect(rr, 10f, 10f, p) }
            val base = y + rh / 2 + 8
            cell(r.n.toString(), 0, base, 22f, Typeface.BOLD, GOLD)
            cell("${r.date.dayOfMonth} ${Fmt.month(r.date.monthValue)} · ${Fmt.weekdayShort(r.date)}", 1, base, 20f, Typeface.NORMAL, CREAM)
            r.times.forEachIndexed { i, tx ->
                val hl = i == 0 || i == 4
                cell(tx, i + 2, base, if (hl) 25f else 23f, if (hl) Typeface.BOLD else Typeface.NORMAL, if (hl) 0xFFFFD978.toInt() else CREAM)
            }
        }
        // alt imza
        val fy = head + th + data.size * rh + 30
        stroke(p, argb(0.5, GOLD), 1.5f)
        c.drawLine(120f, fy, w - 120, fy, p)
        fill(p, GOLD)
        c.drawPath(star8(w / 2, fy, 11f), p)
        candelabra(c, 190f, fy + 215, 0.95f)
        candelabra(c, w - 190, fy + 215, 0.95f)
        t.textSize = 62f
        t.color = GOLD
        c.drawText("Bumba Group", w / 2, fy + 100, t)
        t.textSize = 26f
        t.color = CREAM
        c.drawText("Bumba Life · Saat Tarih · time.bumba.tr", w / 2, fy + 142, t)
        tn.textSize = 19f
        tn.color = argb(0.75, CREAM)
        wrap(tn, L("prayer.imNote"), 560f).take(3).forEachIndexed { i, ln -> c.drawText(ln, w / 2, fy + 186 + i * 26, tn) }
        return bmp
    }

    private fun text(style: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, style)
        textAlign = Paint.Align.CENTER
    }

    private fun fit(p: Paint, s: String, max: Float, width: Float) {
        p.textSize = max
        while (p.measureText(s) > width && p.textSize > 14) p.textSize -= 2
    }

    private fun wrap(p: Paint, s: String, width: Float): List<String> {
        val lines = ArrayList<String>()
        var cur = ""
        for (word in s.split(" ")) {
            val t = if (cur.isEmpty()) word else "$cur $word"
            if (p.measureText(t) > width && cur.isNotEmpty()) { lines += cur; cur = word } else cur = t
        }
        if (cur.isNotEmpty()) lines += cur
        return lines
    }

    private fun stroke(p: Paint, color: Int, width: Float) {
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = width
        p.color = color
    }

    private fun fill(p: Paint, color: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
    }

    private fun star8(cx: Float, cy: Float, r: Float) = Path().apply {
        for (i in 0 until 16) {
            val a = i * PI / 8 - PI / 2
            val rad = if (i % 2 == 1) r * 0.62f else r
            val px = cx + rad * cos(a).toFloat()
            val py = cy + rad * sin(a).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    private fun pattern(c: Canvas, w: Float, h: Float) {
        val s = 120f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        stroke(p, argb(0.13, GOLD), 2f)
        var j = -1
        while (j * s < h + s) {
            var i = -1
            while (i * s < w + s) {
                c.drawPath(star8(i * s + s / 2, j * s + s / 2, s * 0.48f), p)
                c.drawCircle(i * s + s / 2, j * s + s / 2, s * 0.17f, p)
                c.drawCircle(i * s, j * s, s * 0.11f, p)
                i++
            }
            j++
        }
    }

    private fun glow(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(x, y, r, color, argb(0.0, 0xFFC85A), Shader.TileMode.CLAMP)
        c.drawRect(x - r, y - r, x + r, y + r, p)
    }

    /** Hilal: daire eksi kaydırılmış daire, altın geçişli */
    private fun crescent(c: Canvas, x: Float, y: Float, r: Float) {
        val outer = Path().apply { addCircle(x, y, r, Path.Direction.CW) }
        val inner = Path().apply { addCircle(x + r * 0.36f, y - r * 0.08f, r * 0.82f, Path.Direction.CW) }
        outer.op(inner, Path.Op.DIFFERENCE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(x - r, y - r, x + r, y + r, LIGHT, GOLD2, Shader.TileMode.CLAMP)
        c.drawPath(outer, p)
    }

    private fun goldGradient(half: Float) = LinearGradient(-half, 0f, half, 0f, intArrayOf(GOLD2, LIGHT, GOLD2), null, Shader.TileMode.CLAMP)

    private fun lantern(c: Canvas, x: Float, top: Float, s: Float) {
        c.save()
        c.translate(x, top)
        c.scale(s, s)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        stroke(p, GOLD, 3f)
        c.drawLine(0f, -top / s, 0f, 12f, p)
        glow(c, 0f, 120f, 150f, argb(0.38, 0xFFC450))
        val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = goldGradient(40f) }
        c.drawCircle(0f, 8f, 9f, gold)
        c.drawPath(Path().apply { moveTo(-34f, 44f); quadTo(0f, -14f, 34f, 44f); close() }, gold)
        c.drawRect(-38f, 42f, 38f, 51f, gold)
        val body = Path().apply {
            moveTo(-34f, 51f); lineTo(34f, 51f); cubicTo(58f, 90f, 58f, 140f, 30f, 190f); lineTo(-30f, 190f)
            cubicTo(-58f, 140f, -58f, 90f, -34f, 51f); close()
        }
        val bp = Paint(Paint.ANTI_ALIAS_FLAG)
        bp.shader = RadialGradient(0f, 120f, 90f, intArrayOf(0xFFFFE9A0.toInt(), 0xFFF2A93B.toInt(), 0xFFB2541C.toInt()),
            floatArrayOf(0.07f, 0.6f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(body, bp)
        stroke(p, GOLD2, 3f)
        c.drawPath(body, p)
        stroke(p, argb(0.55, 0x783C0A), 2f)
        for (dx in listOf(-18f, 0f, 18f)) {
            c.drawPath(Path().apply { moveTo(dx * 0.6f, 51f); quadTo(dx * 3, 120f, dx, 190f) }, p)
        }
        c.drawRect(-34f, 188f, 34f, 197f, gold)
        c.drawPath(Path().apply { moveTo(-26f, 197f); lineTo(26f, 197f); lineTo(8f, 216f); lineTo(-8f, 216f); close() }, gold)
        stroke(p, GOLD, 3f)
        c.drawLine(0f, 216f, 0f, 238f, p)
        fill(p, GOLD)
        c.drawCircle(0f, 242f, 5f, p)
        c.restore()
    }

    private fun flame(c: Canvas, x: Float, y: Float, s: Float) {
        glow(c, x, y - 10 * s, 34 * s, argb(0.5, 0xFFC85A))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(x, y - 24 * s, x, y, 0xFFFFF7C4.toInt(), 0xFFF59A1D.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(Path().apply {
            moveTo(x, y - 26 * s); cubicTo(x + 11 * s, y - 10 * s, x + 8 * s, y, x, y)
            cubicTo(x - 8 * s, y, x - 11 * s, y - 10 * s, x, y - 26 * s)
        }, p)
    }

    private fun candelabra(c: Canvas, x: Float, base: Float, s: Float) {
        c.save()
        c.translate(x, base)
        c.scale(s, s)
        val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = goldGradient(60f) }
        c.drawPath(Path().apply { moveTo(-44f, 0f); lineTo(44f, 0f); lineTo(26f, -16f); lineTo(-26f, -16f); close() }, gold)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = goldGradient(60f); style = Paint.Style.STROKE; strokeWidth = 7f; strokeCap = Paint.Cap.ROUND
        }
        c.drawLine(0f, -16f, 0f, -130f, line)
        c.drawPath(Path().apply { moveTo(0f, -64f); cubicTo(-14f, -34f, -64f, -44f, -64f, -108f) }, line)
        c.drawPath(Path().apply { moveTo(0f, -64f); cubicTo(14f, -34f, 64f, -44f, 64f, -108f) }, line)
        val cream = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CREAM }
        for ((px, py) in listOf(-64f to -108f, 0f to -130f, 64f to -108f)) {
            c.drawPath(Path().apply { moveTo(px - 12, py); lineTo(px + 12, py); lineTo(px + 7, py + 10); lineTo(px - 7, py + 10); close() }, gold)
            c.drawRect(px - 5, py - 38, px + 5, py, cream)
            flame(c, px, py - 38, 1f)
        }
        c.restore()
    }

    /** Paylaşım dosya adı: "ramazan-imsakiyesi-istanbul-2027.png" */
    fun fileName(place: AppPlace, ramadan: ClosedRange<LocalDate>): String {
        val slug = Normalizer.normalize(
            place.name.lowercase(Strings.locale).replace("ı", "i"), Normalizer.Form.NFD,
        ).replace(Regex("\\p{M}"), "").replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return L("prayer.imFile") + (if (slug.isNotEmpty()) "-$slug" else "") + "-${ramadan.start.year}.png"
    }

    /** PNG'yi önbelleğe yazar ve paylaşım penceresini açar */
    fun share(context: Context, bitmap: Bitmap, name: String, title: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val f = File(dir, name)
        FileOutputStream(f).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TITLE, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** İmsakiye sayfası: görsel ve "Görsel olarak paylaş" */
@Composable
fun ImsakiyeScreen(place: AppPlace, ramadan: ClosedRange<LocalDate>, onBack: () -> Unit) {
    val context = LocalContext.current
    val title = T("imsakiye.title", ramadan.start.year)
    val bitmap = remember(place, ramadan, Strings.lang) { Imsakiye.draw(place, ramadan) }
    Column(Modifier.fillMaxSize()) {
        SubBar(title, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(onClick = { Imsakiye.share(context, bitmap, Imsakiye.fileName(place, ramadan), title) }) {
                Icon(Icons.Filled.Share, null, tint = Theme.c.accent)
                Text("  " + L("imsakiye.share"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
            }
            Image(
                bitmap.asImageBitmap(),
                contentDescription = T("prayer.imAlt", place.name, ramadan.start.year),
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height),
            )
        }
    }
}
