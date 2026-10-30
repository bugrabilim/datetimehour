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

/**
 * Alt bar yerine özellik aç/kapa (30 Ekim 2026, sahibin kararı): ilk açılışta yalnız Saat; Ayarlar › Özellikler'den
 * Hava, Takvim, Dünya, Mevsimler, Vakitler ve Araçlar tek tek açılır. Alt bar yalnız açık özellikleri gösterir,
 * yalnız Saat açıksa hiç görünmez (Ayarlar saat ekranındaki dişliden açılır). Kapalı bir özellik widget'tan,
 * bildirimden ya da saat ekranındaki bir alandan açılırsa o an geçici olarak barda görünür.
 * Kayıt `features` (virgülle, iOS ile aynı). Kayıt yoksa: önceki sürümü kullananlarda hepsi açık (göç), yeni kurulumda boş.
 */
object Features {
    const val KEY = "features"
    val OPTIONAL = listOf(Tab.WEATHER, Tab.CALENDAR, Tab.WORLD, Tab.SEASONS, Tab.PRAYER, Tab.TOOLS)

    var enabled by mutableStateOf(emptySet<Tab>())
        private set

    /** "weather", "calendar"… (iOS'taki AppTab.key) */
    fun id(t: Tab) = t.key.removePrefix("tab.")

    fun parse(raw: String): Set<Tab> = raw.split(",").mapNotNull { k -> OPTIONAL.firstOrNull { id(it) == k } }.toSet()

    /** Prefs.init'in başında, başka bir şey yazılmadan: dosyada anahtar varsa uygulama daha önce kullanılmıştır */
    fun load(sp: SharedPreferences) {
        if (!sp.contains(KEY)) {
            val existing = sp.all.isNotEmpty()
            sp.edit().putString(KEY, if (existing) OPTIONAL.joinToString(",") { id(it) } else "").apply()
        }
        enabled = parse(sp.getString(KEY, "")!!)
    }

    fun isOn(t: Tab) = t == Tab.CLOCK || t in enabled

    fun set(t: Tab, on: Boolean) = save(if (on) enabled + t else enabled - t)

    fun save(v: Set<Tab>) {
        enabled = v
        Prefs.store.edit().putString(KEY, OPTIONAL.filter { it in v }.joinToString(",") { id(it) }).apply()
    }

    /** Alt bardaki sekmeler: Saat, açık özellikler ve kapalı olsa da şu an açık olan sekme (tek sekme kalırsa bar yok) */
    fun barTabs(current: Tab) = Tab.entries.filter { isOn(it) || it == current }
}

/** Ayarlar › Özellikler: Saat her zaman açık, diğerleri anahtarla */
@Composable
fun FeaturesScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard(padding = 4) {
            FeatureRow(Tab.CLOCK, checked = true, enabled = false) { }
            for (t in Features.OPTIONAL) {
                HorizontalDivider(Modifier.padding(start = 50.dp), color = Theme.c.border.copy(alpha = 0.5f))
                FeatureRow(t, Features.isOn(t)) { Features.set(t, it) }
            }
        }
        Text(L("features.note"), color = Theme.c.muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun FeatureRow(t: Tab, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(t.on, null, tint = Theme.c.accent)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(L(t.key), color = Theme.c.text, fontSize = 16.sp)
            Text(L("features.${Features.id(t)}.note"), color = Theme.c.muted, fontSize = 12.sp)
        }
        Switch(checked, onChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = Theme.c.accent))
    }
}
