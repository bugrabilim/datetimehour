# App Store özel ürün sayfaları (taslak, 10 Kasım 2026)

Kaynak: `docs/arastirma-musluman-olmayanlar-2026-11.md` (öneri 3, "kitlelere göre ürün sayfaları"). Bu belge yalnız metin ve
görsel sırasıdır; App Store Connect'te sayfa açmak **yayın sayılır** ve sahibin onayıyla, 1.1 gönderiminden sonra yapılır
(1.0 incelemedeyken yeni sayfa açılmaz). Kod gerekmez: özel ürün sayfası aynı uygulamanın başka görsel sırası ve tanıtım
metnidir.

## Uygulamadan önce denetlenecekler

- **Anahtar kelime atama:** üçüncü taraf kaynaklara göre Temmuz 2025'ten beri bir özel ürün sayfasına anahtar kelime
  atanabiliyor (o kelimeyle arayan varsayılan sayfa yerine o sayfayı görür). Sayfa açılmadan önce Apple'ın kendi belgesinden
  ("Custom product pages", App Store Connect Help) doğrulanmalı; doğrulanamazsa sayfalar yalnız bağlantıyla (ör. sitedeki
  sayfadan ya da reklamdan) kullanılır.
- **Sınırlar:** en çok 70 sayfa (Ekim 2025'ten beri, üçüncü taraf kaynak); her sayfada tanıtım metni ≤170 karakter, görseller
  mevcut ekran görüntülerinden seçilir. Ad, alt başlık ve açıklama varsayılan sayfanınkidir, değiştirilemez.
- **Görseller:** `mobile/ios/scripts/store-screenshots.sh` bugün 7 telefon görüntüsü üretir (müslüman vitrini: `01-saat`,
  `02-vakitler`, `03-takvim`, `04-dunya`, `05-araclar`, `06-imsakiye`, `07-gece`; nötr vitrin: `01-saat`, `02-gece`,
  `03-takvim`, `04-dunya`, `05-uyku`, `06-araclar`, `07-vakitler`). Aşağıdaki sayfalardan bazıları için yeni görüntü gerekir
  (ör. widget galerisi, panchang); betiğe `shoot … -openScreen <ad>` satırı eklenir, `captions.json`'a başlık yazılır.
- **Ölçüm:** App Store Connect her sayfanın gösterim ve indirme oranını ayrı verir; uygulamaya takip kodu eklenmez.

## Sayfalar

Metinler önce İngilizce (en-US, en-GB ve diğer İngilizce sürümler) ve Türkçe; diğer diller sayfa açılırken eklenir (her dil
sürümü sayfaya ayrı girilir). Tanıtım metinleri reklam dili kullanmaz, "en iyi", "1 numara" gibi iddia içermez.

### 1. Komodin ve masa saati

- **Kime:** gece saati, komodin saati, StandBy saati arayanlar.
- **Anahtar kelimeler (en):** bedside clock, night clock, standby clock, desk clock, nightstand, big clock
- **Anahtar kelimeler (tr):** komodin saati, gece saati, masa saati, büyük saat, yatak başı saati
- **Görsel sırası:** `07-gece` (çizim: karanlık oda, komodin), `01-saat`, kadran galerisi (yeni: `-openScreen dials`),
  tam ekran saat (yeni), `05-uyku`.
- **Tanıtım (en, 137):** 21 clock styles and 25 dials, a dim red night mode that won't wake you, and alarms with a gentle
  sunrise light. Free, no ads, no account.
- **Tanıtım (tr, 145):** 21 saat modeli ve 25 kadran, uykunuzu bölmeyen kısık kırmızı gece modu, gün doğumu ışığıyla
  uyandıran alarm. Ücretsiz, reklamsız, hesap gerekmez.

### 2. Widget'lar

- **Kime:** ana ekran ve kilit ekranı widget'ı arayanlar.
- **Anahtar kelimeler (en):** widgets, clock widget, transparent widget, world clock widget, moon widget, lock screen widget
- **Anahtar kelimeler (tr):** widget, saat widget, şeffaf widget, dünya saati widget, ay evresi widget, kilit ekranı
- **Görsel sırası:** widget galerisi (yeni: `-openScreen widgets`), şeffaf widget (yeni), `01-saat`, `04-dunya`.
- **Tanıtım (en, 142):** Clocks, world time, Moon phase, weather and countdowns on your Home and Lock Screen, including a
  see-through widget. Free, no ads, no account.
- **Tanıtım (tr, 162):** Ana ekranda ve kilit ekranında saat, dünya saatleri, Ay evresi, hava ve geri sayım; duvar kâğıdına
  uyan şeffaf widget da var. Ücretsiz, reklamsız, hesap gerekmez.

### 3. Namaz vakitleri

- **Kime:** nötr dil sürümlerinde (en, de, fr, es, pt, ja…) namaz vakti arayanlar. Müslüman vitrini dillerinde (tr, ar-SA, id,
  ms, ur-PK) varsayılan sayfa zaten bu içerikte, ayrı sayfa gerekmez.
- **Anahtar kelimeler (en):** prayer times, salah, azan, qibla, ramadan, quran
- **Anahtar kelimeler (de):** Gebetszeiten, Gebetszeit, Qibla, Ramadan, Koran
- **Görsel sırası:** `07-vakitler` → `02-vakitler` biçimi (müslüman vitrininin sırası: vakitler, imsakiye, takvim, saat).
- **Tanıtım (en, 148):** Prayer times for your city with 14 calculation methods, a qibla compass, Ramadan timetable, Quran
  and duas. Works offline. Free, no ads, no account.
- **Tanıtım (tr, 138):** 14 hesap yöntemiyle namaz vakitleri, kıble pusulası, imsakiye, Kur'an ve dualar. İnternetsiz çalışır.
  Ücretsiz, reklamsız, hesap gerekmez.

### 4. Dünya saatleri ve toplantı

- **Kime:** uzaktan çalışanlar, farklı saat dilimindeki ekipler.
- **Anahtar kelimeler (en):** world clock, time zone converter, meeting planner, time difference, utc, stock market hours
- **Anahtar kelimeler (tr):** dünya saatleri, saat farkı, saat dilimi, toplantı planlayıcı, utc
- **Görsel sırası:** `04-dunya`, toplantı planlayıcı (yeni: `-openScreen planner`), `01-saat`, widget galerisi.
- **Tanıtım (en, 141):** World clocks on a spinning globe, a meeting planner that finds shared working hours, and 8 stock
  exchange sessions. Free, no ads, no account.
- **Tanıtım (tr, 145):** Dönen dünyada şehir saatleri, ortak mesai saatlerini bulan toplantı planlayıcı ve 8 borsanın seans
  saatleri. Ücretsiz, reklamsız, hesap gerekmez.

### 5. Panchang (Hindistan)

- **Kime:** en-IN ve hi kullanıcıları (App Store'da Hintçe dil sürümü var; Hindistan mağazası en-GB/en-US ve hi'yi dizinler).
- **Anahtar kelimeler (en):** panchang, rahu kaal, hindu calendar, tithi, muhurat, choghadiya *(choghadiya yok; eklenmeden
  kelime kullanılmaz)*
- **Anahtar kelimeler (hi):** पंचांग, राहु काल, हिंदू कैलेंडर, तिथि, मुहूर्त
- **Görsel sırası:** panchang ekranı (yeni: `-openScreen panchang`), takvim (Hindu takvimi açık), `01-saat`, `04-dunya`.
- **Tanıtım (en, 138):** Today's Rahu Kaal, Yamaganda, Gulika and Abhijit muhurta for your city, with tithi and nakshatra.
  Works offline. Free, no ads, no account.
- **Tanıtım (hi):** sayfa açılırken yazılır (Hintçe metinler dil denetimi bekliyor).

### 6. Takvimler ve tatiller

- **Kime:** başka takvim ve tatil arayanlar (Çin, Yahudi, Hindu, Ortodoks…).
- **Anahtar kelimeler (en):** holidays, public holidays, lunar calendar, chinese calendar, jewish calendar, hijri calendar
- **Anahtar kelimeler (tr):** resmi tatiller, köprü tatil, ay takvimi, çin takvimi, hicri takvim
- **Görsel sırası:** `03-takvim` (nötr: kapsayıcı takvimler açık), köprü tatil (yeni), `01-saat`.
- **Tanıtım (en, 136):** Public holidays in 32 countries and 22 calendars, from Chinese and Jewish to Hindu and Orthodox,
  side by side. Free, no ads, no account.
- **Tanıtım (tr, 130):** 32 ülkenin resmî tatilleri ve Çin'den Yahudi, Hindu ve Ortodoks takvimine 22 takvim yan yana.
  Ücretsiz, reklamsız, hesap gerekmez.

## Sitede açılış sayfaları (yapılmadı)

Araştırma her kitle için sitede ayrı bir açılış sayfası da öneriyordu. Yapılmadı: sahibin 9 Ekim 2026 kararı sayfalar arası
içerik bağlantısını kaldırdı (yalnız menü, araçlar merkezi ve arama). Bir açılış sayfası ya diğer sayfalara bağlantı verir
(karara aykırı) ya da var olan sayfanın kopyası olur (arama motorunda kopya içerik). Kitlelerin sayfaları zaten tek başına
duruyor: dünya saatleri, toplantı planlayıcı, panchang, fotoğraf ışığı, kapsayıcı takvimli yıl sayfaları, uyku hesaplama.
Sahip kararı değiştirirse bu bölüm yeniden ele alınır.
