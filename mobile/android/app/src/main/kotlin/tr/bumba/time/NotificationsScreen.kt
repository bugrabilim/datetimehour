package tr.bumba.time

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import java.time.LocalDate
import java.time.ZoneId

/**
 * Ayarlar › Bildirimler (iOS'taki NotificationsView): her özelliğin bildirimi (varsayılan açık) tek sayfada;
 * vakitlerin ayrıntısı (hangi vakitler, kaç dakika önce, ses) kendi sayfasında ("prayerNotify").
 */
@Composable
fun NotificationsScreen(onRoute: (String) -> Unit) {
    val context = LocalContext.current
    NotifyPrefs.load()
    var allowed by remember { mutableStateOf(PrayerNotifier.notificationsAllowed(context)) }
    LifecycleResumeEffect(Unit) {
        allowed = PrayerNotifier.notificationsAllowed(context)
        onPauseOrDispose {}
    }
    fun changed() = PrayerNotifier.reschedule(context)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard { Text(L("notify.all.note"), color = Theme.c.muted, fontSize = 14.sp) }
        if (!allowed) {
            AppCard {
                Text(L("notify.denied"), color = Theme.c.text, fontSize = 14.sp)
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Text(L("notify.openSettings"), color = Theme.c.accent) }
            }
        }
        AppCard(padding = 4) {
            ToggleRow("mosque", Theme.c.gold, L("notify.cat.prayer"), L("notify.cat.prayer.note"), NotifyPrefs.enabled) {
                NotifyPrefs.saveEnabled(it)
                changed()
            }
            if (NotifyPrefs.enabled) {
                RowDivider()
                Row(
                    Modifier.fillMaxWidth().clickable { onRoute("prayerNotify") }.padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(L("notify.heading"), Modifier.weight(1f), color = Theme.c.accent, fontSize = 15.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
                }
            }
        }
        AppCard(padding = 4) {
            FeatureToggle(NotifyFeature.CALENDAR, "calendar", Theme.c.sage, ::changed)
            RowDivider()
            FeatureToggle(NotifyFeature.DAILY, "speech", Theme.c.sage, ::changed)
            RowDivider()
            FeatureToggle(NotifyFeature.BEDTIME, "bed", Theme.c.terra, ::changed)
            FeatureToggle(NotifyFeature.HABITS, "coffee", Theme.c.terra, ::changed)
        }
        AppCard(padding = 4) {
            FeatureToggle(NotifyFeature.SEASONS, "leaf", Theme.c.sky, ::changed)
            RowDivider()
            val moonOn = FeatureToggle(NotifyFeature.MOON, "moon", Theme.c.sky, ::changed)
            // Olay seçimi ve önceden haber verme (2. analiz #152)
            if (moonOn) SunMoonOptions(moon = true, ::changed)
            RowDivider()
            val sunOn = FeatureToggle(NotifyFeature.SUN, "sunrise", Theme.c.sky, ::changed)
            if (sunOn) SunMoonOptions(moon = false, ::changed)
            RowDivider()
            FeatureToggle(NotifyFeature.SKY, "telescope", Theme.c.sky, ::changed)
        }
        AppCard(padding = 4) {
            val on = FeatureToggle(NotifyFeature.WEATHER, "sun_cloud", Theme.c.sky, ::changed)
            if (on) {
                RowDivider()
                // Sabah 05.00–11.00 arası, yarım saatte bir
                TimeChoice(L("notify.cat.weather.time"), FeaturePrefs.weatherMinutes, (10..22).map { it * 30 }, ::minutesLabel) {
                    FeaturePrefs.weatherMinutes = it
                    changed()
                }
                RowDivider()
                // Yarın belirgin soğuk/sıcak ve yağmur yaklaşıyor (2. analiz #110)
                var alerts by remember { mutableStateOf(Prefs.store.getBoolean("notify.weather.alerts", true)) }
                ToggleRow("umbrella", Theme.c.sky, L("notify.weatherAlerts"), L("notify.weatherAlerts.note"), alerts) {
                    alerts = it; Prefs.store.edit().putBoolean("notify.weather.alerts", it).apply(); changed()
                }
            }
        }
        AppCard(padding = 4) {
            val on = FeatureToggle(NotifyFeature.HOURLY, "clock", Theme.c.terra, ::changed)
            if (on) {
                RowDivider()
                TimeChoice(L("notify.cat.hourly.from"), FeaturePrefs.hourlyFrom, (0..23).toList(), { minutesLabel(it * 60) }) {
                    FeaturePrefs.hourlyFrom = it
                    changed()
                }
                RowDivider()
                TimeChoice(L("notify.cat.hourly.to"), FeaturePrefs.hourlyTo, (0..23).toList(), { minutesLabel(it * 60) }) {
                    FeaturePrefs.hourlyTo = it
                    changed()
                }
            }
            RowDivider()
            FeatureToggle(NotifyFeature.DST, "dst", Theme.c.terra, ::changed)
        }
    }
}

