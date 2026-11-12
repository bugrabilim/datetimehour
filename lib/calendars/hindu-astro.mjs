/* Hindu (ve Hint) takvimleri için gökyüzü yardımcıları (9 Kasım 2026): yıldızıl (sidereal) Güneş ve Ay boylamı, Lahiri
   ayanamsası, tithi, nakşatra, saṅkrānti (Güneş'in burca girişi), yeni ay ve gün doğumu/batımı. Yapım anında Node'da çalışır;
   hindu.mjs ve Hint ulusal takvimi (indian) kullanır. Dosya adı tire içerdiği için index.mjs'te takvim sayılmaz.

   Duyarlık (JPL Horizons DE441 ile 2025–2032'de ~2 günde bir, 1403 noktada denetlendi; ayrıntı hindu.test.json "note"):
   - Güneş: VSOP87 (Meeus, Ek III) kısaltılmış dizisi; görünür boylam farkı en çok 0,5″.
   - Ay: Meeus bölüm 47'nin tam boylam dizisi (60 terim + üç ek terim); fark en çok 10,8″. src/js/nature-calc.js'teki kısaltılmış
     dizi (32 terim, ~0,01°) tithi sınırında 1–2 dk fark verdiği için burada tam dizi kullanılır.
   - Tithi/nakşatra sınır anı ~20 sn içinde (Ay ~0,5°/sa); drikpanchang'ın Yeni Delhi günleriyle 2556 günde 3+3 fark, hepsi sınıra < 1 dk.
   - Saṅkrānti anları drikpanchang'ın yayımladığı anlardan 2–4 dk önce (ör. Makara 2027: 21:10, drikpanchang 21:14 IST); gece yarısına
     ya da gün batımına bu kadar yakın geçişte tarih ayrı düşebilir. drikpanchang'ın saṅkrānti tarihi geçişin Hindistan tarihidir;
     geçiş geceyse Makara ertesi güne, Karka önceki güne alınır (puṇya kāla).
   - Ayanamsa: Lahiri (Chitrapaksha), Swiss Ephemeris'in SE_SIDM_LAHIRI tanımı: 1956-09-22 0h TT'de 23,245524743°, ardından IAU 2006
     genel presesyonu. Yıldızıl boylam = (ortalama ekinoksa göre) boylam − ortalama ayanamsa (nütasyon iki tarafta da sadeleşir). */

const D2R = Math.PI / 180;
const UNIX_JD = 2440587.5;
const norm = (d) => ((d % 360) + 360) % 360;
const sin = (d) => Math.sin(d * D2R);
const cos = (d) => Math.cos(d * D2R);

/** ΔT = TT − UT (saniye). 2025–2032 için ~69 sn (IERS; 2020'lerde neredeyse sabit). */
export const DELTA_T = 69.2;
/** ms (UTC) → Jülyen yüzyılı (TT, J2000'den) */
export const julianCenturies = (ms) => (ms / 86400000 + UNIX_JD + DELTA_T / 86400 - 2451545) / 36525;

/* VSOP87 Dünya dizisi (Meeus, Astronomical Algorithms, Ek III, kısaltılmış): [A (1e-8 rad), B, C], terim A·cos(B + C·τ),
   τ = J2000'den Jülyen bin yılı (TT). L0..L4 (L5 ihmal). Güneş'in yer merkezli boylamı = L + 180°. */
