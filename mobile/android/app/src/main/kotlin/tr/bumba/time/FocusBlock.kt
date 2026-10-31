package tr.bumba.time

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay

/**
 * Odakta uygulama engelleme (rakip analizi listesi #98; iOS'ta Ekran Süresi kalkanı): pomodoro çalışma süresi işlerken
 * seçilen uygulamalardan biri öne gelirse "Odaklanma zamanı" ekranı açılır. Öne gelen uygulama kullanım erişimiyle
 * (PACKAGE_USAGE_STATS, kullanıcı Ayarlar'dan verir) saniyede bir okunur; ekran "Diğer uygulamaların üzerinde göster"
 * izniyle açılır (yüzen saatle aynı izin). Ön plan hizmeti yalnız çalışma süresince durur; hiçbir şey kaydedilmez ya da
 * gönderilmez. Sitelerin engellenmesi Android'de yok (tarayıcı içindeki sayfalar izlenemez).
 * Kayıt: `focus.block` (açık mı), `focus.apps` (paket adları, virgülle).
 */
object FocusBlock {
    const val ON = "focus.block"
    const val APPS = "focus.apps"

    val on: Boolean get() = Prefs.store.getBoolean(ON, false)

    fun apps(): Set<String> = Prefs.store.getString(APPS, "").orEmpty().split(",").filter { it.isNotBlank() }.toSet()

    fun saveApps(set: Set<String>) { Prefs.store.edit().putString(APPS, set.sorted().joinToString(",")).apply() }

    /** Pomodoro çalışma süresi işliyor mu */
    fun working(now: Long = System.currentTimeMillis()): Boolean =
        Prefs.store.getString("pomo.phase", "work") == "work" && Prefs.store.getLong("pomo.end", 0) > now

    /** Engel şu an etkin mi (açık, uygulama seçili, çalışma süresi işliyor) */
    fun active(now: Long = System.currentTimeMillis()) = on && apps().isNotEmpty() && working(now)

