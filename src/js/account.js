/* Uygulamalardaki hesap: e-postadaki bağlantıların açtığı sayfalar (şifre sıfırlama, doğrulama sonucu). Durum adresten
   okunur; tek kullanımlık anahtar gizli alana alınır ve adres çubuğundan silinir (geçmişte ve ekran görüntüsünde kalmasın).
   Form hesap sunucusuna betiksiz gönderilir (CORS gerekmez); sonuç bu sayfaya ?durum= ile döner. */
(function () {
  var root = document.querySelector("[data-account]");
  if (!root) return;
  var q = new URLSearchParams(location.search);
  function show(name) {
    root.querySelectorAll("[data-state]").forEach(function (el) { el.hidden = el.getAttribute("data-state") !== name; });
  }
  function clean() {
    try { history.replaceState(null, "", location.pathname); } catch (e) {}
  }

  if (root.getAttribute("data-account") === "verified") {
    var adim = q.get("adim");
    if (q.get("error")) show("err");
    else if (adim === "eposta-onay") show("step");
    else if (adim === "eposta") show("changed");
    else show("ok");
    clean();
    return;
  }

  var form = root.querySelector("[data-pw-form]");
  var errBox = root.querySelector("[data-form-err]");
  var token = q.get("token") || "";
  var durum = q.get("durum");
  var neden = q.get("neden");
  function formError(text) {
    errBox.textContent = text || "";
    errBox.hidden = !text;
  }
  if (durum === "tamam") show("ok");
  else if (token && (neden === "eslesme" || neden === "uzunluk")) {
    show("form");
    formError(neden === "eslesme" ? form.getAttribute("data-err-match") : form.getAttribute("data-err-length"));
  } else if (durum === "hata") show("err-baglanti");
  else if (token) show("form");
  else show("none");
  form.elements.token.value = token;
  clean();

  var pw1 = form.elements.sifre, pw2 = form.elements.sifre2;
  var toggle = root.querySelector("[data-pw-show]");
  toggle.addEventListener("change", function () {
    pw1.type = pw2.type = toggle.checked ? "text" : "password";
  });
  form.addEventListener("submit", function (e) {
    var len = pw1.value.length;
    if (len < 8 || len > 128) { e.preventDefault(); formError(form.getAttribute("data-err-length")); pw1.focus(); return; }
    if (pw1.value !== pw2.value) { e.preventDefault(); formError(form.getAttribute("data-err-match")); pw2.focus(); return; }
    formError("");
    form.querySelector("button[type=submit]").disabled = true;
  });
  // Geri tuşuyla dönülürse (sayfa önbellekten gelir) düğme yeniden açılsın
  window.addEventListener("pageshow", function () { form.querySelector("button[type=submit]").disabled = false; });
})();
