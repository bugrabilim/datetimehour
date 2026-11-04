/* Dünya resmî tatilleri (30 Ekim 2026, rakip analizi #27): Nager.Date açık API'sinden (date.nager.at, MIT) seçili
   ülkelerin ulusal resmî tatillerini çekip src/data/world-holidays.json'a yazar. Derleme ağa çıkmaz; bu betik yılda bir
   (ya da yeni yıl eklenirken) elle çalıştırılır: node scripts/fetch-world-holidays.mjs
   Yalnız ülke genelindeki (global) ve "Public" türündeki günler alınır; bölgesel tatiller sayfada ayrıca sayılır.
   Bilerek yok: Mısır ve Kazakistan (Nager verisinde Ramazan/Kurban bayramları eksik), Ukrayna (sıkıyönetim süresince
   resmî tatiller izin günü değil). Eklemeden önce verinin bayramları içerdiğini denetleyin. */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
export const WORLD_COUNTRIES = ["GB", "DE", "FR", "NL", "BE", "AT", "CH", "IT", "ES", "PT", "GR", "CY", "BG", "RO", "PL", "SE", "DK", "NO", "IE", "RU", "GE", "US", "CA", "BR", "AU", "JP", "CN", "HK", "SG", "ZA"];
const YEARS = [2024, 2025, 2026, 2027, 2028, 2029, 2030, 2031, 2032];

const out = { source: "Nager.Date (https://date.nager.at), MIT", fetched: new Date().toISOString().slice(0, 10), years: YEARS, countries: {} };
for (const cc of WORLD_COUNTRIES) {
  out.countries[cc] = {};
  for (const y of YEARS) {
    const r = await fetch(`https://date.nager.at/api/v3/PublicHolidays/${y}/${cc}`);
    if (!r.ok) throw new Error(`${cc} ${y}: HTTP ${r.status}`);
    const list = await r.json();
    const regional = list.filter((h) => !h.global && (h.types || []).includes("Public")).length;
    out.countries[cc][y] = {
      regional,
      days: list.filter((h) => h.global && (h.types || []).includes("Public")).map((h) => ({ d: h.date, n: h.name, l: h.localName })),
    };
  }
  process.stdout.write(cc + " ");
}
// Eksik yıllar (ör. Singapur'da 2028'den sonra ay takvimine bağlı bayramlar yok): gün sayısı ülkenin en dolu yılından
// 2'den fazla azsa o yıl veriden çıkarılır ve "incomplete" altında listelenir (site ve uygulamalar o yılı göstermez)
out.incomplete = {};
for (const cc of WORLD_COUNTRIES) {
  const max = Math.max(...YEARS.map((y) => out.countries[cc][y].days.length));
  for (const y of YEARS) if (out.countries[cc][y].days.length < max - 1) {
    (out.incomplete[cc] ||= []).push(y);
    delete out.countries[cc][y];
  }
}
fs.writeFileSync(path.join(ROOT, "src/data/world-holidays.json"), JSON.stringify(out) + "\n");
console.log("\nYazıldı: src/data/world-holidays.json");
