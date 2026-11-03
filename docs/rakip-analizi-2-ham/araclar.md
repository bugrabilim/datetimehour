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

