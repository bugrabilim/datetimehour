/* Uygulama metinleri ve verileri sitedeki tek kaynaktan üretilir (src/i18n/*.json, src/data/*.json);
   sitede karşılığı olmayan uygulama metinleri mobile/shared/app-strings.json'dadır.
   Çıktılar: iOS String Catalog (Localizable.xcstrings; NS/CF ile başlayan Info.plist anahtarları InfoPlist.xcstrings'e),
   Android metinleri (res/values-…/strings.xml; anahtardaki nokta ve tire "_" olur) ve veri dosyaları
   (il listesi, şehirler, takvim, hicri ay başları, dünya yerleri; iOS'ta App/Resources, Android'de assets).
   Kullanım: node mobile/shared/gen-strings.mjs          → dosyaları yazar
             node mobile/shared/gen-strings.mjs --check  → güncel değilse hata verir (CI) */
import fs from "node:fs";
import { skyEvents } from "../../lib/sky-events.mjs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { computeYear } from "../../lib/events.mjs";
import { bridges } from "../../lib/bridges.mjs";
import { hijriTable } from "../../lib/hijri.mjs";
import { otdDay } from "../../lib/otd.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "../..");
const LANGS = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th", "es", "pt", "hi"];
/* iOS String Catalog dil kodları: Çince Basitleştirilmiş Çince (zh-Hans); diğerleri aynı */
const IOS_CODE = (l) => (l === "zh" ? "zh-Hans" : l);
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
  // 2. analiz #106, #108, #110 (3 Kasım 2026): ölçüm açıklamaları, günlük özet, yarın uyarısı, yeni ölçümler
  "weather.wd.*", "weather.uvw.*", "weather.hw.*", "weather.vw.*", "weather.dw.*", "weather.fw.*", "weather.sum.*", "weather.alert.*",
  "weather.visibility", "weather.dewPoint", "weather.gusts", "weather.details", "weather.compass.*", "weather.windFrom",
  // 2. analiz #147: Ay uzaklığı, yerberi/yeröte, süper Ay (sitedeki gece gökyüzü metinleri)
  "sky.moonHeading", "sky.moonNow", "sky.perigee", "sky.apogee", "sky.km", "sky.superMoon",
  // Gece gökyüzü uygulamada (2. analiz #154–#157, #159; 3 Kasım 2026)
  "sky.h1", "sky.planets.*", "sky.moon", "sky.sun", "sky.chartHeading", "sky.timeLabel", "sky.now", "sky.tableHeading", "sky.window",
  "sky.tooClose", "sky.below", "sky.riseSet", "sky.altFmt", "sky.mkm", "sky.note", "sky.eventsHeading", "sky.eventsLead", "sky.showers.*",
  "sky.evShower", "sky.showerRate", "sky.showerVar", "sky.moonLight", "sky.evOpp", "sky.evElongE", "sky.evElongW", "sky.evConj", "sky.evMoon",
  "sky.evMore", "sky.eventsNote", "sky.tonightHeading", "sky.darkLabel", "sky.darkNone", "sky.moonUpDark", "sky.moonDownDark", "sky.planetsLabel",
  "sky.planetsNone", "sky.cloudLabel", "sky.score.*", "sky.tipMoon", "sky.tipDark", "sky.tipCloudy", "sky.tipBest", "sky.graphHeading",
  "sky.graphCaption", "sky.constellations", "sky.play", "sky.pause", "sky.dateLabel", "sky.red", "sky.conSource",
  "nature.eclLocal", "nature.eclLocalTotal", "nature.eclLocalAnnular", "nature.eclLocalNone", "nature.eclLocalLow", "nature.eclContacts.*",
  "nature.eclMag", "nature.eclApprox", "nature.eclMapHeading", "nature.eclMapSolar", "nature.eclMapLunar", "nature.eclMapNote",
  "stage.models.*", "stage.prev", "stage.next", "stage.dots", "list.*",
  // 9. tur (30 Ekim 2026): kerahat, altın/mavi saat, fitre, hicri çevirici, pomodoro etiketleri
  "prayer.kerahatHeading", "prayer.kerahatNames.*", "prayer.kerahatNote",
  // 2. analiz #56, #59 (3 Kasım 2026): bayram namazı, ek vakitler
  "prayer.extraHeading", "prayer.extraNames.*", "prayer.extraNote", "prayer.bayram", "prayer.bayramTomorrow", "prayer.bayramNote",
  // 2. analiz #60: halk takvimi
  "nature.folkHeading", "nature.folkNote", "nature.folkNow", "nature.folkNames.*",
  "nature.goldenAm", "nature.goldenPm", "nature.blueAm", "nature.bluePm", "nature.lightNote",
  // 2. analiz #151, #148, #161 (3 Kasım 2026): alacakaranlık, yılın dolunayları ve adları
  "nature.tw.*", "nature.ym.*", "nature.ym.names.*",
  // 2. analiz #121, #120: ezani saat, Rumi tarih
  "prayer.ezani", "prayer.ezaniNote", "hijriConv.rumi", "hijriConv.rumiNote",
  // 2. analiz #124: takvim yaprağında tarihte bugün
  "otd.h1", "otd.foundedFmt",
  // 2. analiz #125: ay adlarının kökeni (takvim yaprağı)
  "calendar.monthOrigin.heading", "calendar.monthOrigin.source", "calendar.monthOrigin.items.*",
  "zakat.fitreHeading", "zakat.fitreText", "zakat.fitrePeople", "zakat.fitreTotal", "zakat.fitreNote",
  "hijriConv.h1", "hijriConv.toHijri", "hijriConv.toGreg", "hijriConv.gregLabel", "hijriConv.yearLabel", "hijriConv.monthLabel",
  "hijriConv.dayLabel", "hijriConv.dayMax",
  "tools.pomodoro.tags.*",
  // O maddeleri (30 Ekim 2026): uygulamada toplantı planlayıcı
  "planner.h1", "planner.addLabel", "planner.dateLabel", "planner.remove", "planner.legendWork", "planner.legendEdge",
  "planner.legendNight", "planner.common", "planner.noCommon", "planner.minCities", "planner.meeting", "planner.picked",
  // O maddeleri (30 Ekim 2026): hesap yöntemleri, hilal görünürlüğü
  "prayer.method.*", "prayer.method.names.*",
  "nature.crescentHeading", "nature.crescentLead", "nature.crescentAge", "nature.crescentLag", "nature.crescentBefore", "nature.crescentCats.*", "nature.crescentNote",
  // Sitedeki dokuz araç uygulamada (4 Kasım 2026, sahibin isteği): satranç saati, metronom, konuşan saat, burç, saat hesaplama,
  // köprü tatil, sınav zamanlayıcısı, sınıf araçları (emeklilik aşağıda, yalnız Türkçe)
  "chess.h1", "chess.control", "chess.custom", "chess.minutes", "chess.increment", "chess.white", "chess.black", "chess.pause",
  "chess.resume", "chess.reset", "chess.moves", "chess.flag",
  "metronome.h1", "metronome.bpm", "metronome.beats", "metronome.sub", "metronome.subs.*", "metronome.accent", "metronome.tap",
  "metronome.start", "metronome.stop", "metronome.slower", "metronome.faster", "metronome.volume", "metronome.tempoNote",
  "talking.h1", "talking.say", "talking.auto", "talking.every.*", "talking.chime",
  "zodiac.h1", "zodiac.dateLabel", "zodiac.timeLabel", "zodiac.sun", "zodiac.moon", "zodiac.noTime", "zodiac.cusp", "zodiac.tzNote",
  "zodiac.tableHeading", "zodiac.fun",
  "timeCalc.h1", "timeCalc.betweenHeading", "timeCalc.startLabel", "timeCalc.endLabel", "timeCalc.overnight", "timeCalc.duration",
  "timeCalc.decimal", "timeCalc.minutesTotal", "timeCalc.addHeading", "timeCalc.timeLabel", "timeCalc.opAdd", "timeCalc.opSub",
  "timeCalc.hoursLabel", "timeCalc.minutesLabel", "timeCalc.resultTime", "timeCalc.nextDay", "timeCalc.prevDay", "timeCalc.daysLater",
  "timeCalc.daysEarlier", "timeCalc.sumHeading", "timeCalc.sumLabel", "timeCalc.sumHint", "timeCalc.sumResult", "timeCalc.sumBad",
  "bridge.h1", "bridge.yearLabel", "bridge.maxLabel", "bridge.maxAll", "bridge.maxN", "bridge.noLeave", "bridge.leaveN", "bridge.totalN",
  "bridge.halfNote", "bridge.none", "bridge.best", "bridge.groups.*", "bridge.note", "bridge.projected",
  "examTimer.h1", "examTimer.nameLabel", "examTimer.namePh", "examTimer.roomLabel", "examTimer.roomPh", "examTimer.readLabel",
  "examTimer.durLabel", "examTimer.start", "examTimer.pause", "examTimer.resume", "examTimer.reset", "examTimer.phaseRead",
  "examTimer.phaseExam", "examTimer.phaseDone", "examTimer.endsAt", "examTimer.warn15", "examTimer.warn5",
  "classroom.h1", "classroom.tabPicker", "classroom.tabTeams", "classroom.tabCounter", "classroom.namesLabel", "classroom.namesPh",
  "classroom.pick", "classroom.removePicked", "classroom.picked", "classroom.left", "classroom.restore", "classroom.empty",
  "classroom.teamCount", "classroom.makeTeams", "classroom.team", "classroom.counterName", "classroom.addCounter",
  "classroom.counterDefault", "classroom.reset", "classroom.remove",
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

