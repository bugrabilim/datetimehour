/* Bahai takvimi (Badíʿ takvimi; kapsayıcı takvimler, 9 Kasım 2026): Bahai kutsal günleri ve Badíʿ tarihi.
   Kurallar (Evrensel Adalet Evi, 10 Temmuz 2014; 172 BE'den, 21 Mart 2015'ten beri bütün dünyada):
   - Yıl Naw-Rúz'da başlar: ilkbahar ekinoksunun Tahran'da gerçekleştiği Bahai günü. Bahai günü gün batımından gün batımına
     sürer: ekinoks Tahran'da gün batımından önceyse Naw-Rúz o gündür, sonraysa ertesi gün.
   - 19 ay × 19 gün; 18. ay (Mulk) ile 19. ay (ʿAláʾ, oruç ayı) arasında Ayyám-i-Há: bir sonraki Naw-Rúz'a göre 4 ya da 5 gün.
   - İkiz Kutsal Doğum Günleri (Báb'ın ve Bahá'u'lláh'ın doğumu): Naw-Rúz'dan sonraki sekizinci yeni ayın (kavuşum) düştüğü
     Bahai gününü izleyen iki gün, Tahran'a göre. Naw-Rúz gününün içindeki yeni ay sayılmaz (resmî tabloda 2034 ve 2053);
     24 Kasım 2014 mektubu: 2015'te yeni ay 11 Kasım gün batımından sonra (9 Qudrat 172), doğum günleri 10 ve 11 Qudrat.
   - Diğer kutsal günler Badíʿ tarihine sabittir: Riḍván 13 Jalál – 5 Jamál (1., 9. ve 12. günleri), Báb'ın İlanı 8 ʿAẓamat,
     Bahá'u'lláh'ın Göçü 13 ʿAẓamat, Báb'ın Şehadeti 17 Raḥmat, Ahit Günü 4 Qawl, ʿAbdu'l-Bahá'nın Göçü 6 Qawl.
   Olay tarihi Bahai gününün gündüzünün Gregoryen tarihidir (gün önceki akşam başlar; Bahai Dünya Merkezi tabloları da böyle);
   ikinci tarih de gündüze göredir. Ay numaraları: 1–19 aylar, 20 Ayyám-i-Há (metinlerdeki month dizisinin 20. öğesi).
   Ekinoks ve yeni ay src/js/nature-calc.js (Meeus 27 ve 49), Tahran gün batımı src/js/prayer-calc.js sunAltTimes (−0,833°).
   Ekinoks ya da yeni ay Tahran gün batımına 10 dakikadan yakınsa hesabın kesinliği yetmez: o yılın tarihi Bahai Dünya
   Merkezi'nin "Badíʿ dates 172 to 221 BE" tablosundan alınır (OFFICIAL); tabloda yoksa hesap durur. 172–221 BE (2015–2064)
   için hesap tablodaki bütün Naw-Rúz ve İkiz Doğum Günleri tarihleriyle karşılaştırıldı: tek fark 2026 Naw-Rúz'u (ekinoks
   gün batımından yalnız saniyeler önce/sonra; resmî tarih 21 Mart). Test verisi bahai.test.json. */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { addDays } from "../events.mjs";

export const meta = {
  id: "bahai",
  group: "religious", // varsayılan her yerde kapalı (kimsenin inancı tahmin edilmez)
  regions: [],
  color: "#9C7410", // altın (koyu hardal)
};

export const AYYAM_I_HA = 20; // ay tablosunda Ayyám-i-Há'nın numarası

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
let calc = null;
const lib = () => {
  if (!calc) {
    const load = (rel) => { const m = { exports: {} }; new Function("module", "window", fs.readFileSync(path.join(ROOT, rel), "utf8"))(m, undefined); return m.exports; };
    calc = { nature: load("src/js/nature-calc.js"), prayer: load("src/js/prayer-calc.js") };
  }
  return calc;
};

/* Tahran (WGS84; tablonun başvuru yeri), İran Standart Saati (UTC+3:30). Saat dilimi yalnız gün batımının hangi güne
   düştüğünü bulmak için; gün batımı Tahran'da 13.30–16.30 UTC arasıdır, yaz saati sonucu değiştirmez. */
const TEHRAN = { lat: 35.6892, lon: 51.389, tz: 3.5 };
const DAY = 86400000;
const CLOSE_MS = 10 * 60000;

/* Bahai Dünya Merkezi tablosu ("Badíʿ dates 172 to 221 BE", Her Majesty's Nautical Almanac Office verisiyle): yalnız
   hesabın gün batımına 10 dakikadan yakın kaldığı yıllar. 2026: ekinoks 14.45.36 UTC, hesaplanan gün batımı 14.45.59 UTC. */
const OFFICIAL = {
  nawRuz: { 2026: "2026-03-21", 2059: "2059-03-20" },
  birthBab: { 2027: "2027-10-30" },
};

