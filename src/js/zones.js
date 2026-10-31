/* Yaz saati değişimleri ve Unix zaman damgası sayfaları (30 Ekim 2026, rakip analizi #25).
   Yaz saati tablosu derlemede üretilir; burada kalan gün yazılır, geçmiş satırlar gizlenir.
   Unix: canlı zaman (sunucu-senkron saat), zaman damgası ↔ tarih çevirici, 2038 sınırına kalan süre. */
(function () {
  "use strict";
  var S = window.sth;
  if (!S) return;
  var doc = document;
  var q = function (sel) { return doc.querySelector(sel); };

  /* ---------- Yaz saati ---------- */
  var dst = q("[data-dst]");
  if (dst) {
    var rows = Array.prototype.slice.call(dst.querySelectorAll("tr[data-at]"));
    var dayStart = function (ms) { var d = new Date(ms); return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime(); };
    var paint = function () {
      var now = S.nowMs(), today = dayStart(now);
      rows.forEach(function (r) {
        var at = +r.getAttribute("data-at");
        r.hidden = at < now - 3600000;
        var n = Math.round((dayStart(at) - today) / 86400000), cell = r.querySelector("[data-dst-left]");
        if (cell) cell.textContent = n <= 0 ? dst.getAttribute("data-today") : n === 1 ? dst.getAttribute("data-tomorrow") : S.tpl(dst.getAttribute("data-in"), { n: n });
      });
    };
    paint();
    setInterval(paint, 60000);
  }

  /* ---------- Resmî tatil tabloları (dünya tatilleri): kalan gün, geçenler soluk, sıradaki tatil ---------- */
  var hols = Array.prototype.slice.call(doc.querySelectorAll("[data-hol]"));
  if (hols.length) {
    var midnight = function (d) { return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime(); };
    var parseD = function (s) { var p = s.split("-"); return new Date(+p[0], +p[1] - 1, +p[2]).getTime(); };
    var nextEl = q("[data-hol-next]"), shown = false;
    var today = midnight(new Date(S.nowMs()));
    hols.forEach(function (tb) {
      Array.prototype.forEach.call(tb.querySelectorAll("tr[data-d]"), function (r) {
        var ds = r.getAttribute("data-d"); if (!ds) return;
        var n = Math.round((parseD(ds) - today) / 86400000), cell = r.querySelector("[data-left]");
        r.classList.toggle("is-past", n < 0);
        if (cell) cell.textContent = n < 0 ? tb.getAttribute("data-passed") : n === 0 ? tb.getAttribute("data-today") : n === 1 ? tb.getAttribute("data-tomorrow") : S.tpl(tb.getAttribute("data-in"), { n: n });
        if (nextEl && !shown && n >= 0) {
          shown = true;
          var name = r.children[2] ? r.children[2].firstChild.textContent : "";
          nextEl.textContent = nextEl.getAttribute("data-label") + ": " + name + " · " + r.children[0].textContent + " (" + cell.textContent + ")";
        }
      });
    });
  }

  /* ---------- Unix ---------- */
  var ux = q("[data-unix]");
  if (!ux) return;
  var X = S.T.ux || {};
  var nowEl = q("[data-ux-now]"), nowMsEl = q("[data-ux-now-ms]"), copyBtn = q("[data-ux-copy]");
  var tsIn = q("[data-ux-ts]"), detect = q("[data-ux-detect]"), dateIn = q("[data-ux-date]"), zoneSel = q("[data-ux-zone]");
  var y38 = q("[data-ux-2038]"), LIMIT = 2147483647000;
  var nf = new Intl.NumberFormat(/^(ar|fa|ur)$/.test(S.lang) ? "en-US" : S.locale);
  var pad = function (n) { return String(n).padStart(2, "0"); };
  function set(k, v) { var e = q("[data-ux-" + k + "]"); if (e) e.textContent = v; }
  function span(ms) {
    var a = Math.abs(ms), d = Math.floor(a / 86400000), h = Math.floor((a % 86400000) / 3600000), m = Math.floor((a % 3600000) / 60000);
    return (d ? nf.format(d) + " " + X.days + " " : "") + h + " " + X.hours + " " + m + " " + X.minutes;
  }
  function fmt(ms, tz) {
    try {
      return new Intl.DateTimeFormat(S.locale, { timeZone: tz, year: "numeric", month: "long", day: "numeric", weekday: "long", hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23" }).format(new Date(ms));
    } catch (e) { return "–"; }
  }
  function tick() {
    var now = S.nowMs(), sec = Math.floor(now / 1000);
    if (nowEl.textContent !== String(sec)) nowEl.textContent = String(sec);
    nowMsEl.textContent = String(Math.floor(now));
    if (y38) y38.textContent = S.tpl(X.y2038Left, { d: span(LIMIT - now) });
  }
  tick();
  setInterval(tick, 250);

  if (copyBtn) copyBtn.addEventListener("click", function () {
    var v = nowEl.textContent, done = function () { var o = copyBtn.textContent; copyBtn.textContent = copyBtn.getAttribute("data-copied"); setTimeout(function () { copyBtn.textContent = o; }, 1500); };
    try { navigator.clipboard.writeText(v).then(done, function () {}); } catch (e) {}
  });

  // Zaman damgası → tarih: 12 basamaktan uzunsa milisaniye
  function fromTs() {
    var raw = (tsIn.value || "").trim().replace(/[\s.,_]/g, "");
    if (!raw) { ["utc", "local", "iso", "rel"].forEach(function (k) { set(k, "–"); }); detect.textContent = " "; return; }
    if (!/^-?\d{1,16}$/.test(raw)) { detect.textContent = X.invalid; return; }
    var n = +raw, isMs = raw.replace("-", "").length > 11, ms = isMs ? n : n * 1000;
    if (!isFinite(ms) || Math.abs(ms) > 8.64e15) { detect.textContent = X.invalid; return; }
    detect.textContent = isMs ? X.detectedMs : X.detectedSec;
    set("utc", fmt(ms, "UTC"));
    set("local", fmt(ms, S.localTz));
    set("iso", new Date(ms).toISOString());
    var diff = ms - S.nowMs();
    set("rel", S.tpl(diff < 0 ? X.ago : X.in, { d: span(diff) }));
  }
  tsIn.addEventListener("input", fromTs);

  // Tarih → zaman damgası (yerel saat ya da UTC)
  function fromDate() {
    var v = dateIn.value;
    if (!v) { set("sec", "–"); set("ms", "–"); return; }
    var p = v.split(/[-T:]/).map(Number), ms;
    if (zoneSel.value === "utc") ms = Date.UTC(p[0], p[1] - 1, p[2], p[3] || 0, p[4] || 0, p[5] || 0);
    else ms = new Date(p[0], p[1] - 1, p[2], p[3] || 0, p[4] || 0, p[5] || 0).getTime();
    if (!isFinite(ms)) { set("sec", X.invalid); return; }
    set("sec", String(Math.floor(ms / 1000)));
    set("ms", String(ms));
  }
  dateIn.addEventListener("input", fromDate);
  zoneSel.addEventListener("change", fromDate);

  // Başlangıç: şu anki zaman iki çeviricide de
  var start = new Date(S.nowMs());
  tsIn.value = String(Math.floor(start.getTime() / 1000));
  dateIn.value = start.getFullYear() + "-" + pad(start.getMonth() + 1) + "-" + pad(start.getDate()) + "T" + pad(start.getHours()) + ":" + pad(start.getMinutes()) + ":" + pad(start.getSeconds());
  fromTs(); fromDate();
})();
