/* Halka ve grup bildirimlerinde yönetici işlemi (Apple 1.2: bildirime zamanında yanıt, içeriği kaldırma ve yazanı
   topluluktan çıkarma; Google Play kullanıcı içeriği politikası). Her bildirim e-postası yöneticiye (config.mjs
   adminEmail) imzalı bir bağlantı taşır: /moderasyon?k=… (30 gün geçerli). Bağlantıyı açmak hiçbir şey yapmaz (e-posta
   programları bağlantıları önceden açabilir); sayfadaki düğmeler POST ile çalışır:
   - "İçeriği gizle": adı (halka/grup adı, görünen ad, şehir, alarm etiketi) herkesten gizler.
   - "Gizle ve kişiyi topluluktan uzaklaştır": ayrıca yazan kişi bütün halka ve gruplardan çıkar ve yenisini kuramaz,
     katılamaz (`community_ban`). Hesabın kendisi çalışmaya devam eder (giriş, rozetler, kayıtlar).
   - "Uzaklaştırmayı kaldır".
   Bağlantının kendisi yetkidir; yalnız yöneticinin e-postasına gider. */
import crypto from "node:crypto";

export const MOD_DAYS = 30;

export const SCHEMA = [
  `create table if not exists community_ban (
     user_id text primary key references "user"(id) on delete cascade,
     reason text,
     created_at timestamptz not null default now()
   )`,
];

/* Uzaklaştırılan kişi halka ya da grup kuramaz, katılamaz */
export async function isBanned(pool, userId) {
  return (await pool.query(`select 1 from community_ban where user_id = $1`, [userId])).rowCount > 0;
}

/* spec: { s: "halka" | "grup", id, h: hedef ("ad" | "uye:<userId>" | "istek:<n>"), u: içeriği yazan kişi } */
export function modToken(secret, spec, exp = Date.now() + MOD_DAYS * 864e5) {
  const body = Buffer.from(JSON.stringify({ ...spec, e: Math.floor(exp / 1000) })).toString("base64url");
  const sig = crypto.createHmac("sha256", secret).update("moderasyon:" + body).digest("base64url").slice(0, 32);
  return `${body}.${sig}`;
}

export function readModToken(secret, token, now = Date.now()) {
  const [body, sig] = String(token || "").split(".");
  if (!body || !sig || !secret) return null;
  const want = crypto.createHmac("sha256", secret).update("moderasyon:" + body).digest("base64url").slice(0, 32);
  const a = Buffer.from(sig), b = Buffer.from(want);
  if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) return null;
  try {
    const spec = JSON.parse(Buffer.from(body, "base64url").toString("utf8"));
    if (!spec || !["halka", "grup"].includes(spec.s) || typeof spec.id !== "string" || typeof spec.h !== "string") return null;
    if (!(spec.e * 1000 > now)) return null;
    return spec;
  } catch {
    return null;
  }
}

export function createModeration({ pool, circles, groups }) {
  const side = (spec) => (spec.s === "halka" ? circles : groups);

  /* Sayfada gösterilecek durum: içerik hâlâ var mı, gizli mi, kişi uzaklaştırılmış mı */
  async function look(spec) {
    const st = await side(spec).modState(spec.id, spec.h);
    return { ...st, uzaklastirildi: spec.u ? await isBanned(pool, spec.u) : false };
  }

  async function hide(spec) {
    await side(spec).hideTarget(spec.id, spec.h);
  }

  async function ban(spec) {
    await hide(spec);
    if (!spec.u) return;
    const exists = (await pool.query(`select 1 from "user" where id = $1`, [spec.u])).rowCount;
    if (!exists) return;
    await pool.query(`insert into community_ban (user_id, reason) values ($1, $2) on conflict do nothing`, [spec.u, `${spec.s}:${spec.id}:${spec.h}`]);
    await circles.expel(spec.u);
    await groups.expel(spec.u);
  }

  async function unban(spec) {
    if (spec.u) await pool.query(`delete from community_ban where user_id = $1`, [spec.u]);
  }

  return { look, hide, ban, unban };
}
