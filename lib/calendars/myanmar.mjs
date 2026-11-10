/* Myanmar takvimi (kapsayıcı takvimler, 10 Kasım 2026): Myanmar (Birmanya) ay-güneş takvimi, Myanmar çağı (ME) ve bayramları.

   Hesap Yan Naing Aye'nin "Modern Myanmar Calendrical Calculations" algoritmasıdır (mmcal, MIT lisansı, sürüm 20260909,
   https://github.com/yan9a/mmcal; açıklama cool-emerald.blogspot.com 2013/06): üçüncü dönem (ME 1312'den beri) sabitleri ve
   istisna tabloları (dolunay günü ME 1377 +1; artık ay istisnaları ME 1344–1345). Bu modül yalnız üçüncü dönemi hesaplar.
   Tarih "ayın büyüyen ya da küçülen yarısının kaçıncı günü" diye yazılır: 1–14 büyüyen, 15 dolunay, sonra küçülen 1–14 ve
   ayın son günü yeni ay. Artık ay (watat) yıllarında Waso'dan önce I. Waso gelir; Myanmar yılbaşından önceki Tagu ve Kason
   günleri "geç Tagu/Kason" olarak eski yıla sayılır.
   Thingyan (su bayramı) Akyo, Akya, Akyat ve Atat günleridir; yılbaşı Atat'ın ertesi günüdür. Devletin su bayramı tatili
   bundan uzun olabilir (her yıl ayrıca ilan edilir). Doğrulama myanmar.test.json. */
export const meta = {
  id: "myanmar",
  group: "cultural",
  regions: ["MM"],
  color: "#DF20B9", // pagoda pembesi; diğer takvimlerden ve Türkiye günü renklerinden ΔE ≥ 21
};

const SY = 1577917828 / 4320000; // güneş yılı
const LM = 1577917828 / 53433336; // ay ayı
const MO = 1954168.050623; // ME 0'ın başı (Myanmar saati)
const JD_UNIX = 2440588;
const jdnOf = (s) => Date.UTC(+s.slice(0, 4), +s.slice(5, 7) - 1, +s.slice(8, 10)) / 86400000 + JD_UNIX;
const isoOf = (j) => new Date((j - JD_UNIX) * 86400000).toISOString().slice(0, 10);

function myConst(my) {
  if (my < 1312) throw new Error(`myanmar: ME ${my} üçüncü dönemden önce`);
  let WO = -0.5;
  const EW = my === 1344 || my === 1345 ? 1 : 0;
  if (my === 1377) WO += 1;
  return { WO, NM: 8, EW };
}
function watat(my) {
  const c = myConst(my);
  const TA = (SY / 12 - LM) * (12 - c.NM);
  let ed = (SY * (my + 3739)) % LM;
  if (ed < TA) ed += LM;
  const fm = Math.round(SY * my + MO - ed + 4.5 * LM + c.WO);
  const TW = LM - (SY / 12 - LM) * c.NM;
  return { fm, watat: (ed >= TW ? 1 : 0) ^ c.EW };
}
function yearInfo(my) {
  const y2 = watat(my);
  let yd = 0, y1;
  do { yd++; y1 = watat(my - yd); } while (y1.watat === 0 && yd < 3);
  let myt = y2.watat;
  if (myt) myt = Math.floor(((y2.fm - y1.fm) % 354) / 31) + 1;
  return { myt, tg1: y1.fm + 354 * yd - 102 };
}
/** JDN → { my, mm, md, myt }; mm: 0 I. Waso, 1 Tagu … 12 Tabaung, 13 geç Tagu, 14 geç Kason */
export function toMyanmar(jdn) {
  const my = Math.floor((jdn - 0.5 - MO) / SY);
  const yo = yearInfo(my);
  let dd = jdn - yo.tg1 + 1;
  const b = Math.floor(yo.myt / 2), c = Math.floor(1 / (yo.myt + 1));
  const myl = 354 + (1 - c) * 30 + b;
  const mmt = Math.floor((dd - 1) / myl);
  dd -= mmt * myl;
  const a = Math.floor((dd + 423) / 512);
  let mm = Math.floor((dd - b * a + c * a * 30 + 29.26) / 29.544);
  const e = Math.floor((mm + 12) / 16), f = Math.floor((mm + 11) / 16);
  const md = dd - Math.floor(29.544 * mm - 29.26) - b * e + c * f * 30;
  mm += f * 3 - e * 4 + 12 * mmt;
  return { my, mm, md, myt: yo.myt };
}
const monthLen = (mm, myt) => 30 - (mm % 2) + (mm === 3 ? Math.floor(myt / 2) : 0);

/** Thingyan: Akya ve Atat günleri (JDN), Myanmar yılı my'ye girerken */
function thingyan(my) {
  const ja = SY * my + MO, jk = ja - 2.169918982;
  return { akya: Math.round(jk), atat: Math.round(ja) };
}

export function events(year) {
  const out = [];
  const first = jdnOf(`${year}-01-01`), last = jdnOf(`${year}-12-31`);
  // Thingyan ve yılbaşı (Nisan'da)
  const my = toMyanmar(jdnOf(`${year}-06-01`)).my;
  const t = thingyan(my);
  out.push({ key: "thingyan", date: isoOf(t.akya - 1), end: isoOf(t.atat) }, { key: "newYear", date: isoOf(t.atat + 1) });
  for (let j = first; j <= last; j++) {
    const { mm, md } = toMyanmar(j);
    const m = mm === 13 ? 1 : mm === 14 ? 2 : mm;
    const key = md === 15 ? { 2: "kason", 4: "waso", 7: "thadingyut", 8: "tazaungdaing", 12: "tabaung" }[m]
      : m === 8 && md === 25 ? "nationalDay"
      : m === 10 && md === 1 ? "karenNewYear" : null;
    if (key) out.push({ key, date: isoOf(j) });
  }
  return out.filter((e) => e.date.startsWith(String(year)));
}

/** Günlük kod: ay (m0 I. Waso, m1–m14, artık yılda Waso s4), gün (w1–w14 büyüyen, f15 dolunay, n1–n14 küçülen, z1 yeni ay)
    ve yıl (y1387) */
export function daily(year) {
  const first = jdnOf(`${year}-01-01`), n = jdnOf(`${year + 1}-01-01`) - first;
  return Array.from({ length: n }, (_, i) => {
    const { my, mm, md, myt } = toMyanmar(first + i);
    const len = monthLen(mm, myt);
    const month = mm === 4 && myt ? "s4" : `m${mm}`;
    const day = md < 15 ? `w${md}` : md === 15 ? "f15" : md === len ? "z1" : `n${md - 15}`;
    return `${month}${day}y${my}`;
  });
}
