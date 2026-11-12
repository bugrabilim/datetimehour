// Hicri takvim (30 Ekim 2026, rakip analizi #9): Diyanet'e hizalı ay başları tablosu. Uygulamalar (saat ekranı alanı,
// takvim gün ayrıntısı, Vakitler, widget'lar) bu tablodan okur; platformun Umm al-Qura takvimi Diyanet'ten bazı aylarda
// bir gün ayrıldığı için doğrudan kullanılmaz.
//
// Dayanaklar: Diyanet'in vakit tablolarındaki hicri ay başları (src/data/hijri.json, scripts/fetch-hijri.mjs) ve
// dini günler (src/data/holidays.json, Diyanet): 1 Muharrem (hijri), 10 Muharrem (ashura), 11 Rebiülevvel akşamı
// (mawlid), 1 Recep (ucAylar), 26 Recep akşamı (mirac), 14 Şaban akşamı (berat), 1 Ramazan (ramadanStart),
// 26 Ramazan akşamı (kadir), 1 Şevval (rb[0]), 10 Zilhicce (kb[0]). Kandil tarihleri gecenin başladığı akşamdır.
// Dayanağı olmayan aylar (Safer, Rebiülahir, Cemaziyelevvel, Cemaziyelahir, Zilkade) Umm al-Qura ay başıdır; ay
// 29 ya da 30 gün olacak biçimde komşu dayanaklara sıkıştırılır.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { addDays } from "./events.mjs";

const UQ = new Intl.DateTimeFormat("en-u-ca-islamic-umalqura", { year: "numeric", month: "numeric", day: "numeric", timeZone: "UTC" });
/** Gregoryen gün (YYYY-MM-DD) → Umm al-Qura [yıl, ay, gün] */
export function umalqura(iso) {
  const p = Object.fromEntries(UQ.formatToParts(new Date(iso + "T12:00:00Z")).map((x) => [x.type, x.value]));
  return [parseInt(p.year, 10), +p.month, +p.day];
}
const dayNum = (iso) => Math.round(Date.parse(iso + "T00:00:00Z") / 86400000);
const list = (v) => (v == null ? [] : Array.isArray(v) ? v : [v]);

/**
 * Ay başları tablosu: [[gregoryen ay başı, hicri yıl, ay], …]. Son satır tablonun bittiği (dışarıda kalan) ayın
 * başıdır; bir gün tablonun ilk satırından önce ya da son satırında ya da sonrasındaysa tarih bilinmez.
 * @param {{years: Record<string, any>, ramadans: Array<{from:string,to:string|null}>}} hol
 * @param {{months?: Record<string, string>}} [diyanet]  hicri "yıl-ay" → ay başı (Diyanet vakit tablosu)
 */
