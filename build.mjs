// Saat Tarih statik site derleyicisi. Bağımlılık yok: `node build.mjs` → dist/
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";
import { computeYear, holidayCounts, makeFmt, dowOf, daysInMonth, iso, inRange, addDays, isoWeek1Monday, isoWeeksInYear } from "./lib/events.mjs";
import { bridges } from "./lib/bridges.mjs";
import { hijriTable } from "./lib/hijri.mjs";

const ROOT = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.join(ROOT, "src");
const OUT = path.join(ROOT, "dist");

const readJson = (p) => JSON.parse(fs.readFileSync(path.join(ROOT, p), "utf8"));
const cfg = readJson("site.config.json");
const ORIGIN = (process.env.SITE_ORIGIN || cfg.origin).replace(/\/+$/, "");
const HOST = new URL(ORIGIN).host;
const UMAMI_ID = process.env.UMAMI_ID ?? cfg.umamiId ?? "";
/* Diller: tr (varsayılan) ve en zorunlu; diğerleri src/i18n/<dil>.json varsa derlenir. */
const ALL_LANGS = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th"];
const LANGS = ALL_LANGS.filter((l) => fs.existsSync(path.join(ROOT, "src/i18n", `${l}.json`)));
const I = Object.fromEntries(LANGS.map((l) => [l, readJson(`src/i18n/${l}.json`)]));
const CITIES = readJson("src/data/cities.json");
const LANG_NAMES = { tr: "Türkçe", en: "English", de: "Deutsch", az: "Azərbaycanca", ar: "العربية", id: "Bahasa Indonesia", ur: "اردو", fa: "فارسی", fr: "Français", ru: "Русский", ja: "日本語", ko: "한국어", zh: "简体中文", fil: "Filipino", th: "ไทย" };
/* Ortak liste servisinin bildiği diller (bumbagroup.com/api/liste/katil); servis yeni dil eklediğinde buraya yaz */
const LIST_LANGS = ["tr", "en"];
/* Sağdan sola yazılan diller */
const RTL = ["ar", "ur", "fa"];
const DIR = (l) => (RTL.includes(l) ? "rtl" : "ltr");
const ARROW = (l) => (RTL.includes(l) ? "←" : "→");
/* Liste ayırıcısı: Arapça yazılı dillerde Arapça virgül, Japonca ve Çincede ideografik virgül */
const LSEP = (l) => (RTL.includes(l) ? "، " : ["ja", "zh"].includes(l) ? "、" : ", ");
/* Türkçe dışındaki dillerin adresleri İngilizce tablodan türetilir: /en/ → /<dil>/ */
const deriveRoutes = (o, l) =>
  Object.fromEntries(Object.entries(o).map(([k, v]) => [k, v.replace(/^\/en\//, `/${l}/`).replace(/^\/embed\/en\//, `/embed/${l}/`)]));
const extendRoutes = (table) => {
  for (const l of LANGS) Object.assign(ROUTES[l], table[l] || deriveRoutes(table.en, l));
};

const arm = cfg.arm;
const BADGE_W = { games: 185, suites: 177, life: 164, ventures: 205 }[arm];

/* "2026-01-13" → dilin uzun tarih biçiminde ("13 Ocak 2026") */
const fmtDate = (lang, isoDate) => { const [y, m, d] = isoDate.split("-").map(Number); return tpl(I[lang].calendar.dateFmt, { day: d, month: I[lang].calendar.months[m - 1], year: y }); };

function updatedText(lang) {
  const [y, m, d] = cfg.updated.split("-").map(Number);
  return tpl(I[lang].calendar.dateFmt, { day: d, month: I[lang].calendar.months[m - 1], year: y });
}

/* ---------- yardımcılar ---------- */
const esc = (s) =>
  String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
const tpl = (s, v) => String(s).replace(/\{(\w+)\}/g, (_, k) => (v[k] ?? ""));
function md(s) {
  return esc(s)
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, t, u) =>
      /^https?:/.test(u) ? `<a href="${u}" rel="noopener">${t}</a>` : `<a href="${u}">${t}</a>`
    );
}
const hash = (buf) => crypto.createHash("sha1").update(buf).digest("hex").slice(0, 8);
const write = (rel, data) => {
  const p = path.join(OUT, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, data);
};
const abs = (p) => ORIGIN + p;

/* ---------- adresler ---------- */
const ROUTES = {
  tr: { home: "/", world: "/dunya-saatleri/", privacy: "/gizlilik/", calendar: "/takvim/", tools: "/araclar/", week: "/hafta-numarasi/", tz: "/turkiye-saat-dilimi/", stopwatch: "/kronometre/", countdown: "/geri-sayim/", alarm: "/alarm/", pomodoro: "/pomodoro/" },
  en: { home: "/en/", world: "/en/world-clock/", privacy: "/en/privacy/", calendar: "/en/calendar/", tools: "/en/tools/", week: "/en/week-number/", tz: "/en/turkey-time-zone/", stopwatch: "/en/stopwatch/", countdown: "/en/countdown/", alarm: "/en/alarm/", pomodoro: "/en/pomodoro/" },
};
for (const l of LANGS) if (!ROUTES[l]) ROUTES[l] = deriveRoutes(ROUTES.en, l);
const cityPath = (lang, c) => `${ROUTES[lang].world}${c[lang].slug}/`;

/* ---------- varlıklar (sürüm damgalı) ---------- */
fs.rmSync(OUT, { recursive: true, force: true });
const ASSETS = {};
for (const [name, from] of [
  ["styles.css", "styles.css"],
  ["theme.js", "js/theme.js"],
  ["consent.js", "js/consent.js"],
  ["main.js", "js/main.js"],
  ["tools.js", "js/tools.js"],
  ["calendar.js", "js/calendar.js"],
  ["dates.js", "js/dates.js"],
  ["planner.js", "js/planner.js"],
  ["poll.js", "js/poll.js"],
  ["embed.js", "js/embed.js"],
  ["prayer-calc.js", "js/prayer-calc.js"],
  ["prayer.js", "js/prayer.js"],
  ["qibla.js", "js/qibla.js"],
  ["sky-calc.js", "js/sky-calc.js"],
  ["sky.js", "js/sky.js"],
  ["eclipse-map.js", "js/eclipse-map.js"],
  ["otd.js", "js/otd.js"],
  ["retire.js", "js/retire.js"],
  ["zakat.js", "js/zakat.js"],
  ["more-tools.js", "js/more-tools.js"],
  ["bridge.js", "js/bridge.js"],
  ["zones.js", "js/zones.js"],
  ["imsakiye.js", "js/imsakiye.js"],
  ["world.js", "js/world.js"],
  ["place.js", "js/place.js"],
  ["nature-calc.js", "js/nature-calc.js"],
  ["nature.js", "js/nature.js"],
  ["weather-core.js", "js/weather-core.js"],
  ["weather.js", "js/weather.js"],
  ["globe.js", "js/globe.js"],
]) {
  const buf = fs.readFileSync(path.join(SRC, from));
  write(`assets/${name}`, buf);
  ASSETS[name] = `/assets/${name}?v=${hash(buf)}`;
}
/* Küre: Natural Earth kabartmalı dünya dokusu (kamu malı) ve yer adları (Natural Earth 1:10m Populated Places, kamu malı;
   src/data/places.json, saat dilimiyle birlikte). Yerler ayrıca "konumumu kullan" sonucunu en yakın şehre çevirir (place.js).
   Hepsi repodaki dosyalardan; derlemede dış veri indirilmez. Web küresi 30 Ekim'de kaldırılmış, 31 Ekim 2026'da ana sayfaya geri döndü. */
const EARTH_BUF = fs.readFileSync(path.join(SRC, "assets/earth.jpg"));
write("assets/earth.jpg", EARTH_BUF);
const EARTH_URL = `/assets/earth.jpg?v=${hash(EARTH_BUF)}`;
const PLACES_SRC = readJson("src/data/places.json");
const PLACES_JSON = JSON.stringify({ tz: PLACES_SRC.tz, p: PLACES_SRC.p.map((r) => r.slice(0, 3)) });
write("assets/places.json", PLACES_JSON);
const PLACES_URL = `/assets/places.json?v=${hash(PLACES_JSON)}`;
/* Tutulma haritası için kara sınırları (Natural Earth 1:110m, kamu malı; scripts/fetch-land.mjs) */
const LAND_JSON = JSON.stringify({ rings: JSON.parse(fs.readFileSync(path.join(SRC, "data", "land-110m.json"), "utf8")).rings });
write("assets/land.json", LAND_JSON);
const LAND_URL = `/assets/land.json?v=${hash(LAND_JSON)}`;
const PLACE_NAMES_URL = {};
for (const l of LANGS) {
  const j = JSON.stringify(PLACES_SRC.names[l] || PLACES_SRC.names.en);
  write(`assets/places-${l}.json`, j);
  PLACE_NAMES_URL[l] = `/assets/places-${l}.json?v=${hash(j)}`;
}
for (const f of ["favicon.svg", "favicon.ico", "icon-192.png", "icon-512.png", "apple-touch-icon.png", "og.png"]) {
  fs.copyFileSync(path.join(SRC, "assets", f), path.join(OUT, f));
}

/* ---------- parçalar ---------- */
const CSP = [
  "default-src 'self'",
  "script-src 'self' https://istatistik.bumba.tr",
  "connect-src 'self' https://istatistik.bumba.tr https://bumbagroup.com https://api.open-meteo.com https://overpass-api.de",
  "img-src 'self' data: https://bumbagroup.com https://tile.openstreetmap.org",
  "frame-src https://www.youtube-nocookie.com", // alarm müziği (YouTube gelişmiş gizlilik modu; dış hizmet izniyle, 31 Ekim 2026)
  "style-src 'self'",
  "font-src 'self'",
  "form-action 'self' https://bumbagroup.com",
  "base-uri 'self'",
  "object-src 'none'",
].join("; ");

const ICON_SEARCH = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true" focusable="false"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>`;
const ICON_MOON = `<svg class="only-light" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>`;
const ICON_SUN = `<svg class="only-dark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>`;
const LOGO = `<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><circle cx="32" cy="32" r="26"/><path d="M32 17v15l10 6"/></svg>`;

const ICON_PIN = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21z"/><circle cx="12" cy="9.5" r="2.5"/></svg>`;
const ICON_GEAR = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/></svg>`;
const ICON_LOCK = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg>`;
const ICON_COOKIE = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M21 12.5A9 9 0 1 1 11.5 3a3 3 0 0 0 4 4 3 3 0 0 0 5.5 5.5z"/><circle cx="8.5" cy="11" r="1"/><circle cx="12" cy="16" r="1"/><circle cx="15.5" cy="13" r="1"/></svg>`;
const ICON_MENU = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true" focusable="false"><path d="M4 7h16M4 12h16M4 17h16"/></svg>`;
const ICON_FULL = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/></svg>`;
const ICON_PREV = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m15 5-7 7 7 7"/></svg>`;
const ICON_NEXT = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m9 5 7 7-7 7"/></svg>`;

const tzA = (tz) => (tz ? ` data-tz="${esc(tz)}"` : "");

function analogSvg({ tz = "", numerals = true, minimal = false, size = "lg" } = {}) {
  let marks = "";
  if (minimal) {
    for (let i = 0; i < 4; i++) marks += `<line class="tick tick-major" x1="100" y1="12" x2="100" y2="28" transform="rotate(${i * 90} 100 100)"/>`;
  } else {
    for (let i = 0; i < 60; i++) {
      const major = i % 5 === 0;
      marks += `<line class="tick${major ? " tick-major" : ""}" x1="100" y1="${major ? 12 : 14}" x2="100" y2="${major ? 24 : 19}" transform="rotate(${i * 6} 100 100)"/>`;
    }
  }
  let nums = "";
  if (numerals) {
    for (let n = 1; n <= 12; n++) {
      const a = (n * 30 * Math.PI) / 180;
      nums += `<text class="num" x="${(100 + 66 * Math.sin(a)).toFixed(2)}" y="${(100 - 66 * Math.cos(a)).toFixed(2)}" text-anchor="middle" dominant-baseline="central">${n}</text>`;
    }
  }
  const hh = minimal ? 3.5 : 6;
  const mh = minimal ? 2.5 : 4.5;
  return `<svg class="analog analog-${size}${minimal ? " analog-minimal" : ""}" viewBox="0 0 200 200" data-analog${tzA(tz)} aria-hidden="true" focusable="false"><circle class="face" cx="100" cy="100" r="94"/>${marks}${nums}<line class="hand" data-hand="h" stroke-width="${hh}" x1="100" y1="104" x2="100" y2="${numerals ? 56 : 52}"/><line class="hand" data-hand="m" stroke-width="${mh}" x1="100" y1="106" x2="100" y2="${numerals ? 34 : 30}"/><line class="hand hand-sec" data-hand="s" x1="100" y1="116" x2="100" y2="24"/><circle class="pin" cx="100" cy="100" r="5"/></svg>`;
}

const pTz = (tz) => `<p class="tz-label" data-live="tzname"${tzA(tz)}>&nbsp;</p>`;
const pDigital = (tz, cls = "") => `<p class="clock${cls}" role="timer" data-live="time" data-sec${tzA(tz)}>--:--:--</p>`;
const pDate = (tz) => `<p class="date" data-live="date"${tzA(tz)}>&nbsp;</p>`;
const srTime = (tz) => `<p class="sr-only" role="timer" data-live="time"${tzA(tz)}></p>`;
const calendarHtml = (tz) => `<div class="cal" data-live="calendar"${tzA(tz)}><p class="cal-title" data-cal-title>&nbsp;</p><table><thead><tr data-cal-head></tr></thead><tbody data-cal-body></tbody></table></div>`;
const flipHtml = (tz) => {
  const d = (i) => `<span class="flip-d" data-flip-d="${i}"><span class="flip-v">0</span></span>`;
  return `<div class="flip" data-live="flip"${tzA(tz)}><div class="flip-g">${d(0)}${d(1)}</div><div class="flip-g">${d(2)}${d(3)}</div><div class="flip-g flip-sec">${d(4)}${d(5)}</div><span class="flip-ampm" data-live="ampm"${tzA(tz)}></span></div>`;
};
const ringsHtml = (tz) =>
  `<div class="rings"><svg viewBox="0 0 200 200" data-live="rings"${tzA(tz)} aria-hidden="true" focusable="false">${[
    ["h", 88],
    ["m", 72],
    ["s", 56],
  ]
    .map(
      ([k, r]) =>
        `<circle class="ring-track" cx="100" cy="100" r="${r}"/><circle class="ring r-${k}" data-ring="${k}" cx="100" cy="100" r="${r}" pathLength="100" stroke-dasharray="0 100" transform="rotate(-90 100 100)"/>`
    )
    .join("")}</svg><div class="rings-text">${pDigital(tz, " clock-sm")}</div></div>`;

/* Hava durumlu saatler: içerik weather-core.js'te doldurulur (konum: şehir sayfasında şehir, ana sayfada kayıtlı konum).
   Yalnız bu modeller görünür olunca Open-Meteo'ya gidilir; gömme widget'ında yoktur (kayıtlı konum orada yok). */
const wxBlock = (kind) => `<div class="wxc wxc-${kind}" data-wxc="${kind}" aria-live="polite"><p class="wxc-msg">&nbsp;</p></div>`;

const hText = (...parts) => `<div class="h-text">${parts.join("")}</div>`;
const hAnalog = (tz) => analogSvg({ tz, size: "hz" });

// Model kayıt defteri: id, sınıf ve içerik. Sahnedeki sıra STAGE_ORDER'dadır; gömme widget'ı wx olmayanların hepsini kullanır.
const M = {};
const addModel = (id, cls, html, wx = false) => { M[id] = { id, cls, html, wx }; };
// 1-7: analog dizi (sahibin sıralaması, 9 Ekim 2026)
addModel("classic", "s-analog", (tz) => analogSvg({ tz, size: "lg" }) + srTime(tz));
addModel("analog-date", "s-analog-date", (tz) => analogSvg({ tz, size: "md" }) + pDate(tz) + srTime(tz));
addModel("stack", "s-stack", (tz) => analogSvg({ tz, size: "st" }) + `<div class="stack-text">${pDigital(tz, " clock-md")}</div>`);
addModel("wx-analog", "s-analog-date s-wx", (tz) => analogSvg({ tz, size: "md" }) + srTime(tz) + wxBlock("line"), true);
addModel("wx-stack", "s-stack s-wx", (tz) => analogSvg({ tz, size: "st" }) + `<div class="stack-text">${pDigital(tz, " clock-md")}</div>` + wxBlock("card"), true);
addModel("wx-analog-date", "s-analog-date s-wx", (tz) => analogSvg({ tz, size: "md" }) + pDate(tz) + srTime(tz) + wxBlock("line"), true);
addModel("wx-full", "s-stack s-wx", (tz) => analogSvg({ tz, size: "st" }) + `<div class="stack-text">${pDigital(tz, " clock-md")}${pDate(tz)}</div>` + wxBlock("line"), true);
// 8-14: aynı yedi modelin yatay düzeni (analog solda, bilgi sağda)
addModel("h-classic", "s-h", (tz) => hAnalog(tz) + hText(pTz(tz), srTime(tz)));
addModel("h-analog-date", "s-h", (tz) => hAnalog(tz) + hText(pDate(tz), srTime(tz)));
addModel("h-stack", "s-h", (tz) => hAnalog(tz) + hText(pDigital(tz, " clock-md")));
addModel("h-wx-analog", "s-h s-wx", (tz) => hAnalog(tz) + hText(wxBlock("line"), srTime(tz)), true);
addModel("h-wx-stack", "s-h s-wx", (tz) => hAnalog(tz) + hText(pDigital(tz, " clock-md"), wxBlock("line")), true);
addModel("h-wx-analog-date", "s-h s-wx", (tz) => hAnalog(tz) + hText(pDate(tz), wxBlock("line"), srTime(tz)), true);
addModel("h-wx-full", "s-h s-wx", (tz) => hAnalog(tz) + hText(pDigital(tz, " clock-md"), pDate(tz), wxBlock("line")), true);
// 15-19: dijital dizi
addModel("digital", "s-digital", (tz) => pDigital(tz));
addModel("digital-date", "s-digital", (tz) => pDigital(tz) + pDate(tz));
addModel("wx-digital", "s-digital s-wx", (tz) => pDigital(tz) + pDate(tz) + wxBlock("line"), true);
addModel("wx-digital-cal", "s-cal s-wx", (tz) => `<div class="cal-side">${pDigital(tz, " clock-sm")}${pDate(tz)}${wxBlock("line")}</div>${calendarHtml(tz)}`, true);
addModel("wx-digital-cal-week", "s-cal2 s-wx", (tz) => `<div class="cal2-top">${pDigital(tz, " clock-xl")}</div><div class="cal2-body"><div class="cal-side">${pDate(tz)}${wxBlock("week")}</div>${calendarHtml(tz)}</div>`, true);
// 20: çevirmeli saat (30 Ekim 2026, rakip analizi #5; önceden yalnız gömme widget'ındaydı)
addModel("flip", "s-flip", (tz) => flipHtml(tz) + pDate(tz));
// Yalnız gömme widget'ında (sahnede yok)
addModel("minimal", "s-analog", (tz) => analogSvg({ tz, size: "lg", numerals: false, minimal: true }) + srTime(tz));
addModel("duo", "s-duo", (tz) => analogSvg({ tz, size: "sm" }) + `<div class="duo-text">${pDigital(tz)}</div>`);
addModel("duo-date", "s-duo", (tz) => analogSvg({ tz, size: "sm" }) + `<div class="duo-text">${pTz(tz)}${pDigital(tz)}${pDate(tz)}</div>`);
addModel("calendar", "s-cal", (tz) => `<div class="cal-side">${pTz(tz)}${pDigital(tz, " clock-sm")}${pDate(tz)}</div>${calendarHtml(tz)}`);
addModel("calendar-analog", "s-cal", (tz) => analogSvg({ tz, size: "xs", numerals: true }) + calendarHtml(tz) + srTime(tz));
addModel("words", "s-words", (tz) => `<p class="words" role="timer" data-live="words"${tzA(tz)}>&nbsp;</p>` + pDate(tz));
addModel("rings", "s-rings", (tz) => ringsHtml(tz) + pDate(tz));
addModel("bigdate", "s-bigdate", (tz) => `<p class="bigdate-day" data-live="day"${tzA(tz)}>–</p><p class="bigdate-month" data-live="monthyear"${tzA(tz)}>&nbsp;</p><p class="bigdate-week" data-live="weekday"${tzA(tz)}>&nbsp;</p>${pDigital(tz, " clock-sm")}`);
const STAGE_ORDER = ["classic", "wx-digital-cal-week", "analog-date", "stack", "wx-analog", "wx-stack", "wx-analog-date", "wx-full", "h-classic", "h-analog-date", "h-stack", "h-wx-analog", "h-wx-stack", "h-wx-analog-date", "h-wx-full", "digital", "digital-date", "wx-digital", "wx-digital-cal", "flip"];
const MODELS = STAGE_ORDER.map((id) => M[id]);
const EMBED_MODELS = Object.values(M).filter((m) => !m.wx);

const ICON_CLOSE = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true" focusable="false"><path d="M6 6l12 12M18 6 6 18"/></svg>`;
const ICON_INFO = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="9"/><path d="M12 11v5.5"/><circle cx="12" cy="7.8" r="0.6" fill="currentColor"/></svg>`;
const ICON_GLOBE = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/></svg>`;

/* 24/12 saat tek düğmesi ve saniye düğmesi (sahne) */
function prefControls(t) {
  return `<button type="button" class="fmt-btn" data-fmt-toggle aria-pressed="false" aria-label="${esc(t.client.formatLabel)}" title="${esc(t.client.formatLabel)}"><span data-fmt-part="24">24</span>/<span data-fmt-part="12">12</span></button>`;
}

function clockStage(lang, { tz = "", place = null } = {}) {
  const t = I[lang];
  const s = t.stage;
  const total = MODELS.length;
  const slides = MODELS.map((m, i) => {
    const name = s.models[m.id];
    return `<section class="slide ${m.cls}" id="model-${m.id}" data-model="${m.id}" data-name="${esc(name)}" role="group" aria-roledescription="${esc(s.slideRole)}" aria-label="${esc(tpl(s.slide, { n: i + 1, total, name }))}"${i ? ' aria-hidden="true"' : ' aria-hidden="false"'}>${m.html(tz)}</section>`;
  }).join("");
  const jumpOpts = MODELS.map((m, i) => `<option value="${i}"${i ? "" : " selected"}>${i + 1}. ${esc(s.models[m.id])}</option>`).join("");
  const dots = MODELS.map(
    (m, i) => `<button type="button" class="dot" data-dot="${i}" aria-label="${esc(s.models[m.id])}" title="${esc(s.models[m.id])}"${i ? "" : ' aria-current="true"'}></button>`
  ).join("");
  const wxAttr = place ? ` data-wx-name="${esc(place.name)}" data-wx-lat="${place.lat}" data-wx-lon="${place.lon}"` : "";
  return `<section class="stage" data-carousel${wxAttr} aria-roledescription="${esc(s.carousel)}" aria-labelledby="clock-h">
  <h2 id="clock-h" class="sr-only">${esc(s.label)}</h2>
  <div class="stage-track" data-track tabindex="0" aria-label="${esc(s.hint)}">${slides}</div>
  <p class="sr-only" data-stage-name aria-live="polite">${esc(s.models[MODELS[0].id])}</p>
  <div class="fs-bar" data-fs-bar>
    <button type="button" class="icon-btn" data-fullscreen-exit aria-label="${esc(t.client.exitFullscreen)}" title="${esc(t.client.exitFullscreen)}">${ICON_CLOSE}</button>
  </div>
  <div class="stage-bar">
    <div class="dots sr-only" role="group" aria-label="${esc(s.dots)}">${dots}</div>
    <div class="stage-row">
      ${prefControls(t)}
      <button type="button" class="icon-btn info-btn" data-info-open="info-dialog" aria-haspopup="dialog" aria-label="${esc(s.info)}" title="${esc(s.info)}">${ICON_INFO}</button>
      <button type="button" class="icon-btn" data-fullscreen aria-pressed="false" aria-label="${esc(t.client.fullscreen)}" title="${esc(t.client.fullscreen)}">${ICON_FULL}</button>
      <div class="sr-nav" dir="ltr"><button type="button" class="icon-btn" data-nav="-1" aria-label="${esc(s.prev)}" title="${esc(s.prev)}">${ICON_PREV}</button><label class="stage-count-wrap"><span class="stage-count" data-stage-count>1/${total}</span><select class="stage-jump" data-stage-select aria-label="${esc(s.dots)}">${jumpOpts}</select></label><button type="button" class="icon-btn" data-nav="1" aria-label="${esc(s.next)}" title="${esc(s.next)}">${ICON_NEXT}</button></div>
    </div>
  </div>
  <noscript><p class="meta stage-note">${esc(t.ui.noscript)}</p></noscript>
</section>
<div class="source-note">
  <noscript><p class="meta source-line">${esc(s.source)}</p></noscript>
  <dialog class="info-dialog" id="info-dialog" aria-labelledby="info-title">
    <h2 id="info-title">${esc(s.infoTitle)}</h2>
    <p class="meta source-line">${esc(s.source)}</p>
    <p class="sync-line" data-sync-status>&nbsp;</p>
    <p class="meta">${esc(s.infoSeconds)}</p>
    <button type="button" class="btn" data-info-close>${esc(s.infoClose)}</button>
  </dialog>
</div>`;
}

function cityCard(lang, c) {
  return `<li class="city-card" data-world-item="${c.key}"><a href="${cityPath(lang, c)}"><span class="city-name">${esc(c[lang].name)}</span><span class="city-time" data-live="time" data-tz="${c.tz}">--:--</span><span class="city-diff" data-live="diff" data-tz="${c.tz}">&nbsp;</span></a></li>`;
}

function faqHtml(items, headingId) {
  return `<div class="faq" aria-labelledby="${headingId}">${items
    .map((f) => `<details><summary>${esc(f.q)}</summary><p>${esc(f.a)}</p></details>`)
    .join("")}</div>`;
}

/* SSS: bütün sayfaların soruları tek sayfada (/sss/) toplanır; sayfalarda yalnız oraya bağlantı kalır */
const FAQ_ORDER = ["general", "world", "diff", "converter", "planner", "poll", "calendar", "bridge", "countdowns", "stopwatch", "countdown", "alarm", "pomodoro", "week", "tz", "zones", "dst", "tzn", "unix", "worldHol", "datecalc", "timeCalc", "daysUntil", "hijriConv", "interval", "chess", "metronome", "talking", "zodiac", "embed", "prayer", "qibla", "zakat", "retire", "nature", "sky", "otd", "weather"];
const FAQ_GROUPS = {};
function addFaq(lang, id, title, href, items) {
  const m = (FAQ_GROUPS[lang] ||= new Map());
  if (!m.has(id)) m.set(id, { id, title, href, items: [] });
  const g = m.get(id);
  for (const it of items) {
    if (g.items.some((x) => x.q === it.q)) continue;
    g.items.push(it);
    searchEntries[lang].push({ t: it.q, u: `${ROUTES[lang].faq}#${id}`, d: it.a, k: `sss faq ${title}` });
  }
  return "";
}

/* Görünür ekmek kırıntısı bağlantıları kaldırıldı (9 Ekim 2026, sahibin isteği); BreadcrumbList şeması her sayfada durur */
function crumbs() {
  return "";
}

/* E-posta listesi (standart madde 1 ve 6): altbilgide çerçevesiz form; "Katıl" aydınlatma penceresini açar, metin ortak
   servisten gelir (data-aydinlatma), "Kabul ediyorum" düğmesi riza=on ile formu gönderir. Betiksizde noscript onay kutusu. */
function listForm(lang, pagePath) {
  const l = I[lang].list;
  const url = abs(pagePath);
  const dil = LIST_LANGS.includes(lang) ? lang : "en";
  const q = dil === "tr" ? "" : "?dil=en";
  return `<form class="liste-form" id="liste-form" action="https://bumbagroup.com/api/liste/katil" method="post" data-aydinlatma="https://bumbagroup.com/api/liste/aydinlatma?dil=${dil}">
    <input type="hidden" name="site" value="${esc(cfg.listKey)}">
    <input type="hidden" name="dil" value="${dil}">
    <input type="hidden" name="kaynak" value="${esc(url)}">
    <input type="hidden" name="donus" value="${esc(url)}">
    <input class="bot-tuzagi" type="text" name="web_sitesi" tabindex="-1" autocomplete="off" aria-hidden="true">
    <label for="liste-eposta" class="sr-only">${esc(l.label)}</label>
    <div class="liste-row">
      <input id="liste-eposta" type="email" name="eposta" required autocomplete="email" placeholder="${esc(l.emailLabel)}">
      <button class="btn" type="submit" id="liste-katil">${esc(l.submit)}</button>
    </div>
    <noscript><label class="check"><input type="checkbox" name="riza" value="on" required> ${esc(l.consentText)} <a href="${esc(l.noteLink + q)}" rel="noopener">${esc(l.readNote)}</a></label></noscript>
    <dialog class="info-dialog list-dialog" id="liste-pencere" aria-labelledby="liste-baslik">
      <h2 id="liste-baslik">${esc(l.popupTitle)}</h2>
      <div class="liste-metin" id="liste-metin" tabindex="0" data-loading="${esc(l.loading)}" data-fail="${esc(l.loadFail)}"></div>
      <p><a href="${esc(l.noteLink + q)}" target="_blank" rel="noopener">${esc(l.fullText)}</a></p>
      <div class="im-actions"><button type="submit" class="btn" id="liste-kabul" name="riza" value="on" disabled>${esc(l.accept)}</button><button type="button" class="btn btn-ghost" id="liste-vazgec">${esc(l.cancel)}</button></div>
    </dialog>
    <p class="liste-sonuc" id="liste-sonuc" role="status" data-ok="${esc(l.ok)}" data-err="${esc(l.formErr)}"></p>
  </form>`;
}

/* Çerez onayı (standart madde 14): ilk ziyarette bant, tercihler penceresi; seçim consent.js'te */
function cookieUi(lang) {
  const c = I[lang].consent;
  const pol = `https://bumbagroup.com/cerez-politikasi${lang === "tr" ? "" : "?dil=en"}`;
  const group = (name, text, input, tag = "") => `<div class="cg"><label class="cg-head">${input}<strong>${esc(name)}</strong>${tag ? ` <span class="cg-tag">${esc(tag)}</span>` : ""}</label><p class="meta">${esc(text)}</p></div>`;
  return `<section class="cookie-bar" id="cookie-bar" aria-label="${esc(c.barLabel)}" hidden>
  <div class="wrap cookie-row">
    <p>${esc(c.barText)} <a href="${pol}" rel="noopener">${esc(c.policy)}</a></p>
    <div class="cookie-btns"><button type="button" class="btn" data-consent-accept>${esc(c.accept)}</button><button type="button" class="btn" data-consent-reject>${esc(c.reject)}</button><button type="button" class="btn" data-consent-open aria-haspopup="dialog">${esc(c.prefs)}</button></div>
  </div>
</section>
<dialog class="info-dialog cookie-dialog" id="cookie-dialog" aria-labelledby="cookie-title">
  <h2 id="cookie-title">${esc(c.title)}</h2>
  <p class="meta">${esc(c.intro)} <a href="${pol}" rel="noopener">${esc(c.policy)}</a> · <a href="${ROUTES[lang].privacy}#${lang === "tr" ? "cerezler" : "cookies"}">${esc(c.privacy)}</a></p>
  ${group(c.necessary, c.necessaryText, `<input type="checkbox" checked disabled> `, c.alwaysOn)}
  ${group(c.stats, c.statsText, `<input type="checkbox" data-consent-stats> `)}
  ${group(c.ext, c.extText, `<input type="checkbox" data-consent-ext> `)}
  <div class="im-actions"><button type="button" class="btn" data-consent-save>${esc(c.save)}</button><button type="button" class="btn btn-ghost" data-consent-accept>${esc(c.accept)}</button><button type="button" class="btn btn-ghost" data-consent-reject>${esc(c.reject)}</button></div>
</dialog>`;
}

function badge(lang) {
  const t = I[lang].badge;
  const img = (cls, file) =>
    `<img class="${cls}" src="https://bumbagroup.com/rozet/${file}" width="${BADGE_W}" height="28" alt="${esc(t.alt)}" loading="lazy">`;
  return `<a class="rozet" href="https://bumbagroup.com/${arm}" rel="noopener" title="${esc(t.title)}">${img("only-light", `${arm}.svg`)}${img("only-dark", `${arm}-koyu.svg`)}</a>`;
}

function searchDialog(lang) {
  const u = I[lang].ui;
  return `<dialog class="search" id="search-dialog" aria-labelledby="search-title">
  <h2 id="search-title" class="sr-only">${esc(u.searchTitle)}</h2>
  <form class="search-head" role="search" method="dialog" onsubmit="return false">
    <label for="search-input" class="sr-only">${esc(u.searchLabel)}</label>
    <input id="search-input" type="search" autocomplete="off" role="combobox" aria-expanded="true" aria-controls="search-results" placeholder="${esc(u.searchPlaceholder)}">
    <button type="button" class="icon-btn" id="search-close" aria-label="${esc(u.searchClose)}">✕</button>
  </form>
  <div class="search-body">
    <p class="search-status" id="search-status" role="status" aria-live="polite"></p>
    <ul class="search-results" id="search-results" role="listbox" aria-label="${esc(u.searchResults)}"></ul>
  </div>
</dialog>`;
}

/* Ortak konum penceresi (üst bardaki konum düğmesi açar; namaz, mevsimler, hava durumu ve hava durumlu saatler kullanır) */
function placeDialog(lang) {
  const u = I[lang].ui, s = I[lang].prayer;
  const opts = PROVINCES.map((pr) => `<option value="${pr.slug}" data-lat="${pr.lat}" data-lon="${pr.lon}"${pr.slug === "istanbul" ? " selected" : ""}>${esc(provName(lang, pr))}</option>`).join("");
  return `<dialog class="info-dialog place-dialog" id="place-dialog" aria-labelledby="place-title">
  <h2 id="place-title">${esc(u.placeTitle)}</h2>
  <div class="field"><label for="place-city">${esc(s.chooseLabel)}</label><select id="place-city" data-place-city>${opts}</select></div>
  <button type="button" class="btn" data-place-geo>${esc(s.useLocation)}</button>
  <p class="meta" data-place-status role="status">&nbsp;</p>
  <p class="meta">${esc(u.placeNote)}</p>
  <button type="button" class="btn btn-ghost" data-place-close>${esc(u.placeClose)}</button>
</dialog>`;
}

/* ---------- sayfa iskeleti ---------- */
function layout(p) {
  const lang = p.lang;
  const t = I[lang];
  const u = t.ui;
  const isError = p.error;
  const canonical = p.canonicalPath ? abs(p.canonicalPath) : p.path ? abs(p.path) : "";
  const alt = p.alt; // { tr: path, en: path, ... }; ilçe sayfaları gibi bazı sayfalar yalnız Türkçe
  const altLangs = alt ? LANGS.filter((l) => alt[l]) : [];
  const altHref = (l) => (alt && alt[l]) || (p.langFallback && p.langFallback[l]) || ROUTES[l].home;
  const alternates = alt && !p.noHreflang
    ? [...altLangs.map((l) => `<link rel="alternate" hreflang="${l}" href="${abs(alt[l])}">`), `<link rel="alternate" hreflang="x-default" href="${abs(alt.tr)}">`].join("\n")
    : "";
  const clientT = {
    ...t.client,
    searchHint: u.searchHint,
    searchEmpty: u.searchEmpty,
    searchCount: u.searchCount,
    searchError: u.searchError,
    wx: weatherClient(t),
    pl: placeClient(t.prayer),
  };
  const pageCfg = {
    locale: t.locale,
    t: { ...clientT, ...(p.clientExtra && p.clientExtra.tools ? { tools: p.clientExtra.tools } : {}), ...((p.clientExtra && p.clientExtra.t) || {}) },
    ...(p.clientExtra && p.clientExtra.cities ? { cities: p.clientExtra.cities } : {}),
    ...(p.clientExtra && p.clientExtra.globe ? { globe: p.clientExtra.globe } : {}),
    ...(p.clientExtra && p.clientExtra.eclipses ? { eclipses: p.clientExtra.eclipses } : {}),
    placeData: { places: PLACES_URL, names: PLACE_NAMES_URL[lang] },
    ...(UMAMI_ID ? { umami: { id: UMAMI_ID, host: HOST } } : {}), // betiği consent.js istatistik izniyle ekler
    searchIndex: `/assets/search-${lang}.json?v=${p.searchVersion}`,
    eventsUrl: `/assets/events-${lang}.json?v=${eventsVersion[lang]}`,
    cal: { ...((p.clientExtra && p.clientExtra.cal) || {}), months: t.calendar.months, monthsTitle: t.calendar.monthsTitle, weekdays: t.calendar.weekdays, weekdaysShort: t.calendar.weekdaysShort, fullFmt: t.calendar.fullFmt, shortFmt: t.calendar.shortFmt, dateFmt: t.calendar.dateFmt, calBase: ROUTES[lang].calendar, calMin: HOL_YEARS[0], calMax: HOL_YEARS[HOL_YEARS.length - 1] },
  };
  const og = p.noindex
    ? ""
    : `<meta property="og:type" content="website">
<meta property="og:site_name" content="${esc(cfg.name)}">
<meta property="og:title" content="${esc(p.title)}">
<meta property="og:description" content="${esc(p.description)}">
<meta property="og:url" content="${canonical}">
<meta property="og:locale" content="${t.ogLocale}">
${(alt ? altLangs : LANGS).filter((l) => l !== lang).map((l) => `<meta property="og:locale:alternate" content="${I[l].ogLocale}">`).join("\n")}
<meta property="og:image" content="${abs("/og.png")}">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:image:alt" content="${esc(cfg.name)}: ${esc(t.site.tagline)}">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="${esc(p.title)}">
<meta name="twitter:description" content="${esc(p.description)}">
<meta name="twitter:image" content="${abs("/og.png")}">`;
  const ld = p.ld ? `<script type="application/ld+json">${JSON.stringify({ "@context": "https://schema.org", "@graph": p.ld }).replace(/</g, "\\u003c")}</script>` : "";

  const navLink = (key, label) =>
    `<a href="${key === "calendar" ? yearPath(lang, BUILD_YEAR) : ROUTES[lang][key]}"${p.nav === key ? ' aria-current="page"' : ""}>${esc(label)}</a>`;
  const langOpts = LANGS.map((l) => {
    const href = isError ? ROUTES[l].home : altHref(l);
    return `<option value="${href}" hreflang="${l}" lang="${l}" data-lang-switch${l === lang ? " selected" : ""}>${esc(LANG_NAMES[l])}</option>`;
  }).join("");
  const langLinks = LANGS.map((l) => `<a href="${isError ? ROUTES[l].home : altHref(l)}" hreflang="${l}" lang="${l}">${esc(LANG_NAMES[l])}</a>`).join(" ");
  /* Ayarlar: konum, SSS, arama, tema, gizlilik ve dil. Masaüstünde dişli açılır kutusu, mobilde üç çizgi menüsünün içi (aynı öğeler). */
  const settingItems = (sid, withFaq) => `<button type="button" class="set-item" data-place-open aria-haspopup="dialog">${ICON_PIN}<span>${esc(u.placeChange)}<span class="set-sub" data-place-label></span></span></button>
        ${withFaq ? `<a class="set-item" href="${ROUTES[lang].faq}">${esc(u.faq)}</a>` : ""}
        <button type="button" class="set-item" data-search-open aria-haspopup="dialog">${ICON_SEARCH}<span>${esc(u.searchOpen)}</span></button>
        <button type="button" class="set-item" data-theme-toggle aria-label="${esc(u.themeToggle)}">${ICON_MOON}${ICON_SUN}<span>${esc(u.theme)}</span></button>
        <div class="set-item set-look" role="group" aria-label="${esc(u.look)}"><span class="set-look-h">${esc(u.look)}</span>
          <label class="set-look-row"><span>${esc(u.lookFont)}</span><select class="set-select" data-look="font">${["standard", "rounded", "serif", "mono", "light"].map((k) => `<option value="${k}">${esc(u.lookFonts[k])}</option>`).join("")}</select></label>
          <label class="set-look-row"><span>${esc(u.lookColor)}</span><select class="set-select" data-look="color">${["default", "ink", "blue", "green", "red", "purple"].map((k) => `<option value="${k}">${esc(u.lookColors[k])}</option>`).join("")}</select></label>
          <span class="set-sub">${esc(u.lookNote)}</span></div>
        <a class="set-item" href="${ROUTES[lang].privacy}">${ICON_LOCK}<span>${esc(t.consent.privacy)}</span></a>
        <button type="button" class="set-item" data-consent-open aria-haspopup="dialog">${ICON_COOKIE}<span>${esc(t.consent.footerLink)}</span></button>
        <div class="set-item set-langrow">${ICON_GLOBE}<label class="sr-only" for="${sid}">${esc(u.langMenu)}</label><select id="${sid}" class="set-select" data-lang-select>${langOpts}</select></div>
        <noscript><div class="set-lang">${langLinks}</div></noscript>`;
  const settings = `<details class="settings-menu"><summary class="icon-btn" aria-label="${esc(u.settings)}" title="${esc(u.settings)}">${ICON_GEAR}</summary><div class="settings-pop">
        ${settingItems("lang-sel", true)}
      </div></details>`;
  const mobileSettings = `<div class="menu-extra">${settingItems("lang-sel-m", false)}</div>`;

  return `<!doctype html>
<html lang="${lang}" dir="${DIR(lang)}"${p.home ? ' data-home="1"' : ""}>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(p.title)}</title>
<meta name="description" content="${esc(p.description)}">
${canonical ? `<link rel="canonical" href="${canonical}">` : ""}
${alternates}
<meta name="robots" content="${p.noindex ? (p.error ? "noindex,nofollow" : "noindex,follow") : "index,follow,max-image-preview:large,max-snippet:-1"}">
<meta name="color-scheme" content="light dark">
<meta name="theme-color" media="(prefers-color-scheme: light)" content="#f7f4ee">
<meta name="theme-color" media="(prefers-color-scheme: dark)" content="#11141a">
<meta http-equiv="Content-Security-Policy" content="${esc(CSP)}">
<meta name="referrer" content="strict-origin-when-cross-origin">
${og}
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="icon" href="/favicon.ico" sizes="32x32">
<link rel="apple-touch-icon" href="/apple-touch-icon.png">
<link rel="manifest" href="/site.webmanifest">
<link rel="stylesheet" href="${ASSETS["styles.css"]}">
<script src="${ASSETS["theme.js"]}"></script>
${ld}
</head>
<body${p.needsPlace ? " data-needs-place" : ""}>
<a class="skip-link" href="#main">${esc(u.skip)}</a>
<header class="site-header">
  <div class="wrap">
    <a class="brand" href="${ROUTES[lang].home}" aria-label="${esc(u.logoAlt)}">${LOGO}<span>${esc(cfg.name)}</span></a>
    <p class="top-date" data-live="date">&nbsp;</p>
    <div class="menu" id="site-nav">
      <nav class="nav" aria-label="${esc(u.mainNav)}">${navLink("world", u.world)}${navLink("prayer", u.prayer)}${navLink("nature", u.nature)}${navLink("weather", u.weather)}${navLink("calendar", u.calendar)}${navLink("tools", u.tools)}<a class="nav-faq" href="${ROUTES[lang].faq}"${p.nav === "faq" ? ' aria-current="page"' : ""}>${esc(u.faq)}</a></nav>
      ${mobileSettings}
    </div>
    <div class="top-tools">${settings}</div>
    <button type="button" class="icon-btn nav-toggle" data-nav-toggle aria-expanded="false" aria-controls="site-nav" aria-label="${esc(u.menu)}">${ICON_MENU}</button>
  </div>
</header>
<main id="main">
${isError ? p.body : p.body.replace(/<p class="lead"/g, '<p class="lead sr-only"')}
</main>
<footer class="site-footer">
  <div class="wrap footer-row">
${listForm(lang, p.path || ROUTES[lang].home)}
    <div class="footer-sign">
      ${badge(lang)}
      <p class="sr-only">${esc(u.copyright)}</p>
      ${isError ? "" : `<p class="sr-only">${esc(u.updated)}: <time datetime="${cfg.updated}">${esc(updatedText(lang))}</time></p>`}
    </div>
  </div>
</footer>
${cookieUi(lang)}
${searchDialog(lang)}
${placeDialog(lang)}
<script type="application/json" id="page-config">${JSON.stringify(pageCfg).replace(/</g, "\\u003c")}</script>
<script defer src="${ASSETS["consent.js"]}"></script>
<script defer src="${ASSETS["main.js"]}"></script>
${["place.js", ...(p.scripts || []).filter((n) => n !== "place.js")].map((n) => `<script defer src="${ASSETS[n]}"></script>`).join("\n")}
</body>
</html>
`;
}

/* ---------- JSON-LD ---------- */
function baseLd(lang) {
  return [
    {
      "@type": "Organization",
      "@id": `${ORIGIN}/#org`,
      name: cfg.name,
      url: ORIGIN + "/",
      logo: abs("/icon-512.png"),
      email: cfg.email,
      parentOrganization: { "@type": "Organization", name: "Bumba Group", url: "https://bumbagroup.com" },
    },
    {
      "@type": "WebSite",
      "@id": `${ORIGIN}/#website`,
      url: ORIGIN + "/",
      name: cfg.name,
      description: I[lang].site.description,
      inLanguage: LANGS,
      publisher: { "@id": `${ORIGIN}/#org` },
    },
  ];
}
const breadcrumbLd = (trail) => ({
  "@type": "BreadcrumbList",
  itemListElement: trail.map((c, i) => ({ "@type": "ListItem", position: i + 1, name: c.name, item: abs(c.path) })),
});
const faqLd = (items) => ({
  "@type": "FAQPage",
  mainEntity: items.map((f) => ({ "@type": "Question", name: f.q, acceptedAnswer: { "@type": "Answer", text: f.a } })),
});
const webPageLd = (lang, pagePath, title, description) => ({
  "@type": "WebPage",
  "@id": abs(pagePath) + "#webpage",
  url: abs(pagePath),
  name: title,
  description,
  inLanguage: lang,
  dateModified: cfg.updated,
  isPartOf: { "@id": `${ORIGIN}/#website` },
});

/* ---------- sayfalar ---------- */
const pages = []; // { lang, path, html, indexable, alt }
const searchEntries = Object.fromEntries(LANGS.map((l) => [l, []]));

const GLOBE_CITIES = ["istanbul", "london", "moscow", "dubai", "mumbai", "singapore", "beijing", "tokyo", "sydney", "cairo", "johannesburg", "sao-paulo", "new-york", "los-angeles"];
function buildHome(lang) {
  const t = I[lang];
  const h = t.home;
  const pagePath = ROUTES[lang].home;
  const body = `<section class="hero hero-clock">
  <div class="wrap">
    <h1 class="sr-only">${esc(h.h1)}</h1>
    ${clockStage(lang)}
  </div>
</section>
<section class="sr-only" aria-labelledby="today-q">
  <h2 id="today-q">${esc(h.todayQ)}</h2>
  <p>${esc(h.todayA)} <span data-live="date">&nbsp;</span></p>
</section>
<section class="globe-sec" aria-label="${esc(h.globeLabel)}">
  <figure class="globe" data-globe>
    <div class="globe-bar">
      <button type="button" class="icon-btn" data-globe-zoom="in" aria-label="${esc(h.zoomIn)}" title="${esc(h.zoomIn)}">+</button>
      <button type="button" class="icon-btn info-btn" data-info-open="globe-info" aria-haspopup="dialog" aria-label="${esc(h.globeInfo)}" title="${esc(h.globeInfo)}">${ICON_INFO}</button>
      <button type="button" class="icon-btn" data-globe-zoom="out" aria-label="${esc(h.zoomOut)}" title="${esc(h.zoomOut)}">−</button>
      <button type="button" class="icon-btn globe-reset" data-globe-reset aria-label="${esc(h.resetView)}" title="${esc(h.resetView)}" hidden>${ICON_GLOBE}</button>
    </div>
    <div class="globe-view">
      <canvas class="globe-gl" aria-hidden="true"></canvas>
      <canvas class="globe-fx" role="img" aria-label="${esc(h.globeLabel)}"></canvas>
      <a class="globe-attr" data-globe-attr href="https://www.openstreetmap.org/copyright" rel="noopener" hidden>© OpenStreetMap contributors</a>
      <p class="globe-consent consent-ph" data-globe-consent hidden>${esc(t.consent.mapBlocked)} <button type="button" class="btn btn-sm" data-consent-allow-ext>${esc(t.consent.allow)}</button></p>
      <noscript><p class="meta globe-noscript">${esc(h.globeCaption)}</p></noscript>
    </div>
    <dialog class="info-dialog" id="globe-info" aria-labelledby="globe-info-title">
      <h2 id="globe-info-title">${esc(h.globeTitle)}</h2>
      <p class="meta">${esc(h.globeCaption)}</p>
      <button type="button" class="btn" data-info-close>${esc(t.stage.infoClose)}</button>
    </dialog>
  </figure>
</section>`;
  const ld = [
    ...baseLd(lang),
    webPageLd(lang, pagePath, h.title, h.description),
    {
      "@type": "WebApplication",
      name: cfg.name,
      url: abs(pagePath),
      description: t.site.description,
      applicationCategory: "UtilitiesApplication",
      operatingSystem: "Any",
      inLanguage: lang,
      isAccessibleForFree: true,
      offers: { "@type": "Offer", price: "0", priceCurrency: "TRY" },
      publisher: { "@id": `${ORIGIN}/#org` },
    },
  ];
  addFaq(lang, "general", t.faqPage.general, pagePath, [{ q: h.todayQ, a: h.todayA }, ...h.faq]);
  const e = searchEntries[lang];
  e.push({ t: t.ui.home, u: pagePath, d: h.description, k: "saat tarih bugün clock date today time now saat kaç" });
  e.push({ t: t.stage.label, u: pagePath + "#model-classic", d: t.stage.hint, k: "saat model stil style clock tam ekran full screen fullscreen" });
  MODELS.forEach((m) => e.push({ t: t.stage.models[m.id], u: `${pagePath}#model-${m.id}`, d: t.stage.label, k: "saat model clock style" }));
  e.push({ t: h.globeLabel, u: pagePath, d: h.globeCaption, k: "dünya küre gece gündüz meridyen saat dilimi globe earth day night time zone meridian" });
  e.push({ t: t.list.heading, u: pagePath + "#liste-form", d: t.list.lead, k: "e-posta email bülten newsletter liste list" });
  const globe = { earth: EARTH_URL, places: PLACES_URL, names: PLACE_NAMES_URL[lang], cities: CITIES.map((c) => ({ n: c[lang].name, lat: c.lat, lon: c.lon, tz: c.tz, ...(GLOBE_CITIES.includes(c.key) ? { m: 1 } : {}) })) };
  return { lang, key: "home", path: pagePath, title: h.title, description: h.description, body, ld, home: true, nav: "home", scripts: ["calendar.js", "place.js", "weather-core.js", "globe.js"], clientExtra: { globe } };
}

/* Dünya saatleri: Türkiye'den yalnız İstanbul; Ankara ve İzmir kendi sayfalarında durur ama dünya çizgisinde yoktur */
const WORLD_HIDE = ["ankara", "izmir"];
const WORLD_DEFAULT = ["istanbul", "london", "berlin", "cairo", "moscow", "dubai", "mumbai", "beijing", "tokyo", "sydney", "new-york", "los-angeles"];
function buildWorld(lang) {
  const t = I[lang];
  const w = t.world;
  const pagePath = ROUTES[lang].world;
  const rows = WORLD_DEFAULT.map((k) => CITIES.find((c) => c.key === k)).sort((a, b) => stdMin(a) - stdMin(b)).map(
    (c) => `<li data-world-item="${c.key}"><a href="${cityPath(lang, c)}">${esc(c[lang].name)}</a> <span class="meta">${esc(c[lang].country)} · UTC${c.std === "+00:00" ? "±00:00" : c.std}</span></li>`
  ).join("");
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: w.h1, path: pagePath },
  ];
  const body = `<section class="hero">
  <div class="wrap">
    <h1>${esc(w.h1)}</h1>
    <p class="lead">${esc(tpl(w.lead, { n: WORLD_DEFAULT.length }))}</p>
    <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    <div data-world="line"><ul class="tl-static">${rows}</ul></div>
  </div>
</section>`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, w.title, w.description), breadcrumbLd(trail)];
  searchEntries[lang].push({ t: w.h1, u: pagePath, d: w.description, k: "dünya şehir saat farkı world city time zone timezone" });
  return { lang, key: "world", path: pagePath, title: w.title, description: w.description, body, ld, nav: "world", scripts: ["world.js"], clientExtra: { cities: clientCities(lang).filter((c) => !WORLD_HIDE.includes(c.k)), t: { wc: t.world } } };
}