/* Uygulamalarda ay adları yalnız başlıkta (takvim, aylık çizelge) kullanılır: yalın hâl (calendar.monthsTitle) varsa o
   (Rusçada calendar.months tarih içindeki tamlama hâlidir: "31 октября"; başlık "Октябрь"). */
for (let i = 0; i < 12; i++) KEYS[`calendar.months.${i}`] = (t) => (t.calendar.monthsTitle || t.calendar.months)[i];
for (let i = 0; i < 12; i++) KEYS[`zodiac.signs.${i}`] = (t) => t.zodiac.signs[i];

/* Emeklilik hesaplama yalnız Türkçe (src/data/retire-tr.json; 4/a sigortalıları, Türkiye mevzuatı). Uygulama bu aracı yalnız
   Türkçe arayüzde gösterir; anahtarlar her dilde aynı Türkçe metinle yazılır ki kaynak dosyaları eşit kalsın. */
const RETIRE = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/retire-tr.json"), "utf8"));
for (const k of ["h1", "gender", "women", "men", "birth", "start", "days", "daysHint", "rate", "rateFull", "rateHalf", "rateNone", "calc",
  "result", "resultAge", "resultLeft", "resultPast", "resultNever", "group", "condDays", "condYears", "condAge", "condNoAge", "done",
  "never", "years", "months", "noteScope", "noteLaw"]) KEYS[`retire.${k}`] = () => RETIRE[k];
