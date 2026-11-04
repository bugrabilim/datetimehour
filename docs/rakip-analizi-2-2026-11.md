# Rakip analizi 2: ekranda görünenler (3 Kasım 2026)

**Neden ikinci analiz:** İlk analiz (`docs/rakip-analizi-2026-10.md`, 107 iş) mağaza metinlerinden yapıldı. Bu yüzden ekranda görünen ama metinde yazmayan şeyleri kaçırdı:
- kadranlar, duvar kâğıtları, ses kitaplıkları,
- ilk açılış ve tanıtım ekranları,
- ayarlardaki alışkanlıklar,
- oyunlaştırma,
- Pro'da satılan içerik.

Bu analiz ekranın kendisine bakar: ekran görüntüleri, tanıtım videoları ve Pro/abonelik sayfaları. Ayrıntı düzeyi zemin rengine, yazı tipine, düğme biçimine kadar iner.

**Kaynaklar:**
1. Sahibin telefonundan paylaştığı ekran görüntüleri. Sahip görüntü gönderdikçe bu belgeye eklenir.
2. Ajan taraması (iki dalga): App Store ve Google Play ekran görüntüleri, sürüm notları, uygulama içi satın alma listeleri; ikinci dalgada web siteleri (Playwright görüntüleri) ve piksel ölçümlü tasarım incelemesi. Ham notlar `docs/rakip-analizi-2-ham/` klasöründe durur.

**Ücret notu (sahibin kararı, 3 Kasım 2026):**
- Saat Tarih **reklamsız ve ücretsiz**. Belki çok ileride ücretli bir bölüm olabilir.
- "Rakipte ücret" sütunu rakibin o özellik için ne aldığını yazar.
- "Bizde ücret" sütunu üç değerden birini alır:
  - **ücretsiz**: herkese açık kalır.
  - **ileride Pro adayı**: ücretli bölüm açılırsa oraya konabilir; o güne kadar ücretsiz.
  - **—**: yapılmayacak.
  - #25 (bağış) bu üç değerin dışındadır: bağış özellik açmaz, karar sahibindir.
- Temel işler her zaman ücretsiz kalır: saat, alarm, vakitler, takvim. Bu analiz yalnız ileride neyin ücretli olabileceğini not eder; bugün ücretli hiçbir şey yok.

