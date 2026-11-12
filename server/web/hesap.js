/* Hesap sayfalarının betiği (hesap.time.bumba.tr). Sayfa iskeletini sunucu verir; bu dosya formları aynı kaynaktaki
   uçlara (Better Auth /api/auth/*, kendi /v1/*) JSON olarak gönderir. Satır içi betik yok (CSP). */
(() => {
  "use strict";
  const D = JSON.parse(document.getElementById("hesap-veri").textContent);
  const t = D.t, lang = D.lang;
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const fill = (s, v) => s.replace(/\{(\w+)\}/g, (_, k) => (v[k] != null ? v[k] : ""));
  const numLocale = { ar: "ar-u-nu-latn", fa: "fa-u-nu-latn", ur: "ur-u-nu-latn", hi: "hi-u-nu-latn", bn: "bn-u-nu-latn", th: "th-u-ca-gregory-nu-latn" }[lang] || lang;
  const fmtDate = (d) => { try { return new Intl.DateTimeFormat(numLocale, { dateStyle: "long" }).format(new Date(d)); } catch { return String(d).slice(0, 10); } };
  const fmtDateTime = (d) => { try { return new Intl.DateTimeFormat(numLocale, { dateStyle: "medium", timeStyle: "short" }).format(new Date(d)); } catch { return String(d); } };

  /* ---- Tema: hesapta seçilen tema sunucudan gelir; girişsiz sayfalarda sistem ---- */

  /* ---- Dil seçici: aynı sayfa, başka dil ---- */
  const langSel = $("[data-lang]");
  if (langSel) langSel.addEventListener("change", () => {
    const u = new URL(location.href);
    u.searchParams.set("dil", langSel.value);
    location.href = u.toString();
  });

  /* ---- Çerez bandı (standart madde 14): zorunlu + istatistik; Umami yalnız izinle ---- */
  const KEY = "bumba-cerez", VERSION = "2026-10-21";
  // Seçim sitedekiyle ortak (standart 5.17 madde 14): önce yerel depo, yoksa time.bumba.tr alan adındaki `bumba-cerez`
  // çerezi; yazarken ikisi birlikte. Sitenin "dış hizmetler" seçimi (dis) korunur.
  const cookieRead = () => { const m = document.cookie.match(/(?:^|;\s*)bumba-cerez=([^;]*)/); return m ? decodeURIComponent(m[1]) : null; };
  const raw = () => { let v = null; try { v = localStorage.getItem(KEY); } catch {} return v || cookieRead(); };
  const readChoice = () => { try { const c = JSON.parse(raw() || "null"); return c && c.surum === VERSION && typeof c.istatistik === "boolean" ? c : null; } catch { return null; } };
  const saveChoice = (stats) => {
    const prev = readChoice();
    const v = JSON.stringify({ surum: VERSION, istatistik: !!stats, dis: !!(prev && prev.dis) });
    try { localStorage.setItem(KEY, v); } catch {}
    const dom = /(^|\.)time\.bumba\.tr$/.test(location.hostname) ? "; Domain=time.bumba.tr" : "";
    try { document.cookie = `${KEY}=${encodeURIComponent(v)}${dom}; Path=/; Max-Age=31536000; SameSite=Lax${location.protocol === "https:" ? "; Secure" : ""}`; } catch {}
  };
  const bar = $("#cookie-bar"), dlg = $("#cookie-dialog");
  function loadUmami() {
    if (!D.umami || document.querySelector("script[data-website-id]")) return;
    const s = document.createElement("script");
    s.defer = true; s.src = "https://istatistik.bumba.tr/script.js";
    s.dataset.websiteId = D.umami.id; s.dataset.domains = D.umami.host;
    document.head.appendChild(s);
  }
  function applyChoice(c) { if (c && c.istatistik) loadUmami(); }
  const choice = readChoice();
  if (choice) applyChoice(choice); else if (bar) bar.hidden = false;
  $$("[data-cookie]").forEach((b) => b.addEventListener("click", () => {
    const a = b.dataset.cookie;
    if (a === "prefs") { const c = readChoice(); $("[data-cookie-stats]").checked = !!(c && c.istatistik); dlg.showModal(); return; }
    const stats = a === "accept" ? true : a === "reject" ? false : $("[data-cookie-stats]").checked;
    saveChoice(stats); applyChoice({ istatistik: stats });
    if (bar) bar.hidden = true;
    if (dlg.open) dlg.close();
  }));
  const track = (name) => { try { if (window.umami) window.umami.track(name); } catch {} };

  /* ---- İstekler ---- */
  async function call(path, body, method = "POST") {
    let r;
    try {
      r = await fetch(path, { method, credentials: "same-origin", headers: body ? { "Content-Type": "application/json" } : {}, body: body ? JSON.stringify(body) : undefined });
    } catch { return { status: 0, data: null }; }
    let data = null;
    try { data = await r.json(); } catch {}
    return { status: r.status, data };
  }
  const code = (r) => (r.data && (r.data.code || r.data.hata)) || "";
  function errText(r) {
    const c = code(r);
    if (r.status === 0) return t["err.network"];
    if (r.status === 429) return t["err.tooMany"];
    if (c === "INVALID_EMAIL_OR_PASSWORD" || c === "bilgiler") return t["err.credentials"];
    if (c === "EMAIL_NOT_VERIFIED") return t["err.unverified"];
    if (c === "BANNED_USER") return t["err.suspended"];
    if (c === "WEAK_PASSWORD" || c === "PASSWORD_TOO_SHORT" || c === "PASSWORD_TOO_LONG") return t["err.password"];
    if (c === "EMAIL_TAKEN" || c === "USER_ALREADY_EXISTS") return t["err.emailTaken"];
    if (c === "INVALID_PASSWORD" || c === "sifre") return t["err.wrongPassword"];
    if (c === "INVALID_EMAIL" || c === "eposta") return t["err.email"];
    if (typeof c === "string" && c.startsWith("gecersiz:")) return fill(t["err.field"], { alan: c.slice(9) });
    return t["err.generic"];
  }
  function say(form, text, kind = "") {
    const box = $("[data-msg]", form) || form;
    box.innerHTML = "";
    if (!text) return;
    const p = document.createElement("p");
    p.className = "msg " + kind; p.setAttribute("role", kind === "err" ? "alert" : "status"); p.textContent = text;
    box.appendChild(p);
  }
  const emailOk = (e) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e);
  function busy(form, on) { $$("button", form).forEach((b) => { b.disabled = on; }); }

  /* ---- Şifre kuralı: alanın altında dört satır, sağlandıkça işaretlenir (sunucu da denetler) ---- */
  const rules = (pw) => ({
    pwLen: pw.length >= 8 && pw.length <= 128 && !/\s/.test(pw),
    pwDigit: /\d/.test(pw),
    pwCase: /\p{Lu}/u.test(pw) && /\p{Ll}/u.test(pw),
    pwSymbol: /[^\p{L}\p{N}\s]/u.test(pw),
  });
  const pwOk = (pw) => Object.values(rules(pw)).every(Boolean);
  $$("[data-pw-rule]").forEach((box) => {
    const form = box.closest("form");
    const input = $('input[name="password"]', form);
    if (!input) return;
    const upd = () => { const r = rules(input.value); $$("li", box).forEach((li) => li.classList.toggle("ok", !!r[li.dataset.rule])); };
    input.addEventListener("input", upd); upd();
  });

  /* ---- Giriş ---- */
  const signin = $('[data-form="signin"]');
  async function afterSignIn() {
    const d = await call("/v1/uyelik/durum", null, "GET");
    if (d.status === 200 && d.data && d.data.ayriHesap) {
      signin.hidden = true;
      const f = $('[data-form="link-separate"]'); f.hidden = false; $('input', f).focus();
      return;
    }
    location.href = "/hesap?dil=" + lang;
  }
  if (signin) {
    signin.addEventListener("submit", async (e) => {
      e.preventDefault();
      const email = signin.email.value.trim(), password = signin.password.value;
      if (!emailOk(email)) return say(signin, t["err.email"], "err");
      busy(signin, true);
      const r = await call("/api/auth/sign-in/email", { email, password, rememberMe: true });
      busy(signin, false);
      if (r.status === 200) { track("giris"); return afterSignIn(); }
      say(signin, errText(r), "err");
    });
    $("[data-show=forgot]").addEventListener("click", () => {
      const f = $('[data-form="forgot"]'); f.hidden = false; f.email.value = signin.email.value; f.email.focus();
    });
    const forgot = $('[data-form="forgot"]');
    forgot.addEventListener("submit", async (e) => {
      e.preventDefault();
      const email = forgot.email.value.trim();
      if (!emailOk(email)) return say(forgot, t["err.email"], "err");
      busy(forgot, true);
      const r = await call("/api/auth/request-password-reset", { email });
      busy(forgot, false);
      if (r.status === 200) say(forgot, t.forgotSent, "ok"); else say(forgot, errText(r), "err");
    });
    const sep = $('[data-form="link-separate"]');
    sep.addEventListener("submit", async (e) => {
      e.preventDefault();
      busy(sep, true);
      const r = await call("/v1/uyelik/birlestir", { sifre: sep.password.value });
      busy(sep, false);
      if (r.status === 200) { say(sep, t.ayriDone, "ok"); setTimeout(() => { location.href = "/hesap?dil=" + lang; }, 1200); }
      else say(sep, errText(r), "err");
    });
    $("[data-skip]", sep).addEventListener("click", () => { location.href = "/hesap?dil=" + lang; });
  }

  /* ---- Kayıt: önce e-posta; başka bir Bumba ürününde üyeyse soru, değilse şifre + onay + Üye ol ---- */
  const signup = $('[data-form="signup"]');
  if (signup) {
    let checked = "", status = "yok";
    const rest = $("[data-signup-rest]", signup), ask = $("[data-bumba]", signup);
    const link = $('[data-form="link"]'), done = $("[data-done]");
    async function check(email) {
      if (email === checked) return status;
      const r = await call("/v1/uyelik/sorgula", { eposta: email });
      if (r.status === 200) { checked = email; status = r.data.durum; }
      else if (r.status === 429) { say(signup, t["err.tooMany"], "err"); return null; }
      else { status = "yok"; }
      return status;
    }
    function showAsk(on) { ask.hidden = !on; rest.hidden = on; }
    signup.email.addEventListener("change", async () => {
      const email = signup.email.value.trim().toLowerCase();
      if (emailOk(email)) showAsk((await check(email)) === "var");
    });
    signup.addEventListener("submit", async (e) => {
      e.preventDefault();
      const email = signup.email.value.trim().toLowerCase(), password = signup.password.value;
      if (!emailOk(email)) return say(signup, t["err.email"], "err");
      busy(signup, true);
      const s = await check(email);
      if (s === "var") { busy(signup, false); return showAsk(true); }
      if (s === null) { busy(signup, false); return; }
      if (!pwOk(password)) { busy(signup, false); return say(signup, t["err.password"], "err"); }
      const tz = (() => { try { return Intl.DateTimeFormat().resolvedOptions().timeZone; } catch { return undefined; } })();
      const r = await call("/api/auth/sign-up/email", { email, password, name: "", lang, timeZone: tz });
      busy(signup, false);
      if (r.status === 200) { track("kayit"); signup.hidden = true; done.hidden = false; done.dataset.email = email; return; }
      if (code(r) === "BUMBA_MEMBER") { checked = email; status = "var"; return showAsk(true); }
      say(signup, errText(r), "err");
    });
    $("[data-bumba-yes]", signup).addEventListener("click", () => {
      signup.hidden = true; link.hidden = false;
      link.email.value = signup.email.value.trim().toLowerCase(); link.password.focus();
    });
    $("[data-bumba-no]", signup).addEventListener("click", () => {
      showAsk(false); signup.email.value = ""; checked = "";
      say(signup, t.bumbaNo, ""); signup.email.focus();
    });
    link.addEventListener("submit", async (e) => {
      e.preventDefault();
      busy(link, true);
      const r = await call("/v1/uyelik/bagla", { eposta: link.email.value, sifre: link.password.value, dil: lang });
      busy(link, false);
      if (r.status === 200) { track("bagla"); location.href = "/hesap?dil=" + lang; return; }
      say(link, errText(r), "err");
    });
    $("[data-resend]").addEventListener("click", async (e) => {
      e.target.disabled = true;
      const r = await call("/api/auth/send-verification-email", { email: done.dataset.email });
      const p = document.createElement("p"); p.className = "msg " + (r.status === 200 ? "ok" : "err"); p.setAttribute("role", "status");
      p.textContent = r.status === 200 ? t.resent : errText(r); done.appendChild(p);
    });
  }

  /* ---- Şifre sıfırlama ---- */
  const reset = $('[data-form="reset"]');
  if (reset) reset.addEventListener("submit", async (e) => {
    e.preventDefault();
    const pw = reset.password.value;
    if (!pwOk(pw)) return say(reset, t["err.password"], "err");
    if (pw !== reset.password2.value) return say(reset, t["err.match"], "err");
    busy(reset, true);
    const r = await call("/api/auth/reset-password", { newPassword: pw, token: D.token });
    busy(reset, false);
    if (r.status === 200) { reset.hidden = true; $("[data-done]").hidden = false; return; }
    say(reset, r.status === 400 && code(r) !== "WEAK_PASSWORD" ? t.resetBad : errText(r), "err");
  });

  /* ---- Hesap sayfası ---- */
  if ($('[data-form="profile"]')) account();
  if ($("[data-badges]")) badgesPage();

  async function account() {
    const r = await call("/v1/hesap", null, "GET");
    if (r.status === 401) { location.href = "/giris?dil=" + lang; return; }
    if (r.status !== 200) return;
    const A = r.data, P = A.profil || {};
    $("[data-signout]").addEventListener("click", async () => { await call("/api/auth/sign-out", {}); location.href = "/giris?dil=" + lang; });

    // 1. Profil
    const pf = $('[data-form="profile"]');
    for (const k of ["ad", "soyad", "takmaAd", "dogumTarihi", "cinsiyet", "sehir", "meslek", "hakkimda"]) if (pf[k]) pf[k].value = P[k] || "";
    let names = null;
    try { names = new Intl.DisplayNames([lang], { type: "region" }); } catch {}
    const flag = (cc) => cc.replace(/./g, (c) => String.fromCodePoint(127397 + c.charCodeAt(0)));
    const countries = Object.entries(D.countries).map(([cc, dial]) => ({ cc, dial, name: (names && names.of(cc)) || cc }))
      .sort((a, b) => a.name.localeCompare(b.name, lang));
    const cSel = $("[data-countries]"), dSel = $("[data-dial]");
    cSel.add(new Option(t.none, ""));
    dSel.add(new Option(t.phoneCode, ""));
    for (const c of countries) {
      cSel.add(new Option(`${flag(c.cc)} ${c.name}`, c.cc));
      if (c.dial) dSel.add(new Option(`${flag(c.cc)} ${c.name} (+${c.dial})`, `+${c.dial}|${c.cc}`));
    }
    cSel.value = P.ulke || "";
    if (P.telefon) {
      const opt = Array.from(dSel.options).find((o) => o.value === `${P.telefon.ulkeKodu}|${P.ulke}`) || Array.from(dSel.options).find((o) => o.value.startsWith(P.telefon.ulkeKodu + "|"));
      if (opt) dSel.value = opt.value;
      pf.numara.value = P.telefon.numara || "";
    }
    cSel.addEventListener("change", () => {
      if (!dSel.value && cSel.value) { const o = Array.from(dSel.options).find((x) => x.value.endsWith("|" + cSel.value)); if (o) dSel.value = o.value; }
    });
    // Sosyal hesaplar
    const sBox = $("[data-social]");
    function socialRow(v = { platform: D.social[0], deger: "" }) {
      const row = document.createElement("div"); row.className = "social-row";
      const sel = document.createElement("select"); sel.setAttribute("aria-label", t.social);
      for (const s of D.social) sel.add(new Option(s === "web" ? t.socialWeb : s[0].toUpperCase() + s.slice(1), s));
      sel.value = v.platform;
      const inp = document.createElement("input"); inp.maxLength = 200; inp.value = v.deger || ""; inp.placeholder = t.socialValue; inp.setAttribute("aria-label", t.socialValue);
      const rm = document.createElement("button"); rm.type = "button"; rm.textContent = t.socialRemove; rm.addEventListener("click", () => row.remove());
      row.append(sel, inp, rm); sBox.appendChild(row);
    }
    (P.sosyal || []).forEach(socialRow);
    $("[data-social-add]").addEventListener("click", () => { if ($$(".social-row", sBox).length < 8) socialRow(); });
    // Fotoğraf: cihazda 512 piksel WebP'ye küçültülür (EXIF ve konum çizimle düşer); sunucu boyutu ve türü yeniden denetler
    const img = $("[data-photo]"), rmPhoto = $("[data-photo-remove]");
    function showPhoto(src) { img.hidden = !src; rmPhoto.hidden = !src; if (src) img.src = src; else img.removeAttribute("src"); }
    showPhoto(P.foto);
    $("[data-photo-input]").addEventListener("change", async (e) => {
      const file = e.target.files && e.target.files[0];
      if (!file) return;
      try {
        const bmp = await createImageBitmap(file);
        const side = Math.min(bmp.width, bmp.height), size = Math.min(512, side);
        const cv = document.createElement("canvas"); cv.width = cv.height = size;
        cv.getContext("2d").drawImage(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side, 0, 0, size, size);
        let q = 0.85, data = cv.toDataURL("image/webp", q);
        while (data.length * 0.75 > 190 * 1024 && q > 0.4) { q -= 0.1; data = cv.toDataURL("image/webp", q); }
        if (!data.startsWith("data:image/webp")) throw new Error("webp");
        const s = await call("/v1/profil/foto", { foto: data });
        if (s.status === 200) { showPhoto(s.data.foto); say(pf, t.saved, "ok"); } else say(pf, errText(s), "err");
      } catch { say(pf, t["err.generic"], "err"); }
      e.target.value = "";
    });
    rmPhoto.addEventListener("click", async () => {
      const s = await call("/v1/profil/foto", { foto: null });
      if (s.status === 200) showPhoto(null); else say(pf, errText(s), "err");
    });
    function collect() {
      const v = (k) => pf[k].value.trim() || null;
      const [kod, ] = (dSel.value || "").split("|");
      const num = pf.numara.value.replace(/\D/g, "");
      return {
        ad: v("ad"), soyad: v("soyad"), takmaAd: v("takmaAd"), dogumTarihi: v("dogumTarihi"), cinsiyet: v("cinsiyet"),
        ulke: cSel.value || null, sehir: v("sehir"), meslek: v("meslek"), hakkimda: v("hakkimda"),
        telefon: kod && num ? { ulkeKodu: kod, numara: num } : null,
        sosyal: $$(".social-row", sBox).map((r) => ({ platform: $("select", r).value, deger: $("input", r).value.trim() })).filter((x) => x.deger),
        gorunurluk: visibility(),
      };
    }
    async function saveProfile(form) {
      busy(form, true);
      const s = await call("/v1/profil", { profil: collect() });
      busy(form, false);
      if (s.status === 200) { Object.assign(P, s.data.profil); say(form, t.saved, "ok"); showName(); } else say(form, errText(s), "err");
    }
    pf.addEventListener("submit", (e) => { e.preventDefault(); saveProfile(pf); });

    // 2. Görünürlük
    const vf = $('[data-form="visibility"]');
    const g = P.gorunurluk || { acik: false, alanlar: [] };
    vf.acik.checked = !!g.acik;
    $$('input[name="alan"]', vf).forEach((c) => { c.checked = (g.alanlar || []).includes(c.value); });
    function visibility() { return { acik: vf.acik.checked, alanlar: $$('input[name="alan"]:checked', vf).map((c) => c.value) }; }
    function showName() {
      // Sunucudaki displayName kuralı (profile.mjs): profil açıksa takma ad, yoksa ad (+ soyad), yoksa "Üye N"
      const g = P.gorunurluk || {}, vis = (k) => g.acik && (g.alanlar || []).includes(k) && P[k];
      const name = vis("takmaAd") ? P.takmaAd : vis("ad") ? (vis("soyad") ? `${P.ad} ${P.soyad}` : P.ad) : fill(t.member, { no: A.uyeNo != null ? A.uyeNo : "" });
      $("[data-display-name]").textContent = fill(t.displayName, { ad: name });
    }
    showName();
    vf.addEventListener("submit", (e) => { e.preventDefault(); saveProfile(vf); });

    // 3. E-posta
    $("[data-email]").textContent = A.eposta;
    const ef = $('[data-form="email"]');
    ef.addEventListener("submit", async (e) => {
      e.preventDefault();
      const ne = ef.email.value.trim().toLowerCase();
      if (!emailOk(ne)) return say(ef, t["err.email"], "err");
      busy(ef, true);
      const s = await call("/api/auth/change-email", { newEmail: ne });
      busy(ef, false);
      if (s.status === 200) { say(ef, t.changeEmailSent, "ok"); ef.email.value = ""; } else say(ef, errText(s), "err");
    });

    // 4. Şifre
    const wf = $('[data-form="password"]');
    wf.addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!pwOk(wf.password.value)) return say(wf, t["err.password"], "err");
      busy(wf, true);
      const s = await call("/api/auth/change-password", { currentPassword: wf.current.value, newPassword: wf.password.value, revokeOtherSessions: true });
      busy(wf, false);
      if (s.status === 200) { wf.reset(); say(wf, t.passwordChanged, "ok"); listSessions(); } else say(wf, errText(s), "err");
    });

    // 5. Tercihler
    const prf = $('[data-form="prefs"]');
    prf.theme.value = A.tema || "system";
    let zones = [];
    try { zones = Intl.supportedValuesOf("timeZone"); } catch {}
    const zSel = $("[data-zones]");
    const here = (() => { try { return Intl.DateTimeFormat().resolvedOptions().timeZone; } catch { return ""; } })();
    if (A.saatDilimi && !zones.includes(A.saatDilimi)) zones.unshift(A.saatDilimi);
    zSel.add(new Option(t.none, ""));
    for (const z of zones) zSel.add(new Option(z.replace(/_/g, " "), z));
    zSel.value = A.saatDilimi || here || "";
    prf.addEventListener("submit", async (e) => {
      e.preventDefault();
      busy(prf, true);
      const body = { lang: prf.lang.value, theme: prf.theme.value };
      if (zSel.value) body.timeZone = zSel.value;
      const s = await call("/api/auth/update-user", body);
      busy(prf, false);
      if (s.status !== 200) return say(prf, errText(s), "err");
      if (body.lang !== lang) { location.href = "/hesap?dil=" + body.lang; return; }
      document.documentElement.dataset.theme = body.theme;
      say(prf, t.saved, "ok");
    });

    // 6. E-postalar: kutlama
    const cb = $("[data-celebrate]");
    cb.checked = !!A.kutlama;
    cb.addEventListener("change", async () => {
      const s = await call("/v1/kutlama", { acik: cb.checked });
      if (s.status !== 200) cb.checked = !cb.checked;
    });

    // 7. Oturumlar
    async function listSessions() {
      const ul = $("[data-sessions]");
      const s = await call("/api/auth/list-sessions", null, "GET");
      const cur = await call("/api/auth/get-session", null, "GET");
      const curToken = cur.data && cur.data.session && cur.data.session.token;
      ul.innerHTML = "";
      if (s.status !== 200 || !Array.isArray(s.data)) return;
      for (const x of s.data) {
        const li = document.createElement("li");
        const dev = x.client === "ios" ? t.deviceIos : x.client === "android" ? t.deviceAndroid : x.client ? t.deviceApp : t.deviceWeb;
        const info = document.createElement("div");
        const b = document.createElement("strong"); b.textContent = dev + (x.token === curToken ? " · " + t.thisSession : "");
        const small = document.createElement("div"); small.className = "muted";
        small.textContent = fill(t.opened, { gun: fmtDateTime(x.createdAt) }) + " · " + fill(t.lastUsed, { gun: fmtDateTime(x.updatedAt) });
        info.append(b, small); li.appendChild(info);
        if (x.token !== curToken) {
          const btn = document.createElement("button"); btn.type = "button"; btn.textContent = t.close;
          btn.addEventListener("click", async () => { await call("/api/auth/revoke-session", { token: x.token }); listSessions(); });
          li.appendChild(btn);
        }
        ul.appendChild(li);
      }
    }
    listSessions();
    $("[data-close-others]").addEventListener("click", async () => { await call("/api/auth/revoke-other-sessions", {}); listSessions(); });

    // 8. Üyelik ve öbür Bumba ürünleri
    $("[data-no]").textContent = A.uyeNo != null ? String(A.uyeNo) : "—";
    $("[data-since]").textContent = fmtDate(A.uyelikTarihi);
    const prodBox = $("[data-products]"), pmsg = $("[data-msg-products]");
    let products = [];
    function pSay(text, kind) { pmsg.innerHTML = ""; if (text) { const p = document.createElement("p"); p.className = "msg " + kind; p.textContent = text; pmsg.appendChild(p); } }
    async function askPassword() {
      const pw = window.prompt(t.confirmOpen);
      return pw == null ? null : pw;
    }
    async function setProducts(keys, on) {
      if (!keys.length) return;
      if (on) {
        const pw = await askPassword(); if (pw == null) return;
        const s = await call("/v1/uyelik/ac", { hedefler: keys, sifre: pw });
        if (s.status !== 200) return pSay(errText(s), "err");
      } else {
        const name = keys.length === 1 ? (products.find((p) => p.site === keys[0]) || {}).ad || keys[0] : keys.map((k) => (products.find((p) => p.site === k) || {}).ad || k).join(", ");
        if (!window.confirm(fill(t.confirmClose, { urun: name }))) return;
        const s = await call("/v1/uyelik/kapat", { hedefler: keys });
        if (s.status !== 200) return pSay(errText(s), "err");
      }
      pSay(t.saved, "ok"); loadProducts();
    }
    async function loadProducts() {
      prodBox.innerHTML = "";
      if (A.uyeNo == null) return pSay(t.productsUnavailable, "");
      const s = await call("/v1/uyelik/urunler", null, "GET");
      if (s.status !== 200) return pSay(t.productsUnavailable, "");
      products = (s.data.urunler || []).filter((p) => p && p.site);
      for (const p of products) {
        const li = document.createElement("li");
        const name = document.createElement("span"); name.textContent = (p.ad || p.site) + (p.site === "saat-tarih" ? " · " + t.productThis : "");
        const st = document.createElement("span"); st.className = "muted"; st.textContent = p.acik ? t.productOn : t.productOff;
        li.append(name, st);
        if (p.site !== "saat-tarih") {
          const b = document.createElement("button"); b.type = "button"; b.textContent = p.acik ? t.close : t.open;
          b.addEventListener("click", () => setProducts([p.site], !p.acik));
          li.appendChild(b);
        }
        prodBox.appendChild(li);
      }
    }
    loadProducts();
    $("[data-open-all]").addEventListener("click", () => setProducts(products.filter((p) => !p.acik && p.site !== "saat-tarih").map((p) => p.site), true));
    $("[data-close-all]").addEventListener("click", () => setProducts(products.filter((p) => p.acik && p.site !== "saat-tarih").map((p) => p.site), false));

    // 10. Hesabı sil
    const df = $('[data-form="delete"]');
    df.addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!window.confirm(t.deleteConfirm)) return;
      busy(df, true);
      const s = await call("/v1/hesap/sil", { kapsam: df.kapsam.value, sifre: df.password.value || undefined });
      busy(df, false);
      if (s.status === 200) {
        document.querySelector("main").innerHTML = "";
        const sec = document.createElement("section"); sec.className = "card";
        const p = document.createElement("p"); p.className = "msg ok"; p.setAttribute("role", "status"); p.textContent = t.deleted;
        const a = document.createElement("a"); a.className = "button"; a.href = D.siteUrl; a.textContent = t.toSite;
        sec.append(p, a); document.querySelector("main").appendChild(sec);
        return;
      }
      say(df, errText(s), "err");
    });
  }

  async function badgesPage() {
    const r = await call("/v1/rozetler", null, "GET");
    if (r.status === 401) { location.href = "/giris?dil=" + lang; return; }
    const box = $("[data-badges]");
    const list = (r.data && r.data.rozetler) || [];
    const head = document.createElement("p"); head.className = "strong"; head.textContent = fill(t.badgesCount, { n: list.length });
    box.appendChild(head);
    if (!list.length) { const p = document.createElement("p"); p.className = "muted"; p.textContent = t.badgesNone; box.appendChild(p); }
    else {
      const ul = document.createElement("ul"); ul.className = "badges";
      for (const b of list) {
        const li = document.createElement("li");
        li.textContent = D.badgeNames[b.id] || b.id;
        const s = document.createElement("small"); s.textContent = fmtDate(b.gun + "T12:00:00"); li.appendChild(s);
        ul.appendChild(li);
      }
      box.appendChild(ul);
    }
    const k = await call("/v1/kayitlar", null, "GET");
    const rb = $("[data-records]");
    const recs = (k.data && k.data.kayitlar) || {};
    const n = Object.keys(recs).length;
    const p = document.createElement("p"); p.textContent = n ? fill(t.recordsCount, { n }) : t.recordsNone; rb.appendChild(p);
    const z = recs["zikir.total"];
    if (z && /^\d+$/.test(String(z.v).trim())) {
      const dl = document.createElement("dl"); dl.className = "facts";
      const dt = document.createElement("dt"); dt.textContent = t.zikirTotal;
      const dd = document.createElement("dd"); dd.textContent = Number(z.v).toLocaleString(numLocale);
      dl.append(dt, dd); rb.appendChild(dl);
    }
  }
})();
