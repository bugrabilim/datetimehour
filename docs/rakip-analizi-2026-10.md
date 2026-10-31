# Rakip analizi: rakiplerde olup bizde olmayanlar (30 Ekim 2026)

Sahibin isteği (30 Ekim 2026): rakip uygulama ve sitelerde olup Saat Tarih'te olmayan özellikler, tablo hâlinde. Bir sonraki canlı basıma eklenecek işler için sahibin talimatı beklenir.

**Yöntem:** 5 araştırma ajanı paralel çalıştı (alarm/uyanma; masa/flip/tam ekran saat; kronometre/zamanlayıcı/odak/geri sayım/dünya saati; saat siteleri; namaz/takvim/Güneş-Ay). Kaynaklar: resmî App Store sayfaları, geliştirici siteleri, Apple/Google yardım sayfaları.

**Kaynak sınırları:**
- Google Play sayfaları çoğunlukla okunamadı. Android bilgisinin bir kısmı üçüncü taraf ayna sitelerden ya da arama özetinden gelir.
- timeanddate.com 403 döndü; yardım sayfalarıyla incelendi.
- "Bizde" sütunu proje notlarına ve koda göre yazıldı.

**Ad eşleşmeleri:**
- "Rise", RISE: Sleep Tracker'dır (uyku borcu, enerji).
- "Desk Clock" adıyla en popüler uygulama Desk Clock - Analog Clock Face'tir.
- "Stopwatch+" ve "Full Screen Clock" tam adıyla bulunamadı; en yakınları incelendi.

Çaba: K küçük (bir tur), O orta, B büyük.

## Tek liste (30 Ekim 2026'dan beri; sahibin isteği: "tek liste olarak tut, her işe bir numara ver, K-O-B sırala, bitenler de dursun")

Ajan raporlarındaki bütün maddeler tek numaralı listede: 107 iş; 99 bitti, 1 açık, 7 kararla yapılmayacak. Sıra çabaya göre (K küçük: bir tur; O orta; B büyük), her çaba grubunda önce bitenler, sonra açıklar; en altta kararla yapılmayanlar. Numaralar sabittir: iş bitince yalnız "Durum" değişir, yeni iş en sona eklenir. "Eski no" sütunu ilk 28 maddelik tablonun numarasıdır; CLAUDE.md ve commit mesajlarındaki "rakip analizi #N" bu eski numarayı gösterir. Hangi açık işin yapılacağı sahibin kararıdır.

