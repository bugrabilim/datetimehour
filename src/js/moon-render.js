/* Fotoğraf dokulu Ay (3 Kasım 2026, 2. analiz #103): NASA SVS CGI Moon Kit dokusu (lroc_color_2k.jpg, kamu malı;
   "NASA's Scientific Visualization Studio"), 1024×512 eşdikdörtgen. Her evre tarayıcıda çizilir: diskin her noktası
   için yüzey normali → selenografik boylam/enlem → doku pikseli; aydınlık Güneş yönüyle (evre kesrinden), karanlık
   yarıda hafif yer ışığı. Güney yarımkürede Ay ters görünür (180° döndürülür). Doku yüklenmezse null döner (SVG yedek). */
(function () {
  "use strict";
  var doc = document, tex = null, waiting = [], loading = false, cache = {};
  function load(url, cb) {
    if (tex) { cb(true); return; }
    waiting.push(cb);
    if (loading) return;
    loading = true;
    var img = new Image();
    img.decoding = "async";
    img.onload = function () {
      var c = doc.createElement("canvas"); c.width = img.naturalWidth; c.height = img.naturalHeight;
      var x = c.getContext("2d"); x.drawImage(img, 0, 0);
      try { tex = { w: c.width, h: c.height, d: x.getImageData(0, 0, c.width, c.height).data }; } catch (e) { tex = null; }
      var w = waiting; waiting = []; w.forEach(function (f) { f(!!tex); });
    };
    img.onerror = function () { var w = waiting; waiting = []; w.forEach(function (f) { f(false); }); };
    img.src = url;
  }
  /* frac: evre kesri (0 yeni ay, 0,25 ilk dördün, 0,5 dolunay); south: güney yarımküre; size: piksel */
  function draw(canvas, frac, south, size) {
    if (!tex) return false;
    var dpr = Math.min(2, window.devicePixelRatio || 1), n = Math.round(size * dpr);
    canvas.width = n; canvas.height = n;
    var ctx = canvas.getContext("2d"), img = ctx.createImageData(n, n), o = img.data;
    var th = 2 * Math.PI * frac, sx = Math.sin(th), sz = -Math.cos(th); // Güneş yönü (gözlemciye doğru +z)
    var r = n / 2, W = tex.w, H = tex.h, T = tex.d;
    for (var py = 0; py < n; py++) {
      for (var px = 0; px < n; px++) {
        var x = (px + 0.5 - r) / r, y = (r - py - 0.5) / r, rr = x * x + y * y;
        if (rr > 1) continue;
        if (south) { x = -x; y = -y; }
        var z = Math.sqrt(1 - rr);
        var lon = Math.atan2(x, z), lat = Math.asin(y);
        var u = Math.floor(((lon / Math.PI + 1) / 2) * W) % W, v = Math.min(H - 1, Math.floor((0.5 - lat / Math.PI) * H));
        var ti = (v * W + u) * 4;
        var lit = x * sx + z * sz; // n·s
        var k = lit > 0 ? Math.min(1, 0.15 + lit * 1.1) : 0;
        if (lit > -0.04 && lit <= 0.04) k = Math.max(k, (lit + 0.04) / 0.08 * 0.2); // yumuşak terminatör
        k = Math.max(k, 0.07); // yer ışığı
        var edge = rr > 0.96 ? Math.max(0, (1 - rr) / 0.04) : 1; // kenar yumuşatma
        var i = (py * n + px) * 4;
        o[i] = T[ti] * k; o[i + 1] = T[ti + 1] * k; o[i + 2] = T[ti + 2] * k; o[i + 3] = 255 * edge;
      }
    }
    ctx.putImageData(img, 0, 0);
    canvas.style.width = size + "px"; canvas.style.height = size + "px";
    return true;
  }
  /* Evre başına önbellekli veri adresi (takvim hücreleri, SVG <image>) */
  function dataUrl(frac, south, size) {
    if (!tex) return null;
    var key = Math.round(frac * 200) + (south ? "s" : "n") + size;
    if (cache[key]) return cache[key];
    var c = doc.createElement("canvas");
    if (!draw(c, Math.round(frac * 200) / 200, south, size)) return null;
    return (cache[key] = c.toDataURL("image/png"));
  }
  window.sthMoonTex = { load: load, draw: draw, dataUrl: dataUrl, ready: function () { return !!tex; } };
})();
