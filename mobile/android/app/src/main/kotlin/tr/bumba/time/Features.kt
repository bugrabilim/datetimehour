package tr.bumba.time

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Alt bar yerine özellik aç/kapa (30 Ekim 2026, sahibin kararı): ilk açılışta yalnız Saat; Ayarlar › Özellikler'den
 * Hava, Takvim, Dünya, Mevsimler, Vakitler ve Araçlar tek tek açılır. 3 Kasım 2026'dan beri (Seçenek B) alt çubukta
 * Saat, kullanıcının seçtiği en çok 3 özellik ve "Tümü" durur; yalnız Saat açıksa çubuk yok (Ayarlar dişliden açılır).
 * Kayıt `features` (virgülle, iOS ile aynı). Kayıt yoksa: önceki sürümü kullananlarda hepsi açık (göç), yeni kurulumda boş.
 */
object Features {
    const val KEY = "features"
    /** Alt çubuktaki özellikler (en çok 3, sırayla; 2. analiz #194; iOS ile aynı) */
    const val BAR_KEY = "features.bar"
    const val BAR_MAX = 3
    val OPTIONAL = listOf(Tab.WEATHER, Tab.CALENDAR, Tab.WORLD, Tab.SEASONS, Tab.PRAYER, Tab.TOOLS)
    /** Alt çubuğa girebilen özellikler (Araçlar her zaman "Tümü"nde) */
    val BAR_OPTIONS = listOf(Tab.WEATHER, Tab.CALENDAR, Tab.WORLD, Tab.SEASONS, Tab.PRAYER)
    /** Birden çok özellik aynı anda açılınca (ilk kurulum, göç) çubuğa giriş sırası */
    private val BAR_DEFAULT = listOf(Tab.PRAYER, Tab.WEATHER, Tab.CALENDAR, Tab.WORLD, Tab.SEASONS)

    var enabled by mutableStateOf(emptySet<Tab>())
        private set
    var bar by mutableStateOf(emptyList<Tab>())
        private set

    /** "weather", "calendar"… (iOS'taki AppTab.key) */
    fun id(t: Tab) = t.key.removePrefix("tab.")

    fun parse(raw: String): Set<Tab> = raw.split(",").mapNotNull { k -> OPTIONAL.firstOrNull { id(it) == k } }.toSet()

    fun parseBar(raw: String): List<Tab> = raw.split(",").mapNotNull { k -> BAR_OPTIONS.firstOrNull { id(it) == k } }.distinct().take(BAR_MAX)

    /** Prefs.init'in başında, başka bir şey yazılmadan: dosyada anahtar varsa uygulama daha önce kullanılmıştır */
    fun load(sp: SharedPreferences) {
        var fresh = false
        if (!sp.contains(KEY)) {
            val existing = sp.all.isNotEmpty()
            fresh = !existing
            sp.edit().putString(KEY, if (existing) OPTIONAL.joinToString(",") { id(it) } else "").apply()
        }
        enabled = parse(sp.getString(KEY, "")!!)
        // Göç (Seçenek B): çubuk kaydı yoksa açık özelliklerden varsayılan sırayla en çok 3
        if (!sp.contains(BAR_KEY)) {
            val b = BAR_DEFAULT.filter { it in enabled }.take(BAR_MAX)
            sp.edit().putString(BAR_KEY, b.joinToString(",") { id(it) }).apply()
        }
        bar = parseBar(sp.getString(BAR_KEY, "")!!).filter { it in enabled }
        // Yeni kurulum: ilk açılışta kurulum sihirbazı (Onboarding; 2. analiz #4) ve "Gün ışığı" atmosferi (doğal tasarım
        // madde 7; önceki kullanıcının seçimi korunur)
        if (fresh && !sp.contains(Atmosphere.KEY)) sp.edit().putString(Atmosphere.KEY, Atmosphere.DAYLIGHT).apply()
        Onboarding.load(fresh)
    }

    fun isOn(t: Tab) = t == Tab.CLOCK || t in enabled

    fun set(t: Tab, on: Boolean) = save(if (on) enabled + t else enabled - t)

    /** Açık özellikleri yazar; yeni açılan özellik çubukta yer varsa kendiliğinden girer, kapanan çıkar */
    fun save(v: Set<Tab>) {
        val added = v - enabled
        enabled = v
        Prefs.store.edit().putString(KEY, OPTIONAL.filter { it in v }.joinToString(",") { id(it) }).apply()
        var b = bar.filter { it in v }
        for (t in BAR_DEFAULT) if (t in added && t !in b && b.size < BAR_MAX) b = b + t
        saveBar(b)
    }

    fun saveBar(list: List<Tab>) {
        bar = list.filter { it in BAR_OPTIONS }.distinct().take(BAR_MAX)
        Prefs.store.edit().putString(BAR_KEY, bar.joinToString(",") { id(it) }).apply()
    }

