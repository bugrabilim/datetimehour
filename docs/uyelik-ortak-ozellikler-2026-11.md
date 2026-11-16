# Üyelikle ortak yapılabilecek özellikler (araştırma, 15 Kasım 2026)

Sahibin isteği (15 Kasım 2026): "başka hangi eklentiler ortak yapılabilir araştırılsın". Üç ajan (salt okuma, giriş
yok): ibadet uygulamaları; alarm, uyku, odak ve alışkanlık uygulamaları; takvim, hava, gökyüzü uygulamaları ve mağaza/
KVKK kuralları. Bilgilerin çoğu mağaza açıklamalarından ve geliştirici sayfalarından; bunlar iddiadır, ücretler çoğu
yerde "bilinmiyor", mağaza yorumları okunamadı. Kaynaklar en sonda.

Zaten kararlaştırılanlar (`docs/uyelik-plani-2026-11.md`): ortak zikir ve hatim halkaları, uyanma grupları, cami
notlarının paylaşımı. Bilerek yok: herkese açık akış/sohbet, mesajlaşma, kişinin namaz/oruç günlüğünün ya da konumunun
sunucuya gitmesi.

## Özet: önerilen yeni ortak özellikler
Hepsi ücretsiz, yalnız üyelik ister; hepsi davetle (bağlantı + kod), serbest metin ya da fotoğraf yok ya da yalnız
sahibine/çemberine görünür.

| # | Özellik | Rakipte | Bize uygunluk | Moderasyon / gizlilik riski |
|---|---|---|---|---|
| 1 | **Ortak geri sayım** (bağlantı ya da kodla katılınır; sahibi düzenler, katılanların telefonunda widget ve hatırlatma) | Outside, Reach, Count Me In, TimeTree | Çok yüksek: geri sayım altyapısı hazır | Düşük: başlık ve tarih; fotoğraf yalnız sahibinde kalır |
| 2 | **Ortak dünya saati listesi** ("ailem", "ekibim": herkesin şehri, yerel saati, mesai saati) | Every Time Zone, Timezone.io, Spacetime | Çok yüksek: dünya saatleri ve toplantı planlayıcı hazır | Düşük: yalnız ad ve şehir |
| 3 | **Hesaplı toplantı oylaması** (bugünkü bağlantılı oylamanın kalıcı hâli: sonuç kendiliğinden toplanır) | Doodle, When2meet | Yüksek | Çok düşük: yorum alanı yok |
| 4 | **Ortak odak odası** (aynı anda başlayan pomodoro; kim tamamladı görünür, "biri çıkarsa herkes kaybeder" cezası yok) | Forest (Plant Together), Flora, YPT | Orta–yüksek | Düşük; baskı yaratmaması için ceza ve sıralama yok |
| 5 | **Onaylı "alarm isteği"** (arkadaş bir alarm önerir, alıcı onaylarsa kendi telefonunda kurulur) | Alarm Friend (yalnız iPhone, abonelikli) | Orta | Orta: istenmeyen istek; yalnız karşılıklı arkadaşlar, günlük sınır, engelleme |
| 6 | **"Bu gece izledim" gözlem sayacı** (tutulma, meteor yağmuru, dolunay; kişisel gözlem günlüğü + çemberde toplam) | SkySafari LiveSky, Globe at Night | Orta | Çok düşük: seçenek ve sayı |
| 7 | **Anonim hava/gökyüzü bildirimi** (mPING gibi: "yağmur var", "gökyüzü açık"; yalnız seçenek, toplu sayı) | mPING (NOAA) | Orta | Orta: konum gider (şehir düzeyine yuvarlanmalı), kötüye kullanım sınırı gerekir |

Kararlaştırılan işlere öneriler:
- **Hatim halkası:** piyasadaki en yaygın model (Muslim Pro Khatam en çok 30 kişi; Türkçe "Ortak Hatim", "Hatemât");
  üst sınır 30 kişi uygun. Üye cüzü bırakabilir, durum "boş / alındı / bitti".
