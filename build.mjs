// Saat Tarih statik site derleyicisi. Bağımlılık yok: `node build.mjs` → dist/
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";
import { computeYear, holidayCounts, makeFmt, dowOf, daysInMonth, iso, inRange, addDays, isoWeek1Monday, isoWeeksInYear } from "./lib/events.mjs";

const ROOT = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.join(ROOT, "src");
const OUT = path.join(ROOT, "dist");

const readJson = (p) => JSON.parse(fs.readFileSync(path.join(ROOT, p), "utf8"));
const cfg = readJson("site.config.json");
const ORIGIN = (process.env.SITE_ORIGIN || cfg.origin).replace(/\/+$/, "");
const HOST = new URL(ORIGIN).host;
const UMAMI_ID = process.env.UMAMI_ID ?? cfg.umamiId ?? "";
/* Diller: tr (varsayılan) ve en zorunlu; diğerleri src/i18n/<dil>.json varsa derlenir. */
const ALL_LANGS = ["tr", "en", "de", "az", "ar"];
const LANGS = ALL_LANGS.filter((l) => fs.existsSync(path.join(ROOT, "src/i18n", `${l}.json`)));
const I = Object.fromEntries(LANGS.map((l) => [l, readJson(`src/i18n/${l}.json`)]));
const CITIES = readJson("src/data/cities.json");
const LANG_NAMES = { tr: "Türkçe", en: "English", de: "Deutsch", az: "Azərbaycanca", ar: "العربية" };
/* Ortak liste servisinin bildiği diller (bumbagroup.com/api/liste/katil); servis yeni dil eklediğinde buraya yaz */
const LIST_LANGS = ["tr", "en"];
const DIR = (l) => (l === "ar" ? "rtl" : "ltr");
const ARROW = (l) => (l === "ar" ? "←" : "→");
/* Türkçe dışındaki dillerin adresleri İngilizce tablodan türetilir: /en/ → /<dil>/ */
const deriveRoutes = (o, l) =>
  Object.fromEntries(Object.entries(o).map(([k, v]) => [k, v.replace(/^\/en\//, `/${l}/`).replace(/^\/embed\/en\//, `/embed/${l}/`)]));
const extendRoutes = (table) => {
  for (const l of LANGS) Object.assign(ROUTES[l], table[l] || deriveRoutes(table.en, l));
};

const arm = cfg.arm;
const BADGE_W = { games: 185, suites: 177, life: 164, ventures: 205 }[arm];

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
  tr: { home: "/", world: "/dunya-saatleri/", privacy: "/gizlilik/", calendar: "/takvim/", tools: "/araclar/", week: "/hafta-numarasi/", today: "/bugun-ayin-kaci/", tz: "/turkiye-saat-dilimi/", stopwatch: "/kronometre/", countdown: "/geri-sayim/", alarm: "/alarm/", pomodoro: "/pomodoro/" },
  en: { home: "/en/", world: "/en/world-clock/", privacy: "/en/privacy/", calendar: "/en/calendar/", tools: "/en/tools/", week: "/en/week-number/", today: "/en/todays-date/", tz: "/en/turkey-time-zone/", stopwatch: "/en/stopwatch/", countdown: "/en/countdown/", alarm: "/en/alarm/", pomodoro: "/en/pomodoro/" },
};
for (const l of LANGS) if (!ROUTES[l]) ROUTES[l] = deriveRoutes(ROUTES.en, l);
const cityPath = (lang, c) => `${ROUTES[lang].world}${c[lang].slug}/`;

/* ---------- varlıklar (sürüm damgalı) ---------- */
fs.rmSync(OUT, { recursive: true, force: true });
const ASSETS = {};
for (const [name, from] of [
  ["styles.css", "styles.css"],
  ["theme.js", "js/theme.js"],
  ["main.js", "js/main.js"],
  ["tools.js", "js/tools.js"],
  ["calendar.js", "js/calendar.js"],
  ["dates.js", "js/dates.js"],
  ["planner.js", "js/planner.js"],
  ["embed.js", "js/embed.js"],
]) {
  const buf = fs.readFileSync(path.join(SRC, from));
  write(`assets/${name}`, buf);
  ASSETS[name] = `/assets/${name}?v=${hash(buf)}`;
}
for (const f of ["favicon.svg", "favicon.ico", "icon-192.png", "icon-512.png", "apple-touch-icon.png", "og.png"]) {
  fs.copyFileSync(path.join(SRC, "assets", f), path.join(OUT, f));
}

/* ---------- parçalar ---------- */
const CSP = [
  "default-src 'self'",
  "script-src 'self' https://istatistik.bumba.tr",
  "connect-src 'self' https://istatistik.bumba.tr https://bumbagroup.com",
  "img-src 'self' data: https://bumbagroup.com",
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
  const d = (i) => `<span class="flip-d" data-flip-d="${i}">0</span>`;
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

// Her model: id, sınıf ve içerik. Sıra, kaydırma sırasıdır.
const MODELS = [
  { id: "classic", cls: "s-analog", html: (tz) => analogSvg({ tz, size: "lg" }) + srTime(tz) },
  { id: "minimal", cls: "s-analog", html: (tz) => analogSvg({ tz, size: "lg", numerals: false, minimal: true }) + srTime(tz) },
  { id: "analog-date", cls: "s-analog-date", html: (tz) => analogSvg({ tz, size: "md" }) + pDate(tz) + srTime(tz) },
  { id: "digital", cls: "s-digital", html: (tz) => pDigital(tz) },
  { id: "digital-date", cls: "s-digital", html: (tz) => pTz(tz) + pDigital(tz) + pDate(tz) },
  { id: "duo", cls: "s-duo", html: (tz) => analogSvg({ tz, size: "sm" }) + `<div class="duo-text">${pDigital(tz)}</div>` },
  { id: "duo-date", cls: "s-duo", html: (tz) => analogSvg({ tz, size: "sm" }) + `<div class="duo-text">${pTz(tz)}${pDigital(tz)}${pDate(tz)}</div>` },
  { id: "calendar", cls: "s-cal", html: (tz) => `<div class="cal-side">${pTz(tz)}${pDigital(tz, " clock-sm")}${pDate(tz)}</div>${calendarHtml(tz)}` },
  { id: "calendar-analog", cls: "s-cal", html: (tz) => analogSvg({ tz, size: "xs", numerals: true }) + calendarHtml(tz) + srTime(tz) },
  { id: "flip", cls: "s-flip", html: (tz) => flipHtml(tz) + pDate(tz) },
  { id: "words", cls: "s-words", html: (tz) => `<p class="words" role="timer" data-live="words"${tzA(tz)}>&nbsp;</p>` + pDate(tz) },
  { id: "rings", cls: "s-rings", html: (tz) => ringsHtml(tz) + pDate(tz) },
  { id: "bigdate", cls: "s-bigdate", html: (tz) => `<p class="bigdate-day" data-live="day"${tzA(tz)}>–</p><p class="bigdate-month" data-live="monthyear"${tzA(tz)}>&nbsp;</p><p class="bigdate-week" data-live="weekday"${tzA(tz)}>&nbsp;</p>${pDigital(tz, " clock-sm")}` },
];

function clockStage(lang, { tz = "" } = {}) {
  const t = I[lang];
  const s = t.stage;
  const total = MODELS.length;
  const slides = MODELS.map((m, i) => {
    const name = s.models[m.id];
    return `<section class="slide ${m.cls}" id="model-${m.id}" data-model="${m.id}" data-name="${esc(name)}" role="group" aria-roledescription="${esc(s.slideRole)}" aria-label="${esc(tpl(s.slide, { n: i + 1, total, name }))}"${i ? ' aria-hidden="true"' : ""}>${m.html(tz)}</section>`;
  }).join("");
  const dots = MODELS.map(
    (m, i) => `<button type="button" class="dot" data-dot="${i}" aria-label="${esc(s.models[m.id])}" title="${esc(s.models[m.id])}"${i ? "" : ' aria-current="true"'}></button>`
  ).join("");
  return `<section class="stage" data-carousel aria-roledescription="${esc(s.carousel)}" aria-labelledby="clock-h">
  <h2 id="clock-h" class="sr-only">${esc(s.label)}</h2>
  <div class="stage-track" data-track tabindex="0" aria-label="${esc(s.hint)}">${slides}</div>
  <div class="stage-bar">
    <button type="button" class="icon-btn" data-nav="-1" aria-label="${esc(s.prev)}">${ICON_PREV}</button>
    <p class="stage-name" data-stage-name aria-live="polite">${esc(s.models[MODELS[0].id])}</p>
    <button type="button" class="icon-btn" data-nav="1" aria-label="${esc(s.next)}">${ICON_NEXT}</button>
    <div class="dots" role="group" aria-label="${esc(s.dots)}">${dots}</div>
    <div class="stage-tools">
      <button type="button" class="toggle" data-pref="h12" aria-pressed="false">${esc(t.client.format24)}</button>
      <button type="button" class="toggle" data-pref="sec" aria-pressed="true">${esc(t.client.seconds)}</button>
      <button type="button" class="toggle" data-pref="sync" aria-pressed="true">${esc(t.client.syncLabel)}</button>
      <button type="button" class="icon-btn" data-fullscreen aria-pressed="false" aria-label="${esc(t.client.fullscreen)}" title="${esc(t.client.fullscreen)}">${ICON_FULL}</button>
    </div>
  </div>
  <noscript><p class="meta stage-note">${esc(t.ui.noscript)}</p></noscript>
</section>
<p class="meta stage-hint">${esc(s.hint)}</p>
<p class="meta sync-line" data-sync-status>&nbsp;</p>`;
}

function cityCard(lang, c) {
  return `<li class="city-card"><a href="${cityPath(lang, c)}"><span class="city-name">${esc(c[lang].name)}</span><span class="city-time" data-live="time" data-tz="${c.tz}">--:--</span><span class="city-diff" data-live="diff" data-tz="${c.tz}">&nbsp;</span></a></li>`;
}

function faqHtml(items, headingId) {
  return `<div class="faq" aria-labelledby="${headingId}">${items
    .map((f) => `<details><summary>${esc(f.q)}</summary><p>${esc(f.a)}</p></details>`)
    .join("")}</div>`;
}

function crumbs(lang, trail) {
  const t = I[lang];
  return `<nav class="crumbs" aria-label="${esc(t.ui.breadcrumb)}"><ol>${trail
    .map((c, i) =>
      i === trail.length - 1 ? `<li aria-current="page">${esc(c.name)}</li>` : `<li><a href="${c.path}">${esc(c.name)}</a></li>`
    )
    .join("")}</ol></nav>`;
}

function listForm(lang, pagePath) {
  const t = I[lang];
  const l = t.list;
  const url = abs(pagePath);
  return `<section class="list-box" id="liste" aria-labelledby="liste-h">
  <h2 id="liste-h">${esc(l.heading)}</h2>
  <p>${esc(l.lead)}</p>
  <p class="form-msg ok" id="liste-tamam" role="status">${esc(l.ok)}</p>
  <p class="form-msg err" id="liste-hata" role="alert">${esc(l.err)}</p>
  <form action="https://bumbagroup.com/api/liste/katil" method="post">
    <input type="hidden" name="site" value="${esc(cfg.listKey)}">
    <input type="hidden" name="dil" value="${LIST_LANGS.includes(lang) ? lang : "en"}">
    <input type="hidden" name="kaynak" value="${esc(url)}">
    <input type="hidden" name="donus" value="${esc(url)}">
    <div class="field">
      <label for="liste-eposta">${esc(l.emailLabel)}</label>
      <input id="liste-eposta" type="email" name="eposta" required autocomplete="email">
    </div>
    <div class="check">
      <input id="liste-riza" type="checkbox" name="riza" value="on" required>
      <label for="liste-riza">${esc(l.consentPre)}<a href="${esc(l.noteLink)}" rel="noopener">${esc(l.consentLink)}</a>${esc(l.consentPost)}</label>
    </div>
    <input class="bot-tuzagi" type="text" name="web_sitesi" tabindex="-1" autocomplete="off" aria-hidden="true">
    <button class="btn" type="submit">${esc(l.submit)}</button>
  </form>
</section>`;
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

/* ---------- sayfa iskeleti ---------- */
function layout(p) {
  const lang = p.lang;
  const t = I[lang];
  const u = t.ui;
  const isError = p.error;
  const canonical = p.canonicalPath ? abs(p.canonicalPath) : p.path ? abs(p.path) : "";
  const alt = p.alt; // { tr: path, en: path, ... }
  const alternates = alt && !p.noHreflang
    ? [...LANGS.map((l) => `<link rel="alternate" hreflang="${l}" href="${abs(alt[l])}">`), `<link rel="alternate" hreflang="x-default" href="${abs(alt.tr)}">`].join("\n")
    : "";
  const clientT = {
    ...t.client,
    searchHint: u.searchHint,
    searchEmpty: u.searchEmpty,
    searchCount: u.searchCount,
    searchError: u.searchError,
  };
  const pageCfg = {
    locale: t.locale,
    t: { ...clientT, ...(p.clientExtra && p.clientExtra.tools ? { tools: p.clientExtra.tools } : {}), ...((p.clientExtra && p.clientExtra.t) || {}) },
    ...(p.clientExtra && p.clientExtra.cities ? { cities: p.clientExtra.cities } : {}),
    searchIndex: `/assets/search-${lang}.json?v=${p.searchVersion}`,
    eventsUrl: `/assets/events-${lang}.json?v=${eventsVersion[lang]}`,
    cal: { ...((p.clientExtra && p.clientExtra.cal) || {}), months: t.calendar.months, weekdays: t.calendar.weekdays, weekdaysShort: t.calendar.weekdaysShort, fullFmt: t.calendar.fullFmt, shortFmt: t.calendar.shortFmt, dateFmt: t.calendar.dateFmt },
  };
  const og = p.noindex
    ? ""
    : `<meta property="og:type" content="website">
<meta property="og:site_name" content="${esc(cfg.name)}">
<meta property="og:title" content="${esc(p.title)}">
<meta property="og:description" content="${esc(p.description)}">
<meta property="og:url" content="${canonical}">
<meta property="og:locale" content="${t.ogLocale}">
${LANGS.filter((l) => l !== lang).map((l) => `<meta property="og:locale:alternate" content="${I[l].ogLocale}">`).join("\n")}
<meta property="og:image" content="${abs("/og.png")}">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:image:alt" content="${esc(cfg.name)}: ${esc(t.site.tagline)}">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="${esc(p.title)}">
<meta name="twitter:description" content="${esc(p.description)}">
<meta name="twitter:image" content="${abs("/og.png")}">`;
  const umami = UMAMI_ID
    ? `<script defer src="https://istatistik.bumba.tr/script.js" data-website-id="${esc(UMAMI_ID)}" data-domains="${esc(HOST)}"></script>`
    : "";
  const ld = p.ld ? `<script type="application/ld+json">${JSON.stringify({ "@context": "https://schema.org", "@graph": p.ld }).replace(/</g, "\\u003c")}</script>` : "";

  const navLink = (key, label) =>
    `<a href="${key === "calendar" ? yearPath(lang, BUILD_YEAR) : ROUTES[lang][key]}"${p.nav === key ? ' aria-current="page"' : ""}>${esc(label)}</a>`;
  const langItems = LANGS.map((l) => {
    const href = isError ? ROUTES[l].home : alt[l];
    return `<li><a href="${href}" hreflang="${l}" lang="${l}" data-lang-switch${l === lang ? ' aria-current="true"' : ""}>${esc(LANG_NAMES[l])}</a></li>`;
  }).join("");
  const langSwitch = `<details class="lang-menu"><summary class="lang-link" aria-label="${esc(u.langMenu)}"><span aria-hidden="true">${lang.toUpperCase()}</span></summary><ul>${langItems}</ul></details>`;

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
${umami}
${ld}
</head>
<body>
<a class="skip-link" href="#main">${esc(u.skip)}</a>
<header class="site-header">
  <div class="wrap">
    <a class="brand" href="${ROUTES[lang].home}" aria-label="${esc(u.logoAlt)}">${LOGO}<span>${esc(cfg.name)}</span></a>
    <nav class="nav" aria-label="${esc(u.mainNav)}">${navLink("home", u.home)}${navLink("world", u.world)}${navLink("calendar", u.calendar)}${navLink("tools", u.tools)}</nav>
    <div class="tools">
      <button type="button" class="icon-btn" data-search-open aria-haspopup="dialog" aria-label="${esc(u.searchOpen)}">${ICON_SEARCH}</button>
      ${langSwitch}
      <button type="button" class="icon-btn" data-theme-toggle aria-label="${esc(u.themeToggle)}">${ICON_MOON}${ICON_SUN}</button>
    </div>
  </div>
</header>
<main id="main">
${p.body}
</main>
<footer class="site-footer">
  <div class="wrap">
${isError ? "" : listForm(lang, p.path)}
    <div class="footer-sign">
      ${badge(lang)}
      <p>${esc(u.copyright)}</p>
      ${isError ? "" : `<p>${esc(u.updated)}: <time datetime="${cfg.updated}">${esc(updatedText(lang))}</time></p>`}
    </div>
    <nav aria-label="${esc(u.footerNav)}" class="footer-sign">
      <ul class="footer-nav">
        <li><a href="${ROUTES[lang].home}">${esc(u.home)}</a></li>
        <li><a href="${ROUTES[lang].world}">${esc(u.world)}</a></li>
        <li><a href="${yearPath(lang, BUILD_YEAR)}">${esc(u.calendar)}</a></li>
        <li><a href="${ROUTES[lang].tools}">${esc(u.tools)}</a></li>
        <li><a href="${ROUTES[lang].privacy}">${esc(u.privacy)}</a></li>
      </ul>
    </nav>
  </div>
</footer>
${searchDialog(lang)}
<script type="application/json" id="page-config">${JSON.stringify(pageCfg).replace(/</g, "\\u003c")}</script>
<script defer src="${ASSETS["main.js"]}"></script>
${(p.scripts || []).map((n) => `<script defer src="${ASSETS[n]}"></script>`).join("\n")}
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

function buildHome(lang) {
  const t = I[lang];
  const h = t.home;
  const pagePath = ROUTES[lang].home;
  const featured = ["istanbul", "london", "new-york", "los-angeles", "dubai", "moscow", "tokyo", "sydney"].map((k) =>
    CITIES.find((c) => c.key === k)
  );
  const body = `<section class="hero">
  <div class="wrap">
    <h1>${esc(h.h1)}</h1>
    <p class="lead">${esc(tpl(h.lead, { n: MODELS.length }))}</p>
    ${clockStage(lang)}
  </div>
</section>
<section class="section" id="upcoming" aria-labelledby="upcoming-h">
  <div class="wrap">
    <h2 id="upcoming-h">${esc(h.upcomingHeading)}</h2>
    <p class="meta">${esc(h.upcomingLead)}</p>
    <ul class="upcoming" data-upcoming></ul>
    <p><a href="${yearPath(lang, BUILD_YEAR)}">${esc(h.upcomingAll)} ${ARROW(lang)}</a></p>
  </div>
</section>
<section class="section" id="facts" aria-labelledby="facts-h">
  <div class="wrap">
    <h2 id="facts-h">${esc(h.factsHeading)}</h2>
    <dl class="facts">
      <div class="fact"><dt>${esc(h.factWeek)}</dt><dd data-live="isoweek">–</dd></div>
      <div class="fact"><dt>${esc(h.factDoy)}</dt><dd data-live="doy">–</dd></div>
      <div class="fact"><dt>${esc(h.factLeft)}</dt><dd data-live="daysleft">–</dd></div>
      <div class="fact"><dt>${esc(h.factUnix)}</dt><dd data-live="unix">–</dd></div>
      <div class="fact"><dt>${esc(h.factUtc)}</dt><dd data-live="utc">–</dd></div>
      <div class="fact"><dt>${esc(h.factProgress)}</dt><dd><progress data-live="progress" max="100" value="0">0%</progress><span class="note" data-progress-note>&nbsp;</span></dd></div>
    </dl>
  </div>
</section>
<section class="section" aria-label="${esc(t.tools.hub.infoHeading)}">
  <div class="wrap">
    <ul class="inline-links">${INFO_KEYS.map((k) => `<li><a href="${ROUTES[lang][k]}">${esc(t.tools.hub.cards[k].name)}</a></li>`).join("")}</ul>
  </div>
</section>
<section class="section" id="world" aria-labelledby="world-h">
  <div class="wrap">
    <h2 id="world-h">${esc(h.worldHeading)}</h2>
    <p class="meta">${esc(h.worldLead)}</p>
    <ul class="city-grid">${featured.map((c) => cityCard(lang, c)).join("")}</ul>
    <p><a href="${ROUTES[lang].world}">${esc(h.worldAll)} ${ARROW(lang)}</a></p>
  </div>
</section>
<section class="section" id="faq" aria-labelledby="faq-h">
  <div class="wrap">
    <h2 id="faq-h">${esc(h.faqHeading)}</h2>
    ${faqHtml(h.faq, "faq-h")}
  </div>
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
    faqLd(h.faq),
  ];
  const e = searchEntries[lang];
  e.push({ t: t.ui.home, u: pagePath, d: h.description, k: "saat tarih bugün takvim clock date today time now" });
  e.push({ t: h.upcomingHeading, u: pagePath + "#upcoming", d: h.upcomingLead, k: "bayram tatil resmi tatil arife ramazan kaç gün kaldı holiday bayram days left" });
  e.push({ t: t.stage.label, u: pagePath + "#model-classic", d: t.stage.hint, k: "saat model stil style clock tam ekran full screen fullscreen" });
  MODELS.forEach((m) => e.push({ t: t.stage.models[m.id], u: `${pagePath}#model-${m.id}`, d: t.stage.label, k: "saat model clock style" }));
  e.push({ t: h.factsHeading, u: pagePath + "#facts", d: `${h.factWeek}, ${h.factDoy}, ${h.factLeft}, ${h.factUnix}, ${h.factUtc}`, k: "hafta week unix utc gün day yıl year" });
  h.faq.forEach((f) => e.push({ t: f.q, u: pagePath + "#faq", d: f.a, k: "sss faq" }));
  e.push({ t: t.list.heading, u: pagePath + "#liste", d: t.list.lead, k: "e-posta email bülten newsletter liste list" });
  return { lang, key: "home", path: pagePath, title: h.title, description: h.description, body, ld, home: true, nav: "home", scripts: ["calendar.js"] };
}

function buildWorld(lang) {
  const t = I[lang];
  const w = t.world;
  const pagePath = ROUTES[lang].world;
  const rows = CITIES.map(
    (c) => `<tr><th scope="row"><a href="${cityPath(lang, c)}">${esc(c[lang].name)}</a></th><td>${esc(c[lang].country)}</td><td class="num" data-live="time" data-sec data-tz="${c.tz}">--:--:--</td><td class="num" data-live="offset" data-tz="${c.tz}">UTC</td><td data-live="diff" data-tz="${c.tz}">&nbsp;</td></tr>`
  ).join("");
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: w.h1, path: pagePath },
  ];
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(w.h1)}</h1>
    <p class="lead">${esc(tpl(w.lead, { n: CITIES.length }))}</p>
    <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    <div class="controls">
      <button type="button" class="toggle" data-pref="h12" aria-pressed="false">${esc(t.client.format24)}</button>
      <button type="button" class="toggle" data-pref="sec" aria-pressed="true">${esc(t.client.seconds)}</button>
    </div>
  </div>
</section>
<section class="section">
  <div class="wrap">
    <div class="table-wrap"><table><caption>${esc(w.tableCaption)}</caption><thead><tr><th scope="col">${esc(w.colCity)}</th><th scope="col">${esc(w.colCountry)}</th><th scope="col">${esc(w.colTime)}</th><th scope="col">${esc(w.colOffset)}</th><th scope="col">${esc(w.colDiff)}</th></tr></thead><tbody>${rows}</tbody></table></div>
  </div>
</section>`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, w.title, w.description), breadcrumbLd(trail)];
  searchEntries[lang].push({ t: w.h1, u: pagePath, d: w.description, k: "dünya şehir saat farkı world city time zone timezone" });
  return { lang, key: "world", path: pagePath, title: w.title, description: w.description, body, ld, nav: "world" };
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
  const others = CITIES.filter((o) => o.key !== c.key && o.tz !== c.tz).slice(0, 8);
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(tpl(ct.h1, v))}</h1>
    <p class="lead">${esc(tpl(ct.lead, v))} ${esc(tpl(c.dst ? ct.dstYes : ct.dstNo, v))}</p>
    ${clockStage(lang, { tz: c.tz })}
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
<section class="section" id="faq" aria-labelledby="faq-h">
  <div class="wrap">
    <h2 id="faq-h">${esc(tpl(ct.faqHeading, v))}</h2>
    ${faqHtml(faq, "faq-h")}
  </div>
</section>
<section class="section" aria-labelledby="other-h">
  <div class="wrap">
    <h2 id="other-h">${esc(ct.otherHeading)}</h2>
    <ul class="city-grid">${others.map((o) => cityCard(lang, o)).join("")}</ul>
    <p><a href="${ROUTES[lang].world}">${esc(t.home.worldAll)} ${ARROW(lang)}</a></p>
  </div>
</section>`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, title, description), breadcrumbLd(trail), faqLd(faq)];
  searchEntries[lang].push({
    t: n.name,
    u: pagePath,
    d: description,
    k: [n.name, n.loc, n.country, c.tz, ...LANGS.map((l) => I[l].ui.world), "saat kaç time"].filter(Boolean).join(" "),
  });
  return { lang, key: "city:" + c.key, path: pagePath, title, description, body, ld, nav: "world" };
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
      const exam = es.some((e) => e.type === "exam") || inRange(date, cy.examRanges);
      const cls = [rank && `d-${rank}`, !rank && school && "d-school", !rank && exam && "d-exam", inRange(date, cy.ramadans) && "d-ramadan", c >= 5 && "d-weekend"].filter(Boolean).join(" ");
      const label = es.length ? `<span class="sr-only">, ${esc(es.map((e) => `${cal.events[e.key]} (${cal.types[TYPE_LABEL_KEY[e.type]]})`).join("; "))}</span>` : "";
      const title = es.length ? ` title="${esc(es.map((e) => cal.events[e.key]).join("; "))}"` : "";
      tr += `<td${cls ? ` class="${cls}"` : ""} data-d="${date}"${title}>${d}${label}</td>`;
    }
    rows += `<tr>${tr}</tr>`;
  }
  return `<table class="ymonth"><caption>${esc(cal.months[month - 1])}</caption><thead><tr>${head}</tr></thead><tbody>${rows}</tbody></table>`;
}

