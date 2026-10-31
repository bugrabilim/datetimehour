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
(yüzen saat), vakitte AlarmKit alarmı, dokunulabilen widget'lar ve Denetim Merkezi düğmeleri, hesap yöntemleri.

---

Hello, the requested information is below.

1. SCREEN RECORDING
Attached (physical iPhone, starting at launch). No account or login, no user-generated content, no in-app purchases.

2. PURPOSE AND AUDIENCE
Saat Tarih ("Time & Date") is a free clock and calendar utility: time, date, prayer times, holidays and world clocks. Audience: Turkish, Arabic, German and Azerbaijani speakers who follow prayer times. No ads, no tracking.

3. HOW TO USE
No login or sample files needed.
- First launch shows only the clock; gear > Features turns on Weather, Calendar, World, Seasons, Prayer, Tools.
- Clock: swipe among 20 faces; tap for seconds; "Edit" picks the items below; full screen (backgrounds, night mode).
- Location: Istanbul by default; tap its name to search a place or "Use my location" (permission asked only then).
- Calendar: holidays; tap a day for details; "Show my phone's calendars" (optional permission).
- Prayer: times, monthly/Ramadan tables, Qibla compass, nearby mosques (Apple Maps search), Quran (Arabic, English translation), duas, dhikr counter, prayer tracker, zakat.
- Tools: alarm (optional dismiss task, e.g. maths, shake, QR code or photo; wake-up check), timers, workout routines, countdowns, stopwatches, pomodoro, sleep, morning journal, meeting planner, badges.
- Floating clock (Settings > Clock appearance): picture-in-picture time window.
- Apple Watch app/complications; Siri/Shortcuts (timer, alarm, full-screen clock, next prayer, date).
Runs on iPhone and iPad. The language follows the device.

4. NOTIFICATIONS AND BACKGROUND MODES
- Local notifications (asked once; each category can be turned off): prayer times, special days, seasons, moon, sun, morning weather, DST, bedtime, hourly chimes (off by default).
- Background App Refresh: moves this local schedule forward.
- Audio background mode: only during a session the user starts (workout voice cues, pomodoro/sleep sounds made on device) so it continues when locked; mixes with music.
- AlarmKit: alarms, timers, optional prayer-time alarms. With a dismiss task, Stop opens the app to the task; if not done it rings again a minute later (max 10).
- Picture in Picture: the floating clock, only while the user keeps it open.
- Live Activities: timers, pomodoro, stopwatch, (optional) next prayer. Interactive widgets: tasbih counter, stopwatch. Control Center buttons (iOS 18) open the app.

5. EXTERNAL SERVICES
- Apple WeatherKit: weather. Every WeatherKit view shows the Apple Weather mark and "Legal" link (Weather tab, clock faces, globe, morning card, weather background); the weather notification ends with " Weather".
- Apple MapKit: the globe and the nearby mosques search (search word and an approximate area).
- On-device: Core Location, EventKit (optional), PhotosPicker, camera (only for an alarm's QR code or photo task; images not stored), notifications, ActivityKit, App Intents, speech, WatchConnectivity.
- iCloud key-value storage: syncs settings and personal records among the user's own devices (switch in Settings).
- HealthKit (optional, off by default): writes a finished interval workout; reads nothing.
- Bumba mailing list (bumbagroup.com): only if the user enters an email in About, with explicit consent.
No other servers: prayer times, qibla and sun/moon data are calculated on the device. No analytics, ads or AI.

6. REGIONAL DIFFERENCES
Same everywhere; the holiday calendar shows Turkey's holidays; weather depends on WeatherKit coverage.

7. REGULATED INDUSTRY / THIRD-PARTY MATERIAL
No regulated services. Prayer times are informational (Turkey's Diyanet method by default, others selectable); the app states Diyanet's official times are authoritative. Optional 5-second adhan sound: excerpt of a Wikimedia Commons recording (CC BY-SA 4.0); district coordinates: GeoNames (CC BY 4.0); Quran text: Tanzil Project (CC BY 3.0); Pickthall translation (public domain); all credited in the app.

Developer: Bugra Bilim - bilgi@bumbagroup.com - https://time.bumba.tr
