/* Web'deki giriş, kayıt ve hesap sayfaları (standart madde 7: uygulaması olan projede web'de de giriş, kayıt ve hesap
   sayfası; statik sitede bunlar üyelik sunucusunda). 19 dilde, iki temada, noindex. Sayfa iskeletini sunucu verir,
   işleri /varlik/hesap.js yapar (aynı kaynaktan JSON istekleri). Satır içi betik ve stil yok. */
import { readFileSync } from "node:fs";
import { LANGS, RTL, normLang, PRODUCT_NAME } from "./config.mjs";
import { readUnsubscribeToken } from "./mail.mjs";
import { COUNTRIES, SOCIAL, VISIBLE_FIELDS, birthDateRange } from "./profile.mjs";

export const UI = JSON.parse(readFileSync(new URL("../i18n/account.json", import.meta.url), "utf8"));
const ASSETS = {
  "/varlik/hesap.js": ["text/javascript; charset=utf-8", new URL("../web/hesap.js", import.meta.url)],
  "/varlik/hesap.css": ["text/css; charset=utf-8", new URL("../web/hesap.css", import.meta.url)],
  "/varlik/simge.svg": ["image/svg+xml", new URL("../web/simge.svg", import.meta.url)],
};
const UMAMI_ID = "c2b00135-77be-43a8-8879-06a93dbaa8a5";
const LANG_NAMES = { tr: "Türkçe", en: "English", de: "Deutsch", az: "Azərbaycanca", ar: "العربية", id: "Bahasa Indonesia", ur: "اردو", fa: "فارسی", fr: "Français", ru: "Русский", ja: "日本語", ko: "한국어", zh: "简体中文", fil: "Filipino", th: "ไทย", es: "Español", pt: "Português", hi: "हिन्दी", bn: "বাংলা" };

const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
/* [metin](adres) bağlantılarını yeni sekmede açılan bağlantıya çevirir (metnin geri kalanı kaçışlanır) */
export function linkify(s) {
  let out = "", last = 0;
  const re = /\[([^\]]+)\]\((https:\/\/[^)\s]+)\)/g;
  let m;
  while ((m = re.exec(s))) {
    out += esc(s.slice(last, m.index)) + `<a href="${esc(m[2])}" target="_blank" rel="noopener">${esc(m[1])}</a>`;
    last = re.lastIndex;
  }
  return out + esc(s.slice(last));
}

const CSP = [
  "default-src 'self'", "script-src 'self' https://istatistik.bumba.tr", "connect-src 'self' https://istatistik.bumba.tr",
  "img-src 'self' data: blob: https://bumbagroup.com", "style-src 'self'", "font-src 'self'", "form-action 'self'", "frame-ancestors 'none'", "base-uri 'none'", "object-src 'none'",
].join("; ");

function pickLang(req, url, user) {
  const q = url.searchParams.get("dil");
  if (q && LANGS.includes(normLang(q)) && normLang(q) === String(q).toLowerCase()) return q.toLowerCase();
  if (user?.lang) return normLang(user.lang);
  const al = String(req.headers["accept-language"] || "").split(",").map((x) => x.split(";")[0].trim()).filter(Boolean);
  for (const a of al) { const l = normLang(a); if (l !== "en" || /^en/i.test(a)) return l; }
  return "en";
}

