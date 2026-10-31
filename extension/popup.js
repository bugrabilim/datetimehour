/* Saat Tarih tarayıcı eklentisi (30 Ekim 2026): saat (cihaz saati), bugünün tarihi, hicri tarih (Diyanet'e hizalı tablo)
   ve seçilen il için namaz vakitleri (sitedeki prayer-calc.js ile cihazda hesap, Türkiye saati). İnternete bağlanmaz;
   yalnız seçilen il ve 24/12 tercihi tarayıcının eklenti deposunda (storage.local) saklanır. */
(function () {
  "use strict";
  var D = window.STH_DATA, P = window.sthPrayer;
  var LANGS = ["tr", "en", "de", "az", "ar", "id", "ur", "fa", "fr", "ru"];
  var RTL = ["ar", "ur", "fa"];
  var ui = ((window.chrome && chrome.i18n && chrome.i18n.getUILanguage && chrome.i18n.getUILanguage()) || navigator.language || "en").slice(0, 2).toLowerCase();
  var lang = LANGS.indexOf(ui) >= 0 ? ui : "en";
  var S = D.strings[lang];
  var ORDER = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"];
  var TR_OFF = 3; // Türkiye saati (UTC+3, yaz saati yok)
  var $ = function (id) { return document.getElementById(id); };

  document.documentElement.lang = lang;
  document.documentElement.dir = RTL.indexOf(lang) >= 0 ? "rtl" : "ltr";
  $("province-label").textContent = S.province;
  $("pr-h").textContent = S.today;
  $("site").textContent = S.open;
  $("fmt").textContent = S.format;
  $("note").textContent = S.note;

  var store = (window.chrome && chrome.storage && chrome.storage.local) || null;
  var state = { province: "istanbul", h12: false };

  // İller: arayüz dilinde sıralı (Arapça, Rusça ve Farsçada kendi yazısıyla: p[4] ar, p[5] ru, p[6] fa)
  var sel = $("province");
  var nameIdx = { ar: 4, ru: 5, fa: 6 }[lang];
  D.provinces.map(function (p) { return { slug: p[0], name: nameIdx && p[nameIdx] ? p[nameIdx] : p[1] }; })
    .sort(function (a, b) { return a.name.localeCompare(b.name, lang); })
    .forEach(function (p) { var o = document.createElement("option"); o.value = p.slug; o.textContent = p.name; sel.appendChild(o); });

  var pad = function (n) { return String(n).padStart(2, "0"); };
  var hm = function (min) { min = ((Math.round(min) % 1440) + 1440) % 1440; return pad(Math.floor(min / 60)) + ":" + pad(min % 60); };
  var dateFmt = new Intl.DateTimeFormat(lang + "-u-nu-latn-ca-gregory", { weekday: "long", day: "numeric", month: "long", year: "numeric" });

  /* Türkiye saatinde şimdiki gün ve gece yarısından bu yana saniye */
  function trNow(ms) {
    var t = new Date(ms + TR_OFF * 3600000);
    return { y: t.getUTCFullYear(), m: t.getUTCMonth() + 1, d: t.getUTCDate(), sec: t.getUTCHours() * 3600 + t.getUTCMinutes() * 60 + t.getUTCSeconds() };
  }
  function addDay(y, m, d, n) { var t = new Date(Date.UTC(y, m - 1, d + n)); return { y: t.getUTCFullYear(), m: t.getUTCMonth() + 1, d: t.getUTCDate() }; }
  function iso(y, m, d) { return y + "-" + pad(m) + "-" + pad(d); }

  /* Günün vakitleri: gece yarısından itibaren dakika (sitedeki gibi yuvarlanmış) */
  function dayTimes(prov, y, m, d) {
    var r = P.times(y, m, d, prov[2], prov[3], TR_OFF);
    return ORDER.map(function (k) { return isFinite(r[k]) ? Math.round(r[k] * 60) : null; });
  }

  /* Hicri tarih: tablo satırları [ay başı, yıl, ay], son satır tablonun sonu */
  function hijri(y, m, d) {
    var t = D.hijri, day = iso(y, m, d);
    if (!t.length || day < t[0][0] || day >= t[t.length - 1][0]) return "";
    for (var i = t.length - 2; i >= 0; i--) {
      if (t[i][0] <= day) {
        var diff = Math.round((Date.UTC(y, m - 1, d) - Date.parse(t[i][0] + "T00:00:00Z")) / 86400000) + 1;
        return S.hijriFmt.replace("{d}", diff).replace("{m}", S.hijriMonths[t[i][2] - 1]).replace("{y}", t[i][1]);
      }
    }
    return "";
  }

  var lastDay = "", list = $("times");
  function render() {
    var now = Date.now(), local = new Date(now);
    var h = local.getHours(), mi = local.getMinutes(), se = local.getSeconds();
    $("clock").textContent = state.h12 ? ((h % 12) || 12) + ":" + pad(mi) + ":" + pad(se) + (h < 12 ? " AM" : " PM") : pad(h) + ":" + pad(mi) + ":" + pad(se);
    var tr = trNow(now), dayKey = iso(tr.y, tr.m, tr.d) + state.province;
    var prov = D.provinces.filter(function (p) { return p[0] === state.province; })[0] || D.provinces[0];
    var today = dayTimes(prov, tr.y, tr.m, tr.d);
    if (dayKey !== lastDay) {
      lastDay = dayKey;
      $("date").textContent = dateFmt.format(local);
      $("hijri").textContent = hijri(tr.y, tr.m, tr.d);
      list.textContent = "";
      ORDER.forEach(function (k, i) {
        var li = document.createElement("li"), sp = document.createElement("span"), b = document.createElement("b");
        sp.textContent = S.names[k]; b.textContent = today[i] == null ? "–" : hm(today[i]);
        li.appendChild(sp); li.appendChild(b); li.setAttribute("data-k", k); list.appendChild(li);
      });
    }
    // Sıradaki vakit (bugün kalan ya da yarının imsakı)
    var nowSec = tr.sec, nextIdx = -1, left = 0;
    for (var i = 0; i < ORDER.length; i++) if (today[i] != null && today[i] * 60 > nowSec) { nextIdx = i; left = today[i] * 60 - nowSec; break; }
    if (nextIdx < 0) { var tm = addDay(tr.y, tr.m, tr.d, 1); nextIdx = 0; left = dayTimes(prov, tm.y, tm.m, tm.d)[0] * 60 + 86400 - nowSec; }
    $("next-label").textContent = S.nextIn.replace("{name}", S.names[ORDER[nextIdx]]);
    $("next-in").textContent = pad(Math.floor(left / 3600)) + ":" + pad(Math.floor((left % 3600) / 60)) + ":" + pad(left % 60);
    Array.prototype.forEach.call(list.children, function (li) { li.classList.toggle("is-next", li.getAttribute("data-k") === ORDER[nextIdx]); });
  }

  function save() { if (store) store.set({ province: state.province, h12: state.h12 }); }
  sel.addEventListener("change", function () { state.province = sel.value; save(); render(); });
  $("fmt").addEventListener("click", function () { state.h12 = !state.h12; save(); render(); });

  function start(saved) {
    if (saved && typeof saved.province === "string" && D.provinces.some(function (p) { return p[0] === saved.province; })) state.province = saved.province;
    if (saved && typeof saved.h12 === "boolean") state.h12 = saved.h12;
    sel.value = state.province;
    render();
    setInterval(render, 1000);
  }
  if (store) store.get(["province", "h12"], start); else start(null);
})();
