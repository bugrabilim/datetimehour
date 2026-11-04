# Zaman araçları (ikinci dalga, 3 Kasım 2026)

Alan: kronometre, zamanlayıcı (çoklu ve aralık), geri sayım (etkinliğe kalan gün), dünya saati / saat dilimi çevirici, pomodoro / odak, tarih hesaplama. Alarm ve uyku ilk dalgadaydı. Forest, Focus To-Do (`kadran.md`), Countdown Widget ve Time Buddy (`namaz.md`) ilk dalgada incelendi; burada yeniden ele alınmadı.

Görüntüler: `scratchpad/rakip2/araclar/<uygulama>/iN.jpg` (App Store TR vitrini, 600 px genişlik; depoya konmadı).

Bizim karşılaştırdığımız ekranlar (Android, 31 Ekim 2026, `mobile/android/app/build/screens/`): `tr-araclar`, `tr-zamanlayici`, `tr-hiit`, `tr-rutinler`, `tr-geri-sayimlar`, `tr-paylas-geri-sayim`, `tr-pomodoro`, `tr-pomodoro-istatistik`, `tr-kronometreler`, `tr-dunya`, `tr-toplanti-planlayici`, `tr-tarih-hesaplama`, `widget-geri-sayim`, `widget-dunya`, `widget-kronometre`.

