/* Çin ay-güneş takvimi (kapsayıcı takvimler, 9 Kasım 2026): Çin'in geleneksel takvimi (Çin saatiyle), 24 güneş dönemi ve Çin
   burcu yılı. Kore (korean.mjs, UTC+9) ve Vietnam (vietnamese.mjs, UTC+7) ay takvimleri aynı kuralı bu dosyadaki yardımcılarla
   kendi saatleriyle hesaplayan ayrı takvimlerdir (bazı aylar bir gün farklı başlar).
   Kural (Çin'in 1929'dan beri kullandığı astronomik kural, GB/T 33661-2017):
     - Ay, yeni ayın (Ay–Güneş kavuşumu) düştüğü yerel günde başlar; Çin için UTC+8, Kore için UTC+9, Vietnam için UTC+7.
     - Kış gündönümünü (Güneş boylamı 270°) içeren ay 11. aydır. İki 11. ay arasında 13 ay varsa ilk "orta dönem"siz ay
       (Güneş boylamı 30°'nin katı olan bir an içermeyen ay) artık aydır ve önceki ayın numarasını alır.
     - 24 güneş dönemi: Güneş'in görünür boylamının 15°'nin katlarından geçtiği an; gün yerel saatle.
   Astronomi: yeni aylar src/js/nature-calc.js'teki Meeus bölüm 49 (phaseMs) + bu dosyada bölümün kalan küçük terimleri ve
   gezegen düzeltmeleri (A1–A14; nature-calc'a dokunulmadı, uygulamaların test vektörleri ona bağlı); Güneş boylamı VSOP87'nin
   Meeus Ek III kısaltması (FK5, nütasyon, sapınç), birkaç saniye duyarlıkla. nature-calc'taki sunPos düşük duyarlıklı (0,01°,
   ~15 dk) ve yalnız sağ açıklık/dik açıklık verdiği için gece yarısına yakın dönemlerde gün kayabilirdi.
   Doğrulama: chinese.test.json (Hong Kong Gözlemevi tabloları); Kore ve Vietnam korean.test.json ve vietnamese.test.json.
   Japon takvimi (rokuyō) bu dosyadaki yardımcıları kullanır: lunisolarMonths(from, to, 9), lunarDate(tarih, tablo),
   fromLunar(y, m, d, tablo), solarTerms(yıl), localDay(ms, off), sunLongitude(ms), sunLongitudeMs(boylam, yakın an), newMoonMs(k),
   newMoonK(ms), zodiacOf(yıl), sexagenaryOf(yıl). UTC+9 tablosu KASI'nin 2024–2033 ay başlarıyla birebir (Kore ile Japonya aynı
   saat dilimi); Japon kyūreki'si kuralı Çin kuralından yalnız seyrek durumlarda ayrılır (ör. 2033 sorunu, aralığın dışında). */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { addDays } from "../events.mjs";

export const meta = {
  id: "chinese",
  group: "cultural", // yalnız bölgesinde varsayılan açık
  regions: ["CN", "HK", "MO", "TW", "SG"],
  color: "#C82A64", // kızıl; resmî tatil kırmızısından (#D32F2F) ayırt edilsin diye pembeye çekik
};

const HERE = path.dirname(fileURLToPath(import.meta.url));
const N = (() => { const m = { exports: {} }; new Function("module", "window", fs.readFileSync(path.join(HERE, "../../src/js/nature-calc.js"), "utf8"))(m, undefined); return m.exports; })();

const DAY = 86400000, HOUR = 3600000;
const D2R = Math.PI / 180;
const UNIX_JD = 2440587.5;
const DELTA_T = 69; // saniye (nature-calc ile aynı; 2025–2032 için gerçek değer ~69–71 sn)
const sinD = (d) => Math.sin(d * D2R);
const norm360 = (x) => ((x % 360) + 360) % 360;

/* ---------- Yeni ay ---------- */

