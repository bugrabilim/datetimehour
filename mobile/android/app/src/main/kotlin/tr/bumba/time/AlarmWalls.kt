package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.ZoneId

/**
 * Alarm ekranı duvar kâğıdı (3 Kasım 2026, 2. analiz #2; sahibin ekran görüntüleri): alarm çalarken ekranın zemini.
 * Görsel indirilmez ve telif yoktur: sade olanlar siyah ve atmosfer renk geçişlerinin koyu hâli, hareketliler tam ekran
 * saatteki hareketli arka planlar (yıldızlar, renk akışı, gökyüzü, hava). Alarm alanı `wall` (boş = tema rengi);
 * iOS'taki AlarmWalls ile aynı adlar. Zemin seçiliyken yazılar koyu temanın açık renkleriyle çizilir.
 */
object AlarmWalls {
    val SIMPLE: List<String> = listOf("black") + Atmosphere.ALL.map { it.id }
    val ANIMATED = listOf("stars", "flow", "sky", "weather")
    val CATEGORIES = listOf("simple", "animated")

    fun title(id: String): String = when {
        id.isEmpty() -> L("wall.none")
        id == "black" -> L("wall.black")
        Atmosphere.find(id) != null -> L("atmo.$id")
        else -> L("look.bg.$id")
    }
}

/** Duvar kâğıdının kendisi (çalma ekranında, önizlemede ve seçicideki kartlarda) */
@Composable
fun AlarmWall(id: String, modifier: Modifier = Modifier) {
    val atmo = Atmosphere.find(id)
    when {
        id == "black" -> Box(modifier.background(Color.Black))
        atmo != null -> Box(modifier.background(Brush.verticalGradient(listOf(atmo.top(true), atmo.bottom(true)))))
        id in AlarmWalls.ANIMATED -> AnimatedBackground(FullscreenBackground.of(id, false), modifier)
        else -> Box(modifier.background(Theme.c.bg))
    }
}

/** Duvar kâğıdı açık mı: açıkken ekranlar kendi zeminini çizmez ([wallBackground]) */
val LocalWallActive = staticCompositionLocalOf { false }

/**
 * Duvar kâğıdı seçiliyse zemini çizer ve içeriği koyu temanın renkleriyle gösterir. Renklerdeki `bg` koyu kalır
 * (vurgulu düğmelerin yazısı o renkle çizilir); ekranlar zemini [wallBackground] ile boyar.
 */
@Composable
fun AlarmWallScreen(id: String, content: @Composable () -> Unit) {
    if (id.isEmpty()) { content(); return }
    Box(Modifier.fillMaxSize()) {
        AlarmWall(id, Modifier.fillMaxSize())
        CompositionLocalProvider(LocalColors provides AccentPalette.find(Prefs.accent).colors(true), LocalWallActive provides true) { content() }
    }
}

/** Ekran zemini: duvar kâğıdı açıksa saydam, değilse temanın zemini */
@Composable
fun Modifier.wallBackground(): Modifier = if (LocalWallActive.current) this else background(Theme.c.bg)

/** Seçici: kategori, 9:16 kartlar; karta dokununca tam ekran önizleme ("07:00") ve "Seç" */
@Composable
fun AlarmWallDialog(current: String, hour: Int, minute: Int, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var category by remember { mutableStateOf(if (current in AlarmWalls.ANIMATED) "animated" else "simple") }
    var preview by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(L("common.done"), color = Theme.c.accent) } },
        dismissButton = { TextButton(onClick = { onPick("") }) { Text(L("wall.none"), color = Theme.c.muted) } },
        title = { Text(L("wall.title")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AlarmWalls.CATEGORIES.forEachIndexed { i, c ->
                        SegmentedButton(category == c, { category = c }, SegmentedButtonDefaults.itemShape(i, AlarmWalls.CATEGORIES.size), colors = segColors()) {
                            Text(L("wall.cat.$c"), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                val ids = if (category == "simple") AlarmWalls.SIMPLE else AlarmWalls.ANIMATED
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 420.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ids) { id ->
                        val on = id == current
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp))
                                    .border(if (on) 2.dp else 1.dp, if (on) Color.White else Theme.c.border, RoundedCornerShape(12.dp))
                                    .clickable { preview = id },
                                contentAlignment = Alignment.Center,
                            ) {
                                AlarmWall(id, Modifier.fillMaxSize())
                                Text(String.format("%02d:%02d", hour, minute), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                if (on) Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp))
                            }
                            Text(AlarmWalls.title(id), color = Theme.c.text, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        },
    )
    preview?.let { id ->
        Dialog(onDismissRequest = { preview = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize()) {
                AlarmWall(id, Modifier.fillMaxSize())
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { preview = null }) { Icon(Icons.Filled.Close, null, tint = Color.White) }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(Fmt.dayMonth(LocalDate.now(ZoneId.systemDefault())), color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp)
                    Ltr { Text(String.format("%02d:%02d", hour, minute), color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.weight(1.4f))
                    RoundButton(L("wall.choose"), true, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { onPick(id); preview = null }
                }
            }
        }
    }
}