for (const sub of ["groups", "routes"]) for (const k of Object.keys(RETIRE[sub])) KEYS[`retire.${sub}.${k}`] = () => RETIRE[sub][k];

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
/* Endonezce Android kaynaklarında eski "in" koduyla (values-in) durur; Filipince üç harfli kod BCP 47 biçimiyle (values-b+fil);
   Çince Basitleştirilmiş (values-zh bütün Çince yerel ayarlara uyar, Geleneksel Çincede de Türkçe yerine bu gelir).
   Dil seçimi (locales_config) BCP 47 kodlarıyla: id, fil, zh-Hans. */
const ANDROID_DIRS = { tr: "values", en: "values-en", de: "values-de", az: "values-az", ar: "values-ar", id: "values-in", ur: "values-ur", fa: "values-fa", fr: "values-fr", ru: "values-ru", ja: "values-ja", ko: "values-ko", zh: "values-zh", fil: "values-b+fil", th: "values-th", es: "values-es", pt: "values-pt", hi: "values-hi" };
const LOCALE_TAG = (l) => (l === "zh" ? "zh-Hans" : l);
const androidName = (key) => key.replace(/[^A-Za-z0-9_]/g, "_");
const androidEscape = (s) => s
  .replace(/\\/g, "\\\\").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
  .replace(/'/g, "\\'").replace(/"/g, '\\"').replace(/\n/g, "\\n").replace(/^([@?])/, "\\$1");
