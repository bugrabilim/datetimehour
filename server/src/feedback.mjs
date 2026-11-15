/* Hata bildirimi (sahibin isteği, 15 Kasım 2026): sitede /hata-bildir sayfası, uygulamalarda Ayarlar › Destek. Hesap
   gerekmez. Bildirim veritabanına yazılmaz; yalnız e-postayla bilgi@bumbagroup.com'a gider (Resend), yanıt için
   kişinin yazdığı adres Reply-To olur. Aydınlatma: account.json `fbPrivacy` ve sitenin gizlilik sayfası. */
import { normLang } from "./config.mjs";

export const FEEDBACK_MIN = 10;
export const FEEDBACK_MAX = 4000;
const SOURCES = new Set(["web", "ios", "android"]);
// Kontrol karakterleri (satır sonu ve sekme hariç) atılır; kısa alanlar tek satır
const clean = (v, max) => String(v ?? "").replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/g, "").trim().slice(0, max);
const line = (v, max) => clean(v, max).replace(/\s+/g, " ");

/* Gelen gövdeyi doğrular: {mesaj, eposta?, kaynak, surum?, sayfa?, cihaz?, dil?}. Hata: {hata} */
export function validateFeedback(b = {}) {
  const mesaj = clean(b.mesaj, FEEDBACK_MAX + 1);
  if (mesaj.length < FEEDBACK_MIN) return { hata: "kisa" };
  if (mesaj.length > FEEDBACK_MAX) return { hata: "uzun" };
  const eposta = line(b.eposta, 254).toLowerCase();
  if (eposta && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(eposta)) return { hata: "eposta" };
  const kaynak = SOURCES.has(b.kaynak) ? b.kaynak : "web";
  const sayfa = line(b.sayfa, 300);
  return {
    mesaj, eposta: eposta || null, kaynak,
    surum: line(b.surum, 60) || null,
    sayfa: sayfa.startsWith("/") ? sayfa : null,
    cihaz: line(b.cihaz, 120) || null,
    dil: normLang(b.dil),
  };
}

/* Sahibe giden e-posta (Türkçe; düz metin) */
export function feedbackMail(f, siteUrl) {
  const where = { web: "Site", ios: "iPhone/iPad uygulaması", android: "Android uygulaması" }[f.kaynak];
  const first = f.mesaj.split("\n")[0].slice(0, 60);
  const subject = `Saat Tarih hata bildirimi (${where}): ${first}`;
  const rows = [
    ["Kaynak", where],
    ["Sürüm", f.surum],
    ["Sayfa", f.sayfa ? siteUrl + f.sayfa : null],
    ["Cihaz", f.cihaz],
    ["Dil", f.dil],
    ["E-posta", f.eposta || "yazılmadı (yanıt verilemez)"],
  ].filter(([, v]) => v);
  const text = `${f.mesaj}\n\n—\n${rows.map(([k, v]) => `${k}: ${v}`).join("\n")}\n`;
  return { subject, text, replyTo: f.eposta };
}