function buildCity(lang, c) {
  const t = I[lang];
  const ct = t.city;
  const n = c[lang];
  const v = { name: n.name, loc: n.loc || n.name, country: n.country, tz: c.tz, std: c.std === "+00:00" ? "±00:00" : c.std };
  const pagePath = cityPath(lang, c);
  const title = tpl(ct.title, v);
  const description = tpl(ct.description, v);
  const faq = [
    { q: tpl(ct.faqTime, v), a: tpl(ct.faqTimeA, v) },
    { q: tpl(ct.faqZone, v), a: tpl(ct.faqZoneA, v) },
    { q: tpl(ct.faqDst, v), a: tpl(c.dst ? ct.faqDstYesA : ct.faqDstNoA, v) },
  ];
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: ct.breadcrumbWorld, path: ROUTES[lang].world },
    { name: n.name, path: pagePath },
  ];
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(tpl(ct.h1, v))}</h1>
    <p class="lead">${esc(tpl(ct.lead, v))} ${esc(tpl(c.dst ? ct.dstYes : ct.dstNo, v))}</p>
    ${clockStage(lang, { tz: c.tz, place: { name: n.name, lat: c.lat, lon: c.lon } })}
  </div>
</section>
<section class="section" aria-label="${esc(ct.liveOffset)}">
  <div class="wrap">
    <dl class="facts">
      <div class="fact"><dt>${esc(ct.liveOffset)}</dt><dd data-live="offset" data-tz="${c.tz}">UTC</dd></div>
      <div class="fact"><dt>${esc(ct.liveDiff)}</dt><dd data-live="diff" data-tz="${c.tz}">&nbsp;</dd></div>
      ${["sunrise", "sunset", "daylen"].map((k) => `<div class="fact"><dt>${esc(ct[k])}</dt><dd data-live="${k}" data-tz="${c.tz}" data-lat="${c.lat}" data-lon="${c.lon}">–</dd></div>`).join("")}
    </dl>
    <p class="meta">${esc(ct.sunNote)}</p>
  </div>
</section>
${addFaq(lang, "world", t.ui.world, ROUTES[lang].world, faq.slice(1))}`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, title, description), breadcrumbLd(trail)];
  searchEntries[lang].push({
    t: n.name,
    u: pagePath,
    d: description,
    k: [n.name, n.loc, n.country, c.tz, ...LANGS.map((l) => I[l].ui.world), "saat kaç time"].filter(Boolean).join(" "),
  });
  return { lang, key: "city:" + c.key, path: pagePath, title, description, body, ld, nav: "world", scripts: ["place.js", "weather-core.js"] };
}

function buildPrivacy(lang) {
  const t = I[lang];
  const pr = t.privacy;
  const pagePath = ROUTES[lang].privacy;
  const sections = pr.sections
    .map((s) => {
      const ps = (s.p || []).map((x) => `<p>${md(x)}</p>`).join("");
      const ul = s.ul ? `<ul>${s.ul.map((x) => `<li>${md(x)}</li>`).join("")}</ul>` : "";
      const table = s.table
        ? `<div class="table-wrap"><table><thead><tr>${s.table.head.map((x) => `<th scope="col">${esc(x)}</th>`).join("")}</tr></thead><tbody>${s.table.rows
            .map((r) => `<tr><th scope="row"><code>${esc(r[0])}</code></th>${r.slice(1).map((x) => `<td>${esc(x)}</td>`).join("")}</tr>`)
            .join("")}</tbody></table></div>`
        : "";
      return `<h2 id="${s.id}">${esc(s.h)}</h2>${ps}${ul}${table}`;
    })
    .join("");
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: pr.h1, path: pagePath },
  ];
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(pr.h1)}</h1>
    <p class="lead">${esc(pr.lead)}</p>
    <p class="meta">${esc(t.ui.updated)}: <time datetime="${cfg.updated}">${esc(updatedText(lang))}</time></p>
  </div>
</section>
<section class="section">
  <div class="wrap prose">${sections}</div>
</section>`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, pr.title, pr.description), breadcrumbLd(trail)];
  const e = searchEntries[lang];
  e.push({ t: pr.h1, u: pagePath, d: pr.description, k: "kvkk çerez cookie gizlilik privacy umami localstorage" });
  pr.sections.forEach((s) =>
    e.push({ t: s.h, u: `${pagePath}#${s.id}`, d: (s.p && s.p[0] ? s.p[0] : s.ul ? s.ul[0] : "").replace(/\*\*|\[([^\]]+)\]\([^)]+\)/g, "$1"), k: pr.h1 })
  );
  return { lang, key: "privacy", path: pagePath, title: pr.title, description: pr.description, body, ld, nav: "privacy" };
}

/* ---------- Takvim: etkinlikler ve yıl sayfaları ---------- */
const HOL = readJson("src/data/holidays.json");
HOL.school = readJson("src/data/school.json");
HOL.exams = readJson("src/data/exams.json");
const HOL_YEARS = Object.keys(HOL.years).map(Number).sort((a, b) => a - b);
/* Tutulmalar (NASA tablosu): en büyük tutulma anı TD → UT (ΔT 69 sn, nature-calc.js ile aynı). Mevsimler sayfası ve uygulamalar. */
const ECLIPSES_SRC = JSON.parse(fs.readFileSync(path.join(SRC, "data", "eclipses.json"), "utf8"));
const ECLIPSES = ["solar", "lunar"].flatMap((kind) => ECLIPSES_SRC[kind].map((e) => ({ kind, ut: Date.parse(e.td) - 69000, type: e.type, regions: e.regions, path: e.path || [] }))).sort((a, b) => a.ut - b.ut);
const BUILD_YEAR = Math.min(Math.max(new Date().getFullYear(), HOL_YEARS[0]), HOL_YEARS[HOL_YEARS.length - 1]);
const TYPE_LABEL_KEY = { holiday: "holiday", half: "half", religious: "religious", special: "special", school: "school", exam: "exam" };
const yearPath = (lang, y) => `${ROUTES[lang].calendar}${y}/`;
const YEAR_DATA = Object.fromEntries(HOL_YEARS.map((y) => [y, computeYear(y, HOL)]));

// Arama/geri sayım/ana sayfa için tüm yılların adlandırılmış etkinlik listesi
const eventsJson = {};
for (const lang of LANGS) {
  const names = I[lang].calendar.events;
  eventsJson[lang] = JSON.stringify(
    HOL_YEARS.flatMap((y) => YEAR_DATA[y].events.map((e) => ({ d: e.date, n: names[e.key], t: e.type, k: e.key })))
  );
  write(`assets/events-${lang}.json`, eventsJson[lang]);
}
const eventsVersion = Object.fromEntries(LANGS.map((l) => [l, hash(eventsJson[l])]));

/* Takvim aboneliği (30 Ekim 2026, rakip analizi #21): her dilde iki .ics akışı, 2026–2031 bütün yıllar.
   /ics/<dil>-holidays.ics: resmî tatiller ve yarım gün arifeler; /ics/<dil>-days.ics: dini ve özel günler.
   Tüm gün etkinlikleri (DTSTART;VALUE=DATE), kimlik tarih+anahtar (abonelikte güncellenir), DTSTAMP site güncelleme tarihi. */
const icsEsc = (v) => String(v).replace(/\\/g, "\\\\").replace(/[,;]/g, "\\$&").replace(/\r?\n/g, "\\n");
const icsFold = (line) => {
  const out = []; let cur = "", bytes = 0;
  for (const ch of line) {
    const b = Buffer.byteLength(ch);
    if (bytes + b > (out.length ? 74 : 75)) { out.push(cur); cur = ""; bytes = 0; }
    cur += ch; bytes += b;
  }
  out.push(cur);
  return out.join("\r\n ");
};
const ICS_URL = (lang, kind) => `/ics/${lang}-${kind}.ics`;
for (const lang of LANGS) {
  const cal = I[lang].calendar;
  const stamp = cfg.updated.replace(/-/g, "") + "T000000Z";
  for (const [kind, types, name] of [["holidays", ["holiday", "half"], cal.icsCalHolidays], ["days", ["religious", "special"], cal.icsCalDays]]) {
    const lines = ["BEGIN:VCALENDAR", "VERSION:2.0", `PRODID:-//Bumba//Saat Tarih//${lang.toUpperCase()}`, "CALSCALE:GREGORIAN", "METHOD:PUBLISH", `X-WR-CALNAME:${icsEsc(name)}`, "REFRESH-INTERVAL;VALUE=DURATION:P7D", "X-PUBLISHED-TTL:P7D"];
    for (const y of HOL_YEARS) {
      for (const e of YEAR_DATA[y].events) {
        if (!types.includes(e.type) || !e.date.startsWith(String(y))) continue;
        const d = e.date.replace(/-/g, ""), end = addDays(e.date, 1).replace(/-/g, "");
        const title = cal.events[e.key] + (e.type === "half" ? ` (${cal.icsHalf})` : "");
        const desc = [cal.types[TYPE_LABEL_KEY[e.type]], YEAR_DATA[y].projected && (e.type !== "special") && /^(rb|kb|ramadan|kadir|mirac|berat|regaib|mawlid|hijri|ashura|ucAylar)/.test(e.key) ? cal.icsProjected : "", `${abs(yearPath(lang, y))}`].filter(Boolean).join(" · ");
        lines.push("BEGIN:VEVENT", `UID:${d}-${e.key}-${lang}@time.bumba.tr`, `DTSTAMP:${stamp}`, `DTSTART;VALUE=DATE:${d}`, `DTEND;VALUE=DATE:${end}`, `SUMMARY:${icsEsc(title)}`, `DESCRIPTION:${icsEsc(desc)}`, "TRANSP:TRANSPARENT", "END:VEVENT");
      }
    }
    lines.push("END:VCALENDAR");
    write(ICS_URL(lang, kind).slice(1), lines.map(icsFold).join("\r\n") + "\r\n");
  }
}

function monthTable(lang, year, month, cy, ev) {
  const cal = I[lang].calendar;
  const days = daysInMonth(year, month);
  const lead = dowOf(iso(year, month, 1));
  const head = cal.weekdaysShort.map((w) => `<th scope="col">${esc(w)}</th>`).join("");
  let rows = "";
  let d = 1 - lead;
  while (d <= days) {
    let tr = "";
    for (let c = 0; c < 7; c++, d++) {
      if (d < 1 || d > days) { tr += "<td></td>"; continue; }
      const date = iso(year, month, d);
      const es = ev.byDate[date] || [];
      const rank = ["holiday", "half", "religious", "special"].find((t) => es.some((e) => e.type === t));
      const school = es.some((e) => e.type === "school") || inRange(date, cy.schoolRanges);
      const cls = [rank && `d-${rank}`, !rank && school && "d-school", inRange(date, cy.ramadans) && "d-ramadan", c >= 5 && "d-weekend"].filter(Boolean).join(" ");
      const label = es.length ? `<span class="sr-only">, ${esc(es.map((e) => `${cal.events[e.key]} (${cal.types[TYPE_LABEL_KEY[e.type]]})`).join("; "))}</span>` : "";
      const title = es.length ? ` title="${esc(es.map((e) => cal.events[e.key]).join("; "))}"` : "";
      tr += `<td${cls ? ` class="${cls}"` : ""} data-d="${date}"${title}${es.length ? "" : ` title="${esc(cal.dayNone)}"`} tabindex="0" role="button">${d}${label}</td>`;
    }
    rows += `<tr>${tr}</tr>`;
  }
  return `<table class="ymonth"><caption>${esc((cal.monthsTitle || cal.months)[month - 1])}</caption><thead><tr>${head}</tr></thead><tbody>${rows}</tbody></table>`;
}

