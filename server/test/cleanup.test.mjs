/* Saklama süreleri: süresi geçmiş kayıtlar, doğrulanmamış hesap (7 gün), kullanılmayan hesap (2 yıl, 30 gün önce uyarı). */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { start, signUpVerified } from "./helpers.mjs";
import { runCleanup, fmtDate } from "../src/cleanup.mjs";

let t;
before(async () => { t = await start(); });
after(async () => { await t.stop(); });

const q = (sql, args) => t.app.pool.query(sql, args);
const DAY = 86400_000;

test("giriş son kullanımı yazar", async () => {
  await signUpVerified(t, "aktif@example.com");
  const r = await q(`select a.last_seen, a.warned_at from user_activity a join "user" u on u.id = a.user_id where u.email = 'aktif@example.com'`);
  assert.equal(r.rows.length, 1);
  assert.ok(Date.now() - r.rows[0].last_seen.getTime() < 60_000);
  assert.equal(r.rows[0].warned_at, null);
});

test("doğrulanmamış hesap 7 gün sonra silinir", async () => {
  await t.req("/api/auth/sign-up/email", { method: "POST", body: { email: "dogrulanmamis@example.com", password: "guclu-sifre-1", name: "X", lang: "tr" } });
  let r = await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now: new Date(Date.now() + 6 * DAY) });
  assert.equal(r.unverified, 0);
  r = await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now: new Date(Date.now() + 8 * DAY) });
  assert.equal(r.unverified, 1);
  assert.equal((await q(`select count(*)::int n from "user" where email = 'dogrulanmamis@example.com'`)).rows[0].n, 0);
  assert.equal((await q(`select count(*)::int n from "user" where email = 'aktif@example.com'`)).rows[0].n, 1, "doğrulanmış hesap kalır");
});

test("2 yıl kullanılmayan hesap: önce uyarı, giriş yapılırsa kalır", async () => {
  await signUpVerified(t, "geri@example.com", "guclu-sifre-1", "de");
  const id = (await q(`select id from "user" where email = 'geri@example.com'`)).rows[0].id;
  await q(`update user_activity set last_seen = now() - interval '24 months' + interval '10 days' where user_id = $1`, [id]);
  const now = new Date();
  const r = await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now });
  assert.equal(r.warned, 1);
  const m = t.last("inactive", "geri@example.com");
  assert.ok(m);
  assert.equal(m.lang, "de");
  assert.equal(m.vars.date, fmtDate("de", new Date(now.getTime() + 30 * DAY)));
  assert.match(m.text, /Uhr-&-Datum/);
  assert.equal((await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now })).warned, 0, "bir kez uyarılır");

  // Kullanıcı giriş yapar: uyarı kalkar, silinmez
  const s = await t.req("/api/auth/sign-in/email", { method: "POST", body: { email: "geri@example.com", password: "guclu-sifre-1" } });
  assert.equal(s.status, 200);
  const a = (await q(`select warned_at from user_activity where user_id = $1`, [id])).rows[0];
  assert.equal(a.warned_at, null);
  const later = await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now: new Date(now.getTime() + 31 * DAY) });
  assert.equal(later.deleted, 0);
});

test("uyarıdan 30 gün sonra hâlâ kullanılmayan hesap silinir ve haber gider", async () => {
  await signUpVerified(t, "eski@example.com", "guclu-sifre-1", "ar");
  const id = (await q(`select id from "user" where email = 'eski@example.com'`)).rows[0].id;
  await q(`update user_activity set last_seen = now() - interval '25 months' where user_id = $1`, [id]);
  await q(`delete from session where "userId" = $1`, [id]);
  const now = new Date();
  assert.equal((await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now })).warned, 1);
  assert.equal((await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now: new Date(now.getTime() + 29 * DAY) })).deleted, 0);
  const r = await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now: new Date(now.getTime() + 31 * DAY) });
  assert.equal(r.deleted, 1);
  assert.equal((await q(`select count(*)::int n from "user" where id = $1`, [id])).rows[0].n, 0);
  assert.equal((await q(`select count(*)::int n from user_activity where user_id = $1`, [id])).rows[0].n, 0);
  assert.equal((await q(`select count(*)::int n from account where "userId" = $1`, [id])).rows[0].n, 0);
  assert.equal(t.last("deleted", "eski@example.com").lang, "ar");
});

test("yönetici hesabı kullanılmasa da silinmez", async () => {
  await signUpVerified(t, "bilgi@bumbagroup.com");
  const id = (await q(`select id, role from "user" where email = 'bilgi@bumbagroup.com'`)).rows[0];
  assert.equal(id.role, "admin");
  await q(`update user_activity set last_seen = now() - interval '30 months' where user_id = $1`, [id.id]);
  const r = await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now: new Date(Date.now() + 60 * DAY) });
  assert.equal(r.warned + r.deleted, 0);
});

test("süresi geçmiş oturum ve doğrulama kayıtları silinir", async () => {
  const r = await runCleanup({ pool: t.app.pool, mailer: t.app.mailer, now: new Date(Date.now() + 100 * DAY) });
  assert.ok(r.sessions >= 1);
  assert.equal((await q(`select count(*)::int n from session`)).rows[0].n, 0);
  assert.equal((await q(`select count(*)::int n from verification where "expiresAt" < now() + interval '100 days'`)).rows[0].n, 0);
});

test("tarih Latin rakamla, dilin biçiminde", () => {
  const d = new Date(Date.UTC(2027, 0, 15));
  assert.equal(fmtDate("tr", d), "15 Ocak 2027");
  assert.match(fmtDate("ar", d), /15/);
  assert.match(fmtDate("bn", d), /15/);
  assert.match(fmtDate("fa", d), /2027|1405/);
});

test("zamanlanmış görev ucu yalnız CRON_SECRET'la çalışır", async () => {
  assert.equal((await t.req("/gorev/temizlik", { method: "POST" })).status, 401);
  assert.equal((await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: "Bearer yanlis" } })).status, 401);
  assert.equal((await t.req("/gorev/temizlik", { method: "GET", headers: { authorization: `Bearer ${t.cfg.cronSecret}` } })).status, 404);
  const r = await t.req("/gorev/temizlik", { method: "POST", headers: { authorization: `Bearer ${t.cfg.cronSecret}` } });
  assert.equal(r.status, 200);
  assert.equal(typeof r.json.unverified, "number");
});
