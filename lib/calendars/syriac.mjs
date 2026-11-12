/* Süryani takvimi (kapsayıcı takvimler, 10 Kasım 2026): Süryani ay adları, Seleukos (Yunani) çağına göre yıl ve Süryani
   Ortodoks Kilisesi'nin bayramları.

   Kilise 1955'ten beri sabit bayramları Gregoryen takvimle tutar; Büyük Perhiz ve Paskalya'yı Jülyen hesabıyla (Ortodoks
   Paskalyası) bulur (Süryani Ortodoks Kilisesi, Batı ABD Başpiskoposluğu, "Liturgical Calendar of the Syriac Orthodox
   Church"; Kudüs'te Noel 6 Ocak'tır). Büyük Perhiz Paskalya'dan yedi hafta önceki pazartesi başlar (Paskalya − 48 gün),
   Ninovalıların orucu (Ba'utho d-Ninwoye) Büyük Perhiz'den üç hafta önceki pazartesi başlar ve üç gün sürer. Kilise yılı
   Qudosh 'Idto pazarıyla başlar: 30 ya da 31 Ekim pazarsa o gün, değilse Kasım'ın ilk pazarı.
   Ay adları Gregoryen aylarla aynı günlerde başlar: Teşrin Qdem (Ekim) … Ilul (Eylül). Yıl Seleukos çağıdır ("Yunanilerin
   yılı"): 1 Teşrin Qdem'de (1 Ekim) değişir; Ocak–Eylül'de Gregoryen yıl + 311, Ekim–Aralık'ta + 312.
   Akitu (Kha b-Nisan, 1 Nisan) Süryani ve Asur toplulukların bahar bayramıdır; kilise bayramı değildir. Doğrulama syriac.test.json. */
import { iso, addDays, dowOf } from "../events.mjs";
import { orthodoxEaster } from "./christian.mjs";

export const meta = {
  id: "syriac",
  group: "religious",
  regions: [],
  color: "#119C6C", // zeytin dalı yeşili; diğer takvimlerden ve Türkiye günü renklerinden ΔE ≥ 21
};

/** Kilise yılının başı (Qudosh 'Idto): 30–31 Ekim pazarsa o gün, değilse Kasım'ın ilk pazarı */
export function qudoshIdto(y) {
  const SUNDAY = 6; // dowOf: pazartesi 0 … pazar 6
  for (const d of [iso(y, 10, 30), iso(y, 10, 31)]) if (dowOf(d) === SUNDAY) return d;
  let d = iso(y, 11, 1);
  while (dowOf(d) !== SUNDAY) d = addDays(d, 1);
  return d;
}

export function events(year) {
  const e = orthodoxEaster(year);
  const lent = addDays(e, -48);
  const nin = addDays(lent, -21);
  return [
    { key: "circumcision", date: iso(year, 1, 1) },
    { key: "epiphany", date: iso(year, 1, 6) },
    { key: "presentation", date: iso(year, 2, 2) },
    { key: "nineveh", date: nin, end: addDays(nin, 2) },
    { key: "greatLent", date: lent },
    { key: "annunciation", date: iso(year, 3, 25) },
    { key: "akitu", date: iso(year, 4, 1) },
    { key: "palmSunday", date: addDays(e, -7) },
    { key: "goodFriday", date: addDays(e, -2) },
    { key: "easter", date: e },
    { key: "ascension", date: addDays(e, 39) },
    { key: "pentecost", date: addDays(e, 49) },
    { key: "assumption", date: iso(year, 8, 15) },
    { key: "cross", date: iso(year, 9, 14) },
    { key: "churchYear", date: qudoshIdto(year) },
    { key: "christmas", date: iso(year, 12, 25) },
  ].filter((x) => x.date.startsWith(String(year)));
}

/** Ay tablosu: Teşrin Qdem (1, Ekim) … Ilul (12, Eylül); Seleukos yılı Ekim'de değişir */
export function months(fromYear, toYear) {
  const rows = [];
  for (let y = fromYear - 1; y <= toYear + 1; y++)
    for (let gm = 1; gm <= 12; gm++) {
      const m = gm >= 10 ? gm - 9 : gm + 3;
      rows.push([iso(y, gm, 1), gm >= 10 ? y + 312 : y + 311, m, 0]);
    }
  const a = rows.findLastIndex((r) => r[0] <= `${fromYear}-01-01`), b = rows.findIndex((r) => r[0] > `${toYear}-12-31`);
  return rows.slice(a, b + 1);
}