**Durum (4 Kasım 2026, akşam):** 206 işin 202'si bitti (#5 yeni dillerle: İspanyolca, Portekizce, Hintçe; #112 sahibin kararıyla WeatherKit uyarılarıyla; #76 iPhone kısmı App Group ile), 1'i iptal (#79 kulaklıkta alarm, sahibin kararı), 3'ü ücretli sistemle birlikte gelecek (#25, #53, #75). Açık iş yok. Gece turundan sonraki özet: 206 işin 199'u bitti. 4 iş kısmen: #5 (uygulama içi dil seçimi var; yeni diller es/pt/hi sahibin kararıyla bekliyor), #76 (Android yapıldı; iPhone'da duvar kâğıdından kesilmiş widget App Group ister, sahibin adımı), #79 (Android yapıldı; iOS'ta AlarmKit ses yolunu seçtirmiyor), #112 (iOS'ta WeatherKit uyarıları; Türkiye için açık resmî uyarı kaynağı yok). 3 iş ücretli sistemle birlikte gelecek: #25, #53, #75. Açık iş yok. Önceki özet (3 Kasım 2026): 206 iş: 84 yapıldı (#82, bizdeki 8 hata: #87–#93, #186, 1. ajan dalgasının 36 işi: #10, #12, #13, #17, #21, #22, #26–#29, #31, #41–#44, #47–#52, #56, #57, #59–#66, #69–#72, #74; sahibin isteğiyle ajansız; #33 mağaza görselleri; sahibin görüntüleri ve isteği, ajansız: #1–#4, #6–#8, #77, #78, #80, #81, #83, #84, #193; K işleri, site, ajansız: #141, #174–#177, #179, #181–#185, #190–#192; takvim ve zaman: #117, #118, #120, #121, #123–#125, #148, #151, #161), 5 kısmen (#180: nüfus verisi yok; #119: hicri aylarla gezme yok; #5: uygulama içi dil seçimi yapıldı, yeni diller es/pt/hi açık; #76: Android şeffaf widget yapıldı, iPhone'daki duvar kâğıdı kesmesi App Group ister; #79: Android yapıldı, iOS AlarmKit ses yolunu seçtirmiyor), 114 açık (13'ü kullanıcı dostu düzen işi: #194–#206, sahip Seçenek B'yi seçti; notlar `docs/kullanici-dostu/`), 3 ücretli sistemle birlikte gelecek (#53 ve #75 sunucu ve hesap gerektirir; #25 bağış ödeme yöntemleriyle birlikte; sahibin kararıyla şimdi yapılmayacak). Kaynaklar: 1–8 ve 76–84 sahibin ekran görüntüleri; 9–75 birinci ajan dalgası; 85–86 ilk analizde "bitti" sayılıp eksik kalan kısımlar; 87–192 ikinci ajan dalgası (saat siteleri, hava ve takvim, zaman araçları, Ay ve gökyüzü, Google Play/widget/saat kadranları, tasarım; 189–192 doğrulamadan sonra eklendi); 193 sahibin isteği; 194–206 kullanıcı dostu düzen (Seçenek B, sahibin kararı; notlar ve görsel sunum `docs/kullanici-dostu/`).

**Numaralar:** İlk analizden ayrıdır, "2. analiz #N" diye anılır. Numara sabittir. İş bitince yalnız "Durum" değişir, yeni iş sona eklenir. 1–8 numaralı işler 3 Kasım 2026'da ilk analizin 108–115 numaralarından buraya taşındı.

## Liste

| No | İş | Nerede | Çaba | Durum | Rakipte ücret | Bizde ücret | Kaynak |
|---|---|---|---|---|---|---|---|
| 1 | Kadran koleksiyonu: tasarım kadranlar (kronograf, Roma rakamı, metal çerçeve, radar, çift halka, Çin rakamı, dokulu zemin: deniz, yaprak, mermer), her kadranda renk seçimi (5 renk), "Önizleme" düğmeli galeri, "Yenilikler" tanıtımı; kadran sayısı rakiplere bakılarak belirlenir | Uygulama (+ site, widget, saatler) | B | yapıldı (3 Kasım 2026; uygulamalar: 12 kadran, 5 renk, önizlemeli galeri, Yeni kadranlar kartı; widget'lar klasik kadranda, site sonra) | Analog Clock Pro: kadranların çoğu "Premium" (taç simgesi), Pro ömür boyu ₺399,99 (üstü çizili ₺79.998 "50% Off" gösterimi) | ileride Pro adayı (bir kısmı; temel kadranlar ücretsiz) | sahibin ekran görüntüleri (Analog Clock, Draunt, id 6785887940); ajan: Desk Clock ₺199,99 + "All clock styles" ₺49,99, Clock Face ₺149,99, Bauhaus Clock ₺799,99 |
| 2 | Alarm ekranı duvar kâğıdı: kategorili (KPOP Trend, Popüler, Günlük motivasyon, Uzaya doğru, Basit, Gürültülü uyanış) görsel ve hareketli zeminler, tam ekran önizleme ("Seç") | Uygulama (Android; iPhone'da alarm ekranı Apple'ın sistem ekranı, yalnız uygulama açılınca gösterilebilir) | O | yapıldı (3 Kasım 2026; telifsiz: siyah, 8 atmosfer geçişi, 4 hareketli zemin; Android alarm ekranı, iPhone görev ekranı ve günaydın kartı) | Alarmy: kurulumda ücretsiz seçiliyor; Premium ayrıntısı taramada | ücretsiz (temel zeminler); ileride Pro adayı (hareketli) | sahibin ekran görüntüleri (Alarmy) |
| 3 | Uygulamanın kendi alarm sesi kitaplığı: kategorili (KPOP Trend, Popüler, Varsayılan), komik/yüksek sesler ("Cat Alarm", "Kâbus Alarmı"), dinleme dalga simgesi | Uygulama | O | yapıldı (3 Kasım 2026; 10 sentez ses, 3 kategori, dinleme; iOS AlarmMusic yolu, Android zil sesi adresi) | Alarmy: taramada | ücretsiz (telifsiz, kendi sentezimiz ya da açık lisans) | sahibin ekran görüntüleri (Alarmy) |
| 4 | İlk kurulum sihirbazı ve tanıtım ekranları: 3 tanıtım sayfası (sayfa noktaları, kırmızı tam genişlik düğme), sonra 4 adımlı ilk alarm kurulumu (ilerleme çubuğu "2/4": duvar kâğıdı → ses → görev); kadran uygulamasında "Yenilikler" kaydırmalı tanıtım | Uygulama | O | yapıldı (3 Kasım 2026; yeni kurulumda karşılama, özellik seçimi, konum ve bildirim adımları) | ücretsiz (Pro'ya yönlendirir) | ücretsiz | sahibin ekran görüntüleri (Alarmy, Analog Clock Pro) |
| 5 | Uygulama içinde dil seçimi (bayraklı liste, dilin kendi adıyla; sistem ayarına gitmeden) ve yeni diller (İspanyolca, Portekizce, Hintçe) | Uygulama (+ site dilleri) | O | bitti (3 Kasım 2026: uygulama içi dil seçimi, Ayarlar › Dil; 4 Kasım 2026: yeni diller İspanyolca, Portekizce [Brezilya] ve Hintçe: site, uygulamalar, saatler, eklenti, din içeriği, App Store metinleri; sahibin onayıyla, ajansız) | ücretsiz | ücretsiz | sahibin ekran görüntüleri (alarm uygulaması, sürüm 2.2) |
| 6 | Ayarlarda puanla (mağaza değerlendirme penceresi; rakipte uygulama içi "Uygulamamızı seviyor musunuz?" kartı), uygulamayı paylaş, geri bildirim (e-posta), kullanım koşulları, sürüm numarası | Uygulama | K | yapıldı (3 Kasım 2026; Ayarlar › Destek ve paylaşım) | ücretsiz | ücretsiz | sahibin ekran görüntüleri (alarm uygulaması, Analog Clock Pro) |
| 7 | Uyanma oyunlaştırması: uyandırılan evcil hayvan ("Beni uyandır"), puanlar ("Puanlarım"), uyanma raporu (Rapor sekmesi) | Uygulama | O | yapıldı (3 Kasım 2026; civciv, Puanlarım, Rapor; uyanma kaydı yalnız cihazda) | Alarmy: taramada | ücretsiz | sahibin ekran görüntüleri (Alarmy) |
| 8 | Havaya göre hareketli zemin ana ekranda (rakipte yağmur damlalı cam); bizde yalnız tam ekran saatte | Uygulama | K | yapıldı (#193 ile, 3 Kasım 2026; saat ekranında isteğe bağlı) | Alarmy: ücretsiz görünüyor | ücretsiz | sahibin ekran görüntüleri (Alarmy) |
| 9 | Dijital kadran koleksiyonu: yedi bölütlü LED (sönük bölütler de görünür), LCD taklidi, kabartma bölüt, nokta matris, silindir tekerlek, ahşap kasalı mekanik flip, dikey 3 satır | Uygulama (+ site) | O | bitti (4 Kasım 2026, gece turu): dijital saatlerde rakam görünümü seçimi (`digital.style`): yedi bölütlü LED (sönük bölütler görünür), LCD (yeşilimsi zemin), 5×7 nokta matris, silindir (önceki/sonraki rakam soluk) ve dikey (saat, dakika, saniye alt alta); iOS + Android, Saat görünümü'nde canlı örnekle. Ahşap kasalı mekanik flip için mevcut çevirmeli saat + ahşap zemin kullanılır | ₺49,99/kadran; ayda ₺49,99 ya da ömür boyu ₺449,99 | ücretsiz (birkaçı); ileride Pro adayı (çoğu) | Digital Clock Bedside, Zen Flip, Desk Clock |
| 10 | Saniye hareketi seçimi (tık, mekanik akış, kayma) ve uygulamada saniye tık sesi (sitede var, uygulamada yok) | Uygulama | K | yapıldı (3 Kasım 2026) | ücretli | ücretsiz | Bauhaus Clock, Analog Clock ("Ses Açık") |
| 11 | Lüm gece kadranı: siyah kadranda işaretler seçilen renkte (14 renk) ışır; gece/gündüz geçişi için saat çizelgesi (bizde yalnız Güneş'e göre) | Uygulama | K | bitti (3 Kasım 2026, K-6; lüm kadranı 14 ışıma rengi, gece modu "belirli saatlerde") | ₺799,99 (ücretli uygulama) | ücretsiz | Bauhaus Clock |
| 12 | İkili saat dilimi analog kadranı (iki kadran, köşede şehir kodu ve fark) ve aynısının widget'ı | Uygulama | O | yapıldı (3 Kasım 2026) | ücretli | ücretsiz | Bauhaus Clock |
| 13 | Alternatif uygulama simgeleri (kadranla eşleşen) | Uygulama (iOS) | K | yapıldı (3 Kasım 2026) | Premium (Pillars, Bauhaus) | ileride Pro adayı | Bauhaus Clock, Pillars |
| 14 | iPhone kilit ekranında dairesel widget'lar (saat, sıradaki vakit, pomodoro, kronometre; bizde yalnız satır ve dikdörtgen) | Uygulama (iOS) | K | yapıldı (3 Kasım 2026; kilit ekranında dairesel: sıradaki vakit halkası, analog saat, yıl ilerlemesi; pomodoro ve kronometre App Group gerektirdiği için yok) | ücretsiz | ücretsiz | Zen Flip, Widgetsmith |
| 15 | Hava, takvim etkinliği, fotoğraf ve gün doğumu/batımı widget'ları | Uygulama | K–O | yapıldı (3 Kasım 2026; Android: hava, takvim etkinlikleri, fotoğraf, gün doğumu/batımı; iPhone: gün doğumu/batımı; iPhone'da hava widget'ı uzantıya WeatherKit yetkisi, fotoğraf ve takvim App Group ister) | ücretsiz / Premium | ücretsiz | Widgetsmith, Desk Clock |
| 16 | Günün ayeti, Esma, hadis ve sözü widget'ları; günlük söz bildirimi | Uygulama | K | yapıldı (3 Kasım 2026; günün sözü, ayeti, esması, hadisi widget'ı: iPhone'da widget ayarından, Android'de dokunarak; her sabah 08.00'de sessiz "Günün sözü" bildirimi) | Quran Widgets'ta Premium | ücretsiz | Vakit, Quran Widgets, Fazilet |
| 17 | Widget görünüm düzenleyici (renk, yazı tipi, çerçeve, hazır galeri) | Uygulama | B | yapıldı (3 Kasım 2026) | Premium | ileride Pro adayı | Widgetsmith, Zen Flip, Quran Widgets ("Create") |
| 18 | iOS'ta saniyeli analog ve saniyeli çevirmeli widget (bizde saniyeli widget yalnız düz metin) | Uygulama (iOS) | O | bitti (4 Kasım 2026, gece turu): iOS'ta saniyeli çevirmeli saat widget'ı (saat ve dakika kartları dakikada bir, saniye kartı sistem sayacıyla akar). Saniyeli analog widget iOS'ta yapılamaz: WidgetKit ibreyi saniyede döndürmez (Android'de var) | ücretsiz | ücretsiz | Desk Clock, Flip Clock |
| 19 | Dynamic Island'da canlı saat (Apple'ın 8 saat sınırı var) | Uygulama (iOS) | K/O | yapıldı (3 Kasım 2026), sonra kaldırıldı (4 Kasım 2026, sahibin kararı: saat telefonda zaten var); yerine isteğe bağlı hava etkinliği (Hava sekmesinde anahtar, etkinlikte ×, Denetim Merkezi düğmesi; Android'de bildirimde hava, Hızlı Ayarlar kutucuğu) | ücretsiz | ücretsiz | Desk Clock, Clock Face, Digital Clock |
| 20 | Çevirmeli saat ayrıntıları: iki rakam tek kart, dikeyde kartlar alt alta, kartın köşesinde küçük saniye, "PM" etiketi, rakam/kart renk temaları, kapakları gizleme | Uygulama (+ site) | K | bitti (3 Kasım 2026, K-6) | VIP ₺49,99/kadran | ücretsiz; renk temaları ileride Pro adayı | Flip Clock, Fliqlo, Zen Flip |
| 21 | Dijitalde saniye küçük ve tabana hizalı seçeneği (bizde saatle aynı boyda) | Uygulama (+ site) | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Digital Clock, Desk Clock |
| 22 | Dev metin/tabela modu ("İYİ Kİ DOĞDUN") ve saat boyutu %130'a kadar (bizde fs.text küçük satır, fs.scale 0,5–1) | Uygulama | K | yapıldı (3 Kasım 2026) | VIP ₺149,99 | ücretsiz | Fullscreen Clock |
| 23 | Üretken sanat zeminleri (topografik çizgiler, nokta matris dünya haritası, grenli gradyan, sıvı metal), cam kart, odometre gibi dönen dakika (bizde 6 zemin, rakipte 28+) | Uygulama | O | bitti (4 Kasım 2026, gece turu): tam ekran zeminlerine eş yükselti çizgileri, noktalı dünya haritası (karalar Natural Earth dokusundan, gündüz/gece saate göre), grenli renk geçişi ve sıvı metal eklendi (iOS + Android; toplam 11 zemin) | 3 zemin ücretsiz, Pro ₺199,99 | ileride Pro adayı | Clocks - A fun standby mode |
| 24 | Rakam rengini ayrı seçme, dış hatlı rakam, ahşap doku zemin | Uygulama | K | bitti (3 Kasım 2026, K-6) | Premium ₺69,99/ay | ücretsiz | ClockZ, Digital Clock |
| 25 | Bağış (bahşiş) satın alması: özellik açmaz, destek içindir | Uygulama | K | ücretli sistemle birlikte (3 Kasım 2026, sahibin kararı: ödeme yöntemleri eklenince) | ClockZ ₺49,99–499,99 (5 kademe); Vakit ₺10–500 tek seferlik, ₺1.000–10.000 aylık | bağış (isteğe bağlı; karar sahibin) | ClockZ, Vakit, Countdown Widget |
| 26 | Tam ekran saatte kaydırarak araç değiştirme (sola pomodoro, sağa sayaç) | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Zen Flip |
| 27 | Zaman çizelgeli uygulama engeli (ör. hafta içi 08–17; bizde yalnız pomodoro sırasında) | Uygulama | O | yapıldı (3 Kasım 2026) | abonelik ₺579/yıl | ücretsiz | Forest |
| 28 | Nefes egzersizi | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Forest |
| 29 | Telefon yüzüstüyken işleyen odak sayacı ("Flip Phone" / Strict Mode) | Uygulama | K | yapıldı (3 Kasım 2026) | Premium | ücretsiz | Focus To-Do |
| 30 | Doğa ve ortam sesleri kitaplığı (tik-tak, cırcır, dere, kafe, şömine, kütüphane…) ve uyku sesi karışımı (birden çok ses, ayrı ses düzeyi, fotoğraflı kartlar, kategoriler, mini oynatıcı); bizde 5 üretilmiş gürültü | Uygulama | O | bitti (4 Kasım 2026, gece turu): telefonda üretilen doğa sesleri (dere, rüzgâr, şömine, cırcır böceği, tik-tak; Kotlin ve Swift çekirdekte aynı formül, testli) ve uyku seslerinde karıştırıcı (her ses ayrı düzey, `sleep.mix`); pomodoroda da seçilebilir. Fotoğraflı ses kartları yapılmadı (lisanslı görsel yok) | temel sesler ücretsiz, çoğu Premium | ücretsiz (temel); ileride Pro adayı (ek paketler) | Focus To-Do, Flip Clock, ShutEye, Alarmy, Sleep as Android, SuperAlarm |
| 31 | Odak istatistiğinde ay/yıl görünümü; çalıştıkça büyüyen fidan/orman koleksiyonu | Uygulama (+ site) | K/O | yapıldı (3 Kasım 2026) | Pro | ücretsiz | Forest, Focus To-Do |
| 32 | Wear OS kadranında renk seçimi (WFF UserConfigurations) ve daha çok saat kadranı | Wear OS | K | yapıldı (3 Kasım 2026; kadran düzenlenirken 5 vurgu rengi ve analog/dijital görünüm; Google WFF doğrulayıcısıyla sürüm 1) | karışık | ücretsiz | Facer |
| 33 | Mağaza görüntülerinde gerçek ortam fotoğrafı (masa, komodin, MagSafe standı); bizim görüntülerimiz simülatörden | Mağaza | K | yapıldı (3 Kasım 2026, sahip "sen hallet" dedi: fotoğraf yerine çizim; simülatör görüntüleri telefon/iPad çerçevesinde, kâğıt zeminde kısa başlıkla, gece görseli komodin sahnesinde; `mobile/ios/scripts/frame-screenshots.mjs`, başlıklar `fastlane/captions.json` 11 dilde) | — | ücretsiz (ürün özelliği değil, mağaza görseli) | Fliqlo, Clocks, Bauhaus |
| 34 | Tek alarmda birden çok görev (zincir) ve rastgele görev | Uygulama | K | yapıldı (3 Kasım 2026; asıl görevden sonra en çok 2 görev daha, "Rastgele" görev her çalmada matematik/hafıza/yazma/sallamadan biri; iOS + Android) | bilinmiyor / abonelik | ücretsiz | Alarmy (26.42.0), Wayk |
| 35 | Kapatma görevini alarm çalmadan önizleme ("Önizleme" düğmesi) | Uygulama | K | yapıldı (3 Kasım 2026; düzenleyicide "Görevi dene", ses ve kayıt olmadan; iOS + Android) | ücretsiz | ücretsiz | Wayk, Alarm Clock Xtreme |
| 36 | Kamerayla nesne ya da sahne görevi (bardak, diş fırçası, gökyüzü, toplanmış yatak), squat/şınav; cihaz içinde (Vision, ML Kit). Bizdeki fotoğraf görevi yalnız aynı yeri eşleştirir | Uygulama | O/B | bitti (4 Kasım 2026, gece turu): nesne bulma görevi (bardak, diş fırçası/lavabo, gökyüzü, toplanmış yatak, bitki, ayakkabı ya da her sabah rastgele; iOS Vision VNClassifyImageRequest, Android ML Kit görüntü etiketleme, ikisi de telefonda, fotoğraf saklanmaz) ve squat (15 çömelme, ivmeölçer); şınav yok (telefon elde değil) | büyük olasılıkla Premium | ücretsiz | Alarmy, Wayk, SuperAlarm |
| 37 | Sesli okuma görevi (bir sözü yüksek sesle oku; dua/ayet seçeneği sahibin kararına kalır) | Uygulama | O | bitti (4 Kasım 2026, gece turu): sesli okuma görevi (günün sözlerinden biri; yalnız cihaz içi konuşma tanıma: iOS requiresOnDeviceRecognition, Android 12+ createOnDeviceSpeechRecognizer; yoksa yazma görevi; mikrofon izni görevde); dua/ayet seçeneği eklenmedi (sahibin kararına kalır) | abonelik | ücretsiz | Wayk |
| 38 | Işık sensörü görevi ("ışığı aç") | Uygulama (Android) | K | yapıldı (3 Kasım 2026; ortam ışığı ≥ 100 lüks ve başlangıcın 3 katı, 3 sn; sensör yoksa zor matematik; yalnız Android) | bilinmiyor | ücretsiz | Sleep as Android |
| 39 | Ertele ve kapat için ayrı görev; ses tuşu, güç tuşu ve sallamayla ne olacağını seçme | Uygulama (Android) | K | yapıldı (3 Kasım 2026; ertelemek için ayrı görev, ses tuşu ve güç tuşu: ertele/kapat/hiçbiri; sallamayla erteleme zaten vardı; yalnız Android) | ücretsiz | ücretsiz | Alarm Clock Xtreme |
| 40 | Her ertelemede kısalan aralık, belli saatten sonra erteleme kapalı, alarm başına ses düzeyi | Uygulama | K | yapıldı (3 Kasım 2026; kısalan erteleme 10 → 5 → 2 → 1, son erteleme saati; ses düzeyi yalnız Android, AlarmKit ses düzeyi seçtirmiyor) | ücretsiz | ücretsiz | Alarm Clock Xtreme, Loud Alarm |
| 41 | Sallayarak erteleme; alarm ekranında el feneri | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Alarm Clock for Me, Loud Alarm |
| 42 | Sakin ön alarm: asıl alarmdan 30 dk önce kısık doğa sesi | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Alarm Clock Xtreme |
| 43 | Alarm kapanınca seçilen uygulamayı açma | Uygulama (Android) | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Alarm Clock Xtreme |
| 44 | Alarmları tarih aralığında duraklatma (tatil): iç alan skipUntil var, ekranı yok | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Google Saat |
| 45 | Uyanma denetimi uygulama içinde geri sayım halkasıyla (bizde bildirim + 2 dk sonra yedek alarm) | Uygulama | K | yapıldı (3 Kasım 2026; "Uyanık mısınız?" ekranında yedek alarma kalan süre halkası ve "Uyanığım"; Android tam ekran açılır, iPhone uygulama açılınca) | ücretsiz | ücretsiz | Alarmy, SuperAlarm |
| 46 | iOS'ta alarm çalarken uygulama silme engeli (Ekran Süresi onayından sonra denyAppRemoval) | Uygulama (iOS) | K | bitti (4 Kasım 2026, gece turu): görevli alarm sürerken ManagedSettings denyAppRemoval (uygulama silinemez), görev bitince kalkar; FamilyControls dağıtım onayı gelene kadar gizli bayrağın arkasında (Odakta engelleme ile aynı) | bilinmiyor | ücretsiz | SuperAlarm |
| 47 | Uyku düzenliliği, uyku borcu, uyanma saati eğilimi, haftalık eğilim cümleleri (uyku takibi olmadan, alarm kapatma ve yatma anlarından) | Uygulama | O | yapıldı (3 Kasım 2026) | çoğu abonelik | ücretsiz | Sleep Cycle, Rise, ShutEye, Pillow, Wayk, Sleep as Android |
| 48 | Kronotip testi (anketin lisansı denetlenmeli) | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Pillow, Sleep as Android |
| 49 | Uyku planına bağlı alışkanlık hatırlatmaları (kafein, mavi ışık, akşam rutini) ve enerji bloklarını takvime yazma | Uygulama | K | yapıldı (3 Kasım 2026) | abonelik | ücretsiz | Rise |
| 50 | Günlük notunda etiketler (#kahve, #spor) | Uygulama | K | yapıldı (3 Kasım 2026) | — | ücretsiz | Sleep as Android |
| 51 | Alarm satırında görev simgesi; listede büyük "X sa Y dk sonra çalacak" kartı | Uygulama | K | yapıldı (3 Kasım 2026) | — | ücretsiz | Wayk, Alarm Clock Xtreme |
| 52 | Uyku planının kalkış alarmı alarm listesinde planla bağlı ve ayrı başlıklı | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Apple Saat |
| 53 | Arkadaşlarla uyanma grupları, sanal para | Uygulama | — | ücretli sistemle birlikte (3 Kasım 2026, sahibin kararı: şimdi yapılmayacak) | abonelik / jeton | — | Wayk, SleepTown |
| 54 | Vakit başına ayrı ezan sesi, ayrı "önce" süresi ve gün seçimi; vakit satırında ses düğmesi | Uygulama | O | bitti (4 Kasım 2026, gece turu): Bildirimler › Vakte göre ayarlar: her vakit için ayrı önceden bildirim süresi, ses (ezan dahil) ve günler; boşsa genel ayar (iOS + Android, testli) | ücretsiz | ücretsiz | Ezan Vakti Pro, Vakit (14 ses), Muslim Pro, Codeida, Temkinli |
| 55 | Ramazan'da Vakitler ekranında ikinci sayaç (iftar/sahur) ve iftar widget'ı (bizde yalnız Live Activity'de) | Uygulama | K | yapıldı (3 Kasım 2026; Ramazan'da Vakitler'de iftar/sahur sayacı, vakit widget'larında iftar/sahur adı) | ücretsiz | ücretsiz | Ezan Vakti Pro, Pillars, Quran Widgets |
| 56 | Bayram namazı vakti (ana ekran kartı ve dini günler listesi; Diyanet kuralı doğrulanmalı) | Uygulama + site | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Codeida |
| 57 | Dini günler listesi (yıl seçici, sıradaki kartı, kalan gün) ve vakit ekranında yaklaşan dini gün satırı ("Arefe: 3 gün sonra"), üç aylara kalan süre | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Codeida, Azan Time, Fazilet, Ezan Vakti Pro, Vakit |
| 58 | Cuma hatırlatması; Cuma namazı sırasında bildirimleri susturma (Android) | Uygulama | K | yapıldı (3 Kasım 2026; Cuma öğlesinden 45 dk önce hatırlatma; Android'de Cuma namazında sessize alma, en az 60 dk) | ücretsiz | ücretsiz | Vakit, Azan Time, Codeida, Temkinli |
| 59 | Ek vakit satırları: İşrak, Duha, Asr-ı sânî, İsfirar, İştibak, İşa-i sânî, gecenin üçte biri, gece yarısı, teheccüd, seher (temkin kuralları doğrulanmalı) | Uygulama + site | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Temkinli, Türkiye Takvimi, Fazilet, Vakit |
| 60 | Halk takvimi: Eyyâm-ı bahûr, Erbain, Zemheri, Hamsin, cemreler, kocakarı fırtınaları (kaynak doğrulanmalı) | Uygulama + site | K | yapıldı (3 Kasım 2026) (fırtına takvimi resmî kaynak bulunamadığı için yok) | ücretsiz | ücretsiz | Türkiye Takvimi, Temkinli |
| 61 | İbadet istatistikleri: ne zaman kılındığı, yıllık ısı haritası, aylık grafik, haftalık 7 halka (seri ve rozet yok) | Uygulama | O | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Vakit, Pillars, Muslim Pro |
| 62 | Hatim takibi: sure/cüz halkası, tahmini bitiş tarihi | Uygulama | O | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Vakit, e-Diyanet, Azan Time |
| 63 | Kaza hesabı: ömür boyu kaza ("bu hızla ~6 yılda biter"), oruç kazasında sebep; hayız/nifas döneminde takibi duraklatma | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Vakit, Pillars |
| 64 | Vakit satırından "kıldım" işareti, "2/5" halkası, "hepsini işaretle" | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Muslim Pro, Vakit |
| 65 | Vakitler ekranı görselleri: vakit simgeleri, yarım daire gösterge, analog ya da 24 saat kadranı, vakte göre gökyüzü teması, cami fotoğrafı zemin | Uygulama | O | yapıldı (3 Kasım 2026) (cami fotoğrafı zemini yok: lisanslı görsel yok) | Pillars'ta ek temalar Premium | ücretsiz (temel); ileride Pro adayı (ek temalar) | Temkinli, Azan Time, Pillars, Ezan Vakti Pro, Muslim Pro |
| 66 | Kalan süreyi sözle yazma ("1 sa 42 dk sonra ikindi") | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Pillars, Türkiye Takvimi |
| 67 | Kıble: harita üstünde Kâbe'ye çizgi, kamera görünümü, pusula görünümü seçimi (5 tasarım), favori camiler | Uygulama | O | bitti (4 Kasım 2026, gece turu): 5 pusula tasarımı (klasik, sade, altın, gece, yeşil), haritada Kâbe'ye büyük daire çizgisi (iOS Apple Haritalar, Android kendi dokumuz), favori camiler (yıldız; cihazda, iCloud eşitlemesi / Android yedeğiyle), kamera görünümü (yalnız iPhone; Android'de kamera kitaplığı eklenmedi) | ücretsiz | ücretsiz; pusula tasarımları ileride Pro adayı | Muslim Pro, Codeida, Temkinli, e-Diyanet |
| 68 | Resimli, erkek/kadın ayrımlı, adım adım (1/12) namaz ve abdest rehberi; gusül, teyemmüm, rekât kartları (bizde düz metin) | Uygulama | B | bitti (4 Kasım 2026, gece turu): Namaz rehberinin başında resimli anlatım: namaz 9 adım çizimli (erkek/kadın, Hanefî farkları adım altında), abdest 9 adım simgeyle; gusül/teyemmüm kartları yok | ücretsiz | ücretsiz | Vakit (fotoğraf), Codeida (çizim) |
| 69 | Dua kategorileri (Hısnu'l-Müslim tarzı), dua arama, favori, Ramazan gün duası (içerik lisansı denetlenmeli) | Uygulama | O | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Muslim Pro, Azan Time, Codeida, Ezan Vakti Pro |
| 70 | Zikirmatik: kişisel zikir ekleme, kaydırarak geçiş, sallayınca ya da uzun basınca geri alma, tespih boncuğu görünümü | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz; boncuk tasarımları ileride Pro adayı | Vakit, Azan Time, Temkinli, Pillars |
| 71 | Kur'an okuyucuda tema (Kâğıt/Koyu, 6 zemin rengi) ve günlük okuma hedefi | Uygulama | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | Muslim Pro, Temkinli |
| 72 | Toplantı planlayıcıda saat aralığı seçimi, takvim etkinlikleri ve borsa saatleri, hafta sonu işareti, şehir başına saat biçimi | Uygulama (+ site) | K/O | yapıldı (3 Kasım 2026) | tek seferlik ₺149,99 | ücretsiz | Time Buddy |
| 73 | Geri sayımda birim seçimi, gün/saat/dakika/saniye sütunları, "38. doğum günü", renk, arşiv, kilit ekranında ilerleme halkası, tarih geçince ileri sayım (bizde "bitti" yazıyor) | Uygulama | K | bitti (3 Kasım 2026, K-5) | kısmen Premium | ücretsiz; widget tasarlayıcı ileride Pro adayı | Countdown Widget |
| 74 | Cuma, bayram ve kandil tebrik görselleri; iMessage çıkartma paketi | Uygulama | O | yapıldı (3 Kasım 2026) (iMessage çıkartma paketi yok: ayrı uzantı ve paket kimliği ister) | ücretsiz | ücretsiz | Ezan Vakti Pro, Azan Time |
| 75 | Sunucu gerektirenler: ortak zikir/hatim, topluluk, radyo, fetva araması (yalnız "ALO 190" arama düğmesi K) | Uygulama | B | ücretli sistemle birlikte (3 Kasım 2026, sahibin kararı: şimdi yapılmayacak) | ücretsiz | — (hesap ve sunucu yok kararı) | Codeida, Azan Time, Muslim Pro, e-Diyanet |
| 76 | Şeffaf widget: zemini ana ekran duvar kâğıdından kesilmiş gibi görünen (isteğe bağlı bulanık) saat widget'ı; kullanıcı duvar kâğıdının ekran görüntüsünü uygulamaya bir kez yükler. Dokulu (deniz) kadranlı analog widget | Uygulama (iOS) | O | bitti (3 Kasım 2026: Android Şeffaf ve Yarı saydam görünüm; 4 Kasım 2026: iPhone'da duvar kâğıdından kesilmiş widget, App Group group.tr.bumba.time sahip kaydetti; Widget'lar › Şeffaf widget, widget ayarında görünüm Şeffaf + Ekrandaki yeri; yer tablosu topluluk ölçümü, ince ayarlı; cihazda denenmedi) | ücretsiz görünüyor | ücretsiz | Desk Clock (sahibin ekran görüntüsü) |
| 77 | Uygulama içi yazı boyutu ayarı (%85–%145, kaydırıcı ve canlı "örnek metin" önizlemesi); bizde yalnız saat boyutu var, uygulamanın yazıları sistem ayarına bağlı | Uygulama | K | yapıldı (3 Kasım 2026; %85–%145) | ücretsiz | ücretsiz | Alarm/saat uygulaması (sahibin ekran görüntüsü, sürüm 52) |
| 78 | Adlı atmosfer temaları: uygulamanın bütün zemini gradyanla boyanır ("Lavanta Rüyası", "Mars Tozu", "Siber Kireç"); yana kaydırmalı büyük kart seçici, seçili kart tema renginde ışır ve köşesinde onay işareti; bizde 24 tema var ama adsız renk daireleri ve düz ton | Uygulama | K/O | yapıldı (3 Kasım 2026; 8 adlı atmosfer) | ücretsiz görünüyor | ücretsiz (temel); ileride Pro adayı (ek atmosferler) | Alarm/saat uygulaması (sahibin ekran görüntüsü) |
| 79 | Alarmı bağlı kulaklıkta ya da Bluetooth hoparlörde çalma; bağlantı kesilirse telefonda çalar (iOS'ta AlarmKit'le yapılabilir mi doğrulanmalı) | Uygulama | O | iptal (4 Kasım 2026, sahibin kararı: kulaklıkta alarma gerek yok; Android'deki seçenek kaldırıldı). Bildirimler kulaklıkta duyulur (USAGE_NOTIFICATION / iOS sistem bildirim sesi; engelleyen ayar yok) | Alarmy: Pro deneme döneminde görüldü | ücretsiz | Alarmy (sahibin ekran görüntüsü) |
| 80 | İzin istemeden önce açıklama penceresi ("Seni tam zamanında uyandırabilmemiz için alarm iznine ihtiyacımız var" · Şimdi değil / İzin ver); bizde sistem izin penceresi doğrudan açılıyor | Uygulama | K | yapıldı (3 Kasım 2026; bildirim, alarm, konum) | ücretsiz | ücretsiz | Alarmy (sahibin ekran görüntüsü) |
| 81 | Uygulamayı QR kodla paylaşma ekranı (QR + "Paylaş" + "Değerlendir"); ayarlarda "Diğer uygulamalar" satırı (bizde Bumba'nın öbür ürünleri) ve sürüm numarası | Uygulama | K | yapıldı (3 Kasım 2026; QR telefonda üretilir) | ücretsiz | ücretsiz | Fullscreen Clock, alarm/saat uygulaması (sahibin ekran görüntüleri) |
| 82 | Ekran görüntüsü uyarısı: kişisel kayıt içeren ekranlarda (namaz ve oruç takibi, zekât, sabah günlüğü) ekran görüntüsü alınınca üstte 5 sn'lik uyarı; iOS her sürümde, Android 14+ | Uygulama | K | yapıldı (3 Kasım 2026; sıradaki sürümde) | ücretsiz | ücretsiz | Varlık Saati (sahibin ekran görüntüsü) |
| 83 | Açılış ekranı: bekletmeden, sistemin açılış ekranında uygulama simgesi ve tema zemini (bugün iPhone'da boş ekran, Android'de düz renk). Markalı bekletmeli açılış önerilmez | Uygulama | K | yapıldı (3 Kasım 2026; bekletmesiz tema zemini + simge; markalı bekletmeli açılış yapılmadı, önerilmez) | ücretsiz | ücretsiz | Varlık Saati (sahibin ekran görüntüsü) |
| 84 | Ders çalışma sayacı: ders/konu seçimi ("Genel"), çözülen soru sayacı ("Soru"), not, istatistik; sayaç ve pomodoro yan yana; yedi bölütlü sarı LED rakamlar (sönük bölütler görünür) | Uygulama | O | yapıldı (3 Kasım 2026; LED süre ve soru sayacı, not, istatistik, Pomodoro sekmesi) | bilinmiyor | ücretsiz | adı görünmeyen bir çalışma sayacı (sahibin ekran görüntüsü) |
| 85 | Esmaü'l-Hüsna'da hat görseli ve sesli okuma (ses kaydı lisansı gerekir) | Uygulama | O | bitti (4 Kasım 2026, gece turu): Esma kartında yıldız çerçevede büyük Arapça ad (hat görseli yerine yazı; lisanslı hat görseli yok) ve telefonun Arapça sesiyle dinleme (ses kaydı lisansı gerekmez) | ücretsiz | ücretsiz | Muslim Pro, Pillars |
| 86 | Live Activity'de sıradaki 3 vakit (bizde 1) | Uygulama (iOS) | K | yapıldı (3 Kasım 2026; alt satırda sıradaki 3 vakit) | ücretsiz | ücretsiz | Vakit |
| 87 | **Hata:** Hava sekmesinde başlık "14 günlük tahmin", ekranda 7 gün (iOS `WeatherView.swift:123` `prefix(7)`; Android `Weather.kt:126` `forecast_days=7`, `WeatherScreen.kt:156` `take(7)`). Android 14 güne çıkar; iOS'ta WeatherKit'in günlük tahmini 10 gün olduğu için başlık iOS'ta "10 günlük" olmalı (15 dilde yeni metin; WeatherKit sınırı doğrulanmalı) | Uygulama | K | yapıldı (3 Kasım 2026) | — | ücretsiz | 2. dalga ajan: hava-takvim (ham: `hava-takvim.md`); görüntü `tr-hava.png` |
| 88 | **Hata:** Yüzde işareti her dilde aynı yerde: uygulamalarda başta ("Humidity %64"; iOS `WeatherView.swift:73, 80, 111`, Android `WeatherScreen.kt:112, 119, 146`), sitede sonda (`src/js/weather.js:57`). Türkçede başta ("%64"), İngilizcede sonda ("64%") olmalı; dile göre biçimlenmeli (`NumberFormatter` / `Intl.NumberFormat` yüzde biçimi) | Uygulama + site | K | yapıldı (3 Kasım 2026) | — | ücretsiz | 2. dalga ajan: hava-takvim; görüntü `en-hava-koyu.png` |
| 89 | **Hata:** Ay evresi adı yüzdeyle çelişiyor: ad döngünün 8 eşit diliminden seçiliyor (`src/js/nature-calc.js:71` `floor(frac*8+0.5)`; uygulamalar aynı kuralı taşıyor), ekranda "Son dördün · yüzde 63" (widget) ve "Son dördün · yüzde 33" (site). Dördün adı yalnız yarım Ay'a yakın günlerde kullanılmalı. Açık temada dolunay zeminden ayırt edilemiyor (Ay #F2EDD6, zemin #F7F4EE, karşıtlık ~1,06:1) | Uygulama + site | K | yapıldı (3 Kasım 2026) | — | ücretsiz | 2. dalga ajan: gökyüzü (ham: `gokyuzu.md`); görüntü `widget-ay.png` |
| 90 | **Hata:** Sitede tutulma bölgeleri ad yerine kodla yazıyor ("005, 011, 018"): `nature.js:195` tarayıcının `Intl.DisplayNames`'ine güveniyor, denetimdeki headless Chromium 141 sayısal bölge kodlarına (UN M49) ad vermedi (Node 22 "005 → Güney Amerika" veriyor; gerçek Chrome, Safari ve Firefox'ta denenmeli); bölge adları 15 dilde çeviri dosyasına alınmalı. Tutulma haritasının seçicisi UTC tarihini gösteriyor ("20 Şubat"; üstteki listede "21 Şubat 02:13"; `eclipse-map.js:108`). Uygulamalardaki bölge adları ekranda doğrulanmadı | Site | K | yapıldı (3 Kasım 2026) | — | ücretsiz | 2. dalga ajan: gökyüzü |
| 91 | **Hata:** Gece gökyüzü sayfası: açık temada yıldız adı lacivert diskin dışına taşıyor ve beyaz yazı kayboluyor ("Arcturus"); Venüs için başı sonu aynı aralık ("07:00–07:00"); telefonda (390 px) gezegen tablosu taşıyor | Site | K | yapıldı (3 Kasım 2026) | — | ücretsiz | 2. dalga ajan: gökyüzü |
| 92 | **Hata:** Uygulama takviminde yabancı dillerde Türkiye tatilleri ülke adı olmadan görünüyor (Rusçada "Государственный праздник", `ru-takvim.png`); en azından "Türkiye" etiketi gerekir (ülke seçimi ayrı iş) | Uygulama | K | yapıldı (3 Kasım 2026) | — | ücretsiz | 2. dalga ajan: hava-takvim |
| 93 | **Hata:** Toplantı planlayıcı sayfasında H1 ile kart başlığı aynı metinle alt alta yineleniyor; telefonda tablo ilk ekranda görünmüyor | Site | K | yapıldı (3 Kasım 2026) | — | ücretsiz | 2. dalga ajan: web (ham: `web-siteler.md`) |
| 94 | Saat ekranı dengesi: ekranın ~%58'i boş zemin; saat ve denetim satırı dikeyde ortalanır, saat tam genişliğe büyür (30 Ekim "saat ekranında yalnız saat" kararına dokunmaz) | Uygulama | K | bitti (3 Kasım 2026, K-6) | — | ücretsiz | 2. dalga ajan: tasarım (ham: `tasarim.md`, T2); `tr-saat.png`, `tr-ilk-acilis.png`; Fullscreen Clock (rakam ekranın ~%40'ı) |
| 95 | Alt çubuk: seçili sekmede ana rengin açık tonunda hap (#f6e5d6 / koyu #3a2b1d, sitedeki `.nav` ile aynı), yazı 10 → 11 sp. Dolu/çizgi simge çifti zaten var (Android `MainActivity.kt:103–110`, iOS `SaatTarihApp.swift:282`), ama Android'de `Outlined.Public` ve `Outlined.NightsStay` çizimleri dolu göründüğü için iki sekme seçili gibi duruyor: bu iki glif değiştirilmeli | Uygulama | K | bitti (3 Kasım 2026, K-6; hap ana rengin açık tonu, seçili değilken Android'de Language/DarkMode çizgi simgeleri) | — | ücretsiz | tasarım T5 (doğrulamada düzeltildi); Alarmy, Athan Pro, Vakit (yüzen hap) |
| 96 | Boş durumlar için ortak bileşen: 64 pt çizgi simge (ana renk %60) + 17 pt başlık + soluk açıklama + hap eylem düğmesi (bugün tek gri satır: `Countdowns.swift:122`, `Journal.swift:100`, `ToolsTab.swift:183`) | Uygulama | K | bitti (3 Kasım 2026, K-6; alarm, geri sayım, günlük, konum hatırlatıcısı) | — | ücretsiz | tasarım T6; Rise, Alarmy |
| 97 | Kendi çizgi illüstrasyon ve simge takımı (~24 vektör, ana renk + ikinci renk, 2 pt çizgi, temaya göre renk değişir; fotoğraf yok): emojilerin yerine (Mevsimler 🍂🌍🔦, hava, duygu yüzleri) ve namaz rehberi (#68) için. Bugün hava simgeleri Android'de emoji, iOS'ta SF Symbols (iki platform farklı görünüyor); Mevsimler'de iki platformda da emoji (`WorldSeasons.swift:172, 188`) | Uygulama (+ site) | O/B | bitti (4 Kasım 2026, gece turu): kendi çizgi simge takımı (24 simge: hava, mevsim, Güneş/Ay, gök, dünya, takvim, el feneri, termometre, 5 ruh hâli yüzü; 24×24, 2 pt, ana + ikinci renk), tanımlar `mobile/shared/icons/icons.json`; Mevsimler'de ve sabah günlüğünde emojilerin yerine. Namaz rehberi resimleri #68'de | — | ücretsiz | tasarım T7; ajan hava-takvim; Alarmy (beyin çizimi), Codeida (karakter) |
| 98 | Başlık hiyerarşisini tutarlı yapmak: kimi sayfa başlıkları ~20 sp (Ayarlar, `tr-ayarlar.png`), kimileri 26–28 sp (Takvim, Araçlar, Mevsimler); hepsi iOS `.largeTitle` 34 pt, Android `headlineMedium` 28 sp; bölüm başlığı 13 pt (Android'de var, `Common.kt:96`); büyük harf yalnız yerel ayarla | Uygulama | K | bitti (3 Kasım 2026, K-6) | — | ücretsiz | tasarım T8 (doğrulamada düzeltildi); Analog Clock (38–44 pt), Varlık Saati (44 pt) |
| 99 | Liste satırlarında renkli simge kutuları (36 pt, köşe 10 pt, zemin ana rengin açık tonu): Araçlar, alarm görevleri, ayarlar (Araçlar satırlarında renkli simge var, kutu yok; alarm görevlerinde düz satır, `WakeTasks.kt`, `tr-araclar.png`) | Uygulama | K | bitti (3 Kasım 2026, K-6; Araçlar, bildirimler, ayarlar, ibadet) | — | ücretsiz | tasarım T10; Alarmy görevleri (#223c45 + #4dcff0) |
| 100 | Hata ve başarı renkleri uygulamada site belirteçlerinden (`--err` #a31d1d / koyu #ff9d9d, `--ok` #176b3a / #7fd9a1); Android alarm uyarısı bugün Material kırmızısı #d32f2f | Uygulama | K | bitti (3 Kasım 2026, K-6) | — | ücretsiz | tasarım T12; `tr-alarm.png` |
| 101 | Zamanlayıcı, pomodoro ve aralık ekranlarında ilerleme görseli: kalan süre halkası, evre rengiyle dolan tam ekran (çalış yeşil, dinlen kırmızı), tur noktaları ya da domatesler, ince dev rakam, çalışan zamanlayıcı satırı belirgin. Bugün düz kart ve düz sayı (`tr-zamanlayici.png`, `tr-pomodoro.png`, `tr-hiit.png`); halka çizimi yok; yalnız aralık ekranında iki platformda ince çubuk var (`Intervals.swift:645`, `Intervals.kt:544–548`). Rakiplerin hepsinde var; en zayıf ekranlarımız | Uygulama | K/O | bitti (3 Kasım 2026, K-5): pomodoro halkası ve tur domatesleri, aralıkta evre renkli pano, zamanlayıcı satırında halka | ücretsiz | ücretsiz | 2. dalga ajan: araçlar (ham: `araclar.md`); MultiTimer, Interval Timer (#56D495), Seconds (#8AFF34 / #FF4538), Focus Keeper (#FF5C5D), Be Focused, Timeglass, Timer+ |
| 102 | Geri sayım görselleri: fotoğraflı tam ekran ayrıntı (cam kutuda yıl…saniye sütunları), hazır zemin kitaplığı, sıfıra inince konfeti ya da havai fişek (listede renkli kart ve birim gösterimi #73'te) | Uygulama | K/O | bitti (3 Kasım 2026, K-5) | ücretsiz (Days'te filigran kaldırma büyük olasılıkla Pro) | ücretsiz | Countdown (Find Appiness), Countdown Star, Days |
| 103 | Fotoğraf dokulu Ay: NASA SVS "CGI Moon Kit" dokusuyla (svs.gsfc.nasa.gov/4720, `lroc_color_2k.jpg` 2048×1024, 447 KB; SVS "public domain unless otherwise noted", atıf "NASA's Scientific Visualization Studio") her evre cihazda çizilir: sürekli evre, yeryüzü ışığı, güney yarımkürede ters. Bugün uygulamalarda 8 emoji (`MoonView.swift:5`, `MoonScreen.kt:44`, `MoonWidget.swift`, `Widgets.kt:220`; Android'de sarı çizgi film, iPhone'da gri; takvimde 3–4 gün aynı görünüyor), sitede düz iki renkli SVG (`nature.js` moonIcon) | Uygulama + site + widget | O | yapıldı (4 Kasım 2026, gece turu): NASA SVS CGI Moon Kit dokusu (lroc_color_2k, kamu malı, 1024×512) her evre için cihazda çiziliyor (site moon-render.js, Android MoonTexture, iOS MoonTexture): Mevsimler büyük Ay ve Ay takvimi, uygulamada Ay ekranı, kaydırıcı, gökyüzü çizimi, widget'lar; güney yarımkürede ters | ücretsiz | ücretsiz | 2. dalga ajan: gökyüzü; The Moon, MOON, Deluxe Moon, Moon Phases, Lumos, Lumy, Sky Guide |
| 104 | Kronometre: tam ekran ve yatay dev rakam; turu kopyalama. (En hızlı ve en yavaş tur işareti bizde var: iOS `ToolsTab.swift:197–198`, Android `ToolsScreen.kt:216–217`, site `tools.js:175–176`; doğrulamada düzeltildi) | Uygulama | K | bitti (3 Kasım 2026, K-5): tam ekran/yatay dev rakam, turları kopyala | ücretsiz | ücretsiz | Stopwatch‰, Seconds |
| 105 | Paylaşım kartları: hava, Ay ve odak ilerlemesi ("Ders: 3 sa 58 dk") için görsel kart (bugün yalnız vakit ve geri sayım, `ShareCards`) | Uygulama | K | bitti (3 Kasım 2026, K-5): hava, Ay ve odak kartları | ücretsiz | ücretsiz | MGM, MOON, Focus Keeper, Days |
| 106 | Hava ek ölçümleri: rüzgâr yönü, hamle, görüş, çiy noktası, yağış mm, gün doğumu/batımı, basınç birimi seçimi. Bugün 6 değer; Android rüzgâr yönünü ve doğuş/batışı çekiyor ama göstermiyor (`Weather.kt` `windDir`, `Day.sunrise`); sitede yön ve günlük mm var | Uygulama | K | bitti (3 Kasım 2026, K-5): rüzgâr yönü, hamle, görüş, çiy noktası, günlük yağış, doğuş/batış, basınç birimi | ücretsiz | ücretsiz | Weather Live, MGM, The Weather Channel, Yahoo, AccuWeather |
| 107 | Saatlik sıcaklık eğrisi; grafik türünü çiplerle değiştirme (hissedilen, yağış, rüzgâr); saate ya da güne dokununca ayrıntı (gündüz/gece, nem aralığı, yön). Bugün simge şeridi ve dokunulamayan satırlar | Uygulama (+ site) | O | bitti (3 Kasım 2026, gece turu): saatlik eğri ve çipler (sıcaklık, hissedilen, yağış, rüzgâr), dokununca saat ayrıntısı (gündüz/gece, nem, yön); güne dokununca ayrıntı (gündüz/gece, nem aralığı, en güçlü rüzgâr, UV, gün doğumu/batımı); sitede eğri ve çipler | ücretsiz | ücretsiz | CARROT, The Weather Channel, Weather Live, MGM, AccuWeather |
| 108 | Sözlü günlük özet ("Öğleden sonra rüzgârlı") ve ölçüm kartında açıklama cümlesi ("Hafif esinti", "Güneş yanığı riski yok"); şablonla, 15 dilde | Uygulama + site | K | bitti (3 Kasım 2026, K-5): uygulama ve site, ortak kural WeatherWords (Kotlin, Swift, weather-core.js), 15 dil | ücretsiz | ücretsiz | The Weather Channel, Yahoo |
| 109 | Hava sekmesinde Güneş yayı ve Ay kartı (evre, yeni aya kalan gün, doğuş/batış); bunlar bugün Mevsimler, Ay ekranı ve Vakitler yayında | Uygulama | K | bitti (3 Kasım 2026, K-5) | ücretsiz | ücretsiz | CARROT, Yahoo, Weather Live, Fantastical |
| 110 | Hava bildirimleri: "yarın belirgin şekilde soğuk", "15 dk sonra yağmur" (bugün yalnız sabah özeti). Dakikalık yağış: Open-Meteo `minutely_15` var; WeatherKit'in dakikalık tahmininin Türkiye'yi kapsadığı doğrulanmalı | Uygulama | K/O | bitti (3 Kasım 2026, K-5): yarın 5° ve fazla soğuk/sıcak akşam 19.00, yağmurdan 15 dk önce (saatlik tahminden; dakikalık yağış yok) | The Weather Channel ücretsiz, CARROT Premium | ücretsiz | The Weather Channel, CARROT, Yahoo |
| 111 | Hava kalitesi (AQI) ve polen: Open-Meteo Air Quality API (CAMS) var; polen yalnız Avrupa (Türkiye kapsamı doğrulanmalı); WeatherKit'te yok (doğrulanmalı), iOS'ta da Open-Meteo kullanılabilir | Uygulama + site | O | bitti (3 Kasım 2026, gece turu): Open-Meteo Air Quality (CAMS): EAQI, PM2,5, PM10, O₃, NO₂, polen (yalnız Avrupa alanı; yoksa not); iOS'ta da Open-Meteo (WeatherKit'te yok), sitede kart (CSP `air-quality-api.open-meteo.com`, gizlilik metinleri) | ücretsiz | ücretsiz | AccuWeather, The Weather Channel, Yahoo, CARROT |
| 112 | Resmî meteorolojik uyarılar (il renk haritası, başlangıç/bitiş, bildirim). Kaynak sorunu: MGM'nin açık API'si yok; WeatherKit uyarılarının Türkiye kapsamı ve MeteoAlarm lisansı doğrulanmalı; Open-Meteo uyarı vermiyor | Uygulama + site | O/B | bitti (4 Kasım 2026, sahibin kararı: WeatherKit yeterli): iOS'ta WeatherKit resmî uyarıları (Apple'ın kaynak sağladığı ülkelerde; kaynak ve bağlantıyla); başka kaynak aranmayacak | ücretsiz | ücretsiz | MGM, The Weather Channel, Yahoo, CARROT |
| 113 | Etkinliğe uygunluk (koşu, bisiklet, yürüyüş, bahçe): saat başına iyi/orta/kötü ve "en iyi saat" (veri bizde var) | Uygulama | O | bitti (3 Kasım 2026, gece turu): koşu, bisiklet, yürüyüş, bahçe işi için önümüzdeki 12 saat (iyi/orta/uygun değil) ve en iyi saat; kurallar ortak çekirdekte (`Outdoor`, Kotlin + Swift testli) | ücretsiz | ücretsiz | The Weather Channel |
| 114 | Meteogram (3 saatlik tablo: sıcaklık, mm, rüzgâr, hamle, yön) ve model karşılaştırma (yalnız Open-Meteo ile) | Uygulama | O | bitti (3 Kasım 2026, gece turu): 48 saatlik 3 saatlik meteogram (sıcaklık, mm, rüzgâr, hamle, yön oku) ve ECMWF/GFS/ICON karşılaştırması (5 gün, Open-Meteo) | ücretsiz | ileride Pro adayı | Windy |
| 115 | Kayıtlı yerler listesinde anlık sıcaklık; Apple Watch ve Wear OS'ta hava komplikasyonu (WeatherKit kotası düşünülmeli) | Uygulama + saatler | K/O | bitti (3 Kasım 2026, gece turu): kayıtlı yerlerde anlık sıcaklık (K-5) + Apple Watch ve Wear OS'ta hava komplikasyonu (Open-Meteo, 30 dk; saat uygulamasında atıf) | CARROT Premium | ücretsiz | Yahoo, MGM, CARROT, Windy |
| 116 | Takvim görünümleri: ajanda (liste), gün, hafta, yıl; bugün yalnız ay ızgarası (`CalendarTab.swift` `MonthGrid`, `CalendarScreen.kt`) | Uygulama | O | bitti (3 Kasım 2026, gece turu): ay, hafta, gün (24 saatlik zaman çizelgesi; etkinlikler ve namaz vakitleri), ajanda (60 gün), yıl görünümü; `cal.mode` | ücretsiz | ücretsiz | Fantastical, Calendars (Readdle), Google Calendar |
| 117 | Ay hücresinde etkinlik adları; bayram tatili gibi çok günlük dönemler tek şerit (bugün en çok 3 nokta) | Uygulama | K/O | yapıldı (3 Kasım 2026; hücrede etkinlik adı, ardışık günler şerit gibi birleşir; görünüm menüsünden) | ücretsiz | ücretsiz | Calendars (Readdle), Google Calendar, Fantastical |
| 118 | Ajandada ve gün başlığında hava (en yüksek/en düşük + simge) | Uygulama | K | yapıldı (3 Kasım 2026; seçili günün en yüksek/en düşük sıcaklığı ve simgesi) | Fantastical: 3 gün ücretsiz, 10 gün Premium | ücretsiz | Fantastical, Calendars (Readdle) |
| 119 | Miladi hücrede küçük hicri gün; takvimi hicri aylarla gezme (tablo hazır; bugün hicri yalnız seçili gün başlığında ve çeviricide) | Uygulama | K/O | bitti (3 Kasım 2026, gece turu): hücrede küçük hicri gün (K-2) + görünüm menüsünde "Hicri aylarla gez": hicri ay ızgarası (altında miladi gün), önceki/sonraki hicri ay (Diyanet tablosu; `cal.hijriNav`) | ücretsiz | ücretsiz | Osmanlı Takvimi |
| 120 | Rumi tarih (Hicri / Rumi / Miladi yan yana). Kural doğrulanmalı: Serhend Jülyen takvimiyle 13 gün geri ("20 Mayıs 1440"), Osmanlı Takvimi miladi gün ve ayla ("28 Eylül 1441") gösteriyor | Uygulama + site | K | yapıldı (3 Kasım 2026; Jülyen, Türkiye Takvimi geleneği; hicri çevirici ve takvim yaprağı) | ücretsiz | ücretsiz | Serhend Takvimi, Osmanlı Takvimi |
| 121 | Ezani (alaturka) saat: gün batımı 12:00 kabul edilir | Uygulama + site | K | yapıldı (3 Kasım 2026; site namaz il/ilçe sayfaları, uygulamalarda Vakitler) | ücretsiz | ücretsiz | Saatli Maarif Takvimi |
| 122 | Takvimde ülke seçimi ve özel gün türlerini aç/kapa; sitedeki 30 ülkenin tatil verisi (`world-holidays.json`) uygulamaya taşınır (15 dilli ürün için gerekli; ilgili hata #92) | Uygulama | O | bitti (3 Kasım 2026, gece turu): takvimde ülke seçimi (Türkiye + 30 ülke, 2024–2032 ulusal tatiller; dini günler korunur) ve gösterilen gün türleri (`cal.country`, `cal.hidden`); veri `world-holidays.json` gen-strings ile uygulamalara (ad tablosu, 15 dil) | ücretsiz | ücretsiz | Google Calendar (Holidays, Birthdays) |
| 123 | Takvim ızgarasında hafta numarası sütunu, Ay evresi işaretleri ve yaz saati değişim günü (bugün hafta no yalnız seçili günde; sitede Ay takvimi .ics var) | Uygulama + site | K | yapıldı (3 Kasım 2026; uygulama takviminde hafta no, Ay evreleri, saat değişimi günü; site Mevsimler'de Ay takvimi zaten vardı) | Fantastical Premium; time.is ücretsiz | ücretsiz | Fantastical, time.is /calendar |
| 124 | Uygulamada "tarihte bugün" (veri sitede hazır, `on-this-day.json`) ve takvim yaprağında hadis | Uygulama | K | yapıldı (3 Kasım 2026; takvim yaprağında tarihte bugün ve günün hadisi) | ücretsiz | ücretsiz | Saatli Maarif Takvimi, Osmanlı Takvimi |
| 125 | Ayın adının kökeni ya da ay hakkında kısa bilgi (kaynak gösterilerek) | Uygulama + site | K | yapıldı (3 Kasım 2026; site takvim yıl sayfası ve uygulama takvim yaprağı; Türkçede Türkçe adların kökeni, Japoncada geleneksel ay adları, Taycada burç kökeni) | ücretsiz | ücretsiz | Türk Takvimi |
| 126 | Seçili günü zaman çizelgesi olarak gösterme; bayramlara çizimli kart | Uygulama | O | bitti (3 Kasım 2026, gece turu): gün görünümü zaman çizelgesi; bayram günlerinde telefonda çizilen kart (hilal, yıldızlar, fener, tebrik) | ücretsiz | ücretsiz | Structured, Google Calendar |
| 127 | Yıl görünümünde etkinlik yoğunluğu ısı haritası (düşük öncelik) | Uygulama | O | bitti (3 Kasım 2026, gece turu): yıl görünümünde gün başına özel gün + telefon etkinliği sayısına göre koyulaşan ısı haritası | ücretsiz | ücretsiz | Fantastical |
| 128 | Dünya saatlerinde şehir arama: uygulamada yalnız 31 şehir eklenebiliyor (`cities.json`; `WorldScreen.kt:173`, `WorldSeasons.swift:112`), oysa `places.json` (1960 yer) uygulamada zaten var; kısaltmayla arama (EST, CET) | Uygulama | K | bitti (3 Kasım 2026, K-5) | ücretsiz | ücretsiz | World Clock Widgets (Overdesigned, 10.000+ şehir), World Clock Widget (Lasmit), Time Buddy |
| 129 | Dünya saati satırı ve kartı: kişi adı ve emoji ("Annem"), bayrak, saat dilimi kısaltması, gündüz/gece rengi (gök renkli saat kutusu: gündüz #D5E8FF, akşam #BE5446, gece lacivert), ertesi gün işareti, gün doğumu, şehir başına hava; uygulamada şehre dokununca ayrıntı sayfası (harita, GMT farkı, Güneş, Ay; sitede şehir sayfaları zaten var). Bugün satırda saat, fark ve ofset (`WorldScreen.kt` 126–136, site `world.js` 83–85); hava yalnız kürede (kayıtlı yerlerde hava #115 ile birlikte yapılır) | Uygulama + site | K | bitti (3–4 Kasım 2026): kişisel ad/emoji, bayrak, kısaltma, gök renkli saat kutusu, ertesi gün, ayrıntı (K-5); 4 Kasım gece turunda dünya saatlerinde küre / harita / 24 saat kadranı seçimi (`world.view`), gece gölgeli dünya haritası ve şehir başına hava (Android Open-Meteo, iOS WeatherKit + Apple Hava Durumu işareti; saatte bir, `worldWx`) | ücretsiz | ücretsiz | Lasmit, Overdesigned, Samsung Saat, canlisaat, savvytime, everytimezone, saat-kac.tr |
| 130 | Dünya saati widget'ları: saat çakışma ızgarası (Time Overlap), gece gölgeli dünya haritası, sözle saat, 8 şehir, şehir kodu (LAX). Bugün küçükte 2, ortada 4 şehir (`MoreWidgets.swift:142`). Kilit ekranı #14'e, Dynamic Island'da iki şehir #19'a ek | Uygulama | O | bitti (4 Kasım 2026, gece turu): 8 şehir (kısa kod, ad, gündüz/gece işareti, saat), saat çakışması (09–18 mesai şeritleri, ortak saatler çerçeveli, şimdi çizgisi) ve gece gölgeli dünya haritası widget'ları; Android uygulamadaki listeden, iOS widget ayarından (App Group yok, en çok 8 şehir). Sözle saat zaten var (yazıyla tarih widget'ı) | kısmen Premium | ücretsiz (temel); ileride Pro adayı (harita, ızgara) | Lasmit, Overdesigned |
| 131 | 24 saatlik çok ibreli dünya kadranı: üst yarı gündüz, alt yarı gece; her şehir bir ibre; ibreyi sürükleyerek saat çevirme (saat modeli olarak da) | Uygulama + site | O | bitti (4 Kasım 2026, gece turu): Dünya saatlerinde 24 saatlik çok ibreli kadran (üst yarı gündüz, 12.00 tepede; her şehir renkli ibre; kadranı parmakla döndürmek saati kaydırır, 360° = 24 saat). Saat sahnesi modeli olarak eklenmedi: sahnedeki modeller tek saat dilimini gösterir, kadran dünya listesine bağlı | World Clock Pro: abonelik ₺79,99/ay | sitede ücretsiz; uygulamada ileride Pro adayı | World Clock Pro |
| 132 | Uzak şehrin saatine bağlı alarm (telefon başka dilime geçse de o şehrin saatinde çalar) | Uygulama | K | yapıldı (3 Kasım 2026; alarm "Başka bir şehrin saati" ile o şehrin saatinde çalar; listede yerel karşılığı; iOS + Android) | Pro olabilir | ücretsiz | World Clock Widgets (Overdesigned) |
| 133 | Zamanlayıcı ayrıntıları: renk, simge, bitiş saati ("🕓 16:43"; sitede var `data-cd-endsat`, uygulamada yok), ±1 sa / ±10 dk, belirli saatte başlat ya da bitir, kendiliğinden tekrar, bitince ileri say, iptali geri al, klasör (bugün yalnız "+1 dk", `Intervals.kt:398`; `TimerItem`'da renk ve simge alanı yok) | Uygulama | K | bitti (3 Kasım 2026, K-5) | ücretsiz | ücretsiz | Timer+, MultiTimer, Timeglass, Samsung Saat |
| 134 | Süre girişi: hesap makinesi tuş takımı (gün/sa/dk/sn tuşları) ya da cetvel kaydırıcı (bugün ok düğmeleri ve `Stepper`, `Intervals.kt` 521–524) | Uygulama | K | bitti (3 Kasım 2026, K-5) | ücretsiz | ücretsiz | MultiTimer, Interval Timer, Focus Keeper |
| 135 | Rutin ve aralık ekranı: sıradaki adımların listesi ve bitiş saatleri ("Sıradaki: {ad}" satırı var: `Intervals.swift:644`, `Intervals.kt:541–543`), ⏪/⏩ ile adım atlama, duyuru seçenekleri ("sıradaki: …", "yarısı", "10 sn kaldı"), ekran kilidi, bitiş özeti (seri, toplam süre, "yarın hatırlat"), antrenman geçmişi (bugün duyuruda evre adı ve son 3 sn, `HiitConfig.cue`) | Uygulama | K | bitti (3 Kasım 2026, K-5): sıradakiler ve bitiş saatleri, ⏮/⏭, duyuru seçenekleri, ekran kilidi, bitiş özeti, yarın hatırlat, geçmiş | ücretsiz | ücretsiz | Timer+, Seconds, Interval Timer |
| 136 | Zamanlayıcı ve pomodoro widget'ları: etkileşimli, etkin zamanlayıcılar listesi, "bugün 12 sa 45 dk odak", hazır zamanlayıcıyı ana ekrandan başlatma (bugün yalnız kronometre ve zikir widget'ı, `ToolWidgets.swift`) | Uygulama | O | bitti (4 Kasım 2026, gece turu): Android'de zamanlayıcılar widget'ı (süren en çok üç zamanlayıcı geri sayar, 5/10/15/30 dk hazır süreler tek dokunuşla başlar) ve odak widget'ı (bugünkü pomodoro odak süresi, süren çalışma, 25 dk başlat); iOS'ta hazır süreler ve odak widget'ları (dokununca uygulama açılır ve başlatır: saattarih://timer?min=, pomodoro?start=1; iOS widget'ı App Group olmadan uygulamanın süren zamanlayıcılarını okuyamaz, onlar Live Activity'de) | ücretsiz ya da Premium | ücretsiz | Timer+, MultiTimer, Focus Keeper, Flora, Visual Countdown Timer |
| 137 | Apple Watch ve Wear OS'ta zaman araçları: alarm listesi, zamanlayıcı (hazır süreler 5/10/15/20/30), kronometre, pomodoro, aralık (nabızla), geri sayım, dünya saati, zikirmatik, "sıradaki alarm" kutucuğu; çalan alarmı saatten erteleme ya da kapatma. Bugün saat uygulamaları yalnız saat ve vakit gösteriyor (`Watch/Sources/WatchApp.swift`, `wear/.../WearActivity.kt`; tek kutucuk `PrayerTileService`) | Apple Watch, Wear OS | O/B | bitti (4 Kasım 2026, gece turu): Apple Watch ve Wear OS'ta Araçlar: zamanlayıcı (5/10/15/20/30 dk; bitince saatin kendi bildirimi ve titreşim), kronometre, pomodoro (25/5), dünya saati (Apple Watch'ta iPhone'daki listenin ilk 8 yeri eşitlenir; Wear OS'ta 7 büyük şehir), zikirmatik. Telefon alarmları saate zaten yansıdığı için saatte ayrı alarm listesi ve nabızlı aralık antrenmanı yapılmadı | Google ve Samsung ücretsiz; diğerlerinde çoğu ücretli sürümde | ücretsiz | Google Saat (Wear), Samsung Saat, Seconds, Focus Keeper, Be Focused, MultiTimer, Countdown, Lasmit |
| 138 | Geri sayımı bağlantıyla paylaşma; alan kişi kendi uygulamasına ekleyebilir (sitedeki `?t=&n=` biçimi kullanılabilir; bugün yalnız görsel kart) | Uygulama + site | K | bitti (3 Kasım 2026, K-5) | ücretsiz | ücretsiz | Countdown, Days, Timer+, MultiTimer |
| 139 | Geri sayımda etiket ve etiketle süzme, kişilerden doğum günü alma, takvimden otomatik içe aktarma, yalnız iş günü sayma, esnek tekrar, etkinlik başına saat dilimi, geri sayıma alarm (bugün yalnız yıllık tekrar) | Uygulama | K–O | bitti (3 Kasım 2026, K-5) | otomatik içe aktarma ve alarm Premium | ücretsiz | Countdown, Countdown Star |
| 140 | Görsel zamanlayıcılar: rakamsız pasta zamanlayıcı ("Time Timer" tarzı; geçen süreyle resim açılır), çocuk modu (sayarken düğmeler gizli), kum saati; sitede renk değiştiren sunum zamanlayıcısı | Uygulama + site | K/O | bitti (3 Kasım 2026, K-5): pasta ve kum saati, çocuk modu; sitede sunum renkleri | Visual Countdown Timer resim paketi ₺49,99 | ücretsiz | Visual Countdown Timer, online-stopwatch |
| 141 | Sınav salonu zamanlayıcısı: sınav adı, salon no, okuma süresi + sınav süresi, tam ekran (ortak yazılı ve deneme sınavları için) | Site (+ uygulama) | K | yapıldı (3 Kasım 2026; sitede /sinav-zamanlayicisi/) | online-stopwatch: ücretsiz (reklamlı), Premium $24/yıl | ücretsiz | online-stopwatch |
| 142 | Sınıf araçları: isim seçici, takım oluşturucu, sayaç (kapsam sahibin kararı) | Site | K–O | bitti (4 Kasım 2026, gece turu): sitede `/sinif-araclari/` (15 dil): rastgele ad seçici (güvenli rastgele sayı, seçileni listeden çıkarma, listeyi geri getirme), takım kurucu (2–12 takım), adlı sayaçlar; tam ekran; kayıt yalnız tarayıcıda (`sth-class`, gizlilik tablosunda) | ücretsiz | ücretsiz | online-stopwatch |
| 143 | Mac menü çubuğunda pomodoro (Mac uygulamamız yalnız saat ve vakit gösteriyor) | Mac | O | bitti (4 Kasım 2026, gece turu): Mac menü çubuğunda pomodoro: 25/5, menü çubuğunda "🍅 24:59", bitince Mac bildirimi, panelde başlat/sıfırla ve sayaç | Be Focused Pro | ücretsiz | Be Focused |
| 144 | Günaydın kartında telefon takviminden bugünün etkinlikleri (bugün yalnız özel günler: `Morning.swift`, `AlarmActivity.kt`) | Uygulama | K | yapıldı (3 Kasım 2026; günaydın kartında telefon takviminden bugünün etkinlikleri) | ücretsiz | ücretsiz | Alarm Clock Xtreme |
| 145 | Tablet ve katlanırda iki sütunlu alarm ve dünya saati ızgarası (bugün iPad'de sayfa 900 pt ortalanır; Android'de tablet düzeni yok) | Uygulama | K | bitti (3 Kasım 2026, K-6; 600 dp / iPad geniş sınıfta) | ücretsiz | ücretsiz | Samsung Saat |
| 146 | Ay ve gökyüzünde tarih/saat kaydırma (çentikli zaman cetveli); Ay takviminde güne dokununca o günün Ay'ı | Uygulama | K/O | bitti (3 Kasım 2026, K-5) | ücretsiz | ücretsiz | Moon Phases, The Moon, Lumos, Deluxe Moon |
| 147 | Uygulamada Ay verileri: doğuşa/batışa kalan süre, üst geçiş, dördün tarihleri, uzaklık, yerberi/yeröte, süper Ay ve mavi Ay (bugün uzaklık ve süper Ay yalnız sitede, `sky.js`; sıradaki dolunay ve yeni ay tarihi Ay widget'ında var) | Uygulama | K | bitti (3 Kasım 2026, K-5) | ücretsiz | ücretsiz | Moon Phases, Lumos, MOON, Deluxe Moon |
| 148 | Yıl evre çarkı ve evre tablosu ("13 dolunay, 1 mavi Ay") | Uygulama + site | K | yapıldı (3 Kasım 2026; site Mevsimler: dolunay sayısı, Mavi Ay, çark ve tablo; uygulamada Ay takvimi) | ücretsiz | ücretsiz | Deluxe Moon |
| 149 | Ay widget'ları orta ve büyük boy (sıradaki 4 evre, evreli ay takvimi, Ay yükseklik eğrisi) ve saat komplikasyonu (bugün yalnız küçük, `MoonWidget.swift` `.systemSmall`; kilit ekranı #14'e ek) | Uygulama + saatler | O | yapıldı (4 Kasım 2026, gece turu): iOS Ay widget'ı küçük/orta/büyük (sıradaki 4 ana evre, ayın evre takvimi), Android Ay takvimi widget'ı (4 ana evre), Apple Watch Ay komplikasyonu (dairesel, dikdörtgen, satır, köşe), Wear OS Ay komplikasyonu (kısa, uzun, aralık); Ay yükseklik eğrisi gece gökyüzü ekranında | çoğu ücretsiz, Lumos'ta Pro | ücretsiz | Moon Phases, Deluxe Moon, Lumy, MOON |
| 150 | Güneş için Live Activity ("gün batımına 1:18:36"); Güneş widget'ları (yay, 24 saatlik halka, ekinoks geri sayımı; widget kısmı #15'e ek) | Uygulama | K/O | bitti (3–4 Kasım 2026): Güneş sayacı (iOS Live Activity, Android süren bildirim), Güneş widget'ında mevsim başlangıcına kalan gün; 4 Kasım: Güneş yayı ve 24 saat halka widget'ı (Android ayrı widget, iOS orta boy) | Lumy ücretli uygulama, SolarWatch Gold | ücretsiz | Lumy, SolarWatch |
| 151 | Sivil, denizcilik ve astronomik alacakaranlık; ilk ve son ışık; 24 saatlik ışık halkası (bugün yalnız altın ve mavi saat, `sunAltTimes`) | Uygulama + site | K | yapıldı (3 Kasım 2026; sivil, denizcilik, astronomik alacakaranlık, ilk/son ışık; sitede 24 saatlik ışık halkası) | çoğu ücretsiz | ücretsiz | Sun Seeker, SolarWatch, Lumos, Sky Tonight, Lumy |
| 152 | Özelleştirilebilir Güneş ve Ay bildirimleri: olay seçimi (tutulma, dördün, süper Ay, altın saat), kaç dk önce, günler (bugün iki sabit anahtar: `notify.moon`, `notify.sun`) | Uygulama | K | bitti (3 Kasım 2026, K-5): olay seçimi, önceden haber verme, Güneş için günler | **ücretli**: Moon Phases ₺49,99, Lumos ve Lumy'de Pro | ücretsiz | Moon Phases, Lumos, Lumy, Daily Life with Moon Calendar |
| 153 | Güneşe bağlı kayan alarm: doğuş/batış ± dk (bugün vakit alarmı "Güneş" vaktini kurabiliyor, ama o doğuştan ~7 dk önce ve süre ayarı yok) | Uygulama | K | yapıldı (3 Kasım 2026; alarm "Gün doğumu/Gün batımı" ± 15–60 dk, her gün seçili yere göre hesaplanır; iOS + Android) | ücretsiz görünüyor | ücretsiz | SolarWatch |
| 154 | Gökyüzü olay takvimi: kavuşum, karşı konum, meteor yağmurları, tutulmalar; bildirim ve .ics (bugün tutulma, dolunay ve yeni ay var; meteor yok; tarihler IMO'dan doğrulanmalı) | Site + uygulama | O | yapıldı (4 Kasım 2026, gece turu): lib/sky-events.mjs (meteor yağmurları IMO 2026–2027 λ⊙ değerlerinden, 2026/2027 IMO günleriyle npm test'te doğrulandı; kavuşumlar, Ay–gezegen, karşı konum, en büyük uzanım; 2026–2031); sitede liste + /ics/<dil>-sky.ics; uygulamalarda liste ve bildirim kategorisi "Gökyüzü olayları" (o gece 20.00) | ücretsiz | ücretsiz | Sky Guide, Sky Tonight, SkyView, Star Walk 2 |
| 155 | "Bu gece" özeti: gezegen yükseklik grafiği, gözlem uygunluğu (bulut + Ay + karanlık süre), öneri cümleleri (bugün sitede yalnız tablo) | Site + uygulama | O | yapıldı (4 Kasım 2026, gece turu): tam karanlık aralığı, Ay'ın karanlıktaki etkisi, görülebilen gezegenler, gözlem notu (iyi/orta/zayıf), öneri cümleleri, gezegen yükseklik grafiği; bulutluluk sitede dış hizmet izniyle Open-Meteo'dan (uygulamada bulut yok) | ücretsiz (SkyView eklentisi ₺49,99) | ücretsiz | Sky Guide, Sky Tonight, SkyView, Night Sky |
| 156 | Gece gökyüzü çizimi: takımyıldız çizgileri ve adları, Ay, zamanı oynatma, tarih seçimi, kırmızı mod (bugün 39 yıldız, 5 ad, çizgi yok: `sky.js`, `sky-calc.js`) | Site | O | yapıldı (4 Kasım 2026, gece turu): 88 takımyıldız çizgisi ve adları (d3-celestial, BSD-3; 10 dilde ad, diğerlerinde Latince), dokulu Ay, zamanı oynatma, tarih seçimi, kırmızı ışık | ücretsiz | ücretsiz | Sky Guide, Night Sky, SkyView, Star Walk 2, Sky Tonight |
| 157 | Gece gökyüzünü uygulamaya taşıma (bugün yalnız sitede) | Uygulama | O | yapıldı (4 Kasım 2026, gece turu): uygulamalarda Gece gökyüzü (Tümü › Gök ve dünya): çizim, oynatma, kırmızı ışık, bu gece, gezegenler, olaylar; gezegen hesabı ortak çekirdekte (Sky.kt, Sky.swift; test verisi sky.json) | — | ücretsiz | 2. dalga ajan: gökyüzü |
| 158 | Güneş yolu pusulası, doğuş/batış yönü (azimut), gölge uzunluğu (Vakitler'deki yay yönsüz) | Uygulama | O | yapıldı (4 Kasım 2026, gece turu): uygulamalarda Güneş yolu: bugün, 21 Haziran ve 21 Aralık yolları, pusulayla dönen çizim, doğuş/batış azimutu, öğle yüksekliği, 1 m çubuğun gölgesi | ücretli (Sun Seeker ₺399,99, PhotoPills ₺599,99) | ileride Pro adayı | Sun Seeker, PhotoPills, Lumos |
| 159 | Tutulma ayrıntısı: Ay tutulması çizimi (temas anları), uygulamada tutulma haritası (bugün yalnız sitede), konuma göre Güneş tutulması örtme yüzdesi; haritanın 2°'lik basamaklı kenarları | Uygulama + site | O | yapıldı (4 Kasım 2026, gece turu): Ay tutulması temas anları (Danjon; NASA 2029 gölge büyüklüğüyle ±0,02) ve çizimi, Güneş tutulmasında yerel örtülme ve saatler (site + uygulamalar; ortak çekirdek solarLocal/lunarContacts, test verisi), uygulamada tutulma haritası, sitede haritanın basamaklı kenarları yumuşatıldı | ücretsiz | ücretsiz | Star Walk 2, SolarWatch |
| 160 | Çocuklar için "Ay neden şekil değiştirir?", etkileşimli yörünge, bilgi yarışması (bugün `kids.*` metin kartları ve durağan çizimler) | Uygulama | O | yapıldı (4 Kasım 2026, gece turu): uygulamalarda "Ay neden şekil değiştirir?": uzaydan yörünge ve Dünya'dan görünüş kaydırıcıyla, 5 soruluk bilgi yarışması (15 dil) | Star Walk Kids: ücretsiz + reklam | ücretsiz | Star Walk Kids |
| 161 | Dolunay adları (Türkçe karşılıkları doğrulanmalı) | Uygulama + site | K | yapıldı (3 Kasım 2026; Kuzey Amerika adları, çeviri olduğu notuyla) | ücretsiz | ücretsiz | Star Walk 2 |
| 162 | Komplikasyonda ve küçük vakit widget'ında sıradaki vakte kalan süre halkası (iOS Gauge, Wear `RANGED_VALUE`); saatte kenar boyunca ilerleme yayı ve kavisli yazı (bugün yalnız metin: `WatchComplications.swift` 78–81; Wear `SHORT_TEXT, LONG_TEXT`). Kilit ekranı dairesel widget'ı (#14) ile birlikte yapılır | iOS, watchOS, Wear OS | K | yapıldı (3 Kasım 2026; iPhone kilit ekranı dairesel halka, küçük widget'ta kendiliğinden dolan çubuk; Apple Watch dairesel halka ve köşede yay; Wear RANGED_VALUE; Android küçük widget'ta çubuk) | ücretsiz | ücretsiz | Pray Watch, Google Saat (Wear), Ezan Vakti Pro (Play), Facer |
| 163 | Vakit widget'ında gün ilerleme çubuğu: beş vakit yan yana, çizgi ve işaret sıradaki vakte doğru dolar (bugün `widget_today.xml` yalnız sıradaki vakti vurgular) | Uygulama (widget) | K | yapıldı (3 Kasım 2026; iPhone'da beş parçalı, içinde bulunulan parça kendiliğinden dolar; Android'de yarım saatte bir yenilenen çubuk) | ücretsiz (reklamlı uygulama) | ücretsiz | Ezan360 (Play) |
| 164 | Vakit bildiriminde "Ertele" ve uzun basınca zengin içerik (kıble pusulası + geri sayım; iOS Notification Content Extension) | Uygulama (iOS) | O | bitti (4 Kasım 2026, gece turu): vakit bildiriminde "5 dk sonra hatırlat" (iOS + Android); iOS'ta uzun basınca içerik uzantısı (vakte kalan süre, kıble açısı ve oku; veriler bildirimin içinden) | ücretsiz | ücretsiz | Pray Watch |
| 165 | Yıl ilerlemesi widget'ı ("203/365", 365 nokta) | Uygulama (widget) | K | yapıldı (3 Kasım 2026; "307/365", yüzde, kalan gün ve noktalar; iPhone kilit ekranında da) | ücretsiz görünüyor | ücretsiz | Color Widgets |
| 166 | Uygulama içi widget galerisi: boyuta göre önizleme; Android'de doğrudan "Ana ekrana ekle" (`requestPinAppWidget`) | Uygulama | K | yapıldı (3 Kasım 2026; Saat görünümü › Widget'lar: Android'de canlı önizleme ve "Ana ekrana ekle", iPhone'da liste ve ekleme yolu) | ücretsiz | ücretsiz | Clock Widget with Seconds Live, Clockology, Color Widgets |
| 167 | Android'de Material You dinamik renk (uygulama + widget); bugün `Theme.kt`'de dinamik renk yok, widget renkleri sabit | Uygulama (Android) | K | yapıldı (3 Kasım 2026; Uygulama rengi'nde "Duvar kâğıdı renkleri", widget görünümünde de; Android 12+) | ücretsiz | ücretsiz | Digital Clock Widget, Google Saat |
| 168 | Saat widget'ında sıradaki alarm satırı; widget'a dokununca açılacak ekranı seçme | Uygulama (widget) | K | yapıldı (3 Kasım 2026; Android saat widget'larında sıradaki alarm satırı; dokununca açılacak ekran iki platformda widget ayarında; iPhone'da sıradaki alarm App Group ister) | ücretsiz (reklamlı) | ücretsiz | Digital Clock Widget |
| 169 | Yatay tam ekranda saatin yanında ikinci alan ("Duo": takvim, fotoğraf, hava, vakit); bugün yatayda saat ekranı doldurur | Uygulama | O | bitti (4 Kasım 2026, gece turu): yatay tam ekranda saatin sağında takvim, vakitler ya da hava (`fs.duo`, Saat görünümü'nde seçilir; iOS + Android) | karışık | ücretsiz | StandBy Mode (Android) |
| 170 | Android 16 Live Updates (durum çubuğu çipi: zamanlayıcı, pomodoro, sıradaki vakit; bugün `ProgressStyle` ve `setRequestPromotedOngoing` yok) | Uygulama (Android) | K | yapıldı (3 Kasım 2026; süren sayaç ve sıradaki vakit bildirimleri öne çıkan canlı güncelleme olarak istenir, vakitte çipte vaktin adı) | — (sistem özelliği) | ücretsiz | 2. dalga ajan: android-widget-saat (ham: `android-widget-saat.md`) |
| 171 | iOS 18 koyu ve renklendirilmiş uygulama simgesi; renklendirilmiş widget'larda `widgetAccentable` (bugün uygulama simgesi tek görsel) | Uygulama (iOS) | K | yapıldı (3 Kasım 2026; koyu ve renklendirilmiş simge, widget'larda widgetAccentable) | — (sistem özelliği) | ücretsiz | 2. dalga ajan: android-widget-saat |
| 172 | Yazıyla tarih ve saat gösteren tipografik widget ("Monday / twenty two / JUNE"; düşük öncelik) | Uygulama (widget) | K | yapıldı (3 Kasım 2026; gün adı, gün yazıyla, AY; iPhone'da saat de yazıyla, Android'de dakikada yenilenemediği için saatsiz) | ücretsiz şablon | ücretsiz | KWGT |
| 173 | Apple Watch büyük dikdörtgen komplikasyonda bugünün vakitleri listesi (bugün 3 satır: ad, sıradaki vakit, sayaç) | watchOS | K | yapıldı (3 Kasım 2026; "Bugünün vakitleri" dikdörtgen komplikasyonu: altı vakit, sıradaki kalın) | Clockology: abonelik | ücretsiz | Clockology |
| 174 | Saat doğruluğu satırı ilk ekranda ("Saatiniz tam! fark +0,08 sn ±0,12"): ana sayfa ve şehir sayfalarında ölçüm yalnız "i" penceresinde (`build.mjs:306`); araç sayfalarında satır görünür | Site | K | yapıldı (3 Kasım 2026; sahnenin altında kısa satır, ayrıntı "i"de) | ücretsiz | ücretsiz | time.is |
| 175 | Ana sayfa ve şehir sayfalarında sekme başlığında canlı saat (araçlarda sayaç zaten başlıkta: `tools.js:27` `setTitle`) | Site | K | yapıldı (3 Kasım 2026; ana sayfa ve şehir sayfaları) | ücretsiz | ücretsiz | time.is |
| 176 | Saat görünüm seçenekleri: saate tıklayınca milisaniye, haftanın ilk günü, tarih biçimi, saat ayırıcısı (bugün `sth-prefs` 24/12, saniye, yazı tipi, renk) | Site | K | yapıldı (3 Kasım 2026; ayarlar › saat görünümü: tarih biçimi, ayırıcı, hafta başı, milisaniye) | ücretsiz | ücretsiz | time.is /customize |
| 177 | Sitede saati büyütme ve küçültme (+/−); uygulamada `fs.scale` var | Site | K | yapıldı (3 Kasım 2026; −/+ düğmeleri ve tuşları, %50–150) | ücretsiz | ücretsiz | vClock, onlinesaat.web.tr |
| 178 | Ülke başına ay ızgaralı takvim ve geçmiş yıllar (2024–2032); bugün ülke tatilleri tablo, ızgara yalnız Türkiye, yıllar 2026–2031 | Site | O | bitti (3 Kasım 2026, gece turu): ülke sayfalarında derleme yılının 12 aylık ızgarası (tatiller işaretli); yıllar 2024–2032 (derleme yılı ve ertesi açık, diğerleri katlanır); eksik yıllar (Singapur 2028+) veriden çıkarılır (`incomplete`) | ücretsiz | ücretsiz | time.is |
| 179 | Saat farkı: çubuk grafik; merkez sayfadaki kartlarda şimdiki saat ve fark (bugün ad ve ülke) | Site | K | yapıldı (3 Kasım 2026; kartlarda saat, kısa fark ve fark çubuğu) | ücretsiz | ücretsiz | time.is, savvytime |
| 180 | Şehir sayfasında Güneş öğlesi ve "yerel saat güneş saatinden X dk ileride"; nüfus ve harita | Site | K | yapıldı (3–4 Kasım 2026): Güneş öğlesi, güneş saatiyle fark, OpenStreetMap bağlantısı ve kent bölgesi nüfusu (Natural Earth pop_max, kamu malı; scripts/fetch-city-pop.mjs, cities.json "pop") | ücretsiz | ücretsiz | time.is |
| 181 | Sitede alarm: kurulunca büyük geri sayım halkası + yüzde çubuk (bugün tek satır, `tools.js:451`; #51'e ek), erteleme süresi seçimi (bugün sabit 5 dk, `tools.js:499`) ve ses düzeyi (#40'a ek), sesi tekrarlama ve "şimdi dene", alarm geçmişi + CSV, "son kullanılan saatler" | Site | K | yapıldı (3 Kasım 2026; halka + çubuk, erteleme süresi, ses düzeyi, şimdi dene, geçmiş + CSV, son saatler) | ücretsiz | ücretsiz | onlinealarmkur.com, vClock |
| 182 | Sitede gece ve karartma modu ("ışıkları kapat"; uygulamalarda var) | Site | K | yapıldı (3 Kasım 2026; ayarlar › saat görünümü › gece modu) | ücretsiz | ücretsiz | onlinealarmkur.com, vClock |
| 183 | Temalı tam ekran saatler (Hologram, Matrix, matematik ifadeli); #9 ve #23'e ek | Site + uygulama | K | yapıldı (3 Kasım 2026; sitede hologram, Matrix, matematik saati; 23 model) | ücretsiz | sitede ücretsiz; uygulamada ileride Pro adayı | vClock, saat-kac.tr |
| 184 | Ana sayfada bilgi hapları (UTC+3, "Yaz saati yok", Europe/Istanbul); bugün bu bilgiler `/turkiye-saat-dilimi/` sayfasında. (Rakipteki tek dokunuşla Dijital/Analog anahtarı bizde 20 modelle karşılanıyor; farklı, eksik değil) | Site | K | yapıldı (3 Kasım 2026) | ücretsiz | ücretsiz | saat-kac.tr |
| 185 | Toplantı planlayıcıdan dışa aktarma: Outlook web, Gmail, panoya metin (bugün bağlantı, .ics ve Google var) | Site | K | yapıldı (3 Kasım 2026; çevirici ve planlayıcıda) | ücretsiz | ücretsiz | worldtimebuddy |
| 186 | **Hata:** Sitedeki kıble pusulası iPhone'da manyetik kuzeye göre dönüyor: iğne `bearing - h` ile çiziliyor (`qibla.js:33`); `bearing` gerçek kuzeye göre, `h` ise iPhone'da `webkitCompassHeading` (manyetik kuzey, `qibla.js:83`); sapma düzeltmesi yok. Türkiye'de iğne ~5–6° kayık ve ±3°'lik "hizalandı" işareti yanlış yerde yanıyor. Uygulamalarda düzeltme var (Android `GeomagneticField`). Düzeltme: manyetik sapma modeli (WMM katsayıları, NOAA/BGS; lisansı doğrulanmalı) ya da sapma tablosu; Android'de `deviceorientationabsolute`'un kuzeyi de doğrulanmalı. Kâbe'ye çizgi #67'ye ek | Site | K | yapıldı (3 Kasım 2026) | — | ücretsiz | 2. dalga ajan: web (namazvakti.com manyetik sapmalı açı veriyor); doğrulama ajanı |
| 187 | Metin satırı olarak gömülen widget (script + span; gün doğumu/batımı seçeneği); bugün yalnız iframe | Site | O | yapıldı (4 Kasım 2026, gece turu): /embed/line.js + <span data-saat-tarih="time|date|sun">: başka sitelerde tek satırlık canlı saat, tarih ve gün doğumu/batımı (çerezsiz, veri göndermez); /gomme/ sayfasında üreteç, önizleme ve kopyala. Ayrıca gömme önizlemesini engelleyen CSP hatası düzeltildi (frame-src 'self') | ücretsiz (geri bağlantı şartıyla) | ücretsiz | time.is |
| 188 | Saat sahnesinde paylaş düğmesi | Site | K | yapıldı (4 Kasım 2026, gece turu): saat sahnesinin "i" penceresinde Paylaş (telefonda sistem paylaşımı, masaüstünde bağlantıyı kopyalar); denetim satırına yeni düğme eklenmedi (yeni düğme kuralı) | ücretsiz | ücretsiz | canlisaat.com, vClock |
| 189 | Gece modunda yazı rengi seçimi (7 renk) ve Android'de ışık sensörüyle otomatik gece modu; bugün kırmızı sabit #B82016 (`NightMode.kt`, `NightMode.swift`), otomatik mod yalnız Güneş'e göre (#11 lüm kadranından ayrı iş) | Uygulama | K | bitti (3 Kasım 2026, K-6; ışık sensörü yalnız Android'de, iOS uygulamalara açmıyor) | ücretsiz görünüyor | ücretsiz | StandBy Mode (Android); 2. dalga ajan: android-widget-saat; doğrulamadan sonra ayrı iş yapıldı |
| 190 | Üst bardaki tarihte hafta numarası ("3 Kasım 2026 Salı · 45. hafta"); bugün hafta ayrı sayfada (`/hafta-numarasi/`) | Site (+ uygulama) | K | yapıldı (3 Kasım 2026; dar ekranda gizli) | ücretsiz | ücretsiz | time.is; doğrulamada atlanan bulgu olarak eklendi |
| 191 | Uyku hesaplama sayfası (yatma ya da kalkış saatine göre 90 dk döngüleri); bugün öneriler yalnız hazır alarm sayfalarında (`ALARM_PRESETS`) ve uygulamanın Uyku ekranında | Site | K | yapıldı (3 Kasım 2026; /uyku-hesaplama/) | ücretsiz | ücretsiz | saat-kac.tr; doğrulamada atlanan bulgu olarak eklendi |
| 192 | Çerez bandı telefonda ilk ekranın ~210 px'ini (masaüstünde ~115 px) kaplıyor, metin 4 satır; standart madde 14'ü (eşit düğmeler, politika bağlantısı) bozmadan daha kısa metin ve ince bant | Site | K | yapıldı (3 Kasım 2026; telefonda ~111 px, masaüstünde ~51 px) | — | ücretsiz | 2. dalga ajan: web (`web-siteler.md:238`; rakiplerde ABD IP'siyle bant görülmedi); doğrulamada atlanan bulgu olarak eklendi |
| 193 | **Hava durumu neyse ekran da öyle (sahibin isteği):** yağmurluysa ekranda yağmur yağar, rüzgârlıysa rüzgâr eser (bulutlar hızlı akar, yağmur eğik düşer, yapraklar savrulur), karlıysa kar, sisliyse sis, fırtınada şimşek, güneşliyse ışık parlaması, gece yıldızlar. Yoğunluk ölçüme göre değişir: yağış mm, rüzgâr km/sa, bulut oranı. Yerleri: saat ekranı, hava sekmesi, tam ekran saat, sitedeki hava sayfası. Widget'lar canlandırılamadığı için koşula göre durağan görsel kullanılır. Bugün yalnız tam ekran saatte (`fsBg = weather`) 6 durum var: açık, bulutlu, yağmur, kar, sis, fırtına. Rüzgâr durumu yok; rüzgârlı hava bulutluya düşüyor (`AnimatedBackground.swift:30` `.breezy`/`.windy` → `.cloudy`). Yoğunluk sabit. Hareketi azalt tercihine uyar; ekran görünmezken durur (pil). #8'i ve ilk analiz #79'u kapsar | Uygulama + site + widget | O | yapıldı (3 Kasım 2026; rüzgârlı durum, ölçüme göre yoğunluk; tam ekran, hava sekmesi, saat ekranı [isteğe bağlı], sitedeki hava sayfası; widget'lar için durağan görsel yapılmadı) | Weather Live, The Weather Channel, AccuWeather, Yahoo: ücretsiz | ücretsiz | sahibin isteği; rakip örnekleri 2. dalga ajan: hava-takvim; Alarmy (yağmur damlalı cam) |
| 194 | Kişisel alt çubuk: en çok 5 sekme (Saat her zaman ilk, "Tümü" her zaman son, aradaki en çok 3 yeri kullanıcı seçer); Özellikler ekranında "Alt çubukta" işareti ve sürükleyerek sıra; iPad ve Mac'te kenar çubuğu. 30 Ekim özellik aç/kapa kararının üstüne kurulur; #95 ile birlikte | Uygulama (iOS, Android) | O | bitti (3 Kasım 2026, gece turu): alt çubukta Saat + en çok 3 özellik + Tümü; Özellikler'de "Alt çubukta" işareti ve yukarı/aşağı sıra, canlı önizleme (ilk kurulumda da); iPad'de kenar çubuğu (açık özellikler ve Tümü'nün kategorileri); kayıt `features.bar`, göç: Vakitler, Hava, Takvim | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 195 | "Tümü" sayfası: Araçlar listesinin yerine kategorili ızgara (Süre ve alarm, Gün ve plan, Gök ve dünya, İbadet); Ayarlar sağ üstte dişli, Hakkında Ayarlar'ın içinde, Rozetler sayfa sonunda; kareler #99'daki simge kutularıyla | Uygulama | O | bitti (3 Kasım 2026, gece turu): Araçlar sekmesi "Tümü" oldu: kategorili ızgara (Süre ve alarm, Gün ve plan, Gök ve dünya, İbadet, Diğer: Rozetler), sağ üstte dişli, Hakkında Ayarlar'da; ibadet, Ay ve takvim yaprağı sayfaları da buradan açılır | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 196 | Uygulama içi genel arama: araç, ayar ve içerik tek kutudan; sonuç doğrudan ekranı ya da ayar satırını açar; sitedeki 15 dilli arama sözcükleri yeniden kullanılır (bugün arama yalnız konum, Kur'an ve Esma içinde) | Uygulama | O | bitti (3 Kasım 2026, gece turu): Tümü'nde tek arama kutusu: araçlar, özellikler, ayar sayfaları ve Kur'an sure adları (sure doğrudan açılır); aksan duyarsız | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 197 | Sık kullanılanlar: Tümü'nün üstünde 4 kare, uzun basınca ekle; boşsa son kullanılanlar | Uygulama | K | bitti (3 Kasım 2026, gece turu): 4 karelik sık kullanılanlar (uzun basınca ekle/çıkar; iOS'ta bağlam menüsü), boşsa son kullanılanlar (`all.favs`, `all.recent`) | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 198 | Ayarları 5 başlıklı gruba ayırma (Genel, Renk ve görünüm, Saat, Yedek ve kısayollar, Destek); satırda şimdiki değer; uzun açıklamalar alt sayfada; en üstte arama. Destek grubu #6 ve #81 ile birlikte | Uygulama | K | bitti (3 Kasım 2026, K-6) | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 199 | Vakitler sadeleştirme: alttaki 12 bağlantı kartı yerine 4 karelik İbadet satırı ve İbadet sayfası; paylaş, aylık çizelge, hesap yöntemi, imsakiye "…" menüsünde; kilit ekranı anahtarı Bildirimler'de; vakit simgeleri #65 | Uygulama | K | bitti (3 Kasım 2026, K-6) | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 200 | Araç ekranları için ortak kalıp: tek ana düğme altta tam genişlikte, ikincil işler "…" menüsünde, ayar ve listeler alt sayfada (önce Pomodoro, Zamanlayıcı, Kronometre, Aralık); ilerleme halkası #101 | Uygulama | O | bitti (3 Kasım 2026, gece turu): ortak kalıp (Android `ToolFrame`, iOS `ToolPrimaryBar` + `MoreMenu`): Pomodoro (Sayaç/İstatistik, görev ve ses ile süreler alt sayfada), Zamanlayıcı (yeni sayaç alt sayfada, altta "Yeni zamanlayıcı"), Kronometre (altta tur/sıfırla + başlat/durdur, tam ekran ve kopyalama "…"da); Aralık zaten tek ana düğmeli | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 201 | Alarm düzenleyicisinde "Gelişmiş" bölümü kapalı gelsin (görev, kademeli ses, erteleme sınırı, müzik, gün doğumu ışığı, akıllı alarm, saati söyleme); saat, günler, ses ve erteleme üstte kalır | Uygulama | K | bitti (3 Kasım 2026, K-6) | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 202 | Saat ekranında model adı ("Klasik analog · 1/20"); Tam ekran, Düzenle ve Ayarlar düğmeleri alt çubuğun üstüne (başparmak bölgesi); #94 ile birlikte | Uygulama | K | bitti (3 Kasım 2026, K-6) | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 203 | Geçici sekme yerine geri düğmeli sayfa: kapalı bir özellik bildirimden ya da widget'tan açılınca alt çubuk değişmez (bugün `barTabs` geçici sekme ekliyor) | Uygulama | K | bitti (3 Kasım 2026, K-6) | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 204 | Tam ekran saatte düğmeler dokununca belirsin, birkaç saniye sonra kaybolsun (sitede böyle; Android'de `FullscreenClock` düğmeleri hep gösteriyor) | Uygulama | K | bitti (3 Kasım 2026, K-6) | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 205 | Tek hitap "siz": bugün Özellikler'de "Açtığın", Rozetler'de "sen silene kadar" yazıyor; 15 dilde sonraki dil denetimiyle | Uygulama | K | yapıldı (4 Kasım 2026, gece turu): tr/az'a ek olarak de (Sie), fr (vous), ru (вы), id (Anda), fil (po/kayo) metinlerindeki sen hitapları siz'e çevrildi (çocuklar için Ay, uyanma oyunu, bilgi kutuları); taramada başka sen hitabı yok | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |
| 206 | "Yeni düğme" kuralı: her yeni işte "bu ekranın ana işi mi?" diye sorulur; değilse "…" menüsü ya da alt sayfa; yeni araç Tümü'nde bir kategoriye ve aramaya girer, alt çubuğa girmez (onaylanırsa CLAUDE.md'ye bir satır) | Süreç | K | bitti (3 Kasım 2026): kural CLAUDE.md'ye yazıldı ("Yeni düğme kuralı") | — | ücretsiz | kullanıcı dostu düzen (Seçenek B; `docs/kullanici-dostu/`) |

## Sahibin ekran görüntüleri (3 Kasım 2026)

### Analog Clock Pro (6 görüntü)

**Pro sayfası:**
- Koyu yeşil-siyah bulanık zemin.
- Sol üstte "Onarmak" yazan hap biçimli düğme. Bu, "Restore" sözcüğünün yanlış çevirisi.
- Sağ üstte yuvarlak, cam görünümlü kapatma düğmesi.
- Büyük beyaz başlık "Pro'ya yükselt", altında "Tam erişim".
- 6 kadran küçük resmi, ortadaki büyük.
- Beyaz çerçeveli satın alma kutusu:
  - "Analog Clock Pro." başlığı, sağ üst köşede kırmızı "50% Off" etiketi.
  - "Ömür Boyu Erişim Alın Analog Clock Pro." metni.
  - Fiyat: "₺399,99" ve yanında üstü çizili "₺79.998,00".
  - "Tek seferlik SATIN ALMA" ibaresi.
- Beyaz zeminli, siyah yazılı, tam genişlikte "Devam et" düğmesi.
- Altta "Privacy Policy · Terms & Conditions" bağlantıları (çevrilmemiş).

**Kadran galerisi:**
- Siyah zemin, büyük yuvarlatılmış kartlar.
- Sol üstte "PRO" hapı (taç simgeli), sağ üstte cam görünümlü kapatma düğmesi.
- Her kartta:
  - kalın başlık (ör. "Spor Kronograf"),
  - gri alt yazı "Bir renk teması seçin",
  - sağda hap biçimli "▶ Önizleme" düğmesi,
  - kadranın canlı örneği, üstünde "👑 Premium" etiketi,
  - 5 renk dairesi (kırmızı, mavi, yeşil, turuncu, pembe), her birinin köşesinde sarı taç; seçili renk beyaz halkalı.
- Görülen kadranlar:
  - Spor Kronograf: takometre ölçeği, gün/tarih penceresi.
  - Velar Chronograph: Roma rakamı, iki alt kadran.
  - Adı "PRO" hapının arkasında kalan kart ("Kadran" adı kesin değil): beyaz halka, kadran çevresinde radyal döndürülmüş büyük rakamlar (eğik değil), iç siyah kadran, kırmızı tarih.
  - Fütüristik Çift Halka.
  - Dönen Saniye ve Dakikalar: dönen ölçek, üstünde "Dokun" ipucu.

**Saat Kadranları sayfası:**
- "Klasik Kadranlar" başlığı, altında "Favori tasarımınızı göz atın ve seçin".
- Yatay kaydırmalı iki sıra.
- Fotoğraf dokulu kadranlar: deniz, yaprak, şeker kamışı, mermer, yeşil su.
- Bazıları "Premium".
- Üstte "Ses Açık" yazan siyah hap. Düzeltme (tasarım incelemesi): bu uygulamanın değil, iPhone'un Dynamic Island'daki zil göstergesi (tam Dynamic Island'ın yerinde, saf #000000). Saniye tık sesi bu görüntüden çıkmaz; sahibin telefondaki gözlemiyle (Fullscreen Clock) doğrulandı.

**Saat ekranı:**
- Deniz dokulu büyük analog kadran, beyaz rakamlar, açık mavi saniye ibresi.
- Üstte hap biçimli araç çubuğu: kadran, fotoğraf, cihaz yönü, ses dalgası (kırmızı noktalı), ses, alarm.
- Sağ üstte "üst üste kartlar" (galeri) düğmesi.
- Altta "Uygulamamızı seviyor musunuz?" puanlama kartı:
  - kalp simgesi,
  - "Sonra" ve kırmızı "Derecelendirin" düğmeleri,
  - düğme yazısı taşıyor ("Derecele / ndirin"): çeviri ve yerleşim hatası.

**"Yenilikler" sayfaları:**
- Koyu lacivert-siyah zemin.
- Büyük beyaz başlık "Yenilikler", altında gri "Yeni premium saat stillerimizi keşfedin.".
- Çerçeveli kart içinde kadran görseli; altında kadran adı.
- Kadranlar (saat damgası ve sayfa noktasına göre sıra):
  1. İmparatorluk Yeşimi: altın çerçeve, Çin rakamları, "福" filigranı.
  2. Minimal Radar: altın tonlu radar taraması.
  3. Liquid Mercury: metal çerçeve, mavi ibreler, cıva damlaları.
- 10 sayfa noktası, seçili olan bej uzun çizgi.
- Beyaz tam genişlikte "Devam Et" düğmesi.

### Alarm uygulaması, sürüm 2.2, adı görünmüyor (2 görüntü)

**Dil seçimi:**
- Beyaz zemin, "Choose Language" başlığı, "Done" düğmesi.
- Her satırda yuvarlak bayrak, dilin İngilizce adı ve kendi adı.
- Satırlar ekranda yazıldığı gibi: English/English, Chinese/中国, Korean/한국인, India/इंडिया, Spanish/Española, France/français, Russia/Русский, Portuguese/Português. Düzeltme (tasarım incelemesi): kimi satırda dil yerine ülke adı var (India, France, Russia); yerel adlar da yanlış (中国 = Çin, 한국인 = Koreli, इंडिया = Hindistan, "Española" dişil sıfat). Bizim dil seçicimizde (#5) dilin kendi adı yazılmalı.
- Seçili satır gri hap biçiminde.
- Altta reklam şeridi.

**Ayarlar:**
- Açık gri zemin üstünde beyaz gruplu kartlar.
- Satırlar:
  - Languages (açılır seçim)
  - Fade Effect (anahtar)
  - Other: Feedback, Rate App, Share App
  - About: Terms & Conditions, Privacy Policy, About App 2.2
- Reklam şeridi, 3 sekmeli alt çubuk (Alarm, Clock, Settings).

### Alarmy (9 görüntü)

**Tanıtım sayfaları (3 sayfa):**
- Siyah zemin, yıldız noktacıkları, kupa emojisi.
- Başlık "Dünyanın en güvenilir alarmı".
- Defne dalı çerçeveli üç iddia: "#1 Alarm App in 97 countries", "4.8★ Rating", "100M+ Downloads".
- İkinci sayfa "Artık erteleme yok / Günün kontrolü sende": "Other Apps" ve "Alarmy" karşılaştırmalı iki telefon çizimi.
- Üçüncü sayfa "Tıbbi yayınlarda adı geçen tek alarm": üniversite amblemleri, beyin çizimi, "Morning Productivity 2x" ve "Goal Achievement +15%" iddiaları.
- Kırmızı (ölçülen #fc314c), tam genişlikte, yuvarlak köşeli düğme. Düğme metinleri ters yerde: ilk sayfada "Başla", son sayfada "Sonraki". Tanıtımdaki iddialar İngilizce kalmış.

**Kurulum sihirbazı (4 adım, beyaz ilerleme çubuğu; görülen adımlar duvar kâğıdı 2/4, ses 3/4, görev 4/4; 1/4 görülmedi):**
- Alarm duvar kâğıdı: başlık "Alarm duvar kağıdını seç".
  - Kategoriler: 🪩 KPOP Trend, 💖 Popüler, 🔥 Günlük motivasyon, 🪐 Uzaya doğru, 🤍 Basit duvar kâğıtları, 💥 Gürültülü uyanış.
  - Dikey kartlar; seçili kart beyaz çerçeveli ve ortasında onay işareti var.
  - Kartın altında ♪ işaretli ad (ör. "GO HARD S…"): zemin ve ses birlikte.
  - Örnekler: Earth, Night of Earth, On Earth, Black, Early morning, Daylight.
  - Tam ekran önizlemede "3 Kas Sal" ve büyük "07:00", altta "Seç" düğmesi (#e84652; aynı akışta ikinci bir kırmızı).
- Alarm sesi: hap biçimli sekmeler (KPOP Trend, Popüler, Varsayılan).
  - Gruplu radyo listesi; çalan satırda turkuaz daire ve ses dalgası simgesi.
  - Örnek sesler: GET YOUR BUTT UP!, GO HARD Alarm, Cat Alarm, Kâbus Alarmı, Alarm Halüsinasyonu, Wake up you lazy kid, Gooood Morninggg, Orkney, Çalar Saat, Dijital Alarm, Güneş ışını.
- Uyanma görevi: koyu kartlar, renkli yuvarlak kare simgeler.
  - Seçenekler: Matematik problemleri, Yazma, Renkli Karoları Bul, Sallama, Kapalı.

**Sabah ana ekranı:**
- Yağmur damlalı cam zemin (havaya göre).
- "Ankara >" konum seçici, büyük "11°", "Yağmur", en yüksek/en düşük.
- 9 saatlik şerit.
- Dört kare kart:
  - Evcil hayvan ("Beni uyandır" balonu, yumurtadan çıkan yaratık)
  - Sabah hissi (gülen yüz)
  - Namaz vakti (mor hilal)
  - Puanlarım (0)
- Video reklam.
- 5 sekmeli koyu lacivert alt çubuk: Alarm, Uyku, Sabah, Rapor, Ayar. Seçili sekme koyu hap biçiminde, kırmızı bildirim noktaları var.
- Altta banka reklamı.

### Sahibin ikinci gönderimi (3 Kasım 2026, 13 görüntü)

**Bir alarm uygulamasının Premium sayfası (adı görünmüyor):**
- Beyaz zemin, sağ üstte ince gri ×.
- Kalın siyah başlık "Upgrade to Premium", altında "₺399,99 billed weekly, cancel anytime". Haftalık ₺399,99; şimdiye kadar gördüğümüz en pahalı plan.
- Defne dallı "1K+ Rattings" (yazım hatası) ve beş yıldızlı "4.6 STAR RATING".
- Açık gri kartlarda kayan kullanıcı yorumları. İlk kart İspanyolca: "Tiene unos buenos sonido de alarma…".
- "Auto renew. Secure with App Store" satırı, kalkan simgesiyle.
- Mavi tam genişlikte "Continue" düğmesi, altı çizili "View All Plans", en altta "Terms | Restore | Privacy".

**Desk Clock widget galerisi:**
- iOS widget ekleme penceresi: "Desk Clock Widget" başlığı.
- Gri açıklama: "After adding to homescreen, long press to edit. (Transparent Widget and Blur works only for iPhone after adding Home screen wallpaper in App)".
- Deniz dokulu analog kadran: ince beyaz rakamlar, açık mavi saniye ibresi.
- 4 sayfa noktası, mavi "Araç Takımını Ekle" düğmesi (iOS'un kendi çevirisi). → yeni iş 76.

**Bir alarm/saat uygulaması (reklamlı, sürüm 52, 6 sekmeli alt çubuk):**
- Ayarlar (ekranda bölüm başlıkları "SENIN MODUN", "UYGULAMA BILGISI", "DIL" diye noktasız I ile yazılmış: Türkçe büyük harf hatası; aşağıda doğru yazımla anılıyor):
  - "SENİN MODUN": yana kaydırmalı büyük tema kartları ("Lavanta Rüyası", "Mars Tozu", "Siber Kireç").
  - Bütün ekranın zemini seçilen temanın gradyanına boyanıyor: mor, bordo-kırmızı, zeytin-sarı, lacivert.
  - Seçili kart ince beyaz çerçeveli, tema renginde ışıyor, köşesinde beyaz daire içinde onay işareti var.
- "SAAT GÖRÜNÜMÜ": Dijital / Analog / Minimal. Seçili satır tema renginde çerçeveli, sağında dolu onay.
- "DİL" satırı.
- "YAZI BOYUTU": A− ile A+ arasında 13 adımlı kaydırıcı (%85, %100, %145) ve "Örnek metin önizlemesi". → yeni iş 77.
- "DESTEK VE TOPLULUK": Uygulamayı Puanla, Arkadaşlarınla Paylaş, Geri Bildirim Gönder, Gizlilik Politikası, Diğer Uygulamalar. Her satırda ince çizgili koyu kiremit simge (ölçülen #70463d; mavi temada da aynı kalıyor, temaya uymuyor) ve iki satırlık açıklama. → #6 ve yeni iş 81.
- "UYGULAMA BİLGİSİ": Versiyon 52.
- Bölüm başlıkları geniş harf aralıklı büyük harf, gri.
- Alt çubuk: yalnız simgeler (alarm, dünya, kum saati, kronometre, ay, ayarlar); yalnız seçili sekmede yazı var, tema renginde.
- En altta banner reklam ("Note Together").

**Fullscreen Clock (4 görüntü):**
- Uygulama izleme (ATT) izni penceresi: "…diğer şirketlerin uygulamalarındaki ve web sitelerindeki eylemlerinizi takip etsin mi?". Gerekçe metni İngilizce ve çevrilmemiş. Bizde reklam ve izleme yok; bu pencere bizde hiç çıkmamalı.
- Yatay tam ekran kadran galerisi:
  - Alt şeritte kare küçük resimler: beyaz klasik kadran (PM etiketli), iki alt kadranlı siyah kronograf, "Your Photo" (kendi fotoğrafın kadran zemini), reklam kaldırma kutusu, paylaş kutusu.
  - Sol üstte parlaklık düğmesi.
  - "1 day vip (view ad)": reklam izleyerek 1 günlük VIP.
  - "No Ads" düğmesi.
  - Kronograf: turuncu çerçeveli akrep, kırmızı saniye ibresi, mavi alt kadran ibresi.
- Paylaşma ekranı: siyah zemin, "Download Fullscreen Clock" QR kodu, gri-mavi hap biçimli "Share" ve "Review" düğmeleri, altta geliştiricinin öbür uygulamalarının simgeleri. → yeni iş 81.
- Dev metin modu: beyaz büyük harflerle "KEK VAR", yanında mavi yuvarlak kalem düğmesi; şeritte "MVP" metin karosu ve pomodoro/zamanlayıcı küçük resimleri. → mevcut #22.

**Alarmy (2 görüntü):**
- Ana ekran:
  - Üstte "PRO Free Trial" hapı (kırmızı-turuncu gradyan çerçeve, şimşek simgesi).
  - "TWS: Wake Up to TWS' Calls / Set TWS Alarms NOW" kartı: ünlü bir K-pop grubunun sesli alarm paketi. Lisanslı içerik; bizde yapılmaz, ses kitaplığı (#3) için örnek.
  - "17 sa. 38 dk. sonra çalacak >" satırı (#51).
  - Alarm kartında haftanın günleri harfle (P P S Ç P C C; hafta Pazar'la başlıyor, Türkçe için yerelleştirme hatası) ve turkuaz anahtar.
  - Sağ altta kırmızı yuvarlak "+" düğmesi.
- "Alarm bağlı cihazda çalsın mı? Bağlantı kesilirse alarm mevcut cihazda çalar": koyu alt sayfa, iç içe mavi halkalar ortasında kulaklık simgesi, kırmızı "Ayara gidin" ve gri "Hayır, teşekkürler". → yeni iş 79.
- "Alarm iznini ver" açıklama penceresi: koyu cam görünümü, "Şimdi değil" ve turkuaz "İzin Ver" düğmeleri. → yeni iş 80.

**Ücret notları (bu gönderimden):**
- Haftalık ₺399,99 abonelik.
- "Reklam izle, 1 gün VIP ol" (Fullscreen Clock).
- "Remove Ads" (Fullscreen Clock).
- "PRO Free Trial" (Alarmy).
- Bizde reklam, izleme (ATT) ve abonelik yok. Bu sayfalar yalnız fiyat ve düzen fikri için not edildi.

### Sahibin üçüncü gönderimi ve gözlemleri (3 Kasım 2026)

**Varlık Saati (başka konuda bir uygulama; 2 görüntü):**
- **Ekran görüntüsü uyarısı.** Üstte krem zeminli, yuvarlak köşeli kart: göz simgesi ve "Ekran görüntüsü alındı. İçinde tutarlarınız olabilir; paylaşırken dikkat edin.". Bizde #82 olarak yapıldı.
  - Uyarı bütün uygulamada değil, yalnız kişisel kayıt içeren üç ekranda çıkar: namaz ve oruç takibi, zekât, sabah günlüğü. Saat ekranının görüntüsünü almak olağan bir şey; orada uyarı çıkması rahatsız eder.
  - Görüntü uygulamaya gelmez, engellenmez, hiçbir şey kaydedilmez.
  - Android'de yalnız 14 ve üstünde çıkar; daha eski sürümlerde güvenilir bir yol yok.
- **Açılış ekranı.**
  - Görülen: lacivert-siyah zemin, büyük, altın işaretli, fırçalanmış metal görünümlü kadran fotoğrafı. Ortada logo, "Varlık Saati" yazısı (ikinci sözcük turkuaz) ve ince gri "O gün verdiğiniz kararın bugünkü karşılığı".
  - Değerlendirme (#83): Saat uygulaması açılır açılmaz saati göstermeli; markalı, bekletmeli bir açılış ekranı her açılışta 1–2 sn kaybettirir. Apple da açılış ekranının ilk ekrana benzemesini öneriyor.
  - Önerilen: sistem açılış ekranına yalnız uygulama simgesini ve tema zeminini koymak (iPhone `UILaunchScreen`, Android 12+ `SplashScreen`). Bekleme eklemez, yalnız boş ekranın yerini alır.
  - Markayı ve tanıtımı anlatma yeri ilk açılış sihirbazıdır (#4); orada bir kez gösterilir.
  - Görseldeki kadran fotoğrafı ise kadran koleksiyonu (#1) için iyi bir örnek: fırçalanmış metal, altın üçgen işaretler, ince dakika çizgileri.
- **Üye ol ekranı.** "Ücretsiz üye ol · Kart bilgisi istenmez", turkuaz tam genişlikte düğme, altın renkli seçili haplar (yıl: 2010–2022; yer: Altın, Dolar, Euro). Bizde üyelik yok, not olarak kaldı.

**Bir çalışma sayacı (yatay, adı görünmüyor):**
- Siyah zemin, yedi bölütlü sarı LED rakamlar (#FFB300 civarı, ışımalı), sönük bölütler de görünüyor ("00:00:00").
- Üstte "Genel" ders/konu seçici, yan yana "Sayaç" ve "Pomodoro" (seçili olan kahverengi-altın zeminli), sağda not, istatistik, ızgara ve ayarlar simgeleri.
- Altta "Sıfırla", sarı dolu "Başlat", "Not", "Soru" (çözülen soru sayacı).
- → #84 (ders çalışma sayacı). LED görünümü #9'a ek ayrıntı.

**Sahibin Fullscreen Clock gözlemleri (telefonda kullanarak):**
- Saniyede tik-tak sesi var. Bizde sitede var, uygulamada yok → #10.
- Parmakla yukarı-aşağı kaydırınca ekran ışığı artıyor ya da azalıyor. Bizde var: tam ekranda kenardan kaydırarak karartma (`fs.dim`).
- Widget'ta saniye sorunsuz işliyor. Bizde iOS'ta yalnız düz metin → #18.
- Kilit ekranı widget'ı ayrı. Bizde kilit ekranında dairesel widget yok → #14.
- Ekrana yazı yazma var. Bizde yalnız küçük bir satır → #22.

## Ücretli sistemle birlikte gelecekler (3 Kasım 2026, sahibin kararı)
Sunucu, hesap ya da ödeme gerektiren işler; şimdi yapılmayacak, ücretli bölüm ve ödeme yöntemleri açılınca yeniden ele alınır:
- **#53** Arkadaşlarla uyanma grupları, sanal para (Wayk, SleepTown).
- **#75** Ortak zikir ve hatim, topluluk, radyo, fetva araması (Codeida, Azan Time, Muslim Pro, e-Diyanet). "ALO 190" arama düğmesi sunucu istemez; ayrı yapılabilir.
- **#25** Bağış (bahşiş) satın alması (ClockZ, Vakit): uygulama içi satın alma ve ödeme yöntemleriyle birlikte ele alınır (3 Kasım 2026, sahibin kararı).
- Favori camilere yazılan notların başka kullanıcılarla paylaşımı (kullanıcı izin verirse): sunucu ister; notlar şimdilik yalnız telefonda (4 Kasım 2026, sahibin kararı: "notlar telefonda, paylaşım sonra").

## Kapsam sınırı (3 Kasım 2026)
Birinci ajan dalgası üç alana baktı: kadran/masa saati (ikinci öncelik odak ve zamanlayıcı), alarm/uyku, namaz (ikinci öncelik takvim, dünya saati, geri sayım). **Bakılmayanlar:**
- saat siteleri (time.is, timeanddate, saat-kac.tr: web tarafı hiç taranmadı),
- hava durumu uygulamaları,
- takvim uygulamaları,
- dünya saati uygulamaları (yalnız Time Buddy),
- kronometre ve çoklu zamanlayıcı uygulamaları,
- Ay, gökyüzü ve astronomi uygulamaları,
- Android/Google Play tarafı (birkaç uygulama dışında),
- Apple Watch ve Wear OS kadranları (yalnız Facer).

Bu alanları ikinci ajan dalgası taradı (3 Kasım 2026); aşağıda "İkinci ajan dalgası" bölümü.

## Ajan taraması (3 Kasım 2026)

Üç ajan 43 uygulamanın App Store ekran görüntülerini (iPhone ve iPad, yaklaşık 400 görüntü) tek tek açtı. Bazılarında Google Play görüntülerine de bakıldı. Her uygulama için zemin rengi, kadranlar, uygulama içi satın almalar (₺) ve "bizde var mı" kanıtı (dosya yolu) ham notlarda:

| Alan | Ham not | Uygulamalar |
|---|---|---|
| Kadran, masa saati, widget, odak | `docs/rakip-analizi-2-ham/kadran.md` | Analog Clock (Draunt), Desk Clock, Flip Clock: Digital Desk Widget, Zen Flip Clock, Fliqlo, ClockZ, Widgetsmith, Clocks, Clock Face, Digital Clock - Bedside, Fullscreen Clock, Bauhaus Clock, Facer, Forest, Focus To-Do |
| Alarm ve uyku | `docs/rakip-analizi-2-ham/alarm.md` | Alarmy (App Store + Play), Sleep Cycle, RISE, Pillow, ShutEye, Loud Alarm Clock, Alarm Clock for Me, Wayk, SuperAlarm, SleepTown, Apple Saat, Sleep as Android, Google Saat, Alarm Clock Xtreme |
| Namaz, takvim, dünya saati, geri sayım | `docs/rakip-analizi-2-ham/namaz.md` | Vakit, Azan Time Pro, Ezan Vakti Pro, e-Diyanet, Muslim Pro, Pillars, Codeida Namaz Vakti, Fazilet, Temkinli Vakitler, Türkiye Takvimi, Quran Widgets, Countdown Widget, Time Buddy |

**Bakılamayanlar:**
- BetterSleep, Challenges Alarm Clock, Samsung Saat, IslamicFinder Athan (TR mağazada yok).
- Android canlı duvar kâğıtları.
- Ödeme (Premium) sayfaları ve ilk açılış ekranları mağaza görsellerinde yok. Bunlar yalnız sahibin telefonundan görülebilir; sahibin görüntüleri bu yüzden en iyi kaynak.

**Doğrulama:** Ajanların "bizde yok" dediği maddelerden bazılarını kodda yeniden aradım ve hepsinin doğru olduğunu gördüm:
- iPhone kilit ekranında dairesel widget yok.
- Alternatif uygulama simgesi yok.
- Cuma hatırlatması yok.
- İftar widget'ı yok.
- Bayram namazı vakti yok.
- Geri sayım tarih geçince ileri saymıyor.

### Mevcut maddelere ek ayrıntılar
- **#1 Kadran koleksiyonu:**
  - Rakiplerde görülen kadran öğeleri: Roma rakamı, döndürülmüş rakam, 5–60 dakika halkası, kronograf alt kadranları, demiryolu tipi dakika çizgisi, neon ışıma, siluet ibre, ok uçlu ibre, porselen/ahşap/galaksi dokusu.
  - Kadranlar hazır renk takımıyla geliyor (zemin + kadran + vurgu).
  - Kadranı parçalara bölersek (rakam türü × ibre × çerçeve × doku) az işle çok kadran çıkar.
  - Bizde tek kadran çizimi var (`AnalogClock.swift`); renkler yalnız temadan geliyor.
  - Pusula görünümleri, tespih tasarımları ve Kur'an temaları da aynı "görünüm koleksiyonu" kalıbıyla satılıyor.
- **#2 Alarm duvar kâğıdı:** Alarmy'de "My Photos" (kendi fotoğrafın ya da videon) ve Illustration kategorisi var. Kartta görüntüyle birlikte ses paketi de gösteriliyor (♪ adı). Bizde alarmda arka plan alanı hiç yok.
- **#3 Ses kitaplığı:**
  - Kendi sesini kaydetme, telefondaki müzik, arama.
  - "Faith" (inanç) ve motivasyon konuşması kategorileri.
  - Kategoriden rastgele ses.
  - Wayk'ta Classic 24, Aggressive 24, Motivation 17, Viral 26, Cinematic 15, Peaceful 16, Faith 12 ses var.
- **#5 Dil seçimi:** Vakit 25 dilde anında geçiş yapıyor; Temkinli'de dil ayarlardan seçiliyor.
- **#6 Puanla:** Time Buddy'de "Like the app? Rate it!" satırı var. Muslim Pro ve Countdown Widget sürüm notlarında yorum istiyor.
- **#7 Oyunlaştırma:**
  - SleepTown: yatma ve kalkma hedefine uydukça bina ve kasaba büyüyor.
  - Forest: çalıştıkça orman büyüyor.
  - Bunlar Bumba standardı madde 13'e (rozetler) uymalı. İbadette seri ve rozet sahibin kararıyla yok.

### İlk analizde "bitti" sayılıp ekranda eksik çıkanlar
Bu satırlar ilk analizde kapanmış görünüyordu. Ekrana bakınca aşağıdaki kısımlar eksik çıktı; bu kısımlar yukarıdaki yeni işlere eklendi.

| İlk analiz | Eksik kalan | Yeni iş |
|---|---|---|
| #1 Gece modu | Lüm gece kadranı (renk seçimli), çizelgeye göre geçiş; piksel kaydırma yalnız gece modunda | 11 |
| #21 Silme engeli | iOS'ta yok (Android'de geri tuşu kapalı) | 46 |
| #42 Live Activity | Vakit uygulaması sonraki 3 vakti gösteriyor, bizde 1 (küçük fark) | — |
| #43 Alarm ayarları | Kısalan erteleme, saate bağlı erteleme sınırı, alarm başına ses düzeyi | 40 |
| #44 Çevirmeli saat | Tek düzen, tek renk | 20 |
| #46 Kıble | Harita ve kamera görünümü yok, yalnız pusula | 67 |
| #48 Saniyeli widget'lar | iOS'ta saniyeli analog/flip yok; Dynamic Island'da saat yok | 18, 19 |
| #50, #75 Kapatma görevleri | Birden çok görev, nesne tanıma, ertelemeye görev, görev önizleme; uyanma denetimi uygulama içinde değil | 34–39, 45 |
| #51 Geri sayımlarım | Tarih geçince ileri sayım yok | 73 |
| #52, #53 Arka plan ve uyku sesleri | 5 ses, karışım yok; rakiplerde 9–40+ | 30 |
| #58 Görünüm | LED/yedi bölüt, dış hatlı rakam, rakam rengi yok; 6 zemine karşı 28+; alarm çalma ekranında arka plan yok | 9, 23, 24, 2 |
| #69 Esmaü'l-Hüsna | Hat görseli ve sesli okuma yok | — |
| #71 Namaz rehberi | Resimsiz, düz metin | 68 |
| #72 Takvim yaprağı | "Arka yaprak" metni, ayet/hadis arşivi ve kaydetme yok | 16 |
| #77, #78 Widget'lar, Wear OS kadranı | Kilit ekranında dairesel widget yok; hava/takvim/fotoğraf widget'ı yok; kadranda renk seçimi yok | 14, 15, 32 |
| #92 Ezan sesleri | 2 ses ve tek seçim; rakiplerde 14–20+ ses, vakit başına ses | 54 |
| İlk analiz tablosu satır 149 (dini gün ve Cuma hatırlatması) | Cuma hatırlatması yok | 58 |
| İlk analiz tablosu satır 150 (iftar sayacı ve widget'ı) | Vakitler ekranında ikinci sayaç ve iftar widget'ı yok | 55 |

### Ortak tasarım kalıpları
- **Zemin:**
  - Saat ekranlarında saf siyah (#000) ya da koyu gri (#161616–#424242).
  - Alarm uygulamalarında lacivert ya da siyah (#0B1F5C, #16161C, #1B1E33).
  - Namaz uygulamalarında koyu lacivert ya da yeşil (Vakit #2B3050, Muslim Pro #0F3D33, Pillars #0B1D3F).
  - Bauhaus ve Draunt düşük doygunlukta, zemin ve kadran aynı renkte (bej #D5D1C5, gri-mavi #DCDFE4).
- **Vurgu:**
  - Tek güçlü renk: kırmızı (Alarmy), sarı (SuperAlarm), turuncu (Sleep Cycle), mor (Pillow, Rise), turuncu/altın/turkuaz (namaz uygulamaları).
  - Rakamda neon yeşil (#1DF06D) ya da açık gri (#BABABA).
- **Kadran galerisi:**
  - 2–3 sütunluk küçük resim karoları, gri metal çerçeve.
  - Ücretli olanda köşede sarı çapraz "VIP" şeridi ya da taç; seçili olanda mavi üçgen ya da yeşil onay.
  - Bölümler "Analog / Digital" ya da "Classic / Popular".
  - Renk seçimi adlı yuvarlak daireler ("Rose", "Lavender"); seçili olan beyaz ya da siyah halkalı.
- **Saat ekranı:**
  - Tam ekran saat, köşelerde ince simgeler.
  - Sık görülen dört simge: tişört (kadran), zil + "OFF" (alarm), dişli (ayarlar), kronometre.
  - Bazılarında düğme yok; dokununca beliriyor.
- **Yerleşim:**
  - Kategoriler emoji'li, yatay kaydırmalı hap biçiminde.
  - 16 pt köşeli kartlar.
  - Düzenleyici alt sayfa olarak açılıyor.
  - Alt çubuk 5 sekmeli, seçili sekme hap içinde; yüzen hap biçimli alt çubuk sık.
  - Görev seçimi iki sütunlu, renkli çizimli kare kartlar.
- **Namaz ekranları:**
  - Büyük sayaç; saniye küçük ya da ayrı kutuda.
  - Sıradaki vakit dolu renkli hap; her vakitte gök simgesi.
  - Gün ilerlemesi için Güneş eğrisi ya da yarım daire gösterge çok yaygın.
  - İki tarz var: sade Apple tarzı (Vakit, Pillars, Fazilet) ve fotoğraflı/geleneksel tarz (cami fotoğrafı, çini sure başlığı, desen bordür, tespih boncuğu).
- **Raporlar:**
  - Puan halkaları (Sleep Cycle 88, ShutEye 92).
  - Ölçü kartlarında "Healthy", "High risk" etiketleri ve kesik çizgiyle normal aralık.
- **Oyunlaştırma:** 🔥 seri hapı, altıgen rozetler, maskotlar (güneş, ay, yastık, yumurtadan çıkan yaratık).
- **Mağaza görselleri:**
  - Pastel düz zemin üstünde iki satır başlık (gri ince + siyah kalın).
  - Defne dallı sosyal kanıt ("100M+", "4.8★", "World's #1"), basın logoları.
  - Bağımsız geliştiriciler gerçek ortam fotoğrafı kullanıyor.
- **Rakiplerdeki hatalar (bizde olmamalı):**
  - Bozuk çeviriler: Alarmy "Uyanma Checks", "ös 6:05"; Xtreme "Bir sonraki alam"; Analog Clock "Onarmak".
  - Taşan düğme yazısı.
  - Azan Time hatim tarihi "01.01.1970".
  - Quran Widgets'ta Chicago vakitleri ~9 saat kaymış.
  - Codeida'da aynı bayram namazı için iki farklı saat.

### Ücret modelleri (rakipte)
- **Çinli yayıncı ailesi** (Desk Clock, Flip Clock, Digital Clock, Clock Face):
  - Reklamlı.
  - Tek seferlik satın almalar: "Remove Ads" ₺49,99, "All styles" ₺49,99, Pro ₺149,99–199,99.
  - Kadranlar tek tek de satılıyor: ₺49,99 her biri, hepsi ₺499,99.
- **Bağımsız ve reklamsız:**
  - Tek seferlik paket: Clocks ₺199,99, Analog Clock Pro ₺399,99.
  - Ücretli uygulama: Bauhaus ₺799,99, Fliqlo ₺49,99.
  - Abonelik ve ömür boyu birlikte: Zen Flip, ayda ₺49,99 ya da ömür boyu ₺449,99.
- **Alarm ve uyku:**
  - Neredeyse hepsi abonelikli; haftalık plan da var (Alarm Clock for Me ₺39,99–99,99).
  - Wayk'ta abonelik olmadan alarm kurulamıyor.
  - Pillow akıllı uyandırmayı Premium'da satıyor; bizde ücretsiz.
- **Namaz:**
  - Reklamsız rakipler bağışla (Vakit: ₺10–500 tek seferlik, ₺1.000–10.000 aylık) ya da Premium tema ve simgeyle (Pillars) yaşıyor.
  - Reklamlı Türk rakipler "reklamları kaldır" için ₺99–1.300 ve abonelik için ₺40–350 alıyor.
  - Fazilet "Arkadaşım / Ailem / Komşum" hediye paketleri satıyor.
- **Bizim için not:** Saat Tarih reklamsız ve ücretsiz.
  - Rakiplerin para aldığı yerler çoğunlukla görünüm içeriği: kadran, tema, zemin, ses paketi, pusula ve tespih tasarımı. "İleride Pro adayı" diye işaretlediğimiz şeyler bunlar.
  - Alarm, vakitler, takvim, akıllı alarm ve görevler rakipte çoğu zaman ücretli; bizde hep ücretsiz kalır.
  - Ücretli bölüm açmadan gelir istenirse en uygun yol bağış (#25). Reklamsız rakip Vakit bununla yaşıyor.

## İkinci ajan dalgası (3 Kasım 2026)

Sahibin isteğiyle ilk dalganın bakmadığı alanlar tarandı. Beş tarama ajanı ve bir tasarım ajanı çalıştı. Kurallar şunlardı:
- Her görüntü açılıp bakıldı.
- Renkler piksel ölçülerek (PIL) yazıldı, tahmin edilmedi.
- "Bizde var" ya da "kısmen" yalnız kodda dosya yolu kanıtıyla yazıldı.

Sonuç: 106 yeni iş (#87–#192; 189–192 doğrulamadan sonra eklendi). Bunların 8'i bizde hata (#87–#93 ve #186); en önemlileri hava, yüzde ve sitedeki kıble pusulası hatalarıdır (#87, #88, #186). Hataları, ilk analizin numaralarını ve en önemli "bizde yok" iddialarını kodda yeniden denetledim:
- `prefix(7)`, `forecast_days=7`, `"%\(…)"`;
- `nature-calc.js` evre dilimi;
- `MoonView.swift` emoji;
- `cities.json` 31 kayıt;
- `Theme.kt`'de dinamik renk olmaması;
- widget boyutları;
- sitede erteleme 5 dk; saat sayfalarında sekme başlığı canlı değil (araçlarda `setTitle` ile sayaç başlıkta, `tools.js:27`); ana sayfa ve şehir sayfalarında senkron satırı yalnız `#info-dialog` içinde (araç sayfalarında görünür).

Sonra bağımsız bir doğrulama ajanı raporu denetledi (aşağıda "Doğrulama").

| Alan | Ham not | İncelenenler |
|---|---|---|
| Saat ve zaman siteleri | `docs/rakip-analizi-2-ham/web-siteler.md` | time.is, worldtimebuddy, vClock, onlinealarmkur.com, online-stopwatch, saat-kac.tr, canlisaat.com, onlinesaat.web.tr, namazvakti.com, everytimezone, 24timezones, savvytime ve bizim site (Playwright ile 390 ve 1280 px, açık/koyu; 74 görüntü) |
| Hava durumu ve takvim | `docs/rakip-analizi-2-ham/hava-takvim.md` | CARROT, AccuWeather, Weather Live°, The Weather Channel, MGM Hava Durumu, Windy, Yahoo Weather, Fantastical, Calendars (Readdle), Structured, Google Calendar, Türk Takvimi, Serhend Takvimi, Saatli Maarif Takvimi, Osmanlı Takvimi (124 görüntü) |
| Zaman araçları | `docs/rakip-analizi-2-ham/araclar.md` | Countdown Star, Countdown (Find Appiness), Days, Timer+, MultiTimer, Interval Timer, Seconds, Timeglass, Visual Countdown Timer, Stopwatch‰, World Clock Widget (Lasmit), World Clock Widgets (Overdesigned), World Clock Pro, Flora, Focus Keeper, Be Focused (119 görüntü) |
| Ay, Güneş, gökyüzü | `docs/rakip-analizi-2-ham/gokyuzu.md` | The Moon, MOON, Deluxe Moon Pro, Moon Phases and Lunar Calendar, Daily Life with Moon Calendar, Sky Guide, Night Sky, SkyView Lite, Star Walk 2 Plus, Sky Tonight, Sun Seeker, PhotoPills, Lumy, SolarWatch, Lumos, Star Walk Kids ve bizim site ile Android ekranları |
| Google Play, widget/StandBy, saat kadranları | `docs/rakip-analizi-2-ham/android-widget-saat.md` | Google Saat (Wear), Samsung Saat, Ezan Vakti Pro (Play), Ezan360, Muslim Pro (Play), Alarmy (Play), Alarm Clock Xtreme, KWGT, Digital Clock Widget, StandBy Mode (Android), Facer, Pujie, WatchMaker, Color Widgets, WidgetClub, Clock Widget with Seconds Live, Clockology, Pray Watch |
| Tasarım (sahibin 36 görüntüsü + 12 rakip + biz) | `docs/rakip-analizi-2-ham/tasarim.md` | Sahibin dört gönderiminin her görüntüsü ayrı bölümde (1.1–1.36); Alarmy, Analog Clock, Fullscreen Clock, Desk Clock, sürüm 52 uygulaması, Sleep Cycle, Rise, Pillow, Muslim Pro, Athan Pro, Codeida, Vakit, Pillars; bizim belirteçlerimiz ve 15+ Android ekranımız |

**Okunamayanlar ve sınırlar:**
- timeanddate.com: Cloudflare doğrulaması 403 verdi; takvim, Ay, Güneş ve geri sayım sayfaları görülmedi.
- Galaxy Store sayfası JS ile yükleniyor; Play sayfası okundu. Play TR sayfası uygulama içi satın alma fiyatlarını göstermiyor.
- Apple Watch ekran görüntüleri App Store sayfasında gelmiyor.
- Rakiplerin ödeme ve ilk açılış ekranları mağaza görsellerinde çoğunlukla yok.
- Web taraması ABD çıkışlı bir vekil sunucudan yapıldı. AB ve Türkiye ziyaretçisine çıkan çerez bantları görülmedi; time.is konumu "Columbus, Ohio" sandı.
- Bakılmayanlar: ezanvakti ve Saatli Maarif web sürümleri, saatkac.info.tr (time.is'in Türkçe kopyası), Timepage, Top Widgets⁺, iScreen, Themify, Apple Hava Durumu.
- Robolectric ekran görüntülerimizde yazı tipi cihazdakinden farklı, bulanıklık ve gölge çizilmiyor. TextClock çizilmediği için `widget-dijital.png` boş görünüyor; bu test kusurudur, uygulama hatası değil.

### Sahibin görüntülerinde önceki notlardaki düzeltmeler
Tasarım ajanı sahibin 36 görüntüsünü piksel ölçerek yeniden inceledi. Aşağıdaki düzeltmeler yukarıdaki "Sahibin ekran görüntüleri" bölümlerine işlendi:
- "Ses Açık" hapı uygulamanın değil, iPhone'un Dynamic Island zil göstergesi.
- "Yenilikler" sırası İmparatorluk Yeşimi → Minimal Radar → Liquid Mercury.
- "Kadran" kartında rakamlar eğik değil, radyal döndürülmüş; kartın adı kesin değil.
- Alarmy kırmızısı ölçülünce #fc314c çıktı (#FF2D55 değil); önizlemedeki "Seç" düğmesi #e84652. Düğme metinleri ters yerde. Kurulum adımları 2/4, 3/4, 4/4 olarak görüldü; 1/4 görülmedi.
- Sürüm 52 uygulamasında büyük harfler noktasız I ile yazılmış ("SENIN MODUN"). Destek simgeleri #70463d renginde ve temaya uymuyor.
- Dil listesinde ülke adları ve yanlış yerel adlar var.
- Haftanın günleri Pazar'la başlıyor.

**Atlanmış ayrıntılar** (ham notta tek tek yazılı):
- **Analog Clock:**
  - "thusday" yazım hatası.
  - "Ömür Boyu…" satırı kutudan taşıyor.
  - Yüzen cam öğeler içeriği örtüyor ("Önizlem…").
  - Görünen 6 klasik kadranın 2'si ücretsiz (galeri yana kayıyor, 3. sütun kesik).
  - Puanlama kartı saat ekranındaki tarihi örtüyor.
  - Aynı uygulamada iki ayrı düğme biçimi var.
- **Alarmy:**
  - Yapışkan çip satırı.
  - Görev adımında "Sonraki" düğmesi yok.
  - "Evcil Ha…" yazısı kesik.
  - 30 sn'lik video reklam.
- **Premium sayfası:** İspanyolca yorumda dilbilgisi hatası; iki farklı yıldız çizimi.
- **"Siber Kireç" teması:** beyaz üstündeki limon renkli ✓ işaretinin karşıtlığı 1,1:1.
- **Sürüm 52 uygulaması:**
  - Seçili olmayan sekme simgelerinin karşıtlığı 1,9:1.
  - "Sen" ile "siz" hitabı karışık.
- **ATT penceresi:** gerekçe metni kesik.
- **Fullscreen Clock:** kadranda el yazısı rakamlar var; seçili karoda hiçbir işaret yok.
- **Varlık Saati:** ekran görüntüsü uyarısı örnek veri ekranında da çıkıyor. Bizim "yalnız kişisel kayıt ekranları" seçimimiz (#82) daha doğru.
- **Çalışma sayacı:** yeniden ölçüldü, #84'teki not doğru. LED #FFB300, sönük bölütler #140E00, "Başlat" #F0BE6D.

### Tasarım kimlikleri (ölçüm)
Renkler piksel ölçümüdür. Sahibin görüntüleri 1179×2556 px (pt = px/3).

| Uygulama | Zemin (açık / koyu) | Yüzey | Vurgu | Yazı tipi | Alt çubuk | İlk açılış | Görsel zenginlik |
|---|---|---|---|---|---|---|---|
| Alarmy | — / #09090b | #18181b | #fc314c + #2dbde0 | SF benzeri | yüzen cam hap, 5 sekme, seçili sekme hapta | ≥7 ekran | çok yüksek |
| Analog Clock | — / #000, #06070b | #101010 | beyaz düğme, #dcc5a2 | SF Display | yok | "Yenilikler" 10 sayfa | çok yüksek |
| Fullscreen Clock | — / #000 | #2a282b | #4384f3, #607d8a | SF, dev rakam | yok (karo şeridi) | ATT | düşük |
| Desk Clock | — / #424242, #161616 | #272727 | yok | SF + LCD | yok | — | kadranda yüksek |
| Sürüm 52 uygulaması (adı yok) | tema gradyanı (#342b46→#100f11 vb.) | tema tonlu | #ac89f8 / #ec5f59 / #f1fe67 / #68c1f9 | geniş aralıklı büyük harf | siyah, 6 sekme, yalnız simge | — | orta |
| Sleep Cycle | — / #000 | #0c0b10 | #fe8204 | SF + serif (pazarlama) | 5 sekme | — | orta-yüksek |
| Rise | — / #090909 | #232329 | #701afe | SF | 5 sekme | — | orta |
| Pillow | — / #000 | #1b1b25 | #9f40ff + #ff8c45 | SF | 5 sekme | — | yüksek |
| Muslim Pro | okuyucuda kâğıt #fcf4dd / #062622 | #083231 | #0c9456 | SF | 5 sekme | — | yüksek |
| Athan Pro | gökyüzü gradyanı #244781→#8690c3 | buzlu cam | beyaz | serif başlık | yüzen cam hap, 5 sekme | — | çok yüksek |
| Codeida | #fff, #eaf1e8 / Kur'an siyah | mavi #2f6fa5 | #2b6daa + #1f6401 | SF | kısayol karoları | — | orta (çizimli rehber) |
| Vakit | — / #242638 | #2c314f | #40b8a8 | SF | yüzen hap, 5 simge | — | orta |
| Pillars | #faffff / #001d43 | #2c4160 | #e3925d | Poppins benzeri | — | — | orta |
| **Saat Tarih** | **#f7f4ee / #11141a** | #fff / #1a1e26 | #9a3f00 / #f2a65a (+23 tema) | sistem | düz zemin, **7 sekme, 10 sp**, seçili sekme yalnız renkle | **yok** | **düşük** |

Diğer alanlardaki zemin kalıpları:
- **Hava uygulamaları:** zemin koşula göre fotoğraf ya da gradyan, kimi zaman hareketli; kartlar yarı saydam cam.
- **Ay ve gökyüzü uygulamaları:** koyu lacivert ya da siyah (#081329, #0B1C38, #121238, #0E0E10, #000). Güneş uygulamaları gökyüzü mavisi (#29468A–#406FA7) ya da gün batımı gradyanı (#ECDF7F → pembe). Neredeyse hepsinde yalnız koyu tema var.
- **Zaman araçları:** her öğenin kendi rengi var. Evre rengiyle dolan tam ekran sık: Interval Timer #56D495, Seconds #8AFF34 ile #FF4538, Focus Keeper #FF5C5D. Düzenleyiciler iOS gruplu formunda (#F3F2F8).
- **Türk takvim uygulamaları:** bayrak kırmızısı (Türk Takvimi) ya da yeşil ve altın (Serhend, Saatli Maarif, Osmanlı Takvimi).
- **Saat siteleri:** Türkçe sitelerde varsayılan tema koyu (canlisaat, onlinealarmkur). Eski sitelerde köşesiz düz tasarım (time.is, vClock), yenilerde 8–16 px köşe.

### Tasarımımız: zayıf ve güçlü yanlar
**Zayıf yanlar.** Somut işler: #94–#105; ek ayrıntılar #4, #65, #78 ve #33'te.
1. İlk izlenim boş: tanıtım yok, saat ekranının ~%58'i boş zemin.
2. Görsel öğe çok az, yerine emoji var. Boş durumlar tek gri satır. Namaz rehberi düz metin.
3. 24 tema görünmüyor: seçici adsız, ikiye bölünmüş daireler (`InfoViews.swift` 86–125). Tonlar arasındaki fark %4–20.
4. Alt çubuk kalabalık: 7 sekme, 10 sp yazı, seçili sekme yalnız renkle belli. Simgelerin yarısı dolu, yarısı çizgi.
5. Başlık hiyerarşisi zayıf: sayfa başlıkları ~20 sp, rakiplerde 30–44 pt.
6. Vakitler ekranı tablo gibi.
7. En zayıf ekranlar zamanlayıcı, pomodoro ve aralık (#101). Ay her yerde emoji (#103).
8. Hata rengi platformlar arasında tutarsız.

**Güçlü yanlar (korunmalı):**
- Karşıtlık her yerde ≥6:1 (6,1–9,1:1).
- Site ve iki uygulama aynı belirteçleri kullanıyor.
- Açık ve koyu tema tam.
- Türkçe büyük harf doğru (`Imsakiye.kt:115` yerel ayarla çeviriyor).
- ATT, reklam ve izleme yok.
- Dynamic Type büyük ölçüde destekli.
- Ekran görüntüsü uyarısı (#82) rakipten daha doğru yerde çıkıyor.
- Saat, hava, vakit ve takvim tek uygulamada; rakiplerin hiçbirinde bu dördü bir arada yok.
- Lumy dışında reklamsız ve ücretsiz bir rakip yok. Lumy de cihazda çalışan, sunucusuz bir uygulama ama ücretli.

### Mevcut maddelere ek ayrıntılar (ikinci dalga)
- **#2 Alarm duvar kâğıdı:** Samsung Saat'te alarm zemini kişinin kendi fotoğrafı ya da videosu olabiliyor; hava zemini ve takvim etkinliği de gösteriliyor.
- **#4 İlk açılış:** 3 sayfalık, atlanabilir tanıtım önerisi (tasarım T1):
  - Zemin kâğıt #f7f4ee / gece #11141a.
  - Her sayfada tek bir çizgi illüstrasyon (ana renk, 2 pt).
  - Başlık 28–32 pt kalın.
  - "Geç" düğmesi hep görünür.
  - 3. sayfa özellik anahtarları (Ayarlar › Özellikler).
  - Sayı iddiası yok.
- **#6 ve #81 Puanla, paylaş:** Ayarlarda "Destek" grubu (tasarım T11): Değerlendir, Paylaş, Geri bildirim, Bumba ürünleri, Sürüm. Simgeler temaya uygun, hitap tek ("siz").
- **#8 Havaya göre zemin:** Hava sekmesinin zemini de koşula göre değişmeli (Weather Live, The Weather Channel, AccuWeather, Yahoo, MGM). Bugün düz #f7f4ee; ilk analiz #79 yalnız tam ekran saatte yapıldı.
- **#9 Dijital kadranlar:**
  - Widget'larda yedi bölütlü LCD (sönük bölütlerle), nixie, neon ve çevirmeli saat (Clock Widget with Seconds Live, ömür boyu ₺499,99).
  - Sitede LCD yazı tipi (vClock).
- **#10 Tik-tak sesi:** Çocuk zamanlayıcısında da var (Visual Countdown Timer).
- **#11 Gece kadranı:** gece modu rengi ve ışık sensörü ayrı iş oldu (#189).
- **#13 Alternatif simge:** Fantastical'da 6 alternatif simge (Premium). iOS 18 koyu ve renklendirilmiş simge ayrı iş (#171).
- **#14 Kilit ekranı:** Ay ve dünya saati kilit ekranı widget'ları; kilit ekranı satırında gün doğumu/batımı (Color Widgets).
- **#15 Widget'lar:**
  - Hava widget'ları: anlık, saatlik, 5 gün; kilit ekranında eğri ve dairesel (CARROT, Windy, Yahoo, The Weather Channel).
  - Saatlik hava şeridi, 8 sütun (Color Widgets).
  - Güneş widget'ları: yay, 24 saatlik halka, ekinoks geri sayımı.
  - Takvim: ay ızgarası ve yaklaşan özel günler widget'ı (Readdle, Fantastical).
  - Color Widgets'ın sürüm notunda "iOS 27 XL widget" boyutu geçiyor (yalnız sürüm notundan; doğrulanmadı).
- **#16 Günün içeriği widget'ları:** Takvim yaprağı widget'ı. `CalendarLeafView` ve `CalendarLeafScreen` var ama widget'ları yok.
- **#17 Widget düzenleyici:**
  - Renk daireleri, yazı tipi çipleri, boyut kaydırıcısı, fotoğraf zemini ve tam genişlik "Set Widget" düğmesi (Color Widgets).
  - HSV renk + hex + 10 yazı tipi + ICU tarih kalıbı (Digital Clock Widget).
  - Paketler: Color Widgets ₺149,99–2.499,99, WidgetClub temaları ₺799,99–2.999,99.
- **#19 Dynamic Island:** iki şehrin saati (Lasmit).
- **#25 Bağış:** Lumy ₺49,99–499,99 bahşiş kademeleri; MOON'da "Patron" ₺1.499,99.
- **#31 Odak istatistikleri:**
  - Görev başına tekrar günü ve tahmini tur ("6/7").
  - Oturum zaman çizelgesi, mola süresi kaydı.
  - Grafikte hedef çizgisi ve süzgeç.
  - Ağaç ölümü ekranı, keşif haritası, alışkanlık zinciri takvimi (Focus Keeper, Be Focused, Flora).
- **#32 Wear OS kadranı:**
  - Kadranda renk seçimi: Facer 9, WatchMaker 28 renk; Pujie'de palet ve gradyan.
  - Ortam modu (AOD) için ayrı, kısık tasarım (Pujie "Ambient"). Bizde ortam modunda yalnız saniye ibresi gizleniyor (`watchface.xml` `Variant AMBIENT`).
  - Komplikasyon yerleri yalnız `SHORT_TEXT` alıyor.
  - Eski kadran API'si Wear OS 6'da kapandı; Watch Face Format seçimimiz doğru.
- **#33 Mağaza görselleri:**
  - Tasarım T13: #f7f4ee üstünde telefon ve kısa, kalın başlık; gece görselleri #11141a (Vakit kâğıt #f9f6ef, Sleep Cycle).
  - Play'de "Veri toplanmıyor" bir satış konusu (Samsung Saat, Digital Clock Widget). Vakit uygulamalarının hepsi konum ya da reklam kimliği paylaşıyor; Pray Watch "Always free, no ads" diyor. Bizim mağaza metnimizde reklam kimliği olmadığı öne çıkarılabilir (`PLAY_CONSOLE.md` §4).
- **#40 ve #51 Alarm:** Sitedeki eksikler (erteleme süresi, ses düzeyi, geri sayım halkası) → #181.
- **#54 Vakit başına ayar:** Serhend'de her vakit için ayrı "önce" süresi hapı (60/30/45 dk).
- **#55 İftar:** "13 days left until Ramadan" widget'ı (Pray Watch).
- **#57 Dini günler:** Serhend'de ay başlıklı kartlarda hicri tarihle; Pray Watch'ta Ramazan geri sayımı.
- **#60 Halk takvimi:** Saatli Maarif'te hıdırellez ve cemre.
- **#65 Vakitler görselleri:**
  - Tasarım T9: satır başına çizgi simge. Yalnız geri sayım kartında vakte göre ince ton geçişi (imsak #e4e8f2, öğle #fff6e3, akşam #f6e1d3, yatsı #e8e6f0 → #f7f4ee); liste beyaz kalır.
  - Ezan Vakti Pro (Play): yeşil→sarı→kırmızı gradyan yay, kırmızı "Kerahate 15 dk" rozeti, deri dokulu tema.
  - Site (namazvakti.com): vakit ilerleme çubuğu, başlıkta Ay resmiyle hicri tarih, yıllık vakit çizelgesi.
- **#67 Kıble:** Sitede kıble haritasında Kâbe'ye çizgi (namazvakti.com). Manyetik sapmalı açı #186.
- **#68 Namaz rehberi:** Resimleri kendi illüstrasyon takımımızla (#97).
- **#72 Toplantı planlayıcı (worldtimebuddy, everytimezone):**
  - Süre seçimi: sürüklenen 30 dk'lık cetvel. Bizde takvim kaydı sabit 1 saat (`planner.js` `calLinks`).
  - Hafta sonu işareti, 7 günlük tarih şeridi.
  - Satır sıralama, ev konumu, kısaltma rozeti.
  - Sürüklenebilir "şimdi" çizgisi (everytimezone); fareyle gezdikçe canlı sütun (worldtimebuddy; bizde tıklayınca seçim).
- **#73 Geri sayım:**
  - Renkli dolu kart, sağda koyu tonlu "7 gün" bölmesi (Countdown).
  - "… gündür" sayacı: sigarayı bırakalı 228 gün (MultiTimer), "Being together 365 days" (WidgetClub).
  - Gün, saat ve dakika halkaları (Color Widgets).
- **#76 Şeffaf widget:** Android'de saydamlık kaydırıcısı doğrudan yapılabilir, duvar kâğıdından kırpma gerekmez (Ezan360, Digital Clock Widget).
- **#78 Adlı temalar:**
  - Tasarım T3: yana kaydırmalı ~230×160 pt kartlar. Köşe 20 pt, içinde mini saat; seçili kartta 2 pt ana renk kenar ve ✓; 15 dilde ad.
  - Bugün 24 tema adsız ikiye bölünmüş dairelerde (`InfoViews.swift` 86–125).
  - Tasarım T4 (isteğe bağlı, varsayılan kapalı): hafif zemin geçişi; açık #f7f4ee→#f1e9e0, gece #11141a→#1e1d1e.

### İlk analizde "bitti" sayılıp ekranda eksik çıkanlar (ikinci dalga)

| İlk analiz | Eksik kalan | Yeni iş |
|---|---|---|
| #3 Hicri tarih | Takvim ızgarasında yok, yalnız seçili günün başlığında | 119 |
| #15 Altın ve mavi saat | Bildirimi, widget'ı ve alacakaranlık türleri yok | 151, 152, 15 |
| #33 14 günlük hava | Sitede bitti; uygulamalarda 7 gün gösteriliyor (hata) | 87 |
| #35 Sitede alarm sesi | Alarm geçmişi ve CSV, erteleme süresi, ses düzeyi yok (ilk işin kapsamı dışında; yeni kapsam) | 181 |
| #40 Sitede saat görünümü | Boyut (+/−) ve LCD yazı tipi yok | 177, 9 |
| #42 Live Activity | Dünya saati Live Activity'si yok (ilk işin kapsamı dışında; yeni kapsam) | 19 |
| #47 Ay | Uygulamada Ay emoji ve 8 kalıp; Ay widget'ı yalnız küçük, kilit ekranında yok; dördün tarihleri yok; dördün ve süper Ay bildirimi yok (ilk analiz tablosu satır 326); sitede bölge kodları ve evre adı hatası | 89, 90, 103, 147, 149, 152, 14 |
| #48 Saniyeli, dünya, geri sayım widget'ları | iOS geri sayım widget'ı yalnız küçük boy ve tek etkinlik, kilit ekranında yok; dünya saati en çok 4 şehir. Android'de 12 widget tek görünümde; saydamlık, renk, dinamik renk ve kilit ekranı kategorisi yok | 130, 167, 14, 17, 76 |
| #49 Zamanlayıcılar | İlerleme görseli, renk ve bitiş saati yok; duyuru yalnız evre adı ve son 3 sn | 101, 133, 135 |
| #51 Geri sayımlarım | Renk, tam ekran görünüm, bağlantıyla paylaşma yok | 102, 138, 73 |
| #52 Odak istatistikleri | Mola süresi ve oturum saatleri kaydedilmiyor | 31 |
| #65 Güneş/Ay yayı | Yön bilgisi yok; Ay yalnız gece görünüyor | 158 |
| #72 Takvim yaprağı | Widget'ı yok; Rumi tarih, ezani saat, hadis, tarihte bugün yok | 120, 121, 124, 16 |
| #77 Dokunulabilen widget'lar | Zamanlayıcı ve pomodoro widget'ı yok | 136 |
| #78 Wear OS kadranı | Ortam modunda aynı parlaklık; komplikasyon yalnız `SHORT_TEXT`; renk seçimi yok | 162, 32 |
| #79 Havaya göre arka plan | Yalnız tam ekran saatte; hava sekmesinde yok | 8 |
| #82 Zamanlayıcı geçmişi | Yalnız son 12 ad ve süre; tamamlanan/iptal edilen, saatler, "geri al" yok | 133 |
| #83 Antrenman rutinleri | Yalnız Aralık sekmesinde; sıradaki adım listesi ve adım atlama yok | 135 |
| #87 Gece gökyüzü | Seyrek çizim; açık temada yazı hatası; Venüs aralığı; telefonda tablo taşıyor; yalnız sitede | 91, 156, 157 |
| #88 Tutulma haritası | Kenarları 2°'lik basamaklar; seçici UTC tarihini gösteriyor; yalnız sitede | 90, 159 |
| #90 Apple Watch ve Wear OS | Saatte alarm, zamanlayıcı, kronometre, zikirmatik yok; komplikasyon yalnız metin | 137, 162 |
| #91 Çok ülkeli tatil | Yalnız sitede ve tablo olarak (ızgara yok); uygulama takvimi yalnız Türkiye'yi gösteriyor, yabancı dilde ülke adı yazmıyor | 92, 122, 178 |
| Android eşitleme turu (18 Ekim 2026) | Sitedeki rüzgâr yönü ve günlük mm uygulamaya gelmemiş; Android'in çektiği yön ve doğuş/batış ekranda kullanılmıyor | 106 |
| İlk analizin ajan tablosu, "geniş şehir veritabanı" | Uygulamada hâlâ 31 şehir | 128 |
| İlk analizin ajan tablosu, "sınıf araçları" | 107'lik listeye alınmamış | 142 |
| Sunucu senkronu (CLAUDE.md'de "sahne altında canlı satır") | Ana sayfa ve şehir sayfalarında satır yalnız "i" penceresinde (araç sayfalarında görünür); CLAUDE.md düzeltildi | 174 |

### Doğrulama (bağımsız denetim ajanı)
Birleştirilmiş raporu yazmayan bir denetim ajanı denetledi. Ham notlarla, kodla ve sahibin görüntüleriyle karşılaştırdı. Ayrıntısı oturum dosyalarında kaldı, depoya girmedi. Bulduklarını kodda yeniden denetledim; hepsi doğru çıktı ve belgeye işlendi.
- **Kod denetimi:** 64 satır denetlendi (7 hata satırı ve her alandan 57 satır). 50'si tam doğru, 13'ü kısmen doğru, 1'i yanlış çıktı.
  - **Yanlış olan #104:** en hızlı/en yavaş tur işareti üç platformda da varmış. Satırdan çıkarıldı.
  - **Kısmen doğru olan 13 satır düzeltildi:**
    - #95: simge çifti var; yalnız iki glif dolu görünüyor.
    - #135: "Sıradaki" satırı var.
    - #101: aralık ekranında çubuk var.
    - #97: Mevsimler'de iOS da emoji kullanıyor.
    - #98: kimi başlıklar zaten 26–28 sp.
    - #99: Araçlar satırlarında renkli simge var, kutu yok.
    - #129: sitede şehir sayfaları var.
    - #147: Ay widget'ı sıradaki dolunay/yeni ay tarihini gösteriyor.
    - #152: anahtar adları düzeltildi.
    - #174 ve #175: araç sayfalarında senkron satırı ve sekme başlığı var.
    - #90: tarayıcı iddiası daraltıldı.
    - #184: Dijital/Analog anahtarı "farklı, eksik değil".
- **Yeni hata:** #186 (sitedeki kıble pusulası iPhone'da manyetik kuzeye göre) eksik özellik değil, hata olarak işaretlendi.
- **Yanlış genel cümle:** "hazır süre düğmeleri araç sayfasında yok" yalnız alarm için doğruydu; düzeltildi.
- **Atlanan bulgular:** dört yeni iş eklendi (#189–#192). Bir bulgu #72'ye, biri #15'e, biri #181'e ek ayrıntı oldu.
- **Doğru çıkanlar:**
  - 1–192 numaraları eksiksiz ve tekrarsız; çapraz başvuruların hepsi doğru.
  - İlk analiz numaralarının 22'si de tutuyor.
  - 27 fiyat ve renk değeri ham notlarla aynı.
  - Sahibin görüntülerindeki 6 düzeltme görüntü açılıp ölçülerek doğrulandı.
- **Ücret sütunu:**
  - Sitede üyelik olmayacağı için #131 ve #183'ün site kısmı ücretsiz yapıldı.
  - #33'teki "—" düzeltildi.
  - #25 (bağış) tanıma eklendi.
- **Kalan uyarı:** "Bizde yok" ve "bugün yalnız X" ifadelerinin ~%20'si dar arama yüzünden abartılı ya da kısmen yanlış çıktı (düzeltildi). Bir iş yapılmaya seçilince o satır kodda bir kez daha aranmalı.

### Önerilmeyenler ve yapılamayanlar (numara almadı)
- **Veri kaynağı yok:**
  - Radar, uydu, yıldırım ve kamera görüntüsü.
  - ISS geçişleri: güncel yörünge verisi gerekir, sunucusuz yapmak zor.
  - Işık kirliliği, kutup ışığı ve gün batımı kalitesi tahmini.
- **Bizim ilkemize uymuyor:**
  - Astroloji: Ay burcu, Ay boşluğu, saç kesimi ve bahçe takvimleri, doğum haritası. Bunlar Deluxe Moon ve Daily Life'ta ücretli; bilimsel dayanakları yok.
  - Yapay zekâ sohbeti; SharePlay ile birlikte gözlem.
  - Kadran pazarı, tema ve simge paketi satışı.
- **Sunucu ya da hesap gerekir:**
  - Kendi anketinden özgün içerik (onlinealarmkur): kişisel veri de toplar.
  - time.is favorileri ve reklamsız aboneliği.
  - worldtimebuddy takvim bağlama.
  - everytimezone People/Meetings Pro.
  - online-stopwatch Premium.
- **Platform kısıtı:**
  - Başka uygulamada çalan müziğin kartı: bildirim erişimi ister, iOS'ta yapılamaz.
  - "Namazdayım" arama reddi: Play'de kısıtlı izin ister.
  - AR gökyüzü ve Güneş yolu: kamera ve kalibrasyon işi büyük; önce #156 ve #158.
- **Değmez:**
  - Doğal dille etkinlik ekleme: sistem düzenleyicisi yeterli.
  - Hareketli Wear OS kadranı.

### Ortak tasarım kalıpları (ikinci dalga)
- **Saat siteleri:**
  - İlk ekran hep aynı: çok büyük, saniyeli dijital saat. Altında tarih ve konum, sonra 5–12 popüler şehir. Bizde ilk ekranda rakamlı analog saat var (30 Ekim sahibin kararı).
  - Türk siteleri solda uzun kenar menü kullanıyor.
  - Uzun SEO metni ve SSS sayfanın içinde duruyor; biz bilerek gizliyoruz.
  - Senkron yöntemi açıkça anlatılıyor.
  - Hazır süre ve saat düğmeleri araç sayfasının içinde. Bizde geri sayımda 1–60 dk hapları var (`build.mjs:1005`); alarm sayfasında hazır saat düğmesi yok ve hazır alarm sayfalarına (`/alarm/07-00/`) araç sayfasından bağlantı verilmiyor.
  - Gizlilik vaadi aracın üstünde yazıyor ("listeniz bu tarayıcıda kalır").
- **Hava uygulamaları:**
  - Zemin koşula göre değişiyor.
  - Saatlik veri çizgi grafikle gösteriliyor, grafik türü çiplerle seçiliyor.
  - Günlük aralık çubuğu var (bizde de).
  - Ölçüm kartında sözlü yorum, ekranın üstünde uyarı hapı.
  - Her uygulamanın kendi simge takımı var.
- **Takvim uygulamaları:**
  - Büyük, kalın ay adı; yıl renkli.
  - Hafta şeridi aşağı çekilince ay ızgarasına açılıyor.
  - Görünümler hap sekmelerle seçiliyor.
  - Hücrelerde dolgulu etkinlik adları; gün başlığında hava.
- **Zaman araçları:**
  - İlerleme halkası neredeyse her yerde.
  - Rakamlar ince (ultralight) ve dev, saniye küçük üst simge.
  - Geri sayımda iki tarz var: fotoğraf önde (Countdown Star, Days) ya da renk önde (Countdown).
  - Dünya saatinde kişi adı ve gök renkli saat kutusu.
  - Liquid Glass cam widget'lar.
- **Ay ve Güneş uygulamaları:**
  - Fotoğraf gerçekliğinde Ay ve gezegenler.
  - Çentikli zaman cetveliyle saati ya da günü kaydırma.
  - Kalan süre ön planda ("Dolunaya 12 gün").
  - Renk kodu: altın saat sarı, mavi saat camgöbeği, gece lacivert.
  - 24 saatlik halka kadranlar; buzlu cam kartlar (köşe ~20 px).
- **Widget ve saat kadranı uygulamaları:**
  - Widget'lar duvar kâğıdı üstünde yarı saydam koyu ya da beyaz kart.
  - Galeri boyuta göre satırlara ayrılıyor; ücretsiz sürümde "demo" rozeti.
  - Kadran düzenleyicide renk daireleri; ayrı bir Ambient (ortam modu) sekmesi.
  - Saat uygulamalarında kenar boyunca ilerleme yayı ve büyük hap düğmeler (Google Saat: sarı, camgöbeği, somon).
- **Rakiplerdeki hatalar (bizde olmamalı):**
  - Ezan360 (Play): "saniye saniye saniye", "dogru"; Türkçe arayüzde yön harfleri İngilizce.
  - Ezan Vakti Pro (Play): Wear görüntüsünde vakitler yanlış saat diliminde (öğle 18:27).
  - Yazım hataları: "ARRAGEMENT" (MultiTimer), "Convertor" (Lasmit), "gateaway" (Days).
  - Countdown Star'da taşan "Time Zo…".
  - Focus Keeper görsellerinde Çince filigran kalıntısı.
  - Türk takvim uygulamalarının çoğunda yazım ve tarih hatası.

### Ücret modelleri (ikinci dalga)
- **Hava:**
  - Abonelik: haftalık ₺76,99'dan yıllık ₺1.499,99'a.
  - Aile planları: CARROT Premium Family ₺439,99, AccuWeather Family Plan ₺3.999,99.
  - Reklamsızlık ve radar ücretli.
- **Takvim:** Fantastical Premium (10 günlük hava, abone takvimleri, simgeler); Readdle'da doğal dille ekleme ₺99,99.
- **Zaman araçları:**
  - Tek seferlik satın alma ağırlıkta: Countdown ₺104,99, Seconds ₺399,99, Timeglass ₺149,99, Overdesigned ₺249,99, reklam kaldırma ₺99,99.
  - Abonelik pahalı: Lasmit ₺99,99/ay ile ₺4.999,99 ömür boyu arası; Focus Keeper ₺149,99/ay ile ₺4.999,99 arası.
  - Reklamsız ve ücretsiz rakip yok; Flora reklamsız ama ağaç paketi satıyor.
- **Ay, Güneş, gökyüzü:**
  - Temel işler ücretsiz. Ücretli olanlar astroloji, AR, harita araçları, bildirim özelleştirme ve kimi widget'lar.
  - Baştan ücretli: Sun Seeker ₺399,99, PhotoPills ₺599,99, Lumy ₺309,99. Lumy cihazda çalışıyor, sunucusuz ve reklamsız; ayrıca bahşiş alıyor.
- **Widget ve kadran:**
  - Paket satışı: Color Widgets ₺149,99–2.499,99, WidgetClub temaları ₺799,99–2.999,99, Clock Widget with Seconds Live ömür boyu ₺499,99.
  - Pujie tek seferlik; Clockology abonelik.
- **Siteler:** time.is reklamsız abonelik; online-stopwatch Premium $24/yıl; everytimezone Pro.
- **Bizim için:**
  - "İleride Pro adayı" yalnız görünüm ve uzman araçlarında işaretlendi: dünya kadranı (#131), dünya haritalı widget'lar (#130), meteogram (#114), Güneş yolu pusulası (#158), temalı tam ekran saatler (#183). Sitede hepsi ücretsiz (sitede üyelik olmayacak).
  - Rakiplerin para aldığı özelleştirilebilir Güneş/Ay bildirimleri (#152) ve hava widget'ları bizde ücretsiz kalır.