    /** "Alt çubukta" işareti; çubuk doluysa false döner */
    fun toggleBar(t: Tab): Boolean {
        if (t in bar) { saveBar(bar - t); return true }
        if (bar.size >= BAR_MAX || t !in BAR_OPTIONS || !isOn(t)) return false
        saveBar(bar + t)
        return true
    }

    fun moveBar(t: Tab, delta: Int) {
        val i = bar.indexOf(t)
        val j = i + delta
        if (i < 0 || j !in bar.indices) return
        val m = bar.toMutableList()
        m[i] = m[j].also { m[j] = m[i] }
        saveBar(m)
    }

    /**
     * Alt çubuk (Seçenek B, 2. analiz #194): Saat her zaman ilk, "Tümü" (Tab.TOOLS) her zaman son, aradakiler kullanıcının
     * seçtiği en çok 3 özellik. Hiçbir özellik açık değilse çubuk yok. Çubukta olmayan bir özellik bildirimden, widget'tan ya da
     * Tümü'nden açılırsa çubuk değişmez; sayfa üstte geri düğmesiyle açılır (2. analiz #203).
     */
    fun barTabs(): List<Tab> =
        // Tümü her zaman çubukta (4 Kasım 2026: Ayarlar saat ekranındaki dişliden Tümü'ye taşındı)
        if (enabled.isEmpty()) listOf(Tab.CLOCK, Tab.TOOLS)
        else listOf(Tab.CLOCK) + bar.filter { isOn(it) }.take(BAR_MAX) + Tab.TOOLS

    /** Çubuktaki ad: Araçlar sekmesi "Tümü" olarak görünür */
    fun label(t: Tab) = if (t == Tab.TOOLS) L("tab.all") else L(t.key)
}

/** Ayarlar › Özellikler: Saat her zaman açık, diğerleri anahtarla; açık özelliklerde "Alt çubukta" işareti ve sıra (#194) */
@Composable
fun FeaturesScreen() {
    var full by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Canlı alt çubuk önizlemesi ve sıra
        Text(L("features.barTitle"), color = Theme.c.text, fontSize = if (AppFonts.serif) 19.sp else 18.sp, fontFamily = AppFonts.heading, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 4.dp))
        AppCard {
            BarPreview()
            for ((i, t) in Features.bar.withIndex()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Theme.c.border.copy(alpha = 0.5f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}. " + L(t.key), Modifier.weight(1f), color = Theme.c.text, fontSize = 15.sp)
                    IconButton(onClick = { Features.moveBar(t, -1) }, enabled = i > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, L("features.moveUp"), tint = if (i > 0) Theme.c.accent else Theme.c.border)
                    }
                    IconButton(onClick = { Features.moveBar(t, 1) }, enabled = i < Features.bar.size - 1) {
                        Icon(Icons.Filled.KeyboardArrowDown, L("features.moveDown"), tint = if (i < Features.bar.size - 1) Theme.c.accent else Theme.c.border)
                    }
                }
            }
        }
        Text(L("features.barNote"), color = Theme.c.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
        AppCard(padding = 4) {
            FeatureRow(Tab.CLOCK, checked = true, enabled = false) { }
            for (t in Features.OPTIONAL) {
                HorizontalDivider(Modifier.padding(start = 58.dp), color = Theme.c.border.copy(alpha = 0.5f))
                FeatureRow(t, Features.isOn(t), onBar = if (t in Features.BAR_OPTIONS && Features.isOn(t)) {
                    { if (!Features.toggleBar(t)) full = true }
                } else null) { Features.set(t, it) }
            }
        }
        if (full) Text(L("features.barFull"), color = Theme.c.err, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
        LaunchedEffect(full) { if (full) { kotlinx.coroutines.delay(3000); full = false } }
        Text(L("features.note"), color = Theme.c.muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun FeatureRow(t: Tab, checked: Boolean, enabled: Boolean = true, onBar: (() -> Unit)? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        DuoChip(LineIconMap.tab(t), LineIconMap.tabColor(t))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(L(t.key), color = Theme.c.text, fontSize = 16.sp)
            Text(L("features.${Features.id(t)}.note"), color = Theme.c.muted, fontSize = 12.sp)
            if (onBar != null) {
                val inBar = t in Features.bar
                FilterChip(
                    selected = inBar, onClick = onBar, label = { Text(L("features.bar"), fontSize = 12.sp) },
                    leadingIcon = if (inBar) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Theme.c.accentSoft, selectedLabelColor = Theme.c.text),
                    modifier = Modifier.height(30.dp),
                )
            }
        }
        SoftSwitch(checked, onChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
    }
}

/** Canlı alt çubuk önizlemesi (Özellikler ve ilk kurulum; yalnız Saat açıksa tek simge) */
@Composable
fun BarPreview(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        for ((i, t) in Features.barTabs().withIndex()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                // İki tonlu simge (alt çubuktaki gibi; ilk sekme seçili görünür)
                DuoIcon(LineIconMap.tab(t), 24.dp, if (i == 0) Theme.c.accent else Theme.c.muted)
                Text(Features.label(t), color = Theme.c.text, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}
