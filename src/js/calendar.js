/* Takvim: bugünü işaretler, kalan günleri yazar, güne dokununca pencere açar.
   Tarihler sunucu-senkron saatle (window.sth.nowDate) ve cihazın saat dilimiyle hesaplanır. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;
  // Yazdır / PDF olarak kaydet (30 Ekim 2026): tarayıcının yazdırma penceresi; düzen styles.css @media print
  doc.addEventListener("click", function (e) { if (e.target.closest && e.target.closest("[data-print]")) window.print(); });

  function dayStart(d) { return new Date(d.getFullYear(), d.getMonth(), d.getDate()); }
  function parse(s) { var p = s.split("-"); return new Date(+p[0], +p[1] - 1, +p[2]); }
  function daysUntil(s) { return Math.round((parse(s) - dayStart(S.nowDate())) / 86400000); }
  function leftText(n) {
    if (n === 0) return S.T.today;
    if (n === 1) return S.T.tomorrow;
    if (n < 0) return S.T.passed;
    return S.tpl(S.T.daysLeft, { n: new Intl.NumberFormat(S.locale).format(n) });
  }

  /* Sınav tarihleri 9 Kasım 2026'da kaldırıldı (sahibin kararı: yalnız Türkiye'ye özgü, projenin küresel amacına aykırı); eski seçim silinir */
  try { localStorage.removeItem("sth-cal-exams"); } catch (e) {}

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

    /* Kapsayıcı takvimler (9 Kasım 2026; lib/calendars, docs/takvimler.md). Seçim sth-cals: açık takvim kimlikleri virgülle ("" = hiçbiri).
       Kayıt yoksa varsayılan: dini olmayan (group != religious) ve bölgesi tarayıcının bölgesine uyan takvimler; dini takvimler hep kapalı
       başlar. Açık takvimlerin olayları aylık tablolarda renk noktası (ad başlıkta ve ekran okuyucuda), gün penceresinde ad; ay tablosu,
       günlük kodu ya da yıl etiketi olan takvimlerde gün penceresinde bir satır. Veri assets/cals-<dil>-<yıl>.json, yalnız bir takvim açıkken. */
    var CX = S.cfg.cal && S.cfg.cal.cals, pick = doc.querySelector("[data-cals]"), CK = "sth-cals";
    var calById = {}, calOn = [], calData = null, calWait = null;
    /* Dilin ana ülkesi: tarayıcı dilinde bölge yoksa (ör. "de") */
    var LANG_REGION = { tr: "TR", en: "US", de: "DE", az: "AZ", ar: "SA", id: "ID", ur: "PK", fa: "IR", fr: "FR", ru: "RU", ja: "JP", ko: "KR", zh: "CN", fil: "PH", th: "TH", es: "ES", pt: "BR", hi: "IN", bn: "BD" };
    var userRegion = function () {
      var tag = String(navigator.language || "").split(/[-_]/);
      for (var i = 1; i < tag.length; i++) if (/^[A-Za-z]{2}$/.test(tag[i])) return tag[i].toUpperCase();
      return LANG_REGION[String(tag[0]).toLowerCase()] || LANG_REGION[S.lang] || "";
    };
    var calsRead = function () {
      var v = S.store(CK), list = CX.list;
      if (v !== null) { var ids = v.split(","); return list.filter(function (c) { return ids.indexOf(c.id) >= 0; }).map(function (c) { return c.id; }); }
      var r = userRegion();
      return list.filter(function (c) { return c.group !== "religious" && c.regions.indexOf(r) >= 0; }).map(function (c) { return c.id; });
    };
    var calsWrite = function () {
      var old = S.store(CK), keep = old ? old.split(",").filter(function (x) { return x && !calById[x]; }) : [];
      S.store(CK, keep.concat(calOn).join(","));
    };
    var withCals = function (cb) {
      if (!calOn.length || calData) return cb();
      if (calWait) { calWait.push(cb); return; }
      calWait = [cb];
      fetch(CX.url, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (j) { calData = j; }, function () { calData = {}; })
        .then(function () { var q = calWait; calWait = null; q.forEach(function (f) { f(); }); });
    };
    var dnum = function (s) { var p = s.split("-"); return Date.UTC(+p[0], +p[1] - 1, +p[2]) / 86400000; };
    var nextDay = function (s) { var t = new Date((dnum(s) + 1) * 86400000); return t.getUTCFullYear() + "-" + String(t.getUTCMonth() + 1).padStart(2, "0") + "-" + String(t.getUTCDate()).padStart(2, "0"); };
    /* İkinci tarih: ay tablosunda günün düştüğü satır (son satır tablonun sonu) */
    var secondDate = function (rows, d) {
      if (!rows || !rows.length || d < rows[0][0] || d >= rows[rows.length - 1][0]) return null;
      var r = rows[0];
      for (var i = 1; i < rows.length && rows[i][0] <= d; i++) r = rows[i];
      return { y: r[1], m: r[2], d: dnum(d) - dnum(r[0]) + 1, leap: !!r[3] };
    };
    /* Günlük kod: önce bütün kod (daily.<kod>), yoksa harf+rakam parçaları ("t5n12" → daily.t5 · daily.n12) */
    var dailyText = function (code, names) {
      if (names[code]) return names[code];
      var parts = String(code).match(/[a-z]+[0-9]+/gi) || [];
      return parts.length ? parts.map(function (k) { return names[k] || k; }).join(" · ") : code;
    };
    /* Bir günün açık takvim olayları ve satırları */
    var calDay = function (d) {
      var evs = [], lines = [];
      calOn.forEach(function (id) {
        var c = calData && calData[id], meta = calById[id];
        if (!c || !meta) return;
        c.events.forEach(function (e) { if (e[0] <= d && d <= (e[2] || e[0])) evs.push({ id: id, color: meta.color, text: c.ev[e[1]] || e[1], cal: c.name }); });
        var parts = [], sd = c.months && secondDate(c.months, d);
        if (sd) {
          var mn = (c.month && c.month[sd.m - 1]) || String(sd.m);
          if (sd.leap && c.leapFmt) mn = S.tpl(c.leapFmt, { m: mn });
          parts.push(S.tpl(c.fmt, { d: sd.d, m: mn, y: sd.y }));
        }
        if (c.daily && c.daily[d]) parts.push(dailyText(c.daily[d], c.dailyNames || {}));
        if (c.year != null && c.yearFmt) parts.push(S.tpl(c.yearFmt, { y: c.year }));
        if (parts.length) lines.push({ id: id, color: meta.color, text: c.name + ": " + parts.join(" · ") });
      });
      return { evs: evs, lines: lines };
    };
    var dot = function (color, cls) { var s = doc.createElement("span"); s.className = cls || "cal-dot"; s.setAttribute("aria-hidden", "true"); s.style.setProperty("--calc", color); return s; };
    /* Aylık tablolar, gösterge ve notlar: önceki işaretler silinir, açık takvimlerinki yeniden eklenir */
    var calNotes = doc.querySelector("[data-cal-notes]"), legendEl = doc.querySelector(".legend");
    var decorate = function () {
      doc.querySelectorAll(".cal-x").forEach(function (n) { n.remove(); });
      grid.querySelectorAll("td[data-cal-title]").forEach(function (td) { var t0 = td.getAttribute("data-cal-title"); td.setAttribute("title", t0); td.removeAttribute("data-cal-title"); });
      if (calNotes) calNotes.hidden = true;
      if (!calData) return;
      var byDate = {};
      calOn.forEach(function (id) {
        var c = calData[id], meta = calById[id];
        if (!c || !meta) return;
        c.events.forEach(function (e) {
          for (var d = e[0], end = e[2] || e[0], k = 0; k < 62; k++, d = nextDay(d)) {
            (byDate[d] = byDate[d] || []).push({ id: id, color: meta.color, text: c.ev[e[1]] || e[1] });
            if (d >= end) break;
          }
        });
        if (legendEl) { var li = doc.createElement("li"); li.className = "cal-x"; li.appendChild(dot(meta.color, "lg lg-cal")); li.appendChild(doc.createTextNode(c.name)); legendEl.appendChild(li); }
        if (calNotes && c.note) { var p = doc.createElement("p"); p.className = "meta cal-x"; p.textContent = c.name + ": " + c.note; calNotes.appendChild(p); calNotes.hidden = false; }
      });
      Object.keys(byDate).forEach(function (d) {
        var td = grid.querySelector('td[data-d="' + d + '"]');
        if (!td) return;
        var list = byDate[d], box = doc.createElement("span"), seen = {};
        box.className = "cal-dots cal-x"; box.setAttribute("aria-hidden", "true");
        list.forEach(function (x) { if (!seen[x.id]) { seen[x.id] = 1; box.appendChild(dot(x.color)); } });
        td.appendChild(box);
        var names = list.map(function (x) { return x.text; }).join("; ");
        var sr = doc.createElement("span"); sr.className = "sr-only cal-x"; sr.textContent = ", " + names; td.appendChild(sr);
        var t0 = td.getAttribute("title") || "";
        td.setAttribute("data-cal-title", t0);
        td.setAttribute("title", t0 && t0 !== S.T.cald.none ? t0 + "; " + names : names);
      });
    };
    if (CX && pick) {
      CX.list.forEach(function (c) { calById[c.id] = c; });
      calOn = calsRead();
      pick.querySelectorAll("[data-cal-sw]").forEach(function (sw) { var c = calById[sw.getAttribute("data-cal-sw")]; if (c) sw.style.setProperty("--calc", c.color); });
      var boxes = pick.querySelectorAll("input[data-cal]");
      boxes.forEach(function (inp) {
        inp.checked = calOn.indexOf(inp.getAttribute("data-cal")) >= 0;
        inp.addEventListener("change", function () {
          calOn = [];
          boxes.forEach(function (b) { if (b.checked && calById[b.getAttribute("data-cal")]) calOn.push(b.getAttribute("data-cal")); });
          calsWrite();
          withCals(decorate);
        });
      });
      pick.hidden = false;
      if (calOn.length) withCals(decorate);
    }

    /* Güne dokununca açılan pencere (özel günler, hafta numarası, kalan gün); günler listesine bağlanır */
    var dlg = doc.getElementById("day-dialog"), C = S.T.cald;
    if (dlg && C) {
      var evMap = null, selected = null;
      var loadEvents = function (cb) {
        if (evMap) return cb();
        fetch(S.cfg.eventsUrl, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (list) {
          evMap = {}; list.forEach(function (e) { (evMap[e.d] = evMap[e.d] || []).push(e); }); cb();
        }).catch(function () { evMap = {}; cb(); });
      };
      var closeDlg = function () { if (typeof dlg.close === "function") dlg.close(); else dlg.removeAttribute("open"); };
      var link = dlg.querySelector("[data-day-link]");
      var show = function (td) {
        var d = td.getAttribute("data-d"), m = /^(\d{4})-(\d\d)-(\d\d)$/.exec(d);
        if (!m) return;
        var pp = { year: +m[1], month: +m[2], day: +m[3] };
        loadEvents(function () { withCals(function () {
          if (selected) selected.classList.remove("d-selected");
          selected = td; td.classList.add("d-selected");
          dlg.querySelector("[data-day-title]").textContent = S.longDate(pp);
          var ul = dlg.querySelector("[data-day-list]"); ul.textContent = "";
          var seen = {};
          (evMap[d] || []).forEach(function (e) {
            var li = doc.createElement("li"); li.textContent = e.n + " (" + (C.types[e.t] || e.t) + ")"; ul.appendChild(li); seen[e.t] = 1;
          });
          [["d-ramadan", "ramadan"], ["d-school", "school"]].forEach(function (c) {
            if (td.classList.contains(c[0]) && !seen[c[1]] && C.types[c[1]]) { var li = doc.createElement("li"); li.textContent = C.types[c[1]]; ul.appendChild(li); }
          });
          var cd = calDay(d), cl = dlg.querySelector("[data-day-cals]");
          cd.evs.forEach(function (x) { var li = doc.createElement("li"); li.className = "cal-ev"; li.appendChild(dot(x.color)); li.appendChild(doc.createTextNode(x.text + " (" + x.cal + ")")); ul.appendChild(li); });
          if (cl) {
            cl.textContent = "";
            cd.lines.forEach(function (x) { var li = doc.createElement("li"); li.appendChild(dot(x.color)); li.appendChild(doc.createTextNode(x.text)); cl.appendChild(li); });
            cl.hidden = !cd.lines.length;
          }
          if (!ul.children.length) { var li0 = doc.createElement("li"); li0.textContent = C.none; ul.appendChild(li0); }
          var n = daysUntil(d);
          var doy = Math.round((Date.UTC(pp.year, pp.month - 1, pp.day) - Date.UTC(pp.year, 0, 0)) / 86400000);
          dlg.querySelector("[data-day-meta]").textContent = [S.tpl(C.week, { n: S.isoWeek(pp) }), S.tpl(C.doy, { n: doy }), n === 0 ? C.today : n > 0 ? S.tpl(C.inDays, { n: n }) : S.tpl(C.ago, { n: -n })].join(" · ");
          var row = doc.getElementById("e-" + d);
          link.hidden = !row; link.setAttribute("href", "#e-" + d);
          if (typeof dlg.showModal === "function") { if (!dlg.open) dlg.showModal(); } else dlg.setAttribute("open", "");
        }); });
      };
      dlg.addEventListener("click", function (e) { if (e.target === dlg || (e.target.closest && e.target.closest("[data-day-close]"))) closeDlg(); });
      dlg.addEventListener("close", function () { if (selected) { selected.classList.remove("d-selected"); selected = null; } });
      link.addEventListener("click", function (e) {
        var row = doc.getElementById(link.getAttribute("href").slice(1));
        e.preventDefault(); closeDlg();
        if (!row) return;
        doc.querySelectorAll(".events-table tr.is-hit").forEach(function (r) { r.classList.remove("is-hit"); });
        row.classList.add("is-hit");
        row.scrollIntoView({ block: "center", behavior: "smooth" });
      });
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