function buildCalendarYear(lang, year, { index = false } = {}) {
  const t = I[lang];
  const cal = t.calendar;
  const f = makeFmt(cal, lang);
  const cy = YEAR_DATA[year];
  const counts = holidayCounts(cy.events);
  const byDate = {};
  /* Sınav günleri takvimde gösterilmez (geri sayım sayfaları kendi verisini kullanır) */
  const shown = cy.events.filter((e) => e.type !== "exam");
  shown.forEach((e) => (byDate[e.date] ||= []).push(e));
  const date = (key, i = 0) => cy.events.filter((e) => e.key === key)[i];
  const rbDays = cy.events.filter((e) => e.key.startsWith("rb") && e.key !== "rbEve").map((e) => e.date);
  const kbDays = cy.events.filter((e) => e.key.startsWith("kb") && e.key !== "kbEve").map((e) => e.date);
  const rbEve = date("rbEve")?.date, kbEve = date("kbEve")?.date;
  const ramadan = cy.ramadans.find((r) => r.from.startsWith(String(year)) && r.to) || cy.ramadans[0];
  const kadir = date("kadir")?.date;
  const v = {
    year,
    ramadan: ramadan && ramadan.to ? f.range(ramadan.from, ramadan.to) : "",
    rb: rbDays.length ? f.range(rbDays[0], rbDays[rbDays.length - 1]) : "",
    kb: kbDays.length ? f.range(kbDays[0], kbDays[kbDays.length - 1]) : "",
    eve: "", n: counts.n, w: counts.w,
    start: ramadan ? f.short(ramadan.from) : "", end: ramadan && ramadan.to ? f.short(ramadan.to) : "",
    kadir: kadir ? f.short(kadir) : "",
  };
  const pagePath = index ? ROUTES[lang].calendar : yearPath(lang, year);
  const canonicalPath = yearPath(lang, year);
  const title = tpl(cal.title, v), description = tpl(cal.description, v);
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: tpl(cal.h1, v), path: canonicalPath },
  ];
  const faq = [
    { q: tpl(cal.faqRb, v), a: tpl(cal.faqRbA, { ...v, eve: rbEve ? f.short(rbEve) : "" }) },
    { q: tpl(cal.faqKb, v), a: tpl(cal.faqKbA, { ...v, eve: kbEve ? f.short(kbEve) : "" }) },
    { q: tpl(cal.faqRamadan, v), a: tpl(cal.faqRamadanA, v) },
    { q: tpl(cal.faqHolidays, v), a: tpl(cal.faqHolidaysA, v) },
  ];
  const ev = { byDate };
  const months = Array.from({ length: 12 }, (_, i) => `<div class="ymonth-wrap">${monthTable(lang, year, i + 1, cy, ev)}</div>`).join("");
  const legend = ["holiday", "half", "religious", "ramadan", "special", ...(cy.events.some((e) => e.type === "school") ? ["school"] : []), "weekend", "today"]
    .map((k) => `<li><span class="lg lg-${k}" aria-hidden="true"></span>${esc(cal.types[k])}</li>`).join("");
  const seenRow = new Set();
  const rows = shown
    .map((e) => { const first = !seenRow.has(e.date); seenRow.add(e.date); return `<tr data-d="${e.date}"${first ? ` id="e-${e.date}"` : ""}><th scope="row">${esc(f.short(e.date))}</th><td>${esc(cal.weekdays[dowOf(e.date)])}</td><td>${esc(cal.events[e.key])}</td><td>${esc(cal.types[TYPE_LABEL_KEY[e.type]])}</td><td data-left></td></tr>`; })
    .join("");
  const yearLinks = HOL_YEARS.map((y) => `<li><a href="${yearPath(lang, y)}"${y === year ? ' aria-current="page"' : ""}>${y}</a></li>`).join("");
  const prev = HOL_YEARS.includes(year - 1) ? `<a href="${yearPath(lang, year - 1)}" rel="prev">${esc(tpl(cal.prevYear, { year: year - 1 }))}</a>` : "";
  const next = HOL_YEARS.includes(year + 1) ? `<a href="${yearPath(lang, year + 1)}" rel="next">${esc(tpl(cal.nextYear, { year: year + 1 }))}</a>` : "";
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(tpl(cal.h1, v))}</h1>
    <p class="lead">${esc(tpl(cal.lead, v))}</p>
    <nav class="year-nav" aria-label="${esc(cal.yearNav)}">${prev}<ul>${yearLinks}</ul>${next}</nav>
    ${cy.projected ? `<p class="meta">${esc(cal.projected)}</p>` : ""}
  </div>
</section>
<section class="section" aria-labelledby="legend-h">
  <div class="wrap">
    <h2 id="legend-h" class="sr-only">${esc(cal.legendLabel)}</h2>
    <ul class="legend" aria-label="${esc(cal.legendLabel)}">${legend}</ul>
    <div class="year-grid" data-year="${year}">${months}</div>
    <dialog class="info-dialog day-dialog" id="day-dialog" aria-labelledby="day-title">
      <h2 id="day-title" data-day-title></h2>
      <ul class="day-list" data-day-list></ul>
      <p class="meta" data-day-meta></p>
      <div class="im-actions"><a class="btn" href="#events" data-day-link hidden>${esc(cal.dayToList)}</a><button type="button" class="btn btn-ghost" data-day-close>${esc(cal.dayClose)}</button></div>
    </dialog>
  </div>
</section>
<section class="section" id="events" aria-labelledby="events-h">
  <div class="wrap">
    <h2 id="events-h">${esc(tpl(cal.eventsHeading, v))}</h2>
    <div class="table-wrap"><table class="events-table"><caption>${esc(tpl(cal.tableCaption, v))}</caption><thead><tr><th scope="col">${esc(cal.colDate)}</th><th scope="col">${esc(cal.colDay)}</th><th scope="col">${esc(cal.colEvent)}</th><th scope="col">${esc(cal.colType)}</th><th scope="col">${esc(cal.colLeft)}</th></tr></thead><tbody>${rows}</tbody></table></div>
  </div>
</section>
<section class="section cal-export" aria-labelledby="export-h">
  <div class="wrap">
    <h2 id="export-h">${esc(cal.icsHeading)}</h2>
    ${["holidays", "days"].map((k) => { const u = ICS_URL(lang, k); return `<div class="cal-feed"><h3>${esc(k === "holidays" ? cal.icsHolidays : cal.icsDays)}</h3><p class="share-row"><a class="toggle" href="${u}" download>${esc(cal.icsDownload)}</a><a class="toggle" href="${ORIGIN.replace(/^https?:/, "webcal:")}${u}">${esc(cal.icsSubscribe)}</a><a class="toggle" href="https://calendar.google.com/calendar/render?cid=${encodeURIComponent(ORIGIN.replace(/^https?:/, "webcal:") + u)}" target="_blank" rel="noopener">${esc(cal.icsGoogle)}</a></p></div>`; }).join("")}
    <p class="meta">${esc(cal.icsNote)}</p>
    <p><button type="button" class="btn" data-print>${esc(cal.print)}</button></p>
    <p class="meta">${esc(cal.printNote)}</p>
  </div>
</section>
${addFaq(lang, "calendar", t.faqPage.calendar, yearPath(lang, BUILD_YEAR), faq)}`;
  const ld = [...baseLd(lang), webPageLd(lang, canonicalPath, title, description), breadcrumbLd(trail)];
  const key = index ? "calendar-index" : `calendar:${year}`;
  if (!index) {
    const e = searchEntries[lang];
    e.push({ t: tpl(cal.h1, v), u: canonicalPath, d: description, k: "takvim resmi tatil bayram arife ramazan kandil calendar holiday ramadan eid " + year });
    for (const k of ["rb1", "kb1", "ramadanStart"]) {
      const ee = date(k);
      if (ee) e.push({ t: `${cal.events[k]} ${year}`, u: `${canonicalPath}#events`, d: f.full(ee.date), k: "takvim bayram ramazan kurban calendar eid ramadan " + year });
    }
  }
  return { lang, key, path: pagePath, title, description, body, ld, nav: "calendar", scripts: ["calendar.js"], clientExtra: { cal: { year }, t: { cald: { none: cal.dayNone, week: cal.dayWeek, doy: cal.dayDoy, today: cal.dayToday, inDays: cal.dayIn, ago: cal.dayAgo, close: cal.dayClose, types: cal.types } } }, noindex: index, canonicalPath: index ? canonicalPath : undefined, noHreflang: index, mapKey: index ? undefined : key };
}

/* ---------- Araç sayfaları ---------- */
const TOOL_KEYS = ["stopwatch", "countdown", "alarm", "pomodoro"];
const btn = (cls, attrs, label) => `<button type="button" class="${cls}" ${attrs}>${esc(label)}</button>`;
const numField = (id, label, attrs) => `<div class="field"><label for="${id}">${esc(label)}</label><input id="${id}" type="number" inputmode="numeric" ${attrs}></div>`;
const ringBanner = (tt, name) => `<div class="ring-banner" role="alertdialog" aria-label="${esc(tt.alarm.ringing)}" data-ring-banner hidden><p class="ring-title" data-ring-title>${esc(tt.alarm.ringing)}</p>${name === "alarm" ? `<div class="ring-media" data-ring-media hidden></div>` : ""}<div class="tool-actions">${btn("btn", "data-ring-stop", tt.common.close)}${name === "alarm" ? btn("toggle", "data-ring-snooze", tt.alarm.snooze) : ""}</div></div>`;
const ALARM_SOUNDS = ["chime", "bell", "rise", "digital", "soft"];
const POMO_TAGS = ["none", "work", "study", "personal", "other"];
const soundRow = (tt) => `<div class="tool-extra"><label class="check-inline"><input type="checkbox" data-sound checked> ${esc(tt.common.sound)}</label>${btn("toggle", "data-test-sound", tt.common.testSound)}${btn("toggle", "data-notify", tt.common.notify)}</div>`;

function toolBody(lang, key, preset = {}) {
  const tt = I[lang].tools;
  const s = tt[key];
  if (key === "stopwatch")
    return `<p class="big-time" role="timer" data-sw-display>00:00:00.00</p>
<div class="tool-actions">${btn("btn", "data-sw-start", s.start)}${btn("toggle", "data-sw-lap disabled", s.lap)}${btn("toggle", "data-sw-reset", s.reset)}</div>
<p class="meta">${esc(s.hint)}</p>
<p class="sr-only" role="status" aria-live="polite" data-sw-announce></p>
<p class="meta" data-sw-empty>${esc(s.empty)}</p>
<div class="table-wrap laps" data-sw-laps hidden><table><caption>${esc(s.lapsCaption)}</caption><thead><tr><th scope="col">${esc(s.lapNo)}</th><th scope="col">${esc(s.lapTime)}</th><th scope="col">${esc(s.total)}</th></tr></thead><tbody data-sw-body></tbody></table></div>
<div class="tool-extra" data-sw-csvrow hidden>${btn("toggle", "data-sw-csv", s.csv)}</div>`;
  if (key === "countdown")
    return `<div class="tabs" role="tablist" aria-label="${esc(s.panel)}">
  <button type="button" role="tab" id="tab-timer" aria-selected="true" aria-controls="cd-timer" data-cd-tab="timer">${esc(s.tabTimer)}</button>
  <button type="button" role="tab" id="tab-date" aria-selected="false" aria-controls="cd-date" tabindex="-1" data-cd-tab="date">${esc(s.tabDate)}</button>
</div>
<div role="tabpanel" id="cd-timer" aria-labelledby="tab-timer" data-cd-panel="timer">
  <div class="inputs-row">${numField("cd-h", s.hours, `min="0" max="99" value="${preset.sec ? Math.floor(preset.sec / 3600) : 0}" data-cd-h`)}${numField("cd-m", s.minutes, `min="0" max="59" value="${preset.sec ? Math.floor((preset.sec % 3600) / 60) : 5}" data-cd-m`)}${numField("cd-s", s.seconds, 'min="0" max="59" value="0" data-cd-s')}</div>
  <div class="presets" role="group" aria-label="${esc(s.presetsLabel)}">${[1, 5, 10, 15, 30, 60].map((n) => btn("chip", `data-cd-preset="${n}"`, tpl(s.presetMin, { n }))).join("")}</div>
</div>
<div role="tabpanel" id="cd-date" aria-labelledby="tab-date" data-cd-panel="date" hidden>
  <div class="field"><label for="cd-target">${esc(s.targetLabel)}</label><input id="cd-target" type="datetime-local" data-cd-target></div>
  <div class="field"><label for="cd-name">${esc(s.nameLabel)}</label><input id="cd-name" type="text" maxlength="40" placeholder="${esc(s.namePlaceholder)}" data-cd-nameinput></div>
  <p class="meta">${esc(s.quickHeading)}</p>
  <div class="presets" data-cd-quick>${btn("chip", 'data-cd-quick-newyear', s.quickNewYear)}</div>
</div>
<p class="cd-name" data-cd-name hidden></p>
<p class="big-time" role="timer" aria-label="${esc(s.remaining)}" data-cd-display>${preset.sec ? `${hh(Math.floor(preset.sec / 3600))}:${String(Math.floor((preset.sec % 3600) / 60)).padStart(2, "0")}:00` : "00:00:00"}</p>
<progress class="tool-progress" max="100" value="0" data-cd-progress aria-hidden="true"></progress>
<p class="cd-message" role="status" data-cd-message hidden></p>
${preset.sec ? `<p class="meta" data-cd-endsat data-tpl="${esc(tt.presets.endsAt)}" data-tpl-run="${esc(tt.presets.endsAtRun)}">&nbsp;</p>` : ""}
<div class="tool-actions">${btn("btn", "data-cd-start", s.start)}${btn("toggle", "data-cd-add hidden", s.addMinute)}${btn("toggle", "data-cd-reset", s.reset)}${btn("toggle", "data-cd-copy", s.copyLink)}</div>
<p class="meta">${esc(s.shareNote)}</p>
${soundRow(tt)}
${ringBanner(tt, "countdown")}`;
  if (key === "alarm")
    return `<form class="alarm-form" data-alarm-form>
  <div class="inputs-row">
    <div class="field"><label for="al-time">${esc(s.timeLabel)}</label><input id="al-time" type="time" value="${preset.time || "07:00"}" required data-al-time></div>
    <div class="field grow"><label for="al-label">${esc(s.labelLabel)}</label><input id="al-label" type="text" maxlength="40" placeholder="${esc(s.labelPlaceholder)}" data-al-label></div>
  </div>
  <fieldset class="days"><legend>${esc(s.repeatLabel)}</legend>
    ${s.dayNames.map((d, i) => `<label class="day"><input type="checkbox" value="${i}" data-al-day><span>${esc(d)}</span></label>`).join("")}
    <span class="presets">${btn("chip", 'data-al-preset="none"', s.once)}${btn("chip", 'data-al-preset="weekdays"', s.weekdays)}${btn("chip", 'data-al-preset="all"', s.every)}</span>
  </fieldset>
  <div class="inputs-row al-music">
    <div class="field"><label for="al-sound">${esc(s.soundLabel)}</label><select id="al-sound" data-al-sound>${ALARM_SOUNDS.map((k) => `<option value="${k}">${esc(s.sounds[k])}</option>`).join("")}</select></div>
    <div class="field grow"><label for="al-yt">${esc(s.ytLabel)}</label><input id="al-yt" type="url" inputmode="url" autocomplete="off" placeholder="https://www.youtube.com/watch?v=…" data-al-yt></div>
  </div>
  <p class="meta al-yt-note" data-al-yt-note>${esc(s.ytNote)}</p>
  <button class="btn" type="submit">${esc(s.add)}</button>
</form>
<h2 class="tool-h2">${esc(s.listHeading)}</h2>
<p class="meta" data-al-next>${esc(s.nextNone)}</p>
<ul class="alarm-list" data-al-list></ul>
<p class="meta" data-al-empty>${esc(s.none)}</p>
<div class="tool-extra"><label class="check-inline"><input type="checkbox" data-wake> ${esc(s.wake)}</label><label class="check-inline"><input type="checkbox" data-sound checked> ${esc(tt.common.sound)}</label>${btn("toggle", "data-test-sound", tt.common.testSound)}${btn("toggle", "data-notify", tt.common.notify)}</div>
<p class="meta">${esc(s.wakeNote)}</p>
<p class="meta">${esc(s.note)}</p>
${ringBanner(tt, "alarm")}`;
  // pomodoro
  return `<div class="inputs-row pomo-task"><div class="field grow"><label for="po-task">${esc(s.taskLabel)}</label><input id="po-task" type="text" maxlength="60" placeholder="${esc(s.taskPlaceholder)}" data-pomo-task></div><div class="field"><label for="po-tag">${esc(s.tagLabel)}</label><select id="po-tag" data-pomo-tag>${POMO_TAGS.map((k) => `<option value="${k}">${esc(s.tags[k])}</option>`).join("")}</select></div></div>
<p class="phase" data-pomo-phase aria-live="polite">${esc(s.work)}</p>
<p class="big-time" role="timer" aria-label="${esc(s.phaseLabel)}" data-pomo-display>25:00</p>
<progress class="tool-progress" max="100" value="0" data-pomo-progress aria-hidden="true"></progress>
<p class="meta" data-pomo-round>&nbsp;</p>
<div class="tool-actions">${btn("btn", "data-pomo-start", s.start)}${btn("toggle", "data-pomo-skip", s.skip)}${btn("toggle", "data-pomo-reset", s.reset)}</div>
<p class="meta" data-pomo-count>&nbsp;</p>
<p class="pomo-goal" data-pomo-goal aria-live="polite">&nbsp;</p>
<details class="pomo-settings"><summary>${esc(s.settings)}</summary>
  <div class="inputs-row">${numField("po-w", s.workMin, 'min="1" max="180" value="25" data-pomo-w')}${numField("po-s", s.shortMin, 'min="1" max="60" value="5" data-pomo-s')}${numField("po-l", s.longMin, 'min="1" max="120" value="15" data-pomo-l')}${numField("po-r", s.roundsLabel, 'min="2" max="12" value="4" data-pomo-r')}${numField("po-g", s.goalLabel, 'min="1" max="24" value="4" data-pomo-g')}</div>
</details>
<div class="inputs-row pomo-noise"><div class="field"><label for="po-noise">${esc(s.noise)}</label><select id="po-noise" data-pomo-noise>${["off", "white", "pink", "brown", "rain"].map((k) => `<option value="${k}">${esc(s["noise" + k[0].toUpperCase() + k.slice(1)])}</option>`).join("")}</select></div><div class="field"><label for="po-vol">${esc(s.volume)}</label><input id="po-vol" type="range" min="0" max="100" value="40" data-pomo-vol></div></div>
<p class="meta">${esc(s.noiseNote)}</p>
${soundRow(tt)}
<section class="pomo-stats" aria-labelledby="pomo-stats-h">
  <h2 id="pomo-stats-h" class="tool-h2">${esc(s.statsHeading)}</h2>
  <p class="meta" data-pomo-streak hidden></p>
  <dl class="facts"><div class="fact"><dt>${esc(s.today)}</dt><dd data-pomo-st-today>–</dd></div><div class="fact"><dt>${esc(s.week)}</dt><dd data-pomo-st-week>–</dd></div><div class="fact"><dt>${esc(s.total)}</dt><dd data-pomo-st-total>–</dd></div></dl>
  <figure class="pomo-chart"><div class="pomo-bars" data-pomo-bars aria-hidden="true"></div><figcaption class="meta">${esc(s.chartCaption)}</figcaption></figure>
  <h3 class="tool-h3">${esc(s.topTasks)}</h3>
  <ul class="pomo-tasks" data-pomo-tasks></ul>
  <h3 class="tool-h3" data-pomo-tagsh hidden>${esc(s.byTag)}</h3>
  <ul class="pomo-tasks" data-pomo-bytag hidden></ul>
  <p class="meta" data-pomo-empty>${esc(s.empty)}</p>
  <div class="tool-actions">${btn("toggle", "data-pomo-csv", s.csv)}${btn("toggle", "data-pomo-clear", s.clear)}</div>
</section>`;
}

function buildTool(lang, key, preset = null) {
  const t = I[lang];
  const tt = t.tools;
  const s = tt[key];
  const pagePath = preset ? preset.path : ROUTES[lang][key];
  const page = preset || s;
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: t.ui.tools, path: ROUTES[lang].tools },
    ...(preset ? [{ name: s.h1, path: ROUTES[lang][key] }] : []),
    { name: page.h1, path: pagePath },
  ];
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(page.h1)}</h1>
    <p class="lead">${esc(page.lead)}</p>
    <section class="tool" data-tool="${key}"${preset && preset.sec ? ` data-cd-fixed="${preset.sec * 1000}"` : ""} aria-labelledby="tool-h">
      <div class="tool-head"><h2 id="tool-h" class="tool-title">${esc(s.panel)}</h2><button type="button" class="icon-btn" data-fs aria-pressed="false" aria-label="${esc(tt.common.fullscreen)}" title="${esc(tt.common.fullscreen)}">${ICON_FULL}</button></div>
      ${toolBody(lang, key, preset || {})}
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>
    ${key === "alarm" || key === "countdown" || key === "pomodoro" ? `<p class="meta">${esc(tt.common.pageOpenNote)}</p>` : ""}
  </div>
</section>
${preset ? preset.extra || "" : addFaq(lang, key, tt.hub.cards[key].name, pagePath, s.faq)}`;
  const ld = [
    ...baseLd(lang),
    webPageLd(lang, pagePath, page.title, page.description),
    { "@type": "WebApplication", name: page.h1, url: abs(pagePath), description: page.description, applicationCategory: "UtilitiesApplication", operatingSystem: "Any", inLanguage: lang, isAccessibleForFree: true, offers: { "@type": "Offer", price: "0", priceCurrency: "TRY" }, publisher: { "@id": `${ORIGIN}/#org` } },
    breadcrumbLd(trail),
  ];
  searchEntries[lang].push({ t: page.h1, u: pagePath, d: page.description, k: preset ? preset.searchK : tt.hub.cards[key].name + " " + { stopwatch: "kronometre stopwatch tur lap", countdown: "geri sayım countdown timer zamanlayıcı yılbaşı bayram", alarm: "alarm saati alarm clock uyandırma", pomodoro: "pomodoro odak focus mola break" }[key] });
  return { lang, key: preset ? preset.key : `tool:${key}`, path: pagePath, title: page.title, description: page.description, body, ld, nav: "tools", scripts: ["tools.js"], clientExtra: { tools: tt, cal: {} } };
}

/* Hazır süre ve alarm sayfaları (30 Ekim 2026, rakip analizi #20): "5 dakika zamanlayıcı", "07:00 alarm kur".
   Araç aynıdır (tools.js); sayfa süreyi/saati önceden ayarlar. Adresler: /geri-sayim/5-dakika/, /alarm/07-00/
   (diğer dillerde İngilizce kısa ad: /en/countdown/5-minutes/). Sayfalar arası bağlantı yok; sitemap ve aramada. */
const TIMER_PRESETS = [1, 2, 3, 5, 10, 15, 20, 25, 30, 40, 45, 60, 90, 120];
const ALARM_PRESETS = [...Array.from({ length: 17 }, (_, i) => 240 + i * 30), 315, 345, 375, 405, 435, 465, 495, 525].sort((a, b) => a - b).map((m) => `${String(Math.floor(m / 60)).padStart(2, "0")}:${String(m % 60).padStart(2, "0")}`);
const capFirst = (lang, x) => x.charAt(0).toLocaleUpperCase(lang) + x.slice(1);
const timerSlug = (lang, m) => {
  const l = lang === "tr" ? "tr" : "en";
  const lab = I[l].tools.presets.durations[m].label;
  return trSlug(lab);
};
const time12 = (t) => { const [h, m] = t.split(":").map(Number); return `${((h + 11) % 12) + 1}:${String(m).padStart(2, "0")} ${h < 12 ? "AM" : "PM"}`; };

function buildTimerPreset(lang, m) {
  const pr = I[lang].tools.presets;
  const d = pr.durations[m];
  const v = { label: d.label, Label: capFirst(lang, d.label), name: d.name, Name: capFirst(lang, d.name), s: (m * 60).toLocaleString(I[lang].locale), ms: (m * 60000).toLocaleString(I[lang].locale) };
  return buildTool(lang, "countdown", {
    key: `timer:${m}`, path: `${ROUTES[lang].countdown}${timerSlug(lang, m)}/`, sec: m * 60,
    title: tpl(pr.timerTitle, v), description: tpl(pr.timerDescription, v), h1: v.Name, lead: tpl(pr.timerLead, v),
    extra: `<section class="section"><div class="wrap"><p class="meta">${esc(tpl(pr.timerFacts, v))}</p></div></section>`,
    searchK: `${d.label} ${d.name} zamanlayıcı geri sayım sayaç timer countdown`,
  });
}

function buildAlarmPreset(lang, time) {
  const pr = I[lang].tools.presets;
  const [h, mi] = time.split(":").map(Number);
  const v = { time, time12: time12(time) };
  const fmt = (min) => { const x = ((min % 1440) + 1440) % 1440; const t = `${hh(Math.floor(x / 60))}:${String(x % 60).padStart(2, "0")}`; return lang === "en" ? `${time12(t)} (${t})` : t; };
  const rows = [6, 5, 4].map((c) => `<li>${esc(tpl(pr.sleepRow, { bed: fmt(h * 60 + mi - c * 90 - 15), cycles: c, hours: (c * 1.5).toLocaleString(I[lang].locale) }))}</li>`).join("");
  const extra = `<section class="section" aria-labelledby="sleep-h"><div class="wrap">
    <h2 id="sleep-h">${esc(pr.sleepHeading)}</h2>
    <p>${esc(tpl(pr.sleepText, v))}</p>
    <ul>${rows}</ul>
    <p class="meta">${esc(pr.sleepNote)}</p>
  </div></section>`;
  return buildTool(lang, "alarm", {
    key: `alarmAt:${time}`, path: `${ROUTES[lang].alarm}${time.replace(":", "-")}/`, time,
    title: tpl(pr.alarmTitle, v), description: tpl(pr.alarmDescription, v), h1: tpl(pr.alarmH1, v), lead: tpl(pr.alarmLead, v), extra,
    searchK: `alarm ${time} ${v.time12} alarm kur uyandır wake up`,
  });
}

const svgArt = (inner) => `<svg viewBox="0 0 96 64" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" focusable="false">${inner}</svg>`;
const TOOL_ART = {
  stopwatch: svgArt('<circle cx="48" cy="37" r="20" class="af"/><path d="M42 10h12M48 10v7M48 37l9-9M70 20l4-4"/>'),
  countdown: svgArt('<path d="M32 10h32M32 54h32M35 10c0 14 10 14 13 22-3 8-13 8-13 22M61 10c0 14-10 14-13 22 3 8 13 8 13 22" class="af"/><path d="M42 48h12" />'),
  alarm: svgArt('<circle cx="48" cy="36" r="19" class="af"/><path d="M48 25v11l8 5M28 14l-8 8M68 14l8 8M36 56l-5 5M60 56l5 5"/>'),
  pomodoro: svgArt('<path d="M48 18c-18 0-26 10-26 21s10 17 26 17 26-6 26-17-8-21-26-21z" class="af"/><path d="M48 18c-2-5-6-8-12-8 3 4 6 6 12 8zm0 0c2-5 6-8 12-8-3 4-6 6-12 8zM48 18V9"/>'),
  converter: svgArt('<circle cx="26" cy="30" r="15" class="af"/><path d="M26 21v9l6 4"/><circle cx="70" cy="34" r="15" class="af"/><path d="M70 25v9l6 4M40 12h16m-5-5 5 5-5 5M56 56H40m5 5-5-5 5-5"/>'),
  planner: svgArt('<rect x="16" y="14" width="64" height="42" rx="6" class="af"/><path d="M16 26h64M32 8v10M64 8v10"/><circle cx="34" cy="40" r="4"/><circle cx="48" cy="40" r="4"/><circle cx="62" cy="40" r="4"/>'),
  poll: svgArt('<rect x="16" y="12" width="64" height="48" rx="6" class="af"/><path d="M28 26h8M28 38h8M28 50h8M46 24l4 4 7-8M46 36h10M46 48l4 4 7-8"/><path d="M64 26h6M64 38h6M64 50h6"/>'),
  datecalc: svgArt('<rect x="14" y="14" width="46" height="42" rx="6" class="af"/><path d="M14 26h46M28 8v10M46 8v10"/><path d="M74 28v20M64 38h20"/>'),
  diff: svgArt('<circle cx="24" cy="32" r="14" class="af"/><path d="M24 24v8l5 3"/><circle cx="72" cy="32" r="14" class="af"/><path d="M72 24v8l5 3M42 32h12m-4-4 4 4-4 4"/>'),
  embed: svgArt('<rect x="10" y="12" width="76" height="40" rx="6" class="af"/><path d="M38 24l-9 8 9 8M58 24l9 8-9 8M52 22l-8 20"/>'),
  week: svgArt('<rect x="16" y="14" width="64" height="42" rx="6" class="af"/><path d="M16 26h64M32 8v10M64 8v10M30 36h8m6 0h8m6 0h8M30 46h8m6 0h8"/>'),
  tz: svgArt('<circle cx="48" cy="32" r="22" class="af"/><path d="M26 32h44M48 10c9 7 9 37 0 44M48 10c-9 7-9 37 0 44"/>'),
  worldHol: svgArt('<rect x="12" y="14" width="46" height="40" rx="6" class="af"/><path d="M12 26h46M26 8v10M44 8v10M24 38h6M38 38h6"/><circle cx="72" cy="40" r="14" class="af"/><path d="M58 40h28M72 26c5 4 5 24 0 28M72 26c-5 4-5 24 0 28"/>'),
  zones: svgArt('<circle cx="34" cy="32" r="20" class="af"/><path d="M14 32h40M34 12c7 6 7 34 0 40M34 12c-7 6-7 34 0 40"/><circle cx="70" cy="40" r="14" class="af"/><path d="M70 32v8l5 3"/>'),
  tzn: svgArt('<circle cx="34" cy="34" r="20" class="af"/><path d="M34 22v12l8 5"/><rect x="58" y="18" width="30" height="34" rx="4"/><path d="M64 28h18M64 35h18M64 42h12"/>'),
  dst: svgArt('<circle cx="30" cy="30" r="10" class="af"/><path d="M30 12v4M30 44v4M12 30h4M44 30h4M17 17l3 3M40 40l3 3M17 43l3-3M40 20l3-3"/><path d="M72 18a14 14 0 1 0 10 24 12 12 0 0 1-10-24z" class="af"/><path d="M50 52h20m-5-5 5 5-5 5"/>'),
  unix: svgArt('<rect x="12" y="14" width="72" height="36" rx="6" class="af"/><path d="M24 26v12M30 26h6v12h-6zM44 26v12M50 26h6v12h-6zM64 26v12M70 26h6v12h-6z"/>'),
  bridge: svgArt('<path d="M8 46h80M14 46V30M82 46V30M14 30c10 0 18-10 34-10s24 10 34 10M30 46V26M48 46V20M66 46V26" class="af"/><path d="M8 54h80"/>'),
  qibla: svgArt('<circle cx="48" cy="34" r="24" class="af"/><path d="M48 6v6M48 56v6M20 34h-6M82 34h-6M48 34l12-14-4 18z"/><rect x="62" y="12" width="10" height="10" rx="1"/>'),
  zakat: svgArt('<ellipse cx="34" cy="46" rx="18" ry="6" class="af"/><path d="M16 46v-8c0 3 8 6 18 6s18-3 18-6v8M16 38v-8c0 3 8 6 18 6s18-3 18-6v8M16 30c0-3 8-6 18-6s18 3 18 6"/><path d="M62 18h20M62 30h20M72 12v24M64 44l16 12M80 44 64 56"/>'),
  api: svgArt('<rect x="14" y="12" width="68" height="44" rx="6" class="af"/><path d="M34 26l-8 8 8 8M62 26l8 8-8 8M52 22l-8 24"/>'),
  hijriConv: svgArt('<rect x="10" y="14" width="46" height="42" rx="6" class="af"/><path d="M10 26h46M24 8v10M42 8v10M24 38h6M36 38h6M24 46h6"/><path d="M80 18a16 16 0 1 0 6 28 13 13 0 0 1-6-28z" class="af"/>'),
  timeCalc: svgArt('<circle cx="34" cy="34" r="20" class="af"/><path d="M34 22v12l8 5M66 26h16M74 18v16M66 46h16"/>'),
  daysUntil: svgArt('<rect x="12" y="14" width="52" height="42" rx="6" class="af"/><path d="M12 26h52M26 8v10M50 8v10M28 41h20m-6-6 6 6-6 6"/><circle cx="80" cy="41" r="7" class="af"/>'),
  interval: svgArt('<circle cx="34" cy="36" r="19" class="af"/><path d="M28 12h12M34 36l7-8M62 54V40M72 54V26M82 54V34"/>'),
  chess: svgArt('<rect x="8" y="24" width="80" height="30" rx="6" class="af"/><circle cx="29" cy="39" r="9"/><circle cx="67" cy="39" r="9"/><path d="M23 24v-6h12v6M61 24v-6h12v6M29 39v-5M67 39l4-3"/>'),
  metronome: svgArt('<path d="M32 56 41 10h14l9 46z" class="af"/><path d="M48 46 66 14M28 56h40"/><circle cx="60" cy="25" r="3"/>'),
  talking: svgArt('<circle cx="36" cy="34" r="20" class="af"/><path d="M36 22v12l7 4M64 24c4 6 4 14 0 20M73 17c7 10 7 24 0 34"/>'),
  sky: svgArt('<circle cx="48" cy="34" r="25" class="af"/><circle cx="40" cy="28" r="6"/><path d="M34 28h12"/><circle cx="60" cy="42" r="2"/><circle cx="56" cy="20" r="1.5"/><circle cx="34" cy="46" r="1.5"/>'),
  retire: svgArt('<circle cx="48" cy="30" r="20" class="af"/><path d="M48 18v12l8 6M30 62h36M36 56h24"/>'),
  otd: svgArt('<rect x="24" y="12" width="48" height="44" rx="4" class="af"/><path d="M24 24h48M36 8v8M60 8v8M34 34h28M34 42h20"/>'),
  zodiac: svgArt('<circle cx="48" cy="32" r="23" class="af"/><path d="M48 17l4 9 10 1-7 7 2 10-9-5-9 5 2-10-7-7 10-1z"/>'),
};

