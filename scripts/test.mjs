// Derleme çıktısı (dist/) için kalite denetimi. Bağımlılık yok: `npm test`
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { CALENDARS, calendarData, calendarsJson, loadCalTexts, runCalendarTests, validateCalendars } from "../lib/calendars/index.mjs";

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const DIST = path.join(ROOT, "dist");
const cfg = JSON.parse(fs.readFileSync(path.join(ROOT, "site.config.json"), "utf8"));
const ORIGIN = (process.env.SITE_ORIGIN || cfg.origin).replace(/\/+$/, "");
const ALL = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th", "es", "pt", "hi", "bn"];
const RTL = ["ar", "ur", "fa"];
const LANGS = ALL.filter((l) => fs.existsSync(path.join(ROOT, "src/i18n", `${l}.json`)));
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
  check(hl[2] === (RTL.includes(hl[1]) ? "rtl" : "ltr"), `${name}: dir değeri hatalı`);
  check(!/\sstyle="/.test(h), `${name}: satır içi style (CSP)`);
  check(!/http:\/\/(?!www\.w3\.org)/.test(h), `${name}: http:// bağlantı`);
  check(/class="rozet"/.test(h) && /width="164" height="28"/.test(h), `${name}: Bumba rozeti yok`);
  check(embed || (/data-theme-toggle/.test(h) && /data-search-open/.test(h)), `${name}: tema ya da arama düğmesi yok`);
  // madde 14 ve 9: çerez bandı; Umami betiği HTML'de yok (consent.js istatistik izniyle ekler)
  check(!/istatistik\.bumba\.tr\/script\.js/.test(h), `${name}: Umami betiği izinsiz yükleniyor`);
  if (!embed) {
    check(/id="cookie-bar"[^>]* hidden/.test(h) && /data-consent-accept/.test(h) && /data-consent-reject/.test(h) && /data-consent-open/.test(h) && /bumbagroup\.com\/cerez-politikasi/.test(h) && /id="cookie-dialog"/.test(h) && /data-consent-stats/.test(h) && /data-consent-ext/.test(h), `${name}: çerez bandı ya da tercihler eksik`);
    const ci = h.indexOf("consent.js"), mi = h.indexOf("/assets/main.js");
    check(ci > 0 && ci < mi && !/badges\.js|data-badge/.test(h), `${name}: consent.js/main.js sırası hatalı ya da web'de rozet kalmış`);
    // Bu siteye özel (30 Ekim 2026, sahibin kararı): gizlilik ve çerez tercihleri altbilgide değil ayarlarda (masaüstü dişli + mobil menü)
    const pops = [...h.matchAll(/class="set-item" href="([^"]+)">/g)].map((m) => m[1]);
    check((h.match(/class="set-item" data-consent-open/g) || []).length === 2 && pops.some((u) => /\/(gizlilik|privacy)\/$/.test(u)) && !/footer-links/.test(h), `${name}: ayarlarda gizlilik/çerez tercihleri yok ya da altbilgide bağlantı satırı kalmış`);
  }
  check(/Content-Security-Policy/.test(h), `${name}: CSP yok`);
  if (embed) {
    check(noidx && h.indexOf("embed.js") > 0 && h.indexOf("embed.js") < h.indexOf("main.js") && (h.match(/class="slide /g) || []).length >= 10, `${name}: gömülü sayfa yapısı hatalı`);
  }
  if (!err && !embed && (name === "/index.html" || new RegExp(`^/(${LG})/index\\.html$`).test(name) || /\/(dunya-saatleri|world-clock)\/[a-z-]+\/index\.html$/.test(name) && !/\/(dunya-saatleri|world-clock)\/index\.html$/.test(name))) {
    const slides = [...h.matchAll(/data-model="([a-z-]+)" data-name="([^"]+)"/g)];
    check(slides.length >= 10, `${name}: saat modeli sayısı ${slides.length} < 10`);
    check(new Set(slides.map((m) => m[1])).size === slides.length, `${name}: tekrarlanan model kimliği`);
    check((h.match(/data-dot="\d+"/g) || []).length === slides.length, `${name}: nokta sayısı model sayısıyla uyuşmuyor`);
    check(/data-fullscreen/.test(h) && /data-fullscreen-exit/.test(h) && /data-fs-bar/.test(h) && !/data-color="/.test(h) && !/data-tick/.test(h) && !/data-fs-weather/.test(h) && /data-nav="-1"/.test(h) && /data-nav="1"/.test(h) && !/stage-hint/.test(h) && /data-fmt-toggle/.test(h) && !/data-pref="sec"/.test(h) && /data-stage-count/.test(h) && /data-nav-toggle/.test(h), `${name}: karusel düğmeleri eksik`);
    check(!/data-pref="sync"/.test(h) && /data-sync-status/.test(h) && /class="meta source-line"/.test(h), `${name}: senkron düğmesi/durumu eksik`);
    check(/data-live="calendar"/.test(h), `${name}: özel model türleri eksik`);
  }
  if (!err && !embed && (name === "/index.html" || new RegExp(`^/(${LG})/index\\.html$`).test(name))) {
    // Dönen dünya web ana sayfasına geri döndü (31 Ekim 2026, sahibin kararı); OSM karoları dış hizmet izniyle
    check(/data-globe/.test(h) && /globe\.js/.test(h) && /"globe":\{"earth":"\/assets\/earth\.jpg\?v=/.test(h) && /"places":"\/assets\/places\.json/.test(h) && /data-globe-attr/.test(h) && /data-globe-zoom="in"/.test(h) && /data-globe-consent/.test(h), `${name}: dünya küresi eksik`);
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
    check(/class="top-date"><span data-live="date"/.test(h) && /data-place-open/.test(h) && /class="settings-menu"/.test(h) && /id="place-dialog"/.test(h), `${name}: üst bar (tarih, konum, ayarlar) eksik`);
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
  const timerPreset = /\/(geri-sayim|countdown)\/\d+-(dakika|saat|minutes?|hours?)\/index\.html$/.test(name);
  if (timerPreset) check(/data-cd-fixed="\d+"/.test(h) && /data-cd-endsat/.test(h) && /tools\.js/.test(h) && !/"FAQPage"/.test(h), `${name}: hazır süre sayfası eksik`);
  if (/\/alarm\/\d\d-\d\d\/index\.html$/.test(name)) {
    const t = name.match(/(\d\d)-(\d\d)\/index/);
    check(h.includes(`value="${t[1]}:${t[2]}"`) && /id="sleep-h"/.test(h) && /data-alarm-form/.test(h), `${name}: hazır alarm sayfası eksik`);
  }
  if (!timerPreset && /\/(geri-sayim|countdown)\/[a-z0-9-]+\/index\.html$/.test(name)) check(/data-cdp-keys/.test(h) && !/faq-more/.test(h) && !/"FAQPage"/.test(h) && /events-table/.test(h), `${name}: geri sayım sayfası eksik`);
  if (/\/(namaz-vakitleri|prayer-times)\/([a-z]+\/)?index\.html$/.test(name)) {
    const hub = /\/(namaz-vakitleri|prayer-times)\/index\.html$/.test(name);
    check(/data-prayer\b/.test(h) && (h.match(/data-pr="/g) || []).length === 8 && /prayer-calc\.js/.test(h) && /prayer\.js/.test(h) && /data-pr-month/.test(h) && /data-imsakiye/.test(h) && /imsakiye\.js/.test(h) && !/class="tool prayer"/.test(h) && !/pr-method-h/.test(h) && !/faq-more/.test(h) && !/"FAQPage"/.test(h), `${name}: namaz vakitleri yapısı eksik`);
    if (hub) check((h.match(/<option value="[a-z]+" data-lat=/g) || []).length === 81 && /data-place-geo/.test(h), `${name}: hub il listesi/konum düğmesi eksik`);
    else check(/data-mode="city"/.test(h) && /data-lat="[\d.]+" data-lon="[\d.]+"/.test(h), `${name}: il sayfası verisi eksik`);
  }
  if (/^\/((en|de|az|ar)\/qibla|kible)\/index\.html$/.test(name)) check(/data-qb-rq\b/.test(h), `${name}: Rashdul Qiblat satırı yok`);
  if (/^\/((en|de|az|ar)\/qibla|kible)\/index\.html$/.test(name)) check(/data-qibla\b/.test(h) && /data-qb-rose/.test(h) && /data-qb-needle/.test(h) && /qibla\.js/.test(h) && /prayer-calc\.js/.test(h) && /data-qb-mq-show/.test(h) && /data-needs-place/.test(h), `${name}: kıble sayfası yapısı eksik`);
  if (/\/(mevsimler|seasons)\/index\.html$/.test(name)) {
    // Tutulma namazı notu yalnız Vakitler'i başta soran dillerde (10 Kasım 2026)
    check(/class="meta">[^<]*(küsûf|kusuf|الكسوف|کسوف|কুসুফ)/.test(h) === /^\/((ar|id|ur|fa|bn)\/seasons|mevsimler)\//.test(name), `${name}: tutulma namazı notu yalnız Vakitler'i başta soran dillerde`);
    check(/data-wheel/.test(h) && /data-n-chart/.test(h) && /data-moon/.test(h) && (h.match(/class="season-card /g) || []).length === 4 && /nature\.js/.test(h) && /nature-calc\.js/.test(h) && /data-n-turns/.test(h) && /data-n-moon="rise"/.test(h) && /data-n-mooncal/.test(h) && /data-n-eclipses/.test(h) && !/data-place-city="/.test(h.replace(/<dialog[\s\S]*?<\/dialog>/g, "")) && !/faq-more/.test(h) && !/"FAQPage"/.test(h), `${name}: doğa sayfası yapısı eksik`);
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
      // İlçe namaz vakti sayfaları yalnız Türkçe: hreflang yalnız tr ve x-default
      const trOnly = /^\/namaz-vakitleri\/[a-z-]+\/[a-z0-9-]+\/index\.html$/.test(name) || name === "/emeklilik-hesaplama/index.html";
      for (const l of [...(trOnly ? ["tr"] : LANGS), "x-default"]) check(h.includes(`hreflang="${l}" href="${ORIGIN}`), `${name}: hreflang ${l} yok`);
      if (trOnly) check(!LANGS.some((l) => l !== "tr" && h.includes(`<link rel="alternate" hreflang="${l}"`)), `${name}: ilçe sayfasında başka dil hreflang'ı olmamalı`);
      check(/og:image/.test(h) && /twitter:card/.test(h), `${name}: OG/Twitter yok`);
    } else {
      check(/rel="alternate" hreflang/.test(h) === false && /rel="canonical" href="https:\/\/[^"]+\/\d{4}\/"/.test(h), `${name}: noindex giriş sayfası yıl sayfasına canonical vermeli`);
    }
    check(/action="https:\/\/bumbagroup\.com\/api\/liste\/katil"/.test(h) && /name="riza" value="on" required/.test(h) && !/name="riza"[^>]*checked/.test(h), `${name}: liste formu hatalı`);
    // madde 6: Katıl penceresi ortak aydınlatma metnini yükler; rıza yalnız "Kabul ediyorum" (submit, riza=on, başta pasif)
    check(/data-aydinlatma="https:\/\/bumbagroup\.com\/api\/liste\/aydinlatma\?dil=(tr|en)"/.test(h) && /<dialog[^>]+id="liste-pencere"/.test(h) && /<button type="submit" class="btn" id="liste-kabul" name="riza" value="on" disabled>/.test(h) && /id="liste-vazgec"/.test(h) && /id="liste-sonuc"/.test(h), `${name}: liste penceresi standart iskelete uymuyor`);
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
    if (/^(https?:|mailto:|data:|webcal:)/.test(u)) {
      if (u.startsWith("webcal:")) u = "https:" + u.slice(7); // takvim aboneliği (.ics) aynı dosyaya gider
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
const llms = fs.readFileSync(path.join(DIST, "llms.txt"), "utf8");
check(/Saat Tarih/.test(llms), "llms.txt boş");
// Kazanım rozetleri web'de yok (30 Ekim 2026, yalnız mobil uygulamalarda); eski adresler nginx'te 301
check(!fs.existsSync(path.join(DIST, "rozetler")) && !/\/(rozetler|badges)\//.test(sm), "web'de rozetler sayfası kalmış");

// Dil dosyaları: anahtar ve dizi uzunluğu eşliği (tr = kaynak)
const keys = (o, p = "") => Object.entries(o).flatMap(([k, v]) => (v && typeof v === "object" && !Array.isArray(v) ? keys(v, p + k + ".") : [p + k]));
const flat = (o, p = "") => Object.entries(o).flatMap(([k, v]) => (v && typeof v === "object" ? (Array.isArray(v) ? [[p + k, v]] : flat(v, p + k + ".")) : [[p + k, v]]));
const dict = Object.fromEntries(LANGS.map((l) => [l, new Map(flat(JSON.parse(fs.readFileSync(path.join(ROOT, "src/i18n", `${l}.json`), "utf8"))))]));
for (const l of LANGS.filter((x) => x !== "tr")) {
  for (const k of dict.tr.keys()) {
    check(dict[l].has(k), `${l}.json'da yok: ${k}`);
    const a = dict.tr.get(k), b = dict[l].get(k);
    // client.words.units: 20 ya da (sayıları düzensiz dillerde, ör. Urduca) 60 öğe
    if (Array.isArray(a) && Array.isArray(b)) check(a.length === b.length || (k === "client.words.units" && b.length === 60), `${l}.json: dizi uzunluğu farklı: ${k}`);
  }
  for (const k of dict[l].keys()) check(dict.tr.has(k) || /^(client\.words\.(hours|one|oneJoin)|calendar\.monthsTitle)$/.test(k), `tr.json'da yok: ${k} (${l})`);
  // Rakamlar her dilde Latin (CLAUDE.md › Diller): Farsça/Urduca/Arapça rakam yazılmaz
  for (const [k, v] of dict[l]) check(!/[\u0660-\u0669\u06F0-\u06F9]/.test(JSON.stringify(v)), `${l}.json: Latin olmayan rakam: ${k}`);
}
for (const l of LANGS) {
  const evs = JSON.parse(fs.readFileSync(path.join(DIST, "assets", `events-${l}.json`), "utf8"));
  check(evs.length > 100 && evs.every((e) => e.n && /^\d{4}-\d\d-\d\d$/.test(e.d)), `events-${l}.json: adsız ya da hatalı etkinlik`);
  check(!evs.some((e) => e.t === "exam"), `events-${l}.json: sınav etkinliği var (9 Kasım 2026'da kaldırıldı)`);
}
for (const l of LANGS) check(fs.existsSync(path.join(DIST, "assets", `search-${l}.json`)), `search-${l}.json yok`);
check(sm.split("<url>").slice(1).every((u) => (/\/namaz-vakitleri\/[a-z-]+\/[a-z0-9-]+\/<\/loc>|\/emeklilik-hesaplama\/<\/loc>/.test(u) ? ["tr"] : LANGS).every((l) => u.includes(`hreflang="${l}"`))), "sitemap: bir adreste hreflang eksik");
// İlçe namaz vakti sayfaları (yalnız Türkçe): il merkezi hariç 915 ilçe; il sayfasında kapalı ilçe listesi
{
  const dist = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/districts.json"), "utf8")).provinces;
  const pages = pagesInfo.filter((p) => /^\/namaz-vakitleri\/[a-z-]+\/[a-z0-9-]+\/index\.html$/.test(p.name));
  check(pages.length >= 900 && pages.length < Object.values(dist).flat().length, `ilçe sayfası sayısı beklenmedik: ${pages.length}`);
  const yk = fs.readFileSync(path.join(DIST, "namaz-vakitleri/hakkari/yuksekova/index.html"), "utf8");
  check(/data-mode="city" data-city="hakkari\/yuksekova" data-name="Yüksekova, Hakkari" data-lat="[\d.]+" data-lon="[\d.]+"/.test(yk) && /GeoNames \(CC BY 4\.0\)/.test(yk) && /<title>Yüksekova \(Hakkari\) Namaz Vakitleri \| Saat Tarih<\/title>/.test(yk) && /aria-current="page">Yüksekova</.test(yk), "Yüksekova ilçe sayfası eksik");
  const ist = fs.readFileSync(path.join(DIST, "namaz-vakitleri/istanbul/index.html"), "utf8");
  check(/class="pr-district-list"/.test(ist) && /href="\/namaz-vakitleri\/istanbul\/kadikoy\/"/.test(ist), "İstanbul il sayfasında ilçe listesi yok");
  check(!fs.existsSync(path.join(DIST, "en/prayer-times/istanbul/kadikoy")), "ilçe sayfaları yalnız Türkçe olmalı");
  const enIst = fs.readFileSync(path.join(DIST, "en/prayer-times/istanbul/index.html"), "utf8");
  check(!/pr-district-list/.test(enIst), "İngilizce il sayfasında ilçe listesi olmamalı");
}

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
  // Kıble (30 Ekim 2026): İstanbul 151,6° ve ~2405 km; kıble saati Diyanet'in yayımladığıyla (ezanvakti, İstanbul) ±1 dk
  const kq = m.exports.qibla(41.01, 28.98);
  check(Math.abs(kq.bearing - 151.6) < 0.1 && Math.abs(kq.km - 2405) < 5, `kıble: İstanbul ${kq.bearing.toFixed(2)}° ${kq.km.toFixed(0)} km`);
  for (const [d, v] of [[24, "11:12"], [28, "11:09"], [29, "11:09"]]) {
    const t = m.exports.qiblaTime(2026, 10, d, 41.01, 28.98, 3), [hh, mm] = v.split(":").map(Number);
    check(Number.isFinite(t) && Math.abs(Math.round(t * 60) - (hh * 60 + mm)) <= 1, `kıble saati ${d}.10.2026: ${t} ≠ ${v}`);
  }
  check(!Number.isFinite(m.exports.qiblaTime(2026, 10, 30, 40.71, -74.01, -4)), "kıble saati: New York'ta olmamalı");
  // Rashdul Qiblat (10 Kasım 2026): Kemenag 2025–2027 duyurularıyla aynı dakika (WIB, UTC+7)
  for (const [y, want] of [[2025, ["2025-05-27 16:18", "2025-07-15 16:27"]], [2026, ["2026-05-28 16:18", "2026-07-15 16:27"]], [2027, ["2027-05-28 16:18", "2027-07-16 16:27"]]]) {
    const got = m.exports.rashdulQiblat(y).map((ms) => new Date(Math.round(ms / 60000) * 60000 + 7 * 3600000).toISOString().slice(0, 16).replace("T", " "));
    check(got.join() === want.join(), `Rashdul Qiblat ${y}: ${got} ≠ ${want}`);
  }
  // İlçe merkezleri (src/data/districts.json, GeoNames): Yüksekova ve Datça 30 Ekim 2026, Diyanet vakitleriyle (±1 dk)
  const DS = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/districts.json"), "utf8"));
  check(DS.count === 973 && Object.keys(DS.provinces).length === 81, `ilçe verisi: ${DS.count} ilçe`);
  for (const [slug, name, want] of [["hakkari", "Yüksekova", "04:57 06:18 11:52 14:48 17:15 18:31"], ["mugla", "Datça", "06:03 07:24 12:58 15:56 18:22 19:38"]]) {
    const d = (DS.provinces[slug] || []).find((x) => x[0] === name);
    check(d, `ilçe verisi: ${name} yok`);
    if (!d) continue;
    const r = m.exports.times(2026, 10, 30, d[1], d[2], 3);
    want.split(" ").forEach((v, i) => {
      const k = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"][i], [hh, mm] = v.split(":").map(Number);
      check(Math.abs(Math.round(r[k] * 60) - (hh * 60 + mm)) <= 1, `ilçe vakti ${name} ${k}: ${r[k].toFixed(3)} ≠ ${v}`);
    });
  }
  // Kemenag (10 Kasım 2026): Jakarta 10 Kasım 2026, Kemenag'ın SIHAT tablosundaki 8 satır (Liputan6 aynası) birebir;
  // bölgeye göre varsayılan yöntem
  {
    const P = m.exports, Pm = P.methodParams("kemenag", false), r = P.times(2026, 11, 10, -6.17, 106.83, 7, Pm), mn = {};
    for (const k of ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]) mn[k] = P.toMin(r[k], Pm, k);
    Object.assign(mn, P.methodRows(2026, 11, 10, -6.17, 106.83, 7, Pm, mn.fajr));
    const want = { imsak: "03:56", fajr: "04:06", sunrise: "05:22", duha: "05:50", dhuhr: "11:40", asr: "14:59", maghrib: "17:51", isha: "19:03" };
    for (const [k, v] of Object.entries(want)) {
      const [hh, mm] = v.split(":").map(Number);
      check(mn[k] === hh * 60 + mm, `Kemenag Jakarta 10.11.2026 ${k}: ${mn[k]} ≠ ${v}`);
    }
    const dm = (tz, lat, lon) => { const d = P.defaultMethod(tz, lat, lon); return d.m + (d.h ? "+h" : ""); };
    check(dm("Asia/Jakarta", -6.2, 106.8) === "kemenag" && dm("Asia/Makassar", -5.1, 119.4) === "kemenag" && dm("Asia/Kolkata", 28.6, 77.2) === "karachi+h"
      && dm("Asia/Kolkata", 9.9, 76.3) === "karachi" && dm("Asia/Karachi", 24.9, 67) === "karachi+h" && dm("Asia/Singapore", 1.3, 103.8) === "singapore"
      && dm("Europe/Istanbul", 41, 29) === "diyanet" && dm("Europe/Berlin", 52.5, 13.4) === "diyanet", "bölgeye göre varsayılan yöntem");
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
  // Ay doğuşu/batışı: ABD Deniz Gözlemevi (USNO) değerleriyle ±1 dk (yerel saat; USNO dakikaya yuvarlar)
  const usno = [
    [41.01, 28.98, 3, "2026-10-30", "21:29", "12:36"], [51.51, -0.13, 0, "2026-11-15", "12:41", "21:02"],
    [-33.87, 151.21, 11, "2026-12-24", "20:37", "05:12"], [24.71, 46.68, 3, "2027-06-21", "20:45", "06:52"],
  ];
  for (const [lat, lon, tz, d, rise, set] of usno) {
    const start = Date.parse(d + "T00:00:00Z") - tz * 3600000, r = N.moonRiseSet(start, lat, lon);
    const hm = (s) => start + (+s.slice(0, 2) * 60 + +s.slice(3)) * 60000;
    check(r.rise !== null && Math.abs(r.rise - hm(rise)) <= 90000 && r.set !== null && Math.abs(r.set - hm(set)) <= 90000, `Ay doğuş/batış ${d} ${lat}: ${new Date(r.rise).toISOString()} ${new Date(r.set).toISOString()}`);
  }
  // Reykjavik 30 Ekim 2026: Ay ne doğar ne batar (USNO)
  const rk = N.moonRiseSet(Date.parse("2026-10-30T00:00:00Z"), 64.15, -21.94);
  check(rk.rise === null && rk.set === null, "Ay doğuş/batış: Reykjavik 30 Ekim 2026 boş olmalı");
}
// Tutulma verisi (NASA): her kaydın türü ve bölge kodları geçerli, beş dilde tür adı var
{
  const E = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/eclipses.json"), "utf8"));
  const types = { solar: ["total", "annular", "partial", "hybrid"], lunar: ["total", "partial", "penumbral"] };
  for (const kind of ["solar", "lunar"]) for (const e of E[kind]) {
    check(types[kind].includes(e.type) && !Number.isNaN(Date.parse(e.td)), `tutulma ${kind} ${e.td}: tür/tarih`);
    for (const c of [...e.regions, ...(e.path || [])]) check(/^([A-Z]{2}|\d{3})$/.test(c) && new Intl.DisplayNames(["tr"], { type: "region", fallback: "none" }).of(c), `tutulma ${e.td}: bölge kodu ${c}`);
    for (const l of LANGS) check(dict[l].has(`nature.eclipseTypes.${kind}${e.type[0].toUpperCase()}${e.type.slice(1)}`), `${l}: tutulma türü adı ${kind} ${e.type}`);
  }
}

// Gökyüzü olayları (2. analiz #154): meteor yağmuru zirveleri IMO 2026 ve 2027 takvimindeki günlerle (Table 5) aynı;
// bilinen karşı konumlar; Ay tutulması temas anları ve Güneş tutulması yerel görünüş (2. analiz #159)
{
  const { skyEvents } = await import("../lib/sky-events.mjs");
  const ev = skyEvents(ROOT, 2026, 2027), day = (e) => new Date(e.t).toISOString().slice(0, 10);
  const IMO = { 2026: { qua: "01-03", lyr: "04-22", eta: "05-06", sda: "07-31", per: "08-13", dra: "10-09", ori: "10-21", leo: "11-17", gem: "12-14", urs: "12-22" }, 2027: { qua: "01-04", lyr: "04-23", eta: "05-06", sda: "07-31", per: "08-13", dra: "10-09", ori: "10-22", leo: "11-18", gem: "12-14", urs: "12-23" } };
  for (const y of [2026, 2027]) for (const [id, md] of Object.entries(IMO[y])) check(ev.some((e) => e.k === "shower" && e.id === id && day(e) === `${y}-${md}`), `gökyüzü olayları: ${id} ${y} IMO günü ${md} değil`);
  for (const [p, d] of [["jupiter", "2026-01-10"], ["saturn", "2026-10-04"], ["mars", "2027-02-19"], ["jupiter", "2027-02-11"]]) check(ev.some((e) => e.k === "opp" && e.p === p && day(e) === d), `gökyüzü olayları: ${p} karşı konumu ${d}`);
  check(ev.some((e) => e.k === "conj" && e.a === "venus" && e.b === "jupiter" && day(e) === "2026-06-09"), "gökyüzü olayları: Venüs–Jüpiter 9 Haziran 2026");
  for (const l of LANGS) for (const k of ["evShower", "evOpp", "evConj", "evMoon", "showers.per", "tonightHeading", "nature.eclContacts.u2"].map((k) => k.startsWith("nature.") ? k : "sky." + k)) check(dict[l].has(k), `${l}: ${k} eksik`);
  const ld2 = (rel) => { const m = { exports: {} }; new Function("module", "window", fs.readFileSync(path.join(ROOT, rel), "utf8"))(m, undefined); return m.exports; };
  const N2 = ld2("src/js/nature-calc.js");
  // NASA: 26 Haziran 2029 tam Ay tutulması, gölge büyüklüğü 1,8436 (±0,02)
  const L = N2.lunarContacts(Date.parse("2029-06-26T03:23:22Z") - 69000);
  check(Math.abs(L.umag - 1.8436) < 0.02 && L.u2 && L.u3 && L.u3 - L.u2 > 90 * 60000, `Ay tutulması 2029: gölge büyüklüğü ${L.umag}`);
  const S2 = N2.solarLocal(Date.parse("2027-08-02T10:07:49Z") - 69000, 25.6872, 32.6396);
  check(S2 && S2.central === "total" && S2.obsc > 0.99, "Güneş tutulması 2027: Luksor'dan tam görülmeli");
}

// Metin satırı widget'ı (#187), şehir nüfusu (#180), paylaş düğmesi (#188)
{
  check(fs.existsSync(path.join(DIST, "embed/line.js")), "embed/line.js (metin satırı widget'ı) yok");
  const g = fs.readFileSync(path.join(DIST, "gomme/index.html"), "utf8");
  check(/data-eg-lcode/.test(g) && /data-eg-lkind/.test(g) && /line\.js/.test(g), "gömme sayfasında metin satırı üreteci eksik");
  const tk = fs.readFileSync(path.join(DIST, "dunya-saatleri/tokyo/index.html"), "utf8");
  check(/Nüfus \(kent bölgesi/.test(tk) && /data-share/.test(tk), "Tokyo sayfasında nüfus ya da paylaş düğmesi eksik");
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

// nginx başlıkları
const ng = fs.readFileSync(path.join(ROOT, "nginx.conf"), "utf8");
for (const s of ["Strict-Transport-Security", "max-age=31536000", "X-Content-Type-Options", "Referrer-Policy", "Content-Security-Policy", "https://istatistik.bumba.tr", "https://bumbagroup.com", "error_page 404", "error_page 500"]) check(ng.includes(s), `nginx.conf: ${s} yok`);
check(/location \^~ \/embed\//.test(ng) && /frame-ancestors \*/.test(ng), "nginx.conf: /embed/ için frame-ancestors * yok");
check(ng.split("https://tile.openstreetmap.org").length === 3, "nginx.conf: img-src OpenStreetMap karoları iki konumda olmalı (ana sayfa küresi)");
check(ng.split("frame-src 'self' https://www.youtube-nocookie.com").length === 3, "nginx.conf: frame-src YouTube (alarm müziği) iki konumda olmalı");
check(ng.split("https://api.open-meteo.com").length === 3, "nginx.conf: connect-src open-meteo iki konumda olmalı");
// Dünya resmî tatilleri (30 Ekim 2026, #27)
{
  const rd = (f) => (fs.existsSync(path.join(DIST, f, "index.html")) ? fs.readFileSync(path.join(DIST, f, "index.html"), "utf8") : "");
  const W = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/world-holidays.json"), "utf8"));
  check(Object.keys(W.countries).length === 30 && !W.countries.EG && !W.countries.UA, "dünya tatilleri: 30 ülke olmalı (EG, KZ, UA yok)");
  const hub = rd("resmi-tatiller");
  check((hub.match(/<tr data-d=/g) || []).length === 33 && /href="\/takvim\/\d{4}\/"/.test(hub), "dünya tatilleri merkezi: 32 ülke + Türkiye satırı yok");
  // Endonezya (SKB 3 Menteri) ve Hindistan (DoPT) resmî listelerden (10 Kasım 2026); cuti bersama tatil sayısına girmez
  const idp = rd("en/public-holidays/indonesia"), inp = rd("en/public-holidays/india"), idl = rd("id/public-holidays/indonesia");
  check(/data-d="2027-03-10"/.test(idp) && /class="wh-leave"/.test(idp) && /2027: 18 public holidays/.test(idp) && /SKB 3 Menteri/.test(idp), "Endonezya tatil sayfası eksik");
  check(/Idulfitri/.test(idl) && /Cuti bersama/.test(idl), "Endonezce Endonezya sayfasında yerel adlar yok");
  check(/data-d="2026-05-28"/.test(inp) && !/data-d="2026-05-27"/.test(inp) && /2027: 17 public holidays/.test(inp) && /DoP&amp;T/.test(inp), "Hindistan tatil sayfası eksik (Bakrid 2026 28 Mayıs)");
  const de = rd("de/public-holidays/germany"), tr = rd("resmi-tatiller/almanya");
  check(/data-d="2027-10-03"/.test(de) && /Tag der Deutschen Einheit/.test(de) && /Alman Birliği Günü/.test(tr) && /Yerel adı: Tag der Deutschen Einheit/.test(tr), "Almanya tatil sayfası eksik");
  check(/Public holidays in the United Kingdom/.test(rd("en/public-holidays/united-kingdom")), "EN: the United Kingdom");
}
// Pomodoro istatistikleri (30 Ekim 2026, #16 web)
{
  const h = fs.readFileSync(path.join(DIST, "pomodoro", "index.html"), "utf8");
  check(/data-pomo-task/.test(h) && /data-pomo-noise/.test(h) && /data-pomo-bars/.test(h) && /data-pomo-clear/.test(h), "pomodoro: istatistik/görev/ses eksik");
  check(/sth-pomo-log/.test(fs.readFileSync(path.join(DIST, "gizlilik", "index.html"), "utf8")), "gizlilik: sth-pomo-log yok");
}
// Saat dilimleri, yaz saati, Unix (30 Ekim 2026, #25)
{
  const rd = (f) => (fs.existsSync(path.join(DIST, f, "index.html")) ? fs.readFileSync(path.join(DIST, f, "index.html"), "utf8") : "");
  const hub = rd("saat-dilimleri");
  check((hub.match(/data-live="time" data-tz=/g) || []).length >= 25 && /href="\/saat-dilimleri\/cet\/"/.test(hub), "saat dilimleri merkezi eksik");
  const cet = rd("saat-dilimleri/cet");
  check(/UTC\+1/.test(cet) && /data-tz="Etc\/GMT-1"/.test(cet) && /Türkiye Saati/.test(cet), "CET sayfası eksik");
  check(/UTC−5/.test(rd("en/time-zones/est")), "EST sayfası: UTC−5 yok");
  const dstH = rd("yaz-saati"), at = (y, m, d, h) => Date.UTC(y, m - 1, d, h);
  check(dstH.includes(`data-at="${at(2027, 3, 28, 1)}"`), "yaz saati: 28 Mart 2027 01:00 UTC (Avrupa) yok");
  check(dstH.includes(`data-at="${at(2027, 3, 14, 7)}"`), "yaz saati: 14 Mart 2027 (ABD, New York 07:00 UTC) yok");
  const ux = rd("unix-zaman-damgasi");
  check(/data-unix/.test(ux) && /data-ux-ts/.test(ux) && /data-ux-date/.test(ux) && /zones\.js/.test(ux), "Unix sayfası eksik");
}
// Kapsayıcı takvimler (9 Kasım 2026; lib/calendars, docs/takvimler.md): modül doğrulaması (19 dilde metin eşliği, tarih, renk),
// her modülün <id>.test.json'u, uygulamalardaki calendars.json ve cals.* metinleri güncel, takvim yıl sayfasında seçim ve veri
{
  const data = calendarData(), texts = loadCalTexts(ROOT);
  for (const e of validateCalendars(data, texts, ROOT)) check(false, `takvimler: ${e}`);
  for (const e of runCalendarTests(data, ROOT)) check(false, `takvim testi: ${e}`);
  const body = calendarsJson(data);
  for (const f of ["mobile/android/app/src/main/assets/calendars.json", "mobile/ios/App/Resources/calendars.json"]) {
    const fp = path.join(ROOT, f);
    check(fs.existsSync(fp) && fs.readFileSync(fp, "utf8") === body, `${f} güncel değil: node mobile/shared/gen-strings.mjs`);
  }
  const andr = fs.readFileSync(path.join(ROOT, "mobile/android/app/src/main/res/values/strings.xml"), "utf8");
  for (const { meta } of CALENDARS) for (const k of ["name", "desc", ...Object.keys(texts[meta.id].tr.ev).map((x) => `ev_${x}`)])
    check(andr.includes(`<string name="cals_${meta.id}_${k}">`), `uygulama metni yok: cals.${meta.id}.${k.replace(/_/g, ".")} (node mobile/shared/gen-strings.mjs)`);
  for (const [f, l, y] of [["takvim/2027", "tr", 2027], ["en/calendar/2027", "en", 2027], ["ar/calendar/2027", "ar", 2027]]) {
    const h = fs.readFileSync(path.join(DIST, f, "index.html"), "utf8");
    check(/<details class="cal-pick" data-cals hidden>/.test(h) && CALENDARS.every(({ meta }) => h.includes(`data-cal="${meta.id}"`)) && /data-day-cals/.test(h) && /data-cal-notes/.test(h), `takvim seçimi eksik: /${f}/`);
    const m = h.match(new RegExp(`"cals":\\{"url":"(/assets/cals-${l}-${y}\\.json)\\?v=[0-9a-f]+","list":\\[`));
    check(m, `takvim verisi adresi yok: /${f}/`);
    if (m) {
      const j = JSON.parse(fs.readFileSync(path.join(DIST, m[1]), "utf8"));
      check(j.christian && j.christian.events.some((e) => e[0] === "2027-03-28" && e[1] === "easter") && j.christian.ev.easter === texts.christian[l].ev.easter && j.christian.events.every((e) => e[0].startsWith(String(y)) || (e[2] || "").startsWith(String(y))), `takvim verisi hatalı: ${m[1]}`);
    }
  }
  for (const l of LANGS) check(fs.readFileSync(path.join(DIST, l === "tr" ? "gizlilik" : `${l}/privacy`, "index.html"), "utf8").includes("sth-cals"), `gizlilik: çerez tablosunda sth-cals yok (${l})`);
}
// Takvim aboneliği (.ics) ve yazdırma (30 Ekim 2026, #21)
for (const l of LANGS) for (const k of ["holidays", "days"]) {
  const f = path.join(DIST, "ics", `${l}-${k}.ics`);
  const txt = fs.existsSync(f) ? fs.readFileSync(f, "utf8") : "";
  const lines = txt.split("\r\n");
  check(txt.startsWith("BEGIN:VCALENDAR\r\n") && txt.endsWith("END:VCALENDAR\r\n") && (txt.match(/BEGIN:VEVENT/g) || []).length > 50 && lines.every((x) => Buffer.byteLength(x) <= 75) && !/[^\r]\n/.test(txt), `ics bozuk: ${l}-${k}.ics`);
}
check(fs.readFileSync(path.join(DIST, "ics", "tr-holidays.ics"), "utf8").includes("DTSTART;VALUE=DATE:20270309"), "ics: 2027 Ramazan Bayramı 1. gün yok");
check(/location \^~ \/ics\/[\s\S]*?default_type "text\/calendar/.test(ng), "nginx.conf: /ics/ text/calendar yok");
check(/data-print/.test(fs.readFileSync(path.join(DIST, "takvim", "2027", "index.html"), "utf8")) && /webcal:\/\//.test(fs.readFileSync(path.join(DIST, "en", "calendar", "2027", "index.html"), "utf8")), "takvim: dışa aktarma bölümü yok");
// Açık veri (statik API, 30 Ekim 2026): dosyalar, İstanbul 9 Ekim 2026 vakitleri (yukarıdaki Diyanet değerleri), nginx CORS
{
  const api = (rel) => { const f = path.join(DIST, "api", "v1", rel); return fs.existsSync(f) ? JSON.parse(fs.readFileSync(f, "utf8")) : null; };
  const idx = api("index.json");
  check(idx && idx.version === "v1" && idx.license === "CC BY 4.0" && idx.endpoints.holidays.years.length >= 6, "API: index.json eksik");
  for (const y of idx?.endpoints.holidays.years || []) {
    const h = api(`holidays/${y}.json`), a = api(`astronomy/${y}.json`);
    check(h && h.events.length > 25 && h.events.every((e) => e.date.startsWith(String(y)) && LANGS.every((l) => e.name[l])), `API: holidays/${y}.json bozuk`);
    check(a && a.seasons.marchEquinox.startsWith(`${y}-03-`) && a.moonPhases.length >= 24, `API: astronomy/${y}.json bozuk`);
  }
  check(api("holidays/2027.json")?.events.some((e) => e.date === "2027-03-09" && e.type === "holiday"), "API: 2027 Ramazan Bayramı yok");
  const prov = api("provinces.json");
  check(prov && prov.provinces.length === 81, "API: provinces.json 81 il değil");
  for (const p of prov?.provinces || []) for (const y of idx.endpoints.prayerTimes.years) {
    const f = api(`prayer-times/${p.slug}/${y}.json`);
    check(f && f.days.length === (y % 4 ? 365 : 366) && f.days.every((d) => d.length === 7), `API: prayer-times/${p.slug}/${y}.json bozuk`);
  }
  const ist = api("prayer-times/istanbul/2026.json")?.days.find((d) => d[0] === "2026-10-09") || [];
  ["05:37", "07:02", "12:57", "16:07", "18:41", "20:00"].forEach((v, i) => {
    const m = (x) => { const [h, mm] = String(x).split(":").map(Number); return h * 60 + mm; };
    check(Math.abs(m(ist[i + 1]) - m(v)) <= 1, `API: İstanbul 9.10.2026 ${i}. vakit ${ist[i + 1]} ≠ ${v}`);
  });
  const hj = api("hijri.json");
  check(hj && hj.months.some((r) => r[0] === "2026-11-10" && r[1] === 1448 && r[2] === 6), "API: hicri tabloda Cemaziyelahir 1448 (10 Kasım 2026) yok");
  check(/location \^~ \/api\/[\s\S]*?application\/json[\s\S]*?Access-Control-Allow-Origin "\*"/.test(ng), "nginx.conf: /api/ konumu (JSON, CORS) yok");
  const apiPage = fs.readFileSync(path.join(DIST, "gelistiriciler", "index.html"), "utf8");
  check((apiPage.match(/<tr><td dir="ltr"><a href="\/api\/v1\//g) || []).length === 6 && /"Dataset"/.test(apiPage), "API belge sayfası eksik");
  const zk = fs.readFileSync(path.join(DIST, "zekat-hesaplama", "index.html"), "utf8");
  check(/data-zakat\b/.test(zk) && (zk.match(/data-zk="/g) || []).length === 8 && /zakat\.js/.test(zk) && /data-zk-verdict/.test(zk), "zekât sayfası eksik");
  check(/data-fitre="\d+"/.test(zk) && /data-fitre-people/.test(zk) && /data-fitre-total/.test(zk), "zekât sayfasında fitre bölümü yok");
  // BAZNAS (Endonezya; 10 Kasım 2026): esas seçimi, fitrah Rp 50.000; Endonezce sayfada BAZNAS seçili
  const zkId = fs.readFileSync(path.join(DIST, "id", "zakat-calculator", "index.html"), "utf8");
  check(/data-zk-std/.test(zk) && /data-fitrah="50000"/.test(zk) && /Rp 50\.000/.test(zk) && /<option value="baznas" selected>/.test(zkId), "zekât: BAZNAS esası eksik");
}
// 9. tur (30 Ekim 2026, rakip analizi K maddeleri): kerahat, altın/mavi saat, Ay .ics, araç ekleri, saat görünümü
{
  const rd = (...p) => fs.readFileSync(path.join(DIST, ...p), "utf8");
  const pr = rd("namaz-vakitleri", "istanbul", "index.html");
  check(["israk", "istiva", "isfirar"].every((k) => pr.includes(`data-pr-kr="${k}"`)), "namaz il sayfasında kerahat vakitleri yok");
  const pm = { exports: {} };
  new Function("module", "window", fs.readFileSync(path.join(ROOT, "src/js/prayer-calc.js"), "utf8"))(pm, undefined);
  const P = pm.exports;
  {
    // Bayram namazı (2. analiz #56): Diyanet = güneş + 38 dk; ek vakitler (#59)
    const hm = (h) => Math.round(h * 60), bay = (y, m, d, lat, lon) => hm(P.bayram(P.times(y, m, d, lat, lon, 3)));
    check(Math.abs(bay(2025, 3, 30, 41.0082, 28.9784) - (7 * 60 + 22)) <= 1 && Math.abs(bay(2026, 3, 20, 40.1826, 29.0665) - (7 * 60 + 38)) <= 1, "bayram namazı vakti (İstanbul 30.3.2025 07:22, Bursa 20.3.2026 07:38) beklenen değil");
    const r = P.times(2026, 11, 3, 41.0082, 28.9784, 3), ex = P.extraTimes(2026, 11, 3, 41.0082, 28.9784, 3, r);
    const want = { israk: 490, duha: 725, isfirar: 1044, istibak: 1128, isaSani: 1176, midnight: 1445, tahajjud: 1564, seher: 1684 };
    check(Object.keys(want).every((k) => Math.abs(hm(ex[k]) - want[k]) <= 1), "ek vakitler İstanbul 3.11.2026 beklenen değil");
    check(pr.includes('data-pr-ex="isaSani"') && pr.includes("data-pr-bayram") && /"bayramDays":\["\d{4}-\d\d-\d\d"/.test(pr), "namaz il sayfasında ek vakitler ya da bayram namazı yok");
  }
  {
    const lh = P.lightHours(2026, 10, 30, 41.0082, 28.9784, 3), hm = (h) => Math.round(h * 60);
    check(Math.abs(hm(lh.goldenAm[1]) - (8 * 60 + 11)) <= 1 && Math.abs(hm(lh.bluePm[1]) - (18 * 60 + 31)) <= 1, "altın/mavi saat İstanbul 30.10.2026 beklenen değil");
  }
  const ns = rd("mevsimler", "index.html");
  check(["blueAm", "goldenAm", "goldenPm", "bluePm"].every((k) => ns.includes(`data-n-light="${k}"`)) && ns.includes("/ics/tr-moon.ics"), "mevsimler: altın/mavi saat ya da Ay takvimi aboneliği yok");
  check(ns.includes('data-folk-from="03-11"') && ns.includes('data-folk-from="12-22"') && /Berdü'l-acûz|Berdü&#39;l-acûz/.test(ns), "mevsimler: halk takvimi (2. analiz #60) yok");
  const mi = rd("ics", "tr-moon.ics");
  check(/^BEGIN:VCALENDAR\r\n/.test(mi) && (mi.match(/BEGIN:VEVENT/g) || []).length > 100 && /DTSTART:\d{8}T\d{6}Z/.test(mi) && /Dolunay/.test(mi), "tr-moon.ics yapısı bozuk");
  const cd = rd("geri-sayim", "index.html"), sw = rd("kronometre", "index.html"), po = rd("pomodoro", "index.html"), al = rd("alarm", "index.html");
  check(/data-cd-add/.test(cd) && /data-sw-csv/.test(sw) && /data-pomo-csv/.test(po) && /data-pomo-g/.test(po) && /data-pomo-tag/.test(po) && /data-pomo-streak/.test(po) && (al.match(/<option value="(chime|bell|rise|digital|soft)"/g) || []).length === 5, "araç ekleri (+1 dk, CSV, hedef, etiket, alarm sesi) eksik");
  const home = rd("index.html");
  // Menü sırası (9 Kasım 2026): Namaz vakitleri tr/ar/id/ur/fa'da ikinci, diğer dillerde sonda
  const navOrder = (h) => [...(h.match(/<nav class="nav"[\s\S]*?<\/nav>/) || [""])[0].matchAll(/href="([^"]+)"/g)].map((m) => m[1]);
  const trNav = navOrder(home), enNav = navOrder(rd("en", "index.html")), arNav = navOrder(rd("ar", "index.html"));
  check(trNav[1] === "/namaz-vakitleri/" && arNav[1] === "/ar/prayer-times/" && enNav.indexOf("/en/prayer-times/") === enNav.length - 2, `menü sırası hatalı: ${trNav} | ${enNav}`);
  check((home.match(/data-look="font"/g) || []).length === 2 && (home.match(/data-look="color"/g) || []).length === 2, "ayarlarda saat görünümü (yazı tipi, renk) yok");
  // Ek araçlar: 8 sayfa her dilde, araçlar merkezinde kart, betik
  const more = { "hicri-takvim-cevirici": ["data-hj-g", "data-hj-y", "data-hj-tbody"], "saat-hesaplama": ["data-tc-s", "data-tc-sum"], "kac-gun-kaldi": ["data-du-d", "data-du-row"], "aralik-zamanlayici": ["data-iv-start", "data-iv-preset"], "satranc-saati": ["data-ch-side=\"white\"", "data-ch-control"], "metronom": ["data-mm-range", "data-mm-tap"], "konusan-saat": ["data-tk-say", "data-live=\"words\""], "burc-hesaplama": ["data-zo-d", "nature-calc.js"] };
  for (const [slug, marks] of Object.entries(more)) {
    const h = rd(slug, "index.html");
    check(/more-tools\.js/.test(h) && marks.every((m) => h.includes(m)) && /"WebApplication"/.test(h), `${slug}: ek araç sayfası eksik`);
  }
  for (const p of ["en/hijri-converter", "en/time-calculator", "en/days-until", "en/interval-timer", "en/chess-clock", "en/metronome", "en/talking-clock", "en/zodiac-sign", "de/chess-clock", "ar/zodiac-sign", "az/metronome"]) check(fs.existsSync(path.join(DIST, p, "index.html")), `${p} yok`);
  const hub = rd("araclar", "index.html");
  check(["/hicri-takvim-cevirici/", "/saat-hesaplama/", "/kac-gun-kaldi/", "/aralik-zamanlayici/", "/satranc-saati/", "/metronom/", "/konusan-saat/", "/burc-hesaplama/"].every((u) => hub.includes(`href="${u}"`)), "araçlar merkezinde ek araç kartları yok");
  const hj = rd("hicri-takvim-cevirici", "index.html");
  check(/Cemaziyelahir<\/th><td>10 Kasım 2026<\/td>/.test(hj), "hicri çevirici: Cemaziyelahir 1448 (10 Kasım 2026) tabloda yok");
  check(/14/.test(dict.tr.get("weather.title")) && ![...dict.tr].some(([k, v]) => k.startsWith("weather.") && /\b7 gün/i.test(String(v))), "hava durumu 14 günlük değil");
}
// Köprü tatil (lib/bridges.mjs): 2027 Kurban + 19 Mayıs 2 gün izinle 9 gün; 2026 Ramazan 0,5 gün izinle 4 gün
{
  const { computeYear } = await import("../lib/events.mjs");
  const { bridges } = await import("../lib/bridges.mjs");
  const HOLD = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/holidays.json"), "utf8"));
  const evs = (y) => [y - 1, y, y + 1].flatMap((x) => computeYear(x, HOLD).events).filter((e) => e.type === "holiday" || e.type === "half");
  const b27 = bridges(2027, evs(2027)), b26 = bridges(2026, evs(2026));
  check(b27.some((b) => b.groups.includes("kb") && b.leave === 2 && b.days === 9 && b.from === "2027-05-15"), "köprü tatil: 2027 Kurban 2 gün izinle 9 gün yok");
  check(b26.some((b) => b.group === "rb" && b.leave === 0.5 && b.days === 4), "köprü tatil: 2026 Ramazan 0,5 gün izinle 4 gün yok");
  for (const f of ["kopru-tatil", "en/long-weekends", "ar/long-weekends"]) {
    const h = fs.existsSync(path.join(DIST, f, "index.html")) ? fs.readFileSync(path.join(DIST, f, "index.html"), "utf8") : "";
    check(/data-bridge/.test(h) && (h.match(/data-br-year="/g) || []).length >= 6 && /bridge\.js/.test(h), `köprü tatil sayfası eksik: /${f}/`);
  }
}
// Hicri ay başları (lib/hijri.mjs, uygulamalar): Diyanet vakit tablosundaki hicri tarihler (30 Ekim 2026'da okundu)
{
  const { hijriTable, hijriDate } = await import("../lib/hijri.mjs");
  const HOLD = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/holidays.json"), "utf8"));
  const t = hijriTable(HOLD, JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/hijri.json"), "utf8")));
  for (const [d, want] of [["2026-10-30", "1448/5/19"], ["2026-11-10", "1448/6/1"], ["2026-11-24", "1448/6/15"], ["2026-02-19", "1447/9/1"], ["2027-05-16", "1448/12/10"], ["2027-12-31", "1449/8/3"]]) {
    check((hijriDate(t, d) || []).join("/") === want, `hicri tarih: ${d} ${want} değil (${hijriDate(t, d)})`);
  }
  check(hijriDate(t, "2025-12-20") === null && hijriDate(t, "2031-12-31") !== null, "hicri tarih: tablo aralığı");
  // Endonezya takvimi (10 Kasım 2026): Kemenag 2026 takvimi, SKB 2026/2027 ve isbat; MABIMS hesabı dayanakların hepsini tutturmalı
  const { hijriTableId } = await import("../lib/hijri.mjs");
  const ti = hijriTableId(t, JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/hijri-id.json"), "utf8")));
  check(ti.mismatch.length === 0, `Endonezya hicri: MABIMS hesabı dayanaklardan ayrılıyor: ${ti.mismatch.join(", ")}`);
  check(ti[0][1] === t[0][1] && ti[0][2] === t[0][2] && ti.length === t.length, "Endonezya hicri: Diyanet tablosuyla aynı aralıkta değil");
  for (const [d, want] of [["2026-11-10", "1448/5/30"], ["2026-11-11", "1448/6/1"], ["2026-03-21", "1447/10/1"], ["2026-03-20", "1447/9/30"], ["2027-03-10", "1448/10/1"], ["2027-05-17", "1448/12/10"]]) {
    check((hijriDate(ti, d) || []).join("/") === want, `Endonezya hicri tarih: ${d} ${want} değil (${hijriDate(ti, d)})`);
  }
  check(/data-hj-cal\b/.test(fs.readFileSync(path.join(DIST, "hicri-takvim-cevirici/index.html"), "utf8")), "hicri çevirici: takvim seçimi yok");
}
// O maddeleri (30 Ekim 2026): hesap yöntemleri, hilal, gece gökyüzü, tutulma haritası, tarihte bugün, emeklilik
{
  const ld = (rel) => { const m = { exports: {} }; new Function("module", "window", fs.readFileSync(path.join(ROOT, rel), "utf8"))(m, undefined); return m.exports; };
  const P = ld("src/js/prayer-calc.js"), N = ld("src/js/nature-calc.js"), K = ld("src/js/sky-calc.js");
  const hm = (h) => Math.round(h * 60);
  const d0 = P.times(2026, 10, 30, 41.01, 28.98, 3), dh = P.times(2026, 10, 30, 41.01, 28.98, 3, P.methodParams("diyanet", true));
  check(hm(d0.fajr) === hm(dh.fajr) && hm(dh.asr) - hm(d0.asr) > 30, "Hanefî ikindisi: yalnız ikindi ve daha geç olmalı");
  check(P.METHOD_IDS.length === 15 && P.methodParams("bilinmeyen", false) === P.PARAMS, "hesap yöntemleri: 15 yöntem, bilinmeyen Diyanet");
  const mk = P.times(2026, 10, 30, 21.42, 39.83, 3, P.methodParams("makkah", false));
  check(Math.abs((mk.isha - mk.maghrib) * 60 - 90) < 0.01, "Ümmü'l-Kura: yatsı akşamdan 90 dk sonra olmalı");
  const pr = fs.readFileSync(path.join(DIST, "namaz-vakitleri", "istanbul", "index.html"), "utf8");
  check((pr.match(/<select id="pr-method"[^]*?<\/select>/) || [""])[0].split("<option").length - 1 === 15 && /data-pr-asr/.test(pr), "namaz sayfasında yöntem seçimi yok");
  // Hilal (Yallop): Ramazan 1447 kavuşumu 17 Şubat 2026; İstanbul'da ilk akşam görülmez, ikinci akşam görülür
  const conj = N.phaseMs(Math.floor((Date.UTC(2026, 1, 17) / 86400000 + 2440587.5 - 2451550.09766) / 29.530588861), false);
  const cr = N.crescent(conj, 41.01, 28.98, 3);
  check(Math.abs(conj - Date.UTC(2026, 1, 17, 12, 1)) < 5 * 60000 && cr[0].cat === "F" && cr[1].cat === "A", `hilal görünürlüğü: 17/18 Şubat 2026 beklenen değil (${cr.map((e) => e.cat)})`);
  check(/data-n-crescent/.test(fs.readFileSync(path.join(DIST, "mevsimler", "index.html"), "utf8")), "Mevsimler'de hilal görünürlüğü yok");
  // Gezegenler: JPL Horizons, 30 Ekim 2026 00:00 UT (görünür RA/Dec); yay dakikası doğruluk
  const REF = { mercury: [225.55519, -19.36992], venus: [203.34531, -15.48105], mars: [140.54922, 17.03933], jupiter: [146.61936, 14.14025], saturn: [9.70730, 1.25734] };
  for (const [n, [ra, dec]] of Object.entries(REF)) {
    const pl = K.planet(n, Date.UTC(2026, 9, 30)), err = Math.hypot((pl.ra - ra) * Math.cos(dec * Math.PI / 180), pl.dec - dec) * 60;
    check(err < (n === "saturn" ? 6 : 2), `gezegen konumu: ${n} JPL'den ${err.toFixed(1)}' uzak`);
  }
  for (const f of ["gece-gokyuzu", "en/night-sky", "de/night-sky", "az/night-sky", "ar/night-sky"]) {
    const h = fs.existsSync(path.join(DIST, f, "index.html")) ? fs.readFileSync(path.join(DIST, f, "index.html"), "utf8") : "";
    check(/data-sky-chart/.test(h) && /sky-calc\.js/.test(h), `gece gökyüzü sayfası eksik: /${f}/`);
  }
  // Tutulma haritası: kara sınırları ve harita bölümü
  check(fs.existsSync(path.join(DIST, "assets", "land.json")) && /data-ecl-map/.test(fs.readFileSync(path.join(DIST, "en", "seasons", "index.html"), "utf8")), "tutulma haritası eksik");
  // Tarihte bugün: beş dilde sayfa, ay dosyaları
  for (const f of ["tarihte-bugun", "en/on-this-day", "de/on-this-day", "az/on-this-day", "ar/on-this-day"]) check(fs.existsSync(path.join(DIST, f, "index.html")), `tarihte bugün sayfası yok: /${f}/`);
  check(fs.existsSync(path.join(DIST, "assets", "otd", "tr-10.json")) && fs.existsSync(path.join(DIST, "assets", "otd", "ar-02.json")), "tarihte bugün ay dosyaları yok");
  // Emeklilik: yalnız Türkçe, araçlar merkezinde (yalnız TR)
  check(/data-retire/.test(fs.readFileSync(path.join(DIST, "emeklilik-hesaplama", "index.html"), "utf8")) && fs.readFileSync(path.join(DIST, "araclar", "index.html"), "utf8").includes('href="/emeklilik-hesaplama/"') && !fs.readFileSync(path.join(DIST, "en", "tools", "index.html"), "utf8").includes("emeklilik"), "emeklilik hesaplama sayfası/kartı");
}
// Saat dilimi haberleri (31 Ekim 2026): beş dilde sayfa, her haber için yürürlük ve kaynak, Atom akışı
{
  const tzn = JSON.parse(fs.readFileSync(path.join(ROOT, "src", "data", "tz-news.json"), "utf8"));
  check(tzn.items.every((it) => LANGS.every((l) => Array.isArray(it.t[l]) && it.t[l].length === 2 && it.t[l].every(Boolean)) && /^\d{4}-\d\d-\d\d$/.test(it.date) && it.zones.length), "tz-news.json: her haberin beş dilde başlık+metni, tarihi ve dilimi olmalı");
  for (const f of ["saat-dilimi-haberleri", "en/time-zone-news", "de/time-zone-news", "az/time-zone-news", "ar/time-zone-news"]) {
    const h = fs.existsSync(path.join(DIST, f, "index.html")) ? fs.readFileSync(path.join(DIST, f, "index.html"), "utf8") : "";
    check((h.match(/class="tzn-item"/g) || []).length === tzn.items.length && h.includes("tz-news.xml"), `saat dilimi haberleri sayfası eksik: /${f}/`);
  }
  for (const l of LANGS) {
    const x = fs.existsSync(path.join(DIST, "feeds", `${l}-tz-news.xml`)) ? fs.readFileSync(path.join(DIST, "feeds", `${l}-tz-news.xml`), "utf8") : "";
    check(/^<\?xml/.test(x) && (x.match(/<entry>/g) || []).length === tzn.items.length && x.includes("cuba-2026"), `Atom akışı eksik: /feeds/${l}-tz-news.xml`);
  }
}
for (const f of ["geri-sayim/5-dakika", "geri-sayim/1-saat", "en/countdown/5-minutes", "ar/countdown/2-hours", "alarm/07-00", "en/alarm/06-30", "de/alarm/05-15"]) check(fs.existsSync(path.join(DIST, f, "index.html")), `hazır araç sayfası yok: /${f}/`);
for (const f of ["kible", "en/qibla", "de/qibla", "az/qibla", "ar/qibla"]) check(fs.existsSync(path.join(DIST, f, "index.html")), `kıble sayfası yok: /${f}/`);
check(ng.split("https://overpass-api.de").length === 3, "nginx.conf: connect-src overpass (yakındaki camiler) iki konumda olmalı");
check(!/includeSubDomains|preload/.test(ng), "nginx.conf: includeSubDomains/preload kullanılmamalı");
// Web alarmında YouTube müziği (31 Ekim 2026; dış hizmet izniyle, youtube-nocookie.com)
for (const f of ["alarm", "en/alarm", "ja/alarm", "alarm/07-00"]) {
  const h = fs.existsSync(path.join(DIST, f, "index.html")) ? fs.readFileSync(path.join(DIST, f, "index.html"), "utf8") : "";
  check(/data-al-yt\b/.test(h) && /data-ring-media/.test(h) && /frame-src 'self' https:\/\/www\.youtube-nocookie\.com/.test(h), `alarm sayfasında YouTube müziği alanı ya da frame-src eksik: /${f}/`);
}
// Toplantı oylaması (31 Ekim 2026): sunucusuz, bağlantıyla; bütün dillerde sayfa, araçlar kartı, gizlilik tablosunda sth-polls
for (const l of LANGS) {
  const f = l === "tr" ? "toplanti-oylamasi" : `${l}/meeting-poll`;
  const h = fs.existsSync(path.join(DIST, f, "index.html")) ? fs.readFileSync(path.join(DIST, f, "index.html"), "utf8") : "";
  check(/data-poll\b/.test(h) && /data-po-create/.test(h) && /data-po-add/.test(h) && /data-poll-view/.test(h) && /poll\.js/.test(h) && /"poll":\{"slotsHeading"/.test(h), `toplantı oylaması sayfası eksik: /${f}/`);
  const tools = fs.readFileSync(path.join(DIST, l === "tr" ? "araclar" : `${l}/tools`, "index.html"), "utf8");
  check(tools.includes(`href="/${f}/"`), `araçlar merkezinde toplantı oylaması kartı yok (${l})`);
  const pv = fs.readFileSync(path.join(DIST, l === "tr" ? "gizlilik" : `${l}/privacy`, "index.html"), "utf8");
  check(/sth-polls/.test(pv) && /youtube-nocookie\.com/.test(pv) && /tile\.openstreetmap\.org/.test(pv), `gizlilik: sth-polls, YouTube ya da OSM maddesi eksik (${l})`);
}

// 2. analiz hataları (3 Kasım 2026): #87–#93, #186
{
  const ld = (rel) => { const m = { exports: {} }; new Function("module", "window", fs.readFileSync(path.join(ROOT, rel), "utf8"))(m, undefined); return m.exports; };
  // #89 Ay evresi adı: ana evreler yalnız o anın ±12 saati içinde; dördün adı yarım Ay'dan uzakta çıkmamalı
  const N = ld("src/js/nature-calc.js");
  let bad = 0;
  for (let d = 0; d < 730; d++) { const x = N.moon(Date.UTC(2026, 0, 1, 12) + d * 86400000), i = Math.round(x.illum * 100); if ((x.idx === 2 || x.idx === 6) && (i < 44 || i > 56)) bad++; if (x.idx === 4 && i < 97) bad++; if (x.idx === 0 && i > 3) bad++; }
  check(bad === 0, `Ay evresi adı yüzdeyle çelişiyor (${bad} gün)`);
  check(N.moon(Date.UTC(2026, 10, 17, 12)).idx === 3 && N.moon(Date.UTC(2026, 10, 16, 12)).idx === 2, "Ay evresi: 16 Kasım 2026 ilk dördün, 17 Kasım şişkin olmalı");
  // #186 manyetik sapma: WMM2025 resmî test değerlerinden örnekler (±0,01°)
  const W = ld("src/js/wmm.js");
  for (const [y, alt, la, lo, dec] of [[2025, 28, 89, -121, -99.77], [2025, 0 + 51, -33, 109, -5.49], [2026.5, 43, 17, 52, 1.19], [2028.5, 11, 34, 0, 1.57], [2029.5, 33, 17, 5, 0.89]])
    check(Math.abs(W.declination(la, lo, alt, y) - dec) < 0.01, `WMM2025 sapması tutmuyor: ${la},${lo} ${y}`);
  for (const f of ["kible", "en/qibla", "ar/qibla"]) {
    const h = fs.readFileSync(path.join(DIST, f, "index.html"), "utf8");
    check(/wmm\.[0-9a-f]*\.?js|wmm\.js/.test(h) && h.search(/wmm[.\w]*\.js/) < h.search(/qibla[.\w]*\.js/), `kıble sayfasında wmm.js qibla.js'ten önce yüklenmeli: /${f}/`);
  }
  // #90 tutulma bölge adları derlemede yazılır (sayısal kod kalmamalı)
  for (const f of ["mevsimler", "en/seasons", "ja/seasons"]) {
    const h = fs.readFileSync(path.join(DIST, f, "index.html"), "utf8"), m = h.match(/"regionNames":(\{[^}]*\})/);
    check(m && !/"\d{3}":"\d{3}"/.test(m[1]) && /"005":"[^"\d]/.test(m[1]), `tutulma bölge adları eksik: /${f}/`);
  }
  // #93 araç sayfalarında başlık iki kez görünmez
  for (const f of ["toplanti-planlayici", "saat-cevirici", "toplanti-oylamasi", "gomme"]) {
    const h = fs.readFileSync(path.join(DIST, f, "index.html"), "utf8");
    check(!/class="tool-title"/.test(h) && /<h2 id="tool-h" class="sr-only">/.test(h), `başlık tekrarı: /${f}/`);
  }
  // #88 yüzde: site ortak yardımcıyı kullanır
  // Hava neyse ekran öyle (2. analiz #193): sahne kuralları uygulamalardakiyle aynı (WeatherSceneTest değerleri)
  {
    const m = { exports: {} };
    new Function("module", "window", fs.readFileSync(path.join(ROOT, "src/js/weather-fx.js"), "utf8"))(m, undefined);
    const F = m.exports, sc = F.scene;
    check(sc("clear", 0, 30).kind === "windy" && sc("cloudy", 0, 29).kind === "cloudy" && sc("rain", 0, 60).kind === "rain", "hava sahnesi: rüzgârlı türü 30 km/sa");
    check(sc("rain").drops === 90 && sc("rain", 1).drops === 70 && sc("rain", 50).drops === 240 && sc("storm").drops === 140, "hava sahnesi: damla sayısı");
    check(Math.abs(sc("rain").slant - 0.08) < 1e-9 && Math.abs(sc("rain", 0, 200).slant - 0.9) < 1e-9, "hava sahnesi: eğim");
    check(sc("cloudy", 0, 0, 1).clouds === 6 && sc("clear").clouds === 0 && sc("cloudy").clouds === 4, "hava sahnesi: bulut sayısı");
    check(sc("snow", 2).flakes === 140 && sc("clear", 0, 42).leaves === 12 && sc("rain", 0, 40).leaves === 10 && sc("rain", 0, 10).leaves === 0, "hava sahnesi: kar ve yaprak");
    check(F.kindOf(63) === "rain" && F.kindOf(73) === "snow" && F.kindOf(95) === "storm" && F.kindOf(45) === "fog" && F.kindOf(3) === "cloudy", "hava sahnesi: WMO kodu");
    const wh = fs.readFileSync(path.join(DIST, "hava-durumu/index.html"), "utf8");
    check(/data-wx-fx/.test(wh) && /weather-fx\.js/.test(wh), "hava sayfasında canlandırma canvas'ı ve betiği olmalı");
    check(/cloud_cover/.test(fs.readFileSync(path.join(ROOT, "src/js/weather-core.js"), "utf8")), "hava isteğinde bulut oranı olmalı");
  }
  // Hava açıklamaları ve günlük özet (2. analiz #108): eşikler Android WeatherWordsTest ve Swift WeatherWordsTests ile aynı girdiler
  {
    const win = { sth: { T: {} }, addEventListener() {} };
    const docStub = { querySelectorAll: () => [], querySelector: () => null, createElement: () => ({}) };
    new Function("window", "document", fs.readFileSync(path.join(ROOT, "src/js/weather-core.js"), "utf8"))(win, docStub);
    const W = win.sthWeather, Wd = W.words;
    check([3, 12, 25, 45, 80].map(Wd.wind).join() === "calm,light,breezy,strong,gale", "hava sözleri: rüzgâr");
    check([1, 4, 7, 9.5, 12].map(Wd.uv).join() === "low,moderate,high,veryHigh,extreme", "hava sözleri: UV");
    check([20, 45, 70, 90].map(Wd.humidity).join() === "dry,comfy,humid,veryHumid", "hava sözleri: nem");
    check([500, 2500, 8000, 24000].map(Wd.visibility).join() === "fog,haze,good,clear", "hava sözleri: görüş");
    check([5, 12, 18, 22].map(Wd.dew).join() === "dry,comfy,muggy,oppressive", "hava sözleri: çiy noktası");
    check(Wd.feels(10, 6) === "colder" && Wd.feels(30, 34) === "warmer" && Wd.feels(20, 21) === null, "hava sözleri: hissedilen");
    const hrs = (a, b, f) => Array.from({ length: b - a + 1 }, (_, i) => f({ hour: a + i, code: 1, rain: 10, wind: 10 }));
    const calm = hrs(8, 19, (x) => x), J = (o) => JSON.stringify(o);
    check(J(W.summary(calm, 20, 22)) === J({ key: "default" }) && J(W.summary(calm, 20, 13)) === J({ key: "colder", n: 7 }) && J(W.summary(calm, 20, 25.6)) === J({ key: "warmer", n: 6 }), "hava özeti: varsayılan, soğuk, sıcak");
    const windy = hrs(8, 19, (x) => (x.hour === 15 ? { ...x, wind: 35 } : x));
    const rain = windy.map((x) => (x.hour === 11 ? { ...x, code: 61, rain: 70 } : x));
    const snow = rain.map((x) => (x.hour === 11 ? { ...x, code: 73 } : x));
    const storm = snow.map((x) => (x.hour === 17 ? { ...x, code: 95 } : x));
    check(W.summary(windy, 20, 13).key === "windyAfternoon" && J(W.summary(rain, 20, 13)) === J({ key: "rain", hour: 11 }) && J(W.summary(snow, 20, 13)) === J({ key: "snow", hour: 11 }) && J(W.summary(storm, 20, 13)) === J({ key: "storm", hour: 17 }), "hava özeti: öncelik");
    const late = Array.from({ length: 16 }, (_, i) => ({ hour: 8 + i, code: 8 + i === 21 ? 95 : 1, rain: 0, wind: 0 }));
    check(W.summary(late, 20, null).key === "default", "hava özeti: 12 saatten sonrası bakılmaz");
    const wh = fs.readFileSync(path.join(DIST, "hava-durumu/index.html"), "utf8");
    check(/data-wx-sum/.test(wh) && /visibility/.test(fs.readFileSync(path.join(ROOT, "src/js/weather-core.js"), "utf8")), "hava sayfası: özet satırı ve görüş isteği");
    // Fotoğraf ışığı (9 Kasım 2026): gün doğumu/batımı fotoğraf tahmini kuralı; aynı girdiler iOS ve Android testlerinde
    const PHOTO_CASES = [[0, 0, 0, 0, "fair"], [10, 40, 10, 0, "good"], [10, 10, 60, 0, "good"], [30, 85, 90, 0, "fair"], [50, 50, 50, 0, "fair"], [65, 30, 30, 0, "poor"], [80, 0, 0, 0, "poor"],
      [5, 50, 50, 0.6, "poor"], [39, 20, 0, 0, "good"], [40, 20, 0, 0, "fair"], [0, 80, 0, 0, "good"], [0, 81, 0, 0, "fair"], [74, 0, 0, 0, "poor"], [59, 0, 0, 0, "fair"]];
    for (const [L, M, H, P, want] of PHOTO_CASES) check(W.photoSky(L, M, H, P) === want, `fotoğraf tahmini: (${L}, ${M}, ${H}, ${P}) ${want} değil (${W.photoSky(L, M, H, P)})`);
    const wc = fs.readFileSync(path.join(ROOT, "src/js/weather-core.js"), "utf8");
    check(/hourly=cloud_cover_low,cloud_cover_mid,cloud_cover_high,precipitation/.test(wc) && /forecast_days=7/.test(wc) && /function clouds\(lat, lon\) \{\s*if \(!allowed\(\)\)/.test(wc), "fotoğraf ışığı: bulut isteği (katmanlar, 7 gün, izin denetimi)");
  }
  // Panchang ve Rahu Kaal (10 Kasım 2026): her dilde; hinduKaal Drik Panchang'la (Delhi 8.11.2026, research doc §2.2)
  {
    const pm = { exports: {} };
    new Function("module", "window", fs.readFileSync(path.join(ROOT, "src/js/prayer-calc.js"), "utf8"))(pm, undefined);
    const k = pm.exports.hinduKaal(2026, 11, 8, 28.61, 77.21, 5.5);
    const hm = (h) => { const m = Math.round(h * 60); return String(Math.floor(m / 60)).padStart(2, "0") + ":" + String(m % 60).padStart(2, "0"); };
    check(k && hm(k.sunrise) === "06:38" && hm(k.sunset) === "17:31" && hm(k.rahu[0]) === "16:10" && hm(k.rahu[1]) === "17:31", `panchang: Delhi 8.11.2026 gün doğumu/Rahu Kaal Drik'le aynı değil (${k && [hm(k.sunrise), hm(k.sunset), hm(k.rahu[0]), hm(k.rahu[1])]})`);
    check(pm.exports.hinduKaal(2026, 12, 21, 78.2, 15.6, 1) === null, "panchang: kutup gecesinde dilim olmamalı");
    const PC = { tr: "panchang", en: "en/panchang", hi: "hi/panchang", bn: "bn/panchang", ar: "ar/panchang", ja: "ja/panchang" };
    for (const [l, f] of Object.entries(PC)) {
      const p = path.join(DIST, f, "index.html"), h = fs.existsSync(p) ? fs.readFileSync(p, "utf8") : "";
      check(/data-panchang\b/.test(h) && /panchang\.js/.test(h) && /prayer-calc\.js/.test(h) && /data-needs-place/.test(h) && /"WebApplication"/.test(h) && /panchang-days\.json/.test(h), `panchang sayfası eksik: /${f}/`);
      check(["sunrise", "sunset", "rahu", "yamaganda", "gulika", "abhijit", "brahma"].every((x) => h.includes(`data-pc="${x}"`)) && (h.match(/<tbody data-pc-week>([\s\S]*?)<\/tbody>/) || ["", ""])[1].split("<tr>").length - 1 === 7, `panchang: alanlar ya da 7 günlük tablo eksik (/${f}/)`);
      check(/data-pc-hindu>[^<]*20\d\d/.test(h), `panchang: Hindu takvimi satırı yok (/${f}/)`);
    }
    check(/href="\/panchang\/"/.test(fs.readFileSync(path.join(DIST, "araclar/index.html"), "utf8")) && /id="panchang"/.test(fs.readFileSync(path.join(DIST, "sss/index.html"), "utf8")), "panchang: araçlar kartı ya da SSS grubu yok");
    // Konum penceresinde dünya araması: alan, 31 şehir verisi
    const home = fs.readFileSync(path.join(DIST, "en/index.html"), "utf8");
    check(/data-place-q/.test(home) && /data-place-results/.test(home) && /place-cities-en\.json/.test(home), "konum penceresi: dünya araması eksik");
    const pc = JSON.parse(fs.readFileSync(path.join(DIST, "assets/place-cities-hi.json"), "utf8"));
    check(pc.length === 31 && pc.some((c) => c[1] === "Mumbai" && c[4] === "Asia/Kolkata"), "konum penceresi: 31 şehir verisi eksik");
  }
  // Fotoğraf ışığı sayfası (9 Kasım 2026): her dilde; saatler, Ay, 7 günlük tablo, izinli bulut tahmini ve atıf
  {
    const PH = { tr: "altin-saat", en: "en/golden-hour", ar: "ar/golden-hour", de: "de/golden-hour", ja: "ja/golden-hour", hi: "hi/golden-hour", bn: "bn/golden-hour" };
    for (const [l, f] of Object.entries(PH)) {
      const p = path.join(DIST, f, "index.html"), h = fs.existsSync(p) ? fs.readFileSync(p, "utf8") : "";
      check(/data-photo\b/.test(h) && /photo\.js/.test(h) && /prayer-calc\.js/.test(h) && /nature-calc\.js/.test(h) && /weather-core\.js/.test(h) && /data-needs-place/.test(h) && /"WebApplication"/.test(h), `fotoğraf ışığı sayfası eksik: /${f}/`);
      check(["blueAm", "goldenAm", "goldenPm", "bluePm", "rise", "set", "riseAz", "setAz", "riseQ", "setQ", "moonrise", "moonset", "phase"].every((k) => h.includes(`data-ph="${k}"`)), `fotoğraf ışığı: kart alanları eksik (/${f}/)`);
      check(/Open-Meteo\.com \(CC BY 4\.0\)/.test(h) && /<p class="meta consent-ph" data-ph-consent hidden>[^<]+<button type="button" class="btn btn-ghost btn-sm" data-consent-allow-ext>/.test(h) && /data-ph-fc-box hidden/.test(h), `fotoğraf ışığı: atıf ya da izin yer tutucusu eksik (/${f}/)`);
      check((h.match(/<tbody data-ph-week>([\s\S]*?)<\/tbody>/) || ["", ""])[1].split("<tr>").length - 1 === 7 && /data-ph-date[^>]*value="\d{4}-\d\d-\d\d"/.test(h), `fotoğraf ışığı: 7 günlük tablo ya da tarih seçimi eksik (/${f}/)`);
    }
    // Derleme günü İstanbul (UTC+3): saatler Mevsimler'deki altın/mavi saat tanımıyla aynı hesaptan
    const tr = fs.readFileSync(path.join(DIST, "altin-saat/index.html"), "utf8");
    if (cfg.updated === "2026-10-30") check(tr.includes('data-ph="goldenAm" dir="ltr">07:15–08:11<') && tr.includes('data-ph="bluePm" dir="ltr">18:20–18:31<') && /data-ph="riseAz">10\d° DGD</.test(tr), "fotoğraf ışığı: İstanbul 30.10.2026 değerleri beklenen değil");
    check(/href="\/altin-saat\/"/.test(fs.readFileSync(path.join(DIST, "araclar/index.html"), "utf8")) && /href="\/en\/golden-hour\/"/.test(fs.readFileSync(path.join(DIST, "en/tools/index.html"), "utf8")), "araçlar merkezi: fotoğraf ışığı kartı yok");
    check(/id="photo"/.test(fs.readFileSync(path.join(DIST, "sss/index.html"), "utf8")), "SSS: fotoğraf ışığı grubu yok");
    for (const l of LANGS) check(dict[l].get("photo.dirs").length === 16 && /Open-Meteo/.test(dict[l].get("photo.attribution")), `fotoğraf ışığı: ${l} yönler ya da atıf`);
    // Gizlilik: web, iPhone ve Android maddelerinde fotoğraf ışığının bulut tahmini (Open-Meteo, yaklaşık konum)
    for (const l of LANGS) {
      const j = JSON.parse(fs.readFileSync(path.join(ROOT, "src/i18n", `${l}.json`), "utf8")), name = j.tools.hub.cards.photo.name.toLowerCase();
      const om = j.privacy.sections.flatMap((s) => (s.ul || []).filter((u) => typeof u === "string" && /Open-Meteo/.test(u)));
      check(om.filter((u) => u.toLowerCase().includes(name)).length >= 3, `gizlilik: fotoğraf ışığı bulut tahmini web/iPhone/Android maddelerinde yok (${l})`);
    }
  }
  // K-1 site (2. analiz #174–#181, #184, #190): ilk ekran doğruluk satırı, başlıkta saat, görünüm seçenekleri, büyütme, haplar, hafta no
  {
    const h = fs.readFileSync(path.join(DIST, "index.html"), "utf8");
    check(/data-sync-short/.test(h) && /data-tz-pills/.test(h) && /data-title-clock=""/.test(h), "ana sayfa: kısa doğruluk satırı, saat dilimi hapları, başlıkta saat");
    check(/data-stage-zoom="-1"/.test(h) && /data-stage-zoom="1"/.test(h), "ana sayfa: saati büyütme/küçültme düğmeleri");
    check(/data-look="dateFmt"/.test(h) && /data-look="sep"/.test(h) && /data-look="weekStart"/.test(h) && /data-look-bool="ms"/.test(h), "ayarlar: tarih biçimi, ayırıcı, hafta başı, milisaniye");
    check(/class="top-week" data-live="weeklabel"/.test(h), "üst bar: hafta numarası");
    const ch = fs.readFileSync(path.join(DIST, "dunya-saatleri/new-york/index.html"), "utf8");
    check(/data-title-clock="America\/New_York"/.test(ch) && /data-live="solarnoon"/.test(ch) && /data-live="solaroff"/.test(ch) && /openstreetmap\.org\/\?mlat=/.test(ch) && !/data-tz-pills/.test(ch), "şehir sayfası: başlıkta saat, Güneş öğlesi, harita bağlantısı");
    const dh = fs.readFileSync(path.join(DIST, "saat-farki/index.html"), "utf8");
    check((dh.match(/data-live="diffbar"/g) || []).length === fs.readdirSync(path.join(DIST, "saat-farki")).filter((f) => f !== "index.html").length && /data-live="diffshort"/.test(dh), "saat farkı merkezi: kartlarda saat, fark ve çubuk");
    check(/id="model-holo"/.test(h) && /id="model-matrix"/.test(h) && /data-matrix/.test(h) && /data-live="math"/.test(h) && /data-look-bool="night"/.test(h), "ana sayfa: temalı saatler (hologram, Matrix, matematik) ve gece modu seçeneği");
    for (const [f, mt] of [["uyku-hesaplama", "sleep"], ["en/sleep-calculator", "sleep"], ["sinav-zamanlayicisi", "exam"], ["en/exam-timer", "exam"], ["sinif-araclari", "classroom"], ["en/classroom-tools", "classroom"], ["ja/classroom-tools", "classroom"]]) {
      const ph = fs.readFileSync(path.join(DIST, f, "index.html"), "utf8");
      check(new RegExp(`data-mt="${mt}"`).test(ph) && /more-tools\.js/.test(ph), `yeni araç sayfası: /${f}/`);
    }
    const th = fs.readFileSync(path.join(DIST, "araclar/index.html"), "utf8");
    check(/href="\/uyku-hesaplama\/"/.test(th) && /href="\/sinav-zamanlayicisi\/"/.test(th) && /href="\/sinif-araclari\/"/.test(th), "araçlar merkezi: uyku hesaplama, sınav zamanlayıcısı ve sınıf araçları kartları");
    const clh = fs.readFileSync(path.join(DIST, "sinif-araclari/index.html"), "utf8");
    check(/data-cl-tab="picker"/.test(clh) && /data-cl-tab="teams"/.test(clh) && /data-cl-tab="counter"/.test(clh) && /data-cl-names/.test(clh) && /role="tablist"/.test(clh), "sınıf araçları: ad seçici, takım kurucu, sayaç sekmeleri");
    const ph2 = fs.readFileSync(path.join(DIST, "toplanti-planlayici/index.html"), "utf8");
    check(/data-outlook/.test(ph2) && /data-gmail/.test(ph2) && /data-copytext/.test(ph2), "toplantı planlayıcı: Outlook, Gmail ve metin kopyalama");
    for (const l of LANGS) check(dict[l].get("consent.barText").length <= 170, `çerez bandı metni kısa olmalı (${l})`);
    const nh = fs.readFileSync(path.join(DIST, "mevsimler/index.html"), "utf8");
    check(/data-n-ring/.test(nh) && /data-n-tw="civil"/.test(nh) && /data-n-tw="astro"/.test(nh) && /data-n-ym\b/.test(nh) && /data-n-ymwheel/.test(nh), "mevsimler: alacakaranlık, ışık halkası, yılın dolunayları");
    check(/data-pr-ezani/.test(fs.readFileSync(path.join(DIST, "namaz-vakitleri/istanbul/index.html"), "utf8")), "namaz il sayfası: ezani saat");
    check(/data-hj-rumi\b/.test(fs.readFileSync(path.join(DIST, "hicri-takvim-cevirici/index.html"), "utf8")), "hicri çevirici: Rumi tarih");
    check(/month-origin-list/.test(fs.readFileSync(path.join(DIST, "takvim/2027/index.html"), "utf8")), "takvim: ay adlarının kökeni");
    for (const l of LANGS) check(fs.existsSync(path.join(ROOT, `mobile/android/app/src/main/assets/otd-${l}.json`)), `uygulama: tarihte bugün verisi (${l})`);
    const ah = fs.readFileSync(path.join(DIST, "alarm/index.html"), "utf8");
    check(/data-al-snooze/.test(ah) && /data-al-vol/.test(ah) && /data-al-try/.test(ah) && /data-al-ring/.test(ah) && /data-al-history/.test(ah) && /data-al-recent/.test(ah), "alarm: erteleme süresi, ses düzeyi, şimdi dene, halka, geçmiş, son saatler");
  }
  check(/S\.pct\(/.test(fs.readFileSync(path.join(ROOT, "src/js/weather.js"), "utf8")) && !/\+ "%"/.test(fs.readFileSync(path.join(ROOT, "src/js/weather.js"), "utf8").replace(/toFixed\(1\) \+ "%"/g, "")), "hava sayfasında yüzde dile göre biçimlenmeli");
}

if (fails.length) {
  console.error(`${fails.length} sorun:\n- ` + fails.join("\n- "));
  process.exit(1);
}
console.log(`Tamam: ${htmlFiles.length} HTML dosyası, ${pagesInfo.length} indekslenebilir sayfa, kontroller geçti.`);
