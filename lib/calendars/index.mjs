/* Kapsayıcı takvimler (9 Kasım 2026, sözleşme v1): takvim modüllerinin kayıt defteri, veri üretimi ve doğrulama.
   Hesap yapım anında burada (Node) yapılır; site (build.mjs) ve uygulamalar (mobile/shared/gen-strings.mjs → calendars.json)
   yalnız üretilen veriyi okur. Yeni takvim eklemek: docs/takvimler.md.
     lib/calendars/<id>.mjs        meta + events(year) [+ months(from, to), daily(year), yearLabel(year)]
     lib/calendars/<id>.test.json  güvenilir kaynaktan doğrulama verisi
     src/i18n-cal/<id>.json        18 dilde metinler (name, desc, ev.*, isteğe bağlı month, leapFmt, fmt, yearFmt, daily.*, note)
   ve aşağıdaki CALENDARS dizisine bir satır. */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import * as christian from "./christian.mjs";
import * as jewish from "./jewish.mjs";
import * as persian from "./persian.mjs";
import * as chinese from "./chinese.mjs";
import * as un from "./un.mjs";

/* Kayıt defteri: sıra, seçim listelerindeki sıradır. Yeni takvim = bir import + bir satır. */
export const CALENDARS = [
  christian,
  jewish,
  persian,
  chinese,
  un,
];

export const CAL_FROM = 2025, CAL_TO = 2032;
export const CAL_LANGS = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th", "es", "pt", "hi"];
export const CAL_GROUPS = ["religious", "cultural", "civil"];
/* Metin yer tutucularının uygulamalardaki argüman sırası (iOS %1$@…, Android %1$s…): fmt → (d, m, y), leapFmt → (m),
   yearFmt → (y). Türkçe fmt de bu sırada yazılır (doğrulama denetler; uygulamalar sırayı Türkçe metinden alır); diğer diller
   istediği sırada yazar. Başka yer tutucu (ör. Yahudi takviminin candles/havdalah metinlerinde saat {t}) tek başına kullanılır. */
export const CAL_PLACEHOLDERS = ["d", "m", "y"];
/* Günlük kod: tek anahtar (daily.<kod>) ya da harf+rakam parçalarının birleşimi ("t5n12" → daily.t5 · daily.n12) */
export const DAILY_TOKEN = /[a-z]+[0-9]+/gi;

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "../..");

const ISO = /^(\d{4})-(\d\d)-(\d\d)$/;
const validIso = (s) => {
  const m = ISO.exec(s);
  if (!m) return false;
  const t = new Date(Date.UTC(+m[1], +m[2] - 1, +m[3]));
  return t.getUTCFullYear() === +m[1] && t.getUTCMonth() === +m[2] - 1 && t.getUTCDate() === +m[3];
};
const dayNum = (s) => { const [y, m, d] = s.split("-").map(Number); return Date.UTC(y, m - 1, d) / 86400000; };

/** src/i18n-cal/<id>.json → { id: { dil: {...} } } (yalnız kayıtlı takvimler) */
export function loadCalTexts(root = ROOT) {
  return Object.fromEntries(CALENDARS.map(({ meta }) => [meta.id, JSON.parse(fs.readFileSync(path.join(root, "src/i18n-cal", `${meta.id}.json`), "utf8"))]));
}

/** Günlük kodun metin anahtarları: önce bütün kod, yoksa parçaları (null: çözülemiyor) */
export function dailyKeys(code, names) {
  if (names && typeof names[code] === "string") return [code];
  const parts = String(code).match(DAILY_TOKEN) || [];
  return parts.length && parts.join("") === code && parts.every((k) => names && typeof names[k] === "string") ? parts : null;
}
/** Günlük kodlar → { "YYYY-MM-DD": kod } (dizi biçimi: yıl → yılın günlerine göre kodlar, 0'dan) */
export function dailyMap(daily) {
  const out = {};
  for (const [k, v] of Object.entries(daily || {})) {
    if (!Array.isArray(v)) { out[k] = v; continue; }
    v.forEach((code, i) => {
      if (!code) return;
      const t = new Date(Date.UTC(+k, 0, 1 + i));
      out[t.toISOString().slice(0, 10)] = code;
    });
  }
  return out;
}

