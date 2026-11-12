// Dünya resmî tatilleri: Nager.Date verisi (src/data/world-holidays.json, scripts/fetch-world-holidays.mjs) ile
// hükümetlerin kendi listelerinden elle girilen ülkeler (src/data/world-holidays-official.json; 10 Kasım 2026: Endonezya
// SKB 3 Menteri, Hindistan DoPT). Site (build.mjs) ve uygulamalar (gen-strings.mjs) bu birleşimi kullanır.
// Günlerde k: "leave" = ortak izin günü (cuti bersama): takvimde gösterilir, tatil sayısına girmez.
import fs from "node:fs";
import path from "node:path";

export function loadWorldHolidays(root) {
  const read = (rel) => JSON.parse(fs.readFileSync(path.join(root, rel), "utf8"));
  const out = read("src/data/world-holidays.json");
  const off = read("src/data/world-holidays-official.json");
  out.official = {};
  for (const [cc, years] of Object.entries(off.countries)) {
    if (out.countries[cc]) throw new Error(`Dünya tatilleri: ${cc} hem Nager hem resmî listede`);
    out.countries[cc] = years;
    out.official[cc] = off.sources[cc];
  }
  return out;
}

/** Tatil sayısına girenler (ortak izin günleri hariç) */
export const publicDays = (days) => days.filter((h) => h.k !== "leave");
