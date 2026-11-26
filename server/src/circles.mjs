/* Üyelik aşama 2 (sahibin isteği, 15 Kasım 2026; docs/uyelik-plani-2026-11.md): ortak zikir ve hatim halkaları.
   - Davetle kurulan küçük, gizli gruplar (en çok 30 üye); herkese açık liste, akış, sohbet yok.
   - Zikir halkası: hazır zikir türü (serbest metin yok), ortak hedef, isteğe bağlı bitiş günü; her üye kendi sayacından
     ekler, toplam ve üyelerin payı görünür; sıralama yok.
   - Hatim halkası: 30 cüz; üye cüz alır, bırakır, bitirir; 30'u bitince hatim sayısı artar, yeni hatim başlar.
   - Din verisi (KVKK m. 6, GDPR m. 9): ilk kurma ya da katılmada ayrı açık rıza (`circle_consent`, sürümlü); geri
     alınınca kişi bütün halkalardan çıkar.
   - Kullanıcı içeriği (Apple 1.2, Google Play UGC): yalnız halka adı ve görünen ad. Süzgeç (bağlantı, e-posta, telefon,
     kısa yasaklı sözcük listesi), bildir (e-postayla sahibe; üç ayrı kişiden sonra ad kendiliğinden gizlenir), engelle
     (engellenenin adı engelleyene görünmez), kurucu üyeyi çıkarır.
   - Başka üyelerle ortak içerik (standart madde 7): ayrılan ya da silinen üyenin katkısı kalır, adı yerine "Eski üye" /
     "Silinmiş üye"; kurucu ayrılır ya da hesabı silinirse halka en eski üyeye geçer; üye kalmadıysa hesap silmede 30 gün
     tutulur, sonra silinir (ayrılmada hemen silinir). */
import crypto from "node:crypto";
import { profileOf } from "./accounts.mjs";
import { displayName } from "./profile.mjs";
import { isBanned } from "./moderation.mjs";

export const CIRCLE_CONSENT_VERSION = "2026-11-15";
export const LIMITS = { members: 30, circlesPerUser: 20, nameMax: 40, targetMin: 33, targetMax: 10_000_000, addMax: 10_000, reportsToHide: 3 };
export const ZIKIR_KINDS = ["subhanallah", "elhamdulillah", "allahuekber", "tevhid", "istigfar", "salavat", "free"];
const CODE_ABC = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

export const SCHEMA = [
  `create table if not exists circle_consent (
     user_id text primary key references "user"(id) on delete cascade,
     at timestamptz not null default now(),
     version text not null
   )`,
  `create table if not exists circle (
     id text primary key,
     kind text not null,
     name text not null,
     name_hidden boolean not null default false,
     owner_id text references "user"(id) on delete set null,
     code text not null unique,
     zikir text,
     target bigint,
     total bigint not null default 0,
     round int not null default 1,
     khatms int not null default 0,
     ends_on date,
     created_at timestamptz not null default now(),
     orphan_at timestamptz
   )`,
  `create table if not exists circle_member (
     id bigserial primary key,
     circle_id text not null references circle(id) on delete cascade,
     user_id text not null references "user"(id) on delete cascade,
     joined_at timestamptz not null default now(),
     count bigint not null default 0,
     name_hidden boolean not null default false,
     unique (circle_id, user_id)
   )`,
  `create table if not exists circle_juz (
     circle_id text not null references circle(id) on delete cascade,
     juz int not null,
     user_id text references "user"(id) on delete set null,
     gone text,
     taken_at timestamptz not null default now(),
     done_at timestamptz,
     primary key (circle_id, juz)
   )`,
  `create table if not exists circle_block (
     user_id text not null references "user"(id) on delete cascade,
     blocked_id text not null references "user"(id) on delete cascade,
     primary key (user_id, blocked_id)
   )`,
  `create table if not exists circle_report (
     circle_id text not null references circle(id) on delete cascade,
     reporter_id text not null references "user"(id) on delete cascade,
     target text not null,
     created_at timestamptz not null default now(),
     primary key (circle_id, reporter_id, target)
   )`,
];