/** 2025–2032 verisi (calendars.json biçimi; sözleşme v1) */
export function calendarData(from = CAL_FROM, to = CAL_TO) {
  const cals = {};
  for (const mod of CALENDARS) {
    const { meta } = mod;
    const events = [];
    for (let y = from; y <= to; y++) for (const e of mod.events(y)) events.push(e.end ? [e.date, e.key, e.end] : [e.date, e.key]);
    events.sort((a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : a[1] < b[1] ? -1 : a[1] > b[1] ? 1 : 0));
    const months = mod.months ? mod.months(from, to).map((r) => [r[0], r[1], r[2], r[3] ? 1 : 0]) : null;
    // Günlük kodlar: modül { tarih: kod } ya da dizi (yılın günlerine göre, 0'dan; boş öğe = kod yok) verir; dizi yıl anahtarıyla kalır
    let daily = null;
    if (mod.daily) for (let y = from; y <= to; y++) {
      const d = mod.daily(y);
      if (Array.isArray(d)) (daily ||= {})[y] = d.map((x) => x || "");
      else if (d && Object.keys(d).length) daily = Object.assign(daily || {}, d);
    }
    // Yıl etiketi: sayı ya da metin (metin olduğu gibi gösterilir, çevrilmez)
    let years = null;
    if (mod.yearLabel) for (let y = from; y <= to; y++) { const v = mod.yearLabel(y); if (v != null) (years ||= {})[y] = v; }
    cals[meta.id] = { group: meta.group, regions: meta.regions, color: meta.color, events, months, daily, years };
  }
  return { v: 1, from, to, cals };
}

/** calendars.json (uygulamalar: Android assets, iOS App/Resources) */
export const calendarsJson = (data = calendarData()) => JSON.stringify(data) + "\n";

/** Gün → ikinci tarih: { y, m, d, leap } ya da null (ay tablosunun dışında). Son satır tablonun sonudur. */
export function secondDate(months, date) {
  if (!months || !months.length || date < months[0][0] || date >= months[months.length - 1][0]) return null;
  let lo = 0, hi = months.length - 1;
  while (hi - lo > 1) { const mid = (lo + hi) >> 1; if (months[mid][0] <= date) lo = mid; else hi = mid; }
  const r = months[lo];
  return { y: r[1], m: r[2], d: dayNum(date) - dayNum(r[0]) + 1, leap: !!r[3] };
}

/* WCAG göreli parlaklık (renk iki temanın zemininde en az 3:1 okunmalı: nokta ve şerit, metin değil) */
const lum = (hex) => {
  const c = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255).map((v) => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
};
const contrast = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };
/* src/styles.css zeminleri (açık bg/surface, koyu bg/surface) */
const THEME_BGS = ["#f6f0e4", "#fffbf4", "#14151c", "#1d1e27"];

const flatTexts = (o, p = "") => Object.entries(o).flatMap(([k, v]) =>
  Array.isArray(v) ? v.map((x, i) => [`${p}${k}.${i + 1}`, x]) : v && typeof v === "object" ? flatTexts(v, `${p}${k}.`) : [[p + k, v]]);
const placeholders = (s) => [...new Set([...String(s).matchAll(/\{([a-z]+)\}/gi)].map((m) => m[1]))].sort().join();
/* Latin olmayan rakamlar (Arapça, Farsça/Urduca, Devanagari, Bengalce, Tay): metinlerde Latin rakamı kullanılır */
const NON_LATIN_DIGIT = /[٠-٩۰-۹०-९০-৯๐-๙]/;

