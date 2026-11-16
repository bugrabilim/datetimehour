/* Hata bildirimi (sahibin isteği, 15 Kasım 2026): hesap gerekmez, yalnız e-postayla sahibe gider, veritabanına yazılmaz;
   doğrulama, tuzak alan, başka siteden gönderim, deneme sınırı ve sayfa. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start } from "./helpers.mjs";
import { LANGS } from "../src/config.mjs";
import { UI } from "../src/web.mjs";
import { validateFeedback, feedbackMail } from "../src/feedback.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

test("doğrulama ve e-posta biçimi", () => {
  assert.equal(validateFeedback({ mesaj: "kısa" }).hata, "kisa");
  assert.equal(validateFeedback({ mesaj: "x".repeat(4001) }).hata, "uzun");
  assert.equal(validateFeedback({ mesaj: "Alarm çalmadı, neden?", eposta: "yanlis" }).hata, "eposta");
  const f = validateFeedback({ mesaj: "Alarm çalmadı\u0007, neden?", eposta: "A@B.co", kaynak: "ios", sayfa: "https://kotu.example", surum: "1.1 (31)", dil: "de" });
  assert.equal(f.mesaj, "Alarm çalmadı, neden?");
  assert.equal(f.eposta, "a@b.co");
  assert.equal(f.sayfa, null, "yalnız sitenin yolu");
  assert.equal(f.kaynak, "ios");
  const m = feedbackMail({ ...f, sayfa: "/alarm/" }, "https://time.bumba.tr");
  assert.match(m.subject, /iPhone\/iPad uygulaması/);
  assert.match(m.text, /Sayfa: https:\/\/time\.bumba\.tr\/alarm\//);
  assert.equal(m.replyTo, "a@b.co");
});

test("gönderim: hesapsız, sahibe e-posta, yanıt adresi; veritabanına yazılmaz", async () => {
  const r = await t.req("/v1/geri-bildirim", { method: "POST", body: { mesaj: "Takvimde bayram yanlış görünüyor.", eposta: "kisi@example.com", kaynak: "web", sayfa: "/takvim/2027/", dil: "tr" } });
  assert.equal(r.status, 200);
  const m = t.outbox.filter((x) => x.kind === "raw").pop();
  assert.equal(m.to, "bilgi@bumbagroup.com");
  assert.equal(m.replyTo, "kisi@example.com");
  assert.match(m.text, /Takvimde bayram yanlış/);
  const tables = await t.app.pool.query(`select table_name from information_schema.tables where table_schema = 'public'`);
  assert.ok(!tables.rows.some((x) => /feedback|bildirim/.test(x.table_name)), "bildirim tablosu yok");
  // Tuzak alan: sessizce atılır
  const before = t.outbox.length;
  assert.equal((await t.req("/v1/geri-bildirim", { method: "POST", body: { mesaj: "Spam spam spam spam", site: "http://x" } })).status, 200);
  assert.equal(t.outbox.length, before);
  // Kısa ileti, başka siteden gönderim
  assert.equal((await t.req("/v1/geri-bildirim", { method: "POST", body: { mesaj: "kısa" } })).json.hata, "kisa");
  assert.equal((await t.req("/v1/geri-bildirim", { method: "POST", headers: { origin: "https://kotu.example" }, body: { mesaj: "Başka siteden gönderim" } })).status, 403);
});

test("deneme sınırı: aynı IP saatte 5", async () => {
  const headers = { "x-real-ip": "203.0.113.9" };
  for (let i = 0; i < 5; i++) assert.equal((await t.req("/v1/geri-bildirim", { method: "POST", headers, body: { mesaj: `Deneme bildirimi ${i}` } })).status, 200);
  assert.equal((await t.req("/v1/geri-bildirim", { method: "POST", headers, body: { mesaj: "Altıncı bildirim" } })).status, 429);
});

test("sayfa: 19 dilde, aydınlatma ve gizlilik bağlantısı, dönüş", async () => {
  for (const l of LANGS) {
    for (const k of ["fbTitle", "fbIntro", "fbMessage", "fbEmail", "fbSend", "fbSent", "fbShort", "fbPrivacy", "fbPrivacyLink"]) assert.ok(UI[l][k], `${l}.${k}`);
    const r = await t.req(`/hata-bildir?dil=${l}`);
    assert.equal(r.status, 200);
    assert.match(r.text, /data-form="feedback"/);
    assert.match(r.text, l === "tr" ? /\/gizlilik\/#hata-bildirimi/ : new RegExp(`/${l}/privacy/#report-problem`));
  }
  const r = await t.req("/hata-bildir?dil=tr&donus=%2Falarm%2F");
  assert.match(r.text, /href="https:\/\/time\.bumba\.tr\/alarm\/"/);
  assert.match(r.text, /"donus":"\/alarm\/"/);
  assert.doesNotMatch((await t.req("/hata-bildir?dil=tr&donus=%2F%2Fkotu.example")).text, /kotu\.example/);
});
