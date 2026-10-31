package tr.bumba.time

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.ZoneId

/*
 * Paylaşım (30 Ekim 2026, rakip analizi #16, #28, #29; iOS'taki ShareCards.swift ve CSVFile): bugünün vakitleri ve
 * kişisel geri sayım görsel kart olarak, kronometre turları ve pomodoro geçmişi CSV olarak sistemin paylaşım penceresiyle
 * gönderilir. Dosyalar önbellekteki share klasöründe üretilir (FileProvider), bir yere yüklenmez.
 */
object Share {
    private fun send(context: Context, f: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TITLE, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun dir(context: Context) = File(context.cacheDir, "share").apply { mkdirs(); listFiles()?.forEach { it.delete() } }

    /** CSV (Excel Türkçe karakterleri doğru açsın diye UTF-8 BOM ile) */
    fun csv(context: Context, name: String, rows: List<List<String>>, title: String) {
        fun esc(v: String) = if (v.any { it in ",\"\n\r;" }) "\"" + v.replace("\"", "\"\"") + "\"" else v
        val f = File(dir(context), name)
        f.writeText("﻿" + rows.joinToString("\r\n") { r -> r.joinToString(",") { esc(it) } } + "\r\n")
        send(context, f, "text/csv", title)
    }

    fun image(context: Context, bitmap: Bitmap, name: String, title: String) {
        val f = File(dir(context), name)
        f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        send(context, f, "image/png", title)
    }
}

/** Görsel kartlar: koyu degrade zemin, açık yazı, altta Saat Tarih imzası (iOS'taki ShareCardFrame ile aynı görünüm) */
object ShareCards {
    private const val W = 1080
    private const val PAD = 72f
    private val rtl get() = Strings.isRtl
    private const val GOLD = 0xFFFFD173.toInt()

    private fun paint(size: Float, bold: Boolean = false, color: Int = 0xFFFFFFFF.toInt(), alpha: Int = 255) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            this.alpha = alpha
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    /** Sarılan metin bloğu; y'yi ilerletir */
    private class Pen(val c: Canvas) {
        var y = PAD
        fun block(text: String, p: TextPaint, gap: Float = 12f) {
            val l = StaticLayout.Builder.obtain(text, 0, text.length, p, (W - 2 * PAD).toInt())
                .setAlignment(if (Strings.isRtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL).build()
            c.save(); c.translate(PAD, y); l.draw(c); c.restore()
            y += l.height + gap
        }
        fun row(left: String, right: String, p: TextPaint, gap: Float = 18f) {
            val h = p.fontSpacing
            val a = if (Strings.isRtl) right else left
            val b = if (Strings.isRtl) left else right
            p.textAlign = Paint.Align.LEFT
            c.drawText(a, PAD, y + h * 0.8f, p)
            p.textAlign = Paint.Align.RIGHT
            c.drawText(b, W - PAD, y + h * 0.8f, p)
            p.textAlign = Paint.Align.LEFT
            y += h + gap
        }
    }

    private fun render(height: Int, draw: (Pen) -> Unit): Bitmap {
        val b = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, W.toFloat(), height.toFloat(), 0xFF1A2140.toInt(), 0xFF40214F.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(RectF(0f, 0f, W.toFloat(), height.toFloat()), 64f, 64f, bg)
        val pen = Pen(c)
        draw(pen)
        val sign = paint(32f, bold = true, alpha = 200).apply { textAlign = if (rtl) Paint.Align.LEFT else Paint.Align.RIGHT }
        c.drawText("Saat Tarih · time.bumba.tr", if (rtl) PAD else W - PAD, height - PAD * 0.7f, sign)
        return b
    }

    /** Bugünün vakitleri: yer, miladi ve hicri tarih, varsa o günün dini/özel günleri, altı vakit */
    fun prayer(place: AppPlace, now: Instant): Bitmap {
        val day = place.today(now)
        val events = AppData.events[day].orEmpty().map { it.name }.distinct()
        val entries = place.entries(day)
        val height = 600 + (if (events.isEmpty()) 0 else 110) + entries.size * 98
        return render(height) { p ->
            p.block(place.name, paint(64f, bold = true), 8f)
            p.block(Fmt.longDate(day), paint(40f, alpha = 220), 4f)
            Hijri.date(day)?.let { p.block(it.text, paint(40f, alpha = 220), 24f) }
            if (events.isNotEmpty()) p.block("✦ " + events.joinToString(" · "), paint(40f, bold = true, color = GOLD), 28f)
            p.y += 12f
            for (e in entries) p.row(e.prayer.title, Fmt.hm(e.at, place.zone), paint(48f, bold = false), 40f)
        }
    }

    /** Kişisel geri sayım: simge, başlık, kalan, hedef tarih */
    fun countdown(item: CountdownItem, now: Instant): Bitmap {
        val at = item.effective(now)
        return render(900) { p ->
            p.block(item.emoji, paint(120f), 16f)
            p.block(item.title.ifBlank { L("tools.hub.cards.countdown.name") }, paint(64f, bold = true), 20f)
            p.block(countdownText(at, now), paint(96f, bold = true, color = GOLD), 20f)
            p.block(Fmt.longDate(at.atZone(ZoneId.systemDefault()).toLocalDate()), paint(40f, alpha = 220))
        }
    }
}
