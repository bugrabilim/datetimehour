/* Ürünün kurduğu uçlar (standart madde 7, "Ortak üyelik servisi" › Ürünün kurduğu uçlar): ortak servis çağırır,
   `POST /api/bumba-uyelik/<işlem>`, `Authorization: Bearer <UYELIK_ANAHTARI>` (sabit sürede karşılaştırılır, tutmazsa
   401). Yalnız sunucudan sunucuya (CORS yok); bilinmeyen üye numarasında 404. */
import crypto from "node:crypto";

const OPS = new Set(["dogrula", "hesap-getir", "hesap-ac", "guncelle", "hesap-sil", "durum", "numara-degistir"]);

export function keyOk(header, key) {
  if (!key) return false;
  const got = Buffer.from(String(header || ""));
  const want = Buffer.from(`Bearer ${key}`);
  return got.length === want.length && crypto.timingSafeEqual(got, want);
}

export function linkedRoutes({ cfg, auth, accounts, mailer, json, readJson }) {
  const ctxP = auth.$context;
  const langOf = (u, h) => u?.lang || (h?.profil?.ulke === "TR" ? "tr" : "en");

  return async function handle(req, res, op) {
    if (!OPS.has(op) || req.method !== "POST") return json(res, 404, { hata: "yok" });
    if (!keyOk(req.headers.authorization, cfg.centerKey)) return json(res, 401, { hata: "yetki" });
    const b = await readJson(req, 2 * 1024 * 1024);
    const ctx = await ctxP;

    if (op === "hesap-ac") {
      const h = b.hesap || {};
      if (!h.eposta || !Number.isInteger(Number(h.uyeNo))) return json(res, 400, { hata: "hesap" });
      if (await accounts.userByEmail(h.eposta)) return json(res, 200, { sonuc: "var" });
      const lang = langOf(null, h);
      const u = await accounts.createFromHesap(h, { lang });
      if (!u) return json(res, 200, { sonuc: "var" });
      await mailer.send("linkedOpened", u.email, lang, {}).catch(() => {});
      return json(res, 200, { sonuc: "acildi" });
    }

    const no = Number(op === "numara-degistir" ? b.eskiNo : b.uyeNo);
    const user = Number.isInteger(no) ? await accounts.userByNo(no) : null;
    if (!user) return json(res, 404, { hata: "uye" });

    if (op === "dogrula") {
      if (user.banned || String(user.email).toLowerCase() !== String(b.eposta || "").toLowerCase()) return json(res, 200, { gecerli: false });
      const hash = await accounts.passwordHash(user.id);
      const ok = hash ? await ctx.password.verify({ hash, password: String(b.sifre || "") }).catch(() => false) : false;
      if (!ok) return json(res, 200, { gecerli: false });
      return json(res, 200, { gecerli: true, hesap: await accounts.toHesap(user) });
    }
    if (op === "hesap-getir") return json(res, 200, { hesap: await accounts.toHesap(user) });
    if (op === "guncelle") {
      await accounts.applyChange(user, b.degisiklik || {});
      return json(res, 200, { sonuc: "guncellendi" });
    }
    if (op === "hesap-sil") {
      await accounts.deleteLocal(user.id);
      if (b.bildir) await mailer.send("linkedClosed", user.email, user.lang, {}).catch(() => {});
      return json(res, 200, { sonuc: "silindi" });
    }
    if (op === "durum") {
      const durum = String(b.durum || "");
      if (!["aktif", "askida", "yasakli"].includes(durum)) return json(res, 400, { hata: "durum" });
      if (durum !== "aktif" && !b.sebep) return json(res, 400, { hata: "sebep" });
      await accounts.setStatus(user, durum, b.sebep, b.bitis);
      if (b.bildir) {
        const kind = durum === "aktif" ? "reopened" : durum === "askida" ? "suspended" : "banned";
        await mailer.send(kind, user.email, user.lang, { reason: b.sebep, until: durum === "askida" ? b.bitis : null }).catch(() => {});
      }
      return json(res, 200, { sonuc: durum });
    }
    if (op === "numara-degistir") {
      const yeni = Number(b.yeniNo);
      if (!Number.isInteger(yeni)) return json(res, 400, { hata: "yeniNo" });
      await accounts.ia().then((a) => a.updateUser(user.id, { uyeNo: yeni, updatedAt: new Date() }));
      const h = b.hesap || {};
      await accounts.applyChange({ ...user, uyeNo: yeni }, { sifreOzeti: h.sifreOzeti || undefined, profil: h.profil || undefined });
      return json(res, 200, { sonuc: "degisti" });
    }
    return json(res, 404, { hata: "yok" });
  };
}