function buildCalendarYear(lang, year, { index = false } = {}) {
  const t = I[lang];
  const cal = t.calendar;
  const f = makeFmt(cal, lang);
  const cy = YEAR_DATA[year];
  const counts = holidayCounts(cy.events);
  const byDate = {};
  cy.events.forEach((e) => (byDate[e.date] ||= []).push(e));
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
  const legend = ["holiday", "half", "religious", "ramadan", "special", ...(cy.events.some((e) => e.type === "school") ? ["school"] : []), ...(cy.events.some((e) => e.type === "exam") ? ["exam"] : []), "weekend", "today"]
    .map((k) => `<li><span class="lg lg-${k}" aria-hidden="true"></span>${esc(cal.types[k])}</li>`).join("");
  const rows = cy.events
    .map((e) => `<tr data-d="${e.date}"><th scope="row">${esc(f.short(e.date))}</th><td>${esc(cal.weekdays[dowOf(e.date)])}</td><td>${esc(cal.events[e.key])}</td><td>${esc(cal.types[TYPE_LABEL_KEY[e.type]])}</td><td data-left></td></tr>`)
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
    <p class="meta">${esc(cal.note)}</p>
    ${cy.events.some((e) => e.type === "school") ? `<p class="meta">${esc(cal.schoolNote)}</p>` : ""}
    ${cy.events.some((e) => e.type === "exam") ? `<p class="meta">${esc(cal.examNote)}</p>` : ""}
  </div>
</section>
<section class="section" id="events" aria-labelledby="events-h">
  <div class="wrap">
    <h2 id="events-h">${esc(tpl(cal.eventsHeading, v))}</h2>
    <div class="table-wrap"><table class="events-table"><caption>${esc(tpl(cal.tableCaption, v))}</caption><thead><tr><th scope="col">${esc(cal.colDate)}</th><th scope="col">${esc(cal.colDay)}</th><th scope="col">${esc(cal.colEvent)}</th><th scope="col">${esc(cal.colType)}</th><th scope="col">${esc(cal.colLeft)}</th></tr></thead><tbody>${rows}</tbody></table></div>
  </div>
</section>
<section class="section" id="faq" aria-labelledby="faq-h">
  <div class="wrap">
    <h2 id="faq-h">${esc(tpl(cal.faqHeading, v))}</h2>
    ${faqHtml(faq, "faq-h")}
  </div>
</section>`;
  const ld = [...baseLd(lang), webPageLd(lang, canonicalPath, title, description), breadcrumbLd(trail), faqLd(faq)];
  const key = index ? "calendar-index" : `calendar:${year}`;
  if (!index) {
    const e = searchEntries[lang];
    e.push({ t: tpl(cal.h1, v), u: canonicalPath, d: description, k: "takvim resmi tatil bayram arife ramazan kandil calendar holiday ramadan eid " + year });
    for (const k of ["rb1", "kb1", "ramadanStart"]) {
      const ee = date(k);
      if (ee) e.push({ t: `${cal.events[k]} ${year}`, u: `${canonicalPath}#events`, d: f.full(ee.date), k: "takvim bayram ramazan kurban calendar eid ramadan " + year });
    }
  }
  return { lang, key, path: pagePath, title, description, body, ld, nav: "calendar", scripts: ["calendar.js"], clientExtra: { cal: { year } }, noindex: index, canonicalPath: index ? canonicalPath : undefined, noHreflang: index, mapKey: index ? undefined : key };
}