| No | İş | Nerede | Çaba | Durum | Eski no |
|---|---|---|---|---|---|
| 1 | Gece / yatak başı modu (kırmızı-loş, otomatik, piksel kaydırma, uyku zamanlayıcısı) | Uygulama | K | bitti | #1 |
| 2 | Resmî tatilde çalmayan alarm | Uygulama | K | bitti | #4 |
| 3 | Hicri tarih (widget, kilit ekranı, takvim) | Uygulama | K | bitti | #9 |
| 4 | Alarmdan sonra günaydın kartı | Uygulama | K | bitti | #14 |
| 5 | Hazır süre ve alarm sayfaları ("5 dakika", "07:00 alarm") | Site | K | bitti | #20 |
| 6 | Köprü tatil planlayıcı | Site | K | bitti | #22 |
| 7 | Eksik özel gün geri sayımları (Anneler, Babalar, Sevgililer Günü…) | Site | K | bitti | #23 |
| 8 | Planlayıcı ve çeviricide paylaşım bağlantısı, takvime ekle | Site | K | bitti | #24 |
| 9 | Dualar | Uygulama | K | bitti |  |
| 10 | Zikirmatik | Uygulama | K | bitti |  |
| 11 | Zekât hesaplama | Uygulama + site | K | bitti |  |
| 12 | Kerahat vakti | Uygulama + site | K | bitti |  |
| 13 | Hicri–Miladi tarih çevirici | Uygulama + site | K | bitti |  |
| 14 | Fitre hesabı | Uygulama + site | K | bitti |  |
| 15 | Altın ve mavi saat | Uygulama + site | K | bitti |  |
| 16 | Vakit ve dini gün paylaşım kartı | Uygulama | K | bitti |  |
| 17 | Hızlı alarm ("X dakika sonra") | Uygulama | K | bitti |  |
| 18 | Kaydırarak durdurma (yanlışlıkla kapatmayı önler) | Uygulama | K | bitti (Android; iOS'ta durdurma AlarmKit'in sistem ekranında) |  |
| 19 | Alarm çalmadan önce bildirim, erken uyanınca kapatma | Uygulama | K | bitti |  |
| 20 | Alarmda saatin sesli okunması | Uygulama | K | bitti (iOS'ta saati söyleyen ses dosyası; cihazda denenmedi) |  |
| 21 | Alarm çalarken uygulamayı kapatma/silme engeli | Android | K | bitti (çalarken geri tuşu kapalı; uygulamayı silme engeli yönetici izni ister, yapılmadı) |  |
| 22 | Ekranı kaydırarak karartma, saat boyutu, yön kilidi | Uygulama | K | bitti |  |
| 23 | Saat ekranında pil ve sıradaki alarm alanı | Uygulama | K | bitti |  |
| 24 | Tam ekranda fotoğraf slaytı | Uygulama | K | bitti |  |
| 25 | Saatin altında kişisel metin | Uygulama | K | bitti |  |
| 26 | Çalışan zamanlayıcıya süre ekleme (+1 dk) | Uygulama + site | K | bitti |  |
| 27 | Odak serisi, günlük hedef, etiket | Uygulama + site | K | bitti |  |
| 28 | Tur ve odak geçmişini CSV olarak indirme | Uygulama + site | K | bitti |  |
| 29 | Geri sayımı görsel kart olarak paylaşma | Uygulama | K | bitti |  |
| 30 | Dünya saatlerindeki şehirler için yaz saati uyarısı | Uygulama | K | bitti |  |
| 31 | Saat hesaplama (saat:dakika toplama/çıkarma) | Site | K | bitti |  |
| 32 | "Kaç gün kaldı / kaç gün oldu" sayfaları | Site | K | bitti |  |
| 33 | 14 günlük hava tahmini | Site | K | bitti |  |
| 34 | Ay evreleri ve tutulmalar için takvim aboneliği (.ics) | Site | K | bitti |  |
| 35 | Sitede alarm sesi seçimi | Site | K | bitti |  |
| 36 | Sitede aralık (HIIT) zamanlayıcı | Site | K | bitti |  |
| 37 | Satranç saati | Site | K | bitti |  |
| 38 | Metronom | Site | K | bitti |  |
| 39 | Konuşan saat | Site | K | bitti |  |
| 40 | Sitede saat yazı tipi ve renk seçimi | Site | K | bitti |  |
| 41 | Burç hesaplama | Site | K | bitti |  |
| 42 | Live Activity / Dynamic Island: sayaçlar ve sıradaki vakit kilit ekranında | Uygulama | O | bitti | #2 |
| 43 | Alarm ayarları (gün seçimi, erteleme süresi ve sınırı, atla, kademeli ses) | Uygulama | O | bitti | #3 |
| 44 | Çevirmeli (flip) saat | Uygulama + site | O | bitti | #5 |
| 45 | Siri / Kısayollar ve Android karşılıkları | Uygulama | O | bitti | #7 |
| 46 | Kıble pusulası ve yakındaki camiler | Uygulama + site | O | bitti | #8 |
| 47 | Ay doğuş/batış, Ay takvimi, Ay widget'ı, tutulmalar | Uygulama + site | O | bitti | #10 |
| 48 | Saniyeli, dünya saati ve geri sayım widget'ları | Uygulama | O | bitti | #11 |
| 49 | Birden çok adlı zamanlayıcı, aralık antrenmanı, sesli duyuru | Uygulama | O | bitti | #12 |
| 50 | Alarm kapatma görevi ve uyanma denetimi ("Uyanık" rozeti) | Uygulama | O | bitti | #13 |
| 51 | Kişisel geri sayım listesi (yıllık, hatırlatma, fotoğraf, takvimden) | Uygulama | O | bitti | #15 |
| 52 | Odak istatistikleri, yapılacaklar listesi, arka plan sesi | Uygulama + site | O | bitti | #16 |
| 53 | Uyku planı, yatma hatırlatması, uyku sesleri, gün doğumu ışığı | Uygulama | O | bitti | #17 |
| 54 | iPad (ve Mac'te iPad uygulaması olarak) | Uygulama | O | bitti | #18 |
| 55 | İlçe bazında vakit, kayıtlı yerler | Uygulama + site | O | bitti | #19 |
| 56 | Takvim aboneliği (.ics) ve yazdırma | Site | O | bitti | #21 |
| 57 | Yaz saati listesi, saat dilimi kısaltmaları, Unix saati | Site | O | bitti | #25 |
| 58 | Görünüm: yazı tipi, fotoğraf ve hareketli arka plan | Uygulama | O | bitti | #26 |
| 59 | Her özellik için bildirimler | Uygulama | O | bitti | #28 |
| 60 | Kur'an (Arapça, İngilizce meal, Diyanet meal bağlantısı) | Uygulama | O | bitti |  |
| 61 | Namaz ve oruç takibi (kaza dahil) | Uygulama | O | bitti |  |
| 62 | Hesapsız eşitleme (iCloud, Android yedeği) | Uygulama | O | bitti |  |
| 63 | Açık veri (API) | Site | O | bitti |  |
| 64 | Tarayıcı eklentisi | Chrome, Edge, Firefox | O | bitti |  |
| 65 | Vakit ekranında Güneş/Ay yayı | Uygulama | O | bitti |  |
| 66 | Başka hesap yöntemleri ve Hanefi ikindisi | Uygulama + site | O | bitti |  |
| 67 | Vakti gerçek alarm olarak kurma (telefon sessizdeyken de çalar) | Uygulama | O | bitti |  |
| 68 | Vakitte telefonu sessize alma | Android | O | bitti |  |
| 69 | Esmaü'l-Hüsna | Uygulama | O | bitti |  |
| 70 | Hadis | Uygulama | O | bitti |  |
| 71 | Namaz rehberi | Uygulama | O | bitti |  |
| 72 | Takvim yaprağı, günün sözü | Uygulama | O | bitti |  |
| 73 | Hilal görünürlüğü | Uygulama + site | O | bitti |  |
| 74 | Esnek alarm tekrarı (belirli tarih, iki haftada bir, bitiş tarihi) | Uygulama | O | bitti |  |
| 75 | Başka alarm görevleri (hafıza, yazma, fotoğraf) | Uygulama | O | bitti |  |
| 76 | Sabah günlüğü | Uygulama | O | bitti |  |
| 77 | Dokunulabilen widget'lar, iPhone Denetim Merkezi düğmeleri | Uygulama | O | bitti |  |
| 78 | Wear OS saat kadranı | Android saat | O | bitti |  |
| 79 | Havaya göre hareketli arka plan | Uygulama | O | bitti |  |
| 80 | Diğer uygulamaların üstünde yüzen saat | Uygulama | O | bitti |  |
| 81 | Aynı anda birden çok kronometre | Uygulama | O | bitti |  |
| 82 | Zamanlayıcı geçmişi | Uygulama | O | bitti |  |
| 83 | Çok adımlı antrenman rutini şablonları | Uygulama | O | bitti |  |
| 84 | Apple Sağlık / Health Connect kaydı | Uygulama | O | bitti |  |
| 85 | Uygulamada toplantı planlayıcı | Uygulama | O | bitti |  |
| 86 | Emeklilik hesaplama | Site | O | bitti |  |
| 87 | Gece gökyüzü, gezegenler, Ay uzaklığı | Site | O | bitti |  |
| 88 | Tutulma haritası | Site | O | bitti |  |
| 89 | "Tarihte bugün" | Site | O | bitti |  |
| 90 | Apple Watch ve Wear OS uygulamaları | Uygulama | B | bitti | #6 |
| 91 | Çok ülkeli resmî tatil takvimleri | Site | B | bitti | #27 |
| 92 | Başka ezan sesleri | Uygulama | B | bitti (Medine kaydı, ses seçimi; Konya adayı sahibin dinleyip onayını bekliyor; Mekke indirilemedi, İstanbul kaydı uygun değil) |  |
| 93 | Konuma ya da takvime bağlı alarm | Uygulama | B | bitti |  |
| 94 | iPhone'da müzikle alarm | Uygulama | B | bitti |  |
| 95 | Apple TV ve Android TV uygulamaları | TV | B | bitti |  |
| 96 | Yerel Mac uygulaması (menü çubuğunda vakit) | Mac | B | bitti |  |
| 97 | Vision Pro uygulaması | Vision Pro | B | bitti |  |
| 98 | Odakta uygulama/site engelleme | Uygulama | B | bitti (iOS'ta Apple'ın FamilyControls onayı bekleniyor, o zamana kadar gizli) |  |
| 100 | Saat dilimi haberleri ve bülten | Site | B | bitti (sayfa ve Atom akışı; e-posta bülteni yok) |  |
| 99 | Daha çok dil | Uygulama + site | B | açık |  |
| 101 | Uyku takibi, horlama kaydı, uyku evresine göre akıllı alarm | Uygulama | — | yapılmayacak (gizlilik: mikrofon, hareket) |  |
| 102 | Sitede hesap ve üyelik | Site | — | yapılmayacak (standartta bu proje için yok) |  |
| 103 | 81 il saat sayfaları | Site | — | yapılmayacak (kopya içerik riski) |  |
| 104 | Saat dilimi haritası (dönen dünya) | Site | — | yapılmayacak (sitede kaldırıldı, 30 Ekim kararı; uygulamada var) |  |
| 105 | Namaz serileri, ibadet rozeti | Uygulama | — | yapılmayacak (inanç ele vermemek için) |  |
| 106 | Ekip planlama, toplantı oylaması | Site | — | yapılmayacak (hesap gerektirir) |  |
| 107 | Spotify/YouTube alarm sesleri | Uygulama | — | yapılmayacak (dış hesap gerektirir) |  |

**Konumlanma notu (sahibin düzeltmesiyle, 30 Ekim 2026):** En güçlü doğrudan namaz rakibi **Vakit** (Hakan Çelik). Ücretsiz ve reklamsız; Apple Watch, Mac, Live Activity, 12 hesap yöntemi ve 25 dil var. Ona karşı farkımız fiyat değil, **yalnız namaz uygulaması olmayışımız**: saat (19 model, tam ekran), dünya saatleri ve küre, takvim, hava, mevsimler ve araçlar tek uygulamada. Masa, flip ve alarm kategorilerinde rakiplerin çoğu reklamlı ya da abonelikli; orada "ücretsiz, reklamsız" da belirgin bir farktır.

## Ek: ajan raporları (kaynak)

Aşağıdaki beş rapor listenin kaynağıdır (151 satır). "Bizde" sütunları 30 Ekim 2026 sabahki durumu gösterir; güncel durum yukarıdaki tek listededir.

## Alarm ve uyanma uygulamaları (ajan raporu, 30 Ekim 2026)
Bizde (koddan): alarmda etiket, tekrar (bir kez / her gün / hafta içi), sistemin varsayılan sesi, sabit 5 dk erteleme; tek zamanlayıcı; kronometrede tur var (iOS, Android, web).

| Özellik | Hangi rakipte | Platform | Bizde | Ücret | Kaynak |
|---|---|---|---|---|---|
| Alarmı kapatmak için görev: matematik, hafıza, yazma, sallama, squat, adım, fotoğraf, QR/barkod, gülümseme, CAPTCHA, NFC | Alarmy, Challenges Alarm Clock, Alarm Clock Xtreme, Talking Alarm Clock Beyond, AMdroid, SuperAlarm, Shake-it, Sleep as Android, Alarm Clock for Me, Turbo Alarm | iOS, Android | yok | temel görevler ücretsiz (reklamlı); Alarmy'de bazıları premium | alar.my · play.google.com (com.garageapp.alarmchallenges, com.amdroidalarmclock.amdroid) |
| Uyanma kontrolü ("uyanık mısın", yanıt yoksa yeniden çalar) | Alarmy, Alarm Clock Xtreme, SuperAlarm, AMdroid | iOS, Android | yok | Alarmy premium, diğerleri ücretsiz | alar.my · play.google.com (io.pickyz.superalarm) |
| Çalarken kapatma/çıkma/silme engeli | Alarmy, Challenges, SuperAlarm | Android | yok | doğrulanamadı | play.google.com (droom.sleepIfUCan) |
| Yanlışlıkla kapatmayı önleme (kaydırarak durdur, büyük Ertele) | Apple Saat (iOS 26.1), Alarm Clock Xtreme, Simple Alarm Clock | iOS, Android | kısmen | ücretsiz | 9to5mac.com/2025/11/05/… · play.google.com (ar.com.basejuegos.simplealarm) |
| Erteleme ayarları (süre, sayı sınırı, kısalan erteleme, yedek alarm) | Apple Saat (iOS 26: 1–15 dk), Alarm Clock Xtreme, Simple Alarm Clock, Challenges, Talking Alarm Clock Beyond, Turbo Alarm, Loud Alarm Clock, Sleep as Android | iOS, Android | kısmen (sabit 5 dk) | ücretsiz | 9to5mac.com/2025/09/30/… |
| Kademeli artan ses, telefon sesinden bağımsız düzey, titreşim aç/kapa | Alarm Clock Xtreme, Simple Alarm Clock, Challenges, Talking Alarm Clock Beyond, Alarm Clock for Me, Rise | iOS, Android | yok | ücretsiz | play.google.com (com.alarmclock.xtreme.free) |
| Alarm sesi seçimi: cihaz müziği, çalma listesi, radyo, Spotify/YouTube Music, 100+ yüksek ses, rastgele | Google Saat, Alarm Clock Xtreme, Alarmy, Talking Alarm Clock Beyond, Sleep as Android, Loud Alarm Clock | iOS, Android | yok | ücretsiz (Spotify şarkısı için premium hesap) | support.google.com/clock · alar.my |
| Esnek tekrar: istenen günler, belirli tarih, aralıklı tekrar, bitiş tarihi | Talking Alarm Clock Beyond, AMdroid, Samsung Saat, SuperAlarm | Android | kısmen (bir kez/her gün/hafta içi) | ücretsiz | play.google.com (com.sec.android.app.clockpackage) |
| Sonraki alarmı bir kez atlama; çalmadan önce bildirim; erken uyanınca kapatma | Turbo Alarm, Samsung Saat, SuperAlarm, Alarm Clock Xtreme, Talking Alarm Clock Beyond | Android | yok | ücretsiz | play.google.com (com.turbo.alarm) |
| Resmî tatilde alarm çalmaz | AMdroid | Android | yok (Türkiye tatil/arife verimiz hazır) | ücretsiz (reklamlı) | play.google.com (com.amdroidalarmclock.amdroid) |
| Koşullu alarm (konuma ya da takvim etkinliğine göre) | AMdroid, Wake me up! | Android | yok | kısmen premium | play.google.com (com.chlochlo.adaptativealarm) |
| Alarmdan sonra "günaydın" ekranı (hava, takvim, haber, trafik) | Alarmy, Alarm Clock Xtreme, Google Saat, Samsung Saat, Talking Alarm Clock Beyond, Loud Alarm Clock, AMdroid | iOS, Android | kısmen (veriler var, alarma bağlı değil) | ücretsiz | play.google.com (com.alarmclock.xtreme.free) · support.google.com/pixelphone/answer/2840926 |
| Alarmda saatin/etiketin sesli okunması | Talking Alarm Clock Beyond, Wake me up!, Loud Alarm Clock | Android | kısmen (yalnız vakit bildiriminde) | ücretsiz | play.google.com (me.jlabs.loudalarmclock) |
| Akıllı alarm (hafif uykuda pencere içinde uyandırma), ön alarm | Sleep Cycle, Rise, Sleep as Android, Sleepwave, AMdroid, Alarm Clock for Me | iOS, Android | yok | çoğu abonelik | play.google.com (com.northcube.sleepcycle, com.urbandroid.sleep) |
| Uyku takibi, horlama/konuşma kaydı, Apple Health/Health Connect | Alarmy, Sleep Cycle, Sleep as Android, Sleepwave, Rise, Google Saat | iOS, Android | yok | kısmen premium | apps.apple.com/us/app/id320606217 |
| Uyku borcu, sirkadiyen ritim, enerji takvimi | Rise | iOS, Android | yok | yıllık 69,99 $ | apps.apple.com/us/app/id1453884781 |
| Uyku programı (yatma/kalkma, gevşeme, Uyku odağı) | Apple Saat + Sağlık, Google Saat, Alarmy, Sleep as Android, Alarm Clock for Me, AMdroid, Shake-it | iOS, Android | yok | ücretsiz | support.apple.com/guide/iphone/iphaf56dceb4 |
| Uyku sesleri, beyaz gürültü, meditasyon, uyku zamanlayıcısı | Google Saat, Alarmy, Sleep Cycle, Rise, Sleep as Android, Alarm Clock for Me, Sleepwave | iOS, Android | yok | kısmen premium | play.google.com (com.google.android.deskclock) |
| Gün doğumu alarmı (ekran yavaşça aydınlanır), akıllı lamba | Google Saat, Sleep as Android | Android | yok | ücretsiz | support.google.com/clock |
| Gece/komodin saati (şarjda yatay, karartma, parlaklık) | Apple StandBy, Google Saat, Alarm Clock for Me, Talking Alarm Clock Beyond | iOS, Android | kısmen (tam ekran + ekran açık; karartma/gece modu yok) | ücretsiz | support.apple.com/guide/iphone/iph878d77632 |
| Aynı anda birden çok, etiketli zamanlayıcı | Apple Saat (iOS 17+), Samsung Saat, Alarm Clock Xtreme | iOS, Android | kısmen (tek zamanlayıcı) | ücretsiz | tech.yahoo.com (iOS 17 multiple timers) |
| Hızlı alarm ("X dk sonra"), sayı klavyesiyle saat, sıralı liste | Alarm Clock Xtreme, AMdroid, Simple Alarm Clock | Android | kısmen | ücretsiz | play.google.com (ar.com.basejuegos.simplealarm) |
| Live Activity / Dynamic Island (zamanlayıcı, kronometre) | Apple Saat, Sleep Cycle | iOS | yok | ücretsiz | phonearena.com (iOS 17.4) |
| Siri/Kısayollar/Asistan ile alarm, "alarm durunca" otomasyonu | Apple Saat, Google Saat, Talking Alarm Clock Beyond, Sleep as Android, Wake me up!, Sleep Cycle | iOS, Android | yok | ücretsiz | matthewcassinelli.com/action_groups/apps/clock/ |
| Akıllı saat: bilekten erteleme/kapatma, komplikasyon | Google Saat (Wear OS), Turbo Alarm, AMdroid, Sleep Cycle, Rise, Sleep as Android, Wake me up! | watchOS, Wear OS | yok | kısmen Pro | play.google.com (com.turbo.alarm) |
| Alarm ekranı kişiselleştirme (fotoğraf/video, söz, hava arka planı) | Alarmy, Loud Alarm Clock, Samsung Saat | iOS, Android | kısmen (24 tema) | ücretsiz | play.google.com (droom.sleepIfUCan) |
| Sabah günlüğü (ruh hali, rüya günlüğü, uyanıklık oyunu) | Alarmy, Shake-it, Sleep Cycle, Sleepwave | iOS, Android | yok | karışık | play.google.com (com.ingyomate.shakeit) |
| Hatırlatıcılar (yıllık/aylık/saatlik tekrar) | Alarm Clock Xtreme, Talking Alarm Clock Beyond | Android | yok | ücretsiz | play.google.com (com.alarmclock.xtreme.free) |

Öne çıkanlar: 1) Tatil ve arife bilen alarm (yalnız AMdroid'de; verimiz hazır, "bayramda çalma" az işle ayırır). 2) Alarmın temel ayarları: gün seçimi, erteleme süresi/sayısı, ses seçimi, kademeli ses, "sonraki alarmı atla" — kategorideki en zayıf yanımız. 3) Kapatma görevi ve uyanma kontrolü (Alarmy 50M+; iOS'ta AlarmKit sistem ekranında görevin nasıl açtırılacağı doğrulanmadı). 4) Alarmdan sonra "günaydın" kartı (hava, sıradaki vakit, özel gün, takvim). 5) Live Activity ve Siri/Kısayollar (App Intents); gece saati için karartma/kırmızı mod.
İncelenen: Alarmy (App Store 4,8, 251 bin oy; Play 50M+), RISE: Sleep Tracker (Rise Science; sahibin "Rise"ı; eski "Rise Alarm Clock" ayrı), Sleep Cycle, Google Saat, Apple Saat (iOS 26/26.1 + Sağlık uyku programı), Alarm Clock Xtreme, Alarm Clock for Me, Loud Alarm Clock, Simple Alarm Clock, Challenges Alarm Clock, Talking Alarm Clock Beyond, Sleep as Android, AMdroid, Samsung Saat; satırlarda: Turbo Alarm, SuperAlarm, Shake-it, Sleepwave, Wake me up!. Popüler "Kiwi"/"Mathe Alarm" bulunamadı. Doğrulanamayan: Alarmy'de Live Activity/Watch, ücretli görevler, Sleep Cycle/Rise akıllı alarmının ücreti.

