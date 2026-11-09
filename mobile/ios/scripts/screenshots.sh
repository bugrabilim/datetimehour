#!/bin/bash
# Simülatörde uygulamayı açıp ekran görüntüsü alır (CI'da, macOS).
# Kullanım: screenshots.sh <SaatTarih.app yolu> <çıktı klasörü>
set -euo pipefail
APP="$1"; OUT="$2"; BUNDLE=tr.bumba.time
mkdir -p "$OUT"
# En yeni iOS sürümündeki ilk iPhone simülatörü
UDID=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
d = json.load(sys.stdin)["devices"]
runtimes = sorted((k for k in d if "iOS" in k), key=lambda k: [int(x) for x in k.split("iOS-")[-1].split("-")], reverse=True)
for k in runtimes:
    phones = [x for x in d[k] if x["name"].startswith("iPhone")]
    if phones:
        print(phones[0]["udid"]); break')
echo "Simülatör: $UDID"
xcrun simctl boot "$UDID" || true
xcrun simctl bootstatus "$UDID" -b
xcrun simctl status_bar "$UDID" override --time "09:41" --batteryState charged --batteryLevel 100 --cellularBars 4 --wifiBars 3 || true
xcrun simctl install "$UDID" "$APP"

# Görüntüler bütün özellikler açıkken alınır (yeni kurulumda yalnız Saat açık); FEAT= ile ilk açılış görünümü
ALLFEAT="weather,calendar,world,seasons,prayer,tools"
shoot() { # dil bölge görünüm dosya [ek uygulama argümanları]
  xcrun simctl ui "$UDID" appearance "$3"
  xcrun simctl terminate "$UDID" "$BUNDLE" 2>/dev/null || true
  SIMCTL_CHILD_TZ=Europe/Istanbul xcrun simctl launch "$UDID" "$BUNDLE" -AppleLanguages "($1)" -AppleLocale "$2" -notify.asked "<true/>" -dial.seen2 "<true/>" -onboard.seen "<integer>9</integer>" \
    -features "${FEAT-$ALLFEAT}" "${@:5}"
  sleep 6
  xcrun simctl io "$UDID" screenshot "$OUT/$4.png"
}
shoot tr tr_TR light tr-acik
shoot tr tr_TR dark tr-koyu
shoot en en_US light en-acik
shoot ar ar_SA light ar-acik
# İlk açılış: yalnız Saat (alt bar yok; "özellikleri aç" satırı); iki özellik açıkken bar
FEAT="" shoot tr tr_TR light tr-ilk-acilis
FEAT="prayer,weather" shoot tr tr_TR light tr-iki-ozellik -startTab 5
# İlk kurulum sihirbazı (2. analiz #4; 5 Kasım 2026'dan beri sorulu): yalnız yeni kurulumda ve tanıtımın yeni sürümünü
# görmemiş eski kullanıcıda açılır, burada açıkça istenir. Sayfalar: 0 hoş geldiniz, 1 güven + konum ve bildirim, 2–6 sorular,
# 7 özet, 8 "başka neler var", 9–10 ipuçları, 11–16 özellik turu, 17 "Diğer her şey Tümü'nde"
FEAT="" shoot tr tr_TR light tr-kurulum -onboard.pending "<true/>" -onboard.fresh "<true/>" -onboardPage "<integer>0</integer>"
FEAT="" shoot tr tr_TR light tr-kurulum-guven -onboard.pending "<true/>" -onboard.fresh "<true/>" -onboardPage "<integer>1</integer>"
FEAT="" shoot tr tr_TR light tr-kurulum-soru -onboard.pending "<true/>" -onboard.fresh "<true/>" -onboardPage "<integer>2</integer>"
# Vakitler'i sormayan dillerde (en) 4 soru ve 5 tur, namaz geçmez: 2 hava sorusu, 14 widget turu (9 Kasım 2026)
FEAT="" shoot en en_US light en-kurulum -onboard.pending "<true/>" -onboard.fresh "<true/>" -onboardPage "<integer>0</integer>"
FEAT="" shoot en en_US light en-kurulum-soru-hava -onboard.pending "<true/>" -onboard.fresh "<true/>" -onboardPage "<integer>2</integer>"
FEAT="" shoot en en_US light en-kurulum-tur-widget -onboard.pending "<true/>" -onboard.fresh "<true/>" -onboardPage "<integer>14</integer>"
FEAT="" shoot ar ar_SA light ar-kurulum-soru -onboard.pending "<true/>" -onboard.fresh "<true/>" -onboardPage "<integer>6</integer>"
FEAT="prayer,weather,calendar,tools" shoot tr tr_TR light tr-kurulum-ozet -onboard.pending "<true/>" -onboardPage "<integer>7</integer>"
FEAT="" shoot tr tr_TR light tr-kurulum-daha -onboard.pending "<true/>" -onboardPage "<integer>8</integer>"
FEAT="" shoot tr tr_TR light tr-kurulum-kaydir -onboard.pending "<true/>" -onboardPage "<integer>9</integer>"
FEAT="" shoot tr tr_TR dark tr-kurulum-tam-ekran -onboard.pending "<true/>" -onboardPage "<integer>10</integer>"
FEAT="" shoot tr tr_TR light tr-kurulum-tur-alarm -onboard.pending "<true/>" -onboardPage "<integer>11</integer>"
FEAT="" shoot tr tr_TR light tr-kurulum-tur-vakit -onboard.pending "<true/>" -onboardPage "<integer>12</integer>"
FEAT="" shoot ar ar_SA light ar-kurulum-tur-widget -onboard.pending "<true/>" -onboardPage "<integer>16</integer>"
FEAT="" shoot tr tr_TR light tr-kurulum-tumu -onboard.pending "<true/>" -onboardPage "<integer>17</integer>"
FEAT="" shoot ar ar_SA light ar-kurulum-tumu -onboard.pending "<true/>" -onboardPage "<integer>17</integer>"
# Kurulumu geçen yeni kullanıcı: Saat, ortada "Kuruluma devam et", sağda Tümü
FEAT="" shoot tr tr_TR light tr-kuruluma-devam -onboard.resume "<true/>"
# İzinler sayfası (5 Kasım 2026): durum, ne için, telefonun ayarlarına bağlantı
shoot tr tr_TR light tr-izinler -openScreen permissions
shoot hi hi_IN dark hi-izinler -openScreen permissions
# Saat modelleri (sitedeki 20 model; -clockModel kimlik)
for M in classic stack wx-full h-classic h-stack h-wx-full digital digital-date wx-digital-cal flip; do
  shoot tr tr_TR light "model-$M" -clockModel "$M"
