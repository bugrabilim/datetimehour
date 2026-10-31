#!/usr/bin/env node
// Tarayıcı eklentisinin (extension/) üretilen dosyalarını yazar: lib/prayer-calc.js (sitedeki hesabın kopyası),
// lib/data.js (81 il, hicri ay başları tablosu, sitenin dillerinde metinler) ve _locales/<dil>/messages.json (ad ve açıklama).
// Elle yazılanlar: manifest.json, popup.html/css/js, strings.json, icons/. Kullanım: node scripts/build-extension.mjs [--check]
// --zip: dist-extension/saat-tarih-<sürüm>.zip (Chrome Web Store ve Firefox Add-ons'a yüklenecek paket; zip komutu gerekir).
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { hijriTable } from "../lib/hijri.mjs";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const EXT = path.join(ROOT, "extension");
const LANGS = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th"];
// Chrome'un desteklediği _locales klasörleri (Azerice ve Urduca listede yok; o arayüzler İngilizce ad ve açıklamayı görür).
// Çince klasörü zh_CN (Basitleştirilmiş).
const CHROME_LOCALES = ["tr", "en", "de", "ar", "id", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th"];
const CHROME_DIR = (l) => (l === "zh" ? "zh_CN" : l);
const read = (p) => JSON.parse(fs.readFileSync(path.join(ROOT, p), "utf8"));
const check = process.argv.includes("--check");

const own = read("extension/strings.json");
const app = read("mobile/shared/app-strings.json");
const provinces = read("src/data/provinces.json").map((p) => [p.slug, p.name, p.lat, p.lon, p.ar || "", p.ru || "", p.fa || "", p.ja || "", p.ko || "", p.zh || "", p.th || ""]);
const hijri = hijriTable(read("src/data/holidays.json"), read("src/data/hijri.json"));
const strings = Object.fromEntries(LANGS.map((l) => {
  const site = read(`src/i18n/${l}.json`);
  return [l, {
    province: site.prayer.chooseLabel, today: site.prayer.todayHeading, nextIn: site.prayer.nextIn, names: site.prayer.names,
    hijriFmt: app["hijri.fmt"][l], hijriMonths: Array.from({ length: 12 }, (_, i) => app[`hijri.month.${i + 1}`][l]),
    open: own.open[l], format: own.format[l], note: own.note[l],
  }];
}));
for (const [l, s] of Object.entries(strings)) for (const [k, v] of Object.entries(s)) if (v == null || v === "") throw new Error(`metin yok: ${l}.${k}`);

const files = {
  "lib/prayer-calc.js": fs.readFileSync(path.join(ROOT, "src/js/prayer-calc.js"), "utf8"),
  "lib/data.js": `/* scripts/build-extension.mjs ile üretilir; elle değiştirmeyin. İller: src/data/provinces.json; hicri tablo: lib/hijri.mjs */\nwindow.STH_DATA = ${JSON.stringify({ provinces, hijri, strings })};\n`,
};
for (const l of CHROME_LOCALES) {
  files[`_locales/${CHROME_DIR(l)}/messages.json`] = JSON.stringify({
    name: { message: own.name[l] }, description: { message: own.description[l] },
  }, null, 2) + "\n";
}
for (const [l, v] of Object.entries(own.name)) if (v.length > 75) throw new Error(`ad 75 karakterden uzun: ${l}`);
for (const [l, v] of Object.entries(own.description)) if (v.length > 132) throw new Error(`açıklama 132 karakterden uzun: ${l}`);

let stale = 0;
for (const [rel, body] of Object.entries(files)) {
  const p = path.join(EXT, rel);
  if (check) {
    if (!fs.existsSync(p) || fs.readFileSync(p, "utf8") !== body) { console.error(`Eski: extension/${rel}`); stale++; }
  } else {
    fs.mkdirSync(path.dirname(p), { recursive: true });
    fs.writeFileSync(p, body);
  }
}
if (check) {
  if (stale) { console.error("node scripts/build-extension.mjs ile yeniden üretin"); process.exit(1); }
  console.log("Eklenti dosyaları güncel.");
} else {
  console.log(`Yazıldı: ${Object.keys(files).length} dosya (extension/)`);
}
if (process.argv.includes("--zip")) {
  const version = JSON.parse(fs.readFileSync(path.join(EXT, "manifest.json"), "utf8")).version;
  const out = path.join(ROOT, "dist-extension", `saat-tarih-${version}.zip`);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.rmSync(out, { force: true });
  execFileSync("zip", ["-r", "-X", out, "manifest.json", "popup.html", "popup.css", "popup.js", "lib", "_locales", "icons"], { cwd: EXT, stdio: "inherit" });
  console.log(`Paket: ${path.relative(ROOT, out)}`);
}
