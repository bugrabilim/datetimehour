/* Gece gökyüzü (30 Ekim 2026): seçili yerde bu gece görünen gezegenler, gökyüzü çizimi, Ay'ın uzaklığı.
   Hesap tarayıcıda (sky-calc.js gezegenler, nature-calc.js Güneş ve Ay); konum hiçbir yere gönderilmez. */
(function () {
  "use strict";
  var S = window.sth, N = window.sthNature, K = window.sthSky;
  if (!S || !N || !K || !window.sthPlace) return;
  var doc = document;
  var root = doc.querySelector("[data-sky]");
  if (!root) return;
  var X = S.T.sk || {};
  var q = function (sel) { return doc.querySelector(sel); };
  var chart = q("[data-sky-chart]"), slider = q("[data-sky-time]"), out = q("[data-sky-time-out]");
  var place = null, base = 0, night = null;
  var STEP = 15 * 60000, MIN = 60000, HOUR = 3600000;
  var COLORS = { mercury: "#b9a68d", venus: "#f4e3a1", mars: "#e8714c", jupiter: "#e9c48f", saturn: "#d9c27a" };
  var nf = function (frac) { return new Intl.NumberFormat(/^(ar|fa|ur)$/.test(S.lang) ? "en-US" : S.locale, { minimumFractionDigits: frac, maximumFractionDigits: frac }); };
  var NS = "http://www.w3.org/2000/svg";
  function el(tag, attrs, parent) { var e = doc.createElementNS(NS, tag); for (var k in attrs) e.setAttribute(k, attrs[k]); if (parent) parent.appendChild(e); return e; }
  var hm = function (ms) { return S.fmt("sky-hm", place.tz, { hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(Math.round(ms / MIN) * MIN)); };
  var dayHm = function (ms) { return S.fmt("sky-dhm", place.tz, { day: "numeric", month: "long", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(Math.round(ms / MIN) * MIN)); };
  var dirName = function (az) { var c = X.compass || []; return c.length ? c[Math.round(az / 45) % 8] : Math.round(az) + "°"; };

  function sunAA(ms) { var p = N.sunPos(ms); return N.altAz(ms, p.ra, p.dec, place.lat, place.lon); }
  function planetAA(name, ms) { var p = K.planet(name, ms); return N.altAz(ms, p.ra, p.dec, place.lat, place.lon); }
  function moonAA(ms) { var p = N.moonPos(ms), a = N.altAz(ms, p.ra, p.dec, place.lat, place.lon); a.alt -= p.par * Math.cos(a.alt * Math.PI / 180); return a; }

  /* Bu gecenin sınırları: Güneş −0,833°'nin altına indiği an (gece içindeysek önceki) ile çıktığı an; 10 dk örnekleme */
  function findNight(now) {
    var up = function (t) { return sunAA(t).alt > -0.833; }, t, i;
    var start = null, end = null;
    if (!up(now)) { for (i = 0; i < 24 * 6; i++) { t = now - i * 10 * MIN; if (up(t)) { start = t + 10 * MIN; break; } } if (start === null) start = now; }
    else { for (i = 1; i <= 24 * 6; i++) { t = now + i * 10 * MIN; if (!up(t)) { start = t; break; } } }
    if (start === null) return null; // kutup günü: karanlık yok
    for (i = 1; i <= 24 * 6; i++) { t = start + i * 10 * MIN; if (up(t)) { end = t; break; } }
    return { start: start, end: end === null ? start + 24 * HOUR : end };
  }

  /* Gezegenin bu geceki görünürlüğü: Güneş −6°'nin, gezegen +5°'nin üstündeyken */
  function visibility(name) {
    var first = null, last = null, best = { alt: -90, t: 0, az: 0 }, firstAz = 0;
    if (night) {
      for (var t = night.start; t <= night.end; t += 10 * MIN) {
        if (sunAA(t).alt > -6) continue;
        var a = planetAA(name, t);
        if (a.alt >= 5) { if (first === null) { first = t; firstAz = a.az; } last = t; }
        if (a.alt > best.alt) best = { alt: a.alt, t: t, az: a.az };
      }
    }
    // Doğuş ve batış: gecenin başından 8 saat önce başlayan 24 saatte ilk geçişler
    var rise = null, set = null, t0 = (night ? night.start : base) - 8 * HOUR, f0 = planetAA(name, t0).alt + 0.5667;
    for (var i = 1; i <= 24 * 6; i++) {
      var t1 = t0 + i * 10 * MIN, f1 = planetAA(name, t1).alt + 0.5667;
      if (f0 < 0 && f1 >= 0 && rise === null) rise = t1;
      if (f0 >= 0 && f1 < 0 && set === null) set = t1;
      f0 = f1;
    }
    return { first: first, last: last, best: best, firstAz: firstAz, rise: rise, set: set };
  }

  function renderTable() {
    var body = q("[data-sky-planets]"); body.textContent = "";
    var now = S.nowMs();
    K.PLANETS.forEach(function (name) {
      var p = K.planet(name, now), v = visibility(name), tr = doc.createElement("tr");
      var when, dir = "–", hi = "–";
      if (v.first === null) when = p.elong < 15 ? X.tooClose : X.below;
      else { when = S.tpl(X.window, { from: hm(v.first), to: hm(v.last) }); dir = dirName(v.firstAz); hi = S.tpl(X.altFmt, { deg: Math.round(v.best.alt), time: hm(v.best.t) }); }
      var rs = S.tpl(X.riseSet, { rise: v.rise ? hm(v.rise) : "–", set: v.set ? hm(v.set) : "–" });
      var cells = [(X.planets || {})[name] || name, when + " · " + rs, dir, hi, (p.mag < 0 ? "−" : "") + nf(1).format(Math.abs(p.mag)), S.tpl(X.mkm, { v: nf(0).format(Math.round(p.dist * 149.5978707)) })];
      cells.forEach(function (c, i) { var td = doc.createElement(i ? "td" : "th"); if (!i) td.scope = "row"; td.textContent = c; if (i && i !== 1) td.dir = "ltr"; tr.appendChild(td); });
      body.appendChild(tr);
    });
  }

  function proj(a) { var r = (90 - a.alt) / 90 * 180, z = a.az * Math.PI / 180; return { x: -r * Math.sin(z), y: -r * Math.cos(z) }; }
  function drawChart(ms) {
    chart.textContent = "";
    var sa = sunAA(ms);
    var bg = sa.alt > 0 ? "sky-day" : sa.alt > -6 ? "sky-civil" : sa.alt > -12 ? "sky-twilight" : "sky-night";
    chart.setAttribute("class", "sky-svg " + bg);
    el("circle", { cx: 0, cy: 0, r: 180, class: "sky-bg" }, chart);
    [30, 60].forEach(function (alt) { el("circle", { cx: 0, cy: 0, r: (90 - alt) / 90 * 180, class: "sky-ring" }, chart); });
    var cards = String(X.cardinals || "N E S W").split(/\s+/);
    [[0, -192, 0], [192, 0, 3], [0, 198, 2], [-192, 0, 1]].forEach(function (c) { var tx = el("text", { x: c[0], y: c[1], class: "sky-card", "text-anchor": "middle", "dominant-baseline": "central" }, chart); tx.textContent = cards[c[2]] || ""; });
    // Yıldızlar
    if (sa.alt < 0) K.STARS.forEach(function (st) {
      var a = N.altAz(ms, st[1], st[2], place.lat, place.lon); if (a.alt <= 0) return;
      var p = proj(a), r = Math.max(0.8, 3.2 - st[3] * 0.9);
      el("circle", { cx: p.x.toFixed(1), cy: p.y.toFixed(1), r: r.toFixed(1), class: "sky-star" }, chart);
      if (st[3] < 0.9) { var tx = el("text", { x: (p.x + r + 2).toFixed(1), y: (p.y + 3).toFixed(1), class: "sky-sname" }, chart); tx.textContent = st[0]; }
    });
    // Ay ve Güneş
    var ma = moonAA(ms);
    if (ma.alt > -0.5) { var mp = proj(ma), m = N.moon(ms); el("circle", { cx: mp.x.toFixed(1), cy: mp.y.toFixed(1), r: 7, class: "sky-moon" }, chart); var mt = el("text", { x: (mp.x + 10).toFixed(1), y: (mp.y + 4).toFixed(1), class: "sky-pname" }, chart); mt.textContent = X.moon + " " + Math.round(m.illum * 100) + "%"; }
    if (sa.alt > -0.833) { var sp = proj(sa); el("circle", { cx: sp.x.toFixed(1), cy: sp.y.toFixed(1), r: 8, class: "sky-sun" }, chart); var stx = el("text", { x: (sp.x + 11).toFixed(1), y: (sp.y + 4).toFixed(1), class: "sky-pname" }, chart); stx.textContent = X.sun; }
    // Gezegenler
    K.PLANETS.forEach(function (name) {
      var pl = K.planet(name, ms), a = N.altAz(ms, pl.ra, pl.dec, place.lat, place.lon); if (a.alt <= 0) return;
      var p = proj(a);
      el("circle", { cx: p.x.toFixed(1), cy: p.y.toFixed(1), r: Math.max(2.5, 5 - pl.mag * 0.6).toFixed(1), fill: COLORS[name], class: "sky-planet" }, chart);
      var tx = el("text", { x: (p.x + 7).toFixed(1), y: (p.y - 5).toFixed(1), class: "sky-pname" }, chart); tx.textContent = (X.planets || {})[name] || name;
    });
    q("[data-sky-cap]").textContent = S.tpl(X.chartCaption, { place: place.name, time: dayHm(ms) });
    out.textContent = hm(ms);
  }

  /* Ay: şu anki uzaklık, sıradaki perige/apoje (saatlik tarama + parabol), süper ay */
  function renderMoon() {
    var now = S.nowMs(), d = function (t) { return N.moonPos(t).dist; };
    q("[data-sky-mdist]").textContent = S.tpl(X.km, { v: nf(0).format(Math.round(d(now))) });
    var per = null, apo = null, prev = d(now - HOUR), cur = d(now);
    for (var i = 1; i <= 32 * 24 && (!per || !apo); i++) {
      var t = now + i * HOUR, nx = d(t);
      var refine = function () { var den = prev - 2 * cur + nx; var off = den ? 0.5 * (prev - nx) / den : 0; var tt = t - HOUR + off * HOUR; return { t: tt, km: d(tt) }; };
      if (!per && cur < prev && cur <= nx) per = refine();
      if (!apo && cur > prev && cur >= nx) apo = refine();
      prev = cur; cur = nx;
    }
    var show = function (sel, x) { q(sel).textContent = x ? dayHm(x.t) + " · " + S.tpl(X.km, { v: nf(0).format(Math.round(x.km)) }) : "–"; };
    show("[data-sky-perigee]", per); show("[data-sky-apogee]", apo);
    var sm = q("[data-sky-super]"), full = N.moon(now).nextFull;
    var sup = per && Math.abs(full - per.t) < 1.5 * 86400000 && d(full) < 362000;
    sm.hidden = !sup;
    if (sup) sm.textContent = S.tpl(X.superMoon, { date: S.fmt("sky-d", place.tz, { day: "numeric", month: "long", year: "numeric" }).format(new Date(full)) });
  }

  function current() { return base + (+slider.value) * STEP; }
  function renderAll() {
    var now = S.nowMs();
    base = Math.floor(now / STEP) * STEP;
    night = findNight(now);
    q("[data-sky-place]").textContent = place.name;
    // Gündüzse çizim gecenin başından 1 saat sonrasını gösterir
    var dark = sunAA(now).alt < -6;
    slider.value = dark || !night ? 0 : Math.max(0, Math.min(96, Math.round((night.start + HOUR - base) / STEP)));
    drawChart(current()); renderTable(); renderMoon();
  }
  slider.addEventListener("input", function () { if (place) drawChart(current()); });
  q("[data-sky-now]").addEventListener("click", function () { if (!place) return; base = Math.floor(S.nowMs() / STEP) * STEP; slider.value = 0; drawChart(current()); });
  window.sthPlace.attach(root, function (p) { place = p; renderAll(); });
  setInterval(function () { if (place && !doc.hidden && +slider.value === 0) { base = Math.floor(S.nowMs() / STEP) * STEP; drawChart(current()); } }, 60000);
})();
