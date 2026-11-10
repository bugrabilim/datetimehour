/* Hesap e-postaları (standart "E-posta gönderimi"): Resend ile bilgi@bumbagroup.com'dan, gönderen adı ürünün adı.
   Tanıtım e-postası yok; yalnız doğrulama, şifre sıfırlama, adres değişikliği ve silme bildirimi. Metinler 19 dilde. */
import { readFileSync } from "node:fs";
import { normLang, RTL } from "./config.mjs";

const TEXT = JSON.parse(readFileSync(new URL("../i18n/mail.json", import.meta.url), "utf8"));

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

export function render(kind, lang, vars = {}) {
  const l = normLang(lang);
  const t = TEXT[l] || TEXT.en;
  const k = t[kind] || TEXT.en[kind];
  const fill = (s) => String(s).replace(/\{(\w+)\}/g, (_, n) => vars[n] ?? "");
  const body = fill(k.body);
  const button = k.button && vars.url ? k.button : "";
  const ignore = ["verify", "reset", "changeOld"].includes(kind) ? t.ignore : "";
  const dir = RTL.has(l) ? "rtl" : "ltr";
  const html = `<!doctype html><html lang="${l}" dir="${dir}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${esc(k.subject)}</title></head>
<body style="margin:0;padding:0;background:#f6f0e4;color:#2b2520;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f6f0e4"><tr><td align="center" style="padding:28px 14px">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#fffbf4;border-radius:18px"><tr><td style="padding:28px 26px" dir="${dir}">
<p style="margin:0 0 6px;font-size:13px;color:#6a5f55">Saat Tarih · time.bumba.tr</p>
<h1 style="margin:0 0 16px;font-size:22px;line-height:1.3">${esc(k.subject)}</h1>
<p style="margin:0 0 22px;font-size:16px;line-height:1.55">${esc(body)}</p>
${button ? `<p style="margin:0 0 22px"><a href="${esc(vars.url)}" style="display:inline-block;background:#9a3f00;color:#ffffff;text-decoration:none;font-weight:600;padding:12px 20px;border-radius:12px">${esc(button)}</a></p>
<p style="margin:0 0 18px;font-size:13px;line-height:1.5;color:#6a5f55;word-break:break-all">${esc(vars.url)}</p>` : ""}
${ignore ? `<p style="margin:0 0 12px;font-size:14px;line-height:1.5;color:#6a5f55">${esc(ignore)}</p>` : ""}
<p style="margin:18px 0 0;font-size:12px;line-height:1.5;color:#6a5f55">${esc(t.footer)}</p>
</td></tr></table></td></tr></table></body></html>`;
  const text = [k.subject, "", body, button ? `\n${button}: ${vars.url}` : "", ignore ? `\n${ignore}` : "", "", t.footer].join("\n").replace(/\n{3,}/g, "\n\n");
  return { subject: k.subject, html, text };
}

/* Gönderici: "resend" (canlı), "memory" (testler; kutuya yazar), "log" (yerel deneme; konsola yalnız konu ve alıcı) */
export function createMailer(cfg) {
  const outbox = [];
  const state = { lastOk: null, lastError: null, lastAt: null };
  async function send(kind, to, lang, vars) {
    const m = render(kind, lang, vars);
    state.lastAt = new Date();
    if (cfg.mailDriver === "memory") {
      outbox.push({ kind, to, lang: normLang(lang), vars, ...m });
      state.lastOk = true;
      return;
    }
    if (cfg.mailDriver !== "resend") {
      console.log(`[e-posta] ${kind} → ${to.replace(/^(.).*@/, "$1…@")}`);
      state.lastOk = true;
      return;
    }
    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: { Authorization: `Bearer ${cfg.resendKey}`, "Content-Type": "application/json" },
      body: JSON.stringify({ from: cfg.mailFrom, to: [to], subject: m.subject, html: m.html, text: m.text }),
      signal: AbortSignal.timeout(15000),
    }).catch((e) => ({ ok: false, status: 0, text: async () => String(e) }));
    state.lastOk = res.ok;
    if (!res.ok) {
      state.lastError = `${res.status}`;
      console.error(`[e-posta] gönderilemedi (${res.status}) ${kind}`);
      throw new Error("mail_failed");
    }
  }
  return { send, outbox, state };
}
