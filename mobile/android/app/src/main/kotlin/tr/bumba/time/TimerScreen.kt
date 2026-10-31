package tr.bumba.time

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Zamanlayıcılar (birden çok, adlı), aralık (HIIT) antrenmanı ve tarihe geri sayım (iOS'taki TimerView) */
@Composable
fun TimerScreen() {
    // Aralık antrenmanı sürüyorsa (bildirimden açılınca da) o sekme açılır
    var mode by rememberSaveable { mutableIntStateOf(if (ToolState.long("hiit.start") > 0) 1 else 0) }
    Column(Modifier.fillMaxSize()) {
        val tabs = listOf(L("tools.countdown.tabTimer"), L("hiit.tab"), L("tools.countdown.tabDate"))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
            tabs.forEachIndexed { i, t ->
                SegmentedButton(mode == i, { mode = i }, SegmentedButtonDefaults.itemShape(i, tabs.size), colors = segColors()) { Text(t, maxLines = 1) }
            }
        }
        when (mode) {
            0 -> TimersScreen()
            1 -> HiitScreen()
            else -> CountdownsScreen()
        }
    }
}

/** Kalıcı küçük durum (uygulama kapansa da sürer): iOS'taki tm.* / cd.* / pomo.* anahtarları */
internal object ToolState {
    fun long(k: String) = Prefs.store.getLong(k, 0L)
    fun putLong(k: String, v: Long) = Prefs.store.edit().putLong(k, v).apply()
    fun str(k: String, d: String = "") = Prefs.store.getString(k, d) ?: d
    fun putStr(k: String, v: String) = Prefs.store.edit().putString(k, v).apply()
    fun int(k: String, d: Int) = Prefs.store.getInt(k, d)
    fun putInt(k: String, v: Int) = Prefs.store.edit().putInt(k, v).apply()
}

/** Saniyelik süre: "1:02:03" ya da "02:03" */
internal fun clock(seconds: Long): String = Fmt.duration(seconds)

/** Bildirim izni (Android 13+): süre başlatılırken bir kez istenir */
@Composable
internal fun rememberNotifyAsk(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (Build.VERSION.SDK_INT >= 33 && !AlarmScheduler.canNotify(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** Basit sayı çarkı: yukarı/aşağı oklarla (basılı tutmadan) */
@Composable
internal fun NumberWheel(value: Int, range: IntRange, unit: String, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onChange(if (value >= range.last) range.first else value + 1) }) {
            Icon(Icons.Filled.KeyboardArrowUp, null, tint = Theme.c.accent)
        }
        Text("%02d".format(value), color = Theme.c.text, fontSize = 44.sp, fontWeight = FontWeight.SemiBold, style = Tnum)
        Text(unit, color = Theme.c.muted, fontSize = 13.sp)
        IconButton(onClick = { onChange(if (value <= range.first) range.last else value - 1) }) {
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Theme.c.accent)
        }
    }
}

/** Yeni yıl, Ramazan, bayramlar, 23 Nisan, 19 Mayıs, 30 Ağustos, 29 Ekim: her birinin sıradaki tarihi (iOS ile aynı) */
internal fun quickTargets(today: LocalDate): List<Pair<String, LocalDate>> {
    val keys = listOf("newYear", "ramadanStart", "rb1", "kb1", "apr23", "may19", "aug30", "oct29")
    val all = AppData.events.entries.flatMap { (d, list) -> list.filter { it.date == d } }.sortedBy { it.date }
    return keys.mapNotNull { k ->
        all.firstOrNull { it.key == k && it.date.isAfter(today) }?.let { e ->
            (if (k == "newYear") L("tools.countdown.quickNewYear") else e.name) to e.date
        }
    }.sortedBy { it.second }
}

/** Bölmeli düğmelerde seçili olan uygulama renginde */
@Composable
internal fun segColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = Theme.c.accentSoft, activeContentColor = Theme.c.accent, activeBorderColor = Theme.c.border,
    inactiveContainerColor = Theme.c.surface, inactiveContentColor = Theme.c.text, inactiveBorderColor = Theme.c.border,
)

/** Büyük süre göstergesi */
@Composable
internal fun BigTime(text: String) {
    Ltr {
        Text(text, Modifier.fillMaxWidth(), color = Theme.c.text, fontSize = if (text.length > 9) 44.sp else 60.sp,
            fontWeight = FontWeight.SemiBold, style = Tnum, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
    }
}