/** k (2000 Ocak'tan bu yana kavuşum sırası) için yeni ay anı, UTC ms. nature-calc phaseMs + Meeus 49'un kalan terimleri ve A1–A14. */
export function newMoonMs(k) {
  const T = k / 1236.85;
  const M = 2.5534 + 29.1053567 * k - 0.0000014 * T * T - 0.00000011 * T * T * T;
  const Mp = 201.5643 + 385.81693528 * k + 0.0107582 * T * T + 0.00001238 * T * T * T - 0.000000058 * T * T * T * T;
  const F = 160.7108 + 390.67050284 * k - 0.0016118 * T * T - 0.00000227 * T * T * T + 0.000000011 * T * T * T * T;
  // nature-calc'ta olmayan küçük terimler (gün)
  let c = -0.00007 * sinD(Mp + 2 * M) + 0.00004 * sinD(2 * Mp - 2 * F) + 0.00004 * sinD(3 * M) + 0.00003 * sinD(Mp + M - 2 * F)
    + 0.00003 * sinD(2 * Mp + 2 * F) - 0.00003 * sinD(Mp + M + 2 * F) + 0.00003 * sinD(Mp - M + 2 * F) - 0.00002 * sinD(Mp - M - 2 * F)
    - 0.00002 * sinD(3 * Mp + M) + 0.00002 * sinD(4 * Mp);
  // Gezegen düzeltmeleri A1–A14
  const A = [
    [299.77 + 0.107408 * k - 0.009173 * T * T, 0.000325], [251.88 + 0.016321 * k, 0.000165], [251.83 + 26.651886 * k, 0.000164],
    [349.42 + 36.412478 * k, 0.000126], [84.66 + 18.206239 * k, 0.00011], [141.74 + 53.303771 * k, 0.000062],
    [207.14 + 2.453732 * k, 0.00006], [154.84 + 7.30686 * k, 0.000056], [34.52 + 27.261239 * k, 0.000047],
    [207.19 + 0.121824 * k, 0.000042], [291.34 + 1.844379 * k, 0.00004], [161.72 + 24.198154 * k, 0.000037],
    [239.56 + 25.513099 * k, 0.000035], [331.55 + 3.592518 * k, 0.000023],
  ];
  for (const [a, w] of A) c += w * sinD(a);
  return N.phaseMs(k, false) + Math.round(c * DAY);
}
/** ms anından önceki (ya da o andaki) yeni ayın k sırası */
export function newMoonK(ms) {
  let k = Math.floor((ms / DAY + UNIX_JD - 2451550.09766) / 29.530588861) + 1;
  while (newMoonMs(k) > ms) k--;
  return k;
}

/* ---------- Güneş boylamı (VSOP87, Meeus Ek III kısaltması) ---------- */

// [A, B, C]: A·cos(B + C·τ), τ = Jülyen bin yıl (TT, J2000'den); A 1e-8 radyan
const L0 = [[175347046, 0, 0], [3341656, 4.6692568, 6283.07585], [34894, 4.6261, 12566.1517], [3497, 2.7441, 5753.3849], [3418, 2.8289, 3.5231],
  [3136, 3.6277, 77713.7715], [2676, 4.4181, 7860.4194], [2343, 6.1352, 3930.2097], [1324, 0.7425, 11506.7698], [1273, 2.0371, 529.691],
  [1199, 1.1096, 1577.3435], [990, 5.233, 5884.927], [902, 2.045, 26.298], [857, 3.508, 398.149], [780, 1.179, 5223.694],
  [753, 2.533, 5507.553], [505, 4.583, 18849.228], [492, 4.205, 775.523], [357, 2.92, 0.067], [317, 5.849, 11790.629],
  [284, 1.899, 796.298], [271, 0.315, 10977.079], [243, 0.345, 5486.778], [206, 4.806, 2544.314], [205, 1.869, 5573.143],
  [202, 2.458, 6069.777], [156, 0.833, 213.299], [132, 3.411, 2942.463], [126, 1.083, 20.775], [115, 0.645, 0.98],
  [103, 0.636, 4694.003], [102, 0.976, 15720.839], [102, 4.267, 7.114], [99, 6.21, 2146.17], [98, 0.68, 155.42],
  [86, 5.98, 161000.69], [85, 1.3, 6275.96], [85, 3.67, 71430.7], [80, 1.81, 17260.15], [79, 3.04, 12036.46],
  [75, 1.76, 5088.63], [74, 3.5, 3154.69], [74, 4.68, 801.82], [70, 0.83, 9437.76], [62, 3.98, 8827.39],
  [61, 1.82, 7084.9], [57, 2.78, 6286.6], [56, 4.39, 14143.5], [56, 3.47, 6279.55], [52, 0.19, 12139.55],
  [52, 1.33, 1748.02], [51, 0.28, 5856.48], [49, 0.49, 1194.45], [41, 5.37, 8429.24], [41, 2.4, 19651.05],
  [39, 6.17, 10447.39], [37, 6.04, 10213.29], [37, 2.57, 1059.38], [36, 1.71, 2352.87], [36, 1.78, 6812.77],
  [33, 0.59, 17789.85], [30, 0.44, 83996.85], [30, 2.74, 1349.87], [25, 3.16, 4690.48]];
