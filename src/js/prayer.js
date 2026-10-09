/* Namaz vakitleri: konuma ya da seçilen ile göre günlük vakitler, sıradaki vakte kalan süre, aylık çizelge.
   Hesap tarayıcıda yapılır (prayer-calc.js); konum sunucuya gönderilmez. */
(function () {
  "use strict";
  var S = window.sth, P = window.sthPrayer;
  if (!S || !P) return;
  var doc = document;
  var root = doc.querySelector("[data-prayer]");
  if (!root) return;
  var T = S.T.pr || {};
  var CAL = S.cfg.cal || {};
  var months = CAL.months || [];
  var order = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"];
  var TR_TZ = "Europe/Istanbul";

  var citySel = root.querySelector("[data-pr-city]");
  var geoBtn = root.querySelector("[data-pr-geo]");
  var statusEl = root.querySelector("[data-pr-status]");
  var placeEl = root.querySelector("[data-pr-place]");
  var labelEl = root.querySelector("[data-pr-nextlabel]");
  var cdEl = root.querySelector("[data-pr-countdown]");
  var rows = {};
  order.forEach(function (k) { rows[k] = root.querySelector('[data-pr="' + k + '"]'); });
  var mTable = doc.querySelector("[data-pr-month]");
  var mTitle = doc.querySelector("[data-pr-month-title]");
  var mCap = doc.querySelector("[data-pr-caption]");
  var mPrev = doc.querySelector("[data-pr-prev]"), mNext = doc.querySelector("[data-pr-next]");

  var state = null, view = null, lastDay = "", nextAt = 0, nextKey = "";

  function pad(n) { return String(n).padStart(2, "0"); }
  function fmt(min) {
    if (min == null) return "–";
    min = ((min % 1440) + 1440) % 1440;
    return pad(Math.floor(min / 60)) + ":" + pad(min % 60);
  }
  function offHours(y, m, d) { return S.offsetMin(new Date(Date.UTC(y, m - 1, d, 12)), state.tz) / 60; }
  /* Günün vakitleri: gece yarısından itibaren dakika (yuvarlanmış) */
  function dayTimes(y, m, d) {
    var off = offHours(y, m, d);
    var r = P.times(y, m, d, state.lat, state.lon, off);
    var out = { off: off };
    order.forEach(function (k) { out[k] = isFinite(r[k]) ? Math.round(r[k] * 60) : null; });
    return out;
  }
  function instant(y, m, d, min, off) { return Date.UTC(y, m - 1, d) + (min - off * 60) * 60000; }
  function addDay(y, m, d, n) { var t = new Date(Date.UTC(y, m - 1, d + n)); return { y: t.getUTCFullYear(), m: t.getUTCMonth() + 1, d: t.getUTCDate() }; }

  function findNext(nowMs) {
    var p = S.parts(new Date(nowMs), state.tz);
    for (var add = 0; add <= 1; add++) {
      var dd = addDay(p.year, p.month, p.day, add), t = dayTimes(dd.y, dd.m, dd.d);
      for (var i = 0; i < order.length; i++) {
        var min = t[order[i]];
        if (min == null) continue;
        var at = instant(dd.y, dd.m, dd.d, min, t.off);
        if (at > nowMs) return { key: order[i], at: at, today: add === 0 };
      }
    }
    return null;
  }

  function renderToday() {
    var now = S.nowMs(), p = S.parts(new Date(now), state.tz);
    lastDay = p.year + "-" + p.month + "-" + p.day;
    var t = dayTimes(p.year, p.month, p.day);
    order.forEach(function (k) {
      var td = rows[k] && rows[k].querySelector("[data-pr-time]");
      if (td) td.textContent = fmt(t[k]);
    });
    pickNext(now);
  }
  function pickNext(now) {
    var nx = findNext(now);
    order.forEach(function (k) { if (rows[k]) rows[k].classList.remove("is-next"); });
    if (!nx) { nextAt = 0; nextKey = ""; labelEl.textContent = " "; cdEl.textContent = "–"; return; }
    nextAt = nx.at; nextKey = nx.key;
    if (nx.today && rows[nx.key]) rows[nx.key].classList.add("is-next");
    labelEl.textContent = S.tpl(T.nextIn, { name: T.names[nx.key] });
  }
  function tick() {
    if (!state) return;
    var now = S.nowMs(), p = S.parts(new Date(now), state.tz);
    if (lastDay !== p.year + "-" + p.month + "-" + p.day) { renderToday(); renderMonth(); return; }
    if (!nextAt || now >= nextAt) { pickNext(now); }
    if (nextAt) {
      var s = Math.max(0, Math.floor((nextAt - now) / 1000));
      cdEl.textContent = pad(Math.floor(s / 3600)) + ":" + pad(Math.floor((s % 3600) / 60)) + ":" + pad(s % 60);
    }
  }

  function renderMonth() {
    if (!mTable || !state) return;
    var now = S.parts(new Date(S.nowMs()), state.tz);
    if (!view) view = { y: now.year, m: now.month };
    var days = new Date(Date.UTC(view.y, view.m, 0)).getUTCDate();
    var head = mTable.tHead, body = mTable.tBodies[0];
    body.textContent = "";
    for (var d = 1; d <= days; d++) {
      var t = dayTimes(view.y, view.m, d), tr = doc.createElement("tr");
      var wd = (new Date(Date.UTC(view.y, view.m - 1, d)).getUTCDay() + 6) % 7;
      var th = doc.createElement("th"); th.scope = "row";
      th.textContent = d + " " + ((CAL.weekdaysShort || CAL.weekdays || [])[wd] || "");
      tr.appendChild(th);
      order.forEach(function (k) { var td = doc.createElement("td"); td.textContent = fmt(t[k]); tr.appendChild(td); });
      if (view.y === now.year && view.m === now.month && d === now.day) tr.className = "is-today";
      if (wd === 4) tr.className += " is-fri";
      body.appendChild(tr);
    }
    var vars = { month: months[view.m - 1], year: view.y, place: state.name };
    if (mTitle) mTitle.textContent = S.tpl(T.monthHeading, vars);
    if (mCap) mCap.textContent = S.tpl(T.tableCaption, vars);
  }

  function setState(next) {
    state = next; view = null;
    if (placeEl) placeEl.textContent = state.name;
    renderToday(); renderMonth(); tick();
  }
  function fromOption(opt) {
    return { kind: "city", city: opt.value, name: opt.textContent, lat: parseFloat(opt.getAttribute("data-lat")), lon: parseFloat(opt.getAttribute("data-lon")), tz: TR_TZ };
  }
  function save() {
    if (!state || root.getAttribute("data-mode") !== "hub") return;
    S.store("sth-prayer", JSON.stringify(state.kind === "geo" ? { kind: "geo", lat: state.lat, lon: state.lon } : { kind: "city", city: state.city }));
  }

  if (mPrev) mPrev.addEventListener("click", function () { if (!view) return; view.m--; if (view.m < 1) { view.m = 12; view.y--; } renderMonth(); });
  if (mNext) mNext.addEventListener("click", function () { if (!view) return; view.m++; if (view.m > 12) { view.m = 1; view.y++; } renderMonth(); });

  if (root.getAttribute("data-mode") === "city") {
    setState({ kind: "city", city: root.getAttribute("data-city"), name: root.getAttribute("data-name"), lat: parseFloat(root.getAttribute("data-lat")), lon: parseFloat(root.getAttribute("data-lon")), tz: TR_TZ });
  } else {
    var saved = null;
    try { saved = JSON.parse(S.store("sth-prayer") || "null"); } catch (e) {}
    var geoState = function (lat, lon) {
      return { kind: "geo", name: T.yourLocation + " (" + lat.toFixed(2) + ", " + lon.toFixed(2) + ")", lat: lat, lon: lon, tz: S.localTz };
    };
    if (saved && saved.kind === "geo" && isFinite(saved.lat) && isFinite(saved.lon)) setState(geoState(saved.lat, saved.lon));
    else {
      if (saved && saved.kind === "city") for (var i = 0; i < citySel.options.length; i++) if (citySel.options[i].value === saved.city) citySel.selectedIndex = i;
      setState(fromOption(citySel.options[citySel.selectedIndex]));
    }
    citySel.addEventListener("change", function () {
      setState(fromOption(citySel.options[citySel.selectedIndex])); save();
      if (statusEl) statusEl.textContent = "";
      S.track("namaz-konum", { tur: "il" });
    });
    geoBtn.addEventListener("click", function () {
      if (!navigator.geolocation) { statusEl.textContent = T.locationUnsupported; return; }
      statusEl.textContent = T.locating;
      navigator.geolocation.getCurrentPosition(function (pos) {
        statusEl.textContent = "";
        setState(geoState(pos.coords.latitude, pos.coords.longitude)); save();
        S.track("namaz-konum", { tur: "konum" });
      }, function () { statusEl.textContent = T.locationDenied; }, { enableHighAccuracy: false, timeout: 12000, maximumAge: 600000 });
    });
  }
  setInterval(tick, 1000);
})();
