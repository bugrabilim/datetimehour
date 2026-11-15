/* Üyelik aşama 2d: ortak gruplar (geri sayım, dünya saati, toplantı oylaması, odak odası, alarm isteği, gözlem) ve
   anonim hava bildirimi. Kapalıyken 404, kod ile katılma, türe göre işler, bildir/engelle, hesap silmede devir,
   verilerimi indir, temizlik. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, CRON } from "./helpers.mjs";
import { cellOf, validTz } from "../src/groups.mjs";

let t;
const tok = {};
before(async () => {
  t = await start({ GRUPLAR: "acik" });
  for (const n of ["ayse", "ali", "can", "deniz"]) tok[n] = await signUpVerified(t, `${n}@example.com`);
});
after(async () => { await t.stop(); });

const api = (who, path, body) => t.req(path, { method: body === undefined ? "GET" : "POST", token: tok[who], body });
const make = async (who, b) => {
  const r = await api(who, "/v1/grup", b);
  assert.equal(r.status, 200, JSON.stringify(r.json));
  return r.json;
};

test("yardımcılar: saat dilimi ve 0,25° hücre", () => {
  assert.ok(validTz("Europe/Istanbul") && validTz("America/Argentina/Buenos_Aires") && validTz("UTC"));
  assert.ok(!validTz("Mars/Olympus") && !validTz("../etc") && !validTz(""));
  assert.equal(cellOf(41.0151, 28.9795), "41.00:28.75");
  assert.equal(cellOf(-33.87, 151.21), "-34.00:151.00");
  assert.equal(cellOf(91, 0), null);
});

test("ortak geri sayım: kurucu düzenler, üyeler görür; süzgeç ve tür denetimi", async () => {
  assert.equal((await api("ayse", "/v1/grup", { tur: "sohbet", ad: "x" })).json.hata, "tur");
  assert.equal((await api("ayse", "/v1/grup", { tur: "geri-sayim", ad: "www.x.com", hedef: Date.UTC(2027, 0, 1) })).json.hata, "ad-icerik");
  assert.equal((await api("ayse", "/v1/grup", { tur: "geri-sayim", ad: "Tatil" })).json.hata, "hedef");
  const g = await make("ayse", { tur: "geri-sayim", ad: "Ailece tatil", hedef: Date.UTC(2027, 6, 1, 9), simge: "🏖️" });
  assert.equal(g.simge, "🏖️");
  assert.equal(g.yillik, false);
  let r = await api("ali", "/v1/grup/onizle", { kod: g.kod });
  assert.deepEqual(r.json, { tur: "geri-sayim", ad: "Ailece tatil", uyeSayisi: 1, dolu: false });
  r = await api("ali", "/v1/grup/katil", { kod: g.kod });
  assert.equal(r.json.hedef, Date.UTC(2027, 6, 1, 9));
  assert.equal((await api("ali", `/v1/grup/${g.id}/duzenle`, { hedef: Date.UTC(2027, 7, 1) })).json.hata, "sahip-degil");
  r = await api("ayse", `/v1/grup/${g.id}/duzenle`, { hedef: Date.UTC(2027, 7, 1), yillik: true });
  assert.equal(r.json.yillik, true);
  r = await api("ali", "/v1/gruplar");
  assert.equal(r.json.gruplar[0].hedef, Date.UTC(2027, 7, 1));
  assert.equal((await api("can", `/v1/grup/${g.id}`)).status, 404, "üye olmayan göremez");
});

test("ortak dünya saati: herkes kendi şehrini ve mesai saatini yazar", async () => {
  const g = await make("ayse", { tur: "dunya", ad: "Ekibim" });
  await api("can", "/v1/grup/katil", { kod: g.kod });
  await api("ayse", `/v1/grup/${g.id}/yerim`, { sehir: "İstanbul", tz: "Europe/Istanbul", mesaiBas: 9, mesaiSon: 18 });
  assert.equal((await api("can", `/v1/grup/${g.id}/yerim`, { sehir: "Tokyo", tz: "Asia/Nowhere" })).json.hata, "tz");
  assert.equal((await api("can", `/v1/grup/${g.id}/yerim`, { sehir: "a@b.co", tz: "Asia/Tokyo" })).json.hata, "sehir");
  const r = await api("can", `/v1/grup/${g.id}/yerim`, { sehir: "Tokyo", tz: "Asia/Tokyo" });
  assert.deepEqual(r.json.uyeler.map((u) => [u.sehir, u.tz, u.mesai]), [["İstanbul", "Europe/Istanbul", [9, 18]], ["Tokyo", "Asia/Tokyo", null]]);
});

test("toplantı oylaması: oylar toplanır, en uygun saat; saatler değişince oylar sıfırlanır; kapatma", async () => {
  const s = [29_000_000, 29_000_060, 29_000_120];
  assert.equal((await api("ali", "/v1/grup", { tur: "oylama", ad: "Proje", saatler: [] })).json.hata, "saatler");
  const g = await make("ali", { tur: "oylama", ad: "Proje toplantısı", saatler: s, sure: 45 });
  await api("deniz", "/v1/grup/katil", { kod: g.kod });
  await api("ali", `/v1/grup/${g.id}/oy`, { oylar: [2, 1, 0] });
  assert.equal((await api("deniz", `/v1/grup/${g.id}/oy`, { oylar: [2, 3, 0] })).json.hata, "oy");
  let r = await api("deniz", `/v1/grup/${g.id}/oy`, { oylar: [1, 2, 2] });
  assert.deepEqual(r.json.sonuc, [{ uygun: 1, olabilir: 1 }, { uygun: 1, olabilir: 1 }, { uygun: 1, olabilir: 0 }]);
  assert.equal(r.json.enIyi, 0);
  r = await api("ali", `/v1/grup/${g.id}/duzenle`, { saatler: [...s, 29_000_180] });
  assert.ok(r.json.uyeler.every((u) => u.oy === null), "saatler değişince eski oylar sayılmaz");
  r = await api("ali", `/v1/grup/${g.id}/duzenle`, { kapali: true });
  assert.equal(r.json.kapali, true);
  assert.equal((await api("deniz", `/v1/grup/${g.id}/oy`, { oylar: [2, 2, 2, 2] })).json.hata, "bitti");
});

test("odak odası: başlat, katıl, bitirdim; süren oturum varken yenisi başlamaz", async () => {
  const g = await make("can", { tur: "odak", ad: "Ders çalışma" });
  await api("deniz", "/v1/grup/katil", { kod: g.kod });
  assert.equal((await api("deniz", `/v1/grup/${g.id}/odak`, { islem: "katil" })).json.hata, "oturum-yok");
  let r = await api("can", `/v1/grup/${g.id}/odak`, { islem: "baslat", dk: 25 });
  assert.equal(r.json.oturum.dk, 25);
  assert.equal((await api("deniz", `/v1/grup/${g.id}/odak`, { islem: "baslat", dk: 25 })).json.hata, "suruyor");
  await api("deniz", `/v1/grup/${g.id}/odak`, { islem: "katil" });
  r = await api("can", `/v1/grup/${g.id}/odak`, { islem: "bitti" });
  assert.deepEqual(r.json.uyeler.map((u) => [u.katildi, u.bitti]), [[true, true], [true, false]]);
  r = await api("deniz", "/v1/gruplar");
  assert.ok(r.json.gruplar.find((x) => x.id === g.id).oturum);
});

test("gözlem: izledim, kaldır, olay başına toplam", async () => {
  const g = await make("ayse", { tur: "gozlem", ad: "Gökyüzü" });
  await api("deniz", "/v1/grup/katil", { kod: g.kod });
  await api("ayse", `/v1/grup/${g.id}/izledim`, { olay: "2026-12-14-geminid" });
  await api("deniz", `/v1/grup/${g.id}/izledim`, { olay: "2026-12-14-geminid" });
  let r = await api("deniz", `/v1/grup/${g.id}/izledim`, { olay: "dolunay-2026-11-24" });
  assert.deepEqual(r.json.sayilar, { "2026-12-14-geminid": 2, "dolunay-2026-11-24": 1 });
  assert.deepEqual(r.json.benim, ["2026-12-14-geminid", "dolunay-2026-11-24"]);
  assert.equal((await api("deniz", `/v1/grup/${g.id}/izledim`, { olay: "<script>" })).json.hata, "olay");
  r = await api("deniz", `/v1/grup/${g.id}/izledim`, { olay: "dolunay-2026-11-24", kaldir: true });
  assert.deepEqual(r.json.sayilar, { "2026-12-14-geminid": 2 });
});

test("alarm isteği: yalnız grup üyesine, onay ya da ret, günde 5, engellenen öneremez, etiket bildirilince kalkar", async () => {
  const g = await make("ali", { tur: "alarm", ad: "Ev" });
  let r = await api("ayse", "/v1/grup/katil", { kod: g.kod });
  const ayseMid = r.json.uyeler.find((u) => u.sen).id;
  const aliMid = r.json.uyeler.find((u) => u.sahip).id;
  assert.equal((await api("ali", `/v1/grup/${g.id}/alarm`, { uye: ayseMid, saat: "25:00" })).json.hata, "saat");
  r = await api("ali", `/v1/grup/${g.id}/alarm`, { uye: ayseMid, saat: "06:30", gunler: 31, etiket: "Okul" });
  assert.equal(r.json.istekler[0].gelen, false);
  r = await api("ayse", "/v1/gruplar");
  assert.equal(r.json.gruplar.find((x) => x.id === g.id).bekleyen, 1);
  r = await api("ayse", `/v1/grup/${g.id}`);
  const req = r.json.istekler[0];
  assert.deepEqual([req.gelen, req.kimden, req.saat, req.gunler, req.etiket, req.durum], [true, aliMid, "06:30", 31, "Okul", "bekliyor"]);
  r = await api("ayse", `/v1/grup/${g.id}/alarm-yanit`, { istek: req.id, kabul: true });
  assert.equal(r.json.istekler[0].durum, "kuruldu");
  assert.equal((await api("ali", `/v1/grup/${g.id}/alarm-yanit`, { istek: req.id, kabul: true })).json.hata, "istek", "yalnız alıcı yanıtlar");
  // Etiket bildirilince hemen kalkar (yalnız alıcıya görünür)
  await api("ali", `/v1/grup/${g.id}/alarm`, { uye: ayseMid, saat: "07:00", etiket: "Uyan tembel" });
  r = await api("ayse", `/v1/grup/${g.id}`);
  const second = r.json.istekler.find((x) => x.saat === "07:00");
  assert.equal((await api("ayse", `/v1/grup/${g.id}/bildir`, { istek: second.id })).json.gizlendi, true);
  r = await api("ayse", `/v1/grup/${g.id}`);
  assert.equal(r.json.istekler.find((x) => x.id === second.id).etiket, null);
  // Günde 5
  for (let i = 0; i < 3; i++) await api("ali", `/v1/grup/${g.id}/alarm`, { uye: ayseMid, saat: "08:0" + i });
  assert.equal((await api("ali", `/v1/grup/${g.id}/alarm`, { uye: ayseMid, saat: "09:00" })).json.hata, "alarm-sinir");
  // Engelle: bekleyenler reddedilir, yeni istek gelmez
  await t.q(`delete from grp_alarm`);
  r = await api("ayse", `/v1/grup/${g.id}/engelle`, { uye: aliMid });
  assert.equal((await api("ali", `/v1/grup/${g.id}/alarm`, { uye: ayseMid, saat: "06:00" })).json.hata, "engelli");
});

test("anonim hava bildirimi: hücre sayıları, kişi başına sınır, verilerimde, 24 saatte silinir", async () => {
  assert.equal((await api("ayse", "/v1/hava-bildirim", { tur: "kasirga", enlem: 41, boylam: 29 })).json.hata, "tur");
  let r = await api("ayse", "/v1/hava-bildirim", { tur: "yagmur", enlem: 41.01, boylam: 28.97 });
  assert.deepEqual(r.json, { sayilar: { yagmur: 1 }, dakika: 60 });
  assert.equal((await api("ayse", "/v1/hava-bildirim", { tur: "kar", enlem: 41.01, boylam: 28.97 })).status, 429, "10 dakikada bir");
  await api("ali", "/v1/hava-bildirim", { tur: "yagmur", enlem: 41.2, boylam: 28.8 });
  r = await api("can", "/v1/hava-bildirim?enlem=41.1&boylam=28.9");
  assert.deepEqual(r.json.sayilar, { yagmur: 2 });
  r = await t.req("/v1/verilerim", { token: tok.ayse });
  assert.equal(r.json.havaBildirimleri[0].hucre, "41.00:28.75");
  assert.ok(Array.isArray(r.json.gruplar));
  await t.q(`update weather_report set at = now() - interval '25 hours'`);
  r = await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${CRON}` } });
  assert.equal(r.json.gruplar.hava, 2);
});

test("bildir üç kişide adı gizler; hesap silinince kurucu devri; tek başına ayrılınca grup silinir", async () => {
  const g = await make("deniz", { tur: "dunya", ad: "Uygunsuz olabilir" });
  for (const n of ["ayse", "ali", "can"]) await api(n, "/v1/grup/katil", { kod: g.kod });
  for (const n of ["ayse", "ali"]) await api(n, `/v1/grup/${g.id}/bildir`, {});
  let r = await api("can", `/v1/grup/${g.id}/bildir`, {});
  assert.equal(r.json.gizlendi, true);
  assert.match(t.outbox.filter((m) => m.kind === "raw").at(-1).subject, /grup bildirimi/);
  r = await api("ali", `/v1/grup/${g.id}`);
  assert.equal(r.json.ad, null);
  r = await t.req("/v1/hesap/sil", { method: "POST", token: tok.deniz, body: { kapsam: "urun" } });
  assert.equal(r.status, 200);
  r = await api("ayse", `/v1/grup/${g.id}`);
  assert.equal(r.json.sahip, true);
  assert.equal(r.json.uyeler.length, 3);
  for (const n of ["ali", "can", "ayse"]) await api(n, `/v1/grup/${g.id}/ayril`, {});
  assert.equal((await t.q(`select 1 from grp where id = $1`, [g.id])).rowCount, 0);
});
