#!/usr/bin/env python3
"""Alternatif uygulama simgelerini üretir (2. analiz #13): Gece, Kâğıt, Çevirmeli, Nane.

App/Resources/Assets.xcassets altına AppIcon-<ad>.appiconset (1024x1024) ve seçicideki küçük örnekler için
AppIconPreview(-<ad>).imageset (180x180) yazar. Çizim varsayılan simgeyle aynı oranlarda (dış yarıçap 349 px,
halka kalınlığı 58 px, yelkovan yukarı, akrep saat 4).

Çalıştırma (depo kökünden): python3 mobile/ios/scripts/gen-alt-icons.py   (Pillow gerekir)
"""
import json
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "App", "Resources", "Assets.xcassets")
INFO = {"author": "xcode", "version": 1}
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
SS = 4
N = 1024


def clock(bg, ring, hands, dot):
    W = N * SS
    img = Image.new("RGB", (W, W), bg)
    d = ImageDraw.Draw(img)
    c, r, stroke = W / 2, 349 * SS, 58 * SS
    d.ellipse([c - r, c - r, c + r, c + r], fill=ring)
    ri = r - stroke
    d.ellipse([c - ri, c - ri, c + ri, c + ri], fill=bg)

    def hand(x2, y2):
        d.line([(c, c), (x2, y2)], fill=hands, width=round(stroke))
        for (x, y) in [(c, c), (x2, y2)]:
            d.ellipse([x - stroke / 2, y - stroke / 2, x + stroke / 2, y + stroke / 2], fill=hands)

    hand(c, c - 200 * SS)
    hand(c + 128 * SS, c + 86 * SS)
    k = 37 * SS
    d.ellipse([c - k, c - k, c + k, c + k], fill=dot)
    return img.resize((N, N), Image.LANCZOS)


def flip():
    """Çevirmeli saat: iki kart (12 ve 00), ortada ayrım çizgisi"""
    W = N * SS
    bg, card, text = (0x11, 0x14, 0x1A), (0x26, 0x2B, 0x36), (0xF2, 0xA6, 0x5A)
    img = Image.new("RGB", (W, W), bg)
    d = ImageDraw.Draw(img)
    font = ImageFont.truetype(FONT, 230 * SS)
    cw, ch, gap = 380 * SS, 470 * SS, 44 * SS
    y0 = (W - ch) / 2
    for i, s in enumerate(["12", "00"]):
        x0 = (W - 2 * cw - gap) / 2 + i * (cw + gap)
        d.rounded_rectangle([x0, y0, x0 + cw, y0 + ch], radius=48 * SS, fill=card)
        b = d.textbbox((0, 0), s, font=font)
        d.text((x0 + (cw - (b[2] - b[0])) / 2 - b[0], y0 + (ch - (b[3] - b[1])) / 2 - b[1]), s, font=font, fill=text)
        d.rectangle([x0, y0 + ch / 2 - 5 * SS, x0 + cw, y0 + ch / 2 + 5 * SS], fill=bg)
    return img.resize((N, N), Image.LANCZOS)


ICONS = {
    "night": lambda: clock((0, 0, 0), (0xB8, 0x20, 0x16), (0x8A, 0x3A, 0x30), (0xB8, 0x20, 0x16)),
    "paper": lambda: clock((0xF7, 0xF4, 0xEE), (0x9A, 0x3F, 0x00), (0x1B, 0x1F, 0x27), (0x9A, 0x3F, 0x00)),
    "flip": flip,
    "mint": lambda: clock((0x1B, 0x7A, 0x43), (0xFF, 0xFF, 0xFF), (0xE8, 0xF5, 0xEC), (0xF5, 0xC0, 0x4A)),
}


def write(folder, files, contents):
    os.makedirs(folder, exist_ok=True)
    for name, img in files:
        img.save(os.path.join(folder, name))
    with open(os.path.join(folder, "Contents.json"), "w") as f:
        json.dump(contents, f, indent=2)
        f.write("\n")


def preview(name, img):
    write(os.path.join(ROOT, f"{name}.imageset"), [(f"{name}.png", img.resize((180, 180), Image.LANCZOS))],
          {"images": [{"filename": f"{name}.png", "idiom": "universal"}], "info": INFO})


for key, make in ICONS.items():
    img = make()
    write(os.path.join(ROOT, f"AppIcon-{key}.appiconset"), [("AppIcon.png", img)],
          {"images": [{"filename": "AppIcon.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"}], "info": INFO})
    preview(f"AppIconPreview-{key}", img)

preview("AppIconPreview", Image.open(os.path.join(ROOT, "AppIcon.appiconset", "AppIcon.png")).convert("RGB"))
print("tamam:", ", ".join(ICONS))
