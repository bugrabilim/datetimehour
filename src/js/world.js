/* Dünya saatleri: şehirler UTC farkına göre bir doğru (zaman çizgisi) üzerinde gösterilir.
   Kullanıcı şehir ekler/çıkarır, istediği saat dilimini ekler; seçim yalnız bu tarayıcıda (localStorage: sth-world) saklanır. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;
  var box = doc.querySelector("[data-world]");
  if (!box) return;
  var W = S.T.wc || {};
  var cities = S.cfg.cities || [];
  var byKey = {};
  cities.forEach(function (c) { byKey[c.k] = c; });
  var MAX = 24, MIN_OFF = -12, MAX_OFF = 14;
  var defaults = Array.prototype.map.call(box.querySelectorAll("[data-world-item]"), function (el) { return el.getAttribute("data-world-item"); });
  var saved = null;
  try { var raw = JSON.parse(S.store("sth-world") || "null"); if (Array.isArray(raw)) saved = raw.filter(function (x) { return typeof x === "string"; }).slice(0, MAX); } catch (e) {}
  var current = function () { return saved ? saved : defaults; };

  function zoneName(tz) { return tz.split("/").pop().replace(/_/g, " "); }
  function entry(token) {
    if (token.indexOf("tz:") === 0) { var tz = token.slice(3); return { token: token, name: zoneName(tz), tz: tz }; }
    var c = byKey[token];
    return c ? { token: token, name: c.n, tz: c.tz, path: c.p } : null;
  }
  function el(tag, cls, text) { var e = doc.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  function live(e, kind, tz) { e.setAttribute("data-live", kind); e.setAttribute("data-tz", tz); return e; }

  var line = el("div", "tl"); line.setAttribute("data-world-line", "");
  var track = el("div", "tl-track"), axis = el("div", "tl-axis"), items = el("ul", "tl-items");
  track.appendChild(axis); track.appendChild(items); line.appendChild(track);
  var staticList = box.querySelector(".tl-static");
  if (staticList) staticList.hidden = true;
  box.appendChild(line);

  function offsetOf(tz) { return S.offsetMin(new Date(S.nowMs()), tz) / 60; }
  function pct(off) { return Math.max(0, Math.min(100, (off - MIN_OFF) / (MAX_OFF - MIN_OFF) * 100)); }

  function drawAxis() {
    axis.textContent = "";
    var mine = offsetOf(S.localTz);
    for (var h = MIN_OFF; h <= MAX_OFF; h++) {
      var t = el("span", "tl-tick" + (h % 2 === 0 ? " is-major" : ""));
      t.style.setProperty("--x", pct(h).toFixed(3));
      if (h % 2 === 0) t.appendChild(el("span", "tl-tick-label", (h > 0 ? "+" : h < 0 ? "−" : "") + Math.abs(h)));
      axis.appendChild(t);
    }
    var me = el("span", "tl-me"); me.style.setProperty("--x", pct(mine).toFixed(3)); me.title = S.T.yourZone || ""; axis.appendChild(me);
  }

  function render() {
    var list = current().map(entry).filter(Boolean).map(function (it) { it.off = offsetOf(it.tz); return it; });
    list.sort(function (a, b) { return a.off - b.off; });
    items.textContent = "";
    // yatay yerleşim: kartlar çizginin altına ve üstüne, çakışmayanlar aynı kademeye (satıra) dağıtılır
    var STEP = 4.9, AXIS_H = 2.6;
    var width = track.clientWidth || 900, gap = Math.max(6, 140 / width * 100), ends = { up: [], down: [] }, top = { up: 0, down: 0 }, placed = [];
    list.forEach(function (it) {
      var x = pct(it.off), best = null;
      ["down", "up"].forEach(function (side) {
        var l = 0; while (ends[side][l] != null && x - ends[side][l] < gap) l++;
        if (!best || l < best.l) best = { side: side, l: l };
      });
      ends[best.side][best.l] = x; if (best.l + 1 > top[best.side]) top[best.side] = best.l + 1;
      placed.push({ it: it, x: x, side: best.side, l: best.l });
    });
    var axisTop = top.up * STEP;
    track.style.setProperty("--axis", axisTop + "rem");
    track.style.setProperty("--h", (axisTop + AXIS_H + top.down * STEP) + "rem");
    placed.forEach(function (o) {
      var it = o.it, li = el("li", "tl-item"), ax = axisTop + 1;
      li.setAttribute("data-world-item", it.token);
      li.style.setProperty("--x", o.x.toFixed(3));
      var cardTop = o.side === "down" ? axisTop + AXIS_H + o.l * STEP : axisTop - (o.l + 1) * STEP + 0.4;
      var stemTop = o.side === "down" ? ax : cardTop + STEP - 0.4;
      var stemH = o.side === "down" ? cardTop - ax : ax - stemTop;
      li.style.setProperty("--cy", cardTop + "rem"); li.style.setProperty("--sy", stemTop + "rem"); li.style.setProperty("--sh", stemH + "rem");
      var stem = el("span", "tl-stem"); stem.setAttribute("aria-hidden", "true");
      var card = el("div", "tl-card");
      var nm;
      if (it.path) { nm = el("a", "tl-name", it.name); nm.href = it.path; } else nm = el("span", "tl-name", it.name);
      card.appendChild(nm);
      card.appendChild(live(el("span", "tl-time", "--:--"), "time", it.tz));
      card.appendChild(live(el("span", "tl-off", " "), "offset", it.tz));
      card.appendChild(live(el("span", "tl-diff", " "), "diff", it.tz));
      li.appendChild(stem); li.appendChild(card); items.appendChild(li);
    });
    S.refreshLive();
  }
  function setSaved(list) { saved = list; S.store("sth-world", JSON.stringify(list)); render(); }

  /* Düzenleyici: üstte tek "şehir ekle / çıkar" düğmesi */
  var wrap = el("div", "world-edit");
  var openBtn = el("button", "toggle", W.customize); openBtn.type = "button"; openBtn.setAttribute("aria-expanded", "false");
  var panel = el("div", "world-panel"); panel.hidden = true;
  wrap.appendChild(openBtn); wrap.appendChild(panel);
  box.parentNode.insertBefore(wrap, box);

  function option(sel, value, text) { var o = doc.createElement("option"); o.value = value; o.textContent = text; sel.appendChild(o); }
  function draw() {
    panel.textContent = "";
    panel.appendChild(el("p", "meta", W.customizeLead));
    var list = current().slice();
    var ul = el("ul", "chip-list");
    if (!list.length) ul.appendChild(el("li", "meta", W.empty));
    list.forEach(function (tok, n) {
      var it = entry(tok); if (!it) return;
      var li = el("li", "chip-item"); li.appendChild(el("span", null, it.name));
      var b = el("button", "toggle", "✕"); b.type = "button"; b.setAttribute("aria-label", W.remove + ": " + it.name);
      b.addEventListener("click", function () { var l = list.slice(); l.splice(n, 1); setSaved(l); draw(); });
      li.appendChild(b); ul.appendChild(li);
    });
    panel.appendChild(ul);
    var row = el("div", "inputs-row");
    var f1 = el("div", "field"), l1 = el("label", null, W.addCity), s1 = el("select"); l1.setAttribute("for", "wc-city"); s1.id = "wc-city";
    option(s1, "", W.select);
    cities.slice().sort(function (a, b) { return a.n.localeCompare(b.n, S.locale); }).forEach(function (c) { if (list.indexOf(c.k) < 0) option(s1, c.k, c.n + " (" + c.c + ")"); });
    var add1 = el("button", "btn", W.add); add1.type = "button";
    add1.addEventListener("click", function () { if (s1.value && list.length < MAX) { setSaved(list.concat([s1.value])); draw(); } });
    f1.appendChild(l1); f1.appendChild(s1); row.appendChild(f1); row.appendChild(add1); panel.appendChild(row);
    var zones = [];
    try { zones = Intl.supportedValuesOf("timeZone"); } catch (e) {}
    if (zones.length) {
      var row2 = el("div", "inputs-row"), f2 = el("div", "field"), l2 = el("label", null, W.addZone), s2 = el("select"); l2.setAttribute("for", "wc-zone"); s2.id = "wc-zone";
      option(s2, "", W.select);
      zones.forEach(function (z) { if (list.indexOf("tz:" + z) < 0) option(s2, z, z.replace(/_/g, " ")); });
      var add2 = el("button", "btn", W.add); add2.type = "button";
      add2.addEventListener("click", function () { if (s2.value && list.length < MAX) { setSaved(list.concat(["tz:" + s2.value])); draw(); } });
      f2.appendChild(l2); f2.appendChild(s2); row2.appendChild(f2); row2.appendChild(add2); panel.appendChild(row2);
    }
    var acts = el("div", "tool-actions");
    var rs = el("button", "toggle", W.reset); rs.type = "button";
    rs.addEventListener("click", function () { saved = null; try { localStorage.removeItem("sth-world"); } catch (e) {} render(); draw(); });
    var dn = el("button", "btn", W.done); dn.type = "button";
    dn.addEventListener("click", function () { panel.hidden = true; openBtn.setAttribute("aria-expanded", "false"); openBtn.focus(); });
    acts.appendChild(rs); acts.appendChild(dn); panel.appendChild(acts);
  }
  openBtn.addEventListener("click", function () {
    var open = panel.hidden; panel.hidden = !open; openBtn.setAttribute("aria-expanded", String(open));
    if (open) draw();
  });

  drawAxis(); render();
  var rt = 0;
  window.addEventListener("resize", function () { clearTimeout(rt); rt = setTimeout(render, 150); });
  setInterval(function () { drawAxis(); render(); }, 10 * 60 * 1000); // yaz saati geçişleri için
})();
