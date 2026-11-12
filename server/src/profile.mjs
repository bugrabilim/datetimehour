/* Şifre kuralı ve profil doğrulaması (standart madde 7 ve "Global düşün"). Aynı kurallar web formunda ve
   uygulamalarda önceden gösterilir; sunucu her durumda yeniden denetler. */
import { readFileSync } from "node:fs";

export const COUNTRIES = JSON.parse(readFileSync(new URL("../data/countries.json", import.meta.url), "utf8")).ulkeler;

/* Şifre: 8–128 karakter; en az bir rakam, bir büyük harf, bir küçük harf ve bir sembol (harf, rakam ve boşluk
   dışındaki yazdırılabilir karakter); boşluk yok. Yalnız yeni şifre belirlenirken uygulanır. */
export function passwordChecks(pw) {
  const s = String(pw ?? "");
  const len = [...s].length;
  return {
    length: len >= 8 && len <= 128,
    digit: /\p{Nd}/u.test(s),
    upper: /\p{Lu}/u.test(s),
    lower: /\p{Ll}/u.test(s),
    symbol: /[^\p{L}\p{N}\s\p{Cc}\p{Cf}\p{Cs}\p{Co}\p{Cn}]/u.test(s),
    noSpace: !/\s/u.test(s),
    printable: !/[\p{Cc}\p{Cf}\p{Cs}\p{Co}\p{Cn}]/u.test(s),
  };
}
export const passwordOk = (pw) => Object.values(passwordChecks(pw)).every(Boolean);

export const GENDERS = ["kadin", "erkek", "belirtmek-istemiyorum"];
export const SOCIAL = ["instagram", "x", "facebook", "linkedin", "youtube", "tiktok", "threads", "bluesky", "github", "telegram", "web"];
/* Görünürlükte seçilebilen alanlar (e-posta hiçbir zaman görünmez) */
export const VISIBLE_FIELDS = ["ad", "soyad", "takmaAd", "foto", "dogumTarihi", "cinsiyet", "telefon", "ulke", "sehir", "meslek", "hakkimda", "sosyal"];
/* Metin alanlarının üst sınırı: veritabanını korumak için; "Hakkımda" standarttaki gibi 160 */
const MAX = { ad: 100, soyad: 100, takmaAd: 60, sehir: 100, meslek: 100, hakkimda: 160, sosyalDeger: 300 };

export const PHOTO_MAX_BYTES = 200 * 1024;

/* Doğum tarihi: 16 ile 90 yaş arası (bugünün tarihine göre, UTC) */
export function birthDateRange(now = new Date()) {
  const y = now.getUTCFullYear(), m = now.getUTCMonth(), d = now.getUTCDate();
  const iso = (yy) => new Date(Date.UTC(yy, m, d)).toISOString().slice(0, 10);
  // En geç: bugün 16 yaşını dolduran; en erken: 91. yaşına bir gün kalan (90 yaşında)
  const latest = iso(y - 16);
  const earliestD = new Date(Date.UTC(y - 91, m, d));
  earliestD.setUTCDate(earliestD.getUTCDate() + 1);
  return { earliest: earliestD.toISOString().slice(0, 10), latest };
}

const clean = (v) => (v == null ? null : String(v).normalize("NFC").replace(/[\p{Cc}\p{Cf}]/gu, "").replace(/\s+/g, " ").trim() || null);

function bad(field) {
  return Object.assign(new Error("gecersiz:" + field), { status: 400, field });
}

