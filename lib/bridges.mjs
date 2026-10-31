// Köprü tatil planlayıcı (30 Ekim 2026, rakip analizi #22): resmî tatiller, yarım gün arifeler ve hafta sonlarıyla
// "kaç gün izinle kaç gün tatil". Hafta sonu ve tam gün tatil izin gerektirmez; hafta içine düşen yarım gün (arife)
// 0,5 gün izin sayılır; diğer iş günleri 1 gün. Her tatil için en çok `maxLeave` gün izinle ulaşılabilen en uzun
// kesintisiz dönemler çıkarılır; daha fazla izin ancak tatili izin günü sayısından fazla uzatıyorsa listelenir.
import { dowOf, addDays } from "./events.mjs";

/** Tatil grubu: aynı bayramın günleri (arife dahil) tek grup */
const GROUP = (key) => (/^rb/.test(key) ? "rb" : /^kb/.test(key) ? "kb" : key === "oct28" ? "oct29" : key);

/**
 * @param {number} year
 * @param {Array<{date:string,key:string,type:string}>} events  bu yılın ve komşu yılların "holiday"/"half" günleri
 * @param {{maxLeave?:number, span?:number}} opt
 * @returns {Array<{group:string, groups:string[], from:string, to:string, days:number, leave:number, leaveDays:string[], halfDays:string[]}>}
 */
export function bridges(year, events, { maxLeave = 4, span = 9 } = {}) {
  const off = new Map(); // tarih → "holiday" | "half"
  for (const e of events) if (e.type === "holiday" || (e.type === "half" && off.get(e.date) !== "holiday")) off.set(e.date, e.type);
  const cost = (d) => (dowOf(d) >= 5 || off.get(d) === "holiday" ? 0 : off.get(d) === "half" ? 0.5 : 1);
  // Yılın tatil grupları (hafta içine en az bir tam gün düşenler)
  const groups = new Map();
  for (const e of events) {
    if (e.type !== "holiday" || !e.date.startsWith(String(year))) continue;
    const g = GROUP(e.key);
    if (!groups.has(g)) groups.set(g, []);
    groups.get(g).push(e.date);
  }
  const out = [];
  const seen = new Set();
  for (const [group, dates] of groups) {
    dates.sort();
    const first = dates[0], last = dates[dates.length - 1];
    if (dates.every((d) => dowOf(d) >= 5)) continue; // tamamı hafta sonuna denk gelen tatilde köprü yok
    // Her izin miktarı için en uzun pencere; pencerenin iki ucu izin gerektirmeyen ya da yarım gün olmalı
    const best = new Map();
    for (let i = 0; i <= span; i++) {
      const a = addDays(first, -i);
      if (cost(a) === 1) continue;
      for (let j = 0; j <= span; j++) {
        const b = addDays(last, j);
        if (cost(b) !== 0) continue;
        let c = 0, n = 0;
        for (let d = a; d <= b; d = addDays(d, 1)) { c += cost(d); n++; }
        if (c > maxLeave || n < 3) continue;
        const prev = best.get(c);
        if (!prev || n > prev.n || (n === prev.n && a > prev.a)) best.set(c, { a, b, n, c });
      }
    }
    // Daha fazla izin ancak toplam günü, eklenen izinden fazla artırıyorsa anlamlı
    let last2 = null;
    for (const c of [...best.keys()].sort((x, y) => x - y)) {
      const w = best.get(c);
      if (last2 && !(w.n - last2.n > c - last2.c)) continue;
      last2 = w;
      const id = w.a + w.b;
      if (seen.has(id)) continue;
      seen.add(id);
      const leaveDays = [], halfDays = [], inGroups = new Set([group]);
      for (let d = w.a; d <= w.b; d = addDays(d, 1)) {
        const k = cost(d);
        if (k === 1) leaveDays.push(d);
        else if (k === 0.5) halfDays.push(d);
        for (const e of events) if (e.date === d && e.type === "holiday") inGroups.add(GROUP(e.key));
      }
      out.push({ group, groups: [...inGroups], from: w.a, to: w.b, days: w.n, leave: w.c, leaveDays, halfDays });
    }
  }
  // Tatil sırasıyla (grubun ilk günü), aynı tatilde izin artan
  const firstOf = (g) => groups.get(g)[0];
  return out.sort((x, y) => (firstOf(x.group) < firstOf(y.group) ? -1 : firstOf(x.group) > firstOf(y.group) ? 1 : x.leave - y.leave));
}
