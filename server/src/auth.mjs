/* Kimlik doğrulama: Better Auth (standart madde 7; şifre, oturum ve doğrulama mantığı elle yazılmaz).
   Giriş yalnız e-posta + şifre. Web httpOnly çerezle (30 gün), uygulamalar bearer eklentisiyle (90 gün).
   Bağlı üyelik: kayıtta ve e-posta değişikliğinde ortak servise sorulur; numara e-posta doğrulanınca `kayit`'tan alınır. */
import crypto from "node:crypto";
import { betterAuth } from "better-auth";
import { APIError, createAuthMiddleware } from "better-auth/api";
import { bearer } from "better-auth/plugins/bearer";
import { admin } from "better-auth/plugins/admin";
import { normLang, TERMS_VERSION } from "./config.mjs";
import { passwordOk } from "./profile.mjs";

const DAY = 24 * 60 * 60;
export const WEB_SESSION_DAYS = 30;
export const APP_SESSION_DAYS = 90;
const THEMES = ["system", "light", "dark"];

/* Uygulama isteği: taşıyıcı belirteçle gelir ya da girişte X-Saat-Istemci (ios|android) başlığını taşır */
function clientOf(headers) {
  if (!headers?.get) return null;
  const c = String(headers.get("x-saat-istemci") || "").toLowerCase();
  if (c === "ios" || c === "android") return c;
  if (/^bearer\s/i.test(headers.get("authorization") || "")) return "app";
  return null;
}
const validTz = (tz) => {
  try { new Intl.DateTimeFormat("en", { timeZone: tz }); return true; } catch { return false; }
};

