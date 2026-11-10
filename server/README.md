# Saat Tarih hesap sunucusu

Uygulamalardaki üyeliğin sunucusu (`hesap.time.bumba.tr`). Site statik kalır; bu sunucu yalnız API ve yönetim
paneli sunar. Plan ve sahibin kararları: `docs/uyelik-plani-2026-11.md`.

## Ne yapar
- **Hesap (standart madde 7):** e-posta + şifre ile kayıt, e-posta doğrulama, giriş, çıkış, görünen ad ve arayüz
  dili, şifre değiştirme, şifre sıfırlama, e-posta değiştirme (önce eski adrese onay, sonra yeni adrese doğrulama),
  verilerimi indir, hesabı silme. Google, Apple ya da başka giriş yok (10 Kasım 2026, sahibin kararı).
- Kimlik doğrulama **Better Auth** ile (şifre özeti scrypt, oturumlar, giriş denemesi sınırı). Uygulamalar
  taşıyıcı belirteç (`Authorization: Bearer …`) kullanır; panel httpOnly oturum çereziyle.
- E-postalar Resend ile, 19 dilde (`i18n/mail.json`). E-postadaki bağlantılar sitedeki statik sayfalarda açılır:
  şifre sıfırlama formu `time.bumba.tr/hesap/sifre/` (diğer dillerde `/<dil>/account/password/`), doğrulama sonucu
  `/hesap/dogrulandi/` (`/<dil>/account/verified/`).
- **Saklama süreleri** (`src/cleanup.mjs`, 6 saatte bir): süresi geçen oturum/doğrulama/sınır kayıtları; 7 gün
  doğrulanmayan hesap; 2 yıl kullanılmayan hesaba 30 gün önce uyarı, sonra silme ve haber.
- **Yönetim paneli (madde 10):** `/panel`, yalnız yönetici rolü (`ADMIN_EMAIL`, varsayılan bilgi@bumbagroup.com;
  hesap uygulamadan açılıp doğrulanınca yönetici olur). İstek, yanıt süresi, hata oranı, veritabanı boyutu, üye
  sayıları, Umami özeti, servis durumu, son yayın; yalnız toplu sayılar. tr/en, iki tema, noindex.

## Uçlar
| Yol | Ne |
|---|---|
| `/api/auth/*` | Better Auth: `sign-up/email`, `sign-in/email`, `sign-out`, `get-session`, `update-user`, `change-password`, `request-password-reset`, `reset-password`, `verify-email`, `send-verification-email`, `change-email`, `delete-user` |
| `POST /sifre` | Sitedeki şifre sıfırlama formu (betiksiz): `token`, `sifre`, `sifre2`, `dil` → siteye `?durum=tamam` ya da `?durum=hata&neden=eslesme\|uzunluk\|baglanti` |
| `GET /v1/verilerim` | Oturumdaki hesabın bilgileri (JSON indirme; KVKK md. 11) |
| `GET /saglik` | Sağlık denetimi (veritabanı) |
| `/panel`, `/panel/giris`, `/panel/cikis` | Yönetim paneli |

Uygulamalar `sign-up/email` gövdesinde `lang` (arayüz dili) gönderir; e-postalar o dilde gider. Bilinmeyen dil
İngilizce, Android'in `in`/`tl` kodları `id`/`fil` sayılır.

## Ortam değişkenleri
Gizli değerler koda ve depoya yazılmaz; Coolify'da ana altyapı oturumu girer.

| Değişken | Gerekli | Not |
|---|---|---|
| `DATABASE_URL` | evet | Merkezi PostgreSQL 18, ayrı veritabanı ve rol |
| `BETTER_AUTH_SECRET` | evet | En az 32 karakter, rastgele |
| `RESEND_API_KEY` | canlıda evet | `NODE_ENV=production` iken yoksa sunucu başlamaz |
| `NODE_ENV` | canlıda `production` | Güvenli çerezler |
| `UMAMI_SHARE_ID` | hayır | Sitenin Umami paylaşım kimliği (panel); yoksa panelde "bu ortamda ölçülemiyor" |
| `PUBLIC_URL`, `SITE_URL`, `MAIL_FROM`, `ADMIN_EMAIL`, `UMAMI_URL`, `PORT` | hayır | Varsayılanlar `src/config.mjs` |
| `MAIL_DRIVER` | hayır | `resend` (anahtar varsa), `log` (yerel), `memory` (testler) |

## Komutlar
```sh
npm ci            # bağımlılıklar kilit dosyasından
npm test          # gerçek PostgreSQL ile akış testleri (TEST_DATABASE_URL; her dosya kendi veritabanını açar ve siler)
npm run migrate   # tabloları kurar (sunucu açılışta da kurar; yalnız ekleyici değişiklik)
npm start         # :8080
```
Yerel test veritabanı yoksa: `TEST_DATABASE_URL=postgres://postgres@127.0.0.1:5432/postgres` (CI'da
`server-test` işi `postgres:18-alpine` hizmetiyle çalışır).

## Yayın
Ayrı Coolify uygulaması (`saat-hesap`): temel dizin `server/`, bu klasördeki `Dockerfile`, port 8080, sağlık
`/saglik`, izleme yolları `server/**`. İlk yayın ana altyapı oturumundan, sonraki her yayın sahibin onayıyla
(main'e birleştirme yayındır). Bağımlılık sürümleri sabittir; güncellerken en az 14 günlük sürüm seçilir.
