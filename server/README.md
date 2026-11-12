# Saat Tarih üyelik sunucusu

`hesap.time.bumba.tr`. Bumba Genel Standartlar 5.15, madde 7 (üyelik) ve madde 13 (rozetler) burada uygulanır.
Site (`time.bumba.tr`) statik kalır. Bu sunucu şunları verir: web'deki giriş, kayıt ve hesap sayfaları, uygulamaların
API'si, ortak üyelik servisinin ürün uçları ve zamanlanmış görev uçları. Sahibin kararları kökteki `CLAUDE.md`'de
(Üyelik, 12 Kasım 2026).

## Ne yapar
- **Hesap:** e-posta + şifreyle kayıt ve giriş; Google, Apple ya da başka giriş yok. Kayıtta yalnız e-posta ve şifre
  alınır, onay kaydı `{zaman, sürüm 2026-11-11}` tutulur.
  - Şifre kuralı: 8–128 karakter, boşluksuz, rakam, büyük ve küçük harf, sembol. Alanın altında dört satır olarak
    görünür, sunucu da denetler.
  - E-posta doğrulaması zorunludur (bağlantı 24 saat). Şifre sıfırlama bağlantısı 1 saat geçerli, tek kullanımlık.
  - E-posta değişikliği önce eski adrese onay, sonra yeni adrese doğrulama.
  - Kimlik doğrulama **Better Auth** ile (tam sürüm, varsayılan tablo adları). Web oturumu 30 gün, httpOnly çerez.
    Uygulama oturumu 90 gün, taşıyıcı belirteç: `Authorization: Bearer …` ve `X-Saat-Istemci: ios|android`.
    Oturumlarda IP saklanmaz. Deneme sınırları veritabanında.
- **Bağlı üyelik:** ortak servis `bumbagroup.com/api/uyelik` (`src/center.mjs`), ürün uçları `/api/bumba-uyelik/*`
  (`src/linked.mjs`).
  - Kayıtta e-posta başka bir Bumba ürününde varsa "Burada da kullanmak ister misiniz?" sorusu gelir (`/v1/uyelik/bagla`).
  - Girişten sonra ayrı hesap varsa birleştirme sorulur.
  - Hesap sayfasında "Öbür Bumba ürünlerinde üyeliğim" bölümü vardır.
  - Hesabı silme iki seçenekli: yalnız Saat Tarih'ten ya da bütün Bumba ürünlerinden.
- **Web sayfaları** (`src/web.mjs`, `web/hesap.js`, `web/hesap.css`; 19 dil, iki tema, noindex):
  - `/giris`, `/kayit`, `/sifre?token=`, `/dogrulandi`.
  - `/hesap`: 10 bölüm (profil, görünürlük, e-posta, şifre, dil-tema-saat dilimi, e-postalar, oturumlar, üyelik ve öbür
    ürünler, verilerimi indir, hesabı sil).
  - `/hesap/rozetler`: rozetler ve uygulama kayıtlarının özeti.
  - `/kutlama`: kutlama e-postalarını girişsiz kapatma.
  - Metinler `i18n/account.json`. Onay cümlesi arayüzün dilindedir; hukuk bağlantıları Türkçe arayüzden Türkçe metne,
    öbür dillerden `?dil=en` metnine gider (sahibin kararı, 12 Kasım 2026).
  - Kendi çerez bandı var (`bumba-cerez`). Umami yalnız istatistik izniyle yüklenir; adresinde anahtar olan sayfalarda
    hiç yüklenmez.
- **Profil:** ad, soyad, takma ad, fotoğraf (512 piksel WebP, ≤200 KB, EXIF cihazda silinir), doğum tarihi, cinsiyet,
  telefon, ülke, şehir, meslek, hakkımda (160), sosyal hesaplar (en çok 8). Görünürlük varsayılan kapalı.
- **Kayıtlar ve rozetler** (`src/records.mjs`): uygulama kayıtları hesapta da tutulur (anahtar başına son yazan
  kazanır; iPhone'da iCloud eşitlemesi de sürer). Rozetler yalnız kimlik ve kazanma günü olarak saklanır.
- **E-postalar** Resend ile, 19 dilde (`i18n/mail.json`). Gönderilen türler:
  - hesap e-postaları;
  - askıya alma, yeniden açma ve yasak;
  - bağlı ürünün açılması ya da kapanması;
  - kutlama (`src/celebrate.mjs`: doğum günü, üyelik yıl dönümü, yılbaşı, ülkenin bayramları; yerel saatle 09.00,
    günde bir; Türkiye günleri `data/tr-days.json`).
- **Saklama** (`src/cleanup.mjs`, günlük `POST /gorev/temizlik`):
  - süresi geçen oturum, doğrulama ve sınır kayıtları silinir;
  - 7 gün içinde doğrulanmayan hesap silinir;
  - üye numarası eksik hesaplar numarasını alır;
  - ortak servise ulaşılamayan bildirimler yeniden denenir.

  Kullanılmayan hesap silinmez.

## Uçlar
| Yol | Ne |
|---|---|
| `/api/auth/*` | Better Auth (kayıt, giriş, çıkış, oturumlar, şifre, e-posta, doğrulama) |
| `/api/bumba-uyelik/<işlem>` | Ortak servisin ürün uçları (`dogrula`, `hesap-getir`, `hesap-ac`, `guncelle`, `hesap-sil`, `durum`, `numara-degistir`; `Bearer $UYELIK_ANAHTARI`) |
| `/v1/uyelik/sorgula`, `/v1/uyelik/bagla` | Kayıt ekranındaki Bumba üyeliği sorusu |
| `/v1/hesap`, `/v1/profil`, `/v1/profil/foto`, `/v1/kutlama` | Hesap ve profil |
| `/v1/uyelik/durum`, `birlestir`, `urunler`, `ac`, `kapat` | Bağlı üyelik |
| `/v1/kayitlar`, `/v1/rozetler`, `/v1/rozetler/sil` | Uygulama kayıtları ve rozetler |
| `/v1/verilerim`, `/v1/hesap/sil` | Verilerimi indir, hesabı sil (`kapsam: urun\|hepsi`) |
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
| `ADMIN_EMAIL` | hayır | Varsayılan bilgi@bumbagroup.com |
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