- **Zikir halkası:** yalnız ortak toplam; kişi sıralaması yok (Tarteel'deki gibi tablolar yarışa ve gösterişe kayıyor).
- **Uyanma grupları:** namazla bağ kurulmasın (dini veri olur); yalnız "kalktım" anı gider, alarm saati ve uyku kaydı
  gitmez; "bugün paylaşma" ve sessiz duraklatma; ortak seri (Duolingo'daki gibi en çok 5 kişi) baskı yaratmasın diye
  dondurulabilir.
- **Cami notları:** serbest metin yerine önce seçenek işaretleri (abdest yeri, kadınlar bölümü, otopark, Cuma saati;
  MasjidSpot modeli); serbest not yalnız sahibine ya da çemberine görünsün.

Önerilmeyenler: namaz/oruç arkadaşı ("kim kıldı" listesi; günlük sunucuya gider, en yüksek KVKK riski), kişi sıralama
tabloları, herkese açık dua isteği ya da akış (Muslim Pro Ummah Pro), görüntülü okuma ya da çalışma odası (Focusmate,
Tadarus Online), herkese açık fotoğraf noktası paylaşımı (PhotoHound), yabancıyla eşleşme, aile cihaz denetimi.

## Her ortak özellikte uyulacaklar
- **Apple 1.2:** içerik süzgeci, şikâyet ve zamanında yanıt, engelleme, yayımlanmış iletişim adresi. İncelemeci kendi
  hesabını açar, her içerik türünde "Bildir" düğmesi arar, engellenenin içeriğinin gerçekten kaybolduğuna bakar.
  **5.1.1(v)** hesap silme uygulamada (bizde var). Yaş derecesi sorularında kullanıcı içeriği dürüst yanıtlanır;
  akışsız, davetli grup "sosyal medya" sayılmayabilir.
- **Google Play UGC:** içerik oluşturmadan önce kullanım şartlarının kabulü, uygulama içi şikâyet (kapalı gruplarda
  da), birebir etkileşim varsa engelleme, içerik derecelendirmesinde kullanıcı içeriği; hesap silme web adresi (bizde
  `/hesap-silme/`).
- **KVKK m. 6 ve GDPR m. 9:** zikir/hatim halkası, cami notu gibi özellikler kişinin dinini ele verebilir; ayrı
  aydınlatma ve **ayrı açık rıza** (işaretsiz gelen kutu, geri alınabilir), kayıt onayına bağlanmaz. GDPR 9(2)(d)
  (dini kuruluş üyeleri) bizi kapsamaz. Hukukçuya gösterilir.
- **Tek kişilik moderasyon:** serbest metin ve fotoğrafı kaynağında sınırla (seçenek, sayı, emoji); yalnız davetli
  gruplar, sahibi üyeyi çıkarır; her içerikte "Bildir", şikâyet e-postayla sahibe; üç ayrı kişiden şikâyette içerik
  kendiliğinden gizlenir, karar sahipte; "Engelle"; yanıt süresi 24–72 saat; yeni hesaba ilk gün sınırı; inceleme
  için deneme hesabı ve notta moderasyon akışı; hesap silinince grup verisi `on delete cascade`, sahibi silinen grup
  en eski üyeye geçer (standart "Başka üyelerle ortak içerik").

## Ajan raporlarından ayrıntı
### İbadet uygulamaları
- Grup hatim (davet bağlantısı/kodu, cüz dağıtımı): Nukmil, Maqra, QuranRead, HaTeam, Iqraa, Khatmah, Khatma.eu; Türkçe
  Ortak Hatim, Hatim Dua, Hatemât; Muslim Pro Khatam (en çok 30 kişi, 2021 kaynağı, eski). Serbest metin olmadığı için
  moderasyon yükü yok.
- Grup zikir: Wird (bağlantı/QR), Ajr, Zikar (sıralama tablolu), GoDhikr.
- Sıralama tablosu: Tarteel Groups (aylık sıfırlanır), Ramadan Challenge (6 harfli kod).
- Namaz arkadaşı: Mizan, PrayPic, Prayer Nudge, Everyday Muslim, The Muslim App, Dua Lock: bizim kuralımıza aykırı.
- Akış: Muslim Pro Ummah Pro (bildir düğmesi, 30 gün askı; katılmak için ad, doğum tarihi, cinsiyet), Athan Global Feed.
- Güven eşiği: 2020'de Muslim Pro'nun konum verisinin X-Mode üzerinden satıldığı iddiası (şirket askere satışı
  yalanladı).
- Cami: MasjidSpot (seçenek onayı), Masjidal Athan+ (cami kendi duyurusunu girer); cami notu paylaşan uygulama
  bulunamadı.

### Alarm, uyku, odak
- Alarm Friend: arkadaş alarm önerir, alıcı onaylar (Bekliyor/Kuruldu/Reddedildi), karşılıklı arkadaşlık şart;
  haftalık 1,99 $, yıllık 9,99 $.
- Ortak uyanma: Wake: The Social Alarm, weWake, RingLink (kim zamanında kalktı), Snooze Squad, Alarmy (telefon
  numarasıyla).
- Forest "Plant Together" (ücretsiz kademede; iOS ve Android aynı odada), Flora; şikâyetler: arkadaşı uygulamaya çekmek
  zor, ceza mekaniği.
- YPT: çalışma grubu, canlı durum, günlük toplam (gerçek zamanlı sıralama da var).
- Duolingo arkadaş serisi: en çok 5 kişi; şirkete göre ortak serisi olanlar günlük dersi %22 daha çok bitiriyor.
- Habitica partisi en çok 30 kişi; sohbet moderasyon yükünü getiriyor.
- Finch: arkadaşla hedef paylaşımı, mesajlaşma yok.

### Takvim, hava, gökyüzü
- Ortak takvim: TimeTree, Cozi, FamilyWall (yorum/sohbet var; biz yorumsuz isteriz).
- Ortak geri sayım: Outside, Reach (iPhone ve Android eşit), Count Me In: bağlantıyla paylaşım, tarayıcıda görülür.
- mPING: hava türü seçilir, anonim, en sık 30 sn'de bir, kalite denetimi biçim kontrolü.
- SkySafari LiveSky: gözlem kaydı ve topluluk istatistiği; Globe at Night: 180 ülkeden 200 binden fazla ölçüm.
- PhotoPills yalnız plan/konum paylaşımı (e-postayla); PhotoHound, Locationscout herkese açık: önerilmez.
- Weather Underground, Windy, Meteum, Stellarium, Dreamdays: bu aramada kaynak çıkmadı (bilinmiyor).

## Kaynaklar (erişim 15 Kasım 2026)
- Apple: https://developer.apple.com/app-store/review/guidelines/ ,
  https://developer.apple.com/help/app-store-connect/reference/age-ratings/ , https://developer.apple.com/news?id=ey6d8onl
- Google Play: https://support.google.com/googleplay/android-developer/answer/9876937 ,
  https://support.google.com/googleplay/android-developer/answer/13327111
- KVKK/GDPR: https://www.kvkk.gov.tr/Icerik/8184/Ozel-Nitelikli-Kisisel-Verilerin-Islenmesine-Iliskin-Rehber ,
  https://hukukcularevi.com/ozel-nitelikli-kisisel-veriler-kvkk-6/ , https://gdpr-info.eu/art-9-gdpr/
- Hatim/zikir: https://www.muslimpro.com/giftofkhatam , https://www.nst.com.my/amp/lifestyle/bots/2021/05/691536/tech-muslim-pro-updates-app (eski),
  https://support.tarteel.ai/en/articles/12414382-groups-and-leaderboards , https://hunted.space/product/nukmil ,
  https://apps.apple.com/us/app/hateam/id6756115835 , https://khatma.eu/?lang=en , https://apps.apple.com/ph/app/id6742418688 ,
  https://apps.apple.com/us/app/id6446065807 , https://apps.apple.com/us/app/wird-dhikr-tracker/id6749483583 ,
  https://alternativeto.net/software/zikar/about/ , https://apps.apple.com/app/id1006351719
- Akış ve gizlilik: https://support.muslimpro.com/hc/en-us/articles/43187939777817-Community-Ummah-Pro ,
  https://www.soyacincau.com/2020/11/18/did-muslim-pro-sell-user-data-to-the-us-military-heres-what-we-know-so-far/ ,
  https://www.foxglove.org.uk/?p=559
- Cami: https://mwm.ai/apps/masjidspot/6762530840 , https://apps.apple.com/app/id1524990518
- Uyanma ve alarm: https://apps.apple.com/app/id6756028457 , https://appgoblin.info/apps/6754577206 ,
  https://www.producthunt.com/products/wewake-2/reviews , https://apps.apple.com/mt/app/id6760540608 ,
  https://apps.apple.com/app/id6759291185
- Odak ve alışkanlık: https://apps.apple.com/us/app/id866450515 , https://makeheadway.com/blog/forest-app-review/ ,
  https://apps.apple.com/ie/app/id1225155794?l=en-gb , https://apps.apple.com/us/app/yeolpumta/id1441909643 ,
  https://blog.duolingo.com/friend-streak/ , https://habitica.fandom.com/wiki/Guilds ,
  https://www.internetmatters.org/advice/apps-and-platforms/wellbeing/finch/ , https://lifestack.ai/blog/focusmate-pricing
- Takvim ve geri sayım: https://www.cubbily.com/family-calendar-app-comparison , https://outside.so/shared-countdown ,
  https://www.reachus.app/en/blog/shared-countdown-friends , https://apps.apple.com/app/id6673885018
- Hava ve gökyüzü: https://mping.nssl.noaa.gov/faq.php , https://globeatnight.org/about ,
  https://userguide.skysafariastronomy.com/app-specific/social-stargazing-help
- Saat: https://everytimezone.com , https://alternativeto.net/software/spacetime/
