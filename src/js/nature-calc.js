/* Doğa hesapları (dış veri yok): ekinoks/gündönümü anları (Meeus, Astronomical Algorithms bölüm 27)
   ve yeni ay / dolunay anları (bölüm 49). Tarayıcıda window.sthNature, Node'da module.exports. */
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

  var api = { seasons: seasons, moon: moon, phaseMs: phaseMs };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.sthNature = api;
})(typeof window !== "undefined" ? window : globalThis);
