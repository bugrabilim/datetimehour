/* Hindu takvimi (pañcāṅga; kapsayıcı takvimler, 9 Kasım 2026). Referans yer Yeni Delhi (28,63576° K, 77,22445° D; Hindistan
   saati UTC+5:30); bütün değerler o yerin gün doğumuna göre, drikpanchang.com'un Yeni Delhi sonuçlarıyla aynı kurallarla.
   - Günlük kod (gün doğumundaki değerler): ay (amānta: ay yeni aydan yeni aya; `m1` Caitra … `m12` Phālguna, artık ay `a1`…`a12`),
     tithi (`t1`…`t15` şukla pakşa, `t15` pūrṇimā; `t16`…`t30` kṛṣṇa pakşa, `t30` amāvasyā), nakşatra (`n1` Aśvinī … `n27` Revatī,
     Lahiri ayanamsası) ve Vikram Samvat yılı (`v2083`: Caitra'nın ilk günüyle artar; artık Caitra varsa onunla). Örnek "m10t7n27v2083".
   - Ay adı: iki yeni ay arasındaki ay, yeni ayın başındaki yıldızıl Güneş burcuna göre adlanır (Güneş Mīna'dayken başlayan ay Caitra);
     iki yeni ay arasında Güneş burç değiştirmezse ay artıktır (adhika) ve sonraki ayın adını alır. Kṣaya ay 2025–2032'de yok.
   - Olaylar: drikpanchang'ın Yeni Delhi kurallarıyla (gün doğumu tithisi ya da tithinin günün belli bölümünde [madhyāhna, aparāhṇa,
     pradoṣa, niśītha] sürmesi; Holika Dahan ve Raksha Bandhan'da Bhadrā kuralı). Janmashtami Vaishnava (udaya tithi) tarihidir.
   Hesap: hindu-astro.mjs. Doğrulama: hindu.test.json. */
import {
  elongationTime, newMoonNear, sankranti, sunRashi, sunTimes, tithiAt, nakshatraAt,
} from "./hindu-astro.mjs";

export * from "./hindu-astro.mjs";

export const meta = {
  id: "hindu",
  group: "religious", // varsayılan her yerde kapalı (kimsenin inancı tahmin edilmez)
  regions: [],
  color: "#D63A9A", // rani pembesi (gulal); safran tonları Etiyopya, Budist ve Bahai renklerine çok yakın (ΔE ≥ 20 kuralı)
};

/** Referans yer: Yeni Delhi (GeoNames 1261481) ve Hindistan saati */
export const NEW_DELHI = { lat: 28.63576, lon: 77.22445, tz: 5.5 };
const { lat: LAT, lon: LON, tz: TZ } = NEW_DELHI;

const DAY = 86400000;
const isoOf = (ms) => new Date(ms).toISOString().slice(0, 10);
/** UTC anının Hindistan saatine göre tarihi */
export const istDate = (ms) => isoOf(ms + TZ * 3600000);
const addDay = (d, n) => isoOf(Date.parse(d) + n * DAY);

/* Gün doğumu/batımı önbelleği (Yeni Delhi) */
const sunCache = new Map();
/** Yeni Delhi'de tarihin gün doğumu ve batımı (ms) */
export function delhiSun(date) {
  let s = sunCache.get(date);
  if (!s) { s = sunTimes(date, LAT, LON); sunCache.set(date, s); }
  return s;
}

/* Kameri aylar (amānta): { s: yeni ay, e: sonraki yeni ay, m: 0 Caitra … 11 Phālguna, adhika } */
let LUN = [];
/** startMs ile endMs arasındaki kameri aylar (önbellekli; ΔT sabit olduğu için 2020–2040 dışında duyarlık azalır) */
export function lunations(startMs, endMs) {
  if (!LUN.length || LUN[0].s > startMs - 40 * DAY || LUN[LUN.length - 1].e < endMs + 40 * DAY) {
    const from = Math.min(startMs, LUN.length ? LUN[0].s : startMs) - 60 * DAY;
    const to = Math.max(endMs, LUN.length ? LUN[LUN.length - 1].e : endMs) + 60 * DAY;
    const nm = [];
    for (let t = newMoonNear(from); t < to + 30 * DAY; t = newMoonNear(t + 29.53 * DAY)) nm.push(t);
    LUN = nm.slice(0, -1).map((s, i) => {
      const rs = sunRashi(s), re = sunRashi(nm[i + 1]);
      if ((re - rs + 12) % 12 > 1) throw new Error(`hindu: kṣaya ay (${isoOf(s)}); sözleşme desteklemiyor`);
      return { s, e: nm[i + 1], m: (rs + 1) % 12, adhika: rs === re };
    });
  }
  return LUN.filter((l) => l.e > startMs && l.s < endMs);
}
const lunationAt = (ms) => lunations(ms - 40 * DAY, ms + 40 * DAY).find((l) => l.s <= ms && ms < l.e);
/** Gregoryen yıl y'deki (ortası o yılda olan) artık olmayan m ayı (0 Caitra … 11 Phālguna) */
export function lunarMonth(y, m) {
  return lunations(Date.UTC(y - 1, 10, 1), Date.UTC(y + 1, 2, 1)).find((l) => l.m === m && !l.adhika && new Date(l.s + 15 * DAY).getUTCFullYear() === y) || null;
}
/** Kameri ayın k. tithisinin (1..30) başlangıç ve bitiş anı */
export function tithiSpan(l, k) {
  const g = (a) => l.s + (a / 12.19) * DAY;
  return [k === 1 ? l.s : elongationTime((k - 1) * 12, g((k - 1) * 12)), k === 30 ? l.e : elongationTime(k * 12, g(k * 12))];
}

