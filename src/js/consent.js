/* Çerez onayı (Bumba standardı madde 14): ilk ziyarette bant, tercihler penceresi, Umami yalnız istatistik izniyle.
   Seçim `bumba-cerez` anahtarında: {"surum":"2026-10-21","istatistik":bool,"dis":bool}. "dis": dış hizmetler
   (Open-Meteo hava durumu, OpenStreetMap harita karoları), varsayılan kapalı. main.js'ten önce çalışır. */
(function () {
  "use strict";
  var doc = document;
  var KEY = "bumba-cerez", VER = "2026-10-21";
  var cfgEl = doc.getElementById("page-config");
  var cfg = {};
  try { cfg = cfgEl ? JSON.parse(cfgEl.textContent) : {}; } catch (e) {}
  var choice = null;
  try {
    var raw = JSON.parse(localStorage.getItem(KEY) || "null");
    if (raw && raw.surum === VER && typeof raw.istatistik === "boolean") choice = { istatistik: raw.istatistik, dis: raw.dis === true };
  } catch (e) {}

  var queue = [], umamiState = 0; // 0 yok, 1 yükleniyor, 2 hazır
  function loadUmami() {
    var u = cfg.umami;
    if (!u || !u.id || umamiState) return;
    umamiState = 1;
    var s = doc.createElement("script");
    s.defer = true;
    s.src = "https://istatistik.bumba.tr/script.js";
    s.setAttribute("data-website-id", u.id);
    s.setAttribute("data-domains", u.host);
    s.onload = function () { umamiState = 2; flush(); };
    doc.head.appendChild(s);
  }
  function flush() {
    if (!window.umami || typeof window.umami.track !== "function") return;
    while (queue.length) { var q = queue.shift(); try { window.umami.track(q[0], q[1]); } catch (e) {} }
  }
  function stats() { return !!(choice && choice.istatistik); }
  function ext() { return !!(choice && choice.dis); }
  /* Olay yalnız istatistik izni varsa gider; betik henüz yüklenmediyse sırada bekler */
  function track(name, data) {
    if (!stats()) return;
    queue.push([name, data]);
    if (umamiState === 2) flush();
  }
  if (stats()) loadUmami();

  var bar = doc.getElementById("cookie-bar"), dlg = doc.getElementById("cookie-dialog");
  function save(next) {
    var before = choice;
    choice = { istatistik: !!next.istatistik, dis: !!next.dis };
    try { localStorage.setItem(KEY, JSON.stringify({ surum: VER, istatistik: choice.istatistik, dis: choice.dis })); } catch (e) {}
    if (bar) bar.hidden = true;
    doc.documentElement.classList.remove("has-cookie-bar");
    closePrefs();
    if (choice.istatistik) loadUmami();
    if (!before || before.dis !== choice.dis || before.istatistik !== choice.istatistik) {
      try { window.dispatchEvent(new CustomEvent("sth-consent", { detail: { istatistik: choice.istatistik, dis: choice.dis } })); } catch (e) {}
    }
  }
  function openPrefs() {
    if (!dlg) return;
    var st = dlg.querySelector("[data-consent-stats]"), ex = dlg.querySelector("[data-consent-ext]");
    if (st) st.checked = stats();
    if (ex) ex.checked = ext();
    if (typeof dlg.showModal === "function") dlg.showModal(); else dlg.setAttribute("open", "");
  }
  function closePrefs() { if (dlg) { if (typeof dlg.close === "function") dlg.close(); else dlg.removeAttribute("open"); } }

  if (bar && !choice) { bar.hidden = false; doc.documentElement.classList.add("has-cookie-bar"); }
  doc.addEventListener("click", function (e) {
    var t = e.target && e.target.closest ? e.target : null;
    if (!t) return;
    if (t.closest("[data-consent-accept]")) save({ istatistik: true, dis: true });
    else if (t.closest("[data-consent-reject]")) save({ istatistik: false, dis: false });
    else if (t.closest("[data-consent-open]")) { e.preventDefault(); openPrefs(); }
    else if (t.closest("[data-consent-allow-ext]")) { e.preventDefault(); save({ istatistik: stats(), dis: true }); }
    else if (t.closest("[data-consent-save]")) {
      var st = dlg.querySelector("[data-consent-stats]"), ex = dlg.querySelector("[data-consent-ext]");
      save({ istatistik: st && st.checked, dis: ex && ex.checked });
    } else if (t.closest("[data-consent-close]") || t === dlg) closePrefs();
  });

  window.sthConsent = { stats: stats, ext: ext, track: track, open: openPrefs, chosen: function () { return !!choice; } };
})();
