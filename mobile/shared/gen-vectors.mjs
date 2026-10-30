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

/* Kıble: yön (derece) ve uzaklık (km) her yer için; kıble saati (yerel ondalık saat) dört tarihte */
const qiblaCases = [];
for (const pl of places) {
  const q = P.qibla(pl.lat, pl.lon);
  const qt = [[2026, 3, 20], [2026, 6, 21], [2026, 10, 30], [2026, 12, 21]].map(([y, m, d]) => num(P.qiblaTime(y, m, d, pl.lat, pl.lon, tzOffset(pl.tz, y, m, d))));
  qiblaCases.push({ id: pl.id, lat: pl.lat, lon: pl.lon, bearing: q.bearing, km: q.km, tz: [[2026, 3, 20], [2026, 6, 21], [2026, 10, 30], [2026, 12, 21]].map(([y, m, d]) => tzOffset(pl.tz, y, m, d)), time: qt });
}

/* Güneş yükseklik anları (altın ve mavi saat, 30 Ekim 2026): +6°, −4°, −6° için sabah ve akşam, dört tarihte */
const LIGHT_DATES = [[2026, 3, 20], [2026, 6, 21], [2026, 10, 30], [2026, 12, 21]];
const lightCases = [];
for (const pl of places) {
  for (const [y, m, d] of LIGHT_DATES) {
    const tz = tzOffset(pl.tz, y, m, d);
    const t = [6, -4, -6].flatMap((alt) => { const r = P.sunAltTimes(y, m, d, pl.lat, pl.lon, tz, alt); return [num(r.rise), num(r.set)]; });
    lightCases.push({ id: pl.id, y, m, d, lat: pl.lat, lon: pl.lon, tz, t });
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
// Ay'ın konumu ve doğuş/batışı (yerel gece yarısından 24 saat): kuzey/güney, yüksek enlem (doğmayan/batmayan günler)
const moonPos = [];
for (let ms = Date.UTC(2026, 0, 1, 3); ms < Date.UTC(2028, 0, 1); ms += Math.round(11.7 * DAY)) {
  const p = N.moonPos(ms);
  moonPos.push({ ms, ra: p.ra, dec: p.dec, par: p.par });
}
const moonRise = [];
for (const [lat, lon, tz] of [[41.01, 28.98, 3], [39.93, 32.86, 3], [51.51, -0.13, 0], [-33.87, 151.21, 11], [64.15, -21.94, 0], [69.65, 18.96, 1], [24.71, 46.68, 3]]) {
  for (let d = Date.UTC(2026, 9, 30); d < Date.UTC(2027, 9, 30); d += 23 * DAY) {
    const start = d - tz * 3600000;
    const r = N.moonRiseSet(start, lat, lon);
    moonRise.push({ lat, lon, start, rise: r.rise, set: r.set });
  }
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
  "qibla.json": `{
  "source": "src/js/prayer-calc.js",
  "note": "bearing: Kâbe'ye büyük daire başlangıç yönü (kuzeyden saat yönünde, derece); km: uzaklık. time: kıble saati (Güneş'in kıble yönünde olduğu an, yerel ondalık saat; null = o gün olmuyor) sırasıyla dates günlerinde, tz o günün UTC farkı.",
  "kaaba": ${JSON.stringify(P.KAABA)},
  "dates": [[2026, 3, 20], [2026, 6, 21], [2026, 10, 30], [2026, 12, 21]],
  "cases": ${lines(qiblaCases)}
}
`,
  "sun-altitude.json": `{
  "source": "src/js/prayer-calc.js (sunAltTimes)",
  "note": "t: Güneş'in +6°, −4°, −6° yüksekliğe sabah çıktığı ve akşam indiği an sırasıyla [+6 sabah, +6 akşam, −4 sabah, −4 akşam, −6 sabah, −6 akşam]; yerel ondalık saat, null = o gün oluşmuyor. tz: o gün UTC farkı.",
  "altitudes": [6, -4, -6],
  "cases": ${lines(lightCases)}
}
`,
  "nature.json": `{
  "source": "src/js/nature-calc.js",
  "note": "Zamanlar UTC milisaniye. seasons: yılın 4 mevsim anı; phases: k için yeni ay ve dolunay; moon: o andaki Ay durumu; moonPos: Ay'ın sağ açıklığı, dik açıklığı, paralaksı (derece); moonRise: start'tan 24 saat içinde ilk doğuş ve batış (null = yok).",
  "seasons": ${lines(seasons)},
  "phases": ${lines(phases)},
  "moon": ${lines(moon)},
  "moonPos": ${lines(moonPos)},
  "moonRise": ${lines(moonRise)}
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
console.log(check ? "Test verisi güncel." : `Yazıldı: ${Object.keys(files).join(", ")} (${prayerCases.length} namaz, ${qiblaCases.length} kıble, ${lightCases.length} Güneş yüksekliği, ${seasons.length} yıl, ${phases.length} ay evresi, ${moon.length} Ay örneği, ${moonRise.length} Ay doğuş/batış)`);