/* Günün bölümleri (Yeni Delhi): gündüz beşe bölünür (prātaḥ, saṅgava, madhyāhna, aparāhṇa, sāyāhna); gece on beş muhūrta. */
const KAAL = {
  udaya: (d) => { const r = delhiSun(d).rise; return [r, r + 1]; },
  purvahna: (d) => { const { rise, set } = delhiSun(d); return [rise, (rise + set) / 2]; },
  madhyahna: (d) => { const { rise, set } = delhiSun(d), D = set - rise; return [rise + 0.4 * D, rise + 0.6 * D]; },
  aparahna: (d) => { const { rise, set } = delhiSun(d), D = set - rise; return [rise + 0.6 * D, rise + 0.8 * D]; },
  pradosh: (d) => { const s = delhiSun(d).set, n = delhiSun(addDay(d, 1)).rise - s; return [s, s + n / 5]; },
  nishita: (d) => { const s = delhiSun(d).set, n = delhiSun(addDay(d, 1)).rise - s; return [s + (7 * n) / 15, s + (8 * n) / 15]; },
};
const overlap = (a, b) => Math.max(0, Math.min(a[1], b[1]) - Math.max(a[0], b[0]));
/** Tithinin değdiği günler ve her günün belirtilen bölümüyle örtüşme oranı */
function touch(span, kaal) {
  const out = [];
  for (let d = addDay(istDate(span[0]), -1); d <= istDate(span[1]); d = addDay(d, 1)) {
    const w = KAAL[kaal](d), ov = overlap(span, w);
    if (ov > 0) out.push({ d, ov: ov / (w[1] - w[0]) });
  }
  return out;
}
/** Udaya kuralı: tithinin gün doğumunda olduğu ilk gün; hiçbir gün doğumuna değmiyorsa (kṣaya tithi) başladığı gün */
function udaya(span) {
  const c = touch(span, "udaya");
  return c.length ? c[0].d : istDate(span[0]);
}
/** Vyāpinī kuralı: tithinin günün kaal bölümünde sürdüğü gün; iki günse prefer ("first" | "second" | "max"); hiçbiri değilse fallback */
function vyapini(span, kaal, prefer, fallback = udaya) {
  const c = touch(span, kaal);
  if (!c.length) return fallback(span);
  if (c.length === 1 || prefer === "first") return c[0].d;
  if (prefer === "second") return c[c.length - 1].d;
  return c.reduce((a, b) => (b.ov > a.ov ? b : a)).d;
}

