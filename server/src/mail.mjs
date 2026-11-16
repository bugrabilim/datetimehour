/* Üyelik e-postaları (standart madde 7 ve "E-posta gönderimi"): Resend ile bilgi@bumbagroup.com'dan, gönderen adı
   ürünün adı. Yalnız standarttaki liste: doğrulama, şifre sıfırlama, "şifreniz değişti", e-posta değişikliği onayı,
   "hesabınız silindi", yeniden kayıt denemesi, askı, yeniden açılma, kalıcı kapatma, başka üründe üyelik açıldı ya da
   kapandı, "hesaplarınız birleştirildi" (şifre seçimi bağlantısıyla), kutlama ve "hesabınız açıldı" (e-posta doğrulanınca; sahibin
   kararı, 15 Kasım 2026, standarda eklenmesi istendi: yalnız işlem bilgisi ve hesap bağlantısı). Hoş geldin, tanıtım ya da bülten yok. Metin + HTML; açılma ve tıklama izleme yok (Resend'de alan
   adı ayarı, kodda izleme parametresi açılmaz). Metinler 19 dilde (i18n/mail.json); İngilizceye düşülmez. */
import crypto from "node:crypto";
import { readFileSync } from "node:fs";
import { normLang, RTL, PRODUCT_NAME } from "./config.mjs";

export const TEXT = JSON.parse(readFileSync(new URL("../i18n/mail.json", import.meta.url), "utf8"));

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

/* Kutlama e-postasını girişsiz kapatma bağlantısının anahtarı: hesap kimliği + HMAC */
export function unsubscribeToken(secret, userId) {
  const sig = crypto.createHmac("sha256", secret).update("kutlama:" + userId).digest("base64url").slice(0, 32);
  return `${Buffer.from(userId).toString("base64url")}.${sig}`;
}
export function readUnsubscribeToken(secret, token) {
  const [id64, sig] = String(token || "").split(".");
  if (!id64 || !sig) return null;
  const userId = Buffer.from(id64, "base64url").toString("utf8");
  const want = unsubscribeToken(secret, userId).split(".")[1];
  if (sig.length !== want.length || !crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(want))) return null;
  return userId;
}

/* "Hesaplarınız birleştirildi" e-postasındaki şifre seçimi bağlantısı: hesap kimliği + bitiş anı + HMAC; 7 gün geçerli.
   Tek kullanımlıktır, çünkü seçim yapılınca merkezde bekleyen seçim kalmaz (sifre-sec → 404 secim-yok). Oturum açmaz. */
export const CHOICE_DAYS = 7;
export function choiceToken(secret, userId, exp = Date.now() + CHOICE_DAYS * 864e5) {
  const body = `${Buffer.from(userId).toString("base64url")}.${Math.floor(exp / 1000).toString(36)}`;
  const sig = crypto.createHmac("sha256", secret).update("sifre-sec:" + body).digest("base64url").slice(0, 32);
  return `${body}.${sig}`;
}
export function readChoiceToken(secret, token) {
  const [id64, exp36, sig] = String(token || "").split(".");
  if (!id64 || !exp36 || !sig) return null;
  const want = crypto.createHmac("sha256", secret).update(`sifre-sec:${id64}.${exp36}`).digest("base64url").slice(0, 32);
  if (sig.length !== want.length || !crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(want))) return null;
  if (parseInt(exp36, 36) * 1000 < Date.now()) return null;
  return Buffer.from(id64, "base64url").toString("utf8");
}

function fmtDate(lang, d) {
  try {
    return new Intl.DateTimeFormat(`${lang === "zh" ? "zh-CN" : lang}-u-nu-latn`, { dateStyle: "long", timeZone: "UTC" }).format(new Date(d));
  } catch {
    return new Date(d).toISOString().slice(0, 10);
  }
}