export function hijriTable(hol, diyanet = {}) {
  // Dayanaklar: gregoryen ay başı → [yıl, ay] (yıl, Umm al-Qura'nın o güne yakın tarihinden)
  const fixed = new Map(); // "y-m" → iso
  const put = (iso, day, month, year) => {
    if (!iso) return;
    const start = addDays(iso, -(day - 1));
    // Hicri yıl: ay başının 15 gün sonrasındaki Umm al-Qura tarihi (bir günlük farkta ay karışmasın)
    const [y, m] = umalqura(addDays(start, 15));
    if (m !== month || (year && y !== year)) throw new Error(`Hicri dayanak tutarsız: ${iso} (${day}/${month}), Umm al-Qura ${y}/${m}`);
    const key = y + "-" + m, prev = fixed.get(key);
    if (prev && prev !== start) throw new Error(`Hicri dayanak çelişiyor: ${key} ${prev} / ${start}`);
    fixed.set(key, start);
  };
  for (const [key, start] of Object.entries(diyanet.months || {})) put(start, 1, +key.split("-")[1], +key.split("-")[0]);
  for (const v of Object.values(hol.years)) {
    for (const d of list(v.hijri)) put(d, 1, 1);
    for (const d of list(v.ashura)) put(d, 10, 1);
    for (const d of list(v.mawlid)) put(d, 11, 3);
    for (const d of list(v.ucAylar)) put(d, 1, 7);
    for (const d of list(v.mirac)) put(d, 26, 7);
    for (const d of list(v.berat)) put(d, 14, 8);
    for (const d of list(v.ramadanStart)) put(d, 1, 9);
    for (const d of list(v.kadir)) put(d, 26, 9);
    if (v.rb) put(v.rb[0], 1, 10);
    if (v.kb) put(v.kb[0], 10, 12);
  }
  for (const r of hol.ramadans || []) {
    put(r.from, 1, 9);
    if (r.to) put(addDays(r.to, 1), 1, 10);
  }
  // Umm al-Qura ay başları: ilk dayanaktan son dayanağın ertesi ayına kadar
  const starts = [...fixed.values()].sort();
  const first = starts[0], last = starts[starts.length - 1];
  const uq = []; // [iso, y, m]
  for (let d = addDays(first, -2); d <= addDays(last, 62); d = addDays(d, 1)) {
    const [y, m, day] = umalqura(d);
    if (day === 1) uq.push([d, y, m]);
  }
  const startIdx = uq.findIndex(([, y, m]) => fixed.has(y + "-" + m));
  const months = uq.slice(startIdx);
  // Son dayanaktan sonraki ilk ay tablonun sonu
  const lastIdx = months.findIndex(([, y, m]) => fixed.get(y + "-" + m) === last);
  months.length = lastIdx + 2;
  const out = [];
  for (let i = 0; i < months.length; i++) {
    const [d, y, m] = months[i];
    const key = y + "-" + m;
    if (fixed.has(key)) { out.push([fixed.get(key), y, m]); continue; }
    // Dayanaksız ay: önceki ay 29–30 gün, sonraki ay dayanaklıysa bu ay da 29–30 gün
    let lo = -Infinity, hi = Infinity;
    if (out.length) { const p = dayNum(out[out.length - 1][0]); lo = p + 29; hi = p + 30; }
    const n = months[i + 1];
    if (n && fixed.has(n[1] + "-" + n[2])) { const f = dayNum(fixed.get(n[1] + "-" + n[2])); lo = Math.max(lo, f - 30); hi = Math.min(hi, f - 29); }
    if (lo > hi) throw new Error(`Hicri ay başı bulunamadı: ${key}`);
    const s = Math.min(hi, Math.max(lo, dayNum(d)));
    out.push([addDays(d, s - dayNum(d)), y, m]);
  }
  // Denetim: aylar sıralı ve 29–30 gün (son satır tablonun sonu)
  for (let i = 1; i < out.length; i++) {
    const len = dayNum(out[i][0]) - dayNum(out[i - 1][0]);
    if (len < 29 || len > 30) throw new Error(`Hicri ay uzunluğu ${len}: ${out[i - 1].join(" ")}`);
  }
  return out;
}

/** Tablodan hicri tarih: [yıl, ay, gün] ya da tablo dışında null */
export function hijriDate(table, iso) {
  const n = dayNum(iso);
  for (let i = table.length - 2; i >= 0; i--) {
    const s = dayNum(table[i][0]);
    if (n >= s) return n < dayNum(table[i + 1][0]) ? [table[i][1], table[i][2], n - s + 1] : null;
  }
  return null;
}

