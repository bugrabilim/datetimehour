# Mağaza adları ve ülkeye göre vitrin (9 Kasım 2026)

Sahibin isteği: "tüm ülkelere göre App Store adı farklı olsun." Gerekçe: `docs/arastirma-musluman-olmayanlar-2026-11.md`
(uygulamanın içeriği herkese hitap ediyor, ama vitrin her dilde "namaz uygulaması" diyordu). Bu belge kararı, App Store ve
Google Play'deki karşılığını, görsel sırasını ve elle yapılacakları toplar.

## Strateji

- **Uygulama herkese aynıdır; yalnız vitrin değişir:** ad, alt başlık, anahtar kelimeler, tanıtım metni, açıklamanın başı ve
  görsel sırası.
- **Müslümanların çoğunlukta olduğu pazarların (Google Play'de oran %40 ve üstü) vitrini namaz vakitlerini öne çıkarır;** diğerleri nötrdür (saat, alarm,
  widget, hava, takvim, dünya saatleri, uyku ve odak, gökyüzü). Nötr vitrinde de dini özellikler açıklamanın ilerisinde ve
  birkaç anahtar kelimede kalır; namaz vakti arayan yine bulur.
- **Kimsenin inancı tahmin edilmez;** yalnız pazarın vitrini seçilir. Pazarın grubu Pew 2020 Müslüman oranından gelir
  (%50 üstü: Müslüman pazarı).
- **Ad deseni:** "<uygulamanın o dildeki adı>: <ek>", en çok 30 karakter. Ön ek ana ekrandaki adla aynıdır (Apple 2.3.8 ve
  QA1892: mağaza adı ile ana ekrandaki ad benzer olmalı); yalnız ek değişir. Müslüman pazarı örneği "Saat Tarih: Namaz
  Vakitleri", nötr örnek "Time & Date: Clock & Widgets".
- **Kurallar:** ad ve alt başlıkta "#1", "best", "free", "ücretsiz", "new" gibi sıralama, fiyat ve tanıtım sözcükleri ve emoji
  yok (Apple ve Play kuralı). App Store anahtar kelimeleri 100 bayt, ad ve alt başlıktaki sözcükler tekrarlanmaz, marka ve başka
  uygulama adı yok. Sayılar kodla aynı (21 saat modeli, 25 kadran, 14 hesap yöntemi, 973 ilçe, 30 ülkenin tatilleri, 18 dil).
  Türkçe ve Azericede "siz". Rakamlar Latin.

## App Store

Apple adı **ülkeye değil dil sürümüne** bağlar. Her mağaza ülkesi bir varsayılan dil sürümünü ve birkaç ek dil sürümünü
dizinler; kişiye cihazının dilindeki sürüm gösterilir, yoksa mağazanın dili, o da yoksa birincil dil. Bu yüzden "ülkeye göre
ad" App Store'da "dil sürümüne göre ad" demektir. 31 Mart 2026'dan beri App Store Connect'te 50 dil sürümü var; Farsça,
Azerice, Filipince, Kazakça, Özbekçe, Boşnakça, Svahili ve Hausa için yok.

### Hangi mağaza hangi dil sürümünü dizinler