const androidRes = Object.fromEntries(LANGS.map((l) => [l, []]));
/* Wear OS saati: yalnız saat ekranlarının kullandığı metinler (mobile/android/wear) */
const WEAR_KEYS = /^(prayer\.|hijri\.|wear\.|watch\.|desk\.(pomoDone|breakDone)$|nature\.phases\.|widget\.(next|hijri|weather|moon)\.|widget\.province\.auto$|live\.(iftar|sahur)$|place\.myLocation$|client\.(hourShort|minuteShort)$|weather\.codes\.|weather\.attribution$)/;
const wearRes = Object.fromEntries(LANGS.map((l) => [l, []]));
const androidNames = new Map();

/* Siri ifadeleri (iOS App Shortcuts): "siri.phrase.*" yalnız AppShortcuts.xcstrings'e gider; anahtar İngilizce ifadedir,
   {app} → ${applicationName}. Kısa adlar (siri.short.*) iki katalogda da bulunur. Her İngilizce ifade
   App/Sources/AppIntents.swift'te birebir (\(.applicationName) ile) yazılı olmalı. */
const shortcutStrings = {};
const intentsSwift = fs.readFileSync(path.join(ROOT, "mobile/ios/App/Sources/AppIntents.swift"), "utf8");
for (const [key, get] of Object.entries(KEYS)) {
  if (!/^siri\.(phrase|short)\./.test(key)) continue;
  const phrase = key.startsWith("siri.phrase.");
  const conv = (v) => (phrase ? v.replace("{app}", "${applicationName}") : v);
  const en = conv(get(I.en, "en"));
  if (phrase && !intentsSwift.includes(`"${get(I.en, "en").replace("{app}", "\\(.applicationName)")}"`)) throw new Error(`AppIntents.swift'te ifade yok: ${en}`);
  const localizations = {};
  for (const l of LANGS) {
    const v = get(I[l], l);
    if (phrase && !v.includes("{app}")) throw new Error(`${l}: ${key} {app} içermeli`);
    localizations[IOS_CODE(l)] = { stringUnit: { state: "translated", value: conv(v) } };
  }
  shortcutStrings[phrase ? en : key] = { extractionState: "manual", localizations };
}
for (const key of Object.keys(KEYS)) if (key.startsWith("siri.phrase.")) delete KEYS[key];

const strings = {}, plistStrings = {};
for (const [key, get] of Object.entries(KEYS)) {
  const localizations = {};
  const order = names(get(I.tr, "tr") ?? "");
  for (const l of LANGS) {
    const v = get(I[l], l);
    if (typeof v !== "string" || !v) throw new Error(`${l}: ${key} metni yok`);
    // Rakamlar her dilde Latin (CLAUDE.md › Diller); Farsça/Urduca/Arapça rakam yazılmaz
    if (/[\u0660-\u0669\u06F0-\u06F9]/.test(v)) throw new Error(`${l}: ${key} Latin olmayan rakam içeriyor`);
    if (names(v).sort().join() !== [...order].sort().join()) throw new Error(`${l}: ${key} yer tutucuları Türkçeyle aynı değil`);
    localizations[IOS_CODE(l)] = { stringUnit: { state: "translated", value: toIOS(v, order) } };
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
    if (WEAR_KEYS.test(key)) wearRes[l].push(androidRes[l][androidRes[l].length - 1]);
  }
}
// Uygulama adı (ana ekran simgesinin altında, dile göre)
for (const l of LANGS) androidRes[l].push(`    <string name="app_name">${androidEscape(APP["app.displayName"][l])}</string>`);
for (const l of LANGS) wearRes[l].push(androidRes[l][androidRes[l].length - 1]);
const androidXml = (l, res = androidRes) => '<?xml version="1.0" encoding="utf-8"?>\n<!-- gen-strings.mjs üretir; elle düzenlemeyin -->\n<resources>\n' +
  res[l].join("\n") + "\n</resources>\n";