done
shoot tr tr_TR dark model-analog-date-koyu -clockModel analog-date -secA "<true/>"
shoot ar ar_SA light model-wx-digital-cal-ar -clockModel wx-digital-cal
# Saat ekranında bütün alanlar açık (Ayarlar › Saat ekranı)
ALL="date,week,hijri,next,prayers,upcoming,world,weather,sun,globe"
shoot tr tr_TR light tr-alanlar -homeItems "$ALL" -clockModel digital
shoot ar ar_SA light ar-alanlar -homeItems "$ALL" -clockModel digital
# Sekmeler: 0 saat, 1 hava durumu, 2 takvim, 3 dünya saatleri, 4 mevsimler, 5 vakitler, 6 araçlar
shoot tr tr_TR light tr-hava -startTab 1
shoot tr tr_TR light tr-dunya -startTab 3
shoot tr tr_TR light tr-mevsimler -startTab 4
shoot tr tr_TR dark tr-mevsimler-koyu -startTab 4
shoot en en_US light en-mevsimler -startTab 4
shoot tr tr_TR light tr-takvim -startTab 2
shoot tr tr_TR dark tr-takvim-koyu -startTab 2
shoot en en_US light en-takvim -startTab 2
shoot tr tr_TR light tr-vakitler -startTab 5
shoot tr tr_TR light tr-araclar -startTab 6
# Bildirim ayarı penceresi (açık hâli; izin sorulmaz, yalnız görünüm)
shoot tr tr_TR light tr-bildirim -startTab 5 -openNotifications -notify.enabled "<true/>" -notify.lead "<integer>10</integer>"
# Alt ekranlar (doğrudan açılır)
for S in monthly imsakiye alarm timer stopwatch pomodoro datecalc world seasons settings homeItems privacy about list badges notifications morning moon look features qibla study wakegame language dials chess metronome talking zodiac timecalc bridge retire exam classroom photo; do
  shoot tr tr_TR light "ekran-$S" -openScreen "$S"
done
# Fotoğraf ışığı (9 Kasım 2026): Arapça, koyu (sağdan sola)
shoot ar ar_SA dark ekran-photo-ar -openScreen photo
shoot tr tr_TR dark ekran-about-koyu -openScreen about
shoot tr tr_TR light ekran-renk-cocuk -openScreen settings -accent kid-purple
shoot tr tr_TR light tr-renk-cocuk -accent kid-green
shoot tr tr_TR light ekran-google -openScreen google
# Hareketli arka plan: saat görünümü sayfası (canlı örnek) gökyüzü ve renk akışıyla
shoot tr tr_TR dark ekran-look-yildizlar -openScreen look -fsBg stars
shoot tr tr_TR light ekran-look-renk-akisi -openScreen look -fsBg flow
shoot en en_US light ekran-world-en -openScreen world
shoot ar ar_SA dark ekran-qibla-ar -openScreen qibla
# İbadet: Kur'an (liste, Yâsîn; İngilizce meal açık, Arapça koyu), dualar, zikirmatik, takip, zekât
for S in sura duas track zakat esma hadith guide leaf method journal planner reldays greet qr; do
  shoot tr tr_TR light "ekran-$S" -openScreen "$S"
