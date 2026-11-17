/* Veritabanı: Better Auth tablolarını kendi geçiş planıyla kurar (yalnız yeni tablo ve sütun ekler), ardından bu
   sunucunun tabloları. Standart: veritabanında yalnız ekleyici değişiklik. Kişiye ait tablolar hesap silinince
   kendiliğinden gider (on delete cascade). */
import { getMigrations } from "better-auth/db/migration";
import { SCHEMA as CIRCLES } from "./circles.mjs";
import { SCHEMA as GROUPS } from "./groups.mjs";
import { SCHEMA as DELETION } from "./deletion.mjs";
import { SCHEMA as MODERATION } from "./moderation.mjs";

const OWN = [
  // Toplu istek ölçüleri (tek panel için; kişiye ait bir şey yok)
  `create table if not exists metric_day (
     day date primary key,
     requests bigint not null default 0,
     errors bigint not null default 0,
     total_ms bigint not null default 0
   )`,
  // Doğrulama sayfasının sonucu (e-postadaki bağlantıya özgü anahtar; 1 gün sonra silinir)
  `create table if not exists verify_ref (
     k text primary key,
     email text not null,
     result text,
     created_at timestamptz not null default now()
   )`,
  // Kutlama e-postası: aynı üye ve gün için ikinci kez bırakılmaz
  `create table if not exists celebration_sent (
     user_id text not null references "user"(id) on delete cascade,
     day date not null,
     kind text not null,
     sent_at timestamptz not null default now(),
     primary key (user_id, day)
   )`,
  // Uygulamadaki kayıtlar (sahibin kararı 12 Kasım 2026: hem hesapta hem iCloud'da); anahtar başına son yazan kazanır
  `create table if not exists account_record (
     user_id text not null references "user"(id) on delete cascade,
     key text not null,
     value text not null,
     t bigint not null,
     primary key (user_id, key)
   )`,
  // Kazanım rozetleri (madde 13): yalnız rozet kimliği ve kazanma günü
  `create table if not exists user_badge (
     user_id text not null references "user"(id) on delete cascade,
     badge text not null,
     day date not null,
     primary key (user_id, badge)
   )`,
  // Ortak servise ulaşılamayınca yeniden gönderilecek bildirimler (relay.mjs, 10 dakikada bir)
  `create table if not exists center_retry (
     id bigserial primary key,
     op text not null,
     body jsonb not null,
     tries int not null default 0,
     created_at timestamptz not null default now()
   )`,
  // 5.22: aynı üyenin bekleyenleri birleşir; 1, 2, 4… dakika arayla, en çok günde bir (relay.mjs)
  `alter table center_retry add column if not exists uye_no bigint`,
  `alter table center_retry add column if not exists next_at timestamptz not null default now()`,
  `create index if not exists center_retry_uye on center_retry (uye_no)`,
  // Kendi uçlarımızın deneme sınırı (kayıt sorusu, bağlama): Better Auth'unki gibi veritabanında
  `create table if not exists own_limit (
     key text primary key,
     count int not null,
     window_start bigint not null
   )`,
];

/* options: Better Auth ayarları (betterAuth() çağrılmadan önce; tablolar yokken uyarı yazmasın diye önce kurulur) */
export async function migrate(options, pool) {
  const { runMigrations } = await getMigrations(options);
  await runMigrations();
  for (const sql of [...OWN, ...CIRCLES, ...GROUPS, ...DELETION, ...MODERATION]) await pool.query(sql);
}
