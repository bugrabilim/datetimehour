/* Ortak özellikler (üyelik aşama 2d, 15 Kasım 2026; sahibin onayı "tüm önerilerin uygun";
   docs/uyelik-ortak-ozellikler-2026-11.md): davetle kurulan küçük, gizli gruplar. Halkalardan (circles.mjs) farkı: din
   verisi değildir, ayrı açık rıza istemez. Ortak kurallar halkalarla aynı: en çok 30 üye, kişi başına 20 grup, 8 harfli
   davet kodu, yalnız grup adı ve görünen ad kullanıcı metni (süzgeç), Bildir (üç kişide gizlenir, sahibe e-posta),
   Engelle (halkalarla ortak liste), kurucu Çıkar; ayrılan/silinen üyenin katkısı kalır, kurucu giderse grup en eski
   üyeye geçer. Türler:
   - geri-sayim: kurucunun belirlediği tarih (ve her yıl tekrarı, simge); üyelerin telefonunda geri sayım.
   - dunya: her üye kendi şehrini (IANA saat dilimi) ve mesai saatini yazar; herkes herkesin yerel saatini görür.
   - oylama: kurucu en çok 12 aday saat koyar, üyeler Uygun/Olabilir/Uygun değil der; sonuç kendiliğinden toplanır.
   - odak: herhangi bir üye ortak odak oturumu başlatır, üyeler katılır ve "bitirdim" der; ceza ve sıralama yok.
   - alarm: üye başka bir üyeye alarm önerir; alıcı onaylarsa kendi telefonunda kurulur (günde en çok 5 öneri,
     engellenen öneremez).
   - gozlem: gökyüzü olaylarında (tutulma, meteor yağmuru, dolunay) "izledim"; grupta olay başına toplam.
   Ayrıca anonim hava bildirimi (mPING gibi): konum 0,25° ızgaraya yuvarlanır, yalnız tür ve sayı; 24 saatte silinir. */
import crypto from "node:crypto";
import { profileOf } from "./accounts.mjs";
import { displayName } from "./profile.mjs";
import { cleanName } from "./circles.mjs";

export const KINDS = ["geri-sayim", "dunya", "oylama", "odak", "alarm", "gozlem"];
export const LIMITS = { members: 30, groupsPerUser: 20, reportsToHide: 3, slots: 12, alarmsPerDay: 5, watched: 300, weatherPer10m: 1, weatherPerDay: 20 };
export const ICONS = ["🎉", "🎂", "💍", "🎓", "✈️", "🏖️", "🎄", "🌙", "⭐", "❤️", "🏠", "👶", "⚽", "🎵", "📚", "🕌"];
export const WEATHER = ["acik", "bulutlu", "yagmur", "saganak", "kar", "dolu", "sis", "ruzgar", "firtina"];
const CODE_ABC = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

export const SCHEMA = [
  `create table if not exists grp (
     id text primary key,
     kind text not null,
     name text not null,
     name_hidden boolean not null default false,
     owner_id text references "user"(id) on delete set null,
     code text not null unique,
     data jsonb not null default '{}',
     created_at timestamptz not null default now(),
     orphan_at timestamptz
   )`,
  `create table if not exists grp_member (
     id bigserial primary key,
     grp_id text not null references grp(id) on delete cascade,
     user_id text not null references "user"(id) on delete cascade,
     joined_at timestamptz not null default now(),
     data jsonb not null default '{}',
     name_hidden boolean not null default false,
     unique (grp_id, user_id)
   )`,
  `create table if not exists grp_report (
     grp_id text not null references grp(id) on delete cascade,
     reporter_id text not null references "user"(id) on delete cascade,
     target text not null,
     created_at timestamptz not null default now(),
     primary key (grp_id, reporter_id, target)
   )`,
  `create table if not exists grp_alarm (
     id bigserial primary key,
     grp_id text not null references grp(id) on delete cascade,
     from_id text references "user"(id) on delete cascade,
     to_id text not null references "user"(id) on delete cascade,
     at text not null,
     days int not null default 0,
     label text,
     state text not null default 'bekliyor',
     created_at timestamptz not null default now()
   )`,
  `create table if not exists weather_report (
     id bigserial primary key,
     user_id text not null references "user"(id) on delete cascade,
     cell text not null,
     kind text not null,
     at timestamptz not null default now()
   )`,
  `create index if not exists weather_report_cell on weather_report (cell, at)`,
];

