/* Hava durumu çekirdeği: Open-Meteo (CORS açık, anahtarsız) ve tam ekran saatin yanındaki hava durumu kutusu.
   Koordinatlar yaklaşık (2 ondalık) ve yönlendiren sayfa adresi gönderilmeden doğrudan tarayıcıdan gider. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;
  var API = "https://api.open-meteo.com/v1/forecast";
  var X = function () { return S.T.wx || {}; };
  var ICONS = {
    clear: ["☀️", "🌙"], mostlyClear: ["🌤️", "🌙"], partly: ["⛅", "☁️"], overcast: ["☁️", "☁️"], fog: ["🌫️", "🌫️"],
    drizzle: ["🌦️", "🌧️"], freezingDrizzle: ["🌧️", "🌧️"], rain: ["🌧️", "🌧️"], freezingRain: ["🌧️", "🌧️"],
    snow: ["🌨️", "🌨️"], snowGrains: ["❄️", "❄️"], showers: ["🌦️", "🌧️"], snowShowers: ["🌨️", "🌨️"], thunder: ["⛈️", "⛈️"], thunderHail: ["⛈️", "⛈️"]
  };
  function codeKey(c) {
    if (c === 0) return "clear"; if (c === 1) return "mostlyClear"; if (c === 2) return "partly"; if (c === 3) return "overcast";
    if (c === 45 || c === 48) return "fog"; if (c >= 51 && c <= 55) return "drizzle"; if (c === 56 || c === 57) return "freezingDrizzle";
    if (c >= 61 && c <= 65) return "rain"; if (c === 66 || c === 67) return "freezingRain"; if (c >= 71 && c <= 75) return "snow";
    if (c === 77) return "snowGrains"; if (c >= 80 && c <= 82) return "showers"; if (c === 85 || c === 86) return "snowShowers";
    if (c === 95) return "thunder"; if (c === 96 || c === 99) return "thunderHail";
    return "partly";
  }
  function info(code, isDay) {
    var k = codeKey(code);
    return { key: k, icon: ICONS[k][isDay === 0 ? 1 : 0], label: (X().codes || {})[k] || "" };
  }
  function compass(deg) { var c = X().compass || []; return c[Math.round(((deg % 360) + 360) % 360 / 45) % 8] || ""; }
  var round2 = function (v) { return Math.round(v * 100) / 100; };
  var cache = {}, inflight = {}, failed = {};
  function load(lat, lon) {
    lat = round2(lat); lon = round2(lon);
    var key = lat + "," + lon, now = Date.now();
    if (cache[key] && now - cache[key].at < 600000) return Promise.resolve(cache[key].data);
    if (inflight[key]) return inflight[key]; // aynı konum için eşzamanlı istekler tek istekte birleşir
    if (failed[key] && now - failed[key] < 60000) return Promise.reject(new Error("backoff")); // hata sonrası 1 dk bekle (hız sınırına yüklenme)
    var url = API + "?latitude=" + lat + "&longitude=" + lon +
      "&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,pressure_msl,is_day,uv_index" +
      "&hourly=temperature_2m,precipitation_probability,weather_code,is_day" +
      "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,sunrise,sunset,uv_index_max" +
      "&timezone=auto&forecast_days=7";
    inflight[key] = fetch(url, { credentials: "omit", referrerPolicy: "no-referrer" })
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(function (d) { cache[key] = { at: now, data: d }; delete failed[key]; delete inflight[key]; return d; })
      .catch(function (e) { failed[key] = Date.now(); delete inflight[key]; throw e; });
    return inflight[key];
  }
  var temp = function (t) { return Math.round(t) + "°"; };
  window.sthWeather = { load: load, info: info, compass: compass, temp: temp, clearFailures: function () { failed = {}; } };

  /* Hava durumlu saat modelleri: yalnız model seçiliyken (görünürken) Open-Meteo'ya gidilir */
  function mk(tag, cls, text) { var e = doc.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  function wxPlace(stage) {
    var lat = parseFloat(stage.getAttribute("data-wx-lat")), lon = parseFloat(stage.getAttribute("data-wx-lon"));
    if (isFinite(lat) && isFinite(lon)) return { name: stage.getAttribute("data-wx-name") || "", lat: lat, lon: lon };
    return window.sthPlace && window.sthPlace.saved();
  }
  function hourLabel(iso) {
    var h = parseInt(iso.slice(11, 13), 10);
    try { return new Intl.DateTimeFormat(S.locale, { hour: "numeric", hour12: !!(S.prefs && S.prefs.h12) }).format(new Date(2000, 0, 1, h)); } catch (e) { return iso.slice(11, 16); }
  }
  function fillBlock(box, place, d) {
    var kind = box.getAttribute("data-wxc"), c = d.current, i = info(c.weather_code, c.is_day), x = X();
    box.textContent = "";
    var head = mk("p", "wxc-main");
    var ic = mk("span", "wxc-icon", i.icon); ic.setAttribute("aria-hidden", "true");
    head.appendChild(ic); head.appendChild(mk("span", "wxc-temp", temp(c.temperature_2m)));
    box.appendChild(head);
    box.appendChild(mk("p", "wxc-cond", i.label + (place.name ? " · " + place.name : "")));
    if (kind === "card") {
      var hl = d.daily ? temp(d.daily.temperature_2m_max[0]) + " / " + temp(d.daily.temperature_2m_min[0]) : "";
      box.appendChild(mk("p", "wxc-more", x.feelsLike + " " + temp(c.apparent_temperature) + (hl ? " · " + hl : "")));
      box.appendChild(mk("p", "wxc-more", x.humidity + " " + Math.round(c.relative_humidity_2m) + "% · " + x.wind + " " + Math.round(c.wind_speed_10m) + " km/h"));
    } else if (kind === "hours" && d.hourly) {
      var strip = mk("ul", "wxc-hours"), start = d.hourly.time.indexOf(c.time.slice(0, 13) + ":00");
      if (start < 0) start = 0;
      for (var k = 1; k <= 6 && start + k < d.hourly.time.length; k++) {
        var j = start + k, hi = info(d.hourly.weather_code[j], d.hourly.is_day[j]), li = mk("li");
        li.appendChild(mk("span", "wxc-h", hourLabel(d.hourly.time[j])));
        var hic = mk("span", "wxc-hi", hi.icon); hic.setAttribute("aria-hidden", "true"); li.appendChild(hic);
        li.appendChild(mk("span", "wxc-ht", temp(d.hourly.temperature_2m[j])));
        strip.appendChild(li);
      }
      box.appendChild(strip);
    }
  }
  function wxMessage(box, key, withLink) {
    box.textContent = "";
    var p = mk("p", "wxc-msg", (X()[key] || "") + (withLink ? " " : ""));
    if (withLink) {
      var src = doc.querySelector("[data-fs-weather-box]"), a = mk("a", null, X().fsLink);
      a.href = src ? src.getAttribute("data-weather-url") : "#";
      p.appendChild(a);
    }
    box.appendChild(p);
  }
  Array.prototype.forEach.call(doc.querySelectorAll(".stage"), function (stage) {
    var slides = Array.prototype.slice.call(stage.querySelectorAll(".s-wx"));
    if (!slides.length) return;
    function refresh(slide) {
      var box = slide.querySelector("[data-wxc]"), place = wxPlace(stage);
      if (!box) return;
      if (!place) { wxMessage(box, "fsNeed", true); return; }
      load(place.lat, place.lon).then(function (d) { fillBlock(box, place, d); }).catch(function () { wxMessage(box, "fsFail", false); });
    }
    function sync(slide) {
      var on = slide.getAttribute("aria-hidden") === "false";
      if (!on) { clearInterval(slide._wxt); slide._wxt = 0; return; }
      refresh(slide);
      if (!slide._wxt) slide._wxt = setInterval(function () { refresh(slide); }, 10 * 60 * 1000);
    }
    slides.forEach(function (slide) {
      new MutationObserver(function () { sync(slide); }).observe(slide, { attributes: true, attributeFilter: ["aria-hidden"] });
      sync(slide);
    });
  });

  /* Tam ekran: saatin yanında hava durumu (isteğe bağlı, varsayılan kapalı) */
  var box = doc.querySelector("[data-fs-weather-box]"), btn = doc.querySelector("[data-fs-weather]");
  if (!box || !btn) return;
  var stage = box.closest(".stage"), timer = null, shownKey = "";
  function el(tag, cls, text) { var e = doc.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  function fill(place, d) {
    var c = d.current, i = info(c.weather_code, c.is_day);
    box.textContent = "";
    box.appendChild(el("p", "fsw-place", place.name));
    var row = el("p", "fsw-main"); row.appendChild(el("span", "fsw-icon", i.icon)); row.appendChild(el("span", "fsw-temp", temp(c.temperature_2m)));
    row.firstChild.setAttribute("aria-hidden", "true");
    box.appendChild(row);
    box.appendChild(el("p", "fsw-cond", i.label));
    var x = X();
    box.appendChild(el("p", "fsw-more", x.feelsLike + " " + temp(c.apparent_temperature) + " · " + (d.daily ? temp(d.daily.temperature_2m_max[0]) + " / " + temp(d.daily.temperature_2m_min[0]) : "")));
    box.appendChild(el("p", "fsw-more", x.humidity + " " + Math.round(c.relative_humidity_2m) + "% · " + x.wind + " " + Math.round(c.wind_speed_10m) + " km/h"));
  }
  function need() {
    var x = X();
    box.textContent = "";
    var p = el("p", "fsw-more", x.fsNeed + " ");
    var a = el("a", null, x.fsLink); a.href = box.getAttribute("data-weather-url");
    p.appendChild(a); box.appendChild(p);
  }
  function refresh() {
    var place = window.sthPlace && window.sthPlace.saved();
    if (!place) { need(); return; }
    window.sthWeather.load(place.lat, place.lon).then(function (d) { fill(place, d); }).catch(function () { box.textContent = ""; box.appendChild(el("p", "fsw-more", X().fsFail)); });
  }
  function update() {
    var on = S.prefs.fsWeather === true, full = stage.classList.contains("fs-on");
    btn.setAttribute("aria-pressed", String(on));
    box.hidden = !(on && full);
    clearInterval(timer);
    if (on && full) { refresh(); timer = setInterval(refresh, 15 * 60 * 1000); }
  }
  btn.addEventListener("click", function () { S.prefs.fsWeather = !S.prefs.fsWeather; S.store("sth-prefs", JSON.stringify(S.prefs)); S.track("tam-ekran-hava", { acik: S.prefs.fsWeather }); update(); });
  new MutationObserver(function () { var full = stage.classList.contains("fs-on"); if (full !== (shownKey === "1")) { shownKey = full ? "1" : "0"; update(); } }).observe(stage, { attributes: true, attributeFilter: ["class"] });
  update();
})();
