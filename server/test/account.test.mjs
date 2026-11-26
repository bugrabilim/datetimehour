/* Hesap sayfası işleri ve bağlı üyelik (standart madde 7 ve 13): profil ve görünürlük, fotoğraf, kutlama tercihi ve
   girişsiz kapatma, kayıtlar ve rozetler, verilerimi indir, hesabı silme (bu üründen / bütün Bumba ürünlerinden),
   ortak servisin ürün uçları (dogrula, hesap-getir, hesap-ac, guncelle, durum, hesap-sil, numara-degistir), "Evet"le
   bağlanma, ayrı hesap birleştirme, ürün listesi, kutlama e-postaları, temizlik ve kapalı üyelik. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, PW, testPhoto, CENTER_KEY, CRON } from "./helpers.mjs";
import { createApp } from "../src/index.mjs";
import { loadConfig } from "../src/config.mjs";
import { birthDateRange } from "../src/profile.mjs";
import { occasionFor, nextNine, runCelebrations } from "../src/celebrate.mjs";
import { runCleanup } from "../src/cleanup.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

const center = (op, body, key = CENTER_KEY) => t.req(`/api/bumba-uyelik/${op}`, { method: "POST", headers: { authorization: `Bearer ${key}` }, body });
const tokenOf = (email, password = PW) => t.req("/api/auth/sign-in/email", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { email, password } }).then((r) => r.json?.token);

let tok;
test("profil: isteğe bağlı alanlar, doğrulama, görünürlük varsayılan kapalı; bağlı ürünlere iletilir", async () => {
  tok = await signUpVerified(t, "profil@example.com");
  let h = await t.req("/v1/hesap", { token: tok });
  assert.equal(h.status, 200);
  assert.equal(h.json.uyeNo, 10458);
  assert.deepEqual(h.json.profil.gorunurluk, { acik: false, alanlar: [] });
  const r = birthDateRange();
  const profil = {
    ad: "Ayşe", soyad: null, takmaAd: "aysee", dogumTarihi: r.latest, cinsiyet: "kadin",
    telefon: { ulkeKodu: "+90", numara: "0532 123 45 67" }, ulke: "tr", sehir: "İzmir", meslek: "Öğretmen", hakkimda: "Merhaba",
    sosyal: [{ platform: "github", deger: "ayse" }], gorunurluk: { acik: true, alanlar: ["takmaAd", "eposta", "sehir"] },
  };
  const p = await t.req("/v1/profil", { method: "POST", token: tok, body: { profil } });
  assert.equal(p.status, 200, p.text);
  assert.deepEqual(p.json.profil.telefon, { ulkeKodu: "+90", numara: "5321234567" }, "E.164 parçaları");
  assert.equal(p.json.profil.ulke, "TR");
  assert.deepEqual(p.json.profil.gorunurluk.alanlar, ["takmaAd", "sehir"], "e-posta hiçbir zaman görünmez");
  assert.equal(t.center.find("guncelle").at(-1).body.degisiklik.profil.ad, "Ayşe");
  for (const [k, v] of [["dogumTarihi", "2015-01-01"], ["dogumTarihi", "1900-01-01"], ["cinsiyet", "x"], ["ulke", "XX"], ["hakkimda", "a".repeat(161)], ["telefon", { ulkeKodu: "+999", numara: "1234" }], ["sosyal", [{ platform: "myspace", deger: "a" }]]]) {
    const bad = await t.req("/v1/profil", { method: "POST", token: tok, body: { profil: { ...profil, [k]: v } } });
    assert.equal(bad.status, 400, k);
  }
  const din = await t.req("/v1/profil", { method: "POST", token: tok, body: { profil: { ...profil, din: "x" } } });
  assert.equal(din.status, 200);
  h = await t.req("/v1/hesap", { token: tok });
  assert.equal(h.json.profil.din, undefined, "listede olmayan alan kaydedilmez");
});

test("fotoğraf: JPEG, PNG ya da WebP gelir; sunucu 512 piksel kare WebP'ye çevirir, üst veri düşer; kaldırılabilir", async () => {
  const post = (foto) => t.req("/v1/profil/foto", { method: "POST", token: tok, body: { foto } });
  assert.equal((await post("data:image/png;base64,AAAA")).status, 400, "bozuk görüntü");
  assert.equal((await post("data:image/gif;base64,R0lGODlhAQABAAAAACw=")).status, 400, "desteklenmeyen tür");
  assert.equal((await post((await testPhoto("jpeg")).replace("image/jpeg", "image/png"))).status, 400, "tür başlıkla uyuşmuyor");
  const { default: sharp } = await import("sharp");
  for (const fmt of ["jpeg", "png", "webp"]) {
    const ok = await post(await testPhoto(fmt, 1200, 800, fmt === "jpeg"));
    assert.equal(ok.status, 200, ok.text);
    assert.match(ok.json.foto, /^data:image\/webp;base64,/);
    const buf = Buffer.from(ok.json.foto.split(",")[1], "base64");
    assert.ok(buf.length <= 200 * 1024);
    const meta = await sharp(buf).metadata();
    assert.equal(meta.format, "webp");
    assert.deepEqual([meta.width, meta.height], [512, 512]);
    assert.equal(meta.exif, undefined, "EXIF düşer");
    assert.ok(!buf.includes("gizli-iz"));
  }
  const small = await post(await testPhoto("png", 200, 300));
  const sm = await sharp(Buffer.from(small.json.foto.split(",")[1], "base64")).metadata();
  assert.deepEqual([sm.width, sm.height], [200, 200], "küçük görüntü büyütülmez");
  assert.equal((await t.req("/v1/hesap", { token: tok })).json.profil.foto, small.json.foto);
  const s = (await t.req("/api/auth/get-session", { token: tok })).json;
  assert.equal(s.user.photo, undefined, "fotoğraf oturum yanıtına girmez");
  assert.equal((await post(null)).status, 200);
  assert.equal((await t.req("/v1/hesap", { token: tok })).json.profil.foto, null);
});

test("kutlama tercihi ve girişsiz kapatma bağlantısı", async () => {
  await t.req("/v1/kutlama", { method: "POST", token: tok, body: { acik: false } });
  assert.equal((await t.req("/v1/hesap", { token: tok })).json.kutlama, false);
  assert.deepEqual(t.center.find("guncelle").at(-1).body.degisiklik, { kutlama: false });
  await t.req("/v1/kutlama", { method: "POST", token: tok, body: { acik: true } });
  // Doğum günü bugünse kutlama gider; bağlantı girişsiz kapatır
  const u = (await t.q(`select * from "user" where email = 'profil@example.com'`)).rows[0];
  const now = new Date();
  const next = nextNine(now, "UTC");
  await t.q(`update "user" set profile = jsonb_set(profile::jsonb, '{dogumTarihi}', to_jsonb($1::text))::json where id = $2`, ["2000-" + next.day.slice(5), u.id]);
  const r = await runCelebrations({ pool: t.app.pool, mailer: t.app.mailer, now });
  assert.equal(r.scheduled, 1);
  const m = t.last("celebrate", "profil@example.com");
  assert.equal(m.vars.kind, "birthday");
  assert.match(m.subject, /İyi ki doğdunuz/);
  assert.match(m.text, /Merhaba Ayşe,/);
  assert.equal(new Date(m.scheduledAt).toISOString(), next.at.toISOString(), "yerel 09:00'a zamanlanır");
  assert.equal((await runCelebrations({ pool: t.app.pool, mailer: t.app.mailer, now })).scheduled, 0, "aynı gün ikinci kez bırakılmaz");
  const url = new URL(m.unsubUrl);
  const post = await t.req(url.pathname + url.search, { method: "POST" });
  assert.ok(post.status < 500);
  assert.equal((await t.q(`select celebrate from "user" where id = $1`, [u.id])).rows[0].celebrate, false);
  const forged = url.search.replace(/k=[^&]+/, "k=YWJj.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
  await t.q(`update "user" set celebrate = true where id = $1`, [u.id]);
  await t.req(url.pathname + forged, { method: "POST" });
  assert.equal((await t.q(`select celebrate from "user" where id = $1`, [u.id])).rows[0].celebrate, true, "sahte anahtar işe yaramaz");
});

test("kutlama günleri: Türkiye bayramları, öbür ülkede anma ve yas günü yok, yılbaşı, yıl dönümü", () => {
  const base = { createdAt: "2025-03-10T12:00:00Z", timeZone: "Europe/Istanbul" };
  const tr = { ...base, profile: { ulke: "TR" } };
  assert.deepEqual(occasionFor(tr, "2027-03-09", "tr"), { kind: "holiday", name: "Ramazan Bayramı" });
  assert.deepEqual(occasionFor(tr, "2026-10-29", "en"), { kind: "holiday", name: "Republic Day" });
  assert.equal(occasionFor(tr, "2026-07-15", "tr"), null, "15 Temmuz anma günü kutlanmaz");
  assert.deepEqual(occasionFor(tr, "2027-01-01", "tr"), { kind: "newyear" });
  assert.deepEqual(occasionFor(tr, "2027-03-10", "tr"), { kind: "anniversary", years: 2 });
  const us = { ...base, profile: { ulke: "US" } };
  assert.equal(occasionFor(us, "2026-05-25", "en"), null, "Memorial Day");
  assert.equal(occasionFor(us, "2026-07-04", "en").kind, "holiday");
  const de = { ...base, profile: { ulke: "DE" } };
  assert.equal(occasionFor(de, "2026-04-03", "de"), null, "Karfreitag");
  assert.equal(occasionFor({ ...base, profile: {} }, "2026-07-04", "en"), null, "ülke yoksa bayram yok");
  const leap = { ...base, profile: { dogumTarihi: "2004-02-29" } };
  assert.deepEqual(occasionFor(leap, "2027-02-28", "tr"), { kind: "birthday" });
});

test("kayıtlar: son yazan kazanır, silme, sınırlar", async () => {
  const put = (degisiklik) => t.req("/v1/kayitlar", { method: "POST", token: tok, body: { degisiklik } });
  assert.equal((await put({ "zikir.total": { v: "33", t: 1000 }, "pomo.log": { v: "[]", t: 1000 } })).status, 200);
  assert.equal((await put({ "zikir.total": { v: "10", t: 500 } })).status, 200);
  let g = await t.req("/v1/kayitlar", { token: tok });
  assert.deepEqual(g.json.kayitlar["zikir.total"], { v: "33", t: 1000 }, "eski yazım yeniyi ezmez");
  await put({ "pomo.log": { v: null, t: 2000 } });
  g = await t.req("/v1/kayitlar", { token: tok });
  assert.equal(g.json.kayitlar["pomo.log"], undefined);
  assert.equal((await t.req("/v1/kayitlar?since=1000", { token: tok })).json.kayitlar["zikir.total"], undefined);
  assert.equal((await put({ "kötü anahtar": { v: "1", t: 1 } })).status, 400);
  assert.equal((await put({ büyük: { v: "x".repeat(257 * 1024), t: 1 } })).status, 400);
});

test("rozetler: yalnız bilinen kimlik ve geçmiş gün; erken gün kalır; silinebilir", async () => {
  const add = (rozetler) => t.req("/v1/rozetler", { method: "POST", token: tok, body: { rozetler } });
  assert.equal((await add([{ id: "renk", gun: "2026-11-05" }, { id: "saniye", gun: "2026-11-06" }])).status, 200);
  assert.equal((await add([{ id: "renk", gun: "2026-11-01" }])).status, 200);
  assert.equal((await add([{ id: "namaz", gun: "2026-11-01" }])).status, 400);
  assert.equal((await add([{ id: "renk", gun: "2099-01-01" }])).status, 400);
  const g = await t.req("/v1/rozetler", { token: tok });
  assert.match(g.headers.get("cache-control"), /private, no-store/);
  assert.deepEqual(g.json.rozetler, [{ id: "renk", gun: "2026-11-01" }, { id: "saniye", gun: "2026-11-06" }]);
});

test("verilerimi indir: profil, fotoğraf, onay, oturumlar (IP yok), kayıtlar, rozetler", async () => {
  const foto = (await t.req("/v1/profil/foto", { method: "POST", token: tok, body: { foto: await testPhoto() } })).json.foto;
  const r = await t.req("/v1/verilerim", { token: tok });
  assert.equal(r.status, 200);
  assert.match(r.headers.get("content-disposition"), /attachment/);
  assert.equal(r.json.hesap.eposta, "profil@example.com");
  assert.equal(r.json.hesap.profil.foto, foto);
  assert.equal(r.json.onay.surum, "2026-11-22");
  assert.ok(r.json.oturumlar.length >= 1);
  assert.equal(r.json.oturumlar[0].ipAddress, undefined);
  assert.equal(r.json.kayitlar["zikir.total"].v, "33");
  assert.equal(r.json.rozetler.length, 2);
  assert.equal(JSON.stringify(r.json).includes("password"), false);
});

test("ortak servisin ürün uçları: anahtar, dogrula, hesap-getir, guncelle", async () => {
  assert.equal((await center("hesap-getir", { uyeNo: 10458 }, "yanlis")).status, 401);
  assert.equal((await t.req("/api/bumba-uyelik/hesap-getir", { method: "POST", body: { uyeNo: 10458 } })).status, 401);
  assert.equal((await center("hesap-getir", { uyeNo: 99999 })).status, 404);
  const g = await center("hesap-getir", { uyeNo: 10458 });
  assert.equal(g.status, 200);
  const h = g.json.hesap;
  assert.equal(h.eposta, "profil@example.com");
  assert.equal(h.epostaDogrulandi, true);
  assert.match(h.sifreOzeti, /^[0-9a-f]+:[0-9a-f]+$/);
  assert.match(h.profil.foto, /^data:image\/webp;base64,/);
  assert.equal(h.kutlama, true);
  assert.equal(h.onay.surum, "2026-11-22");
  assert.equal(h.anaUrun, true);
  assert.deepEqual((await center("dogrula", { uyeNo: 10458, eposta: "profil@example.com", sifre: "Yanlis-1a" })).json, { gecerli: false });
  const ok = await center("dogrula", { uyeNo: 10458, eposta: "profil@example.com", sifre: PW });
  assert.equal(ok.json.gecerli, true);
  assert.equal(ok.json.hesap.uyeNo, 10458);
  // Başka üründe değişen profil ve şifre
  const other = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "profil@example.com", password: PW } });
  const newHash = (await import("better-auth/crypto")).hashPassword;
  const hash = await newHash("Baska-urun1");
  const u = await center("guncelle", { uyeNo: 10458, degisiklik: { profil: { ...h.profil, sehir: "Ankara" }, sifreOzeti: hash, kutlama: false } });
  assert.equal(u.status, 200, u.text);
  assert.equal((await t.req("/v1/hesap", { token: other.json.token })).status, 401, "şifre değişince oturumlar kapanır");
  const tok2 = await tokenOf("profil@example.com", "Baska-urun1");
  const hh = await t.req("/v1/hesap", { token: tok2 });
  assert.equal(hh.json.profil.sehir, "Ankara");
  assert.equal(hh.json.kutlama, false);
  tok = tok2;
});

test("askı ve yasak: giriş engeli, e-postalar, yeniden açma; yasakta veri silinir", async () => {
  const s = await center("durum", { uyeNo: 10458, durum: "askida", sebep: "Kurallara aykırı içerik", bitis: "2027-01-01T00:00:00Z", bildir: true });
  assert.equal(s.status, 200, s.text);
  const m = t.last("suspended", "profil@example.com");
  assert.match(m.text, /Kurallara aykırı içerik/);
  assert.match(m.text, /bilgi@bumbagroup\.com/);
  assert.match(m.text, /Askı .* tarihinde kendiliğinden kalkacak/);
  assert.equal((await t.req("/v1/hesap", { token: tok })).status, 401, "oturumlar kapandı");
  const li = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "profil@example.com", password: "Baska-urun1" } });
  assert.equal(li.status, 403);
  assert.equal(li.json.code, "BANNED_USER");
  assert.equal((await center("dogrula", { uyeNo: 10458, eposta: "profil@example.com", sifre: "Baska-urun1" })).json.gecerli, false);
  await center("durum", { uyeNo: 10458, durum: "aktif", bildir: true });
  assert.ok(t.last("reopened", "profil@example.com"));
  tok = await tokenOf("profil@example.com", "Baska-urun1");
  assert.ok(tok);
  assert.equal((await center("durum", { uyeNo: 10458, durum: "yasakli" })).status, 400, "sebep zorunlu");
  await center("durum", { uyeNo: 10458, durum: "yasakli", sebep: "Ağır ihlal", bildir: true });
  assert.ok(t.last("banned", "profil@example.com"));
  const row = (await t.q(`select email, "uyeNo", "banReason", profile, photo from "user" where "uyeNo" = 10458`)).rows[0];
  assert.equal(row.email, "profil@example.com");
  assert.equal(row.banReason, "Ağır ihlal");
  assert.equal(row.profile, null);
  assert.equal(row.photo, null);
  assert.equal((await t.q(`select count(*)::int n from account_record r join "user" u on u.id = r.user_id where u."uyeNo" = 10458`)).rows[0].n, 0);
  assert.equal((await t.q(`select count(*)::int n from user_badge b join "user" u on u.id = b.user_id where u."uyeNo" = 10458`)).rows[0].n, 0);
  assert.equal((await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "profil@example.com", password: "Baska-urun1" } })).status >= 400, true);
});

test("hesap-ac, numara-degistir ve hesap-sil", async () => {
  const hesap = { uyeNo: 20001, eposta: "acilan@example.com", epostaDogrulandi: true, sifreOzeti: await (await import("better-auth/crypto")).hashPassword("Acilan-sifre1"), profil: { ad: "Can", ulke: "TR", gorunurluk: { acik: false, alanlar: [] } }, kutlama: true, onay: { zaman: "2026-11-01T10:00:00Z", surum: "2026-11-11" }, anaUrun: false };
  const a = await center("hesap-ac", { hesap });
  assert.deepEqual(a.json, { sonuc: "acildi" });
  assert.ok(t.last("linkedOpened", "acilan@example.com"));
  assert.equal(t.last("linkedOpened", "acilan@example.com").lang, "tr");
  assert.deepEqual((await center("hesap-ac", { hesap })).json, { sonuc: "var" });
  const tk = await tokenOf("acilan@example.com", "Acilan-sifre1");
  assert.ok(tk, "aynı şifreyle giriş");
  const h = (await t.req("/v1/hesap", { token: tk })).json;
  assert.equal(h.uyeNo, 20001);
  assert.equal(h.profil.ad, "Can");
  const n = await center("numara-degistir", { eskiNo: 20001, yeniNo: 10003, hesap: { profil: { ad: "Can", soyad: "Y" } } });
  assert.equal(n.status, 200, n.text);
  assert.equal((await center("hesap-getir", { uyeNo: 10003 })).json.hesap.profil.soyad, "Y");
  const d = await center("hesap-sil", { uyeNo: 10003, bildir: true });
  assert.equal(d.status, 200);
  assert.ok(t.last("linkedClosed", "acilan@example.com"));
  assert.equal((await t.q(`select count(*)::int n from "user" where email = 'acilan@example.com'`)).rows[0].n, 0);
});

test("kayıt sorusunda Evet: ortak servis şifreyi doğrular, hesap burada açılır, oturum açılır", async () => {
  t.center.others.set("evet@example.com", { sifre: "Gander-sifre1", hesap: { uyeNo: 10077, eposta: "evet@example.com", epostaDogrulandi: true, sifreOzeti: await (await import("better-auth/crypto")).hashPassword("Gander-sifre1"), profil: { ad: "Deniz", gorunurluk: { acik: false, alanlar: [] } }, kutlama: true, onay: { zaman: "2026-11-01T10:00:00Z", surum: "2026-11-11" }, anaUrun: true } });
  const bad = await t.req("/v1/uyelik/bagla", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { eposta: "evet@example.com", sifre: "Yanlis-1a", dil: "tr" } });
  assert.equal(bad.status, 401);
  const r = await t.req("/v1/uyelik/bagla", { method: "POST", headers: { "x-saat-istemci": "ios" }, body: { eposta: "evet@example.com", sifre: "Gander-sifre1", dil: "tr" } });
  assert.equal(r.status, 200, r.text);
  assert.ok(r.json.token);
  const h = (await t.req("/v1/hesap", { token: r.json.token })).json;
  assert.equal(h.uyeNo, 10077, "numara aynı kalır");
  assert.equal(h.profil.ad, "Deniz");
  const u = (await t.q(`select "anaUrun", lang from "user" where email = 'evet@example.com'`)).rows[0];
  assert.equal(u.anaUrun, false, "kutlamaları ana ürün gönderir");
  assert.equal(u.lang, "tr");
});

test("ayrı hesaplar girişte sormadan birleşir; şifre değişmez, seçim ekrandan ya da e-postadaki bağlantıdan; ürün listesi, aç ve kapat", async () => {
  const tk = await signUpVerified(t, "ayri@example.com");
  // Ayrı hesap yokken birleşme olmaz, seçim de beklenmez
  const d0 = (await t.req("/v1/uyelik/durum", { method: "POST", token: tk, body: {} })).json;
  assert.equal(d0.birlesti, false);
  assert.deepEqual(d0.sifreSecimi, []);
  t.center.ayriHesap.add("ayri@example.com");
  t.center.merge.set("ayri@example.com", { uyeNo: 10005, eposta: "ayri@example.com", profil: { ad: "Eski", sehir: null } });
  const d = await t.req("/v1/uyelik/durum", { method: "POST", token: tk, body: {} });
  assert.equal(d.status, 200, d.text);
  assert.equal(d.json.birlesti, true);
  assert.equal(d.json.uyeNo, 10005, "en eski numara kalır");
  assert.deepEqual(d.json.sifreSecimi.map((x) => x.site), ["saat-tarih", "gander"]);
  assert.equal(t.center.find("birlestir").at(-1).body.sifre, undefined, "birleşme şifre istemez");
  const mail = t.last("merged", "ayri@example.com");
  assert.ok(mail, "hesaplarınız birleştirildi e-postası");
  assert.match(mail.text, /Saat Tarih ve Gander/);
  assert.match(mail.text, /10005/);
  const link = new URL(mail.vars.url);
  assert.equal(link.pathname, "/sifre-sec");
  // Şifre değişmez: eski şifre geçerli, profil birleşmiş profil
  const tk2 = await tokenOf("ayri@example.com");
  assert.ok(tk2, "kendi şifresi geçerli kalır");
  assert.equal((await t.req("/v1/hesap", { token: tk2 })).json.profil.ad, "Eski");
  // Seçim yapılana kadar her girişte yine çıkar (birleşme yeniden olmaz)
  const d2 = await t.req("/v1/uyelik/durum", { method: "POST", token: tk2, body: {} });
  assert.equal(d2.json.birlesti, false);
  assert.equal(d2.json.sifreSecimi.length, 2);
  // E-postadaki bağlantı: oturum açmadan seçim sayfası
  const page = await t.req(link.pathname + link.search);
  assert.equal(page.status, 200);
  assert.match(page.text, /name="site" value="gander"/);
  assert.equal(page.headers.getSetCookie().length, 0, "bağlantı oturum açmaz");
  assert.equal((await t.req(`/sifre-sec?dil=tr&k=bozuk`)).status, 200);
  // Ekrandan seçim; ikinci seçim yok
  assert.equal((await t.req("/v1/uyelik/sifre-sec", { method: "POST", token: tk2, body: { site: "gander" } })).status, 200);
  assert.equal((await t.req("/v1/uyelik/sifre-sec", { method: "POST", token: tk2, body: { site: "gander" } })).status, 404);
  assert.deepEqual((await t.req("/v1/uyelik/durum", { method: "POST", token: tk2, body: {} })).json.sifreSecimi, []);
  // Seçim yapıldıktan sonra bağlantı geçersiz
  const after = await t.req(link.pathname + link.search);
  assert.doesNotMatch(after.text, /name="site"/);
  const list = await t.req("/v1/uyelik/urunler", { token: tk2 });
  assert.equal(list.json.urunler.length, 2);
  assert.equal((await t.req("/v1/uyelik/ac", { method: "POST", token: tk2, body: { hedefler: ["gander"], sifre: "Yanlis-1a" } })).status, 401, "açmadan önce şifre");
  const ac = await t.req("/v1/uyelik/ac", { method: "POST", token: tk2, body: { hedefler: ["gander"], sifre: PW } });
  assert.deepEqual(ac.json, { sonuclar: { gander: "acildi" } });
  assert.match(t.center.find("ac").at(-1).body.hesap.sifreOzeti, /:/);
  assert.equal((await t.req("/v1/uyelik/kapat", { method: "POST", token: tk2, body: { hedefler: ["gander"] } })).status, 404, "kapatma yok (5.21)");
});

test("hesabı sil (5.21 silme onayı): şifre ya da taze oturum, e-postadaki bağlantı, açmak silmez, tek kullanımlık, iptal", async () => {
  const a = await signUpVerified(t, "sil1@example.com");
  const r1 = await t.req("/v1/hesap/sil", { method: "POST", token: a, body: { sifre: "Yanlis-1a" } });
  assert.equal(r1.status, 401);
  const ok = await t.req("/v1/hesap/sil", { method: "POST", token: a, body: {} });
  assert.equal(ok.status, 200, "son 24 saatte açılmış oturum yeter");
  assert.equal(ok.json.bekliyor, true);
  assert.equal(ok.json.istek.tur, "hesap");
  assert.equal((await t.req("/v1/hesap", { token: a })).json.silmeIstegi.tur, "hesap", "hesap sayfasında bekleyen istek");
  const mail = t.last("deleteAsk", "sil1@example.com");
  assert.ok(mail && !mail.unsubUrl, "işlem e-postası, kapatma bağlantısı yok");
  const u = new URL(mail.vars.url);
  const k = u.searchParams.get("k");
  assert.equal((await t.q(`select count(*)::int n from delete_request where token_hash = $1`, [k])).rows[0].n, 0, "anahtar düz yazılmaz");
  const page = await t.req(u.pathname + u.search);
  assert.equal(page.status, 200);
  assert.match(page.text, /method="post"/);
  assert.equal((await t.q(`select count(*)::int n from "user" where email = 'sil1@example.com'`)).rows[0].n, 1, "bağlantıyı açmak silmez");
  assert.equal(t.center.find("ayril").length, 0);
  const done = await t.req("/silme-onay?dil=tr", { method: "POST", form: { k } });
  assert.equal(done.status, 200);
  assert.ok(t.center.find("ayril").at(-1).body.uyeNo > 0);
  assert.equal(t.center.find("ayril").at(-1).body.neden, undefined, "üyenin kendi isteğinde neden gönderilmez (5.28)");
  assert.equal(t.center.find("sil").length, 0, "merkezin sil ucu kullanılmaz");
  assert.ok(t.last("deleted", "sil1@example.com"));
  assert.equal((await t.q(`select count(*)::int n from "user" where email = 'sil1@example.com'`)).rows[0].n, 0);
  const again = await t.req("/silme-onay?dil=tr", { method: "POST", form: { k } });
  assert.match(again.text, /msg err/, "tek kullanımlık");

  const b = await signUpVerified(t, "sil2@example.com");
  await t.q(`update session set "createdAt" = now() - interval '2 days' where token is not null`);
  assert.equal((await t.req("/v1/hesap/sil", { method: "POST", token: b, body: {} })).status, 401, "eski oturumda şifre gerekir");
  assert.equal((await t.req("/v1/hesap/sil", { method: "POST", token: b, body: { sifre: PW } })).status, 200);
  const k1 = new URL(t.last("deleteAsk", "sil2@example.com").vars.url).searchParams.get("k");
  assert.equal((await t.req("/v1/hesap/sil", { method: "POST", token: b, body: { sifre: PW } })).status, 200, "yeni istek eskisinin yerine");
  assert.match((await t.req("/silme-onay?dil=tr", { method: "POST", form: { k: k1 } })).text, /msg err/, "eski bağlantı geçersiz");
  assert.equal((await t.req("/v1/silme-istegi/iptal", { method: "POST", token: b, body: {} })).json.iptal, true);
  assert.equal((await t.req("/v1/silme-istegi", { token: b })).json.istek, null);
  const k2 = new URL(t.last("deleteAsk", "sil2@example.com").vars.url).searchParams.get("k");
  assert.match((await t.req("/silme-onay?dil=tr", { method: "POST", form: { k: k2 } })).text, /msg err/, "iptalden sonra bağlantı geçersiz");
  assert.equal((await t.q(`select count(*)::int n from "user" where email = 'sil2@example.com'`)).rows[0].n, 1);

  // Rozetleri silme de onaylı; 7 gün dolunca temizlik isteği siler
  await t.req("/v1/rozetler", { method: "POST", token: b, body: { rozetler: [{ id: "renk", gun: "2026-11-01" }] } });
  assert.equal((await t.req("/v1/rozetler/sil", { method: "POST", token: b, body: { sifre: PW } })).json.istek.tur, "rozetler");
  const rk = new URL(t.last("dataDeleteAsk", "sil2@example.com").vars.url).searchParams.get("k");
  assert.equal((await t.req("/v1/rozetler", { token: b })).json.rozetler.length, 1, "onaydan önce silinmez");
  await t.req("/silme-onay?dil=tr", { method: "POST", form: { k: rk } });
  assert.equal((await t.req("/v1/rozetler", { token: b })).json.rozetler.length, 0);
  assert.ok(t.last("dataDeleted", "sil2@example.com"));
  await t.req("/v1/hesap/sil", { method: "POST", token: b, body: { sifre: PW } });
  await t.q(`update delete_request set expires_at = now() - interval '1 minute'`);
  const c = await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${CRON}` } });
  assert.equal(c.json.silmeIstekleri, 1);
});

test("temizlik: doğrulanmamış hesap 7 günde, eksik numara alınır, merkeze iletilemeyenler yeniden denenir", async () => {
  const pool = t.app.pool;
  assert.equal((await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: "Bearer yanlis" } })).status, 401);
  await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "dogrulanmamis@example.com", password: PW, name: "", lang: "tr" } });
  const DAY = 86400_000;
  let r = await runCleanup({ pool, center: t.app.center, relay: t.app.relay, now: new Date(Date.now() + 6 * DAY) });
  assert.equal(r.unverified, 0);
  r = await runCleanup({ pool, center: t.app.center, relay: t.app.relay, now: new Date(Date.now() + 8 * DAY) });
  assert.ok(r.unverified >= 1);
  assert.equal((await t.q(`select count(*)::int n from "user" where email = 'dogrulanmamis@example.com'`)).rows[0].n, 0);

  // Numara (kayit) alınamazsa hesap yine açılır; numara temizlikte alınır
  t.center.downOps.add("kayit");
  const tk = await signUpVerified(t, "numarasiz@example.com");
  assert.equal((await t.q(`select "uyeNo" from "user" where email = 'numarasiz@example.com'`)).rows[0].uyeNo, null, "numara alınamazsa hesap yine açılır");
  t.center.downOps.delete("kayit");
  let g = await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${CRON}` } });
  assert.equal(g.status, 200);
  assert.equal(g.json.numbers, 1);
  assert.ok((await t.q(`select "uyeNo" from "user" where email = 'numarasiz@example.com'`)).rows[0].uyeNo != null);
  // Merkeze iletilemeyen guncelle bırakılmaz; 1, 2, 4… dakika arayla denenir (5.22)
  t.center.down = true;
  await t.req("/v1/kutlama", { method: "POST", token: tk, body: { acik: false } });
  await t.req("/v1/kutlama", { method: "POST", token: tk, body: { acik: true } });
  assert.equal((await t.q(`select count(*)::int n from center_retry`)).rows[0].n, 1, "aynı üyenin guncelle'leri birleşir");
  t.center.down = false;
  g = await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${CRON}` } });
  assert.equal(g.json.retried, 0, "zamanı gelmeden denenmez");
  await t.q(`update center_retry set next_at = now() - interval '1 second'`);
  const before = t.center.find("guncelle").length;
  g = await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${CRON}` } });
  assert.equal(g.json.retried, 1);
  const sentNow = t.center.find("guncelle").slice(before);
  assert.equal(sentNow.length, 1);
  assert.equal(sentNow[0].body.degisiklik.kutlama, true, "hesabın o anki hâli gider");
  const k = await t.req("/gorev/kutlama", { method: "POST", headers: { authorization: `Bearer ${CRON}` } });
  assert.equal(k.status, 200);
});

test("ayar eksikse üyelik kapalı: uçlar 503, sağlık açık", async () => {
  const cfg = loadConfig({ MAIL_DRIVER: "memory", BETTER_AUTH_URL: "http://127.0.0.1" });
  assert.equal(cfg.open, false);
  assert.ok(cfg.missing.includes("DATABASE_URL") && cfg.missing.includes("UYELIK_ANAHTARI"));
  const app = await createApp(cfg);
  await new Promise((r) => app.server.listen(0, "127.0.0.1", r));
  const base = `http://127.0.0.1:${app.server.address().port}`;
  assert.equal((await fetch(base + "/api/auth/get-session")).status, 503);
  assert.equal((await fetch(base + "/gorev/temizlik", { method: "POST" })).status, 503);
  assert.equal((await fetch(base + "/saglik")).status, 200);
  await app.close();
});
