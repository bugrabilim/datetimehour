# App Review notları

Apple'ın 20 Ekim 2026'daki bilgi isteğine (Guideline 2.1 - Information Needed, yeni geliştirici hesabı) verilen yanıt.
Aynı metin App Store Connect › App Review Information › Notes alanına da girilir (Apple sonraki gönderimler için
istedi). Uygulama değişirse (yeni özellik, yeni dış servis) bu metni de güncelleyin. App Store Connect'teki alan en
çok 4000 karakter alır (aşağıdaki metin; `python3 -c` ile sayılır). 30 Ekim 2026: özellik aç/kapa, bildirim
kategorileri, arka plan yenilemesi, arka plan ses modu ve ezan sesi eklendi (sıradaki sürüm için).

---

Hello, thank you for the review. The requested information is below. A screen recording made on a physical iPhone (latest iOS), starting with launching the app, is attached.

1. SCREEN RECORDING
Attached. No account or login, no user-generated content, no paid content or in-app purchases.

2. PURPOSE AND AUDIENCE
Saat Tarih ("Time & Date") is a free clock and calendar utility: time, date, prayer times, holidays and world clocks in one app. Audience: everyday users in Turkey and Turkish, Arabic, German and Azerbaijani speakers worldwide, especially people who follow daily prayer times and Turkey's holiday calendar. No ads, no tracking.

3. HOW TO USE
No login or sample files needed.
- First launch shows only the clock. To see every feature, tap the gear on the clock screen > Features and turn on Weather, Calendar, World, Seasons, Prayer and Tools; each then appears in the bottom bar.
- Clock: swipe to change among 20 faces; tap the clock for seconds; "Edit" picks the items below; full-screen button (optional photo or animated background).
- Location: Istanbul by default; tap the location name to search a province or district, or "Use my location" (permission asked only then).
- Calendar: holidays; tap a day for details; "Show my phone's calendars" (optional permission).
- Prayer: today's times and countdown, monthly table, Ramadan timetable.
- Tools: alarm, timers, interval workout, countdowns, stopwatch, pomodoro, date calculator, badges.
The language follows the device.

4. NOTIFICATIONS AND BACKGROUND MODES
- Notifications (permission asked once at first launch; each category can be turned off in Settings > Notifications): prayer times, special days, season starts, moon phases, sunrise/sunset, a morning weather summary, daylight saving changes and hourly chimes (off by default). All are local notifications scheduled on the device.
- Background App Refresh (fetch): only moves this local notification schedule forward when the app has not been opened for a while.
- Audio (background mode): used only while an interval workout the user started is running, to speak phase changes ("Work", "Rest", "Last round") and the 3-2-1 countdown with the screen locked or over other apps. It mixes with music and stops when the workout is paused, reset or finished.
- AlarmKit: alarms and timers ring at the set time (optional gradual volume uses a bundled sound).

5. EXTERNAL SERVICES
- Apple WeatherKit: weather. Every place that shows WeatherKit data displays the Apple Weather mark and a "Legal" link: Weather tab, weather clock faces and clock-screen item, the globe, the morning card; the weather notification ends with " Weather".
- Apple MapKit: the globe.
- On-device frameworks: Core Location (optional), EventKit (optional), PhotosPicker (no photo library permission), UserNotifications, AlarmKit, AVSpeechSynthesizer.
- Bumba mailing list (bumbagroup.com): only if the user enters an email in About, with explicit consent.
No other servers: prayer times and sun/moon data are calculated on the device. No analytics, ads or AI services.

6. REGIONAL DIFFERENCES
Same in all regions. Prayer times work for any location. The holiday calendar shows Turkey's holidays for everyone. Weather depends on WeatherKit coverage.

7. REGULATED INDUSTRY / THIRD-PARTY MATERIAL
No regulated services. Prayer times are informational, calculated with the method of Turkey's Presidency of Religious Affairs (Diyanet); the app states Diyanet's official times are authoritative. Optional 5-second adhan notification sound: an excerpt of an openly licensed recording from Wikimedia Commons (CC BY-SA 4.0), credited in the app. District coordinates: GeoNames (CC BY 4.0), credited in the app.

Developer: Bugra Bilim - bilgi@bumbagroup.com - https://time.bumba.tr
