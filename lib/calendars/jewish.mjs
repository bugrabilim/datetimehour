/* Yahudi (İbrani) takvimi (kapsayıcı takvimler, 9 Kasım 2026): hesap kurallarına dayanan (aritmetik) İbrani takvimi.
   Yıl başı (1 Tişri): molad (ortalama kavuşum; ay 29 gün 12 saat 793 helek, 1 saat = 1080 helek) ve dört erteleme kuralı
   (molad zaken: molad öğleden sonraysa ertesi gün; lo ADU: pazar, çarşamba, cuma olmaz; GaTaRaD ve BeTUTaKPaT: yıl 356 ya da
   382 gün olmasın diye). Artık yıl: (7y + 1) mod 19 < 7 (19 yıllık döngünün 3, 6, 8, 11, 14, 17, 19. yılları; Adar I eklenir).
   Yıl uzunluğu 353–355 ya da 383–385 gün; Heşvan ve Kislev'in 29/30 günü buna göre.
   Yahudi günü önceki akşam gün batımında başlar: olay tarihi ilk tam gündür (bayram önceki akşam başlar), ikinci tarih de günün
   gündüzüne göredir. Bayram süreleri diasporaya göredir (İsrail farkı metinlerdeki notta). Test verisi jewish.test.json
   (Hebcal sonuçlarıyla karşılaştırılır; Hebcal'in kodu kullanılmadı). */
import { addDays, dowOf } from "../events.mjs";

export const meta = {
  id: "jewish",
  group: "religious", // varsayılan her yerde kapalı (kimsenin inancı tahmin edilmez)
  regions: [],
  color: "#2F6FB3",
};

/* Ay numaraları (metinlerdeki month dizisinin sırası, 1'den): artık yılda Adar yerine Adar I ve Adar II */
export const M = { tishri: 1, heshvan: 2, kislev: 3, tevet: 4, shevat: 5, adar: 6, adar1: 7, adar2: 8, nisan: 9, iyar: 10, sivan: 11, tammuz: 12, av: 13, elul: 14 };

const HOUR = 1080; // helek
const DAY_PARTS = 24 * HOUR;
const MONTH_PARTS = 29 * DAY_PARTS + 12 * HOUR + 793; // ortalama ay
/* Sayım AM 1'den önceki pazar gününün başından (cumartesi akşamı 18.00; İbrani günü akşam başlar): gün 0 = pazar.
   AM 1 Tişri moladı BaHaRaD: 2. gün (pazartesi) 5 saat 204 helek. Molad zaken: molad günün 18. saatinden (öğleden) sonraysa
   Roş Aşana ertesi gündür; 6 saat ekleyip güne yuvarlamak bunu kendiliğinden yapar. */
const MOLAD_1 = 1 * DAY_PARTS + 5 * HOUR + 204;
const ZAKEN = 6 * HOUR;

export const isLeap = (y) => (7 * y + 1) % 19 < 7;
const monthsBefore = (y) => Math.floor((235 * y - 234) / 19); // AM 1 Tişri'den y yılının Tişri'sine kadar geçen ay

/** Sayımın başından y yılının Roş Aşana adayına (molad zaken ve lo ADU uygulanmış) kadar geçen gün */
function elapsed(y) {
  let day = Math.floor((MOLAD_1 + ZAKEN + monthsBefore(y) * MONTH_PARTS) / DAY_PARTS);
  // lo ADU Roş: 1 Tişri pazar (0), çarşamba (3) ya da cuma (5) olmaz → ertesi gün
  if ([0, 3, 5].includes(day % 7)) day++;
  return day;
}
/** GaTaRaD / BeTUTaKPaT: yıl 356 gün olacaksa bu yıl 2 gün ertelenir; önceki yıl 382 gün olacaksa bu yıl 1 gün */
function delay(y) {
  const a = elapsed(y - 1), b = elapsed(y), c = elapsed(y + 1);
  if (c - b === 356) return 2;
  if (b - a === 382) return 1;
  return 0;
}
/* Sayımın 0. günü (Unix günü, 1970-01-01 = 0): 1 Tişri AM 1 Jülyen takvimde MÖ 3761 (astronomik −3760) 7 Ekim pazartesidir;
   0. gün ondan önceki pazar, 6 Ekim. Değer jewish.test.json'daki tarihlerle doğrulanır. */
const EPOCH = -2092591;

/** y yılının 1 Tişri'si (Unix günü) */
export const newYearDay = (y) => EPOCH + elapsed(y) + delay(y);
export const yearLength = (y) => newYearDay(y + 1) - newYearDay(y);

const dayIso = (n) => new Date(n * 86400000).toISOString().slice(0, 10);

