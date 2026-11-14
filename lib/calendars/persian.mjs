/* İran takvimi (Hicri Şemsi; kapsayıcı takvimler, 9 Kasım 2026): İran'ın resmî güneş takvimi ve kültür günleri.
   Kural (1925 yasası; Tahran Üniversitesi Takvim Merkezi): ilkbahar ekinoksu İran saatiyle (UTC+3:30, 52,5° D boylamının
   ortalama öğlesi) öğleden önceyse o gün, sonraysa ertesi gün 1 Ferverdin. Ekinoks src/js/nature-calc.js'ten (Meeus bölüm 27;
   USNO ile en çok 1 dk fark). Ekinoks öğleye 10 dakikadan yakınsa hesap durur (o yıl elle doğrulanmalı; 2024–2034'te en yakın
   2029: 11:31, 2025: 12:31). Aylar: 1–6 31 gün, 7–11 30 gün, Esfend bir sonraki Nevruz'a kadar (29 ya da 30).
   Yalnız kültür günleri: Nevruz (1–4 Ferverdin), Sizdebedar (13 Ferverdin), Çarşamba Suri (yılın son çarşambasından önceki
   salı akşamı), Tirgan (13 Tir), Mihrican (10 Mehr), Yelda gecesi (30 Azer), Sede (10 Behmen). İran'ın dini ve siyasi resmî
   günleri bilerek yok. Yıl etiketi yok: Şemsi yıl mart ayında değiştiği için yıl ikinci tarihte ({y}) yazar.
   Test verisi persian.test.json. */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { addDays, dowOf } from "../events.mjs";

export const meta = {
  id: "persian",
  group: "cultural", // İran, Afganistan ve Tacikistan'da varsayılan açık
  regions: ["IR", "AF", "TJ"],
  color: "#1B8F84", // firuze
};

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
let nature = null;
const seasons = (y) => {
  if (!nature) {
    const m = { exports: {} };
    new Function("module", "window", fs.readFileSync(path.join(ROOT, "src/js/nature-calc.js"), "utf8"))(m, undefined);
    nature = m.exports;
  }
  return nature.seasons(y);
};

const IRST_MS = 3.5 * 3600000; // İran Standart Saati (UTC+3:30; yaz saati 2022'den beri yok, takvim kuralında zaten kullanılmaz)
const NOON_GUARD_MS = 10 * 60000;

/** Gregoryen yıl y'de başlayan Şemsi yılın 1 Ferverdin'i (YYYY-MM-DD) */
export function nowruz(y) {
  const t = seasons(y).march + IRST_MS; // İran saatiyle ekinoks anı (UTC alanlarında okunur)
  const day = new Date(Math.floor(t / 86400000) * 86400000);
  const sinceMidnight = t - day.getTime(), noon = 12 * 3600000;
  if (Math.abs(sinceMidnight - noon) < NOON_GUARD_MS) throw new Error(`persian: ${y} ekinoksu öğleye çok yakın, elle doğrulayın`);
  const d = day.toISOString().slice(0, 10);
  return sinceMidnight < noon ? d : addDays(d, 1);
}

const MONTH_DAYS = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30];
/** Şemsi yıl P'nin ay başları: [[tarih, P, ay], …] (12 ay) */
function yearMonths(P) {
  let d = nowruz(P + 621);
  const out = [];
  for (let m = 1; m <= 12; m++) {
    out.push([d, P, m]);
    if (m < 12) d = addDays(d, MONTH_DAYS[m - 1]);
  }
  return out;
}
/** Şemsi tarih → Gregoryen (ay içi gün 1'den) */
const persianDate = (P, m, d) => addDays(yearMonths(P)[m - 1][0], d - 1);

/** Ay tablosu: 1 Ocak from'u kapsayan aydan to'nun sonrasındaki ilk ay başına (tablonun sonu) kadar */
export function months(from, to) {
  const rows = [];
  for (let P = from - 622; P <= to - 621; P++) for (const r of yearMonths(P)) rows.push([r[0], r[1], r[2], 0]);
  rows.push([nowruz(to + 1), to - 620, 1, 0]);
  const first = `${from}-01-01`, last = `${to}-12-31`;
  let a = rows.findIndex((r) => r[0] > first) - 1;
  const b = rows.findIndex((r) => r[0] > last);
  if (a < 0) a = 0;
  return rows.slice(a, b + 1);
}

/** Yılın son çarşambasından (1 Ferverdin'den önceki son çarşamba) önceki salı: Çarşamba Suri akşamı */
function chaharshanbeSuri(nowruzDate) {
  const back = ((dowOf(nowruzDate) - 2 + 7) % 7) || 7; // Pazartesi=0 … çarşamba=2
  return addDays(nowruzDate, -back - 1);
}

export function events(year) {
  const list = [];
  for (const P of [year - 622, year - 621]) {
    const nw = nowruz(P + 621);
    list.push(
      { date: nw, key: "nowruz", end: addDays(nw, 3) },
      { date: persianDate(P, 1, 13), key: "sizdahBedar" },
      { date: chaharshanbeSuri(nw), key: "chaharshanbeSuri" },
      { date: persianDate(P, 4, 13), key: "tirgan" },
      { date: persianDate(P, 7, 10), key: "mehregan" },
      { date: persianDate(P, 9, 30), key: "yalda" },
      { date: persianDate(P, 11, 10), key: "sadeh" },
    );
  }
  return list.filter((e) => e.date.startsWith(String(year))).sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0));
}
