# Google Play özel mağaza sayfaları (custom store listings)

Hazırlanma: 9 Kasım 2026 (sahibin isteği: "tüm ülkelere göre mağaza adı farklı olsun"). Strateji ve tam ülke tablosu:
`docs/magaza-adlari-2026-11.md`. Bu klasördeki metinler Play Console'a **elle** girilir: fastlane supply ve Play API yalnız
varsayılan mağaza sayfasını ve onun çevirilerini yükleyebilir (`mobile/android/fastlane/metadata/android/<kod>/`, nötr sayfa).

## Sayfalar

| Sayfa | Ne gösterir | Hedefleme | Metinler |
|---|---|---|---|
| Varsayılan (nötr) | Saat, alarm, widget, hava, takvim önde; namaz vakitleri açıklamanın ilerisinde | Aşağıda adı geçmeyen bütün ülkeler | `mobile/android/fastlane/metadata/android/` (bu klasörde değil) |
| `muslim` | Namaz vakitleri, ezan ve kıble önde; ad "…: Namaz Vakitleri" | Müslümanların çoğunlukta olduğu ülkeler (aşağıda) | `muslim/<kod>/` |
| `secular` | Ad nötr ("Saat Tarix: Təqvim və Hava" gibi), kısa açıklamada namaz vakitleri geçer, açıklamada vakitler sonda | Müslüman çoğunluklu ama laik eski Sovyet ve Balkan ülkeleri (aşağıda) | `secular/<kod>/` |
| `prayer-search` | `muslim` ile aynı metinler ve görseller | Arama kelimeleri (aşağıda): hangi ülkede olursa olsun "prayer times", "namaz", "jadwal sholat" … arayan | `muslim/<kod>/` (ayrı dosya yok) |

Her dilde üç dosya: `title.txt` (en çok 30 karakter), `short_description.txt` (80), `full_description.txt` (4000).
Çeviriler (Play dil kodlarıyla):

- `muslim`: ar, tr-TR, az-AZ, id, ur, fa, bn-BD, ms, ms-MY, fr-FR, ru-RU, en-US, en-GB, pt-BR (`ms` ve `ms-MY` aynı metin;
  `en-GB` yalnız "analogue", "maths" yazımıyla ayrılır; `pt-BR` Gine-Bissau içindir)
- `secular`: az-AZ, ru-RU, tr-TR, en-US