const VSOP_L = [
  [[175347046, 0, 0], [3341656, 4.6692568, 6283.07585], [34894, 4.6261, 12566.1517], [3497, 2.7441, 5753.3849], [3418, 2.8289, 3.5231],
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
    [33, 0.59, 17789.85], [30, 0.44, 83996.85], [30, 2.74, 1349.87], [25, 3.16, 4690.48]],
  [[628331966747, 0, 0], [206059, 2.678235, 6283.07585], [4303, 2.6351, 12566.1517], [425, 1.59, 3.523], [119, 5.796, 26.298],
    [109, 2.966, 1577.344], [93, 2.59, 18849.23], [72, 1.14, 529.69], [68, 1.87, 398.15], [67, 4.41, 5507.55],
    [59, 2.89, 5223.69], [56, 2.17, 155.42], [45, 0.4, 796.3], [36, 0.47, 775.52], [29, 2.65, 7.11],
    [21, 5.34, 0.98], [19, 1.85, 5486.78], [19, 4.97, 213.3], [17, 2.99, 6275.96], [16, 0.03, 2544.31],
    [16, 1.43, 2146.17], [15, 1.21, 10977.08], [12, 2.83, 1748.02], [12, 3.26, 5088.63], [12, 5.27, 1194.45],
    [12, 2.08, 4694], [11, 0.77, 553.57], [10, 1.3, 6286.6], [10, 4.24, 1349.87], [9, 2.7, 242.73],
    [9, 5.64, 951.72], [8, 5.3, 2352.87], [6, 2.65, 9437.76], [6, 4.67, 4690.48]],
  [[52919, 0, 0], [8720, 1.0721, 6283.0758], [309, 0.867, 12566.152], [27, 0.05, 3.52], [16, 5.19, 26.3],
    [16, 3.68, 155.42], [10, 0.76, 18849.23], [9, 2.06, 77713.77], [7, 0.83, 775.52], [5, 4.66, 1577.34],
    [4, 1.03, 7.11], [4, 3.44, 5573.14], [3, 5.14, 796.3], [3, 6.05, 5507.55], [3, 1.19, 242.73],
    [3, 6.12, 529.69], [3, 0.31, 398.15], [3, 2.28, 553.57], [2, 4.38, 5223.69], [2, 3.75, 0.98]],
  [[289, 5.844, 6283.076], [35, 0, 0], [17, 5.49, 12566.15], [3, 5.2, 155.42], [1, 4.72, 3.52], [1, 5.3, 18849.23], [1, 5.97, 242.73]],
  [[114, 3.142, 0], [8, 4.13, 6283.08], [1, 3.84, 12566.15]],
];
const VSOP_R0 = [[100013989, 0, 0], [1670700, 3.0984635, 6283.07585], [13956, 3.05525, 12566.1517], [3084, 5.1985, 77713.7715],
  [1628, 1.1739, 5753.3849], [1576, 2.8469, 7860.4194], [925, 5.453, 11506.77], [542, 4.564, 3930.21], [472, 3.661, 5884.927]];
const VSOP_R1 = [[103019, 1.10749, 6283.07585], [1721, 1.0644, 12566.1517], [702, 3.142, 0]];
const series = (rows, tau) => rows.reduce((s, [a, b, c]) => s + a * Math.cos(b + c * tau), 0);

/** Güneş'in yer merkezli geometrik boylamı (ortalama ekinoks, derece) ve uzaklığı (AB) */
export function sunGeometric(ms) {
  const tau = julianCenturies(ms) / 10;
  let L = 0, p = 1;
  for (const rows of VSOP_L) { L += series(rows, tau) * p; p *= tau; }
  const R = (series(VSOP_R0, tau) + series(VSOP_R1, tau) * tau) / 1e8;
  // FK5'e dönüşüm (−0,09033″) dahil
  return { lon: norm((L / 1e8) / D2R + 180 - 0.09033 / 3600), dist: R };
}
/** Güneş'in görünür boylamı, ortalama ekinoksa göre (sapınç dahil, nütasyonsuz; derece) */
export function sunLongitude(ms) {
  const g = sunGeometric(ms);
  return norm(g.lon - 20.4898 / 3600 / g.dist);
}

