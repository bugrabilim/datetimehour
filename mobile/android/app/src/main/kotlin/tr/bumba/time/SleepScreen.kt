package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.bumba.time.core.Sleep
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/*
 * Uyku (30 Ekim 2026, rakip analizi #17; iOS'taki SleepView): uyku planı ve yatma hatırlatması, 90 dakikalık döngülere
 * göre saat önerileri, uyku sesleri (telefonda üretilen gürültü, kapanma süresiyle) ve gün doğumu ışığı (alarm ayarı;
 * tam ekran saatte). Uyku takibi bilerek yok. Kayıtlar yalnız telefonda (sleep.*; iOS'takiyle aynı anahtarlar).
 */

object SleepPlan {
    const val ON = "sleep.on"
    const val WAKE = "sleep.wake"
    const val DURATION = "sleep.dur"
    const val DAYS = "sleep.days"
    const val REMIND = "sleep.remind"
    val REMIND_CHOICES = listOf(0, 15, 30, 60)

    private val sp get() = Prefs.store
    var on: Boolean
        get() = sp.getBoolean(ON, false)
        set(v) = sp.edit().putBoolean(ON, v).apply()
    /** Kalkış (gece yarısından dakika; varsayılan 07.00) */
    var wake: Int
        get() = sp.getInt(WAKE, 420)
        set(v) = sp.edit().putInt(WAKE, v).apply()
    /** Uyku süresi (dakika; varsayılan 8 saat) */
    var duration: Int
        get() = sp.getInt(DURATION, 480)
        set(v) = sp.edit().putInt(DURATION, v).apply()
    /** Kalkış günleri (ISO 1 pazartesi … 7 pazar; varsayılan her gün) */
    var days: List<Int>
        get() = parse(sp.getString(DAYS, null))
        set(v) = sp.edit().putString(DAYS, v.sorted().joinToString(",")).apply()
    /** Hatırlatma yatma saatinden kaç dakika önce (varsayılan 30) */
    var remind: Int
        get() = sp.getInt(REMIND, 30)
        set(v) = sp.edit().putInt(REMIND, v).apply()

    fun parse(raw: String?) = (raw ?: "1,2,3,4,5,6,7").split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }

    /** Günün dakikası → "23:00" (telefonun saat biçimiyle) */
    fun hm(minute: Int): String {
        val m = Sleep.mod(minute)
        return Fmt.time(LocalDate.now().atTime(LocalTime.of(m / 60, m % 60)).atZone(ZoneId.systemDefault()))
    }

    /** "8 sa 30 dk" */
    fun durationText(minutes: Int) = T("sleep.durationFmt", minutes / 60, minutes % 60)

    /** Döngü süresi saat olarak: "7,5" (Arapçada da Latin rakamı) */
    fun hours(minutes: Int): String {
        val loc = Strings.numberLocale
        return NumberFormat.getNumberInstance(loc).apply { maximumFractionDigits = 1 }.format(minutes / 60.0)
    }

    /** Yatma hatırlatmaları: kalkış günü D için yatma = D'nin kalkış saati − uyku süresi, hatırlatma ondan `remind` dk önce */
    fun reminders(now: Instant, count: Int, zone: ZoneId = ZoneId.systemDefault()): List<FeatureNotifier.Planned> {
        val days = days
        if (!on || days.isEmpty()) return emptyList()
        val today = now.atZone(zone).toLocalDate()
        val wake = wake; val duration = duration; val remind = remind
        return (0..count).mapNotNull { i ->
            val day = today.plusDays(i.toLong())
            if (day.dayOfWeek.value !in days) return@mapNotNull null
            val wakeAt = day.atTime(wake / 60, wake % 60).atZone(zone).toInstant()
            val fire = wakeAt.minusSeconds(duration * 60L + remind * 60L)
            if (!fire.isAfter(now)) return@mapNotNull null
            FeatureNotifier.Planned("bed-$day", fire, L(if (remind == 0) "sleep.n.now" else "sleep.n.soon"),
                T("sleep.n.body", hm(wake - duration), hm(wake), durationText(duration)), Tab.TOOLS)
        }
    }
}

