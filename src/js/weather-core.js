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
  /* Open-Meteo yalnız çerez bandında "dış hizmetler" izni verildiyse çağrılır (consent.js) */
  function allowed() { return !!(window.sthConsent && window.sthConsent.ext()); }
  function load(lat, lon) {
    if (!allowed()) { var ce = new Error("consent"); ce.consent = true; return Promise.reject(ce); }
    lat = round2(lat); lon = round2(lon);
    var key = lat + "," + lon, now = Date.now();
    if (cache[key] && now - cache[key].at < 600000) return Promise.resolve(cache[key].data);
    if (inflight[key]) return inflight[key]; // aynı konum için eşzamanlı istekler tek istekte birleşir
    if (failed[key] && now - failed[key] < 60000) return Promise.reject(new Error("backoff")); // hata sonrası 1 dk bekle (hız sınırına yüklenme)
    var url = API + "?latitude=" + lat + "&longitude=" + lon +
      "&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,pressure_msl,is_day,uv_index,cloud_cover,wind_gusts_10m,visibility,dew_point_2m" +
      "&hourly=temperature_2m,precipitation_probability,weather_code,is_day,wind_speed_10m,apparent_temperature,precipitation,wind_gusts_10m,wind_direction_10m,relative_humidity_2m,cloud_cover" +
      "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,sunrise,sunset,uv_index_max" +
      "&timezone=auto&forecast_days=14";
    inflight[key] = fetch(url, { credentials: "omit", referrerPolicy: "no-referrer" })
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(function (d) { cache[key] = { at: now, data: d }; delete failed[key]; delete inflight[key]; return d; })
      .catch(function (e) { failed[key] = Date.now(); delete inflight[key]; throw e; });
    return inflight[key];
  }
  /* Hava kalitesi ve polen (2. analiz #111): Open-Meteo Air Quality API (CAMS), aynı izin; 30 dk önbellek */
  var airCache = {};
  function air(lat, lon) {
    if (!allowed()) { var ce = new Error("consent"); ce.consent = true; return Promise.reject(ce); }
    lat = round2(lat); lon = round2(lon);
    var key = lat + "," + lon, now = Date.now();
    if (airCache[key] && now - airCache[key].at < 1800000) return Promise.resolve(airCache[key].data);
    var url = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + lat + "&longitude=" + lon + "&timezone=auto" +
      "&current=european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide,alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen";
    return fetch(url, { credentials: "omit", referrerPolicy: "no-referrer" })
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(function (d) { airCache[key] = { at: now, data: d }; return d; });
  }
  /* Fotoğraf ışığı (9 Kasım 2026): alçak/orta/yüksek bulut ve yağış, saatlik, bugün + 6 gün; aynı izin, 30 dk önbellek.
     timeformat=unixtime: saatler mutlak an (yaz saati geçişinde kaymaz). */
  var cloudCache = {}, cloudFailed = {};
  function clouds(lat, lon) {
    if (!allowed()) { var ce = new Error("consent"); ce.consent = true; return Promise.reject(ce); }
    lat = round2(lat); lon = round2(lon);
    var key = lat + "," + lon, now = Date.now();
    if (cloudCache[key] && now - cloudCache[key].at < 1800000) return Promise.resolve(cloudCache[key].data);
    if (cloudFailed[key] && now - cloudFailed[key] < 60000) return Promise.reject(new Error("backoff"));
    var url = API + "?latitude=" + lat + "&longitude=" + lon +
      "&hourly=cloud_cover_low,cloud_cover_mid,cloud_cover_high,precipitation&timezone=auto&timeformat=unixtime&forecast_days=7";
    return fetch(url, { credentials: "omit", referrerPolicy: "no-referrer" })
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(function (d) { cloudCache[key] = { at: now, data: d }; delete cloudFailed[key]; return d; })
      .catch(function (e) { cloudFailed[key] = Date.now(); throw e; });
  }
  /* Gün doğumu/batımı fotoğrafı tahmini; uygulamalardaki kuralla birebir (iOS, Android). L, M, H: alçak, orta, yüksek
     bulut (%), P: yağış (mm/sa), gün doğumuna/batımına en yakın saatlik değer. Renkleri orta ve yüksek bulut yakalar;
     ufuktaki alçak bulut ve yağış Güneş'i kapatır; açık gökyüzü temiz ama sade. */
  function photoSky(L, M, H, P) {
    if (P >= 0.5 || L >= 75) return "poor";
    var U = Math.max(M, H);
    if (L < 40 && U >= 20 && U <= 80) return "good";
    if (L < 60) return "fair";
    return "poor";
  }
  /* Düzeyler: uygulamalardaki Outdoor.aqiLevel / pollenLevel ile aynı eşikler */
  function aqiLevel(v) { return v < 20 ? 0 : v < 40 ? 1 : v < 60 ? 2 : v < 80 ? 3 : v < 100 ? 4 : 5; }
  function pollenLevel(g) { return g < 1 ? 0 : g < 20 ? 1 : g < 100 ? 2 : 3; }
  var temp = function (t) { return Math.round(t) + "°"; };
  /* Sözlü açıklamalar ve günlük özet (2. analiz #108): anahtar döner; eşikler uygulamalardaki WeatherWords ile aynı
     (Android core/WeatherWords.kt, iOS SaatCore/WeatherWords.swift) */
  var words = {
    wind: function (k) { return k < 6 ? "calm" : k < 20 ? "light" : k < 39 ? "breezy" : k < 62 ? "strong" : "gale"; },
    uv: function (u) { return u < 3 ? "low" : u < 6 ? "moderate" : u < 8 ? "high" : u < 11 ? "veryHigh" : "extreme"; },
    humidity: function (h) { return h < 30 ? "dry" : h < 60 ? "comfy" : h < 80 ? "humid" : "veryHumid"; },
    visibility: function (m) { return m < 1000 ? "fog" : m < 4000 ? "haze" : m < 10000 ? "good" : "clear"; },
    dew: function (c) { return c < 10 ? "dry" : c < 16 ? "comfy" : c < 20 ? "muggy" : "oppressive"; },
    feels: function (t, f) { return f <= t - 3 ? "colder" : f >= t + 3 ? "warmer" : null; }
  };
  /* next: [{hour, code, rain, wind}] şimdiki saatten başlayarak; ilk 12'si bakılır */
  function summary(next, todayMax, tomorrowMax) {
    var h = next.slice(0, 12), i;
    for (i = 0; i < h.length; i++) if (h[i].code >= 95 && h[i].code <= 99) return { key: "storm", hour: h[i].hour };
    for (i = 0; i < h.length; i++) if (h[i].rain >= 60) return { key: (h[i].code >= 71 && h[i].code <= 77) || h[i].code === 85 || h[i].code === 86 ? "snow" : "rain", hour: h[i].hour };
    for (i = 0; i < h.length; i++) if (h[i].hour >= 12 && h[i].hour <= 18 && h[i].wind >= 30) return { key: "windyAfternoon" };
    if (tomorrowMax != null) {
      var d = tomorrowMax - todayMax;
      if (d <= -5) return { key: "colder", n: Math.round(-d) };
      if (d >= 5) return { key: "warmer", n: Math.round(d) };
    }
    return { key: "default" };
  }
  /* İzin yokken gösterilen yer tutucu: kısa açıklama ve "İzin ver" düğmesi */
  function consentNote(box) {
    box.textContent = "";
    var p = mk("p", "wxc-msg consent-ph", (X().consent || "") + " ");
    var b = mk("button", "btn btn-ghost btn-sm", X().allow || "");
    b.type = "button"; b.setAttribute("data-consent-allow-ext", "");
    p.appendChild(b);
    box.appendChild(p);
  }
  window.sthWeather = { load: load, air: air, clouds: clouds, photoSky: photoSky, aqiLevel: aqiLevel, pollenLevel: pollenLevel, info: info, compass: compass, temp: temp, words: words, summary: summary, allowed: allowed, consentNote: consentNote, clearFailures: function () { failed = {}; } };

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
      box.appendChild(mk("p", "wxc-more", x.humidity + " " + S.pct(Math.round(c.relative_humidity_2m)) + " · " + x.wind + " " + Math.round(c.wind_speed_10m) + " km/h"));
    } else if (kind === "week" && d.daily) {
      var days = mk("ul", "wxc-days");
      for (var k = 1; k < d.daily.time.length && k <= 6; k++) {
        var di = info(d.daily.weather_code[k], 1), li = mk("li"), dt = new Date(d.daily.time[k] + "T12:00:00Z"), wd = "";
        try { wd = new Intl.DateTimeFormat(S.locale, { weekday: "short", timeZone: "UTC" }).format(dt); } catch (e) { wd = d.daily.time[k].slice(5); }
        li.appendChild(mk("span", "wxc-h", wd));
        var dic = mk("span", "wxc-hi", di.icon); dic.setAttribute("aria-hidden", "true"); li.appendChild(dic);
        li.appendChild(mk("span", "wxc-ht", temp(d.daily.temperature_2m_max[k])));
        li.appendChild(mk("span", "wxc-lo", temp(d.daily.temperature_2m_min[k])));
        days.appendChild(li);
      }
      box.appendChild(days);
    }
  }
  function wxMessage(box, key, withLink) {
    box.textContent = "";
    var p = mk("p", "wxc-msg", (X()[key] || "") + (withLink ? " " : ""));
    if (withLink) {
      var a = mk("a", null, X().fsLink);
      a.href = "#"; a.setAttribute("data-place-open", "");
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
      load(place.lat, place.lon).then(function (d) { fillBlock(box, place, d); }).catch(function (e) { if (e && e.consent) consentNote(box); else wxMessage(box, "fsFail", false); });
    }
    function sync(slide) {
      var on = slide.getAttribute("aria-hidden") !== "true";
      if (!on) { clearInterval(slide._wxt); slide._wxt = 0; return; }
      refresh(slide);
      if (!slide._wxt) slide._wxt = setInterval(function () { refresh(slide); }, 10 * 60 * 1000);
    }
    window.addEventListener("sth-consent", function () { slides.forEach(function (slide) { if (slide.getAttribute("aria-hidden") !== "true") refresh(slide); }); });
    window.addEventListener("sth-place", function () { slides.forEach(function (slide) { if (slide.getAttribute("aria-hidden") !== "true") refresh(slide); }); });
    slides.forEach(function (slide) {
      new MutationObserver(function () { sync(slide); }).observe(slide, { attributes: true, attributeFilter: ["aria-hidden"] });
      sync(slide);
    });
  });
})();