/* ---------- Araç sayfaları ---------- */
const TOOL_KEYS = ["stopwatch", "countdown", "alarm", "pomodoro"];
const btn = (cls, attrs, label) => `<button type="button" class="${cls}" ${attrs}>${esc(label)}</button>`;
const numField = (id, label, attrs) => `<div class="field"><label for="${id}">${esc(label)}</label><input id="${id}" type="number" inputmode="numeric" ${attrs}></div>`;
const ringBanner = (tt, name) => `<div class="ring-banner" role="alertdialog" aria-label="${esc(tt.alarm.ringing)}" data-ring-banner hidden><p class="ring-title" data-ring-title>${esc(tt.alarm.ringing)}</p><div class="tool-actions">${btn("btn", "data-ring-stop", tt.common.close)}${name === "alarm" ? btn("toggle", "data-ring-snooze", tt.alarm.snooze) : ""}</div></div>`;
const soundRow = (tt) => `<div class="tool-extra"><label class="check-inline"><input type="checkbox" data-sound checked> ${esc(tt.common.sound)}</label>${btn("toggle", "data-test-sound", tt.common.testSound)}${btn("toggle", "data-notify", tt.common.notify)}</div>`;

function toolBody(lang, key) {
  const tt = I[lang].tools;
  const s = tt[key];
  if (key === "stopwatch")
    return `<p class="big-time" role="timer" data-sw-display>00:00:00.00</p>
<div class="tool-actions">${btn("btn", "data-sw-start", s.start)}${btn("toggle", "data-sw-lap disabled", s.lap)}${btn("toggle", "data-sw-reset", s.reset)}</div>
<p class="meta">${esc(s.hint)}</p>
<p class="sr-only" role="status" aria-live="polite" data-sw-announce></p>
<p class="meta" data-sw-empty>${esc(s.empty)}</p>
<div class="table-wrap laps" data-sw-laps hidden><table><caption>${esc(s.lapsCaption)}</caption><thead><tr><th scope="col">${esc(s.lapNo)}</th><th scope="col">${esc(s.lapTime)}</th><th scope="col">${esc(s.total)}</th></tr></thead><tbody data-sw-body></tbody></table></div>`;
  if (key === "countdown")
    return `<div class="tabs" role="tablist" aria-label="${esc(s.panel)}">
  <button type="button" role="tab" id="tab-timer" aria-selected="true" aria-controls="cd-timer" data-cd-tab="timer">${esc(s.tabTimer)}</button>
  <button type="button" role="tab" id="tab-date" aria-selected="false" aria-controls="cd-date" tabindex="-1" data-cd-tab="date">${esc(s.tabDate)}</button>
</div>
<div role="tabpanel" id="cd-timer" aria-labelledby="tab-timer" data-cd-panel="timer">
  <div class="inputs-row">${numField("cd-h", s.hours, 'min="0" max="99" value="0" data-cd-h')}${numField("cd-m", s.minutes, 'min="0" max="59" value="5" data-cd-m')}${numField("cd-s", s.seconds, 'min="0" max="59" value="0" data-cd-s')}</div>
  <div class="presets" role="group" aria-label="${esc(s.presetsLabel)}">${[1, 5, 10, 15, 30, 60].map((n) => btn("chip", `data-cd-preset="${n}"`, tpl(s.presetMin, { n }))).join("")}</div>
</div>
<div role="tabpanel" id="cd-date" aria-labelledby="tab-date" data-cd-panel="date" hidden>
  <div class="field"><label for="cd-target">${esc(s.targetLabel)}</label><input id="cd-target" type="datetime-local" data-cd-target></div>
  <div class="field"><label for="cd-name">${esc(s.nameLabel)}</label><input id="cd-name" type="text" maxlength="40" placeholder="${esc(s.namePlaceholder)}" data-cd-nameinput></div>
  <p class="meta">${esc(s.quickHeading)}</p>
  <div class="presets" data-cd-quick>${btn("chip", 'data-cd-quick-newyear', s.quickNewYear)}</div>
</div>
<p class="cd-name" data-cd-name hidden></p>
<p class="big-time" role="timer" aria-label="${esc(s.remaining)}" data-cd-display>00:00:00</p>
<progress class="tool-progress" max="100" value="0" data-cd-progress aria-hidden="true"></progress>
<p class="cd-message" role="status" data-cd-message hidden></p>
<div class="tool-actions">${btn("btn", "data-cd-start", s.start)}${btn("toggle", "data-cd-reset", s.reset)}${btn("toggle", "data-cd-copy", s.copyLink)}</div>
<p class="meta">${esc(s.shareNote)}</p>
${soundRow(tt)}
${ringBanner(tt, "countdown")}`;
  if (key === "alarm")
    return `<form class="alarm-form" data-alarm-form>
  <div class="inputs-row">
    <div class="field"><label for="al-time">${esc(s.timeLabel)}</label><input id="al-time" type="time" value="07:00" required data-al-time></div>
    <div class="field grow"><label for="al-label">${esc(s.labelLabel)}</label><input id="al-label" type="text" maxlength="40" placeholder="${esc(s.labelPlaceholder)}" data-al-label></div>
  </div>
  <fieldset class="days"><legend>${esc(s.repeatLabel)}</legend>
    ${s.dayNames.map((d, i) => `<label class="day"><input type="checkbox" value="${i}" data-al-day><span>${esc(d)}</span></label>`).join("")}
    <span class="presets">${btn("chip", 'data-al-preset="none"', s.once)}${btn("chip", 'data-al-preset="weekdays"', s.weekdays)}${btn("chip", 'data-al-preset="all"', s.every)}</span>
  </fieldset>
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
  return `<p class="phase" data-pomo-phase aria-live="polite">${esc(s.work)}</p>
<p class="big-time" role="timer" aria-label="${esc(s.phaseLabel)}" data-pomo-display>25:00</p>
<progress class="tool-progress" max="100" value="0" data-pomo-progress aria-hidden="true"></progress>
<p class="meta" data-pomo-round>&nbsp;</p>
<div class="tool-actions">${btn("btn", "data-pomo-start", s.start)}${btn("toggle", "data-pomo-skip", s.skip)}${btn("toggle", "data-pomo-reset", s.reset)}</div>
<p class="meta" data-pomo-count>&nbsp;</p>
<details class="pomo-settings"><summary>${esc(s.settings)}</summary>
  <div class="inputs-row">${numField("po-w", s.workMin, 'min="1" max="180" value="25" data-pomo-w')}${numField("po-s", s.shortMin, 'min="1" max="60" value="5" data-pomo-s')}${numField("po-l", s.longMin, 'min="1" max="120" value="15" data-pomo-l')}${numField("po-r", s.roundsLabel, 'min="2" max="12" value="4" data-pomo-r')}</div>
</details>
${soundRow(tt)}`;
}

function buildTool(lang, key) {
  const t = I[lang];
  const tt = t.tools;
  const s = tt[key];
  const pagePath = ROUTES[lang][key];
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: t.ui.tools, path: ROUTES[lang].tools },
    { name: s.h1, path: pagePath },
  ];
  const related = TOOL_KEYS.filter((k) => k !== key)
    .map((k) => `<li class="city-card"><a href="${ROUTES[lang][k]}"><span class="city-name">${esc(tt.hub.cards[k].name)}</span><span class="city-diff">${esc(tt.hub.cards[k].desc)}</span></a></li>`)
    .join("");
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(s.h1)}</h1>
    <p class="lead">${esc(s.lead)}</p>
    <section class="tool" data-tool="${key}" aria-labelledby="tool-h">
      <div class="tool-head"><h2 id="tool-h" class="tool-title">${esc(s.panel)}</h2><button type="button" class="icon-btn" data-fs aria-pressed="false" aria-label="${esc(tt.common.fullscreen)}" title="${esc(tt.common.fullscreen)}">${ICON_FULL}</button></div>
      ${toolBody(lang, key)}
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>
    ${key === "alarm" || key === "countdown" || key === "pomodoro" ? `<p class="meta">${esc(tt.common.pageOpenNote)}</p>` : ""}
  </div>
</section>
<section class="section" id="faq" aria-labelledby="faq-h">
  <div class="wrap">
    <h2 id="faq-h">${esc(tt.common.faqHeading)}</h2>
    ${faqHtml(s.faq, "faq-h")}
  </div>
</section>
<section class="section" aria-labelledby="related-h">
  <div class="wrap">
    <h2 id="related-h">${esc(tt.common.relatedHeading)}</h2>
    <ul class="city-grid">${related}</ul>
    <p><a href="${ROUTES[lang].tools}">${esc(tt.common.toolsHub)} ${ARROW(lang)}</a></p>
  </div>
</section>`;
  const ld = [
    ...baseLd(lang),
    webPageLd(lang, pagePath, s.title, s.description),
    { "@type": "WebApplication", name: s.h1, url: abs(pagePath), description: s.description, applicationCategory: "UtilitiesApplication", operatingSystem: "Any", inLanguage: lang, isAccessibleForFree: true, offers: { "@type": "Offer", price: "0", priceCurrency: "TRY" }, publisher: { "@id": `${ORIGIN}/#org` } },
    breadcrumbLd(trail),
    faqLd(s.faq),
  ];
  searchEntries[lang].push({ t: s.h1, u: pagePath, d: s.description, k: tt.hub.cards[key].name + " " + { stopwatch: "kronometre stopwatch tur lap", countdown: "geri sayım countdown timer zamanlayıcı yılbaşı bayram", alarm: "alarm saati alarm clock uyandırma", pomodoro: "pomodoro odak focus mola break" }[key] });
  return { lang, key: `tool:${key}`, path: pagePath, title: s.title, description: s.description, body, ld, nav: "tools", scripts: ["tools.js"], clientExtra: { tools: tt, cal: {} } };
}

