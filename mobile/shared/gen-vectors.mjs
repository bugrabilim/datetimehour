/* Mobil uygulamalar için ortak test verisi ("altın vektörler").
   Sitedeki hesap kodu doğrulanmış kaynaktır: src/js/prayer-calc.js (Diyanet ile ±1 dk) ve src/js/nature-calc.js (Meeus).
   Swift (mobile/ios/SaatCore) ve Kotlin (mobile/android/core) sürümleri buradaki sonuçları birebir vermelidir.
   Kullanım: node mobile/shared/gen-vectors.mjs          → testdata/*.json dosyalarını yazar
             node mobile/shared/gen-vectors.mjs --check  → dosyalar güncel değilse hata verir (CI) */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "../..");
const OUT = path.join(HERE, "testdata");

function load(rel) {
  const m = { exports: {} };
  new Function("module", "window", fs.readFileSync(path.join(ROOT, rel), "utf8"))(m, undefined);
  return m.exports;
}
const P = load("src/js/prayer-calc.js");
const N = load("src/js/nature-calc.js");
const provinces = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/provinces.json"), "utf8"));
const cities = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/cities.json"), "utf8"));

/* Bir saat diliminin o gün öğlen UTC'ye göre farkı (saat) */
function tzOffset(tz, y, m, d) {
  const s = new Intl.DateTimeFormat("en-US", { timeZone: tz, timeZoneName: "longOffset" })
    .formatToParts(new Date(Date.UTC(y, m - 1, d, 12)))
    .find((p) => p.type === "timeZoneName").value; // "GMT+03:00" ya da "GMT"
  const mm = s.match(/([+-])(\d\d):(\d\d)/);
  return mm ? (mm[1] === "-" ? -1 : 1) * (+mm[2] + +mm[3] / 60) : 0;
}
const num = (x) => (Number.isFinite(x) ? x : null); // NaN JSON'da yok: null

/* Namaz vakitleri: 81 il + 31 dünya şehri, 2026'nın her ayının 15'i; kutup bölgesi için ek yerler */
const KEYS = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"];
const places = [
  ...provinces.map((p) => ({ id: p.slug, lat: p.lat, lon: p.lon, tz: "Europe/Istanbul" })),
  ...cities.filter((c) => c.tz !== "Europe/Istanbul").map((c) => ({ id: c.key, lat: c.lat, lon: c.lon, tz: c.tz })),
  { id: "reykjavik", lat: 64.1, lon: -21.9, tz: "Atlantic/Reykjavik" }, // imsak/yatsı açısı oluşmaz: yedide bir kuralı
  { id: "tromso", lat: 69.65, lon: 18.96, tz: "Europe/Oslo" }, // gece yarısı güneşi / kutup gecesi: vakit yok
];
const dates = Array.from({ length: 12 }, (_, i) => [2026, i + 1, 15]);
dates.push([2026, 6, 21], [2026, 12, 21], [2027, 3, 20]);
const prayerCases = [];
for (const pl of places) {
  for (const [y, m, d] of dates) {
    const tz = tzOffset(pl.tz, y, m, d);
    const r = P.times(y, m, d, pl.lat, pl.lon, tz);
    prayerCases.push({ id: pl.id, y, m, d, lat: pl.lat, lon: pl.lon, tz, t: KEYS.map((k) => num(r[k])) });
  }
}

/* Doğa: 2000–2050 mevsim anları, 2020–2035 yeni ay/dolunay anları, 2026–2028 Ay durumu örnekleri */
const DAY = 86400000;
const kFor = (ms) => Math.floor((ms / DAY + 2440587.5 - 2451550.09766) / 29.530588861);
const seasons = [];
for (let y = 2000; y <= 2050; y++) seasons.push({ y, ...N.seasons(y) });
const phases = [];
for (let k = kFor(Date.UTC(2020, 0, 1)); k <= kFor(Date.UTC(2035, 11, 31)); k++) phases.push({ k, new: N.phaseMs(k, false), full: N.phaseMs(k, true) });
const moon = [];
for (let ms = Date.UTC(2026, 0, 1); ms < Date.UTC(2029, 0, 1); ms += Math.round(7.3 * DAY)) {
  const r = N.moon(ms);
  moon.push({ ms, age: r.age, frac: r.frac, illum: r.illum, idx: r.idx, nextNew: r.nextNew, nextFull: r.nextFull });
}

/* Her durum tek satır: farklar okunur kalır */
const lines = (arr) => "[\n" + arr.map((o) => "    " + JSON.stringify(o)).join(",\n") + "\n  ]";
const files = {
  "prayer-times.json": `{
  "source": "src/js/prayer-calc.js",
  "note": "t: yerel saat, ondalık saat (fajr, sunrise, dhuhr, asr, maghrib, isha); null = vakit oluşmuyor. tz: o gün UTC farkı (saat).",
  "keys": ${JSON.stringify(KEYS)},
  "cases": ${lines(prayerCases)}
}
`,
  "nature.json": `{
  "source": "src/js/nature-calc.js",
  "note": "Zamanlar UTC milisaniye. seasons: yılın 4 mevsim anı; phases: k için yeni ay ve dolunay; moon: o andaki Ay durumu.",
  "seasons": ${lines(seasons)},
  "phases": ${lines(phases)},
  "moon": ${lines(moon)}
}
`,
};

const check = process.argv.includes("--check");
let stale = [];
fs.mkdirSync(OUT, { recursive: true });
for (const [name, body] of Object.entries(files)) {
  const file = path.join(OUT, name);
  if (check) {
    if (!fs.existsSync(file) || fs.readFileSync(file, "utf8") !== body) stale.push(name);
  } else fs.writeFileSync(file, body);
}
if (stale.length) {
  console.error(`Güncel değil: ${stale.join(", ")}. Çalıştırın: node mobile/shared/gen-vectors.mjs`);
  process.exit(1);
}
console.log(check ? "Test verisi güncel." : `Yazıldı: ${Object.keys(files).join(", ")} (${prayerCases.length} namaz, ${seasons.length} yıl, ${phases.length} ay evresi, ${moon.length} Ay örneği)`);
