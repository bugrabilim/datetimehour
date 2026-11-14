/* 1) Kutlama e-postaları için Türkiye günleri (standart madde 7): Ramazan ve Kurban Bayramı Diyanet takviminden
   (src/data/holidays.json), 23 Nisan, 1 Mayıs, 19 Mayıs, 30 Ağustos, 29 Ekim sabit; adlar sitenin 19 dilinden.
   Sunucu yalnız server/ klasörüyle derlendiği için çıktı depoya yazılır: server/data/tr-days.json.
   2) Web hesap sayfalarının çerez bandı metinleri (sitedeki consent.*) ve rozet adları (uygulamalardaki badges.items.*.name):
   server/data/web-texts.json.
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
const web = { _: "Üretilir: node server/scripts/gen-data.mjs (kaynak src/i18n consent, mobile/shared/app-strings.json). Elle değiştirmeyin.", cookie: {}, badges: {} };
const app = read("mobile/shared/app-strings.json");
const BADGE_IDS = Object.keys(app).filter((k) => /^badges\.items\.[^.]+\.name$/.test(k)).map((k) => k.split(".")[2]);
for (const l of LANGS) {
  const c = read(`src/i18n/${l}.json`).consent;
  web.cookie[l] = Object.fromEntries(["accept", "reject", "prefs", "policy", "title", "necessary", "stats", "statsText", "save"].map((k) => [k, c[k]]));
  web.rozet = web.rozet || {};
  web.rozet[l] = read(`src/i18n/${l}.json`).badge;
  web.badges[l] = Object.fromEntries(BADGE_IDS.map((id) => [id, app[`badges.items.${id}.name`][l]]));
}
const files = [["tr-days.json", JSON.stringify(out) + "\n"], ["web-texts.json", JSON.stringify(web) + "\n"]];
const check = process.argv.includes("--check");
let stale = false;
for (const [name, text] of files) {
  const dest = new URL(`../data/${name}`, import.meta.url);
  if (check) {
    let cur = "";
    try { cur = readFileSync(dest, "utf8"); } catch {}
    if (cur !== text) { console.error(`server/data/${name} güncel değil: node server/scripts/gen-data.mjs`); stale = true; }
  } else {
    writeFileSync(dest, text);
    console.log(`${name} yazıldı`);
  }
}
if (stale) process.exit(1);
if (check) console.log("server/data güncel");