/* Bayram kuralları. Ay: 0 Caitra … 11 Phālguna (amānta); tithi 1..30. */
function holikaDahan(y) {
  const l = lunarMonth(y, 11); if (!l) return null;
  const P = tithiSpan(l, 15), bhadraEnd = (P[0] + P[1]) / 2; // Bhadrā (Viṣṭi karaṇa) pūrṇimānın ilk yarısıdır
  const c = touch(P, "pradosh");
  if (!c.length) return udaya(P);
  const d1 = c[0].d;
  if (c.length > 1) return c[1].d; // iki günün pradoşunda da pūrṇimā: ikinci gün (Bhadrā'sız)
  // Bhadrā gece yarısından önce bitiyorsa ilk gün (Bhadrā bitince)
  const s1 = delhiSun(d1).set, r2 = delhiSun(addDay(d1, 1)).rise;
  if (bhadraEnd <= (s1 + r2) / 2) return d1;
  // Bhadrā gece yarısını aşıyor: ertesi gün pūrṇimā gündüzün 3,5 praharını (7/8) kaplıyor ve pratipadā uzuyorsa ertesi günün pradoşu
  const d2 = addDay(d1, 1), { rise, set } = delhiSun(d2);
  const pratipada = tithiSpan(l, 16); // kṛṣṇa pratipadā
  if (P[1] - rise >= (7 / 8) * (set - rise) && pratipada[1] - pratipada[0] >= P[1] - P[0]) return d2;
  return d1;
}
function rakshaBandhan(y) {
  const l = lunarMonth(y, 4); if (!l) return null;
  const P = tithiSpan(l, 15), bhadraEnd = (P[0] + P[1]) / 2;
  const c = touch(P, "aparahna");
  if (!c.length) return udaya(P);
  const d1 = c[0].d, w = KAAL.aparahna(d1);
  if (overlap([bhadraEnd, P[1]], w) > 0) return d1; // aparāhṇada Bhadrā'sız pūrṇimā
  const d2 = addDay(d1, 1);
  return P[1] - delhiSun(d2).rise >= 144 * 60000 ? d2 : d1; // ertesi gün gün doğumundan en az 6 ghaṭī pūrṇimā
}
function diwali(y) {
  const l = lunarMonth(y, 6); if (!l) return null;
  const A = tithiSpan(l, 30), c = touch(A, "pradosh");
  if (!c.length) return udaya(A);
  // iki günün pradoşunda da amāvasyā: ikinci gün yalnız pradoşu tamamen kaplıyorsa
  return c.length > 1 && c[1].ov >= 1 ? c[1].d : c[0].d;
}
function makarSankranti(y) {
  const t = sankranti(y, 9), d = istDate(t);
  return t < delhiSun(d).set ? d : addDay(d, 1); // geçiş gün batımından sonraysa puṇya kāla ertesi gün
}
const tithiRule = (m, k, rule) => (y) => { const l = lunarMonth(y, m); return l ? rule(tithiSpan(l, k)) : null; };
const RULES = [
  ["makarSankranti", makarSankranti],
  ["vasantPanchami", tithiRule(10, 5, (s) => vyapini(s, "purvahna", "first"))], // Māgha śukla 5
  ["mahaShivaratri", tithiRule(10, 29, (s) => vyapini(s, "nishita", "first", (x) => vyapini(x, "pradosh", "first")))], // Māgha kṛṣṇa 14
  ["holikaDahan", holikaDahan], // Phālguna pūrṇimā
  ["chaitraNavratri", tithiRule(0, 1, udaya)], // Caitra śukla 1: Hindu yeni yılı (Vikram Samvat)
  ["ramNavami", tithiRule(0, 9, (s) => vyapini(s, "madhyahna", "second"))], // Caitra śukla 9 (Aṣṭamī ile karışanı değil)
  ["hanumanJayanti", tithiRule(0, 15, udaya)], // Caitra pūrṇimā
  ["rakshaBandhan", rakshaBandhan], // Śrāvaṇa pūrṇimā
  ["janmashtami", tithiRule(4, 23, udaya)], // Śrāvaṇa (amānta) kṛṣṇa 8, Vaishnava
  ["ganeshChaturthi", tithiRule(5, 4, (s) => vyapini(s, "madhyahna", "first"))], // Bhādrapada śukla 4
  ["navratri", tithiRule(6, 1, udaya)], // Āśvina śukla 1: Śāradīya Navarātri
  ["dussehra", tithiRule(6, 10, (s) => vyapini(s, "aparahna", "first"))], // Āśvina śukla 10
  ["dhanteras", tithiRule(6, 28, (s) => vyapini(s, "pradosh", "first"))], // Āśvina (amānta) kṛṣṇa 13
  ["diwali", diwali], // Āśvina (amānta) amāvasyā: Lakṣmī Pūjā
  ["bhaiDooj", tithiRule(7, 2, (s) => vyapini(s, "aparahna", "max"))], // Kārttika śukla 2
  ["chhath", tithiRule(7, 6, udaya)], // Kārttika śukla 6: akşam arghyası
];

const evCache = new Map();
export function events(year) {
  if (!evCache.has(year)) {
    const list = [];
    for (const [key, rule] of RULES) {
      const date = rule(year);
      if (date && date.startsWith(String(year))) list.push({ date, key });
      if (key === "holikaDahan" && date) { const h = addDay(date, 1); if (h.startsWith(String(year))) list.push({ date: h, key: "holi" }); }
    }
    evCache.set(year, list);
  }
  return evCache.get(year);
}

/** Vikram Samvat yılının başladığı gün (Gregoryen yıl y): yılın ilk Caitra ayının (artık olabilir) ilk günü */
const vsCache = new Map();
export function vikramSamvatStart(y) {
  if (!vsCache.has(y)) {
    const l = lunations(Date.UTC(y, 1, 1), Date.UTC(y, 4, 15)).find((x) => x.m === 0);
    const d = istDate(l.s);
    vsCache.set(y, delhiSun(d).rise > l.s ? d : addDay(d, 1));
  }
  return vsCache.get(y);
}
/** Gün doğumundaki pañcāṅga (Yeni Delhi): { month 0..11, adhika, tithi 1..30, nakshatra 1..27, vs } */
export function panchang(date) {
  const r = delhiSun(date).rise, l = lunationAt(r), y = +date.slice(0, 4);
  return { month: l.m, adhika: l.adhika, tithi: tithiAt(r), nakshatra: nakshatraAt(r), vs: date >= vikramSamvatStart(y) ? y + 57 : y + 56 };
}

/** Yılın günlerine göre günlük kodlar (0 = 1 Ocak) */
const dailyCache = new Map();
export function daily(year) {
  if (!dailyCache.has(year)) {
    const out = [];
    for (let t = Date.UTC(year, 0, 1); t < Date.UTC(year + 1, 0, 1); t += DAY) {
      const p = panchang(isoOf(t));
      out.push(`${p.adhika ? "a" : "m"}${p.month + 1}t${p.tithi}n${p.nakshatra}v${p.vs}`);
    }
    dailyCache.set(year, out);
  }
  return dailyCache.get(year).slice();
}
