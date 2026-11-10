/* Saklama süreleri (KVKK md. 4 ve 7: gereğinden uzun saklanmaz; aydınlatma metnindeki sürelerle aynı):
   - süresi geçmiş oturum, doğrulama ve hız sınırı kayıtları silinir;
   - 7 gün içinde doğrulanmayan hesap silinir (e-posta gönderilmez; adresin sahibi olduğu bilinmiyor);
   - 2 yıl kullanılmayan hesaba 30 gün önce haber verilir, o sürede giriş yapılmazsa hesap silinir ve haber gider.
   Çalıştıran: Coolify'daki zamanlanmış görev (standart, Altyapı kuralı), CRON_SECRET'la korunan POST /gorev/temizlik.
   Kullanım: oturum açılınca ve günde bir yenilenince user_activity.last_seen yazılır (auth.mjs). */
export const RETENTION = { unverifiedDays: 7, inactiveMonths: 24, warnDays: 30 };

export function fmtDate(lang, d) {
  try {
    return new Intl.DateTimeFormat(`${lang}-u-nu-latn`, { dateStyle: "long", timeZone: "UTC" }).format(d);
  } catch {
    return d.toISOString().slice(0, 10);
  }
}

export async function runCleanup({ pool, mailer, now = new Date() }) {
  const out = { sessions: 0, verifications: 0, rateLimits: 0, unverified: 0, warned: 0, deleted: 0 };
  out.sessions = (await pool.query(`delete from session where "expiresAt" < $1`, [now])).rowCount;
  out.verifications = (await pool.query(`delete from verification where "expiresAt" < $1`, [now])).rowCount;
  out.rateLimits = (await pool.query(`delete from "rateLimit" where "lastRequest" < $1`, [now.getTime() - 86400_000])).rowCount;
  out.unverified = (await pool.query(
    `delete from "user" where "emailVerified" = false and "createdAt" < $1::timestamptz - make_interval(days => $2)`,
    [now, RETENTION.unverifiedDays],
  )).rowCount;

  // Uyarı: son kullanım 2 yıl − 30 gün önceyse
  const warn = await pool.query(
    `select u.id, u.email, u.lang from "user" u left join user_activity a on a.user_id = u.id
     where u."emailVerified" = true and a.warned_at is null and u.role <> 'admin'
       and coalesce(a.last_seen, u."createdAt") < $1::timestamptz - make_interval(months => $2) + make_interval(days => $3)`,
    [now, RETENTION.inactiveMonths, RETENTION.warnDays],
  );
  for (const u of warn.rows) {
    const date = fmtDate(u.lang, new Date(now.getTime() + RETENTION.warnDays * 86400_000));
    try {
      await mailer.send("inactive", u.email, u.lang, { date });
    } catch {
      continue; // gönderilemezse uyarılmış sayılmaz, sonraki turda yeniden denenir
    }
    await pool.query(
      `insert into user_activity (user_id, last_seen, warned_at) values ($1, null, $2)
       on conflict (user_id) do update set warned_at = excluded.warned_at`,
      [u.id, now],
    );
    out.warned++;
  }

  // Silme: uyarıdan 30 gün geçti ve o zamandan beri kullanılmadı
  const del = await pool.query(
    `select u.id, u.email, u.lang from "user" u join user_activity a on a.user_id = u.id
     where a.warned_at is not null and a.warned_at < $1::timestamptz - make_interval(days => $2)
       and (a.last_seen is null or a.last_seen < a.warned_at) and u.role <> 'admin'`,
    [now, RETENTION.warnDays],
  );
  for (const u of del.rows) {
    await pool.query(`delete from "user" where id = $1`, [u.id]);
    await mailer.send("deleted", u.email, u.lang, {}).catch(() => {});
    out.deleted++;
  }
  return out;
}