/* Ay: Meeus bölüm 47, tablo 47.A'nın boylam sütunu tam (60 satır): D, M, M', F, Σl (1e-6°). M'li terimler E (|M|=2: E²) ile. */
const MOON_L = [
  [0, 0, 1, 0, 6288774], [2, 0, -1, 0, 1274027], [2, 0, 0, 0, 658314], [0, 0, 2, 0, 213618], [0, 1, 0, 0, -185116],
  [0, 0, 0, 2, -114332], [2, 0, -2, 0, 58793], [2, -1, -1, 0, 57066], [2, 0, 1, 0, 53322], [2, -1, 0, 0, 45758],
  [0, 1, -1, 0, -40923], [1, 0, 0, 0, -34720], [0, 1, 1, 0, -30383], [2, 0, 0, -2, 15327], [0, 0, 1, 2, -12528],
  [0, 0, 1, -2, 10980], [4, 0, -1, 0, 10675], [0, 0, 3, 0, 10034], [4, 0, -2, 0, 8548], [2, 1, -1, 0, -7888],
  [2, 1, 0, 0, -6766], [1, 0, -1, 0, -5163], [1, 1, 0, 0, 4987], [2, -1, 1, 0, 4036], [2, 0, 2, 0, 3994],
  [4, 0, 0, 0, 3861], [2, 0, -3, 0, 3665], [0, 1, -2, 0, -2689], [2, 0, -1, 2, -2602], [2, -1, -2, 0, 2390],
  [1, 0, 1, 0, -2348], [2, -2, 0, 0, 2236], [0, 1, 2, 0, -2120], [0, 2, 0, 0, -2069], [2, -2, -1, 0, 2048],
  [2, 0, 1, -2, -1773], [2, 0, 0, 2, -1595], [4, -1, -1, 0, 1215], [0, 0, 2, 2, -1110], [3, 0, -1, 0, -892],
  [2, 1, 1, 0, -810], [4, -1, -2, 0, 759], [0, 2, -1, 0, -713], [2, 2, -1, 0, -700], [2, 1, -2, 0, 691],
  [2, -1, 0, -2, 596], [4, 0, 1, 0, 549], [0, 0, 4, 0, 537], [4, -1, 0, 0, 520], [1, 0, -2, 0, -487],
  [2, 1, 0, -2, -399], [0, 0, 2, -2, -381], [1, 1, 1, 0, 351], [3, 0, -2, 0, -340], [4, 0, -3, 0, 330],
  [2, -1, 2, 0, 327], [0, 2, 1, 0, -323], [1, 1, -1, 0, 299], [2, 0, 3, 0, 294],
];
/** Ay'ın yer merkezli boylamı, ortalama ekinoksa göre (nütasyonsuz; derece) */
export function moonLongitude(ms) {
  const T = julianCenturies(ms), T2 = T * T, T3 = T2 * T, T4 = T3 * T;
  const Lp = 218.3164477 + 481267.88123421 * T - 0.0015786 * T2 + T3 / 538841 - T4 / 65194000;
  const D = 297.8501921 + 445267.1114034 * T - 0.0018819 * T2 + T3 / 545868 - T4 / 113065000;
  const M = 357.5291092 + 35999.0502909 * T - 0.0001536 * T2 + T3 / 24490000;
  const Mp = 134.9633964 + 477198.8675055 * T + 0.0087414 * T2 + T3 / 69699 - T4 / 14712000;
  const F = 93.2720950 + 483202.0175233 * T - 0.0036539 * T2 - T3 / 3526000 + T4 / 863310000;
  const A1 = 119.75 + 131.849 * T, A2 = 53.09 + 479264.290 * T;
  const E = 1 - 0.002516 * T - 0.0000074 * T2;
  let sl = 0;
  for (const [d, m, mp, f, l] of MOON_L) sl += l * (m === 0 ? 1 : Math.abs(m) === 2 ? E * E : E) * sin(d * D + m * M + mp * Mp + f * F);
  sl += 3958 * sin(A1) + 1962 * sin(Lp - F) + 318 * sin(A2);
  return norm(Lp + sl / 1e6);
}

