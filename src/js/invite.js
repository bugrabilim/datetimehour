/* Halka ve grup daveti (üyelik aşama 2c, 15 Kasım 2026): /davet/#KOD, grup için /davet/#g:KOD. Kod adresin # kısmındadır (sunucuya gitmez); sayfa
   tarayıcının diline uyan bölümü gösterir, kodu yazar ve "Uygulamada aç" düğmesine saattarih://davet?kod=KOD verir. */
(function () {
  "use strict";
  var blocks = Array.prototype.slice.call(document.querySelectorAll("[data-invite]"));
  if (!blocks.length) return;
  var langs = (navigator.languages && navigator.languages.length ? navigator.languages : [navigator.language || "en"]).map(function (l) {
    l = String(l).toLowerCase();
    return l === "tl" || l.indexOf("tl-") === 0 ? "fil" : l.indexOf("in") === 0 && (l.length === 2 || l[2] === "-") ? "id" : l.split("-")[0];
  });
  var pick = null;
  for (var i = 0; i < langs.length && !pick; i++) pick = blocks.filter(function (b) { return b.getAttribute("data-invite") === langs[i]; })[0] || null;
  pick = pick || blocks.filter(function (b) { return b.getAttribute("data-invite") === "en"; })[0] || blocks[0];
  blocks.forEach(function (b) { b.hidden = b !== pick; });
  document.documentElement.lang = pick.getAttribute("lang") || "en";
  document.documentElement.dir = pick.getAttribute("dir") || "ltr";
  var raw = decodeURIComponent((location.hash || "").slice(1));
  var group = /^g:/i.test(raw);
  var code = (group ? raw.slice(2) : raw).toUpperCase().replace(/[^A-Z0-9]/g, "");
  var ok = code.length === 8;
  pick.querySelectorAll("[data-invite-kind]").forEach(function (el) {
    el.hidden = el.getAttribute("data-invite-kind") !== (group ? "group" : "circle");
  });
  pick.querySelectorAll("[data-invite-code]").forEach(function (el) { el.textContent = code; });
  pick.querySelectorAll("[data-invite-has]").forEach(function (el) { el.hidden = !ok; });
  pick.querySelectorAll("[data-invite-none]").forEach(function (el) { el.hidden = ok; });
  pick.querySelectorAll("[data-invite-open]").forEach(function (el) { el.href = "saattarih://davet?kod=" + code + (group ? "&tur=grup" : ""); });
})();
