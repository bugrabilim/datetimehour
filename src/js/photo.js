/* Fotoğraf ışığı (9 Kasım 2026, sahibin onayı): seçili yerde ve günde mavi saat, altın saat, gün doğumu/batımı saati
   ve yönü, Ay (doğuş, batış, evre), 7 günlük ışık tablosu ve bugün + 6 gün için gün doğumu/batımı fotoğraf tahmini.
   Saatler tarayıcıda hesaplanır (prayer-calc.js sunAltTimes, lightHours, sunAltAz; nature-calc.js Ay). Bulut tahmini
   Open-Meteo'dan yalnız dış hizmet izniyle (weather-core.js clouds); koordinat iki ondalık. Tahmin kuralı
   weather-core.js photoSky (uygulamalarla birebir). Gün seçimi ?d=YYYY-MM-DD. */
(function () {
  "use strict";
  var S = window.sth, P = window.sthPrayer, N = window.sthNature, W = window.sthWeather;
  if (!S || !P || !N || !window.sthPlace) return;
  var doc = document;
  var root = doc.querySelector("[data-photo]");
  if (!root) return;
  var X = S.T.ph || {}, CAL = S.cfg.cal || {};
  var PHASES = ["new", "waxingCrescent", "firstQuarter", "waxingGibbous", "full", "waningGibbous", "lastQuarter", "waningCrescent"];
  var KEYS = ["blueAm", "goldenAm", "goldenPm", "bluePm"];
  var DASH = "—", HOUR = 3600000, NS = "http://www.w3.org/2000/svg";
  var place = null, picked = null, data = null, dataKey = "", lastToday = "";
  var q = function (sel) { return doc.querySelector(sel); };
  var pad = function (n) { return String(n).padStart(2, "0"); };
  var isoOf = function (p) { return p.y + "-" + pad(p.m) + "-" + pad(p.d); };
  function setText(sel, v) { var e = q(sel); if (e) e.textContent = v; }

  /* Gün: { y, m, d } (yerin takvimiyle); n gün ötesi Date.UTC ile taşmasız */
  function shift(p, n) { var t = new Date(Date.UTC(p.y, p.m - 1, p.d + n)); return { y: t.getUTCFullYear(), m: t.getUTCMonth() + 1, d: t.getUTCDate() }; }
  function today() { var r = S.parts(new Date(S.nowMs()), place.tz); return { y: r.year, m: r.month, d: r.day }; }
  function parseIso(s) {
    var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s || "");
    if (!m) return null;
    var p = { y: +m[1], m: +m[2], d: +m[3] }, c = shift(p, 0);
    return c.y === p.y && c.m === p.m && c.d === p.d && p.y >= 1900 && p.y <= 2100 ? p : null;
  }
  function offH(p) { return S.offsetMin(new Date(Date.UTC(p.y, p.m - 1, p.d, 12)), place.tz) / 60; }
  /* Yerin gece yarısı (UTC ms) */
  function midnight(p) {
    var guess = Date.UTC(p.y, p.m - 1, p.d);
    var t = guess - S.offsetMin(new Date(guess), place.tz) * 60000;
    return guess - S.offsetMin(new Date(t), place.tz) * 60000;
  }

  /* Saat: kullanıcının 24/12 seçimiyle, yerin saat diliminde; oluşmayan an "—" */
  function hm(ms) {
    if (!isFinite(ms)) return DASH;
    var h12 = !!(S.prefs && S.prefs.h12);
    return S.fmt("ph-hm" + (h12 ? 12 : 24), place.tz, { hour: "2-digit", minute: "2-digit", hourCycle: h12 ? "h12" : "h23" }).format(new Date(Math.round(ms / 60000) * 60000));
  }
  function range(r) { return !isFinite(r[0]) && !isFinite(r[1]) ? DASH : hm(r[0]) + "–" + hm(r[1]); }
  function dir(az) { if (!isFinite(az)) return DASH; var d = X.dirs || []; return Math.round(az) % 360 + "°" + (d.length ? " " + d[Math.round(az / 22.5) % 16] : ""); }
  function dayLabel(p) {
    var wd = (new Date(Date.UTC(p.y, p.m - 1, p.d)).getUTCDay() + 6) % 7;
    return ((CAL.weekdaysShort || [])[wd] || "") + " " + S.tpl(CAL.shortFmt || "{day} {month}", { day: p.d, month: (CAL.months || [])[p.m - 1] });
  }
  function longLabel(p) {
    var wd = (new Date(Date.UTC(p.y, p.m - 1, p.d)).getUTCDay() + 6) % 7;
    return S.tpl(CAL.fullFmt || CAL.dateFmt || "{day} {month} {year}", { weekday: (CAL.weekdays || [])[wd] || "", day: p.d, month: (CAL.months || [])[p.m - 1], year: p.y });
  }

  /* Bir günün ışığı: mutlak anlar (ms; oluşmuyorsa NaN), Güneş'in doğuş/batış azimutu, kutup durumu */
  function lightOf(p) {
    var tz = offH(p), lat = place.lat, lon = place.lon, base = Date.UTC(p.y, p.m - 1, p.d);
    var ms = function (h) { return isFinite(h) ? base + (h - tz) * HOUR : NaN; };
    var az = function (h) { return isFinite(h) ? P.sunAltAz(p.y, p.m, p.d, h - tz, lat, lon).az : NaN; };
    var lh = P.lightHours(p.y, p.m, p.d, lat, lon, tz), rs = P.sunAltTimes(p.y, p.m, p.d, lat, lon, tz, -0.833);
    var o = { rise: ms(rs.rise), set: ms(rs.set), riseAz: az(rs.rise), setAz: az(rs.set), polar: "", partial: false };
    KEYS.forEach(function (k) { o[k] = [ms(lh[k][0]), ms(lh[k][1])]; if (!isFinite(o[k][0]) || !isFinite(o[k][1])) o.partial = true; });
    if (!isFinite(o.rise) || !isFinite(o.set)) o.polar = P.sunAltAz(p.y, p.m, p.d, 12 - lon / 15, lat, lon).alt > 0 ? "day" : "night";
    return o;
  }

  /* Bulut tahmini: gün doğumuna/batımına en yakın saatlik değer (1 saatten uzaksa yok) */
  function rate(ms) {
    if (!data || !isFinite(ms)) return null;
    var t = data.t, best = -1, bd = Infinity;
    for (var i = 0; i < t.length; i++) { var dd = Math.abs(t[i] - ms); if (dd < bd) { bd = dd; best = i; } }
    if (best < 0 || bd > HOUR) return null;
    var L = data.l[best], M = data.m[best], H = data.h[best], R = data.p[best];
    if (L == null || M == null || H == null) return null;
    R = R == null ? 0 : R;
    return { q: W.photoSky(L, M, H, R), l: L, m: M, h: H, p: R };
  }
  function num1(v) { try { return new Intl.NumberFormat(S.locale, { maximumFractionDigits: 1, numberingSystem: "latn" }).format(v); } catch (e) { return String(Math.round(v * 10) / 10); } }
  function badge(r) {
    var s = doc.createElement("span");
    s.className = "ph-q ph-q-" + r.q;
    s.textContent = (X.q || {})[r.q] || r.q;
    return s;
  }
  function detail(r) {
    var d = doc.createElement("small");
    d.className = "ph-cl";
    d.textContent = S.tpl(X.clouds || "", { l: S.pct(Math.round(r.l)), m: S.pct(Math.round(r.m)), h: S.pct(Math.round(r.h)) }) +
      (r.p >= 0.1 ? " · " + S.tpl(X.rain || "", { p: num1(r.p) }) : "");
    return d;
  }

  function renderDay() {
    var p = picked || today(), o = lightOf(p), t = today();
    setText("[data-ph-place]", place.name || "");
    setText("[data-ph-day]", longLabel(p));
    var din = q("[data-ph-date]"); if (din) din.value = isoOf(p);
    var tb = q("[data-ph-today]"); if (tb) tb.disabled = isoOf(p) === isoOf(t);
    setText('[data-ph="blueAm"]', range(o.blueAm)); setText('[data-ph="goldenAm"]', range(o.goldenAm));
    setText('[data-ph="goldenPm"]', range(o.goldenPm)); setText('[data-ph="bluePm"]', range(o.bluePm));
    setText('[data-ph="rise"]', hm(o.rise)); setText('[data-ph="set"]', hm(o.set));
    setText('[data-ph="riseAz"]', dir(o.riseAz)); setText('[data-ph="setAz"]', dir(o.setAz));
    var pol = q("[data-ph-polar]");
    if (pol) { var msg = o.polar === "day" ? X.polarDay : o.polar === "night" ? X.polarNight : o.partial ? X.partial : ""; pol.textContent = msg || ""; pol.hidden = !msg; }
    [["riseQ", o.rise], ["setQ", o.set]].forEach(function (x) {
      var dd = q('[data-ph="' + x[0] + '"]'), r = rate(x[1]);
      if (!dd) return;
      dd.textContent = "";
      dd.parentNode.hidden = !r;
      if (r) { dd.appendChild(badge(r)); dd.appendChild(doc.createTextNode(" ")); dd.appendChild(detail(r)); }
    });
    renderMoon(p, t);
    renderWeek(p, t);
  }
  function renderMoon(p, t) {
    var a = midnight(p), b = midnight(shift(p, 1));
    var rs = N.moonRiseSet(a, place.lat, place.lon, b - a);
    setText('[data-ph="moonrise"]', rs.rise === null ? X.noMoonrise : hm(rs.rise));
    setText('[data-ph="moonset"]', rs.set === null ? X.noMoonset : hm(rs.set));
    var m = N.moon(isoOf(p) === isoOf(t) ? S.nowMs() : (a + b) / 2);
    setText('[data-ph="phase"]', ((X.phases || {})[PHASES[m.idx]] || "") + " · " + S.tpl(X.lit || "{p}", { p: S.pct(Math.round(m.illum * 100)) }));
    drawMoon(m.frac);
  }
  /* Küçük Ay çizimi: aydınlık yarım disk + terminatör elipsi (güney yarımkürede ayna) */
  function drawMoon(frac) {
    var el = q("[data-ph-moon]"); if (!el) return;
    el.textContent = "";
    var r = 36, c = 40, k = Math.cos(2 * Math.PI * frac), rx = Math.abs(k) * r;
    var mk = function (tag, attrs, parent) { var e = doc.createElementNS(NS, tag); for (var n in attrs) e.setAttribute(n, attrs[n]); parent.appendChild(e); return e; };
    mk("circle", { cx: c, cy: c, r: r, class: "moon-dark" }, el);
    var flip = (frac < 0.5 ? 1 : -1) * (place.lat < 0 ? -1 : 1);
    var g = mk("g", { transform: flip === 1 ? "" : "translate(80 0) scale(-1 1)" }, el);
    mk("path", { d: "M " + c + " " + (c - r) + " A " + r + " " + r + " 0 0 1 " + c + " " + (c + r) + " A " + rx.toFixed(2) + " " + r + " 0 0 " + (k > 0 ? 0 : 1) + " " + c + " " + (c - r) + " Z", class: "moon-lit" }, g);
    mk("circle", { cx: c, cy: c, r: r, class: "moon-ring" }, el);
  }
  function renderWeek(p, t) {
    var body = q("[data-ph-week]"); if (!body) return;
    body.textContent = "";
    for (var i = 0; i < 7; i++) {
      var dp = shift(p, i), o = lightOf(dp), tr = doc.createElement("tr");
      if (isoOf(dp) === isoOf(t)) tr.className = "is-today";
      var th = doc.createElement("th"); th.scope = "row"; th.textContent = dayLabel(dp); tr.appendChild(th);
      [o.goldenAm, o.goldenPm, o.bluePm].forEach(function (r) { var td = doc.createElement("td"); td.dir = "ltr"; td.textContent = range(r); tr.appendChild(td); });
      body.appendChild(tr);
    }
    setText("[data-ph-wk-place]", place.name || "");
  }

  /* Tahmin tablosu: her zaman bugün + 6 gün */
  function renderForecast() {
    var box = q("[data-ph-fc-box]"), body = q("[data-ph-fc]");
    if (!box || !body) return;
    body.textContent = "";
    var t = today();
    for (var i = 0; i < 7; i++) {
      var dp = shift(t, i), o = lightOf(dp), tr = doc.createElement("tr");
      if (i === 0) tr.className = "is-today";
      var th = doc.createElement("th"); th.scope = "row"; th.textContent = dayLabel(dp); tr.appendChild(th);
      [o.rise, o.set].forEach(function (ms) {
        var td = doc.createElement("td"), r = rate(ms);
        if (r) { td.appendChild(badge(r)); td.appendChild(doc.createElement("br")); td.appendChild(detail(r)); } else td.textContent = DASH;
        tr.appendChild(td);
      });
      body.appendChild(tr);
    }
    box.hidden = false;
  }
  function fcState(kind) {
    var st = q("[data-ph-fc-state]"), cs = q("[data-ph-consent]"), box = q("[data-ph-fc-box]");
    if (cs) cs.hidden = kind !== "consent";
    if (box && kind !== "ok") box.hidden = true;
    if (st) { st.textContent = kind === "loading" ? X.loading || "" : kind === "error" ? X.error || "" : ""; st.hidden = !st.textContent; }
  }
  function loadClouds() {
    if (!W || !place) return;
    if (!W.allowed()) { data = null; dataKey = ""; fcState("consent"); renderDay(); return; }
    var key = place.lat.toFixed(2) + "," + place.lon.toFixed(2);
    if (key !== dataKey) { data = null; fcState("loading"); }
    W.clouds(place.lat, place.lon).then(function (d) {
      var h = d && d.hourly;
      if (!h || !h.time || !h.cloud_cover_low) throw new Error("data");
      data = { t: h.time.map(function (s) { return s * 1000; }), l: h.cloud_cover_low, m: h.cloud_cover_mid, h: h.cloud_cover_high, p: h.precipitation || [] };
      dataKey = key;
      fcState("ok"); renderForecast(); renderDay();
    }).catch(function (e) {
      if (e && e.consent) { fcState("consent"); return; }
      if (!data) fcState("error");
    });
  }

  function go(p, push) {
    var t = today();
    picked = p && isoOf(p) !== isoOf(t) ? p : null;
    if (push) { try { history.replaceState(null, "", picked ? "?d=" + isoOf(picked) : location.pathname); } catch (e) {} }
    renderDay();
  }
  root.addEventListener("click", function (e) {
    var b = e.target.closest && e.target.closest("[data-ph-step], [data-ph-today]");
    if (!b || !place) return;
    if (b.hasAttribute("data-ph-today")) go(null, true);
    else go(shift(picked || today(), +b.getAttribute("data-ph-step")), true);
  });
  var din = q("[data-ph-date]");
  if (din) din.addEventListener("change", function () { var p = parseIso(din.value); if (p && place) go(p, true); });

  window.addEventListener("sth-consent", loadClouds);
  window.sthPlace.use(function (p) {
    if (!p) return;
    place = p;
    if (!lastToday) { picked = parseIso((location.search.match(/[?&]d=([\d-]+)/) || [])[1]); if (picked && isoOf(picked) === isoOf(today())) picked = null; }
    lastToday = isoOf(today());
    renderDay(); loadClouds();
  });
  /* Gece yarısı geçince bugün ve tahmin tablosu yeni güne geçer */
  setInterval(function () {
    if (!place || doc.hidden) return;
    var t = isoOf(today());
    if (t !== lastToday) { lastToday = t; renderDay(); if (data) renderForecast(); loadClouds(); }
  }, 60000);
})();
