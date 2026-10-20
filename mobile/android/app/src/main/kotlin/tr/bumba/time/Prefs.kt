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

    fun init(c: Context) {
        sp = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        accent = sp.getString("accent", AccentPalette.DEFAULT_ID)!!
        appearance = sp.getString("appearance", "system")!!
        h12 = sp.getBoolean("h12", false)
        keepAwake = sp.getBoolean("keepAwake", true)
        secD = sp.getBoolean("secD", true)
        secA = sp.getBoolean("secA", false)
        clockModel = sp.getInt("clockModel", 0)
        province = sp.getString("province", "istanbul")!!
        world = sp.getString("world", City.DEFAULT_KEYS)!!
        units = sp.getString("units", null) ?: Units.systemDefault
    }

    var accent by mutableStateOf(AccentPalette.DEFAULT_ID)
        private set
    var appearance by mutableStateOf("system")
        private set
    var h12 by mutableStateOf(false)
        private set
    var keepAwake by mutableStateOf(true)
        private set
    /** Dijital saatte saniye (varsayılan açık), analogda (varsayılan kapalı) */
    var secD by mutableStateOf(true)
        private set
    var secA by mutableStateOf(false)
        private set
    var clockModel by mutableStateOf(0)
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
    fun saveSecD(v: Boolean) { secD = v; sp.edit().putBoolean("secD", v).apply() }
    fun saveSecA(v: Boolean) { secA = v; sp.edit().putBoolean("secA", v).apply() }
    fun saveClockModel(v: Int) { clockModel = v; sp.edit().putInt("clockModel", v).apply() }
    fun saveProvince(v: String) { province = v; sp.edit().putString("province", v).apply() }
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