function buildToolsHub(lang) {
  const t = I[lang];
  const h = t.tools.hub;
  const pagePath = ROUTES[lang].tools;
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: h.h1, path: pagePath },
  ];
  /* Kart: görsel + araç adı; kartın tamamı bağlantıdır. Menüde bağlantısı olan sayfalar (takvim, dünya saatleri) burada yok. */
  const card = (k) => `<li class="tool-card"><a href="${ROUTES[lang][k]}"><span class="tool-art" aria-hidden="true">${TOOL_ART[k]}</span><span class="tool-name">${esc(k === "retire" ? RETIRE.cardName : h.cards[k].name)}</span></a></li>`;
  const cards = [...TOOL_KEYS, "interval", "chess", "metronome", "talking", "converter", "planner", "poll", "datecalc", "timeCalc", "daysUntil", "hijriConv", "diff", "zones", "dst", "tzn", "unix", "bridge", "worldHol", "qibla", "zakat", ...(lang === "tr" ? ["retire"] : []), "sky", "otd", "zodiac", "embed", "api", ...INFO_KEYS].map(card).join("");
  const body = `<section class="hero">
  <div class="wrap">
    <h1>${esc(h.h1)}</h1>
    <p class="lead">${esc(h.lead)}</p>
    <ul class="tool-cards">${cards}</ul>
  </div>
</section>`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, h.title, h.description), breadcrumbLd(trail)];
  searchEntries[lang].push({ t: h.h1, u: pagePath, d: h.description, k: "araç tool kronometre geri sayım alarm pomodoro stopwatch countdown" });
  return { lang, key: "tools", path: pagePath, title: h.title, description: h.description, body, ld, nav: "tools" };
}

/* ---------- Bilgi sayfaları: hafta numarası, bugünün tarihi, Türkiye saat dilimi ---------- */
const INFO_KEYS = ["week", "tz"];
const liveSpan = (kind, extra = "") => `<span data-live="${kind}"${extra}>–</span>`;

function infoShell(lang, key, { v = {}, body, faq, searchK }) {
  const t = I[lang];
  const s = t.info[key];
  const pagePath = ROUTES[lang][key];
  const title = tpl(s.title, v), description = tpl(s.description, v);
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: t.ui.tools, path: ROUTES[lang].tools },
    { name: tpl(s.h1, v), path: pagePath },
  ];
  const faqItems = faq.map((f) => ({ q: tpl(f.q, v), a: tpl(f.a, v) }));
  const html = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(tpl(s.h1, v))}</h1>
    <p class="lead">${esc(tpl(s.lead, v))}</p>
    ${body.hero}
    <p class="meta sync-line" data-sync-status>&nbsp;</p>
  </div>
</section>
${body.sections}
${addFaq(lang, key, t.tools.hub.cards[key].name, pagePath, faqItems)}`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, title, description), breadcrumbLd(trail)];
  searchEntries[lang].push({ t: tpl(s.h1, v), u: pagePath, d: description, k: searchK });
  return { lang, key: `info:${key}`, path: pagePath, title, description, body: html, ld, nav: "tools", scripts: ["dates.js"] };
}

function buildWeek(lang) {
  const t = I[lang];
  const s = t.info.week;
  const f = makeFmt(t.calendar, lang);
  const year = BUILD_YEAR;
  const weeks = isoWeeksInYear(year);
  const v = { year, weeks };
  const mon1 = isoWeek1Monday(year);
  const rows = Array.from({ length: weeks }, (_, i) => {
    const a = addDays(mon1, i * 7), b = addDays(a, 6);
    const lab = (d) => (d.startsWith(String(year)) ? f.short(d) : `${f.short(d)} ${d.slice(0, 4)}`);
    return `<tr data-wk="${i + 1}"><th scope="row">${i + 1}</th><td>${esc(lab(a))}</td><td>${esc(lab(b))}</td></tr>`;
  }).join("");
  const bigHtml = esc(s.big).replace("{n}", liveSpan("isoweek"));
  const body = {
    hero: `<p class="big-time info-big" role="timer">${bigHtml}</p>
    <p class="meta info-sub">${esc(s.thisWeek)}: <strong data-live="weekrange">–</strong></p>`,
    sections: `<section class="section" id="find" aria-labelledby="find-h">
  <div class="wrap">
    <h2 id="find-h">${esc(s.findHeading)}</h2>
    <div class="field"><label for="wk-date">${esc(s.findLabel)}</label><input id="wk-date" type="date" data-weekfind></div>
    <p class="find-out" role="status" data-weekfind-out>${esc(t.client.pickDate)}</p>
  </div>
</section>
<section class="section" id="table" aria-labelledby="table-h">
  <div class="wrap">
    <h2 id="table-h">${esc(tpl(s.tableHeading, v))}</h2>
    <div class="table-wrap"><table class="events-table week-table"><caption>${esc(tpl(s.tableCaption, v))}</caption><thead><tr><th scope="col">${esc(s.colWeek)}</th><th scope="col">${esc(s.colStart)}</th><th scope="col">${esc(s.colEnd)}</th></tr></thead><tbody>${rows}</tbody></table></div>
  </div>
</section>`,
  };
  return infoShell(lang, "week", { v, body, faq: s.faq, searchK: "hafta numarası kaçıncı haftadayız iso hafta week number which week yılın haftası" });
}

function buildTz(lang) {
  const t = I[lang];
  const s = t.info.tz;
  const std = (c) => { const m = c.std.match(/([+-])(\d\d):(\d\d)/); return (m[1] === "-" ? -1 : 1) * (+m[2] * 60 + +m[3]); };
  const durText = (min) => { const a = Math.abs(min), h = Math.floor(a / 60), m = a % 60; return [h && `${h} ${t.client.hourShort}`, m && `${m} ${t.client.minuteShort}`].filter(Boolean).join(" "); };
  const refName = "Türkiye";
  const rows = CITIES.filter((c) => c.tz !== "Europe/Istanbul").map((c) => {
    const d = std(c) - 180;
    const stdTxt = d === 0 ? tpl(t.client.refSame, { ref: refName }) : tpl(d > 0 ? t.client.refAhead : t.client.refBehind, { d: durText(d), ref: refName });
    return `<tr><th scope="row">${esc(c[lang].name)}</th><td>${esc(stdTxt)}</td><td>${esc(c.dst ? s.yes : s.no)}</td><td data-live="diff" data-tz="${c.tz}" data-ref="Europe/Istanbul" data-ref-name="${refName}">&nbsp;</td></tr>`;
  }).join("");
  const body = {
    hero: `<p class="meta info-sub">${esc(s.nowLabel)}</p><p class="big-time" role="timer" data-live="time" data-sec data-tz="Europe/Istanbul">--:--:--</p>`,
    sections: `<section class="section" aria-labelledby="facts-h">
  <div class="wrap prose">
    <h2 id="facts-h">${esc(s.factsHeading)}</h2>
    <ul>${s.facts.map((x) => `<li>${esc(x)}</li>`).join("")}</ul>
    <h2>${esc(s.historyHeading)}</h2>
    ${s.history.map((x) => `<p>${esc(x)}</p>`).join("")}
  </div>
</section>
<section class="section" id="table" aria-labelledby="table-h">
  <div class="wrap">
    <h2 id="table-h">${esc(s.tableHeading)}</h2>
    <div class="table-wrap"><table><caption>${esc(s.tableCaption)}</caption><thead><tr><th scope="col">${esc(s.colCity)}</th><th scope="col">${esc(s.colStd)}</th><th scope="col">${esc(s.colDst)}</th><th scope="col">${esc(s.colNow)}</th></tr></thead><tbody>${rows}</tbody></table></div>
    <p class="meta">${esc(s.note)}</p>
  </div>
</section>`,
  };
  return infoShell(lang, "tz", { body, faq: s.faq, searchK: "yaz saati türkiye saat dilimi utc+3 trt daylight saving time zone dst europe istanbul" });
}

/* ---------- Saat farkı, çevirici, planlayıcı ve geri sayım sayfaları ---------- */
const ROUTES_EXTRA = {
  tr: { diff: "/saat-farki/", converter: "/saat-cevirici/", planner: "/toplanti-planlayici/" },
  en: { diff: "/en/time-difference/", converter: "/en/time-converter/", planner: "/en/meeting-planner/" },
};
extendRoutes(ROUTES_EXTRA);
extendRoutes({
  tr: { hijriConv: "/hicri-takvim-cevirici/", timeCalc: "/saat-hesaplama/", daysUntil: "/kac-gun-kaldi/", interval: "/aralik-zamanlayici/", chess: "/satranc-saati/", metronome: "/metronom/", talking: "/konusan-saat/", zodiac: "/burc-hesaplama/" },
  en: { hijriConv: "/en/hijri-converter/", timeCalc: "/en/time-calculator/", daysUntil: "/en/days-until/", interval: "/en/interval-timer/", chess: "/en/chess-clock/", metronome: "/en/metronome/", talking: "/en/talking-clock/", zodiac: "/en/zodiac-sign/" },
});
extendRoutes({ tr: { prayer: "/namaz-vakitleri/", nature: "/mevsimler/", weather: "/hava-durumu/", qibla: "/kible/", zakat: "/zekat-hesaplama/", api: "/gelistiriciler/", bridge: "/kopru-tatil/", zones: "/saat-dilimleri/", dst: "/yaz-saati/", unix: "/unix-zaman-damgasi/", worldHol: "/resmi-tatiller/" }, en: { prayer: "/en/prayer-times/", nature: "/en/seasons/", weather: "/en/weather/", qibla: "/en/qibla/", zakat: "/en/zakat-calculator/", api: "/en/developers/", bridge: "/en/long-weekends/", zones: "/en/time-zones/", dst: "/en/daylight-saving-time/", unix: "/en/unix-time/", worldHol: "/en/public-holidays/" } });
extendRoutes({ tr: { sky: "/gece-gokyuzu/", otd: "/tarihte-bugun/" }, en: { sky: "/en/night-sky/", otd: "/en/on-this-day/" } });
extendRoutes({ tr: { tzn: "/saat-dilimi-haberleri/" }, en: { tzn: "/en/time-zone-news/" } });
extendRoutes({ tr: { poll: "/toplanti-oylamasi/" }, en: { poll: "/en/meeting-poll/" } });
ROUTES.tr.retire = "/emeklilik-hesaplama/"; // yalnız Türkçe (SGK kuralları)
const stdMin = (c) => { const m = c.std.match(/([+-])(\d\d):(\d\d)/); return (m[1] === "-" ? -1 : 1) * (+m[2] * 60 + +m[3]); };
const HOME_CITY = CITIES.find((c) => c.key === "istanbul");
const PAIR_CITIES = CITIES.filter((c) => c.tz !== HOME_CITY.tz);
const pairPath = (lang, c) => `${ROUTES[lang].diff}${HOME_CITY[lang].slug}-${c[lang].slug}/`;
const durTxt = (lang, min) => { const t = I[lang].client; const a = Math.abs(min), h = Math.floor(a / 60), m = a % 60; return [h && `${h} ${t.hourShort}`, m && `${m} ${t.minuteShort}`].filter(Boolean).join(" "); };
const hh = (n) => String(((n % 24) + 24) % 24).padStart(2, "0");
/* Paylaşılabilir bağlantı ve takvime ekle (30 Ekim 2026; .ics veri adresi, Google Takvim bağlantısı) */
const shareRow = (s) => `<div class="share-row"><button type="button" class="toggle" data-share>${esc(s.shareLink)}</button><a class="toggle" data-ics hidden download="saat-tarih.ics">${esc(s.addIcs)}</a><a class="toggle" data-gcal hidden target="_blank" rel="noopener">${esc(s.addGoogle)}</a><span class="meta" role="status" aria-live="polite" data-share-status></span></div>`;
const cityOptions = (lang, withMine) => `${withMine ? `<option value="">${esc(I[lang].converter.mine)}</option>` : ""}${[...CITIES].sort((a, b) => a[lang].name.localeCompare(b[lang].name, lang)).map((c) => `<option value="${c.key}">${esc(c[lang].name)} (${esc(c[lang].country)})</option>`).join("")}`;
const clientCities = (lang) => CITIES.map((c) => ({ k: c.key, n: c[lang].name, tz: c.tz, p: cityPath(lang, c), c: c[lang].country }));
const webAppLd = (lang, name, pagePath, description) => ({ "@type": "WebApplication", name, url: abs(pagePath), description, applicationCategory: "UtilitiesApplication", operatingSystem: "Any", inLanguage: lang, isAccessibleForFree: true, offers: { "@type": "Offer", price: "0", priceCurrency: "TRY" } });

function simplePage(lang, key, { title, description, h1, lead, trail, bodyInner, faq, faqGroup, scripts, clientExtra, nav = "tools", searchK, extraLd = [], needsPlace = false, search = true }) {
  const pagePath = trail[trail.length - 1].path;
  const faqBlock = faq && faq.length && faqGroup ? addFaq(lang, faqGroup.id, faqGroup.title, faqGroup.href || pagePath, faq) : "";
  const body = `<section class="hero">
  <div class="wrap">
    ${bodyInner.pre || ""}
    ${crumbs(lang, trail)}
    <h1>${esc(h1)}</h1>
    <p class="lead">${esc(lead)}</p>
    ${bodyInner.hero || ""}
  </div>
</section>
${bodyInner.sections || ""}
${faqBlock}`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, title, description), breadcrumbLd(trail), ...extraLd];
  if (search) searchEntries[lang].push({ t: h1, u: pagePath, d: description, k: searchK });
  return { lang, key, path: pagePath, title, description, body, ld, nav, scripts, clientExtra, needsPlace };
}

/* Saat farkı: merkez ve İstanbul–şehir sayfaları */
function buildPair(lang, c) {
  const t = I[lang];
  const p = t.pair;
  const a = HOME_CITY[lang].name, b = c[lang].name;
  const v = { a, b };
  const d = 180 - stdMin(c); // a − b, dakika (standart zaman)
  const lead = d === 0 ? tpl(p.same, v) : tpl(d > 0 ? p.ahead : p.behind, { ...v, d: durTxt(lang, d) });
  let dstLine = "";
  if (c.dst) { const d2 = d - 60; dstLine = d2 === 0 ? tpl(p.dstSame, v) : tpl(p.dstNote, { ...v, d: durTxt(lang, d2) }); } else dstLine = p.noDst;
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: t.pairHub.h1, path: ROUTES[lang].diff },
    { name: tpl(p.h1, v), path: pairPath(lang, c) },
  ];
  const rows = Array.from({ length: 24 }, (_, h) => {
    const m = h * 60 - d; // b şehrindeki dakika
    const day = m < 0 ? p.prevDay : m >= 1440 ? p.nextDay : "";
    return `<tr><th scope="row">${hh(h)}:00</th><td>${hh(Math.floor(m / 60))}:${String(((m % 60) + 60) % 60).padStart(2, "0")}${day ? ` <span class="meta">(${esc(day)})</span>` : ""}</td></tr>`;
  }).join("");
  const faq = [
    { q: tpl(p.faqDiff, v), a: `${lead} ${dstLine}` },
    { q: tpl(p.faqNow, v), a: p.faqNowA },
    { q: tpl(p.faqDst, v), a: tpl(c.dst ? p.faqDstYes : p.faqDstNo, v) },
  ];
  const inner = {
    hero: `<div class="pair-live">
      <div class="pair-city"><p class="meta">${esc(a)}</p><p class="big-time" role="timer" data-live="time" data-sec data-tz="${HOME_CITY.tz}">--:--:--</p></div>
      <div class="pair-city"><p class="meta">${esc(b)}</p><p class="big-time" role="timer" data-live="time" data-sec data-tz="${c.tz}">--:--:--</p></div>
    </div>
    <p class="info-sub"><strong data-live="diff" data-tz="${c.tz}" data-ref="${HOME_CITY.tz}" data-ref-name="${esc(a)}">&nbsp;</strong></p>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>
    <p class="meta">${esc(dstLine)}</p>`,
    sections: `<section class="section" aria-labelledby="conv-h">
  <div class="wrap">
    <h2 id="conv-h">${esc(tpl(p.convHeading, v))}</h2>
    <div class="table-wrap"><table class="events-table" data-pair-table data-a="${HOME_CITY.tz}" data-b="${c.tz}" data-next="${esc(p.nextDay)}" data-prev="${esc(p.prevDay)}"><caption>${esc(tpl(p.convCaption, v))}</caption><thead><tr><th scope="col">${esc(tpl(p.colA, v))}</th><th scope="col">${esc(tpl(p.colB, v))}</th></tr></thead><tbody>${rows}</tbody></table></div>
    <p class="meta">${esc(p.convNote)}</p>
  </div>
</section>`,
  };
  return simplePage(lang, `pair:${c.key}`, { title: tpl(p.title, v), description: tpl(p.description, v), h1: tpl(p.h1, v), lead, trail, bodyInner: inner, faq: faq.filter((_, i) => i !== 1), faqGroup: { id: "diff", title: t.tools.hub.cards.diff.name, href: ROUTES[lang].diff }, scripts: ["planner.js"], nav: "world", searchK: `saat farkı time difference ${a} ${b} ${c[lang].country} ${c.tz}` });
}

function buildPairHub(lang) {
  const t = I[lang];
  const h = t.pairHub;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: h.h1, path: ROUTES[lang].diff }];
  const list = PAIR_CITIES.map((c) => `<li class="city-card"><a href="${pairPath(lang, c)}"><span class="city-name">${esc(tpl(h.linkText, { a: HOME_CITY[lang].name, b: c[lang].name }))}</span><span class="city-diff">${esc(c[lang].country)}</span></a></li>`).join("");
  return simplePage(lang, "diff", { title: h.title, description: h.description, h1: h.h1, lead: h.lead, trail, bodyInner: { sections: `<section class="section" aria-labelledby="list-h"><div class="wrap"><h2 id="list-h">${esc(h.listHeading)}</h2><ul class="city-grid">${list}</ul></div></section>` }, nav: "world", searchK: "saat farkı time difference istanbul dünya şehirleri" });
}

/* Saat çevirici */
function buildConverter(lang) {
  const t = I[lang];
  const s = t.converter;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: t.ui.tools, path: ROUTES[lang].tools }, { name: s.h1, path: ROUTES[lang].converter }];
  const inner = {
    hero: `<section class="tool" data-converter aria-labelledby="tool-h">
      <div class="tool-head"><h2 id="tool-h" class="tool-title">${esc(s.h1)}</h2></div>
      <div class="inputs-row">
        <div class="field"><label for="cv-from">${esc(s.fromLabel)}</label><select id="cv-from" data-cv-from>${cityOptions(lang, true)}</select></div>
        <button type="button" class="icon-btn" data-cv-swap aria-label="${esc(s.swap)}" title="${esc(s.swap)}">⇄</button>
        <div class="field"><label for="cv-to">${esc(s.toLabel)}</label><select id="cv-to" data-cv-to>${cityOptions(lang, true)}</select></div>
        <div class="field"><label for="cv-when">${esc(s.whenLabel)}</label><input id="cv-when" type="datetime-local" data-cv-when></div>
        <button type="button" class="toggle" data-cv-now>${esc(s.now)}</button>
      </div>
      <p class="big-time cv-result" role="status" aria-live="polite" data-cv-result>&nbsp;</p>
      <p class="meta info-sub" data-cv-diff>&nbsp;</p>
      ${shareRow(s)}
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>`,
  };
  return simplePage(lang, "converter", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: inner, faq: s.faq, faqGroup: { id: "converter", title: t.tools.hub.cards.converter.name }, scripts: ["planner.js"], clientExtra: { cities: clientCities(lang), t: { conv: s } }, searchK: "saat çevirici time converter dönüştürücü saat dilimi time zone", extraLd: [webAppLd(lang, s.h1, ROUTES[lang].converter, s.description)] });
}

/* Toplantı planlayıcı */
function buildPlanner(lang) {
  const t = I[lang];
  const s = t.planner;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: t.ui.tools, path: ROUTES[lang].tools }, { name: s.h1, path: ROUTES[lang].planner }];
  const inner = {
    hero: `<section class="tool" data-planner aria-labelledby="tool-h">
      <div class="tool-head"><h2 id="tool-h" class="tool-title">${esc(s.h1)}</h2></div>
      <div class="inputs-row">
        <div class="field"><label for="pl-city">${esc(s.addLabel)}</label><select id="pl-city" data-pl-city>${cityOptions(lang, false)}</select></div>
        <button type="button" class="toggle" data-pl-add>${esc(s.addBtn)}</button>
        <div class="field"><label for="pl-date">${esc(s.dateLabel)}</label><input id="pl-date" type="date" data-pl-date></div>
      </div>
      <ul class="chips-list" data-pl-chips></ul>
      <ul class="legend" aria-label="${esc(s.tableLabel)}"><li><span class="lg lg-work" aria-hidden="true"></span>${esc(s.legendWork)}</li><li><span class="lg lg-edge" aria-hidden="true"></span>${esc(s.legendEdge)}</li><li><span class="lg lg-night" aria-hidden="true"></span>${esc(s.legendNight)}</li></ul>
      <div class="table-wrap"><table class="planner" data-pl-table><caption class="sr-only">${esc(s.tableLabel)}</caption></table></div>
      <p class="pl-common" role="status" aria-live="polite" data-pl-common>&nbsp;</p>
      <p class="meta pl-hint" data-pl-picked>${esc(s.pickHint)}</p>
      ${shareRow(s)}
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>`,
  };
  return simplePage(lang, "planner", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: inner, faq: s.faq, faqGroup: { id: "planner", title: t.tools.hub.cards.planner.name }, scripts: ["planner.js"], clientExtra: { cities: clientCities(lang), t: { plan: s } }, searchK: "toplantı planlayıcı meeting planner ortak mesai saatleri world meeting", extraLd: [webAppLd(lang, s.h1, ROUTES[lang].planner, s.description)] });
}

/* Toplantı oylaması (31 Ekim 2026, sahibin kararı: sunucusuz, bağlantıyla; src/js/poll.js) */
const POLL_CLIENT_KEYS = ["slotsHeading", "removeSlot", "maxSlots", "needSlot", "tzNote", "orgTime", "byLine", "linkHeading", "linkHint", "linkLabel", "copy", "copied", "share",
  "voteHeading", "yourName", "yes", "maybe", "no", "respond", "responseHint", "ownSaved", "needName", "resultsHeading", "participant", "total", "best", "noAnswers",
  "pasteLabel", "pasteBtn", "pasteBad", "added", "removeAnswer", "resultsLink", "myPolls", "answers", "deletePoll", "invalid", "newPoll"];
function buildPoll(lang) {
  const t = I[lang];
  const s = t.poll;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: t.ui.tools, path: ROUTES[lang].tools }, { name: s.h1, path: ROUTES[lang].poll }];
  const durOpts = [15, 30, 45, 60, 90, 120, 180].map((m) => `<option value="${m}"${m === 60 ? " selected" : ""}>${esc(durTxt(lang, m))}</option>`).join("");
  const inner = {
    hero: `<section class="tool poll" data-poll aria-labelledby="tool-h">
      <div class="tool-head"><h2 id="tool-h" class="tool-title">${esc(s.h1)}</h2></div>
      <div data-poll-create>
        <div class="inputs-row">
          <div class="field grow"><label for="po-title">${esc(s.titleLabel)}</label><input id="po-title" type="text" maxlength="80" placeholder="${esc(t.planner.meeting)}" data-po-title></div>
          <div class="field"><label for="po-org">${esc(s.orgLabel)}</label><input id="po-org" type="text" maxlength="40" autocomplete="name" data-po-org></div>
          <div class="field"><label for="po-dur">${esc(s.durLabel)}</label><select id="po-dur" data-po-dur>${durOpts}</select></div>
        </div>
        <h3 class="poll-h">${esc(s.slotsHeading)}</h3>
        <div class="inputs-row">
          <div class="field"><label for="po-date">${esc(s.dateLabel)}</label><input id="po-date" type="date" data-po-date></div>
          <div class="field"><label for="po-time">${esc(s.timeLabel)}</label><input id="po-time" type="time" step="300" data-po-time></div>
          <button type="button" class="toggle" data-po-add>${esc(s.addSlot)}</button>
        </div>
        <ul class="chips-list poll-slots" data-po-slots aria-label="${esc(s.slotsHeading)}"></ul>
        <p class="meta poll-tz" data-po-tz></p>
        <div class="tool-actions"><button type="button" class="btn" data-po-create>${esc(s.create)}</button></div>
        <p class="meta poll-msg" role="status" aria-live="polite" data-po-msg></p>
      </div>
      <div data-poll-link hidden></div>
      <div class="poll-view" data-poll-view hidden></div>
      <div class="poll-minebox" data-poll-mine hidden></div>
      <p class="meta poll-privacy">${esc(s.privacyNote)}</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>`,
  };
  const client = Object.fromEntries(POLL_CLIENT_KEYS.map((k) => [k, s[k]]));
  return simplePage(lang, "poll", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: inner, faq: s.faq, faqGroup: { id: "poll", title: t.tools.hub.cards.poll.name }, scripts: ["poll.js"], clientExtra: { t: { poll: client, plan: { meeting: t.planner.meeting, addIcs: t.planner.addIcs, addGoogle: t.planner.addGoogle } } }, searchK: "toplantı oylaması anket uygun saat doodle meeting poll vote schedule availability", extraLd: [webAppLd(lang, s.h1, ROUTES[lang].poll, s.description)] });
}

/* Hazır geri sayım sayfaları */
const CDP = [
  { key: "newYear", keys: ["newYear"], tr: "yilbasi", en: "new-year" },
  { key: "rb1", keys: ["rb1"], tr: "ramazan-bayrami", en: "eid-al-fitr" },
  { key: "kb1", keys: ["kb1"], tr: "kurban-bayrami", en: "eid-al-adha" },
  { key: "oct29", keys: ["oct29"], tr: "cumhuriyet-bayrami", en: "republic-day" },
  { key: "ramadanStart", keys: ["ramadanStart"], tr: "ramazan", en: "ramadan" },
  { key: "apr23", keys: ["apr23"], tr: "23-nisan", en: "april-23" },
  { key: "may19", keys: ["may19"], tr: "19-mayis", en: "may-19" },
  { key: "aug30", keys: ["aug30"], tr: "30-agustos", en: "august-30" },
  // Özel günler (30 Ekim 2026, rakip analizi: saat-kac.tr, vclock)
  { key: "feb14", keys: ["feb14"], tr: "sevgililer-gunu", en: "valentines-day" },
  { key: "mar8", keys: ["mar8"], tr: "kadinlar-gunu", en: "womens-day" },
  { key: "mothers", keys: ["mothers"], tr: "anneler-gunu", en: "mothers-day" },
  { key: "fathers", keys: ["fathers"], tr: "babalar-gunu", en: "fathers-day" },
  { key: "nov24", keys: ["nov24"], tr: "ogretmenler-gunu", en: "teachers-day" },
  { key: "semester", keys: ["semester"], tr: "yariyil-tatili", en: "mid-year-break", school: true },
  { key: "karne", keys: ["term1End", "schoolEnd"], tr: "karne-gunu", en: "report-card-day", school: true },
  { key: "schoolStart", keys: ["schoolStart"], tr: "okul-acilisi", en: "school-start", school: true },
  { key: "midterm", keys: ["midterm1", "midterm2"], tr: "ara-tatil", en: "mid-term-break", school: true },
  { key: "yks", keys: ["yksTyt"], tr: "yks", en: "yks", exam: true },
  { key: "kpss", keys: ["kpssOrta", "kpssDhbt"], tr: "kpss", en: "kpss", exam: true },
  { key: "ales", keys: ["ales3"], tr: "ales", en: "ales", exam: true },
  { key: "yds", keys: ["yds2"], tr: "yds", en: "yds", exam: true },
  { key: "aof", keys: ["aofMid1", "aofFinal1", "aofMid2", "aofFinal2", "aofSummer"], tr: "aof-sinavi", en: "aof-exam", exam: true },
  { key: "aol", keys: ["aolWritten"], tr: "aol-sinavi", en: "aol-exam", exam: true },
];
const cdpPath = (lang, x) => `${ROUTES[lang].countdown}${x[lang] || x.en}/`;

function buildCdp(lang, x) {
  const t = I[lang];
  const s = t.cdp;
  const f = makeFmt(t.calendar, lang);
  const tg = s.targets[x.key];
  const v = { name: tg.name, dat: tg.dat };
  const pagePath = cdpPath(lang, x);
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: t.ui.tools, path: ROUTES[lang].tools },
    { name: t.tools.countdown.h1, path: ROUTES[lang].countdown },
    { name: tpl(s.h1, v), path: pagePath },
  ];
  // Yıllara göre tarihler (veri dosyalarından)
  const dates = [];
  const years = x.key === "newYear" ? [...HOL_YEARS, HOL_YEARS[HOL_YEARS.length - 1] + 1] : HOL_YEARS;
  for (const y of years) {
    const ev = YEAR_DATA[y] ? YEAR_DATA[y].events.filter((e) => x.keys.includes(e.key)) : [];
    if (ev.length) ev.forEach((e) => dates.push({ y, d: e.date, k: e.key }));
    else if (x.key === "newYear") dates.push({ y, d: `${y}-01-01`, k: "newYear" });
  }
  const fixed = ["newYear", "apr23", "may19", "aug30", "oct29", "feb14", "mar8", "nov24"].includes(x.key);
  const names = t.calendar.events;
  const rowsHtml = dates.map((r) => `<tr><th scope="row">${r.y}</th><td>${esc(f.full(r.d))}${x.keys.length > 1 ? ` · ${esc(names[r.k])}` : ""}</td></tr>`).join("");
  const faq = [
    { q: tpl(s.faqWhen, v), a: fixed ? tpl(s.faqWhenFixed, { ...v, date: f.short(dates[0].d) }) : tpl(s.faqWhenYears, { ...v, list: dates.slice(0, 4).map((r) => `${r.y}: ${f.short(r.d)}`).join(" · ") }) },
    { q: s.faqHow, a: s.faqHowA },
  ];
  const inner = {
    hero: `<section class="tool cdp" data-cdp data-cdp-keys="${x.keys.join(",")}" aria-labelledby="tool-h">
      <div class="tool-head"><h2 id="tool-h" class="tool-title">${esc(tg.name)}</h2><button type="button" class="icon-btn" data-fs aria-pressed="false" aria-label="${esc(t.tools.common.fullscreen)}" title="${esc(t.tools.common.fullscreen)}">${ICON_FULL}</button></div>
      <p class="big-time" role="timer" data-cdp-display>–</p>
      <p class="meta info-sub">${esc(s.target)}: <strong data-cdp-date>–</strong></p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>`,
    sections: `<section class="section" aria-labelledby="years-h">
  <div class="wrap">
    <h2 id="years-h">${esc(tpl(s.yearsHeading, v))}</h2>
    <div class="table-wrap"><table class="events-table"><caption>${esc(tpl(s.yearsCaption, v))}</caption><thead><tr><th scope="col">${esc(s.colYear)}</th><th scope="col">${esc(s.colDate)}</th></tr></thead><tbody>${rowsHtml}</tbody></table></div>
    ${x.school ? `<p class="meta">${esc(s.school)}</p>` : ""}
    ${x.exam ? `<p class="meta">${esc(s.exam)}</p>` : ""}
    <h2>${esc(s.customHeading)}</h2>
    <p>${esc(s.customText)}</p>
  </div>
