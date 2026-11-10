/* Saat Tarih hesap sunucusu: uygulamaların üyeliği (e-posta + şifre), sitedeki şifre sıfırlama formunun alıcısı ve
   yönetim paneli (standart madde 7 ve 10). Site statik kalır; bu sunucu yalnız hesap.time.bumba.tr'de çalışır. */
import http from "node:http";
import pg from "pg";
import { toNodeHandler, fromNodeHeaders } from "better-auth/node";
import { loadConfig, siteLink, normLang } from "./config.mjs";
import { createMailer } from "./mail.mjs";
import { betterAuth } from "better-auth";
import { authOptions } from "./auth.mjs";
import { migrate } from "./migrate.mjs";
import { createMetrics } from "./metrics.mjs";
import { panelRoutes } from "./panel.mjs";
import { scheduleCleanup } from "./cleanup.mjs";
import { readForm } from "./http.mjs";

const SEC_HEADERS = {
  "Strict-Transport-Security": "max-age=31536000",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer",
  "Content-Security-Policy": "default-src 'none'; style-src 'self'; img-src 'self' data:; form-action 'self'; base-uri 'none'; frame-ancestors 'none'",
};

function send(res, status, body, type = "text/plain; charset=utf-8", extra = {}) {
  res.writeHead(status, { "Content-Type": type, "Cache-Control": "no-store", ...extra });
  res.end(body);
}
const json = (res, status, obj) => send(res, status, JSON.stringify(obj), "application/json; charset=utf-8");
const redirect = (res, to, extra = {}) => { res.writeHead(303, { Location: to, "Cache-Control": "no-store", ...extra }); res.end(); };

export async function createApp(cfg, { pool, mailer } = {}) {
  pool ||= new pg.Pool({ connectionString: cfg.databaseUrl, max: 10 });
  mailer ||= createMailer(cfg);
  const options = authOptions(cfg, pool, mailer);
  await migrate(options, pool);
  const auth = betterAuth(options);
  // Yönetici: sabit bilgilerdeki adres, doğrulanmışsa (hesap önce uygulamadan açılır)
  await pool.query(`update "user" set role = 'admin' where lower(email) = $1 and "emailVerified" = true and role <> 'admin'`, [cfg.adminEmail]);
  const metrics = createMetrics(pool);
  const authHandler = toNodeHandler(auth);
  const panel = panelRoutes({ cfg, pool, auth, mailer, metrics });
  const stopCleanup = cfg.cleanup === false ? () => {} : scheduleCleanup({ pool, mailer });

  const session = (req) => auth.api.getSession({ headers: fromNodeHeaders(req.headers) }).catch(() => null);

  async function route(req, res) {
    const url = new URL(req.url, cfg.publicUrl);
    const p = url.pathname;
    if (p.startsWith("/api/auth/")) return authHandler(req, res);

    if (p === "/saglik" && req.method === "GET") {
      const db = await pool.query("select 1").then(() => true, () => false);
      return json(res, db ? 200 : 503, { durum: db ? "ok" : "sorunlu", veritabani: db });
    }

    // Kişinin bilgileri (KVKK md. 11 erişim hakkı): uygulamadaki "Verilerimi indir"
    if (p === "/v1/verilerim" && req.method === "GET") {
      const s = await session(req);
      if (!s) return json(res, 401, { hata: "oturum" });
      const u = await pool.query(`select id, email, name, lang, "emailVerified", "createdAt", "updatedAt" from "user" where id = $1`, [s.user.id]);
      const ss = await pool.query(`select "createdAt", "updatedAt", "expiresAt", "ipAddress", "userAgent" from session where "userId" = $1 order by "createdAt"`, [s.user.id]);
      res.setHeader("Content-Disposition", 'attachment; filename="saat-tarih-hesap.json"');
      return json(res, 200, { hesap: u.rows[0], oturumlar: ss.rows, olusturma: new Date().toISOString() });
    }

    // Sitedeki şifre sıfırlama formu buraya gönderilir (betiksiz çalışır); sonuç siteye dönüş adresinde
    if (p === "/sifre" && req.method === "POST") {
      const f = await readForm(req);
      const lang = normLang(f.get("dil"));
      const back = siteLink(cfg, lang, "password");
      const pw = f.get("sifre") || "";
      if (pw !== (f.get("sifre2") || "")) return redirect(res, `${back}?durum=hata&neden=eslesme`);
      if (pw.length < 8 || pw.length > 128) return redirect(res, `${back}?durum=hata&neden=uzunluk`);
      try {
        await auth.api.resetPassword({ body: { newPassword: pw, token: f.get("token") || "" }, headers: fromNodeHeaders(req.headers) });
        return redirect(res, `${back}?durum=tamam`);
      } catch {
        return redirect(res, `${back}?durum=hata&neden=baglanti`);
      }
    }

    if (p === "/robots.txt") return send(res, 200, "User-agent: *\nDisallow: /\n");
    if (p === "/" && req.method === "GET") return redirect(res, cfg.siteUrl);
    if (await panel(req, res, url)) return;
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
      console.error("[hata]", e?.message || e);
      if (!res.headersSent) send(res, e?.status || 500, "Hata / Error");
      else res.end();
    }
  });

  async function close() {
    stopCleanup();
    await new Promise((r) => server.close(r));
    await metrics.flush();
    metrics.stop();
    await pool.end();
  }
  return { server, auth, pool, mailer, metrics, close };
}

/* Doğrudan çalıştırma */
if (import.meta.url === `file://${process.argv[1]}`) {
  const cfg = loadConfig();
  const app = await createApp(cfg);
  app.server.listen(cfg.port, () => console.log(`Saat Tarih hesap sunucusu :${cfg.port}`));
  const stop = () => app.close().finally(() => process.exit(0));
  process.on("SIGTERM", stop);
  process.on("SIGINT", stop);
}
