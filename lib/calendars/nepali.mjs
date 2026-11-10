/* Nepal takvimi (kapsayıcı takvimler, 10 Kasım 2026): Nepal'in resmî takvimi Bikram Sambat (Vikram Samvat, BS) ve yıla
   bağlı resmî günler.

   Bikram Sambat güneş takvimidir: ay, Güneş'in yıldızıl burca girmesiyle (saṅkrānti) başlar; ay uzunlukları (29–32 gün)
   Nepal Panchang Karar Kurulu'nun (Nepal Panchanga Nirnayak Samiti) her yıl yayımladığı takvimden gelir ve geleneksel
   Surya Siddhanta hesabına dayanır. Modern astronomik hesap bu tabloyu tutturmadığı için (Lahiri ayanamsası ve Nepal
   saatiyle denendi) ay uzunlukları tablo olarak alındı: açık kaynak nepali-date-converter (MIT) ve bikram-sambat (Apache-2.0)
   tablolarının 2075–2090 BS satırları birebir aynı. 2083 BS'ye (Nisan 2026–Nisan 2027) kadar resmî takvim yayımlandı;
   sonrası yaygın kullanılan tahmin tablosudur (notta yazar), resmî takvim çıkınca karşılaştırılır.
   BS yılı Gregoryen yıl + 56 ya da 57'dir (yılbaşı Nisan ortası). Ay dışı (Ay'a bağlı) bayramlar (Dashain, Tihar…) Nepal'de
   Hindistan'dan bir gün ayrılabildiği için gösterilmez. Doğrulama nepali.test.json. */
import { addDays } from "../events.mjs";

export const meta = {
  id: "nepali",
  group: "civil",
  regions: ["NP"],
  color: "#AB495D", // Nepal kırmızısı (rododendron); diğer takvimlerden ve Türkiye günü renklerinden ΔE ≥ 21
};

/** BS yılı → 12 ayın gün sayısı (Baisakh … Chaitra) */
export const MONTH_DAYS = {
  2080: [31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30],
  2081: [31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31],
  2082: [31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30],
  2083: [31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30],
  2084: [31, 31, 32, 31, 31, 30, 30, 30, 29, 30, 30, 30],
  2085: [31, 32, 31, 32, 30, 31, 30, 30, 29, 30, 30, 30],
  2086: [30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30],
  2087: [31, 31, 32, 31, 31, 31, 30, 30, 29, 30, 30, 30],
  2088: [30, 31, 32, 32, 30, 31, 30, 30, 29, 30, 30, 30],
  2089: [30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30],
  2090: [30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30],
};
export const OFFICIAL_UNTIL = 2083; // resmî takvimi yayımlanmış son BS yılı
const ANCHOR = "2023-04-14"; // 1 Baisakh 2080

function allMonths() {
  const rows = [];
  let d = ANCHOR;
  for (const [y, lens] of Object.entries(MONTH_DAYS)) lens.forEach((n, i) => { rows.push([d, +y, i + 1, 0]); d = addDays(d, n); });
  rows.push([d, 2091, 1, 0]); // tablonun sonu
  return rows;
}

export function months(fromYear, toYear) {
  const rows = allMonths();
  const a = rows.findLastIndex((r) => r[0] <= `${fromYear}-01-01`), b = rows.findIndex((r) => r[0] > `${toYear}-12-31`);
  if (a < 0 || b < 0) throw new Error("nepali: ay tablosu aralığı karşılamıyor");
  return rows.slice(a, b + 1);
}

/** BS tarihi → ISO */
export function bsDate(y, m, d) {
  const r = allMonths().find((x) => x[1] === y && x[2] === m);
  if (!r) throw new Error(`nepali: ${y}/${m} tabloda yok`);
  return addDays(r[0], d - 1);
}

export function events(year) {
  const out = [];
  for (const y of [year + 56, year + 57]) {
    if (!MONTH_DAYS[y]) continue;
    out.push(
      { key: "newYear", date: bsDate(y, 1, 1) },
      { key: "republicDay", date: bsDate(y, 2, 15) },
      { key: "constitutionDay", date: bsDate(y, 6, 3) },
      { key: "maghe", date: bsDate(y, 10, 1) },
      { key: "democracyDay", date: bsDate(y, 11, 7) },
    );
  }
  return out.filter((e) => e.date.startsWith(String(year)));
}
