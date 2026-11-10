/* Saat Tarih hesap sunucusu: ayarlar ortam değişkenlerinden (gizli değerler koda ve depoya yazılmaz). */

export const LANGS = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru", "ja", "ko", "zh", "fil", "th", "es", "pt", "hi", "bn"];
export const RTL = new Set(["ar", "ur", "fa"]);

export function normLang(v) {
  const s = String(v || "").toLowerCase().replace("_", "-");
  if (LANGS.includes(s)) return s;
  const base = s.split("-")[0];
  if (base === "in") return "id";
  if (base === "tl") return "fil";
  return LANGS.includes(base) ? base : "en";
}

const trim = (u) => String(u || "").replace(/\/+$/, "");

export function loadConfig(env = process.env) {
  const production = env.NODE_ENV === "production";
  const cfg = {
    production,
    port: Number(env.PORT) || 8080,
    databaseUrl: env.DATABASE_URL || "",
    secret: env.BETTER_AUTH_SECRET || "",
    publicUrl: trim(env.PUBLIC_URL || "https://hesap.time.bumba.tr"),
    siteUrl: trim(env.SITE_URL || "https://time.bumba.tr"),
    resendKey: env.RESEND_API_KEY || "",
    mailFrom: env.MAIL_FROM || "Saat Tarih <bilgi@bumbagroup.com>",
    mailDriver: env.MAIL_DRIVER || (env.RESEND_API_KEY ? "resend" : "log"),
    adminEmail: String(env.ADMIN_EMAIL || "bilgi@bumbagroup.com").toLowerCase(),
    umamiUrl: trim(env.UMAMI_URL || "https://istatistik.bumba.tr"),
    umamiShareId: env.UMAMI_SHARE_ID || "",
    commit: env.SOURCE_COMMIT || env.COMMIT || "",
    rateLimit: env.RATE_LIMIT !== "off",
  };
  const missing = [];
  if (!cfg.databaseUrl) missing.push("DATABASE_URL");
  if (cfg.secret.length < 32) missing.push("BETTER_AUTH_SECRET (en az 32 karakter)");
  if (production && cfg.mailDriver !== "resend") missing.push("RESEND_API_KEY");
  if (missing.length) throw new Error("Eksik ortam değişkeni: " + missing.join(", "));
  return cfg;
}

/* Sitedeki hesap sayfaları (statik, noindex): tr kökte Türkçe adla, diğer diller /<dil>/account/… */
export function siteLink(cfg, lang, page) {
  const l = normLang(lang);
  const tr = { password: "/hesap/sifre/", verified: "/hesap/dogrulandi/" };
  const other = { password: "/account/password/", verified: "/account/verified/" };
  return cfg.siteUrl + (l === "tr" ? tr[page] : `/${l}${other[page]}`);
}
