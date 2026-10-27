/* Kazanım rozetleri (Bumba standardı madde 13). Yalnız bu tarayıcıda, tek anahtarda (`sth-rozet`) tutulur:
   r: { rozet kimliği: kazanma günü (yerel YYYY-AA-GG) }, c: koşul sayaçları (m: görülen saat modelleri bit maskesi,
   g: son ziyaret günü, n: üst üste gün sayısı). Eylem günlüğü ya da saat tutulmaz; hiçbir yere gönderilmez.
   Umami'ye yalnız istatistik izni varsa ve rozet o anki eylemle ilk kez kazanıldığında `rozet-kazanildi` {rozet} gider. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document, KEY = "sth-rozet";
  var B = (S.T && S.T.bd) || {};
  var IDS = B.ids || [];
  var MODEL_GOAL = 10;

  function read() {
    try {
      var j = JSON.parse(localStorage.getItem(KEY) || "null");
      if (j && typeof j === "object") return { r: j.r || {}, c: j.c || {} };
    } catch (e) {}
    return { r: {}, c: {} };
  }
  function write(st) { try { localStorage.setItem(KEY, JSON.stringify(st)); } catch (e) {} }
  var pad = function (n) { return (n < 10 ? "0" : "") + n; };
  function dayStr(d) { return d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate()); }
  function bits(m) { var c = 0; m = m >>> 0; while (m) { c += m & 1; m >>>= 1; } return c; }

  /* ---------- bildirim: akışı bölmez, kendiliğinden kaybolur ---------- */
  var toastEl = null, toastQ = [], toastBusy = false;
  function showToast(id, first) {
    toastQ.push([id, first]);
    if (!toastBusy) nextToast();
  }
  function nextToast() {
    var it = toastQ.shift();
    if (!it) { toastBusy = false; if (toastEl) toastEl.hidden = true; return; }
    toastBusy = true;
    if (!toastEl) {
      toastEl = doc.createElement("div");
      toastEl.className = "badge-toast";
      toastEl.setAttribute("role", "status");
      toastEl.setAttribute("aria-live", "polite");
      doc.body.appendChild(toastEl);
    }
    toastEl.textContent = "";
    var p = doc.createElement("p");
    var ic = doc.createElement("span"); ic.className = "bt-icon"; ic.setAttribute("aria-hidden", "true"); ic.textContent = (B.icons || {})[it[0]] || "🏅";
    p.appendChild(ic);
    p.appendChild(doc.createTextNode(S.tpl(B.toast || "{name}", { name: (B.names || {})[it[0]] || it[0] }) + " "));
    if (B.url) { var a = doc.createElement("a"); a.href = B.url; a.textContent = B.toastLink || ""; p.appendChild(a); }
    toastEl.appendChild(p);
    if (it[1] && B.note) { var n = doc.createElement("p"); n.className = "bt-note"; n.textContent = B.note; toastEl.appendChild(n); } // ilk rozette not bir kez (bağlantısız)
    toastEl.hidden = false;
    setTimeout(nextToast, it[1] ? 8000 : 5000);
  }

  function award(st, id, fromLoad) {
    if (IDS.indexOf(id) < 0 || st.r[id]) return false;
    var first = !Object.keys(st.r).length;
    st.r[id] = dayStr(new Date());
    write(st);
    showToast(id, first);
    if (!fromLoad) S.track("rozet-kazanildi", { rozet: id });
    render();
    return true;
  }
  function hit(id, fromLoad, extra) {
    var st = read();
    if (id === "@model") {
      if (!extra || !(extra.n > 0)) return;
      var m = (st.c.m >>> 0) | (1 << extra.i);
      if (m !== st.c.m) { st.c.m = m; write(st); }
      var seen = bits(m);
      if (seen >= MODEL_GOAL) award(st, "model-10", fromLoad);
      if (seen >= extra.n) award(st, "model-hepsi", fromLoad);
      render();
      return;
    }
    award(st, id, fromLoad);
  }

  /* Üst üste ziyaret serisi: yalnız son gün ve gün sayısı */
  (function streak() {
    var st = read(), now = new Date(), today = dayStr(now);
    if (st.c.g === today) return;
    var y = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1);
    st.c.n = st.c.g === dayStr(y) ? (st.c.n || 1) + 1 : 1;
    st.c.g = today;
    write(st);
    if (st.c.n >= 3) award(st, "seri-3", true);
    if (st.c.n >= 7) award(st, "seri-7", true);
  })();

  /* ---------- Rozetler sayfası ---------- */
  var page = doc.querySelector("[data-badges-page]");
  function fmtDay(s) {
    var p = String(s).split("-"), cal = S.cfg.cal || {};
    if (p.length !== 3 || !cal.dateFmt) return s;
    return S.tpl(cal.dateFmt, { day: +p[2], month: (cal.months || [])[+p[1] - 1] || p[1], year: p[0] });
  }
  function render() {
    if (!page) return;
    var st = read(), got = 0;
    var prog = { "model-10": [bits(st.c.m), MODEL_GOAL], "model-hepsi": [bits(st.c.m), +(page.getAttribute("data-models") || 19)], "seri-3": [st.c.n || 0, 3], "seri-7": [st.c.n || 0, 7] };
    Array.prototype.forEach.call(page.querySelectorAll("[data-badge]"), function (li) {
      var id = li.getAttribute("data-badge"), d = st.r[id], status = li.querySelector("[data-badge-status]");
      li.classList.toggle("is-earned", !!d);
      if (d) { got++; status.textContent = S.tpl(B.earnedOn || "{date}", { date: fmtDay(d) }); return; }
      var pr = prog[id];
      status.textContent = (B.locked || "") + (pr ? " · " + S.tpl(B.progress || "{n}/{total}", { n: Math.min(pr[0], pr[1]), total: pr[1] }) : "");
    });
    var cnt = page.querySelector("[data-badges-count]");
    if (cnt) cnt.textContent = S.tpl(B.count || "{n}/{total}", { n: got, total: IDS.length });
  }
  if (page) {
    render();
    var dlg = doc.getElementById("badges-delete"), msg = page.querySelector("[data-badges-msg]");
    var openBtn = page.querySelector("[data-badges-delete]");
    if (openBtn && dlg && typeof dlg.showModal === "function") {
      openBtn.addEventListener("click", function () { dlg.showModal(); });
      dlg.addEventListener("click", function (e) {
        if (e.target.closest && e.target.closest("[data-badges-yes]")) {
          try { localStorage.removeItem(KEY); } catch (x) {}
          dlg.close(); render();
          if (msg) msg.textContent = B.deleted || "";
        } else if ((e.target.closest && e.target.closest("[data-badges-no]")) || e.target === dlg) dlg.close();
      });
    } else if (openBtn) openBtn.hidden = true;
  }

  window.sthBadges = { hit: hit };
  (window.__sthBq || []).forEach(function (q) { hit(q[0], q[1], q[2]); });
  window.__sthBq = [];
})();
