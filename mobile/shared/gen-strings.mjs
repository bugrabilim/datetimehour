/* Uygulama metinleri ve verileri sitedeki tek kaynaktan üretilir (src/i18n/*.json, src/data/*.json);
   sitede karşılığı olmayan uygulama metinleri mobile/shared/app-strings.json'dadır.
   Çıktılar: iOS String Catalog (Localizable.xcstrings; NS/CF ile başlayan Info.plist anahtarları InfoPlist.xcstrings'e),
   Android metinleri (res/values-…/strings.xml; anahtardaki nokta ve tire "_" olur) ve veri dosyaları
   (il listesi, şehirler, takvim, dünya yerleri; iOS'ta App/Resources, Android'de assets).
   Kullanım: node mobile/shared/gen-strings.mjs          → dosyaları yazar
             node mobile/shared/gen-strings.mjs --check  → güncel değilse hata verir (CI) */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { computeYear } from "../../lib/events.mjs";

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
/* Sitedeki metinlerden aynı anahtarla alınanlar: "a.b.*" bir nesnenin ya da dizinin bütün öğeleri ("a.b.0" …). */
const SITE_PATHS = [
  "site.name", "client.format24", "client.format12", "client.seconds", "client.today", "client.tomorrow", "client.passed",
  "client.daysLeft", "client.weekShort", "client.diffSame", "client.diffAhead", "client.diffBehind", "client.hourShort", "client.minuteShort",
  "client.yourZone", "client.fullscreen", "client.exitFullscreen", "client.formatLabel",
  "prayer.monthHeading", "prayer.colDay", "prayer.prevMonth", "prayer.nextMonth", "prayer.useLocation",
  "prayer.imTitle", "prayer.imDay", "prayer.imDate", "prayer.imSahur", "prayer.imIftar", "prayer.imNote", "prayer.imFile", "prayer.imAlt",
  "calendar.months.*", "calendar.weekdays.*", "calendar.weekdaysShort.*", "calendar.types.*", "calendar.events.*", "calendar.noYear",
  "tools.hub.cards.stopwatch.name", "tools.hub.cards.countdown.name", "tools.hub.cards.alarm.name", "tools.hub.cards.pomodoro.name",
  "tools.hub.cards.datecalc.name", "tools.hub.cards.week.name", "tools.hub.cards.converter.name", "tools.hub.h1",
  "tools.stopwatch.*", "tools.countdown.*", "tools.alarm.*", "tools.alarm.dayNames.*", "tools.pomodoro.*",
  "datecalc.*", "datecalc.units.*",
  "info.week.big", "info.week.thisWeek",
  "world.h1", "world.addCity", "world.remove", "world.reset", "world.done", "world.customize",
  "converter.h1", "converter.whenLabel", "converter.now",
  "nature.h1", "nature.seasons.*", "nature.current", "nature.nextIn", "nature.startsOn", "nature.sunHeading", "nature.sunrise", "nature.sunset",
  "nature.dayLength", "nature.solarNoon", "nature.moonHeading", "nature.phases.*", "nature.illumination", "nature.age", "nature.nextNew",
  "nature.nextFull", "nature.tableHeading", "nature.tableLead", "nature.colYear", "nature.hoursShort", "nature.minutesShort",
  "nature.moonrise", "nature.moonset", "nature.moonNoRise", "nature.moonNoSet", "nature.moonCalHeading", "nature.eclipseHeading",
  "nature.eclipseLead", "nature.eclipseTypes.*", "nature.eclipseRegions", "nature.eclipsePath", "nature.eclipseHere", "nature.eclipseNotHere",
  "nature.eclipseSafety", "nature.eclipseSource",
  "weather.h1", "weather.now", "weather.feelsLike", "weather.humidity", "weather.wind", "weather.precipitation", "weather.uv",
  "weather.hourlyHeading", "weather.dailyHeading", "weather.loading", "weather.error", "weather.retry", "weather.pressure",
  "weather.codes.*", "weather.attribution", "weather.sunrise", "weather.sunset",
  "stage.models.*", "stage.prev", "stage.next", "stage.dots", "list.*",
];
const at = (obj, parts) => parts.reduce((o, k) => (o == null ? undefined : o[k]), obj);
for (const spec of SITE_PATHS) {
  const parts = spec.split(".");
  const wild = parts[parts.length - 1] === "*";
  const base = wild ? parts.slice(0, -1) : parts;
  const leafKeys = wild ? Object.keys(at(I.tr, base) || {}) : [null];
  if (wild && !leafKeys.length) throw new Error(`${spec}: sitede yok`);
  for (const leaf of leafKeys) {
    const p = leaf === null ? base : [...base, leaf];
    if (typeof at(I.tr, p) !== "string") continue; // iç içe nesneler ve SSS dizileri alınmaz
    const key = p.join(".");
    if (!KEYS[key]) KEYS[key] = (t) => at(t, p);
  }
}