## Masa, flip, tam ekran, yatak başı saatleri (ajan raporu, 30 Ekim 2026)
Not: Google Play sayfaları okunamadı; bazı Android satırları arama özetine dayanır. Kategoride çoğu uygulama ücretsiz + satın alma/abonelik; hesapsız, reklamsız, ücretsiz olmamız belirgin fark.

| Özellik | Hangi rakipte | Platform | Bizde | Ücret | Kaynak |
|---|---|---|---|---|---|
| Flip (çevirmeli kart) animasyonlu tam ekran saat | Fliqlo, Flip Clock: Digital Desk Widget, Zen Flip Clock, Flip Clock - Pomodoro & Focus, Flip Clock: Timer & Widget | iOS, iPadOS, macOS, Windows | kısmen (yalnız sitenin gömme widget'ında) | Fliqlo iOS 0,99 $; diğerleri ücretsiz + premium | fliqlo.com · apps.apple.com/app/id1181028777 |
| Gece modu: kırmızı/loş, düşük ışıkta kendiliğinden | iOS StandBy Night Mode, Google Saat ekran koruyucu, Pixel Low light clock, Night Clock | iOS, Android | yok | sistem ücretsiz; Night Clock 1,99 $ | apple.com/newsroom/2023/09 · support.google.com/clock/answer/6143369 · apps.apple.com/app/id991595133 |
| Gün batımından sonra kendiliğinden kararma | Night Clock | iOS | yok | 1,99 $ | id991595133 |
| Ekranda karartma/parlaklık (kaydırarak), saat boyutu, yön kilidi, renk ters çevirme | Fliqlo, Big Clock HD, Dock Clock HD, Clocky, Flip Clock - Digital Clock | iOS | yok | 0,49–1,99 $ | fliqlo.com · apps.apple.com/us/app/id364394834 |
| OLED yanma önleme (piksel kaydırma) | Flip Clock: Digital Desk Widget, Clock Screensaver & Widget, Big Digital Clock | iOS, Android | yok | ücretsiz + satın alma | id1181028777 · f-droid.org/packages/systems.sieber.fsclock/ |
| Şarja takınca kendiliğinden açılma | iOS StandBy, Flip Clock: Digital Desk Widget, Flip Clock - Pomodoro & Focus (Kısayol otomasyonu), StandBy Mode (Android) | iOS, Android | yok | ücretsiz + satın alma | apps.apple.com/us/app/id6739022744 |
| Android ekran koruyucusu (Daydream), Pixel hub modu | Clock Screensaver & Widget, Google Saat, Pixel | Android, Android TV | yok | ücretsiz | f-droid.org/packages/systems.sieber.fsclock/ |
| Masaüstü ekran koruyucusu | Fliqlo (Mac, Windows) | macOS, Windows | kısmen (sitede tam ekran + ekran açık) | ücretsiz | fliqlo.com |
| StandBy'ın kendi saati (stiller, renk, MagSafe hatırlama, Smart Stack) | iOS StandBy | iOS | yok (StandBy'a özel görünüm yok) | sistem | apple.com/newsroom/2023/09 |
| StandBy'a özel widget'lar (büyük saat, 2–3 saat dilimi, pil, mesaj kartı, fotoğraf) | StandBy Clock, Big Digital Clock Widget, Big Clock Pro, Flip Clock - Digital Clock | iOS | kısmen | StandBy Clock 6,99 $/yıl; Big Clock Pro 7,99 $ | apps.apple.com/app/id6450166379 · id1495547377 |
| Saniyeli widget'lar (ana ekran, kilit, StandBy, Dynamic Island) | Flip Clock: Digital Desk Widget, Desk Clock, Big Clock Pro | iOS | kısmen (saniyesiz) | ücretsiz + satın alma | id1181028777 · id1253066126 |
| Dynamic Island ve kilit ekranında canlı gösterim (geri sayım, saat) | Flip Clock - Pomodoro & Focus, Big Clock Pro, Flip Clock: Digital Desk Widget | iOS | yok | ücretsiz + satın alma | id6739022744 |
| Etkileşimli ve şeffaf widget'lar | Big Clock Pro, Flip Clock - Digital Clock | iOS | yok | 7,99 $ | id1495547377 |
| Günün saatine göre değişen widget | Widgetsmith | iOS | yok | ücretsiz + Premium | apps.apple.com/us/app/id1523682319 |
| Saat başı zil / sesli saat | Flip Clock: Digital Desk Widget, Desk Clock, Zen Flip Clock | iOS | yok | ücretsiz + satın alma | id1253066126 · id1265404088 |
| Uyku zamanlayıcısı (süre sonunda ekran kilidine dönüş) | Fliqlo | iOS | yok | 0,99 $/ay | fliqlo.com |
| Beyaz gürültü, uyku sesleri | Flip Clock: Digital Desk Widget, Flip Clock - Pomodoro & Focus (12 ses), Google Saat Yatma vakti | iOS, Android | yok | ücretsiz + satın alma | id6739022744 · support.google.com/pixelphone/answer/9887159 |
| Yatma vakti rutini (program, gri ekran, sessize alma) | Google Saat (Digital Wellbeing) | Android | yok | ücretsiz | support.google.com/pixelphone/answer/9887159 |
| Gün doğumu alarmı (ekran yavaşça aydınlanır) | Google Saat | Android | yok | ücretsiz | support.google.com/pixelphone/answer/9887159 |
| Kademeli artan alarm sesi, müzik zil kitaplığı, alarm kapatınca hava özeti | Desk Clock, Flip Clock: Digital Desk Widget, Google Saat | iOS, Android | kısmen (alarm var) | ücretsiz + satın alma | id1253066126 · androidauthority.com |
| Saat uygulaması/kadranı, alarm/kadran eşitleme | Desk Clock (Watch Face Sync), Google Saat (Pixel Watch), Wear OS flip kadranları | watchOS, Wear OS | yok (yol haritasında) | çeşitli | id1253066126 |
| iPad desteği | Fliqlo, Zen Flip Clock, Flip Clock - Pomodoro, Big Clock HD, Dock Clock HD | iPadOS | yok (yalnız iPhone) | çeşitli | apps.apple.com/us/app/id1020599499 |
| TV, Mac, Vision Pro uygulaması | Clocky (Apple TV), Flip Clock - Digital Clock (TV), Clock Screensaver (Android TV), Zen Flip Clock (Mac, visionOS), Big Clock Pro (visionOS) | tvOS, Android TV, macOS, visionOS | yok | 1,99–6,99 $ | apps.apple.com/us/app/id828625087 |
| Diğer uygulamaların üstünde yüzen saat (PiP) | Nightstand Desk Clock, Flip Clock: Timer & Widget | iOS | yok | 2,99 $ | apps.apple.com/app/id6443995614 |
| Fotoğraf/hareketli arka plan, kendi kadran görseli, slayt | Flip Clock - Digital Clock, Nightstand, Clock Screensaver, StandBy, Pixel | iOS, Android | yok | ücretsiz + satın alma | id6443995614 |
| Yazı tipi seçimi, kişisel metin/imza | Flip Clock: Digital Desk Widget, Flip Clock: Timer & Widget, Flip Clock - Pomodoro, Widgetsmith | iOS | kısmen (24 renk teması) | ücretsiz + satın alma | apps.apple.com/us/app/id6740893494 |
| Hareketli hava sahneleri | Flip Clock - Pomodoro & Focus, Pixel (Weather Frog) | iOS, Android | kısmen (hava durumlu modeller) | ücretsiz + satın alma | id6739022744 |
| Saat ekranında pil, takvim etkinliği, sıradaki alarm | Clock Screensaver & Widget, StandBy Clock | Android, iOS | kısmen (sıradaki vakit) | ücretsiz + satın alma | f-droid.org/packages/systems.sieber.fsclock/ |
| Odak istatistikleri, ileri sayan sayaç | Zen Flip Clock | iOS | kısmen | Pro 0,99 $/ay | id1265404088 |