/** Boylamda nütasyon (derece; IAU 1980'in dört büyük terimi, ~0,5″). Yalnız görünür boylamla karşılaştırmak için. */
export function nutationLongitude(ms) {
  const T = julianCenturies(ms);
  const Om = 125.04452 - 1934.136261 * T, L = 280.4665 + 36000.7698 * T, Lp = 218.3165 + 481267.8813 * T;
  return (-17.2 * sin(Om) - 1.32 * sin(2 * L) - 0.23 * sin(2 * Lp) + 0.21 * sin(2 * Om)) / 3600;
}

/* Lahiri ayanamsası (SE_SIDM_LAHIRI): t0 = JD 2435553,5 (1956-09-22), değer 23,245524743°; IAU 2006 genel presesyonu p_A (″). */
const pA = (T) => 5028.796195 * T + 1.1054348 * T * T + 0.00007964 * T ** 3 - 0.000023857 * T ** 4;
const LAHIRI_T0 = (2435553.5 - 2451545) / 36525;
/** Lahiri ayanamsası (derece; ortalama, nütasyonsuz) */
export function lahiriAyanamsa(ms) { return 23.245524743 + (pA(julianCenturies(ms)) - pA(LAHIRI_T0)) / 3600; }
/** Güneş'in yıldızıl (nirayana) boylamı, Lahiri (derece) */
export const siderealSun = (ms) => norm(sunLongitude(ms) - lahiriAyanamsa(ms));
/** Ay'ın yıldızıl (nirayana) boylamı, Lahiri (derece) */
export const siderealMoon = (ms) => norm(moonLongitude(ms) - lahiriAyanamsa(ms));
/** Güneş'in yıldızıl burcu: 0 = Meṣa (Koç) … 11 = Mīna (Balık) */
export const sunRashi = (ms) => Math.floor(siderealSun(ms) / 30);

/** Ay − Güneş uzanımı (derece, 0..360) */
export const elongation = (ms) => norm(moonLongitude(ms) - sunLongitude(ms));
/** Tithi (1..30): 1–15 şukla pakşa (15 = pūrṇimā), 16–30 kṛṣṇa pakşa (30 = amāvasyā) */
export const tithiAt = (ms) => Math.floor(elongation(ms) / 12) + 1;
/** Nakşatra (1..27: Aśvinī … Revatī) */
export const nakshatraAt = (ms) => Math.floor(siderealMoon(ms) / (360 / 27)) + 1;

/* f(t) − hedef açısı (−180..180) sıfır olana dek Newton benzeri yaklaşım; rate: derece/gün */
function solveAngle(f, target, guessMs, rate) {
  let t = guessMs;
  for (let i = 0; i < 50; i++) {
    const d = ((f(t) - target + 540) % 360) - 180;
    if (Math.abs(d) < 1e-7) break;
    t -= (d / rate) * 86400000;
  }
  return t;
}
/** guessMs'e en yakın anda uzanım = angle (derece; 0 = yeni ay, 180 = dolunay, 12k = tithi sınırı) */
export const elongationTime = (angle, guessMs) => solveAngle(elongation, angle, guessMs, 12.1907);
/** Tithi k'nın (1..30) bittiği an; guessMs o tithi içinde ya da yakınında olmalı */
export const tithiEnd = (k, guessMs) => elongationTime((k * 12) % 360, guessMs);
/** guessMs'e en yakın yeni ay (amāvasyā sonu) anı */
export const newMoonNear = (guessMs) => elongationTime(0, guessMs);
/** Güneş'in yıldızıl boylamı = angle olduğu, guessMs'e en yakın an (saṅkrānti: angle = 30 · burç) */
export const siderealSunTime = (angle, guessMs) => solveAngle(siderealSun, angle, guessMs, 0.9856);
/** Güneş'in yıldızıl burç rashi'ye (0 = Meṣa … 11 = Mīna) girdiği an, verilen Gregoryen yılda.
    Meṣa 13/14 Nisan, Makara 14/15 Ocak dolayı; ilk tahmin ~ 21 Mart + 30,44 gün × (burç + 0,8). */
