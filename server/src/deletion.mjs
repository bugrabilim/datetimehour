/* Silme onayı (standart 5.21, madde 7; sahibin kararı, 15 Kasım 2026): hesabı silme ya da hesaba bağlı veriyi silme
   hemen yapılmaz. İstek anında şifre (ya da son 24 saatte açılmış oturum) istenir, hesabın e-posta adresine tek
   kullanımlık onay bağlantısı gider (rastgele anahtar, veritabanında yalnız özeti; 7 gün geçerli). Bağlantıyı açmak
   tek başına silmez (e-posta programları bağlantıları önceden açabilir): açılan sayfa ne silineceğini yazar, işlem
   "Onaylıyorum" düğmesiyle (POST) yapılır, sonra bilgilendirme e-postası gider. Hesap bu sürede olduğu gibi çalışır;
   hesap sayfasında bekleyen istek ve "İsteği iptal et" görünür; yeni istek eskisinin yerine geçer. 7 gün içinde
   onaylanmayan istek günlük temizlikte silinir, e-posta gitmez. Hesap silinince istek de gider (on delete cascade). */
import crypto from "node:crypto";

export const DELETE_DAYS = 7;
/* tur: "hesap" (hesabın kendisi) ya da "rozetler" (hesaptaki rozetler) */
export const KINDS = ["hesap", "rozetler"];

export const SCHEMA = [
  `create table if not exists delete_request (
     user_id text primary key references "user"(id) on delete cascade,
     kind text not null,
     scope text not null default 'urun',
     token_hash text not null unique,
     created_at timestamptz not null default now(),
     expires_at timestamptz not null
   )`,
];

const digest = (k) => crypto.createHash("sha256").update(String(k || "")).digest("hex");
const view = (r) => (r ? { tur: r.kind, sonGun: new Date(r.expires_at).toISOString() } : null);

/* perform(user, tur): onaydan sonra asıl işlem (index.mjs); bilgilendirme e-postasını da o gönderir */
export function createDeletion({ pool, mailer, publicUrl, perform, normLang }) {
  async function request(user, kind) {
    if (!KINDS.includes(kind)) throw Object.assign(new Error("tur"), { status: 400 });
    const token = crypto.randomBytes(32).toString("base64url");
    const exp = new Date(Date.now() + DELETE_DAYS * 864e5);
    const r = await pool.query(
      `insert into delete_request (user_id, kind, scope, token_hash, expires_at) values ($1, $2, $3, $4, $5)
       on conflict (user_id) do update set kind = excluded.kind, scope = excluded.scope, token_hash = excluded.token_hash,
         created_at = now(), expires_at = excluded.expires_at
       returning *`,
      [user.id, kind, "urun", digest(token), exp],
    );
    const url = `${publicUrl}/silme-onay?dil=${normLang(user.lang)}&k=${token}`;
    try {
      await mailer.send(kind === "hesap" ? "deleteAsk" : "dataDeleteAsk", user.email, user.lang, { url, until: exp });
    } catch (e) {
      await pool.query(`delete from delete_request where user_id = $1 and token_hash = $2`, [user.id, digest(token)]);
      throw Object.assign(new Error("gonderilemedi"), { status: 503 });
    }
    return view(r.rows[0]);
  }
  async function pending(userId) {
    const r = await pool.query(`select * from delete_request where user_id = $1 and expires_at > now()`, [userId]);
    return view(r.rows[0]);
  }
  async function cancel(userId) {
    return (await pool.query(`delete from delete_request where user_id = $1`, [userId])).rowCount > 0;
  }
  /* Onay sayfası: geçerli istek ne siler (GET; hiçbir şey değişmez) */
  async function peek(token) {
    if (!token) return null;
    const r = await pool.query(`select * from delete_request where token_hash = $1 and expires_at > now()`, [digest(token)]);
    return view(r.rows[0]);
  }
  /* "Onaylıyorum" (POST): istek tek seferde alınır ve silinir (tek kullanımlık), sonra işlem yapılır */
  async function confirm(token) {
    if (!token) return null;
    const r = await pool.query(`delete from delete_request where token_hash = $1 and expires_at > now() returning *`, [digest(token)]);
    const row = r.rows[0];
    if (!row) return null;
    const user = (await pool.query(`select * from "user" where id = $1`, [row.user_id])).rows[0];
    if (!user) return null;
    await perform(user, row.kind);
    return row.kind;
  }
  async function cleanup(now = new Date()) {
    return (await pool.query(`delete from delete_request where expires_at <= $1`, [now])).rowCount;
  }
  return { request, pending, cancel, peek, confirm, cleanup };
}
