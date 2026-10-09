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
  function fact(dl, label, value) { var d = el("div", "fact"); d.appendChild(el("dt", null, label)); d.appendChild(el("dd", null, value)); dl.appendChild(d); }
  var pad = function (n) { return String(n).padStart(2, "0"); };

  function render(place, d) {
    var c = d.current, i = W.info(c.weather_code, c.is_day);
    q("[data-wx-place]").textContent = place.name;
    var theme = /^(clear|mostlyClear)$/.test(i.key) ? "clear" : /^(partly|overcast)$/.test(i.key) ? "cloud" : i.key === "fog" ? "fog" : /snow/i.test(i.key) ? "snow" : /thunder/.test(i.key) ? "storm" : "rain";
    root.className = "wx-hero wx-" + theme + (c.is_day === 0 ? " is-night" : "");
    q("[data-wx-icon]").textContent = i.icon;
    q("[data-wx-temp]").textContent = W.temp(c.temperature_2m);
    q("[data-wx-cond]").textContent = i.label;
    var dl = q("[data-wx-facts]"); dl.textContent = "";
    fact(dl, X.feelsLike, W.temp(c.apparent_temperature));
    fact(dl, X.humidity, Math.round(c.relative_humidity_2m) + "%");
    fact(dl, X.wind, Math.round(c.wind_speed_10m) + " km/h · " + S.tpl(X.windFrom, { dir: W.compass(c.wind_direction_10m) }));
    fact(dl, X.precipitation, (Math.round(c.precipitation * 10) / 10) + " mm");
    fact(dl, X.pressure, Math.round(c.pressure_msl) + " hPa");
    fact(dl, X.uv, String(Math.round(c.uv_index * 10) / 10));
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
      li.appendChild(el("span", "wx-h-rain", pp != null && pp >= 10 ? pp + "%" : " "));
      ul.appendChild(li);
    }
    q("[data-wx-hourly-box]").hidden = false;
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
      row.appendChild(el("span", "wx-d-rain", pr != null && pr >= 10 ? pr + "% · " + (Math.round(dd.precipitation_sum[j] * 10) / 10) + " mm" : " "));
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
  function go(place) {
    current = place; var my = ++seq;
    stateEl.textContent = X.loading; retry.hidden = true;
    q("[data-wx-place]").textContent = place.name;
    W.load(place.lat, place.lon).then(function (d) { if (my === seq) render(place, d); })
      .catch(function () { if (my !== seq) return; stateEl.textContent = X.error; retry.hidden = false; nowBox.hidden = true; q("[data-wx-hourly-box]").hidden = true; q("[data-wx-daily-box]").hidden = true; });
  }
  retry.addEventListener("click", function () { W.clearFailures(); if (current) go(current); });
  window.sthPlace.use(go);
})();
