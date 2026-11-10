/* Üyelik akışları (standart madde 7): kayıt, doğrulama, giriş, dil, şifre sıfırlama, e-posta değiştirme, verilerim,
   hesap silme; aynı adresle yeniden kayıt hesabın varlığını ele vermez; hız sınırı. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified } from "./helpers.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

test("kayıt doğrulama ister, doğrulama bağlantısı sitedeki sayfaya döner", async () => {
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "ali@example.com", password: "guclu-sifre-1", name: "Ali", lang: "tr" } });
  assert.equal(r.status, 200);
  assert.equal(r.json.token, null, "doğrulanmadan oturum açılmaz");
  const m = t.last("verify", "ali@example.com");
  assert.ok(m, "doğrulama e-postası gitti");
  assert.equal(m.lang, "tr");
  assert.match(m.text, /Saat Tarih/);
  assert.match(m.html, /hesap\/dogrulandi|verify-email/);
  const u = new URL(m.vars.url);
  assert.equal(u.origin, t.url);
  assert.equal(u.searchParams.get("callbackURL"), "https://time.bumba.tr/hesap/dogrulandi/");

  const early = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "guclu-sifre-1" } });
  assert.equal(early.status, 403, "doğrulanmamış hesap giremez");

  const v = await t.req(u.pathname + u.search);
  assert.equal(v.status, 302);
  assert.equal(v.headers.get("location"), "https://time.bumba.tr/hesap/dogrulandi/");
});

test("aynı adresle yeniden kayıt: yanıt aynı, sahibine haber gider", async () => {
  const before = t.outbox.length;
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "ali@example.com", password: "baska-sifre-2", name: "X", lang: "en" } });
  assert.equal(r.status, 200);
  assert.equal(r.json.token, null);
  const sent = t.outbox.slice(before);
  assert.equal(sent.length, 1);
  assert.equal(sent[0].kind, "exists");
  assert.equal(sent[0].lang, "tr", "kayıtlı hesabın dilinde");
});

test("giriş, oturum, dil ve rol", async () => {
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "guclu-sifre-1" } });
  assert.equal(s.status, 200);
  const token = s.json.token;
  assert.ok(token);
  const g = await t.req("/api/auth/get-session", { token });
  assert.equal(g.json.user.email, "ali@example.com");
  assert.equal(g.json.user.lang, "tr");
  assert.equal(g.json.user.role, "user");

  assert.equal((await t.req("/api/auth/update-user", { method: "POST", token, body: { lang: "de" } })).status, 200);
  assert.equal((await t.req("/api/auth/get-session", { token })).json.user.lang, "de");
  await t.req("/api/auth/update-user", { method: "POST", token, body: { lang: "in" } });
  assert.equal((await t.req("/api/auth/get-session", { token })).json.user.lang, "id", "Android'in eski Endonezce kodu");
  await t.req("/api/auth/update-user", { method: "POST", token, body: { lang: "xx" } });
  assert.equal((await t.req("/api/auth/get-session", { token })).json.user.lang, "en", "bilinmeyen dil İngilizce");

  const role = await t.req("/api/auth/update-user", { method: "POST", token, body: { role: "admin" } });
  assert.notEqual(role.status, 200, "rol uygulamadan değiştirilemez");
  assert.equal((await t.req("/api/auth/get-session", { token })).json.user.role, "user");
  await t.req("/api/auth/update-user", { method: "POST", token, body: { lang: "de" } });
});

test("verilerimi indir", async () => {
  assert.equal((await t.req("/v1/verilerim")).status, 401);
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "guclu-sifre-1" } });
  const r = await t.req("/v1/verilerim", { token: s.json.token });
  assert.equal(r.status, 200);
  assert.match(r.headers.get("content-disposition"), /attachment/);
  assert.equal(r.json.hesap.email, "ali@example.com");
  assert.ok(Array.isArray(r.json.oturumlar) && r.json.oturumlar.length >= 1);
  assert.equal(r.json.hesap.password, undefined);
});

test("şifre sıfırlama sitedeki formdan", async () => {
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "guclu-sifre-1" } });
  const old = s.json.token;
  const r = await t.req("/api/auth/request-password-reset", { method: "POST", body: { email: "ali@example.com" } });
  assert.equal(r.status, 200);
  const m = t.last("reset", "ali@example.com");
  assert.equal(m.lang, "de");
  const u = new URL(m.vars.url);
  assert.equal(u.origin + u.pathname, "https://time.bumba.tr/de/account/password/");
  const token = u.searchParams.get("token");
  assert.ok(token);

  const back = "https://time.bumba.tr/de/account/password/";
  let p = await t.req("/sifre", { method: "POST", form: { token, sifre: "yeni-sifre-3", sifre2: "farkli", dil: "de" } });
  assert.equal(p.status, 303);
  assert.equal(p.headers.get("location"), back + "?durum=hata&neden=eslesme&token=" + encodeURIComponent(token), "anahtar formun yeniden açılması için geri gelir");
  p = await t.req("/sifre", { method: "POST", form: { token, sifre: "kisa", sifre2: "kisa", dil: "de" } });
  assert.equal(p.headers.get("location"), back + "?durum=hata&neden=uzunluk&token=" + encodeURIComponent(token));
  p = await t.req("/sifre", { method: "POST", form: { token: "yanlis", sifre: "yeni-sifre-3", sifre2: "yeni-sifre-3", dil: "de" } });
  assert.equal(p.headers.get("location"), back + "?durum=hata&neden=baglanti");
  p = await t.req("/sifre", { method: "POST", form: { token, sifre: "yeni-sifre-3", sifre2: "yeni-sifre-3", dil: "de" } });
  assert.equal(p.headers.get("location"), back + "?durum=tamam");
  p = await t.req("/sifre", { method: "POST", form: { token, sifre: "yeni-sifre-4", sifre2: "yeni-sifre-4", dil: "de" } });
  assert.equal(p.headers.get("location"), back + "?durum=hata&neden=baglanti", "bağlantı bir kez kullanılır");

  assert.equal((await t.req("/api/auth/get-session", { token: old })).json, null, "eski oturumlar kapanır");
  assert.equal((await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "guclu-sifre-1" } })).status, 401);
  assert.equal((await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "yeni-sifre-3" } })).status, 200);
});

test("bilinmeyen adrese sıfırlama: yanıt aynı, e-posta gitmez", async () => {
  const before = t.outbox.length;
  const r = await t.req("/api/auth/request-password-reset", { method: "POST", body: { email: "yok@example.com" } });
  assert.equal(r.status, 200);
  assert.equal(t.outbox.length, before);
});

test("şifre değiştirme", async () => {
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "yeni-sifre-3" } });
  const token = s.json.token;
  const bad = await t.req("/api/auth/change-password", { method: "POST", token, body: { currentPassword: "yanlis-sifre", newPassword: "en-yeni-sifre-5" } });
  assert.equal(bad.status, 400);
  const ok = await t.req("/api/auth/change-password", { method: "POST", token, body: { currentPassword: "yeni-sifre-3", newPassword: "en-yeni-sifre-5", revokeOtherSessions: true } });
  assert.equal(ok.status, 200);
  assert.equal((await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "en-yeni-sifre-5" } })).status, 200);
});

test("e-posta değiştirme: önce eski adrese onay, sonra yeni adrese doğrulama", async () => {
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "en-yeni-sifre-5" } });
  const token = s.json.token;
  const r = await t.req("/api/auth/change-email", { method: "POST", token, body: { newEmail: "ali.yeni@example.com" } });
  assert.equal(r.status, 200, r.text);
  const c = t.last("changeOld", "ali@example.com");
  assert.ok(c, "eski adrese onay gitti");
  assert.equal(c.vars.email, "ali.yeni@example.com");
  const cu = new URL(c.vars.url);
  assert.equal(cu.searchParams.get("callbackURL"), "https://time.bumba.tr/de/account/verified/?adim=eposta-onay");
  const step1 = await t.req(cu.pathname + cu.search);
  assert.equal(step1.status, 302);
  assert.equal(step1.headers.get("location"), "https://time.bumba.tr/de/account/verified/?adim=eposta-onay");

  const v = t.last("verify", "ali.yeni@example.com");
  assert.ok(v, "yeni adrese doğrulama gitti");
  const vu = new URL(v.vars.url);
  assert.equal(vu.searchParams.get("callbackURL"), "https://time.bumba.tr/de/account/verified/?adim=eposta");
  const step2 = await t.req(vu.pathname + vu.search);
  assert.equal(step2.status, 302);
  assert.equal(step2.headers.get("location"), "https://time.bumba.tr/de/account/verified/?adim=eposta");

  const g = await t.req("/api/auth/get-session", { token });
  assert.equal(g.json.user.email, "ali.yeni@example.com");
  assert.equal(g.json.user.emailVerified, true);
  assert.equal((await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali.yeni@example.com", password: "en-yeni-sifre-5" } })).status, 200);
});

test("hesap silme şifreyle; haber e-postası gider, hesap kalmaz", async () => {
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali.yeni@example.com", password: "en-yeni-sifre-5" } });
  const token = s.json.token;
  assert.equal((await t.req("/api/auth/delete-user", { method: "POST", token, body: { password: "yanlis-sifre" } })).status, 400);
  const d = await t.req("/api/auth/delete-user", { method: "POST", token, body: { password: "en-yeni-sifre-5" } });
  assert.equal(d.status, 200, d.text);
  const m = t.last("deleted", "ali.yeni@example.com");
  assert.ok(m);
  assert.equal(m.lang, "de");
  assert.equal((await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali.yeni@example.com", password: "en-yeni-sifre-5" } })).status, 401);
  const rows = await t.app.pool.query(`select count(*)::int n from "user"`);
  assert.equal(rows.rows[0].n, 0);
  const sessions = await t.app.pool.query(`select count(*)::int n from session`);
  assert.equal(sessions.rows[0].n, 0, "oturumlar da silinir");
});

test("dil kodları: bilinmeyen dil İngilizce, e-posta o dilde", async () => {
  await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "x1@example.com", password: "guclu-sifre-1", name: "X", lang: "it" } });
  assert.equal(t.last("verify", "x1@example.com").lang, "en");
  await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "x2@example.com", password: "guclu-sifre-1", name: "X", lang: "ar" } });
  const m = t.last("verify", "x2@example.com");
  assert.equal(m.lang, "ar");
  assert.match(m.html, /dir="rtl"/);
  assert.equal(new URL(m.vars.url).searchParams.get("callbackURL"), "https://time.bumba.tr/ar/account/verified/");
});

test("giriş hız sınırı: aynı adresten dakikada 5 deneme", async () => {
  await signUpVerified(t, "hiz@example.com");
  const ip = { "x-real-ip": "192.0.2.77" };
  const codes = [];
  for (let i = 0; i < 6; i++) codes.push((await t.req("/api/auth/sign-in/email", { method: "POST", headers: ip, body: { email: "hiz@example.com", password: "yanlis-sifre" } })).status);
  assert.deepEqual(codes, [401, 401, 401, 401, 401, 429]);
  const other = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "hiz@example.com", password: "guclu-sifre-1" } });
  assert.equal(other.status, 200, "başka adres etkilenmez");
});

test("sağlık, robots, güvenlik başlıkları", async () => {
  const h = await t.req("/saglik");
  assert.equal(h.status, 200);
  assert.deepEqual(h.json, { durum: "ok", veritabani: true });
  assert.equal(h.headers.get("strict-transport-security"), "max-age=31536000");
  assert.equal(h.headers.get("x-content-type-options"), "nosniff");
  assert.match(h.headers.get("content-security-policy"), /frame-ancestors 'none'/);
  assert.match((await t.req("/robots.txt")).text, /Disallow: \//);
  const root = await t.req("/");
  assert.equal(root.status, 303);
  assert.equal(root.headers.get("location"), "https://time.bumba.tr");
  assert.equal((await t.req("/yok")).status, 404);
});
