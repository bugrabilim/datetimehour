/* Yönetim paneli ve durum sayfası (standart madde 10): yalnız yönetici rolündeki hesap girer (sabit bilgilerdeki
   bilgi@bumbagroup.com, uygulamadan açılıp doğrulanınca). Yalnız toplu sayılar; tek kişinin içeriği ya da kişisel
   verisi görünmez. İki dil (tr, en), iki tema (sistem), noindex; site içi aramaya girmez. Ölçülemeyen satıra sayı
   uydurulmaz: "bu ortamda ölçülemiyor". */
import { fromNodeHeaders } from "better-auth/node";
import { readForm } from "./http.mjs";

const STARTED = new Date();
const T = {
  tr: {
    title: "Saat Tarih hesap paneli", login: "Yönetici girişi", email: "E-posta", password: "Şifre", signIn: "Giriş yap", signOut: "Çıkış",
    bad: "Giriş yapılamadı.", wait: "Çok fazla deneme. Biraz sonra yeniden deneyin.", notAdmin: "Bu hesap yönetici değil.",
    ranges: { bugun: "Bugün", 7: "Son 7 gün", 30: "Son 30 gün" }, app: "Uygulama", requests: "İstek", avg: "Ortalama yanıt süresi",
    errRate: "Hata oranı", dbSize: "Veritabanı boyutu", storage: "Depolama", members: "Üyeler", total: "Toplam", verified: "Doğrulanmış",
    newSignups: "Yeni kayıt", site: "Site istatistiği (time.bumba.tr, Umami)", visitors: "Ziyaretçi", views: "Sayfa görüntüleme",
    active: "Şu an sitede", topPages: "En çok görüntülenen sayfalar", referrers: "Kaynaklar", countries: "Ülke", devices: "Cihaz",
    searches: "En çok aranan ve sonuçsuz aramalar", services: "Servis durumu", svcApp: "Uygulama", svcDb: "Veritabanı",
    svcMail: "E-posta gönderimi", svcUmami: "Umami", ok: "çalışıyor", bad2: "sorunlu", na: "bu ortamda ölçülemiyor",
    deploy: "Son yayın", started: "başlangıç", server: "Sunucu yükü ve liste aboneleri", note: "Yalnız toplu sayılar gösterilir. Panel yalnız oturum çerezi kullanır (zorunlu).",
    searchNote: "site arama sözcüğünü göndermez",
  },
  en: {
    title: "Time & Date account panel", login: "Administrator sign-in", email: "Email", password: "Password", signIn: "Sign in", signOut: "Sign out",
    bad: "Could not sign in.", wait: "Too many attempts. Try again a little later.", notAdmin: "This account is not an administrator.",
    ranges: { bugun: "Today", 7: "Last 7 days", 30: "Last 30 days" }, app: "Application", requests: "Requests", avg: "Average response time",
    errRate: "Error rate", dbSize: "Database size", storage: "Storage", members: "Members", total: "Total", verified: "Verified",
    newSignups: "New sign-ups", site: "Site statistics (time.bumba.tr, Umami)", visitors: "Visitors", views: "Page views",
    active: "On the site now", topPages: "Most viewed pages", referrers: "Referrers", countries: "Country", devices: "Device",
    searches: "Top and no-result searches", services: "Service status", svcApp: "Application", svcDb: "Database",
    svcMail: "Email sending", svcUmami: "Umami", ok: "working", bad2: "having problems", na: "cannot be measured in this environment",
    deploy: "Last deployment", started: "started", server: "Server load and list subscribers", note: "Only aggregate numbers are shown. The panel uses only a session cookie (necessary).",
    searchNote: "the site does not send search terms",
  },
};

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

