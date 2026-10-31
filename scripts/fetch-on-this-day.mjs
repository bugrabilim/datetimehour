/* "Tarihte bugün" verisi (30 Ekim 2026): Wikidata (CC0) sorgu hizmetinden olaylar, kuruluşlar ve (hizmet yanıt
   verirse) doğum/ölümler; gün-ay başına en çok bilinenler (Vikipedi bağlantı sayısına göre), sitenin dillerinde ad.
   Derleme ağa çıkmaz; bu betik elle çalıştırılır ve src/data/on-this-day.json yazılır.
   Kullanım: node scripts/fetch-on-this-day.mjs
   Yalnız adlar (yeni dil eklenince; kayıtlar ve seçim değişmez): node scripts/fetch-on-this-day.mjs --labels */
import fs from "node:fs";

const UA = "SaatTarih/1.0 (https://time.bumba.tr)";
const LANGS = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru"];
const OUT = "src/data/on-this-day.json";
const WDQS = "https://query.wikidata.org/sparql";

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
/* Hizmete nazik: sorgular arasında 3 sn, 429'da Retry-After kadar (yoksa 60 sn) beklenir */
async function sparql(query, tries = 4) {
  for (let i = 0; i < tries; i++) {
    await sleep(3000);
    const res = await fetch(WDQS, { method: "POST", headers: { "User-Agent": UA, Accept: "application/sparql-results+json", "Content-Type": "application/x-www-form-urlencoded" }, body: "query=" + encodeURIComponent(query) });
    if (res.ok) return (await res.json()).results.bindings;
    const wait = res.status === 429 ? (+res.headers.get("retry-after") || 60) * 1000 : 10000 * (i + 1);
    console.warn(`  ${res.status}, ${Math.round(wait / 1000)} sn sonra yeniden (${i + 1})`);
    await sleep(wait);
  }
  throw new Error("sorgu başarısız");
}
const qid = (u) => u.replace("http://www.wikidata.org/entity/", "");

/* Olay sınıfları (doğrudan P31): savaş, muharebe, antlaşma, devrim, deprem, darbe, saldırı, kazalar, kuşatma… */
const EVENT_CLASSES = ["Q178561", "Q131569", "Q198", "Q10931", "Q7944", "Q45382", "Q2223653", "Q744913", "Q852190", "Q3882219",
  "Q188055", "Q3199915", "Q43109", "Q7692360", "Q8070", "Q3839081", "Q179057", "Q645883", "Q467011", "Q124734", "Q752783", "Q1190554", "Q159821"];
const FOUND_CLASSES = ["Q6256", "Q3624078", "Q3024240", "Q484652"];
/* Meslekler: siyasetçi, yazar, oyuncu, şarkıcı, ressam, besteci, filozof, futbolcu, şair, subay, matematikçi, fizikçi,
   kimyager, film yönetmeni, iş insanı, astronot, kâşif, mimar, hükümdar (soylu), din adamı, bilim insanı */
const PEOPLE_OCC = ["Q82955", "Q36180", "Q33999", "Q177220", "Q1028181", "Q36834", "Q4964182", "Q937857", "Q49757", "Q189290",
  "Q170790", "Q169470", "Q593644", "Q2526255", "Q43845", "Q11631", "Q11900058", "Q42973", "Q2478141", "Q250867", "Q901"];

/* Adlar ve kısa açıklamalar (sitenin dilleri), 250'lik kümelerle */
async function fetchLabels(ids) {
  const labels = {};
  const langs = LANGS.map((l) => `"${l}"`).join(",");
  for (let i = 0; i < ids.length; i += 250) {
    const vals = ids.slice(i, i + 250).map((q) => "wd:" + q).join(" ");
    const rows = await sparql(`SELECT ?item ?l ?ll ?ds WHERE { VALUES ?item { ${vals} } { ?item rdfs:label ?l . BIND(LANG(?l) AS ?ll) FILTER(?ll IN (${langs})) } UNION { ?item schema:description ?ds . BIND(LANG(?ds) AS ?ll) FILTER(?ll IN (${langs})) } }`);
    for (const b of rows) {
      const q = qid(b.item.value), o = (labels[q] ||= { l: {}, s: {} });
      if (b.l) o.l[b.ll.value] = b.l.value;
      if (b.ds) o.s[b.ll.value] = b.ds.value;
    }
    console.log(`adlar ${Math.min(i + 250, ids.length)}/${ids.length}`);
  }
  return labels;
}

/* --labels: mevcut kayıtların adlarını yeniler (yeni dil eklenince); seçim ve sıra korunur */
async function relabel() {
  const data = JSON.parse(fs.readFileSync(OUT, "utf8"));
  const ids = [...new Set(Object.values(data.days).flat().map((x) => x.q))];
  const labels = await fetchLabels(ids);
  for (const list of Object.values(data.days)) for (const x of list) {
    const lb = labels[x.q];
    if (!lb) continue;
    x.l = { ...x.l, ...lb.l };
    if (x.s) x.s = { ...x.s, ...lb.s };
  }
  data.langs = LANGS;
  data.labelsFetched = new Date().toISOString().slice(0, 10);
  fs.writeFileSync(OUT, JSON.stringify(data) + "\n");
  console.log(`Yazıldı: ${OUT} (${ids.length} öğenin adları)`);
}

