package tr.bumba.time

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Pomodoro (iOS'taki PomodoroView): çalışma, kısa mola, uzun mola; süreler ayarlanır, aşama bitince bildirim gelir
 * (uygulama kapalıyken de). Durum kalıcı: pomo.* anahtarları.
 */
@Composable
fun PomodoroScreen() {
    val context = LocalContext.current
    val ask = rememberNotifyAsk()
    var workMin by remember { mutableIntStateOf(ToolState.int("pomo.work", 25)) }
    var shortMin by remember { mutableIntStateOf(ToolState.int("pomo.short", 5)) }
    var longMin by remember { mutableIntStateOf(ToolState.int("pomo.long", 15)) }
    var rounds by remember { mutableIntStateOf(ToolState.int("pomo.rounds", 4)) }
    var phase by remember { mutableStateOf(ToolState.str("pomo.phase", "work")) }
    var round by remember { mutableIntStateOf(ToolState.int("pomo.round", 1)) }
    var end by remember { mutableLongStateOf(ToolState.long("pomo.end")) }
    var left by remember { mutableLongStateOf(ToolState.long("pomo.left")) }
    var done by remember { mutableIntStateOf(ToolState.int("pomo.done", 0)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Görev adı, istatistik ve arka plan sesi (rakip analizi #16; sitedeki pomodoroyla aynı)
    var task by remember { mutableStateOf(ToolState.str("pomo.task")) }
    var noise by remember { mutableStateOf(ToolState.str("pomo.noise", "off")) }
    var volume by remember { mutableIntStateOf(ToolState.int("pomo.vol", 40)) }
    var log by remember { mutableStateOf(PomoLog.load()) }
    // Günlük hedef ve etiket (rakip analizi #27)
    var goal by remember { mutableIntStateOf(ToolState.int("pomo.goal", 4)) }
    var tag by remember { mutableStateOf(ToolState.str("pomo.tag", "none")) }

    fun phaseMs() = (when (phase) { "work" -> workMin; "short" -> shortMin; else -> longMin }) * 60_000L
    fun persist() {
        ToolState.putStr("pomo.phase", phase); ToolState.putInt("pomo.round", round); ToolState.putLong("pomo.end", end)
        ToolState.putLong("pomo.left", left); ToolState.putInt("pomo.done", done)
        PomoNoise.sync(context)
        LiveNotifications.sync(context)
        FocusBlock.sync(context)
    }
    fun start(ms: Long) {
        ask()
        end = System.currentTimeMillis() + ms
        left = 0
        now = System.currentTimeMillis()
        persist()
        AlarmScheduler.schedule(context, AlarmScheduler.POMODORO, end,
            T("tools.pomodoro.phaseDone", L("tools.pomodoro.$phase")), AlarmScheduler.POMODORO)
    }
    /** Sonraki aşamaya geçer (duraklatılmış başlar) */
    fun advance(completed: Boolean) {
        if (!completed) AlarmScheduler.cancel(context, AlarmScheduler.POMODORO)
        if (phase == "work") {
            if (completed) {
                done += 1
                BadgeStore.hit(Badge.POMODORO) // çalışma süresi sonuna kadar tamamlandı
                // Odak geçmişi: atlanan tur sayılmaz (sitedeki gibi)
                PomoLog.record(workMin, task, if (end > 0) end else System.currentTimeMillis(), tag)
                PomoTodo.save(PomoTodo.credit(PomoTodo.load(), task))
                log = PomoLog.load()
            }
            phase = if (round >= rounds) "long" else "short"
        } else {
            if (phase == "long") round = 1 else round += 1
            phase = "work"
        }
        end = 0
        left = 0
        persist()
    }

    val running = end > 0
    val remaining = if (running) maxOf(0, end - now) else if (left > 0) left else phaseMs()
    LaunchedEffect(end) {
        while (end > 0) {
            now = System.currentTimeMillis()
            // Aşama bittiyse (uygulama kapalıyken bitmiş olabilir) sonrakine geçilir
            if (now >= end) { advance(completed = true); break }
            delay(250)
        }
    }

    // Araç kalıbı (2. analiz #200): Sayaç / İstatistik sekmeleri, altta tek ana düğme, ikincil işler "…" menüsünde,
    // görev ve ses ile süre ayarları alt sayfada
    var view by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<String?>(null) }
    val primary = L(if (running) "tools.pomodoro.pause" else if (left > 0) "tools.pomodoro.resume" else "tools.pomodoro.start")
    ToolFrame(
        primary = primary,
        onPrimary = {
            if (running) {
                AlarmScheduler.cancel(context, AlarmScheduler.POMODORO)
                left = remaining
                end = 0
                persist()
            } else start(if (left > 0) left else phaseMs())
        },
        menu = listOf(
            L("tools.pomodoro.skip") to { advance(completed = false) },
            L("tools.pomodoro.reset") to {
                AlarmScheduler.cancel(context, AlarmScheduler.POMODORO)
                end = 0; left = 0; phase = "work"; round = 1; done = 0
                persist()
            },
            L("tools.pomodoro.settings") to { sheet = "settings" },
        ),
        center = true,
        header = {
            val tabs = listOf(L("tool.tab.timer"), L("tool.tab.stats"))
            androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                tabs.forEachIndexed { i, t ->
                    SegmentedButton(view == i, { view = i },
                        androidx.compose.material3.SegmentedButtonDefaults.itemShape(i, tabs.size), colors = segColors()) { Text(t, maxLines = 1) }
                }
            }
        },
    ) {
        if (view == 0) {
            Spacer(Modifier.height(4.dp))
            Text(L("tools.pomodoro.$phase"), color = if (phase == "work") Theme.c.accent else AppData.color("religious"),
                fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            // Kalan süre halkası ve tur domatesleri (2. analiz #101)
            val pColor = if (phase == "work") Theme.c.accent else AppData.color("religious")
            ProgressRing(remaining.toFloat() / phaseMs().coerceAtLeast(1L), pColor, Modifier.fillMaxWidth(0.8f), stroke = 12.dp) {
                BigTime(clock((remaining + 999) / 1000))
            }
            RoundDots(rounds, if (phase == "long") rounds else round - 1, if (phase == "work") round else 0, T("tools.pomodoro.round", round, rounds))
            val todayCount = log.count { it.d == java.time.LocalDate.now().toString() }
            Text(if (todayCount >= goal) L("tools.pomodoro.goalDone") + " · " + T("tools.pomodoro.goalProgress", todayCount, goal)
                else T("tools.pomodoro.goalProgress", todayCount, goal),
                color = if (todayCount >= goal) Theme.c.accent else Theme.c.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(T("tools.pomodoro.completed", done), color = Theme.c.muted, fontSize = 13.sp)
            // Görev ve ses tek satır: dokununca alt sayfa
            AppCard(Modifier.fillMaxWidth().clickable { sheet = "task" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(L("tool.taskSound"), color = Theme.c.muted, fontSize = 12.sp)
                        Text(task.ifBlank { L("tools.pomodoro.noTask") } + " · " +
                            noiseName(noise) +
                            (if (tag != "none") " · " + L("tools.pomodoro.tags.$tag") else ""),
                            color = Theme.c.text, fontSize = 15.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.muted)
                }
            }
            Spacer(Modifier.height(8.dp))
        } else {
            FocusStatsCard(log) { PomoLog.save(emptyList()); log = emptyList() }
            FocusForest(log)
            Spacer(Modifier.height(8.dp))
        }
    }
    if (sheet != null) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.ModalBottomSheet({ sheet = null }, containerColor = Theme.c.bg) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (sheet == "task") {
                    OutlinedTextField(task, { task = it.take(60); ToolState.putStr("pomo.task", task) }, Modifier.fillMaxWidth(),
                        label = { Text(L("tools.pomodoro.taskLabel")) }, placeholder = { Text(L("tools.pomodoro.taskPlaceholder")) },
                        singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
                    // Etiket
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (k in PomoLog.TAGS) {
                            val on = k == tag
                            androidx.compose.foundation.layout.Box(
                                Modifier.background(if (on) Theme.c.accent else Theme.c.surface2, androidx.compose.foundation.shape.RoundedCornerShape(50))
                                    .clickable { tag = k; ToolState.putStr("pomo.tag", k) }.padding(horizontal = 14.dp, vertical = 8.dp),
                            ) { Text(L("tools.pomodoro.tags.$k"), color = if (on) Theme.c.bg else Theme.c.text, fontSize = 14.sp) }
                        }
                    }
                    // Tur bitince sayaçlar yeniden okunsun (geçmiş uzayınca kart yenilenir)
                    androidx.compose.runtime.key(log.size) { PomoTodoCard(task) { task = it; ToolState.putStr("pomo.task", it) } }
                    AppCard {
                        SectionTitle(L("tools.pomodoro.noise"))
                        NoiseControls(noise, volume,
                            onKind = { noise = it; ToolState.putStr("pomo.noise", it); PomoNoise.sync(context) },
                            onVolume = { volume = it; ToolState.putInt("pomo.vol", it); if (SoundService.source == "pomo") PomoNoise.sync(context) })
                        Text(L("pomo.noiseNote"), color = Theme.c.muted, fontSize = 12.sp)
                    }
                } else {
                    AppCard {
                        SectionTitle(L("tools.pomodoro.settings"))
                        Stepper(L("tools.pomodoro.workMin"), workMin, 5..90, 5) { workMin = it; ToolState.putInt("pomo.work", it) }
                        Stepper(L("tools.pomodoro.shortMin"), shortMin, 1..30, 1) { shortMin = it; ToolState.putInt("pomo.short", it) }
                        Stepper(L("tools.pomodoro.longMin"), longMin, 5..60, 5) { longMin = it; ToolState.putInt("pomo.long", it) }
                        Stepper(L("tools.pomodoro.roundsLabel"), rounds, 2..8, 1) { rounds = it; ToolState.putInt("pomo.rounds", it) }
                        Stepper(L("tools.pomodoro.goalLabel"), goal, 1..24, 1) { goal = it; ToolState.putInt("pomo.goal", it) }
                    }
                    FocusBlockCard()
                    // Telefon yüzüstüyken odak (2. analiz #29)
                    FlipFocusCard { end = ToolState.long("pomo.end"); left = ToolState.long("pomo.left"); now = System.currentTimeMillis() }
                }
            }
        }
    }
}

/** Başlık, değer ve − / + düğmeleri */
@Composable
internal fun Stepper(title: String, value: Int, range: IntRange, step: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Theme.c.text)
        IconButton(onClick = { onChange((value - step).coerceIn(range)) }, enabled = value > range.first) {
            Icon(Icons.Filled.Remove, null, tint = Theme.c.accent)
        }
        Ltr { Text("$value", color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, style = Tnum) }
        IconButton(onClick = { onChange((value + step).coerceIn(range)) }, enabled = value < range.last) {
            Icon(Icons.Filled.Add, null, tint = Theme.c.accent)
        }
    }
}
