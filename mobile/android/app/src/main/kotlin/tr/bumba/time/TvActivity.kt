package tr.bumba.time

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Android TV (rakip analizi listesi #95; Apple TV uygulamasıyla aynı içerik): televizyonda tam ekran saat, tarih (miladi +
 * hicri), sıradaki vakit ve kalan süre, bugünün vakitleri. Uygulamadaki konumu kullanır, kumandayla değiştirilir.
 * Açıkken ekran kararmaz. Leanback başlatıcısında görünür (manifest: LEANBACK_LAUNCHER, banner).
 */
class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { AppTheme { TvClock() } }
    }
}

@androidx.compose.runtime.Composable
fun TvClock() {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1000L - now.toEpochMilli() % 1000)
        }
    }
    val place = AppPlace.current()
    val zone = place.zone
    val day = place.today(now)
    val next = place.next(now)
    val orange = Color(0xFFF2A341)
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Ltr { Text(Fmt.time(now, zone, seconds = true), color = Color.White, fontSize = 128.sp, fontWeight = FontWeight.Light, style = Tnum) }
        Text(Fmt.longDate(day), color = Color.White, fontSize = 34.sp)
        Hijri.date(day)?.let { Text(it.text, color = orange, fontSize = 26.sp) }
        if (next != null) {
            Ltr {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(next.prayer.title + " " + Fmt.hm(next.at, zone), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                    Text(clock(maxOf(0, next.at.epochSecond - now.epochSecond)), color = orange, fontSize = 34.sp, fontWeight = FontWeight.Bold, style = Tnum)
                }
            }
        }
        Ltr {
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                for (e in place.entries(day)) {
                    val hot = e.at == next?.at
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(e.prayer.title, color = if (hot) orange else Color(0xFFBBBBBB), fontSize = 22.sp)
                        Text(Fmt.hm(e.at, zone), color = if (hot) orange else Color.White, fontSize = 30.sp, style = Tnum)
                    }
                }
            }
        }
        PlacePicker(size = 22.sp)
    }
}
