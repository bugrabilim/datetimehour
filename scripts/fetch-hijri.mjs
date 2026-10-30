/* Hicri ay başları (30 Ekim 2026, rakip analizi #9): Diyanet'in namaz vakitleri sayfasındaki (namazvakitleri.diyanet.gov.tr,
   İstanbul; haftalık, aylık ve yıllık tablolar) "Hicri Tarih" sütunundan her hicri ayın gregoryen başlangıcını çıkarıp
   src/data/hijri.json'a ekler (eski kayıtlar korunur, çelişki varsa durur). Hicri takvim Türkiye'nin her yerinde aynıdır.
   Derleme ağa çıkmaz; Diyanet yeni yılın vakitlerini yayımlayınca elle çalıştırılır: node scripts/fetch-hijri.mjs
   Tablo lib/hijri.mjs'te Diyanet'in dini günleriyle (holidays.json) birlikte kullanılır. */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const FILE = path.join(ROOT, "src/data/hijri.json");
const URL = "https://namazvakitleri.diyanet.gov.tr/tr-TR/9541/istanbul-icin-namaz-vakti";
const AY = { ocak: 1, şubat: 2, mart: 3, nisan: 4, mayıs: 5, haziran: 6, temmuz: 7, ağustos: 8, eylül: 9, ekim: 10, kasım: 11, aralık: 12 };
const fold = (s) => s.toLocaleLowerCase("tr").replace(/ü/g, "u").replace(/ş/g, "s").replace(/[^a-z]/g, "");
const HM = ["Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce"].map(fold);
const pad = (n) => String(n).padStart(2, "0");
const iso = (ms) => new Date(ms).toISOString().slice(0, 10);

const r = await fetch(URL);
if (!r.ok) throw new Error("HTTP " + r.status);
const html = await r.text();
const text = (s) => s.replace(/<[^>]+>/g, "").replace(/&nbsp;/g, " ").replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(+n)).trim();
const old = fs.existsSync(FILE) ? JSON.parse(fs.readFileSync(FILE, "utf8")) : { months: {} };
const months = { ...old.months };
let rows = 0;
for (const tr of html.match(/<tr[^>]*>[\s\S]*?<\/tr>/g) || []) {
  const td = [...tr.matchAll(/<td[^>]*>([\s\S]*?)<\/td>/g)].map((m) => text(m[1]));
  if (td.length < 2 || !/\d{4}/.test(td[0])) continue;
  const [d, mn, y] = td[0].split(/\s+/);
  const h = td[1].split(/\s+/);
  const hd = +h[0], hy = +h[h.length - 1], hm = HM.indexOf(fold(h.slice(1, -1).join(" "))) + 1;
  if (!AY[mn.toLocaleLowerCase("tr")] || !hm || !hd || !hy) throw new Error("Okunamayan satır: " + td.join(" | "));
  const start = iso(Date.UTC(+y, AY[mn.toLocaleLowerCase("tr")] - 1, +d) - (hd - 1) * 86400000);
  const key = hy + "-" + hm;
  if (months[key] && months[key] !== start) throw new Error(`${key}: kayıtlı ${months[key]}, Diyanet ${start}`);
  months[key] = start;
  rows++;
}
if (!rows) throw new Error("Tablo bulunamadı");
const sorted = Object.fromEntries(Object.entries(months).sort((a, b) => (a[1] < b[1] ? -1 : 1)));
const out = {
  source: "Diyanet İşleri Başkanlığı, namazvakitleri.diyanet.gov.tr (İstanbul vakit tablosu, Hicri Tarih sütunu)",
  note: "Anahtar: hicri yıl-ay (1 Muharrem … 12 Zilhicce); değer: o ayın 1. gününün gregoryen tarihi. scripts/fetch-hijri.mjs günceller.",
  fetched: new Date().toISOString().slice(0, 10),
  months: sorted,
};
fs.writeFileSync(FILE, JSON.stringify(out, null, 1) + "\n");
console.log(rows + " satır, " + Object.keys(sorted).length + " ay");
