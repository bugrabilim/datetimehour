# Saat Tarih üyelik sunucusu

`hesap.time.bumba.tr`. Bumba Genel Standartlar 5.18, madde 7 (üyelik) ve madde 13 (rozetler) burada uygulanır.
Site (`time.bumba.tr`) statik kalır. Bu sunucu şunları verir: web'deki giriş, kayıt ve hesap sayfaları, uygulamaların
API'si, ortak üyelik servisinin ürün uçları ve zamanlanmış görev uçları. Sahibin kararları kökteki `CLAUDE.md`'de
(Üyelik, 12 Kasım 2026).

## Ne yapar
- **Hesap:** e-posta + şifreyle kayıt ve giriş; Google, Apple ya da başka giriş yok. Kayıtta yalnız e-posta ve şifre
  alınır, onay kaydı `{zaman, sürüm 2026-11-11}` tutulur. Kayıt ekranındaki onay cümlesi merkezden alınır
  (standart 5.20; `src/consent.mjs`: `bumbagroup.com/api/uyelik/onay-cumlesi?dil=hepsi`, açılışta ve saatte bir; ulaşılamazsa
  `i18n/account.json` `consent` kopyası, sürüm 2026-11-14; bağlantısı ortak metinler dışına giden metin alınmaz).
  - Şifre kuralı: 8–128 karakter, boşluksuz, rakam, büyük ve küçük harf, sembol. Alanın altında dört satır olarak
    görünür, sunucu da denetler.
  - E-posta doğrulaması zorunludur (bağlantı 24 saat). Şifre sıfırlama bağlantısı 1 saat geçerli, tek kullanımlık.
  - E-posta değişikliği önce mevcut şifre (`/v1/eposta`), sonra eski adrese onay, en son yeni adrese doğrulama.
  - Şifre değişince öbür oturumlar kapanır, bu oturumun kimliği yenilenir.
  - Kimlik doğrulama **Better Auth** ile (tam sürüm, varsayılan tablo adları). Web oturumu 30 gün, httpOnly çerez.
    Uygulama oturumu 90 gün, taşıyıcı belirteç: `Authorization: Bearer …` ve `X-Saat-Istemci: ios|android`.
    Oturumlarda IP saklanmaz. Deneme sınırları veritabanında.
