/* Tarihte bugün (30 Ekim 2026): bugünün (cihazın tarihi) ya da ?d=AA-GG gününün kayıtları; ay dosyası bir kez yüklenir.
   Veri Wikidata (CC0), derlemede ay başına dosyalara bölünür. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;
  var root = doc.querySelector("[data-otd]");
  if (!root) return;
  var X = S.T.ot || {}, CAL = S.cfg.cal || {};
  var list = root.querySelector("[data-otd-list]"), title = root.querySelector("[data-otd-title]"), input = root.querySelector("[data-otd-day]");
  var cache = {}, cur = null;
  var pad = function (n) { return String(n).padStart(2, "0"); };
  function render(md, days) {
    var rows = days[md] || [];
    list.textContent = "";
    [["e", X.events], ["f", X.founded], ["b", X.births], ["d", X.deaths]].forEach(function (g) {
      var items = rows.filter(function (r) { return r[1] === g[0]; });
      if (!items.length) return;
      var h = doc.createElement("h3"); h.textContent = g[1]; list.appendChild(h);
      var ul = doc.createElement("ul"); ul.className = "otd-list";
      items.forEach(function (r) {
        var li = doc.createElement("li"), y = doc.createElement("span"), tx = doc.createElement("span");
        y.className = "otd-y"; y.dir = "ltr"; y.textContent = r[0];
        tx.textContent = g[0] === "f" ? S.tpl(X.foundedFmt, { name: r[2] }) : r[2];
        if (r[3]) { var ds = doc.createElement("span"); ds.className = "meta"; ds.textContent = " (" + r[3] + ")"; tx.appendChild(ds); }
        li.appendChild(y); li.appendChild(doc.createTextNode(" ")); li.appendChild(tx); ul.appendChild(li);
      });
      list.appendChild(ul);
    });
    if (!list.firstChild) { var p = doc.createElement("p"); p.className = "meta"; p.textContent = X.empty; list.appendChild(p); }
  }
  function show(m, d, push) {
    var md = pad(m) + "-" + pad(d);
    cur = { m: m, d: d };
    title.textContent = S.tpl(CAL.shortFmt || "{day} {month}", { day: d, month: (CAL.months || [])[m - 1] });
    var y = S.parts(new Date(S.nowMs()), S.localTz).year;
    input.value = y + "-" + md;
    if (push) { try { history.replaceState(null, "", "?d=" + md); } catch (e) {} }
    var mm = pad(m);
    if (cache[mm]) return render(md, cache[mm]);
    fetch((X.urls || {})[mm]).then(function (r) { return r.json(); }).then(function (j) { cache[mm] = j; if (cur.m === m && cur.d === d) render(md, j); }).catch(function () {});
  }
  function step(n) { var t = new Date(Date.UTC(2024, cur.m - 1, cur.d + n)); show(t.getUTCMonth() + 1, t.getUTCDate(), true); }
  root.querySelector("[data-otd-prev]").addEventListener("click", function () { step(-1); });
  root.querySelector("[data-otd-next]").addEventListener("click", function () { step(1); });
  root.querySelector("[data-otd-today]").addEventListener("click", function () { var p = S.parts(new Date(S.nowMs()), S.localTz); show(p.month, p.day, true); });
  input.addEventListener("change", function () { var v = (input.value || "").split("-"); if (v.length === 3) show(+v[1], +v[2], true); });
  var q = (location.search.match(/[?&]d=(\d\d)-(\d\d)/) || []);
  if (q.length) show(+q[1], +q[2], false);
  else { var p = S.parts(new Date(S.nowMs()), S.localTz); show(p.month, p.day, false); }
})();
