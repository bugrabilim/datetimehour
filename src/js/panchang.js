/* Panchang ve Rahu Kaal (10 Kasım 2026, Hindistan araştırması #9; uygulamalarla aynı tanımlar): seçili yerde ve günde gün
   doğumu/batımı, geleneksel olarak kaçınılan dilimler (Rahu Kaal, Yamaganda, Gulika) ve uygun sayılanlar (Abhijit, Brahma
   muhurta), yedi günün Rahu Kaal tablosu. Hesap tarayıcıda (prayer-calc.js hinduKaal). Hindu takviminin günlük satırı
   (tithi, nakşatra, ay, Vikram Samvat) Yeni Delhi gün doğumuna göre derlemede hesaplanmış kodlardan (assets/panchang-days.json,
   lib/calendars/hindu.mjs); adlar sayfanın dilinde. Gün seçimi ?d=YYYY-MM-DD. */
(function () {
  "use strict";
  var S = window.sth, P = window.sthPrayer;
  if (!S || !P || !window.sthPlace) return;
  var doc = document;
  var root = doc.querySelector("[data-panchang]");
  if (!root) return;
  var X = S.T.pc || {}, CAL = S.cfg.cal || {}, CFG = S.cfg.pc || {};
  var DASH = "—", HOUR = 3600000;
  var place = null, picked = null, lastToday = "", days = null, daysWait = false;
  var q = function (sel) { return doc.querySelector(sel); };
  var pad = function (n) { return String(n).padStart(2, "0"); };
  var isoOf = function (p) { return p.y + "-" + pad(p.m) + "-" + pad(p.d); };
  function setText(sel, v) { var e = q(sel); if (e) e.textContent = v; }

  function shift(p, n) { var t = new Date(Date.UTC(p.y, p.m - 1, p.d + n)); return { y: t.getUTCFullYear(), m: t.getUTCMonth() + 1, d: t.getUTCDate() }; }
  function today() { var r = S.parts(new Date(S.nowMs()), place.tz); return { y: r.year, m: r.month, d: r.day }; }
  function parseIso(s) {
    var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s || "");
    if (!m) return null;
    var p = { y: +m[1], m: +m[2], d: +m[3] }, c = shift(p, 0);
    return c.y === p.y && c.m === p.m && c.d === p.d && p.y >= 1900 && p.y <= 2100 ? p : null;
  }
  function offH(p) { return S.offsetMin(new Date(Date.UTC(p.y, p.m - 1, p.d, 12)), place.tz) / 60; }

  /* Saat: kullanıcının 24/12 seçimiyle, yerin saat diliminde */
  function hm(ms) {
    if (!isFinite(ms)) return DASH;
    var h12 = !!(S.prefs && S.prefs.h12);
    return S.fmt("pc-hm" + (h12 ? 12 : 24), place.tz, { hour: "2-digit", minute: "2-digit", hourCycle: h12 ? "h12" : "h23" }).format(new Date(Math.round(ms / 60000) * 60000));
  }
  function range(r) { return r && isFinite(r[0]) && isFinite(r[1]) ? hm(r[0]) + "–" + hm(r[1]) : DASH; }
  function dayLabel(p) {
    var wd = (new Date(Date.UTC(p.y, p.m - 1, p.d)).getUTCDay() + 6) % 7;
    return ((CAL.weekdaysShort || [])[wd] || "") + " " + S.tpl(CAL.shortFmt || "{day} {month}", { day: p.d, month: (CAL.months || [])[p.m - 1] });
  }
  function longLabel(p) {
    var wd = (new Date(Date.UTC(p.y, p.m - 1, p.d)).getUTCDay() + 6) % 7;
    return S.tpl(CAL.fullFmt || CAL.dateFmt || "{day} {month} {year}", { weekday: (CAL.weekdays || [])[wd] || "", day: p.d, month: (CAL.months || [])[p.m - 1], year: p.y });
  }

  /* Bir günün dilimleri mutlak an olarak (ms); Güneş doğmuyor ya da batmıyorsa null */
  function kaalOf(p) {
    var tz = offH(p), base = Date.UTC(p.y, p.m - 1, p.d);
    var k = P.hinduKaal(p.y, p.m, p.d, place.lat, place.lon, tz);
    if (!k) return null;
    var ms = function (h) { return base + (h - tz) * HOUR; };
    var pair = function (r) { return [ms(r[0]), ms(r[1])]; };
    return { sunrise: ms(k.sunrise), sunset: ms(k.sunset), rahu: pair(k.rahu), yamaganda: pair(k.yamaganda), gulika: pair(k.gulika), abhijit: pair(k.abhijit), brahma: pair(k.brahma) };
  }

  /* Hindu takviminin günlük satırı: kod ("m10t13n4v2082") → adlar " · " ile */
  function hinduLine(p) {
    if (!days) return null;
    var arr = days[String(p.y)];
    if (!arr) return "";
    var code = arr[Math.round((Date.UTC(p.y, p.m - 1, p.d) - Date.UTC(p.y, 0, 1)) / 86400000)];
    if (!code) return "";
    var names = CFG.names || {}, parts = String(code).match(/[a-z]+[0-9]+/gi) || [];
    return parts.map(function (k) { return names[k] || k; }).join(" · ");
  }
  function loadDays() {
    if (days || daysWait || !CFG.days) return;
    daysWait = true;
    fetch(CFG.days, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (j) { days = j; }, function () { days = {}; })
      .then(function () { daysWait = false; if (place) renderDay(); });
  }

  function renderDay() {
    var p = picked || today(), t = today(), k = kaalOf(p);
    setText("[data-pc-place]", place.name || "");
    setText("[data-pc-day]", longLabel(p));
    var din = q("[data-pc-date]"); if (din) din.value = isoOf(p);
    var tb = q("[data-pc-today]"); if (tb) tb.hidden = isoOf(p) === isoOf(t);
    var polar = q("[data-pc-polar]"); if (polar) polar.hidden = !!k;
    setText("[data-pc=sunrise]", k ? hm(k.sunrise) : DASH);
    setText("[data-pc=sunset]", k ? hm(k.sunset) : DASH);
    ["rahu", "yamaganda", "gulika", "abhijit", "brahma"].forEach(function (key) { setText("[data-pc=" + key + "]", k ? range(k[key]) : DASH); });
    // Abhijit: bazı geleneklerde çarşamba uygun sayılmaz (Drik Panchang)
    var wed = q("[data-pc-wed]"); if (wed) wed.hidden = new Date(Date.UTC(p.y, p.m - 1, p.d)).getUTCDay() !== 3;
    var line = hinduLine(p), hl = q("[data-pc-hindu]");
    if (hl && line !== null) hl.textContent = line || X.hinduNone || DASH;
    renderWeek(p);
  }
  function renderWeek(p) {
    var tb = q("[data-pc-week]");
    if (!tb) return;
    tb.textContent = "";
    for (var i = 0; i < 7; i++) {
      var d = shift(p, i), k = kaalOf(d), tr = doc.createElement("tr"), th = doc.createElement("th"), td = doc.createElement("td");
      th.scope = "row"; th.textContent = dayLabel(d);
      td.dir = "ltr"; td.textContent = k ? range(k.rahu) : DASH;
      tr.appendChild(th); tr.appendChild(td); tb.appendChild(tr);
    }
  }

  function go(p, push) {
    var t = today();
    picked = p && isoOf(p) !== isoOf(t) ? p : null;
    if (push) { try { history.replaceState(null, "", picked ? "?d=" + isoOf(picked) : location.pathname); } catch (e) {} }
    renderDay();
  }
  root.addEventListener("click", function (e) {
    var b = e.target.closest && e.target.closest("[data-pc-step], [data-pc-today]");
    if (!b || !place) return;
    if (b.hasAttribute("data-pc-today")) go(null, true);
    else go(shift(picked || today(), +b.getAttribute("data-pc-step")), true);
  });
  var din = q("[data-pc-date]");
  if (din) din.addEventListener("change", function () { var p = parseIso(din.value); if (p && place) go(p, true); });

  window.sthPlace.use(function (p) {
    if (!p) return;
    place = p;
    if (!lastToday) { picked = parseIso((location.search.match(/[?&]d=([\d-]+)/) || [])[1]); if (picked && isoOf(picked) === isoOf(today())) picked = null; }
    lastToday = isoOf(today());
    renderDay(); loadDays();
  });
  setInterval(function () {
    if (!place || doc.hidden) return;
    var t = isoOf(today());
    if (t !== lastToday) { lastToday = t; renderDay(); }
  }, 60000);
})();
