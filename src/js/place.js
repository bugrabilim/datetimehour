/* Ortak konum: bütün sayfalarda tek düğme (üst bar) ve tek pencere. İl listesi ya da "konumumu kullan".
   Namaz vakitleri, mevsimler ve hava durumu (hava durumlu saat modelleri dahil) buradaki seçimi kullanır; bir kez seçilir.
   Konum cihazda kalır; son seçim yalnız bu tarayıcıda (localStorage: sth-place). */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;
  var TR_TZ = "Europe/Istanbul";
  var T = function () { return S.T.pl || {}; };
  var dlg = doc.getElementById("place-dialog");
  var sel = dlg && dlg.querySelector("[data-place-city]"), geo = dlg && dlg.querySelector("[data-place-geo]"), st = dlg && dlg.querySelector("[data-place-status]");
  var listeners = [];

  function read() {
    try { var v = JSON.parse(S.store("sth-place") || "null"); return v && typeof v === "object" ? v : null; } catch (e) { return null; }
  }
  function write(p) {
    S.store("sth-place", JSON.stringify(p.kind === "geo" ? { kind: "geo", lat: p.lat, lon: p.lon, name: p.name || "" } : { kind: "city", city: p.city, name: p.name, lat: p.lat, lon: p.lon }));
  }
  function geoPlace(lat, lon, name) {
    return { kind: "geo", name: name || "", lat: lat, lon: lon, tz: S.localTz };
  }
  /* Koordinattan şehir adı: önce en yakın il (100 km içinde), yoksa dünya yerleri listesinden en yakını; koordinat hiçbir yerde gösterilmez */
  function dist(a, b, c, d) {
    var r = Math.PI / 180, dl = (d - b) * r, x = Math.sin((c - a) * r / 2), y = Math.sin(dl / 2);
    var h = x * x + Math.cos(a * r) * Math.cos(c * r) * y * y;
    return 12742 * Math.asin(Math.min(1, Math.sqrt(h)));
  }
  function nearestProvince(lat, lon) {
    var best = null;
    if (!sel) return null;
    for (var i = 0; i < sel.options.length; i++) {
      var o = sel.options[i], d = dist(lat, lon, parseFloat(o.getAttribute("data-lat")), parseFloat(o.getAttribute("data-lon")));
      if (!best || d < best.d) best = { d: d, name: o.textContent };
    }
    return best;
  }
  function resolveName(lat, lon, cb) {
    var np = nearestProvince(lat, lon), pd = S.cfg.placeData;
    if ((np && np.d <= 100) || !pd) return cb(np ? np.name : "");
    Promise.all([fetch(pd.places, { credentials: "omit" }).then(function (r) { return r.json(); }), fetch(pd.names, { credentials: "omit" }).then(function (r) { return r.json(); })])
      .then(function (a) {
        var best = null;
        a[0].p.forEach(function (r, i) { var d = dist(lat, lon, r[0], r[1]); if (!best || d < best.d) best = { d: d, name: a[1][i] }; });
        cb(best && (best.d < (np ? np.d : 1e9)) ? best.name : (np ? np.name : ""));
      }).catch(function () { cb(np ? np.name : ""); });
  }
  function cityPlace(opt) {
    return { kind: "city", city: opt.value, name: opt.textContent, lat: parseFloat(opt.getAttribute("data-lat")), lon: parseFloat(opt.getAttribute("data-lon")), tz: TR_TZ };
  }
  /* Kayıtlı konum (yoksa null) */
  function saved() {
    var v = read();
    if (!v || !isFinite(v.lat) || !isFinite(v.lon)) return null;
    return v.kind === "geo" ? geoPlace(v.lat, v.lon, v.name) : { kind: "city", city: v.city, name: v.name || "", lat: v.lat, lon: v.lon, tz: TR_TZ };
  }
  /* Kayıtlı konum ya da öntanımlı (İstanbul) */
  function current() {
    var s = saved();
    if (s) return s;
    if (!sel) return null;
    var i = 0;
    for (var k = 0; k < sel.options.length; k++) if (sel.options[k].defaultSelected) i = k;
    return cityPlace(sel.options[i]);
  }
  function label(p) {
    Array.prototype.forEach.call(doc.querySelectorAll("[data-place-label]"), function (e) { e.textContent = p ? p.name : ""; });
  }
  function emit() {
    var p = current();
    label(p);
    listeners.forEach(function (cb) { cb(p); });
    try { window.dispatchEvent(new CustomEvent("sth-place", { detail: p })); } catch (e) {}
  }
  function syncSelect() {
    if (!sel) return;
    var v = read();
    if (v && v.kind === "city") for (var i = 0; i < sel.options.length; i++) if (sel.options[i].value === v.city) sel.selectedIndex = i;
  }
  function open() {
    if (!dlg) return;
    syncSelect();
    if (st) st.textContent = "";
    if (typeof dlg.showModal === "function") { if (!dlg.open) dlg.showModal(); } else dlg.setAttribute("open", "");
  }
  function close() { if (!dlg) return; if (typeof dlg.close === "function") dlg.close(); else dlg.removeAttribute("open"); }

  /* Sayfa kullanımı: cb(place) hemen ve konum her değiştiğinde çağrılır */
  function use(cb) {
    listeners.push(cb);
    cb(current());
  }

  if (dlg) {
    dlg.addEventListener("click", function (e) { if (e.target === dlg || (e.target.closest && e.target.closest("[data-place-close]"))) close(); });
    sel.addEventListener("change", function () {
      var p = cityPlace(sel.options[sel.selectedIndex]);
      write(p); if (st) st.textContent = "";
      S.track("konum-secimi", { tur: "il" });
      emit(); close();
    });
    if (geo) geo.addEventListener("click", function () {
      if (!navigator.geolocation) { if (st) st.textContent = T().locationUnsupported; return; }
      if (st) st.textContent = T().locating;
      navigator.geolocation.getCurrentPosition(function (pos) {
        if (st) st.textContent = "";
        var la = pos.coords.latitude, lo = pos.coords.longitude;
        S.track("konum-secimi", { tur: "konum" });
        resolveName(la, lo, function (nm) { write(geoPlace(la, lo, nm)); emit(); close(); });
      }, function () { if (st) st.textContent = T().locationDenied; }, { enableHighAccuracy: false, timeout: 12000, maximumAge: 600000 });
    });
  }
  doc.addEventListener("click", function (e) {
    var b = e.target.closest && e.target.closest("[data-place-open]");
    if (b) { e.preventDefault(); open(); }
  });

  window.sthPlace = { use: use, attach: function (root, cb) { use(cb); }, saved: saved, current: current, open: open };
  label(current());
  var sv0 = read();
  if (sv0 && sv0.kind === "geo" && !sv0.name && isFinite(sv0.lat) && isFinite(sv0.lon)) resolveName(sv0.lat, sv0.lon, function (nm) { write(geoPlace(sv0.lat, sv0.lon, nm)); emit(); });
  /* Konuma bağlı sayfada henüz seçim yoksa bir kez sor */
  if (!saved() && doc.body.hasAttribute("data-needs-place")) open();
})();
