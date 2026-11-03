# Ham notlar: kadran, masa saati, tam ekran saat, widget, StandBy (ikinci öncelik: zamanlayıcı, odak)

Ajan taraması, 3 Kasım 2026. Kaynak: App Store (TR mağaza, `itunes.apple.com/lookup?country=tr`), ekran görüntüleri 600 px (yataylar 1200 px) indirilip tek tek açıldı; uygulama içi satın almalar ve sürüm notları `apps.apple.com/tr/app/id…` sayfasından. Renkler görüntüden piksel örneğiyle (yaklaşık hex). Görüntüler depoya konmadı.

Bizim taraf için kanıt: tek analog kadran çizimi `mobile/ios/App/Sources/AnalogClock.swift` (rakamlı, 60 çizgi, üç ibre; renkler temadan: `Theme.surface2`, `Theme.ink`, `Theme.accent`, `Theme.accent2`), 20 model `mobile/ios/App/Sources/ClockModels.swift` (`enum ClockModel`; yalnız yerleşim farkı), yazı tipi `mobile/ios/App/Sources/Appearance.swift` (`ClockFont`: rounded, standard, serif, mono, light). Uygulamada saniye tık sesi yok (`grep -i tick` yalnız zamanlayıcı döngüleri), puanlama penceresi yok (`requestReview`/`ReviewManager` yok), StandBy'a özel görünüm yok (`widgetRenderingMode`/`showsWidgetContainerBackground` yok), ilk açılış tanıtımı yok.

