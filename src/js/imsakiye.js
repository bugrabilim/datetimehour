/* Ramazan imsakiyesi: sıradaki Ramazan ayı için konuma göre günlük vakit çizelgesi, renkli görsel olarak çizilir
   (canvas: yıldız dokulu zemin, fener, şamdan, hilal; Bumba Group imzası). Pencerede açılır, PNG olarak indirilir.
   Hesap tarayıcıda yapılır (prayer-calc.js); dış görsel kullanılmaz, konum sunucuya gitmez. */
(function () {
  "use strict";
  var S = window.sth, P = window.sthPrayer;
  if (!S || !P) return;
  var doc = document;
  var box = doc.querySelector("[data-imsakiye]");
  if (!box) return;
  var T = S.T.im || {}, CAL = S.cfg.cal || {};
  var TR_TZ = "Europe/Istanbul";
  var ORDER = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"];
  var dlg = doc.getElementById("im-dialog"), img = box.querySelector("[data-im-img]");
  var heading = box.querySelector("[data-im-heading]"), dtitle = box.querySelector("[data-im-dtitle]");
  var place = null, ram = null;

  var pad = function (n) { return String(n).padStart(2, "0"); };
  function isoOf(p) { return p.year + "-" + pad(p.month) + "-" + pad(p.day); }
  function hm(h) {
    if (!isFinite(h)) return "–";
    var m = Math.round(h * 60); m = ((m % 1440) + 1440) % 1440;
    return pad(Math.floor(m / 60)) + ":" + pad(m % 60);
  }
  /* Bugünden sonra biten ilk Ramazan */
  function pickRamadan(tz) {
    var today = isoOf(S.parts(new Date(S.nowMs()), tz)), list = T.ramadans || [];
    for (var i = 0; i < list.length; i++) if (list[i].to && list[i].to >= today) return list[i];
    return null;
  }
  function rows() {
    var out = [], a = ram.from.split("-").map(Number), b = ram.to.split("-").map(Number);
    var t0 = Date.UTC(a[0], a[1] - 1, a[2]), t1 = Date.UTC(b[0], b[1] - 1, b[2]), n = 1;
    for (var t = t0; t <= t1; t += 86400000, n++) {
      var d = new Date(t), y = d.getUTCFullYear(), m = d.getUTCMonth() + 1, dd = d.getUTCDate();
      var off = S.offsetMin(new Date(Date.UTC(y, m - 1, dd, 12)), place.tz) / 60;
      var r = P.times(y, m, dd, place.lat, place.lon, off), wd = (d.getUTCDay() + 6) % 7;
      out.push({ n: n, y: y, m: m, d: dd, wd: wd, t: ORDER.map(function (k) { return hm(r[k]); }) });
    }
    return out;
  }
  var dateShort = function (r) { return S.tpl(CAL.shortFmt || "{day} {month}", { day: r.d, month: (CAL.months || [])[r.m - 1] }); };
  var dateFull = function (y, m, d) { return S.tpl(CAL.dateFmt || "{day} {month} {year}", { day: d, month: (CAL.months || [])[m - 1], year: y }); };

  /* ---------- çizim ---------- */
  var GOLD = "#e0bb62", GOLD2 = "#a8802f", CREAM = "#fff4d6", BG1 = "#0b4a43", BG2 = "#061f2b";
  function rr(c, x, y, w, h, r) {
    c.beginPath(); c.moveTo(x + r, y); c.arcTo(x + w, y, x + w, y + h, r); c.arcTo(x + w, y + h, x, y + h, r); c.arcTo(x, y + h, x, y, r); c.arcTo(x, y, x + w, y, r); c.closePath();
  }
  function star8(c, cx, cy, r) {
    c.beginPath();
    for (var i = 0; i < 16; i++) {
      var a = i * Math.PI / 8 - Math.PI / 2, rad = i % 2 ? r * 0.62 : r;
      c.lineTo(cx + rad * Math.cos(a), cy + rad * Math.sin(a));
    }
    c.closePath();
  }
  function pattern(c, w, h) {
    var s = 120;
    c.save();
    c.lineWidth = 2; c.strokeStyle = "rgba(224,187,98,0.13)";
    for (var j = -1; j * s < h + s; j++) for (var i = -1; i * s < w + s; i++) {
      star8(c, i * s + s / 2, j * s + s / 2, s * 0.48); c.stroke();
      c.beginPath(); c.arc(i * s + s / 2, j * s + s / 2, s * 0.17, 0, 6.2832); c.stroke();
      c.beginPath(); c.arc(i * s, j * s, s * 0.11, 0, 6.2832); c.stroke();
    }
    c.restore();
  }
  function glow(c, x, y, r, col) {
    var g = c.createRadialGradient(x, y, 0, x, y, r);
    g.addColorStop(0, col); g.addColorStop(1, "rgba(255,200,90,0)");
    c.fillStyle = g; c.fillRect(x - r, y - r, r * 2, r * 2);
  }
  function crescent(c, x, y, R) {
    var o = doc.createElement("canvas"); o.width = o.height = Math.ceil(R * 2.4);
    var k = o.getContext("2d"), m = o.width / 2;
    var g = k.createLinearGradient(0, 0, o.width, o.height); g.addColorStop(0, "#fff0b8"); g.addColorStop(1, GOLD2);
    k.fillStyle = g; k.beginPath(); k.arc(m, m, R, 0, 6.2832); k.fill();
    k.globalCompositeOperation = "destination-out"; k.beginPath(); k.arc(m + R * 0.36, m - R * 0.08, R * 0.82, 0, 6.2832); k.fill();
    c.drawImage(o, x - m, y - m);
  }
  function lantern(c, x, top, s) {
    c.save(); c.translate(x, top); c.scale(s, s);
    c.strokeStyle = GOLD; c.lineWidth = 3; c.beginPath(); c.moveTo(0, -top / s); c.lineTo(0, 12); c.stroke();
    glow(c, 0, 120, 150, "rgba(255,196,80,0.38)");
    var gold = c.createLinearGradient(-40, 0, 40, 0); gold.addColorStop(0, GOLD2); gold.addColorStop(0.5, "#fff0b8"); gold.addColorStop(1, GOLD2);
    c.fillStyle = gold; c.beginPath(); c.arc(0, 8, 9, 0, 6.2832); c.fill();
    c.beginPath(); c.moveTo(-34, 44); c.quadraticCurveTo(0, -14, 34, 44); c.closePath(); c.fill();
    c.fillRect(-38, 42, 76, 9);
    var body = c.createRadialGradient(0, 120, 6, 0, 120, 90); body.addColorStop(0, "#ffe9a0"); body.addColorStop(0.6, "#f2a93b"); body.addColorStop(1, "#b2541c");
    c.fillStyle = body; c.beginPath(); c.moveTo(-34, 51); c.lineTo(34, 51); c.bezierCurveTo(58, 90, 58, 140, 30, 190); c.lineTo(-30, 190); c.bezierCurveTo(-58, 140, -58, 90, -34, 51); c.closePath(); c.fill();
    c.strokeStyle = GOLD2; c.lineWidth = 3; c.stroke();
    c.strokeStyle = "rgba(120,60,10,0.55)"; c.lineWidth = 2;
    [-18, 0, 18].forEach(function (dx) { c.beginPath(); c.moveTo(dx * 0.6, 51); c.quadraticCurveTo(dx * 3, 120, dx, 190); c.stroke(); });
    c.fillStyle = gold; c.fillRect(-34, 188, 68, 9);
    c.beginPath(); c.moveTo(-26, 197); c.lineTo(26, 197); c.lineTo(8, 216); c.lineTo(-8, 216); c.closePath(); c.fill();
    c.strokeStyle = GOLD; c.lineWidth = 3; c.beginPath(); c.moveTo(0, 216); c.lineTo(0, 238); c.stroke();
    c.beginPath(); c.arc(0, 242, 5, 0, 6.2832); c.fillStyle = GOLD; c.fill();
    c.restore();
  }
  function flame(c, x, y, s) {
    glow(c, x, y - 10 * s, 34 * s, "rgba(255,200,90,0.5)");
    var g = c.createLinearGradient(x, y - 24 * s, x, y); g.addColorStop(0, "#fff7c4"); g.addColorStop(1, "#f59a1d");
    c.fillStyle = g; c.beginPath(); c.moveTo(x, y - 26 * s); c.bezierCurveTo(x + 11 * s, y - 10 * s, x + 8 * s, y, x, y); c.bezierCurveTo(x - 8 * s, y, x - 11 * s, y - 10 * s, x, y - 26 * s); c.fill();
  }
  function candelabra(c, x, base, s) {
    c.save(); c.translate(x, base); c.scale(s, s);
    var gold = c.createLinearGradient(-60, 0, 60, 0); gold.addColorStop(0, GOLD2); gold.addColorStop(0.5, "#fff0b8"); gold.addColorStop(1, GOLD2);
    c.fillStyle = gold; c.strokeStyle = gold; c.lineWidth = 7; c.lineCap = "round";
    c.beginPath(); c.moveTo(-44, 0); c.lineTo(44, 0); c.lineTo(26, -16); c.lineTo(-26, -16); c.closePath(); c.fill();
    c.beginPath(); c.moveTo(0, -16); c.lineTo(0, -130); c.stroke();
    c.beginPath(); c.moveTo(0, -64); c.bezierCurveTo(-14, -34, -64, -44, -64, -108); c.stroke();
    c.beginPath(); c.moveTo(0, -64); c.bezierCurveTo(14, -34, 64, -44, 64, -108); c.stroke();
    [[-64, -108], [0, -130], [64, -108]].forEach(function (p) {
      c.fillStyle = gold; c.beginPath(); c.moveTo(p[0] - 12, p[1]); c.lineTo(p[0] + 12, p[1]); c.lineTo(p[0] + 7, p[1] + 10); c.lineTo(p[0] - 7, p[1] + 10); c.closePath(); c.fill();
      c.fillStyle = CREAM; c.fillRect(p[0] - 5, p[1] - 38, 10, 38);
      flame(c, p[0], p[1] - 38, 1);
    });
    c.restore();
  }
  function fit(c, text, weight, max, width) {
    var size = max;
    do { c.font = weight + " " + size + "px system-ui, 'Segoe UI', Roboto, Arial, sans-serif"; size -= 2; } while (c.measureText(text).width > width && size > 14);
  }
  function wrap(c, text, width) {
    var words = text.split(" "), lines = [], cur = "";
    words.forEach(function (w) { var t = cur ? cur + " " + w : w; if (c.measureText(t).width > width && cur) { lines.push(cur); cur = w; } else cur = t; });
    if (cur) lines.push(cur);
    return lines;
  }

  function draw() {
    var data = rows(), W = 1080, HEAD = 400, TH = 84, RH = 46, FOOT = 330;
    var H = HEAD + TH + data.length * RH + FOOT;
    var cv = doc.createElement("canvas"); cv.width = W; cv.height = H;
    var c = cv.getContext("2d");
    var rtl = S.cfg && S.cfg.dir === "rtl" || doc.documentElement.dir === "rtl";
    var bg = c.createLinearGradient(0, 0, 0, H); bg.addColorStop(0, BG1); bg.addColorStop(1, BG2);
    c.fillStyle = bg; c.fillRect(0, 0, W, H);
    pattern(c, W, H);
    glow(c, W / 2, 130, 330, "rgba(255,196,80,0.22)");
    // çerçeve
    c.strokeStyle = GOLD; c.lineWidth = 4; rr(c, 20, 20, W - 40, H - 40, 26); c.stroke();
    c.strokeStyle = "rgba(224,187,98,0.45)"; c.lineWidth = 1.5; rr(c, 34, 34, W - 68, H - 68, 18); c.stroke();
    // süsler
    lantern(c, 150, 44, 0.78); lantern(c, W - 150, 44, 0.78);
    crescent(c, W / 2, 128, 64);
    c.fillStyle = GOLD; star8(c, W / 2 + 62, 118, 15); c.fill(); star8(c, W / 2 - 96, 80, 9); c.fill(); star8(c, W / 2 + 100, 66, 7); c.fill();
    // başlık
    var yr = data[0].y, lc = S.locale || "tr", c0 = ram.from.split("-").map(Number), c1 = ram.to.split("-").map(Number);
    if (rtl) c.direction = "rtl";
    c.textAlign = "center"; c.textBaseline = "alphabetic";
    var title = S.tpl(T.title, { year: yr }).toLocaleUpperCase(lc);
    fit(c, title, 800, 56, 760); c.fillStyle = CREAM; c.fillText(title, W / 2, 266);
    fit(c, place.name, 700, 40, 700); c.fillStyle = GOLD; c.fillText(place.name, W / 2, 318);
    var range = dateFull(c0[0], c0[1], c0[2]) + " – " + dateFull(c1[0], c1[1], c1[2]);
    fit(c, range, 500, 26, 860); c.fillStyle = "rgba(255,244,214,0.85)"; c.fillText(range, W / 2, 362);
    c.direction = "ltr";
    // tablo başlığı
    var X0 = 50, cols = [104, 180, 116, 116, 116, 116, 116, 116], x = X0, xs = [];
    cols.forEach(function (w) { xs.push(x); x += w; });
    var ty = HEAD + 6;
    var hg = c.createLinearGradient(0, ty, 0, ty + TH - 10); hg.addColorStop(0, "rgba(224,187,98,0.34)"); hg.addColorStop(1, "rgba(224,187,98,0.14)");
    c.fillStyle = hg; rr(c, X0, ty, 980, TH - 10, 14); c.fill();
    c.strokeStyle = "rgba(224,187,98,0.6)"; c.lineWidth = 1.5; c.stroke();
    function cell(txt, i, y, font, col) { c.font = font; c.fillStyle = col; c.fillText(txt, xs[i] + cols[i] / 2, y); }
    var HF = "700 22px system-ui, 'Segoe UI', Arial, sans-serif", HS = "600 17px system-ui, 'Segoe UI', Arial, sans-serif";
    cell(T.day, 0, ty + 44, "700 19px system-ui, Arial, sans-serif", CREAM); cell(T.date, 1, ty + 44, HF, CREAM);
    ORDER.forEach(function (k, i) {
      var sp = k === "fajr" ? T.sahur : k === "maghrib" ? T.iftar : "";
      cell(T.names[k], i + 2, sp ? ty + 36 : ty + 44, HF, sp ? GOLD : CREAM);
      if (sp) cell("(" + sp + ")", i + 2, ty + 58, HS, GOLD);
    });
    // satırlar
    data.forEach(function (r, j) {
      var y = HEAD + TH + j * RH;
      if (r.wd === 4) { c.fillStyle = "rgba(224,187,98,0.16)"; rr(c, X0, y + 2, 980, RH - 4, 10); c.fill(); }
      else if (j % 2 === 0) { c.fillStyle = "rgba(255,255,255,0.06)"; rr(c, X0, y + 2, 980, RH - 4, 10); c.fill(); }
      var base = y + RH / 2 + 8;
      cell(String(r.n), 0, base, "700 22px system-ui, 'Segoe UI', Arial, sans-serif", GOLD);
      cell(dateShort(r) + " · " + ((CAL.weekdaysShort || CAL.weekdays || [])[r.wd] || ""), 1, base, "500 20px system-ui, 'Segoe UI', Arial, sans-serif", CREAM);
      r.t.forEach(function (tx, i) {
        var hl = i === 0 || i === 4;
        cell(tx, i + 2, base, (hl ? "800 25px " : "500 23px ") + "system-ui, 'Segoe UI', Arial, sans-serif", hl ? "#ffd978" : CREAM);
      });
    });
    // alt imza
    var fy = HEAD + TH + data.length * RH + 30;
    c.strokeStyle = "rgba(224,187,98,0.5)"; c.lineWidth = 1.5; c.beginPath(); c.moveTo(120, fy); c.lineTo(W - 120, fy); c.stroke();
    star8(c, W / 2, fy, 11); c.fillStyle = GOLD; c.fill();
    candelabra(c, 190, fy + 215, 0.95); candelabra(c, W - 190, fy + 215, 0.95);
    c.textAlign = "center";
    c.font = "800 62px system-ui, 'Segoe UI', Arial, sans-serif"; c.fillStyle = GOLD; c.fillText("Bumba Group", W / 2, fy + 100);
    c.font = "600 26px system-ui, 'Segoe UI', Arial, sans-serif"; c.fillStyle = CREAM; c.fillText("Bumba Life · Saat Tarih · " + location.host, W / 2, fy + 142);
    c.font = "400 19px system-ui, 'Segoe UI', Arial, sans-serif"; c.fillStyle = "rgba(255,244,214,0.75)";
    if (rtl) c.direction = "rtl";
    wrap(c, T.note, 560).slice(0, 3).forEach(function (ln, i) { c.fillText(ln, W / 2, fy + 186 + i * 26); });
    return cv;
  }

  function fileName() {
    var slug = place.name.toLowerCase().replace(/ı/g, "i").replace(/ş/g, "s").replace(/ğ/g, "g").replace(/ü/g, "u").replace(/ö/g, "o").replace(/ç/g, "c").normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    return (T.file || "imsakiye") + (slug ? "-" + slug : "") + "-" + ram.from.slice(0, 4) + ".png";
  }
  function make() {
    var cv = draw();
    return { url: cv.toDataURL("image/png"), w: cv.width, h: cv.height };
  }
  function download() {
    if (!ram) return;
    var g = make(), a = doc.createElement("a");
    a.href = g.url; a.download = fileName(); doc.body.appendChild(a); a.click(); doc.body.removeChild(a);
    S.track("imsakiye", { eylem: "indir" });
  }
  function open() {
    if (!ram || !dlg) return;
    var g = make();
    img.src = g.url; img.width = g.w; img.height = g.h;
    img.alt = S.tpl(T.alt, { place: place.name, year: ram.from.slice(0, 4) });
    if (typeof dlg.showModal === "function") { if (!dlg.open) dlg.showModal(); } else dlg.setAttribute("open", "");
    S.track("imsakiye", { eylem: "ac" });
  }

  function update(p) {
    place = p;
    ram = pickRamadan(p.tz);
    box.hidden = !ram;
    if (!ram) return;
    var t = S.tpl(T.title, { year: ram.from.slice(0, 4) });
    if (heading) heading.textContent = t;
    if (dtitle) dtitle.textContent = t;
  }

  Array.prototype.forEach.call(box.querySelectorAll("[data-im-download]"), function (b) { b.addEventListener("click", download); });
  box.querySelector("[data-im-open]").addEventListener("click", open);
  if (dlg) dlg.addEventListener("click", function (e) { if (e.target === dlg || (e.target.closest && e.target.closest("[data-im-close]"))) { if (typeof dlg.close === "function") dlg.close(); else dlg.removeAttribute("open"); } });

  var root = doc.querySelector("[data-prayer]");
  if (root && root.getAttribute("data-mode") === "city") {
    update({ name: root.getAttribute("data-name"), lat: parseFloat(root.getAttribute("data-lat")), lon: parseFloat(root.getAttribute("data-lon")), tz: TR_TZ });
  } else if (window.sthPlace) {
    window.sthPlace.use(update);
  }
})();
