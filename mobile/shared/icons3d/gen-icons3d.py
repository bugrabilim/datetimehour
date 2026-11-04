#!/usr/bin/env python3
"""3D simgeleri üretir (4 Kasım 2026): icons3d.json'daki her ad için Microsoft Fluent Emoji (MIT) deposunun sabit
sürümünden 3D PNG'yi indirir, kenar boşluğunu koruyarak `size` px kareye küçültür ve yazar:
  Android  mobile/android/app/src/main/res/drawable-nodpi/e3d_<ad>.png
  iOS      mobile/ios/Icons3D.xcassets/e3d-<ad>.imageset/ (tek ölçek; uygulama ve widget hedefinde)
Kullanım: python3 -I mobile/shared/icons3d/gen-icons3d.py   (ağ ve Pillow gerekir; çıktı depoya girer)
          python3 -I mobile/shared/icons3d/gen-icons3d.py --check   (yalnız dosyaların varlığını denetler)
"""
import io, json, os, sys, urllib.error, urllib.parse, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
CFG = json.load(open(os.path.join(HERE, "icons3d.json"), encoding="utf-8"))
ANDROID = os.path.join(ROOT, "mobile/android/app/src/main/res/drawable-nodpi")
IOS = os.path.join(ROOT, "mobile/ios/Icons3D.xcassets")


def slug(folder):
    return folder.lower().replace(" ", "_")


def url(folder, skin):
    base = f"https://raw.githubusercontent.com/microsoft/fluentui-emoji/{CFG['commit']}/assets/{urllib.parse.quote(folder)}"
    s = slug(folder)
    return f"{base}/Default/3D/{s}_3d_default.png" if skin else f"{base}/3D/{s}_3d.png"


def fetch(folder):
    import time
    for attempt in range(4):
        for skin in (False, True):
            try:
                with urllib.request.urlopen(url(folder, skin), timeout=30) as r:
                    return r.read()
            except urllib.error.HTTPError as e:
                if e.code == 404:
                    continue
            except Exception:
                pass
        time.sleep(2 * (attempt + 1))
    raise SystemExit(f"indirilemedi: {folder}")


def main():
    names = CFG["icons"]
    if "--check" in sys.argv:
        missing = [n for n in names if not os.path.exists(os.path.join(ANDROID, f"e3d_{n}.png"))
                   or not os.path.exists(os.path.join(IOS, f"e3d-{n}.imageset", f"e3d-{n}.png"))]
        if missing:
            raise SystemExit("eksik 3D simge: " + ", ".join(missing))
        print(f"3D simgeler tamam ({len(names)})")
        return
    from PIL import Image  # yalnız üretirken gerekir (--check Pillow'suz çalışır)
    os.makedirs(ANDROID, exist_ok=True)
    os.makedirs(IOS, exist_ok=True)
    with open(os.path.join(IOS, "Contents.json"), "w") as f:
        json.dump({"info": {"author": "xcode", "version": 1}}, f, indent=2)
    size = CFG["size"]
    cache = {}
    total = 0
    for name, folder in names.items():
        if folder not in cache:
            cache[folder] = fetch(folder)
        im = Image.open(io.BytesIO(cache[folder])).convert("RGBA").resize((size, size), Image.LANCZOS)
        buf = io.BytesIO()
        im.save(buf, "PNG", optimize=True)
        data = buf.getvalue()
        total += len(data)
        open(os.path.join(ANDROID, f"e3d_{name}.png"), "wb").write(data)
        d = os.path.join(IOS, f"e3d-{name}.imageset")
        os.makedirs(d, exist_ok=True)
        open(os.path.join(d, f"e3d-{name}.png"), "wb").write(data)
        with open(os.path.join(d, "Contents.json"), "w") as f:
            json.dump({"images": [{"filename": f"e3d-{name}.png", "idiom": "universal"}],
                       "info": {"author": "xcode", "version": 1}}, f, indent=2)
    print(f"{len(names)} simge, toplam {total // 1024} KB (her platformda)")


if __name__ == "__main__":
    main()
