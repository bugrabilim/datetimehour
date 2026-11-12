/* Kutlama e-postaları (standart madde 7): doğum günü, üyelik yıl dönümü, yılbaşı ve profildeki ülkenin resmi
   bayramları. Uç günde bir çağrılır (saat fark etmez): yerel saati önümüzdeki 24 saat içinde 09:00'a gelecek üyelerin
   kutlamasını Resend'e zamanlanmış gönderimle (scheduled_at) bırakır; aynı üye ve gün için ikinci kez bırakmaz.
   Türkiye: Ramazan ve Kurban Bayramı (Diyanet takvimi), 23 Nisan, 1 Mayıs, 19 Mayıs, 30 Ağustos, 29 Ekim.
   Öbür ülkeler: date-holidays, `public` türü; anma ve yas günleri adına bakılarak ayıklanır, şüpheli gün gönderilmez.
   Yalnız ana ürün (anaUrun) gönderir; kapatılmış tercihte ve askıdaki hesapta gönderilmez. */
import { readFileSync } from "node:fs";
import Holidays from "date-holidays";
import { profileOf } from "./accounts.mjs";

const TR = JSON.parse(readFileSync(new URL("../data/tr-days.json", import.meta.url), "utf8"));

/* İngilizce adlarda anma, yas, şehit ve ölüm günleri (şüpheli günde kutlama gönderilmez) */
export const SOMBER = /good friday|memor|mourn|remembr|martyr|victim|genocide|death|tragedy|ashura|fallen|hero|massacre|commemor|souls|\bdead\b|deceased|assassinat|disappear|veteran|day of memory|truth and justice|moncada|nkrumah|independence struggle/i;

/* Saat dilimindeki yerel tarih ve saat */
function localParts(date, tz) {
  const f = new Intl.DateTimeFormat("en-CA", { timeZone: tz, year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
  const p = Object.fromEntries(f.formatToParts(date).map((x) => [x.type, x.value]));
  return { day: `${p.year}-${p.month}-${p.day}`, hour: Number(p.hour), minute: Number(p.minute) };
}

/* Yerel "gün 09:00" anının UTC karşılığı (yaz saati değişimine dayanıklı: iki kez düzeltilir) */
export function localNineUtc(day, tz) {
  const [y, m, d] = day.split("-").map(Number);
  let t = Date.UTC(y, m - 1, d, 9, 0);
  for (let i = 0; i < 2; i++) {
    const lp = localParts(new Date(t), tz);
    const [ly, lm, ld] = lp.day.split("-").map(Number);
    const shown = Date.UTC(ly, lm - 1, ld, lp.hour, lp.minute);
    t += Date.UTC(y, m - 1, d, 9, 0) - shown;
  }
  return new Date(t);
}

const addDay = (day, n) => new Date(Date.parse(day + "T00:00:00Z") + n * 86400_000).toISOString().slice(0, 10);
const isLeap = (y) => (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;

/* Önümüzdeki 24 saat içindeki ilk yerel 09:00 (şu an 09:00'dan önceyse bugün, değilse yarın) */
export function nextNine(now, tz) {
  const lp = localParts(now, tz);
  const day = lp.hour < 9 ? lp.day : addDay(lp.day, 1);
  return { day, at: localNineUtc(day, tz) };
}

const hdCache = new Map();
function countryHolidays(country, year, lang) {
  const key = `${country}:${year}:${lang}`;
  if (!hdCache.has(key)) {
    let list = [];
    try {
      const h = new Holidays(country);
      const en = (h.getHolidays(year, "en") || []).filter((x) => x.type === "public");
      const local = lang === "en" ? en : h.getHolidays(year, lang) || [];
      list = en
        .filter((x) => !SOMBER.test(x.name) && !x.date.startsWith(`${year}-01-01`))
        .map((x) => ({ day: x.date.slice(0, 10), name: (local.find((y) => y.date === x.date && y.rule === x.rule) || x).name }));
    } catch {}
    hdCache.set(key, list);
  }
  return hdCache.get(key);
}

/* Bir üyenin o günkü kutlaması (öncelik: doğum günü, yıl dönümü, yılbaşı, bayram); yoksa null */
export function occasionFor(user, day, lang) {
  const md = day.slice(5);
  const year = Number(day.slice(0, 4));
  const prof = profileOf(user);
  if (prof.dogumTarihi) {
    let bmd = prof.dogumTarihi.slice(5);
    if (bmd === "02-29" && !isLeap(year)) bmd = "02-28";
    if (bmd === md) return { kind: "birthday" };
  }
  if (user.createdAt) {
    const c = localParts(new Date(user.createdAt), user.timeZone || "UTC").day;
    let cmd = c.slice(5);
    if (cmd === "02-29" && !isLeap(year)) cmd = "02-28";
    const years = year - Number(c.slice(0, 4));
    if (years >= 1 && cmd === md) return { kind: "anniversary", years };
  }
  if (md === "01-01") return { kind: "newyear" };
  if (prof.ulke === "TR") {
    const key = TR.bayram[day] || Object.keys(TR.sabit).find((k) => TR.sabit[k] === md);
    if (key) return { kind: "holiday", name: (TR.adlar[lang] || TR.adlar.en)[key] };
  } else if (prof.ulke) {
    const h = countryHolidays(prof.ulke, year, lang).find((x) => x.day === day);
    if (h) return { kind: "holiday", name: h.name };
  }
  return null;
}

export async function runCelebrations({ pool, mailer, now = new Date() }) {
  const out = { candidates: 0, scheduled: 0, failed: 0 };
  const r = await pool.query(
    `select id, email, lang, "timeZone", "createdAt", profile from "user"
     where "emailVerified" = true and celebrate = true and "anaUrun" = true and coalesce(banned, false) = false`,
  );
  for (const u of r.rows) {
    out.candidates++;
    const tz = u.timeZone || "UTC";
    let next;
    try { next = nextNine(now, tz); } catch { next = nextNine(now, "UTC"); }
    const occ = occasionFor(u, next.day, u.lang);
    if (!occ) continue;
    const ins = await pool.query(
      `insert into celebration_sent (user_id, day, kind) values ($1, $2, $3) on conflict do nothing`,
      [u.id, next.day, occ.kind],
    );
    if (!ins.rowCount) continue;
    const ad = profileOf(u).ad || "";
    try {
      await mailer.send("celebrate", u.email, u.lang, { ...occ, ad, userId: u.id }, { scheduledAt: next.at });
      out.scheduled++;
    } catch {
      await pool.query(`delete from celebration_sent where user_id = $1 and day = $2`, [u.id, next.day]);
      out.failed++;
    }
  }
  return out;
}
