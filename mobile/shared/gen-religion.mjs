#!/usr/bin/env node
// Kur'an ve dua verisini uygulamalara yazar (iOS App/Resources, Android assets). Kaynaklar mobile/shared/religion/:
//   source/quran-simple.txt  Tanzil Projesi Arapça metni (Simple 1.1, CC BY 3.0: değiştirmeden, atıfla ve tanzil.net
//                            bağlantısıyla; telif bloğu türetilen dosyada aynen korunur)
//   source/en.pickthall.txt  M. M. Pickthall'ın İngilizce meali (1930; çevirmen 1936'da vefat etti, kamu malı)
//   source/quran-data.js     Tanzil sure ve cüz bilgisi (CC BY 3.0)
//   surahs-tr.json           Türkçe sure adları
//   duas.json                dualar (Kur'an'dan olanların Arapçası Tanzil metninden birebir alınır)
//   esma.json                Esmaü'l-Hüsna (99 isim, anlamlar dört dilde)
//   hadith.json              günün hadisi: çeviriler; Arapçası source/ara-nawawi.json'dan (Nevevî, Kırk Hadis;
//                            fawazahmed0/hadith-api, Unlicense) birebir, yalnız <br> boşluğa çevrilir
//   guide.json               namaz rehberi (Hanefî; beş dil)
//   sayings.json             günün sözü (her dilin atasözleri)
// Türkçe meal yok: özgün ve kamu malı bir dijital meal bulunamadı (Tanzil'deki Elmalılı metni sonradan sadeleştirilmiş
// bir baskıdır); uygulamalar Türkçe için Diyanet'in meal sitesine bağlanır. Kullanım: node gen-religion.mjs [--check]
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.join(HERE, "religion");
const ROOT = path.resolve(HERE, "../..");
const OUT = [
  path.join(ROOT, "mobile/ios/App/Resources"),
  path.join(ROOT, "mobile/android/app/src/main/assets"),
];
const check = process.argv.includes("--check");

/** "sure|ayet|metin" satırları ve sondaki # telif bloğu */
function readTanzil(file) {
  const raw = fs.readFileSync(path.join(SRC, "source", file), "utf8");
  const verses = [];
  const notice = [];
  for (const line of raw.split("\n")) {
    const m = line.match(/^(\d+)\|(\d+)\|(.*)$/);
    if (m) verses.push({ s: +m[1], a: +m[2], t: m[3].trim() });
    else if (line.startsWith("#")) notice.push(line);
  }
  return { verses, notice };
}

const ar = readTanzil("quran-simple.txt");
const en = readTanzil("en.pickthall.txt");
if (ar.verses.length !== 6236 || en.verses.length !== 6236) throw new Error("ayet sayısı 6236 olmalı");
ar.verses.forEach((v, i) => {
  if (v.s !== en.verses[i].s || v.a !== en.verses[i].a) throw new Error(`sıra farkı ${v.s}:${v.a}`);
});

// Sure bilgisi (quran-data.js): [start, ayas, order, rukus, name, tname, ename, type]
const js = fs.readFileSync(path.join(SRC, "source/quran-data.js"), "utf8");
const suraBlock = js.slice(js.indexOf("QuranData.Sura"), js.indexOf("QuranData.Sura") + js.slice(js.indexOf("QuranData.Sura")).indexOf("];"));
const suraRows = [...suraBlock.matchAll(/\[(\d+), (\d+), (\d+), (\d+), '([^']+)', "([^"]+)", '([^']+)', '(\w+)'\]/g)];
if (suraRows.length !== 114) throw new Error(`sure sayısı ${suraRows.length}`);
const trNames = JSON.parse(fs.readFileSync(path.join(SRC, "surahs-tr.json"), "utf8"));
const suras = suraRows.map((m, i) => [m[5], m[6], m[7], trNames[i], m[8] === "Meccan" ? "M" : "D", +m[2], +m[1]]);
const juzBlock = js.slice(js.indexOf("QuranData.Juz"));
const juz = [...juzBlock.slice(0, juzBlock.indexOf("];")).matchAll(/\[(\d+), (\d+)\]/g)].slice(0, 30).map((m) => [+m[1], +m[2]]);
if (juz.length !== 30) throw new Error("cüz sayısı 30 olmalı");
for (const [i, s] of suras.entries()) {
  const first = ar.verses[s[6]];
  if (first.s !== i + 1 || first.a !== 1) throw new Error(`sure başı ${i + 1}`);
}