</section>`,
  };
  return simplePage(lang, `cdp:${x.key}`, { title: tpl(s.title, v), description: tpl(s.description, v), h1: tpl(s.h1, v), lead: tpl(s.lead, v), trail, bodyInner: inner, faq, faqGroup: { id: "countdowns", title: t.faqPage.countdowns, href: ROUTES[lang].countdown }, scripts: ["calendar.js"], clientExtra: { t: { cdp: s } }, searchK: `geri sayım kaç gün kaldı countdown days until ${tg.name}` });
}

/* ---------- Tarih hesaplayıcılar ve gömme kodu (widget) ---------- */
const ROUTES_C = {
  tr: { datecalc: "/tarih-hesaplama/", embed: "/gomme/", embedFrame: "/embed/tr/" },
  en: { datecalc: "/en/date-calculator/", embed: "/en/embed/", embedFrame: "/embed/en/" },
};
extendRoutes(ROUTES_C);
extendRoutes({ tr: { faq: "/sss/" }, en: { faq: "/en/faq/" } });

function buildDateCalc(lang) {
  const t = I[lang];
  const s = t.datecalc;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: t.ui.tools, path: ROUTES[lang].tools }, { name: s.h1, path: ROUTES[lang].datecalc }];
  const dateField = (id, label, attr) => `<div class="field"><label for="${id}">${esc(label)}</label><input id="${id}" type="date" ${attr}></div>`;
  const unitOpts = Object.entries(s.units).map(([k, v]) => `<option value="${k}">${esc(v)}</option>`).join("");
  const inner = {
    hero: `<p class="meta sync-line" data-sync-status>&nbsp;</p>`,
    sections: `<section class="section" aria-labelledby="dc-diff-h">
  <div class="wrap">
    <div class="tool" data-dc="diff">
      <h2 id="dc-diff-h" class="tool-h2">${esc(s.diffHeading)}</h2>
      <div class="inputs-row">${dateField("dc-d1", s.startLabel, "data-dc-start")}${dateField("dc-d2", s.endLabel, "data-dc-end")}</div>
      <div class="dc-out" role="status" aria-live="polite" data-dc-out>${esc(s.pick)}</div>
      <p class="meta">${esc(s.diffNote)}</p>
    </div>
    <div class="tool" data-dc="add">
      <h2 id="dc-add-h" class="tool-h2">${esc(s.addHeading)}</h2>
      <div class="inputs-row">${dateField("dc-a1", s.startLabel, "data-dc-start")}
        <div class="field"><label for="dc-a2">${esc(s.amountLabel)}</label><input id="dc-a2" type="number" inputmode="numeric" value="30" step="1" data-dc-amount></div>
        <div class="field"><label for="dc-a3">${esc(s.unitLabel)}</label><select id="dc-a3" data-dc-unit>${unitOpts}</select></div>
      </div>
      <div class="dc-out" role="status" aria-live="polite" data-dc-out>${esc(s.pick)}</div>
      <p class="meta">${esc(s.addHint)}</p>
    </div>
    <div class="tool" data-dc="age">
      <h2 id="dc-age-h" class="tool-h2">${esc(s.ageHeading)}</h2>
      <div class="inputs-row">${dateField("dc-b1", s.birthLabel, "data-dc-birth")}</div>
      <div class="dc-out" role="status" aria-live="polite" data-dc-out>${esc(s.pick)}</div>
    </div>
    <p class="meta" data-dc-range></p>
  </div>
</section>`,
  };
  return simplePage(lang, "datecalc", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: inner, faq: s.faq, faqGroup: { id: "datecalc", title: t.tools.hub.cards.datecalc.name }, scripts: ["dates.js"], clientExtra: { t: { dc: s, dcRange: { from: HOL_YEARS[0], to: HOL_YEARS[HOL_YEARS.length - 1] } } }, searchK: "tarih hesaplama iki tarih arası gün iş günü yaş hesaplama date calculator days between working days age", extraLd: [webAppLd(lang, s.h1, ROUTES[lang].datecalc, s.description)] });
}

function buildEmbedGen(lang) {
  const t = I[lang];
  const s = t.embedgen;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: t.ui.tools, path: ROUTES[lang].tools }, { name: s.h1, path: ROUTES[lang].embed }];
  const modelOpts = EMBED_MODELS.map((m) => `<option value="${m.id}">${esc(t.stage.models[m.id])}</option>`).join("");
  const cityOpts = `<option value="">${esc(s.cityLocal)}</option>${[...CITIES].sort((a, b) => a[lang].name.localeCompare(b[lang].name, lang)).map((c) => `<option value="${c.key}">${esc(c[lang].name)} (${esc(c[lang].country)})</option>`).join("")}`;
  const inner = {
    hero: `<section class="tool" data-embedgen aria-labelledby="tool-h">
      <div class="tool-head"><h2 id="tool-h" class="tool-title">${esc(s.h1)}</h2></div>
      <div class="inputs-row">
        <div class="field"><label for="eg-m">${esc(s.modelLabel)}</label><select id="eg-m" data-eg-model>${modelOpts}</select></div>
        <div class="field"><label for="eg-c">${esc(s.cityLabel)}</label><select id="eg-c" data-eg-city>${cityOpts}</select></div>
        <div class="field"><label for="eg-t">${esc(s.themeLabel)}</label><select id="eg-t" data-eg-theme><option value="auto">${esc(s.themeAuto)}</option><option value="light">${esc(s.themeLight)}</option><option value="dark">${esc(s.themeDark)}</option></select></div>
        <div class="field"><label for="eg-l">${esc(s.langLabel)}</label><select id="eg-l" data-eg-lang>${LANGS.map((l) => `<option value="${l}"${lang === l ? " selected" : ""}>${LANG_NAMES[l]}</option>`).join("")}</select></div>
      </div>
      <div class="inputs-row">
        <div class="field"><label for="eg-w">${esc(s.widthLabel)}</label><input id="eg-w" type="number" min="160" max="1200" value="360" data-eg-w></div>
        <div class="field"><label for="eg-h">${esc(s.heightLabel)}</label><input id="eg-h" type="number" min="160" max="1200" value="360" data-eg-h></div>
        <label class="check-inline"><input type="checkbox" checked data-eg-sec> ${esc(s.secLabel)}</label>
        <label class="check-inline"><input type="checkbox" data-eg-h12> ${esc(s.h12Label)}</label>
      </div>
      <h3>${esc(s.previewHeading)}</h3>
      <div class="eg-preview"><iframe data-eg-frame title="${esc(s.previewTitle)}" width="360" height="360" loading="lazy"></iframe></div>
      <h3>${esc(s.codeHeading)}</h3>
      <textarea class="eg-code" readonly rows="4" aria-label="${esc(s.codeHeading)}" data-eg-code></textarea>
      <div class="tool-actions">${btn("btn", "data-eg-copy", s.copy)}</div>
      <p class="meta" data-eg-status role="status"></p>
      <p class="meta">${esc(s.sizeNote)}</p>
      <p class="meta">${esc(s.note)}</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`,
  };
  return simplePage(lang, "embed", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: inner, faq: s.faq, faqGroup: { id: "embed", title: t.tools.hub.cards.embed.name }, scripts: ["planner.js"], clientExtra: { cities: clientCities(lang), t: { eg: { copied: s.copied, frameTitle: s.frameTitle, path: Object.fromEntries(LANGS.map((l) => [l, ROUTES[l].embedFrame])) } } }, searchK: "widget gömme kodu iframe saat embed clock site", extraLd: [webAppLd(lang, s.h1, ROUTES[lang].embed, s.description)] });
}

/* ---------- Doğa (mevsim çarkı, Güneş, Ay, gün uzunluğu) ve hava durumu ---------- */
const NATURE = (() => { const m = { exports: {} }; new Function("module", "window", fs.readFileSync(path.join(SRC, "js/nature-calc.js"), "utf8"))(m, undefined); return m.exports; })();

function trDateTime(lang, ms) { // Türkiye saatiyle (UTC+3) "20 Mart 14:46"
  const d = new Date(ms + 3 * 3600000);
  return `${d.getUTCDate()} ${I[lang].calendar.months[d.getUTCMonth()]} ${String(d.getUTCHours()).padStart(2, "0")}:${String(d.getUTCMinutes()).padStart(2, "0")}`;
}

function buildNature(lang) {
  const t = I[lang];
  const n = t.nature;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: n.h1, path: ROUTES[lang].nature }];
  const wheel = `<section class="nature-top" data-nature aria-label="${esc(n.wheelLabel)}">
      <div class="wheel-wrap">
        <svg class="wheel" data-wheel viewBox="0 0 360 360" role="img" aria-label="${esc(n.wheelLabel)}"></svg>
        <div class="wheel-center" aria-live="polite">
          <p class="wheel-season" data-n-season>&nbsp;</p>
          <p class="wheel-line" data-n-dayof>&nbsp;</p>
          <p class="wheel-line" data-n-next>&nbsp;</p>
        </div>
      </div>
    </section>`;
  const head = ["spring", "summer", "autumn", "winter"].map((k) => `<th scope="col">${esc(n.seasons[k])}</th>`).join("");
  const rows = HOL_YEARS.map((y) => { const q = NATURE.seasons(y); return `<tr><th scope="row">${y}</th>${[q.march, q.june, q.sept, q.dec].map((ms) => `<td>${esc(trDateTime(lang, ms))}</td>`).join("")}</tr>`; }).join("");
  const facts = ["spring", "summer", "autumn", "winter"].map((k) => `<article class="season-card sc-${k}"><h3>${esc(n.seasons[k])}</h3><ul>${n.facts[k].map((f) => `<li>${esc(f)}</li>`).join("")}</ul></article>`).join("");
  const cell = (label, attr) => `<div class="fact"><dt>${esc(label)}</dt><dd ${attr}>–</dd></div>`;
  const sections = `<section class="section" aria-labelledby="n-turn-h">
  <div class="wrap">
    <h2 id="n-turn-h">${esc(n.turnHeading)}</h2>
    <ul class="turns" data-n-turns></ul>
  </div>
</section>
<section class="section" aria-labelledby="n-sun-h">
  <div class="wrap">
    <h2 id="n-sun-h">${esc(n.sunHeading)}</h2>
    <dl class="facts">${cell(n.sunrise, "data-n-sun=\"rise\"")}${cell(n.sunset, "data-n-sun=\"set\"")}${cell(n.dayLength, "data-n-sun=\"len\"")}${cell(n.solarNoon, "data-n-sun=\"noon\"")}</dl>
    <p class="meta" data-n-delta>&nbsp;</p>
    <p class="meta" data-n-left>&nbsp;</p>
    <dl class="facts light-hours">${["blueAm", "goldenAm", "goldenPm", "bluePm"].map((k) => cell(n[k], `data-n-light="${k}" dir="ltr"`)).join("")}</dl>
    <p class="meta">${esc(n.lightNote)}</p>
  </div>
</section>
<section class="section" aria-labelledby="n-moon-h">
  <div class="wrap">
    <h2 id="n-moon-h">${esc(n.moonHeading)}</h2>
    <div class="moon-row">
      <svg class="moon" data-moon viewBox="0 0 80 80" aria-hidden="true" focusable="false"></svg>
      <div>
        <p class="moon-phase" data-n-phase>&nbsp;</p>
        <p class="meta" data-n-illum>&nbsp;</p>
        <p class="meta" data-n-age>&nbsp;</p>
        <p class="meta"><strong>${esc(n.nextNew)}:</strong> <span data-n-nextnew>–</span></p>
        <p class="meta"><strong>${esc(n.nextFull)}:</strong> <span data-n-nextfull>–</span></p>
      </div>
    </div>
    <dl class="facts">${cell(n.moonrise, "data-n-moon=\"rise\"")}${cell(n.moonset, "data-n-moon=\"set\"")}</dl>
  </div>
</section>
<section class="section" aria-labelledby="n-mcal-h">
  <div class="wrap">
    <h2 id="n-mcal-h">${esc(n.moonCalHeading)}</h2>
    <div class="moon-cal" data-n-mooncal></div>
    <p class="meta" data-n-mooncalcap>&nbsp;</p>
  </div>
</section>
<section class="section" aria-labelledby="n-cres-h">
  <div class="wrap">
    <h2 id="n-cres-h">${esc(n.crescentHeading)}</h2>
    <p class="meta" data-n-creslead>&nbsp;</p>
    <div class="table-wrap"><table class="events-table crescent"><thead><tr>${n.crescentCols.map((c) => `<th scope="col">${esc(c)}</th>`).join("")}</tr></thead><tbody data-n-crescent></tbody></table></div>
    <p class="meta">${esc(n.crescentNote)}</p>
  </div>
</section>
<section class="section" aria-labelledby="n-ecl-h">
  <div class="wrap">
    <h2 id="n-ecl-h">${esc(n.eclipseHeading)}</h2>
    <p class="meta" data-n-ecllead>&nbsp;</p>
    <ul class="eclipses" data-n-eclipses></ul>
    <div class="ecl-map" data-ecl-map data-land="${LAND_URL}">
      <h3>${esc(n.eclMapHeading)}</h3>
      <label class="field-label" for="ecl-sel">${esc(n.eclMapLabel)}</label> <select id="ecl-sel" data-ecl-sel></select>
      <canvas data-ecl-canvas width="720" height="360" role="img" aria-label="${esc(n.eclMapHeading)}" hidden></canvas>
      <p class="meta" data-ecl-legend></p>
      <p class="meta">${esc(n.eclMapNote)}</p>
    </div>
    <p class="meta"><strong>${esc(n.eclipseSafety)}</strong></p>
    <p class="meta">${esc(n.eclipseSource)}</p>
    <h3>${esc(n.moonIcsHeading)}</h3>
    <p class="share-row">${(() => { const u = ICS_URL(lang, "moon"), cal = t.calendar; return `<a class="toggle" href="${u}" download>${esc(cal.icsDownload)}</a><a class="toggle" href="${ORIGIN.replace(/^https?:/, "webcal:")}${u}">${esc(cal.icsSubscribe)}</a><a class="toggle" href="https://calendar.google.com/calendar/render?cid=${encodeURIComponent(ORIGIN.replace(/^https?:/, "webcal:") + u)}" target="_blank" rel="noopener">${esc(cal.icsGoogle)}</a>`; })()}</p>
    <p class="meta">${esc(n.moonIcsNote)}</p>
  </div>
</section>
<section class="section" aria-labelledby="n-chart-h">
  <div class="wrap">
    <h2 id="n-chart-h">${esc(n.chartHeading)}</h2>
    <figure class="daychart"><svg data-n-chart viewBox="0 0 640 240" role="img" aria-label="${esc(n.chartHeading)}"></svg><figcaption class="meta" data-n-chartcap>&nbsp;</figcaption></figure>
    <p class="meta" data-n-extremes>&nbsp;</p>
  </div>
</section>
<section class="section" aria-labelledby="n-table-h">
  <div class="wrap">
    <h2 id="n-table-h">${esc(n.tableHeading)}</h2>
    <p class="meta">${esc(n.tableLead)}</p>
    <div class="table-wrap"><table class="events-table"><caption class="sr-only">${esc(n.tableHeading)}</caption><thead><tr><th scope="col">${esc(n.colYear)}</th>${head}</tr></thead><tbody>${rows}</tbody></table></div>
    <p class="meta">${esc(n.meteoNote)}</p>
  </div>
</section>
<section class="section" aria-labelledby="n-facts-h">
  <div class="wrap">
    <h2 id="n-facts-h">${esc(n.factsHeading)}</h2>
    <div class="season-cards">${facts}</div>
    <h2>${esc(n.whyHeading)}</h2>
    ${n.why.map((p) => `<p>${esc(p)}</p>`).join("")}
    <p class="meta">${esc(n.hemisphereNote)}</p>
    <p class="meta">${esc(n.methodNote)}</p>
  </div>
</section>`;
  return simplePage(lang, "nature", { title: n.title, description: n.description, h1: n.h1, lead: n.lead, trail, bodyInner: { hero: `${wheel}<noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>`, sections }, faq: n.faq, faqGroup: { id: "nature", title: t.ui.nature }, scripts: ["place.js", "prayer-calc.js", "nature-calc.js", "nature.js", "eclipse-map.js"], clientExtra: { t: { pl: placeClient(t.prayer), nt: { eclMapSolar: n.eclMapSolar, eclMapLunar: n.eclMapLunar, seasons: n.seasons, current: n.current, dayOf: n.dayOf, nextIn: n.nextIn, startsOn: n.startsOn, phases: n.phases, illumination: n.illumination, age: n.age, dayLonger: n.dayLonger, dayShorter: n.dayShorter, dayLeft: n.dayLeft, chartCaption: n.chartCaption, longest: n.longest, shortest: n.shortest, hoursShort: n.hoursShort, minutesShort: n.minutesShort, turnNames: n.turnNames, turnNotes: n.turnNotes, turnIn: n.turnIn, turnAgo: n.turnAgo, turnToday: n.turnToday, moonNoRise: n.moonNoRise, moonNoSet: n.moonNoSet, moonCalCaption: n.moonCalCaption, eclipseLead: n.eclipseLead, eclipseTypes: n.eclipseTypes, eclipseRegions: n.eclipseRegions, eclipsePath: n.eclipsePath, eclipseHere: n.eclipseHere, eclipseNotHere: n.eclipseNotHere, crescentLead: n.crescentLead, crescentAge: n.crescentAge, crescentLag: n.crescentLag, crescentBefore: n.crescentBefore, crescentCats: n.crescentCats } }, eclipses: ECLIPSES }, nav: "nature", searchK: n.search, needsPlace: true });
}

function buildWeather(lang) {
  const t = I[lang];
  const w = t.weather;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: w.h1, path: ROUTES[lang].weather }];
  const hero = `<section class="wx-hero" data-weather aria-labelledby="wx-h">
      <h2 id="wx-h" class="wx-place" data-wx-place>&nbsp;</h2>
      <p class="meta" data-wx-state role="status">${esc(w.loading)}</p>
      <div class="wx-now" data-wx-now hidden>
        <p class="wx-icon" data-wx-icon aria-hidden="true"></p>
        <p class="wx-temp" data-wx-temp>–</p>
        <p class="wx-cond" data-wx-cond>&nbsp;</p>
        <dl class="wx-facts" data-wx-facts></dl>
      </div>
      <button type="button" class="toggle" data-wx-retry hidden>${esc(w.retry)}</button>
    </section>
    <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>`;
  const sections = `<section class="section" aria-labelledby="wx-hourly-h" data-wx-hourly-box hidden>
  <div class="wrap">
    <h2 id="wx-hourly-h">${esc(w.hourlyHeading)}</h2>
    <ul class="wx-hourly" data-wx-hourly></ul>
  </div>
</section>
<section class="section" aria-labelledby="wx-daily-h" data-wx-daily-box hidden>
  <div class="wrap">
    <h2 id="wx-daily-h">${esc(w.dailyHeading)}</h2>
    <ul class="wx-daily" data-wx-daily></ul>
    <p class="meta"><span data-wx-updated></span> · ${esc(w.attribution)}</p>
    <p class="meta">${esc(w.locationNote)}</p>
  </div>
</section>`;
  const wx = { ...weatherClient(t), now: w.now, pressure: w.pressure, precipitation: w.precipitation, uv: w.uv, sunrise: w.sunrise, sunset: w.sunset, updated: w.updated, retry: w.retry };
  return simplePage(lang, "weather", { title: w.title, description: w.description, h1: w.h1, lead: w.lead, trail, bodyInner: { hero, sections }, faq: w.faq, faqGroup: { id: "weather", title: t.ui.weather }, scripts: ["place.js", "weather-core.js", "weather.js"], clientExtra: { t: { pl: placeClient(t.prayer), wx } }, nav: "weather", searchK: w.search, needsPlace: true, extraLd: [webAppLd(lang, w.h1, ROUTES[lang].weather, w.description)] });
}

/* ---------- Namaz vakitleri: konuma göre hub + 81 il sayfası (hesap tarayıcıda, prayer-calc.js) ---------- */
const PROVINCES = readJson("src/data/provinces.json");
const PRAYER_KEYS = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"];
/* İl adı: Latin dışı yazılı dillerde kendi yazısıyla (provinces.json ar/ru/fa/ja/ko/zh/th); Türkçe ve Azericede Türkçe
   yazımla; diğerlerinde İ → I */
const NATIVE_PROV = ["ar", "ru", "fa", "ja", "ko", "zh", "th"];
const provName = (lang, p) => (p[lang] && NATIVE_PROV.includes(lang) ? p[lang] : lang === "tr" || lang === "az" ? p.name : p.name.replace(/İ/g, "I"));
const prayerPath = (lang, p) => `${ROUTES[lang].prayer}${p.slug}/`;

function prayerBody(lang, prov, extra = "") {
  const t = I[lang];
  const s = t.prayer;
  const rows = PRAYER_KEYS.map((k) => `<tr data-pr="${k}"><th scope="row">${esc(s.names[k])}</th><td data-pr-time>--:--</td></tr>`).join("");
  const shown = prov ? prov.display || provName(lang, prov) : "";
  const attrs = prov ? ` data-mode="city" data-city="${prov.slug}" data-name="${esc(shown)}" data-lat="${prov.lat}" data-lon="${prov.lon}"` : ` data-mode="hub"`;
  /* Solda geri sayım saati, sağda bugünün vakitleri; çerçeve yok. Konum üst bardaki ortak düğmeyle seçilir. */
  const hero = `<section class="pr-top" data-prayer${attrs} aria-labelledby="pr-h">
      <div class="pr-left">
        <h2 id="pr-h" class="pr-place" data-pr-place>${prov ? esc(shown) : "&nbsp;"}</h2>
        <p class="meta" data-pr-nextlabel>&nbsp;</p>
        <p class="big-time" role="timer" data-pr-countdown>–</p>
      </div>
      <div class="pr-right">
        <h3>${esc(s.todayHeading)}</h3>
        <table class="events-table pr-today"><thead class="sr-only"><tr><th scope="col">${esc(s.colPrayer)}</th><th scope="col">${esc(s.colTime)}</th></tr></thead><tbody>${rows}</tbody></table>
        <details class="pr-kerahat"><summary>${esc(s.kerahatHeading)}</summary><ul>${["israk", "istiva", "isfirar"].map((k) => `<li>${esc(s.kerahatNames[k])}: <span data-pr-kr="${k}" dir="ltr">–</span></li>`).join("")}</ul><p class="meta">${esc(s.kerahatNote)}</p></details>
        <details class="pr-kerahat pr-method"><summary>${esc(s.method.heading)}</summary>
          <p class="meta" data-pr-method-active hidden></p>
          <label class="field-label" for="pr-method">${esc(s.method.label)}</label>
          <select id="pr-method" data-pr-method>${Object.keys(s.method.names).map((k) => `<option value="${k}">${esc(s.method.names[k])}</option>`).join("")}</select>
          <label class="field-label" for="pr-asr">${esc(s.method.asrLabel)}</label>
          <select id="pr-asr" data-pr-asr><option value="0">${esc(s.method.asrStd)}</option><option value="1">${esc(s.method.asrHanafi)}</option></select>
          <p class="meta">${esc(s.method.note)}</p>
        </details>
      </div>
      <noscript><p class="meta">${esc(s.noscript)}</p></noscript>
    </section>`;
  const head = PRAYER_KEYS.map((k) => `<th scope="col">${esc(s.names[k])}</th>`).join("");
  const im = `<section class="section" data-imsakiye aria-labelledby="im-h" hidden>
  <div class="wrap">
    <h2 id="im-h" data-im-heading>${esc(s.imHeading)}</h2>
    <p>${esc(s.imText)}</p>
    <div class="im-actions"><button type="button" class="btn" data-im-open aria-haspopup="dialog">${esc(s.imOpen)}</button><button type="button" class="btn btn-ghost" data-im-download>${esc(s.imDownload)}</button></div>
    <dialog class="info-dialog im-dialog" id="im-dialog" aria-labelledby="im-dtitle">
      <h2 id="im-dtitle" class="sr-only" data-im-dtitle>${esc(s.imHeading)}</h2>
      <div class="im-view"><img data-im-img alt="" width="1080" height="1900"></div>
      <div class="im-actions"><button type="button" class="btn" data-im-download>${esc(s.imDownload)}</button><button type="button" class="btn btn-ghost" data-im-close>${esc(t.ui.placeClose)}</button></div>
    </dialog>
  </div>
</section>`;
  const sections = `<section class="section" aria-labelledby="pr-month-h">
  <div class="wrap">
    <div class="pr-month-head">
      <h2 id="pr-month-h" data-pr-month-title>${esc(s.colDay)}</h2>
      <div class="pr-month-nav"><button type="button" class="toggle" data-pr-prev>${esc(s.prevMonth)}</button><button type="button" class="toggle" data-pr-next>${esc(s.nextMonth)}</button></div>
    </div>
    <div class="table-wrap"><table class="events-table pr-month" data-pr-month><caption class="sr-only" data-pr-caption></caption><thead><tr><th scope="col">${esc(s.colDay)}</th>${head}</tr></thead><tbody></tbody></table></div>
  </div>
</section>
${im}
${extra}`;
  return { hero, sections };
}

/* İlçe namaz vakti sayfaları (30 Ekim 2026, rakip analizi #19): yalnız Türkçe (/namaz-vakitleri/<il>/<ilçe>/).
   Koordinatlar GeoNames (CC BY 4.0), uygulamalardaki districts.json; Diyanet ilçe vakitleriyle 32.220 karşılaştırmada
   en çok 1 dk fark. İl merkezi ilçesi (adı ille aynı) il sayfasındadır, ayrı sayfası yok. Diğer dillerde dil seçici il
   sayfasına gider; hreflang yalnız tr. */
const DISTRICTS = readJson("src/data/districts.json").provinces;
const trSlug = (name) => name.toLocaleLowerCase("tr").replace(/ı/g, "i").replace(/ğ/g, "g").replace(/ü/g, "u").replace(/ş/g, "s")
  .replace(/ö/g, "o").replace(/ç/g, "c").normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const districtsOf = (prov) => (DISTRICTS[prov.slug] || [])
  .map(([name, lat, lon]) => ({ name, lat, lon, slug: trSlug(name), prov }))
  .filter((d) => d.slug !== prov.slug)
  .sort((a, b) => a.name.localeCompare(b.name, "tr"));
const districtPath = (d) => `${prayerPath("tr", d.prov)}${d.slug}/`;

/** İl ve ilçe sayfalarının altında kapalı "İlçeler" listesi (yalnız Türkçe; ilk satır il merkezi) */
function districtList(prov, current) {
  const s = I.tr.prayer;
  const list = districtsOf(prov);
  if (!list.length) return "";
  const item = (href, name, on) => `<li>${on ? `<span aria-current="page">${esc(name)}</span>` : `<a href="${href}">${esc(name)}</a>`}</li>`;
  return `<section class="section pr-districts" aria-label="${esc(s.districtsHeading)}">
  <div class="wrap">
    <details${current ? " open" : ""}><summary>${esc(s.districtsHeading)} (${list.length})</summary>
      <ul class="pr-district-list">${item(prayerPath("tr", prov), tpl(s.provinceCentre, { city: provName("tr", prov) }), !current)}${list.map((d) => item(districtPath(d), d.name, current === d.slug)).join("")}</ul>
    </details>
    ${current ? `<p class="meta">${esc(s.districtSource)}</p>` : ""}
  </div>
