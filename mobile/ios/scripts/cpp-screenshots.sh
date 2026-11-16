#!/bin/bash
# App Store özel ürün sayfalarının ekran görüntüleri (15 Kasım 2026; fastlane/custom-pages.json, docs/urun-sayfalari-2026-11.md).
# 6,9" iPhone ve 13" iPad simülatörlerinde, her sayfa ve dil sürümü için custom-pages.json'daki çekim sırasıyla.
# Çıktı: <çıktı>/<sayfa>/<dil sürümü>/NN-ad.png ve ipad-NN-ad.png (ham). Her sayfa klasörü frame-screenshots.mjs ile ayrı
# çerçevelenir (başlıklar captions.json'da, anahtar "ad"), asc_cpp.py çerçevelenmiş klasörü yükler.
# Kullanım: cpp-screenshots.sh <SaatTarih.app yolu> <çıktı klasörü>
# Bash 3.2 (macOS'un /bin/bash'i) ile çalışır.
set -euo pipefail
APP="$1"; OUT="$2"; BUNDLE=tr.bumba.time
HERE="$(cd "$(dirname "$0")" && pwd)"
PAGES="$HERE/../fastlane/custom-pages.json"

pick() { # ad önekleri (ilk bulunan)
  xcrun simctl list devices available -j | python3 -c '
import json, sys
d = json.load(sys.stdin)["devices"]
wants = sys.argv[1:]
runtimes = sorted((k for k in d if "iOS" in k), key=lambda k: [int(x) for x in k.split("iOS-")[-1].split("-")], reverse=True)
for want in wants:
    for k in runtimes:
        hit = [x for x in d[k] if x["name"].startswith(want)]
        if hit:
            print(hit[0]["udid"]); sys.exit()
sys.exit("simülatör yok: " + ", ".join(wants))' "$@"
}

prepare() { # udid
  xcrun simctl boot "$1" || true
  xcrun simctl bootstatus "$1" -b
  xcrun simctl status_bar "$1" override --time "9:41" --batteryState charged --batteryLevel 100 --cellularBars 4 --wifiBars 3 || true
  xcrun simctl install "$1" "$APP"
  xcrun simctl ui "$1" appearance light
}

# Saatin altındaki alanlar ve alt çubuk: store-screenshots.sh ile aynı (m: Müslüman vitrini, n: nötr)
ITEMS="-date,week,next,prayers,-upcoming,-world,-weather,-sun,-globe"
ITEMS_N="-date,week,world,sun,-next,-prayers,-upcoming,-weather,-globe"

shoot() { # dil bölge klasör dosya bekleme saat-dilimi alt-çubuk [ek argümanlar]
  mkdir -p "$OUT/$3"
  xcrun simctl terminate "$UDID" "$BUNDLE" 2>/dev/null || true
  local extra=()
  if [ -n "$7" ] && [ "$7" != - ]; then extra=(-features.bar "$7"); fi
  SIMCTL_CHILD_TZ="$6" xcrun simctl launch "$UDID" "$BUNDLE" -AppleLanguages "($1)" -AppleLocale "$2" -notify.asked "<true/>" \
    -dial.seen2 "<true/>" -onboard.seen "<integer>9</integer>" -features "weather,calendar,world,seasons,prayer,tools" \
    ${extra[@]+"${extra[@]}"} "${@:8}"
  sleep "$5"
  xcrun simctl io "$UDID" screenshot "$OUT/$3/$4.png"
}

one() { # sayfa dil-sürümü saat-dilimi önek sıra ad
  local page=$1 loc=$2 tz=$3 pre=$4 n=$5 key=$6 lang region v bar
  case "$loc" in
    tr) lang=tr; region=tr_TR; v=m ;;
    ar-SA) lang=ar; region=ar_SA; v=m ;;
    de-DE) lang=de; region=de_DE; v=n ;;
    hi) lang=hi; region=hi_IN; v=n ;;
    *) lang=en; region=en_US; v=n ;;
  esac
  [ "$tz" = Asia/Kolkata ] && region="${lang}_IN"
  bar=-; [ "$v" = n ] && bar="weather,calendar,world"
  local f="$page/$loc" name="${pre}$(printf %02d "$n")-$key"
  case "$key" in
    gece) shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -openScreen night -nightMode on ;;
    saat) if [ "$v" = m ]; then shoot $lang $region "$f" "$name" 6 "$tz" - -clockModel stack -homeItems "$ITEMS"
          else shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -clockModel stack -homeItems "$ITEMS_N"; fi ;;
    kadranlar) shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -openScreen dials ;;
    uyku) shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -openScreen sleep -sleep.on "<true/>" ;;
    widgetlar) shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -openScreen widgets ;;
    dunya) shoot $lang $region "$f" "$name" 15 "$tz" "$bar" -openScreen world ;;
    vakitler) shoot $lang $region "$f" "$name" 6 "$tz" "weather,calendar,prayer" -startTab 5 ;;
    imsakiye) shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -openScreen imsakiye ;;
    takvim) shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -startTab 2 ;;
    planlayici) shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -openScreen planner ;;
    panchang) shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -openScreen panchang ;;
    takvimler)
      if [ "$page" = panchang ]; then shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -startTab 2 -cal.extra hindu -cal.hidden religious
      else shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -startTab 2 -cal.extra "chinese,jewish,hindu,christian" -cal.hidden religious; fi ;;
    kopru) shoot $lang $region "$f" "$name" 6 "$tz" "$bar" -openScreen bridge ;;
    *) echo "bilinmeyen çekim: $key" >&2; exit 1 ;;
  esac
}

# Satırlar: sayfa dil-sürümü saat-dilimi ad1,ad2,… (köprü tatil yalnız Türkçede: Türkiye'nin tatilleri)
PLAN=$(python3 -c '
import json, sys
for p in json.load(open(sys.argv[1]))["pages"]:
    for loc in p["locales"]:
        shots = [s for s in p["shots"] if s != "kopru" or loc == "tr"]
        print(p["id"], loc, p.get("tz", "Europe/Istanbul"), ",".join(shots))' "$PAGES")

run() { # önek (iPhone: "", iPad: "ipad-")
  echo "$PLAN" | while read -r page loc tz shots; do
    i=1
    for key in ${shots//,/ }; do one "$page" "$loc" "$tz" "$1" $i "$key"; i=$((i + 1)); done
  done
}

UDID=$(pick "iPhone 17 Pro Max" "iPhone 16 Pro Max")
echo "Simülatör: $UDID"
prepare "$UDID"
run ""
xcrun simctl shutdown "$UDID" || true
UDID=$(pick "iPad Pro 13-inch")
echo "iPad simülatörü: $UDID"
prepare "$UDID"
run "ipad-"
ls -R "$OUT"
