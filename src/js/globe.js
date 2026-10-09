/* Ana sayfa: yavaş dönen dünya küresi. Gündüz/gece (Güneş'in tepe noktasından), 15°'lik saat dilimi
   meridyenleri ve belli başlı şehirlerin yerel saati. Kara maskesi derlemede Natural Earth verisinden üretilir. */
(function () {
  "use strict";
  var doc = document, S = window.sth;
  var fig = doc.querySelector("[data-globe]");
  if (!fig || !S) return;
  var canvas = fig.querySelector("canvas");
  var ctx = canvas && canvas.getContext && canvas.getContext("2d");
  if (!ctx) return;
  var G = S.cfg.globe || {};
  var cities = G.cities || [];
  var RAD = Math.PI / 180;
  var TILT0 = 23, tilt = TILT0, sinT = 0, cosT = 1; // görüş merkezinin enlemi (derece)
  var zoom = 1, ZMIN = 1, ZMAX = 6, dirty = true;
  var SPEED = 3; // derece/saniye: bir tur 2 dakika
  var reduce = window.matchMedia && matchMedia("(prefers-reduced-motion: reduce)").matches;
  var rtl = doc.documentElement.dir === "rtl";

  // Başlangıç: cihazın saat dilimine denk gelen boylam biraz batıda, kendi bölgesi görünür
  var lon0 = -new Date().getTimezoneOffset() / 4 - 10;
  var mask = null, MW = 720, MH = 360;
  var px = 0, dpr = 1, R = 0, R0 = 0, C = 0, N = 0;
  var pIdx, pLat, pLon, pSinLat, pCosLat, pCosL, pSinL, pShade, pAlpha, img;
  var col = {};

  function hex(v, fb) {
    v = (v || "").trim();
    var m = /^#([0-9a-f]{6})$/i.exec(v);
    if (!m) return fb;
    var n = parseInt(m[1], 16);
    return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
  }
  function readColors() {
    var cs = getComputedStyle(fig);
    var g = function (k, fb) { return hex(cs.getPropertyValue(k), fb); };
    col.ocean = g("--globe-ocean", [120, 180, 220]);
    col.land = g("--globe-land", [150, 190, 120]);
    col.oceanN = g("--globe-ocean-night", [20, 40, 70]);
    col.landN = g("--globe-land-night", [30, 50, 40]);
    col.grid = cs.getPropertyValue("--globe-grid").trim() || "rgba(255,255,255,.35)";
    col.label = cs.getPropertyValue("--globe-label").trim() || "#111";
    col.halo = cs.getPropertyValue("--globe-halo").trim() || "#fff";
    col.accent = cs.getPropertyValue("--accent").trim() || "#c60";
    col.rim = cs.getPropertyValue("--border").trim() || "#999";
  }

  function setup() {
    var css = canvas.clientWidth || 320;
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    var size = Math.round(css * dpr);
    if (size !== px) { px = size; canvas.width = canvas.height = px; img = ctx.createImageData(px, px); dirty = true; }
    C = px / 2;
    R0 = C - 2 * dpr;
  }

  // Piksel başına ters izdüşüm (enlem/boylam); yalnız boyut, yakınlaştırma ya da eğim değişince hesaplanır
  function geometry() {
    dirty = false;
    R = R0 * zoom;
    sinT = Math.sin(tilt * RAD); cosT = Math.cos(tilt * RAD);
    var list = [], lim = 1 + 2 / R;
    for (var j = 0; j < px; j++) {
      var y = (C - (j + 0.5)) / R;
      for (var i = 0; i < px; i++) {
        var x = (i + 0.5 - C) / R;
        if (x * x + y * y <= lim) list.push(j * px + i);
      }
    }
    N = list.length;
    pIdx = new Int32Array(list);
    pLat = new Float32Array(N); pLon = new Float32Array(N);
    pSinLat = new Float32Array(N); pCosLat = new Float32Array(N);
    pCosL = new Float32Array(N); pSinL = new Float32Array(N);
    pShade = new Float32Array(N); pAlpha = new Uint8ClampedArray(N);
    for (var k = 0; k < N; k++) {
      var p = pIdx[k], jj = (p / px) | 0, ii = p - jj * px;
      var xx = (ii + 0.5 - C) / R, yy = (C - (jj + 0.5)) / R, rr = xx * xx + yy * yy;
      var d = Math.sqrt(rr);
      if (rr > 1) { xx /= d; yy /= d; rr = 1; }
      var z = Math.sqrt(Math.max(0, 1 - rr));
      var sl = z * sinT + yy * cosT;
      var lat = Math.asin(Math.max(-1, Math.min(1, sl)));
      var lr = Math.atan2(xx, z * cosT - yy * sinT);
      pLat[k] = lat / RAD;
      pLon[k] = lr / RAD;
      pSinLat[k] = Math.sin(lat); pCosLat[k] = Math.cos(lat);
      pCosL[k] = Math.cos(lr); pSinL[k] = Math.sin(lr);
      pShade[k] = 0.7 + 0.3 * z;
      pAlpha[k] = Math.max(0, Math.min(1, (1 - d) * R + 0.5)) * 255;
    }
    var data = img.data;
    for (var q = 3; q < data.length; q += 4) data[q] = 0; // önceki karenin disk dışı kalıntısı
  }

  // Güneş'in tepe noktası (yaklaşık; USNO formülü, birkaç yay dakikası)
  function sun(ms) {
    var d = ms / 86400000 + 2440587.5 - 2451545.0;
    var g = (357.529 + 0.98560028 * d) * RAD;
    var q = 280.459 + 0.98564736 * d;
    var L = (q + 1.915 * Math.sin(g) + 0.02 * Math.sin(2 * g)) * RAD;
    var e = (23.439 - 0.00000036 * d) * RAD;
    var ra = Math.atan2(Math.cos(e) * Math.sin(L), Math.cos(L)) / RAD;
    var dec = Math.asin(Math.sin(e) * Math.sin(L));
    var eqt = (((q - ra) % 360) + 540) % 360 - 180; // derece
    var utcH = (((ms % 86400000) + 86400000) % 86400000) / 3600000;
    var lon = -15 * (utcH - 12) - eqt;
    lon = ((lon + 540) % 360) - 180;
    return { dec: dec, lon: lon };
  }

  function proj(latD, lonD) {
    var f = latD * RAD, l = (lonD - lon0) * RAD, cf = Math.cos(f), cl = Math.cos(l);
    return {
      x: C + cf * Math.sin(l) * R,
      y: C - (cosT * Math.sin(f) - sinT * cf * cl) * R,
      z: sinT * Math.sin(f) + cosT * cf * cl,
    };
  }

  // Yüzey: kara oranı maskeden çift doğrusal örnekle (yumuşak kıyı), gündüz/gece alacakaranlık geçişli
  function paintSurface(s) {
    var data = img.data, sd = Math.sin(s.dec), cd = Math.cos(s.dec);
    var D = (lon0 - s.lon) * RAD, cD = Math.cos(D), sD = Math.sin(D);
    var O = col.ocean, L = col.land, On = col.oceanN, Ln = col.landN;
    var sx = MW / 360, sy = MH / 180, base = lon0 + 540;
    for (var k = 0; k < N; k++) {
      var lf = 0;
      if (mask) {
        var cf = ((pLon[k] + base) % 360) * sx - 0.5;
        var c0 = Math.floor(cf), fx = cf - c0;
        c0 = (c0 + MW) % MW;
        var c1 = (c0 + 1) % MW;
        var rf = (90 - pLat[k]) * sy - 0.5;
        var r0 = Math.floor(rf), fy = rf - r0;
        if (r0 < 0) { r0 = 0; fy = 0; } else if (r0 >= MH - 1) { r0 = MH - 2; fy = 1; }
        var a0 = r0 * MW, a1 = a0 + MW;
        var v = (mask[a0 + c0] * (1 - fx) + mask[a0 + c1] * fx) * (1 - fy) + (mask[a1 + c0] * (1 - fx) + mask[a1 + c1] * fx) * fy;
        lf = (v - 0.5) * 2.5 + 0.5;
        lf = lf <= 0 ? 0 : lf >= 1 ? 1 : lf;
      }
      var cz = pSinLat[k] * sd + pCosLat[k] * cd * (pCosL[k] * cD - pSinL[k] * sD);
      var t = (cz + 0.1) / 0.16;
      t = t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t);
      var sh = pShade[k], o = pIdx[k] * 4;
      var dr = O[0] + (L[0] - O[0]) * lf, dg = O[1] + (L[1] - O[1]) * lf, db = O[2] + (L[2] - O[2]) * lf;
      var nr = On[0] + (Ln[0] - On[0]) * lf, ng = On[1] + (Ln[1] - On[1]) * lf, nb = On[2] + (Ln[2] - On[2]) * lf;
      data[o] = (nr + (dr - nr) * t) * sh;
      data[o + 1] = (ng + (dg - ng) * t) * sh;
      data[o + 2] = (nb + (db - nb) * t) * sh;
      data[o + 3] = pAlpha[k];
    }
    ctx.putImageData(img, 0, 0);
  }

  // Kırmızı çizgi: sıfır noktası, Greenwich meridyeni (UTC)
  function meridians() {
    ctx.lineWidth = 1 * dpr;
    for (var m = -12; m < 12; m++) {
      var lon = m * 15, mine = m === 0;
      ctx.beginPath();
      var pen = false;
      for (var lat = -78; lat <= 78; lat += 3) {
        var p = proj(lat, lon);
        if (p.z > 0) { if (pen) ctx.lineTo(p.x, p.y); else ctx.moveTo(p.x, p.y); pen = true; }
        else pen = false;
      }
      ctx.strokeStyle = mine ? col.accent : col.grid;
      ctx.lineWidth = (mine ? 2 : 1) * dpr;
      ctx.stroke();
    }
    // ekvator
    ctx.beginPath();
    var on = false;
    for (var lo = -180; lo <= 180; lo += 4) {
      var q = proj(0, lo);
      if (q.z > 0) { if (on) ctx.lineTo(q.x, q.y); else ctx.moveTo(q.x, q.y); on = true; } else on = false;
    }
    ctx.strokeStyle = col.grid; ctx.lineWidth = 1 * dpr; ctx.stroke();
    // UTC farkı etiketleri (ekvatorun hemen altında)
    ctx.font = "600 " + Math.round(10.5 * dpr) + "px system-ui, sans-serif";
    ctx.textAlign = "center"; ctx.textBaseline = "top";
    for (var n = -12; n < 12; n++) {
      var e = proj(-3, n * 15);
      if (e.z < 0.45) continue;
      var txt = n === 0 ? "UTC" : "UTC" + (n > 0 ? "+" : "−") + Math.abs(n);
      ctx.globalAlpha = Math.min(1, (e.z - 0.45) / 0.25);
      halo(txt, e.x, e.y, n === 0 ? col.accent : col.label);
      ctx.globalAlpha = 1;
    }
  }

  function halo(txt, x, y, fill) {
    ctx.lineJoin = "round";
    ctx.lineWidth = 3 * dpr; ctx.strokeStyle = col.halo; ctx.strokeText(txt, x, y);
    ctx.fillStyle = fill; ctx.fillText(txt, x, y);
  }

  var fmtCache = {}, labelTimes = {}, lastLabelSec = -1;
  function cityTimes(ms) {
    var sec = Math.floor(ms / 1000);
    if (sec === lastLabelSec) return;
    lastLabelSec = sec;
    var h12 = S.prefs && S.prefs.h12;
    cities.forEach(function (c) {
      var key = c.tz + (h12 ? "1" : "0");
      var f = fmtCache[key];
      if (!f) {
        try { f = fmtCache[key] = new Intl.DateTimeFormat(S.locale, { hour: "2-digit", minute: "2-digit", hour12: !!h12, timeZone: c.tz }); }
        catch (e) { f = fmtCache[key] = { format: function () { return ""; } }; }
      }
      labelTimes[c.tz] = f.format(new Date(ms));
    });
  }

  function citiesLayer() {
    var pts = cities.filter(function (c) { return c.m || zoom >= 1.8; })
      .map(function (c) { var p = proj(c.lat, c.lon); p.c = c; return p; })
      .filter(function (p) { return p.z > 0.12 && p.x > -40 && p.x < px + 40 && p.y > -20 && p.y < px + 20; })
      .sort(function (a, b) { return ((b.c.m ? 1 : 0) - (a.c.m ? 1 : 0)) || b.z - a.z; });
    var boxes = [];
    ctx.font = "600 " + Math.round(11.5 * dpr) + "px system-ui, sans-serif";
    ctx.textBaseline = "middle";
    pts.forEach(function (p) {
      var a = Math.min(1, (p.z - 0.12) / 0.2);
      ctx.globalAlpha = a;
      ctx.beginPath(); ctx.arc(p.x, p.y, 3.2 * dpr, 0, 2 * Math.PI);
      ctx.fillStyle = col.accent; ctx.fill();
      ctx.lineWidth = 1.2 * dpr; ctx.strokeStyle = col.halo; ctx.stroke();
      var txt = p.c.n + " " + (labelTimes[p.c.tz] || "");
      var w = ctx.measureText(txt).width, h = 14 * dpr;
      var left = rtl ? p.x > C - R * 0.45 : p.x > C + R * 0.45;
      var x = left ? p.x - 6 * dpr - w : p.x + 6 * dpr;
      var box = { x: x - 2, y: p.y - h / 2, w: w + 4, h: h };
      var hit = boxes.some(function (b) { return box.x < b.x + b.w && b.x < box.x + box.w && box.y < b.y + b.h && b.y < box.y + box.h; });
      if (!hit) {
        boxes.push(box);
        ctx.textAlign = "left";
        halo(txt, x, p.y, col.label);
      }
      ctx.globalAlpha = 1;
    });
  }

  function sunMarker(s) {
    var p = proj(s.dec / RAD, s.lon);
    if (p.z <= 0) return;
    var g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, 14 * dpr);
    g.addColorStop(0, "rgba(255,214,90,0.95)");
    g.addColorStop(0.35, "rgba(255,200,60,0.55)");
    g.addColorStop(1, "rgba(255,200,60,0)");
    ctx.fillStyle = g;
    ctx.beginPath(); ctx.arc(p.x, p.y, 14 * dpr, 0, 2 * Math.PI); ctx.fill();
  }

  function draw() {
    if (!px) return;
    if (dirty) geometry();
    var ms = S.nowMs(), s = sun(ms);
    paintSurface(s);
    meridians();
    sunMarker(s);
    cityTimes(ms);
    citiesLayer();
    ctx.beginPath(); ctx.arc(C, C, R, 0, 2 * Math.PI);
    ctx.strokeStyle = col.rim; ctx.lineWidth = 1.5 * dpr; ctx.stroke();
  }

  // Döngü: görünürken ve sekme açıkken ~30 kare/sn; hareketi azalt tercihinde dönmez, 30 sn'de bir yenilenir
  var visible = true, last = 0, raf = 0, dragging = false, resumeAt = 0, pending = true, lastDraw = 0;
  function frame(ts) {
    raf = 0;
    if (!visible || doc.hidden) return;
    if (ts - last >= 33) {
      var dt = last ? Math.min(0.1, (ts - last) / 1000) : 0;
      last = ts;
      var spin = !reduce && !dragging && zoom < 1.2 && Date.now() >= resumeAt;
      if (spin) lon0 -= SPEED * dt;
      lon0 = ((lon0 + 540) % 360) - 180;
      // Dönmüyorken (yakınlaştırılmış) yalnız etkileşimde ve saniyede bir çiz
      if (spin || pending || ts - lastDraw >= 1000) { pending = false; lastDraw = ts; draw(); }
    }
    if (!reduce) raf = requestAnimationFrame(frame);
  }
  function kick() { if (!raf) { last = 0; raf = requestAnimationFrame(frame); } }

  if ("IntersectionObserver" in window) {
    new IntersectionObserver(function (en) { visible = en[0].isIntersecting; if (visible) kick(); }).observe(fig);
  }
  doc.addEventListener("visibilitychange", function () { if (!doc.hidden) kick(); });
  if (reduce) setInterval(function () { if (visible && !doc.hidden) draw(); }, 30000);

  // Etkileşim: sürükle (yatay = boylam, dikey = enlem), iki parmakla ya da Ctrl/trackpad tekerleğiyle yakınlaştır, + ve − düğmeleri
  function setZoom(z) {
    z = Math.max(ZMIN, Math.min(ZMAX, z));
    if (Math.abs(z - zoom) < 0.001) return;
    zoom = z; dirty = true;
    fig.classList.toggle("is-zoomed", zoom > 1.01);
    if (zoomOut) zoomOut.disabled = zoom <= ZMIN + 0.001;
    if (zoomIn) zoomIn.disabled = zoom >= ZMAX - 0.001;
    redraw();
  }
  function setTilt(t) {
    t = Math.max(-80, Math.min(80, t));
    if (t !== tilt) { tilt = t; dirty = true; }
  }
  function redraw() { pending = true; if (reduce) draw(); else kick(); }
  var pts = {}, lastX = 0, lastY = 0, pinch0 = 0, zoom0 = 1;
  function pcount() { return Object.keys(pts).length; }
  function pdist() { var k = Object.keys(pts); var a = pts[k[0]], b = pts[k[1]]; return Math.hypot(a.x - b.x, a.y - b.y); }
  canvas.addEventListener("pointerdown", function (e) {
    pts[e.pointerId] = { x: e.clientX, y: e.clientY };
    try { canvas.setPointerCapture(e.pointerId); } catch (x) {}
    dragging = true; lastX = e.clientX; lastY = e.clientY;
    if (pcount() === 2) { pinch0 = pdist(); zoom0 = zoom; }
  });
  canvas.addEventListener("pointermove", function (e) {
    if (!pts[e.pointerId]) return;
    pts[e.pointerId] = { x: e.clientX, y: e.clientY };
    if (pcount() >= 2) { if (pinch0) setZoom(zoom0 * pdist() / pinch0); return; }
    var dx = e.clientX - lastX, dy = e.clientY - lastY;
    lastX = e.clientX; lastY = e.clientY;
    var k = (dpr / R) / RAD;
    lon0 -= dx * k;
    if (zoom > 1.01 || e.pointerType === "mouse") setTilt(tilt + dy * k);
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
    setZoom(zoom * Math.exp(-e.deltaY * (e.ctrlKey ? 0.01 : 0.0015)));
  }, { passive: false });
  canvas.addEventListener("dblclick", function () { tilt = TILT0; dirty = true; setZoom(zoom > 1.01 ? 1 : 2.5); redraw(); });
  var zoomIn = fig.querySelector('[data-globe-zoom="in"]'), zoomOut = fig.querySelector('[data-globe-zoom="out"]');
  if (zoomIn) zoomIn.addEventListener("click", function () { setZoom(zoom * 1.6); });
  if (zoomOut) zoomOut.addEventListener("click", function () { setZoom(zoom / 1.6); if (zoom <= 1.01) { tilt = TILT0; dirty = true; redraw(); } });
  if (zoomOut) zoomOut.disabled = true;

  // Tema değişince renkleri yeniden oku
  new MutationObserver(function () { readColors(); draw(); }).observe(doc.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
  if (window.matchMedia) {
    var mq = matchMedia("(prefers-color-scheme: dark)");
    var onMq = function () { readColors(); draw(); };
    if (mq.addEventListener) mq.addEventListener("change", onMq); else if (mq.addListener) mq.addListener(onMq);
  }
  var rt = 0;
  window.addEventListener("resize", function () { clearTimeout(rt); rt = setTimeout(function () { setup(); draw(); }, 150); });

  readColors();
  setup();
  draw();
  kick();

  if (G.land) {
    fetch(G.land, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (m) {
      MW = m.w; MH = m.h;
      var out = new Uint8Array(MW * MH);
      m.rows.forEach(function (runs, r) {
        var c = 0, v = 0;
        for (var i = 0; i < runs.length; i++) { if (v) out.fill(1, r * MW + c, r * MW + c + runs[i]); c += runs[i]; v ^= 1; }
      });
      mask = out;
      draw();
    }).catch(function () {});
  }
})();
