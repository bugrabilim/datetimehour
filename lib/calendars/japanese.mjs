/* Japon takvimi (kapsayıcı takvimler, 9 Kasım 2026): Japon dönemi yılı (Reiwa), rokuyō ve resmî tatil olmayan mevsim/halk günleri.
   - Yıl etiketi: Reiwa yılı (2019 = 1). Dönem 1 Mayıs 2019'da başladı; 2025–2032 yıllarının hepsi Reiwa'dır. Yeni dönem ilan
     edilirse (ör. tahttan çekilme) yıl içinde değişen etiket bu sözleşmeye sığmaz, sözleşme genişletilmeli.
   - Rokuyō (六曜): eski takvimin (kyūreki, Japon ay-güneş takvimi) ay ve gününden: (ay + gün) mod 6 → 0 大安 taian, 1 赤口 shakko,
     2 先勝 sensho, 3 友引 tomobiki, 4 先負 senbu, 5 仏滅 butsumetsu. Artık ay, tekrar ettiği ayın numarasını kullanır.
   - Eski takvim: yeni ay Japonya saatiyle (UTC+9) düştüğü gün ay başıdır; ay numarası içerdiği "orta dönemden" (中気; Güneş
     boylamı 30°'nin katı) gelir (Tenpō kuralı: 雨水 → 1. ay … 冬至 → 11. ay … 大寒 → 12. ay), orta dönemsiz ay artık aydır.
     Hesap lib/calendars/chinese.mjs'teki yardımcılarla (lunisolarMonths(…, 9): Çin kuralı, UTC+9). İki kural her ayın tam bir
     orta dönem içerdiği yıllarda aynı sonucu verir; aşağıdaki kyurekiMonths() bunu her ay için denetler ve ayrılırsa (ör. 2033
     sorunu) derlemeyi durdurur.
   - Olaylar (resmî tatil olanlar ülke tatillerinde zaten var, burada yok): Setsubun (Risshun'dan önceki gün), bahar ve sonbahar
     Higan'ı (ekinoks günü ±3 gün; ekinoks günü Shunbun/Shūbun no Hi resmî tatildir), Hina Matsuri (3 Mart), Tanabata (7 Temmuz),
     Obon (13–16 Ağustos; Tokyo ve bazı yerlerde Temmuz'da), Shichi-Go-San (15 Kasım), yaz Doyō'sunun öküz (丑) günleri
     (Güneş 117°'den Risshū'ya [135°] kadar; günlerin 12'li döngüsü 2000-01-01 = 戊午 sabitiyle). Güneş dönemleri Japonya saatiyle.
   Doğrulama: japanese.test.json (NAOJ 暦要項 ve gelecekteki ekinoks günleri tablosu, yayımlanmış rokuyō takvimi). */
import { addDays } from "../events.mjs";
import { lunisolarMonths, lunarDate, solarTerms, localDay, sunLongitudeMs } from "./chinese.mjs";

export const meta = {
  id: "japanese",
  group: "cultural", // yalnız Japonya'da varsayılan açık
  regions: ["JP"],
  color: "#C24A8E", // açelya pembesi (tsutsuji-iro)
};

const JST = 9;
const DAY = 86400000;
/** Rokuyō: (ay + gün) mod 6 sırası */
export const ROKUYO = ["taian", "shakko", "sensho", "tomobiki", "senbu", "butsumetsu"];

/** Japonya saatiyle güneş dönemi günü: { lon → "YYYY-MM-DD" } (Gregoryen yıl) */
const termDays = (year) => Object.fromEntries(solarTerms(year).map((t) => [t.lon, localDay(t.ms, JST)]));

/** Yaz Doyō'sunun girişi (土用の入り): Güneş boylamı 117° (24 dönemden biri değil), Japonya saatiyle gün */
const doyoStart = (year) => localDay(sunLongitudeMs(117, Date.UTC(year, 6, 19)), JST);

