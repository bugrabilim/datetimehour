#!/bin/bash
# App Store ekran görüntüleri: 6,9" iPhone (1320×2868) ve 13" iPad (2064×2752) simülatörlerinde, 24 mağaza klasörü için.
# Çıktı ham görüntülerdir (<çıktı>/<mağaza klasörü>/NN-ad.png); frame-screenshots.mjs bunları çerçeveleyip fastlane/screenshots'a
# yazar, fastlane deliver o klasörü olduğu gibi yükler (görseller dosya adı sırasıyla).
# Kullanım: store-screenshots.sh <SaatTarih.app yolu> <çıktı klasörü>
#
# Vitrin (9 Kasım 2026, docs/magaza-adlari-2026-11.md): uygulama herkese aynı, yalnız görsel sırası pazara göre değişir.
#  m: Müslümanların çoğunlukta olduğu pazarların klasörleri (tr, ar-SA, id, ms, ur-PK). 1.0'daki sıra: saat (altında sıradaki
#     vakit ve bugünün vakitleri), vakitler, takvim, dünya, araçlar, imsakiye, gece modu.
#  n: diğer bütün klasörler, nötr sıra: saat (altında hafta, dünya saatleri, gün doğumu/batımı), gece modu, takvim (klasörün
#     ülkesinin resmî tatilleri), dünya, uyku, araçlar ve en sonda vakitler. Alt çubukta Hava, Takvim, Dünya (vakitler
#     görselinde Hava, Takvim, Vakitler). İmsakiye nötr klasörlerde yok.
#  Hava sekmesi bilerek yok: imzasız simülatör yapısında WeatherKit veri vermez. Widget galerisinin doğrudan açılan ekranı yok.
# Bash 3.2 (macOS'un /bin/bash'i) ile çalışır: ilişkisel dizi yok, boş dizi açılımı yok.
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

shoot() { # dil bölge klasör dosya bekleme [ek uygulama argümanları]; BAR=… alt çubuktaki özellikler (boşsa uygulamanın varsayılanı)
  mkdir -p "$OUT/$3"
  xcrun simctl terminate "$UDID" "$BUNDLE" 2>/dev/null || true
  if [ -n "${BAR:-}" ]; then set -- "$@" -features.bar "$BAR"; fi
  SIMCTL_CHILD_TZ=Europe/Istanbul xcrun simctl launch "$UDID" "$BUNDLE" -AppleLanguages "($1)" -AppleLocale "$2" -notify.asked "<true/>" -dial.seen2 "<true/>" -onboard.seen "<integer>9</integer>" \
    -features "weather,calendar,world,seasons,prayer,tools" "${@:6}"
  sleep "$5"
  xcrun simctl io "$UDID" screenshot "$OUT/$3/$4.png"
}

# Mağaza klasörleri: "uygulama dili:bölge:klasör:vitrin:takvim ülkesi:ek klasörler"
#  vitrin: m (Müslüman pazarı) ya da n (nötr), yukarıda
#  takvim ülkesi: nötr klasörün takvim görselinde resmî tatilleri gösterilen ülke (world-holidays.json'daki 30 ülkeden biri);
#   "-": ülkenin verisi yok, takvim özel günsüz çekilir. m klasörlerinde takvim 1.0'daki gibidir (Türkiye, dini günler açık).
#  ek klasörler: aynı uygulama dilinde bir kez çekilen görüntüler bunlara kopyalanır ("klasör/takvim ülkesi", virgülle; "-" yok).
#   Ek klasörün ülkesi farklıysa yalnız takvim görseli o klasör için yeniden çekilir.
# App Store'da Azerice, Farsça ve Filipince dil sürümü yok. Malayca ve Bengalce arayüz yok: ms İngilizce arayüzle (Müslüman sırası,
# başlıklar Malayca), bn-BD şimdilik en-US'nin kopyası (nötr sıra, başlıklar Bengalce). Bengalce arayüz gelince bn-BD en satırından
# çıkarılıp kendi satırına taşınır: bn:bn_BD:bn-BD:n:-:-
# ur-PK ve bn-BD için fastlane 2.234.0 ya da sonrası gerekir (iş akışı Homebrew'dan en yenisini kurar).
STORE_LANGS="tr:tr_TR:tr:m:-:- ar:ar_SA:ar-SA:m:-:- id:id_ID:id:m:-:- en:en_MY:ms:m:-:- ur:ur_PK:ur-PK:m:-:-
en:en_US:en-US:n:US:en-GB/GB,en-AU/AU,en-CA/CA,bn-BD/- de:de_DE:de-DE:n:DE:- fr:fr_FR:fr-FR:n:FR:fr-CA/CA ru:ru_RU:ru:n:RU:-
ja:ja_JP:ja:n:JP:- ko:ko_KR:ko:n:-:- zh-Hans:zh_CN:zh-Hans:n:CN:zh-Hant/HK th:th_TH:th:n:-:- es:es_ES:es-ES:n:ES:es-MX/-
pt:pt_BR:pt-BR:n:BR:pt-PT/PT hi:hi_IN:hi:n:-:-"
# Saatin altındaki alanlar: Müslüman vitrininde sıradaki vakit ve bugünün vakitleri, nötr vitrinde dünya saatleri ve gün doğumu/batımı
ITEMS="-date,week,next,prayers,-upcoming,-world,-weather,-sun,-globe"
ITEMS_N="-date,week,world,sun,-next,-prayers,-upcoming,-weather,-globe"
# Nötr vitrinde alt çubuk (uygulamanın varsayılanı Vakitler, Hava, Takvim)
NBAR="weather,calendar,world"; NBAR_PRAYER="weather,calendar,prayer"

