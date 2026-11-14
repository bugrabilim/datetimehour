package tr.bumba.time

import androidx.compose.foundation.layout.width
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

/*
 * Geri sayım ekleri (2. analiz #73, #102, #138, #139; 3 Kasım 2026; iOS CountdownExtras.swift ile aynı): gösterim birimi,
 * tarih geçince ileri sayım, "38. yıl", kart rengi, arşiv, etiket, yalnız iş günü; tam ekran ayrıntı (gün/saat/dakika/
 * saniye sütunları, sıfırda konfeti), sitedeki ?t=&n= bağlantısıyla paylaşma ve bağlantıdan ekleme, hedef anına alarm.
 */

val CountdownItem.Companion.UNITS: List<String> get() = listOf("auto", "days", "weeks", "hours", "minutes", "parts")

/** Kart renkleri (0 = renksiz) */
val CountdownItem.Companion.PALETTE: List<Color>
    get() = listOf(Color.Transparent, Color(0xFFDB4040), Color(0xFFED8521), Color(0xFFD9AD14), Color(0xFF2E9E5C),
        Color(0xFF2978D9), Color(0xFF8554D1), Color(0xFFDB4D94))

val CountdownItem.tint: Color? get() = if (color in 1 until CountdownItem.PALETTE.size) CountdownItem.PALETTE[color] else null

/** Her yıl tekrarlanan geri sayımda kaçıncı yıl (" · 38. yıl"); ilk yıl ya da tekrarsızda boş */
fun CountdownItem.ordinalText(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    if (!yearly) return ""
    val n = at.atZone(zone).year - Instant.ofEpochMilli(target).atZone(zone).year
    return if (n > 0) " · " + T("countdowns.ordinal", n) else ""
}

/** Sitedeki geri sayım aracının paylaşım bağlantısı (?t=YYYY-MM-DDTHH:MM&n=Başlık) */
fun CountdownItem.shareUrl(now: Instant = Instant.now()): String {
    val t = effective(now).atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
    val path = if (Strings.lang == "tr") "/geri-sayim/" else "/${Strings.lang}/countdown/"
    return "https://time.bumba.tr$path?t=$t&n=" + java.net.URLEncoder.encode(title, "UTF-8").replace("+", "%20")
}

/** Kalan süre: seçilen birimle; tarih geçtiyse (yıllık değilse) ileri sayım ("12 gün geçti") */
fun countdownDisplay(c: CountdownItem, at: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val left = (at.toEpochMilli() - now.toEpochMilli()) / 1000
    val days = ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), at.atZone(zone).toLocalDate())
    if (left <= 0) return if (c.yearly || days == 0L) countdownText(at, now, zone) else T("countdowns.since", -days)
    if (c.workdays) {
        val w = DateCalc.workdays(now.atZone(zone).toLocalDate(), at.atZone(zone).toLocalDate())
        return T("countdowns.workLeft", if (w == Math.floor(w)) w.toLong().toString() else "%.1f".format(java.util.Locale.ROOT, w).replace(".", Strings.decimalPoint))
    }
    return when (c.unit) {
        "days" -> T("client.daysLeft", days)
        "weeks" -> T("countdowns.weeksLeft", days / 7, days % 7)
        "hours" -> T("countdowns.hoursLeft", left / 3600)
        "minutes" -> T("countdowns.minutesLeft", left / 60)
        "parts" -> T("countdowns.parts", left / 86400) + " " + Fmt.countdown(left % 86400)
        else -> countdownText(at, now, zone)
    }
}

/** Paylaşılan bağlantıdan (https://time.bumba.tr/geri-sayim/?t=…&n=… ya da saattarih://countdown?…) ekleme */
object CountdownImport {
    /** Liste ekranı açıkken yenilensin diye */
    var version by mutableIntStateOf(0)

    fun handle(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        val isSite = uri.host == "time.bumba.tr" && (uri.path.orEmpty().contains("/geri-sayim") || uri.path.orEmpty().contains("/countdown"))
        val isApp = uri.scheme == "saattarih" && uri.host == "countdown"
        if (!isSite && !isApp) return false
        val t = uri.getQueryParameter("t") ?: return false
        val at = runCatching { if (t.length > 10) LocalDateTime.parse(t) else LocalDate.parse(t).atStartOfDay() }.getOrNull() ?: return false
        val title = (uri.getQueryParameter("n") ?: "").take(40)
        val ms = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val list = CountdownItem.load()
        if (list.none { it.target == ms && it.title == title }) CountdownItem.save(list + CountdownItem(title = title, emoji = "🎉", target = ms))
        version++
        return true
    }
}