/* Gelen profil (standarttaki ortak biçim) doğrulanır ve temizlenir; foto ayrı denetlenir (checkPhoto) */
export function validateProfile(p, now = new Date()) {
  const out = {};
  for (const k of ["ad", "soyad", "takmaAd", "sehir", "meslek", "hakkimda"]) {
    const v = clean(p[k]);
    if (v && [...v].length > MAX[k]) throw bad(k);
    out[k] = v;
  }
  if (p.dogumTarihi) {
    const s = String(p.dogumTarihi);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s) || Number.isNaN(Date.parse(s + "T00:00:00Z")) || new Date(s + "T00:00:00Z").toISOString().slice(0, 10) !== s) throw bad("dogumTarihi");
    const r = birthDateRange(now);
    if (s < r.earliest || s > r.latest) throw bad("dogumTarihi");
    out.dogumTarihi = s;
  } else out.dogumTarihi = null;
  if (p.cinsiyet) {
    if (!GENDERS.includes(p.cinsiyet)) throw bad("cinsiyet");
    out.cinsiyet = p.cinsiyet;
  } else out.cinsiyet = null;
  if (p.ulke) {
    const c = String(p.ulke).toUpperCase();
    if (!(c in COUNTRIES)) throw bad("ulke");
    out.ulke = c;
  } else out.ulke = null;
  if (p.telefon && (p.telefon.numara || p.telefon.ulkeKodu)) {
    const code = String(p.telefon.ulkeKodu || "").replace(/^\+/, "");
    const num = String(p.telefon.numara || "").replace(/[\s().-]/g, "").replace(/^0+/, "");
    if (!/^\d{1,3}$/.test(code) || !Object.values(COUNTRIES).includes(code)) throw bad("telefon");
    // E.164: en çok 15 rakam
    if (!/^\d{4,14}$/.test(num) || code.length + num.length > 15) throw bad("telefon");
    out.telefon = { ulkeKodu: "+" + code, numara: num };
  } else out.telefon = null;
  const social = Array.isArray(p.sosyal) ? p.sosyal : [];
  if (social.length > 8) throw bad("sosyal");
  out.sosyal = social.map((s) => {
    const platform = String(s?.platform || "");
    const deger = clean(s?.deger);
    if (!SOCIAL.includes(platform) || !deger || [...deger].length > MAX.sosyalDeger) throw bad("sosyal");
    if (platform === "web" && !/^https?:\/\/[^\s/$.?#].[^\s]*$/i.test(deger)) throw bad("sosyal");
    return { platform, deger };
  });
  if (!out.sosyal.length) out.sosyal = null;
  const g = p.gorunurluk || {};
  const alanlar = Array.isArray(g.alanlar) ? [...new Set(g.alanlar.filter((a) => VISIBLE_FIELDS.includes(a)))] : [];
  out.gorunurluk = { acik: g.acik === true, alanlar };
  return out;
}

/* Fotoğraf: data:image/webp;base64,… en çok 200 KB, gerçekten WebP (RIFF….WEBP) */
export function checkPhoto(dataUrl) {
  if (dataUrl == null || dataUrl === "") return null;
  const m = /^data:image\/webp;base64,([A-Za-z0-9+/]+={0,2})$/.exec(String(dataUrl));
  if (!m) throw bad("foto");
  const buf = Buffer.from(m[1], "base64");
  if (buf.length > PHOTO_MAX_BYTES || buf.length < 16) throw bad("foto");
  if (buf.toString("ascii", 0, 4) !== "RIFF" || buf.toString("ascii", 8, 12) !== "WEBP") throw bad("foto");
  return `data:image/webp;base64,${buf.toString("base64")}`;
}

/* Görünen ad (standart madde 7): görünür takma ad; yoksa görünür ad (soyad da görünürse onunla); yoksa "Üye 10458" */
export function displayName(profile, uyeNo, lang) {
  const g = profile?.gorunurluk || {};
  const vis = (k) => g.acik && (g.alanlar || []).includes(k) && profile[k];
  if (vis("takmaAd")) return profile.takmaAd;
  if (vis("ad")) return vis("soyad") ? `${profile.ad} ${profile.soyad}` : profile.ad;
  return `${lang === "tr" ? "Üye" : "Member"} ${uyeNo ?? ""}`.trim();
}
