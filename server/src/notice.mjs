/* Tek seferlik hukuki bilgilendirme (standart 5.33, 22 Kasım 2026): ortak üyelik aydınlatma metnine "7. Ürün
   bildirimleri" bölümü eklendi; her üyeye bir kez kısa e-posta gider. Metin standartta verilen metindir: Türkçe
   arayüzdeki üyeye Türkçe, öbür dillerdekilere İngilizce (standart yalnız bu iki dilin metnini verir), ürün adı
   İngilizcede "Time & Date". Doğrulanmamış, askıdaki ve yasaklı hesaplara ve yönetici hesabına gitmez. Hukuki
   bilgilendirmedir: kapatma bağlantısı yoktur. Günlük temizlik görevi gönderir (cleanup.mjs); gönderilen üye
   `notice_sent`'e yazılır, gönderilemeyen ertesi gün yeniden denenir. Yeni bir bilgilendirme gerekirse yeni bir
   `NOTICE` kimliğiyle eklenir. */

export const NOTICE = "aydinlatma-2026-11-22";

const T = {
  tr: {
    product: "Saat Tarih",
    subject: "Üyelik aydınlatma metni güncellendi",
    paras: [
      "Merhaba,",
      "{Ürün} üyeliğiniz için geçerli üyelik aydınlatma metnine yeni bir bölüm ekledik: \"7. Ürün bildirimleri\". Üye olduğunuz site ya da uygulama, kullandığınız hizmetle ilgili size ara bilgilendirmeler gönderebilir (ör. yeni bir bölüm açıldı, sıranız geldi, başlattığınız iş bitti). Bunlar tanıtım, kampanya ya da teklif içermez. E-postayla gelenleri her bildirim e-postasının altındaki bağlantıdan ya da hesap sayfanızdan kapatabilirsiniz. Tarayıcı ve telefon bildirimleri yalnız siz izin verirseniz gelir; tarayıcınızın ya da telefonunuzun bildirim servisi üzerinden iletilir. Sizden bir şey yapmanız gerekmiyor.",
      "Metnin tamamı: {url}",
    ],
    url: "https://bumbagroup.com/uyelik-aydinlatma",
    footer: ["Bu e-postayı {Ürün} üyesi olduğunuz için aldınız.", "Bumba Group"],
  },
  en: {
    product: "Time & Date",
    subject: "Membership privacy notice updated",
    paras: [
      "Hello,",
      "We have added a new section to the membership privacy notice that applies to your {Ürün} membership: \"7. Product notifications\". The website or app you are a member of may send you interim updates about the service you use (e.g. a new chapter is available, your turn has come, a task you started has finished). They contain no promotion, campaign or offer. You can turn off email notifications from the link at the bottom of every notification email or on your account page. Browser and phone notifications come only if you allow them; they are delivered through your browser's or phone's notification service. You don't need to do anything.",
      "Full text: {url}",
    ],
    url: "https://bumbagroup.com/en/uyelik-aydinlatma",
    footer: ["You are receiving this email because you are a {Ürün} member.", "Bumba Group"],
  },
};

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

export function renderNotice(lang) {
  const l = lang === "tr" ? "tr" : "en";
  const t = T[l];
  const fill = (s) => s.replaceAll("{Ürün}", t.product).replaceAll("{url}", t.url);
  const paras = t.paras.map(fill);
  const footer = t.footer.map(fill);
  const text = [...paras, "", ...footer].join("\n\n").replace(/\n{3,}/g, "\n\n") + "\n";
  const p = (s) => {
    const html = esc(s).replace(esc(t.url), `<a href="${t.url}" style="color:#9a3f00">${esc(t.url)}</a>`);
    return `<p style="margin:0 0 14px;font-size:16px;line-height:1.55">${html}</p>`;
  };
  const html = `<!doctype html><html lang="${l}" dir="ltr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${esc(t.subject)}</title></head>
<body style="margin:0;padding:0;background:#f6f0e4;color:#2b2520;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f6f0e4"><tr><td align="center" style="padding:28px 14px">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#fffbf4;border-radius:18px"><tr><td style="padding:28px 26px">
<h1 style="margin:0 0 16px;font-size:22px;line-height:1.3">${esc(t.subject)}</h1>
${paras.map(p).join("\n")}
<p style="margin:18px 0 0;font-size:12px;line-height:1.5;color:#6a5f55">${footer.map(esc).join("<br>")}</p>
</td></tr></table></td></tr></table></body></html>`;
  return { subject: t.subject, text, html };
}

/* Gönderilmemiş uygun üyelere gönderir; sonuç: gönderilen ve gönderilemeyen sayısı */
export async function sendNotices({ pool, mailer, adminEmail = "" }) {
  const out = { sent: 0, failed: 0 };
  const r = await pool.query(
    `select u.id, u.email, u.lang from "user" u
       where u."emailVerified" = true and coalesce(u.banned, false) = false and lower(u.email) <> $1
         and not exists (select 1 from notice_sent n where n.user_id = u.id and n.notice = $2)`,
    [String(adminEmail).toLowerCase(), NOTICE],
  );
  for (const u of r.rows) {
    const m = renderNotice(u.lang);
    try {
      await mailer.sendRaw({ to: u.email, subject: m.subject, text: m.text, html: m.html, kind: "notice" });
      await pool.query(`insert into notice_sent (user_id, notice) values ($1, $2) on conflict do nothing`, [u.id, NOTICE]);
      out.sent++;
    } catch {
      out.failed++;
    }
  }
  return out;
}
