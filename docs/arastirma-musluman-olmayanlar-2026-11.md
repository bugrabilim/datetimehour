# Müslüman olmayan kullanıcılar için Saat Tarih (8 Kasım 2026)

Sahibin sorusu: "Bu uygulamayı müslüman olmayanların kullanmasını nasıl cazip hâle getiririz?" Araştırma ajansız yapıldı: kod ve mağaza metinleri incelendi, pazar ve rakipler web'de tarandı. Kaynaklar en sonda.

## Kısa sonuç

Uygulamanın içeriği zaten büyük ölçüde herkese hitap ediyor: saat ve kadranlar, alarm ve uyku, hava, takvim, dünya saatleri, gökyüzü, odak ve onlarca araç. Bunların yanında yalnız bir bölüm (Vakitler ve İbadet) dine özel. Sorun içerikte değil, **vitrinde**: uygulamanın adı, mağaza görselleri, ilk kurulumdaki ilk soru, takvimin varsayılanları ve sitenin menüsü, kullanıcıya önce "namaz uygulaması" diyor. Önerilen yol üç adımlı:

1. Vitrini değiştirmek (ucuz, hızlı).
2. Herkese hitap eden özellikleri öne çıkarmak (çoğu zaten var).
3. Başka inançların ve kültürlerin takvimlerini eklemek (orta–büyük iş).

## Neden önemli

- **Türkiye:** KONDA verilerine göre her 100 yetişkinden 24'ü hiç namaz kılmıyor; düzenli kılanlar 2021'de %44 idi. Kendini ateist ya da inançsız sayanlar 2008'de %2, 2025'te %8. Bu kitlenin çoğu bayram tarihlerine bakar (resmî tatil), ama her gün vakit bildirimi istemez.
- **Diğer diller:** 18 dilin çoğunda kullanıcıların büyük kısmı müslüman değil. Ja, ko, zh, th, es, pt, fr, de, ru, fil ve en pazarları bunlardan. Hindistan'da müslümanlar nüfusun %15'i (Pew, 2020); Avrupa'nın tamamında %6. Müslümanların çoğunlukta olduğu diller ar, id, ur, fa ve tr. Endonezya için ~%87 rakamı Pew'e atıfla üçüncü bir kaynaktan, doğrulanmadı.

## Bugün "dini uygulama" izlenimi veren yerler

Koddan ve mağaza metinlerinden:

| # | Yer | Bugün | Etkisi |
|---|---|---|---|
| 1 | App Store adı | 14 mağaza dilinden 13'ünde adda namaz/dua geçiyor: en "Time & Date: Clock & **Prayer**", de "Uhr & Datum: **Gebetszeiten**", ja "時刻と日付：**礼拝時刻**と時計", es "Hora y Fecha: Reloj y **Oración**"… Yalnız tr adı ("Saat Tarih") nötr | Japonya'da, Brezilya'da ya da Almanya'da arama sonucunda uygulama bir dua uygulaması gibi görünüyor |
| 2 | Alt başlık | en "**Prayer times**, weather, alarms" (namaz önde); tr'de en sonda | Aynı |
| 3 | Anahtar kelimeler | en'de 12 kelimenin 6'sı dini (salah, adhan, qibla, quran, tasbih, ramadan) | Nötr aramalarda (alarm, widget, moon phase, sleep) görünürlük düşük |
| 4 | Mağaza görselleri | 1. görselin başlığı bile "Clock, date and **prayer times**"; 2. görsel Vakitler | İlk izlenim |
| 5 | Tanıtım metni ve açıklama | "clock, prayer times, Qibla, calendar…" | Aynı |
| 6 | İlk kurulum | Beş sorudan ilki Vakitler (`Onboarding.QUESTIONS`: PRAYER, WEATHER, CALENDAR, WORLD, SEASONS) | Uygulamayı açan herkese ilk sorulan şey namaz |
| 7 | Takvim | Varsayılan ülke **her dilde Türkiye** (`cal.country` = "TR"). Dini günler ve kandiller varsayılan açık. Takvim bildirimi açıksa bunlar bildirim olarak da gelir | Almanca ya da Japonca kullanan biri kendi ülkesinin tatilleri yerine Kandil'leri görüyor |
| 8 | Günaydın kartı | Vakitler kapalı olsa da sıradaki vakti gösteriyor (Android `AlarmActivity.kt` `MorningCard`, iOS `Morning.swift`) | Küçük hata: Vakitler'e "Hayır" diyen kişi alarmdan sonra yine vakit görüyor |
| 9 | Site menüsü | Her dilde ikinci sırada "Namaz vakitleri" | Sitenin ilk izlenimi |
| 10 | Hoş geldiniz ızgarası | 9 özellikten 2'si dini (Namaz vakitleri, Kıble) | Hafif; sorun değil, ama dil grubuna göre değişebilir |

