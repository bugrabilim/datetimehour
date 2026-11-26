/* Yönetim paneline veri veren uçlar (standart madde 10, "Ürün uçları", 5.16). Panel ürünü doğrudan çağırmaz; ortak
   üyelik servisi aktarır: `POST /api/bumba-panel/<islem>`, `Authorization: Bearer <UYELIK_ANAHTARI>` (sabit sürede
   karşılaştırılır, tutmazsa 401) ve `X-Bumba-Site`. Yalnız POST, JSON, CORS yok, en geç 8 saniye. Ölçülemeyen alan
   `null` döner (panel "bu ortamda ölçülemiyor" yazar). Yönetici hesabı üye değildir, sayılara ve listelere girmez.
   Askı ve yasak buradan değil, ortak servisin `durum` işlemiyle uygulanır. */
import { readFileSync } from "node:fs";
import { keyOk } from "./linked.mjs";
import { SITE_KEY, TERMS_VERSION } from "./config.mjs";
import { profileOf } from "./accounts.mjs";

const OPS = new Set(["ozet", "uyeler", "uye"]);
const RANGES = { bugun: 1, "7g": 7, "30g": 30 };
const AGE_GROUPS = ["16-24", "25-34", "35-44", "45-54", "55+"];

export function ageGroup(birth, now = new Date()) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(birth || ""));
  if (!m) return "bilinmiyor";
  let age = now.getUTCFullYear() - Number(m[1]);
  const md = (now.getUTCMonth() + 1) * 100 + now.getUTCDate();
  if (md < Number(m[2]) * 100 + Number(m[3])) age--;
  if (age < 16) return "bilinmiyor";
  if (age <= 24) return "16-24";
  if (age <= 34) return "25-34";
  if (age <= 44) return "35-44";
  if (age <= 54) return "45-54";
  return "55+";
}

/* Görünen ad (madde 7): takma ad, ad (+ soyad), yoksa "Üye <no>". Panelde görünürlükten bağımsız (yönetici her şeyi görür). */
function displayName(user) {
  const p = profileOf(user);
  if (p.takmaAd) return p.takmaAd;
  if (p.ad) return [p.ad, p.soyad].filter(Boolean).join(" ");
  return user.uyeNo != null ? `Üye ${user.uyeNo}` : "";
}
const statusOf = (u) => (!u.banned ? "aktif" : u.banKind === "aski" ? "askida" : "yasakli");
const iso = (d) => (d ? new Date(d).toISOString() : null);

function deviceOf(s) {
  if (s.client === "ios") return "iOS";
  if (s.client === "android") return "Android";
  const ua = String(s.userAgent || "");
  const os = /iPhone|iPad/.test(ua) ? "iOS" : /Android/.test(ua) ? "Android" : /Mac OS X/.test(ua) ? "macOS" : /Windows/.test(ua) ? "Windows" : /Linux/.test(ua) ? "Linux" : "";
  const br = /Edg\//.test(ua) ? "Edge" : /Firefox\//.test(ua) ? "Firefox" : /Chrome\//.test(ua) ? "Chrome" : /Safari\//.test(ua) ? "Safari" : "";
  return [br, os].filter(Boolean).join(" · ") || null;
}

/* Rozet adları (5.22, isteğe bağlı): panel tr ve en gösterir; kaynak uygulamalardaki badges.items.*.name */
const WEB = JSON.parse(readFileSync(new URL("../data/web-texts.json", import.meta.url), "utf8"));
const badgeName = (id) => (WEB.badges.tr?.[id] || WEB.badges.en?.[id] ? { tr: WEB.badges.tr?.[id] || WEB.badges.en[id], en: WEB.badges.en?.[id] || WEB.badges.tr[id] } : null);

