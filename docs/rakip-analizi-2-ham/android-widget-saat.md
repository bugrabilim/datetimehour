# Google Play, widget/StandBy ve saat kadranları (ikinci dalga, 3 Kasım 2026)

Ajan taraması. Görüntüler yalnız oturum klasöründe (depoya girmedi). Play sayfaları `play.google.com/store/apps/details?id=<paket>&hl=tr&gl=TR`, veri güvenliği `.../datasafety?id=<paket>`. Play TR sayfası uygulama içi satın alma fiyatlarını göstermez (yalnız "Uygulama içi satın alma" etiketi); fiyat yazılmayan yerde "Play'de görülmedi" denir.

**Bizim karşılıklar (koddan, 3 Kasım 2026):**
- iOS widget'ları (`mobile/ios/Widget/Sources/`): analog (küçük, büyük), dijital (küçük), saat+tarih (küçük, orta), saniyeli (küçük), dünya (küçük, orta), geri sayım (küçük), Ay (küçük), hicri (küçük + kilit ekranı satır/dikdörtgen), sıradaki vakit (küçük + kilit ekranı satır/dikdörtgen), bugünün vakitleri (orta), zikirmatik ve kronometre (küçük, dokunulabilir), Denetim Merkezi'nde üç düğme. Kilit ekranı dairesel (`accessoryCircular`) yok; `systemExtraLarge` yok; `widgetAccentable`/`widgetRenderingMode` (iOS 18 renklendirilmiş ana ekran) hiçbir widget'ta yok (`grep -rn widgetAccentable mobile/ios/Widget` boş).
- Android widget'ları (`Widgets.kt`, `ToolWidgets.kt`, `res/layout/widget_*.xml`): 12 widget; hepsi tek görünüm: açık temada kâğıt (#F7F4EE) kart, koyuda #11141A (`values-night/colors.xml` `widget_bg`); saydamlık, renk, yazı tipi seçimi yok; Material You dinamik renk yok; yalnız iki vakit widget'ında yer ayarı (`widget_next_info.xml`, `widget_today_info.xml` `widgetFeatures="reconfigurable|configuration_optional"`); hepsi `widgetCategory="home_screen"` (kilit ekranı/`keyguard` yok).
- Uygulama teması: Android'de dinamik renk (Material You) yok, 24 elle seçilen tema var (`Theme.kt` `AccentPalette`; `dynamicDarkColorScheme` hiçbir yerde yok). Tema simgesi (Android 13 monochrome) var: `res/mipmap-anydpi-v26/ic_launcher.xml` `<monochrome>`. iOS simgesinde koyu ve renklendirilmiş (tinted) sürüm yok: `App/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json` tek 1024 görsel.
- Saatler: Apple Watch (`mobile/ios/Watch`: saat, tarih, hicri, sıradaki vakit, bugünün vakitleri) + komplikasyonlar (`WatchWidget`: sıradaki vakit, hicri; dairesel/dikdörtgen/satır/köşe); Wear OS (`mobile/android/wear`: aynı ekran, `PrayerTileService` tek kutucuk, iki komplikasyon); Wear OS kadranı (`mobile/android/watchface`: tek rakamlı analog WFF kadranı, iki SHORT_TEXT komplikasyon yeri, `UserConfigurations` yok). İki saatte de alarm, zamanlayıcı, kronometre, zikirmatik **yok** (`grep -l "Timer\|Zikir" mobile/ios/Watch/Sources mobile/android/wear/src` boş).
- Android'e özgü: yüzen saat (`FloatingClock.kt`), ekran koruyucu (`ClockDreamService`), Hızlı Ayarlar kutucukları (`ToolWidgets.kt`), Android TV (`TvActivity.kt`). Live Updates (Android 16 `ProgressStyle`/`setRequestPromotedOngoing`) yok: `grep -rn "ProgressStyle\|RequestPromotedOngoing" mobile/android/app/src` boş; süren sayaçlar sıradan kalıcı bildirim (`LiveNotifications.kt`).