export function sankranti(year, rashi) {
  const guess = Date.UTC(year, 2, 21) + (rashi + 0.8) * 30.44 * 86400000;
  const y0 = Date.UTC(year, 0, 1);
  let t = siderealSunTime(rashi * 30, guess > Date.UTC(year + 1, 0, 1) ? guess - 365.2422 * 86400000 : guess);
  if (t < y0) t = siderealSunTime(rashi * 30, t + 365.2564 * 86400000);
  return t;
}

/* Güneş'in ekvator koordinatları (görünür; derece) ve Greenwich ortalama yıldız zamanı (derece) */
function sunEquatorial(ms) {
  const T = julianCenturies(ms), lam = sunLongitude(ms) + nutationLongitude(ms), eps = 23.4392911 - 0.0130042 * T;
  return { ra: Math.atan2(cos(eps) * sin(lam), cos(lam)) / D2R, dec: Math.asin(sin(eps) * sin(lam)) / D2R };
}
const gmst = (ms) => { const d = ms / 86400000 + UNIX_JD - 2451545, T = d / 36525; return 280.46061837 + 360.98564736629 * d + 0.000387933 * T * T; };
/* Doğuş (rising) ya da batış: Güneş'in merkezi −0,8333°'de (üst kenar + standart kırılma), yer merkezli. Saat açısıyla
   yinelemeli çözüm (Meeus bölüm 15'teki gibi), approx'a en yakın olay; kutup günü/gecesinde null. */
function riseSet(approx, lat, lon, rising) {
  let t = approx;
  for (let i = 0; i < 8; i++) {
    const { ra, dec } = sunEquatorial(t);
    const cosH = (sin(-0.8333) - sin(lat) * sin(dec)) / (cos(lat) * cos(dec));
    if (cosH < -1 || cosH > 1) return null;
    const H0 = Math.acos(cosH) / D2R, H = gmst(t) + lon - ra;
    const dt = ((((((rising ? -H0 : H0) - H) % 360) + 540) % 360) - 180) / 360.985647 * 86400000;
    t += dt;
    if (Math.abs(dt) < 50) break;
  }
  return Math.round(t);
}
/** Yerel tarihin (YYYY-MM-DD) gün doğumu ve batımı (UTC ms). Yerel gün boylamdan bulunur (güneş öğlesi ± 6 sa'ten başlanır). */
export function sunTimes(date, lat, lon) {
  const [y, m, d] = date.split("-").map(Number);
  const noon = Date.UTC(y, m - 1, d) + (12 - lon / 15) * 3600000; // yaklaşık güneş öğlesi (UTC)
  return { rise: riseSet(noon - 6 * 3600000, lat, lon, true), set: riseSet(noon + 6 * 3600000, lat, lon, false) };
}

/* Yüklemede denetim (dizilerdeki bir yazım hatası takvimi sessizce bozmasın): Meeus örnek 47.a (1992-04-12 0h TT, Ay λ = 133,162655°)
   ve 25.b (1992-10-13 0h TT, Güneş geometrik boylamı FK5'te 199,907347°). Hata derlemeyi, gen-strings'i ve npm test'i durdurur. */
{
  const tt = (jde) => (jde - UNIX_JD) * 86400000 - DELTA_T * 1000;
  const m = moonLongitude(tt(2448724.5)), s = sunGeometric(tt(2448908.5)).lon;
  if (Math.abs(m - 133.162655) > 1e-5 || Math.abs(s - 199.907347) > 1e-5) throw new Error(`hindu-astro: Meeus örnekleri tutmuyor (Ay ${m}, Güneş ${s})`);
}
