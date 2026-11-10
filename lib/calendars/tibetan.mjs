/* Tibet takvimi (kapsayıcı takvimler, 10 Kasım 2026): Phugpa sistemi (Tibet, Hindistan ve Nepal'deki Tibet toplulukları,
   Dalay Lama'nın kurumu Men-Tsee-Khang'ın yıllıkları), Losar ve Budizm'in dört büyük günü (düchen) ile Ganden Ngamchö.

   Hesap Svante Janson, "Tibetan calendar mathematics" (arXiv:1401.6285, 2014) formülleridir: gerçek ay sayısı (5.10), artık
   ay kuralı (5.8: aralama sayısı 48 ya da 49), ay gününün bittiği an (7.1–7.22, "true date") ve takvim günü kuralı (bölüm 6:
   gün, o gün biten ay gününün adını alır; iki ay günü aynı gün biterse ilkinin adı, hiçbiri bitmezse ertesi günün adı).
   Karşılaştırma için açık kaynak uygulama: tibetan-date-calculator (Eszter Hoffmann, MIT, aynı makaleye dayanır).
   Losar, önceki yılın 12. (asıl) ayının 30. gününün ertesi günüdür. Çift ayda bayram birinci ayda, tekrarlanan günde ilk
   günde, atlanan günde bir önceki gündedir (feastJd).
   Aynı motor Moğol takviminde (mongolian.mjs) Yeni Genden sabitleriyle kullanılır (Janson, Ek A.3).
   Tibet yılı = Gregoryen yıl + 127 (Losar'dan itibaren). Doğrulama tibetan.test.json. */

export const meta = {
  id: "tibetan",
  group: "religious",
  regions: [],
  color: "#E94E0C", // safran turuncusu (keşiş cübbesi); diğer takvimlerden ve Türkiye günü renklerinden ΔE ≥ 21
};

const JD_UNIX = 2440588;
const isoOf = (j) => new Date((j - JD_UNIX) * 86400000).toISOString().slice(0, 10);
export const jdnOf = (s) => Date.UTC(+s.slice(0, 4), +s.slice(5, 7) - 1, +s.slice(8, 10)) / 86400000 + JD_UNIX;

// Ortalama hareketler (Phugpa ve Yeni Genden'de aynı; Janson 7.2, 7.6, 7.11)
const M1 = 167025 / 5656, M2 = M1 / 30, S1 = 65 / 804, S2 = S1 / 30, A1 = 253 / 3528, A2 = 1 / 28;
const MOON_TAB = [0, 5, 10, 15, 19, 22, 24, 25], SUN_TAB = [0, 6, 10, 11];
const frac = (x) => x - Math.floor(x);
const mod = (a, b) => ((a % b) + b) % b;
function moonTabInt(i) {
  const k = mod(i, 28);
  return k <= 7 ? MOON_TAB[k] : k <= 14 ? MOON_TAB[14 - k] : k <= 21 ? -MOON_TAB[k - 14] : -MOON_TAB[28 - k];
}
function sunTabInt(i) {
  const k = mod(i, 12);
  return k <= 3 ? SUN_TAB[k] : k <= 6 ? SUN_TAB[6 - k] : k <= 9 ? -SUN_TAB[k - 6] : -SUN_TAB[12 - k];
}
const interp = (f, x) => { const i = Math.floor(x), a = f(i); return a + (f(i + 1) - a) * frac(x); };

/** Sürüm sabitleri: epoch yılı Y0 (ay 3), β*, artık ay eşiği (Phugpa 48, Yeni Genden 46) ve epoch değerleri m0, s0, a0 */
export const PHUGPA = { Y0: 806, beta: 61, ix: 48, m0: 2015501 + 4783 / 5656, s0: 743 / 804, a0: 475 / 3528 };
export const NEW_GENDEN = { Y0: 1747, beta: 10, ix: 46, m0: 2359237 + 2603 / 2828, s0: 397 / 402, a0: 1523 / 1764 };

/** Ay sayısı: Tibet yılı yerine Gregoryen yıl Y (Losar'ın yılı) ve ay M (1–12); artık ay için leap = true */
export function monthCount(V, Y, M, leap = false) {
  const ms = 12 * (Y - V.Y0) + M - 3;
  const ix = mod(2 * ms + V.beta, 65);
  const hasLeap = ix === V.ix || ix === V.ix + 1;
  return Math.floor((67 * ms + V.beta + 65 - V.ix) / 65) - (hasLeap && leap ? 1 : 0);
}
export const hasLeapMonth = (V, Y, M) => { const ix = mod(2 * (12 * (Y - V.Y0) + M - 3) + V.beta, 65); return ix === V.ix || ix === V.ix + 1; };

