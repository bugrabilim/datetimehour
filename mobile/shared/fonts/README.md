# Yazı tipleri

## Literata (başlıklar)

- Kaynak: Google Fonts deposu, `ofl/literata/Literata[opsz,wght].ttf` (TypeTogether, 2017; https://github.com/googlefonts/literata).
- Lisans: SIL Open Font License 1.1 (`OFL-Literata.txt`). Uygulamaya gömülür; yazı tipi tek başına satılmaz.
- Üretim: `make-literata.py` (fontTools): değişken yazı tipinden `opsz=24` ile iki sabit ağırlık (500 Medium, 600 SemiBold),
  Latin (Türkçe, Azerice ə, Filipince ñ dahil) + Kiril alt kümesi, `kern/liga/tnum/lnum` özellikleri korunur.
  Çıktılar: `Literata-Medium.ttf`, `Literata-SemiBold.ttf` (~96 KB) ve site için `literata-500.woff2`, `literata-600.woff2` (~37 KB).
- Kullanıldığı yerler: Android `app/src/main/res/font/literata_*.ttf` (`Fonts.kt`), iOS `App/Resources/Fonts/` (`Info.plist`
  `UIAppFonts`, `Fonts.swift`), site `src/assets/fonts/`.
- Kural: Literata yalnız Latin ve Kiril yazılı dillerde (tr, en, de, az, id, fr, fil, es, pt, ru); diğer dillerde sistem yazısı.

Yeniden üretmek için:

```sh
python3 -m venv /tmp/ftv && /tmp/ftv/bin/pip install fonttools brotli
curl -L -o "Literata[opsz,wght].ttf" "https://raw.githubusercontent.com/google/fonts/main/ofl/literata/Literata%5Bopsz%2Cwght%5D.ttf"
/tmp/ftv/bin/python make-literata.py "Literata[opsz,wght].ttf" ./out
```

## Amiri (Kur'an, dua, hadis ve Esma'nın Arapçası)

- Kaynak: Google Fonts deposu, `ofl/amiri/Amiri-Regular.ttf` (Khaled Hosny; https://github.com/aliftype/amiri).
- Lisans: SIL Open Font License 1.1 (`OFL-Amiri.txt`).
- Üretim: fontTools `subset` ile Arapça alt kümesi (U+0600–06FF, 0750–077F, 08A0–08FF, FB50–FDFF, FE70–FEFF, temel
  noktalama ve rakamlar; bütün OpenType özellikleri korunur), ~340 KB.
- Kullanıldığı yerler: Android `res/font/amiri_regular.ttf` (`AppFonts.arabic`), iOS `App/Resources/Fonts/Amiri-Regular.ttf`
  (`UIAppFonts`, `AppFonts.arabic`). Arayüzün Arapça metinleri sistem yazısında kalır.
