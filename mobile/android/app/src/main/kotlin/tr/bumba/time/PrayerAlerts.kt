package tr.bumba.time

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tr.bumba.time.core.Prayer
import java.time.Instant
import java.time.LocalDate

/**
 * Vakit bildirimi planı (iOS'taki SaatCore/PrayerAlerts.swift ile aynı kural): `now` sonrasındaki bildirimler,
 * zamana göre sıralı; en çok `limit` tane ve `days` gün. Bildirimi işletim sistemi kurar; burada yalnız hangi
 * anlarda ne bildirileceği hesaplanır (JVM testi: PrayerAlertsTest).
 */
object PrayerAlerts {
    data class Settings(
        val enabled: Boolean = false,
        val prayers: Set<Prayer> = DEFAULT_PRAYERS,
        /** Vakitten kaç dakika önce (0 = vaktinde) */
        val leadMinutes: Int = 0,
        /** Vakte özel "önce" süresi (2. analiz #54); yoksa genel süre */
        val perLead: Map<Prayer, Int> = emptyMap(),
        /** Vakte özel günler (ISO 1 pazartesi … 7 pazar); yoksa her gün */
        val days: Map<Prayer, Set<Int>> = emptyMap(),
    ) {
        fun lead(p: Prayer): Int = maxOf(0, perLead[p] ?: leadMinutes)
    }

    data class Alert(val prayer: Prayer, val prayerAt: Instant, val fireAt: Instant)

    /** Varsayılan: beş vakit (güneş doğuşu kapalı) */
    val DEFAULT_PRAYERS: Set<Prayer> = setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
    val LEAD_CHOICES = listOf(0, 5, 10, 15, 30, 45)

    fun plan(place: AppPlace, settings: Settings, now: Instant, limit: Int = 63, days: Int = 30): List<Alert> {
        if (!settings.enabled || settings.prayers.isEmpty() || limit <= 0) return emptyList()
        val start: LocalDate = now.atZone(place.zone).toLocalDate()
        val out = ArrayList<Alert>()
        for (add in 0 until days) {
            val day = start.plusDays(add.toLong())
            for (e in place.entries(day)) {
                if (e.prayer !in settings.prayers) continue
                val only = settings.days[e.prayer]
                if (!only.isNullOrEmpty() && day.dayOfWeek.value !in only) continue
                val fire = e.at.minusSeconds(settings.lead(e.prayer) * 60L)
                if (!fire.isAfter(now)) continue
                out += Alert(e.prayer, e.at, fire)
                if (out.size == limit) return out
            }
        }
        return out
    }
}

/**
 * Bildirim tercihleri (iOS'taki NotificationPrefs ile aynı anahtarlar; "prefs" dosyasında). Ekranlar Compose durumu
 * olarak okur; değişince plan yeniden kurulur (PrayerNotifier).
 */
object NotifyPrefs {
    const val ENABLED = "notify.enabled"
    const val PRAYERS = "notify.prayers"
    const val LEAD = "notify.lead"
    const val SOUND = "notify.sound"
    const val ADHAN_VOICE = "notify.adhanVoice"

    private val sp get() = Prefs.store
    private var loaded = false

    /** Varsayılan açık (30 Ekim 2026, sahibin isteği: her özelliğin bildirimi açık gelir, kullanıcı kapatır) */
    var enabled by mutableStateOf(true)
        private set
    var prayersRaw by mutableStateOf(encode(PrayerAlerts.DEFAULT_PRAYERS))
        private set
    var lead by mutableIntStateOf(0)
        private set
    /** "voice" (sesli duyuru, varsayılan), "chime" (kısa zil), "adhan" (vaktinde ezanın ilk cümlesi), "system" (telefonun sesi) */
    var sound by mutableStateOf("voice")
        private set
    /** Ezan sesi (#92; iOS'la aynı kimlikler): "azeez" (5 sn, varsayılan), "medina" */
    var adhanVoice by mutableStateOf("azeez")
        private set

    /** Prefs.init'ten sonra ilk kullanımda okunur */
    fun load() {
        if (loaded) return
        loaded = true
        enabled = sp.getBoolean(ENABLED, true)
        prayersRaw = sp.getString(PRAYERS, null) ?: encode(PrayerAlerts.DEFAULT_PRAYERS)
        lead = sp.getInt(LEAD, 0)
        sound = sp.getString(SOUND, null)?.takeIf { it in SOUNDS } ?: "voice"
        adhanVoice = sp.getString(ADHAN_VOICE, null)?.takeIf { it in ADHAN_VOICES } ?: "azeez"
    }

    val SOUNDS = listOf("voice", "chime", "adhan", "system")
    val ADHAN_VOICES = listOf("azeez", "medina")

    val prayers: Set<Prayer> get() { load(); return decode(prayersRaw) }

    val current: PrayerAlerts.Settings
        get() {
            load()
            return PrayerAlerts.Settings(enabled, decode(prayersRaw), lead,
                Prayer.entries.mapNotNull { p -> sp.getInt("notify.p.${p.key}.lead", -1).takeIf { it >= 0 }?.let { p to it } }.toMap(),
                Prayer.entries.mapNotNull { p -> days(p).takeIf { it.isNotEmpty() }?.let { p to it } }.toMap())
        }

    // Vakte özel ayarlar (2. analiz #54): "önce" süresi (-1 genel), ses ("" genel) ve günler ("" her gün)
    var perVersion by mutableIntStateOf(0)
        private set
    fun leadFor(p: Prayer): Int = sp.getInt("notify.p.${p.key}.lead", -1)
    fun saveLeadFor(p: Prayer, v: Int) { sp.edit().putInt("notify.p.${p.key}.lead", v).apply(); perVersion++ }
    fun soundFor(p: Prayer): String = sp.getString("notify.p.${p.key}.sound", "")?.takeIf { it in SOUNDS } ?: sound
    fun rawSoundFor(p: Prayer): String = sp.getString("notify.p.${p.key}.sound", "") ?: ""
    fun saveSoundFor(p: Prayer, v: String) { sp.edit().putString("notify.p.${p.key}.sound", v).apply(); perVersion++ }
    fun days(p: Prayer): Set<Int> = (sp.getString("notify.p.${p.key}.days", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }.toSet()
    fun saveDays(p: Prayer, v: Set<Int>) { sp.edit().putString("notify.p.${p.key}.days", v.sorted().joinToString(",")).apply(); perVersion++ }

    fun saveEnabled(v: Boolean) { load(); enabled = v; sp.edit().putBoolean(ENABLED, v).apply() }
    fun savePrayers(v: Set<Prayer>) { load(); prayersRaw = encode(v); sp.edit().putString(PRAYERS, prayersRaw).apply() }
    fun saveLead(v: Int) { load(); lead = v; sp.edit().putInt(LEAD, v).apply() }
    fun saveSound(v: String) { load(); sound = v; sp.edit().putString(SOUND, v).apply() }
    fun saveAdhanVoice(v: String) { load(); adhanVoice = v; sp.edit().putString(ADHAN_VOICE, v).apply() }

    fun decode(raw: String): Set<Prayer> =
        raw.split(",").mapNotNull { k -> Prayer.entries.firstOrNull { it.key == k } }.toSet()

    fun encode(set: Set<Prayer>): String = Prayer.entries.filter { it in set }.joinToString(",") { it.key }
}
