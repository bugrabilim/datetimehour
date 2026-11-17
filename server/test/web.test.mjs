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
  // Standart 5.23: kayıt ve girişte ayrı bağlantı satırı yok; ortak metinler yalnız onay cümlesinde, hesap silmeye bağlantı yok
  for (const p of ["/kayit?dil=tr", "/giris?dil=tr", "/kayit?dil=de", "/giris?dil=de"]) {
    const x = (await t.req(p)).text.replace(/<script type="application\/json"[\s\S]*?<\/script>/, "");
    assert.ok(!/hesap-silme|account-deletion/.test(x), p);
    const foot = x.slice(x.indexOf('<footer class="foot">'), x.indexOf("</footer>"));
    assert.ok(!/uyelik-(aydinlatma|sozlesmesi)/.test(foot), p);
    if (p.startsWith("/giris")) assert.ok(!/uyelik-(aydinlatma|sozlesmesi)/.test(x), p);
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
  // Beş sekme (Profil 1-2, Güvenlik 3, 4 ve 7 [e-posta değişikliği güvenlikte; sahibin kararı, 15 Kasım 2026],
  // Tercihler 5-6, Üyelikler 8, Veri ve hesap 9-10), sonra rozetler ve kayıtlar
  const sections = [...r.text.matchAll(/<span class="num">(\d+)<\/span>/g)].map((m) => Number(m[1]));
  assert.deepEqual(sections, [1, 2, 3, 4, 7, 5, 6, 8, 9, 10]);
  const guv = r.text.slice(r.text.indexOf('id="tab-guvenlik"'), r.text.indexOf('id="tab-tercihler"'));
  assert.match(guv, /data-form="email"/, "e-posta değişikliği Güvenlik sekmesinde");
  // "İsteğe bağlı" notu profil formunun başında
  assert.ok(r.text.indexOf("isteğe bağlıdır") < r.text.indexOf('name="ad"'));
  const tabs = [...r.text.matchAll(/data-tab="([a-z]+)"/g)].map((m) => m[1]);
  assert.deepEqual(tabs, ["profil", "guvenlik", "tercihler", "uyelikler", "veri", "rozetler"]);
  assert.match(r.text, /class="tabpanel on" id="tab-profil"/);
  for (const k of ["tabProfile", "tabSecurity", "tabPrefs", "tabMembership", "tabData", "products", "download", "delete", "delPending", "badgesTitle", "deleteDownloadFirst", "deleteKept"]) assert.ok(has(r.text, UI.tr[k]), k);
  // Her sekmenin altında ortak aydınlatma metni ve sözleşme
  assert.equal(r.text.split('<p class="muted legal">').length - 1, 6);
  // E-posta değişikliği mevcut şifreyi ister; silme düğmesi ayrı kırmızı blokta
  assert.match(r.text, /data-form="email"[^]*?name="current"/);
  assert.match(r.text, /class="danger-zone"/);
  const g = await t.req("/hesap?sekme=guvenlik&dil=en", { cookie });
  assert.match(g.text, /class="tabpanel on" id="tab-guvenlik"/);
  // Kaydedilmemiş değişiklik uyarısı yok (sahibin kararı, 15 Kasım 2026)
  const js = (await t.req("/varlik/hesap.js")).text;
  assert.doesNotMatch(js, /beforeunload|t\.unsaved/);
  // Siteden "Hesabım" ile gelen dönüş yolu girişe taşınır; başka adrese yönlendirme kabul edilmez
  assert.equal((await t.req("/hesap?dil=tr&donus=%2Fdunya-saatleri%2F")).headers.get("location"), "/giris?dil=tr&donus=%2Fdunya-saatleri%2F");
  for (const bad of ["//kotu.example", "https://kotu.example/", "/\\kotu", "/a b"]) assert.equal((await t.req("/hesap?dil=tr&donus=" + encodeURIComponent(bad))).headers.get("location"), "/giris?dil=tr", bad);
  assert.match((await t.req("/giris?dil=tr&donus=%2Fen%2F")).text, /"donus":"\/en\/"/);
  const b = await t.req("/hesap/rozetler?dil=en", { cookie });
  assert.equal(b.status, 301, "rozetler artık sekme");
  assert.equal(b.headers.get("location"), "/hesap?sekme=rozetler&dil=en");
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

test("onay cümlesi merkezden: kayıt sayfası ve /v1/onay-cumlesi; bozuk bağlantılı dil İngilizceye düşer", async () => {
  const UIj = JSON.parse(await (await import("node:fs/promises")).readFile(new URL("../i18n/account.json", import.meta.url), "utf8"));
  // Merkez yokken koddaki kopya
  let r = await t.req("/v1/onay-cumlesi?dil=de");
  assert.equal(r.status, 200);
  assert.equal(r.json.surum, "2026-11-14");
  assert.equal(r.json.metin, UIj.de.consent);
  const de = "Neu: [Datenschutz](https://bumbagroup.com/uyelik-aydinlatma?dil=en) und [Bedingungen](https://bumbagroup.com/uyelik-sozlesmesi?dil=en).";
  t.center.consent = { surum: "2099-01-01", diller: [
    { kod: "tr", rtl: false, metin: UIj.tr.consent },
    { kod: "en", rtl: false, metin: "EN: [Privacy](https://bumbagroup.com/uyelik-aydinlatma?dil=en) and [Terms](https://bumbagroup.com/uyelik-sozlesmesi?dil=en)." },
    { kod: "de", rtl: false, metin: de },
    { kod: "fr", rtl: false, metin: "Mauvais [lien](https://example.com/x)." },
  ] };
  assert.equal(await t.app.consent.refresh(), true);
  r = await t.req("/v1/onay-cumlesi?dil=de-AT");
  assert.deepEqual(r.json, { surum: "2099-01-01", kod: "de", rtl: false, metin: de });
  r = await t.req("/v1/onay-cumlesi?dil=fr");
  assert.equal(r.json.kod, "en", "başka adrese bağlantılı metin alınmaz");
  const page = await t.req("/kayit?dil=de");
  assert.ok(page.text.includes(`<a href="https://bumbagroup.com/uyelik-aydinlatma?dil=en" target="_blank" rel="noopener">Datenschutz</a>`));
  t.center.consent = null;
});
