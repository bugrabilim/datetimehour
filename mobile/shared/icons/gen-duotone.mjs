/* İki tonlu ("duotone") küçük simgeler (8 Kasım 2026, sahibin kararı "B Karma": 3D yalnız hava, gökyüzü, vakit satırları
   ve büyük çizimlerde; liste, kare, sekme ve satır simgeleri iki tonlu). Kaynak Phosphor Icons (@phosphor-icons/core,
   MIT; PHOSPHOR-LICENSE.txt), sabit sürüm ve bütünlük özeti duotone-map.json'da. Her glif iki katmandır: arka katman
   (saydamlık 0,2) ve ön katman (dolu). Yollar uygulamalardaki çizgi simge ayrıştırıcısının (LineIcons.kt,
   LineIcons.swift) anladığı komutlara çevrilir: yalnız mutlak M, L, C, Z; H/V → L, Q/T/S → C, yaylar (A) kübik
   Bézier'e; 256'lık alan 24'e ölçeklenir, iki ondalığa yuvarlanır. Doldurma kuralı sıfırdan farklı (nonzero; SVG
   varsayılanı): delikler yön farkıyla oluşur.
   Kullanım: node mobile/shared/icons/gen-duotone.mjs          → duotone.json ve PHOSPHOR-LICENSE.txt yazar
             node mobile/shared/icons/gen-duotone.mjs --check  → güncel değilse hata verir (CI; npm ve ağ gerekir)
   Paket `npm pack` ile geçici klasöre indirilir, bütünlüğü denetlenir ve bellekte açılır (dosyaya çıkarılmaz).
   Uygulamalara gen-strings.mjs kopyalar (iOS App/Resources, Android assets). */
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import zlib from "node:zlib";
import crypto from "node:crypto";
import { execFileSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const MAP_FILE = path.join(HERE, "duotone-map.json");
const OUT_FILE = path.join(HERE, "duotone.json");
const LICENSE_FILE = path.join(HERE, "PHOSPHOR-LICENSE.txt");
const SCALE = 24 / 256;

/* ---------- SVG yolu → M L C Z ---------- */

const NUM = /[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?/y;

/** SVG yol metnini mutlak M/L/C/Z parçalarına çevirir (koordinatlar özgün ölçekte) */
export function toSegments(d) {
  let pos = 0;
  const skip = () => { while (pos < d.length && /[\s,]/.test(d[pos])) pos++; };
  const num = () => {
    skip();
    NUM.lastIndex = pos;
    const m = NUM.exec(d);
    if (!m) throw new Error(`sayı beklendi: ${d.slice(pos, pos + 20)}`);
    pos = NUM.lastIndex;
    return parseFloat(m[0]);
  };
  const flag = () => {
    skip();
    const c = d[pos++];
    if (c !== "0" && c !== "1") throw new Error(`yay bayrağı beklendi: ${c}`);
    return c === "1";
  };
  const out = [];
  let cx = 0, cy = 0, sx = 0, sy = 0;
  let lastC = null, lastQ = null; // önceki kübik/ikinci derece denetim noktası (S/T yansıması için)
  let closed = false;
  const ensureStart = () => { if (closed) { out.push({ t: "M", p: [cx, cy] }); closed = false; } };
  const line = (x, y) => { ensureStart(); out.push({ t: "L", p: [x, y] }); cx = x; cy = y; };
  const cubic = (x1, y1, x2, y2, x, y) => { ensureStart(); out.push({ t: "C", p: [x1, y1, x2, y2, x, y] }); cx = x; cy = y; };
  let cmd = null;
  while (true) {
    skip();
    if (pos >= d.length) break;
    if (/[A-Za-z]/.test(d[pos])) cmd = d[pos++];
    else if (!cmd) throw new Error("komut beklendi");
    const rel = cmd === cmd.toLowerCase();
    const C = cmd.toUpperCase();
    const ox = rel ? cx : 0, oy = rel ? cy : 0;
    let nextC = null, nextQ = null;
    switch (C) {
      case "M": {
        const x = num() + ox, y = num() + oy;
        out.push({ t: "M", p: [x, y] });
        cx = sx = x; cy = sy = y; closed = false;
        cmd = rel ? "l" : "L"; // M'den sonra gelen çiftler çizgidir
        break;
      }
      case "Z":
        out.push({ t: "Z" });
        cx = sx; cy = sy; closed = true;
        cmd = null; // Z'den sonra komut harfi gelmeli
        break;
      case "L": line(num() + ox, num() + oy); break;
      case "H": line(num() + ox, cy); break;
      case "V": line(cx, num() + oy); break;
      case "C": {
        const x1 = num() + ox, y1 = num() + oy, x2 = num() + ox, y2 = num() + oy, x = num() + ox, y = num() + oy;
        cubic(x1, y1, x2, y2, x, y);
        nextC = [x2, y2];
        break;
      }
      case "S": {
        const [rx, ry] = lastC ? [2 * cx - lastC[0], 2 * cy - lastC[1]] : [cx, cy];
        const x2 = num() + ox, y2 = num() + oy, x = num() + ox, y = num() + oy;
        cubic(rx, ry, x2, y2, x, y);
        nextC = [x2, y2];
        break;
      }
      case "Q": {
        const qx = num() + ox, qy = num() + oy, x = num() + ox, y = num() + oy;
        cubic(cx + (2 / 3) * (qx - cx), cy + (2 / 3) * (qy - cy), x + (2 / 3) * (qx - x), y + (2 / 3) * (qy - y), x, y);
        nextQ = [qx, qy];
        break;
      }
      case "T": {
        const [qx, qy] = lastQ ? [2 * cx - lastQ[0], 2 * cy - lastQ[1]] : [cx, cy];
        const x = num() + ox, y = num() + oy;
        cubic(cx + (2 / 3) * (qx - cx), cy + (2 / 3) * (qy - cy), x + (2 / 3) * (qx - x), y + (2 / 3) * (qy - y), x, y);
        nextQ = [qx, qy];
        break;
      }
      case "A": {
        const rx = num(), ry = num(), rot = num(), large = flag(), sweep = flag(), x = num() + ox, y = num() + oy;
        const parts = arcToCubics(cx, cy, rx, ry, rot, large, sweep, x, y);
        if (parts === null) line(x, y);
        else for (const c of parts) cubic(...c);
        cx = x; cy = y;
        break;
      }
      default:
        throw new Error(`bilinmeyen komut: ${cmd}`);
    }
    lastC = nextC; lastQ = nextQ;
    // Komut harfi yazılmadan yinelenen parametreler döngünün başında aynı komutla işlenir (cmd değişmez)
  }
  return out;
}

/** SVG yayı → kübik Bézier parçaları (SVG 1.1 ek F.6.5–F.6.6; parça başına en çok 90°). Yarıçap 0 ise null (düz çizgi). */
export function arcToCubics(x1, y1, rx, ry, rotDeg, large, sweep, x2, y2) {
  if (x1 === x2 && y1 === y2) return [];
  rx = Math.abs(rx); ry = Math.abs(ry);
  if (rx === 0 || ry === 0) return null;
  const phi = (rotDeg * Math.PI) / 180;
  const cos = Math.cos(phi), sin = Math.sin(phi);
  const dx2 = (x1 - x2) / 2, dy2 = (y1 - y2) / 2;
  const x1p = cos * dx2 + sin * dy2, y1p = -sin * dx2 + cos * dy2;
  const lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
  if (lambda > 1) { const s = Math.sqrt(lambda); rx *= s; ry *= s; }
  const num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
  const den = rx * rx * y1p * y1p + ry * ry * x1p * x1p;
  const coef = (large !== sweep ? 1 : -1) * Math.sqrt(Math.max(0, num / den));
  const cxp = (coef * rx * y1p) / ry, cyp = (-coef * ry * x1p) / rx;
  const cx = cos * cxp - sin * cyp + (x1 + x2) / 2, cy = sin * cxp + cos * cyp + (y1 + y2) / 2;
  const angle = (ux, uy, vx, vy) => Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
  const ux = (x1p - cxp) / rx, uy = (y1p - cyp) / ry, vx = (-x1p - cxp) / rx, vy = (-y1p - cyp) / ry;
  const theta1 = angle(1, 0, ux, uy);
  let dtheta = angle(ux, uy, vx, vy);
  if (!sweep && dtheta > 0) dtheta -= 2 * Math.PI;
  if (sweep && dtheta < 0) dtheta += 2 * Math.PI;
  const n = Math.max(1, Math.ceil(Math.abs(dtheta) / (Math.PI / 2) - 1e-9));
  const delta = dtheta / n;
  const t = (4 / 3) * Math.tan(delta / 4);
  const map = (px, py) => [cx + rx * px * cos - ry * py * sin, cy + rx * px * sin + ry * py * cos];
  const out = [];
  for (let i = 0; i < n; i++) {
    const a1 = theta1 + i * delta, a2 = a1 + delta;
    const c1a = Math.cos(a1), s1a = Math.sin(a1), c2a = Math.cos(a2), s2a = Math.sin(a2);
    const c1 = map(c1a - t * s1a, s1a + t * c1a);
    const c2 = map(c2a + t * s2a, s2a - t * c2a);
    const e = i === n - 1 ? [x2, y2] : map(c2a, s2a);
    out.push([...c1, ...c2, ...e]);
  }
  return out;
}

/** Sayı biçimi: iki ondalık, sondaki sıfırlar yok, "-0" yok */
const fmt = (v) => {
  const r = Math.round(v * SCALE * 100) / 100;
  return Object.is(r, -0) || r === 0 ? "0" : String(r);
};

/** SVG yolu → uygulamaların ayrıştırıcısına uygun 24'lük yol metni */
export function convertPath(d) {
  const out = [];
  let last = null;
  for (const s of toSegments(d)) {
    if (s.t === "Z") { out.push("Z"); last = null; continue; }
    const p = s.p.map(fmt);
    const end = p.slice(-2).join(" ");
    if (s.t === "L" && end === last) continue; // yuvarlamadan sonra boy 0 kalan çizgi
    out.push(s.t + p.join(" "));
    last = end;
  }
  return out.join(" ");
}

/** Duotone SVG → katmanlar [{o, d}] (belge sırasıyla: arka katman 0,2, ön katman 1) */
export function svgLayers(svg) {
  const layers = [];
  for (const m of svg.matchAll(/<path\b([^>]*)\/?>/g)) {
    const attrs = m[1];
    const d = /\bd="([^"]*)"/.exec(attrs)?.[1];
    if (!d) continue;
    const o = parseFloat(/\bopacity="([^"]*)"/.exec(attrs)?.[1] ?? "1");
    layers.push({ o, d: convertPath(d) });
  }
  if (/<(circle|rect|line|polyline|polygon|ellipse)\b/.test(svg)) throw new Error("yalnız <path> destekleniyor");
  return layers;
}