const verse = (ref) => {
  const [s, a] = ref.split(":").map(Number);
  const i = suras[s - 1][6] + a - 1;
  return { ar: ar.verses[i].t, en: en.verses[i].t };
};

// Dualar: Kur'an'dan olanların Arapçası verilen sıradaki sözcükten ayet sonuna kadar (Tanzil metni değiştirilmeden)
const duaSrc = JSON.parse(fs.readFileSync(path.join(SRC, "duas.json"), "utf8"));
const duas = duaSrc.items.map((d) => {
  const out = { id: d.id, title: d.title, when: d.when, okunus: d.okunus };
  if (d.quran) {
    const parts = d.quran.map(([ref, word]) => {
      const v = verse(ref);
      const words = v.ar.split(" ");
      if (word >= words.length) throw new Error(`${d.id}: ${ref} ${word}. sözcük yok`);
      return { ar: words.slice(word).join(" "), en: v.en, ref };
    });
    out.ar = parts.map((p) => p.ar).join(" ۝ ");
    out.mean = { en: parts.map((p) => p.en).join(" ") };
    out.refs = parts.map((p) => p.ref);
  } else {
    out.ar = d.ar;
    out.mean = d.mean;
  }
  for (const k of ["title", "when"]) for (const l of ["tr", "en", "de", "az", "ar"]) if (!out[k][l]) throw new Error(`${d.id}.${k}.${l} yok`);
  return out;
});

// Esmaü'l-Hüsna, hadis, namaz rehberi, günün sözü (30 Ekim 2026)
const esma = JSON.parse(fs.readFileSync(path.join(SRC, "esma.json"), "utf8")).items;
if (esma.length !== 99 || esma.some((r) => r.length !== 7 || r.some((x) => !x))) throw new Error("esma.json: 99 satır, 7 alan olmalı");
const nawawi = JSON.parse(fs.readFileSync(path.join(SRC, "source/ara-nawawi.json"), "utf8")).hadiths;
const hadith = JSON.parse(fs.readFileSync(path.join(SRC, "hadith.json"), "utf8")).items.map((h) => {
  const src = nawawi.find((x) => x.hadithnumber === h.n);
  if (!src) throw new Error(`Kırk Hadis ${h.n} yok`);
  for (const l of ["tr", "en", "de", "az"]) if (!h.t[l]) throw new Error(`hadis ${h.n}: ${l} anlamı yok`);
  return { n: h.n, ar: src.text.replace(/<br>/g, " ").replace(/\s+/g, " ").trim(), r: h.r, s: h.s, t: h.t };
});
const guideSrc = JSON.parse(fs.readFileSync(path.join(SRC, "guide.json"), "utf8"));
const guide = { sections: guideSrc.sections, note: guideSrc.note };
const sayings = JSON.parse(fs.readFileSync(path.join(SRC, "sayings.json"), "utf8")).items;
for (const l of ["tr", "en", "de", "az", "ar"]) if (!(sayings[l] || []).length) throw new Error(`sayings.json: ${l} yok`);

const files = {
  "quran-ar.txt": ar.verses.map((v) => v.t).join("\n") + "\n\n" + ar.notice.join("\n") + "\n",
  "quran-en.txt": en.verses.map((v) => v.t).join("\n") + "\n\n" + en.notice.join("\n") + "\n",
  "quran-meta.json": JSON.stringify({ suras, juz }) + "\n",
  "duas.json": JSON.stringify(duas) + "\n",
  "esma.json": JSON.stringify(esma) + "\n",
  "hadith.json": JSON.stringify(hadith) + "\n",
  "guide.json": JSON.stringify(guide) + "\n",
  "sayings.json": JSON.stringify(sayings) + "\n",
};

let stale = 0;
for (const dir of OUT) {
  for (const [name, body] of Object.entries(files)) {
    const p = path.join(dir, name);
    if (check) {
      if (!fs.existsSync(p) || fs.readFileSync(p, "utf8") !== body) { console.error(`Eski: ${path.relative(ROOT, p)}`); stale++; }
    } else {
      fs.writeFileSync(p, body);
    }
  }
}
if (check) {
  if (stale) { console.error("node mobile/shared/gen-religion.mjs ile yeniden üretin"); process.exit(1); }
  console.log("Kur'an ve dua verisi güncel.");
} else {
  console.log(`Yazıldı: ${Object.keys(files).join(", ")} (${OUT.length} hedef)`);
}