Zaten iyi olanlar:

- Ad ("Saat Tarih"), simge ve renkler nötr.
- Vakitler'e "Hayır" denince İbadet kategorisi Tümü'nden tamamen kalkıyor (`AllPrefs.visible`) ve vakit bildirimleri kapanıyor.
- Rozetlerde dini rozet bilerek yok.
- Günün sözü her dilin kendi atasözlerinden geliyor.
- Reklam, hesap ve takip olmaması herkes için güçlü bir neden.

## Öneriler

### A. Vitrin (kısa iş, kod + mağaza metni)

1. **Mağaza adı ve alt başlık:** Müslümanların azınlıkta olduğu mağaza dillerinde (en, de, fr, es, pt, ru, ja, ko, zh, th, hi) ad nötr olsun. Örnek: en "Time & Date: Clock & Widgets", alt başlık "Alarm, weather, world clock". Namaz, kıble, ezan gibi kelimeler anahtar kelime alanında kalır; aramada yine bulunur. ar ve id'de ad olduğu gibi kalır, tr zaten nötr.
   - **Bir uygulama, iki vitrin:** App Store'da 70'e kadar *özel ürün sayfası* açılabiliyor (Ekim 2025'ten beri). Temmuz 2025'ten beri bu sayfalara anahtar kelime atanabiliyor; bu kelimelerle arayan kişi varsayılan sayfa yerine o sayfayı görüyor. Böylece "Gebetszeiten" ya da "prayer times" arayan biri vakit görselli sayfayı görür, "Wecker" ya da "widget" arayan biri nötr sayfayı.
   - **Google Play'de:** 50'ye kadar ülkeye özel mağaza sayfası var. Türkiye, Endonezya ve Arap ülkeleri vakit önde, diğer ülkeler nötr olabilir.
   - Ad değişikliği yeni sürüm gönderimiyle olur; 1.1 gönderiminde, sahibin onayıyla.
2. **Görsel sırası dile göre:** Nötr dillerde sıra saat ve gece modu → widget'lar → hava → uyku ve odak → gökyüzü → dünya saatleri olsun; Vakitler ayrı ürün sayfasında. Görsel başlıkları `captions.json`'da zaten dile göre; sıra `store-screenshots.sh`'ta dile göre ayarlanır.
3. **İlk kurulum:** Soru sırası nötr başlasın, örneğin Hava, Takvim, Dünya, Mevsimler, Vakitler. Ya da dil grubuna göre sırala (ar, id, ur, fa, tr'de Vakitler önde). Kişinin inancını konumdan ya da dilden **tahmin etmeyiz**; yalnız sorunun yerini değiştiririz, soru herkese aynı sorulur.
4. **Takvim varsayılanları:**
   - Ülke kayıtlı değilse cihazın bölgesinden alınsın (30 ülke + Türkiye zaten var).
   - Hicri tarih, kandiller ve dini günler yalnız Vakitler açıkken varsayılan görünsün.
   - Türkiye'nin resmî tatilleri (bayramlar dahil) herkes için kalsın; bunlar dini değil resmî günler.
5. **Günaydın kartı:** Sıradaki vakit yalnız Vakitler açıkken görünsün (iki platformda küçük düzeltme).
6. **Site:** ar, id, ur, fa ve tr dışındaki dillerde menüde Namaz vakitleri sona alınsın; sayfalar ve adresler aynı kalır, SEO etkilenmez.
7. **Hoş geldiniz ızgarası:** Nötr dillerde Kıble yerine Uyku ya da Widget'lar gösterilebilir.

### B. Herkese hitap eden vitrin özellikleri (çoğu var, öne çıkarılmalı)

| Kitle | Bizde olan | Rakip | Ne yapılabilir |
|---|---|---|---|
| Komodin/masa saati isteyenler | 21 saat modeli, 25 kadran, tam ekran, gece modu, iPhone StandBy uyumlu widget'lar | "StandBy Clock" türü uygulamalar ücretsiz ama tasarımların çoğu ücretli (ör. 240+ stilin 125+'ı Premium) | "Ücretsiz komodin saati" ürün sayfası ve görselleri |
| Widget sevenler | Saydam widget, gün ışığına göre renk, 8 şehir, Ay, hava | Widgetsmith vb. abonelikli | Ayrı ürün sayfası |
| Uyku | Akıllı alarm, uyku sesleri, gün doğumu ışığı, kronotip | Sleep Cycle, Alarmy abonelikli | "Abonelik yok" vurgusu |
| Fotoğrafçılar | Altın ve mavi saat, Ay doğuşu, gece gökyüzü, tutulma haritası | PhotoPills ~11 $; Sun Surveyor ücretli; PhotoTime ücretsiz | **Yeni, küçük iş:** "Fotoğraf ışığı" kartı ve bildirimi (altın/mavi saat başlıyor, Ay doğuşu, bulut oranı Open-Meteo'dan); hesap motoru hazır |
| Uzaktan çalışanlar | Dünya saatleri, toplantı planlayıcı, borsa seansları, toplantı oylaması | World Time Buddy | Ürün sayfası + sitede açılış sayfası |
| Öğrenci ve öğretmen | Ders sayacı, pomodoro, sınav zamanlayıcısı, sınıf araçları | Forest, Focus To-Do | Ürün sayfası |
| Spor | Aralık antrenmanı, rutinler, sesli duyuru, Sağlık'a kayıt | Seconds, Tabata uygulamaları | Ürün sayfası |
| Çocuklar ve aileler | Çocuklar için mevsimler ve Ay, bilgi yarışması | — | Ebeveyn odaklı görsel |
| Müzisyen | Metronom | Soundbrenner vb. | Arama kelimesi |

Her kitle için ayrı bir App Store ürün sayfası (anahtar kelimesiyle) ve sitede bir açılış sayfası, ek kod gerektirmeden yapılabilir.

### C. Kapsayıcı takvimler (orta–büyük iş)

İslami içeriğe gösterdiğimiz özen herkese: isteyen açar, kimse için tahmin yapılmaz. Hepsi kendi hesap motorumuzla (Güneş ve Ay boylamı, gün batımı) cihazda hesaplanabilir; dış servis gerekmez.

- **Hristiyan:** Batı ve Ortodoks Paskalya, Noel, Advent, Lent. Paskalya tarihi bilinen bir kuralla hesaplanır; 30 ülkenin resmî tatilleri zaten var.
- **Yahudi:** İbrani tarih, bayramlar, Şabat mum yakma ve Havdala saatleri. Bunlar gün batımından türetilir, vakit hesabına çok benzer. Doğrulama için Hebcal kullanılabilir: API çıktısı CC BY 4.0, kodu GPL; kodu almayız, yalnız sonuçlarla karşılaştırırız.
- **Çin, Kore, Vietnam:** Ay takvimi ve 24 güneş dönemi (立春, 清明…). Güneş dönemleri doğrudan Güneş boylamından hesaplanır, motorumuzda var. Çince, Korece ve Japonca kullanıcılar için güçlü bir özellik.
- **Hindu:** Panchang (tithi, nakshatra) ve büyük bayramlar. Hindistan raporunda (`docs/arastirma-hindistan-2026-11.md`) ayrıntılı.
- **Budist:** Uposatha günleri (Tayland), ay evresinden.
- **Laik:** BM uluslararası günleri ve gökyüzü olayları (tutulmalar, meteor yağmurları zaten var).

Ayrıca tebrik kartlarına nötr vesileler eklenebilir: doğum günü, yılbaşı, yıl dönümü, mezuniyet, yeni iş. Bugün kartların hepsi dini vesile (Cuma, Ramazan, bayram, kandil, hicri yılbaşı).

### D. Ölçme (gizliliği bozmadan)

- **App Store Connect:** her ürün sayfasının gösterim ve indirme oranını ayrı verir. *Ürün sayfası iyileştirme* ile simge ve görseller A/B denenebilir. Bunlar uygulamaya takip kodu eklemeden çalışır.
- **Google Play Console:** mağaza sayfası deneyleri aynı işi görür.
- **Site:** Umami (izinle) dil başına sayfa ziyaretleri.

## Önerilen sıra

| Adım | İş | Boy | Yayın |
|---|---|---|---|
| 1 | Takvim varsayılanları (cihaz bölgesi; dini günler ve hicri tarih Vakitler'e bağlı), günaydın kartı, ilk kurulum soru sırası, site menü sırası, hoş geldiniz ızgarası | K | Site yayını ve TestFlight, sahibin onayıyla |
| 2 | Nötr ad/alt başlık/görsel sırası (azınlık dilleri) ve "namaz vakitleri" anahtar kelimeli ürün sayfası | K–O | 1.1 gönderiminde, sahibin onayıyla |
| 3 | Kitlelere göre ürün sayfaları ve sitede açılış sayfaları; "Fotoğraf ışığı" | O | — |
| 4 | Kapsayıcı takvimler (Paskalya ve Hristiyan günleri → Çin ay takvimi ve güneş dönemleri → Yahudi takvimi ve Şabat → Hindu panchang); nötr tebrik kartları | B | — |

## Sahibe sorular

1. App Store adı: ar ve id dışındaki dillerde adda namaz geçmesin mi? Öneri: evet; namaz kelimeleri anahtar kelimelerde ve ürün sayfasında kalsın.
2. İlk kurulumda Vakitler sorusu herkes için sona mı alınsın, yoksa yalnız nötr dillerde mi? Öneri: dil grubuna göre.
3. Kapsayıcı takvimlerde başlangıç hangisi olsun? Öneri: önce Hristiyan günleri ile Çin ay takvimi ve güneş dönemleri. Kitlesi büyük, hesabı kolay ve motorumuzda hazır.
4. "Fotoğraf ışığı" yapılsın mı?

## Kaynaklar

- KONDA verileri (özet haberler): [BirGün](https://www.birgun.net/haber/konda-genel-muduru-agirdir-oruc-tutanlarin-namaz-kilanlarin-orani-dustu-279611), [Rapor Bülteni: Türkiye 100 kişi olsaydı](https://raporbulteni.substack.com/p/33-konda-turkiye-100-kisi-olsayd-22-06-19), [Medyascope](https://medyascope.tv/2022/01/15/konda-arastirma-her-100-kisiden-31i-kendini-modern-45i-geleneksel-muhafazakar-24u-dindar-muhafazakar-olarak-tanimliyor/), [Yetkin Report](https://yetkinreport.com/2022/01/12/turkiyede-toplum-on-yilda-nereden-nereye-geldi/)
- Pew Research: [Islam was the world's fastest-growing religion 2010–2020](https://www.pewresearch.org/short-reads/2025/06/10/islam-was-the-worlds-fastest-growing-religion-from-2010-to-2020/), [Muslim population change](https://www.pewresearch.org/religion/2025/06/09/muslim-population-change/)
- App Store özel ürün sayfaları (70 sayfa, anahtar kelime atama): [Mobile Action](https://www.mobileaction.co/blog/apple-doubles-the-custom-product-page-limit/), [Adapty](https://adapty.io/blog/custom-product-pages-app-store/), [SplitMetrics](https://splitmetrics.com/glossary/custom-product-page-apple), [iClarified](https://www.iclarified.com/98906/apple-expands-app-store-marketing-tools-with-more-custom-pages-offer-codes). Anahtar kelime atama tarihi üçüncü taraf kaynaklardan; uygulamadan önce Apple belgesinden doğrulanmalı.
- Google Play ülkeye özel mağaza sayfaları: [Play Console](https://play.google.com/intl/en-GB_ALL/console/about/customstorelistings), [Play Yardım](https://support.google.com/googleplay/android-developer/answer/9867158), [AppTweak](https://www.apptweak.com/en/aso-blog/custom-store-listings)
- StandBy saat uygulamaları: [StandBy Clock: Custom Widgets](https://apps.apple.com/app/id6450166379), [Digital Clock Widget · StandBy](https://apps.apple.com/app/id6467921671), [iPhone Life](https://iphonelife.com/content/how-to-use-standby-mode)
- Fotoğrafçı uygulamaları: [PhotoPills fiyatı](https://apppricinglab.com/app/apple/596026805), [AlternativeTo](https://alternativeto.net/software/photopills), [Space.com incelemesi](https://www.space.com/photopills-app-review)
- Hebcal: [Developer APIs](https://www.hebcal.com/home/developer-api), [hebcal-es6](https://github.com/hebcal/hebcal-es6)
