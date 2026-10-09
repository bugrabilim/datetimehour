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
    S.store("sth-place", JSON.stringify(p.kind === "geo" ? { kind: "geo", lat: p.lat, lon: p.lon } : { kind: "city", city: p.city, name: p.name, lat: p.lat, lon: p.lon }));
  }
  function geoPlace(lat, lon) {
    return { kind: "geo", name: T().yourLocation + " (" + lat.toFixed(2) + ", " + lon.toFixed(2) + ")", lat: lat, lon: lon, tz: S.localTz };
  }
  function cityPlace(opt) {
    return { kind: "city", city: opt.value, name: opt.textContent, lat: parseFloat(opt.getAttribute("data-lat")), lon: parseFloat(opt.getAttribute("data-lon")), tz: TR_TZ };
  }
  /* Kayıtlı konum (yoksa null) */
  function saved() {
    var v = read();
    if (!v || !isFinite(v.lat) || !isFinite(v.lon)) return null;
    return v.kind === "geo" ? geoPlace(v.lat, v.lon) : { kind: "city", city: v.city, name: v.name || "", lat: v.lat, lon: v.lon, tz: TR_TZ };
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
        write(geoPlace(pos.coords.latitude, pos.coords.longitude)); S.track("konum-secimi", { tur: "konum" });
        emit(); close();
      }, function () { if (st) st.textContent = T().locationDenied; }, { enableHighAccuracy: false, timeout: 12000, maximumAge: 600000 });
    });
  }
  doc.addEventListener("click", function (e) {
    var b = e.target.closest && e.target.closest("[data-place-open]");
    if (b) { e.preventDefault(); open(); }
  });

  window.sthPlace = { use: use, attach: function (root, cb) { use(cb); }, saved: saved, current: current, open: open };
  label(current());
  /* Konuma bağlı sayfada henüz seçim yoksa bir kez sor */
  if (!saved() && doc.body.hasAttribute("data-needs-place")) open();
})();
