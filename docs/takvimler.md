# Kapsayıcı takvimler: altyapı ve yeni takvim ekleme

9 Kasım 2026, sözleşme v1. Sahibin onayı: "tüm takvimleri yapmak lazım", "11 takvimlik liste uygun yap". İlke: **isteyen açar,
kimsenin inancı tahmin edilmez.** Dini takvimler (`group: "religious"`) her yerde kapalı başlar; kültürel ve resmî takvimler
yalnız kendi bölgelerinde varsayılan açıktır.

Takvimler (kimlikler sabit; 10 Kasım 2026'da 15'i yapıldı): `christian`, `jewish`, `persian`, `chinese`, `korean`, `vietnamese`,
`un`, `bahai`, `ethiopian`, `coptic`, `japanese`, `buddhist`, `hindu`, `sikh`, `indian` (listedeki `eastern` Etiyopya, Kıpti ve
Bahai olarak üç modüle, `chinese` Kore ve Vietnam'la üçe ayrıldı). 11 takvimlik liste bitti; yeni takvim ancak sahibin isteğiyle.
Mevcut İslami günler (Türkiye verisi, `lib/events.mjs`, tür `religious`) ve ülke tatilleri (`src/data/world-holidays.json`) bu sistemin dışındadır, olduğu gibi çalışır.

## Nasıl çalışır

```
lib/calendars/<id>.mjs  ──┐                         ┌─> build.mjs ──> dist/assets/cals-<dil>-<yıl>.json + takvim yıl sayfaları
src/i18n-cal/<id>.json  ──┼─> lib/calendars/index.mjs ┤
lib/calendars/<id>.test.json (doğrulama)             └─> gen-strings.mjs ──> calendars.json + cals.<id>.* uygulama metinleri
```

- **Tek doğru kaynak yapım anındaki JS'dir.** Hesap Node'da, `lib/calendars/<id>.mjs`'te yapılır; site ve uygulamalar yalnız
  üretilen veriyi okur. Swift/Kotlin'de takvim başına hesap yazılmaz.
- `lib/calendars/index.mjs`: kayıt defteri (`CALENDARS`), 2025–2032 verisi (`calendarData()`), doğrulama (`validateCalendars()`),
  test verisi denetimi (`runCalendarTests()`), sitenin yıl dilimi (`siteYearSlice()`), ikinci tarih (`secondDate()`).
- Doğrulama hatası derlemeyi (`node build.mjs`) ve `gen-strings.mjs`'i durdurur; `npm test` hepsini ayrıca denetler.

## Modül (`lib/calendars/<id>.mjs`)

```js
export const meta = {
  id: "christian",          // dosya adıyla aynı, yalnız küçük harf
  group: "religious",       // "religious" | "cultural" | "civil"; religious her yerde varsayılan kapalı
  regions: [],              // varsayılan açık olduğu ülkeler (ISO 3166-1 iki harf); religious'ta etkisiz
  color: "#7B5EA7",         // takvim rengi; iki temanın zemininde en az 3:1, diğer takvimlerden ve Türkiye günlerinin renklerinden ΔE ≥ 20 (doğrulama denetler)
};
export function events(year) { return [{ date: "2027-03-28", key: "easter" }, { date: "2027-12-24", key: "x", end: "2027-12-26" }]; }
// İsteğe bağlı:
export function months(fromYear, toYear) { return [["2027-02-06", 2027, 1, 0], /* … */]; }  // [ay başı, yıl, ay (1..), 0 | 1 (artık ay)]
export function daily(year) { return { "2027-01-01": "t5n12" }; }   // ya da yılın günlerine göre dizi (0'dan): ["t5n12", "t6n13", …]
export function yearLabel(year) { return 9; }                         // sayı ya da metin ("丙午")
```

Kurallar:
- `events(year)` yalnız o yıl **başlayan** olayları verir (`date` o yılda). Çok günlü olayda `end` (dahil), en çok 60 gün; yıl
  sınırını aşabilir (ör. Hanuka). Aynı gün aynı anahtar iki kez olmaz. Anahtar harf ve rakam (`[A-Za-z0-9]+`).
- `months(from, to)`: tablo `from-01-01`'den önce başlar ve **son satır yalnız bitiş sınırıdır**: o satırın ayı gösterilmez. Bu
  yüzden aralığın son ayının günleri de görünsün diye `to-12-31`'den **sonra** başlayan bir ay başı daha eklenir (doğrulama denetler).
  Satırlar tarih sırasında, ay en çok 40 gün. Ay numarası `month` dizisinin sırasıdır (1'den; artık ay ayrı numara alabilir ya da
  bayrak 1 ile `leapFmt` kullanılır). İkinci tarih: günün düştüğü satırdan `{d}` (ayın günü), `{m}` (ay adı), `{y}` (yıl).
- `daily(year)`: iki biçimden biri (bir takvimde tek biçim): `{ "YYYY-MM-DD": kod }` ya da **yılın günlerine göre dizi** (0 = 1 Ocak;
  boş öğe = kod yok; JSON'da `{"YYYY": [...]}`). Her gün kodu olan takvimde dizi biçimi daha küçüktür (365 gün × 8 yıl
  calendars.json'a girer). Kod önce bütün olarak (`daily.t5n12`), yoksa **harf+rakam parçalarına** bölünerek aranır
  (`t5n12` → `daily.t5` · `daily.n12`; parça düzeni `[a-z]+[0-9]+`). Rakamsız tek kod (ör. `sensho`) bütün olarak bulunmalıdır.
- `yearLabel(year)`: Gregoryen yıl için **sayı** (ör. Budist çağı 2570, Reiwa 9) ya da **metin** (çevrilmeden gösterilir; ör.
  "丙午"). Metinde `yearFmt` tek yer tutucu `{y}` içerir; `yearFmt` yoksa uygulamalar etiketi göstermez (doğrulama hata sayar).
  Yıl içinde değişen etiket (ör. dönem değişimi) bu biçime sığmaz; gerekirse sözleşme genişletilir.
- Astronomi gerekiyorsa mevcut kodu kullanın: `src/js/nature-calc.js` (Meeus: Güneş boylamı, yeni ay/dolunay, ekinoks),
  `src/js/prayer-calc.js` (`sunAltTimes`, gün doğumu/batımı). Bunlar tarayıcı betiğidir; Node'da `scripts/test.mjs`'teki gibi
  `new Function("module", "window", kaynak)` ile yüklenir. Yeni formül aynı dosyalara ya da modüle eklenir ve test edilir.
- Tarih hesapları için `lib/events.mjs`'teki `iso`, `addDays`, `dowOf`, `daysInMonth` kullanılabilir. Tarihler bölge saatine göre
  ise (ör. Hindistan saatiyle tithi, Pekin saatiyle yeni ay) modülde o saat dilimiyle hesaplayın ve `note` metninde yazın.

## Metinler (`src/i18n-cal/<id>.json`)

```json
{ "tr": { "name": "Hristiyan takvimi", "desc": "Bir cümle: ne gösterir.", "note": "Kaynak/projeksiyon/bölge notu.",
          "ev": { "easter": "Paskalya" },
          "month": ["…", "…"], "leapFmt": "{m} (artık)", "fmt": "{d} {m} {y}", "yearFmt": "Reiwa {y}",
          "daily": { "t5": "…", "n12": "…" } },
  "en": { … }, … }
```

- 19 dil: tr, en, de, az, ar, id, ur, fa, fr, ru, ja, ko, zh, fil, th, es, pt, hi, bn (Bengalce 10 Kasım 2026). Her dilde **aynı
  anahtarlar**, diziler aynı uzunlukta, metin boş olmaz.
- Zorunlu: `name`, `desc`, `ev.<anahtar>` (kullanılan her olay için; kullanılmayan `ev` anahtarı da hata). Ay tablosu varsa `month`
  (artık ay bayrağı varsa `leapFmt`); `fmt` isteğe bağlı (yoksa site ve uygulamalar "{d} {m} {y}" kullanır). Günlük kod varsa
  `daily.*`, yıl etiketi varsa `yearFmt`. `note` isteğe bağlı.
- Yer tutucular: `fmt` tam olarak `{d}`, `{m}`, `{y}`; **Türkçe `fmt`'de bu sırayla** (uygulamalar argüman sırasını Türkçe metinden
  alır; diğer diller istediği sırada yazar). `leapFmt` yalnız `{m}`, `yearFmt` yalnız `{y}`. Diğer metinlerde en çok bir yer
  tutucu. Her dilde aynı küme. **Latin rakamı** (Arapça, Farsça, Devanagari, Bengalce, Tay rakamı yasak). tr/az "siz" hitabı.
  Fransızcada kesme işareti ’ (sitedeki gibi).
- Takvime özel isteğe bağlı anahtarlar uygulamalarca tanınır: **Yahudi takvimi** `candles` ve `havdalah` (her biri tek yer tutucu,
  saat: `{t}`; ör. "Şabat mumları {t}"): varsa uygulamalar Şabat mumu ve Havdala satırlarını gösterir (saat uygulamada, seçili
  yerin gün batımından hesaplanır). Sitede bu satırlar yok (gerekirse `calendar.js` + `prayer-calc.js` ile eklenir).
- Olay adlarını her dilin yerleşik adıyla yazın (o dilde o topluluğun kullandığı terim). Belirsizse kısa açıklamalı ad
  ("Ortodoks Paskalyası") tercih edin.
- Site `t.cals.<id>` olarak okur; uygulamalara `cals.<id>.<anahtar>` olarak gider (aşağıda).

## Üretilen veri: `calendars.json` (uygulamalar)

`node mobile/shared/gen-strings.mjs` yazar: Android `mobile/android/app/src/main/assets/calendars.json`, iOS
`mobile/ios/App/Resources/calendars.json` (XcodeGen `App/Resources` klasörünü bütünüyle alır, project.yml'ye satır gerekmez).

```json
{ "v": 1, "from": 2025, "to": 2032,
  "cals": { "christian": { "group": "religious", "regions": [], "color": "#7B5EA7",
             "events": [["2027-03-28", "easter"], ["2027-12-24", "x", "2027-12-26"]],
             "months": [["2027-02-06", 2027, 1, 0], …] | null,
             "daily": { "2027-01-01": "t5n12" } | { "2027": ["t5n12", "", …] } | null,
             "years": { "2027": 9, "2028": "丙午" } | null } } }
```

- `events` bütün yıllar tek dizide, tarih sırasında; üçüncü öğe varsa bitiş (dahil).
- `months` bütün aralık tek tabloda; son satır yalnız bitiş sınırı (o günden itibaren ikinci tarih yok).
- `daily` iki biçimden biri (yukarıda); renk `#RRGGBB`.
- Takvimlerin sırası `CALENDARS` sırasıdır (seçim listesinde de bu sıra).

## Uygulama metinleri

- `src/i18n-cal/<id>.json` → `cals.<id>.<anahtar>`: `cals.christian.name`, `cals.christian.ev.easter`, dizi öğeleri **1'den**:
  `cals.<id>.month.1`. Android adı noktası alt çizgi olur (`cals_christian_ev_easter`).
- **Argüman sırası:** `fmt` → (d, m, y), `leapFmt` → (m), `yearFmt` → (y), diğerleri tek argüman. gen-strings `{d}`, `{m}`, `{y}`'yi
  her zaman bu sırayla numaralar (iOS `%1$@`, Android `%1$s`); Türkçe `fmt` de bu sırada yazıldığı için uygulamalar aynı sırayı görür.
- Uygulamaların varsayılanları: `fmt` yoksa "{d} {m} {y}", `yearFmt` yoksa yıl etiketi yok.
- `cals.<id>.` öneki takvim metinlerine ayrılmıştır; takvim özelliğinin arayüz metinleri `app-strings.json`'da başka adla durur
  (çakışırsa gen-strings hata verir).

## Site (takvim yıl sayfaları)

- `/takvim/<yıl>/` ve diğer dillerde `/<dil>/calendar/<yıl>/`: göstergenin üstünde "Takvimler" seçimi (`<details data-cals>`,
  onay kutuları; JS'siz gizli, olaylar sayfada yok).
- Seçim `localStorage` `sth-cals`: açık kimlikler virgülle (`""` = hiçbiri; uygulamalardaki `cal.extra` ile aynı biçim). Kayıt
  yoksa varsayılan: `group != "religious"` ve tarayıcı bölgesi `regions` içinde. Bölge `navigator.language`'ın bölge alt
  etiketinden (`en-GB` → GB), yoksa dilin ana ülkesinden (`calendar.js` `LANG_REGION`: tr→TR, en→US, de→DE, ar→SA, pt→BR, hi→IN…).
- Veri: `assets/cals-<dil>-<yıl>.json` (sayfa yapılandırmasında `cal.cals.url`, sürüm damgalı), yalnız bir takvim açıkken
  getirilir. İçinde o dilde ad, not, o yıla değen olaylar ve adları, yılı kapsayan ay satırları (+ sonraki ilk satır), ay adları ve
  biçimler, o yılın günlük kodları ve yıl etiketi (`siteYearSlice()`).
- Gösterim: aylık tablolarda hücrenin altında takvim renginde nokta (ad `title`'da ve ekran okuyucu metninde), göstergede takvim
  adı, göstergenin altında açık takvimlerin `note` metni; gün penceresinde olay adı "(takvim adı)" ve ay tablosu, günlük kod ya da
  yıl etiketi olan takvimlerde "Takvim adı: {fmt} · günlük · yıl etiketi" satırı. Renk CSSOM ile (`--calc`), satır içi stil yok.
  Yıl dosyasında günlük kodlar her zaman `{ tarih: kod }` biçimindedir (`siteYearSlice` diziyi çevirir).
- Gizlilik sayfasının çerez tablosunda `sth-cals` satırı var (18 dil).

## Doğrulama (`lib/calendars/<id>.test.json`)

```json
{ "source": ["https://… (tablo adı; okunduğu tarih)"],
  "note": "Türetilen günler kuraldan …",
  "cases":  [{ "date": "2027-03-28", "key": "easter" }],
  "absent": [{ "date": "2027-03-28", "key": "orthodoxEaster" }],
  "months": [{ "date": "2027-02-06", "y": 2027, "m": 1, "d": 1, "leap": false }],
  "daily":  [{ "date": "2027-01-01", "code": "t5n12" }],
  "years":  [{ "year": 2027, "value": 9 }] }
```

- Değerler **güvenilir yayımlanmış kaynaktan** (resmî takvim ya da yerleşik tablo); kaynağın kodu alınmaz, yalnız sonuçla
  karşılaştırılır; lisansa dikkat. Kaynak yalnız bir bölge için geçerliyse (ör. Hindistan saatiyle) `note`'ta ve takvimin `note`
  metninde yazın. En az: her yıl için takvimin ana olayı (2025–2032) ve ay tablosu varsa her yıldan birkaç ay başı.
- `scripts/test.mjs` her takvim için: `validateCalendars` (aşağıdaki kurallar), `<id>.test.json` durumları, uygulamalardaki
  `calendars.json`'ın ve `cals.<id>.*` Android metinlerinin güncelliği, yıl sayfasındaki seçim ve veri dosyası.
- `validateCalendars` kuralları: modül kayıtlı ve test dosyası var; kimlik/grup/bölge/renk biçimi; renk 4 tema zemininde ≥3:1 ve diğer takvimlerle resmî tatil/yarım gün/dinî gün/okul renklerinden CIE76 ΔE ≥ 20 (`CAL_MIN_DE`);
  19 dilde metin eşliği, boş metin yok, Latin rakamı, yer tutucular aynı ve izinli (Türkçe `fmt` sırası); olay tarihleri geçerli ve
  yılında, `ev` metinleri var ve hepsi kullanılıyor; ay tablosu (son satır `to`'dan sonra), günlük kod (iki biçim, her kodun metni)
  ve yıl etiketi kuralları (yukarıda).

## Yeni takvim ekleme: kontrol listesi

1. **Modül:** `lib/calendars/<id>.mjs` (`meta`, `events`; gerekiyorsa `months`, `daily`, `yearLabel`). Kimlik yukarıdaki listeden.
2. **Kayıt:** `lib/calendars/index.mjs`'te `import * as <id> from "./<id>.mjs";` ve `CALENDARS` dizisine `<id>,` satırı.
3. **Metinler:** `src/i18n-cal/<id>.json`, 19 dil, aynı anahtarlar (yukarıdaki kurallar). `src/i18n/*.json`'a dokunmayın.
4. **Doğrulama:** `lib/calendars/<id>.test.json`, kaynak adresiyle.
5. **Üret:** `node mobile/shared/gen-strings.mjs` (calendars.json ve uygulama metinleri; ikisi de commit'lenir).
6. **Denetle:** `npm test` (sonra `rm -rf dist`), `node mobile/shared/gen-strings.mjs --check`,
   `node mobile/shared/gen-religion.mjs --check`, `node scripts/build-extension.mjs --check`. İsterseniz bir yıl sayfasını
   tarayıcıda açıp takvimi seçin ve bir olay gününe dokunun.
7. Başka dosyaya dokunmanız gerekmez: site seçimi, gösterim, gizlilik satırı ve uygulamalardaki veri/metin kendiliğinden gelir.
   Paralel çalışan ajanlar yalnız kendi üç dosyasını ve index.mjs'teki iki satırı değiştirir (çakışma bu iki satırda olur,
   ikisini de koruyarak çözülür).

**Uygulamalar kendiliğinden alır:** `calendars.json`'daki olaylar (takvim ekranı, gün ayrıntısı, takvim bildirimi `calEvents`),
ay tablosundan ikinci tarih, günlük kod ve sayı yıl etiketi, ad ve açıklama (Takvimler ve ülke penceresi), renk, varsayılan bölge.

**Uygulama kodu gerektiren durumlar** (iOS/Android görünüm oturumuna bildirin):
- Konuma bağlı saatler (ör. Şabat mumu = gün batımı − 18 dk, Havdala = Güneş −8,5°): yapım anında hesaplanamaz, uygulamada mevcut
  güneş fonksiyonlarıyla (`sunAltTimes` karşılıkları) hesaplanır; sitede `prayer-calc.js` ile.
- Sözleşmede olmayan biçim: çevrilecek adlı yıl etiketi (v1'de metin etiketi çevrilmeden gösterilir), yıl içinde değişen etiket,
  günlük kodda sayı biçimlendirme, saatli olaylar (yalnız tarih var), takvime özel ızgara (ör. ay takvimi görünümü). Yahudi
  takviminin `candles`/`havdalah` satırları uygulamalarda hazırdır (yukarıda).
- Bölgeye göre farklı tarih (aynı bayram iki ülkede farklı günde): ya iki ayrı olay anahtarı ve notta açıklama, ya da sözleşmenin
  genişletilmesi (önce sahibe sorun).

## Komutlar

```
node mobile/shared/gen-strings.mjs           # calendars.json + uygulama metinleri
node mobile/shared/gen-strings.mjs --check   # CI (Mobil iş akışı; src/i18n-cal/** ve lib/** değişince çalışır)
npm test                                     # site derlemesi + denetimler (takvimler dahil)
```
