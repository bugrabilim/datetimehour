/* Ortak üyelik servisine iletim ve yeniden deneme (standart madde 7, 5.22): merkeze gönderilemeyen `guncelle` ve
   `ayril` bırakılmaz. Ulaşılamayınca (bağlantı yok, 5xx, 429) `center_retry`'ye yazılır; 10 dakikada bir çalışan görev
   (`run`) zamanı gelenleri yeniden dener: 1, 2, 4… dakika arayla, en çok günde bir. Aynı üyenin bekleyen
   `guncelle`'leri tek satırda birleşir ve gönderilirken hesabın o anki hâli okunur; `ayril` bekleyen `guncelle`'yi
   geçersiz kılar. 4xx yanıtta (429 hariç) iş biter. Bekleyenlerin sayısı ve en eskisi panelde (`merkezeBekleyen`). */

export const RETRY_EVERY_MS = 10 * 60 * 1000;
export const MAX_GAP_MIN = 24 * 60;
const FIELDS = ["eposta", "epostaDogrulandi", "sifreOzeti", "profil", "kutlama"];

const transient = (r) => !r.ok && (r.status === 0 || r.status >= 500 || r.status === 429);
export const gapMinutes = (tries) => Math.min(2 ** tries, MAX_GAP_MIN);

export function createRelay({ pool, center, accounts = () => null }) {
  async function enqueue(op, body, now = new Date()) {
    const no = Number(body?.uyeNo);
    const uyeNo = Number.isInteger(no) ? no : null;
    const next = new Date(now.getTime() + 60_000);
    if (uyeNo != null && op === "ayril") {
      await pool.query(`delete from center_retry where uye_no = $1 and op = 'guncelle'`, [uyeNo]);
      const has = await pool.query(`select 1 from center_retry where uye_no = $1 and op = 'ayril'`, [uyeNo]);
      if (has.rowCount) return;
    }
    if (uyeNo != null && op === "guncelle") {
      if ((await pool.query(`select 1 from center_retry where uye_no = $1 and op = 'ayril'`, [uyeNo])).rowCount) return;
      const old = await pool.query(`select id, body from center_retry where uye_no = $1 and op = 'guncelle' order by id limit 1`, [uyeNo]);
      if (old.rowCount) {
        const merged = { uyeNo, degisiklik: { ...(old.rows[0].body?.degisiklik || {}), ...(body.degisiklik || {}) } };
        await pool.query(`update center_retry set body = $1 where id = $2`, [JSON.stringify(merged), old.rows[0].id]);
        return;
      }
    }
    await pool.query(`insert into center_retry (op, body, uye_no, next_at) values ($1, $2, $3, $4)`, [op, JSON.stringify(body), uyeNo, next]);
  }

  /* Merkeze gönderir; geçici hatada sıraya alır */
  async function tell(op, body) {
    const r = await center.call(op, body);
    if (transient(r)) await enqueue(op, body);
    return r;
  }

  /* Bekleyen `guncelle`: değişen alanların hesaptaki o anki değerleri (hesap yoksa ya da numarası değiştiyse null) */
  async function current(body) {
    const acc = accounts();
    if (!acc) return body;
    const user = await acc.userByNo(Number(body.uyeNo));
    if (!user) return null;
    const keys = Object.keys(body.degisiklik || {}).filter((k) => FIELDS.includes(k));
    if (!keys.length) return null;
    const h = await acc.toHesap(user, { withPassword: keys.includes("sifreOzeti") });
    const degisiklik = {};
    for (const k of keys) degisiklik[k] = h[k];
    if (keys.includes("sifreOzeti") && !degisiklik.sifreOzeti) delete degisiklik.sifreOzeti;
    return Object.keys(degisiklik).length ? { uyeNo: Number(body.uyeNo), degisiklik } : null;
  }

  /* Zamanı gelenleri dener; { sent, waiting } */
  async function run(now = new Date()) {
    let sent = 0;
    const rows = await pool.query(`select id, op, body, tries from center_retry where next_at <= $1 order by id limit 500`, [now]);
    for (const r of rows.rows) {
      const body = r.op === "guncelle" ? await current(r.body) : r.body;
      if (!body) { await pool.query(`delete from center_retry where id = $1`, [r.id]); continue; }
      const res = await center.call(r.op, body);
      if (!transient(res)) {
        await pool.query(`delete from center_retry where id = $1`, [r.id]);
        if (res.ok) sent++;
        continue;
      }
      const tries = r.tries + 1;
      await pool.query(`update center_retry set tries = $1, next_at = $2 where id = $3`, [tries, new Date(now.getTime() + gapMinutes(tries) * 60_000), r.id]);
    }
    return { sent, waiting: (await pending()).sayi };
  }

  /* Panel özeti (madde 10, `merkezeBekleyen`) */
  async function pending() {
    const r = (await pool.query(`select count(*)::int n, min(created_at) e from center_retry`)).rows[0];
    return { sayi: r.n, enEski: r.e ? new Date(r.e).toISOString() : null };
  }

  let timer = null;
  function start(every = RETRY_EVERY_MS) {
    if (timer) return;
    timer = setInterval(() => run().catch((e) => console.error("[merkez] yeniden deneme:", e?.message || e)), every);
    timer.unref?.();
  }
  function stop() {
    if (timer) clearInterval(timer);
    timer = null;
  }

  return { tell, enqueue, run, pending, start, stop };
}
