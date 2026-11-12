/* Hristiyan takvimi (kapsayıcı takvimler, 9 Kasım 2026): Batı (Gregoryen hesap) ve Ortodoks (Jülyen hesap) kiliselerinin
   hareketli bayramları ve sabit bayramlar. Bütün tarihler Gregoryen takvimde.
   Paskalya: Batı için Meeus/Jones/Butcher (anonim Gregoryen algoritma), Ortodoks için Meeus'un Jülyen algoritması
   (Jülyen tarih + 13 gün; 1 Mart 1900 – 28 Şubat 2100 arasında geçerli). Test verisi christian.test.json. */
import { iso, addDays, dowOf } from "../events.mjs";

export const meta = {
  id: "christian",
  group: "religious", // varsayılan her yerde kapalı (kimsenin inancı tahmin edilmez)
  regions: [],
  color: "#7B5EA7",
};

/** Batı Paskalyası (Gregoryen): [ay, gün] */
export function westernEaster(y) {
  const a = y % 19, b = Math.floor(y / 100), c = y % 100, d = Math.floor(b / 4), e = b % 4;
  const f = Math.floor((b + 8) / 25), g = Math.floor((b - f + 1) / 3);
  const h = (19 * a + b - d - g + 15) % 30, i = Math.floor(c / 4), k = c % 4;
  const l = (32 + 2 * e + 2 * i - h - k) % 7, m = Math.floor((a + 11 * h + 22 * l) / 451);
  const month = Math.floor((h + l - 7 * m + 114) / 31), day = ((h + l - 7 * m + 114) % 31) + 1;
  return iso(y, month, day);
}

/** Ortodoks Paskalyası: Jülyen hesap, Gregoryen tarihe çevrilmiş */
export function orthodoxEaster(y) {
  if (y < 1901 || y > 2099) throw new Error("orthodoxEaster: 1901–2099 dışında Jülyen–Gregoryen farkı 13 gün değil");
  const a = y % 4, b = y % 7, c = y % 19, d = (19 * c + 15) % 30, e = (2 * a + 4 * b - d + 34) % 7;
  const month = Math.floor((d + e + 114) / 31), day = ((d + e + 114) % 31) + 1;
  return addDays(iso(y, month, day), 13);
}

export function events(year) {
  const w = westernEaster(year), o = orthodoxEaster(year);
  const list = [
    ["ashWednesday", addDays(w, -46)], ["palmSunday", addDays(w, -7)], ["maundyThursday", addDays(w, -3)],
    ["goodFriday", addDays(w, -2)], ["easter", w], ["easterMonday", addDays(w, 1)], ["ascension", addDays(w, 39)],
    ["pentecost", addDays(w, 49)], ["whitMonday", addDays(w, 50)], ["corpusChristi", addDays(w, 60)],
    ["orthodoxGoodFriday", addDays(o, -2)], ["orthodoxEaster", o], ["orthodoxEasterMonday", addDays(o, 1)],
    ["orthodoxPentecost", addDays(o, 49)],
    ["epiphany", iso(year, 1, 6)], ["orthodoxChristmas", iso(year, 1, 7)], ["annunciation", iso(year, 3, 25)],
    ["assumption", iso(year, 8, 15)], ["allSaints", iso(year, 11, 1)],
    // Advent'in ilk pazarı: Noel'den önceki dördüncü pazar (27 Kasım – 3 Aralık)
    ["advent1", addDays(iso(year, 12, 25), -(dowOf(iso(year, 12, 25)) + 1) - 21)],
    ["christmasEve", iso(year, 12, 24)], ["christmas", iso(year, 12, 25)], ["boxingDay", iso(year, 12, 26)],
  ];
  return list.map(([key, date]) => ({ date, key }));
}
