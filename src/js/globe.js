/* Ana sayfa: dünya küresi. Uzakken yavaş dönen kabartmalı küre (gündüz/gece, 15°'lik saat dilimi meridyenleri, şehirlerin
   yerel saati); yakınlaştıkça o bölgedeki yerlerin saati de görünür; daha da yakınlaşınca OpenStreetMap haritasına
   geçilir (sokak düzeyine kadar), meridyen ve meridyen parçaları boylamlarıyla çizilir.
   Küre dokusu Natural Earth (kamu malı); harita karoları yalnız harita düzeyine inildiğinde OpenStreetMap'ten gelir. */
(function () {
  "use strict";
  var doc = document, S = window.sth;
  var fig = doc.querySelector("[data-globe]");
  if (!fig || !S) return;
  var canvas = fig.querySelector("canvas");
  var ctx = canvas && canvas.getContext && canvas.getContext("2d");
  if (!ctx) return;
  var G = S.cfg.globe || {};
  var RAD = Math.PI / 180;
  var TILT0 = 23, SPEED = 3; // başlangıç enlemi, derece/sn (bir tur 2 dk)
  var Z_MAP0 = 2.2, Z_MAP1 = 3.0; // yakınlaştırma çarpanı: haritaya geçiş aralığı (çapraz geçiş)
  var ZMAX_TILE = 19.2;
  var TILE_URL = "https://tile.openstreetmap.org/";
  var reduce = window.matchMedia && matchMedia("(prefers-reduced-motion: reduce)").matches;
  var rtl = doc.documentElement.dir === "rtl";

  var lon0 = -new Date().getTimezoneOffset() / 4 - 10; // başlangıçta cihazın bölgesi görünür
  var tilt = TILT0, zoom = 1, zadj = null;
  var px = 0, dpr = 1, C = 0, R0 = 0, R = 0;
  var col = {}, dirty = true, pending = true;
  var attr = fig.querySelector("[data-globe-attr]");
  var zoomIn = fig.querySelector('[data-globe-zoom="in"]'), zoomOut = fig.querySelector('[data-globe-zoom="out"]'), resetBtn = fig.querySelector("[data-globe-reset]");

  /* ---------- renkler ---------- */
  function readColors() {
    var cs = getComputedStyle(fig), g = function (k, fb) { return (cs.getPropertyValue(k) || "").trim() || fb; };
    col.grid = g("--globe-grid", "rgba(255,255,255,.5)"); col.label = g("--globe-label", "#111"); col.halo = g("--globe-halo", "#fff");
    col.accent = g("--accent", "#c60"); col.rim = g("--border", "#999");
  }

  /* ---------- doku (Natural Earth) ---------- */
  var tex = null, TW = 0, TH = 0;
  if (G.earth) {
    var im = new Image();
    im.onload = function () {
      try {
        var c = doc.createElement("canvas"); TW = c.width = im.naturalWidth; TH = c.height = im.naturalHeight;
        var x = c.getContext("2d"); x.drawImage(im, 0, 0);
        tex = x.getImageData(0, 0, TW, TH).data; pending = true; kick();
      } catch (e) {}
    };
    im.src = G.earth;
  }

  /* ---------- yer adları (yakınlaşınca yüklenir) ---------- */
  var featured = (G.cities || []).map(function (c) { return { n: c.n, lat: c.lat, lon: c.lon, tz: c.tz, m: c.m ? 1 : 0 }; });
  var places = featured.slice(), placesState = 0;
  function ensurePlaces() {
    if (placesState || !G.places || !G.names) return;
    placesState = 1;
    Promise.all([fetch(G.places, { credentials: "omit" }).then(function (r) { return r.json(); }), fetch(G.names, { credentials: "omit" }).then(function (r) { return r.json(); })])
      .then(function (a) {
        var d = a[0], names = a[1];
        d.p.forEach(function (r, i) { places.push({ n: names[i], lat: r[0], lon: r[1], tz: d.tz[r[2]], m: 0 }); });
        placesState = 2; pending = true; kick();
      }).catch(function () { placesState = 0; });
  }

  /* ---------- yerel saat metni ---------- */
  var fmtCache = {}, timeCache = {};
  function localTime(tz, sec) {
    var h12 = !!(S.prefs && S.prefs.h12), key = tz + (h12 ? "1" : "0"), t = timeCache[key];
    if (t && t.s === sec) return t.v;
    var f = fmtCache[key];
    if (!f) { try { f = fmtCache[key] = new Intl.DateTimeFormat(S.locale, { hour: "2-digit", minute: "2-digit", hour12: h12, timeZone: tz }); } catch (e) { f = fmtCache[key] = { format: function () { return ""; } }; } }
    t = timeCache[key] = { s: sec, v: f.format(new Date(sec * 1000)) };
    return t.v;
  }

  /* ---------- geometri ---------- */
  var sc = doc.createElement("canvas"), sctx = sc.getContext("2d"), sp = 0, sk = 1; // yüzey ara belleği (CSS çözünürlüğünde)
  var img = null, N = 0, pIdx, pLon, pLat, pSinLat, pCosLat, pCosL, pSinL, pShade, pAlpha;
  var sinT = 0, cosT = 1;

  function setup() {
    var css = canvas.clientWidth || 320;
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    var size = Math.round(css * dpr);
    if (size !== px) { px = size; canvas.width = canvas.height = px; dirty = true; }
    C = px / 2; R0 = C - 2 * dpr;
    sp = Math.max(64, Math.round(css * Math.min(dpr, 1.25))); // yüzey bu çözünürlükte hesaplanıp büyütülür
    if (sc.width !== sp) { sc.width = sc.height = sp; img = sctx.createImageData(sp, sp); dirty = true; }
    sk = sp / px;
  }
  function geometry() {
    dirty = false;
    R = R0 * zoom;
    sinT = Math.sin(tilt * RAD); cosT = Math.cos(tilt * RAD);
    var Rs = R * sk, Cs = sp / 2, lim = 1 + 2 / Rs, list = [];
    for (var j = 0; j < sp; j++) {
      var y = (Cs - (j + 0.5)) / Rs;
      for (var i = 0; i < sp; i++) { var x = (i + 0.5 - Cs) / Rs; if (x * x + y * y <= lim) list.push(j * sp + i); }
    }
    N = list.length; pIdx = new Int32Array(list);
    pLon = new Float32Array(N); pLat = new Float32Array(N); pSinLat = new Float32Array(N); pCosLat = new Float32Array(N);
    pCosL = new Float32Array(N); pSinL = new Float32Array(N); pShade = new Float32Array(N); pAlpha = new Uint8ClampedArray(N);
    for (var k = 0; k < N; k++) {
      var p = pIdx[k], jj = (p / sp) | 0, ii = p - jj * sp;
      var xx = (ii + 0.5 - Cs) / Rs, yy = (Cs - (jj + 0.5)) / Rs, rr = xx * xx + yy * yy, d = Math.sqrt(rr);
      if (rr > 1) { xx /= d; yy /= d; rr = 1; }
      var z = Math.sqrt(Math.max(0, 1 - rr)), sl = z * sinT + yy * cosT;
      var lat = Math.asin(Math.max(-1, Math.min(1, sl))), lr = Math.atan2(xx, z * cosT - yy * sinT);
      pLat[k] = lat / RAD; pLon[k] = lr / RAD;
      pSinLat[k] = Math.sin(lat); pCosLat[k] = Math.cos(lat); pCosL[k] = Math.cos(lr); pSinL[k] = Math.sin(lr);
      pShade[k] = 0.72 + 0.28 * z;
      pAlpha[k] = Math.max(0, Math.min(1, (1 - d) * Rs + 0.5)) * 255;
    }
    var data = img.data; for (var q = 3; q < data.length; q += 4) data[q] = 0;
  }

  /* Güneş'in tepe noktası (USNO yaklaşığı) */
  function sun(ms) {
    var d = ms / 86400000 + 2440587.5 - 2451545.0, g = (357.529 + 0.98560028 * d) * RAD, q = 280.459 + 0.98564736 * d;
    var L = (q + 1.915 * Math.sin(g) + 0.02 * Math.sin(2 * g)) * RAD, e = (23.439 - 0.00000036 * d) * RAD;
    var ra = Math.atan2(Math.cos(e) * Math.sin(L), Math.cos(L)) / RAD, dec = Math.asin(Math.sin(e) * Math.sin(L));
    var eqt = (((q - ra) % 360) + 540) % 360 - 180, utcH = (((ms % 86400000) + 86400000) % 86400000) / 3600000;
    var lon = ((-15 * (utcH - 12) - eqt + 540) % 360) - 180;
    return { dec: dec, lon: lon, sd: Math.sin(dec), cd: Math.cos(dec) };
  }
  var smooth = function (c) { var t = (c + 0.1) / 0.16; return t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t); };

  /* ---------- harita (Web Mercator) ---------- */
  var mapA = 0; // 0 = küre, 1 = harita
  var mv = { W: 0, cx: 0, cy: 0, z: 0 };
  function mercY(lat) { lat = Math.max(-85.05, Math.min(85.05, lat)) * RAD; return 0.5 - Math.log(Math.tan(Math.PI / 4 + lat / 2)) / (2 * Math.PI); }
  function mercLat(y) { return Math.atan(Math.sinh(Math.PI * (1 - 2 * y))) / RAD; }
  function mapZ() { return Math.log(2 * Math.PI * R0 * zoom / 256) / Math.LN2 + (zadj || 0); }
  function setMv() {
    var z = Math.min(ZMAX_TILE, mapZ());
    mv.z = z; mv.W = 256 * Math.pow(2, z);
    mv.cx = (lon0 + 180) / 360 * mv.W; mv.cy = mercY(tilt) * mv.W;
  }
  function freezeScale() { zadj = Math.log(Math.cos(Math.max(-80, Math.min(80, tilt)) * RAD)) / Math.LN2; } // yer ölçeği geçişte eşleşsin
  function updateMode() {
    var a = Math.max(0, Math.min(1, (zoom - Z_MAP0) / (Z_MAP1 - Z_MAP0)));
    if (a > 0 && zadj === null) freezeScale();
    if (a === 0) zadj = null;
    mapA = a;
    if (attr) attr.hidden = a < 0.02;
    fig.classList.toggle("is-zoomed", zoom > 1.01);
    if (resetBtn) resetBtn.hidden = zoom <= 1.01;
    if (zoomOut) zoomOut.disabled = zoom <= 1.001;
    if (zoomIn) zoomIn.disabled = a > 0 && mapZ() >= ZMAX_TILE - 0.01;
  }

  var tiles = {}, tileCount = 0, active = 0, queue = [], stamp = 0, MAXACT = 6;
  function tileKey(z, x, y) { return z + "/" + x + "/" + y; }
  function pump() {
    while (active < MAXACT && queue.length) {
      var t = queue.shift();
      if (t.s !== 0) continue;
      t.s = 1; active++;
      (function (t) {
        var im2 = new Image();
        im2.crossOrigin = "anonymous";
        im2.onload = function () { t.s = 2; t.img = im2; active--; pending = true; kick(); pump(); };
        im2.onerror = function () { t.s = 3; t.at = Date.now(); active--; pump(); };
        im2.src = TILE_URL + t.z + "/" + t.x + "/" + t.y + ".png";
      })(t);
    }
  }
  function want(z, x, y, prio) {
    var k = tileKey(z, x, y), t = tiles[k];
    if (!t) { t = tiles[k] = { z: z, x: x, y: y, s: 0, img: null, used: 0, at: 0 }; tileCount++; queue.push(t); }
    else if (t.s === 3 && Date.now() - t.at > 20000) { t.s = 0; queue.push(t); }
    t.used = stamp; t.prio = prio;
    return t;
  }
  function evict() {
    if (tileCount < 420) return;
    var arr = Object.keys(tiles).map(function (k) { return tiles[k]; }).filter(function (t) { return t.used < stamp - 1 && t.s !== 1; }).sort(function (a, b) { return a.used - b.used; });
    for (var i = 0; i < arr.length && tileCount > 300; i++) { delete tiles[tileKey(arr[i].z, arr[i].x, arr[i].y)]; tileCount--; }
  }
  function drawTiles(alpha) {
    var zi = Math.max(0, Math.min(19, Math.round(mv.z))), n = Math.pow(2, zi), ts = 256 * Math.pow(2, mv.z - zi);
    var x0 = Math.floor((mv.cx - C) / ts), x1 = Math.floor((mv.cx + C) / ts), y0 = Math.max(0, Math.floor((mv.cy - C) / ts)), y1 = Math.min(n - 1, Math.floor((mv.cy + C) / ts));
    stamp++;
    var list = [];
    for (var ty = y0; ty <= y1; ty++) for (var tx = x0; tx <= x1; tx++) {
      var left = C + tx * ts - mv.cx, top = C + ty * ts - mv.cy;
      list.push({ tx: tx, ty: ty, left: left, top: top, d: Math.abs(left + ts / 2 - C) + Math.abs(top + ts / 2 - C) });
    }
    list.sort(function (a, b) { return a.d - b.d; });
    ctx.save(); ctx.globalAlpha = alpha;
    ctx.fillStyle = "rgb(170,200,225)"; ctx.fillRect(0, 0, px, px);
    list.forEach(function (it) {
      var wx = ((it.tx % n) + n) % n, t = want(zi, wx, it.ty, it.d);
      var L = Math.floor(it.left), T = Math.floor(it.top), Wd = Math.ceil(it.left + ts) - L, Ht = Math.ceil(it.top + ts) - T;
      if (t.s === 2) { ctx.drawImage(t.img, L, T, Wd, Ht); return; }
      for (var d = 1; d <= 5 && zi - d >= 0; d++) { // yüklenirken üst düzeydeki karodan kes
        var a = tiles[tileKey(zi - d, wx >> d, it.ty >> d)];
        if (a && a.s === 2) {
          var sw = 256 / (1 << d), sx = (wx & ((1 << d) - 1)) * sw, sy = (it.ty & ((1 << d) - 1)) * sw;
          ctx.drawImage(a.img, sx, sy, sw, sw, L, T, Wd, Ht); break;
        }
      }
    });
    ctx.restore();
    queue.sort(function (a, b) { return (a.prio || 0) - (b.prio || 0); });
    pump(); evict();
  }

  /* ---------- izdüşüm ---------- */
  function projOrtho(lat, lon) {
    var f = lat * RAD, l = (lon - lon0) * RAD, cf = Math.cos(f), cl = Math.cos(l);
    return { x: C + cf * Math.sin(l) * R, y: C - (cosT * Math.sin(f) - sinT * cf * cl) * R, z: sinT * Math.sin(f) + cosT * cf * cl };
  }
  function projMerc(lat, lon) {
    var dx = (lon - lon0) / 360 * mv.W;
    dx = ((dx + mv.W / 2) % mv.W + mv.W) % mv.W - mv.W / 2;
    return { x: C + dx, y: C + mercY(lat) * mv.W - mv.cy, z: 1 };
  }
  function proj(lat, lon) { return mapA >= 0.5 ? projMerc(lat, lon) : projOrtho(lat, lon); }
  function unproject(ax, ay) { // ekran noktası -> {lat, lon} (küre dışında null)
    if (mapA >= 0.5) { var W = mv.W; return { lon: lon0 + (ax - C) / W * 360, lat: mercLat((mv.cy + ay - C) / W) }; }
    var xx = (ax - C) / R, yy = (C - ay) / R, rr = xx * xx + yy * yy;
    if (rr > 1) return null;
    var z = Math.sqrt(1 - rr), sl = z * sinT + yy * cosT;
    return { lat: Math.asin(Math.max(-1, Math.min(1, sl))) / RAD, lon: lon0 + Math.atan2(xx, z * cosT - yy * sinT) / RAD };
  }

  /* ---------- çizim ---------- */
  function halo(txt, x, y, fill, w) {
    ctx.lineJoin = "round"; ctx.lineWidth = (w || 3) * dpr; ctx.strokeStyle = col.halo; ctx.strokeText(txt, x, y);
    ctx.fillStyle = fill; ctx.fillText(txt, x, y);
  }
  function paintSurface(s) {
    var data = img.data, sd = s.sd, cd = s.cd, D = (lon0 - s.lon) * RAD, cD = Math.cos(D), sD = Math.sin(D);
    var base = lon0 + 540;
    for (var k = 0; k < N; k++) {
      var o = pIdx[k] * 4, r, g, b;
      if (tex) {
        var u = (((pLon[k] + base) % 360) / 360) * TW - 0.5, v = (90 - pLat[k]) / 180 * TH - 0.5;
        var c0 = Math.floor(u), fx = u - c0, r0 = Math.floor(v), fy = v - r0;
        c0 = (c0 + TW) % TW; var c1 = (c0 + 1) % TW;
        if (r0 < 0) { r0 = 0; fy = 0; } else if (r0 >= TH - 1) { r0 = TH - 2; fy = 1; }
        var a0 = (r0 * TW) * 4, a1 = a0 + TW * 4, i00 = a0 + c0 * 4, i01 = a0 + c1 * 4, i10 = a1 + c0 * 4, i11 = a1 + c1 * 4;
        var w00 = (1 - fx) * (1 - fy), w01 = fx * (1 - fy), w10 = (1 - fx) * fy, w11 = fx * fy;
        r = tex[i00] * w00 + tex[i01] * w01 + tex[i10] * w10 + tex[i11] * w11;
        g = tex[i00 + 1] * w00 + tex[i01 + 1] * w01 + tex[i10 + 1] * w10 + tex[i11 + 1] * w11;
        b = tex[i00 + 2] * w00 + tex[i01 + 2] * w01 + tex[i10 + 2] * w10 + tex[i11 + 2] * w11;
      } else { r = 110; g = 160; b = 200; }
      var cz = pSinLat[k] * sd + pCosLat[k] * cd * (pCosL[k] * cD - pSinL[k] * sD), t = smooth(cz), sh = pShade[k];
      // gece tarafı: koyu mavi ton, doku hafifçe seçilir
      data[o] = (r * 0.2 + 6 + (r - r * 0.2 - 6) * t) * sh;
      data[o + 1] = (g * 0.2 + 12 + (g - g * 0.2 - 12) * t) * sh;
      data[o + 2] = (b * 0.24 + 30 + (b - b * 0.24 - 30) * t) * sh;
      data[o + 3] = pAlpha[k];
    }
    sctx.putImageData(img, 0, 0);
    ctx.save(); ctx.imageSmoothingEnabled = true; ctx.imageSmoothingQuality = "high";
    ctx.drawImage(sc, 0, 0, px, px); ctx.restore();
  }
  var nsc = doc.createElement("canvas"), nctx = nsc.getContext("2d"), NG = 32, nimg = nctx.createImageData(NG, NG);
  nsc.width = nsc.height = NG;
  function nightOverlay(s, alpha) { // harita üstünde gündüz/gece (düşük çözünürlükte hesaplanır, yumuşakça büyütülür)
    var d = nimg.data;
    for (var j = 0; j < NG; j++) {
      var lat = mercLat((mv.cy + ((j + 0.5) / NG * px - C)) / mv.W) * RAD, sl = Math.sin(lat), cl = Math.cos(lat);
      for (var i = 0; i < NG; i++) {
        var lon = (lon0 + (((i + 0.5) / NG * px - C) / mv.W) * 360) * RAD, cz = sl * s.sd + cl * s.cd * Math.cos(lon - s.lon * RAD);
        var o = (j * NG + i) * 4; d[o] = 6; d[o + 1] = 14; d[o + 2] = 40; d[o + 3] = 150 * (1 - smooth(cz));
      }
    }
    nctx.putImageData(nimg, 0, 0);
    ctx.save(); ctx.globalAlpha = alpha; ctx.imageSmoothingEnabled = true; ctx.imageSmoothingQuality = "high"; ctx.drawImage(nsc, 0, 0, px, px); ctx.restore();
  }

  function globeLines(alpha) { // 15° saat dilimi meridyenleri, ekvator, UTC etiketleri (küre)
    ctx.save(); ctx.globalAlpha = alpha;
    for (var m = -12; m < 12; m++) {
      var lon = m * 15, zero = m === 0;
      ctx.beginPath(); var pen = false;
      for (var lat = -78; lat <= 78; lat += 3) {
        var p = projOrtho(lat, lon);
        if (p.z > 0) { if (pen) ctx.lineTo(p.x, p.y); else ctx.moveTo(p.x, p.y); pen = true; } else pen = false;
      }
      ctx.strokeStyle = zero ? "#c62828" : col.grid; ctx.lineWidth = (zero ? 2.2 : 1) * dpr; ctx.stroke();
    }
    ctx.beginPath(); var on = false;
    for (var lo = -180; lo <= 180; lo += 4) { var q = projOrtho(0, lo); if (q.z > 0) { if (on) ctx.lineTo(q.x, q.y); else ctx.moveTo(q.x, q.y); on = true; } else on = false; }
    ctx.strokeStyle = col.grid; ctx.lineWidth = 1 * dpr; ctx.stroke();
    ctx.font = "600 " + Math.round(10.5 * dpr) + "px system-ui, sans-serif"; ctx.textAlign = "center"; ctx.textBaseline = "top";
    for (var n = -12; n < 12; n++) {
      var e = projOrtho(-3, n * 15);
      if (e.z < 0.45) continue;
      ctx.globalAlpha = alpha * Math.min(1, (e.z - 0.45) / 0.25);
      halo(n === 0 ? "UTC" : "UTC" + (n > 0 ? "+" : "−") + Math.abs(n), e.x, e.y, n === 0 ? "#c62828" : col.label);
    }
    ctx.restore();
  }
  function fmtLon(lon, step) { // 28°58′E gibi
    var hemi = lon < 0 ? "W" : "E", a = Math.abs(lon);
    if (step >= 1) return Math.round(a) + "°" + hemi;
    var deg = Math.floor(a + 1e-9), mins = (a - deg) * 60;
    if (step >= 1 / 60 - 1e-9) return deg + "°" + Math.round(mins) + "′" + hemi;
    var m = Math.floor(mins + 1e-9), secs = (mins - m) * 60;
    return deg + "°" + m + "′" + (step >= 1 / 3600 ? Math.round(secs) : secs.toFixed(1)) + "″" + hemi;
  }
  var STEPS = [0.00005, 0.0001, 0.00025, 0.0005, 0.001, 0.0025, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2, 5, 15];
  function mapLines(alpha) { // meridyen ve meridyen parçaları: 15° çizgileri koyu, ara çizgiler ince; 0° kırmızı
    var perDeg = mv.W / 360, step = 15;
    for (var i = 0; i < STEPS.length; i++) if (STEPS[i] * perDeg >= 80 * dpr) { step = STEPS[i]; break; }
    var lonL = lon0 - C / perDeg, lonR = lon0 + C / perDeg, k0 = Math.ceil(lonL / step), k1 = Math.floor(lonR / step);
    var occupied = [];
    ctx.save(); ctx.globalAlpha = alpha; ctx.font = "600 " + Math.round(11 * dpr) + "px system-ui, sans-serif"; ctx.textAlign = "center"; ctx.textBaseline = "top";
    for (var k = k0; k <= k1 && k - k0 < 60; k++) {
      var lon = k * step, x = C + (lon - lon0) * perDeg, h15 = lon / 15, major = Math.abs(h15 - Math.round(h15)) < 1e-7;
      var nl = ((lon + 540) % 360) - 180, zero = Math.abs(nl) < 1e-9;
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, px);
      if (zero) { ctx.strokeStyle = "#c62828"; ctx.lineWidth = 2.6 * dpr; ctx.setLineDash([]); }
      else if (major) { ctx.strokeStyle = "rgba(20,70,140,0.85)"; ctx.lineWidth = 2 * dpr; ctx.setLineDash([]); }
      else { ctx.strokeStyle = "rgba(40,40,40,0.5)"; ctx.lineWidth = 1 * dpr; ctx.setLineDash([5 * dpr, 4 * dpr]); }
      ctx.stroke(); ctx.setLineDash([]);
      var txt = fmtLon(nl, step);
      if (major) { var off = Math.round(nl / 15); txt = (off === 0 ? "UTC" : "UTC" + (off > 0 ? "+" : "−") + Math.abs(off)) + " · " + txt; }
      halo(txt, x, 6 * dpr, zero ? "#c62828" : major ? "#14468c" : col.label);
      var tw = ctx.measureText(txt).width; occupied.push({ x: x - tw / 2 - 2, y: 4 * dpr, w: tw + 4, h: 16 * dpr });
    }
    ctx.restore();
    return occupied;
  }

  function labels(sec, occupied) {
    var map = mapA >= 0.5, extra = zoom >= 1.25 && placesState === 2;
    var budget = extra ? Math.round(8 + Math.min(24, zoom * 2.2) * (px / 700)) : 0;
    var cands = [];
    for (var i = 0; i < places.length; i++) {
      var pl = places[i];
      if (!pl.m && !extra) { if (i >= featured.length) break; continue; }
      var p = proj(pl.lat, pl.lon);
      if (map ? (p.x < 4 * dpr || p.x > px - 4 * dpr || p.y < 8 * dpr || p.y > px - 8 * dpr) : p.z < 0.12) continue;
      cands.push({ pl: pl, p: p });
    }
    // öncelik: seçili şehirler; sonra büyüklük sırası (liste nüfusa göre sıralı, sort kararlı)
    cands.sort(function (a, b) { return b.pl.m - a.pl.m; });
    var boxes = occupied ? occupied.slice() : [], shown = 0;
    ctx.save(); ctx.font = "600 " + Math.round(11.5 * dpr) + "px system-ui, sans-serif"; ctx.textBaseline = "middle";
    for (var c = 0; c < cands.length; c++) {
      var it = cands[c], pl2 = it.pl, p2 = it.p;
      if (!pl2.m && shown >= budget) break;
      var fade = map ? 1 : Math.min(1, (p2.z - 0.12) / 0.2);
      var txt = pl2.n + " " + localTime(pl2.tz, sec), w = ctx.measureText(txt).width, h = 14 * dpr;
      var left = map ? p2.x > px - w - 14 * dpr : (rtl ? p2.x > C - R * 0.45 : p2.x > C + R * 0.45);
      var x = left ? p2.x - 6 * dpr - w : p2.x + 6 * dpr;
      if (x < 2 * dpr || x + w > px - 2 * dpr) continue; // kenarda kesilecek etiketi çizme
      var box = { x: x - 2, y: p2.y - h / 2, w: w + 4, h: h };
      var hit = boxes.some(function (b) { return box.x < b.x + b.w && b.x < box.x + box.w && box.y < b.y + b.h && b.y < box.y + box.h; });
      ctx.globalAlpha = fade;
      ctx.beginPath(); ctx.arc(p2.x, p2.y, (pl2.m ? 3.2 : 2.4) * dpr, 0, 2 * Math.PI);
      ctx.fillStyle = pl2.m ? col.accent : "#33415c"; ctx.fill(); ctx.lineWidth = 1.2 * dpr; ctx.strokeStyle = col.halo; ctx.stroke();
      if (hit) continue;
      boxes.push(box); if (!pl2.m) shown++;
      ctx.textAlign = "left"; halo(txt, x, p2.y, col.label);
    }
    ctx.restore();
  }

  function draw() {
    if (!px) return;
    updateMode();
    var ms = S.nowMs(), s = sun(ms), sec = Math.floor(ms / 1000);
    ctx.clearRect(0, 0, px, px);
    if (mapA < 1) { if (dirty) geometry(); paintSurface(s); }
    if (mapA > 0) { setMv(); drawTiles(mapA); nightOverlay(s, mapA); }
    if (mapA < 1) globeLines(1 - mapA);
    var occ = mapA > 0 ? mapLines(mapA) : null;
    if (mapA < 0.5) { // Güneş işareti ve küre kenarı
      var sp2 = projOrtho(s.dec / RAD, s.lon);
      if (sp2.z > 0) {
        var gr = ctx.createRadialGradient(sp2.x, sp2.y, 0, sp2.x, sp2.y, 14 * dpr);
        gr.addColorStop(0, "rgba(255,214,90,0.95)"); gr.addColorStop(0.35, "rgba(255,200,60,0.55)"); gr.addColorStop(1, "rgba(255,200,60,0)");
        ctx.fillStyle = gr; ctx.beginPath(); ctx.arc(sp2.x, sp2.y, 14 * dpr, 0, 2 * Math.PI); ctx.fill();
      }
      if (R < px) { ctx.beginPath(); ctx.arc(C, C, R, 0, 2 * Math.PI); ctx.strokeStyle = col.rim; ctx.lineWidth = 1.5 * dpr; ctx.stroke(); }
    }
    labels(sec, occ);
  }

  /* ---------- döngü ---------- */
  var visible = true, last = 0, raf = 0, dragging = false, resumeAt = 0, lastDraw = 0;
  function frame(ts) {
    raf = 0;
    if (!visible || doc.hidden) return;
    if (ts - last >= 33) {
      var dt = last ? Math.min(0.1, (ts - last) / 1000) : 0;
      last = ts;
      var spin = !reduce && !dragging && zoom < 1.2 && Date.now() >= resumeAt;
      if (spin) lon0 -= SPEED * dt;
      lon0 = ((lon0 + 540) % 360) - 180;
      if (spin || pending || ts - lastDraw >= 1000) { pending = false; lastDraw = ts; draw(); }
    }
    if (!reduce) raf = requestAnimationFrame(frame);
  }
  function kick() { if (!raf) { last = 0; raf = requestAnimationFrame(frame); } }
  function redraw() { pending = true; if (reduce) { pending = false; draw(); } else kick(); }

  if ("IntersectionObserver" in window) new IntersectionObserver(function (en) { visible = en[0].isIntersecting; if (visible) kick(); }).observe(fig);
  doc.addEventListener("visibilitychange", function () { if (!doc.hidden) kick(); });
  if (reduce) setInterval(function () { if (visible && !doc.hidden) draw(); }, 30000);

  /* ---------- etkileşim ---------- */
  var ZMAX_GLOBE = 1e5;
  function zoomLimit() { // harita düzeyinde en çok ZMAX_TILE
    if (zadj === null) return ZMAX_GLOBE;
    return Math.min(ZMAX_GLOBE, Math.pow(2, ZMAX_TILE - zadj) * 256 / (2 * Math.PI * R0));
  }
  function setZoom(z, ax, ay) {
    z = Math.max(1, z);
    var anchor = (ax != null && (z > Z_MAP0 || zoom > Z_MAP0)) ? unproject(ax, ay) : null;
    if (z > Z_MAP0 && zadj === null) freezeScale();
    z = Math.min(z, zoomLimit());
    if (Math.abs(z - zoom) < 1e-6) return;
    zoom = z; dirty = true;
    if (zoom > 1.12) ensurePlaces();
    if (anchor && zoom > Z_MAP0) { // işaret edilen nokta yerinde kalsın (Mercator'da)
      var W = 256 * Math.pow(2, Math.min(ZMAX_TILE, mapZ())), wx = (anchor.lon + 180) / 360 * W, wy = mercY(anchor.lat) * W;
      lon0 = (wx - (ax - C)) / W * 360 - 180; tilt = Math.max(-80, Math.min(80, mercLat((wy - (ay - C)) / W)));
      lon0 = ((lon0 + 540) % 360) - 180;
    }
    if (zoom <= 1.001) { zoom = 1; tilt = TILT0; }
    redraw();
  }
  function toCanvas(e) { var r = canvas.getBoundingClientRect(); return { x: (e.clientX - r.left) / r.width * px, y: (e.clientY - r.top) / r.height * px }; }
  var pts = {}, lastX = 0, lastY = 0, pinch0 = 0, zoom0 = 1;
  function pcount() { return Object.keys(pts).length; }
  function pair() { var k = Object.keys(pts); return [pts[k[0]], pts[k[1]]]; }
  canvas.addEventListener("pointerdown", function (e) {
    ensurePlaces();
    pts[e.pointerId] = { x: e.clientX, y: e.clientY };
    try { canvas.setPointerCapture(e.pointerId); } catch (x) {}
    dragging = true; lastX = e.clientX; lastY = e.clientY;
    if (pcount() === 2) { var q = pair(); pinch0 = Math.hypot(q[0].x - q[1].x, q[0].y - q[1].y); zoom0 = zoom; }
  });
  canvas.addEventListener("pointermove", function (e) {
    if (!pts[e.pointerId]) return;
    pts[e.pointerId] = { x: e.clientX, y: e.clientY };
    if (pcount() >= 2) {
      if (!pinch0) return;
      var q = pair(), d = Math.hypot(q[0].x - q[1].x, q[0].y - q[1].y), mid = toCanvas({ clientX: (q[0].x + q[1].x) / 2, clientY: (q[0].y + q[1].y) / 2 });
      setZoom(zoom0 * d / pinch0, mid.x, mid.y); return;
    }
    var dx = (e.clientX - lastX) * dpr, dy = (e.clientY - lastY) * dpr;
    lastX = e.clientX; lastY = e.clientY;
    if (mapA >= 0.5) { setMv(); lon0 -= dx / mv.W * 360; tilt = Math.max(-80, Math.min(80, mercLat((mv.cy - dy) / mv.W))); }
    else { var k = 1 / R / RAD; lon0 -= dx * k; if (zoom > 1.01 || e.pointerType === "mouse") { tilt = Math.max(-80, Math.min(80, tilt + dy * k)); dirty = true; } }
    redraw();
  });
  function endPointer(e) {
    delete pts[e.pointerId];
    if (pcount() < 2) pinch0 = 0;
    if (pcount() === 1) { var k = Object.keys(pts)[0]; lastX = pts[k].x; lastY = pts[k].y; }
    if (!pcount() && dragging) { dragging = false; resumeAt = Date.now() + 2500; }
  }
  canvas.addEventListener("pointerup", endPointer);
  canvas.addEventListener("pointercancel", endPointer);
  canvas.addEventListener("wheel", function (e) {
    if (!e.ctrlKey && zoom <= 1.01) return; // sayfa kaydırması küreye takılmasın
    e.preventDefault();
    var p = toCanvas(e);
    setZoom(zoom * Math.exp(-e.deltaY * (e.ctrlKey ? 0.01 : 0.0018)), p.x, p.y);
  }, { passive: false });
  canvas.addEventListener("dblclick", function (e) { var p = toCanvas(e); setZoom(zoom * 2, p.x, p.y); });
  if (zoomIn) zoomIn.addEventListener("click", function () { ensurePlaces(); setZoom(zoom * 2, C, C); });
  if (zoomOut) zoomOut.addEventListener("click", function () { setZoom(zoom / 2, C, C); });
  if (resetBtn) resetBtn.addEventListener("click", function () { zoom = 1; zadj = null; tilt = TILT0; dirty = true; resumeAt = 0; redraw(); });

  new MutationObserver(function () { readColors(); redraw(); }).observe(doc.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
  if (window.matchMedia) {
    var mq = matchMedia("(prefers-color-scheme: dark)"), onMq = function () { readColors(); redraw(); };
    if (mq.addEventListener) mq.addEventListener("change", onMq); else if (mq.addListener) mq.addListener(onMq);
  }
  var rt = 0;
  window.addEventListener("resize", function () { clearTimeout(rt); rt = setTimeout(function () { setup(); redraw(); }, 150); });

  readColors(); setup(); updateMode(); draw(); kick();
  window.sthGlobe = { state: function () { return { zoom: zoom, lon: lon0, lat: tilt, mapA: mapA, z: mv.z, places: places.length, tiles: tileCount }; }, set: function (lat, lon, z) { if (z) setZoom(z); lon0 = lon; tilt = lat; dirty = true; redraw(); } };
})();
