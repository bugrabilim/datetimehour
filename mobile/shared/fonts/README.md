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
