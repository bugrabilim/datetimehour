import sys
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools import subset
src, out = sys.argv[1], sys.argv[2]
UNI = ("U+0020-007E,U+00A0-024F,U+0259,U+018F,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0300-0301,U+0400-045F,"
       "U+0490-0491,U+04B0-04B1,U+04D8-04D9,U+2010-2027,U+2030-203A,U+20AC,U+20BA,U+20BD,U+2116,U+2122,U+2190-2193,U+2212")
for wght, style in ((500, "Medium"), (600, "SemiBold")):
    f = TTFont(src)
    inst = instancer.instantiateVariableFont(f, {"wght": wght, "opsz": 24}, updateFontNames=False)
    name = inst["name"]
    # Ad tablosu: aile "Literata", alt aile ağırlık; PostScript adı Literata-<Ağırlık> (iOS Font.custom bu adla bulur)
    for rec in list(name.names):
        if rec.nameID in (1, 2, 3, 4, 6, 16, 17, 25):
            name.removeNames(nameID=rec.nameID)
    name.setName("Literata " + style, 1, 3, 1, 0x409); name.setName("Regular", 2, 3, 1, 0x409)
    name.setName("Literata " + style + "; subset Saat Tarih", 3, 3, 1, 0x409)
    name.setName("Literata " + style, 4, 3, 1, 0x409); name.setName("Literata-" + style, 6, 3, 1, 0x409)
    name.setName("Literata", 16, 3, 1, 0x409); name.setName(style, 17, 3, 1, 0x409)
    inst["OS/2"].usWeightClass = wght
    opts = subset.Options(); opts.layout_features = ["kern", "liga", "tnum", "lnum", "case", "locl", "mark", "mkmk", "ccmp"]
    opts.name_IDs = ["*"]; opts.notdef_outline = True; opts.drop_tables += ["STAT"]
    sub = subset.Subsetter(opts); sub.populate(unicodes=subset.parse_unicodes(UNI)); sub.subset(inst)
    inst.save(f"{out}/Literata-{style}.ttf")
    inst.flavor = "woff2"; inst.save(f"{out}/literata-{wght}.woff2")
    print(style, "ok")