Öne çıkanlar: 1) Gece/yatak başı modu (kırmızı/loş, gün batımında kararma, dokunarak karartma, piksel kaydırma, uyku zamanlayıcısı) — küçük iş, en büyük boşluk. 2) Flip saat modeli (gömmedeki eski model sahneye ve uygulamalara). 3) Live Activity/Dynamic Island, saniyeli ve StandBy'a uygun widget'lar ("sıradaki vakte kalan" farklılaştırıcı). 4) Şarja takınca açılma (iOS Kısayollar eylemi, Android Daydream). 5) iPad desteği.
İncelenen (26): Fliqlo, Flip Clock: Digital Desk Widget, Desk Clock - Analog Clock Face (Desk Clock adıyla en popüler; 4,6, 22 bin), Flip Clock - Digital Clock, Big Clock - Pro, Flip Clock: Timer & Widget, Flip Clock - Pomodoro & Focus, Zen Flip Clock, Nightstand Desk Clock, Dock Clock HD, Clocky, Night Clock, Big Clock HD, Always On - Full Screen Clock ("Full Screen Clock" tam adıyla bulunamadı), StandBy Clock, Widgetsmith, Big Digital Clock Widget, Clock Screensaver & Widget, StandBy Mode (Android), Big Digital Clock - FullScreen, Digital Clock (envos), Google Saat, Pixel ekran koruyucu, iOS StandBy, Wear OS flip kadranları, FlipClock.tv (doğrulanamadı).

