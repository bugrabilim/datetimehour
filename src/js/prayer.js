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
  // Başlıkta ayın yalın hâli (Rusçada tarih içindeki çekimli hâlden ayrı: calendar.monthsTitle)
  var months = CAL.monthsTitle || CAL.months || [];
  var order = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"];
  var TR_TZ = "Europe/Istanbul";

  var placeEl = root.querySelector("[data-pr-place]");
  var labelEl = root.querySelector("[data-pr-nextlabel]");
  var cdEl = root.querySelector("[data-pr-countdown]");
  var rows = {};
  order.concat(["imsak", "duha"]).forEach(function (k) { rows[k] = root.querySelector('[data-pr="' + k + '"]'); });
  var mTable = doc.querySelector("[data-pr-month]");
  var mTitle = doc.querySelector("[data-pr-month-title]");
  var mCap = doc.querySelector("[data-pr-caption]");
  var mPrev = doc.querySelector("[data-pr-prev]"), mNext = doc.querySelector("[data-pr-next]");

  var state = null, view = null, lastDay = "", nextAt = 0, nextKey = "";
  /* Hesap yöntemi ve ikindi (sth-prayer: { m, h }); varsayılan Diyanet, standart ikindi */
  var mSel = doc.querySelector("[data-pr-method]"), aSel = doc.querySelector("[data-pr-asr]"), mAct = doc.querySelector("[data-pr-method-active]");
  /* Kullanıcı seçmediyse yöntem yerin saat dilimine göre (10 Kasım 2026, P.defaultMethod): Endonezya Kemenag,
     Güney Asya Karaçi + Hanefî … Seçim yapılınca (Diyanet dahil) saklanır ve her yerde o kullanılır. */
  var stored = {}; try { stored = JSON.parse(S.store("sth-prayer") || "{}") || {}; } catch (e) {}
  var method = { m: "diyanet", h: 0 }, fromRegion = false, params = P.methodParams("diyanet", false);
  var mKem = doc.querySelector("[data-pr-kemenag]"), trOnly = doc.querySelectorAll("[data-pr-tr]");
  function resolveMethod() {
    if (stored.m && P.METHODS[stored.m]) { method = { m: stored.m, h: stored.h ? 1 : 0 }; fromRegion = false; }
    else if (state) { method = P.defaultMethod(state.tz, state.lat, state.lon); fromRegion = method.m !== "diyanet" || !!method.h; }
    params = P.methodParams(method.m, !!method.h);
  }
  function showMethod() {
    if (mSel) mSel.value = P.METHODS[method.m] ? method.m : "diyanet";
    if (aSel) aSel.value = method.h ? "1" : "0";
    if (mKem) mKem.hidden = method.m !== "kemenag";
    // Türkiye Takvimi ek vakitleri ve ezani saat Diyanet'e özgü: başka yöntemde gizlenir
    Array.prototype.forEach.call(trOnly, function (el) { el.hidden = method.m !== "diyanet"; });
    if (!mAct) return;
    var custom = method.m !== "diyanet" || method.h;
    mAct.hidden = !custom;
    if (custom) mAct.textContent = S.tpl(T.mActive, { name: (T.mNames || {})[method.m] || "" }) + (method.h ? " · " + T.hanafi : "") + (fromRegion && T.mRegion ? " · " + T.mRegion : "");
  }
  function setMethod() {
    stored = { m: mSel ? mSel.value : "diyanet", h: aSel && aSel.value === "1" ? 1 : 0 };
    S.store("sth-prayer", JSON.stringify(stored));
    resolveMethod();
    showMethod();
    if (state) { renderToday(); renderMonth(); tick(); }
    doc.dispatchEvent(new CustomEvent("sth-prayer-method"));
  }
  if (mSel) mSel.addEventListener("change", setMethod);
  if (aSel) aSel.addEventListener("change", setMethod);

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
    var r = P.times(y, m, d, state.lat, state.lon, off, params);
    var out = { off: off };
    order.forEach(function (k) { out[k] = P.toMin(r[k], params, k); });
    var ex = P.methodRows(y, m, d, state.lat, state.lon, off, params, out.fajr);
    out.imsak = ex ? ex.imsak : null; out.duha = ex ? ex.duha : null;
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
    order.concat(["imsak", "duha"]).forEach(function (k) {
      if (!rows[k]) return;
      if (k === "imsak" || k === "duha") rows[k].hidden = t[k] == null;
      var td = rows[k].querySelector("[data-pr-time]");
      if (td) td.textContent = fmt(t[k]);
    });
    // Kerahat (yaklaşık, P.KERAHAT_MIN dakika): işrak, istiva, isfirar
    var km = P.KERAHAT_MIN || 45, kr = { israk: [t.sunrise, t.sunrise + km], istiva: [t.dhuhr - km, t.dhuhr], isfirar: [t.maghrib - km, t.maghrib] };
    Object.keys(kr).forEach(function (k) {
      var el = doc.querySelector('[data-pr-kr="' + k + '"]');
      if (el) el.textContent = kr[k][0] == null || isNaN(kr[k][0]) ? "–" : fmt(kr[k][0]) + "–" + fmt(kr[k][1]);
    });
    // Ek vakitler (2. analiz #59; Türkiye Takvimi tanımlarıyla yaklaşık) ve asr-ı sânî (Hanefî ikindi)
    if (P.extraTimes) {
      var raw = P.times(p.year, p.month, p.day, state.lat, state.lon, t.off, P.PARAMS);
      var ex = P.extraTimes(p.year, p.month, p.day, state.lat, state.lon, t.off, raw);
      ex.asrSani = P.times(p.year, p.month, p.day, state.lat, state.lon, t.off, P.methodParams("diyanet", true)).asr;
      Array.prototype.forEach.call(doc.querySelectorAll("[data-pr-ex]"), function (el) {
        var v = ex[el.getAttribute("data-pr-ex")];
        el.textContent = v == null || !isFinite(v) ? "–" : fmt(Math.round(v * 60));
      });
    }
    // Bayram namazı (2. analiz #56): bayramın ilk günü öğleye kadar, arefe günü "yarın"
    var bEl = doc.querySelector("[data-pr-bayram]");
    if (bEl && P.bayram && T.bayramDays && method.m !== "diyanet") bEl.hidden = true;
    else if (bEl && P.bayram && T.bayramDays) {
      var iso = function (q) { return q.y + "-" + pad(q.m) + "-" + pad(q.d); };
      var tod = { y: p.year, m: p.month, d: p.day }, tom = addDay(p.year, p.month, p.day, 1), txt = "";
      if (T.bayramDays.indexOf(iso(tod)) >= 0 && t.dhuhr != null && p.hour * 60 + p.minute < t.dhuhr) {
        txt = T.bayram + ": " + fmt(Math.round(P.bayram(P.times(tod.y, tod.m, tod.d, state.lat, state.lon, t.off, P.PARAMS)) * 60));
      } else if (T.bayramDays.indexOf(iso(tom)) >= 0) {
        var to = offHours(tom.y, tom.m, tom.d);
        txt = S.tpl(T.bayramTomorrow, { t: fmt(Math.round(P.bayram(P.times(tom.y, tom.m, tom.d, state.lat, state.lon, to, P.PARAMS)) * 60)) });
      }
      bEl.hidden = !txt;
      if (txt) { bEl.textContent = txt; bEl.title = T.bayramNote; }
    }
    // Ezani (alaturka) saat (2. analiz #121): son akşam vakti 12.00 sayılır
    var yd = addDay(p.year, p.month, p.day, -1), ty = dayTimes(yd.y, yd.m, yd.d);
    ezRef = { today: t.maghrib == null ? null : instant(p.year, p.month, p.day, t.maghrib, t.off), yday: ty.maghrib == null ? null : instant(yd.y, yd.m, yd.d, ty.maghrib, ty.off) };
    pickNext(now);
  }
  var ezRef = null, ezEl = doc.querySelector("[data-pr-ezani]");
  function renderEzani(now) {
    if (!ezEl || !ezRef || !T.ezani || method.m !== "diyanet") return;
    var ref = ezRef.today != null && now >= ezRef.today ? ezRef.today : ezRef.yday;
    if (ref == null) { ezEl.textContent = ""; return; }
    var m = Math.floor((now - ref) / 60000), h = Math.floor(m / 60) % 12;
    var txt = S.tpl(T.ezani, { t: pad(h || 12) + ":" + pad(m % 60) });
    if (ezEl.textContent !== txt) ezEl.textContent = txt;
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
    renderEzani(now);
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
    var sub = !!params.imsakMin, cols = sub ? ["imsak", "fajr", "sunrise", "duha", "dhuhr", "asr", "maghrib", "isha"] : order;
    Array.prototype.forEach.call(head.querySelectorAll("[data-pr-col]"), function (th) { th.hidden = !sub; });
    for (var d = 1; d <= days; d++) {
      var t = dayTimes(view.y, view.m, d), tr = doc.createElement("tr");
      var wd = (new Date(Date.UTC(view.y, view.m - 1, d)).getUTCDay() + 6) % 7;
      var th = doc.createElement("th"); th.scope = "row";
      th.textContent = d + " " + ((CAL.weekdaysShort || CAL.weekdays || [])[wd] || "");
      tr.appendChild(th);
      cols.forEach(function (k) { var td = doc.createElement("td"); td.textContent = fmt(t[k]); tr.appendChild(td); });
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
    resolveMethod(); showMethod();
    renderToday(); renderMonth(); tick();
  }
  if (mPrev) mPrev.addEventListener("click", function () { if (!view) return; view.m--; if (view.m < 1) { view.m = 12; view.y--; } renderMonth(); });
  if (mNext) mNext.addEventListener("click", function () { if (!view) return; view.m++; if (view.m > 12) { view.m = 1; view.y++; } renderMonth(); });

  if (root.getAttribute("data-mode") === "city") {
    setState({ kind: "city", city: root.getAttribute("data-city"), name: root.getAttribute("data-name"), lat: parseFloat(root.getAttribute("data-lat")), lon: parseFloat(root.getAttribute("data-lon")), tz: TR_TZ });
  } else {
    window.sthPlace.attach(root, function (p) { setState(p); });
  }
  setInterval(tick, 1000);
})();
