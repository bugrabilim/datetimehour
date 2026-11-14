/* Moğol takvimi (kapsayıcı takvimler, 10 Kasım 2026): Moğolistan'ın geleneksel ay-güneş takvimi (Yeni Genden, Moğolca Tögs
   Buyant; Sümbe Khamba İşbaljir, 1786), Tsagaan Sar (Ak Ay, Moğol yılbaşı) ve Cengiz Han'ın doğum günü.

   Tibet takviminin bir sürümüdür; hesap tibetan.mjs'teki motorla, Janson'ın Ek A.3'teki sabitleriyle yapılır (epoch 9 Nisan
   1747, β* = 10, artık ay aralama sayısı 46 ya da 47). Tibet Losar'ıyla çoğu yıl aynı gündür; artık aylar farklı yerde
   olduğu için bazı yıllarda bir gün ya da bir ay ayrılır (ör. 2025: Losar 28 Şubat, Tsagaan Sar 1 Mart).
   Tsagaan Sar yılın ilk günüdür (Moğolistan'da üç gün resmî tatil); Cengiz Han'ın doğum günü kışın ilk ayının (10. ay) ilk
   günüdür (2012'den beri resmî tatil, "Ulusal Gurur Günü"). Doğrulama mongolian.test.json. */
import { NEW_GENDEN as V, newYearJd, monthCount, hasLeapMonth, dayJd } from "./tibetan.mjs";
import { addDays } from "../events.mjs";

export const meta = {
  id: "mongolian",
  group: "cultural",
  regions: ["MN"],
  color: "#1D85ED", // gök mavisi (Moğol göğü); diğer takvimlerden ve Türkiye günü renklerinden ΔE ≥ 21
};

const JD_UNIX = 2440588;
const isoOf = (j) => new Date((j - JD_UNIX) * 86400000).toISOString().slice(0, 10);

/** Y yılının M. ayının ilk günü (ay çiftse birincisinin) */
const monthStart = (Y, M) => isoOf(dayJd(V, 30, monthCount(V, Y, M, hasLeapMonth(V, Y, M)) - 1) + 1);

export function events(year) {
  const out = [];
  for (const Y of [year - 1, year]) {
    const ny = isoOf(newYearJd(V, Y));
    out.push({ key: "tsagaanSar", date: ny, end: addDays(ny, 2) }, { key: "chinggis", date: monthStart(Y, 10) });
  }
  return out.filter((e) => e.date.startsWith(String(year)));
}
