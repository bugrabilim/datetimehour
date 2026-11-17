/* Yönetim paneline veri veren uçlar (standart madde 10, 5.16) ve yönetici hesabı: anahtar sabit sürede, yalnız POST,
   ölçülemeyen alan null; yönetici hesabı kurulumda açılır, üye değildir (kayıt sayfasından açılamaz, merkez çağrılmaz,
   sayılara girmez). */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, CENTER_KEY } from "./helpers.mjs";
import { ageGroup } from "../src/panel.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

const panel = (op, body, key = CENTER_KEY, method = "POST") =>
  t.req(`/api/bumba-panel/${op}`, { method, headers: { authorization: `Bearer ${key}`, "x-bumba-site": "saat-tarih" }, body });

test("yönetici hesabı: kurulumda açılır, adı admin, numarası yok; kayıt sayfasından açılamaz, merkez çağrılmaz", async () => {
  const a = (await t.q(`select name, role, "uyeNo", "emailVerified", celebrate from "user" where email = 'bilgi@bumbagroup.com'`)).rows[0];
  assert.deepEqual(a, { name: "admin", role: "admin", uyeNo: null, emailVerified: true, celebrate: false });
  const before = t.center.calls.length;
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "Bilgi@BumbaGroup.com", password: "Guclu-sifre1", name: "", lang: "tr" } });
  assert.equal(r.status, 200, "yanıt sıradan kayıttaki gibi");
  assert.equal(t.outbox.filter((m) => m.to.toLowerCase() === "bilgi@bumbagroup.com").length, 0, "e-posta gitmez");
  assert.equal(t.center.calls.length, before, "merkez çağrılmaz");
  // Şifre "Şifremi unuttum"la belirlenir
  await t.req("/api/auth/request-password-reset", { method: "POST", body: { email: "bilgi@bumbagroup.com" } });
  const m = t.last("reset", "bilgi@bumbagroup.com");
  const token = new URL(m.vars.url).searchParams.get("token");
  assert.equal((await t.req("/api/auth/reset-password", { method: "POST", body: { token, newPassword: "Yonetici-sifre1" } })).status, 200);
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { email: "bilgi@bumbagroup.com", password: "Yonetici-sifre1" } });
  assert.equal(s.status, 200);
  const d = await t.req("/v1/uyelik/durum", { method: "POST", token: s.json.token, body: {} });
  assert.deepEqual(d.json, { birlesti: false, sifreSecimi: [] });
  assert.equal(t.center.calls.filter((c) => c.op === "kayit" && c.body.eposta === "bilgi@bumbagroup.com").length, 0);
  // Temizlik görevi yönetici hesabına numara almaz
  await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${t.cfg.cronSecret}` } });
  assert.equal(t.center.calls.filter((c) => c.op === "kayit" && c.body.eposta === "bilgi@bumbagroup.com").length, 0);
  assert.equal((await t.q(`select "uyeNo" from "user" where email = 'bilgi@bumbagroup.com'`)).rows[0].uyeNo, null);
});

test("panel uçları: anahtar, yalnız POST, bilinmeyen işlem", async () => {
  assert.equal((await panel("ozet", {}, "yanlis-anahtar")).status, 401);
  assert.equal((await t.req("/api/bumba-panel/ozet", { method: "POST", body: {} })).status, 401);
  assert.equal((await panel("ozet", undefined, CENTER_KEY, "GET")).status, 404);
  assert.equal((await panel("sil", {})).status, 404);
  assert.equal((await panel("ozet", { aralik: "1yil" })).status, 400);
  const r = await panel("ozet", {});
  assert.equal(r.headers.get("access-control-allow-origin"), null, "CORS yok");
});

test("ozet: üye sayısı, yeni kayıt, ülke ve yaş grupları, ölçüler, servisler", async () => {
  const tk = await signUpVerified(t, "panel1@example.com");
  await t.req("/v1/profil", { method: "POST", token: tk, body: { profil: { ulke: "TR", dogumTarihi: "1990-05-05" } } });
  await signUpVerified(t, "panel2@example.com");
  const r = await panel("ozet", { aralik: "30g" });
  assert.equal(r.status, 200, r.text);
  const o = r.json;
  assert.equal(o.uyeSayisi, 2, "yönetici sayılmaz");
  assert.equal(o.yeniKayit, 2);
  assert.deepEqual(o.ulkeyeGore, { TR: 1, bilinmiyor: 1 });
  assert.deepEqual(Object.keys(o.yasGrubunaGore), ["16-24", "25-34", "35-44", "45-54", "55+", "bilinmiyor"]);
  assert.equal(o.yasGrubunaGore.bilinmiyor, 1);
  assert.equal(o.aramalar, null, "sitede üye araması yok");
  assert.ok(o.olculer.istek > 0);
  assert.equal(typeof o.olculer.yanitSuresiMs, "number");
  assert.equal(typeof o.olculer.hataOrani, "number");
  assert.ok(o.olculer.veritabaniBayt > 0);
  assert.equal(o.olculer.depolamaBayt, null);
  assert.equal(o.servisler.veritabani, true);
  assert.deepEqual(Object.keys(o.surum), ["commit", "tarih"]);
});

test("uyeler ve uye: arama, süzme, sayfalama; ayrıntıda hesap (şifre özeti yok), oturumlar, rozetler, onay", async () => {
  const l = await panel("uyeler", { sayfa: 1, sayfaBoyu: 1, siralama: "eski" });
  assert.equal(l.json.toplam, 2);
  assert.equal(l.json.uyeler.length, 1);
  assert.equal(l.json.uyeler[0].eposta, "panel1@example.com");
  assert.equal(l.json.uyeler[0].gorunenAd, `Üye ${l.json.uyeler[0].uyeNo}`);
  assert.equal(l.json.uyeler[0].durum, "aktif");
  assert.equal(l.json.uyeler[0].ulke, "TR");
  assert.equal((await panel("uyeler", { ara: "panel2" })).json.toplam, 1);
  assert.equal((await panel("uyeler", { ulke: "TR" })).json.toplam, 1);
  assert.equal((await panel("uyeler", { durum: "askida" })).json.toplam, 0);
  assert.equal((await panel("uyeler", { ara: "bilgi@bumbagroup" })).json.toplam, 0, "yönetici listede yok");
  const no = l.json.uyeler[0].uyeNo;
  const tk = (await t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { email: "panel1@example.com", password: "Guclu-sifre1" } })).json.token;
  await t.req("/v1/rozetler", { method: "POST", token: tk, body: { rozetler: [{ id: "renk", gun: "2026-11-10" }] } });
  const u = await panel("uye", { uyeNo: no });
  assert.equal(u.status, 200, u.text);
  assert.equal(u.json.hesap.eposta, "panel1@example.com");
  assert.equal(u.json.hesap.sifreOzeti, null);
  assert.ok(u.json.oturumlar.length >= 1);
  assert.equal(u.json.oturumlar[0].cihaz, "iOS");
  assert.deepEqual(u.json.rozetler, [{ kimlik: "renk", ad: { tr: "Renk ustası", en: "Colorist" }, gun: "2026-11-10" }]);
  assert.ok(u.json.onay.zaman && u.json.onay.surum);
  assert.ok(u.json.sonGiris);
  assert.equal((await panel("uye", { uyeNo: 99999 })).status, 404);
});

test("yaş grubu: 16'dan küçük ve tarihsiz bilinmiyor", () => {
  const now = new Date("2026-11-14T12:00:00Z");
  assert.equal(ageGroup("2010-11-15", now), "bilinmiyor");
  assert.equal(ageGroup("2010-11-14", now), "16-24");
  assert.equal(ageGroup("1990-05-05", now), "35-44");
  assert.equal(ageGroup("1960-01-01", now), "55+");
  assert.equal(ageGroup(null, now), "bilinmiyor");
});
