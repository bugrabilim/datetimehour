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
