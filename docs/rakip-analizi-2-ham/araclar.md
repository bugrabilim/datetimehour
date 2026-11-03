# Zaman araçları (ikinci dalga, 3 Kasım 2026)

Alan: kronometre, zamanlayıcı (çoklu ve aralık), geri sayım (etkinliğe kalan gün), dünya saati / saat dilimi çevirici, pomodoro / odak, tarih hesaplama. Alarm ve uyku ilk dalgadaydı. Forest, Focus To-Do (`kadran.md`), Countdown Widget ve Time Buddy (`namaz.md`) ilk dalgada incelendi; burada yeniden ele alınmadı.

Görüntüler: `scratchpad/rakip2/araclar/<uygulama>/iN.jpg` (App Store TR vitrini, 600 px genişlik; depoya konmadı).

Bizim karşılaştırdığımız ekranlar (Android, 31 Ekim 2026, `mobile/android/app/build/screens/`): `tr-araclar`, `tr-zamanlayici`, `tr-hiit`, `tr-rutinler`, `tr-geri-sayimlar`, `tr-paylas-geri-sayim`, `tr-pomodoro`, `tr-pomodoro-istatistik`, `tr-kronometreler`, `tr-dunya`, `tr-toplanti-planlayici`, `tr-tarih-hesaplama`, `widget-geri-sayim`, `widget-dunya`, `widget-kronometre`.

