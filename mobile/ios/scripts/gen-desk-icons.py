#!/usr/bin/env python3
"""Apple TV ve Vision Pro simgelerini üretir (katmanlı; iPhone simgesindeki saatle aynı çizim).

Apple TV: TV/Assets.xcassets/App Icon & Top Shelf Image.brandassets (arka + halka + akrep/yelkovan katmanları;
400x240 @1x/@2x, App Store 1280x768; üst raf görselleri 1920x720 ve 2320x720, @1x/@2x).
Vision Pro: Vision/Assets.xcassets/AppIcon.solidimagestack (1024x1024 üç katman; arka opak).

Çalıştırma (depo kökünden): python3 mobile/ios/scripts/gen-desk-icons.py   (Pillow gerekir)
"""
import json
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
BG = (0x1B, 0x1F, 0x27)
BG2 = (0x26, 0x2B, 0x36)
ORANGE = (0xF2, 0xA6, 0x5A)
WHITE = (0xEE, 0xF0, 0xF4)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
INFO = {"author": "xcode", "version": 1}
SS = 4  # kenar yumuşatma için büyük çizip küçültme


def background(w, h):
    """Hafif dikey geçişli koyu zemin (opak)"""
    img = Image.new("RGB", (w, h), BG)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(round(BG2[i] * (1 - t) + BG[i] * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


def layer(w, h, cx, cy, r, part):
    """Saat çizimi (iPhone simgesindeki oranlarla): part = "ring" ya da "hands"; şeffaf zemin"""
    W, H = w * SS, h * SS
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy, r = cx * SS, cy * SS, r * SS
    k = r / 349.0  # iPhone simgesinde dış yarıçap 349 px (1024'lük tuval)
    stroke = 58 * k
    if part == "ring":
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=ORANGE)
        ri = r - stroke
        d.ellipse([cx - ri, cy - ri, cx + ri, cy + ri], fill=(0, 0, 0, 0))
    else:
        def hand(x2, y2):
            d.line([(cx, cy), (x2, y2)], fill=WHITE, width=round(stroke))
            for (x, y) in [(cx, cy), (x2, y2)]:
                d.ellipse([x - stroke / 2, y - stroke / 2, x + stroke / 2, y + stroke / 2], fill=WHITE)
        hand(cx, cy - 200 * k)              # yelkovan: yukarı
        hand(cx + 128 * k, cy + 86 * k)     # akrep: sağ alta (saat 4)
        dot = 37 * k
        d.ellipse([cx - dot, cy - dot, cx + dot, cy + dot], fill=ORANGE)
    return img.resize((w, h), Image.LANCZOS)


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def imagestack(base, sizes, idiom="tv", kind="imagestack"):
    """Katmanlı simge: sizes = [(ölçek, w, h)]; katmanlar önden arkaya"""
    layers = ["Front", "Middle", "Back"]
    write_json(f"{base}/Contents.json", {"info": INFO, "layers": [{"filename": f"{n}.{kind}layer"} for n in layers]})
    for name in layers:
        lay = f"{base}/{name}.{kind}layer"
        write_json(f"{lay}/Contents.json", {"info": INFO})
        images = []
        for scale, w, h in sizes:
            fn = f"{name.lower()}-{w}x{h}.png"
            r = h * 0.36
            if name == "Back":
                img = background(w, h)
            else:
                img = layer(w, h, w / 2, h / 2, r, "hands" if name == "Front" else "ring")
            save(img, f"{lay}/Content.imageset/{fn}")
            images.append({"filename": fn, "idiom": idiom, "scale": scale})
        write_json(f"{lay}/Content.imageset/Contents.json", {"images": images, "info": INFO})


def top_shelf(path, w1, h1):
    """Üst raf görseli: solda saat, sağda "Saat Tarih" (@1x ve @2x)"""
    images = []
    for scale, m in [("1x", 1), ("2x", 2)]:
        w, h = w1 * m, h1 * m
        img = background(w, h).convert("RGBA")
        r = h * 0.30
        cx = w * 0.30
        for part in ("ring", "hands"):
            img.alpha_composite(layer(w, h, cx, h / 2, r, part))
        d = ImageDraw.Draw(img)
        f = ImageFont.truetype(FONT, round(h * 0.16))
        x = cx + r + h * 0.18
        d.text((x, h / 2 - h * 0.19), "Saat", font=f, fill=WHITE)
        d.text((x, h / 2 + h * 0.01), "Tarih", font=f, fill=ORANGE)
        fn = f"top-shelf-{w}x{h}.png"
        save(img.convert("RGB"), f"{path}/{fn}")
        images.append({"filename": fn, "idiom": "tv", "scale": scale})
    write_json(f"{path}/Contents.json", {"images": images, "info": INFO})


def tv():
    cat = f"{ROOT}/TV/Assets.xcassets"
    write_json(f"{cat}/Contents.json", {"info": INFO})
    ba = f"{cat}/App Icon & Top Shelf Image.brandassets"
    write_json(f"{ba}/Contents.json", {"assets": [
        {"filename": "App Icon - App Store.imagestack", "idiom": "tv", "role": "primary-app-icon", "size": "1280x768"},
        {"filename": "App Icon.imagestack", "idiom": "tv", "role": "primary-app-icon", "size": "400x240"},
        {"filename": "Top Shelf Image Wide.imageset", "idiom": "tv", "role": "top-shelf-image-wide", "size": "2320x720"},
        {"filename": "Top Shelf Image.imageset", "idiom": "tv", "role": "top-shelf-image", "size": "1920x720"},
    ], "info": INFO})
    imagestack(f"{ba}/App Icon - App Store.imagestack", [("1x", 1280, 768)])
    imagestack(f"{ba}/App Icon.imagestack", [("1x", 400, 240), ("2x", 800, 480)])
    top_shelf(f"{ba}/Top Shelf Image.imageset", 1920, 720)
    top_shelf(f"{ba}/Top Shelf Image Wide.imageset", 2320, 720)


def vision():
    cat = f"{ROOT}/Vision/Assets.xcassets"
    write_json(f"{cat}/Contents.json", {"info": INFO})
    base = f"{cat}/AppIcon.solidimagestack"
    layers = ["Front", "Middle", "Back"]
    write_json(f"{base}/Contents.json", {"info": INFO, "layers": [{"filename": f"{n}.solidimagestacklayer"} for n in layers]})
    for name in layers:
        lay = f"{base}/{name}.solidimagestacklayer"
        write_json(f"{lay}/Contents.json", {"info": INFO})
        fn = f"{name.lower()}.png"
        if name == "Back":
            img = background(1024, 1024)
        else:
            img = layer(1024, 1024, 512, 512, 349, "hands" if name == "Front" else "ring")
        save(img, f"{lay}/Content.imageset/{fn}")
        write_json(f"{lay}/Content.imageset/Contents.json",
                   {"images": [{"filename": fn, "idiom": "vision", "scale": "2x"}], "info": INFO})


if __name__ == "__main__":
    tv()
    vision()
    print("ok")
