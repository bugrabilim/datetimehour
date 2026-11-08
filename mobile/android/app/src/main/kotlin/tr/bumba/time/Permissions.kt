package tr.bumba.time

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Ayarlar › İzinler (5 Kasım 2026, sahibin isteği): uygulamanın isteyebileceği her izin, ne için olduğu ("izin verirseniz…
 * vermezseniz…"), şu anki durumu ve dokununca telefonun ilgili ayarı. İzinler yalnız o özellik ilk kullanılınca sorulur;
 * burada kapatılır ya da yeniden açılır. Üstte güven kartı (hesap, reklam, takip yok; verileriniz bize gelmez).
 * iOS'taki PermissionsView ile aynı metin anahtarları (`perms.<kimlik>.title/why`).
 */
object AppPermissions {
    /** granted: şu anki durum; open: dokununca açılacak ayar ekranı */
    class Item(val id: String, val icon: String, val granted: (Context) -> Boolean, val open: (Context) -> Intent)

    private fun has(c: Context, p: String) = ContextCompat.checkSelfPermission(c, p) == PackageManager.PERMISSION_GRANTED
    private fun pkg(c: Context) = Uri.fromParts("package", c.packageName, null)
    fun appDetails(c: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg(c))

    fun items(): List<Item> = buildList {
        add(Item("location", "pin", { has(it, Manifest.permission.ACCESS_FINE_LOCATION) || has(it, Manifest.permission.ACCESS_COARSE_LOCATION) }, ::appDetails))
        if (GeoAlarms.ENABLED && Build.VERSION.SDK_INT >= 29) add(Item("locationBg", "compass", { has(it, Manifest.permission.ACCESS_BACKGROUND_LOCATION) }, ::appDetails))
        add(Item("notify", "bell", { NotificationManagerCompat.from(it).areNotificationsEnabled() }) { c ->
            if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName)
            else appDetails(c)
        })
        if (Build.VERSION.SDK_INT >= 31) add(Item("exactAlarm", "alarm",
            { (it.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms() },
            { c -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg(c)) }))
        if (Build.VERSION.SDK_INT >= 34) add(Item("fullScreen", "fullscreen",
            { (it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent() },
            { c -> Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg(c)) }))
        add(Item("calendar", "calendar", { has(it, Manifest.permission.READ_CALENDAR) }, ::appDetails))
        add(Item("mic", "mic", { has(it, Manifest.permission.RECORD_AUDIO) }, ::appDetails))
        add(Item("health", "heart", { HealthLog.enabled }) { c ->
            runCatching { Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS") }.getOrElse { appDetails(c) }
        })
        add(Item("overlay", "overlay", { Settings.canDrawOverlays(it) }) { c -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg(c)) })
        add(Item("usage", "chart", { FocusBlock.hasUsageAccess(it) }) { Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS) })
        add(Item("dnd", "moon", { (it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted }) {
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        })
    }

    /** Ayar ekranını açar; açılamazsa uygulamanın genel ayarları */
    fun open(c: Context, item: Item) {
        val intent = item.open(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { c.startActivity(intent) }.onFailure { runCatching { c.startActivity(appDetails(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
}

@Composable
fun PermissionsScreen() {
    val context = LocalContext.current
    // Ayarlardan dönünce durumlar yenilensin
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val items = remember { AppPermissions.items() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TrustCard()
        Text(L("perms.note"), color = Theme.c.muted, fontSize = 14.sp, lineHeight = 20.sp)
        AppCard(padding = 4) {
            for (item in items) {
                val on = remember(tick) { runCatching { item.granted(context) }.getOrDefault(false) }
                PermissionRow(item.id, item.icon, on) { AppPermissions.open(context, item) }
            }
        }
        AppCard(padding = 16) {
            SectionTitle(L("perms.notUsed.title"))
            Text(L("perms.notUsed.android"), color = Theme.c.muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
        RoundButton(L("perms.open"), false, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            runCatching { context.startActivity(AppPermissions.appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}

/** İzin satırı: iki tonlu simge (adaçayı), ad, ne için; sağda durum (Açık / Kapalı) */
@Composable
private fun PermissionRow(id: String, icon: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DuoChip(icon, Theme.c.sage)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(L("perms.$id.title"), color = Theme.c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(L("perms.$id.why"), color = Theme.c.muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            L(if (on) "perms.status.on" else "perms.status.off"),
            Modifier.background(if (on) Theme.c.accent.copy(alpha = 0.14f) else Theme.c.surface2, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            color = if (on) Theme.c.accent else Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}
