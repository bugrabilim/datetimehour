# App Review notları

Apple'ın 20 Ekim 2026'daki bilgi isteğine (Guideline 2.1 - Information Needed, yeni geliştirici hesabı) verilen yanıt.
Aynı metin App Store Connect › App Review Information › Notes alanına da girilir (Apple sonraki gönderimler için
istedi). Uygulama değişirse (yeni özellik, yeni dış servis) bu metni de güncelleyin. App Store Connect'teki alan en
çok 4000 karakter alır (aşağıdaki metin; `python3 -c` ile sayılır). 30 Ekim 2026: özellik aç/kapa, bildirim
kategorileri, arka plan yenilemesi, arka plan ses modu, ezan sesi, kıble pusulası ve yakındaki camiler (MapKit araması)
eklendi (sıradaki sürüm için); ardından Siri/Kısayollar (App Intents), Live Activity, Apple Watch uygulaması
(WatchConnectivity), hicri tarih ve gece modu; ardından pomodoro arka plan sesi, uyku (yatma hatırlatması, uyku sesleri),
alarm kapatma görevi (VisionKit ile QR/barkod okuma, kamera izni) ve iPad desteği; ardından İbadet bölümü (Kur'an:
Tanzil, Pickthall) ve iCloud anahtar-değer eşitlemesi. 31 Ekim 2026: HealthKit (yalnız yazma, isteğe bağlı), resim içinde resim
(yüzen saat), vakitte AlarmKit alarmı, dokunulabilen widget'lar ve Denetim Merkezi düğmeleri, hesap yöntemleri;
ardından müzikle alarm (Müzik arşivi izni), konum hatırlatıcıları (bölge bildirimi), takvim etkinliğine alarm, ikinci ezan
kaydı (CC BY 3.0); ardından akıllı alarm (ivmeölçer yalnız alarm penceresinde, uygulama açıkken; yeni izin yok) ve
15 dil (hedef kitle cümlesi). 3 Kasım 2026: Seçenek B gezinme (Tümü), Open-Meteo (hava kalitesi, model karşılaştırma, saat komplikasyonu). 4 Kasım 2026: yeni alarm görevleri (squat, nesne bulma: Vision; sesli okuma: yalnız cihaz içi konuşma tanıma, mikrofon izni), kıble kamera görünümü. Odakta engelleme (FamilyControls) Apple onayına kadar gizli olduğu için metinde yoktu; 4 Kasım 2026'da onay geldi, Ekran Süresi maddesi eklendi (yer açmak için bazı satırlar kısaltıldı; takvimde 30 ülke). Aynı gün Dynamic Island'daki canlı saat kalktı, yerine isteğe bağlı hava etkinliği (WeatherKit, işaretli). 10 Kasım 2026: isteğe bağlı hesap (e-posta + şifre, hesap sunucusu, Resend), ilk kurulum soruları, konum saat diliminden; hesap ekranları olan sürümde App Store Connect › App Review Information › Sign-In Information'a bir deneme hesabı girilir (e-posta ve şifre depoya yazılmaz).

---

1. SCREEN RECORDING
Attached (physical iPhone, from launch). No purchases; account optional (3).

2. PURPOSE AND AUDIENCE
Saat Tarih ("Time & Date") is a free clock and calendar utility: time, date, prayer times, holidays, world clocks. Audience: people who follow prayer times; 19 languages. No ads or tracking.

3. HOW TO USE
No login needed. Optional email+password account (Settings > Account; no feature needs it yet), deletable in the app; demo account in the sign-in fields.
- First launch: short setup questions pick sections (up to 3 in the tab bar, the rest and Settings under "All", with search).
- Clock: many faces (swipe); tap for seconds; full screen (backgrounds, night mode).
- Location: from the device time zone; tap its name to search or "Use my location".
- Calendar: holidays, day details, optional phone calendars.
- Prayer: times, monthly/Ramadan tables, Qibla compass, nearby mosques (Apple Maps search), Quran, duas, dhikr counter, prayer tracker, zakat.
- Tools: alarm (song from library or Files; dismiss tasks: maths, squats, QR, photo, object, read-aloud; wake-up check; place reminders), timers, workouts, countdowns, pomodoro, sleep (smart alarm: motion sensor, only in the alarm window), journal, badges.
- Apple Watch app; Siri/Shortcuts.
iPhone and iPad; language: device or Settings.

4. NOTIFICATIONS AND BACKGROUND MODES
- Local notifications (asked once; each category can be turned off): prayer times, special days, morning weather, hourly chimes (off by default).
- Background App Refresh: moves this local schedule forward. Optional place reminders use location notification triggers (iOS watches the region).
- Audio background mode: only in a session the user starts (workout cues, pomodoro/sleep sounds); mixes with music.
- AlarmKit: alarms, timers, optional prayer-time alarms. With a dismiss task, Stop opens the app to the task; if not done it rings again a minute later (max 10).
- Picture in Picture: floating clock, while the user keeps it open.
- Screen Time (Family Controls, individual; asked when turned on): optional "Block while focusing" shields apps the user picks during their pomodoro; a ringing alarm with a dismiss task blocks deleting the app. Tokens stay on device.
- Live Activities (timers, prayer, weather with mark), widgets, Control Center buttons.

5. EXTERNAL SERVICES
- Apple WeatherKit: weather. Every WeatherKit view shows the Apple Weather mark and "Legal" link; the weather notification ends with " Weather".
- Open-Meteo (no key): air quality, model comparison, photo light, Watch complication; coordinates rounded to 2 decimals; credited.
- Apple MapKit: the globe and nearby mosques search (search word, approximate area).
- On-device: Core Location, EventKit, PhotosPicker, MediaPlayer, camera + Vision (alarm tasks, qibla camera; images not stored), on-device speech recognition (read-aloud task), WatchConnectivity.
- iCloud key-value storage: syncs settings and records among the user's own devices (switch in Settings).
- HealthKit (optional, off by default): writes a finished interval workout; reads nothing.
- Our account server (hesap.time.bumba.tr), only with an account: email, display name, password hash; emails via Resend.
- Bumba mailing list: only if the user enters an email in About, with consent.
No other servers: prayer times, qibla and sun/moon are computed on device. No analytics, ads, AI.

6. REGIONAL DIFFERENCES
Same everywhere; holidays: Turkey plus 32 selectable countries; weather depends on WeatherKit coverage.

7. REGULATED INDUSTRY / THIRD-PARTY MATERIAL
No regulated services. Prayer times are informational (method by region, others selectable); the app says official times are the authority's. Adhan sounds: Wikimedia Commons (CC BY-SA 4.0, CC BY 3.0); district coordinates: GeoNames (CC BY 4.0); Quran text: Tanzil Project (CC BY 3.0); Pickthall translation (public domain); all credited in the app.

Developer: Bugra Bilim - bilgi@bumbagroup.com - https://time.bumba.tr
