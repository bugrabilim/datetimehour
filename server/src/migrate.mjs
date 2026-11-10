/* Veritabanı: Better Auth tablolarını kendi geçiş planıyla kurar (yalnız yeni tablo ve sütun ekler),
   ardından bu sunucunun tabloları. Standart: veritabanında yalnız ekleyici değişiklik. */
import { getMigrations } from "better-auth/db/migration";

const OWN = [
  `create table if not exists metric_day (
     day date primary key,
     requests bigint not null default 0,
     errors bigint not null default 0,
     total_ms bigint not null default 0
   )`,
  // Son kullanım (2 yıl kullanılmayan hesap silinir, cleanup.mjs); hesap silinince satır da gider
  `create table if not exists user_activity (
     user_id text primary key references "user"(id) on delete cascade,
     last_seen timestamptz,
     warned_at timestamptz
   )`,
];

/* options: Better Auth ayarları (betterAuth() çağrılmadan önce; tablolar yokken uyarı yazmasın diye önce kurulur) */
export async function migrate(options, pool) {
  const { runMigrations } = await getMigrations(options);
  await runMigrations();
  for (const sql of OWN) await pool.query(sql);
}

/* Doğrudan çalıştırma: npm run migrate */
if (import.meta.url === `file://${process.argv[1]}`) {
  const { loadConfig } = await import("./config.mjs");
  const { createMailer } = await import("./mail.mjs");
  const { authOptions } = await import("./auth.mjs");
  const pg = (await import("pg")).default;
  const cfg = loadConfig();
  const pool = new pg.Pool({ connectionString: cfg.databaseUrl });
  await migrate(authOptions(cfg, pool, createMailer(cfg)), pool);
  await pool.end();
  console.log("Veritabanı güncel.");
}
