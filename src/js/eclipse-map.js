/* Tutulma haritası (30 Ekim 2026): seçilen tutulmanın görüldüğü yerler, tarayıcıda hesaplanır (nature-calc.js Güneş ve
   Ay konumları). Güneş tutulması: her ızgara hücresinde tutulma boyunca (en büyük andan ±3,5 saat) yerel örtülmenin
   en büyük değeri; tam/halkalı yol, en büyük örtülmesi 0,9'u aşan hücrelerin ince ızgarayla taranmasıyla. Ay tutulması:
   en büyük anda Ay'ın ufkun üstünde olduğu yerler. Kara sınırları Natural Earth 1:110m (kamu malı). Yaklaşıktır. */
(function () {
  "use strict";
  var S = window.sth, N = window.sthNature;
  if (!S || !N) return;
  var doc = document;
  var box = doc.querySelector("[data-ecl-map]");
  if (!box) return;
  var T = S.T.nt || {};
  var sel = box.querySelector("[data-ecl-sel]"), cv = box.querySelector("[data-ecl-canvas]"), legend = box.querySelector("[data-ecl-legend]");
  var W = cv.width, H = cv.height, land = null, place = null, list = [];
  var D2R = Math.PI / 180, R = 6378.14, AU = 149597870.7, MIN = 60000;

  function gmst(ms) { var d = ms / 86400000 + 2440587.5 - 2451545, t = d / 36525; return 280.46061837 + 360.98564736629 * d + 0.000387933 * t * t; }
  function vec(ra, dec, r) { var a = ra * D2R, b = dec * D2R; return [r * Math.cos(b) * Math.cos(a), r * Math.cos(b) * Math.sin(a), r * Math.sin(b)]; }
  function geo(ms) { var m = N.moonPos(ms), s = N.sunPos(ms); return { m: vec(m.ra, m.dec, m.dist), s: vec(s.ra, s.dec, s.dist * AU), g: gmst(ms) }; }
  function lerp(a, b, f) { return { m: [a.m[0] + (b.m[0] - a.m[0]) * f, a.m[1] + (b.m[1] - a.m[1]) * f, a.m[2] + (b.m[2] - a.m[2]) * f], s: [a.s[0] + (b.s[0] - a.s[0]) * f, a.s[1] + (b.s[1] - a.s[1]) * f, a.s[2] + (b.s[2] - a.s[2]) * f], g: a.g + (b.g - a.g) * f }; }
  /* Yerdeki gözlemci için o anda örtülme (0 = yok, 1 = tam) ve tür: Güneş ufkun altındaysa -1 */
  function local(p, lat, lon) {
    var th = (p.g + lon) * D2R, cl = Math.cos(lat * D2R), o = [R * cl * Math.cos(th), R * cl * Math.sin(th), R * Math.sin(lat * D2R)];
    var sx = p.s[0] - o[0], sy = p.s[1] - o[1], sz = p.s[2] - o[2], mx = p.m[0] - o[0], my = p.m[1] - o[1], mz = p.m[2] - o[2];
    var ds = Math.sqrt(sx * sx + sy * sy + sz * sz), dm = Math.sqrt(mx * mx + my * my + mz * mz);
    if ((sx * o[0] + sy * o[1] + sz * o[2]) <= 0) return { mag: -1 };
    var c = (sx * mx + sy * my + sz * mz) / (ds * dm), d = Math.acos(Math.max(-1, Math.min(1, c)));
    var rs = Math.asin(696000 / ds), rm = Math.asin(1737.4 / dm);
    if (d >= rs + rm) return { mag: 0, d: d, rs: rs, rm: rm };
    return { mag: (rs + rm - d) / (2 * rs), d: d, rs: rs, rm: rm, central: d < Math.abs(rm - rs) ? (rm > rs ? "total" : "annular") : "" };
  }
  function solar(ut) {
    var steps = [], STEP = 5 * MIN, t0 = ut - 210 * MIN;
    for (var t = t0; t <= ut + 210 * MIN; t += STEP) steps.push(geo(t));
    var cell = function (lat, lon) {
      var best = 0, bi = -1;
      for (var i = 0; i < steps.length; i++) { var l = local(steps[i], lat, lon); if (l.mag > best) { best = l.mag; bi = i; } }
      if (bi < 0) return { mag: 0, central: "" };
      var central = "";
      for (var j = Math.max(0, bi - 1) * 10; j <= Math.min(steps.length - 1, bi + 1) * 10; j++) { // 30 sn ara adım
        var i0 = Math.floor(j / 10), f = (j % 10) / 10, a = steps[i0], b = steps[Math.min(steps.length - 1, i0 + 1)];
        var l2 = local(lerp(a, b, f), lat, lon);
        if (l2.mag > best) best = l2.mag;
        if (l2.central) central = l2.central;
      }
      return { mag: best, central: central };
    };
    var out = [], fine = [], G = 2;
    for (var lat = -89; lat < 90; lat += G) for (var lon = -179; lon < 180; lon += G) {
      var c = cell(lat, lon);
      if (c.mag > 0) out.push([lat, lon, G, c.mag]);
      if (c.mag > 0.9) for (var a = lat - G / 2 + 0.125; a < lat + G / 2; a += 0.25) for (var b = lon - G / 2 + 0.125; b < lon + G / 2; b += 0.25) { var f2 = cell(a, b); if (f2.central) fine.push([a, b, 0.25, f2.central]); }
    }
    return { cells: out, path: fine };
  }

  var X = function (lon) { return (lon + 180) / 360 * W; }, Y = function (lat) { return (90 - lat) / 180 * H; };
  function css(name, fb) { var v = getComputedStyle(cv).getPropertyValue(name).trim(); return v || fb; }
  function landPaths(c, fill) {
    if (!land) return;
    c.fillStyle = css("--ecl-land", "#f3efe4"); c.strokeStyle = css("--ecl-coast", "#9aa6b2"); c.lineWidth = 0.6;
    land.forEach(function (r) { c.beginPath(); for (var i = 0; i < r.length; i += 2) { var x = X(r[i]), y = Y(r[i + 1]); if (i) c.lineTo(x, y); else c.moveTo(x, y); } c.closePath(); if (fill) c.fill(); c.stroke(); });
  }
  function drawBase(c) { c.fillStyle = css("--ecl-sea", "#d9e6f2"); c.fillRect(0, 0, W, H); landPaths(c, true); }
  /* Hücreler ayrı katmana opak çizilir, sonra yarı saydam bindirilir: üst üste binen kenarlar koyu çizgi bırakmaz */
  function overlay(c, draw, alpha) {
    var o = doc.createElement("canvas"); o.width = W; o.height = H;
    draw(o.getContext("2d"));
    c.globalAlpha = alpha; c.drawImage(o, 0, 0); c.globalAlpha = 1;
    landPaths(c, false);
  }
  function drawPlace(c) {
    if (!place) return;
    var x = X(place.lon), y = Y(place.lat);
    c.fillStyle = "#111"; c.beginPath(); c.arc(x, y, 4, 0, 7); c.fill(); c.strokeStyle = "#fff"; c.lineWidth = 1.5; c.stroke();
  }
  function render() {
    var e = list[+sel.value]; if (!e) return;
    var c = cv.getContext("2d");
    drawBase(c);
    if (e.kind === "lunar") {
      var g = geo(e.ut), mp = N.moonPos(e.ut);
      overlay(c, function (o) {
        o.fillStyle = "#3c46a0";
        for (var lat = -89.5; lat < 90; lat += 1) for (var lon = -179.5; lon < 180; lon += 1) {
          if (N.altAz(e.ut, mp.ra, mp.dec, lat, lon).alt > 0) o.fillRect(Math.floor(X(lon - 0.5)), Math.floor(Y(lat + 0.5)), Math.ceil(W / 360) + 1, Math.ceil(H / 180) + 1);
        }
      }, 0.35);
      var sub = { lat: mp.dec, lon: ((mp.ra - g.g + 540) % 360) - 180 };
      c.fillStyle = "#3b4bb3"; c.beginPath(); c.arc(X(sub.lon), Y(sub.lat), 5, 0, 7); c.fill();
      legend.textContent = T.eclMapLunar || "";
    } else {
      var r = solar(e.ut);
      overlay(c, function (o) {
        r.cells.forEach(function (q) {
          var m = q[3];
          o.fillStyle = m > 0.75 ? "#c2410c" : m > 0.5 ? "#ea7a24" : m > 0.25 ? "#f4a65a" : "#f9cf9c";
          o.fillRect(Math.floor(X(q[1] - q[2] / 2)), Math.floor(Y(q[0] + q[2] / 2)), Math.ceil(W * q[2] / 360) + 1, Math.ceil(H * q[2] / 180) + 1);
        });
      }, 0.6);
      r.path.forEach(function (q) { c.fillStyle = q[3] === "total" ? "#8b0000" : "#6a2a8a"; c.fillRect(X(q[1] - 0.125), Y(q[0] + 0.125), W / 1440 + 1, H / 720 + 1); });
      legend.textContent = T.eclMapSolar || "";
    }
    drawPlace(c);
    cv.hidden = false;
  }
  function init() {
    var now = S.nowMs(), f = S.fmt("eclmap", "UTC", { day: "numeric", month: "long", year: "numeric" });
    list = (S.cfg.eclipses || []).filter(function (e) { return e.ut > now - 6 * 3600000; });
    sel.textContent = "";
    list.forEach(function (e, i) { var o = doc.createElement("option"); o.value = i; o.textContent = f.format(new Date(e.ut)) + " · " + ((T.eclipseTypes || {})[e.kind + e.type.charAt(0).toUpperCase() + e.type.slice(1)] || e.type); sel.appendChild(o); });
    fetch(box.getAttribute("data-land")).then(function (r) { return r.json(); }).then(function (j) { land = j.rings; render(); }).catch(function () { render(); });
  }
  sel.addEventListener("change", render);
  window.addEventListener("sth-place", function (ev) { if (ev.detail) { place = ev.detail; if (land) render(); } });
  if (window.sthPlace && window.sthPlace.current) place = window.sthPlace.current();
  var io = "IntersectionObserver" in window ? new IntersectionObserver(function (en) { if (en[0].isIntersecting) { io.disconnect(); init(); } }, { rootMargin: "200px" }) : null;
  if (io) io.observe(box); else init();
})();