const APP = JSON.parse(fs.readFileSync(path.join(HERE, "app-strings.json"), "utf8"));
for (const [key, byLang] of Object.entries(APP)) {
  if (key === "_") continue;
  if (KEYS[key]) throw new Error(`${key} hem sitede hem app-strings.json'da`);
  KEYS[key] = (t, l) => byLang[l];
}

/* {ad} → %1$@, %2$@…: sıra Türkçe metindeki ilk görünüş sırasıdır; her dil aynı adları kullanmalı. */
const names = (s) => [...new Set([...s.matchAll(/\{([a-z]+)\}/gi)].map((m) => m[1]))];
const toIOS = (s, order) => s.replace(/%/g, "%%").replace(/\{([a-z]+)\}/gi, (_, n) => `%${order.indexOf(n) + 1}$@`);

/* Android: {ad} → %1$s; yer tutucusu olmayan metinde % olduğu gibi kalır (formatted="false") */
const ANDROID_DIRS = { tr: "values", en: "values-en", de: "values-de", az: "values-az", ar: "values-ar" };
const androidName = (key) => key.replace(/[^A-Za-z0-9_]/g, "_");
const androidEscape = (s) => s
  .replace(/\\/g, "\\\\").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
  .replace(/'/g, "\\'").replace(/"/g, '\\"').replace(/\n/g, "\\n").replace(/^([@?])/, "\\$1");
const androidRes = Object.fromEntries(LANGS.map((l) => [l, []]));
const androidNames = new Map();

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
  if (/^(NS|CF)/.test(key)) continue;
  const name = androidName(key);
  if (androidNames.has(name)) throw new Error(`Android adı çakışıyor: ${key} / ${androidNames.get(name)}`);
  androidNames.set(name, key);
  for (const l of LANGS) {
    const v = get(I[l], l);
    if (order.length) {
      const body = androidEscape(v.replace(/%/g, "%%").replace(/\{([a-z]+)\}/gi, (_, n) => `%${order.indexOf(n) + 1}$s`));
      androidRes[l].push(`    <string name="${name}">${body}</string>`);
    } else {
      const attr = v.includes("%") ? ' formatted="false"' : "";
      androidRes[l].push(`    <string name="${name}"${attr}>${androidEscape(v)}</string>`);
    }
  }
}
// Uygulama adı (ana ekran simgesinin altında, dile göre)
for (const l of LANGS) androidRes[l].push(`    <string name="app_name">${androidEscape(APP["app.displayName"][l])}</string>`);
const androidXml = (l) => '<?xml version="1.0" encoding="utf-8"?>\n<!-- gen-strings.mjs üretir; elle düzenlemeyin -->\n<resources>\n' +
  androidRes[l].join("\n") + "\n</resources>\n";
const localesConfig = '<?xml version="1.0" encoding="utf-8"?>\n<locale-config xmlns:android="http://schemas.android.com/apk/res/android">\n' +
  LANGS.map((l) => `    <locale android:name="${l}" />`).join("\n") + "\n</locale-config>\n";
