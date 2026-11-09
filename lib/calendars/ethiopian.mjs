/* Etiyopya takvimi (kapsayıcı takvimler, 9 Kasım 2026): Etiyopya'nın resmî takvimi ve Etiyopya Ortodoks Tewahedo Kilisesi'nin
   bayramları; Eritre'de kilise takvimi. Aylar ve yılbaşı Kıpti takvimiyle aynıdır (ortak aritmetik coptic.mjs'te): 12 × 30 gün
   + Pagume (5, Luka yılında 6 gün); 1 Meskerem (Enkutataş) 11 Eylül, Gregoryen artık yıldan önceki yılda 12 Eylül.
   Yıl Amete Mihret (Gregoryen yıl − 7 Eylül'den itibaren, − 8 Ocak–Eylül); yıl ikinci tarihte göründüğü için yıl etiketi yok.
   Genna 29 Tahsas'tır, Yuhanna yılında (12 Eylül'de başlayan yıl) 28 Tahsas: her yıl 7 Ocak. Timkat 11 Tir: 19 Ocak, Gregoryen
   artık yılda 20 Ocak. Fasika Ortodoks Paskalyası (christian.mjs). Test verisi ethiopian.test.json. */
import { addDays } from "../events.mjs";
import { orthodoxEaster } from "./christian.mjs";
import { newYearDay, alexToIso, nativity, alexMonths, ETHIOPIAN_OFFSET, COPTIC_OFFSET } from "./coptic.mjs";

export const meta = {
  id: "ethiopian",
  group: "cultural", // Etiyopya'nın resmî takvimi: yalnız Etiyopya ve Eritre'de varsayılan açık
  regions: ["ET", "ER"],
  color: "#9A7B0E", // koyu altın (Meskel çiçeği); İslami günlerin yeşili ve diğer takvimlerden ayrı
};

export function events(year) {
  const fasika = orthodoxEaster(year);
  return [
    { key: "genna", date: nativity(year) },
    { key: "timkat", date: alexToIso(year - 1, 5, 11) }, // 11 Tir
    { key: "siklet", date: addDays(fasika, -2) },
    { key: "fasika", date: fasika },
    { key: "enkutatash", date: newYearDay(year) }, // 1 Meskerem
    { key: "meskel", date: alexToIso(year, 1, 17) }, // 17 Meskerem
  ];
}

export const months = (fromYear, toYear) => alexMonths(fromYear, toYear, (y) => y - COPTIC_OFFSET + ETHIOPIAN_OFFSET);
