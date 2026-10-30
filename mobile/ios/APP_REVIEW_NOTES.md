# App Review notları

Apple'ın 20 Ekim 2026'daki bilgi isteğine (Guideline 2.1 - Information Needed, yeni geliştirici hesabı) verilen yanıt.
Aynı metin App Store Connect › App Review Information › Notes alanına da girilir (Apple sonraki gönderimler için
istedi). Uygulama değişirse (yeni özellik, yeni dış servis) bu metni de güncelleyin. App Store Connect'teki alan en
çok 4000 karakter alır (aşağıdaki metin; `python3 -c` ile sayılır). 30 Ekim 2026: özellik aç/kapa, bildirim
kategorileri, arka plan yenilemesi, arka plan ses modu, ezan sesi, kıble pusulası ve yakındaki camiler (MapKit araması)
eklendi (sıradaki sürüm için); ardından Siri/Kısayollar (App Intents), Live Activity, Apple Watch uygulaması
(WatchConnectivity), hicri tarih ve gece modu; ardından pomodoro arka plan sesi, uyku (yatma hatırlatması, uyku sesleri),
alarm kapatma görevi (VisionKit ile QR/barkod okuma, kamera izni) ve iPad desteği; ardından İbadet bölümü (Kur'an:
Tanzil, Pickthall) ve iCloud anahtar-değer eşitlemesi.

---

Hello, the requested information is below.

1. SCREEN RECORDING
Attached (physical iPhone, starting at launch). No account or login, no user-generated content, no in-app purchases.

2. PURPOSE AND AUDIENCE
Saat Tarih ("Time & Date") is a free clock and calendar utility: time, date, prayer times, holidays and world clocks. Audience: people in Turkey and Turkish, Arabic, German and Azerbaijani speakers who follow prayer times. No ads, no tracking.

3. HOW TO USE
No login or sample files needed.
- First launch shows only the clock; tap the gear > Features to turn on Weather, Calendar, World, Seasons, Prayer and Tools.
- Clock: swipe among 20 faces; tap for seconds; "Edit" picks the items below; full screen (backgrounds, night mode).
- Location: Istanbul by default; tap its name to search a place or "Use my location" (permission asked only then).
- Calendar: holidays; tap a day for details; "Show my phone's calendars" (optional permission).
- Prayer: times, countdown, monthly and Ramadan tables, Qibla compass, nearby mosques (Apple Maps search); Worship: Quran (Arabic, English translation), duas, dhikr counter, prayer/fasting tracker, zakat calculator.
- Tools: alarm (optional dismiss task: maths, shake, steps or a saved QR/barcode; wake-up check), timers, interval workout, countdowns, stopwatch, pomodoro (stats, background noise), sleep (bedtime reminder, sleep sounds), date calculator, badges.
- Apple Watch app and complications: time, Hijri date, prayer times. Siri/Shortcuts: timer, alarm, full-screen clock, next prayer, date.
Runs on iPhone and iPad. The language follows the device.

4. NOTIFICATIONS AND BACKGROUND MODES
- Local notifications (asked once; each category can be turned off in Settings > Notifications): prayer times, special days, seasons, moon, sunrise/sunset, morning weather, DST, bedtime, hourly chimes (off by default).
- Background App Refresh: moves this local schedule forward.
- Audio (background mode): only during a session the user starts (interval workout voice cues, pomodoro noise, sleep sounds generated on device), so it continues with the screen locked; mixes with music; stops when it ends.
- AlarmKit: alarms and timers. For an alarm with a dismiss task, Stop opens the app to the task; if not done, it rings again a minute later (max 10).
- Live Activities: running timers, pomodoro, stopwatch and (optional) the next prayer.

5. EXTERNAL SERVICES
- Apple WeatherKit: weather. Every WeatherKit view shows the Apple Weather mark and "Legal" link (Weather tab, weather clock faces and item, globe, morning card); the weather notification ends with " Weather".
- Apple MapKit: the globe and the nearby mosques search (search word and an approximate area).
- On-device: Core Location, EventKit (both optional), PhotosPicker, VisionKit scanner (camera only for an alarm's code; image not stored), UserNotifications, AlarmKit, ActivityKit, App Intents, AVSpeechSynthesizer, WatchConnectivity.
- iCloud key-value storage: syncs settings and personal records among the user's own devices (switch in Settings).
- Bumba mailing list (bumbagroup.com): only if the user enters an email in About, with explicit consent.
No other servers: prayer times, qibla and sun/moon data are calculated on the device. No analytics, ads or AI.

6. REGIONAL DIFFERENCES
Same in all regions; prayer times work anywhere; the holiday calendar shows Turkey's holidays; weather depends on WeatherKit coverage.

7. REGULATED INDUSTRY / THIRD-PARTY MATERIAL
No regulated services. Prayer times are informational, calculated with the method of Turkey's Presidency of Religious Affairs (Diyanet); the app states Diyanet's official times are authoritative. Optional 5-second adhan sound: excerpt of a Wikimedia Commons recording (CC BY-SA 4.0); district coordinates: GeoNames (CC BY 4.0); Quran text: Tanzil Project (CC BY 3.0); Pickthall translation (public domain); all credited in the app.

Developer: Bugra Bilim - bilgi@bumbagroup.com - https://time.bumba.tr
