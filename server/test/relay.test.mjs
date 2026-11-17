/* Standart 5.22: kayıtta ortak servis cevap vermezse kayıt açılmaz; merkeze iletilemeyen guncelle ve ayril bırakılmaz
   (1, 2, 4… dakika, en çok günde bir; ayril bekleyen guncelle'yi geçersiz kılar); bağlı üyelik girişinde "Şifremi
   unuttum" merkezin sifirla-iste'sini çağırır; panel özetinde merkezeBekleyen ve rozet adları; ürün listesinde `hesap`. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, CENTER_KEY } from "./helpers.mjs";
import { gapMinutes } from "../src/relay.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

const panel = (op, body) => t.req(`/api/bumba-panel/${op}`, { method: "POST", headers: { authorization: `Bearer ${CENTER_KEY}`, "x-bumba-site": "saat-tarih" }, body });

test("kayıtta ortak servis cevap vermezse kayıt açılmaz", async () => {
  t.center.downOps.add("sorgula");
  const q = await t.req("/v1/uyelik/sorgula", { method: "POST", body: { eposta: "yeni@example.com" } });
  assert.equal(q.status, 503);
  const r = await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "yeni@example.com", password: "Guclu-sifre1", name: "", lang: "tr" } });
  assert.equal(r.status, 503);
  assert.equal(r.json.code, "CENTER_DOWN");
  assert.equal((await t.q(`select count(*)::int n from "user" where email = 'yeni@example.com'`)).rows[0].n, 0);
  t.center.downOps.delete("sorgula");
  await signUpVerified(t, "yeni@example.com");
});

test("yeniden deneme aralığı: 1, 2, 4… dakika, en çok günde bir", () => {
  assert.deepEqual([1, 2, 3, 10, 11, 30].map(gapMinutes), [2, 4, 8, 1024, 1440, 1440]);
  assert.equal(gapMinutes(0), 1);
});

test("merkeze iletilemeyen: bırakılmaz, ayril bekleyen guncelle'yi geçersiz kılar, panelde görünür", async () => {
  const relay = t.app.relay;
  await t.q(`delete from center_retry`);
  const tk = await signUpVerified(t, "bekleyen@example.com");
  const no = (await t.q(`select "uyeNo" from "user" where email = 'bekleyen@example.com'`)).rows[0].uyeNo;
  t.center.down = true;
  await t.req("/v1/kutlama", { method: "POST", token: tk, body: { acik: false } });
  let o = (await panel("ozet", { aralik: "bugun" })).json;
  t.center.down = false;
  assert.equal(o.merkezeBekleyen.sayi, 1);
  assert.ok(o.merkezeBekleyen.enEski);
  // 40 kez başarısız olsa da bırakılmaz; aralık en çok bir gün
  t.center.down = true;
  for (let i = 0; i < 40; i++) {
    await t.q(`update center_retry set next_at = now() - interval '1 second'`);
    await relay.run();
  }
  const row = (await t.q(`select tries, extract(epoch from (next_at - now()))::int s from center_retry`)).rows;
  assert.equal(row.length, 1);
  assert.equal(row[0].tries, 40);
  assert.ok(row[0].s <= 86400 && row[0].s > 86000);
  // ayril gelince bekleyen guncelle düşer, yalnız ayril kalır
  await relay.enqueue("ayril", { uyeNo: Number(no) });
  await relay.enqueue("guncelle", { uyeNo: Number(no), degisiklik: { kutlama: true } });
  assert.deepEqual((await t.q(`select op from center_retry`)).rows.map((r) => r.op), ["ayril"]);
  t.center.down = false;
  await t.q(`update center_retry set next_at = now() - interval '1 second'`);
  assert.equal((await relay.run()).sent, 1);
  assert.equal(t.center.find("ayril").at(-1).body.uyeNo, Number(no));
  o = (await panel("ozet", { aralik: "bugun" })).json;
  assert.deepEqual(o.merkezeBekleyen, { sayi: 0, enEski: null });
});

test("bağlı üyelik girişinde şifremi unuttum: merkez gönderir; burada açık hesapta kendi sıfırlamamız", async () => {
  const before = t.center.find("sifirla-iste").length;
  let r = await t.req("/v1/uyelik/sifirla-iste", { method: "POST", body: { eposta: "baska@example.com", dil: "de" } });
  assert.equal(r.status, 200);
  assert.deepEqual(r.json, { tamam: true });
  const c = t.center.find("sifirla-iste").slice(before);
  assert.deepEqual(c.map((x) => x.body), [{ eposta: "baska@example.com", dil: "de" }]);
  // Hesap bu üründe varsa merkez çağrılmaz, kendi sıfırlama e-postamız gider
  r = await t.req("/v1/uyelik/sifirla-iste", { method: "POST", body: { eposta: "yeni@example.com", dil: "tr" } });
  assert.equal(r.status, 200);
  assert.equal(t.center.find("sifirla-iste").length, before + 1);
  assert.ok(t.last("reset", "yeni@example.com"));
  assert.equal((await t.req("/v1/uyelik/sifirla-iste", { method: "POST", body: { eposta: "bozuk" } })).status, 400);
  t.center.downOps.add("sifirla-iste");
  assert.equal((await t.req("/v1/uyelik/sifirla-iste", { method: "POST", body: { eposta: "baska2@example.com" } })).status, 503);
  t.center.downOps.delete("sifirla-iste");
});

test("panel: rozet adları (tr, en) ve en çok verilen rozetler; ürün listesinde hesap adresi", async () => {
  const tk = await signUpVerified(t, "rozetli@example.com");
  await t.req("/v1/rozetler", { method: "POST", token: tk, body: { rozetler: [{ id: "renk", gun: "2026-11-10" }] } });
  const o = (await panel("ozet", { aralik: "bugun" })).json;
  const renk = o.rozetler.find((x) => x.kimlik === "renk");
  assert.deepEqual(renk, { kimlik: "renk", ad: { tr: "Renk ustası", en: "Colorist" }, sayi: 1 });
  const no = (await t.q(`select "uyeNo" from "user" where email = 'rozetli@example.com'`)).rows[0].uyeNo;
  const u = (await panel("uye", { uyeNo: Number(no) })).json;
  assert.deepEqual(u.rozetler, [{ kimlik: "renk", ad: { tr: "Renk ustası", en: "Colorist" }, gun: "2026-11-10" }]);
  const l = (await t.req("/v1/uyelik/urunler", { token: tk })).json.urunler;
  assert.equal(l.find((p) => p.site === "gander").hesap, "https://gander.bumba.tr/hesap");
});
