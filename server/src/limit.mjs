/* Kendi uçlarımızın deneme sınırı, veritabanında (standart madde 7: bellekte tutulan sınır her yayında sıfırlanır).
   IP yalnız X-Real-Ip'den okunur. */
export function ipOf(req) {
  return String(req.headers["x-real-ip"] || "").trim() || "yok";
}

export function createLimiter(pool, enabled = true) {
  /* true: izin var; false: sınır aşıldı */
  return async function hit(key, windowSec, max) {
    if (!enabled) return true;
    const now = Date.now();
    const r = await pool.query(
      `insert into own_limit (key, count, window_start) values ($1, 1, $2)
       on conflict (key) do update set
         count = case when own_limit.window_start < $2 - $3::bigint then 1 else own_limit.count + 1 end,
         window_start = case when own_limit.window_start < $2 - $3::bigint then $2 else own_limit.window_start end
       returning count`,
      [key, now, windowSec * 1000],
    );
    return r.rows[0].count <= max;
  };
}
