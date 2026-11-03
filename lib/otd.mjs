/* Tarihte bugün (Wikidata, CC0; src/data/on-this-day.json): bir günün kayıtlarını dile göre seçer. Site (build.mjs) ve
   uygulamalar (mobile/shared/gen-strings.mjs) aynı kuralı kullanır. Kayıt: [yıl, tür (e olay, f kuruluş, b doğum,
   d ölüm), ad, açıklama, Wikidata kimliği]; adı o dilde olmayan kayıt alınmaz (Filipincede İngilizceye düşülür). */
export const OTD_FALLBACK = { fil: "en" };
export function otdDay(src, lang, md, limit = { e: 8, f: 2, b: 5, d: 5 }) {
  const out = { e: [], f: [], b: [], d: [] }, seen = new Set(), fb = OTD_FALLBACK[lang];
  for (const it of src.days[md] || []) {
    const own = it.l && it.l[lang], name = own || (fb && it.l && it.l[fb]);
    // Aynı öğe (ör. hem olay hem kuruluş olarak kayıtlı bir anlaşma) bir kez
    if (!name || !(it.k in out) || out[it.k].length >= (limit[it.k] || 0) || seen.has(it.q)) continue;
    seen.add(it.q);
    out[it.k].push([it.y, it.k, name, (it.s && (own ? it.s[lang] : it.s[fb])) || "", it.q]);
  }
  const by = (a, b) => a[0] - b[0];
  return [...out.e.sort(by), ...out.f.sort(by), ...out.b.sort(by), ...out.d.sort(by)];
}
