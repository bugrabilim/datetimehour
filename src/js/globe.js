/* Ana sayfa: tam sayfa dünya küresi. Uzakken yavaş dönen kabartmalı küre (gündüz/gece, 15°'lik saat dilimi meridyenleri,
   şehirlerin yerel saati); yakınlaştıkça o bölgedeki yerlerin saati de görünür; daha da yakınlaşınca OpenStreetMap
   haritasına geçilir (sokak düzeyine kadar), meridyen ve meridyen parçaları boylamlarıyla çizilir.
   Yüzey WebGL ile çizilir (doku doğrudan GPU'ya yüklenir, piksel okuma yok); WebGL yoksa yazılımla çizilir.
   Küre dokusu Natural Earth (kamu malı); harita karoları yalnız harita düzeyine inildiğinde OpenStreetMap'ten gelir. */
(function () {
  "use strict";
  var doc = document, S = window.sth;
  var fig = doc.querySelector("[data-globe]");
  if (!fig || !S) return;
  var view = fig.querySelector(".globe-view");
  var canvas = fig.querySelector("canvas.globe-fx"), glc = fig.querySelector("canvas.globe-gl");
  var ctx = canvas && canvas.getContext && canvas.getContext("2d");
  if (!ctx || !view) return;
  var G = S.cfg.globe || {};
  var gfs = fig.querySelector("[data-globe-fs]");
  if (gfs && S.makeFullscreen) S.makeFullscreen(fig, gfs);
  var RAD = Math.PI / 180;
  var TILT0 = 23, SPEED = 3; // başlangıç enlemi, derece/sn (bir tur 2 dk)
  var ZMAX_TILE = 19.2;
  var TILE_URL = "https://tile.openstreetmap.org/";
  var reduce = window.matchMedia && matchMedia("(prefers-reduced-motion: reduce)").matches;
  var rtl = doc.documentElement.dir === "rtl";

  var lon0 = -new Date().getTimezoneOffset() / 4 - 10; // başlangıçta cihazın bölgesi görünür
  var tilt = TILT0, zoom = 1, zadj = null;
  var pw = 0, ph = 0, cx = 0, cy = 0, dpr = 1, R0 = 0, R = 0, ZM0 = 2.2, ZM1 = 3.0; // piksel boyutu, merkez, küre yarıçapı, haritaya geçiş aralığı
  var col = {}, dirty = true, pending = true;
  var attr = fig.querySelector("[data-globe-attr]");
  var zoomIn = fig.querySelector('[data-globe-zoom="in"]'), zoomOut = fig.querySelector('[data-globe-zoom="out"]'), resetBtn = fig.querySelector("[data-globe-reset]");

  /* ---------- renkler ---------- */
  function readColors() {
    var cs = getComputedStyle(fig), g = function (k, fb) { return (cs.getPropertyValue(k) || "").trim() || fb; };
    col.grid = g("--globe-grid", "rgba(255,255,255,.5)"); col.label = g("--globe-label", "#111"); col.halo = g("--globe-halo", "#fff");
    col.accent = g("--accent", "#c60"); col.rim = g("--border", "#999");
  }

  /* ---------- WebGL yüzey ---------- */
  var gl = null, glOk = false, glProg = null, glU = {}, texFull = null, texSmall = null, isGL2 = false, glCleared = false;
  var VS = "attribute vec2 a;void main(){gl_Position=vec4(a,0.,1.);}";
  var FS = [
    "#ifdef GL_FRAGMENT_PRECISION_HIGH", "precision highp float;", "#else", "precision mediump float;", "#endif",
    "uniform vec2 uRes;uniform float uR;uniform float uSinT;uniform float uCosT;uniform float uLon0;uniform vec2 uSun;uniform sampler2D uTex;uniform float uHas;",
    "const float PI=3.14159265358979;",
    "void main(){",
    "vec2 p=(gl_FragCoord.xy-0.5*uRes)/uR;float rr=dot(p,p);",
    "if(rr>1.01){gl_FragColor=vec4(0.);return;}",
    "float d=sqrt(rr);if(d>1.0){p=p/d;rr=1.0;}",
    "float z=sqrt(max(0.,1.-rr));float sl=z*uSinT+p.y*uCosT;float lat=asin(clamp(sl,-1.,1.));",
    "float lon=uLon0+atan(p.x,z*uCosT-p.y*uSinT);",
    "vec3 c=vec3(0.43,0.63,0.78);",
    "if(uHas>0.5){c=texture2D(uTex,vec2(lon/(2.*PI)+0.5,0.5-lat/PI)).rgb;}",
    "float cz=sin(lat)*sin(uSun.y)+cos(lat)*cos(uSun.y)*cos(lon-uSun.x);",
    "float t=clamp((cz+0.1)/0.16,0.,1.);t=t*t*(3.-2.*t);",
    "float sh=0.72+0.28*z;",
    "vec3 n=vec3(c.r*0.2+6./255.,c.g*0.2+12./255.,c.b*0.24+30./255.);",
    "vec3 o=mix(n,c,t)*sh;",
    "float a=clamp((1.-sqrt(rr))*uR+0.5,0.,1.);",
    "gl_FragColor=vec4(o*a,a);}"
  ].join("\n");
  function shader(type, src) {
    var s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s);
    if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
    return s;
  }
  function initGL() {
    glOk = false;
    if (!glc) return;
    try {
      var o = { antialias: false, alpha: true, premultipliedAlpha: true, depth: false, stencil: false };
      gl = glc.getContext("webgl2", o); isGL2 = !!gl;
      if (!gl) gl = glc.getContext("webgl", o) || glc.getContext("experimental-webgl", o);
      if (!gl) return;
      glProg = gl.createProgram();
      gl.attachShader(glProg, shader(gl.VERTEX_SHADER, VS)); gl.attachShader(glProg, shader(gl.FRAGMENT_SHADER, FS));
      gl.linkProgram(glProg);
      if (!gl.getProgramParameter(glProg, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(glProg));
      gl.useProgram(glProg);
      ["uRes", "uR", "uSinT", "uCosT", "uLon0", "uSun", "uTex", "uHas"].forEach(function (n) { glU[n] = gl.getUniformLocation(glProg, n); });
      var buf = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buf);
      gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
      var loc = gl.getAttribLocation(glProg, "a"); gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
      gl.disable(gl.DEPTH_TEST); gl.disable(gl.BLEND);
      glOk = true;
    } catch (e) { gl = null; glOk = false; if (window.console) console.warn("globe: WebGL kullanılamadı, yazılım çizimine geçildi", e); }
  }
  function makeTex(src) {
    var t = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, t);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, src);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, isGL2 ? gl.REPEAT : gl.CLAMP_TO_EDGE); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    return t;
  }

  /* ---------- doku (Natural Earth) ---------- */
  var texImg = null, tex = null, TW = 0, TH = 0; // tex: yazılım yolu için piksel verisi
  function uploadTex() {
    if (!texImg) return;
    if (glOk) {
      try {
        texFull = makeTex(texImg);
        var sc2 = doc.createElement("canvas"); sc2.width = Math.round(texImg.naturalWidth / 2); sc2.height = Math.round(texImg.naturalHeight / 2);
        var x2 = sc2.getContext("2d"); x2.imageSmoothingQuality = "high"; x2.drawImage(texImg, 0, 0, sc2.width, sc2.height);
        texSmall = makeTex(sc2);
      } catch (e) { glOk = false; if (window.console) console.warn("globe: doku GPU'ya yüklenemedi", e); }
    }
    if (!glOk) {
      try {
        var c = doc.createElement("canvas"); TW = c.width = texImg.naturalWidth; TH = c.height = texImg.naturalHeight;
        var x = c.getContext("2d"); x.drawImage(texImg, 0, 0);
        tex = x.getImageData(0, 0, TW, TH).data;
      } catch (e) { tex = null; if (window.console) console.warn("globe: doku okunamadı", e); }
    }
    pending = true; kick();
  }
  function loadTex(n) {
    if (!G.earth) return;
    var im = new Image();
    im.onload = function () { texImg = im; uploadTex(); };
    im.onerror = function () { if (n < 3) setTimeout(function () { loadTex(n + 1); }, 1500 * (n + 1)); };
    im.src = G.earth + (n ? (G.earth.indexOf("?") > -1 ? "&" : "?") + "r=" + n : "");
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

  /* ---------- boyut ---------- */
  function setup() {
    var cw = view.clientWidth || 320, ch = view.clientHeight || 320;
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    if (cw * ch * dpr * dpr > 3.4e6) dpr = Math.sqrt(3.4e6 / (cw * ch)); // çok büyük ekranlarda piksel sayısını sınırla
    var w = Math.max(64, Math.round(cw * dpr)), h = Math.max(64, Math.round(ch * dpr));
    if (w !== pw || h !== ph) {
      pw = w; ph = h; canvas.width = pw; canvas.height = ph;
      if (glc) { glc.width = pw; glc.height = ph; }
      dirty = true;
    }
    cx = pw / 2; cy = ph / 2;
    R0 = Math.max(60 * dpr, Math.min(pw * 0.49, ph * 0.5 - 8 * dpr) * 0.95); // %5 küçük
    ZM0 = Math.max(2.2, Math.hypot(cx, cy) / R0 * 1.06); ZM1 = ZM0 + 0.8; // küre tüm alanı kaplayınca harita karışmaya başlar
  }

  /* ---------- yazılım yüzeyi (WebGL yoksa) ---------- */
  var sc = doc.createElement("canvas"), sctx = sc.getContext("2d"), sw = 0, sh = 0, sk = 1;
  var img = null, N = 0, pIdx, pLon, pLat, pSinLat, pCosLat, pCosL, pSinL, pShade, pAlpha;
  var sinT = 0, cosT = 1;
  function geometry() { // piksel başına ters izdüşüm (yalnız yazılım yolu; boyut, yakınlaştırma ya da enlem değişince)
    dirty = false;
    sk = Math.min(1, 640 / Math.max(pw, ph)); sw = Math.max(1, Math.round(pw * sk)); sh = Math.max(1, Math.round(ph * sk));
    if (sc.width !== sw || sc.height !== sh) { sc.width = sw; sc.height = sh; img = sctx.createImageData(sw, sh); }
    var Rs = R * sk, Cx = sw / 2, Cy = sh / 2, lim = 1 + 2 / Rs, list = [];
    for (var j = 0; j < sh; j++) {
      var y = (Cy - (j + 0.5)) / Rs;
      for (var i = 0; i < sw; i++) { var x = (i + 0.5 - Cx) / Rs; if (x * x + y * y <= lim) list.push(j * sw + i); }
    }
    N = list.length; pIdx = new Int32Array(list);
    pLon = new Float32Array(N); pLat = new Float32Array(N); pSinLat = new Float32Array(N); pCosLat = new Float32Array(N);
    pCosL = new Float32Array(N); pSinL = new Float32Array(N); pShade = new Float32Array(N); pAlpha = new Uint8ClampedArray(N);
    for (var k = 0; k < N; k++) {
      var p = pIdx[k], jj = (p / sw) | 0, ii = p - jj * sw;
      var xx = (ii + 0.5 - Cx) / Rs, yy = (Cy - (jj + 0.5)) / Rs, rr = xx * xx + yy * yy, d = Math.sqrt(rr);
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
    var a = Math.max(0, Math.min(1, (zoom - ZM0) / (ZM1 - ZM0)));
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
    var x0 = Math.floor((mv.cx - cx) / ts), x1 = Math.floor((mv.cx + cx) / ts), y0 = Math.max(0, Math.floor((mv.cy - cy) / ts)), y1 = Math.min(n - 1, Math.floor((mv.cy + cy) / ts));
    stamp++;
    var list = [];
    for (var ty = y0; ty <= y1; ty++) for (var tx = x0; tx <= x1; tx++) {
      var left = cx + tx * ts - mv.cx, top = cy + ty * ts - mv.cy;
      list.push({ tx: tx, ty: ty, left: left, top: top, d: Math.abs(left + ts / 2 - cx) + Math.abs(top + ts / 2 - cy) });
    }
    list.sort(function (a, b) { return a.d - b.d; });
    ctx.save(); ctx.globalAlpha = alpha;
    ctx.fillStyle = "rgb(170,200,225)"; ctx.fillRect(0, 0, pw, ph);
    list.forEach(function (it) {
      var wx = ((it.tx % n) + n) % n, t = want(zi, wx, it.ty, it.d);
      var L = Math.floor(it.left), T = Math.floor(it.top), Wd = Math.ceil(it.left + ts) - L, Ht = Math.ceil(it.top + ts) - T;
      if (t.s === 2) { ctx.drawImage(t.img, L, T, Wd, Ht); return; }
      for (var d = 1; d <= 5 && zi - d >= 0; d++) { // yüklenirken üst düzeydeki karodan kes
        var a = tiles[tileKey(zi - d, wx >> d, it.ty >> d)];
        if (a && a.s === 2) {
          var sw2 = 256 / (1 << d), sx = (wx & ((1 << d) - 1)) * sw2, sy = (it.ty & ((1 << d) - 1)) * sw2;
          ctx.drawImage(a.img, sx, sy, sw2, sw2, L, T, Wd, Ht); break;
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
    return { x: cx + cf * Math.sin(l) * R, y: cy - (cosT * Math.sin(f) - sinT * cf * cl) * R, z: sinT * Math.sin(f) + cosT * cf * cl };
  }
  function projMerc(lat, lon) {
    var dx = (lon - lon0) / 360 * mv.W;
    dx = ((dx + mv.W / 2) % mv.W + mv.W) % mv.W - mv.W / 2;
    return { x: cx + dx, y: cy + mercY(lat) * mv.W - mv.cy, z: 1 };
  }
  function proj(lat, lon) { return mapA >= 0.5 ? projMerc(lat, lon) : projOrtho(lat, lon); }
  function unproject(ax, ay) { // ekran noktası -> {lat, lon} (küre dışında null)
    if (mapA >= 0.5) { var W = mv.W; return { lon: lon0 + (ax - cx) / W * 360, lat: mercLat((mv.cy + ay - cy) / W) }; }
    var xx = (ax - cx) / R, yy = (cy - ay) / R, rr = xx * xx + yy * yy;
    if (rr > 1) return null;
    var z = Math.sqrt(1 - rr), sl = z * sinT + yy * cosT;
    return { lat: Math.asin(Math.max(-1, Math.min(1, sl))) / RAD, lon: lon0 + Math.atan2(xx, z * cosT - yy * sinT) / RAD };
  }

  /* ---------- çizim ---------- */
  function halo(txt, x, y, fill, w) {
    ctx.lineJoin = "round"; ctx.lineWidth = (w || 3) * dpr; ctx.strokeStyle = col.halo; ctx.strokeText(txt, x, y);
    ctx.fillStyle = fill; ctx.fillText(txt, x, y);
  }
  function drawGL(s) {
    gl.viewport(0, 0, pw, ph);
    gl.clearColor(0, 0, 0, 0); gl.clear(gl.COLOR_BUFFER_BIT);
    gl.uniform2f(glU.uRes, pw, ph); gl.uniform1f(glU.uR, R);
    gl.uniform1f(glU.uSinT, sinT); gl.uniform1f(glU.uCosT, cosT); gl.uniform1f(glU.uLon0, lon0 * RAD);
    gl.uniform2f(glU.uSun, s.lon * RAD, s.dec);
    var t = (2 * R > 1000 * Math.max(1, dpr * 0.8)) ? texFull : texSmall;
    gl.activeTexture(gl.TEXTURE0);
    if (t) { gl.bindTexture(gl.TEXTURE_2D, t); gl.uniform1i(glU.uTex, 0); gl.uniform1f(glU.uHas, 1); } else gl.uniform1f(glU.uHas, 0);
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
    glCleared = false;
  }
  function clearGL() { if (glCleared || !gl) return; gl.viewport(0, 0, pw, ph); gl.clearColor(0, 0, 0, 0); gl.clear(gl.COLOR_BUFFER_BIT); glCleared = true; }
  function paintSurfaceCPU(s) {
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
      var cz = pSinLat[k] * sd + pCosLat[k] * cd * (pCosL[k] * cD - pSinL[k] * sD), t = smooth(cz), sh2 = pShade[k];
      data[o] = (r * 0.2 + 6 + (r - r * 0.2 - 6) * t) * sh2;
      data[o + 1] = (g * 0.2 + 12 + (g - g * 0.2 - 12) * t) * sh2;
      data[o + 2] = (b * 0.24 + 30 + (b - b * 0.24 - 30) * t) * sh2;
      data[o + 3] = pAlpha[k];
    }
    sctx.putImageData(img, 0, 0);
    ctx.save(); ctx.imageSmoothingEnabled = true; ctx.imageSmoothingQuality = "high";
    ctx.drawImage(sc, 0, 0, pw, ph); ctx.restore();
  }
  var nsc = doc.createElement("canvas"), nctx = nsc.getContext("2d"), NG = 36, nimg = nctx.createImageData(NG, NG);
  nsc.width = nsc.height = NG;
  function nightOverlay(s, alpha) { // harita üstünde gündüz/gece (düşük çözünürlükte hesaplanır, yumuşakça büyütülür)
    var d = nimg.data;
    for (var j = 0; j < NG; j++) {
      var lat = mercLat((mv.cy + ((j + 0.5) / NG * ph - cy)) / mv.W) * RAD, sl = Math.sin(lat), cl = Math.cos(lat);
      for (var i = 0; i < NG; i++) {
        var lon = (lon0 + (((i + 0.5) / NG * pw - cx) / mv.W) * 360) * RAD, cz = sl * s.sd + cl * s.cd * Math.cos(lon - s.lon * RAD);
        var o = (j * NG + i) * 4; d[o] = 6; d[o + 1] = 14; d[o + 2] = 40; d[o + 3] = 150 * (1 - smooth(cz));
      }
    }
    nctx.putImageData(nimg, 0, 0);
    ctx.save(); ctx.globalAlpha = alpha; ctx.imageSmoothingEnabled = true; ctx.imageSmoothingQuality = "high"; ctx.drawImage(nsc, 0, 0, pw, ph); ctx.restore();
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
    ctx.font = "700 " + Math.round(Math.max(11, Math.min(15, R0 / dpr / 24)) * dpr) + "px system-ui, sans-serif"; ctx.textAlign = "center"; ctx.textBaseline = "top";
    for (var n = -12; n < 12; n++) {
      var e = projOrtho(-3, n * 15);
      if (e.z < 0.45) continue;
      ctx.globalAlpha = alpha * Math.min(1, (e.z - 0.45) / 0.25);
      halo(n === 0 ? "UTC" : "UTC" + (n > 0 ? "+" : "−") + Math.abs(n), e.x, e.y, n === 0 ? "#c62828" : col.label, 4);
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
    var lonL = lon0 - cx / perDeg, lonR = lon0 + cx / perDeg, k0 = Math.ceil(lonL / step), k1 = Math.floor(lonR / step);
    var occupied = [];
    ctx.save(); ctx.globalAlpha = alpha; ctx.font = "700 " + Math.round(15 * dpr) + "px system-ui, sans-serif"; ctx.textAlign = "center"; ctx.textBaseline = "top";
    for (var k = k0; k <= k1 && k - k0 < 80; k++) {
      var lon = k * step, x = cx + (lon - lon0) * perDeg, h15 = lon / 15, major = Math.abs(h15 - Math.round(h15)) < 1e-7;
      var nl = ((lon + 540) % 360) - 180, zero = Math.abs(nl) < 1e-9;
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, ph);
      if (zero) { ctx.strokeStyle = "#c62828"; ctx.lineWidth = 2.6 * dpr; ctx.setLineDash([]); }
      else if (major) { ctx.strokeStyle = "rgba(20,70,140,0.85)"; ctx.lineWidth = 2 * dpr; ctx.setLineDash([]); }
      else { ctx.strokeStyle = "rgba(40,40,40,0.5)"; ctx.lineWidth = 1 * dpr; ctx.setLineDash([5 * dpr, 4 * dpr]); }
      ctx.stroke(); ctx.setLineDash([]);
      var txt = fmtLon(nl, step);
      if (major) { var off = Math.round(nl / 15); txt = (off === 0 ? "UTC" : "UTC" + (off > 0 ? "+" : "−") + Math.abs(off)) + " · " + txt; }
      halo(txt, x, 6 * dpr, zero ? "#c62828" : major ? "#14468c" : col.label, 4);
      var tw = ctx.measureText(txt).width; occupied.push({ x: x - tw / 2 - 2, y: 4 * dpr, w: tw + 4, h: 22 * dpr });
    }
    ctx.restore();
    return occupied;
  }

  function labels(sec, occupied) {
    var map = mapA >= 0.5, extra = zoom >= 1.25 && placesState === 2;
    var budget = extra ? Math.round(8 + Math.min(24, zoom * 2.2) * (Math.min(pw, ph) / 700)) : 0;
    var cands = [];
    for (var i = 0; i < places.length; i++) {
      var pl = places[i];
      if (!pl.m && !extra) { if (i >= featured.length) break; continue; }
      var p = proj(pl.lat, pl.lon);
      if (map ? (p.x < 4 * dpr || p.x > pw - 4 * dpr || p.y < 8 * dpr || p.y > ph - 8 * dpr) : p.z < 0.12) continue;
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
      var fs = pl2.m ? 17 : 14.5; // şehir saatleri okunaklı olsun
      ctx.font = (pl2.m ? "700 " : "600 ") + Math.round(fs * dpr) + "px system-ui, sans-serif";
      var txt = pl2.n + " " + localTime(pl2.tz, sec), w = ctx.measureText(txt).width, h = (fs + 5) * dpr;
      var left = map ? p2.x > pw - w - 14 * dpr : (rtl ? p2.x > cx - R * 0.45 : p2.x > cx + R * 0.45);
      var x = left ? p2.x - 8 * dpr - w : p2.x + 8 * dpr;
      if (x < 2 * dpr || x + w > pw - 2 * dpr) continue; // kenarda kesilecek etiketi çizme
      var box = { x: x - 2, y: p2.y - h / 2, w: w + 4, h: h };
      var hit = boxes.some(function (b) { return box.x < b.x + b.w && b.x < box.x + box.w && box.y < b.y + b.h && b.y < box.y + box.h; });
      ctx.globalAlpha = fade;
      ctx.beginPath(); ctx.arc(p2.x, p2.y, (pl2.m ? 4.4 : 3.2) * dpr, 0, 2 * Math.PI);
      ctx.fillStyle = pl2.m ? col.accent : "#33415c"; ctx.fill(); ctx.lineWidth = 1.2 * dpr; ctx.strokeStyle = col.halo; ctx.stroke();
      if (hit) continue;
      boxes.push(box); if (!pl2.m) shown++;
      ctx.textAlign = "left"; halo(txt, x, p2.y, col.label, 4);
    }
    ctx.restore();
  }

  function draw() {
    if (!pw) return;
    updateMode();
    var ms = S.nowMs(), s = sun(ms), sec = Math.floor(ms / 1000);
    ctx.clearRect(0, 0, pw, ph);
    R = R0 * zoom; sinT = Math.sin(tilt * RAD); cosT = Math.cos(tilt * RAD);
    if (dirty && !glOk && mapA < 1) geometry();
    if (mapA < 1) { if (glOk) drawGL(s); else paintSurfaceCPU(s); } else if (glOk) clearGL();
    if (mapA > 0) { setMv(); drawTiles(mapA); nightOverlay(s, mapA); }
    if (mapA < 1) globeLines(1 - mapA);
    var occ = mapA > 0 ? mapLines(mapA) : null;
    if (mapA < 0.5) { // Güneş işareti ve küre kenarı
      var sp2 = projOrtho(s.dec / RAD, s.lon);
      if (sp2.z > 0) {
        var gr = ctx.createRadialGradient(sp2.x, sp2.y, 0, sp2.x, sp2.y, 30 * dpr);
        gr.addColorStop(0, "rgba(255,214,90,0.95)"); gr.addColorStop(0.35, "rgba(255,200,60,0.55)"); gr.addColorStop(1, "rgba(255,200,60,0)");
        ctx.fillStyle = gr; ctx.beginPath(); ctx.arc(sp2.x, sp2.y, 30 * dpr, 0, 2 * Math.PI); ctx.fill();
        ctx.beginPath(); ctx.arc(sp2.x, sp2.y, 8 * dpr, 0, 2 * Math.PI); ctx.fillStyle = "#ffd23f"; ctx.fill(); ctx.lineWidth = 2 * dpr; ctx.strokeStyle = "#e08a00"; ctx.stroke();
      }
      if (R < Math.hypot(cx, cy)) { ctx.beginPath(); ctx.arc(cx, cy, R, 0, 2 * Math.PI); ctx.strokeStyle = col.rim; ctx.lineWidth = 1.5 * dpr; ctx.stroke(); }
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
    var anchor = (ax != null && (z > ZM0 || zoom > ZM0)) ? unproject(ax, ay) : null;
    if (z > ZM0 && zadj === null) freezeScale();
    z = Math.min(z, zoomLimit());
    if (Math.abs(z - zoom) < 1e-6) return;
    zoom = z; dirty = true;
    if (zoom > 1.12) ensurePlaces();
    if (anchor && zoom > ZM0) { // işaret edilen nokta yerinde kalsın (Mercator'da)
      var W = 256 * Math.pow(2, Math.min(ZMAX_TILE, mapZ())), wx = (anchor.lon + 180) / 360 * W, wy = mercY(anchor.lat) * W;
      lon0 = (wx - (ax - cx)) / W * 360 - 180; tilt = Math.max(-80, Math.min(80, mercLat((wy - (ay - cy)) / W)));
      lon0 = ((lon0 + 540) % 360) - 180;
    }
    if (zoom <= 1.001) { zoom = 1; tilt = TILT0; }
    redraw();
  }
  function toCanvas(e) { var r = canvas.getBoundingClientRect(); return { x: (e.clientX - r.left) / r.width * pw, y: (e.clientY - r.top) / r.height * ph }; }
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
  if (zoomIn) zoomIn.addEventListener("click", function () { ensurePlaces(); setZoom(zoom * 2, cx, cy); });
  if (zoomOut) zoomOut.addEventListener("click", function () { setZoom(zoom / 2, cx, cy); });
  if (resetBtn) resetBtn.addEventListener("click", function () { zoom = 1; zadj = null; tilt = TILT0; dirty = true; resumeAt = 0; redraw(); });

  new MutationObserver(function () { readColors(); redraw(); }).observe(doc.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
  if (window.matchMedia) {
    var mq = matchMedia("(prefers-color-scheme: dark)"), onMq = function () { readColors(); redraw(); };
    if (mq.addEventListener) mq.addEventListener("change", onMq); else if (mq.addListener) mq.addListener(onMq);
  }
  var rt = 0;
  function onResize() { clearTimeout(rt); rt = setTimeout(function () { setup(); redraw(); }, 120); }
  window.addEventListener("resize", onResize);
  if (window.ResizeObserver) new ResizeObserver(onResize).observe(view);
  if (glc) {
    glc.addEventListener("webglcontextlost", function (e) { e.preventDefault(); glOk = false; });
    glc.addEventListener("webglcontextrestored", function () { initGL(); texFull = texSmall = null; dirty = true; uploadTex(); redraw(); });
  }

  readColors(); setup(); initGL(); updateMode(); loadTex(0); draw(); kick();
  window.sthGlobe = {
    state: function () { return { zoom: zoom, lon: lon0, lat: tilt, mapA: mapA, z: mv.z, places: places.length, tiles: tileCount, gl: glOk, tex: !!(texFull || tex), size: [pw, ph] }; },
    set: function (lat, lon, z) { if (z) setZoom(z); lon0 = lon; tilt = lat; dirty = true; redraw(); }
  };
})();
