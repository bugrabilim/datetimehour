/* Doğa hesapları (dış veri yok): ekinoks/gündönümü anları (Meeus, Astronomical Algorithms bölüm 27),
   yeni ay / dolunay anları (bölüm 49), Ay'ın konumu (bölüm 47, kısaltılmış) ve doğuş/batışı (bölüm 15). Tarayıcıda window.sthNature, Node'da module.exports. */
(function (root) {
  "use strict";
  var D2R = Math.PI / 180;
  var sin = function (d) { return Math.sin(d * D2R); };
  var cos = function (d) { return Math.cos(d * D2R); };
  var UNIX_JD = 2440587.5;
  var jdToMs = function (jd) { return (jd - UNIX_JD) * 86400000; };

  var DELTA_T = 69; // saniye (2026 dolayı; yalnız TT→UT için)

  var SEASON_TERMS = [
    [485, 324.96, 1934.136], [203, 337.23, 32964.467], [199, 342.08, 20.186], [182, 27.85, 445267.112],
    [156, 73.14, 45036.886], [136, 171.52, 22518.443], [77, 222.54, 65928.934], [74, 296.72, 3034.906],
    [70, 243.58, 9037.513], [58, 119.81, 33718.147], [52, 297.17, 150.678], [50, 21.02, 2281.226],
    [45, 247.54, 29929.562], [44, 325.15, 31555.956], [29, 60.93, 4443.417], [18, 155.12, 67555.328],
    [17, 288.79, 4562.452], [16, 198.04, 62894.029], [14, 199.76, 31436.921], [12, 95.39, 14577.848],
    [12, 287.11, 31931.756], [12, 320.81, 34777.259], [9, 227.73, 1222.114], [8, 15.45, 16859.074]
  ];
  var BASE = {
    march: function (y) { return 2451623.80984 + 365242.37404 * y + 0.05169 * y * y - 0.00411 * y * y * y - 0.00057 * y * y * y * y; },
    june: function (y) { return 2451716.56767 + 365241.62603 * y + 0.00325 * y * y + 0.00888 * y * y * y - 0.00030 * y * y * y * y; },
    sept: function (y) { return 2451810.21715 + 365242.01767 * y - 0.11575 * y * y + 0.00337 * y * y * y + 0.00078 * y * y * y * y; },
    dec: function (y) { return 2451900.05952 + 365242.74049 * y - 0.06223 * y * y - 0.00823 * y * y * y + 0.00032 * y * y * y * y; }
  };
  /** Verilen yılın 4 mevsim anı (UTC milisaniye): { march, june, sept, dec } */
  function seasons(year) {
    var y = (year - 2000) / 1000, out = {};
    Object.keys(BASE).forEach(function (k) {
      var jde0 = BASE[k](y), T = (jde0 - 2451545.0) / 36525;
      var W = 35999.373 * T - 2.47;
      var dl = 1 + 0.0334 * cos(W) + 0.0007 * cos(2 * W);
      var S = 0;
      SEASON_TERMS.forEach(function (t) { S += t[0] * cos(t[1] + t[2] * T); });
      var jde = jde0 + (0.00001 * S) / dl;
      out[k] = Math.round(jdToMs(jde) - DELTA_T * 1000);
    });
    return out;
  }

  /* Yeni ay / dolunay: k tam sayı = yeni ay, k+0.5 = dolunay */
  function phaseJDE(k, full) {
    var T = k / 1236.85;
    var jde = 2451550.09766 + 29.530588861 * k + 0.00015437 * T * T - 0.00000015 * T * T * T + 0.00000000073 * T * T * T * T;
    var E = 1 - 0.002516 * T - 0.0000074 * T * T;
    var M = 2.5534 + 29.10535670 * k - 0.0000014 * T * T - 0.00000011 * T * T * T;
    var Mp = 201.5643 + 385.81693528 * k + 0.0107582 * T * T + 0.00001238 * T * T * T - 0.000000058 * T * T * T * T;
    var F = 160.7108 + 390.67050284 * k - 0.0016118 * T * T - 0.00000227 * T * T * T + 0.000000011 * T * T * T * T;
    var Om = 124.7746 - 1.56375588 * k + 0.0020672 * T * T + 0.00000215 * T * T * T;
    var c;
    if (!full) {
      c = -0.40720 * sin(Mp) + 0.17241 * E * sin(M) + 0.01608 * sin(2 * Mp) + 0.01039 * sin(2 * F) + 0.00739 * E * sin(Mp - M) - 0.00514 * E * sin(Mp + M) + 0.00208 * E * E * sin(2 * M) - 0.00111 * sin(Mp - 2 * F) - 0.00057 * sin(Mp + 2 * F) + 0.00056 * E * sin(2 * Mp + M) - 0.00042 * sin(3 * Mp) + 0.00042 * E * sin(M + 2 * F) + 0.00038 * E * sin(M - 2 * F) - 0.00024 * E * sin(2 * Mp - M) - 0.00017 * sin(Om);
    } else {
      c = -0.40614 * sin(Mp) + 0.17302 * E * sin(M) + 0.01614 * sin(2 * Mp) + 0.01043 * sin(2 * F) + 0.00734 * E * sin(Mp - M) - 0.00515 * E * sin(Mp + M) + 0.00209 * E * E * sin(2 * M) - 0.00111 * sin(Mp - 2 * F) - 0.00057 * sin(Mp + 2 * F) + 0.00056 * E * sin(2 * Mp + M) - 0.00042 * sin(3 * Mp) + 0.00042 * E * sin(M + 2 * F) + 0.00038 * E * sin(M - 2 * F) - 0.00024 * E * sin(2 * Mp - M) - 0.00017 * sin(Om);
    }
    return jde + c;
  }
  function phaseMs(k, full) { return Math.round(jdToMs(phaseJDE(k + (full ? 0.5 : 0), full)) - DELTA_T * 1000); }
  function kFor(ms) { return Math.floor(((ms / 86400000 + UNIX_JD - 2451550.09766) / 29.530588861)); }

  /** ms anı için Ay: { age (gün), frac (0..1 döngü), illum (0..1), idx (0..7: yeni ay, hilal…), nextNew, nextFull } */
  function moon(ms) {
    var k = kFor(ms) - 1, prevNew = 0, nextNew = 0;
    for (var i = 0; i < 4; i++, k++) {
      var t = phaseMs(k, false);
      if (t <= ms) prevNew = t; else { nextNew = t; break; }
    }
    var len = nextNew - prevNew, frac = (ms - prevNew) / len;
    var illum = (1 - Math.cos(2 * Math.PI * frac)) / 2;
    var idx = Math.floor(frac * 8 + 0.5) % 8;
    // sonraki dolunay
    var kk = kFor(ms) - 1, nextFull = 0;
    for (var j = 0; j < 4; j++, kk++) { var f = phaseMs(kk, true); if (f > ms) { nextFull = f; break; } }
    return { age: (ms - prevNew) / 86400000, frac: frac, illum: illum, idx: idx, nextNew: nextNew, nextFull: nextFull };
  }

  /* Ay'ın konumu: Meeus bölüm 47, kısaltılmış seri (boylamda en büyük 32, enlemde 28 terim; ~0,01°).
     Satırlar: D, M, M', F, boylam (1e-6°), uzaklık (1e-3 km) / enlem (1e-6°). M'li terimler E (|M|=2: E²) ile çarpılır. */
  var MOON_LR = [
    [0, 0, 1, 0, 6288774, -20905355], [2, 0, -1, 0, 1274027, -3699111], [2, 0, 0, 0, 658314, -2955968], [0, 0, 2, 0, 213618, -569925],
    [0, 1, 0, 0, -185116, 48888], [0, 0, 0, 2, -114332, -3149], [2, 0, -2, 0, 58793, 246158], [2, -1, -1, 0, 57066, -152138],
    [2, 0, 1, 0, 53322, -170733], [2, -1, 0, 0, 45758, -204586], [0, 1, -1, 0, -40923, -129620], [1, 0, 0, 0, -34720, 108743],
    [0, 1, 1, 0, -30383, 104755], [2, 0, 0, -2, 15327, 10321], [0, 0, 1, 2, -12528, 0], [0, 0, 1, -2, 10980, 79661],
    [4, 0, -1, 0, 10675, -34782], [0, 0, 3, 0, 10034, -23210], [4, 0, -2, 0, 8548, -21636], [2, 1, -1, 0, -7888, 24208],
    [2, 1, 0, 0, -6766, 30824], [1, 0, -1, 0, -5163, -8379], [1, 1, 0, 0, 4987, -16675], [2, -1, 1, 0, 4036, -12831],
    [2, 0, 2, 0, 3994, -10445], [4, 0, 0, 0, 3861, -11650], [2, 0, -3, 0, 3665, 14403], [0, 1, -2, 0, -2689, -7003],
    [2, 0, -1, 2, -2602, 0], [2, -1, -2, 0, 2390, 10056], [1, 0, 1, 0, -2348, 6322], [2, -2, 0, 0, 2236, -9884]
  ];
  var MOON_B = [
    [0, 0, 0, 1, 5128122], [0, 0, 1, 1, 280602], [0, 0, 1, -1, 277693], [2, 0, 0, -1, 173237], [2, 0, -1, 1, 55413],
    [2, 0, -1, -1, 46271], [2, 0, 0, 1, 32573], [0, 0, 2, 1, 17198], [2, 0, 1, -1, 9266], [0, 0, 2, -1, 8822],
    [2, -1, 0, -1, 8216], [2, 0, -2, -1, 4324], [2, 0, 1, 1, 4200], [2, 1, 0, -1, -3359], [2, -1, -1, 1, 2463],
    [2, -1, 0, 1, 2211], [2, -1, -1, -1, 2065], [0, 1, -1, -1, -1870], [4, 0, -1, -1, 1828], [0, 1, 0, 1, -1794],
    [0, 0, 0, 3, -1749], [0, 1, -1, 1, -1565], [1, 0, 0, 1, -1491], [0, 1, 1, 1, -1475], [0, 1, 1, -1, -1410],
    [0, 1, 0, -1, -1344], [1, 0, 0, -1, -1335], [0, 0, 3, 1, 1107]
  ];
  /** ms anında Ay'ın yer merkezli konumu: { ra, dec (derece), par (yatay paralaks, derece) } */
  function moonPos(ms) {
    var T = (ms / 86400000 + UNIX_JD + DELTA_T / 86400 - 2451545) / 36525;
    var T2 = T * T, T3 = T2 * T, T4 = T3 * T;
    var Lp = 218.3164477 + 481267.88123421 * T - 0.0015786 * T2 + T3 / 538841 - T4 / 65194000;
    var D = 297.8501921 + 445267.1114034 * T - 0.0018819 * T2 + T3 / 545868 - T4 / 113065000;
    var M = 357.5291092 + 35999.0502909 * T - 0.0001536 * T2 + T3 / 24490000;
    var Mp = 134.9633964 + 477198.8675055 * T + 0.0087414 * T2 + T3 / 69699 - T4 / 14712000;
    var F = 93.2720950 + 483202.0175233 * T - 0.0036539 * T2 - T3 / 3526000 + T4 / 863310000;
    var A1 = 119.75 + 131.849 * T, A2 = 53.09 + 479264.290 * T, A3 = 313.45 + 481266.484 * T;
    var E = 1 - 0.002516 * T - 0.0000074 * T2;
    var sl = 0, sr = 0, sb = 0, i, t, arg, e;
    for (i = 0; i < MOON_LR.length; i++) {
      t = MOON_LR[i];
      arg = t[0] * D + t[1] * M + t[2] * Mp + t[3] * F;
      e = t[1] === 0 ? 1 : (t[1] === 2 || t[1] === -2 ? E * E : E);
      sl += t[4] * e * sin(arg);
      sr += t[5] * e * cos(arg);
    }
    for (i = 0; i < MOON_B.length; i++) {
      t = MOON_B[i];
      arg = t[0] * D + t[1] * M + t[2] * Mp + t[3] * F;
      e = t[1] === 0 ? 1 : (t[1] === 2 || t[1] === -2 ? E * E : E);
      sb += t[4] * e * sin(arg);
    }
    sl += 3958 * sin(A1) + 1962 * sin(Lp - F) + 318 * sin(A2);
    sb += -2235 * sin(Lp) + 382 * sin(A3) + 175 * sin(A1 - F) + 175 * sin(A1 + F) + 127 * sin(Lp - Mp) - 115 * sin(Lp + Mp);
    var lam = Lp + sl / 1000000, bet = sb / 1000000, dist = 385000.56 + sr / 1000;
    var eps = 23.4392911 - 0.0130042 * T;
    var ra = Math.atan2(sin(lam) * cos(eps) - (sin(bet) / cos(bet)) * sin(eps), cos(lam)) / D2R;
    var dec = Math.asin(sin(bet) * cos(eps) + cos(bet) * sin(eps) * sin(lam)) / D2R;
    return { ra: ra, dec: dec, par: Math.asin(6378.14 / dist) / D2R };
  }
  /* Ay'ın ufka göre yüksekliği eksi doğuş/batış eşiği (Meeus bölüm 15: h0 = 0,7275 π − 0°34') */
  function moonAlt(ms, lat, lon) {
    var p = moonPos(ms);
    var d = ms / 86400000 + UNIX_JD - 2451545, T = d / 36525;
    var gmst = 280.46061837 + 360.98564736629 * d + 0.000387933 * T * T;
    var sh = sin(lat) * sin(p.dec) + cos(lat) * cos(p.dec) * cos(gmst + lon - p.ra);
    return Math.asin(sh) / D2R - (0.7275 * p.par - 0.5667);
  }
  /** startMs'ten başlayan spanMs (varsayılan 24 sa) içinde Ay'ın ilk doğuşu ve batışı (ms; olmuyorsa null).
      10 dakikalık adımlarla işaret değişimi aranır, 12 kez ikiye bölünerek ~0,15 sn'ye inilir. */
  function moonRiseSet(startMs, lat, lon, spanMs) {
    var step = 600000, n = Math.round((spanMs || 86400000) / step), out = { rise: null, set: null };
    var t0 = startMs, f0 = moonAlt(t0, lat, lon);
    for (var i = 1; i <= n; i++) {
      var t1 = startMs + i * step, f1 = moonAlt(t1, lat, lon);
      if ((f0 < 0) !== (f1 < 0)) {
        var key = f0 < 0 ? "rise" : "set";
        if (out[key] === null) {
          var a = t0, b = t1, fa = f0;
          for (var j = 0; j < 12; j++) {
            var m = (a + b) / 2, fm = moonAlt(m, lat, lon);
            if ((fa < 0) === (fm < 0)) { a = m; fa = fm; } else b = m;
          }
          out[key] = Math.round((a + b) / 2);
        }
      }
      t0 = t1; f0 = f1;
    }
    return out;
  }

  var api = { seasons: seasons, moon: moon, phaseMs: phaseMs, moonPos: moonPos, moonRiseSet: moonRiseSet, moonAltitude: moonAlt };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.sthNature = api;
})(typeof window !== "undefined" ? window : globalThis);
