package tr.bumba.time

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Alarm ses kitaplığı (3 Kasım 2026, 2. analiz #3; sahibin ekran görüntüleri): uygulamayla gelen, telifsiz (sentez) 10
 * ses, üç kategoride. Dosyalar res/raw/tone_<ad>.ogg (üretici mobile/shared/sounds/gen-tones.mjs). Seçilen ses alarmın
 * `sound` alanına android.resource adresi olarak yazılır: bildirim kanalı, kademeli ses (AlarmRingService) ve
 * titreşim olduğu gibi çalışır. iOS'taki AlarmTones.swift ile aynı adlar ve sıra.
 */
object AlarmTones {
    data class Tone(val id: String, val category: String)

    val CATEGORIES = listOf("soft", "classic", "loud")
    val ALL = listOf(
        Tone("chime", "soft"), Tone("marimba", "soft"), Tone("harp", "soft"), Tone("birds", "soft"), Tone("kalimba", "soft"),
        Tone("bell", "classic"), Tone("digital", "classic"), Tone("buzzer", "classic"),
        Tone("siren", "loud"), Tone("pulse", "loud"),
    )

    fun uri(c: Context, id: String): String = "android.resource://${c.packageName}/raw/tone_$id"

    /** Alarmın sesi kitaplıktansa adı (yoksa null) */
    fun id(sound: String): String? = Regex("/raw/tone_([a-z]+)$").find(sound)?.groupValues?.get(1)?.takeIf { s -> ALL.any { it.id == s } }
}

/** Kitaplık penceresi: kategori seçici, ▶ ile dinleme, satıra dokununca seçim */
@Composable
fun AlarmToneDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var category by remember { mutableStateOf(AlarmTones.id(current)?.let { id -> AlarmTones.ALL.first { it.id == id }.category } ?: "soft") }
    var playing by remember { mutableStateOf<String?>(null) }
    val player = remember { arrayOfNulls<MediaPlayer>(1) }
    fun stop() { player[0]?.runCatching { stop(); release() }; player[0] = null; playing = null }
    DisposableEffect(Unit) { onDispose { stop() } }
    fun toggle(id: String) {
        val was = playing
        stop()
        if (was == id) return
        player[0] = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                setDataSource(context, Uri.parse(AlarmTones.uri(context, id)))
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
        }.getOrNull()
        if (player[0] != null) playing = id
    }
    val selected = AlarmTones.id(current)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(L("common.done"), color = Theme.c.accent) } },
        title = { Text(L("tone.library")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AlarmTones.CATEGORIES.forEachIndexed { i, c ->
                        SegmentedButton(category == c, { category = c }, SegmentedButtonDefaults.itemShape(i, AlarmTones.CATEGORIES.size), colors = segColors()) {
                            Text(L("tone.cat.$c"), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(AlarmTones.ALL.filter { it.category == category }) { t ->
                        Row(Modifier.fillMaxWidth().clickable { stop(); onPick(AlarmTones.uri(context, t.id)) }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { toggle(t.id) }) {
                                Icon(if (playing == t.id) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
                                    L(if (playing == t.id) "tone.stop" else "tone.play"),
                                    tint = if (playing == t.id) Color(0xFF14B8A6) else Theme.c.accent, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(L("tone.${t.id}"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
                            if (selected == t.id) Icon(Icons.Filled.Check, null, tint = Theme.c.accent)
                        }
                    }
                }
                Text(L("tone.note"), color = Theme.c.muted, fontSize = 12.sp)
            }
        },
    )
}
