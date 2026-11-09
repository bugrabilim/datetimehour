# Endonezya araştırması: resmî din kurumunun çıktıları ve Endonezyalı kullanıcı (8 Kasım 2026)

Sahibin isteği: Saat Tarih Endonezya'da gerçekten işe yarasın. Özellikle ülkenin din kurumundan gelen bildirimler ve duyurular aktarılabilsin. Bu kurum Kementerian Agama RI (Kemenag), Ditjen Bimas Islam ve ilgili kurullardır.

Bu belge araştırmadır. Kod değişmedi, commit yapılmadı. Kaynaklar satır içinde verildi. **(doğrulanmadı)** işareti, birincil kaynakta görülmeyen ya da kaynakların çeliştiği bilgiyi gösterir.

Hesap denemeleri depodaki `src/js/prayer-calc.js` dosyasının geçici bir kopyasıyla yapıldı. Resmî vakitler Kemenag'ın SIHAT sayfasının kullandığı JSON ucundan elle alındı: toplam 20 istek, 10 il/ay (aşağıda 1.2). Betikler ve ham veri oturumun geçici klasöründe duruyor, depoya girmedi.

**Öne çıkanlar**
- **Endonezyalı kullanıcı bugün yanlış vakit görüyor.** Varsayılan yöntem her yerde Diyanet. Kemenag tablosuna göre Diyanet'te Subuh 6–7 dk geç, İsya 5–7 dk erken, Magrib 4–5 dk geç çıkıyor. Ayrı bir İmsak satırı da yok. Listedeki "Singapura" yöntemiyse Subuh, Zuhur, Asar, Magrib ve İsya'yı 2–4 dk erken, Terbit'i 3–4 dk geç veriyor; erken çıkan vakitlere iftar da dahil. Ölçüm 7 şehirde 213 gün üzerinden yapıldı (aşağıda 1.1).
- **Kemenag yöntemi cihazda ±1 dk ile kurulabiliyor.** Kurallar: Subuh −20°, İsya −18°, her vakte 2 dk ihtiyat, Zuhur'a 3 dk, Terbit'ten −2 dk. Vakit başlangıçları yukarı, Terbit aşağı yuvarlanıyor. İmsak = Subuh − 10 dk. Duha = Güneş +4,5° + 2 dk. Bu kurallarla 213 gün × 8 vakit = 1704 değerin %97'si birebir, hepsi ±1 dk içinde çıktı. Terbit ve Magrib için ufuk yüksekliği alçak yerlerde −1°, yaylalarda −2°. Bandung, Karo ve Jayawijaya'da bu ölçüldü. Kemenag bu yöntemi belge olarak yayımlamıyor; kurallar tersine mühendislikle bulundu.
- **Hicrî tarih Diyanet tablosundan geldiği için Endonezya'da sık sık bir gün kayıyor.** 2026'da 12 aydan 5'inin başı Kemenag'ın resmî takviminden farklı. Endonezya'da Ramazan Bayramı 2026'da 21 Mart'taydı, Diyanet'te 20 Mart. 2027 için SKB'ye göre Ramazan Bayramı 10 Mart, Kurban Bayramı 17 Mayıs; Diyanet'te 9 Mart ve 16 Mayıs. İki gün sonra (11 Kasım 2026) başlayan Cemaziyelahir 1448 de Endonezya'da Diyanet'ten bir gün sonra.
- **"Kurumdan gelen duyurular" yılda birkaç tarih olaydır ve çoğu önceden bilinir.** Sıralama şöyle:
  - Sidang isbat yılda üç kez yapılır: Ramazan, Şevval ve Zilhicce başı. Sonuç ilgili ayın 29'u akşamı yaklaşık 19.00 WIB'de açıklanır.
  - SKB 3 Menteri resmî tatilleri ve cuti bersama günlerini yılda bir kez belirler.
  - Kemenag'ın yıllık Kalender Hijriah Indonesia'sı.
  - Rashdul Qiblat yılda iki kez.
  - Salat gerhana çağrısı (tutulmalarda).
  - BAZNAS'ın zekât fitrah ve nisab kararları.

  Bunların hepsi sunucusuz aktarılabilir. Uygulama yerel bildirim kurar, durağan bir dosyayı okur ve verileri biz elle güncelleriz. Sidang isbat aktarımında tek darboğaz "her yayın sahibin onayıyla" kuralı (aşağıda 4.3).
- **Endonezya verisi hiç yok.**
  - Dünya tatillerinde (30 ülke) Endonezya yok. Nager.Date'in Endonezya verisi de yalnız 8 gün içeriyor; İslami bayramlar, Imlek, Nyepi, Waisak ve cuti bersama eksik.
  - 31 şehirlik listede Jakarta yok.
  - WIB, WITA ve WIT saat dilimi kısaltmaları yok.
  - Endonezce takvim sayfası Türkiye'nin tatillerini gösteriyor.
  - Zekât sayfası Endonezyalıya Türkiye'nin 240 TL'lik fitresini ve Diyanet'in 80,18 g nisabını gösteriyor. BAZNAS nisabı 85 g; 2026'da fitrah kişi başı Rp 50.000.

---

## 1. Resmî din kurumunun çıktıları ve bugün kullanıcıya nasıl ulaştıkları

### 1.1 Kemenag namaz vakti yöntemi

