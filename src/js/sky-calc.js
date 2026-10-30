/* Gezegen konumları (dış veri yok): JPL'in yaklaşık Kepler öğeleri (E. M. Standish, "Keplerian Elements for Approximate
   Positions of the Major Planets", 1800–2050 tablosu; doğruluk yay dakikası düzeyinde). Yer: Yer-Ay ağırlık merkezi.
   Işık süresi bir kez düzeltilir; J2000 tutulum boylamına genel presesyon eklenir, tarih eğikliğiyle ekvatora çevrilir.
   Parlaklık: Astronomical Almanac (Meeus bölüm 41) formülleri, Satürn'de halka terimi yok (±0,5 kadir).
   Tarayıcıda window.sthSky, Node'da module.exports. */
(function (root) {
  "use strict";
  var D2R = Math.PI / 180;
  var sin = function (d) { return Math.sin(d * D2R); };
  var cos = function (d) { return Math.cos(d * D2R); };
  var UNIX_JD = 2440587.5, DELTA_T = 69;
  // a, ȧ, e, ė, I, İ, L, L̇, ϖ, ϖ̇, Ω, Ω̇ (AB, derece; yüzyıl başına)
  var EL = {
    mercury: [0.38709927, 0.00000037, 0.20563593, 0.00001906, 7.00497902, -0.00594749, 252.25032350, 149472.67411175, 77.45779628, 0.16047689, 48.33076593, -0.12534081],
    venus: [0.72333566, 0.00000390, 0.00677672, -0.00004107, 3.39467605, -0.00078890, 181.97909950, 58517.81538729, 131.60246718, 0.00268329, 76.67984255, -0.27769418],
    earth: [1.00000261, 0.00000562, 0.01671123, -0.00004392, -0.00001531, -0.01294668, 100.46457166, 35999.37244981, 102.93768193, 0.32327364, 0, 0],
    mars: [1.52371034, 0.00001847, 0.09339410, 0.00007882, 1.84969142, -0.00813131, -4.55343205, 19140.30268499, -23.94362959, 0.44441088, 49.55953891, -0.29257343],
    jupiter: [5.20288700, -0.00011607, 0.04838624, -0.00013253, 1.30439695, -0.00183714, 34.39644051, 3034.74612775, 14.72847983, 0.21252668, 100.47390909, 0.20469106],
    saturn: [9.53667594, -0.00125060, 0.05386179, -0.00050991, 2.48599187, 0.00193609, 49.95424423, 1222.49362201, 92.59887831, -0.41897216, 113.66242448, -0.28867794],
    uranus: [19.18916464, -0.00196176, 0.04725744, -0.00004397, 0.77263783, -0.00242939, 313.23810451, 428.48202785, 170.95427630, 0.40805281, 74.01692503, 0.04240589],
    neptune: [30.06992276, 0.00026291, 0.00859048, 0.00005105, 1.77004347, 0.00035372, -55.12002969, 218.45945325, 44.96476227, -0.32241464, 131.78422574, -0.00508664]
  };
  var PLANETS = ["mercury", "venus", "mars", "jupiter", "saturn"];

  /* Güneş merkezli konum (J2000 tutulum düzlemi, AB); T: J2000'den Jülyen yüzyıl (TT) */
  function helio(name, T) {
    var q = EL[name];
    var a = q[0] + q[1] * T, e = q[2] + q[3] * T, I = q[4] + q[5] * T, L = q[6] + q[7] * T, w = q[8] + q[9] * T, O = q[10] + q[11] * T;
    var M = L - w, om = w - O;
    M = ((M + 180) % 360 + 360) % 360 - 180;
    var E = M + (e / D2R) * sin(M);
    for (var i = 0; i < 10; i++) { var dE = (M - (E - (e / D2R) * sin(E))) / (1 - e * cos(E)); E += dE; if (Math.abs(dE) < 1e-8) break; }
    var xp = a * (cos(E) - e), yp = a * Math.sqrt(1 - e * e) * sin(E);
    return {
      x: (cos(om) * cos(O) - sin(om) * sin(O) * cos(I)) * xp + (-sin(om) * cos(O) - cos(om) * sin(O) * cos(I)) * yp,
      y: (cos(om) * sin(O) + sin(om) * cos(O) * cos(I)) * xp + (-sin(om) * sin(O) + cos(om) * cos(O) * cos(I)) * yp,
      z: (sin(om) * sin(I)) * xp + (cos(om) * sin(I)) * yp
    };
  }
  var MAG = {
    mercury: function (i) { return -0.42 + 0.0380 * i - 0.000273 * i * i + 0.000002 * i * i * i; },
    venus: function (i) { return -4.40 + 0.0009 * i + 0.000239 * i * i - 0.00000065 * i * i * i; },
    mars: function (i) { return -1.52 + 0.016 * i; },
    jupiter: function (i) { return -9.40 + 0.005 * i; },
    saturn: function () { return -8.88; },
    uranus: function () { return -7.19; },
    neptune: function () { return -6.87; }
  };

  /** ms anında gezegenin yer merkezli görünür konumu: { ra, dec (derece, tarih ekinoksu), dist (AB), r (Güneş'e AB),
      elong (Güneş'ten açısal uzaklık, derece), phase (evre açısı), illum (0..1), mag (kadir) } */
  function planet(name, ms) {
    var T = (ms / 86400000 + UNIX_JD + DELTA_T / 86400 - 2451545) / 36525;
    var ea = helio("earth", T), p = helio(name, T);
    var dx = p.x - ea.x, dy = p.y - ea.y, dz = p.z - ea.z, dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
    var T2 = T - dist * 0.0057755183 / 36525; // ışık süresi
    p = helio(name, T2);
    dx = p.x - ea.x; dy = p.y - ea.y; dz = p.z - ea.z; dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
    var r = Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z), R = Math.sqrt(ea.x * ea.x + ea.y * ea.y + ea.z * ea.z);
    var lam = Math.atan2(dy, dx) / D2R + 1.3969713 * T, bet = Math.asin(dz / dist) / D2R;
    var eps = 23.4392911 - 0.0130042 * T;
    var ra = Math.atan2(sin(lam) * cos(eps) - Math.tan(bet * D2R) * sin(eps), cos(lam)) / D2R;
    var dec = Math.asin(sin(bet) * cos(eps) + cos(bet) * sin(eps) * sin(lam)) / D2R;
    var ci = (r * r + dist * dist - R * R) / (2 * r * dist), i = Math.acos(Math.max(-1, Math.min(1, ci))) / D2R;
    var ce = (R * R + dist * dist - r * r) / (2 * R * dist), elong = Math.acos(Math.max(-1, Math.min(1, ce))) / D2R;
    return { ra: ((ra % 360) + 360) % 360, dec: dec, dist: dist, r: r, elong: elong, phase: i, illum: (1 + ci) / 2, mag: MAG[name](i) + 5 * Math.log10(r * dist) };
  }

  /* Parlak yıldızlar (J2000; sağ açıklık ve dik açıklık derece, kadir). Presesyon gece gökyüzü çizimi için yok sayılır (~0,4°). */
  var STARS = [
    ["Sirius", 101.287, -16.716, -1.46], ["Canopus", 95.988, -52.696, -0.74], ["Arcturus", 213.915, 19.182, -0.05],
    ["Vega", 279.234, 38.784, 0.03], ["Capella", 79.172, 45.998, 0.08], ["Rigel", 78.634, -8.202, 0.13],
    ["Procyon", 114.825, 5.225, 0.34], ["Achernar", 24.429, -57.237, 0.46], ["Betelgeuse", 88.793, 7.407, 0.50],
    ["Hadar", 210.956, -60.373, 0.61], ["Altair", 297.696, 8.868, 0.76], ["Acrux", 186.650, -63.099, 0.76],
    ["Aldebaran", 68.980, 16.509, 0.86], ["Antares", 247.352, -26.432, 0.96], ["Spica", 201.298, -11.161, 0.97],
    ["Pollux", 116.329, 28.026, 1.14], ["Fomalhaut", 344.413, -29.622, 1.16], ["Deneb", 310.358, 45.280, 1.25],
    ["Mimosa", 191.930, -59.689, 1.25], ["Regulus", 152.093, 11.967, 1.35], ["Adhara", 104.656, -28.972, 1.50],
    ["Castor", 113.650, 31.888, 1.58], ["Shaula", 263.402, -37.104, 1.62], ["Bellatrix", 81.283, 6.350, 1.64],
    ["Elnath", 81.573, 28.608, 1.65], ["Alnilam", 84.053, -1.202, 1.69], ["Alnitak", 85.190, -1.943, 1.77],
    ["Alioth", 193.507, 55.960, 1.77], ["Dubhe", 165.932, 61.751, 1.79], ["Mirfak", 51.081, 49.861, 1.79],
    ["Alkaid", 206.885, 49.313, 1.86], ["Polaris", 37.955, 89.264, 1.98], ["Mintaka", 83.002, -0.299, 2.23],
    ["Mizar", 200.981, 54.925, 2.23], ["Merak", 165.460, 56.383, 2.37], ["Phecda", 178.458, 53.695, 2.44],
    ["Megrez", 183.857, 57.033, 3.31], ["Schedar", 10.127, 56.537, 2.24], ["Caph", 2.295, 59.150, 2.28]
  ];

  var api = { planet: planet, PLANETS: PLANETS, STARS: STARS, helio: helio };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.sthSky = api;
})(typeof window !== "undefined" ? window : globalThis);
