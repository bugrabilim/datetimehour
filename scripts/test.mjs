// Derleme çıktısı (dist/) için kalite denetimi. Bağımlılık yok: `npm test`
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const DIST = path.join(ROOT, "dist");
const cfg = JSON.parse(fs.readFileSync(path.join(ROOT, "site.config.json"), "utf8"));
const ORIGIN = (process.env.SITE_ORIGIN || cfg.origin).replace(/\/+$/, "");
const LANGS = ["tr", "en", "de", "az", "ar"].filter((l) => fs.existsSync(path.join(ROOT, "src/i18n", `${l}.json`)));
const LG = LANGS.join("|");
const fails = [];
const check = (ok, msg) => { if (!ok) fails.push(msg); };

const walk = (d) => fs.readdirSync(d, { withFileTypes: true }).flatMap((e) => (e.isDirectory() ? walk(path.join(d, e.name)) : [path.join(d, e.name)]));
const files = walk(DIST);
const htmlFiles = files.filter((f) => f.endsWith(".html"));
const rel = (f) => "/" + path.relative(DIST, f).split(path.sep).join("/");
const urlToFile = (u) => {
  const p = u.split("#")[0].split("?")[0];
  let f = path.join(DIST, decodeURIComponent(p));
  if (p.endsWith("/")) f = path.join(f, "index.html");
  return fs.existsSync(f) ? f : null;
};
const ids = {};
const pagesInfo = [];

