# Bekleyen işler

Saat Tarih'in sonraya kalan bütün işleri (standart "Bekleyen işler dosyası"). Biten iş silinir; geçmiş git'te.

## Sahibe sorular
- (Şu an yok.)

## Sahipte
- TestFlight yapı 33 (1.1; main `be0ab19`, 17 Kasım 2026 patronun onayıyla yüklendi; denemesi patronda): standart 5.21–5.23, bağlı üyelik düzeltmeleri, yapı 32 düzeltmeleri, tam ekranın hava durumsuz açılması, halka/grup moderasyonu; site ve hesap sunucusu 17 Kasım 2026'da canlıda. App Store 1.1 gönderimi bu yapıyla. (17 Kasım 2026)
- App Store 1.1 gönderimi (canlı sırası, bitti; içerik TestFlight yapı 32'de, 16 Kasım 2026): hata bildirimi, hesap düzeltmeleri, Global düşün, halka ve grup ekranları (sunucu açılana kadar "yakında"), sahibin TestFlight 31 ve 16 Kasım geri bildirimleri + 24 dil sürümünde mağaza metinleri (nötr adlar, yeni dil sürümleri en-GB, en-AU, en-CA, fr-CA, es-MX, pt-PT, zh-Hant, ms, ur-PK, bn-BD) ve ülkeye göre ekran görüntüsü sırası; sahibin onayıyla; `MARKETING_VERSION` ve `Deliverfile` `app_version` 15 Kasım 2026'da 1.1 oldu (TestFlight yapı 31); göndermeden önce 24 dil sürümünde `release_notes.txt`, App Store Connect Uygulama Gizliliği (e-posta, ad, kullanıcı kimliği), App Review deneme hesabı (depoya yazılmaz). (13 Kasım 2026)
- Google Play ve Wear OS (canlı sırası, bitti: iOS'la aynı içerik, hata bildirimi, hesap düzeltmeleri ve Global düşün dahil; mağaza sayfası 23 dilde): şahıs şirketi adına kurum hesabı; D-U-N-S numarası bekleniyor (ücretsiz, 30 güne kadar). Hesap açılınca `mobile/android/PLAY_CONSOLE.md` beyanları ve ilk yayından sonra ülkeye özel mağaza sayfaları (`mobile/android/store-listings/`). (5 Kasım 2026)
- Üyelik aşama 2 (canlı sırası, bitti; sahibin 15 Kasım 2026 onayı "tüm önerilerin uygun"): zikir ve hatim halkaları (2a sunucu, 2b uygulama ekranları), davet bağlantısı `/davet/#KOD` (2c) ve yedi ortak özellik (2d: ortak geri sayım, dünya saati, toplantı oylaması, odak odası, alarm önerisi, gözlem sayacı, anonim hava bildirimi); gizlilik sayfasında 19 dilde. Site ve hesap sunucusu 16 Kasım 2026'da yayında (uçlar kapalı); uygulama ekranları TestFlight yapı 32'de. 16 Kasım 2026 sahibin "ne gerekiyorsa yap" isteğiyle moderasyon (topluluk kuralları, yönetici bağlantısıyla gizle/uzaklaştır), App Review notu 8. bölüm ve Play beyanları hazırlandı (PR bugrabilim/datetimehour#65). Sıra: PR #65 birleştirildi (17 Kasım 2026, moderasyon canlıda) → ana altyapıdan `HALKALAR=acik`, `GRUPLAR=acik` (26 Kasım 2026 patronun onayıyla açıldı, 13:03 UTC; oturumsuz istek 401 döndüğü için uçlar açık) → TestFlight'ta deneme → 1.1 gönderiminde App Store Connect yaş derecelendirmesi ve Uygulama Gizliliği (`docs/uyelik-plani-2026-11.md` › Aşama 2) ve deneme hesabı; Play yayınıyla. Sıradaki aşama: uyanma grupları ve cami notları (`docs/uyelik-plani-2026-11.md`). (15 Kasım 2026)
- App Store özel ürün sayfaları (`docs/urun-sayfalari-2026-11.md`, sahibin 15 Kasım 2026 onayı "hemen yap"): altı sayfa App Store Connect'te taslak olarak açıldı (15 Kasım 2026, `ios-appstore.yml` `urun_sayfalari`; tr, en-US, de-DE, ar-SA; arama kelimeleri canlı 1.0'ın anahtar kelime listesiyle sınırlı, çoğu atanamadı: 1.1 yayımlanınca iş akışı yeniden çalıştırılır, var olan sayfalara 1.1 listesindeki kelimeler atanır; yeni dil sürümleri için betiğe sayfa sürümü ekleme yazılacak); incelemeye 1.1 ile, `ios-review.yml` `urun-sayfalari-gonder` ile, sahibin onayıyla. (15 Kasım 2026)
- Bağış (bahşiş) satın alması (2. rakip analizi #25): ayrıca değerlendirilecek. (10 Kasım 2026)

## Bu oturumda
- App Store 1.1 ve ilk Play gönderiminden önce standart "Mobil yayın" kontrol listesini sahibe raporla. Mobil derlemeler patronun kararıyla bekliyor (26 Kasım 2026). (14 Kasım 2026)

## Başkasında
- Ana altyapı oturumu: merkezle bağlı üyelik 17 Kasım 2026'dan beri etkin (`etkin: true`, bumbagroup.com #101); 26 Kasım 2026'da doğrulandı, bizden beklenen yok. Canlıda deneme hesabıyla uçtan uca deneme (kayıt, doğrulama, giriş, merkezde şifre doğrulama, güncelleme, silme) patronun 26 Kasım 2026 onayıyla ana altyapıda; sonucu bekleniyor. (26 Kasım 2026)
- MEB 2027-2028 çalışma takvimi: yayımlanınca `src/data/school.json`. (6 Kasım 2026)
- Fitre 2027: Diyanet Ocak 2027'de açıklayınca `src/data/fitre.json`; BAZNAS 2027 kararları (zekât fitrah, fidye). Bakılacak tarih: Ocak 2027. (6 Kasım 2026)
- Diyanet 2027 takvimi ve vakit tablosu: yayımlanınca `holidays.json` ve `node scripts/fetch-hijri.mjs`. (8 Kasım 2026)
- Endonezya SKB 2028 (Eylül–Ekim 2027) ve Hindistan DoPT 2028 (Haziran–Temmuz 2027) tatil listeleri: `src/data/world-holidays-official.json`. (10 Kasım 2026)
- Tayland'ın 2030 resmî takvimi (Budist takvimi artık günü doğrulaması), Nepal'in 2084 BS takvimi, NAOJ'un yeni 暦要項'sı: yayımlanınca test verilerine. (10 Kasım 2026)
- WMM2030 (Aralık 2029 civarı): `src/js/wmm.js` katsayıları. (3 Kasım 2026)