function buildToolsHub(lang) {
  const t = I[lang];
  const h = t.tools.hub;
  const pagePath = ROUTES[lang].tools;
  const trail = [
    { name: t.ui.home, path: ROUTES[lang].home },
    { name: h.h1, path: pagePath },
  ];
  const card = (href, name, desc) => `<li class="city-card"><a href="${href}"><span class="city-name">${esc(name)}</span><span class="city-diff">${esc(desc)}</span><span class="card-open">${esc(h.open)} ${ARROW(lang)}</span></a></li>`;
  const cards = TOOL_KEYS.map((k) => card(ROUTES[lang][k], h.cards[k].name, h.cards[k].desc)).join("") + ["converter", "planner", "datecalc", "embed", "diff"].map((k) => card(ROUTES[lang][k], h.cards[k].name, h.cards[k].desc)).join("") + card(yearPath(lang, BUILD_YEAR), t.calendar.hubName, t.calendar.hubDesc) + card(ROUTES[lang].world, t.ui.world, t.world.description.split(". ")[0] + ".");
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(h.h1)}</h1>
    <p class="lead">${esc(h.lead)}</p>
    <ul class="city-grid tool-grid">${cards}</ul>
    <h2>${esc(h.infoHeading)}</h2>
    <ul class="city-grid tool-grid">${INFO_KEYS.map((k) => card(ROUTES[lang][k], h.cards[k].name, h.cards[k].desc)).join("")}</ul>
  </div>
