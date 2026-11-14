#!/usr/bin/env node
// App Store görselleri (2. analiz #33, 3 Kasım 2026): simülatör ekran görüntülerini telefon/iPad çerçevesine koyar, üstüne kısa
// ve kalın bir başlık ile bir alt satır yazar. Gündüz görselleri kâğıt zemin (#f7f4ee), "gece" görseli komodin sahnesi
// (karanlık oda, lamba ışığı, ahşap komodin, standa yaslanmış telefonda gece modu). Gerçek ortam fotoğrafı yerine çizim:
// fotoğraf çekimi gerektirmez, her dilde aynı sahne. Çıktı girdiyle aynı piksel boyutundadır (App Store ölçüsü korunur).
//
// Kullanım: node frame-screenshots.mjs <ham klasör> <çıktı klasörü>
//   Klasör yapısı: <dil>/NN-ad.png, <dil>/ipad-NN-ad.png (store-screenshots.sh çıktısı). Başlıklar fastlane/captions.json.
// Tarayıcı: yerelde "playwright" (Chromium), CI'da (macOS) "playwright-core" + sistemdeki Google Chrome (PW_DIR, CHROME_PATH).
import { readFileSync, readdirSync, mkdirSync, existsSync, statSync } from "node:fs";
import { join, dirname } from "node:path";
import { createRequire } from "node:module";

const [IN, OUT] = process.argv.slice(2);
if (!IN || !OUT) { console.error("Kullanım: frame-screenshots.mjs <ham klasör> <çıktı klasörü>"); process.exit(2); }
const HERE = dirname(new URL(import.meta.url).pathname);
const CAPTIONS = JSON.parse(readFileSync(join(HERE, "../fastlane/captions.json"), "utf8"));
// fastlane klasörü → başlık dili (captions.json'daki dil). Bölgesel sürümler ana dilin başlığını kullanır; Malayca (ms),
// Bengalce (bn-BD) ve Geleneksel Çince (zh-Hant) görsellerinde arayüz başka dilde olsa da başlık o dilde (store-screenshots.sh)
const LANG = {
  tr: "tr", "en-US": "en", "en-GB": "en", "en-AU": "en", "en-CA": "en", "de-DE": "de", "ar-SA": "ar", "fr-FR": "fr", "fr-CA": "fr",
  ru: "ru", id: "id", ja: "ja", ko: "ko", "zh-Hans": "zh", "zh-Hant": "zh-Hant", th: "th", "es-ES": "es", "es-MX": "es",
  "pt-BR": "pt", "pt-PT": "pt", hi: "hi", ms: "ms", "ur-PK": "ur", "bn-BD": "bn",
};
const HTML_LANG = { zh: "zh-Hans", pt: "pt-BR" };
const RTL = new Set(["ar", "ur", "fa"]);

function loadPlaywright() {
  const tries = [];
  if (process.env.PW_DIR) tries.push([createRequire(join(process.env.PW_DIR, "x.js")), "playwright-core"]);
  tries.push([createRequire(import.meta.url), "playwright"], [createRequire(import.meta.url), "playwright-core"]);
  for (const [req, name] of tries) { try { return req(name); } catch {} }
  throw new Error("playwright bulunamadı (PW_DIR ile playwright-core kurulu klasörü verin)");
}
function chromePath() {
  const list = [process.env.CHROME_PATH, "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"].filter(Boolean);
  return list.find((p) => existsSync(p));
}
function pngSize(file) {
  const b = readFileSync(file);
  if (b.toString("ascii", 1, 4) !== "PNG") throw new Error(`${file}: PNG değil`);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20), data: b };
}
const esc = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

const FONTS = `-apple-system, "SF Pro Display", "Helvetica Neue", "Geeza Pro", "Hiragino Sans", "PingFang SC", "Apple SD Gothic Neo", "Thonburi", "Kohinoor Bangla", "Noto Sans Bengali", "Noto Sans", "DejaVu Sans", sans-serif`;
// Geleneksel Çince başlıklarda Geleneksel yazı biçimli yazı tipi önce gelsin
const fontsFor = (lang) => (lang === "zh-Hant" ? FONTS.replace('"PingFang SC"', '"PingFang TC", "PingFang HK", "PingFang SC"') : FONTS);

