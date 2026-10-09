// Saat Tarih statik site derleyicisi. Bağımlılık yok: `node build.mjs` → dist/
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const ROOT = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.join(ROOT, "src");
const OUT = path.join(ROOT, "dist");

const readJson = (p) => JSON.parse(fs.readFileSync(path.join(ROOT, p), "utf8"));
const cfg = readJson("site.config.json");
const ORIGIN = (process.env.SITE_ORIGIN || cfg.origin).replace(/\/+$/, "");
const HOST = new URL(ORIGIN).host;
const UMAMI_ID = process.env.UMAMI_ID ?? cfg.umamiId ?? "";
const LANGS = ["tr", "en"];
const I = { tr: readJson("src/i18n/tr.json"), en: readJson("src/i18n/en.json") };
const CITIES = readJson("src/data/cities.json");
const OTHER = { tr: "en", en: "tr" };

const arm = cfg.arm;
const BADGE_W = { games: 185, suites: 177, life: 164, ventures: 205 }[arm];

const MONTHS = {
  tr: ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"],
  en: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
};
function updatedText(lang) {
  const [y, m, d] = cfg.updated.split("-").map(Number);
  return `${d} ${MONTHS[lang][m - 1]} ${y}`;
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
  tr: { home: "/", world: "/dunya-saatleri/", privacy: "/gizlilik/" },
  en: { home: "/en/", world: "/en/world-clock/", privacy: "/en/privacy/" },
};
const cityPath = (lang, c) => `${ROUTES[lang].world}${c[lang].slug}/`;

