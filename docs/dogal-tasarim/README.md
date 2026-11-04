# Doğal tasarım önerisi (4 Kasım 2026)

**Soru (sahip):** "Tüm tasarımlar aşırı robotik, daha doğal tasarımlar lazım."

**Kısa cevap:** Uygulamanın iskeleti iyi; robot hissini iskelet değil yüzey veriyor. Her şey aynı beyaz kutuda, aynı kalınlıkta yazıyla, hazır simgelerle ve tek turuncu renkle çiziliyor. Öneri, bu yüzeyi "kâğıt, gök ve ışık" diliyle değiştirmek. İşin çoğu iki platformdaki ortak belirteç ve bileşen dosyalarında yapılır; ekranları tek tek yeniden yazmak gerekmez.

- **Görsel sunum:** [`sunum.html`](sunum.html). Altı ekranın "şimdi / önerilen" çizimleri, renk, yazı, boşluk ve hareket belirteçleri.
  - Sağ üstteki düğmeyle koyu temaya geçilir.
  - "Günün saati" düğmeleri (şafak … gece) saat ve vakit çizimlerinin ışığını değiştirir.
  - Tarayıcıda dosyayı açmak yeter; `now/` klasörü yanında durmalı. Yazı tipleri Google Fonts'tan yüklenir (yalnız bu sunum için).
- **"Şimdi" görüntüleri:** [`now/`](now/) klasöründe. Android ekran görüntüleridir (Robolectric, 3–4 Kasım 2026). iPhone aynı belirteçleri kullandığı için çok benzer görünür.
- **Kapsam:** yalnız öneri. Kod değişmedi.
- **Değişmeyenler:**
  - Gezinme kararı (Seçenek B: kişisel alt çubuk ve "Tümü"; [`docs/kullanici-dostu/`](../kullanici-dostu/README.md)).
  - Özellikler, kayıt anahtarları, 24 uygulama rengi ve 8 atmosfer. Bunlar yeni belirteçlerin üstünde çalışmaya devam eder.

İncelenen ekranlar: 216 görüntüden 56'sı. 24'ü tek tek, 32'si dörtlü sayfalarda açıldı (saat, vakitler, Tümü, ayarlar, hava, takvim, dünya, mevsimler, kıble, araçlar, ibadet, hakkında, ilk açılış; açık ve koyu). Ayrıca 33 widget görüntüsüne toplu bakıldı.

---

## 1. Teşhis: uygulamayı "robot gibi" gösteren 10 şey

