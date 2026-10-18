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

    fun init(c: Context) {
        context = c.applicationContext
    }

    /** Uygulamanın dili (tr, en, de, az, ar); desteklenmeyen dilde Türkçe metinler gösterilir */
    val lang: String
        get() {
            val l = context.resources.configuration.locales[0].language
            return if (l in listOf("tr", "en", "de", "az", "ar")) l else "tr"
        }

    val locale: Locale get() = Locale.forLanguageTag(lang)
    val isRtl: Boolean get() = lang == "ar"

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