</section>`;
}
const placeClient = (s) => ({ locating: s.locating, locationDenied: s.locationDenied, locationUnsupported: s.locationUnsupported, yourLocation: s.yourLocation });
const RAMADANS = readJson("src/data/holidays.json").ramadans.filter((r) => r.to);
const prayerClient = (s) => ({ pl: placeClient(s), pr: { nextIn: s.nextIn, monthHeading: s.monthHeading, tableCaption: s.tableCaption, names: s.names, mActive: s.method.active, mNames: s.method.names, hanafi: s.method.hanafiShort }, im: { ramadans: RAMADANS, heading: s.imHeading, title: s.imTitle, alt: s.imAlt, day: s.imDay, date: s.imDate, sahur: s.imSahur, iftar: s.imIftar, note: s.imNote, brand: s.imBrand, file: s.imFile, names: s.names } });
const weatherClient = (t) => { const w = t.weather; return { feelsLike: w.feelsLike, humidity: w.humidity, wind: w.wind, windFrom: w.windFrom, rainChance: w.rainChance, loading: w.loading, error: w.error, codes: w.codes, compass: w.compass, fsNeed: t.client.fsWeatherNeed, fsLink: t.client.fsWeatherLink, fsFail: t.client.fsWeatherFail, consent: t.consent.wxBlocked, allow: t.consent.allow }; };

/* ---------- Saat dilimi kısaltmaları, yaz saati değişimleri, Unix zamanı (30 Ekim 2026, rakip analizi #25) ----------
   Kısaltma adları ICU'dan (Node Intl, temsilci dilim + ay: kış/yaz), TRT elle (ICU'da adı yok). Canlı saat sabit ofsetli
   Etc/GMT±n ya da yaz saati olmayan temsilci dilimle (main.js data-live="time"). Yaz saati tablosu derlemede IANA
   verisinden (sonraki 400 gün), zones.js kalan günü yazar ve geçenleri gizler. */
const TZ_ABBRS = [
  { a: "UTC", tz: "UTC", m: 1, off: 0, live: "UTC", regions: [] },
  { a: "GMT", tz: "Europe/London", m: 1, off: 0, live: "Etc/GMT", regions: ["GB", "IE", "PT", "IS", "GH", "SN", "CI"], pair: ["BST", 60] },
  { a: "BST", tz: "Europe/London", m: 7, off: 60, live: "Etc/GMT-1", regions: ["GB"], pair: ["GMT", 0], summer: true },
  { a: "WET", tz: "Europe/Lisbon", m: 1, off: 0, live: "Etc/GMT", regions: ["PT", "FO"], pair: ["WEST", 60] },
  { a: "CET", tz: "Europe/Berlin", m: 1, off: 60, live: "Etc/GMT-1", regions: ["DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH", "PL", "CZ", "SE", "NO", "DK", "HU", "HR", "RS"], pair: ["CEST", 120] },
  { a: "CEST", tz: "Europe/Berlin", m: 7, off: 120, live: "Etc/GMT-2", regions: ["DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH", "PL", "CZ", "SE", "NO", "DK", "HU", "HR", "RS"], pair: ["CET", 60], summer: true },
  { a: "EET", tz: "Europe/Athens", m: 1, off: 120, live: "Etc/GMT-2", regions: ["GR", "RO", "BG", "FI", "UA", "EE", "LV", "LT", "CY", "EG", "LB"], pair: ["EEST", 180] },
  { a: "EEST", tz: "Europe/Athens", m: 7, off: 180, live: "Etc/GMT-3", regions: ["GR", "RO", "BG", "FI", "UA", "EE", "LV", "LT", "CY", "EG", "LB"], pair: ["EET", 120], summer: true },
  { a: "TRT", tz: "Europe/Istanbul", m: 1, off: 180, live: "Europe/Istanbul", regions: ["TR"], trt: true },
  { a: "MSK", tz: "Europe/Moscow", m: 1, off: 180, live: "Europe/Moscow", regions: ["RU", "BY"] },
  { a: "GST", tz: "Asia/Dubai", m: 1, off: 240, live: "Asia/Dubai", regions: ["AE", "OM"] },
  { a: "PKT", tz: "Asia/Karachi", m: 1, off: 300, live: "Asia/Karachi", regions: ["PK"] },
  { a: "IST", tz: "Asia/Kolkata", m: 1, off: 330, live: "Asia/Kolkata", regions: ["IN", "LK"] },
  { a: "SGT", tz: "Asia/Singapore", m: 1, off: 480, live: "Asia/Singapore", regions: ["SG"] },
  { a: "JST", tz: "Asia/Tokyo", m: 1, off: 540, live: "Asia/Tokyo", regions: ["JP"] },
  { a: "KST", tz: "Asia/Seoul", m: 1, off: 540, live: "Asia/Seoul", regions: ["KR"] },
  { a: "AEST", tz: "Australia/Sydney", m: 7, off: 600, live: "Australia/Brisbane", regions: ["AU"], pair: ["AEDT", 660] },
  { a: "AEDT", tz: "Australia/Sydney", m: 1, off: 660, live: "Etc/GMT-11", regions: ["AU"], pair: ["AEST", 600], summer: true },
  { a: "EST", tz: "America/New_York", m: 1, off: -300, live: "Etc/GMT+5", regions: ["US", "CA", "PA", "JM"], pair: ["EDT", -240] },
  { a: "EDT", tz: "America/New_York", m: 7, off: -240, live: "Etc/GMT+4", regions: ["US", "CA"], pair: ["EST", -300], summer: true },
  { a: "CST", tz: "America/Chicago", m: 1, off: -360, live: "Etc/GMT+6", regions: ["US", "CA", "MX"], pair: ["CDT", -300] },
  { a: "CDT", tz: "America/Chicago", m: 7, off: -300, live: "Etc/GMT+5", regions: ["US", "CA"], pair: ["CST", -360], summer: true },
  { a: "MST", tz: "America/Denver", m: 1, off: -420, live: "America/Phoenix", regions: ["US", "CA", "MX"], pair: ["MDT", -360] },
  { a: "PST", tz: "America/Los_Angeles", m: 1, off: -480, live: "Etc/GMT+8", regions: ["US", "CA"], pair: ["PDT", -420] },
  { a: "PDT", tz: "America/Los_Angeles", m: 7, off: -420, live: "Etc/GMT+7", regions: ["US", "CA"], pair: ["PST", -480], summer: true },
];
const TZ_COMPARE = ["UTC", "GMT", "CET", "CEST", "EET", "TRT", "MSK", "GST", "IST", "JST", "AEST", "EST", "EDT", "CST", "PST"];
const tzName = (lang, z) => z.trt ? I[lang].zones.trtName : new Intl.DateTimeFormat(lang, { timeZone: z.tz, timeZoneName: "long" }).formatToParts(new Date(Date.UTC(2026, z.m - 1, 15))).find((p) => p.type === "timeZoneName").value;
const offTxt = (min) => `UTC${min === 0 ? "+0" : (min < 0 ? "−" : "+") + Math.floor(Math.abs(min) / 60) + (Math.abs(min) % 60 ? ":" + String(Math.abs(min) % 60).padStart(2, "0") : "")}`;
// Hong Kong'un uzun adı ("… ÖİB") başlıklara sığmıyor: kısa biçim
const regionName = (lang, code) => { try { return new Intl.DisplayNames([lang], { type: "region", style: code === "HK" ? "short" : "long" }).of(code); } catch { return code; } };
const zonePath = (lang, z) => `${ROUTES[lang].zones}${z.a.toLowerCase()}/`;
const diffTxt = (lang, min) => { const s = I[lang].zones; const a = Math.abs(min), h = Math.floor(a / 60), m = a % 60; return [h && `${h} ${s.hoursShort}`, m && `${m} ${s.minutesShort}`].filter(Boolean).join(" "); };
const hm24 = (min) => `${String(Math.floor((((min % 1440) + 1440) % 1440) / 60)).padStart(2, "0")}:${String((((min % 1440) + 1440) % 1440) % 60).padStart(2, "0")}`;

function buildZonesHub(lang) {
  const t = I[lang], s = t.zones;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.hubH1, path: ROUTES[lang].zones }];
  const rows = TZ_ABBRS.map((z) => `<tr><th scope="row"><a href="${zonePath(lang, z)}">${z.a}</a></th><td>${esc(tzName(lang, z))}</td><td><bdi dir="ltr">${offTxt(z.off)}</bdi></td><td><span class="tnum" data-live="time" data-tz="${z.live}">--:--</span></td></tr>`).join("");
  const hero = `<div class="table-wrap"><table class="events-table zones-table"><caption class="sr-only">${esc(s.caption)}</caption><thead><tr><th scope="col">${esc(s.colAbbr)}</th><th scope="col">${esc(s.colName)}</th><th scope="col">${esc(s.colOffset)}</th><th scope="col">${esc(s.colNow)}</th></tr></thead><tbody>${rows}</tbody></table></div>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>`;
  return simplePage(lang, "zones", { title: s.hubTitle, description: s.hubDescription, h1: s.hubH1, lead: s.hubLead, trail, bodyInner: { hero }, faq: s.faq, faqGroup: { id: "zones", title: s.hubH1 }, scripts: [], nav: "world", searchK: s.hubSearch });
}

function buildZone(lang, z) {
  const t = I[lang], s = t.zones;
  const name = tzName(lang, z);
  const v = { abbr: z.a, name, offset: offTxt(z.off), pair: z.pair ? z.pair[0] : "", pairOffset: z.pair ? offTxt(z.pair[1]) : "" };
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.hubH1, path: ROUTES[lang].zones }, { name: tpl(s.pageH1, v), path: zonePath(lang, z) }];
  const dst = !z.pair ? s.dstNone : tpl(z.summer ? s.dstSummer : s.dstWinter, v);
  const regions = z.regions.length ? `<p>${esc(z.regions.map((c) => regionName(lang, c)).join(LSEP(lang)))}</p>` : `<p>${esc(s.usedNone)}</p>`;
  const others = TZ_COMPARE.filter((a) => a !== z.a).map((a) => TZ_ABBRS.find((x) => x.a === a));
  const rows = others.map((o) => {
    const d = o.off - z.off;
    const rel = d === 0 ? s.same : tpl(d > 0 ? s.ahead : s.behind, { d: diffTxt(lang, d) });
    return `<tr><th scope="row"><a href="${zonePath(lang, o)}">${o.a}</a> <span class="meta">${esc(tzName(lang, o))}</span></th><td>${esc(rel)}</td><td class="tnum">${hm24(12 * 60 + d)}</td></tr>`;
  }).join("");
  const hero = `<div class="zone-now">
      <p class="meta">${esc(tpl(s.nowLabel, v))}</p>
      <p class="big-time" role="timer" data-live="time" data-sec data-tz="${z.live}">--:--:--</p>
      <p class="info-sub"><bdi dir="ltr">${esc(tpl(s.offsetLine, v))}</bdi></p>
      <p class="meta sync-line" data-sync-status>&nbsp;</p>
    </div>
    <p>${esc(dst)}</p>${s.ambiguous[z.a] ? `<p class="meta">${esc(s.ambiguous[z.a])}</p>` : ""}`;
  const sections = `<section class="section" aria-labelledby="used-h"><div class="wrap">
    <h2 id="used-h">${esc(s.usedIn)}</h2>${regions}
  </div></section>
  <section class="section" aria-labelledby="diff-h"><div class="wrap">
    <h2 id="diff-h">${esc(tpl(s.diffHeading, v))}</h2>
    <div class="table-wrap"><table class="events-table"><thead><tr><th scope="col">${esc(s.colZone)}</th><th scope="col">${esc(s.colDiff)}</th><th scope="col">${esc(tpl(s.colExample, v))}</th></tr></thead><tbody>${rows}</tbody></table></div>
  </div></section>`;
  // Uzun adlarda başlık ve açıklama için kısa genel ad (ör. "Pasifik Saati")
  let title = tpl(s.pageTitle, v), description = tpl(s.pageDescription, v);
  if (title.length > 70 || description.length > 170) {
    const g = new Intl.DateTimeFormat(lang, { timeZone: z.tz, timeZoneName: "longGeneric" }).formatToParts(new Date(Date.UTC(2026, z.m - 1, 15))).find((p) => p.type === "timeZoneName").value;
    if (title.length > 70) title = tpl(s.pageTitle, { ...v, name: g });
    if (description.length > 170) description = tpl(s.pageDescription, { ...v, name: g });
  }
  return simplePage(lang, `zone:${z.a}`, { title, description, h1: tpl(s.pageH1, v), lead: tpl(s.pageLead, v), trail, bodyInner: { hero, sections }, scripts: [], nav: "world", searchK: `${z.a} ${name} ${offTxt(z.off)} saat dilimi time zone` });
}

/* Yaz saati değişimleri: temsilci dilimde 6 saatlik adımla ofset değişimi aranır, dakikaya kadar ikiye bölünür */
const DST_GROUPS = [
  ["eu", "Europe/Berlin", ["EU", "GB", "NO", "CH", "UA"]], ["us", "America/New_York", ["US", "CA"]], ["cl", "America/Santiago", ["CL"]],
  ["au", "Australia/Sydney", ["AU"]], ["nz", "Pacific/Auckland", ["NZ"]], ["il", "Asia/Jerusalem", ["IL"]], ["lb", "Asia/Beirut", ["LB"]],
  ["eg", "Africa/Cairo", ["EG"]], ["ma", "Africa/Casablanca", ["MA"]], ["cu", "America/Havana", ["CU"]], ["md", "Europe/Chisinau", ["MD"]],
  ["ps", "Asia/Gaza", ["PS"]], ["gl", "America/Nuuk", ["GL"]],
];
const tzOffCache = new Map();
function tzOffsetMin(tz, ms) {
  let f = tzOffCache.get(tz);
  if (!f) { f = new Intl.DateTimeFormat("en-US", { timeZone: tz, timeZoneName: "longOffset" }); tzOffCache.set(tz, f); }
  const v = f.formatToParts(new Date(ms)).find((p) => p.type === "timeZoneName").value;
  const m = v.match(/([+-])(\d\d):(\d\d)/);
  return m ? (m[1] === "-" ? -1 : 1) * (+m[2] * 60 + +m[3]) : 0;
}
function tzTransitions(tz, from, to) {
  const H6 = 6 * 3600000, out = [];
  let prev = tzOffsetMin(tz, from);
  for (let t = from + H6; t <= to; t += H6) {
    const cur = tzOffsetMin(tz, t);
    if (cur === prev) continue;
    let lo = t - H6, hi = t;
    while (hi - lo > 60000) { const mid = lo + Math.floor((hi - lo) / 120000) * 60000; if (mid === lo) break; if (tzOffsetMin(tz, mid) === prev) lo = mid; else hi = mid; }
    out.push({ at: hi, from: prev, to: cur });
    prev = cur;
  }
  return out;
}
const DST_FROM = Date.UTC(...cfg.updated.split("-").map((x, i) => (i === 1 ? +x - 1 : +x)));
const DST_ROWS = DST_GROUPS.flatMap(([id, tz, regions]) => tzTransitions(tz, DST_FROM, DST_FROM + 400 * 86400000).map((x) => ({ id, tz, regions, ...x }))).sort((a, b) => a.at - b.at);

function buildDst(lang) {
  const t = I[lang], s = t.dst, cal = t.calendar;
  const F = makeFmt(cal);
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].dst }];
  const cityOf = (tz) => { const c = CITIES.find((x) => x.tz === tz); return c ? c[lang].name : tz.split("/").pop().replace(/_/g, " "); };
  const rows = DST_ROWS.map((r) => {
    const localBefore = r.at + r.from * 60000, localAfter = r.at + r.to * 60000;
    const d = new Date(localAfter), date = `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, "0")}-${String(d.getUTCDate()).padStart(2, "0")}`;
    const fromT = hm24(Math.round((localBefore % 86400000) / 60000)), toT = hm24(Math.round((localAfter % 86400000) / 60000));
    const step = Math.abs(r.to - r.from);
    const tplKey = r.to > r.from ? (step === 60 ? s.forward : s.forwardN) : (step === 60 ? s.back : s.backN);
    const change = tpl(tplKey, { from: fromT, to: toT, d: diffTxt(lang, step) }) + ` (${cityOf(r.tz)})`;
    return `<tr data-at="${r.at}"><th scope="row">${esc(F.full(date))}</th><td><strong>${esc(s.groups[r.id])}</strong>${r.regions.length > 1 ? `<br><span class="meta">${esc(r.regions.map((c) => regionName(lang, c)).join(LSEP(lang)))}</span>` : ""}</td><td>${esc(change)}</td><td data-dst-left></td></tr>`;
  }).join("");
  const hero = `<p class="info-sub">${esc(s.turkeyNote)}</p>`;
  const sections = `<section class="section"><div class="wrap">
    <div class="table-wrap"><table class="events-table dst-table" data-dst data-today="${esc(s.today)}" data-tomorrow="${esc(s.tomorrow)}" data-in="${esc(s.inDays)}"><caption>${esc(s.caption)}</caption><thead><tr><th scope="col">${esc(s.colDate)}</th><th scope="col">${esc(s.colWhere)}</th><th scope="col">${esc(s.colChange)}</th><th scope="col">${esc(s.colLeft)}</th></tr></thead><tbody>${rows}</tbody></table></div>
    <p class="meta">${esc(s.note)}</p>
  </div></section>`;
  return simplePage(lang, "dst", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero, sections }, faq: s.faq, faqGroup: { id: "dst", title: s.h1 }, scripts: ["zones.js"], nav: "world", searchK: s.search });
}

/* Saat dilimi haberleri (31 Ekim 2026, rakip analizi listesi #100): IANA tz veritabanının NEWS dosyasındaki
   geleceğe dönük değişikliklerden (kamu malı) elle yazılmış özetler, src/data/tz-news.json (beş dil). Yeni tzdata
   sürümü çıkınca NEWS okunur, yeni karar eklenir, `latest` güncellenir. Her dilde Atom akışı /feeds/<dil>-tz-news.xml.
   E-posta bülteni yok: e-posta yalnız ortak Bumba listesinden gider (standart). */
const TZN = readJson("src/data/tz-news.json");
const TZN_FEED = (lang) => `/feeds/${lang}-tz-news.xml`;
function buildTzNews(lang) {
  const t = I[lang], s = t.tzn, cal = t.calendar;
  const F = makeFmt(cal);
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].tzn }];
  const items = [...TZN.items].sort((a, b) => b.date.localeCompare(a.date));
  const zoneName = (tz) => { const c = CITIES.find((x) => x.tz === tz); return c ? c[lang].name : tz.split("/").pop().replace(/_/g, " "); };
  const list = items.map((it) => {
    const [h, p] = it.t[lang];
    const clocks = it.zones.map((z) => `<li><span class="meta">${esc(zoneName(z))}</span> <span data-live="time" data-tz="${z}" dir="ltr">--:--</span></li>`).join("");
    return `<article class="tzn-item" id="${it.id}"><h2>${esc(h)}</h2>
      <p class="meta">${esc(tpl(s.effective, { date: F.full(it.date) }))} · ${esc(it.cc.map((c) => regionName(lang, c)).join(LSEP(lang)))}</p>
      <p>${esc(p)}</p>
      <ul class="tzn-now" aria-label="${esc(s.nowIn)}">${clocks}</ul>
      <p class="meta">${esc(tpl(s.source, { rel: it.release, date: F.full(it.releaseDate) }))}</p>
    </article>`;
  }).join("");
  const hero = `<p class="info-sub">${esc(tpl(s.latest, { rel: TZN.latest, date: F.full(TZN.latestDate) }))}</p>`;
  const sections = `<section class="section"><div class="wrap tzn">
    ${list}
    <p><a href="${TZN_FEED(lang)}" type="application/atom+xml">${esc(s.feed)}</a></p>
    <p class="meta">${esc(s.feedNote)}</p>
    <p class="meta">${esc(s.note)}</p>
  </div></section>`;
  // Atom akışı (en yeni 30 haber; güncelleme = tzdata duyuru tarihi)
  const abs = (u) => ORIGIN + u;
  const entries = items.slice(0, 30).map((it) => {
    const [h, p] = it.t[lang];
    return `  <entry>
    <id>tag:${new URL(ORIGIN).host},2026:tz-news/${it.id}</id>
    <title>${esc(h)}</title>
    <updated>${it.releaseDate}T00:00:00Z</updated>
    <link rel="alternate" href="${abs(ROUTES[lang].tzn)}#${it.id}"/>
    <summary>${esc(p)}</summary>
  </entry>`;
  }).join("\n");
  const updated = items.reduce((m, it) => (it.releaseDate > m ? it.releaseDate : m), "");
  write(TZN_FEED(lang).slice(1), `<?xml version="1.0" encoding="utf-8"?>
<feed xmlns="http://www.w3.org/2005/Atom" xml:lang="${lang}">
  <id>${abs(ROUTES[lang].tzn)}</id>
  <title>${esc(s.feedTitle)}</title>
  <updated>${updated}T00:00:00Z</updated>
  <link rel="alternate" href="${abs(ROUTES[lang].tzn)}"/>
  <link rel="self" href="${abs(TZN_FEED(lang))}"/>
  <author><name>Saat Tarih</name></author>
${entries}
</feed>
`);
  return simplePage(lang, "tzn", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero, sections }, faq: s.faq, faqGroup: { id: "tzn", title: s.h1 }, nav: "world", searchK: s.search });
}

function buildUnix(lang) {
  const t = I[lang], s = t.unix;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].unix }];
  const res = (k, label) => `<div class="fact"><dt>${esc(label)}</dt><dd data-ux-${k}>–</dd></div>`;
  const hero = `<section class="unix" data-unix aria-labelledby="ux-h">
      <h2 id="ux-h" class="sr-only">${esc(s.nowLabel)}</h2>
      <p class="meta">${esc(s.nowLabel)}</p>
      <p class="big-time unix-now" role="timer" data-ux-now dir="ltr">–</p>
      <p class="meta"><span data-ux-now-ms dir="ltr">–</span> ${esc(s.milliseconds)} · <button type="button" class="toggle" data-ux-copy data-copied="${esc(s.copied)}">${esc(s.copy)}</button></p>
      <p class="meta sync-line" data-sync-status>&nbsp;</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
  const sections = `<section class="section" aria-labelledby="ux-to-h"><div class="wrap">
    <h2 id="ux-to-h">${esc(s.toDateHeading)}</h2>
    <div class="field"><label for="ux-ts">${esc(s.tsLabel)}</label><input id="ux-ts" type="text" inputmode="numeric" autocomplete="off" dir="ltr" data-ux-ts></div>
    <p class="meta" data-ux-detect role="status" aria-live="polite">&nbsp;</p>
    <dl class="facts unix-res">${res("utc", s.resUtc)}${res("local", s.resLocal)}${res("iso", s.resIso)}${res("rel", s.resRel)}</dl>
  </div></section>
  <section class="section" aria-labelledby="ux-from-h"><div class="wrap">
    <h2 id="ux-from-h">${esc(s.fromDateHeading)}</h2>
    <div class="inputs-row"><div class="field"><label for="ux-date">${esc(s.dateLabel)}</label><input id="ux-date" type="datetime-local" step="1" data-ux-date></div>
    <div class="field"><label for="ux-zone">${esc(s.zoneLabel)}</label><select id="ux-zone" data-ux-zone><option value="local">${esc(s.zoneLocal)}</option><option value="utc">${esc(s.zoneUtc)}</option></select></div></div>
    <dl class="facts unix-res">${res("sec", s.resSec)}${res("ms", s.resMs)}</dl>
  </div></section>
  <section class="section" aria-labelledby="ux-38-h"><div class="wrap">
    <h2 id="ux-38-h">${esc(s.y2038Heading)}</h2>
    <p>${esc(s.y2038)}</p>
    <p class="meta" data-ux-2038>&nbsp;</p>
    <h2>${esc(s.factsHeading)}</h2>
    <ul>${s.facts.map((x) => `<li>${esc(x)}</li>`).join("")}</ul>
  </div></section>`;
  const ux = { invalid: s.invalid, detectedMs: s.detectedMs, detectedSec: s.detectedSec, ago: s.ago, in: s.in, y2038Left: s.y2038Left, days: s.days, hours: s.hours, minutes: s.minutes };
  return simplePage(lang, "unix", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero, sections }, faq: s.faq, faqGroup: { id: "unix", title: s.h1 }, scripts: ["zones.js"], clientExtra: { t: { ux } }, nav: "tools", searchK: s.search, extraLd: [webAppLd(lang, s.h1, ROUTES[lang].unix, s.description)] });
}

/* ---------- Dünya resmî tatilleri (30 Ekim 2026, rakip analizi #27) ----------
   Veri src/data/world-holidays.json (Nager.Date, MIT; scripts/fetch-world-holidays.mjs ile elle güncellenir), yalnız
   ülke genelindeki resmî tatiller. Ad: İngilizce sayfada İngilizce; ülkenin dilindeki sayfada (de: DE/AT/CH, ar: EG, fr: FR, ru: RU, ja: JP, zh: CN)
   yerel ad; diğerlerinde src/data/holiday-names.json çevirisi, yoksa İngilizce. Yerel ad ayrıca gösterilir. */
const WHOL = readJson("src/data/world-holidays.json");
const HOLNAMES = readJson("src/data/holiday-names.json").names;
const WCC = Object.keys(WHOL.countries);
const NATIVE_CC = { de: ["DE", "AT", "CH"], ar: ["EG"], fr: ["FR"], ru: ["RU"], ja: ["JP"], zh: ["CN"] };
const holName = (lang, cc, h) => {
  const main = lang === "en" ? h.n : (NATIVE_CC[lang] || []).includes(cc) && h.l !== h.n ? h.l : (HOLNAMES[h.n] && HOLNAMES[h.n][lang]) || h.n;
  return { main, local: h.l && h.l !== main && h.l !== h.n ? h.l : "" };
};
const countrySlug = (lang, cc) => trSlug(regionName(lang === "tr" ? "tr" : "en", cc));
const worldHolPath = (lang, cc) => `${ROUTES[lang].worldHol}${countrySlug(lang, cc)}/`;
const holLeftAttrs = (s) => `data-hol data-today="${esc(s.today)}" data-tomorrow="${esc(s.tomorrow)}" data-in="${esc(s.inDays)}" data-passed="${esc(s.passed)}"`;

function buildWorldHolHub(lang) {
  const t = I[lang], s = t.worldHol, F = makeFmt(t.calendar);
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.hubH1, path: ROUTES[lang].worldHol }];
  const from = cfg.updated, y = BUILD_YEAR;
  const rows = [...WCC.map((cc) => ({ cc, name: regionName(lang, cc) })), { cc: "TR", name: s.turkey }]
    .sort((a, b) => a.name.localeCompare(b.name, lang))
    .map(({ cc, name }) => {
      if (cc === "TR") {
        const next = HOL_YEARS.flatMap((yy) => YEAR_DATA[yy].events).find((e) => e.type === "holiday" && e.date >= from);
        return `<tr data-d="${next ? next.date : ""}"><th scope="row"><a href="${yearPath(lang, y)}">${esc(name)}</a></th><td>${next ? `${esc(t.calendar.events[next.key])} <span class="meta">${esc(F.short(next.date))}</span>` : "–"}</td><td data-left></td><td>${holidayCounts(YEAR_DATA[y].events).n}</td></tr>`;
      }
      const all = WHOL.years.flatMap((yy) => (WHOL.countries[cc][yy] || { days: [] }).days);
      const next = all.find((h) => h.d >= from);
      return `<tr data-d="${next ? next.d : ""}"><th scope="row"><a href="${worldHolPath(lang, cc)}">${esc(name)}</a></th><td>${next ? `${esc(holName(lang, cc, next).main)} <span class="meta">${esc(F.short(next.d))}</span>` : "–"}</td><td data-left></td><td>${(WHOL.countries[cc][y] || { days: [] }).days.length}</td></tr>`;
    }).join("");
  const hero = `<div class="table-wrap"><table class="events-table" ${holLeftAttrs(s)}><thead><tr><th scope="col">${esc(s.colCountry)}</th><th scope="col">${esc(s.colNext)}</th><th scope="col">${esc(s.colLeft)}</th><th scope="col">${esc(tpl(s.colCount, { year: y }))}</th></tr></thead><tbody>${rows}</tbody></table></div>
    <p class="meta">${esc(tpl(s.source, { date: WHOL.fetched }))}</p>`;
  return simplePage(lang, "worldHol", { title: s.hubTitle, description: s.hubDescription, h1: s.hubH1, lead: s.hubLead, trail, bodyInner: { hero }, faq: s.faq, faqGroup: { id: "worldHol", title: s.hubH1 }, scripts: ["zones.js"], nav: "calendar", searchK: s.hubSearch });
}

const EN_THE = ["GB", "US", "NL"]; // "Public holidays in the United Kingdom"
function buildWorldHol(lang, cc) {
  const t = I[lang], s = t.worldHol, cal = t.calendar, F = makeFmt(cal);
  const country = (lang === "en" && EN_THE.includes(cc) ? "the " : "") + regionName(lang, cc);
  const years = WHOL.years;
  const y = BUILD_YEAR + 1 <= years[years.length - 1] ? BUILD_YEAR + 1 : years[0];
  const dy = WHOL.countries[cc][y].days;
  const v = { country, from: years[0], to: years[years.length - 1], year: y, n: dy.length, w: dy.filter((h) => dowOf(h.d) < 5).length };
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.hubH1, path: ROUTES[lang].worldHol }, { name: tpl(s.pageH1, v), path: worldHolPath(lang, cc) }];
  const sections = years.map((yy) => {
    const d = WHOL.countries[cc][yy] || { days: [], regional: 0 };
    const rows = d.days.map((h) => {
      const nm = holName(lang, cc, h);
      return `<tr data-d="${h.d}"><th scope="row">${esc(F.short(h.d))}</th><td>${esc(cal.weekdays[dowOf(h.d)])}</td><td>${esc(nm.main)}${nm.local ? `<br><span class="meta" lang="">${esc(tpl(s.localName, { name: nm.local }))}</span>` : ""}</td><td data-left></td></tr>`;
    }).join("");
    const sum = tpl(s.summary, { year: yy, n: d.days.length, w: d.days.filter((h) => dowOf(h.d) < 5).length });
    return `<section class="section" aria-labelledby="wh-${yy}"><div class="wrap">
    <h2 id="wh-${yy}">${yy}</h2>
    <p class="meta">${esc(sum)}${d.regional ? " " + esc(tpl(s.regional, { year: yy, n: d.regional })) : ""}</p>
    <div class="table-wrap"><table class="events-table" ${holLeftAttrs(s)}><caption class="sr-only">${esc(tpl(s.caption, { country, year: yy }))}</caption><thead><tr><th scope="col">${esc(s.colDate)}</th><th scope="col">${esc(s.colDay)}</th><th scope="col">${esc(s.colName)}</th><th scope="col">${esc(s.colLeft)}</th></tr></thead><tbody>${rows}</tbody></table></div>
  </div></section>`;
  }).join("\n");
  const hero = `<p class="info-sub" data-hol-next data-label="${esc(s.nextLabel)}">&nbsp;</p>`;
  const tail = `<section class="section"><div class="wrap"><p class="meta">${esc(tpl(s.source, { date: WHOL.fetched }))}</p></div></section>`;
  // Uzun ülke adında açıklamanın ikinci cümlesi atlanır (170 karakter sınırı)
  let description = tpl(s.pageDescription, v);
  if (description.length > 170) description = description.slice(0, description.indexOf(". ") + 1);
  return simplePage(lang, `worldHol:${cc}`, { title: tpl(s.pageTitle, v), description, h1: tpl(s.pageH1, v), lead: tpl(s.pageLead, v), trail, bodyInner: { hero, sections: sections + tail }, scripts: ["zones.js"], nav: "calendar", searchK: `${country} ${regionName("en", cc)} ${s.search}` });
}

/* ---------- Köprü tatil planlayıcı (30 Ekim 2026, rakip analizi #22): lib/bridges.mjs ---------- */
function buildBridge(lang) {
  const t = I[lang];
  const s = t.bridge;
  const cal = t.calendar;
  const F = makeFmt(cal);
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].bridge }];
  const num = (n) => n.toLocaleString(t.locale);
  const gname = (g) => s.groups[g] || cal.events[g];
  const dayFmt = (d) => `${F.short(d)}${d.slice(0, 4) !== d0year ? ` ${d.slice(0, 4)}` : ""} ${cal.weekdaysShort[dowOf(d)]}`;
  let d0year = "";
  let bestFaq = null;
  const years = HOL_YEARS.map((y) => {
    d0year = String(y);
    const ev = [y - 1, y, y + 1].flatMap((x) => (YEAR_DATA[x] || computeYear(x, HOL)).events).filter((e) => e.type === "holiday" || e.type === "half");
    const list = bridges(y, ev);
    const paid = list.filter((b) => b.leave > 0);
    // En verimli: en az bir haftalık dönemler arasında gün/izin oranı en yüksek olan (yoksa bütün izinli dönemler)
    const pool = paid.filter((b) => b.days >= 7).length ? paid.filter((b) => b.days >= 7) : paid;
    const best = pool.length ? pool.reduce((a, b) => (b.days / b.leave > a.days / a.leave ? b : a)) : null;
    if (best && y === BUILD_YEAR + 1) bestFaq = { year: y, name: best.groups.map(gname).join(" + "), leave: num(best.leave), days: best.days, dates: `${dayFmt(best.from)} – ${dayFmt(best.to)}` };
    const rows = list.map((b) => {
      const days = [...b.leaveDays.map((d) => [d, dayFmt(d)]), ...b.halfDays.map((d) => [d, tpl(s.halfNote, { date: dayFmt(d) })])].sort((x, y) => (x[0] < y[0] ? -1 : 1)).map((x) => x[1]);
      return `<tr data-leave="${b.leave}"${b === best ? ' class="br-best"' : ""}><th scope="row">${esc(b.groups.map(gname).join(" + "))}${b === best ? ` <span class="badge">${esc(s.best)}</span>` : ""}</th><td>${esc(dayFmt(b.from))} – ${esc(dayFmt(b.to))}</td><td>${b.leave ? esc(tpl(s.leaveN, { n: num(b.leave) })) : esc(s.noLeave)}</td><td><strong>${esc(tpl(s.totalN, { n: b.days }))}</strong></td><td>${days.length ? esc(days.join(LSEP(lang))) : esc(s.none)}</td></tr>`;
    }).join("");
    return `<section class="br-year" data-br-year="${y}" aria-labelledby="br-${y}">
    <h2 id="br-${y}">${y}</h2>
    ${YEAR_DATA[y].projected ? `<p class="meta">${esc(s.projected)}</p>` : ""}
    <div class="table-wrap"><table class="events-table br-table"><caption class="sr-only">${esc(tpl(s.caption, { year: y }))}</caption><thead><tr><th scope="col">${esc(s.colHoliday)}</th><th scope="col">${esc(s.colDates)}</th><th scope="col">${esc(s.colLeave)}</th><th scope="col">${esc(s.colTotal)}</th><th scope="col">${esc(s.colDays)}</th></tr></thead><tbody>${rows}</tbody></table></div>
  </section>`;
  }).join("\n");
  const opts = HOL_YEARS.map((y) => `<option value="${y}"${y === BUILD_YEAR ? " selected" : ""}>${y}</option>`).join("");
  const maxOpts = [`<option value="9">${esc(s.maxAll)}</option>`, ...[1, 2, 3, 4].map((n) => `<option value="${n}">${esc(tpl(s.maxN, { n }))}</option>`)].join("");
  const hero = `<div class="br-controls" data-bridge hidden>
      <div class="field"><label for="br-y">${esc(s.yearLabel)}</label><select id="br-y" data-br-y>${opts}</select></div>
      <div class="field"><label for="br-max">${esc(s.maxLabel)}</label><select id="br-max" data-br-max>${maxOpts}</select></div>
    </div>`;
  const sections = `<section class="section"><div class="wrap">
  ${years}
  <p class="meta">${esc(s.note)}</p>
</div></section>`;
  const faq = [...s.faq, ...(bestFaq ? [{ q: tpl(s.faqBest.q, bestFaq), a: tpl(s.faqBest.a, bestFaq) }] : [])];
  return simplePage(lang, "bridge", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero, sections }, faq, faqGroup: { id: "bridge", title: s.h1 }, scripts: ["bridge.js"], nav: "tools", searchK: s.search, extraLd: [webAppLd(lang, s.h1, ROUTES[lang].bridge, s.description)] });
}