| # | Ne görülüyor | Nerede (görüntü) | Koddaki kaynağı |
|---|---|---|---|
| 1 | **Her şey aynı beyaz kutu.** Kartlar saf beyaz (#FFFFFF), 1 dp kenarlık, aynı köşe, aynı iç boşluk. Krem zemin üstünde beyaz kutular "form doldurma" hissi veriyor. | `tr-ayarlar.png`, `tr-alarm.png`, `tr-uyku.png`, `tr-ozellikler.png`, `tr-rozetler.png` | Android `Common.kt` `AppCard` (köşe 16, kenarlık %50); iOS `Format.swift` `.card()` (köşe 14; iki platform farklı) |
| 2 | **Hazır, dolu simgeler; hepsi aynı kahverengi.** Tümü sayfasında 15 kare birbirinin kopyası: beyaz kart, bej kutu, kahverengi dolu simge. | `tr-araclar.png`, `tr-rozetler.png`, `tr-ozellikler.png` | Android'de 274 `Icons.Filled` (13 `Outlined`); iOS'ta 125 SF Symbol. Kendi çizgi simge takımımız var (`mobile/shared/icons/icons.json`, 24 simge) ama yalnız Mevsimler'de ve sabah günlüğünde kullanılıyor (`SeasonsScreen.kt`, `Journal.kt`; iOS `WorldSeasons.swift`, `Journal.swift`) |
| 3 | **Emoji simge yerine kullanılıyor.** Parlak, üç boyutlu emoji düz arayüzle çatışıyor ve her telefon markasında başka görünüyor. | ⛅ ve 📤 `tr-hava.png`; 📅 📍 `tr-alarm.png`; 🕓 `tr-zamanlayici.png`, `tr-hiit.png`; 🍅 `tr-pomodoro.png`; 🕋 `tr-kible.png`; 🔒 `tr-hiit.png`; yüzler `tr-gunaydin.png`; Ay emojileri `tr-takvim.png` | Android kaynakta 📍 5, 📤 3, 🔒 3, 🕓 2, 🍅 2, 📅 2, 🕋 2 kez (`ToolVisuals.kt` `RoundDots`, `HoldToUnlock` vb.) |
| 4 | **Tek yazı tipi, her yerde kalın.** Sistem yazısı (Roboto / SF) yarı kalın ağırlıkla basılıyor; bir ekranda dört ayrı kalın turuncu satır yarışıyor. | `tr-alarm.png` (Sıradaki alarm, iki bağlantı, "Sonraki alarmı atla"), `tr-vakitler.png` ("Yaklaşan…") | Android'de 237 `FontWeight.SemiBold`; `SectionTitle` 13 sp yarı kalın gri etiket |
| 5 | **Renk sıcaklığı tutmuyor.** Sıcak kâğıt zemine hazır, doygun maviler konmuş. Koyu tema mavi-gri (#11141A); "gece" değil "terminal" gibi. | Vakit sayacı mavisi `tr-vakitler.png`; açık mavi saat kutuları `tr-dunya.png`, koyu temada göz alıyor `tr-dunya-koyu.png`; `tr-saat-koyu.png`, `tr-hakkinda-koyu.png`; kırmızı blok `tr-hiit.png` | `WorshipExtras.kt:322–327` ve `WorshipExtras.swift:312–` (#2C7BD1→#6FB3EA); `WorldScreen.kt:124` ve `WorldSeasons.swift:202` (#D5E8FF iki temada aynı); `ToolVisuals.kt` `PhaseColors` |
| 6 | **Mekanik boşluk ve hizalama.** Her yerde aynı 16 dp kenar ve 14 dp aralık. Bir ekranda hem çok boş alan hem sıkışık metin var. Aynı ekranda sola ve ortaya hizalı satırlar karışık. | Boş: `tr-saat.png` (üst ve alt), `tr-zamanlayici.png` (alt yarı). Sıkışık: `tr-alarm.png` (açıklama paragrafı), `tr-takvim.png` (gün kartı). Karışık hizalama: `tr-vakitler.png`. Başlık satırında 6 simge: `tr-takvim.png` | `Components.kt` `ToolFrame` `spacedBy(14.dp)`, `padding(horizontal = 16.dp)` |
| 7 | **Hazır Material parçaları olduğu gibi.** Onay işaretli bölmeli düğme ("✓ 24 saat"), kalın anahtarlar, çerçeveli metin kutusu. Bunlar "Android ayar ekranı" diye bağırıyor. | `tr-ayarlar.png`, `tr-pomodoro.png`, `tr-zamanlayici.png`, `tr-ders-sayaci.png`; `tr-uyku.png` (2 anahtar ve 7 dolu gün hapı aynı ağır turuncu); `tr-hakkinda-koyu.png` | Android'de 8 dosyada 11 `SegmentedButton`, 48 `Switch`; iOS'ta 15 `.pickerStyle(.segmented)` |
| 8 | **Doku ve çizim yok; olan çizimler kaba.** Bütün yüzeyler düz renk. Hava sahnesindeki bulutlar keskin kenarlı gri elipsler ve yazıların üstüne biniyor. Namaz rehberi figürleri çöp adam. | Bulut: `tr-hava.png`, `en-hava-koyu.png` ("Humidity" yazısı bulutun altında). Çöp adam: `tr-namaz-rehberi-cizimler.png` | `AnimatedBackground.kt:268–281`: radyal geçiş yarıçapı genişliğe göre (w/2), elips yüksekliği 0,28w; geçiş üstte ve altta sönmeden kesiliyor, kenar sert çıkıyor |
| 9 | **İlk görülen saat kadranı istasyon saati gibi.** Kalın siyah çentikler, düz kadran, düz ibreler, gölge yok. Uygulamanın ilk yüzü bu. | `tr-saat.png`, `tr-kurulum.png`, `tr-saat-koyu.png` | `Dials.kt` (klasik kadran), iOS `AnalogClock.swift` |
| 10 | **Vurgu rengi her şeyde.** Bağlantı, sayı, simge, anahtar, hap, başlık: hepsi #9A3F00. Ne önemli, ne değil belli olmuyor. | `tr-uyku.png` (07:00, 8 sa 0 dk, 30 dk önce, gün hapları, anahtarlar, bağlantı hepsi turuncu), `tr-alarm.png` | Değerler `Row2` ve ekran kodunda `Theme.c.accent` ile boyanıyor |

**İyi olanlar (korunacak):**
- Kâğıt paleti fikri ve kiremit rengi marka vurgusu.
- Karşıtlık her yerde ≥6:1.
- Atmosferler ve NASA dokulu Ay.
- Kadran galerisi (`tr-kadranlar.png`).
- Tebrik kartları (`tr-tebrik.png`) ve takvim yaprağı (`tr-takvim-yapragi.png`): uygulamanın en sıcak ekranları.
- Ders sayacındaki sarı LED (`tr-ders-sayaci.png`).
- Site ve iki uygulamanın aynı belirteçleri kullanması.

Öneri, bu iyi ekranlardaki duyguyu bütün uygulamaya yaymak.

---

## 2. İlkeler: bu uygulama için "doğal" ne demek

Saat Tarih zamanı gösterir. Zaman doğada ışıktır: şafak, öğle, ikindi, akşam, gece. Uygulama bu ışığı, bir takvim yaprağının kâğıdını ve bir mürekkebin sıcaklığını taşımalı. Her ilkenin altında uygulanabilir kural ve değerler var. Bütün değerler bölüm 3'teki belirteçlerde toplu.

### İlke 1. Sıcak kâğıt, sıcak gece
- **Açık tema "kâğıt":** zemin bir ton daha krem (#F6F0E4). Kart saf beyaz değil, sıcak beyaz (#FFFBF4). Yazı soğuk lacivert-siyah değil, mürekkep kahvesi (#2B2520).
- **Koyu tema "gece, lamba ışığı":** zemin mavi-gri değil, çivit-kömür (#14151C). Yazı soğuk beyaz değil, krem (#F1EADF). Vurgu yine lamba turuncusu (#F2A65A).
- **Kural:** saf beyaz (#FFFFFF), saf siyah (#000000) ve soğuk gri arayüz zemininde kullanılmaz. İstisna: tam ekran saat ve gece modu siyahı (göz ve ekran için).

### İlke 2. Kutu değil kâğıt
- Kartın kenarlığı kalkar. Yerine açık temada sıcak, çok yumuşak gölge gelir (kâğıt üstüne kâğıt). Koyu temada gölge yok; kart bir ton açık ve üst kenarında ince ışık çizgisi var.
- Köşe 16 → **22**; büyük kahraman kartlar (vakit sayacı, hava) **28**.
- **Kural:** bir ekranda en çok iki kart yüksekliği kullanılır (e1 normal, e2 öne çıkan). Kart içinde kart yok. Bir kartta tek satır varsa kart yerine düz satır kullanılır.
- **Kural:** liste ayırıcıları azalır. Satırlar arası boşluk ayırır; ayırıcı yalnız 5'ten uzun listelerde, simgeden sonra başlar ve %60 saydamdır.

### İlke 3. Kitap gibi yazı
- **Başlıklar ve tarih rakamları** için açık lisanslı (SIL OFL 1.1) **Literata** yazı tipi. Literata, Google'ın e-kitap uygulaması için yaptırdığı sıcak, okunaklı bir kitap yazısıdır.
  - Latin, Latin Ext (ğ, ş, İ, ı, ə dahil), Kiril, Yunan ve Vietnam harflerini kapsar. Bu, 18 dilin 10'u demek: tr, en, de, az, id, fr, fil, es, pt, ru.
  - Tablo rakamları (`tnum`) var; 4 Kasım 2026'da Google Fonts dosyasında denetlendi. Saat ve süreler zıplamaz.
  - Rakamlar her dilde Latin olduğu için tarih ve sayı rakamlarında 18 dilin hepsinde kullanılabilir.
- **Gövde metni sistem yazısında kalır** (Roboto / SF Pro). Sistem yazısı 18 dilin harflerini, Dynamic Type'ı ve ekran okuyucuyu en iyi taşır. Arapça, Urduca, Farsça, Japonca, Korece, Çince, Tayca ve Hintçede başlıklar da sistem yazısında kalır (bu dillerin sistem yazıları zaten nitelikli).
- **Ağırlık:** yalnız üç ağırlık. 400 gövde, 500 vurgu ve kart başlığı, 600 sayfa başlığı. Büyük sayaç rakamları 300 (ince, dev rakam; zaman araçlarında yaygın). 700 (kalın) arayüzde kullanılmaz; saat modellerindeki kullanıcı seçimleri hariç.
- **Kural:** bir ekranda en çok bir kalın turuncu satır olur.

### İlke 4. Nefes alan düzen
- **Boşluk ölçeği** (dp/pt): 4, 8, 12, 16, 20, 24, 32, 48.
- **Sayfa kenarı** 16 → **20**. Kart iç boşluğu **20**.
- **Ritim kuralı:** ilgili öğeler arasında 8–12, ayrı konular arasında 24–32 boşluk. Bugün her şey 14; göz neyin neyle ilgili olduğunu çıkaramıyor.
- **Hizalama kuralı:** bir ekranda tek hizalama. Sayfalar sola hizalı. Ortaya hizalı olanlar yalnız tek odaklı ekranlar: saat, sayaç, boş durum.
- **Uzun açıklama kuralı:** listede en çok 2 satırlık not, 14 pt, soluk renk. Fazlası "Nasıl çalışır?" alt sayfasına.

### İlke 5. Kendi çizgimiz: simge ve küçük resim

> **Sahibin kararıyla değişti (4 Kasım 2026):** "Ben çizgi simge istemiyorum. İnsancıl, kullanıcı dostu ve göze hoş gelen tasarım istiyorum; bulut varsa gerçekçi bir bulut olsun." Örnek sayfasından seçim: hava ve uygulama simgeleri **gerçekçi 3D** (Microsoft Fluent Emoji 3D, MIT; `mobile/shared/icons3d/`). Aşağıdaki çizgi simge önerisi uygulanmadı; yalnız tarihsel kayıt.
- **Simgeler:** mevcut `icons.json` çizgi simge takımı 24'ten yaklaşık **64**'e çıkar ve bütün arayüzde Material/SF simgelerinin yerini alır.
  - Kapsam: alt çubuk, Tümü, vakitler, ayarlar, araç düğmeleri.
  - Kurallar: 24×24 alan, **1,75** çizgi, yuvarlak uç ve birleşim, açık biçimler. Tek bir ayrıntı ikinci renkte (altın). Dolgu yalnız küçük noktalarda.
  - Boyutlar 20 / 24 / 28.
  - Seçili sekmede aynı simgenin hafif dolgulu eşi.
- **Emoji arayüzde kullanılmaz.** İstisnalar: kullanıcının kendisinin seçtiği şeyler (geri sayım simgesi, zikir adı gibi) ve paylaşım kartlarındaki içerik.
- **Simge kutusu "çakıl taşı" olur.**
  - Biçim: köşeleri birbirinden biraz farklı, organik bir yuvarlak kare (Compose'ta köşe başına yüzde, iOS 17'de `UnevenRoundedRectangle`).
  - Zemin: kategorinin doğa renginin açık tonu (açık temada %16, koyuda %20).
  - Kategoriler:
    - Süre ve alarm: kiremit.
    - Gün ve plan: adaçayı.
    - Gök ve dünya: gök mavisi.
    - İbadet: altın.
    - Diğer: akşam moru.
- **Küçük resim üslubu:** "kâğıt kesik": düz biçimler, yumuşak geçiş, palette en çok 4 renk, mürekkep çizgisi 1,5. Kodla çizilir (Canvas / SwiftUI Shape / SVG). Fotoğraf ve lisanslı görsel yok.

### İlke 6. Günün saatine göre ışık
- **"Gün ışığı" otomatik atmosferi:** sayfaların üst kısmı seçili yerde Güneş'in yüksekliğine göre çok soluk bir renge bürünür. Şafak şeftali, sabah gök, ikindi altın, akşam mor, gece çivit.
  - Renk sayfanın üstünden zemine doğru söner (ilk ~360 dp).
  - Bütün tonlarda yazı karşıtlığı ≥4,5:1 (bölüm 3).
  - Bugünkü 8 atmosferin yanına 9. seçenek olarak gelir; önerimiz yeni kurulumlarda varsayılan olması.
- **Vakit sayacı gökyüzü:** bugünkü doygun maviler yerine boyalı, yumuşak altı gökyüzü.
  - Açık gökte yazı mürekkep rengi, koyu gökte krem.
  - Her gökte karşıtlık ≥5:1.
  - Güneş ya da Ay gerçek yüksekliğe göre karta yerleşir.
- **Tema başka, günün saati başka:** koyu temada öğle saati gökyüzü karartılmış gündüz mavisidir, gece göğü değil.
- **Simge sahneyle uyuşur:** gece sahnesinde hava simgesi Ay'lı olur. Bugün hava kartında yıldızlı gece göğünün üstünde güneşli bulut emojisi duruyor (`tr-hava.png`, `en-hava-koyu.png`).

### İlke 7. Ölçülü süs: doku ve motif
- **Kâğıt dokusu:** zeminde %4 saydam, kodla üretilen gren. Android'de 128×128 gürültü bitmap'i, iOS'ta aynı görüntü, sitede SVG `feTurbulence`. Döşenerek basılır; dosya ve lisans gerekmez. Gece modunda ve tam ekranda kapalı.
- **İslami geometrik motif (8 köşeli yıldız, hatem) ve mihrap kemeri çizgisi.** Saygı kuralları:
  - Yalnız ibadet ekranlarında, vakit sayacının köşesinde, Kur'an ve dua başlıklarında, tebrik kartlarında.
  - Saydamlık %6–10; ince çizgi; hiç oynamaz, dönmez, zıplamaz.
  - Kur'an ve dua metninin arkasına, alt çubuğa, oyun ve rozet ekranlarına konmaz.
  - Hat taklidi yapılmaz (Arapça yalnız gerçek metin olarak yazılır).
  - Süs amaçlı insan ya da yüz çizimi yok. Namaz rehberi figürleri yüzsüz, yumuşak silüet.
  - Vakit sayacının altında alçak bir ufuk çizgisi (tepe + küçük kubbe ve minare), %12 saydamlık.

### İlke 8. Tek vurgu, çok doğa rengi
- **Kiremit vurgu** yalnız şunlarda: ekranın ana düğmesi, seçili durum (sekme, seçenek), açık anahtar, bağlantı.
- Sayılar ve değerler yazı renginde (#2B2520). "07:00" turuncu olmaz.
- **Doğa renkleri** (adaçayı, gök, altın, akşam moru) yalnız simge, kategori ve ince vurgu için. Yazı olarak kullanılırsa karşıtlıkları ≥4,5 (bölüm 3).
- Aralık antrenmanı evre renkleri yumuşar:
  - Çalış: adaçayı #3F7A4F.
  - Dinlen: kiremit #B4532A.
  - Isın: altın #9A6A12.
  - Hepsi beyaz yazıyla ≥4,5:1.

### İlke 9. Yumuşak hareket
| Belirteç | Değer | Nerede |
|---|---|---|
| hızlı | 150 ms | basma, anahtar, seçim |
| orta | 250 ms | kart açılma, sekme geçişi, alt sayfa |
| yavaş | 400 ms | sayfa geçişi |
| gök | 1.200 ms | gökyüzü ve atmosfer renk geçişi |
| giriş eğrisi | cubic-bezier(0.2, 0.8, 0.2, 1) | Compose `CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)`, SwiftUI `.timingCurve(0.2, 0.8, 0.2, 1)` |
| çıkış eğrisi | cubic-bezier(0.4, 0, 1, 1) | kaybolan öğeler |
| yay | sönüm 0,8, sertlik 380 | sürükleme, kaydırma; SwiftUI `.spring(response: 0.35, dampingFraction: 0.8)` |
| basma | ölçek 0,97, 120 ms | kart, kare, düğme |

- Sayfa geçişi: 12 dp kayma + soluklaşma (Material'in tam ekran kaydırması yerine).
- **Hareketi azalt** (iOS `accessibilityReduceMotion`, Android animasyon ölçeği 0): yalnız soluklaşma. Gökyüzü renkleri anında değişir; gren ve bulutlar durur.

---

## 3. Belirteçler

### 3.1 Renk

Karşıtlıklar 4 Kasım 2026'da WCAG 2.1 formülüyle hesaplandı (sitedeki `npm test` ile aynı formül).

| Belirteç | Açık "kâğıt" | Koyu "gece" | Bugün (açık / koyu) | Not |
|---|---|---|---|---|
| `bg` zemin | **#F6F0E4** | **#14151C** | #F7F4EE / #11141A | |
| `surface` kart | **#FFFBF4** | **#1D1E27** | #FFFFFF / #1A1E26 | |
| `surface2` çukur, iz, kadran | **#EDE4D3** | **#272834** | #EFEBE2 / #232834 | |
| `line` ince çizgi | **#E2D6C1** | **#343542** | (`border` #D6CFBF / #343B49) | yalnız ayırıcı ve alan kenarı |
| `text` yazı | **#2B2520** | **#F1EADF** | #1B1F27 / #EEF0F4 | bg'de 13,3 / 15,2 |
| `muted` soluk yazı | **#6A5F55** | **#B4AA9B** | #565C68 / #A5ACB9 | bg 5,5 / 7,9; surface2 4,9 / 6,4 |
| `faint` süs | #A89A88 | #6E675E | yok | **yazıda kullanılmaz** |
| `accent` kiremit | #9A3F00 (aynı) | #F2A65A (aynı) | aynı | bg 6,0 / 9,0 |
| `accentSoft` | **#F3E1CC** | **#3B2A1C** | %16 saydam ana renk | yazı bunun üstünde 11,9 / 11,5 |
| `onAccent` düğme yazısı | **#FFF8EF** | **#1A1410** | #FFFFFF / zemin | 6,5 / 9,0 |
| `gold` altın | **#835A0D** | **#E2B65C** | (`accent2`) | bg 5,4 / 9,6 |
| `sage` adaçayı | **#4A6B47** | **#9CC294** | yok | bg 5,3 / 9,2 |
| `sky` gök | **#3A6889** | **#8DB8DA** | yok | bg 5,3 / 8,7 |
| `dusk` akşam moru | **#7A4760** | **#D69BB5** | yok | bg 6,4 / 8,0 |
| `ok` | #2E6A39 | #8FD3A0 | #176B3A / #7FD9A1 | bg 5,7 / 10,4 |
| `err` | #A3311B | #FF9F8A | #A31D1D / #FF9D9D | biraz sıcak; bg 6,1 / 9,2 |

**Çakıl taşı simge kutusu** (simge rengi, kendi açık tonu üstünde; çizgi simge için en az 3:1 gerekir):
- Açık temada 4,6–5,5.
- Koyu temada 5,0–5,7.

**Gün ışığı tonları** (sayfa üstü; altında `muted` ve `accent` yazı ≥4,5):

| An | Açık | Koyu |
|---|---|---|
| Şafak | #F8E4D6 | #2A1E2C |
| Sabah ve öğle | #E9EFF0 | #172230 |
| İkindi, altın saat | #F6E3C4 | #2A2118 |
| Akşam | #EEDDE4 | #271B2C |
| Gece | #E3E4EE | #12131C |

En düşük karşıtlık: açık temada akşam tonu üstünde soluk yazı, 4,76.

**Vakit sayacı gökleri** (üstten alta; yazı rengi ve en kötü karşıtlık):

| Vakit aralığı | Renkler | Yazı | Karşıtlık |
|---|---|---|---|
| Gece (yatsı → imsak) | #1F2240 → #3B3762 | krem #FFF8EC | 10,4 |
| Şafak (imsak → güneş) | #2E2C55 → #8A5A78 | krem | 5,2 |
| Sabah (güneş → öğle) | #A9C9DF → #F2E6CC | mürekkep #2B2520 | 8,7 |
| Öğle (öğle → ikindi) | #9CC4E0 → #E3EEF2 | mürekkep | 8,2 |
| İkindi (ikindi → akşam) | #E0B577 → #F5E3BF | mürekkep | 8,0 |
| Akşam (akşam → yatsı) | #46305A → #8E4B4E | krem | 6,1 |

- Koyu temada gündüz gökleri karartılır: sabah #2D4A63 → #4F5E6E, krem yazı ile 6,3.
- Şafak ve akşamdaki şeftali ufuk ışığı (#E8A27C) yalnız alttaki süs şeridinde kullanılır, yazının arkasında değil.

**Uygulama renkleri (24 tema) ne olur?** Bugünkü `AccentPalette.colors()` taban renkleri ana renge karıştırıyor. Yalnız taban renkler değişir (yukarıdaki `bg`, `surface`, `surface2`, `line`, `text`, `muted`), karıştırma kuralı aynı kalır. Bütün temalar kendiliğinden ısınır.

### 3.2 Yazı

| Rol | Yazı tipi | Boyut / satır | Ağırlık | Not |
|---|---|---|---|---|
| Sayfa başlığı | Literata* | 30 / 36 | 600 | harf aralığı −0,01 em |
| Bölüm başlığı | Literata* | 19 / 24 | 500 | bugünkü 13 sp gri yarı kalın etiketin yerine |
| Kart başlığı | sistem | 17 / 22 | 500 | |
| Gövde | sistem | 16 / 24 (Android), 17 / 22 (iOS) | 400 | |
| Açıklama | sistem | 14 / 20 | 400, `muted` | en çok 2 satır |
| Küçük etiket (alt çubuk) | sistem | 12 / 16 | 500 | |
| Büyük sayaç | sistem | 56–88 | 300, `tnum` | zamanlayıcı, pomodoro, vakit sayacı |
| Tarih rakamı, sıcaklık | Literata | 40–96 | 500, `tnum` | takvim yaprağı, hava, saat ekranı tarihi |

\* Arapça, Urduca, Farsça, Japonca, Korece, Çince, Tayca, Hintçede sistem yazısı: başlık 28 / 34, 600.

- **Dynamic Type:**
  - iOS: `Font.custom("Literata", size: 30, relativeTo: .largeTitle)`.
  - Android: `sp` birimi ve uygulamanın yazı boyutu ayarı (`ProvideFontScale`) aynen çalışır.
- **Dil seçimi kuralı:** başlık bileşeni dili sorar (`Strings.lang` / `AppLocale.code`). Literata yalnız Latin ve Kiril dillerinde seçilir. Böylece Rusça başlıkta harf karışmaz.

### 3.3 Biçim, boşluk, yükseklik

| Belirteç | Değer |
|---|---|
| Köşe | xs 8 · s 12 · m 16 · l **22** (kart) · xl **28** (kahraman kart, alt sayfa) · hap · çakıl taşı (köşeler %46 / %54 / %50 / %48) |
| Boşluk | 4 · 8 · 12 · 16 · 20 · 24 · 32 · 48; sayfa kenarı 20; kart içi 20 |
| Satır | liste satırı en az 52; dokunma alanı en az 48 dp / 44 pt |
| Yükseklik e1 (açık) | `0 1px 2px rgba(74,52,30,.06), 0 6px 18px rgba(74,52,30,.07)` |
| Yükseklik e2 (açık) | `0 2px 6px rgba(74,52,30,.08), 0 16px 36px rgba(74,52,30,.10)` |
| Yükseklik (koyu) | gölge yok; kart `surface`, üst kenarda 1 px `rgba(255,255,255,.05)` |
| Compose karşılığı | `Modifier.shadow(6.dp, shape, ambientColor = Color(0x334A341E), spotColor = Color(0x224A341E))` |
| SwiftUI karşılığı | `.shadow(color: Color(red: 0.29, green: 0.2, blue: 0.12).opacity(0.08), radius: 12, y: 6)` |

---

## 4. Öncelikli değişiklik listesi

**Çaba:** K küçük (yarım gün ile bir gün), O orta (birkaç gün), B büyük (bir hafta ve üstü). Her madde iki platformda birlikte yapılır. Android ve iOS dosyaları aynı satırda.

### P1: en az işle en çok görünen değişiklik (hepsi ortak dosyalarda)

| # | Değişiklik | Dosyalar | Çaba |
|---|---|---|---|
| 1 | **Renk belirteçlerini ısıt.** Taban renkler (bölüm 3.1); yeni `line`, `gold`, `sage`, `sky`, `dusk`, `onAccent`; `accentSoft` saydam değil düz renk. Sitede de aynı belirteçler. | Android `Theme.kt` (`AccentPalette.colors`, `AppColors`); iOS `Theme.swift`; site `src/styles.css` (`npm test` karşıtlık denetimi aynen geçmeli) | K |
| 2 | **Kartları kâğıt yap.** Kenarlık yok, köşe 22, iç boşluk 20, sıcak yumuşak gölge (koyuda ışık çizgisi). Tek bileşen değişince Android'de 50, iOS'ta 63 kullanım yeri birlikte değişir. | Android `Common.kt` `AppCard`; iOS `Format.swift` `.card()`; site `--shadow`, `--radius` ve `border: 1px solid var(--border)` kullanan 9 kural | K |
| 3 | **Yazıyı yumuşat.** Literata'yı ekle (iki ağırlık, 500 ve 600; Latin + Kiril alt kümesi; woff2 ölçümü ağırlık başına ~75 KB, TTF birkaç yüz KB). `PageTitle` ve `SectionTitle` yeni ölçekte. Ortak bileşenlerde SemiBold → Medium. | Android `res/font/`, `Components.kt` (`PageTitle`, `EmptyState`), `Common.kt` (`SectionTitle`, `SubBar`, `Row2`), `ToolsScreen.kt` (`RoundButton`); iOS `App/Resources/Fonts`, `Info.plist` `UIAppFonts`, `Components.swift`, `Format.swift` (`SectionTitle`), `ToolsTab.swift` (`RoundButton`) | K/O |
| 4 | **Emoji ve dolu simgeler yerine kendi çizgimiz.** `icons.json` 24 → ~64 simge. Sırayla: alt çubuk, Tümü kareleri, vakit satırları, ayarlar, araç düğmeleri. `IconBox` çakıl taşı biçimine ve kategori rengine geçer. | `mobile/shared/icons/icons.json`; Android `LineIcons.kt`, `MainActivity.kt` `TabBar`, `AllScreen.kt`, `PrayerScreen.kt`, `SettingsScreen.kt`, `Components.kt` `IconBox`, `ToolVisuals.kt` (`RoundDots` 🍅, `HoldToUnlock` 🔒); iOS `LineIcons.swift`, `SaatTarihApp.swift` (TabBar), `AllView.swift`, `PrayerTab.swift`, `InfoViews.swift`, `Components.swift` (`IconBox`, `BoxLabel`), `ToolVisuals.swift` | O |
| 5 | **Yumuşak bölmeli seçici ve anahtar.** Onay işaretsiz, kayan hap (iz `surface2`, seçili `surface` + e1). Anahtar: kenarlıksız iz, açıkken ana renk. Tek bileşen, bütün kullanım yerlerine. | Android `Components.kt` (yeni `SoftSegmented`, `SoftSwitch`), 8 dosyadaki 11 `SegmentedButton`; iOS `Components.swift`, 15 `.pickerStyle(.segmented)` | K |
| 6 | **Hazır mavileri doğaya çevir.** Vakit sayacı gökleri (bölüm 3.1) ve yazı rengini gökyüzünün parlaklığından seç. Dünya saati kutuları: göz alan açık mavi yerine küçük gök penceresi + kutusuz saat. Aralık evre renkleri yumuşak. | Android `WorshipExtras.kt:322–327` (`skyColors`), `WorldScreen.kt:121–126`, `ToolVisuals.kt` `PhaseColors`; iOS `WorshipExtras.swift:312–`, `WorldSeasons.swift:202–203`, `ToolVisuals.swift` | K |

P1 bitince uygulamanın genel duygusu değişir. Ekran düzenlerine dokunulmaz.

### P2: düzen ve atmosfer

| # | Değişiklik | Dosyalar | Çaba |
|---|---|---|---|
| 7 | **"Gün ışığı" otomatik atmosferi.** Güneş yüksekliği hesabı zaten var (`AnimatedBackground.sunAltitude`). Yeni kurulumda varsayılan; renk geçişi 1,2 sn. | Android `Theme.kt` (`Atmosphere`, `appBackdrop`), `AnimatedBackground.kt`; iOS `Theme.swift` (`Atmosphere`, `AppBackdrop`), `AnimatedBackground.swift` | O |
| 8 | **Kâğıt dokusu.** %4 gren, kodla üretilir; gece modu ve tam ekranda kapalı. | Android `Theme.kt` `appBackdrop`; iOS `Theme.swift` `AppBackdrop`; site `src/styles.css` (`body::before`) | K |
| 9 | **Klasik kadranı yumuşat.** İnce dış halka; dakika çentikleri nokta (%40); saat çentikleri yuvarlak uçlu, kısa. İbreler sivrilen biçimde, 2 dp yumuşak gölgeli; ortada altın kapak; kadranda hafif radyal geçiş ve altında yumuşak gölge. Saat ekranında model adı ve düğmeler soluk renkte. | Android `Dials.kt`, `ClockModels.kt`, `ClockScreen.kt`; iOS `Dials.swift`, `AnalogClock.swift`, `HomeView.swift`; site `src/js/main.js` `analogSvg` | O |
| 10 | **Hava sahnesi.** Bulutlar üst üste binen yumuşak dairelerden. Yazı alanında bulut yok (bulutlar üst üçte birde ve sağda). Ölçümler sahneden çıkıp altındaki kâğıt karta, çizgi simgeli. Emoji hava simgesi yerine çizim. Gündüz-gece sahneyi gerçek saat belirler, tema değil. | Android `AnimatedBackground.kt:268–281` (`clouds`), `WeatherScreen.kt`; iOS `AnimatedBackground.swift`, `WeatherView.swift`; site `src/js/weather-fx.js` | O |
| 11 | **Araç görselleri.** İlerleme halkası 10 → 6 dp, iz `line`, uçta küçük ışıklı nokta. Rakamlar 300 ağırlıkta. Tur domatesleri çizim; pasta zamanlayıcı temaya uyar (bugün koyu temada bile beyaz disk ve #333 çentik). | Android `ToolVisuals.kt` (`ProgressRing`, `RoundDots`, `PieTimer`, `PhasePanel`), `PomodoroScreen.kt`, `TimerScreen.kt`, `Intervals.kt`; iOS `ToolVisuals.swift`, `ToolsTab.swift`, `Intervals.swift` | K |
| 12 | **Hareket belirteçleri.** Tek `Motion` nesnesi (bölüm 2, ilke 9), basınca küçülme, sayfa geçişi. | Android yeni `Motion` (`Components.kt`), `MainActivity.kt`; iOS `Components.swift`, `SaatTarihApp.swift` | O |
| 13 | **Ekran düzeni temizliği.** Sayfalar sola hizalı. Uzun açıklamalar "Nasıl çalışır?" alt sayfasına. Değerler yazı renginde (ilke 8). Takvim başlığında en çok iki simge, gerisi "…" menüsünde. | `PrayerScreen.kt` / `PrayerTab.swift`, `AlarmScreen.kt` / `ToolsTab.swift`, `SleepScreen.kt` / `SleepView.swift`, `CalendarScreen.kt` / `CalendarTab.swift` | O |
| 14 | **Site.** Belirteçler (madde 1), Literata (kendi sunucumuzdan, `font-display: swap`), gren, kart gölgesi. | `src/styles.css`, `src/assets/fonts/`, `build.mjs` (preload) | K/O |

### P3: karakter

| # | Değişiklik | Dosyalar | Çaba |
|---|---|---|---|
| 15 | **Hatem ve mihrap kemeri bileşeni** (ilke 7 kurallarıyla): vakit sayacı köşesi, Kur'an ve dua başlığı, tebrik kartları, kıble. | Android yeni `Motifs.kt`, `WorshipExtras.kt`, `QuranExtras.kt`, `Greetings.kt`, `QiblaScreen.kt`; iOS yeni `Motifs.swift` ve karşılıkları | K/O |
| 16 | **Kur'an, dua ve Esma için Amiri yazı tipi** (SIL OFL 1.1, klasik nesih; woff2 ölçümü ~140 KB). Bugün sistemin Arapça yazısı kullanılıyor. | Android `res/font/`, `QuranExtras.kt`, `Religion.kt`, `Content.kt`; iOS `QuranExtras.swift`, `Religion.swift`, `Content.swift` | O |
| 17 | **Namaz rehberi figürleri yumuşak silüet.** Yüzsüz, yuvarlak uzuvlar, seccade biçimi; eklem sayısı iki platformda aynı kalır. | Android `GuideFigures.kt`; iOS `GuideFigures.swift` | O/B |
| 18 | **Boş durum ve ilk açılış küçük resimleri** ("kâğıt kesik" üslubu, ~12 çizim). | `Components.kt` / `Components.swift` (`EmptyState`), `Onboarding.kt` / `Onboarding.swift` | B |
| 19 | **Widget'lar.** Aynı belirteçler; 📍 ve hava emojisi yerine çizgi simge. Gün ışığı tonu yalnız saat widget'larında. | Android `Widgets.kt`, `Widgets2.kt`, `WorldWidgets.kt`, `res/layout/widget_*.xml`; iOS `Widget/Sources/*` | O |
| 20 | **Mağaza görselleri.** Yeni ekranlarla yeniden alınır. | `mobile/ios/scripts/frame-screenshots.mjs`, `fastlane/captions.json` | K |

**Önerilen sıra:**
1. P1'in 1, 2, 3 maddeleri birlikte (tek gönderim; bütün ekranlar aynı anda ısınır, ekran görüntüsü testleri bir kez yenilenir).
2. Sonra P1'in 4–6 maddeleri.
3. P2 madde madde.

---

## 5. Erişilebilirlik ve kısıtlar

- **Karşıtlık:**
  - Bütün yazı belirteçleri iki temada ≥4,5:1 (bölüm 3.1).
  - Çizgi simgeler ve odak halkası ≥3:1.
  - `faint` yalnız süs içindir.
  - Sitedeki `npm test` karşıtlık denetimi yeni değerlerle geçer: en düşük `muted`/`surface-2` 4,92.
- **Yazı boyutu:**
  - Literata başlıkları Dynamic Type ve Android yazı ölçeğiyle büyür.
  - %145'te başlıklar 2 satıra kadar kırılır, kesilmez.
  - Sayaçlar `minimumScaleFactor` / `maxLines = 1` + otomatik küçülme ile sığar.
- **Hareketi azalt:** bütün geçişler soluklaşmaya iner. Gren, bulut ve gökyüzü renk geçişi durur.
- **Sağdan sola (ar, ur, fa):**
  - Geri okları ve ileri/geri simgeleri aynalanır.
  - Gökyüzündeki Güneş yolu, kadranlar ve saat ibreleri aynalanmaz (fiziksel yön).
  - Çakıl taşı biçimi aynalanabilir; fark etmez.
- **Latin rakamı:** Literata rakamları 18 dilde aynı. Tablo rakamı (`tnum`) açık.
- **Lisans:** yalnız SIL OFL 1.1 yazı tipleri (Literata, ileride Amiri). OFL, uygulamaya gömmeye ve siteden sunmaya izin verir; lisans metni yazı tipi dosyasının yanına konur, yazı tipi tek başına satılmaz. Görseller kodla çizilir; fotoğraf ve lisanslı görsel yok.
- **Site gizliliği:** yazı tipi Google Fonts'tan değil kendi sunucumuzdan verilir. Ziyaretçinin IP'si Google'a gitmez. `nginx.conf` CSP'sinde zaten `font-src 'self'` var; değişiklik gerekmez.
- **Reklam yok, izleme yok:** değişmez.
- **Boyut:** Literata iki ağırlık + Amiri ≈ 1 MB'tan az; uygulama boyutunda fark edilmez.
- **Testler:** Android ekran görüntüsü testleri ve iOS CI görüntüleri her adımda yenilenir; iOS dosyaları değişince `mobile/ios/scripts/check-swift.mjs` çalıştırılır (CLAUDE.md kuralı).

## 6. Sahibe sorular (karar gerekenler)

1. **Literata başlıklar için uygun mu?** Alternatif: başlıklar da sistem yazısında, yalnız ağırlık ve boyut değişir. Daha az doğal ama sıfır dosya.
2. **"Gün ışığı" yeni kurulumlarda varsayılan olsun mu?** Öneri: evet. Mevcut kullanıcının seçimi korunur.
3. **İbadet ekranlarında hatem motifi ve ufuktaki kubbe-minare silüeti uygun mu?** Öneri: evet, %6–12 saydamlıkta ve yalnız bu ekranlarda.
