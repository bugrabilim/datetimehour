(function () {
  "use strict";

  var doc = document;
  var root = doc.documentElement;
  var lang = root.lang === "en" ? "en" : "tr";
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
      .toLowerCase();
  }

  /* ---------- Dil: seçimi hatırla, ana sayfada kayıtlı dile yönlendir ---------- */
  var langLink = doc.querySelector("[data-lang-switch]");
  if (langLink) {
    langLink.addEventListener("click", function () {
      store("sth-lang", langLink.getAttribute("hreflang"));
      track("dil-degisimi", { dil: langLink.getAttribute("hreflang") });
    });
  }
  (function redirectToSavedLang() {
    var saved = store("sth-lang");
    if (!saved || saved === lang || !langLink || root.getAttribute("data-home") !== "1") return;
    var sameOrigin = false;
    try { sameOrigin = doc.referrer && new URL(doc.referrer).origin === location.origin; } catch (e) {}
    if (sameOrigin || saved !== langLink.getAttribute("hreflang")) return;
    location.replace(langLink.getAttribute("href") + location.hash);
  })();

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

  /* ---------- Saat ---------- */
  var prefs = { h12: false, sec: true };
  try { Object.assign(prefs, JSON.parse(store("sth-prefs") || "{}")); } catch (e) {}
  prefs.h12 = !!prefs.h12; prefs.sec = prefs.sec !== false;

  var localTz;
  try { localTz = Intl.DateTimeFormat().resolvedOptions().timeZone; } catch (e) {}
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

  var nf = new Intl.NumberFormat(locale);
  var liveEls = Array.prototype.slice.call(doc.querySelectorAll("[data-live]"));
  var analog = doc.querySelector("[data-analog]");

  function render() {
    var now = new Date();
    var localOff = offsetMin(now, localTz);
    liveEls.forEach(function (el) {
      var kind = el.getAttribute("data-live");
      var tz = el.getAttribute("data-tz") || localTz;
      var out = "";
      switch (kind) {
        case "time": out = timeText(now, tz, el.hasAttribute("data-sec") ? prefs.sec : false); break;
        case "date": out = fmt("date", tz, { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(now); break;
        case "tzname": out = (tz || "") + " · " + offsetLabel(offsetMin(now, tz)); break;
        case "offset": out = offsetLabel(offsetMin(now, tz)); break;
        case "diff":
          var d = offsetMin(now, tz) - localOff;
          out = d === 0 ? T.diffSame : tpl(d > 0 ? T.diffAhead : T.diffBehind, { d: durLabel(d) });
          break;
        case "isoweek": out = String(isoWeek(parts(now, tz))); break;
        case "doy": out = String(dayOfYear(parts(now, tz))); break;
        case "daysleft":
          var p = parts(now, tz);
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
      }
      if (el.textContent !== out) el.textContent = out;
    });
    if (analog) {
      var ap = parts(now, localTz);
      var s = ap.second, m = ap.minute + s / 60, h = (ap.hour % 12) + m / 60;
      analog.querySelector("[data-hand=h]").setAttribute("transform", "rotate(" + h * 30 + " 100 100)");
      analog.querySelector("[data-hand=m]").setAttribute("transform", "rotate(" + m * 6 + " 100 100)");
      analog.querySelector("[data-hand=s]").setAttribute("transform", "rotate(" + s * 6 + " 100 100)");
    }
  }
  function schedule() {
    render();
    setTimeout(schedule, 1000 - (Date.now() % 1000) + 5);
  }

  var btn12 = doc.querySelector("[data-pref=h12]");
  var btnSec = doc.querySelector("[data-pref=sec]");
  function syncButtons() {
    if (btn12) {
      btn12.setAttribute("aria-pressed", String(prefs.h12));
      btn12.textContent = prefs.h12 ? T.format12 : T.format24;
    }
    if (btnSec) btnSec.setAttribute("aria-pressed", String(prefs.sec));
  }
  function savePrefs() { store("sth-prefs", JSON.stringify(prefs)); syncButtons(); render(); }
  if (btn12) btn12.addEventListener("click", function () { prefs.h12 = !prefs.h12; savePrefs(); });
  if (btnSec) btnSec.addEventListener("click", function () { prefs.sec = !prefs.sec; savePrefs(); });
  syncButtons();
  if (liveEls.length) schedule();

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
})();
