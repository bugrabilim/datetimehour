package tr.bumba.time

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Uygulama içinde dil seçimi (3 Kasım 2026, 2. analiz #5; sahibin ekran görüntüsü): bayrak ve dilin kendi adıyla 15 dil
 * ve "Sistem dili"; telefonun ayarlarına gitmeden. Android 13 ve sonrasında sistemin uygulama dili (LocaleManager;
 * telefon Ayarları › Uygulama dili ile aynı kayıt), daha eskilerde `app.lang` kaydı ve uygulama bağlamının dili.
 * iOS'taki AppLanguage ile aynı sıra ve adlar.
 */
object AppLanguage {
    const val KEY = "app.lang"

    data class Option(val code: String, val flag: String, val native: String)

    /** Dilin kendi adıyla (her arayüz dilinde aynı görünür) */
    val ALL = listOf(
        Option("tr", "🇹🇷", "Türkçe"), Option("en", "🇬🇧", "English"), Option("de", "🇩🇪", "Deutsch"),
        Option("az", "🇦🇿", "Azərbaycanca"), Option("ar", "🇸🇦", "العربية"), Option("id", "🇮🇩", "Bahasa Indonesia"),
        Option("ur", "🇵🇰", "اردو"), Option("fa", "🇮🇷", "فارسی"), Option("fr", "🇫🇷", "Français"),
        Option("ru", "🇷🇺", "Русский"), Option("ja", "🇯🇵", "日本語"), Option("ko", "🇰🇷", "한국어"),
        Option("zh", "🇨🇳", "简体中文"), Option("fil", "🇵🇭", "Filipino"), Option("th", "🇹🇭", "ไทย"),
        Option("es", "🇪🇸", "Español"), Option("pt", "🇧🇷", "Português"), Option("hi", "🇮🇳", "हिन्दी"),
        Option("bn", "🇧🇩", "বাংলা"),
    )

    private fun tag(code: String) = if (code == "zh") "zh-Hans" else code

    /** Seçili dil kodu; boş = sistem dili */
    fun current(c: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val l = c.getSystemService(LocaleManager::class.java)?.applicationLocales ?: return ""
            if (l.isEmpty) return ""
            return l[0].language.let { when (it) { "in" -> "id"; "tl" -> "fil"; else -> it } }
        }
        return Prefs.store.getString(KEY, "") ?: ""
    }

    fun set(a: Activity, code: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            // Sistem etkinlikleri kendisi yeniden kurar
            a.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (code.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag(code))
            return
        }
        Prefs.store.edit().putString(KEY, code).commit()
        Strings.init(a.application)
        a.recreate()
    }

    /** Android 13 öncesi: seçilen dille bağlam (Strings ve etkinliklerin attachBaseContext'i) */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val code = base.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        if (code.isEmpty()) return base
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(Locale.forLanguageTag(tag(code)))
        return base.createConfigurationContext(cfg)
    }
}

/** Ayarlar › Dil */
@Composable
fun LanguageScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val current = AppLanguage.current(context)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppCard(padding = 4) {
            LanguageRow(null, L("lang.system"), current.isEmpty()) { activity?.let { AppLanguage.set(it, "") } }
            AppLanguage.ALL.forEach { o ->
                HorizontalDivider(color = Theme.c.border.copy(alpha = 0.4f))
                LanguageRow(o.flag, o.native, current == o.code) { activity?.let { AppLanguage.set(it, o.code) } }
            }
        }
        Text(L("lang.note"), color = Theme.c.muted, fontSize = 12.sp)
    }
}

@Composable
private fun LanguageRow(flag: String?, name: String, on: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        // Sistem dili: bayrak yerine iki tonlu simge (8 Kasım 2026)
        if (flag == null) DuoIcon("language", 26.dp, Theme.c.sage) else Text(flag, fontSize = 22.sp)
        Spacer(Modifier.width(14.dp))
        Text(name, Modifier.weight(1f), color = if (on) Theme.c.accent else Theme.c.text, fontSize = 16.sp,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
        if (on) Icon(Icons.Filled.Check, null, tint = Theme.c.accent)
    }
}

/** Ayarlardaki satır: şu anki dil */
@Composable
fun LanguageSettingsRow(onClick: () -> Unit) {
    val context = LocalContext.current
    val code = AppLanguage.current(context)
    val name = AppLanguage.ALL.firstOrNull { it.code == code }?.let { "${it.flag}  ${it.native}" } ?: L("lang.system")
    AppCard(padding = 4) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            DuoChip("language", Theme.c.sage)
            Spacer(Modifier.width(14.dp))
            Text(L("lang.title"), Modifier.weight(1f), color = Theme.c.text, fontSize = 16.sp)
            Text(name, color = Theme.c.muted, fontSize = 14.sp)
        }
    }
}