/* ---------- Paket: npm pack → bellekte tar ---------- */

/** .tgz içinden istenen dosyaları okur (ustar + pax başlıkları) */
function untar(tgz, wanted) {
  const buf = zlib.gunzipSync(tgz);
  const files = new Map();
  let off = 0, paxPath = null, longName = null;
  const str = (a, b) => buf.toString("utf8", a, b).replace(/\0.*$/s, "");
  while (off + 512 <= buf.length) {
    const h = buf.subarray(off, off + 512);
    if (h.every((b) => b === 0)) break;
    const size = parseInt(str(off + 124, off + 136).trim() || "0", 8);
    const type = String.fromCharCode(h[156] || 48);
    const prefix = str(off + 345, off + 500);
    let name = longName ?? paxPath ?? (prefix ? `${prefix}/${str(off, off + 100)}` : str(off, off + 100));
    const body = buf.subarray(off + 512, off + 512 + size);
    off += 512 + Math.ceil(size / 512) * 512;
    if (type === "x") {
      const rec = body.toString("utf8");
      paxPath = /(?:^|\n)\d+ path=([^\n]*)\n/.exec(rec)?.[1] ?? null;
      continue;
    }
    if (type === "L") { longName = body.toString("utf8").replace(/\0.*$/s, ""); continue; }
    paxPath = null; longName = null;
    if ((type === "0" || type === "\0") && wanted(name)) files.set(name, Buffer.from(body));
  }
  return files;
}