export function authOptions(cfg, pool, mailer, center) {
  const send = (kind, to, lang, vars) => mailer.send(kind, to, lang, vars);
  const isAdminEmail = (email) => String(email || "").toLowerCase() === cfg.adminEmail;
  const page = (path, lang, extra = "") => `${cfg.publicUrl}${path}?dil=${normLang(lang)}${extra}`;
  const hashOf = async (userId) =>
    (await pool.query(`select password from account where "userId" = $1 and "providerId" = 'credential'`, [userId])).rows[0]?.password || null;

  /* Şifre değişince: üyeye haber, bağlı ürünlere yeni özet */
  async function passwordChanged(user) {
    await send("passwordChanged", user.email, user.lang, {}).catch(() => {});
    if (user.uyeNo != null) {
      const h = await hashOf(user.id);
      if (h) await center.guncelle(Number(user.uyeNo), { sifreOzeti: h });
    }
  }

  const before = createAuthMiddleware(async (ctx) => {
    const p = ctx.path;
    if (p === "/sign-up/email") {
      if (!passwordOk(ctx.body?.password)) throw new APIError("BAD_REQUEST", { code: "WEAK_PASSWORD", message: "weak password" });
      // Kayıtta yalnız e-posta ve şifre: ad boş, profil ve öbür alanlar kayıttan sonra hesap sayfasında
      ctx.body.name = "";
      const email = String(ctx.body?.email || "").toLowerCase();
      // Bu üründe zaten kayıtlıysa Better Auth'un sıradan akışı (yanıt aynı, sahibine haber); hesabın varlığı ele verilmez
      const local = (await pool.query(`select 1 from "user" where lower(email) = $1`, [email])).rowCount > 0;
      const q = local ? { ok: false } : await center.sorgula(email);
      if (q.ok && q.data?.durum === "var" && !q.data?.buradaAcik) throw new APIError("BAD_REQUEST", { code: "BUMBA_MEMBER", message: "bumba member" });
      // Yasaklı adres: hesap açılmaz, yanıt sıradan kayıttaki gibi
      if (q.ok && q.data?.durum === "yasakli") return ctx.json({ token: null, user: null });
    }
    if (p === "/reset-password" || p === "/change-password") {
      if (!passwordOk(ctx.body?.newPassword)) throw new APIError("BAD_REQUEST", { code: "WEAK_PASSWORD", message: "weak password" });
    }
    if (p === "/change-email") {
      const q = await center.sorgula(String(ctx.body?.newEmail || "").toLowerCase());
      if (q.ok && q.data?.durum !== "yok") throw new APIError("BAD_REQUEST", { code: "EMAIL_TAKEN", message: "email taken" });
    }
    if (p === "/update-user") {
      const b = ctx.body || {};
      if (b.theme !== undefined && !THEMES.includes(b.theme)) throw new APIError("BAD_REQUEST", { code: "INVALID_THEME", message: "theme" });
      if (b.timeZone !== undefined && b.timeZone !== null && !validTz(b.timeZone)) throw new APIError("BAD_REQUEST", { code: "INVALID_TZ", message: "tz" });
      // Ad ve görüntü Better Auth alanları kullanılmaz: profil kendi ucunda doğrulanır
      delete b.name;
      delete b.image;
    }
  });

  const after = createAuthMiddleware(async (ctx) => {
    if (ctx.path === "/change-password" && ctx.context.returned && !(ctx.context.returned instanceof Error)) {
      const s = ctx.context.session;
      if (s?.user) await passwordChanged(s.user);
    }
  });

  return {
    appName: "Saat Tarih",
    baseURL: cfg.publicUrl,
    basePath: "/api/auth",
    secret: cfg.secret,
    database: pool,
    telemetry: { enabled: false },
    trustedOrigins: [cfg.publicUrl],
    emailAndPassword: {
      enabled: true,
      requireEmailVerification: true,
      autoSignIn: false,
      minPasswordLength: 8,
      maxPasswordLength: 128,
      resetPasswordTokenExpiresIn: 60 * 60,
      revokeSessionsOnPasswordReset: true,
      sendResetPassword: async ({ user, token }) => {
        await send("reset", user.email, user.lang, { url: page("/sifre", user.lang, `&token=${encodeURIComponent(token)}`) });
      },
      onPasswordReset: async ({ user }) => { await passwordChanged(user); },
      // Aynı adresle yeniden kayıt denemesi: yanıt kayıtla aynıdır (hesabın varlığı ele verilmez), sahibine haber gider
      onExistingUserSignUp: async ({ user }) => { await send("exists", user.email, user.lang, {}); },
    },
    emailVerification: {
      sendOnSignUp: true,
      sendOnSignIn: true,
      autoSignInAfterVerification: false,
      expiresIn: DAY,
      sendVerificationEmail: async ({ user, url }) => {
        // Doğrulama sonucunu gösteren sayfa: anahtar (k) bu e-postaya özgüdür; sonuç (numara alındı ya da başka
        // Bumba ürününde üyelik var) doğrulama anında kaydedilir, sayfa onu okur
        const k = crypto.randomBytes(18).toString("base64url");
        await pool.query(`insert into verify_ref (k, email) values ($1, lower($2))`, [k, user.email]);
        const u = new URL(url);
        u.searchParams.set("callbackURL", page("/dogrulandi", user.lang, `&k=${k}`));
        await send("verify", user.email, user.lang, { url: u.toString() });
      },
      afterEmailVerification: async (user) => {
        if (isAdminEmail(user.email)) await pool.query(`update "user" set role = 'admin' where id = $1`, [user.id]);
        let result = "ok";
        if (user.uyeNo != null) {
          // E-posta değişikliği tamamlandı: bağlı ürünlere yeni adres
          await center.guncelle(Number(user.uyeNo), { eposta: user.email, epostaDogrulandi: true });
          result = "eposta";
        } else {
          const r = await center.kayit(user.email);
          if (r.ok && Number.isInteger(r.data?.no)) {
            await pool.query(`update "user" set "uyeNo" = $1 where id = $2 and "uyeNo" is null`, [r.data.no, user.id]);
          } else if (r.status === 409 || r.status === 403) {
            // Adres başka bir Bumba ürününde kayıtlı (ya da yasaklı): bu hesap açılmaz
            await pool.query(`delete from "user" where id = $1`, [user.id]);
            result = r.status === 409 ? "bumba" : "kapali";
          }
          // Merkeze ulaşılamadıysa hesap açık kalır, numara temizlik görevinde alınır
        }
        await pool.query(`update verify_ref set result = $1 where email = lower($2) and result is null`, [result, user.email]);
      },
    },
    user: {
      additionalFields: {
        lang: { type: "string", required: false, defaultValue: "en", input: true },
        theme: { type: "string", required: false, defaultValue: "system", input: true },
        timeZone: { type: "string", required: false, input: true },
        uyeNo: { type: "number", required: false, input: false, unique: true },
        consentAt: { type: "date", required: false, input: false },
        consentVersion: { type: "string", required: false, input: false },
        celebrate: { type: "boolean", required: false, defaultValue: true, input: false },
        anaUrun: { type: "boolean", required: false, defaultValue: true, input: false },
        profile: { type: "json", required: false, input: false },
        photo: { type: "string", required: false, input: false, returned: false },
        banKind: { type: "string", required: false, input: false },
      },
      changeEmail: {
        enabled: true,
        // Önce eski adrese onay; onaylanınca yeni adrese doğrulama (bağlantı sendVerificationEmail'den)
        sendChangeEmailConfirmation: async ({ user, newEmail, url }) => {
          const u = new URL(url);
          u.searchParams.set("callbackURL", page("/dogrulandi", user.lang, "&adim=eposta-onay"));
          await send("changeOld", user.email, user.lang, { email: newEmail, url: u.toString() });
        },
      },
      // Hesap silme kendi ucumuzda (yalnız bu üründen ya da bütün Bumba ürünlerinden; index.mjs)
      deleteUser: { enabled: false },
    },
    session: {
      expiresIn: WEB_SESSION_DAYS * DAY,
      updateAge: DAY,
      additionalFields: { client: { type: "string", required: false, input: false } },
    },
    rateLimit: {
      enabled: cfg.rateLimit,
      storage: "database",
      customRules: {
        "/sign-in/email": { window: 60, max: 5 },
        "/sign-up/email": { window: 5 * 60, max: 3 },
        "/request-password-reset": { window: 5 * 60, max: 3 },
        "/send-verification-email": { window: 5 * 60, max: 3 },
      },
    },
    advanced: {
      useSecureCookies: cfg.production,
      cookiePrefix: "saat",
      // Traefik X-Real-Ip başlığını kendisi yazar; X-Forwarded-For istemcinin eklediği değeri taşıyabildiği için okunmaz
      ipAddress: { ipAddressHeaders: ["x-real-ip"] },
    },
    hooks: { before, after },
    databaseHooks: {
      session: {
        create: {
          // Oturum kaydına IP yazılmaz; uygulama oturumu 90 gün
          before: async (session, ctx) => {
            const client = clientOf(ctx?.headers || ctx?.request?.headers);
            const data = { ...session, ipAddress: null, client: client === "app" ? "app" : client };
            if (client) data.expiresAt = new Date(Date.now() + APP_SESSION_DAYS * DAY * 1000);
            return { data };
          },
        },
        update: {
          // Günlük yenilemede uygulama oturumu yine 90 gün uzar (web 30 gün)
          before: async (data, ctx) => {
            const d = { ...data };
            if ("ipAddress" in d) d.ipAddress = null;
            if (d.expiresAt && clientOf(ctx?.headers || ctx?.request?.headers)) d.expiresAt = new Date(Date.now() + APP_SESSION_DAYS * DAY * 1000);
            return { data: d };
          },
        },
      },
      user: {
        create: {
          // Dil uygulamanın ya da sayfanın gönderdiği koddan; onay anı ve ortak metinlerin sürümü
          before: async (user) => ({
            data: {
              ...user,
              name: user.name ?? "",
              lang: normLang(user.lang),
              theme: THEMES.includes(user.theme) ? user.theme : "system",
              timeZone: user.timeZone && validTz(user.timeZone) ? user.timeZone : null,
              consentAt: user.consentAt || new Date(),
              consentVersion: user.consentVersion || TERMS_VERSION,
            },
          }),
        },
        update: {
          before: async (data) => (data.lang !== undefined ? { data: { ...data, lang: normLang(data.lang) } } : { data }),
        },
      },
    },
    plugins: [
      bearer(),
      // Askıya alma ve yasak alanları ile giriş engeli (banUser/unbanUser'ın kullandığı alanlar)
      admin({ bannedUserMessage: "BANNED_USER" }),
    ],
  };
}

export const createAuth = (cfg, pool, mailer, center) => betterAuth(authOptions(cfg, pool, mailer, center));
