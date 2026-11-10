/* Zerdüşt takvimi (kapsayıcı takvimler, 10 Kasım 2026): Hindistan ve dünyadaki Parsilerin çoğunun kullandığı Şehinşahi
   (Shenshai) takvimi; Kadimi (Kadmi) ve Fasli yılbaşları olay olarak.

   Üç takvim de 30 günlük 12 ay ve yılın sonunda 5 Gatha gününden oluşur (365 gün). Şehinşahi ve Kadimi'de artık gün yoktur,
   bu yüzden yılbaşı her Gregoryen artık yılda bir gün geriye kayar; Kadimi Şehinşahi'nin 30 gün önündedir. Fasli (İran'da
   Bastani) yılbaşı her yıl 21 Mart'tır (Hindistan'da Jamshedi Navroz).
   Yezdicerd çağı (YZ): Şehinşahi YZ yılının 1 Ferverdin'i JDN 1952093 + (YZ − 1) × 365, Kadimi'de 1952063 + (YZ − 1) × 365
   (Wikipedia "Zoroastrian calendar": 21 Ağustos 2000 Şehinşahi 1370 YZ yılbaşı).
   Olaylar Şehinşahi günleriyle: Navroz (1/1), Khordad Sal (1/6, Zerdüşt'ün doğumu), Farvardegan (1/19), Zarthost no Diso
   (10/11, Zerdüşt'ün ölüm yıldönümü), altı Gahambar (2/11–15, 4/11–15, 6/26–30, 7/26–30, 10/16–20, Gatha günleri) ve Muktad
   (yılın son 10 günü). Doğrulama zoroastrian.test.json (Hindistan hükümeti tatil listeleri, Parsi topluluk takvimleri). */
import { addDays } from "../events.mjs";

export const meta = {
  id: "zoroastrian",
  group: "religious",
  regions: [],
  color: "#B04C3B", // ateş kırmızısı; diğer takvimlerden ve Türkiye günü renklerinden ΔE ≥ 21
};

export const GATHA = 13; // ay tablosunda Gatha günlerinin numarası

const JD_UNIX = 2440588; // 1970-01-01'in JDN'si
const fromJdn = (j) => new Date((j - JD_UNIX) * 86400000).toISOString().slice(0, 10);
const shahanshahiNavroz = (yz) => fromJdn(1952093 + (yz - 1) * 365);
const kadmiNavroz = (yz) => fromJdn(1952063 + (yz - 1) * 365);
/** Gregoryen yıl g'de yılbaşı olan Şehinşahi yılı (Ağustos'ta; 2025 → 1395) */
const yzOf = (g) => g - 630;

/** Şehinşahi yılı yz'de ay m (1–12, Gatha 13) gün d (1'den) */
const shDate = (yz, m, d) => addDays(shahanshahiNavroz(yz), (m - 1) * 30 + d - 1);

export function months(fromYear, toYear) {
  const rows = [];
  for (let yz = yzOf(fromYear) - 1; yz <= yzOf(toYear) + 1; yz++)
    for (let m = 1; m <= GATHA; m++) rows.push([shDate(yz, m, 1), yz, m, 0]);
  const a = rows.findLastIndex((r) => r[0] <= `${fromYear}-01-01`), b = rows.findIndex((r) => r[0] > `${toYear}-12-31`);
  return rows.slice(a, b + 1);
}

/** Şehinşahi yılı yz'nin olayları (bitiş dahil) */
function yearEvents(yz) {
  const ev = (key, m, d, len) => {
    const date = shDate(yz, m, d);
    return len > 1 ? { key, date, end: addDays(date, len - 1) } : { key, date };
  };
  return [
    ev("navroz", 1, 1),
    ev("khordadSal", 1, 6),
    ev("farvardegan", 1, 19),
    ev("maidyozarem", 2, 11, 5),
    ev("maidyoshahem", 4, 11, 5),
    ev("paitishahem", 6, 26, 5),
    ev("ayathrem", 7, 26, 5),
    ev("zarthostNoDiso", 10, 11),
    ev("maidyarem", 10, 16, 5),
    ev("muktad", 12, 26, 10), // son 5 gün + 5 Gatha günü
    ev("hamaspathmaedem", GATHA, 1, 5),
  ];
}

export function events(year) {
  const out = [];
  for (let yz = yzOf(year) - 1; yz <= yzOf(year); yz++) out.push(...yearEvents(yz));
  // Kadimi yılbaşı (Şehinşahi'den 30 gün önce) ve Fasli yılbaşı (Jamshedi Navroz, 21 Mart)
  out.push({ key: "kadmiNavroz", date: kadmiNavroz(yzOf(year)) });
  out.push({ key: "jamshediNavroz", date: `${year}-03-21` });
  return out.filter((e) => e.date.startsWith(String(year)));
}
