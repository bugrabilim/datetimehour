# Hindistan araştırması: Hindistanlı kullanıcıya gerçekten ne yarar? (8 Kasım 2026)

Sahibin sorusu: Hindistan'da nüfusun çoğu Hindu olduğu için namaz vakitleri çoğu kişiye az yarıyor. Peki Hindistanlı kullanıcı için gerçekten yararlı olan ne?

Bu belge araştırmadır. Kod değişmedi. Kaynaklar satır içinde verildi. **(doğrulanmadı)** işareti, birincil kaynakta görülmeyen ya da kaynakların çeliştiği bilgiyi gösterir. Hesap denemeleri, depodaki `src/js/nature-calc.js` ve `src/js/prayer-calc.js` dosyalarıyla geçici bir betikte yapıldı. Betik depoya girmedi.

**Öne çıkanlar**
- Hindistan'da günlük takvim ihtiyacının merkezinde **panchang** var: tithi, nakshatra, yoga, karana ve gün; gün doğumu ve batımı; Rahu Kaal gibi geleneksel zaman dilimleri; bayram ve oruç günleri. Bunun yanında **resmî tatiller** ve ulusal **Saka takvimi** gelir.
- Panchang **cihazda hesaplanabilir**. Depodaki Meeus Ay hesabıyla yapılan denemede tithi, nakshatra, yoga ve karana bitişleri Drik Panchang'la (New Delhi) **8 karşılaştırmanın 8'inde ±1 dk** içinde çıktı. 2026, 2029 ve 2031'in artık ayları da doğru bulundu. Güneş'in burç geçişlerinde (sankranti) ise **6–15 dk sapma** var. Bunun nedeni düşük duyarlıklı Güneş formülü: JPL Horizons'a göre +33″. Güneş hesabının iyileştirilmesi gerekiyor.
- Hindistan Müslümanları için bugün iki somut hata var:
  - **Varsayılan yöntem her yerde Diyanet.** Bu yüzden Delhi'de ikindi Hanefî vaktinden 41 dk erken görünüyor. Akşamda da Türkiye'ye özgü +8 dk ihtiyat payı çıkıyor.
  - **Hicri tarih Diyanet tablosundan geliyor.** 2026'da Hindistan'da Ramazan Bayramı, Kurban Bayramı ve Aşure bir gün sonraydı.
