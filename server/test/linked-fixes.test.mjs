/* Bağlı üyelik denemesinde (17 Kasım 2026, üye 10003) öbür ürünlerde bulunan iki açık bizde de kapalı mı:
   (1) ana ürün bilgisi (anaUrun) birleşme yanıtından ve numara-degistir gövdesinden yerel hesaba yazılır;
   (2) şifre seçiminde merkezin seçimi yapan ürüne de gönderdiği guncelle {sifreOzeti}: özet aynıysa oturumlar kalır,
       gerçekten değiştiyse istemciye `yenidenGiris` döner (hata yerine "seçtiğiniz şifreyle yeniden girin"). */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, CENTER_KEY, PW } from "./helpers.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

const center = (op, body) => t.req(`/api/bumba-uyelik/${op}`, { method: "POST", headers: { authorization: `Bearer ${CENTER_KEY}` }, body });
const tokenOf = (email, password = PW) => t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { email, password } }).then((r) => r.json?.token);
const anaUrun = async (email) => (await t.q(`select "anaUrun" from "user" where email = $1`, [email])).rows[0].anaUrun;
const hashOf = async (email) => (await t.q(`select a.password from account a join "user" u on u.id = a."userId" where u.email = $1 and a."providerId" = 'credential'`, [email])).rows[0].password;

test("birleşme yanıtındaki anaUrun yerel hesaba yazılır", async () => {
  await signUpVerified(t, "ana1@example.com");
  assert.equal(await anaUrun("ana1@example.com"), true);
  t.center.ayriHesap.add("ana1@example.com");
  t.center.merge.set("ana1@example.com", { uyeNo: 10011, eposta: "ana1@example.com", profil: { ad: "A" }, anaUrun: false });
  const tk = await tokenOf("ana1@example.com");
  const d = await t.req("/v1/uyelik/durum", { method: "POST", token: tk, body: {} });
  assert.equal(d.json.birlesti, true);
  assert.equal(await anaUrun("ana1@example.com"), false, "ana ürün başka üründe");
});

test("numara-degistir gövdesindeki anaUrun uygulanır", async () => {
  await signUpVerified(t, "ana2@example.com");
  const no = Number((await t.q(`select "uyeNo" from "user" where email = 'ana2@example.com'`)).rows[0].uyeNo);
  const r = await center("numara-degistir", { eskiNo: no, yeniNo: 10012, hesap: { profil: { ad: "B" }, anaUrun: false } });
  assert.equal(r.status, 200, r.text);
  assert.equal(await anaUrun("ana2@example.com"), false);
});

test("şifre seçimi: aynı özet oturumu kapatmaz; değişen özette istemciye yeniden giriş", async () => {
  // Bu ürünün şifresi seçilir: merkez aynı özeti geri gönderir, oturum açık kalır
  await signUpVerified(t, "sec1@example.com");
  t.center.ayriHesap.add("sec1@example.com");
  t.center.merge.set("sec1@example.com", { uyeNo: 10013, eposta: "sec1@example.com", profil: {} });
  let tk = await tokenOf("sec1@example.com");
  await t.req("/v1/uyelik/durum", { method: "POST", token: tk, body: {} });
  const same = await hashOf("sec1@example.com");
  t.center.onSifreSec = (b) => center("guncelle", { uyeNo: Number(b.uyeNo), degisiklik: { sifreOzeti: same } });
  let s = await t.req("/v1/uyelik/sifre-sec", { method: "POST", token: tk, body: { site: "saat-tarih" } });
  assert.deepEqual(s.json, { tamam: true });
  assert.equal((await t.req("/v1/hesap", { token: tk })).status, 200, "oturum açık kalır");

  // Başka ürünün şifresi seçilir: özet değişir, oturumlar kapanır, istemciye yeniden giriş
  await signUpVerified(t, "sec2@example.com");
  t.center.ayriHesap.add("sec2@example.com");
  t.center.merge.set("sec2@example.com", { uyeNo: 10014, eposta: "sec2@example.com", profil: {} });
  tk = await tokenOf("sec2@example.com");
  await t.req("/v1/uyelik/durum", { method: "POST", token: tk, body: {} });
  const other = await (await import("better-auth/crypto")).hashPassword("Gander-sifre1");
  t.center.onSifreSec = (b) => center("guncelle", { uyeNo: Number(b.uyeNo), degisiklik: { sifreOzeti: other } });
  s = await t.req("/v1/uyelik/sifre-sec", { method: "POST", token: tk, body: { site: "gander" } });
  assert.equal(s.status, 200, s.text);
  assert.deepEqual(s.json, { tamam: true, yenidenGiris: true });
  assert.equal((await t.req("/v1/hesap", { token: tk })).status, 401);
  assert.ok(await tokenOf("sec2@example.com", "Gander-sifre1"), "seçilen şifreyle giriş");
  t.center.onSifreSec = null;
  // Giriş ekranı iletiyi gösterir
  assert.match((await t.req("/giris?dil=tr&secildi=1")).text, /Seçtiğiniz şifreyle yeniden girin/);
});
