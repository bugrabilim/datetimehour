/* Saat çevirici, toplantı planlayıcı ve şehir ikilisi çevirme tablosu.
   Yaz saati geçişleri tarayıcının IANA veritabanından (Intl) otomatik hesaplanır. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;
  var cities = S.cfg.cities || [];
  function pad(n) { return String(n).padStart(2, "0"); }
  function city(key) { for (var i = 0; i < cities.length; i++) if (cities[i].k === key) return cities[i]; return null; }
  function tzOf(key) { var c = city(key); return c ? c.tz : S.localTz; }
  function nameOf(key, conv) { var c = city(key); return c ? c.n : conv.mine; }

  // Yerel (şehir) duvar saatini UTC anına çevirir; yaz saati sınırında iki turda düzeltir.
  function zoneToUtc(y, mo, d, h, mi, tz) {
    var guess = Date.UTC(y, mo - 1, d, h, mi);
    var off = S.offsetMin(new Date(guess), tz);
    var t = guess - off * 60000;
    var off2 = S.offsetMin(new Date(t), tz);
    if (off2 !== off) t = guess - off2 * 60000;
    return t;
  }
  function inputValue(ms, tz) {
    var p = S.parts(new Date(ms), tz);
    return p.year + "-" + pad(p.month) + "-" + pad(p.day) + "T" + pad(p.hour) + ":" + pad(p.minute);
  }

  /* ---------- Şehir ikilisi: A saati → B saati tablosu (güncel farkla) ---------- */
  var pt = doc.querySelector("[data-pair-table]");
  if (pt) {
    var a = pt.getAttribute("data-a"), b = pt.getAttribute("data-b");
    var now = S.nowDate();
    var off = S.offsetMin(now, b) - S.offsetMin(now, a);
    var body = pt.tBodies[0];
    body.textContent = "";
    for (var h = 0; h < 24; h++) {
      var m = h * 60 + off;
      var tr = doc.createElement("tr"), th = doc.createElement("th"), td = doc.createElement("td");
      th.scope = "row"; th.textContent = pad(h) + ":00";
      var hh = Math.floor(((m % 1440) + 1440) % 1440 / 60), mm = ((m % 60) + 60) % 60;
      td.textContent = pad(hh) + ":" + pad(mm);
      if (m < 0 || m >= 1440) {
        var sp = doc.createElement("span"); sp.className = "meta";
        sp.textContent = " (" + (m < 0 ? pt.getAttribute("data-prev") : pt.getAttribute("data-next")) + ")";
        td.appendChild(sp);
      }
      tr.appendChild(th); tr.appendChild(td); body.appendChild(tr);
    }
  }

  /* ---------- Paylaşılabilir bağlantı ve takvime ekle (30 Ekim 2026) ----------
     Seçim adres çubuğuna yazılır (replaceState); .ics veri adresiyle indirilir, Google Takvim bağlantısıyla açılır. */
  function icsDate(ms) { var d = new Date(ms); return d.getUTCFullYear() + pad(d.getUTCMonth() + 1) + pad(d.getUTCDate()) + "T" + pad(d.getUTCHours()) + pad(d.getUTCMinutes()) + "00Z"; }
  function icsText(v) { return String(v).replace(/\\/g, "\\\\").replace(/[,;]/g, "\\$&").replace(/\r?\n/g, "\\n"); }
  function setUrl(params) {
    try {
      var u = new URL(location.href); u.search = "";
      Object.keys(params).forEach(function (k) { if (params[k] !== "" && params[k] != null) u.searchParams.set(k, params[k]); });
      history.replaceState(null, "", u.toString());
    } catch (e) {}
  }
  function shareSetup(box, T) {
    var st = box.querySelector("[data-share-status]");
    box.querySelector("[data-share]").addEventListener("click", function () {
      var url = location.href, done = function () { st.textContent = T.linkCopied; setTimeout(function () { st.textContent = ""; }, 2500); };
      try { navigator.clipboard.writeText(url).then(done, function () { window.prompt(T.shareLink, url); }); } catch (e) { window.prompt(T.shareLink, url); }
    });
  }
  function calLinks(box, ms, title, desc) {
    var ics = box.querySelector("[data-ics]"), g = box.querySelector("[data-gcal]");
    ics.hidden = g.hidden = ms == null;
    if (ms == null) return;
    var end = ms + 3600000, details = desc + "\n" + location.href;
    var file = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Saat Tarih//time.bumba.tr//TR", "BEGIN:VEVENT", "UID:" + ms + "@time.bumba.tr",
      "DTSTAMP:" + icsDate(Date.now()), "DTSTART:" + icsDate(ms), "DTEND:" + icsDate(end), "SUMMARY:" + icsText(title),
      "DESCRIPTION:" + icsText(details), "END:VEVENT", "END:VCALENDAR"].join("\r\n");
    ics.href = "data:text/calendar;charset=utf-8," + encodeURIComponent(file);
    g.href = "https://calendar.google.com/calendar/render?action=TEMPLATE&text=" + encodeURIComponent(title) +
      "&dates=" + icsDate(ms) + "/" + icsDate(end) + "&details=" + encodeURIComponent(details);
  }
  var query = new URLSearchParams(location.search);

  /* ---------- Saat çevirici ---------- */
  var cv = doc.querySelector("[data-converter]");
  if (cv) {
    var C = S.T.conv || {};
    var selF = cv.querySelector("[data-cv-from]"), selT = cv.querySelector("[data-cv-to]"), when = cv.querySelector("[data-cv-when]");
    var res = cv.querySelector("[data-cv-result]"), diffEl = cv.querySelector("[data-cv-diff]");
    var saved = {}; try { saved = JSON.parse(S.store("sth-conv") || "{}"); } catch (e) {}
    selF.value = saved.from != null && city(saved.from) !== undefined ? saved.from : "";
    selT.value = saved.to || "london";
    if (selT.value !== (saved.to || "london")) selT.value = "";
    // Paylaşılan bağlantı: ?from=istanbul&to=london&t=2026-11-03T15:00 (t kaynak şehrin saatiyle)
    if (query.has("from")) selF.value = city(query.get("from")) ? query.get("from") : "";
    if (query.has("to")) selT.value = city(query.get("to")) ? query.get("to") : "";
    var fmtOpts = { weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" };
    function setNow() { when.value = inputValue(S.nowMs(), tzOf(selF.value)); update(); }
    function update() {
      S.store("sth-conv", JSON.stringify({ from: selF.value, to: selT.value }));
      var m = /^(\d{4})-(\d\d)-(\d\d)T(\d\d):(\d\d)$/.exec(when.value);
      setUrl({ from: selF.value, to: selT.value, t: when.value });
      if (!m) { res.textContent = " "; diffEl.textContent = " "; calLinks(cv, null); return; }
      var ftz = tzOf(selF.value), ttz = tzOf(selT.value);
      var inst = zoneToUtc(+m[1], +m[2], +m[3], +m[4], +m[5], ftz);
      var f1 = new Intl.DateTimeFormat(S.locale, Object.assign({ timeZone: ftz }, fmtOpts)).format(new Date(inst));
      var f2 = new Intl.DateTimeFormat(S.locale, Object.assign({ timeZone: ttz }, fmtOpts)).format(new Date(inst));
      res.textContent = S.tpl(C.result, { from: nameOf(selF.value, C), fromTime: f1, to: nameOf(selT.value, C), toTime: f2 });
      var d = S.offsetMin(new Date(inst), ttz) - S.offsetMin(new Date(inst), ftz);
      diffEl.textContent = S.tpl(C.diff, { d: d === 0 ? "0" : (d > 0 ? "+" : "−") + S.durLabel(d) });
      calLinks(cv, inst, res.textContent, diffEl.textContent);
    }
    selF.addEventListener("change", function () { setNow(); });
    selT.addEventListener("change", update);
    when.addEventListener("input", update);
    cv.querySelector("[data-cv-now]").addEventListener("click", setNow);
    cv.querySelector("[data-cv-swap]").addEventListener("click", function () { var x = selF.value; selF.value = selT.value; selT.value = x; update(); });
    shareSetup(cv, C);
    if (/^\d{4}-\d\d-\d\dT\d\d:\d\d$/.test(query.get("t") || "")) { when.value = query.get("t"); update(); } else setNow();
    S.track("arac", { arac: "converter" });
  }

  /* ---------- Toplantı planlayıcı ---------- */
  var pl = doc.querySelector("[data-planner]");
  if (pl) {
    var P = S.T.plan || {};
    var selC = pl.querySelector("[data-pl-city]"), dateIn = pl.querySelector("[data-pl-date]");
    var chips = pl.querySelector("[data-pl-chips]"), table = pl.querySelector("[data-pl-table]"), common = pl.querySelector("[data-pl-common]");
    var sel = ["istanbul", "london", "new-york"];
    try { var sv = JSON.parse(S.store("sth-planner") || "null"); if (Array.isArray(sv) && sv.length) sel = sv.filter(city); } catch (e) {}
    // Paylaşılan bağlantı: ?c=istanbul,london&d=2026-11-03&h=14 (h ilk şehrin saatiyle)
    if (query.get("c")) { var qc = query.get("c").split(",").filter(city).slice(0, 6); if (qc.length) sel = qc; }
    if (!sel.length) sel = ["istanbul"];
    dateIn.value = inputValue(S.nowMs(), tzOf(sel[0])).slice(0, 10);
    if (/^\d{4}-\d\d-\d\d$/.test(query.get("d") || "")) dateIn.value = query.get("d");
    var picked = /^\d{1,2}$/.test(query.get("h") || "") && +query.get("h") < 24 ? +query.get("h") : null;
    var pickedEl = pl.querySelector("[data-pl-picked]");

    var workOf = function (h) { return h >= 9 && h < 18 ? "work" : (h >= 7 && h < 9) || (h >= 18 && h < 22) ? "edge" : "night"; };
    function render() {
      S.store("sth-planner", JSON.stringify(sel));
      chips.textContent = "";
      sel.forEach(function (k) {
        var li = doc.createElement("li"); li.className = "chip-item";
        var sp = doc.createElement("span"); sp.textContent = city(k).n;
        var bt = doc.createElement("button"); bt.type = "button"; bt.className = "toggle"; bt.textContent = "✕";
        bt.setAttribute("aria-label", P.remove + ": " + city(k).n);
        bt.addEventListener("click", function () { sel = sel.filter(function (x) { return x !== k; }); if (!sel.length) sel = [k]; render(); });
        li.appendChild(sp); li.appendChild(bt); chips.appendChild(li);
      });
      table.textContent = "";
      var cap = doc.createElement("caption"); cap.className = "sr-only"; cap.textContent = P.tableLabel; table.appendChild(cap);
      var m = /^(\d{4})-(\d\d)-(\d\d)$/.exec(dateIn.value);
      if (!m) return;
      var refTz = tzOf(sel[0]);
      var start = zoneToUtc(+m[1], +m[2], +m[3], 0, 0, refTz);
      var refDay = S.parts(new Date(start), refTz).day;
      var thead = doc.createElement("thead"), hr = doc.createElement("tr");
      var corner = doc.createElement("th"); corner.scope = "col"; corner.textContent = ""; hr.appendChild(corner);
      var instants = [];
      for (var h = 0; h < 24; h++) {
        var inst = start + h * 3600000; instants.push(inst);
        var th = doc.createElement("th"); th.scope = "col";
        var hb = doc.createElement("button"); hb.type = "button"; hb.textContent = pad(S.parts(new Date(inst), refTz).hour);
        hb.setAttribute("aria-pressed", picked === h ? "true" : "false");
        hb.addEventListener("click", (function (hh) { return function () { picked = picked === hh ? null : hh; render(); }; })(h));
        if (picked === h) th.className = "pl-picked";
        th.appendChild(hb); hr.appendChild(th);
      }
      thead.appendChild(hr); table.appendChild(thead);
      var tb = doc.createElement("tbody"), allWork = instants.map(function () { return true; });
      sel.forEach(function (k) {
        var tz = tzOf(k), tr = doc.createElement("tr"), rh = doc.createElement("th");
        rh.scope = "row"; rh.textContent = city(k).n; tr.appendChild(rh);
        instants.forEach(function (inst, i) {
          var p = S.parts(new Date(inst), tz), td = doc.createElement("td");
          var cls = workOf(p.hour); td.className = "pl-" + cls; td.textContent = pad(p.hour);
          if (cls !== "work") allWork[i] = false;
          var rp = S.parts(new Date(inst), tz);
          var shift = Date.UTC(rp.year, rp.month - 1, rp.day) - Date.UTC(+m[1], +m[2] - 1, +m[3]);
          if (k !== sel[0] && shift !== 0) { var sup = doc.createElement("sup"); sup.textContent = shift > 0 ? "+1" : "−1"; td.appendChild(sup); }
          if (picked === i) td.className += " pl-picked";
          tr.appendChild(td);
        });
        tb.appendChild(tr);
      });
      table.appendChild(tb);
      // Seçili saat: bağlantıya ve takvime girer
      setUrl({ c: sel.join(","), d: dateIn.value, h: picked });
      if (picked != null) {
        var pInst = instants[picked];
        pickedEl.textContent = S.tpl(P.picked, { time: pad(S.parts(new Date(pInst), refTz).hour) + ":00", city: city(sel[0]).n });
        var lines = sel.map(function (k) { var pp = S.parts(new Date(pInst), tzOf(k)); return city(k).n + " " + pad(pp.hour) + ":" + pad(pp.minute); });
        calLinks(pl, pInst, P.meeting + " · " + sel.map(function (k) { return city(k).n; }).join(", "), lines.join("\n"));
      } else {
        pickedEl.textContent = P.pickHint;
        calLinks(pl, null);
      }
      if (sel.length < 2) { common.textContent = P.minCities; return; }
      var ranges = [], from = -1;
      allWork.forEach(function (ok, i) {
        if (ok && from < 0) from = i;
        if ((!ok || i === 23) && from >= 0) { var to = ok ? i : i - 1; ranges.push(pad(S.parts(new Date(instants[from]), refTz).hour) + ":00–" + pad((S.parts(new Date(instants[to]), refTz).hour + 1) % 24) + ":00"); from = -1; }
      });
      common.textContent = ranges.length ? S.tpl(P.common, { city: city(sel[0]).n, hours: ranges.join(", ") }) : P.noCommon;
    }
    pl.querySelector("[data-pl-add]").addEventListener("click", function () {
      var k = selC.value;
      if (k && sel.indexOf(k) < 0 && sel.length < 6) { sel.push(k); render(); }
    });
    dateIn.addEventListener("input", render);
    shareSetup(pl, P);
    render();
    S.track("arac", { arac: "planner" });
  }

  /* ---------- Gömme kodu üreteci ---------- */
  var eg = doc.querySelector("[data-embedgen]");
  if (eg) {
    var E = S.T.eg || {};
    var g = function (n) { return eg.querySelector("[data-eg-" + n + "]"); };
    var fr = g("frame"), codeEl = g("code"), st = g("status");
    var build = function () {
      var qs = new URLSearchParams();
      qs.set("m", g("model").value);
      if (g("city").value) qs.set("c", g("city").value);
      if (g("theme").value !== "auto") qs.set("theme", g("theme").value);
      if (!g("sec").checked) qs.set("sec", "0");
      if (g("h12").checked) qs.set("h12", "1");
      var w = Math.min(1200, Math.max(160, parseInt(g("w").value, 10) || 360)), h = Math.min(1200, Math.max(160, parseInt(g("h").value, 10) || 360));
      var rel = E.path[g("lang").value] + "?" + qs.toString();
      fr.setAttribute("width", w); fr.setAttribute("height", h); fr.setAttribute("src", rel);
      codeEl.value = '<iframe src="' + location.origin + rel + '" width="' + w + '" height="' + h + '" style="border:0;max-width:100%" title="' + E.frameTitle + '" loading="lazy"></iframe>';
    };
    var touchedH = false;
    g("h").addEventListener("input", function () { touchedH = true; });
    g("model").addEventListener("change", function () { if (!touchedH) g("h").value = /^calendar/.test(g("model").value) ? 460 : 360; });
    ["model", "city", "theme", "lang", "w", "h", "sec", "h12"].forEach(function (n) { g(n).addEventListener("input", build); g(n).addEventListener("change", build); });
    g("copy").addEventListener("click", function () {
      var done = function () { st.textContent = E.copied; setTimeout(function () { st.textContent = ""; }, 2500); };
      try { navigator.clipboard.writeText(codeEl.value).then(done, function () { codeEl.select(); }); } catch (e) { codeEl.select(); }
    });
    build();
    S.track("arac", { arac: "embed" });
  }
})();
