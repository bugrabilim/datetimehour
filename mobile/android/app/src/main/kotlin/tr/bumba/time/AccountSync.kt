package tr.bumba.time

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Hesapla eşitleme (sahibin kararı S1, 12 Kasım 2026: kayıtlar hem hesapta hem iCloud'da; Android'de yalnız hesapta).
 * Eşitlenen anahtarlar iOS'taki CloudSync.keys ile aynıdır; değer JSON ilkel değeridir ("true", "5", "\"metin\"") ki
 * iPhone ve site aynı kaydı okusun. Anahtar başına son yazan kazanır: sunucu `t` (Unix ms) tutar, cihaz son eşitlenen
 * değerin özetini ve zamanını tutar ("account" dosyasında, yedeğe girmez).
 * Android okuma kodu her anahtarı tek türle okur (getInt, getBoolean…); yanlış türle yazmak çöker. Bu yüzden her
 * anahtarın türü [TYPES]'ta sabittir (12 Kasım 2026'da koddan çıkarıldı); yeni anahtar eklenirken türü de yazılır.
 * Rozetler (madde 13): bu cihazda kayıt olunan hesaba ilk girişte sormadan geçer; var olan hesaba girişte bir kez sorulur;
 * hesaptaki rozetler cihaza gelir. Rozet ekranında "Bu cihazdaki rozetleri hesabıma ekle".
 */
object AccountSync {
    enum class T { S, I, B, F }

    val TYPES: Map<String, T> = buildMap {
        val s = "metro classroom talking.every appearance accent units clockFont digital.style world.view qibla.style qibla.favs homeItems " +
            "features features.bar all.favs cal.mode cal.country cal.hidden cal.extra fsBg nightMode fs.text world places.saved countdowns " +
            "hiit.cfg hiit.routine hiit.log world.labels timers.visual routines timers.history journal meet.cities prayer.method " +
            "pomo.log pomo.todo study.log study.subjects wake.events pomo.task pomo.tag pomo.noise sleep.days sleep.noise quran.bookmark " +
            "zikir.kind track.prayer track.kaza track.fast zakat.values zakat.std track.when track.pauses track.pausedFrom track.kazaLog " +
            "track.life quran.hatim quran.read quran.theme dua.favs zikir.custom prayer.visual hijri.cal meet.h12 atmo night.color " +
            "dial.lume look.digitColor flip.card"
        val i = "clockModel fsSlideEvery pomo.goal pomo.vol pomo.work pomo.short pomo.long pomo.rounds sleep.wake sleep.dur sleep.remind " +
            "sleep.vol sleep.timer quran.size zikir.target zikir.total quran.goal hijri.offset fontScale night.from night.to"
        val b = "talking.chime h12 keepAwake secA secD cal.hijriNav tools.voice hiit.cue.next hiit.cue.half hiit.cue.ten timers.keypad " +
            "prayer.hanafi sleep.on quran.en zikir.tesbihat track.inline zikir.beads zikir.shake prayer.words meet.exchanges look.outline " +
            "flip.pair flip.smallSec flip.hinge"
        s.split(" ").forEach { put(it, T.S) }
        i.split(" ").forEach { put(it, T.I) }
        b.split(" ").forEach { put(it, T.B) }
        put("fs.scale", T.F)
    }

    var busy by mutableStateOf(false)
        private set
    /** Var olan hesaba girişte sorulacak rozet sayısı (0: soru yok) */
    var askBadges by mutableIntStateOf(0)
    var accountBadges by mutableIntStateOf(0)
        private set
    var records by mutableIntStateOf(0)
        private set

    private val store get() = Prefs.store
    private val state get() = stateFile
    private var stateFile: android.content.SharedPreferences? = null
    fun init(c: android.content.Context) { stateFile = c.getSharedPreferences("account", android.content.Context.MODE_PRIVATE) }

    /** Cihazdaki değer JSON ilkel değeri olarak; yoksa null */
    internal fun encode(key: String): String? {
        if (!Prefs.ready || !store.contains(key)) return null
        return runCatching {
            when (TYPES[key]) {
                T.S -> JSONObject.quote(store.getString(key, null) ?: return null)
                T.I -> store.getInt(key, 0).toString()
                T.B -> store.getBoolean(key, false).toString()
                T.F -> store.getFloat(key, 0f).toString()
                null -> null
            }
        }.getOrNull()
    }

    /** Sunucudaki değeri cihaza yazar; tür tutmazsa (bozuk kayıt) yazmaz */
    internal fun apply(key: String, json: String?) {
        val e = store.edit()
        if (json == null) { e.remove(key).apply(); return }
        val ok = runCatching {
            val v = JSONArray("[$json]").get(0)
            when (TYPES[key]) {
                T.S -> e.putString(key, v as? String ?: return)
                T.I -> e.putInt(key, (v as? Number)?.toInt() ?: return)
                T.B -> e.putBoolean(key, v as? Boolean ?: return)
                T.F -> e.putFloat(key, (v as? Number)?.toFloat() ?: return)
                null -> return
            }
            true
        }.getOrDefault(false)
        if (ok) e.apply()
    }

