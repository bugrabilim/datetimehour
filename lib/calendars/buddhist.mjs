/* Budist takvimi (kapsayıcı takvimler, 9 Kasım 2026): Theravada bayramları ve uposatha günleri Tayland ay takvimine
   (ปฏิทินจันทรคติไทย, "ปฏิทินหลวง") göre; yıl etiketi Tayland'daki Budist çağı (BE = CE + 543, yıl 1 Ocak'ta değişir).

   Ay takvimi: ay yılı Kasım/Aralık'ta 1. ayla (เดือนอ้าย) başlar; tek numaralı aylar 29, çift numaralılar 30 gün. Her ay 15
   günlük büyüyen yarı (ขึ้น 1–15 ค่ำ) ve 14 ya da 15 günlük küçülen yarı (แรม). Yıl türleri: normal 354 gün; athikawan
   (อธิกวาร) 355 gün, 7. ay 30 gün; athikamat (อธิกมาส) 384 gün, 8. ay iki kez (8/8, "เดือน 8 หลัง", 30 gün).
   Yıl türü, Suriyayatra hesabının güneş yılı başındaki değerlerinden (Chula Sakarat yılı CS = CE − 638, Nisan'da başlar;
   J. C. Eade, "Rules for interpolation in the Thai calendar", Journal of the Siam Society 88, 2000, Ek A):
     horakhun h = ⌊(292207·CS + 373) / 800⌋ + 1, kammacubala = 800 − (292207·CS + 373) mod 800,
     avoman = (11h + 650) mod 692, tithi = (h + ⌊(11h + 650) / 692⌋) mod 30.
   athikamat: tithi ≥ 25 ya da ≤ 5 (Eade/Prasert kuralı); tithi 24 ise yalnız ertesi yılın tithi'si 6 olduğunda (ertesi yıl
   kurala girmeyeceği için ara yıl bu yıla düşer; 2012 ve 2031).
   athikawan: güneş artık yılında (kammacubala ≤ 207) avoman ≤ 126, diğer yıllarda avoman < 137; athikamat yılına düşerse
   ertesi yıla geçer (athikamat yılı 355 gün olamaz). Eade "137 ya da daha az" yazar; 137'de (CS 1376 = 2014) resmî takvim
   günü eklemedi, eklenen gün 2016'ya geçti; "< 137" 2006–2028 yayımlanmış bütün tarihlerle aynı sonucu verir (test verisi).
   Yıl başı: ay yılı 2025'in 1. ayının 1. günü 2024-12-01 (resmî Makha Bucha 2025-02-12 = 3. ayın dolunayı, 15. gün).

   Bayramlar (Tayland usulü): Makha Bucha 3. ayın 15'i, Visakha Bucha (Vesak) 6. ayın 15'i (athikamat yılında ikisi de bir ay
   sonra: 4. ve 7. ay), Asalha Bucha 8. ayın 15'i (athikamat: 8/8), Vassa başı (Khao Phansa) ertesi gün, Vassa sonu (Ok Phansa)
   11. ayın 15'i. Uposatha (วันพระ): büyüyen yarının 8. ve 15. günleri, küçülen yarının 8. günü ve ayın son günü (แรม 14 ya da
   15). Bunlar gerçek Ay evresinden (Tayland saatiyle) sıkça bir gün önceye düşer (2000–2100'de 15. günün %45'i astronomik
   dolunayla aynı gün, %49'u bir gün önce, %5'i iki gün önce ya da bir gün sonra; geleneksel hesap ortalama Ay'a dayanır);
   bayramlarla tutarlı olsun diye astronomik evre değil Tayland takviminin günleri kullanılır. Tayland'da resmî takvim her yıl
   ilan edilir (athikawan'ın yeri Mahanikai ve Dhammayut toplulukları arasında kararlaştırılır); yayımlanmamış yıllar hesaptır.
   Test verisi buddhist.test.json. */

export const meta = {
  id: "buddhist",
  group: "religious", // varsayılan her yerde kapalı (kimsenin inancı tahmin edilmez)
  regions: [],
  color: "#A06A55", // kahverengi-toprak (Theravada cübbesi); ΔE ≥ 20 diğer renklerden
};

const DAY = 86400000;
const isoOf = (n) => new Date(n * DAY).toISOString().slice(0, 10);

/** Suriyayatra: CS yılının başındaki değerler */
export function suriyayatra(cs) {
  const t = 292207 * cs + 373, h = Math.floor(t / 800) + 1, a = 11 * h + 650;
  return { horakhun: h, kammacubala: 800 - (t % 800), avoman: a % 692, tithi: (h + Math.floor(a / 692)) % 30 };
}
const tithi = (y) => suriyayatra(y - 638).tithi;

