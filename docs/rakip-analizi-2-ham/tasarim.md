# Tasarım incelemesi: sahibin ekran görüntüleri ve rakiplerin genel tasarımı (ikinci dalga, 3 Kasım 2026)

Yöntem: Her görüntü Read ile açıldı, renkler PIL ile piksel düzeyinde ölçüldü (hex değerleri ölçümdür; "ölçülemedi" yazmayanlar tahmin değildir). Sahibin görüntüleri 1179×2556 px (iPhone 15/16 Pro, 393×852 pt, 3x); yatay olanlar 2556×1179. Pt değerleri px/3'tür. Kırpmalar çalışma klasöründe (`scratchpad/rakip2/tasarim/crops/`), depoya görüntü konmadı.

## Bölüm 1: Sahibin 36 görüntüsü

### 1.1 `75fefac1` — Analog Clock Pro (Draunt, id 6785887940), Pro (ödeme) sayfası
- Uygulama adı görüntüde okunuyor: "Analog Clock Pro." (satın alma kutusunun başlığı).
- Sunum: iOS sayfa (sheet) sunumu; üstte arkadaki ikinci sayfanın kenarı ince koyu şerit olarak görünüyor (y≈190 px: #0b0b16). Yani Pro sayfası başka bir sayfanın (galeri) üstüne açılmış. **Durum çubuğu görünmüyor** (saat/pil yok; üst 0–140 px düz #1e262b), uygulama durum çubuğunu gizliyor.
- Yukarıdan aşağıya:
  1. Sol üst: hap düğme "Onarmak" (cam görünümlü, yarı saydam yeşilimsi gri #3e4338 civarı, beyaz yazı). "Restore" = "Geri yükle" olmalı; çeviri hatası (önceki notta var).
  2. Sağ üst: yuvarlak cam kapatma düğmesi, beyaz "×" (çap ~44 pt, zemin #3b4139 civarı). Gecikme ölçülemedi (tek kare).
  3. Başlık "Pro'ya yükselt": beyaz, SF Pro Display Bold, ~40 pt. Altında "Tam erişim": beyaz, SF Pro Semibold ~22 pt.
  4. Kadran küçük resim kolajı (6 kart): yeşil yapraklı koyu kadran, pembe-mavi fırça boyalı kadran, ortada büyük gri-mor yaprak dokulu kadran (beyaz halka), sağ üstte geniş kart "thusday / 21 May,2021" + yapraklı kadran, turuncu çizgili kadran, beyaz kadran + yeşil yaprak. Kartların köşesi küçük (~4–6 pt), hafif gölgeli.
  5. Satın alma kutusu: 2–3 px beyaz çerçeve, iç zemin #202220 (ölçüm: kutunun %77'si #1d1f1e–#202220), köşe ~12 pt. Başlık "Analog Clock Pro." (SF Pro Regular ~26 pt, sonunda gereksiz nokta). Sağ üst köşede kırmızı **#eb524d** "50% Off" etiketi (yalnız sol alt köşesi yuvarlak, kutunun köşesine oturuyor). "Ömür Boyu Erişim Alın Analog Clock Pro." (sonda yine nokta). "₺399,99" ve üstü çizili "₺79.998,00". "Tek seferlik SATIN ALMA".
  6. Birincil düğme "Devam et": düz beyaz **#ffffff**, siyah yazı SF Pro Semibold ~22 pt; ölçü 300×54 pt, köşe ~8–10 pt (ölçüm: köşegen yöntemiyle 24 px ≈ 8 pt). Ekran genişliğinin (393 pt) %76'sı, ortalı.
  7. Altta "Privacy Policy" ve "Terms & Conditions": gri **#aaaaaa**, SF Pro Regular ~15 pt, çevrilmemiş.
- Zemin: bulanık fotoğraf (yeşil-koyu orman): ölçülen noktalar #22281c, #252a1d, #262b29, #1c211c, alt kısım #191f1d. Düz renk değil, koyu zeytin-yeşil bulanık görüntü.
- Yazı tipi: bütün arayüz SF Pro (sistem). Küçük resimdeki tarih yazısı eşit aralıklı daktilo tipi.
- Reklam: yok. Premium işareti: bu sayfanın kendisi. Kapatma düğmesi sağ üstte, ilk karede görünür durumda.
- **Önceki notlarda atlananlar:**
  - Küçük resimdeki **"thusday"** yazım hatası (Thursday) ve "21 May,2021" (virgülden sonra boşluk yok): tanıtım görselinde hata.
  - "Ömür Boyu Erişim Alın Analog Clock Pro." satırı kutunun çerçevesine **değiyor/taşıyor** (sol ve sağda çerçeveye yapışık; görüntüde "Ö" çerçeveye biniyor). Yerleşim hatası.
  - Başlıkta ve açıklamada fazladan nokta ("Analog Clock Pro.").
  - Durum çubuğu gizli; sayfa ikinci bir sayfanın üstünde (sheet üstünde sheet).
  - Önceki notta "beyaz çerçeveli satın alma kutusu" doğru; "6 kadran küçük resmi, ortadaki büyük" doğru.
- Bizde olmayan: ödeme sayfası yok (ücretsiziz). Tasarım dersi: tek koyu bulanık fotoğraf zemin + tek beyaz birincil düğme; bizde ilk açılış/tanıtım sayfası yok (mevcut #4).

### 1.2 `731f8c39` — Analog Clock Pro, kadran galerisi (1. kaydırma konumu)
- Ad görünmüyor (üstte yalnız "PRO" hapı); 1.1 ile aynı sayfa düzeni ve aynı arka sayfa kenarı (#1f262b şerit, y≈0–170 px), aynı uygulama.
- Sunum: sayfa (sheet), durum çubuğu gizli. **Başlık çubuğu yok; "PRO" hapı ve × düğmesi içerik üstünde yüzen cam (iOS 26 Liquid Glass benzeri) öğeler.** İçerik bunların altından kayıyor: hapın içinden bir önceki kartın renk daireleri bulanık görünüyor (kırmızı, mavi, yeşil, turuncu, pembe lekeler).
- Yukarıdan aşağıya:
  1. Sol üst cam hap: beyaz taç simgesi + "PRO" (SF Pro Bold). Sağ üst cam daire: beyaz "×".
  2. Kart 1: başlık "Spor Kronograf" (beyaz, SF Pro Display Bold ~30 pt), alt yazı "Bir renk teması seçin" (gri **#939393**, SF Pro Regular ~20 pt). Sağda hap düğme "▶ Önizleme": dolgu **#292929**, 1 px kenar **#585858**, 98×32 pt, tam yuvarlak (hap), beyaz SF Pro Semibold.
  3. Önizleme kuyusu: saf siyah **#000000**, köşe ~28 pt; içinde canlı kadran: takometre ölçeği (60…240), "SAL 03" gün/tarih penceresi (SAL kırmızı, 03 beyaz), "SPORT CHRONO" geniş harf aralıklı küçük yazı, kırmızı **#e55652** yay ve işaretler, beyaz dikdörtgen saat işaretleri. Sol üstte cam hap "👑 Premium" (taç beyaz).
  4. Renk seçimi: 5 dolu daire (çap ~33 pt): kırmızı **#de4e49**, mavi **#3d8aee**, yeşil **#63c663**, turuncu **#e79245**, pembe **#e2485f**. Her birinin sağ üstünde siyah daire içinde **sarı taç**. Seçili (kırmızı) olanın etrafında beyaz iç halka + gri dış halka.
  5. Kart 2: "Velar / Chronograph" (iki satıra kırılmış başlık), "Bir renk teması seçin", "▶ Önizleme"; Roma rakamlı (XII, III, IX) kadran, gümüş çerçeve, "SAL" "03" beyaz kutucuklarda, üç alt kadran (60/15, 24/6/18/12, 100/75/25), "Premium" hapı.
- Ölçülen: sayfa zemini **#000000** (en sık renk %36), kart yüzeyi **#101010–#111111** (%29), kart köşesi ~24 pt (köşegen ölçümü 72 px; 393 pt genişliğe göre %6), kartlar arası ~12 pt boşluk, kenar boşluğu 20 pt (kart x=61 px).
- Yazı tipi: SF Pro (başlıklar Bold, alt yazılar Regular). Kadran içi yazılar: geniş harf aralıklı sans.
- Reklam yok. Premium işaretleri: her kartta "Premium" hapı + her renk dairesinde taç (yani **ücretsiz renk yok**, 5 rengin 5'i taçlı).
- Önceki notla karşılaştırma: "Siyah zemin, büyük yuvarlatılmış kartlar", "5 renk dairesi … her birinin köşesinde sarı taç; seçili renk beyaz halkalı" doğru. **Eksik:** cam yüzen başlık öğeleri (içerik altından kayıyor), "SAL 03" gün penceresi yazısının dilimize çevrilmiş olması (Türkçe gün kısaltması), kadranın kartta canlı işlemesi (ibre konumları 1.2 ve 1.3'te farklı: 10:42 → 10:43 civarı; canlı), seçili rengin çift halkası.

### 1.3 `77354235` — Analog Clock Pro, kadran galerisi (2. kaydırma konumu)
- Aynı sayfa. **Başlığı "PRO" hapının arkasında kalan kart:** görünen kısmı "…Kadran"; hapın altında başka harfler bulanık seçiliyor (ör. "C…" ya da "…l"): tam ad okunamıyor. Önceki notta "Kadran" diye ad verilmiş; **asıl ad hapın arkasında kalmış olabilir**, "Kadran" kesin değil. Alt yazı "Bir renk teması seçin" de hapın altında yarı kesik. "▶ Önizlem…" düğmesi × düğmesinin altında kalıp kesilmiş: **yüzen cam öğeler içeriği örtüyor** (yerleşim kusuru).
- Kadran: beyaz dış halka (#f7f7f7, ekranın %5'i), halkada **radyal döndürülmüş** büyük kalın rakamlar (alttaki "6" ters dönmüş "9" gibi görünüyor, "7" ve "8" baş aşağı), ince dakika çizgileri, kırmızı 5'lik işaretler; iç siyah kadran, üstte "03 KAS" (03 kırmızı, KAS beyaz), kırmızı akrep/yelkovan beyaz kenarlı, ince kırmızı saniye ibresi. Önceki nottaki "büyük eğik rakamlar" yanlış betimleme: rakamlar eğik (italik) değil, kadran çevresinde dönük.
- Renk daireleri bu kartta daha koyu tonlar: kırmızı, mavi, yeşil, turuncu, bordo (her kartın kendi 5 tonu var); hepsi taçlı.
- Kart 3: "Fütüristik / Çift Halka": siyah kadran, kalın kırmızı (#e55652 civarı) dış çizgiler, "03 KAS" kırmızı, iç halkada beyaz 1–12 rakamları, noktalı iç daire, kırmızı saniye ibresi sonunda küçük daire.
- Ölçülen: zemin #000000 (%48), kart #101010 (%24); diğerleri 1.2 ile aynı.
- Bizde olmayan (bu iki ekrandan): kadran başına renk teması seçimi (mevcut #1), galeri kartında canlı kadran + "Önizleme" (mevcut #1), kadranda gün/tarih penceresi ("SAL 03") — bizde analog modellerde tarih kadran dışında yazıyor (`mobile/ios/App/Sources/AnalogClock.swift` kadranın içinde tarih penceresi yok; site `src/js/main.js` analogSvg) → mevcut #1'e ek ayrıntı.

### 1.4 `e7c1a804` — Analog Clock Pro, "Saat Kadranları" (galerinin en üstü)
- Ad görünmüyor ("PRO" hapı); aynı uygulama (aynı cam hap, aynı kart ve "Önizleme" düğmesi).
- **En üstteki siyah hap ("🔔 Ses Açık") uygulamanın değil, büyük olasılıkla iOS'un Dynamic Island zil/sessiz göstergesi.** Kanıt: tam Dynamic Island'ın yerinde ve biçiminde (yatay ortalı, y 35–143 px ≈ 12–48 pt, saf siyah **#000000**), sayfanın (sheet) dışında, arka sayfa kenarının üstünde; solda zil simgesi, sağda yazı: iOS'un zil anahtarına basınca gösterdiği düzen. **Önceki not "Üstte 'Ses Açık' yazan siyah hap. Bu muhtemelen saniye tık sesi" büyük olasılıkla yanlış**; bu görüntü uygulamada tık sesi olduğunu kanıtlamaz (sahip Fullscreen Clock'ta tık sesi olduğunu ayrıca söylüyor; Analog Clock'ta tık sesini 1.5'teki araç çubuğundaki hoparlör düğmesi gösterebilir).
- Yukarıdan aşağıya: "PRO" cam hap, × cam düğme; başlık **"Saat Kadranları"** (beyaz SF Pro Display Bold ~38 pt), alt yazı "Uygulama kadranını seçin" (gri); kart "Klasik Kadranlar" (Bold ~28 pt) + "Favori tasarımınızı göz atın ve seçin" (gri, devrik çeviri: "göz atın ve seçin"); **iki sıralı yatay kaydırmalı ızgara**, üçüncü sütun kartın sağ kenarında kesik (kaydırma ipucu). Küçük resim kartları köşe ~17 pt (köşegen ölçümü 51 px), her biri kadranın zemin fotoğrafının bulanığıyla dolu kare.
  - Kadranlar: 1) deniz (koyu mavi-gri dalga), 2) alacakaranlıkta yapraklar (mor-gri), 3) kırmızı zemin üstünde şeker kamışları (Roma değil Arap rakamları 7–10 görünüyor, taçlı), 4) pembe-turkuaz mermer/akışkan boya (Premium), 5) koyu yeşil dalgalı su + rakamlar (Premium), 6) siyah palmiye yaprakları + rakamlar (taçlı).
  - İlk iki kadran (deniz, yaprak) taçsız → **ücretsiz olanlar bunlar** (önceki not "bazıları Premium" diyor; hangilerinin ücretsiz olduğunu yazmamış: 6'dan 2'si taçsız).
- Kart "Dönen Saniye / ve Dakikalar" + "▶ Önizleme" + kadran: kırmızı ve beyaz sayılı (00, 05, 10, 15, … 45) dönen ölçek; üstte cam hap "👆 Dokun" (el simgeli dokunma ipucu).
- Ölçülen: zemin #000000 (%83), kart #101010, beyaz yazı #ffffff; gri alt yazı #939393 (1.2 ile aynı).
- Bizde olmayan: fotoğraf dokulu kadranlar (mevcut #1), ızgarada kadran önizlemesi. Bizim "Saat görünümü" sayfasında model seçimi yok; modeller saat ekranında kaydırılarak seçiliyor (`mobile/ios/App/Sources/ClockModels.swift`, `Appearance.swift`).

### 1.5 `880630f9` — Analog Clock Pro, ana saat ekranı + puanlama kartı
- Ad görünmüyor; aynı uygulama (deniz kadranı 1.4'teki ilk küçük resim).
- **Durum çubuğu gizli** (üstte saat/pil yok).
- Zemin: kadranın deniz fotoğrafının çok bulanık hâli, düz değil: üst #2a3439, orta #293337, alt #1d272c; en sık #242f34 / #253035. Yani zemin kadran fotoğrafından türetiliyor.
- Üst sol: cam araç çubuğu hapı (dolgu #414d53 civarı), 6 beyaz çizgi simge, soldan sağa: dört daireli "çiçek" (kadran/tema seçimi), üst üste iki fotoğraf (zemin fotoğrafı), telefon dikdörtgeni (yön kilidi), dikey çizgili daire üstünde **kırmızı nokta** (ölçülemedi, küçük; büyük olasılıkla "canlı/hareketli zemin" ya da kayıt), hoparlör (ses), onay işaretli saat (alarm). Simgeler SF Symbols değil gibi ince özel çizim (çiçek simgesi SF'de "circle.grid.cross" değil); kesin ölçülemedi.
- Üst sağ: cam daire içinde üst üste kartlar simgesi (galeri; 1.2–1.4'e götürüyor olmalı).
- Kadran: tam genişliğe yakın (ekranın %90'ı ≈ 355 pt çap), ince beyaz dış halka, beyaz dakika çizgileri, beyaz **SF Pro Light/Regular** büyük Arap rakamları (1–12), beyaz parlak (gradyan gölgeli) akrep/yelkovan, açık mavi **#88d3fb** saniye ibresi (karşı ağırlığı dikdörtgen kürek), ortada beyaz disk + mavi nokta.
- Puanlama kartı (alt): cam görünümlü gri kart (dolgu #334148–#3d474c), köşe ~16 pt; solda kırmızı-pembe kalpler + yeşil noktalar çizimi; başlık "Uygulamamızı seviyor musunuz?" (beyaz SF Pro Bold ~20 pt), metin "Derecelendirmeniz, uygulamamızı geliştirmemize ve daha fazla kullanıcıya ulaşmamıza yardımcı olur!"; "Sonra" gri cam hap; **"Derecele / ndirin"** kırmızı gradyan hap (ölçülen #cc5857): yazı iki satıra bölünüp hapın dışına taşıyor (önceki notta var). Sağ üstte daire içinde ×.
- **Önceki notta atlanan:** kartın sol arkasında kısmen görünen **"Sa…" ve "3"**: altta saat ekranının tarih yazısı ("Salı" / "3") var ve **puanlama kartı onu örtüyor**. Yani uygulama kendi içeriğinin üstüne puanlama kartı koyuyor. Ayrıca kartın kendi kapatma (×) düğmesi ile "Sonra" aynı işi yapıyor (iki kapatma).
- Bizde: puanlama yok (mevcut #6). Zeminin kadran fotoğrafından bulanıklaştırılması bizde yok (bizde tam ekran zemini düz/fotoğraf/hareketli, `mobile/ios/App/Sources/AnimatedBackground.swift`; normal saat ekranı tema zemini).

### 1.6–1.8 `3454b4e3`, `1dfc7a97`, `63805975` — Analog Clock Pro, "Yenilikler" tanıtım dizisi (sayfa 1, 2, 3)
- Ad görünmüyor; aynı uygulamanın "Yenilikler" sayfaları (aynı beyaz düğme, aynı kadran üslubu). **Sıra saatten belli:** `3454b4e3` 11:21 ve 1. nokta seçili (İmparatorluk Yeşimi), `1dfc7a97` 11:22 2. nokta (Minimal Radar), `63805975` 11:22 3. nokta (Liquid Mercury). Önceki not sırayı Mercury–Radar–Yeşim diye vermiş; **gerçek sıra Yeşim → Radar → Mercury**.
- **Durum çubuğu bu sayfalarda görünüyor** (11:21/11:22, 5G, pil 90); öbür ekranlarda gizli. Tutarsız.
- Yukarıdan aşağıya (üçünde aynı düzen):
  1. Üstte ~150 pt boşluk (içerik dikeyde ortalanmış).
  2. Başlık "Yenilikler": beyaz, SF Pro Display Bold ~44 pt, ortalı.
  3. Alt yazı "Yeni premium saat stillerimizi keşfedin.": gri **#a6a9aa**, SF Pro Regular ~20 pt.
  4. Kadran görseli: kare çerçeve (dış çerçeve rengi **#181c22**, ince açık kenar; köşe ~24 pt), içinde 1:1 kadran görseli, kendi zemini kadran rengine göre (Mercury lacivert-siyah + cıva damlaları, Radar altın-kahve ışıma, Yeşim koyu yeşil-siyah).
  5. Kadran adı (beyaz, SF Pro Medium/Semibold ~34 pt): "İmparatorluk Yeşimi" (çevrilmiş), "Minimal Radar" ve "Liquid Mercury" (çevrilmemiş) → **ad çevirisi tutarsız**.
  6. Yine aynı alt yazı "Yeni premium saat stillerimizi keşfedin." → **her kadranın açıklaması yerine başlıktaki cümle tekrarlanmış** (içerik hatası; önceki notta yok).
  7. 10 sayfa noktası: seçili olan uzun hap **#dcc5a2** (bej/altın), diğerleri gri **#5a5c5e** nokta.
  8. Birincil düğme "Devam Et": beyaz **#ffffff**, siyah SF Pro Bold ~22 pt, 344×57 pt, **tam hap** (köşe ~26 pt ≈ yüksekliğin yarısı), kenarlara 24 pt boşluk. Not: 1.1'deki "Devam et" düğmesi köşesi ~8 pt dikdörtgen ve "et" küçük harfli; **aynı uygulamada iki farklı düğme biçimi ve yazımı**.
- Zemin: düz değil, çok koyu dikey ışıma: kenarlar **#06070b** (en sık, %26–33), görselin çevresi kadranın rengine göre hafif ışıyor (#181c22 lacivert, #181e1f gri-yeşil, #121b1b yeşil).
- Kadran ayrıntıları (yeni):
  - İmparatorluk Yeşimi: altın tırtıklı çerçeve, Çince rakamlar 一…十二, "IMPERIAL / JADE" serif büyük harf, ortada silik "福" filigranı, kırmızı **"SAL" "03" kutucukları** (Türkçe gün), altın akrep/yelkovan, kırmızı saniye ibresi + kırmızı top uç.
  - Minimal Radar: altın çerçeve, sağda **kurma kolu (taç)**, saydam radar taraması (saniye dilimi), ince eşmerkezli halkalar, ortada turuncu nokta. Rakam yok.
  - Liquid Mercury: gümüş metal çerçeve, mavi uçlu çubuk işaretler, "MERCURY / LIQUID METAL" geniş aralıklı yazı, "KAS SAL" (Türkçe ay+gün, ama gün numarası "3" ayrı pencerede sağda), mavi saniye yayı (dakika başından beri dolan mavi yay), yüzen gri cıva damlaları.
- Reklam yok; kapatma düğmesi yok (yalnız "Devam Et"); bu sayfa bir tanıtım akışı.
- Bizde: "Yenilikler" ya da ilk açılış tanıtımı yok (mevcut #4). Kadranlar yok (mevcut #1).

### 1.9 `0409601c` — Alarm uygulaması (sürüm 2.2, ad görünmüyor), "Choose Language"
- Ad görünmüyor (yalnız 1.10'da "About App 2.2"). Durum çubuğu 08:14, 5G, pil 34.
- Gezinme çubuğu: sol "‹" (geri, siyah), orta "Choose Language" (siyah Bold ~22 pt), sağ "Done" (sistem mavisi **#007aff**, ölçüldü). Altında ince ayraç.
- Liste (her satır ~55 pt): yuvarlak bayrak (çap ~28 pt), üstte İngilizce ad (siyah Semibold ~20 pt), altta "kendi adı" (gri **#8e8e93**). Seçili satır: **#e5e5ea** dolgulu tam hap (253 pt'den geniş, köşe ~28 pt ≈ yükseklik yarısı), onay işareti yok.
- Satırlar birebir: English/English · Chinese/中国 · Korean/한국인 · India/इंडिया · Spanish/Española · France/français · Russia/Русский · Portuguese/Português · (9. satırın bayrağının üst kenarı reklamın arkasında görünüyor; liste reklamın altına giriyor).
- **Çeviri hataları (önceki notta yok; not "Diller: … India (Hintçe) …" diye düzeltilmiş yazmış):**
  - Dil yerine ülke adı: "India", "France", "Russia" (Hindi, French, Russian olmalı).
  - "中国" = Çin (ülke); dil adı "中文" olmalı. "한국인" = Koreli (kişi); "한국어" olmalı. "इंडिया" = Hindistan (ülke); "हिन्दी" olmalı. "Española" (dişil sıfat); "Español" olmalı.
  - Hindistan bayrağı Hintçe için, İngiltere bayrağı İngilizce için: bayrakla dil eşlemek bilinen bir kötü uygulama.
- Reklam (alt, ~90 pt yükseklik, zemin #f2f2f7): "Weather Live Radar - NOAA", mavi "Ad" etiketi, "Weather Live Radar, çevrenizdeki hava durumunu takip etmenin ve günü…", mavi tam hap "Yükleyin" (#068dc7 civarı görselde; düğme ölçülmedi). Sağ üstte ⓘ.
- Yazı tipi: SF Pro değil gibi; yuvarlak hatlı geometrik sans (Rubik benzeri). Kesin ad ölçülemedi.
- Zemin **#ffffff** (%76), seçili hap #e5e5ea, ikincil yazı #8e8e93 (iOS sistem grileri birebir).
- Bizde: uygulama içi dil seçimi yok (iOS'ta sistem ayarı; Android 13+ `locales_config.xml` ile sistem) → mevcut #5. Biz dili kendi adıyla yazmalıyız, bayraksız.

### 1.10 `8e4ee86f` — Aynı alarm uygulaması, "Setting" (ayarlar)
- Başlık **"Setting"** (tekil; alt çubukta "Settings"): yazım tutarsızlığı (önceki notta yok).
- Liste kaydırılmış: ilk grubun üst kenarı başlığın altına girmiş, grubun başlığı görünmüyor.
- Gruplar (beyaz kartlar **#ffffff**, köşe ~12 pt, kenar boşluğu 16 pt; zemin **#f2f2f6** ≈ iOS systemGroupedBackground; grup başlıkları "Other", "About" gri #8e8e92, büyük harfsiz):
  1. (başlıksız) "文A Languages" … sağda gri "English ▾" (açılır seçici); "↗ Fade Effect" + kapalı anahtar (gri). Kapalı anahtar iOS'un yeni (iOS 26) geniş anahtarı.
  2. Other: ✉ Feedback · 👍 Rate App · ⟨ Share App.
  3. About: 🛡✓ Terms & Conditions · 🔒 Privacy Policy · ⓘ About App … "2.2".
- Simgeler: siyah çizgi simgeler (SF Symbols değil gibi; başparmak ve paylaş simgeleri farklı çizim), satır başı 24 pt.
- Ayraçlar simgeden sonra başlıyor (iOS düzeni).
- Reklam: "Ad Xena - Grup Sesli Chat Partisi / Xena – En İyi Sosyal Oyun Alanınız!" + mavi "Yükleyin" (köşeli, #377eec civarı), ⓘ. Alt çubuğun hemen üstünde, ~55 pt.
- Alt çubuk: 3 sekme, simge + yazı: Alarm (zil saati), Clock (saat), Settings (mavi altıgen dişli). **Seçili sekme gri yuvarlak cam kabarcık içinde** (#e2e2e4; iOS 26 sekme çubuğu biçimi), seçili renk mavi **#377eec**, seçili olmayan siyah.
- Önceki not doğru (Languages, Fade Effect, Other, About, 3 sekme). Eksik: "Setting" yazımı, iOS 26 cam sekme kabarcığı, satır sırası, gri gruplu zemin değerleri.
- Bizde: ayarlarda puanla/paylaş/geri bildirim yok (mevcut #6). Bizim ayarlar sayfası: `mobile/ios/App/Sources/InfoViews.swift` / `ToolsTab.swift` (aşağıda Bölüm 3'te ölçüldü).

### 1.11 `065d645c` — Alarmy, tanıtım sayfası 1/3
- Ad bu sayfada görünmüyor; 1.12'deki "Alarmy" yazısından ve 1.13+ akışından Alarmy (Delight Room) olduğu kesin. Durum çubuğu 10:06, 5G, pil 70.
- Zemin **#09090b** (düz, %90), üzerine dağınık küçük beyaz yıldız noktaları (~12 nokta, 1–2 pt).
- Yukarıdan aşağıya: 🏆 Apple emojisi (~34 pt); başlık "Dünyanın / en güvenilir alarmı" (beyaz, Bold ~40 pt, iki satır ortalı); üç ödül satırı, her biri iki yanda **gümüş-gri gradyan defne dalı** çizimi: "#1 Alarm App" (Bold ~36 pt) + "in 97 countries" (Semibold ~15 pt); "4.8★" + "Rating"; "100M+" + "Downloads". Bu altı yazı **İngilizce kalmış** (başlık Türkçe): yarım çeviri (önceki not alıntılamış ama çevrilmemiş olduğunu yazmamış).
- 3 sayfa noktası: seçili **#fafafa**, diğerleri **#52525b**.
- Birincil düğme "Başla": dolgu **#fc314c** (ölçüldü; önceki nottaki "#FF2D55'e yakın" yaklaşık doğru, gerçek değer #fc314c), beyaz Semibold ~18 pt yazı, 344×52 pt, köşe ~9–12 pt (köşegen ölçümü 27 px). Kenarlara 24 pt.
- Atla/kapat düğmesi **yok**.
- Yazı tipi: SF Pro'dan ayırt edilemedi (Bold/Semibold).

### 1.12 `766e782c` — Alarmy, tanıtım sayfası 2/3
- Başlık "Artık erteleme yok / Günün kontrolü sende" (beyaz Bold ~30 pt, ortalı).
- Karşılaştırma çizimi: iki telefon ekranı kesiti (üst köşeleri ~40 pt yuvarlak, gri kenar #2f2f31 civarı, iç **#161618–#1a1a1c**).
  - Sol "Other Apps" (gri **#aeaeb2**): 6:55, 7:10, 7:20, 7:30 dört alarm, iOS yeşili anahtarlar (**#39bf39**), alttaki soluk (aşağı doğru kararan geçiş).
  - Sağ "Alarmy" (pembe-kırmızı **#fd6d80**): tek 7:00 alarmı, kırmızı anahtar **#fc314c**.
  - Saat rakamları ince (SF Pro Light ~44 pt), iOS Saat uygulamasını taklit ediyor.
- "Other Apps" İngilizce (çevrilmemiş).
- Noktalar: 2. seçili. Düğme "Sonraki", 1.11 ile aynı.
- Bizde: tanıtım sayfası yok (mevcut #4).

### 1.13 `c4b7078f` — Alarmy, tanıtım sayfası 3/3
- Başlık "Tıbbi yayınlarda / adı geçen tek alarm" (beyaz Bold ~30 pt).
- Üç gri (silik) üniversite amblemi: "University" (baykuşlu), "HANYANG UNIVERSITY 1939 한양", "State College 1983". Adı belirsiz "University" ve "State College" amblemleri: iddianın kaynağı gösterilmiyor.
- Ortada **çizgi çizim beyin**: devre kartı gibi çizgiler, turuncu-sarıdan pembeye gradyan (ölçülen tonlar #ffd39e → #ffac93 → #ff8b8a → #ff6780), iki beyaz ışıklı nokta; arkada kesik çizgili insan başı silüeti (gri).
- İki gri daire (dolgu **#282a2c**): "Morning Productivity / 2x", "Goal Achievement / +15%" (İngilizce, çevrilmemiş).
- Noktalar: 3. seçili. **Düğme son sayfada "Sonraki", ilk sayfada "Başla"**: sözcükler ters yerde (önceki not "Başla"/"Sonraki" diye geçmiş, sırayı yazmamış). Atla düğmesi yok.
- Bu ekran görselinin anlamı: Alarmy tanıtımı sosyal kanıt + "bilimsel" iddia + rakip karşılaştırması üçlüsü. Bizim kimliğimize uymaz (sayı iddiası yok), ama **çizgi illüstrasyon + gradyan** üslubu (tek renk ailesi, ince çizgi) bizim için uyarlanabilir.

### 1.14 `15fdb6a4` — Alarmy, kurulum sihirbazı adım 2/4: "Alarm duvar kağıdını seç"
- Durum çubuğu 10:07. **İlerleme çubuğu:** 160 pt genişlik, ~4 pt kalınlık, dolu kısım beyaz **#fafafa** (yarısı), iz **#27272a**, yanında "2/4" (2 beyaz, "/4" gri). Geri düğmesi görülmedi.
- Başlık "Alarm duvar kağıdını seç" (beyaz Bold ~30 pt). TDK yazımı "kâğıdını" olurdu (küçük hata).
- Bölümler: emoji + başlık (Bold ~22 pt): 🪩 "KPOP Trend", 💖 "Popüler", 🔥 "Günlük motivasyon" (alt kısmı ekrandan çıkıyor).
- Her bölüm yatay kaydırmalı dikey kart şeridi; kart 114×203 pt (**9:16**, telefon ekranı oranı), köşe ~15–20 pt; soldaki ve sağdaki kartlar kesik (kaydırma ipucu). Kartın altında, görselin üstüne beyaz gölgeli yazıyla "♪ Ad" (tek satır, "…" ile kesilmiş): "Morning is…", "GO HARD S…", "Siren ver. G…", "Tiny Cat", "Cat Alarm", "Screaming…".
- **Seçili kart:** 2 pt beyaz **#fafafa** çerçeve + ortada büyük beyaz ✓ (karartma yok).
- İçerik: gerçek K-pop grubu fotoğrafı ve Korece "아침이 왔다잖아 / Morning is here" yazısı (lisanslı içerik), kedi "meme" görselleri (dans eden/bağıran kediler), havai fişek; kartlar büyük olasılıkla hareketli (video): kedi görselleri GIF/video karesi gibi bulanık ve pikselli.
- Zemin #09090b.
- Önceki notla karşılaştırma: "Earth, Night of Earth, On Earth, Black, Early morning, Daylight" adları bu görüntüde değil, aynı adımın aşağı kaydırılmış hâlinde (`b49c98ab`, 1.16). "Kartın altında ♪ işaretli ad (ör. 'GO HARD S…'): zemin ve ses birlikte" doğru, ama **yalnız bu bölümlerdeki kartlarda ♪ var**; "Uzaya doğru" ve "Basit" kartlarında ♪ yok (yalnız görsel). Seçili kartta "ortasında onay işareti" doğru. Tam ekran önizleme `2f1fbc44`'te (1.15).
- Bizde: alarm duvar kâğıdı yok (mevcut #2). Bizim alarm ekranı Android `AlarmActivity.kt` tema zeminli.

### 1.15 `2f1fbc44` — Alarmy, duvar kâğıdı tam ekran önizlemesi ("Earth")
- Durum çubuğu 10:07. Zemin: tam ekran Dünya fotoğrafı (uzaydan, sol yarı karanlık, saf siyah **#000000** %62).
- Sol üstte beyaz "‹" (geri). Ortada "3 Kas Sal" (gri **#c8c8c8**, Semibold ~20 pt; Türkçe kısa tarih, gün adı sonda), altında **"07:00"** dev beyaz (**#fafafa**), SF Pro Bold/Semibold ~95 pt (rakamlar eşit genişlikte, SF'nin tabular rakamları), alarm çalarken görünecek ekranın taklidi.
- Altta "Seç" düğmesi: dolgu **#e84652** (ölçüldü). **Tanıtımdaki kırmızıdan (#fc314c) farklı ton**: aynı akışta iki ayrı kırmızı. 344×52 pt, köşe ~12 pt.
- Önceki not ("Tam ekran önizlemede '3 Kas Sal' ve büyük '07:00', altta 'Seç' düğmesi") doğru; eksik: iki farklı kırmızı, tarih biçimi.
- Bizde: alarm ekranında zemin seçimi yok (mevcut #2). Bizim alarm çalma ekranı Android `mobile/android/app/src/main/kotlin/tr/bumba/time/AlarmActivity.kt`; iPhone'da AlarmKit sistem ekranı.

### 1.16 `b49c98ab` — Alarmy, adım 2/4 aşağı kaydırılmış
- Aynı ilerleme çubuğu "2/4". Bölümler: 🪐 "Uzaya doğru" (kartlar: **Earth** [seçili: 2 pt beyaz çerçeve + ✓], **Night of Earth** [şehir ışıkları], **On Earth** [yıldızlı ufuk], 4. kart "S…" kesik), 🩶 "Basit duvar kâğıtları" (**Black** [#0f0f0f düz], **Early morning** [mavi-gri → amber gradyan], **Daylight** [şeftali → turuncu gradyan], 4. "L…" kesik), 💥 "Gürültülü uyanış" (siyah kart, kırmızı ışıklı siren/hoparlör fotoğrafı, alarm ışığı; adlar ekran dışında).
- Kart adları İngilizce (çevrilmemiş), ♪ yok (yalnız görsel, ses ayrı).
- **Yazım tutarsızlığı:** bölüm başlığı "Basit duvar **kâğıtları**" (şapkalı), sayfa başlığı "Alarm duvar **kağıdını** seç" (şapkasız).
- Kart etiketi rengi: beyaz üstüne gri gölge, Semibold ~18 pt (#b7b7b7 civarı yarı saydam görünüm).
- Basit gradyan kartlar (Early morning, Daylight) **telifsiz, kendi üretebileceğimiz** zemin örnekleri: ölçülen (kart ortası, üst/orta/alt): Early morning **#a8b9ca → #b69b76 → #ba8833**; Daylight **#efd0a3 → #e6a255 → #d15d33**.

### 1.17 `8260230e` — Alarmy, kurulum adım **3/4**: "Alarm sesini seç"
- Durum çubuğu 10:08. İlerleme çubuğu dolu kısmı ~%75, "3/4". **Not:** önceki not sihirbazı "2/4: duvar kâğıdı → ses → görev" diye anlatıyor; görüntülerde duvar kâğıdı **2/4**, ses **3/4**; 1/4 adımı sahibin görüntülerinde **yok** (görülmedi; ne olduğu bilinmiyor), görev adımı da görülmedi (1.18'de ilerleme yok).
- Başlık "Alarm sesini seç" (beyaz Bold ~30 pt).
- Çip satırı (yatay kaydırmalı, yükseklik ~33 pt, tam hap): seçili çip **#fafafa** zemin + siyah Bold yazı; diğerleri **#27272a** zemin + beyaz yazı; her çipte emoji: 🪩 KPOP Trend, 💖 Popüler, 🔔 Varsayılan (4. çip ekran dışında).
- Bölüm başlığı (emoji + Regular ~18 pt): "🪩 KPOP Trend".
- Gruplu liste kartı: dolgu **#1b1b1d**, köşe ~19 pt (köşegen ölçümü 58 px), ayraç **#3f3f46** (radyodan sonra başlıyor), satır ~56 pt. Her satır: boş daire (2 pt beyaz kenar) + ad (beyaz Regular ~20 pt). **Seçili/çalan satır:** daire turkuaz **#2dbde0** dolu + beyaz iç nokta, ad Bold'a geçiyor, sağda beyaz ses dalgası simgesi (dinleniyor).
  - Satırlar: GET YOUR BUTT UP! (seçili), GO HARD Alarm, MEOVV Facetime, Go HARD Siren ver.; "💖 Popüler": Cat Alarm, Kâbus Alarmı…
- "Sonraki" düğmesi (#fc314c) listenin üstünde sabit, altında ve üstünde zemine kararan geçiş (liste düğmenin arkasından kayıyor).
- "MEOVV Facetime" gibi adlar: K-pop grubu (MEOVV) lisanslı ses. Önceki not "Örnek sesler: GET YOUR BUTT UP!, GO HARD Alarm, Cat Alarm, Kâbus Alarmı…" doğru; **"MEOVV Facetime" ve "Go HARD Siren ver." eksik**.
- Bizde: uygulamanın kendi ses kitaplığı yok (mevcut #3). Bizim alarm sesi: iOS'ta tek sentez melodi `mobile/ios/App/Resources/alarm-rise.caf` + müzik dosyası, Android'de sistem zil sesi seçicisi (`AlarmScreen.kt`).

### 1.18 `0ca45771` — Alarmy, adım 3/4 aşağı kaydırılmış
- Başlık yukarı kaymış, **çip satırı üstte yapışkan** ve kaydırma konumuna göre seçili çip değişiyor (şimdi "💖 Popüler" seçili, beyaz): kaydırma izleyen sekme (scroll-spy). Önceki notta yok.
- Popüler: Cat Alarm, Kâbus Alarmı, Alarm Halüsinasyonu, Wake up you lazy kid, **Gooooood Morningggggg** (seçili, turkuaz, ses dalgası). "🔔 Varsayılan": Orkney, Çalar Saat, Dijital Alarm, Güneş ışını.
- Önceki notta "Gooood Morninggg" diye kısaltılmış; görüntüde "Gooooood Morningggggg" (7 "o", 6 "g").
- Ses adlarının bir kısmı çevrilmiş (Kâbus Alarmı, Alarm Halüsinasyonu, Çalar Saat, Dijital Alarm, Güneş ışını), bir kısmı İngilizce (Cat Alarm, Wake up you lazy kid, Orkney): karışık.
- Zemin #09090b, liste #1b1b1d, düğme #fc314c (ölçüldü; bu ekranda tanıtımdaki kırmızı).

### 1.19 `c48ca2d5` — Alarmy, kurulum adım 4/4: "Bir uyanma görevi seç"
- İlerleme çubuğu tamamen dolu, "4/4". Başlık "Bir uyanma görevi seç" (beyaz Bold ~30 pt).
- **Gruplu liste değil, her seçenek ayrı kart** (adım 3'teki gruplu listeden farklı): kart dolgu **#18181b**, ~57 pt yükseklik, köşe ~17 pt, aralarında ~13 pt boşluk, kenarlara 24 pt.
- Her kartta solda yuvarlak kare simge kutusu (~42×37 pt, köşe ~10 pt) ve beyaz Regular ~20 pt ad:
  - "Matematik problemleri": kutu **#223c45** (koyu camgöbeği), simge **#4dcff0** (+ ve × işaretli kare).
  - "Yazma": aynı camgöbeği, klavye simgesi.
  - "Renkli Karoları Bul": aynı camgöbeği, 2×2 karo (biri taralı).
  - "Sallama": kutu **#382e48** (koyu mor), simge **#ba85ff** (sallanan telefon).
  - "Kapalı": kutusuz, beyaz × simgesi.
- **"Sonraki" düğmesi yok**: karta dokunmak seçimi ve geçişi birlikte yapıyor (görülen: düğme yok; davranış görülmedi).
- Simgeler: dolgulu, yuvarlak köşeli özel simgeler (SF Symbols'e benzemiyor).
- Kart rengini simge rengine göre boyama: aynı türden görevler aynı renk ailesi (camgöbeği = zihin/el, mor = hareket).
- Önceki not doğru ("koyu kartlar, renkli yuvarlak kare simgeler"); eksik: düğmesiz seçim, renk ailesi kuralı, Kapalı'nın kutusuz olması.
- Bizde: kapatma görevi seçimi var ama alarm düzenleyicide bir seçim listesi (Android `WakeTasks.kt`, iOS `WakeTasks.swift`); renkli simge kutuları yok.

### 1.20 `088c5832` — Alarmy, "Sabah" sekmesi (ana ekran)
- Durum çubuğu 10:09. **Zemin: yağmur damlalı cam fotoğrafı/animasyonu** (havaya göre; bulanık yeşil-gri doğa arkada, damlalar keskin). Ölçülen: üst #676b6d, orta #313a24; altta lacivert panel.
- Hava bloğu (doğrudan zemin üstünde, kartsız): "Ankara ›" (beyaz Regular ~22 pt), **"11°"** (beyaz, SF Pro Light/Regular ~64 pt), sağda "Yağmur 🌧" ve "en yüksek 17° | en düşük 10°" (beyaz Semibold ~20 pt); 9 saatlik şerit 10…18: saat (beyaz %70 opak), düz beyaz bulut simgeleri (11'de güneşli bulut, 13'te yağmurlu bulut, damlalar mavi), sıcaklık 13°, 15°, 16°, 16°, 16°, 17°, 17°, 16°, 15°.
- Dört kare kart (~52×52 pt, köşe ~14 pt, **pastel gradyan** dolgular, ölçülen): "Evcil Ha…" (açık mavi **#d3e3fb**; yumurtadan çıkan pembe yaratık, üstünde "Beni uyandır" konuşma balonu, mavi yazı), "Sabah hissi" (sarı **#fceda5 → #fdf5cf**, gülen yüz emojisi), "Namaz vakti" (lavanta **#ccb4f9 → #e2e1f9**, mor hilal + yıldız), "Puanlarım" (açık mavi **#a3cdfa → #c9e5f8**, mavi "0"). Kart adları beyaz Semibold ~20 pt, kart altında.
  - **Yazı taşması/kesilmesi:** "Evcil Ha…" ("Evcil Hayvan" sığmamış), "Namaz / vakti" iki satıra kırılmış: aynı satırdaki etiketler hizasız (önceki notta yok).
- Video reklam kartı (~290 pt yükseklik, köşe ~20 pt, koyu): sol üstte ⓘ, sağ üstte sessiz düğmesi ve beyaz daire içinde × (**#fafafa**), video 0:04 / 0:30 ilerleme çubuğu, pembe-kırmızı "LEARN MORE" (**#c14664**, İngilizce).
- Altta **açılır panel tutamağı**: lacivert (**#151833**) yuvarlak üst köşeli panel, ortasında beyaz "︿" ok (yukarı çekilince bir şey açılıyor; içeriği görülmedi). Önceki notta yok.
- Alt çubuk: yüzen cam hap (dolgu #303553 / #313353 civarı, lacivert), 5 sekme simge + yazı: Alarm, Uyku, Sabah (seçili: daha koyu hap **#161d3e** içinde, beyaz güneş simgesi), Rapor, Ayar. Rapor ve Ayar üstünde **kırmızı bildirim noktası** (#7c3851 ölçülen kenar, nokta küçük).
- En altta banner reklam: "AD | Enpara | Ayrıntıları göster ›" ve sağda reklam rozeti.
- **Önceki notla karşılaştırma:** "Yağmur damlalı cam zemin", "Ankara >", "11°", "Yağmur", en yüksek/en düşük, 9 saatlik şerit, dört kare kart, video reklam, 5 sekme, kırmızı noktalar, banka reklamı: hepsi doğru. **Eksik:** açılır panel tutamağı; etiket taşmaları; reklamın 30 sn'lik video olduğu ve sessize alma/kapatma düğmeleri; kartların pastel gradyan renkleri; sıcaklığın büyük ince yazı tipi.
- Bizde: Saat ekranı hava alanı var (iOS `HomeItems.swift` `weather`), havaya göre zemin yalnız tam ekranda (`fsBg = weather`) → mevcut #8. Sabah sekmesi bizde yok; "Günaydın kartı" alarmdan sonra bir kez (iOS `Morning.swift`).

