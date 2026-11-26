/* Temizlik (standart madde 7, "Saklama"): `POST /gorev/temizlik`, günde bir (görevi ana altyapı oturumu kurar).
   - süresi geçmiş oturum, doğrulama ve deneme sınırı kayıtları her gün silinir;
   - 7 gün içinde doğrulanmayan hesap silinir (ortak servise `ayril`; e-posta gönderilmez);
   - e-postası doğrulanmış ama Bumba üye numarası eksik hesaplar numarasını `kayit`'tan alır (yönetici hesabı hariç:
     üye değildir, merkez çağrılmaz);
   - tek seferlik hukuki bilgilendirme e-postası (5.33, notice.mjs) henüz almamış üyelere gider (mailer verilmişse);
   - ortak servise ulaşılamadığı için bekleyen bildirimlerin zamanı gelenler de denenir (asıl görev relay.mjs, 10 dakikada bir).
   Kullanılmayan hesabı ürün silmez (5.28: 3 yıl hiçbir Bumba ürününde girilmeyen hesabı merkez sayar, uyarır ve `hesap-sil`
   ile siler; ürün her başarılı girişte `sorgula`'yı `uyeNo`'yla çağırır, panel `uye` son girişi ve oturumların son
   kullanımını verir). */
import { sendNotices } from "./notice.mjs";

export const RETENTION = { unverifiedDays: 7 };

export async function runCleanup({ pool, center, relay, mailer = null, adminEmail = "", now = new Date() }) {
  const out = { sessions: 0, verifications: 0, rateLimits: 0, unverified: 0, numbers: 0, retried: 0, notices: 0 };
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
    if (u.uyeNo != null) await relay.tell("ayril", { uyeNo: Number(u.uyeNo), neden: "dogrulanmamis" });
    await pool.query(`delete from "user" where id = $1`, [u.id]);
    out.unverified++;
  }

  const missing = await pool.query(`select id, email from "user" where "emailVerified" = true and "uyeNo" is null and coalesce(banned, false) = false and lower(email) <> $1`, [String(adminEmail).toLowerCase()]);
  for (const u of missing.rows) {
    const r = await center.kayit(u.email);
    if (r.ok && Number.isInteger(r.data?.no)) {
      await pool.query(`update "user" set "uyeNo" = $1 where id = $2 and "uyeNo" is null`, [r.data.no, u.id]);
      out.numbers++;
    }
  }

  if (mailer) out.notices = (await sendNotices({ pool, mailer, adminEmail })).sent;

  out.retried = (await relay.run(now)).sent;
  return out;
}
