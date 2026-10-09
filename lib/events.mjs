// Türkiye takvimi: sabit tarihli günler kuraldan, dini günler src/data/holidays.json'dan (Diyanet) gelir.
export const pad = (n) => String(n).padStart(2, "0");
export const iso = (y, m, d) => `${y}-${pad(m)}-${pad(d)}`;
const parse = (s) => s.split("-").map(Number);

/** Pazartesi=0 … Pazar=6 */
export const dowOf = (s) => {
  const [y, m, d] = parse(s);
  return (new Date(Date.UTC(y, m - 1, d)).getUTCDay() + 6) % 7;
};
export const daysInMonth = (y, m) => new Date(Date.UTC(y, m, 0)).getUTCDate();

/** Ayın n'inci haftanın günü (wd: Pazartesi=0) */
function nthWeekday(y, m, wd, n) {
  const first = dowOf(iso(y, m, 1));
  return 1 + ((wd - first + 7) % 7) + (n - 1) * 7;
}

const FIXED = [
  ["newYear", 1, 1, "holiday"], ["apr23", 4, 23, "holiday"], ["may1", 5, 1, "holiday"], ["may19", 5, 19, "holiday"],
  ["jul15", 7, 15, "holiday"], ["aug30", 8, 30, "holiday"], ["oct28", 10, 28, "half"], ["oct29", 10, 29, "holiday"],
  ["mar8", 3, 8, "special"], ["nov10", 11, 10, "special"], ["nov24", 11, 24, "special"],
];

/** Verilen yıl için { events: [{date,key,type}], ramadans: [{from,to}], hasReligious, projected } */
export function computeYear(year, data) {
  const events = FIXED.map(([key, m, d, type]) => ({ date: iso(year, m, d), key, type }));
  events.push({ date: iso(year, 5, nthWeekday(year, 5, 6, 2)), key: "mothers", type: "special" });
  events.push({ date: iso(year, 6, nthWeekday(year, 6, 6, 3)), key: "fathers", type: "special" });
  const h = data.years[String(year)];
  if (h) {
    const list = (v) => [].concat(v || []);
    const rel = (key, v) => list(v).forEach((date) => events.push({ date, key, type: "religious" }));
    rel("ucAylar", h.ucAylar); rel("regaib", h.regaib); rel("mirac", h.mirac); rel("berat", h.berat);
    rel("ramadanStart", h.ramadanStart); rel("kadir", h.kadir); rel("mawlid", h.mawlid); rel("hijri", h.hijri); rel("ashura", h.ashura);
    if (h.rbEve) events.push({ date: h.rbEve, key: "rbEve", type: "half" });
    list(h.rb).forEach((date, i) => events.push({ date, key: `rb${i + 1}`, type: "holiday" }));
    if (h.kbEve) events.push({ date: h.kbEve, key: "kbEve", type: "half" });
    list(h.kb).forEach((date, i) => events.push({ date, key: `kb${i + 1}`, type: "holiday" }));
  }
  const schoolRanges = [];
  for (const e of (data.school && data.school.events) || []) {
    if (!e.date.startsWith(String(year))) continue;
    events.push({ date: e.date, key: e.key, type: "school" });
    if (e.to) schoolRanges.push({ from: e.date, to: e.to });
  }
  events.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : a.key < b.key ? -1 : 1));
  const first = iso(year, 1, 1), last = iso(year, 12, 31);
  const ramadans = (data.ramadans || []).filter((r) => r.from <= last && (r.to || last) >= first);
  return { events, ramadans, schoolRanges, hasReligious: !!h, projected: !!(h && h.projected) };
}

export const inRange = (s, ranges) => ranges.some((r) => s >= r.from && s <= (r.to || "9999-12-31"));

/** Yıl özeti: resmi tatil günü sayısı ve hafta içine denk gelenler */
export function holidayCounts(events) {
  const days = [...new Set(events.filter((e) => e.type === "holiday").map((e) => e.date))];
  return { n: days.length, w: days.filter((d) => dowOf(d) < 5).length };
}

/* ---------- biçimlendirme (aylar ve gün adları i18n'den gelir) ---------- */
export function makeFmt(cal) {
  const fill = (t, v) => t.replace(/\{(\w+)\}/g, (_, k) => v[k]);
  const short = (s) => { const [, m, d] = parse(s); return fill(cal.shortFmt, { day: d, month: cal.months[m - 1] }); };
  const full = (s) => {
    const [y, m, d] = parse(s);
    return fill(cal.fullFmt, { weekday: cal.weekdays[dowOf(s)], day: d, month: cal.months[m - 1], year: y });
  };
  const range = (a, b) => {
    const [, m1, d1] = parse(a), [, m2, d2] = parse(b);
    if (a === b) return short(a);
    return m1 === m2 ? `${d1}–${d2} ${cal.months[m1 - 1]}` : `${short(a)} – ${short(b)}`;
  };
  return { short, full, range };
}

/* ---------- ISO hafta yardımcıları ---------- */
export const addDays = (s, n) => {
  const [y, m, d] = s.split("-").map(Number);
  const t = new Date(Date.UTC(y, m - 1, d + n));
  return iso(t.getUTCFullYear(), t.getUTCMonth() + 1, t.getUTCDate());
};
/** Yılın ISO 1. haftasının pazartesisi (4 Ocak'ı içeren hafta) */
export const isoWeek1Monday = (y) => addDays(iso(y, 1, 4), -dowOf(iso(y, 1, 4)));
/** ISO yılında 52 ya da 53 hafta */
export const isoWeeksInYear = (y) => {
  const jan1 = dowOf(iso(y, 1, 1));
  const leap = (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
  return jan1 === 3 || (leap && jan1 === 2) ? 53 : 52;
};