const isoOf = (ms) => new Date(ms).toISOString().slice(0, 10);
/** Tahran'da o günün gün batımı (UTC ms) */
function sunset(day) {
  const [y, m, d] = day.split("-").map(Number);
  const s = lib().prayer.sunAltTimes(y, m, d, TEHRAN.lat, TEHRAN.lon, TEHRAN.tz, -0.833).set;
  return Date.UTC(y, m - 1, d) + (s - TEHRAN.tz) * 3600000;
}
/** An t'nin düştüğü Bahai gününün gündüz tarihi; an gün batımına çok yakınsa null */
function bahaiDay(t) {
  const local = isoOf(t + TEHRAN.tz * 3600000), s = sunset(local);
  if (Math.abs(t - s) < CLOSE_MS) return null;
  return t < s ? local : addDays(local, 1);
}

const nrCache = new Map();
/** Gregoryen yıl g'nin Naw-Rúz'u (1 Bahá; YYYY-MM-DD) */
export function nawRuz(g) {
  if (OFFICIAL.nawRuz[g]) return OFFICIAL.nawRuz[g];
  if (!nrCache.has(g)) {
    const d = bahaiDay(lib().nature.seasons(g).march);
    if (!d) throw new Error(`bahai: ${g} ekinoksu Tahran'da gün batımına çok yakın; Bahai Dünya Merkezi tablosundaki tarihi OFFICIAL.nawRuz'a ekleyin`);
    nrCache.set(g, d);
  }
  return nrCache.get(g);
}

/** Gregoryen yıl g'deki Báb'ın doğumu (Bahá'u'lláh'ın doğumu ertesi gün) */
export function birthOfTheBab(g) {
  if (OFFICIAL.birthBab[g]) return OFFICIAL.birthBab[g];
  const { phaseMs } = lib().nature;
  const after = sunset(nawRuz(g)); // Naw-Rúz gününün bitişi
  let k = Math.floor((after / DAY + 2440587.5 - 2451550.09766) / 29.530588861) - 2;
  while (phaseMs(k, false) < after) k++;
  const d = bahaiDay(phaseMs(k + 7, false)); // sekizinci yeni ay
  if (!d) throw new Error(`bahai: ${g} sekizinci yeni ayı Tahran'da gün batımına çok yakın; Bahai Dünya Merkezi tablosundaki tarihi OFFICIAL.birthBab'a ekleyin`);
  return addDays(d, 1);
}

/** Naw-Rúz'u nr olan yılda Badíʿ tarihi (ay 1–18, gün 1–19) */
const badi = (nr, month, day) => addDays(nr, 19 * (month - 1) + day - 1);

/** Naw-Rúz'u Gregoryen yıl g'de olan Badíʿ yılının ay başları: [[tarih, BE yılı, ay], …] (19 ay ve Ayyám-i-Há) */
function yearMonths(g) {
  const nr = nawRuz(g), next = nawRuz(g + 1), be = g - 1843;
  const rows = [];
  for (let m = 1; m <= 18; m++) rows.push([badi(nr, m, 1), be, m]);
  const ayyam = badi(nr, 18, 20), ala = addDays(next, -19);
  const n = (Date.parse(ala) - Date.parse(ayyam)) / DAY;
  if (n !== 4 && n !== 5) throw new Error(`bahai: ${be} BE Ayyám-i-Há ${n} gün (4 ya da 5 olmalı)`);
  rows.push([ayyam, be, AYYAM_I_HA], [ala, be, 19]);
  return rows;
}

/** Gregoryen yılda başlayan olaylar: önceki Badíʿ yılının Ayyám-i-Há'sı ve orucu (şubat–mart), bu yılınkiler (mart–kasım) */
export function events(year) {
  const nr = nawRuz(year), prev = nawRuz(year - 1), bab = birthOfTheBab(year);
  return [
    { key: "ayyamIHa", date: badi(prev, 18, 20), end: addDays(nr, -20) },
    { key: "fast", date: addDays(nr, -19), end: addDays(nr, -1) }, // ʿAláʾ ayı, 19 gün
    { key: "nawRuz", date: nr },
    { key: "ridvan1", date: badi(nr, 2, 13) },
    { key: "ridvan9", date: badi(nr, 3, 2) },
    { key: "ridvan12", date: badi(nr, 3, 5) },
    { key: "declarationBab", date: badi(nr, 4, 8) },
    { key: "ascensionBahaullah", date: badi(nr, 4, 13) },
    { key: "martyrdomBab", date: badi(nr, 6, 17) },
    { key: "birthBab", date: bab },
    { key: "birthBahaullah", date: addDays(bab, 1) },
    { key: "covenant", date: badi(nr, 14, 4) },
    { key: "ascensionAbdulBaha", date: badi(nr, 14, 6) },
  ];
}

/** Ay tablosu: [ay başı, BE yılı, ay (1–19; 20 Ayyám-i-Há), 0]; fromYear-01-01'i kapsayan aydan toYear-12-31'den sonraki ilk ay başına */
export function months(fromYear, toYear) {
  const rows = [];
  for (let g = fromYear - 1; g <= toYear; g++) for (const r of yearMonths(g)) rows.push([r[0], r[1], r[2], 0]);
  const first = `${fromYear}-01-01`, last = `${toYear}-12-31`;
  const a = rows.findLastIndex((r) => r[0] <= first), b = rows.findIndex((r) => r[0] > last);
  return rows.slice(a, b + 1);
}