export function panelRoutes({ cfg, pool, accounts, records, mailer, metrics, relay, json, readJson }) {
  const admin = String(cfg.adminEmail || "").toLowerCase();
  const members = `"emailVerified" = true and lower(email) <> $1`;

  async function ozet(b) {
    const days = RANGES[b.aralik] || 1;
    const since = new Date(Date.now() - (days - 1) * 864e5);
    const sinceDay = metrics.dayOf(since);
    const startOfFirst = new Date(`${sinceDay}T00:00:00+03:00`);
    const all = await pool.query(`select profile from "user" where ${members}`, [admin]);
    const ulkeyeGore = {};
    const yasGrubunaGore = Object.fromEntries([...AGE_GROUPS, "bilinmiyor"].map((g) => [g, 0]));
    for (const r of all.rows) {
      const p = profileOf(r);
      const c = /^[A-Z]{2}$/.test(p.ulke || "") ? p.ulke : "bilinmiyor";
      ulkeyeGore[c] = (ulkeyeGore[c] || 0) + 1;
      yasGrubunaGore[ageGroup(p.dogumTarihi)]++;
    }
    const yeni = await pool.query(`select count(*)::int n from "user" where ${members} and "createdAt" >= $2`, [admin, startOfFirst]);
    await metrics.flush();
    const m = (await pool.query(`select coalesce(sum(requests),0)::bigint r, coalesce(sum(errors),0)::bigint e, coalesce(sum(total_ms),0)::bigint t from metric_day where day >= $1`, [sinceDay])).rows[0];
    const istek = Number(m.r);
    const db = await pool.query(`select pg_database_size(current_database())::bigint b`).then((r) => Number(r.rows[0].b), () => null);
    return {
      uyeSayisi: all.rowCount,
      yeniKayit: yeni.rows[0].n,
      ulkeyeGore,
      yasGrubunaGore,
      aramalar: null, // sitede üye araması yok
      olculer: {
        istek,
        yanitSuresiMs: istek ? Math.round(Number(m.t) / istek) : null,
        hataOrani: istek ? Number(m.e) / istek : null,
        veritabaniBayt: db,
        depolamaBayt: null, // ayrı dosya deposu yok; fotoğraflar veritabanında
      },
      servisler: { veritabani: db !== null, eposta: mailer.state?.lastOk ?? null },
      // Merkeze gönderilemeyen guncelle ve ayril (5.22; panel enEski 7 günü geçince uyarır)
      merkezeBekleyen: relay ? await relay.pending() : { sayi: 0, enEski: null },
      // Üyelere en çok verilen rozetler (5.22, isteğe bağlı; en çok 20)
      rozetler: (await pool.query(
        `select b.badge, count(*)::int n from user_badge b join "user" u on u.id = b.user_id where u."emailVerified" = true and lower(u.email) <> $1 group by b.badge order by n desc, b.badge limit 20`,
        [admin],
      )).rows.map((r) => ({ kimlik: r.badge, ad: badgeName(r.badge), sayi: r.n })).map((r) => (r.ad ? r : { kimlik: r.kimlik, sayi: r.sayi })),
      surum: { commit: cfg.commit || null, tarih: cfg.commitDate || null },
    };
  }

  async function uyeler(b) {
    const size = Math.min(100, Math.max(1, Number(b.sayfaBoyu) || 20));
    const page = Math.max(1, Number(b.sayfa) || 1);
    const where = [members];
    const args = [admin];
    if (b.ara) {
      args.push(`%${String(b.ara).toLowerCase().slice(0, 100)}%`);
      const n = args.length;
      where.push(`(lower(email) like $${n} or "uyeNo"::text like $${n} or lower(coalesce(profile::text, '')) like $${n})`);
    }
    if (b.durum === "aktif") where.push(`coalesce(banned, false) = false`);
    if (b.durum === "askida") where.push(`banned = true and "banKind" = 'aski'`);
    if (b.durum === "yasakli") where.push(`banned = true and coalesce("banKind", 'yasak') <> 'aski'`);
    if (/^[A-Z]{2}$/.test(String(b.ulke || ""))) { args.push(b.ulke); where.push(`(profile::jsonb ->> 'ulke') = $${args.length}`); }
    // Son giriş (5.28): girişte yazılan zaman; ondan önce açılmış hesaplarda en yeni oturumun açılışı
    const last = `coalesce(u."lastLoginAt", (select max("createdAt") from session s where s."userId" = u.id))`;
    const order = b.siralama === "eski" ? `u."createdAt" asc` : b.siralama === "sonGiris" ? `${last} desc nulls last` : `u."createdAt" desc`;
    const w = where.join(" and ");
    const total = (await pool.query(`select count(*)::int n from "user" u where ${w}`, args)).rows[0].n;
    const rows = (await pool.query(`select u.*, ${last} as "lastLogin" from "user" u where ${w} order by ${order} limit ${size} offset ${(page - 1) * size}`, args)).rows;
    return {
      toplam: total,
      uyeler: rows.map((u) => ({
        uyeNo: u.uyeNo != null ? Number(u.uyeNo) : null,
        eposta: u.email,
        gorunenAd: displayName(u),
        durum: statusOf(u),
        kayit: iso(u.createdAt),
        sonGiris: iso(u.lastLogin),
        ulke: profileOf(u).ulke || null,
      })),
    };
  }

  async function uye(b) {
    const user = Number.isInteger(Number(b.uyeNo)) ? await accounts.userByNo(Number(b.uyeNo)) : null;
    if (!user || String(user.email).toLowerCase() === admin) return null;
    const ss = (await pool.query(`select "createdAt", "updatedAt", "userAgent", client from session where "userId" = $1 and "expiresAt" > now() order by "createdAt" desc`, [user.id])).rows;
    const last = (await pool.query(`select coalesce(u."lastLoginAt", (select max("createdAt") from session s where s."userId" = u.id)) m from "user" u where u.id = $1`, [user.id])).rows[0]?.m;
    const kayit = await records.list(user.id);
    return {
      hesap: await accounts.toHesap(user, { withPassword: false }),
      sonGiris: iso(last),
      oturumlar: ss.map((s) => ({ cihaz: deviceOf(s), acilis: iso(s.createdAt), sonKullanim: iso(s.updatedAt) })),
      rozetler: (await records.badges(user.id)).map((r) => { const ad = badgeName(r.id); return ad ? { kimlik: r.id, ad, gun: r.gun } : { kimlik: r.id, gun: r.gun }; }),
      kullanim: { kayitSayisi: Object.keys(kayit).length, dil: user.lang, kutlama: user.celebrate !== false },
      onay: { zaman: iso(user.consentAt), surum: user.consentVersion || TERMS_VERSION },
    };
  }

  return async function handle(req, res, op) {
    res.setHeader("Cache-Control", "no-store");
    if (!OPS.has(op) || req.method !== "POST") return json(res, 404, { hata: "yok" });
    if (!keyOk(req.headers.authorization, cfg.centerKey)) return json(res, 401, { hata: "yetki" });
    if (req.headers["x-bumba-site"] && req.headers["x-bumba-site"] !== SITE_KEY) return json(res, 401, { hata: "site" });
    const b = await readJson(req);
    if (op === "ozet") {
      if (b.aralik !== undefined && !RANGES[b.aralik]) return json(res, 400, { hata: "aralik" });
      return json(res, 200, await ozet(b));
    }
    if (op === "uyeler") return json(res, 200, await uyeler(b));
    const out = await uye(b);
    if (!out) return json(res, 404, { hata: "uye" });
    return json(res, 200, out);
  };
}