/** Özellik satırı; açık mı döner (alttaki ayarlar için) */
@Composable
private fun FeatureToggle(f: NotifyFeature, icon: String, tint: Color, changed: () -> Unit): Boolean {
    var on by remember { mutableStateOf(FeaturePrefs.isOn(f)) }
    ToggleRow(icon, tint, f.title, f.note, on) {
        on = it
        FeaturePrefs.set(f, it)
        changed()
    }
    return on
}

@Composable
private fun ToggleRow(icon: String, tint: Color, title: String, note: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DuoChip(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Theme.c.text, fontSize = 16.sp)
            Text(note, color = Theme.c.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        SoftSwitch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
    }
}

/** Değer seçimi (açılır liste); seçenekler gece yarısından dakika ya da saat */
@Composable
private fun TimeChoice(title: String, value: Int, options: List<Int>, label: (Int) -> String, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var current by remember { mutableIntStateOf(value) }
    Row(
        Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        Box {
            Text(label(current), color = Theme.c.accent, fontSize = 15.sp)
            DropdownMenu(open, { open = false }) {
                for (o in options) {
                    DropdownMenuItem(text = { Text(label(o)) }, onClick = {
                        open = false
                        current = o
                        onPick(o)
                    })
                }
            }
        }
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(Modifier.padding(start = 50.dp), color = Theme.c.border.copy(alpha = 0.5f))

/** Gece yarısından dakika → "07:30" ya da 12 saatte "7:30 AM" */
private fun minutesLabel(m: Int): String {
    val at = LocalDate.now().atTime(m / 60, m % 60).atZone(ZoneId.systemDefault())
    return listOf(Fmt.time(at), Fmt.ampm(at)).filter { it.isNotEmpty() }.joinToString(" ")
}

/** İlk açılışta bildirim izni bir kez sorulur (Android 13+; bildirimler varsayılan açık); sonra yalnız Ayarlar'dan */
@Composable
fun AskNotificationsOnce() {
    if (Build.VERSION.SDK_INT < 33 || LocalActivityResultRegistryOwner.current == null) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { PrayerNotifier.reschedule(context) }
    // Önce kısa açıklama (2. analiz #80); "Şimdi değil"de sistem penceresi açılmaz, Ayarlar › Bildirimler'den istenir
    var primer by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val sp = Prefs.store
        if (sp.getBoolean("notify.asked", false)) return@LaunchedEffect
        sp.edit().putBoolean("notify.asked", true).apply()
        if (!PrayerNotifier.notificationsAllowed(context)) primer = true
    }
    if (primer) PermissionPrimer(PermissionKind.NOTIFY, onAllow = {
        primer = false
        runCatching { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }, onNotNow = { primer = false })
}

/** Güneş/Ay bildirimi ayrıntısı (2. analiz #152): olaylar, önceden haber verme, (Güneş için) günler */
@Composable
private fun SunMoonOptions(moon: Boolean, changed: () -> Unit) {
    var events by remember { mutableStateOf(if (moon) SunMoonPrefs.moonEvents else SunMoonPrefs.sunEvents) }
    var lead by remember { mutableIntStateOf(if (moon) SunMoonPrefs.moonLead else SunMoonPrefs.sunLead) }
    var days by remember { mutableStateOf(SunMoonPrefs.sunDays) }
    val all = if (moon) SunMoonPrefs.MOON_EVENTS else SunMoonPrefs.SUN_EVENTS
    fun label(e: String) = when (e) {
        "new", "full" -> L("nature.phases.$e"); "quarter" -> L("moon.ev.quarters"); "super" -> L("moon.ev.super"); "eclipse" -> L("moon.ev.eclipse")
        "sunrise" -> L("nature.sunrise"); "sunset" -> L("nature.sunset"); "goldenAm" -> L("nature.goldenAm"); else -> L("nature.goldenPm")
    }
    @Composable
    fun chip(text: String, on: Boolean, onClick: () -> Unit) {
        Text(text, Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(if (on) Theme.c.accent else Theme.c.surface2).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp), color = if (on) Color.White else Theme.c.text, fontSize = 13.sp)
    }
    Column(Modifier.fillMaxWidth().padding(start = 50.dp, end = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (e in all) chip(label(e), e in events) {
                events = if (e in events) events - e else events + e
                if (moon) SunMoonPrefs.moonEvents = events else SunMoonPrefs.sunEvents = events
                changed()
            }
        }
        Text(L("notify.lead"), color = Theme.c.muted, fontSize = 12.sp)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in if (moon) SunMoonPrefs.MOON_LEADS else SunMoonPrefs.SUN_LEADS) chip(SunMoonPrefs.leadLabel(m), m == lead) {
                lead = m
                if (moon) SunMoonPrefs.moonLead = m else SunMoonPrefs.sunLead = m
                changed()
            }
        }
        if (!moon) {
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (d in 1..7) chip(L("tools.alarm.dayNames.${d - 1}"), d in days) {
                    days = if (d in days) days - d else days + d
                    SunMoonPrefs.sunDays = days
                    changed()
                }
            }
        }
    }
}

