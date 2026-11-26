/* Standart 5.28 (madde 7 "Saklama", "Hukuk"): kullanılmayan hesabı merkez sayar ve siler; ürün her başarılı girişte
   `sorgula`'yı `uyeNo`'yla çağırır, son girişi kalıcı yazar (panel `uye`/`uyeler` `sonGiris`, oturumlar silinse de),
   doğrulanmamış hesabın silinmesinde merkeze `ayril {neden: "dogrulanmamis"}` gönderir ve kullanılmayan hesabı kendisi
   silmez. Üyelik metinleri sürümü 2026-11-22. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, CENTER_KEY, CRON, PW } from "./helpers.mjs";
import { TERMS_VERSION } from "../src/config.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

const panel = (op, body) => t.req(`/api/bumba-panel/${op}`, { method: "POST", headers: { authorization: `Bearer ${CENTER_KEY}`, "x-bumba-site": "saat-tarih" }, body });
const until = async (fn, ms = 3000) => { const end = Date.now() + ms; while (Date.now() < end) { if (await fn()) return true; await new Promise((r) => setTimeout(r, 25)); } return false; };

test("üyelik metinleri sürümü 2026-11-22", () => {
  assert.equal(TERMS_VERSION, "2026-11-22");
});

test("her başarılı girişte sorgula uyeNo'yla gider; son giriş kalıcıdır, oturumlar silinse de panelde görünür", async () => {
  await signUpVerified(t, "giris1@example.com");
  const u = (await t.q(`select "uyeNo", "lastLoginAt" from "user" where email = 'giris1@example.com'`)).rows[0];
  assert.ok(u.uyeNo != null && u.lastLoginAt, "son giriş yazıldı");
  const no = Number(u.uyeNo);
  const hasSorgula = () => t.center.find("sorgula").some((c) => c.body.uyeNo === no && c.body.eposta === "giris1@example.com");
  assert.ok(await until(hasSorgula), "girişte sorgula {eposta, uyeNo}");
  // Yanlış şifreyle giriş: ne son giriş ne sorgula
  const n = t.center.find("sorgula").length;
  const bad = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "giris1@example.com", password: "Yanlis-sifre9" } });
  assert.equal(bad.status, 401);
  await new Promise((r) => setTimeout(r, 100));
  assert.equal(t.center.find("sorgula").length, n);
  // Oturumlar silinse de (çıkış, süre dolması) panel son girişi verir
  await t.q(`delete from session where "userId" = (select id from "user" where email = 'giris1@example.com')`);
  const d = await panel("uye", { uyeNo: no });
  assert.equal(d.status, 200, d.text);
  assert.equal(d.json.sonGiris, new Date(u.lastLoginAt).toISOString());
  assert.deepEqual(d.json.oturumlar, []);
  const l = await panel("uyeler", { ara: "giris1" });
  assert.equal(l.json.uyeler[0].sonGiris, new Date(u.lastLoginAt).toISOString());
  // Yeni giriş son girişi ve oturumun son kullanımını günceller
  await new Promise((r) => setTimeout(r, 20));
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { email: "giris1@example.com", password: PW } });
  assert.equal(s.status, 200);
  const d2 = await panel("uye", { uyeNo: no });
  assert.ok(new Date(d2.json.sonGiris) > new Date(u.lastLoginAt));
  assert.equal(d2.json.oturumlar.length, 1);
  assert.ok(d2.json.oturumlar[0].sonKullanim);
});

test("doğrulanmamış hesap 7 günde silinir, merkeze ayril {neden: dogrulanmamis}; kullanılmayan hesap silinmez", async () => {
  // Doğrulanmamış ama numaralı hesap (ör. bağlı üyelikten) ve 4 yıldır girmeyen doğrulanmış hesap
  await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "dogrulanmadi@example.com", password: PW, name: "", lang: "tr" } });
  await t.q(`update "user" set "uyeNo" = 77001, "createdAt" = now() - interval '8 days' where email = 'dogrulanmadi@example.com'`);
  await signUpVerified(t, "eski@example.com");
  await t.q(`update "user" set "createdAt" = now() - interval '4 years', "lastLoginAt" = now() - interval '4 years' where email = 'eski@example.com'`);
  await t.q(`delete from session where "userId" = (select id from "user" where email = 'eski@example.com')`);
  const r = await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${CRON}` } });
  assert.equal(r.status, 200, r.text);
  const a = t.center.find("ayril").find((c) => c.body.uyeNo === 77001);
  assert.deepEqual(a?.body, { uyeNo: 77001, neden: "dogrulanmamis" });
  assert.equal((await t.q(`select 1 from "user" where email = 'dogrulanmadi@example.com'`)).rowCount, 0);
  assert.equal((await t.q(`select 1 from "user" where email = 'eski@example.com'`)).rowCount, 1, "kullanılmayan hesabı ürün silmez");
});

test("5.33: aydınlatma güncellemesi e-postası her üyeye bir kez; dile göre TR/EN; askıdaki, yasaklı, doğrulanmamış ve yönetici almaz", async () => {
  await signUpVerified(t, "bildirim-tr@example.com", PW, "tr");
  await signUpVerified(t, "bildirim-de@example.com", PW, "de");
  await signUpVerified(t, "bildirim-aski@example.com", PW, "tr");
  await t.q(`update "user" set banned = true where email = 'bildirim-aski@example.com'`);
  await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "bildirim-dogrulanmadi@example.com", password: PW, name: "", lang: "tr" } });
  const run = () => t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${CRON}` } });
  assert.equal((await run()).status, 200);
  const tr = t.last("notice", "bildirim-tr@example.com");
  assert.ok(tr, "Türkçe üye aldı");
  assert.equal(tr.subject, "Üyelik aydınlatma metni güncellendi");
  assert.match(tr.text, /Saat Tarih üyeliğiniz/);
  assert.match(tr.text, /https:\/\/bumbagroup\.com\/uyelik-aydinlatma/);
  assert.match(tr.html, /<a href="https:\/\/bumbagroup\.com\/uyelik-aydinlatma"/);
  assert.doesNotMatch(tr.text + tr.html, /kutlama|unsubscribe|List-Unsubscribe/i, "kapatma bağlantısı yok");
  const de = t.last("notice", "bildirim-de@example.com");
  assert.equal(de.subject, "Membership privacy notice updated");
  assert.match(de.text, /your Time & Date membership/);
  assert.match(de.text, /https:\/\/bumbagroup\.com\/en\/uyelik-aydinlatma/);
  assert.equal(t.last("notice", "bildirim-aski@example.com"), undefined);
  assert.equal(t.last("notice", "bildirim-dogrulanmadi@example.com"), undefined);
  assert.equal(t.last("notice", t.cfg.adminEmail), undefined);
  // İkinci çalıştırma kimseye yeniden göndermez
  const n = t.outbox.filter((m) => m.kind === "notice").length;
  assert.equal((await run()).status, 200);
  assert.equal(t.outbox.filter((m) => m.kind === "notice").length, n);
});
