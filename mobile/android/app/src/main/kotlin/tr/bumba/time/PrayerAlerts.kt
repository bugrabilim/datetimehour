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
    )

    data class Alert(val prayer: Prayer, val prayerAt: Instant, val fireAt: Instant)

    /** Varsayılan: beş vakit (güneş doğuşu kapalı) */
    val DEFAULT_PRAYERS: Set<Prayer> = setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
    val LEAD_CHOICES = listOf(0, 5, 10, 15, 30, 45)

    fun plan(place: AppPlace, settings: Settings, now: Instant, limit: Int = 63, days: Int = 30): List<Alert> {
        if (!settings.enabled || settings.prayers.isEmpty() || limit <= 0) return emptyList()
        val lead = maxOf(0, settings.leadMinutes) * 60L
        val start: LocalDate = now.atZone(place.zone).toLocalDate()
        val out = ArrayList<Alert>()
        for (add in 0 until days) {
            for (e in place.entries(start.plusDays(add.toLong()))) {
                if (e.prayer !in settings.prayers) continue
                val fire = e.at.minusSeconds(lead)
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

    private val sp get() = Prefs.store
    private var loaded = false

    /** Varsayılan açık (30 Ekim 2026, sahibin isteği: her özelliğin bildirimi açık gelir, kullanıcı kapatır) */
    var enabled by mutableStateOf(true)
        private set
    var prayersRaw by mutableStateOf(encode(PrayerAlerts.DEFAULT_PRAYERS))
        private set
    var lead by mutableIntStateOf(0)
        private set
    /** "voice" (sesli duyuru, varsayılan), "chime" (kısa zil), "adhan" (vaktinde 5 sn ezan), "system" (telefonun sesi) */
    var sound by mutableStateOf("voice")
        private set

    /** Prefs.init'ten sonra ilk kullanımda okunur */
    fun load() {
        if (loaded) return
        loaded = true
        enabled = sp.getBoolean(ENABLED, true)
        prayersRaw = sp.getString(PRAYERS, null) ?: encode(PrayerAlerts.DEFAULT_PRAYERS)
        lead = sp.getInt(LEAD, 0)
        sound = sp.getString(SOUND, null)?.takeIf { it in SOUNDS } ?: "voice"
    }

    val SOUNDS = listOf("voice", "chime", "adhan", "system")

    val prayers: Set<Prayer> get() { load(); return decode(prayersRaw) }

    val current: PrayerAlerts.Settings
        get() { load(); return PrayerAlerts.Settings(enabled, decode(prayersRaw), lead) }

    fun saveEnabled(v: Boolean) { load(); enabled = v; sp.edit().putBoolean(ENABLED, v).apply() }
    fun savePrayers(v: Set<Prayer>) { load(); prayersRaw = encode(v); sp.edit().putString(PRAYERS, prayersRaw).apply() }
    fun saveLead(v: Int) { load(); lead = v; sp.edit().putInt(LEAD, v).apply() }
    fun saveSound(v: String) { load(); sound = v; sp.edit().putString(SOUND, v).apply() }

    fun decode(raw: String): Set<Prayer> =
        raw.split(",").mapNotNull { k -> Prayer.entries.firstOrNull { it.key == k } }.toSet()

    fun encode(set: Set<Prayer>): String = Prayer.entries.filter { it in set }.joinToString(",") { it.key }
}
