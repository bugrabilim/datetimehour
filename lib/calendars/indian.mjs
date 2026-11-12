/* Hint ulusal takvimi (kapsayıcı takvimler, 10 Kasım 2026): Hindistan'ın resmî sivil takvimi (Saka takvimi; Takvim Reform
   Komitesi 1957, Resmî Gazete ve devlet tatil listelerinde Gregoryen tarihin yanında yazılır) ve iki bölgesel güneş yılbaşı.

   Saka takvimi: 1 Chaitra 22 Mart'tır (Gregoryen artık yılda 21 Mart). Chaitra 30 gün (artık yılda 31), Vaisakha'dan Bhadra'ya
   beş ay 31'er, Asvina'dan Phalguna'ya altı ay 30'ar gün. Saka yılı = Gregoryen yıl − 78 (1 Chaitra'dan itibaren; öncesinde − 79);
   Saka yılı Y, Gregoryen yıl Y + 78 artıksa artıktır. Yıl ikinci tarihte göründüğü için yıl etiketi yok (yıl Mart'ta değişir).
   Doğrulama: Hindistan hükümetinin (DoP&T) 2025–2027 tatil listelerindeki "Saka Date" sütunu ve Node'daki ICU'nun "indian" takvimi.

   Yılbaşları (Güneş'in yıldızıl Meṣa burcuna girişi, Lahiri ayanamsası; hindu-astro.mjs `sankranti`):
   - Pohela Boishakh (Bengal yılbaşı, Batı Bengal ve Hindistan'daki Bengalliler): Bengal kuralı, geçiş gün doğumuyla gece yarısı
     arasındaysa ertesi gün, gece yarısından sonra (gün doğumundan önce) ise gün doğumundan sayılan günün iki gün sonrası; ikisi de
     Hindistan saatiyle geçişin takvim gününün ertesi günüdür. Bangladeş 1987'den beri her yıl 14 Nisan'ı kullanır (notta yazar).
   - Puthandu (Tamil yılbaşı, 1 Chithirai): Tamil kuralı, geçiş gün batımından önceyse o gün, sonraysa ertesi gün (Chennai).
   Test verisi indian.test.json (drikpanchang 2025–2031, DoP&T listeleri 2025–2027). */
import { sankranti, sunTimes, istDate } from "./hindu.mjs";
import { addDays } from "../events.mjs";

export const meta = {
  id: "indian",
  group: "civil", // Hindistan'ın resmî sivil takvimi: yalnız Hindistan'da varsayılan açık
  regions: ["IN"],
  color: "#C07842", // kesariya (safran) tonu; Etiyopya turuncusundan ve yarım gün renginden ΔE ≥ 20
};

const isLeap = (y) => (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
const pad = (n) => String(n).padStart(2, "0");

/** Gregoryen yıl g'de başlayan Saka yılının (g − 78) 1 Chaitra'sı */
export const chaitra1 = (g) => `${g}-03-${isLeap(g) ? 21 : 22}`;

/** Saka yılı (g − 78) ayları: [başlangıç, Saka yılı, ay 1..12] */
function sakaYear(g) {
  const lens = [isLeap(g) ? 31 : 30, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30];
  let d = chaitra1(g);
  return lens.map((len, i) => { const row = [d, g - 78, i + 1, 0]; d = addDays(d, len); return row; });
}

export function months(fromYear, toYear) {
  const all = [];
  for (let g = fromYear - 1; g <= toYear + 1; g++) all.push(...sakaYear(g));
  const a = all.findLastIndex((r) => r[0] <= `${fromYear}-01-01`), b = all.findIndex((r) => r[0] > `${toYear}-12-31`);
  return all.slice(a, b + 1);
}

const CHENNAI = { lat: 13.0827, lon: 80.2707 };

/** Pohela Boishakh (Bengal kuralı): Hindistan saatiyle Meṣa geçişinin takvim gününün ertesi günü */
export const bengaliNewYear = (y) => addDays(istDate(sankranti(y, 0)), 1);

/** Puthandu (Tamil kuralı, Chennai): geçiş gün batımından önceyse o gün, sonraysa ertesi gün */
export function tamilNewYear(y) {
  const t = sankranti(y, 0), d = istDate(t);
  return t < sunTimes(d, CHENNAI.lat, CHENNAI.lon).set ? d : addDays(d, 1);
}

export function events(year) {
  return [
    { key: "sakaNewYear", date: chaitra1(year) },
    { key: "bengaliNewYear", date: bengaliNewYear(year) },
    { key: "tamilNewYear", date: tamilNewYear(year) },
  ];
}
