/* Üyelik akışları (standart madde 7): kayıt (yalnız e-posta + şifre, şifre kuralı, onay kaydı), doğrulama ve Bumba üye
   numarası, giriş ve oturum süreleri (web 30, uygulama 90 gün; IP yok), deneme sınırları, şifre sıfırlama ve değiştirme,
   e-posta değiştirme, aynı adresle yeniden kayıt, başka Bumba ürününde kayıtlı adres. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, PW, cookieOf } from "./helpers.mjs";
import { passwordChecks, passwordOk } from "../src/profile.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

const DAY = 86400_000;

test("şifre kuralı: 8–128, rakam, büyük, küçük, sembol, boşluk yok", () => {
  assert.equal(passwordOk("Guclu-sifre1"), true);
  assert.equal(passwordOk("Gü-çlü1ş"), true, "her alfabedeki harf");
  assert.equal(passwordOk("guclu-sifre1"), false, "büyük harf yok");
  assert.equal(passwordOk("GUCLU-SIFRE1"), false, "küçük harf yok");
  assert.equal(passwordOk("Guclu-sifre"), false, "rakam yok");
  assert.equal(passwordOk("Gucludsifre1"), false, "sembol yok");
  assert.equal(passwordOk("Guclu sifre-1"), false, "boşluk");
  assert.equal(passwordOk("Gu-1a"), false, "kısa");
  assert.equal(passwordOk("A-1" + "a".repeat(126)), false, "uzun");
  assert.deepEqual(passwordChecks("abc"), { length: false, digit: false, upper: false, lower: true, symbol: false, noSpace: true, printable: true });
});

test("kayıt: zayıf şifre reddedilir, ad ve öbür alanlar kayıtta alınmaz", async () => {
  const weak = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "zayif@example.com", password: "guclu-sifre-1", name: "", lang: "tr" } });
  assert.equal(weak.status, 400);
  assert.equal(weak.json.code, "WEAK_PASSWORD");
  const extra = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "ek@example.com", password: PW, name: "", lang: "tr", uyeNo: 5 } });
  assert.notEqual(extra.status, 200, "numara istemciden verilemez");
});

test("kayıt, doğrulama ve üye numarası; onay kaydı ve sonuç sayfası", async () => {
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "ali@example.com", password: PW, name: "Ali", lang: "tr" } });
  assert.equal(r.status, 200, r.text);
  assert.equal(r.json.token, null, "doğrulanmadan oturum açılmaz");
  let u = (await t.q(`select * from "user" where email = 'ali@example.com'`)).rows[0];
  assert.equal(u.name, "", "kayıtta ad alınmaz");
  assert.equal(u.consentVersion, "2026-11-11");
  assert.ok(Date.now() - new Date(u.consentAt).getTime() < 60_000);
  assert.equal(u.uyeNo, null, "numara doğrulanmadan alınmaz");
  assert.equal(t.center.find("kayit").length, 0);

  const m = t.last("verify", "ali@example.com");
  assert.equal(m.lang, "tr");
  const vu = new URL(m.vars.url);
  const cb = new URL(vu.searchParams.get("callbackURL"));
  assert.equal(cb.origin + cb.pathname, t.url + "/dogrulandi");
  assert.equal(cb.searchParams.get("dil"), "tr");
  const k = cb.searchParams.get("k");
  assert.ok(k);

  assert.equal((await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: PW } })).status, 403, "doğrulanmamış hesap giremez");
  const v = await t.req(vu.pathname + vu.search);
  assert.equal(v.status, 302);
  u = (await t.q(`select * from "user" where email = 'ali@example.com'`)).rows[0];
  assert.equal(Number(u.uyeNo), 10458, "numara ortak servisin kayit işleminden");
  assert.equal(t.center.find("kayit")[0].site, "saat-tarih");
  const ref = (await t.q(`select result from verify_ref where k = $1`, [k])).rows[0];
  assert.equal(ref.result, "ok");
});

test("oturum: web 30 gün, uygulama 90 gün; oturum kaydında IP yok", async () => {
  const web = await t.req("/api/auth/sign-in/email", { method: "POST", headers: { origin: t.url }, body: { email: "ali@example.com", password: PW } });
  assert.equal(web.status, 200);
  const app = await t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "android" }, body: { email: "ali@example.com", password: PW } });
  const rows = (await t.q(`select s."expiresAt", s."ipAddress", s.client from session s join "user" u on u.id = s."userId" where u.email = 'ali@example.com' order by s."createdAt"`)).rows;
  assert.equal(rows.length, 2);
  for (const r of rows) assert.ok(!r.ipAddress, "IP yazılmaz");
  const days = (r) => Math.round((new Date(r.expiresAt).getTime() - Date.now()) / DAY);
  assert.equal(days(rows[0]), 30);
  assert.equal(days(rows[1]), 90);
  assert.equal(rows[1].client, "android");
  assert.ok(cookieOf(web).includes("saat"), "web çerezle");
  assert.ok(app.json.token);
});

test("aynı adresle yeniden kayıt: yanıt aynı, sahibine haber gider", async () => {
  const before = t.outbox.length;
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "ali@example.com", password: "Baska-sifre2", name: "", lang: "en" } });
  assert.equal(r.status, 200);
  const sent = t.outbox.slice(before);
  assert.equal(sent.length, 1);
  assert.equal(sent[0].kind, "exists");
  assert.equal(sent[0].lang, "tr", "kayıtlı hesabın dilinde");
});

test("başka Bumba ürününde kayıtlı adres: kayıt reddedilir (bağlı üyelik sorusu)", async () => {
  t.center.others.set("bumba@example.com", { sifre: "Gander-sifre1", hesap: { uyeNo: 10001, eposta: "bumba@example.com" } });
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "bumba@example.com", password: PW, name: "", lang: "tr" } });
  assert.equal(r.status, 400);
  assert.equal(r.json.code, "BUMBA_MEMBER");
  const q = await t.req("/v1/uyelik/sorgula", { method: "POST", body: { eposta: "bumba@example.com" } });
  assert.deepEqual(q.json, { durum: "var" });
  const q2 = await t.req("/v1/uyelik/sorgula", { method: "POST", body: { eposta: "yeni@example.com" } });
  assert.deepEqual(q2.json, { durum: "yok" });
});

test("doğrulama anında adres başka üründe çıkarsa hesap açılmaz, sonuç sayfası bunu bilir", async () => {
  await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "gec@example.com", password: PW, name: "", lang: "en" } });
  t.center.others.set("gec@example.com", { sifre: "x", hesap: { uyeNo: 10002, eposta: "gec@example.com" } });
  const m = t.last("verify", "gec@example.com");
  const vu = new URL(m.vars.url);
  const k = new URL(vu.searchParams.get("callbackURL")).searchParams.get("k");
  await t.req(vu.pathname + vu.search);
  assert.equal((await t.q(`select count(*)::int n from "user" where email = 'gec@example.com'`)).rows[0].n, 0);
  assert.equal((await t.q(`select result from verify_ref where k = $1`, [k])).rows[0].result, "bumba");
});

test("yasaklı adresle kayıt: hesap açılmaz, yanıt sıradan kayıttaki gibi", async () => {
  t.center.banned.add("yasak@example.com");
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "yasak@example.com", password: PW, name: "", lang: "tr" } });
  assert.equal(r.status, 200);
  assert.equal(r.json.token, null);
  assert.equal((await t.q(`select count(*)::int n from "user" where email = 'yasak@example.com'`)).rows[0].n, 0);
  assert.equal(t.last("verify", "yasak@example.com"), undefined);
});

test("şifre sıfırlama: bağlantı hesap sunucusundaki sayfaya, yeni şifre kurala uymalı, oturumlar kapanır", async () => {
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { email: "ali@example.com", password: PW } });
  const old = s.json.token;
  await t.req("/api/auth/request-password-reset", { method: "POST", body: { email: "ali@example.com" } });
  const m = t.last("reset", "ali@example.com");
  const u = new URL(m.vars.url);
  assert.equal(u.origin + u.pathname, t.url + "/sifre");
  const token = u.searchParams.get("token");
  const weak = await t.req("/api/auth/reset-password", { method: "POST", body: { token, newPassword: "zayif-sifre" } });
  assert.equal(weak.status, 400);
  const ok = await t.req("/api/auth/reset-password", { method: "POST", body: { token, newPassword: "Yeni-sifre3" } });
  assert.equal(ok.status, 200, ok.text);
  assert.ok(t.last("passwordChanged", "ali@example.com"), "şifreniz değişti e-postası");
  const g = t.center.find("guncelle").at(-1);
  assert.equal(g.body.uyeNo, 10458);
  assert.match(g.body.degisiklik.sifreOzeti, /^[0-9a-f]+:[0-9a-f]+$/, "bağlı ürünlere yeni şifre özeti");
  assert.equal((await t.req("/api/auth/get-session", { token: old })).json, null, "eski oturumlar kapanır");
  assert.equal((await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "ali@example.com", password: "Yeni-sifre3" } })).status, 200);
  const again = await t.req("/api/auth/reset-password", { method: "POST", body: { token, newPassword: "Yeni-sifre4" } });
  assert.equal(again.status, 400, "bağlantı bir kez kullanılır");
});

test("şifre değiştirme: mevcut şifre, kural, haber ve bağlı ürünlere özet", async () => {
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { email: "ali@example.com", password: "Yeni-sifre3" } });
  const token = s.json.token;
  assert.equal((await t.req("/api/auth/change-password", { method: "POST", token, body: { currentPassword: "Yanlis-sifre1", newPassword: "En-yeni-sifre5" } })).status, 400);
  assert.equal((await t.req("/api/auth/change-password", { method: "POST", token, body: { currentPassword: "Yeni-sifre3", newPassword: "zayif" } })).status, 400);
  const before = t.center.find("guncelle").length;
  const ok = await t.req("/api/auth/change-password", { method: "POST", token, body: { currentPassword: "Yeni-sifre3", newPassword: "En-yeni-sifre5", revokeOtherSessions: true } });
  assert.equal(ok.status, 200, ok.text);
  assert.equal(t.center.find("guncelle").length, before + 1);
  assert.equal(t.outbox.filter((m) => m.kind === "passwordChanged" && m.to === "ali@example.com").length, 2);
});

test("e-posta değiştirme: önce ortak servise sorulur, eski adrese onay, yeni adrese doğrulama, bağlı ürünlere yeni adres", async () => {
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { email: "ali@example.com", password: "En-yeni-sifre5" } });
  const token = s.json.token;
  const taken = await t.req("/api/auth/change-email", { method: "POST", token, body: { newEmail: "bumba@example.com" } });
  assert.equal(taken.status, 400, "başka üyenin adresi");
  const r = await t.req("/api/auth/change-email", { method: "POST", token, body: { newEmail: "ali.yeni@example.com" } });
  assert.equal(r.status, 200, r.text);
  const c = t.last("changeOld", "ali@example.com");
  const cu = new URL(c.vars.url);
  assert.equal((await t.req(cu.pathname + cu.search)).status, 302);
  const v = t.last("verify", "ali.yeni@example.com");
  const vu = new URL(v.vars.url);
  assert.equal((await t.req(vu.pathname + vu.search)).status, 302);
  const g = await t.req("/api/auth/get-session", { token });
  assert.equal(g.json.user.email, "ali.yeni@example.com");
  const last = t.center.find("guncelle").at(-1);
  assert.deepEqual(last.body.degisiklik, { eposta: "ali.yeni@example.com", epostaDogrulandi: true });
});

test("deneme sınırları: giriş dakikada 5, kayıt 5 dakikada 3", async () => {
  await signUpVerified(t, "hiz@example.com");
  const ip = { "x-real-ip": "192.0.2.77" };
  const codes = [];
  for (let i = 0; i < 6; i++) codes.push((await t.req("/api/auth/sign-in/email", { method: "POST", headers: ip, body: { email: "hiz@example.com", password: "Yanlis-sifre1" } })).status);
  assert.deepEqual(codes, [401, 401, 401, 401, 401, 429]);
  const ip2 = { "x-real-ip": "192.0.2.78" };
  const reg = [];
  for (let i = 0; i < 4; i++) reg.push((await t.req("/api/auth/sign-up/email", { method: "POST", headers: ip2, body: { email: `k${i}@example.com`, password: PW, name: "", lang: "tr" } })).status);
  assert.deepEqual(reg, [200, 200, 200, 429]);
  const xff = await t.req("/api/auth/sign-in/email", { method: "POST", headers: { ...ip, "x-forwarded-for": "198.51.100.1" }, body: { email: "hiz@example.com", password: PW } });
  assert.equal(xff.status, 429, "X-Forwarded-For okunmaz");
});

test("dil, tema ve saat dilimi hesapta", async () => {
  const token = await signUpVerified(t, "dil@example.com", PW, "ar");
  assert.equal(t.last("verify", "dil@example.com").lang, "ar");
  assert.match(t.last("verify", "dil@example.com").html, /dir="rtl"/);
  assert.equal((await t.req("/api/auth/update-user", { method: "POST", token, body: { lang: "in", theme: "dark", timeZone: "Asia/Jakarta" } })).status, 200);
  const g = (await t.req("/api/auth/get-session", { token })).json.user;
  assert.equal(g.lang, "id");
  assert.equal(g.theme, "dark");
  assert.equal(g.timeZone, "Asia/Jakarta");
  assert.equal((await t.req("/api/auth/update-user", { method: "POST", token, body: { timeZone: "Mars/Olympus" } })).status, 400);
  assert.equal((await t.req("/api/auth/update-user", { method: "POST", token, body: { role: "admin" } })).status === 200
    ? (await t.req("/api/auth/get-session", { token })).json.user.role : "user", "user", "rol uygulamadan değiştirilemez");
});

test("sağlık, robots, güvenlik başlıkları", async () => {
  const h = await t.req("/saglik");
  assert.equal(h.status, 200);
  assert.equal(h.json.durum, "ok");
  assert.equal(h.headers.get("strict-transport-security"), "max-age=31536000");
  assert.equal(h.headers.get("x-content-type-options"), "nosniff");
  assert.match((await t.req("/robots.txt")).text, /Disallow: \//);
});