- **Bağlı üyelik:** ortak servis `bumbagroup.com/api/uyelik` (`src/center.mjs`), ürün uçları `/api/bumba-uyelik/*`
  (`src/linked.mjs`).
  - Kayıtta e-posta başka bir Bumba ürününde varsa "Burada da kullanmak ister misiniz?" sorusu gelir (`/v1/uyelik/bagla`).
  - Aynı e-postanın ayrı hesapları hesap sayfası açılınca sormadan birleşir (`/v1/uyelik/durum`); "hesaplarınız
    birleştirildi" e-postası 7 gün geçerli şifre seçimi bağlantısıyla gelir (`/sifre-sec`), seçim yapılana kadar hesap
    sayfasında yalnız seçim görünür. Birleşme yanıtındaki ve `numara-degistir` gövdesindeki `anaUrun` yerel hesaba yazılır
    (`guncelle` ile aynı yol). Şifre seçiminde merkezin gönderdiği `guncelle {sifreOzeti}` kayıtlı özetle aynıysa yazılmaz
    ve oturumlar kalır; özet değiştiyse oturumlar kapanır ve `/v1/uyelik/sifre-sec` `yenidenGiris: true` döner, istemci
    "Seçtiğiniz şifreyle yeniden girin" iletisiyle girişe gider (`/giris?secildi=1`).
  - Hesap sayfasında "Öbür Bumba ürünlerinde üyeliğim" bölümü vardır.
  - Hesabı silme yalnız Saat Tarih'teki hesabı siler (5.21) ve e-postadaki bağlantıyla onaylanır ("Silme onayı", `src/deletion.mjs`):
    istek 7 gün geçerli, bekleyen istek hesap sayfasında iptal edilir; onay sayfası `/silme-onay?k=` (giriş istemez, POST'la siler).
    "Öbür Bumba ürünlerinde üyeliğim"de kapatma yok, her ürünün yanında o ürünün hesap sayfasına bağlantı.
- **Web sayfaları** (`src/web.mjs`, `web/hesap.js`, `web/hesap.css`; 19 dil, iki tema, noindex):
  - `/giris`, `/kayit`, `/sifre?token=`, `/dogrulandi`.
  - `/hesap`: 10 bölüm beş sekmede (`?sekme=profil|guvenlik|tercihler|uyelikler|veri`; Profil 1-2-3, Güvenlik 4-7,
    Tercihler 5-6, Üyelikler 8, Veri ve hesap 9-10), sonra `?sekme=rozetler` (rozetler ve uygulama kayıtları; eski
    `/hesap/rozetler` 301). Telefonda açılır kapanır bölümler, her bölümün kendi Kaydet'i, kaydedilmemiş değişiklik uyarısı.
  - `/sifre-sec?k=`: birleşmeden sonra şifre seçimi (oturum açmaz).
  - `/kutlama`: kutlama e-postalarını girişsiz kapatma.
  - Metinler `i18n/account.json`. Onay cümlesi arayüzün dilindedir; hukuk bağlantıları Türkçe arayüzden Türkçe metne,
    öbür dillerden İngilizce metne (`/en/uyelik-…`, standart 5.23) gider (sahibin kararı, 12 Kasım 2026). Kayıt ve giriş
    sayfasında ayrı bağlantı satırı yok (5.23); hesap silme bilgi sayfası merkezde (`bumbagroup.com/hesap-silme/saat-tarih`).
  - Kendi çerez bandı var (`bumba-cerez`). Umami yalnız istatistik izniyle yüklenir; adresinde anahtar olan sayfalarda
    hiç yüklenmez.
- **Profil:** ad, soyad, takma ad, fotoğraf (istemci JPEG, PNG ya da WebP gönderir, en çok 2 MB; sunucu `sharp` ile yönü düzeltip 512 piksel kare WebP'ye çevirir, ≤200 KB, bütün üst veri düşer), doğum tarihi, cinsiyet,
  telefon, ülke, şehir, meslek, hakkımda (160), sosyal hesaplar (en çok 8). Görünürlük varsayılan kapalı.
- **Yönetici hesabı:** `bilgi@bumbagroup.com` kurulumda açılır (adı "admin", numarasız; şifre "Şifremi unuttum"la);
  kayıt sayfasından açılamaz, ortak servise bildirilmez, panel sayılarına girmez.
- **Kayıtlar ve rozetler** (`src/records.mjs`): uygulama kayıtları hesapta da tutulur (anahtar başına son yazan
  kazanır; iPhone'da iCloud eşitlemesi de sürer). Rozetler yalnız kimlik ve kazanma günü olarak saklanır.
- **E-postalar** Resend ile, 19 dilde (`i18n/mail.json`). Gönderilen türler:
  - hesap e-postaları;
  - askıya alma, yeniden açma ve yasak;
  - bağlı ürünün açılması ya da kapanması;
  - kutlama (`src/celebrate.mjs`: doğum günü, üyelik yıl dönümü, yılbaşı, ülkenin bayramları; yerel saatle 09.00,
    günde bir; Türkiye günleri `data/tr-days.json`; üyeler 500'lük partilerle ve yalnız ad, doğum tarihi, ülke alanlarıyla
    okunur, fotoğraf okunmaz; saat dilimi başına bir biçimleyici, ülke başına bir date-holidays örneği; 5.000 fotoğraflı
    üyede en yüksek RSS 178 MB, önce 269 MB).
- **Saklama** (`src/cleanup.mjs`, günlük `POST /gorev/temizlik`):
  - süresi geçen oturum, doğrulama ve sınır kayıtları silinir;
  - 7 gün içinde doğrulanmayan hesap silinir (numarası varsa merkeze `ayril {neden: "dogrulanmamis"}`, standart 5.28);
  - 7 gün içinde onaylanmayan silme isteği silinir (e-posta gitmez);
  - üye numarası eksik hesaplar numarasını alır (yönetici hesabı hariç);
  - ortak servise ulaşılamayan bildirimlerin zamanı gelenler de denenir. Asıl yeniden deneme `src/relay.mjs`'te, süreç içinde 10 dakikada bir (standart 5.22): `guncelle` ve `ayril` bırakılmaz, 1, 2, 4… dakika arayla, en çok günde bir; aynı üyenin `guncelle`'leri birleşir ve hesabın o anki hâli gider; `ayril` bekleyen `guncelle`'yi siler. Bekleyenler panel özetinde `merkezeBekleyen`.

  Kullanılmayan hesabı ürün silmez (standart 5.28): 3 yıl hiçbir Bumba ürününde girilmeyen hesabı merkez sayar, 30 gün
  önce uyarır ve `hesap-sil` ile siler. Bunun için her başarılı girişte son giriş `user.lastLoginAt`'e yazılır ve merkeze
  `sorgula {eposta, uyeNo}` gider (`src/auth.mjs`, girişi bekletmez); panel `uye`/`uyeler` `sonGiris`'i oradan verir
  (eski hesaplarda en yeni oturumun açılışı), oturumların `sonKullanim`'ı oturumun günlük yenilenmesinden. Hesap silinince
  üyeyi hedef alan halka ve grup bildirimleri de silinir ("Hukuk", silinen hesabın izi; başka işlem kaydı tutmuyoruz).
  Testler `test/retention.test.mjs`.

## Uçlar
| Yol | Ne |
|---|---|
| `/api/auth/*` | Better Auth (kayıt, giriş, çıkış, oturumlar, şifre, e-posta, doğrulama) |
| `/api/bumba-uyelik/<işlem>` | Ortak servisin ürün uçları (`dogrula`, `hesap-getir`, `hesap-ac`, `guncelle`, `hesap-sil`, `durum`, `numara-degistir`; `Bearer $UYELIK_ANAHTARI`) |
| `/v1/uyelik/sorgula`, `/v1/uyelik/bagla` | Kayıt ekranındaki Bumba üyeliği sorusu (5.22: ortak servis cevap vermezse `sorgula` 503, kayıt `CENTER_DOWN` ile açılmaz) |
| `/v1/uyelik/sifirla-iste` | Bağlı üyelik girişinde "Şifremi unuttum" (5.22): merkezin `sifirla-iste`'si; hesap burada varsa kendi sıfırlamamız |
| `/v1/onay-cumlesi?dil=` | Kayıttaki onay cümlesi (merkezden; `{surum, kod, rtl, metin}`; uygulamalar) |
| `/v1/hesap`, `/v1/profil`, `/v1/profil/foto`, `/v1/kutlama` | Hesap ve profil |
| `/v1/eposta` | E-posta değişikliği (`{eposta, sifre}`; Better Auth'un `/change-email` ucu dışarıdan kapalı) |
| `/v1/uyelik/durum`, `sifre-sec`, `urunler`, `ac` | Bağlı üyelik (birleşme, şifre seçimi) |
| `POST /api/bumba-panel/ozet\|uyeler\|uye` | Yönetim paneli (`Bearer $UYELIK_ANAHTARI`, `X-Bumba-Site`; `src/panel.mjs`) |
| `/v1/kayitlar`, `/v1/rozetler`, `/v1/rozetler/sil` | Uygulama kayıtları ve rozetler (silme onaylı) |
| `/v1/verilerim`, `/v1/hesap/sil`, `/v1/silme-istegi`, `/v1/silme-istegi/iptal` | Verilerimi indir, silme isteği (onay e-postayla), bekleyen istek, iptal |
| `/v1/halkalar`, `/v1/halka…` | Ortak zikir ve hatim halkaları (`src/circles.mjs`; `HALKALAR=acik` değilse 404) |
| `/v1/gruplar`, `/v1/grup…`, `/v1/hava-bildirim` | Ortak gruplar (geri sayım, dünya saati, toplantı oylaması, odak, alarm isteği, gözlem) ve anonim hava bildirimi (`src/groups.mjs`; `GRUPLAR=acik` değilse 404) |
| `/topluluk-kurallari` | Halka ve grupların topluluk kuralları (19 dil; uygulamalardaki kurma ve katılma ekranları bağlanır) |
| `/moderasyon?k=` | Yönetici sayfası: bildirim e-postasındaki imzalı bağlantı (30 gün); GET bir şey yapmaz, POST ile gizle, topluluktan uzaklaştır ya da uzaklaştırmayı kaldır (`src/moderation.mjs`) |
| `POST /gorev/temizlik`, `POST /gorev/kutlama` | Zamanlanmış görevler (`Bearer $CRON_SECRET`) |
| `GET /saglik` | Sağlık denetimi |

## Ortam değişkenleri
Gizli değerler koda ve depoya yazılmaz; Coolify'da ana altyapı oturumu girer. Zorunlulardan biri eksikse üyelik kapalı
açılır: sayfalar "üyelik şu an kapalı" der, uçlar 503 döner.

| Değişken | Gerekli | Not |
|---|---|---|
| `DATABASE_URL` | evet | Merkezi PostgreSQL, ayrı veritabanı ve rol |
| `BETTER_AUTH_SECRET` | evet | En az 32 karakter |
| `BETTER_AUTH_URL` | evet | `https://hesap.time.bumba.tr` |
| `RESEND_API_KEY` | evet | E-posta |
| `CRON_SECRET` | evet | En az 32 karakter |
| `UYELIK_ANAHTARI` | evet | Ortak üyelik servisinin site anahtarı (`saat-tarih`) |
| `HALKALAR` | hayır | `acik`: halkalar açılır (açık rıza metni ve uygulama yayına hazır olunca, sahibin onayıyla) |
| `GRUPLAR` | hayır | `acik`: ortak gruplar ve hava bildirimi açılır (sahibin onayıyla) |
| `NODE_ENV` | canlıda `production` | Güvenli çerezler |

## Komutlar
```sh
npm ci                                # bağımlılıklar kilit dosyasından
npm test                              # gerçek PostgreSQL ile (TEST_DATABASE_URL; her dosya kendi veritabanını açar ve siler)
npm start                             # :8080 (tabloları açılışta kurar; yalnız ekleyici değişiklik)
node scripts/gen-data.mjs [--check]   # data/tr-days.json ve data/web-texts.json (sitenin metinlerinden)
```

## Yayın
Ayrı Coolify uygulaması (`saat-hesap`): temel dizin `server/`, bu klasördeki `Dockerfile`, port 8080, sağlık `/saglik`,
izleme yolları `server/**`. Zamanlanmış görevler günde bir: `/gorev/temizlik` ve `/gorev/kutlama`.

İlk yayını ana altyapı oturumu yapar. Sonraki her yayın sahibin onayıyla olur; main'e birleştirme yayındır.

Bağımlılıklar en son kararlı sürümde ve tam sürümle sabitlenir.
