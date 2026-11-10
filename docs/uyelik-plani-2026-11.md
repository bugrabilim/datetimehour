# Üyelik planı (taslak, 10 Kasım 2026)

Sahibin kararları: üyelikle gelecek işler **ücretsizdir, yalnız üyelik ister** (10 Kasım 2026); bağış ayrı
değerlendirilir; **sitede üyelik yok** (#102, standart da statik sitede yasaklar). Üyelik yalnız iOS ve Android
uygulamalarında. Bu belge plan ve sahibin sorularıdır; kod henüz yazılmadı.

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
  küçük HTTP çatısı (Hono) ve **Better Auth** (MIT; e-posta kodu, Apple ile giriş, mobil için taşıyıcı belirteç,
  giriş denemesi sınırı, yönetici rolü hazır; şifre ve oturum mantığı elle yazılmaz, standart madde 7).
  Adres önerisi `hesap.time.bumba.tr` (`time.bumba.tr/api/v1/` zaten açık veri olduğu için ayrı alt alan).
  Site statik kalır; sitenin Coolify izleme yolları `server/**`'u kapsamaz.
- **Veritabanı:** merkezi PostgreSQL 18, ayrı veritabanı ve rol, `DATABASE_URL` ortamda; gece R2 yedeği. Yalnız
  ekleyici değişiklik (yeni tablo/sütun).
- **E-posta:** giriş kodu ve e-posta değiştirme onayı Resend ile `bilgi@bumbagroup.com`'dan, gönderen adı
  "Saat Tarih", 19 dilde. Tanıtım e-postası yok (ortak liste ayrı; "üye olmak listeye katılmak değildir").
- **Giriş:** şifresiz: e-postaya 6 haneli kod (10 dk geçerli, 5 deneme) ve iPhone'da "Apple ile giriş". Google ile
  giriş Play hesabı açılınca. Şifre olmadığı için şifre sıfırlama da yok (aşağıda istisna talebi).
- **Mobil:** belirteç iOS'ta Anahtar Zinciri'nde, Android'de Keystore ile şifreli; hesap ekranı Ayarlar › Hesap
  (görünen ad, e-posta, dil ve tema [uygulamadaki ayarlar], e-posta listesi formu, verilerimi indir, çıkış, hesabı
  sil). Hesap yoksa uygulamanın bugünkü her özelliği aynen çalışır; giriş yalnız grup özelliklerinde istenir.
- **Davet bağlantısı:** `https://time.bumba.tr/davet/#KOD` (statik, noindex sayfa; uygulama kuruluysa evrensel
  bağlantıyla uygulamada açılır, değilse mağaza bağlantıları). Sitede `/.well-known/apple-app-site-association` ve
  `assetlinks.json`. Kod adresin `#` kısmında olduğu için sunucuya gitmez.
- **Anlık bildirim (APNs/FCM):** aşama 3'e kadar yok; grup verisi uygulama açılınca ve arka plan yenilemesinde
  gelir. Uyanma grupları için "Ayşe uyandı" bildirimi APNs anahtarı ister (sahipte).
- **Yönetim paneli (madde 10):** `hesap.time.bumba.tr/panel`, yalnız yönetici (bilgi@bumbagroup.com, e-posta
  koduyla). İstek sayısı, yanıt süresi, hata oranı, veritabanı boyutu, üye sayısı ve yeni kayıtlar, grup sayıları
  (toplu), sitenin Umami özeti (anahtar sunucuda), servis durumu, durum.bumba.tr bağlantısı; iki dil, iki tema,
  noindex. Ölçülemeyen satırda "bu ortamda ölçülemiyor" (ör. sitede arama sözcüğü tutulmuyor).

## Veri (en az)
- Hesap: e-posta, görünen ad (takma ad olabilir), arayüz dili, kayıt günü, son giriş günü. Konum, cihaz bilgisi,
  reklam kimliği yok; Umami ve istatistik uygulamada yok.
- Halka: ad, kurucu, üyeler (görünen adlarıyla), zikir hedefi ve toplamlar, hatim cüz durumları. Kişinin zikir
  sayısı halkadaki payı kadar tutulur; günlük dökümü yok.
- Uyanma grubu: üyenin o sabahki uyanma saati (yerel, dakika), 30 gün tutulur, sonra silinir.
- Cami notu: not metni, caminin kimliği (OSM ya da Apple Haritalar) ve yuvarlatılmış konumu; yazarın görünen adı
  isteğe bağlı.
- **Silme:** hesabı silince her şey hemen silinir (yedeklerde en geç yedek döngüsü kadar kalır, gizlilikte
  yazar). 24 ay giriş yapılmayan hesap bir uyarı e-postasından 30 gün sonra silinir.
- **Din verisi:** zikir/hatim halkasına katılmak KVKK'da özel nitelikli kişisel veri (din ve inanç) sayılabilir.
  Önerim: halkaya ilk katılışta ayrı ve açık bir onay ("Bu halkaya katılmanız inancınızla ilgili bilgi
  sayılabilir; …"), takma ad kullanılabilmesi, halka listelerinin herkese açık olmaması, sunucuda şifreli disk.
  Aydınlatma ve açık rıza metinleri hukuk gözden geçirmesi ister (aşağıda soru 3).

## Mağaza ve yasal
- **Apple:** uygulama içinde hesabı silme (5.1.1(v)); kullanıcı içeriği için süzgeç, şikâyet, engelleme ve
  iletişim (1.2): grup adı, görünen ad ve cami notu bunun içinde; Apple ile giriş zorunlu değil (yalnız kendi
  hesabımız ve Apple var) ama sunulur. Gizlilik etiketleri değişir (e-posta, kullanıcı kimliği, diğer kullanıcı
  içeriği; kimlikle bağlantılı, izleme yok). Yaş derecelendirme soruları (kullanıcı içeriği) yeniden yanıtlanır,
  derece değişebilir. `APP_REVIEW_NOTES.md`'ye inceleme için deneme hesabı yolu (e-posta kodu) yazılır.
- **Google Play:** hesabı silme için web adresi şart: sitede statik, noindex `/hesap-silme/` sayfası (uygulamadan
  nasıl silinir; uygulamaya erişemeyen hesabın e-postasından bilgi@bumbagroup.com'a yazar). Veri güvenliği formu
  güncellenir.
- **KVKK:** veri sorumlusu Bumba Teknoloji Ltd. Şti.; gizlilik sayfasına 19 dilde "Hesap" bölümü; e-posta
  gönderimi (Resend) ve sunucunun yeri nedeniyle yurt dışına aktarım cümlesi hukukçuya gösterilir.
- **Moderasyon:** şikâyet edilen içerik panelde bir kuyruğa düşer; üç ayrı şikâyette içerik kendiliğinden gizlenir.
  Madde 10 panelde tek kullanıcının içeriğini göstermeyi yasaklar; şikâyet edilen içeriği (yazarın kimliği
  olmadan) görmek için aşama 4'te istisna talebi gerekir.

## İş sırası
1. **Hesap** (sunucu, Better Auth, e-posta kodu, Apple ile giriş, panel, hesap ekranları, 19 dil, gizlilik,
   testler). Sunucunun ilk yayını standart gereği **ana altyapı oturumundan**, sahibin oradaki onayıyla.
2. **Ortak zikir ve hatim** (davet bağlantısı, evrensel bağlantılar, şikâyet/engelleme).
3. **Uyanma grupları** (APNs anahtarı gelirse anlık bildirim).
4. **Cami notları** (moderasyon kuyruğu, istisna talebi).

Her aşama ayrı PR; uygulama yayını (TestFlight, App Store 1.x) her seferinde sahibin onayıyla. Android, Play
hesabı açılana kadar yalnız kodda.

## Sahipte olanlar (aşama 1 için)
- Apple Developer › `tr.bumba.time`: **Sign in with Apple** ve **Associated Domains** işaretlenir; Apple ile
  giriş anahtarı (.p8) oluşturulup Coolify'a sahip tarafından girilir.
- Resend anahtarı ve rastgele gizliler: ana altyapı oturumu (standart "Ana altyapı istekleri").
- Aşama 3'te APNs anahtarı; Play hesabı açılınca Google girişi için OAuth istemcisi.

## Ana altyapı isteği (kod hazır olunca gönderilecek)
- Yeni sunuculu uygulama: ad `saat-hesap`, depo `bugrabilim/datetimehour`, dal `main`, temel klasör `server/`,
  `server/Dockerfile`, port 8080, sağlık `/saglik`, izleme yolları `server/**`, adres `hesap.time.bumba.tr`
  (DNS + HTTPS).
- Merkezi PostgreSQL 18'de veritabanı ve rol `saat_hesap`, `DATABASE_URL`; gece R2 yedeği.
- Ortam: oturum sırrı (ana oturum üretir), `RESEND_API_KEY` (ana oturum), gönderen bilgi@bumbagroup.com /
  "Saat Tarih", Apple ile giriş değişkenleri (sahip girer), Umami okuma bilgisi (panel için, sunucuda kalır).
- Node derleme sürümü 22.23.4; durum.bumba.tr izlemesine ekleme (ilk yayından sonra).
- Site (`saat`): değişiklik yok; yalnız `/davet/`, `/hesap-silme/` ve `.well-known` dosyaları normal site
  yayınıyla gelir.

## Sahibin soruları
1. **Sıra:** hesap → ortak zikir ve hatim → uyanma grupları → cami notları. Uygun mu?
2. **İstisna talebi (madde 7, şifre sıfırlama):** giriş şifresiz (e-posta kodu + iPhone'da Apple ile giriş);
   şifre tutulmadığı için şifre sıfırlama yok, e-posta doğrulama kodla zaten yapılır. Süre: kalıcı. Onay?
3. **Hukuk:** zikir/hatim halkası din verisi sayılabilir; aydınlatma ve açık rıza metinlerini (ve yurt dışına
   aktarım cümlesini) bir hukukçu görecek mi, yoksa metinleri ben yazayım, yayından önce siz mi bakarsınız?
4. **Şikâyetler:** grup adı, görünen ad ve cami notu şikâyetleri bilgi@bumbagroup.com'a ve panele gelir; siz mi
   bakarsınız? (Apple, şikâyetlere zamanında bakılmasını ister.)