/** Doğrulama: hata dizisi döner (boşsa sorun yok). build.mjs, gen-strings.mjs ve scripts/test.mjs çağırır. */
export function validateCalendars(data = calendarData(), texts = loadCalTexts(), root = ROOT) {
  const err = [];
  const ids = CALENDARS.map((m) => m.meta.id);
  if (new Set(ids).size !== ids.length) err.push("takvim kimliği iki kez kayıtlı");
  // Kayıtsız modül ya da metin dosyası kalmasın
  for (const f of fs.readdirSync(path.join(root, "lib/calendars"))) {
    const m = /^([a-z]+)\.mjs$/.exec(f);
    if (m && m[1] !== "index" && !ids.includes(m[1])) err.push(`lib/calendars/${f} CALENDARS'a kayıtlı değil`);
  }
  for (const f of fs.readdirSync(path.join(root, "src/i18n-cal"))) {
    const m = /^([a-z]+)\.json$/.exec(f);
    if (!m || !ids.includes(m[1])) err.push(`src/i18n-cal/${f}: kayıtlı bir takvime ait değil`);
  }
  for (const mod of CALENDARS) {
    const { meta } = mod, id = meta.id, c = data.cals[id], tx = texts[id], P = `${id}:`;
    if (!/^[a-z]+$/.test(id)) err.push(`${P} kimlik yalnız küçük harf olmalı`);
    if (typeof mod.events !== "function") err.push(`${P} events(year) yok`);
    if (!CAL_GROUPS.includes(meta.group)) err.push(`${P} group ${CAL_GROUPS.join("|")} olmalı`);
    if (!Array.isArray(meta.regions) || !meta.regions.every((r) => /^[A-Z]{2}$/.test(r))) err.push(`${P} regions ISO 3166-1 iki harfli kod dizisi olmalı`);
    if (!/^#[0-9A-Fa-f]{6}$/.test(meta.color)) err.push(`${P} color #RRGGBB olmalı`);
    else for (const bg of THEME_BGS) if (contrast(meta.color, bg) < 3) err.push(`${P} renk ${meta.color} ${bg} zemininde 3:1'den az (${contrast(meta.color, bg).toFixed(2)})`);
    if (!fs.existsSync(path.join(root, "lib/calendars", `${id}.test.json`))) err.push(`${P} lib/calendars/${id}.test.json yok`);
    if (!tx) { err.push(`${P} src/i18n-cal/${id}.json yok`); continue; }

    // Metinler: 18 dil, aynı anahtarlar, boş değil, aynı yer tutucular, Latin rakamı
    for (const l of Object.keys(tx)) if (!CAL_LANGS.includes(l)) err.push(`${P} bilinmeyen dil: ${l}`);
    const base = new Map(flatTexts(tx.tr || {}));
    for (const l of CAL_LANGS) {
      if (!tx[l]) { err.push(`${P} ${l} metinleri yok`); continue; }
      const cur = new Map(flatTexts(tx[l]));
      for (const k of base.keys()) if (!cur.has(k)) err.push(`${P} ${l}: ${k} yok`);
      for (const [k, v] of cur) {
        if (!base.has(k)) { err.push(`${P} ${l}: ${k} tr'de yok`); continue; }
        if (typeof v !== "string" || !v.trim()) err.push(`${P} ${l}: ${k} boş ya da metin değil`);
        else {
          if (NON_LATIN_DIGIT.test(v)) err.push(`${P} ${l}: ${k} Latin olmayan rakam`);
          if (placeholders(v) !== placeholders(base.get(k))) err.push(`${P} ${l}: ${k} yer tutucuları tr ile aynı değil`);
          if (k === "fmt" && placeholders(v) !== "d,m,y") err.push(`${P} ${l}: fmt {d}, {m} ve {y} içermeli (başka yer tutucu olmadan)`);
          if (k === "leapFmt" && placeholders(v) !== "m") err.push(`${P} ${l}: leapFmt yalnız {m} içermeli`);
          if (k === "yearFmt" && placeholders(v) !== "y") err.push(`${P} ${l}: yearFmt yalnız {y} içermeli`);
          if (!/^(fmt|leapFmt|yearFmt)$/.test(k) && /[,]/.test(placeholders(v))) err.push(`${P} ${l}: ${k} en çok bir yer tutucu içerebilir`);
        }
      }
    }
    const t = tx.tr || {};
    for (const k of ["name", "desc"]) if (typeof t[k] !== "string") err.push(`${P} ${k} metni yok`);
    // Uygulamalar argüman sırasını Türkçe metinden alır: Türkçe fmt'de {d}, {m}, {y} bu sırada
    if (typeof t.fmt === "string" && !/\{d\}[^]*\{m\}[^]*\{y\}/.test(t.fmt)) err.push(`${P} tr: fmt'de sıra {d}, {m}, {y} olmalı`);
    if (!t.ev || typeof t.ev !== "object") err.push(`${P} ev nesnesi yok`);
    if (!c) { err.push(`${P} veri üretilmedi`); continue; }

    // Olaylar
    const seen = new Set();
    for (const e of c.events) {
      const [d, k, end] = e;
      if (!validIso(d) || +d.slice(0, 4) < data.from || +d.slice(0, 4) > data.to) err.push(`${P} geçersiz tarih ${d} (${k})`);
      if (!/^[A-Za-z0-9]+$/.test(k)) err.push(`${P} olay anahtarı harf ve rakam olmalı: ${k}`);
      if (!t.ev || typeof t.ev[k] !== "string") err.push(`${P} ev.${k} metni yok`);
      if (end !== undefined && (!validIso(end) || end <= d || dayNum(end) - dayNum(d) > 60)) err.push(`${P} geçersiz bitiş ${end} (${d} ${k})`);
      if (seen.has(d + k)) err.push(`${P} aynı olay iki kez: ${d} ${k}`);
      seen.add(d + k);
    }
    for (let y = data.from; y <= data.to; y++) for (const e of mod.events(y)) if (!e.date.startsWith(String(y))) err.push(`${P} events(${y}) başka yılın olayını veriyor: ${e.date} ${e.key}`);
    const used = new Set(c.events.map((e) => e[1]));
    for (const k of Object.keys(t.ev || {})) if (!used.has(k)) err.push(`${P} ev.${k} hiçbir yılda kullanılmıyor`);

    // Ay tablosu
    if (c.months) {
      if (!Array.isArray(t.month)) err.push(`${P} ay tablosu var ama month dizisi yok (fmt isteğe bağlı, varsayılan "{d} {m} {y}")`);
      const r0 = c.months[0], rn = c.months[c.months.length - 1];
      if (!r0 || r0[0] > `${data.from}-01-01`) err.push(`${P} ay tablosu ${data.from}-01-01'den önce başlamalı`);
      if (!rn || rn[0] <= `${data.to}-12-31`) err.push(`${P} ay tablosunun son satırı (tablonun sonu) ${data.to}-12-31'den sonra olmalı`);
      c.months.forEach((r, i) => {
        if (!validIso(r[0]) || !Number.isInteger(r[1]) || !Number.isInteger(r[2]) || r[2] < 1 || (Array.isArray(t.month) && r[2] > t.month.length)) err.push(`${P} ay satırı hatalı: ${JSON.stringify(r)}`);
        if (i && !(r[0] > c.months[i - 1][0])) err.push(`${P} ay satırları tarih sırasında değil: ${r[0]}`);
        if (i && dayNum(r[0]) - dayNum(c.months[i - 1][0]) > 40) err.push(`${P} ay 40 günden uzun: ${c.months[i - 1][0]}`);
        if (r[3] && typeof t.leapFmt !== "string") err.push(`${P} artık ay var ama leapFmt yok`);
      });
    } else if (t.month || t.fmt) err.push(`${P} month/fmt metni var ama months() yok`);

    // Günlük kodlar: { tarih: kod } ya da { yıl: [kod, …] } (bir takvimde tek biçim); kod daily.<kod> ya da parçaları
    if (c.daily) {
      const keys = Object.keys(c.daily), arr = keys.filter((k) => Array.isArray(c.daily[k]));
      if (arr.length && arr.length !== keys.length) err.push(`${P} daily: iki biçim karışık`);
      for (const y of arr) {
        const n = (Date.UTC(+y + 1, 0, 1) - Date.UTC(+y, 0, 1)) / 86400000;
        if (!/^\d{4}$/.test(y) || c.daily[y].length > n) err.push(`${P} daily: ${y} dizisi hatalı (en çok ${n} gün)`);
      }
      const bad = new Set();
      for (const [d, code] of Object.entries(dailyMap(c.daily))) {
        if (!validIso(d)) err.push(`${P} daily: geçersiz tarih ${d}`);
        if (!dailyKeys(code, t.daily)) bad.add(code);
      }
      for (const code of bad) err.push(`${P} daily: "${code}" kodunun metni yok (daily.${code} ya da harf+rakam parçaları)`);
    } else if (t.daily) err.push(`${P} daily metni var ama daily() yok`);

    // Yıl etiketleri: sayı ya da boş olmayan metin; görünmesi için yearFmt gerekir
    if (c.years) {
      if (typeof t.yearFmt !== "string") err.push(`${P} yıl etiketi var ama yearFmt yok (uygulamalar yearFmt yoksa etiketi göstermez)`);
      for (const [y, v] of Object.entries(c.years)) if (!(typeof v === "number" || (typeof v === "string" && v))) err.push(`${P} yıl etiketi hatalı: ${y} → ${JSON.stringify(v)}`);
    } else if (t.yearFmt) err.push(`${P} yearFmt var ama yearLabel() yok`);
  }
  return err;
}

/** <id>.test.json denetimi: { source, cases: [{date, key}], absent?: [{date, key}], months?: [{date, y, m, d?, leap?}],
    daily?: [{date, code}], years?: [{year, value}] } */
export function runCalendarTests(data = calendarData(), root = ROOT) {
  const err = [];
  for (const { meta } of CALENDARS) {
    const id = meta.id, f = path.join(root, "lib/calendars", `${id}.test.json`);
    if (!fs.existsSync(f)) continue; // validateCalendars bildirir
    const T = JSON.parse(fs.readFileSync(f, "utf8")), c = data.cals[id], P = `${id}.test.json:`;
    if (!T.source || !(T.cases || []).length) err.push(`${P} source ve en az bir case gerekli`);
    const has = (date, key) => c.events.some((e) => e[0] === date && e[1] === key);
    for (const x of T.cases || []) if (!has(x.date, x.key)) err.push(`${P} ${x.date} ${x.key} yok`);
    for (const x of T.absent || []) if (has(x.date, x.key)) err.push(`${P} ${x.date} ${x.key} olmamalı`);
    for (const x of T.months || []) {
      const s = secondDate(c.months, x.date);
      if (!s || s.y !== x.y || s.m !== x.m || (x.d !== undefined && s.d !== x.d) || (x.leap !== undefined && s.leap !== !!x.leap))
        err.push(`${P} ${x.date} ikinci tarih ${JSON.stringify(s)}, beklenen ${JSON.stringify(x)}`);
    }
    const dm = dailyMap(c.daily);
    for (const x of T.daily || []) if (dm[x.date] !== x.code) err.push(`${P} ${x.date} günlük kod ${dm[x.date]}, beklenen ${x.code}`);
    for (const x of T.years || []) if (JSON.stringify(c.years && c.years[x.year]) !== JSON.stringify(x.value)) err.push(`${P} ${x.year} yıl etiketi ${JSON.stringify(c.years && c.years[x.year])}, beklenen ${JSON.stringify(x.value)}`);
  }
  return err;
}

/** Sitedeki takvim yıl sayfası için dil ve yıl dilimi: assets/cals-<dil>-<yıl>.json (build.mjs).
    Yıla değen olaylar (çok günlü olaylar önceki yıldan taşabilir), yılı kapsayan ay satırları (+ sonraki ilk satır), o yılın
    günlük kodları ({ tarih: kod } biçiminde) ve yıl etiketi; metinlerden yalnız gerekenler. */
export function siteYearSlice(data, texts, lang, year) {
  const first = `${year}-01-01`, last = `${year}-12-31`, out = {};
  for (const { meta } of CALENDARS) {
    const id = meta.id, c = data.cals[id], t = texts[id][lang];
    const events = c.events.filter((e) => e[0] <= last && (e[2] || e[0]) >= first);
    const o = { name: t.name, events, ev: Object.fromEntries([...new Set(events.map((e) => e[1]))].map((k) => [k, t.ev[k]])) };
    if (t.note) o.note = t.note;
    if (c.months) {
      let a = c.months.findIndex((r) => r[0] > first) - 1, b = c.months.findIndex((r) => r[0] > last);
      if (a < 0) a = 0;
      if (b < 0) b = c.months.length - 1;
      o.months = c.months.slice(a, b + 1);
      o.month = t.month; o.fmt = t.fmt || "{d} {m} {y}";
      if (t.leapFmt) o.leapFmt = t.leapFmt;
    }
    if (c.daily) {
      const d = Object.fromEntries(Object.entries(dailyMap(c.daily)).filter(([k]) => k.startsWith(String(year))));
      if (Object.keys(d).length) { o.daily = d; o.dailyNames = t.daily; }
    }
    if (c.years && c.years[year] != null && t.yearFmt) { o.year = c.years[year]; o.yearFmt = t.yearFmt; }
    out[id] = o;
  }
  return out;
}
