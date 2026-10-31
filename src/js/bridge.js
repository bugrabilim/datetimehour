/* Köprü tatil planlayıcı (30 Ekim 2026, rakip analizi #22): yıl ve en çok izin seçimi. Tablolar derlemede üretilir
   (lib/bridges.mjs); betik yalnız seçilen yılı ve izin sınırına uyan satırları gösterir. JS'siz bütün yıllar görünür. */
(function () {
  "use strict";
  var doc = document, box = doc.querySelector("[data-bridge]");
  if (!box) return;
  var ySel = doc.querySelector("[data-br-y]"), mSel = doc.querySelector("[data-br-max]");
  var years = Array.prototype.slice.call(doc.querySelectorAll("[data-br-year]"));
  var now = new Date().getFullYear();
  if (years.some(function (s) { return +s.getAttribute("data-br-year") === now; })) ySel.value = String(now);
  function apply() {
    var y = ySel.value, max = +mSel.value;
    years.forEach(function (s) {
      s.hidden = s.getAttribute("data-br-year") !== y;
      Array.prototype.forEach.call(s.querySelectorAll("tr[data-leave]"), function (r) { r.hidden = +r.getAttribute("data-leave") > max; });
    });
  }
  ySel.addEventListener("change", apply);
  mSel.addEventListener("change", apply);
  box.hidden = false;
  apply();
})();
