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

  /* Kıble (30 Ekim 2026): Kâbe'ye büyük daire başlangıç yönü (kuzeyden saat yönünde, derece) ve uzaklık (km) */
  var KAABA = { lat: 21.422487, lon: 39.826206 };
  function qibla(lat, lon) {
    var dl = KAABA.lon - lon;
    var bearing = fixAngle(atan2(sin(dl), cos(lat) * tan(KAABA.lat) - sin(lat) * cos(dl)));
    var c = sin(lat) * sin(KAABA.lat) + cos(lat) * cos(KAABA.lat) * cos(dl);
    var km = Math.acos(Math.max(-1, Math.min(1, c))) * 6371.0088;
    return { bearing: bearing, km: km };
  }

  /* Güneşin yüksekliği ve azimutu (derece; azimut kuzeyden saat yönünde) — UT ondalık saatinde */
  function sunAltAz(y, m, d, ut, lat, lon) {
    var p = sunPos(julian(y, m, d) + ut / 24);
    var H = 15 * (ut + p.eqt + lon / 15 - 12); // saat açısı
    var alt = asin(sin(lat) * sin(p.decl) + cos(lat) * cos(p.decl) * cos(H));
    var az = fixAngle(atan2(sin(H), cos(H) * sin(lat) - tan(p.decl) * cos(lat)) + 180);
    return { alt: alt, az: az };
  }

  /* Kıble saati: Güneşin kıble yönünde olduğu an (gölge kıbleye ters düşer); yerel ondalık saat, gündüz yoksa NaN.
     Gündüz 5 dk adımla taranır, azimut farkının işaret değiştirdiği aralık ikiye bölünür. */
  function qiblaTime(y, m, d, lat, lon, tz) {
    var q = qibla(lat, lon).bearing;
    var diff = function (ut) { var a = sunAltAz(y, m, d, ut, lat, lon); return { alt: a.alt, f: ((a.az - q + 540) % 360) - 180 }; };
    var step = 5 / 60, prev = diff(-tz);
    for (var ut = -tz + step; ut <= 24 - tz + 1e-9; ut += step) {
      var cur = diff(ut);
      if (prev.alt > 0 && cur.alt > 0 && prev.f <= 0 && cur.f > 0 && cur.f - prev.f < 90) {
        var lo = ut - step, hi = ut;
        for (var i = 0; i < 30; i++) { var mid = (lo + hi) / 2; if (diff(mid).f <= 0) lo = mid; else hi = mid; }
        return (lo + hi) / 2 + tz;
      }
      prev = cur;
    }
    return NaN;
  }

  /* Güneşin belli bir yüksekliğe (derece, ufkun üstü pozitif) sabah çıktığı ve akşam indiği an; yerel ondalık saat,
     o gün oluşmuyorsa NaN. Vakitlerdeki gibi düzeltmesiz, ikinci yaklaşımla (30 Ekim 2026: altın ve mavi saat). */
  function sunAltTimes(y, m, d, lat, lon, tz, alt) {
    var jd = julian(y, m, d) - lon / 360;
    var at = function (t, ccw) {
      var p = sunPos(jd + t);
      var a = (sin(alt) - sin(p.decl) * sin(lat)) / (cos(p.decl) * cos(lat));
      if (a < -1 || a > 1) return NaN;
      return fixHour(12 - p.eqt) + (ccw ? -1 : 1) * acos(a) / 15;
    };
    var rise = at(6 / 24, true), set = at(18 / 24, false);
    if (!isNaN(rise)) rise = at(rise / 24, true);
    if (!isNaN(set)) set = at(set / 24, false);
    var off = tz - lon / 15;
    return { rise: rise + off, set: set + off };
  }

  /* Altın saat (Güneş +6° ile −4° arası) ve mavi saat (−4° ile −6° arası); sabah ve akşam [başlangıç, bitiş] */
  var LIGHT = { golden: 6, edge: -4, blue: -6 };
  function lightHours(y, m, d, lat, lon, tz) {
    var g = sunAltTimes(y, m, d, lat, lon, tz, LIGHT.golden), e = sunAltTimes(y, m, d, lat, lon, tz, LIGHT.edge), b = sunAltTimes(y, m, d, lat, lon, tz, LIGHT.blue);
    return { blueAm: [b.rise, e.rise], goldenAm: [e.rise, g.rise], goldenPm: [g.set, e.set], bluePm: [e.set, b.set] };
  }

  /* Kerahat vakitleri (yaklaşık): Diyanet'in yayımladığı bir dakika ölçüsü yok; takvimlerde yaygın olan 45 dakika
     kullanılır. İşrak: güneş vaktinden 45 dk sonrasına; istiva: öğleden 45 dk önceden öğleye; isfirar: akşamdan
     45 dk önceden akşama. r: times() sonucu. */
  var KERAHAT_MIN = 45;
  function kerahat(r) {
    var k = KERAHAT_MIN / 60;
    return { israk: [r.sunrise, r.sunrise + k], istiva: [r.dhuhr - k, r.dhuhr], isfirar: [r.maghrib - k, r.maghrib] };
  }

  var api = { times: times, PARAMS: PARAMS, julian: julian, sunPos: sunPos, qibla: qibla, qiblaTime: qiblaTime, sunAltAz: sunAltAz, KAABA: KAABA, sunAltTimes: sunAltTimes, lightHours: lightHours, LIGHT: LIGHT, kerahat: kerahat, KERAHAT_MIN: KERAHAT_MIN };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.sthPrayer = api;
})(typeof window !== "undefined" ? window : globalThis);