    private fun hash(v: String?): String = if (v == null) "-" else
        MessageDigest.getInstance("SHA-256").digest(v.toByteArray()).take(12).joinToString("") { "%02x".format(it) }

    private fun synced(): JSONObject = runCatching { JSONObject(state?.getString("sync", null) ?: "{}") }.getOrDefault(JSONObject())

    /** Çıkışta ve silmede eşitleme durumu unutulur (cihazdaki kayıtlar kalır) */
    fun forget() { state?.edit()?.remove("sync")?.remove("syncUser")?.apply(); accountBadges = 0; records = 0; askBadges = 0 }

    /**
     * Kayıtlar ve rozetler. [first]: girişten hemen sonra; o hesapla ilk eşitlemede sunucuda olan değer kazanır, sunucuda
     * olmayan cihaz değeri gönderilir. Sonraki eşitlemelerde değişen taraf kazanır, ikisi de değiştiyse cihaz (son yazan).
     */
    suspend fun sync(first: Boolean = false, now: Long = System.currentTimeMillis()): Boolean {
        if (!Account.signedIn || !Prefs.ready || busy) return false
        busy = true
        try {
            val uid = Account.user?.id ?: return false
            var map = synced()
            val fresh = first || state?.getString("syncUser", null) != uid
            if (fresh) map = JSONObject()
            val r = Account.call("GET", "/v1/kayitlar", auth = true)
            if (r.code != 200) return false
            val server = JSONObject(r.body).optJSONObject("kayitlar") ?: JSONObject()
            val push = JSONObject()
            for (key in TYPES.keys) {
                val local = encode(key)
                val last = map.optJSONObject(key)
                val srv = server.optJSONObject(key)
                val srvV = srv?.let { if (it.isNull("v")) null else it.optString("v") }
                val srvT = srv?.optLong("t") ?: 0L
                if (fresh) {
                    if (srv != null) { if (srvV != local) apply(key, srvV); map.put(key, JSONObject().put("h", hash(srvV)).put("t", srvT)) }
                    else if (local != null) { push.put(key, JSONObject().put("v", local).put("t", now)); map.put(key, JSONObject().put("h", hash(local)).put("t", now)) }
                    continue
                }
                val localChanged = hash(local) != (last?.optString("h") ?: "-")
                val serverChanged = srv != null && srvT > (last?.optLong("t") ?: 0L)
                when {
                    localChanged -> { push.put(key, JSONObject().put("v", local ?: JSONObject.NULL).put("t", now)); map.put(key, JSONObject().put("h", hash(local)).put("t", now)) }
                    serverChanged -> { apply(key, srvV); map.put(key, JSONObject().put("h", hash(srvV)).put("t", srvT)) }
                }
            }
            if (push.length() > 0) {
                val p = Account.call("POST", "/v1/kayitlar", JSONObject().put("degisiklik", push), auth = true)
                if (p.code != 200) return false
            }
            state?.edit()?.putString("sync", map.toString())?.putString("syncUser", uid)?.apply()
            records = server.length() + push.keys().asSequence().count { !server.has(it) }
            syncBadges(first)
            return true
        } catch (e: Exception) {
            return false
        } finally {
            busy = false
        }
    }

    private fun localBadges(): Map<String, String> = BadgeStore.saved.r

    private suspend fun syncBadges(first: Boolean) {
        val r = Account.call("GET", "/v1/rozetler", auth = true)
        if (r.code != 200) return
        val arr = JSONObject(r.body).optJSONArray("rozetler") ?: JSONArray()
        val server = (0 until arr.length()).associate { arr.getJSONObject(it).let { o -> o.getString("id") to o.getString("gun") } }
        val missing = localBadges().filter { (id, day) -> server[id] == null || day < server[id]!! }
        val email = Account.user?.email
        val uid = Account.user?.id ?: return
        when {
            missing.isEmpty() -> {}
            first && email != null && email == Account.createdHere -> { addBadges(missing); Account.createdHere = null }
            first && !Account.badgesAsked(uid) -> askBadges = missing.size
        }
        BadgeStore.mergeFromAccount(server)
        accountBadges = maxOf(server.size, accountBadges)
    }

    /** "Bu cihazdaki rozetleri hesabıma ekle" ve sorudaki "Evet" */
    suspend fun addBadges(only: Map<String, String> = localBadges()): Boolean {
        if (only.isEmpty()) return true
        val list = JSONArray(only.map { (id, day) -> JSONObject().put("id", id).put("gun", day) })
        val r = Account.call("POST", "/v1/rozetler", JSONObject().put("rozetler", list), auth = true)
        if (r.code != 200) return false
        accountBadges = JSONObject(r.body).optJSONArray("rozetler")?.length() ?: accountBadges
        Account.user?.id?.let { Account.setBadgesAsked(it) }
        askBadges = 0
        return true
    }

    fun declineBadges() { Account.user?.id?.let { Account.setBadgesAsked(it) }; askBadges = 0 }
}
