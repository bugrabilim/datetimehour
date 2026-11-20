/* Üyelik aşama 2: ortak zikir ve hatim halkaları. Açık rıza, davet kodu, zikir toplamı, 30 cüz ve yeni hatim, süzgeç,
   bildir (üç kişide gizlenir, sahibe e-posta), engelle, çıkar, kurucunun ayrılması, hesap silmede devir ve
   "Silinmiş üye", verilerimi indir, onayın geri alınması, temizlik. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified, deleteAccount, CRON } from "./helpers.mjs";
import { cleanName } from "../src/circles.mjs";

let t;
const tok = {};
before(async () => {
  t = await start({ HALKALAR: "acik" });
  for (const n of ["ayse", "ali", "can", "deniz", "ece"]) tok[n] = await signUpVerified(t, `${n}@example.com`);
});
after(async () => { await t.stop(); });

const api = (who, path, body) => t.req(path, { method: body === undefined ? "GET" : "POST", token: tok[who], body });

test("süzgeç: bağlantı, e-posta, telefon ve yasaklı sözcük", () => {
  assert.equal(cleanName("Cuma halkamız").ad, "Cuma halkamız");
  assert.equal(cleanName("  Aile   hatmi ").ad, "Aile hatmi");
  for (const bad of ["www.ornek.com", "bak https://x.y", "a@b.co", "0532 123 45 67", "Orospu halkası", "x".repeat(41), ""]) assert.ok(cleanName(bad).hata, bad);
});

test("zikir halkası: rıza şart, kod ile katılma, toplam ve paylar", async () => {
  assert.equal((await api("ayse", "/v1/halka", { tur: "zikir", ad: "Sabah zikri", hedef: 1000 })).json.hata, "riza");
  let r = await api("ayse", "/v1/halka", { tur: "zikir", ad: "Sabah zikri", hedef: 1000, zikir: "salavat", riza: true });
  assert.equal(r.status, 200);
  const c = r.json;
  assert.equal(c.sahip, true);
  assert.equal(c.zikir, "salavat");
  assert.match(c.kod, /^[A-Z2-9]{8}$/);
  // Önizleme ve katılma (Ali'nin rızası yok → önce riza hatası)
  r = await api("ali", "/v1/halka/onizle", { kod: c.kod.toLowerCase() });
  assert.deepEqual(r.json, { tur: "zikir", ad: "Sabah zikri", uyeSayisi: 1, dolu: false });
  assert.equal((await api("ali", "/v1/halka/katil", { kod: c.kod })).json.hata, "riza");
  r = await api("ali", "/v1/halka/katil", { kod: c.kod, riza: true });
  assert.equal(r.json.uyeler.length, 2);
  assert.equal((await api("ali", "/v1/halka/katil", { kod: "YANLIS22" })).json.hata, "kod");
  // Ekleme
  await api("ayse", `/v1/halka/${c.id}/zikir`, { ekle: 100 });
  r = await api("ali", `/v1/halka/${c.id}/zikir`, { ekle: 33 });
  assert.equal(r.json.toplam, 133);
  assert.equal((await api("ali", `/v1/halka/${c.id}/zikir`, { ekle: 0 })).json.hata, "sayi");
  assert.equal((await api("ali", `/v1/halka/${c.id}/zikir`, { ekle: 10001 })).json.hata, "sayi");
  r = await api("ali", `/v1/halka/${c.id}`);
  assert.equal(r.json.toplam, 133);
  assert.deepEqual(r.json.uyeler.map((u) => u.sayi), [100, 33]);
  assert.equal(r.json.uyeler[0].ad, "Üye 10458", "görünen ad standarttaki kural");
  assert.equal(r.json.sahip, false);
  // Üye olmayan göremez
  assert.equal((await api("can", `/v1/halka/${c.id}`)).status, 404);
  // Liste
  r = await api("ali", "/v1/halkalar");
  assert.equal(r.json.riza, true);
  assert.equal(r.json.halkalar[0].toplam, 133);
});

test("hatim halkası: cüz al, bırak, bitir; 30 cüz bitince yeni hatim", async () => {
  let r = await api("ayse", "/v1/halka", { tur: "hatim", ad: "Aile hatmi" });
  const id = r.json.id;
  assert.equal(r.json.cuzler.length, 30);
  await api("ali", "/v1/halka/katil", { kod: r.json.kod });
  r = await api("ali", `/v1/halka/${id}/cuz`, { cuz: 5, islem: "al" });
  assert.equal(r.json.cuzler[4].durum, "alindi");
  assert.equal(r.json.cuzler[4].sen, true);
  assert.equal((await api("ayse", `/v1/halka/${id}/cuz`, { cuz: 5, islem: "al" })).json.hata, "alinmis");
  assert.equal((await api("ayse", `/v1/halka/${id}/cuz`, { cuz: 5, islem: "birak" })).json.hata, "senin-degil");
  r = await api("ali", `/v1/halka/${id}/cuz`, { cuz: 5, islem: "birak" });
  assert.equal(r.json.cuzler[4].durum, "bos");
  for (let j = 1; j <= 29; j++) await api(j % 2 ? "ayse" : "ali", `/v1/halka/${id}/cuz`, { cuz: j, islem: "bitir" });
  r = await api("ayse", `/v1/halka/${id}`);
  assert.equal(r.json.cuzler.filter((x) => x.durum === "bitti").length, 29);
  r = await api("ayse", `/v1/halka/${id}/cuz`, { cuz: 30, islem: "bitir" });
  assert.deepEqual(r.json.hatim, { tur: 2, biten: 1 });
  assert.ok(r.json.cuzler.every((x) => x.durum === "bos"));
});

test("bildir, engelle, çıkar: üç ayrı bildirimde ad gizlenir, sahibe e-posta", async () => {
  let r = await api("deniz", "/v1/halka", { tur: "zikir", ad: "Kötü olabilecek ad", hedef: 99, riza: true });
  const c = r.json;
  for (const n of ["ayse", "ali", "can"]) await api(n, "/v1/halka/katil", { kod: c.kod, riza: true });
  const before = t.outbox.filter((m) => m.kind === "raw").length;
  for (const n of ["ayse", "ali"]) assert.equal((await api(n, `/v1/halka/${c.id}/bildir`, {})).json.gizlendi, false);
  assert.equal((await api("ali", `/v1/halka/${c.id}/bildir`, {})).json.yeni, false, "aynı kişi iki kez sayılmaz");
  r = await api("can", `/v1/halka/${c.id}/bildir`, {});
  assert.equal(r.json.gizlendi, true);
  const mails = t.outbox.filter((m) => m.kind === "raw").slice(before);
  assert.equal(mails.length, 3);
  assert.match(mails[2].subject, /gizlendi/);
  assert.match(mails[2].text, /Kötü olabilecek ad/);
  assert.doesNotMatch(mails[2].text, /@example\.com/, "kim bildirdi/yazdı yazılmaz");
  r = await api("ali", `/v1/halka/${c.id}`);
  assert.equal(r.json.ad, null);
  assert.equal(r.json.adGizli, true);
  // Kurucu yeni ad verince görünür
  r = await api("deniz", `/v1/halka/${c.id}/duzenle`, { ad: "Akşam zikri" });
  assert.equal(r.json.ad, "Akşam zikri");
  assert.equal((await api("ali", `/v1/halka/${c.id}/duzenle`, { ad: "Başka" })).json.hata, "sahip-degil");
  // Engelle: engellenenin adı engelleyene görünmez
  const denizMid = r.json.uyeler.find((u) => u.sahip).id;
  r = await api("ali", `/v1/halka/${c.id}/engelle`, { uye: denizMid });
  assert.equal(r.json.uyeler.find((u) => u.id === denizMid).ad, null);
  assert.equal(r.json.uyeler.find((u) => u.id === denizMid).engelli, true);
  // Çıkar
  const canMid = r.json.uyeler.find((u) => !u.sahip && !u.sen && !u.engelli && u.id !== denizMid && u.sayi === 0 && true).id;
  r = await api("deniz", `/v1/halka/${c.id}/cikar`, { uye: canMid });
  assert.equal(r.json.uyeler.length, 3);
});

test("kurucu ayrılınca en eski üyeye geçer; hesap silinince 'Silinmiş üye' ve devir; rıza geri alınır; temizlik", async () => {
  let r = await api("ece", "/v1/halka", { tur: "hatim", ad: "Ece'nin hatmi", riza: true });
  const id = r.json.id;
  await api("ayse", "/v1/halka/katil", { kod: r.json.kod });
  await api("ece", `/v1/halka/${id}/cuz`, { cuz: 1, islem: "bitir" });
  await api("ece", `/v1/halka/${id}/cuz`, { cuz: 2, islem: "al" });
  // Verilerimi indir
  r = await t.req("/v1/verilerim", { token: tok.ece });
  assert.equal(r.json.halkalar.halkalar.find((h) => h.id === id).cuzler.join(), "1,2");
  assert.ok(r.json.halkalar.onay.surum);
  // Ayşe Ece'yi bildirir; hesap silinince bu bildirim satırı da gider (5.28 "Hukuk", silinen hesabın izi)
  const eceMid = (await api("ayse", `/v1/halka/${id}`)).json.uyeler.find((u) => u.sahip).id;
  assert.equal((await api("ayse", `/v1/halka/${id}/bildir`, { uye: eceMid })).status, 200);
  const eceId = (await t.q(`select id from "user" where email = 'ece@example.com'`)).rows[0].id;
  assert.equal((await t.q(`select 1 from circle_report where target = $1`, [`uye:${eceId}`])).rowCount, 1);
  // Ece hesabını siler: Ayşe kurucu olur, biten cüz "silindi", alınmış cüz serbest
  r = await deleteAccount(t, tok.ece, "ece@example.com");
  assert.equal(r.status, 200);
  assert.equal((await t.q(`select 1 from circle_report where target = $1`, [`uye:${eceId}`])).rowCount, 0, "silinen üyeyi hedef alan bildirim kalmaz");
  r = await api("ayse", `/v1/halka/${id}`);
  assert.equal(r.json.sahip, true);
  assert.equal(r.json.cuzler[0].eski, "silindi");
  assert.equal(r.json.cuzler[1].durum, "bos");
  // Ayşe tek başına ayrılırsa halka silinir
  await api("ayse", `/v1/halka/${id}/ayril`, {});
  assert.equal((await t.q(`select 1 from circle where id = $1`, [id])).rowCount, 0);
  // Rıza geri alınınca bütün halkalardan çıkar
  await api("can", "/v1/halka/riza/geri-al", {});
  r = await api("can", "/v1/halkalar");
  assert.equal(r.json.riza, false);
  assert.equal(r.json.halkalar.length, 0);
  // Üyesi kalmayan (hesap silmede) halka 30 gün sonra silinir
  await t.q(`insert into circle (id, kind, name, code, orphan_at) values ('yetim0001', 'hatim', 'x', 'ZZZZZZZZ', now() - interval '31 days')`);
  r = await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${CRON}` } });
  assert.equal(r.json.halkalar, 1);
});