/* Süzgeç (Apple 1.2 "filtering"): bağlantı, e-posta, telefon numarası ve kısa yasaklı sözcük listesi. Bütün diller için
   kusursuz değildir; asıl önlem bildir/engelle ve davetle girilen küçük gruplardır. */
const BANNED = ["amk", "aq", "orospu", "piç", "pic", "sik", "yarrak", "göt", "got", "ibne", "kahpe", "pezevenk", "fuck", "shit", "bitch", "cunt", "dick", "nigger", "nigga", "faggot", "whore", "slut", "porn", "porno", "sex", "seks", "nazi", "hitler"];
export function cleanName(v) {
  const s = String(v ?? "").replace(/[\u0000-\u001f\u007f​-‏‪-‮⁦-⁩]/g, "").replace(/\s+/g, " ").trim();
  if (!s || s.length > LIMITS.nameMax) return { hata: "ad" };
  if (/(https?:|www\.|\.[a-z]{2,}\/|@|\b\d[\d\s().-]{7,}\d\b)/i.test(s)) return { hata: "ad-icerik" };
  const words = s.toLocaleLowerCase("tr").normalize("NFKD").replace(/[̀-ͯ]/g, "").split(/[^\p{L}\p{N}]+/u);
  const banned = new Set(BANNED.map((w) => w.normalize("NFKD").replace(/[̀-ͯ]/g, "")));
  if (words.some((w) => banned.has(w))) return { hata: "ad-icerik" };
  return { ad: s };
}

const newCode = () => Array.from(crypto.randomBytes(8), (b) => CODE_ABC[b % CODE_ABC.length]).join("");
const newId = () => crypto.randomBytes(9).toString("base64url");
const err = (status, hata) => Object.assign(new Error(hata), { status, hata });

