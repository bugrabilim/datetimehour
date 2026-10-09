// Sayfa çizilmeden önce kayıtlı temayı uygular (yanlış temanın bir an görünmesini engeller).
document.documentElement.classList.add("js");
(function () {
  try {
    var t = localStorage.getItem("sth-theme");
    if (t === "light" || t === "dark") document.documentElement.setAttribute("data-theme", t);
  } catch (e) {}
})();
