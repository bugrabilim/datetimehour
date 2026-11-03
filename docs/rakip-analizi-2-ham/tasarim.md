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