- Ürünün Hintçe yüzü bugün Türkiye merkezli:
  - Takvim sayfasının başlığı "तुर्किये की छुट्टियाँ" (Türkiye'nin tatilleri).
  - Uygulama takvimi varsayılan olarak "TR" açılıyor ve listede Hindistan yok.
  - Saat farkı sayfaları İstanbul'a göre.
  - Konum seçicide yalnız 81 il var.
  - Saat varsayılan olarak 24 saatlik; Hindistan'da 12 saatlik kullanılıyor.
  - Hintçe "चंद्र राशि" (Ay burcu) sayfası tropikal hesaplıyor. Hindistan'da "raşi" yıldızlara göre (sidereal) anlaşılır.

---

## 1. Bağlam

- **Din dağılımı (2011 sayımı, 121,09 crore kişi):** Hindu %79,8, Müslüman %14,2 (~172 milyon), Hristiyan %2,3, Sih %1,7, Budist %0,7, Caynist %0,4 ([PTC News](https://www.ptcnews.tv/registrar-general-and-census-commissioner-releases-census-data-on-religion-based-census-indias-population-in-2011-is-121-09-crores-hindu-79-8muslim-14-2christian-2-3sikh-1-7buddhist-0-7jain), [Gulf News](https://gulfnews.com/world/asia/india/india-has-798-hindus-142-muslims-census-1.1572882)). Bugün Müslüman nüfus ~200 milyon tahmin ediliyor **(doğrulanmadı)**.
- **Saat biçimi:** CLDR'ye göre `hi-IN`, `en-IN` ve `ur-IN` dillerinin varsayılanı 12 saatlik (`h12`; Node `Intl` ile denendi). Türkçede 24 saatlik (`h23`).
- **Saat dilimi:** IST (UTC+05:30). Resmî saatin sorumlusu CSIR-National Physical Laboratory. 2025'te IST'yi zorunlu kılan bir kural taslağı yayımlandı ([ThePrint](https://theprint.in/economy/government-notifies-draft-rules-for-one-nation-one-time-initiative-to-synchronize-time-across-india/2465056)). Kuralın kesinleşip kesinleşmediği **(doğrulanmadı)**.

## 2. Hintli kullanıcılar saat ve takvim uygulamasını ne için kullanıyor?

### 2.1 Panchang ve önde gelen uygulamalar

**Drik Panchang** ("Hindu Calendar - Drik Panchang", Adarsh Mobile Applications LLP)
- Android'de ~4,7 milyon kurulum, 213 bin oyda 4,63 ([AppGoblin](https://appgoblin.info/apps/com.drikp.core)). iOS'ta ~5,9 bin oyda 4,8 ([App Store](https://apps.apple.com/us/app/hindu-calendar-drik-panchang/id1321271821)).
- **Günlük panchang:** beş öğe (tithi, nakshatra, yoga, karana, vaar) ile Panchaka, Ganda Moola, Bhadra, Rahu Kalam.
- **Muhurta tabloları:** gündüz ve gece Choghadiya, Shubh Horai, Udaya Lagna, Abhijit, Amrit Kalam; her dilim için geri sayım.
- **Takvim:** bayram ve oruç listesi (Ekadashi, Pradosham, Purnima, Sankranti, Shivaratri), tutulmalar. Purnimanta ve amanta seçimi; Vikram, Shaka ve Gujarati Samvat; Bengal, Malayalam, Odia ve Tamil güneş takvimleri.
- **Diğer:** kundali (doğum haritası) ve eş uyumu, Vastu pusulası, kendi tithi hatırlatıcısı, widget.
- **Erişim:** internetsiz çalışır. Reklamlıdır; reklamsız sürüm aylık ₹19 ya da yıllık ₹199.
- Sitenin bölgesel panchang menüsünde Assam, Bengal, Tamil, Odia, Malayalam, Marathi, Gujarati, Kannada, Telugu, Nepal ve ISKCON seçenekleri var ([Drik apps](https://www.drikpanchang.com/apps/android/drikpanchang-apps.html)).

**Prokerala**
- Günlük sayfada beş öğe, gün doğumu ve batımı, Ay'ın doğuşu ve batışı, Güneş ve Ay burcu, ritu (mevsim) gösteriliyor.
- Kaçınılan dilimler: Rahu Kaal, Yamaganda, Gulika, Dur Muhurat, Varjyam.
- Uygun sayılan dilimler: Abhijit, Amrit Kaal, Brahma Muhurat.
- Ayrıca bayram listesi, Hintçe, Gujarati, Tamil, Malayalam ve Telugu panchangları ([Prokerala](https://www.prokerala.com/astrology/panchang/)). Ücretli raporlar ayrı bir bölümde.

**Kalnirnay** (Marathi duvar takvimi geleneği)
- Panchang, evlilik için uygun günler, ay ve güneş olayları, notlar ve hatırlatıcılar. Marathi, Hintçe, İngilizce ve Gujarati dillerinde ([appfollow](https://apps.appfollow.io/ios/kalnirnay-2025/955413793?country=in)).
- Bir toplayıcıya göre Android'de ~27 milyon kurulum var **(doğrulanmadı)** ([appwisp](https://appwisp.com/app/android/com.awt.kalnirnay)).

**Google Takvim**
- Bölgesel tatil takvimi var. "Resmî tatiller ve diğer günler", "yalnız resmî tatiller" ya da "seçili günler" seçilebiliyor ([Google yardım](https://support.google.com/calendar/answer/13748345)).
- Not: Uygulamamız telefon takvimlerini zaten okuyor. Kullanıcının Google'daki Hindistan tatilleri bugün de Takvim sekmesinde görünebilir.

**Apple**
- Swift Foundation 6.2 için Hindistan takvimlerinin eklenmesi önerildi: `vikram`, `tamil`, `bangla`, `gujarati`, `kannada`, `malayalam`, `marathi`, `odia`, `telugu` ve artık gün için `isAdhikaDay` ([Swift Forums](https://forums.swift.org/t/pitch-expanded-calendar-support/77438)).
- Bir kullanıcı, iOS 26'da Tamil tarihinin bir gün kaydığını bildirdi ([Apple Community](https://discussions.apple.com/thread/256198716)).
- Hangi sistemde çıktığı ve ne kadar doğru olduğu **(doğrulanmadı)**. Bu yüzden ona dayanmayı önermiyorum.

**Ortak payda:** Kullanıcılar her gün açıp "bugün hangi tithi, Rahu Kaal ne zaman, sıradaki bayram ve oruç günü hangisi" diye bakıyor. Bayram tarihinin bulunduğu yere göre doğru çıkması bekleniyor.

### 2.2 Geleneksel zaman dilimleri (gün doğumu ve batımından hesaplanır)

**Kurallar**
- **Rahu Kaal, Yamaganda, Gulika:** Gündüz (doğuştan batışa) 8 eşit parçaya bölünür. Rahu Kaal haftanın gününe göre şu parçadır: Pazar 8., Pazartesi 2., Salı 7., Çarşamba 5., Perşembe 6., Cuma 4., Cumartesi 3. ([hindutone](https://hindutone.com/astrology/rahu-kalam-yamagandam-gulika-kalam-explained/), [panchangam crate](https://docs.rs/crate/panchangam/0.2.0/source/src/muhurat/rahu_kaal.rs)).
- **Choghadiya:** Gündüz ve gece ayrı ayrı 8 parçaya bölünür. İlk dilimin adı haftanın gününe göre değişir: Pazar Udveg, Pazartesi Amrit, Salı Rog, Çarşamba Labh, Perşembe Shubh, Cuma Char, Cumartesi Kaal. Gece sırasının kuralı kaynaklarda çelişiyor **(doğrulanmadı)** ([shubhpanchang](https://shubhpanchang.com/choghadiya)).
- **Abhijit ve Brahma muhurta:** Gündüz ve gece ayrı ayrı 15 muhurtaya bölünür. Abhijit gündüzün 8. muhurtasıdır. Brahma muhurta gecenin 14. muhurtasıdır, yani gün doğumundan 2 muhurta önce başlar. Bu tanımı Drik'in sayılarıyla ben doğruladım (aşağıdaki tablo).

**Delhi, 8 Kasım 2026 (Pazar) karşılaştırması**

Drik değerleri [Drik Panchang günlük sayfasından](https://www.drikpanchang.com/panchang/day-panchang.html?geoname-id=1261481&date=08/11/2026) alındı. Hesap, yukarıdaki kuralların Drik'in kendi doğuş ve batış saatlerine (06:38 / 17:31) uygulanmasıyla yapıldı:

| Dilim | Drik | Kuraldan hesap |
|---|---|---|
| Rahu Kaal (8. parça) | 16:10–17:31 | 16:09,4–17:31 |
| Yamaganda (5.) | 12:05–13:26 | 12:04,5–13:26 |
| Gulika (7.) | 14:48–16:10 | 14:47,8–16:09,4 |
| Abhijit (gündüzün 8./15'i) | 11:43–12:26 | 11:42,7–12:26,2 |
| Brahma muhurta (gecenin 14./15'i) | 04:53–05:46 | 04:53,2–05:45,6 |

Drik'in ekranda gösterdiği gün doğumu, Güneş'in üst kenarı ve kırılma dahil standart tanımdır. Bizim namaz hesabımızın gün doğumuyla aynıdır. Karaçi yöntemi Delhi için 06:38 / 17:31 veriyor, Drik'le birebir aynı. "Hindu gün doğumu" (disk merkezi, kırılmasız) ayrı bir seçenektir ve bu konuda uzlaşma yoktur ([Drik gün doğumu](https://www.drikpanchang.com/panchang/sunrise/panchang-sunrise.html), [Drik SSS](https://www.drikpanchang.com/faq/faq-ans2.html?lang=hi)).

### 2.3 Bayramlar ve oruç günleri

**Bayramların çoğu ay takvimindedir.** Tarih, o günün tithi'si ve günün belli bir anındaki durum kuralıyla bulunur:
- **Diwali (Lakshmi Puja):** Amavasya (yeni ay tithi'si) akşamın pradosh dilimine denk gelen gün ([Drik Lakshmi Puja](https://www.drikpanchang.com/festivals/lakshmipuja/festivals-lakshmipuja-timings.html?year=2021)). 2024'te kaynaklar 31 Ekim ile 1 Kasım arasında bölündü ([Business Today](https://www.businesstoday.in/amp/india/story/happy-diwali-2024-october-31-or-november-1-for-diwali-check-lakshmi-puja-mahurat-preparations-for-both-days-452158-2024-10-31)). Bu, kural tabanlı tarihin bile tartışmalı olabileceğini gösteriyor.
- **Janmashtami:** Smarta geleneği gece yarısındaki Ashtami ve Rohini'ye, Vaishnava geleneği gün doğumundaki tithi'ye bakar. 2025'te 15 ve 16 Ağustos olarak iki ayrı tarih çıktı ([India TV](https://www.indiatvnews.com/lifestyle/spirituality/krishna-janmashtami-2025-date-confusion-puja-time-2025-08-10-1002881)). Devlet listesi "Janmashtami (Vaishnavi)" yazıyor.
- **Ekadashi:** Smarta geleneğinde gün doğumundaki tithi esastır. Vaishnava geleneğinde Dashami tithi'si gün doğumundan 96 dk önceki (Arunodaya) dilime değiyorsa oruç ertesi güne kayar ([dekhopanchang](https://dekhopanchang.com/en/learn/smarta-vaishnava), [advaita-l](https://advaita-vedanta.org/archives/advaita-l/2010-February/023796.html)). Drik ikisini de yayımlıyor.
- **Diğer bayramların kuralları:** Holi (Phalguna Purnima, pradosh, Bhadra karanasından kaçınılarak), Raksha Bandhan (Shravana Purnima, Bhadra'sız), Ganesh Chaturthi (öğle dilimi), Dussehra, Navratri, Maha Shivaratri, Karva Chauth (Ay'ın doğuşuyla), Chhath. Bunları genel bilgiden yazdım **(doğrulanmadı)**. Uygulamadan önce her kural ayrıca kaynaktan doğrulanmalı.
- **Güneş takvimine bağlı bayramlar:** Makar Sankranti, Pongal (Tamil Thai ayının 1'i), Vishu ve Baisakhi (Mesha sankranti), Onam (Malayalam Chingam ayında Thiruvonam nakshatra'sı).

### 2.4 Bölgesel takvimler

**Ulusal takvim (Saka, güneş takvimi)**
- Resmî kullanım 1 Chaitra 1879 = 22 Mart 1957'de başladı. Gazette of India, All India Radio haberleri ve devlet yazışmalarında kullanılır.
- Chaitra 30 gündür (artık yılda 31). Vaishakha'dan Bhadra'ya kadar aylar 31 gün, Ashvina'dan Phalguna'ya kadar 30 gündür ([Wikipedia](https://en.wikipedia.org/wiki/Indian_national_calendar)).
- 8 Kasım 2026 = 17 Kartika 1948 Śaka. ICU'da `indian` takvimi olarak hazır var: `Intl.DateTimeFormat("hi-IN-u-ca-indian")` → "रविवार, 17 कार्तिक 1948 शक".

**Ay-güneş takvimleri**
- **Amanta** (ay yeni ayda biter): Gujarat, Maharashtra, Goa, Karnataka, Andhra Pradesh, Telangana.
- **Purnimanta** (ay dolunayda biter): Vindhya dağlarının kuzeyindeki eyaletlerin çoğu ve Nepal ([Wikipedia, Hindu calendar](https://en.wikipedia.org/wiki/Hindu_calendar)).
- Aynı gün iki sistemde farklı ay adı taşır. Örnek: 8 Kasım 2026 amanta'da Ashwina, purnimanta'da Kartika.
- **Yıl sayımları:** Vikram Samvat (MÖ 57; 2083 yılı 19 Mart 2026'da başladı). Gujarati Vikram Samvat Diwali'nin ertesinde başlar. Shalivahana Shaka da kullanılır. Drik bugün "Shaka 1948 Parabhava, Vikram 2083 Siddharthi" gösteriyor.

**Güneş takvimleri:** Tamil, Malayalam, Bengal, Odia, Assam, Pencap. Ayın başladığı gün sankranti anının günün hangi diliminde olduğuna bağlıdır:
- Tamil: sankranti gün batımından önceyse ay o gün başlar.
- Malayalam: gündüzün ilk 3/5'i içindeyse o gün başlar.
- Odia, Pencap: sankranti günü başlar.
- Bengal: kaynaklar çelişiyor (gece yarısı kuralı mı, ertesi gün mü) **(doğrulanmadı)** ([hindu-blog](https://www.hindu-blog.com/2019/04/hindu-solar-calendars-differences.html)).

### 2.5 Resmî tatiller

**Merkezî hükümet (DoPT)**
- 3 Temmuz 2025 tarihli ve 12/2/2023-JCA sayılı yazıyla 2026 belirlendi ([StaffNews](https://www.staffnews.in/2025/07/list-of-holidays-2026-dopt-order-reg-closed-and-restricted-holidays.html)).
- **14 zorunlu tatil** var: Republic Day, Independence Day, Gandhi Jayanti, Buddha Purnima, Christmas, Dussehra, Diwali, Good Friday, Guru Nanak Jayanti, Id-ul-Fitr, Id-ul-Zuha, Mahavir Jayanti, Muharram, Milad-un-Nabi.
- Bunlara 12 seçenekten 3'ü eklenir. Delhi dışında bu üçünü her eyalet başkentindeki koordinasyon komitesi seçer.
- Çalışan ayrıca listeden 2 kısıtlı tatil (RH) kullanabilir.
- İslami tatiller hilale göre kayabilir. Değişiklik kısa sürede PIB, TV ve radyo üzerinden duyurulur.

**2026 Delhi kapalı günleri (17)** ([StaffNews, Ek-I](https://www.staffnews.in/2025/07/list-of-holidays-2026-closed-holidays.html))

26 Oca · Holi 4 Mar · Id-ul-Fitr 21 Mar · Ram Navami 26 Mar · Mahavir Jayanti 31 Mar · Good Friday 3 Nis · Buddha Purnima 1 May · Bakrid 27 May (sonra 28 May'a alındı) · Muharram 26 Haz · 15 Ağu · Milad-un-Nabi 26 Ağu · Janmashtami 4 Eyl · 2 Eki · Dussehra 20 Eki · Diwali 8 Kas · Guru Nanak 24 Kas · 25 Ara.

**Diğer listeler**
- 2027 listesi de yayımlandı (16 Temmuz 2026 tarihli yazı; 17 kapalı gün, 32 RH) ([StaffNews 2027](https://www.staffnews.in/2026/07/list-of-holidays-for-the-year-2027-gazetted.html)).
- **Banka ve eyalet tatilleri:** RBI, Negotiable Instruments Act kapsamındaki tatilleri 34 bölge ofisi için ay ay yayımlıyor ([RBI](https://www.rbi.org.in/Scripts/HolidayMatrixDisplay.aspx)). Eyaletler arası fark büyük.

### 2.6 Diğer topluluklar (kısa)

- **Sihler:** Nanakshahi takvimi tartışmalı. SGPC 2010'da sabit tarihlerden ay takvimine (Bikrami) döndü. Bazı kuruluşlar 2003 sürümünü kullanıyor. 2016'da Guru Gobind Singh'in doğum günü 5 ve 16 Ocak olmak üzere iki tarihte kutlandı ([Wikipedia](https://wikipedia.com/wiki/Nanakshahi), [Asia Samachar](https://asiasamachar.com/2016/01/08/5431/)). Guru Nanak Jayanti, Kartika Purnima'da kutlanır (ay takvimi).
- **Hristiyanlar** (%2,3): Good Friday ve Christmas kapalı gün. Easter ve Christmas Eve seçmeli listede ([python-holidays India](https://raw.githubusercontent.com/vacanza/holidays/dev/holidays/countries/india.py)).
- **Caynistler ve Budistler:** Mahavir Jayanti (Chaitra Shukla 13) ve Buddha Purnima (Vaishakha Purnima) aynı ay hesabıyla bulunur. Paryushana'nın Shvetambara ve Digambara gelenekleri farklıdır **(doğrulanmadı)**.

### 2.7 Kullanıcı yorumlarında şikâyetler

- **Reklam:** Drik'te tam ekran reklamlar ekranı saniyelerce kapatıyor ([App Store](https://apps.apple.com/us/app/hindu-calendar-drik-panchang/id1321271821)). Kalnirnay'da sesli video reklamlar uygulamayı yavaşlatıyor.
- **Kalnirnay'ın eksikleri:** çevrimdışı çalışmıyor, iOS widget'ı yok, konum Mumbai dışına değiştirilemiyor ([appfollow](https://apps.appfollow.io/ios/kalnirnay-2025/955413793?country=in)).
- **Yanlış tarih:** saat dilimi yanlış ayarlandığı için tarih kayıyor, ya da Buddha Purnima bir gün kaymış görünüyor ([arama özeti](https://www.drikpanchang.com/festivals/hindu-festival-date-shift-with-timezone.html)). Drik, bayramın yere göre değiştiğini açıklıyor: Seattle'da Deepavali New Delhi'den bir gün önce olabiliyor.
- **Uygulama hatası:** ayrıntılı gün görünümü açılırken çöküyor.

**Çıkarım:** Reklamsız, çevrimdışı, konuma göre doğru tarih veren ve widget'ı olan bir panchang gerçek bir boşluk. Bu, bizim ürünün güçlü yanlarıyla örtüşüyor.

## 3. Hesap: cihazda yapılabilir mi?

### 3.1 Tanımlar

Kaynaklar: [Wikipedia, Tithi](https://www.wikiwand.com/en/articles/tithi), [Astronomical basis](https://Www.wikipedia.org/wiki/Astronomical_basis_of_the_Hindu_calendar).

| Öğe | Tanım | Ayanamsa etkisi |
|---|---|---|
| **Tithi** | (λ☾ − λ☉) / 12°, ayda 30 tithi. Günün tithi'si, gün doğumu anındaki tithi'dir. | Etkilemez (fark alınır) |
| **Karana** | Yarım tithi (6°), 60 dilim. 7 hareketli karana 8 kez döner; 4 sabit karana (Shakuni, Chatushpada, Naga, Kimstughna) yeni ay çevresindedir. | Etkilemez |
| **Nakshatra** | Ay'ın yıldızlara göre (sidereal) boylamı / 13°20′, 27 dilim | Etkiler |
| **Yoga** | Ay ve Güneş'in yıldızlara göre boylamları toplamı / 13°20′, 27 dilim | Etkiler (ayanamsa iki kez girer) |
| **Vaar** | Gün doğumundan gün doğumuna haftanın günü | Yok |
| **Masa (ay)** | Amanta ay, içine düşen sankrantinin adını alır: Mesha → Chaitra, Vrishabha → Vaishakha … | Etkiler (sankranti üzerinden) |
| **Adhika masa** | İçinde sankranti olmayan ay. Bir sonraki ayın adını "adhika" ekiyle alır. Ortalama 32,5 ayda bir. | Etkiler |
| **Kshaya masa** | İki sankranti içeren ay. Çok seyrek: 1963–64, 1982–83, sonraki 2124 ([Wikipedia](https://en.wikipedia.org/wiki/Hindu_calendar)). | Etkiler |

### 3.2 Ayanamsa: Lahiri (Chitrapaksha)

- 1955'teki Takvim Reformu Komitesi (başkan Meghnad Saha, sekreter N. C. Lahiri) Spica (Chitra) yıldızına dayalı ayanamsayı Hindistan standardı yaptı.
- Değer 21 Mart 1956, 0:00 ET için 23°15′00″ olarak konuldu. 1985'teki Indian Astronomical Ephemeris bunu 23°15′00,658″ olarak düzeltti. Değer "true"dur, yani nütasyon dahildir ([Swiss Ephemeris belgesi §2.8.5](https://www.astro.com/swisseph/swisseph.htm)).
- Komite resmî bir Rashtriya Panchang da önerdi. Bu panchang 1957'den beri yayımlanıyor; bugün IMD'ye bağlı Positional Astronomy Centre (Kolkata) çıkarıyor ([PAC tarihçe](https://packolkata.imd.gov.in/history.php)). Hintçe, İngilizce ve 12 dilde çıkıyor; dört büyükşehrin doğuş ve batış saatlerini, tithi, nakshatra, yoga ve gezegen geçişlerini, başlıca bayramları içeriyor. Fiyatı ₹225 ([PAC Rashtriya Panchang](https://packolkata.imd.gov.in/rp.php)).

### 3.3 Prototip sonuçları: depodaki formüllerle

**Yöntem**
- Ay: `nature-calc.js` `moonPos` (Meeus bölüm 47, kısaltılmış seri).
- Güneş: `sunPos` (Meeus bölüm 25, düşük duyarlıklı).
- Ayanamsa: Lahiri değeri (23°15′00,658″, 1956) üstüne IAU 1976 presesyonu eklendi.
- Sınır anları 10 dk adımla arandı, sonra 30 kez ikiye bölündü.

**Karşılaştırma: Drik Panchang, New Delhi, 8–9 Kasım 2026**

| Öğe | Drik | Prototip | Fark |
|---|---|---|---|
| Tithi Chaturdashi bitişi | 8 Kas 11:27 | 11:28 | 1 dk |
| Tithi Amavasya bitişi | 9 Kas 12:31 | 12:32 | 1 dk |
| Karana Chatushpada bitişi | 8 Kas 23:56 | 23:57 | 1 dk |
| Karana Nagava bitişi | 9 Kas 12:31 | 12:32 | 1 dk |
| Nakshatra Swati bitişi | 9 Kas 07:24 | 07:24 | 0 |
| Nakshatra Chitra bitişi (gün doğumundan önce, Drik 8 Kasım'da Swati gösteriyor) | – | 8 Kas 05:52 | tutarlı |
| Yoga Ayushman bitişi | 9 Kas 03:03 | 03:03 | 0 |
| Yoga Saubhagya bitişi | 10 Kas 02:57 | 02:57 | 0 |
| Ay adı (amanta / purnimanta) | Ashwina / Kartika | Ashwina (Tula sankranti 17 Eki bu aya düşüyor) | aynı |

**Diwali 2026**
- Prototipe göre Amavasya 8 Kasım 11:28'de başlıyor, 9 Kasım 12:32'de bitiyor.
- 8 Kasım akşamının pradosh dilimi (17:31'den itibaren) Amavasya'ya düşüyor. Bu yüzden Diwali 8 Kasım çıkıyor. DoPT listesi de 8 Kasım diyor.

**Artık aylar (prototip)**

| Prototip | Kaynak | Not |
|---|---|---|
| **Adhika Jyeshtha 2026:** yeni ay 17 May 01:32 → 15 Haz 08:24 IST | Basında 17 Mayıs – 15 Haziran (Drik'te bitiş 14 Haziran olarak da görünüyor) ([hindutone](https://hindutone.com/hindutone/adhik-maas-2026-dates-purushottam-maas-rules/), [Drik Jyeshtha 2026](https://www.drikpanchang.com/festivals/lunar-month/festivals-jyeshtha.html?year=2026)) | Mithuna sankranti 15 Haziran 12:48'de (Drik 12:59), yeni aydan yalnız ~4,5 saat sonra. Sınıra yakın bir durum. |
| **Adhika Chaitra 2029:** 15 Mar 09:49 → 14 Nis 03:11 | Drik ve Patrika: 16 Mart – 13 Nisan 2029 ([Patrika](https://www.patrika.com/religion-and-spirituality/next-adhik-maas-2029-date-purushottam-maas-malmas-2029-20672222)) | Yeni ay ile Mesha sankranti arasında yalnız **~0,6 saat** var. Duyarlılığın en kritik olduğu örnek. |
| **Adhika Bhadrapada 2031:** 18 Ağu → 17 Eyl | Kaynaklarda ay adı (Bhadrapada mı, Shravana mı) ve tarihler çelişiyor **(doğrulanmadı)** | |

**Güneş'in burç geçişi (sankranti): bizde sistematik olarak erken**

| Geçiş | Drik | Prototip | Fark |
|---|---|---|---|
| Makara 2026 | 14 Oca 15:13 | 15:04 | −8,5 dk |
| Mesha 2026 | 14 Nis 09:39 | 09:23 | −15,2 dk |
| Mithuna 2026 | 15 Haz 12:59 | 12:48 | −10,4 dk |

Kaynaklar: [Makar](https://www.drikpanchang.com/sankranti/makar-sankranti-date-time.html?year=2026), [Mesha](https://www.drikpanchang.com/sankranti/mesha-sankranti-date-time.html?year=2026), [Mithuna](https://www.drikpanchang.com/sankranti/mithuna-sankranti-date-time.html?year=2026).

**Nedeni ölçüldü:** [JPL Horizons](https://ssd.jpl.nasa.gov/api/horizons.api) ile, yer merkezli görünür ekliptik boylam (QUANTITIES=31) karşılaştırıldı.
- 14 Nisan 2026 04:00 UT'de Güneş boylamımız **+32,6″** fazla. Güneş günde ~1° yürüdüğü için bu ~13 dk erkene denk geliyor.
- 8 Kasım 2026 06:00 UT'de Ay boylamımızın farkı 0,1–10″. Ay günde ~13° yürüdüğü için bu en çok ~20 sn eder.

### 3.4 Yeterli mi?

- **Ay'a bağlı öğeler (tithi, karana, nakshatra, yoga) ve Rahu Kaal gibi dilimler:** Mevcut kod yeterli (±1 dk). Daha da sağlamak için Meeus bölüm 47'nin tam serisine (~60 terim, ~10″) geçmek ucuz bir iş.
- **Güneşe bağlı her şey:** sankranti, güneş takvimleri (Tamil, Malayalam, Bengal, Odia), Makar Sankranti ve Vishu gibi bayramlar, ay adlandırması ve artık ay. Bunlar için Güneş hesabı **yetersiz**. 1″ düzeyinde bir Güneş gerekiyor: Meeus bölüm 25'in yüksek duyarlıklı yolu ya da kısaltılmış VSOP87 (Meeus Ek III terimleri). 2029'daki 0,6 saatlik sınır payı bunun neden şart olduğunu gösteriyor.
- **ΔT:** Sabit 69 sn alınıyor. 2026–2031 için sorun değil.
- **Ayanamsa:** Nakshatra ve yoga bitişlerinin dakikası dakikasına tutması, IAU 1976 tabanlı ortalama Lahiri değerinin bu dönemde ~15″'ten iyi olduğunu gösteriyor. Ayrıntı (true/mean, IAE ile fark) Rashtriya Panchang'la doğrulanmalı.

### 3.5 Uç durumlar

- **Kshaya tithi:** hiçbir gün doğumuna değmeyen, takvimden düşen tithi.
- **Vriddhi (adhika) tithi:** iki gün doğumunu kapsayan, iki gün yazılan tithi ([wikiwand](https://www.wikiwand.com/en/articles/tithi)). Aynı durum nakshatra için de olur.
- **Adhika ve kshaya masa** (yukarıda).
- **Yere bağlılık:** Tarih bulunulan yere göre değişir. Hindistan dışındaki kullanıcıda bayram bir gün kayabilir.
- **Gelenek farkları:** Smarta/Vaishnava; amanta/purnimanta; bölgesel yılbaşları (Ugadi ve Gudi Padwa amanta, Puthandu ve Vishu güneş takvimi); Bhadra (Vishti karana) kısıtı.
- **Gece yarısını aşan tithi:** Bitiş saatleri "+1 gün" etiketiyle gösterilmeli. Drik "03:03 AM, Nov 09" yazıyor.

### 3.6 Kütüphaneler ve lisanslar

- **Swiss Ephemeris:** AGPL ya da ücretli profesyonel lisans ([§1.1](https://www.astro.com/swisseph/swisseph.htm)). AGPL, App Store dağıtımı ve kapalı uygulama koduyla sorun çıkarır. Önermiyorum.
- **Açık panchang kodları:** Çoğu Swiss Ephemeris'e dayanıyor. GitHub'da lisansı görünmeyenler var: `webresh/drik-panchanga`, `karthikraman/panchangam`. Kod alınmamalı, yalnız tanımlar için okunabilir.
- **Kendi hesabımız:** Depodaki Meeus kodunun lisans sorunu yok. "Tek doğru kaynak JS + Swift ve Kotlin test verisi" yöntemi bire bir uygulanabilir.
- **Saka takvimi:** ICU'da hazır (site `Intl`, iOS `Calendar(identifier: .indian)`, Android `android.icu.util.IndianCalendar`, API 24+; minSdk 26 olduğu için uygun).

### 3.7 Doğrulama için kaynaklar

- **JPL Horizons:** açık uç nokta, astronomik konumlar için birincil kaynak.
- **Rashtriya Panchang:** resmî, satın alınır.
- **DoPT listeleri:** devletin seçtiği bayram günleri.
- **python-holidays:** MIT lisanslı. Hindistan için 2001–2035 Hindu bayramları ve eyalet listeleri var ([india.py](https://raw.githubusercontent.com/vacanza/holidays/dev/holidays/countries/india.py)).
- **Drik Panchang:** yalnız elle örnek karşılaştırma için. Altbilgisinde "all images and data are under copyright" yazıyor (arama özetinden; [Drik uygulama koşulları](https://www.drikpanchang.com/contactus/privacy-policy/app/dp-apps-terms-and-conditions.html)). Veri depoya kopyalanmamalı.

## 4. Hindistan Müslümanları (ve Urduca kullananlar)

### Yöntem

- Uygulamaların çoğunda Hindistan için varsayılan **Karaçi İslami İlimler Üniversitesi (sabah 18° / yatsı 18°) + Hanefî ikindi** ([esalah](https://esalah.com/?p=100), [islamicprayerstimes](https://islamicprayerstimes.com/world/india); ikincil kaynaklar). Ülke çapında resmî bir vakit kurumu yok. Camiler kendi çizelgelerini kullanıyor **(doğrulanmadı)**.
- **Kerala'da** Müslümanların çoğu Şâfiî ([Wikipedia](https://en.wikipedia.org/wiki/Islam_in_Kerala)). Orada ikindi standart (birinci gölge) hesabıyla bulunur.
- Şiiler (Caferî) ve Ehl-i Hadis azınlıktır; oranları **(doğrulanmadı)**.

**Bugünkü hatanın büyüklüğü (Delhi, 8 Kasım 2026, bizim hesap)**

| Vakit | Varsayılan (Diyanet) | Karaçi + Hanefî | Fark |
|---|---|---|---|
| Güneş | 06:31 | 06:38 | Diyanet −7 dk ihtiyat payı uyguluyor |
| Öğle | 12:10 | 12:05 | +5 |
| İkindi | **15:14** | **15:55** | **41 dk erken** |
| Akşam | 17:39 | 17:31 | +8 |
| Yatsı | 18:48 | 18:52 | −4 |

Hyderabad'da ve Kozhikode'de (Kerala) Hanefî ikindiyle fark 45–48 dk. Kerala için Karaçi standart ikindi 15:31; Diyanet bunu 4 dk farkla tutturuyor.

### Hicri tarih

Hilal görme kurulları karar veriyor (Delhi Cami-i Mescid / Ruet-e-Hilal, Hyderabad Central Ruet-e-Hilal, Lucknow). 2026 karşılaştırması:

| Gün | Diyanet (`holidays.json`) | Hindistan |
|---|---|---|
| 1 Ramazan | 19 Şub | 19 Şub ([FPJ](https://www.freepressjournal.in/india/ramadan-2026-crescent-moon-sighted-in-delhi-hyderabad-moon-sighting-committee-announces-1st-day-of-ramzan)) |
| Ramazan Bayramı | 20 Mar | **21 Mar**; Kerala 20 Mar ([OneIndia](https://www.oneindia.com/lifestyle/when-is-eid-2026-moon-not-sighted-in-hyderabad-kerala-to-celebrate-eid-ul-fitr-on-march-20-india-8032137.html)) |
| Kurban Bayramı | 27 May | **28 May**; Cammu-Keşmir 27 May. DoPT tatili 27'den 28'e aldı ([People Matters](https://www.peoplematters.in/news/business/bakrid-2026-holiday-moved-from-may-27-to-may-28-says-centre-49897)) |
| Aşure (Muharrem) | 25 Haz | 26 Haz (DoPT listesi; hilale bağlı) |
| 12 Rebiülevvel (Mevlid) | 25 Ağu (kandil 24 Ağu akşamı) | 26 Ağu (DoPT listesi) |

**Sonuç:** Fark sabit değil; o yıl 0 ya da +1 gün oldu ve bölgeden bölgeye değişti. python-holidays Hindistan için Ümmü'l-Kurâ'ya göre +1 gün kaydırma kullanıyor (`calendar_delta_days=+1`).

### Gün adları ve diğer noktalar

- **Gün adları:** Bizim dini günler listemiz Türkiye'nin adlandırması (Regaib, Berat, Kadir kandili…). Güney Asya'da yaygın adlar Shab-e-Barat, Shab-e-Qadr, Shab-e-Miraj, Eid-e-Milad, Muharram. Hangi gecenin ne kadar yaygın kutlandığı **(doğrulanmadı)**.
- **Urduca:** Urduca kullananların çoğu Pakistan'da. Orada da varsayılan Karaçi + Hanefî. Varsayılanlar dile göre değil **konuma göre** seçilmeli.
- **Hristiyan ve Sih kullanıcılar:** Onlar için en yararlı olan resmî tatil listesi ve kendi bayram günleri (§2.5–2.6). Namaz özelliği zaten tanıtımdaki "Vakitler?" sorusuyla kapatılabiliyor.

## 5. Ürünümüzle karşılaştırma: ne eksik?

| Alan | Depoda bugün | Eksik / sorun |
|---|---|---|
| Namaz yöntemi | Site `src/js/prayer.js:31` `methodParams(method.m \|\| "diyanet", …)`. iOS `PrayerExtras.swift` `?? "diyanet"`. Android `PrayerExtras.kt` `getString(METHOD, "diyanet")`. 14 yöntem var, `karachi` dahil. | Konuma göre varsayılan yok. Hindistan'da Diyanet ihtiyat payları ve Şâfiî ikindi çıkıyor. **Widget'lar, saat uygulamaları ve açık veri seçimden bağımsız olarak Diyanet'te kalıyor** (CLAUDE.md). Hindistanlı kullanıcının widget'ında ikindi 41 dk erken görünür. |
| Hicri tarih | `lib/hijri.mjs`, Diyanet tablosu | ±1 gün düzeltmesi yok. Hindistan ve Pakistan için not yok. |
| Takvim (site) | `/hi/calendar/<yıl>/` başlığı "कैलेंडर {year}: तुर्किये की छुट्टियाँ और रमज़ान" | Hindistan tatilleri, Hindu, Sih ve Caynist bayramları ve panchang hiç yok. |
| Takvim (uygulama) | `cal.country` varsayılanı `"TR"` (`CalendarViews.swift:16`, Android `CalendarViews.kt`). 30 ülke var. | **Hindistan listede yok** (`world-holidays.json`). Nager.Date'te de yok: 8 Kasım 2026'da `AvailableCountries` listesinde IN yok, `PublicHolidays/2026/IN` boş (204) dönüyor. Ayrı bir veri kaynağı gerekiyor. |
| Panchang | Yok | Tithi, nakshatra, yoga, karana, sankranti, ay adları, Samvat; Rahu Kaal gibi dilimler. |
| Saka tarihi | Yok | ICU'da hazır, bir satırlık iş. |
| Konum seçimi | Site `place.js`: yalnız 81 il + "konumumu kullan", varsayılan İstanbul. Uygulamada il/ilçe araması + konum. | Hindistan'daki bir şehir elle seçilemiyor. Oysa `places.json`'da 152 Hindistan yeri var. Konum izni vermeyen kullanıcı İstanbul vakitlerini görüyor. |
| Saat farkı sayfaları | `build.mjs:1409` `HOME_CITY = istanbul`, her dilde | Hintçe sayfalar "इस्तांबुल – …" biçiminde. "Hindistan saati ile ABD/BAE/İngiltere farkı" sayfası yok. 31 şehirde Hindistan'dan yalnız Mumbai var; Delhi, Kolkata, Chennai, Bengaluru, Karaçi, Dakka, Katmandu, Kolombo, Maskat ve Kuveyt yok. |
| 12/24 saat | Site `main.js:160` `h12: false`. Uygulamalar `h12` varsayılanı `false`. | Hindistan'da gündelik biçim 12 saatlik (CLDR `h12`), sistem ayarı da izlenmiyor. |
| Burç sayfası | `more-tools.js` `zodiac()` tropikal (ayanamsasız). Hintçe başlık "राशि कैलकुलेटर … चंद्र राशि". | Hindistan'da "chandra rashi" yıldızlara göre anlaşılır. Tropikal hesap ~24° kaydığı için çoğu tarihte panchang'dan farklı burç verir. Yanıltıcı. |
| Köprü tatil | `lib/bridges.mjs`, yalnız Türkiye | Hindistan'da "long weekends" planlaması çok aranan bir konu (ör. [happyfares](https://happyfares.in/blog/long-weekends-2027-india-leave-planner/)); arama hacmi **(doğrulanmadı)**. |
| İlk kurulum | Beş soru (Vakitler, Hava, Takvim, Dünya, Mevsimler) | Hindistan'da "Panchang?" sorusu yok. Vakitler sorusu var, bu iyi. |
| Güçlü yanlarımız | Ay'ın doğuş ve batışı (USNO ile ±1 dk), yeni ay ve dolunay, tutulmalar, gün doğumu ve batımı, hilal görünürlüğü, 18 dil (hi, ur dahil), çevrimdışı hesap, kerahat örneği ("geleneksel zaman dilimi"), dünya tatilleri altyapısı, .ics akışları, widget altyapısı. | Panchang'ın astronomik çekirdeği büyük ölçüde hazır. |

## 6. Öneriler (öncelik sırasıyla)

Efor: **K** ≤1 gün, **O** birkaç gün, **B** 1–3 hafta. Platform: S site, i iOS, A Android.

### Önce: küçük, hatayı gideren işler

**1. Konuma göre namaz varsayılanı** — K · S, i, A
- **Ne yapılır:**
  - Konum Hindistan, Pakistan, Bangladeş ya da Afganistan'daysa varsayılan Karaçi olur ve bir kez "İkindi: Hanefî / Şâfiî" diye sorulur. Kerala'da Şâfiî önerilir.
  - Diyanet ihtiyat payları yalnız Türkiye'de uygulanır.
  - Widget ve saat uygulamaları da seçimi izler.
- **Veri:** yok.
- **Risk:** Mezhep varsayımı hassas bir konu. Bu yüzden önce sorulur, sonra seçilir.
- **Doğrulama:** 5 şehirde (Delhi, Mumbai, Hyderabad, Lucknow, Kozhikode) IslamicFinder ve 2–3 cami çizelgesiyle ±2 dk; mevcut `methods.json` test verisi.

**2. Hicri gün düzeltmesi (−1 / 0 / +1)** — K · S, i, A
- **Ne yapılır:** Düzeltme seçeneği eklenir ve "Hindistan ve Pakistan'da bayram tarihleri hilale göre çoğu zaman bir gün sonra" notu yazılır. Hindistan resmî tatil verisi (madde 7) ilan edilen tarihi kullanır.
- **Veri:** DoPT ve yerel duyurular.
- **Risk:** Fark sabit değil (2026'da 0 ya da +1 oldu).
- **Doğrulama:** 2025–2026 bayramlarının ilan edilen tarihleri.

**3. 12/24 saat varsayılanı dilden ya da sistemden** — K · S, i, A
- **Ne yapılır:** Sitede `Intl…resolvedOptions().hourCycle` kullanılır; uygulamalarda sistemin 24 saat ayarı izlenir. Kayıtlı seçim korunur.
- **Veri:** CLDR.
- **Risk:** Mevcut kullanıcının ayarı değişmemeli.
- **Doğrulama:** `hi-IN`, `en-IN`, `tr-TR` için test.

**4. Elle şehir seçimi** (`places.json`, 1960 yer) — K–O · S, i, A
- **Ne yapılır:** Konum penceresine dünya yerleri araması eklenir. Saat dilimi verisi var.
- **Veri:** Natural Earth (kamu malı).
- **Risk:** Arama dizininin boyutu.
- **Doğrulama:** Mumbai, Lucknow ve Srinagar seçilip vakit, hava ve panchang denenir.

**5. Saka (ulusal) tarihi** — K · S, i, A
- **Ne yapılır:** hi ve en-IN için tarih satırında ve takvimde "17 कार्तिक 1948 शक" gösterilir.
- **Veri:** ICU `indian` takvimi.
- **Risk:** Yok.
- **Doğrulama:** 22 Mart ve 21 Mart'ta (artık yıl) yıl başı; ay sınırları.

**6. Hintçe burç sayfasını düzeltmek** — K · S (+ uygulamadaki burç aracı)
- **Ne yapılır:** Sayfaya "सायन (पश्चिमी)" (tropikal, Batı) etiketi konur, ya da Lahiri'ye göre yıldızsal (nirayana) Ay burcu da gösterilir. "Eğlence amaçlı" notu kalır.
- **Veri:** Kendi hesabımız.
- **Risk:** Astrolojiye kayma riski. Yalnız Ay'ın hangi burçta olduğu yazılır, yorum yapılmaz.
- **Doğrulama:** Drik'in "Moonsign"ı (8 Kasım 2026: Tula).

### Sonra: Hindistan için asıl değer

**7. Hindistan resmî tatilleri** — K–O · S, i, A
- **Ne yapılır:**
  - Yeni dosya `src/data/india-holidays.json`: DoPT Delhi kapalı günleri ve RH listesi (2026, 2027), İslami günler "hilale bağlı" işaretli, DoPT değişiklikleri işlenmiş.
  - Sitede `/hi/public-holidays/india/` ülke sayfası ve .ics akışı.
  - Uygulamada `cal.country = "IN"`; konum Hindistan'daysa varsayılan.
  - **Yıllık görev (Temmuz):** DoPT gelecek yılın listesini yayımlayınca dosya güncellenir.
- **Veri:** DoPT yazıları (devlet belgesi, olgu verisi).
- **Risk:** Eyaletler arası fark büyük. İlk sürüm "merkezî hükümet (Delhi)" diye açıkça etiketlenir.
- **Doğrulama:** Ek-I ve Ek-II ile birebir; python-holidays ile fark raporu.

**8. Panchang çekirdeği `src/js/panchang-calc.js`** — O · S, i, A (test verisi)
- **Ne yapılır:**
  - Hesaplananlar: tithi, paksha, karana, nakshatra, yoga, vaar ve bitiş anları; amanta/purnimanta ay adı (adhika dahil); Vikram ve Shaka yılı; sankranti; Lahiri ayanamsa.
  - Güneş hesabı 1″ düzeyine yükseltilir.
  - `gen-vectors` ile Swift ve Kotlin'e aktarılır.
- **Veri:** Kendi hesabımız (Meeus ve VSOP87 katsayıları yayımlı).
- **Risk:** Sınıra yakın durumlar (2029'da 0,6 saat).
- **Doğrulama:** §7'de.

**9. Panchang ekranı, kartı ve geleneksel zaman dilimleri** — O · S, i, A
- **Ne yapılır:**
  - Sitede `/hi/panchang/` ve `/en/panchang/`; şehir sayfaları.
  - Uygulamada "Panchang" özelliği (Hindistan'daki yeni kurulumda ilk kurulum sorusu), saat ekranı alanı, widget (bugünün tithi'si + gün doğumu).
  - Rahu Kaal, Yamaganda, Gulika, Abhijit, Brahma muhurta varsayılan olarak gösterilir. Choghadiya isteğe bağlı ve kapalı başlar.
  - İsteğe bağlı bildirimler: Ekadashi, Purnima, Amavasya, Sankranti.
- **Veri:** Madde 8.
- **Risk:** Dilimler gelenektir. Metin kerahatteki gibi tarafsız olmalı: "geleneksel olarak kaçınılır / uygun sayılır". Hintçe terimler ana dil konuşuruna denetletilmeli.
- **Doğrulama:** 5 şehir × 30 gün, Drik'le ±1 dk (Delhi örneği §2.2'de tuttu).

**10. Saat farkı sayfalarını IST'ye göre kurmak; şehir eklemek** — O · S
- **Ne yapılır:** `HOME_CITY` dile göre seçilir: hi → New Delhi. Delhi, Kolkata, Chennai, Bengaluru, Hyderabad, Karaçi, Lahor, Dakka, Katmandu, Kolombo, Maskat, Kuveyt ve Chicago eklenir (18 dilde ad).
- **Veri:** `cities.json`, Natural Earth.
- **Risk:** Sayfa sayısı artar (SEO, sitemap).
- **Doğrulama:** `npm test` çift denetimleri.

**11. Hindistan için köprü tatil** — K (madde 7'den sonra) · S, i, A
- **Ne yapılır:** `lib/bridges.mjs` Hindistan verisiyle çalıştırılır (hafta sonu: 2. ve 4. Cumartesi kuralı bankalar içindir, devlet daireleri için Cumartesi/Pazar).
- **Veri:** DoPT.
- **Risk:** Hafta sonu kuralı kuruma göre değişiyor **(doğrulanmadı)**.
- **Doğrulama:** 2027 listesiyle elle karşılaştırma.

### En son: büyük işler

**12. Bayram ve oruç motoru** — B · S, i, A
- **Ne yapılır:** ~25 Hindistan geneli bayram ve oruç günü kurallardan hesaplanır: Ekadashi (Smarta ve Vaishnava ikisi birden), Purnima, Amavasya, Pradosh, Sankashti. Kuzey/Güney ve Smarta/Vaishnava seçimi olur. Gelenekler ayrılınca iki tarih birden gösterilir.
- **Veri:** Kurallar Dharmasindhu ve Nirnayasindhu esaslı, Drik'in açıklamalarından derlenir.
- **Risk:** **Bölgesel anlaşmazlık** (Diwali 2024). Resmî tatil işareti yalnız DoPT'tan gelir.
- **Doğrulama:** 2026–2027 DoPT günleri birebir tutmalı; python-holidays ile 2001–2035 farkı; Drik'le örnekler.

**13. Güneş takvimleri ve bölgesel yılbaşları** — B · S, i, A
- **Ne yapılır:** Tamil, Malayalam, Bengal ve Odia ay ve günleri; Puthandu, Vishu, Pohela Boishakh, Baisakhi, Ugadi, Gudi Padwa.
- **Veri:** Madde 8 (iyi Güneş hesabı şart).
- **Risk:** Ay başlangıcı kuralı kaynaklarda çelişiyor (Bengal).
- **Doğrulama:** Bölgesel panjika ve Drik bölgesel takvimleri; Apple'ın takvimleriyle çapraz bakış.

**14. Sih, Caynist ve Budist günleri** — K–O (veri) · S, i, A
- **Ne yapılır:** SGPC'nin yıllık jantrisine göre gurpurablar ve Paryushana; Buddha Purnima motordan.
- **Veri:** SGPC ve topluluk takvimleri.
- **Risk:** Nanakshahi tartışması. Hangi takvim kullanıldıysa açıkça yazılır.
- **Doğrulama:** DoPT ve RH listesi.

**15. Eyalet tatilleri** — B (bakım yükü) · S, i, A
- **Ne yapılır:** RBI listesinden eyalet bazında banka tatilleri.
- **Veri:** RBI (aylık yayın).
- **Risk:** Bakım yükü yüksek, sık değişiyor.
- **Doğrulama:** RBI karşılaştırması.

**16. Yeni Hint dilleri** (bn, ta, te, mr, gu, kn, ml) — B (her dil için) · S, i, A
- **Ne yapılır:** Önce yalnız panchang terimleri çevrilir.
- **Veri:** Kendi çevirimiz.
- **Risk:** Çeviri kalitesi.
- **Doğrulama:** Ana dil denetimi (dil denetimi sürecine eklenir).

Menü notu: Hintçe menüde "Panchang" bağlantısı Namaz'ın yanına konabilir. Namaz kalmalı, çünkü 170 milyonun üstünde Müslüman kullanıcı var. Sıralama sahibin kararı.

## 7. Doğrulama planı (somut)

1. **Konum:** JPL Horizons'tan 2026–2031 arasında ~2000 anlık Güneş ve Ay görünür ekliptik boylamı alınır. Hedef |Δλ☉| < 2″, |Δλ☾| < 15″. Bugün Güneş +32,6″, Ay ≤10″.
2. **Sınır anları:** Delhi, Mumbai, Kolkata, Chennai ve Bengaluru'da yıla yayılmış 60 gün için tithi, nakshatra, yoga ve karana bitişleri Drik'le elle karşılaştırılır (≤300 değer, depoya girmez). Hedef ±1 dk. Bugünkü prototip 8/8.
3. **Sankranti:** 2026–2027'nin 24 geçişi Drik ve Rashtriya Panchang'la karşılaştırılır. Hedef ±2 dk. Bugün −6 ile −15 dk arası; Güneş yükseltmesiyle düzelmeli.
4. **Takvim yapısı:**
   - Artık aylar 2026, 2029 ve 2031 (ilk ikisi prototipte tuttu, üçüncüsünü kaynaklar netleştirmeli).
   - Rashtriya Panchang'ın 4 büyükşehir tablolarıyla ay adları.
   - Yıllık kshaya ve vriddhi tithi sayısı.
5. **Zaman dilimleri:** 5 şehir × 30 gün, ±1 dk.
6. **Bayramlar:** DoPT 2026–2027'deki Holi, Ram Navami, Mahavir Jayanti, Buddha Purnima, Janmashtami (Vaishnava), Dussehra, Diwali ve Guru Nanak birebir tutmalı. python-holidays ile 2001–2035 fark raporu çıkarılır, farklar elle incelenir.
7. **Namaz:** Karaçi + Hanefî, 5 şehirde ±2 dk. Kerala'da Şâfiî.
8. **Platform eşliği:** `mobile/shared/gen-vectors.mjs` ile panchang test verisi; Swift ve Kotlin'de 1 sn ve 10⁻⁶° toleransla (mevcut yöntem).
9. **Dil:** Hintçe panchang terimleri (तिथि, नक्षत्र, योग, करण, राहु काल …) ve Urduca İslami terimler ana dil konuşuruna denetletilir.

## 8. Yapılmamasını önerdiklerim

- **Kişiye özel ve yorum içeren her şey:** kundali, rashifal (günlük burç yorumu), eş uyumu (Ashta Kuta), dosha, taş önerisi, "bugün sizin için uğurlu" türü öneriler, prashnavali, Vastu pusulası.
  - Ürünün tarafsız ve olgu temelli çizgisiyle çelişir.
  - App Store kuralı 4.3(b) "fortune telling" kategorisini doymuş sayıyor ([Apple](https://developer.apple.com/app-store/review/guidelines/)).
- **Varjyam, Amrit Kaal, Dur Muhurat:** nakshatra'ya bağlı ghati tablolarıyla hesaplanıyor ve astrolojiye daha yakın. İlk sürümde olmasın, istenirse sonra bakılır.
- **İçerik paketleri:** dua sesleri, aarti ve chalisa metinleri, Gita kayıtları. Telif ve çeviri hakları ayrıca araştırılmadan eklenmesin.
- **Swiss Ephemeris ve AGPL kod:** kullanılmasın (§3.6).

---

**Özet.** Hindistanlı kullanıcı için en yararlı iş, reklamsız ve çevrimdışı çalışan, konuma göre doğru bir panchang ile Hindistan resmî tatilleridir. Panchang'ın öğeleri tithi, nakshatra, yoga, karana, ay adı, artık ay, gün doğumu, Ay'ın doğuşu ve Rahu Kaal gibi geleneksel dilimlerdir. Bunlara ulusal Saka tarihi ve 12 saatlik biçim eklenir. Depodaki Meeus hesabı Ay'a bağlı öğelerde Drik Panchang'la ±1 dk tutuyor ve 2026 ile 2029 artık aylarını doğru buluyor. Güneş'in burç geçişleri için Güneş hesabının 1″ düzeyine çıkarılması gerekiyor (bugün +33″, sankrantide 6–15 dk erken). Hindistan'daki ~170–200 milyon Müslüman için hemen düzeltilmesi gereken iki şey var: konuma göre Karaçi + Hanefî varsayılanı (bugün Delhi'de ikindi 41 dk erken çıkıyor, widget'lar da Diyanet'te kalıyor) ve ±1 günlük hicri düzeltmesi (2026'da Ramazan Bayramı, Kurban Bayramı ve Aşure Hindistan'da bir gün sonraydı). Ürünün Hintçe yüzü bugün Türkiye merkezli: takvim başlığı Türkiye tatilleri, uygulama takvimi varsayılan olarak TR açılıyor ve Hindistan listede yok, saat farkı sayfaları İstanbul'a göre, konum seçicide yalnız iller var, burç sayfası Hint geleneğine uymayan tropikal hesap kullanıyor. Önerilen sıra şöyle: önce küçük düzeltmeler (yöntem, hicri, 12/24, şehir seçimi, Saka, burç etiketi), sonra DoPT tabanlı tatil verisi ve panchang çekirdeği, en son kurala dayalı bayram motoru ve bölgesel güneş takvimleri. İçerik takvim ve astronomi olgularıyla sınırlı kalmalı. Geleneksel zaman dilimleri kerahat örneğindeki gibi tarafsız sunulmalı. Kişiye özel astroloji ve yorum hiç girmemeli.
