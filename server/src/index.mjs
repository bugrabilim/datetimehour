/* Saat Tarih üyelik sunucusu (hesap.time.bumba.tr; standart madde 7). Site (time.bumba.tr) statik kalır; giriş, kayıt
   ve hesap sayfası, uygulamaların API'si, ortak üyelik servisinin ürün uçları ve zamanlanmış görev uçları buradadır.
   Zorunlu ayarlardan biri eksikse üyelik kapalı açılır: sayfalar "üyelik şu an kapalı" der, uçlar 503 döner. */
import http from "node:http";
import crypto from "node:crypto";
import pg from "pg";
import { betterAuth } from "better-auth";
import { toNodeHandler, fromNodeHeaders } from "better-auth/node";
import { loadConfig, normLang } from "./config.mjs";
import { createMailer, readUnsubscribeToken } from "./mail.mjs";
import { authOptions } from "./auth.mjs";
import { migrate } from "./migrate.mjs";
import { createMetrics } from "./metrics.mjs";
import { runCleanup } from "./cleanup.mjs";
import { runCelebrations } from "./celebrate.mjs";
import { createCenter } from "./center.mjs";
import { createAccounts, profileOf } from "./accounts.mjs";
import { createRecords } from "./records.mjs";
import { linkedRoutes } from "./linked.mjs";
import { validateProfile, checkPhoto, passwordChecks } from "./profile.mjs";
import { createLimiter, ipOf } from "./limit.mjs";
import { readJson, send, json } from "./http.mjs";
import { webRoutes } from "./web.mjs";

const SEC_HEADERS = {
  "Strict-Transport-Security": "max-age=31536000",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer",
  "Permissions-Policy": "camera=(), microphone=(), geolocation=()",
};

const bearerOk = (header, secret) => {
  if (!secret) return false;
  const got = Buffer.from(String(header || ""));
  const want = Buffer.from(`Bearer ${secret}`);
  return got.length === want.length && crypto.timingSafeEqual(got, want);
};

