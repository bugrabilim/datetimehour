/* Panel ölçüleri (madde 10): istek sayısı, yanıt süresi, hata oranı. Bellekte toplanır, dakikada bir günlük satıra
   eklenir (Türkiye saatiyle gün). Kişiye ait hiçbir şey tutulmaz; yalnız toplam sayılar. */
const dayOf = (d = new Date()) => new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Istanbul" }).format(d);

export function createMetrics(pool) {
  let buf = new Map();
  function record(status, ms) {
    const day = dayOf();
    const b = buf.get(day) || { requests: 0, errors: 0, ms: 0 };
    b.requests++;
    if (status >= 500) b.errors++;
    b.ms += Math.round(ms);
    buf.set(day, b);
  }
  async function flush() {
    const cur = buf;
    buf = new Map();
    for (const [day, b] of cur) {
      await pool.query(
        `insert into metric_day (day, requests, errors, total_ms) values ($1, $2, $3, $4)
         on conflict (day) do update set requests = metric_day.requests + excluded.requests,
           errors = metric_day.errors + excluded.errors, total_ms = metric_day.total_ms + excluded.total_ms`,
        [day, b.requests, b.errors, b.ms],
      ).catch(() => {
        const x = buf.get(day) || { requests: 0, errors: 0, ms: 0 };
        buf.set(day, { requests: x.requests + b.requests, errors: x.errors + b.errors, ms: x.ms + b.ms });
      });
    }
  }
  const timer = setInterval(() => flush(), 60_000);
  timer.unref();
  return { record, flush, stop: () => clearInterval(timer), dayOf };
}