const CSS = `:root{--bg:#f6f0e4;--surface:#fffbf4;--text:#2b2520;--muted:#6a5f55;--line:#e2d6c1;--accent:#9a3f00;--on:#fff;--ok:#2f6b3a;--err:#a3271c}
@media (prefers-color-scheme:dark){:root{--bg:#14151c;--surface:#1d1e27;--text:#f1eadf;--muted:#b4aa9b;--line:#2c2e3a;--accent:#f2a65a;--on:#14151c;--ok:#7fc48a;--err:#f08a7e}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:16px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
main{max-width:960px;margin:0 auto;padding:24px 16px 48px}h1{font-size:1.5rem;margin:0 0 4px}h2{font-size:1.1rem;margin:0 0 10px}
.top{display:flex;flex-wrap:wrap;gap:12px;align-items:center;justify-content:space-between;margin-bottom:18px}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:14px}.card{background:var(--surface);border-radius:18px;padding:18px}
table{width:100%;border-collapse:collapse}td{padding:4px 0;border-bottom:1px solid var(--line);vertical-align:top}td:last-child{text-align:end;font-variant-numeric:tabular-nums}
.muted{color:var(--muted);font-size:.9rem}.ok{color:var(--ok)}.bad{color:var(--err)}a{color:var(--accent)}
nav a{margin-inline-end:12px}nav a[aria-current]{font-weight:700;text-decoration:none}
form.login{max-width:360px;background:var(--surface);border-radius:18px;padding:22px;margin-top:20px}
label{display:block;margin:10px 0 4px}input{width:100%;padding:10px;border-radius:10px;border:1px solid var(--line);background:var(--bg);color:var(--text);font:inherit}
button{margin-top:16px;padding:10px 18px;border:0;border-radius:12px;background:var(--accent);color:var(--on);font:inherit;font-weight:600;cursor:pointer}
button.link{background:none;color:var(--accent);padding:0;margin:0;text-decoration:underline}:focus-visible{outline:3px solid var(--accent);outline-offset:2px}`;

function page(lang, title, body) {
  return `<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex,nofollow"><title>${esc(title)}</title><link rel="stylesheet" href="/panel.css"></head><body><main>${body}</main></body></html>`;
}

function langOf(req, url) {
  const q = url.searchParams.get("dil");
  if (q === "tr" || q === "en") return q;
  return /^tr\b/i.test(req.headers["accept-language"] || "") ? "tr" : "en";
}

/* Umami paylaşım bağlantısıyla salt okunur istatistik; paylaşım kimliği sunucuda kalır, tarayıcıya gitmez */
async function umami(cfg, startAt, endAt) {
  if (!cfg.umamiShareId) return null;
  const get = async (path, token) => {
    const r = await fetch(cfg.umamiUrl + path, { headers: token ? { "x-umami-share-token": token } : {}, signal: AbortSignal.timeout(6000) });
    if (!r.ok) throw new Error(String(r.status));
    return r.json();
  };
  try {
    const share = await get(`/api/share/${encodeURIComponent(cfg.umamiShareId)}`);
    const id = share.websiteId, tok = share.token;
    const q = `startAt=${startAt}&endAt=${endAt}`;
    const num = (v) => (typeof v === "object" && v ? v.value : v) ?? 0;
    const stats = await get(`/api/websites/${id}/stats?${q}`, tok);
    const active = await get(`/api/websites/${id}/active`, tok).catch(() => null);
    const metric = async (type, alt) => get(`/api/websites/${id}/metrics?${q}&type=${type}&limit=8`, tok).catch(() => (alt ? get(`/api/websites/${id}/metrics?${q}&type=${alt}&limit=8`, tok) : []));
    return {
      visitors: num(stats.visitors), views: num(stats.pageviews),
      active: active ? (active.visitors ?? active.x ?? active[0]?.x ?? null) : null,
      pages: await metric("url", "path"), referrers: await metric("referrer"), countries: await metric("country"), devices: await metric("device"),
    };
  } catch {
    return false;
  }
}