</section>`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, h.title, h.description), breadcrumbLd(trail)];
  searchEntries[lang].push({ t: h.h1, u: pagePath, d: h.description, k: "araç tool kronometre geri sayım alarm pomodoro stopwatch countdown" });
  return { lang, key: "tools", path: pagePath, title: h.title, description: h.description, body, ld, nav: "tools" };
}

/* ---------- Bilgi sayfaları: hafta numarası, bugünün tarihi, Türkiye saat dilimi ---------- */
const INFO_KEYS = ["week", "today", "tz"];
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
  const related = [...TOOL_KEYS.slice(0, 2).map((k) => [ROUTES[lang][k], t.tools.hub.cards[k].name, t.tools.hub.cards[k].desc]), ...INFO_KEYS.filter((k) => k !== key).map((k) => [ROUTES[lang][k], t.tools.hub.cards[k].name, t.tools.hub.cards[k].desc])]
    .map(([href, name, desc]) => `<li class="city-card"><a href="${href}"><span class="city-name">${esc(name)}</span><span class="city-diff">${esc(desc)}</span></a></li>`).join("");
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
<section class="section" id="faq" aria-labelledby="faq-h">
  <div class="wrap">
    <h2 id="faq-h">${esc(s.faqHeading)}</h2>
    ${faqHtml(faqItems, "faq-h")}
  </div>
</section>
<section class="section" aria-labelledby="related-h">
  <div class="wrap">
    <h2 id="related-h">${esc(t.tools.common.relatedHeading)}</h2>
    <ul class="city-grid">${related}</ul>
    <p><a href="${ROUTES[lang].tools}">${esc(t.tools.common.toolsHub)} ${ARROW(lang)}</a></p>
  </div>
</section>`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, title, description), breadcrumbLd(trail), faqLd(faqItems)];
  searchEntries[lang].push({ t: tpl(s.h1, v), u: pagePath, d: description, k: searchK });
  faqItems.forEach((f) => searchEntries[lang].push({ t: f.q, u: pagePath + "#faq", d: f.a, k: searchK }));
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

