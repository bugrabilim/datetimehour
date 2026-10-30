# Google Play Console beyanları (Android uygulaması)

Play'e ilk yüklemeden önce Play Console › Uygulama içeriği (App content) bölümünde doldurulacak beyanlar.
Yapıştırılacak metinler İngilizcedir (Play incelemesi İngilizce okur). Uygulama değişirse (yeni izin, yeni hizmet) bu
dosya da güncellenir. Hazırlanma: 30 Ekim 2026.

## 1. Ön plan hizmeti izinleri (Foreground service permissions)

Uygulama `FOREGROUND_SERVICE_SPECIAL_USE` kullanır: `HiitService` (aralık antrenmanı). Manifestte alt tür
`PROPERTY_SPECIAL_USE_FGS_SUBTYPE` yazılıdır.

- **Tür (type):** Special use
- **Açıklama (description), yapıştır:**

> Interval workout timer. When the user starts an interval (HIIT) workout in Tools › Timer › Interval, a foreground
> service runs only for the duration of that workout. It speaks the phase changes ("Work", "Rest", "Last round") and
> the 3-2-1 countdown with the device's text-to-speech engine, so the user hears the cues while the screen is off or
> while another app (for example a music app) is in front. The ongoing notification shows the current phase, round
> and remaining time. The service stops as soon as the workout is paused, reset or finished. It does not collect or
> send any data.

- **Kullanıcıya etkisi (user impact if deferred or interrupted), yapıştır:**

> If the service were deferred or stopped, the user would miss the spoken phase changes in the middle of a workout
> and would have to keep the screen on and watch it, which is the exact problem the feature solves. Phase timing
> must be accurate to the second.

- **Neden başka bir tür değil (why other types don't fit):**

> There is no dedicated foreground service type for workout timers. "health" requires body sensor or activity
> recognition permissions, which the app does not use; "mediaPlayback" is for media playback, not short spoken
> cues; "shortService" is limited to about 3 minutes, while workouts typically last 10–40 minutes.

- **Video:** Play kısa bir video bağlantısı ister (YouTube'da "liste dışı" olabilir). Çekilecek akış: Araçlar ›
  Zamanlayıcı › Aralık › Başlat → ekranı kilitle → evre geçişinde sesli duyuru duyulur, bildirimde evre ve kalan süre
  görünür → kilidi aç, Duraklat → bildirim kalkar. 30–60 sn yeterli.

### 1b. Kademeli alarm sesi (mediaPlayback)

Uygulama `FOREGROUND_SERVICE_MEDIA_PLAYBACK` da kullanır: `AlarmRingService`, kullanıcının "Kademeli ses" seçtiği
alarm çalarken sesi kısıktan tam sese yükselterek çalar.

- **Tür:** Media playback
- **Açıklama, yapıştır:**

> Gradual alarm volume. When an alarm the user set with the "Gradual volume" option goes off, a foreground service
> plays the alarm sound (the user's chosen ringtone, alarm audio stream) and raises its volume from quiet to full over
> 30 seconds, because a notification channel's sound cannot change volume. It runs only while that alarm is ringing
> and stops when the user taps Stop or Snooze, or after 10 minutes. No data is collected.

- **Kullanıcıya etkisi, yapıştır:**

> Without the service the alarm the user set would not ring audibly with the chosen gradual volume; an alarm clock
> must ring at the exact time even while the phone is locked.

- **Video:** Araçlar › Alarm › yeni alarm, "Kademeli ses" açık, 1 dk sonrasına kur → kilit ekranında alarm açılır, ses
  yükselir → Kapat. 30–60 sn yeterli.

## 2. Tam ekran bildirim izni (Full-screen intent)

Uygulama `USE_FULL_SCREEN_INTENT` kullanır: alarm çalınca kilit ekranında `AlarmActivity` açılır.

- **Seçenek:** "Alarm clock" (kullanıcının kurduğu alarmlar ve zamanlayıcılar)
- **Açıklama, yapıştır:**

> The app is a clock with user-set alarms and timers (Tools › Alarm, Timer). When an alarm or timer the user set
> goes off, a full-screen alarm screen is shown on the lock screen with Stop and Snooze buttons. Full-screen intents
> are used only for these user-set alarms.

## 3. Tam zamanlı alarm (exact alarm)

`SCHEDULE_EXACT_ALARM` kullanıcının açtığı izindir (Android 14'te varsayılan kapalı, uygulama kırmızı satırla ayara
götürür); `USE_EXACT_ALARM` bilerek yok. Play beyanı gerekmez. Sorulursa: alarmlar, zamanlayıcılar ve vakit
bildirimleri tam zamanında çalmalıdır.

## 4. Veri güvenliği (Data safety) özeti

Sitedeki gizlilik sayfasının "Android uygulaması" bölümüyle aynı olmalı:

- **Yaklaşık konum:** toplanır (hava durumu ve yakındaki camiler için iki ondalığa yuvarlanmış koordinat Open-Meteo'ya
  ve OpenStreetMap Overpass'a gider), amaç uygulama işlevi, paylaşılmaz, kimlikle bağlantısız, isteğe bağlı ("Konumumu kullan").
- **E-posta adresi:** yalnız Hakkında'daki listeye katılan için toplanır, amaç geliştiricinin iletişimi/pazarlama,
  isteğe bağlı.
- Kıble yönü ve pusula telefonda hesaplanır; yakındaki camiler aramasındaki koordinat yukarıdaki "yaklaşık konum"
  beyanına girer (aynı amaç).
- Takvim etkinlikleri, fotoğraflar, rozetler ve tercihler telefondan çıkmaz: "toplanmaz".
- Veri aktarımı şifreli (HTTPS). Silme: uygulama silinince yerel veriler silinir; e-posta listesinden çıkış
  e-postadaki bağlantıyla.
- Reklam yok, izleme yok, istatistik yok.

Gizlilik politikası adresi: https://time.bumba.tr/gizlilik/ (İngilizce: https://time.bumba.tr/en/privacy/).

## 5. Wear OS, kısayollar ve ekran koruyucu (30 Ekim 2026, sıradaki sürüm)

- **Wear OS uygulaması** (`mobile/android/wear`, paket adı telefonla aynı `tr.bumba.time`, minSdk 30): Play Console'da
  "Form faktörleri" › Wear OS eklenir, Wear OS ekran görüntüleri (`wear-saat.png` CI artifact'inde) ve ayrı sürüm
  (Wear OS kanalı) yüklenir. Bağımsız çalışır (`com.google.android.wearable.standalone` = true). İzin: yalnız yaklaşık
  konum ("Konumum" seçilirse, saatte hesap için; hiçbir yere gönderilmez). Kutucuk ve iki komplikasyon (sıradaki vakit,
  hicri tarih) internet kullanmaz.
- **Saat uygulaması istekleri** (`ClockIntentActivity`: SET_TIMER, SET_ALARM, SHOW_ALARMS, SHOW_TIMERS): çağıranın
  `com.android.alarm.permission.SET_ALARM` iznine sahip olması gerekir (AOSP saat uygulamasındaki gibi); yeni izin
  istemez, beyan gerekmez.
- **Ekran koruyucu** (`ClockDreamService`, BIND_DREAM_SERVICE) ve **süren sayaç bildirimleri** (kanal `live`):
  yeni izin yok.
- Veri güvenliği beyanı değişmez: saat tarafındaki konum cihazdan çıkmaz ("toplanmaz").
