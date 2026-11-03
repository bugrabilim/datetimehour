/* Hava durumu sayfası: güncel durum, saatlik (24 saat) ve 7 günlük tahmin. */
(function () {
  "use strict";
  var S = window.sth, W = window.sthWeather;
  if (!S || !W || !window.sthPlace) return;
  var doc = document;
  var root = doc.querySelector("[data-weather]");
  if (!root) return;
  var X = S.T.wx || {}, CAL = S.cfg.cal || {};
  var q = function (sel) { return doc.querySelector(sel); };
  var stateEl = q("[data-wx-state]"), nowBox = q("[data-wx-now]"), retry = q("[data-wx-retry]");
  var current = null, seq = 0;
  function el(tag, cls, text) { var e = doc.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  function fact(dl, label, value, note) {
    var d = el("div", "fact"); d.appendChild(el("dt", null, label)); d.appendChild(el("dd", null, value));
    if (note) d.appendChild(el("dd", "fact-note", note));
    dl.appendChild(d);
  }
  var pad = function (n) { return String(n).padStart(2, "0"); };

  function render(place, d) {
    var c = d.current, i = W.info(c.weather_code, c.is_day);
    q("[data-wx-place]").textContent = place.name;
    var theme = /^(clear|mostlyClear)$/.test(i.key) ? "clear" : /^(partly|overcast)$/.test(i.key) ? "cloud" : i.key === "fog" ? "fog" : /snow/i.test(i.key) ? "snow" : /thunder/.test(i.key) ? "storm" : "rain";
    root.className = "wx-hero wx-" + theme + (c.is_day === 0 ? " is-night" : "");
    // Hava neyse ekran öyle (2. analiz #193): kartın zemininde seçili yerin havası canlanır
    var FX = window.sthWeatherFx, cv = q("[data-wx-fx]");
    if (FX && cv && cv.getContext) { root.classList.add("wx-live"); FX.set(cv, FX.fromCurrent(c), c.is_day !== 0); }
    q("[data-wx-icon]").textContent = i.icon;
    q("[data-wx-temp]").textContent = W.temp(c.temperature_2m);
    q("[data-wx-cond]").textContent = i.label;
    var dl = q("[data-wx-facts]"); dl.textContent = "";
    var Wd = W.words, sub = function (grp, k) { return k && X[grp] ? X[grp][k] || "" : ""; };
    fact(dl, X.feelsLike, W.temp(c.apparent_temperature), sub("fw", Wd.feels(c.temperature_2m, c.apparent_temperature)));
    fact(dl, X.humidity, S.pct(Math.round(c.relative_humidity_2m)), sub("hw", Wd.humidity(c.relative_humidity_2m)));
    fact(dl, X.wind, Math.round(c.wind_speed_10m) + " km/h · " + S.tpl(X.windFrom, { dir: W.compass(c.wind_direction_10m) }), sub("wd", Wd.wind(c.wind_speed_10m)));
    if (c.wind_gusts_10m != null) fact(dl, X.gusts, Math.round(c.wind_gusts_10m) + " km/h");
    fact(dl, X.precipitation, (Math.round(c.precipitation * 10) / 10) + " mm");
    fact(dl, X.pressure, Math.round(c.pressure_msl) + " hPa");
    fact(dl, X.uv, String(Math.round(c.uv_index * 10) / 10), sub("uvw", Wd.uv(c.uv_index)));
    if (c.visibility != null) fact(dl, X.visibility, (c.visibility >= 10000 ? Math.round(c.visibility / 1000) : Math.round(c.visibility / 100) / 10) + " km", sub("vw", Wd.visibility(c.visibility)));
    if (c.dew_point_2m != null) fact(dl, X.dewPoint, W.temp(c.dew_point_2m), sub("dw", Wd.dew(c.dew_point_2m)));
    nowBox.hidden = false;
    // saatlik: şu andan sonraki 24 saat
    var h = d.hourly, start = 0;
    for (var k = 0; k < h.time.length; k++) if (h.time[k] >= c.time.slice(0, 13)) { start = k; break; }
    var ul = q("[data-wx-hourly]"); ul.textContent = "";
    for (var n = start; n < Math.min(start + 24, h.time.length); n++) {
      var li = el("li", "wx-hour"), hi = W.info(h.weather_code[n], h.is_day[n]);
      li.appendChild(el("span", "wx-h-time", h.time[n].slice(11, 16)));
      var ic = el("span", "wx-h-icon", hi.icon); ic.setAttribute("aria-hidden", "true"); ic.title = hi.label; li.appendChild(ic);
      li.appendChild(el("span", "wx-h-temp", W.temp(h.temperature_2m[n])));
      var pp = h.precipitation_probability[n];
      li.appendChild(el("span", "wx-h-rain", pp != null && pp >= 10 ? S.pct(pp) : " "));
      ul.appendChild(li);
    }
    q("[data-wx-hourly-box]").hidden = false;
    chart(d, start);
    // Günlük özet cümlesi (2. analiz #108)
    var nx = [];
    for (var z = start; z < Math.min(start + 12, h.time.length); z++) nx.push({ hour: +h.time[z].slice(11, 13), code: h.weather_code[z], rain: h.precipitation_probability[z] || 0, wind: (h.wind_speed_10m || [])[z] || 0 });
    var dd0 = d.daily, sm = W.summary(nx, dd0.temperature_2m_max[0], dd0.temperature_2m_max.length > 1 ? dd0.temperature_2m_max[1] : null);
    var sumEl = q("[data-wx-sum]");
    if (sumEl && X.sum) sumEl.textContent = S.tpl(X.sum[sm.key] || "", { time: pad(sm.hour) + ":00", n: sm.n, cond: i.label, max: W.temp(dd0.temperature_2m_max[0]), min: W.temp(dd0.temperature_2m_min[0]) });
    // günlük
    var dd = d.daily, du = q("[data-wx-daily]"); du.textContent = "";
    var lo = Math.min.apply(null, dd.temperature_2m_min), hi = Math.max.apply(null, dd.temperature_2m_max), span = Math.max(1, hi - lo);
    for (var j = 0; j < dd.time.length; j++) {
      var row = el("li", "wx-day"), di = W.info(dd.weather_code[j], 1), p = dd.time[j].split("-").map(Number);
      var wd = (new Date(Date.UTC(p[0], p[1] - 1, p[2])).getUTCDay() + 6) % 7;
      row.appendChild(el("span", "wx-d-name", j === 0 ? S.T.today : (CAL.weekdaysShort || CAL.weekdays || [])[wd]));
      var di2 = el("span", "wx-d-icon", di.icon); di2.setAttribute("aria-hidden", "true"); row.appendChild(di2);
      row.appendChild(el("span", "wx-d-cond", di.label));
      var pr = dd.precipitation_probability_max[j];
      row.appendChild(el("span", "wx-d-rain", pr != null && pr >= 10 ? S.pct(pr) + " · " + (Math.round(dd.precipitation_sum[j] * 10) / 10) + " mm" : " "));
      var bar = el("span", "wx-d-bar"), fill = el("i");
      fill.style.setProperty("--lo", ((dd.temperature_2m_min[j] - lo) / span * 100).toFixed(1) + "%");
      fill.style.setProperty("--hi", ((dd.temperature_2m_max[j] - lo) / span * 100).toFixed(1) + "%");
      bar.appendChild(fill); bar.setAttribute("aria-hidden", "true"); row.appendChild(bar);
      row.appendChild(el("span", "wx-d-temp", W.temp(dd.temperature_2m_min[j]) + " / " + W.temp(dd.temperature_2m_max[j])));
      du.appendChild(row);
    }
    q("[data-wx-daily-box]").hidden = false;
    q("[data-wx-updated]").textContent = S.tpl(X.updated, { time: c.time.slice(11, 16) }) + " · " + X.sunrise + " " + dd.sunrise[0].slice(11, 16) + " · " + X.sunset + " " + dd.sunset[0].slice(11, 16);
    stateEl.textContent = " ";
    retry.hidden = true;
  }
  /* Saatlik eğri (2. analiz #107): çiplerle sıcaklık, hissedilen, yağış ihtimali, rüzgâr; dokununca/üzerine gelince ayrıntı */
  var metric = 0, chartData = null;
  var SVGNS = "http://www.w3.org/2000/svg";
  function svg(tag, attrs) { var e = doc.createElementNS(SVGNS, tag); for (var k in attrs) e.setAttribute(k, attrs[k]); return e; }
  function chart(d, start) {
    var h = d.hourly, n = Math.min(24, h.time.length - start);
    if (n < 2) return;
    chartData = { d: d, start: start, n: n, sel: 0 };
    var chips = q("[data-wx-chips]");
    if (chips && !chips.childNodes.length) {
      [X.chartTemp, X.feelsLike, X.precipitation, X.wind].forEach(function (label, i) {
        var b = el("button", "wx-chip", label); b.type = "button";
        b.setAttribute("aria-pressed", i === metric ? "true" : "false");
        b.addEventListener("click", function () {
          metric = i;
          Array.prototype.forEach.call(chips.children, function (c, j) { c.setAttribute("aria-pressed", j === i ? "true" : "false"); });
          drawChart();
        });
        chips.appendChild(b);
      });
    }
    drawChart();
  }
  function series() {
    var h = chartData.d.hourly, out = [];
    var key = ["temperature_2m", "apparent_temperature", "precipitation_probability", "wind_speed_10m"][metric];
    for (var i = 0; i < chartData.n; i++) { var v = (h[key] || [])[chartData.start + i]; out.push(v == null ? 0 : v); }
    return out;
  }
  function drawChart() {
    var box = q("[data-wx-chart]"); if (!box || !chartData) return;
    var vals = series(), n = vals.length, W0 = 600, H0 = 140, P = 6;
    var lo = metric === 2 ? 0 : Math.min.apply(null, vals) - 1, hi = metric === 2 ? 100 : Math.max.apply(null, vals) + 1;
    var x = function (i) { return P + (W0 - 2 * P) * i / (n - 1); }, y = function (v) { return P + (H0 - 2 * P) * (1 - (v - lo) / (hi - lo || 1)); };
    var s = svg("svg", { viewBox: "0 0 " + W0 + " " + H0, class: "wx-svg m" + metric, role: "img", "aria-label": q("#wx-hourly-h").textContent });
    if (metric === 2) {
      var bw = (W0 - 2 * P) / n * 0.7;
      vals.forEach(function (v, i) { s.appendChild(svg("rect", { x: (x(i) - bw / 2).toFixed(1), y: y(v).toFixed(1), width: bw.toFixed(1), height: (H0 - P - y(v)).toFixed(1), class: "wx-bar" })); });
    } else {
      var pts = vals.map(function (v, i) { return x(i).toFixed(1) + "," + y(v).toFixed(1); });
      s.appendChild(svg("polygon", { points: P + "," + (H0 - P) + " " + pts.join(" ") + " " + (W0 - P) + "," + (H0 - P), class: "wx-area" }));
      s.appendChild(svg("polyline", { points: pts.join(" "), class: "wx-line" }));
    }
    var sel = chartData.sel;
    s.appendChild(svg("line", { x1: x(sel), x2: x(sel), y1: 0, y2: H0, class: "wx-rule" }));
    s.appendChild(svg("circle", { cx: x(sel), cy: y(vals[sel]), r: 5, class: "wx-dot" }));
    function pick(ev) {
      var r = s.getBoundingClientRect(), px = ((ev.touches ? ev.touches[0].clientX : ev.clientX) - r.left) / r.width * W0;
      var i = Math.max(0, Math.min(n - 1, Math.round((px - P) / (W0 - 2 * P) * (n - 1))));
      if (i !== chartData.sel) { chartData.sel = i; drawChart(); }
    }
    s.addEventListener("click", pick); s.addEventListener("mousemove", pick);
    box.textContent = ""; box.appendChild(s);
    // eksen saatleri
    var ax = el("div", "wx-axis");
    for (var i = 0; i < n; i += 6) ax.appendChild(el("span", null, chartData.d.hourly.time[chartData.start + i].slice(11, 16)));
    box.appendChild(ax);
    // seçili saat
    var h = chartData.d.hourly, k = chartData.start + sel, info = W.info(h.weather_code[k], h.is_day[k]);
    var parts = [h.time[k].slice(11, 16) + " " + info.icon + " " + info.label + " · " + (h.is_day[k] ? X.dayPart : X.nightPart),
      W.temp(h.temperature_2m[k]) + (h.apparent_temperature ? " (" + X.feelsLike + " " + W.temp(h.apparent_temperature[k]) + ")" : ""),
      S.pct(h.precipitation_probability[k] || 0) + (h.precipitation && h.precipitation[k] > 0 ? " · " + (Math.round(h.precipitation[k] * 10) / 10) + " mm" : ""),
      X.wind + " " + Math.round(h.wind_speed_10m[k]) + " km/h" + (h.wind_direction_10m ? " · " + S.tpl(X.windFrom, { dir: W.compass(h.wind_direction_10m[k]) }) : "") +
        (h.relative_humidity_2m ? " · " + X.humidity + " " + S.pct(Math.round(h.relative_humidity_2m[k])) : "")];
    q("[data-wx-detail]").textContent = parts.join(" · ");
  }
  /* Hava kalitesi ve polen (2. analiz #111) */
  function airRender(place) {
    var box = q("[data-wx-aqi]"), sec = q("[data-wx-aqi-box]");
    if (!box || !W.air) return;
    W.air(place.lat, place.lon).then(function (a) {
      var c = a && a.current;
      if (!c || c.european_aqi == null) { sec.hidden = true; return; }
      var lv = W.aqiLevel(c.european_aqi);
      box.textContent = "";
      var head = el("div", "wx-aqi-head");
      var badge = el("span", "wx-aqi-badge l" + lv, String(Math.round(c.european_aqi)));
      head.appendChild(badge);
      var t = el("div"); t.appendChild(el("strong", null, (X.aqiLevels || [])[lv] || "")); t.appendChild(el("span", "meta", " " + (X.aqiIndex || "")));
      head.appendChild(t); box.appendChild(head);
      var dl = el("dl", "wx-facts");
      [["PM2.5", c.pm2_5], ["PM10", c.pm10], ["O₃", c.ozone], ["NO₂", c.nitrogen_dioxide]].forEach(function (p) {
        if (p[1] != null) fact(dl, p[0], Math.round(p[1]) + " µg/m³");
      });
      box.appendChild(dl);
      box.appendChild(el("h3", null, X.pollen || ""));
      var pl = el("ul", "wx-pollen"), any = false;
      ["alder", "birch", "grass", "mugwort", "olive", "ragweed"].forEach(function (k) {
        var g = c[k + "_pollen"]; if (g == null) return;
        var lvl = W.pollenLevel(g); if (!lvl) return;
        any = true;
        pl.appendChild(el("li", "p" + lvl, ((X.pollenNames || {})[k] || k) + " · " + (X.pollenLevels || [])[lvl - 1]));
      });
      box.appendChild(any ? pl : el("p", "meta", X.pollenNone || ""));
      sec.hidden = false;
    }).catch(function () { sec.hidden = true; });
  }
  function go(place) {
    current = place; var my = ++seq;
    airRender(place);
    stateEl.textContent = X.loading; retry.hidden = true;
    q("[data-wx-place]").textContent = place.name;
    W.load(place.lat, place.lon).then(function (d) { if (my === seq) render(place, d); })
      .catch(function (e) {
        if (my !== seq) return;
        nowBox.hidden = true; q("[data-wx-hourly-box]").hidden = true; q("[data-wx-daily-box]").hidden = true;
        if (e && e.consent) { // dış hizmetlere izin yok: yer tutucu
          stateEl.textContent = X.consent + " ";
          var b = el("button", "btn btn-sm", X.allow); b.type = "button"; b.setAttribute("data-consent-allow-ext", "");
          stateEl.appendChild(b);
          return;
        }
        stateEl.textContent = X.error; retry.hidden = false;
      });
  }
  window.addEventListener("sth-consent", function () { if (current) go(current); });
  retry.addEventListener("click", function () { W.clearFailures(); if (current) go(current); });
  window.sthPlace.use(go);
})();
