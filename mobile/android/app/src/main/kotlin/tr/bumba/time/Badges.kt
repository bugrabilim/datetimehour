package tr.bumba.time

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.LocalDate

/**
 * Kazanım rozetleri (Bumba standardı madde 13; Saat Tarih'te yalnız uygulamalarda, 30 Ekim 2026). Kimlikler ve
 * kurallar iOS'taki Badge/BadgeStore ile aynı; adlar `badges.items.<kimlik>.*` metinleri (app-strings.json).
 * İnanç ele vermemek için namaz/imsakiye rozeti yoktur.
 */
enum class Badge(val id: String, val icon: ImageVector) {
    RENK("renk", Icons.Filled.Palette),
    SANIYE("saniye", Icons.Filled.TouchApp),
    ILK_MODEL("ilk-model", Icons.Filled.Autorenew),
    TAM_EKRAN("tam-ekran", Icons.Filled.Fullscreen),
    DUZENLE("duzenle", Icons.Filled.Tune),
    TAKVIM("takvim", Icons.Filled.CalendarMonth),
    KRONOMETRE("kronometre", Icons.Filled.Timer),
    ZAMANLAYICI("zamanlayici", Icons.Filled.HourglassBottom),
    ALARM("alarm", Icons.Filled.Alarm),
    CEVIRICI("cevirici", Icons.Filled.SwapHoriz),
    TARIH_HESAP("tarih-hesap", Icons.Filled.Calculate),
    DUNYA("dunya", Icons.Filled.Public),
    KURE("kure", Icons.Filled.TravelExplore),
    POMODORO("pomodoro", Icons.Filled.Spa),
    SERI3("seri-3", Icons.Filled.LocalFireDepartment),
    MODEL10("model-10", Icons.Filled.GridView),
    SERI7("seri-7", Icons.Filled.EmojiEvents),
    MODEL_HEPSI("model-hepsi", Icons.Filled.WorkspacePremium);

    val title: String get() = L("badges.items.$id.name")
    val how: String get() = L("badges.items.$id.how")
}

/**
 * Rozetler yalnız bu cihazda, "prefs" içinde tek anahtarda ("badges", JSON) tutulur: kazanılan rozetin kimliği ve
 * kazanma günü (yerel tarih, saat yok) ile koşulu hesaplamaya yeten sayaçlar (m: görülen modeller bit maskesi,
 * g/n: son açılış günü ve üst üste gün sayısı). Eylem günlüğü yok; hiçbir yere gönderilmez.
 */
object BadgeStore {
    const val MODEL_GOAL = 10
    private const val KEY = "badges"

    data class Saved(val r: Map<String, String> = emptyMap(), val m: Long = 0, val g: String = "", val n: Int = 0)

    var saved by mutableStateOf(Saved())
        private set
    /** Ekrandaki bildirim (akışı bölmez, birkaç saniyede kaybolur); ilk rozette not bir kez gösterilir */
    var toast by mutableStateOf<Badge?>(null)
        private set
    var toastNote by mutableStateOf(false)
        private set
    private val queue = ArrayDeque<Pair<Badge, Boolean>>()
    private var showing = false
    private val main by lazy { Handler(Looper.getMainLooper()) }

    /** Prefs.init'ten çağrılır */
    fun load() {
        saved = runCatching {
            val j = JSONObject(Prefs.store.getString(KEY, null) ?: return)
            val r = j.optJSONObject("r")
            Saved(
                r = r?.keys()?.asSequence()?.associateWith { r.getString(it) } ?: emptyMap(),
                m = j.optLong("m"), g = j.optString("g"), n = j.optInt("n"),
            )
        }.getOrDefault(Saved())
    }

    private fun persist() {
        val j = JSONObject().put("r", JSONObject(saved.r)).put("m", saved.m).put("g", saved.g).put("n", saved.n)
        Prefs.store.edit().putString(KEY, j.toString()).apply()
    }

    fun earned(b: Badge): String? = saved.r[b.id]
    val earnedCount: Int get() = Badge.entries.count { saved.r[it.id] != null }
    val modelsSeen: Int get() = java.lang.Long.bitCount(saved.m)

    fun hit(b: Badge) {
        if (saved.r[b.id] != null) return
        val first = saved.r.isEmpty()
        saved = saved.copy(r = saved.r + (b.id to LocalDate.now().toString()))
        persist()
        queue.addLast(b to first)
        if (!showing) showNext()
    }

    private fun showNext() {
        val next = queue.removeFirstOrNull()
        if (next == null) {
            showing = false
            return
        }
        showing = true
        toastNote = next.second
        toast = next.first
        main.postDelayed({
            toast = null
            main.postDelayed({ showNext() }, 400)
        }, if (next.second) 7000 else 4000)
    }

