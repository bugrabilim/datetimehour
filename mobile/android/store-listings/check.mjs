#!/usr/bin/env node
// Play özel mağaza sayfalarının metinlerini denetler: başlık ≤30, kısa açıklama ≤80, tam açıklama ≤4000 karakter;
// Play meta veri kuralı (emoji, BÜYÜK HARF, fiyat/sıralama/tanıtım sözcükleri yok); rakamlar Latin; her dosya tek satır sonuyla biter.
// Kullanım: node mobile/android/store-listings/check.mjs
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, dirname } from "node:path";

const HERE = dirname(new URL(import.meta.url).pathname);
const LIMITS = { "title.txt": 30, "short_description.txt": 80, "full_description.txt": 4000 };
const BANNED = /(\bfree\b|\bbest\b|\bnew\b|#1|\btop\b|ücretsiz|\ben iyi\b|\byeni\b|pulsuz|ən yaxşı|\bgratis\b|gratuit|meilleur|nouveau|бесплатн|лучш|(?<!\p{L})нов(ый|ое|ая|ые)(?!\p{L})|مجان|الأفضل|جديد|\bbaru\b|percuma|terbaik|مفت|بہترین|رایگان|بهترین|جدید|বিনামূল্যে|সেরা|নতুন|grátis|\bnovo\b|melhor)/iu;
const EMOJI = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{2B00}-\u{2BFF}\u{FE0F}]/u;
const CAPS_OK = new Set(["ISNA", "MWL", "HIIT"]);
let bad = 0, n = 0;
const fail = (m) => { console.error(m); bad++; };

for (const group of readdirSync(HERE).filter((d) => statSync(join(HERE, d)).isDirectory()).sort()) {
  for (const lang of readdirSync(join(HERE, group)).sort()) {
    for (const [file, limit] of Object.entries(LIMITS)) {
      const p = join(HERE, group, lang, file);
      let raw;
      try { raw = readFileSync(p, "utf8"); } catch { fail(`${group}/${lang}/${file}: yok`); continue; }
      if (!raw.endsWith("\n") || raw.endsWith("\n\n")) fail(`${group}/${lang}/${file}: tek satır sonuyla bitmeli`);
      const s = raw.replace(/\n$/, "");
      const len = [...s].length;
      if (len > limit || s.length > limit) fail(`${group}/${lang}/${file}: ${len} karakter (UTF-16 ${s.length}) > ${limit}`);
      if (file !== "full_description.txt" && s.includes("\n")) fail(`${group}/${lang}/${file}: tek satır olmalı`);
      const m = s.match(BANNED);
      if (m) fail(`${group}/${lang}/${file}: Play kuralına aykırı sözcük "${m[0]}"`);
      if (EMOJI.test(s)) fail(`${group}/${lang}/${file}: emoji`);
      const caps = (s.match(/\b[A-ZÇĞİÖŞÜ]{4,}\b/g) ?? []).filter((w) => !CAPS_OK.has(w));
      if (caps.length) fail(`${group}/${lang}/${file}: büyük harf ${caps.join(", ")}`);
      if (/[٠-٩۰-۹০-৯०-९]/.test(s)) fail(`${group}/${lang}/${file}: Latin olmayan rakam`);
      n++;
    }
  }
}
console.log(bad ? `${bad} sorun` : `${n} dosya, sorun yok`);
process.exit(bad ? 1 : 0);
