/* Gömülü saat widget'ı: adres parametrelerine göre tek bir saat modelini gösterir (main.js'ten önce çalışır).
   ?m=<model>&c=<şehir>&theme=auto|light|dark&sec=0&h12=1 */
(function () {
  "use strict";
  var doc = document;
  var q = new URLSearchParams(location.search);
  var cfg = {};
  try { cfg = JSON.parse(doc.getElementById("page-config").textContent); } catch (e) {}
  var slides = Array.prototype.slice.call(doc.querySelectorAll(".slide"));
  var want = q.get("m") || "classic";
  var sel = slides.filter(function (s) { return s.getAttribute("data-model") === want; })[0] || slides[0];
  if (!sel) return;
  var tz = "";
  var key = q.get("c");
  (cfg.cities || []).forEach(function (c) { if (c.k === key) tz = c.tz; });
  if (tz) sel.querySelectorAll("[data-live],[data-analog]").forEach(function (el) { el.setAttribute("data-tz", tz); });
  slides.forEach(function (s) { if (s !== sel) s.parentNode.removeChild(s); });
  sel.classList.add("is-selected");
  sel.setAttribute("aria-hidden", "false");
  var theme = q.get("theme");
  if (theme === "light" || theme === "dark") doc.documentElement.setAttribute("data-theme", theme);
  else if (theme === "auto") doc.documentElement.removeAttribute("data-theme");
  window.__sthPrefs = { sec: q.get("sec") !== "0", h12: q.get("h12") === "1", sync: true };
})();