    fun hasUsageAccess(c: Context): Boolean {
        val ops = c.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        else @Suppress("DEPRECATION") ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun canShowOver(c: Context) = Settings.canDrawOverlays(c)

    /** Ana ekranda simgesi olan uygulamalar (bu uygulama hariç), ada göre */
    fun launchable(c: Context): List<Pair<String, String>> {
        val pm = c.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != c.packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }

    /** Hizmeti pomodoro durumuna göre başlatır ya da durdurur */
    fun sync(c: Context) {
        if (active() && hasUsageAccess(c)) {
            runCatching { ContextCompat.startForegroundService(c, Intent(c, FocusBlockService::class.java)) }
        } else {
            runCatching { c.stopService(Intent(c, FocusBlockService::class.java)) }
        }
    }
}

class FocusBlockService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastShown = ""
    private var lastShownAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FocusBlock.active()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(NotificationChannel(CHANNEL, L("focus.title"), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        val open = PendingIntent.getActivity(
            this, 3200, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val end = Prefs.store.getLong("pomo.end", 0)
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(L("focus.title"))
            .setContentText(T("focus.blocking", FocusBlock.apps().size))
            .setOngoing(true)
            .setSilent(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(end)
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(ID, n)
        handler.removeCallbacksAndMessages(null)
        handler.post(tick)
        return START_STICKY
    }

    private val tick: Runnable = object : Runnable {
        override fun run() {
            if (!FocusBlock.active()) {
                stopSelf()
                return
            }
            val now = System.currentTimeMillis()
            val pkg = foreground(now)
            // Aynı uygulama için ekran en çok 3 sn'de bir açılır (geri tuşu döngüsü olmasın)
            if (pkg != null && pkg in FocusBlock.apps() && (pkg != lastShown || now - lastShownAt > 3000) && FocusBlock.canShowOver(this@FocusBlockService)) {
                lastShown = pkg
                lastShownAt = now
                runCatching {
                    startActivity(Intent(this@FocusBlockService, FocusBlockActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra("pkg", pkg))
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    /** Son 10 sn'de öne gelen son uygulama */
    private fun foreground(now: Long): String? {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return null
        val events = runCatching { usm.queryEvents(now - 10_000, now) }.getOrNull() ?: return null
        val e = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) last = e.packageName
        }
        return last
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "focus"
        const val ID = 3201
    }
}

/** "Odaklanma zamanı": engelli uygulamanın üstünde açılır; kalan süre, görev ve ana ekrana dönüş */
class FocusBlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContent {
            AppTheme {
                BackHandler { home() }
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                val end = Prefs.store.getLong("pomo.end", 0)
                LaunchedEffect(Unit) {
                    while (true) {
                        now = System.currentTimeMillis()
                        if (!FocusBlock.active(now)) { finish(); break }
                        delay(500)
                    }
                }
                Column(
                    Modifier.fillMaxSize().background(Theme.c.bg).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    Text("🎯", fontSize = 56.sp)
                    Text(L("focus.screenTitle"), color = Theme.c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Ltr { Text(clock(maxOf(0, end - now) / 1000), color = Theme.c.accent, fontSize = 48.sp, fontWeight = FontWeight.Light, style = Tnum) }
                    val task = Prefs.store.getString("pomo.task", "").orEmpty()
                    if (task.isNotBlank()) Text(task, color = Theme.c.text, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Text(L("focus.screenNote"), color = Theme.c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { home() }) { Text(L("focus.home"), color = Theme.c.accent, fontSize = 17.sp) }
                    TextButton(onClick = {
                        startActivity(Intent(this@FocusBlockActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra("shortcut", "pomodoro"))
                        finish()
                    }) { Text(L("focus.openApp"), color = Theme.c.muted, fontSize = 15.sp) }
                }
            }
        }
    }

    private fun home() {
        runCatching { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        finish()
    }
}

/** Pomodoro sayfasındaki kart: aç/kapat, uygulama seçimi, izinler */
@Composable
fun FocusBlockCard() {
    val context = LocalContext.current
    var on by remember { mutableStateOf(FocusBlock.on) }
    var apps by remember { mutableStateOf(FocusBlock.apps()) }
    var usage by remember { mutableStateOf(FocusBlock.hasUsageAccess(context)) }
    var over by remember { mutableStateOf(FocusBlock.canShowOver(context)) }
    var picking by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        usage = FocusBlock.hasUsageAccess(context)
        over = FocusBlock.canShowOver(context)
        FocusBlock.sync(context)
        onPauseOrDispose { }
    }
    AppCard {
        SectionTitle(L("focus.title"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L("focus.toggle"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
            Switch(on, { v ->
                on = v
                Prefs.store.edit().putBoolean(FocusBlock.ON, v).apply()
                if (v && apps.isEmpty()) picking = true
                FocusBlock.sync(context)
            }, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
        }
        if (on) {
            Text(T("focus.chosenAndroid", apps.size), Modifier.fillMaxWidth().clickable { picking = true }.padding(vertical = 6.dp),
                color = Theme.c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (!usage) Text(L("focus.needUsage"), Modifier.fillMaxWidth().clickable {
                runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }.padding(vertical = 6.dp), color = Theme.c.accent, fontSize = 14.sp)
            if (!over) Text(L("focus.needOverlay"), Modifier.fillMaxWidth().clickable {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }.padding(vertical = 6.dp), color = Theme.c.accent, fontSize = 14.sp)
        }
        Text(L("focus.noteAndroid"), color = Theme.c.muted, fontSize = 12.sp)
    }
    if (picking) {
        AppPickerDialog(apps, onDismiss = { picking = false }) { chosen ->
            apps = chosen
            FocusBlock.saveApps(chosen)
            picking = false
            FocusBlock.sync(context)
        }
    }
}

@Composable
private fun AppPickerDialog(start: Set<String>, onDismiss: () -> Unit, onDone: (Set<String>) -> Unit) {
    val context = LocalContext.current
    val all = remember { FocusBlock.launchable(context) }
    var chosen by remember { mutableStateOf(start) }
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onDone(chosen) }) { Text(L("common.done")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L("alarm.cancel")) } },
        title = { Text(L("focus.pickApps")) },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, singleLine = true, label = { Text(L("place.search")) })
                val shown = all.filter { query.isBlank() || it.second.contains(query, ignoreCase = true) }
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(shown, key = { it.first }) { (pkg, name) ->
                        val on = pkg in chosen
                        Row(Modifier.fillMaxWidth().clickable { chosen = if (on) chosen - pkg else chosen + pkg }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(on, { chosen = if (on) chosen - pkg else chosen + pkg }, colors = CheckboxDefaults.colors(checkedColor = Theme.c.accent))
                            Text(name, color = Theme.c.text, fontSize = 15.sp, maxLines = 1)
                        }
                    }
                }
            }
        },
    )
}
