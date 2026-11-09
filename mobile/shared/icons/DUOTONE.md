# İki tonlu küçük simgeler (Phosphor duotone)

## Karar (8 Kasım 2026, sahibin seçimi "B Karma")

Sahip "çok fazla emoji" dedi ve örnek sayfadan **B "Karma"**yı seçti:

- **3D (Fluent Emoji, `icons3d`) yalnız şu yerlerde kalır:** hava ve gökyüzü verisi (hava ekranı, saatlik/günlük, hava durumlu
  saat modelleri, saat ekranındaki hava alanı, günaydın kartı, dünya saatlerindeki hava, küre etiketleri, paylaşım kartı,
  Dynamic Island / Live Activity, widget'lar), **altı vakit satırı** (imsak, güneş, öğle, ikindi, akşam, yatsı) ve sıradaki vakit
  kartının simgesi, **büyük tek çizimler** (ilk kurulumdaki soru sayfasının büyük simgesi, tur ve "başka neler var" sayfalarının üçlü
  simgesi, Mevsimler'deki büyük mevsim simgesi) ve **boş durumlar** (`EmptyState`).
- **Geri kalan bütün küçük simgeler iki tonludur:** alt çubuk, Tümü kareleri ve arama sonuçları, ayar satırları, İzinler, bildirim
  ayarı satırları, Özellikler satırları, Vakitler'deki dört ibadet karesi ve İbadet sayfası satırları, ilk sayfanın 3 × 3 özellik
  ızgarası, özet ve alt çubuk önizlemeleri, saat ekranı düzenleyicisinin satırları, kart başlıklarındaki ve bağlantılardaki küçük
  simgeler. İnce çizgi simge **istenmiyor**: iki tonlu = dolu biçim + aynı renkte %20 saydam arka katman.

## Dosyalar

| Dosya | Ne |
|---|---|
| `mobile/shared/icons/duotone-map.json` | Ad → Phosphor glif adı (111 ad), paket ve sürüm (`@phosphor-icons/core@2.1.1`), bütünlük özeti (sha512) |
| `mobile/shared/icons/gen-duotone.mjs` | Üretici (bağımlılıksız Node): `npm pack` ile paketi geçici klasöre indirir, bütünlüğü denetler, bellekte açar, yolları çevirir; `--check` CI'da |
| `mobile/shared/icons/duotone.json` | Üretilen veri (elle düzenlenmez) |
| `mobile/shared/icons/PHOSPHOR-LICENSE.txt` | Phosphor'un MIT lisans metni (paketten; üretici yazar ve denetler) |
| `mobile/ios/App/Resources/duotone.json`, `mobile/android/app/src/main/assets/duotone.json` | `gen-strings.mjs` kopyalar (`_note` çıkarılır, sıkıştırılır). iOS'ta uygulama hedefi `App/Resources` klasörünün tamamını aldığı için `project.yml`'e ekleme gerekmedi; **widget, saat ve masaüstü hedeflerinde yok** (orada gerekmez) |

Yeni simge: `duotone-map.json`'a ad ekle → `node mobile/shared/icons/gen-duotone.mjs` → `node mobile/shared/gen-strings.mjs`.
CI ("Ortak test verisi güncel"): `node mobile/shared/icons/gen-duotone.mjs --check` (npm ve ağ ister).

### Biçim

```json
{ "alarm": [{"o": 0.2, "d": "M12 …Z"}, {"o": 1, "d": "M12 …Z"}], … }
```

- Her ad iki katman: önce `o: 0.2` arka katman, sonra `o: 1` ön katman. İkisi **aynı renkle**, sırayla **doldurulur** (stroke yok);
  katmanın saydamlığı = rengin saydamlığı × `o`.
- `d`: 24 × 24 alanda, yalnız **mutlak `M x y`, `L x y`, `C x1 y1 x2 y2 x y`, `Z`** (her komut kendi harfiyle; `O` yok). Sayılar iki
  ondalık. Mevcut ayrıştırıcılar (`LineIcons.parse` Kotlin ve Swift) **değişiklik gerektirmeden** okur (denendi: 111 ad, 6 bin+ komut,
  sorunsuz).
- Doldurma kuralı **nonzero** (Compose `Path` ve SwiftUI `fill` varsayılanı; even-odd kullanılmaz). Delikler yön farkıyla oluşur.
- Gliflerin hiçbiri yazı yönüne göre aynalanmaz (RTL'de de aynı çizilir).

## Çizim kuralı

Önerilen ortak parçalar (adlar öneridir; iki platformda aynı ölçüler):

**Android** (`LineIcons.kt` yanına):
- `object DuoIcons`: `assets/duotone.json`'ı bir kez okur, her katmanı `LineIcons.parse(d).first` ile `Path`'e çevirir ve **önbellekte
  tutar** (her karede yeniden ayrıştırma yok; yollar uzun). `has(name)`.
- `@Composable fun DuoIcon(name: String, size: Dp, color: Color, label: String? = null)`: `Canvas` + `scale(size/24)`;
  `drawPath(path, color.copy(alpha = color.alpha * o))` (Fill). `label` yoksa ekran okuyucudan gizli.
- `@Composable fun DuoChip(name: String, color: Color, box: Dp = 32.dp, glyph: Dp = 19.dp)`: daire
  (`CircleShape`), zemin `color.copy(alpha = if (Theme.c.dark) 0.22f else 0.14f).compositeOver(Theme.c.surface)`, ortada `DuoIcon`.
- Ad `duotone.json`'da yoksa eski `LineIcon` çizilir (hiçbir şey kaybolmasın).

**iOS** (`LineIcons.swift` yanına):
- `enum DuoIcons`: `Bundle.main` `duotone.json` (`[String: [[String: Any]]]`), `LineIcons.parse(d).0`, önbellekli; `has(_:)`.
- `struct DuoIcon: View { name; size; color }`: `Canvas` + `ctx.scaleBy(k, k)`, `ctx.fill(path, with: .color(color.opacity(o)))`
  (varsayılan `FillStyle`, nonzero), `.frame(width: size, height: size)`, `.accessibilityHidden(true)`.
- `struct DuoChip: View { name; color; box = 32; glyph = 19 }`: `ZStack { Circle().fill(Theme.surface); Circle().fill(color.opacity(dark ? 0.22 : 0.14)) }`
  (iOS 17 hedefi; `Color.mix` iOS 18'de), ortada `DuoIcon`.
- Ad yoksa eski `LineIcon` çizilir.

**Ölçüler** (pt/dp):

| Yer | Kutu (daire) | Glif |
|---|---|---|
| Kare (Tümü karesi, ibadet karesi, ilk sayfa ızgarası) | 42 | 25 |
| Satır (ayar, izin, bildirim, özellik, arama sonucu, özet, kart başlığı, ibadet satırı, saat ekranı düzenleyicisi) | 32 | 19 |
| Alt çubuk ve önizlemesi | yok | 24 |
| Tümü tanıtımındaki küçük telefon (`tip:all`) | 34 | 20 |
| Satır içi bağlantı ("Nasıl çalışır?", "Takvimden…") | yok | 18–20, `accent` |
| Ruh hâli seçici / geçmiş | yok | 34 / yazı boyu (18, 24) |
| Pomodoro turları (`RoundDots`) | yok | 26 (süren) / 22 |

- **Renk**: glif ve arka katman aynı renk (aşağıdaki tablolar); koyu temada belirteçler zaten açık tonludur (`Theme.c.terra` vb.).
- **Seçili değil / soluk**: eski `dim` (gri + %45) yerine `muted` renk. Alt çubukta seçili sekme `accent`, diğerleri `muted`; seçili
  sekmenin arkasındaki `accentSoft` hap (56 × 30) kalır. Alt çubukta daire yok.
- Eski çakıl taşı kutu (`IconBox`, 36, `PebbleShape`) iki tonlu simgelerde **daire** olur. `IconBox`'ın yerine `DuoChip` (ya da
  `IconBox` içi `DuoChip`'e çevrilir); karar platform ajanının.
- `AllPrefs.line` (Android) ve `LineIconMap.entry` / `LineIconMap.of` (iOS ve Android) şu an adı `Icons3D.res(it) != 0 ||
  LineIcons.get(it).isNotEmpty()` ile süzüyor: **`|| DuoIcons.has(it)` eklenmeli**, yoksa yalnız iki tonlu olan adlar (`guide`,
  `language`, `privacy`…) düşer.
- Çağrı yerlerinde adı **açıkça** verin (Material/SF sembolünden eşlemeye güvenmeyin; aynı sembol farklı anlamlarda kullanılıyor).

## Renk kuralları

Renkler doğa belirteçleri: `terra`, `sage`, `sky`, `gold`, `dusk` (Android `Theme.c.*`, iOS `Theme.*`).

**Konu → renk** (Tümü kategorileriyle aynı; `AllCat.color()` / `AllCat.color`):

| Tümü kategorisi | Renk | Sekme |
|---|---|---|
| Süre ve alarm (`time`) | terra | Saat |
| Gün ve plan (`plan`) | sage | Takvim |
| Gök ve dünya (`sky`) | sky | Hava, Dünya, Mevsimler |
| İbadet (`faith`) | gold | Vakitler |
| Diğer (`more`; ayar arama girdileri `s:*`, Ayarlar, Rozetler) | dusk | Tümü |

Sekme rengi (Özellikler satırları, ilk kurulum özeti): Saat terra, Hava sky, Takvim sage, Dünya sky, Mevsimler sky, Vakitler gold,
Tümü dusk.

**Ayar grupları** (Ayarlar ekranı; grup adları kodda doğrulandı):

| Grup (anahtar) | Renk |
|---|---|
| Genel (`settings.color.general`): Özellikler, Konum, Bildirimler, İzinler, Dil, ekran açık; iOS'ta birim, basınç, 12/24 | sage |
| Renk ve görünüm (`settings.group.look`) | dusk |
| Saat (`tab.clock`) | terra |
| Yedek ve kısayollar (`settings.group.backup`) | sky |
| Destek ve paylaşım (`support.title`) ve altındaki Rozetler/Hakkında/Tanıtım kartı | gold |

**Diğer sayfalar**:
- İzinler sayfası ve ilk kurulumdaki konum/bildirim/güven satırları: hepsi **sage**.
- Bildirim ayarı satırları ve saat ekranı düzenleyicisi: **konu → renk** (vakit/Cuma gold; takvim, günün sözü, tarih, hafta, hicri,
  yaklaşan gün sage; uyku, alışkanlık, saat başı, yaz saati, pil, alarm, sıradaki vakte kalan süre terra… aşağıdaki tablolarda tek tek).
- Vakitler'deki dört ibadet karesi ve İbadet sayfası satırları: **gold**.
- İlk sayfa 3 × 3 ızgara: Saat terra, Takvim sage, Hava durumu sky / Namaz vakitleri gold, Alarm terra, Dünya saatleri sky /
  Kıble gold, Ay ve gökyüzü dusk, Pomodoro terra.
- Ruh hâli: 1 terra, 2 gold, 3 dusk, 4 sky, 5 sage; bir yüz seçiliyse diğerleri `muted` (bugünkü %35 soluklaştırmanın yerine).
- Satır içi bağlantı simgeleri: `accent` (bağlantının yazı rengi).

## Glif tablosu (ad → Phosphor)

Ad, uygulamadaki simge adıdır (3D adlarıyla aynı; son 20'si yalnız iki tonlu). Kaynak: `duotone-map.json`.

| | | | |
|---|---|---|---|
| `sun` → sun | `moon` → moon | `cloud` → cloud | `rain` → cloud-rain |
| `snow` → cloud-snow | `storm` → cloud-lightning | `wind` → wind | `fog` → cloud-fog |
| `sun_small_cloud` → cloud-sun | `sun_cloud` → cloud-sun | `sun_rain` → cloud-rain | `leaf` → leaf |
| `flower` → flower-tulip | `snowman` → snowflake | `beach` → island | `sunrise` → sun-horizon |
| `sunset` → sun-horizon | `star` → star | `globe` → globe-hemisphere-east | `flashlight` → flashlight |
| `calendar` → calendar-dots | `thermometer` → thermometer | `mood1` → smiley-sad | `mood2` → smiley-nervous |
| `mood3` → smiley-meh | `mood4` → smiley | `mood5` → smiley-wink | `clock` → clock |
| `grid` → squares-four | `mosque` → mosque | `kaaba` → compass | `alarm` → alarm |
| `hourglass` → hourglass-high | `stopwatch` → timer | `tomato` → orange | `book` → book-open |
| `bed` → bed | `chick` → egg-crack | `knight` → horse | `metronome` → metronome |
| `speaker` → speaker-high | `paper` → exam | `people` → users-three | `calc` → calculator |
| `leafcal` → notepad | `bridge` → bridge | `compass` → compass | `beads` → hand-tap |
| `checklist` → list-checks | `coin` → hand-coins | `hands` → hands-praying | `gear` → gear |
| `bell` → bell | `pin` → map-pin | `palette` → palette | `textsize` → text-aa |
| `info` → info | `share` → export | `qr` → qr-code | `mail` → envelope-simple |
| `sliders` → sliders-horizontal | `cap` → graduation-cap | `fullscreen` → corners-out | `dawn` → sun-horizon |
| `afternoon` → sun-dim | `noon` → sun | `night` → moon-stars | `heart` → heart |
| `plus` → plus-circle | `search` → magnifying-glass | `books` → books | `scroll` → scroll |
| `sparkles` → sparkle | `medal` → medal | `telescope` → planet | `milkyway` → shooting-star |
| `school` → chalkboard-teacher | `date` → calendar-plus | `star_crescent` → star-and-crescent | `love_letter` → envelope-open |
| `notebook` → notebook | `ruler` → ruler | `speech` → quotes | `memo` → note-pencil |
| `clipboard` → clipboard-text | `chart` → chart-bar | `halfmoon` → circle-half | `gift` → gift |
| `numbers` → hash | `coffee` → coffee | `party` → confetti | `privacy` → shield-check |
| `language` → translate | `backup` → cloud-arrow-up | `shortcut` → lightning | `wand` → magic-wand |
| `camera` → camera | `mic` → microphone | `waveform` → waveform | `music` → music-notes |
| `overlay` → stack | `battery` → battery-high | `umbrella` → umbrella | `dst` → clock-clockwise |
| `gauge` → gauge | `guide` → book-open-user | `dial` → watch | `features` → toggle-right |
| `widgets` → layout | `homeitems` → list-dashes | `idea` → lightbulb | |

Phosphor'da Kâbe, teleskop, tesbih, domates, burç glifi yok: `kaaba` → pusula, `telescope` → gezegen, `beads` → dokunan el
(zikirmatik), `tomato` → portakal (domatese benzer yuvarlak meyve), `milkyway` (burç) → kayan yıldız. Dua (`hands`) için açık avuç
glifi yok; birleşik eller (hands-praying) seçildi.

## Envanter: Android (`mobile/android/app/src/main/kotlin/tr/bumba/time/`)

Satır numaraları 8 Kasım 2026, commit öncesi. "Ad" sütunu iki tonlu için kullanılacak adtır; değişen adlar **kalın**.

### İki tonlu olacaklar

| Dosya:satır | Bugün | Ad | Renk | Ölçü |
|---|---|---|---|---|
| MainActivity.kt:322 | alt çubuk `LineIcon(LineIconMap.tab(t), 26, dim)` | `LineIconMap.tab(t)` (clock, sun_cloud, calendar, globe, leaf, mosque, grid) | seçili `accent`, diğer `muted` | 24, daire yok (hap kalır) |
| Features.kt:207 | `BarPreview` 3D | `LineIconMap.tab(t)` | ilk (Saat) `accent`, diğerleri `muted` | 24 |
| Features.kt:181 | `FeatureRow` Material `Icon(t.on)` | `LineIconMap.tab(t)` | sekme rengi | satır 32/19 |
| Onboarding.kt:427 | özet satırı `LineIcon(tab, 28)` | `LineIconMap.tab(t)` | sekme rengi | satır |
| AllScreen.kt:379 | Tümü karesi `IconBox(e.icon, cat.color, line)` | `AllPrefs.line(e.id)` | `e.cat.color()` | kare 42/25 (yıldız rozeti kalır) |
| AllScreen.kt:308 | arama sonucu `IconBox` | `AllPrefs.line(e.id)` | `e.cat.color()` | satır |
| AllScreen.kt:182–193 | `AllPrefs.line` tablosu | `"guide"` → **`guide`** (bugün kaaba); ayar girdileri için ekle: appearance→`clock`, color→`palette`, dials→**`dial`**, widgets→**`widgets`**, homeItems→**`homeitems`**, features→**`features`**, notifications→`bell`, language→**`language`**, about→`info`, shareQr→`qr`; süzgece `DuoIcons.has` | | |
| Onboarding.kt:385 | ilk sayfa ızgarası `LineIcon(icon, 34)` (liste 344–348) | aynı adlar (clock, calendar, sun_cloud, mosque, alarm, globe, kaaba, telescope, tomato) | ızgara renkleri (yukarıda) | kare 42/25 |
| Onboarding.kt:442 | `TrustCard` `LineIcon("heart", 30)` | **`privacy`** | sage | satır |
| Onboarding.kt:464 | kurulum, konum `LineIcon("pin", 26)` | `pin` | sage | satır |
| Onboarding.kt:481 | kurulum, bildirim `LineIcon("bell", 26)` | `bell` | sage | satır |
| Onboarding.kt:651 | `tip:all` telefon kareleri `LineIcon(icon, 22)` (liste 632–636, renkleri var) | aynı adlar | listedeki renk | 34/20; kutu dairede |
| Permissions.kt:125 | `PermissionRow` `LineIcon(icon, 30)` | `item.icon` | sage | satır |
| Permissions.kt:58–77 | izin adları | location `pin`, locationBg `compass`, notify `bell`, exactAlarm `alarm`, fullScreen `fullscreen`, calendar `calendar`, mic **`mic`** (bugün speech), health `heart`, overlay **`overlay`** (bugün sliders), usage `chart`, dnd **`moon`** (bugün halfmoon) | | |
| SettingsScreen.kt:121 | Özellikler `SettingsRow(Apps)` | **`features`** | sage | satır |
| SettingsScreen.kt:123 | Konum `IconBox(LocationOn)` | `pin` | sage | satır |
| SettingsScreen.kt:128 | Bildirimler | `bell` | sage | satır |
| SettingsScreen.kt:130 | İzinler (`line = "checklist"`) | **`privacy`** | sage | satır |
| SettingsScreen.kt:132 | Ekran açık `IconBox(LightMode)` | `sun` | sage | satır |
| AppLanguage.kt:128 | Dil `IconBox(Language)` | **`language`** | sage | satır |
| SettingsScreen.kt:182 | Uygulama rengi `IconBox(Palette)` | `palette` | dusk | satır |
| SettingsScreen.kt:207 | Saat görünümü (TextFields) | **`clock`** | terra | satır |
| SettingsScreen.kt:208 | Saat ekranını düzenle (Tune) | **`homeitems`** | terra | satır |
| SettingsScreen.kt:216 | Yedek (Backup) | **`backup`** | sky | satır |
| SettingsScreen.kt:218 | Kısayollar (Shortcut; bugün Material) | **`shortcut`** | sky | satır |
| Support.kt:135–139 | `SupportRow` Material simge (150–155) | rate `star`, share `share`, qr `qr`, feedback `mail`, moreApps `grid` | gold | satır |
| SettingsScreen.kt:226 | Rozetler | `medal` | gold | satır |
| SettingsScreen.kt:227 | Hakkında | `info` | gold | satır |
| SettingsScreen.kt:229 | Tanıtımı yeniden göster (School) | `cap` | gold | satır |
| SettingsScreen.kt:248–253 | `SettingsRow` (`IconBox(icon, line)`) | imzaya ad + renk | | |
| NotificationsScreen.kt:93 | vakit bildirimi (NightsStay) | **`mosque`** | gold | satır |
| NotificationsScreen.kt:109 | takvim | `calendar` | sage | satır |
| NotificationsScreen.kt:111 | günün sözü (FormatQuote) | `speech` | sage | satır |
| NotificationsScreen.kt:113 | yatma (Bed; bugün Material) | `bed` | terra | satır |
| NotificationsScreen.kt:114 | alışkanlık (LocalCafe) | `coffee` | terra | satır |
| NotificationsScreen.kt:117 | mevsim (Eco) | `leaf` | sky | satır |
| NotificationsScreen.kt:119 | Ay (DarkMode) | **`moon`** | sky | satır |
| NotificationsScreen.kt:123 | Güneş (WbTwilight) | `sunrise` | sky | satır |
| NotificationsScreen.kt:126 | gökyüzü (AutoAwesome) | **`telescope`** | sky | satır |
| NotificationsScreen.kt:129 | hava (WbSunny) | **`sun_cloud`** | sky | satır |
| NotificationsScreen.kt:140 | hava uyarısı (WbSunny) | **`umbrella`** | sky | satır |
| NotificationsScreen.kt:146 | saat başı (Schedule) | `clock` | terra | satır |
| NotificationsScreen.kt:160 | yaz saati (Update; bugün Material) | **`dst`** | terra | satır |
| NotificationsScreen.kt:178–183 | `ToggleRow` (`IconBox(icon)`) | imzaya ad + renk | | |
| PrayerScreen.kt:193–196 | dört ibadet karesi `WorshipTile` (`IconBox` 210) | qibla `compass`, quran `book`, zikir `beads`, track `checklist` | gold | kare 42/25 |
| PrayerScreen.kt:224–234 | İbadet sayfası `LinkCard` (`IconBox` 356) | qibla `compass`, quran `book`, duas `hands`, esma `sparkles`, hadith **`scroll`**, guide **`guide`**, zikir `beads`, track `checklist`, reldays **`star_crescent`**, greet **`love_letter`**, zakat **`coin`** (Tümü ile aynı) | gold | satır |
| HomeItems.kt:196 | düzenleyici satırı Material `Icon(item.icon)` (enum 73–85) | date `calendar`, week `numbers`, hijri `star_crescent`, next `hourglass`, prayers `mosque`, upcoming `star`, world `globe`, weather `sun_cloud`, sun `sunrise`, globe `globe`, battery **`battery`**, alarm `alarm` | date/week/hijri/upcoming sage; next/prayers gold; world/weather/sun/globe sky; battery/alarm terra | satır |
| HomeItems.kt:177 | "Havaya göre zemin" Material Umbrella | **`umbrella`** | sky | satır |
| SeasonsScreen.kt:142 | `Info` kart başlığı `LineIcon(icon, 30)` (çağrılar 74–77: globe, flashlight, calendar, beach) | aynı | sky | satır |
| SeasonsScreen.kt:79 | "💡 " metin öneki | **`idea`** (öneki kaldır, başlığın yanında simge) | sky | satır |
| Journal.kt:88 | ruh hâli seçici `LineIcon(MOODS[v-1], 38)` | mood1…mood5 | ruh hâli renkleri | 34, daire yok |
| Journal.kt:136, 169 | geçmişte emoji metni `Journal.moods` (57) | mood1…mood5 | ruh hâli renkleri (boşsa "·" kalır) | 18 / 24 |
| ToolVisuals.kt:82 | `RoundDots` `LineIcon("tomato", dim)` | `tomato` | biten/süren terra, kalan `muted` (saydamlıklar aynı) | 26 / 22 |
| Components.kt:272 | "Nasıl çalışır?" `LineIcon("info", 18)` | `info` | accent | 18 |
| AlarmScreen.kt:137 | "Takvimden alarm" `LineIcon("calendar", 20)` | `calendar` | accent | 20 |
| AlarmScreen.kt:142 | konum hatırlatıcısı `LineIcon("pin", 20)` | `pin` | accent | 20 |
| Components.kt:93–101 | `IconBox` (çakıl taşı 36, 3D) | → `DuoChip` (daire) | | |
| LineIcons.kt:123–145 | `LineIconMap.byVector` | Material'den kalan çağrılar için ekle: `Filled.Bed`→bed, `Filled.Update`→dst, `Filled.Lock`→privacy, `Filled.Shortcut`→shortcut, `Filled.Backup`→backup, `Filled.QrCode2`→qr, `Filled.Umbrella`→umbrella; süzgece `DuoIcons.has` | | |

`ToolsScreen.kt:131` `ToolRow` hiçbir yerden çağrılmıyor (ölü kod); dokunmayın ya da silin.

### 3D kalanlar (değişmez)

| Dosya:satır | Ne |
|---|---|
| Weather.kt:415–417 `WeatherIcon` ve bütün çağıranları | hava verisi (hava ekranı, saatlik/günlük, saat modelleri, saat ekranı hava alanı, günaydın, dünya saatleri, küre…) |
| Weather.kt:99–109 `widgetIcon`, Widgets2.kt:255, WorldWidgets.kt:54 | widget'lar (hava; dünya widget'ında gündüz/alacakaranlık/gece) |
| PrayerScreen.kt:172 | altı vakit satırı (`LineIconMap.prayer`) |
| WorshipExtras.kt:619 | sıradaki vakit kartının simgesi |
| SeasonsScreen.kt:122 | büyük mevsim simgesi (76) |
| Onboarding.kt:406 | soru sayfasının büyük simgesi (104) |
| Onboarding.kt:550, 553, 555 (`IconCluster`; `TourPage` 524–532, `MorePage` 515) | tur ve "başka neler var" çizimleri |
| Components.kt:68 `EmptyState` (çağıranlar AlarmScreen.kt:173, AllScreen.kt:299, CalendarViews.kt:285, Countdowns.kt:208, GeoAlarms.kt:253, Intervals.kt:448, Journal.kt:166, SettingsScreen.kt:233) | boş durumlar |

Not: `EmptyState` simgeyi `LineIconMap.of(icon.name)` ile buluyor; `Outlined.Alarm`, `Outlined.HourglassEmpty`, `Outlined.EditNote`,
`Outlined.WhereToVote`, `Filled.DateRange` eşlemede olmadığı için bugün Material simge çiziliyor. İstenirse bunlara 3D ad verilir
(alarm, hourglass, memo, pin, calendar); iki tonluya çevrilmez.

### Android'de yapılanlar ve sapmalar (8 Kasım 2026)

- `DuoIcons`, `DuoIcon`, `DuoChip` `LineIcons.kt`'te, ölçüler yukarıdaki gibi. `IconBox` kaldırılmadı: içi `DuoChip` (daire; `box`/`glyph`
  parametreli, satır 32/19, kare 42/25); adı verilmezse Material simgesinden eşler, o da yoksa Material simgesi aynı dairede.
- Boş durumlar `LineIconMap.of3D` ile yalnız 3D görseli olan adı alır; yukarıdaki notta önerilen beş eşleme (`Outlined.Alarm` …
  `Filled.DateRange`) eklendi, artık boş durumlarda 3D çizilir.
- Envanterde olmayan ama aynı kurala giren yerler de iki tonlu: izin öncesi açıklama penceresi (`PermissionPrimer`, adaçayı 72/40),
  Tümü tanıtımındaki küçük telefonun alt çubuğu, dil listesindeki "Sistem dili" satırı (`language`).
- **Metin içindeki emoji** (sahibin "çok emoji" şikâyetiyle bu turda): yeni ortak parçalar `IconText` (simge + metin, `Components.kt`)
  ve simgeli `RoundButton(title, prominent, icon, …)`. Alarm görev öneki `AlarmTaskIcon` (matematik `calc`, sallama `waveform`, adım
  `gauge`, kod `qr`, hafıza `palette`, yazma `memo`, fotoğraf `camera`, çömelme `heart`, nesne `search`, konuşma `mic`, ışık `idea`,
  rastgele `wand`; kiremit), şehir/güneş bağlı alarm `globe`/`sun`, "🌧" yerine "Sakin ön alarm" yazısı; "📤 Paylaş" `share`;
  "🕓" bitiş `clock`; "🔒" `privacy`; "🔦" `flashlight`; "🔗"/"⏰" (geri sayım tam ekranı) `share`/`alarm`; "📅 Takvimden seç" `calendar`;
  saat ekranı Güneş sütunu `sunrise`/`sunset`/`sun`; alacakaranlık satırları (mavi saat `sunrise`/`sunset` gök mavisi, altın saat
  altın, ilk/son ışık `star`, sivil `halfmoon`, denizcilik `compass`, astronomik `telescope`; akşam moru); mevsim tablosu
  `flower`/`sun`/`leaf`/`snowman`; tutulma başlığı `sun`/`moon`; tarihte bugün (kuruluş `sparkles`, doğum `gift`, ölüm `leaf`, olay
  `scroll`); hilal görünürlüğü `sunset`/`moon`; takvim hücresinde saat değişimi `dst`; dünya saatinde Güneş `sun`; ışık görevi `sun`;
  ekran görüntüsü uyarısı `privacy`; odak engeli ekranı (🎯) `tomato` dairede; aralık antrenmanı bitişi (🏁) `medal`. Simgesiz
  bırakılan (yalnız yazı): odak serisi "🔥", yüzüstü odak "⚠️/📱", arşiv "🗄", çocuk modu "🧒". `world.labelHint`'teki 👵 18 dilde çıkarıldı.
  Hava verisi metninde emoji (`WeatherCode.icon()`) olan iki yer 3D `WeatherIcon`'a geçti (hava 3D kalır): dünya saatlerindeki şehir
  havası ve hava günü ayrıntı penceresi.
- Kalan emoji (bilerek): geri sayım emoji seçicisi ve varsayılanları (kullanıcı içeriği), Kıble 🕋, Ay evresi emojileri (takvim,
  Ay ekranı, hava), burç sembolleri, paylaşım kartı çizimleri, odak ormanı, abdest adımlarındaki resimler (`GuideFigures.WUDU`), dil
  bayrakları, widget ve bildirim metinleri, küre etiketlerindeki hava emojisi (`GlobeView`, tuvale yazı olarak çiziliyor; 3D görsele
  çevrilmesi ayrı iş); ▶ ↑ ↓ ✓ ★ ☆ ☾ ↻ ● gibi yazı işaretleri.
- Hakkında'da ikinci lisans bağlantısı `about.licensePhosphor` (18 dil); `about.license`'ın metni de "Fluent Emoji lisansı" oldu (iki
  bağlantı karışmasın diye; adres aynı).

## Envanter: iOS (`mobile/ios/App/Sources/`)

### İki tonlu olacaklar

| Dosya:satır | Bugün | Ad | Renk | Ölçü |
|---|---|---|---|---|
| SaatTarihApp.swift:500 | alt çubuk `LineIcon(LineIconMap.tab(tab), 26, dim)` | `LineIconMap.tab` | seçili `accent`, diğer `muted` | 24, hap kalır |
| SaatTarihApp.swift:456–459 | iPad kenar çubuğu `row` SF `Image(systemName:)` (çağrılar 419–446) | sekmeler `LineIconMap.tab`, girişler `LineIconMap.entry(e.id)`, Ayarlar `gear` | sekme / kategori rengi, Ayarlar dusk | 22, daire yok (önerilir) |
| Features.swift:209 | `BarPreview` 3D | `LineIconMap.tab` | ilk `accent`, diğer `muted` | 24 |
| Features.swift:166 | `barRow` SF `Label(systemImage: tab.icon)` | `LineIconMap.tab` | sekme rengi | satır |
| Features.swift:194 | özellik satırı SF `Image(systemName: tab.icon)` | `LineIconMap.tab` | sekme rengi | satır |
| Onboarding.swift:417 | özet satırı `LineIcon(tab, 28)` | `LineIconMap.tab` | sekme rengi | satır |
| AllView.swift:282 | Tümü karesi `IconBox(icon, cat.color, line: entry)` | `LineIconMap.entry(e.id)` | `e.cat.color` | kare 42/25 (yıldız kalır) |
| AllView.swift:254 | arama sonucu `IconBox` | `LineIconMap.entry(e.id)` | `e.cat.color` | satır |
| LineIcons.swift:156–171 | `LineIconMap.entry` tablosu | `"guide"` → **`guide`**; ekle (anahtarlar `s:` öneksiz): appearance `clock`, color `palette`, dials **`dial`**, widgets **`widgets`**, homeItems **`homeitems`**, features **`features`**, notifications `bell`, language **`language`**, about `info`, shareQr `qr`; süzgece (169) `DuoIcons.has` | | |
| Onboarding.swift:362 | ilk sayfa ızgarası `LineIcon(f.icon, 34)` (liste 167–176) | aynı adlar | ızgara renkleri | kare 42/25 |
| Onboarding.swift:557 | `TrustCard` (553) `LineIcon("heart", 30)` | **`privacy`** | sage | satır |
| Onboarding.swift:442 | kurulum, konum `LineIcon("pin", 26)` | `pin` | sage | satır |
| Onboarding.swift:459 | kurulum, bildirim `LineIcon("bell", 26)` | `bell` | sage | satır |
| Onboarding.swift:758 | `tip:all` kareleri `Icon3D(name: t.0, 22)` (liste 701–703, renkleri var) | aynı adlar | listedeki renk | 34/20, daire |
| Permissions.swift:110 | izin satırı `LineIcon(item.icon, 30)` | `item.icon` | sage | satır |
| Permissions.swift:33–60 | izin adları | location `pin`, notify `bell`, alarm `alarm`, calendar `calendar`, camera **`camera`** (bugün qr), mic **`mic`** (bugün speech), speech **`waveform`** (bugün memo), health `heart`, music **`music`** (bugün speaker), screenTime `hourglass` | | |
| InfoViews.swift:39 | Özellikler `BoxLabel(square.grid.2x2)` | **`features`** | sage | satır |
| InfoViews.swift:42 | Konum (location) | `pin` | sage | satır |
| InfoViews.swift:45 | Bildirimler | `bell` | sage | satır |
| InfoViews.swift:49 | İzinler (lock.shield) | **`privacy`** | sage | satır |
| AppLanguage.swift:95 | Dil (`LanguageRow`, globe) | **`language`** | sage | satır |
| InfoViews.swift:56 | Birimler (ruler) | `ruler` | sage | satır |
| InfoViews.swift:63 | Basınç birimi (gauge) | **`gauge`** | sage | satır |
| InfoViews.swift:69 | 12/24 (clock) | `clock` | sage | satır |
| InfoViews.swift:72 | Ekran açık (sun.max) | `sun` | sage | satır |
| InfoViews.swift:81 | Uygulama rengi | `palette` | dusk | satır |
| InfoViews.swift:92 | Görünüm açık/koyu (circle.lefthalf.filled) | `halfmoon` | dusk | satır |
| InfoViews.swift:101 | Saat görünümü (textformat.size) | **`clock`** | terra | satır |
| InfoViews.swift:105 | Saat ekranını düzenle (slider.horizontal.3) | **`homeitems`** | terra | satır |
| InfoViews.swift:114 | iCloud eşitleme (icloud) | **`backup`** | sky | satır |
| InfoViews.swift:126 | Siri ve Kısayollar (wand.and.stars) | **`wand`** | sky | satır |
| Support.swift:47–53 | `SupportSection` SF `Label` | rate `star`, share `share`, qr `qr`, feedback `mail`, moreApps `grid` (BoxLabel gibi) | gold | satır |
| InfoViews.swift:136 | Rozetler (rosette) | `medal` | gold | satır |
| InfoViews.swift:138 | Hakkında | `info` | gold | satır |
| InfoViews.swift:140 | Tanıtımı yeniden göster | `cap` | gold | satır |
| Components.swift:91–104 | `BoxLabel` (`IconBox`) | imzaya ad + renk (SF sembolü yerine) | | |
| NotificationsView.swift:39 | vakit bildirimi (moon.stars) | **`mosque`** | gold | satır |
| FeatureNotifier.swift:20–34 (`NotifyFeature.icon`, satırlar NotificationsView 45–78, `BoxLabel` 104) | calendar, seasons leaf, moon.circle, sunrise, cloud.sun, clock, clock.arrow.2.circlepath, bed.double, cup.and.saucer, building.columns, quote.bubble, sparkles | calendar `calendar` sage; seasons `leaf` sky; moon **`moon`** sky; sun `sunrise` sky; weather **`sun_cloud`** sky; hourly `clock` terra; dst **`dst`** terra; bedtime `bed` terra; habits `coffee` terra; friday **`mosque`** gold; daily `speech` sage; sky **`telescope`** sky | | satır |
| Religion.swift:1063–1066 | dört ibadet karesi (`IconBox` 1074; qibla ve track bugün SF) | qibla `compass`, quran `book`, zikir `beads`, track `checklist` | gold | kare 42/25 |
| Religion.swift:1093–1103 | İbadet sayfası satırları (`BoxLabel` 1118) | qibla `compass`, quran `book`, duas `hands`, esma `sparkles`, hadith **`scroll`**, guide **`guide`**, zikir `beads`, track `checklist`, reldays **`star_crescent`**, greet **`love_letter`**, zakat **`coin`** | gold | satır |
| HomeItems.swift:87 | düzenleyici SF `Label(systemImage: item.icon)` (sembol tablosu 14–28) | Android ile aynı adlar ve renkler (date `calendar` … battery `battery`, alarm `alarm`) | konu rengi | satır |
| HomeItems.swift:73 | "Havaya göre zemin" (cloud.rain) | **`umbrella`** | sky | satır |
| WorldSeasons.swift:360 | kart başlığı `LineIcon(name: emoji, 30)` (çağrılar 257, 266, 270, 274, 278) | globe, flashlight, calendar, beach; 278'deki "💡" → **`idea`** | sky | satır |
| Journal.swift:53 | ruh hâli seçici `LineIcon(moods, 38)` | mood1…mood5 | ruh hâli renkleri | 34 |
| Journal.swift:139, 181 | geçmişte emoji metni `Journal.moods` (20) | mood1…mood5 | ruh hâli renkleri | yazı boyu |
| ToolVisuals.swift:73 | `RoundDots` domates | `tomato` | biten/süren terra, kalan `muted` | 26 / 22 |
| Components.swift:184 | "Nasıl çalışır?" `LineIcon("info", 18)` | `info` | accent | 18 |
| ToolsTab.swift:363 | "Takvimden alarm" `LineIcon("calendar", 20)` | `calendar` | accent | 20 |
| Countdowns.swift:294 | "Takvimden seç" `LineIcon("calendar", 20)` | `calendar` | accent | 20 |
| Components.swift:59–87 | `IconBox` (çakıl taşı 36, 3D) | → `DuoChip` (daire) | | |
| LineIcons.swift:116–139 | `LineIconMap.bySymbol` | SF'den kalan çağrılar için ekle: `location.north.circle`→compass, `checklist`→checklist, `text.quote`→speech, `lock.shield`→privacy, `icloud`→backup, `wand.and.stars`→wand, `gauge`→gauge, `number`→numbers, `battery.75percent`→battery, `cloud.rain`→umbrella, `clock.arrow.2.circlepath`→dst, `building.columns`→mosque, `moon.circle`→moon; süzgece `DuoIcons.has` | | |

### 3D kalanlar (değişmez)

| Dosya:satır | Ne |
|---|---|
| WeatherView.swift:288, WeatherExtras.swift:133, 164, 314, CalendarTab.swift:593, HomeItems.swift:338, ClockModels.swift:448, 474, WorldExtras.swift:66, Morning.swift:54, ShareCards.swift:139, GlobeView.swift:615 | hava verisi (`WeatherIcon3D`, küre etiketleri) |
| `mobile/ios/Widget/Sources/`LiveActivityWidget.swift:117, WorldWidgets.swift:99 | Dynamic Island / kilit ekranı havası, dünya widget'ında gündüz/alacakaranlık/gece |
| PrayerTab.swift:113 | altı vakit satırı (`prayerLineIcon`, WorshipExtras.swift:302) |
| WorshipExtras.swift:366 | sıradaki vakit kartının simgesi |
| WorldSeasons.swift:321 | büyük mevsim simgesi |
| Onboarding.swift:530 | soru sayfasının büyük simgesi |
| Onboarding.swift:492, 496, 500 (`OnboardFeature`; turlar 329–334, "başka neler var" 314) | tur çizimleri |
| Components.swift:22–23 `EmptyState` (çağıranlar AllView.swift:238, CalendarViews.swift:284, Countdowns.swift:147, GeoAlarms.swift:124, Intervals.swift:442, Journal.swift:134, ToolsTab.swift:372) | boş durumlar |

## Lisans

- `PHOSPHOR-LICENSE.txt` depoda. `about.credits` 18 dilde "Microsoft Fluent Emoji ve Phosphor Icons (MIT)" diyor (8 Kasım 2026).
- `about.license` bağlantısı ("Simgelerin lisans metni") kodda tek adrese gidiyor: Fluent'in LICENSE'ı (Android
  `SettingsScreen.kt:399`, iOS `InfoViews.swift:369`). Phosphor için ikinci bağlantı yeni metin anahtarı ve kod ister; bu turda
  eklenmedi. Eklenirse adres: https://github.com/phosphor-icons/core/blob/main/LICENSE

## Bu turun dışında kalanlar

- Saat (Watch/Wear) komplikasyonları ve widget'lardaki SF Symbol / Material simgeler (saat yüzü renklendirir).
- Düğme, ok, kapat, ekle, arama kutusu gibi denetim simgeleri (Material / SF) aynen kalır.
- **Metin içindeki emoji** (simge değil, yazı karakteri). Sahibin "çok emoji" şikâyetine girebilir; **sahibe sorulmadan
  değiştirilmez**. Başlıcaları: alarm görev önekleri (Android AlarmScreen.kt:194–195, iOS ToolsTab.swift:477–484), "📤 Paylaş"
  (FocusSounds.kt:346, MoonScreen.kt:274, WeatherScreen.kt:89), "🕓" bitiş saati (Intervals.kt:475, 814; iOS Intervals.swift:594,
  886), "🔒" (Intervals.kt:799, FocusExtras.kt:132; iOS Intervals.swift:870), dil listesindeki "🌐" (AppLanguage.kt:99, iOS
  AppLanguage.swift:52), saat ekranı Güneş sütunu (HomeItems.kt:439–441), alacakaranlık satırları (SeasonsScreen.kt:191–196, iOS
  WorldSeasons.swift:470–475), mevsim bilgileri (iOS WorldSeasons.swift:281), geri sayım emoji seçici (Countdowns.kt:113, iOS
  Countdowns.swift:29; kullanıcının seçtiği içerik), odak ormanı (FocusExtras.kt:315, iOS FocusExtras.swift:299). Kıble pusulasındaki
  🕋, Ay evresi emojileri, burç sembolleri ve paylaşım kartı görselleri çizimdir; kalır.