/** Ay günü d'nin (1–30) n. ayda bittiği an (JD, gerçek sayı) */
export function trueDate(V, d, n) {
  const meanDate = n * M1 + d * M2 + V.m0;
  const anomMoon = n * A1 + d * A2 + V.a0;
  const meanSun = n * S1 + d * S2 + V.s0;
  return meanDate + interp(moonTabInt, 28 * anomMoon) / 60 - interp(sunTabInt, 12 * (meanSun - 0.25)) / 60;
}
/** (8.1): ay gününün adını verdiği takvim gününün JDN'si (tekrarlanan günde ikincisi) */
export const dayJd = (V, d, n) => Math.floor(trueDate(V, d, n));
/** Yılın ilk günü (Losar / Tsagaan Sar): önceki yılın 12. asıl ayının 30. gününün ertesi günü */
export const newYearJd = (V, Y) => dayJd(V, 30, monthCount(V, Y - 1, 12)) + 1;

/** Gregoryen yıllar a..b için takvim günleri: JDN → { Y, M, leap, d } */
export function dayLabels(V, a, b) {
  const months = [];
  for (let Y = a - 1; Y <= b + 1; Y++)
    for (let M = 1; M <= 12; M++) {
      if (hasLeapMonth(V, Y, M)) months.push({ Y, M, leap: true, n: monthCount(V, Y, M, true) });
      months.push({ Y, M, leap: false, n: monthCount(V, Y, M) });
    }
  const named = new Map();
  for (const m of months)
    for (let d = 1; d <= 30; d++) {
      const j = dayJd(V, d, m.n);
      if (!named.has(j)) named.set(j, { Y: m.Y, M: m.M, leap: m.leap, d }); // iki ay günü aynı gün biterse ilki
    }
  const out = new Map();
  const first = jdnOf(`${a}-01-01`), last = jdnOf(`${b}-12-31`);
  for (let j = first; j <= last; j++) {
    let k = j;
    while (!named.has(k)) k++; // hiçbir ay günü bitmiyorsa ertesi günün adı (tekrarlanan gün)
    out.set(j, named.get(k));
  }
  return out;
}

/** Bayram: Y yılının M. ayının d. günü. Çift ayda birinci ayda (Men-Tsee-Khang yıllıklarına göre tutulan günler: Lhabab
    Düchen 2021 = 27 Ekim, Chokhor Düchen 2024 = 9 Temmuz); tekrarlanan günde ilk gün, atlanan günde bir önceki gün (Janson
    bölüm 11). */
export function feastJd(V, Y, M, d, labels = dayLabels(V, Y - 1, Y + 1)) {
  const leap = hasLeapMonth(V, Y, M);
  for (const [j, x] of labels) if (x.Y === Y && x.M === M && x.leap === leap && x.d >= d) return x.d === d ? j : j - 1;
  throw new Error(`tibetan: ${Y}/${M}/${d} bulunamadı`);
}
const feast = (V, Y, M, d, L) => isoOf(feastJd(V, Y, M, d, L));

export function events(year) {
  const V = PHUGPA, out = [], L = dayLabels(V, year - 1, year + 1);
  for (const Y of [year - 1, year]) {
    out.push(
      { key: "losar", date: isoOf(newYearJd(V, Y)) },
      { key: "chotrul", date: feast(V, Y, 1, 15, L) },
      { key: "sagaDawa", date: feast(V, Y, 4, 15, L) },
      { key: "chokhor", date: feast(V, Y, 6, 4, L) },
      { key: "lhabab", date: feast(V, Y, 9, 22, L) },
      { key: "ganden", date: feast(V, Y, 10, 25, L) },
    );
  }
  return out.filter((e) => e.date.startsWith(String(year)));
}

/** Günlük kod: ay (m1–m12, artık ay l1–l12), gün (d1–d30), Tibet yılı (y2152) */
export function daily(year) {
  const L = dayLabels(PHUGPA, year, year), first = jdnOf(`${year}-01-01`);
  return Array.from({ length: L.size }, (_, i) => {
    const x = L.get(first + i);
    return `${x.leap ? "l" : "m"}${x.M}d${x.d}y${x.Y + 127}`;
  });
}