/** Uyku sesleri: tür, ses düzeyi, kapanma süresi (sleep.noise, sleep.vol, sleep.timer) */
object SleepSounds {
    val TIMER_CHOICES = listOf(15, 30, 45, 60, 90, 0)
    var until = 0L
        private set

    val playing: Boolean get() = SoundService.source == "sleep"

    fun play(c: android.content.Context) {
        val sp = Prefs.store
        val minutes = sp.getInt("sleep.timer", 30)
        until = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else 0L
        SoundService.play(c, sp.getString("sleep.noise", "rain") ?: "rain", sp.getInt("sleep.vol", 35), until, "sleep", fade = true)
    }

    fun stop(c: android.content.Context) = SoundService.stop(c, "sleep")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen() {
    val context = LocalContext.current
    val now = rememberNow()
    var on by remember { mutableStateOf(SleepPlan.on) }
    var wake by remember { mutableIntStateOf(SleepPlan.wake) }
    var duration by remember { mutableIntStateOf(SleepPlan.duration) }
    var days by remember { mutableStateOf(SleepPlan.days) }
    var remind by remember { mutableIntStateOf(SleepPlan.remind) }
    var notify by remember { mutableStateOf(FeaturePrefs.isOn(NotifyFeature.BEDTIME)) }
    var noise by remember { mutableStateOf(Prefs.store.getString("sleep.noise", "rain") ?: "rain") }
    var volume by remember { mutableIntStateOf(Prefs.store.getInt("sleep.vol", 35)) }
    var timer by remember { mutableIntStateOf(Prefs.store.getInt("sleep.timer", 30)) }
    var picking by remember { mutableStateOf(false) }
    var alarmAdded by remember { mutableStateOf(false) }
    val ask = rememberNotifyAsk()
    fun changed() = FeatureNotifier.reschedule(context)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle(L("sleep.plan"))
        AppCard {
            SwitchRow(L("sleep.plan.use"), on) { on = it; SleepPlan.on = it; if (it) ask(); changed() }
            if (on) {
                Row(Modifier.fillMaxWidth().clickable { picking = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(L("sleep.wake"), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    Text(SleepPlan.hm(wake), color = Theme.c.accent, fontSize = 15.sp, style = Tnum)
                }
                ChoiceRow(L("sleep.duration"), SleepPlan.durationText(duration),
                    (300..660 step 30).map { it to SleepPlan.durationText(it) }) {
                    duration = it; SleepPlan.duration = it; changed()
                }
                Ltr {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (d in 1..7) {
                            val sel = d in days
                            Box(
                                Modifier.weight(1f).height(36.dp).clip(CircleShape)
                                    .background(if (sel) Theme.c.accent else Theme.c.surface2)
                                    .clickable { days = if (sel) days - d else (days + d).sorted(); SleepPlan.days = days; changed() }
                                    .semantics { selected = sel },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(L("tools.alarm.dayNames.${d - 1}"), color = if (sel) Color.White else Theme.c.text,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(L("sleep.bedtime"), Modifier.weight(1f), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(SleepPlan.hm(wake - duration), color = Theme.c.accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, style = Tnum)
                }
                SwitchRow(L("sleep.remind"), notify) {
                    notify = it; FeaturePrefs.set(NotifyFeature.BEDTIME, it); if (it) ask(); changed()
                }
                if (notify) {
                    fun remindText(m: Int) = if (m == 0) L("sleep.remindAt") else T("sleep.remindBefore", m)
                    ChoiceRow(L("sleep.remindWhen"), remindText(remind), SleepPlan.REMIND_CHOICES.map { it to remindText(it) }) {
                        remind = it; SleepPlan.remind = it; changed()
                    }
                }
                TextButton(onClick = {
                    AlarmItem.addNew(context, AlarmItem(hour = wake / 60, minute = wake % 60, label = L("sleep.alarmLabel"), days = days))
                    alarmAdded = true
                }) { Text(L("sleep.addAlarm"), color = Theme.c.accent) }
                if (alarmAdded) Text(L("sleep.alarmAdded"), color = Theme.c.muted, fontSize = 13.sp)
            }
            Text(L("sleep.plan.note"), color = Theme.c.muted, fontSize = 12.sp)
        }

        SectionTitle(L("sleep.cycles"))
        AppCard {
            val t = now.atZone(ZoneId.systemDefault())
            Text(L("sleep.ifNow"), color = Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            for ((n, m) in Sleep.wakeTimes(t.hour * 60 + t.minute)) CycleRow(SleepPlan.hm(m), n)
            Spacer(Modifier.height(4.dp))
            Text(T("sleep.forWake", SleepPlan.hm(wake)), color = Theme.c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            for ((n, m) in Sleep.bedtimes(wake)) CycleRow(SleepPlan.hm(m), n)
            Text(L("sleep.cycles.note"), color = Theme.c.muted, fontSize = 12.sp)
        }

        SectionTitle(L("sleep.sounds"))
        AppCard {
            NoiseControls(noise, volume, allowOff = false,
                onKind = {
                    noise = it; Prefs.store.edit().putString("sleep.noise", it).apply()
                    if (SleepSounds.playing) SleepSounds.play(context)
                },
                onVolume = { volume = it; Prefs.store.edit().putInt("sleep.vol", it).apply() })
            fun timerText(m: Int) = if (m == 0) L("sleep.timerOff") else T("alarm.snoozeMin", m)
            ChoiceRow(L("sleep.timer"), timerText(timer), SleepSounds.TIMER_CHOICES.map { it to timerText(it) }) {
                timer = it; Prefs.store.edit().putInt("sleep.timer", it).apply()
            }
            // Çalarken: kalan süre (saniyede bir yenilenen "şimdi" ile)
            val playing = SleepSounds.playing
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton(L(if (playing) "sleep.stop" else "sleep.play"), true, Modifier.weight(1f)) {
                    if (playing) SleepSounds.stop(context) else SleepSounds.play(context)
                }
                if (playing && SleepSounds.until > 0) {
                    Text("  " + clock((SleepSounds.until - now.toEpochMilli() + 999).coerceAtLeast(0) / 1000), color = Theme.c.muted, style = Tnum)
                }
            }
            Text(L("sleep.sounds.note"), color = Theme.c.muted, fontSize = 12.sp)
        }

        SectionTitle(L("alarm.sunrise"))
        AppCard { Text(L("sleep.sunriseInfo"), color = Theme.c.text, fontSize = 14.sp) }
        Text(L("sleep.privacy"), color = Theme.c.muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
    }

    if (picking) {
        val state = rememberTimePickerState(wake / 60, wake % 60, is24Hour = !Prefs.h12)
        AlertDialog(
            onDismissRequest = { picking = false },
            text = {
                Ltr {
                    TimePicker(state, colors = TimePickerDefaults.colors(selectorColor = Theme.c.accent,
                        timeSelectorSelectedContainerColor = Theme.c.accentSoft, timeSelectorSelectedContentColor = Theme.c.accent))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    wake = state.hour * 60 + state.minute; SleepPlan.wake = wake; picking = false; changed()
                }) { Text(L("alarm.save"), color = Theme.c.accent) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(L("alarm.cancel"), color = Theme.c.muted) } },
        )
    }
}

@Composable
private fun SwitchRow(title: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
    }
}

@Composable
private fun CycleRow(time: String, cycles: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(time, Modifier.weight(1f), color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, style = Tnum)
        Text(T("sleep.cycleRow", cycles, SleepPlan.hours(cycles * Sleep.CYCLE)), color = Theme.c.muted, fontSize = 14.sp)
    }
}
