package tr.bumba.time

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Kullanıcı tercihleri (iOS'taki UserDefaults anahtarlarıyla aynı adlar). Compose durumu olarak tutulur;
 * değişince ilgili ekranlar yeniden çizilir ve değer kalıcı olarak yazılır.
 */
object Prefs {
    private lateinit var sp: SharedPreferences

    /** Ekranlara özgü küçük kayıtlar için (araçlar, bildirim tercihleri…): aynı "prefs" dosyası */
    val store: SharedPreferences get() = sp

    /**
     * Google yedeğinden yeni telefona geri yüklenen kayıtlarda cihaza özgü olanlar silinir: bildirim izni sorusu
     * (izin yeni telefonda yok) ve widget yer seçimleri (widget kimlikleri cihaza özgü). Yalnız ilk kurulumda bir kez
     * (geri yükleme kurulumla olur); uygulama güncellemesinde dokunulmaz. İşaret yedeğe girmeyen klasörde durur.
     */
    private fun clearRestoredDeviceKeys(c: Context) {
        val marker = java.io.File(c.noBackupFilesDir, "installed")
        if (marker.exists()) return
        val info = runCatching { c.packageManager.getPackageInfo(c.packageName, 0) }.getOrNull()
        val fresh = info == null || info.firstInstallTime == info.lastUpdateTime
        if (fresh && sp.all.isNotEmpty()) {
            val e = sp.edit().remove("notify.asked")
            sp.all.keys.filter { it.startsWith("widget.place.") || it.startsWith("alarm.snoozed.") }.forEach { e.remove(it) }
            e.apply()
        }
        runCatching { marker.createNewFile() }
    }

    fun init(c: Context) {
        sp = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        clearRestoredDeviceKeys(c)
        Features.load(sp) // ilk iş: önceki kullanıcı mı diye dosyanın boş olup olmadığına bakar
        accent = sp.getString("accent", AccentPalette.DEFAULT_ID)!!
        appearance = sp.getString("appearance", "system")!!
        h12 = sp.getBoolean("h12", false)
        keepAwake = sp.getBoolean("keepAwake", true)
        secD = sp.getBoolean("secD", true)
        secA = sp.getBoolean("secA", true) // varsayılan: analogda saniye açık (30 Ekim 2026)
        clockModel = sp.getInt("clockModel", ClockModel.CLASSIC.ordinal) // varsayılan: rakamlı analog (30 Ekim 2026)
        // Seçim sıra numarasıyla saklanır; 30 Ekim 2026'da ilk iki model yer değiştirdi (classic 1. sıraya geldi):
        // eski kayıt bir kez çevrilir ki kullanıcının seçtiği model değişmesin.
        if (sp.getInt("clockModelOrder", 1) < 2) {
            if (sp.contains("clockModel") && clockModel in 0..1) clockModel = 1 - clockModel
            sp.edit().putInt("clockModel", clockModel).putInt("clockModelOrder", 2).apply()
        }
        province = sp.getString("province", "istanbul")!!
        savedPlaces = sp.getString("places.saved", "")!!.split("|").filter { it.isNotEmpty() }
        clockFont = sp.getString("clockFont", "rounded")!!
        fsPhoto = sp.getLong("fsPhoto", 0L)
        fsBg = sp.getString(FullscreenBackground.KEY, null)
        nightMode = sp.getString(NightMode.KEY, "off")!!
        FsLook.load()
        world = sp.getString("world", City.DEFAULT_KEYS)!!
        units = sp.getString("units", null) ?: Units.systemDefault
        BadgeStore.load()
    }

    var accent by mutableStateOf(AccentPalette.DEFAULT_ID)
        private set
    var appearance by mutableStateOf("system")
        private set
    var h12 by mutableStateOf(false)
        private set
    var keepAwake by mutableStateOf(true)
        private set
    /** Dijital ve analog saatte saniye (ikisinde de varsayılan açık; analog 30 Ekim 2026'dan beri) */
    var secD by mutableStateOf(true)
        private set
    var secA by mutableStateOf(true)
        private set
    var clockModel by mutableStateOf(ClockModel.CLASSIC.ordinal)
        private set
    /** İl adı ya da "auto" (konumum) */
    var province by mutableStateOf("istanbul")
        private set
    /** Dünya saatleri: şehir anahtarları, virgülle */
    var world by mutableStateOf(City.DEFAULT_KEYS)
        private set

    /** Ölçü birimleri: "metric" ya da "imperial" (seçim yoksa bölgeye göre) */
    var units by mutableStateOf("metric")
        private set

    fun saveUnits(v: String) { units = v; sp.edit().putString("units", v).apply() }
    fun saveAccent(v: String) { accent = v; sp.edit().putString("accent", v).apply() }
    fun saveAppearance(v: String) { appearance = v; sp.edit().putString("appearance", v).apply() }
    fun saveH12(v: Boolean) { h12 = v; sp.edit().putBoolean("h12", v).apply() }
    fun saveKeepAwake(v: Boolean) { keepAwake = v; sp.edit().putBoolean("keepAwake", v).apply() }

    /** Saat yazı tipi ve tam ekran fotoğrafının sürümü (0 = yok) */
    var clockFont by mutableStateOf("rounded")
        private set
    fun saveClockFont(v: String) { clockFont = v; sp.edit().putString("clockFont", v).apply() }
    var fsPhoto by mutableStateOf(0L)
        private set
    fun saveFsPhoto(v: Long) { fsPhoto = v; sp.edit().putLong("fsPhoto", v).apply() }
    /** Tam ekran arka planı (FullscreenBackground; null = fotoğraf varsa fotoğraf, yoksa düz) */
    var fsBg by mutableStateOf<String?>(null)
        private set
    fun saveFsBg(v: String) { fsBg = v; sp.edit().putString(FullscreenBackground.KEY, v).apply() }
    /** Gece / yatak başı modu (NightMode): off, on, auto */
    var nightMode by mutableStateOf("off")
        private set
    fun saveNightMode(v: String) { nightMode = v; sp.edit().putString(NightMode.KEY, v).apply() }
    fun saveSecD(v: Boolean) { secD = v; sp.edit().putBoolean("secD", v).apply() }
    fun saveSecA(v: Boolean) { secA = v; sp.edit().putBoolean("secA", v).apply() }
    fun saveClockModel(v: Int) { clockModel = v; sp.edit().putInt("clockModel", v).apply() }
    fun saveProvince(v: String) { province = v; sp.edit().putString("province", v).apply() }

    /** Kayıtlı yerler (il ya da ilçe kimlikleri, en çok 8; "|" ile ayrılır) */
    var savedPlaces by mutableStateOf(emptyList<String>())
        private set
    fun saveSavedPlaces(v: List<String>) {
        savedPlaces = v.distinct().take(8)
        sp.edit().putString("places.saved", savedPlaces.joinToString("|")).apply()
    }
    fun saveWorld(v: String) { world = v; sp.edit().putString("world", v).apply() }

    // Otomatik konumun son sonucu (2 ondalık; telefonda kalır)
    fun autoLat(): Double? = if (sp.contains("auto.lat")) sp.getFloat("auto.lat", 0f).toDouble() else null
    fun autoLon(): Double = sp.getFloat("auto.lon", 0f).toDouble()
    fun autoZone(): String = sp.getString("auto.tz", "Europe/Istanbul")!!
    fun autoRef(): String = sp.getString("auto.ref", "")!!
    fun saveAuto(lat: Double, lon: Double, zone: String, ref: String) {
        sp.edit().putFloat("auto.lat", lat.toFloat()).putFloat("auto.lon", lon.toFloat())
            .putString("auto.tz", zone).putString("auto.ref", ref).apply()
    }
}