**Kurumun söylediği:**
- Subuh için Güneş'in −20°'de olduğu kriter kullanılıyor. Ditjen Bimas Islam Direktörü Kamaruddin Amin, Aralık 2020'de bu kriterin "fıkıh ve bilim açısından doğru" olduğunu söyledi. Dayanak, Labuan Bajo (2018) ve Banyuwangi'deki rukyat fajar gözlemleri ([NU Online](https://www.nu.or.id/nasional/kemenag-kriteria-waktu-subuh-20-derajat-benar-secara-fiqih-dan-sains-fDnQZ)).
- Temmuz 2022'de BRIN ile Timau Gözlemevi'nde (Kupang) yapılan gözlemde de Subuh vaktinin doğru olduğu sonucuna varıldı ([detik](https://inet.detik.com/science/d-6221630/pengamatan-di-observatorium-timau-buktikan-waktu-subuh-sudah-benar)).
- İsya −18° ve ihtiyat 2 dk akademik kaynaklarda Depag/Kemenag uygulaması olarak geçiyor ([UMSU Al-Marshad](https://jurnal.umsu.ac.id/index.php/almarshad/article/download/8200/6352)).
- Kemenag'ın bütün parametreleri (ihtiyat, ufuk, Duha, yuvarlama) tek bir resmî belgede yayımlanmıyor. Böyle bir belge bulunamadı **(doğrulanmadı)**.

**Bizim ölçtüğümüz:** Kemenag'ın yayımladığı aylık tablolar alındı:
- Kota Jakarta, Kasım ve Haziran 2026
- Kota Banda Aceh, Haziran 2026
- Kota Medan, Aralık 2026
- Kota Makassar, Aralık 2026
- Kota Kupang, Aralık 2026
- Kota Jayapura, Haziran 2026
- Yaylalardan Kab. Karo (Aralık), Kab. Jayawijaya (Haziran) ve Kota Bandung (Kasım)

Bu tablolar Kemenag'ın kendi kab/kota koordinatlarıyla (`lintang`, `bujur`) bizim formülümüze kondu.

| Vakit | Kemenag'a uyan kural (ampirik) | 7 alçak şehir, 213 gün |
|---|---|---|
| İmsak | Subuh − 10 dk | 213/213 kurala birebir uyuyor |
| Subuh | Güneş −20°, +2 dk, yukarı yuvarla | %98 birebir, hepsi ±1 dk |
| Terbit | Ufuk −1,0°, −2 dk, aşağı yuvarla | %99 birebir, hepsi ±1 dk |
| Duha | Güneş +4,5°, +2 dk, yukarı yuvarla | %99,5 birebir, hepsi ±1 dk |
| Zuhur | Meridyen geçişi +3 dk, yukarı yuvarla | %97 birebir, hepsi ±1 dk |
| Asar | Gölge 1 kat (Şâfiî), +2 dk, yukarı yuvarla | %92 birebir, hepsi ±1 dk |
| Magrib | Ufuk −1,0°, +2 dk, yukarı yuvarla | %99 birebir, hepsi ±1 dk |
| İsya | Güneş −18°, +2 dk, yukarı yuvarla | %98 birebir, hepsi ±1 dk |

**Yayla bulgusu:** Bandung (~700 m), Karo (~1.200 m) ve Jayawijaya'da (~1.550 m) Terbit ile Magrib 4–5 dk farklı çıktı. Ufuk −2,0° alınınca uydu: 59/60, 61/62 ve 59/60. −1,0 ile −2,0 dışındaki değerler belirgin biçimde kötüleşiyor. Demek ki Kemenag kab/kota başına iki sınıflı bir "ketinggian tempat" düzeltmesi uyguluyor. Eşik ve hangi kab/kota'nın yayla sayıldığı bilinmiyor **(doğrulanmadı)**. Subuh, Zuhur, Asar ve İsya yaylada da aynı kuralla birebir çıktı.

**Bizdeki yöntemlerin Kemenag'dan farkı** (dakika; artı = bizimki geç):

| Yöntem (`METHODS`) | Subuh | Terbit | Zuhur | Asar | Magrib | İsya |
|---|---|---|---|---|---|---|
| `diyanet` (varsayılan) | +6…+7 | −4…−3 | +1…+2 | +1…+3 | +4…+5 | −7…−5 |
| `singapore` (20°/18°, ihtiyatsız) | −3…−2 | +3…+4 | −4…−3 | −3…−2 | −4…−3 | −3…−2 |
| `mwl` | +6…+7 | +3…+4 | −4…−3 | −3…−2 | −4…−3 | −8…−6 |

Bu farkların sonuçları:
- **Diyanet:** Sahur, resmî Subuh'tan 6–7 dk sonrasına uzar. Resmî İmsak'a göre bu 16–17 dk eder. İsya da resmî vaktinden önce kılınır.
- **Singapura:** Vakit başlangıçları, iftar dahil, resmî vaktinden 2–4 dk önce gelir. Terbit 3–4 dk geç çıktığı için Subuh'un bitişi de geç görünür.
- **Diğer uygulamalar:** AlAdhan'ın `KEMENAG` yöntemi de yalnız 20°/18° tanımlıyor; ihtiyat ve yuvarlama yok ([api.aladhan.com/v1/methods](https://api.aladhan.com/v1/methods)). adhan-js'in `Singapore` yöntemi 20°/18°'e Zuhur +1 dk ve yukarı yuvarlama ekliyor ([adhan-js](https://raw.githubusercontent.com/batoulapps/adhan-js/master/src/CalculationMethod.ts)). Yani Kemenag tablosuna ±1 dk uyan bir yöntem, piyasadaki birçok uygulamadan daha doğru olur.

**Muhammadiyah farkı:** Munas Tarjih XXXI (2020) Subuh'u −18°'e aldı ve bu PP Muhammadiyah'ın 2021 kararıyla yürürlüğe girdi. Subuh ortalama ~8 dk geç çıkıyor ([CNN Indonesia](https://www.cnnindonesia.com/nasional/20210326114910-20-622392/muhammadiyah-resmi-koreksi-waktu-salat-subuh-mundur-8-menit/amp), [Kompas TV](https://www.kompas.tv/article/132347/respon-putusan-tarjih-muhammadiyah-soal-waktu-subuh-pihak-kemenag-mengaku-sudah-benar)). Kemenag −20°'de kaldı. Kasım 2026 tablosu da hâlâ −20° ile uyuşuyor. Muhammadiyah'ın İsya açısı ve ihtiyatı **(doğrulanmadı)**.

### 1.2 Resmî vakit ve imsakiye kaynakları, lisans

**SIHAT (Sistem Informasi Hisab Rukyat):**
- Bimas Islam'ın sitesinde il → kab/kota → ay/yıl seçilerek aylık tablo alınıyor ([bimasislam.kemenag.go.id/jadwalshalat](https://bimasislam.kemenag.go.id/jadwalshalat)).
- Sütunlar: imsak, subuh, terbit, dhuha, dzuhur, ashar, maghrib, isya. Sayfada "Sumber: SIHAT Kemenag RI" yazıyor.
- Sitenin menüsünde ayrıca "Imsakiyah" ve "Arah Kiblat" sayfaları var.
- Sayfa Next.js uygulaması. Arka planda belgelenmemiş `/api/sihat/provinsi`, `/api/sihat/kabko?prov=…`, `/api/sihat/jadwal?prov=…&kabko=…&thn=…&bln=…` uçlarını kullanıyor. Yanıt JSON: kab/kota koordinatı derece-dakika-saniye olarak ve günlük 8 vakit.
- Sayfada Cloudflare Turnstile anahtarı da gömülü.

**Kullanım koşulları:**
- Sitede kullanım koşulları sayfası bulunamadı. Altbilgide "© 2026 Bimas Islam", gizlilik ve çerez bağlantıları var.
- `bimasislam.kemenag.go.id/robots.txt` bütün tarayıcılara `Disallow: /api/` ve `Disallow: /_next/` diyor. CCBot tamamen yasaklı.
- **Sonuç:** Ürün bu ucu çağırmamalı. Vakitleri cihazda kendimiz hesaplarız; tablolar yalnız doğrulama içindir ve elle ya da izinle alınır.
- Bu araştırmada doğrulama için uçtan 20 istek yapıldı; bir daha tekrarlanmamalı.

**Liste eksikleri:** SIHAT il listesinde 37 il ve bir "PUSAT" kaydı var. Papua Pegunungan boş dönüyor; Jayawijaya hâlâ "PAPUA" altında. Yani 2022 bölünmesi yansımamış. Kendi yer listemiz bu adlarla eşlenirken dikkat gerekir.

**Yasal çerçeve:** UU 28/2014 (Telif Kanunu) Madde 42, açık devlet toplantılarının sonuçları, mevzuat, resmî konuşmalar ve kutsal kitaplar için telif tanımıyor ([Wikisource metni](https://id.wikisource.org/wiki/Undang-Undang_Republik_Indonesia_Nomor_28_Tahun_2014), [Commons PD-ID-exempt](https://commons.wikimedia.org/wiki/Template:PD-ID-exempt/id)). Namaz vakti tabloları için hüküm açık değil. Hesaplanmış tarihler ve vakitler zaten olgudur. Yine de bu bir hukuki görüş değildir **(doğrulanmadı)**.

**Resmî olmayan aynalar:** Örneğin `api.myquran.com` Kemenag verisini yansıttığını söylüyor; dakikada 5 istek sınırı var ([Docker Hub](https://hub.docker.com/r/banghasan/apimuslim3)). Lisansı belirsiz, kullanılmamalı.

**Kemenag uygulamaları:**
- **Pusaka:** Kemenag'ın "süper uygulaması". App Store'da 4 oyla 3,0 puanı var; ağırlığı personel devamı ve din bilgisinde. Sürüm geçmişinde vakit bildirimi ve cami konumu düzeltmeleri geçiyor ([App Store](https://apps.apple.com/us/app/-/id6443819441)). Güçlü bir rakip değil.
- **Qur'an Kemenag:** LPMQ'nun uygulaması ve web sitesi [quran.kemenag.go.id](https://quran.kemenag.go.id/). Resmî Endonezce meal burada.

### 1.3 Sidang isbat: Ramazan, Şevval ve Zilhicce başı

**Nasıl işliyor:**
- Ayın 29'unda yapılır. Sırasıyla: hilal konumu semineri (16.00–16.30 WIB, açık), Magrib'den sonra kapalı oturum (~18.00), basın toplantısı (~19.00 WIB).
- Seminer Bimas Islam TV YouTube kanalında ve Kemenag'ın sosyal medya hesaplarında yayınlanır. Bazı kaynaklar seminerin 16.00'da başladığını yazıyor ([RCTI+ / iNews](https://www.rctiplus.com/news/detail/inews/5384364/jadwal-dan-tahapan-sidang-isbat-iduladha-2026-hari-ini--hilal-bakal-terlihat-), [CNBC Indonesia](https://www.cnbcindonesia.com/news/20260319063611-4-720190/sidang-isbat-penetapan-lebaran-2026-mulai-jam-berapa-ini-jadwalnya/amp)).
- 19.00 WIB, Türkiye saatiyle 15.00'e denk gelir.
- Rukyat 88–117 noktada yapılıyor ([Antara](https://megapolitan.antaranews.com/berita/513694/pemantauan-hilal-1-syawal-1447-hijriah-dilaksanakan-di-117-titik-di-indonesia)).
- Kriter **MABIMS**: hilal yüksekliği ≥3°, uzanım ≥6,4°. 2022'den beri uygulanıyor ([CNN Indonesia](https://www.cnnindonesia.com/nasional/20220223192803-20-763150/kriteria-baru-kemenag-untuk-awal-bulan-hijriah-tinggi-hilal-3-derajat)).

**1447 H sonuçları:**

| Ay | Kemenag (isbat) | Muhammadiyah | Diyanet (bizdeki veri) |
|---|---|---|---|
| 1 Ramazan | 19 Şubat 2026 ([Antara](https://jogja.antaranews.com/berita/806358/pemerintah-tetapkan-awal-ramadhan-2026-jatuh-pada-kamis)) | 18 Şubat (KHGT) | 19 Şubat |
| 1 Şevval (Idul Fitri) | **21 Mart 2026** ([Metro TV](https://www.metrotvnews.com/read/NgxCaa29-pemerintah-tetapkan-1-syawal-1447-h-jatuh-pada-21-maret-2026), [CNA](https://www.cna.id/indonesia/hasil-sidang-isbat-idul-fitri-1447-h-jatuh-pada-sabtu-21-maret-2026-45496)) | 20 Mart | **20 Mart** |
| 1 Zilhicce / Iduladha | 18 Mayıs / 27 Mayıs 2026 ([Bisnis](https://kabar24.bisnis.com/read/20260517/15/1974295/hasil-sidang-isbat-1-zulhijah-jatuh-pada-senin-18-mei-2026-iduladha-27-mei-2026)) | 27 Mayıs | 18 Mayıs / 27 Mayıs |

**1448 H beklenen tarihler (tahmin):**
- 1 Ramazan için BRIN'in tahmini 8 Şubat 2027; isbat 7 Şubat akşamı olur ([Metro TV](https://www.metrotvnews.com/read/KYVC0XgP-prediksi-awal-ramadan-dan-lebaran-2027-versi-brin)).
- SKB 2027, Idul Fitri'yi 10–11 Mart, Iduladha'yı 17 Mayıs olarak koydu (1.6). Buna göre isbatlar yaklaşık 8 Mart ve 7 Mayıs akşamı **(tahmin)**.

**Nereden okunur:**
- Kemenag haberleri `kemenag.go.id` üzerinde. `robots.txt` her şeye, `/api/` dahil, izin veriyor ve Google News biçiminde [sitemap-articles.xml](https://kemenag.go.id/sitemap-articles.xml) yayımlıyor.
- `kemenag.go.id/rss` HTTP 500 dönüyor ve içinde hiç öğe yok (8 Kasım 2026'da denendi). Kullanılabilir bir RSS yok.
- Bimas Islam'da `/rss` ve `/feed` 404.
- Sonuç ayrıca ana haber ajanslarında (Antara, RRI) dakikalar içinde çıkıyor.

**Uygulama nasıl aktarabilir:** Sonuç bir tarih ve bir cümledir: "Pemerintah menetapkan 1 Syawal 1447 H jatuh pada Sabtu, 21 Maret 2026". Metni kopyalamak gerekmez; olgu yazılır, kaynağa bağlantı verilir. Seçenekler 4.3'te.

### 1.4 Muhammadiyah ve NU

**Muhammadiyah:**
- 1 Muharrem 1447'den beri **KHGT** (Kalender Hijriah Global Tunggal) kullanıyor. Kararı PP Muhammadiyah 22 Ocak 2025'te verdi, takvim 25 Haziran 2025'te tanıtıldı ([tirto](https://tirto.id/apa-itu-khgt-bisakah-menyatukan-kalender-hijriah-umat-islam-hrfh), [detik](https://www.detik.com/hikmah/khazanah/d-7848676/lebaran-2025-muhammadiyah-terakhir-pakai-hisab-tahun-depan-mengacu-khgt)).
- Eski yöntem hisab hakiki wujudul hilaldi; 1446 Bayramı onunla belirlenen son bayramdı. Bazı haberler 1447 Şevval'i hem wujudul hilal hem KHGT diye anlatıyor **(çelişkili)**.
- KHGT tek dünya takvimi olduğu için Kemenag'dan sık sık bir gün önce başlar.

**NU:**
- Rukyat esaslı. Lembaga Falakiyah PBNU'nun kriteri 3°/6,4°, yani MABIMS'le aynı (SK No. 001/SK/LF–PBNU/III/2022) ([NU Online](https://www.nu.or.id/nasional/rakernas-falakiyah-nu-tegaskan-imkan-rukyah-3-derajat-dan-elongasi-6-4-derajat-0QW1j)).
- Ramazan, Şevval ve Zilhicce'de ikhbar yayımlıyor; 1447 Şevval'i 21 Mart bekliyordu ([iNews](https://www.inews.id/news/nasional/pbnu-prediksi-idulfitri-1447-h-sabtu-21-maret-2026-ramadan-digenapkan-30-hari/all)).
- Pratikte hükümetle aynı günü buluyor. Diğer aylarda Kemenag takviminden ayrıldığı olup olmadığı **(doğrulanmadı)**.

### 1.5 Resmî hicrî takvim: Kalender Hijriah Indonesia

**Yayın:**
- Ditjen Bimas Islam her yıl "Kalender Hijriah Indonesia" yayımlıyor. 2026 baskısı PDF olarak 14 Ekim 2025'te çıktı; Recep 1447'den Cemaziyelahir 1448'e kadar ([detik](https://news.detik.com/berita/d-8163340/kalender-hijriah-2026-resmi-kemenag-isi-dan-link-unduh-pdf), [PDF](https://aceh.kemenag.go.id/storage/berita_informasi/01K7GE96M0ZVRJ62Z520RMYG1B.pdf)).
- Takvimde her günde miladi tarih, hicrî gün (Arapça rakamla) ve **Cava takvimi** var: pasaran günü (Legi, Pahing, Pon, Wage, Kliwon), Cava ayı ve yılı (ör. "Rejeb – Ruwah 1959 (Dal)").
- Ramazan, Şevval ve Zilhicce için "isbatı bekleyin" notu var.
- 2027 baskısı bulunamadı **(doğrulanmadı)**.

**Kemenag 2026 ay başları ile bizdeki Diyanet tablosu** (`src/data/hijri.json`):

| Hicrî ay | Kemenag 2026 takvimi | Diyanet (bizde) |
|---|---|---|
| Şaban 1447 | 20 Ocak | 20 Ocak |
| Ramazan | 19 Şubat | 19 Şubat |
| Şevval | **21 Mart** | 20 Mart |
| Zilkade | **19 Nisan** | 18 Nisan |
| Zilhicce | 18 Mayıs | 18 Mayıs |
| Muharrem 1448 | 16 Haziran | 16 Haziran |
| Safer | **16 Temmuz** | 15 Temmuz |
| Rebiülevvel | 14 Ağustos | 14 Ağustos |
| Rebiülahir | **13 Eylül** | 12 Eylül |
| Cemaziyelevvel | 12 Ekim | 12 Ekim |
| Cemaziyelahir | **11 Kasım** | 10 Kasım |
| Recep | 10 Aralık | 10 Aralık |

- 12 ayın 5'inde bir gün fark var. O aylarda 29–30 gün boyunca hicrî tarih bir gün önde görünür. 2026'da bu ~150 gün eder. Ayyamul Bidh (13–15) ve diğer sünnet oruç hatırlatmaları da kayar.
- **Cava pasaranı** basit bir hesap: `["Legi","Pahing","Pon","Wage","Kliwon"][JDN % 5]`. JDN, Jülyen gün numarasıdır. 1 Ocak 2026 (JDN 2461042) Pon, 2 Ocak Wage çıkar; ikisi de Kemenag takvimine uyuyor.

### 1.6 Resmî tatiller ve cuti bersama (SKB 3 Menteri)

**2026:** Din İşleri, Çalışma ve Kamu Yönetimi bakanlarının ortak kararı (SKB 1497/2025, 2/2025, 5/2025). 19 Eylül 2025'te imzalandı: 17 resmî tatil, 8 cuti bersama ([Setneg](https://www.setneg.go.id/baca/index/inilah_skb_3_menteri_libur_nasional_dan_cuti_bersama_2026), [PDF](https://www.kemenkopmk.go.id/sites/default/files/pengumuman/2025-09/SKB%20Libur%20Nasional%20dan%20Cuti%20Bersama%20Tahun%202026.pdf)).
- **Resmî tatil:** 1 Ocak, 16 Ocak (Isra Mikraj), 17 Şubat (Imlek), 19 Mart (Nyepi), 21–22 Mart (Idul Fitri), 3 Nisan, 5 Nisan, 1 Mayıs, 14 Mayıs, 27 Mayıs (Iduladha), 31 Mayıs (Waisak), 1 Haziran, 16 Haziran (1 Muharrem), 17 Ağustos, 25 Ağustos (Maulid), 25 Aralık.
- **Cuti bersama:** 16 Şubat, 18 Mart, 20, 23 ve 24 Mart, 15 Mayıs, 28 Mayıs, 24 Aralık.

**2027:** SKB 1205/2026, 3/2026, 2/2026; koordinasyon toplantısı 15 Eylül 2026. 18 resmî tatil, 8 cuti bersama ([Kemenko PMK](https://kemenkopmk.go.id/pemerintah-tetapkan-18-hari-libur-nasional-dan-8-cuti-bersama-tahun-2027), [Setneg](https://setneg.go.id/baca/index/inilah_skb_3_menteri_libur_nasional_dan_cuti_bersama_2027)). SKB numaraları kaynaklarda farklı yazılıyor (1205 ve 1025) **(doğrulanmadı)**.
- **Resmî tatil:** 1 Ocak, 5 Ocak (Isra Mikraj 1448), 6 Şubat (Imlek), 8 Mart (Nyepi), 10–11 Mart (Idul Fitri 1448), 26 Mart, 28 Mart, 1 Mayıs, 6 Mayıs, 17 Mayıs (Iduladha), 20 Mayıs (Waisak), 1 Haziran, 6 Haziran (1 Muharrem 1449), 15 Ağustos (Maulid), 17 Ağustos, 25 Aralık, 26 Aralık (Isra Mikraj 1449).
- **Cuti bersama:** 5 Şubat, 9, 12 ve 15 Mart, 25 Mart, 18 Mayıs, 19 Mayıs, 24 Aralık.
- Cuti bersama günleri devlet memurunu bağlar; özel sektörde yıllık izinden düşülür ve işverene bağlıdır ([Katadata](https://katadata.co.id/berita/industri/6aaa599202c89/skb-libur-dan-cuti-bersama-2027-terbit-menaker-beberkan-hak-pekerja)). Veri modelinde ayrı tür olmalı.
- **Nager.Date** Endonezya için yalnız 8 gün veriyor (1 Ocak, Paskah ve Kutsal Cuma, 1 Mayıs, Göğe Yükseliş, 1 Haziran, 17 Ağustos, Noel). İslami bayramlar, Imlek, Nyepi, Waisak ve cuti bersama yok ([API](https://date.nager.at/api/v3/PublicHolidays/2026/ID), 8 Kasım 2026'da denendi). Mısır'da olduğu gibi kullanılamaz. Veri SKB'den elle girilmeli.

### 1.7 MUI fetvaları

- **Fatwa MUI No. 2/2004:** Ramazan, Şevval ve Zilhicce başı, hükümetin hisab ve rukyat yoluyla yaptığı belirlemeye bırakılıyor. Dayanağı 2003 İjtimak Ulema kararıdır: "bütün Müslümanlar hükümetin kararına uymakla yükümlüdür" ([analiz PDF](https://media.neliti.com/media/publications/41845-ID-analisis-fatwa-mui-nomor-2-tahun-2004-tentang-penetapan-awal-ramadhan-syawal-dan.pdf), [Ijtima' metni](https://fatwamui.com/storage/406/Ijtima'-Ulama-Tentang-Penetapan-Awal-Ramadhan-Syawal-dan-Dzulhijah.pdf)).
- Mart 2026'da MUI fetva işleri başkanı bunu yineledi ([RRI](https://rri.co.id/nasional/2273912/mui-tegaskan-kewenangan-negara-tetapkan-awal-syawal-lewat-sidang-isbat)).
- Muhammadiyah bu fetvayı uygulamıyor.
- **Ürün için anlamı:** Endonezya'da varsayılan takvim hükümetin takvimi olmalı (Kemenag ve isbat). Muhammadiyah ve NU tarihleri en fazla bilgi olarak gösterilebilir.
- 2020'lerde namaz vakti düzeltmesiyle ilgili bir MUI fetvası bulunamadı **(doğrulanmadı)**. Subuh tartışması Kemenag ile Muhammadiyah arasında (1.1).

### 1.8 Kurumların diğer duyuruları

- **Rashdul Qiblat:** Güneş'in Kâbe'nin tam üstünde olduğu an; Kemenag her yıl "kıbleyi denetleyin" çağrısı yapıyor. 2026'da 27–28 Mayıs 16.18 WIB ve 15–16 Temmuz 16.27 WIB'deydi. Temmuz'da 725.669 noktada denetim yapıldı ([Antara Jogja](https://jogja.antaranews.com/berita/831064/kemenag-mengajak-umat-verifikasi-arah-kiblat-pada-27-28-mei-2026), [Metro TV](https://www.metrotvnews.com/read/kj2CD92M-matahari-di-atas-kabah-menag-imbau-masyarakat-cek-arah-kiblat-15-16-juli), [gebrak.id](https://www.gebrak.id/2026/07/gerakan-nasional-verifikasi-arah-kiblat-diikuti-di-725-ribu-lokasi-kemenag-antusiasme-masyarakat-sangat-tinggi.html)). Bizim Güneş formülümüz aynı anları veriyor: 2026 için 28 Mayıs 16.18 WIB ve 15 Temmuz 16.27 WIB; 2027 için de 28 Mayıs 16.18 ve 16 Temmuz 16.27 WIB. Günlük yerel "kıble saati" bizde zaten var.
- **Salat gerhana:** Kemenag tutulmalarda namaz çağrısı yapıyor, ör. Ay tutulması 3 Mart 2026. Saatler BMKG'den ([Kompas TV](https://www.kompas.tv/kalam-hati/654157/gerhana-bulan-total-3-maret-2026-ini-jadwal-dan-tata-caranya), [Setkab](https://setkab.go.id/ada-gerhana-bulan-total-kemenag-ajak-umat-islam-laksanakan-salat-khusuf/)).
- **BAZNAS** (devletin zekât kurumu):
  - 2026 zekât fitrah kişi başı **Rp 50.000**, yaklaşık 2,5 kg ya da 3,5 litre pirinç (Keputusan Ketua BAZNAS 14/2026). Fidyah günlük Rp 65.000.
  - Gelir zekâtı nisabı **85 g altın** ölçüsüyle ayda Rp 7.640.144 (SK 15/2026).
  - İl ve ilçe BAZNAS'ları fitrahı farklı belirleyebiliyor ([Bisnis](https://kabar24.bisnis.com/read/20260218/243/1953554/zakat-fitrah-2026-resmi-rp50000-ini-syarat-dan-tata-cara-bayarnya), [Warta Ekonomi](https://wartaekonomi.co.id/read602278/baznas-tetapkan-nisab-zakat-2026-naik-7-sesuai-kenaikan-upah-nasional-gaji-764-juta-wajib-zakat), [RMOL](https://rmol.id/nusantara/read/2026/03/06/699697/besaran-zakat-fitrah-2026-di-jabodetabek-ini-daftarnya)).
- **Saat dilimleri:** Keppres 41/1987 (1 Ocak 1988'den beri) üç dilim belirliyor: UTC+7, +8 ve +9. "WIB", "WITA" ve "WIT" kısaltmaları kararnamede geçmiyor ama her yerde böyle yazılıyor ([PDF](https://www.peraturan.go.id/files/kp41-1987.pdf), [tz listesi](https://mm.icann.org/pipermail/tz/2017-January/024784.html)). Endonezyalılar saati "pukul 04.09 WIB" biçiminde, iki nokta yerine noktayla yazar; `Intl` `id-ID` de "04.09" veriyor.

---

## 2. Endonezyalı kullanıcı ne bekliyor?

| Uygulama | Ölçek | Öne çıkanlar | Şikâyetler, notlar |
|---|---|---|---|
| **Muslim Pro** (Bitsmedia) | Play'de 100 mn+ indirme, 4,3 puan, 1,92 mn yorum; reklamlı, uygulama içi satın alma ([Play](https://play.google.com/store/apps/details?id=com.bitsmedia.android.muslimpro&hl=id&gl=ID)) | "Doğrulanmış vakit", ezan bildirimi, Kur'an ses ve meal, kıble, tesbih, dua, helal restoran, cami, Umre | 995 yorumluk bir çalışmada en sık şikâyetler: rahatsız edici reklam, sık bozulan vakit uyarısı, kendiliğinden oturum kapanması, sürüm sorunları ([UIN Suska](https://repository.uin-suska.ac.id/82270/)). Bir yorumcu, vakti görmek için önce reklam izlemek zorunda kaldığını yazıyor. 2020'de verinin ABD'li bir aracıya satıldığı haberi tartışıldı, Kominfo inceledi ([Bisnis](https://teknologi.bisnis.com/read/20201121/84/1320604/apa-itu-aplikasi-muslim-pro-yang-disorot-kemenkominfo), [detik](https://inet.detik.com/mobile-apps/d-5258593/aplikasi-muslim-pro-jual-data-ini-7-alternatif-penggantinya)) |
| **NU Online** (NU) | 1 mn+ indirme, 4,8 puan, 33,9 bin yorum; "veri toplanmaz" beyanı ([Play](https://play.google.com/store/apps/details?id=id.or.nu.app&hl=id&gl=ID)) | Vakitlerde **imsak, dhuha ve terbit**; bildirimde **bedug, kentongan** ya da ezan sesi; hicrî takvimde **Cava pasaranı**, İslami günler, **sünnet oruç takvimi**, resmî tatiller ve NU günleri; **Yasin ve Tahlil**, wirid, salavat, ratib, Mevlid metinleri; Şâfiî zekât ve miras hesabı; kıble kamerası; hutbe | Yorumlarda teheccüt vakti için alarm (başlangıç ve bitiş) isteği var; Wi-Fi'da makaleler açılmıyor |
| **Al Quran Indonesia** (Andi Unpam) | 50 mn+ indirme, ~4,9 puan (2023) ([CNN Indonesia](https://www.cnnindonesia.com/teknologi/20230411145632-185-936284/daftar-aplikasi-pengingat-salat-hingga-penunjuk-kiblat)) | Vakitler **Kemenag'a uyumlu**, ezan alarmı, 30 günlük imsakiye, internetsiz | |
| **Usholli** | | Vakitler Kemenag portalından, reklamsız, cami haritası, Android widget ([detik](https://inet.detik.com/mobile-apps/d-7949290/aplikasi-cek-waktu-puasa-dan-salat-terbaik-gratis-akurat)) | |
| **Pusaka** (Kemenag) | Zayıf (1.2) | Altı dinin bilgileri, personel devamı | |

**Beklenen temel özellikler** (yukarıdaki uygulamalardan ve Kemenag tablolarından):
1. **Vakit Kemenag tablosuyla aynı dakikayı göstersin.** Camiler Kemenag ya da il Kemenag tablolarına göre ezan okuyor. Bir dakikalık fark bile "uygulama yanlış" diye algılanıyor. Al Quran Indonesia ve Usholli bunu öne çıkarıyor.
2. **Sekiz satır:** Imsak, Subuh, Terbit, Dhuha, Zuhur, Asar, Magrib, Isya. Saatin yanında "WIB", "WITA" ya da "WIT".
3. **Kab/kota seçimi:** SIHAT'ta il başına 2 (DKI Jakarta) ile 33 (Kuzey Sumatra) arasında kab/kota görülüyor. Kemenag tabloları kab/kota başınadır.
4. **İmsakiye** (Ramazan çizelgesi), Ramazan'da imsak ve iftar sayacı.
5. **Hükümetin hicrî takvimi:** isbat sonrası doğru gün, Endonezce tatil adları, resmî tatil ve cuti bersama ayrımı. NU kullanıcıları için Cava pasaranı.
6. **Sünnet oruç hatırlatmaları:** Pazartesi–Perşembe, Ayyamul Bidh (13–15), Arefe, Tasua ve Aşure, Şevval'de altı gün. NU Online'da var.
7. **Duaların zenginliği:** günlük dualar, Yasin, Tahlil, wirid, Esmaül Hüsna, Endonezce meal. Kemenag meali LPMQ'nundur.
8. **Bildirim sesi:** ezanın yanında bedug ve kentongan; ezanın tamamı. Kayıtların lisans sorunu ilk analizde ele alındı.
9. **Reklamsız ve verimi gizli** olması bir fark yaratır (Muslim Pro tartışması). Saat Tarih'in "hesap, reklam ve takip yok" ilkesi Endonezya'da güçlü bir mesaj.

---

## 3. Boşluk analizi (depo, 8 Kasım 2026)

**Var olan ve işe yarayan:** Endonezce arayüz (`src/i18n/id.json`, `mobile/shared/app-strings.json`), konum seçici (cihazın saat dilimiyle), 14 hesap yöntemi seçimi, kıble ve günlük kıble saati, tutulma verisi ve haritası, zikirmatik, Kur'an'ın Arapça metni, hicrî widget, imsakiye görseli, bildirim kategorileri ve gizlilik.

**Eksik ya da yanlış:**

1. **Kemenag yöntemi yok.**
   - `src/js/prayer-calc.js` `METHODS` 14 yöntem içeriyor; `singapore` 20°/18°, ihtiyatsız ve yuvarlamasız. Swift ve Kotlin'de birebir aynısı var: `mobile/android/core/.../PrayerTimes.kt` `METHOD_IDS`.
   - Yuvarlama her vakitte `Math.round` (`src/js/prayer.js` `dayTimes`). Kemenag ise başlangıçları yukarı, Terbit'i aşağı yuvarlıyor.
   - Ufuk her yerde sabit 0,833°; Kemenag'da −1° ya da −2°.
   - `PRAYER_KEYS` 6 vakit; **İmsak (−10 dk) ve Duha satırı yok**.
   - Endonezce metinde `names.fajr` "Subuh" (bu doğru), ama "Sahur (imsak)" ifadesi Endonezya anlamındaki imsakla karışır.
2. **Varsayılan yöntem konumdan bağımsız olarak Diyanet:** sitede `prayer.js` 31. satır, Android'de `PrayerExtras.kt` `PrayerMethod.id`, iOS'ta `PrayerExtras.swift`. Endonezya'daki kullanıcı yöntemi kendisi bulup değiştirmezse 1.1'deki Diyanet farklarını görür.
   - CLAUDE.md'ye göre widget'lar, saat uygulamaları ve açık veri Diyanet'te kalıyor.
   - Apple Watch'ta `Watch/Sources/PhonePlace.swift` `PrayerSchedule(latitude:longitude:timeZone:)` yöntem almıyor.
   - Kemenag seçen bir kullanıcı da saatinde 6–7 dk farklı Subuh görür.
3. **Hicrî takvim yalnız Diyanet** (`src/data/hijri.json`, `lib/hijri.mjs`).
   - 2026'da Endonezya'yla 5 ayda bir gün fark var (1.5).
   - İmsakiye ve Ramazan aralığı `holidays.json` `ramadans`'tan geliyor. 1447 için 19 Şubat–19 Mart, yani 29 gün; Endonezya'da Ramazan 30 gündü ve 20 Mart'ta bitti. İmsakiyenin son günü eksik kalırdı.
   - Bayram tarihleri 2026 Şevval'de ve 2027 Şevval ile Zilhicce'de Endonezya'dan bir gün önce.
   - Hilal kartı yalnız Yallop q'yu gösteriyor; MABIMS'in 3°/6,4° koşulu hesaplanmıyor.
4. **Takvim ve tatiller Türkiye'nin.**
   - Endonezce takvim sayfasının başlığı "Kalender {year}: Hari Libur Resmi Turki dan Ramadan".
   - Olay adları Türk kandilleri: "Malam Ragaib (Regaib Kandili)" vb.
   - `world-holidays.json`'da 30 ülke var, Endonezya yok. Nager verisi de yetersiz (1.6).
   - Uygulama takviminin ülke seçiminde (`cal.country`) Endonezya yok.
5. **Yer verisi.**
   - Konum penceresinde yalnız Türkiye'nin 81 ili ve "konumumu kullan" var.
   - `places.json`'da 48 Endonezya yeri var, kab/kota listesi yok.
   - **Veri hatası:** Singaraja (Bali, −8,12 / 115,09) `Asia/Jakarta` yazılmış, doğrusu `Asia/Makassar` (WITA, +1 saat). Kürede ve dünya saatlerinde yanlış saat gösterir.
   - Tanjung Pinang `Asia/Pontianak` (ofset doğru, UTC+7). Jayapura listede yok.
   - `cities.json`'da 31 şehir var, Jakarta yok.
   - `TZ_ABBRS`'ta (25 kısaltma, `build.mjs` ~2064) WIB, WITA ve WIT yok.
6. **Türkiye'ye özgü öğeler Endonezyalıya da gösteriliyor.**
   - Vakit sayfalarında "bayram namazı = güneş + 38 dk (Diyanet)" (`BAYRAM_MIN`). Endonezya'da bayram namazı saatini her cami kendisi belirler.
   - Ezani saat.
   - Türkiye Takvimi'ne dayanan ek vakit notları.
   - Zekâtta nisab 80,18 g (`src/js/zakat.js`) ve fitre "240 TL, Diyanet" (`src/data/fitre.json`, `zakat.fitreText`).
7. **Kur'an'ın Endonezce meali yok.** Endonezce arayüzde Pickthall'ın İngilizce meali açılabiliyor. Türkçe ve Azerice arayüzdeki "resmî meal sitesine bağlantı" düzeni Endonezce için kurulmamış (Kemenag ya da LPMQ'ya bağlantı yok).
8. **Kurum duyurusu akışı yok:** sidang isbat, SKB, Rashdul Qiblat ya da salat gerhana. Uygulamalar sunucusuz; sitenin her yayını sahibin onayını istiyor (CLAUDE.md, Genel Standartlar).

---

## 4. Öneriler

Efor: K küçük, O orta, B büyük. "Hepsi" site, iOS ve Android demek; eklenti, saat uygulamaları ve gerekiyorsa açık veri de buna dahil.

| # | İş | Efor | Platform | Veri kaynağı ve lisans | Risk |
|---|---|---|---|---|---|
| **1** | **Kemenag yöntemi** (`kemenag`): 20°/18°; ihtiyat Subuh, Asar, Magrib ve İsya'da +2 dk, Zuhur'da +3, Terbit'te −2; yöntem başına yuvarlama (başlangıç yukarı, Terbit aşağı); ufuk −1° ya da −2°; İmsak = Subuh − 10 dk ve Duha (+4,5°, +2 dk) satırları. JS, Swift ve Kotlin birebir; test verisi `prayer-methods.json`'a | O | Hepsi | Kendi hesabımız. Kemenag tablosu yalnız doğrulamada | Yayla eşiği bilinmiyor (−1°/−2°); ±1 dk kalıntı (düşük duyarlıklı Güneş formülü, Kemenag'ın kab/kota koordinatı); resmî belge yok. Kemenag'dan parametre teyidi istenmeli |
| **2** | **Konuma göre varsayılan yöntem:** konum Endonezya'daysa (saat dilimi Asia/Jakarta, Asia/Pontianak, Asia/Makassar ya da Asia/Jayapura; ya da ülke sınırı) bir kez "Kemenag'ı kullan?" sorulsun. Yapılan seçim widget, Watch, Wear, Live Activity ve bildirimlere geçsin. Aynısı Malezya (JAKIM), Singapur (MUIS) ve Brunei için de düşünülebilir | K–O | Hepsi | Yok | Yöntemi elle seçen kullanıcı bozulmamalı. Widget ve Watch'ta yöntem şu an hiç aktarılmıyor |
| **3** | **Endonezya hicrî tablosu** `hijri-id.json`: Kalender Hijriah Indonesia'dan ay başları (2026 PDF elle aktarılır), üstüne isbat sonuçları. Takvim seçimi: Diyanet / Kemenag (Endonezya) / Umm al-Qura; konuma göre varsayılan. Hicrî tarih, widget, imsakiye aralığı, takvim ve sünnet oruçlar bundan beslenir | O | Hepsi | Kemenag PDF'i (olgu, kaynak satırıyla). 2027 baskısı çıkınca güncellenir. Çıkana kadar MABIMS ile kendi hesabımız "tahmini" işaretli | Takvim yayımlanmazsa kendi MABIMS hesabımız gerekir (#12). İki tablo yönetmek bakım yükü (yıllık görev) |
| **4** | **Endonezya tatilleri** `holidays-id.json`: SKB 2026 ve 2027; resmî tatil ve cuti bersama ayrı türlerle, Endonezce adlarıyla. `/id/public-holidays/indonesia/` (ve diğer diller), uygulama takvimine "Indonesia" ülkesi, .ics akışı | K–O | Hepsi | SKB PDF'leri (Setneg, Kemenko PMK); mevzuat telifsiz | SKB yıl içinde değişebiliyor (cuti bersama revizyonu). Kaynak her yıl Eylül'de okunmalı |
| **5** | **Sidang isbat aktarımı:** isbat günleri takvimden bilinir. Uygulama o akşam "Pemerintah menetapkan …" yerel bildirimi gösterir, isbattan önce "tahmini" notu. Tasarım 4.3'te | O | Hepsi | Kemenag basın açıklaması (olgu ve bağlantı) | Yayın onayı darboğazı; gece hatası olursa yanlış bayram bildirimi. Elle onaysız yayın yok |
| **6** | **Rashdul Qiblat bildirimi** (yılda iki kez, kıble ekranında da) | K | Hepsi | Kendi hesabımız (1.8; Kemenag 2026 ile aynı dakika) | Düşük |
| **7** | **Salat gerhana notu:** Endonezya'dan görülen tutulmalarda vakit ve namaz notu | K | Hepsi | Elimizdeki `eclipses.json` (NASA) | Görünürlük hesabı yerel; Kemenag'ın kendi duyurusuyla çelişmemeli |
| **8** | **Sünnet oruç hatırlatmaları** (Pazartesi–Perşembe, Ayyamul Bidh, Arefe, Tasua ve Aşure, Şevval'de altı gün; oruç yasak günlerinde uyarı) | K–O | iOS, Android (sitede takvim işareti) | Hicrî tablo (#3) | #3 olmadan Endonezya'da her yıl ~5 ay yanlış gün. Varsayılanın açık olup olmayacağına sahip karar verir |
| **9** | **Endonezya zekâtı:** nisab 85 g (BAZNAS), fitrah Rp 50.000 (2026) ve pirinç ölçüsü, fidyah Rp 65.000, gelir zekâtı nisabı; "il BAZNAS'ı farklı olabilir" notu; Endonezce sayfada Türkiye fitresi gösterilmesin | K–O | Hepsi | BAZNAS kararları (olgu) | Yıllık güncelleme; yerel tutar farkı |
| **10** | **Türkiye'ye özgü öğeleri konuma göre gizlemek:** bayram namazı +38 dk, ezani saat, Türkiye Takvimi ek vakit notu, kandil adları (Endonezya'da "Isra Mikraj", "Nisfu Sya'ban", "Nuzulul Qur'an" vb.) | K | Hepsi | Yok | Türkçe kullanıcıya dokunmamalı |
| **11** | **Saat dilimleri ve şehir:** WIB, WITA ve WIT sayfaları ve vakit yanında etiket; Jakarta şehir sayfası ("jam berapa sekarang di Jakarta"); `places.json` Singaraja düzeltmesi | K | Site (etiket her yerde) | Keppres 41/1987, IANA tz | Düşük |
| **12** | **MABIMS hesabı:** ayın 29'u Endonezya'da gün batımında yükseklik ≥3° ve uzanım ≥6,4°; hilal kartında gösterilir, #3'ün yedeği | O | Hepsi | Kendi hesabımız (`nature-calc.js`); Kemenag takvimi 2022–2026 ile doğrulanır (~60 ay) | Sınır durumlar: Kemenag'ın topocentric/geocentric seçimi ve hangi noktayı kullandığı **(doğrulanmadı)** |
| **13** | **Kab/kota listesi** (il → kab/kota seçici) | O | Hepsi | Koordinat: Wikidata (CC0) ya da GeoNames ADM2 (CC BY 4.0, Türkiye ilçelerindeki gibi). İdeali Kemenag'ın kab/kota koordinatı, izinle | Kemenag'ın noktası başka olabilir; büyük kabupatenlerde (ör. Merauke) birkaç dakikalık fark. Yayla sınıfı için yükseklik verisi |
| **14** | **Kab/kota vakit sayfaları** `/id/prayer-times/<kab-kota>/` (Türkiye'deki 81 il ve 915 ilçe gibi) | B | Site | #13 | Önce #1 ve #13 doğrulanmalı; ince içerik ve SEO riski; sayfa sayısı artar |
| **15** | **Cava pasaranı** (takvim ve takvim yaprağında; isteğe bağlı) ve Cava ayı ile yılı | K (pasaran), O (ay/yıl) | Hepsi | Hesap (JDN mod 5); Kemenag 2026 takvimiyle doğrulandı | Ay ve yıl kuralı için kaynak gerekir **(doğrulanmadı)** |
| **16** | **Endonezce meal:** şimdilik ayet ve sure başına quran.kemenag.go.id bağlantısı (Diyanet'teki gibi); paket içine alma ancak LPMQ izniyle | K / O | iOS, Android | Tanzil'deki "Indonesian Ministry of Religious Affairs" çevirisi yalnız ticari olmayan kullanım içindir ve başka kullanım için yayıncı izni ister ([tanzil.net/trans](https://tanzil.net/trans/)). Baskısı eski olabilir **(doğrulanmadı)** | Sahibin 30 Ekim kararıyla aynı ilke: lisans doğrulanamazsa yalnız bağlantı. Ayet bağlantı biçimi doğrulanmalı (SPA) |
| **17** | **Günlük dualar, Yasin ve Tahlil** | O | iOS, Android | Arapça metinler Tanzil'den; anlam bizim çevirimiz | Tahlil NU geleneği, Muhammadiyah uygulamaz. Hangi grubun pratiğinin gösterileceği sahibin kararı |
| **18** | **Bedug ve kentongan bildirim sesi** (telefonda sentezlenir, telifsiz) | K–O | iOS, Android | Kendi üretimimiz (`mobile/shared/sounds`) | Düşük |
| **19** | **Muhammadiyah seçenekleri** (KHGT takvimi, Subuh −18°), varsayılan değil | O | Hepsi | KHGT tablosu ve Muhammadiyah kararları | Parametreler ve lisans **(doğrulanmadı)**; yanlış yorum ve hassasiyet |

**Öncelik:**
- **Önce:** #1, #2, #3, #4 ve #11. Doğruluk ve güven buna bağlı: bugün oruç ve iftar vakti yanlış görünüyor.
- **Sonra:** #5–#10, yani kurum duyuruları ve beklentiler.
- **En son:** #12–#19.

### 4.1 Kemenag yöntemi doğrulama planı

**1. Resmî parametreyi isteyin.** Ditjen Bimas Islam'a yazın: Direktorat Urusan Agama Islam dan Pembinaan Syariah, hisab rukyat birimi **(birim adı doğrulanmadı)**. Sorulacaklar:
- ihtiyat değerleri;
- Terbit ve Magrib'deki −1°/−2° ufuk kuralı ve yükseklik eşiği;
- Duha tanımı;
- yuvarlama;
- doğrulama için SIHAT tablolarını ve kab/kota koordinatlarını kullanma izni.

**2. Referans tablolar.** SIHAT aylık tabloları kullanılır, arayüzden elle ya da izinle alınır. Çapraz kontrol için il Kemenag ofislerinin (Kanwil) Ramazan 1448 imsakiye PDF'leri.

**3. Şehirler (en az 15 kab/kota):**

| Gerekçe | Kab/kota |
|---|---|
| Kuzeybatı ucu | Kota Sabang, Kota Banda Aceh |
| Kuzey enlemi | Kota Medan |
| Ekvator | Kota Padang, Kota Pontianak (`Asia/Pontianak`) |
| Başkent | Kota Jakarta |
| Yayla sınıfı (−2°) | Kota Bandung, Kab. Karo, Kab. Wonosobo ya da Kota Batu, Kab. Jayawijaya |
| WIB'in doğusu | Kota Surabaya |
| WITA | Kota Denpasar, Kota Makassar, Kota Manado |
| Güney | Kota Kupang |
| WIT | Kota Ambon, Kota Jayapura, Kab. Merauke |

**4. Dönem.** 2026 ve 2027'nin Mart, Haziran, Eylül ve Aralık ayları (ekinoks ve gündönümleri), ayrıca 1448 Ramazan'ı. Toplam ~120 şehir-ay, ~3.600 gün × 8 vakit.

**5. Ölçüt.** Her vakitte |fark| ≤ 1 dk olmalı (Diyanet doğrulamasıyla aynı ölçüt); birebir oran ≥ %95 hedeflenir. Ölçütü geçemeyen her şehir için yükseklik sınıfı denenir. Bu yolla yayla eşiği de ampirik olarak çıkar.

**6. Kalıcı testler.**
- `npm test`'e Jakarta 1 Kasım 2026 (8 vakit) ve Bandung (yayla) eklenir.
- `mobile/shared/testdata`'ya `kemenag` vektörleri eklenir; Swift ve Kotlin bunlarla karşılaştırılır.
- Karşılaştırma verisi depoya ve yayına girmez; Diyanet doğrulamasında da böyleydi.

**7. Güneş formülü.** ±1 dk kalıntının bir kısmı düşük duyarlıklı Güneş konumundan olabilir; Hindistan araştırması da bunu gördü. Daha duyarlı formüle geçilirse doğrulama yenilenir.

### 4.2 Hicrî takvim doğrulaması

- Kemenag 2026 PDF'indeki 12 ay başı elle `hijri-id.json`'a yazılır.
- 2022–2025 PDF'leri bulunursa geçmiş yıllar da eklenir. Bunlar MABIMS hesabımızın (#12) sınav verisidir.
- Isbat sonuçları (yılda 3) kaynak bağlantısıyla kaydedilir.
- Test: 21 Mart 2026 = 1 Şevval 1447; 11 Kasım 2026 = 1 Cemaziyelahir 1448; 10 Mart 2027 = 1 Şevval 1448 (SKB'ye göre, isbatla teyit edilecek).

### 4.3 Sidang isbat aktarımı (sunucusuz)

**A. Önceden bilinen tarih (hemen yapılabilir).**
- Uygulama Kemenag takviminden (#3) isbat gününü bilir.
- O gün "Bu akşam 19.00 WIB'de sidang isbat; hisaba göre beklenen: …" bildirimi verir. Ertesi sabah tarih "tahmini" olarak kalır.
- Sunucu ve ağ gerekmez. Ama isbat hisaptan ayrılırsa uygulama yanlış kalır. MABIMS'ten beri 1447'de üç isbatın üçü de Kemenag takvimiyle aynı çıktı.

**B. Durağan sonuç dosyası (önerilen).**
- `time.bumba.tr/api/v1/id/isbat.json` gibi bir dosya. İçinde ay, karar tarihi, kaynak bağlantısı ve `confirmed` alanı bulunur.
- Yalnız isbat akşamı ve ertesi sabah, birkaç kez okunur. İstek anonimdir ve konum göndermez.
- Okununca yerel bildirim gösterilir ve hicrî tablo düzeltilir.
- **Darboğaz:** "her yayın sahibin onayıyla" kuralı. 19.00 WIB Türkiye'de 15.00'e denk geldiği için sahip o saatte onay vermeli. Ya da sahip yalnız bu dosya için önceden genel bir izin tanımlamalı (örneğin ayrı bir veri yolu). **Bu sahibin kararıdır.**

**C. Haber yakalama (yardımcı).**
- GitHub Actions'ta zamanlanmış bir iş, yalnız isbat akşamı `kemenag.go.id/sitemap-articles.xml` dosyasında "sidang isbat" başlıklarını arar.
- Bulunca bakıcıya issue ya da PR açar. Kendisi yayımlamaz.
- kemenag.go.id'nin `robots.txt` dosyası buna izin veriyor. Bimas Islam'ın `/api/` yolu ise kullanılmaz.

**Önerilen:** A + B, C ile destekli. Metin kopyalanmaz; yalnız olgu ve bağlantı verilir.

---

## 5. Doğrulanmamış ve açık sorular

- Kemenag'ın resmî parametre belgesi: ihtiyat, ufuk −1°/−2°, yükseklik eşiği, Duha. Bu belgedeki kurallar ampirik.
- SIHAT verisinin ve kab/kota koordinatlarının kullanım koşulları; telif muafiyetinin namaz vakti tablolarını kapsayıp kapsamadığı.
- 2027 Kalender Hijriah Indonesia'nın yayımlanıp yayımlanmadığı; 2027 SKB numarası (1205 ve 1025 karışıklığı).
- NU'nun olağan aylarda Kemenag takviminden ayrılıp ayrılmadığı; Muhammadiyah'ın İsya açısı ve ihtiyatı.
- Tanzil'deki Kemenag mealinin baskısı (2019 baskısı mı?) ve LPMQ'nun kullanım koşulları; quran.kemenag.go.id ayet bağlantı biçimi.
- Cava ayı ve yılı kuralı (Kemenag takviminde ay başları hicrîyle aynı görünüyor).

---

## Özet

Endonezya'nın resmî din kurumu Kemenag'ın kullanıcıya ulaşan çıktıları az sayıdadır ve çoğu önceden bilinir: kab/kota başına SIHAT vakit tabloları ve imsakiye, yılda üç sidang isbat, yıllık Kalender Hijriah Indonesia, SKB 3 Menteri tatil ve cuti bersama listesi, Rashdul Qiblat, salat gerhana çağrıları ve BAZNAS'ın zekât kararları. MUI'nin 2/2004 fetvası nedeniyle Endonezya'da varsayılan takvim hükümetin takvimi olmalı. Bugün Saat Tarih Endonezyalı kullanıcıya Diyanet vaktini gösteriyor: Subuh 6–7 dk geç, İsya 5–7 dk erken, İmsak satırı yok. Hicrî tarih 2026'da 12 ayın 5'inde bir gün kayıyor. Takvim, tatiller, zekât ve fitre Türkiye'ye göre. Endonezya tatil verisi, Jakarta ve WIB/WITA/WIT da yok. Kemenag tablosu cihazda ±1 dk ile kurulabiliyor: 20°/18°, ihtiyat, yöntem başına yuvarlama, İmsak −10 dk, Duha +4,5° ve iki sınıflı ufuk düzeltmesi. Bu sonuç 10 şehir-ayda ölçüldü. Önce bu yöntem, konuma göre varsayılan yöntem, Endonezya hicrî tablosu, SKB tatilleri ve saat dilimi etiketleri yapılmalı. Kurum duyuruları sunucusuz aktarılabilir: takvimden bilinen tarihlere yerel bildirim ve isbat akşamı elle güncellenen durağan bir dosya. Bu dosyanın yayın onayı sahibin kararına bağlı. Kemenag'dan resmî parametreleri ve doğrulama için SIHAT kullanım iznini yazılı istemek, en ucuz ve en önemli adımdır.
