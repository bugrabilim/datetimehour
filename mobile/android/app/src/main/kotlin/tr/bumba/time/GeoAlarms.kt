package tr.bumba.time

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Konuma bağlı hatırlatıcı (rakip analizi listesi #93; iOS'taki GeoAlarms ile aynı kayıt `geoAlarms`): bir yere varınca ya
 * da oradan ayrılınca sesli bildirim. Google Play hizmetlerinin bölge izleme (geofence) özelliği kullanılır; konum takibini
 * sistem yapar, uygulama arka planda çalışmaz, konum hiçbir yere gönderilmez. Android 10+ bölge izleme için "her zaman"
 * konum izni ister (ACCESS_BACKGROUND_LOCATION; yalnız kullanıcı hatırlatıcı eklerken istenir, Play beyanı PLAY_CONSOLE.md).
 * Bölgeler telefon yeniden başlayınca silindiği için AlarmScheduler.restoreAll yeniden kurar.
 */
data class GeoAlarm(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val lat: Double,
    val lon: Double,
    /** Yarıçap (m) */
    val r: Int = 200,
    /** "arrive" (varınca) ya da "leave" (ayrılınca) */
    val on: String = "arrive",
    /** Her seferinde mi, bir kez mi */
    val repeats: Boolean = true,
    val enabled: Boolean = true,
) {
    val title: String get() = label.ifBlank { L("geo.defaultLabel") }

    fun toJson(): JSONObject = JSONObject().put("id", id).put("label", label).put("lat", lat).put("lon", lon).put("r", r)
        .put("on", on).put("repeats", repeats).put("enabled", enabled)

    companion object {
        val RADII = listOf(100, 200, 300, 500, 1000)

        fun fromJson(o: JSONObject) = GeoAlarm(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() }, label = o.optString("label"),
            lat = o.getDouble("lat"), lon = o.getDouble("lon"), r = o.optInt("r", 200),
            on = o.optString("on", "arrive"), repeats = o.optBoolean("repeats", true), enabled = o.optBoolean("enabled", true),
        )
    }
}

object GeoAlarms {
    const val KEY = "geoAlarms"
    const val LIMIT = 20
    private const val CHANNEL = "geo"

    fun load(): List<GeoAlarm> = runCatching {
        val a = JSONArray(Prefs.store.getString(KEY, "[]"))
        (0 until a.length()).map { GeoAlarm.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun save(list: List<GeoAlarm>) {
        Prefs.store.edit().putString(KEY, JSONArray(list.take(LIMIT).map { it.toJson() }).toString()).apply()
    }

    fun hasFine(c: Context) = ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasBackground(c: Context) = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun pending(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 3100, Intent(c, GeoAlarmReceiver::class.java),
        // Bölge olayı niyete eklendiği için değiştirilebilir olmalı
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0),
    )

    /** Bölgeleri baştan kurar (izin yoksa yalnız siler) */
    @SuppressLint("MissingPermission")
    fun sync(c: Context) {
        val client = runCatching { LocationServices.getGeofencingClient(c) }.getOrNull() ?: return
        val pi = pending(c)
        runCatching { client.removeGeofences(pi) }
        val list = load().filter { it.enabled }
        if (list.isEmpty() || !hasFine(c) || !hasBackground(c)) return
        val fences = list.map { g ->
            Geofence.Builder()
                .setRequestId(g.id)
                .setCircularRegion(g.lat, g.lon, g.r.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(if (g.on == "leave") Geofence.GEOFENCE_TRANSITION_EXIT else Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }
        val request = GeofencingRequest.Builder()
            // Zaten içerideyken eklenen "varınca" hatırlatıcısı hemen çalmasın
            .setInitialTrigger(0)
            .addGeofences(fences)
            .build()
        runCatching { client.addGeofences(request, pi) }
    }

    fun createChannel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, L("geo.title"), NotificationManager.IMPORTANCE_HIGH))
    }