/* Endonezya hicri takvimi (10 Kasım 2026; docs/arastirma-endonezya-2026-11.md 1.5): Kemenag'ın MABIMS ölçütü. Ayın 29'u
   akşamı (kavuşum günü) Banda Aceh'te gün batımında yer merkezli hilal yüksekliği ≥ 3° ve uzanım ≥ 6,4° ise ertesi gün ay
   başıdır, değilse ay 30 gün tamamlanır; kavuşum gün batımından sonraysa ay başı iki gün sonradır. Dayanaklar
   (src/data/hijri-id.json: Kemenag takvimi, isbat kararları, SKB tatilleri) hesabın önüne geçer; hesap hepsini tutturmalı. */
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
function natureCalc() {
  const m = { exports: {} };
  new Function("module", "window", fs.readFileSync(path.join(ROOT, "src/js/nature-calc.js"), "utf8"))(m, undefined);
  return m.exports;
}
const MABIMS = { lat: 5.55, lon: 95.32, tz: 7, alt: 3, elong: 6.4 };
const D2R = Math.PI / 180;
const isoAt = (ms, tz) => new Date(ms + tz * 3600000).toISOString().slice(0, 10);
/** Kavuşum anından (ms) Endonezya'da yeni ayın ilk günü (MABIMS) */
export function mabimsStart(N, conjMs) {
  const D = isoAt(conjMs, MABIMS.tz);
  const ev = N.crescent(conjMs, MABIMS.lat, MABIMS.lon, 1)[0];
  if (!ev || isoAt(ev.sunset, MABIMS.tz) !== D) return addDays(D, 2);
  const m = N.moonPos(ev.sunset), s = N.sunPos(ev.sunset), alt = N.altAz(ev.sunset, m.ra, m.dec, MABIMS.lat, MABIMS.lon).alt;
  const c = Math.sin(m.dec * D2R) * Math.sin(s.dec * D2R) + Math.cos(m.dec * D2R) * Math.cos(s.dec * D2R) * Math.cos((m.ra - s.ra) * D2R);
  const elong = Math.acos(Math.max(-1, Math.min(1, c))) / D2R;
  return addDays(D, alt >= MABIMS.alt && elong >= MABIMS.elong ? 1 : 2);
}
/**
 * Endonezya ay başları tablosu, `like` tablosuyla (Diyanet) aynı aralıkta: [[ay başı, hicri yıl, ay], …]; son satır
 * tablonun sonu. Dayanak hesapla çelişirse dayanak kullanılır ama ayrılan dayanaklar `mismatch` listesinde döner.
 */
export function hijriTableId(like, anchors = {}) {
  const N = natureCalc();
  const kFor = (ms) => Math.floor((ms / 86400000 + 2440587.5 - 2451550.09766) / 29.530588861);
  const anchorDates = Object.values(anchors.months || {}).sort();
  const from = addDays([like[0][0], anchorDates[0] || like[0][0]].sort()[0], -40), to = addDays(like[like.length - 1][0], 40);
  const starts = [];
  for (let k = kFor(Date.parse(from)); k <= kFor(Date.parse(to)) + 1; k++) starts.push(mabimsStart(N, N.phaseMs(k, false)));
  const rows = starts.map((d) => { const [y, m] = umalqura(addDays(d, 15)); return [d, y, m]; });
  const mismatch = [];
  for (const r of rows) {
    const a = (anchors.months || {})[r[1] + "-" + r[2]];
    if (a && a !== r[0]) { mismatch.push(`${r[1]}-${r[2]} ${a} / ${r[0]}`); r[0] = a; }
  }
  for (const key of Object.keys(anchors.months || {})) if (!rows.some((r) => r[1] + "-" + r[2] === key)) throw new Error(`Endonezya hicri dayanağı tabloda yok: ${key}`);
  const first = like[0][1] + "-" + like[0][2], last = like[like.length - 1][1] + "-" + like[like.length - 1][2];
  const i0 = rows.findIndex((r) => r[1] + "-" + r[2] === first), i1 = rows.findIndex((r) => r[1] + "-" + r[2] === last);
  if (i0 < 0 || i1 < 0) throw new Error("Endonezya hicri tablosu aralığı bulunamadı");
  const out = rows.slice(i0, i1 + 1);
  for (let i = 1; i < out.length; i++) {
    const len = dayNum(out[i][0]) - dayNum(out[i - 1][0]);
    if (len < 29 || len > 30) throw new Error(`Endonezya hicri ay uzunluğu ${len}: ${out[i - 1].join(" ")}`);
  }
  out.mismatch = mismatch;
  return out;
}

