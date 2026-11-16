/* Bildirime yönetici yanıtı (Apple 1.2): bildirim e-postasındaki imzalı bağlantı, bağlantıyı açmak bir şey yapmaz,
   "gizle" içeriği gizler, "uzaklaştır" yazanı bütün halka ve gruplardan çıkarır ve yenisine sokmaz, "geri" kaldırır;
   topluluk kuralları sayfası. */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified } from "./helpers.mjs";

let t;
const tok = {};
before(async () => {
  t = await start({ HALKALAR: "acik", GRUPLAR: "acik" });
  for (const n of ["ayse", "ali", "deniz"]) tok[n] = await signUpVerified(t, `${n}@example.com`);
});
after(async () => { await t.stop(); });

const api = (who, path, body) => t.req(path, { method: body === undefined ? "GET" : "POST", token: tok[who], body });
const lastLink = () => {
  const m = t.outbox.filter((x) => x.kind === "raw").at(-1);
  const u = new URL(m.text.match(/https?:\/\/\S+\/moderasyon\?k=\S+/)[0]);
  return u.searchParams.get("k");
};

test("topluluk kuralları sayfası 19 dilde", async () => {
  const tr = await t.req("/topluluk-kurallari?dil=tr");
  assert.equal(tr.status, 200);
  assert.match(tr.text, /Topluluk kuralları/);
  assert.match(tr.text, /24 saat/);
  assert.match((await t.req("/topluluk-kurallari?dil=en")).text, /no tolerance/);
});

test("bildirim bağlantısı: aç, gizle, uzaklaştır, geri al", async () => {
  const c = (await api("deniz", "/v1/halka", { tur: "zikir", ad: "Uygunsuz ad", hedef: 99, riza: true })).json;
  const g = (await api("deniz", "/v1/grup", { tur: "dunya", ad: "Deniz grubu" })).json;
  await api("ayse", "/v1/halka/katil", { kod: c.kod, riza: true });
  await api("ali", "/v1/halka/katil", { kod: c.kod, riza: true });
  await api("ayse", "/v1/grup/katil", { kod: g.kod });

  assert.equal((await api("ayse", `/v1/halka/${c.id}/bildir`, {})).json.gizlendi, false);
  const k = lastLink();
  // Açmak hiçbir şey yapmaz
  let page = await t.req(`/moderasyon?k=${encodeURIComponent(k)}`);
  assert.equal(page.status, 200);
  assert.match(page.text, /Uygunsuz ad/);
  assert.match(page.text, /Şu an görünüyor/);
  assert.equal((await api("ali", `/v1/halka/${c.id}`)).json.ad, "Uygunsuz ad");
  // Bozuk bağlantı
  assert.match((await t.req("/moderasyon?k=bozuk.imza")).text, /geçersiz/);
  assert.match((await t.req("/moderasyon", { method: "POST", form: { k: k.slice(0, -2) + "xx", islem: "gizle" } })).text, /geçersiz/);
  // Gizle
  page = await t.req("/moderasyon", { method: "POST", form: { k, islem: "gizle" } });
  assert.match(page.text, /İçerik gizlendi/);
  assert.equal((await api("ali", `/v1/halka/${c.id}`)).json.ad, null);
  // Uzaklaştır: yazan (kurucu) bütün halka ve gruplardan çıkar, yenisini kuramaz ve katılamaz
  page = await t.req("/moderasyon", { method: "POST", form: { k, islem: "uzaklastir" } });
  assert.match(page.text, /çıkarıldı/);
  assert.equal((await api("deniz", "/v1/halkalar")).json.halkalar.length, 0);
  assert.equal((await api("deniz", "/v1/gruplar")).json.gruplar.length, 0);
  let r = await api("deniz", "/v1/halka", { tur: "zikir", ad: "Yeni", hedef: 99, riza: true });
  assert.equal(r.status, 403);
  assert.equal(r.json.hata, "uzaklastirildi");
  assert.equal((await api("deniz", "/v1/grup/katil", { kod: g.kod })).json.hata, "uzaklastirildi");
  // Halka öbür üyelerle sürer (kurucu en eski üyeye geçer), ad gizli kalır
  r = await api("ayse", `/v1/halka/${c.id}`);
  assert.equal(r.json.sahip, true);
  assert.equal(r.json.ad, null);
  // Hesabın kendisi çalışır
  assert.equal((await api("deniz", "/v1/hesap")).status, 200);
  // Geri al
  page = await t.req("/moderasyon", { method: "POST", form: { k, islem: "geri" } });
  assert.match(page.text, /kaldırıldı/);
  assert.equal((await api("deniz", "/v1/halka", { tur: "zikir", ad: "Yeni", hedef: 99, riza: true })).status, 200);
});

test("grup bildiriminde üyenin adı: gizle", async () => {
  const g = (await api("ayse", "/v1/grup", { tur: "dunya", ad: "Ayşe grubu" })).json;
  await api("ali", "/v1/grup/katil", { kod: g.kod });
  const mid = (await api("ayse", `/v1/grup/${g.id}`)).json.uyeler.find((u) => !u.sen).id;
  await api("ayse", `/v1/grup/${g.id}/bildir`, { uye: mid });
  const k = lastLink();
  assert.match((await t.req(`/moderasyon?k=${encodeURIComponent(k)}`)).text, /Üyenin görünen adı/);
  await t.req("/moderasyon", { method: "POST", form: { k, islem: "gizle" } });
  // Gizlenen görünen ad yerine yalnız üye numarası görünür
  assert.match((await t.req(`/moderasyon?k=${encodeURIComponent(k)}`)).text, /Şu an gizli/);
  assert.match((await api("ayse", `/v1/grup/${g.id}`)).json.uyeler.find((u) => u.id === mid).ad, /^Üye \d+$/);
});
