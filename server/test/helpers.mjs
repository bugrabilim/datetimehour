/* Test yardımcıları: her dosya yeni bir PostgreSQL veritabanında, gerçek HTTP sunucusuyla ve sahte bir ortak üyelik
   servisiyle (merkez) çalışır; e-postalar bellekte. Veritabanı: TEST_DATABASE_URL
   (varsayılan yerel postgres://postgres@127.0.0.1:54329/postgres). */
import net from "node:net";
import http from "node:http";
import crypto from "node:crypto";
import pg from "pg";
import { createApp } from "../src/index.mjs";
import { loadConfig } from "../src/config.mjs";

const BASE = process.env.TEST_DATABASE_URL || "postgres://postgres@127.0.0.1:54329/postgres";
export const CENTER_KEY = "test-uyelik-anahtari-".padEnd(48, "z");
export const CRON = "test-gorev-sirri-".padEnd(40, "y");

const freePort = () => new Promise((resolve, reject) => {
  const s = net.createServer().listen(0, "127.0.0.1", () => { const p = s.address().port; s.close(() => resolve(p)); });
  s.on("error", reject);
});

/* Sahte merkez: başka üründe kayıtlı adresler (`others`), yasaklı adresler, gelen çağrıların kaydı */
export async function startCenter() {
  const c = {
    calls: [], next: 10458, numbers: new Map(), others: new Map(), banned: new Set(), down: false, ayriHesap: new Set(), merge: new Map(), pending: new Map(),
    find: (op) => c.calls.filter((x) => x.op === op),
  };
  const server = http.createServer(async (req, res) => {
    const chunks = [];
    for await (const ch of req) chunks.push(ch);
    const body = chunks.length ? JSON.parse(Buffer.concat(chunks).toString("utf8")) : {};
    const op = req.url.split("/").pop();
    c.calls.push({ op, body, auth: req.headers.authorization, site: req.headers["x-bumba-site"] });
    const out = (s, o) => { res.writeHead(s, { "Content-Type": "application/json" }); res.end(JSON.stringify(o)); };
    if (c.down) return out(503, { hata: "kapali" });
    if (req.headers.authorization !== `Bearer ${CENTER_KEY}` || req.headers["x-bumba-site"] !== "saat-tarih") return out(401, {});
    const e = String(body.eposta || "").toLowerCase();
    if (op === "sorgula") {
      if (c.banned.has(e)) return out(200, { durum: "yasakli" });
      if (c.others.has(e)) return out(200, { durum: "var", uyeNo: c.others.get(e).hesap.uyeNo, urunler: ["gander"], buradaAcik: false });
      if (c.numbers.has(e)) {
        const no = Number(body.uyeNo) || c.numbers.get(e);
        const extra = c.pending.has(no) ? { sifreSecimi: c.pending.get(no) } : {};
        return out(200, { durum: "var", uyeNo: c.numbers.get(e), urunler: ["saat-tarih"], buradaAcik: true, ayriHesap: c.ayriHesap.has(e), ...extra });
      }
      return out(200, { durum: "yok" });
    }
    if (op === "kayit") {
      if (c.banned.has(e)) return out(403, {});
      if (c.others.has(e)) return out(409, { durum: "var" });
      if (!c.numbers.has(e)) c.numbers.set(e, c.next++);
      return out(200, { no: c.numbers.get(e) });
    }
    if (op === "bagla") {
      const o = c.others.get(e);
      if (!o || o.sifre !== body.sifre) return out(200, { gecerli: false });
      return out(200, { gecerli: true, hesap: o.hesap });
    }
    // Birleşme şifresiz (5.16): en eski numara, birleşmiş profil, şifre özeti null; şifre seçimi bekler
    if (op === "birlestir") {
      const h = c.merge.get(e);
      if (!h || !c.ayriHesap.has(e)) return out(200, { birlesti: false });
      c.ayriHesap.delete(e);
      c.numbers.set(e, h.uyeNo);
      const secim = [{ site: "saat-tarih", ad: "Saat Tarih" }, { site: "gander", ad: "Gander" }];
      c.pending.set(h.uyeNo, secim);
      return out(200, { birlesti: true, yeniNo: h.uyeNo, hesap: { ...h, sifreOzeti: null }, urunler: ["saat-tarih", "gander"], sifreSecimi: secim });
    }
    if (op === "sifre-sec") {
      const no = Number(body.uyeNo);
      if (!c.pending.has(no) || !c.pending.get(no).some((x) => x.site === body.site)) return out(404, { hata: "secim-yok" });
      c.pending.delete(no);
      return out(200, { tamam: true });
    }
    if (op === "urunler") return out(200, { urunler: [{ site: "saat-tarih", ad: "Saat Tarih", adres: "https://time.bumba.tr", acik: true, kendisi: true }, { site: "gander", ad: "Gander", adres: "https://gander.bumba.tr", acik: false, kendisi: false }] });
    if (op === "ac") return out(200, { sonuclar: Object.fromEntries((body.hedefler || []).map((h) => [h, "acildi"])) });
    if (op === "kapat") return out(200, { sonuclar: Object.fromEntries((body.hedefler || []).map((h) => [h, "kapandi"])) });
    return out(200, { tamam: true });
  });
  const port = await freePort();
  await new Promise((r) => server.listen(port, "127.0.0.1", r));
  c.url = `http://127.0.0.1:${port}`;
  c.stop = () => new Promise((r) => server.close(r));
  return c;
}

