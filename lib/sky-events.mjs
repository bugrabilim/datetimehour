/* Gökyüzü olay takvimi (3 Kasım 2026, 2. analiz #154): meteor yağmuru zirveleri, gezegen kavuşumları, Ay–gezegen
   yakınlaşmaları, karşı konumlar ve en büyük uzanımlar. Dış veri yok: gezegenler sky-calc.js (JPL yaklaşık öğeleri),
   Ay ve Güneş nature-calc.js. Site (gece gökyüzü sayfası, /ics/<dil>-sky.ics) ve uygulamalar (gen-strings → sky-events.json)
   aynı listeyi kullanır.
   Meteor yağmurları: IMO Meteor Shower Calendar 2026 ve 2027 (imo.net, Table 5 "Working List of Visual Meteor Showers"):
   zirve Güneş boylamıyla (λ⊙, J2000.0) verilir; tarih her yıl o boylama göre hesaplanır. 2026 ve 2027 tarihleri IMO
   tablosundaki günlerle karşılaştırıldı (npm test). ZHR: IMO'nun "son yıllarda gözlenen" değeri; Draconidler değişken. */
import fs from "node:fs";
import path from "node:path";

const load = (file) => { const m = { exports: {} }; new Function("module", "window", fs.readFileSync(file, "utf8"))(m, undefined); return m.exports; };

export const SHOWERS = [
  // kimlik, IMO kodu, λ⊙ (J2000), ZHR (null: değişken), etkinlik aralığı (AA-GG)
  { id: "qua", lam: 283.15, zhr: 80, from: "12-28", to: "01-12" },
  { id: "lyr", lam: 32.32, zhr: 18, from: "04-14", to: "04-30" },
  { id: "eta", lam: 45.5, zhr: 50, from: "04-19", to: "05-28" },
  { id: "sda", lam: 128, zhr: 25, from: "07-12", to: "08-23" },
  { id: "per", lam: 140.0, zhr: 100, from: "07-17", to: "08-24" },
  { id: "dra", lam: 195.4, zhr: null, from: "10-06", to: "10-10" },
  { id: "ori", lam: 208, zhr: 20, from: "10-02", to: "11-07" },
  { id: "leo", lam: 235.27, zhr: 15, from: "11-06", to: "11-30" },
  { id: "gem", lam: 262.2, zhr: 150, from: "12-04", to: "12-20" },
  { id: "urs", lam: 270.7, zhr: 10, from: "12-17", to: "12-26" },
];

const DAY = 86400000, HOUR = 3600000;
const D2R = Math.PI / 180;
const norm = (x) => ((x % 360) + 360) % 360;
const sep = (a1, d1, a2, d2) => {
  const c = Math.sin(d1 * D2R) * Math.sin(d2 * D2R) + Math.cos(d1 * D2R) * Math.cos(d2 * D2R) * Math.cos((a1 - a2) * D2R);
  return Math.acos(Math.max(-1, Math.min(1, c))) / D2R;
};