for (const f of htmlFiles) {
  const h = fs.readFileSync(f, "utf8");
  const name = rel(f);
  const err = name === "/404.html" || name === "/500.html";
  const noidx = /<meta name="robots" content="noindex/.test(h);
  const embed = new RegExp(`^/embed/(${LG})/index\\.html$`).test(name);
  ids[f] = new Set([...h.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]));
  const title = (h.match(/<title>([^<]*)<\/title>/) || [])[1];
  const desc = (h.match(/<meta name="description" content="([^"]*)"/) || [])[1];
  check(title, `${name}: title yok`);
  check(desc || embed, `${name}: description yok`);
  check((h.match(/<h1[ >]/g) || []).length === (embed ? 0 : err ? LANGS.length : 1), `${name}: H1 sayısı hatalı`);
  check(new RegExp(`<html lang="(${LG})" dir="(ltr|rtl)"`).test(h), `${name}: html lang/dir yok`);
  const hl = (h.match(/<html lang="(\w+)" dir="(\w+)"/) || []);
  check(hl[2] === (hl[1] === "ar" ? "rtl" : "ltr"), `${name}: dir değeri hatalı`);
  check(!/\sstyle="/.test(h), `${name}: satır içi style (CSP)`);
  check(!/http:\/\/(?!www\.w3\.org)/.test(h), `${name}: http:// bağlantı`);
  check(/class="rozet"/.test(h) && /width="164" height="28"/.test(h), `${name}: Bumba rozeti yok`);
  check(embed || (/data-theme-toggle/.test(h) && /data-search-open/.test(h)), `${name}: tema ya da arama düğmesi yok`);
  check(/Content-Security-Policy/.test(h), `${name}: CSP yok`);
  if (embed) {
    check(noidx && h.indexOf("embed.js") > 0 && h.indexOf("embed.js") < h.indexOf("main.js") && (h.match(/class="slide /g) || []).length >= 10, `${name}: gömülü sayfa yapısı hatalı`);
  }
  if (!err && !embed && (name === "/index.html" || new RegExp(`^/(${LG})/index\\.html$`).test(name) || /\/(dunya-saatleri|world-clock)\/[a-z-]+\/index\.html$/.test(name) && !/\/(dunya-saatleri|world-clock)\/index\.html$/.test(name))) {
    const slides = [...h.matchAll(/data-model="([a-z-]+)" data-name="([^"]+)"/g)];
    check(slides.length >= 10, `${name}: saat modeli sayısı ${slides.length} < 10`);
    check(new Set(slides.map((m) => m[1])).size === slides.length, `${name}: tekrarlanan model kimliği`);
    check((h.match(/data-dot="\d+"/g) || []).length === slides.length, `${name}: nokta sayısı model sayısıyla uyuşmuyor`);
    check(/data-fullscreen/.test(h) && /data-fullscreen-exit/.test(h) && /data-fs-bar/.test(h) && (h.match(/data-color="/g) || []).length >= 10 && /data-tick/.test(h) && !/data-nav="-1"/.test(h) && !/stage-hint/.test(h) && /data-fmt-toggle/.test(h) && !/data-pref="sec"/.test(h) && /data-stage-count/.test(h) && !/class="stage-bar"[\s\S]*?<\/section>/.exec(h)[0].includes("data-tick") && /data-nav-toggle/.test(h), `${name}: karusel düğmeleri eksik`);
    check(!/data-pref="sync"/.test(h) && /data-sync-status/.test(h) && /class="meta source-line"/.test(h), `${name}: senkron düğmesi/durumu eksik`);
    check(/data-live="calendar"/.test(h), `${name}: özel model türleri eksik`);
  }
  if (!err && !embed && (name === "/index.html" || new RegExp(`^/(${LG})/index\\.html$`).test(name))) {
    check(/data-globe/.test(h) && /globe\.js/.test(h) && /"globe":\{"earth":"\/assets\/earth\.jpg\?v=/.test(h) && /"places":"\/assets\/places\.json/.test(h) && /data-globe-attr/.test(h) && /data-globe-zoom="in"/.test(h), `${name}: dünya küresi eksik`);
    check(!/data-upcoming/.test(h) && !/id="facts"/.test(h) && !/data-world="grid"/.test(h) && !/<details>/.test(h) && !/"FAQPage"/.test(h), `${name}: ana sayfada kaldırılan bölüm duruyor`);
    check(/<h1 class="sr-only">/.test(h), `${name}: ana sayfa H1 gizli olmalı`);
  }
  if (/\/(sss|faq)\/index\.html$/.test(name)) {
    check(/"FAQPage"/.test(h) && (h.match(/class="section faq-group"/g) || []).length >= 15 && (h.match(/<details>/g) || []).length >= 100, `${name}: SSS sayfası eksik`);
  }
  check(!/(bugun-ayin-kaci|todays-date)\/index\.html$/.test(name), `${name}: kaldırılan Bugün sayfası geri geldi`);
  if (!err && !embed && (name === "/index.html" || new RegExp(`^/(${LG})/index\\.html$`).test(name))) check(/id="today-q"/.test(h) && /class="sr-only" aria-labelledby="today-q"/.test(h), `${name}: gizli "bugün ayın kaçı" bölümü eksik`);
  if (!err && !embed) {
    check(!/class="crumbs"/.test(h), `${name}: görünür ekmek kırıntısı bağlantısı olmamalı`);
    check(/class="top-date" data-live="date"/.test(h) && /data-place-open/.test(h) && /class="settings-menu"/.test(h) && /id="place-dialog"/.test(h), `${name}: üst bar (tarih, konum, ayarlar) eksik`);
    check(!/<p class="lead"[^>]*>/.test(h.replace(/<p class="lead sr-only"/g, "")), `${name}: başlık altı açıklama görünür durmamalı`);
  }
  if (/\/(takvim|calendar)\/\d{4}\/index\.html$/.test(name)) {
    check(!/data-upcoming/.test(h) && !/id="day-detail"/.test(h) && !/d-exam/.test(h) && /id="day-dialog"/.test(h), `${name}: takvimde kaldırılan bölüm duruyor ya da gün penceresi yok`);
    check((h.match(/<table class="ymonth"/g) || []).length === 12, `${name}: 12 ay tablosu yok`);
    check(/class="events-table"/.test(h) && /d-holiday/.test(h) && /d-half/.test(h) && !/faq-more/.test(h) && !/"FAQPage"/.test(h), `${name}: takvim içeriği eksik`);
    check(/data-year="\d{4}"/.test(h) && /calendar\.js/.test(h), `${name}: takvim betiği/yılı eksik`);
  }
  if (/\/(saat-farki|time-difference)\/istanbul-[a-z-]+\/index\.html$/.test(name)) {
    check(/data-pair-table/.test(h) && (h.match(/data-live="time"/g) || []).length === 2 && /data-ref="Europe\/Istanbul"/.test(h) && !/faq-more/.test(h) && !/"FAQPage"/.test(h), `${name}: saat farkı sayfası eksik`);
  }
  if (/\/(saat-cevirici|time-converter)\/index\.html$/.test(name)) check(/data-converter/.test(h) && /data-cv-from/.test(h) && /"cities"/.test(h), `${name}: çevirici eksik`);
  if (/\/(toplanti-planlayici|meeting-planner)\/index\.html$/.test(name)) check(/data-planner/.test(h) && /data-pl-table/.test(h) && /"cities"/.test(h), `${name}: planlayıcı eksik`);
  if (/\/(geri-sayim|countdown)\/[a-z0-9-]+\/index\.html$/.test(name)) check(/data-cdp-keys/.test(h) && !/faq-more/.test(h) && !/"FAQPage"/.test(h) && /events-table/.test(h), `${name}: geri sayım sayfası eksik`);
  if (/\/(namaz-vakitleri|prayer-times)\/([a-z]+\/)?index\.html$/.test(name)) {
    const hub = /\/(namaz-vakitleri|prayer-times)\/index\.html$/.test(name);
    check(/data-prayer\b/.test(h) && (h.match(/data-pr="/g) || []).length === 6 && /prayer-calc\.js/.test(h) && /prayer\.js/.test(h) && /data-pr-month/.test(h) && /data-imsakiye/.test(h) && /imsakiye\.js/.test(h) && !/class="tool prayer"/.test(h) && !/pr-method-h/.test(h) && !/faq-more/.test(h) && !/"FAQPage"/.test(h), `${name}: namaz vakitleri yapısı eksik`);
    if (hub) check((h.match(/<option value="[a-z]+" data-lat=/g) || []).length === 81 && /data-place-geo/.test(h), `${name}: hub il listesi/konum düğmesi eksik`);
    else check(/data-mode="city"/.test(h) && /data-lat="[\d.]+" data-lon="[\d.]+"/.test(h), `${name}: il sayfası verisi eksik`);
  }
  if (/\/(mevsimler|seasons)\/index\.html$/.test(name)) {
    check(/data-wheel/.test(h) && /data-n-chart/.test(h) && /data-moon/.test(h) && (h.match(/class="season-card /g) || []).length === 4 && /nature\.js/.test(h) && /nature-calc\.js/.test(h) && /data-n-turns/.test(h) && !/data-place-city="/.test(h.replace(/<dialog[\s\S]*?<\/dialog>/g, "")) && !/faq-more/.test(h) && !/"FAQPage"/.test(h), `${name}: doğa sayfası yapısı eksik`);
    check(h.indexOf("<h1") < h.indexOf("data-nature"), `${name}: başlık çarkın üstünde olmalı`);
  }
  if (/\/(hava-durumu|weather)\/index\.html$/.test(name)) {
    check(/data-weather\b/.test(h) && /data-wx-hourly/.test(h) && /data-wx-daily/.test(h) && /weather-core\.js/.test(h) && /weather\.js/.test(h) && /Open-Meteo\.com/.test(h) && /data-place-geo/.test(h), `${name}: hava durumu sayfası yapısı eksik`);
  }
  if (/\/(araclar|tools)\/index\.html$/.test(name)) check((h.match(/class="tool-card"/g) || []).length >= 10 && !/card-open/.test(h) && !/\/(takvim|calendar)\/\d{4}\//.test(h.slice(h.indexOf('class="tool-cards"'), h.indexOf("</main>"))) && !/class="tool-desc"/.test(h), `${name}: araçlar kartları (görsel + ad) eksik`);
  if (/\/(dunya-saatleri|world-clock)\/index\.html$/.test(name)) check(/data-world="line"/.test(h) && /world\.js/.test(h) && !/data-fmt-toggle/.test(h) && !/data-pref="sec"/.test(h) && !/ankara|izmir/i.test(h.slice(h.indexOf('data-world="line"'), h.indexOf("</main>"))), `${name}: zaman çizgisi sayfası eksik ya da gizlenen şehir duruyor`);
  const toolMatch = /\/(kronometre|geri-sayim|alarm|pomodoro|stopwatch|countdown)\/index\.html$/.exec(name);
  if (toolMatch) {
    check(/data-tool="(stopwatch|countdown|alarm|pomodoro)"/.test(h) && /tools\.js/.test(h) && /data-fs/.test(h) && /"WebApplication"/.test(h) && !/faq-more/.test(h) && !/"FAQPage"/.test(h), `${name}: araç yapısı eksik`);
  }
  if (!err && !embed) {
    check(/rel="canonical" href="https:\/\//.test(h), `${name}: canonical yok`);
    if (!noidx) {
      for (const l of [...LANGS, "x-default"]) check(h.includes(`hreflang="${l}" href="${ORIGIN}`), `${name}: hreflang ${l} yok`);
      check(/og:image/.test(h) && /twitter:card/.test(h), `${name}: OG/Twitter yok`);
    } else {
      check(/rel="alternate" hreflang/.test(h) === false && /rel="canonical" href="https:\/\/[^"]+\/\d{4}\/"/.test(h), `${name}: noindex giriş sayfası yıl sayfasına canonical vermeli`);
    }
    check(/action="https:\/\/bumbagroup\.com\/api\/liste\/katil"/.test(h) && /name="riza" value="on" required/.test(h) && !/name="riza"[^>]*checked/.test(h), `${name}: liste formu hatalı`);
    check(/name="site" value="[a-z-]+"/.test(h) && /name="web_sitesi"/.test(h), `${name}: liste formu alanları eksik`);
    check(/name="dil" value="(tr|en|de|az|ar)"/.test(h), `${name}: liste dil alanı yok`);
    check(/datetime="\d{4}-\d{2}-\d{2}"/.test(h), `${name}: güncelleme tarihi yok`);
    const ld = [...h.matchAll(/<script type="application\/ld\+json">(.*?)<\/script>/gs)];
    check(ld.length === 1, `${name}: JSON-LD yok`);
    for (const m of ld) {
      try {
        const j = JSON.parse(m[1]);
        const org = j["@graph"].find((x) => x["@type"] === "Organization");
        check(org && org.parentOrganization && org.parentOrganization.url === "https://bumbagroup.com", `${name}: parentOrganization yok`);
      } catch (e) { fails.push(`${name}: JSON-LD bozuk`); }
    }
    if (!noidx) pagesInfo.push({ name, title, desc });
  } else if (err) {
    check(/noindex/.test(h), `${name}: noindex yok`);
  }
  if (UMAMI_ID_REQUIRED()) check(/data-website-id="[0-9a-f-]{36}"/.test(h), `${name}: Umami yok`);
}
function UMAMI_ID_REQUIRED() { return process.env.REQUIRE_UMAMI === "1"; }

// başlık ve açıklama benzersizliği
for (const k of ["title", "desc"]) {
  const seen = new Map();
  for (const p of pagesInfo) {
    if (seen.has(p[k])) fails.push(`${p.name}: ${k} tekrar (${seen.get(p[k])})`);
    seen.set(p[k], p.name);
  }
}
for (const p of pagesInfo) {
  check(p.title.length <= 70, `${p.name}: title uzun (${p.title.length})`);
  check(p.desc.length <= 170, `${p.name}: description uzun (${p.desc.length})`);
}

// iç bağlantılar, anchor'lar, görseller
for (const f of htmlFiles) {
  const h = fs.readFileSync(f, "utf8");
  for (const m of h.matchAll(/(?:href|src)="([^"]+)"/g)) {
    let u = m[1];
    if (/^(https?:|mailto:|data:)/.test(u)) {
      if (u.startsWith(ORIGIN)) u = u.slice(ORIGIN.length) || "/"; else continue;
    }
    if (u.startsWith("#")) { check(ids[f].has(u.slice(1)), `${rel(f)}: anchor ${u} yok`); continue; }
    const target = urlToFile(u);
    check(target, `${rel(f)}: kırık bağlantı ${u}`);
    if (target && u.includes("#") && target.endsWith(".html")) check(ids[target].has(u.split("#")[1]), `${rel(f)}: ${u} anchor yok`);
  }
}

// sitemap, robots, llms.txt
const sm = fs.readFileSync(path.join(DIST, "sitemap.xml"), "utf8");
check((sm.match(/<loc>/g) || []).length === pagesInfo.length, "sitemap sayfa sayısı uyuşmuyor");
const robots = fs.readFileSync(path.join(DIST, "robots.txt"), "utf8");
check(robots.includes(`Sitemap: ${ORIGIN}/sitemap.xml`), "robots.txt Sitemap satırı yok");
check(!/Disallow:\s*\//.test(robots), "robots.txt engelliyor");
for (const b of ["GPTBot", "ClaudeBot", "PerplexityBot", "OAI-SearchBot"]) check(robots.includes(`User-agent: ${b}`), `robots.txt ${b} yok`);
check(/Saat Tarih/.test(fs.readFileSync(path.join(DIST, "llms.txt"), "utf8")), "llms.txt boş");

// Dil dosyaları: anahtar ve dizi uzunluğu eşliği (tr = kaynak)
const keys = (o, p = "") => Object.entries(o).flatMap(([k, v]) => (v && typeof v === "object" && !Array.isArray(v) ? keys(v, p + k + ".") : [p + k]));
const flat = (o, p = "") => Object.entries(o).flatMap(([k, v]) => (v && typeof v === "object" ? (Array.isArray(v) ? [[p + k, v]] : flat(v, p + k + ".")) : [[p + k, v]]));
const dict = Object.fromEntries(LANGS.map((l) => [l, new Map(flat(JSON.parse(fs.readFileSync(path.join(ROOT, "src/i18n", `${l}.json`), "utf8"))))]));
for (const l of LANGS.filter((x) => x !== "tr")) {
  for (const k of dict.tr.keys()) {
    check(dict[l].has(k), `${l}.json'da yok: ${k}`);
    const a = dict.tr.get(k), b = dict[l].get(k);
    if (Array.isArray(a) && Array.isArray(b)) check(a.length === b.length, `${l}.json: dizi uzunluğu farklı: ${k}`);
  }
  for (const k of dict[l].keys()) check(dict.tr.has(k) || /^client\.words\.(hours|one)$/.test(k), `tr.json'da yok: ${k} (${l})`);
}
for (const l of LANGS) {
  const evs = JSON.parse(fs.readFileSync(path.join(DIST, "assets", `events-${l}.json`), "utf8"));
  check(evs.length > 100 && evs.every((e) => e.n && /^\d{4}-\d\d-\d\d$/.test(e.d)), `events-${l}.json: adsız ya da hatalı etkinlik`);
  check(evs.some((e) => e.t === "exam"), `events-${l}.json: sınav etkinliği yok`);
}
for (const l of LANGS) check(fs.existsSync(path.join(DIST, "assets", `search-${l}.json`)), `search-${l}.json yok`);
check(sm.split("<url>").slice(1).every((u) => LANGS.every((l) => u.includes(`hreflang="${l}"`))), "sitemap: bir adreste hreflang eksik");

// namaz vakti hesabı: İstanbul 9 Ekim 2026, Diyanet'in yayımladığı vakitlerle (±1 dk)
{
  const m = { exports: {} };
  new Function("module", "window", fs.readFileSync(path.join(ROOT, "src/js/prayer-calc.js"), "utf8"))(m, undefined);
  const r = m.exports.times(2026, 10, 9, 41.01, 28.98, 3);
  const want = { fajr: "05:37", sunrise: "07:02", dhuhr: "12:57", asr: "16:07", maghrib: "18:41", isha: "20:00" };
  for (const [k, v] of Object.entries(want)) {
    const [hh, mm] = v.split(":").map(Number);
    check(Math.abs(Math.round(r[k] * 60) - (hh * 60 + mm)) <= 1, `namaz hesabı: ${k} ${(r[k]).toFixed(3)} ≠ ${v}`);
  }
  const hl = m.exports.times(2027, 6, 21, 64.1, -21.9, 0); // Reykjavik: imsak/yatsı açısı oluşmaz, yedide bir kuralı
  check(Object.values(hl).every((x) => Number.isFinite(x)), "namaz hesabı: yüksek enlemde NaN");
}

// doğa hesapları: ekinoks/gündönümü (±2 dk) ve yeni ay/dolunay (±10 dk)
{
  const m = { exports: {} };
  new Function("module", "window", fs.readFileSync(path.join(ROOT, "src/js/nature-calc.js"), "utf8"))(m, undefined);
  const N = m.exports;
  const want = { 2025: ["2025-03-20T09:01", "2025-06-21T02:42", "2025-09-22T18:19", "2025-12-21T15:03"], 2026: ["2026-03-20T14:46", "2026-06-21T08:24", "2026-09-23T00:06", "2026-12-21T20:50"] };
  for (const [y, list] of Object.entries(want)) {
    const q = N.seasons(+y);
    [q.march, q.june, q.sept, q.dec].forEach((ms, i) => check(Math.abs(ms - Date.parse(list[i] + ":00Z")) <= 2 * 60000, `mevsim anı ${y}/${i}: ${new Date(ms).toISOString()} ≠ ${list[i]}`));
  }
  const k = (ms) => Math.floor((ms / 86400000 + 2440587.5 - 2451550.09766) / 29.530588861);
  const anchors = [["new", 2026, 7, 12, "2026-08-12T17:37"], ["full", 2026, 2, 3, "2026-03-03T11:38"], ["new", 2026, 1, 17, "2026-02-17T12:01"]];
  for (const [kind, y, mo, d, iso] of anchors) {
    const base = k(Date.UTC(y, mo, d)), got = [0, 1].map((o) => N.phaseMs(base + o, kind === "full"));
    check(got.some((ms) => Math.abs(ms - Date.parse(iso + ":00Z")) <= 10 * 60000), `ay anı ${kind} ${iso}: ${got.map((x) => new Date(x).toISOString()).join(" ")}`);
  }
}

// kontrast (WCAG AA)
const css = fs.readFileSync(path.join(ROOT, "src/styles.css"), "utf8");
const block = (re) => Object.fromEntries([...css.match(re)[1].matchAll(/--([\w-]+):\s*(#[0-9a-f]{6})/g)].map((m) => [m[1], m[2]]));
const lum = (hex) => {
  const c = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255).map((v) => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
};
const ratio = (a, b) => { const [x, y] = [lum(a), lum(b)].sort((p, q) => q - p); return (x + 0.05) / (y + 0.05); };
const themes = { açık: block(/:root \{([^}]*)\}/), koyu: block(/:root\[data-theme="dark"\] \{([^}]*)\}/) };
for (const [n, t] of Object.entries(themes)) {
  for (const [fg, bg, min] of [["text", "bg", 4.5], ["text", "surface", 4.5], ["muted", "bg", 4.5], ["muted", "surface", 4.5], ["muted", "surface-2", 4.5], ["accent", "bg", 4.5], ["accent", "surface", 4.5], ["accent-ink", "accent", 4.5], ["ok", "ok-bg", 4.5], ["err", "err-bg", 4.5], ["text", "accent-soft", 4.5], ["focus", "bg", 3], ["muted", "bg", 3], ["text", "ramadan", 4.5], ["muted", "ramadan", 4.5], ["accent", "ramadan", 3]]) {
    const r = ratio(t[fg], t[bg]);
    check(r >= min, `kontrast ${n}: ${fg}/${bg} = ${r.toFixed(2)} < ${min}`);
  }
}

// tam ekran renk paletleri: okunurluk
for (const m of css.matchAll(/\.stage\.fs-on\[data-fs-color="(\w+)"\] \{([^}]*)\}/g)) {
  const v = Object.fromEntries([...m[2].matchAll(/--([\w-]+):\s*(#[0-9a-f]{6})/g)].map((x) => [x[1], x[2]]));
  check(ratio(v.text, v.bg) >= 4.5 && ratio(v.muted, v.bg) >= 3 && ratio(v.accent, v.bg) >= 3, `palet ${m[1]}: kontrast yetersiz (${ratio(v.text, v.bg).toFixed(1)}/${ratio(v.muted, v.bg).toFixed(1)}/${ratio(v.accent, v.bg).toFixed(1)})`);
}
check((css.match(/\.stage\.fs-on\[data-fs-color=/g) || []).length >= 12, "en az 12 tam ekran paleti olmalı");

// nginx başlıkları
const ng = fs.readFileSync(path.join(ROOT, "nginx.conf"), "utf8");
for (const s of ["Strict-Transport-Security", "max-age=86400", "X-Content-Type-Options", "Referrer-Policy", "Content-Security-Policy", "https://istatistik.bumba.tr", "https://bumbagroup.com", "error_page 404", "error_page 500"]) check(ng.includes(s), `nginx.conf: ${s} yok`);
check(/location \^~ \/embed\//.test(ng) && /frame-ancestors \*/.test(ng), "nginx.conf: /embed/ için frame-ancestors * yok");
check(ng.split("https://tile.openstreetmap.org").length === 3, "nginx.conf: img-src OpenStreetMap karoları iki konumda olmalı");
check(ng.split("https://api.open-meteo.com").length === 3, "nginx.conf: connect-src open-meteo iki konumda olmalı");
check(!/includeSubDomains|preload/.test(ng), "nginx.conf: includeSubDomains/preload kullanılmamalı");

if (fails.length) {
  console.error(`${fails.length} sorun:\n- ` + fails.join("\n- "));
  process.exit(1);
}
console.log(`Tamam: ${htmlFiles.length} HTML dosyası, ${pagesInfo.length} indekslenebilir sayfa, kontroller geçti.`);