const newCode = () => Array.from(crypto.randomBytes(8), (b) => CODE_ABC[b % CODE_ABC.length]).join("");
const newId = () => crypto.randomBytes(9).toString("base64url");
const err = (status, hata) => Object.assign(new Error(hata), { status, hata });
const int = (v) => Math.trunc(Number(v));

/* IANA saat dilimi: Intl tanıyorsa geçerli */
export function validTz(tz) {
  if (typeof tz !== "string" || tz.length > 60 || !/^[A-Za-z_]+(\/[A-Za-z0-9_+-]+){0,2}$/.test(tz)) return false;
  try { new Intl.DateTimeFormat("en", { timeZone: tz }); return true; } catch { return false; }
}

/* Hava bildirimi hücresi: 0,25° ızgara (yaklaşık 25 km; şehir düzeyi) */
export function cellOf(lat, lon) {
  const la = Number(lat), lo = Number(lon);
  if (!(Math.abs(la) <= 90 && Math.abs(lo) <= 180)) return null;
  return `${(Math.floor(la * 4) / 4).toFixed(2)}:${(Math.floor(lo * 4) / 4).toFixed(2)}`;
}

/* Türüne göre grubun verisi (kurma ve düzenleme) */
function groupData(kind, b, old = {}) {
  const d = { ...old };
  if (kind === "geri-sayim") {
    if (b.hedef != null) {
      const t = int(b.hedef);
      if (!(t > 946684800000 && t < 4102444800000)) throw err(400, "hedef"); // 2000–2100, Unix ms
      d.hedef = t;
    }
    if (d.hedef == null) throw err(400, "hedef");
    if (b.yillik != null) d.yillik = b.yillik === true;
    if (b.simge != null) d.simge = ICONS.includes(b.simge) ? b.simge : "🎉";
    d.simge ||= "🎉";
    d.yillik ??= false;
  } else if (kind === "oylama") {
    if (b.sure != null) {
      const s = int(b.sure);
      if (!(s >= 15 && s <= 480)) throw err(400, "sure");
      d.sure = s;
    }
    d.sure ??= 60;
    if (b.saatler != null) {
      if (!Array.isArray(b.saatler) || !b.saatler.length || b.saatler.length > LIMITS.slots) throw err(400, "saatler");
      const s = [...new Set(b.saatler.map(int))].filter((x) => x > 15778080 && x < 68374080).sort((a, c) => a - c); // Unix dakika, 2000–2100
      if (s.length !== b.saatler.length) throw err(400, "saatler");
      if (old.saatler && JSON.stringify(old.saatler) !== JSON.stringify(s)) d.degisti = (old.degisti || 0) + 1;
      d.saatler = s;
    }
    if (!d.saatler) throw err(400, "saatler");
    if (b.kapali != null) d.kapali = b.kapali === true;
    d.kapali ??= false;
  }
  return d;
}