export function webRoutes({ cfg, closed, accounts }) {
  function page(res, { lang, title, body, data = {}, stats = true, theme = "system", status = 200 }) {
    const t = UI[lang];
    const dir = RTL.has(lang) ? "rtl" : "ltr";
    const cfgData = { lang, t, siteUrl: cfg.siteUrl + (lang === "tr" ? "/" : `/${lang}/`), ...data, umami: stats ? { id: UMAMI_ID, host: new URL(cfg.publicUrl).host } : null, langs: LANGS.map((l) => [l, LANG_NAMES[l]]) };
    const html = `<!doctype html><html lang="${lang}" dir="${dir}" data-theme="${esc(theme)}"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex, nofollow">
<meta name="referrer" content="no-referrer"><title>${esc(title)} · ${PRODUCT_NAME}</title>
<link rel="icon" href="/varlik/simge.svg"><link rel="stylesheet" href="/varlik/hesap.css">
<script type="application/json" id="hesap-veri">${JSON.stringify(cfgData).replace(/</g, "\\u003c")}</script>
<script src="/varlik/hesap.js" defer></script></head>
<body><header class="top"><a class="brand" href="${esc(cfg.siteUrl)}${lang === "tr" ? "/" : `/${lang}/`}"><img src="/varlik/simge.svg" alt="" width="28" height="28"> ${PRODUCT_NAME}</a>
<label class="lang"><span class="sr">${esc(t.language)}</span><select data-lang>${LANGS.map((l) => `<option value="${l}"${l === lang ? " selected" : ""}>${esc(LANG_NAMES[l])}</option>`).join("")}</select></label></header>
<main id="main" class="wrap">${body}</main>
<footer class="foot"><a href="${esc(legal(lang).notice)}" target="_blank" rel="noopener">${esc(t.notice)}</a> · <a href="${esc(legal(lang).terms)}" target="_blank" rel="noopener">${esc(t.terms)}</a> · <a href="${esc(deletionUrl(lang))}">${esc(t.deletionInfo)}</a>
<a class="badge" href="https://bumbagroup.com/life" rel="noopener" title="${esc(ROZET[lang].title)}"><img class="only-light" src="https://bumbagroup.com/rozet/life.svg" width="164" height="28" alt="${esc(ROZET[lang].alt)}"><img class="only-dark" src="https://bumbagroup.com/rozet/life-koyu.svg" width="164" height="28" alt="${esc(ROZET[lang].alt)}"></a></footer>
<div class="cookie" id="cookie-bar" role="region" aria-label="${esc(t.cbLabel)}" hidden><p>${esc(t.cbText)} <a href="https://bumbagroup.com/cerez-politikasi${lang === "tr" ? "" : "?dil=en"}" target="_blank" rel="noopener">${esc(data.cookie?.policy || "")}</a></p>
<div class="row"><button type="button" data-cookie="accept">${esc(data.cookie?.accept)}</button><button type="button" data-cookie="reject">${esc(data.cookie?.reject)}</button><button type="button" data-cookie="prefs">${esc(data.cookie?.prefs)}</button></div></div>
<dialog id="cookie-dialog"><h2>${esc(data.cookie?.title)}</h2><h3>${esc(data.cookie?.necessary)}</h3><p>${esc(t.cbNecessaryText)}</p>
<label class="check"><input type="checkbox" data-cookie-stats> ${esc(data.cookie?.stats)}</label><p class="muted">${esc(data.cookie?.statsText)}</p>
<div class="row"><button type="button" data-cookie="save">${esc(data.cookie?.save)}</button></div></dialog>
</body></html>`;
    res.writeHead(status, { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "private, no-store", "Content-Security-Policy": CSP, "X-Frame-Options": "DENY" });
    res.end(html);
  }
  const legal = (lang) => ({ notice: `https://bumbagroup.com/uyelik-aydinlatma${lang === "tr" ? "" : "?dil=en"}`, terms: `https://bumbagroup.com/uyelik-sozlesmesi${lang === "tr" ? "" : "?dil=en"}` });
  const deletionUrl = (lang) => `${cfg.siteUrl}${lang === "tr" ? "/hesap-silme/" : `/${lang}/account-deletion/`}`;
  const cookieTexts = (lang) => COOKIE[lang] || COOKIE.en;

  const shell = (inner) => `<section class="card">${inner}</section>`;
  const msg = (text, kind = "") => `<p class="msg ${kind}" role="status">${esc(text)}</p>`;

  return async function handle(req, res, url, ctx) {
    const p = url.pathname;
    if (ASSETS[p] && req.method === "GET") {
      const [type, file] = ASSETS[p];
      res.writeHead(200, { "Content-Type": type, "Cache-Control": "public, max-age=3600" });
      res.end(readFileSync(file));
      return true;
    }
    const pages = ["/", "/giris", "/kayit", "/sifre", "/dogrulandi", "/hesap", "/hesap/rozetler", "/kutlama"];
    if (!pages.includes(p) || !["GET", "POST"].includes(req.method) || (req.method === "POST" && p !== "/kutlama")) return false;

    const s = ctx && !closed && ["/", "/giris", "/kayit", "/hesap", "/hesap/rozetler"].includes(p) ? await ctx.session(req) : null;
    const user = s?.user || null;
    const lang = pickLang(req, url, user);
    const t = UI[lang];
    const theme = user?.theme || "system";
    const base = { lang, theme, data: { cookie: cookieTexts(lang) } };

    if (closed) {
      page(res, { ...base, title: t.title, body: shell(`<h1>${esc(t.title)}</h1>${msg(t.closed)}`), status: 503, stats: false });
      return true;
    }
    const go = (to) => { res.writeHead(303, { Location: `${to}?dil=${lang}`, "Cache-Control": "no-store" }); res.end(); return true; };

    if (p === "/") return go(user ? "/hesap" : "/giris");
    if ((p === "/giris" || p === "/kayit") && user && !url.searchParams.has("adim")) return go("/hesap");
    if ((p === "/hesap" || p === "/hesap/rozetler") && !user) return go("/giris");

    const pw = `<div class="pw-rule" data-pw-rule><p>${esc(t.pwRuleTitle)}</p><ul>${["pwLen", "pwDigit", "pwCase", "pwSymbol"].map((k) => `<li data-rule="${k}">${esc(t[k])}</li>`).join("")}</ul></div>`;

    if (p === "/giris") {
      page(res, { ...base, title: t.signIn, body: shell(`<h1>${esc(t.signIn)}</h1><p class="muted">${esc(t.intro)}</p>
<form data-form="signin" novalidate><label>${esc(t.email)}<input type="email" name="email" autocomplete="username" required></label>
<label>${esc(t.password)}<input type="password" name="password" autocomplete="current-password" required></label>
<div data-msg></div><button type="submit" class="primary">${esc(t.signIn)}</button></form>
<p><button type="button" class="link" data-show="forgot">${esc(t.forgot)}</button></p>
<form data-form="forgot" hidden novalidate><p>${esc(t.forgotLead)}</p><label>${esc(t.email)}<input type="email" name="email" autocomplete="email" required></label><div data-msg></div><button type="submit">${esc(t.forgotSend)}</button></form>
<form data-form="link-separate" hidden novalidate><p>${esc(t.ayriQ)}</p><p class="muted">${esc(t.ayriNote)}</p><label>${esc(t.password)}<input type="password" name="password" autocomplete="off"></label><div data-msg></div>
<div class="row"><button type="submit" class="primary">${esc(t.yes)}</button><button type="button" data-skip>${esc(t.no)}</button></div></form>
<p class="muted">${esc(t.noAccount)} <a href="/kayit?dil=${lang}">${esc(t.signUp)}</a></p>`) });
      return true;
    }
    if (p === "/kayit") {
      page(res, { ...base, title: t.signUp, body: shell(`<h1>${esc(t.signUp)}</h1><p class="muted">${esc(t.intro)}</p>
<form data-form="signup" novalidate><label>${esc(t.email)}<input type="email" name="email" autocomplete="email" required></label>
<div data-bumba hidden><p>${esc(t.bumbaQ)}</p><p class="muted">${esc(t.bumbaNote)}</p><div class="row"><button type="button" class="primary" data-bumba-yes>${esc(t.yes)}</button><button type="button" data-bumba-no>${esc(t.no)}</button></div></div>
<div data-signup-rest><label>${esc(t.password)}<input type="password" name="password" autocomplete="new-password" required aria-describedby="pwr"></label><div id="pwr">${pw}</div>
<div data-msg></div><button type="submit" class="primary">${esc(t.signUp)}</button><p class="consent">${linkify(t.consent)}</p></div></form>
<form data-form="link" hidden novalidate><h2>${esc(t.bumbaSignIn)}</h2><p class="muted">${esc(t.bumbaNote)}</p><label>${esc(t.email)}<input type="email" name="email" autocomplete="username" readonly></label>
<label>${esc(t.password)}<input type="password" name="password" autocomplete="current-password" required></label><div data-msg></div><button type="submit" class="primary">${esc(t.signIn)}</button>
<p><a href="/giris?dil=${lang}">${esc(t.forgot)}</a></p></form>
<div data-done hidden>${msg(t.signedUp, "ok")}<button type="button" data-resend>${esc(t.resend)}</button></div>
<p class="muted">${esc(t.haveAccount)} <a href="/giris?dil=${lang}">${esc(t.signIn)}</a></p>`) });
      return true;
    }
    if (p === "/sifre") {
      const token = url.searchParams.get("token") || "";
      page(res, { ...base, stats: false, title: t.resetTitle, data: { ...base.data, token }, body: shell(`<h1>${esc(t.resetTitle)}</h1>
${token ? `<form data-form="reset" novalidate><label>${esc(t.newPassword)}<input type="password" name="password" autocomplete="new-password" required></label>${pw}
<label>${esc(t.newPassword2)}<input type="password" name="password2" autocomplete="new-password" required></label><div data-msg></div><button type="submit" class="primary">${esc(t.resetSave)}</button></form>` : msg(t.resetBad, "err")}
<div data-done hidden>${msg(t.resetDone, "ok")}<p><a class="button primary" href="/giris?dil=${lang}">${esc(t.signIn)}</a></p></div>`) });
      return true;
    }
    if (p === "/dogrulandi") {
      const k = url.searchParams.get("k");
      const step = url.searchParams.get("adim");
      const err = url.searchParams.get("error");
      let text = t.verifiedOk, kind = "ok";
      if (err) { text = t.verifiedErr; kind = "err"; }
      else if (step === "eposta-onay") text = t.verifiedStep;
      else if (k) {
        const r = (await ctx.pool.query(`select result from verify_ref where k = $1`, [k])).rows[0];
        if (r?.result === "bumba") { text = t.verifiedBumba; kind = ""; }
        else if (r?.result === "eposta") text = t.verifiedChanged;
        else if (r?.result === "kapali") { text = t.verifiedErr; kind = "err"; }
      }
      page(res, { ...base, stats: false, title: t.title, body: shell(`<h1>${esc(t.title)}</h1>${msg(text, kind)}<p><a class="button primary" href="/giris?dil=${lang}">${esc(t.signIn)}</a></p>`) });
      return true;
    }
    if (p === "/kutlama") {
      const userId = readUnsubscribeToken(cfg.secret, url.searchParams.get("k"));
      const k = esc(url.searchParams.get("k") || "");
      const body = !userId ? msg(t.unsubBad, "err") : req.method === "POST" ? msg(t.unsubDone, "ok")
        : `<p>${esc(t.unsubAsk)}</p><form method="post" action="/kutlama?dil=${lang}&amp;k=${k}"><button type="submit" class="primary">${esc(t.unsubButton)}</button></form>`;
      page(res, { ...base, stats: false, title: t.unsubTitle, body: shell(`<h1>${esc(t.unsubTitle)}</h1>${body}`) });
      return true;
    }
    if (p === "/hesap/rozetler") {
      page(res, { ...base, title: t.badgesTitle, data: { ...base.data, badgeNames: BADGE_NAMES[lang] || BADGE_NAMES.en }, body: `<p><a href="/hesap?dil=${lang}">‹ ${esc(t.title)}</a></p>
${shell(`<h1>${esc(t.badgesTitle)}</h1><p class="muted">${esc(t.badgesNote)}</p><div data-badges></div>`)}
${shell(`<h2>${esc(t.recordsTitle)}</h2><p class="muted">${esc(t.recordsNote)}</p><div data-records></div>`)}` });
      return true;
    }
    if (p === "/hesap") {
      const range = birthDateRange();
      const sec = (id, n, title, inner) => `<section class="card" id="${id}" aria-labelledby="${id}-h"><h2 id="${id}-h"><span class="num">${n}</span> ${esc(title)}</h2>${inner}</section>`;
      const opt = (v, l, sel) => `<option value="${esc(v)}"${sel ? " selected" : ""}>${esc(l)}</option>`;
      const body = `<h1>${esc(t.title)}</h1><p class="row"><a class="button" href="/hesap/rozetler?dil=${lang}">${esc(t.badgesTitle)}</a><button type="button" data-signout>${esc(t.signOut)}</button></p>
${sec("profil", 1, t.profile, `<form data-form="profile" novalidate>
<div class="photo"><img data-photo alt="" width="96" height="96" hidden><div><label class="button">${esc(t.photoChoose)}<input type="file" accept="image/*" data-photo-input class="sr"></label> <button type="button" data-photo-remove hidden>${esc(t.photoRemove)}</button><p class="muted">${esc(t.photoNote)}</p></div></div>
<div class="grid"><label>${esc(t.firstName)}<input name="ad" maxlength="100" autocomplete="given-name"></label><label>${esc(t.lastName)}<input name="soyad" maxlength="100" autocomplete="family-name"></label>
<label>${esc(t.nickname)}<input name="takmaAd" maxlength="60" autocomplete="nickname"></label>
<label>${esc(t.birthDate)}<input type="date" name="dogumTarihi" min="${range.earliest}" max="${range.latest}"></label>
<label>${esc(t.gender)}<select name="cinsiyet">${opt("", t.none)}${opt("kadin", t.genderF)}${opt("erkek", t.genderM)}${opt("belirtmek-istemiyorum", t.genderN)}</select></label>
<label>${esc(t.country)}<select name="ulke" data-countries></select></label>
<fieldset class="phone"><legend>${esc(t.phone)}</legend><label><span class="sr">${esc(t.phoneCode)}</span><select name="ulkeKodu" data-dial></select></label><label><span class="sr">${esc(t.phoneNumber)}</span><input name="numara" inputmode="tel" autocomplete="tel-national" placeholder="${esc(t.phoneNumber)}"></label></fieldset>
<label>${esc(t.city)}<input name="sehir" maxlength="100" autocomplete="address-level2"></label><label>${esc(t.job)}<input name="meslek" maxlength="100" autocomplete="organization-title"></label></div>
<label>${esc(t.about)}<textarea name="hakkimda" maxlength="160" rows="3" aria-describedby="about-h"></textarea></label><p class="muted" id="about-h">${esc(t.aboutHint)}</p>
<fieldset><legend>${esc(t.social)}</legend><div data-social></div><button type="button" data-social-add>${esc(t.socialAdd)}</button></fieldset>
<p class="muted">${linkify(t.optionalNote)}</p><div data-msg></div><button type="submit" class="primary">${esc(t.save)}</button></form>`)}
${sec("gorunurluk", 2, t.visibility, `<form data-form="visibility" novalidate><label class="check"><input type="checkbox" name="acik"> ${esc(t.showProfile)}</label>
<fieldset><legend>${esc(t.visibleFields)}</legend><div class="checks">${VISIBLE_FIELDS.map((f) => `<label class="check"><input type="checkbox" name="alan" value="${f}"> ${esc(t[FIELD_LABEL[f]])}</label>`).join("")}</div></fieldset>
<p class="muted">${esc(t.emailNever)}</p><p data-display-name class="muted"></p><div data-msg></div><button type="submit" class="primary">${esc(t.save)}</button></form>`)}
${sec("eposta", 3, t.emailSection, `<p data-email class="strong"></p><form data-form="email" novalidate><label>${esc(t.newEmail)}<input type="email" name="email" autocomplete="email" required></label><div data-msg></div><button type="submit">${esc(t.changeEmail)}</button></form>`)}
${sec("sifre", 4, t.passwordSection, `<form data-form="password" novalidate><label>${esc(t.passwordCurrent)}<input type="password" name="current" autocomplete="current-password" required></label>
<label>${esc(t.newPassword)}<input type="password" name="password" autocomplete="new-password" required></label>${pw}<div data-msg></div><button type="submit">${esc(t.changePassword)}</button></form>`)}
${sec("tercihler", 5, t.prefs, `<form data-form="prefs" novalidate><div class="grid"><label>${esc(t.language)}<select name="lang">${LANGS.map((l) => opt(l, LANG_NAMES[l], l === lang)).join("")}</select></label>
<label>${esc(t.theme)}<select name="theme">${opt("system", t.themeSystem)}${opt("light", t.themeLight)}${opt("dark", t.themeDark)}</select></label>
<label>${esc(t.timeZone)}<select name="timeZone" data-zones></select></label></div><div data-msg></div><button type="submit">${esc(t.save)}</button></form>`)}
${sec("epostalar", 6, t.emails, `<label class="check"><input type="checkbox" data-celebrate> ${esc(t.celebrate)}</label><p class="muted">${esc(t.celebrateNote)}</p>
<h3>${esc(t.list)}</h3><p class="muted">${esc(t.listNote)}</p><p><a href="${esc(cfg.siteUrl)}${lang === "tr" ? "/" : `/${lang}/`}#liste-form">${esc(t.listJoin)}</a></p>`)}
${sec("oturumlar", 7, t.sessions, `<ul class="list" data-sessions></ul><button type="button" data-close-others>${esc(t.closeOthers)}</button>`)}
${sec("uyelik", 8, t.membership, `<dl class="facts"><dt>${esc(t.memberNo)}</dt><dd data-no></dd><dt>${esc(t.memberSince)}</dt><dd data-since></dd></dl>
<h3>${esc(t.products)}</h3><p class="muted">${esc(t.productsNote)}</p><ul class="list" data-products></ul><div class="row"><button type="button" data-open-all>${esc(t.openAll)}</button><button type="button" data-close-all>${esc(t.closeAll)}</button></div><div data-msg-products></div>`)}
${sec("verilerim", 9, t.download, `<p class="muted">${esc(t.downloadNote)}</p><a class="button" href="/v1/verilerim" download>${esc(t.download)}</a>`)}
${sec("sil", 10, t.delete, `<form data-form="delete" novalidate><p>${esc(t.deleteLead)}</p><fieldset><label class="check"><input type="radio" name="kapsam" value="urun" checked> ${esc(t.deleteScopeProduct)}</label><label class="check"><input type="radio" name="kapsam" value="hepsi"> ${esc(t.deleteScopeAll)}</label></fieldset>
<label>${esc(t.deletePassword)}<input type="password" name="password" autocomplete="current-password"></label><div data-msg></div><button type="submit" class="danger">${esc(t.delete)}</button></form>`)}
<p class="muted"><a href="${esc(legal(lang).notice)}" target="_blank" rel="noopener">${esc(t.notice)}</a> · <a href="${esc(legal(lang).terms)}" target="_blank" rel="noopener">${esc(t.terms)}</a></p>`;
      page(res, { ...base, title: t.title, data: { ...base.data, countries: COUNTRIES, social: SOCIAL }, body });
      return true;
    }
    return false;
  };
}

const FIELD_LABEL = { ad: "firstName", soyad: "lastName", takmaAd: "nickname", foto: "photo", dogumTarihi: "birthDate", cinsiyet: "gender", telefon: "phone", ulke: "country", sehir: "city", meslek: "job", hakkimda: "about", sosyal: "social" };
const DATA = JSON.parse(readFileSync(new URL("../data/web-texts.json", import.meta.url), "utf8"));
const COOKIE = DATA.cookie;
const BADGE_NAMES = DATA.badges;
const ROZET = DATA.rozet;
