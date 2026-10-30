# Ezan sesi (vakit bildirimi, 5 sn)

Vakit bildirimlerinde "Ezan" ses seçeneği, vaktinde bir ezanın ilk cümlesini ("Allāhu akbar, Allāhu akbar") çalar.

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
