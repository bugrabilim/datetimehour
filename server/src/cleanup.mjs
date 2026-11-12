/* Temizlik (standart madde 7, "Saklama"): `POST /gorev/temizlik`, günde bir (görevi ana altyapı oturumu kurar).
   - süresi geçmiş oturum, doğrulama ve deneme sınırı kayıtları her gün silinir;
   - 7 gün içinde doğrulanmayan hesap silinir (ortak servise `ayril`; e-posta gönderilmez);
   - e-postası doğrulanmış ama Bumba üye numarası eksik hesaplar numarasını `kayit`'tan alır;
   - ortak servise ulaşılamadığı için bekleyen bildirimler yeniden gönderilir.
   Kullanılmayan hesap silinmez. */
export const RETENTION = { unverifiedDays: 7 };

export async function runCleanup({ pool, center, now = new Date() }) {
  const out = { sessions: 0, verifications: 0, rateLimits: 0, unverified: 0, numbers: 0, retried: 0 };
  out.sessions = (await pool.query(`delete from session where "expiresAt" < $1`, [now])).rowCount;
  out.verifications = (await pool.query(`delete from verification where "expiresAt" < $1`, [now])).rowCount;
  out.rateLimits = (await pool.query(`delete from "rateLimit" where "lastRequest" < $1`, [now.getTime() - 86400_000])).rowCount;
  await pool.query(`delete from verify_ref where created_at < $1::timestamptz - interval '2 days'`, [now]);
  await pool.query(`delete from own_limit where window_start < $1`, [now.getTime() - 86400_000]);

  const old = await pool.query(
    `select id, "uyeNo" from "user" where "emailVerified" = false and "createdAt" < $1::timestamptz - make_interval(days => $2)`,
    [now, RETENTION.unverifiedDays],
  );
  for (const u of old.rows) {
    if (u.uyeNo != null) await center.ayril(Number(u.uyeNo));
    await pool.query(`delete from "user" where id = $1`, [u.id]);
    out.unverified++;
  }

  const missing = await pool.query(`select id, email from "user" where "emailVerified" = true and "uyeNo" is null and coalesce(banned, false) = false`);
  for (const u of missing.rows) {
    const r = await center.kayit(u.email);
    if (r.ok && Number.isInteger(r.data?.no)) {
      await pool.query(`update "user" set "uyeNo" = $1 where id = $2 and "uyeNo" is null`, [r.data.no, u.id]);
      out.numbers++;
    }
  }

  const retry = await pool.query(`select id, op, body, tries from center_retry order by id limit 500`);
  for (const r of retry.rows) {
    const res = await center.call(r.op, r.body);
    if (res.ok || (res.status >= 400 && res.status < 500 && res.status !== 429)) {
      await pool.query(`delete from center_retry where id = $1`, [r.id]);
      if (res.ok) out.retried++;
    } else if (r.tries >= 30) {
      console.error(`[merkez] ${r.op}: 30 denemede iletilemedi`);
      await pool.query(`delete from center_retry where id = $1`, [r.id]);
    } else {
      await pool.query(`update center_retry set tries = tries + 1 where id = $1`, [r.id]);
    }
  }
  return out;
}