/** Bir görselin HTML'i. kind: "phone" | "tablet" | "night" */
function page({ w, h, img, lang, title, sub, kind }) {
  const base = Math.min(w, h * 0.46);
  const night = kind === "night";
  const tablet = kind === "tablet";
  const dir = RTL.has(lang) ? "rtl" : "ltr";
  // Cihaz ölçüleri: ekran oranı görüntüden; çerçeve kalınlığı ve köşe yarıçapı ekran genişliğine oranla (yerleşim sayfadaki betikte)
  const ratio = img.h / img.w;
  const bez = tablet ? 0.028 : 0.034; // çerçeve / ekran genişliği
  const rad = tablet ? 0.05 : 0.15;
  const css = `
  :root { --base: ${base}px; --ratio: ${ratio}; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html, body { width: ${w}px; height: ${h}px; overflow: hidden; }
  body { font-family: ${fontsFor(lang)}; -webkit-font-smoothing: antialiased; display: flex; flex-direction: column; align-items: center;
    background: ${night ? "linear-gradient(180deg, #0b0e14 0%, #141925 55%, #1a1f2b 100%)"
      : "radial-gradient(120% 60% at 50% 0%, #fdf3df 0%, rgba(253,243,223,0) 60%), #f7f4ee"}; }
  header { width: 100%; padding: calc(var(--base) * 0.085) calc(var(--base) * 0.07) 0; text-align: center; position: relative; z-index: 3; }
  h1 { font-size: calc(var(--base) * ${tablet ? 0.072 : 0.078}); line-height: 1.12; font-weight: 800; letter-spacing: -0.01em;
    color: ${night ? "#efece6" : "#1b1b1b"}; }
  p { margin-top: calc(var(--base) * 0.026); font-size: calc(var(--base) * 0.038); line-height: 1.3; font-weight: 500;
    color: ${night ? "#a9a397" : "#6b6458"}; }
  h1::after { content: ""; display: block; width: calc(var(--base) * 0.09); height: calc(var(--base) * 0.012);
    border-radius: 99px; background: ${night ? "#c0392b" : "#d68a1f"}; margin: calc(var(--base) * 0.032) auto 0; }
  .stage { flex: 1; width: 100%; min-height: 0; position: relative; display: flex; justify-content: center; align-items: flex-start;
    padding-top: calc(var(--base) * 0.06); padding-bottom: calc(var(--base) * ${night ? 0.0 : 0.05}); }
  .device { position: relative; flex: none; background: #17181b; z-index: 2;
    box-shadow: inset 0 0 0 calc(var(--base) * 0.004) #3a3c42, 0 calc(var(--base) * 0.03) calc(var(--base) * 0.08) rgba(${night ? "0,0,0,.55" : "60,45,20,.25"}); }
  .screen { position: absolute; overflow: hidden; background: #000; }
  .screen img { display: block; width: 100%; height: 100%; }
  .island { position: absolute; top: 1.25%; left: 50%; transform: translateX(-50%); width: 29%; border-radius: 99px; background: #000; }
  ${night ? nightCss(base) : ""}
  `;
  const device = `<div class="device"><div class="screen"><img src="data:image/png;base64,${img.data.toString("base64")}" alt="">${tablet ? "" : '<div class="island"></div>'}</div></div>`;
  const scene = night ? `<div class="lamp"><div class="glow"></div><div class="shade"></div><div class="stem"></div><div class="foot"></div></div>
    <div class="books"><i></i><i></i><i></i></div><div class="table"></div><div class="stand"></div>` : "";
  const fit = `(() => {
    const st = document.querySelector(".stage"), cs = getComputedStyle(st);
    const availH = st.clientHeight - parseFloat(cs.paddingTop) - parseFloat(cs.paddingBottom);
    const maxW = st.clientWidth * ${tablet ? 0.86 : night ? 0.56 : 0.8};
    const r = ${ratio}, b = ${bez}, k = ${rad};
    const sw = Math.min(maxW / (1 + 2 * b), availH / (r + 2 * b)); // ekran genişliği
    const d = document.querySelector(".device"), sc = document.querySelector(".screen"), is = document.querySelector(".island");
    d.style.width = sw * (1 + 2 * b) + "px"; d.style.height = sw * (r + 2 * b) + "px"; d.style.borderRadius = sw * k + "px";
    Object.assign(sc.style, { left: sw * b + "px", top: sw * b + "px", width: sw + "px", height: sw * r + "px", borderRadius: sw * (k - b) + "px" });
    if (is) is.style.height = sw * 0.085 + "px";
  })();`;
  return `<!doctype html><html lang="${HTML_LANG[lang] || lang}" dir="${dir}"><head><meta charset="utf-8"><style>${css}</style></head>
  <body><header><h1>${esc(title)}</h1><p>${esc(sub)}</p></header><div class="stage">${scene}${device}</div><script>${fit}</script></body></html>`;
}

