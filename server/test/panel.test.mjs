/* Yönetim paneli (madde 10): yalnız doğrulanmış yönetici girer, aynı kaynaktan form, giriş sınırı, iki dil, toplu sayılar. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, cookieOf } from "./helpers.mjs";

let t;
before(async () => {
  t = await start({ ADMIN_EMAIL: "Yonetici@Example.com" });
  await signUpVerified(t, "yonetici@example.com", "yonetici-sifre-1");
  await signUpVerified(t, "uye@example.com", "uye-sifre-1", "en");
});
after(async () => { await t.stop(); });

const login = (email, password, extra = {}) =>
  t.req("/panel/giris?dil=tr", { method: "POST", form: { email, password }, headers: { origin: t.url, ...extra } });

test("girişsiz panel giriş sayfasına yönlenir; sayfa dizine girmez", async () => {
  const r = await t.req("/panel", { headers: { "accept-language": "tr-TR,tr;q=0.9" } });
  assert.equal(r.status, 303);
  assert.equal(r.headers.get("location"), "/panel/giris?dil=tr");
  const g = await t.req("/panel/giris?dil=en");
  assert.equal(g.status, 200);
  assert.match(g.text, /noindex/);
  assert.match(g.text, /Administrator sign-in/);
  const css = await t.req("/panel.css");
  assert.match(css.headers.get("content-type"), /text\/css/);
  assert.match(css.text, /prefers-color-scheme:dark/);
});

test("başka kaynaktan gelen form reddedilir", async () => {
  const r = await t.req("/panel/giris?dil=tr", { method: "POST", form: { email: "yonetici@example.com", password: "yonetici-sifre-1" }, headers: { origin: "https://kotu.example" } });
  assert.equal(r.status, 400);
  assert.equal(r.headers.get("set-cookie"), null);
});

test("yönetici olmayan hesap giremez, oturumu açık kalmaz", async () => {
  const before = (await t.app.pool.query(`select count(*)::int n from session`)).rows[0].n;
  const r = await login("uye@example.com", "uye-sifre-1");
  assert.equal(r.status, 400);
  assert.match(r.text, /yönetici değil/);
  assert.equal((await t.app.pool.query(`select count(*)::int n from session`)).rows[0].n, before);
  const role = (await t.app.pool.query(`select role from "user" where email = 'uye@example.com'`)).rows[0].role;
  assert.equal(role, "user");
});

test("yönetici girer, toplu sayıları görür, çıkış yapar", async () => {
  const role = (await t.app.pool.query(`select role from "user" where email = 'yonetici@example.com'`)).rows[0].role;
  assert.equal(role, "admin", "doğrulanınca sabit adres yönetici olur");
  const r = await login("yonetici@example.com", "yonetici-sifre-1");
  assert.equal(r.status, 303);
  assert.equal(r.headers.get("location"), "/panel?dil=tr");
  const cookie = cookieOf(r);
  assert.match(cookie, /saat\.session_token=/);
  assert.match(r.headers.getSetCookie().join("\n"), /HttpOnly/i);

  const p = await t.req("/panel?dil=tr&aralik=bugun", { cookie });
  assert.equal(p.status, 200);
  assert.match(p.text, /Üyeler/);
  assert.match(p.text, /<td>Toplam<\/td><td>2<\/td>/);
  assert.match(p.text, /<td>Doğrulanmış<\/td><td>2<\/td>/);
  assert.match(p.text, /bu ortamda ölçülemiyor/, "Umami yoksa sayı uydurulmaz");
  assert.doesNotMatch(p.text, /uye@example\.com/, "kişisel veri görünmez");
  assert.match(p.headers.get("cache-control"), /no-store/);

  const en = await t.req("/panel?dil=en&aralik=30", { cookie });
  assert.match(en.text, /Members/);
  assert.match(en.text, /Last 30 days/);

  const out = await t.req("/panel/cikis?dil=tr", { method: "POST", cookie, headers: { origin: t.url } });
  assert.equal(out.status, 303);
  const after = await t.req("/panel", { cookie });
  assert.equal(after.status, 303, "çıkıştan sonra oturum geçersiz");
});

test("üye hesabın belirteciyle panel açılmaz", async () => {
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "uye@example.com", password: "uye-sifre-1" } });
  const r = await t.req("/panel", { token: s.json.token });
  assert.equal(r.status, 303);
});

test("panel girişinde deneme sınırı", async () => {
  const ip = { "x-real-ip": "198.51.100.9" };
  let last;
  for (let i = 0; i < 6; i++) last = await login("yonetici@example.com", "yanlis-sifre", ip);
  assert.equal(last.status, 400);
  assert.match(last.text, /Çok fazla deneme/);
});