## Kronometre, zamanlayıcı, odak, geri sayım, dünya saati (ajan raporu, 30 Ekim 2026)
Not: Google Play sayfaları araçla okunamadı; özellikler çoğunlukla App Store açıklamalarından.

| Özellik | Hangi rakipte | Platform | Bizde | Ücret | Kaynak |
|---|---|---|---|---|---|
| Çoklu, adlı, kayıtlı zamanlayıcılar; pano; çalışırken süre ekle/çıkar; tekrar; zamanlanmış başlatma | Timer+, MultiTimer | iOS (MultiTimer: iOS, Android, web) | yok (tek zamanlayıcı) | temel ücretsiz, sınırsız premium | apps.apple.com/us/app/id391564049 · apps.apple.com/app/id973421278 |
| Aynı anda birden çok kronometre (ad, renk, sıralama) | Stopwatch plus Multiple Timer, Timer+ | iOS | yok | ücretsiz reklamlı | apps.apple.com/us/app/id923834737 |
| Tur ayrıntısı: en hızlı/yavaş tur, tura ad | Stopwatch plus, Seconds, Timer+ | iOS | doğrulanmalı | ücretsiz | id923834737 · apps.apple.com/us/app/id475816966 |
| Dışa aktarma: CSV, PDF/görsel rapor | Stopwatch plus, MultiTimer, Be Focused, Pomofocus | iOS, web | yok | kısmen premium | id923834737 · id973130201 · pomofocus.io |
| Aralık zamanlayıcı (HIIT, Tabata, EMOM), çok adımlı rutinler, şablonlar | Seconds, Timer+, MultiTimer | iOS, Android | kısmen (yalnız pomodoro) | Seconds 7,99 $ tek sefer | id475816966 · id391564049 |
| Sesli duyurular (adım adı, yarı yol, kalan süre) | Timer+, Seconds, MultiTimer | iOS | yok (yalnız vakit bildiriminde) | Timer+ Pro | id391564049 · id475816966 |
| Aralığa özel müzik; uyarıda müziği kısma | Seconds | iOS | yok | tek sefer | id475816966 |
| Apple Health kaydı (antrenman/mindful) | Seconds, Forest | iOS | yok | belirtilmemiş | id475816966 · id866450515 |
| Sessizde/Odak'ta çalan zamanlayıcı (iOS 26) | Timer+ | iOS | kısmen (alarm AlarmKit; zamanlayıcı bildirim) | doğrulanamadı | id391564049 |
| Zamanlayıcı geçmişi, iptali geri alma | Timer+, MultiTimer | iOS | yok | Pro | id391564049 |
| Live Activity / Dynamic Island; widget'tan başlat/durdur; Denetim Merkezi, Eylem Düğmesi | Timer+, MultiTimer, Be Focused, Focus To-Do, Session | iOS | yok | çoğunda belirtilmemiş | id391564049 · id973130201 · id966057213 |
| Siri, Kısayollar, App Intents | Timer+, MultiTimer, Be Focused, Session, Stopwatch plus | iOS | yok | belirtilmemiş | id391564049 · id1521432881 |
| Apple Watch uygulaması ve komplikasyonlar | Timer+, Seconds, MultiTimer, Be Focused, Focus To-Do, World Clock Widget, Time Until | watchOS | yok | belirtilmemiş | id391564049 · id912698955 · id1608767848 |
| Zamanlayıcı/rutin paylaşma, başka cihaza aktarma | Timer+, Seconds, MultiTimer | iOS, web | kısmen (sitede ?t= geri sayım) | belirtilmemiş | id391564049 · id475816966 |
| Cihazlar arası eşitleme (iCloud) | MultiTimer, Be Focused, Session, World Clock Widget | iOS, Mac | **yapıldı** (iCloud anahtar-değer) | Be Focused Pro | id973421278 · id973130201 |
| Odak istatistikleri (gün/hafta/ay), seri, etiket, günlük hedef | Forest, Focus To-Do, Be Focused, Flora, Pomofocus, Session | iOS, Android, web | yok | ayrıntılı analiz premium | forestapp.cc · id966057213 · pomofocus.io |
| Pomodoro'ya bağlı görev listesi | Focus To-Do, Be Focused, Pomofocus, Flora | iOS, Android, web | yok | premium kısmen | id966057213 · pomofocus.io |
| Odakta uygulama/site engelleme | Forest, Focus To-Do, Flora, Session | iOS, Android, Mac | yok | Forest Plus kısmen | id866450515 · id1521432881 |
| Beyaz gürültü, ortam sesleri | Forest, Focus To-Do, Pomofocus | iOS, Android, web | yok | jeton/Plus | forestapp.cc · id966057213 |
| Oyunlaştırma: büyüyen ağaç, gerçek ağaç, grup oturumu | Forest, Flora | iOS, Android | kısmen (18 rozet) | ücretsiz + Plus | id866450515 · id1225155794 |
| Oturum niyeti/değerlendirme, takvime yazma, Slack/Todoist | Session, Pomofocus | iOS, Mac, web | yok | 4,99 $/ay | id1521432881 · pomofocus.io |
| Kişisel etkinlik geri sayım listesi (emoji/fotoğraf), olay geçince ileri sayım | Countdown (Sevenlogics), Dreamdays, Time Until, Timer+ | iOS | kısmen (hazır hedefler + tek özel tarih) | temel ücretsiz | apps.apple.com/us/app/id424216726 · id1079298736 · id1608767848 |
| Geri sayım widget'ı (ana/kilit ekranı, StandBy, fotoğraf) | Countdown Widget Premium, Sevenlogics, Time Until | iOS | kısmen (yalnız sıradaki vakit) | yıllık 14,99 $'a kadar | apps.apple.com/mx/app/id6621249393 · id424216726 |
| Yinelenen olaylar ve önceden hatırlatma (1 ay/1 hafta/1 gün) | Sevenlogics, Countdown Widget Premium, Time Until, Dreamdays | iOS | yok | kısmen premium | id424216726 · id6621249393 |
| Telefon takviminden geri sayım oluşturma | Sevenlogics, Time Until | iOS | yok | belirtilmemiş | id424216726 · id1608767848 |
| Geri sayımı görsel kart/bağlantı olarak paylaşma | Countdown Widget Premium, Sevenlogics, Dreamdays | iOS | kısmen (sitede ?t=) | ücretsiz | id6621249393 |
| Uygulamada toplantı planlayıcı (mesai vurgusu, takvime ekleme, paylaşım) | Time Buddy (Helloka), World Clock Widget, World Time Buddy | iOS, web | kısmen (yalnız sitede) | 2,99 $ / abonelik | id719765848 · id912698955 · worldtimebuddy.com |
| Dünya saati widget'ı (8 dilime kadar, kilit ekranı, StandBy, Watch) | World Clock Widget (Lasmit), Time Buddy (MOVEO) | iOS, watchOS | yok | ücretsiz + satın alma | id912698955 · id943431399 |
| Geniş şehir veritabanı (5.000+) ve yaz saati değişiminden 1 hafta önce uyarı | World Clock – Time Zones (timeanddate), Time Buddy (Helloka) | iOS | kısmen (31 şehir + IANA; yaz saati uyarısı yok) | ücretsiz reklamlı | apps.apple.com/us/app/id403693694 · id719765848 |