const localesConfig = '<?xml version="1.0" encoding="utf-8"?>\n<locale-config xmlns:android="http://schemas.android.com/apk/res/android">\n' +
  LANGS.map((l) => `    <locale android:name="${LOCALE_TAG(l)}" />`).join("\n") + "\n</locale-config>\n";
const catalog = (s, source = "tr") => JSON.stringify({ sourceLanguage: source, strings: s, version: "1.0" }, null, 2) + "\n";
const xcstrings = catalog(strings);

const provinces = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/provinces.json"), "utf8"))
  .map(({ slug, name, lat, lon, ar, ru, fa, ja, ko, zh, th, hi }) => ({ slug, name, lat, lon, ar, ru, fa, ja, ko, zh, th, hi }));
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

/* Köprü tatil planlayıcı (sitedeki /kopru-tatil/ ile aynı hesap, lib/bridges.mjs): yıl → dönemler; "best" en verimli dönem
   (en az 7 günlük dönemler arasında gün/izin oranı en yüksek). Grup adları: bridge.groups.* ya da calendar.events.* */
const bridgesJson = JSON.stringify(Object.fromEntries(Object.keys(years).map((y) => {
  y = +y;
  const ev = [y - 1, y, y + 1].flatMap((x) => computeYear(x, HOL).events).filter((e) => e.type === "holiday" || e.type === "half");
  const list = bridges(y, ev);
  const paid = list.filter((b) => b.leave > 0);
  const pool = paid.filter((b) => b.days >= 7).length ? paid.filter((b) => b.days >= 7) : paid;
  const best = pool.length ? pool.reduce((a, b) => (b.days / b.leave > a.days / a.leave ? b : a)) : null;
  return [y, { projected: !!years[y].projected, list: list.map((b) => ({ g: b.groups, from: b.from, to: b.to, days: b.days, leave: b.leave,
    leaveDays: b.leaveDays, halfDays: b.halfDays, best: b === best })) }];
}))) + "\n";

/* Hicri takvim: Diyanet'e hizalı ay başları (lib/hijri.mjs): [[ay başı, hicri yıl, ay], …]; son satır tablonun sonu */
const hijriJson = JSON.stringify(hijriTable(HOL, JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/hijri.json"), "utf8")))) + "\n";

/* Fitre (src/data/fitre.json): en son açıklanan yıl, tutar (TL) ve açıklama tarihi */
const fitreSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/fitre.json"), "utf8")).years;
const fitreYear = Object.keys(fitreSrc).sort().pop();
const fitreJson = JSON.stringify({ year: +fitreYear, amount: fitreSrc[fitreYear].amount, date: fitreSrc[fitreYear].date }) + "\n";

/* Dünya saatleri için şehirler: ad ve ülke beş dilde, IANA saat dilimi, koordinat */
const cities = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/cities.json"), "utf8")).map((c) => ({
  key: c.key, tz: c.tz, lat: c.lat, lon: c.lon,
  name: Object.fromEntries(LANGS.map((l) => [l, c[l].name])), country: Object.fromEntries(LANGS.map((l) => [l, c[l].country])),
}));
const citiesJson = JSON.stringify(cities) + "\n";

/* Küre yakınlaşınca gösterilen dünya yerleri (sitedeki assets/places*.json ile aynı kaynak; az adları İngilizce) */
const placesSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/places.json"), "utf8"));
const placesJson = JSON.stringify({ tz: placesSrc.tz, p: placesSrc.p, names: placesSrc.names }) + "\n";