// Komodin sahnesi: telefon standın üstünde, alt kısmı komodinin üst yüzeyine oturur
function nightCss(base) {
  return `
  .stage { align-items: flex-end; padding-bottom: calc(var(--base) * 0.26); }
  .table { position: absolute; left: -2%; right: -2%; bottom: 0; height: calc(var(--base) * 0.3); z-index: 1;
    background: linear-gradient(180deg, #8a5f3b 0%, #6e4a2c 7%, #4a3020 8%, #3a2618 40%, #24170e 100%);
    box-shadow: 0 calc(var(--base) * -0.01) calc(var(--base) * 0.04) rgba(255,170,90,.08); }
  .stand { position: absolute; left: 50%; transform: translateX(-50%); bottom: calc(var(--base) * 0.27); width: 30%; height: calc(var(--base) * 0.05);
    border-radius: calc(var(--base) * 0.02) calc(var(--base) * 0.02) calc(var(--base) * 0.01) calc(var(--base) * 0.01);
    background: linear-gradient(180deg, #3b3e45, #1f2125); z-index: 3; box-shadow: 0 calc(var(--base) * 0.012) calc(var(--base) * 0.02) rgba(0,0,0,.6); }
  .lamp { position: absolute; left: 2%; bottom: calc(var(--base) * 0.27); width: 17%; height: 50%; z-index: 1; }
  .lamp .glow { position: absolute; left: -60%; top: -25%; width: 220%; height: 120%;
    background: radial-gradient(closest-side, rgba(255,184,108,.32), rgba(255,170,90,.10) 55%, rgba(255,170,90,0)); }
  .lamp .shade { position: absolute; left: 0; right: 0; top: 0; height: 34%; clip-path: polygon(18% 0, 82% 0, 100% 100%, 0 100%);
    background: linear-gradient(180deg, #f3d9a8, #e2b06a); box-shadow: 0 0 calc(var(--base) * 0.08) rgba(255,190,110,.6); }
  .lamp .stem { position: absolute; left: 47%; width: 6%; top: 34%; bottom: 6%; background: linear-gradient(90deg, #6b5a44, #a48a63, #6b5a44); }
  .lamp .foot { position: absolute; left: 22%; right: 22%; bottom: 0; height: 6%; border-radius: 50% 50% 8% 8%; background: linear-gradient(180deg, #b0946a, #5d4c38); }
  .books { position: absolute; right: 3%; bottom: calc(var(--base) * 0.27); width: 15%; z-index: 1; display: flex; flex-direction: column; align-items: flex-end; }
  .books i { display: block; height: calc(var(--base) * 0.045); border-radius: calc(var(--base) * 0.006); margin-top: calc(var(--base) * 0.004); }
  .books i:nth-child(1) { width: 78%; background: linear-gradient(180deg, #3f5a74, #2b3f52); }
  .books i:nth-child(2) { width: 92%; background: linear-gradient(180deg, #7a3b34, #55261f); }
  .books i:nth-child(3) { width: 100%; background: linear-gradient(180deg, #4b5e3c, #33412a); }
  `;
}

const pw = loadPlaywright();
const browser = await pw.chromium.launch({ executablePath: chromePath() });
const ctx = await browser.newContext({ deviceScaleFactor: 1 });
const pg = await ctx.newPage();
let n = 0;
for (const folder of readdirSync(IN).sort()) {
  const dir = join(IN, folder);
  if (!statSync(dir).isDirectory()) continue;
  const lang = LANG[folder];
  if (!lang) { console.warn(`${folder}: başlık dili tanımlı değil, atlandı`); continue; }
  mkdirSync(join(OUT, folder), { recursive: true });
  for (const file of readdirSync(dir).filter((f) => f.endsWith(".png")).sort()) {
    const key = file.replace(/^ipad-/, "").replace(/^\d+-/, "").replace(/\.png$/, "");
    const cap = CAPTIONS[key]?.[lang];
    if (!cap) throw new Error(`${folder}/${file}: captions.json'da "${key}.${lang}" yok`);
    const img = pngSize(join(dir, file));
    const kind = file.startsWith("ipad-") ? "tablet" : key === "gece" ? "night" : "phone";
    await pg.setViewportSize({ width: img.w, height: img.h });
    await pg.setContent(page({ w: img.w, h: img.h, img, lang, title: cap[0], sub: cap[1], kind }), { waitUntil: "load" });
    await pg.evaluate(() => document.fonts.ready);
    await pg.screenshot({ path: join(OUT, folder, file), type: "png" });
    n++;
  }
}
await browser.close();
console.log(`${n} görsel çerçevelendi → ${OUT}`);
