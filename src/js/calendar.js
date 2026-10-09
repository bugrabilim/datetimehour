/* Takvim: bugünü işaretler, kalan günleri yazar, ana sayfada yaklaşan günleri listeler.
   Tarihler sunucu-senkron saatle (window.sth.nowDate) ve cihazın saat dilimiyle hesaplanır. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;

  function dayStart(d) { return new Date(d.getFullYear(), d.getMonth(), d.getDate()); }
  function parse(s) { var p = s.split("-"); return new Date(+p[0], +p[1] - 1, +p[2]); }
  function daysUntil(s) { return Math.round((parse(s) - dayStart(S.nowDate())) / 86400000); }
  function leftText(n) {
    if (n === 0) return S.T.today;
    if (n === 1) return S.T.tomorrow;
    if (n < 0) return S.T.passed;
    return S.tpl(S.T.daysLeft, { n: new Intl.NumberFormat(S.locale).format(n) });
  }

  /* Yıl sayfası */
  var grid = doc.querySelector("[data-year]");
  if (grid) {
    var pageYear = +grid.getAttribute("data-year");
    var now = S.nowDate();
    // /takvim/ giriş sayfası: cihazın yıluna git (veri varsa)
    var p = location.pathname.replace(/\/+$/, "");
    if (/\/(takvim|calendar)$/.test(p) && now.getFullYear() !== pageYear) {
      var a = doc.querySelectorAll(".year-nav ul a");
      for (var i = 0; i < a.length; i++) if (a[i].textContent.trim() === String(now.getFullYear())) { location.replace(a[i].getAttribute("href") + location.hash); return; }
    }
    var today = now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0") + "-" + String(now.getDate()).padStart(2, "0");
    var cell = grid.querySelector('td[data-d="' + today + '"]');
    if (cell) { cell.classList.add("d-today"); cell.setAttribute("aria-current", "date"); }
    var nextRow = null;
    doc.querySelectorAll(".events-table tbody tr").forEach(function (tr) {
      var d = tr.getAttribute("data-d"), n = daysUntil(d);
      tr.querySelector("[data-left]").textContent = pageYear === now.getFullYear() || n >= 0 ? leftText(n) : "";
      if (n < 0) tr.classList.add("is-past");
      if (!nextRow && n >= 0) nextRow = tr;
    });
    if (nextRow) nextRow.classList.add("is-next");

    /* Güne dokununca ayrıntı paneli (özel günler, hafta numarası, kalan gün) */
    var panel = doc.getElementById("day-detail"), C = S.T.cald;
    if (panel && C) {
      var evMap = null, selected = null;
      var loadEvents = function (cb) {
        if (evMap) return cb();
        fetch(S.cfg.eventsUrl, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (list) {
          evMap = {}; list.forEach(function (e) { (evMap[e.d] = evMap[e.d] || []).push(e); }); cb();
        }).catch(function () { evMap = {}; cb(); });
      };
      var show = function (td) {
        var d = td.getAttribute("data-d"), m = /^(\d{4})-(\d\d)-(\d\d)$/.exec(d);
        if (!m) return;
        var pp = { year: +m[1], month: +m[2], day: +m[3] };
        loadEvents(function () {
          if (selected) selected.classList.remove("d-selected");
          selected = td; td.classList.add("d-selected");
          panel.textContent = "";
          var head = doc.createElement("div"); head.className = "day-detail-head";
          var h = doc.createElement("h3"); h.textContent = S.longDate(pp); head.appendChild(h);
          var x = doc.createElement("button"); x.type = "button"; x.className = "icon-btn"; x.textContent = "✕"; x.setAttribute("aria-label", C.close);
          x.addEventListener("click", function () { panel.hidden = true; if (selected) selected.classList.remove("d-selected"); selected = null; });
          head.appendChild(x); panel.appendChild(head);
          var ul = doc.createElement("ul"), seen = {};
          (evMap[d] || []).forEach(function (e) {
            var li = doc.createElement("li"); li.textContent = e.n + " (" + (C.types[e.t] || e.t) + ")"; ul.appendChild(li); seen[e.t] = 1;
          });
          [["d-ramadan", "ramadan"], ["d-school", "school"], ["d-exam", "exam"]].forEach(function (c) {
            if (td.classList.contains(c[0]) && !seen[c[1]] && C.types[c[1]]) { var li = doc.createElement("li"); li.textContent = C.types[c[1]]; ul.appendChild(li); }
          });
          if (!ul.children.length) { var li0 = doc.createElement("li"); li0.textContent = C.none; ul.appendChild(li0); }
          panel.appendChild(ul);
          var n = daysUntil(d);
          var doy = Math.round((Date.UTC(pp.year, pp.month - 1, pp.day) - Date.UTC(pp.year, 0, 0)) / 86400000);
          var meta = doc.createElement("p"); meta.className = "meta";
          meta.textContent = [S.tpl(C.week, { n: S.isoWeek(pp) }), S.tpl(C.doy, { n: doy }), n === 0 ? C.today : n > 0 ? S.tpl(C.inDays, { n: n }) : S.tpl(C.ago, { n: -n })].join(" · ");
          panel.appendChild(meta);
          panel.hidden = false;
          panel.focus({ preventScroll: true });
          if (panel.scrollIntoView) panel.scrollIntoView({ block: "nearest", behavior: "smooth" });
        });
      };
      grid.addEventListener("click", function (ev) {
        var td = ev.target.closest && ev.target.closest("td[data-d]");
        if (td && grid.contains(td)) show(td);
      });
      grid.addEventListener("keydown", function (ev) {
        if (ev.key !== "Enter" && ev.key !== " ") return;
        var td = ev.target.closest && ev.target.closest("td[data-d]");
        if (td) { ev.preventDefault(); show(td); }
      });
      var want = new URLSearchParams(location.search).get("d");
      if (want) { var wtd = grid.querySelector('td[data-d="' + want + '"]'); if (wtd) show(wtd); }
    }
  }

  /* Ana sayfa: yaklaşan günler */
  var up = doc.querySelector("[data-upcoming]");
  if (up && S.cfg.eventsUrl) {
    fetch(S.cfg.eventsUrl, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (list) {
      var skip = { rb2: 1, rb3: 1, kb2: 1, kb3: 1, kb4: 1 };
      var today = dayStart(S.nowDate()), shown = 0;
      var df = new Intl.DateTimeFormat(S.locale, { weekday: "long", day: "numeric", month: "long" });
      list.forEach(function (e) {
        if (shown >= 6 || skip[e.k] || e.t === "school" || e.t === "exam") return;
        var d = parse(e.d);
        if (d < today) return;
        shown++;
        var li = doc.createElement("li");
        li.className = "up-item up-" + e.t;
        var dt = doc.createElement("span"); dt.className = "up-date"; dt.textContent = df.format(d);
        var nm = doc.createElement("strong"); nm.className = "up-name"; nm.textContent = e.n;
        var lf = doc.createElement("span"); lf.className = "up-left"; lf.textContent = leftText(daysUntil(e.d));
        li.appendChild(nm); li.appendChild(dt); li.appendChild(lf);
        up.appendChild(li);
      });
      if (!shown) { var li = doc.createElement("li"); li.textContent = S.T.upcomingNone || ""; up.appendChild(li); }
    }).catch(function () {});
  }

  /* Hazır geri sayım sayfaları */
  var cdp = doc.querySelector("[data-cdp]");
  if (cdp && S.cfg.eventsUrl) {
    var C = S.T.cdp || {};
    var keys = cdp.getAttribute("data-cdp-keys").split(",");
    var disp = cdp.querySelector("[data-cdp-display]"), dateEl = cdp.querySelector("[data-cdp-date]");
    S.makeFullscreen(cdp, cdp.querySelector("[data-fs]"));
    var long = new Intl.DateTimeFormat(S.locale, { weekday: "long", day: "numeric", month: "long", year: "numeric" });
    fetch(S.cfg.eventsUrl, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (list) {
      var today = dayStart(S.nowDate()), best = null;
      list.forEach(function (e) {
        if (keys.indexOf(e.k) < 0) return;
        var d = parse(e.d);
        if (d >= today && (!best || d < best)) best = d;
      });
      if (!best && keys[0] === "newYear") best = new Date(today.getFullYear() + 1, 0, 1);
      if (!best) { disp.textContent = C.noTarget; disp.classList.add("is-empty"); dateEl.textContent = "–"; return; }
      dateEl.textContent = long.format(best);
      function tick() {
        var rem = best.getTime() - S.nowMs();
        if (rem <= 0) { disp.textContent = C.today; return; }
        var t = Math.floor(rem / 1000), d = Math.floor(t / 86400), h = Math.floor((t % 86400) / 3600), m = Math.floor((t % 3600) / 60), s = t % 60;
        var p2 = function (n) { return String(n).padStart(2, "0"); };
        disp.textContent = (d ? d + " " + S.T.daysShort + " " : "") + p2(h) + ":" + p2(m) + ":" + p2(s);
      }
      tick(); setInterval(tick, 250);
    }).catch(function () { disp.textContent = C.noTarget; disp.classList.add("is-empty"); });
  }
})();