    /** Saat sahnesinde bir model göründü (açılıştaki model dahil) */
    fun sawModel(index: Int, total: Int) {
        if (index !in 0..61) return
        val m = saved.m or (1L shl index)
        if (m != saved.m) {
            saved = saved.copy(m = m)
            persist()
        }
        if (modelsSeen >= MODEL_GOAL) hit(Badge.MODEL10)
        if (modelsSeen >= total) hit(Badge.MODEL_HEPSI)
    }

    /** Uygulama öne geldi: üst üste gün serisi (yalnız son gün ve gün sayısı tutulur) */
    fun appOpened(today: LocalDate = LocalDate.now()) {
        val t = today.toString()
        if (saved.g == t) return
        val n = if (saved.g == today.minusDays(1).toString()) maxOf(saved.n, 1) + 1 else 1
        saved = saved.copy(g = t, n = n)
        persist()
        if (n >= 3) hit(Badge.SERI3)
        if (n >= 7) hit(Badge.SERI7)
    }

    fun deleteAll() {
        saved = Saved()
        Prefs.store.edit().remove(KEY).apply()
        queue.clear()
        toast = null
    }
}

/** Rozet kazanılınca üstte beliren kısa bildirim; dokununca Rozetler açılır */
@Composable
fun BadgeToast(modifier: Modifier = Modifier) {
    val b = BadgeStore.toast
    val view = LocalView.current
    LaunchedEffect(b) { if (b != null) view.announceForAccessibility(T("badges.toast", b.title)) }
    AnimatedVisibility(b != null, modifier, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
        val shown = b ?: return@AnimatedVisibility
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .background(Theme.c.surface, RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, Theme.c.accent.copy(alpha = 0.6f)), RoundedCornerShape(14.dp))
                .clickable { Nav.tool("badges") }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(shown.icon, null, tint = Theme.c.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(T("badges.toast", shown.title), color = Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            if (BadgeStore.toastNote) Text(L("badges.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
    }
}

/**
 * Rozetler ekranı: bütün rozetler (kazanılan ve henüz kazanılmayan, koşulu ve ilerlemesiyle), not, gizlilik
 * bağlantısı ve onaylı "Rozetleri sil"
 */
@Composable
fun BadgesScreen() {
    val context = LocalContext.current
    var confirming by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard {
            Text(L("badges.note"), color = Theme.c.text, fontSize = 14.sp)
            Text(L("badges.privacyLink"), Modifier.clickable { openUrl(context, privacyUrl()) }, color = Theme.c.accent, fontSize = 14.sp)
            Text(T("badges.count", BadgeStore.earnedCount, Badge.entries.size), color = Theme.c.text, fontWeight = FontWeight.SemiBold)
        }
        AppCard(padding = 4) {
            Badge.entries.forEachIndexed { i, b ->
                BadgeRow(b)
                if (i < Badge.entries.size - 1) HorizontalDivider(Modifier.padding(start = 56.dp), color = Theme.c.border.copy(alpha = 0.5f))
            }
        }
        TextButton(onClick = { confirming = true }) { Text(L("badges.delete"), color = DangerRed) }
        if (deleted) Text(L("badges.deleted"), color = Theme.c.muted, fontSize = 13.sp)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(L("badges.deleteTitle")) },
            text = { Text(L("badges.deleteText")) },
            confirmButton = {
                TextButton(onClick = { BadgeStore.deleteAll(); deleted = true; confirming = false }) {
                    Text(L("badges.deleteYes"), color = DangerRed)
                }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(L("badges.deleteNo")) } },
        )
    }
}

@Composable
private fun BadgeRow(b: Badge) {
    val day = BadgeStore.earned(b)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val status = badgeStatus(b, day)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = "${b.title}. ${b.how}. $status" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(b.icon, null, tint = if (day != null) Theme.c.accent else Theme.c.muted.copy(alpha = 0.5f), modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(b.title, color = Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(b.how, color = Theme.c.text, fontSize = 14.sp)
            // Durum yalnız renkle değil simge ve metinle de anlatılır
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (day != null) Icons.Filled.Verified else Icons.Filled.Lock, null,
                    tint = if (day != null) Theme.c.accent else Theme.c.muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(status, color = if (day != null) Theme.c.text else Theme.c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun badgeStatus(b: Badge, day: LocalDate?): String {
    if (day != null) return T("badges.earnedOn", Fmt.longDate(day))
    val s = BadgeStore.saved
    val progress = when (b) {
        Badge.MODEL10 -> minOf(BadgeStore.modelsSeen, BadgeStore.MODEL_GOAL) to BadgeStore.MODEL_GOAL
        Badge.MODEL_HEPSI -> BadgeStore.modelsSeen to ClockModel.count
        Badge.SERI3 -> minOf(s.n, 3) to 3
        Badge.SERI7 -> minOf(s.n, 7) to 7
        else -> null
    } ?: return L("badges.locked")
    return L("badges.locked") + " · " + T("badges.progress", progress.first, progress.second)
}

internal val DangerRed = Color(0xFFD32F2F)

private fun openUrl(c: Context, url: String) {
    runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
