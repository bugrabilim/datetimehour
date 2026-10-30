#!/bin/bash
# App Store ekran görüntüleri: 6,9" iPhone simülatöründe (1320×2868), dört mağaza dilinde.
# fastlane deliver bu klasörü olduğu gibi yükler: <çıktı>/<mağaza dili>/NN-ad.png
# Kullanım: store-screenshots.sh <SaatTarih.app yolu> <çıktı klasörü>
set -euo pipefail
APP="$1"; OUT="$2"; BUNDLE=tr.bumba.time
UDID=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
d = json.load(sys.stdin)["devices"]
runtimes = sorted((k for k in d if "iOS" in k), key=lambda k: [int(x) for x in k.split("iOS-")[-1].split("-")], reverse=True)
for want in ("iPhone 17 Pro Max", "iPhone 16 Pro Max"):
    for k in runtimes:
        hit = [x for x in d[k] if x["name"] == want]
        if hit:
            print(hit[0]["udid"]); sys.exit()
sys.exit("6,9 inç iPhone simülatörü yok")')
echo "Simülatör: $UDID"
xcrun simctl boot "$UDID" || true
xcrun simctl bootstatus "$UDID" -b
xcrun simctl status_bar "$UDID" override --time "9:41" --batteryState charged --batteryLevel 100 --cellularBars 4 --wifiBars 3 || true
xcrun simctl install "$UDID" "$APP"
xcrun simctl ui "$UDID" appearance light

shoot() { # dil bölge klasör dosya bekleme [ek uygulama argümanları]
  mkdir -p "$OUT/$3"
  xcrun simctl terminate "$UDID" "$BUNDLE" 2>/dev/null || true
  SIMCTL_CHILD_TZ=Europe/Istanbul xcrun simctl launch "$UDID" "$BUNDLE" -AppleLanguages "($1)" -AppleLocale "$2" -notify.asked "<true/>" \
    -features "weather,calendar,world,seasons,prayer,tools" "${@:6}"
  sleep "$5"
  xcrun simctl io "$UDID" screenshot "$OUT/$3/$4.png"
}
ITEMS="-date,week,next,prayers,-upcoming,-world,-weather,-sun,-globe"
for L in "tr tr_TR tr" "en en_US en-US" "de de_DE de-DE" "ar ar_SA ar-SA"; do
  set -- $L
  shoot $1 $2 $3 01-saat 6 -clockModel stack -homeItems "$ITEMS"
  shoot $1 $2 $3 02-vakitler 6 -startTab 5
  shoot $1 $2 $3 03-takvim 6 -startTab 2
  shoot $1 $2 $3 04-dunya 15 -openScreen world
  shoot $1 $2 $3 05-araclar 6 -startTab 6
  shoot $1 $2 $3 06-imsakiye 6 -openScreen imsakiye
done
ls -R "$OUT"
