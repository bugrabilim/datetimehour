/* Yerel hesap işleri (Better Auth'un iç bağdaştırıcısıyla; şifre ve oturum mantığı elle yazılmaz) ve ortak üyelik
   servisinin hesap biçimine çeviri (standart madde 7, "Ortak üyelik servisi" › Hesap). */
import { TERMS_VERSION } from "./config.mjs";
import { validateProfile, checkPhoto } from "./profile.mjs";

export const EMPTY_PROFILE = {
  ad: null, soyad: null, takmaAd: null, dogumTarihi: null, cinsiyet: null, telefon: null, ulke: null, sehir: null,
  meslek: null, hakkimda: null, sosyal: null, gorunurluk: { acik: false, alanlar: [] },
};

const parse = (v) => {
  if (v == null) return null;
  if (typeof v === "object") return v;
  try { return JSON.parse(v); } catch { return null; }
};

export function profileOf(user) {
  return { ...EMPTY_PROFILE, ...(parse(user?.profile) || {}) };
}

export function createAccounts({ pool, auth }) {
  const ctxP = auth.$context;
  const ia = async () => (await ctxP).internalAdapter;

  async function userByNo(uyeNo) {
    const r = await pool.query(`select * from "user" where "uyeNo" = $1`, [Number(uyeNo)]);
    return r.rows[0] || null;
  }
  async function userByEmail(email) {
    const r = await pool.query(`select * from "user" where lower(email) = lower($1)`, [String(email || "")]);
    return r.rows[0] || null;
  }
  async function passwordHash(userId) {
    const r = await pool.query(`select password from account where "userId" = $1 and "providerId" = 'credential'`, [userId]);
    return r.rows[0]?.password || null;
  }
  async function photoOf(userId) {
    const r = await pool.query(`select photo from "user" where id = $1`, [userId]);
    return r.rows[0]?.photo || null;
  }

  /* Ortak biçim: { uyeNo, eposta, epostaDogrulandi, sifreOzeti, profil, kutlama, onay, anaUrun } */
  async function toHesap(user, { withPassword = true } = {}) {
    const profil = { ...profileOf(user), foto: await photoOf(user.id) };
    // Saklı özet Better Auth'un scrypt biçiminde değilse (taşınmış eski özet) null gönderilir
    let sifreOzeti = withPassword ? await passwordHash(user.id) : null;
    if (sifreOzeti && !/^[0-9a-f]+:[0-9a-f]+$/i.test(sifreOzeti)) sifreOzeti = null;
    return {
      uyeNo: user.uyeNo != null ? Number(user.uyeNo) : null,
      eposta: user.email,
      epostaDogrulandi: !!user.emailVerified,
      sifreOzeti,
      profil,
      kutlama: user.celebrate !== false,
      onay: user.consentAt ? { zaman: new Date(user.consentAt).toISOString(), surum: user.consentVersion || TERMS_VERSION } : null,
      anaUrun: user.anaUrun !== false,
    };
  }

  /* Profilin ortak biçimini yerel alanlara yazar (foto ayrı sütunda) */
  async function writeProfile(userId, profil) {
    const { foto, ...rest } = profil || {};
    const clean = validateProfile({ ...EMPTY_PROFILE, ...rest });
    const sets = { profile: JSON.stringify(clean), updatedAt: new Date() };
    if (foto !== undefined) sets.photo = checkPhoto(foto);
    await (await ia()).updateUser(userId, sets);
    return clean;
  }

  /* Gelen ortak hesapla yerel hesap açar (bağlı üyelik). E-posta zaten kayıtlıysa null. */
  async function createFromHesap(h, { lang = "en" } = {}) {
    if (await userByEmail(h.eposta)) return null;
    const a = await ia();
    const user = await a.createUser({
      email: h.eposta,
      name: "",
      emailVerified: true,
      lang,
      uyeNo: Number(h.uyeNo),
      celebrate: h.kutlama !== false,
      anaUrun: h.anaUrun === true,
      consentAt: h.onay?.zaman ? new Date(h.onay.zaman) : new Date(),
      consentVersion: h.onay?.surum || TERMS_VERSION,
    });
    if (h.sifreOzeti) await a.createAccount({ userId: user.id, providerId: "credential", accountId: user.id, password: h.sifreOzeti });
    if (h.profil) await writeProfile(user.id, h.profil).catch(() => writeProfile(user.id, { ...EMPTY_PROFILE }));
    return user;
  }

  /* Ortak biçimdeki değişikliği uygular: eposta, epostaDogrulandi, sifreOzeti, profil, kutlama, anaUrun */
  async function applyChange(user, d) {
    const a = await ia();
    const sets = {};
    if (d.eposta) sets.email = String(d.eposta).toLowerCase();
    if (d.epostaDogrulandi !== undefined) sets.emailVerified = !!d.epostaDogrulandi;
    if (d.kutlama !== undefined) sets.celebrate = !!d.kutlama;
    if (d.anaUrun !== undefined) sets.anaUrun = !!d.anaUrun;
    if (Object.keys(sets).length) await a.updateUser(user.id, { ...sets, updatedAt: new Date() });
    if (d.profil) await writeProfile(user.id, d.profil);
    if (d.sifreOzeti) {
      const acc = await a.findCredentialAccount(user.id);
      if (acc) await a.updateAccount(acc.id, { password: d.sifreOzeti });
      else await a.createAccount({ userId: user.id, providerId: "credential", accountId: user.id, password: d.sifreOzeti });
      await a.deleteUserSessions(user.id);
    }
  }

  async function deleteLocal(userId) {
    await (await ia()).deleteUser(userId);
  }

  /* Askı ve yasak: Better Auth yönetici eklentisinin alanları (banned, banReason, banExpires); giriş engelini eklenti
     uygular. Yasakta profil, fotoğraf, kullanım verisi, şifre ve oturumlar silinir; e-posta, numara, sebep ve tarih kalır. */
  async function setStatus(user, durum, sebep, bitis) {
    const a = await ia();
    if (durum === "aktif") {
      await a.updateUser(user.id, { banned: false, banReason: null, banExpires: null, banKind: null, updatedAt: new Date() });
      return;
    }
    const permanent = durum === "yasakli";
    await a.updateUser(user.id, {
      banned: true,
      banReason: String(sebep || "").slice(0, 500) || null,
      banExpires: !permanent && bitis ? new Date(bitis) : null,
      banKind: permanent ? "yasak" : "aski",
      updatedAt: new Date(),
    });
    await a.deleteUserSessions(user.id);
    if (permanent) {
      await a.updateUser(user.id, { profile: null, photo: null, name: "", timeZone: null });
      await pool.query(`delete from account_record where user_id = $1`, [user.id]);
      await pool.query(`delete from user_badge where user_id = $1`, [user.id]);
      await pool.query(`delete from account where "userId" = $1`, [user.id]);
    }
  }

  return { userByNo, userByEmail, passwordHash, toHesap, writeProfile, createFromHesap, applyChange, deleteLocal, setStatus, ia };
}
