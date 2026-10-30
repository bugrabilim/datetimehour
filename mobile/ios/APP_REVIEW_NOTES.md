# App Review notları

Apple'ın 20 Ekim 2026'daki bilgi isteğine (Guideline 2.1 - Information Needed, yeni geliştirici hesabı) verilen yanıt.
Aynı metin App Store Connect › App Review Information › Notes alanına da girilir (Apple sonraki gönderimler için
istedi). Uygulama değişirse (yeni özellik, yeni dış servis) bu metni de güncelleyin. App Store Connect'teki alan en
çok 4000 karakter alır (aşağıdaki metin; `python3 -c` ile sayılır). 30 Ekim 2026: özellik aç/kapa, bildirim
kategorileri, arka plan yenilemesi, arka plan ses modu, ezan sesi, kıble pusulası ve yakındaki camiler (MapKit araması)
eklendi (sıradaki sürüm için); ardından Siri/Kısayollar (App Intents), Live Activity, Apple Watch uygulaması
(WatchConnectivity), hicri tarih ve gece modu.

---

Hello, thank you for the review. The requested information is below. A screen recording made on a physical iPhone (latest iOS), starting with launching the app, is attached.

1. SCREEN RECORDING
Attached. No account or login, no user-generated content, no paid content or in-app purchases.

2. PURPOSE AND AUDIENCE
Saat Tarih ("Time & Date") is a free clock and calendar utility: time, date, prayer times, holidays and world clocks. Audience: users in Turkey and Turkish, Arabic, German and Azerbaijani speakers worldwide who follow prayer times and Turkey's holidays. No ads, no tracking.

3. HOW TO USE
No login or sample files needed.
- First launch shows only the clock. To see every feature, tap the gear on the clock screen > Features and turn on Weather, Calendar, World, Seasons, Prayer and Tools; each then appears in the bottom bar.
- Clock: swipe among 20 faces; tap for seconds; "Edit" picks the items below; full screen (photo/animated background, night mode).
- Location: Istanbul by default; tap the location name to search a province or district, or "Use my location" (permission asked only then).
- Calendar: holidays; tap a day for details; "Show my phone's calendars" (optional permission).
- Prayer: today's times and countdown, monthly table, Ramadan timetable, Qibla compass (no extra permission) and nearby mosques (Apple Maps search).
- Tools: alarm, timers, interval workout, countdowns, stopwatch, pomodoro, date calculator, badges.
- Apple Watch app and complications: time, Hijri date, prayer times (place from the iPhone or the watch's location).
- Siri/Shortcuts: start a timer, set an alarm, open the full-screen clock, next prayer, today's date.
The language follows the device.

4. NOTIFICATIONS AND BACKGROUND MODES
- Local notifications (permission asked once; each category can be turned off in Settings > Notifications): prayer times, special days, seasons, moon phases, sunrise/sunset, morning weather, DST changes, hourly chimes (off by default).
- Background App Refresh (fetch): moves this local schedule (and the next-prayer Live Activity) forward when the app has not been opened.
- Audio (background mode): only while a user-started interval workout runs, to speak phase changes and the 3-2-1 countdown with the screen locked; mixes with music, stops when the workout is paused or ends.
- AlarmKit: alarms and timers ring at the set time (optional gradual volume uses a bundled sound).
- Live Activities: running timers, pomodoro, stopwatch and, if turned on in Prayer, the next prayer on the Lock Screen.

5. EXTERNAL SERVICES
- Apple WeatherKit: weather. Every WeatherKit view shows the Apple Weather mark and "Legal" link (Weather tab, weather clock faces and item, globe, morning card); the weather notification ends with " Weather".
- Apple MapKit: the globe and the nearby mosques search (search word and an approximate area).
- On-device: Core Location (optional), EventKit (optional), PhotosPicker (no library permission), UserNotifications, AlarmKit, ActivityKit, App Intents, AVSpeechSynthesizer, WatchConnectivity (sends the selected place to the user's own watch).
- Bumba mailing list (bumbagroup.com): only if the user enters an email in About, with explicit consent.
No other servers: prayer times, qibla and sun/moon data are calculated on the device. No analytics, ads or AI.

6. REGIONAL DIFFERENCES
Same in all regions. Prayer times work for any location. The holiday calendar shows Turkey's holidays for everyone. Weather depends on WeatherKit coverage.

7. REGULATED INDUSTRY / THIRD-PARTY MATERIAL
No regulated services. Prayer times are informational, calculated with the method of Turkey's Presidency of Religious Affairs (Diyanet); the app states Diyanet's official times are authoritative. Optional 5-second adhan sound: excerpt of a Wikimedia Commons recording (CC BY-SA 4.0); district coordinates: GeoNames (CC BY 4.0); both credited in the app.

Developer: Bugra Bilim - bilgi@bumbagroup.com - https://time.bumba.tr
