/* Saat Tarih üyelik sunucusu: ayarlar ortam değişkenlerinden (gizli değerler koda ve depoya yazılmaz).
   Standart madde 7: değişken adları her projede aynı. Zorunlulardan biri eksikse sunucu çökmez, üyelik kapalı açılır
   (giriş ve kayıt sayfaları "üyelik şu an kapalı" der, üyelik ve görev uçları 503); günlüğe yalnız eksik adın adı yazılır. */

export const LANGS = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th", "es", "pt", "hi", "bn"];
export const RTL = new Set(["ar", "ur", "fa"]);

/* Ortak üyelik metinlerinin sürümü (standart, Sabit bilgiler: "Üyelik metinleri sürümü"); kayıttaki onay kaydına yazılır */
export const TERMS_VERSION = "2026-11-11";
/* Ortak üyelik servisindeki ürün kimliği (standart madde 7, "Ortak üyelik servisi") */
export const SITE_KEY = "saat-tarih";
export const PRODUCT_NAME = "Saat Tarih";

export function normLang(v) {
  const s = String(v || "").toLowerCase().replace("_", "-");
  if (LANGS.includes(s)) return s;
  const base = s.split("-")[0];
  if (base === "in") return "id";
  if (base === "tl") return "fil";
  return LANGS.includes(base) ? base : "en";
}

const trim = (u) => String(u || "").replace(/\/+$/, "");

/* Zorunlu değişkenler (standart madde 7). Testler ve yerel deneme MAIL_DRIVER ile Resend'siz çalışır. */
const REQUIRED = ["DATABASE_URL", "BETTER_AUTH_SECRET", "BETTER_AUTH_URL", "RESEND_API_KEY", "CRON_SECRET", "UYELIK_ANAHTARI"];

export function loadConfig(env = process.env) {
  const production = env.NODE_ENV === "production";
  const mailDriver = env.MAIL_DRIVER || (env.RESEND_API_KEY ? "resend" : "log");
  const cfg = {
    production,
    port: Number(env.PORT) || 8080,
    databaseUrl: env.DATABASE_URL || "",
    secret: env.BETTER_AUTH_SECRET || "",
    publicUrl: trim(env.BETTER_AUTH_URL || ""),
    siteUrl: "https://time.bumba.tr",
    resendKey: env.RESEND_API_KEY || "",
    mailFrom: `${PRODUCT_NAME} <bilgi@bumbagroup.com>`,
    mailDriver,
    cronSecret: env.CRON_SECRET || "",
    centerKey: env.UYELIK_ANAHTARI || "",
    // Ortak üyelik servisinin adresi sabittir; testler sahte merkezi UYELIK_MERKEZ ile verir
    centerUrl: trim(env.UYELIK_MERKEZ || "https://bumbagroup.com/api/uyelik"),
    adminEmail: String(env.ADMIN_EMAIL || "bilgi@bumbagroup.com").toLowerCase(),
    contactEmail: "bilgi@bumbagroup.com",
    commit: env.SOURCE_COMMIT || env.COMMIT || "",
    rateLimit: env.RATE_LIMIT !== "off",
  };
  const missing = REQUIRED.filter((k) => {
    if (k === "RESEND_API_KEY") return mailDriver === "resend" ? !cfg.resendKey : false;
    return !String(env[k] || "").trim();
  });
  if (cfg.secret && cfg.secret.length < 32 && !missing.includes("BETTER_AUTH_SECRET")) missing.push("BETTER_AUTH_SECRET");
  if (cfg.cronSecret && cfg.cronSecret.length < 32 && !missing.includes("CRON_SECRET")) missing.push("CRON_SECRET");
  if (production && mailDriver !== "resend" && !missing.includes("RESEND_API_KEY")) missing.push("RESEND_API_KEY");
  cfg.missing = missing;
  cfg.open = missing.length === 0;
  if (!cfg.publicUrl) cfg.publicUrl = "https://hesap.time.bumba.tr";
  return cfg;
}

/* Ortak hukuki metinler (standart, Sabit bilgiler). Türkçe arayüzden Türkçesi, öbür dillerden İngilizcesi açılır
   (12 Kasım 2026, sahibin kararı). */
export function legalLinks(lang) {
  const q = normLang(lang) === "tr" ? "" : "?dil=en";
  return {
    notice: `https://bumbagroup.com/uyelik-aydinlatma${q}`,
    terms: `https://bumbagroup.com/uyelik-sozlesmesi${q}`,
  };
}