const L1 = [[628331966747, 0, 0], [206059, 2.678235, 6283.07585], [4303, 2.6351, 12566.1517], [425, 1.59, 3.523], [119, 5.796, 26.298],
  [109, 2.966, 1577.344], [93, 2.59, 18849.23], [72, 1.14, 529.69], [68, 1.87, 398.15], [67, 4.41, 5507.55],
  [59, 2.89, 5223.69], [56, 2.17, 155.42], [45, 0.4, 796.3], [36, 0.47, 775.52], [29, 2.65, 7.11],
  [21, 5.34, 0.98], [19, 1.85, 5486.78], [19, 4.97, 213.3], [17, 2.99, 6275.96], [16, 0.03, 2544.31],
  [16, 1.43, 2146.17], [15, 1.21, 10977.08], [12, 2.83, 1748.02], [12, 3.26, 5088.63], [12, 5.27, 1194.45],
  [12, 2.08, 4694], [11, 0.77, 553.57], [10, 1.3, 6286.6], [10, 4.24, 1349.87], [9, 2.7, 242.73],
  [9, 5.64, 951.72], [8, 5.3, 2352.87], [6, 2.65, 9437.76], [6, 4.67, 4690.48]];
const L2 = [[52919, 0, 0], [8720, 1.0721, 6283.0758], [309, 0.867, 12566.152], [27, 0.05, 3.52], [16, 5.19, 26.3], [16, 3.68, 155.42],
  [10, 0.76, 18849.23], [9, 2.06, 77713.77], [7, 0.83, 775.52], [5, 4.66, 1577.34], [4, 1.03, 7.11], [4, 3.44, 5573.14],
  [3, 5.14, 796.3], [3, 6.05, 5507.55], [3, 1.19, 242.73], [3, 6.12, 529.69], [3, 0.31, 398.15], [3, 2.28, 553.57],
  [2, 4.38, 5223.69], [2, 3.75, 0.98]];
const L3 = [[289, 5.844, 6283.076], [35, 0, 0], [17, 5.49, 12566.15], [3, 5.2, 155.42], [1, 4.72, 3.52], [1, 5.3, 18849.23], [1, 5.97, 242.73]];
const L4 = [[114, 3.142, 0], [8, 4.13, 6283.08], [1, 3.84, 12566.15]];
const L5 = [[1, 3.14, 0]];
const R0 = [[100013989, 0, 0], [1670700, 3.0984635, 6283.07585], [13956, 3.05525, 12566.1517], [3084, 5.1985, 77713.7715], [1628, 1.1739, 5753.3849],
  [1576, 2.8469, 7860.4194], [925, 5.453, 11506.77], [542, 4.564, 3930.21], [472, 3.661, 5884.927], [346, 0.964, 5507.553]];
const R1 = [[103019, 1.10749, 6283.07585], [1721, 1.0644, 12566.1517]];
const series = (S, t) => S.reduce((s, [a, b, c]) => s + a * Math.cos(b + c * t), 0);

/** Güneş'in yer merkezli görünür ekliptik boylamı (tarihin ekinoksuna göre, derece) */
export function sunLongitude(ms) {
  const jde = ms / DAY + UNIX_JD + DELTA_T / 86400;
  const tau = (jde - 2451545) / 365250, T = tau * 10;
  const L = (series(L0, tau) + series(L1, tau) * tau + series(L2, tau) * tau * tau + series(L3, tau) * tau ** 3 + series(L4, tau) * tau ** 4 + series(L5, tau) * tau ** 5) / 1e8;
  const R = (series(R0, tau) + series(R1, tau) * tau) / 1e8;
  let lon = L / D2R + 180; // yer merkezli geometrik boylam (VSOP87 ekinoksu)
  lon += -0.09033 / 3600; // FK5'e dönüşüm (boylamda yalnız sabit terim)
  // Nütasyon (Meeus 22, ana terimler) ve yıllık sapınç
  const Om = 125.04452 - 1934.136261 * T, Ls = 280.4665 + 36000.7698 * T, Lm = 218.3165 + 481267.8813 * T;
  const dpsi = (-17.2 * sinD(Om) - 1.32 * sinD(2 * Ls) - 0.23 * sinD(2 * Lm) + 0.21 * sinD(2 * Om)) / 3600;
  return norm360(lon + dpsi - 20.4898 / 3600 / R);
}