function buildToday(lang) {
  const t = I[lang];
  const s = t.info.today;
  const fact = (label, kind) => `<div class="fact"><dt>${esc(label)}</dt><dd data-live="${kind}">–</dd></div>`;
  const body = {
    hero: `<p class="big-time info-date" role="timer" data-live="date">&nbsp;</p>`,
    sections: `<section class="section" id="details" aria-labelledby="details-h">
  <div class="wrap">
    <h2 id="details-h">${esc(s.factsHeading)}</h2>
    <dl class="facts">${fact(s.fWeekday, "weekday")}${fact(s.fMonthYear, "monthyear")}${fact(s.fNumeric, "datenum")}${fact(s.fIso, "dateiso")}${fact(s.fDoy, "doy")}${fact(s.fWeek, "isoweek")}${fact(s.fYearLeft, "daysleft")}${fact(s.fMonthLeft, "monthleft")}${fact(s.fUnix, "unix")}</dl>
  </div>
</section>`,
  };
  return infoShell(lang, "today", { body, faq: s.faq, searchK: "bugün ayın kaçı günlerden ne tarih bugünün tarihi todays date today what day yılın günü" });
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
    return `<tr><th scope="row"><a href="${cityPath(lang, c)}">${esc(c[lang].name)}</a></th><td>${esc(stdTxt)}</td><td>${esc(c.dst ? s.yes : s.no)}</td><td data-live="diff" data-tz="${c.tz}" data-ref="Europe/Istanbul" data-ref-name="${refName}">&nbsp;</td></tr>`;
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
const stdMin = (c) => { const m = c.std.match(/([+-])(\d\d):(\d\d)/); return (m[1] === "-" ? -1 : 1) * (+m[2] * 60 + +m[3]); };
const HOME_CITY = CITIES.find((c) => c.key === "istanbul");
const PAIR_CITIES = CITIES.filter((c) => c.tz !== HOME_CITY.tz);
const pairPath = (lang, c) => `${ROUTES[lang].diff}${HOME_CITY[lang].slug}-${c[lang].slug}/`;
const durTxt = (lang, min) => { const t = I[lang].client; const a = Math.abs(min), h = Math.floor(a / 60), m = a % 60; return [h && `${h} ${t.hourShort}`, m && `${m} ${t.minuteShort}`].filter(Boolean).join(" "); };
const hh = (n) => String(((n % 24) + 24) % 24).padStart(2, "0");
const cityOptions = (lang, withMine) => `${withMine ? `<option value="">${esc(I[lang].converter.mine)}</option>` : ""}${[...CITIES].sort((a, b) => a[lang].name.localeCompare(b[lang].name, lang)).map((c) => `<option value="${c.key}">${esc(c[lang].name)} (${esc(c[lang].country)})</option>`).join("")}`;
const clientCities = (lang) => CITIES.map((c) => ({ k: c.key, n: c[lang].name, tz: c.tz }));
const relatedCards = (lang, keys) => keys.map((k) => `<li class="city-card"><a href="${ROUTES[lang][k]}"><span class="city-name">${esc(I[lang].tools.hub.cards[k].name)}</span><span class="city-diff">${esc(I[lang].tools.hub.cards[k].desc)}</span></a></li>`).join("");
const webAppLd = (lang, name, pagePath, description) => ({ "@type": "WebApplication", name, url: abs(pagePath), description, applicationCategory: "UtilitiesApplication", operatingSystem: "Any", inLanguage: lang, isAccessibleForFree: true, offers: { "@type": "Offer", price: "0", priceCurrency: "TRY" } });

function simplePage(lang, key, { title, description, h1, lead, trail, bodyInner, faq, faqHeading, scripts, clientExtra, nav = "tools", searchK, extraLd = [] }) {
  const pagePath = trail[trail.length - 1].path;
  const faqBlock = faq && faq.length ? `<section class="section" id="faq" aria-labelledby="faq-h">
  <div class="wrap">
    <h2 id="faq-h">${esc(faqHeading)}</h2>
    ${faqHtml(faq, "faq-h")}
  </div>
</section>` : "";
  const body = `<section class="hero">
  <div class="wrap">
    ${crumbs(lang, trail)}
    <h1>${esc(h1)}</h1>
    <p class="lead">${esc(lead)}</p>
    ${bodyInner.hero || ""}
  </div>
</section>
${bodyInner.sections || ""}
${faqBlock}`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, title, description), breadcrumbLd(trail), ...(faq && faq.length ? [faqLd(faq)] : []), ...extraLd];
  searchEntries[lang].push({ t: h1, u: pagePath, d: description, k: searchK });
  if (faq) faq.forEach((f) => searchEntries[lang].push({ t: f.q, u: pagePath + "#faq", d: f.a, k: searchK }));
  return { lang, key, path: pagePath, title, description, body, ld, nav, scripts, clientExtra };
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
    <p><a href="${ROUTES[lang].converter}">${esc(p.converterLink)} ${ARROW(lang)}</a> · <a href="${cityPath(lang, c)}">${esc(c[lang].name)}</a></p>
  </div>
