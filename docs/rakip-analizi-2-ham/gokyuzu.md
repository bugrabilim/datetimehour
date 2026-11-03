# Ay, Güneş ve gökyüzü (ikinci dalga, 3 Kasım 2026)

Yöntem: App Store TR ve US aramaları (`itunes.apple.com/search`), ayrıntı `itunes.apple.com/lookup`; ekran görüntüleri 600 px genişlikte indirilip tek tek Read ile açıldı; zemin rengi PIL ile ölçüldü. Uygulama içi satın almalar `apps.apple.com/tr/app/id…` sayfasından. Görüntüler depoda değil (oturum çalışma klasöründe, `scratchpad/rakip2/gokyuzu/`). "Bizde" sütunu koddan doğrulandı. Bizim tarafımız: Android ekran görüntüleri (31 Ekim) ve canlı site (time.bumba.tr/mevsimler/, /gece-gokyuzu/) Playwright görüntüleri.

## The Moon: Calendar Moon Phases (id1077455978; US 4,82 / 118.289 puan, TR 4,77 / 1.064; Vitalii Gryniuk)
- Ücret modeli: ücretsiz + reklam (ayrı "No Ads" satın alması var) + abonelik. Satın almalar: Unlock calendar ₺12,99 ve ₺199,99 (yıllık otomatik yenilenen abonelik; metin "Unlock calendar subscription"), No Ads ₺249,99, Zodiac Signs Pro ₺99,99. Takvim abonelikle açılıyor, burç takvimi ayrı ücretli. Diller yalnız İngilizce ve Rusça. Sürüm 6.5 (12 Mart 2026).
- İncelenen görüntü: 5 iPhone (hepsi 2017 tarihli, iOS 10 durum çubuğu "Carrier"; mağaza görselleri yenilenmemiş).
- İlk açılış / tanıtım / ödeme sayfası: görülmedi.
- Ana ekran ve gezinme: tek ekran, gezinme çubuğu: solda (i), ortada "The Moon", sağda takvim ve konum iğnesi. Altında "‹ January 11, 2017 ›" tarih gezgini (her güne geçilebilir), sağ üstte küçük "Lat 37.79° / Lon −122.41°". Bir sıra yuvarlak çizgi simge: burç işareti (♋ o günün Ay burcu), teleskop, ay+yıldız, yarım ay, pusula. Ortada **büyük, fotoğraf gerçekliğinde Ay** (gerçek Ay fotoğrafı dokusu, kraterler ve denizler, mavimsi ton, hafif hale). Altında üç değer: "14 Day" (Ay günü), "Full Moon", "13.5 days Age"; çok büyük ince "98.7%"; en altta "1:49 AM Moonrise / 4:25 PM Moonset". Zemin yıldız noktalı lacivert.
- Öne çıkan ekranlar:
  - Takvim (Calendar): ay ızgarası, hücrede gün numarası sol üstte ve **o günün evresine göre gerçek fotoğraf dokulu Ay** (8 kalıp değil, sürekli evre; hilal inceliği günden güne değişiyor). Dolunay ve yeni ay günlerinde Ay'ın üstüne kırmızı küçük yazı "Full Moon 1:44 PM", "New Moon 2:29 AM" (anın saati). Bugünün altında kırmızı çizgi. Önceki/sonraki ayın günleri soluk. Sağ üstte iki düğme: evre görünümü / burç görünümü.
  - Burç takvimi (aynı ızgara): her günde o gün Ay'ın bulunduğu burcun işareti ve adı (Aquarius, Pisces, Aries…), ince çizgi simgeler. "Zodiac Signs Pro" satın alması bununla ilgili.
  - Konum: harita (eski Apple haritası görünümünde, ızgaralı enlem/boylam), "Automatic / Custom" bölümlü düğme, enlem/boylam sağ üstte, yakınlaştırma +/−, "Style: square / circle".
  - Moon Data: çizgili tablo: Moonrise, Moonset, Azimuth 276°33′44″, Altitude 23°03′27″, Distance 374 255 km, Angular Diameter 0°32′27″, Ecliptic Longitude, Ecliptic Latitude, Obliquity of the ecliptic, Local Sidereal Time. Altında küçük Ay fotoğrafı + "Full Moon 98.7%", "Full Moon January 12", "New Moon January 28"; "Day 13 / Day 14" Ay günü ilerleme çubuğu ve küçük Ay başlıklı zaman kaydırıcısı.