/* ---------- Kıble (30 Ekim 2026, rakip analizi #8): pusula, açı, uzaklık, kıble saati, yakındaki camiler ---------- */
function qiblaDial(lang) {
  const s = I[lang].qibla;
  const ticks = Array.from({ length: 72 }, (_, i) => {
    const major = i % 6 === 0, a = (i * 5 * Math.PI) / 180, r1 = major ? 126 : 132;
    const f = (r) => `${(Math.sin(a) * r).toFixed(1)} ${(-Math.cos(a) * r).toFixed(1)}`;
    return `<path d="M${f(r1)}L${f(140)}" class="${i % 18 === 0 ? "qb-tick-c" : major ? "qb-tick-m" : "qb-tick"}"/>`;
  }).join("");
  const letters = s.cardinals.split(" ").map((c, i) => `<text class="qb-card${i === 0 ? " qb-north" : ""}" transform="rotate(${i * 90}) translate(0 -104) rotate(${-i * 90})" text-anchor="middle" dominant-baseline="central">${esc(c)}</text>`).join("");
  return `<svg class="qb-svg" viewBox="-165 -165 330 330" role="img" aria-labelledby="qb-svg-t" focusable="false"><title id="qb-svg-t">${esc(s.angle)}</title>
  <g data-qb-rose><circle r="141" class="qb-face"/>${ticks}${letters}
    <g data-qb-kaaba><rect x="-9" y="-161" width="18" height="18" rx="2" class="qb-kaaba"/><rect x="-9" y="-156" width="18" height="3" class="qb-kaaba-band"/></g>
  </g>
  <g data-qb-needle><path d="M0 -112L13 0H-13Z" class="qb-head"/><path d="M-13 0H13L0 92Z" class="qb-tail"/></g>
  <circle r="7" class="qb-hub"/>
</svg>`;
}

/* Zekât hesaplama (30 Ekim 2026, sahibin isteği; uygulamalardaki İbadet › Zekât ile aynı hesap): nisab 80,18 g altın
   (Diyanet), oran 1/40, altın fiyatını kullanıcı yazar; tutarlar kaydedilmez, gönderilmez. Betik zakat.js. */
const ZAKAT_FIELDS = ["cash", "goldGrams", "goldPrice", "silverValue", "foreign", "trade", "receivable", "debt"];
const FITRE = readJson("src/data/fitre.json");
function buildZakat(lang) {
  const t = I[lang], s = t.zakat;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].zakat }];
  const field = (k) => `<div class="field"><label for="zk-${k}">${esc(s.fields[k])}</label><input id="zk-${k}" type="text" inputmode="decimal" autocomplete="off" dir="ltr" data-zk="${k}"></div>`;
  const res = (k, label) => `<div class="fact"><dt${k === "nisab" ? " data-zk-nisab-label" : ""}>${esc(label)}</dt><dd data-zk-${k} dir="ltr">–</dd></div>`;
  const nisabLabel = tpl(s.nisab, { g: new Intl.NumberFormat(RTL.includes(lang) ? "en" : I[lang].locale, { maximumFractionDigits: 2 }).format(80.18) });
  const hero = `<section class="zk" data-zakat aria-labelledby="zk-h">
      <h2 id="zk-h" class="sr-only">${esc(s.h1)}</h2>
      <p class="meta">${esc(s.intro)}</p>
      <div class="zk-grid">${ZAKAT_FIELDS.map(field).join("")}</div>
      <h2 class="zk-res-h">${esc(s.resultHeading)}</h2>
      <dl class="facts zk-res">${res("net", s.net)}${res("nisab", nisabLabel)}<div class="fact" data-zk-amount-box hidden><dt>${esc(s.amount)}</dt><dd data-zk-amount dir="ltr">–</dd></div></dl>
      <p class="zk-verdict" data-zk-verdict role="status" aria-live="polite">${esc(s.needGold)}</p>
      <p class="meta">${esc(s.privacy)}</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
  const fy = Object.keys(FITRE.years).sort().pop(), fv = FITRE.years[fy];
  const fitre = `<section class="zk-fitre" data-fitre="${fv.amount}" aria-labelledby="zk-f-h">
      <h2 id="zk-f-h" class="zk-res-h">${esc(s.fitreHeading)}</h2>
      <p>${esc(tpl(s.fitreText, { year: fy, amount: fv.amount, date: fmtDate(lang, fv.date) }))}</p>
      <div class="zk-grid"><div class="field"><label for="zk-people">${esc(s.fitrePeople)}</label><input id="zk-people" type="number" inputmode="numeric" min="1" max="99" value="1" data-fitre-people></div></div>
      <dl class="facts zk-res"><div class="fact"><dt>${esc(s.fitreTotal)}</dt><dd data-fitre-total dir="ltr">${fv.amount} TL</dd></div></dl>
      <p class="meta">${esc(s.fitreNote)}</p>
    </section>`;
  const sections = `<section class="section"><div class="wrap">${fitre}<p class="meta">${esc(s.note)}</p></div></section>`;
  const zk = { needGold: s.needGold, due: s.due, notDue: s.notDue };
  return simplePage(lang, "zakat", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero, sections }, faq: s.faq, faqGroup: { id: "zakat", title: s.h1 }, scripts: ["zakat.js"], clientExtra: { t: { zk } }, nav: "prayer", searchK: s.search, extraLd: [webAppLd(lang, s.h1, ROUTES[lang].zakat, s.description)] });
}

/* ---------- Ek araçlar (30 Ekim 2026, rakip analizi K maddeleri 13, 31, 32, 36–39, 41) ----------
   Hicri takvim çevirici, saat hesaplama, kaç gün kaldı, aralık zamanlayıcı, satranç saati, metronom, konuşan saat,
   burç hesaplama. Hepsi tarayıcıda çalışır (more-tools.js), dış istek yok; ses Web Audio ve cihazdaki konuşma motoru. */
const MORE_KEYS = ["hijriConv", "timeCalc", "daysUntil", "interval", "chess", "metronome", "talking", "zodiac"];
const HIJRI_TABLE = hijriTable(HOL, readJson("src/data/hijri.json"));
const DU_KEYS = ["newYear", "ramadanStart", "rb1", "apr23", "may1", "may19", "kb1", "jul15", "aug30", "oct29", "hijri", "ashura", "mawlid", "regaib", "mirac", "berat", "kadir", "feb14", "mothers", "fathers"];
// Burçların yaklaşık başlangıç günleri (tropikal zodyak; Koç'tan başlayarak [ay, gün])
const ZODIAC_START = [[3, 21], [4, 20], [5, 21], [6, 21], [7, 23], [8, 23], [9, 23], [10, 23], [11, 22], [12, 22], [1, 20], [2, 19]];
const ZODIAC_SYM = ["♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓"];
const mtField = (id, label, input) => `<div class="field"><label for="${id}">${esc(label)}</label>${input}</div>`;
const mtNum = (id, label, attrs) => mtField(id, label, `<input id="${id}" type="number" inputmode="numeric" ${attrs}>`);
const mtFsBtn = (t) => `<button type="button" class="icon-btn" data-fs aria-label="${esc(t.client.fullscreen)}" title="${esc(t.client.fullscreen)}"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/></svg></button>`;
function moreBody(lang, key) {
  const t = I[lang], s = t[key];
  const res = (attr, label) => `<div class="fact"><dt>${esc(label)}</dt><dd ${attr}>–</dd></div>`;
  if (key === "hijriConv") {
    const cur = HIJRI_TABLE.filter((r) => r[0] <= cfg.updated).pop() || HIJRI_TABLE[0];
    const hy = cur[1];
    const rows = HIJRI_TABLE.map((r, i) => [r, HIJRI_TABLE[i + 1]]).filter(([r, n]) => r[1] === hy && n)
      .map(([r, n]) => `<tr><th scope="row">${esc(s.months[r[2] - 1])}</th><td>${esc(fmtDate(lang, r[0]))}</td><td>${Math.round((Date.parse(n[0]) - Date.parse(r[0])) / 86400000)}</td></tr>`).join("");
    const hero = `<section class="mt" data-mt="hijri" aria-labelledby="hj-h">
      <h2 id="hj-h" class="sr-only">${esc(s.todayLabel)}</h2>
      <p class="meta">${esc(s.todayLabel)}</p>
      <p class="big-time mt-big" data-hj-today>–</p>
      <p class="meta" data-hj-today-greg>&nbsp;</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
    const sections = `<section class="section" aria-labelledby="hj-a"><div class="wrap">
      <h2 id="hj-a">${esc(s.toHijri)}</h2>
      <div class="inputs-row">${mtField("hj-g", s.gregLabel, '<input id="hj-g" type="date" min="1900-01-01" max="2100-12-31" data-hj-g>')}</div>
      <p class="mt-result" data-hj-r1 role="status" aria-live="polite">&nbsp;</p><p class="meta" data-hj-n1 hidden></p>
    </div></section>
    <section class="section" aria-labelledby="hj-b"><div class="wrap">
      <h2 id="hj-b">${esc(s.toGreg)}</h2>
      <div class="inputs-row">${mtNum("hj-y", s.yearLabel, `min="1300" max="1600" value="${hy}" data-hj-y`)}${mtField("hj-m", s.monthLabel, `<select id="hj-m" data-hj-m>${s.months.map((m, i) => `<option value="${i + 1}">${esc(m)}</option>`).join("")}</select>`)}${mtNum("hj-d", s.dayLabel, 'min="1" max="30" value="1" data-hj-d')}</div>
      <p class="mt-result" data-hj-r2 role="status" aria-live="polite">&nbsp;</p><p class="meta" data-hj-n2 hidden></p>
    </div></section>
    <section class="section" aria-labelledby="hj-c"><div class="wrap">
      <div class="table-wrap"><table class="events-table" data-hj-table><caption id="hj-c">${esc(tpl(s.yearHeading, { y: hy }))}</caption><thead><tr><th scope="col">${esc(s.colMonth)}</th><th scope="col">${esc(s.colStart)}</th><th scope="col">${esc(s.colDays)}</th></tr></thead><tbody data-hj-tbody>${rows}</tbody></table></div>
      <p class="meta">${esc(s.note)}</p>
    </div></section>`;
    return { hero, sections, client: { months: s.months, fmt: s.fmt, outside: s.outside, dayMax: s.dayMax, invalid: s.invalid, yearHeading: s.yearHeading, table: HIJRI_TABLE } };
  }
  if (key === "timeCalc") {
    const hero = `<section class="mt" data-mt="timecalc" aria-labelledby="tc-a">
      <h2 id="tc-a">${esc(s.betweenHeading)}</h2>
      <div class="inputs-row">${mtField("tc-s", s.startLabel, '<input id="tc-s" type="time" value="09:00" data-tc-s>')}${mtField("tc-e", s.endLabel, '<input id="tc-e" type="time" value="17:30" data-tc-e>')}</div>
      <p class="mt-result" data-tc-r1 role="status" aria-live="polite">&nbsp;</p><p class="meta" data-tc-n1>&nbsp;</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
    const sections = `<section class="section" aria-labelledby="tc-b"><div class="wrap">
      <h2 id="tc-b">${esc(s.addHeading)}</h2>
      <div class="inputs-row">${mtField("tc-t", s.timeLabel, '<input id="tc-t" type="time" value="08:00" data-tc-t>')}${mtField("tc-op", s.opLabel, `<select id="tc-op" data-tc-op><option value="1">${esc(s.opAdd)}</option><option value="-1">${esc(s.opSub)}</option></select>`)}${mtNum("tc-h", s.hoursLabel, 'min="0" max="9999" value="2" data-tc-h')}${mtNum("tc-m", s.minutesLabel, 'min="0" max="59999" value="30" data-tc-m')}</div>
      <p class="mt-result" data-tc-r2 role="status" aria-live="polite">&nbsp;</p>
    </div></section>
    <section class="section" aria-labelledby="tc-c"><div class="wrap">
      <h2 id="tc-c">${esc(s.sumHeading)}</h2>
      <div class="field"><label for="tc-sum">${esc(s.sumLabel)}</label><textarea id="tc-sum" rows="5" dir="ltr" data-tc-sum>1:30
45
2:15:30</textarea></div>
      <p class="meta">${esc(s.sumHint)}</p>
      <p class="mt-result" data-tc-r3 role="status" aria-live="polite">&nbsp;</p><p class="meta" data-tc-n3>&nbsp;</p>
    </div></section>`;
    return { hero, sections, client: { overnight: s.overnight, duration: s.duration, decimal: s.decimal, minutesTotal: s.minutesTotal, resultTime: s.resultTime, nextDay: s.nextDay, prevDay: s.prevDay, daysLater: s.daysLater, daysEarlier: s.daysEarlier, sumResult: s.sumResult, sumBad: s.sumBad } };
  }
  if (key === "daysUntil") {
    const names = t.calendar.events;
    const evs = HOL_YEARS.flatMap((y) => YEAR_DATA[y].events).filter((e) => DU_KEYS.includes(e.key) && e.date >= cfg.updated).slice(0, 30);
    const rows = evs.map((e) => `<tr data-du-row="${e.date}"><th scope="row">${esc(names[e.key])}</th><td>${esc(fmtDate(lang, e.date))}</td><td data-du-left>–</td></tr>`).join("");
    const hero = `<section class="mt" data-mt="days" aria-labelledby="du-h">
      <h2 id="du-h" class="sr-only">${esc(s.h1)}</h2>
      <div class="inputs-row">${mtField("du-d", s.dateLabel, `<input id="du-d" type="date" min="1900-01-01" max="2200-12-31" data-du-d>`)}</div>
      <p class="big-time mt-big" data-du-main role="status" aria-live="polite">&nbsp;</p>
      <ul class="mt-lines" data-du-lines hidden><li data-du-weeks></li><li data-du-months></li><li data-du-work></li><li data-du-dow></li></ul>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
    const sections = `<section class="section" aria-labelledby="du-u"><div class="wrap">
      <div class="table-wrap"><table class="events-table"><caption id="du-u">${esc(s.upcomingHeading)}</caption><thead><tr><th scope="col">${esc(s.colEvent)}</th><th scope="col">${esc(s.colDate)}</th><th scope="col">${esc(s.colLeft)}</th></tr></thead><tbody>${rows}</tbody></table></div>
    </div></section>`;
    return { hero, sections, client: { left: s.left, left1: s.left1, passed: s.passed, passed1: s.passed1, today: s.today, weeks: s.weeks, months: s.months, workdays: s.workdays, weekday: s.weekday, daysUnit: s.daysUnit, daysUnit1: s.daysUnit1 } };
  }
  if (key === "interval") {
    const hero = `<section class="tool mt mt-iv" data-mt="interval" aria-labelledby="iv-h">
      <h2 id="iv-h" class="sr-only">${esc(s.h1)}</h2>
      <div class="mt-tools-row">${mtFsBtn(t)}</div>
      <p class="phase" data-iv-phase aria-live="polite">${esc(s.phases.warmup)}</p>
      <p class="big-time" role="timer" data-iv-display>00:10</p>
      <progress class="tool-progress" max="100" value="0" data-iv-progress aria-hidden="true"></progress>
      <p class="meta" data-iv-round>&nbsp;</p>
      <div class="tool-actions"><button type="button" class="btn" data-iv-start>${esc(s.start)}</button><button type="button" class="toggle" data-iv-reset>${esc(s.reset)}</button></div>
      <p class="meta" data-iv-total>&nbsp;</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
    const sections = `<section class="section"><div class="wrap">
      <div class="inputs-row">${mtNum("iv-w", s.warmup, 'min="0" max="3600" value="10" data-iv-cfg="warm"')}${mtNum("iv-k", s.work, 'min="1" max="3600" value="30" data-iv-cfg="work"')}${mtNum("iv-r", s.rest, 'min="0" max="3600" value="15" data-iv-cfg="rest"')}${mtNum("iv-n", s.rounds, 'min="1" max="99" value="8" data-iv-cfg="rounds"')}</div>
      <div class="presets" role="group" aria-label="${esc(s.presets)}"><button type="button" class="chip" data-iv-preset="10,20,10,8">${esc(s.presetTabata)}</button><button type="button" class="chip" data-iv-preset="10,30,15,10">${esc(s.presetHiit)}</button><button type="button" class="chip" data-iv-preset="10,60,0,10">${esc(s.presetEmom)}</button></div>
      <div class="tool-extra"><label class="check-inline"><input type="checkbox" data-iv-voice checked> ${esc(s.voice)}</label></div>
      <p class="meta">${esc(s.voiceNote)}</p>
      <p class="meta">${esc(s.note)}</p>
    </div></section>`;
    return { hero, sections, client: { phases: s.phases, lastRound: s.lastRound, roundOf: s.roundOf, total: s.total, start: s.start, pause: s.pause, resume: s.resume } };
  }
  if (key === "chess") {
    const presets = [[1, 0], [3, 0], [3, 2], [5, 0], [5, 3], [10, 0], [15, 10], [30, 0]];
    const side = (c, label) => `<button type="button" class="chess-side chess-${c}" data-ch-side="${c}" aria-label="${esc(label)}"><span class="chess-name">${esc(label)}</span><span class="chess-time" data-ch-time>05:00</span><span class="chess-moves" data-ch-moves>&nbsp;</span></button>`;
    const hero = `<section class="tool mt mt-chess" data-mt="chess" aria-labelledby="ch-h">
      <h2 id="ch-h" class="sr-only">${esc(s.h1)}</h2>
      <div class="chess-board">${side("black", s.black)}${side("white", s.white)}</div>
      <p class="cd-message" role="status" aria-live="assertive" data-ch-msg hidden></p>
      <div class="tool-actions"><button type="button" class="toggle" data-ch-pause>${esc(s.pause)}</button><button type="button" class="toggle" data-ch-reset>${esc(s.reset)}</button>${mtFsBtn(t)}</div>
      <p class="meta">${esc(s.hint)}</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
    const sections = `<section class="section"><div class="wrap">
      <div class="inputs-row">${mtField("ch-c", s.control, `<select id="ch-c" data-ch-control>${presets.map(([m, i]) => `<option value="${m},${i}"${m === 5 && i === 0 ? " selected" : ""}>${m} + ${i}</option>`).join("")}<option value="custom">${esc(s.custom)}</option></select>`)}${mtNum("ch-m", s.minutes, 'min="1" max="180" value="5" data-ch-min disabled')}${mtNum("ch-i", s.increment, 'min="0" max="60" value="0" data-ch-inc disabled')}</div>
    </div></section>`;
    return { hero, sections, client: { white: s.white, black: s.black, moves: s.moves, flag: s.flag, pause: s.pause, resume: s.resume } };
  }
  if (key === "metronome") {
    const hero = `<section class="tool mt mt-metro" data-mt="metronome" aria-labelledby="mm-h">
      <h2 id="mm-h" class="sr-only">${esc(s.h1)}</h2>
      <div class="metro-dots" data-mm-dots aria-hidden="true"></div>
      <p class="big-time" data-mm-bpm-show>100</p>
      <p class="meta" data-mm-tempo>Andante</p>
      <div class="metro-row"><button type="button" class="toggle" data-mm-step="-1" aria-label="${esc(s.slower)}">−</button><input type="range" min="30" max="250" value="100" aria-label="${esc(s.bpm)}" data-mm-range><button type="button" class="toggle" data-mm-step="1" aria-label="${esc(s.faster)}">+</button></div>
      <div class="tool-actions"><button type="button" class="btn" data-mm-start>${esc(s.start)}</button><button type="button" class="toggle" data-mm-tap>${esc(s.tap)}</button>${mtFsBtn(t)}</div>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
    const sections = `<section class="section"><div class="wrap">
      <div class="inputs-row">${mtNum("mm-b", s.bpm, 'min="30" max="250" value="100" data-mm-bpm')}${mtField("mm-n", s.beats, `<select id="mm-n" data-mm-beats>${Array.from({ length: 12 }, (_, i) => `<option value="${i + 1}"${i === 3 ? " selected" : ""}>${i + 1}</option>`).join("")}</select>`)}${mtField("mm-s", s.sub, `<select id="mm-s" data-mm-sub>${["1", "2", "3", "4"].map((k) => `<option value="${k}">${esc(s.subs[k])}</option>`).join("")}</select>`)}${mtField("mm-v", s.volume, '<input id="mm-v" type="range" min="0" max="100" value="70" data-mm-vol>')}</div>
      <div class="tool-extra"><label class="check-inline"><input type="checkbox" data-mm-accent checked> ${esc(s.accent)}</label></div>
      <p class="meta">${esc(s.tempoNote)}</p>
    </div></section>`;
    return { hero, sections, client: { start: s.start, stop: s.stop } };
  }
  if (key === "talking") {
    const hero = `<section class="tool mt mt-talk" data-mt="talking" aria-labelledby="tk-h">
      <h2 id="tk-h" class="sr-only">${esc(s.h1)}</h2>
      <p class="clock" data-live="time" data-sec>--:--:--</p>
      <p class="mt-words" data-live="words">&nbsp;</p>
      <div class="tool-actions"><button type="button" class="btn" data-tk-say>${esc(s.say)}</button>${mtFsBtn(t)}</div>
      <p class="meta" data-tk-novoice hidden>${esc(s.noVoice)}</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
    const sections = `<section class="section"><div class="wrap">
      <div class="inputs-row">${mtField("tk-e", s.auto, `<select id="tk-e" data-tk-every>${["off", "1", "5", "15", "30", "60"].map((k) => `<option value="${k}">${esc(s.every[k])}</option>`).join("")}</select>`)}</div>
      <div class="tool-extra"><label class="check-inline"><input type="checkbox" data-tk-chime checked> ${esc(s.chime)}</label></div>
      <p class="meta">${esc(s.note)}</p>
    </div></section>`;
    return { hero, sections, client: {} };
  }
  // zodiac
  const ranges = ZODIAC_START.map((st, i) => {
    const nx = ZODIAC_START[(i + 1) % 12], endD = new Date(Date.UTC(2027, nx[0] - 1, nx[1] - 1));
    const f = (m, d) => tpl(t.calendar.shortFmt, { day: d, month: t.calendar.months[m - 1] });
    return `<tr><th scope="row"><span aria-hidden="true">${ZODIAC_SYM[i]}&#xFE0E;</span> ${esc(s.signs[i])}</th><td>${esc(f(st[0], st[1]))} – ${esc(f(endD.getUTCMonth() + 1, endD.getUTCDate()))}</td></tr>`;
  }).join("");
  const hero = `<section class="mt" data-mt="zodiac" aria-labelledby="zo-h">
      <h2 id="zo-h" class="sr-only">${esc(s.h1)}</h2>
      <div class="inputs-row">${mtField("zo-d", s.dateLabel, '<input id="zo-d" type="date" min="1900-01-01" max="2100-12-31" value="2000-01-01" data-zo-d>')}${mtField("zo-t", s.timeLabel, '<input id="zo-t" type="time" data-zo-t>')}</div>
      <dl class="facts zo-res"><div class="fact"><dt>${esc(s.sun)}</dt><dd data-zo-sun>–</dd></div><div class="fact"><dt>${esc(s.moon)}</dt><dd data-zo-moon>–</dd></div></dl>
      <p class="meta" data-zo-note>&nbsp;</p>
      <p class="meta">${esc(s.tzNote)}</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
  const sections = `<section class="section"><div class="wrap">
      <div class="table-wrap"><table class="events-table"><caption>${esc(s.tableHeading)}</caption><thead><tr><th scope="col">${esc(s.colSign)}</th><th scope="col">${esc(s.colDates)}</th></tr></thead><tbody>${ranges}</tbody></table></div>
      <p class="meta">${esc(s.fun)}</p>
    </div></section>`;
  return { hero, sections, client: { signs: s.signs, sym: ZODIAC_SYM, noTime: s.noTime, cusp: s.cusp } };
}
function buildMore(lang, key) {
  const t = I[lang], s = t[key];
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang][key] }];
  const b = moreBody(lang, key);
  const scripts = key === "zodiac" ? ["prayer-calc.js", "nature-calc.js", "more-tools.js"] : ["more-tools.js"];
  return simplePage(lang, key, { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero: b.hero, sections: b.sections }, faq: s.faq, faqGroup: { id: key, title: s.h1 }, scripts, clientExtra: { t: { mt: b.client } }, nav: "tools", searchK: s.search, extraLd: [webAppLd(lang, s.h1, ROUTES[lang][key], s.description)] });
}

/* Açık veri (statik API, 30 Ekim 2026, sahibin isteği): /api/v1/ altında derlemede üretilen JSON dosyaları (aşağıda,
   sitemap'ten önce) ve bu belge sayfası. Sunucu yok; nginx /api/ konumu CORS * verir. */
const API_BASE = "/api/v1";
const API_PRAYER_YEARS = [Number(cfg.updated.slice(0, 4)), Number(cfg.updated.slice(0, 4)) + 1];
function buildApi(lang) {
  const t = I[lang], s = t.api;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].api }];
  const y0 = HOL_YEARS[0], y1 = HOL_YEARS[HOL_YEARS.length - 1], yNext = API_PRAYER_YEARS[1];
  const rows = [
    ["index", `${API_BASE}/index.json`, `${API_BASE}/index.json`],
    ["holidays", `${API_BASE}/holidays/{year}.json`, `${API_BASE}/holidays/${yNext}.json`],
    ["prayer", `${API_BASE}/prayer-times/{province}/{year}.json`, `${API_BASE}/prayer-times/istanbul/${API_PRAYER_YEARS[0]}.json`],
    ["provinces", `${API_BASE}/provinces.json`, `${API_BASE}/provinces.json`],
    ["hijri", `${API_BASE}/hijri.json`, `${API_BASE}/hijri.json`],
    ["astronomy", `${API_BASE}/astronomy/{year}.json`, `${API_BASE}/astronomy/${yNext}.json`],
  ].map(([k, pattern, example]) => `<tr><td dir="ltr"><a href="${example}"><code>${esc(pattern)}</code></a></td><td>${esc(s.ep[k])}</td></tr>`).join("");
  const code = `const r = await fetch("${abs(`${API_BASE}/prayer-times/istanbul/${API_PRAYER_YEARS[0]}.json`)}");
const { fields, days } = await r.json();
const today = new Date().toLocaleDateString("sv-SE", { timeZone: "Europe/Istanbul" });
console.log(fields, days.find((d) => d[0] === today));`;
  const sections = `<section class="section" aria-labelledby="api-ep-h"><div class="wrap">
    <h2 id="api-ep-h">${esc(s.endpointsHeading)}</h2>
    <div class="table-wrap"><table class="api-table"><thead><tr><th scope="col">${esc(s.colPath)}</th><th scope="col">${esc(s.colDesc)}</th></tr></thead><tbody>${rows}</tbody></table></div>
    <p class="meta">${esc(tpl(s.yearsNote, { from: y0, to: y1, p1: API_PRAYER_YEARS[0], p2: API_PRAYER_YEARS[1] }))}</p>
  </div></section>
  <section class="section" aria-labelledby="api-f-h"><div class="wrap">
    <h2 id="api-f-h">${esc(s.fieldsHeading)}</h2>
    <ul>${s.fields.map((x) => `<li>${esc(x)}</li>`).join("")}</ul>
    <h2>${esc(s.exampleHeading)}</h2>
    <p>${esc(s.exampleNote)}</p>
    <pre class="api-code" dir="ltr"><code>${esc(code)}</code></pre>
  </div></section>
  <section class="section" aria-labelledby="api-t-h"><div class="wrap">
    <h2 id="api-t-h">${esc(s.termsHeading)}</h2>
    <ul>${s.terms.map((x) => `<li>${esc(x)}</li>`).join("")}</ul>
  </div></section>`;
  return simplePage(lang, "api", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { sections }, nav: "tools", searchK: s.search, extraLd: [{ "@type": "Dataset", name: s.h1, description: s.description, url: abs(ROUTES[lang].api), license: "https://creativecommons.org/licenses/by/4.0/", isAccessibleForFree: true, creator: { "@type": "Organization", name: cfg.name, url: ORIGIN }, distribution: [{ "@type": "DataDownload", encodingFormat: "application/json", contentUrl: abs(`${API_BASE}/index.json`) }] }] });
}

function buildQibla(lang) {
  const t = I[lang];
  const s = t.qibla;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].qibla }];
  const hero = `<section class="qb" data-qibla aria-labelledby="qb-h">
      <h2 id="qb-h" class="qb-place" data-qb-place>&nbsp;</h2>
      <div class="qb-dial">${qiblaDial(lang)}</div>
      <p class="qb-status" data-qb-status role="status" aria-live="polite">&nbsp;</p>
      <button type="button" class="btn" data-qb-start hidden>${esc(s.start)}</button>
      <p class="meta" data-qb-calib hidden>${esc(s.calibrate)}</p>
      <dl class="qb-facts">
        <div class="fact"><dt>${esc(s.angle)}</dt><dd data-qb-angle>–</dd><dd class="meta">${esc(s.angleNote)}</dd></div>
        <div class="fact"><dt>${esc(s.distance)}</dt><dd data-qb-dist>–</dd></div>
        <div class="fact"><dt>${esc(s.time)}</dt><dd data-qb-time>–</dd><dd class="meta" data-qb-time-note>&nbsp;</dd></div>
      </dl>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
  const sections = `<section class="section" aria-labelledby="qb-mq-h">
  <div class="wrap">
    <h2 id="qb-mq-h">${esc(s.mosquesHeading)}</h2>
    <p>${esc(s.mosquesText)}</p>
    <p><button type="button" class="btn" data-qb-mq-show>${esc(s.mosquesShow)}</button></p>
    <p class="meta consent-ph" data-qb-mq-state role="status" aria-live="polite"></p>
    <ul class="qb-mosques" data-qb-mosques hidden></ul>
    <p class="meta">${esc(s.mapsText)} <a data-qb-google href="https://www.google.com/maps" target="_blank" rel="noopener">Google Maps</a> · <a data-qb-apple href="https://maps.apple.com/" target="_blank" rel="noopener">Apple Maps</a> · <a data-qb-osm href="https://www.openstreetmap.org/" target="_blank" rel="noopener">OpenStreetMap</a></p>
    <p class="meta">${esc(s.mosquesAttribution)}</p>
    <p class="meta">${esc(s.method)}</p>
  </div>
</section>`;
  const qb = { timeNote: s.timeNote, noTime: s.noTime, aligned: s.aligned, turn: s.turn, waiting: s.waiting, noCompass: s.noCompass, denied: s.denied, atKaaba: s.atKaaba, mosquesLoading: s.mosquesLoading, mosquesNone: s.mosquesNone, mosquesError: s.mosquesError, mosqueUnnamed: s.mosqueUnnamed, mapsQuery: s.mapsQuery, km: s.km, m: s.m, blocked: t.consent.mqBlocked, allow: t.consent.allow };
  return simplePage(lang, "qibla", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero, sections }, faq: s.faq, faqGroup: { id: "qibla", title: s.h1 }, scripts: ["place.js", "prayer-calc.js", "qibla.js"], clientExtra: { t: { pl: placeClient(t.prayer), qb } }, nav: "prayer", searchK: s.search, needsPlace: true, extraLd: [webAppLd(lang, s.h1, ROUTES[lang].qibla, s.description)] });
}

