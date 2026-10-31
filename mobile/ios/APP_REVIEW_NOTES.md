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
kaydı (CC BY 3.0). Odakta engelleme (FamilyControls) Apple onayına kadar gizli olduğu için metinde yok.

---

Hello, the requested information is below.

1. SCREEN RECORDING
Attached (physical iPhone, from launch). No account, no user-generated content, no in-app purchases.

2. PURPOSE AND AUDIENCE
Saat Tarih ("Time & Date") is a free clock and calendar utility: time, date, prayer times, holidays, world clocks. Audience: Turkish, Arabic, German and Azerbaijani speakers who follow prayer times. No ads or tracking.

3. HOW TO USE
No login or sample files needed.
- First launch shows only the clock; gear > Features turns on Weather, Calendar, World, Seasons, Prayer, Tools.
- Clock: 20 faces (swipe); tap for seconds; "Edit" picks items below; full screen (backgrounds, night mode).
- Location: Istanbul by default; tap its name to search or "Use my location" (permission asked only then).
- Calendar: holidays, day details, "Show my phone's calendars" (optional).
- Prayer: times, monthly/Ramadan tables, Qibla compass, nearby mosques (Apple Maps search), Quran (Arabic, English translation), duas, dhikr counter, prayer tracker, zakat.
- Tools: alarm (song from the user's library or Files; dismiss task, e.g. maths, shake, QR or photo; wake-up check; alarm for a calendar event; arrive/leave place reminders), timers, workout routines, countdowns, stopwatches, pomodoro, sleep, morning journal, meeting planner, badges.
- Floating clock: picture-in-picture time window.
- Apple Watch app; Siri/Shortcuts (timer, alarm, full-screen clock, next prayer, date).
iPhone and iPad; language follows the device.

4. NOTIFICATIONS AND BACKGROUND MODES
- Local notifications (asked once; each category can be turned off): prayer times, special days, seasons, moon, sun, morning weather, DST, bedtime, hourly chimes (off by default).
- Background App Refresh: moves this local schedule forward. Optional place reminders use location notification triggers (iOS watches the region).
- Audio background mode: only during a session the user starts (workout voice cues, pomodoro/sleep sounds) so it continues when locked; mixes with music.
- AlarmKit: alarms, timers, optional prayer-time alarms. With a dismiss task, Stop opens the app to the task; if not done it rings again a minute later (max 10).
- Picture in Picture: the floating clock, only while the user keeps it open.
- Live Activities: timers, pomodoro, stopwatch, (optional) next prayer. Interactive widgets: tasbih counter, stopwatch. Control Center buttons (iOS 18) open the app.

5. EXTERNAL SERVICES
- Apple WeatherKit: weather. Every WeatherKit view shows the Apple Weather mark and "Legal" link (Weather tab, clock faces, globe, morning card, weather background); the weather notification ends with " Weather".
- Apple MapKit: the globe and the nearby mosques search (search word and an approximate area).
- On-device: Core Location, EventKit, PhotosPicker, MediaPlayer (alarm song; a 30 s excerpt is saved on device), camera (alarm QR/photo task; images not stored), ActivityKit, App Intents, speech, WatchConnectivity.
- iCloud key-value storage: syncs settings and personal records among the user's own devices (switch in Settings).
- HealthKit (optional, off by default): writes a finished interval workout; reads nothing.
- Bumba mailing list: only if the user enters an email in About, with consent.
No other servers: prayer times, qibla, sun/moon are calculated on device. No analytics, ads or AI.

6. REGIONAL DIFFERENCES
Same everywhere; holidays are Turkey's; weather depends on WeatherKit coverage.

7. REGULATED INDUSTRY / THIRD-PARTY MATERIAL
No regulated services. Prayer times are informational (Turkey's Diyanet method by default, others selectable); the app states Diyanet's official times are authoritative. Optional adhan sounds: excerpts of Wikimedia Commons recordings (CC BY-SA 4.0, CC BY 3.0); district coordinates: GeoNames (CC BY 4.0); Quran text: Tanzil Project (CC BY 3.0); Pickthall translation (public domain); all credited in the app.

Developer: Bugra Bilim - bilgi@bumbagroup.com - https://time.bumba.tr
