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
  - Çalışan zamanlayıcı: kenar boyunca camgöbeği ilerleme yayı (#78DAED), "00 : 31" ince rakamlar, krem yuvarlak ▶, üstte ↻ ve ×.
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
  - Alarm kartları: beyazımsı (#FCFCFF) kart, köşe ~24 px, büyük ince "6:30 AM" (AM küçük), kartın üstünde küçük etiket ("Wake up", "Stretch", "Medicine"), sağda tarih "Fri, 1 Dec" (mavi-mor) ya da gün harfleri "S M T W T F S" (seçili günler mor, altında nokta), mor (#5C5CE6) açma anahtarı; kapalı alarm gri soluk.
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

## KWGT Kustom Widget Maker (Google Play org.kustom.widget, 3,8 / 51.422 puan, 10 Mn+ indirme; Kustom Industries)
- Ücret modeli: reklamlı + uygulama içi satın alma; **KWGT Pro** (ayrı anahtar paketi `org.kustom.widget.pro`): reklam kaldırma, SD karttan/harici temalardan ön ayar içe aktarma, ön ayar kurtarma; fiyat Play'de görülmedi. Tema paketleri (Play'de ayrı uygulamalar: "Achromatic KWGT", "Basic Pack" vb.).
- Veri güvenliği: paylaşılan: cihaz kimliği, uygulama etkileşimleri, kilitlenme (analiz); toplanan: ad, e-posta, kullanıcı kimliği (isteğe bağlı, hesap).
- İncelenen görüntü: 7 telefon (açıldı).
- Öne çıkan ekranlar: (1) **tipografik hafta widget'ı**: günler alt alta, aralıklı küçük harflerle ("M o n d a y"), bugün dev kalın "THURSDAY"; (2) **hafta şeridi**: "mon 01 … sun 07" çerçeveli kutular, eşit aralıklı yazı; müzik (albüm kapağı + şarkı), pil %71, adım 638, hava 30°; (3) "MONDAY" dikey dev yazı + "TIME IS = 10:46" cümle + hava cümlesi; (4) "08:10 AM / Monday 25th of May", **"Upcoming Events" takvim listesi**, hava kartı "Partly Cloudy, Wind: West 5 mph"; (5) **yazıyla tarih**: "Monday / twenty two / JUNE" ve "clear sky. 28°". Düzenleyici: Explore (paket keşfi, "Currently applied"), Library ("New widget", "Import"), "Add item": Komponent, Text, Image, Progress, Stack Group, Overlap Group.
- Tasarım: koyu lacivert/mor zemin, görüntü başına tek renk (sarı, kırmızı, lila, mor); düzenleyici koyu (#16171D), beyaz ince simgeler.
- Bizde olmayan / farklı: KWGT bir tasarım aracı; birebir yapılmaz. Alınabilecek tek fikir: **yazıyla tarih/saat ve tipografik hafta widget'ı** — bizde yok (widget'larımız rakamlı; sitedeki "yazıyla" yalnız gömme widget'ında: CLAUDE.md "EMBED_MODELS") — K — KWGT'de ücretsiz şablon. Mevcut 2. analiz #17'ye (widget düzenleyici) ek ayrıntı: "Health Connect" ile adım/uyku widget'ı ve "Tasker" entegrasyonu bu türün güç kullanıcısı ucu.

## Dijital Saat Widget'ı / Digital Clock Widget (Google Play com.maize.digitalClock, 4,5 / 211.227 puan, 10 Mn+ indirme; Maize)
- Ücret modeli: reklamlı, uygulama içi satın alma yok.
- Veri güvenliği: **"Veri toplanamadı"** ve "Üçüncü taraflarla veri paylaşımı yok" (reklamlı olmasına karşın beyan böyle).
- İncelenen görüntü: 8 telefon (açıldı).
- Widget: **duvar kâğıdı üstünde saydam** saat + tarih + **sıradaki alarm** ("Monday, August 24 ⏰ Tue 9:30 AM"); başka saat dilimindeki saat yanında **bayrak** ("Sunday, March 22 🇮🇹"); isteğe bağlı yarı saydam beyaz yuvarlak köşeli zemin; ince, serif, el yazısı (italik kırmızı), yuvarlak (neon yeşil) yazı tipleri.
- Ayarlar (Material 3, açık zemin #FFFFFF, koyu zemin #111410): **"Click the widget"** (Apps / Shortcuts sekmeleri: "Do nothing", "Bring me here", herhangi bir uygulama); **renk seçici** (HSV kare + ton çubuğu + saydamlık çubuğu + hex "#FF44AC74") ve **"Dynamic color"** anahtarı (Material You); **yazı tipi listesi** (Pro, Regular, Light, Medium, Condensed, Classic, Cursive, Casual, Mono, CAPS); **saat dilimi seçimi dünya haritasında** (nokta deseni harita, bayraklı şehir listesi "Zagreb UTC+1"); saat biçimi (5:24 / 5:24 PM / 17:24); tarih biçimi (haftanın günü / tarih / yıl onay kutuları, kısaltma, "Numbers only", gelişmiş ICU kalıbı "EEEE, MMMM d").
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - Saat widget'ında sıradaki alarm satırı — **yok** (`widget_clock_date.xml` yalnız saat + tarih; saat ekranında "sıradaki alarm" alanı var: `HomeItem.alarm`) — K — ücretsiz.
  - Widget'a dokununca ne açılacağını seçme — **yok** (her widget sabit ekrana gider: `Widgets.kt` `BaseWidget`) — K — ücretsiz.
  - Material You dinamik renk — **yok** (uygulama ve widget'lar: `Theme.kt`, `values-night/colors.xml`) — K — ücretsiz.
  - Saydam zemin, renk, yazı tipi — mevcut 2. analiz #17 ve #76'ya ek ayrıntı (Android'de bu tür uygulamada hepsi ücretsiz).

## StandBy Mode: Masa Saati (Google Play br.com.zetabit.ios_standby, 4,5 / 32.018 puan, 1 Mn+ indirme; Zetabit Tecnologia) — iOS StandBy'ın Android karşılığı
- Ücret modeli: reklamlı + uygulama içi satın alma (fiyat Play'de görülmedi).
- Veri güvenliği: paylaşılan: uygulama etkileşimleri, arama geçmişi, yaklaşık/tam konum, kullanıcı kimlikleri, kilitlenme, cihaz kimliği.
- Sürüm notu: "Alarms: Wake up to a video or your ringtone", "Screensaver: Every widget group, including Duo", "Quick Launch: … close on pick-up", "Calendar: Hide declined and cancelled events".
- İncelenen görüntü: 8 yatay (2 tanıtım kolajı + 6 özellik; açıldı). Türkçe yazılı tanıtım görselleri.
- Açıklama (Türkçe): 100+ dijital saat (tarihli, segmentli LCD, flip, neon, analog+dijital, güneş saati, Matrix, piksel sanat, **kelime saati**, Roma rakamlı, dünya saati, Pixel tarzı büyük saat); **büyük yazılı saat**: tarih, gün ve **günün bölümü** ("Wednesday / Night / 9:28 / Oct 7, 2026"); AMOLED saat; **gece modu kırmızıya boyar, programla ya da ışık sensörüyle**, gündüz/gece ayrı parlaklık; **piksel kaydırma**; **uyku zamanlayıcısı**; **şarj cihazı takılınca kendiliğinden aç** (yalnız kablosuz şarjda, yalnız Wi-Fi'da, belli saatlerde), kilit ekranı üstünde görün, şarjdan çekince kapan, **Android ekran koruyucusu** olarak çalış; şarj ekranı (pil düzeyi ve dolmaya kalan süre); **Duo**: iki/üç/dört widget yan yana (dijital saat, müzik çalar, takvim, sistem panosu, pil, hava, fotoğraf, alıntı, GIF, yapılacaklar, zamanlayıcı ve ana ekrandaki bütün uygulama widget'ları).
- Görüntüler: kolajda pastel kabarcık rakamlar ("9:38" sarı-pembe), çevirmeli "11 07 28", pembe dev "04:28", segment "18:08", el yazısı not kâğıdı zemin; "TELEFONUNUZ ARTIK BİR BAŞUCU SAATİ" (saat "9:41" pastel + müzik kartı "This Is It / Billie Jean"); "DİLEDİĞİNİZ İKİ WIDGET YAN YANA" (yeşil LCD "10 11" + fotoğraf; analog + takvim "Paz Ara 14"; müzik + ajanda; widget seçici); **"GECELERİ GÖZ YORMAZ"**: kırmızı analog kadran + altta **7 renk noktası** (kırmızı, camgöbeği, yeşil, sarı, turuncu, gri, açık yeşil: gece rengi seçimi); "MÜZİĞİNİZ ELİNİZİN ALTINDA" (200+ müzik uygulaması için albüm kapağı ve kontroller); "SEVDİĞİNİZ FOTOĞRAFLAR" (fotoğraf çerçevesi + köşede "10:30").
- Tasarım: siyah (#0B0B0B) zemin, alttan mor-kırmızı ışıma; başlıklar kalın büyük harf, vurgu kelimesi kırmızı-pembe; kablosuz şarj standındaki tablet/telefon çizimi.
- Hatalar: "Paz Ara 14" (Pazar + Aralık kısaltmaları yan yana; Türkçe çevirisi doğru ama okunması zor).
- Bizde olmayan / farklı:
  - Yatay tam ekranda saatin yanında ikinci widget (takvim, fotoğraf, sıradaki vakit, hava) — **yok** (yatayda saat ekranı doldurur: `FullscreenClock.stageHeight`, Android `Root` `autoFull`; alanlar dikeyde saatin altında) — O — StandBy'da ücretsiz/karışık.
  - Gece modu rengini seçme (7 renk) — **yok** (sabit kırmızı `#B82016`: `NightMode.swift`, `NightMode.kt`) — K — ücretsiz görünüyor. (Mevcut 2. analiz #11 "lüm gece kadranı"ndan farklı: o kadran işaretlerinin rengi.)
  - Gece modunu ışık sensörüyle açma — **yok** (yalnız Güneş'e göre `auto`: CLAUDE.md "Gece / yatak başı modu") — K (yalnız Android; iOS'ta ortam ışığı API'si yok) — ücretsiz.
  - Şarj olurken kendiliğinden açılma — **var** (Android ekran koruyucusu `ClockDreamService`; iOS Kısayollar otomasyonu `OpenFullscreenClockIntent`; Ayarlar'da not) — —.
  - Piksel kaydırma, uyku zamanlayıcısı, fotoğraf slaytı — **var** (`NightMode.shift`, `SleepTimer`, `fsSlides`).
  - Çalan müzik kartı (başka uygulamanın şarkısı) — **yok** — O (Android'de bildirim erişimi izni ister: Play'de hassas; iOS'ta başka uygulamanın çalanı okunamaz) — önerilmez, karar sahibin.
  - Şarj ekranı (pil düzeyi, dolmaya kalan süre) — **kısmen** (saat ekranı alanı "pil" var: `HomeItem.battery`; dolmaya kalan süre yok) — K.

## Color Widgets (App Store 1531594277, ABD 4,60 / 528.016 puan; Digital Charms, Inc.; sürüm 9.0.0, 15 Eylül 2026)
- Ücret modeli: ücretsiz + abonelik **Color Widgets Pro** (TR: ₺149,99, ₺179,99, ₺299,99, ₺699,99, ₺799,99, ₺899,99, ₺999,99, ₺2.499,99 kademeleri); açıklama: "3 günlük ücretsiz deneme, sonra yıllık faturalı ayda 2,50 $".
- Sürüm notu: **"New iOS 27 XL Widget Size"**, "New Highlighted Quote Widget".
- İncelenen görüntü: 8 iPhone (açıldı).
- Widget türleri (görülen): olumlama metni ("I am worthy." serif + oyuncak ayı), anımsatıcı listesi, **doğum günü geri sayımı halkalar içinde** ("6 Days / 4 Hours / 0 Mins"), **"Good Evening" birleşik widget** (hafta şeridi, son çalan şarkı, pil %80, "Sun sets at 9:54PM", "4,560 steps today"), büyük saat + gün + pil ("8:00 Sunday JUNE 14", gradyan zemin), el yazısı ay adlı takvim ("July"), **yıl ilerlemesi "This is my year." 365 noktalı ızgara "203/365"**, **saatlik hava şeridi** (siyah, 8 sütun: Now 73° … 3pm 79°), fotoğraf widget'ı, **yıl dönümü geri sayımı** ("Anniversary 💘 7 Days Left Thursday JULY 23", siyah kart), müzik (iPod tıkla-döndür çarkı), açık mavi analog saat (iOS 6 skeuomorf teması). Kilit ekranı: **satır widget'ında gün doğumu ve batımı** ("Thu 14 ☀ 5:52AM ☀ 8:35PM"), QR kod widget'ı, ay takvimi.
- Düzenleyici ("CUSTOMIZE"): üstte canlı önizleme ("8:00PM", mor gradyan), Background (kırmızı "Photo" hapı, "Change Photo" mor hap, "Photo Album"), Theme (renk daireleri: gökkuşağı, siyah, beyaz, kırmızı tonlar), Font (çip satırı: italik serif, dış hatlı, kalın), Font Size kaydırıcı "100%", "Change Widget", mor tam genişlik **"✓ Set Widget"**.
- Galeri: "Discover / Liked / #minimal / #aesthetic" hapları, tema kartlarında beğeni sayısı (65K, 1.5K), alt çubuk Themes / Widgets / Icons + arama (iOS 26 cam).
- Tasarım: tanıtımda pastel bulanık zemin, renkli etiket bantlı kalın başlıklar; düzenleyici beyaz, mor (~#968BC3, düğme) vurgu; "60,000,000 Users", "Featured by Apple" defne süsleri.
- Bizde olmayan / farklı:
  - **Yıl ilerlemesi widget'ı** (365 nokta ya da yüzde) — **yok** (yılın günü yalnız saat ekranı alanı ve hafta numarası sayfasında: `HomeItems.swift`; widget yok) — K — Color Widgets'ta ücretsiz görünüyor.
  - Saatlik hava şeridi widget'ı, gün doğumu/batımı kilit ekranı satırı — mevcut 2. analiz #15'e ek ayrıntı.
  - Yıl dönümü/doğum günü geri sayımında gün-saat-dakika halkaları — mevcut 2. analiz #73'e ek ayrıntı.
  - Düzenleyici ayrıntısı (renk daireleri, yazı tipi çipleri, boyut kaydırıcısı, fotoğraf zemini, "Set Widget") — mevcut 2. analiz #17'ye ek ayrıntı (Color Widgets'ta Pro).
  - iOS 27 "XL" widget boyutu — **yok** (en büyük `systemLarge`; `ClockWidgets.swift`) — K — doğrulanmalı (yalnız sürüm notunda görüldü).

## WidgetClub: Widget, Theme, Icon (App Store 1580284904, ABD 4,71 / 2.779 puan; Liume INC.; sürüm 4.17.40, 27 Ekim 2026)
- Ücret modeli: ücretsiz + abonelik ve tek seferlik: Premium Member 1 month ₺99,99, 1 year ₺229,99; Theme Packs ₺1.499,99–₺1.999,99; Forever Plan for Theme packs ₺2.999,99; tek tema "Shop Tier9–13" ₺799,99–₺1.299,99.
- İncelenen görüntü: 8 iPhone (açıldı).
- Görülen: tema = duvar kâğıdı + widget + simge takımı (bej/gül kurusu "estetik", siyah-neon, kırmızı, ayıcıklı); "App Icon Changer — Change icons at once, No more Shortcut app" (34 uygulama simgesini toplu değiştirme listesi); widget'lar: serif büyük tarih "Sep.3 FRI", "Feb.13 SUN", serif saat "4:23 WED, Sep.8", **"Being together 365 Days"**, **"Our anniversary 10 Days left"**, **"my birthday 124 Days left"**, "Being together Since Oct 10, 2020 359 days", takvim + fotoğraf, ay takvimi, analog saat, GIF/video widget'ı (açıklama), "Lock Screen Memo", **şarj animasyonu** (açıklama).
- Bizde olmayan / farklı:
  - "Birlikte X gün" (geçen gün sayacı) widget'ı — mevcut 2. analiz #73'e ek ayrıntı (tarih geçince ileri sayım).
  - Tema paketi/simge değiştirici — yapılmamalı (başka uygulamaların simgesini değiştirmek bizim konumuz değil) — —.

## Clock Widget with Seconds Live (App Store 1560484096, ABD 4,36 / 44 puan; Barys Bandarenko; sürüm 1.4.8, 8 Ağustos 2026) — StandBy ve saniyeli widget odaklı küçük uygulama (StandBy odaklı başka iOS uygulamaları da çok küçük: "StandBy - Widgets" 3,27 / 44 puan, son sürüm Nisan 2024)
- Ücret modeli: ücretsiz, aktif widget'larda **saydam "demo" rozeti**; **Lifetime Premium ₺499,99** rozeti kaldırır.
- İncelenen görüntü: 9 iPhone (açıldı).
- Görülen: widget galerisi **boyuta göre satırlar** ("Small clocks", "Medium clocks", "Large clocks"), dokununca **"WIDGET PREVIEW"** alt sayfası; stiller: yeşil **yedi bölütlü LCD (sönük bölütler görünür)** "10:09 SAT, OCT 15", yeşil çerçeveli gri LCD (gün/ay üst kutularda), tuğla duvar zeminde neon yeşil, **nixie tüpleri** (turuncu, ay takvimi altında), metal çevirmeli "10 09 SATURDAY", fotoğraf zeminli ince saat + takvim (pagoda, yıldızlı gece), pembe neon "10:09"; uygulama başlığı "Clock widget", sol üstte ⓘ, sağ üstte dişli; zemin siyah, kartlar #292929 (zemin #1D1D1D).
- Bizde olmayan / farklı:
  - Uygulama içinde widget galerisi (boyuta göre önizleme, "nasıl eklenir"; Android'de doğrudan "Ana ekrana ekle") — **yok** (widget'lar yalnız sistem seçicisinden; `requestPinAppWidget` kodda yok: `grep -rn requestPinAppWidget mobile/android` boş) — K — ücretsiz.
  - LCD/nixie/neon widget stilleri — mevcut 2. analiz #9 ve #18'e ek ayrıntı (widget sürümü).

## Clockology Watch Faces (App Store 1456386228, ABD 4,80 / 57.294 puan; Clockology, LLC; sürüm 4.0.6, 12 Ekim 2026) — Apple Watch "kadran" uygulaması
- Ücret modeli: ücretsiz + abonelik **Clockology+**: aylık ₺29,99 / ₺99,99, yıllık ₺249,99 / ₺999,99.
- İncelenen görüntü: 7 iPhone (açıldı). Apple Watch görüntüleri App Store sayfasında okunamadı (yalnız iPhone/iPad görselleri geliyor).
- Nasıl çalışır (görüntü ve açıklama): Apple üçüncü taraf kadrana izin vermediği için iki yol: (1) saatte açık kalan **uygulama içinde tam ekran "kadran"**; (2) Apple'ın **Modular Duo** kadranına büyük dikdörtgen komplikasyon ("Add Modular Duo Watch Face": "BATTERY/DAY/MONTH/DATE/YEAR/TEMP" satırları, eşit aralıklı yazı); ayrıca aynı düzenleyiciyle **iPhone ana ekran widget'ı** ("Choose Widget — Active/Inactive", "Large Home Screen #2 Today at a Glance V2", "#3 Five-Day"). Katmanlar: video/animasyon, ses (saat başı ya da dokununca), tarih-saat, hava (Apple Weather), etkinlik/sağlık, takvim, grafik, **altın saat**, pil; tetikleyiciler (saniye = 0, hava koşulu).
- Görülen kadranlar: yoğun bilgi (adım, kalori, nabız, pil, hava, "SAT 30 NOV", segment yazı), LCD sarı-yeşil "SUNNY SAT 30 NOV", dolunay fotoğraflı "10:09", turuncu "DISCOVER/EXPLORE/CREATE/DESIGN" etiketli tanıtım; "ENHANCE All Of Your iOS Devices" (saat, iPhone, iPad, Mac, Vision simgeleri); iPhone'da yuvarlak kadranlı ana ekran widget'ı ("30 SATURDAY 12:46 NOVEMBER 2024").
- Tasarım: turuncu (#E37737) zemin, koyu kahve etiket kutuları, beyaz kalın büyük harf.
- Bizde olmayan / farklı:
  - Apple Watch'ta Modular Duo/Ultra büyük dikdörtgen komplikasyonu için zengin vakit düzeni (bugünün beş vakti satır satır) — **kısmen** (`accessoryRectangular` var: ad, sıradaki vakit ve sayaç üç satır: `WatchComplications.swift` satır 85–90; bugünün vakitleri listesi yok) — K — Clockology'de abonelik.
  - Kadran/widget düzenleyici ve paylaşım — yapılmamalı (bizim ölçeğe uymaz) — —.

## Pray Watch (App Store 989923828, ABD 4,81 / 9.314 puan; Human Assistance & Development International; sürüm 28.0.0, 21 Ekim 2026) — Apple Watch komplikasyonlu namaz uygulaması
- Ücret modeli: **ücretsiz, reklamsız, satın alma yok** ("100% Privacy. Always free. And no ads…ever."; TR sayfasında uygulama içi satın alma listesi yok). Bizimle aynı model.
- İncelenen görüntü: 10 iPhone (açıldı); Apple Watch görüntüleri sayfada okunamadı.
- Öne çıkan ekranlar: siyah (#000000) zemin; köşelerde dört vakit ("Dhuhr 1:08", "Asr 4:30", "Maghrib 7:11", "Isha 8:25"), ortada turuncu hicri tarih "Sha'ban 17, 1443" (ince) ve şehir, **yeşil kalın "prayer ring"** (sıradaki vakte kalan süre halkası, uçta yuvarlak işaret), içinde "5:55:37 left until Fajr 5:30 AM"; açık temada da aynı (#FFFFFF zemin, koyu yeşil halka). Vakitler listesi: her vaktin altında açılır ayar (Notification sound "Al-Aqsa", Reminder sound "Tone", Reminder time −/+), **Midnight** ve **Last Third** satırları; aylık tablo (hicri sütunlu, bugün yeşil); İslami günler listesi (hicri + miladi); Ayarlar: Snooze time 30 min, **Pre-adhan reminder 20 min**, Scheduled reminders, Calculation (Method "Moonsighting Committee", Jurisprudence, Adjustments), Location, Time.
- **Widget'lar**: orta: hicri tarih + şehir başlık satırı, "5:52:23 left until Fajr" ve altında yeşil ilerleme çizgisi, altı vakit kısaltmayla (FJR SHK DHR ASR MGB ISH); küçük: **halka + dört köşede vakitler** (DHR 1:09, ASR 4:29, MGB 7:10, ISH 8:24), halkanın içinde "17 / 5:52:23 left until Fajr 5:30 AM"; küçük liste; **küçük "13 days, 0 hr left until Ramadan 1, 1443 April 2, 2022"**.
- **Bildirim**: "TIME SENSITIVE" vakit bildirimi; uzun basınca **zengin içerik**: kıble pusulası ("Qibla direction is 58.49° from North", "Fajr starts in 5:53:53") ve **"Snooze" (ay simgeli) düğmesi**.
- Açıklamadaki saat özellikleri: iCloud ile Apple Watch eşitleme, "Dynamic Apple Watch face complications", saatte kıble (titreşimli), komplikasyonlarda hicri tarih ve İslami günler, **renk temaları uygulamaya, widget'lara ve komplikasyonlara**; "travel mode" (uygulamayı açmadan yolculukta bildirimleri günceller).
- Hatalar: görülmedi.
- Bizde olmayan / farklı:
  - Widget ve komplikasyonda kalan süre halkası/ilerleme (sıradaki vakte) — **yok** (`WatchComplications.swift` dairesel komplikasyon yalnız ad + saat metni; Android `SUPPORTED_TYPES` yalnız `SHORT_TEXT,LONG_TEXT`, `RANGED_VALUE` yok; iOS `PrayerWidgets.swift` küçük widget'ta halka yok) — K — ücretsiz.
  - Vakit bildiriminde erteleme ("Snooze") ve uzun basınca kıble/geri sayım içeriği (Notification Content Extension) — **yok** (iOS projesinde yalnız widget ve saat uzantıları: `mobile/ios/project.yml`) — O — ücretsiz.
  - Ramazan'a kalan gün widget'ı — mevcut 2. analiz #55/#57'ye ek ayrıntı (bizde geri sayım widget'ı elle tarih ister: `MoreWidgets.swift` `CountdownIntent`).
  - Widget/komplikasyon renk teması — mevcut 2. analiz #17 ve #32'ye ek ayrıntı (Pray Watch'ta ücretsiz).

## Facer Watch Faces — Wear OS (Google Play com.jeremysteckling.facerrel, 4,1 / 192.969 puan, 10 Mn+ indirme; Facer Studios) — iOS sürümü ilk dalgada (`kadran.md`)
- Ücret modeli: reklamlı + uygulama içi satın alma (Play'de fiyat görülmedi).
- Veri güvenliği: paylaşılan: kilitlenme/teşhis (analiz); toplanan: cihaz kimliği…
- İncelenen görüntü: 16 (8 telefon + 8 kare Wear kadranı; açıldı).
- Görülen: "Over 500,000 Watch Faces", lisanslı markalar (Fallout, Atari, MTV, Star Trek, SpongeBob, Call of Duty), kategoriler (Fitness, Classic, Digital, Fun), **"Color themes"** 9 renk dairesi (camgöbeği, turuncu, sarı-turuncu, yeşil, açık yeşil, pembe, kırmızı, orkide, mor) ve **"Customizable data"** (komplikasyon önizlemeleri: MAY 05, 10, %89 pil, 29° hava, 77 nabız), Facer Creator (web). Wear kadranları: kenar boyunca renkli yaylar (pil %52, nem %78, sıcaklık 84°F), **kenarda takvim etkinliği** ("11:00am Coffee Chat"), Pip-Boy, Syron LCD, BT dalgıç kadranı.
- Bizde olmayan / farklı: mevcut 2. analiz #32'ye ek ayrıntı (renk teması 9 renk, komplikasyon önizlemeli düzenleyici); kenar boyunca yay göstergeleri yukarıdaki "ilerleme halkası" işine ek.

## Pujie Watch Faces — Wear OS 4- (Google Play com.pujie.wristwear.pujieblack, 4,1 / 11.096 puan, 100 B+ indirme; Pujie)
- Ücret modeli: Play'de "Reklam içerir"/"Uygulama içi satın alma" etiketi yok; tanıtımda "All for one **fixed price**, including very fast support!" (tek seferlik).
- Veri güvenliği: "Üçüncü taraflarla veri paylaşımı yok"; toplanan: uygulama etkileşimleri (analiz), ad/e-posta/kullanıcı kimliği (hesap, isteğe bağlı).
- **Önemli:** "Bu sürüm Galaxy Watch 7/8, Galaxy Watch Ultra ve Pixel Watch 3 ile uyumlu değildir. Ayrıca **Wear OS 6** veya daha yeni saatlerle de uyumlu olmayacaktır" → ayrı paket `com.pujie.watchfaces` (Wear OS 6). Eski kadran API'si yeni saatlerde kapandı; bizim kadranımız baştan **Watch Face Format** (`mobile/android/watchface/src/main/res/raw/watchface.xml`) — doğru yol.
- İncelenen görüntü: 16 (8 telefon + 8 kare Wear; açıldı).
- Telefon düzenleyicisi: koyu kahve-bordo (#211919) Material 3 zemin, şeftali vurgu; sekmeler **Palette / Interactive / Ambient / Styling / Calendar**; "Adjust palette" (6 renk: turuncu, beyaz, gri, şeftali, koyu gri, siyah), **"Gradient"** (iki durak, konum ve opaklık), "Cloud Library" (14073 kadran), öğeler listesi (Calculator, Stopwatch, Rounded tick mark, Watch hand), Settings (Time zones, Tap actions, Animations & battery saving, Notifications "Watch battery notification").
- Saat görüntüleri: "Interactive" kadranlar (müzik ⏮ ⏭, adım, hava 15°C, kalp), **saatte kadran içe aktarma** ("Browse to pujie.io/import, Temporary ID U8rr9"), "Tap to select" kadran seçimi, bej klasik kadran (Roma rakamı XII/III/IX, "THU, JUL 6", alt kadranda "6 Jul").
- Bizde olmayan / farklı:
  - **Ortam (AOD) modu için ayrı tasarım** — **kısmen** (ortamda yalnız saniye ibresi gizleniyor; kadran görseli aynı parlaklıkta kalıyor: `watchface.xml` `<Variant mode="AMBIENT" target="alpha" value="0" />` yalnız `SecondHand`'de) — K — ücretsiz (Wear OS AOD güç ve yanma kuralları için de iyi).
  - Kadranda renk ve gradyan — mevcut 2. analiz #32'ye ek ayrıntı.

## WatchMaker Watch Faces (Google Play slide.watchFrenzy, 3,8 / 98.406 puan, 5 Mn+ indirme; androidslide)
- Ücret modeli: uygulama içi satın alma (Premium; Play'de fiyat görülmedi); reklam etiketi yok.
- Veri güvenliği: paylaşılan: yaklaşık konum (işlev), kilitlenme, **cihaz kimliği (reklam veya pazarlama)**, uygulama etkileşimleri.
- İncelenen görüntü: 17 (8 telefon + 1 tablet + 8 kare Wear; açıldı).
- Görülen: "Over 100,000 Watch Faces", "Real Time 3D Watch Faces", "Build your own", "Then send straight to your smartwatch!", **"New Themeable Colors!"** (28 renk dairesi + damlalık + görsel + kamera), "Advanced Functions: Stopwatch, GIFs, Calendars, Sounds, Maps"; Wear kadranları: kalın dar grotesk rakamlı **minimal dijital kadranlar hareketli zeminlerle** (tutulma ışıması, synthwave güneşi ve ızgara, alev, mavi duman, sarı hilal, ışık halkası).
- Bizde olmayan / farklı: mevcut 2. analiz #32'ye ek ayrıntı (28 renk). Hareketli kadran zemini — Wear OS'ta pil maliyeti yüksek; WFF'de sınırlı — yapılmamalı (bizim için) — —.

## Android'e özgü konular (bu taramada ekranda görülenler ve görülmeyenler)
- **Material You dinamik renk:** Digital Clock Widget'ta "Dynamic color" anahtarı görüldü; Google Saat "Material 3 Expressive" (ilk dalga görüntüleri). Bizde yok (`Theme.kt`). K.
- **Tema simgeleri (monochrome):** bizde var (`ic_launcher.xml` `<monochrome>`). Rakip görüntülerinde görülmedi.
- **Kenar ışığı (edge lighting), Always-On Display, kilit ekranı saati:** Samsung'da sistem ayarı (Saat uygulamasının Play görüntülerinde yok); StandBy Mode açıklamasında "AMOLED saat", "kilit ekranının üzerinde görünür". Bizde ekran koruyucu ve tam ekran gece modu var; AOD'ye üçüncü taraf erişemez. Görülmedi.
- **Yüzen pencere:** rakiplerde görülmedi; bizde var (`FloatingClock.kt`).
- **Android 16 Live Updates** (durum çubuğunda süren sayaç/çip): incelenen Play görüntülerinin hiçbirinde görülmedi. Bizde yok (`LiveNotifications.kt` sıradan kalıcı bildirim; `ProgressStyle`/`setRequestPromotedOngoing` yok). Zamanlayıcı, pomodoro ve sıradaki vakit için uygun aday — K.
- **Kilit ekranı widget'ları (Android 16, tablet "hub"):** görülmedi; bizim widget'larımız `widgetCategory="home_screen"`.
- **Play veri güvenliği:** Samsung Saat ve Digital Clock Widget "Veri toplanmıyor"; Google Saat "paylaşım yok"; vakit uygulamalarının hepsi (Ezan360, Muslim Pro, Ezan Vakti Pro) en az konum ya da reklam kimliği paylaşıyor. Bizim planımız (`mobile/android/PLAY_CONSOLE.md` §4): yaklaşık konum + isteğe bağlı e-posta toplanır, paylaşılmaz, reklam kimliği yok — mağaza metninde öne çıkarılabilir.

## Okunamayanlar / bakılmayanlar
- Galaxy Store sayfası (JS ile yükleniyor) okunmadı; Samsung Saat'in Play sayfası okundu.
- App Store arama API'si (`itunes.apple.com/search`) taramanın ortasında boş dönmeye başladı (istek sınırı); "StandBy" ve "Clockology" kimlikleri web aramasıyla bulundu, `lookup` çalıştı.
- Apple Watch ekran görüntüleri (Clockology, Pray Watch) App Store sayfasında okunamadı; yalnız iPhone görselleri ve açıklama kullanıldı.
- Top Widgets⁺ (6446477593), Widgetsmith (ilk dalgada), iScreen, Themify bakılmadı (süre; Color Widgets ve WidgetClub aynı türü kapsıyor).
- Namaz Vakti PRO (com.namazvakti.app.namaz_vakti, Hfs Yazılım, 19 puan) Play sayfası okundu ama küçük olduğu için incelenmedi.
