# Kur'an ve dua verisi

`node mobile/shared/gen-religion.mjs` bu klasördeki kaynaklardan uygulamalara dört dosya yazar
(`mobile/ios/App/Resources` ve `mobile/android/app/src/main/assets`): `quran-ar.txt`, `quran-en.txt`,
`quran-meta.json`, `duas.json`. CI (`.github/workflows/mobile.yml`) `--check` ile güncel mi diye denetler.

## Kaynaklar ve lisanslar (30 Ekim 2026)

| Dosya | Kaynak | Lisans / durum |
|---|---|---|
| `source/quran-simple.txt` | Tanzil Projesi, Quran Text Simple 1.1 (tanzil.net/download) | CC BY 3.0. Değiştirilmeden kopyalanabilir; kaynak (Tanzil Projesi) açıkça yazılmalı ve tanzil.net'e bağlantı verilmeli; telif bloğu türetilen dosyalarda korunmalı. `quran-ar.txt` sonunda blok aynen durur; uygulamalarda Kur'an listesinin ve her surenin altında atıf + tanzil.net bağlantısı var. |
| `source/quran-data.js` | Tanzil Projesi, sure ve cüz bilgisi | CC BY 3.0 (aynı atıf). |
| `source/en.pickthall.txt` | M. M. Pickthall, *The Meaning of the Glorious Koran* (1930), Tanzil'deki dijital metin | Çevirmen 1936'da vefat etti: eser kamu malı. Tanzil'in çeviriler için "ticari olmayan kullanım" şartı vardır; Saat Tarih ticari değildir (30 Ekim 2026, sahibin kararı). Dosyanın başlık bloğu korunur. |
| `surahs-tr.json` | Türkçe sure adları (Diyanet yazımı) | Ad listesi, telif konusu değil. |
| `duas.json` | Dualar | Kur'an'dan olanlar (`quran`: ayet ve başlangıç sözcüğü) Tanzil metninden birebir alınır; diğerlerinin Arapçası yaygın metinlerden, okunuş ve anlamlar bu proje için yazıldı. |

**Türkçe meal yok.** Kamu malı ve özgün haliyle dijital bir Türkçe meal bulunamadı: Tanzil'deki Elmalılı metni
(`tr.yazir`) sonradan sadeleştirilmiş bir baskıdır, Diyanet meali Diyanet'in hakkıdır. Sahibin kararı (30 Ekim 2026):
lisansı doğrulanamazsa yalnız Arapça metin + Diyanet'in meal sitesine bağlantı. Uygulamalar Türkçe ve Azerice
arayüzde her ayet için `https://kuran.diyanet.gov.tr/kuran?sure=S&ayet=A&mode=kuran-meal` bağlantısını verir.
Almanca arayüzde İngilizce meal gösterilir (Henning çevirisinin yalnız taranmış baskıları var).

## Güncelleme

Tanzil metni güncellenirse (tanzil.net/updates) `source/` altındaki dosya değiştirilir ve `gen-religion.mjs`
yeniden çalıştırılır. Betik ayet sayısını (6236), sure sırasını, sure başlarını ve 30 cüzü denetler; Kur'an'dan
alınan duaların sözcük sırası metinde yoksa durur.
