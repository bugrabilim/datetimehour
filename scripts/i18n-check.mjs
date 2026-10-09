// Çeviri dosyası denetimi: `node scripts/i18n-check.mjs src/i18n/de.json [kaynak=src/i18n/en.json]`
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const readJson = (p) => JSON.parse(fs.readFileSync(path.resolve(p), "utf8"));
const ph = (s) => (String(s).match(/\{\w+\}/g) || []).sort().join(",");
const md = (s) => (String(s).match(/\]\([^)]*\)/g) || []).join("|");

export function compare(src, tgt, name = "") {
  const issues = [];
  const walk = (a, b, p) => {
    if (Array.isArray(a)) {
      if (!Array.isArray(b)) return issues.push(`${p}: dizi olmalı`);
      if (p.endsWith("words.hours") || p.endsWith("words.units") || p.endsWith("words.tens")) return;
      if (a.length !== b.length) issues.push(`${p}: dizi uzunluğu ${b.length} ≠ ${a.length}`);
      a.forEach((x, i) => b[i] !== undefined && walk(x, b[i], `${p}[${i}]`));
    } else if (a && typeof a === "object") {
      if (!b || typeof b !== "object") return issues.push(`${p}: nesne olmalı`);
      for (const k of Object.keys(a)) {
        if (!(k in b)) { if (!/words\.(unitFirst|hours)$/.test(`${p}.${k}`)) issues.push(`${p}.${k}: eksik`); continue; }
        walk(a[k], b[k], p ? `${p}.${k}` : k);
      }
      for (const k of Object.keys(b)) if (!(k in a) && !/words\.(unitFirst|hours)$/.test(`${p}.${k}`)) issues.push(`${p}.${k}: fazladan`);
    } else {
      if (typeof b !== typeof a) return issues.push(`${p}: tür farklı`);
      if (typeof a === "string") {
        if (ph(a) !== ph(b)) issues.push(`${p}: yer tutucular farklı (${ph(a)} ≠ ${ph(b)})`);
        if (md(a) !== md(b)) issues.push(`${p}: bağlantı adresleri farklı`);
        if (b.trim() === "" && a.trim() !== "" && !/(words\.join|consentPre|consentPost)$/.test(p)) issues.push(`${p}: boş`);
      }
    }
  };
  walk(src, tgt, "");
  for (const k of ["home", "world", "privacy", "converter", "planner", "datecalc", "embedgen", "pairHub"]) {
    const t = tgt[k] && tgt[k].title, d = tgt[k] && tgt[k].description;
    if (t && t.length > 70) issues.push(`${k}.title ${t.length} > 70 karakter`);
    if (d && d.length > 170) issues.push(`${k}.description ${d.length} > 170 karakter`);
  }
  for (const k of ["city", "pair", "cdp"]) {
    const t = tgt[k] && tgt[k].title;
    const fill = { name: "Johannesburg", a: "Istanbul", b: "Johannesburg", dat: "Cumhuriyet Bayramı'na" };
    if (t && t.replace(/\{(\w+)\}/g, (_, x) => fill[x] || "").length > 70) issues.push(`${k}.title şablonu en uzun şehirle 70 karakteri aşabilir`);
  }
  return issues;
}

if (process.argv[1] && process.argv[1].endsWith("i18n-check.mjs")) {
  const [, , tgtPath, srcPath = path.join(ROOT, "src/i18n/en.json")] = process.argv;
  const issues = compare(readJson(srcPath), readJson(tgtPath));
  if (issues.length) { console.error(`${issues.length} sorun:\n- ` + issues.join("\n- ")); process.exit(1); }
  console.log("Tamam: yapı, yer tutucular, bağlantılar ve uzunluklar kaynakla uyumlu.");
}
