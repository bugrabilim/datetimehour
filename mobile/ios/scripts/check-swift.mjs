#!/usr/bin/env node
// iOS uygulama kaynakları için Linux'ta çalışan hızlı denetim (3 Kasım 2026). Mac olmadığı için uygulama yalnız CI'daki macOS
// makinesinde derlenebiliyor; en sık iki derleme hatasını burada, saniyeler içinde yakalar:
//  1) SaatCore'daki bir türü kullanıp `import SaatCore` yazmayan dosya ("cannot find 'CalendarDay' in scope"),
//  2) aynı hedefte iki dosyada aynı adla tanımlanan üst düzey tür ("invalid redeclaration").
// Kullanım: node mobile/ios/scripts/check-swift.mjs (hata varsa çıkış kodu 1). CI: mobile.yml "Ortak test verisi güncel".
import { readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative } from "node:path";

const ROOT = new URL("..", import.meta.url).pathname;
const swiftFiles = (dir) => readdirSync(dir).flatMap((n) => {
  const p = join(dir, n);
  return statSync(p).isDirectory() ? swiftFiles(p) : n.endsWith(".swift") ? [p] : [];
});
// Yorumları ve metin değişmezlerini at (kaba ama yeterli: çok satırlı """ metinler de)
const strip = (s) => s
  .replace(/"""[\s\S]*?"""/g, '""')
  .replace(/\/\*[\s\S]*?\*\//g, "")
  .replace(/\/\/.*$/gm, "")
  .replace(/"(?:\\.|[^"\\\n])*"/g, '""');

const DECL = /^(?:@\w+(?:\([^)]*\))?\s+)*((?:(?:public|private|fileprivate|internal|final|open)\s+)*)(?:struct|enum|class|protocol|actor|typealias)\s+([A-Z]\w*)/gm;

const core = new Set();
for (const f of swiftFiles(join(ROOT, "SaatCore/Sources"))) {
  for (const m of strip(readFileSync(f, "utf8")).matchAll(DECL)) if (/\bpublic\b|\bopen\b/.test(m[1])) core.add(m[2]);
}

// Hedefler: her biri kendi klasörleri (App/Sources'taki ortak dosyaları kullanan hedefler de olduğu gibi denetlenir)
const TARGETS = { App: ["App/Sources"], Widget: ["Widget/Sources"], Watch: ["Watch"], WatchWidget: ["WatchWidget"], Desk: ["Desk", "Mac", "TV", "Vision"] };
const problems = [];
for (const [target, dirs] of Object.entries(TARGETS)) {
  const files = dirs.flatMap((d) => { try { return swiftFiles(join(ROOT, d)); } catch { return []; } });
  const decls = new Map(); // ad → [{file, priv}]
  const own = new Set();
  const texts = files.map((f) => [f, strip(readFileSync(f, "utf8"))]);
  for (const [f, s] of texts) for (const m of s.matchAll(DECL)) {
    own.add(m[2]);
    if (!decls.has(m[2])) decls.set(m[2], []);
    decls.get(m[2]).push({ file: relative(ROOT, f), priv: /\b(private|fileprivate)\b/.test(m[1]) });
  }
  for (const [name, list] of decls) {
    const filesFor = new Set(list.map((x) => x.file));
    if (filesFor.size > 1 && list.some((x) => !x.priv)) problems.push(`${target}: '${name}' birden çok dosyada tanımlı: ${[...filesFor].join(", ")}`);
  }
  for (const [f, s] of texts) {
    if (/^\s*(@\w+\s+)?import\s+SaatCore\b/m.test(s)) continue;
    const used = [...core].filter((n) => !own.has(n) && new RegExp(`\\b${n}\\b`).test(s));
    if (used.length) problems.push(`${relative(ROOT, f)}: 'import SaatCore' yok ama ${used.slice(0, 4).join(", ")} kullanılıyor`);
  }
}
if (problems.length) {
  console.error(problems.join("\n"));
  process.exit(1);
}
console.log(`Swift denetimi: sorun yok (${core.size} SaatCore türü)`);