</section>`,
  };
  return simplePage(lang, `pair:${c.key}`, { title: tpl(p.title, v), description: tpl(p.description, v), h1: tpl(p.h1, v), lead, trail, bodyInner: inner, faq, faqHeading: tpl(p.faqHeading, v), scripts: ["planner.js"], nav: "world", searchK: `saat farkı time difference ${a} ${b} ${c[lang].country} ${c.tz}` });
}

function buildPairHub(lang) {
  const t = I[lang];
  const h = t.pairHub;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: h.h1, path: ROUTES[lang].diff }];
  const list = PAIR_CITIES.map((c) => `<li class="city-card"><a href="${pairPath(lang, c)}"><span class="city-name">${esc(tpl(h.linkText, { a: HOME_CITY[lang].name, b: c[lang].name }))}</span><span class="city-diff">${esc(c[lang].country)}</span></a></li>`).join("");
  return simplePage(lang, "diff", { title: h.title, description: h.description, h1: h.h1, lead: h.lead, trail, bodyInner: { sections: `<section class="section" aria-labelledby="list-h"><div class="wrap"><h2 id="list-h">${esc(h.listHeading)}</h2><ul class="city-grid">${list}</ul><ul class="city-grid">${relatedCards(lang, ["converter", "planner"])}</ul></div></section>` }, nav: "world", searchK: "saat farkı time difference istanbul dünya şehirleri" });
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
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>`,
  };
  return simplePage(lang, "converter", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: inner, faq: s.faq, faqHeading: s.faqHeading, scripts: ["planner.js"], clientExtra: { cities: clientCities(lang), t: { conv: s } }, searchK: "saat çevirici time converter dönüştürücü saat dilimi time zone", extraLd: [webAppLd(lang, s.h1, ROUTES[lang].converter, s.description)] });
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
      <noscript><p class="meta">${esc(t.ui.noscript)}</p></noscript>
    </section>
    <p class="meta sync-line" data-sync-status>&nbsp;</p>`,
  };
  return simplePage(lang, "planner", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: inner, faq: s.faq, faqHeading: s.faqHeading, scripts: ["planner.js"], clientExtra: { cities: clientCities(lang), t: { plan: s } }, searchK: "toplantı planlayıcı meeting planner ortak mesai saatleri world meeting", extraLd: [webAppLd(lang, s.h1, ROUTES[lang].planner, s.description)] });
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
  const fixed = ["newYear", "apr23", "may19", "aug30", "oct29"].includes(x.key);
  const names = t.calendar.events;
  const rowsHtml = dates.map((r) => `<tr><th scope="row">${r.y}</th><td>${esc(f.full(r.d))}${x.keys.length > 1 ? ` · ${esc(names[r.k])}` : ""}</td></tr>`).join("");
  const faq = [
    { q: tpl(s.faqWhen, v), a: fixed ? tpl(s.faqWhenFixed, { ...v, date: f.short(dates[0].d) }) : tpl(s.faqWhenYears, { ...v, list: dates.slice(0, 4).map((r) => `${r.y}: ${f.short(r.d)}`).join(" · ") }) },
    { q: s.faqHow, a: s.faqHowA },
  ];
  const others = CDP.filter((o) => o !== x).map((o) => `<li class="city-card"><a href="${cdpPath(lang, o)}"><span class="city-name">${esc(tpl(s.h1, { dat: s.targets[o.key].dat, name: s.targets[o.key].name }))}</span></a></li>`).join("");
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
    <p>${esc(s.customText)} <a href="${ROUTES[lang].countdown}">${esc(s.customLink)} ${ARROW(lang)}</a></p>
    <h2>${esc(s.listHeading)}</h2>
    <ul class="city-grid">${others}</ul>
  </div>
</section>`,
  };
  return simplePage(lang, `cdp:${x.key}`, { title: tpl(s.title, v), description: tpl(s.description, v), h1: tpl(s.h1, v), lead: tpl(s.lead, v), trail, bodyInner: inner, faq, faqHeading: tpl(s.faqHeading, v), scripts: ["calendar.js"], clientExtra: { t: { cdp: s } }, searchK: `geri sayım kaç gün kaldı countdown days until ${tg.name}` });
}