const catalog = (s) => JSON.stringify({ sourceLanguage: "tr", strings: s, version: "1.0" }, null, 2) + "\n";
const xcstrings = catalog(strings);

const provinces = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/provinces.json"), "utf8"))
  .map(({ slug, name, lat, lon, ar }) => ({ slug, name, lat, lon, ar }));
const provincesJson = JSON.stringify(provinces) + "\n";

/* Takvim: resmî tatiller, arifeler, dini ve özel günler, okul takvimi (sitenin takvim sayfalarıyla aynı kaynak; sınavlar takvimde yok). */
const HOL = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/holidays.json"), "utf8"));
HOL.school = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/school.json"), "utf8"));
const years = {};
for (const y of Object.keys(HOL.years).map(Number).sort((a, b) => a - b)) {
  const r = computeYear(y, HOL);
  years[y] = { events: r.events.filter((e) => e.type !== "exam"), schoolRanges: r.schoolRanges, projected: r.projected };
  for (const e of years[y].events) if (!KEYS[`calendar.events.${e.key}`]) throw new Error(`calendar.events.${e.key} metni yok`);
}
const calendarJson = JSON.stringify({ years, ramadans: HOL.ramadans }) + "\n";

/* Dünya saatleri için şehirler: ad ve ülke beş dilde, IANA saat dilimi, koordinat */
const cities = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/cities.json"), "utf8")).map((c) => ({
  key: c.key, tz: c.tz, lat: c.lat, lon: c.lon,
  name: Object.fromEntries(LANGS.map((l) => [l, c[l].name])), country: Object.fromEntries(LANGS.map((l) => [l, c[l].country])),
}));
const citiesJson = JSON.stringify(cities) + "\n";

/* Küre yakınlaşınca gösterilen dünya yerleri (sitedeki assets/places*.json ile aynı kaynak; az adları İngilizce) */
const placesSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/places.json"), "utf8"));
const placesJson = JSON.stringify({ tz: placesSrc.tz, p: placesSrc.p, names: placesSrc.names }) + "\n";

/* Tutulmalar (NASA tablosu, src/data/eclipses.json): en büyük tutulma anı TD → UT (ΔT 69 sn, nature-calc.js ile aynı) */
const eclSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/eclipses.json"), "utf8"));
const eclipsesJson = JSON.stringify(["solar", "lunar"].flatMap((kind) => eclSrc[kind].map((e) => ({ kind, ut: Date.parse(e.td) - 69000, type: e.type, regions: e.regions, path: e.path || [] }))).sort((a, b) => a.ut - b.ut)) + "\n";

const files = {
  "mobile/ios/App/Resources/Localizable.xcstrings": xcstrings,
  "mobile/ios/App/Resources/InfoPlist.xcstrings": catalog(plistStrings),
  "mobile/ios/App/Resources/provinces.json": provincesJson,
  "mobile/ios/App/Resources/calendar.json": calendarJson,
  "mobile/ios/App/Resources/cities.json": citiesJson,
  "mobile/ios/App/Resources/places.json": placesJson,
  "mobile/ios/App/Resources/eclipses.json": eclipsesJson,
  "mobile/android/app/src/main/assets/provinces.json": provincesJson,
  "mobile/android/app/src/main/assets/calendar.json": calendarJson,
  "mobile/android/app/src/main/assets/cities.json": citiesJson,
  "mobile/android/app/src/main/assets/places.json": placesJson,
  "mobile/android/app/src/main/assets/eclipses.json": eclipsesJson,
  "mobile/android/app/src/main/res/xml/locales_config.xml": localesConfig,
  ...Object.fromEntries(LANGS.map((l) => [`mobile/android/app/src/main/res/${ANDROID_DIRS[l]}/strings.xml`, androidXml(l)])),
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
