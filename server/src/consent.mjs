/* Kayıttaki onay cümlesi (standart 5.20, "Hukuk", onay 1): çeviriler merkezde durur,
   `GET https://bumbagroup.com/api/uyelik/onay-cumlesi?dil=hepsi` → `{surum, diller: [{kod, ad, rtl, metin}]}`.
   Sunucu metni merkezden alır (açılışta ve saatte bir), bellekte tutar; merkeze ulaşılamazsa ya da bir dil eksikse
   `i18n/account.json` içindeki kopyaya düşer (merkezdeki 2026-11-14 metniyle aynı). Onay kaydının `surum`u standarttaki
   gibi ortak metinlerin sürümüdür (`TERMS_VERSION`); cümlenin sürümü uygulamalara `/v1/onay-cumlesi`'nde gider. */
import { readFileSync } from "node:fs";
import { LANGS, RTL, normLang } from "./config.mjs";

const UI = JSON.parse(readFileSync(new URL("../i18n/account.json", import.meta.url), "utf8"));
/* Kodda duran kopyanın sürümü (merkezde aynı metin bu sürümle yayımlandı) */
export const BUNDLED_VERSION = "2026-11-14";
const REFRESH_MS = 60 * 60 * 1000;

/* Metindeki bağlantılar yalnız ortak metinlere gidebilir; başka adres içeren metin kabul edilmez */
function safe(metin) {
  if (typeof metin !== "string" || !metin.trim() || metin.length > 2000) return false;
  const links = [...metin.matchAll(/\]\(([^)\s]+)\)/g)].map((m) => m[1]);
  return links.length > 0 && links.every((u) => /^https:\/\/bumbagroup\.com\/uyelik-(aydinlatma|sozlesmesi)(\?dil=en)?$/.test(u));
}

export function createConsent({ url, fetchImpl = fetch, log = console } = {}) {
  let remote = null; // { surum, metin: Map<kod, metin> }
  let at = 0;
  let pending = null;

  async function refresh() {
    if (!url) return false;
    if (pending) return pending;
    pending = (async () => {
      try {
        const r = await fetchImpl(`${url}?dil=hepsi`, { headers: { accept: "application/json" }, signal: AbortSignal.timeout(8000) });
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        const d = await r.json();
        const surum = String(d?.surum || "");
        if (!/^\d{4}-\d{2}-\d{2}$/.test(surum) || !Array.isArray(d?.diller)) throw new Error("biçim");
        const metin = new Map();
        for (const x of d.diller) if (LANGS.includes(x?.kod) && safe(x.metin)) metin.set(x.kod, x.metin);
        if (!metin.has("tr") || !metin.has("en")) throw new Error("tr/en yok");
        remote = { surum, metin };
        at = Date.now();
        return true;
      } catch (e) {
        log.error?.(`[onay] merkezden alınamadı: ${e.message}`);
        at = Date.now() - REFRESH_MS + 5 * 60 * 1000; // 5 dk sonra yeniden dene
        return false;
      } finally {
        pending = null;
      }
    })();
    return pending;
  }

  /* Bir dilin metni ve sürümü. Merkezde o dil yoksa İngilizcesi (merkezin kuralı); hiç alınamadıysa koddaki kopya. */
  function get(lang) {
    const l = normLang(lang);
    if (url && Date.now() - at > REFRESH_MS) refresh();
    if (remote) {
      const metin = remote.metin.get(l) || remote.metin.get("en");
      return { surum: remote.surum, kod: remote.metin.has(l) ? l : "en", rtl: RTL.has(remote.metin.has(l) ? l : "en"), metin };
    }
    return { surum: BUNDLED_VERSION, kod: l, rtl: RTL.has(l), metin: UI[l]?.consent || UI.en.consent };
  }

  let timer = null;
  function start() {
    if (!url || timer) return;
    refresh();
    timer = setInterval(refresh, REFRESH_MS);
    timer.unref?.();
  }
  function stop() {
    if (timer) clearInterval(timer);
    timer = null;
  }
  return { get, refresh, start, stop, version: () => get("tr").surum };
}