/* Deneme fotoğrafı: verilen biçimde gerçek görüntü (data URL); EXIF'e iz yazılabilir */
export async function testPhoto(fmt = "jpeg", w = 900, h = 600, exif = false) {
  const { default: sharp } = await import("sharp");
  let img = sharp({ create: { width: w, height: h, channels: 3, background: "#c84" } });
  if (exif) img = img.withExif({ IFD0: { Make: "Deneme", Software: "gizli-iz" } });
  const buf = await img[fmt]().toBuffer();
  return `data:image/${fmt};base64,${buf.toString("base64")}`;
}

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
  const center = await startCenter();
  const cfg = loadConfig({
    DATABASE_URL: dbUrl.toString(), BETTER_AUTH_SECRET: "test-gizli-deger-".padEnd(48, "x"), BETTER_AUTH_URL: publicUrl,
    MAIL_DRIVER: "memory", CRON_SECRET: CRON, UYELIK_ANAHTARI: CENTER_KEY, UYELIK_MERKEZ: center.url, ...env,
  });
  const app = await createApp(cfg);
  await new Promise((r) => app.server.listen(port, "127.0.0.1", r));
  let ip = 0;
  const t = {
    cfg, app, center, url: publicUrl, outbox: app.mailer?.outbox,
    // Her çağrı ayrı istemci adresinden gelir (deneme sınırı testleri kendi adresini verir)
    async req(path, { method = "GET", body, token, headers = {}, form, cookie } = {}) {
      const h = { "x-real-ip": headers["x-real-ip"] || `10.0.${Math.floor(++ip / 250)}.${ip % 250}`, ...headers };
      if (token) h.authorization = `Bearer ${token}`;
      if (cookie) h.cookie = cookie;
      let b;
      if (form) { h["content-type"] = "application/x-www-form-urlencoded"; b = new URLSearchParams(form).toString(); }
      else if (body !== undefined) { h["content-type"] = "application/json"; b = JSON.stringify(body); }
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
      let json = null;
      try { json = JSON.parse(res.body); } catch {}
      return { status: res.status, headers: res.headers, text: res.body, json };
    },
    last(kind, to) {
      const list = app.mailer.outbox.filter((m) => m.kind === kind && (!to || m.to === to));
      return list[list.length - 1];
    },
    q: (sql, args) => app.pool.query(sql, args),
    async stop() {
      await app.close();
      await center.stop();
      const a = new pg.Client({ connectionString: BASE });
      await a.connect();
      await a.query(`drop database if exists ${name} with (force)`);
      await a.end();
    },
  };
  return t;
}

export const PW = "Guclu-sifre1";

/* Kayıt + doğrulama + giriş (uygulama gibi: X-Saat-Istemci); taşıyıcı belirteci döner */
export async function signUpVerified(t, email, password = PW, lang = "tr") {
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email, password, name: "", lang } });
  if (r.status !== 200) throw new Error("kayıt " + r.status + " " + r.text);
  const v = t.last("verify", email);
  const u = new URL(v.vars.url);
  const res = await t.req(u.pathname + u.search);
  if (res.status !== 302) throw new Error("doğrulama " + res.status + " " + res.text);
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { email, password } });
  if (s.status !== 200) throw new Error("giriş " + s.status + " " + s.text);
  return s.json.token;
}

export const cookieOf = (res) => res.headers.getSetCookie().map((c) => c.split(";")[0]).join("; ");