takvim() { # dil bölge klasör dosya ülke ("-": özel günsüz)
  if [ "$5" = - ]; then
    BAR="$NBAR" shoot "$1" "$2" "$3" "$4" 6 -startTab 2 -cal.hidden "holiday,half,religious,special,school"
  else
    BAR="$NBAR" shoot "$1" "$2" "$3" "$4" 6 -startTab 2 -cal.country "$5" -cal.hidden religious
  fi
}

phone() { # dil bölge klasör vitrin ülke
  if [ "$4" = m ]; then
    shoot "$1" "$2" "$3" 01-saat 6 -clockModel stack -homeItems "$ITEMS"
    shoot "$1" "$2" "$3" 02-vakitler 6 -startTab 5
    shoot "$1" "$2" "$3" 03-takvim 6 -startTab 2
    shoot "$1" "$2" "$3" 04-dunya 15 -openScreen world
    shoot "$1" "$2" "$3" 05-araclar 6 -startTab 6
    shoot "$1" "$2" "$3" 06-imsakiye 6 -openScreen imsakiye
    # Gece modu (frame-screenshots.mjs bunu komodin sahnesine koyar; 2. analiz #33)
    shoot "$1" "$2" "$3" 07-gece 6 -openScreen night -nightMode on
  else
    BAR="$NBAR" shoot "$1" "$2" "$3" 01-saat 6 -clockModel stack -homeItems "$ITEMS_N"
    BAR="$NBAR" shoot "$1" "$2" "$3" 02-gece 6 -openScreen night -nightMode on
    takvim "$1" "$2" "$3" 03-takvim "$5"
    BAR="$NBAR" shoot "$1" "$2" "$3" 04-dunya 15 -openScreen world
    BAR="$NBAR" shoot "$1" "$2" "$3" 05-uyku 6 -openScreen sleep -sleep.on "<true/>"
    BAR="$NBAR" shoot "$1" "$2" "$3" 06-araclar 6 -startTab 6
    BAR="$NBAR_PRAYER" shoot "$1" "$2" "$3" 07-vakitler 6 -startTab 5
  fi
}

# iPad (rakip analizi #18; uygulama iPad'i de destekleyince App Store 13" iPad görüntüsü ister). deliver cihazı
# çözünürlükten tanır; dosya adları iPhone'unkilerle karışmasın diye "ipad-" ile başlar.
ipad() { # dil bölge klasör vitrin ülke
  if [ "$4" = m ]; then
    shoot "$1" "$2" "$3" ipad-01-saat 6 -clockModel stack -homeItems "$ITEMS"
    shoot "$1" "$2" "$3" ipad-02-vakitler 6 -startTab 5
    shoot "$1" "$2" "$3" ipad-03-takvim 6 -startTab 2
    shoot "$1" "$2" "$3" ipad-04-dunya 15 -openScreen world
    shoot "$1" "$2" "$3" ipad-05-araclar 6 -startTab 6
  else
    BAR="$NBAR" shoot "$1" "$2" "$3" ipad-01-saat 6 -clockModel stack -homeItems "$ITEMS_N"
    takvim "$1" "$2" "$3" ipad-02-takvim "$5"
    BAR="$NBAR" shoot "$1" "$2" "$3" ipad-03-dunya 15 -openScreen world
    BAR="$NBAR" shoot "$1" "$2" "$3" ipad-04-araclar 6 -startTab 6
    BAR="$NBAR_PRAYER" shoot "$1" "$2" "$3" ipad-05-vakitler 6 -startTab 5
  fi
}

extras() { # cihaz (phone|ipad) dil bölge klasör ülke ek klasörler: kopyala, ülke farklıysa takvimi yeniden çek
  if [ "$6" = - ]; then return 0; fi
  local pat cal E f c
  if [ "$1" = phone ]; then pat='[0-9]*.png'; cal=03-takvim; else pat='ipad-*.png'; cal=ipad-02-takvim; fi
  for E in ${6//,/ }; do
    f=${E%/*}; c=${E#*/}
    mkdir -p "$OUT/$f"
    cp "$OUT/$4"/$pat "$OUT/$f/"
    if [ "$c" != "$5" ]; then takvim "$2" "$3" "$f" "$cal" "$c"; fi
  done
}

for L in $STORE_LANGS; do
  set -- ${L//:/ }
  phone "$1" "$2" "$3" "$4" "$5"
  extras phone "$1" "$2" "$3" "$5" "$6"
done

IPAD=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
d = json.load(sys.stdin)["devices"]
runtimes = sorted((k for k in d if "iOS" in k), key=lambda k: [int(x) for x in k.split("iOS-")[-1].split("-")], reverse=True)
for k in runtimes:
    hit = [x for x in d[k] if x["name"].startswith("iPad Pro 13-inch")]
    if hit:
        print(hit[0]["udid"]); sys.exit()
sys.exit("13 inç iPad simülatörü yok")')
echo "iPad simülatörü: $IPAD"
xcrun simctl shutdown "$UDID" || true
UDID="$IPAD"
xcrun simctl boot "$UDID" || true
xcrun simctl bootstatus "$UDID" -b
xcrun simctl status_bar "$UDID" override --time "9:41" --batteryState charged --batteryLevel 100 --wifiBars 3 || true
xcrun simctl install "$UDID" "$APP"
xcrun simctl ui "$UDID" appearance light
for L in $STORE_LANGS; do
  set -- ${L//:/ }
  ipad "$1" "$2" "$3" "$4" "$5"
  extras ipad "$1" "$2" "$3" "$5" "$6"
done
ls -R "$OUT"