Denetim: `node mobile/android/store-listings/check.mjs` (sınırlar, emoji, BÜYÜK HARF, "free/best/new/ücretsiz/yeni…"
gibi Play'in yasakladığı fiyat, sıralama ve tanıtım sözcükleri, Latin rakam). Metin değişince yeniden çalıştırılır.

İçerik kuralları (Android'e göre doğru olsun diye): hava Open-Meteo'dan ve 14 günlük; Wear OS uygulaması (kutucuk,
komplikasyonlar), Android TV, ekran koruyucu, yüzen saat, Hızlı Ayarlar kutucukları, vakitte titreşim/Rahatsız Etmeyin var;
Apple Watch, WeatherKit, AlarmKit yok; **konuma bağlı hatırlatıcılar ilk Play sürümünde kapalı olduğu için hiçbir metinde
geçmez** (`GeoAlarms.ENABLED = false`, `PLAY_CONSOLE.md` 7). Bengalce ve Malayca metinler uygulamanın arayüzünün bu dillerde
olmadığını açıkça yazar. Saat kadranı (Wear OS watch face) ayrı paket olduğu için bu sayfalarda anılmaz.

## Hedefleme

Kaynak: Pew 2020 Müslüman oranları ve mağaza bilgisi (`countries.csv`, oturum çalışma dosyası; tablo
`docs/magaza-adlari-2026-11.md`'de). Kural: Pew'e göre Müslümanların %50'den fazla olduğu ve Google Play'in açık olduğu
ülkeler `muslim`; bunlardan laik eski Sovyet ve Balkan ülkeleri `secular`. Bir ülke yalnız bir özel sayfada olabilir.

**`muslim` (40 ülke):** AE, BD, BF, BH, BN, DJ, DZ, EG, GM, GN, GW, ID, IQ, JO, KM, KW, LB, LY, MA, ML, MR, MV, MY, NE, NG,
OM, PK, QA, SA, SD, SL, SN, SO, TD, TN, TR; Play Console'un ülke listesinde varsa (mağaza durumu doğrulanmadı): AF, ER, PS, YE.
Mayotte (YT) Play'de ayrı ülke olarak görünürse buraya eklenir.

**`secular` (9 ülke):** AL, AZ, BA, KG, KZ, TJ, TM, UZ; listede varsa XK (Kosova).

**Sınırda olanlar** (Pew %50–70 ya da başka kaynaklarda yarı yarıya; sahip isterse varsayılana alınır): Nijerya (NG),
Lübnan (LB), Çad (TD), Gine-Bissau (GW), Eritre (ER) `muslim`'de; Bosna-Hersek (BA), Arnavutluk (AL), Kazakistan (KZ)
`secular`'da.

**Hedeflenmeyenler:** İran (IR; Play resmî olarak yok), Suriye (SY; mağaza durumu belirsiz, açılırsa `muslim`), Batı Sahra
(EH; Fas üzerinden), Çin, Küba, Kuzey Kore. Müslümanların büyük azınlık olduğu ülkeler (Fildişi Sahili, Etiyopya, Kuzey
Makedonya, Benin, Tanzanya, Kamerun, Kıbrıs, Karadağ; Hindistan, Rusya, Birleşik Krallık…) varsayılan sayfada kalır;
oradaki kullanıcı namaz vakti arayınca `prayer-search` sayfasını görür.

**`prayer-search` arama kelimeleri** (Play Console'un izin verdiği sayı kadar, bu sırayla):
prayer times, namaz vakitleri, jadwal sholat, waktu solat, مواقيت الصلاة, نماز کے اوقات, اوقات شرعی, নামাজের সময়,
heures de prière, время намаза, namaz vaxtları, horários de oração, adhan, azan, ezan, qibla, kıble, kiblat, salah,
salat, namaz, imsakiye, ramadan timetable, jadwal imsakiyah. Ülke hedefli sayfa ile arama kelimesi hedefli sayfa aynı
kullanıcıya denk gelirse Play'in hangisini seçtiği belgelerde açık değil; ilk yayından sonra Play Console önizlemesiyle
bakılır.

## Dil seçimi nasıl çalışır

Hangi sayfanın görüneceğini kullanıcının Google Play **ülkesi** seçer, sayfanın içindeki çeviriyi cihazın **dili**. Özel
sayfalar makineyle çevrilmez: çevirisi olmayan dildeki cihaz, özel sayfanın varsayılan dilini görür. Bu yüzden özel
sayfanın varsayılan dili **en-US** olmalı (Arnavutluk, Kosova, Bosna-Hersek, Gambiya, Sierra Leone, Maldivler, Nijerya,
Kazakça/Özbekçe/Tacikçe cihazlar İngilizce görür). Play Console özel sayfa için ayrı varsayılan dil seçtirmiyorsa uygulamanın
varsayılan dili geçerlidir; o zaman bu not sahibe bildirilir.

## Play Console'da elle yapılacaklar

Ancak uygulama Play'de yayımlandıktan ve hedeflenecek ülkelerde yayına açıldıktan sonra (özel sayfa için yayımlanmış uygulama
ve varsayılan mağaza sayfası şart; ülke, uygulama orada yayındayken hedeflenebilir). Her değişiklik yayın sayılır ve
sahibin onayıyla yapılır; mağaza sayfası değişiklikleri Google incelemesinden geçer.

1. Play Console › Grow users › Store presence › Store listings › **Create custom store listing**.
2. Ad (yalnız konsolda görünür): `muslim`. Adres parametresi (unique URL) `listing=muslim`.
3. Hedefleme: **Countries/regions** › yukarıdaki `muslim` listesi.
4. Varsayılan dil en-US (seçilebiliyorsa); `muslim/en-US/` dosyalarını yapıştır. Sonra **Manage translations** ile
   klasördeki her dil kodu için üç dosyayı yapıştır (ar, tr-TR, az-AZ, id, ur, fa, bn-BD, ms, ms-MY, fr-FR, ru-RU, en-GB, pt-BR).
5. Simge varsayılanla aynı. Ekran görüntüleri: vakitler ikinci sırada (App Store'daki Müslüman vitrini sırası: saat,
   vakitler, takvim, dünya, araçlar, imsakiye, gece modu; `docs/magaza-adlari-2026-11.md` › Görseller). Özellik grafiği
   varsayılanla aynı kalabilir.
6. Kaydet, incelemeye gönder.
7. Aynısını `secular` için yap (hedefleme: `secular` listesi; çeviriler az-AZ, ru-RU, tr-TR, en-US; görseller nötr sıra).
8. `prayer-search`: Create custom store listing › hedefleme **Search keywords** › yukarıdaki kelimeler; metin ve görseller
   `muslim` ile aynı.
9. Yayından sonra Play Console › Store listing experiments ile (isteğe bağlı) ad ve görseller denenir; sonuç bu dosyaya yazılır.
