/* Emeklilik hesaplama (30 Ekim 2026): 4/a (hizmet akdiyle çalışan, eski SSK) sigortalıları için en erken emeklilik tarihi.
   Kurallar kanun metinlerinden (mevzuat.gov.tr, 30 Ekim 2026):
   - 8.9.1999 öncesi: 506 sayılı Kanun geçici 81 (A: 1999'da kadın 18, erkek 23 yılını dolduranlar eski hükümler;
     B: 23.5.2002'deki sigortalılık süresine göre 5000–5975 gün, kadın 20, erkek 25 yıl), yaş şartı 5510 geçici 95 (EYT)
     ile kalktı; C: 15 yıl + 3600 gün + kadın 58, erkek 60 yaş.
   - 8.9.1999–30.4.2008: 5510 geçici 9: kadın 58, erkek 60 yaş ve 7000 gün; ya da aynı yaş + 25 yıl + 4500 gün.
   - 1.5.2008 sonrası: 5510 m.28: 7200 gün + yaş (gün şartının dolduğu tarihe göre 2036'dan itibaren yükselir);
     5400 gün + yaş + 3 (en çok 65).
   Gelecek prim günleri yılda seçilen gün kadar düzenli varsayılır. Girdiler kaydedilmez, gönderilmez. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;
  var root = doc.querySelector("[data-retire]");
  if (!root) return;
  var X = S.T.rt || {};
  var q = function (s) { return root.querySelector(s); };
  var DAY = 86400000;

  var parse = function (v) { var m = /^(\d{4})-(\d\d)-(\d\d)$/.exec(v || ""); return m ? Date.UTC(+m[1], +m[2] - 1, +m[3]) : null; };
  function addYMD(t, y, mo, d) { var x = new Date(t); return Date.UTC(x.getUTCFullYear() + y, x.getUTCMonth() + (mo || 0), x.getUTCDate() + (d || 0)); }
  var fmt = function (t) { return S.fmt("rt", "UTC", { day: "numeric", month: "long", year: "numeric" }).format(new Date(t)); };

  /* 506 geçici 81/B: 23.5.2002'deki sigortalılık süresi (yıl, ay, gün; alt sınır dahil) → gereken prim günü */
  var B_WOMEN = [[18, 0, 0, 5000], [17, 0, 0, 5000], [16, 0, 0, 5075], [15, 0, 0, 5150], [14, 0, 0, 5225], [13, 0, 0, 5300], [12, 0, 0, 5375],
    [11, 0, 0, 5450], [10, 0, 0, 5525], [9, 0, 0, 5600], [8, 0, 0, 5675], [7, 0, 0, 5750], [6, 0, 0, 5825], [5, 0, 0, 5900], [4, 0, 0, 5975], [3, 0, 0, 5975], [2, 8, 15, 5975]];
  var B_MEN = [[23, 0, 0, 5000], [21, 6, 0, 5000], [20, 0, 0, 5075], [18, 6, 0, 5150], [17, 0, 0, 5225], [15, 6, 0, 5300], [14, 0, 0, 5375],
    [12, 6, 0, 5450], [11, 0, 0, 5525], [9, 6, 0, 5600], [8, 0, 0, 5675], [6, 6, 0, 5750], [5, 0, 0, 5825], [3, 6, 0, 5900], [2, 8, 15, 5975]];
  var REF_B = Date.UTC(2002, 4, 23), LAW_1999 = Date.UTC(1999, 8, 8), LAW_2008 = Date.UTC(2008, 4, 1);

  /* 5510 m.28/b: gün şartının dolduğu tarihe göre yaş [kadın, erkek] */
  function postAge(t, female) {
    var y = new Date(t).getUTCFullYear();
    var tbl = [[2036, 58, 60], [2038, 59, 61], [2040, 60, 62], [2042, 61, 63], [2044, 62, 64], [2046, 63, 65], [2048, 64, 65], [99999, 65, 65]];
    for (var i = 0; i < tbl.length; i++) if (y < tbl[i][0]) return female ? tbl[i][1] : tbl[i][2];
    return 65;
  }

  function calc(inp) {
    var f = inp.female, today = inp.today;
    var dayDate = function (need) { // gün şartının dolacağı tarih (null: dolmuyor)
      if (inp.days >= need) return today;
      if (!inp.rate) return null;
      return today + Math.ceil((need - inp.days) * 365.25 / inp.rate) * DAY;
    };
    var route = function (key, conds) { // conds: [{ label, at }], at null = olmuyor
      var at = 0;
      for (var i = 0; i < conds.length; i++) { if (conds[i].at === null) { at = null; break; } at = Math.max(at, conds[i].at); }
      return { key: key, conds: conds, at: at };
    };
    var cDays = function (n) { return { label: S.tpl(X.condDays, { n: n.toLocaleString("tr-TR") }), at: dayDate(n) }; };
    var cYears = function (n) { return { label: S.tpl(X.condYears, { n: n }), at: addYMD(inp.start, n, 0, 0) }; };
    var cAge = function (n) { return { label: S.tpl(X.condAge, { n: n }), at: addYMD(inp.birth, n, 0, 0) }; };
    var group, routes = [];
    if (inp.start < LAW_1999) {
      var old = f ? inp.start <= addYMD(LAW_1999, -18, 0, 0) : inp.start <= addYMD(LAW_1999, -23, 0, 0);
      var need = 5975;
      if (old) need = 5000;
      else {
        var tbl = f ? B_WOMEN : B_MEN;
        for (var i = 0; i < tbl.length; i++) if (inp.start <= addYMD(REF_B, -tbl[i][0], -tbl[i][1], -tbl[i][2])) { need = tbl[i][3]; break; }
      }
      group = old ? "a" : "b";
      routes.push(route("full", [cDays(need), cYears(f ? 20 : 25), { label: X.condNoAge, at: inp.start }]));
      // 81/A eski hükümler (506 m.60): 15 yıl + 3600 gün + kadın 50, erkek 55; 81/C: kadın 58, erkek 60
      routes.push(route("partialPre", [cDays(3600), cYears(15), cAge(old ? (f ? 50 : 55) : f ? 58 : 60)]));
    } else if (inp.start < LAW_2008) {
      group = "mid";
      routes.push(route("mid1", [cDays(7000), cAge(f ? 58 : 60)]));
      routes.push(route("mid2", [cDays(4500), cYears(25), cAge(f ? 58 : 60)]));
    } else {
      group = "post";
      var d72 = dayDate(7200), d54 = dayDate(5400);
      var a1 = d72 === null ? (f ? 58 : 60) : postAge(d72, f), a2 = Math.min(65, (d54 === null ? (f ? 58 : 60) : postAge(d54, f)) + 3);
      routes.push(route("full", [cDays(7200), cAge(a1)]));
      routes.push(route("partialPost", [cDays(5400), cAge(a2)]));
    }
    var best = null;
    routes.forEach(function (r) { if (r.at !== null && (!best || r.at < best.at)) best = r; });
    return { group: group, routes: routes, best: best };
  }

  function diffText(a, b) {
    var x = new Date(a), y = new Date(b);
    var m = (y.getUTCFullYear() - x.getUTCFullYear()) * 12 + (y.getUTCMonth() - x.getUTCMonth());
    if (y.getUTCDate() < x.getUTCDate()) m--;
    var parts = [];
    if (m >= 12) parts.push(S.tpl(X.years, { n: Math.floor(m / 12) }));
    if (m % 12) parts.push(S.tpl(X.months, { n: m % 12 }));
    return parts.join(" ") || S.tpl(X.months, { n: 0 });
  }

  function show(r, inp) {
    var out = q("[data-rt-out]"); out.hidden = false;
    if (!r.best) { q("[data-rt-date]").textContent = X.resultNever; q("[data-rt-age]").textContent = ""; }
    else {
      q("[data-rt-date]").textContent = S.tpl(X.result, { date: fmt(r.best.at) });
      var age = new Date(r.best.at).getUTCFullYear() - new Date(inp.birth).getUTCFullYear();
      if (addYMD(inp.birth, age, 0, 0) > r.best.at) age--;
      q("[data-rt-age]").textContent = S.tpl(X.resultAge, { age: age }) + " " + (r.best.at <= inp.today ? X.resultPast : S.tpl(X.resultLeft, { left: diffText(inp.today, r.best.at) }));
    }
    q("[data-rt-group]").textContent = S.tpl(X.group, { name: (X.groups || {})[r.group] || "" });
    var body = q("[data-rt-rows]"); body.textContent = "";
    r.routes.forEach(function (rt) {
      rt.conds.forEach(function (c, i) {
        var tr = doc.createElement("tr");
        if (rt === r.best) tr.className = "is-next";
        var cells = [i ? "" : (X.routes || {})[rt.key] || rt.key, c.label, c.at === null ? X.never : c.at <= inp.today ? X.done : fmt(c.at), i ? "" : rt.at === null ? X.never : fmt(Math.max(rt.at, inp.today))];
        cells.forEach(function (v, j) { var td = doc.createElement(j ? "td" : "th"); if (!j) td.scope = "row"; td.textContent = v; tr.appendChild(td); });
        body.appendChild(tr);
      });
    });
  }

  q("[data-rt-form]").addEventListener("submit", function (e) {
    e.preventDefault();
    var birth = parse(q("[data-rt-birth]").value), start = parse(q("[data-rt-start]").value), days = parseInt(q("[data-rt-days]").value, 10);
    if (birth === null || start === null || !(days >= 0) || start < birth) { q("[data-rt-form]").reportValidity(); return; }
    var p = S.parts(new Date(S.nowMs()), "Europe/Istanbul");
    var inp = { female: root.querySelector('input[name="g"]:checked').value === "f", birth: birth, start: start, days: days, rate: +q("[data-rt-rate]").value, today: Date.UTC(p.year, p.month - 1, p.day) };
    show(calc(inp), inp);
    S.track("arac", { arac: "emeklilik" });
  });
  window.sthRetire = { calc: calc }; // denetim için
})();