    /** Bölgeye girildi ya da çıkıldı: bildirim; bir kezlik hatırlatıcı kapanır */
    fun fired(c: Context, ids: List<String>) {
        val list = load()
        val hit = list.filter { it.id in ids && it.enabled }
        if (hit.isEmpty()) return
        createChannel(c)
        val open = PendingIntent.getActivity(
            c, 3101, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        for (g in hit) {
            val n = NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_prayer)
                .setContentTitle(g.title)
                .setContentText(L(if (g.on == "leave") "geo.left" else "geo.arrived"))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            runCatching { NotificationManagerCompat.from(c).notify("geo", g.id.hashCode(), n) }
        }
        if (hit.any { !it.repeats }) {
            save(list.map { g -> if (g.id in ids && !g.repeats) g.copy(enabled = false) else g })
            sync(c)
        }
    }

    /** Şu anki konum (kesin; izin yoksa ya da bulunamazsa null) */
    @SuppressLint("MissingPermission")
    suspend fun locate(c: Context): Pair<Double, Double>? {
        if (!hasFine(c)) return null
        val client = runCatching { LocationServices.getFusedLocationProviderClient(c) }.getOrNull() ?: return null
        return suspendCancellableCoroutine { cont ->
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc -> cont.resume(loc?.let { it.latitude to it.longitude }) }
                .addOnFailureListener { cont.resume(null) }
        }
    }
}

class GeoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val ids = event.triggeringGeofences?.map { it.requestId } ?: return
        Prefs.init(context)
        GeoAlarms.fired(context, ids)
    }
}

/** Konum hatırlatıcıları listesi (Araçlar › Alarm › Konuma bağlı hatırlatıcılar) */
@Composable
fun GeoAlarmsScreen() {
    val context = LocalContext.current
    var list by remember { mutableStateOf(GeoAlarms.load()) }
    var editing by remember { mutableStateOf<GeoAlarm?>(null) }
    var fine by remember { mutableStateOf(GeoAlarms.hasFine(context)) }
    var background by remember { mutableStateOf(GeoAlarms.hasBackground(context)) }
    LifecycleResumeEffect(Unit) {
        fine = GeoAlarms.hasFine(context)
        background = GeoAlarms.hasBackground(context)
        list = GeoAlarms.load()
        GeoAlarms.sync(context)
        onPauseOrDispose { }
    }
    val askBackground = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        background = it
        GeoAlarms.sync(context)
    }
    val askFine = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        fine = r[Manifest.permission.ACCESS_FINE_LOCATION] == true
        GeoAlarms.sync(context)
    }

    fun update(l: List<GeoAlarm>) {
        list = l
        GeoAlarms.save(l)
        GeoAlarms.sync(context)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("geo.title"), Modifier.weight(1f), color = Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (list.size < GeoAlarms.LIMIT) {
                IconButton(onClick = {
                    val p = AppPlace.current()
                    editing = GeoAlarm(lat = p.lat, lon = p.lon)
                }) { Icon(Icons.Filled.Add, L("geo.add"), tint = Theme.c.accent) }
            }
        }
        AppCard(padding = 4) {
            if (list.isEmpty()) Text(L("geo.none"), Modifier.padding(12.dp), color = Theme.c.muted)
            for (g in list) {
                Row(Modifier.fillMaxWidth().clickable { editing = g }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(g.title, color = if (g.enabled) Theme.c.text else Theme.c.muted, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(L(if (g.on == "leave") "geo.onLeave" else "geo.onArrive") + " · " + T("geo.radiusM", g.r) +
                            if (g.repeats) "" else " · " + L("geo.once"), color = Theme.c.muted, fontSize = 13.sp)
                    }
                    Switch(g.enabled, { on -> update(list.map { if (it.id == g.id) it.copy(enabled = on) else it }) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
            }
        }
        when {
            !fine -> TextButton(onClick = {
                askFine.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }) { Text(L("geo.allow"), color = Theme.c.accent) }
            !background && Build.VERSION.SDK_INT >= 29 -> {
                Text(L("geo.backgroundAndroid"), color = Theme.c.muted, fontSize = 13.sp)
                TextButton(onClick = { askBackground.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }) {
                    Text(L("geo.allowAlways"), color = Theme.c.accent)
                }
            }
        }
        Text(L("geo.noteAndroid"), color = Theme.c.muted, fontSize = 13.sp)
    }

    editing?.let { g ->
        GeoAlarmEditor(g, onDismiss = { editing = null }, onDelete = {
            update(list.filter { it.id != g.id })
            editing = null
        }) { saved ->
            update(if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved)
            editing = null
            if (!fine) askFine.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}

@Composable
private fun GeoAlarmEditor(start: GeoAlarm, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (GeoAlarm) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var g by remember { mutableStateOf(start) }
    var locating by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(g) }) { Text(L("alarm.music.save")) } },
        dismissButton = {
            Row {
                if (GeoAlarms.load().any { it.id == g.id }) IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, L("geo.delete"), tint = Theme.c.muted) }
                TextButton(onClick = onDismiss) { Text(L("alarm.cancel")) }
            }
        },
        title = { Text(L("geo.title")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(g.label, { g = g.copy(label = it.take(40)) }, label = { Text(L("geo.labelPlaceholder")) }, singleLine = true)
                Text(String.format(Locale.US, "%.5f, %.5f", g.lat, g.lon), color = Theme.c.muted, fontSize = 13.sp)
                TextButton(enabled = !locating, onClick = {
                    locating = true
                    scope.launch {
                        val here = GeoAlarms.locate(context)
                        if (here != null) g = g.copy(lat = here.first, lon = here.second) else msg = L("geo.noLocation")
                        locating = false
                    }
                }) { Text(L(if (locating) "geo.locating" else "geo.useMine"), color = Theme.c.accent) }
                if (msg.isNotEmpty()) Text(msg, color = Theme.c.muted, fontSize = 13.sp)
                Text(L("geo.placeHintAndroid"), color = Theme.c.muted, fontSize = 12.sp)
                ChoiceRow(L("geo.when"), L(if (g.on == "leave") "geo.onLeave" else "geo.onArrive"),
                    listOf(0 to L("geo.onArrive"), 1 to L("geo.onLeave"))) { g = g.copy(on = if (it == 1) "leave" else "arrive") }
                ChoiceRow(L("geo.radius"), T("geo.radiusM", g.r), GeoAlarm.RADII.map { it to T("geo.radiusM", it) }) { g = g.copy(r = it) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L("geo.every"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Switch(g.repeats, { g = g.copy(repeats = it) }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
                }
            }
        },
    )
}