export async function createApp(cfg, { pool, mailer, center, fetchImpl } = {}) {
  if (!cfg.open) {
    console.error("[ayar] üyelik kapalı; eksik: " + cfg.missing.join(", "));
    const web = webRoutes({ cfg, closed: true });
    const server = http.createServer(async (req, res) => {
      for (const [k, v] of Object.entries(SEC_HEADERS)) res.setHeader(k, v);
      const url = new URL(req.url, "http://x");
      if (url.pathname === "/saglik") return json(res, 200, { durum: "kapali" });
      if (url.pathname.startsWith("/api/") || url.pathname.startsWith("/v1/") || url.pathname.startsWith("/gorev/")) return json(res, 503, { hata: "kapali" });
      if (await web(req, res, url, null)) return;
      return send(res, 404, "Bulunamadı / Not found");
    });
    return { server, closed: true, close: () => new Promise((r) => server.close(r)) };
  }

  pool ||= new pg.Pool({ connectionString: cfg.databaseUrl, max: 10 });
  mailer ||= createMailer(cfg);
  center ||= createCenter(cfg, fetchImpl);
  const options = authOptions(cfg, pool, mailer, center);
  await migrate(options, pool);
  const auth = betterAuth(options);
  // Yönetici: sabit bilgilerdeki adres, doğrulanmışsa (madde 10: yönetici rolü üyelikten)
  await pool.query(`update "user" set role = 'admin' where lower(email) = $1 and "emailVerified" = true and coalesce(role, '') <> 'admin'`, [cfg.adminEmail]);
  const metrics = createMetrics(pool);
  const authHandler = toNodeHandler(auth);
  const accounts = createAccounts({ pool, auth });
  const records = createRecords(pool);
  const limit = createLimiter(pool, cfg.rateLimit);
  const linked = linkedRoutes({ cfg, auth, accounts, mailer, json, readJson });
  const web = webRoutes({ cfg, closed: false, accounts });
  const ctxP = auth.$context;

  const session = (req) => auth.api.getSession({ headers: fromNodeHeaders(req.headers) }).catch(() => null);
  const queue = (op, body) => pool.query(`insert into center_retry (op, body) values ($1, $2)`, [op, JSON.stringify(body)]);
  /* Ortak servise iletim; ulaşılamazsa temizlik görevinde yeniden denenir */
  async function tell(op, body) {
    const r = await center.call(op, body);
    if (!r.ok && (r.status === 0 || r.status >= 500 || r.status === 429)) await queue(op, body);
    return r;
  }
  async function verifyPassword(userId, password) {
    const hash = await accounts.passwordHash(userId);
    if (!hash) return false;
    return (await ctxP).password.verify({ hash, password: String(password || "") }).catch(() => false);
  }
  /* Tarayıcıdan çerezle gelen değiştirici isteklerde başka siteden gönderim engellenir (uygulamalar Origin göndermez) */
  const sameOrigin = (req) => {
    const o = req.headers.origin;
    return !o || o === cfg.publicUrl;
  };
  async function profileView(user) {
    const r = await pool.query(`select photo from "user" where id = $1`, [user.id]);
    return { ...profileOf(user), foto: r.rows[0]?.photo || null };
  }
  async function accountView(user) {
    return {
      eposta: user.email,
      uyeNo: user.uyeNo != null ? Number(user.uyeNo) : null,
      uyelikTarihi: new Date(user.createdAt).toISOString(),
      dil: user.lang, tema: user.theme || "system", saatDilimi: user.timeZone || null,
      kutlama: user.celebrate !== false,
      profil: await profileView(user),
    };
  }

  async function api(req, res, p, url) {
    const m = req.method;
    // Kayıt sayfası: yazılan e-posta başka bir Bumba ürününde kayıtlı mı (bağlı üyelik sorusu; kayıt deneme sınırı geçerli)
    if (p === "/v1/uyelik/sorgula" && m === "POST") {
      if (!(await limit(`sorgula:${ipOf(req)}`, 300, 3))) return json(res, 429, { hata: "sinir" });
      const b = await readJson(req);
      const email = String(b.eposta || "").trim().toLowerCase();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return json(res, 400, { hata: "eposta" });
      if (await accounts.userByEmail(email)) return json(res, 200, { durum: "yok" });
      const q = await center.sorgula(email);
      return json(res, 200, { durum: q.ok && q.data?.durum === "var" ? "var" : "yok" });
    }
    // "Evet"ten sonra giriş: ortak servis şifreyi doğrular, hesap burada açılır, oturum Better Auth'un girişiyle
    if (p === "/v1/uyelik/bagla" && m === "POST") {
      const b = await readJson(req);
      const email = String(b.eposta || "").trim().toLowerCase();
      if (!(await limit(`bagla:${email}`, 900, 5)) || !(await limit(`bagla-ip:${ipOf(req)}`, 60, 5))) return json(res, 429, { hata: "sinir" });
      if (!sameOrigin(req)) return json(res, 403, { hata: "kaynak" });
      const r = await center.bagla(email, String(b.sifre || ""));
      if (r.status === 429) return json(res, 429, { hata: "sinir" });
      if (!r.ok || !r.data?.gecerli || !r.data.hesap) return json(res, 401, { hata: "bilgiler" });
      const h = r.data.hesap;
      if (!(await accounts.userByEmail(h.eposta))) await accounts.createFromHesap({ ...h, anaUrun: false }, { lang: normLang(b.dil) });
      const out = await auth.api.signInEmail({ body: { email: h.eposta, password: String(b.sifre || "") }, headers: fromNodeHeaders(req.headers), asResponse: true });
      const headers = {};
      out.headers.forEach((v, k) => { if (k !== "content-length") headers[k] = v; });
      const setCookie = out.headers.getSetCookie?.() || [];
      if (setCookie.length) headers["set-cookie"] = setCookie;
      res.writeHead(out.status, { ...headers, "Cache-Control": "no-store" });
      return res.end(Buffer.from(await out.arrayBuffer()));
    }

    const s = await session(req);
    if (!s) return json(res, 401, { hata: "oturum" });
    const user = (await pool.query(`select * from "user" where id = $1`, [s.user.id])).rows[0];
    if (!user) return json(res, 401, { hata: "oturum" });
    if (m !== "GET" && !sameOrigin(req)) return json(res, 403, { hata: "kaynak" });

    if (p === "/v1/hesap" && m === "GET") return json(res, 200, await accountView(user));
    if (p === "/v1/profil" && m === "POST") {
      const b = await readJson(req);
      const clean = validateProfile(b.profil || {});
      await accounts.writeProfile(user.id, clean);
      if (user.uyeNo != null) await tell("guncelle", { uyeNo: Number(user.uyeNo), degisiklik: { profil: { ...clean, foto: (await profileView(user)).foto } } });
      return json(res, 200, { profil: clean });
    }
    if (p === "/v1/profil/foto" && m === "POST") {
      const b = await readJson(req, 400 * 1024);
      const foto = checkPhoto(b.foto);
      await (await accounts.ia()).updateUser(user.id, { photo: foto, updatedAt: new Date() });
      if (user.uyeNo != null) await tell("guncelle", { uyeNo: Number(user.uyeNo), degisiklik: { profil: { ...profileOf(user), foto } } });
      return json(res, 200, { foto });
    }
    if (p === "/v1/kutlama" && m === "POST") {
      const b = await readJson(req);
      const on = b.acik === true;
      await pool.query(`update "user" set celebrate = $1 where id = $2`, [on, user.id]);
      if (user.uyeNo != null) await tell("guncelle", { uyeNo: Number(user.uyeNo), degisiklik: { kutlama: on } });
      return json(res, 200, { kutlama: on });
    }
    if (p === "/v1/sifre-kurali" && m === "POST") {
      const b = await readJson(req);
      return json(res, 200, passwordChecks(b.sifre));
    }
    // Girişten sonra: aynı e-postayla başka bir Bumba ürününde ayrı hesap var mı (ayrı hesap sorusu)
    if (p === "/v1/uyelik/durum" && m === "GET") {
      if (user.uyeNo == null) return json(res, 200, { ayriHesap: false });
      const q = await center.sorgula(user.email, Number(user.uyeNo));
      return json(res, 200, { ayriHesap: !!(q.ok && q.data?.ayriHesap), urunler: q.data?.urunler || [] });
    }
    if (p === "/v1/uyelik/birlestir" && m === "POST") {
      const b = await readJson(req);
      if (!(await limit(`birlestir:${user.id}`, 900, 5))) return json(res, 429, { hata: "sinir" });
      const r = await center.birlestir(Number(user.uyeNo), user.email, String(b.sifre || ""));
      if (!r.ok || !r.data?.gecerli) return json(res, r.status === 429 ? 429 : 401, { hata: "bilgiler" });
      const gelen = r.data.hesap || {};
      const mine = await profileView(user);
      const profil = { ...mine };
      for (const [k, v] of Object.entries(gelen.profil || {})) if (v != null && !(Array.isArray(v) && !v.length)) profil[k] = v;
      await (await accounts.ia()).updateUser(user.id, { uyeNo: Number(r.data.yeniNo), updatedAt: new Date() });
      await accounts.applyChange({ ...user, uyeNo: r.data.yeniNo }, { sifreOzeti: gelen.sifreOzeti || undefined, profil });
      await tell("guncelle", { uyeNo: Number(r.data.yeniNo), degisiklik: { profil } });
      return json(res, 200, { uyeNo: Number(r.data.yeniNo) });
    }
    if (p === "/v1/uyelik/urunler" && m === "GET") {
      const r = await center.urunler(Number(user.uyeNo));
      if (!r.ok) return json(res, 503, { hata: "merkez" });
      return json(res, 200, { urunler: Array.isArray(r.data) ? r.data : r.data?.urunler || [] });
    }
    if (p === "/v1/uyelik/ac" && m === "POST") {
      const b = await readJson(req);
      if (!(await verifyPassword(user.id, b.sifre))) return json(res, 401, { hata: "sifre" });
      const hesap = await accounts.toHesap(user);
      const r = await center.ac(Number(user.uyeNo), Array.isArray(b.hedefler) ? b.hedefler : [], hesap);
      if (!r.ok) return json(res, 503, { hata: "merkez" });
      return json(res, 200, r.data);
    }
    if (p === "/v1/uyelik/kapat" && m === "POST") {
      const b = await readJson(req);
      const r = await center.kapat(Number(user.uyeNo), Array.isArray(b.hedefler) ? b.hedefler : []);
      if (!r.ok) return json(res, 503, { hata: "merkez" });
      return json(res, 200, r.data);
    }
    if (p === "/v1/kayitlar" && m === "GET") return json(res, 200, { kayitlar: await records.list(user.id, url.searchParams.get("since")), simdi: Date.now() });
    if (p === "/v1/kayitlar" && m === "POST") {
      const b = await readJson(req, 6 * 1024 * 1024);
      await records.put(user.id, b.degisiklik);
      return json(res, 200, { tamam: true, simdi: Date.now() });
    }
    if (p === "/v1/rozetler" && m === "GET") return json(res, 200, { rozetler: await records.badges(user.id) }, { "Cache-Control": "private, no-store" });
    if (p === "/v1/rozetler" && m === "POST") {
      const b = await readJson(req);
      return json(res, 200, { rozetler: await records.addBadges(user.id, b.rozetler) });
    }
    if (p === "/v1/rozetler/sil" && m === "POST") {
      await records.clearBadges(user.id);
      return json(res, 200, { rozetler: [] });
    }
    // Verilerimi indir (KVKK md. 11): hesap, profil (fotoğraf dahil), onay kaydı, oturumlar (IP yok), kayıtlar, rozetler
    if (p === "/v1/verilerim" && m === "GET") {
      const ss = await pool.query(`select "createdAt", "updatedAt", "expiresAt", "userAgent", client from session where "userId" = $1 order by "createdAt"`, [user.id]);
      const data = {
        hesap: await accountView(user),
        onay: { zaman: user.consentAt ? new Date(user.consentAt).toISOString() : null, surum: user.consentVersion },
        oturumlar: ss.rows,
        kayitlar: await records.list(user.id),
        rozetler: await records.badges(user.id),
        olusturma: new Date().toISOString(),
      };
      return json(res, 200, data, { "Content-Disposition": 'attachment; filename="saat-tarih-hesap.json"' });
    }
    // Hesabı sil: şifre ya da son 24 saatte açılmış oturum; "urun" yalnız Saat Tarih'ten, "hepsi" bütün Bumba ürünlerinden
    if (p === "/v1/hesap/sil" && m === "POST") {
      const b = await readJson(req);
      const fresh = Date.now() - new Date(s.session.createdAt).getTime() < 24 * 3600 * 1000;
      if (!fresh && !(await verifyPassword(user.id, b.sifre))) return json(res, 401, { hata: "sifre" });
      if (b.sifre && !(await verifyPassword(user.id, b.sifre))) return json(res, 401, { hata: "sifre" });
      const all = b.kapsam === "hepsi";
      if (user.uyeNo != null) await tell(all ? "sil" : "ayril", { uyeNo: Number(user.uyeNo) });
      await accounts.deleteLocal(user.id);
      await mailer.send(all ? "deletedAll" : "deleted", user.email, user.lang, {}).catch(() => {});
      return json(res, 200, { silindi: true });
    }
    return json(res, 404, { hata: "yok" });
  }

  async function route(req, res) {
    const url = new URL(req.url, cfg.publicUrl);
    const p = url.pathname;
    if (p.startsWith("/api/auth/")) return authHandler(req, res);
    if (p.startsWith("/api/bumba-uyelik/")) return linked(req, res, p.slice("/api/bumba-uyelik/".length));
    if (p.startsWith("/v1/")) return api(req, res, p, url);

    if (p === "/saglik" && req.method === "GET") {
      const db = await pool.query("select 1").then(() => true, () => false);
      return json(res, db ? 200 : 503, { durum: db ? "ok" : "sorunlu", veritabani: db, surum: cfg.commit || null });
    }
    // Zamanlanmış görevler (CRON_SECRET'la; görevleri ana altyapı oturumu kurar)
    if (p === "/gorev/temizlik" && req.method === "POST") {
      if (!bearerOk(req.headers.authorization, cfg.cronSecret)) return json(res, 401, { hata: "yetki" });
      return json(res, 200, await runCleanup({ pool, center }));
    }
    if (p === "/gorev/kutlama" && req.method === "POST") {
      if (!bearerOk(req.headers.authorization, cfg.cronSecret)) return json(res, 401, { hata: "yetki" });
      return json(res, 200, await runCelebrations({ pool, mailer }));
    }
    // Kutlama e-postalarını girişsiz kapatma (e-postadaki bağlantı; tek tıkla kapatma için POST da)
    if (p === "/kutlama" && req.method === "POST") {
      const userId = readUnsubscribeToken(cfg.secret, url.searchParams.get("k"));
      if (userId) {
        const u = (await pool.query(`update "user" set celebrate = false where id = $1 returning "uyeNo"`, [userId])).rows[0];
        if (u?.uyeNo != null) await tell("guncelle", { uyeNo: Number(u.uyeNo), degisiklik: { kutlama: false } });
      }
    }
    if (p === "/robots.txt") return send(res, 200, "User-agent: *\nDisallow: /\n");
    if (await web(req, res, url, { session, pool })) return;
    return send(res, 404, "Bulunamadı / Not found");
  }

  const server = http.createServer(async (req, res) => {
    const t0 = performance.now();
    for (const [k, v] of Object.entries(SEC_HEADERS)) res.setHeader(k, v);
    res.on("finish", () => {
      if (req.url !== "/saglik") metrics.record(res.statusCode, performance.now() - t0);
    });
    try {
      await route(req, res);
    } catch (e) {
      if (e?.status && e.status < 500) {
        if (!res.headersSent) json(res, e.status, { hata: e.field ? `gecersiz:${e.field}` : e.message });
        else res.end();
        return;
      }
      console.error("[hata]", e?.message || e);
      if (!res.headersSent) send(res, 500, "Hata / Error");
      else res.end();
    }
  });

  async function close() {
    await new Promise((r) => server.close(r));
    await metrics.flush();
    metrics.stop();
    await pool.end();
  }
  return { server, auth, pool, mailer, center, metrics, accounts, records, close };
}

/* Doğrudan çalıştırma */
if (import.meta.url === `file://${process.argv[1]}`) {
  const cfg = loadConfig();
  const app = await createApp(cfg);
  app.server.listen(cfg.port, () => console.log(`Saat Tarih üyelik sunucusu :${cfg.port}${app.closed ? " (üyelik kapalı)" : ""}`));
  const stop = () => app.close().finally(() => process.exit(0));
  process.on("SIGTERM", stop);
  process.on("SIGINT", stop);
}
