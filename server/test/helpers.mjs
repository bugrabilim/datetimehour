/* Test yardımcıları: her dosya yeni bir PostgreSQL veritabanında, gerçek HTTP sunucusuyla çalışır; e-postalar bellekte.
   Veritabanı: TEST_DATABASE_URL (varsayılan yerel postgres://postgres@127.0.0.1:54329/postgres). */
import net from "node:net";
import http from "node:http";
import crypto from "node:crypto";
import pg from "pg";
import { createApp } from "../src/index.mjs";
import { loadConfig } from "../src/config.mjs";

const BASE = process.env.TEST_DATABASE_URL || "postgres://postgres@127.0.0.1:54329/postgres";

const freePort = () => new Promise((resolve, reject) => {
  const s = net.createServer().listen(0, "127.0.0.1", () => { const p = s.address().port; s.close(() => resolve(p)); });
  s.on("error", reject);
});

export async function start(env = {}) {
  const name = "saat_test_" + crypto.randomBytes(6).toString("hex");
  const admin = new pg.Client({ connectionString: BASE });
  await admin.connect();
  await admin.query(`create database ${name}`);
  await admin.end();
  const dbUrl = new URL(BASE);
  dbUrl.pathname = "/" + name;
  const port = await freePort();
  const publicUrl = `http://127.0.0.1:${port}`;
  const cfg = loadConfig({ DATABASE_URL: dbUrl.toString(), BETTER_AUTH_SECRET: "test-gizli-deger-".padEnd(48, "x"), MAIL_DRIVER: "memory", PUBLIC_URL: publicUrl, CRON_SECRET: "test-gorev-sirri-".padEnd(40, "y"), ...env });
  const app = await createApp(cfg);
  await new Promise((r) => app.server.listen(port, "127.0.0.1", r));
  let ip = 0;
  const t = {
    cfg, app, url: publicUrl, outbox: app.mailer.outbox,
    // Her çağrı ayrı istemci adresinden gelir (hız sınırı testleri kendi adresini verir)
    async req(path, { method = "GET", body, token, headers = {}, form, cookie } = {}) {
      const h = { "x-real-ip": headers["x-real-ip"] || `10.0.${Math.floor(++ip / 250)}.${ip % 250}`, ...headers };
      if (token) h.authorization = `Bearer ${token}`;
      if (cookie) h.cookie = cookie;
      let b;
      if (form) { h["content-type"] = "application/x-www-form-urlencoded"; b = new URLSearchParams(form).toString(); }
      else if (body !== undefined) { h["content-type"] = "application/json"; b = JSON.stringify(body); }
      // node:http: uygulamalar gibi (URLSession, OkHttp) tarayıcı başlığı (Sec-Fetch-*) göndermez
      const res = await new Promise((resolve, reject) => {
        const rq = http.request(publicUrl + path, { method, headers: b === undefined ? h : { ...h, "content-length": Buffer.byteLength(b) } }, (rs) => {
          const chunks = [];
          rs.on("data", (c) => chunks.push(c));
          rs.on("end", () => resolve({ status: rs.statusCode, headers: new Headers(Object.entries(rs.headers).flatMap(([k, v]) => (Array.isArray(v) ? v.map((x) => [k, x]) : [[k, v]]))), body: Buffer.concat(chunks).toString("utf8") }));
        });
        rq.on("error", reject);
        if (b !== undefined) rq.write(b);
        rq.end();
      });
      const text = res.body;
      let json = null;
      try { json = JSON.parse(text); } catch {}
      return { status: res.status, headers: res.headers, text, json };
    },
    last(kind, to) {
      const list = app.mailer.outbox.filter((m) => m.kind === kind && (!to || m.to === to));
      return list[list.length - 1];
    },
    async stop() {
      await app.close();
      const a = new pg.Client({ connectionString: BASE });
      await a.connect();
      await a.query(`drop database if exists ${name} with (force)`);
      await a.end();
    },
  };
  return t;
}

/* Kayıt + doğrulama + giriş; taşıyıcı belirteci döner */
export async function signUpVerified(t, email, password = "guclu-sifre-1", lang = "tr") {
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email, password, name: "Deneme", lang } });
  if (r.status !== 200) throw new Error("kayıt " + r.status + " " + r.text);
  const v = t.last("verify", email);
  const u = new URL(v.vars.url);
  const res = await t.req(u.pathname + u.search);
  if (res.status !== 302) throw new Error("doğrulama " + res.status + " " + res.text);
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email, password } });
  if (s.status !== 200) throw new Error("giriş " + s.status + " " + s.text);
  return s.json.token;
}

export const cookieOf = (res) => res.headers.getSetCookie().map((c) => c.split(";")[0]).join("; ");