export function render(kind, lang, vars = {}, cfg = {}) {
  const l = normLang(lang);
  const t = TEXT[l];
  let k = t[kind];
  if (kind === "celebrate") k = t.celebrate[vars.kind];
  const selam = vars.ad ? t.hello.replace("{ad}", vars.ad) : t.helloNoName;
  const v = {
    ...vars,
    urun: PRODUCT_NAME,
    iletisim: "bilgi@bumbagroup.com",
    selam,
    gun: vars.name ?? "",
    n: vars.years ?? "",
    tarih: vars.until ? fmtDate(l, vars.until) : "",
    sebep: vars.reason || "—",
  };
  const fill = (s) => String(s).replace(/\{(\w+)\}/g, (_, n) => v[n] ?? "");
  const subject = fill(k.subject);
  let body = fill(k.body);
  if (kind === "suspended" && vars.until) body += " " + fill(t.suspendedUntil);
  if (kind === "opened" && vars.no != null) body += " " + fill(t.openedNo);
  const button = k.button && vars.url ? k.button : "";
  const ignore = ["verify", "reset", "changeOld"].includes(kind) ? t.ignore : "";
  const celebrate = kind === "celebrate";
  const unsubUrl = celebrate && cfg.publicUrl && vars.userId
    ? `${cfg.publicUrl}/kutlama?dil=${l}&k=${unsubscribeToken(cfg.secret, vars.userId)}` : "";
  const footer = celebrate ? t.footerCelebrate : t.footer;
  const dir = RTL.has(l) ? "rtl" : "ltr";
  const html = `<!doctype html><html lang="${l}" dir="${dir}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${esc(subject)}</title></head>
<body style="margin:0;padding:0;background:#f6f0e4;color:#2b2520;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f6f0e4"><tr><td align="center" style="padding:28px 14px">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#fffbf4;border-radius:18px"><tr><td style="padding:28px 26px" dir="${dir}">
<p style="margin:0 0 6px;font-size:13px;color:#6a5f55">${PRODUCT_NAME} · time.bumba.tr</p>
<h1 style="margin:0 0 16px;font-size:22px;line-height:1.3">${esc(subject)}</h1>
<p style="margin:0 0 22px;font-size:16px;line-height:1.55">${esc(body)}</p>
${button ? `<p style="margin:0 0 22px"><a href="${esc(vars.url)}" style="display:inline-block;background:#9a3f00;color:#ffffff;text-decoration:none;font-weight:600;padding:12px 20px;border-radius:12px">${esc(button)}</a></p>
<p style="margin:0 0 18px;font-size:13px;line-height:1.5;color:#6a5f55;word-break:break-all">${esc(vars.url)}</p>` : ""}
${ignore ? `<p style="margin:0 0 12px;font-size:14px;line-height:1.5;color:#6a5f55">${esc(ignore)}</p>` : ""}
<p style="margin:18px 0 0;font-size:12px;line-height:1.5;color:#6a5f55">${esc(footer)}</p>
${unsubUrl ? `<p style="margin:10px 0 0;font-size:12px"><a href="${esc(unsubUrl)}" style="color:#6a5f55">${esc(t.unsubscribe)}</a></p>` : ""}
</td></tr></table></td></tr></table></body></html>`;
  const text = [subject, "", body, button ? `\n${button}: ${vars.url}` : "", ignore ? `\n${ignore}` : "", "", footer, unsubUrl ? `${t.unsubscribe}: ${unsubUrl}` : ""]
    .join("\n").replace(/\n{3,}/g, "\n\n").trim() + "\n";
  return { subject, html, text, unsubUrl };
}

/* Gönderici: "resend" (canlı), "memory" (testler; kutuya yazar), "log" (yerel deneme; konsola yalnız tür ve alıcının ilk harfi) */
export function createMailer(cfg) {
  const outbox = [];
  const state = { lastOk: null, lastError: null, lastAt: null };
  async function send(kind, to, lang, vars = {}, { scheduledAt } = {}) {
    const m = render(kind, lang, vars, cfg);
    state.lastAt = new Date();
    if (cfg.mailDriver === "memory") {
      outbox.push({ kind, to, lang: normLang(lang), vars, scheduledAt: scheduledAt || null, ...m });
      state.lastOk = true;
      return;
    }
    if (cfg.mailDriver !== "resend") {
      console.log(`[e-posta] ${kind} → ${to.replace(/^(.).*@/, "$1…@")}`);
      state.lastOk = true;
      return;
    }
    const body = { from: cfg.mailFrom, to: [to], subject: m.subject, html: m.html, text: m.text };
    if (scheduledAt) body.scheduled_at = new Date(scheduledAt).toISOString();
    if (m.unsubUrl) body.headers = { "List-Unsubscribe": `<${m.unsubUrl}>`, "List-Unsubscribe-Post": "List-Unsubscribe=One-Click" };
    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: { Authorization: `Bearer ${cfg.resendKey}`, "Content-Type": "application/json" },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(15000),
    }).catch((e) => ({ ok: false, status: 0 }));
    state.lastOk = res.ok;
    if (!res.ok) {
      state.lastError = `${res.status}`;
      console.error(`[e-posta] gönderilemedi (${res.status}) ${kind}`);
      throw new Error("mail_failed");
    }
  }
  /* İçe giden düz metin e-posta (hata bildirimi; sahibin kutusuna). Testlerde kutuya yazılır. */
  async function sendRaw({ to, subject, text, replyTo }) {
    state.lastAt = new Date();
    if (cfg.mailDriver === "memory") {
      outbox.push({ kind: "raw", to, subject, text, replyTo: replyTo || null });
      state.lastOk = true;
      return;
    }
    if (cfg.mailDriver !== "resend") {
      console.log(`[e-posta] ham → ${to.replace(/^(.).*@/, "$1…@")}`);
      state.lastOk = true;
      return;
    }
    const body = { from: cfg.mailFrom, to: [to], subject, text };
    if (replyTo) body.reply_to = replyTo;
    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: { Authorization: `Bearer ${cfg.resendKey}`, "Content-Type": "application/json" },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(15000),
    }).catch(() => ({ ok: false, status: 0 }));
    state.lastOk = res.ok;
    if (!res.ok) {
      state.lastError = `${res.status}`;
      console.error(`[e-posta] gönderilemedi (${res.status}) ham`);
      throw new Error("mail_failed");
    }
  }
  return { send, sendRaw, outbox, state };
}