export function panelRoutes({ cfg, pool, auth, mailer, metrics }) {
  const tries = new Map();
  const limited = (ip) => {
    const now = Date.now();
    if (tries.size > 5000) tries.clear();
    const list = (tries.get(ip) || []).filter((t) => now - t < 10 * 60_000);
    list.push(now);
    tries.set(ip, list);
    return list.length > 5;
  };
  const sameOrigin = (req) => req.headers.origin === cfg.publicUrl;
  const ipOf = (req) => String(req.headers["x-real-ip"] || req.socket.remoteAddress || "");
  const adminSession = async (req) => {
    const s = await auth.api.getSession({ headers: fromNodeHeaders(req.headers) }).catch(() => null);
    return s && s.user.role === "admin" && s.user.emailVerified ? s : null;
  };

  function loginPage(res, lang, msg = "") {
    const t = T[lang];
    res.writeHead(msg ? 400 : 200, { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" });
    res.end(page(lang, t.title, `<h1>${esc(t.title)}</h1>
<form class="login" method="post" action="/panel/giris?dil=${lang}"><h2>${esc(t.login)}</h2>${msg ? `<p class="bad" role="alert">${esc(msg)}</p>` : ""}
<label for="e">${esc(t.email)}</label><input id="e" name="email" type="email" autocomplete="username" required>
<label for="p">${esc(t.password)}</label><input id="p" name="password" type="password" autocomplete="current-password" required>
<button type="submit">${esc(t.signIn)}</button></form><p class="muted">${esc(t.note)}</p>`));
  }

  async function dashboard(req, res, url, lang) {
    const t = T[lang];
    const range = ["bugun", "7", "30"].includes(url.searchParams.get("aralik")) ? url.searchParams.get("aralik") : "7";
    const days = range === "bugun" ? 1 : Number(range);
    await metrics.flush();
    const today = metrics.dayOf();
    const m = (await pool.query(`select coalesce(sum(requests),0)::bigint r, coalesce(sum(errors),0)::bigint e, coalesce(sum(total_ms),0)::bigint ms
      from metric_day where day > ($1::date - $2::int)`, [today, days])).rows[0];
    const size = (await pool.query(`select pg_database_size(current_database()) s`)).rows[0].s;
    const since = new Date(Date.now() - days * 86400_000);
    const u = (await pool.query(`select count(*)::int total, count(*) filter (where "emailVerified")::int verified,
      count(*) filter (where "createdAt" >= $1)::int fresh from "user"`, [range === "bugun" ? new Date(`${today}T00:00:00+03:00`) : since])).rows[0];
    const db = await pool.query("select 1").then(() => true, () => false);
    const startAt = range === "bugun" ? Date.parse(`${today}T00:00:00+03:00`) : since.getTime();
    const um = await umami(cfg, startAt, Date.now());
    const r = Number(m.r), e = Number(m.e);
    const fmt = new Intl.NumberFormat(lang === "tr" ? "tr-TR" : "en-US");
    const mb = (b) => `${fmt.format(Math.round(Number(b) / 1024 / 102.4) / 10)} MB`;
    const row = (k, v) => `<tr><td>${esc(k)}</td><td>${v}</td></tr>`;
    const na = `<span class="muted">${esc(t.na)}</span>`;
    const list = (items) => (items && items.length ? `<table>${items.map((x) => row(x.x || "—", fmt.format(x.y))).join("")}</table>` : na);
    const svc = (okv) => (okv === null ? na : okv ? `<span class="ok">${esc(t.ok)}</span>` : `<span class="bad">${esc(t.bad2)}</span>`);
    const mailOk = cfg.mailDriver !== "resend" ? null : mailer.state.lastOk !== false;
    const nav = Object.entries(t.ranges).map(([k, v]) => `<a href="/panel?aralik=${k}&dil=${lang}"${k === range ? ' aria-current="page"' : ""}>${esc(v)}</a>`).join("");
    const other = lang === "tr" ? "en" : "tr";
    const body = `<div class="top"><div><h1>${esc(t.title)}</h1><nav>${nav}</nav></div>
<div><a href="/panel?aralik=${range}&dil=${other}">${other === "tr" ? "Türkçe" : "English"}</a>
<form method="post" action="/panel/cikis?dil=${lang}" style="display:inline"><button class="link" type="submit">${esc(t.signOut)}</button></form></div></div>
<div class="grid">
<section class="card"><h2>${esc(t.app)}</h2><table>${row(t.requests, fmt.format(r))}${row(t.avg, r ? `${fmt.format(Math.round(Number(m.ms) / r))} ms` : "—")}${row(t.errRate, r ? `${fmt.format(Math.round((e / r) * 1000) / 10)} %` : "—")}${row(t.dbSize, mb(size))}${row(t.storage, na)}</table></section>
<section class="card"><h2>${esc(t.members)}</h2><table>${row(t.total, fmt.format(u.total))}${row(t.verified, fmt.format(u.verified))}${row(`${t.newSignups} (${t.ranges[range]})`, fmt.format(u.fresh))}</table></section>
<section class="card"><h2>${esc(t.services)}</h2><table>${row(t.svcApp, svc(true))}${row(t.svcDb, svc(db))}${row(t.svcMail, svc(mailOk))}${row(t.svcUmami, svc(um === null ? null : um !== false))}
${row(t.deploy, `${esc(cfg.commit ? cfg.commit.slice(0, 7) : "—")} · ${esc(t.started)} ${esc(STARTED.toISOString().slice(0, 16).replace("T", " "))} UTC`)}</table>
<p class="muted">${esc(t.server)}: <a href="https://durum.bumba.tr" rel="noopener">durum.bumba.tr</a></p></section>
<section class="card"><h2>${esc(t.site)}</h2>${um ? `<table>${row(t.visitors, fmt.format(um.visitors))}${row(t.views, fmt.format(um.views))}${row(t.active, um.active === null ? na : fmt.format(um.active))}</table>` : na}</section>
<section class="card"><h2>${esc(t.topPages)}</h2>${um ? list(um.pages) : na}</section>
<section class="card"><h2>${esc(t.referrers)}</h2>${um ? list(um.referrers) : na}</section>
<section class="card"><h2>${esc(t.countries)}</h2>${um ? list(um.countries) : na}</section>
<section class="card"><h2>${esc(t.devices)}</h2>${um ? list(um.devices) : na}</section>
<section class="card"><h2>${esc(t.searches)}</h2><p>${na}</p><p class="muted">${esc(t.searchNote)}</p></section>
</div><p class="muted">${esc(t.note)}</p>`;
    res.writeHead(200, { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" });
    res.end(page(lang, t.title, body));
  }

  return async function handle(req, res, url) {
    const p = url.pathname;
    if (p === "/panel.css") {
      res.writeHead(200, { "Content-Type": "text/css; charset=utf-8", "Cache-Control": "public, max-age=3600" });
      res.end(CSS);
      return true;
    }
    if (!p.startsWith("/panel")) return false;
    const lang = langOf(req, url);
    if (p === "/panel" && req.method === "GET") {
      if (!(await adminSession(req))) {
        res.writeHead(303, { Location: `/panel/giris?dil=${lang}`, "Cache-Control": "no-store" });
        res.end();
        return true;
      }
      await dashboard(req, res, url, lang);
      return true;
    }
    if (p === "/panel/giris" && req.method === "GET") {
      loginPage(res, lang);
      return true;
    }
    if (p === "/panel/giris" && req.method === "POST") {
      if (!sameOrigin(req)) return loginPage(res, lang, T[lang].bad), true;
      if (limited(ipOf(req))) return loginPage(res, lang, T[lang].wait), true;
      const f = await readForm(req);
      let r;
      try {
        r = await auth.api.signInEmail({ body: { email: f.get("email") || "", password: f.get("password") || "" }, headers: fromNodeHeaders(req.headers), asResponse: true });
      } catch {
        return loginPage(res, lang, T[lang].bad), true;
      }
      const data = await r.clone().json().catch(() => null);
      if (!r.ok || !data?.user) return loginPage(res, lang, T[lang].bad), true;
      if (data.user.role !== "admin") {
        if (data.token) await pool.query(`delete from session where token = $1`, [data.token]);
        return loginPage(res, lang, T[lang].notAdmin), true;
      }
      res.writeHead(303, { Location: `/panel?dil=${lang}`, "Set-Cookie": r.headers.getSetCookie(), "Cache-Control": "no-store" });
      res.end();
      return true;
    }
    if (p === "/panel/cikis" && req.method === "POST") {
      let cookies = [];
      if (sameOrigin(req)) {
        const r = await auth.api.signOut({ headers: fromNodeHeaders(req.headers), asResponse: true }).catch(() => null);
        cookies = r ? r.headers.getSetCookie() : [];
      }
      res.writeHead(303, { Location: `/panel/giris?dil=${lang}`, "Set-Cookie": cookies, "Cache-Control": "no-store" });
      res.end();
      return true;
    }
    return false;
  };
}