**Bizim ekranlarımızın kısa tarifi (kıyas için):** krem zemin (#F6F3EC civarı), beyaz 16–20 px köşeli kartlar, kiremit vurgusu (#9C3F00 civarı), Roboto. Zamanlayıcı listesi beyaz kartta satır satır "03:05" ince büyük rakam + ‖ / ▶ / "+1 dk" / ×; görsel ilerleme (halka, çubuk) yok. Yeni zamanlayıcı üç sütunlu ok düğmeli seçici ("00 05 00") + hap biçimli hazır süreler. Pomodoro: ortada "Çalışma" + "25:00" düz metin, altında "Tur 1 / 4"; halka ya da görsel yok. Aralık (HIIT): koyu zemin (#11141B civarı), evre adı yeşil, "00:05", ince yeşil ilerleme çubuğu. Kronometre: "01:35.40" büyük kalın, tur listesi ("En iyi" yeşil, "En yavaş" kırmızı). Geri sayımlarım: emoji + başlık + kiremit renkli "23 gün kaldı" + tarih; fotoğraflı kart koyu gradyan. Geri sayım widget'ı: krem zemin, "Yılbaşı" kiremit başlık, "62 gün kaldı" kalın, tarih. Dünya saatleri widget'ı: dört satır ad + fark + saat.

## Countdown Star (App Store 576177593; ABD 4,77 / 216.589 puan; TR 4,69 / 42; Joseph Merrill; sürüm 3.0.12, 27 Ekim 2026)
- Ücret modeli: ücretsiz + reklamlı; tek satın alma **Remove Ads ₺99,99**; ayrıca reklamsız ücretli sürümü var (açıklamada yazıyor). Abonelik yok.
- İncelenen görüntü: 7 iPhone (hepsi açıldı; mağaza görselleri çerçevesiz, doğrudan uygulama ekranı).
- İlk açılış / ödeme: görülmedi.
- Ana ekran ve gezinme: sekme yok. Her etkinlik **tam ekran fotoğraf** (i1 deniz/kumsal, i2 doğum günü mumları, i3 havai fişek); etkinlikler arasında kaydırılarak geçiliyor (açıklama). Sol üstte iki yuvarlak cam düğme (liste ☰, +), sağ üstte hap içinde kalem + "…" ve ayrı dişli; düğmeler fotoğrafın rengini alan yarı saydam cam.
- Öne çıkan ekranlar:
  - **Sayaç kutusu:** fotoğrafın ortasında yarı saydam buzlu cam kutu (köşe ~20 px; açık fotoğrafta beyaz %60, koyuda siyah %40). Sol üstte başlık "Holiday" (normal ağırlık, ~22 pt) ve gri tarih "Sat, 18 Jul 2026". Altında **altı sütun: Years · Months · Days · Hours · Minutes · Seconds**; kullanılmayan birimler soluk gri, kullanılanlar ince (light) büyük rakam + küçük birim adı ("18 Days 7 Hours 20 Minutes 10 Seconds").
  - **Birim kaydırıcısı:** sütunların altında ince çizgi üstünde beyaz hap tutamak; kaydırınca en büyük birim değişir: i2'de "102,680 Minutes 4 Seconds" (tutamak Minutes'ta), i3'te "16,013,999 Seconds" (tutamak en sağda). Yani aynı süre "x gün" ya da "x saniye" diye gösterilebiliyor.
  - **Etkinlik düzenleyici (i4):** açık gri sayfa (#F3F2F8), beyaz 20 px köşeli gruplar, mavi çizgi simgeler. Alanlar: Title; Date (gri hap "13 Aug 2026"), Time (hap "00:00"), **Time Zone** ("Default (System) Time Zo…" mavi, taşıyor); Appearance: **Wallpaper** (sağda yuvarlak küçük resim), **"Timer with Dark Background Colour"** (yeşil anahtar), **Timer Position** (Middle; üst/orta/alt), **Celebration** (None; konfeti, havai fişek, balon, yıldız, lazer — açıklama); **Countdown Only**, **Repeat Annually**; **Favourite** (yarım görünüyor).
  - **Duvar kâğıdı seçici (i5):** "Choose Wallpaper", 4 sütun kare küçük resim, 15 hazır fotoğraf: kumsal, havai fişek, bulutlu gök, yeşil yaprak dokusu, doğum günü mumları, konser kalabalığı, renkli bokeh, çakıl + tahta, kırmızı çiçek, mavi bokeh, sonbahar yaprakları, kar, mezuniyet keplerini atanlar, gül + nikâh yüzükleri, oyuncak fil (bebek). Sağ üstte "+" (kendi fotoğrafın).
  - **Liste (i6):** "Events" büyük başlık, iki bölüm **"Countdown"** ve **"Count Up"** (geçmiş etkinlikler; burada "No Events"), her satırda yuvarlak fotoğraf, başlık, tarih, sağda "19 days"; üstte Edit, ☆ (favoriler), ↑↓ (sıralama), ×; altta yüzen arama hapı "Search Events".
- Widget (i7): orta boy, açık camgöbeğinden leylağa gradyan zemin (#8EEAF9 üstte), her etkinlikte yuvarlak fotoğraf + ad + kalın "77 days" + gri tarih; 3 etkinlik ızgara. Açıklamada küçük/orta/büyük widget ve Apple Watch.
- Tasarım: zemin etkinliğin fotoğrafı; sistem yazı tipi (SF), sayılar ince; düğmeler yuvarlak cam (iOS 26 "Liquid Glass"). Açık sayfalar #F3F2F8.
- Hatalar: düzenleyicide "Default (System) Time Zo…" taşıyor (kesilmiş).
- Bizde olmayan / farklı:
  - **Etkinlik başına tam ekran görünüm** (fotoğraf zemin, cam sayaç kutusu, kaydırarak etkinlik değiştirme) — yok: bizde yalnız liste kartı (`mobile/ios/App/Sources/Countdowns.swift` `CountdownsView`, Android `Countdowns.kt` satır 181–215; fotoğraf kartın zemini) — O — rakipte ücretsiz.
  - **Hazır duvar kâğıdı kitaplığı** (15 konu fotoğrafı: doğum günü, düğün, mezuniyet, tatil, yılbaşı, bebek) — yok: yalnız kendi fotoğrafın (`CountdownItem.photo`) — K (telifsiz fotoğraf ya da üretilmiş zemin gerekir) — rakipte ücretsiz.
  - **Birim kaydırıcısı** (aynı süreyi yıl…saniye arasında gösterme; 6 sütun) — yok: bizde "N gün kaldı", son 24 saatte SS:DD:ss (`countdownText`, `Countdowns.swift` satır 77–84) — mevcut 2. analiz #73'e ek ayrıntı (kaydırıcı biçimi) — K — rakipte ücretsiz.
  - **Sıfıra inince kutlama animasyonu** (konfeti, havai fişek, balon, yıldız, lazer) — yok (`grep -i confetti\|celebrat` boş) — K — ücretsiz.
  - **Etkinlik başına saat dilimi** — yok (`CountdownItem` alanları: id, title, emoji, target, yearly, remind, photo) — K — ücretsiz.
  - **İleri sayım bölümü ("Count Up")**, favori, sıralama, arama — yok (bizde tek sıralama `sortedBy effective`) — mevcut #73'e ek ayrıntı — K.
  - Apple Watch'ta geri sayım — yok (`mobile/ios/Watch/Sources/WatchApp.swift` yalnız vakitler) — O — ücretsiz.
  - Çok etkinlikli (3'lü ızgara) widget — yok: iOS geri sayım widget'ı yalnız `.systemSmall`, tek etkinlik (`mobile/ios/Widget/Sources/MoreWidgets.swift` satır 224–252) — K.
  - Alternatif uygulama simgeleri — mevcut #13.

## Countdown (App Store 1403367428; ABD 4,81 / 190.521 puan; TR 4,63 / 500; Find Appiness LLC; sürüm 7.5.6, 29 Ekim 2026; 28 dil, Türkçe var)
- Ücret modeli: ücretsiz + reklamlı; **tek seferlik Premium**: "Countdown Widget" ₺104,99, "Countdown Widget Offer" ₺73,99 (indirimli). Açıklama: "One purchase. No subscriptions." Premium'da: bütün widget'lar, kilit ekranı dikdörtgen widget, StandBy, özel hatırlatmalar, **sessizde de çalan alarm (iOS 26.1+, AlarmKit)**, takvimden otomatik içe aktarma, bütün sayaç biçimleri, reklamsız. Gizlilik notu: "analitiğimiz başlık, tarih ve notları görmez".
- İncelenen görüntü: 10 iPhone (hepsi açıldı).
- İlk açılış / ödeme: görülmedi. Mağaza görselleri yeşilden maviye gradyan zemin, beyaz iki satır başlık (ince + kalın sözcük karışık: "Unlimited **countdowns**"), "* Premium feature" dipnotu.
- Ana ekran ve gezinme: tek liste "Countdowns" (büyük kalın başlık), sol üstte "Edit" hapı, sağ üstte takvimden ekle (takvim + artı) ve paylaş. Sekme çubuğu yok.
- Öne çıkan ekranlar:
  - **Renkli satır kartları (i1, i6, i8, i10):** her geri sayım **tam doygun renkli** (mor, camgöbeği, turuncu, mavi, kırmızı, kahverengi, yeşil) ~12 px köşeli şerit. Solda büyük emoji (köşesinde küçük ↻ tekrar rozeti), beyaz kalın başlık, altında tarih + "· Turns 30" (kaçıncı yaş) ya da "· 7 years"; sağda aynı rengin koyu tonunda ayrı bölme: büyük kalın "7" + "days left". Doğum günleri listesinde renkler kırmızıdan yeşile **gökkuşağı sırasıyla** diziliyor (i6).
  - **Etiket çipleri (i6, i8, i10):** başlığın altında hap çipler: "🏷 All", "🎂 Birthdays", "🎉 Holidays", "🏝 Vacation", "🎓 School", mavi "⚙ Manage Tags"; satırda tarih yanında etiketin küçük yuvarlak simgesi.
  - **Ayrıntı ekranı (i4):** bütün ekran etkinliğin rengi (mavi #2167D9 civarı), ortada büyük emoji ✈️, başlık "Vacation", tarih; altında koyu yarı saydam iki kutu: **"1 month · 20 days"** (dar, yoğun yazı tipi: Bebas/DIN benzeri sıkışık kalın) ve **"01 : 21 : 07 hour min sec"**. Üstte geri, paylaş, "Edit", "…" cam haplar.
  - **Widget'lar (i2):** küçük dolu renkli kare (tarih + "Turns 30" sağ üstte, emoji, ad, altta koyu bant "in 43 days"), siyah liste widget'ları (sol kenarda renkli dikey çizgi, emoji köşesinde yaş rozeti "65", "in 49 days"), orta boy liste (sağda "7 days left"). Premium.
  - **Kilit ekranı (i3, i5):** satır "✈ Vacation in 24 days / Sat, 1 Nov 2025", dairesel "♥ in 7 days", "🎂 Today ✓". Bildirim: "1 wk until Anniversary".
  - **Takvimden otomatik içe aktarma (i7):** "Import Events" sayfası, "Select Events | Auto Import" bölütlü seçici; takvim başına anahtar (Birthdays 4 events, US Holidays 45 events) ve "Assign Tag". Premium.
  - **Paylaşma (i9):** birden çok etkinlik seçip "Share (3)"; arama + "Filter" (etiket). Açıklama: **"Send a link so friends and family can count down with you"** (bağlantıyla paylaşım; alıcı uygulamada açar).
  - Açıklamada ayrıca: kişiler uygulamasından doğum günlerini içe aktarma, **yalnız iş günlerini sayma**, gün/hafta/ay/yıl ya da "her iki haftada bir", "ayın son cuması" gibi tekrar, notlar, arşiv (geçenleri kendiliğinden arşivleme), çoğaltma, çoklu düzenleme, Siri ve Kısayollar, takvime ekleme.
- Tasarım: liste zemini sistem açık grisi, kartlar tam doygun renk; rakamlar kalın yuvarlak (SF Rounded benzeri); ayrıntı ekranında sıkışık dar rakam. Mağaza zemini yeşil→mavi gradyan.
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - **Renk başına kart** (dolu renkli satır, sağda koyu ton bölme) — yok: bizim kartımız beyaz, yalnız sayı kiremit renkli (`tr-geri-sayimlar.png`; `Countdowns.kt` satır 196–208 `Theme.c.surface`) — mevcut #73'e ek ayrıntı (görsel biçim) — K — rakipte ücretsiz.
  - **Etiketler ve etiket çipleriyle süzme** — yok (`CountdownItem` alanlarında etiket yok) — K — rakipte ücretsiz (yönetim), widget'ta süzme Premium.
  - **Kaçıncı yaş / kaçıncı yıl ("Turns 30", "7 years")** — mevcut #73.
  - **Kişilerden doğum günü içe aktarma** — yok (yalnız takvimden tek etkinlik seçme: `countdowns.fromCalendar`) — K — ücretsiz.
  - **Takvimden otomatik içe aktarma** (seçili takvimin yeni etkinlikleri kendiliğinden geri sayım olur) — yok — O — rakipte Premium.
  - **Bağlantıyla paylaşma** (alıcı aynı geri sayımı kendi uygulamasına ekler) — yok: bizde yalnız görsel kart paylaşımı (`ShareCards.swift`, `tr-paylas-geri-sayim.png`) — K (sitede `?t=&n=` bağlantısı zaten var: `/geri-sayim/?t=…&n=…`; uygulama bu bağlantıyı açıp içe aktarabilir) — ücretsiz.
  - **Yalnız iş günü sayma** — yok (tarih hesaplamada iş günü var: `DateCalc.kt`, ama geri sayımda yok) — K — ücretsiz.
  - **Esnek tekrar** (her hafta/ay, iki haftada bir, ayın son cuması) — yok: yalnız `yearly` — K — ücretsiz.
  - **Geri sayıma alarm** (sessizde de çalar, AlarmKit) — yok — K (alarm altyapısı var: `AlarmCenter.swift`) — rakipte Premium.
  - **Kilit ekranı widget'ları (satır, dairesel)** — yok: iOS geri sayım widget'ı yalnız `.systemSmall` (`MoreWidgets.swift` satır 252) — mevcut #14/#73'e ek ayrıntı — K — rakipte satır/dairesel ücretsiz, dikdörtgen Premium.
  - Liste widget'ı (3 etkinlik) — yok (aynı dosya) — K — rakipte Premium.
  - Arşiv, sıralama (sürükle), arama, çoğaltma — yok — mevcut #73.

## Timer+ Countdown & Stopwatch (App Store 391564049; ABD 4,75 / 41.133 puan; TR 4,52 / 63; Minima Software LLC; sürüm 4.0, 29 Ekim 2026; 19 dil, Türkçe var)
- Ücret modeli: ücretsiz + reklamlı; "Pro" çok kademeli (TR fiyatları: ₺10,99, ₺39,99, ₺50,00, ₺125,00, ₺250,00, ₺379,00, ₺499,99, ₺1.499,99, "Timer+ Pro" ₺2.499,99; adlardan abonelik/ömür boyu ayrımı okunamıyor). Pro'da ne kilitli: görselde yazmıyor.
- İncelenen görüntü: 10 iPhone (hepsi açıldı). Mağaza görselleri pastel düz zemin (bej, pembe, leylak, sarı, nane) + sol hizalı kalın siyah iki satır başlık + telefon çerçevesi.
- İlk açılış / ödeme: görülmedi.
- Ana ekran ve gezinme: "All Timers" listesi (i1); altta üç düğme: geçmiş (saat ↺), ortada hap biçimli "All | Active" bölütlü seçici (Active'te mavi rozet "3"), sağda "+". Sol üstte geri (klasör listesi), sağ üstte "…".
- Öne çıkan ekranlar:
  - **Liste (i1):** her zamanlayıcı tam genişlik satır; **çalışan satır koyu gri zemine geçer** (#353A40), duran satır açık (#F8F8F8). Satırda **renkli nokta + ad** (Roast kırmızı, Pasta turuncu, Garlic Bread kahverengi, Rest the Roast mor, Warm Plates mavi), altında çok büyük ince rakam ("11:51"), sağda yuvarlak düğmeler: çalışanda gri × + zamanlayıcının renginde açık tonlu ‖, durmuşta renkli açık tonlu ▶. Takvimden bir satır: "📅 Dinner Starts Sep 28 at 5:30 PM 58:13" (tarihe geri sayım aynı listede).
  - **Tek zamanlayıcı (i2):** siyah zemin (#0E1215), ortada koyu gri kart (#353A40, ince kırmızı çerçeve = zamanlayıcının rengi): ad "Roast", dev ince "11:47", "🕓 4:43 PM" (bitiş saati), zil hapı; kartın altında "End of Timer 4:43 PM". Altta "ELAPSED 2:03:13" + ilerleme çubuğu; düğmeler × / kırmızı ‖ / ±. **± açılır balonu:** kırmızı haplar "+1h +10m +1m / -1h -10m -1m".
  - **Rutin çalışırken (i3, i9):** kart adımın renginde dolu gradyan (Tabata'da mor #E65AF9, Pomodoro'da mavi): "Jumping Jacks 1 of 8 0:19"; altında **sıradaki adımlar listesi** (sol kenarda renkli dikey çizgi, ad + "2 of 8", sağda süre ve **o adımın biteceği saat** "4:38 PM"); altta "ELAPSED 0:01 / REMAINING 3:59" ve ilerleme çubuğu, × ⏪ ‖ ⏩ ±. Sesli duyuru balonu: **"Rest. Up next: Squats."**
  - **Kronometre (i4):** aynı kart düzeni, "Track Laps 6:21 / Lap 4 · 01:09", mavi "Lap" hapı; tur listesi bayrak simgeli (en hızlı yeşil, en yavaş kırmızı bayrak).
  - **Rutin düzenleyici (i6):** "Routine Total: 2:52:00", Name "Classic Sandwich Loaf", Steps: her adımda renkli dikey çizgi + ad + süre (First rise 1:15:00, Shape loaf 1:00:00, Bake 20:00, Tent loaf 17:00), sürükleme tutamağı; "+ Add Step"; "When Routine Ends: None"; "Start Countdown" anahtarı. Yani rutinler yalnız spor değil, **ekmek yapımı gibi günlük işler** için.
  - **Duyuru ayarı (i7):** "When 10 seconds are remaining ▾" alt sayfası; Announcements: Off / **Time Remaining** ("10 seconds remaining") ✓ / **Up Next** ("Up next: Rest") ✓ / **Name** ("Work") / **Halfway** / Switch Sides / Time Elapsed; sürükleyerek sıralama; altta "Sounds (1) | Announcements (2)" hap sekme. Zamanlayıcı seçici iOS tekerleği "0 hrs 1 min 0 secs".
  - **Tarihe geri sayım (i8):** "Vacation 1 mth, 29 days", "📅 November 28, 2026 at 4:34 PM", zil hapı "School Bell (Once)".
  - **Geçmiş (i10):** "Timer History", gün başlıklı liste: "Tabata Workout 4 min · Completed (yeşil) · 6:30–6:34 AM".
- Widget (i5): iki küçük koyu widget (renkli nokta + ad, "44:51", × ve renkli ‖ düğmeleri: **etkileşimli**), orta boy beyaz "Active Timers" listesi (Roast 11:50, Focus ⇄1/4 24:25, Jumping Jacks ⇄1/8 0:19, 📅 Vacation 1 mth, 29 days) ve "Start a timer" bağlantısı.
- Sürüm notları (4.0): **"Urgent" zamanlayıcı: sessizde ve Odak'ta da alarm çalar (iOS 26)**, nota ekleme, **bağlantıyla zamanlayıcı paylaşma** (alıcı uygulamayı kurmadan da deneyebiliyor; bağlantı 7 gün), iPad kenar çubuğu, **iptali geri alma (Undo)**, tur yeniden adlandırma. Açıklama: 99 güne kadar süre, 64 kez tekrar, **belirli saatte başlat ya da belirli saatte bitecek şekilde başlat**, bitince ileri saymaya devam, klasörler, rutinde bloklar (her blok kendi tekrarıyla), "Done'a basınca geç" adımı, şablonlar (Tabata, Pomodoro, EMOM).
- Tasarım: koyu zamanlayıcı ekranı #0E1215 + kart #353A40; rakamlar SF ince (ultralight); her zamanlayıcının kendi rengi (nokta, kart çerçevesi, düğme tonu). Liste açık tema #F8F8F8.
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - **Zamanlayıcı başına renk** (nokta, kart çerçevesi, düğme) — yok (`TimerItem`: id, name, total, end, left; `mobile/android/app/src/main/kotlin/tr/bumba/time/Intervals.kt` satır 67–75) — K — rakipte ücretsiz.
  - **Çalışan zamanlayıcının görsel ayrımı ve ilerlemesi** (koyu satır, ilerleme çubuğu, bitiş saati "🕓 4:43 PM") — yok: bizde çalışan ile duran satır aynı beyaz kart, yalnız düğme simgesi değişiyor; bitiş saati yazmıyor (`tr-zamanlayici.png`; `Intervals.kt` satır 380–406) — K — ücretsiz. (Sitede hazır süre sayfalarında "bitiş saati" satırı var: `data-cd-endsat`; uygulamada yok.)
  - **Çalışırken ±1 sa / ±10 dk / ±1 dk** — kısmen: yalnız "+1 dk" (`Intervals.kt` satır 398) — K — ücretsiz.
  - **Rutinde sıradaki adımlar listesi ve her adımın bitiş saati**, adım başına renk, ⏪/⏩ adım atlama — kısmen (rutin adımları adlı ve süreli; atlama ve sıradaki liste yok: `Intervals.kt` satır 512–514 yalnız adım listesi) — K — ücretsiz.
  - **Sesli duyuru seçenekleri** ("sıradaki: …", "yarısı", "10 saniye kaldı", geçen süre; her biri aç/kapa) — kısmen: bizde evre adı + son 3 sn sayımı, tek anahtar (`HiitConfig.cue`, `tools.voice`) — K — ücretsiz.
  - **Rutinin spor dışı kullanımı** (ekmek, yemek; adımlar saatlerce sürebilir, "Done'a basınca geç") — kısmen: rutinler Aralık sekmesinde antrenman şablonlarıyla (`tr-rutinler.png`) — K — ücretsiz.
  - **Tamamlanan zamanlayıcı geçmişi** (gün gün, başlangıç–bitiş saati, "Completed/Cancelled") — kısmen: bizde son 12 ad + süre, yeniden başlatmak için (`TimerHistory`, `Intervals.kt` satır 130–134) — K — ücretsiz.
  - **Belirli saatte başlayacak / belirli saatte bitecek zamanlayıcı** — yok — K — ücretsiz.
  - **Bitince ileri sayma** (ne kadar geçtiğini gösterme) — yok (bizde "bitti" durumu: `done`) — K — ücretsiz.
  - **Etkileşimli zamanlayıcı widget'ı** (duraklat/iptal) ve **etkin zamanlayıcılar listesi widget'ı** — yok: bizde kronometre ve zikir için etkileşimli widget var, zamanlayıcı için yok (`mobile/ios/Widget/Sources/ToolWidgets.swift`; `widget-kronometre.png`) — O — ücretsiz.
  - **Bağlantıyla zamanlayıcı/rutin paylaşma** — yok — K — ücretsiz.
  - **Klasörler** — yok — K — ücretsiz.
  - **İptali geri alma (Undo)** — yok — K — ücretsiz.
  - "Urgent" (sessizde çalan) — **var** (bizde her zamanlayıcı AlarmKit/AlarmManager ile alarm kurar: CLAUDE.md "Zamanlayıcılar ve aralık antrenmanı"; `AlarmScheduler` `timer-<id>`).

## MultiTimer: Multiple timers (App Store 973421278; ABD 4,74 / 15.657 puan; TR 4,87 / 103; PERSAPPS (PERSEPS), TOO; sürüm 7.9.2, 12 Ekim 2026; 16 dil, Türkçe var)
- Ücret modeli: **reklamsız** ("NO ADS"); "MultiTimer Pro" ₺79,99 / ₺124,99 / ₺349,99 / ₺999,99 (kademeler; abonelik mi ömür boyu mu sayfada yazmıyor). Pro'da ne kilitli: görselde yazmıyor.
- İncelenen görüntü: 10 iPhone (hepsi açıldı). Mağaza zemini mordan pembeye gradyan (#993BE7 üstte), beyaz büyük harf kalın başlık, açık yeşil-gri telefon çerçevesi.
- İlk açılış / ödeme: görülmedi.
- Ana ekran ve gezinme: "MY TIMERS" panosu (i1), siyah zemin (#000), sol üstte ☰, sağ üstte dişli; altta sayfa noktaları (birden çok pano), sol altta bağlantı (paylaş), sağ altta büyüteç (yakınlaştır).
- Öne çıkan ekranlar:
  - **Halka zamanlayıcı ızgarası (i1):** 2 sütun büyük daire; her zamanlayıcı **kendi renginde kalın halka** (yeşil, beyaz-kırmızı-yeşil ölçekli, açık yeşil tik çizgili, mavi, sarı, camgöbeği, kırmızı); halka kalan süreyi yay olarak gösteriyor (yeşil 04:20 halkası açık). Ortada ince rakam (çalışmayanlarda gri), ince çizgi, altında ad ya da emoji ("🫖 / ☕", "HOMEMADE BREAD", "DOING LAUNDRY", "Meditation"); sağ üst köşede küçük yuvarlak simge rozeti (çay fincanı, yin-yang, fidan). Biten zamanlayıcı gri halka + kırmızı **"COMPLETED"**, üstte geçen süre "03:12". **İleri sayan sayaç: "+ 228 Days without smoking"**. **Analog saat kutusu: "14:01 Local time"** (aynı ızgarada saat).
  - **Özel pano (i8):** "Hockey 🏒 - Club" 3 sütun; yeşil "Green Card 02:00", sarı "Yellow Card 05:00" halkaları (asma kilit simgeli), ortada saat "02:53 PM 07", altta büyük mavi halka "17:00" ve "End Period", yeşil START / kırmızı STOP çerçeveli düğmeler. Yani pano bir **maç/oyun kontrol masası** gibi kurulabiliyor.
  - **Zamanlayıcı türleri (i4):** Countdown timer (sınırsız kendiliğinden tekrar), Quick timer (süre başlarken girilir), **CountUp timer** (ek bildirim ayarlı), Pomodoro, Interval, **Stepped timer** (aralar arası kendiliğinden duraklama), Stopwatch (yakınlaştırarak dönem yakalama), Lap timer; açıklamada ayrıca **Counter** (sayaç), **Clock**, **Buttons**, **Container**.
  - **Aralık düzenleyici (i2):** üstte renk daireleri şeridi (kırmızı, turuncu, sarı, yeşil, camgöbeği seçili, mavi, pembe, renk tekerleği), simge (meditasyon) + ad "TABATA"; "UNITS OF TIME DD:HH:MM:SS", "ALERT Alarm", "AUTOCOMPLETION"; adımlar renkli çerçeveli haplar: "Get ready 5s" (camgöbeği), grup "3 ×" içinde "Work 30s" (sarı) + "Rest 10s" (kırmızı); Ungroup / Copy / Delete; "SIRI SHORTCUTS".
  - **Pomodoro düzenleyici (i3):** simge + "Reading", WORK 25m, BREAK 5m, LONG BREAK CYCLE 4, LONG BREAK 15m, WORK END ALARM / BREAK END ALARM ayrı, HISTORY.
  - **Süre girişi (i5, i7):** hesap makinesi tuş takımı (7-8-9 / 4-5-6 / 1-2-3 / 0 ⌫) + sağda birim tuşları **Days / Hours / Minutes / Seconds**; ekranda "3650d 12h 6m 30s"; "By Time | By Date" sekmeleri; "AUTO-REPEAT" anahtarı.
  - **Gecikmeli başlatma (i6):** "DELAYED START": Wait time | **Start date** | **End date** sekmeleri, iOS tekerleği (Today 15 15), START.
  - **Geçmiş (i10):** "Daily timer" günlere bölünmüş tablo: STARTED saat + TIME süre ("* 29m 1s" yıldız = yarıda kesilen), gün TOTAL; paylaş ve Edit.
- Widget (i9): beyaz zeminli widget'larda renkli halkalar (4'lü yatay, 1 büyük, 2×2), her birinde süre ve ad; uygulama simgesinde kırmızı rozet "4" (çalışan sayısı).
- Açıklamada: Apple Watch uygulaması, iCloud eşitleme, **web bağlantısıyla zamanlayıcı paylaşma** (arkadaşlar süren zamanlayıcıyı izler), x-callback-URL, panoları başka cihaza aktarma.
- Tasarım: saf siyah zemin, düzenleyici koyu gri (#262626) gruplu liste, büyük harf etiketler, mavi iOS bağlantı rengi; zamanlayıcılarda doygun neon renkler. Rakamlar ince, dar (DIN Condensed benzeri).
- Hatalar: mağaza başlığı "FLEXIBLE ARRAGEMENT" (yazım hatası, "ARRANGEMENT"). Geçmiş ekranı tarihleri 2022 (eski görüntü).
- Bizde olmayan / farklı:
  - **Halka (dairesel ilerleme) görselleştirme** ve renkli zamanlayıcı ızgarası — yok: bizde düz satır listesi (`tr-zamanlayici.png`; `Intervals.kt` satır 380–406) — O — rakipte ücretsiz. → ayrı görsel iş (aşağıda rapor).
  - **"… gündür" ileri sayaç** (sigarayı bırakalı 228 gün) — yok (geri sayımlarda geçmiş tarih "bitti"; mevcut #73 ileri sayımı) — mevcut #73'e ek ayrıntı (ayrı tür olarak "kaç gündür" sayacı) — K.
  - **Birden çok pano / ızgara düzeni / panoyu tam ekran** — yok — O — ücretsiz.
  - **Sayaç (Counter, artı/eksi sayma)** — kısmen: zikirmatik var (`Religion.kt`/`Religion.swift` zikirmatik) ama genel amaçlı sayaç değil — K — ücretsiz.
  - **Hesap makinesi tuş takımıyla süre girişi** (gün/saat/dk/sn birim tuşları) — yok: bizde ok düğmeli üç sütun (`tr-zamanlayici.png`) — K — ücretsiz.
  - **Gecikmeli başlatma** (bekleme süresi, başlangıç tarihi, bitiş tarihi) — yok (Timer+ ile aynı iş) — K.
  - **Kendiliğinden tekrar eden zamanlayıcı** (biter bitmez yeniden başlar; ilaç, çay) — yok — K — ücretsiz.
  - **Uygulama simgesinde çalışan zamanlayıcı sayısı rozeti** — yok — K — ücretsiz.
  - **Halkalı widget'lar** (4'lü yatay, 2×2) — yok: zamanlayıcı widget'ı yok (`mobile/ios/Widget/Sources/` dosyalarında timer widget'ı yok; yalnız Live Activity) — O.
  - Pomodoro'da çalışma sonu ve mola sonu için ayrı alarm sesi — kısmen: uzun mola bizde var (`PomodoroScreen.kt` satır 52 `pomo.long`, satır 98), aşama başına ses seçimi yok — K.
  - Not: pomodoro, zamanlayıcı ve geri sayım ekranlarımızın hiçbirinde halka ya da ilerleme çizimi yok (`grep drawArc\|CircularProgress\|LinearProgress PomodoroScreen.kt Intervals.kt TimerScreen.kt Countdowns.kt` boş; HIIT'te yalnız ince çubuk).

## Interval Timer □ HIIT Timer (App Store 1124297113; ABD 4,84 / 87.414 puan; TR 4,82 / 273; Float Tech, LLC; sürüm 26.3.0, 26 Mart 2026; 22 dil, Türkçe var)
- Ücret modeli: reklamsız; **Premium Pass** aylık abonelik (₺12,99 ve ₺69,99 kademeleri; ücretsiz deneme ile başlar), **Lifetime Pass** ₺799,99 / ₺999,99, "Premium Forever Promo" ₺1.499,99. Pro'da ne kilitli: görselde yazmıyor.
- İncelenen görüntü: 5 iPhone (hepsi açıldı; eski iPhone çerçevesi, "No SIM").
- İlk açılış / ödeme: görülmedi.
- Ana ekran (i1): üst yarı **turuncudan kırmızıya gradyan** (#FC5A55 → #F32152), ortada beyaz kalın "04:50" (toplam süre), **beyaz zeminde ortadan çıkıntı yapan yuvarlak ▶ düğmesi** (beyaz daire, kırmızı üçgen). Alt yarı beyaz; beş ayar satırı **her biri kendi açık tonunda dolu hap** (~14 px köşe): Work 00:20 (açık yeşil #E7FBF0, yeşil yazı), Rest 00:10 (açık pembe, kırmızı), Exercises 5 (gri), Rounds 2X (açık mavi, mavi), Round Reset 00:10 (açık sarı, sarı); solda ince çizgi simge (▶, ‖, şimşek, döngü, saat).
- Öne çıkan ekranlar:
  - **Dikey cetvel seçici (i2):** ayara dokununca o rengin tam ekran zemini (Rounds mavi #657CEC, Rest kırmızı, Work yeşil); üstte "2X ✎", ortada dikey ince çizgili **cetvel**, solda ölçek (1X, 5X, … 25X; 0–60 sn), beyaz hap tutamak sürüklenir. Sayfalar yana kaydırılarak (Rest → Rounds → Work) geçiliyor.
  - **Çalışma ekranı (i3):** bütün ekran evrenin rengi (Work yeşil #56D495, Rest kırmızı, Get Ready sarı-turuncu); üstte iki ince çerçeveli satır "Exercise ●○" ve "Round ●" (ilerleme noktaları); ortada **büyük ince beyaz halka**, kalan kısım parlak beyaz yay, içinde "00:07" ve "WORK"; altta beyaz yuvarlak ‖ düğmesi, solda ses düğmesi; en altta "🕓 Time Remaining: 00:37".
  - **Bitiş ekranı (i5):** kırmızı gradyan üst yarı, **konfeti deseni**, ortada iç içe saydam halkalı beyaz daire içinde kırmızı ✓, "Congratulations!" ve "Workout Complete"; altta üç istatistik (yeşil şimşek "1 Workout streak", kırmızı kalp "1 Total workouts completed", mavi kronometre "00:01 Total workout time"); "Remind me to workout at the same time tomorrow" anahtarı; "Show History" (açık pembe) ve "Done" (kırmızı dolu) düğmeleri.
  - **Geçmiş (i4):** kırmızı başlık çubuğu "History", beyaz gölgeli kart "📅 Today 50 s"; Work 20 s · Rest 10 s · Exercises 2 · Rounds 1 (kırmızı rakamlar).
- Açıklamada: müziği kesmeden sesli ya da bip rehber, uygulamadan çıkınca da duyuru, geçmişten antrenmanı yeniden başlatma.
- Tasarım: evre başına tam ekran doygun renk (yeşil çalış, kırmızı dinlen, sarı hazırlan), beyaz kalın yuvarlak sans (SF), büyük beyaz yuvarlak düğmeler, pastel hap ayar satırları.
- Hatalar: açıklamada "Premium Pass beings with" (yazım hatası, "begins").
- Bizde olmayan / farklı:
  - **Evre rengiyle dolan tam ekran ve büyük ilerleme halkası** — kısmen: bizim HIIT ekranı koyu zemin (#11141A), evre adı yeşil yazı, ince yeşil çubuk (`tr-hiit.png`); halka ve evreye göre zemin rengi yok — K — rakipte ücretsiz. → görsel iş (rapor).
  - **Egzersiz/tur ilerleme noktaları** — yok (bizde "Tur 1/8" metni) — K.
  - **Bitiş kutlama ekranı** (konfeti, seri, toplam antrenman, toplam süre) — yok: bizde bitişte alarm çalar (`HiitService`) ve Sağlık'a yazar; özet ekranı yok — K — ücretsiz. (Seri: bizde pomodoroda var, antrenmanda yok.)
  - **"Yarın aynı saatte hatırlat"** — yok — K — ücretsiz.
  - **Antrenman geçmişi ve istatistik** (gün başına çalışma/dinlenme/egzersiz/tur toplamı, geçmişten yeniden başlatma) — yok (yalnız `hiit.healthSaved` ve Sağlık'a kayıt; `Health.kt`) — K — ücretsiz.
  - **Cetvel/kaydırıcıyla süre seçimi** — yok (bizde − / + adım düğmeleri: `Intervals.kt` satır 521–524 `Stepper`) — K.
  - Müzikle birlikte sesli duyuru, uygulama dışında da sürme — **var** (`HiitService`, iOS `HiitRunner`; CLAUDE.md "Zamanlayıcılar ve aralık antrenmanı").

## Seconds Interval Timer (App Store 475816966; ABD 4,68 / 23.795 puan; TR 4,33 / 55; Runloop Ltd; sürüm 26.1, 29 Ekim 2026; 10 dil, Türkçe yok)
- Ücret modeli: reklamsız; tek satın alma **"Advanced Version" ₺399,99** (Seconds Pro). Ücretsizde ne kilitli: görselde yazmıyor.
- İncelenen görüntü: 6 iPhone (hepsi açıldı; görseller eski: "Carrier", ana düğmeli iPhone; sürüm 26.0 notunda "uygulamanın çoğu yeniden yapıldı, artık hep koyu, turuncu vurgu" deniyor, yani mağaza görselleri güncel değil).
- İlk açılış / ödeme: görülmedi (sürüm notu yeni "welcome screens" diyor).
- Ana ekran ve gezinme: alt sekme çubuğu: Intervals / Countdown / Stopwatch (kırmızı seçili simge). "My Timers" (i6) koyu liste: kırmızı yuvarlak simgeler (klasör "Sample Timers Items: 6", "Seconds Demo Custom Timer (0:39)", "7 Minute Workout Circuit Timer (7:50)", "Tabata Timer (13:30)"), kırmızı kalem; altta "Play All".
- Öne çıkan ekranlar:
  - **Çalışan aralık (i1, i2):** ekranın üst üçte ikisi **neon yeşil** (#8AFF34), alt üçte biri **kırmızı-turuncu** (#FF4538): üstte dev ince siyah "00:28", altında üç sütun ELAPSED / INTERVAL 1/6 / REMAINING 15:28, "CURRENT INTERVAL High Intensity 0:30"; alt blokta **"UP NEXT Low Intensity 1:30"**. Salonun öbür ucundan okunacak kadar büyük; dikey ve yatay.
  - **Hızlı geri sayım (i3):** koyu (#1A1A1A), üstte dev "00:28", altında 2×3 gri kare düğmeler (#343434): 00:30, 01:00, 01:30, 02:00, 02:30 ve kırmızı Reset.
  - **Kronometre (i4):** "01:51.51", tur satırları hap: en yavaş tur kırmızı dolu (Lap 4), en hızlı neon yeşil dolu (Lap 1), diğerleri gri; altta Reset / yeşil Start büyük kare düğmeler.
  - **Devre (circuit) düzenleyici (i5):** başlıkta zamanlayıcının **renkli kesikli şerit önizlemesi** (her aralık rengiyle, "Tabata Timer (13:30)"); Name, Number of Sets 8, **APPLE HEALTH Activity: Not Set**, EXERCISES: Squats (camgöbeği hap 0:20), Push-Ups (yeşil hap 0:20), Sit-Ups; "+ Add Exercise", Cut/Copy.
- Apple Watch (i2–i6): saatte bağımsız çalışır; yeşil/kırmızı iki blok "0:28 High Intensity / 1:30 Low Intensity", altında "♥60 • 1/6 • 15:28" (nabız); saatte hızlı geri sayım düğmeleri, kronometre, şablon seçici (renkli yay "STYLE Tabata Total Time 5:50").
- Açıklamada: aralık başına müzik (çalma listesi), duyurular müziği kısıp geri açar, birden çok zamanlayıcıyı zincirleme (ısınma → ana set → soğuma), **kilit düğmesi** (yanlışlıkla dokunmayı engeller), nabız monitörüyle kalori tahmini, Sağlık'a antrenman + farkındalık kaydı, paylaşma.
- Tasarım: koyu gri zemin, kırmızı vurgu (yeni sürümde turuncu); çalışırken tam doygun neon renk blokları; ince sistem rakamları.
- Hatalar: mağaza görselleri eski sürümden (uygulama artık hep koyu ve turuncu; görseller kırmızı).
- Bizde olmayan / farklı:
  - **"Sıradaki" evrenin ekranda gösterilmesi** (UP NEXT bloğu, adıyla ve süresiyle) — yok: bizde yalnız şu anki evre (`tr-hiit.png`) — K — ücretsiz.
  - **Evre renginde dev tam ekran** (uzaktan okunur) ve aralık başına renk — yok (bizde koyu zemin, küçük renkli evre adı) — mevcut görsel işe ek (Interval Timer'la aynı) — K.
  - **Ekran kilidi düğmesi** (antrenmanda yanlış dokunmayı engeller) — yok — K — ücretsiz.
  - **Saatte aralık antrenmanı** (Apple Watch'ta bağımsız, nabızla) — yok: saat uygulamamız yalnız vakitler (`mobile/ios/Watch/Sources/WatchApp.swift`; Wear `WearActivity.kt`) — B — rakipte ücretli sürümde.
  - **Hızlı süre ızgarası** (tek dokunuşla 30 sn…2:30) — kısmen: bizde hap biçimli hazır süreler var (1, 3, 5, 10, 15, 25 dk: `tr-zamanlayici.png`) ama hemen başlatmıyor, önce "Başlat" — K.
  - **Kronometrede en hızlı/en yavaş turu dolu renkle** — kısmen: bizde "En iyi"/"En yavaş" yeşil/kırmızı yazı etiketi (`tr-kronometreler.png`) — var sayılır (küçük görsel fark).
  - **Aralık başına müzik** — yok — O — rakipte ücretli.
  - Sağlık'a kayıt — **var** (`Health.kt`, `Health.swift`; CLAUDE.md #84).

## Timer & stopwatch - Timeglass (App Store 1138584627; ABD 4,69 / 1.450 puan; TR 4,70 / 10; COSMIC TEAPOT PTY. LTD.; sürüm 1.2.1, 2 Temmuz 2020; güncellenmiyor; yalnız İngilizce)
- Ücret modeli: reklamsız; tek satın alma **"Upgrade Timeglass" ₺149,99**. Ücretsizde ne kilitli: görselde yazmıyor.
- İncelenen görüntü: 6 iPhone (hepsi açıldı). Apple "Apps We Love — Best of 2016" defne dallı rozet (i2).
- İlk açılış / ödeme: görülmedi.
- Ana ekran ve gezinme: "All Timers" (i1, i2): **eğik açılı (izometrik) yüzen kartlar**, gölgeli; her kartta **çizgi simge** (diş "Brush & floss", fırın "Roast beef", şınav "Workout", kronometre "Stopwatch"), ince harfli ad, süre "1h 30m 00s" (rakam büyük, birim küçük harf, ince yazı tipi: Avenir Next Ultra Light benzeri), sağ altta yarı saydam beyaz daire içinde ▶. Zemin mavi-camgöbeği gradyan (#4B7CD4 civarı); "Edit", "+".
- Öne çıkan ekranlar:
  - **Etkin zamanlayıcılar (i3):** yeşil gradyan zemin (#5A9E38 → #2E9C60), beyaz kartlar (~14 px köşe): "'7 Minute' workout 6m 52s", altında **adım ilerleme noktaları** (10 nokta, ilki dolu yeşil), şu anki adım "Jumping jacks 22s" ve sağda **yeşil kalın ilerleme halkası** (kalan yay); "Roast beef 1h 24m 04s" yanında küçük halka + açık yeşil yuvarlak ‖ düğmesi.
  - **Adımlı zamanlayıcı düzenleyici (i4):** mor gradyan tam ekran (#A62FD1 civarı) form: "Morning exercises", "Four steps": 1. Star jumps 1m 00s, 2. Push ups 45s, 3. Crunches, 4. Leg raises; "Add a step" / "Copy steps" çerçeveli düğmeler; Icon, "Speak step names Off".
  - **Adım adlarını söyle (i5):** turuncu sayfa: "At step start" ✓, "At step end"; not: "Replaces tones with spoken step names. Available for timers of three hours or less."
  - **Simge seçici (i6):** kategorili beyaz çizgi simgeler: Exercise & fitness (yürüyüş, koşu, bisiklet, ağırlık, şınav…), Cooking & food (buzdolabı, fırın, ocak, alev, kar tanesi, kahve, çatal-bıçak, kek, pasta, kurabiye, yumurta, et), Home & routine (ev, ekmek, duş, diş, çamaşır, süpürge, temizlik, damla, sulama kabı, pati, köpek kulübesi), Hobbies & study; seçili simge yarı saydam kare içinde. Her zamanlayıcı kendi **renk temasıyla** (yeşil, mavi, mor, turuncu gradyan).
- Tasarım: tam ekran doygun gradyanlar (tema başına), ince ve geniş harf aralıklı yazı (Avenir Next Ultra Light), beyaz çizgi simgeler; kartlar gölgeli ve hafif kalın kenarlı.
- Hatalar: görülmedi (uygulama 2020'den beri güncellenmiyor).
- Bizde olmayan / farklı:
  - **Zamanlayıcıya simge** (kategorili çizgi simge kitaplığı: mutfak, ev, spor, ders) — yok (bizde zamanlayıcıda yalnız ad; geri sayımlarda 16 emoji: `CountdownItem.emojis`) — K — rakipte ücretsiz.
  - **Zamanlayıcı başına renk teması** (gradyan) — yok — mevcut "zamanlayıcı rengi" işi (Timer+) ile aynı — K.
  - **Kartta küçük ilerleme halkası** ve adım ilerleme noktaları — yok (bkz. görsel iş) — K.
  - **Adımlı zamanlayıcı genel amaçlı** (sabah egzersizi, diş fırçalama) ve "adımın başında/sonunda adını söyle" ayrı anahtarlar — kısmen: rutinler + sesli duyuru var (`Intervals.kt` rutinler, `HiitConfig.cue`) — K.

## Visual Countdown Timer (App Store 541364004; ABD 4,56 / 41.383 puan; TR 5,0 / 4; Fehners Software LLP; sürüm 4.6.2, 4 Temmuz 2026; 3 dil, Türkçe yok) — çocuklar için görsel zamanlayıcı
- Ücret modeli: ücretsiz + reklamlı; **Remove Ads ₺99,99**, **Premium Upgrade ₺199,99**, resim paketleri ₺49,99'ar (Cartoon, Animal, Transport, Ocean Pack), **Mask Maker ₺49,99**. Tek seferlik.
- İncelenen görüntü: 7 iPhone (hepsi açıldı; eski iPhone 8 çerçevesi).
- İlk açılış / ödeme: görülmedi.
- Ana ekran (i1): gökyüzü fotoğrafı zemin (mavi, bulutlu); ortada **açık gri plastik oyuncak gövdeli zamanlayıcı** (yuvarlak köşeli dikey gövde, üstte büyük daire pencere); dairede **kalan süre yeşil pasta dilimi** (#47B804 civarı), geçen süre kadar **altındaki resim açılıyor** (sarı lastik ördek). Altta sarı yuvarlak "↺10" düğmeleri (−10 / +10 sn ya da dk), ortada **mavi büyük yarım daire ikili düğme ‖ | ■**, sağ altta pembe dişli.
- Öne çıkan ekranlar:
  - **Resim kitaplığı (i2, i7):** "Choose Picture": Picture Packs | Custom | Masks sekmeleri; yuvarlak resimler: Random ??, ördek (seçili, mavi ✓), eski araba, davul, helikopter, denizanası, robot, telefon, kamyon, balık, sevimli canavarlar. Dilim rengi resme göre (canavarda kırmızı-turuncu, pandada kırmızı).
  - **Maske (i3):** çocuğun yüz fotoğrafı yarış pilotu kaskının içine yerleşiyor (dama bayrak, kupa).
  - **Temalar (i5):** şeker (pembe-gökkuşağı, donut "Start" düğmesi, kiraz "10" düğmeleri), uzay (gezegen "Start", mor), korsan gemisi (pirinç lombar çerçeve, dümen "Start", ahşap zemin), orman (yapraklar, leopar, kahverengi "Start"). Süre seçici dairenin içinde iOS tekerleği "1 mins 20 secs".
  - **Ayarlar (i7):** Picture To Reveal, Theme (Day), **Child Mode** ("Hide buttons when the timer is running"), **Slowly Reveal Picture**, **Play Ticking Noise**, **Sneaky Pause** (duraklatınca tik-tak sürer), "Hey Siri, Start Countdown Timer" (Add to Siri), Preset Timers.
  - **Ana ekran widget'ı (i6):** üç yuvarlak resimli hazır zamanlayıcı: "15mins Read book", "5mins Time Out", "3mins Brush Teeth"; dokununca başlar.
  - Tanıtım (i4): "Great for Motivating: Getting dressed, Brushing teeth, Getting shoes on, Sharing toys, Tidying up, Homework time".
- Tasarım: el yazısı benzeri yuvarlak çocuk yazı tipi (mağaza), plastik oyuncak görünümü (gölgeli, kabartmalı), doygun ana renkler.
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - **Görsel pasta zamanlayıcı ("Time Timer" tarzı: kalan süre renkli dilim, sayı okumayı bilmeyen çocuk ve dikkat güçlüğü olanlar için)** — yok (zamanlayıcı yalnız rakam: `tr-zamanlayici.png`; `grep drawArc` boş) — K/O — rakipte ücretsiz (resim paketleri ücretli). Bizde "Mevsimler"in çocuk bölümü var (CLAUDE.md "Mevsimler çocuklara yönelik"); araçlarda çocuk modu yok.
  - **Çocuk modu** (sayarken düğmeleri gizle) — yok — K.
  - **Tik-tak sesi** — mevcut #10 (uygulamada saniye tık sesi yok); zamanlayıcıda da yok — K.
  - **Hazır adlı zamanlayıcıları ana ekrandan başlatan widget** — yok (zamanlayıcı widget'ı yok; "Son kullanılanlar" yalnız uygulama içinde: `TimerHistory`) — K.
  - Resim açılması, maske, tema paketleri — yok — O — rakipte ücretli; bizde ileride Pro adayı (temalar).

## Stopwatch‰ (App Store 505889167; ABD 4,87 / 32.033 puan; TR 4,62 / 1.132; Tim O's Studios, LLC; sürüm 6.1, 12 Eylül 2026)
- Ücret modeli: ücretsiz + reklamlı ("Contains Advertising"); uygulama içi satın alma yok.
- İncelenen görüntü: 2 iPhone (hepsi açıldı; iPad görüntülerine bakılmadı).
- Ana ekran (i1, i2): tek ekran. Koyu **fırçalanmış/dokulu antrasit zemin** (#201F1D civarı, çapraz ince çizgiler); ortada **dev beyaz rakam "0.0" / "3.5"** (ekran genişliğini dolduran, yalnız saniye ve onda bir); altta iki büyük hap düğme: sol **yeşil parlak gradyanlı "Start"** (çalışınca **kırmızı "Stop"**), sağ gri "Reset"; düğme yazıları küçük büyük harf (small caps, Copperplate benzeri serif), kabartma gölge (eski iOS skeuomorfik).
- Tasarım: tek amaç, iki düğme; tur yok, ayar yok (görüntüde).
- Bizde olmayan / farklı: kronometremizde rakam büyük ve kalın ("01:35.40"), Sıfırla/Devam hap düğmeleri ve turlar var (`tr-kronometreler.png`). Bu rakibin tek farkı **onda bir saniyeye kadar ekranı dolduran dev rakam** ve yalnız iki düğme. Yeni iş değil; ancak bizde kronometre için **tam ekran / yatay dev rakam görünümü** yok (tam ekran yalnız saat sekmesinde: CLAUDE.md "Yatay ekran") — K — ücretsiz.

## Days • Countdown & Widgets (App Store 939368917; ABD 4,76 / 12.745 puan; TR 4,57 / 53; MD Apps LTD; sürüm 3.40, 15 Eylül 2026; 10 dil, Türkçe yok)
- Ücret modeli: reklamsız; **abonelik**: Monthly Plan ₺7,49 / ₺79,99, Yearly Plan ₺50,99 / ₺99,99 / ₺229,99 (kademeler), **Lifetime Pro** ₺699,99 / ₺799,99. Pro'da ne kilitli: görselde yazmıyor.
- İncelenen görüntü: 8 iPhone (hepsi açıldı). Mağaza: beyaz zemin, siyah büyük başlık, "App of the day — Featured on the App store" defne dallı (i2).
- İlk açılış / ödeme: görülmedi.
- Ana ekran ve gezinme: **büyük fotoğraf kartları dikey liste** ("All events" hapı ortada, ☰ solda, + sağda; i2, i8). Kartta tam fotoğraf, altta karartma; üstte geniş harf aralıklı küçük büyük harf **"IN 2 WEEKS, 2 DAYS"**, altında **dar, kalın, sıkışık başlık** (Anton/Bebas benzeri) "Kate's birthday". Altta yüzen hap seçici **"Previous | Upcoming"** (geçmiş ve gelecek etkinlikler).
- Öne çıkan ekranlar:
  - **Tam ekran etkinlik (i4):** bütün ekran fotoğraf, "IN 16 DAYS", dev sıkışık başlık, ince çerçeveli hap içinde "JULY 2, 2024"; üstte × / beyaz "Share" hapı / "…". Etkinlikler arası yana kaydırma (açıklama).
  - **Etkinlik oluştur (i5, i7):** başlık "Vacation" yazınca altta **Unsplash'tan konuya uygun fotoğraf ızgarası** (3 sütun dikey dikdörtgen: kumsal, mavi deniz, kıyı yolu, palmiye, cankurtaran kulübesi, flamingo simit) ya da kamera. Tarih tekerleği, All day, Start/End time, Repeats anahtarı; siyah tam genişlik "Continue".
  - **Paylaşma (i6):** "Share event with your friends" (beş Memoji avatar), siyah hap **"Share link on event"**; altında **kaydırmalı paylaşım kartı şablonları** (8 sayfa noktası; fotoğraf üstünde "2 hours 10 minutes"), "Watermark" anahtarı (filigranı kaldırma — muhtemelen Pro).
  - **Gece modu (i8):** siyah zemin, aynı kartlar.
- Widget (i3): küçük ve orta boy **tam fotoğraflı** widget: "IN 24 DAYS Moving day", "IN 3 DAYS, 12 HOURS Beach gateaway".
- Sürüm notu (3.40): paylaşılan etkinlik yeniden açılınca alıcıdaki kopya güncellenir (tarih ve görsel değişiklikleri gelir).
- Tasarım: beyaz/siyah sade çerçeve, içerik tamamen fotoğraf; tipografi iki katmanlı (küçük geniş aralıklı büyük harf + dar kalın başlık).
- Hatalar: widget görselinde "Beach **gateaway**" (yazım hatası, "getaway").
- Bizde olmayan / farklı:
  - **Konuya göre hazır fotoğraf önerisi** (başlıktan Unsplash araması) — yok (yalnız kendi fotoğrafın: `PhotoFile`) — K/O; Unsplash ağ isteği ve API anahtarı ister (bizde hesap/sunucu yok; Unsplash API anahtarı uygulamaya gömülmeli) — rakipte ücretsiz. Countdown Star'daki hazır kitaplık daha uygun (ağ istemez).
  - **Fotoğraflı widget** — yok: iOS geri sayım widget'ı düz zemin (`MoreWidgets.swift` CountdownWidget), Android `widget-geri-sayim.png` krem kart — K — rakipte ücretsiz (görselde).
  - **Paylaşım kartı şablonları** (birkaç düzen arasında kaydırma, filigran aç/kapa) — kısmen: bizde tek düzen kart, "Saat Tarih · time.bumba.tr" imzası sabit (`tr-paylas-geri-sayim.png`, `ShareCards.kt`) — K — rakipte filigran kaldırma büyük olasılıkla Pro.
  - **Geçmiş/Gelecek hap seçici** — mevcut #73 (ileri sayım, arşiv).
  - Bağlantıyla paylaşıp alıcıda güncellenme — Countdown (Find Appiness) maddesiyle aynı iş.