export function createCircles({ pool, mailer, adminEmail, modLink = () => "" }) {
  async function q(sql, args = []) { return pool.query(sql, args); }

  async function hasConsent(userId) {
    return (await q(`select 1 from circle_consent where user_id = $1`, [userId])).rowCount > 0;
  }
  async function giveConsent(userId) {
    await q(`insert into circle_consent (user_id, version) values ($1, $2) on conflict (user_id) do update set at = now(), version = $2`, [userId, CIRCLE_CONSENT_VERSION]);
  }
  /* Halkaya girmeden önce: onay ya da önceden verilmiş onay; kişi başına en çok 20 halka */
  async function admit(userId, riza) {
    // Topluluk kurallarını çiğnediği için uzaklaştırılan kişi (moderation.mjs) halka kuramaz, katılamaz
    if (await isBanned(pool, userId)) throw err(403, "uzaklastirildi");
    if (!(await hasConsent(userId))) {
      if (riza !== true) throw err(400, "riza");
      await giveConsent(userId);
    }
    const n = (await q(`select count(*)::int n from circle_member where user_id = $1`, [userId])).rows[0].n;
    if (n >= LIMITS.circlesPerUser) throw err(400, "halka-sinir");
  }

  async function memberOf(circleId, userId) {
    const r = await q(`select c.*, m.id as mid from circle c join circle_member m on m.circle_id = c.id where c.id = $1 and m.user_id = $2`, [circleId, userId]);
    if (!r.rowCount) throw err(404, "halka-yok");
    return r.rows[0];
  }

  async function create(user, b) {
    await admit(user.id, b.riza);
    const kind = b.tur === "hatim" ? "hatim" : b.tur === "zikir" ? "zikir" : null;
    if (!kind) throw err(400, "tur");
    const n = cleanName(b.ad);
    if (n.hata) throw err(400, n.hata);
    let zikir = null, target = null, endsOn = null;
    if (kind === "zikir") {
      zikir = ZIKIR_KINDS.includes(b.zikir) ? b.zikir : "subhanallah";
      target = Math.trunc(Number(b.hedef));
      if (!Number.isFinite(target) || target < LIMITS.targetMin || target > LIMITS.targetMax) throw err(400, "hedef");
    }
    if (b.bitis) {
      if (!/^\d{4}-\d{2}-\d{2}$/.test(b.bitis) || Number.isNaN(Date.parse(b.bitis + "T00:00:00Z"))) throw err(400, "bitis");
      endsOn = b.bitis;
    }
    const id = newId();
    for (let i = 0; ; i++) {
      try {
        await q(`insert into circle (id, kind, name, owner_id, code, zikir, target, ends_on) values ($1,$2,$3,$4,$5,$6,$7,$8)`, [id, kind, n.ad, user.id, newCode(), zikir, target, endsOn]);
        break;
      } catch (e) {
        if (e.code !== "23505" || i > 4) throw e; // kod çakışırsa yeniden
      }
    }
    await q(`insert into circle_member (circle_id, user_id) values ($1, $2)`, [id, user.id]);
    return id;
  }

  async function preview(code) {
    const r = await q(`select c.id, c.kind, c.name, c.name_hidden, (select count(*)::int from circle_member m where m.circle_id = c.id) n from circle c where c.code = $1 and c.orphan_at is null`, [String(code || "").toUpperCase().trim()]);
    if (!r.rowCount) throw err(404, "kod");
    const c = r.rows[0];
    return { tur: c.kind, ad: c.name_hidden ? null : c.name, uyeSayisi: c.n, dolu: c.n >= LIMITS.members };
  }

  async function join(user, b) {
    const r = await q(`select id from circle where code = $1 and orphan_at is null`, [String(b.kod || "").toUpperCase().trim()]);
    if (!r.rowCount) throw err(404, "kod");
    const id = r.rows[0].id;
    if ((await q(`select 1 from circle_member where circle_id = $1 and user_id = $2`, [id, user.id])).rowCount) return id;
    await admit(user.id, b.riza);
    const client = await pool.connect();
    try {
      await client.query("begin");
      await client.query(`select id from circle where id = $1 for update`, [id]);
      const n = (await client.query(`select count(*)::int n from circle_member where circle_id = $1`, [id])).rows[0].n;
      if (n >= LIMITS.members) throw err(400, "dolu");
      await client.query(`insert into circle_member (circle_id, user_id) values ($1, $2) on conflict do nothing`, [id, user.id]);
      await client.query("commit");
    } catch (e) {
      await client.query("rollback").catch(() => {});
      throw e;
    } finally {
      client.release();
    }
    return id;
  }

  /* Görünen ad: standart madde 7'deki kural (takma ad / ad / "Üye 10458"); bildirimle gizlendiyse yalnız numara */
  function nameOf(row, lang) {
    const p = profileOf({ profile: row.profile });
    return row.name_hidden ? displayName({}, row.uyeNo, lang) : displayName(p, row.uyeNo, lang);
  }

  async function detail(user, id, lang = "tr") {
    const c = await memberOf(id, user.id);
    const blocked = new Set((await q(`select blocked_id from circle_block where user_id = $1`, [user.id])).rows.map((x) => x.blocked_id));
    const ms = (await q(
      `select m.id, m.user_id, m.count, m.name_hidden, m.joined_at, u.profile, u."uyeNo" from circle_member m join "user" u on u.id = m.user_id where m.circle_id = $1 order by m.joined_at, m.id`,
      [id],
    )).rows;
    const byUser = new Map(ms.map((m) => [m.user_id, m]));
    const out = {
      id: c.id, tur: c.kind, ad: c.name_hidden ? null : c.name, adGizli: c.name_hidden, kod: c.code,
      sahip: c.owner_id === user.id, bitis: c.ends_on ? new Date(c.ends_on).toISOString().slice(0, 10) : null,
      uyeler: ms.map((m) => ({
        id: Number(m.id), ad: blocked.has(m.user_id) ? null : nameOf(m, lang), engelli: blocked.has(m.user_id),
        sen: m.user_id === user.id, sahip: m.user_id === c.owner_id, sayi: Number(m.count),
      })),
      enCok: LIMITS.members,
    };
    if (c.kind === "zikir") Object.assign(out, { zikir: c.zikir, hedef: Number(c.target), toplam: Number(c.total) });
    else {
      const js = (await q(`select juz, user_id, gone, done_at from circle_juz where circle_id = $1`, [id])).rows;
      const map = new Map(js.map((j) => [j.juz, j]));
      out.hatim = { tur: c.round, biten: c.khatms };
      out.cuzler = Array.from({ length: 30 }, (_, i) => {
        const j = map.get(i + 1);
        if (!j) return { cuz: i + 1, durum: "bos" };
        const m = j.user_id ? byUser.get(j.user_id) : null;
        return { cuz: i + 1, durum: j.done_at ? "bitti" : "alindi", uye: m ? Number(m.id) : null, sen: j.user_id === user.id, eski: j.user_id ? null : j.gone || "ayrildi" };
      });
    }
    return out;
  }

  async function list(user, lang = "tr") {
    const r = await q(
      `select c.id, c.kind, c.name, c.name_hidden, c.zikir, c.target, c.total, c.round, c.khatms, c.owner_id,
              (select count(*)::int from circle_member x where x.circle_id = c.id) n,
              (select count(*)::int from circle_juz j where j.circle_id = c.id and j.done_at is not null) done
         from circle c join circle_member m on m.circle_id = c.id where m.user_id = $1 order by m.joined_at`,
      [user.id],
    );
    return r.rows.map((c) => ({
      id: c.id, tur: c.kind, ad: c.name_hidden ? null : c.name, sahip: c.owner_id === user.id, uyeSayisi: c.n,
      ...(c.kind === "zikir" ? { zikir: c.zikir, hedef: Number(c.target), toplam: Number(c.total) } : { hatim: { tur: c.round, biten: c.khatms, bitenCuz: c.done } }),
    }));
  }

  async function addZikir(user, id, n) {
    const c = await memberOf(id, user.id);
    if (c.kind !== "zikir") throw err(400, "tur");
    const add = Math.trunc(Number(n));
    if (!Number.isFinite(add) || add < 1 || add > LIMITS.addMax) throw err(400, "sayi");
    if (c.ends_on && new Date(c.ends_on).getTime() + 86400_000 < Date.now()) throw err(400, "bitti");
    await q(`update circle_member set count = count + $1 where circle_id = $2 and user_id = $3`, [add, id, user.id]);
    const r = await q(`update circle set total = total + $1 where id = $2 returning total`, [add, id]);
    return Number(r.rows[0].total);
  }

  /* Cüz: al (boşsa), bırak (kendi aldığı, bitmemiş), bitir (kendi aldığı ya da boşsa doğrudan); 30'u bitince yeni hatim */
  async function juz(user, id, juzNo, op) {
    const c = await memberOf(id, user.id);
    if (c.kind !== "hatim") throw err(400, "tur");
    const n = Math.trunc(Number(juzNo));
    if (!(n >= 1 && n <= 30)) throw err(400, "cuz");
    const client = await pool.connect();
    try {
      await client.query("begin");
      await client.query(`select id from circle where id = $1 for update`, [id]);
      const cur = (await client.query(`select user_id, done_at from circle_juz where circle_id = $1 and juz = $2`, [id, n])).rows[0];
      if (op === "al") {
        if (cur) throw err(409, "alinmis");
        await client.query(`insert into circle_juz (circle_id, juz, user_id) values ($1, $2, $3)`, [id, n, user.id]);
      } else if (op === "birak") {
        if (!cur || cur.user_id !== user.id || cur.done_at) throw err(409, "senin-degil");
        await client.query(`delete from circle_juz where circle_id = $1 and juz = $2`, [id, n]);
      } else if (op === "bitir") {
        if (cur && (cur.user_id !== user.id || cur.done_at)) throw err(409, "senin-degil");
        if (cur) await client.query(`update circle_juz set done_at = now() where circle_id = $1 and juz = $2`, [id, n]);
        else await client.query(`insert into circle_juz (circle_id, juz, user_id, done_at) values ($1, $2, $3, now())`, [id, n, user.id]);
        const done = (await client.query(`select count(*)::int n from circle_juz where circle_id = $1 and done_at is not null`, [id])).rows[0].n;
        if (done >= 30) {
          await client.query(`delete from circle_juz where circle_id = $1`, [id]);
          await client.query(`update circle set khatms = khatms + 1, round = round + 1 where id = $1`, [id]);
        }
      } else throw err(400, "islem");
      await client.query("commit");
    } catch (e) {
      await client.query("rollback").catch(() => {});
      throw e;
    } finally {
      client.release();
    }
  }

  /* Halkadan çıkış (ayrılma, çıkarılma, onayın geri alınması, hesap silme): bitmemiş cüz serbest kalır, biten cüz ve
     zikir katkısı halkada kalır; kurucu çıkarsa halka en eski üyeye geçer. Üye kalmadıysa: hesap silmede 30 gün tutulur
     (orphan_at), öbür durumlarda hemen silinir. */
  async function removeMember(circleId, userId, gone = "ayrildi") {
    await q(`delete from circle_juz where circle_id = $1 and user_id = $2 and done_at is null`, [circleId, userId]);
    await q(`update circle_juz set user_id = null, gone = $3 where circle_id = $1 and user_id = $2`, [circleId, userId, gone]);
    await q(`delete from circle_member where circle_id = $1 and user_id = $2`, [circleId, userId]);
    const c = (await q(`select owner_id from circle where id = $1`, [circleId])).rows[0];
    if (!c) return;
    const next = (await q(`select user_id from circle_member where circle_id = $1 order by joined_at, id limit 1`, [circleId])).rows[0];
    if (!next) {
      if (gone === "silindi") await q(`update circle set owner_id = null, orphan_at = now() where id = $1`, [circleId]);
      else await q(`delete from circle where id = $1`, [circleId]);
    } else if (c.owner_id === userId || c.owner_id == null) {
      await q(`update circle set owner_id = $2 where id = $1`, [circleId, next.user_id]);
    }
  }

  async function leave(user, id) {
    await memberOf(id, user.id);
    await removeMember(id, user.id, "ayrildi");
  }

  async function kick(user, id, mid) {
    const c = await memberOf(id, user.id);
    if (c.owner_id !== user.id) throw err(403, "sahip-degil");
    const m = (await q(`select user_id from circle_member where circle_id = $1 and id = $2`, [id, Number(mid)])).rows[0];
    if (!m || m.user_id === user.id) throw err(404, "uye");
    await removeMember(id, m.user_id, "ayrildi");
  }

  async function edit(user, id, b) {
    const c = await memberOf(id, user.id);
    if (c.owner_id !== user.id) throw err(403, "sahip-degil");
    if (b.ad != null) {
      const n = cleanName(b.ad);
      if (n.hata) throw err(400, n.hata);
      // Yeni ad, gizlenmiş adın yerine geçer; bildirimler sıfırlanır
      await q(`update circle set name = $2, name_hidden = false where id = $1`, [id, n.ad]);
      await q(`delete from circle_report where circle_id = $1 and target = 'ad'`, [id]);
    }
    if (b.hedef != null && c.kind === "zikir") {
      const t = Math.trunc(Number(b.hedef));
      if (!(t >= LIMITS.targetMin && t <= LIMITS.targetMax)) throw err(400, "hedef");
      await q(`update circle set target = $2 where id = $1`, [id, t]);
    }
    if (b.bitis !== undefined) {
      if (b.bitis && !/^\d{4}-\d{2}-\d{2}$/.test(b.bitis)) throw err(400, "bitis");
      await q(`update circle set ends_on = $2 where id = $1`, [id, b.bitis || null]);
    }
    if (b.kodYenile === true) await q(`update circle set code = $2 where id = $1`, [id, newCode()]);
  }

  async function remove(user, id) {
    const c = await memberOf(id, user.id);
    if (c.owner_id !== user.id) throw err(403, "sahip-degil");
    await q(`delete from circle where id = $1`, [id]);
  }

  /* Bildir: halka adı ya da bir üyenin görünen adı. Her bildirim e-postayla yöneticiye gider (yalnız bildirilen metin;
     kimin bildirdiği yazılmaz) ve imzalı yönetici bağlantısı taşır (moderation.mjs: gizle, topluluktan uzaklaştır). Üç
     ayrı kişiden bildirim gelince ad kendiliğinden gizlenir. */
  async function report(user, id, b) {
    const c = await memberOf(id, user.id);
    let target, text, author;
    if (b.uye != null) {
      const m = (await q(`select m.id, m.user_id, u.profile, u."uyeNo", m.name_hidden from circle_member m join "user" u on u.id = m.user_id where m.circle_id = $1 and m.id = $2`, [id, Number(b.uye)])).rows[0];
      if (!m || m.user_id === user.id) throw err(404, "uye");
      target = `uye:${m.user_id}`;
      text = nameOf({ ...m, name_hidden: false }, "tr");
      author = m.user_id;
    } else {
      target = "ad";
      text = c.name;
      author = c.owner_id;
    }
    const ins = await q(`insert into circle_report (circle_id, reporter_id, target) values ($1, $2, $3) on conflict do nothing`, [id, user.id, target]);
    if (!ins.rowCount) return { yeni: false };
    const n = (await q(`select count(*)::int n from circle_report where circle_id = $1 and target = $2`, [id, target])).rows[0].n;
    const hidden = n >= LIMITS.reportsToHide;
    if (hidden) await hideTarget(id, target);
    const what = target === "ad" ? "halka adı" : "üyenin görünen adı";
    const link = modLink({ s: "halka", id, h: target, u: author || undefined });
    await mailer.sendRaw({
      to: adminEmail || "bilgi@bumbagroup.com",
      subject: `Saat Tarih halka bildirimi: ${what}${hidden ? " (gizlendi)" : ""}`,
      text: `Bildirilen ${what}: ${text}\nHalka: ${id} (${c.kind})\nBu içerik için bildirim sayısı: ${n}${hidden ? "\nÜç ayrı bildirimle kendiliğinden gizlendi." : ""}\n\nTopluluk kurallarındaki söz: mümkün olan en kısa sürede incele.${link ? `\nGizlemek ya da yazanı topluluktan uzaklaştırmak için: ${link}` : ""}\n`,
    }).catch(() => {});
    return { yeni: true, gizlendi: hidden };
  }

  /* Yönetici işlemleri (moderation.mjs) */
  async function hideTarget(id, target) {
    if (target === "ad") await q(`update circle set name_hidden = true where id = $1`, [id]);
    else if (target.startsWith("uye:")) await q(`update circle_member set name_hidden = true where circle_id = $1 and user_id = $2`, [id, target.slice(4)]);
  }
  async function modState(id, target) {
    const c = (await q(`select name, name_hidden from circle where id = $1`, [id])).rows[0];
    if (!c) return { var: false };
    if (target === "ad") return { var: true, metin: c.name, gizli: c.name_hidden };
    const m = (await q(`select m.name_hidden, u.profile, u."uyeNo" from circle_member m join "user" u on u.id = m.user_id where m.circle_id = $1 and m.user_id = $2`, [id, target.slice(4)])).rows[0];
    if (!m) return { var: false };
    return { var: true, metin: nameOf({ ...m, name_hidden: false }, "tr"), gizli: m.name_hidden };
  }
  /* Topluluktan uzaklaştırma: kişi bütün halkalardan çıkar (katkısı "Eski üye" olarak kalır) */
  async function expel(userId) {
    const ids = (await q(`select circle_id from circle_member where user_id = $1`, [userId])).rows.map((x) => x.circle_id);
    for (const id of ids) await removeMember(id, userId, "ayrildi");
  }

  async function block(user, id, mid, on = true) {
    await memberOf(id, user.id);
    const m = (await q(`select user_id from circle_member where circle_id = $1 and id = $2`, [id, Number(mid)])).rows[0];
    if (!m || m.user_id === user.id) throw err(404, "uye");
    if (on) await q(`insert into circle_block (user_id, blocked_id) values ($1, $2) on conflict do nothing`, [user.id, m.user_id]);
    else await q(`delete from circle_block where user_id = $1 and blocked_id = $2`, [user.id, m.user_id]);
  }

  /* Onayı geri alma: bütün halkalardan çıkar, onay kaydı silinir */
  async function withdraw(user) {
    const ids = (await q(`select circle_id from circle_member where user_id = $1`, [user.id])).rows.map((x) => x.circle_id);
    for (const id of ids) await removeMember(id, user.id, "ayrildi");
    await q(`delete from circle_consent where user_id = $1`, [user.id]);
  }

  /* Hesap silinmeden ya da yasaklanmadan önce (standart "Başka üyelerle ortak içerik") */
  async function onUserGone(userId) {
    const ids = (await q(`select circle_id from circle_member where user_id = $1`, [userId])).rows.map((x) => x.circle_id);
    for (const id of ids) await removeMember(id, userId, "silindi");
    await q(`update circle_juz set user_id = null, gone = 'silindi' where user_id = $1`, [userId]);
    // 5.28 "Hukuk", silinen hesabın izi: üyeyi hedef alan bildirim satırları da o anda silinir
    await q(`delete from circle_report where target = $1`, [`uye:${userId}`]);
  }

  /* Verilerimi indir */
  async function exportOf(userId) {
    const consent = (await q(`select at, version from circle_consent where user_id = $1`, [userId])).rows[0];
    const rows = (await q(
      `select c.id, c.kind, c.name, m.joined_at, m.count, c.owner_id = $1 as owner,
              (select array_agg(j.juz order by j.juz) from circle_juz j where j.circle_id = c.id and j.user_id = $1) juz
         from circle_member m join circle c on c.id = m.circle_id where m.user_id = $1`,
      [userId],
    )).rows;
    return {
      onay: consent ? { zaman: new Date(consent.at).toISOString(), surum: consent.version } : null,
      halkalar: rows.map((r) => ({ id: r.id, tur: r.kind, ad: r.name, katilma: new Date(r.joined_at).toISOString(), kurucu: r.owner, zikir: Number(r.count), cuzler: r.juz || [] })),
    };
  }

  /* Temizlik: üyesi kalmamış halkalar 30 gün sonra */
  async function cleanup(now = new Date()) {
    return (await q(`delete from circle where orphan_at is not null and orphan_at < $1::timestamptz - interval '30 days'`, [now])).rowCount;
  }

  async function counts() {
    const r = (await q(`select count(*) filter (where kind = 'zikir')::int z, count(*) filter (where kind = 'hatim')::int h from circle`)).rows[0];
    return { zikir: r.z, hatim: r.h };
  }

  return { hasConsent, create, preview, join, detail, list, addZikir, juz, leave, kick, edit, remove, report, block, withdraw, onUserGone, exportOf, cleanup, counts, hideTarget, modState, expel };
}
