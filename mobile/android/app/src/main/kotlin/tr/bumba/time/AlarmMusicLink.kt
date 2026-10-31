package tr.bumba.time

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Alarm müziği bağlantısı (31 Ekim 2026, sahibin kararı: Spotify/YouTube alarm sesi iPhone hariç; Android'de).
 * Alarm çalınca Spotify ya da YouTube bağlantısı kendi uygulamasında açılır, alarmın kendi sesi 1 dakika sonra başlar
 * (AlarmRingService; müzik başlamazsa da uyandırsın diye). Spotify kuruluysa `spotify:<tür>:<kimlik>:play` adresiyle
 * açılır (Spotify'ın kendiliğinden çalması uygulamaya bağlı), değilse web adresi. Telefon kilitliyken Android başka
 * uygulamanın ekranını ancak kilit açılınca gösterir; o yüzden kilitliyse önce kilit açma istenir (AlarmActivity).
 * Ses ya da görüntü alınmaz, kaydedilmez; uygulama yalnız bağlantıyı açar.
 */
object MusicLink {
    const val SPOTIFY = "com.spotify.music"
    const val DELAY_MS = 60_000L
    private val SPOTIFY_WEB = Regex("^https?://open\\.spotify\\.com/(?:intl-[a-z]{2}(?:-[a-zA-Z]{2})?/)?(track|album|playlist|artist|show|episode)/([A-Za-z0-9]{22})")
    private val SPOTIFY_URI = Regex("^spotify:(track|album|playlist|artist|show|episode):([A-Za-z0-9]{22})")
    private val YT_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")

    /** Kayda yazılacak biçim: geçerli Spotify ya da YouTube bağlantısı (boşluklar kırpılır); değilse null, boşsa "" */
    fun normalize(raw: String): String? {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        if (SPOTIFY_URI.containsMatchIn(s) || SPOTIFY_WEB.containsMatchIn(s)) return s.take(300)
        if (Regex("^https?://spotify\\.link/[A-Za-z0-9]+").containsMatchIn(s)) return s.take(300)
        val u = runCatching { Uri.parse(if (s.startsWith("http")) s else "https://$s") }.getOrNull() ?: return null
        val host = u.host?.lowercase() ?: return null
        if (host !in YT_HOSTS) return null
        val ok = when {
            host == "youtu.be" -> u.pathSegments.firstOrNull()?.matches(Regex("[\\w-]{11}")) == true
            u.getQueryParameter("v")?.matches(Regex("[\\w-]{11}")) == true -> true
            u.getQueryParameter("list")?.matches(Regex("[\\w-]{10,64}")) == true -> true
            else -> Regex("^/(shorts|live|embed)/[\\w-]{11}").containsMatchIn(u.path ?: "")
        }
        return if (ok) u.toString().take(300) else null
    }

    /** "spotify" ya da "youtube" (listede gösterilen ad) */
    fun kind(link: String): String = if (link.startsWith("spotify:") || link.contains("spotify.")) "spotify" else "youtube"

    /** Kimlikli Spotify adresi (kendiliğinden çalma için `:play`); kısa bağlantıda null */
    fun spotifyUri(link: String): String? {
        val m = SPOTIFY_URI.find(link) ?: SPOTIFY_WEB.find(link) ?: return null
        return "spotify:${m.groupValues[1]}:${m.groupValues[2]}:play"
    }

    /** Aynı çalmada bir kez açılsın (hizmet ve alarm ekranı ikisi de dener) */
    @Volatile private var lastOpen: Pair<String, Long>? = null

    fun openOnce(c: Context, ringId: String, link: String): Boolean {
        val now = System.currentTimeMillis()
        lastOpen?.let { if (it.first == ringId && now - it.second < 5 * 60_000L) return false }
        val ok = open(c, link)
        if (ok) lastOpen = ringId to now
        return ok
    }

    /** Bağlantıyı açar: Spotify uygulaması, yoksa YouTube/tarayıcı */
    fun open(c: Context, link: String): Boolean {
        val referrer = Uri.parse("android-app://" + c.packageName)
        spotifyUri(link)?.let { uri ->
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(SPOTIFY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(Intent.EXTRA_REFERRER, referrer)
            try { c.startActivity(i); return true } catch (_: ActivityNotFoundException) {} catch (_: SecurityException) {}
        }
        val web = if (link.startsWith("spotify:")) "https://open.spotify.com/" + link.removePrefix("spotify:").split(":").take(2).joinToString("/") else link
        return try {
            c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(Intent.EXTRA_REFERRER, referrer))
            true
        } catch (_: Exception) { false }
    }
}