- Ayarlar: görülmedi.
- Widget / kilit ekranı / saat: görüntüde yok.
- Tasarım: zemin #081329 (koyu lacivert), gezinme çubuğu #202D40; yazılar ince (SF light), büyük yüzde ince; düz liste, kart yok; simgeler ince çizgi. Tek koyu tema.
- Hatalar: mağaza görselleri 9 yıllık (2017).
- Bizde olmayan ya da farklı olan:
  - Fotoğraf gerçekliğinde Ay (gerçek doku, sürekli evre) — yok: uygulamalarda Ay **sistem emojisi** (8 kalıp: 🌑🌒🌓🌔🌕🌖🌗🌘), sitede düz iki renkli SVG — `mobile/ios/App/Sources/MoonView.swift` (moonPhaseEmoji, satır 5), `mobile/android/app/src/main/kotlin/tr/bumba/time/MoonScreen.kt` (MOON_PHASE_EMOJI, satır 44), `mobile/ios/Widget/Sources/MoonWidget.swift` (phaseEmoji), `src/js/nature.js` moonIcon (satır 146) — O — rakipte ücretsiz (ana ekran), takvim ücretli.
  - Ay takviminde her gün sürekli evre (bizde 8 kalıp; ör. ayın 5–8'i aynı görünür) ve dolunay/yeni ay anının saati hücrede — kısmen (sitede SVG sürekli evre var; uygulamada emoji 8 kalıp; hücrede yalnız "Dolunay" adı, saat yok) — aynı dosyalar — K — ücretli (takvim aboneliği).
  - Her güne gitme (tarih gezgini) ve seçili günün Ay ayrıntısı — yok (Ay takviminde güne dokunma işlevi yok; doğuş/batış yalnız bugün) — `MoonView.swift` MoonMonthGrid (dokunma yok) — K — ücretsiz.
  - Ay verileri tablosu (azimut, yükseklik, uzaklık, açısal çap, ekliptik boylam/enlem) — uygulamada yok, sitede yalnız uzaklık/perige/apoje (`src/js/sky.js`) — K — ücretsiz.
  - Ay günü (1–30) ve "Ay'ın yaşı" — kısmen (sitede "Ayın yaşı: 23,8 gün"; uygulamada yok: `nature.age` site anahtarı, `app-strings.json`'da Ay yaşı yok) — K — ücretsiz.
  - Ay'ın burcu takvimi (astroloji) — yok (sitede yalnız doğum tarihine göre Güneş/Ay burcu hesaplayıcı `src/js/more-tools.js` burç bölümü) — K — rakipte ücretli (₺99,99). Not: inanç/astroloji içeriği sahibin kararına kalır.

## MOON - Current Moon Phase (id660036257; US 4,82 / 29.922, TR 4,78 / 605; MOON CALENDAR LTD)
- Ücret modeli: ücretsiz + abonelik. Satın almalar: MOON Pro Yearly ₺149,99 / ₺299,99 (birden çok fiyat kademesi, "NT" = deneme yok?), "MOON Pro Yearly 50% Off" ₺699,99 ve "MOON Limited Time Offer" ₺699,99 (indirim gösterimli), **MOON Ambassador ₺249,99**, **MOON Patron ₺1.499,99** (destekçi/bağış kademeleri). Reklam görülmedi. Sürüm 16.3.13 (7 Nisan 2025), "8M+ Users", Webby 2017.
- İncelenen görüntü: 6 iPhone.
- İlk açılış / ödeme: görülmedi.
- Ana ekran: tam ekran **gerçek Ay fotoğrafı** (yüksek çözünürlüklü, kraterler ve ışınlar keskin), Ay ekranın soluna taşacak kadar büyük; zemin Samanyolu dokulu yıldızlı gökyüzü fotoğrafı (#0D0C12 civarı). Sağda evre adı "Waxing Crescent" (ince), dev "17%", altında iki satır "New Moon 27 days / Full Moon 12 days" (gün olarak geri sayım). Hilalin karanlık kısmında **yeryüzü ışığı (earthshine)**: karanlık yüz tamamen siyah değil, soluk gri dokulu.
- Öne çıkan ekranlar:
  - Takvim: iOS tekerlek tarih seçici (ay/gün/yıl: "September 17 2021"); seçilen tarihin Ay'ı üstte büyük gösteriliyor (karanlık yüzde yeryüzü ışığı).
  - Apple Watch: aynı görünüm (Ay fotoğrafı + "Waxing Crescent 17%").
  - Widget: orta boy, zemin yıldızlı fotoğraf, solda Ay fotoğrafı, sağda "Waning Gibbous / 76% / New moon in 8 days".
  - Paylaşım: iMessage'da Ay kartı (Ay fotoğrafı + "Waning Crescent 15%"); "Share it with anyone". Metinde: Ay görüntüsünü fotoğraflara kaydet, iMessage uygulaması, telefonu sallayınca "Star Harp" (yıldız arpı sesi), "animated, layered spacescape" (katmanlı hareketli uzay zemini), yarımküre ayarı, dolunay/yeni ay ve "other special lunar events" öncesi bildirim.
  - Son görsel: "Trusted by Millions", 4.8 yıldız ve üç kullanıcı yorumu (gri yuvarlak kartlar), altta büyük Ay.
- Ayarlar: görülmedi.
- Tasarım: siyah-gri, renksiz (tek renk aksan yok); yazı tipi yuvarlak hatlı geometrik sans (Poppins benzeri) ince + kalın karışık; kartlar çok yuvarlak (~24 px), widget köşesi sistem. Süs yok.
- Hatalar: görülmedi.
- Bizde olmayan ya da farklı olan:
  - Gerçek Ay fotoğrafı, yeryüzü ışığı (karanlık yüz soluk görünür), yıldızlı fotoğraf zemin — yok (emoji / düz SVG; bkz. The Moon) — O — ücretsiz.
  - "Dolunaya 12 gün, yeni aya 27 gün" gün olarak geri sayım — kısmen (bizde tarih var: "Sonraki dolunay: 24 Kasım 17:53", widget'ta "🌑 9 Kas"; kalan gün yok) — `src/js/nature.js`, `MoonWidget.swift` — K — ücretsiz.
  - Ay'ı görsel kart olarak paylaşma / fotoğraflara kaydetme — yok (paylaşım kartları yalnız vakit ve geri sayım için: `ShareCards.swift`, `ShareCards.kt`) — K — ücretsiz.
  - Orta boy Ay widget'ı (yıldızlı zemin, büyük Ay + metin yan yana) — yok (bizde yalnız küçük boy, düz tema zemini: `MoonWidget.swift` `.supportedFamilies([.systemSmall])`) — K — ücretsiz.
  - Apple Watch'ta Ay evresi (komplikasyon/uygulama) — yok (saat uygulaması yalnız vakit ve hicri tarih: `mobile/ios/Watch`, `WatchWidget`) — K — ücretsiz.
  - Destekçi kademeleri ("Ambassador", "Patron") — mevcut #25'e ek ayrıntı (bağış).

## Deluxe Moon Pro • App & Widget (id482361332; US 4,72 / 7.351, TR 4,69 / 32; Sergey Vdovenko)
- Ücret modeli: ücretsiz indirme + Premium abonelik (3 gün deneme). Satın almalar: Annual Premium + 3 Day Trial ₺999,99; Premium 1 week ₺199,99; 1 Month ₺399,99; 6 month ₺1.499,99; eski tek seferlik eklentiler: Planetary Hours ₺249,99, Moon Tables for professionals ₺249,99, Twilights • Sunrise • Sunset ₺249,99, Blue Moons ₺99,99, Supermoons/Minimoons ₺99,99, "Лунные аспекты" ₺249,99 (TR mağazasında Rusça adla duran satın alma: çeviri hatası). Premium'da kilitli: kişisel astroloji, etkinlik planlama, 28 etkinlik takvimi ("Personal forecasts, activity planning, the 28 activity calendars and other advanced tools require an optional Premium subscription"). Reklam görülmedi. Sürüm 5.0 (26 Ekim 2026), iOS 26+.
- İncelenen görüntü: 10 iPhone (mağaza görselleri: üstte küçük büyük harfli etiket "ULTIMATE LUNAR CALENDAR", kalın iki satır başlık, altında ince alt başlık; telefon çerçevesi bakır/turuncu kenarlı).
- İlk açılış / ödeme: görülmedi.
- Ana ekran: üstte "‹ Oct 23, 2026 ›" ve haftanın günleri şeridi (Su Mo Tu We Th **Fr** Sa, seçili gün kutulu, sağda geri dön ↺). Solda "Waxing Gibbous / Moon phase", sağda koordinat "51.507° N 0.128° W / Your location". Ortada büyük saat "9:41 AM / British Summer Time". **Fotoğraf dokulu Ay** (gri, gerçek doku, karanlık yüz siyah, arkasında yeşil-mavi kuzey ışığı benzeri sis). Ay'ın solunda küçük Ay + "90.4% ↑" (artıyor oku), sağında burç "♓ Pisces". Altta "05:34 Since moonset", "11.8 days" (yaş), "06:52 Until moonrise". Beş yuvarlak simge düğmesi. En altta **Ay'ın günlük yolu yayı**: "4:06 AM Moonset", "10:28 AM Lower transit", "4:33 PM Moonrise" yay üzerinde noktalar. Alt çubuk: yüzen yuvarlak simgeler (profil, yıldızlar, ortada büyük "Menu", parıltı, takvim).
- Öne çıkan ekranlar:
  - Lunar Calendars (28 takvim): kategori hapları (Beauty, Health, Garden, Home, Love, Money…), etkinlik simgeleri (Haircut, Hair coloring, Beauty treatment, Manicure, Spa), ay ızgarasında her günün altında renkli puan (28, 34, 64…) ve renkli çizgi (Excellent yeşil / Good / Neutral mavi / Weak turuncu / Avoid kırmızı lejant).
  - Western Forecast: doğum profili kartı ("Emma Parker, Mar 12, 1985 2:30 PM, London"), Sun ♓ Pisces, Moon ♐ Sagittarius, Asc ♌ Leo; tam doğum haritası çarkı (evler, gezegen işaretleri, açı çizgileri renkli); "Horoscope / Today" bölümlü düğme; "AI" açılır bölüm.
  - Moment check: hap seçiciler (Signing a contract, Job interview, First date, Haircut, Launching a project, Starting a trip, Doctor visit, Planting and sowing, More…), hafta şeridi, tekerlek saat seçici, sonuç kartı: halkada "72 Good", "Jupiter hour · Day of Jupiter, Moon in Gemini, Moon day 19", artı puanlı etkenler (Planetary hour +14, Moon aspects +7, Moon sign +5, Day ruler +5).
  - Vedic Forecast (Rashi/Navamsha, North/South Hint haritası), Kundli Milan (gelin-damat uyumu, "63 / 22½ of 36 points", Ashtakoota listesi).
  - Gardening (Ay'a göre bahçe): Planting and sowing / Pruning and weeding / Harvesting, ay ızgarasında gün puanı ve simge, alt kısımda günün 24 saatlik renkli şeridi.
  - **Moon Phases (yıl görünümü)**: "2026" yıl çarkı: 12 ay çember üzerinde, her ayın yeni ay/dolunay/dördünleri fotoğraf dokulu küçük Aylar, ortada "2026 / 13 Full Moons / 1 blue Moon", bugünü gösteren ibre; altında "Table / List" ve tablo: Month | New | First quarter | Full | Last quarter (Jan 18 26 3 10…), sütun başlıklarında evre simgeleri.
  - **Moon close-up**: döndürülebilir 3B Ay ("Drag to turn the Moon, pinch to zoom"), katman düğmeleri None / Seas / Craters / **Apollo** (Apollo 11–17 iniş yerleri kırmızı iğneli), güneş ve nişangâh simgesi; kart "Waxing Gibbous · 91% lit · 11.8 days old"; "BEST SEEN TONIGHT: Aristarchus, Gassendi, Kepler" (sonlandırıcı çizgisine yakın kraterler, dürbünle görülür notu); altta "Moon maps: NASA's Scientific Visualization Studio".
  - Widget ve saat: "15 widgets, 14 complications". Görselde: "Sun & Moon" geniş widget (24 saatlik **Güneş ve Ay yükseklik eğrileri**, sarı ve beyaz dalga, ↑7:38 AM ↓5:50 PM, ↑4:33 PM ↓4:06 AM), büyük ay takvimi widget'ı (her günde fotoğraf dokulu Ay, "10/26 4:11 AM", "11/01 8:28 PM" sıradaki evre anları), Apple Watch Ultra'da Ay fotoğrafı + "Waxing Gibbous, Illumination 99% ↑, Age 14.0 days, Aries 23°08′".
  - Metinde: sivil/denizcilik/astronomik alacakaranlık, altın ve mavi saat, tutulmalar, süper Ay/mini Ay/mavi Ay, ekinoks, "3D Sun–Moon diagram", Güneş ve Ay pusulası, Ay boşluğu (void-of-course), gezegen saatleri, notlar (etiket, renk, puan), yeni ay niyeti ve dolunayda geri dönme, Apple Takvim'e ekleme ve hatırlatıcı.
- Ayarlar: görülmedi (metinde tema, zemin, kısayollar).
- Tasarım: zemin lacivert gradyan #223B51 → #132333 (mağaza), uygulama içi #121824–#080E1A; hafif yeşil-mavi aurora sisi; kartlar yarı saydam koyu, köşe ~16 px; yüzen yuvarlak simge düğmeleri; yazı SF (ince ve orta). Tek koyu tema.
- Hatalar: TR mağazasında Rusça adlı satın alma ("Лунные аспекты").
- Bizde olmayan ya da farklı olan:
  - Döndürülebilir 3B Ay, denizler/kraterler/Apollo iniş yerleri katmanı, "bu gece en iyi görülen kraterler" — yok — `grep -ri "apollo\|crater" mobile/ src/` boş — O — rakipte büyük olasılıkla ücretsiz bölümde (görselde kilit yok). Doku kaynağı NASA SVS "CGI Moon Kit" (svs.gsfc.nasa.gov/4720; `lroc_color_2k.jpg` 2048×1024, 447 KB; SVS SSS: "All of our content is in the public domain (unless otherwise noted)", atıf "NASA's Scientific Visualization Studio"; 3 Kasım 2026'da sayfadan okundu).
  - Yıl çarkı (bir yılın bütün evreleri çember üzerinde, "13 dolunay, 1 mavi Ay") ve yıl evre tablosu (ay başına yeni ay / ilk dördün / dolunay / son dördün günü) — yok (bizde yalnız aylık takvim ve sitede yıllık ekinoks tablosu) — `MoonView.swift`, `src/js/nature.js` drawMoonCal — K — ücretsiz görünüyor.
  - Mavi Ay, süper Ay / mini Ay işaretleri — kısmen (sitede gece gökyüzü sayfasında "süper ay" notu: `src/js/sky.js`; uygulamada yok) — K — rakipte eski tek seferlik ₺99,99'luk eklenti.
  - Ay'ın günlük yolu yayı (doğuş–üst geçiş–batış) ve "Ay batalı 5 sa 34 dk / doğuşa 6 sa 52 dk" geri sayımları — kısmen (Vakitler'deki Güneş/Ay yayı yalnız gece Ay'ı gösteriyor: `PrayerExtras.swift` SunMoonArc; doğuşa kalan süre yok) — K — ücretsiz.
  - Güneş ve Ay yükseklik eğrisi widget'ı (24 saat) — yok (`Widget/Sources` içinde yok) — O — ücretsiz görünüyor.
  - Ay evreli aylık takvim widget'ı (büyük boy) — yok (yalnız küçük Ay widget'ı) — O — ücretsiz görünüyor.
  - Apple Watch'ta Ay evresi — yok (bkz. MOON) — K.
  - Astroloji (doğum haritası, Vedik, uyum), Ay'a göre etkinlik/saç kesimi/bahçe takvimleri, gezegen saatleri — yok — ücretli (Premium). Not: bilimsel dayanağı yok ve bazı kullanıcılar için inançla çelişebilir; yapılması sahibin kararına kalır, öneri: yapılmasın (en çok "Ay'a göre bahçe" halk bilgisi olarak düşünülebilir).
  - Sabah günlüğü ile Ay döngüsü bağı (yeni ay niyeti, dolunayda geri dönüş) — yok (`Journal.swift` Ay'a bağlı değil) — K — ücretli.

## Moon Phases and Lunar Calendar (id1126370589; US 4,87 / 37.897, TR 4,82 / 564; Business City s.r.o.)
- Ücret modeli: ücretsiz + reklam ("Remove Ads" ₺149,99) + abonelik/tek seferlik karışık: Moon PRO ₺1,99 / ₺19,99 / ₺39,99 / ₺79,99 / ₺149,99 (kademeler), Moon Plus ₺21,99 / ₺26,99, **"Enable Notifications" ₺49,99 (bildirimler ücretli)**, **"Wolf Howl" ₺49,99 (kurt uluması sesi)**. Sürüm 2.9.46 (21 Eylül 2026), 11 dil, Türkçe yok.
- İncelenen görüntü: 9 iPhone (2017–2021 tarihli; "Carrier" durum çubuğu).
- İlk açılış / ödeme: görülmedi.
- Ana ekran ve gezinme: alt sekmeler Moon / Calendar / Compass / Sun / Our Apps (ince çizgi simgeler, seçili beyaz). Üstte hamburger, zil (bildirimler), konum iğnesi (altın sarısı). Moon sekmesi: "Cupertino ✎" şehir, "← Waxing Gibbous →" (evreler arası atlama okları), **büyük fotoğraf dokulu Ay**, üstüne beyaz yazı "Tuesday 6/2/20 / 87% / Time in Cupertino 9:49 AM"; solda **uluyan kurt** simgesi (ücretli ses), sağda "→ Now" hap düğmesi. Altında **yatay zaman cetveli** (çentikli, gün etiketleri "Jun 1, 2020 monday", ortada kırmızı üçgen imleç; Ay'ın gökte olduğu aralıklar sarı bant): parmakla kaydırarak saat/gün değiştiriliyor. Altında liste: Distance 226,827.350 mi, Moonset 3:56 AM (5h52m ago), Moonrise 5:16 PM (in 7h27m), Culmination 10:53 PM (in 13h4m), Full Moon in 3d2h23m, Moon Age 10d23h10m, New Moon in 18d13h52m, Moon Azimuth, Moon Altitude (aşağı kaydırınca soluklaşan liste). Metinde: ana ekranda aşağı kaydırınca "etkileşimli Ay".
- Öne çıkan ekranlar:
  - Calendar: sürekli kayan aylar ("May 2017", "June 2017"), her gün **fotoğraf dokulu Ay** ve üstünde aydınlık yüzdesi (35%, 50%…; yeni ayda "0.7%", "0%"); dördün/dolunay/yeni ay günlerinde anın saati sarı ("19:47", "14:42", "05:16"); bugün turuncu nokta; altta yıl şeridi (2016 2017 2018).
  - Widget'lar (iOS 14 dönemi, 5 sayfa): küçük Ay (gökyüzü mavisi zeminde gündüz Ay'ı; siyah zeminde şehir adıyla "Sydney"), iki şehir yan yana; geniş ay takvimi (her günde evre dairesi); **"sıradaki 4 evre" widget'ı** (Jan 20 1:01 PM ilk dördün, Jan 28 dolunay… her birinde doğuş/batış oklarıyla saat); büyük takvim widget'ı (lacivert #283347 zemin, gün kutuları, bugün kırmızı çerçeve); **Ay yükseklik eğrisi widget'ı** ("New York": günün Ay yükseklik dalgası, doğuş/üst geçiş/batış noktaları ve saatleri, "now" çizgisi; Sydney sürümü dikey yazılı); bilgi widget'ı (Ay fotoğrafı + "28% Waxing Crescent" + Moonrise 10:53 AM, Culmination 5:00 PM, Moonset 11:07 PM, Full Moon in 10d 5h, Moon in ♈ Aries).
  - Notifications: şehir başlığı altında satırlar: "at Moonrise / at Full Moon / at New Moon / 30 min before Moonset / 2 hrs after Full Moon / 2 hrs 10 min before Full Moon Rise", sağda "every time" ve anahtar; "+", "Edit", "Add Notification" çerçeveli düğme. **Kullanıcı kendi olay + önce/sonra süresi bileşimini kuruyor.**
  - Konumlar: "AUTOMATIC" (Paris) ve "CUSTOM" (Munich, Los Angeles ✓, Rome, Madrid, Paris) listesi, koordinat ve ülke; "Add Location".
  - Burç çarkı: Tropical / Sidereal / Astronomical bölümlü düğme; iç içe burç halkaları, Güneş (sarı top) ve Ay (beyaz top) konumu, seçili burç dilimi altın vurgulu; altta "Libra" ve aynı zaman cetveli.
  - Compass ve Sun sekmeleri: görüntüsü yok (metinde Güneş doğuş/batış, altın ve mavi saat, Ay'ın tepe noktasına kalan süre, Ay tutulması).
- Ayarlar: görülmedi.
- Tasarım: zemin siyah #000000, üst çubuk #131313, alt çubuk #2E2E2E; vurgu altın sarısı (~#E0C27A, ölçülen kenar #C1AC77); yazı SF ince-orta; ayırıcı çizgili liste; düğmeler çerçeveli hap. Bildirim/konum ekranları sistem açık teması (beyaz, mavi #007AFF düğmeler).
- Hatalar: mağaza görselleri eski (2017–2021).
- Bizde olmayan ya da farklı olan:
  - Zaman cetveliyle saat/gün kaydırma ve o anın Ay'ı (evre, yükseklik, azimut, uzaklık) — uygulamada yok; sitede gece gökyüzünde yalnız saat kaydırıcısı (bir gece) — `src/js/sky.js`; Mevsimler/Ay ekranı (`MoonView.swift`, `MoonScreen.kt`) yalnız bugün — O — ücretsiz.
  - "Doğuşa 7 sa 27 dk", "dolunaya 3 g 2 sa 23 dk", Ay'ın üst geçişi (culmination) saati — kısmen (dolunay tarihi var, geri sayım ve üst geçiş yok) — `MoonView.swift`, `src/js/nature.js` — K — ücretsiz.
  - Özelleştirilebilir Ay/Güneş bildirimleri (olay seçimi: Ay doğuşu/batışı, dolunay, yeni ay, dolunay doğuşu; önce/sonra süresi; birden çok şehir) — kısmen (bizde tek anahtar "Ay evreleri: dolunay ve yeni ay anında" ve "Gün doğumu ve batımı"; süre seçimi, Ay doğuşu yok) — `mobile/shared/app-strings.json` `notify.cat.moon.note`, `FeatureNotifier.swift`/`FeatureNotifier.kt` — K — **rakipte ücretli (₺49,99)**.
  - Sıradaki 4 evre widget'ı, Ay yükseklik eğrisi widget'ı, Ay bilgi widget'ı (doğuş/üst geçiş/batış/dolunaya kalan), aylık evre takvimi widget'ı — yok (yalnız küçük evre widget'ı: `MoonWidget.swift`, `widget_moon.xml`) — O — ücretsiz görünüyor (widget'lar ücretsiz sürümde).
  - Ay takviminde her günde aydınlık yüzdesi ve evre anının saati — kısmen (sitede yüzde yalnız `title` ipucunda; uygulamada yok) — `src/js/nature.js` satır 180–182 — K — ücretsiz görünüyor.
  - Gündüz Ay'ı gökyüzü mavisi zeminde (widget) — yok — K.
  - Ses (kurt uluması) — yok — rakipte ücretli; önerilmez.

## Daily Life with Moon Calendar (id1510595432; TR 4,71 / 430, US 4,85 / 33; Sevda Meriç Çölgeçen, Türk geliştirici)
- Ücret modeli: ücretsiz + "Premium Upgrade" ₺8,99 ve ₺99,99 (abonelik mi tek seferlik mi sayfada yazmıyor). Reklam görülmedi. Sürüm 1.09 (19 Aralık 2022; 4 yıldır güncellenmemiş), 20 dil, Türkçe var. Türkiye'deki "Ay takvimi / saç kesimi" türünün en çok puan alanı.
- İncelenen görüntü: 10 iPhone (pembe-kırmızı gradyan mağaza zemini #F9375C, ince başlık "Moon Calendar", eğik telefon çizimleri; görseller İngilizce).
- İlk açılış / ödeme: görülmedi.
- Ana ekran: üstte yıldızlı gece fotoğrafı ve ağaç silueti üstünde **fotoğraf dokulu Ay**; sağda kırmızı "GMT-08:00 Fri", "‹ 07 Feb 2020 08:00 AM ›" (yuvarlak çerçeveli oklarla gün gezgini), "Location 34°03′08″ N 118°14′37″ W". Dört simge + saat: Güneş doğuşu 06:45, batışı 05:28 PM, Ay doğuşu 03:54 PM, batışı 05:41 AM (kırmızı saatler). Altında 9 yuvarlak ince çizgi bölüm simgesi (dünya, yıldız, güneş, Ay evreleri, hilal+yıldız, Ay burcu, saat, tutulma); seçili bölüm kırmızı. Altında seçili bölümün içeriği.
- Öne çıkan ekranlar: Moon info (Moon Phase Waxing Gibbous, Moon Age 12.93 days, Moon Zodiac Cancer 25°58′, Illumination %96.25, Moon Sign Element Water, Ay menzili "10. Al-Jabhah", sonraki yeni ay "23 February 2020 07:33 AM", Güneş/Ay burç dereceleri); Moon/Zodiac takvimi (ay ızgarası, her günde fotoğraf dokulu küçük Ay ya da burç işareti, burç değişim saati "03:46 PM", yeni ay/dolunay işaretli); Today's Advice (saç boyama, ameliyat, epilasyon için "uygun değil" kartları, makas/havan çizimleri, "Share"); Energy of the day; Void of Course listesi (açı, başlangıç-bitiş, süre "4 hr 42 min"); Planetary Hours (gündüz/gece saatleri ve gezegen); Transit map (Kuzey/Güney Ay düğümü, Ay–Venüs "Trine" yorumu); Notifications: Push Notification (All, Void of Course, **Moon Phase, Lunar eclipse, Solar eclipse**, Diet days, Epilation, Skin care, Manicure-Pedicure, Haircut, Hair coloring, Massage) ve Planetary Hours bildirimleri (Sun, Moon, Mercury…), yeşil iOS anahtarları.
- Ayarlar: görülmedi.
- Tasarım: uygulama zemini #1C1C1E (koyu gri, iOS koyu), kartlar #2B2B2D yuvarlak (~12 px); vurgu kırmızı-pembe #F9375C; yazı Montserrat benzeri ince geometrik; simgeler el çizimi ince çizgi. Mağaza görselleri eğik telefon (yazılar okunması zor).
- Hatalar: "Thurs", "Tues" kısaltmaları tutarsız; mağaza görselleri 2020.
- Bizde olmayan ya da farklı olan:
  - Tutulma bildirimi (Ay ve Güneş tutulması) — yok (bildirim kategorileri: namaz, takvim, mevsimler, Ay [dolunay/yeni ay], Güneş, hava, saat başı, yaz saati; tutulma yok) — `mobile/shared/app-strings.json` `notify.cat.*`, `FeatureNotifier.swift` — K — ücretsiz.
  - Ay sayfasında tarih gezgini (önceki/sonraki gün) ve yıldızlı fotoğraf başlık — yok (bkz. The Moon) — K.
  - Ay burcu, Ay boşluğu, gezegen saatleri, saç kesimi/epilasyon günleri — yok — astroloji; öneri: yapılmasın (sahibin kararı).

## Sky Guide (id576588894; US 4,84 / 378.873, TR 4,80 / 1.282; Fifth Star Labs; Apple Design Award 2014 kazananı, 2024 finalisti)
- Ücret modeli: ücretsiz + abonelik: PLUS ₺109,99 ve ₺869,99, PRO ₺214,99 ve ₺1.309,99 (aylık/yıllık oldukları fiyattan anlaşılıyor), PRO Lifetime ₺5.599,99, All Constellations ₺99,99 (eski tek seferlik). Reklam görülmedi. Sürüm 13.1.2 (24 Ekim 2026), 18 dil, Türkçe var. Görsellerde Pro kilidi görülmedi.
- İncelenen görüntü: 10 iPhone (mor-lacivert bokeh zemin #261243 → altta deniz yeşili #005363; çok kalın beyaz başlıklar "Stargazing made easy").
- İlk açılış / ödeme: görülmedi.
- Ana ekran (gökyüzü haritası): telefonu göğe tutunca **AR/hareket sensörü** ile gökyüzü; takımyıldızlar ince beyaz çizgi + yarı saydam mavi cam gibi **figür çizimleri** (Büyükayı yarı saydam kristal ayı), takımyıldız adları büyük harf ve harf aralıklı, eğrisel yazı ("URSA MINOR", "DRACO", "LEO", yıldız adı "Regulus"). Altta yüzen yuvarlak buzlu cam düğmeler: ayarlar anahtarı, saat "3:52" (zaman kontrolü), pusula/AR, katmanlar, arama.
- Öne çıkan ekranlar:
  - Explore (alttan açılan sayfa, X kapatma): büyük kart "Tonight's Best" (göz/dürbün/uydu simgeli liste ikonu), "Featured" yatay kartlar (fotoğraflı: "Nova in Sagitta", "Blood Moon Rising"), "Calendar" yatay kartlar (tarih rozetli: Full moon, Saturn at opposition, Moon and Mars), arama çubuğu.
  - **Tonight**: üstte bulutlu gece gökyüzünde fotoğraf gerçekliğinde hilal, "Partly cloudy all night / Moon rises at 1:22 PM" (hava durumu ile birleşik); "Visible Planets" grafiği: SUNSET 7:17 PM – SUNRISE 6:46 AM ekseni, her gezegen için ayrı şeritte **renkli yükseklik eğrisi** (Moon gri, Venus pembe, Mars kırmızı, Jupiter mavi, Saturn yeşil), alt eksende saatler 4 7 10 1 4 7.
  - **Calendar (olay takvimi)**: koyu kartlar, üstte "THURSDAY, AUGUST 27" küçük büyük harf, altında kalın olay adı; sağda olayın fotoğraf gerçekliğinde görseli: "Partial lunar eclipse" (kızıl Ay), "Full moon", "Metis at opposition" (asteroit şekli), "Moon and Mars", "Moon and Jupiter" (alacakaranlık gökyüzünde ince hilal + nokta: **Ay–gezegen kavuşumu**). Metinde: konuma göre süzülmüş Güneş/Ay tutulmaları, meteor yağmurları.
  - **ISS**: "TIME SENSITIVE / ISS Flyover / International Space Station peaks soon (5 minutes)!" bildirimi; ISS 3B modeli; "NEXT PASS: TONIGHT — APPEARS 9:27:59 PM (WSW), PEAKS 9:31:21 PM (NNW), DISAPPEARS 9:31:47 PM (NNE), APPARENT MAGNITUDE −3.7, PEAK ALTITUDE 70°".
  - Zaman kontrolü: "2021 Aug 15 17:14:00 UTC" hap, altında çentikli tekerlek cetvel (15 16), geri al ve arama; Jüpiter yakın plan (Ganymede, Europa uyduları ve gölgeleri), Güneş tutulması tam evresinde korona (fotoğraf gerçekliği).
  - Featured haberler ("December 2025", "NEWS Blast from the Past", "Martian Lightning"): sunucudan gelen astronomi haberleri.
  - **Location / Dark Sky Finder**: Apple Maps üstünde **ışık kirliliği haritası** (mor-turuncu-sarı ısı), "Save Location", "Artificial Light Pollution 4" (Bortle benzeri ölçek rozeti), "Limiting magnitude now −6.4", "Darkest 9:15 PM – 4:55 AM" (astronomik gece aralığı).
  - Derin yakınlaşma: Orion Bulutsusu fotoğrafı üstünde kesik çizgili daireler ve gök cismi adları (Trapezium Cluster, NGC 1980, HD 36999).
- Ayarlar: görülmedi.
- Widget / kilit ekranı: görüntüde yok (ISS bildirimi kilit ekranında).
- Tasarım: uygulama zemini neredeyse siyah #0E0E10 – #0C101C; kartlar #2E3350 civarı koyu lacivert, köşe ~20 px; yüzen buzlu cam düğmeler; yazı SF Pro kalın başlık + küçük büyük harf etiketler; olay görselleri fotoğraf gerçekliğinde 3B render. Tek koyu tema.
- Hatalar: görülmedi.
- Bizde olmayan ya da farklı olan:
  - Gece gökyüzü haritasında takımyıldız çizgileri ve adları (bizde 39 parlak yıldız nokta ve 4–5 ad, çizgi yok, Ay ve Samanyolu yok) — kısmen — `src/js/sky.js`, `src/js/sky-calc.js` (39 yıldız); canlı site görüntüsü: koyu lacivert disk üstünde seyrek beyaz noktalar, açık temada "Arcturus" adı disk kenarında kesiliyor — O — rakipte ücretsiz (takımyıldızların tamamı eski ₺99,99 eklenti).
  - "Bu gece gezegenler" için **yükseklik eğrisi grafiği** (gün batımı–doğumu ekseni, gezegen başına renkli eğri); bizde dar ekranda 6 sütunlu tablo yatay taşıyor ve satırlar 6–8 satıra kırılıyor ("07:00–07:00 · Doğuş 06:30 · Batış 17:00") — kısmen — `src/js/sky.js`; canlı site 390 px görüntüsü — K/O — ücretsiz.
  - Gökyüzü olay takvimi: Ay–gezegen kavuşumları, karşı konum (opposition), **meteor yağmurları** (tepe tarihi, saatlik sayı), tutulmalar tek listede — kısmen (tutulmalar ve dolunay/yeni ay var; kavuşum, karşı konum, meteor yağmuru yok: `grep -ri meteor src mobile` boş) — O — ücretsiz.
  - ISS geçiş tahmini ve bildirimi — yok — B (yörünge öğeleri TLE her gün değişir; sunucusuz yapılamaz: ya her derlemede site için statik veri, ya uygulamanın CelesTrak'tan çekmesi gerekir; dış hizmet izni) — rakipte ücretsiz görünüyor.
  - Gece en karanlık aralığı (astronomik alacakaranlık sonu–başı), "çıplak gözle sınır parlaklık" — kısmen (sitede altın/mavi saat var; astronomik alacakaranlık yok) — `src/js/nature-calc.js` `sunAltTimes` — K — ücretsiz.
  - Işık kirliliği haritası — yok — B (veri lisansı: World Atlas/VIIRS; harita karosu) — rakipte ücretsiz görünüyor.
  - AR (telefonu göğe tutma) — yok — B — ücretsiz.
  - Bu geceki hava ile birleşik gözlem özeti ("Parçalı bulutlu, Ay 13:22'de doğuyor") — yok (hava ve gökyüzü ayrı) — K — ücretsiz.

## Night Sky (id475772902; US 4,71 / 498.220, TR 4,70 / 25.909; iCandi Apps)
- Ücret modeli: ücretsiz + abonelik "Night Sky +" ₺199,99 / ₺249,99 / ₺399,99 ve "Night Sky + Aura" (yapay zekâ) ₺449,99 / ₺899,99 / ₺2.999,99 (iki kez). Aura, Connected Stargazing (SharePlay) ve "+" özellikleri abonelikte. Reklam görülmedi. Sürüm 27.2 (2 Kasım 2026), 68 dil, Türkçe var; iPhone, iPad, Apple Watch, Vision.
- İncelenen görüntü: 8 iPhone (zemin gerçek yıldızlı gökyüzü fotoğrafı + alt kısımda ağaç silueti, kalın beyaz ortalı başlık).
- İlk açılış / ödeme: görülmedi.
- Ana ekran (gökyüzü): AR ile telefonu göğe tutma; takımyıldızlar **gümüş/kristal 3B heykel** figürler (kartal, Yılancı ve yılan, at), ince çizgiler, çok sayıda etiket (yıldız, Messier, NGC, uydu "EUTELSAT KA-SAT 9A", meteor yağmuru radyantı "METEOR PRIRODA"); ufukta ağaç silueti ve yeşil kuzey ışığı. Sol üstte iki küçük gösterge: "☁ 89%" (kırmızı çubuk, **bulutluluk**) ve "○ 35%" (sarı çubuk, Ay aydınlığı). Sağda dikey yuvarlak düğme sütunu (AR çerçeve, pusula, katmanlar, renk çarkı, Ay). Altta "Ask Aura or Search" arama çubuğu (hap).
- Öne çıkan ekranlar:
  - Night Sky Aura: sohbet (mor balon kullanıcı, koyu gri balon yanıt), "Start UK Night Sky Tour" düğmesi; yapay zekâ astronom (sunucu).
  - Derin yakınlaşma: Samanyolu fotoğrafı (Yay bölgesi), yüzlerce NGC/IC/HIP etiketi.
  - **Time Travel**: "23/08/2025, 21:34", geri sar / duraklat / ileri sar, hız "1x", yıl-ay-gün-saat-dakika satırı, saat cetveli (15…23) ve kırmızı imleç; "Sky Timestamps" kayıtlı anlar listesi (31/12/2032 01:26).
  - Sky Tags: renkli etiket iğneleri (10 renk daire), kategori hapları (Favorite +, Memorials +, Sightings +, Observations), liste ("David's Favourite Star", "Aliens Home"); yıldızı birine anma olarak işaretleme.
  - **Aurora Forecast**: Apple Maps koyu haritası, uzaklığa göre konum listesi ve mor yuvarlak yüzde rozetleri (Dawson City 16%, Fairbanks 16%…), bildirimli (metin).
  - SharePlay ile uzaktan birlikte gözlem (FaceTime üst çubuğu, "Participants / Objects", "Add Objects", Neptune "Found/Searching").
  - Metinde: AirPods ile uzamsal ses yönlendirme (cisim "cıvıldar"), tavanı AR ile açma, Apple Watch'ta gökyüzü.
- Ayarlar: görülmedi.
- Tasarım: panel zemini #191919, kartlar #363636 (iOS koyu gri), vurgu mor-mavi #5B5BF0 civarı, mavi + düğmesi; yazı SF; alttan açılan sayfalar (tutamaçlı). Mağaza zemini #051143.
- Hatalar: görülmedi.
- Bizde olmayan ya da farklı olan:
  - Gece gökyüzünde zamanı oynatma (ileri/geri sarma, hız) ve herhangi bir tarihe gitme — kısmen (sitede yalnız o gecenin saat kaydırıcısı; tarih seçimi yok) — `src/js/sky.js` — K — ücretsiz görünüyor.
  - Gökyüzü sayfasında bulutluluk ve Ay aydınlığı göstergesi (gözlem koşulu) — yok (hava ve gökyüzü ayrı sayfalar; hava Open-Meteo'da bulut verisi alınabilir) — K — ücretsiz.
  - Takımyıldız figürleri (çizim) — yok — O/B (çizim lisansı) — ücretsiz.
  - Kutup ışığı tahmini — yok — B (NOAA SWPC verisi, sunucusuz istemciden çekilebilir; Türkiye için nadiren anlamlı) — ücretsiz görünüyor.
  - Yapay zekâ astronom, SharePlay, uzamsal ses — yok — rakipte abonelik; kapsam dışı (sunucu/hesap).

## SkyView® Lite (id413936865; US 4,54 / 18.801, TR 4,55 / 1.587; Terminal Eleven)
- Ücret modeli: ücretsiz (Lite) + tek seferlik eklentiler: Brightest Satellites & Sightings ₺49,99, **Stargazing Music Pack ₺49,99** (gözlem müziği). Ücretli tam sürüm ayrı uygulama (SkyView, US $2.99). Reklam görülmedi. Sürüm 3.8.11 (23 Eylül 2025), yalnız İngilizce.
- İncelenen görüntü: 4 iPhone (2011–2012 ödül rozetleri; eski görseller).
- Öne çıkan ekranlar:
  - AR: telefonu göğe tutunca gündüz gökyüzü fotoğrafı üstünde Ay ve yıldızlar; alt bilgi kartı "ⓘ MOON — NATURAL SATELLITE OF THE EARTH, ABOVE HORIZON — The ocean tides on Earth are mostly due to the Moon's gravitational pull."
  - Takımyıldız: Akrep figürü beyaz kontur çizim, mavi çizgiler, Samanyolu fotoğrafı zemin, parlak yıldızda ışın parlaması; kart "ⓘ SCORPIUS — CONSTELLATION, ABOVE HORIZON" + tek cümle bilgi.
  - **Sightings (gözlem önerileri listesi)**: her satırda cismin yuvarlak görseli (Ay, Capella, Sirius, Jüpiter, Arcturus, ISS, Hubble, JB-3 2 uydusu), mavi başlık, bir cümle "Right now, if you look up towards the northeastern sky you might see Capella.", "Planet Jupiter will be rising above the horizon after midnight in the eastern sky.", sağda saat (9:41 PM, 12:11 AM, 4:46 AM) ve **takvime ekle** simgesi. Başlık "Set notifications for stargazing events!". Alt sekmeler Search / Favorites / Sightings.
  - ISS 3B modeli ve yörünge izi (noktalı yol), Ay yakın plan.
- Tasarım: zemin #1C1C1C civarı, mavi başlıklar (#4A90E2 civarı), **dar (condensed) yazı tipi** büyük harf başlıklar; yuvarlak cisim görselleri fotoğraf gerçekliğinde.
- Hatalar: görselde "JB-3 2" adlı uydu açıklaması kesik.
- Bizde olmayan ya da farklı olan:
  - Gece gözlem önerileri listesi (cümleyle yön ve saat: "Jüpiter gece yarısından sonra doğuda yükselir") ve takvime ekleme — kısmen (sitede gezegen tablosu yön ve saat veriyor ama cümle/öneri ve takvime ekle yok; uygulamada hiç yok) — `src/js/sky.js` — K — ücretli eklenti (₺49,99).
  - Gökyüzü cisimleri için kısa bilgi kartı ("Gelgitler çoğunlukla Ay'ın çekiminden") — yok (sitede SSS var, cisim kartı yok) — K — ücretsiz.
  - Gözlem müziği — yok — rakipte ücretli; öneri: bizdeki uyku/odak sesleri (`FocusSounds`) yeterli.