/* ---------- Tarih hesaplayıcılar ve gömme kodu (widget) ---------- */
const ROUTES_C = {
  tr: { datecalc: "/tarih-hesaplama/", embed: "/gomme/", embedFrame: "/embed/tr/" },
  en: { datecalc: "/en/date-calculator/", embed: "/en/embed/", embedFrame: "/embed/en/" },
};
extendRoutes(ROUTES_C);

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
  return simplePage(lang, "datecalc", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: inner, faq: s.faq, faqHeading: s.faqHeading, scripts: ["dates.js"], clientExtra: { t: { dc: s, dcRange: { from: HOL_YEARS[0], to: HOL_YEARS[HOL_YEARS.length - 1] } } }, searchK: "tarih hesaplama iki tarih arası gün iş günü yaş hesaplama date calculator days between working days age", extraLd: [webAppLd(lang, s.h1, ROUTES[lang].datecalc, s.description)] });
}

function buildEmbedGen(lang) {
  const t = I[lang];
  const s = t.embedgen;
  const trail = [{ name: t.ui.home, path: ROUTES[lang].home }, { name: t.ui.tools, path: ROUTES[lang].tools }, { name: s.h1, path: ROUTES[lang].embed }];
  const modelOpts = MODELS.map((m) => `<option value="${m.id}">${esc(t.stage.models[m.id])}</option>`).join("");
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
  return simplePage(lang, "embed", { title: s.title, description: s.description, h1: s.h1, lead: s.lead, trail, bodyInner: inner, faq: s.faq, faqHeading: s.faqHeading, scripts: ["planner.js"], clientExtra: { cities: clientCities(lang), t: { eg: { copied: s.copied, frameTitle: s.frameTitle, path: Object.fromEntries(LANGS.map((l) => [l, ROUTES[l].embedFrame])) } } }, searchK: "widget gömme kodu iframe saat embed clock site", extraLd: [webAppLd(lang, s.h1, ROUTES[lang].embed, s.description)] });
}

/* Gömülü widget sayfası: yalnız seçilen saat modeli, başlık ve alt bilgi (indekslenmez) */
function embedFramePage(lang) {
  const t = I[lang];
  const slides = MODELS.map((m) => `<section class="slide ${m.cls}" data-model="${m.id}" aria-hidden="true">${m.html("")}</section>`).join("");
  const umami = UMAMI_ID ? `<script defer src="https://istatistik.bumba.tr/script.js" data-website-id="${esc(UMAMI_ID)}" data-domains="${esc(HOST)}"></script>` : "";
  const pageCfg = { locale: t.locale, t: { ...t.client }, cities: clientCities(lang), cal: { months: t.calendar.months, weekdays: t.calendar.weekdays, weekdaysShort: t.calendar.weekdaysShort, fullFmt: t.calendar.fullFmt, shortFmt: t.calendar.shortFmt, dateFmt: t.calendar.dateFmt } };
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
${umami}
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

const built = [];
for (const lang of LANGS) {
  built.push(buildHome(lang), buildWorld(lang), buildToolsHub(lang), ...TOOL_KEYS.map((k) => buildTool(lang, k)), buildWeek(lang), buildToday(lang), buildTz(lang), buildConverter(lang), buildPlanner(lang), buildDateCalc(lang), buildEmbedGen(lang), buildPairHub(lang), ...PAIR_CITIES.map((c) => buildPair(lang, c)), ...CDP.map((x) => buildCdp(lang, x)), buildPrivacy(lang), ...CITIES.map((c) => buildCity(lang, c)));
  built.push(...HOL_YEARS.map((y) => buildCalendarYear(lang, y)), buildCalendarYear(lang, BUILD_YEAR, { index: true }));
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
${LANGS.map((l) => `    <xhtml:link rel="alternate" hreflang="${l}" href="${abs(a[l])}"/>`).join("\n")}
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
  const main = mine.filter((p) => !p.key.startsWith("city:") && !p.key.startsWith("calendar:"));
  const years = mine.filter((p) => p.key.startsWith("calendar:"));
  const cities = mine.filter((p) => p.key.startsWith("city:"));
  return `> ${t.llms.summary}\n\n## ${t.llms.pages}\n${[...main, ...years].map((p) => llmsLine(lang, p)).join("\n")}\n\n## ${t.llms.cities}\n${cities.map((p) => llmsLine(lang, p)).join("\n")}\n\n## ${t.llms.about}\n${t.llms.aboutText}\n`;
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

if (!UMAMI_ID) console.warn("UYARI: Umami kimliği boş; sayaç betiği eklenmedi (site.config.json → umamiId ya da UMAMI_ID).");
console.log(`Derlendi: ${built.length} sayfa + 404/500 → dist/ (${ORIGIN})`);