export function skyEvents(root, fromYear, toYear) {
  const N = load(path.join(root, "src/js/nature-calc.js"));
  const K = load(path.join(root, "src/js/sky-calc.js"));
  const from = Date.UTC(fromYear, 0, 1), to = Date.UTC(toYear + 1, 0, 1);
  const out = [];
  const T = (ms) => (ms / DAY + 2440587.5 + 69 / 86400 - 2451545) / 36525;
  /* Güneş'in J2000 ekinoksuna göre gerçek boylamı (Meeus 25; tarih ekinoksundan genel presesyon çıkarılır) */
  const sunLam = (ms) => {
    const t = T(ms), L0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t, M = 357.52911 + 35999.05029 * t - 0.0001537 * t * t;
    const C = (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(M * D2R) + (0.019993 - 0.000101 * t) * Math.sin(2 * M * D2R) + 0.000289 * Math.sin(3 * M * D2R);
    return norm(L0 + C - 1.3969713 * t);
  };
  // Meteor yağmurları: her yıl λ⊙'nın geçildiği an (gün adımı + ikiye bölme)
  for (let y = fromYear; y <= toYear; y++) {
    for (const s of SHOWERS) {
      const d = (ms) => ((sunLam(ms) - s.lam + 540) % 360) - 180;
      let a = Date.UTC(y, 0, 1), found = null;
      for (let t = a; t < Date.UTC(y + 1, 0, 1); t += DAY) { if (d(t) < 0 && d(t + DAY) >= 0) { found = t; break; } }
      if (found === null) continue;
      let lo = found, hi = found + DAY;
      for (let i = 0; i < 30; i++) { const m = (lo + hi) / 2; if (d(m) < 0) lo = m; else hi = m; }
      const at = Math.round((lo + hi) / 2 / 60000) * 60000;
      out.push({ t: at, k: "shower", id: s.id, zhr: s.zhr, illum: Math.round(N.moon(at).illum * 100) });
    }
  }
  // Gezegen ve Ay için günlük/saatlik örnekler
  const PL = K.PLANETS;
  const sunRaDec = (ms) => N.sunPos(ms);
  const pos = (name, ms) => K.planet(name, ms);
  const elongSigned = (name, ms) => { // + doğu (akşam), − batı (sabah): ekliptik boylam farkı yerine sağ açıklık farkı yeterli
    const p = pos(name, ms), s = sunRaDec(ms);
    const dra = ((p.ra - s.ra + 540) % 360) - 180;
    return { e: p.elong, east: dra > 0 };
  };
  const refineMin = (f, a, b) => { // altın oran araması, a..b arasında f en küçük
    const g = (Math.sqrt(5) - 1) / 2;
    let x1 = b - g * (b - a), x2 = a + g * (b - a), f1 = f(x1), f2 = f(x2);
    for (let i = 0; i < 40 && b - a > 30000; i++) {
      if (f1 < f2) { b = x2; x2 = x1; f2 = f1; x1 = b - g * (b - a); f1 = f(x1); } else { a = x1; x1 = x2; f1 = f2; x2 = a + g * (b - a); f2 = f(x2); }
    }
    return (a + b) / 2;
  };
  // Karşı konum (Mars, Jüpiter, Satürn) ve en büyük uzanım (Merkür, Venüs): günlük uzanımın yerel en büyüğü
  for (const name of ["mercury", "venus", "mars", "jupiter", "saturn"]) {
    let prev = pos(name, from - DAY).elong, cur = pos(name, from).elong;
    for (let t = from; t < to; t += DAY) {
      const nx = pos(name, t + DAY).elong;
      if (cur >= prev && cur > nx) {
        const at = Math.round(refineMin((ms) => -pos(name, ms).elong, t - DAY, t + DAY) / 60000) * 60000;
        const e = elongSigned(name, at);
        if (name === "mercury" || name === "venus") out.push({ t: at, k: "elong", p: name, deg: Math.round(e.e * 10) / 10, ev: e.east });
        else if (e.e > 170) out.push({ t: at, k: "opp", p: name, mag: Math.round(pos(name, at).mag * 10) / 10 });
      }
      prev = cur; cur = nx;
    }
  }
  // Gezegen–gezegen kavuşumları: ayrılık 2°'nin altında yerel en küçük, ikisi de Güneş'ten en az 12° uzakta
  for (let i = 0; i < PL.length; i++) for (let j = i + 1; j < PL.length; j++) {
    const a = PL[i], b = PL[j];
    const f = (ms) => { const p = pos(a, ms), q = pos(b, ms); return sep(p.ra, p.dec, q.ra, q.dec); };
    let prev = f(from - DAY), cur = f(from);
    for (let t = from; t < to; t += DAY) {
      const nx = f(t + DAY);
      if (cur <= prev && cur < nx && cur < 2.5) {
        const at = Math.round(refineMin(f, t - DAY, t + DAY) / 60000) * 60000, s = f(at);
        if (s < 2 && pos(a, at).elong > 12 && pos(b, at).elong > 12) out.push({ t: at, k: "conj", a, b, sep: Math.round(s * 10) / 10 });
      }
      prev = cur; cur = nx;
    }
  }
  // Ay–gezegen yakınlaşmaları (yer merkezli): ayrılık 3°'nin altında, gezegen Güneş'ten en az 15° uzakta
  for (const name of PL) {
    const f = (ms) => { const m = N.moonPos(ms), p = pos(name, ms); return sep(m.ra, m.dec, p.ra, p.dec); };
    let prev = f(from - 3 * HOUR), cur = f(from);
    for (let t = from; t < to; t += 3 * HOUR) {
      const nx = f(t + 3 * HOUR);
      if (cur <= prev && cur < nx && cur < 4) {
        const at = Math.round(refineMin(f, t - 3 * HOUR, t + 3 * HOUR) / 60000) * 60000, s = f(at);
        if (s < 3 && pos(name, at).elong > 15) out.push({ t: at, k: "moon", p: name, sep: Math.round(s * 10) / 10, illum: Math.round(N.moon(at).illum * 100) });
      }
      prev = cur; cur = nx;
    }
  }
  return out.filter((e) => e.t >= from && e.t < to).sort((x, y) => x.t - y.t);
}