/** Gregoryen yıl y'nin ay yılı (Kasım/Aralık y−1'de başlayan) athikamat mı (8. ay iki kez) */
export function isAthikamat(y) {
  const t = tithi(y);
  return t >= 25 || t <= 5 || (t === 24 && tithi(y + 1) === 6);
}
const wantsDay = (y) => { const s = suriyayatra(y - 638); return s.kammacubala <= 207 ? s.avoman <= 126 : s.avoman < 137; };
/* Eklenecek gün: bu yılın isteği ya da athikamat yılından devreden istek */
const pendingDay = (y, depth = 0) => wantsDay(y) || (depth < 4 && isAthikamat(y - 1) && pendingDay(y - 1, depth + 1));
/** Gregoryen yıl y'nin ay yılı athikawan mı (7. ay 30 gün) */
export const isAthikawan = (y) => !isAthikamat(y) && pendingDay(y);
export const yearLength = (y) => 354 + (isAthikamat(y) ? 30 : isAthikawan(y) ? 1 : 0);

/* Ay yılı 2025'in ilk günü (Unix günü): 2024-12-01 */
const ANCHOR_YEAR = 2025, ANCHOR_DAY = Date.UTC(2024, 11, 1) / DAY;
const startCache = new Map();
/** Ay yılı y'nin ilk günü (1. ayın büyüyen 1. günü), Unix günü */
export function yearStart(y) {
  if (startCache.has(y)) return startCache.get(y);
  let d = ANCHOR_DAY;
  for (let k = ANCHOR_YEAR; k < y; k++) d += yearLength(k);
  for (let k = y; k < ANCHOR_YEAR; k++) d -= yearLength(k);
  startCache.set(y, d);
  return d;
}

/** Ay yılı y'nin ayları: [{ month (1–12), second (8/8 için true), start (Unix günü), len }] */
export function lunarMonths(y) {
  const am = isAthikamat(y), aw = isAthikawan(y), out = [];
  let d = yearStart(y);
  const push = (month, len, second = false) => { out.push({ month, second, start: d, len }); d += len; };
  for (let m = 1; m <= 12; m++) {
    push(m, m === 7 && aw ? 30 : m % 2 ? 29 : 30);
    if (m === 8 && am) push(8, 30, true);
  }
  if (d !== yearStart(y + 1)) throw new Error(`Tayland ay takvimi: ${y} ayları yıl uzunluğuna uymuyor`);
  return out;
}

/** Ay yılı y'nin bayramları (Gregoryen tarih) */
function yearEvents(y) {
  const ms = lunarMonths(y), am = isAthikamat(y);
  const nth = (month, second = false) => ms.find((r) => r.month === month && r.second === second);
  const full = (r) => isoOf(r.start + 14); // büyüyen yarının 15. günü (ขึ้น 15 ค่ำ)
  const m8 = am ? nth(8, true) : nth(8);
  return [
    { key: "maghaPuja", date: full(nth(am ? 4 : 3)) },
    { key: "vesak", date: full(nth(am ? 7 : 6)) },
    { key: "asalhaPuja", date: full(m8) },
    { key: "vassaStart", date: isoOf(m8.start + 15) }, // แรม 1 ค่ำ
    { key: "vassaEnd", date: full(nth(11)) },
  ];
}

export function events(year) {
  return yearEvents(year).filter((e) => e.date.startsWith(String(year))).map((e) => ({ date: e.date, key: e.key }));
}

/** Uposatha (วันพระ) günleri: k8 (ขึ้น 8 ค่ำ, ilk dördün), k15 (ขึ้น 15 ค่ำ, dolunay), r8 (แรม 8 ค่ำ, son dördün),
    r14 / r15 (ayın son günü, แรม 14 ya da 15 ค่ำ, yeni ay) */
export function daily(year) {
  const out = {};
  for (const y of [year, year + 1]) for (const r of lunarMonths(y)) {
    const days = [[7, "k8"], [14, "k15"], [22, "r8"], [r.len - 1, r.len === 30 ? "r15" : "r14"]];
    for (const [off, code] of days) { const d = isoOf(r.start + off); if (d.startsWith(String(year))) out[d] = code; }
  }
  return Object.fromEntries(Object.entries(out).sort());
}

/** Budist çağı (Tayland: Gregoryen yıl + 543) */
export const yearLabel = (year) => year + 543;