/**
 * Takvimdeki bir etkinliğe alarm (#93; iOS CalendarAlarm ile aynı kural): etkinliğin başlangıcından seçilen süre önce bir
 * kezlik alarm (tüm gün etkinlikte o gün 09.00). Yalnız o anki etkinlik bilgisi kullanılır.
 */
object CalendarAlarm {
    const val LEAD_KEY = "alarm.calLead"
    val LEADS = listOf(0, 5, 10, 15, 30, 60)

    fun item(title: String, start: java.time.LocalDateTime, lead: Int, now: java.time.LocalDateTime = java.time.LocalDateTime.now()): AlarmItem? {
        val allDay = start.toLocalTime() == java.time.LocalTime.MIDNIGHT
        val at = if (allDay) start.withHour(9) else start.minusMinutes(lead.toLong())
        if (!at.isAfter(now)) return null
        return AlarmItem(hour = at.hour, minute = at.minute, label = title.take(40), date = at.toLocalDate().toString())
    }
}

/** Telefondaki takvimlerden önümüzdeki 60 günün etkinlikleri; seçince başlık ve başlangıç */
@Composable
fun CalendarEventDialog(onDismiss: () -> Unit, onPick: (String, java.time.LocalDateTime) -> Unit) {
    val context = LocalContext.current
    val events = remember { upcomingDeviceEvents(context) }
    val zone = java.time.ZoneId.systemDefault()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(L("alarm.cancel")) } },
        title = { Text(L("alarm.fromCalendar")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    !DeviceCalendar.authorized -> Text(L("countdowns.calendarDenied"), color = Theme.c.muted)
                    events.isEmpty() -> Text(L("countdowns.calendarNone"), color = Theme.c.muted)
                    else -> for ((name, start) in events) {
                        Column(Modifier.fillMaxWidth().clickable { onPick(name, start) }.padding(vertical = 10.dp)) {
                            Text(name, color = Theme.c.text, fontSize = 15.sp, maxLines = 1)
                            Text(Fmt.longDate(start.toLocalDate()) +
                                if (start.toLocalTime() != java.time.LocalTime.MIDNIGHT) " " + Fmt.time(start.atZone(zone)) else "",
                                color = Theme.c.muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
    )
}
