(function () {
  "use strict";

  var doc = document;
  var root = doc.documentElement;
  var lang = root.lang || "tr";
  var cfgEl = doc.getElementById("page-config");
  var cfg = cfgEl ? JSON.parse(cfgEl.textContent) : { t: {}, locale: "tr-TR" };
  var T = cfg.t || {};
  var locale = cfg.locale || "tr-TR";

  function store(key, value) {
    try {
      if (value === undefined) return localStorage.getItem(key);
      localStorage.setItem(key, value);
    } catch (e) {}
    return null;
  }
  function track(name, data) {
    try {
      if (window.umami && typeof window.umami.track === "function") window.umami.track(name, data);
    } catch (e) {}
  }
  function tpl(s, vars) {
    return String(s).replace(/\{(\w+)\}/g, function (_, k) { return vars[k] != null ? vars[k] : ""; });
  }
  function norm(s) {
    return String(s || "")
      .replace(/İ/g, "i").replace(/ı/g, "i")
      .normalize("NFD").replace(/[̀-ͯ]/g, "")
      .toLowerCase()
      .replace(/ß/g, "ss").replace(/ə/g, "e").replace(/ø/g, "o")
      .replace(/[ً-ٟـ]/g, "").replace(/[أإآٱ]/g, "ا").replace(/ى/g, "ي").replace(/ة/g, "ه");
  }

  /* ---------- Dil: seçimi hatırla, ana sayfada kayıtlı dile yönlendir ---------- */
  var langLinks = doc.querySelectorAll("[data-lang-switch]");
  Array.prototype.forEach.call(langLinks, function (a) {
    a.addEventListener("click", function () {
      store("sth-lang", a.getAttribute("hreflang"));
      track("dil-degisimi", { dil: a.getAttribute("hreflang") });
    });
  });
  (function redirectToSavedLang() {
    var saved = store("sth-lang");
    if (!saved || saved === lang || !langLinks.length || root.getAttribute("data-home") !== "1") return;
    var sameOrigin = false;
    try { sameOrigin = doc.referrer && new URL(doc.referrer).origin === location.origin; } catch (e) {}
    if (sameOrigin) return;
    var target = null;
    Array.prototype.forEach.call(langLinks, function (a) { if (a.getAttribute("hreflang") === saved) target = a; });
    if (target) location.replace(target.getAttribute("href") + location.hash);
  })();
  Array.prototype.forEach.call(doc.querySelectorAll(".lang-menu"), function (m) {
    doc.addEventListener("click", function (e) { if (!m.contains(e.target)) m.removeAttribute("open"); });
    m.addEventListener("keydown", function (e) { if (e.key === "Escape") { m.removeAttribute("open"); var sm = m.querySelector("summary"); if (sm) sm.focus(); } });
  });

  /* ---------- Tema ---------- */
  var themeBtn = doc.querySelector("[data-theme-toggle]");
  if (themeBtn) {
    themeBtn.addEventListener("click", function () {
      var cur = root.getAttribute("data-theme");
      if (!cur) cur = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
      var next = cur === "dark" ? "light" : "dark";
      root.setAttribute("data-theme", next);
      store("sth-theme", next);
      track("tema-degisimi", { tema: next });
    });
  }


  /* ---------- Ortak tam ekran: Fullscreen API, yoksa sabit kaplama ---------- */
  function makeFullscreen(el, button, onChange) {
    var isFull = function () { return doc.fullscreenElement === el || el.classList.contains("is-full"); };
    var update = function () {
      var on = isFull();
      root.classList.toggle("has-full", on);
      if (button) {
        var label = on ? T.exitFullscreen : T.fullscreen;
        button.setAttribute("aria-pressed", String(on));
        button.setAttribute("aria-label", label);
        button.setAttribute("title", label);
      }
      if (onChange) onChange(on);
    };
    var toggle = function () {
      if (isFull()) {
        if (doc.fullscreenElement) doc.exitFullscreen(); else { el.classList.remove("is-full"); update(); }
        return;
      }
      track("tam-ekran");
      if (el.requestFullscreen) el.requestFullscreen().catch(function () { el.classList.add("is-full"); update(); });
      else { el.classList.add("is-full"); update(); }
    };
    if (button) button.addEventListener("click", toggle);
    doc.addEventListener("fullscreenchange", function () { update(); setTimeout(update, 60); });
    doc.addEventListener("keydown", function (ev) {
      var dl = doc.getElementById("search-dialog");
      if (ev.key === "Escape" && el.classList.contains("is-full") && !(dl && dl.open)) { el.classList.remove("is-full"); update(); }
    });
    return { toggle: toggle, isFull: isFull };
  }

  /* ---------- Saat ---------- */
  var prefs = { h12: false, sec: true, model: "", sync: true };
  try { Object.assign(prefs, JSON.parse(store("sth-prefs") || "{}")); } catch (e) {}
  if (window.__sthPrefs) Object.assign(prefs, window.__sthPrefs);
  prefs.h12 = !!prefs.h12; prefs.sec = prefs.sec !== false; prefs.sync = prefs.sync !== false;

  var localTz;
  try { localTz = Intl.DateTimeFormat().resolvedOptions().timeZone; } catch (e) {}

  /* ---------- Zaman kaynağı: sunucu saatiyle senkron (cihaz saatinden bağımsız) ---------- */
  var sync = { offset: 0, err: 0, ok: false, state: "pending", busy: false, at: 0 };
  function mono() { return performance.timeOrigin + performance.now(); }
  function nowMs() { return prefs.sync && sync.ok ? mono() + sync.offset : Date.now(); }
  function nowDate() { return new Date(nowMs()); }

  // Sunucunun Date başlığı saniye çözünürlüklüdür; farklı fazlardaki çok sayıda örneğin aralıklarını
  // kesiştirerek ofseti ~yüz milisaniyeye kadar daraltırız. Cihaz saati değişse bile mono() etkilenmez.
  function measure() {
    if (sync.busy) return;
    sync.busy = true;
    var lo = -Infinity, hi = Infinity, mids = [], n = 0, N = 16, good = 0;
    function finish() {
      sync.busy = false;
      if (!good) { sync.ok = false; sync.state = "fail"; return; }
      if (lo >= hi) { mids.sort(function (a, b) { return a - b; }); lo = hi = mids[Math.floor(mids.length / 2)]; }
      sync.offset = (lo + hi) / 2; sync.err = Math.max((hi - lo) / 2, 5);
      sync.ok = true; sync.state = "ok"; sync.at = mono();
    }
    function one() {
      var t0 = mono();
      fetch("/?_=" + Math.random().toString(36).slice(2), { method: "HEAD", cache: "no-store", credentials: "omit" })
        .then(function (r) {
          var t1 = mono(), d = Date.parse(r.headers.get("Date"));
          if (isNaN(d) || r.headers.get("Age") || t1 - t0 > 2000) return;
          var a = d - t1, b = d + 1000 - t0;
          lo = Math.max(lo, a); hi = Math.min(hi, b); mids.push((a + b) / 2); good++;
        })
        .catch(function () {})
        .then(function () { if (++n < N) setTimeout(one, 80 + Math.random() * 220); else finish(); });
    }
    one();
  }
  function deviceError() { return sync.ok ? Date.now() - (mono() + sync.offset) : 0; }
  var syncEls = Array.prototype.slice.call(doc.querySelectorAll("[data-sync-status]"));
  var nf1 = new Intl.NumberFormat(locale, { maximumFractionDigits: 1, minimumFractionDigits: 1 });
  function syncText() {
    if (!prefs.sync) return T.syncOff;
    if (sync.state === "pending") return T.syncPending;
    if (sync.state === "fail") return T.syncFail;
    var e = deviceError();
    if (Math.abs(e) < 500) return tpl(T.syncOk, { ms: Math.round(sync.err / 10) * 10 || 10 });
    return tpl(e > 0 ? T.syncAhead : T.syncBehind, { s: nf1.format(Math.abs(e) / 1000) });
  }
  function renderSync() {
    var txt = syncText();
    syncEls.forEach(function (el) { if (el.textContent !== txt) el.textContent = txt; });
  }
  setTimeout(measure, 400);
  setInterval(function () { if (prefs.sync) measure(); }, 15 * 60 * 1000);
  doc.addEventListener("visibilitychange", function () {
    if (!doc.hidden && prefs.sync && sync.ok && mono() - sync.at > 5 * 60 * 1000) measure();
  });

  var fmtCache = {};
  function fmt(key, tz, opts) {
    var k = key + "|" + (tz || "");
    if (!fmtCache[k]) {
      var o = Object.assign({}, opts);
      if (tz) o.timeZone = tz;
      try { fmtCache[k] = new Intl.DateTimeFormat(locale, o); }
      catch (e) { delete o.timeZone; fmtCache[k] = new Intl.DateTimeFormat(locale, o); }
    }
    return fmtCache[k];
  }
  function parts(date, tz) {
    var p = fmt("parts", tz, { year: "numeric", month: "numeric", day: "numeric", hour: "numeric", minute: "numeric", second: "numeric", hourCycle: "h23" }).formatToParts(date);
    var o = {};
    p.forEach(function (x) { if (x.type !== "literal") o[x.type] = parseInt(x.value, 10); });
    if (o.hour === 24) o.hour = 0;
    return o;
  }
  function offsetMin(date, tz) {
    var p = parts(date, tz);
    var asUtc = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second);
    return Math.round((asUtc - Math.floor(date.getTime() / 1000) * 1000) / 60000);
  }
  function offsetLabel(min) {
    var s = min < 0 ? "−" : "+";
    var a = Math.abs(min);
    var h = Math.floor(a / 60), m = a % 60;
    return "UTC" + s + String(h).padStart(2, "0") + ":" + String(m).padStart(2, "0");
  }
  function durLabel(min) {
    var a = Math.abs(min), h = Math.floor(a / 60), m = a % 60, out = [];
    if (h) out.push(h + " " + T.hourShort);
    if (m) out.push(m + " " + T.minuteShort);
    return out.join(" ");
  }
  function timeText(date, tz, withSec) {
    var o = { hour: "2-digit", minute: "2-digit", hourCycle: prefs.h12 ? "h12" : "h23" };
    if (withSec) o.second = "2-digit";
    return fmt("time" + (prefs.h12 ? 12 : 24) + (withSec ? "s" : ""), tz, o).format(date);
  }
  function dayPeriod(date, tz) {
    var f = fmt("dp", tz, { hour: "numeric", hour12: true }).formatToParts(date);
    for (var i = 0; i < f.length; i++) if (f[i].type === "dayPeriod") return f[i].value;
    return "";
  }
function sunTimes(y, m, d, lat, lon) {
    var rad = Math.PI / 180;
    var jd = Date.UTC(y, m - 1, d, 12) / 86400000 + 2440587.5;
    var n = Math.round(jd - 2451545.0);
    var js = n - lon / 360;
    var M = (357.5291 + 0.98560028 * js) % 360;
    var C = 1.9148 * Math.sin(M * rad) + 0.02 * Math.sin(2 * M * rad) + 0.0003 * Math.sin(3 * M * rad);
    var lam = (M + C + 180 + 102.9372) % 360;
    var jt = 2451545.0 + js + 0.0053 * Math.sin(M * rad) - 0.0069 * Math.sin(2 * lam * rad);
    var dec = Math.asin(Math.sin(lam * rad) * Math.sin(23.4397 * rad));
    var cw = (Math.sin(-0.833 * rad) - Math.sin(lat * rad) * Math.sin(dec)) / (Math.cos(lat * rad) * Math.cos(dec));
    if (cw < -1 || cw > 1) return null;
    var w = Math.acos(cw) / rad / 360;
    var ms = function (j) { return (j - 2440587.5) * 86400000; };
    return { rise: ms(jt - w), set: ms(jt + w) };
  }
  function isoWeek(p) {
    var d = new Date(Date.UTC(p.year, p.month - 1, p.day));
    var day = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - day);
    var y0 = Date.UTC(d.getUTCFullYear(), 0, 1);
    return Math.ceil(((d - y0) / 86400000 + 1) / 7);
  }
  function dayOfYear(p) {
    return Math.round((Date.UTC(p.year, p.month - 1, p.day) - Date.UTC(p.year, 0, 0)) / 86400000);
  }
  function daysInYear(y) { return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0 ? 366 : 365; }

  /* Tarayıcının Intl verisi bu dili bilmiyorsa (ör. kısaltılmış ICU) tarih adları i18n dizilerinden gelir */
  var intlOk = true;
  try { intlOk = !/^M\d/.test(new Intl.DateTimeFormat(locale, { month: "long" }).format(new Date(2026, 9, 9))); } catch (e) { intlOk = false; }
  var CAL = cfg.cal || {};
  var useOwn = !intlOk && CAL.months && CAL.weekdays && CAL.fullFmt;
  function dowOfParts(p) { return (new Date(Date.UTC(p.year, p.month - 1, p.day)).getUTCDay() + 6) % 7; }
  function ownDate(p) { return tpl(CAL.fullFmt, { weekday: CAL.weekdays[dowOfParts(p)], day: p.day, month: CAL.months[p.month - 1], year: p.year }); }

  function weekRangeText(p, tz) {
    var dow = (new Date(Date.UTC(p.year, p.month - 1, p.day)).getUTCDay() + 6) % 7;
    var a = new Date(Date.UTC(p.year, p.month - 1, p.day - dow, 12)), b = new Date(Date.UTC(p.year, p.month - 1, p.day - dow + 6, 12));
    if (useOwn) return tpl(CAL.shortFmt, { day: a.getUTCDate(), month: CAL.months[a.getUTCMonth()] }) + " – " + tpl(CAL.dateFmt, { day: b.getUTCDate(), month: CAL.months[b.getUTCMonth()], year: b.getUTCFullYear() });
    var f = fmt("wr", "UTC", { day: "numeric", month: "long", year: "numeric" });
    return typeof f.formatRange === "function" ? f.formatRange(a, b) : f.format(a) + " – " + f.format(b);
  }
  var W = T.words || { units: [], tens: [], join: " ", oh: "", sentence: "{h} {m}", oclock: "{h}" };
  function numWords(n) {
    if (n < 20) return W.units[n];
    var u = n % 10, t = W.tens[Math.floor(n / 10)];
    if (!u) return t;
    return W.unitFirst ? (u === 1 && W.one ? W.one : W.units[u]) + W.join + t : t + W.join + W.units[u];
  }
  function hourWords(h) { return W.hours && W.hours[h] ? W.hours[h] : numWords(h); }
  function wordsText(p) {
    var h = prefs.h12 ? (p.hour % 12 || 12) : p.hour;
    if (p.minute === 0) return tpl(W.oclock, { h: hourWords(h) });
    var m = p.minute < 10 ? (W.oh ? W.oh + " " : "") + W.units[p.minute] : numWords(p.minute);
    return tpl(W.sentence, { h: hourWords(h), m: m });
  }

  var calCache = new WeakMap();
  function renderCalendar(el, p, tz) {
    var key = p.year + "-" + p.month + "-" + p.day;
    if (calCache.get(el) === key) return;
    calCache.set(el, key);
    var title = el.querySelector("[data-cal-title]");
    var head = el.querySelector("[data-cal-head]");
    var body = el.querySelector("[data-cal-body]");
    title.textContent = useOwn ? CAL.months[p.month - 1] + " " + p.year : fmt("calTitle", "UTC", { month: "long", year: "numeric" }).format(new Date(Date.UTC(p.year, p.month - 1, 1, 12)));
    head.textContent = "";
    var wk = doc.createElement("th");
    wk.scope = "col"; wk.textContent = T.weekShort;
    head.appendChild(wk);
    for (var i = 0; i < 7; i++) {
      var th = doc.createElement("th");
      th.scope = "col";
      th.textContent = useOwn && CAL.weekdaysShort ? CAL.weekdaysShort[i] : fmt("wd", "UTC", { weekday: "short" }).format(new Date(Date.UTC(2024, 0, 1 + i, 12))); // 1 Ocak 2024 pazartesi
      head.appendChild(th);
    }
    body.textContent = "";
    var first = (new Date(Date.UTC(p.year, p.month - 1, 1)).getUTCDay() + 6) % 7;
    var count = new Date(Date.UTC(p.year, p.month, 0)).getUTCDate();
    var day = 1 - first;
    while (day <= count) {
      var tr = doc.createElement("tr");
      var wh = doc.createElement("th");
      wh.scope = "row";
      var ref = Math.max(day, 1);
      wh.textContent = String(isoWeek({ year: p.year, month: p.month, day: ref }));
      tr.appendChild(wh);
      for (var c = 0; c < 7; c++, day++) {
        var td = doc.createElement("td");
        if (day >= 1 && day <= count) {
          td.textContent = String(day);
          if (day === p.day) { td.className = "today"; td.setAttribute("aria-current", "date"); }
        }
        tr.appendChild(td);
      }
      body.appendChild(tr);
    }
  }

  var nf = new Intl.NumberFormat(locale);
  var liveEls = Array.prototype.slice.call(doc.querySelectorAll("[data-live]"));
  var analogs = Array.prototype.slice.call(doc.querySelectorAll("[data-analog]"));

  function render() {
    var now = nowDate();
    renderSync();
    var localOff = offsetMin(now, localTz);
    liveEls.forEach(function (el) {
      var kind = el.getAttribute("data-live");
      var tz = el.getAttribute("data-tz") || localTz;
      var out = "", p;
      switch (kind) {
        case "time": out = timeText(now, tz, el.hasAttribute("data-sec") ? prefs.sec : false); break;
        case "date": out = useOwn ? ownDate(parts(now, tz)) : fmt("date", tz, { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(now); break;
        case "day": out = String(parts(now, tz).day); break;
        case "dateiso": p = parts(now, tz); out = p.year + "-" + String(p.month).padStart(2, "0") + "-" + String(p.day).padStart(2, "0"); break;
        case "datenum": out = fmt("dn", tz, { day: "2-digit", month: "2-digit", year: "numeric" }).format(now); break;
        case "monthleft": p = parts(now, tz); out = String(new Date(Date.UTC(p.year, p.month, 0)).getUTCDate() - p.day); break;
        case "weekrange": out = weekRangeText(parts(now, tz), tz); break;
        case "sunrise": case "sunset": case "daylen":
          p = parts(now, tz);
          var st = sunTimes(p.year, p.month, p.day, parseFloat(el.getAttribute("data-lat")), parseFloat(el.getAttribute("data-lon")));
          if (!st) { out = "–"; break; }
          if (kind === "daylen") { var mins = Math.round((st.set - st.rise) / 60000); out = Math.floor(mins / 60) + " " + T.hourShort + " " + (mins % 60) + " " + T.minuteShort; }
          else out = fmt("sun", tz, { hour: "2-digit", minute: "2-digit", hourCycle: prefs.h12 ? "h12" : "h23" }).format(new Date(kind === "sunrise" ? st.rise : st.set));
          break;
        case "monthyear": p = parts(now, tz); out = useOwn ? CAL.months[p.month - 1] + " " + p.year : fmt("my", tz, { month: "long", year: "numeric" }).format(now); break;
        case "weekday": out = useOwn ? CAL.weekdays[dowOfParts(parts(now, tz))] : fmt("wdl", tz, { weekday: "long" }).format(now); break;
        case "ampm": out = prefs.h12 ? dayPeriod(now, tz) : ""; break;
        case "words": out = wordsText(parts(now, tz)); break;
        case "tzname": out = (tz || "") + " · " + offsetLabel(offsetMin(now, tz)); break;
        case "offset": out = offsetLabel(offsetMin(now, tz)); break;
        case "diff":
          var refTz = el.getAttribute("data-ref");
          var d = offsetMin(now, tz) - (refTz ? offsetMin(now, refTz) : localOff);
          if (refTz) { var rn = el.getAttribute("data-ref-name"); out = d === 0 ? tpl(T.refSame, { ref: rn }) : tpl(d > 0 ? T.refAhead : T.refBehind, { d: durLabel(d), ref: rn }); }
          else out = d === 0 ? T.diffSame : tpl(d > 0 ? T.diffAhead : T.diffBehind, { d: durLabel(d) });
          break;
        case "isoweek": out = String(isoWeek(parts(now, tz))); break;
        case "doy": out = String(dayOfYear(parts(now, tz))); break;
        case "daysleft":
          p = parts(now, tz);
          out = String(daysInYear(p.year) - dayOfYear(p));
          break;
        case "unix": out = nf.format(Math.floor(now.getTime() / 1000)); break;
        case "utc": out = timeText(now, "UTC", true); break;
        case "progress":
          var q = parts(now, tz);
          var pct = Math.min(100, Math.max(0, (dayOfYear(q) / daysInYear(q.year)) * 100));
          el.value = pct;
          el.textContent = Math.floor(pct) + "%";
          var note = el.parentNode.querySelector("[data-progress-note]");
          if (note) note.textContent = tpl(T.yearDone, { p: Math.floor(pct) });
          return;
        case "calendar": renderCalendar(el, parts(now, tz), tz); return;
        case "flip":
          p = parts(now, tz);
          var hh = prefs.h12 ? (p.hour % 12 || 12) : p.hour;
          var digits = [Math.floor(hh / 10), hh % 10, Math.floor(p.minute / 10), p.minute % 10, Math.floor(p.second / 10), p.second % 10];
          el.querySelectorAll("[data-flip-d]").forEach(function (n) {
            var v = String(digits[+n.getAttribute("data-flip-d")]);
            if (n.textContent !== v) n.textContent = v;
          });
          return;
        case "rings":
          p = parts(now, tz);
          var vals = { h: (((p.hour % 12) + p.minute / 60) / 12) * 100, m: ((p.minute + p.second / 60) / 60) * 100, s: (p.second / 60) * 100 };
          el.querySelectorAll("[data-ring]").forEach(function (c) {
            c.setAttribute("stroke-dasharray", vals[c.getAttribute("data-ring")].toFixed(2) + " 100");
          });
          return;
      }
      if (el.textContent !== out) el.textContent = out;
    });
    analogs.forEach(function (svg) {
      var ap = parts(now, svg.getAttribute("data-tz") || localTz);
      var s = ap.second, m = ap.minute + s / 60, h = (ap.hour % 12) + m / 60;
      svg.querySelector("[data-hand=h]").setAttribute("transform", "rotate(" + h * 30 + " 100 100)");
      svg.querySelector("[data-hand=m]").setAttribute("transform", "rotate(" + m * 6 + " 100 100)");
      svg.querySelector("[data-hand=s]").setAttribute("transform", "rotate(" + s * 6 + " 100 100)");
    });
  }
  function schedule() {
    render();
    setTimeout(schedule, 1000 - (Date.now() % 1000) + 5);
  }

  var btn12 = doc.querySelector("[data-pref=h12]");
  var btnSec = doc.querySelector("[data-pref=sec]");
  var btnSync = doc.querySelector("[data-pref=sync]");
  function syncButtons() {
    if (btn12) {
      btn12.setAttribute("aria-pressed", String(prefs.h12));
      btn12.textContent = prefs.h12 ? T.format12 : T.format24;
    }
    if (btnSec) btnSec.setAttribute("aria-pressed", String(prefs.sec));
    if (btnSync) btnSync.setAttribute("aria-pressed", String(prefs.sync));
    root.classList.toggle("sec-off", !prefs.sec);
  }
  function savePrefs() { store("sth-prefs", JSON.stringify(prefs)); syncButtons(); render(); }
  if (btn12) btn12.addEventListener("click", function () { prefs.h12 = !prefs.h12; savePrefs(); });
  if (btnSec) btnSec.addEventListener("click", function () { prefs.sec = !prefs.sec; savePrefs(); });
  if (btnSync) btnSync.addEventListener("click", function () {
    prefs.sync = !prefs.sync; savePrefs();
    if (prefs.sync && !sync.ok) { sync.state = "pending"; measure(); }
  });
  syncButtons();
  if (liveEls.length) schedule();

  /* ---------- Saat modelleri: kaydırmalı karusel ve tam ekran ---------- */
  var stage = doc.querySelector("[data-carousel]");
  if (stage) {
    var trackEl = stage.querySelector("[data-track]");
    var slides = Array.prototype.slice.call(trackEl.children);
    var dots = Array.prototype.slice.call(stage.querySelectorAll("[data-dot]"));
    var nameEl = stage.querySelector("[data-stage-name]");
    var reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    var current = 0, ready = false, settleTimer = null, reported = -1;

    var setActive = function (i) {
      if (i === current && slides[i].getAttribute("aria-hidden") === "false") return;
      current = i;
      slides.forEach(function (s, n) { s.setAttribute("aria-hidden", n === i ? "false" : "true"); });
      dots.forEach(function (d, n) { if (n === i) d.setAttribute("aria-current", "true"); else d.removeAttribute("aria-current"); });
      nameEl.textContent = slides[i].getAttribute("data-name");
    };
    var goTo = function (i, smooth) {
      i = (i + slides.length) % slides.length;
      trackEl.scrollTo({ left: i * trackEl.clientWidth, behavior: smooth && !reduceMotion ? "smooth" : "auto" });
      setActive(i);
    };
    var indexFromHash = function () {
      var m = /^#model-(.+)$/.exec(location.hash);
      if (!m) return -1;
      for (var n = 0; n < slides.length; n++) if (slides[n].getAttribute("data-model") === m[1]) return n;
      return -1;
    };

    trackEl.addEventListener("scroll", function () {
      var i = Math.round(trackEl.scrollLeft / Math.max(trackEl.clientWidth, 1));
      if (i >= 0 && i < slides.length) setActive(i);
      clearTimeout(settleTimer);
      settleTimer = setTimeout(function () {
        if (!ready || current === reported) return;
        reported = current;
        prefs.model = slides[current].getAttribute("data-model");
        store("sth-prefs", JSON.stringify(prefs));
        track("saat-modeli", { model: prefs.model });
      }, 250);
    }, { passive: true });
    dots.forEach(function (d, n) { d.addEventListener("click", function () { goTo(n, true); }); });
    stage.querySelectorAll("[data-nav]").forEach(function (b) {
      b.addEventListener("click", function () { goTo(current + parseInt(b.getAttribute("data-nav"), 10), true); });
    });
    trackEl.addEventListener("keydown", function (ev) {
      if (ev.altKey || ev.ctrlKey || ev.metaKey) return;
      if (ev.key === "ArrowRight") { ev.preventDefault(); goTo(current + 1, true); }
      else if (ev.key === "ArrowLeft") { ev.preventDefault(); goTo(current - 1, true); }
      else if (ev.key === "Home") { ev.preventDefault(); goTo(0, true); }
      else if (ev.key === "End") { ev.preventDefault(); goTo(slides.length - 1, true); }
      else if (ev.key === "f" || ev.key === "F") { ev.preventDefault(); toggleFull(); }
    });
    window.addEventListener("resize", function () { trackEl.scrollTo({ left: current * trackEl.clientWidth, behavior: "auto" }); });
    window.addEventListener("hashchange", function () { var i = indexFromHash(); if (i > -1) goTo(i, true); });

    var fsBtn = stage.querySelector("[data-fullscreen]");
    var full = makeFullscreen(stage, fsBtn, function () { trackEl.scrollTo({ left: current * trackEl.clientWidth, behavior: "auto" }); });
    var toggleFull = full.toggle;

    // Açılışta: bağlantıdaki model, yoksa son seçilen model
    var start = indexFromHash();
    if (start < 0 && prefs.model) {
      for (var n = 0; n < slides.length; n++) if (slides[n].getAttribute("data-model") === prefs.model) start = n;
    }
    if (start > 0) goTo(start, false);
    reported = current;
    setTimeout(function () { ready = true; }, 400);
  }

  /* ---------- E-posta listesi: sonuç iletisi ve Umami olayı ---------- */
  (function listResult() {
    var id = location.hash.replace("#", "");
    if (id !== "liste-tamam" && id !== "liste-hata") return;
    var el = doc.getElementById(id);
    if (el) { el.classList.add("is-shown"); el.setAttribute("tabindex", "-1"); el.focus(); }
    if (id === "liste-tamam") track("liste-katil");
    try { history.replaceState(null, "", location.pathname + location.search); } catch (e) {}
  })();

  /* ---------- Site içi arama ---------- */
  var dlg = doc.getElementById("search-dialog");
  var openers = doc.querySelectorAll("[data-search-open]");
  if (dlg && typeof dlg.showModal === "function") {
    var input = doc.getElementById("search-input");
    var list = doc.getElementById("search-results");
    var status = doc.getElementById("search-status");
    var index = null, loading = false, active = -1, timer = null, lastTracked = "";

    function loadIndex(cb) {
      if (index) return cb();
      if (loading) return;
      loading = true;
      fetch(cfg.searchIndex, { credentials: "omit" })
        .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
        .then(function (j) {
          index = j.map(function (e) {
            e._t = norm(e.t); e._k = norm(e.k || ""); e._d = norm(e.d || "");
            return e;
          });
          loading = false; cb();
        })
        .catch(function () { loading = false; status.textContent = T.searchError; });
    }
    function search(q) {
      var terms = norm(q).split(/\s+/).filter(Boolean);
      if (!terms.length) return [];
      var res = [];
      index.forEach(function (e) {
        var score = 0;
        for (var i = 0; i < terms.length; i++) {
          var t = terms[i], s = 0;
          if (e._t.indexOf(t) === 0) s = 12;
          else if (e._t.indexOf(t) > -1) s = 8;
          else if (e._k.indexOf(t) > -1) s = 5;
          else if (e._d.indexOf(t) > -1) s = 2;
          if (!s) return;
          score += s;
        }
        res.push({ e: e, s: score });
      });
      res.sort(function (a, b) { return b.s - a.s; });
      return res.slice(0, 12).map(function (r) { return r.e; });
    }
    function setActive(i) {
      var links = list.querySelectorAll("a");
      if (!links.length) { active = -1; input.removeAttribute("aria-activedescendant"); return; }
      active = (i + links.length) % links.length;
      links.forEach(function (a, n) { a.setAttribute("aria-selected", String(n === active)); });
      input.setAttribute("aria-activedescendant", links[active].id);
      links[active].scrollIntoView({ block: "nearest" });
    }
    function show(q) {
      list.textContent = "";
      active = -1;
      input.removeAttribute("aria-activedescendant");
      if (!q.trim()) { status.textContent = T.searchHint; return; }
      var res = search(q);
      if (!res.length) { status.textContent = T.searchEmpty; return; }
      status.textContent = tpl(T.searchCount, { n: res.length });
      res.forEach(function (e, n) {
        var li = doc.createElement("li");
        li.setAttribute("role", "presentation");
        var a = doc.createElement("a");
        a.href = e.u; a.id = "sr-" + n; a.setAttribute("role", "option"); a.setAttribute("aria-selected", "false");
        var t = doc.createElement("span"); t.className = "r-title"; t.textContent = e.t;
        var d = doc.createElement("span"); d.className = "r-desc"; d.textContent = e.d || "";
        a.appendChild(t); a.appendChild(d); li.appendChild(a); list.appendChild(li);
      });
      setActive(0);
      clearTimeout(timer);
      var key = norm(q);
      timer = setTimeout(function () {
        if (key.length > 1 && key !== lastTracked) { lastTracked = key; track("arama", { sonuc: res.length ? "var" : "yok" }); }
      }, 900);
    }
    function openSearch() {
      if (!dlg.open) dlg.showModal();
      status.textContent = T.searchHint;
      input.focus(); input.select();
      loadIndex(function () { show(input.value); });
    }
    openers.forEach(function (b) { b.addEventListener("click", openSearch); });
    input.addEventListener("input", function () { loadIndex(function () { show(input.value); }); });
    input.addEventListener("keydown", function (ev) {
      if (ev.key === "Escape") { ev.preventDefault(); dlg.close(); }
      else if (ev.key === "ArrowDown") { ev.preventDefault(); setActive(active + 1); }
      else if (ev.key === "ArrowUp") { ev.preventDefault(); setActive(active - 1); }
      else if (ev.key === "Enter") {
        var links = list.querySelectorAll("a");
        if (links[active]) { ev.preventDefault(); location.href = links[active].getAttribute("href"); }
      }
    });
    doc.getElementById("search-close").addEventListener("click", function () { dlg.close(); });
    dlg.addEventListener("click", function (ev) { if (ev.target === dlg) dlg.close(); });
    doc.addEventListener("keydown", function (ev) {
      var tag = (ev.target && ev.target.tagName) || "";
      var typing = /^(INPUT|TEXTAREA|SELECT)$/.test(tag) || (ev.target && ev.target.isContentEditable);
      if ((ev.key === "k" && (ev.ctrlKey || ev.metaKey)) || (ev.key === "/" && !typing && !ev.ctrlKey && !ev.metaKey)) {
        ev.preventDefault(); openSearch();
      }
    });
  } else {
    openers.forEach(function (b) { b.hidden = true; });
  }

  window.sth = {
    T: T, cfg: cfg, lang: lang, locale: locale, store: store, track: track, tpl: tpl, norm: norm,
    nowMs: nowMs, nowDate: nowDate, mono: mono, prefs: prefs, makeFullscreen: makeFullscreen,
    parts: parts, offsetMin: offsetMin, fmt: fmt, localTz: localTz, durLabel: durLabel,
    syncState: function () { return sync; },
  };
})();
