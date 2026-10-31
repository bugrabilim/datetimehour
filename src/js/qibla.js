/* Kıble sayfası (30 Ekim 2026, rakip analizi #8): seçili yere göre kıble açısı, Kâbe'ye uzaklık ve bugünün kıble saati
   (prayer-calc.js qibla/qiblaTime; uygulamalarla aynı hesap), telefonda pusula (DeviceOrientation; iPhone'da izin
   düğmeyle istenir, Android'de deviceorientationabsolute), yakındaki camiler (OpenStreetMap Overpass, yalnız dış
   hizmetler izniyle; koordinat iki ondalığa yuvarlanır). Yön sensörü yalnız tarayıcıda okunur, hiçbir yere gitmez. */
(function () {
  "use strict";
  var S = window.sth, P = window.sthPrayer;
  if (!S || !P || !window.sthPlace) return;
  var doc = document;
  var root = doc.querySelector("[data-qibla]");
  if (!root) return;
  var X = S.T.qb || {};
  var q = function (sel) { return doc.querySelector(sel); };
  var rose = q("[data-qb-rose]"), kaaba = q("[data-qb-kaaba]"), needle = q("[data-qb-needle]");
  var cards = Array.prototype.slice.call(doc.querySelectorAll(".qb-card"));
  var statusEl = q("[data-qb-status]"), startBtn = q("[data-qb-start]"), calib = q("[data-qb-calib]");
  var place = null, bearing = 0, heading = null, aligned = false, listening = false, gotAbsolute = false;
  var nf = function (frac) {
    var loc = /^(ar|fa|ur)$/.test(S.lang) ? "en-US" : S.locale;
    return new Intl.NumberFormat(loc, { minimumFractionDigits: frac, maximumFractionDigits: frac });
  };
  var deg = function (x) { return nf(1).format(x) + "°"; };
  function dist(km) {
    if (km < 1) return S.tpl(X.m, { m: nf(0).format(Math.round(km * 1000)) });
    return S.tpl(X.km, { km: km < 10 ? nf(1).format(km) : nf(0).format(Math.round(km)) });
  }
  var diff = function (a, b) { return ((a - b + 540) % 360) - 180; };

  /* Kadran telefonun yönüyle döner (kuzey kuzeyi gösterir); iğne telefonun üstüne göre kıbleyi gösterir */
  function draw() {
    var h = heading == null ? 0 : heading;
    rose.setAttribute("transform", "rotate(" + (-h).toFixed(1) + ")");
    needle.setAttribute("transform", "rotate(" + (bearing - h).toFixed(1) + ")");
    // Yön harfleri kadranla döner ama dik kalır
    cards.forEach(function (c, i) { c.setAttribute("transform", "rotate(" + i * 90 + ") translate(0 -104) rotate(" + (h - i * 90).toFixed(1) + ")"); });
    var now = heading != null && Math.abs(diff(bearing, heading)) < 3;
    if (now !== aligned) {
      aligned = now;
      root.classList.toggle("is-aligned", aligned);
      if (aligned && navigator.vibrate) { try { navigator.vibrate(60); } catch (e) {} }
    }
    if (heading != null) statusEl.textContent = aligned ? X.aligned : X.turn;
  }

  function hm(ms, tz) { var p = S.parts(new Date(ms), tz); return String(p.hour).padStart(2, "0") + ":" + String(p.minute).padStart(2, "0"); }

  function render(pl) {
    place = pl;
    q("[data-qb-place]").textContent = pl.name || " ";
    var r = P.qibla(pl.lat, pl.lon);
    bearing = r.bearing;
    var atKaaba = r.km < 1;
    root.classList.toggle("is-kaaba", atKaaba);
    q("[data-qb-angle]").textContent = atKaaba ? "–" : deg(r.bearing);
    q("[data-qb-dist]").textContent = dist(r.km);
    kaaba.setAttribute("transform", "rotate(" + r.bearing.toFixed(2) + ")");
    // Kıble saati: yerin bugünkü tarihi ve UTC farkıyla
    var tz = pl.tz, now = S.nowMs(), p = S.parts(new Date(now), tz);
    var off = S.offsetMin(new Date(Date.UTC(p.year, p.month - 1, p.day, 12)), tz) / 60;
    var t = atKaaba ? NaN : P.qiblaTime(p.year, p.month, p.day, pl.lat, pl.lon, off);
    var tEl = q("[data-qb-time]"), tNote = q("[data-qb-time-note]");
    if (isFinite(t)) {
      // namaz vakitleri gibi dakikaya yuvarlanır (Diyanet'in kıble saatiyle aynı)
      tEl.textContent = hm(Date.UTC(p.year, p.month - 1, p.day) + (Math.round(t * 60) - off * 60) * 60000, tz);
      tNote.textContent = X.timeNote;
    } else {
      tEl.textContent = "–";
      tNote.textContent = atKaaba ? X.atKaaba : X.noTime;
    }
    if (atKaaba) statusEl.textContent = X.atKaaba;
    var lat = pl.lat.toFixed(4), lon = pl.lon.toFixed(4), qq = encodeURIComponent(X.mapsQuery);
    q("[data-qb-google]").href = "https://www.google.com/maps/search/" + qq + "/@" + lat + "," + lon + ",15z";
    q("[data-qb-apple]").href = "https://maps.apple.com/?q=" + qq + "&sll=" + lat + "," + lon + "&z=15";
    q("[data-qb-osm]").href = "https://www.openstreetmap.org/search?query=" + qq + "#map=15/" + lat + "/" + lon;
    resetMosques();
    draw();
  }

  /* ---------- Pusula ---------- */
  function onOrient(e, absolute) {
    var h = null;
    if (typeof e.webkitCompassHeading === "number" && !isNaN(e.webkitCompassHeading)) {
      h = e.webkitCompassHeading; // iPhone: manyetik kuzeyden saat yönünde
      if (calib) calib.hidden = !(e.webkitCompassAccuracy < 0 || e.webkitCompassAccuracy > 20);
    } else if (e.alpha != null && (absolute || e.absolute)) {
      h = 360 - e.alpha; // alpha saat yönünün tersine; ekran yönü düzeltmesi
      var so = (screen.orientation && screen.orientation.angle) || window.orientation || 0;
      h = (h + so + 360) % 360;
    }
    if (h == null) return;
    if (absolute) gotAbsolute = true;
    heading = heading == null ? h : (heading + diff(h, heading) * 0.3 + 360) % 360; // yumuşatma
    root.classList.add("is-live");
    root.classList.remove("no-compass");
    startBtn.hidden = true;
    draw();
  }
  var absHandler = function (e) { onOrient(e, true); };
  var relHandler = function (e) { if (!gotAbsolute) onOrient(e, false); };
  function listen() {
    if (listening) return;
    listening = true;
    statusEl.textContent = X.waiting;
    if ("ondeviceorientationabsolute" in window) window.addEventListener("deviceorientationabsolute", absHandler);
    window.addEventListener("deviceorientation", relHandler);
    setTimeout(function () { if (heading == null) { statusEl.textContent = X.noCompass; root.classList.add("no-compass"); } }, 3000);
  }
  var DOE = window.DeviceOrientationEvent;
  var needsPermission = DOE && typeof DOE.requestPermission === "function";
  var touch = window.matchMedia && window.matchMedia("(pointer: coarse)").matches;
  if (!DOE) { statusEl.textContent = X.noCompass; root.classList.add("no-compass"); }
  else if (needsPermission) { startBtn.hidden = false; statusEl.textContent = " "; }
  else if (touch) listen();
  else { statusEl.textContent = X.noCompass; root.classList.add("no-compass"); }
  startBtn.addEventListener("click", function () {
    DOE.requestPermission().then(function (r) {
      if (r === "granted") { startBtn.hidden = true; listen(); } else statusEl.textContent = X.denied;
    }).catch(function () { statusEl.textContent = X.denied; });
  });

  /* ---------- Yakındaki camiler (Overpass, dış hizmetler izniyle) ---------- */
  var mqList = q("[data-qb-mosques]"), mqState = q("[data-qb-mq-state]"), mqBtn = q("[data-qb-mq-show]");
  var cache = {}, seq = 0, wanted = false;
  var allowed = function () { return !!(window.sthConsent && window.sthConsent.ext()); };
  function el(tag, cls, text) { var e = doc.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  function resetMosques() { mqList.textContent = ""; mqList.hidden = true; mqState.textContent = ""; mqBtn.hidden = false; wanted = false; }
  function meters(a, b, c, d) {
    var r = Math.PI / 180, x = Math.sin((c - a) * r / 2), y = Math.sin((d - b) * r / 2);
    var h = x * x + Math.cos(a * r) * Math.cos(c * r) * y * y;
    return 2 * 6371008.8 * Math.asin(Math.sqrt(h));
  }
  function showList(items) {
    mqList.textContent = "";
    if (!items.length) { mqState.textContent = X.mosquesNone; return; }
    mqState.textContent = "";
    items.forEach(function (m) {
      var li = el("li"), a = el("a");
      a.href = "https://www.openstreetmap.org/" + m.type + "/" + m.id;
      a.target = "_blank"; a.rel = "noopener";
      a.appendChild(el("span", "qb-mq-name", m.name));
      a.appendChild(el("span", "qb-mq-dist", dist(m.d / 1000)));
      li.appendChild(a); mqList.appendChild(li);
    });
    mqList.hidden = false;
  }
  function loadMosques() {
    if (!place) return;
    wanted = true;
    mqBtn.hidden = true;
    if (!allowed()) {
      mqState.textContent = X.blocked + " ";
      var b = el("button", "btn btn-sm", X.allow); b.type = "button"; b.setAttribute("data-consent-allow-ext", "");
      mqState.appendChild(b);
      return;
    }
    var lat = place.lat.toFixed(2), lon = place.lon.toFixed(2), key = lat + "," + lon, my = ++seq, pl = place;
    var done = function (raw) {
      if (my !== seq) return;
      var items = raw.map(function (m) { return { type: m.type, id: m.id, name: m.name, d: meters(pl.lat, pl.lon, m.lat, m.lon) }; })
        .sort(function (a, b) { return a.d - b.d; }).slice(0, 30);
      showList(items);
    };
    if (cache[key]) { done(cache[key]); return; }
    mqState.textContent = X.mosquesLoading;
    var query = '[out:json][timeout:20];nwr["amenity"="place_of_worship"]["religion"="muslim"](around:4000,' + lat + "," + lon + ");out center 60;";
    fetch("https://overpass-api.de/api/interpreter?data=" + encodeURIComponent(query), { referrerPolicy: "no-referrer", credentials: "omit" })
      .then(function (r) { if (!r.ok) throw new Error("HTTP " + r.status); return r.json(); })
      .then(function (j) {
        var raw = (j.elements || []).map(function (e) {
          var c = e.lat != null ? e : e.center; if (!c) return null;
          var t = e.tags || {};
          return { type: e.type, id: e.id, lat: c.lat, lon: c.lon, name: t["name:" + S.lang] || t.name || X.mosqueUnnamed };
        }).filter(Boolean);
        cache[key] = raw; done(raw);
      })
      .catch(function () { if (my === seq) { mqState.textContent = X.mosquesError; mqBtn.hidden = false; } });
  }
  mqBtn.addEventListener("click", loadMosques);
  window.addEventListener("sth-consent", function () { if (wanted && allowed()) loadMosques(); });

  window.sthPlace.use(render);
})();
