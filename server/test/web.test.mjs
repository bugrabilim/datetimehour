/* Web'deki giriş, kayıt ve hesap sayfaları (standart madde 7): 19 dil, sağdan sola diller, noindex, CSP, yönlendirmeler,
   onay cümlesindeki hukuk bağlantıları (sahibin kararı 12 Kasım 2026: Türkçeden Türkçe metin, öbür dillerden ?dil=en),
   e-posta doğrulama sonucu, kutlama e-postasını girişsiz kapatma ve kapalı üyelik. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, PW } from "./helpers.mjs";
import { createApp } from "../src/index.mjs";
import { loadConfig, LANGS } from "../src/config.mjs";
import { unsubscribeToken } from "../src/mail.mjs";
import { UI, linkify } from "../src/web.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
const has = (html, s) => html.includes(esc(s));
const cookieOf = (res) => res.headers.getSetCookie().map((c) => c.split(";")[0]).join("; ");

test("metinler: 19 dilde aynı anahtarlar, yer tutucular ve hukuk bağlantıları", () => {
  const keys = Object.keys(UI.tr).sort();
  assert.equal(Object.keys(UI).length, 19);
  for (const l of LANGS) {
    assert.deepEqual(Object.keys(UI[l]).sort(), keys, l);
    for (const k of keys) {
      const ph = (s) => (s.match(/\{\w+\}/g) || []).sort().join();
      assert.equal(ph(UI[l][k]), ph(UI.tr[k]), `${l} ${k}`);
      assert.ok(!/[٠-٩۰-۹०-९০-৯๐-๙]/.test(UI[l][k]), `${l} ${k}: Latin rakamı`);
    }
    const suffix = l === "tr" ? "" : "?dil=en";
    const links = [...UI[l].consent.matchAll(/\]\((https:[^)]+)\)/g)].map((m) => m[1]);
    assert.deepEqual(links, [`https://bumbagroup.com/uyelik-aydinlatma${suffix}`, `https://bumbagroup.com/uyelik-sozlesmesi${suffix}`], l);
  }
  assert.equal(linkify("a [b](https://x.y/z?q=1) <c>"), 'a <a href="https://x.y/z?q=1" target="_blank" rel="noopener">b</a> &lt;c&gt;');
});

test("sayfalar: 19 dil, yön, noindex, CSP, satır içi betik yok", async () => {
  for (const l of LANGS) {
    const r = await t.req(`/kayit?dil=${l}`);
    assert.equal(r.status, 200, l);
    assert.match(r.text, new RegExp(`<html lang="${l}" dir="${["ar", "ur", "fa"].includes(l) ? "rtl" : "ltr"}"`));
    assert.match(r.text, /<meta name="robots" content="noindex, nofollow">/);
    assert.ok(r.text.includes(`>${esc(UI[l].signUp)}</button>`), l);
    const terms = l === "tr" ? "https://bumbagroup.com/uyelik-sozlesmesi\"" : "https://bumbagroup.com/uyelik-sozlesmesi?dil=en\"";
    assert.ok(r.text.includes(terms), l);
    assert.equal(r.headers.get("cache-control"), "private, no-store");
    const csp = r.headers.get("content-security-policy");
    assert.match(csp, /script-src 'self' https:\/\/istatistik\.bumba\.tr/);
    assert.match(csp, /frame-ancestors 'none'/);
    // Betik etiketleri yalnız JSON verisi ve dış dosya (CSP satır içi betiğe izin vermez)
    for (const m of r.text.matchAll(/<script([^>]*)>/g)) assert.ok(/type="application\/json"|src="\/varlik\/hesap\.js"/.test(m[1]), m[0]);
    assert.ok(!/ style="/.test(r.text), "satır içi stil yok");
  }
  // Kayıtta yalnız e-posta ve şifre (görünen ad yok)
  const k = await t.req("/kayit?dil=tr");
  const names = [...k.text.matchAll(/<input[^>]*name="([^"]+)"/g)].map((m) => m[1]);
  assert.deepEqual([...new Set(names)], ["email", "password"]);
  // Dil: ?dil yoksa tarayıcının dili, desteklenmiyorsa İngilizce
  assert.match((await t.req("/giris", { headers: { "accept-language": "de-DE,de;q=0.9" } })).text, /<html lang="de"/);
  assert.match((await t.req("/giris", { headers: { "accept-language": "it-IT" } })).text, /<html lang="en"/);
  // Varlıklar
  const js = await t.req("/varlik/hesap.js");
  assert.equal(js.status, 200);
  assert.match(js.headers.get("content-type"), /javascript/);
  assert.match((await t.req("/varlik/hesap.css")).headers.get("content-type"), /text\/css/);
  assert.equal((await t.req("/robots.txt")).text, "User-agent: *\nDisallow: /\n");
});

test("yönlendirmeler: girişsiz hesap sayfası girişe, girişli giriş sayfası hesaba; hesap sayfası 10 bölüm", async () => {
  let r = await t.req("/?dil=fr");
  assert.equal(r.status, 303);
  assert.equal(r.headers.get("location"), "/giris?dil=fr");
  r = await t.req("/hesap?dil=tr");
  assert.equal(r.headers.get("location"), "/giris?dil=tr");
  await signUpVerified(t, "web@example.com");
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "web@example.com", password: PW } });
  assert.equal(s.status, 200);
  const cookie = cookieOf(s);
  assert.equal((await t.req("/giris?dil=tr", { cookie })).headers.get("location"), "/hesap?dil=tr");
  // Dil verilmezse hesabın dili
  r = await t.req("/hesap", { cookie });
  assert.equal(r.status, 200);
  assert.match(r.text, /<html lang="tr"/);
  const sections = [...r.text.matchAll(/<span class="num">(\d+)<\/span>/g)].map((m) => Number(m[1]));
  assert.deepEqual(sections, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  for (const k of ["products", "download", "delete", "deleteScopeProduct", "deleteScopeAll", "badgesTitle"]) assert.ok(has(r.text, UI.tr[k]), k);
  assert.ok(r.text.includes('href="/hesap/rozetler?dil=tr"'));
  const b = await t.req("/hesap/rozetler?dil=en", { cookie });
  assert.equal(b.status, 200);
  assert.ok(has(b.text, UI.en.badgesTitle));
});

test("doğrulama sonucu: başarı, bağlantı hatası ve e-posta değişikliğinin ilk adımı", async () => {
  await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "dogrula@example.com", password: PW, name: "", lang: "de" } });
  const v = t.last("verify", "dogrula@example.com");
  const u = new URL(v.vars.url);
  const r = await t.req(u.pathname + u.search);
  assert.equal(r.status, 302);
  const to = new URL(r.headers.get("location"), t.url);
  assert.equal(to.pathname, "/dogrulandi");
  const page = await t.req(to.pathname + to.search);
  assert.ok(has(page.text, UI.de.verifiedOk));
  assert.ok(has((await t.req("/dogrulandi?dil=en&error=INVALID_TOKEN")).text, UI.en.verifiedErr));
  assert.ok(has((await t.req("/dogrulandi?dil=tr&adim=eposta-onay")).text, UI.tr.verifiedStep));
  // Şifre sıfırlama: anahtarsız bağlantı hata gösterir, anahtarlı sayfada form ve kural satırları
  assert.ok(has((await t.req("/sifre?dil=tr")).text, UI.tr.resetBad));
  const rs = await t.req("/sifre?dil=tr&token=abc");
  assert.ok(rs.text.includes('data-form="reset"') && has(rs.text, UI.tr.pwSymbol));
});

test("kutlama e-postasını girişsiz kapatma: sayfa sorar, POST kapatır", async () => {
  const id = (await t.q(`select id from "user" where email = 'web@example.com'`)).rows[0].id;
  const k = unsubscribeToken(t.cfg.secret, id);
  const g = await t.req(`/kutlama?dil=tr&k=${encodeURIComponent(k)}`);
  assert.equal(g.status, 200);
  assert.ok(has(g.text, UI.tr.unsubButton));
  assert.equal((await t.q(`select celebrate from "user" where id = $1`, [id])).rows[0].celebrate, true);
  const p = await t.req(`/kutlama?dil=tr&k=${encodeURIComponent(k)}`, { method: "POST" });
  assert.ok(has(p.text, UI.tr.unsubDone));
  assert.equal((await t.q(`select celebrate from "user" where id = $1`, [id])).rows[0].celebrate, false);
  assert.ok(has((await t.req("/kutlama?dil=en&k=bozuk")).text, UI.en.unsubBad));
});

test("kapalı üyelik: sayfalar 'şu an kapalı' der", async () => {
  const cfg = loadConfig({ BETTER_AUTH_URL: "http://127.0.0.1:1" });
  const app = await createApp(cfg);
  await new Promise((r) => app.server.listen(0, "127.0.0.1", r));
  const port = app.server.address().port;
  const res = await fetch(`http://127.0.0.1:${port}/giris?dil=ar`);
  assert.equal(res.status, 503);
  const html = await res.text();
  assert.ok(has(html, UI.ar.closed));
  assert.match(html, /dir="rtl"/);
  await app.close();
});