/* ---------- varlıklar (sürüm damgalı) ---------- */
fs.rmSync(OUT, { recursive: true, force: true });
const ASSETS = {};
for (const [name, from] of [
  ["styles.css", "styles.css"],
  ["theme.js", "js/theme.js"],
  ["main.js", "js/main.js"],
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
      <button type="button" class="icon-btn" data-fullscreen aria-pressed="false" aria-label="${esc(t.client.fullscreen)}" title="${esc(t.client.fullscreen)}">${ICON_FULL}</button>
    </div>
  </div>
  <noscript><p class="meta stage-note">${esc(t.ui.noscript)}</p></noscript>
</section>
<p class="meta stage-hint">${esc(s.hint)}</p>`;
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
    <input type="hidden" name="dil" value="${lang}">
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
  const canonical = p.path ? abs(p.path) : "";
  const alt = p.alt; // { tr: path, en: path }
  const alternates = alt
    ? `<link rel="alternate" hreflang="tr" href="${abs(alt.tr)}">\n<link rel="alternate" hreflang="en" href="${abs(alt.en)}">\n<link rel="alternate" hreflang="x-default" href="${abs(alt.tr)}">`
    : "";
  const clientT = {
    ...t.client,
    searchHint: u.searchHint,
    searchEmpty: u.searchEmpty,
    searchCount: u.searchCount,
    searchError: u.searchError,
  };
  const pageCfg = { locale: t.locale, t: clientT, searchIndex: `/assets/search-${lang}.json?v=${p.searchVersion}` };
  const og = p.noindex
    ? ""
    : `<meta property="og:type" content="website">
<meta property="og:site_name" content="${esc(cfg.name)}">
<meta property="og:title" content="${esc(p.title)}">
<meta property="og:description" content="${esc(p.description)}">
<meta property="og:url" content="${canonical}">
<meta property="og:locale" content="${t.ogLocale}">
<meta property="og:locale:alternate" content="${I[OTHER[lang]].ogLocale}">
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
    `<a href="${ROUTES[lang][key]}"${p.nav === key ? ' aria-current="page"' : ""}>${esc(label)}</a>`;
  const langSwitch = isError
    ? `<a class="lang-link" href="/" hreflang="tr" lang="tr">TR</a><a class="lang-link" href="/en/" hreflang="en" lang="en">EN</a>`
    : `<a class="lang-link" href="${alt[OTHER[lang]]}" hreflang="${OTHER[lang]}" lang="${OTHER[lang]}" data-lang-switch aria-label="${esc(u.langSwitchLabel)}">${esc(u.langSwitch)}</a>`;

  return `<!doctype html>
<html lang="${lang}"${p.home ? ' data-home="1"' : ""}>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(p.title)}</title>
<meta name="description" content="${esc(p.description)}">
${canonical ? `<link rel="canonical" href="${canonical}">` : ""}
${alternates}
<meta name="robots" content="${p.noindex ? "noindex,nofollow" : "index,follow,max-image-preview:large,max-snippet:-1"}">
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
    <nav class="nav" aria-label="${esc(u.mainNav)}">${navLink("home", u.home)}${navLink("world", u.world)}</nav>
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
        <li><a href="${ROUTES[lang].privacy}">${esc(u.privacy)}</a></li>
      </ul>
    </nav>
  </div>
</footer>
${searchDialog(lang)}
<script type="application/json" id="page-config">${JSON.stringify(pageCfg).replace(/</g, "\\u003c")}</script>
<script defer src="${ASSETS["main.js"]}"></script>
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
      inLanguage: ["tr", "en"],
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
const searchEntries = { tr: [], en: [] };

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
<section class="section" id="world" aria-labelledby="world-h">
  <div class="wrap">
    <h2 id="world-h">${esc(h.worldHeading)}</h2>
    <p class="meta">${esc(h.worldLead)}</p>
    <ul class="city-grid">${featured.map((c) => cityCard(lang, c)).join("")}</ul>
    <p><a href="${ROUTES[lang].world}">${esc(h.worldAll)} →</a></p>
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
  e.push({ t: t.stage.label, u: pagePath + "#model-classic", d: t.stage.hint, k: "saat model stil style clock tam ekran full screen fullscreen" });
  MODELS.forEach((m) => e.push({ t: t.stage.models[m.id], u: `${pagePath}#model-${m.id}`, d: t.stage.label, k: "saat model clock style" }));
  e.push({ t: h.factsHeading, u: pagePath + "#facts", d: `${h.factWeek}, ${h.factDoy}, ${h.factLeft}, ${h.factUnix}, ${h.factUtc}`, k: "hafta week unix utc gün day yıl year" });
  h.faq.forEach((f) => e.push({ t: f.q, u: pagePath + "#faq", d: f.a, k: "sss faq" }));
  e.push({ t: t.list.heading, u: pagePath + "#liste", d: t.list.lead, k: "e-posta email bülten newsletter liste list" });
  return { lang, key: "home", path: pagePath, title: h.title, description: h.description, body, ld, home: true, nav: "home" };
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
    </dl>
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
    <p><a href="${ROUTES[lang].world}">${esc(t.home.worldAll)} →</a></p>
  </div>
</section>`;
  const ld = [...baseLd(lang), webPageLd(lang, pagePath, title, description), breadcrumbLd(trail), faqLd(faq)];
  searchEntries[lang].push({
    t: n.name,
    u: pagePath,
    d: description,
    k: [n.name, n.loc, n.country, c.tz, I.tr.ui.world, I.en.ui.world, "saat kaç time"].filter(Boolean).join(" "),
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

const built = [];
for (const lang of LANGS) {
  built.push(buildHome(lang), buildWorld(lang), buildPrivacy(lang), ...CITIES.map((c) => buildCity(lang, c)));
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

/* ---------- hata sayfaları (iki dil bir arada) ---------- */
function errorPage(kind) {
  const trs = I.tr[kind], ens = I.en[kind];
  const block = (lang, s) => `<section class="center-page" lang="${lang}">
  <div class="wrap">
    <h1>${esc(s.h1)}</h1>
    <p class="lead">${esc(s.p)}</p>
    <p><a class="btn" href="${ROUTES[lang].home}">${esc(s.cta)}</a></p>
  </div>
</section>`;
  return layout({
    lang: "tr",
    title: `${trs.title.split(" | ")[0]} / ${ens.title}`,
    description: trs.h1,
    body: block("tr", trs) + block("en", ens),
    noindex: true,
    error: true,
    path: "",
    searchVersion: searchVersion.tr,
  });
}
write("404.html", errorPage("notFound"));
write("500.html", errorPage("serverError"));

/* ---------- sitemap, robots, llms.txt, manifest ---------- */
const indexable = built;
const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
${indexable
  .map((p) => {
    const a = byKey[p.key];
    return `  <url>
    <loc>${abs(p.path)}</loc>
    <lastmod>${cfg.updated}</lastmod>
    <xhtml:link rel="alternate" hreflang="tr" href="${abs(a.tr)}"/>
    <xhtml:link rel="alternate" hreflang="en" href="${abs(a.en)}"/>
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
  const mine = built.filter((p) => p.lang === lang);
  const main = mine.filter((p) => !p.key.startsWith("city:"));
  const cities = mine.filter((p) => p.key.startsWith("city:"));
  return `> ${t.llms.summary}\n\n## ${t.llms.pages}\n${main.map((p) => llmsLine(lang, p)).join("\n")}\n\n## ${t.llms.cities}\n${cities.map((p) => llmsLine(lang, p)).join("\n")}\n\n## ${t.llms.about}\n${t.llms.aboutText}\n`;
};
write("llms.txt", `# ${cfg.name}\n\n${llmsBlock("tr")}\n---\n\n${llmsBlock("en")}`);

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