/* Gece gökyüzü (30 Ekim 2026, rakip analizi listesi #87): bu gece görünen gezegenler (sky-calc.js, JPL yaklaşık
   öğeleri), gökyüzü çizimi (parlak yıldızlar J2000), Ay'ın uzaklığı, sıradaki perige ve apoje. Hesap tarayıcıda. */
function buildSky(lang) {
  const t = I[lang];
  const s = t.sky;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].sky }];
  const hero = `<section class="sky" data-sky aria-labelledby="sky-h">
      <h2 id="sky-h" class="qb-place" data-sky-place>&nbsp;</h2>
      <figure class="sky-chart"><svg data-sky-chart viewBox="-200 -200 400 400" role="img" aria-labelledby="sky-cap"></svg><figcaption id="sky-cap" class="meta" data-sky-cap>&nbsp;</figcaption></figure>
      <p class="sky-time"><label for="sky-t">${esc(s.timeLabel)}</label> <input id="sky-t" type="range" min="0" max="96" step="1" value="0" data-sky-time> <output data-sky-time-out dir="ltr">–</output> <button type="button" class="toggle" data-sky-now>${esc(s.now)}</button></p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
  const sections = `<section class="section" aria-labelledby="sky-pl-h">
  <div class="wrap">
    <h2 id="sky-pl-h">${esc(s.tableHeading)}</h2>
    <div class="table-wrap"><table class="events-table sky-table"><thead><tr>${s.cols.map((c) => `<th scope="col">${esc(c)}</th>`).join("")}</tr></thead><tbody data-sky-planets></tbody></table></div>
  </div>
</section>
<section class="section" aria-labelledby="sky-moon-h">
  <div class="wrap">
    <h2 id="sky-moon-h">${esc(s.moonHeading)}</h2>
    <dl class="facts"><div class="fact"><dt>${esc(s.moonNow)}</dt><dd data-sky-mdist>–</dd></div><div class="fact"><dt>${esc(s.perigee)}</dt><dd data-sky-perigee>–</dd></div><div class="fact"><dt>${esc(s.apogee)}</dt><dd data-sky-apogee>–</dd></div></dl>
    <p class="meta" data-sky-super hidden></p>
    <p class="meta">${esc(s.note)}</p>
  </div>
</section>`;
  const sk = { planets: s.planets, moon: s.moon, sun: s.sun, chartCaption: s.chartCaption, window: s.window, tooClose: s.tooClose, below: s.below, riseSet: s.riseSet, altFmt: s.altFmt, mkm: s.mkm, km: s.km, superMoon: s.superMoon, compass: t.weather.compass, cardinals: t.qibla.cardinals };
  return simplePage(lang, "sky", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero, sections }, faq: s.faq, faqGroup: { id: "sky", title: s.h1 }, scripts: ["place.js", "nature-calc.js", "sky-calc.js", "sky.js"], clientExtra: { t: { pl: placeClient(t.prayer), sk } }, nav: "tools", searchK: s.search, needsPlace: true, extraLd: [webAppLd(lang, s.h1, ROUTES[lang].sky, s.description)] });
}

/* Tarihte bugün (30 Ekim 2026, rakip analizi listesi #89): Wikidata (CC0) verisi, scripts/fetch-on-this-day.mjs ile elle
   yenilenir (derleme ağa çıkmaz). Her dil için ay başına bir dosya: { "MM-DD": [[yıl, tür, ad, açıklama, Qkimlik]] };
   tür e olay, f kuruluş, b doğum, d ölüm. Adı o dilde olmayan kayıt o dilde gösterilmez (Filipincede İngilizce ad). Sayfa derleme gününün listesini
   HTML olarak taşır; otd.js bugünün (ya da ?d=AA-GG) listesini yükler. */
const OTD_SRC = fs.existsSync(path.join(SRC, "data", "on-this-day.json")) ? readJson("src/data/on-this-day.json") : { days: {} };
const OTD_LIMIT = { e: 8, f: 2, b: 5, d: 5 };
const OTD_URL = {};
/* Filipincede Wikidata etiketleri azdır (kayıtların ~%30'u); Filipinler'de yaygın olan İngilizce ad ve açıklama kullanılır */
const OTD_FALLBACK = { fil: "en" };
function otdDay(lang, md) {
  const out = { e: [], f: [], b: [], d: [] }, seen = new Set(), fb = OTD_FALLBACK[lang];
  for (const it of OTD_SRC.days[md] || []) {
    const own = it.l && it.l[lang], name = own || (fb && it.l && it.l[fb]);
    // Aynı öğe (ör. hem olay hem kuruluş olarak kayıtlı bir anlaşma) bir kez
    if (!name || out[it.k].length >= OTD_LIMIT[it.k] || seen.has(it.q)) continue;
    seen.add(it.q);
    out[it.k].push([it.y, it.k, name, (it.s && (own ? it.s[lang] : it.s[fb])) || "", it.q]);
  }
  return [...out.e.sort((a, b) => a[0] - b[0]), ...out.f.sort((a, b) => a[0] - b[0]), ...out.b.sort((a, b) => a[0] - b[0]), ...out.d.sort((a, b) => a[0] - b[0])];
}
for (const lang of LANGS) {
  OTD_URL[lang] = {};
  for (let m = 1; m <= 12; m++) {
    const mm = String(m).padStart(2, "0"), days = {};
    for (let d = 1; d <= 31; d++) { const md = `${mm}-${String(d).padStart(2, "0")}`; if (OTD_SRC.days[md]) days[md] = otdDay(lang, md); }
    const j = JSON.stringify(days);
    write(`assets/otd/${lang}-${mm}.json`, j);
    OTD_URL[lang][mm] = `/assets/otd/${lang}-${mm}.json?v=${hash(j)}`;
  }
}
function otdList(lang, md) {
  const s = I[lang].otd, rows = otdDay(lang, md);
  const groups = [["e", s.events], ["f", s.founded], ["b", s.births], ["d", s.deaths]];
  return groups.map(([k, h]) => {
    const items = rows.filter((r) => r[1] === k);
    if (!items.length) return "";
    return `<h3>${esc(h)}</h3><ul class="otd-list">${items.map((r) => `<li><span class="otd-y" dir="ltr">${r[0]}</span> <span>${esc(k === "f" ? tpl(s.foundedFmt, { name: r[2] }) : r[2])}${r[3] ? ` <span class="meta">· ${esc(r[3])}</span>` : ""}</span></li>`).join("")}</ul>`;
  }).join("") || `<p class="meta">${esc(s.empty)}</p>`;
}
function buildOtd(lang) {
  const t = I[lang];
  const s = t.otd;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].otd }];
  const [y, m, d] = cfg.updated.split("-").map(Number);
  const md = `${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
  const cal = t.calendar;
  const dateTxt = tpl(cal.shortFmt || "{day} {month}", { day: d, month: cal.months[m - 1] });
  const hero = `<section class="otd" data-otd data-md="${md}" aria-labelledby="otd-h">
      <p class="otd-nav"><button type="button" class="toggle" data-otd-prev>${esc(s.prev)}</button> <label for="otd-day" class="sr-only">${esc(s.dayLabel)}</label><input id="otd-day" type="date" data-otd-day value="${cfg.updated}"> <button type="button" class="toggle" data-otd-next>${esc(s.next)}</button> <button type="button" class="toggle" data-otd-today>${esc(s.today)}</button></p>
      <h2 id="otd-h" data-otd-title>${esc(dateTxt)}</h2>
      <div data-otd-list>${otdList(lang, md)}</div>
      <p class="meta">${esc(s.source)}</p>
    </section>`;
  const ot = { events: s.events, founded: s.founded, births: s.births, deaths: s.deaths, foundedFmt: s.foundedFmt, empty: s.empty, urls: OTD_URL[lang] };
  return simplePage(lang, "otd", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero, sections: "" }, faq: s.faq, faqGroup: { id: "otd", title: s.h1 }, scripts: ["otd.js"], clientExtra: { t: { ot } }, nav: "tools", searchK: s.search });
}

/* Emeklilik hesaplama (30 Ekim 2026, rakip analizi listesi #86): yalnız Türkçe, yalnız 4/a (SSK). Kurallar retire.js'te,
   kanun metinlerinden (5510 m.28, geçici 9 ve 95; 506 geçici 81). Metinler src/data/retire-tr.json. */
const RETIRE = readJson("src/data/retire-tr.json");
function buildRetire() {
  const lang = "tr", t = I.tr, s = RETIRE;
  const trail = [{ name: t.ui.home, path: ROUTES.tr.home }, { name: s.h1, path: ROUTES.tr.retire }];
  const hero = `<section class="rt" data-retire aria-labelledby="rt-h">
      <h2 id="rt-h" class="sr-only">${esc(s.h1)}</h2>
      <form class="rt-form" data-rt-form novalidate>
        <fieldset class="rt-gender"><legend>${esc(s.gender)}</legend><label><input type="radio" name="g" value="f" checked> ${esc(s.women)}</label> <label><input type="radio" name="g" value="m"> ${esc(s.men)}</label></fieldset>
        <div class="field"><label for="rt-birth">${esc(s.birth)}</label><input id="rt-birth" type="date" data-rt-birth required min="1930-01-01" max="2015-12-31"></div>
        <div class="field"><label for="rt-start">${esc(s.start)}</label><input id="rt-start" type="date" data-rt-start required min="1950-01-01"></div>
        <div class="field"><label for="rt-days">${esc(s.days)}</label><input id="rt-days" type="number" inputmode="numeric" min="0" max="20000" step="1" data-rt-days required><p class="meta">${esc(s.daysHint)}</p></div>
        <div class="field"><label for="rt-rate">${esc(s.rate)}</label><select id="rt-rate" data-rt-rate><option value="360">${esc(s.rateFull)}</option><option value="180">${esc(s.rateHalf)}</option><option value="0">${esc(s.rateNone)}</option></select></div>
        <button type="submit" class="btn">${esc(s.calc)}</button>
      </form>
      <div class="rt-out" data-rt-out role="status" aria-live="polite" hidden>
        <p class="rt-date" data-rt-date></p>
        <p data-rt-age></p>
        <p class="meta" data-rt-group></p>
        <div class="table-wrap"><table class="events-table"><thead><tr>${s.cols.map((c) => `<th scope="col">${esc(c)}</th>`).join("")}</tr></thead><tbody data-rt-rows></tbody></table></div>
      </div>
      <p class="meta">${esc(s.noteScope)}</p>
      <p class="meta">${esc(s.noteLaw)}</p>
      <p class="meta">${esc(s.noteData)}</p>
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>`;
  const keys = ["result", "resultAge", "resultLeft", "resultPast", "resultNever", "group", "groups", "routes", "condDays", "condYears", "condAge", "condNoAge", "done", "never", "years", "months"];
  const rt = Object.fromEntries(keys.map((k) => [k, s[k]]));
  const page = simplePage(lang, "retire", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: { hero, sections: "" }, faq: s.faq, faqGroup: { id: "retire", title: s.h1 }, scripts: ["retire.js"], clientExtra: { t: { rt } }, nav: "tools", searchK: s.search, extraLd: [webAppLd(lang, s.h1, ROUTES.tr.retire, s.description)] });
  page.langFallback = Object.fromEntries(LANGS.map((l) => [l, ROUTES[l].tools]));
  return page;
}

function buildPrayerHub(lang) {
  const t = I[lang];
  const s = t.prayer;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].prayer }];
  return simplePage(lang, "prayer", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: prayerBody(lang, null), faq: s.faq, faqGroup: { id: "prayer", title: t.ui.prayer, href: ROUTES[lang].prayer }, scripts: ["place.js", "prayer-calc.js", "prayer.js", "imsakiye.js"], clientExtra: { t: prayerClient(s) }, nav: "prayer", searchK: s.search, needsPlace: true, extraLd: [webAppLd(lang, s.h1, ROUTES[lang].prayer, s.description)] });
}
function buildPrayerCity(lang, prov) {
  const t = I[lang];
  const s = t.prayer;
  const name = provName(lang, prov);
  const v = { city: name };
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].prayer }, { name: tpl(s.cityH1, v), path: prayerPath(lang, prov) }];
  return simplePage(lang, `prayer:${prov.slug}`, { title: tpl(s.cityTitle, v), description: tpl(s.cityDescription, v), h1: tpl(s.cityH1, v), lead: tpl(s.cityLead, v), trail, bodyInner: prayerBody(lang, prov, lang === "tr" ? districtList(prov, null) : ""), faq: s.faq, faqGroup: { id: "prayer", title: t.ui.prayer, href: ROUTES[lang].prayer }, scripts: ["place.js", "prayer-calc.js", "prayer.js", "imsakiye.js"], clientExtra: { t: prayerClient(s) }, nav: "prayer", searchK: `${name} ${s.search}` });
}

function buildPrayerDistrict(d) {
  const lang = "tr";
  const t = I.tr;
  const s = t.prayer;
  const prov = provName(lang, d.prov);
  const v = { city: `${d.name} (${prov})` };
  const path = districtPath(d);
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: s.h1, path: ROUTES[lang].prayer }, { name: tpl(s.cityH1, { city: prov }), path: prayerPath(lang, d.prov) }, { name: tpl(s.cityH1, v), path }];
  const place = { slug: `${d.prov.slug}/${d.slug}`, display: `${d.name}, ${prov}`, lat: d.lat, lon: d.lon };
  const page = simplePage(lang, `district:${d.prov.slug}/${d.slug}`, { title: tpl(s.districtTitle, v), description: tpl(s.cityDescription, v), h1: tpl(s.cityH1, v), lead: tpl(s.cityLead, v), trail, bodyInner: prayerBody(lang, place, districtList(d.prov, d.slug)), scripts: ["place.js", "prayer-calc.js", "prayer.js", "imsakiye.js"], clientExtra: { t: prayerClient(s) }, nav: "prayer", search: false });
  // Arama dizininde kısa satır (dizin büyümesin)
  searchEntries.tr.push({ t: tpl(s.cityH1, { city: d.name }), u: path, d: prov, k: `${d.name} ${prov} ilçe namaz vakti ezan imsak` });
  page.langFallback = Object.fromEntries(LANGS.map((l) => [l, prayerPath(l, d.prov)]));
  return page;
}

/* Gömülü widget sayfası: yalnız seçilen saat modeli, başlık ve alt bilgi (indekslenmez) */
function embedFramePage(lang) {
  const t = I[lang];
  const slides = EMBED_MODELS.map((m) => `<section class="slide ${m.cls}" data-model="${m.id}" aria-hidden="true">${m.html("")}</section>`).join("");
  const pageCfg = { locale: t.locale, t: { ...t.client }, cities: clientCities(lang), cal: { months: t.calendar.months, monthsTitle: t.calendar.monthsTitle, weekdays: t.calendar.weekdays, weekdaysShort: t.calendar.weekdaysShort, fullFmt: t.calendar.fullFmt, shortFmt: t.calendar.shortFmt, dateFmt: t.calendar.dateFmt } };
  return `<!doctype html>
<html lang="${lang}" dir="${DIR(lang)}" data-embed="1">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(t.embedgen.frameTitle)}</title>
<meta name="robots" content="noindex,nofollow">
<meta name="color-scheme" content="light dark">
<meta http-equiv="Content-Security-Policy" content="${esc(CSP)}">
<meta name="referrer" content="strict-origin-when-cross-origin">
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="stylesheet" href="${ASSETS["styles.css"]}">
<script src="${ASSETS["theme.js"]}"></script>
</head>
<body>
<main class="embed-stage">${slides}</main>
<footer class="embed-foot"><a href="${abs(ROUTES[lang].home)}" target="_blank" rel="noopener">${esc(t.embed.link)}</a>${badge(lang)}</footer>
<script type="application/json" id="page-config">${JSON.stringify(pageCfg).replace(/</g, "\\u003c")}</script>
<script defer src="${ASSETS["embed.js"]}"></script>
<script defer src="${ASSETS["main.js"]}"></script>
</body>
</html>
`;
}

function buildFaq(lang) {
  const t = I[lang];
  const f = t.faqPage;
  const pagePath = ROUTES[lang].faq;
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: f.h1, path: pagePath },
  ];
  const groups = [...(FAQ_GROUPS[lang] || new Map()).values()].sort((a, b) => FAQ_ORDER.indexOf(a.id) - FAQ_ORDER.indexOf(b.id));
  const toc = groups.map((g) => `<li><a href="#${g.id}">${esc(g.title)}</a></li>`).join("");
  const sections = groups.map((g) => `<section class="section faq-group" id="${g.id}" aria-labelledby="${g.id}-h">
  <div class="wrap">
    <h2 id="${g.id}-h">${esc(g.title)}</h2>
    ${faqHtml(g.items, `${g.id}-h`)}
  </div>
</section>`).join("\n");
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(f.h1)}</h1>
    <p class="lead">${esc(f.lead)}</p>
    <nav class="faq-toc" aria-label="${esc(f.toc)}"><ul class="inline-links">${toc}</ul></nav>
  </div>
</section>
${sections}
<section class="section faq-privacy">
  <div class="wrap"><p><a href="${ROUTES[lang].privacy}">${esc(t.ui.privacy)}</a></p></div>
</section>`;
  const all = groups.flatMap((g) => g.items);
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, f.title, f.description), breadcrumbLd(trail), faqLd(all)];
  searchEntries[lang].push({ t: f.h1, u: pagePath, d: f.description, k: "sss sıkça sorulan sorular faq frequently asked questions soru question" });
  return { lang, key: "faq", path: pagePath, title: f.title, description: f.description, body, ld, nav: "faq" };
}

const built = [];
for (const lang of LANGS) {
  built.push(buildHome(lang), buildWorld(lang), buildToolsHub(lang), ...TOOL_KEYS.map((k) => buildTool(lang, k)), ...TIMER_PRESETS.map((m) => buildTimerPreset(lang, m)), ...ALARM_PRESETS.map((t) => buildAlarmPreset(lang, t)), buildWeek(lang), buildTz(lang), buildConverter(lang), buildPlanner(lang), buildPoll(lang), buildDateCalc(lang), buildEmbedGen(lang), buildPrayerHub(lang), ...PROVINCES.map((pr) => buildPrayerCity(lang, pr)), buildQibla(lang), buildZakat(lang), buildSky(lang), buildOtd(lang), ...(lang === "tr" ? [buildRetire()] : []), ...MORE_KEYS.map((k) => buildMore(lang, k)), buildApi(lang), buildBridge(lang), buildZonesHub(lang), ...TZ_ABBRS.map((z) => buildZone(lang, z)), buildDst(lang), buildTzNews(lang), buildUnix(lang), buildWorldHolHub(lang), ...WCC.map((cc) => buildWorldHol(lang, cc)), buildNature(lang), buildWeather(lang), buildPairHub(lang), ...PAIR_CITIES.map((c) => buildPair(lang, c)), ...CDP.map((x) => buildCdp(lang, x)), buildPrivacy(lang), ...CITIES.map((c) => buildCity(lang, c)));
  built.push(...HOL_YEARS.map((y) => buildCalendarYear(lang, y)), buildCalendarYear(lang, BUILD_YEAR, { index: true }));
  if (lang === "tr") built.push(...PROVINCES.flatMap((pr) => districtsOf(pr).map(buildPrayerDistrict)));
  built.push(buildFaq(lang)); // en sonda: diğer sayfaların SSS'leri toplanmış olur
}

// Dil karşılıkları: aynı "key" ile eşleşir.
const byKey = {};
for (const p of built) (byKey[p.key] ||= {})[p.lang] = p.path;

/* ---------- arama dizini ---------- */
const searchVersion = {};
for (const lang of LANGS) {
  const json = JSON.stringify(searchEntries[lang]);
  write(`assets/search-${lang}.json`, json);
  searchVersion[lang] = hash(json);
}

/* ---------- sayfaları yaz ---------- */
for (const p of built) {
  const html = layout({ ...p, alt: byKey[p.key], searchVersion: searchVersion[p.lang] });
  write(path.join(p.path, "index.html"), html);
}

for (const lang of LANGS) write(path.join(ROUTES[lang].embedFrame, "index.html"), embedFramePage(lang));

/* ---------- hata sayfaları (iki dil bir arada) ---------- */
function errorPage(kind) {
  const block = (lang, s) => `<section class="center-page" lang="${lang}" dir="${DIR(lang)}">
  <div class="wrap">
    <h1>${esc(s.h1)}</h1>
    <p class="lead">${esc(s.p)}</p>
    <p><a class="btn" href="${ROUTES[lang].home}">${esc(s.cta)}</a></p>
  </div>
</section>`;
  return layout({
    lang: "tr",
    title: `${I.tr[kind].title.split(" | ")[0]} / ${I.en[kind].title}`,
    description: I.tr[kind].h1,
    body: LANGS.map((l) => block(l, I[l][kind])).join(""),
    noindex: true,
    error: true,
    path: "",
    searchVersion: searchVersion.tr,
  });
}
write("404.html", errorPage("notFound"));
write("500.html", errorPage("serverError"));

/* ---------- Açık veri (statik API, /api/v1/): JSON dosyaları ---------- */
{
  const PRAYER = (() => { const m = { exports: {} }; new Function("module", "window", fs.readFileSync(path.join(SRC, "js/prayer-calc.js"), "utf8"))(m, undefined); return m.exports; })();
  const PKEYS = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"];
  const hm = (h) => { const min = ((Math.round(h * 60) % 1440) + 1440) % 1440; return `${String(Math.floor(min / 60)).padStart(2, "0")}:${String(min % 60).padStart(2, "0")}`; };
  const json = (rel, obj) => write(`${API_BASE.slice(1)}/${rel}`, JSON.stringify(obj) + "\n");
  const meta = { source: cfg.name, url: abs(ROUTES.en.api), license: "CC BY 4.0", attribution: `${cfg.name} (time.bumba.tr)`, updated: cfg.updated };
  const isoUtc = (ms) => new Date(ms).toISOString().replace(/\.\d{3}Z$/, "Z");
  // Tatiller: resmî tatil, yarım gün arife, dini ve özel günler (sınav ve okul hariç); adlar beş dilde
  for (const y of HOL_YEARS) {
    const events = YEAR_DATA[y].events.filter((e) => e.date.startsWith(String(y)) && ["holiday", "half", "religious", "special"].includes(e.type))
      .map((e) => ({ date: e.date, key: e.key, type: e.type, name: Object.fromEntries(LANGS.map((l) => [l, I[l].calendar.events[e.key]])) }));
    const ramadan = HOL.ramadans?.find((r) => r.from.startsWith(String(y)));
    json(`holidays/${y}.json`, { ...meta, year: y, country: "TR", projected: !!YEAR_DATA[y].projected, ramadan: ramadan ? { from: ramadan.from, to: ramadan.to || null } : null, events });
  }
  // İller ve namaz vakitleri (Türkiye saati, UTC+3; sitedeki prayer.js ile aynı yuvarlama)
  json("provinces.json", { ...meta, provinces: PROVINCES.map((p) => ({ slug: p.slug, name: p.name, lat: p.lat, lon: p.lon })) });
  for (const p of PROVINCES) for (const y of API_PRAYER_YEARS) {
    const days = [];
    for (let d = new Date(Date.UTC(y, 0, 1)); d.getUTCFullYear() === y; d.setUTCDate(d.getUTCDate() + 1)) {
      const r = PRAYER.times(y, d.getUTCMonth() + 1, d.getUTCDate(), p.lat, p.lon, 3);
      days.push([d.toISOString().slice(0, 10), ...PKEYS.map((k) => (isFinite(r[k]) ? hm(r[k]) : null))]);
    }
    json(`prayer-times/${p.slug}/${y}.json`, { ...meta, province: p.slug, name: p.name, lat: p.lat, lon: p.lon, timezone: "Europe/Istanbul", year: y, method: "Diyanet (fajr 18°, isha 17°, asr shadow 1)", fields: ["date", ...PKEYS], days });
  }
  // Hicri ay başları (uygulamalardakiyle aynı tablo: Diyanet + Umm al-Qura)
  const hij = hijriTable(HOL, readJson("src/data/hijri.json"));
  json("hijri.json", { ...meta, calendar: "Diyanet-aligned Hijri months (gaps filled with Umm al-Qura)", fields: ["gregorianStart", "hijriYear", "hijriMonth"], months: hij.slice(0, -1), end: hij[hij.length - 1][0] });
  // Astronomi: ekinoks/gündönümü, yeni ay ve dolunaylar, tutulmalar (UTC)
  for (const y of HOL_YEARS) {
    const q = NATURE.seasons(y), from = Date.UTC(y, 0, 1), to = Date.UTC(y + 1, 0, 1);
    const k0 = Math.floor(((from / 86400000 + 2440587.5 - 2451550.09766) / 29.530588861)) - 1;
    const phases = [];
    for (let k = k0; k < k0 + 15; k++) for (const full of [false, true]) { const ms = NATURE.phaseMs(k, full); if (ms >= from && ms < to) phases.push({ phase: full ? "full" : "new", at: isoUtc(ms) }); }
    phases.sort((a, b) => a.at.localeCompare(b.at));
    const eclipses = ECLIPSES.filter((e) => e.ut >= from && e.ut < to).map((e) => ({ kind: e.kind, type: e.type, at: isoUtc(e.ut), regions: e.regions }));
    json(`astronomy/${y}.json`, { ...meta, year: y, seasons: { marchEquinox: isoUtc(q.march), juneSolstice: isoUtc(q.june), septemberEquinox: isoUtc(q.sept), decemberSolstice: isoUtc(q.dec) }, moonPhases: phases, eclipses });
  }
  json("index.json", { ...meta, version: "v1", docs: Object.fromEntries(LANGS.map((l) => [l, abs(ROUTES[l].api)])), endpoints: {
    holidays: { path: `${API_BASE}/holidays/{year}.json`, years: HOL_YEARS },
    prayerTimes: { path: `${API_BASE}/prayer-times/{province}/{year}.json`, years: API_PRAYER_YEARS, provinces: `${API_BASE}/provinces.json` },
    provinces: { path: `${API_BASE}/provinces.json` },
    hijri: { path: `${API_BASE}/hijri.json` },
    astronomy: { path: `${API_BASE}/astronomy/{year}.json`, years: HOL_YEARS },
  } });
}

/* Ay evreleri ve tutulmalar takvimi (30 Ekim 2026): /ics/<dil>-moon.ics, yeni ay, dolunay ve tutulmalar 2026–2031,
   UTC anıyla (bitişsiz zamanlı etkinlik: RFC 5545'e göre başlangıç anında biter). Mevsimler sayfasındaki bağlantılar. */
for (const lang of LANGS) {
  const cal = I[lang].calendar, n = I[lang].nature;
  const stamp = cfg.updated.replace(/-/g, "") + "T000000Z";
  const ut = (ms) => new Date(ms).toISOString().replace(/[-:]/g, "").replace(/\.\d{3}Z$/, "Z");
  const from = Date.UTC(HOL_YEARS[0], 0, 1), to = Date.UTC(HOL_YEARS[HOL_YEARS.length - 1] + 1, 0, 1);
  const ev = [];
  const k0 = Math.floor(((from / 86400000 + 2440587.5 - 2451550.09766) / 29.530588861)) - 1;
  for (let k = k0; ; k++) {
    const nw = NATURE.phaseMs(k, false), fl = NATURE.phaseMs(k, true);
    if (nw >= to) break;
    if (nw >= from) ev.push({ at: nw, kind: "new", title: n.phases.new });
    if (fl >= from && fl < to) ev.push({ at: fl, kind: "full", title: n.phases.full });
  }
  for (const e of ECLIPSES) if (e.ut >= from && e.ut < to) ev.push({ at: e.ut, kind: "ecl", title: n.eclipseTypes[e.kind + e.type[0].toUpperCase() + e.type.slice(1)] || e.type });
  ev.sort((a, b) => a.at - b.at);
  const lines = ["BEGIN:VCALENDAR", "VERSION:2.0", `PRODID:-//Bumba//Saat Tarih Moon//${lang.toUpperCase()}`, "CALSCALE:GREGORIAN", "METHOD:PUBLISH", `X-WR-CALNAME:${icsEsc(cal.icsCalMoon)}`, "REFRESH-INTERVAL;VALUE=DURATION:P30D", "X-PUBLISHED-TTL:P30D"];
  for (const e of ev) lines.push("BEGIN:VEVENT", `UID:${ut(e.at)}-${e.kind}-${lang}@time.bumba.tr`, `DTSTAMP:${stamp}`, `DTSTART:${ut(e.at)}`, `SUMMARY:${icsEsc((e.kind === "new" ? "🌑 " : e.kind === "full" ? "🌕 " : "") + e.title)}`, `URL:${abs(ROUTES[lang].nature)}`, "TRANSP:TRANSPARENT", "END:VEVENT");
  lines.push("END:VCALENDAR");
  write(ICS_URL(lang, "moon").slice(1), lines.map(icsFold).join("\r\n") + "\r\n");
}

/* ---------- sitemap, robots, llms.txt, manifest ---------- */
const indexable = built.filter((p) => !p.noindex);
const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
${indexable
  .map((p) => {
    const a = byKey[p.key];
    return `  <url>
    <loc>${abs(p.path)}</loc>
    <lastmod>${cfg.updated}</lastmod>
${LANGS.filter((l) => a[l]).map((l) => `    <xhtml:link rel="alternate" hreflang="${l}" href="${abs(a[l])}"/>`).join("\n")}
    <xhtml:link rel="alternate" hreflang="x-default" href="${abs(a.tr)}"/>
  </url>`;
  })
  .join("\n")}
</urlset>
`;
write("sitemap.xml", sitemap);

const AI_BOTS = [
  "GPTBot", "OAI-SearchBot", "ChatGPT-User", "ClaudeBot", "Claude-SearchBot", "Claude-User",
  "PerplexityBot", "Perplexity-User", "Google-Extended", "Applebot-Extended", "Googlebot", "Bingbot",
];
write(
  "robots.txt",
  `User-agent: *\nAllow: /\n\n${AI_BOTS.map((b) => `User-agent: ${b}\nAllow: /\n`).join("\n")}\nSitemap: ${abs("/sitemap.xml")}\n`
);

const llmsLine = (lang, p) => `- [${p.title.split(" | ")[0]}](${abs(p.path)}): ${p.description}`;
const llmsBlock = (lang) => {
  const t = I[lang];
  const mine = built.filter((p) => p.lang === lang && !p.noindex);
  const prayers = mine.filter((p) => p.key.startsWith("prayer:"));
  const main = mine.filter((p) => !p.key.startsWith("city:") && !p.key.startsWith("calendar:") && !p.key.startsWith("prayer:") && !p.key.startsWith("district:"));
  const years = mine.filter((p) => p.key.startsWith("calendar:"));
  const cities = mine.filter((p) => p.key.startsWith("city:"));
  return `> ${t.llms.summary}\n\n## ${t.llms.pages}\n${[...main, ...years].map((p) => llmsLine(lang, p)).join("\n")}\n\n## ${t.llms.cities}\n${cities.map((p) => llmsLine(lang, p)).join("\n")}\n\n## ${t.prayer.provincesHeading}\n${prayers.map((p) => llmsLine(lang, p)).join("\n")}\n\n## ${t.llms.about}\n${t.llms.aboutText}\n`;
};
write("llms.txt", `# ${cfg.name}\n\n${LANGS.map(llmsBlock).join("\n---\n\n")}`);

write(
  "site.webmanifest",
  JSON.stringify(
    {
      name: cfg.name,
      short_name: cfg.name,
      description: I.tr.site.description,
      lang: "tr",
      start_url: "/",
      scope: "/",
      display: "standalone",
      background_color: "#f7f4ee",
      theme_color: "#11141a",
      icons: [
        { src: "/icon-192.png", sizes: "192x192", type: "image/png" },
        { src: "/icon-512.png", sizes: "512x512", type: "image/png" },
      ],
    },
    null,
    2
  )
);

if (!UMAMI_ID) console.warn("UYARI: Umami kimliği boş; sayaç eklenmeyecek (site.config.json → umamiId ya da UMAMI_ID).");
console.log(`Derlendi: ${built.length} sayfa + 404/500 → dist/ (${ORIGIN})`);
