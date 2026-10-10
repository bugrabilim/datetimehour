/* Doğa sayfası: mevsim çarkı, bugünün Güneşi, Ay'ın evresi ve yıl boyunca gün uzunluğu.
   Hesaplar tarayıcıda yapılır (nature-calc.js, prayer-calc.js); konum sunucuya gönderilmez. */
(function () {
  "use strict";
  var S = window.sth, P = window.sthPrayer, N = window.sthNature;
  if (!S || !P || !N || !window.sthPlace) return;
  var doc = document;
  var root = doc.querySelector("[data-nature]");
  if (!root) return;
  var T = S.T.nt || {}, CAL = S.cfg.cal || {};
  var NS = "http://www.w3.org/2000/svg";
  var ZERO = { fajr: 18, isha: 17, sunrise: 0.833, sunset: 0.833, asrFactor: 1, adj: { fajr: 0, sunrise: 0, dhuhr: 0, asr: 0, maghrib: 0, isha: 0 } };
  var PHASES = ["new", "waxingCrescent", "firstQuarter", "waxingGibbous", "full", "waningGibbous", "lastQuarter", "waningCrescent"];
  var place = null;

  function svg(tag, attrs, parent) {
    var e = doc.createElementNS(NS, tag);
    for (var k in attrs) e.setAttribute(k, attrs[k]);
    if (parent) parent.appendChild(e);
    return e;
  }
  function pad(n) { return String(n).padStart(2, "0"); }
  function hhmm(h) { if (!isFinite(h)) return "–"; var m = Math.round(h * 60); m = ((m % 1440) + 1440) % 1440; return pad(Math.floor(m / 60)) + ":" + pad(m % 60); }
  function dur(h) { var m = Math.round(h * 60); return Math.floor(m / 60) + " " + T.hoursShort + " " + pad(m % 60) + " " + T.minutesShort; }
  function setText(sel, txt) { var el = doc.querySelector(sel); if (el) el.textContent = txt; }
  function offH(y, m, d) { return S.offsetMin(new Date(Date.UTC(y, m - 1, d, 12)), place.tz) / 60; }
  function sun(y, m, d) {
    var r = P.times(y, m, d, place.lat, place.lon, offH(y, m, d), ZERO);
    var len = r.maghrib - r.sunrise;
    if (!isFinite(len)) { var summer = (m >= 4 && m <= 9); len = (place.lat > 0) === summer ? 24 : 0; }
    return { rise: r.sunrise, set: r.maghrib, noon: r.dhuhr, len: len };
  }
  function seasonName(i, south) { // 0..4 parça: kışı kapatan başlangıç, ilkbahar, yaz, sonbahar, kış
    var north = ["winter", "spring", "summer", "autumn", "winter"], sth = ["summer", "autumn", "winter", "spring", "summer"];
    return T.seasons[(south ? sth : north)[i]];
  }
  function shortDate(m, d) { return S.tpl(CAL.shortFmt || "{day} {month}", { day: d, month: (CAL.months || [])[m - 1] }); }

  var ICONS = { summer: "🏖️", autumn: "🍂", winter: "⛄", spring: "🌸" };
  function drawWheel(nowMs) {
    var el = doc.querySelector("[data-wheel]"); el.textContent = "";
    var p = S.parts(new Date(nowMs), place.tz), Y = p.year, south = place.lat < 0;
    var off = function (ms) { return S.offsetMin(new Date(ms), place.tz) * 60000; };
    var jan1 = Date.UTC(Y, 0, 1), L = Math.round((Date.UTC(Y + 1, 0, 1) - jan1) / 86400000);
    var idx = function (ms) { return (ms + off(ms) - jan1) / 86400000; };
    var sPrev = N.seasons(Y - 1), sCur = N.seasons(Y), sNext = N.seasons(Y + 1);
    var b = [idx(sPrev.dec), idx(sCur.march), idx(sCur.june), idx(sCur.sept), idx(sCur.dec), idx(sNext.march)]; // b[0]..b[5]
    var segs = [[0, b[1], 0], [b[1], b[2], 1], [b[2], b[3], 2], [b[3], b[4], 3], [b[4], L, 4]];
    var td = idx(nowMs), cur = 0;
    segs.forEach(function (s, i) { if (td >= s[0]) cur = i; });
    var startIdx = cur === 0 ? b[0] : segs[cur][0], endIdx = cur === 4 ? b[5] : segs[cur][1];
    var classes = ["winter", "spring", "summer", "autumn", "winter"], southClasses = ["summer", "autumn", "winter", "spring", "summer"];
    var names = south ? southClasses : classes;
    var cx = 180, cy = 180, R = 140;
    // yaz her zaman üstte, kış altta: yazın orta noktası saat 12 yönüne getirilir
    var summerMid = south ? (b[4] + b[5]) / 2 : (b[2] + b[3]) / 2;
    var deg0 = -summerMid / L * 360, TAU = 2 * Math.PI, rad0 = deg0 * Math.PI / 180;
    var ang = function (i) { return -(i / L * TAU + rad0); }; // yaz üstte; ilkbahar sağda, sonbahar solda (zaman saat yönünün tersine akar)
    var arcs = svg("g", { transform: "matrix(-1 0 0 1 360 0)" }, el); // yaylar aynalanır (yazılar aynalanmaz)
    segs.forEach(function (s) {
      var len = (s[1] - s[0]) / L * 360;
      svg("circle", { cx: cx, cy: cy, r: R, fill: "none", "stroke-width": 34, class: "ws ws-" + names[s[2]], pathLength: 360, "stroke-dasharray": len.toFixed(3) + " " + (360 - len).toFixed(3), "stroke-dashoffset": (-(s[0] / L * 360)).toFixed(3), transform: "rotate(" + (-90 + deg0).toFixed(3) + " 180 180)" }, arcs);
    });
    // ay işaretleri (1-12)
    for (var m = 1; m <= 12; m++) {
      var di = (Date.UTC(Y, m - 1, 1) - jan1) / 86400000, a = ang(di);
      var x1 = cx + 124 * Math.sin(a), y1 = cy - 124 * Math.cos(a), x2 = cx + 160 * Math.sin(a), y2 = cy - 160 * Math.cos(a);
      svg("line", { x1: x1.toFixed(1), y1: y1.toFixed(1), x2: x2.toFixed(1), y2: y2.toFixed(1), class: "wtick" }, el);
      var am = ang(di + (new Date(Date.UTC(Y, m, 0)).getUTCDate()) / 2);
      var t = svg("text", { x: (cx + 176 * Math.sin(am)).toFixed(1), y: (cy - 176 * Math.cos(am)).toFixed(1), class: "wmonth", "text-anchor": "middle", "dominant-baseline": "central" }, el);
      t.textContent = String(m);
    }
    // mevsim simgeleri (plaj, kardan adam...) ve adları
    segs.forEach(function (s) {
      var mid = ang((s[0] + s[1]) / 2);
      if (s[1] - s[0] < 20) return;
      var ic = svg("text", { x: (cx + R * Math.sin(mid)).toFixed(1), y: (cy - R * Math.cos(mid)).toFixed(1), class: "wicon", "text-anchor": "middle", "dominant-baseline": "central" }, el);
      ic.textContent = ICONS[names[s[2]]];
    });
    // dönüm noktaları (ekinoks, gündönümü) ve tarihleri
    [b[1], b[2], b[3], b[4]].forEach(function (i, k) {
      var a = ang(i), ms = [sCur.march, sCur.june, sCur.sept, sCur.dec][k];
      svg("circle", { cx: (cx + R * Math.sin(a)).toFixed(1), cy: (cy - R * Math.cos(a)).toFixed(1), r: 6, class: "wturn" }, el);
      var pp = S.parts(new Date(ms), place.tz);
      var t = svg("text", { x: (cx + 104 * Math.sin(a)).toFixed(1), y: (cy - 104 * Math.cos(a)).toFixed(1), class: "wturn-label", "text-anchor": "middle", "dominant-baseline": "central" }, el);
      t.textContent = pp.day + "." + pp.month;
    });
    // bugün
    var at = ang(td);
    svg("circle", { cx: (cx + R * Math.sin(at)).toFixed(1), cy: (cy - R * Math.cos(at)).toFixed(1), r: 13, class: "wtoday" }, el);
    // orta metin
    var nm = seasonName(cur, south);
    setText("[data-n-season]", S.tpl(T.current, { season: nm }));
    setText("[data-n-dayof]", S.tpl(T.dayOf, { n: Math.floor(td - startIdx) + 1, total: Math.round(endIdx - startIdx) }));
    var nextSeasonName = seasonName(cur === 4 ? 1 : cur + 1, south);
    setText("[data-n-next]", S.tpl(T.nextIn, { season: nextSeasonName, days: Math.max(0, Math.ceil(endIdx - td)) }));
    drawTurns(Y, south, sCur, td, idx);
  }

  /* Ekinoks ve gündönümü kartları: yarımküreye göre ad, en uzun gün/gece açıklaması, tarih ve kalan gün */
  function drawTurns(Y, south, sCur, td, idx) {
    var box = doc.querySelector("[data-n-turns]"); if (!box) return;
    box.textContent = "";
    var order = [["march", south ? "autumnEq" : "springEq", "eq"], ["june", south ? "winterSol" : "summerSol", south ? "longNight" : "longDay"], ["sept", south ? "springEq" : "autumnEq", "eq"], ["dec", south ? "summerSol" : "winterSol", south ? "longDay" : "longNight"]];
    var f = S.fmt("turn", place.tz, { day: "numeric", month: "long", hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
    order.forEach(function (o) {
      var ms = sCur[o[0]], d = Math.round(idx(ms) - td), li = doc.createElement("li");
      li.className = "turn turn-" + o[1];
      var h = doc.createElement("h3"); h.textContent = T.turnNames[o[1]]; li.appendChild(h);
      var dt = doc.createElement("p"); dt.className = "turn-date"; dt.textContent = f.format(new Date(ms)); li.appendChild(dt);
      var rel = doc.createElement("p"); rel.className = "meta"; rel.textContent = d === 0 ? T.turnToday : S.tpl(d > 0 ? T.turnIn : T.turnAgo, { n: Math.abs(d) }); li.appendChild(rel);
      var nt = doc.createElement("p"); nt.textContent = T.turnNotes[o[2]]; li.appendChild(nt);
      box.appendChild(li);
    });
  }

  function drawMoon(ms) {
    var m = N.moon(ms), k = Math.cos(2 * Math.PI * m.frac), r = 36, c = 40;
    var el = doc.querySelector("[data-moon]"); el.textContent = "";
    svg("circle", { cx: c, cy: c, r: r, class: "moon-dark" }, el);
    var rx = Math.abs(k) * r, waxing = m.frac < 0.5, flip = (waxing ? 1 : -1) * (place.lat < 0 ? -1 : 1);
    var g = svg("g", { transform: flip === 1 ? "" : "translate(80 0) scale(-1 1)" }, el);
    // sağ yarım disk + terminatör elips
    var d = "M " + c + " " + (c - r) + " A " + r + " " + r + " 0 0 1 " + c + " " + (c + r) + " A " + rx.toFixed(2) + " " + r + " 0 0 " + (k > 0 ? 1 : 0) + " " + c + " " + (c - r) + " Z";
    svg("path", { d: d, class: "moon-lit" }, g);
    setText("[data-n-phase]", T.phases[PHASES[m.idx]]);
    setText("[data-n-illum]", S.tpl(T.illumination, { p: Math.round(m.illum * 100) }));
    setText("[data-n-age]", S.tpl(T.age, { d: new Intl.NumberFormat(S.locale, { maximumFractionDigits: 1, minimumFractionDigits: 1 }).format(m.age) }));
    var f = S.fmt("nm", place.tz, { day: "numeric", month: "long", hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
    setText("[data-n-nextnew]", f.format(new Date(m.nextNew)));
    setText("[data-n-nextfull]", f.format(new Date(m.nextFull)));
  }

  function drawSun(nowMs) {
    var p = S.parts(new Date(nowMs), place.tz);
    var t = sun(p.year, p.month, p.day);
    var prev = new Date(Date.UTC(p.year, p.month - 1, p.day - 1));
    var y = sun(prev.getUTCFullYear(), prev.getUTCMonth() + 1, prev.getUTCDate());
    var set = function (k, v) { var e = doc.querySelector('[data-n-sun="' + k + '"]'); if (e) e.textContent = v; };
    set("rise", hhmm(t.rise)); set("set", hhmm(t.set)); set("len", dur(t.len)); set("noon", hhmm(t.noon));
    var dm = Math.round((t.len - y.len) * 60);
    setText("[data-n-delta]", dm === 0 ? "" : S.tpl(dm > 0 ? T.dayLonger : T.dayShorter, { m: Math.abs(dm) }));
    var nowH = (p.hour * 60 + p.minute) / 60 + p.second / 3600;
    if (isFinite(t.rise) && isFinite(t.set) && nowH >= t.rise && nowH < t.set) setText("[data-n-left]", S.tpl(T.dayLeft, { t: dur(t.set - nowH) }));
    else setText("[data-n-left]", "");
  }

  function drawChart(nowMs) {
    var el = doc.querySelector("[data-n-chart]"); el.textContent = "";
    var p = S.parts(new Date(nowMs), place.tz), Y = p.year;
    var days = Math.round((Date.UTC(Y + 1, 0, 1) - Date.UTC(Y, 0, 1)) / 86400000), data = [], lo = 24, hi = 0, iMax = 0, iMin = 0;
    for (var i = 0; i < days; i++) {
      var d = new Date(Date.UTC(Y, 0, 1 + i)), s = sun(d.getUTCFullYear(), d.getUTCMonth() + 1, d.getUTCDate());
      data.push(s.len);
      if (s.len > hi) { hi = s.len; iMax = i; }
      if (s.len < lo) { lo = s.len; iMin = i; }
    }
    var W = 640, H = 240, L = 40, Rr = 10, Tp = 14, B = 28;
    var yMin = Math.max(0, Math.floor(lo) - 1), yMax = Math.min(24, Math.ceil(hi) + 1);
    var X = function (i) { return L + (i / (days - 1)) * (W - L - Rr); };
    var Yy = function (h) { return Tp + (1 - (h - yMin) / (yMax - yMin)) * (H - Tp - B); };
    for (var h = Math.ceil(yMin / 2) * 2; h <= yMax; h += 2) {
      svg("line", { x1: L, x2: W - Rr, y1: Yy(h).toFixed(1), y2: Yy(h).toFixed(1), class: "cgrid" }, el);
      var tx = svg("text", { x: L - 6, y: Yy(h).toFixed(1), class: "clabel", "text-anchor": "end", "dominant-baseline": "central" }, el); tx.textContent = String(h);
    }
    for (var m = 1; m <= 12; m++) {
      var di = Math.round((Date.UTC(Y, m - 1, 1) - Date.UTC(Y, 0, 1)) / 86400000);
      var mt = svg("text", { x: X(di + 14).toFixed(1), y: H - 8, class: "clabel", "text-anchor": "middle" }, el); mt.textContent = String(m);
    }
    var pts = data.map(function (v, i) { return X(i).toFixed(1) + "," + Yy(v).toFixed(1); }).join(" ");
    svg("polyline", { points: pts, class: "cline", fill: "none" }, el);
    var ti = Math.round((Date.UTC(p.year, p.month - 1, p.day) - Date.UTC(Y, 0, 1)) / 86400000);
    svg("line", { x1: X(ti).toFixed(1), x2: X(ti).toFixed(1), y1: Tp, y2: H - B, class: "ctoday" }, el);
    svg("circle", { cx: X(ti).toFixed(1), cy: Yy(data[ti]).toFixed(1), r: 5, class: "cdot" }, el);
    var dm = function (i) { var d = new Date(Date.UTC(Y, 0, 1 + i)); return shortDate(d.getUTCMonth() + 1, d.getUTCDate()); };
    setText("[data-n-chartcap]", S.tpl(T.chartCaption, { place: place.name, year: Y }));
    setText("[data-n-extremes]", T.longest + ": " + dm(iMax) + ", " + dur(hi) + " · " + T.shortest + ": " + dm(iMin) + ", " + dur(lo));
  }

  function renderAll() {
    var now = S.nowMs();
    drawWheel(now); drawSun(now); drawMoon(now); drawChart(now);
  }
  window.sthPlace.attach(root, function (p) { place = p; renderAll(); });
  setInterval(function () { if (place && !doc.hidden) { var n = S.nowMs(); drawSun(n); drawMoon(n); } }, 60000);
})();
