# Web siteleri (ikinci dalga, 3 Kasım 2026)

Yöntem: Playwright (Chromium), masaüstü 1280×800 ve telefon 390×844, `locale tr-TR`, saat dilimi Europe/Istanbul; koyu tema `colorScheme: dark` ile. Görüntüler yalnız scratchpad'de (`rakip2/web/<site>/`), depoya girmedi. Bağlantı ABD çıkışlı bir vekil sunucudan geçtiği için IP'ye bakan siteler konumu "Columbus, Ohio" sandı; çerez bandı AB/TR ziyaretçisine farklı görünebilir (bu yüzden "çerez bandı görülmedi" notları kesin değil). Renkler PIL ile ölçüldü.

Karşılaştırma için kendi canlı sitemizin görüntüleri: `rakip2/web/biz/` (ana sayfa açık/koyu, dünya saatleri, 2026 takvimi, geri sayım, alarm, saat farkı, toplantı planlayıcı, mevsimler; masaüstü + telefon). Bizim zemin `#f7f4ee` (açık), `#11141a` (koyu), kadran `#efebe2`.

## time.is (https://time.is, Türkçe arayüz; SaatKac.info.tr bunun Türkçe kopyası; puan yok, geliştirici Time.is)
- Ücret modeli: reklamlı ücretsiz. Altbilgide "Time.is Ad-free" (reklamsız abonelik) ve "Reklam verin" bağlantıları; sağ üstte kişi simgesi (hesap: reklamsız abonelik, favori yerler). İlk analize göre reklamsız sürüm yeni abone almıyordu; bu turda abonelik sayfası açılmadı (görülmedi).
- İncelenen görüntü: 11 (ana sayfa masaüstü ilk ekran ve açık/koyu tam sayfa, telefon ilk ekran ve tam sayfa; İstanbul sayfası masaüstü tam sayfa + telefon; saat dilimi çevirici `/compare`; Kişiselleştir `/customize`; Widget `/widgets` tam sayfa; 2026 takvimi `/calendar` tam sayfa).
- İlk açılış / tanıtım / ödeme sayfası: tanıtım yok. Çerez bandı görülmedi (ABD IP'si).
- Ana ekran ve gezinme: solda kırmızı kare logo "TİME.İS" (`#cc3355`, beyaz harf, aralıklı büyük harf), sağda yalnız üç gri simge: büyüteç (arama), kişi (hesap), hamburger. Menü bağlantılarının hepsi altbilgide 4 sütunda: Şu anki yerel saat, Saatleri Karşılaştır, Bulunduğun zaman dilimi, Zaman dilimleri, Yaz saati, "This day in history" (çevrilmemiş), Saat, Geri sayım, "Timer" (çevrilmemiş), Takvim, Kompakt takvim, Hafta numarası, Ses · Uygulamalar, Widget, Zaman dilimi haberleri, Bülten, Reklam verin, Kullanım şartları, Gizlilik · UTC, Unix saati, Unix saati dönüştürücüsü · Kişiselleştir, Time.is Ad-free, SSS, Nasıl kullanılır, Hakkında, İletişim. Altında "App Store'dan İndirin" rozeti ve 20 dilde "Saat kaç?" bağlantı satırı ("≫ more languages"; 58 dil).
- Öne çıkan ekranlar:
  - **Ana sayfa:** en üstte kalın başlık **"Saatiniz tam!"**, altında "Time.is'den fark: +0,081 saniye (±0,123 saniye)" (sapma + ölçüm belirsizliği, her yüklemede değişiyor: +0,064 ±0,074; +0,077 ±0,095), sonra "Columbus, Ohio, Amerika Birleşik Devletleri'deki şimdiki saat:" (IP'den konum, bağlantılı). Saat **dev, çok kalın geometrik sans** (Montserrat ağır kesim; rakamlar ~150 px yüksek, `#333333`), saniyeli, iki nokta kare. Sağa yaslı ince yazıyla "3 Kasım 2026, Salı, hafta 45" (tarihte hafta numarası), altında "Güneş: ↑ 07:04 ↓ 17:28 (10s 24d) - Daha fazla bilgi - Columbus'u ana lokasyon olarak kaydedin - Favori lokasyonlarınızı ekleyiniz". Altında 7 açık gri kare çip (`#eeeeee`, köşesiz): New York, Londra, Berlin, İstanbul, Mekke, Pekin, Sidney; kalın şehir adı + saat; telefonda 3'ü görünür ve "daha fazla" bağlantısı.
  - Ana sayfanın altı: koyu (`#333333`) bantta UTC, GMT, CET, Pacific/Mountain/Central/Eastern Time, China/India Standard Time bağlantıları; sonra **şehir bulutu** (nüfusa göre büyüklüğü değişen 60 şehir adı; büyükler beyaz kutu içinde siyah: Londra, New York, Paris, Tokyo…); sonra mor (`#7755aa`) tam genişlik alıntı bandı (BM Genel Sekreteri'nin iklim sözü, İngilizce).
  - **Şehir sayfası (time.is/Istanbul):** başlık "**İstanbul**, Türkiye'de saat kaç" (şehir kalın, gerisi ince), dev saat, tarih + hafta, Güneş satırı; çiplerde seçili şehir koyu gri. Altta bölümler, her biri ayrı renkte tam genişlik bant: (1) açık gri (`#dddddd`) "İstanbul için saat dilimi bilgisi": dev "UTC +3", "Türkiye Time (TRT)", "şimdi Columbus'un 8 saat ötesindedir", maddeler: 2016'dan beri yaz saati yok, saat dilimi haberi bağlantısı ("Turkey makes summer time permanent", "Sadece İngilizce şimdilik"), IANA kimliği Europe/Istanbul. (2) Dolar banknotu fotoğraflı bant + beyaz kart "Güncel döviz kuru için tıklayın" (yeşil yazı; kendi ya da ortak reklamı). (3) Koyu (`#222222`) **"Saat farkı İstanbul'dan" çubuk grafiği**: 22 şehir UTC farkına göre sıralı, ortada dikey kırmızı (`#cc3355`) çizgi, eksi farklar solda beyaz çubuk, artılar sağda (−11 Los Angeles … +8 Sidney; Moskova 0), "Diğer zaman dilimleriyle karşılaştır" çerçeveli düğme. (4) "İstanbul için güneşin doğuşu, güneşin batışı, gün uzunluğu ve güneş zamanı bilgisi": doğuş, batış, gün uzunluğu, **öğlen vakti 12:48** ve "İstanbul'daki yerel saat 48 dakika ötesinde öğlen vaktidir" (güneş saati farkı). (5) Mor harita bandı "Haritada İstanbul": yer, enlem/boylam, **nüfus 15.702.000**, "İstanbul Google Haritalarda açın" düğmesi, düz renkli vektör dünya haritasında kırmızı nokta ve ülke açık mor. (6) "50 en büyük şehirler Türkiye'de": şehir bulutu (Ankara, Bursa, Muratpaşa, Büyükçekmece… ilçeler de karışık).
  - **Saat dilimi çevirici (/compare):** "Lokasyon ya da saat dilimi" + "Zaman 08:00" + "Gün bugün" + "Diğer lokasyon ya da saat dilimleri (opsiyonel)", üç seçenek: Tablo olarak göster / Sadece belirli saati göster / **Geri sayımı göster** (bir andan geri sayım), "Saati karşılaştır" gri düğme.
  - **Kişiselleştir (/customize):** Dil; Saat formatı (24 saat, saniyeyi göster, **ayırıcı seçimi** açılır listesi); **Gün formatı** (açılır liste: "3 Kasım 2026, Salı, hafta 45" gibi kalıplar); **Haftanın ilk günü** (Pazar/Pazartesi…); **Fon** (yazı tipi: "Montserrat / Time Traveler" vb.); Renkler (Beyaz üstüne siyah / Siyah üstüne beyaz); Çeşitli: **"Pencere başlığı olarak zamanı göster"** (sekme başlığında canlı saat, varsayılan açık), sosyal butonları göster, **"Saniye ve milisaniyeleri açıp kapatmak için saatin sağ tarafını tıklayın"**. Arkada soluk dev saat önizleme olarak duruyor.
  - **Widget (/widgets):** "Ücretsiz, kişiselleştirilebilir saat butonu": konum (aramadan), Gösterim (Zaman, Gün, Gündoğuşu/Günbatışı, Gün uzunluğu), saat biçimi (4 seçenek), font ebatı/rengi, arka plan rengi, "Ne gibi gözükecek" canlı önizleme, kopyalanacak kod (iframe değil `<script src="//widget.time.is/t.js">` + `<span>`; site kendi CSS'iyle biçimler). Kullanım şartları: geri bağlantı zorunlu, metin kalıpları belirli; "İleri kullanım" tablosu (template, time_format, date_format, sun_format, coords, callback) ve birden çok saat örneği.
  - **Takvim (/calendar):** "2026 takvim Amerika Birleşik Devletleri için resmi tatiller ile birlikte" (ülke IP'den, bağlantıyla değişir); 4×3 ay ızgarası, **her satırın solunda soluk hafta numarası**, pazar günleri koyu kırmızı kalın, tatiller kırmızı dolu kare, **yaz saati değişim günleri gri kare** ve ayın altında "Yaz saati uygulaması başladı/bitti" bağlantılı satırı, bugün kırmızı halka; her ayın altında o ayın tatil adları; altta tatil listesi, kaynak "Holidays library: Yasumi (MIT)", 2024–2032 yıl düğmeleri.
- Ayarlar: yukarıdaki Kişiselleştir sayfası; ayrıca "Columbus'u ana lokasyon olarak kaydedin" ve "Favori lokasyonlarınızı ekleyiniz" (çiplerin yerine kendi şehirleri; hesapsız çerezle mi hesapla mı görülmedi).
- Widget / kilit ekranı / saat: iOS uygulaması var (App Store rozeti); web widget'ı yukarıda.
- Tasarım: zemin beyaz `#ffffff` (koyu tema yok: `colorScheme: dark` ile aynı beyaz), yazı `#333333`, vurgu `#cc3355`, bantlar `#dddddd`/`#222222`/`#333333`/mor `#7755aa`–`#7755cc`, harita koyu mor `#332244`. Yazı tipi Montserrat (başlık ve saat 800–900 ağırlık, gövde ince 300–400). Köşe yuvarlaklığı hiç yok (kare çip, kare düğme). Bağlantılar alt çizgili gri. Simgeler düz gri glif.
- Reklam yerleri: masaüstünde ilk ekranın sağ altında **yapışkan video oynatıcı** (~400×225, "Bermuda: The King…" haber videosu, oynat düğmesi; telefonda tam genişlik, saatin hemen altında); ilk ekranın altında `#f7f7f7` boş reklam şeridi (yükleniyor halkası); şehir sayfasında saat dilimi bandının altında ve şehir bulutundan sonra ~400 px boş reklam alanı; döviz kuru bandı. Yoğunluk: orta-yüksek (her bölüm arası bir yer).
- Hatalar: "This day in history", "Timer" çevrilmemiş; "Türkiye Time (TRT)" yarı İngilizce; kesme işaretsiz ekler: "İstanbul dan", "İstanbul daki", "2016 den beri", "Türkiye de"; "50 en büyük şehirler"; widget sayfasında "Ücretsiz,kişiselleştirilebilir" (boşluksuz), "Gün uzuluğu", "Paremetre", "öağrılacaktır", "and … parameters' deki"; takvimde ABD tatil adları İngilizce (Juneteenth, Columbus Day) ama "Labor Day" → "Emek ve Dayanışma Günü". "50 en büyük şehir" listesinde Muratpaşa ve Büyükçekmece gibi ilçeler.
- Bizde olmayan ya da farklı olan:
  - Saat doğruluğu başlığı ("Saatiniz tam!" / "X sn ileri") ilk ekranda, ± belirsizlikle — **farklı**: bizde aynı ölçüm var ama yalnız "i" penceresinde (`build.mjs` 306. satır: `data-sync-status` `#info-dialog` içinde; `src/i18n/tr.json` `syncOk` "fark ±{ms} ms"); ana sayfada görünür satır yok. Ayrıca bizde ± belirsizlik ve "saatiniz tam" yargısı tek cümlede değil. Çaba K. Rakipte ücretsiz.
  - Sekme (pencere) başlığında canlı saat — bizde yok (`src/js/` içinde `document.title` yazan kod yok; grep boş). Çaba K. Ücretsiz.
  - Milisaniye gösterimi (saate tıklayınca) — bizde yok (`src/js/main.js`'te milisaniye yok; kronometrede var). Çaba K. Ücretsiz.
  - Haftanın ilk günü seçimi (takvimde Pazar/Pazartesi) — bizde yok (takvim hep Pazartesi; `build.mjs` `monthTable`; ayar yok). Çaba K. Ücretsiz.
  - Tarih biçimi ve saat ayırıcı seçimi — bizde yok (`sth-prefs` yalnız 24/12, saniye, yazı tipi, renk; `src/js/theme.js`, `main.js`). Çaba K. Ücretsiz.
  - Ana tarih satırında hafta numarası ("…Salı, hafta 45") — bizde üst barda tarih var, hafta yok (`data-live="date"`); hafta ayrı sayfada (`/hafta-numarasi/`). Çaba K.
  - Takvim ızgarasında hafta numarası sütunu ve yaz saati değişim günü işareti — bizde yok (`build.mjs` `monthTable`; hafta sütunu yok, DST günü işaretlenmiyor). Çaba K. Ücretsiz.
  - Her ülke için ay ızgaralı yıllık takvim (ülke seçilebilir, 2024–2032) — bizde kısmen: ülke tatilleri yalnız tablo (`/resmi-tatiller/<ülke>/`, `build.mjs`, `src/data/world-holidays.json`), ızgara yalnız Türkiye; geçmiş yıllar (2024–2025) yok (`HOL_YEARS` 2026–2031). Çaba O.
  - Şehir sayfasında "saat farkı çubuk grafiği" (22 şehir, ortada sıfır çizgisi) — bizde farklı: şehir sayfasında fark yazıyla, dünya saatleri sayfasında zaman çizgisi (`src/js/world.js`). Çaba K.
  - Güneş öğle vakti ve "yerel saat güneş saatinden X dk ileride" — bizde kısmen: şehir sayfasında doğuş/batış/gün uzunluğu var (`main.js` `sunrise`/`sunset`/`daylen`), güneş öğlesi ve güneş saati farkı yok (namazda öğle var ama Diyanet düzeltmeli). Çaba K.
  - Şehir sayfasında nüfus, enlem/boylam ve haritada konum — bizde yok (şehir sayfasında harita yok; `cities.json`'da `lat`/`lon` var, nüfus yok). Çaba K.
  - Bir andan geri sayım + çok şehirli karşılaştırmayı tek formda ("Geri sayımı göster") — bizde ayrı ayrı var (`/saat-cevirici/`, `/geri-sayim/?t=`). Fark küçük.
  - Metin satırı olarak gömülen widget (script + span, sitenin kendi CSS'i; gün doğumu/batımı, gün uzunluğu seçeneği) — bizde farklı: iframe gömme (`/gomme/`, `src/js/embed.js`), gün doğumu/batımı seçeneği yok. Çaba O.
  - Şehir bulutu (nüfusa göre büyüyen ad listesi) — tasarım kalıbı; bizde dünya saatleri kart listesi. Gerek değil.
  - Hesapla favori ve "ana lokasyon" — **hesap gerektirir**, bizde hesapsız `sth-world`/`sth-place` ile var.

## timeanddate.com (https://www.timeanddate.com) — OKUNAMADI
- Ana sayfa ve `/worldclock/` Playwright'ta 403 ve Cloudflare "Just a moment… Gerçek kişi olduğunuzu doğrulayın" kutusu verdi (20 sn beklemede de geçmedi; görüntü `rakip2/web/timeanddate/home2-dl.png`, zemin mavi). curl da 403. Wayback Machine 429 verdi. Doğrulama kutusu aşılmaya çalışılmadı. `/calendar/`, `/moon/`, `/sun/`, `/countdown/`, `/time/zones/` bu yüzden görülmedi. İlk analizde de 403 dönmüştü; timeanddate karşılaştırması ancak sahibin tarayıcısından alınacak ekran görüntüleriyle yapılabilir.

## World Time Buddy (https://www.worldtimebuddy.com, Helloka LLC, © 2011–26; puan yok)
- Ücret modeli: reklamlı ücretsiz; "Sign In" (ücretsiz hesap: "Sign in to save settings — it's free!"); takvim bağlama (Settings › Calendars…) hesap ister gibi görünüyor (açılmadı). Pro/ücretli katman bu turda görülmedi. Mobil uygulama tanıtımı üst menüde ("Mobile App").
- İncelenen görüntü: 5 (masaüstü açık ve koyu, telefon, saat karesine tıklama, Settings menüsü).
- İlk açılış / tanıtım / ödeme sayfası: yok; sağda "Getting Started" 4 adımlı liste (1 ADD LOCATIONS, 2 MOUSE OVER HOURS TO CONVERT TIME, 3 CLICK HOUR TILES TO SCHEDULE AND SHARE, + SIGN IN TO SAVE SETTINGS), pembe numaralar. Çerez bandı görülmedi (ABD IP'si).
- Ana ekran ve gezinme: logo + Features, Widgets, Mobile App, sosyal simgeler (Facebook, Pinterest, Twitter, Google+ — eski), sağda Sign In ve **üçlü saat biçimi anahtarı "am/pm | 24 | MX"** (MX = karışık). Sağ kenarda dikey mavi "Feedback" sekmesi.
- Öne çıkan ekranlar:
  - **Çevirici/planlayıcı tablosu** (tek ekran): solda "+ Place or timezone" arama kutusu ve ▲▼ sıralama okları; üstte tarih şeridi (takvim simgesi + 2, **Nov 3** sekmesi, 4, 5, 6, 7, 8 — hafta sonu günleri kırmızı). Her satır: × (kaldır), ev simgesi (ana konum) ya da ana konuma göre fark "+2", "+5"; kalın şehir adı + gri **saat dilimi rozeti** (EST, GMT-3, GMT), altında ülke/eyalet; sağda kalın şimdiki saat "6:47a" ve "Tue, Nov 3"; sonra 24 saatlik kare şeridi (her kare saat + am/pm; gece koyu mavi `#8ba3c9`, sabah/akşam açık mavi, gündüz açık sarı `#fffff3`; gün geçişinde "NOV 4 / WED" kutusu). Şimdiki saat siyah çerçeveli dikey sütun. Fare gezdikçe sütun hareket eder (anında çeviri).
  - **Saat karesine tıklayınca:** yeşil çerçeveli sütun seçilir, sol üstte **sürüklenebilir süre cetveli "|——30 minutes——|"**, satırlarda "10:00a – 10:30a" aralığı; üst çubukta **Outlook / iCal, Google Calendar, Clipboard, Gmail**, "Event" ve **"LINK TO THIS SELECTION"**.
  - **Settings** açılır menü: Show Timezones, **Mark Weekends**, **Calendars…** (takvim bağlama).
  - Alt bölüm: "About this World Clock / Converter" uzun SEO metni (3 paragraf), ortada kesik çizgili boş reklam kutusu (~300×250), sağda Getting Started.
- Ayarlar: yukarıdaki Settings menüsü + saat biçimi anahtarı.
- Widget / kilit ekranı / saat: üst menüde "Widgets" (gömme) ve "Mobile App" (açılmadı).
- Tasarım: zemin dokulu açık gri-bej `#e8e6e3` (ince çapraz desen), panel `#f0f0f0` gölgeli, köşeler hafif yuvarlak (~3 px), yazı Helvetica/Arial benzeri küçük (11–14 px), soluk mavi tonlar, pembe vurgu. Koyu tema yok (`colorScheme: dark` aynı). **Telefonda duyarlı tasarım yok**: 390 px'de masaüstü düzeni küçültülmüş, yazılar okunamayacak kadar küçük (`home-ml.png`).
- Hatalar: altbilgide "Annoucements" (yazım hatası). Google+ simgesi (kapanmış hizmet).
- Bizde olmayan ya da farklı olan:
  - Toplantı süresi seçimi (sürüklenebilir 30 dk… cetvel) ve saat aralığı — bizde yok: planlayıcı tek saat seçer, takvim kaydı sabit 1 saat (`src/js/planner.js` `calLinks`: `end = ms + 3600000`). **Mevcut 2. analiz #72'ye ek ayrıntı** (saat aralığı seçimi). Çaba K. Rakipte ücretsiz.
  - Hafta sonu işareti (tarih şeridinde kırmızı, Settings › Mark Weekends) — bizde yok. **Mevcut #72'ye ek ayrıntı.**
  - Outlook (web), Gmail ile davet, panoya metin olarak kopyalama — bizde kısmen: bağlantı kopyalama, .ics ve Google Takvim var (`planner.js` `shareSetup`, `calLinks`), Outlook web bağlantısı ve "saatleri metin olarak kopyala" yok. Çaba K.
  - Tarih şeridi (bugünden 7 gün sekme) — bizde tarih kutusu var (`<input type=date>`), şerit yok. Çaba K.
  - Satır başında saat dilimi kısaltması rozeti (EST, GMT-3) ve ana konuma göre fark (+2) — bizde satırda yalnız şehir adı (`toplanti-dl.png`); kısaltmalar ayrı sayfada (`/saat-dilimleri/`). Çaba K.
  - Satırları sıralama (▲▼) ve ana konum (ev simgesi) seçimi — bizde planlayıcıda yok (İstanbul/ilk şehir referans). Çaba K.
  - Fareyle gezdikçe canlı sütun (hover ile çeviri) — bizde tıklayınca seçim; hover yok. Çaba K.
  - "MX" karışık saat biçimi — bizde yok; gerek düşük.
  - Takvim bağlama (Calendars…) — **hesap gerektirir**, sitede yapılmayacak.

## vClock (https://vclock.com, Comfort Software Group; puan yok)
- Ücret modeli: reklamlı ücretsiz; hesap yok. "Apps" menüsü (masaüstü/mobil uygulamaları). Yalnız İngilizce (`/tr/` 404, sayfada hreflang yok; Türkçe benzeri onlinesaat.web.tr, aşağıda).
- İncelenen görüntü: 9 (masaüstü açık/koyu/tam sayfa, telefon, "Set Alarm" penceresi, Settings çekmecesi, Tools menüsü, paylaşım çubuğu).
- İlk açılış / tanıtım / ödeme sayfası: yok. Çerez bandı görülmedi (ABD IP'si).
- Ana ekran ve gezinme: mavi (`#0090dd`) üst bar, beyaz yuvarlak hatlı "vClock" logosu; sağda Holidays, Tools ▾ (Alarm Clock, Timer, Stopwatch, Clock), Apps, **yarım daire tema düğmesi** (açık/koyu), dişli. Masaüstünde solda koyu (`#3d3c3c`) dikey simgeli araç çubuğu (Alarm Clock, Timer, Stopwatch, Time; ince çizgi simgeler, seçili açık gri). Telefonda hamburger + "Online Alarm Clock - vClock" başlığı + dişli, altında açılır ok.
- Öne çıkan ekranlar:
  - **Saat kartı:** beyaz kart (`#ffffff`, zemin `#f0f0f0`, köşesiz) ortada **LCD/yedi bölütlü italik yazı tipiyle** saat "2:49:31 PM" (gri `#5a5a5a`), altında aynı yazıyla "TUE - NOV 3, 2026", yeşil (`#4aae71`) kare "Set Alarm" düğmesi. Kartın sağ üstünde dört ince simge: **paylaş, küçült (−), büyüt (+), tam ekran**. Koyu temada zemin siyah `#000000`, rakamlar **turuncu LCD**.
  - **Set Alarm penceresi ("Edit Alarm"):** mavi başlık bandı; Hours (‹ 2 PM ›), Minutes (‹ 50 ›) — açılır liste ve iki yanda ok düğmeleri; **Sound** listesi (Bells…) + ▶ dinle + "…" (daha çok: özel ses/YouTube), **"Repeat sound"** onay kutusu; **Title**; altta **"Test"** (alarmı şimdi dene), Cancel, yeşil Start.
  - Kartın altında iki kutu: "Set the alarm for the specified time" (4:00 AM–2:00 PM arası 22 hazır saat, mavi kare düğmeler) ve **"Recently used"** (son kullanılan alarm saatleri bağlantı listesi + düzenle simgesi).
  - "How to use the online alarm clock" uzun yardım metni (alarm sesi testi, görünüm ayarlarının saklanması, tarayıcı kapanınca çalmaz, internetsiz çalışır, sık kullanılanlara eklenen bağlantı alarmı kurar).
  - Paylaşım kutusu: sayfa adresi kutusu + Facebook, X, WhatsApp, Blogger, Reddit, Tumblr, Pinterest, LinkedIn, e-posta simgeleri ve mavi **"Embed"** düğmesi.
- Ayarlar (sağdan kayan koyu çekmece): **Digital Font** (LCD yazı tipi aç/kapa), 12 hours (am/pm), Show Date, Show Seconds, **Night Mode**, Colors (beyaz, turuncu, kırmızı, yeşil, mavi daireler), mavi OK. Yeşil ON/OFF anahtarlar.
- Widget / kilit ekranı / saat: Apps sayfası (açılmadı).
- Tasarım: düz (flat) Microsoft benzeri; Open Sans; köşesiz kartlar ve kare düğmeler; ince çizgi simgeler. Açık/koyu tema düğmesi var.
- Reklam yerleri: saat kartının hemen altında 728×90 yatay afiş (NBC/Peacock NBA reklamı), koyu temada beyaz boş afiş kutusu. Yoğunluk düşük-orta (ilk ekranda bir afiş).
- Hatalar: görülmedi.
- Bizde olmayan ya da farklı olan:
  - Sayfadaki saati **büyütme/küçültme (+/−)** — bizde sitede yok (`src/js/main.js`'te ölçek yok; uygulamalarda `fs.scale` var). Çaba K. Ücretsiz.
  - **LCD yedi bölütlü yazı tipi** ve koyu temada turuncu/renkli LCD — bizde yok (sitedeki yazı tipi seçenekleri `sth-prefs.font`; LCD yok). **Mevcut 2. analiz #9'a ek ayrıntı** (dijital kadran koleksiyonu; web'de de). Çaba K.
  - Alarm sesini "tekrarla" seçeneği ve alarmı **şimdi deneme ("Test")** — bizde ses dinleme düğmesi var (`src/js/tools.js` 69. satır `testBtn` → `chime`), tam alarm önizlemesi (çalma şeridi + bildirim) yok. Çaba K.
  - Alarm sayfasında **hazır saat düğmeleri ızgarası** ve "son kullanılanlar" — bizde hazır alarm sayfaları var (`/alarm/07-00/`, `ALARM_PRESETS`, `build.mjs`) ama /alarm/ sayfasından bağlantı verilmiyor (CLAUDE.md: "sayfalar arası bağlantı yok"); son kullanılanlar yok (kayıtlı alarm listesi `sth-alarms` var). Farklı; sahibin kararıyla bağlantı yok.
  - Kartta paylaşım çubuğu (9 sosyal ağ) — bizde araç sayfalarında sosyal paylaşım yok (planlayıcıda bağlantı kopyala var). Gerek sahibin kararı. Çaba K.
  - Gece modu ayarı sitede — bizde sitede gece modu yok (uygulamalarda var; sitede koyu tema var). Çaba K.

## Online Alarm Kur (https://onlinealarmkur.com, Türkçe kök adres; `/tr/` 404; puan yok)
- Ücret modeli: ücretsiz; bu turda **reklam görülmedi** (masaüstü ve telefon, tam sayfa). Hesap yok. Altbilgide dil seçici "TR Türkçe" (ilk analize göre 32 dil), YouTube/X/Bluesky simgeleri.
- İncelenen görüntü: 9 (masaüstü açık/koyu/tam sayfa, telefon, Menü, Ayarlar, ampul düğmesi, alarm kurulmuş hâl masaüstü + telefon).
- İlk açılış / tanıtım / ödeme sayfası: yok. Çerez bandı görülmedi.
- Ana ekran ve gezinme: solda hamburger + "Online Alarm Kur / Menü" iki satır, ortada kalın başlık, sağda dişli ve **ampul** simgeleri (alarm kurulunca bir de tam ekran simgesi). Menü **ortada açılan pencere**, üç sütun kart: ZAMANLAYICILAR (Online Alarm Kur, Online Zamanlayıcı, Geri Sayım Sayacı, Online Kronometre), ZAMAN (Online Saat, Dünya Saatleri, Saat Dilimleri, Hafta Numarası), HESAPLAYICILAR (Tarih Hesaplama, Saat Hesaplama, Yaş Hesaplama). Her kart: çerçeveli kare simge kutusu + kalın ad; seçili kart mavi çerçeveli ve mavi yazılı.
- Öne çıkan ekranlar:
  - **Alarm kurma:** üstte "Salı, 3 Kasım 2026", altında **çok büyük ince (light) sans saat** "14:55:46" (açık gri), "Alarm saatini ayarla" + "✎ Düzenle"; Saat/Dakika açılır listeleri; **8 hazır saat** (05:00…08:30, zil simgeli mavi bağlantılar; "Düzenle" ile değiştirilebilir); "Alarm sesi" (Çalar Saat…) + "▷ Sesi test et"; "Alarm başlığı"; tam genişlik açık mavi (`#3abdf8`) "Alarm Kur" düğmesi (siyah yazı, 6 px köşe).
  - **Alarm kurulunca:** büyük **mavi halka** içinde "Alarm", kalan süre "16:02:51" (mavi, büyük), altında turuncu zil "07:00"; altında **yüzde ilerleme çubuğu "0%"**, pembe (`~#f87185`) "Durdur" düğmesi. Telefonda altında **"Alarm geçmişi" tablosu** (#, Başlık, Alarm, Başlangıç, Bitiş, ×), anahtar ile aç/kapa, **"CSV'ye Aktar"** (yeşil çerçeveli) ve "Temizle" (kırmızı çerçeveli) düğmeleri.
  - **Ampul:** "dikkatiniz dağılmadan ekranı açık tutmaya yarayan tamamen karanlık ekran" (görüntüde ekran tümüyle siyah).
  - Çalınca iki düğme: Durdur, "10 dakika ertele" (yardım metninden).
  - **SEO metni:** "Online Alarm Kur Nedir?", adım adım kullanım (5 madde), "Alarm ayarları", SSS (Alarm sesi yüksek mi?, İnternet bağlantısına ihtiyacım var mı?, Sekmeyi kapatırsam alarm çalışır mı?, Bilgisayar uyku modundayken alarm çalışır mı?) ve **"Türkiye'nin Uyku Alışkanlıkları"**: kendi ziyaretçilerine yaptıkları iki anonim anketin sonuçları (en çok yatış 23:00–23:59, kalkış 07:00–07:59, uyku 7–8 saat; en az uyuyan il Mersin 6 sa 14 dk, en çok Konya 7 sa 25 dk). Özgün veri, alıntılanabilir içerik.
- Ayarlar (ortada pencere, beyaz çerçeveli, 16 px köşe): Karanlık mod, **Alarmı tekrarla** ("kapatılana kadar çalmaya devam eder"), **Alarm saatini göster**, **İlerleme çubuğunu göster**, **Alarm geçmişi** ("kurduğunuz alarmları kaydeder"), **Alarm erteleme süresi** kaydırıcı 0–60 dk (10 dk), **Alarm ses seviyesi** kaydırıcı (50). Mavi yuvarlak onay işaretleri.
- Widget / kilit ekranı / saat: görülmedi.
- Tasarım: varsayılan **koyu** (açık tema isteğinde de koyu açıldı; zemin `#010104`, alanlar `#0e1118`), Inter benzeri yazı, saat ince kesim; kartlar ve alanlar 6–8 px köşe; vurgu açık mavi `#3abdf8`, ikincil turuncu (alarm saati) ve pembe (Durdur). Simgeler ince çizgi (Lucide benzeri).
- Hatalar: görülmedi.
- Bizde olmayan ya da farklı olan:
  - Alarm kurulunca **büyük geri sayım halkası + yüzde ilerleme çubuğu** — bizde yalnız tek satır "Sonraki alarm: 07:00 (16 sa 2 dk)" (`src/js/tools.js` 451. satır). **Mevcut 2. analiz #51'e ek ayrıntı** (listede büyük "X sa Y dk sonra çalacak" kartı; web'de de). Çaba K. Rakipte ücretsiz.
  - **Alarm geçmişi tablosu ve CSV'ye aktarma** — bizde yok (alarm listesi `sth-alarms` var, geçmiş yok; CSV yalnız kronometre ve pomodoroda, `tools.js` 150. satır). İlk analizde "alarm geçmişi/CSV" kısmen sayılmıştı; hâlâ yok. Çaba K. Ücretsiz.
  - **Erteleme süresi seçimi ve alarm ses düzeyi** sitede — bizde erteleme sabit 5 dk (`tools.js` 499. satır `5 * 60000`), ses düzeyi ayarı yok (sabit kazanç). Uygulamalarda erteleme seçimi var (CLAUDE.md "Alarm ayarları"). **Mevcut 2. analiz #40'a ek ayrıntı** (alarm başına ses düzeyi; web'de de). Çaba K.
  - **Karartılmış "ışıkları kapat" ekranı** (ampul) — bizde sitede yok (tam ekran var, karartma/gece modu yok). Çaba K.
  - Hazır saatleri kullanıcının düzenleyebilmesi — bizde hazır alarm sayfaları sabit (`ALARM_PRESETS`). Gerek düşük.
  - **Kendi anketinden özgün veri içeriği** ("Türkiye'nin uyku alışkanlıkları") — bizde yok; ankette sunucu ve kişisel veri işleme gerekir (statik site; sahibin kararı). Çaba O.
  - Yaş hesaplama ayrı araç — bizde tarih hesaplamada yaş var (`/tarih-hesaplama/`, `src/js/dates.js`). Var.

## Online-Stopwatch (https://www.online-stopwatch.com; puan yok)
- Ücret modeli: reklamlı ücretsiz + **Premium**: 1 aylık deneme erişimi $10 (tek seferlik), 12 aylık erişim $24/yıl ("günde $0.06", otomatik yenileme yok), Grup hesabı $96/yıl (30 hesap, kişi başı $3.20). Premium'un ilk özelliği "No Ads!". Premium için e-posta + parola hesabı ve çerez şart. Üstte "Log In!", sarı "Join Premium!" düğmesi; zamanlayıcı altında kırmızı "Go Ad Free!".
- İncelenen görüntü: 5 (masaüstü ilk ekran ve tam sayfa, telefon ve tam sayfa, Exam Timers sayfası). Masaüstü görüntüsü Playwright'ın normal yolunda yazı tipi beklerken takıldı, CDP ile alındı.
- İlk açılış / tanıtım / ödeme sayfası: ödeme sayfası metinden okundu (yukarıda); görsel alınmadı. Çerez bandı görülmedi.
- Ana ekran ve gezinme: solda yeşil yukarı + kırmızı aşağı ok logosu, "Online-Stopwatch"; sağda Log In!, Join Premium!, Search, "Change Language" (İngiliz bayrağı). Altında **iki satır renkli hap kategori düğmesi** (her biri kalın koyu çerçeveli, solda beyaz daire içinde simge): Race Timers (kırmızı), Classroom Timers (yeşil), Holiday Timers (mor), Random Name Pickers (turuncu), Random Number Generators (gri), Sensory Timers (bordo), Clocks (turkuaz), Exam Timers (lacivert), Chance Games, Group Generators, Presentation Timers, Tally Counters. Altbilgide aynı kategoriler büyük renkli kutularla + "Dates".
- Öne çıkan ekranlar:
  - **Ana sayfa:** mavi bant içinde koyu hap kutuda tanıtım cümlesi; altında lacivert "www.online-stopwatch.com" çubuğu, iki büyük sütun: "Stopwatch" (dev yeşil yukarı ok) ve "Countdown" (dev kırmızı aşağı ok); altında üç hap düğme: **"Add to My Page!"** (yeşil, kendi sayfana ekle), **"Go Fullscreen!"** (mavi), "Go Ad Free!" (kırmızı).
  - **Exam Timers:** "formal monochrome exam timers… easy to read at the back of a hall", sınav adı, merkez numarası ve tarih yazılabilen, **okuma süresi + sınav süresi** ayrı ayarlanan, "tarayıcı kapanırsa süreyi hatırlar" zamanlayıcılar; 4 tasarım: Basic (yalnız "Time left: 01:30:00" + bilgiler), Exam Timer With Clock (analog saat + bilgiler), With Visual (siyah **pasta dilimi** kalan süre), Exam Timer (okuma süresi + analog saat). Mavi çerçeveli kare kartlar.
  - Classroom Timers şeridi: roket, robot, koşucular, ördek yarışı gibi **çizgi film temalı yarış zamanlayıcıları** (kaydırmalı karusel).
  - Diğer kategoriler (bu turda açılmadı, adlarından): rakamsız duyusal/rahatlatıcı zamanlayıcılar ("No numbers. No Stress."), sunum/konuşma zamanlayıcıları, isim seçici, takım oluşturucu, sayaç (tally), şans oyunları, konuşan saatler.
- Ayarlar: görülmedi.
- Widget / kilit ekranı / saat: "Add to My Page!" (kendi sayfasına ekleme; hesap gerektirebilir, açılmadı).
- Tasarım: beyaz zemin, doygun "oyuncak" renkler, kalın çerçeveli hap düğmeler, parlak 3B ok çizimleri; çocuk ve öğretmen kitlesine yönelik. Koyu tema görülmedi.
- Reklam yerleri: üst menünün altında boş beyaz reklam alanı (~1280×140), tanıtım bandının altında ikinci boş alan, **ekranın altında yapışkan gri reklam şeridi** (telefonda da, "⌄" kapatma okuyla). Yoğunluk yüksek.
- Hatalar: görülmedi.
- Bizde olmayan ya da farklı olan:
  - **Sınav salonu zamanlayıcısı** (sınav adı, salon/merkez no, okuma süresi + sınav süresi, uzaktan okunur tek renkli düzen, kapanınca süreyi hatırlar) — bizde yok (sınav **tarihine** geri sayım var: `src/data/exams.json`, `/geri-sayim/yks/`; salon zamanlayıcısı yok). Türkiye'de ortak yazılı/deneme sınavları için uygun. Çaba K. Rakipte ücretsiz (reklamlı).
  - **Sunum/konuşma zamanlayıcısı** (süre dolarken renk değişimi) — bizde yok (`build.mjs` `MORE_KEYS`: hicri, saat hesaplama, kaç gün kaldı, aralık, satranç, metronom, konuşan saat, burç). Çaba K.
  - **Görsel (rakamsız) zamanlayıcılar**: pasta dilimi, kum saati, renkli çubuk; çocuklar ve sınıf için — bizde yok (geri sayım rakamla; `src/js/tools.js`). Çaba K.
  - Sınıf araçları: isim seçici, rastgele sayı, takım oluşturucu, sayaç — bizde sitede yok (uygulamada zikirmatik sayaç var, sitede yok). İlk analiz tablosunda "yok" yazıyordu, 107'lik listeye alınmamış. Çaba K–O (zaman ürünü kapsamı sahibin kararı).
  - Temalı (yarış, tatil) eğlenceli zamanlayıcılar — bizde yok. Çaba O.

## saat-kac.tr (https://saat-kac.tr, "Türkiye'nin Canlı Saat Sitesi · UTC+3"; puan yok)
- Ücret modeli: reklamlı ücretsiz (Google). Hesap yok.
- İncelenen görüntü: 5 (masaüstü ilk ekran ve tam sayfa, telefon, Matematik Temalı Saat, Hologram Temalı Saat).
- İlk açılış / tanıtım / ödeme sayfası: yok. Çerez bandı görülmedi.
- Ana ekran ve gezinme: koyu lacivert (`#1e2130`) üst bar, solda altın çerçeveli analog saat logolu "saat-kaç.tr" görseli; telefonda hamburger. Masaüstünde **solda uzun kenar menü**: Şu An Saat Kaç? (turuncu vurgulu seçili), Dünya Saatleri, Saat Dilimi Çevirici, Kaçıncı Haftadayız?, 2026 Takvimi; TÜRKİYE İLLERİ (Tüm 81 İl →, İstanbul/Ankara/İzmir/Antalya/Bursa/Adana/Trabzon/Konya Saati); TAM EKRAN SAATLER (Analog, **Hologram Temalı**, **Matrix Temalı**, **Matematik Temalı**); ZAMANLAYICILAR (Online Alarm, Kronometre, Zamanlayıcı, Geri Sayım Sayacı); GERİ SAYIM SAYAÇLARI (LGS, Okul Kapanış, Ramazan Bayramı, Kurban Bayramı, Sevgililer Günü, Anneler Günü, Babalar Günü, Yeni Yıl, YKS); HESAPLAMA ARAÇLARI (Kaç Gün Kaldı?, Kaç Gün Oldu?, Uyku Hesaplama, Yaş Hesaplama, Burç Hesaplama Sayacı, Emeklilik Hesaplama).
- Öne çıkan ekranlar:
  - **Ana sayfa:** görünür ekmek kırıntısı "Ana Sayfa", H1 "Şu An Saat Kaç? Anlık ve Doğru Türkiye Saati", görünür kısa açıklama; beyaz kart (16 px köşe, ince kenarlık) içinde **Dijital | Analog hap anahtarı** (seçili siyah dolu), **çok büyük kalın eşit aralıklı (monospace) rakamlar** "15:03:19" (`#1e2130` benzeri koyu), altında "03.11.2026 · Türkiye İstanbul", renkli küçük haplar: UTC+3 (mavi), Türkiye Saati (yeşil), **Yaz Saati Yok** (turuncu), Europe/Istanbul (mor). "Popüler Ülke Saatleri": **bayraklı** 12 kart (ülke kalın, şehir küçük, saat sağda), "Tüm Dünya Saatleri" hap düğmesi.
  - **SEO metni:** "Türkiye Saati Nasıl Belirleniyor?", "Canlı Saat Ne Demektir?" (sunucu Unix zamanı, `performance.now()`, "/api/time.php ile beş dakikada bir eşitleme, gidiş-dönüş süresi hesaba katılır" — **senkron yöntemini açıkça anlatıyor**), "Türkiye Saati (Europe/Istanbul) Nedir?", "İstanbul Saati ile Türkiye Saati Arasında Fark Var mı?", "Dijital Saat mi, Analog Saat mi?", "Dünya Saatleri ve Saat Farkı Neden Önemlidir?" + tablo (Londra 2–3 saat geride…), "UTC ve GMT Kısaca", "Türkiye Saati Hakkında Sorular" (+ simgeli açılır SSS).
  - **Matematik Temalı Saat:** koyu lacivert ızgaralı kart, saat rakamları matematik ifadeleriyle ("sin(π/2)+5 : 0!−1 ⌊π⌋ : 4" gibi; karekök, üs, faktöriyel), sağ üstte "Tam ekran" hap düğmesi.
  - **Hologram Temalı Saat:** koyu camgöbeği ızgaralı zemin, neon parlayan açık camgöbeği eşit aralıklı rakamlar, "Tam ekran".
- Ayarlar: görülmedi (yalnız Dijital/Analog anahtarı).
- Widget / kilit ekranı / saat: görülmedi.
- Tasarım: beyaz zemin `#ffffff`, koyu üst bar `#1e2130`, Inter benzeri gövde, saat monospace kalın; kartlar 12–16 px köşe, ince gri kenarlık; renkli pastel haplar.
- Reklam yerleri: **çok yoğun**. Her bölümün başında ve kenar menüde Google "Daha fazlasını keşfedin" arama-reklam blokları (Canlı saat uygulaması, Zaman takip hizmeti, Saat dilimi dönüştürücü, Dijital saat satışı, Kol saati modelleri, Analog saat satışı, "time", "Takvim Seç", "Atlas İncele", "Tarih Kitabı"…); metin içindeki sözcüklere eklenen mavi **reklam bağlantıları** (□ simgeli "saati", "saat", "matematik") ve mavi hap reklam ("Zaman takip hizmeti", "Duvar Saati", "Türkiye şehir saatleri" — "Popüler Ülke Saatleri" başlığının yanında bile); tema sayfalarında altta yapışkan afiş (APNIC); SSS öncesi boş reklam alanı. "Türkiye Saati" hapının içine bile reklam bağlantısı girmiş.
- Hatalar: reklam bağlantıları içerik haplarının içine girmiş ("Türkiye □ Saati"); kenar menüde "Burç Hesaplama Sayacı" (adı tuhaf).
- Bizde olmayan ya da farklı olan:
  - **Görünür kısa özet, ekmek kırıntısı ve sayfada SSS** — bizde bilerek görünmez (`.lead` `sr-only`, ekmek kırıntısı boş, SSS yalnız `/sss/`; CLAUDE.md "9 Ekim 2026 ikinci düzenleme turu"). Farklı (sahibin kararı); arama motoru için metin duruyor.
  - **Ana sayfada saat bilgisi hapları** (UTC+3, "Yaz saati yok", IANA adı) — bizde ana sayfada yok (bilgiler `/turkiye-saat-dilimi/` sayfasında). Çaba K.
  - Ana sayfada **Dijital | Analog tek dokunuşla anahtar** — bizde 20 model ok/sayaçla gezilir (`build.mjs` `STAGE_ORDER`); farklı, eksik değil.
  - **Dünya saatlerinde bayrak** — bizde yok (`src/js/world.js`'te bayrak yok). Çaba K.
  - **Temalı tam ekran saatler** (Hologram, Matrix, Matematik ifadeli) — bizde yok. **Mevcut 2. analiz #9 ve #23'e ek ayrıntı** (web'de de; matematik ifadeli saat yeni bir tür). Çaba K.
  - 81 il saat sayfaları — **kararla yapılmayacak** (ilk analiz #103).
  - Uyku hesaplama sayfası — bizde ayrı sayfa yok, 90 dk döngü önerisi hazır alarm sayfalarında (`build.mjs` `ALARM_PRESETS`, CLAUDE.md "Hazır süre ve alarm sayfaları"). Kısmen.
  - "Okul kapanış" ve LGS sayacı — bizde LGS bilerek yok (MEB tarihi açıklanmadı), karne günü sayfası var (`/geri-sayim/karne-gunu/`). Var/karar.

## Canlı Saat (https://canlisaat.com; puan yok)
- Ücret modeli: ücretsiz; bu turda **reklam görülmedi**. Hesap yok. Altbilgide dil "Türkçe", X, Gizlilik Politikası, Kullanım Koşulları.
- İncelenen görüntü: 3 (masaüstü ilk ekran ve tam sayfa, telefon).
- İlk açılış / tanıtım / ödeme sayfası: yok. Çerez bandı görülmedi.
- Ana ekran ve gezinme: **varsayılan koyu** (zemin `#121c22`, açık tema isteğinde de koyu). Masaüstünde solda sabit kenar menü (ince çizgi simgeli): Canlı Saat (camgöbeği `#00bbab` dikey çizgiyle seçili), Analog Saat, Dünya Saatleri, Saat Dilimi Çevirici, Kaçıncı Haftadayız?, 2026 Takvimi; ZAMANLAYICILAR (Online Alarm, Kronometre, Zamanlayıcı, Geri Sayım Sayacı); GERİ SAYIM SAYAÇLARI (Yeni Yıl, **15 Tatil Sayacı**, Ramazan); HESAPLAMA ARAÇLARI (Kaç Gün Kaldı?, Kaç Gün Oldu?, Yaş Hesaplama, Saat Farkı Hesaplama). Sağ üstte tam ekran ve paylaş simgeleri. Telefonda hamburger, "Canlı Saat" başlığı, paylaş.
- Öne çıkan ekranlar:
  - Ana sayfa: "Şu An Saat Kaç?" başlığı, ortada dev **kalın monospace** rakamlar (soluk mavi-gri), altında "📍 İstanbul  📅 3 Kasım 2026" monospace küçük.
  - **"Ülke Saatleri" kartları:** bayrak + "Türkiye Saati", büyük monospace saat, "3 KASIM 2026" aralıklı büyük harf; kart zemini **gündüzse sarı-kahve, geceyse lacivert gradyan**, sağ üstte **sarı (gündüz) ya da beyaz (gece) nokta**. 16 px köşe.
  - SEO: "Canlı Saat Nedir?", "Nasıl Kullanılır?", "Ne İşe Yarar?" (iç bağlantılarla).
- Ayarlar: görülmedi.
- Tasarım: koyu, Inter + monospace; yumuşak gradyan kartlar.
- Hatalar: görülmedi.
- Bizde olmayan ya da farklı olan:
  - **Dünya saati kartında gündüz/gece göstergesi** (renk gradyanı + nokta) — bizde kartta yok (`src/js/world.js`; gündüz/gece yalnız kürede, `src/js/globe.js`). Çaba K. Rakipte ücretsiz.
  - Bayrak — yukarıda (saat-kac.tr).
  - Sağ üstte **sayfayı paylaş** düğmesi — bizde saat sahnesinde paylaş yok (`build.mjs` 290–297. satırlar: 24/12, i, tam ekran, oklar). Çaba K.
  - "15 Tatil Sayacı" (sıradaki tatillere tek sayfada geri sayım) — bizde kısmen: resmî tatiller sayfasında kalan gün var (`/resmi-tatiller/`, `src/js/zones.js` `[data-hol]`), takvim günler listesinde kalan gün (`data-left`). Var.

## OnlineSaat (https://onlinesaat.web.tr; vClock'un Türkçe kopyası)
- Ücret modeli: reklamlı ücretsiz (vClock ile aynı).
- İncelenen görüntü: 2 (masaüstü, telefon).
- Tasarım ve yapı vClock ile birebir aynı: mavi (`#0090dd`) bar, Tatiller, Araçlar ▾, Uygulamalar, tema ve dişli; solda Alarm, Zamanlayıcı, Kronometre, Saat kaç; LCD italik rakamlar "15:02:52", altında **Türkçe tarih normal yazıyla** "Salı – 3 Kasım 2026" (vClock'ta LCD), yeşil "Alarm Etkinleştir" düğmesi; kartın altında boş reklam kutusu. Telefonda başlık "Online Alarm Saati - Alarm K…" (**kesik başlık**, taşma).
- Bizde olmayan ya da farklı olan: vClock bölümündekilerle aynı (saat boyutu +/−, LCD yazı tipi, paylaşım).

## NamazVakti.com (https://www.namazvakti.com; web sitesi olarak, yalnız tasarım ve araçlar)
- Ücret modeli: ücretsiz; ilk ekranda reklam görülmedi. Dil: TÜRKÇE | ENGLISH | العربي + açılır dil listesi (dünya simgesi).
- İncelenen görüntü: 2 (masaüstü, telefon; ilk ekran).
- Ana ekran ve gezinme: koyu kahve-gri (`~#3a3434`) başlık, turuncu-beyaz "NamazVakti.com" logosu ve "DÜNYA ŞEHİRLERİ İÇİN NAMAZ VAKİTLERİ"; ANA SAYFA | BİLGİLER; şehir arama kutusu + açılır ok, turuncu "Ülkeler listesi için tıklayınız", sağda konum düğmesi. Masaüstünde sayfa ortada sabit genişlikte (~995 px) gölgeli kutu, kenarlar gri `~#dcdad6`.
- Öne çıkan ekranlar:
  - Başlıkta şehir ("İstanbul", "İstanbul / Türkiye") ve **Ay evresi resmiyle hicri tarih** "23 Cemâzil-evvel 1448".
  - **Vakit kartı:** "Öğle" (şimdiki vakit, camgöbeği), altında **ilerleme çubuğu** (vaktin ne kadarı geçti), "İkindi'ye kalan zaman 39 dakika"; liste İmsâk, **Sabâh** (imsaktan ayrı satır), Güneş, Öğle (turuncu dolu satır), İkindi, Akşam, Yatsı + "⌄" ile ek vakitler; sağda büyük serif "3 / Kasım 2026 / Salı", koyu kutuda "Mahalli Sâat 15:05:29", "Europe/Istanbul +03:00"; altta "AYLIK VAKİTLER", "YILLIK VAKİTLER".
  - **Kıble kartı:** "Kıble Sâati Vakti 11:06", metal çerçeveli pusula çizimi (kırmızı iğne, Kâbe simgesi), **"Coğrafi kuzeyden kıble açısı 152°" ve "Pusula kuzeyinden kıble açısı 146°"** (manyetik sapma düzeltilmiş ikinci açı, kırmızı).
  - **Harita kartı:** uydu haritası (Leaflet + Esri), şehirde kırmızı iğne ve **Kâbe yönüne yeşil çizgi**, koordinat "41.0000, 29.0000", "Şehirden çıkan yeşil hat, kıble istikametidir."
  - Altta BİLGİLER bağlantıları ("İmsak Vakti Hakkında Mühim Açıklama!..").
- Tasarım: krem kartlar (`~#fdfcf8`), 6 px köşe, Arial benzeri yazı, serif tarih, turuncu (`~#f7941d`) vurgu, camgöbeği ilerleme.
- Hatalar: görülmedi.
- Bizde olmayan ya da farklı olan:
  - Vakit ilerleme çubuğu (şimdiki vaktin ne kadarı geçti) — bizde sitede yok (`src/js/prayer.js`'te çubuk yok; geri sayım yazıyla). Uygulamalarda Güneş/Ay yayı var. **Mevcut 2. analiz #65'e ek ayrıntı** (vakit ekranı görselleri; web'de de). Çaba K.
  - **Kıble haritasında Kâbe'ye çizgi** — bizde sitede yok (`src/js/qibla.js` yalnız SVG kadran ve harita arama bağlantıları, 71–74. satırlar). **Mevcut 2. analiz #67'ye ek ayrıntı** (web'de de; OSM karoları dış hizmet izniyle zaten kürede kullanılıyor). Çaba O.
  - **Pusula kuzeyine göre (manyetik sapmalı) kıble açısı** — bizde yok (`qibla.js` yalnız coğrafi açı; iPhone'da `webkitCompassHeading` manyetik okunuyor, sapma eklenmiyor; Android uygulamasında `GeomagneticField` var). Çaba K.
  - Yıllık vakit çizelgesi sayfada — bizde aylık çizelge var (`prayer.js`), yıllık yalnız açık veri JSON'unda (`/api/v1/prayer-times/<il>/<yıl>.json`). Çaba K.
  - Başlıkta Ay evresi resmiyle hicri tarih — bizde sitede namaz sayfasında hicri tarih yok (hicri çevirici ayrı sayfa; uygulamalarda Vakitler başlığında var). Çaba K.

## Every Time Zone (https://everytimezone.com, Slash7 LLC; puan yok)
- Ücret modeli: ücretsiz + **Pro** (üst menüde "Meetings Pro", "People Pro"; "Share iCal Pro"); "Sign In". Fiyat görülmedi. Altbilgide kendi ürünü Noko'nun reklamı.
- İncelenen görüntü: 2 (masaüstü, telefon).
- Ana ekran ve gezinme: mor gradyan (`~#3a1180`) üst bar, logo + "Never warp your brain with time zone math again."; Timeline, Meetings Pro, People Pro, Sign In. İkinci satırda takvim simgesi + "November 1 – 6" (kesik alt çizgili, tıklanır), mavi "Share Link", "Share iCal Pro", sağda "Customize new".
- Öne çıkan ekranlar:
  - **Zaman çizelgesi:** koyu zemin (`~#1d1f20`), her satırda bayrak + bölge adı kalın + gri kısaltma ve UTC ("PST UTC-8"), satırın üstünde o bölgenin saati ("ÖÖ 4:05"); altında günlere bölünmüş koyu gri **gün çubukları** (gündüz kısmı açık gri, gece koyu; ortada gün adı "November 2"). Dikey **sarı-yeşil (`~#d4f54a`) şimdi çizgisi** ve başında "ÖS 3:05 your local time" etiketi + "link" düğmesi (o anın bağlantısı). Çizgi sürüklenerek zaman değiştirilir (ipucu: çift çizgi tutamaç).
  - Telefonda aynı çizelge daraltılmış; saat solda büyük, bölge adı sağda iki satır.
- Hatalar: arayüz İngilizce iken saatler tarayıcı diline göre "ÖÖ/ÖS" (karışık dil). Görüntüde şimdi çizgisi gün çubuklarında "November 2" etiketli çubuğun üstüne düşüyor (bugün 3 Kasım); etiketlerin çubuk ortasına konmasından mı tarih kaymasından mı olduğu doğrulanmadı.
- Bizde olmayan ya da farklı olan:
  - **Sürüklenebilir "şimdi" çizgisi** ile çok bölgeli zaman çizelgesi ve anın bağlantısı — bizde kısmen: planlayıcıda saate dokunma + bağlantı (`src/js/planner.js`), dünya saatlerinde UTC zaman çizgisi (`src/js/world.js`); sürükleme yok. **Mevcut 2. analiz #72'ye ek ayrıntı.** Çaba O.
  - Ekip/kişi saat dilimleri ("People") ve toplantı yönetimi — **hesap gerektirir** (Pro). Bizde toplantı oylaması bağlantıyla var (`/toplanti-oylamasi/`, `src/js/poll.js`).

## 24TimeZones (https://24timezones.com; puan yok)
- Ücret modeli: reklamlı ücretsiz; üst menüde "Announce event!". Dil seçici "EN ▾" (ilk analize göre 12 dil).
- İncelenen görüntü: 1 (masaüstü ilk ekran).
- Ana ekran: ekmek kırıntısı, sağda **am/pm | 24** düğmeleri ve dil; turuncu kare "24" logolu "timezones", geniş arama kutusu ("e.g. India, London, Japan"); koyu menü çubuğu (World Time, Time Zones, Time Converter, Maps, Clock Widgets, About Us — açılır oklu). Menünün altında ~250 px **boş reklam alanı**, sonra H1 "World Time Clock — what time is it around the world?" (sarı alt çizgi), "Customizable world clock with seconds & World time zones" ve **düz dünya haritası**: mavi tonlu, gece/gündüz eğrisi (gündüz açık mavi), şehir noktaları, kırmızı etiketli şehir saatleri (London 12:06, New York 07:06, Tokyo 21:06), sol üstte arama kutusu, sağda saat, dişli, kalem (düzenle), tam ekran simgeleri.
- Bizde olmayan ya da farklı olan: düz (Mercator benzeri) gece/gündüz haritası — bizde dönen küre (`src/js/globe.js`) aynı bilgiyi veriyor (ilk analiz #104). Var/farklı biçim.

## Savvy Time (https://savvytime.com; puan yok)
- Ücret modeli: ücretsiz; ilk ekranda reklam görülmedi. Menüde "Mobile".
- İncelenen görüntü: 1 (masaüstü ilk ekran).
- Ana ekran: beyaz/çok açık gri zemin (`~#fafafc`), renkli halka logo "Savvy Time"; menü Home (mavi seçili), Converter, Local Time, Map, Unix Time, Timers ▾, Calendar ▾, Mobile. H1 "World clock", altında **"Your list is saved in this browser; nothing is sent to our servers."** Solda 8 px köşeli kart: arama kutusu, **12h | 24h** bölütlü anahtar, "✎ Edit"; satırlar: "Your time / Istanbul time" ve şehirler (kalın ad; altında bayrak + ülke + kısaltma "United Kingdom · GMT"), sağda büyük saat + küçük "pm", altında "UTC+0 · Tue, Nov 3"; altta "Compare these cities at another time →" ve "Your list stays in this browser." Sağda "Major cities now" listesi (şehir mavi bağlantı, saat, UTC), **ertesi günde olan şehirde turuncu "Wed"** (Auckland 1:06 am Wed).
- Bizde olmayan ya da farklı olan:
  - Satırda saat dilimi kısaltması ve bayrak — bizde dünya saatleri kartında yok (`src/js/world.js`: ad, saat, "Sizden X sa ileride"). Çaba K.
  - **Ertesi/önceki gün işareti** ("Wed") — bizde yok: kartta yalnız saat, ofset ve "Sizden X sa ileride" var (`src/js/world.js` 83–85. satırlar; `main.js` `diff` türü gün yazmıyor). Çaba K.
  - Gizlilik cümlesi araç üstünde görünür ("listeniz bu tarayıcıda") — bizde aynı davranış var, cümle gizlilik sayfasında. Çaba K (yalnız metin).

## Bizim site (https://time.bumba.tr) aynı tür sayfalarda, yan yana notlar
Görüntüler `rakip2/web/biz/`: ana sayfa (masaüstü/telefon, açık/koyu, tam sayfa), `/dunya-saatleri/`, `/takvim/2026/`, `/geri-sayim/`, `/alarm/`, `/saat-farki/`, `/toplanti-planlayici/`, `/mevsimler/`.
- **Ana sayfa:** ilk ekranda yalnız rakamlı analog saat (kadran `#efebe2`, zemin `#f7f4ee`; koyuda `#11141a`), altında 24/12 · i · tam ekran · ‹ 1/20 › satırı, sonra dönen dünya ve altbilgi. Ekranda **dijital saat, tarih (üst bardaki dışında), saat dilimi, doğruluk satırı ve hiçbir açıklama yok**; rakiplerin hepsi (time.is, saat-kac.tr, canlisaat, onlinealarmkur, vClock) ilk ekranda büyük dijital saat + tarih + konum gösteriyor. Doğruluk ölçümü yalnız "i" penceresinde (`build.mjs` 306. satır). Rakiplerde olmayan: reklamsızlık, sade düzen, dönen dünya.
- **Çerez bandı:** bizde ilk ekranın altını (~115 px masaüstü, ~210 px telefon) kaplıyor; incelenen rakiplerin hiçbirinde (ABD IP'siyle) çerez bandı görülmedi. Bant metni uzun (4 satır) ama düğmeler eşit; standart madde 14 gereği.
- **Dünya saatleri:** UTC zaman çizgisi üstünde kartlar (rakiplerde yok, özgün); kartta bayrak, saat dilimi kısaltması, gündüz/gece ve gün farkı yok (savvytime, canlisaat, everytimezone'da var).
- **Takvim:** renkli lejant, Ramazan gölgesi, yıl düğmeleri — time.is'ten zengin; hafta numarası sütunu ve yaz saati günü işareti yok, yalnız Türkiye.
- **Saat farkı merkezi (`/saat-farki/`):** "İstanbul – Londra / Birleşik Krallık" kartları yalnız ad gösteriyor; **kartta şimdiki saat ve fark yok** (time.is şehir sayfasındaki çubuk grafik ve savvytime satırları farkı ilk bakışta veriyor). Çaba K.
- **Toplantı planlayıcı:** mesai/erken-geç/gece renkleri ve "Ortak mesai saatleri" cümlesi worldtimebuddy'den daha açıklayıcı; H1 "Toplantı planlayıcı" ile kart başlığı "Toplantı planlayıcı" alt alta yineleniyor (masaüstü ve telefon). Telefonda tablo ilk ekranda görünmüyor (şehir ekleme + lejant önce).
- **Alarm:** gün seçimi, ses, YouTube — vClock/onlinealarmkur'dan zengin; kurulunca büyük geri sayım yok, geçmiş yok, erteleme sabit 5 dk.
- **Geri sayım:** hazır süre hapları (1–60 dk), büyük rakam, ilerleme çubuğu — rakiplerle eşit.

## Bu turda bakılamayanlar / atlananlar
- timeanddate.com: Cloudflare doğrulama kutusu (403); hiçbir sayfası görülmedi (yukarıda).
- Görev listesindeki ezanvakti web sitesi, "saatli maarif takvimi" web sürümü, saatkac.info.tr (time.is'in Türkçe kopyası olduğu time.is altbilgisinde yazıyor), timeanddate tarih hesaplayıcısı ile `/tarih-hesaplama/` karşılaştırması: süre nedeniyle atlandı.
- 24timezones ve savvytime yalnız masaüstü ilk ekran; telefon ve koyu tema alınmadı.
- Çerez bantları: bağlantı ABD çıkışlı olduğu için AB/TR ziyaretçisine gösterilen bantlar görülmedi.