/** Güneş'in görünür boylamının `lon` (derece) değerinden geçtiği an (UTC ms), `nearMs` anına en yakın geçiş */
export function sunLongitudeMs(lon, nearMs) {
  let t = nearMs;
  for (let i = 0; i < 12; i++) {
    const d = ((lon - sunLongitude(t) + 540) % 360) - 180;
    t += (d * 365.2422 / 360) * DAY;
    if (Math.abs(d) < 1e-7) break;
  }
  return Math.round(t);
}

/** Güneş dönemleri: n = 1 (Lichun, 315°) … 24 (Dahan, 300°); boylam = (315 + 15·(n − 1)) mod 360 */
export const termLongitude = (n) => (315 + 15 * (n - 1)) % 360;
/** Gregoryen yılda 24 güneş dönemi: [{ n, lon, ms }] (zamana göre sıralı; ms UTC) */
export function solarTerms(year) {
  const jan1 = Date.UTC(year, 0, 1), out = [];
  for (let n = 1; n <= 24; n++) {
    const lon = termLongitude(n);
    // 1 Ocak'ta Güneş ~280°; boylam farkı kadar ilerlenmiş tahminden başla
    const ms = sunLongitudeMs(lon, jan1 + (norm360(lon - 280) / 360) * 365.2422 * DAY);
    out.push({ n, lon, ms });
  }
  return out.sort((a, b) => a.ms - b.ms);
}

/* ---------- Ay-güneş ayları ---------- */

/** UTC ms → verilen saat dilimindeki (UTC + off saat) gün, "YYYY-MM-DD" */
export const localDay = (ms, off) => new Date(ms + off * HOUR).toISOString().slice(0, 10);

/** Ay başları: [ay başı (yerel gün), ay-yılı (o ay-yılının 1. ayının başladığı Gregoryen yıl), ay (1–12), 0 | 1 (artık ay)].
    fromYear-01-01'i içeren aydan başlar; son satır toYear-12-31'den sonra başlayan ilk aydır (takvim sözleşmesindeki bitiş
    sınırı). off: saat dilimi (Çin 8, Kore 9, Japonya 9, Vietnam 7). */
export function lunisolarMonths(fromYear, toYear, off = 8) {
  const rows = [];
  // Kış gündönümleri (yerel gün) fromYear − 2 … toYear + 1
  const ws = [];
  for (let y = fromYear - 2; y <= toYear + 1; y++) ws.push(sunLongitudeMs(270, Date.UTC(y, 11, 21)));
  for (let i = 0; i + 1 < ws.length; i++) {
    const wsA = localDay(ws[i], off), wsB = localDay(ws[i + 1], off);
    // Gündönümünü içeren ay: başlangıcı gündönümü gününe eşit ya da önce olan son yeni ay
    let k = newMoonK(ws[i] + DAY);
    while (localDay(newMoonMs(k), off) > wsA) k--;
    const starts = [];
    for (let kk = k; ; kk++) {
      const d = localDay(newMoonMs(kk), off);
      starts.push(d);
      if (d > wsB) break;
    }
    // starts[0] = 11. ay; sonraki 11. ay = wsB gününü içeren ay = sondan bir önceki
    const n = starts.length - 2; // iki 11. ay arasındaki ay sayısı
    let leapIdx = -1;
    if (n === 13) {
      // Orta dönemler (30°'nin katları) bu aralıkta: yerel günleri
      const zq = [];
      for (let y = +wsA.slice(0, 4); y <= +wsB.slice(0, 4); y++) for (const t of solarTerms(y)) if (t.lon % 30 === 0) zq.push(localDay(t.ms, off));
      for (let j = 1; j < n; j++) {
        if (!zq.some((d) => d >= starts[j] && d < starts[j + 1])) { leapIdx = j; break; }
      }
    }
    let m = 11;
    for (let j = 0; j < n; j++) {
      if (j > 0 && j !== leapIdx) m = (m % 12) + 1;
      rows.push([starts[j], 0, m, j === leapIdx ? 1 : 0]);
    }
  }
  // Ay-yılı: 1. ayın (artık olmayan) başladığı Gregoryen yıl
  const firstNy = rows.findIndex((r) => r[2] === 1 && !r[3]);
  let year = +rows[firstNy][0].slice(0, 4) - 1;
  for (const r of rows) { if (r[2] === 1 && !r[3]) year = +r[0].slice(0, 4); r[1] = year; }
  // Aralığa kırp: fromYear-01-01'i içeren aydan toYear-12-31'den sonraki ilk aya
  const a = rows.findIndex((r) => r[0] > `${fromYear}-01-01`) - 1, b = rows.findIndex((r) => r[0] > `${toYear}-12-31`);
  return rows.slice(Math.max(0, a), b + 1);
}

