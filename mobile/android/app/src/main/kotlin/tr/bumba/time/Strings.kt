package tr.bumba.time

import android.annotation.SuppressLint
import android.content.Context
import java.util.Locale

/**
 * Uygulama metinleri: iOS'taki anahtarlarla aynı ("nature.h1" → R.string.nature_h1). Metinler sitedeki
 * i18n dosyalarından ve mobile/shared/app-strings.json'dan gen-strings.mjs ile üretilir. Anahtar çoğu zaman
 * çalışma anında oluştuğu için (ör. "calendar.months.$i") adla okunur; bu yüzden kaynak küçültme kapalıdır.
 */
object Strings {
    private lateinit var context: Context
    private val cache = HashMap<String, Int>()

    /** Uygulama bağlamı (varlık dosyalarını okumak için; dünya saatleri yer araması) */
    val appContext: Context get() = context

    fun init(c: Context) {
        // Android 13 öncesinde uygulama içi dil seçimi bağlamın dilini değiştirir (AppLanguage, 2. analiz #5)
        context = AppLanguage.wrap(c.applicationContext)
        cache.clear()
    }

    /** Uygulamanın dilleri (gen-strings.mjs ile aynı); sağdan sola yazılanlar */
    val LANGS = listOf("tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th", "es", "pt", "hi", "bn")
    val RTL = listOf("ar", "ur", "fa")

    /** Uygulamanın dili: telefonun dil listesinde desteklenen ilk dil (Android'in metin seçimiyle aynı), hiçbiri yoksa
     *  İngilizce (values/ İngilizce; 9 Kasım 2026, sahibin kararı). Java eski Endonezce ("in") ve Tagalog ("tl") kodlarını
     *  verebilir (Filipince "fil"). */
    val lang: String
        get() {
            val ls = context.resources.configuration.locales
            for (i in 0 until ls.size()) {
                val l = ls[i].language.let { when (it) { "in" -> "id"; "tl" -> "fil"; else -> it } }
                if (l in LANGS) return l
            }
            return "en"
        }

    /** Bengalcede Latin rakamı (Bengalce yerel ayarının varsayılanı Bengal rakamları) */
    val locale: Locale get() = Locale.forLanguageTag(when (lang) { "zh" -> "zh-CN"; "pt" -> "pt-BR"; "bn" -> "bn-BD-u-nu-latn"; else -> lang })
    val isRtl: Boolean get() = lang in RTL

    /** Sayılar her dilde Latin rakamıyla: sağdan sola dillerde (Arapça/Farsça rakam vermesin diye) ABD biçimi */
    val numberLocale: Locale get() = if (isRtl) Locale.US else locale

    /** Ondalık ayırıcı: İngilizce, Doğu ve Güneydoğu Asya dillerinde ve sağdan sola dillerde nokta, diğerlerinde virgül */
    val decimalPoint: String get() = if (lang in listOf("en", "ja", "ko", "zh", "fil", "th", "hi", "bn") || isRtl) "." else ","

    @SuppressLint("DiscouragedApi")
    fun id(key: String): Int = cache.getOrPut(key) {
        context.resources.getIdentifier(key.replace(Regex("[^A-Za-z0-9_]"), "_"), "string", context.packageName)
    }

    fun get(key: String): String {
        val id = id(key)
        return if (id == 0) key else context.getString(id)
    }

    fun format(key: String, vararg args: Any): String {
        val id = id(key)
        return if (id == 0) key else context.getString(id, *args.map { it.toString() }.toTypedArray())
    }
}

/** Metin (iOS'taki L ile aynı) */
fun L(key: String): String = Strings.get(key)

/** Yer tutuculu metin: "{n} gün kaldı" gibi; değerler sırayla */
fun T(key: String, vararg args: Any): String = Strings.format(key, *args)
