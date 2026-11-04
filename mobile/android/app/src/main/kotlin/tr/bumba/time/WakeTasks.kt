package tr.bumba.time

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.app.PendingIntent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

    /** Soru bildirimi: "Uyanığım" ya da dokunmak yedek alarmı iptal eder. Ekran kilitliyken ya da kapalıyken uygulama
     *  içi geri sayım halkası (WakeCheckActivity, 2. analiz #45) tam ekran açılır. */
    fun ask(c: Context, id: String) {
        val alarmId = id.removePrefix(ASK)
        val awake = PendingIntent.getBroadcast(c, (ACTION_AWAKE + alarmId).hashCode(),
            Intent(c, AlarmReceiver::class.java).setAction(ACTION_AWAKE).putExtra(AlarmScheduler.EXTRA_ID, alarmId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val deadline = System.currentTimeMillis() + WakeTask.CHECK_GRACE_MIN * 60_000L
        val screen = PendingIntent.getActivity(c, (ASK + alarmId).hashCode(),
            Intent(c, WakeCheckActivity::class.java).putExtra(AlarmScheduler.EXTRA_ID, alarmId).putExtra(WakeCheckActivity.DEADLINE, deadline)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, AlarmScheduler.CHANNEL_TOOLS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(L("wake.checkTitle"))
            .setContentText(L("wake.checkBody"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
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

/** Görev paneli (alarm ekranında Kapat'ın yerine); görevler sırayla (2. analiz #34 zincir), hepsi çözülünce [onSolved].
 *  [tasks] verilmezse alarmın zinciri (rastgele olanlar burada seçilir); ertele görevi ve önizleme kendi listesini verir. */
@Composable
fun WakeTaskPanel(item: AlarmItem, tasks: List<String>? = null, title: String = L("wake.title"), onSolved: () -> Unit) {
    val list = remember(item.id) { (tasks ?: item.taskChain()).ifEmpty { listOf("math") } }
    var step by remember { mutableIntStateOf(0) }
    var other by remember { mutableStateOf<String?>(null) }
    var level by remember { mutableIntStateOf(item.taskLevel) }
    val kind = other ?: list[step.coerceAtMost(list.size - 1)]
    val next = {
        if (step + 1 >= list.size) onSolved() else { step += 1; other = null; level = item.taskLevel }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, color = Theme.c.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (list.size > 1) Text(T("wake.step", step + 1, list.size), color = Theme.c.muted, fontSize = 14.sp)
        androidx.compose.runtime.key(step, kind) {
            when (kind) {
                "math" -> MathTask(level, next)
                "shake", "steps", "squat" -> MotionTask(kind, next)
                "object" -> ObjectTask(item.code, next)
                "speak" -> SpeakTask(next) { other = "typing" }
                "code" -> CodeTask(item.code, next)
                "memory" -> MemoryTask(level, next)
                "typing" -> TypingTask(next)
                "photo" -> PhotoTask(item.code, next)
                "light" -> LightTask(next) { other = "math"; level = 3 }
                else -> MathTask(level, next)
            }
        }
        if (kind != "math") TextButton(onClick = { other = "math"; level = 3 }) { Text(L("wake.other"), color = Theme.c.muted) }
    }
}

/** Işık görevi (2. analiz #38; yalnız Android): ortam ışığı sensörü 3 sn boyunca aydınlık (≥ 100 lüks ve başlangıcın
 *  3 katı) görmeli; sensör yoksa [onUnavailable] (zor matematik) */
@Composable
private fun LightTask(onSolved: () -> Unit, onUnavailable: () -> Unit) {
    val c = LocalContext.current
    val sm = remember { c.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember { sm?.getDefaultSensor(Sensor.TYPE_LIGHT) }
    if (sensor == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onUnavailable() }
        return
    }
    var lux by remember { mutableStateOf(0f) }
    var held by remember { mutableStateOf(0f) }
    DisposableEffect(sensor) {
        var base = -1f
        var since = 0L
        var done = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val v = e.values[0]
                lux = v
                if (base < 0) base = v
                val need = maxOf(WakeTask.LIGHT_LUX, base * 3)
                val now = android.os.SystemClock.elapsedRealtime()
                if (v >= need) { if (since == 0L) since = now } else since = 0L
                held = if (since == 0L) 0f else ((now - since) / 3000f).coerceIn(0f, 1f)
                if (!done && held >= 1f) { done = true; onSolved() }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }
    Text(L("wake.light"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    Text("☀️ " + T("wake.lux", lux.toInt()), color = Theme.c.muted, fontSize = 15.sp)
    LinearProgressIndicator(progress = { held }, modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp), color = Theme.c.accent, trackColor = Theme.c.surface2)
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
    if (wrong) Text(L("wake.wrong"), color = Theme.c.err, fontSize = 14.sp)
    RoundButton(L("wake.check"), true, Modifier.fillMaxWidth()) { check() }
}

@Composable
private fun MotionTask(kind: String, onSolved: () -> Unit) {
    val context = LocalContext.current
    val total = when (kind) { "shake" -> WakeTask.SHAKES; "squat" -> WakeTask.SQUATS; else -> WakeTask.STEPS }
    var count by remember(kind) { mutableIntStateOf(0) }
    val sm = context.getSystemService(SensorManager::class.java)
    val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    DisposableEffect(kind) {
        val counter = when (kind) { "shake" -> MotionCounter.shake(); "squat" -> MotionCounter.squat(); else -> MotionCounter.steps() }
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
    Text(T(when (kind) { "shake" -> "wake.shake"; "squat" -> "wake.squat"; else -> "wake.steps" }, total), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
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
    if (wrong) Text(L("wake.code.wrong"), color = Theme.c.err, fontSize = 14.sp)
    if (failed) Text(L("wake.noCamera"), color = Theme.c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
}

/** Hafıza (#75): 3×3 ızgarada kareler sırayla yanar, aynı sırayla dokunulur; yanlışta yeni dizi */
@Composable
private fun MemoryTask(level: Int, onSolved: () -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    val seq = remember(round) { WakeTask.memorySequence(level, Random(System.nanoTime())) }
    var lit by remember(round) { mutableIntStateOf(-1) }
    var showing by remember(round) { mutableStateOf(true) }
    var pos by remember(round) { mutableIntStateOf(0) }
    var wrong by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(round) {
        kotlinx.coroutines.delay(600)
        for (c in seq) { lit = c; kotlinx.coroutines.delay(650); lit = -1; kotlinx.coroutines.delay(250) }
        showing = false
    }
    Text(L(if (showing) "wake.memory.watch" else "wake.memory.repeat"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until 3) androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (col in 0 until 3) {
                val i = r * 3 + col
                androidx.compose.foundation.layout.Box(
                    Modifier.size(72.dp).background(if (lit == i) Theme.c.accent else Theme.c.surface2, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable(enabled = !showing) {
                            if (seq[pos] == i) {
                                lit = i; pos++
                                if (pos >= seq.size) onSolved()
                            } else { wrong = true; round++ }
                        },
                )
            }
        }
    }
    Text(T("wake.progress", pos, seq.size), color = Theme.c.muted, fontSize = 14.sp)
    if (wrong) Text(L("wake.memory.wrong"), color = Theme.c.err, fontSize = 14.sp)
}

/** Yazma (#75): günün sözlerinden rastgele biri aynen yazılır (büyük/küçük harf ve noktalama sayılmaz) */
@Composable
private fun TypingTask(onSolved: () -> Unit) {
    val context = LocalContext.current
    val phrase = remember { Sayings.random(context) ?: "Saat Tarih" }
    var input by remember { mutableStateOf("") }
    Text(L("wake.typing"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Text(phrase, color = Theme.c.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    OutlinedTextField(input, { input = it; if (WakeTask.typed(phrase, it)) onSolved() }, Modifier.fillMaxWidth(), label = { Text(L("wake.typing.field")) })
}

/** Fotoğraf (#75): kayıtlı yerin fotoğrafı çekilir; 9×8 gri özet (dHash) kayıtlıyla karşılaştırılır. Fotoğraf saklanmaz. */
object PhotoHash {
    const val PREFIX = "ph:"
    fun of(bmp: android.graphics.Bitmap): Long {
        val small = android.graphics.Bitmap.createScaledBitmap(bmp, 9, 8, true)
        val gray = IntArray(72) { i ->
            val p = small.getPixel(i % 9, i / 9)
            ((p shr 16 and 0xFF) * 299 + (p shr 8 and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
        }
        return WakeTask.dHash(gray)
    }
    fun encode(h: Long) = PREFIX + java.lang.Long.toHexString(h)
    fun decode(code: String): Long? = if (code.startsWith(PREFIX)) java.lang.Long.parseUnsignedLong(code.removePrefix(PREFIX), 16) else null
}

@Composable
private fun PhotoTask(code: String, onSolved: () -> Unit) {
    var wrong by remember { mutableStateOf(false) }
    val target = PhotoHash.decode(code)
    val take = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null && target != null) { if (WakeTask.samePhoto(PhotoHash.of(bmp), target)) onSolved() else wrong = true }
    }
    Text(L("wake.photo"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    RoundButton(L("wake.photo.take"), true, Modifier.fillMaxWidth()) { runCatching { take.launch(null) } }
    if (wrong) Text(L("wake.photo.wrong"), color = Theme.c.err, fontSize = 14.sp, textAlign = TextAlign.Center)
}

/** Nesne görevi (2. analiz #36): sistem kamerasıyla çekilen küçük görüntü ML Kit görüntü etiketleme ile telefonda
 *  sınıflandırılır (Play hizmetleri modeli; ağa çıkmaz); hedef `code` alanında "obj:<kimlik>", yoksa her seferinde rastgele.
 *  Görüntü saklanmaz. */
object ObjectTarget {
    const val PREFIX = "obj:"
    fun of(code: String): String = code.removePrefix(PREFIX).takeIf { code.startsWith(PREFIX) && it in WakeTask.OBJECTS }
        ?: WakeTask.OBJECTS.keys.random()
}

@Composable
private fun ObjectTask(code: String, onSolved: () -> Unit) {
    val target = remember { ObjectTarget.of(code) }
    var wrong by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val take = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp == null) return@rememberLauncherForActivityResult
        busy = true
        runCatching {
            val labeler = com.google.mlkit.vision.label.ImageLabeling.getClient(
                com.google.mlkit.vision.label.defaults.ImageLabelerOptions.Builder().setConfidenceThreshold(WakeTask.OBJECT_MIN).build())
            labeler.process(com.google.mlkit.vision.common.InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { labels ->
                    busy = false
                    if (WakeTask.objectMatch(target, labels.map { it.text to it.confidence })) onSolved() else wrong = true
                }
                .addOnFailureListener { busy = false; wrong = true }
        }.onFailure { busy = false; wrong = true }
    }
    Text(T("wake.object", L("wake.object.$target")), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    RoundButton(L("wake.photo.take"), !busy, Modifier.fillMaxWidth()) { wrong = false; runCatching { take.launch(null) } }
    if (wrong) Text(L("wake.object.wrong"), color = Theme.c.err, fontSize = 14.sp, textAlign = TextAlign.Center)
    Text(L("wake.object.note"), color = Theme.c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
}

/** Sesli okuma (#37): günün sözlerinden biri; Android'in cihaz içi konuşma tanıması (Android 12+ createOnDeviceSpeechRecognizer,
 *  öncesinde çevrimdışı tercihli tanıyıcı). Ses kaydedilmez. Tanıma yoksa [onUnavailable] (yazma görevi). Mikrofon izni burada istenir. */
@Composable
private fun SpeakTask(onSolved: () -> Unit, onUnavailable: () -> Unit) {
    val context = LocalContext.current
    val phrase = remember { Sayings.random(context) ?: "Saat Tarih" }
    var heard by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    val available = remember {
        android.speech.SpeechRecognizer.isRecognitionAvailable(context) ||
            (android.os.Build.VERSION.SDK_INT >= 31 && runCatching { android.speech.SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false))
    }
    androidx.compose.runtime.LaunchedEffect(available) { if (!available) onUnavailable() }
    val recognizer = remember {
        if (!available) null else runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 31 && android.speech.SpeechRecognizer.isOnDeviceRecognitionAvailable(context))
                android.speech.SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else android.speech.SpeechRecognizer.createSpeechRecognizer(context)
        }.getOrNull()
    }
    DisposableEffect(recognizer) { onDispose { recognizer?.destroy() } }
    fun listen() {
        val r = recognizer ?: run { onUnavailable(); return }
        r.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: android.os.Bundle) {
                listening = false
                val list = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                heard = list.firstOrNull().orEmpty()
                if (list.any { WakeTask.spoken(phrase, it) }) onSolved()
            }
            override fun onPartialResults(partial: android.os.Bundle) {
                partial.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { heard = it }
            }
            override fun onError(error: Int) { listening = false }
            override fun onReadyForSpeech(p: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(t: Int, b: android.os.Bundle?) {}
        })
        val i = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Strings.locale.toLanguageTag())
            .putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        listening = true
        runCatching { r.startListening(i) }.onFailure { listening = false; onUnavailable() }
    }
    val mic = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) listen() else onUnavailable()
    }
    Text(L("wake.speak"), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Text(phrase, color = Theme.c.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    RoundButton(if (listening) L("wake.speak.listening") else L("wake.speak.start"), !listening, Modifier.fillMaxWidth()) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) listen() else mic.launch(android.Manifest.permission.RECORD_AUDIO)
    }
    if (heard.isNotEmpty()) Text(T("wake.speak.heard", heard), color = Theme.c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    Text(L("wake.speak.note"), color = Theme.c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
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

/** Uyanma denetimi ekranı (2. analiz #45): yedek alarma kalan süre halka olarak azalır; "Uyanığım" denetimi bitirir.
 *  Süre dolunca yedek alarm kendisi çalar (AlarmManager), bu ekran kapanır. */
class WakeCheckActivity : androidx.activity.ComponentActivity() {
    companion object { const val DEADLINE = "deadline" }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: run { finish(); return }
        val deadline = intent.getLongExtra(DEADLINE, System.currentTimeMillis() + WakeTask.CHECK_GRACE_MIN * 60_000L)
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            AppTheme {
                WakeCheckScreen(deadline, onAwake = {
                    WakeCheck.awake(this, alarmId)
                    finish()
                }, onTimeout = { finish() })
            }
        }
    }
}

/** Geri sayım halkası + "Uyanığım" (önizleme ve testte de kullanılır) */
@Composable
fun WakeCheckScreen(deadline: Long, onAwake: () -> Unit, onTimeout: () -> Unit) {
    val total = WakeTask.CHECK_GRACE_MIN * 60_000f
    var left by remember { mutableStateOf((deadline - System.currentTimeMillis()).coerceAtLeast(0)) }
    androidx.compose.runtime.LaunchedEffect(deadline) {
        while (left > 0) {
            kotlinx.coroutines.delay(200)
            left = (deadline - System.currentTimeMillis()).coerceAtLeast(0)
        }
        onTimeout()
    }
    val accent = Theme.c.accent
    val track = Theme.c.surface2
    Column(
        Modifier.fillMaxSize().background(Theme.c.bg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text(L("wake.checkTitle"), color = Theme.c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        androidx.compose.foundation.layout.Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(220.dp)) {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 16.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                val inset = 8.dp.toPx()
                val sz = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                val tl = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(track, 0f, 360f, false, tl, sz, style = stroke)
                drawArc(accent, -90f, 360f * (left / total).coerceIn(0f, 1f), false, tl, sz, style = stroke)
            }
            val s = (left / 1000).toInt()
            Ltr { Text("%d:%02d".format(java.util.Locale.ROOT, s / 60, s % 60), color = Theme.c.ink, fontSize = 44.sp, fontWeight = FontWeight.Bold, style = Tnum) }
        }
        Text(L("wake.checkRing"), color = Theme.c.muted, fontSize = 15.sp, textAlign = TextAlign.Center)
        RoundButton(L("wake.awake"), true, Modifier.fillMaxWidth().widthIn(max = 360.dp), onAwake)
    }
}
