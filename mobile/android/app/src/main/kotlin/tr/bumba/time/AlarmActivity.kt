package tr.bumba.time

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Çalan alarm ekranı: kilit ekranının üstünde açılır, ekranı yakar. Kapat (bildirim ve ses kapanır) ya da
 * alarmın erteleme süresi kadar ertele (yalnız alarmlarda, süre 0 değilse). Ses bildirim kanalından gelir.
 * Alarm kapatılınca "günaydın" kartı (iOS'taki MorningCard): selam, tarih, bugünün havası, sıradaki vakit, özel günler.
 */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: run { finish(); return }
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: L("tools.alarm.defaultLabel")
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: "alarm"
        // Çalarken geri tuşu ekranı kapatmaz (rakip analizi #21): alarm yalnız kaydırarak, görevle ya da ertelemeyle susar
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
        setContent {
            AppTheme {
                var morning by remember { mutableStateOf(false) }
                if (morning) {
                    MorningCard { finish() }
                    return@AppTheme
                }
                val now = rememberNow()
                Column(
                    Modifier.fillMaxSize().background(Theme.c.bg).verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Alarm, null, tint = Theme.c.accent, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Ltr {
                        Text(Fmt.time(now.atZone(ZoneId.systemDefault())), color = Theme.c.ink, fontSize = 72.sp,
                            fontWeight = FontWeight.Bold, style = Tnum)
                    }
                    Text(title, color = Theme.c.text, fontSize = 24.sp, textAlign = TextAlign.Center)
                    Text(L("tools.alarm.ringing"), color = Theme.c.muted, fontSize = 15.sp)
                    Spacer(Modifier.height(40.dp))
                    // Kapatma görevi (rakip analizi #13): görev yapılınca alarm susar
                    val item = remember { if (kind == "alarm") AlarmItem.find(id) else null }
                    if (item != null && item.effectiveTask != "none") {
                        WakeTaskPanel(item) {
                            BadgeStore.hit(Badge.UYANIK)
                            AlarmScheduler.stop(this@AlarmActivity, id)
                            morning = true
                        }
                    } else {
                        // Kaydırarak durdur (rakip analizi #18): yanlışlıkla dokunuşla kapanmaz
                        SwipeToStop(L("alarm.swipe")) {
                            AlarmScheduler.stop(this@AlarmActivity, id)
                            if (kind == "alarm") morning = true else finish()
                        }
                    }
                    // Erteleme kapalıysa ya da sınır dolduysa düğme yok
                    val snooze = AlarmScheduler.snoozeMinutes(id, kind)
                    if (snooze > 0) {
                        Spacer(Modifier.height(12.dp))
                        RoundButton(T("alarm.snoozeFor", snooze), false, Modifier.fillMaxWidth()) {
                            AlarmScheduler.snooze(this@AlarmActivity, id, title)
                            finish()
                        }
                    }
                }
            }
        }
    }
}

/** Sağa kaydırınca tetiklenen düğme (alarmı durdurmak için; yanlışlıkla dokunuşla kapanmasın) */
@Composable
fun SwipeToStop(label: String, onStop: () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    var offset by remember { mutableStateOf(0f) }
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxWidth().height(64.dp).background(Theme.c.surface2, androidx.compose.foundation.shape.RoundedCornerShape(50)),
    ) {
        val knob = 56.dp
        val max = with(density) { (maxWidth - knob - 8.dp).toPx() }
        Text(label, Modifier.align(Alignment.Center), color = Theme.c.muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Box(
            Modifier.padding(4.dp).offset { androidx.compose.ui.unit.IntOffset(offset.toInt(), 0) }.size(knob)
                .background(Theme.c.accent, androidx.compose.foundation.shape.CircleShape)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { if (offset >= max * 0.85f) onStop() else offset = 0f },
                        onDragCancel = { offset = 0f },
                    ) { _, dx -> offset = (offset + dx).coerceIn(0f, max) }
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Alarm, L("tools.alarm.stop"), tint = Theme.c.bg) }
    }
}

/** Selam (saate göre) */
fun greeting(hour: Int): String = L(
    when (hour) {
        in 5..10 -> "morning.good"
        in 11..16 -> "morning.day"
        in 17..21 -> "morning.evening"
        else -> "morning.night"
    },
)

/** Günaydın kartı: hava Open-Meteo'dan (atıf altta), vakit ve özel günler telefonda hesaplanır */
@Composable
fun MorningCard(onDone: () -> Unit) {
    val place = AppPlace.current()
    val now = remember { Instant.now() }
    val today = place.today(now)
    var weather by remember { mutableStateOf(WeatherCache.cached(place.lat, place.lon)) }
    LaunchedEffect(place.id) { weather = runCatching { WeatherCache.weather(place.lat, place.lon) }.getOrNull() ?: weather }
    val events = AppData.events[LocalDate.now()].orEmpty().map { it.name }.distinct()
    Column(
        Modifier.fillMaxSize().background(Theme.c.bg).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(greeting(now.atZone(ZoneId.systemDefault()).hour), color = Theme.c.ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(Fmt.longDate(LocalDate.now()), color = Theme.c.muted, fontSize = 16.sp)
        weather?.daily?.firstOrNull { it.date == today }?.let { day ->
            val code = WeatherCode.of(day.code)
            AppCard {
                Text(L("weather.h1"), color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(code.icon(), fontSize = 36.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(code.label, color = Theme.c.text, fontSize = 17.sp)
                        Text("${Units.temp(day.max)} / ${Units.temp(day.min)}", color = Theme.c.muted, fontSize = 15.sp)
                    }
                }
                WeatherAttribution(Modifier.fillMaxWidth().padding(top = 6.dp))
            }
        }
        place.next(now)?.let { next ->
            AppCard {
                Text(L("home.item.next"), color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${next.prayer.title} ${Fmt.hm(next.at, place.zone)}", color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (events.isNotEmpty()) {
            AppCard {
                Text(L("morning.today"), color = Theme.c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                for (e in events) Text(e, color = Theme.c.text, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        RoundButton(L("common.done"), true, Modifier.fillMaxWidth(), onDone)
    }
}
