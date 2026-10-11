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

shoot() { # dil bölge görünüm dosya
  xcrun simctl ui "$UDID" appearance "$3"
  xcrun simctl terminate "$UDID" "$BUNDLE" 2>/dev/null || true
  SIMCTL_CHILD_TZ=Europe/Istanbul xcrun simctl launch "$UDID" "$BUNDLE" -AppleLanguages "($1)" -AppleLocale "$2"
  sleep 6
  xcrun simctl io "$UDID" screenshot "$OUT/$4.png"
}
shoot tr tr_TR light tr-acik
shoot tr tr_TR dark tr-koyu
shoot en en_US light en-acik
shoot ar ar_SA light ar-acik
ls -la "$OUT"
