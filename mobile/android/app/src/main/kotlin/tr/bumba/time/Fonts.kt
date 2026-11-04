package tr.bumba.time

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Başlık yazısı (doğal tasarım, docs/dogal-tasarim bölüm 3.2; sahibin onayı 4 Kasım 2026): Literata (SIL OFL 1.1;
 * res/font, Latin + Kiril alt kümesi, 500 ve 600; üretim mobile/shared/fonts/make-literata.py). Literata yalnız Latin ve
 * Kiril yazılı dillerde seçilir; Arapça, Urduca, Farsça, Japonca, Korece, Çince, Tayca ve Hintçede sistem yazısı.
 */
object AppFonts {
    private val literata = FontFamily(
        Font(R.font.literata_medium, FontWeight.Medium),
        Font(R.font.literata_semibold, FontWeight.SemiBold),
    )
    private val LITERATA_LANGS = setOf("tr", "en", "de", "az", "id", "fr", "fil", "es", "pt", "ru")

    /** Başlıkta Literata kullanılıyor mu (dilin yazısına göre) */
    val serif: Boolean get() = Strings.lang in LITERATA_LANGS

    /** Sayfa ve bölüm başlıklarının ailesi */
    val heading: FontFamily get() = if (serif) literata else FontFamily.Default

    /** Tarih rakamı, sıcaklık gibi büyük sayılar (her dilde Latin rakamı olduğu için hep Literata) */
    val numerals: FontFamily get() = literata
}
