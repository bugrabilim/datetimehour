/* Kimlik doğrulama: Better Auth (denenmiş çözüm; şifre özeti ve oturum mantığı elle yazılmaz, standart madde 7).
   Giriş yalnız e-posta ve şifreyle (10 Kasım 2026, sahibin kararı: Google ya da başka giriş yok).
   Uygulamalar taşıyıcı belirteç (bearer) kullanır; panel httpOnly + secure oturum çereziyle. */
import { betterAuth } from "better-auth";
import { bearer } from "better-auth/plugins/bearer";
import { siteLink, normLang } from "./config.mjs";

/* Better Auth'un ürettiği bağlantıdaki dönüş adresini sitedeki sayfayla değiştirir */
function withCallback(url, callback) {
  const u = new URL(url);
  u.searchParams.set("callbackURL", callback);
  return u.toString();
}

export function authOptions(cfg, pool, mailer) {
  const verified = (lang, step) => siteLink(cfg, lang, "verified") + (step ? `?adim=${step}` : "");
  const send = (kind, to, lang, vars) => mailer.send(kind, to, lang, vars);
  const isAdminEmail = (email) => String(email || "").toLowerCase() === cfg.adminEmail;
  const seen = (userId) => pool.query(
    `insert into user_activity (user_id, last_seen, warned_at) values ($1, now(), null)
     on conflict (user_id) do update set last_seen = now(), warned_at = null`,
    [userId],
  ).catch(() => {});

  return {
    appName: "Saat Tarih",
    baseURL: cfg.publicUrl,
    basePath: "/api/auth",
    secret: cfg.secret,
    database: pool,
    telemetry: { enabled: false },
    trustedOrigins: [cfg.siteUrl, cfg.publicUrl],
    emailAndPassword: {
      enabled: true,
      requireEmailVerification: true,
      autoSignIn: false,
      minPasswordLength: 8,
      maxPasswordLength: 128,
      resetPasswordTokenExpiresIn: 60 * 60,
      revokeSessionsOnPasswordReset: true,
      sendResetPassword: async ({ user, token }) => {
        const url = `${siteLink(cfg, user.lang, "password")}?token=${encodeURIComponent(token)}`;
        await send("reset", user.email, user.lang, { url });
      },
      // Aynı adresle yeniden kayıt denemesi: yanıt kayıtla aynıdır (hesabın varlığı ele verilmez), sahibine haber gider
      onExistingUserSignUp: async ({ user }) => {
        await send("exists", user.email, user.lang, {});
      },
    },
    emailVerification: {
      sendOnSignUp: true,
      sendOnSignIn: true,
      autoSignInAfterVerification: false,
      expiresIn: 24 * 60 * 60,
      sendVerificationEmail: async ({ user, url }) => {
        // Adres değişikliğinde yeni adrese giden bağlantı da buradan geçer (kullanıcının kayıtlı adresi farklıdır)
        const change = new URL(url).searchParams.get("callbackURL")?.includes("adim=") ?? false;
        await send("verify", user.email, user.lang, { url: withCallback(url, verified(user.lang, change ? "eposta" : "")) });
      },
      afterEmailVerification: async (user) => {
        if (isAdminEmail(user.email)) await pool.query(`update "user" set role = 'admin' where id = $1`, [user.id]);
      },
    },
    user: {
      additionalFields: {
        lang: { type: "string", required: false, defaultValue: "en", input: true },
        role: { type: "string", required: false, defaultValue: "user", input: false },
      },
      changeEmail: {
        enabled: true,
        // Doğrulanmış hesapta önce eski adrese onay gider; onaylanınca yeni adrese doğrulama
        sendChangeEmailConfirmation: async ({ user, newEmail, url }) => {
          await send("changeOld", user.email, user.lang, { email: newEmail, url: withCallback(url, verified(user.lang, "eposta-onay")) });
        },
      },
      deleteUser: {
        enabled: true,
        afterDelete: async (user) => {
          await send("deleted", user.email, user.lang, {}).catch(() => {});
        },
      },
    },
    session: {
      expiresIn: 90 * 24 * 60 * 60,
      updateAge: 24 * 60 * 60,
    },
    rateLimit: {
      enabled: cfg.rateLimit,
      storage: "database",
      window: 60,
      max: 60,
      customRules: {
        "/sign-in/email": { window: 60, max: 5 },
        "/sign-up/email": { window: 60 * 60, max: 10 },
        "/request-password-reset": { window: 60 * 60, max: 5 },
        "/send-verification-email": { window: 60 * 60, max: 5 },
        "/change-password": { window: 60, max: 5 },
        "/change-email": { window: 60 * 60, max: 5 },
        "/delete-user": { window: 60, max: 5 },
        "/reset-password": { window: 60, max: 10 },
      },
    },
    advanced: {
      useSecureCookies: cfg.production,
      cookiePrefix: "saat",
      // Traefik X-Real-Ip başlığını kendisi yazar; X-Forwarded-For istemcinin eklediği değeri taşıyabildiği için okunmaz
      ipAddress: { ipAddressHeaders: ["x-real-ip"] },
    },
    databaseHooks: {
      // Son kullanım: oturum açılınca ve günde bir yenilenince (saklama süresi, cleanup.mjs); uyarı varsa kalkar
      session: {
        create: { after: async (session) => { await seen(session.userId); } },
        update: { after: async (session) => { if (session?.userId) await seen(session.userId); } },
      },
      user: {
        create: {
          // Dil uygulamanın gönderdiği koddan, bilinmeyen kod İngilizce
          before: async (user) => ({ data: { ...user, lang: normLang(user.lang) } }),
        },
        update: {
          before: async (data) => (data.lang !== undefined ? { data: { ...data, lang: normLang(data.lang) } } : { data }),
        },
      },
    },
    plugins: [bearer()],
  };
}

export const createAuth = (cfg, pool, mailer) => betterAuth(authOptions(cfg, pool, mailer));