Öne çıkanlar: 1) Live Activity/Dynamic Island + kilit ekranı denetimleri (zamanlayıcı, pomodoro, kronometre; "sıradaki vakte kalan" Live Activity'si farklılaştırıcı). 2) Çoklu adlı zamanlayıcı, HIIT/Tabata rutinleri, sesli duyurular. 3) Cihazda odak istatistikleri/geçmiş, CSV. 4) Kişisel etkinlik geri sayımı + geri sayım widget'ı (yinelenen, hatırlatmalı). 5) Apple Watch, Siri/Kısayollar (Kısayollar ucuz ilk adım).
İncelenen: Stopwatch plus Multiple Timer (Roman Kotov; "Stopwatch+" adıyla popüler uygulama bulunamadı, Android'de "Stopwatch & Timer+" jupiterapps doğrulanamadı), Timer+, MultiTimer, Seconds, Forest, Flora, Focus To-Do, Be Focused, Session, Pomofocus (web), Countdown (Sevenlogics), Countdown Widget Premium, Countdown Widget – Time Until (ONNEX, Türk), Dreamdays, World Clock – Time Zones (timeanddate), Time Buddy (Helloka), World Clock Widget (Lasmit), Time Buddy (MOVEO), World Time Buddy (web).

## Saat/zaman web siteleri (ajan raporu, 30 Ekim 2026)
Not: timeanddate.com 403 döndü (yardım sayfaları ve arama sonuçlarıyla). Türkçe arama sırası birebir görülemedi; "saat kaç"ta üst sıralar haber siteleri.

| Özellik | Hangi sitede | Bizde | Ücret | Kaynak |
|---|---|---|---|---|
| Çok ülkeli resmî tatil takvimleri | timeanddate, time.is, vclock | kısmen (yalnız Türkiye) | ücretsiz | timeanddate.com/holidays/turkey · time.is/calendar · vclock.com/holidays/ |
| PDF takvim oluşturucu (kendi etkinliği, tatiller, dil) | timeanddate; time.is yazıcı dostu; worldtimeserver | kısmen (tarayıcıdan yazdırma) | ücretsiz; şablon/logo Supporter 5,99 $/3 ay | timeanddate.com/calendar/create.html |
| Takvime aktarma (ICS, Google, Outlook) | worldtimebuddy, everytimezone | yok | ETZ'de Pro | worldtimebuddy.com · everytimezone.com |
| Paylaşılabilir toplantı/etkinlik bağlantısı (bir an, birçok şehir) | worldtimebuddy, everytimezone, worldtimeserver, time.is/compare, timeanddate Event Time Announcer, 24timezones | kısmen (yalnız geri sayımda ?t=) | ücretsiz | worldtimebuddy.com · time.is/compare |
| Toplantı saati oylaması, ekip saat dilimleri | everytimezone Pro | yok | Pro | everytimezone.com |
| Saat dilimi haritası (canlı gece/gündüz) | 24timezones, timeanddate | yok (küre kaldırıldı) | ücretsiz | 24timezones.com · timeanddate.com/time/zone |
| Dünya geneli yaz saati değişim listesi | time.is, timeanddate, worldtimeserver | yok (yalnız Türkiye) | ücretsiz | time.is/DST_changes |
| Saat dilimi haberleri, bülten | time.is, worldtimeserver | yok | ücretsiz | time.is |
| Kısaltma/dönüşüm sayfaları (UTC, GMT, CET, "PST to EST") | time.is, worldtimeserver, 24timezones | yok (yalnız İstanbul–şehir) | ücretsiz | worldtimeserver.com |
| Ay doğuşu/batışı, Ay evresi takvimi, Ay uzaklığı | timeanddate | kısmen (anlık evre, yeni ay/dolunay) | ücretsiz | timeanddate.com/moon/help |
| Tutulmalar (tablo, harita, konumdan sonraki tutulma) | timeanddate | yok | ücretsiz | timeanddate.com/eclipse/in/help |
| Gece gökyüzü, görünen gezegenler | timeanddate | yok | ücretsiz | timeanddate.com/astronomy/night/help.html |
| 14 günlük tahmin, geçmiş hava, iklim | timeanddate | kısmen (7 gün) | ücretsiz | timeanddate.com/weather/ext-help.html |
| Unix saati/çevirici, UTC sayfası | time.is, saatkac.info.tr | yok | ücretsiz | time.is |
| "Tarihte bugün" | time.is, saatkac.info.tr | yok | ücretsiz | time.is |
| Saat hesaplama (saat:dakika) | onlinealarmkur | kısmen (tarih hesaplama var) | ücretsiz | onlinealarmkur.com |
| Ayrı "Kaç gün kaldı / Kaç gün oldu" sayfaları | saat-kac.tr, canlisaat.com | kısmen (tarih hesaplamada) | ücretsiz | saat-kac.tr |
| Uyku, burç, emeklilik hesaplama | saat-kac.tr | yok | ücretsiz | saat-kac.tr |
| Hazır süreli zamanlayıcı sayfaları (/5-dakika/), adrese süre yazma | vclock (10 sn–8 sa), e.ggtimer | kısmen (?t= yalnız tarih) | ücretsiz | vclock.com/timer/ |
| Hazır alarm bağlantıları (07:00 alarm vb.) | vclock, onlinesaat.web.tr, onlinealarmkur | yok | ücretsiz | vclock.com · onlinealarmkur.com |
| Alarm sesi seçimi (11 ses), YouTube alarm sesi, alarm geçmişi/CSV | onlinealarmkur, vclock, timer-tab | kısmen (tek zil) | ücretsiz | onlinealarmkur.com |
| Kronometre turlarını CSV'ye aktarma | wavespeed.ai/tr | kısmen (tur var) | ücretsiz | wavespeed.ai/tr/free-tools/stopwatch |
| Interval zamanlayıcı, satranç saati, metronom | online-stopwatch | yok | ücretsiz + Premium | online-stopwatch.com |
| Sınıf araçları (isim seçici, grup, yarış/sunum zamanlayıcısı) | online-stopwatch | yok | ücretsiz + Premium | online-stopwatch.com |
| Konuşan saat | online-stopwatch | yok | ücretsiz | online-stopwatch.com |
| Temalı tam ekran saatler (hologram, matrix), videolu zamanlayıcı | saat-kac.tr, online-stopwatch | kısmen (19 model; tema/arka plan yok) | ücretsiz | saat-kac.tr |
| Görünüm ayarı (yazı tipi, renk, boyut), paylaşılabilir tema | vclock, onlinesaat.web.tr, timer-tab | kısmen (açık/koyu) | ücretsiz | vclock.com |
| 81 il saat sayfaları | saat-kac.tr | yok (bilerek ertelendi) | ücretsiz | saat-kac.tr |
| Özel gün geri sayımları (Anneler, Babalar, Sevgililer Günü, LGS, okul kapanışı) | saat-kac.tr, canlisaat.com, vclock | kısmen | ücretsiz | saat-kac.tr · vclock.com/timer/ |
| Köprü tatil / izin planı ("kaç gün izinle kaç gün tatil") | tatilsepeti (tablo), haber siteleri (makale); etkileşimli araç yok | yok | ücretsiz | tatilsepeti.com/resmi-tatil-gunleri |
| İndirilebilir tatil takvimi görseli | jibble, turna (doğrulanamadı) | kısmen (imsakiye görseli) | ücretsiz | jibble.io/tr |
| Hesapla ayar/favori eşitleme | worldtimebuddy, time.is, worldtimeserver | uygulamalarda hesapsız eşitleme (iCloud, Android yedeği); sitede yok | ücretsiz üyelik | worldtimebuddy.com |
| Tarayıcı uzantısı | worldtimebuddy, onlinealarmkur, timer-tab | **yapıldı** (`extension/`, Chrome/Edge/Firefox; mağaza yayını sahibin kararıyla) | ücretsiz | addons.opera.com (onlinealarmkur) |
| Çok dil | time.is 58, onlinealarmkur 32, 24timezones 12 | kısmen (5 dil) | ücretsiz | time.is |
| API/veri hizmeti | timeanddate API, worldtimeserver | **yapıldı** (statik açık veri `/api/v1/`, ücretsiz) | ücretli | timeanddate.com/services/api |

Gelgit ve hava uyarıları doğrulanamadı. Para: timeanddate Supporter 5,99 $/3 ay; time.is reklamsız sürüm şu an yeni abone almıyor.
Öne çıkanlar: 1) Hazır süreli zamanlayıcı/alarm sayfaları ("5 dakika zamanlayıcı", "07:00 alarm") — uzun kuyruk SEO, statik üretim ucuz. 2) PDF takvim ve ICS (Türkiye tatil ICS'i veren rakip yok; 2027 takvim aramaları Kasım–Ocak tepe). 3) Köprü tatil/izin planlayıcı + eksik özel gün geri sayımları (Anneler/Babalar/Sevgililer Günü, okul kapanışı). 4) Ay doğuşu/batışı, Ay takvimi, tutulmalar (Meeus hesapları var). 5) Planlayıcı/çeviricide paylaşılabilir bağlantı + takvime ekle; dünya yaz saati listesi; kısaltma/dönüşüm sayfaları (EN/DE trafiği).
İncelenen: time.is, worldtimebuddy, vclock, online-stopwatch, onlinealarmkur, everytimezone, worldtimeserver, 24timezones, saat-kac.tr, canlisaat.com, saatkac.info.tr (time.is benzeri), onlinesaat.web.tr (vclock benzeri), tatilsepeti; arama/yardım ile timeanddate, e.ggtimer, timer-tab.

## Namaz vakti, takvim, Güneş/Ay uygulamaları (ajan raporu, 30 Ekim 2026)
Not: Google Play sayfaları okunamadı; Android bilgisi üçüncü taraf aynalardan (memuplay, tamindir). Kısaltmalar: MP Muslim Pro, EVNV Pro Ezan Vakti Namaz Vakitleri Pro, EV Pro Ezan Vakti Pro (Android), NV-Codeida Namaz Vakti (Codeida), TT Türkiye Takvimi, KuKa Namaz Vakitleri ve Camiler, TPE The Photographer's Ephemeris.

| Özellik | Hangi rakipte | Platform | Bizde | Ücret | Kaynak |
|---|---|---|---|---|---|
| Kıble pusulası (pusula/kamera/harita, hizalanınca titreşim, kıble saati) | MP, Athan, e-Diyanet, NV-Codeida, Vakit, Vakitler, EVNV Pro, EV Pro, Fazilet, Pillars, TT, KuKa, namazvakti.com | iOS, Android, web | yok | çoğunda ücretsiz | apps.apple.com/tr/app/id6748356813 · apps.apple.com/us/app/id852500166 |
| Yakındaki camiler ve yol tarifi; helal restoran, namaz yeri | e-Diyanet, NV-Codeida, Vakit, EV Pro; MP | iOS, Android | yok | ücretsiz | apps.apple.com/us/app/id6745179920 |
| Hicri tarih, Hicri–Miladi çevirici, widget/kilit ekranında Hicri | MP, Athan, e-Diyanet, NV-Codeida, Vakit, Vakitler, Fazilet, EV Pro, EVNV Pro | iOS, Android | yok | ücretsiz (EVNV kilit ekranı Pro) | apps.apple.com/us/app/id505858403 · apps.apple.com/app/id1372054007 |
| Birden çok hesap yöntemi, mezhebe göre ikindi, yüksek enlem düzeltmesi | Vakit (12 yöntem), Pillars, EV Pro | iOS, Android | kısmen (yalnız Diyanet; yüksek enlemde 1/7 kuralı) | ücretsiz | apps.apple.com/us/app/id1559086853 |
| Dünya şehirleri, ilçe seçimi, birden çok kayıtlı konum | Fazilet (7.000+ şehir), EVNV Pro, Athan (20.000+), namazvakti.com; ilçe: NV-Codeida, Vakitler, KuKa | iOS, web | kısmen (81 il merkezi ya da GPS, tek konum) | Fazilet abonelik | apps.apple.com/tr/app/id1326757844 · namazvakti.com |
| Kerahat vakti gösterimi/uyarısı | EVNV Pro, Vakit | iOS | yok | ücretsiz | id1372054007 |
| Vakti/sahuru gerçek alarm olarak kurma (sessizde çalar, iOS 26) | Vakit | iOS | kısmen (vakitte yalnız bildirim) | ücretsiz | id6748356813 |
| Vakitte telefonu kendiliğinden sessize alma | EV Pro | Android | yok | doğrulanamadı | memuplay.com (ezanvakti) |
| Ezan sesi seçenekleri (20+ ses, paketler) | EVNV Pro, Athan, Vakitler, NV-Codeida, EV Pro | iOS, Android | yok (bilerek) | karışık | id1372054007 |
| Dini gün, kandil, bayram, Cuma hatırlatma bildirimleri | e-Diyanet, NV-Codeida, Fazilet | iOS | yok (takvimde var) → bu turda "Özel günler" bildirimi eklendi | ücretsiz / abonelik | id6745179920 |
| Ramazan'a özel iftar sayacı, widget, Live Activity | Vakitler, Athan, Pillars, EVNV Pro | iOS | kısmen (sıradaki vakit widget'ı) | ücretsiz | apps.apple.com/za/app/id6468837330 |
| Live Activity, Dynamic Island, StandBy (gece) | Vakit, Vakitler, Pillars, Lunar | iOS | yok | çoğunda ücretsiz | id6748356813 |
| Apple Watch ve komplikasyonlar | NV-Codeida, Vakit, Vakitler, Fazilet, EVNV Pro, Pillars, MP, Sun Seeker, Lunar, LunarPhase | watchOS | yok (yol haritasında) | karışık | id852500166 |
| Wear OS | EV Pro | Android | yok | doğrulanamadı | memuplay.com |
| iPad ve Mac (menü çubuğunda geri sayım) | Vakit, Fazilet, NV-Codeida | iPadOS, macOS | kısmen (yalnız iPhone; web var) | ücretsiz | id6748356813 |
| Siri, Kısayollar, HomeKit | Vakitler | iOS | yok | ücretsiz | id6468837330 |
| Paylaşım kartları (vakit, dini gün, ayet) | NV-Codeida, TT, KuKa | iOS | kısmen (imsakiye görseli) | ücretsiz | id852500166 |
| Takvim yaprağı (günlük yaprak, arka yüz, günün sözü) | TT, Fazilet, Diyanet Takvimi | iOS, Android | yok | TT ücretsiz, Fazilet yıllık ₺55 | apps.apple.com/tr/app/id6651851291 |
| Kur'an, dua, zikirmatik, hadis, Esmaü'l-Hüsna, namaz rehberi | MP, Athan, e-Diyanet, NV-Codeida, Vakit, EVNV Pro, Fazilet, EV Pro, KuKa | iOS, Android | **yapıldı** (Kur'an Arapça + İngilizce meal + Diyanet bağlantısı, 12 dua, zikirmatik; hadis, Esmaü'l-Hüsna ve namaz rehberi yok) | kısmen premium | apps.apple.com/us/app/id388389451 |
| Namaz/oruç/kaza takibi, seriler | MP, Athan, NV-Codeida, Vakit, EVNV Pro, Pillars | iOS | **yapıldı** (vakit, kaza, Ramazan orucu; seri ve rozet bilerek yok) | ücretsiz | id1559086853 |
| Zekât ve fitre hesaplayıcı | NV-Codeida, Vakit, Athan | iOS | **yapıldı** (zekât, uygulamalar ve site; fitre yok) | ücretsiz | id852500166 |
| Vakit ekranında Güneş/Ay yayı | Vakit, Sun Seeker | iOS | yok | ücretsiz / 11,99 $ | apps.apple.com/us/app/id330247123 |
| Ay doğuş ve batış saatleri (ve yönü) | Lunar, LunarPhase, Lumos, TPE | iOS | yok | Lunar/LunarPhase ücretsiz, TPE 9,99 $ | apps.apple.com/app/id1141720421 · id562061111 |
| Ay takvimi (aylık ızgara, evre, aydınlanma %) ve Ay evresi widget'ı | Lunar, LunarPhase, Lumos | iOS | kısmen (bugünün Ay'ı) | ücretsiz + Pro | id1141720421 |
| Ay olayı bildirimleri (dolunay, yeni ay, dördün, süper ay) | Lunar, LunarPhase, Lumos, TPE | iOS | yok → bu turda dolunay/yeni ay bildirimi eklendi | temel ücretsiz | apps.apple.com/app/id725106192 |
| Tutulmalar (tarihler, harita, bildirim) | Lunar, TPE, Sun Seeker | iOS | yok | ücretli/Pro | apps.apple.com/us/app/id366195670 |
| Altın/mavi saat, alacakaranlık saatleri ve bildirimleri | Sun Seeker, Lumos, TPE | iOS | kısmen (doğuş, batış, gün uzunluğu) | 9,99–14,99 $ | id330247123 |
| Güneş/Ay yolu: AR kamera, azimut, gölge boyu | Sun Seeker, Lumos, TPE | iOS | kısmen (kürede tepe noktası) | ücretli/Pro | id725106192 |
| Hilal görünürlüğü, Ramazan hilal bildirimi | TPE, Athan | iOS | yok | TPE ücretli, Athan ücretsiz | id505858403 |
| Ay evrelerini/tutulmaları telefon takvimine aktarma | Lunar | iOS | kısmen (özel günleri takvime ekleme var) | Pro | id1141720421 |

