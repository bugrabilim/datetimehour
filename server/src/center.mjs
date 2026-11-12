/* Ortak üyelik servisinin istemcisi (standart madde 7, "Ortak üyelik servisi"): bumbagroup.com/api/uyelik/<işlem>.
   Her istekte ürüne özel anahtar (UYELIK_ANAHTARI) ve site anahtarı; yanıt en geç 8 saniye beklenir.
   Ulaşılamazsa { ok: false, status: 0 } döner; çağıran kendi kuralına göre sürdürür (ör. numara temizlikte alınır). */
import { SITE_KEY } from "./config.mjs";

export function createCenter(cfg, fetchImpl = fetch) {
  async function call(op, body) {
    if (!cfg.centerKey) return { ok: false, status: 0, data: null };
    try {
      const res = await fetchImpl(`${cfg.centerUrl}/${op}`, {
        method: "POST",
        headers: { Authorization: `Bearer ${cfg.centerKey}`, "X-Bumba-Site": SITE_KEY, "Content-Type": "application/json" },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(8000),
      });
      let data = null;
      try { data = await res.json(); } catch {}
      return { ok: res.ok, status: res.status, data };
    } catch (e) {
      console.error(`[merkez] ${op}: ulaşılamadı`);
      return { ok: false, status: 0, data: null };
    }
  }
  return {
    call,
    sorgula: (eposta, uyeNo) => call("sorgula", uyeNo ? { eposta, uyeNo } : { eposta }),
    kayit: (eposta) => call("kayit", { eposta }),
    bagla: (eposta, sifre) => call("bagla", { eposta, sifre }),
    urunler: (uyeNo) => call("urunler", { uyeNo }),
    ac: (uyeNo, hedefler, hesap) => call("ac", { uyeNo, hedefler, hesap }),
    kapat: (uyeNo, hedefler) => call("kapat", { uyeNo, hedefler }),
    guncelle: (uyeNo, degisiklik) => call("guncelle", { uyeNo, degisiklik }),
    ayril: (uyeNo) => call("ayril", { uyeNo }),
    sil: (uyeNo) => call("sil", { uyeNo }),
    bildir: (hesaplar) => call("bildir", { hesaplar }),
    birlestir: (uyeNo, eposta, sifre) => call("birlestir", { uyeNo, eposta, sifre }),
  };
}
