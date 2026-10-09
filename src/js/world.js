/* Dünya saatleri: kullanıcı kendi şehirlerini seçer, sıralar, istediği saat dilimini ekler.
   Seçim yalnız bu tarayıcıda (localStorage: sth-world) saklanır. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;
  var boxes = Array.prototype.slice.call(doc.querySelectorAll("[data-world]"));
  if (!boxes.length) return;
  var W = S.T.wc || {};
  var cities = S.cfg.cities || [];
  var byKey = {};
  cities.forEach(function (c) { byKey[c.k] = c; });
  var MAX = 30;

  var saved = null;
  try { var raw = JSON.parse(S.store("sth-world") || "null"); if (Array.isArray(raw)) saved = raw.filter(function (x) { return typeof x === "string"; }).slice(0, MAX); } catch (e) {}

  function zoneName(tz) { return tz.split("/").pop().replace(/_/g, " "); }
  function entry(token) {
    if (token.indexOf("tz:") === 0) { var tz = token.slice(3); return { token: token, name: zoneName(tz), tz: tz, country: "–" }; }
    var c = byKey[token];
    return c ? { token: token, name: c.n, tz: c.tz, country: c.c, path: c.p } : null;
  }
  var defaults = boxes.map(function (b) { return Array.prototype.map.call(b.querySelectorAll("[data-world-item]"), function (el) { return el.getAttribute("data-world-item"); }); });
  function current(i) { return saved ? saved : defaults[i]; }

  function el(tag, cls, text) { var e = doc.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  function live(e, kind, tz, extra) { e.setAttribute("data-live", kind); e.setAttribute("data-tz", tz); if (extra) e.setAttribute(extra, ""); return e; }

  function renderBox(i) {
    var box = boxes[i], kind = box.getAttribute("data-world");
    var items = current(i).map(entry).filter(Boolean);
    if (kind === "grid") {
      var ul = box.querySelector("ul"); ul.textContent = "";
      items.forEach(function (it) {
        var li = el("li", "city-card"); li.setAttribute("data-world-item", it.token);
        var wrap = it.path ? el("a") : el("span", "card-static");
        if (it.path) wrap.href = it.path;
        wrap.appendChild(el("span", "city-name", it.name));
        wrap.appendChild(live(el("span", "city-time", "--:--"), "time", it.tz));
        wrap.appendChild(live(el("span", "city-diff", " "), "diff", it.tz));
        li.appendChild(wrap); ul.appendChild(li);
      });
    } else {
      var tb = box.querySelector("tbody"); tb.textContent = "";
      items.forEach(function (it) {
        var tr = el("tr"); tr.setAttribute("data-world-item", it.token);
        var th = el("th"); th.scope = "row";
        if (it.path) { var a = el("a", null, it.name); a.href = it.path; th.appendChild(a); } else th.textContent = it.name;
        tr.appendChild(th); tr.appendChild(el("td", null, it.country));
        tr.appendChild(live(el("td", "num", "--:--:--"), "time", it.tz, "data-sec"));
        tr.appendChild(live(el("td", "num", "UTC"), "offset", it.tz));
        tr.appendChild(live(el("td", null, " "), "diff", it.tz));
        tb.appendChild(tr);
      });
    }
    S.refreshLive();
  }
  function setSaved(list) { saved = list; S.store("sth-world", JSON.stringify(list)); boxes.forEach(function (_, i) { renderBox(i); }); }

  function buildEditor(i) {
    var box = boxes[i];
    var wrap = el("div", "world-edit");
    var openBtn = el("button", "toggle", W.customize); openBtn.type = "button"; openBtn.setAttribute("aria-expanded", "false");
    var panel = el("div", "world-panel"); panel.hidden = true;
    wrap.appendChild(openBtn); wrap.appendChild(panel);
    box.parentNode.insertBefore(wrap, box.nextSibling);

    function option(sel, value, text) { var o = doc.createElement("option"); o.value = value; o.textContent = text; sel.appendChild(o); }
    function draw() {
      panel.textContent = "";
      panel.appendChild(el("p", "meta", W.customizeLead));
      var list = current(i).slice();
      var ul = el("ul", "chip-list");
      if (!list.length) ul.appendChild(el("li", "meta", W.empty));
      list.forEach(function (tok, n) {
        var it = entry(tok); if (!it) return;
        var li = el("li", "chip-item"); li.appendChild(el("span", null, it.name));
        var mk = function (txt, label, fn, dis) { var b = el("button", "toggle", txt); b.type = "button"; b.setAttribute("aria-label", label + ": " + it.name); b.disabled = !!dis; b.addEventListener("click", fn); li.appendChild(b); };
        mk("↑", W.moveUp, function () { var l = list.slice(); l.splice(n - 1, 0, l.splice(n, 1)[0]); setSaved(l); draw(); }, n === 0);
        mk("↓", W.moveDown, function () { var l = list.slice(); l.splice(n + 1, 0, l.splice(n, 1)[0]); setSaved(l); draw(); }, n === list.length - 1);
        mk("✕", W.remove, function () { var l = list.slice(); l.splice(n, 1); setSaved(l); draw(); });
        ul.appendChild(li);
      });
      panel.appendChild(ul);
      var row = el("div", "inputs-row");
      var f1 = el("div", "field"), l1 = el("label", null, W.addCity), s1 = el("select"); l1.setAttribute("for", "wc-city-" + i); s1.id = "wc-city-" + i;
      option(s1, "", W.select);
      cities.slice().sort(function (a, b) { return a.n.localeCompare(b.n, S.locale); }).forEach(function (c) { if (list.indexOf(c.k) < 0) option(s1, c.k, c.n + " (" + c.c + ")"); });
      var add1 = el("button", "btn", W.add); add1.type = "button";
      add1.addEventListener("click", function () { if (s1.value && list.length < MAX) { setSaved(list.concat([s1.value])); draw(); } });
      f1.appendChild(l1); f1.appendChild(s1); row.appendChild(f1); row.appendChild(add1); panel.appendChild(row);
      var zones = [];
      try { zones = Intl.supportedValuesOf("timeZone"); } catch (e) {}
      if (zones.length) {
        var row2 = el("div", "inputs-row"), f2 = el("div", "field"), l2 = el("label", null, W.addZone), s2 = el("select"); l2.setAttribute("for", "wc-zone-" + i); s2.id = "wc-zone-" + i;
        option(s2, "", W.select);
        zones.forEach(function (z) { if (list.indexOf("tz:" + z) < 0) option(s2, z, z.replace(/_/g, " ")); });
        var add2 = el("button", "btn", W.add); add2.type = "button";
        add2.addEventListener("click", function () { if (s2.value && list.length < MAX) { setSaved(list.concat(["tz:" + s2.value])); draw(); } });
        f2.appendChild(l2); f2.appendChild(s2); row2.appendChild(f2); row2.appendChild(add2); panel.appendChild(row2);
      }
      var acts = el("div", "tool-actions");
      var rs = el("button", "toggle", W.reset); rs.type = "button";
      rs.addEventListener("click", function () { saved = null; S.store("sth-world", "null"); try { localStorage.removeItem("sth-world"); } catch (e) {} boxes.forEach(function (_, k) { renderBox(k); }); draw(); });
      var dn = el("button", "btn", W.done); dn.type = "button";
      dn.addEventListener("click", function () { panel.hidden = true; openBtn.setAttribute("aria-expanded", "false"); openBtn.focus(); });
      acts.appendChild(rs); acts.appendChild(dn); panel.appendChild(acts);
    }
    openBtn.addEventListener("click", function () {
      var open = panel.hidden; panel.hidden = !open; openBtn.setAttribute("aria-expanded", String(open));
      if (open) draw();
    });
  }

  boxes.forEach(function (_, i) { if (saved) renderBox(i); buildEditor(i); });
})();