/** y yılının ayları: [[ay numarası, ilk gün (Unix günü), gün sayısı], …] Tişri'den Elul'e */
export function yearMonths(y) {
  const len = yearLength(y), leap = isLeap(y);
  const heshvan = len % 10 === 5 ? 30 : 29; // 355/385 gün: "tam" yıl, Heşvan 30
  const kislev = len % 10 === 3 ? 29 : 30; // 353/383 gün: "eksik" yıl, Kislev 29
  const list = [[M.tishri, 30], [M.heshvan, heshvan], [M.kislev, kislev], [M.tevet, 29], [M.shevat, 30],
    ...(leap ? [[M.adar1, 30], [M.adar2, 29]] : [[M.adar, 29]]),
    [M.nisan, 30], [M.iyar, 29], [M.sivan, 30], [M.tammuz, 29], [M.av, 30], [M.elul, 29]];
  let day = newYearDay(y);
  const out = list.map(([m, n]) => { const r = [m, day, n]; day += n; return r; });
  if (day !== newYearDay(y + 1)) throw new Error(`İbrani takvimi: ${y} yılının ayları yıl uzunluğuna uymuyor`);
  return out;
}

/** İbrani tarih → ISO tarih. Purim gibi "Adar" günleri artık yılda Adar II'dedir (month = M.adar verilirse). */
export function hebrewToIso(y, month, day) {
  const ms = yearMonths(y);
  const want = month === M.adar && isLeap(y) ? M.adar2 : month;
  const r = ms.find((x) => x[0] === want);
  if (!r || day < 1 || day > r[2]) throw new Error(`İbrani takvimi: geçersiz tarih ${y}/${month}/${day}`);
  return dayIso(r[1] + day - 1);
}

/** İbrani yılın olayları (yalnız o yılın; tarih ilk tam gün, end dahil) */
function yearEvents(y) {
  const h = (m, d) => hebrewToIso(y, m, d);
  const tishaBav = h(M.av, 9);
  // Yom HaŞoa: 27 Nisan; cumaya düşerse perşembeye, pazara düşerse pazartesiye (İsrail Knesset kuralı)
  let shoah = h(M.nisan, 27);
  if (dowOf(shoah) === 4) shoah = addDays(shoah, -1);
  else if (dowOf(shoah) === 6) shoah = addDays(shoah, 1);
  return [
    { key: "roshHashana", date: h(M.tishri, 1), end: h(M.tishri, 2) },
    { key: "yomKippur", date: h(M.tishri, 10) },
    { key: "sukkot", date: h(M.tishri, 15), end: h(M.tishri, 21) },
    { key: "sheminiAtzeret", date: h(M.tishri, 22) },
    { key: "simchatTorah", date: h(M.tishri, 23) }, // diaspora; İsrail'de Şemini Atzeret ile aynı gün
    { key: "hanukkah", date: h(M.kislev, 25), end: addDays(h(M.kislev, 25), 7) }, // 8 gün; Kislev 29 ya da 30 gün
    { key: "tuBishvat", date: h(M.shevat, 15) },
    { key: "purim", date: h(M.adar, 14) }, // artık yılda 14 Adar II
    { key: "pesach", date: h(M.nisan, 15), end: h(M.nisan, 22) }, // diaspora 8 gün; İsrail'de 7
    { key: "yomHashoah", date: shoah },
    { key: "lagBaomer", date: h(M.iyar, 18) },
    { key: "shavuot", date: h(M.sivan, 6), end: h(M.sivan, 7) }, // diaspora 2 gün; İsrail'de 1
    // Tişa BeAv: 9 Av cumartesiye düşerse oruç ertesi gün (10 Av, pazar)
    { key: "tishaBav", date: dowOf(tishaBav) === 5 ? addDays(tishaBav, 1) : tishaBav },
  ];
}

/** Gregoryen yılda başlayan olaylar: o yılın ilk aylarında önceki İbrani yılın (Tevet–Elul), sonbaharda yeni yılın olayları */
export function events(year) {
  const y = year + 3760; // Ocak'taki İbrani yıl
  return [...yearEvents(y), ...yearEvents(y + 1)]
    .filter((e) => e.date.startsWith(String(year)))
    .map((e) => (e.end ? { date: e.date, key: e.key, end: e.end } : { date: e.date, key: e.key }));
}

/** Ay tablosu: [ay başı, AM yılı, ay numarası, 0]; fromYear-01-01'den önceki son ay başından toYear-12-31'den sonraki ilk ay başına */
export function months(fromYear, toYear) {
  const rows = [];
  for (let y = fromYear + 3759; y <= toYear + 3762; y++) for (const [m, day] of yearMonths(y)) rows.push([dayIso(day), y, m, 0]);
  const first = `${fromYear}-01-01`, last = `${toYear}-12-31`;
  const a = rows.findLastIndex((r) => r[0] <= first), b = rows.findIndex((r) => r[0] > last);
  return rows.slice(a, b + 1);
}
