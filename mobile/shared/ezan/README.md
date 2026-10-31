# Ezan sesleri (vakit bildirimi, ilk cümle)

Vakit bildirimlerinde "Ezan" ses seçeneği, vaktinde bir ezanın ilk cümlesini ("Allāhu akbar, Allāhu akbar") çalar.
"Ezan sesi" seçimi (`notify.adhanVoice`, iOS ve Android aynı kimlikler; rakip analizi listesi #92, 31 Ekim 2026):

| Kimlik | Dosyalar | Kayıt | Lisans |
|---|---|---|---|
| `azeez` (varsayılan) | `ezan.caf`, `res/raw/ezan.ogg` | Aaqib Azeez, 5 sn | CC BY-SA 4.0 |
| `medina` | `ezan-medina.caf`, `res/raw/ezan_medina.ogg` | Medine, Mescid-i Nebevî (kaydedenin beyanı), 17,7 sn | CC BY 3.0 |

Android'de ezan, ses başına ayrı bildirim kanalının sesidir (`prayer-adhan-<kimlik>`): sistem kaydı sonuna kadar çalar,
sessiz modda ve Rahatsız Etmeyin'de çalmaz. Yeni kayıt eklemek: dosyaları üret, iOS `AdhanVoice`, Android
`NotifyPrefs.ADHAN_VOICES` ve `NotificationSounds.adhanFile` / `PrayerNotifier.adhanChannel`, metinler
`notify.adhan.<kimlik>` ve `notify.adhan.credit.<kimlik>` (app-strings.json).

## Aaqib Azeez (`azeez`)

- **Kaynak:** "The Adhan - Muslim Call to Prayer - Aaqib Azeez.mp3", Wikimedia Commons
  (https://commons.wikimedia.org/wiki/File:The_Adhan_-_Muslim_Call_to_Prayer_-_Aaqib_Azeez.mp3),
  yükleyen Atcovi ("Own work", 23 Aralık 2020), okuyan Aaqib Azeez.
- **Lisans:** CC BY-SA 4.0 (https://creativecommons.org/licenses/by-sa/4.0/). Kısaltılmış hâl de aynı lisansla
  paylaşılır (ShareAlike yalnız ses dosyası içindir, uygulamanın koduna geçmez). Atıf uygulamada, ses seçeneğinin
  altında yazar (`notify.sound.adhanCredit`).
- **Neden bu kayıt (30 Ekim 2026):** açık lisanslı ezan kayıtları içinde en temiz olanı (stüdyo kaydı gibi, arka plan
  gürültüsü çok düşük; cümleler ezanın 12 bölümüne birebir oturuyor). CC0 olan "Adhan in Istanbul" bir cami
  hoparlöründen sokak kaydıdır (yankı ve şehir gürültüsü), Konya kaydı (CC BY 3.0) da öyledir. Cambridge Kahire
  kayıtları CC BY-NC-ND, everyayah/QuranicAudio yalnız kişisel kullanım: uygulamaya konamaz.
- **Kesim:** özgün dosyanın 0,30–5,30. saniyesi (ilk cümle 0,6–4,9 sn), başta 0,05 sn ve sonda 0,45 sn yumuşatma,
  ses düzeyi `loudnorm` (−16 LUFS). Özgün dosyanın SHA-256'sı
  `0faa59e2028bbb2f60a670b53ec89ffcf628f3c195b43fb41d9bdbc99000c1b3`.

```sh
ffmpeg -ss 0.30 -t 5.0 -i adhan.mp3 -ac 1 \
  -af "afade=t=in:st=0:d=0.05,afade=t=out:st=4.55:d=0.45,loudnorm=I=-16:TP=-1.5:LRA=11" -ar 44100 clip.wav
ffmpeg -i clip.wav -c:a pcm_s16le -f caf mobile/ios/App/Resources/ezan.caf          # iOS (bildirim sesi en çok 30 sn)
ffmpeg -i clip.wav -c:a libvorbis -q:a 5 mobile/android/app/src/main/res/raw/ezan.ogg # Android
```

## Medine (`medina`)

- **Kaynak:** "33937 ejaz215 call-to-prayer-from-the-prophet-s-mo.ogg", Wikimedia Commons
  (https://commons.wikimedia.org/wiki/File:33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg), özgün yükleme
  Freesound'da ejaz215 (http://www.freesound.org/people/ejaz215/sounds/33937/), açıklama "Islamic call to prayer from
  the prophet mosque".
- **Lisans:** CC BY 3.0 (https://creativecommons.org/licenses/by/3.0/). Değişiklik belirtilir (kısaltıldı, ses düzeyi
  eşitlendi); atıf ayarda (`notify.adhan.credit.medina`).
- **Kesim:** özgün dosyanın 0,8–18,5. saniyesi (ilk cümle 1,0–16,0 sn, ardından yankı), başta 0,1 sn ve sonda 2,3 sn
  yumuşatma, `loudnorm` (−16 LUFS). iOS dosyası IMA4 (bildirim sesinin 30 sn sınırı içinde, ~0,4 MB). Özgün dosyanın
  SHA-256'sı `4d0fabe0e0ef52186646a256220f74a972ba698ac269165fed5d52de3202d656`.

```sh
ffmpeg -ss 0.8 -t 17.7 -i medina.ogg -ac 1 \
  -af "afade=t=in:st=0:d=0.1,afade=t=out:st=15.4:d=2.3,loudnorm=I=-16:TP=-1.5:LRA=11" -ar 44100 clip.wav
ffmpeg -i clip.wav -c:a adpcm_ima_qt -f caf mobile/ios/App/Resources/ezan-medina.caf
ffmpeg -i clip.wav -c:a libvorbis -q:a 4 mobile/android/app/src/main/res/raw/ezan_medina.ogg
```

## Bakılan ama alınmayanlar

- "Adhan, Great Mosque of Mecca - Jan 21, 2013.webm" (CC BY 3.0, YouTube'dan aktarım): uygun lisanslı, ama 31 Ekim
  2026'da Wikimedia dosya sunucusu geliştirme ortamına 20 denemede de 429 (çok istek) verdi; eklenirse yukarıdaki
  tabloya yazılır.
- "Adhan in Istanbul.webm" (Viceskeeni2, CC0; SHA-1 `15ea3b39550146a4b18a8d7debadb99f3ea7b5e2`): indirildi; 50 sn'lik
  sokak kaydı, ezanın başı yok (ses ilk saniyeden sürüyor) ve şehir gürültüsü var: alınmadı.
- "Hminsec150 Konya.webm" (Frans van der Vaart, CC BY 3.0): indirildi, ilk 16 sn'lik aday kesit sahibe dinletildi;
  31 Ekim 2026'da sahip beğenmedi, alınmadı.
- Sabah Fakhry kaydı ("public domain" beyanı dayanaksız), Cambridge Kahire (CC BY-NC-ND), everyayah/QuranicAudio
  (yalnız kişisel kullanım): uygulamaya konamaz.
