/* Uygulama metinleri ve verileri sitedeki tek kaynaktan üretilir (src/i18n/*.json, src/data/*.json);
   sitede karşılığı olmayan uygulama metinleri mobile/shared/app-strings.json'dadır.
   Çıktılar: iOS String Catalog (Localizable.xcstrings; NS/CF ile başlayan Info.plist anahtarları InfoPlist.xcstrings'e)
   ve il listesi (provinces.json).
   Kullanım: node mobile/shared/gen-strings.mjs          → dosyaları yazar
             node mobile/shared/gen-strings.mjs --check  → güncel değilse hata verir (CI) */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "../..");
const LANGS = ["tr", "en", "de", "az", "ar"];
const I = Object.fromEntries(LANGS.map((l) => [l, JSON.parse(fs.readFileSync(path.join(ROOT, "src/i18n", `${l}.json`), "utf8"))]));

/* Uygulama anahtarı → sitedeki metin. {ad} yer tutucuları iOS biçimine (%@) çevrilir. */
const KEYS = {
  "prayer.fajr": (t) => t.prayer.names.fajr,
  "prayer.sunrise": (t) => t.prayer.names.sunrise,
  "prayer.dhuhr": (t) => t.prayer.names.dhuhr,
  "prayer.asr": (t) => t.prayer.names.asr,
  "prayer.maghrib": (t) => t.prayer.names.maghrib,
  "prayer.isha": (t) => t.prayer.names.isha,
  "prayer.nextIn": (t) => t.prayer.nextIn,
  "prayer.today": (t) => t.prayer.todayHeading,
  "prayer.province": (t) => t.prayer.chooseLabel,
  "brand.product": (t) => t.badge.alt,
};
const APP = JSON.parse(fs.readFileSync(path.join(HERE, "app-strings.json"), "utf8"));
for (const [key, byLang] of Object.entries(APP)) {
  if (key === "_") continue;
  if (KEYS[key]) throw new Error(`${key} hem sitede hem app-strings.json'da`);
  KEYS[key] = (t, l) => byLang[l];
}

/* {ad} → %1$@, %2$@…: sıra Türkçe metindeki ilk görünüş sırasıdır; her dil aynı adları kullanmalı. */
const names = (s) => [...new Set([...s.matchAll(/\{([a-z]+)\}/gi)].map((m) => m[1]))];
const toIOS = (s, order) => s.replace(/\{([a-z]+)\}/gi, (_, n) => `%${order.indexOf(n) + 1}$@`);

const strings = {}, plistStrings = {};
for (const [key, get] of Object.entries(KEYS)) {
  const localizations = {};
  const order = names(get(I.tr, "tr") ?? "");
  for (const l of LANGS) {
    const v = get(I[l], l);
    if (typeof v !== "string" || !v) throw new Error(`${l}: ${key} metni yok`);
    if (names(v).sort().join() !== [...order].sort().join()) throw new Error(`${l}: ${key} yer tutucuları Türkçeyle aynı değil`);
    localizations[l] = { stringUnit: { state: "translated", value: toIOS(v, order) } };
  }
  (/^(NS|CF)/.test(key) ? plistStrings : strings)[key] = { extractionState: "manual", localizations };
}
const catalog = (s) => JSON.stringify({ sourceLanguage: "tr", strings: s, version: "1.0" }, null, 2) + "\n";
const xcstrings = catalog(strings);

const provinces = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/provinces.json"), "utf8"))
  .map(({ slug, name, lat, lon, ar }) => ({ slug, name, lat, lon, ar }));
const provincesJson = JSON.stringify(provinces) + "\n";

const files = {
  "mobile/ios/App/Resources/Localizable.xcstrings": xcstrings,
  "mobile/ios/App/Resources/InfoPlist.xcstrings": catalog(plistStrings),
  "mobile/ios/App/Resources/provinces.json": provincesJson,
};
const check = process.argv.includes("--check");
const stale = [];
for (const [rel, body] of Object.entries(files)) {
  const file = path.join(ROOT, rel);
  if (check) {
    if (!fs.existsSync(file) || fs.readFileSync(file, "utf8") !== body) stale.push(rel);
  } else {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, body);
  }
}
if (stale.length) {
  console.error(`Güncel değil: ${stale.join(", ")}. Çalıştırın: node mobile/shared/gen-strings.mjs`);
  process.exit(1);
}
console.log(check ? "Uygulama metinleri güncel." : `Yazıldı: ${Object.keys(files).join(", ")}`);
