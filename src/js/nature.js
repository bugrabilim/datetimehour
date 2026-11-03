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
  /* Halk takvimi (2. analiz #60): bugünün düştüğü dönemi işaretle (yılı aşan erbain dahil) */
  function markFolk(nowMs) {
    var p = S.parts(new Date(nowMs), place.tz), md = pad(p.month) + "-" + pad(p.day);
    Array.prototype.forEach.call(doc.querySelectorAll("[data-folk-from]"), function (li) {
      var a = li.getAttribute("data-folk-from"), b = li.getAttribute("data-folk-to");
      var on = a <= b ? (md >= a && md <= b) : (md >= a || md <= b);
      li.classList.toggle("is-now", on);
      var badge = li.querySelector(".folk-now"); if (badge) badge.hidden = !on;
    });
  }
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
    var d = "M " + c + " " + (c - r) + " A " + r + " " + r + " 0 0 1 " + c + " " + (c + r) + " A " + rx.toFixed(2) + " " + r + " 0 0 " + (k > 0 ? 0 : 1) + " " + c + " " + (c - r) + " Z";
    svg("path", { d: d, class: "moon-lit" }, g);
    svg("circle", { cx: c, cy: c, r: r, class: "moon-ring" }, el);
    texImage(el, m.frac, 4, 72, 160);
    setText("[data-n-phase]", T.phases[PHASES[m.idx]]);
    setText("[data-n-illum]", S.tpl(T.illumination, { p: Math.round(m.illum * 100) }));
    setText("[data-n-age]", S.tpl(T.age, { d: new Intl.NumberFormat(S.locale, { maximumFractionDigits: 1, minimumFractionDigits: 1 }).format(m.age) }));
    var f = S.fmt("nm", place.tz, { day: "numeric", month: "long", hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
    setText("[data-n-nextnew]", f.format(new Date(m.nextNew)));
    setText("[data-n-nextfull]", f.format(new Date(m.nextFull)));
    // Ay doğuşu ve batışı: yerin bugünkü gece yarısından 24 saat
    var p = S.parts(new Date(ms), place.tz), start = localMidnight(p.year, p.month, p.day);
    var rs = N.moonRiseSet(start, place.lat, place.lon, localMidnight(p.year, p.month, p.day + 1) - start);
    var hm = S.fmt("mrs", place.tz, { hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
    var put = function (k, v, none) { var e = doc.querySelector('[data-n-moon="' + k + '"]'); if (e) e.textContent = v === null ? none : hm.format(new Date(roundMin(v))); };
    put("rise", rs.rise, T.moonNoRise); put("set", rs.set, T.moonNoSet);
  }
  /* Yerin gece yarısı (UTC ms); ay taşmasını Date.UTC düzeltir */
  function localMidnight(y, m, d) {
    var guess = Date.UTC(y, m - 1, d);
    var t = guess - S.offsetMin(new Date(guess), place.tz) * 60000;
    return guess - S.offsetMin(new Date(t), place.tz) * 60000;
  }
  function roundMin(ms) { return Math.round(ms / 60000) * 60000; }
  function moonIcon(frac, size, parent) {
    var r = size / 2 - 1, c = size / 2, k = Math.cos(2 * Math.PI * frac);
    var el = svg("svg", { viewBox: "0 0 " + size + " " + size, width: size, height: size, "aria-hidden": "true", focusable: "false", class: "moon-mini" }, parent);
    svg("circle", { cx: c, cy: c, r: r, class: "moon-dark" }, el);
    var rx = Math.abs(k) * r, flip = (frac < 0.5 ? 1 : -1) * (place.lat < 0 ? -1 : 1);
    var g = svg("g", { transform: flip === 1 ? "" : "translate(" + size + " 0) scale(-1 1)" }, el);
    svg("path", { d: "M " + c + " " + (c - r) + " A " + r + " " + r + " 0 0 1 " + c + " " + (c + r) + " A " + rx.toFixed(2) + " " + r + " 0 0 " + (k > 0 ? 0 : 1) + " " + c + " " + (c - r) + " Z", class: "moon-lit" }, g);
    svg("circle", { cx: c, cy: c, r: r, class: "moon-ring" }, el);
    texImage(el, frac, 1, size - 2, size * 2);
    return el;
  }
  /* Fotoğraf dokulu Ay (2. analiz #103): doku yüklüyse çizimin üstüne konur (moon-render.js) */
  function texImage(el, frac, at, w, px) {
    var R = window.sthMoonTex;
    if (!R || !R.ready()) return;
    var u = R.dataUrl(frac, place.lat < 0, px); if (!u) return;
    svg("image", { href: u, x: at, y: at, width: w, height: w, class: "moon-tex" }, el);
  }

  /* Ay takvimi: bu ayın her günü öğle saatinde evre; yeni ay ve dolunay (hesaplanan anın düştüğü gün) adıyla */
  function drawMoonCal(ms) {
    var box = doc.querySelector("[data-n-mooncal]"); if (!box) return;
    box.textContent = "";
    var p = S.parts(new Date(ms), place.tz), Y = p.year, M = p.month;
    var days = new Date(Date.UTC(Y, M, 0)).getUTCDate();
    var first = (new Date(Date.UTC(Y, M - 1, 1)).getUTCDay() + 6) % 7; // pazartesi 0
    var head = doc.createElement("div"); head.className = "mc-row mc-head";
    (CAL.weekdaysShort || []).forEach(function (w) { var s = doc.createElement("span"); s.textContent = w; head.appendChild(s); });
    box.appendChild(head);
    var grid = doc.createElement("div"); grid.className = "mc-grid";
    for (var i = 0; i < first; i++) grid.appendChild(doc.createElement("span"));
    // Bu ayın yeni ay ve dolunay anları → yerin gününe göre
    var marks = {}, k0 = Math.floor((localMidnight(Y, M, 1) / 86400000 + 2440587.5 - 2451550.09766) / 29.530588861) - 1;
    for (var k = k0; k <= k0 + 3; k++) {
      [[N.phaseMs(k, false), "new"], [N.phaseMs(k, true), "full"]].forEach(function (x) {
        var q = S.parts(new Date(x[0]), place.tz);
        if (q.year === Y && q.month === M) marks[q.day] = x[1];
      });
    }
    for (var d = 1; d <= days; d++) {
      var m = N.moon(localMidnight(Y, M, d) + 43200000), cell = doc.createElement("span");
      cell.className = "mc-day" + (d === p.day ? " is-today" : "");
      moonIcon(m.frac, 28, cell);
      var num = doc.createElement("b"); num.textContent = String(d); cell.appendChild(num);
      if (marks[d]) { var nm = doc.createElement("small"); nm.textContent = T.phases[marks[d]]; cell.appendChild(nm); cell.classList.add("mc-" + marks[d]); }
      cell.title = T.phases[PHASES[m.idx]] + " · " + S.tpl(T.illumination, { p: Math.round(m.illum * 100) });
      grid.appendChild(cell);
    }
    box.appendChild(grid);
    setText("[data-n-mooncalcap]", S.tpl(T.moonCalCaption, { month: (CAL.months || [])[M - 1] + " " + Y }));
  }

  /* Tutulmalar: NASA tablosundan sonraki 6; Ay tutulmasında o anda Ay yerin ufkunun üstünde mi */
  function drawEclipses(ms) {
    var box = doc.querySelector("[data-n-eclipses]"); if (!box) return;
    box.textContent = "";
    setText("[data-n-ecllead]", S.tpl(T.eclipseLead, { place: place.name }));
    var RN = T.regionNames || {}, names = null;
    try { names = new Intl.DisplayNames([S.locale], { type: "region" }); } catch (e) {}
    // Ad derlemede yazılır (bazı tarayıcılar "005" gibi sayısal bölge kodlarına ad vermiyor); yoksa tarayıcıya sorulur
    var list = function (codes) { return codes.map(function (c) { if (RN[c]) return RN[c]; try { var n = names ? names.of(c) : c; return n && n !== c ? n : c; } catch (e) { return c; } }).join(", "); };
    var f = S.fmt("ecl", place.tz, { day: "numeric", month: "long", year: "numeric", hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
    var hmF = S.fmt("ecl-hm", place.tz, { hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
    /* Ay tutulması: temas anları ve gölge çizimi (2. analiz #159); Ay'ın ufkun altında olduğu anlar soluk */
    function lunarDetail(li, e) {
      var C = N.lunarContacts(e.ut), NS = "http://www.w3.org/2000/svg";
      var t0 = C.p1 || C.max - 3 * 3600000, t1 = C.p4 || C.max + 3 * 3600000;
      C.path = C.path.filter(function (q) { return q.t >= t0 - 600000 && q.t <= t1 + 600000; });
      var ext = C.rP; C.path.forEach(function (q) { ext = Math.max(ext, Math.abs(q.x) + C.rM, Math.abs(q.y) + C.rM); });
      var sc = 150 / ext, svgE = doc.createElementNS(NS, "svg");
      svgE.setAttribute("viewBox", "-155 -155 310 310"); svgE.setAttribute("class", "ecl-diagram"); svgE.setAttribute("aria-hidden", "true");
      var mk = function (tag, a) { var x = doc.createElementNS(NS, tag); for (var k in a) x.setAttribute(k, a[k]); svgE.appendChild(x); return x; };
      mk("circle", { cx: 0, cy: 0, r: (C.rP * sc).toFixed(1), class: "ecl-pen" });
      mk("circle", { cx: 0, cy: 0, r: (C.rU * sc).toFixed(1), class: "ecl-umb" });
      var d = C.path.map(function (q, i) { return (i ? "L" : "M") + (-q.x * sc).toFixed(1) + " " + (-q.y * sc).toFixed(1); }).join("");
      mk("path", { d: d, class: "ecl-track" });
      ["p1", "u1", "max", "u4", "p4"].forEach(function (k) {
        if (!C[k]) return;
        var q = null; C.path.forEach(function (p) { if (!q || Math.abs(p.t - C[k]) < Math.abs(q.t - C[k])) q = p; });
        mk("circle", { cx: (-q.x * sc).toFixed(1), cy: (-q.y * sc).toFixed(1), r: (C.rM * sc).toFixed(1), class: k === "max" ? "ecl-moon ecl-moon-max" : "ecl-moon" });
      });
      li.appendChild(svgE);
      var dl = doc.createElement("dl"); dl.className = "ecl-times";
      ["p1", "u1", "u2", "max", "u3", "u4", "p4"].forEach(function (k) {
        if (!C[k]) return;
        var row = doc.createElement("div"); if (N.moonAltitude(C[k], place.lat, place.lon) < 0) row.className = "ecl-below";
        var dt = doc.createElement("dt"); dt.textContent = (T.eclContacts || {})[k] || k; row.appendChild(dt);
        var dd = doc.createElement("dd"); dd.textContent = hmF.format(new Date(roundMin(C[k]))); dd.dir = "ltr"; row.appendChild(dd);
        dl.appendChild(row);
      });
      li.appendChild(dl);
      if (C.umag > 0) { var mg = doc.createElement("p"); mg.className = "meta"; mg.textContent = S.tpl(T.eclMag, { v: new Intl.NumberFormat(/^(ar|fa|ur)$/.test(S.lang) ? "en" : S.locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(C.umag) }); li.appendChild(mg); }
    }
    (S.cfg.eclipses || []).filter(function (e) { return e.ut > ms - 6 * 3600000; }).slice(0, 6).forEach(function (e) {
      var li = doc.createElement("li"); li.className = "eclipse eclipse-" + e.kind;
      var key = e.kind + e.type.charAt(0).toUpperCase() + e.type.slice(1);
      var h = doc.createElement("h3"); h.textContent = (T.eclipseTypes || {})[key] || key; li.appendChild(h);
      var dt = doc.createElement("p"); dt.className = "turn-date"; dt.textContent = f.format(new Date(roundMin(e.ut))); li.appendChild(dt);
      var rg = doc.createElement("p"); rg.className = "meta"; rg.textContent = S.tpl(T.eclipseRegions, { list: list(e.regions) }); li.appendChild(rg);
      if (e.path.length) { var pt = doc.createElement("p"); pt.className = "meta"; pt.textContent = S.tpl(T.eclipsePath, { list: list(e.path) }); li.appendChild(pt); }
      if (e.kind === "lunar") {
        var up = N.moonAltitude(e.ut, place.lat, place.lon) > 0;
        var hp = doc.createElement("p"); hp.className = up ? "ecl-here" : "meta"; hp.textContent = S.tpl(up ? T.eclipseHere : T.eclipseNotHere, { place: place.name }); li.appendChild(hp);
        if (N.lunarContacts) lunarDetail(li, e);
      } else if (N.solarLocal) {
        var L = N.solarLocal(e.ut, place.lat, place.lon), sp = doc.createElement("p");
        if (!L) { sp.className = "meta"; sp.textContent = S.tpl(T.eclLocalNone, { place: place.name }); }
        else if (L.altMax < 0) { sp.className = "meta"; sp.textContent = S.tpl(T.eclLocalLow, { place: place.name }); }
        else {
          var hm2 = function (t) { return t ? hmF.format(new Date(roundMin(t))) : "–"; };
          sp.className = "ecl-here";
          sp.textContent = S.tpl(L.central === "total" ? T.eclLocalTotal : L.central === "annular" ? T.eclLocalAnnular : T.eclLocal, { place: place.name, obsc: S.pct(Math.round(L.obsc * 100)), c1: hm2(L.c1), c4: hm2(L.c4), max: hm2(L.max) });
        }
        li.appendChild(sp);
      }
      box.appendChild(li);
    });
  }

  /* Hilal görünürlüğü (Yallop): son yeni aydan 3 günden az geçtiyse o, değilse sıradaki yeni ay; üç akşam */
  function drawCrescent(ms) {
    var body = doc.querySelector("[data-n-crescent]"); if (!body || !N.crescent) return;
    var m = N.moon(ms), conj = m.age < 3 ? Math.round(ms - m.age * 86400000) : m.nextNew;
    var fd = S.fmt("cres-d", place.tz, { day: "numeric", month: "long", weekday: "short" });
    var ft = S.fmt("cres-t", place.tz, { hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
    var fl = S.fmt("cres-l", place.tz, { day: "numeric", month: "long", year: "numeric", hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
    setText("[data-n-creslead]", S.tpl(T.crescentLead, { place: place.name, date: fl.format(new Date(roundMin(conj))) }));
    body.textContent = "";
    N.crescent(conj, place.lat, place.lon, 3).forEach(function (e) {
      var tr = doc.createElement("tr");
      var cells = [fd.format(new Date(e.sunset)), ft.format(new Date(roundMin(e.sunset))),
        e.moonset == null ? "–" : ft.format(new Date(roundMin(e.moonset))) + (e.lag != null ? " (" + (e.lag > 0 ? S.tpl(T.crescentLag, { m: Math.round(e.lag) }) : T.crescentBefore) + ")" : ""),
        e.age > 0 ? S.tpl(T.crescentAge, { h: Math.round(e.age) }) : "–",
        (T.crescentCats || {})[e.cat] || e.cat];
      cells.forEach(function (v, i) { var td = doc.createElement(i ? "td" : "th"); if (!i) td.scope = "row"; td.textContent = v; if (i === 4) td.className = "cres-" + e.cat; tr.appendChild(td); });
      body.appendChild(tr);
    });
  }

  function drawSun(nowMs) {
    var p = S.parts(new Date(nowMs), place.tz);
    var t = sun(p.year, p.month, p.day);
    var prev = new Date(Date.UTC(p.year, p.month - 1, p.day - 1));
    var y = sun(prev.getUTCFullYear(), prev.getUTCMonth() + 1, prev.getUTCDate());
    var set = function (k, v) { var e = doc.querySelector('[data-n-sun="' + k + '"]'); if (e) e.textContent = v; };
    set("rise", hhmm(t.rise)); set("set", hhmm(t.set)); set("len", dur(t.len)); set("noon", hhmm(t.noon));
    // Altın saat (+6°…−4°) ve mavi saat (−4°…−6°)
    if (P.lightHours) {
      var lh = P.lightHours(p.year, p.month, p.day, place.lat, place.lon, offH(p.year, p.month, p.day));
      Object.keys(lh).forEach(function (k) {
        var e = doc.querySelector('[data-n-light="' + k + '"]');
        if (e) e.textContent = isFinite(lh[k][0]) && isFinite(lh[k][1]) ? hhmm(lh[k][0]) + "–" + hhmm(lh[k][1]) : "–";
      });
    }
    drawTwilight(p, t);
    var dm = Math.round((t.len - y.len) * 60);
    setText("[data-n-delta]", dm === 0 ? "" : S.tpl(dm > 0 ? T.dayLonger : T.dayShorter, { m: Math.abs(dm) }));
    var nowH = (p.hour * 60 + p.minute) / 60 + p.second / 3600;
    if (isFinite(t.rise) && isFinite(t.set) && nowH >= t.rise && nowH < t.set) setText("[data-n-left]", S.tpl(T.dayLeft, { t: dur(t.set - nowH) }));
    else setText("[data-n-left]", "");
  }

  /* Alacakaranlık (2. analiz #151): sivil −6°, denizcilik −12°, astronomik −18°; ilk ışık / son ışık; 24 saatlik ışık halkası */
  function drawTwilight(p, t) {
    var tz = offH(p.year, p.month, p.day);
    var at = function (a) { return P.sunAltTimes(p.year, p.month, p.day, place.lat, place.lon, tz, a); };
    var c6 = at(-6), c12 = at(-12), c18 = at(-18);
    var rng = function (a, b) { return isFinite(a) && isFinite(b) ? hhmm(a) + "–" + hhmm(b) : "–"; };
    var set = function (k, v) { var e = doc.querySelector('[data-n-tw="' + k + '"]'); if (e) e.textContent = v; };
    set("civil", rng(c6.rise, t.rise) + " · " + rng(t.set, c6.set));
    set("nautical", rng(c12.rise, c6.rise) + " · " + rng(c6.set, c12.set));
    set("astro", rng(c18.rise, c12.rise) + " · " + rng(c12.set, c18.set));
    set("first", hhmm(c18.rise)); set("last", hhmm(c18.set));
    drawRing(p, tz);
  }
  function drawRing(p, tz) {
    var el = doc.querySelector("[data-n-ring]"); if (!el) return;
    el.textContent = "";
    var band = function (alt) { return alt > -0.833 ? "day" : alt > -6 ? "civil" : alt > -12 ? "nautical" : alt > -18 ? "astro" : "night"; };
    var pt = function (h, r) { var a = h / 24 * 2 * Math.PI; return [100 - r * Math.sin(a), 100 + r * Math.cos(a)]; };
    var STEP = 0.1, R = 78, segs = [], cur = null;
    for (var h = 0; h < 24 + 1e-9; h += STEP) {
      var b = band(P.sunAltAz(p.year, p.month, p.day, h - tz, place.lat, place.lon).alt);
      if (!cur || cur.b !== b) { cur = { b: b, a: h, z: h }; segs.push(cur); } else cur.z = h;
    }
    segs.forEach(function (sg, i) {
      var a = sg.a, z = i < segs.length - 1 ? segs[i + 1].a : 24;
      if (z - a >= 23.99) { svg("circle", { cx: 100, cy: 100, r: R, "class": "lr lr-" + sg.b }, el); return; }
      var s0 = pt(a, R), s1 = pt(z, R), large = z - a > 12 ? 1 : 0;
      svg("path", { d: "M" + s0[0].toFixed(2) + " " + s0[1].toFixed(2) + " A" + R + " " + R + " 0 " + large + " 1 " + s1[0].toFixed(2) + " " + s1[1].toFixed(2), "class": "lr lr-" + sg.b }, el);
    });
    [0, 6, 12, 18].forEach(function (h) { var q = pt(h, 54); var tx = svg("text", { x: q[0].toFixed(1), y: (q[1] + 4).toFixed(1), "class": "lr-tick" }, el); tx.textContent = pad(h); });
    var nowH = p.hour + p.minute / 60, n0 = pt(nowH, R - 16), n1 = pt(nowH, R + 16);
    svg("line", { x1: n0[0].toFixed(1), y1: n0[1].toFixed(1), x2: n1[0].toFixed(1), y2: n1[1].toFixed(1), "class": "lr-now" }, el);
    var c = svg("text", { x: 100, y: 104, "class": "lr-center" }, el); c.textContent = hhmm(nowH);
  }

  /* Yılın dolunayları (2. analiz #148, #161): sayı, Mavi Ay (aynı takvim ayında ikinci dolunay), Kuzey Amerika adları, çark */
  function drawYearMoons(ms) {
    var tb = doc.querySelector("[data-n-ym]"); if (!tb || !T.ym) return;
    var Y = S.parts(new Date(ms), place.tz).year, YM = T.ym;
    var k0 = Math.floor((Date.UTC(Y, 0, 1) / 86400000 + 2440587.5 - 2451550.09766) / 29.530588861) - 2, list = [];
    for (var k = k0; k <= k0 + 16; k++) {
      var t = N.phaseMs(k, true), q = S.parts(new Date(t), place.tz);
      if (q.year === Y) list.push({ t: t, m: q.month, d: q.day });
    }
    var eq = N.seasons(Y).sept, hv = 0;
    list.forEach(function (x, i) { if (Math.abs(x.t - eq) < Math.abs(list[hv].t - eq)) hv = i; });
    var blue = null;
    list.forEach(function (x, i) {
      x.name = YM.names[x.m - 1];
      if (i > 0 && list[i - 1].m === x.m) { x.blue = true; blue = x; x.name = YM.blueName; }
    });
    if (list[hv]) { list[hv].name = YM.harvest; if (list[hv].m === 10 && list[hv + 1]) list[hv + 1].name = YM.names[9]; }
    var f = S.fmt("ymf", place.tz, { day: "numeric", month: "long", hour: "2-digit", minute: "2-digit", hourCycle: S.prefs && S.prefs.h12 ? "h12" : "h23" });
    var df = S.fmt("ymd", place.tz, { day: "numeric", month: "long" });
    setText("[data-n-ymh]", S.tpl(YM.heading, { y: Y }));
    setText("[data-n-ymcount]", S.tpl(YM.count, { y: Y, n: list.length }) + " " + (blue ? S.tpl(YM.blue, { date: df.format(new Date(blue.t)) }) : YM.noBlue));
    tb.textContent = "";
    var nextDone = false;
    list.forEach(function (x) {
      var tr = doc.createElement("tr"), a = doc.createElement("td"), b = doc.createElement("td");
      a.textContent = f.format(new Date(x.t)); b.textContent = (x.blue ? "🔵 " : "🌕 ") + x.name;
      if (x.t < ms) tr.className = "is-past"; else if (!nextDone) { tr.className = "is-next"; nextDone = true; }
      tr.appendChild(a); tr.appendChild(b); tb.appendChild(tr);
    });
    var w = doc.querySelector("[data-n-ymwheel]"); if (!w) return;
    w.textContent = "";
    var jan1 = Date.UTC(Y, 0, 1), L = (Date.UTC(Y + 1, 0, 1) - jan1);
    var ang = function (t) { return (t - jan1) / L * 2 * Math.PI; };
    var at2 = function (a, r) { return [110 + r * Math.sin(a), 110 - r * Math.cos(a)]; };
    svg("circle", { cx: 110, cy: 110, r: 86, "class": "ymw-ring" }, w);
    for (var mth = 0; mth < 12; mth++) {
      var a0 = ang(Date.UTC(Y, mth, 1)), p0 = at2(a0, 76), p1 = at2(a0, 96);
      svg("line", { x1: p0[0].toFixed(1), y1: p0[1].toFixed(1), x2: p1[0].toFixed(1), y2: p1[1].toFixed(1), "class": "ymw-tick" }, w);
      var mid = at2(ang(Date.UTC(Y, mth, 15)), 104), lab = svg("text", { x: mid[0].toFixed(1), y: (mid[1] + 3.5).toFixed(1), "class": "ymw-m" }, w);
      lab.textContent = String(mth + 1);
    }
    list.forEach(function (x) {
      var q = at2(ang(x.t), 86);
      svg("circle", { cx: q[0].toFixed(1), cy: q[1].toFixed(1), r: 6.5, "class": "ymw-moon" + (x.blue ? " is-blue" : "") + (x.t < ms ? " is-past" : "") }, w);
    });
    var nq0 = at2(ang(ms), 28), nq = at2(ang(ms), 68);
    svg("line", { x1: nq0[0].toFixed(1), y1: nq0[1].toFixed(1), x2: nq[0].toFixed(1), y2: nq[1].toFixed(1), "class": "ymw-now" }, w);
    var ct = svg("text", { x: 110, y: 116, "class": "ymw-c" }, w); ct.textContent = String(list.length);
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
    drawWheel(now); drawSun(now); drawMoon(now); drawChart(now); drawMoonCal(now); drawYearMoons(now); drawCrescent(now); drawEclipses(now); markFolk(now);
  }
  window.sthPlace.attach(root, function (p) { place = p; renderAll(); });
  if (window.sthMoonTex && S.cfg.moonTex) window.sthMoonTex.load(S.cfg.moonTex, function (ok) { if (ok && place) { var n = S.nowMs(); drawMoon(n); drawMoonCal(n); } });
  setInterval(function () { if (place && !doc.hidden) { var n = S.nowMs(); drawSun(n); drawMoon(n); } }, 60000);
})();