Öne çıkanlar: 1) Apple Watch ve Wear OS komplikasyonları (Türk rakiplerin çoğunda var; "sıradaki vakte kalan" saate en uygun bilgi). 2) Kıble pusulası ve yakın camiler (13 namaz uygulamasının hepsinde kıble; büyük çember yönü hesabı, ucuz; camiler MapKit aramasıyla). 3) Hicri tarih ve dini gün hatırlatma bildirimleri (Hicri tarih Diyanet'ten bir gün kayabilir). 4) Ay doğuş/batış, Ay takvimi, Ay olayları ve Ay evresi widget'ı (Meeus çekirdeği genişletilebilir). 5) Live Activity, Dynamic Island ve AlarmKit vakit alarmı (iftar/sahur).
Konumlanma: en güçlü doğrudan rakip Vakit (Hakan Çelik): ücretsiz, reklamsız, Apple Watch, Mac, Live Activity, 12 yöntem, 25 dil; "ücretsiz, reklamsız" farkımız ona karşı işlemiyor. İlçe bazında vakit (Diyanet ilçe ilçe yayımlıyor) NV-Codeida, Vakitler, KuKa'da var; bizde yok.
İncelenen: e-Diyanet (Diyanet'in eski "Namaz Vakti" ve "Diyanet Takvimi" App Store sayfaları 404), Muslim Pro, Athan, Ezan Vakti Namaz Vakitleri Pro, Ezan Vakti Pro (Android), Namaz Vakti (Codeida), Vakit, Vakitler, Namaz Vakitleri ve Camiler (KuKa), Pillars, Fazilet Takvimi, Türkiye Takvimi, namazvakti.com, Sun Seeker, Lumos, Lunar, LunarPhase, The Photographer's Ephemeris. Bulunamayan: Saatli Maarif Takvimi uygulaması, "Pusula" adlı namaz uygulaması, güncel "Phases of the Moon".