/** Tarih → ay-güneş tarihi { y, m, d, leap } (tablo: lunisolarMonths çıktısı; tablonun dışında null) */
export function lunarDate(date, table) {
  if (!table.length || date < table[0][0] || date >= table[table.length - 1][0]) return null;
  let i = 0;
  while (table[i + 1][0] <= date) i++;
  const r = table[i];
  return { y: r[1], m: r[2], d: Math.round((Date.parse(date) - Date.parse(r[0])) / DAY) + 1, leap: !!r[3] };
}
/** Ay-güneş tarihi → Gregoryen gün (artık olmayan ay; tabloda yoksa null) */
export function fromLunar(y, m, d, table, leap = false) {
  const r = table.find((x) => x[1] === y && x[2] === m && !!x[3] === leap);
  return r ? addDays(r[0], d - 1) : null;
}

/** Burç yılı sırası: 1 Fare … 12 Domuz (ay-yılı için; 2020 Fare) */
export const zodiacOf = (lunarYear) => ((((lunarYear - 4) % 12) + 12) % 12) + 1;
/** Altmışlı döngü: { stem: 1–10 (甲…癸), branch: 1–12 (子…亥) } */
export const sexagenaryOf = (lunarYear) => ({ stem: ((((lunarYear - 4) % 10) + 10) % 10) + 1, branch: zodiacOf(lunarYear) });

/* ---------- Takvim sözleşmesi ---------- */

const cache = new Map();
const table = (year, off) => {
  const key = `${year}:${off}`;
  if (!cache.has(key)) cache.set(key, lunisolarMonths(year - 1, year + 1, off));
  return cache.get(key);
};

export function events(year) {
  const cn = table(year, 8);
  const list = [];
  const add = (key, date, end) => { if (date && date.startsWith(String(year))) list.push(end ? { date, key, end } : { date, key }); };
  const ny = fromLunar(year, 1, 1, cn);
  add(`ny${zodiacOf(year)}`, ny);
  add("nyEve", addDays(ny, -1));
  add("lantern", fromLunar(year, 1, 15, cn));
  add("dragonBoat", fromLunar(year, 5, 5, cn));
  add("qixi", fromLunar(year, 7, 7, cn));
  add("midAutumn", fromLunar(year, 8, 15, cn));
  add("doubleNinth", fromLunar(year, 9, 9, cn));
  for (const t of solarTerms(year)) {
    if (t.lon === 15) add("qingming", localDay(t.ms, 8));
    if (t.lon === 270) add("dongzhi", localDay(t.ms, 8));
  }
  // Kore (Seollal, Chuseok…) ve Vietnam (Tết, Trung Thu…) bayramları kendi takvimlerinde: korean.mjs, vietnamese.mjs
  return list;
}

/** İkinci tarih: Çin saatiyle (UTC+8) ay tablosu */
export function months(fromYear, toYear) {
  return lunisolarMonths(fromYear, toYear, 8);
}

/** Güneş dönemleri: dönemin başladığı gün (Çin saati) → "s1" (Lichun) … "s24" (Dahan) */
export function daily(year) {
  const out = {};
  for (const t of solarTerms(year)) out[localDay(t.ms, 8)] = `s${t.n}`;
  return out;
}
