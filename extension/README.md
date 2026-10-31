# Saat Tarih tarayıcı eklentisi

Manifest V3 eklentisi (Chrome, Edge, Firefox 128+). Araç çubuğundaki simgeye basınca: saat (cihaz saati, 24/12),
bugünün tarihi, hicri tarih (uygulamalardaki Diyanet'e hizalı tablo), seçilen il için bugünün namaz vakitleri ve
sıradaki vakte kalan süre (Türkiye saati). Arayüz dili tarayıcının dilinden (tr, en, de, az, ar, id, ur, fa, fr, ru; diğerlerinde İngilizce; ar/ur/fa sağdan sola; il adları Arapça, Rusça ve Farsçada kendi yazısıyla).

**İnternete bağlanmaz:** izin yalnız `storage` (seçilen il ve 24/12 tercihi, `storage.local`); ağ isteği yok
(`connect-src 'none'`), istatistik, reklam ve takip yok. Gizlilik metni sitenin gizlilik sayfasında "Tarayıcı eklentisi"
bölümüdür (https://time.bumba.tr/gizlilik/#eklenti, İngilizce https://time.bumba.tr/en/privacy/#eklenti).

## Dosyalar

- Elle yazılanlar: `manifest.json`, `popup.html`, `popup.css`, `popup.js`, `strings.json` (eklentiye özgü metinler,
  sitenin dilleri), `icons/` (site simgesinden, `src/assets/favicon.svg`; 16/32/48/128 px).
- Üretilenler (`node scripts/build-extension.mjs`; CI `--check` ile denetler): `lib/prayer-calc.js` (sitedeki hesabın
  kopyası), `lib/data.js` (81 il, hicri tablo, sitedeki ve uygulamalardaki metinlerden namaz adları ve hicri ay adları),
  `_locales/{tr,en,de,ar,id,fa,fr,ru}/messages.json` (Chrome'un desteklediği diller; Azerice ve Urduca listede olmadığı
  için o tarayıcılarda ad ve açıklama İngilizce, eklentinin içi kendi dilinde).
- `src/i18n`, `mobile/shared/app-strings.json`, `src/data/provinces.json`, `holidays.json`, `hijri.json` ya da
  `src/js/prayer-calc.js` değişince betiği yeniden çalıştırın.

## Deneme ve paket

- Chrome/Edge: `chrome://extensions` › Geliştirici modu › "Paketlenmemiş öğe yükle" › `extension/` klasörü.
- Firefox: `about:debugging` › Bu Firefox › "Geçici eklenti yükle" › `extension/manifest.json`.
- Paket: `node scripts/build-extension.mjs --zip` → `dist-extension/saat-tarih-<sürüm>.zip` (depoya girmez).

## Mağaza yayını (yapılmadı; sahibin kararı ve hesapları gerekir)

- **Chrome Web Store:** geliştirici hesabı (bir kerelik kayıt ücreti), zip yükleme, kategori "Araçlar", gizlilik
  sekmesinde "veri toplamıyor" beyanı ve tek amaç açıklaması ("saat, tarih ve namaz vakitlerini gösterir"), gizlilik
  politikası adresi yukarıdaki sayfa. `storage` izninin gerekçesi: "Seçilen ili ve saat biçimini hatırlamak için".
- **Firefox Add-ons (AMO):** hesap ücretsiz; aynı zip; `browser_specific_settings.gecko.id` = `saat-tarih@bumba.tr`,
  veri toplama beyanı `none` (manifest'te).
- **Edge Add-ons:** Chrome paketiyle aynı.
- Sürüm numarası `manifest.json` › `version`; her yüklemede artırılır.