## Google Saat / Clock (Google Play com.google.android.deskclock, 4,0 / 802.354 puan, 1 Mr+ indirme; Google LLC)
- Ücret modeli: ücretsiz, reklamsız, satın alma yok (sayfada "Reklam içerir" ve "Uygulama içi satın alma" etiketi yok).
- Veri güvenliği: "Üçüncü taraflarla veri paylaşımı yok"; toplanan: cihaz kimliği (analiz), e-posta (isteğe bağlı, analiz), kilitlenme/teşhis (isteğe bağlı), uygulama etkileşimleri ve "kullanıcı tarafından oluşturulan diğer içerikler" (isteğe bağlı); "Veriler silinemiyor"; "Bağımsız güvenlik değerlendirmesi" rozeti.
- Sürüm notu: "Material 3 Expressive tasarımına güncellendi".
- İncelenen görüntü: 13 Play (6 telefon ilk dalgada incelendi, `alarm.md`; bu taramada 7 kare **Wear OS** görüntüsü açıldı).
- Wear OS uygulaması (7 kare görüntü, saf siyah zemin #000000):
  - Çalan alarm: "Alarm / 08:15", altta iki yuvarlak düğme: sarı erteleme simgesi (koyu gri daire) ve "×" kapat; zeminde aşağıdan mor-kahve gradyan ışıma.
  - Sıradaki alarm kutucuğu: üstte "4 h 23 m" (kalan süre), ortada **sarı-krem gradyan büyük hap** içinde "08:30", altında sarı "Mon, Tue, Wed".
  - Alarm listesi: koyu gri hap kartlar ("8:15 AM / Tue, Wed, Sat…" ve sarı-yeşil açma anahtarı), altta krem yuvarlak "+".
  - Zamanlayıcı hazırları: koyu yeşil daireler "05, 10, 15, 20, 30" (bal peteği dizilim) + camgöbeği "New" hapı.
  - Çalışan zamanlayıcı: kenar boyunca camgöbeği ilerleme yayı (#6?C?E?), "00 : 31" ince rakamlar, krem yuvarlak ▶, üstte ↻ ve ×.
  - Saat seçici: "Hour" başlığı, dikey tekerlek "11 / 12 / 13" camgöbeği, krem ✓ düğmesi.
  - Kronometre: somon kenar yayı, "00 : 15 . 31" (saniye ve salise somon), koyu ‖ düğmesi.
- Tasarım: Wear Material 3; kenar yayı ilerleme, büyük haplar, sarı/camgöbeği/somon üç vurgu; ince geniş rakamlar (Google Sans Flex benzeri).
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - Saatte alarm listesi, zamanlayıcı (hazır süreler), kronometre ve "sıradaki alarm" kutucuğu — **bizde yok** (Wear OS uygulamamız yalnız saat + vakitler: `mobile/android/wear/src/main/kotlin/tr/bumba/time/wear/WearActivity.kt`, tek kutucuk `PrayerTileService`; Apple Watch aynı: `mobile/ios/Watch/Sources/WatchApp.swift`) — O (Wear) / O (watchOS) — Google'da ücretsiz.
  - Saatte çalan alarmı erteleme/kapatma — **kısmen** (telefon bildirimi saate yansıyor; CLAUDE.md "Bilekten erteleme: telefonun alarm bildirimleri saate yansır"; saatin kendi alarm ekranı yok) — K — ücretsiz.
  - Kenar boyunca ilerleme yayı (zamanlayıcı, kronometre) — bizim saat ekranında sayaç düz metin (`wear-saat.png`: "00:39:00" turuncu, yay yok) — K — ücretsiz.

## Samsung Saat / Clock (Google Play com.sec.android.app.clockpackage, 4,3 / 352.901 puan, 5 Mr+ indirme; Samsung Electronics) — Galaxy Store sayfası yerine Play sayfası okundu
- Ücret modeli: ücretsiz, reklamsız, satın alma yok.
- Veri güvenliği: "Geliştirici, bu uygulamanın hiçbir kullanıcı verisini toplamadığını veya paylaşmadığını söylüyor" ("Veri toplanamadı"). Bizim hedefimizle aynı etiket.
- İncelenen görüntü: 12 Play (4 telefon, 8 tablet/katlanır; hepsi açıldı).
- Açıklamadaki işlevler: alarma **tarih atama**, tekrarlayan alarmda **bir günü atlayıp sonra kendiliğinden açılma**, dünya saatinde **şehir başına hava durumu** ve dünya haritasında konum, kronometre **tur değerini kopyalama**, zamanlayıcıda **hazır zamanlayıcılar** ve aynı anda birden çok zamanlayıcı; izinler: "Fotoğraflar ve videolar: **Alarm arka planları için resim ve video** seçmek (Android 14+)", "Takvim: **Hava Durumu alarmı arka planı** kullanılırken yaklaşan bir etkinliği göstermek".
- Ana ekran ve gezinme: One UI düzeni: ekranın üst üçte biri **büyük başlık alanı** ("Alarm in 8 hours / 6:30 AM, Fri, 1 Dec"; dünya saatinde "10:30:06 AM / Korea Standard time"), içerik başparmak bölgesinde; alt çubuk 4 sekme (Alarm, World clock, Stopwatch, Timer; seçili simge dolu siyah, yazı kalın); başlığın altında sağda "+" ve "⋮".
- Öne çıkan ekranlar:
  - Alarm kartları: beyazımsı (#FCFCFF) kart, köşe ~24 px, büyük ince "6:30 AM" (AM küçük), kartın üstünde küçük etiket ("Wake up", "Stretch", "Medicine"), sağda tarih "Fri, 1 Dec" (mavi-mor) ya da gün harfleri "S M T W T F S" (seçili günler mor, altında nokta), mor (#6?6?F?) açma anahtarı; kapalı alarm gri soluk.
  - Dünya saati kartı: şehir, "13 hours behind", büyük saat, **sağda hava simgesi + derece** (bulut 17°, güneş 23°).
  - Kronometre: "00 : 13 . 24" büyük, altında tur süresi "00:02.00" gri; tur tablosu (Lap / Lap time / Overall time): **en yavaş tur kırmızı ↓, en hızlı tur mavi ↑**; "Lap" (gri hap) ve "Stop" (kırmızı #DB332A hap); sağ üstte kopyala simgesi.
  - Zamanlayıcı: mor ilerleme halkası, içinde "30 m" (toplam), "22 : 26" büyük, altında **"🔔 1:15 pm" (bitiş saati)**; "Delete" (gri) ve "Pause" (kırmızı) hapları; sağ üstte hazır zamanlayıcı listesi simgesi.
  - Tablet/katlanır: alarmlar **iki sütunlu ızgara**, dünya saatleri iki sütun kart.
- Ayarlar: görülmedi.
- Widget / kilit ekranı / saat: Play görüntülerinde widget yok (One UI saat widget'ları sistemde; görülmedi). Açıklamada Galaxy Watch (Wear OS) desteği.
- Tasarım: zemin #F1F1F3 (açık gri), kart #FCFCFF, vurgu mor-mavi, uyarı kırmızısı #DB332A; Samsung One yazı tipi (yuvarlak hatlı grotesk, ince saat rakamları); çok geniş beyaz boşluk.
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - Alarm ekranı arka planı olarak kullanıcının fotoğrafı/videosu; hava durumu arka planı + takvimdeki yaklaşan etkinlik — **bizde yok** (alarm ekranı düz: `AlarmActivity.kt`; tam ekran saatte fotoğraf var ama alarmda yok) — O — Samsung'da ücretsiz. Mevcut 2. analiz #2'ye (alarm duvar kâğıdı) ek ayrıntı: Samsung kategori yerine **kişinin kendi fotoğrafı/videosu** ve **hava zemini** sunuyor.
  - Dünya saati kartında şehir başına hava — **kısmen** (küre etiketlerinde hava var: `GlobeView.kt`/`GlobeView.swift` `globeWx`; dünya saatleri listesinde yok: `WorldScreen.kt`) — K — ücretsiz.
  - Zamanlayıcıda bitiş saati ("1:15 pm") — **uygulamada yok** (sitede var: `data-cd-endsat`; `app-strings.json`'da karşılığı yok) — K — ücretsiz.
  - Kronometrede en hızlı/en yavaş tur işareti ve tur kopyalama — **yok** (turlar yalnız CSV paylaşımıyla: CLAUDE.md #28) — K — ücretsiz. (İlk dalga Apple Saat'te de gördü, listeye alınmadı.)
  - Tablet/katlanırda iki sütunlu alarm ve dünya saati ızgarası — **yok** (iPad'de sayfa 900 pt ortalanır: `RootView.readable`; Android'de tablet düzeni yok) — K — ücretsiz.

## Ezan Vakti Pro: Namaz, Kuran (Google Play com.mobilexsoft.ezanvakti, 4,6 / 1.418.373 puan, 10 Mn+ indirme; **Deen Studios LLC-FZ**) — Play'deki Ezan Vakti Pro, iOS'taki aynı adlı uygulamadan (id1372054007, Muhammed Tanrıverdi; `namaz.md`) **farklı yayıncı**; Deen Studios iOS'taki Azan Time Pro'nun yayıncısı
- Ücret modeli: reklamlı + uygulama içi satın alma (fiyat Play'de görülmedi).
- Veri güvenliği: paylaşılan: tam konum (uygulama işlevselliği, kişiselleştirme); "Veri toplanamadı"; silme talebi var.
- İncelenen görüntü: 15 (8 telefon, 5 Android TV, 2 Wear OS; hepsi açıldı).
- Ana ekran / vakit görünümleri ("Birden çok vakit gösterim şekli"): (1) siyah zeminde yeşil→sarı→kırmızı **gradyan yay gösterge** (İkindi 16:40 → 19:22, üstünde güneş simgesi), altta "VAKTİN ÇIKMASINA 01:01:26" büyük; (2) lacivert tema, dev "00:56:18" ve "Vaktin Çıkmasına"; (3) **yeşil deri dokulu** tema, 45° döndürülmüş beyaz vakit kartları; köşede kırmızı **"Kerahate 15 dk"** rozeti. Mevcut 2. analiz #65'e ek ayrıntı (gradyan yay ve kerahat rozeti).
- Diğer ekranlar: Kur'an (oklu takip, 19 hafız), Kütüphane (kitap kapakları ızgarası), Multimedia (video, ayet, hadis, duvar kâğıtları, Hikmetname), Esmaü'l-Hüsna (renkli hat kartları), Menü (ikon ızgarası: Kur'an, Kütüphane, Aşr-ı Şerifler, Multimedya, Tebrik Kartı, Ana Sayfa, Vaktinde Kıl, Zikirmatik, Yakın Camiler, Hatim, Kazalar, Ajanda).
- Android TV (5 görüntü): büyük "00:17:37" geri sayım, altta vakit satırı ve ikon ızgarası; Kur'an okuma, Kur'an Radyo, Dua Kardeşliği, Hatim.
- **Wear OS** (2 kare görüntü, siyah #000000): üstte saat "1:49", turuncu şehir adı "Fort McCoy", altı vakit listesi (geçen vakit gri, sıradaki turuncu ve koyu gri (#202020) hap içinde), altta **kenar boyunca kavisli turuncu yazı "4 hours 37 mins"**. Açıklama: "Bu uygulamada Wear OS komplikasyonlar bulunur".
- Hatalar: Wear görüntüsünde vakitler yanlış saat diliminde (Fort McCoy için "Fajr 10:51, Dhuhr 18:27, Maghrib 00:56" — öğle akşamdan sonra geliyor gibi; görüntü saat dilimi kaymış bir emülatörden).
- Bizde olmayan / farklı:
  - Saat ekranında kavisli (kenara oturan) kalan süre yazısı — bizde düz metin (`wear-saat.png`) — K — ücretsiz.
  - Namazdayım (aramayı reddet / SMS gönder) — Android'e özgü; **kısmen** (vakitte sessize alma var: `PrayerSilent`; arama reddi yok) — O — Play kısıtlı izin (`READ_CALL_LOG`/`ANSWER_PHONE_CALLS`), önerilmez.

## Ezan360 - Ezan Vakti (Google Play com.ezanvakti.namazvakitleri, 4,7 / 60.612 puan, 1 Mn+ indirme; "Ezan Vakitleri") — Play'de çok indirilen Türk vakit uygulaması
- Ücret modeli: reklamlı + uygulama içi satın alma ("Premium üye olarak reklamsız kullanıp…"; fiyat Play'de görülmedi).
- Veri güvenliği: paylaşılan: kilitlenme/teşhis (analiz), **cihaz kimliği (reklam veya pazarlama)**, uygulama etkileşimleri; "Veri toplanamadı"; "Veriler silinemiyor". Açıklama "Bizim tarafımızdan hiçbir bilginiz depolanmamaktadır" diyor ama reklam kimliği paylaşılıyor.
- İncelenen görüntü: 7 telefon (hepsi açıldı).
- Öne çıkan ekranlar: "Yatsının Çıkmasına 06 : 59 : 45" kartı (beyaz, gölgeli, altında sarı/koyu ilerleme çubuğu), altında 2 sütun vakit ızgarası (ince çizgi simgeler); "Vakit Köşesi" (Vaktin Hadisi, Vaktin Ayeti, Kız Erkek isimleri; her kartta paylaş); Araçlar ızgarası (Kıble Pusulası, Yakındaki Camiler, Dini Günler ve Geceler, Esma-ül Hüsna, Kur'an, Dualar, Dini Hikâye, Dini Rehber, Kâbe Canlı, Dini Radyolar, Zikirler, Tesbihat); Bildirim Ayarları (vakit zamanında ve vakitten önce ayrı listeler, her satırda "Sesi Değiştir" hapı ve "30 dk" süre); Kıble (yeşil daire pusula, üstte Kâbe küpü).
- **Widget** ("Widget seçenekleri" görüntüsü): duvar kâğıdı üstünde **yarı saydam koyu gri** (~%50 siyah) köşeli kartlar: (1) geniş 4×1: beş vakit yan yana (geçenler soluk, sıradaki beyaz kalın) ve altında **gün ilerleme çubuğu** (sarı dolu kısım + sarı yuvarlak işaret: günün hangi vaktinde olduğun); (2) 2×2 liste: beş vakit alt alta. Açıklama: "1x1 ve değer boyutlarda widget". Görüntü iPhone kalıbında (9:41 durum çubuğu) çizilmiş.
- Tasarım: yeşil (#50A55E) desenli zemin, beyaz kartlar, başlıklarda dekoratif serif ("Namaz Vakitleri", "Vakit Köşesi").
- Hatalar: geri sayımın üç sütununun altında da **"saniye saniye saniye"** yazıyor (saat/dakika/saniye olmalı); kıble ekranında "Şu an yönünüz kıbleye **dogru**" (ğ eksik) ve Türkçe arayüzde yön harfleri İngilizce (W, N, S, E).
- Bizde olmayan / farklı:
  - Saydam (duvar kâğıdını gösteren) vakit widget'ı — **yok** (`widget_bg` opak: `res/drawable/widget_bg.xml`, `values-night/colors.xml`) — K (Android'de saydamlık kaydırıcısı) — Ezan360'ta ücretsiz. Mevcut 2. analiz #76'ya (şeffaf widget, iOS) ek ayrıntı: Android'de saydamlık doğrudan yapılabilir (duvar kâğıdı kırpması gerekmez).
  - Vakit widget'ında günün ilerleme çubuğu (sıradaki vakte doğru dolan çizgi) — **yok** (`widget_today.xml` yalnız sıradaki vakti vurgular; `widget-bugun.png`) — K — ücretsiz.

## Muslim Pro: Namaz Saat, Ezan (Google Play com.bitsmedia.android.muslimpro, 4,0 / 1.918.940 puan, 100 Mn+ indirme; Bitsmedia) — iOS sürümü ilk dalgada (`namaz.md`)
- Ücret modeli: reklamlı + uygulama içi satın alma (Premium: reklamsız, çevrimdışı Kur'an, sesli kıraat, **"özelleştirilebilir namaz widget'ları"**, dersler).
- Veri güvenliği: paylaşılan: uygulama içi mesajlar, **fotoğraflar**, kilitlenme, uygulama etkileşimleri (reklam/pazarlama dahil), uygulama içi arama geçmişi, ad, e-posta, telefon numarası… (listenin en uzunu).
- İncelenen görüntü: 18 (7 telefon, 8 tablet, 3 Android TV; hepsi açıldı).
- iOS'tan farkı: telefon görüntüleri iOS ile aynı tasarım (yeşil #C3D7C7 çerçeve, koyu petrol #042321 uygulama zemini, "2/5 prayed" halkası, 5 kıble pusula tasarımı, tespih boncuğu 25/33). Play'e özgü olarak **Android TV'de Qalbox video** (film/dizi: "The Message", "Iran From Above"; koyu mor-yeşil zemin, Netflix benzeri raflar).
- Widget / saat: Play görüntülerinde widget ve Wear OS görüntüsü **görülmedi**; widget özelleştirmesi Premium'da (açıklama).
- Bizde olmayan / farklı: widget özelleştirmesinin Premium'da olması → mevcut 2. analiz #17'ye ek ayrıntı (Muslim Pro da widget görünümünü ücretli satıyor).

## Alarmy (Google Play droom.sleepIfUCan, 4,7 / 2.187.836 puan, 50 Mn+ indirme; Delightroom) — iOS sürümü ilk dalgada (`alarm.md`)
- Ücret modeli: reklamlı + uygulama içi satın alma; **7 günlük ücretsiz deneme**. Play açıklamasındaki ücretsiz/Premium ayrımı: ücretsiz: matematik, sallama (999'a kadar), fotoğraf, QR/barkod, hafıza, hızlı alarm, uyku sesleri, uyku takibi; **Premium**: uyanma kontrolü, yazma, adım, squat, çoklu görev (2–5), ekstra yüksek ses, saat hatırlatıcı, etiket hatırlatıcı. Android'e özgü: **"Kapatma önleme: alarm çalarken güç kapatılmaya çalışılırsa engeller (yalnız Samsung)"**.
- Veri güvenliği: paylaşılan: uygulama etkileşimleri, cihaz kimliği (reklam), **web tarama geçmişi**; toplanan: konum (yaklaşık/tam, isteğe bağlı), e-posta, kullanıcı kimlikleri.
- İncelenen görüntü: 8 (1 tanıtım + 7 telefon; açıldı). Parlak mavi (#32A0FF) gradyan çerçeve, uygulama zemini #1D1F26; iOS ile aynı ekranlar (yüksek sesler "Loud/Trending", görevler, "Uyanma Checks 94", horlama, uyku sesleri ızgarası, uyku vakti hatırlatması). Widget görüntüsü yok.
- Bizde olmayan / farklı: bizim görevlerimizin hepsi ücretsiz (yazma, adım dahil: `WakeTasks.kt`); Alarmy'nin Premium'daki "saat hatırlatıcı" (her dakika saati söyler) bizde var (`speak`, CLAUDE.md #20). Yeni iş yok.

## Alarm Clock Xtreme (Google Play com.alarmclock.xtreme.free, 4,4 / 1.249.629 puan, 50 Mn+; AVG Labs) — ekranları ilk dalgada (`alarm.md`)
- Bu taramada yalnız veri güvenliği ve ücret okundu: "Reklam içerir" + "Uygulama içi satın alma"; paylaşılan: kilitlenme/teşhis, **cihaz kimliği (reklam)**, **tam konum**, **yüklü uygulamalar (reklam veya pazarlama)**, uygulama etkileşimleri; toplanan: **işlem geçmişi** (finansal), yaklaşık konum. Açıklamada **"Günüm"** (uyanınca hava + takvimdeki yaklaşan etkinlikler) ve **çevrim içi radyo** alarm sesi.
- Bizde: günaydın kartı var (`MorningCard`, hava + vakit + özel gün); takvim etkinliği günaydın kartında yok (`Morning.swift`'te telefon takvimi okunmuyor) — K — ücretsiz. Çevrim içi radyo — yapılmamalı (sunucu/akış lisansı).
