/*
 * Hava neyse ekran öyle (3 Kasım 2026, 2. analiz #193; sahibin isteği): hava sayfasının üst kartında seçili yerin
 * havası canlandırılır. Yağmurda yağmur yağar (yağış mm/sa damla sayısını, rüzgâr km/sa eğimini belirler), 30 km/sa
 * ve üstü rüzgârda açık ya da bulutlu hava "rüzgârlı" olur ve yapraklar savrulur, bulutların sayısı bulut oranından,
 * hızı rüzgârdan; karda kar (rüzgârla savrulur), sisde sis, fırtınada şimşek, açık gündüz Güneş parlar, gece yıldızlar.
 * Uygulamalardaki WeatherScene (iOS AnimatedBackground.swift, Android AnimatedBackground.kt) ile aynı kurallar.
 * "Hareketi azalt" tercihinde tek kare çizilir; sekme arka plandayken ya da kart ekranda değilken durur (pil).
 * Dış kaynak yok: yalnız canvas.
 */
(function (root) {
  "use strict";
  var WINDY = 30;

  /* WMO kodu → tür (uygulamalardaki SkyWeather.of ile aynı) */
  function kindOf(code) {
    if (code === 0 || code === 1) return "clear";
    if (code === 45 || code === 48) return "fog";
    if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "rain";
    if ((code >= 71 && code <= 77) || code === 85 || code === 86) return "snow";
    if (code >= 95 && code <= 99) return "storm";
    return "cloudy";
  }
  function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }

  /* Sahne: tür + yoğunluk; sayılar uygulamalardakiyle aynı */
  function scene(kind, precip, wind, cloud) {
    precip = Math.max(0, precip || 0); wind = Math.max(0, wind || 0);
    if (cloud == null || isNaN(cloud)) cloud = -1;
    if ((kind === "clear" || kind === "cloudy") && wind >= WINDY) kind = "windy";
    var s = { kind: kind, precip: precip, wind: wind, cloud: cloud };
    s.clouds = kind === "clear" ? (cloud < 0 ? 0 : Math.min(2, Math.round(cloud * 3))) : kind === "fog" ? 3 : (cloud < 0 ? 4 : clamp(Math.round(2 + cloud * 4), 2, 6));
    s.cloudSpeed = (kind === "fog" ? 0.5 : 1) * (1 + wind / 12);
    s.drops = kind === "rain" ? (precip <= 0 ? 90 : clamp(Math.floor(40 + precip * 30), 40, 240)) : kind === "storm" ? clamp(Math.floor(140 + precip * 20), 140, 300) : 0;
    s.slant = Math.min(0.9, 0.08 + wind / 80);
    s.flakes = kind !== "snow" ? 0 : precip <= 0 ? 110 : clamp(Math.floor(60 + precip * 40), 60, 240);
    s.leaves = kind === "windy" ? clamp(8 + Math.floor((wind - WINDY) / 3), 8, 30) : (kind === "storm" || kind === "rain") && wind >= WINDY ? 10 : 0;
    return s;
  }
  /* Open-Meteo "current" nesnesinden */
  function fromCurrent(c) {
    return scene(kindOf(c.weather_code), c.precipitation, c.wind_speed_10m, c.cloud_cover == null ? -1 : c.cloud_cover / 100);
  }

  /* Sabit tohumlu sözde rastgele (uygulamalarla aynı LCG yerine basit mulberry32: dağılım yeter) */
  function rng(seed) {
    var a = seed >>> 0;
    return function () { a = (a + 0x6D2B79F5) >>> 0; var t = a; t = Math.imul(t ^ (t >>> 15), t | 1); t ^= t + Math.imul(t ^ (t >>> 7), t | 61); return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
  }

  function grad(ctx, w, h, colors) {
    var g = ctx.createLinearGradient(0, 0, 0, h);
    colors.forEach(function (c, i) { g.addColorStop(colors.length === 1 ? 0 : i / (colors.length - 1), c); });
    ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
  }
  function stars(ctx, w, h, t) {
    var r = rng(0x5EED);
    for (var i = 0; i < 120; i++) {
      var x = (r() * w + t * 2) % w, y = r() * h, s = 0.6 + r() * 1.6, ph = r() * 6.28, sp = 0.5 + r() * 1.5;
      ctx.globalAlpha = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * sp + ph));
      ctx.fillStyle = "#fff"; ctx.beginPath(); ctx.arc(x, y, s / 2, 0, 6.2832); ctx.fill();
    }
    ctx.globalAlpha = 1;
  }
  /* Bulutlar (doğal tasarım madde 10): üst üste binen yumuşak daireler; yazı alanına girmesin diye üst üçte birde */
  var PUFFS = [[0.22, 0.66, 0.20], [0.42, 0.46, 0.27], [0.64, 0.52, 0.24], [0.84, 0.68, 0.16], [0.52, 0.74, 0.22]];
  function clouds(ctx, w, h, t, n, k, dark) {
    var c = dark ? "191,191,191" : "255,255,255";
    for (var i = 0; i < n; i++) {
      var cw = w * (0.30 + 0.08 * (i % 3)), ch = cw * 0.45, sp = (6 + (i % 4) * 3) * k;
      var x = ((i * w * 0.37 + t * sp) % (w + cw)) - cw, y = h * (0.03 + 0.07 * (i % 4) + 0.025 * Math.floor(i / 4));
      for (var j = 0; j < PUFFS.length; j++) {
        var px = x + cw * PUFFS[j][0], py = y + ch * PUFFS[j][1], r = cw * PUFFS[j][2];
        var g = ctx.createRadialGradient(px, py, 0, px, py, r);
        g.addColorStop(0, "rgba(" + c + ",0.5)"); g.addColorStop(0.55, "rgba(" + c + ",0.42)"); g.addColorStop(1, "rgba(" + c + ",0)");
        ctx.fillStyle = g; ctx.beginPath(); ctx.arc(px, py, r, 0, 6.2832); ctx.fill();
      }
    }
  }

  /* Bir kare: gökyüzü (gündüz/gece) + hava; yazılar okunsun diye üstte koyulaştırma */
  function draw(ctx, w, h, t, s, day) {
    if (day) grad(ctx, w, h, ["#2F6FCB", "#8EC9F2"]);
    else { grad(ctx, w, h, ["#0B1026", "#1B2350"]); stars(ctx, w, h, t); }
    var k = s.kind;
    if ((k === "clear" || k === "windy") && day) {
      var cx = w * 0.82, cy = h * 0.12, r = w * 0.5, g = ctx.createRadialGradient(cx, cy, 0, cx, cy, r);
      g.addColorStop(0, "rgba(255,243,196," + (0.45 + 0.08 * Math.sin(t * 0.5)).toFixed(3) + ")"); g.addColorStop(1, "rgba(255,243,196,0)");
      ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
    } else if (k === "cloudy") { ctx.fillStyle = "rgba(115,115,115," + (0.15 + 0.15 * Math.max(0, s.cloud)) + ")"; ctx.fillRect(0, 0, w, h); }
    else if (k === "fog") { var fg = ctx.createLinearGradient(0, 0, 0, h); fg.addColorStop(0, "rgba(255,255,255,0.15)"); fg.addColorStop(1, "rgba(255,255,255,0.45)"); ctx.fillStyle = fg; ctx.fillRect(0, 0, w, h); }
    else if (k === "rain" || k === "storm") { ctx.fillStyle = "rgba(51,51,51,0.35)"; ctx.fillRect(0, 0, w, h); }
    else if (k === "snow") { ctx.fillStyle = "rgba(153,153,153,0.2)"; ctx.fillRect(0, 0, w, h); }
    clouds(ctx, w, h, t, s.clouds, s.cloudSpeed, k === "rain" || k === "storm");
    if (k === "storm") { var p = t % 12; if (p < 0.15) { ctx.fillStyle = "rgba(255,255,255," + (0.35 * (1 - p / 0.15)) + ")"; ctx.fillRect(0, 0, w, h); } }
    var i, R;
    if (s.drops) {
      R = rng(0xA1A1); ctx.strokeStyle = "rgba(255,255,255,0.45)"; ctx.lineWidth = 1.2; ctx.beginPath();
      for (i = 0; i < s.drops; i++) {
        var x0 = R() * w, sp = 380 + R() * 220, len = 10 + R() * 14;
        var y = ((R() * h + t * sp) % (h + len)) - len, x = (x0 - (y + len) * s.slant) % w;
        if (x < 0) x += w;
        ctx.moveTo(x, y); ctx.lineTo(x - len * s.slant, y + len);
      }
      ctx.stroke();
    }
    if (s.flakes) {
      R = rng(0x5E0); ctx.fillStyle = "rgba(255,255,255,0.8)";
      for (i = 0; i < s.flakes; i++) {
        var fx0 = R() * w, fsp = 25 + R() * 45, fr = 1.2 + R() * 2.6, fph = R() * 6.28;
        var fy = ((R() * h + t * fsp) % (h + 10)) - 5, fx = (fx0 + t * s.wind * 1.2 + 12 * Math.sin(t * 0.8 + fph)) % w;
        if (fx < 0) fx += w;
        ctx.beginPath(); ctx.arc(fx, fy, fr, 0, 6.2832); ctx.fill();
      }
    }
    if (s.leaves) {
      R = rng(0x1EAF); var cols = ["#E8590C", "#82C91E", "#B07D2B", "#F59F00"];
      for (i = 0; i < s.leaves; i++) {
        var lsp = (90 + s.wind * 3) * (0.7 + R() * 0.6), ly0 = R() * h * 0.9, lph = R() * 6.28;
        var lx = ((R() * (w + 40) + t * lsp) % (w + 40)) - 20, ly = ly0 + 30 * Math.sin(t * 1.5 + lph);
        ctx.save(); ctx.translate(lx, ly); ctx.rotate(t * 3 + lph); ctx.globalAlpha = 0.85; ctx.fillStyle = cols[i % 4];
        ctx.beginPath(); ctx.ellipse(0, 0, 6, 3, 0, 0, 6.2832); ctx.fill(); ctx.restore();
      }
      ctx.globalAlpha = 1;
    }
    // Beyaz yazılar okunsun diye koyulaştırma (gündüz daha çok)
    ctx.fillStyle = day ? "rgba(0,0,0,0.42)" : "rgba(0,0,0,0.2)"; ctx.fillRect(0, 0, w, h);
  }

  /* Canvas'ı sahneyle çalıştırır; aynı canvas için yeniden çağrılınca sahne değişir */
  var state = null;
  function set(canvas, s, day) {
    if (!canvas || !canvas.getContext) return;
    var reduce = root.matchMedia && root.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (state && state.canvas === canvas) { state.s = s; state.day = day; if (reduce || !state.raf) state.frame(0); return; }
    state = { canvas: canvas, s: s, day: day, raf: 0, visible: true, t0: 0 };
    function size() {
      var dpr = Math.min(2, root.devicePixelRatio || 1), r = canvas.getBoundingClientRect();
      canvas.width = Math.max(1, Math.round(r.width * dpr)); canvas.height = Math.max(1, Math.round(r.height * dpr));
      canvas.getContext("2d").setTransform(dpr, 0, 0, dpr, 0, 0);
    }
    function frame(ms) {
      var r = canvas.getBoundingClientRect();
      draw(canvas.getContext("2d"), r.width, r.height, reduce ? 0 : ms / 1000, state.s, state.day);
    }
    state.frame = frame;
    function loop(ms) { frame(ms); state.raf = root.requestAnimationFrame(loop); }
    function run() {
      root.cancelAnimationFrame(state.raf); state.raf = 0;
      if (reduce) { frame(0); return; }
      if (state.visible && !document.hidden) state.raf = root.requestAnimationFrame(loop);
    }
    size();
    if (root.ResizeObserver) new root.ResizeObserver(function () { size(); if (reduce || !state.raf) frame(0); }).observe(canvas);
    if (root.IntersectionObserver) new root.IntersectionObserver(function (es) { state.visible = es[0].isIntersecting; run(); }).observe(canvas);
    document.addEventListener("visibilitychange", run);
    run();
  }

  var api = { scene: scene, kindOf: kindOf, fromCurrent: fromCurrent, set: set, WINDY: WINDY };
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.sthWeatherFx = api;
})(typeof window !== "undefined" ? window : globalThis);