/* İlçeler (src/data/districts.json, GeoNames CC BY 4.0): il → [ad, enlem, boylam] (ilçe merkezi) */
const districtsJson = JSON.stringify(JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/districts.json"), "utf8")).provinces) + "\n";

/* Tutulmalar (NASA tablosu, src/data/eclipses.json): en büyük tutulma anı TD → UT (ΔT 69 sn, nature-calc.js ile aynı) */
const eclSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/eclipses.json"), "utf8"));
const eclipsesJson = JSON.stringify(["solar", "lunar"].flatMap((kind) => eclSrc[kind].map((e) => ({ kind, ut: Date.parse(e.td) - 69000, type: e.type, regions: e.regions, path: e.path || [] }))).sort((a, b) => a.ut - b.ut)) + "\n";

/* Tarihte bugün (2. analiz #124; Wikidata CC0, src/data/on-this-day.json; sitedeki seçim kuralı lib/otd.mjs): uygulamada
   takvim yaprağında gösterilir; boyut için gün başına olay 5, kuruluş 1, doğum 3, ölüm 2; {"AA-GG": [[yıl, tür, ad, açıklama]]} */
const otdSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/on-this-day.json"), "utf8"));
const otdJson = (lang) => JSON.stringify(Object.fromEntries(Object.keys(otdSrc.days).sort()
  .map((md) => [md, otdDay(otdSrc, lang, md, { e: 5, f: 1, b: 3, d: 2 }).map((r) => r.slice(0, 4))]))) + "\n";
const otdFiles = Object.fromEntries(LANGS.flatMap((l) => {
  const j = otdJson(l);
  return [[`mobile/ios/App/Resources/otd-${l}.json`, j], [`mobile/android/app/src/main/assets/otd-${l}.json`, j]];
}));

/* Dünya resmî tatilleri (2. analiz #122; src/data/world-holidays.json, Nager.Date MIT): takvimde ülke seçimi. Ad tablosu
   bir kez: "s" [ad], ülke → yıl → [[tarih, İngilizce ad sırası, yerel ad sırası]]; "tx" İngilizce ad sırası → {dil: çeviri}
   (src/data/holiday-names.json); "native" sitedeki NATIVE_CC (o dilde yerel ad gösterilir) */
const wholSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/world-holidays.json"), "utf8"));
const holNamesSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/holiday-names.json"), "utf8")).names;
const wholJson = (() => {
  const strs = [], idx = new Map();
  const id = (t) => { if (!idx.has(t)) { idx.set(t, strs.length); strs.push(t); } return idx.get(t); };
  const countries = {};
  for (const [cc, years] of Object.entries(wholSrc.countries)) {
    countries[cc] = {};
    for (const [y, v] of Object.entries(years)) countries[cc][y] = v.days.map((h) => [h.d, id(h.n), id(h.l || h.n)]);
  }
  const tx = {};
  for (const [en, tr] of Object.entries(holNamesSrc)) if (idx.has(en)) tx[idx.get(en)] = tr;
  return JSON.stringify({ s: strs, countries, tx, native: { de: ["DE", "AT", "CH"], ar: ["EG"], fr: ["FR"], ru: ["RU"], ja: ["JP"], zh: ["CN"] } }) + "\n";
})();

/* Gökyüzü (2. analiz #154, #157): olaylar (lib/sky-events.mjs, 2026–2031) ve takımyıldızlar (d3-celestial, BSD-3) */
const skyEventsJson = JSON.stringify(skyEvents(ROOT, 2026, 2031)) + "\n";
const conSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/constellations.json"), "utf8"));
const constellationsJson = JSON.stringify(conSrc.con) + "\n";
// Çizgi simge takımı (2. analiz #97)
const iconsSrc = JSON.parse(fs.readFileSync(path.join(ROOT, "mobile/shared/icons/icons.json"), "utf8"));
delete iconsSrc._note;
const iconsJson = JSON.stringify(iconsSrc) + "\n";
/* Widget'lardaki hava ve gök simgeleri 4 Kasım 2026'dan beri 3D görsel (res/drawable-nodpi/e3d_*.png,
   mobile/shared/icons3d/gen-icons3d.py); eski çizgi vektörleri (wi_*.xml) kaldırıldı. */
/* NASA SVS CGI Moon Kit dokusu (kamu malı; 2. analiz #103): ikili dosya, uygulamalara kopyalanır */
const moonJpg = fs.readFileSync(path.join(ROOT, "src/assets/moon.jpg"));

const files = {
  ...otdFiles,
  "mobile/ios/App/Resources/sky-events.json": skyEventsJson,
  "mobile/android/app/src/main/assets/sky-events.json": skyEventsJson,
  "mobile/ios/App/Resources/constellations.json": constellationsJson,
  "mobile/android/app/src/main/assets/constellations.json": constellationsJson,
  "mobile/ios/App/Resources/icons.json": iconsJson,
  "mobile/android/app/src/main/assets/icons.json": iconsJson,
  "mobile/ios/App/Resources/moon.jpg": moonJpg,
  "mobile/android/app/src/main/assets/moon.jpg": moonJpg,
  "mobile/ios/App/Resources/world-holidays.json": wholJson,
  "mobile/android/app/src/main/assets/world-holidays.json": wholJson,
  "mobile/ios/App/Resources/Localizable.xcstrings": xcstrings,
  "mobile/ios/App/Resources/InfoPlist.xcstrings": catalog(plistStrings),
  "mobile/ios/App/Resources/AppShortcuts.xcstrings": catalog(shortcutStrings, "en"),
  "mobile/ios/App/Resources/provinces.json": provincesJson,
  "mobile/ios/App/Resources/calendar.json": calendarJson,
  "mobile/ios/App/Resources/cities.json": citiesJson,
  "mobile/ios/App/Resources/places.json": placesJson,
  "mobile/ios/App/Resources/eclipses.json": eclipsesJson,
  "mobile/ios/App/Resources/districts.json": districtsJson,
  "mobile/ios/App/Resources/hijri.json": hijriJson,
  "mobile/ios/App/Resources/fitre.json": fitreJson,
  "mobile/ios/App/Resources/bridges.json": bridgesJson,
  "mobile/android/app/src/main/assets/provinces.json": provincesJson,
  "mobile/android/app/src/main/assets/calendar.json": calendarJson,
  "mobile/android/app/src/main/assets/cities.json": citiesJson,
  "mobile/android/app/src/main/assets/places.json": placesJson,
  "mobile/android/app/src/main/assets/eclipses.json": eclipsesJson,
  "mobile/android/app/src/main/assets/districts.json": districtsJson,
  "mobile/android/app/src/main/assets/hijri.json": hijriJson,
  "mobile/android/app/src/main/assets/fitre.json": fitreJson,
  "mobile/android/app/src/main/assets/bridges.json": bridgesJson,
  "mobile/android/app/src/main/res/xml/locales_config.xml": localesConfig,
  ...Object.fromEntries(LANGS.map((l) => [`mobile/android/app/src/main/res/${ANDROID_DIRS[l]}/strings.xml`, androidXml(l)])),
  // Wear OS saati (Wear modülü): il listesi, hicri ay başları ve saat ekranlarının metinleri
  "mobile/android/wear/src/main/assets/provinces.json": provincesJson,
  "mobile/android/wear/src/main/assets/hijri.json": hijriJson,
  "mobile/android/wear/src/main/res/xml/locales_config.xml": localesConfig,
  ...Object.fromEntries(LANGS.map((l) => [`mobile/android/wear/src/main/res/${ANDROID_DIRS[l]}/strings.xml`, androidXml(l, wearRes)])),
};
const check = process.argv.includes("--check");
const stale = [];
for (const [rel, body] of Object.entries(files)) {
  const file = path.join(ROOT, rel);
  if (check) {
    if (!fs.existsSync(file) || (Buffer.isBuffer(body) ? !fs.readFileSync(file).equals(body) : fs.readFileSync(file, "utf8") !== body)) stale.push(rel);
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
