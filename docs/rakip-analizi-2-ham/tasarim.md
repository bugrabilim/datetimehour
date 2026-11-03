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

