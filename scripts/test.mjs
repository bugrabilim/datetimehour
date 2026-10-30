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
    check(!/data-globe|globe\.js|openstreetmap/.test(h), `${name}: ana sayfada dünya küresi kalmış (30 Ekim 2026'da kaldırıldı)`);
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
  const timerPreset = /\/(geri-sayim|countdown)\/\d+-(dakika|saat|minutes?|hours?)\/index\.html$/.test(name);
  if (timerPreset) check(/data-cd-fixed="\d+"/.test(h) && /data-cd-endsat/.test(h) && /tools\.js/.test(h) && !/"FAQPage"/.test(h), `${name}: hazır süre sayfası eksik`);
  if (/\/alarm\/\d\d-\d\d\/index\.html$/.test(name)) {
    const t = name.match(/(\d\d)-(\d\d)\/index/);
    check(h.includes(`value="${t[1]}:${t[2]}"`) && /id="sleep-h"/.test(h) && /data-alarm-form/.test(h), `${name}: hazır alarm sayfası eksik`);
  }
  if (!timerPreset && /\/(geri-sayim|countdown)\/[a-z0-9-]+\/index\.html$/.test(name)) check(/data-cdp-keys/.test(h) && !/faq-more/.test(h) && !/"FAQPage"/.test(h) && /events-table/.test(h), `${name}: geri sayım sayfası eksik`);
  if (/\/(namaz-vakitleri|prayer-times)\/([a-z]+\/)?index\.html$/.test(name)) {
    const hub = /\/(namaz-vakitleri|prayer-times)\/index\.html$/.test(name);
    check(/data-prayer\b/.test(h) && (h.match(/data-pr="/g) || []).length === 6 && /prayer-calc\.js/.test(h) && /prayer\.js/.test(h) && /data-pr-month/.test(h) && /data-imsakiye/.test(h) && /imsakiye\.js/.test(h) && !/class="tool prayer"/.test(h) && !/pr-method-h/.test(h) && !/faq-more/.test(h) && !/"FAQPage"/.test(h), `${name}: namaz vakitleri yapısı eksik`);
    if (hub) check((h.match(/<option value="[a-z]+" data-lat=/g) || []).length === 81 && /data-place-geo/.test(h), `${name}: hub il listesi/konum düğmesi eksik`);
    else check(/data-mode="city"/.test(h) && /data-lat="[\d.]+" data-lon="[\d.]+"/.test(h), `${name}: il sayfası verisi eksik`);
  }
  if (/^\/((en|de|az|ar)\/qibla|kible)\/index\.html$/.test(name)) check(/data-qibla\b/.test(h) && /data-qb-rose/.test(h) && /data-qb-needle/.test(h) && /qibla\.js/.test(h) && /prayer-calc\.js/.test(h) && /data-qb-mq-show/.test(h) && /data-needs-place/.test(h), `${name}: kıble sayfası yapısı eksik`);
  if (/\/(mevsimler|seasons)\/index\.html$/.test(name)) {
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
check(!ng.includes("openstreetmap"), "nginx.conf: OpenStreetMap izni kalmış (web küresi kaldırıldı)");
check(ng.split("https://api.open-meteo.com").length === 3, "nginx.conf: connect-src open-meteo iki konumda olmalı");
// Dünya resmî tatilleri (30 Ekim 2026, #27)
{
  const rd = (f) => (fs.existsSync(path.join(DIST, f, "index.html")) ? fs.readFileSync(path.join(DIST, f, "index.html"), "utf8") : "");
  const W = JSON.parse(fs.readFileSync(path.join(ROOT, "src/data/world-holidays.json"), "utf8"));
  check(Object.keys(W.countries).length === 30 && !W.countries.EG && !W.countries.UA, "dünya tatilleri: 30 ülke olmalı (EG, KZ, UA yok)");
  const hub = rd("resmi-tatiller");
  check((hub.match(/<tr data-d=/g) || []).length === 31 && /href="\/takvim\/\d{4}\/"/.test(hub), "dünya tatilleri merkezi: 30 ülke + Türkiye satırı yok");
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
// Takvim aboneliği (.ics) ve yazdırma (30 Ekim 2026, #21)
for (const l of ["tr", "en", "de", "az", "ar"]) for (const k of ["holidays", "days"]) {
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
    check(h && h.events.length > 25 && h.events.every((e) => e.date.startsWith(String(y)) && ["tr", "en", "de", "az", "ar"].every((l) => e.name[l])), `API: holidays/${y}.json bozuk`);
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
    const lh = P.lightHours(2026, 10, 30, 41.0082, 28.9784, 3), hm = (h) => Math.round(h * 60);
    check(Math.abs(hm(lh.goldenAm[1]) - (8 * 60 + 11)) <= 1 && Math.abs(hm(lh.bluePm[1]) - (18 * 60 + 31)) <= 1, "altın/mavi saat İstanbul 30.10.2026 beklenen değil");
  }
  const ns = rd("mevsimler", "index.html");
  check(["blueAm", "goldenAm", "goldenPm", "bluePm"].every((k) => ns.includes(`data-n-light="${k}"`)) && ns.includes("/ics/tr-moon.ics"), "mevsimler: altın/mavi saat ya da Ay takvimi aboneliği yok");
  const mi = rd("ics", "tr-moon.ics");
  check(/^BEGIN:VCALENDAR\r\n/.test(mi) && (mi.match(/BEGIN:VEVENT/g) || []).length > 100 && /DTSTART:\d{8}T\d{6}Z/.test(mi) && /Dolunay/.test(mi), "tr-moon.ics yapısı bozuk");
  const cd = rd("geri-sayim", "index.html"), sw = rd("kronometre", "index.html"), po = rd("pomodoro", "index.html"), al = rd("alarm", "index.html");
  check(/data-cd-add/.test(cd) && /data-sw-csv/.test(sw) && /data-pomo-csv/.test(po) && /data-pomo-g/.test(po) && /data-pomo-tag/.test(po) && /data-pomo-streak/.test(po) && (al.match(/<option value="(chime|bell|rise|digital|soft)"/g) || []).length === 5, "araç ekleri (+1 dk, CSV, hedef, etiket, alarm sesi) eksik");
  const home = rd("index.html");
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
}
// O maddeleri (30 Ekim 2026): hesap yöntemleri, hilal, gece gökyüzü, tutulma haritası, tarihte bugün, emeklilik
{
  const ld = (rel) => { const m = { exports: {} }; new Function("module", "window", fs.readFileSync(path.join(ROOT, rel), "utf8"))(m, undefined); return m.exports; };
  const P = ld("src/js/prayer-calc.js"), N = ld("src/js/nature-calc.js"), K = ld("src/js/sky-calc.js");
  const hm = (h) => Math.round(h * 60);
  const d0 = P.times(2026, 10, 30, 41.01, 28.98, 3), dh = P.times(2026, 10, 30, 41.01, 28.98, 3, P.methodParams("diyanet", true));
  check(hm(d0.fajr) === hm(dh.fajr) && hm(dh.asr) - hm(d0.asr) > 30, "Hanefî ikindisi: yalnız ikindi ve daha geç olmalı");
  check(P.METHOD_IDS.length === 14 && P.methodParams("bilinmeyen", false) === P.PARAMS, "hesap yöntemleri: 14 yöntem, bilinmeyen Diyanet");
  const mk = P.times(2026, 10, 30, 21.42, 39.83, 3, P.methodParams("makkah", false));
  check(Math.abs((mk.isha - mk.maghrib) * 60 - 90) < 0.01, "Ümmü'l-Kura: yatsı akşamdan 90 dk sonra olmalı");
  const pr = fs.readFileSync(path.join(DIST, "namaz-vakitleri", "istanbul", "index.html"), "utf8");
  check((pr.match(/<select id="pr-method"[^]*?<\/select>/) || [""])[0].split("<option").length - 1 === 14 && /data-pr-asr/.test(pr), "namaz sayfasında yöntem seçimi yok");
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
for (const f of ["geri-sayim/5-dakika", "geri-sayim/1-saat", "en/countdown/5-minutes", "ar/countdown/2-hours", "alarm/07-00", "en/alarm/06-30", "de/alarm/05-15"]) check(fs.existsSync(path.join(DIST, f, "index.html")), `hazır araç sayfası yok: /${f}/`);
for (const f of ["kible", "en/qibla", "de/qibla", "az/qibla", "ar/qibla"]) check(fs.existsSync(path.join(DIST, f, "index.html")), `kıble sayfası yok: /${f}/`);
check(ng.split("https://overpass-api.de").length === 3, "nginx.conf: connect-src overpass (yakındaki camiler) iki konumda olmalı");
check(!/includeSubDomains|preload/.test(ng), "nginx.conf: includeSubDomains/preload kullanılmamalı");

if (fails.length) {
  console.error(`${fails.length} sorun:\n- ` + fails.join("\n- "));
  process.exit(1);
}
console.log(`Tamam: ${htmlFiles.length} HTML dosyası, ${pagesInfo.length} indekslenebilir sayfa, kontroller geçti.`);
