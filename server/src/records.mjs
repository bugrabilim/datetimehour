/* Uygulamadaki kayıtlar ve kazanım rozetleri hesapta (standart madde 7 "kullanım verisi" ve madde 13 "Üye").
   Kayıtlar: sahibin kararı (12 Kasım 2026) hem hesapta hem iCloud'da. Anahtar başına son yazan kazanır (t: istemcinin
   yazdığı an, Unix ms). Rozetler: yalnız kimlik ve kazanma günü; bilinmeyen kimlik ve ileri tarih reddedilir, aynı
   rozette erken gün kalır. Hepsi hesapla indirilir ve silinir. */

/* Uygulamalardaki rozetler (iOS Badges.swift, Android Badges.kt; iki platformda aynı kimlikler) */
export const BADGES = [
  "renk", "saniye", "ilk-model", "tam-ekran", "duzenle", "takvim", "kronometre", "zamanlayici", "alarm", "cevirici",
  "tarih-hesap", "dunya", "kure", "pomodoro", "seri-3", "model-10", "seri-7", "model-hepsi", "uyanik",
];

const KEY_RE = /^[A-Za-z0-9._-]{1,80}$/;
export const LIMITS = { valueBytes: 256 * 1024, totalBytes: 4 * 1024 * 1024, keys: 600, batch: 200 };

const bad = (field) => Object.assign(new Error("gecersiz:" + field), { status: 400, field });

export function createRecords(pool) {
  async function list(userId, since = 0) {
    const r = await pool.query(`select key, value, t from account_record where user_id = $1 and t > $2 order by key`, [userId, Number(since) || 0]);
    const out = {};
    for (const row of r.rows) out[row.key] = { v: row.value, t: Number(row.t) };
    return out;
  }

  /* degisiklik: { anahtar: { v: metin | null, t: ms } }; null siler. Daha yeni kayıt varsa eskisi yazılmaz. */
  async function put(userId, degisiklik) {
    const entries = Object.entries(degisiklik || {});
    if (entries.length > LIMITS.batch) throw bad("toplu");
    for (const [k, e] of entries) {
      if (!KEY_RE.test(k) || !e || !Number.isFinite(Number(e.t))) throw bad("anahtar");
      if (e.v !== null && typeof e.v !== "string") throw bad("deger");
      if (e.v && Buffer.byteLength(e.v) > LIMITS.valueBytes) throw bad("boyut");
    }
    const client = await pool.connect();
    try {
      await client.query("begin");
      for (const [k, e] of entries) {
        const t = Math.round(Number(e.t));
        if (e.v === null) {
          await client.query(`delete from account_record where user_id = $1 and key = $2 and t <= $3`, [userId, k, t]);
        } else {
          await client.query(
            `insert into account_record (user_id, key, value, t) values ($1, $2, $3, $4)
             on conflict (user_id, key) do update set value = excluded.value, t = excluded.t where account_record.t < excluded.t`,
            [userId, k, e.v, t],
          );
        }
      }
      const s = await client.query(`select count(*)::int n, coalesce(sum(octet_length(value)), 0)::bigint b from account_record where user_id = $1`, [userId]);
      if (s.rows[0].n > LIMITS.keys || Number(s.rows[0].b) > LIMITS.totalBytes) throw bad("kota");
      await client.query("commit");
    } catch (e) {
      await client.query("rollback").catch(() => {});
      throw e;
    } finally {
      client.release();
    }
  }

  async function badges(userId) {
    const r = await pool.query(`select badge, to_char(day, 'YYYY-MM-DD') as gun from user_badge where user_id = $1 order by day, badge`, [userId]);
    return r.rows.map((x) => ({ id: x.badge, gun: x.gun }));
  }

  /* rozetler: [{ id, gun: "YYYY-MM-DD" }]; gün en geç yarın (saat dilimi payı) */
  async function addBadges(userId, rozetler, now = new Date()) {
    const list = Array.isArray(rozetler) ? rozetler : [];
    if (list.length > BADGES.length) throw bad("rozet");
    const maxDay = new Date(now.getTime() + 86400_000).toISOString().slice(0, 10);
    for (const b of list) {
      if (!BADGES.includes(b?.id)) throw bad("rozet");
      const d = String(b?.gun || "");
      if (!/^\d{4}-\d{2}-\d{2}$/.test(d) || d > maxDay || d < "2026-01-01" || Number.isNaN(Date.parse(d))) throw bad("gun");
    }
    for (const b of list) {
      await pool.query(
        `insert into user_badge (user_id, badge, day) values ($1, $2, $3)
         on conflict (user_id, badge) do update set day = least(user_badge.day, excluded.day)`,
        [userId, b.id, b.gun],
      );
    }
    return badges(userId);
  }

  async function clearBadges(userId) {
    await pool.query(`delete from user_badge where user_id = $1`, [userId]);
  }

  return { list, put, badges, addBadges, clearBadges };
}