**Bizim ekranlarımızın kısa tarifi (kıyas için):** krem zemin (#F7F4EE, ölçüldü), beyaz 16–20 px köşeli kartlar, kiremit vurgusu (#9A3F00, ölçüldü), Roboto. Zamanlayıcı listesi beyaz kartta satır satır "03:05" ince büyük rakam + ‖ / ▶ / "+1 dk" / ×; görsel ilerleme (halka, çubuk) yok. Yeni zamanlayıcı üç sütunlu ok düğmeli seçici ("00 05 00") + hap biçimli hazır süreler. Pomodoro: ortada "Çalışma" + "25:00" düz metin, altında "Tur 1 / 4"; halka ya da görsel yok. Aralık (HIIT): koyu zemin (#11141A, ölçüldü), evre adı yeşil, "00:05", ince yeşil ilerleme çubuğu. Kronometre: "01:35.40" büyük kalın, tur listesi ("En iyi" yeşil, "En yavaş" kırmızı). Geri sayımlarım: emoji + başlık + kiremit renkli "23 gün kaldı" + tarih; fotoğraflı kart koyu gradyan. Geri sayım widget'ı: krem zemin, "Yılbaşı" kiremit başlık, "62 gün kaldı" kalın, tarih. Dünya saatleri widget'ı: dört satır ad + fark + saat.

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

## World Clock Time Zone Widget / "World Time Widget" (App Store 912698955; ABD 4,70 / 30.873 puan; TR 4,65 / 242; Lasmit TLB Ltd; sürüm 2026.15, 28 Ekim 2026; 4 dil, Türkçe yok)
- Ücret modeli: reklamsız ("No ads, no sign up"); **"Premium Club" aboneliği**: Monthly ₺99,99 / ₺299,99, Annual ₺449,99 / ₺899,99 / ₺1.299,99, Lifetime ₺2.499,99 / ₺2.999,99 / ₺4.999,99, "Pro Club - Family Sharing" ₺1.299,99. Ayarlarda "Premium Club — Member" satırı (i6). Ücretsizde ne kilitli: görselde yazmıyor.
- İncelenen görüntü: 10 iPhone (hepsi açıldı). Mağaza zemini düz mavi (#0F6FE6 civarı) + koyu mavi dünya haritası silueti, beyaz kalın yuvarlak başlık (Rubik benzeri).
- İlk açılış / ödeme: görülmedi.
- Ana ekran (i3, i4): "World Clock" başlıklı liste (koyu #121212 ya da açık). Her satır: **sol kenarda renkli dikey çizgi** (yeşil/turuncu/mor: gündüz, akşam, gece durumu), **kişi adı + emoji** ("🏠 Me", " Tim", "🎨 Frida", "🏃 Forrest", "🗽 Liberty", "🇬🇲 Kunta", "🇫🇷 Amélie"; şehir adı yerine kişi), altında küçük gri "PDT -9 hrs, Mon, ☀︎↑ 6:45" (kısaltma, fark, gün, **gün doğumu saati**), sağda dev ince saat "10:36" + üst simge küçük saniye "38" ve "AM". Açık temada saat yerine **dünya haritası desenli analog kadran** (gri harita, siyah akrep/yelkovan, kırmızı saniye). Altta yüzen "⏰" ve "⟳ Calculator" hapları, dişli.
- Öne çıkan ekranlar:
  - **Saat dilimi hesaplayıcı (i5):** liste aynı, seçili saat dilimiyle aynı olanlar vurgulu satır (koyu yeşil); altta cam panel: ×, takvime ekle, hatırlatma ekle, paylaş, "+02:35 ⟲" (şimdiden fark), "CENTRAL EUROPEAN TIME", "10:30 PM" ve "Sep 7, 2026" hapları, − / +; en altta **saat şeridi "12 • 1 • 2 … 11 PM"** üzerinde sürüklenen seçici (scrubber).
  - **Ayarlar (i6):** üstte canlı örnek satır (New York EDT -6 hrs…, "1:37 07 PM"); Premium Club; Design: Clock Style, Clock Themes & Icons; **Desktop StandBy Mode**; Help: What's New, Help & Support.
  - **Tema ayarı (i7):** Light | Dark; "Use Colors: Urban" 18 renk karosu (zemin + nokta rengi çifti), "Your themes": Aurora, Dunes, Harbour (gradyan) ve **"From a photo"**; Font Weight kaydırıcısı (Regular).
  - **Fotoğraftan tema (i8):** "Choose a photo and we'll make a theme from its colours. Your wallpaper, a holiday snap…" — Choose a photo / Paste; önizleme; Night / Save theme.
- Widget'lar (i1, i2, i9): **Liquid Glass** cam widget'lar; dev üst üste "2 / 14 PM" + dikey "LIBERTY"; **gün yayı** (kesikli yarım daire üstünde Güneş noktası, "↑7:16am ↓8:21pm", "8:14 PM"); **sözle saat** "HIRO — It's quarter past three · 3:14 am · tomorrow"; 4 kişilik liste; **"TIME OVERLAP" ızgarası** (EDT/CEST/JST satırları, 24 saat kutucuk: gündüz, mesai, gece, yeni gün çizgisi; "NOW · +12H · +24H"); 3'lü analog (gün + AM/PM etiketli); 4 sütun "MONDAY 11:14 · Tim" (altında renkli gün/gece noktası); büyük **dünya haritası widget'ı** (gece gölgesi, kişiler harita üstünde); kilit ekranı / masaüstü **saniyeli** "7:37:23 GMT+2 MONDAY September 7". En çok 8 saat dilimi tek widget'ta.
- Sürüm notu: saat dilimi kısaltmasıyla ekleme (EST, CET, Pacific; Almanca MEZ), **saat başına 12/24 saat ayarı**, Siri ve Kısayollar, Spotlight. Açıklama: Apple Watch uygulaması ve komplikasyonlar, Mac, iCloud eşitleme, toplantı planlayıcı (sürükleyerek).
- Tasarım: koyu zemin #121212, ince büyük rakam + üst simge saniye; gündüz/gece durumu renkli kenar çizgisiyle; kişi odaklı ("Mum in Sydney").
- Hatalar: mağaza başlığı "World Time Clock **Convertor**" (yazım: "Converter").
- Bizde olmayan / farklı:
  - **Şehir seçimi yalnız 31 şehir** — bizde uygulamada dünya saatlerine yalnız `cities.json`'daki 31 şehir eklenebilir (`mobile/ios/App/Sources/WorldSeasons.swift` satır 8 "sitedeki 31 şehir", satır 112 `City.all`; Android `WorldScreen.kt` satır 173 `City.all`; `assets/cities.json` 31 kayıt). Sitede istenen IANA dilimi eklenebiliyor (`world.js` `tz:`), uygulamada küre için 1960 yerlik `places.json` zaten var. Rakiplerde arama ile binlerce şehir ve kısaltma (EST, CET). **ilk analiz satırı "dünya saatleri" bitti sayılmış ama eksik** — K (places.json'dan arama) — ücretsiz.
  - **Saate kişi adı ve emoji verme** ("Annem — Sidney") — yok (satırda yalnız `city.displayName`: `WorldScreen.kt` satır 126) — K — rakipte ücretsiz.
  - **Gündüz/gece durumu** (renkli kenar çizgisi ya da nokta) ve **satırda gün doğumu saati** — yok (satırda fark ve yaz saati notu var: `WorldScreen.kt` satır 127–136) — K — ücretsiz.
  - **Saat dilimi hesaplayıcıda saat şeridi kaydırıcısı** — kısmen: bizde "Başka bir zamana bak" anahtarı + tarih/saat seçici (`WorldScreen.kt` satır 80–95), sürüklenir şerit yok — mevcut #72'ye ek ayrıntı — K.
  - **"Time overlap" (çakışma ızgarası) widget'ı** — yok (toplantı planlayıcı yalnız uygulamada: `MeetingPlanner.swift`) — O — ücretsiz.
  - **Gün yayı widget'ı** (Güneş'in gökteki yeri) — kısmen: uygulamada Vakitler'de Güneş/Ay yayı var (`SunMoonArc`), widget'ı yok — mevcut #15'e ek ayrıntı (gün doğumu/batımı widget'ı) — K.
  - **Sözle saat widget'ı** ("üçü çeyrek geçiyor") — kısmen: sitede "yazıyla saat" ve konuşan saat var (`client.words`, `/konusan-saat/`), uygulamada widget yok — K — ücretsiz.
  - **Dünya haritası widget'ı** (gece gölgesi ve kişiler) — yok (küre yalnız uygulamada: `GlobeView.swift`) — O — ücretsiz.
  - **Dünya saatleri widget'ında 8 şehir, kilit ekranı dünya saati** — kısmen: iOS widget'ı küçük 2 / orta 4 şehir (`MoreWidgets.swift` satır 135–142), kilit ekranı yok — K.
  - **Saat başına 12/24 biçimi** — mevcut #72 (Time Buddy "Native").
  - **Fotoğraftan tema** (renkleri fotoğraftan çıkarma) — yok (24 hazır tema: `AccentPalette`) — mevcut #78'e ek ayrıntı — K — rakipte büyük olasılıkla Premium.
  - **Apple Watch'ta dünya saatleri ve komplikasyonları** — yok (`mobile/ios/Watch/Sources/WatchApp.swift`) — O.

## World Clock Time Zone Widgets (App Store 956377119; ABD 4,71 / 22.672 puan; TR 4,71 / 213; Overdesigned, LLC; sürüm 1.27.2, 13 Ekim 2026; 12 dil, Türkçe yok)
- Ücret modeli: ücretsiz + reklamlı; **tek seferlik "World Clock Pro" ₺249,99** (reklamları kaldırır, ek özellik açar; "One-time purchase — no subscriptions!").
- İncelenen görüntü: 10 iPhone (hepsi açıldı). Mağaza zemini koyu gri (#2C2C2C), beyaz kalın başlık; "2,000,000+ DOWNLOADS" camgöbeği defne dalı (i8).
- İlk açılış / ödeme: görülmedi.
- Ana ekran (i4, i7, i10): "World Clock" büyük gri başlık; satırda ad (kalın) + gri "Thursday, August 8, -3:00" (gün, tarih, fark), sağda **gök renginde dolu saat kutusu**: gündüz açık mavi gradyan (#D5E8FF → mavi), akşam turuncu-mor (Cairo #BE5446), gece lacivert (#1A2C55 civarı); içinde ince beyaz dev saat "6:41 AM". **"Grandma's House"** (yeniden adlandırılmış), "Local ➤" (GPS'le konum). Altta yüzen hap "⟲ Convert". Analog seçenekte **dünya haritası desenli mavi kadran** (gece koyu lacivert).
- Öne çıkan ekranlar:
  - **Çevirici (i5):** mavi cam panel "SET DATE & TIME": ×, paylaş, "+4:18 ⟲"; "2:00 PM", "Aug 8, 2024", − / +; **iki satır saat şeridi** "12•1•2…11 AM" ve "12•1•[2]•…11 PM" (seçili beyaz kare). Altında üç sütun saat tablosu LAX / GMT / TYO, gün değişiminde "Fri" etiketi, gece saatleri koyu mavi blok.
  - **Konum ayrıntısı (i6):** LOCATION harita kartı (Apple Haritalar), TIME ZONE: "Eastern Daylight Time EDT", Offset from GMT -4:00, Offset from local +3:00; SUN: Sunrise 6:01 AM, Solar noon 1:02 PM, Sunset 8:04 PM, Day length 14h 3m ve **24 saatlik daire kadranda Güneş yayı** (gündüz mavi yay, gece noktalı, Güneş simgesi, "Morning"); MOON bölümü (kesik).
  - **Uzak saatte alarm (i9):** "Alarms": solda "11:00 AM August 8" (yerel), sağda gri "2:00 PM EDT August 8" (uzak); başlık "Alarms that stick, even if you move" — alarm uzak şehrin saatine bağlı kalır, telefon başka dilime geçse de.
- Widget / kilit ekranı / Dynamic Island (i1, i2, i3): 4 şehir kodlu liste (LAX, NYC, LON, TYO; açık mavi), **gün eğrisi widget'ı** (Cairo, sinüs eğrisi üstünde Güneş, "☀︎↓ 2M", turuncu-mor gün batımı gradyanı), 2×2 kutular (gece kutusu lacivert), **dünya haritası widget'ı** (gece gölgesi, şehir kodları ve saatleri, sarı/mavi gündüz/gece noktaları), dev saniyeli "06:41:33 Grandma's House -3:00", tek uygulama simgesi boyutunda analog; kilit ekranında 2×2 kod listesi, "Thursday New York 12:41 PM", **"Thu 8 🌐 NYC 12:41:11 PM"** satırı, kodlu üçlü liste; **Dynamic Island'da iki şehir saati** ("12:41P NYC · 5:41P LON"), Live Activity Apple Watch'ta da görünür.
- Tasarım: iOS sistem açık/koyu, saat kutuları göğün rengini (gün saati) taşıyor; ince yazı tipi; şehir kodları (IATA) kısa ad.
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - **Saat kutusunun gök rengiyle boyanması** (gündüz mavi, akşam turuncu, gece lacivert) — yok: bizim satırda düz metin saat (`tr-dunya.png`) — K — rakipte ücretsiz. (Kürede gece gölgesi var; listede yok.)
  - **10.000+ şehir araması, GPS'le "Yerel"** — bizde 31 şehir (bkz. Lasmit maddesi) — K.
  - **Şehri yeniden adlandırma** ("Büyükannemin evi") — Lasmit maddesiyle aynı iş.
  - **Dynamic Island / Live Activity'de dünya saati** — yok (Live Activity türleri: timer/pomodoro/stopwatch/prayer: `LiveActivityModel.swift`) — mevcut #19'a ek ayrıntı (iki şehir saati) — K.
  - **Şehir ayrıntı sayfası** (harita, saat dilimi adı, GMT farkı, gün doğumu/öğle/batımı, gün uzunluğu, 24 saat kadranında Güneş, Ay evresi) — yok: dünya satırına dokununca ayrıntı açılmıyor (`WorldScreen.kt`); sitede şehir sayfalarında gün doğumu/batımı var (`/dunya-saatleri/<şehir>/`) — K — ücretsiz.
  - **Uzak şehrin saatine bağlı alarm** — yok (alarmlar telefonun saat dilimiyle: `AlarmItem`) — K — ücretsiz.
  - **Şehir kodu (IATA: LAX, NYC) kısa gösterim** widget'ta — yok (widget'ta tam ad: `widget-dunya.png`) — K.
  - Saniyeli dünya saati widget'ı — kısmen (saniyeli widget yalnız yerel saat: `SecondsClockWidget`) — K.
  - Dünya haritası widget'ı, gün eğrisi widget'ı — Lasmit maddesiyle aynı iş.

## World Clock Pro: Convert Time (App Store 969635117; ABD 4,78 / 1.354 puan; TR 5,0 / 8; Alex Komarov Inc.; sürüm 1.6.4, 22 Eylül 2025; yalnız İngilizce)
- Ücret modeli: reklamsız; **abonelik**: 1 month ₺79,99 / ₺99,99, 1 year ₺399,99 / ₺499,99 / ₺799,99. Ücretsizde ne kilitli: görselde yazmıyor.
- İncelenen görüntü: 4 iPhone (hepsi açıldı; eski iPhone 8 çerçevesi, görseller 2018 tarihli).
- Ana ekran (i1): siyah zemin; üstte mavi ince dev "9:41 AM", "➤ San Francisco, GMT-7", sayfa noktaları; ortada **24 saatlik çok ibreli kadran**: **üst yarı beyaz (gündüz, ☀︎), alt yarı siyah (gece, ☾)**, kenarda 12–1…11–12 iki kez (öğle üstte, gece yarısı altta), **her şehir bir ibre** ve ibrenin üstünde dar büyük harfle şehir adı (NEW YORK, LONDON, MOSCOW, BEIJING, SYDNEY; seçili şehir mavi "SAN FRANCISCO"). Altında koyu gri dünya haritası, şehir noktaları; sağ altta mavi paylaş düğmesi.
- Öne çıkan ekranlar:
  - **İbreyi sürükleyerek çevirme (i2):** mavi ibre sürüklenince kenarda mavi yay (geçen süre), altta "+4h 19m Friday, October 26, 2018", ⟲ sıfırla, mavi "Add Event" hapı.
  - **Paylaşma (i3):** beyaz alt sayfa "Times & locations to share": "Select all", şehir başına mavi ✓ daireler, sağda saat + gün ("☾ 10:00 PM Friday, Oct 26"; gece saatlerinde ay simgesi), mavi tam genişlik "Share".
  - **Bugün widget'ı (i4):** 4 sütun "Today 2:00 PM San Francisco… Tomorrow 8:00 AM Sydney", seçili sütun koyu, altta "− +3 HOURS +" (widget içinden saati kaydırma).
- Tasarım: siyah zemin, iOS mavisi vurgu, beyaz/siyah ikiye bölünmüş kadran; dar büyük harf şehir etiketleri (DIN Condensed benzeri).
- Hatalar: görülmedi (görseller eski).
- Bizde olmayan / farklı:
  - **24 saatlik çok ibreli dünya kadranı** (gündüz/gece yarıları, her şehir bir ibre, ibreyi sürükleyerek saat çevirme) — yok: dünya saatlerimiz liste + küre (`tr-dunya.png`, `GlobeView.kt`); 2. analiz #12 yalnız ikili analog kadran — O — rakipte abonelik. Saat modellerine de eklenebilir (site + uygulama).
  - **Çeviricide gece saatine ☾ işareti** ve paylaşılacak şehirleri seçme — kısmen: sitede toplantı planlayıcı paylaşımı var (`planner.js` `shareRow`), uygulamada toplantı planlayıcıda "paylaş" var (CLAUDE.md #85) — K.

## Flora - Green Focus (App Store 1225155794; ABD 4,76 / 82.944 puan; TR 4,77 / 536; AppFinca Inc.; sürüm 3.7.43, 30 Ekim 2026; 3 dil, Türkçe yok)
- Ücret modeli: **ücretsiz, reklamsız**; satın almalar "ağaç turu" paketleri: Amazon Rainforest Tour ₺49,99 / ₺99,99, Drylands' Marvels Tour ₺99,99 (yeni ağaç türleri). Ayrıca "Price/Care" ile **gerçek ağaç diktirme** (i8–i9; bağış benzeri).
- İncelenen görüntü: 10 iPhone (hepsi açıldı). Mağaza zemini turkuaz (#00ACC2), beyaz başlık, çizgi film karakterleri (gözlüklü papyonlu, kravatlı iki yuvarlak kafalı figür, yelkenli gemi).
- İlk açılış / ödeme: görülmedi.
- Ana ekran (i1, i4): **açık sarı zemin** (#FCF7B7), üstte turuncu başlık "Kally & Jason's Tree", ince turuncu dev "25:00", ortada **filiz** (iki yapraklı, yeşil), altında **lacivert-mavi dünya küresi** tepe gibi, yeşil köşeli "Start" düğmesi (birlikte odakta "Invite" / "Join").
- Öne çıkan ekranlar:
  - **Ağaç öldü (i5):** mercan pembe zemin (#E57373 civarı), "Oops!", açık pembe daire içinde **kurumuş çıplak gövde**, turuncu "See Who Killed the Tree" (arkadaşlarla odakta kimin telefona baktığı).
  - **Bahçe (i3, i8):** koyu lacivert gök, "Your Garden", avatar; altta düz çizimli ağaçlar (baobab, yeşil yuvarlak, pembe kiraz, sarı çam, lavanta, elma, kavun, incir); altında tarih çizgisi. **Alışkanlık takvimi:** "To-Dos | Habits" sekmeleri, "Read books" ay takviminde art arda günler koyu kahverengi dairelerle **zincir** (10–11–12, 14–15).
  - **Ağaç koleksiyonu haritası (i6):** "Grand Tour": dünya haritası etrafında sarı dairelerde açılacak ağaçlar (çiçek, çam, lavanta, kiraz, orkide, çilek, kavun, baobab).
  - **Yapılacaklara süre ekleme (i7):** "Read books" turuncu ilerleme çubuğu, "+1 h" etiketi ağaca bağlanıyor; renkli "+" daireleri (5 renk) ile yeni yapılacak.
- Widget (i2): orta-büyük widget: "27 h 40 m This Week", ağaç sırası, "UPCOMING 10:00 AM, Wed: Complete the work" (pasta ilerleme), iki sütun renkli kenar çizgili yapılacaklar (her birinde küçük pasta ilerleme).
- Tasarım: düz (flat) çizim, pastel sarı + turkuaz + mercan; yuvarlak sans; maskot figürler.
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - **Büyüyen bitki ve "ağaç öldü" (uygulamadan çıkınca)** — yok: 2. analiz #31 (fidan/orman koleksiyonu) — mevcut #31'e ek ayrıntı: Flora'da ölüm ekranı ve keşif haritası ("Grand Tour") var; inanç verisi değil, Bumba madde 13'e uygun tasarlanabilir — O.
  - **Arkadaşla birlikte odak** — sunucu gerekir — mevcut #53/#75 kapsamında (ücretli sistemle birlikte) — —.
  - **Alışkanlık takvimi (art arda günler zinciri)** — kısmen: pomodoroda seri var ("🔥 Seri: 4 gün", `tr-pomodoro-istatistik.png`), takvim görünümü yok — mevcut #31'e ek ayrıntı (ay/yıl görünümü) — K.
  - **Yapılacak başına toplam süre ve ilerleme çubuğu** — kısmen: `PomoTodo` her biten turu görevin sayacına yazar (`n`), ilerleme çubuğu ve hedef saat yok — K.
  - **Yapılacaklar / odak widget'ı** ("bu hafta 27 sa 40 dk") — yok (pomodoro widget'ı yok; `mobile/ios/Widget/Sources/` ve Android `ToolWidgets.kt`'de yalnız kronometre ve zikir) — K.

## Focus Keeper - Pomodoro Timer (App Store 867374917; ABD 4,79 / 31.740 puan; TR 4,73 / 1.795; PIXO Incorporation; sürüm 4.2.21, 27 Ekim 2026; 12 dil, Türkçe yok)
- Ücret modeli: reklamsız; **Focus Keeper Pro abonelik**: 1 Month ₺149,99 / ₺699,99, 1 Year ₺1.299,99 / ₺2.499,99 / ₺3.999,99, ömür boyu "Focus Keeper Pro" ₺4.999,99 (bu alandaki en pahalı ömür boyu fiyat). Pro'da ne kilitli: görselde yazmıyor.
- İncelenen görüntü: 8 iPhone (hepsi açıldı). Mağaza zemini açık gri (#F7F7F8), siyah kalın başlık, "Working from Home — Featured", "Perfect for Classroom — Featured" defne rozetleri.
- İlk açılış / ödeme: görülmedi.
- Ana ekran (i1): **bütün ekran mercan kırmızı** (#FD5D5D civarı), sol üstte kronometre simgesi + görev "Reading"; ortada yarı saydam hap "FOCUS 🍅🍅🍅🍅" (tur sayısı küçük domatesler, ilki dolu); **çok ince (hairline) beyaz dev rakam "24:59"**; altında **yatay kayan cetvel** (5, 10, 15 dakika çentikleri, sağa doğru incelen; süre bu cetveli kaydırarak ayarlanıyor gibi); büyük yarı saydam halka içinde ‖; altta hap biçimli 3 sekme (liste / kronometre seçili siyah / grafik).
- Öne çıkan ekranlar:
  - **Görevler (i2):** mercan başlıkta haftalık şerit (M 16 … S 22, bugün beyaz daire "19", altında nokta); beyaz kartlarda renkli kare simgeli görevler: "Reading — Every day" (büyük), "Morning Workout — Mon Wed Fri" (sayaç kırmızı "02:03:02"), "Work at Home — Weekdays" ("START"), "08:04:02" — görevler **tekrar günleriyle** (her gün, pzt-çar-cum, hafta içi) ve görev başına toplam süre.
  - **Günlük yapılacaklar (i3):** "Today" onay kutulu kartlar, tarih (kırmızı "Feb 4"), bayrak, alt görev, etiket, kronometre simgeleri; "Completed 2" katlanır bölüm; kırmızı yüzen "+".
  - **Grafikler (i5):** "Daily | Weekly | Monthly", "< Today >", "DAILY OVERVIEW" kartı "Focus 7h 36m · 18 Sessions · 9h 56m Total · 2h 20m Break", paylaş simgesi; "Focus | Total | Sessions" sekmeleri; açık pembe çubuklar, seçili gün dolu kırmızı; "⟲ Timeline" hapı.
  - **Zaman çizelgesi (i6):** gün başlığı "Thu, Jun 10"; dikey çizgide kırmızı kronometre simgeleri: "Completed Reading — 1h 15m Focus · 20m Break · 1h 35m Total · Task completed at 14:05"; altında oturumlar "13:40 - 14:05 Focused 25m Ended at…".
  - **İlerleme paylaşım kartı (i7):** beyaz kart, ortada renkli yuvarlak kare simge (mor şimşekli kronometre, yeşil kitap, kırmızı kalp), "Focused on Study **3h 58m**", "12 Sessions · 5h 28m Total · 1h 30m Break", tarih, sol altta "🍅 FOCUS KEEPER" logosu.
  - **Hatırlatmalar (i8):** "It's time. Let's get some work done!", "Focus session has finished. Let's take a short break.", "Short break has ended. Get back to work!".
- Widget (i4): büyük "Today" görev listesi (renkli simgeler + ▶ başlat), küçük kırmızı "Focus time today 12h 45m / Sessions 12 · Total 8h 45m · Break 45m", küçük "🍅 Focus 2/4 3:03 Reading ▶". Apple Watch (i2): siyah zeminde kırmızı ilerleme halkası "24:59 Reading".
- Tasarım: tek güçlü vurgu mercan kırmızı; rakamlar çok ince (ultralight); hap biçimli siyah seçili sekme.
- Hatalar: i3 ve i4'te "Complete the pitch deck", "Focus time", "Coding" satırlarının üstünde silik beyaz Çince karakter filigranı (görüntü işleme kalıntısı) görünüyor.
- Bizde olmayan / farklı:
  - **Odak ekranının tamamen vurgu rengiyle dolması ve ince dev rakam, tur domatesleri** — yok: bizim pomodoro ekranı krem zemin, kalın "25:00", altında "Tur 1 / 4" metni (`tr-pomodoro.png`) → görsel iş (rapor) — K.
  - **Kaydırılabilir süre cetveli** — yok (bizde ayarlarda süreler) — K.
  - **Görev başına tekrar günü ve görev listesi tarih şeridi** — kısmen: `PomoTodo` tek liste, tekrar yok — K.
  - **Gün/hafta/ay grafik sekmeleri, odak/mola/oturum ayrımı, zaman çizelgesi (her oturum başlangıç–bitiş)** — kısmen: bizde bugün/7 gün/toplam ve 7 günlük çubuk (`FocusStatsCard`, `tr-pomodoro-istatistik.png`); mola süresi ve oturum saatleri kayıtta yok (`pomo.log` `{d,m,t,g}`) — mevcut #31'e ek ayrıntı — K.
  - **Odak ilerleme paylaşım kartı** ("Ders: 3 sa 58 dk") — yok (paylaşım kartları vakit ve geri sayım için: `ShareCards.swift`; pomodoroda yalnız CSV) — K — ücretsiz.
  - **Pomodoro widget'ları** (bugünkü odak süresi, süren tur, görev listesinden başlat) — yok — K.
  - **Apple Watch'ta pomodoro** — yok — O.
  - Aşama sonu bildirimleri — **var** (pomodoro aşama bitince bildirim: `PomodoroScreen.kt` satır 43).

## Be Focused – Pomodoro Timer (App Store 973130201; ABD 4,71 / 4.323 puan; TR 4,74 / 1.361; Denys Ievenko; sürüm 2.7.1, 19 Ekim 2026; yalnız İngilizce)
- Ücret modeli: ücretsiz + reklamlı; **Pro**: Monthly ₺29,99, Yearly ₺199,99, Lifetime ₺499,99.
- İncelenen görüntü: 6 iPhone (hepsi açıldı). Mağaza zemini şeftaliden mercana gradyan, beyaz başlık, "5+ Million Users Worldwide" defne.
- İlk açılış / ödeme: görülmedi.
- Ana ekran (i1): beyaz zemin; üstte **yana kaydırmalı görev şeridi** ("…phic design · **Project Plan 3/7** · Implementa…", seçili ortada mercan), ortada **ince çizgili büyük halka**: geçen kısım açık gri, kalan kısım somon/mercan (#F7958C civarı), halkanın ucunda içi beyaz küçük tutamak dairesi; halkanın içinde ince somon "15:45" ve ince çizgi ‖; sağ üstte daire içinde × ; altta "Today 3/10" (günlük hedef). Alt sekme çubuğu (görevler, sayaç seçili gri hap, grafik, ayarlar, bilgi) ince çizgi simgeler.
- Öne çıkan ekranlar:
  - **Görevler (i2):** "To Do | Done" hap seçici, gün başlıklı liste, sağda "6/7" (yapılan/tahmini aralık; aşınca "4/3").
  - **Görev ayrıntısı (i3):** "Details | Intervals"; Activity type Work, Estimated intervals 5, Completed intervals 6, Due Date + tarih, All day task, etiket "Work".
  - **İlerleme (i4):** "Yesterday · **This week** · Previous week" kaydırmalı; Start/End date, Task, Tags, Units (Intervals) filtreleri (yeşil değerler); grafik türü: çubuk / pasta / liste; nane yeşili çubuklar (#9BE0C9), **kesikli kırmızı hedef çizgisi** (günlük 10 aralık).
  - **Eşitleme (i5):** iPhone, iPad, Mac menü çubuğu penceresi, Apple Watch (siyah, kırmızı halka "15:45 Today 3/10").
  - **Focus Matrix (i6):** aynı geliştiricinin Eisenhower matrisi uygulamasıyla görev eşitleme (Urgent/Important dörtlü).
- Tasarım: beyaz zemin, tek vurgu somon/mercan, her şey ince çizgi (yazı, halka, simgeler); yumuşak gölgeli hap sekme çubuğu.
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - **İnce halkalı pomodoro sayacı** (kalan yay + tutamak) ve **"Bugün 3/10" hedefin halka altında görünmesi** — kısmen: günlük hedef var ("Bugün 0/4 tur": `tr-pomodoro.png`, `pomo.goal`), halka yok → görsel iş — K.
  - **Görev şeridinde kaydırarak görev seçme** ve görev başına tahmini/tamamlanan tur ("6/7") — kısmen: `PomoTodo` sayacı (`n`) var, tahmin yok — K.
  - **Grafikte hedef çizgisi, tarih aralığı/görev/etiket süzgeci, pasta grafik** — kısmen: 7 günlük çubuk + etiket toplamı (`FocusLog.tags`) — mevcut #31'e ek ayrıntı — K.
  - **Mac menü çubuğunda pomodoro** — kısmen: Mac menü çubuğu uygulamamız yalnız saat ve vakit (`mobile/ios/Mac`, CLAUDE.md #95) — O.
  - Apple Watch'ta pomodoro — Focus Keeper maddesiyle aynı iş.

## Sahibin paylaştığı çalışma sayacı (adı görünmüyor; `bdd312d2-image.png`, 2556×1179 yatay) — 2. analiz #84'ün doğrulaması
- Görüntü yeniden açıldı ve ölçüldü. Belgedeki not doğru: siyah zemin (#000), **yedi bölütlü sarı LED rakamlar #FFB300** (ışıma/hale var), **sönük bölütler çok koyu kahverengi #140E00 civarı** (yalnız ortadaki yatay bölütler sönük, "00:00:00"), rakamlar ince gri çerçeveli (#1F1F1F civarı) yuvarlak köşeli kutu içinde, iki nokta üst üste yuvarlak.
- Üstte ince koyu gri ilerleme çizgisi (#141414; dolu kısmı görünmüyor, sayaç sıfırda). Solda "📖 Genel ⌄" hapı (ders seçici), ortada bölütlü seçici "✓ Sayaç" (seçili, kahverengi-altın zemin) | "⏲ Pomodoro"; sağda dört çizgi simge: pano/liste (not ya da kayıtlar), çubuk grafik, 3×3 ızgara, dişli.
- Altta hap düğmeler: "⟳ Sıfırla" (çerçeveli), **"▶ Başlat" dolu açık altın #F0BE6D**, "≡✎ Not", "⍰ Soru".
- Ek ayrıntı (önceki notta yok): rakamlar saat:dakika:saniye üç çift, çerçeveli kutu ekranın neredeyse tamamını kaplıyor (yatay kullanım için tasarlanmış).

## Ek kod denetimi (bizde ilerleme görseli)
- iOS: `mobile/ios/App/Sources/Intervals.swift` satır 645 yalnız HIIT'te düz `ProgressView` (çubuk); zamanlayıcı, pomodoro ve geri sayımda halka/çubuk yok (`grep "trim(from\|ProgressView\|Gauge" Intervals.swift ToolsTab.swift FocusSounds.swift Countdowns.swift` → tek sonuç satır 645).
- Android: aynı (`PomodoroScreen.kt`, `Intervals.kt`, `TimerScreen.kt`, `Countdowns.kt`'de `drawArc`/`CircularProgress` yok).
- Saatlerde (Apple Watch, Wear OS) zaman aracı yok: `mobile/ios/Watch/Sources/WatchApp.swift` ve `mobile/android/wear/.../WearActivity.kt` yalnız vakitler, tarih, hicri tarih.
- Uygulamada dünya saatlerine eklenebilen şehir sayısı 31 (`mobile/android/app/src/main/assets/cities.json` 31 kayıt; `WorldScreen.kt` satır 173, `WorldSeasons.swift` satır 112 `City.all`).

## İncelenen uygulamalar (özet)
| Uygulama | App Store id | iPhone görüntü | Not |
|---|---|---|---|
| Countdown Star | 576177593 | 7 | geri sayım |
| Countdown (Find Appiness) | 1403367428 | 10 | geri sayım |
| Days • Countdown & Widgets | 939368917 | 8 | geri sayım |
| Timer+ | 391564049 | 10 | çoklu zamanlayıcı, rutin |
| MultiTimer | 973421278 | 10 | çoklu zamanlayıcı, pano |
| Interval Timer □ HIIT Timer | 1124297113 | 5 | aralık |
| Seconds Interval Timer | 475816966 | 6 | aralık, Apple Watch |
| Timeglass | 1138584627 | 6 | çoklu zamanlayıcı |
| Visual Countdown Timer | 541364004 | 7 | çocuklar için görsel zamanlayıcı |
| Stopwatch‰ | 505889167 | 2 | kronometre |
| World Clock Time Zone Widget (Lasmit) | 912698955 | 10 | dünya saati |
| World Clock Time Zone Widgets (Overdesigned) | 956377119 | 10 | dünya saati |
| World Clock Pro | 969635117 | 4 | 24 saat kadranı |
| Flora | 1225155794 | 10 | odak |
| Focus Keeper | 867374917 | 8 | pomodoro |
| Be Focused | 973130201 | 6 | pomodoro |
Toplam 16 uygulama, 119 iPhone görüntüsü (hepsi açıldı) + sahibin 1 görüntüsü. iPad ve Google Play görüntülerine bakılmadı. Bulunamayan: Dreamdays ve "Time Until" aramada çıkmadı (TR ve ABD); "Tide" aramasında yalnız gelgit uygulamaları çıktı; Timeglass TR mağazasında 10 puanlı (ABD'de 1.450).
