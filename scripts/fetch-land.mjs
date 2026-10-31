/* Dünya kara sınırları (30 Ekim 2026; tutulma haritası için): Natural Earth 1:110m "land" (kamu malı), GitHub'daki
   nvkelso/natural-earth-vector deposundan. Koordinatlar 0,1°'ye yuvarlanır, ardışık aynı noktalar atılır.
   Derleme ağa çıkmaz; bu betik elle çalıştırılır ve src/data/land-110m.json yazılır. */
import fs from "node:fs";

const URL = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_110m_land.geojson";
const res = await fetch(URL, { headers: { "User-Agent": "SaatTarih/1.0 (https://time.bumba.tr)" } });
if (!res.ok) throw new Error(`indirilemedi: ${res.status}`);
const gj = await res.json();
const rings = [];
for (const f of gj.features) {
  const polys = f.geometry.type === "Polygon" ? [f.geometry.coordinates] : f.geometry.coordinates;
  for (const poly of polys) {
    const ring = [];
    for (const [lon, lat] of poly[0]) {
      const p = [Math.round(lon * 10) / 10, Math.round(lat * 10) / 10];
      const last = ring[ring.length - 1];
      if (!last || last[0] !== p[0] || last[1] !== p[1]) ring.push(p);
    }
    if (ring.length >= 4) rings.push(ring.flat());
  }
}
fs.writeFileSync("src/data/land-110m.json", JSON.stringify({ source: "Natural Earth 1:110m land (public domain), naturalearthdata.com", rings }) + "\n");
console.log(`Yazıldı: src/data/land-110m.json (${rings.length} çokgen, ${rings.reduce((a, r) => a + r.length / 2, 0)} nokta)`);
