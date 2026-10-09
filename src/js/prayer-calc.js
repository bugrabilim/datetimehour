/* Namaz vakitleri hesabı (astronomik; dış veri yok). Güneş konumu PrayTimes tarzı düşük duyarlıklı formüllerle.
   Yöntem: İmsak 18°, Yatsı 17° (Diyanet ile uyumlu), ikindi gölge katsayısı 1, resmî vakitlere uyum için küçük düzeltmeler.
   Tarayıcıda window.sthPrayer, Node'da module.exports olarak kullanılır. */
(function (root) {
  "use strict";
  var D2R = Math.PI / 180, R2D = 180 / Math.PI;
  var sin = function (d) { return Math.sin(d * D2R); };
  var cos = function (d) { return Math.cos(d * D2R); };
  var tan = function (d) { return Math.tan(d * D2R); };
  var asin = function (x) { return Math.asin(x) * R2D; };
  var acos = function (x) { return Math.acos(x) * R2D; };
  var acot = function (x) { return Math.atan(1 / x) * R2D; };
  var atan2 = function (y, x) { return Math.atan2(y, x) * R2D; };
  var fixAngle = function (a) { a = a - 360 * Math.floor(a / 360); return a; };
  var fixHour = function (h) { h = h - 24 * Math.floor(h / 24); return h; };

  var PARAMS = {
    fajr: 18, isha: 17, sunrise: 0.833, sunset: 0.833, asrFactor: 1,
    // dakika cinsinden düzeltmeler (resmî vakitlerle karşılaştırılarak ayarlandı)
    adj: { fajr: 0, sunrise: -7.1, dhuhr: 5.1, asr: 4.5, maghrib: 7.9, isha: 0.9 }
  };

  function julian(y, m, d) {
    if (m <= 2) { y -= 1; m += 12; }
    var A = Math.floor(y / 100), B = 2 - A + Math.floor(A / 4);
    return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + B - 1524.5;
  }
  function sunPos(jd) {
    var D = jd - 2451545.0;
    var g = fixAngle(357.529 + 0.98560028 * D);
    var q = fixAngle(280.459 + 0.98564736 * D);
    var L = fixAngle(q + 1.915 * sin(g) + 0.020 * sin(2 * g));
    var e = 23.439 - 0.00000036 * D;
    var RA = fixHour(atan2(cos(e) * sin(L), cos(L)) / 15);
    return { decl: asin(sin(e) * sin(L)), eqt: q / 15 - RA };
  }

  /* Bir gün için vakitler (yerel saat olarak ondalık saat). tz: UTC'ye göre saat farkı. */
  function times(y, m, d, lat, lon, tz, opt) {
    var P = opt || PARAMS;
    var jd = julian(y, m, d) - lon / (360);
    var midDay = function (t) { return fixHour(12 - sunPos(jd + t).eqt); };
    var angleTime = function (alt, t, ccw) { // alt: ufkun altındaki açı (derece, pozitif = altında)
      var decl = sunPos(jd + t).decl;
      var a = (-sin(alt) - sin(decl) * sin(lat)) / (cos(decl) * cos(lat));
      if (a < -1 || a > 1) return NaN;
      return midDay(t) + (ccw ? -1 : 1) * acos(a) / 15;
    };
    var asrTime = function (t) {
      var decl = sunPos(jd + t).decl;
      var altAbove = acot(P.asrFactor + tan(Math.abs(lat - decl)));
      return angleTime(-altAbove, t, false);
    };
    var base = 12 + tz - lon / 15;
    var t0 = { fajr: 5, sunrise: 6, dhuhr: 12, asr: 13, sunset: 18, isha: 18 };
    for (var k in t0) t0[k] = t0[k] / 24;
    var r = {
      fajr: angleTime(P.fajr, t0.fajr, true),
      sunrise: angleTime(P.sunrise, t0.sunrise, true),
      dhuhr: midDay(t0.dhuhr),
      asr: asrTime(t0.asr),
      maghrib: angleTime(P.sunset, t0.sunset, false),
      isha: angleTime(P.isha, t0.isha, false)
    };
    var off = tz - lon / 15;
    for (var n in r) r[n] = r[n] + off;
    // Yüksek enlemlerde (beyaz geceler) imsak/yatsı açısı hiç oluşmuyorsa: gecenin yedide biri kuralı
    if (!isNaN(r.sunrise) && !isNaN(r.maghrib)) {
      var night = 24 - (r.maghrib - r.sunrise);
      if (isNaN(r.fajr)) r.fajr = r.sunrise - night / 7;
      if (isNaN(r.isha)) r.isha = r.maghrib + night / 7;
    }
    var a = P.adj;
    r.fajr += a.fajr / 60; r.sunrise += a.sunrise / 60; r.dhuhr += a.dhuhr / 60;
    r.asr += a.asr / 60; r.maghrib += a.maghrib / 60; r.isha += a.isha / 60;
    return r;
  }

  var api = { times: times, PARAMS: PARAMS, julian: julian, sunPos: sunPos };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.sthPrayer = api;
})(typeof window !== "undefined" ? window : globalThis);
