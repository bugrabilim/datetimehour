// Derleme çıktısı (dist/) için kalite denetimi. Bağımlılık yok: `npm test`
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const DIST = path.join(ROOT, "dist");
const cfg = JSON.parse(fs.readFileSync(path.join(ROOT, "site.config.json"), "utf8"));
const ORIGIN = (process.env.SITE_ORIGIN || cfg.origin).replace(/\/+$/, "");
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
  ids[f] = new Set([...h.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]));
  const title = (h.match(/<title>([^<]*)<\/title>/) || [])[1];
  const desc = (h.match(/<meta name="description" content="([^"]*)"/) || [])[1];
  check(title, `${name}: title yok`);
  check(desc, `${name}: description yok`);
  check((h.match(/<h1[ >]/g) || []).length === (err ? 2 : 1), `${name}: H1 sayısı hatalı`);
  check(/<html lang="(tr|en)"/.test(h), `${name}: html lang yok`);
  check(!/\sstyle="/.test(h), `${name}: satır içi style (CSP)`);
  check(!/http:\/\/(?!www\.w3\.org)/.test(h), `${name}: http:// bağlantı`);
  check(/class="rozet"/.test(h) && /width="164" height="28"/.test(h), `${name}: Bumba rozeti yok`);
  check(/data-theme-toggle/.test(h) && /data-search-open/.test(h), `${name}: tema ya da arama düğmesi yok`);
  check(/Content-Security-Policy/.test(h), `${name}: CSP yok`);
  if (!err && (name === "/index.html" || name === "/en/index.html" || /\/(dunya-saatleri|world-clock)\/[a-z-]+\/index\.html$/.test(name) && !/\/(dunya-saatleri|world-clock)\/index\.html$/.test(name))) {
    const slides = [...h.matchAll(/data-model="([a-z-]+)" data-name="([^"]+)"/g)];
    check(slides.length >= 10, `${name}: saat modeli sayısı ${slides.length} < 10`);
    check(new Set(slides.map((m) => m[1])).size === slides.length, `${name}: tekrarlanan model kimliği`);
    check((h.match(/data-dot="\d+"/g) || []).length === slides.length, `${name}: nokta sayısı model sayısıyla uyuşmuyor`);
    check(/data-fullscreen/.test(h) && /data-nav="-1"/.test(h) && /data-nav="1"/.test(h) && /data-pref="h12"/.test(h), `${name}: karusel düğmeleri eksik`);
    check(/data-live="calendar"/.test(h) && /data-live="flip"/.test(h) && /data-live="words"/.test(h) && /data-live="rings"/.test(h), `${name}: özel model türleri eksik`);
  }
  if (!err) {
    check(/rel="canonical" href="https:\/\//.test(h), `${name}: canonical yok`);
    for (const l of ["tr", "en", "x-default"]) check(h.includes(`hreflang="${l}" href="${ORIGIN}`), `${name}: hreflang ${l} yok`);
    check(/og:image/.test(h) && /twitter:card/.test(h), `${name}: OG/Twitter yok`);
    check(/action="https:\/\/bumbagroup\.com\/api\/liste\/katil"/.test(h) && /name="riza" value="on" required/.test(h) && !/name="riza"[^>]*checked/.test(h), `${name}: liste formu hatalı`);
    check(/name="site" value="[a-z-]+"/.test(h) && /name="web_sitesi"/.test(h), `${name}: liste formu alanları eksik`);
    check(/name="dil" value="(tr|en)"/.test(h), `${name}: liste dil alanı yok`);
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
    pagesInfo.push({ name, title, desc });
  } else {
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

// TR / EN anahtar eşliği
const keys = (o, p = "") => Object.entries(o).flatMap(([k, v]) => (v && typeof v === "object" && !Array.isArray(v) ? keys(v, p + k + ".") : [p + k]));
const tr = JSON.parse(fs.readFileSync(path.join(ROOT, "src/i18n/tr.json"), "utf8"));
const en = JSON.parse(fs.readFileSync(path.join(ROOT, "src/i18n/en.json"), "utf8"));
const kt = keys(tr), ke = keys(en);
for (const k of kt) check(ke.includes(k), `en.json'da yok: ${k}`);
for (const k of ke) check(kt.includes(k), `tr.json'da yok: ${k}`);

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
  for (const [fg, bg, min] of [["text", "bg", 4.5], ["text", "surface", 4.5], ["muted", "bg", 4.5], ["muted", "surface", 4.5], ["muted", "surface-2", 4.5], ["accent", "bg", 4.5], ["accent", "surface", 4.5], ["accent-ink", "accent", 4.5], ["ok", "ok-bg", 4.5], ["err", "err-bg", 4.5], ["text", "accent-soft", 4.5], ["focus", "bg", 3], ["muted", "bg", 3]]) {
    const r = ratio(t[fg], t[bg]);
    check(r >= min, `kontrast ${n}: ${fg}/${bg} = ${r.toFixed(2)} < ${min}`);
  }
}

// nginx başlıkları
const ng = fs.readFileSync(path.join(ROOT, "nginx.conf"), "utf8");
for (const s of ["Strict-Transport-Security", "max-age=86400", "X-Content-Type-Options", "Referrer-Policy", "Content-Security-Policy", "https://istatistik.bumba.tr", "https://bumbagroup.com", "error_page 404", "error_page 500"]) check(ng.includes(s), `nginx.conf: ${s} yok`);
check(!/includeSubDomains|preload/.test(ng), "nginx.conf: includeSubDomains/preload kullanılmamalı");

if (fails.length) {
  console.error(`${fails.length} sorun:\n- ` + fails.join("\n- "));
  process.exit(1);
}
console.log(`Tamam: ${htmlFiles.length} HTML dosyası, ${pagesInfo.length} indekslenebilir sayfa, kontroller geçti.`);
