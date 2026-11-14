# Bekleyen işler

Saat Tarih'in sonraya kalan bütün işleri (standart "Bekleyen işler dosyası"). Biten iş silinir; geçmiş git'te.

## Sahibe sorular
- (Şu an açık soru yok.)

## Sahipte
- Site yayını (canlı sırası, bitti): PR bugrabilim/datetimehour#60'ın main'e birleştirilmesi; Coolify kendiliğinden yayımlar. İçinde: sınav takviminin kaldırılması (126 sayfa 301), fotoğraf ışığı `/altin-saat/`, 22 kapsayıcı takvim, Bengalce (`/bn/`), Endonezya ve Hindistan işleri, panchang, dünya şehri araması, dil düzeltmeleri, bakım, üyelik (ayarlarda "Hesap", gizlilikte üyelik bölümü, `/hesap-silme/`, eski hesap adresleri 301). Önce CI yeşil, `npm test`; zamanı sahibin onayıyla. (9 Kasım 2026)
- iOS TestFlight yapı 30 (canlı sırası, bitti): tanıtımın ilk sayfası, iki tonlu simgeler, Müslüman olmayanlar A grubu, İngilizce yedek dil, fotoğraf ışığı, 22 takvim, varsayılan konum, Bengalce, Endonezya ve Hindistan işleri, kutlama kartları, Bumba üyeliği (standart 5.18). Elle başlatılır, sahibin onayıyla; hesap ekranlı yapıdan önce hesap sunucusu canlıda olmalı; `MARKETING_VERSION` 1.0 kalır. (9 Kasım 2026)
- App Store 1.1 gönderimi (canlı sırası, bitti): yukarıdaki iOS işleri + 24 dil sürümünde mağaza metinleri (nötr adlar, yeni dil sürümleri en-GB, en-AU, en-CA, fr-CA, es-MX, pt-PT, zh-Hant, ms, ur-PK, bn-BD) ve ülkeye göre ekran görüntüsü sırası; sahibin onayıyla; göndermeden önce `MARKETING_VERSION` ve `Deliverfile` `app_version` 1.1, 24 dil sürümünde `release_notes.txt`, App Store Connect Uygulama Gizliliği (e-posta, ad, kullanıcı kimliği), App Review deneme hesabı (depoya yazılmaz). (13 Kasım 2026)
- Google Play ve Wear OS (canlı sırası, bitti: iOS'la aynı içerik, mağaza sayfası 23 dilde): şahıs şirketi adına kurum hesabı; D-U-N-S numarası bekleniyor (ücretsiz, 30 güne kadar). Hesap açılınca `mobile/android/PLAY_CONSOLE.md` beyanları ve ilk yayından sonra ülkeye özel mağaza sayfaları (`mobile/android/store-listings/`). (5 Kasım 2026)
- Tarayıcı eklentisinin mağazalara konması: "şimdilik gerek yok"; kod hazır ve testli. (6 Kasım 2026)
- App Store özel ürün sayfaları (`docs/urun-sayfalari-2026-11.md`): 1.1'den sonra, sahibin onayıyla. (10 Kasım 2026)
- Bağış (bahşiş) satın alması (2. rakip analizi #25): ayrıca değerlendirilecek. (10 Kasım 2026)
- Dil denetimi: 3 Kasım'dan sonra eklenen ur, fa, fr, ru, ja, ko, zh, fil, th metinleri (56 parça, ~15 milyon token) ve Bengalce; iptal edildi, sahip isterse ajanlarla. (10 Kasım 2026)
- Sınav takvimi: ancak sahip isterse ve küresel bir kapsamla. (9 Kasım 2026)
- Yeni dil ya da yeni kapsayıcı takvim: ancak sahip isterse (Ermeni takvimi bilerek yok). (10 Kasım 2026)

## Bu oturumda
- PR #60 birleşince: ana altyapı oturumuna hesap sunucusunun hazır olduğunu (dal, son commit, kontroller) yaz; sahibe sunucunun ilk yayınını ana altyapı oturumunda onaylamasını hatırlat. (10 Kasım 2026)
- App Store 1.1 ve ilk Play gönderiminden önce standart "Mobil yayın" kontrol listesini sahibe raporla. (14 Kasım 2026)
- Üyelik aşama 2 (ücretsiz, üyelik zorunlu): ortak zikir ve hatim, uyanma grupları, favori cami notlarının paylaşımı; sıra `docs/uyelik-plani-2026-11.md`'de. Sahibin "başla" demesini bekliyor. (10 Kasım 2026)

## Başkasında
- Ana altyapı oturumu: hesap sunucusunun (`hesap.time.bumba.tr`; canlı sırası, bitti: standart 5.18'e göre) ilk yayını ve sonra otomatik yayının açılması; PR #60'tan sonra, sahibin onayıyla. Coolify uygulaması `saat-hesap` kurulu, otomatik yayın kapalı. İki günlük görev (`POST /gorev/temizlik`, `POST /gorev/kutlama`) ilk yayında açılacak (sahibin onayı 14 Kasım 2026, merkeze iletildi). (10 Kasım 2026)
- Ana altyapı oturumu: yönetim paneli uçları (`/api/bumba-panel/ozet|uyeler|uye`) hazır; merkezdeki kayıt ve uçtan uca deneme. (14 Kasım 2026)
- Ana altyapı oturumu: kayıttaki onay cümlesinin 19 dildeki çevirisi iletildi; bumbagroup.com'daki yerinin belirlenmesi. (14 Kasım 2026)
- MEB 2027-2028 çalışma takvimi: yayımlanınca `src/data/school.json`. (6 Kasım 2026)
- Fitre 2027: Diyanet Ocak 2027'de açıklayınca `src/data/fitre.json`; BAZNAS 2027 kararları (zekât fitrah, fidye). Bakılacak tarih: Ocak 2027. (6 Kasım 2026)
- Diyanet 2027 takvimi ve vakit tablosu: yayımlanınca `holidays.json` ve `node scripts/fetch-hijri.mjs`. (8 Kasım 2026)
- Endonezya SKB 2028 (Eylül–Ekim 2027) ve Hindistan DoPT 2028 (Haziran–Temmuz 2027) tatil listeleri: `src/data/world-holidays-official.json`. (10 Kasım 2026)
- Tayland'ın 2030 resmî takvimi (Budist takvimi artık günü doğrulaması), Nepal'in 2084 BS takvimi, NAOJ'un yeni 暦要項'sı: yayımlanınca test verilerine. (10 Kasım 2026)
- WMM2030 (Aralık 2029 civarı): `src/js/wmm.js` katsayıları. (3 Kasım 2026)

