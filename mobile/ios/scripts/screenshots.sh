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

shoot() { # dil bölge görünüm dosya [ek uygulama argümanları]
  xcrun simctl ui "$UDID" appearance "$3"
  xcrun simctl terminate "$UDID" "$BUNDLE" 2>/dev/null || true
  SIMCTL_CHILD_TZ=Europe/Istanbul xcrun simctl launch "$UDID" "$BUNDLE" -AppleLanguages "($1)" -AppleLocale "$2" -notify.asked "<true/>" "${@:5}"
  sleep 6
  xcrun simctl io "$UDID" screenshot "$OUT/$4.png"
}
shoot tr tr_TR light tr-acik
shoot tr tr_TR dark tr-koyu
shoot en en_US light en-acik
shoot ar ar_SA light ar-acik
# Saat modelleri (sitedeki 20 model; -clockModel kimlik)
for M in classic stack wx-full h-classic h-stack h-wx-full digital digital-date wx-digital-cal flip; do
  shoot tr tr_TR light "model-$M" -clockModel "$M"
done
shoot tr tr_TR dark model-analog-date-koyu -clockModel analog-date -secA "<true/>"
shoot ar ar_SA light model-wx-digital-cal-ar -clockModel wx-digital-cal
# Saat ekranında bütün alanlar açık (Ayarlar › Saat ekranı)
ALL="date,week,next,prayers,upcoming,world,weather,sun,globe"
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
for S in monthly imsakiye alarm timer stopwatch pomodoro datecalc world seasons settings homeItems privacy about list badges notifications morning moon look; do
  shoot tr tr_TR light "ekran-$S" -openScreen "$S"
done
shoot tr tr_TR dark ekran-about-koyu -openScreen about
shoot tr tr_TR light ekran-renk-cocuk -openScreen settings -accent kid-purple
shoot tr tr_TR light tr-renk-cocuk -accent kid-green
shoot tr tr_TR light ekran-google -openScreen google
shoot en en_US light ekran-world-en -openScreen world
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