async function main() {
  const items = new Map(); // qid → { d: "MM-DD", y, k, n }
  const add = (b, k, dateVar) => {
    const v = b[dateVar].value; // "+1923-10-29T00:00:00Z" ya da "1923-10-29T00:00:00Z"
    const m = v.match(/^([+-]?\d+)-(\d\d)-(\d\d)T/);
    if (!m) return;
    const y = parseInt(m[1], 10);
    if (y < 1 || y > 2026) return;
    const id = qid(b.item.value), n = +b.links.value;
    const key = id + k;
    const prev = items.get(key);
    if (!prev || prev.n < n) items.set(key, { q: id, d: `${m[2]}-${m[3]}`, y, k, n });
  };
  for (const prop of ["P585", "P580", "P619"]) {
    for (let i = 0; i < EVENT_CLASSES.length; i += 6) {
      const vals = EVENT_CLASSES.slice(i, i + 6).map((c) => "wd:" + c).join(" ");
      console.log(`olaylar ${prop} ${vals}`);
      const rows = await sparql(`SELECT ?item ?date ?links WHERE { VALUES ?cls { ${vals} } ?item wdt:P31 ?cls ; wikibase:sitelinks ?links . FILTER(?links >= 20) ?item p:${prop}/psv:${prop} [ wikibase:timePrecision 11 ; wikibase:timeValue ?date ] . }`);
      rows.forEach((b) => add(b, "e", "date"));
    }
  }
  for (const c of FOUND_CLASSES) {
    console.log(`kuruluş ${c}`);
    const rows = await sparql(`SELECT ?item ?date ?links WHERE { ?item wdt:P31 wd:${c} ; wikibase:sitelinks ?links . FILTER(?links >= 40) ?item p:P571/psv:P571 [ wikibase:timePrecision 11 ; wikibase:timeValue ?date ] . }`);
    rows.forEach((b) => add(b, "f", "date"));
  }
  // Doğum ve ölümler: meslek gruplarıyla (tüm insanlar üzerinden tarama zaman aşımına düşüyor), en az 100 Vikipedi
  for (const occ of PEOPLE_OCC) {
    for (const [prop, k] of [["P569", "b"], ["P570", "d"]]) {
      try {
        const rows = await sparql(`SELECT ?item ?date ?links WHERE { ?item wdt:P106 wd:${occ} ; wdt:P31 wd:Q5 ; wikibase:sitelinks ?links . FILTER(?links >= 100) ?item p:${prop}/psv:${prop} [ wikibase:timePrecision 11 ; wikibase:timeValue ?date ] . }`, 2);
        rows.forEach((b) => add(b, k, "date"));
        console.log(`kişiler ${occ} ${prop}: ${rows.length}`);
      } catch (e) { console.warn(`kişiler ${occ} ${prop}: atlandı`); }
    }
  }
  // Gün başına seçim: olay 8, kuruluş 2, doğum 5, ölüm 5 (bağlantı sayısına göre)
  const LIMIT = { e: 8, f: 2, b: 5, d: 5 };
  const byDay = {};
  for (const it of items.values()) ((byDay[it.d] ||= {})[it.k] ||= []).push(it);
  const chosen = [];
  for (const d of Object.keys(byDay)) for (const k of Object.keys(byDay[d])) chosen.push(...byDay[d][k].sort((a, b) => b.n - a.n).slice(0, LIMIT[k] * 2));
  // Adlar ve kişiler için kısa açıklama (beş dil)
  const ids = [...new Set(chosen.map((c) => c.q))];
  const labels = await fetchLabels(ids);
  const out = {};
  for (const c of chosen) {
    const lb = labels[c.q];
    if (!lb || !lb.l.en) continue;
    (out[c.d] ||= []).push({ y: c.y, k: c.k, q: c.q, n: c.n, l: lb.l, ...(c.k === "b" || c.k === "d" ? { s: lb.s } : {}) });
  }
  for (const d of Object.keys(out)) out[d].sort((a, b) => (a.k === b.k ? b.n - a.n : "efbd".indexOf(a.k) - "efbd".indexOf(b.k)));
  const sorted = Object.fromEntries(Object.keys(out).sort().map((d) => [d, out[d]]));
  fs.writeFileSync(OUT, JSON.stringify({ source: "Wikidata (CC0), query.wikidata.org", fetched: new Date().toISOString().slice(0, 10), langs: LANGS, limits: LIMIT, days: sorted }) + "\n");
  console.log(`Yazıldı: ${OUT} (${Object.keys(sorted).length} gün, ${chosen.length} kayıt)`);
}
(process.argv.includes("--labels") ? relabel() : main()).catch((e) => { console.error(e); process.exit(1); });
