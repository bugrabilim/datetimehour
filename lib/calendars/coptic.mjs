/* Kıpti takvimi (kapsayıcı takvimler, 9 Kasım 2026) ve Etiyopya takvimiyle ortak İskenderiye aritmetiği.
   Kıpti ve Etiyopya takvimleri aynı günlerde başlayan 13 aydır: 12 × 30 gün + 5 günlük küçük ay (Kıpti Nesî, Etiyopya Pagume);
   küçük ay her dört yılda bir istisnasız 6 gündür (Kıpti yılı mod 4 = 3; Etiyopya yılı aynı kalanı verir, fark 276 dörde bölünür).
   Yılbaşı (1 Tût = 1 Meskerem) Gregoryen takvimde 11 Eylül'dür; Gregoryen artık yıldan önceki yılda (önceki yılın küçük ayı
   6 gün olduğu için) 12 Eylül. Bu kural Mart 1900 – Şubat 2100 arasında geçerlidir (1900 ve 2100 Gregoryen artık yıl değil).
   Yıl numaraları: Kıpti yılı Şehitler çağı (Anno Martyrum, MS 284) = Gregoryen yıl − 283 (Eylül'den itibaren), Etiyopya yılı
   (Amete Mihret) = Kıpti yılı + 276. Paskalya Ortodoks hesabıyla (İskenderiye kuralı), christian.mjs'ten.
   Test verisi coptic.test.json (copticchurch.net bayram tabloları, truecalendar.com ay tabloları; kodları kullanılmadı). */
import { iso, addDays } from "../events.mjs";
import { orthodoxEaster } from "./christian.mjs";

export const meta = {
  id: "coptic",
  group: "religious", // varsayılan her yerde kapalı (kimsenin inancı tahmin edilmez)
  regions: [],
  color: "#23848C", // camgöbeği; İslami günlerin yeşili ve diğer takvimlerden ayrı
};

/* ---------- Ortak İskenderiye aritmetiği (ethiopian.mjs de kullanır) ---------- */

/** Etiyopya yılı = Kıpti yılı + ETHIOPIAN_OFFSET */
export const ETHIOPIAN_OFFSET = 276;
/** Eylül'de başlayan Kıpti yılı = Gregoryen yıl − COPTIC_OFFSET */
export const COPTIC_OFFSET = 283;

const gregLeap = (y) => (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;

/** Gregoryen y yılının Eylül'ünde başlayan İskenderiye yılının ilk günü (1 Tût = 1 Meskerem) */
export function newYearDay(y) {
  if (y < 1900 || y > 2098) throw new Error("İskenderiye takvimi: 11/12 Eylül kuralı 1900–2098 yılbaşları için geçerli");
  return iso(y, 9, gregLeap(y + 1) ? 12 : 11);
}

/** Gregoryen y yılının Eylül'ünde başlayan İskenderiye yılının ayı ve günü → ISO tarih (ay 1–13, gün 1–30) */
export function alexToIso(y, month, day) {
  const len = month === 13 ? (newYearDay(y + 1).endsWith("-12") ? 6 : 5) : 30;
  if (month < 1 || month > 13 || day < 1 || day > len) throw new Error(`İskenderiye takvimi: geçersiz tarih ${y}/${month}/${day}`);
  return addDays(newYearDay(y), 30 * (month - 1) + day - 1);
}

/** Noel (Kıpti 29 Kiyehk, Etiyopya 29 Tahsas): Gregoryen y yılının Ocak'ındaki gün. Artık yıldan sonraki yılda (yıl
    12 Eylül'de başlamışsa) kilise bayramı 28'inde tutar; böylece Noel her yıl 7 Ocak'tır (Jülyen 25 Aralık). */
export function nativity(y) {
  const shifted = newYearDay(y - 1).endsWith("-12");
  return alexToIso(y - 1, 4, shifted ? 28 : 29);
}

/** Ay tablosu: [ay başı, yıl, ay (1–13), 0]; yılın numarası yearOf(Eylül'deki Gregoryen yıl) ile.
    fromYear-01-01'den önceki son ay başından toYear-12-31'den sonraki ilk ay başına (son satır yalnız tablonun sonu). */
export function alexMonths(fromYear, toYear, yearOf) {
  const rows = [];
  for (let y = fromYear - 1; y <= toYear + 1; y++) for (let m = 1; m <= 13; m++) rows.push([alexToIso(y, m, 1), yearOf(y), m, 0]);
  const first = `${fromYear}-01-01`, last = `${toYear}-12-31`;
  const a = rows.findLastIndex((r) => r[0] <= first), b = rows.findIndex((r) => r[0] > last);
  return rows.slice(a, b + 1);
}

/* ---------- Kıpti takvimi ---------- */

export function events(year) {
  const easter = orthodoxEaster(year);
  return [
    { key: "christmas", date: nativity(year) },
    { key: "epiphany", date: alexToIso(year - 1, 5, 11) }, // 11 Tûbe (Gıtâs): 19 Ocak, Gregoryen artık yılda 20 Ocak
    { key: "greatLent", date: addDays(easter, -55) }, // Büyük Perhiz (Kutsal Hafta dahil 55 gün), Paskalya'dan önceki 8. pazartesi
    { key: "palmSunday", date: addDays(easter, -7) },
    { key: "goodFriday", date: addDays(easter, -2) },
    { key: "easter", date: easter },
    { key: "nayrouz", date: newYearDay(year) }, // 1 Tût
  ];
}

export const months = (fromYear, toYear) => alexMonths(fromYear, toYear, (y) => y - COPTIC_OFFSET);