/** Orta dönemin (中気) eski takvimdeki ay numarası: 雨水 (330°) → 1, 春分 (0°) → 2, … 冬至 (270°) → 11, 大寒 (300°) → 12 */
const zhongqiMonth = (lon) => ((((lon - 330) / 30) % 12) + 12) % 12 + 1;

/** Eski takvim (kyūreki) ay başları, Japonya saatiyle: [ay başı, ay-yılı, ay, 0 | 1 (artık)]; son satır yalnız bitiş sınırı.
    Tenpō kuralıyla (her ay içerdiği orta dönemin numarasını alır) birebir aynı olduğu denetlenir; ayrılırsa hata. */
export function kyurekiMonths(fromYear, toYear) {
  const rows = lunisolarMonths(fromYear, toYear, JST);
  const zq = [];
  for (let y = fromYear - 1; y <= toYear + 1; y++) for (const t of solarTerms(y)) if (t.lon % 30 === 0) zq.push([localDay(t.ms, JST), zhongqiMonth(t.lon)]);
  for (let i = 0; i + 1 < rows.length; i++) {
    const [start, , m, leap] = rows[i], next = rows[i + 1][0];
    const inside = zq.filter(([d]) => d >= start && d < next);
    const ok = leap ? inside.length === 0 : inside.length === 1 && inside[0][1] === m;
    if (!ok) throw new Error(`japanese: ${start} ayı Tenpō kuralıyla (orta dönemler: ${JSON.stringify(inside)}) Çin kuralından (${m}${leap ? " artık" : ""}) ayrılıyor; elle doğrulayın`);
  }
  return rows;
}

const tables = new Map();
const table = (year) => {
  if (!tables.has(year)) tables.set(year, kyurekiMonths(year, year)); // yalnız o yılın ayları denetlenir (2033 sorunu aralığın dışında)
  return tables.get(year);
};

/** Gün → rokuyō kodu (tablo dışında null) */
export function rokuyo(date) {
  const l = lunarDate(date, table(+date.slice(0, 4)));
  return l ? ROKUYO[(l.m + l.d) % 6] : null;
}

/** Günün 12'li döngüdeki yeri: 0 子 … 1 丑 … 11 亥 (60'lı gün döngüsü: 2000-01-01 = 戊午, sıra 54) */
export const dayBranch = (date) => ((((Date.parse(date) - Date.parse("2000-01-01")) / DAY + 54) % 12) + 12) % 12;

export function events(year) {
  const y = String(year), t = termDays(year), list = [];
  const add = (key, date, end) => { if (date.startsWith(y)) list.push(end ? { date, key, end } : { date, key }); };
  add("setsubun", addDays(t[315], -1));
  add("hinamatsuri", `${y}-03-03`);
  add("higanSpring", addDays(t[0], -3), addDays(t[0], 3));
  add("tanabata", `${y}-07-07`);
  // Yaz Doyō'su: Güneş 117°'ye ulaştığı günden Risshū'dan (135°) önceki güne; içindeki öküz (丑) günleri (bir ya da iki)
  for (let d = doyoStart(year); d < t[135]; d = addDays(d, 1)) if (dayBranch(d) === 1) add("doyoUshi", d);
  add("obon", `${y}-08-13`, `${y}-08-16`);
  add("higanAutumn", addDays(t[180], -3), addDays(t[180], 3));
  add("shichiGoSan", `${y}-11-15`);
  return list;
}

/** Rokuyō: yılın her günü için kod (dizi; 0 = 1 Ocak) */
export function daily(year) {
  const out = [];
  for (let d = `${year}-01-01`; d.startsWith(String(year)); d = addDays(d, 1)) out.push(rokuyo(d) || "");
  return out;
}

/** Reiwa yılı (2019 = 1) */
export function yearLabel(year) {
  return year >= 2020 ? year - 2018 : null;
}
