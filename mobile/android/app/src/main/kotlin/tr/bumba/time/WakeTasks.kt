package tr.bumba.time

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.app.PendingIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import tr.bumba.time.core.MotionCounter
import tr.bumba.time.core.WakeTask
import kotlin.random.Random

/*
 * Alarm kapatma görevi ve uyanma denetimi (30 Ekim 2026, rakip analizi #13). Görevli alarm bildirimden kapatılamaz;
 * alarm ekranında (AlarmActivity) görev yapılınca susar: matematik (3 işlem), telefonu sallama (30), adım (20; ivmeölçer,
 * izin istemez) ya da kayıtlı QR/barkodu okutma (Google kod tarayıcı: kamera izni istemez, görüntü uygulamaya gelmez).
 * Sensör ya da tarayıcı yoksa "Başka görev" zor matematiğe geçer. Uyanma denetimi: kapattıktan 5 dk sonra
 * "Uyanık mısınız?" bildirimi, 2 dk içinde "Uyanığım"a basılmazsa yedek alarm çalar.
 */

object WakeCheck {
    const val KIND = "wakecheck"
    const val ACTION_AWAKE = "tr.bumba.time.WAKE_AWAKE"
    private const val ASK = "wakeask-"
    private const val BACKUP = "wakecheck-"

    /** Kapattıktan sonra: 5 dk sonra soru, 7 dk sonra yedek alarm */
    fun schedule(c: Context, alarmId: String, nowMs: Long = System.currentTimeMillis()) {
        val ask = nowMs + WakeTask.CHECK_AFTER_MIN * 60_000L
        AlarmScheduler.schedule(c, ASK + alarmId, ask, L("wake.checkTitle"), KIND)
        AlarmScheduler.schedule(c, BACKUP + alarmId, ask + WakeTask.CHECK_GRACE_MIN * 60_000L, L("wake.checkAlarm"), "alarm")
    }

    /** Soru bildirimi: "Uyanığım" ya da dokunmak yedek alarmı iptal eder */
    fun ask(c: Context, id: String) {
        val alarmId = id.removePrefix(ASK)
        val awake = PendingIntent.getBroadcast(c, (ACTION_AWAKE + alarmId).hashCode(),
            Intent(c, AlarmReceiver::class.java).setAction(ACTION_AWAKE).putExtra(AlarmScheduler.EXTRA_ID, alarmId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, AlarmScheduler.CHANNEL_TOOLS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(L("wake.checkTitle"))
            .setContentText(L("wake.checkBody"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(awake)
            .setAutoCancel(true)
            .addAction(0, L("wake.awake"), awake)
            .build()
        runCatching { NotificationManagerCompat.from(c).notify((ASK + alarmId).hashCode(), n) }
    }

    fun awake(c: Context, alarmId: String) {
        AlarmScheduler.cancel(c, BACKUP + alarmId)
        AlarmScheduler.cancel(c, ASK + alarmId)
        NotificationManagerCompat.from(c).cancel((ASK + alarmId).hashCode())
    }
}

/** Görev paneli (alarm ekranında Kapat'ın yerine); çözülünce [onSolved] */
@Composable
fun WakeTaskPanel(item: AlarmItem, onSolved: () -> Unit) {
    var kind by remember { mutableStateOf(item.effectiveTask) }
    var level by remember { mutableIntStateOf(item.taskLevel) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(L("wake.title"), color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        when (kind) {
            "math" -> MathTask(level, onSolved)
            "shake", "steps" -> MotionTask(kind, onSolved)
            "code" -> CodeTask(item.code, onSolved)
        }
        if (kind != "math") TextButton(onClick = { kind = "math"; level = 3 }) { Text(L("wake.other"), color = Theme.c.muted) }
    }
}

@Composable
private fun MathTask(level: Int, onSolved: () -> Unit) {
    val problems = remember(level) { val r = Random(System.nanoTime()); List(WakeTask.MATH_COUNT) { WakeTask.problem(level, r) } }
    var index by remember(level) { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    fun check() {
        if (answer.trim().toIntOrNull() == problems[index].answer) {
            wrong = false; answer = ""; index++
            if (index >= problems.size) onSolved()
        } else {
            wrong = true; answer = ""
        }
    }
    Text(T("wake.math", minOf(index + 1, problems.size), problems.size), color = Theme.c.muted)
    if (index < problems.size) Ltr {
        Text(problems[index].text + " = ?", color = Theme.c.ink, fontSize = 36.sp, fontWeight = FontWeight.Bold, style = Tnum)
    }
    OutlinedTextField(answer, { answer = it.filter(Char::isDigit).take(6) }, Modifier.widthIn(max = 220.dp), singleLine = true,
        label = { Text(L("wake.answer")) }, textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    if (wrong) Text(L("wake.wrong"), color = Color(0xFFD32F2F), fontSize = 14.sp)
    RoundButton(L("wake.check"), true, Modifier.fillMaxWidth()) { check() }
}

@Composable
private fun MotionTask(kind: String, onSolved: () -> Unit) {
    val context = LocalContext.current
    val total = if (kind == "shake") WakeTask.SHAKES else WakeTask.STEPS
    var count by remember(kind) { mutableIntStateOf(0) }
    val sm = context.getSystemService(SensorManager::class.java)
    val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    DisposableEffect(kind) {
        val counter = if (kind == "shake") MotionCounter.shake() else MotionCounter.steps()
        var done = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val g = MotionCounter.G
                if (counter.feed(e.timestamp / 1_000_000, e.values[0] / g, e.values[1] / g, e.values[2] / g)) {
                    count = counter.count
                    if (!done && count >= total) { done = true; onSolved() }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(listener) }
    }
    Text(T(if (kind == "shake") "wake.shake" else "wake.steps", total), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Ltr { Text(T("wake.progress", minOf(count, total), total), color = Theme.c.accent, fontSize = 44.sp, fontWeight = FontWeight.Bold, style = Tnum) }
    LinearProgressIndicator(progress = { minOf(count, total) / total.toFloat() }, Modifier.fillMaxWidth(), color = Theme.c.accent,
        trackColor = Theme.c.accent.copy(alpha = 0.2f))
    if (sensor == null) Text(L("wake.noSensor"), color = Theme.c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
}

@Composable
private fun CodeTask(code: String, onSolved: () -> Unit) {
    val context = LocalContext.current
    var wrong by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    Text(L("wake.code"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    RoundButton(L("wake.code.scan"), true, Modifier.fillMaxWidth()) {
        CodeScan.scan(context, onCode = { v -> if (v == code) onSolved() else wrong = true }, onFail = { failed = true })
    }
    if (wrong) Text(L("wake.code.wrong"), color = Color(0xFFD32F2F), fontSize = 14.sp)
    if (failed) Text(L("wake.noCamera"), color = Theme.c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
}

/** Google kod tarayıcı (Play hizmetleri): kamera izni istemez, yalnız okunan değer uygulamaya gelir */
object CodeScan {
    fun scan(c: Context, onCode: (String) -> Unit, onFail: () -> Unit) {
        runCatching {
            val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
            GmsBarcodeScanning.getClient(c, options).startScan()
                .addOnSuccessListener { b -> b.rawValue?.takeIf { it.isNotEmpty() }?.let(onCode) ?: onFail() }
                .addOnFailureListener { onFail() }
        }.onFailure { onFail() }
    }
}
