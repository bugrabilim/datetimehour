/* Manyetik sapma (deklinasyon): NOAA/BGS Dünya Manyetik Modeli WMM2025 (2025.0–2030.0).
   Katsayılar WMM2025.COF'tan birebir (kamu malı: "The WMM source code is in the public domain", NOAA NCEI).
   Hesap NOAA'nın klasik geomag.c yordamının karşılığıdır (Schmidt yarı normalize katsayılar, jeodezik → küresel).
   Kullanım: telefon pusulası manyetik kuzeyi verir; gerçek kuzey = manyetik + sapma (doğu artı). 2. analiz #186. */
(function (root) {
  "use strict";
  var EPOCH = 2025.0, MAXORD = 12;
  var COF = [[1,0,-29351.8,0.0,12.0,0.0],[1,1,-1410.8,4545.4,9.7,-21.5],[2,0,-2556.6,0.0,-11.6,0.0],[2,1,2951.1,-3133.6,-5.2,-27.7],[2,2,1649.3,-815.1,-8.0,-12.1],[3,0,1361.0,0.0,-1.3,0.0],[3,1,-2404.1,-56.6,-4.2,4.0],[3,2,1243.8,237.5,0.4,-0.3],[3,3,453.6,-549.5,-15.6,-4.1],[4,0,895.0,0.0,-1.6,0.0],[4,1,799.5,278.6,-2.4,-1.1],[4,2,55.7,-133.9,-6.0,4.1],[4,3,-281.1,212.0,5.6,1.6],[4,4,12.1,-375.6,-7.0,-4.4],[5,0,-233.2,0.0,0.6,0.0],[5,1,368.9,45.4,1.4,-0.5],[5,2,187.2,220.2,0.0,2.2],[5,3,-138.7,-122.9,0.6,0.4],[5,4,-142.0,43.0,2.2,1.7],[5,5,20.9,106.1,0.9,1.9],[6,0,64.4,0.0,-0.2,0.0],[6,1,63.8,-18.4,-0.4,0.3],[6,2,76.9,16.8,0.9,-1.6],[6,3,-115.7,48.8,1.2,-0.4],[6,4,-40.9,-59.8,-0.9,0.9],[6,5,14.9,10.9,0.3,0.7],[6,6,-60.7,72.7,0.9,0.9],[7,0,79.5,0.0,-0.0,0.0],[7,1,-77.0,-48.9,-0.1,0.6],[7,2,-8.8,-14.4,-0.1,0.5],[7,3,59.3,-1.0,0.5,-0.8],[7,4,15.8,23.4,-0.1,0.0],[7,5,2.5,-7.4,-0.8,-1.0],[7,6,-11.1,-25.1,-0.8,0.6],[7,7,14.2,-2.3,0.8,-0.2],[8,0,23.2,0.0,-0.1,0.0],[8,1,10.8,7.1,0.2,-0.2],[8,2,-17.5,-12.6,0.0,0.5],[8,3,2.0,11.4,0.5,-0.4],[8,4,-21.7,-9.7,-0.1,0.4],[8,5,16.9,12.7,0.3,-0.5],[8,6,15.0,0.7,0.2,-0.6],[8,7,-16.8,-5.2,-0.0,0.3],[8,8,0.9,3.9,0.2,0.2],[9,0,4.6,0.0,-0.0,0.0],[9,1,7.8,-24.8,-0.1,-0.3],[9,2,3.0,12.2,0.1,0.3],[9,3,-0.2,8.3,0.3,-0.3],[9,4,-2.5,-3.3,-0.3,0.3],[9,5,-13.1,-5.2,0.0,0.2],[9,6,2.4,7.2,0.3,-0.1],[9,7,8.6,-0.6,-0.1,-0.2],[9,8,-8.7,0.8,0.1,0.4],[9,9,-12.9,10.0,-0.1,0.1],[10,0,-1.3,0.0,0.1,0.0],[10,1,-6.4,3.3,0.0,0.0],[10,2,0.2,0.0,0.1,-0.0],[10,3,2.0,2.4,0.1,-0.2],[10,4,-1.0,5.3,-0.0,0.1],[10,5,-0.6,-9.1,-0.3,-0.1],[10,6,-0.9,0.4,0.0,0.1],[10,7,1.5,-4.2,-0.1,0.0],[10,8,0.9,-3.8,-0.1,-0.1],[10,9,-2.7,0.9,-0.0,0.2],[10,10,-3.9,-9.1,-0.0,-0.0],[11,0,2.9,0.0,0.0,0.0],[11,1,-1.5,0.0,-0.0,-0.0],[11,2,-2.5,2.9,0.0,0.1],[11,3,2.4,-0.6,0.0,-0.0],[11,4,-0.6,0.2,0.0,0.1],[11,5,-0.1,0.5,-0.1,-0.0],[11,6,-0.6,-0.3,0.0,-0.0],[11,7,-0.1,-1.2,-0.0,0.1],[11,8,1.1,-1.7,-0.1,-0.0],[11,9,-1.0,-2.9,-0.1,0.0],[11,10,-0.2,-1.8,-0.1,0.0],[11,11,2.6,-2.3,-0.1,0.0],[12,0,-2.0,0.0,0.0,0.0],[12,1,-0.2,-1.3,0.0,-0.0],[12,2,0.3,0.7,-0.0,0.0],[12,3,1.2,1.0,-0.0,-0.1],[12,4,-1.3,-1.4,-0.0,0.1],[12,5,0.6,-0.0,-0.0,-0.0],[12,6,0.6,0.6,0.1,-0.0],[12,7,0.5,-0.1,-0.0,-0.0],[12,8,-0.1,0.8,0.0,0.0],[12,9,-0.4,0.1,0.0,-0.0],[12,10,-0.2,-1.0,-0.1,-0.0],[12,11,-1.3,0.1,-0.0,0.0],[12,12,-0.7,0.2,-0.1,-0.1]];
  var c = [], cd = [], k = [], snorm = [], fn = [], fm = [];
  var i, n, m;
  for (i = 0; i <= MAXORD; i++) { c[i] = []; cd[i] = []; k[i] = []; for (var j = 0; j <= MAXORD; j++) { c[i][j] = 0; cd[i][j] = 0; k[i][j] = 0; } }
  COF.forEach(function (r) {
    n = r[0]; m = r[1];
    c[m][n] = r[2]; cd[m][n] = r[4];
    if (m !== 0) { c[n][m - 1] = r[3]; cd[n][m - 1] = r[5]; }
  });
  /* Schmidt yarı normalize katsayıları normalize edilmemiş biçime çevir */
  snorm[0] = 1; fm[0] = 0;
  for (n = 1; n <= MAXORD; n++) {
    snorm[n] = snorm[n - 1] * (2 * n - 1) / n;
    var jj = 2;
    for (m = 0; m <= n; m++) {
      k[m][n] = ((n - 1) * (n - 1) - m * m) / ((2 * n - 1) * (2 * n - 3));
      if (m > 0) {
        var flnmj = ((n - m + 1) * jj) / (n + m);
        snorm[n + m * 13] = snorm[n + (m - 1) * 13] * Math.sqrt(flnmj);
        jj = 1;
        c[n][m - 1] *= snorm[n + m * 13];
        cd[n][m - 1] *= snorm[n + m * 13];
      }
      c[m][n] *= snorm[n + m * 13];
      cd[m][n] *= snorm[n + m * 13];
    }
    fn[n] = n + 1; fm[n] = n;
  }
  k[1][1] = 0;

  /** Manyetik sapma (derece, doğu artı). lat/lon derece (jeodezik), altKm deniz üstü km, year ondalık yıl. */
  function declination(lat, lon, altKm, year) {
    var a = 6378.137, b = 6356.7523142, re = 6371.2;
    var a2 = a * a, b2 = b * b, c2 = a2 - b2, a4 = a2 * a2, b4 = b2 * b2, c4 = a4 - b4;
    var dtr = Math.PI / 180, alt = altKm || 0, dt = year - EPOCH;
    var rlon = lon * dtr, rlat = lat * dtr;
    var srlon = Math.sin(rlon), crlon = Math.cos(rlon), srlat = Math.sin(rlat), crlat = Math.cos(rlat);
    var srlat2 = srlat * srlat, crlat2 = crlat * crlat;
    var sp = [0, srlon], cp = [1, crlon];
    var q = Math.sqrt(a2 - c2 * srlat2), q1 = alt * q;
    var q2 = ((q1 + a2) / (q1 + b2)) * ((q1 + a2) / (q1 + b2));
    var ct = srlat / Math.sqrt(q2 * crlat2 + srlat2), st = Math.sqrt(1 - ct * ct);
    var r2 = alt * alt + 2 * q1 + (a4 - c4 * srlat2) / (q * q), r = Math.sqrt(r2);
    var d = Math.sqrt(a2 * crlat2 + b2 * srlat2);
    var ca = (alt + d) / r, sa = c2 * crlat * srlat / (r * d);
    for (var mm = 2; mm <= MAXORD; mm++) { sp[mm] = sp[1] * cp[mm - 1] + cp[1] * sp[mm - 1]; cp[mm] = cp[1] * cp[mm - 1] - sp[1] * sp[mm - 1]; }
    var aor = re / r, ar = aor * aor, br = 0, bt = 0, bp = 0, bpp = 0;
    var p = [], dp = [], pp = [1];
    for (i = 0; i <= MAXORD; i++) { p[i] = []; dp[i] = []; for (var j2 = 0; j2 <= MAXORD; j2++) { p[i][j2] = 0; dp[i][j2] = 0; } }
    p[0][0] = 1; dp[0][0] = 0;
    for (n = 1; n <= MAXORD; n++) {
      ar = ar * aor;
      for (m = 0; m <= n; m++) {
        if (n === m) {
          p[m][n] = st * p[m - 1][n - 1];
          dp[m][n] = st * dp[m - 1][n - 1] + ct * p[m - 1][n - 1];
        } else if (n === 1 && m === 0) {
          p[m][n] = ct * p[m][n - 1];
          dp[m][n] = ct * dp[m][n - 1] - st * p[m][n - 1];
        } else {
          if (m > n - 2) { p[m][n - 2] = 0; dp[m][n - 2] = 0; }
          p[m][n] = ct * p[m][n - 1] - k[m][n] * p[m][n - 2];
          dp[m][n] = ct * dp[m][n - 1] - st * p[m][n - 1] - k[m][n] * dp[m][n - 2];
        }
        var tcmn = c[m][n] + dt * cd[m][n];
        var tcnm = m !== 0 ? c[n][m - 1] + dt * cd[n][m - 1] : 0;
        var par = ar * p[m][n], t1, t2;
        if (m === 0) { t1 = tcmn * cp[m]; t2 = tcmn * sp[m]; }
        else { t1 = tcmn * cp[m] + tcnm * sp[m]; t2 = tcmn * sp[m] - tcnm * cp[m]; }
        bt -= ar * t1 * dp[m][n];
        bp += fm[m] * t2 * par;
        br += fn[n] * t1 * par;
        if (st === 0 && m === 1) {
          pp[n] = n === 1 ? pp[n - 1] : ct * pp[n - 1] - k[m][n] * pp[n - 2];
          bpp += fm[m] * t2 * ar * pp[n];
        }
      }
    }
    bp = st === 0 ? bpp : bp / st;
    var bx = -bt * ca - br * sa, by = bp;
    return Math.atan2(by, bx) / dtr;
  }

  var api = { declination: declination, epoch: EPOCH, validUntil: 2030.0 };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.sthWmm = api;
})(typeof window !== "undefined" ? window : globalThis);
