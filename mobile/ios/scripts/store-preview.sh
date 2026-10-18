#!/bin/bash
# App Store önizleme videosu: 6,9" iPhone simülatöründe tam ekran dönen dünya, dört mağaza dilinde.
# Apple'ın biçimi: 886×1920 dikey, 30 kare/sn, H.264, 15–30 sn, stereo AAC ses izi (sessiz).
# Kullanım: store-preview.sh <SaatTarih.app yolu> <çıktı klasörü>   (ffmpeg gerekir)
set -euo pipefail
APP="$1"; OUT="$2"; BUNDLE=tr.bumba.time
mkdir -p "$OUT"
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
xcrun simctl boot "$UDID" || true
xcrun simctl bootstatus "$UDID" -b
xcrun simctl status_bar "$UDID" override --time "9:41" --batteryState charged --batteryLevel 100 --cellularBars 4 --wifiBars 3 || true
xcrun simctl install "$UDID" "$APP"
xcrun simctl ui "$UDID" appearance dark

for L in "tr tr_TR tr" "en en_US en-US" "de de_DE de-DE" "ar ar_SA ar-SA"; do
  set -- $L
  xcrun simctl terminate "$UDID" "$BUNDLE" 2>/dev/null || true
  SIMCTL_CHILD_TZ=Europe/Istanbul xcrun simctl launch "$UDID" "$BUNDLE" -AppleLanguages "($1)" -AppleLocale "$2" -openScreen globe
  sleep 12 # harita görüntüleri yüklensin
  RAW="$OUT/ham-$3.mov"
  xcrun simctl io "$UDID" recordVideo --codec=h264 --force "$RAW" &
  REC=$!
  sleep 27
  kill -INT $REC
  wait $REC || true
  # 1320×2868 → 886 genişlik (1925 yükseklik), ortadan 1920'ye kırp; ilk 1 sn (kayıt başlangıcı) atılır, 25 sn
  ffmpeg -loglevel error -y -ss 1 -i "$RAW" -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 \
    -vf "scale=886:-2,crop=886:1920,fps=30" -t 25 -c:v libx264 -profile:v high -pix_fmt yuv420p -b:v 8M \
    -c:a aac -b:a 256k -shortest -movflags +faststart "$OUT/onizleme-$3.mp4"
  rm -f "$RAW"
done
ls -la "$OUT"
