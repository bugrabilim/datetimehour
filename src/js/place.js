/* Ortak konum seçici: il listesi ya da "konumumu kullan". Namaz vakitleri, doğa ve hava durumu sayfaları kullanır.
   Konum cihazda kalır; son seçim yalnız bu tarayıcıda (localStorage: sth-place). */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var TR_TZ = "Europe/Istanbul";
  var T = function () { return S.T.pl || {}; };

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
  /* Seçici olmayan sayfalar (tam ekran hava durumu) için kayıtlı konum */
  function saved() {
    var v = read();
    if (!v || !isFinite(v.lat) || !isFinite(v.lon)) return null;
    return v.kind === "geo" ? geoPlace(v.lat, v.lon) : { kind: "city", city: v.city, name: v.name || "", lat: v.lat, lon: v.lon, tz: TR_TZ };
  }

  /* root: [data-place-city] select, [data-place-geo] düğme, [data-place-status] ileti içeren kutu */
  function attach(root, cb) {
    var sel = root.querySelector("[data-place-city]"), geo = root.querySelector("[data-place-geo]"), st = root.querySelector("[data-place-status]");
    var sv = read();
    var initial = null;
    if (sv && sv.kind === "geo" && isFinite(sv.lat) && isFinite(sv.lon)) initial = geoPlace(sv.lat, sv.lon);
    else {
      if (sv && sv.kind === "city") for (var i = 0; i < sel.options.length; i++) if (sel.options[i].value === sv.city) sel.selectedIndex = i;
      initial = cityPlace(sel.options[sel.selectedIndex]);
    }
    cb(initial);
    sel.addEventListener("change", function () {
      var p = cityPlace(sel.options[sel.selectedIndex]);
      write(p); if (st) st.textContent = "";
      S.track("konum-secimi", { tur: "il" });
      cb(p);
    });
    if (geo) geo.addEventListener("click", function () {
      if (!navigator.geolocation) { if (st) st.textContent = T().locationUnsupported; return; }
      if (st) st.textContent = T().locating;
      navigator.geolocation.getCurrentPosition(function (pos) {
        if (st) st.textContent = "";
        var p = geoPlace(pos.coords.latitude, pos.coords.longitude);
        write(p); S.track("konum-secimi", { tur: "konum" });
        cb(p);
      }, function () { if (st) st.textContent = T().locationDenied; }, { enableHighAccuracy: false, timeout: 12000, maximumAge: 600000 });
    });
  }
  window.sthPlace = { attach: attach, saved: saved };
})();
