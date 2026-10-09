/* Tarih araçları: hafta numarası bulucu ve hafta tablosunda geçerli haftanın işaretlenmesi. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;

  function isoWeekOf(y, m, d) {
    var t = new Date(Date.UTC(y, m - 1, d));
    var day = t.getUTCDay() || 7;
    t.setUTCDate(t.getUTCDate() + 4 - day);
    var y0 = Date.UTC(t.getUTCFullYear(), 0, 1);
    return { week: Math.ceil(((t - y0) / 86400000 + 1) / 7), year: t.getUTCFullYear() };
  }

  var find = doc.querySelector("[data-weekfind]");
  if (find) {
    var out = doc.querySelector("[data-weekfind-out]");
    var df = new Intl.DateTimeFormat(S.locale, { day: "numeric", month: "long", year: "numeric", weekday: "long", timeZone: "UTC" });
    var show = function () {
      var m = /^(\d{4})-(\d\d)-(\d\d)$/.exec(find.value);
      if (!m) { out.textContent = S.T.pickDate; return; }
      var r = isoWeekOf(+m[1], +m[2], +m[3]);
      out.textContent = S.tpl(S.T.weekFound, { date: df.format(new Date(Date.UTC(+m[1], +m[2] - 1, +m[3], 12))), n: r.week, y: r.year });
    };
    find.addEventListener("input", show);
  }

  var rows = doc.querySelectorAll("tr[data-wk]");
  if (rows.length) {
    var n = S.nowDate();
    var cur = isoWeekOf(n.getFullYear(), n.getMonth() + 1, n.getDate());
    rows.forEach(function (tr) { if (+tr.getAttribute("data-wk") === cur.week) { tr.classList.add("is-next"); tr.setAttribute("aria-current", "date"); } });
  }

  /* ---------- Tarih hesaplayıcılar ---------- */
  var dcRoots = doc.querySelectorAll("[data-dc]");
  if (dcRoots.length) {
    var D = S.T.dc || {}, R = S.T.dcRange || {};
    var p2 = function (n) { return String(n).padStart(2, "0"); };
    var parse = function (s) { var m = /^(\d{4})-(\d\d)-(\d\d)$/.exec(s || ""); return m ? { y: +m[1], m: +m[2], d: +m[3] } : null; };
    var ms = function (o) { return Date.UTC(o.y, o.m - 1, o.d); };
    var from = function (t) { var x = new Date(t); return { y: x.getUTCFullYear(), m: x.getUTCMonth() + 1, d: x.getUTCDate() }; };
    var dim = function (y, m) { return new Date(Date.UTC(y, m, 0)).getUTCDate(); };
    var iso = function (o) { return o.y + "-" + p2(o.m) + "-" + p2(o.d); };
    var df = new Intl.DateTimeFormat(S.locale, { weekday: "long", day: "numeric", month: "long", year: "numeric", timeZone: "UTC" });
    var nf = new Intl.NumberFormat(S.locale, { maximumFractionDigits: 1 });
    var fmtDate = function (o) { return df.format(new Date(Date.UTC(o.y, o.m - 1, o.d, 12))); };
    var dow = function (o) { return (new Date(ms(o)).getUTCDay() + 6) % 7; };
    var hol = {};
    var ready = null;
    var rangeEl = doc.querySelector("[data-dc-range]");
    if (rangeEl && R.from) rangeEl.textContent = S.tpl(D.range, { from: R.from, to: R.to });
    var load = function () {
      if (ready) return ready;
      ready = fetch(S.cfg.eventsUrl, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (list) {
        list.forEach(function (e) { if (e.t === "holiday") hol[e.d] = 0; else if (e.t === "half" && hol[e.d] === undefined) hol[e.d] = 0.5; });
      }).catch(function () {});
      return ready;
    };
    var weight = function (o) { if (dow(o) >= 5) return 0; var h = hol[iso(o)]; return h === undefined ? 1 : h; };
    var line = function (box, texts) {
      box.textContent = "";
      texts.forEach(function (t, i) { var p = doc.createElement("p"); p.textContent = t; if (i === 0) p.className = "dc-main"; box.appendChild(p); });
    };
    var ymd = function (a, b) {
      var y = b.y - a.y, m = b.m - a.m, d = b.d - a.d;
      if (d < 0) { m--; d += dim(b.m === 1 ? b.y - 1 : b.y, b.m === 1 ? 12 : b.m - 1); }
      if (m < 0) { y--; m += 12; }
      return { y: y, m: m, d: d };
    };
    var addMonths = function (o, n) {
      var t = o.y * 12 + (o.m - 1) + n, y = Math.floor(t / 12), m = (t % 12 + 12) % 12 + 1;
      return { y: y, m: m, d: Math.min(o.d, dim(y, m)) };
    };

    dcRoots.forEach(function (root) {
      var kind = root.getAttribute("data-dc"), out = root.querySelector("[data-dc-out]");
      var update;
      if (kind === "diff") {
        var s1 = root.querySelector("[data-dc-start]"), e1 = root.querySelector("[data-dc-end]");
        update = function () {
          var a = parse(s1.value), b = parse(e1.value);
          if (!a || !b) { out.textContent = D.pick; return; }
          if (ms(a) > ms(b)) { var x = a; a = b; b = x; }
          load().then(function () {
            var n = Math.round((ms(b) - ms(a)) / 86400000), w = 0;
            for (var t = ms(a); t < ms(b); t += 86400000) w += weight(from(t));
            var r = ymd(a, b);
            line(out, [S.tpl(D.diffDays, { n: new Intl.NumberFormat(S.locale).format(n) }), S.tpl(D.diffWeeks, { w: Math.floor(n / 7), d: n % 7 }), S.tpl(D.diffYmd, r), S.tpl(D.diffWork, { n: nf.format(w) })]);
          });
        };
        [s1, e1].forEach(function (i) { i.addEventListener("input", update); });
      } else if (kind === "add") {
        var s2 = root.querySelector("[data-dc-start]"), am = root.querySelector("[data-dc-amount]"), un = root.querySelector("[data-dc-unit]");
        update = function () {
          var a = parse(s2.value), n = parseInt(am.value, 10);
          if (!a || isNaN(n)) { out.textContent = D.pick; return; }
          load().then(function () {
            var r;
            switch (un.value) {
              case "days": r = from(ms(a) + n * 86400000); break;
              case "weeks": r = from(ms(a) + n * 7 * 86400000); break;
              case "months": r = addMonths(a, n); break;
              case "years": r = addMonths(a, n * 12); break;
              default:
                var cur = ms(a), step = n < 0 ? -1 : 1, left = Math.abs(n);
                while (left > 0) { cur += step * 86400000; if (weight(from(cur)) > 0) left--; }
                r = from(cur);
            }
            line(out, [S.tpl(D.addResult, { date: fmtDate(r) })]);
          });
        };
        [s2, am, un].forEach(function (i) { i.addEventListener("input", update); i.addEventListener("change", update); });
      } else if (kind === "age") {
        var bi = root.querySelector("[data-dc-birth]");
        update = function () {
          var b = parse(bi.value), now = S.nowDate();
          var t = { y: now.getFullYear(), m: now.getMonth() + 1, d: now.getDate() };
          if (!b || ms(b) > ms(t)) { out.textContent = b ? D.invalid : D.pick; return; }
          var r = ymd(b, t), days = Math.round((ms(t) - ms(b)) / 86400000);
          var next = { y: t.y, m: b.m, d: Math.min(b.d, dim(t.y, b.m)) };
          if (ms(next) < ms(t)) next = { y: t.y + 1, m: b.m, d: Math.min(b.d, dim(t.y + 1, b.m)) };
          var left = Math.round((ms(next) - ms(t)) / 86400000);
          line(out, [S.tpl(D.ageResult, r), S.tpl(D.ageDays, { n: new Intl.NumberFormat(S.locale).format(days) }), left === 0 ? D.ageToday : S.tpl(D.ageNext, { date: fmtDate(next), n: left })]);
        };
        bi.addEventListener("input", update);
      }
    });
    S.track("arac", { arac: "datecalc" });
  }
})();
