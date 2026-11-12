/* Sih takvimi (kapsayıcı takvimler, 10 Kasım 2026): gurpurablar ve Sih kutsal günleri, Hindistan'da geniş ölçüde izlenen
   SGPC (Şiromani Gurdwara Parbandhak Komitesi) takvimine göre: çoğu Bikrami ay takviminin günleri, Vaisakhi güneş takviminden.
   2003'teki asıl Nanakşahi takvimi (sabit güneş tarihleri) SGPC'de 2010'da değiştirildi; onu izleyen gurdwaralarda (ör. Pakistan,
   bazı diaspora toplulukları) bazı günler farklıdır (notta yazar). Ay tablosu ve yıl etiketi yok: iki takvimin ayları ayrışır.

   Kurallar (Yeni Delhi gün doğumu, hindu.mjs ile aynı gökyüzü hesabı; amānta ay numaraları 0 Caitra … 11 Phālguna):
   - Guru Nanak Gurpurab: Kārttika pūrṇimā (amānta 7. ay, tithi 15), tithinin gün doğumuna değdiği gün (udaya).
   - Guru Gobind Singh Gurpurab: Pauṣa śukla saptamī (amānta 9. ay, tithi 7), udaya; Aralık–Ocak'a düştüğü için bir Gregoryen
     yılda iki kez ya da hiç olmayabilir.
   - Vaisakhi (Khalsa Sajna Divas): Güneş'in yıldızıl Meṣa burcuna girdiği an, Hindistan saatiyle takvim günü.
   - Hola Mohalla: Holi'nin ertesi günü (Chet vadi 2; Anandpur Sahib'de üç günlük şenliğin son günü, BBC ve Vikipedi tanımı).
   - Bandi Chhor Divas: Diwali günü (hindu.mjs `diwali`). 2025'te Altın Tapınak 21 Ekim'de kutladı (OFFICIAL).
   - Guru Tegh Bahadur'un şehadet günü: 24 Kasım (Hindistan hükümeti listelerinde her yıl bu gün; Nanakşahi tarihi).
   Test verisi sikh.test.json. */
import { lunations, tithiSpan, delhiSun, istDate, sankranti, events as hinduEvents } from "./hindu.mjs";
import { addDays } from "../events.mjs";

export const meta = {
  id: "sikh",
  group: "religious", // varsayılan her yerde kapalı (kimsenin inancı tahmin edilmez)
  regions: [],
  color: "#0090BA", // Nihang mavisine yakın gök mavisi; diğer mavilerden ΔE ≥ 20
};

/* Yayımlanmış ve kurala uymayan tarihler: yıl → anahtar → tarih */
const OFFICIAL = {
  2025: { bandiChhor: "2025-10-21" }, // Altın Tapınak (IANS, 21 Ekim 2025); Delhi Diwali'si 20 Ekim
};

/** Tithi aralığına (ms) gün doğumu (Yeni Delhi) düşen ilk gün */
function udaya([a, b]) {
  let d = istDate(a);
  for (let i = 0; i < 3; i++, d = addDays(d, 1)) {
    const r = delhiSun(d).rise;
    if (r >= a && r < b) return d;
  }
  return null;
}

/** Yıl y'nin çevresindeki artık olmayan m. kameri aylar (Pauṣa Aralık–Ocak'a düştüğü için birden çok) */
const monthsAround = (y, m) => lunations(Date.UTC(y - 1, 9, 1), Date.UTC(y + 1, 2, 1)).filter((l) => l.m === m && !l.adhika);

const evCache = new Map();
export function events(year) {
  if (evCache.has(year)) return evCache.get(year);
  const inYear = (d) => d && d.startsWith(String(year));
  const hindu = Object.fromEntries(hinduEvents(year).map((e) => [e.key, e.date]));
  const list = [];
  const push = (key, date) => { date = (OFFICIAL[year] || {})[key] || date; if (inYear(date)) list.push({ key, date }); };
  for (const l of monthsAround(year, 9)) push("gurpurabGobind", udaya(tithiSpan(l, 7)));
  if (hindu.holi) push("holaMohalla", addDays(hindu.holi, 1));
  push("vaisakhi", istDate(sankranti(year, 0)));
  if (hindu.diwali) push("bandiChhor", hindu.diwali);
  for (const l of monthsAround(year, 7)) push("gurpurabNanak", udaya(tithiSpan(l, 15)));
  push("martyrdomTeghBahadur", `${year}-11-24`);
  list.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0));
  evCache.set(year, list);
  return list;
}
