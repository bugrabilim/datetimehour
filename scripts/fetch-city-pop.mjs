/* Şehir nüfusu (4 Kasım 2026, 2. analiz #180): Natural Earth 1:10m Populated Places (kamu malı) pop_max değeri (kent
   bölgesi, yaklaşık) src/data/cities.json'daki her şehre "pop" olarak yazılır: şehrin 0,4° içindeki en kalabalık yer.
   Elle çalıştırılır (derleme ağa çıkmaz): node scripts/fetch-city-pop.mjs */
import fs from "node:fs";
const URL_NE = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_populated_places_simple.geojson";
const ne = await (await fetch(URL_NE)).json();
const file = new URL("../src/data/cities.json", import.meta.url);
const cities = JSON.parse(fs.readFileSync(file, "utf8"));
for (const c of cities) {
  let best = 0;
  for (const f of ne.features) {
    const p = f.properties;
    if (Math.abs(p.latitude - c.lat) < 0.4 && Math.abs(p.longitude - c.lon) < 0.4 && p.pop_max > best) best = p.pop_max;
  }
  if (best) c.pop = best; else delete c.pop;
  console.log(c.key, best);
}
fs.writeFileSync(file, JSON.stringify(cities, null, 2) + "\n");
