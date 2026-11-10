/* Zekât hesaplama (30 Ekim 2026): Diyanet esasları, nisab 80,18 g altın, oran kırkta bir. Uygulamalardaki Zakat.calc ile
   aynı hesap. Tutarlar yalnız bu sayfada hesaplanır; kaydedilmez ve gönderilmez.
   10 Kasım 2026: "Hesap esası" seçimi; BAZNAS (Endonezya) nisabı 85 g, fitrah ve fidye. Tarayıcının saat dilimi
   Endonezya'daysa BAZNAS seçilir; seçim kaydedilmez. */
(function () {
  "use strict";
  var S = window.sth;
  var root = document.querySelector("[data-zakat]");
  if (!S || !root) return;
  var Z = S.T.zk || {};
  var NISAB_GRAMS = 80.18;
  var stdSel = document.querySelector("[data-zk-std]");
  var ID_TZ = ["Asia/Jakarta", "Asia/Pontianak", "Asia/Makassar", "Asia/Jayapura", "Asia/Ujung_Pandang"];
  if (stdSel) {
    var tz = ""; try { tz = Intl.DateTimeFormat().resolvedOptions().timeZone || ""; } catch (e) {}
    if (ID_TZ.indexOf(tz) >= 0) stdSel.value = "baznas";
  }
  function applyStd() {
    if (!stdSel) return;
    var b = stdSel.value === "baznas";
    NISAB_GRAMS = b ? parseFloat(stdSel.getAttribute("data-grams-baznas")) || 85 : 80.18;
    var lab = document.querySelector("[data-zk-nisab-label]");
    if (lab) lab.textContent = stdSel.getAttribute(b ? "data-nisab-baznas" : "data-nisab-diyanet");
    Array.prototype.forEach.call(document.querySelectorAll("[data-zk-only]"), function (el) { el.hidden = el.getAttribute("data-zk-only") !== stdSel.value; });
  }
  var inputs = Array.prototype.slice.call(root.querySelectorAll("[data-zk]"));
  var q = function (sel) { return root.querySelector(sel); };
  var netEl = q("[data-zk-net]"), nisabEl = q("[data-zk-nisab]"), amountEl = q("[data-zk-amount]");
  var amountBox = q("[data-zk-amount-box]"), verdict = q("[data-zk-verdict]");
  // Ondalık ayırıcı dile göre: tr/de/az/fr/ru/id virgül, en/ar/fa/ur nokta (virgül binlik); Arapça rakamlar da okunur
  var comma = ["tr", "de", "az", "fr", "ru", "id"].indexOf(S.lang) >= 0;
  var nf = new Intl.NumberFormat(/^(ar|fa|ur)$/.test(S.lang) ? "en" : S.locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  var used = false;

  function parse(s) {
    s = String(s || "").replace(/[٠-٩]/g, function (c) { return String(c.charCodeAt(0) - 0x0660); })
      .replace(/[۰-۹]/g, function (c) { return String(c.charCodeAt(0) - 0x06F0); })
      .replace(/[\s٬]/g, "").replace(/٫/g, ".");
    if (!s) return 0;
    s = comma ? s.replace(/\./g, "").replace(",", ".") : s.replace(/,/g, "");
    var n = parseFloat(s);
    return isFinite(n) && n > 0 ? n : 0;
  }

  function calc(v) {
    var g = function (k) { return v[k] || 0; };
    var goldPrice = g("goldPrice");
    var assets = g("cash") + g("goldGrams") * goldPrice + g("silverValue") + g("foreign") + g("trade") + g("receivable");
    var net = Math.max(0, assets - g("debt"));
    var nisab = goldPrice > 0 ? NISAB_GRAMS * goldPrice : null;
    var due = nisab !== null && net >= nisab;
    return { net: net, nisab: nisab, due: due, zakat: due ? net / 40 : 0 };
  }

  function render() {
    var v = {};
    inputs.forEach(function (i) { v[i.getAttribute("data-zk")] = parse(i.value); });
    var r = calc(v);
    netEl.textContent = nf.format(r.net);
    nisabEl.textContent = r.nisab === null ? "–" : nf.format(r.nisab);
    amountEl.textContent = nf.format(r.zakat);
    amountBox.hidden = !r.due;
    verdict.textContent = r.nisab === null ? Z.needGold : r.due ? Z.due : Z.notDue;
    verdict.classList.toggle("is-due", r.due);
  }

  // Fitre: kişi sayısı × Diyanet'in açıkladığı tutar
  var fBox = document.querySelector("[data-fitre]"), fIn = document.querySelector("[data-fitre-people]"), fOut = document.querySelector("[data-fitre-total]");
  if (fBox && fIn && fOut) {
    var fAmount = parseFloat(fBox.getAttribute("data-fitre")) || 0;
    var nf0 = new Intl.NumberFormat(/^(ar|fa|ur)$/.test(S.lang) ? "en" : S.locale, { maximumFractionDigits: 0 });
    var fRender = function () { var n = Math.max(1, Math.min(99, parseInt(fIn.value, 10) || 1)); fOut.textContent = nf0.format(n * fAmount) + " TL"; };
    fIn.addEventListener("input", fRender);
    fRender();
  }

  // Zekât fitrah (BAZNAS): kişi sayısı × tutar (rupiah)
  var iBox = document.querySelector("[data-fitrah]"), iIn = document.querySelector("[data-fitrah-people]"), iOut = document.querySelector("[data-fitrah-total]");
  if (iBox && iIn && iOut) {
    var iAmount = parseFloat(iBox.getAttribute("data-fitrah")) || 0, idf = new Intl.NumberFormat("id-ID", { maximumFractionDigits: 0 });
    var iRender = function () { var n = Math.max(1, Math.min(99, parseInt(iIn.value, 10) || 1)); iOut.textContent = "Rp " + idf.format(n * iAmount); };
    iIn.addEventListener("input", iRender);
    iRender();
  }
  if (stdSel) stdSel.addEventListener("change", function () { applyStd(); render(); });
  applyStd();

  inputs.forEach(function (i) {
    i.addEventListener("input", function () {
      if (!used) { used = true; S.track("arac", { arac: "zekat" }); }
      render();
    });
  });
  render();
})();
