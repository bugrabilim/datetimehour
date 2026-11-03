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

