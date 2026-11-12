/* Kutlama e-postaları için Türkiye günleri (standart madde 7): Ramazan ve Kurban Bayramı Diyanet takviminden
   (src/data/holidays.json), 23 Nisan, 1 Mayıs, 19 Mayıs, 30 Ağustos, 29 Ekim sabit; adlar sitenin 19 dilinden.
   Sunucu yalnız server/ klasörüyle derlendiği için çıktı depoya yazılır: server/data/tr-days.json.
   Kullanım: node server/scripts/gen-data.mjs [--check] */
import { readFileSync, writeFileSync } from "node:fs";
import { LANGS } from "../src/config.mjs";

const root = new URL("../../", import.meta.url);
const read = (p) => JSON.parse(readFileSync(new URL(p, root), "utf8"));
const hol = read("src/data/holidays.json");
const first = (v) => [].concat(v || [])[0];
const out = { _: "Üretilir: node server/scripts/gen-data.mjs (kaynak src/data/holidays.json, src/i18n). Elle değiştirmeyin.", bayram: {}, sabit: { apr23: "04-23", may1: "05-01", may19: "05-19", aug30: "08-30", oct29: "10-29" }, adlar: {} };
for (const [y, h] of Object.entries(hol.years)) {
  if (h.rb) out.bayram[first(h.rb)] = "rb";
  if (h.kb) out.bayram[first(h.kb)] = "kb";
}
for (const l of LANGS) {
  const t = read(`src/i18n/${l}.json`);
  const e = t.calendar.events, c = t.cdp.targets;
  out.adlar[l] = { rb: c.rb1.name, kb: c.kb1.name, apr23: e.apr23, may1: e.may1, may19: e.may19, aug30: e.aug30, oct29: e.oct29 };
}
const text = JSON.stringify(out) + "\n";
const dest = new URL("../data/tr-days.json", import.meta.url);
if (process.argv.includes("--check")) {
  let cur = "";
  try { cur = readFileSync(dest, "utf8"); } catch {}
  if (cur !== text) { console.error("server/data/tr-days.json güncel değil: node server/scripts/gen-data.mjs"); process.exit(1); }
  console.log("tr-days.json güncel");
} else {
  writeFileSync(dest, text);
  console.log("tr-days.json yazıldı:", Object.keys(out.bayram).length, "bayram günü");
}
