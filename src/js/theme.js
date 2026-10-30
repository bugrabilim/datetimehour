// Sayfa çizilmeden önce kayıtlı temayı uygular (yanlış temanın bir an görünmesini engeller).
document.documentElement.classList.add("js");
(function () {
  try {
    var t = localStorage.getItem("sth-theme");
    if (t === "light" || t === "dark") document.documentElement.setAttribute("data-theme", t);
    // Saat görünümü (yazı tipi, renk; main.js applyLook ile aynı): gömme widget'ında yok
    if (location.pathname.indexOf("/embed/") !== 0) {
      var p = JSON.parse(localStorage.getItem("sth-prefs") || "{}");
      if (/^(rounded|serif|mono|light)$/.test(p.font)) document.documentElement.setAttribute("data-cfont", p.font);
      if (/^(ink|blue|green|red|purple)$/.test(p.color)) document.documentElement.setAttribute("data-ccolor", p.color);
    }
  } catch (e) {}
})();
