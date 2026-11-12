/* BM uluslararası günleri (kapsayıcı takvimler, 9 Kasım 2026): Birleşmiş Milletler'in uluslararası günler listesinden
   (https://www.un.org/en/observances/list-days-weeks) en çok bilinen 45 gün. Ölçüt: listede olan, BM Genel Kurulu kararıyla
   ya da BM uzman kuruluşunca (UNESCO, DSÖ, FAO, WMO) ilan edilmiş, dünya çapında yaygın anılan gün; aynı tarihte birden çok gün
   varsa yalnız en bilineni (ör. 21 Mart: ırk ayrımcılığı; 20 Temmuz: satranç). Haftalar, on yıllar ve dil günleri yok.
   İki gün her yıl değişir: Dünya Habitat Günü ekimin ilk pazartesi, Dünya Felsefe Günü kasımın üçüncü perşembesi.
   Test verisi un.test.json (her günün un.org adresi). */
import { iso, dowOf, daysInMonth } from "../events.mjs";

export const meta = {
  id: "un",
  group: "civil",
  regions: [], // hiçbir yerde varsayılan açık değil
  color: "#64748B",
};

/* [anahtar, ay, gün]: un.org listesindeki sabit tarih */
const FIXED = [
  ["education", 1, 24], ["holocaust", 1, 27], ["womenScience", 2, 11], ["motherLanguage", 2, 21], ["wildlife", 3, 3],
  ["women", 3, 8], ["happiness", 3, 20], ["racialDiscrimination", 3, 21], ["water", 3, 22], ["meteorology", 3, 23],
  ["autism", 4, 2], ["health", 4, 7], ["motherEarth", 4, 22], ["book", 4, 23], ["pressFreedom", 5, 3], ["bees", 5, 20],
  ["biodiversity", 5, 22], ["environment", 6, 5], ["oceans", 6, 8], ["refugees", 6, 20], ["yoga", 6, 21],
  ["population", 7, 11], ["chess", 7, 20], ["friendship", 7, 30], ["youth", 8, 12], ["humanitarian", 8, 19],
  ["literacy", 9, 8], ["peace", 9, 21], ["olderPersons", 10, 1], ["teachers", 10, 5], ["mentalHealth", 10, 10],
  ["girlChild", 10, 11], ["food", 10, 16], ["unDay", 10, 24], ["tolerance", 11, 16], ["toilet", 11, 19],
  ["children", 11, 20], ["violenceWomen", 11, 25], ["aids", 12, 1], ["disabilities", 12, 3], ["volunteers", 12, 5],
  ["humanRights", 12, 10], ["migrants", 12, 18],
];

/** Ayın n. haftanın günü (wd: pazartesi 0 … pazar 6, events.mjs dowOf gibi; n: 1'den) */
export function nthWeekday(year, month, wd, n) {
  const first = dowOf(iso(year, month, 1));
  const day = 1 + ((wd - first + 7) % 7) + 7 * (n - 1);
  if (day > daysInMonth(year, month)) throw new Error(`nthWeekday: ${year}-${month} ayında ${n}. gün yok`);
  return iso(year, month, day);
}

export function events(year) {
  const list = FIXED.map(([key, m, d]) => ({ date: iso(year, m, d), key }));
  list.push({ date: nthWeekday(year, 10, 0, 1), key: "habitat" });     // ekimin ilk pazartesi
  list.push({ date: nthWeekday(year, 11, 3, 3), key: "philosophy" });  // kasımın üçüncü perşembesi
  return list.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0));
}
