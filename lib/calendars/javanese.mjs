/* Cava takvimi (kapsayıcı takvimler, 10 Kasım 2026): Sultan Agung'un 1633'te kurduğu ay takvimi (Anno Javanico, AJ),
   beş günlük pasaran haftası ve Cava yılbaşı (1 Suro).

   Aritmetik takvimdir: aylar sırayla 30 ve 29 gün (Sura 30, Sapar 29 … Sela 30, Besar 29; artık yılda Besar 30). Sekiz yıllık
   windu: Alip 354, Ehe 354, Jimawal 355, Je 354, Dal 355, Be 354, Wawu 354, Jimakir 355 gün (2835 gün = 567 pasaran haftası).
   120 yıllık kurup'un son yılında Besar'dan bir gün düşülür. Bugünkü kurup "Alip Selasa Pon": 24 Mart 1936'da (1 Sura 1867 AJ,
   Salı Pon) başladı, 25 Ağustos 2052'de biter (Wikipedia "Javanese calendar"; H. Danudji, Penanggalan Jawa 120 Tahun Kurup
   Asapon, 2006). Bu modül yalnız bu kurup içinde hesaplar (dışına çıkınca durur).
   Pasaran: Legi, Pahing, Pon, Wage, Kliwon (17 Ağustos 1945 Cuma Legi). 1 Suro hükümetin 1 Muharrem'inden bir gün ayrılabilir.
   Olaylar: 1 Suro, Grebeg Mulud (12 Mulud, Yogyakarta ve Surakarta saraylarında). Doğrulama javanese.test.json. */
import { addDays } from "../events.mjs";

export const meta = {
  id: "javanese",
  group: "cultural",
  regions: ["ID"],
  color: "#978049", // batik kahvesi (soga); diğer takvimlerden ve Türkiye günü renklerinden ΔE ≥ 21
};

const EPOCH = "1936-03-24", EPOCH_AJ = 1867, KURUP_END = "2052-08-25";
const YEAR_LEN = [354, 354, 355, 354, 355, 354, 354, 355]; // Alip … Jimakir
const dayNum = (s) => Date.UTC(+s.slice(0, 4), +s.slice(5, 7) - 1, +s.slice(8, 10)) / 86400000;

/** AJ yılının 1 Sura'sı (kurup Asapon içinde) */
export function sura1(aj) {
  if (aj < EPOCH_AJ || aj > EPOCH_AJ + 119) throw new Error(`javanese: ${aj} AJ kurup Asapon dışında`);
  let d = 0;
  for (let y = EPOCH_AJ; y < aj; y++) d += YEAR_LEN[(y - EPOCH_AJ) % 8];
  return addDays(EPOCH, d);
}
/** AJ yılının ay başları (12 ay) */
function yearMonths(aj) {
  const rows = [];
  let d = sura1(aj);
  for (let m = 1; m <= 12; m++) { rows.push([d, aj, m, 0]); d = addDays(d, m % 2 ? 30 : 29); }
  return rows;
}
/** Gregoryen yıl g'nin 1 Ocak'ının düştüğü AJ yılı */
const ajOf = (g) => { let aj = g - 67; while (sura1(aj + 1) <= `${g}-01-01`) aj++; while (sura1(aj) > `${g}-01-01`) aj--; return aj; };

export function months(fromYear, toYear) {
  const rows = [];
  for (let aj = ajOf(fromYear); sura1(aj) <= `${toYear + 1}-01-01`; aj++) rows.push(...yearMonths(aj));
  if (rows[rows.length - 1][0] > KURUP_END) throw new Error("javanese: kurup Asapon'un sonu");
  const b = rows.findIndex((r) => r[0] > `${toYear}-12-31`);
  return rows.slice(0, b + 1);
}

export function events(year) {
  const out = [];
  for (let aj = ajOf(year); aj <= ajOf(year) + 1; aj++) {
    const m = yearMonths(aj);
    out.push({ key: "suro", date: m[0][0] }, { key: "grebegMulud", date: addDays(m[2][0], 11) });
  }
  return out.filter((e) => e.date.startsWith(String(year)));
}

/** Pasaran: p1 Legi, p2 Pahing, p3 Pon, p4 Wage, p5 Kliwon (yılın günlerine göre dizi) */
export function daily(year) {
  const first = dayNum(`${year}-01-01`), n = (Date.UTC(year + 1, 0, 1) - Date.UTC(year, 0, 1)) / 86400000, e = dayNum(EPOCH);
  return Array.from({ length: n }, (_, i) => `p${((((first + i - e) % 5) + 5 + 2) % 5) + 1}`);
}