| Mağaza (ülke) | Varsayılan | Ek dizinlenenler |
|---|---|---|
| ABD | en-US | ar, zh-Hans, zh-Hant, fr, ko, pt-BR, ru, es-MX, vi |
| Birleşik Krallık | en-GB | — |
| Kanada | en-CA | fr-CA |
| Avustralya | en-AU | en-GB |
| Hindistan | en-GB | bn, gu, hi, kn, ml, mr, or, pa, ta, te, ur |
| Pakistan | en-GB | ur |
| Bangladeş | (App Store yok; Bengalce yalnız Hindistan'da dizinlenir) | |
| Endonezya | en-GB | id |
| Malezya | en-GB | ms |
| Suudi Arabistan, BAE | en-GB | ar |
| Mısır | en-GB | ar, fr |
| Türkiye | en-GB | tr |
| Azerbaycan, Kazakistan, Özbekistan | en-GB | — |
| İran | (App Store yok) | |
| Almanya / Fransa | de / fr | en-GB |
| İspanya / Meksika | es-ES / es-MX | ca, en-GB / en-GB |
| Brezilya / Portekiz | pt-BR / pt-PT | en-GB |
| Rusya | ru | en-GB, uk |
| Japonya / Kore | ja / ko | en-US / en-GB |
| Çin / Tayvan, Hong Kong | zh-Hans / zh-Hant | en-GB |
| Tayland | en-GB | th |
| Filipinler, Nijerya, Güney Afrika | en-GB | — |
| Cezayir, Fas, Tunus, Moritanya | en-GB | ar, fr |
| Bahreyn, Irak, Ürdün, Kuveyt, Lübnan, Libya, Umman, Katar, Yemen | en-GB | ar |
| Senegal, Mali, Nijer | en-GB | fr |
| Brunei, Maldivler, Kırgızistan, Tacikistan, Türkmenistan, Arnavutluk, Kosova | en-GB | — |
| Kıbrıs / Singapur | el, tr / zh-Hans | |

Sonuç: **en-GB** hem İngiltere, Hindistan, Nijerya, Güney Afrika, Filipinler'in hem de Pakistan, Endonezya, Malezya, Suudi
Arabistan, BAE, Mısır, Türkiye, Azerbaycan, Kazakistan ve Özbekistan'ın varsayılanıdır; bu yüzden adı nötr, alt başlığında
"prayer times" geçer. ar-SA, fr-FR, ru, es-MX, pt-BR, ko ve zh ABD mağazasında da dizinlenir. Birincil dil sürümünün
anahtar kelimelerinin her mağazada dizinlendiği söylenir (üçüncü taraf kaynak).

### Dil sürümleri ve vitrinleri

| Vitrin | Dil sürümleri |
|---|---|
| Müslüman pazarı (vakitler önde, ad "…: Namaz Vakitleri" gibi) | tr, ar-SA, id, ms (yeni), ur-PK (yeni) |
| Nötr ad, alt başlıkta namaz vakti (karma kitle) | en-GB (yeni), bn-BD (yeni; yalnız Hindistan'da dizinlenir), fr-FR (Fransızca dizinlenen Müslüman ülkeleri çok) |
| Nötr | en-US, en-AU (yeni), en-CA (yeni), fr-CA (yeni), de-DE, es-ES, es-MX (yeni), pt-BR, pt-PT (yeni), ru, ja, ko, zh-Hans, zh-Hant (yeni), th, hi |

Yeni dil sürümlerinde bütün dosyalar vardır (ad, alt başlık, anahtar kelimeler, açıklama, tanıtım metni, 1.1 yenilikleri,
gizlilik/destek/pazarlama adresleri). Malayca (ms) ve Geleneksel Çince (zh-Hant) arayüz yok; açıklamaları bunu açıkça yazar
(ms: uygulama İngilizce, Endonezce ve 16 dilde daha; zh-Hant: arayüz Basitleştirilmiş Çince). Bengalce arayüz de yok.

### Adlar tablosu

> Bu tablo `mobile/ios/fastlane/metadata/<dil>/{name,subtitle}.txt` dosyalarından üretildi (9 Kasım 2026); ad değişirse
> yeniden üretilir. "(ek)": o mağazada ek dil sürümü olarak dizinlenir, gösterim cihaz diline göre.

| Dil sürümü | Ad | Alt başlık | Dizinlendiği mağazalar |
|---|---|---|---|
| tr | Saat Tarih: Namaz Vakitleri | Ezan vakti, kıble, hava, alarm | Türkiye, Kıbrıs (ek) |
| ar-SA | الوقت والتاريخ: مواقيت الصلاة | الأذان، القبلة، الطقس، المنبه | Arap ülkeleri, Mısır, Kuzey Afrika (ek); ABD (ek) |
| id | Jam & Tanggal: Jadwal Sholat | Adzan, kiblat, cuaca & alarm | Endonezya (ek) |
| ms | Masa & Tarikh: Waktu Solat | Azan, kiblat, cuaca & penggera | Malezya (ek) |
| ur-PK | وقت اور تاریخ: نماز کے اوقات | اذان، قبلہ، موسم اور الارم | Pakistan, Hindistan (ek) |
| en-GB | Time & Date: Clock & Widgets | Alarms, weather & prayer times | İngiltere (varsayılan); Hindistan, Pakistan, Endonezya, Malezya, Körfez, Mısır, Türkiye, Orta Asya, Afrika'nın çoğu (varsayılan); Avrupa'nın çoğunda ek |
| bn-BD | সময় ও তারিখ: ঘড়ি ও উইজেট | অ্যালার্ম, আবহাওয়া ও নামাজ | Hindistan (ek; Bangladeş'te App Store yok) |
| fr-FR | Heure & Date: Réveil & Widgets | Météo et horaires de prière | Fransa (varsayılan); Kuzey ve Batı Afrika, Mısır, ABD (ek) |
| en-US | Time & Date: Clock & Widgets | Alarm, weather & calendar | ABD (varsayılan), Japonya (ek) |
| en-AU | Time & Date: Clock & Widgets | Alarm, weather & calendar | Avustralya |
| en-CA | Time & Date: Clock & Widgets | Alarm, weather & calendar | Kanada |
| fr-CA | Heure & Date: Réveil & Widgets | Météo, calendrier, minuterie | Kanada (ek) |
| de-DE | Uhr & Datum: Wecker & Widgets | Weltuhr, Kalender und Wetter | Almanya, Avusturya, İsviçre |
| es-ES | Hora y Fecha: Reloj y Widgets | Alarma, tiempo y calendario | İspanya |
| es-MX | Hora y Fecha: Reloj y Widgets | Alarma, clima y calendario | Meksika (varsayılan), ABD (ek) |
| pt-BR | Hora e Data: Relógio e Widgets | Alarme, clima e calendário | Brezilya, ABD (ek) |
| pt-PT | Hora e Data: Relógio e Widgets | Alarme, tempo e calendário | Portekiz |
| ru | Время и дата: часы и виджеты | Будильник, погода, календарь | Rusya, ABD (ek) |
| ja | 時刻と日付：時計とウィジェット | アラーム、天気、カレンダー、世界時計 | Japonya |
| ko | 시간과 날짜: 시계와 위젯 | 알람, 날씨, 달력, 세계 시계 | Kore, ABD (ek) |
| zh-Hans | 时间与日期：时钟与小组件 | 闹钟、天气、日历、世界时钟 | Çin, Singapur (ek), ABD (ek) |
| zh-Hant | 時間與日期：時鐘與小工具 | 鬧鐘、天氣、行事曆、世界時鐘 | Tayvan, Hong Kong, ABD (ek) |
| th | เวลาและวันที่: นาฬิกาปลุก | สภาพอากาศ ปฏิทิน นาฬิกาโลก | Tayland (ek) |
| hi | समय और तारीख: घड़ी और विजेट | अलार्म, मौसम, कैलेंडर, टाइमर | Hindistan (ek) |

### Özel ürün sayfası "Vakitler"

Ad, alt başlık, simge ve açıklama özel ürün sayfasında değişmez; yalnız görseller, tanıtım metni, anahtar kelimeler (dil
sürümünün onaylı anahtar kelime alanından seçilir) ve derin bağlantı değişir. Nötr dil sürümlerinde "prayer", "qibla",
"ramadan" gibi kelimelerle eşleşen bir "Vakitler" sayfası açılır: vakit görselleri önde. 1.1 onaylandıktan sonra App Store
Connect'te elle (en çok 70 sayfa). Anahtar kelime atamanın tarihi ve işleyişi üçüncü taraf kaynaklardan; açmadan önce Apple
belgesinden doğrulanır.

### Görseller (store-screenshots.sh, frame-screenshots.mjs, captions.json)

24 mağaza klasörü. Görüntüler uygulama dili başına bir kez çekilir, aynı dildeki ek klasörlere kopyalanır; ek klasörün takvim
ülkesi farklıysa yalnız takvim görseli o klasör için yeniden çekilir. deliver görselleri dosya adı sırasıyla yükler.

| Vitrin | Klasörler | iPhone sırası | iPad sırası |
|---|---|---|---|
| Müslüman pazarı (1.0'daki sıra) | tr, ar-SA, id, ms, ur-PK | 01 saat (altında sıradaki vakit ve bugünün vakitleri), 02 vakitler, 03 takvim (Türkiye, dini günler), 04 dünya, 05 araçlar, 06 imsakiye, 07 gece modu | saat, vakitler, takvim, dünya, araçlar |
| Nötr | diğer 19 klasör | 01 saat (altında hafta, dünya saatleri, gün doğumu/batımı), 02 gece modu, 03 takvim (klasörün ülkesinin resmî tatilleri, dini günler gizli), 04 dünya, 05 uyku, 06 araçlar, 07 vakitler | saat, takvim, dünya, araçlar, vakitler |

- Nötr görsellerde alt çubuk Saat, Hava, Takvim, Dünya, Tümü (vakitler görselinde Hava, Takvim, Vakitler); uygulamanın
  varsayılanı Vakitler, Hava, Takvim'dir. İmsakiye nötr klasörlerde yok.
- Uygulama dili ve takvim ülkesi: en-US (ABD), en-GB (Birleşik Krallık), en-AU (Avustralya), en-CA (Kanada), bn-BD
  (İngilizce arayüz, takvim özel günsüz), ms (İngilizce arayüz, Müslüman sırası, takvim 1.0'daki gibi), de-DE (Almanya),
  fr-FR (Fransa), fr-CA (Fransızca, Kanada), ru (Rusya), ja (Japonya), ko (özel günsüz), zh-Hans (Çin), zh-Hant
  (Basitleştirilmiş Çince arayüz, Hong Kong), th (özel günsüz), es-ES (İspanya), es-MX (İspanyolca, özel günsüz), pt-BR
  (Brezilya), pt-PT (Brezilya Portekizcesi arayüz, Portekiz), hi (özel günsüz). "Özel günsüz": ülkenin tatil verisi yok
  (world-holidays.json'daki 30 ülkede değil), takvim görselinde özel gün gösterilmez.
- Başlık dili: klasörün dili; bölgesel sürümler ana dilinkini kullanır (en-GB/AU/CA → en, fr-CA → fr, es-MX → es,
  pt-PT → pt). ms, ur, bn ve zh-Hant başlıkları eklendi; İngilizce arayüzlü ms ve bn-BD görsellerinin başlığı Malayca ve
  Bengalce. Müslüman pazarı dillerinin (tr, ar, id, ms, ur) saat ve takvim başlıkları vakitleri ve dini günleri anar,
  diğerleri nötrdür ("Clock and date at a glance", "Calendar and holidays").
- Hava sekmesi görsel değildir: imzasız simülatör yapısında WeatherKit veri vermez. Widget galerisinin doğrudan açılan
  ekranı olmadığı için widget görseli yok; widget'lar saat başlığının alt satırında anılır.
- Bengalce arayüz gelince `store-screenshots.sh`'ta bn-BD en satırından çıkarılıp kendi satırına taşınır.

## Google Play

Play'de **ülkeye göre** ayrı sayfa açılabilir (özel mağaza sayfası; en çok 50; ad, simge, açıklamalar ve görseller farklı
olabilir; iletişim, gizlilik politikası ve kategori ortaktır). Hangi sayfanın görüneceğini kullanıcının Google Play ülkesi,
sayfadaki çeviriyi cihazın dili seçer. Özel sayfalar makineyle çevrilmez; çevirisi olmayan dil özel sayfanın varsayılan
dilini görür. fastlane ve Play API yalnız varsayılan sayfayı yükler; özel sayfalar Play Console'a elle girilir.

| Sayfa | Hedefleme | Dosyalar |
|---|---|---|
| Varsayılan (nötr), 23 çeviri: tr-TR, en-US, en-GB, de-DE, az-AZ, ar, id, ur, fa, fr-FR, ru-RU, ja-JP, ko-KR, zh-CN, fil, th, es-419, es-ES, pt-BR, hi-IN, bn-BD, ms, ms-MY | Özel sayfaya atanmamış bütün ülkeler | `mobile/android/fastlane/metadata/android/` |
| `muslim`, 14 çeviri: ar, tr-TR, az-AZ, id, ur, fa, bn-BD, ms, ms-MY, fr-FR, ru-RU, en-US, en-GB, pt-BR | Pew'e göre Müslümanların %40 ve üstü olduğu ve Play'in açık olduğu 44 ülke (aşağıdaki laik 6 ülke hariç) | `mobile/android/store-listings/muslim/` |
| `secular`, 4 çeviri: az-AZ, ru-RU, tr-TR, en-US | Müslüman çoğunluklu laik 6 ülke: Azerbaycan, Özbekistan, Türkmenistan, Kırgızistan, Tacikistan, Kosova | `mobile/android/store-listings/secular/` |
| `prayer-search` (muslim metinleri) | Arama kelimeleri: prayer times, namaz vakitleri, jadwal sholat, waktu solat, مواقيت الصلاة … | aynı |

Özel sayfa adları: `muslim` "Saat Tarih: Namaz Vakitleri", "الوقت والتاريخ: مواقيت الصلاة", "Jam & Tanggal: Jadwal Sholat",
"Masa & Tarikh: Waktu Solat", "وقت اور تاریخ: نماز کے اوقات", "ساعت و تاریخ: اوقات نماز", "সময় ও তারিখ: নামাজের সময়",
"Saat Tarix: Namaz Vaxtları", "Heure & Date: Heures de prière", "Время и дата: намаз и кибла", "Time & Date: Prayer Times",
"Hora e Data: Horário de Oração"; `secular` "Saat Tarix: Təqvim və Hava", "Время и дата: часы и календарь",
"Saat Tarih: Alarm ve Takvim", "Time & Date: Clock & Widgets". Metinler Android'e göre yazıldı (Open-Meteo, 14 günlük
tahmin, Wear OS, Android TV; Apple Watch ve WeatherKit yok; konuma bağlı hatırlatıcılar ilk Play sürümünde kapalı olduğu için
anılmaz). Denetim: `node mobile/android/store-listings/check.mjs`. Ayrıntı ve Play Console adımları:
`mobile/android/store-listings/README.md`.

**Sınırda olanlar (10 Kasım 2026, sahibin kararı):** Müslüman oranı %40 ve üstüyse `muslim`, değilse laik (nötr) sayfa.
Böylece Nijerya (%56), Lübnan (%68), Çad, Gine-Bissau ve Eritre `muslim`'de kaldı; Kazakistan (%78), Arnavutluk (%75) ve
Bosna-Hersek (%54) `secular`'dan, Fildişi Sahili (%46) varsayılandan `muslim`'e geçti. %40'ın altındakiler (Etiyopya %36,
Kuzey Makedonya %35, Benin %31 …) nötr varsayılan sayfada kalır; varsayılan sayfa laik sayfayla aynı nitelikte ve daha çok
dili var. `secular`'da kalan altı ülke sınırda değildi: Müslüman oranı %90'ın üstündeki laik eski Sovyet ülkeleri ve
Kosova.

### Ülke tablosu

Kaynak: Pew Research 2020 Müslüman oranları ("tahmin": CIA Factbook vb.), mağaza durumu araştırma notları (doğrulanmamış
olanlar işaretli). "En uygun dil": o ülkede çoğu kullanıcının göreceği çeviri. `muslim?` / `varsayılan?`: ülke Play'de
ayrı ülke olarak görünürse. "—": Google Play yok. Sıra: sayfa, sonra Müslüman oranı.

| ISO | Ülke | Müslüman % (Pew 2020) | Play sayfası | En uygun dil (Play kodu) | Not |
|---|---|---|---|---|---|
| AF | Afganistan | 99.9 | muslim | fa | mağaza durumu doğrulanmadı |
| YE | Yemen | 99.9 | muslim | ar | mağaza durumu doğrulanmadı; App Store yok |
| SO | Somali | 99.8 | muslim | ar | App Store yok |
| MA | Fas | 99.7 | muslim | ar |  |
| IQ | Irak | 99.5 | muslim | ar |  |
| TN | Tunus | 99.3 | muslim | ar |  |
| MR | Moritanya | 99.2 | muslim | ar |  |
| LY | Libya | 99.0 | muslim | ar |  |
| PS | Filistin Bölgeleri | 99.0 | muslim | ar | mağaza durumu doğrulanmadı; App Store yok |
| SD | Sudan | 98.9 | muslim | ar | App Store yok |
| DZ | Cezayir | 98.4 | muslim | ar |  |
| KM | Komorlar | 98.3 | muslim | fr-FR | App Store yok |
| NE | Nijer | 98.1 | muslim | fr-FR |  |
| DJ | Cibuti | 97.7 | muslim | fr-FR | App Store yok |
| SN | Senegal | 97.6 | muslim | fr-FR |  |
| JO | Ürdün | 97.1 | muslim | ar |  |
| TR | Türkiye | 97.1 | muslim | tr-TR |  |
| GM | Gambiya | 97.0 | muslim | en-GB |  |
| PK | Pakistan | 96.5 | muslim | ur |  |
| EG | Mısır | 95.2 | muslim | ar |  |
| ML | Mali | 94.1 | muslim | fr-FR |  |
| MV | Maldivler | 94.1 | muslim | en-GB |  |
| SA | Suudi Arabistan | 92.7 | muslim | ar |  |
| BD | Bangladeş | 91.1 | muslim | bn-BD | App Store yok |
| ID | Endonezya | 87.0 | muslim | id |  |
| GN | Gine | 86.8 | muslim | fr-FR |  |
| BN | Brunei | 82.2 | muslim | ms-MY |  |
| OM | Umman | 81.8 | muslim | ar |  |
| KW | Kuveyt | 80.2 | muslim | ar |  |
| SL | Sierra Leone | 80.1 | muslim | en-GB |  |
| KZ | Kazakistan | 77.9 | muslim | ru-RU | sınırda; %40 kuralıyla laikten Müslüman sayfasına (10 Kasım 2026) |
| QA | Katar | 75.9 | muslim | ar |  |
| AL | Arnavutluk | 74.5 | muslim | en-US | sınırda; %40 kuralıyla laikten Müslüman sayfasına (10 Kasım 2026) |
| BH | Bahreyn | 74.0 | muslim | ar |  |
| AE | Birleşik Arap Emirlikleri | 72.9 | muslim | ar |  |
| LB | Lübnan | 67.8 | muslim | ar | sınırda; %40 kuralıyla yerinde |
| BF | Burkina Faso | 67.4 | muslim | fr-FR |  |
| MY | Malezya | 64.1 | muslim | ms-MY |  |
| TD | Çad | 56.4 | muslim | fr-FR | sınırda; %40 kuralıyla yerinde |
| NG | Nijerya | 56.1 | muslim | en-GB | sınırda; %40 kuralıyla yerinde |
| GW | Gine-Bissau | 56.0 | muslim | pt-BR | sınırda; %40 kuralıyla yerinde |
| BA | Bosna-Hersek | 53.6 | muslim | en-US | sınırda; %40 kuralıyla laikten Müslüman sayfasına (10 Kasım 2026) |
| ER | Eritre | 51.7 | muslim | en-GB | sınırda; %40 kuralıyla yerinde; mağaza durumu doğrulanmadı; App Store yok |
| CI | Côte d’Ivoire | 46.3 | muslim | fr-FR | %40 kuralıyla varsayılandan Müslüman sayfasına (10 Kasım 2026) |
| YT | Mayotte | 98.8 | muslim? | fr-FR | mağaza durumu doğrulanmadı |
| TJ | Tacikistan | 98.9 | secular | ru-RU |  |
| UZ | Özbekistan | 95.6 | secular | ru-RU |  |
| AZ | Azerbaycan | 94.7 | secular | az-AZ |  |
| TM | Türkmenistan | 94.3 | secular | ru-RU |  |
| XK | Kosova | 94.3 | secular | en-US | mağaza durumu doğrulanmadı |
| KG | Kırgızistan | 91.7 | secular | ru-RU |  |
| ET | Etiyopya | 36.2 | varsayılan | en-GB | App Store yok |
| MK | Kuzey Makedonya | 34.7 | varsayılan | en-GB |  |
| BJ | Benin | 31.4 | varsayılan | fr-FR |  |
| TZ | Tanzanya | 29.9 | varsayılan | en-GB |  |
| CM | Kamerun | 25.2 | varsayılan | fr-FR |  |
| CY | Kıbrıs | 24.4 | varsayılan | en-GB |  |
| ME | Karadağ | 21.3 | varsayılan | en-GB |  |
| GH | Gana | 19.9 | varsayılan | en-GB |  |
| MZ | Mozambik | 19.4 | varsayılan | pt-BR |  |
| MU | Mauritius | 17.5 | varsayılan | en-GB |  |
| SG | Singapur | 16.1 | varsayılan | en-GB |  |
| TG | Togo | 16.1 | varsayılan | fr-FR |  |
| IN | Hindistan | 15.2 | varsayılan | hi-IN |  |
| IL | İsrail | 14.7 | varsayılan | en-GB |  |
| SR | Surinam | 13.1 | varsayılan | en-GB |  |
| LR | Liberya | 12.7 | varsayılan | en-US |  |
| MW | Malavi | 11.6 | varsayılan | en-GB |  |
| UG | Uganda | 11.4 | varsayılan | en-GB |  |
| KE | Kenya | 10.9 | varsayılan | en-GB |  |
| GE | Gürcistan | 10.5 | varsayılan | en-GB |  |
| BG | Bulgaristan | 10.3 | varsayılan | en-GB |  |
| LK | Sri Lanka | 10.2 | varsayılan | en-GB |  |
| FR | Fransa | 9.1 | varsayılan | fr-FR |  |
| GA | Gabon | 8.6 | varsayılan | fr-FR |  |
| AT | Avusturya | 8.3 | varsayılan | de-DE |  |
| RU | Rusya | 8.2 | varsayılan | ru-RU |  |
| SE | İsveç | 8.1 | varsayılan | en-GB |  |
| BE | Belçika | 6.8 | varsayılan | fr-FR |  |
| DE | Almanya | 6.5 | varsayılan | de-DE |  |
| GY | Guyana | 6.5 | varsayılan | en-GB |  |
| PH | Filipinler | 6.5 | varsayılan | fil |  |
| GB | Birleşik Krallık | 6.4 | varsayılan | en-GB |  |
| CF | Orta Afrika Cumhuriyeti | 6.2 | varsayılan | fr-FR | App Store yok |
| SS | Güney Sudan | 6.2 | varsayılan | en-GB | App Store yok |
| CH | İsviçre | 6.1 | varsayılan | de-DE |  |
| LI | Liechtenstein | 5.9 (tahmin) | varsayılan | de-DE | mağaza durumu doğrulanmadı; App Store yok |
| FJ | Fiji | 5.8 | varsayılan | en-GB |  |
| NL | Hollanda | 5.5 | varsayılan | en-GB |  |
| TT | Trinidad ve Tobago | 5.5 | varsayılan | en-GB |  |
| GR | Yunanistan | 5.1 | varsayılan | en-GB |  |
| NP | Nepal | 5.1 | varsayılan | en-GB |  |
| CA | Kanada | 4.9 | varsayılan | en-US |  |
| TH | Tayland | 4.5 | varsayılan | th |  |
| IT | İtalya | 4.4 | varsayılan | en-GB |  |
| RS | Sırbistan | 4.4 | varsayılan | en-GB |  |
| DK | Danimarka | 4.2 | varsayılan | en-GB |  |
| NO | Norveç | 4.1 | varsayılan | en-GB |  |
| GQ | Ekvator Ginesi | 4.0 | varsayılan | es-419 | App Store yok |
| ES | İspanya | 3.6 | varsayılan | es-ES |  |
| GI | Cebelitarık | 3.6 (tahmin) | varsayılan | en-GB | mağaza durumu doğrulanmadı; App Store yok |
| MT | Malta | 3.6 | varsayılan | en-GB |  |
| AU | Avustralya | 3.5 | varsayılan | en-GB |  |
| MN | Moğolistan | 3.5 | varsayılan | en-GB |  |
| MM | Myanmar (Burma) | 3.3 | varsayılan | en-GB |  |
| BI | Burundi | 3.0 | varsayılan | fr-FR | App Store yok |
| KH | Kamboçya | 2.0 | varsayılan | en-GB |  |
| LU | Lüksemburg | 1.8 | varsayılan | fr-FR |  |
| RW | Ruanda | 1.8 | varsayılan | en-GB |  |
| IE | İrlanda | 1.7 | varsayılan | en-GB |  |
| FI | Finlandiya | 1.6 | varsayılan | en-GB |  |
| SI | Slovenya | 1.6 | varsayılan | en-GB |  |
| ZA | Güney Afrika | 1.6 | varsayılan | en-GB |  |
| CG | Kongo - Brazavil | 1.5 | varsayılan | fr-FR |  |
| HR | Hırvatistan | 1.4 | varsayılan | en-GB |  |
| NZ | Yeni Zelanda | 1.4 | varsayılan | en-GB |  |
| CD | Kongo - Kinşasa | 1.3 | varsayılan | fr-FR |  |
| IS | İzlanda | 1.2 | varsayılan | en-GB |  |
| MG | Madagaskar | 1.2 | varsayılan | fr-FR |  |
| US | Amerika Birleşik Devletleri | 1.2 | varsayılan | en-US |  |
| VG | Britanya Virjin Adaları | 1.2 (tahmin) | varsayılan | en-GB |  |
| BM | Bermuda | 1.0 (tahmin) | varsayılan | en-GB |  |
| AR | Arjantin | 0.9 | varsayılan | es-419 |  |
| SC | Seyşeller | 0.9 | varsayılan | en-GB |  |
| AD | Andorra | 0.8 (tahmin) | varsayılan | es-419 | mağaza durumu doğrulanmadı; App Store yok |
| BB | Barbados | 0.7 | varsayılan | en-GB |  |
| LV | Letonya | 0.7 | varsayılan | en-GB |  |
| AG | Antigua ve Barbuda | 0.6 (tahmin) | varsayılan | en-GB |  |
| EE | Estonya | 0.6 | varsayılan | en-GB |  |
| UA | Ukrayna | 0.6 | varsayılan | en-GB |  |
| CW | Curaçao | 0.5 | varsayılan | en-GB | mağaza durumu doğrulanmadı; App Store yok |
| MC | Monako | 0.5 (tahmin) | varsayılan | fr-FR | mağaza durumu doğrulanmadı; App Store yok |
| MO | Çin Makao ÖİB | 0.5 | varsayılan | zh-CN (Geleneksel Çince çeviri yok) |  |
| TW | Tayvan | 0.5 | varsayılan | zh-CN (Geleneksel Çince çeviri yok) |  |
| ZM | Zambiya | 0.5 | varsayılan | en-GB |  |
| ZW | Zimbabve | 0.5 | varsayılan | en-GB |  |
| GD | Grenada | 0.4 | varsayılan | en-GB |  |
| PT | Portekiz | 0.4 | varsayılan | pt-BR (pt-PT çevirisi yok) |  |
| RO | Romanya | 0.4 | varsayılan | en-GB |  |
| TV | Tuvalu | 0.4 (tahmin) | varsayılan | en-GB | mağaza durumu doğrulanmadı; App Store yok |
| AI | Anguilla | 0.3 (tahmin) | varsayılan | en-GB |  |
| AM | Ermenistan | 0.3 | varsayılan | ru-RU |  |
| AO | Angola | 0.3 | varsayılan | pt-BR |  |
| AW | Aruba | 0.3 | varsayılan | en-GB | mağaza durumu doğrulanmadı; App Store yok |
| BW | Botsvana | 0.3 | varsayılan | en-GB |  |
| BY | Belarus | 0.3 | varsayılan | ru-RU |  |
| CV | Cabo Verde | 0.3 | varsayılan | pt-BR |  |
| CZ | Çekya | 0.3 | varsayılan | en-GB |  |
| HK | Çin Hong Kong ÖİB | 0.3 | varsayılan | zh-CN (Geleneksel Çince çeviri yok) |  |
| KN | Saint Kitts ve Nevis | 0.3 (tahmin) | varsayılan | en-GB |  |
| KR | Güney Kore | 0.3 | varsayılan | ko-KR |  |
| SB | Solomon Adaları | 0.3 | varsayılan | en-GB |  |
| TL | Timor-Leste | 0.3 | varsayılan | pt-BR | mağaza durumu doğrulanmadı; App Store yok |
| BT | Butan | 0.2 | varsayılan | en-GB |  |
| BZ | Belize | 0.2 | varsayılan | en-GB |  |
| JP | Japonya | 0.2 | varsayılan | ja-JP |  |
| KY | Cayman Adaları | 0.2 (tahmin) | varsayılan | en-GB |  |
| LS | Lesotho | 0.2 | varsayılan | en-GB | App Store yok |
| MD | Moldova | 0.2 | varsayılan | en-GB |  |
| PW | Palau | 0.2 (tahmin) | varsayılan | en-US |  |
| BS | Bahamalar | 0.1 | varsayılan | en-GB |  |
| CL | Şili | 0.1 | varsayılan | es-419 |  |
| DM | Dominika | 0.1 (tahmin) | varsayılan | en-GB |  |
| HU | Macaristan | 0.1 | varsayılan | en-GB |  |
| KI | Kiribati | 0.1 | varsayılan | en-GB | App Store yok |
| LC | Saint Lucia | 0.1 | varsayılan | en-GB |  |
| LT | Litvanya | 0.1 | varsayılan | en-GB |  |
| MH | Marshall Adaları | 0.1 (tahmin) | varsayılan | en-US | mağaza durumu doğrulanmadı; App Store yok |
| MS | Montserrat | 0.1 (tahmin) | varsayılan | en-GB |  |
| NA | Namibya | 0.1 | varsayılan | en-GB |  |
| PA | Panama | 0.1 | varsayılan | es-419 |  |
| PG | Papua Yeni Gine | 0.1 | varsayılan | en-GB |  |
| SK | Slovakya | 0.1 | varsayılan | en-GB |  |
| SM | San Marino | 0.1 (tahmin) | varsayılan | en-GB | mağaza durumu doğrulanmadı; App Store yok |
| ST | Sao Tome ve Principe | 0.1 | varsayılan | pt-BR |  |
| SZ | Esvatini | 0.1 | varsayılan | en-GB |  |
| TC | Turks ve Caicos Adaları | 0.1 (tahmin) | varsayılan | en-GB |  |
| TO | Tonga | 0.1 | varsayılan | en-GB |  |
| UY | Uruguay | 0.1 | varsayılan | es-419 |  |
| VC | Saint Vincent ve Grenadinler | 0.1 | varsayılan | en-GB |  |
| VN | Vietnam | 0.1 | varsayılan | en-GB |  |
| BO | Bolivya | 0.0 | varsayılan | es-419 |  |
| BR | Brezilya | 0.0 | varsayılan | pt-BR |  |
| CO | Kolombiya | 0.0 | varsayılan | es-419 |  |
| CR | Kosta Rika | 0.0 | varsayılan | es-419 |  |
| DO | Dominik Cumhuriyeti | 0.0 | varsayılan | es-419 |  |
| EC | Ekvador | 0.0 | varsayılan | es-419 |  |
| FM | Mikronezya | 0.0 | varsayılan | en-US |  |
| GT | Guatemala | 0.0 | varsayılan | es-419 |  |
| HN | Honduras | 0.0 | varsayılan | es-419 |  |
| HT | Haiti | 0.0 | varsayılan | fr-FR | App Store yok |
| JM | Jamaika | 0.0 | varsayılan | en-GB |  |
| LA | Laos | 0.0 | varsayılan | en-GB |  |
| MX | Meksika | 0.0 | varsayılan | es-419 |  |
| NI | Nikaragua | 0.0 | varsayılan | es-419 |  |
| NR | Nauru | 0.0 (tahmin) | varsayılan | en-GB |  |
| PE | Peru | 0.0 | varsayılan | es-419 |  |
| PL | Polonya | 0.0 | varsayılan | en-GB |  |
| PY | Paraguay | 0.0 | varsayılan | es-419 |  |
| SV | El Salvador | 0.0 | varsayılan | es-419 |  |
| VE | Venezuela | 0.0 | varsayılan | es-419 |  |
| VU | Vanuatu | 0.0 | varsayılan | en-GB |  |
| WS | Samoa | 0.0 | varsayılan | en-GB | App Store yok |
| RE | Reunion | 4.2 | varsayılan? | fr-FR | mağaza durumu doğrulanmadı |
| NC | Yeni Kaledonya | 2.8 | varsayılan? | fr-FR | mağaza durumu doğrulanmadı |
| GF | Fransız Guyanası | 0.9 | varsayılan? | fr-FR | mağaza durumu doğrulanmadı |
| GP | Guadeloupe | 0.4 | varsayılan? | fr-FR | mağaza durumu doğrulanmadı |
| PF | Fransız Polinezyası | 0.3 | varsayılan? | fr-FR | mağaza durumu doğrulanmadı |
| MQ | Martinik | 0.2 | varsayılan? | fr-FR | mağaza durumu doğrulanmadı |
| GG | Guernsey | 0.1 | varsayılan? | en-GB | mağaza durumu doğrulanmadı |
| JE | Jersey | 0.1 | varsayılan? | en-GB | mağaza durumu doğrulanmadı |
| PR | Porto Riko | 0.1 | varsayılan? | es-419 | mağaza durumu doğrulanmadı |
| VI | ABD Virjin Adaları | 0.1 | varsayılan? | en-US | mağaza durumu doğrulanmadı |
| GU | Guam | 0.0 | varsayılan? | en-US | mağaza durumu doğrulanmadı |
| IR | İran | 99.8 | — | — | yaptırım / erişim yok |
| EH | Batı Sahra | 99.6 | — | — | Fas üzerinden |
| SY | Suriye | 94.2 | — | — | mağaza durumu doğrulanmadı; ABD yaptırımları 2025'te büyük ölçüde kalktı; Play açılırsa muslim |
| CN | Çin | 1.8 | — | — | Google Play yok |
| CU | Küba | 0.1 | — | — | yaptırım / erişim yok |
| KP | Kuzey Kore | 0.0 | — | — | yaptırım / erişim yok |

## Elle yapılacaklar ve yayın notları

**App Store**

- Ad, alt başlık ve anahtar kelimeler yalnız yeni sürümle değişir: **1.1 gönderimiyle, sahibin onayıyla.** 1.0 yapı 25 ile
  incelemedeyken metin ve görsel yüklenmez. Gönderim sırasında `project.yml` `MARKETING_VERSION` ve `fastlane/Deliverfile`
  `app_version` 1.1 yapılır.
- Yeni dil sürümleri (ms, ur-PK, bn-BD, en-GB, en-AU, en-CA, fr-CA, es-MX, pt-PT, zh-Hant) incelemeden geçer. ur-PK ve bn-BD
  için fastlane 2.234.0 ya da sonrası gerekir (iş akışı Homebrew'dan en yenisini kurar). Bir dil sürümünde ad başka bir
  uygulamada kullanılıyorsa deliver hata verir; o dilin eki değiştirilir.
- Görselsiz dil sürümü birincil dilin görsellerini devralır; bu yüzden 24 klasörün hepsine görsel üretilir.
- Önce "iOS App Store" iş akışı `only_images` ile çalıştırılır (hiçbir şey yüklenmez; görseller artifact olarak gelir),
  sahip görselleri görür; yükleme yayın sayılır ve sahibin onayıyla yapılır.
- Özel ürün sayfası "Vakitler" 1.1 onaylandıktan sonra App Store Connect'te elle açılır (yukarıda).

**Google Play**

- Play geliştirici hesabı henüz yok (şahıs şirketi adına kurum hesabı, D-U-N-S bekleniyor). Özel sayfalar **ancak ilk Play
  yayınından sonra** açılabilir: özel sayfa yayımlanmış uygulama ve varsayılan mağaza sayfası ister, bir ülke ancak uygulama
  orada yayındayken hedeflenebilir.
- Sıra: varsayılan (nötr) sayfa fastlane ile → ilk yayın (sahibin onayıyla) → Play Console'da `muslim`, `secular`,
  `prayer-search` özel sayfaları elle (`mobile/android/store-listings/README.md`) → inceleme.
- Özel sayfanın varsayılan dili en-US seçilir (seçilebiliyorsa): Arnavutça, Boşnakça, Kazakça, Özbekçe, Tacikçe gibi
  çevirimiz olmayan dillerdeki cihazlar İngilizce görür.
- Konuma bağlı hatırlatıcılar Android'de açılırsa (`GeoAlarms.ENABLED`) metinlere eklenir ve Play'de konum beyanı yapılır.
- İsteğe bağlı: mağaza sayfası deneyleri (Play Console) ve App Store'da ürün sayfası iyileştirme; sonuçlar bu belgeye yazılır.
  İkisi de uygulamaya takip kodu eklemeden çalışır.
