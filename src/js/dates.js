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
})();
