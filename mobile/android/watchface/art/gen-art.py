# Kullanım: python3 mobile/android/watchface/art/gen-art.py mobile/android/watchface/src/main/res/drawable-nodpi (Pillow gerekir)
# Saat Tarih Wear OS kadranı görselleri (450×450, merkez 225,225): kadran, akrep, yelkovan, saniye ve önizleme
import math, sys
from PIL import Image, ImageDraw, ImageFont
out = sys.argv[1]
S = 450; C = S / 2
AMBER = (240, 165, 92, 255); INK = (238, 240, 244, 255); MUTED = (150, 160, 180, 255)
def font(sz):
    for p in ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf"]:
        try: return ImageFont.truetype(p, sz)
        except OSError: pass
    return ImageFont.load_default()
# Kadran: 60 çizgi (saat başları kalın), 12 rakam; zemin saydam (sahnenin rengi altta)
dial = Image.new("RGBA", (S * 2, S * 2), (0, 0, 0, 0)); d = ImageDraw.Draw(dial); k = 2
for i in range(60):
    a = math.radians(i * 6); big = i % 5 == 0
    r1, r2 = (C - 10) * k, (C - (34 if big else 22)) * k
    x1, y1 = C * k + r1 * math.sin(a), C * k - r1 * math.cos(a)
    x2, y2 = C * k + r2 * math.sin(a), C * k - r2 * math.cos(a)
    d.line([(x1, y1), (x2, y2)], fill=INK if big else MUTED, width=(8 if big else 3))
f = font(34 * k)
for h in range(1, 13):
    a = math.radians(h * 30); r = (C - 66) * k
    x, y = C * k + r * math.sin(a), C * k - r * math.cos(a)
    t = str(h); bb = d.textbbox((0, 0), t, font=f)
    d.text((x - (bb[2] - bb[0]) / 2 - bb[0], y - (bb[3] - bb[1]) / 2 - bb[1]), t, font=f, fill=INK)
dial = dial.resize((S, S), Image.LANCZOS); dial.save(f"{out}/wf_dial.png")
# Kollar: 12'yi gösterir, merkez etrafında döner
def hand(name, length, width, color, tail=0, dot=0):
    im = Image.new("RGBA", (S * 2, S * 2), (0, 0, 0, 0)); g = ImageDraw.Draw(im)
    g.rounded_rectangle([C * 2 - width, C * 2 - length * 2, C * 2 + width, C * 2 + tail * 2], radius=width, fill=color)
    if dot: g.ellipse([C * 2 - dot, C * 2 - dot, C * 2 + dot, C * 2 + dot], fill=color)
    im.resize((S, S), Image.LANCZOS).save(f"{out}/{name}.png")
hand("wf_hour", 110, 9, INK, tail=14)
hand("wf_minute", 165, 6, AMBER, tail=18)
hand("wf_second", 185, 2, (230, 120, 70, 255), tail=30, dot=12)
# Önizleme: 10:08:32, iki komplikasyon yeri
pv = Image.new("RGBA", (S, S), (11, 16, 32, 255))
mask = Image.new("L", (S, S), 0); ImageDraw.Draw(mask).ellipse([0, 0, S, S], fill=255)
pv.alpha_composite(dial)
def rot(name, deg):
    h = Image.open(f"{out}/{name}.png"); pv.alpha_composite(h.rotate(-deg, resample=Image.BICUBIC, center=(C, C)))
pg = ImageDraw.Draw(pv); sf = font(22)
for (txt, y) in [("Öğle 12:41", 132), ("31 Eki", 300)]:
    bb = pg.textbbox((0, 0), txt, font=sf); pg.text((C - (bb[2] - bb[0]) / 2, y), txt, font=sf, fill=AMBER if y < 200 else MUTED)
rot("wf_hour", (10 + 8 / 60) * 30); rot("wf_minute", 8 * 6 + 32 / 10); rot("wf_second", 32 * 6)
out_pv = Image.new("RGBA", (S, S), (0, 0, 0, 0)); out_pv.paste(pv, (0, 0), mask)
out_pv.save(f"{out}/wf_preview.png")