export function createGroups({ pool, mailer, adminEmail }) {
  const q = (sql, args = []) => pool.query(sql, args);

  async function admit(userId) {
    const n = (await q(`select count(*)::int n from grp_member where user_id = $1`, [userId])).rows[0].n;
    if (n >= LIMITS.groupsPerUser) throw err(400, "grup-sinir");
  }

  async function memberOf(id, userId) {
    const r = await q(`select g.*, m.id as mid, m.data as mdata from grp g join grp_member m on m.grp_id = g.id where g.id = $1 and m.user_id = $2`, [id, userId]);
    if (!r.rowCount) throw err(404, "grup-yok");
    return r.rows[0];
  }

  async function create(user, b) {
    const kind = KINDS.includes(b.tur) ? b.tur : null;
    if (!kind) throw err(400, "tur");
    await admit(user.id);
    const n = cleanName(b.ad);
    if (n.hata) throw err(400, n.hata);
    const data = groupData(kind, b);
    const id = newId();
    for (let i = 0; ; i++) {
      try {
        await q(`insert into grp (id, kind, name, owner_id, code, data) values ($1,$2,$3,$4,$5,$6)`, [id, kind, n.ad, user.id, newCode(), data]);
        break;
      } catch (e) {
        if (e.code !== "23505" || i > 4) throw e;
      }
    }
    await q(`insert into grp_member (grp_id, user_id) values ($1, $2)`, [id, user.id]);
    return id;
  }

  async function preview(code) {
    const r = await q(`select g.kind, g.name, g.name_hidden, (select count(*)::int from grp_member m where m.grp_id = g.id) n from grp g where g.code = $1 and g.orphan_at is null`, [String(code || "").toUpperCase().trim()]);
    if (!r.rowCount) throw err(404, "kod");
    const g = r.rows[0];
    return { tur: g.kind, ad: g.name_hidden ? null : g.name, uyeSayisi: g.n, dolu: g.n >= LIMITS.members };
  }

  async function join(user, b) {
    const r = await q(`select id from grp where code = $1 and orphan_at is null`, [String(b.kod || "").toUpperCase().trim()]);
    if (!r.rowCount) throw err(404, "kod");
    const id = r.rows[0].id;
    if ((await q(`select 1 from grp_member where grp_id = $1 and user_id = $2`, [id, user.id])).rowCount) return id;
    await admit(user.id);
    const client = await pool.connect();
    try {
      await client.query("begin");
      await client.query(`select id from grp where id = $1 for update`, [id]);
      const n = (await client.query(`select count(*)::int n from grp_member where grp_id = $1`, [id])).rows[0].n;
      if (n >= LIMITS.members) throw err(400, "dolu");
      await client.query(`insert into grp_member (grp_id, user_id) values ($1, $2) on conflict do nothing`, [id, user.id]);
      await client.query("commit");
    } catch (e) {
      await client.query("rollback").catch(() => {});
      throw e;
    } finally {
      client.release();
    }
    return id;
  }

  function nameOf(row, lang) {
    return row.name_hidden ? displayName({}, row.uyeNo, lang) : displayName(profileOf({ profile: row.profile }), row.uyeNo, lang);
  }

  /* Odak oturumu sürüyor mu (başlangıç + süre, 10 dk pay) */
  const running = (o, now = Date.now()) => !!o && o.bas + (o.dk + 10) * 60000 > now;

  async function detail(user, id, lang = "tr") {
    const g = await memberOf(id, user.id);
    const blocked = new Set((await q(`select blocked_id from circle_block where user_id = $1`, [user.id])).rows.map((x) => x.blocked_id));
    const ms = (await q(
      `select m.id, m.user_id, m.data, m.name_hidden, u.profile, u."uyeNo" from grp_member m join "user" u on u.id = m.user_id where m.grp_id = $1 order by m.joined_at, m.id`,
      [id],
    )).rows;
    const d = g.data || {};
    const out = {
      id: g.id, tur: g.kind, ad: g.name_hidden ? null : g.name, adGizli: g.name_hidden, kod: g.code, sahip: g.owner_id === user.id, enCok: LIMITS.members,
      uyeler: ms.map((m) => {
        const u = { id: Number(m.id), ad: blocked.has(m.user_id) ? null : nameOf(m, lang), engelli: blocked.has(m.user_id), sen: m.user_id === user.id, sahip: m.user_id === g.owner_id };
        const md = m.data || {};
        if (g.kind === "dunya" && md.tz) Object.assign(u, { sehir: md.sehir, tz: md.tz, mesai: md.mesai || null });
        if (g.kind === "oylama") Object.assign(u, { oy: Array.isArray(md.oy) && md.oy.length === (d.saatler || []).length && md.degisti === (d.degisti || 0) ? md.oy : null });
        if (g.kind === "odak") Object.assign(u, { katildi: running(d.oturum) && md.oturum === d.oturum?.bas, bitti: running(d.oturum) && md.oturum === d.oturum?.bas && md.bitti === true });
        return u;
      }),
    };
    if (g.kind === "geri-sayim") Object.assign(out, { hedef: d.hedef, yillik: !!d.yillik, simge: d.simge || "🎉" });
    if (g.kind === "oylama") {
      const votes = out.uyeler.filter((u) => u.oy && !u.engelli).map((u) => u.oy);
      const tally = d.saatler.map((_, i) => ({ uygun: votes.filter((v) => v[i] === 2).length, olabilir: votes.filter((v) => v[i] === 1).length }));
      let best = -1;
      tally.forEach((t, i) => { if (t.uygun + t.olabilir > 0 && (best < 0 || t.uygun > tally[best].uygun || (t.uygun === tally[best].uygun && t.olabilir > tally[best].olabilir))) best = i; });
      Object.assign(out, { sure: d.sure, saatler: d.saatler, kapali: !!d.kapali, sonuc: tally, enIyi: best < 0 ? null : best });
    }
    if (g.kind === "odak") out.oturum = running(d.oturum) ? d.oturum : null;
    if (g.kind === "gozlem") {
      const counts = {};
      for (const m of ms) for (const k of (m.data?.izledim || [])) counts[k] = (counts[k] || 0) + 1;
      Object.assign(out, { sayilar: counts, benim: (g.mdata?.izledim) || [] });
    }
    if (g.kind === "alarm") {
      const rows = (await q(`select a.*, (select id from grp_member x where x.grp_id = a.grp_id and x.user_id = a.from_id) fm, (select id from grp_member x where x.grp_id = a.grp_id and x.user_id = a.to_id) tm
                               from grp_alarm a where a.grp_id = $1 and (a.to_id = $2 or a.from_id = $2) and a.created_at > now() - interval '30 days' order by a.created_at desc limit 50`, [id, user.id])).rows;
      out.istekler = rows.filter((a) => !(a.to_id === user.id && blocked.has(a.from_id))).map((a) => ({
        id: Number(a.id), gelen: a.to_id === user.id, kimden: a.fm == null ? null : Number(a.fm), kime: a.tm == null ? null : Number(a.tm),
        saat: a.at, gunler: a.days, etiket: a.label, durum: a.state, zaman: new Date(a.created_at).toISOString(),
      }));
    }
    return out;
  }

  async function list(user) {
    const r = await q(
      `select g.id, g.kind, g.name, g.name_hidden, g.owner_id, g.data, (select count(*)::int from grp_member x where x.grp_id = g.id) n,
              (select count(*)::int from grp_alarm a where a.grp_id = g.id and a.to_id = $1 and a.state = 'bekliyor') bekleyen
         from grp g join grp_member m on m.grp_id = g.id where m.user_id = $1 order by m.joined_at`,
      [user.id],
    );
    return r.rows.map((g) => ({
      id: g.id, tur: g.kind, ad: g.name_hidden ? null : g.name, sahip: g.owner_id === user.id, uyeSayisi: g.n,
      ...(g.kind === "geri-sayim" ? { hedef: g.data.hedef, yillik: !!g.data.yillik, simge: g.data.simge || "🎉" } : {}),
      ...(g.kind === "odak" && running(g.data.oturum) ? { oturum: g.data.oturum } : {}),
      ...(g.kind === "alarm" ? { bekleyen: g.bekleyen } : {}),
    }));
  }

  async function setMemberData(id, userId, data) {
    await q(`update grp_member set data = $3 where grp_id = $1 and user_id = $2`, [id, userId, data]);
  }

  /* Dünya: kendi şehrim (ad süzgeçten geçer), saat dilimi ve isteğe bağlı mesai saati (0–24) */
  async function setPlace(user, id, b) {
    const g = await memberOf(id, user.id);
    if (g.kind !== "dunya") throw err(400, "tur");
    if (b.tz == null) return setMemberData(id, user.id, {});
    if (!validTz(b.tz)) throw err(400, "tz");
    const n = cleanName(b.sehir);
    if (n.hata) throw err(400, "sehir");
    let mesai = null;
    if (b.mesaiBas != null || b.mesaiSon != null) {
      const a = int(b.mesaiBas), c = int(b.mesaiSon);
      if (!(a >= 0 && a <= 24 && c >= 0 && c <= 24 && a !== c)) throw err(400, "mesai");
      mesai = [a, c];
    }
    await setMemberData(id, user.id, { sehir: n.ad, tz: b.tz, mesai });
  }

  /* Oylama: saat başına 2 uygun, 1 olabilir, 0 uygun değil */
  async function vote(user, id, b) {
    const g = await memberOf(id, user.id);
    if (g.kind !== "oylama") throw err(400, "tur");
    if (g.data.kapali) throw err(400, "bitti");
    const v = Array.isArray(b.oylar) ? b.oylar.map(int) : [];
    if (v.length !== g.data.saatler.length || v.some((x) => ![0, 1, 2].includes(x))) throw err(400, "oy");
    await setMemberData(id, user.id, { oy: v, degisti: g.data.degisti || 0 });
  }

  /* Odak: başlat (sürmüyorsa), katıl, bitirdim, ayrıl */
  async function focus(user, id, op, b) {
    const g = await memberOf(id, user.id);
    if (g.kind !== "odak") throw err(400, "tur");
    const o = g.data.oturum;
    if (op === "baslat") {
      if (running(o) && o.bas + o.dk * 60000 > Date.now()) throw err(409, "suruyor");
      const dk = int(b.dk);
      if (!(dk >= 5 && dk <= 120)) throw err(400, "sure");
      const s = { bas: Date.now(), dk };
      await q(`update grp set data = jsonb_set(data, '{oturum}', $2::jsonb) where id = $1`, [id, JSON.stringify(s)]);
      await setMemberData(id, user.id, { oturum: s.bas, bitti: false });
      return;
    }
    if (!running(o)) throw err(400, "oturum-yok");
    if (op === "katil") await setMemberData(id, user.id, { oturum: o.bas, bitti: false });
    else if (op === "bitti") await setMemberData(id, user.id, { oturum: o.bas, bitti: true });
    else if (op === "ayril") await setMemberData(id, user.id, {});
    else throw err(400, "islem");
  }

  /* Gözlem: olay anahtarı uygulamadan (ör. "2026-12-14-geminid", "ay-2026-11-24") */
  async function watched(user, id, b) {
    const g = await memberOf(id, user.id);
    if (g.kind !== "gozlem") throw err(400, "tur");
    const k = String(b.olay || "");
    if (!/^[a-z0-9-]{3,60}$/.test(k)) throw err(400, "olay");
    const set = new Set(g.mdata?.izledim || []);
    if (b.kaldir === true) set.delete(k);
    else {
      if (set.size >= LIMITS.watched) throw err(400, "sinir");
      set.add(k);
    }
    await setMemberData(id, user.id, { izledim: [...set] });
  }

  /* Alarm isteği: alıcı aynı grupta olmalı, engellemişse öneremez; günde en çok 5 */
  async function alarmRequest(user, id, b) {
    const g = await memberOf(id, user.id);
    if (g.kind !== "alarm") throw err(400, "tur");
    const to = (await q(`select user_id from grp_member where grp_id = $1 and id = $2`, [id, int(b.uye)])).rows[0];
    if (!to || to.user_id === user.id) throw err(404, "uye");
    if ((await q(`select 1 from circle_block where user_id = $1 and blocked_id = $2`, [to.user_id, user.id])).rowCount) throw err(403, "engelli");
    if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(String(b.saat || ""))) throw err(400, "saat");
    const days = int(b.gunler ?? 0);
    if (!(days >= 0 && days <= 127)) throw err(400, "gunler");
    let label = null;
    if (b.etiket) {
      const n = cleanName(b.etiket);
      if (n.hata) throw err(400, "etiket");
      label = n.ad;
    }
    const today = (await q(`select count(*)::int n from grp_alarm where from_id = $1 and created_at > now() - interval '1 day'`, [user.id])).rows[0].n;
    if (today >= LIMITS.alarmsPerDay) throw err(429, "alarm-sinir");
    await q(`insert into grp_alarm (grp_id, from_id, to_id, at, days, label) values ($1,$2,$3,$4,$5,$6)`, [id, user.id, to.user_id, b.saat, days, label]);
  }

  async function alarmAnswer(user, id, b) {
    await memberOf(id, user.id);
    const r = await q(`update grp_alarm set state = $4 where id = $1 and grp_id = $2 and to_id = $3 and state = 'bekliyor'`, [int(b.istek), id, user.id, b.kabul === true ? "kuruldu" : "reddedildi"]);
    if (!r.rowCount) throw err(404, "istek");
  }

  async function removeMember(id, userId, gone = "ayrildi") {
    await q(`delete from grp_alarm where grp_id = $1 and state = 'bekliyor' and (from_id = $2 or to_id = $2)`, [id, userId]);
    await q(`delete from grp_member where grp_id = $1 and user_id = $2`, [id, userId]);
    const g = (await q(`select owner_id from grp where id = $1`, [id])).rows[0];
    if (!g) return;
    const next = (await q(`select user_id from grp_member where grp_id = $1 order by joined_at, id limit 1`, [id])).rows[0];
    if (!next) {
      if (gone === "silindi") await q(`update grp set owner_id = null, orphan_at = now() where id = $1`, [id]);
      else await q(`delete from grp where id = $1`, [id]);
    } else if (g.owner_id === userId || g.owner_id == null) {
      await q(`update grp set owner_id = $2 where id = $1`, [id, next.user_id]);
    }
  }

  async function leave(user, id) {
    await memberOf(id, user.id);
    await removeMember(id, user.id);
  }

  async function kick(user, id, mid) {
    const g = await memberOf(id, user.id);
    if (g.owner_id !== user.id) throw err(403, "sahip-degil");
    const m = (await q(`select user_id from grp_member where grp_id = $1 and id = $2`, [id, int(mid)])).rows[0];
    if (!m || m.user_id === user.id) throw err(404, "uye");
    await removeMember(id, m.user_id);
  }

  async function edit(user, id, b) {
    const g = await memberOf(id, user.id);
    if (g.owner_id !== user.id) throw err(403, "sahip-degil");
    if (b.ad != null) {
      const n = cleanName(b.ad);
      if (n.hata) throw err(400, n.hata);
      await q(`update grp set name = $2, name_hidden = false where id = $1`, [id, n.ad]);
      await q(`delete from grp_report where grp_id = $1 and target = 'ad'`, [id]);
    }
    if (g.kind === "geri-sayim" || g.kind === "oylama") {
      const keys = g.kind === "geri-sayim" ? ["hedef", "yillik", "simge"] : ["sure", "saatler", "kapali"];
      if (keys.some((k) => b[k] != null)) await q(`update grp set data = $2 where id = $1`, [id, groupData(g.kind, b, g.data)]);
    }
    if (b.kodYenile === true) await q(`update grp set code = $2 where id = $1`, [id, newCode()]);
  }

  async function remove(user, id) {
    const g = await memberOf(id, user.id);
    if (g.owner_id !== user.id) throw err(403, "sahip-degil");
    await q(`delete from grp where id = $1`, [id]);
  }

  /* Bildir: grup adı, üyenin görünen adı, dünya grubundaki şehir adı ya da alarm isteğinin etiketi */
  async function report(user, id, b) {
    const g = await memberOf(id, user.id);
    let target, text, hide;
    if (b.istek != null) {
      const a = (await q(`select id, label from grp_alarm where grp_id = $1 and id = $2 and to_id = $3`, [id, int(b.istek), user.id])).rows[0];
      if (!a) throw err(404, "istek");
      target = `istek:${a.id}`; text = a.label || "";
      hide = () => q(`update grp_alarm set label = null where id = $1`, [a.id]);
    } else if (b.uye != null) {
      const m = (await q(`select m.id, m.user_id, m.data, u.profile, u."uyeNo" from grp_member m join "user" u on u.id = m.user_id where m.grp_id = $1 and m.id = $2`, [id, int(b.uye)])).rows[0];
      if (!m || m.user_id === user.id) throw err(404, "uye");
      target = `uye:${m.user_id}`;
      text = nameOf({ ...m, name_hidden: false }, "tr") + (m.data?.sehir ? ` (şehir: ${m.data.sehir})` : "");
      hide = async () => {
        await q(`update grp_member set name_hidden = true where grp_id = $1 and user_id = $2`, [id, m.user_id]);
        if (m.data?.sehir) await q(`update grp_member set data = data - 'sehir' where grp_id = $1 and user_id = $2`, [id, m.user_id]);
      };
    } else {
      target = "ad"; text = g.name;
      hide = () => q(`update grp set name_hidden = true where id = $1`, [id]);
    }
    const ins = await q(`insert into grp_report (grp_id, reporter_id, target) values ($1, $2, $3) on conflict do nothing`, [id, user.id, target]);
    if (!ins.rowCount) return { yeni: false };
    const n = (await q(`select count(*)::int n from grp_report where grp_id = $1 and target = $2`, [id, target])).rows[0].n;
    // Alarm isteğinin etiketi yalnız alıcıya görünür: tek bildirimde kalkar
    const hidden = target.startsWith("istek:") || n >= LIMITS.reportsToHide;
    if (hidden) await hide();
    const what = target === "ad" ? "grup adı" : target.startsWith("istek:") ? "alarm isteğinin etiketi" : "üyenin görünen adı";
    await mailer.sendRaw({
      to: adminEmail || "bilgi@bumbagroup.com",
      subject: `Saat Tarih grup bildirimi: ${what}${hidden ? " (gizlendi)" : ""}`,
      text: `Bildirilen ${what}: ${text}\nGrup: ${id} (${g.kind})\nBu içerik için bildirim sayısı: ${n}${hidden ? "\nKendiliğinden gizlendi." : ""}\n\nYanıt süresi: 24–72 saat.\n`,
    }).catch(() => {});
    return { yeni: true, gizlendi: hidden };
  }

  async function block(user, id, mid, on = true) {
    await memberOf(id, user.id);
    const m = (await q(`select user_id from grp_member where grp_id = $1 and id = $2`, [id, int(mid)])).rows[0];
    if (!m || m.user_id === user.id) throw err(404, "uye");
    if (on) {
      await q(`insert into circle_block (user_id, blocked_id) values ($1, $2) on conflict do nothing`, [user.id, m.user_id]);
      await q(`update grp_alarm set state = 'reddedildi' where to_id = $1 and from_id = $2 and state = 'bekliyor'`, [user.id, m.user_id]);
    } else await q(`delete from circle_block where user_id = $1 and blocked_id = $2`, [user.id, m.user_id]);
  }

  /* Anonim hava bildirimi: kişi başına 10 dakikada bir, günde 20; son 60 dakikanın sayıları (kişi yazılmaz) */
  async function weatherReport(user, b) {
    if (!WEATHER.includes(b.tur)) throw err(400, "tur");
    const cell = cellOf(b.enlem, b.boylam);
    if (!cell) throw err(400, "konum");
    const r = (await q(`select count(*) filter (where at > now() - interval '10 minutes')::int a, count(*)::int d from weather_report where user_id = $1 and at > now() - interval '1 day'`, [user.id])).rows[0];
    if (r.a >= LIMITS.weatherPer10m || r.d >= LIMITS.weatherPerDay) throw err(429, "sinir");
    await q(`insert into weather_report (user_id, cell, kind) values ($1, $2, $3)`, [user.id, cell, b.tur]);
    return weatherNear(b.enlem, b.boylam);
  }

  async function weatherNear(lat, lon) {
    const cell = cellOf(lat, lon);
    if (!cell) throw err(400, "konum");
    const rows = (await q(`select kind, count(*)::int n from weather_report where cell = $1 and at > now() - interval '60 minutes' group by kind`, [cell])).rows;
    return { sayilar: Object.fromEntries(rows.map((r) => [r.kind, r.n])), dakika: 60 };
  }

  async function onUserGone(userId) {
    const ids = (await q(`select grp_id from grp_member where user_id = $1`, [userId])).rows.map((x) => x.grp_id);
    for (const id of ids) await removeMember(id, userId, "silindi");
  }

  async function exportOf(userId) {
    const rows = (await q(
      `select g.id, g.kind, g.name, m.joined_at, m.data, g.owner_id = $1 as owner from grp_member m join grp g on g.id = m.grp_id where m.user_id = $1`,
      [userId],
    )).rows;
    const alarms = (await q(`select grp_id, at, days, label, state, created_at, from_id = $1 as sent from grp_alarm where from_id = $1 or to_id = $1`, [userId])).rows;
    const weather = (await q(`select cell, kind, at from weather_report where user_id = $1`, [userId])).rows;
    return {
      gruplar: rows.map((r) => ({ id: r.id, tur: r.kind, ad: r.name, katilma: new Date(r.joined_at).toISOString(), kurucu: r.owner, verim: r.data })),
      alarmIstekleri: alarms.map((a) => ({ grup: a.grp_id, gonderdim: a.sent, saat: a.at, gunler: a.days, etiket: a.label, durum: a.state, zaman: new Date(a.created_at).toISOString() })),
      havaBildirimleri: weather.map((w) => ({ hucre: w.cell, tur: w.kind, zaman: new Date(w.at).toISOString() })),
    };
  }

  /* Temizlik: üyesiz gruplar 30 gün, hava bildirimleri 24 saat, yanıtlanmış ya da eski alarm istekleri 30 gün sonra */
  async function cleanup(now = new Date()) {
    const g = (await q(`delete from grp where orphan_at is not null and orphan_at < $1::timestamptz - interval '30 days'`, [now])).rowCount;
    const w = (await q(`delete from weather_report where at < $1::timestamptz - interval '1 day'`, [now])).rowCount;
    const a = (await q(`delete from grp_alarm where created_at < $1::timestamptz - interval '30 days'`, [now])).rowCount;
    return { gruplar: g, hava: w, alarm: a };
  }

  async function counts() {
    const r = (await q(`select kind, count(*)::int n from grp group by kind`)).rows;
    return Object.fromEntries(r.map((x) => [x.kind, x.n]));
  }

  return { create, preview, join, detail, list, setPlace, vote, focus, watched, alarmRequest, alarmAnswer, leave, kick, edit, remove, report, block, weatherReport, weatherNear, onUserGone, exportOf, cleanup, counts };
}
