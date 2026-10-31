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
