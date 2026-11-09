/* Kore ay takvimi (음력; kapsayıcı takvimler, 9 Kasım 2026): Çin ay takviminin (chinese.mjs) aynı astronomik kuralı Kore saatiyle
   (UTC+9). Yeni ay Çin'de gece yarısından önce, Kore'de sonra düşerse ay Kore'de bir gün sonra başlar (2025–2032'de 5 ay:
   2026/9, 2027/1, 2028/1, 2029/6, 2031/2); bu yüzden Kore ayrı takvimdir. Hesap chinese.mjs'teki yardımcılarla (kopyalanmaz).
   Olaylar: Seollal (설날, önceki ve sonraki günle üç günlük resmî tatil), Jeongwol Daeboreum (1/15), Hansik (동지'den 105 gün
   sonra, KASI tanımı), Dano (5/5), Chilseok (7/7), Chuseok (8/15, üç gün), Dongji (kış gündönümü, Kore günü).
   Günlük kod: 24 güneş dönemi (24절기) "s1" (입춘) … "s24" (대한), dönemin başladığı Kore günü.
   Doğrulama: korean.test.json (Kore Astronomi ve Uzay Bilimi Enstitüsü (KASI) 월력요항 2025–2028 ve 음양력 변환 2024–2033,
   USNO ekinoks/gündönümü anları 2029–2032). */
import { addDays } from "../events.mjs";
import { lunisolarMonths, fromLunar, solarTerms, localDay, sunLongitudeMs } from "./chinese.mjs";

export const meta = {
  id: "korean",
  group: "cultural", // yalnız bölgesinde varsayılan açık
  regions: ["KR"],
  color: "#5563D6", // kobalt mavisi (청색); Hristiyan morundan ΔE 26, Yahudi mavisinden ve diğerlerinden ayrı
};

const OFF = 9; // Kore saati (UTC+9)

const cache = new Map();
const table = (year) => {
  if (!cache.has(year)) cache.set(year, lunisolarMonths(year - 1, year + 1, OFF));
  return cache.get(year);
};
/** Kış gündönümünün (Güneş boylamı 270°) Kore günü */
const dongji = (year) => localDay(sunLongitudeMs(270, Date.UTC(year, 11, 21)), OFF);

export function events(year) {
  const t = table(year), list = [];
  const add = (key, date) => { if (date && date.startsWith(String(year))) list.push({ date, key }); };
  const seol = fromLunar(year, 1, 1, t), chu = fromLunar(year, 8, 15, t);
  // Seollal ve Chuseok: önceki ve sonraki gün de resmî tatil (설날 연휴, 추석 연휴)
  add("seollalHol", addDays(seol, -1)); add("seollal", seol); add("seollalHol", addDays(seol, 1));
  add("daeboreum", fromLunar(year, 1, 15, t));
  add("hansik", addDays(dongji(year - 1), 105));
  add("dano", fromLunar(year, 5, 5, t));
  add("chilseok", fromLunar(year, 7, 7, t));
  add("chuseokHol", addDays(chu, -1)); add("chuseok", chu); add("chuseokHol", addDays(chu, 1));
  add("dongji", dongji(year));
  return list;
}

/** İkinci tarih: Kore saatiyle ay tablosu (artık ay bayrağı 1 → leapFmt "윤{m}") */
export function months(fromYear, toYear) {
  return lunisolarMonths(fromYear, toYear, OFF);
}

/** 24 güneş dönemi: dönemin başladığı Kore günü → "s1" (입춘) … "s24" (대한) */
export function daily(year) {
  const out = {};
  for (const t of solarTerms(year)) out[localDay(t.ms, OFF)] = `s${t.n}`;
  return out;
}
