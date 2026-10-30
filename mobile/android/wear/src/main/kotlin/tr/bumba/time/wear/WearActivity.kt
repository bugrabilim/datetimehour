package tr.bumba.time.wear

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executors

/** Saat tarafının renkleri (telefon uygulamasının varsayılan koyu teması) */
internal object WColors {
    val accent = Color(0xFFF59E0B)
    val text = Color(0xFFF5F1E8)
    val muted = Color(0xFFA8A29E)
    val card = Color(0xFF292524)
}

/**
 * Wear OS saati (rakip analizi #6): saat ve tarih (miladi + hicri), sıradaki vakit ve kalan süre, bugünün vakitleri,
 * yer seçimi (Konumum ya da 81 il). Hesap telefondakiyle aynı çekirdekten (:core), internet gerekmez.
 */
class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WearRoot() } }
    }
}

@Composable
fun WearRoot(initialNow: Instant? = null) {
    var picking by remember { mutableStateOf(false) }
    var version by remember { mutableLongStateOf(0L) }
    var now by remember { mutableStateOf(initialNow ?: Instant.now()) }
    if (initialNow == null) LaunchedEffect(Unit) { while (true) { now = Instant.now(); delay(1000) } }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (picking) {
            BackHandler { picking = false }
            PlacePicker { picking = false; version++ }
        } else {
            // version: yer değişince yeniden hesaplansın
            key(version) { MainScreen(now) { picking = true } }
        }
    }
}

@Composable
private fun key(v: Long, content: @Composable () -> Unit) = androidx.compose.runtime.key(v) { content() }

@Composable
private fun MainScreen(now: Instant, onPlace: () -> Unit) {
    val place = Wear.place()
    val state = rememberScalingLazyListState()
    val z = now.atZone(place.zone)
    val next = place.next(now)
    Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(state) }) {
        ScalingLazyColumn(Modifier.fillMaxWidth(), state = state, horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Text("%02d:%02d".format(z.hour, z.minute), color = WColors.text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
            item { Text(Wear.dayText(z), color = WColors.muted, fontSize = 13.sp, textAlign = TextAlign.Center) }
            Wear.hijriText(z.toLocalDate())?.let { h -> item { Text(h, color = WColors.accent, fontSize = 12.sp, textAlign = TextAlign.Center) } }
            if (next != null) item {
                Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Wear.label(next, place.zone) + " " + place.hm(next.at), color = WColors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    val s = maxOf(0L, next.at.epochSecond - now.epochSecond)
                    Text("%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60), color = WColors.accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { Text(Wear.text("wear.today"), color = WColors.muted, fontSize = 12.sp) }
            for (e in place.entries(z.toLocalDate())) item {
                val isNext = e.at == next?.at
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Wear.prayerName(e.prayer), color = if (isNext) WColors.accent else WColors.text, fontSize = 15.sp)
                    Text(place.hm(e.at), color = if (isNext) WColors.accent else WColors.text, fontSize = 15.sp,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal)
                }
            }
            item {
                Chip(
                    onClick = onPlace,
                    label = { Text(place.name, maxLines = 1) },
                    secondaryLabel = { Text(Wear.text("wear.place"), maxLines = 1) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

/** Yer seçimi: Konumum (en yakın il; izin istenir) ya da 81 il */
@Composable
private fun PlacePicker(onDone: () -> Unit) {
    val context = LocalContext.current
    val state = rememberScalingLazyListState()
    var note by remember { mutableStateOf<String?>(null) }
    fun locate() {
        val lm = context.getSystemService(LocationManager::class.java)
        val provider = if (Build.VERSION.SDK_INT >= 31 && lm.allProviders.contains(LocationManager.FUSED_PROVIDER)) LocationManager.FUSED_PROVIDER
            else LocationManager.NETWORK_PROVIDER
        Wear.select("auto")
        runCatching {
            lm.getCurrentLocation(provider, null, Executors.newSingleThreadExecutor()) { loc ->
                if (loc != null) Wear.saveLocation(loc.latitude, loc.longitude)
                ContextCompat.getMainExecutor(context).execute { onDone() }
            }
        }.onFailure { note = Wear.text("wear.noLocation"); onDone() }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) locate() else { Wear.select("auto"); onDone() }
    }
    Scaffold(positionIndicator = { PositionIndicator(state) }) {
        ScalingLazyColumn(Modifier.fillMaxWidth(), state = state) {
            item { Text(Wear.text("wear.place"), color = WColors.muted, fontSize = 13.sp) }
            item {
                Chip(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
                        else ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    label = { Text(Wear.text("widget.province.auto"), maxLines = 1) },
                    secondaryLabel = note?.let { n -> { Text(n, maxLines = 2) } },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = WColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            for (p in Wear.provinces) item {
                Chip(
                    onClick = { Wear.select(p.slug); onDone() },
                    label = { Text(p.displayName, maxLines = 1) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