/** Tam ekran ayrıntı (2. analiz #102): fotoğraf ya da renk zemini, gün/saat/dakika/saniye sütunları, sıfırda konfeti;
 *  bağlantıyla paylaş (#138), hedef anına alarm (#139), arşivle, düzenle */
@Composable
fun CountdownDetail(item: CountdownItem, onDismiss: () -> Unit, onEdit: () -> Unit, onArchive: () -> Unit) {
    val context = LocalContext.current
    var msg by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val now = rememberNow()
        val at = item.effective(now)
        val left = maxOf(0L, (at.toEpochMilli() - now.toEpochMilli()) / 1000)
        val finished = !at.isAfter(now) && now.toEpochMilli() - at.toEpochMilli() < 86_400_000L
        val photo = PhotoFile.image(context, item.photoName, item.photo)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(item.tint ?: Theme.c.accent, Color(0xD9000000))))) {
            if (photo != null) {
                PhotoBackground(photo, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0x80000000)))
            }
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                Text(item.emoji, fontSize = 64.sp)
                Text(item.title.ifBlank { L("tools.hub.cards.countdown.name") }, color = Color.White, fontSize = 28.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                val local = at.atZone(ZoneId.systemDefault())
                Text(Fmt.longDate(local.toLocalDate()) + " " + Fmt.time(local) + item.ordinalText(at), color = Color(0xDDFFFFFF), fontSize = 14.sp)
                if (at.isAfter(now)) Ltr {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for ((v, k) in listOf(left / 86400 to "days", left % 86400 / 3600 to "hours", left % 3600 / 60 to "minutes", left % 60 to "seconds")) {
                            Column(Modifier.background(Color(0x33FFFFFF), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp).widthIn(min = 56.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("%02d".format(java.util.Locale.ROOT, v), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, style = Tnum)
                                Text(L("countdowns.col.$k"), color = Color(0xCCFFFFFF), fontSize = 12.sp)
                            }
                        }
                    }
                } else Text(countdownDisplay(item, at, now), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, item.label + "\n" + item.shareUrl()), item.label).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { DuoIcon("share", 18.dp, Color.White); Spacer(Modifier.width(6.dp)); Text(L("countdowns.shareLink"), color = Color.White) }
                    if (at.isAfter(now)) TextButton(onClick = {
                        val l = at.atZone(ZoneId.systemDefault())
                        AlarmItem.addNew(context, AlarmItem(hour = l.hour, minute = l.minute, label = item.label.take(40), date = l.toLocalDate().toString()))
                        msg = T("alarm.quickSet", Fmt.longDate(l.toLocalDate()) + " " + Fmt.time(l))
                    }) { DuoIcon("alarm", 18.dp, Color.White); Spacer(Modifier.width(6.dp)); Text(L("countdowns.alarm"), color = Color.White) }
                }
                if (msg.isNotEmpty()) Text(msg, color = Color(0xDDFFFFFF), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text(L("countdowns.edit"), color = Color.White) }
                    TextButton(onClick = onArchive) { Text(L(if (item.archived) "countdowns.unarchive" else "countdowns.archive"), color = Color.White) }
                    TextButton(onClick = onDismiss) { Text(L("common.done"), color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
            if (finished) Confetti(at.toEpochMilli(), now.toEpochMilli())
        }
    }
}

/** Konfeti: renkli parçalar döne döne düşer (her saniye yeniden çizilir; animasyonlar kapalıysa çizilmez) */
@Composable
private fun Confetti(startMs: Long, nowMs: Long) {
    val context = LocalContext.current
    val off = remember { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    if (off) return
    val frame by produceFrameMs()
    val colors = listOf(Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFD81B60))
    Canvas(Modifier.fillMaxSize()) {
        val t = (frame - startMs).coerceAtLeast(nowMs - startMs) / 1000.0
        for (i in 0 until 90) {
            val seed = i * 12.9898
            val x0 = (sin(seed) * 0.5 + 0.5) * size.width
            val speed = 60 + (cos(seed * 1.7) * 0.5 + 0.5) * 120
            val y = ((i % 15) * -40.0 + (t % 12) * speed * density).mod(size.height + 40.0) - 20
            val x = x0 + sin(t * 2 + seed) * 18 * density
            rotate(((t * 3 + seed) * 57.3).toFloat(), Offset(x.toFloat(), y.toFloat())) {
                drawRect(colors[i % colors.size], Offset(x.toFloat() - 4 * density, y.toFloat() - 2 * density), Size(8 * density, 4 * density))
            }
        }
    }
}

/** Ekran karesi zamanı (ms) */
@Composable
private fun produceFrameMs(): androidx.compose.runtime.State<Long> = androidx.compose.runtime.produceState(System.currentTimeMillis()) {
    while (true) androidx.compose.runtime.withFrameMillis { value = System.currentTimeMillis() }
}