function fetchPackage(cfg) {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "phosphor-"));
  try {
    const spec = `${cfg.package}@${cfg.version}`;
    const raw = execFileSync("npm", ["pack", spec, "--json", "--pack-destination", tmp], { cwd: tmp, encoding: "utf8", stdio: ["ignore", "pipe", "inherit"] });
    const info = JSON.parse(raw)[0];
    const tgz = fs.readFileSync(path.join(tmp, info.filename));
    const integrity = "sha512-" + crypto.createHash("sha512").update(tgz).digest("base64");
    if (integrity !== cfg.integrity) throw new Error(`${spec} bütünlüğü tutmuyor: ${integrity}`);
    return untar(tgz, (n) => n === "package/LICENSE" || /^package\/assets\/duotone\/[a-z0-9-]+-duotone\.svg$/.test(n));
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
}

/* ---------- Çıktı ---------- */

export function build(cfg, files) {
  const lines = [];
  const note = `Saat Tarih'in iki tonlu küçük simgeleri (8 Kasım 2026, sahibin kararı "B Karma"; kurallar mobile/shared/icons/DUOTONE.md). ` +
    `Kaynak Phosphor Icons ${cfg.package}@${cfg.version} (MIT, PHOSPHOR-LICENSE.txt), duotone ağırlığı; ad → glif eşlemesi duotone-map.json. ` +
    `Biçim: ad → [{o: saydamlık (0.2 arka katman, 1 ön katman), d: yol}]; yollar 24×24 alanda, yalnız mutlak M L C Z, doldurma kuralı nonzero; ` +
    `iki katman aynı renkle, sırayla doldurulur. Elle düzenlemeyin; yeniden üretmek: node mobile/shared/icons/gen-duotone.mjs ` +
    `(gen-strings.mjs uygulamalara kopyalar).`;
  lines.push(`  "_note": ${JSON.stringify(note)}`);
  for (const [name, glyph] of Object.entries(cfg.icons)) {
    const svg = files.get(`package/assets/duotone/${glyph}-duotone.svg`);
    if (!svg) throw new Error(`Phosphor'da yok: ${glyph} (${name})`);
    const layers = svgLayers(svg.toString("utf8"));
    if (layers.length !== 2 || layers[0].o !== 0.2 || layers[1].o !== 1) throw new Error(`beklenmeyen katmanlar: ${glyph}`);
    lines.push(`  ${JSON.stringify(name)}: [${layers.map((l) => `{"o": ${l.o}, "d": ${JSON.stringify(l.d)}}`).join(", ")}]`);
  }
  const license = files.get("package/LICENSE");
  if (!license) throw new Error("LICENSE yok");
  return { json: `{\n${lines.join(",\n")}\n}\n`, license: license.toString("utf8") };
}

function main() {
  const cfg = JSON.parse(fs.readFileSync(MAP_FILE, "utf8"));
  const { json, license } = build(cfg, fetchPackage(cfg));
  const check = process.argv.includes("--check");
  const targets = [[OUT_FILE, json], [LICENSE_FILE, license]];
  if (check) {
    const stale = targets.filter(([f, body]) => !fs.existsSync(f) || fs.readFileSync(f, "utf8") !== body).map(([f]) => path.basename(f));
    if (stale.length) {
      console.error(`Güncel değil: ${stale.join(", ")}. Çalıştırın: node mobile/shared/icons/gen-duotone.mjs`);
      process.exit(1);
    }
    console.log(`İki tonlu simgeler güncel (${Object.keys(cfg.icons).length}).`);
  } else {
    for (const [f, body] of targets) fs.writeFileSync(f, body);
    console.log(`${Object.keys(cfg.icons).length} simge, ${Math.round(json.length / 1024)} KB: ${path.relative(process.cwd(), OUT_FILE)}`);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) main();
