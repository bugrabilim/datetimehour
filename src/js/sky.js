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
  var place = null, base = 0, night = null, con = null, picked = null, playT = 0, evShown = 12, clouds = null;
  var conBox = q("[data-sky-con]"), dateIn = q("[data-sky-date]"), playBtn = q("[data-sky-play]"), redBtn = q("[data-sky-red]");
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
    var heads = Array.prototype.map.call(body.parentNode.querySelectorAll("thead th"), function (h) { return h.textContent; });
    var now = S.nowMs();
    K.PLANETS.forEach(function (name) {
      var p = K.planet(name, now), v = visibility(name), tr = doc.createElement("tr");
      var when, dir = "–", hi = "–";
      if (v.first === null) when = p.elong < 15 ? X.tooClose : X.below;
      else { when = S.tpl(X.window, { from: hm(v.first), to: hm(v.last > v.first ? v.last : v.first + 10 * MIN) }); dir = dirName(v.firstAz); hi = S.tpl(X.altFmt, { deg: Math.round(v.best.alt), time: hm(v.best.t) }); }
      var rs = S.tpl(X.riseSet, { rise: v.rise ? hm(v.rise) : "–", set: v.set ? hm(v.set) : "–" });
      var cells = [(X.planets || {})[name] || name, when + " · " + rs, dir, hi, (p.mag < 0 ? "−" : "") + nf(1).format(Math.abs(p.mag)), S.tpl(X.mkm, { v: nf(0).format(Math.round(p.dist * 149.5978707)) })];
      cells.forEach(function (c, i) { var td = doc.createElement(i ? "td" : "th"); if (!i) td.scope = "row"; td.textContent = c; if (heads[i]) td.setAttribute("data-l", heads[i]); if (i && i !== 1) td.dir = "ltr"; tr.appendChild(td); });
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
    // Takımyıldızlar (2. analiz #156): çizgiler ufkun üstündeki parçalarda, adlar ağırlık noktasında
    if (con && conBox && conBox.checked && sa.alt < -3) {
      var g = el("g", { class: "sky-con" }, chart), lang = S.lang;
      Object.keys(con).forEach(function (id) {
        var c = con[id];
        c.lines.forEach(function (ln) {
          var d = "", pen = false;
          ln.forEach(function (pt) {
            var a = N.altAz(ms, pt[0], pt[1], place.lat, place.lon);
            if (a.alt < -2) { pen = false; return; }
            var p = proj(a); d += (pen ? "L" : "M") + p.x.toFixed(1) + " " + p.y.toFixed(1); pen = true;
          });
          if (d) el("path", { d: d, class: "sky-cline" }, g);
        });
        if (c.at && c.rank <= 2) {
          var a = N.altAz(ms, c.at[0], c.at[1], place.lat, place.lon);
          if (a.alt > 12) { var p = proj(a), tx = el("text", { x: p.x.toFixed(1), y: p.y.toFixed(1), class: "sky-cname", "text-anchor": "middle" }, g); tx.textContent = (c.n && (c.n[lang] || c.n.en)) || id; }
        }
      });
    }
    // Yıldızlar
    if (sa.alt < 0) K.STARS.forEach(function (st) {
      var a = N.altAz(ms, st[1], st[2], place.lat, place.lon); if (a.alt <= 0) return;
      var p = proj(a), r = Math.max(0.8, 3.2 - st[3] * 0.9);
      el("circle", { cx: p.x.toFixed(1), cy: p.y.toFixed(1), r: r.toFixed(1), class: "sky-star" }, chart);
      if (st[3] < 0.9) { var left = p.x > 110, tx = el("text", { x: (left ? p.x - r - 2 : p.x + r + 2).toFixed(1), y: (p.y + 3).toFixed(1), class: "sky-sname", "text-anchor": left ? "end" : "start" }, chart); tx.textContent = st[0]; }
    });
    // Ay ve Güneş
    var ma = moonAA(ms);
    if (ma.alt > -0.5) {
      var mp = proj(ma), m = N.moon(ms), mu = window.sthMoonTex && window.sthMoonTex.ready() ? window.sthMoonTex.dataUrl(m.frac, place.lat < 0, 32) : null;
      if (mu) el("image", { href: mu, x: (mp.x - 8).toFixed(1), y: (mp.y - 8).toFixed(1), width: 16, height: 16 }, chart);
      else el("circle", { cx: mp.x.toFixed(1), cy: mp.y.toFixed(1), r: 7, class: "sky-moon" }, chart);
      var mt = el("text", { x: (mp.x + 10).toFixed(1), y: (mp.y + 4).toFixed(1), class: "sky-pname" }, chart); mt.textContent = X.moon + " " + S.pct(Math.round(m.illum * 100));
    }
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
  /* Gece sınırları belirli bir Güneş yüksekliğine göre (−18°: tam karanlık); t0'dan sonraki ilk iniş ve çıkış */
  function darkness(t0, lim) {
    var below = function (t) { return sunAA(t).alt < lim; }, a = null, b = null, i, t;
    if (below(t0)) { for (i = 1; i <= 24 * 6; i++) { t = t0 - i * 10 * MIN; if (!below(t)) { a = t + 10 * MIN; break; } } if (a === null) return null; }
    else { for (i = 1; i <= 24 * 6; i++) { t = t0 + i * 10 * MIN; if (below(t)) { a = t; break; } } if (a === null) return null; }
    for (i = 1; i <= 24 * 6; i++) { t = a + i * 10 * MIN; if (!below(t)) { b = t; break; } }
    return { start: a, end: b === null ? a + 24 * HOUR : b };
  }
  /* "Bu gece" özeti (2. analiz #155): tam karanlık, Ay'ın karanlıktaki etkisi, görülebilen gezegenler, bulut (izinle) */
  function renderTonight() {
    var ref = night ? night.start + HOUR : base, dark = darkness(ref, -18);
    var tips = q("[data-sky-tips]"); tips.textContent = "";
    var addTip = function (txt) { var li = doc.createElement("li"); li.textContent = txt; tips.appendChild(li); };
    q("[data-sky-dark]").textContent = dark ? S.tpl(X.window2 || X.window, { from: hm(dark.start), to: hm(dark.end) }) : X.darkNone;
    var m = N.moon(dark ? dark.start : ref), illum = S.pct(Math.round(m.illum * 100)), up = [], t;
    if (dark) for (t = dark.start; t <= dark.end; t += 10 * MIN) if (moonAA(t).alt > 0) up.push(t);
    var moonImpact = dark ? (up.length * 10 * MIN) / Math.max(1, dark.end - dark.start) * m.illum : m.illum;
    q("[data-sky-tmoon]").textContent = up.length ? S.tpl(X.moonUpDark, { illum: illum, from: hm(up[0]), to: hm(up[up.length - 1]) }) : S.tpl(X.moonDownDark, { illum: illum });
    var vis = [];
    K.PLANETS.forEach(function (name) { var v = visibility(name); if (v.first !== null) vis.push({ name: name, v: v }); });
    q("[data-sky-tpl]").textContent = vis.length ? vis.map(function (x) { return (X.planets || {})[x.name] || x.name; }).join(", ") : X.planetsNone;
    var cloud = null;
    if (clouds && dark) {
      var sum = 0, n = 0;
      clouds.forEach(function (c) { if (c.t >= dark.start && c.t <= dark.end) { sum += c.v; n++; } });
      if (n) cloud = sum / n;
    }
    var cr = q("[data-sky-cloud-row]"); cr.hidden = cloud === null;
    if (cloud !== null) q("[data-sky-cloud]").textContent = S.pct(Math.round(cloud));
    var score = !dark || (cloud !== null && cloud > 70) || moonImpact > 0.6 ? "poor" : (moonImpact < 0.25 && (cloud === null || cloud < 30) && dark.end - dark.start >= 3 * HOUR ? "good" : "fair");
    var sc = q("[data-sky-score]"); sc.textContent = (X.score || {})[score] || ""; sc.className = "sky-score sky-score-" + score;
    if (m.illum > 0.5 && up.length) addTip(X.tipMoon); else if (dark && !up.length) addTip(X.tipDark);
    if (cloud !== null && cloud > 50) addTip(X.tipCloudy);
    vis.slice().sort(function (a, b) { return b.v.best.alt - a.v.best.alt; }).slice(0, 3).forEach(function (x) { addTip(S.tpl(X.tipBest, { p: (X.planets || {})[x.name] || x.name, time: hm(x.v.best.t), deg: Math.round(x.v.best.alt) })); });
    drawGraph(dark);
  }
  /* Bu gece gezegen yükseklikleri: gün batımından 1 saat önce ile gün doğumundan 1 saat sonrası arası */
  function drawGraph(dark) {
    var svgG = q("[data-sky-graph]"); if (!svgG) return;
    svgG.textContent = "";
    var t0 = (night ? night.start : base) - HOUR, t1 = (night ? night.end : base + 12 * HOUR) + HOUR, W = 640, H = 240, L = 34, R = 8, TOP = 10, B = 26;
    var x = function (t) { return L + (t - t0) / (t1 - t0) * (W - L - R); }, y = function (alt) { return TOP + (1 - Math.max(0, alt) / 90) * (H - TOP - B); };
    el("rect", { x: L, y: TOP, width: W - L - R, height: H - TOP - B, class: "skg-bg" }, svgG);
    if (dark) el("rect", { x: x(Math.max(t0, dark.start)).toFixed(1), y: TOP, width: Math.max(0, x(Math.min(t1, dark.end)) - x(Math.max(t0, dark.start))).toFixed(1), height: H - TOP - B, class: "skg-dark" }, svgG);
    [30, 60].forEach(function (a) { el("line", { x1: L, x2: W - R, y1: y(a), y2: y(a), class: "skg-grid" }, svgG); var tx = el("text", { x: L - 4, y: y(a) + 4, class: "skg-lab", "text-anchor": "end" }, svgG); tx.textContent = a + "°"; });
    for (var h = Math.ceil(t0 / HOUR) * HOUR; h <= t1; h += 2 * HOUR) { var tx = el("text", { x: x(h).toFixed(1), y: H - 8, class: "skg-lab", "text-anchor": "middle" }, svgG); tx.textContent = hm(h); }
    var series = K.PLANETS.map(function (n) { return { name: (X.planets || {})[n] || n, color: COLORS[n], f: function (t) { return planetAA(n, t).alt; } }; });
    series.push({ name: X.moon, color: "#c9d1db", f: function (t) { return moonAA(t).alt; }, dash: "4 3" });
    series.forEach(function (sr) {
      var d = "", pen = false, best = null;
      for (var t = t0; t <= t1; t += 10 * MIN) {
        var a = sr.f(t);
        if (a <= 0) { pen = false; continue; }
        d += (pen ? "L" : "M") + x(t).toFixed(1) + " " + y(a).toFixed(1); pen = true;
        if (!best || a > best.a) best = { a: a, t: t };
      }
      if (!d) return;
      var attrs = { d: d, class: "skg-line", stroke: sr.color }; if (sr.dash) attrs["stroke-dasharray"] = sr.dash;
      el("path", attrs, svgG);
      var lab = el("text", { x: Math.min(W - R - 2, x(best.t)).toFixed(1), y: (y(best.a) - 5).toFixed(1), class: "skg-name", "text-anchor": "middle", fill: sr.color }, svgG); lab.textContent = sr.name;
    });
  }
  /* Gökyüzü olayları (2. analiz #154): derlemede hesaplanan liste, seçili yerin saatiyle */
  function evTitle(e) {
    var P = X.planets || {}, pn = function (k) { return P[k] || k; };
    if (e.k === "shower") return S.tpl(X.evShower, { name: (X.showers || {})[e.id] || e.id });
    if (e.k === "opp") return S.tpl(X.evOpp, { p: pn(e.p) });
    if (e.k === "elong") return S.tpl(e.ev ? X.evElongE : X.evElongW, { p: pn(e.p), deg: nf(1).format(e.deg) });
    if (e.k === "conj") return S.tpl(X.evConj, { a: pn(e.a), b: pn(e.b), sep: nf(1).format(e.sep) });
    if (e.k === "moon") return S.tpl(X.evMoon, { p: pn(e.p), sep: nf(1).format(e.sep) });
    return "";
  }
  function renderEvents() {
    var box = q("[data-sky-events]"); if (!box) return;
    box.textContent = "";
    var now = S.nowMs(), list = (S.cfg.skyEvents || []).filter(function (e) { return e.t > now - 12 * HOUR; });
    list.slice(0, evShown).forEach(function (e) {
      var li = doc.createElement("li"); li.className = "sky-ev sky-ev-" + e.k;
      var d = doc.createElement("time"); d.dateTime = new Date(e.t).toISOString(); d.textContent = dayHm(e.t); li.appendChild(d);
      var b = doc.createElement("b"); b.textContent = evTitle(e); li.appendChild(b);
      if (e.k === "shower") { var sm = doc.createElement("small"); sm.textContent = (e.zhr ? S.tpl(X.showerRate, { zhr: e.zhr }) : X.showerVar) + " · " + S.tpl(X.moonLight, { moon: S.pct(e.illum) }); li.appendChild(sm); }
      box.appendChild(li);
    });
    var more = q("[data-sky-more]"); if (more) more.hidden = list.length <= evShown;
  }
  function loadClouds() {
    var W = window.sthWeather;
    if (!W || !W.allowed() || !place) { clouds = null; return; }
    W.load(place.lat, place.lon).then(function (d) {
      var h = d && d.hourly; if (!h || !h.cloud_cover) return;
      var off = (d.utc_offset_seconds || 0) * 1000;
      clouds = h.time.map(function (tm, i) { return { t: Date.parse(tm + ":00Z") - off, v: h.cloud_cover[i] }; });
      renderTonight();
    }).catch(function () {});
  }
  function renderAll() {
    var now = picked || S.nowMs();
    base = Math.floor(now / STEP) * STEP;
    night = findNight(now);
    q("[data-sky-place]").textContent = place.name;
    // Gündüzse çizim gecenin başından 1 saat sonrasını gösterir
    var dark = sunAA(now).alt < -6;
    slider.value = dark || !night ? 0 : Math.max(0, Math.min(96, Math.round((night.start + HOUR - base) / STEP)));
    drawChart(current()); renderTable(); renderMoon(); renderTonight(); renderEvents(); loadClouds();
  }
  function stopPlay() { if (playT) { clearInterval(playT); playT = 0; } if (playBtn) { playBtn.textContent = playBtn.getAttribute("data-play"); playBtn.setAttribute("aria-pressed", "false"); } }
  slider.addEventListener("input", function () { stopPlay(); if (place) drawChart(current()); });
  q("[data-sky-now]").addEventListener("click", function () { if (!place) return; stopPlay(); picked = null; if (dateIn) dateIn.value = ""; renderAll(); slider.value = 0; base = Math.floor(S.nowMs() / STEP) * STEP; drawChart(current()); });
  if (playBtn) playBtn.addEventListener("click", function () {
    if (playT) { stopPlay(); return; }
    if (+slider.value >= 96) slider.value = 0;
    playBtn.textContent = playBtn.getAttribute("data-pause"); playBtn.setAttribute("aria-pressed", "true");
    playT = setInterval(function () { if (+slider.value >= 96) { stopPlay(); return; } slider.value = +slider.value + 1; if (place) drawChart(current()); }, 160);
  });
  if (dateIn) dateIn.addEventListener("change", function () {
    if (!place) return; stopPlay();
    var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateIn.value);
    if (!m) { picked = null; renderAll(); return; }
    // Seçilen günün yerel öğlesi; çizim o akşamın gecesine gelir
    var guess = Date.UTC(+m[1], +m[2] - 1, +m[3], 12);
    picked = guess - S.offsetMin(new Date(guess), place.tz) * 60000;
    renderAll();
  });
  if (conBox) conBox.addEventListener("change", function () { if (place) drawChart(current()); });
  if (redBtn) redBtn.addEventListener("click", function () {
    var on = !doc.documentElement.classList.contains("red-mode");
    doc.documentElement.classList.toggle("red-mode", on); redBtn.setAttribute("aria-pressed", on ? "true" : "false");
  });
  var more = q("[data-sky-more]"); if (more) more.addEventListener("click", function () { evShown += 12; renderEvents(); });
  var conUrl = root.getAttribute("data-con");
  if (conUrl) fetch(conUrl, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (d) { con = d; if (place) drawChart(current()); }).catch(function () {});
  if (window.sthMoonTex && S.cfg.moonTex) window.sthMoonTex.load(S.cfg.moonTex, function (ok) { if (ok && place) drawChart(current()); });
  window.addEventListener("sth-consent", function () { loadClouds(); });
  window.sthPlace.attach(root, function (p) { place = p; renderAll(); });
  setInterval(function () { if (place && !doc.hidden && !picked && !playT && +slider.value === 0) { base = Math.floor(S.nowMs() / STEP) * STEP; drawChart(current()); } }, 60000);
})();