## Analog Clock (id6785887940; sahibin "Analog Clock Pro" diye paylaştığı uygulama; puan yok (0), Draunt LLC)
- Ücret modeli: ücretsiz + reklamlı ("Contains Advertising"); tek uygulama içi satın alma: **Analog Clock Pro — ₺399,99** (tek seferlik; sahibin görüntüsünde üstü çizili ₺79.998 "50% Off" gösterimi). Deneme süresi görülmedi. Pro'da kilitli olanlar: sahibin görüntülerine göre kadranların çoğu ve her kadranın renkleri (taç simgesi). Yayın 7 Temmuz 2026, sürüm 1.2 (7 Ekim 2026), 5,2 MB, yalnız İngilizce listelenmiş (ama uygulama içinde Türkçe çeviri var, sahibin görüntülerinde).
- İncelenen görüntü: 7 iPhone + 10 iPad (hepsi açıldı; video yok).
- İlk açılış / tanıtım: mağaza görüntülerinde yok. (Sahibin görüntülerinde "Yenilikler" kaydırmalı tanıtım, 10 sayfa noktası; bkz. 2. analiz.)
- Ana ekran: mağaza görüntüleri yalnız kadranı gösterir; hiçbir düğme, yazı, çerçeve yok (tam ekran kadran, ortada, ekran genişliğinin ~%80'i).
- Kadran / saat görünümü (mağazada görülen 10 farklı kadran; hepsi 10:10:30 konumunda):
  1. **Neon mavi (i1)**: zemin #050B19 lacivert-siyah; kadran içi #0C1323; ince açık mavi dış halka ve dış ışıma (glow); mavi Arap rakamları (orta kalınlıkta, yuvarlak sans); mavi kalın çubuk akrep/yelkovan (yuvarlak uçlu); kırmızı-pembe ince saniye ibresi (#D04765); orta göbek pembe halkalı.
  2. **Okul/istasyon saati (i2)**: beyaz zemin, kalın siyah dış çerçeve (~%3), çok kalın siyah rakamlar (Arial Black benzeri), siyah kalın ibreler, kırmızı ince saniye (#981422 civarı), siyah göbek kırmızı halkalı.
  3. **Bauhaus / kum (i3)**: zemin #D5D1C5 bej, kadran #E3E1D5, gri metal görünümlü ince dış halka; rakam yok; saat başı çizgileri lacivert, 12-3-6-9 turuncu-kırmızı (#AC7264 civarı); lacivert ince uzun ibreler, turuncu saniye; ibrelerde hafif gölge.
  4. **Gece minimal (i4)**: zemin #05060B, kadran #070B14, çok ince gri halka; rakamsız, yalnız çizgiler; açık lila-beyaz ibreler, açık mavi saniye (#77BDDF), göbek açık mavi.
  5. **Demiryolu/çift halka (i5)**: zemin #DCDFE4 gri-mavi; kadran #E8E8E0; gri metal dış halka + iç iki ince halka; ince Arap rakamları; 12-3-6-9'da mavi işaret; mavi ince saniye.
  6. **Asker/saha (i6)**: zemin #0F140E koyu yeşil-siyah, kadran #1C211A; gri-yeşil yarı saydam çerçeve; açık yeşil (#B5DB78) saat çizgileri ve saniye; açık yeşil-beyaz ibreler (lüm etkisi).
  7. **Klasik kalın (i7)**: zemin #D6E0EA açık mavi; kadran #EAEFE9; gri metal çerçeve, kalın siyah rakamlar, mavi saniye, göbek mavi halkalı.
  8. **Kronograf (p5, iPad)**: koyu antrasit zemin, kadranda **üç alt kadran** (9, 3 ve 6'da), turuncu 12-3-6-9 işaretleri ve turuncu saniye; beyaz ibreler, beyaz rakamlar.
  9. **Roma rakamlı krem (p6)**: koyu mor-kahve zemin; krem/fildişi kadran, **serif Roma rakamları** (XII … I), kahve ibreler, bronz saniye.
  10. Beyaz/gri tonlu varyantlar (p4, p7, p9, p10: i3/i5/i7'nin iPad hâlleri).
  Açıklamada adı geçen stiller: "Classic, Roman, Railway, Modern, Bauhaus, Field, Sector, and more". Her kadranın kendi zemini ve renk eşleşmesi var (zemin + kadran + vurgu rengi takım). Hangi kadranın ücretli olduğu mağaza görüntülerinde işaretli değil.
- Ayarlar (açıklamadan; ekranda görülmedi): saniye ibresi aç/kapa, tarih ve **pil** gösterimi, 12/24 saat, "Bedside mode" (komodin), dünya saati, kronometre, zamanlayıcı, ana ekran ve kilit ekranı widget'ları, özel renk ve zemin.
- Widget / kilit ekranı / StandBy / saat: açıklamada ana ve kilit ekranı widget'ı; görüntüde yok. Apple Watch yok.
- Pro sayfası: mağazada görüntü yok (sahibin görüntüleri 2. analizde).
- Tasarım: her kadran tek başına, kenar boşluğu bol; ibrelerde yumuşak alt gölge (gerçek saat hissi); renk takımları düşük doygunluklu (bej, gri-mavi, koyu yeşil, lacivert); tek vurgu rengi saniyede ve 4 ana işarette.
- Hatalar: mağaza görüntülerinde yazı yok. Sahibin görüntülerindeki "Onarmak" (Restore) ve taşan "Derecele/ndirin" çeviri hataları bu uygulamada.
- Bizde olmayan / farklı:
  - Kadran tasarım çeşitleri (Roma rakamı, rakamsız Bauhaus, kronograf alt kadranları, demiryolu çift halka, neon ışıma, kalın okul saati) — **bizde yok** (tek çizim; renk yalnız temadan) — `mobile/ios/App/Sources/AnalogClock.swift` — B — rakipte çoğu Pro (₺399,99 tek sefer) — mevcut 2. analiz #1'e ek ayrıntı: kadranlar **takım renk** olarak gelir (zemin + kadran + vurgu); Roma rakamlarında serif yazı tipi.
  - İbre gölgesi, dış ışıma (glow), metal çerçeve halkası gibi kadran malzemesi efektleri — bizde yok (düz `Capsule`) — aynı dosya — K/O — rakipte bazı kadranlar ücretli.
  - Kadranda pil göstergesi — kısmen (saat ekranı alanı olarak `HomeItem.battery` var, kadranın içinde değil) — `mobile/ios/App/Sources/HomeItems.swift` — K — ücretsiz görünüyor.

## Desk Clock - Analog Clock Face (id1253066126; 4,40; 4.462 puan; geliştirici 翠玲 施 — Flip Clock: Digital Desk Widget ile aynı satıcı)
- Ücret modeli: ücretsiz + reklamlı. Uygulama içi: **Professional Version — ₺199,99**, **Remove All Ads — ₺49,99**, **All clock styles — ₺49,99** (hepsi tek seferlik görünüyor; abonelik yok). Deneme görülmedi. Pro'da kilitli: kadranların bir kısmı (galeride sağ üst köşede sarı çapraz **"VIP"** şeridi).
- İncelenen görüntü: 8 iPhone + 6 iPad (iPad 1–3 açıldı; 4–6 iPhone'larla aynı türde).
- Sürüm notları (son 5): "transparent glass effect" uyumu (4.18, 26 Eki), "second-hand scanning performance" (4.17), "**3D porcelain koi watch face**" (4.16), "Added new classic clock faces. **Added Apple Watch face support**" (4.15).
- İlk açılış / tanıtım: görülmedi.
- Ana ekran (i2): zemin düz koyu gri #373737; kadran ortada ekran genişliğinin ~%90'ı. Köşelerde dört küçük ince çizgili simge: sol üst **tişört** (kadran/"skin" değiştir), sağ üst daire içinde "…" (menü), sol alt zil + "OFF" (alarm durumu), sağ alt **kaydırıcılar** (ayarlar). Sağ üstte gri "Wed, Sep 2" ve altında beyaz küçük dijital "10:09" (kadranın yanında dijital saat ve tarih, sağa yaslı, ince SF yazı).
- Kadran / saat görünümü (görülen 13):
  1. **3D porselen koi (i1)**: siyah kalın dış çerçeve, beyaz porselen kâse halka (parlak, gölgeli 3B), halkada kırmızı-beyaz koi balıkları; siyah iç kadran, beyaz dakika izi; ince gri ibreler, açık altın saniye. (Sürüm 4.16'da "optimize edildi"; muhtemelen VIP.)
  2. **İsviçre demiryolu (i2)**: açık gri #D4D4D4 kadran, 60 kalın siyah çubuk işaret (saatlerde daha kalın), siyah çubuk ibreler, **ucunda kırmızı disk olan kırmızı saniye ibresi** (Mondaine tarzı), "Swiss" el yazısı logo.
  3. **Balerin (i5)**: siyah kadran, gri metal çerçeve, 12 beyaz çizgi; **ibreler yerine balerin silueti** (kollar/bacak ibre görevinde).
  4. **Klasik koyu (i7)**: #161616 zemin, #222222 kadran (çerçevesiz), çok kalın beyaz rakamlar, noktalı dakika izi, beyaz ibreler, kırmızı saniye ve kırmızı göbek.
  5. **Minimal "Desk Clock" (iPad p2)**: siyah, rakamsız kalın beyaz çubuk işaretler, serif italik "Desk Clock" yazısı.
  6. Galeri (i4 "Change skin") "Analog Clock" bölümünde ayrıca: "OL Clock" siyah nokta işaretli, rakamlı OL Clock, **altın çerçeveli Roma rakamlı lacivert kadran**, **ahşap dokulu** kadran (beyaz çubuklar).
  7. "Digital Clock" bölümü: **yeşil LED** (16:40 büyük + :47 saniye küçük, üstte "WED JUL 27 2022"), **camgöbeği LED**, **mavi LED (VIP şeritli)**, **ahşap zemin üstünde beyaz rakamlar**; her biri gri metal çerçeveli karo.
  8. **Dikey dijital (i6)**: siyah zemin, neon yeşil #1DF06D, şablon/stencil karakterli kalın dar yazı; saat, dakika, saniye **alt alta** üç satır, aralarında iki kare nokta; üstte "TUE SEP 6 2022".
- Ayarlar: görülmedi (yalnız alarm ekranı). Alarm (i8): "Enable Alarm" anahtarı, "Sounds › Classic", sistem tekerlek seçici (saat, dakika, AM/PM); siyah zemin, #1C1C1E satırlar (iOS gruplu liste).
- Widget / kilit ekranı / StandBy / saat (i3, p1): **Dynamic Island'da** solda takvim simgesi ("27"), sağda "58:18" (canlı dakika:saniye). Ana ekranda: orta boy **analog + aylık takvim** widget'ı (siyah, bugün kırmızı daire), dört küçük analog widget (gri kadran kırmızı saniye, İsviçre beyaz, siyah rakamlı, kırmızı halkalı minimal), orta boy **saniyeli çevirmeli dijital widget** ("15:05:56", açık mavi-gri kart, siyah kart rakamlar, "Thu, Oct 27"). Açıklama: saniye ana ekran, kilit ekranı, Dynamic Island ve StandBy'da; Apple Watch kadranı eşitleme ("Watch Face Sync"), saat başı zil ve sesli hatırlatma, "Smart Volume-Adjusting Alarm".
- Pro sayfası: görülmedi.
- Tasarım: koyu gri düz zeminler (#373737–#424242), simgeler ince çizgi (SF Symbols değil, kendi çizimi), köşe yazıları küçük; galeri koyu, karolar gri metal çerçeveli 3 sütun yatay kaydırmalı. Görüntüler eski (2017/2022 tarihli: "7/9/17", "Carrier", "2022").
- Hatalar: görüntülerdeki tarihler eski ("Sep 6 2022"); iPad görüntüsünde "Carrier" durum çubuğu (simülatör artığı).
- Bizde olmayan / farklı:
  - Kadran galerisi ("Change skin": analog + dijital bölümleri, küçük resimli karolar, VIP şeridi) — bizde yok (20 model listesi yerleşim seçer; `ClockModels.swift`) — B — rakipte "All clock styles" ₺49,99 / Pro ₺199,99 — mevcut 2. analiz #1'e ek: **dijital kadranlar da** koleksiyonda (LED renkleri, ahşap zemin, dikey üç satır).
  - Figürlü ibreler (balerin silueti) ve 3B malzemeli kadranlar (porselen, ahşap doku, altın çerçeve) — bizde yok — `AnalogClock.swift` — B (görsel üretimi) — VIP.
  - Dikey (üç satır, saat/dakika/saniye alt alta) dijital saat — bizde yok (dijital modeller tek satır; `ClockModels.swift` `DigitalPart`) — K — muhtemelen ücretsiz.
  - Analog saatin yanında küçük dijital saat + tarih (köşede) — kısmen (bizim `stack`/`analog-date` modelleri altta gösterir) — `ClockModels.swift` — K — ücretsiz.
  - **Dynamic Island'da canlı saat/saniye** (sayaç değil, saatin kendisi) — bizde yok (Live Activity yalnız zamanlayıcı/pomodoro/kronometre/vakit: `LiveActivityModel.swift`) — K/O (Apple kuralı: süresiz Live Activity 8 sa sonra biter) — rakipte ücretsiz görünüyor.
  - **Saniyeli analog widget** ve **saniyeli çevirmeli dijital widget** — kısmen (iOS'ta saniyeli widget yalnız düz dijital metin `SecondsClockWidget`; analog widget saniyesiz; Android 12+ analog widget'ta saniye var) — `mobile/ios/Widget/Sources/MoreWidgets.swift`, `ClockWidgets.swift` — O (iOS'ta saniye ibresi ancak `Text(timerInterval:)` hilesi ya da yazı tipi hilesiyle) — ücretsiz görünüyor.
  - Analog + aylık takvim birleşik widget — bizde yok (ayrı ayrı yok; takvim widget'ı hiç yok) — `mobile/ios/Widget/Sources/` — K — ücretsiz.
  - Apple Watch kadranı eşitleme (uygulamada seçilen kadranı saate göndermek) — bizde yok (Apple Watch uygulamamızda tek görünüm; watchOS üçüncü taraf kadrana izin vermez, rakip muhtemelen fotoğraf kadranı/komplikasyon yoluyla) — `mobile/ios/Watch` — B — belirsiz.
  - Ana ekranda köşe simgeleri (tişört = kadran değiştir, zil OFF = alarm durumu tek bakışta) — kısmen (bizde denetim satırı ve dişli var) — `ClockModels.swift`/`HomeView.swift` — K — ücretsiz.

## Flip Clock: Digital Desk Widget (id1181028777; 4,47; 7.888 puan; 翠玲 施 — Desk Clock ile aynı satıcı)
- Ücret modeli: ücretsiz + reklamlı. Uygulama içi (tek seferlik): **Flip Clock Professional Edition — ₺149,99**, **Remove Ads — ₺49,99**, **More clock skins — ₺49,99**, **Digital Clock 2…6 — her biri ₺49,99** (tek tek satılan kadranlar), **Simulated Desk Fan 1 — ₺49,99** (masa vantilatörü canlandırması), **Unlock All Screensavers — ₺499,99**. Aynı satıcının ayrı ücretli sürümü **Flip Clock Pro - time widgets (id1181029289) ₺149,99**. Deneme görülmedi. Kilitli: galeride sarı köşe "VIP" şeritli kadranlar.
- İncelenen görüntü: 6 iPhone (yatay, 1200 px) + 7 iPad (4'ü açıldı).
- Sürüm notları: "background music playback is not interrupted when a timer ends" (7.2, 21 Eki), "**Added OLED burn-in protection**" (7.1, 12 Ağu), Pro yükseltme hatası düzeltmesi (6.20).
- İlk açılış / tanıtım: görülmedi.
- Ana ekran: tam ekran çevirmeli saat. Yatayda üç kart (saat, dakika, saniye), dikeyde **iki kart alt alta** (saat üstte, dakika altta; saniye kartın sağ alt köşesinde küçük "11"). Zemin #000000, kart ~#11100C–#1C1C1C (köşe yarıçapı kart yüksekliğinin ~%10'u), rakamlar açık gri #BABABA, çok kalın dar grotesk (DIN Condensed benzeri). **Her kartta iki rakam** (tek kart = bir grup), kartın ortasında yatay siyah ayrım çizgisi. 12 saat biçiminde kartın sol altında küçük kalın "PM". Kartların üstünde "Jun 6, 2021 Sun" tarih satırı, altında gri küçük **kişisel metin "Less is more"** (imza/motto).
- Kadran / saat görünümü ("Multiple skin themes" görüntüsü, 9 tema): gri klasik (saniye kartın köşesinde küçük), **sarı rakam**, **beyaz kart + siyah rakam (açık tema)**, **yeşil**, gri 3 kartlı küçük + tarih + motto, **pembe/kırmızı**, **mor**, **turkuaz**, **turuncu** (son üçü 3 kartlı, tarih ve motto aynı renkte). iPad galerisi: "Classic" (seçili, sağ alt köşede mavi üçgen + onay), "Popular" (sarı, kırmızı [VIP], beyaz kart, yeşil [VIP], mor…); karolar gri metal çerçeveli, kenarda yatay kaydırma.
- Ayarlar: görülmedi. Pomodoro ekranında sağ üstte kaydırıcı simgesi.
- Pomodoro: yatay iki dev kart "25 00", altında yeşil (#1BAE55 civarı) yuvarlak köşeli dikdörtgen **"Start Focus"** düğmesi, üstte "Focus" başlığı ve geri oku.
- Alarm çalma ekranı: koyu lacivert gradyan (#0F2940 → siyah), "Alarm", "12:43", beyaz zil simgesi, **yeşil hap "Snooze"** (büyük) ve **kırmızı-pembe hap "Stop"** (küçük). Başlık "Natural ringtones / Wake you up".
- Widget / kilit ekranı / StandBy / saat: ana ekranda orta boy **çevirmeli saat + aylık takvim** (açık zemin, beyaz kart siyah rakam, küçük saniye "38"), küçük açık ve küçük koyu (3 kartlı saniyeli "18 05 38"), büyük koyu **"Stay hungry, Stay foolish" motto satırlı** widget; üstte **yüzen kronometre/saat penceresi** (PiP: "17:58:23.9", kapat × ve pencere simgesi). Kilit ekranı yığınında (Smart Stack) turkuaz rakamlı widget'lar, "FLIPCLOCKPRO" başlıklı saniyeli "16 07 30". iPad kilit ekranında beyaz/koyu/sarı/turkuaz küçük widget'lar. Açıklama: saniye ana ekran, kilit, **Dynamic Island** ve StandBy'da; **"Smart Auto-Launch: Automatically turns into a screensaver when charging"**; **OLED piksel kaydırma**; kişisel imza widget'larda; sayaç, pomodoro, **yüzen kronometre**, olay geri sayımı, **beyaz gürültü**, **çevrimiçi müzik zil sesi kitaplığı**, kademeli alarm, saat başı sesli zil.
- Pro sayfası: görülmedi.
- Tasarım: tam siyah zemin, koyu kartlar, tek vurgu rengi (rakam rengi tema), yazılar SF; tanıtım görüntüleri gri cihaz kasası içinde, altında siyah büyük başlık ("Pomodoro, focus on learning"). Eski görüntüler (iPhone 8 kasası, "No SIM", 2021 tarihleri).
- Hatalar: görüntüler 2021'den kalma; "FLIPCLOCKPRO" ayrı uygulamanın adı bu uygulamanın görüntüsünde.
- Bizde olmayan / farklı:
  - Çevirmeli saatte **iki rakam bir kart** düzeni, **dikeyde kartlar alt alta**, saniyenin kartın köşesinde küçük gösterimi, kartta "PM" etiketi — kısmen (bizde her rakam ayrı kart, tek satır, saniye ayrı kart; `FlipPart` `mobile/ios/App/Sources/ClockModels.swift`) — K — ücretsiz.
  - Çevirmeli saatte **rakam/kart renk temaları** (sarı, yeşil, pembe, mor, turkuaz, turuncu, beyaz kart) — kısmen (bizde renk uygulama temasından, kart rengi ayrı seçilmez) — `ClockModels.swift` `FlipCard`, `Theme.swift` — K — bir kısmı VIP (₺49,99 kadran başına).
  - Kişisel metin/motto tam ekran saatin altında — **var** (`fs.text`, `Appearance.swift`), ama **widget'ta yok** — `mobile/ios/Widget/Sources/` — K — rakipte ücretsiz.
  - Çevirmeli + takvim widget'ı, çevirmeli saniyeli widget, motto satırlı widget — bizde yok (widget'larımız düz metin) — `ClockWidgets.swift`, `MoreWidgets.swift` — O — ücretsiz görünüyor.
  - "Şarja takınca ekran koruyucuya dön" — kısmen (iOS'ta Kısayollar otomasyonu `OpenFullscreenClockIntent`, Android `ClockDreamService`; uygulama içinde otomatik değil, iOS'ta mümkün değil) — `AppIntents.swift` — —.
  - OLED piksel kaydırma — **var** ama yalnız gece modunda (`NightMode.shift`, `NightMode.swift`); normal tam ekranda yok — K — rakipte ücretsiz.
  - Tek tek satılan kadranlar, "masa vantilatörü" gibi eğlence ekran koruyucuları (Unlock All Screensavers ₺499,99) — bizde yok — B — ücretli; bizde ileride Pro adayı olabilir.
  - Çevrimiçi müzik zil sesi kitaplığı — bizde yok (2. analiz #3 ile aynı iş; ek ayrıntı: "Natural ringtones" kategorisi) — O.
  - Alarm çalma ekranında büyük yeşil "Snooze" + küçük kırmızı "Stop" — kısmen (Android `AlarmActivity`; iOS'ta sistem ekranı) — K.

## Zen Flip Clock - Minimal Timer (id1265404088; TR 4,53, 754 puan; ABD'de 4,66, 6.195; Chaoyang Minimal Software Studio)
- Ücret modeli: **reklamsız** ("Contains Advertising" yok), abonelik: **Monthly Pro ₺49,99**, **Yearly Pro ₺229,99**, **Lifetime Pro ₺449,99**. Deneme süresi görülmedi. Pro'da kilitlenen temalar mağazada işaretli değil. Sürüm notu 4.14.3: "**Improved the Onboarding and Paywall experience**" (tanıtım + ödeme duvarı var, görüntüsü yok).
- İncelenen görüntü: 6 iPhone (yatay, 1200 px) + 5 iPad (4'ü açıldı).
- İlk açılış / tanıtım: sürüm notunda var, görüntü yok.
- Ana ekran / denetim: açıklamaya göre **kaydırma hareketleri**: sola kaydır = Pomodoro, sağa kaydır = ileri sayan sayaç, dokun = Ayarlar. Ekranda düğme yok (yalnız sayaç ekranlarında sağ kenarda dikey üç küçük yuvarlak düğme: durdur ■, sıfırla ↺, duraklat ‖).
- Kadran / saat görünümü (görülen 8 tema; tanıtım başlıkları "Minimalism", "Aesthetic", "Digital Wall", "Digital Style", "Retro Style"):
  1. **Minimalism / silindir (i1)**: açık gri zemin, arka planda bulanık büyük rakamlar dairesel dizili (derinlik), ortada beyaz **hap biçimli çerçeve** içinde "09:41:05", her rakam ayrı **dönen silindir** (kilit/kasa şifresi tekerleği) görünümünde, rakamlar koyu gri dar.
  2. **Aesthetic / 3B yedi bölüt (i2)**: açık gri #D? zemin, beyaz **kabartma (neumorphic) yedi bölütlü** dev rakamlar "09:41", sönük bölütler de hafifçe görünür, yumuşak gölge.
  3. **Digital Wall (i3, iki varyant)**: koyu ve açık (bej #D?): solda **00–59 sayı matrisi** (geçerli saniye/dakika vurgulu: turuncu ve beyaz), sağda **nokta matris (dot-matrix) yazı** "8:30:13 AM" (saat hanesi turuncu), altında "2026/03/31 TUE", sağ üstte ay/güneş (tema) düğmesi, sağ altta hoparlör ızgarası gibi nokta deseni ("Time glows in the matrix"). Pomodoro varyantında "00:25:00 POMODORO READY" ve alt düğmeler.
  4. **Digital Style / LCD (i4)**: eski **LCD ekran** taklidi: açık mavi ya da **fosforlu yeşil** zemin, koyu yedi bölüt rakamlar, sönük bölütler görünür, üstte "AM" ve "MON/0905", lacivert-gri plastik çerçeve, köşelerde ▮▮▮▮ işaretleri.
  5. **Retro Style / ahşap (i4)**: **koyu ahşap** (ya da açık meşe) kasa içinde beyaz plastik kartlı **mekanik çevirmeli** saat; kartlarda "AM", "MON", kart menteşe çubukları görünür; koyu kart varyantı da var (iPad).
  6. **Pomodoro koyu çevirmeli (i5)**: siyah zemin, iki koyu gri kart "30 00", üstte küçük "POMODORO".
  7. **Kronometre açık çevirmeli (i5)**: #F2F2F2 zemin, beyaz kartlar, siyah rakam "00 05", üstte "STOPWATCH".
- Ayarlar: görülmedi. Açıklama: saat başı zil ve özelleştirilebilir hatırlatmalar, **odak istatistikleri**.
- Widget / kilit ekranı / StandBy / saat (i6): ana ekran widget'ları temaların aynısı: beyaz kartlı çevirmeli (büyük), küçük beyaz ve siyah kartlı "9 41" + "09 / 04 / SUN" (gün adı kırmızı), **ahşap kasalı çevirmeli** (büyük ve iki küçük: koyu meşe, açık meşe), **LCD** widget'lar (fosforlu yeşil büyük; açık mavi ve gri küçük). Kilit ekranında dairesel widget'lar: çevirmeli "00", pomodoro kadranı "25", kronometre simgesi, **hayvan simgeleri** (köpek, sıçan, öküz, at: Çin burç saatleri olabilir; açıklamada yok). Sürüm notları: "extra-large portrait widgets for iOS 27", "Ready for iPhone Duo".
- Pro sayfası: görülmedi.
- Tasarım: tanıtım görüntülerinde açık mavi-lila gradyan zemin, ince gri çizgili ayrık başlıklar; uygulama içi temalar ya tam siyah ya #F2F2F2; yazılar SF; **malzeme taklidi** (ahşap, plastik LCD, kabartma) belirgin.
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - **Malzeme/dönem temalı dijital saatler**: LCD (sönük bölütlerle), 3B kabartma yedi bölüt, nokta matris, silindir tekerlek, ahşap kasalı mekanik çevirmeli — bizde yok (dijital modellerimiz yazı tipi ile çizilen düz metin; `ClockModels.swift` `DigitalPart`, `FlipPart`) — O (her biri K–O) — rakipte Pro (₺49,99/ay, ₺449,99 ömür boyu) — mevcut 2. analiz #1'e ek: **dijital kadran koleksiyonu** da istenir.
  - Kaydırma hareketleriyle araç değiştirme (sola pomodoro, sağa sayaç) — bizde yok (bizde yatay kaydırma model değiştirir) — K — ücretsiz.
  - Saniye/dakika matrisi (00–59 ızgara, geçerli değer vurgulu) — bizde yok — K — muhtemelen Pro.
  - Tam ekran pomodoro/kronometreyi çevirmeli kart olarak göstermek — kısmen (araçlarımızda tam ekran var, çevirmeli değil; `mobile/ios/App/Sources/ToolsTab.swift`) — K — ücretsiz.
  - Temalı widget'lar (ahşap, LCD, beyaz kart) — bizde yok (widget'larımız tek görünüm) — O — muhtemelen Pro.
  - Kilit ekranında dairesel araç widget'ları (pomodoro, kronometre başlat) — kısmen (bizde iOS'ta zikirmatik ve kronometre etkileşimli widget'ları var, kilit ekranında dairesel araç widget'ı yok; `ToolWidgets.swift`) — K.

## Fliqlo (id900833042; TR 3,80, 86 puan; ABD 4,75, 2.344; Yuji Adachi)
- Ücret modeli: **ücretli uygulama ₺49,99** (ABD 0,99 $), reklamsız; buna ek abonelik listelenmiş: **Monthly Plan ₺49,99**, **Yearly Plan ₺499,99** (ne açtığı mağazada yazmıyor; görüntülerde Pro işareti yok). TR puanının düşüklüğü (3,8) ücretli + abonelik birleşiminden olabilir (doğrulanmadı).
- İncelenen görüntü: 5 iPhone (yatay, 1200 px) + 5 iPad (iPhone'larla aynı türde, açılmadı).
- Sürüm notları: "Added Sleep Timer" (2.3), iPhone Duo düzeni (2.4), Liquid Glass uyumu.
- İlk açılış / tanıtım: görülmedi.
- Ana ekran: 2002'den beri değişmeyen tek tasarım (macOS ekran koruyucusunun aynısı). Zemin #000000, kartlar ~#141414 (köşe yarıçapı ~%8), rakamlar açık gri #BEBEBE, **Helvetica Neue Condensed Bold** benzeri dar kalın; kart ortasında siyah ayrım çizgisi; 12 saatte sol altta "PM"/"AM". Dikeyde iki kart alt alta, yatayda yan yana. Hiç düğme yok; **ekrana dokununca ayar düğmesi belirir** (ayarda: saniye açıkken ayar düğmesinin yerini saniye alır).
- Kadran / saat görünümü: tek kadran; ayarlarla "flaps" (kart kapakları) gizlenebilir, boyut küçültülebilir, karartılabilir.
- Ayarlar (i5): koyu #1C1C1E zemin, ortada "Settings" başlığı, sağ üstte daire içinde × kapatma; gruplar: **Timer › Sleep Timer (Off)**; **Time Display › Time Format "1-12AM/PM"**, **Show Seconds** anahtarı ve altında açıklama ("The seconds display replaces the icon of the settings button that appears when you tap the screen."); **Appearance › Screen Orientation "Automatic"**, **Show Flaps** anahtarı; (devamı açıklamaya göre: Dimmable, Display size). Satırlar hap/yuvarlak köşeli koyu gri gruplar, solda ince çizgi SF simge.
- Widget / kilit ekranı / StandBy / saat: yok (açıklama: "cannot replace your device's Lock Screen or StandBy mode", "does not start automatically").
- Pro sayfası: görülmedi.
- Tasarım: tek renk mat siyah; tanıtım görüntülerinden biri **gerçek masa fotoğrafı** (kahve, defter, kalem yanında telefon) — "iç mekân dekoru" konumlandırması; iPad + iPhone + Mac (ekran koruyucu) birlikte.
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - "Show Flaps" (kart kapaklarını gizleme: yalnız rakamlar), ekran boyutunu küçültme — kısmen (bizde `fs.scale` saat boyutu var; kapak gizleme yok) — `Appearance.swift`, `ClockModels.swift` `FlipCard` — K — rakipte ücretli uygulamanın içinde.
  - Dokununca beliren ayar düğmesi, saniye açıkken düğmenin yerine saniye — kısmen (bizde tam ekranda düğmeler dokununca belirir) — K.
  - Uyku zamanlayıcısı (otomatik kilidi belli süre sonra geri açar) — **var** (`SleepTimer`, `NightMode.swift`) — —.
  - Dikey/yatay yön seçimi — **var** (`fs.lock`) — —.
  - Mağaza görüntüsünde gerçek ortam fotoğrafı (masada telefon) — bizde yok (mağaza görüntülerimiz simülatör: `mobile/ios/scripts/store-screenshots.sh`) — K — —.

## ClockZ | Desk Clock & Alarm (id414587395; TR 4,45, 333 puan; ABD 4,51, 5.581; Makoto Katsumata)
- Ücret modeli: ücretsiz + reklamlı; abonelik **ClockZ Premium ₺69,99/ay** ve **₺699,99** (yıllık olmalı); **Theme ₺149,99**, **Tower Backgrounds ₺99,99**, **Remove Ads ₺249,99**; ayrıca **bahşiş kavanozu**: "Good App ₺49,99", "Best App ₺149,99", "Thank You ₺249,99", "Regular Support ₺249,99", "Never Give Up ₺499,99". Açıklama: Premium = "All text colors and themes, **3D map backgrounds**, **Battery display**, **Outline display**"; temel özellikler ücretsiz.
- İncelenen görüntü: 5 iPhone + 5 iPad (iPhone'lar açıldı).
- İlk açılış / tanıtım: görülmedi. Uygulama simgesi maskotu: kulaklı (sincap/kedi) çalar saat karakteri.
- Ana ekran (i1): siyah zemin, ortada **neon yeşil #00FF00 benzeri** büyük "10:11:30" (Helvetica Bold), üstte aynı renkte "Tuesday, February 4, 2025", altta küçük "ClockZ" yazısı. Altta beş yuvarlak gri cam düğme: alarm, kum saati (zamanlayıcı), kronometre, **yüz/renk** (görünüm), **+** (ekle).
- Kadran / saat görünümü: tek dijital düzen; yazı rengi palet ızgarası (i4 "Colorful.": koyu panel, 3 sütun büyük renk daireleri: kırmızı, turuncu, mercan, somon, krem, sarı, hardal, açık yeşil…; üstte "Done" ve mor-lacivert gradyan başlık şeridi), zemin rengi düz (bej #D?B?9? örneği), **fotoğraf zemin** (i2: dağ, şehir kıyısı, kanyon; saat beyaz, kanyon örneğinde **serif yazı tipi**), "3D map backgrounds" (Premium; görüntüsü yok), "Outline display" (dış hatlı rakam, Premium).
- Ayarlar: görülmedi. Alarm (i3): koyu yarı saydam panel "Done / ⏰ 07:30:00 / Save", tekerlek seçici, şarkı adı "Fight The Power", **"Pick a song"** (müzik arşivinden şarkı), kırmızı "Delete"; kaydedince üstte "Alarm Time | 07:30:00" bildirim kartı.
- Widget / kilit ekranı / StandBy / saat (i5): tek görüntü: beyaz yuvarlak köşeli widget'ta kalın siyah "14:55".
- Pro sayfası: görülmedi.
- Tasarım: tanıtım görüntüleri **parlak camgöbeği #00B?F? zemin**, beyaz kalın eğik başlıklar ("The Clock App! Anytime, Anywhere.", "Photos as backgrounds.", "Alarm, Timer, Stopwatch.", "Colorful.", "Widgets."), arka planda beyaz eşmerkezli halkalar; telefonlar eğik yerleştirilmiş.
- Hatalar: sürüm notları üç sürümdür aynı genel teşekkür metni.
- Bizde olmayan / farklı:
  - **Bahşiş (tip jar) uygulama içi satın almaları** (özellik açmayan, yalnız destek) — bizde yok — K (StoreKit) — ₺49,99–499,99 — bizim "ücretsiz, reklamsız" modelimize en uygun gelir yolu; sahibin kararı.
  - Saat rakamı rengi için geniş palet (yazı rengi ayrı seçimi) — kısmen (24 tema var, yalnız saat yazı rengini ayrı seçme yok; `Theme.swift` `AccentPalette`) — K — rakipte "All text colors" Premium.
  - Dış hatlı (outline) rakam görünümü — bizde yok — K — Premium.
  - Fotoğraf zemin — **var** (`fsPhoto`, slayt `fsSlides`; `Appearance.swift`) — —.
  - 3B harita zemini — bizde yok (küre var ama tam ekran saat zemini değil; `GlobeView.swift`) — O — Premium.
  - Alarm için müzik arşivinden şarkı — **var** (`AlarmMusic.swift`) — —.

## Widgetsmith (id1523682319; TR 4,53, 18.755 puan; Cross Forward Consulting, LLC — David Smith)
- Ücret modeli: ücretsiz + reklamlı; abonelik **Widgetsmith Premium**: ₺14,99, ₺144,99, ₺179,99, ₺1.299,99 (dört kademe; süreleri sayfada yazmıyor; en yükseği ömür boyu olabilir, doğrulanmadı). Premium'da neler kilitli mağaza görüntüsünde işaretli değil.
- İncelenen görüntü: 5 iPhone + 5 iPad (iPhone'lar açıldı).
- Sürüm 9 notu (2 Kas 2026): yeni tasarım, **kaydırmalı "Gallery"** (hazır widget tasarımları; olduğu gibi kullan ya da düzenle), daha görsel düzenleyici, **11 yeni tema rengi**, iPhone Duo tam ekran görünümleri.
- İlk açılış / tanıtım: görülmedi.
- Ana ekran: uygulama ekranı görüntüde yok; görüntüler ana ekran sonuçlarını gösterir.
- Kadran / saat görünümü (widget olarak): **pembe tema** (#F?A?A? zemin, mürdüm #9?3?6? kare) içinde ince noktalı dakika izi + ince rakamlı analog saat; aynı renkte 5 günlük hava listesi (serif gün adları "FRI SAT…").
- Widget türleri (görülen): analog saat, 5 günlük hava (liste), **büyük tarih ("FRI 18") + polaroid çerçeveli fotoğraf**, **polen göstergesi yay çubukları** (Trees/Grasses/Weeds: "None", "Very Low"; monospace yazı), bugünün havası (şehir, 59°, en düşük/en yüksek), fotoğraf widget'ları (köşesinde hava "☀ 59°", **çerçeve stilleri**: düz, gri metal çerçeve), **haftalık takvim + etkinlik listesi** (renkli dikey çizgili etkinlikler: "Lunch with John 12-2pm"), **fotoğraflı geri sayım "SPRING BREAK 35 DAYS"** (serif büyük harf), **gelgit eğrisi** ("New York City Receding", mavi dalga, gelgit saatleri), müzik albüm kapağı, havası fotoğraf zeminli widget (altında üç sekmeli küçük düğme: hava/saat/takvim). Açıklama: "Solar & Lunar Trackers", "Time Zone Tracker", adım sayar, **Actions** (widget'tan uygulama/Kısayol başlatma, **Denetim Merkezi**), kilit ekranında şarkı sözü/fotoğraf/hava; **duvar kâğıdı kitaplığı** ve duvar kâğıdına uygun **widget teması** üretici.
- Pro sayfası: görülmedi.
- Tasarım: her tanıtım görüntüsü farklı pastel zemin (pembe #F?D?D?, şeftali, açık sarı, açık yeşil, açık mavi), üstte renkli tek simge (telefon, parıltı, fotoğraf, palet, kalp), gri ince "customize your" + siyah kalın "Home Screen" iki satırlı başlık; telefon beyaz kalın çerçeveli çizim. Sosyal kanıt: **"over 150,000,000 users"**.
- Hatalar: görülmedi.
- Bizde olmayan / farklı (bizim iOS widget listesi: `mobile/ios/Widget/Sources/SaatTarihWidgets.swift` — sıradaki vakit, bugünün vakitleri, analog, dijital, saat+tarih, Ay, saniyeli, dünya, geri sayım, hicri, zikirmatik, kronometre):
  - **Widget görünüm düzenleyicisi** (renk, yazı tipi, çerçeve, tema; hazır tasarım galerisi) — bizde yok (widget'lar uygulamanın varsayılan temasında; App Group yok: CLAUDE.md "Widget'lar varsayılan temada kalır") — B (App Group + yapılandırma) — Premium.
  - **Kilit ekranı dairesel widget'ı (`accessoryCircular`)** — bizde **hiç yok** (yalnız `accessoryInline` ve `accessoryRectangular`; `grep supportedFamilies`) — K — ücretsiz (Zen Flip, Widgetsmith).
  - Hava widget'ı (bugün, 5 gün) — bizde yok (hava yalnız uygulamada ve saat modellerinde; WeatherKit) — K/O — ücretsiz.
  - Takvim/etkinlik widget'ı (haftalık + telefon etkinlikleri) — bizde yok (takvim widget'ı yok) — O — ücretsiz.
  - Fotoğraf widget'ı, fotoğraflı geri sayım widget'ı — kısmen (geri sayımlarımızda fotoğraf uygulama içinde var, widget'ta yok; `Countdowns.swift`, `MoreWidgets.swift` `CountdownWidget`) — K — ücretsiz.
  - Güneş/Ay izleyici widget'ı — kısmen (Ay evresi widget'ı var `MoonWidget.swift`; gün doğumu/batımı widget'ı yok) — K.
  - Widget'tan Kısayol/uygulama başlatma, Denetim Merkezi düğmeleri — kısmen (Denetim Merkezi düğmelerimiz var: tam ekran saat, zamanlayıcı, vakitler; CLAUDE.md #77) — —.
  - Duvar kâğıdı kitaplığı — bizde yok (2. analiz #2 ile benzer, ama burada ana ekran duvar kâğıdı) — O.
