package tr.bumba.time

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Telefondaki takvimler (Google, Outlook, Samsung… telefona ekli hepsi; iOS'taki DeviceCalendar). Okumak için
 * takvim izni gerekir; etkinlik eklemek sistemin takvim uygulamasıyla izinsiz de çalışır. Veriler telefonda kalır.
 */
object DeviceCalendar {
    data class Event(
        val id: Long,
        val title: String,
        val begin: Instant,
        val end: Instant,
        val allDay: Boolean,
        val color: Color,
        val calendar: String,
        val location: String,
        /** Tekrarlanan etkinlik tek tek silinmez (seri bütünüyle gider); yalnız tek seferlikler silinir */
        val deletable: Boolean,
    )

    /** Takvim değişince artar (ekranlar yeniden okur) */
    var version by mutableIntStateOf(0)
        private set
    var authorized by mutableStateOf(false)
        private set
    private var observing = false

    val permissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

    fun reload(c: Context) {
        authorized = ContextCompat.checkSelfPermission(c, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (authorized && !observing) {
            observing = true
            c.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) { version++ }
                })
        }
        version++
    }

    /** Ayın etkinlikleri, gün gün (birden çok güne yayılanlar her günde; tüm gün olanlar önce) */
    fun events(c: Context, month: YearMonth): Map<LocalDate, List<Event>> {
        if (!authorized) return emptyMap()
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start.toEpochMilli() - 86_400_000)
            ContentUris.appendId(it, end.toEpochMilli() + 86_400_000)
        }.build()
        val cols = arrayOf(
            CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME, CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.RRULE, CalendarContract.Instances.CALENDAR_ACCESS_LEVEL,
        )
        val out = HashMap<LocalDate, MutableList<Event>>()
        runCatching {
            c.contentResolver.query(uri, cols, null, null, CalendarContract.Instances.BEGIN)?.use { cur ->
                while (cur.moveToNext()) {
                    val allDay = cur.getInt(4) == 1
                    val e = Event(
                        id = cur.getLong(0),
                        title = cur.getString(1) ?: "",
                        begin = Instant.ofEpochMilli(cur.getLong(2)),
                        end = Instant.ofEpochMilli(cur.getLong(3)),
                        allDay = allDay,
                        color = Color(cur.getInt(5) or 0xFF000000.toInt()),
                        calendar = cur.getString(6) ?: "",
                        location = cur.getString(7) ?: "",
                        deletable = cur.getString(8).isNullOrEmpty() &&
                            cur.getInt(9) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                    )
                    // Tüm gün etkinlikleri UTC gece yarısıyla saklanır; bitiş gününü kapsamaz
                    val z = if (allDay) ZoneOffset.UTC else zone
                    var d = e.begin.atZone(z).toLocalDate()
                    val last = e.end.minusMillis(1).atZone(z).toLocalDate().let { if (it.isBefore(d)) d else it }
                    while (!d.isAfter(last)) {
                        if (YearMonth.from(d) == month) out.getOrPut(d) { ArrayList() }.add(e)
                        d = d.plusDays(1)
                    }
                }
            }
        }
        return out.mapValues { (_, v) -> v.sortedWith(compareBy({ !it.allDay }, { it.begin })) }
    }

    /** Sistemin takvim uygulamasında yeni etkinlik: boş (gün 09:00–10:00) ya da özel günden (tüm gün) */
    fun add(c: Context, day: LocalDate, title: String? = null) {
        val zone = ZoneId.systemDefault()
        val i = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        if (title != null) {
            val ms = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            i.putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ms)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, ms + 86_400_000)
        } else {
            val ms = day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
            i.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ms).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, ms + 3_600_000)
        }
        runCatching { c.startActivity(i) }
    }

    /** Sistemin etkinlik ayrıntısı (düzenleme dahil) */
    fun open(c: Context, e: Event) {
        val i = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, e.id))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.begin.toEpochMilli())
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.end.toEpochMilli())
        runCatching { c.startActivity(i) }
    }

    fun remove(c: Context, e: Event) {
        runCatching { c.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, e.id), null, null) }
        reload(c)
    }
}

/** İzin isteyen düğme ("Telefonumdaki takvimleri göster") */
@Composable
fun rememberCalendarPermission(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        DeviceCalendar.reload(context)
    }
    return { launcher.launch(DeviceCalendar.permissions) }
}

/**
 * Google Takvimi bağlama rehberi (Android). Google hesabı telefona ekliyse takvimleri telefonun takvim veritabanına
 * gelir; uygulama onları izinle okur ve yeni etkinliği Google takvimine de kaydettirebilir. Google'a doğrudan
 * bağlanılmaz, hesap bilgisi görülmez.
 */
@Composable
fun GoogleCalendarGuide() {
    val context = LocalContext.current
    val ask = rememberCalendarPermission()
    DeviceCalendar.version
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard { Text(L("google.android.lead"), color = Theme.c.text, fontSize = 15.sp) }
        AppCard {
            SectionTitle(L("google.stepsTitle"))
            for (i in 1..4) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(26.dp).background(Theme.c.accentSoft, CircleShape), contentAlignment = Alignment.Center) {
                        Text("$i", color = Theme.c.accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(L("google.android.step$i"), color = Theme.c.text, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            TextButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_SYNC_SETTINGS)) } }) {
                Text(L("google.openSettings"), color = Theme.c.accent)
            }
        }
        AppCard {
            if (DeviceCalendar.authorized) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(8.dp))
                    Text(L("google.ready"), color = Color(0xFF2E7D32))
                }
            } else {
                TextButton(onClick = ask) {
                    Icon(Icons.Filled.EventAvailable, null, tint = Theme.c.accent)
                    Text("  " + L("calendar.showDevice"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                }
                Text(L("google.android.denied"), color = Theme.c.muted, fontSize = 13.sp)
            }
            Text(L("google.note"), color = Theme.c.muted, fontSize = 13.sp)
        }
    }
}
