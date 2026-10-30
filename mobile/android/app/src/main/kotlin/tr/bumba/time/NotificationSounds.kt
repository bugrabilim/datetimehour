package tr.bumba.time

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tr.bumba.time.core.Prayer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Bildirim sesleri telefonda üretilir (iOS'taki NotificationSounds gibi): kısa bir zil ve isteğe bağlı sesli duyuru
 * ("Öğle vakti geldi."). Duyuru telefonun kendi metin okuma motoruyla (TextToSpeech) dosyaya yazılır; dışarıdan ses
 * kaydı yok, internet gerekmez. "Ezan" seçeneği (30 Ekim 2026, sahibin kararı): vaktinde uygulamayla gelen 5 sn'lik ezan
 * kesiti (res/raw/ezan.ogg; Wikimedia Commons, CC BY-SA 4.0, atıf ayarda; kaynak mobile/shared/ezan/README.md);
 * vakitten önceki bildirimlerde ve Güneş'te sesli duyuru çalar.
 *
 * Neden bildirim kanalının sesi değil: Android'de kanal sesi kanal kurulunca sabitlenir ve sistem arayüzünün
 * okuyabileceği bir adres ister (uygulama dosyası için FileProvider + sistem paketine izin; üreticiye göre kırılgan).
 * Bunun yerine bildirim sessiz kanaldan gösterilir, zil ve duyuru bildirim anında alıcıdan (PrayerAlarmReceiver,
 * goAsync) bildirim ses düzeyiyle çalınır. Telefon sessiz/titreşimdeyse ya da Rahatsız Etmeyin açıksa çalınmaz.
 * "Telefonun sesi" seçiliyse bildirim, varsayılan bildirim sesli kanaldan gelir.
 */
object NotificationSounds {
    /** Dosya adı sürümü: ses üretimi değişirse artırılır, eski dosyalar silinir */
    private const val VERSION = "s1"
    private const val RATE = 22050

    fun dir(context: Context) = File(context.filesDir, "sounds").apply { mkdirs() }

    /** Uygulamanın dili ve konuşma dili (Azericede Türkçe ses, iOS'taki gibi) */
    private val speechLocale: Locale
        get() = when (Strings.lang) {
            "en" -> Locale.US
            "de" -> Locale.GERMANY
            "ar" -> Locale.forLanguageTag("ar-SA")
            else -> Locale.forLanguageTag("tr-TR")
        }

    fun spokenText(prayer: Prayer, lead: Int): String =
        if (lead > 0) T("notify.spoken.before", prayer.title, lead) else T("notify.spoken.now", prayer.title)

    fun chimeFile(context: Context): File {
        val f = File(dir(context), "$VERSION-chime.wav")
        if (!f.exists()) writeWav(f, chime())
        return f
    }

    /** Sesli duyuru dosyası (zil ayrı çalınır); motor ya da dil yoksa null */
    private fun voiceFile(context: Context, voiceKey: String, prayer: Prayer, lead: Int) =
        File(dir(context), "$VERSION-${Strings.lang}-$voiceKey-${prayer.key}-$lead.wav")

    /** Uygulamayla gelen ezan kesiti, çalınabilsin diye uygulama dosyalarına bir kez kopyalanır */
    fun adhanFile(context: Context): File {
        val f = File(dir(context), "ezan-v1.ogg")
        if (!f.exists() || f.length() == 0L) {
            context.resources.openRawResource(R.raw.ezan).use { input -> FileOutputStream(f).use { input.copyTo(it) } }
        }
        return f
    }

    /** Ezan yalnız vaktinde ve namaz vakitlerinde (Güneş bir namaz vakti değil); diğerlerinde sesli duyuru */
    fun effective(mode: String, prayer: Prayer, lead: Int): String =
        if (mode == "adhan" && (lead > 0 || prayer == Prayer.SUNRISE)) "voice" else mode

    /** Bildirim anında çalınacak dosyalar (sırayla); hazır değilse yalnız zil */
    fun filesFor(context: Context, prayer: Prayer, lead: Int, mode: String): List<File> = when (effective(mode, prayer, lead)) {
        "system" -> emptyList()
        "chime" -> listOf(chimeFile(context))
        "adhan" -> listOf(adhanFile(context))
        else -> listOf(chimeFile(context)) + listOfNotNull(
            dir(context).listFiles()?.firstOrNull { it.name.startsWith("$VERSION-${Strings.lang}-") && it.name.endsWith("-${prayer.key}-$lead.wav") },
        )
    }

    /**
     * Plan kurulurken eksik duyuru dosyalarını üretir (vakit başına bir kez; dil, ses ya da süre değişince yeniden).
     * Eski sürüm ya da başka dildeki dosyalar silinir.
     */
    suspend fun prepare(context: Context, prayers: Set<Prayer>, lead: Int) = withContext(Dispatchers.IO) {
        chimeFile(context)
        withTts(context) { tts, voiceKey ->
            val keep = prayers.map { voiceFile(context, voiceKey, it, lead).name }.toSet()
            dir(context).listFiles()?.forEach { f ->
                if (!f.name.startsWith("$VERSION-chime") && !f.name.startsWith("ezan-") && f.name !in keep) f.delete()
            }
            for (p in prayers) {
                val f = voiceFile(context, voiceKey, p, lead)
                if (!f.exists() || f.length() < 1000) synthesize(tts, spokenText(p, lead), f)
            }
        }
    }

    /** Ses durumu: kurulu ses yok / standart ses / yüksek kaliteli ses (ayar sayfasındaki ipucu için) */
    enum class VoiceStatus { MISSING, BASIC, GOOD }

    suspend fun voiceStatus(context: Context): VoiceStatus =
        withTts(context) { tts, _ -> if ((tts.voice?.quality ?: 0) >= Voice.QUALITY_HIGH) VoiceStatus.GOOD else VoiceStatus.BASIC }
            ?: VoiceStatus.MISSING

    /** Ayar sayfasındaki "Dinle": seçili sesin örneği (öğle vakti); telefon sessizde de çalar (kullanıcı istedi) */
    suspend fun preview(context: Context, mode: String, lead: Int) {
        if (mode == "system") {
            runCatching {
                RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
            }
            return
        }
        if (effective(mode, Prayer.DHUHR, lead) == "voice") prepare(context, setOf(Prayer.DHUHR), lead)
        play(context, filesFor(context, Prayer.DHUHR, lead, mode), AudioAttributes.USAGE_MEDIA) {}
    }

    /** Bildirim anında: sessiz modda, titreşimde ya da Rahatsız Etmeyin'de çalınmaz */
    fun canPlayNow(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return false
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
        val nm = context.getSystemService(NotificationManager::class.java)
        val filter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
        return filter == NotificationManager.INTERRUPTION_FILTER_ALL || filter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    private var player: MediaPlayer? = null

    /** Dosyaları sırayla çalar (bildirim ses düzeyiyle); bitince ya da hata olunca onDone */
    fun play(context: Context, files: List<File>, usage: Int = AudioAttributes.USAGE_NOTIFICATION, onDone: () -> Unit) {
        val main = Handler(Looper.getMainLooper())
        main.post {
            val am = context.getSystemService(AudioManager::class.java)
            val attrs = AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attrs).build()
            am?.requestAudioFocus(focus)
            fun finish() {
                am?.abandonAudioFocusRequest(focus)
                onDone()
            }
            fun next(i: Int) {
                player?.release()
                player = null
                if (i >= files.size) { finish(); return }
                val mp = MediaPlayer()
                player = mp
                val ok = runCatching {
                    mp.setAudioAttributes(attrs)
                    mp.setDataSource(files[i].absolutePath)
                    mp.setOnCompletionListener { next(i + 1) }
                    mp.setOnErrorListener { _, _, _ -> next(i + 1); true }
                    mp.prepare()
                    mp.start()
                }.isSuccess
                if (!ok) next(i + 1)
            }
            next(0)
        }
    }

    // MARK: Üretim

    /**
     * Metin okuma motorunu açar, uygulamanın diline uygun en iyi kurulu sesi seçer (önce yüksek kalite, ağ gerektirmeyen);
     * dil yoksa null döner. Ses kimliği dosya adına girer: kullanıcı daha iyi bir ses kurarsa sesler yeniden üretilir.
     */
    private suspend fun <T> withTts(context: Context, block: suspend (TextToSpeech, String) -> T): T? {
        var engine: TextToSpeech? = null
        val ready = withTimeoutOrNull(8000) {
            suspendCancellableCoroutine { cont ->
                engine = TextToSpeech(context.applicationContext) { status ->
                    if (cont.isActive) cont.resume(status == TextToSpeech.SUCCESS)
                }
            }
        } ?: false
        val tts = engine ?: return null
        try {
            if (!ready) return null
            val target = speechLocale
            val voice = runCatching { tts.voices }.getOrNull().orEmpty()
                .filter {
                    it.locale.language == target.language && !it.isNetworkConnectionRequired &&
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
                }
                .maxWithOrNull(compareBy<Voice>({ it.quality }, { it.locale.country == target.country }))
            val key = if (voice != null) {
                tts.voice = voice
                voice.name
            } else {
                if (tts.isLanguageAvailable(target) < TextToSpeech.LANG_AVAILABLE) return null
                tts.language = target
                "default"
            }
            tts.setSpeechRate(0.9f)
            return block(tts, key.filter { it.isLetterOrDigit() }.takeLast(40))
        } finally {
            tts.shutdown()
        }
    }

    private suspend fun synthesize(tts: TextToSpeech, text: String, out: File): Boolean {
        val tmp = File(out.parentFile, out.name + ".tmp")
        val id = out.name
        val ok = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { cont ->
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { if (utteranceId == id && cont.isActive) cont.resume(true) }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { if (utteranceId == id && cont.isActive) cont.resume(false) }
                    override fun onError(utteranceId: String?, errorCode: Int) { if (utteranceId == id && cont.isActive) cont.resume(false) }
                })
                if (tts.synthesizeToFile(text, Bundle(), tmp, id) != TextToSpeech.SUCCESS && cont.isActive) cont.resume(false)
            }
        } ?: false
        if (ok && tmp.length() > 1000) tmp.renameTo(out) else tmp.delete()
        return out.exists()
    }

    /** İki notalı yumuşak zil (E5 → A5), sönümlü; sonunda kısa sessizlik (iOS'taki zille aynı) */
    private fun chime(): ShortArray {
        val total = (RATE * 1.5).toInt()
        val notes = listOf(0.0 to 659.25, 0.28 to 880.0)
        return ShortArray(total) { i ->
            val t = i.toDouble() / RATE
            var v = 0.0
            for ((start, freq) in notes) {
                if (t < start) continue
                val dt = t - start
                val env = min(1.0, dt / 0.01) * exp(-dt * 4)
                v += sin(2 * PI * freq * dt) * env + 0.3 * sin(4 * PI * freq * dt) * env * exp(-dt * 6)
            }
            (v * 0.32).coerceIn(-1.0, 1.0).times(32767).toInt().toShort()
        }
    }

    /** 16 bit, tek kanal PCM WAV */
    private fun writeWav(f: File, samples: ShortArray) {
        val data = samples.size * 2
        val b = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(RATE).putInt(RATE * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(data)
        for (s in samples) b.putShort(s)
        val tmp = File(f.parentFile, f.name + ".tmp")
        FileOutputStream(tmp).use { it.write(b.array()) }
        tmp.renameTo(f)
    }
}