done
# Sahibin ekran görüntülerinden: atmosfer (renk seçici ve saat ekranı), yazı boyutu %130, ayarlar (destek bölümü)
shoot tr tr_TR light ekran-atmosfer -openScreen colors -atmo "<string>lavender</string>"
shoot tr tr_TR dark tr-saat-atmosfer-koyu -startTab 0 -atmo "<string>ocean</string>"
shoot tr tr_TR light ekran-yazi-boyutu -openScreen settings -fontScale "<integer>130</integer>"
shoot tr tr_TR light ekran-quran -openScreen quran -quran.bookmark "<string>18:10</string>"
shoot tr tr_TR light ekran-zikir -openScreen zikir -zikir.tesbihat "<true/>" -zikir.count "<integer>41</integer>" -zikir.total "<integer>1250</integer>"
# 1. dalga D: tesbih görünümü, Kur'an zemini (sepya) ve hatim, vakitlerde 24 saat kadranı ve sözle kalan süre
shoot tr tr_TR light ekran-zikir-tesbih -openScreen zikir -zikir.beads "<true/>" -zikir.count "<integer>21</integer>"
shoot tr tr_TR light ekran-sura-sepya -openScreen sura -quran.theme "<string>sepia</string>" -quran.goal "<integer>20</integer>"
shoot tr tr_TR light tr-vakitler-kadran -startTab 5 -prayer.visual "<string>dial</string>" -prayer.words "<true/>"
shoot en en_US light ekran-sura-en -openScreen sura
shoot ar ar_SA dark ekran-sura-ar -openScreen sura
shoot en en_US dark ekran-hadith-en -openScreen hadith
shoot ar ar_SA light ekran-esma-ar -openScreen esma
shoot tr tr_TR light ekran-zakat-ornek -openScreen zakat \
  -zakat.values '<string>{"cash":400000,"debt":30000,"goldGrams":20,"goldPrice":4000}</string>'
# Uyku: plan açık (kalkış 07.00, 8 saat; yatma 23.00)
shoot tr tr_TR light ekran-uyku -openScreen sleep -sleep.on "<true/>"
# Alarm kapatma görevi (matematik)
shoot tr tr_TR dark ekran-alarm-gorevi -openScreen wake
shoot tr tr_TR light ekran-alarm-duvar -openScreen wall
# Gece modu: siyah zemin, kısık kırmızı saat (yıldızlı arka plan seçili olsa da düz siyah)
shoot tr tr_TR dark ekran-gece-modu -openScreen night -nightMode on -fsBg stars
# Yatay (yan çevrilmiş): saat sekmesi tam ekran açılır, diğerlerinde alt bar gizli. Sona konur ki sonraki
# açılışlar dik kalsın; görüntü cihazın dik çerçevesinde yan döner.
shoot tr tr_TR light tr-yatay-saat -landscape
shoot tr tr_TR light tr-yatay-hava -startTab 1 -landscape
shoot tr tr_TR dark tr-yatay-vakitler -startTab 5 -landscape
# Uygulamanın Info.plist anahtarları (izin metinleri) derlenmiş pakette var mı
plutil -p "$APP/Info.plist" | grep -E "NSAlarmKit|NSLocation|CFBundleDisplayName" || true
# Bildirim sesi örnekleri (telefonda üretilen dosyalar): Library/Sounds → çıktı klasörü
shoot tr tr_TR light tr-ses -makeSoundSample
sleep 10
DATA=$(xcrun simctl get_app_container "$UDID" "$BUNDLE" data)
mkdir -p "$OUT/sesler"
cp "$DATA"/Library/Sounds/*.caf "$OUT/sesler/" 2>/dev/null || echo "Ses dosyası üretilmedi"
ls -la "$OUT" "$OUT/sesler"

# iPad (rakip analizi #18): en yeni iOS'taki 13 inç iPad simülatörü (yoksa ilk iPad); sayfalar okunur genişlikte ortalı
IPAD=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
d = json.load(sys.stdin)["devices"]
runtimes = sorted((k for k in d if "iOS" in k), key=lambda k: [int(x) for x in k.split("iOS-")[-1].split("-")], reverse=True)
for k in runtimes:
    pads = [x for x in d[k] if x["name"].startswith("iPad")]
    if pads:
        big = [x for x in pads if "13-inch" in x["name"]]
        print((big or pads)[0]["udid"]); break')
if [ -n "$IPAD" ]; then
  echo "iPad simülatörü: $IPAD"
  xcrun simctl shutdown "$UDID" || true
  UDID="$IPAD"
  xcrun simctl boot "$UDID" || true
  xcrun simctl bootstatus "$UDID" -b
  xcrun simctl status_bar "$UDID" override --time "09:41" --batteryState charged --batteryLevel 100 --wifiBars 3 || true
  xcrun simctl install "$UDID" "$APP"
  shoot tr tr_TR light ipad-saat
  shoot tr tr_TR dark ipad-vakitler -startTab 5
  shoot tr tr_TR light ipad-takvim -startTab 2
  shoot en en_US light ipad-hava -startTab 1
  shoot ar ar_SA light ipad-dunya -startTab 3
  shoot tr tr_TR light ipad-uyku -openScreen sleep -sleep.on "<true/>"
  ls -la "$OUT"/ipad-*
fi
