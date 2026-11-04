/* Saat Tarih metin satırı widget'ı (4 Kasım 2026, 2. analiz #187): başka sitelere tek satırlık canlı saat, tarih ya da
   gün doğumu/batımı yazar. Kullanım: <span data-saat-tarih="time|date|sun" data-tz="Europe/Istanbul" data-lang="tr"
   [data-h12="1"] [data-lat="41.01" data-lon="28.98"]></span> + bu betik. Çerez bırakmaz, veri göndermez; saat ziyaretçinin
   cihazından, saat dilimi ve dil öznitelikten. Gün doğumu/batımı NOAA yaklaşığıyla (±birkaç dakika). Yazı sayfanın
   yazı tipini ve rengini alır. */
(function () {
  "use strict";
  var D2R = Math.PI / 180;
  function fmt(lang, tz, opts) {
    var o = {}; for (var k in opts) o[k] = opts[k];
    if (tz) o.timeZone = tz;
    try { return new Intl.DateTimeFormat((lang || "tr") + "-u-nu-latn", o); } catch (e) { delete o.timeZone; return new Intl.DateTimeFormat(lang || "tr", o); }
  }
  /* Güneş'in doğuşu/batışı (UTC ms; olmuyorsa null): NOAA güneş hesabı, gün ortası yaklaşığı */
  function sunTimes(date, lat, lon) {
    var day0 = Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
    var n = (day0 - Date.UTC(2000, 0, 1, 12)) / 86400000 + 0.5;
    var Jstar = n - lon / 360;
    var M = (357.5291 + 0.98560028 * Jstar) % 360;
    var C = 1.9148 * Math.sin(M * D2R) + 0.02 * Math.sin(2 * M * D2R) + 0.0003 * Math.sin(3 * M * D2R);
    var lam = (M + C + 180 + 102.9372) % 360;
    var Jt = Jstar + 0.0053 * Math.sin(M * D2R) - 0.0069 * Math.sin(2 * lam * D2R);
    var dec = Math.asin(Math.sin(lam * D2R) * Math.sin(23.44 * D2R));
    var cosw = (Math.sin(-0.833 * D2R) - Math.sin(lat * D2R) * Math.sin(dec)) / (Math.cos(lat * D2R) * Math.cos(dec));
    if (cosw > 1 || cosw < -1) return null;
    var w = Math.acos(cosw) / D2R / 360;
    var noon = Date.UTC(2000, 0, 1, 12) + Jt * 86400000;
    return { rise: noon - w * 86400000, set: noon + w * 86400000 };
  }
  function render(el) {
    var kind = el.getAttribute("data-saat-tarih") || "time", lang = el.getAttribute("data-lang") || document.documentElement.lang || "tr";
    var tz = el.getAttribute("data-tz") || undefined, h12 = el.getAttribute("data-h12") === "1", now = new Date();
    var hm = fmt(lang, tz, { hour: "2-digit", minute: "2-digit", hour12: h12 });
    if (kind === "date") { el.textContent = fmt(lang, tz, { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(now) + " · " + fmt(lang, tz, { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: h12 }).format(now); return; }
    if (kind === "sun") {
      var lat = parseFloat(el.getAttribute("data-lat")), lon = parseFloat(el.getAttribute("data-lon"));
      if (isNaN(lat) || isNaN(lon)) return;
      var s = sunTimes(now, lat, lon);
      el.textContent = s ? "☀ " + hm.format(new Date(s.rise)) + " – " + hm.format(new Date(s.set)) : "–";
      return;
    }
    el.textContent = fmt(lang, tz, { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: h12 }).format(now);
  }
  function tick() { var els = document.querySelectorAll("[data-saat-tarih]"); for (var i = 0; i < els.length; i++) render(els[i]); }
  tick();
  setInterval(tick, 1000);
  window.saatTarihLine = { refresh: tick };
})();
