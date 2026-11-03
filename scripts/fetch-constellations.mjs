/* Takımyıldız çizgileri ve adları (3 Kasım 2026, 2. analiz #156, #157): d3-celestial 0.7.35 veri dosyalarından
   (constellations.lines.json, constellations.json; © 2015 Olaf Frohn, BSD-3-Clause lisansı; lisans metni dosyada).
   Koordinatlar J2000 sağ açıklık (derece, 0–360) ve dik açıklık; 2 ondalığa yuvarlanır. Adlar: kaynakta olan
   dillerde (tr, en, de, ar, fa, fr, ru, ja, ko, zh) o dilde, diğerlerinde (az, id, ur, fil, th) Latince ad.
   Elle çalıştırılır (derleme ağa çıkmaz): node scripts/fetch-constellations.mjs */
import fs from "node:fs";
const BASE = "https://cdn.jsdelivr.net/npm/d3-celestial@0.7.35";
const get = async (p) => { const r = await fetch(`${BASE}/${p}`); if (!r.ok) throw new Error(`${p}: ${r.status}`); return p.endsWith(".json") ? r.json() : r.text(); };
const [lines, names, license] = await Promise.all([get("data/constellations.lines.json"), get("data/constellations.json"), get("LICENSE")]);
const LANGS = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th"];
const r2 = (x) => Math.round(x * 100) / 100;
const out = { source: "d3-celestial 0.7.35 (https://github.com/ofrohn/d3-celestial), data/constellations.lines.json and data/constellations.json", license: license.trim(), con: {} };
for (const f of lines.features) out.con[f.id] = { rank: +f.properties.rank, lines: f.geometry.coordinates.map((l) => l.map(([ra, dec]) => [r2(ra < 0 ? ra + 360 : ra), r2(dec)])) };
const T2S = { "儀": "仪", "劍": "剑", "圓": "圆", "壇": "坛", "寶": "宝", "時": "时", "烏": "乌", "爐": "炉", "獅": "狮", "獵": "猎", "盤": "盘", "網": "网", "繪": "绘", "羅": "罗", "蒼": "苍", "處": "处", "蠅": "蝇", "蠍": "蝎", "規": "规", "貓": "猫", "遠": "远", "鏡": "镜", "鐘": "钟", "長": "长", "雙": "双", "顯": "显", "飛": "飞", "馬": "马", "髮": "发", "魚": "鱼", "鯨": "鲸", "鳳": "凤", "鴉": "鸦", "鴿": "鸽", "鵑": "鹃", "鵝": "鹅", "鶴": "鹤", "鷹": "鹰", "龍": "龙", "戶": "户" }; // kaynak geleneksel Çince, uygulama basitleştirilmiş
for (const f of names.features) {
  const c = out.con[f.id] || (out.con[f.id] = { rank: +f.properties.rank, lines: [] });
  const p = f.properties, [ra, dec] = f.geometry.coordinates;
  c.at = [r2(ra < 0 ? ra + 360 : ra), r2(dec)];
  c.n = Object.fromEntries(LANGS.map((l) => [l, (p[l] && p[l].trim()) || p.la || p.name]));
  c.n.zh = [...c.n.zh].map((ch) => T2S[ch] || ch).join("");
}
fs.writeFileSync(new URL("../src/data/constellations.json", import.meta.url), JSON.stringify(out) + "\n");
console.log(Object.keys(out.con).length, "takımyıldız");
