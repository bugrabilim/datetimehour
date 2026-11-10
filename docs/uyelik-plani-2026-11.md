# Üyelik planı (10 Kasım 2026; sahibin kararlarıyla)

Sahibin kararları: üyelikle gelecek işler **ücretsizdir, yalnız üyelik ister** (10 Kasım 2026); bağış ayrı
değerlendirilir; **sitede üyelik yok** (#102, standart da statik sitede yasaklar). Üyelik yalnız iOS ve Android
uygulamalarında. Sahibin yanıtları (10 Kasım 2026): sıra uygun; **giriş e-posta ve şifreyle, Google, Apple ya da
başka giriş yok**; aydınlatma ve açık rıza metinlerini Claude yazar; şikâyetlere sahip bakar. Aşama 1'in sunucusu
`server/`'da yazıldı (aşağıda "Durum").

## Üyelikle gelecek işler
| Aşama | İş | Kaynak |
|---|---|---|
| 1 | **Hesap:** kayıt/giriş, görünen ad, çıkış, e-posta değiştirme, verilerimi indir, hesabı sil | Standart madde 7 |
| 2 | **Ortak zikir ve hatim halkaları:** davetle kurulan küçük gruplar; ortak hedefli zikir (her üye kendi sayacından ekler, canlı toplam, bitiş tarihi) ve 30 cüzün paylaştırıldığı hatim (cüz al, bitince işaretle, hatim bitince yenisi) | 2. analiz #75 |
| 3 | **Arkadaşlarla uyanma grupları:** 2–10 kişi; alarmı kapatınca "uyandım" saati gruba gider (grup başına aç/kapa), grubun ortak serisi; sanal para yok | 2. analiz #53 |
| 4 | **Favori camilere yazılan notların paylaşımı:** kişi notunu paylaşmayı seçerse aynı camiyi açan başkaları görür | 4 Kasım 2026 kararı |

Bilerek yok: herkese açık sohbet/akış, mesajlaşma, radyo, fetva araması, kişinin namaz/oruç takibinin ya da
konumunun sunucuya gitmesi (bunlar telefonda kalır).

## Mimari
- **Yeni sunuculu uygulama** (Coolify, ana altyapının kurduğu): aynı depoda `server/` klasörü, Node 22.23.4,
  çerçevesiz `node:http` ve **Better Auth** 1.7.14 (MIT; e-posta + şifre, e-posta doğrulama, şifre sıfırlama,
  mobil için taşıyıcı belirteç, giriş denemesi sınırı; şifre özeti ve oturum mantığı elle yazılmaz, standart
  madde 7) ve `pg` 8.23.1; iki bağımlılık, sürümler sabit.
  Adres önerisi `hesap.time.bumba.tr` (`time.bumba.tr/api/v1/` zaten açık veri olduğu için ayrı alt alan).
  Site statik kalır; sitenin Coolify izleme yolları `server/**`'u kapsamaz.
- **Veritabanı:** merkezi PostgreSQL 18, ayrı veritabanı ve rol, `DATABASE_URL` ortamda; gece R2 yedeği. Yalnız
  ekleyici değişiklik (yeni tablo/sütun).
- **E-posta:** doğrulama, şifre sıfırlama, adres değişikliği onayı, "bu adresle hesabınız var", kullanılmayan
  hesap uyarısı ve silme bildirimi; Resend ile `bilgi@bumbagroup.com`'dan, gönderen adı "Saat Tarih", 19 dilde
  (`server/i18n/mail.json`). Tanıtım e-postası yok (ortak liste ayrı; "üye olmak listeye katılmak değildir").
- **Giriş (madde 7 tam):** e-posta + şifre (8–128 karakter); kayıt e-posta doğrulaması ister (bağlantı 24 saat),
  doğrulanmadan giriş yok; şifre sıfırlama (bağlantı 1 saat, tek kullanım, bütün oturumlar kapanır); aynı adresle
  yeniden kayıt hesabın varlığını ele vermez (yanıt aynı, sahibine e-posta gider). Google ve Apple ile giriş yok
  (sahibin kararı); Apple'ın 4.8 kuralı yalnız üçüncü taraf girişi sunan uygulamalara Apple ile girişi şart koşar.
- **E-postadaki bağlantılar sitede açılır** (statik, noindex): şifre sıfırlama formu `time.bumba.tr/hesap/sifre/`
  (diğer dillerde `/<dil>/account/password/`) betiksiz `hesap.time.bumba.tr/sifre`'ye gönderir ve sonuçla döner;
  doğrulama sonucu `/hesap/dogrulandi/` (`/<dil>/account/verified/`). Sitenin CSP `form-action`'ına
  `https://hesap.time.bumba.tr` eklenir. Sunucu kullanıcıya sayfa göstermez (çerez bandı ve 19 dil sitede var).
- **Mobil:** belirteç iOS'ta Anahtar Zinciri'nde, Android'de Keystore ile şifreli; hesap ekranı Ayarlar › Hesap
  (görünen ad, e-posta, dil ve tema [uygulamadaki ayarlar], e-posta listesi formu, verilerimi indir, çıkış, hesabı
  sil). Hesap yoksa uygulamanın bugünkü her özelliği aynen çalışır; giriş yalnız grup özelliklerinde istenir.
- **Davet bağlantısı:** `https://time.bumba.tr/davet/#KOD` (statik, noindex sayfa; uygulama kuruluysa evrensel
  bağlantıyla uygulamada açılır, değilse mağaza bağlantıları). Sitede `/.well-known/apple-app-site-association` ve
  `assetlinks.json`. Kod adresin `#` kısmında olduğu için sunucuya gitmez.
- **Anlık bildirim (APNs/FCM):** aşama 3'e kadar yok; grup verisi uygulama açılınca ve arka plan yenilemesinde
  gelir. Uyanma grupları için "Ayşe uyandı" bildirimi APNs anahtarı ister (sahipte).
- **Yönetim paneli (madde 10):** `hesap.time.bumba.tr/panel`, yalnız yönetici (bilgi@bumbagroup.com; hesap
  uygulamadan açılıp doğrulanınca yönetici olur, panele e-posta ve şifreyle girilir; httpOnly oturum çerezi). İstek sayısı, yanıt süresi, hata oranı, veritabanı boyutu, üye sayısı ve yeni kayıtlar, grup sayıları
  (toplu), sitenin Umami özeti (anahtar sunucuda), servis durumu, durum.bumba.tr bağlantısı; iki dil, iki tema,
  noindex. Ölçülemeyen satırda "bu ortamda ölçülemiyor" (ör. sitede arama sözcüğü tutulmuyor).

## Veri (en az)
- Hesap: e-posta, şifre özeti (scrypt; şifrenin kendisi tutulmaz), görünen ad (takma ad olabilir), arayüz dili,
  kayıt günü, son kullanım günü. Oturumlarda IP adresi ve cihaz/uygulama sürümü (güvenlik ve giriş denemesi sınırı
  için; oturum bitince silinir). Konum, reklam kimliği yok; Umami ve istatistik uygulamada yok.
- Halka: ad, kurucu, üyeler (görünen adlarıyla), zikir hedefi ve toplamlar, hatim cüz durumları. Kişinin zikir
  sayısı halkadaki payı kadar tutulur; günlük dökümü yok.
- Uyanma grubu: üyenin o sabahki uyanma saati (yerel, dakika), 30 gün tutulur, sonra silinir.
- Cami notu: not metni, caminin kimliği (OSM ya da Apple Haritalar) ve yuvarlatılmış konumu; yazarın görünen adı
  isteğe bağlı.
- **Silme:** hesabı silince her şey hemen silinir (yedeklerde en geç yedek döngüsü kadar kalır, gizlilikte
  yazar). 7 gün içinde doğrulanmayan hesap silinir. 24 ay kullanılmayan hesaba silmeden 30 gün önce e-posta
  gider; o sürede giriş yapılmazsa silinir (`server/src/cleanup.mjs`, 6 saatte bir).
- **Din verisi:** zikir/hatim halkasına katılmak KVKK'da özel nitelikli kişisel veri (din ve inanç) sayılabilir.
  Önerim: halkaya ilk katılışta ayrı ve açık bir onay ("Bu halkaya katılmanız inancınızla ilgili bilgi
  sayılabilir; …"), takma ad kullanılabilmesi, halka listelerinin herkese açık olmaması, sunucuda şifreli disk.
  Aydınlatma ve açık rıza metinlerini Claude yazar (sahibin kararı), yayından önce sahip bakar.

## Mağaza ve yasal
- **Apple:** uygulama içinde hesabı silme (5.1.1(v)); kullanıcı içeriği için süzgeç, şikâyet, engelleme ve
  iletişim (1.2): grup adı, görünen ad ve cami notu bunun içinde; şikâyetlere sahip bakar. Apple ile giriş zorunlu
  değil (yalnız kendi hesabımız var, 4.8). Gizlilik etiketleri değişir (e-posta, kullanıcı kimliği, diğer kullanıcı
  içeriği; kimlikle bağlantılı, izleme yok). Yaş derecelendirme soruları (kullanıcı içeriği) yeniden yanıtlanır,
  derece değişebilir. `APP_REVIEW_NOTES.md`'ye inceleme için deneme hesabı (e-posta ve şifre) yazılır; hesabın
  şifresi depoya değil App Store Connect'in inceleme bilgisine girer.
- **Google Play:** hesabı silme için web adresi şart: sitede statik, noindex `/hesap-silme/` sayfası (uygulamadan
  nasıl silinir; uygulamaya erişemeyen hesabın e-postasından bilgi@bumbagroup.com'a yazar). Veri güvenliği formu
  güncellenir.
- **KVKK:** veri sorumlusu Bumba Teknoloji Ltd. Şti.; gizlilik sayfasına 19 dilde "Hesap" bölümü; e-posta
  gönderimi (Resend) ve sunucunun yeri nedeniyle yurt dışına aktarım cümlesi hukukçuya gösterilir.
- **Moderasyon:** şikâyet edilen içerik panelde bir kuyruğa düşer; üç ayrı şikâyette içerik kendiliğinden gizlenir.
  Madde 10 panelde tek kullanıcının içeriğini göstermeyi yasaklar; şikâyet edilen içeriği (yazarın kimliği
  olmadan) görmek için aşama 4'te istisna talebi gerekir.

## İş sırası
1. **Hesap** (sunucu, Better Auth, e-posta + şifre, panel, hesap ekranları, sitedeki şifre ve doğrulama sayfaları,
   19 dil, aydınlatma, gizlilik, testler). Sunucunun ilk yayını standart gereği **ana altyapı oturumundan**, sahibin oradaki onayıyla.
2. **Ortak zikir ve hatim** (davet bağlantısı, evrensel bağlantılar, şikâyet/engelleme).
3. **Uyanma grupları** (APNs anahtarı gelirse anlık bildirim).
4. **Cami notları** (moderasyon kuyruğu, istisna talebi).

Her aşama ayrı PR; uygulama yayını (TestFlight, App Store 1.x) her seferinde sahibin onayıyla. Android, Play
hesabı açılana kadar yalnız kodda.

## Sahipte olanlar (aşama 1 için)
- Aşama 1'de yok (Apple ile giriş olmadığı için Apple Developer'da değişiklik gerekmez). Aşama 2'de davet
  bağlantıları için **Associated Domains**.
- Resend anahtarı ve rastgele gizliler: ana altyapı oturumu (standart "Ana altyapı istekleri").
- Aşama 3'te APNs anahtarı.

## Ana altyapı isteği (kod hazır olunca gönderilecek)
- Yeni sunuculu uygulama: ad `saat-hesap`, depo `bugrabilim/datetimehour`, dal `main`, temel klasör `server/`,
  `server/Dockerfile`, port 8080, sağlık `/saglik`, izleme yolları `server/**`, adres `hesap.time.bumba.tr`
  (DNS + HTTPS).
- Merkezi PostgreSQL 18'de veritabanı ve rol `saat_hesap`, `DATABASE_URL`; gece R2 yedeği.
- Ortam: `NODE_ENV=production`, `DATABASE_URL`, `BETTER_AUTH_SECRET` (en az 32 karakter, ana oturum üretir),
  `RESEND_API_KEY` (ana oturum), `UMAMI_SHARE_ID` (sitenin Umami paylaşım kimliği; panel için, sunucuda kalır;
  yoksa panelde "bu ortamda ölçülemiyor"). Varsayılanı uygun olanlar: `PUBLIC_URL`, `SITE_URL`, `MAIL_FROM`,
  `ADMIN_EMAIL`, `UMAMI_URL`. Coolify'ın `SOURCE_COMMIT`'i panelde son yayın olarak görünür.
- Node derleme sürümü 22.23.4; durum.bumba.tr izlemesine ekleme (ilk yayından sonra).
- Site (`saat`): yalnız normal site yayını (hesap sayfaları, `/hesap-silme/`, CSP `form-action`; aşama 2'de
  `/davet/` ve `.well-known`). Site imajı `server/`'ı içermez (`.dockerignore`).

## Sahibin yanıtları (10 Kasım 2026)
1. Sıra (hesap → ortak zikir ve hatim → uyanma grupları → cami notları): **uygun**.
2. **Normal üyelik: e-posta ve şifre.** Google ya da başka giriş eklenmeyecek. Madde 7 tam uygulanır, istisna
   talebi yok.
3. Aydınlatma ve açık rıza metinlerini **Claude yazar**. Sahibin sorusu: "genel Bumba metinlerinde yok mu?"
   Yanıt: bumbagroup.com'da yalnız ortak çerez politikası ve ortak e-posta listesinin aydınlatma metni var; standart
   madde 11'e göre kişisel veri toplayan her formun kendi aydınlatma metni olur (ortak metin yalnız liste için,
   madde 6). Üyelikte toplanan veri (şifre özeti, oturum IP'si, ileride halka üyeliği) ve amacı bu ürüne özgü;
   zikir/hatim halkası din verisi sayılabildiği için ayrıca açık rıza gerekir. Bu yüzden metin projeye özel.
4. Şikâyetlere **sahip bakar** (bilgi@bumbagroup.com ve panel).

## Durum
- **Aşama 1a, sunucu (10 Kasım 2026, yapıldı, yayında değil):** `server/` (ayrıntı `server/README.md`): Better
  Auth ile kayıt, doğrulama, giriş (taşıyıcı belirteç), dil, şifre değiştirme ve sıfırlama (sitedeki formdan),
  e-posta değiştirme (önce eski adrese onay), verilerimi indir (`/v1/verilerim`), hesabı silme (şifreyle), saklama
  süreleri, yönetim paneli (tr/en, iki tema), sağlık ucu; 25 test gerçek PostgreSQL'le (`npm test`), CI işi
  `server-test`. Sunucunun ilk yayını ana altyapı oturumundan, sahibin onayıyla.
- **Sırada:** 1b iOS ve Android hesap ekranları; 1c sitedeki şifre/doğrulama sayfaları, CSP, aydınlatma metni,
  gizlilikte "Hesap" bölümü, `/hesap-silme/` (19 dil); sonra ana altyapı isteği.
