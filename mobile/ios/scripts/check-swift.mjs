#!/usr/bin/env node
// iOS uygulama kaynakları için Linux'ta çalışan hızlı denetim (3 Kasım 2026). Mac olmadığı için uygulama yalnız CI'daki macOS
// makinesinde derlenebiliyor; en sık iki derleme hatasını burada, saniyeler içinde yakalar:
//  1) SaatCore'daki bir türü kullanıp `import SaatCore` yazmayan dosya ("cannot find 'CalendarDay' in scope"),
//  2) aynı hedefte iki dosyada aynı adla tanımlanan üst düzey tür ("invalid redeclaration"),
//  3) dengesiz süslü parantez ("extraneous '}' at top level", 3 Kasım 2026'da CI'da yakalandı),
//  4) SaatCore'daki bir türle aynı adlı uygulama türü ("'Planet' is not a member type of struct 'Sky'", 4 Kasım 2026).
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

// "if … { } .padding()" gibi bir blok kapanışından sonra yeni satırda başlayan modifier: SwiftUI'de görünüme bağlanmaz,
// derleyici "ifade çok karmaşık" hatasıyla uzun süre takılır (3 Kasım 2026, DialGallery.swift). Satır no ya da null.
function danglingModifier(src) {
  const lines = src.split("\n");
  const stack = [];
  for (let i = 0; i < lines.length; i++) {
    const code = lines[i].replace(/"(?:\\.|[^"\\])*"/g, '""').replace(/\/\/.*$/, "");
    for (const ch of code) {
      if (ch === "{") stack.push(i);
      else if (ch === "}" && stack.length) {
        const open = stack.pop();
        if (code.trim() !== "}") continue;
        if (!/^(if |\} else|else|for |switch )/.test(lines[open].trim())) continue;
        let j = i + 1;
        while (j < lines.length && !lines[j].trim()) j++;
        if (j < lines.length && lines[j].trim().startsWith(".")) return j + 1;
      }
    }
  }
  return null;
}

// Süslü parantez dengesi: yorumları (iç içe blok yorumlar dahil), metinleri ("…", """…""", #"…"#) ve metin içindeki
// \( … ) aralığını doğru tanıyan küçük tarayıcı. Sorun yoksa null, varsa açıklama döner.
function braces(src) {
  let i = 0, line = 1, depth = 0, bad = 0;
  const n = src.length;
  function code(stopParen) {
    let paren = 0;
    while (i < n) {
      const c = src[i];
      if (c === "\n") { line++; i++; continue; }
      if (c === "/" && src[i + 1] === "/") { while (i < n && src[i] !== "\n") i++; continue; }
      if (c === "/" && src[i + 1] === "*") { let d = 0; do { if (src[i] === "/" && src[i + 1] === "*") { d++; i += 2; } else if (src[i] === "*" && src[i + 1] === "/") { d--; i += 2; } else { if (src[i] === "\n") line++; i++; } } while (i < n && d > 0); continue; }
      if (c === '"' || (c === "#" && /^#+"/.test(src.slice(i, i + 8)))) { str(); continue; }
      if (stopParen) { if (c === "(") paren++; else if (c === ")") { if (paren === 0) { i++; return; } paren--; } }
      if (c === "{") depth++;
      else if (c === "}" && --depth < 0 && !bad) bad = line;
      i++;
    }
  }
  function str() {
    let hashes = 0;
    while (src[i] === "#") { hashes++; i++; }
    const multi = src.startsWith('"""', i);
    i += multi ? 3 : 1;
    const close = (multi ? '"""' : '"') + "#".repeat(hashes);
    const esc = "\\" + "#".repeat(hashes);
    while (i < n) {
      if (src.startsWith(close, i)) { i += close.length; return; }
      if (src.startsWith(esc, i)) {
        i += esc.length;
        if (src[i] === "(") { i++; code(true); } else i++;
        continue;
      }
      if (src[i] === "\n") { line++; if (!multi) return; }
      i++;
    }
  }
  code(false);
  return bad ? `satır ${bad}` : depth !== 0 ? `sonda ${depth} açık` : null;
}

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
    // Uygulamadaki üst düzey tür SaatCore'daki aynı adlı türü gölgeler: `Sky.Planet` gibi kullanımlar bozulur
    // (4 Kasım 2026'da WorshipExtras.swift'teki `Sky` yapısı CI'da yakalandı)
    if (core.has(name) && list.some((x) => !x.priv)) problems.push(`${target}: '${name}' SaatCore'daki aynı adlı türü gölgeliyor: ${list.map((x) => x.file).join(", ")}`);
    const filesFor = new Set(list.map((x) => x.file));
    if (filesFor.size > 1 && list.some((x) => !x.priv)) problems.push(`${target}: '${name}' birden çok dosyada tanımlı: ${[...filesFor].join(", ")}`);
  }
  for (const [f, s] of texts) {
    const raw = readFileSync(f, "utf8");
    const bal = braces(raw);
    if (bal) problems.push(`${relative(ROOT, f)}: süslü parantez dengesiz (${bal})`);
    const dangling = danglingModifier(raw);
    if (dangling) problems.push(`${relative(ROOT, f)}:${dangling}: 'if/else/for/switch' bloğundan sonra başıboş '.modifier' (görünüme değil bloğa bağlanmış)`);
    if (/^\s*(@\w+\s+)?import\s+SaatCore\b/m.test(s)) continue;
    const used = [...core].filter((n) => !own.has(n) && new RegExp(`\\b${n}\\b`).test(s));
    if (used.length) problems.push(`${relative(ROOT, f)}: 'import SaatCore' yok ama ${used.slice(0, 4).join(", ")} kullanılıyor`);
  }
}
// 5) Hesapla eşitlenen kayıtların türleri iki platformda aynı olmalı (12 Kasım 2026): iOS AccountSync.swift ile Android
//    AccountSync.kt'deki s/i/b listeleri (yanlış türle yazılan kayıt Android'de çöker)
{
  const lists = (src) => Object.fromEntries([...src.matchAll(/(?:let|val) ([sib])(?:: \w+)? = ((?:"[^"]*"\s*\+?\s*)+)/g)]
    .map((m) => [m[1], [...m[2].matchAll(/"([^"]*)"/g)].map((x) => x[1]).join("").split(/\s+/).filter(Boolean).sort().join(" ")]));
  const ios = lists(readFileSync(join(ROOT, "App/Sources/AccountSync.swift"), "utf8"));
  const android = lists(readFileSync(join(ROOT, "../android/app/src/main/kotlin/tr/bumba/time/AccountSync.kt"), "utf8"));
  for (const k of ["s", "i", "b"]) if (!ios[k] || ios[k] !== android[k]) problems.push(`AccountSync: '${k}' türündeki anahtarlar iOS ile Android'de farklı`);
}
if (problems.length) {
  console.error(problems.join("\n"));
  process.exit(1);
}
console.log(`Swift denetimi: sorun yok (${core.size} SaatCore türü)`);
