/* Vietnam ay takvimi (Âm lịch; kapsayıcı takvimler, 9 Kasım 2026): Çin ay takviminin (chinese.mjs) aynı astronomik kuralı
   Vietnam saatiyle (UTC+7; Vietnam 1968'den beri bu saatle hesaplar). Yeni ay Vietnam'da gece yarısından önce, Çin'de sonra
   düşerse ay Vietnam'da bir gün önce başlar (2025–2032'de 3 ay: 2030/1 [Tết 2 Şubat, Çin'de 3 Şubat], 2031 artık 3., 2032/2);
   bu yüzden Vietnam ayrı takvimdir. Hesap chinese.mjs'teki yardımcılarla (kopyalanmaz).
   Olaylar: Tết Nguyên Đán (1/1–1/3, çok günlü), Tết Nguyên Tiêu (1/15), Giỗ Tổ Hùng Vương (3/10, resmî tatil; artık 3. ayda
   olağan ayın 10'u), Tết Đoan Ngọ (5/5), Vu Lan (7/15), Tết Trung Thu (8/15), Ông Công Ông Táo (12/23; Gregoryen yılın
   Ocak–Şubat'ına, önceki ay-yılının son ayına düşer). Günlük kod: 24 güneş dönemi (tiết khí) "s1" (Lập xuân) … "s24" (Đại hàn),
   dönemin başladığı Vietnam günü.
   Not: 2031 artık 3. ayın başındaki yeni ay Vietnam saatiyle 23.57'de (USNO 16.57 UTC); duyarlığı düşük bazı Vietnam
   çeviricileri ayı 22 Nisan'da başlatır. Doğrulama: vietnamese.test.json. */
import { addDays } from "../events.mjs";
import { lunisolarMonths, fromLunar, solarTerms, localDay } from "./chinese.mjs";

export const meta = {
  id: "vietnamese",
  group: "cultural", // yalnız bölgesinde varsayılan açık
  regions: ["VN"],
  color: "#B87074", // nilüfer pembesi (hồng sen; Vietnam’ın ulusal çiçeği); en yakın renk Kıpti erik moru, ΔE 33
};

const OFF = 7; // Vietnam saati (UTC+7)

const cache = new Map();
const table = (year) => {
  if (!cache.has(year)) cache.set(year, lunisolarMonths(year - 1, year + 1, OFF));
  return cache.get(year);
};

export function events(year) {
  const t = table(year), list = [];
  const add = (key, date, end) => { if (date && date.startsWith(String(year))) list.push(end ? { date, key, end } : { date, key }); };
  const tet = fromLunar(year, 1, 1, t);
  add("tet", tet, tet && addDays(tet, 2)); // Mùng 1–3
  add("nguyenTieu", fromLunar(year, 1, 15, t));
  add("hungKings", fromLunar(year, 3, 10, t));
  add("doanNgo", fromLunar(year, 5, 5, t));
  add("vuLan", fromLunar(year, 7, 15, t));
  add("trungThu", fromLunar(year, 8, 15, t));
  // 23 tháng Chạp: önceki ay-yılının 12. ayı bu yılın Ocak–Şubat'ında
  for (const y of [year - 1, year]) add("ongTao", fromLunar(y, 12, 23, t));
  return list;
}

/** İkinci tarih: Vietnam saatiyle ay tablosu (artık ay bayrağı 1 → leapFmt) */
export function months(fromYear, toYear) {
  return lunisolarMonths(fromYear, toYear, OFF);
}

/** 24 güneş dönemi: dönemin başladığı Vietnam günü → "s1" (Lập xuân) … "s24" (Đại hàn) */
export function daily(year) {
  const out = {};
  for (const t of solarTerms(year)) out[localDay(t.ms, OFF)] = `s${t.n}`;
  return out;
}
