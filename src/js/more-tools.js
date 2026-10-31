/* Ek araçlar (30 Ekim 2026, rakip analizi K maddeleri): hicri takvim çevirici, saat hesaplama, kaç gün kaldı, aralık
   zamanlayıcı, satranç saati, metronom, konuşan saat, burç hesaplama. main.js'in window.sth'i ile çalışır; ağa çıkmaz
   (yalnız kaç gün kaldı sitenin kendi events JSON'unu okur). Ses Web Audio ile üretilir; konuşma tarayıcının cihazdaki
   sesleriyle (localService), cihaz içi ses yoksa konuşulmaz. */
(function () {
  "use strict";
  var S = window.sth, root = document.querySelector("[data-mt]");
  if (!S || !root) return;
  var doc = document, M = (S.T && S.T.mt) || {}, tpl = S.tpl, kind = root.getAttribute("data-mt");
  var CAL = S.cfg.cal || {};
  function q(sel) { return doc.querySelector(sel); }
  function qa(sel) { return Array.prototype.slice.call(doc.querySelectorAll(sel)); }
  function pad(n) { return String(n).padStart(2, "0"); }
  function setText(el, v) { if (el && el.textContent !== v) el.textContent = v; }
  var used = false;
  function used1() { if (!used) { used = true; S.track("arac", { arac: kind }); } }
  function numFmt(opts) { return new Intl.NumberFormat(/^(ar|fa|ur)$/.test(S.lang) ? "en" : S.locale, opts); }
  function load(key, fb) { try { return Object.assign({}, fb, JSON.parse(S.store(key) || "{}")); } catch (e) { return fb; } }
  function save(key, v) { S.store(key, JSON.stringify(v)); }
  function typing(ev) { var t = ev.target; return t && (/^(INPUT|TEXTAREA|SELECT)$/.test(t.tagName) || t.isContentEditable); }

  /* Gün numarası (UTC gece yarısı) ve yerel bugün */
  function dnum(iso) { return Math.round(Date.parse(iso + "T00:00:00Z") / 86400000); }
  function isoOf(n) { return new Date(n * 86400000).toISOString().slice(0, 10); }
  function todayIso() { var d = S.nowDate(); return d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate()); }
  function dateText(iso) { var p = iso.split("-").map(Number); return tpl(CAL.dateFmt, { day: p[2], month: CAL.months[p[1] - 1], year: p[0] }); }
  function longText(iso) { var p = iso.split("-").map(Number); return S.longDate({ year: p[0], month: p[1], day: p[2] }); }
  function clockText(min) {
    min = ((min % 1440) + 1440) % 1440;
    var h = Math.floor(min / 60), m = min % 60;
    if (S.prefs && S.prefs.h12) return (h % 12 || 12) + ":" + pad(m) + (h < 12 ? " AM" : " PM");
    return pad(h) + ":" + pad(m);
  }

  /* ---------- Ses ---------- */
  var audio = null;
  function ctx() {
    try {
      if (!audio) audio = new (window.AudioContext || window.webkitAudioContext)();
      if (audio.state === "suspended") audio.resume();
    } catch (e) {}
    return audio;
  }
  function tone(freq, dur, when, vol, type, at) {
    var c = ctx(); if (!c) return;
    var o = c.createOscillator(), g = c.createGain(), t = (at || c.currentTime) + (when || 0);
    o.type = type || "sine"; o.frequency.value = freq;
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(Math.max(0.0002, vol == null ? 0.3 : vol), t + 0.005);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(c.destination); o.start(t); o.stop(t + dur + 0.03);
  }
  function chime() { tone(880, 0.25, 0, 0.3); tone(1318.5, 0.4, 0.18, 0.25); }

  /* ---------- Konuşma: yalnız cihazdaki sesler (metin dışarı gitmesin); Azericede Türkçe ses ---------- */
  var synth = window.speechSynthesis, voice = null, voiceTried = false;
  function pickVoice() {
    if (!synth) return null;
    var list = synth.getVoices().filter(function (v) { return v.localService; });
    var want = [S.lang].concat(S.lang === "az" ? ["tr"] : []);
    for (var i = 0; i < want.length; i++) {
      var v = list.filter(function (x) { return x.lang && x.lang.toLowerCase().slice(0, 2) === want[i]; });
      if (v.length) return v.filter(function (x) { return x.default; })[0] || v[0];
    }
    return null;
  }
  function voiceReady(cb) {
    if (!synth) { cb(null); return; }
    voice = pickVoice(); voiceTried = true;
    if (voice || synth.getVoices().length) { cb(voice); return; }
    var done = false, fin = function () { if (done) return; done = true; voice = pickVoice(); cb(voice); };
    synth.addEventListener("voiceschanged", fin, { once: true });
    setTimeout(fin, 1500);
  }
  function speak(text) {
    if (!synth || !text) return;
    if (!voiceTried) voice = pickVoice();
    if (!voice) return;
    try {
      synth.cancel();
      var u = new SpeechSynthesisUtterance(text); u.voice = voice; u.lang = voice.lang; u.rate = 1;
      synth.speak(u);
    } catch (e) {}
  }

  /* Ekranı açık tut (çalışırken) */
  var lock = null;
  function keepAwake(on) {
    if (!navigator.wakeLock) return;
    if (on && !lock) navigator.wakeLock.request("screen").then(function (l) { lock = l; l.addEventListener("release", function () { lock = null; }); }).catch(function () {});
    else if (!on && lock) { lock.release().catch(function () {}); lock = null; }
  }
  var fsBtn = root.querySelector("[data-fs]");
  if (fsBtn && S.makeFullscreen) S.makeFullscreen(root, fsBtn);

  /* ---------- Hicri takvim çevirici (#13): Diyanet'e hizalı tablo, dışında Umm al-Qura ---------- */
  function hijri() {
    var T = M.table || [], first = T[0][0], end = T[T.length - 1][0];
    var UQ = null;
    try { UQ = new Intl.DateTimeFormat("en-u-ca-islamic-umalqura", { year: "numeric", month: "numeric", day: "numeric", timeZone: "UTC" }); } catch (e) {}
    function uq(n) {
      if (!UQ) return null;
      var p = {}; UQ.formatToParts(new Date(n * 86400000 + 43200000)).forEach(function (x) { p[x.type] = x.value; });
      var y = parseInt(p.year, 10), m = parseInt(p.month, 10), d = parseInt(p.day, 10);
      return isNaN(y + m + d) ? null : [y, m, d];
    }
    function toH(iso) {
      if (iso >= first && iso < end) {
        for (var i = T.length - 2; i >= 0; i--) if (T[i][0] <= iso) return { y: T[i][1], m: T[i][2], d: dnum(iso) - dnum(T[i][0]) + 1 };
      }
      var u = uq(dnum(iso)); return u ? { y: u[0], m: u[1], d: u[2], out: true } : null;
    }
    function toG(y, m, d) {
      for (var i = 0; i < T.length - 1; i++) {
        if (T[i][1] === y && T[i][2] === m) {
          var len = dnum(T[i + 1][0]) - dnum(T[i][0]);
          return d > len ? { err: tpl(M.dayMax, { n: len }) } : { iso: isoOf(dnum(T[i][0]) + d - 1) };
        }
      }
      // Tablonun dışında: tahmin + Umm al-Qura ile düzeltme (Hicri takvim başlangıcı 16 Temmuz 622 = Unix −492148. gün)
      var n = Math.round(-492148 + (y - 1) * 354.36707 + (m - 1) * 29.530589 + d - 1);
      for (var k = 0; k < 6; k++) {
        var u = uq(n); if (!u) return null;
        var diff = Math.round((y - u[0]) * 354.36707 + (m - u[1]) * 29.530589 + (d - u[2]));
        if (!diff) return { iso: isoOf(n), out: true };
        n += diff;
      }
      var last = uq(n);
      if (last && last[0] === y && last[1] !== m && d === 30) return { err: tpl(M.dayMax, { n: 29 }) };
      return null;
    }
    function hText(h) { return tpl(M.fmt, { d: h.d, m: M.months[h.m - 1], y: h.y }); }
    // Bugün
    var today = todayIso(), th = toH(today);
    setText(q("[data-hj-today]"), th ? hText(th) : "–");
    setText(q("[data-hj-today-greg]"), longText(today));
    // Miladi → hicri
    var gIn = q("[data-hj-g]"), r1 = q("[data-hj-r1]"), n1 = q("[data-hj-n1]");
    function run1() {
      var v = gIn.value, h = /^\d{4}-\d\d-\d\d$/.test(v) ? toH(v) : null;
      setText(r1, h ? hText(h) : M.invalid);
      n1.hidden = !(h && h.out); n1.textContent = M.outside;
    }
    gIn.value = today; gIn.addEventListener("input", function () { used1(); run1(); }); run1();
    // Hicri → miladi
    var yIn = q("[data-hj-y]"), mIn = q("[data-hj-m]"), dIn = q("[data-hj-d]"), r2 = q("[data-hj-r2]"), n2 = q("[data-hj-n2]");
    function run2() {
      var y = parseInt(yIn.value, 10), m = parseInt(mIn.value, 10), d = parseInt(dIn.value, 10);
      var g = y >= 1 && d >= 1 && d <= 30 ? toG(y, m, d) : null;
      setText(r2, !g ? M.invalid : g.err ? g.err : longText(g.iso));
      n2.hidden = !(g && g.out); n2.textContent = M.outside;
    }
    if (th) { yIn.value = th.y; mIn.value = th.m; dIn.value = th.d; }
    [yIn, mIn, dIn].forEach(function (el) { el.addEventListener("input", function () { used1(); run2(); }); });
    run2();
    // Bu hicri yılın ay başları (derlemedeki yıl değiştiyse yeniden)
    var tb = q("[data-hj-tbody]"), cap = q("[data-hj-table] caption");
    if (th && tb) {
      var rows = [];
      for (var i = 0; i < T.length - 1; i++) {
        if (T[i][1] !== th.y) continue;
        rows.push("<tr><th scope=\"row\"></th><td></td><td></td></tr>");
      }
      if (rows.length) {
        tb.innerHTML = rows.join("");
        var trs = tb.querySelectorAll("tr"), j = 0;
        for (i = 0; i < T.length - 1; i++) {
          if (T[i][1] !== th.y) continue;
          var c = trs[j++].children;
          c[0].textContent = M.months[T[i][2] - 1]; c[1].textContent = dateText(T[i][0]); c[2].textContent = String(dnum(T[i + 1][0]) - dnum(T[i][0]));
          if (T[i][0] <= today && T[i + 1][0] > today) trs[j - 1].className = "is-today";
        }
        cap.textContent = tpl(M.yearHeading, { y: th.y });
      }
    }
  }

  /* ---------- Saat hesaplama (#31) ---------- */
  function timecalc() {
    var nf2 = numFmt({ maximumFractionDigits: 2 });
    function hm(v) { var m = /^(\d{1,2}):(\d{2})/.exec(v || ""); return m ? +m[1] * 60 + +m[2] : NaN; }
    function dur(min) { return tpl(M.duration, { h: Math.floor(min / 60), m: min % 60 }); }
    var s = q("[data-tc-s]"), e = q("[data-tc-e]"), r1 = q("[data-tc-r1]"), n1 = q("[data-tc-n1]");
    function run1() {
      var a = hm(s.value), b = hm(e.value);
      if (isNaN(a) || isNaN(b)) { setText(r1, "–"); setText(n1, ""); return; }
      var d = b - a, over = d < 0; if (over) d += 1440;
      setText(r1, dur(d) + " · " + tpl(M.decimal, { x: nf2.format(d / 60) }));
      setText(n1, over ? M.overnight : tpl(M.minutesTotal, { n: d }));
    }
    var t = q("[data-tc-t]"), op = q("[data-tc-op]"), h = q("[data-tc-h]"), mi = q("[data-tc-m]"), r2 = q("[data-tc-r2]");
    function run2() {
      var base = hm(t.value), add = (Math.max(0, +h.value || 0) * 60 + Math.max(0, +mi.value || 0)) * (+op.value || 1);
      if (isNaN(base)) { setText(r2, "–"); return; }
      var tot = base + add, days = Math.floor(tot / 1440), extra = "";
      if (days === 1) extra = M.nextDay; else if (days === -1) extra = M.prevDay;
      else if (days > 1) extra = tpl(M.daysLater, { n: days }); else if (days < -1) extra = tpl(M.daysEarlier, { n: -days });
      setText(r2, tpl(M.resultTime, { time: clockText(tot) }) + (extra ? " (" + extra + ")" : ""));
    }
    var sum = q("[data-tc-sum]"), r3 = q("[data-tc-r3]"), n3 = q("[data-tc-n3]");
    function run3() {
      var total = 0, bad = [];
      sum.value.split(/\r?\n/).forEach(function (line, i) {
        var l = line.trim().replace(/\s+/g, ""); if (!l) return;
        var sign = 1; if (/^[-−–]/.test(l)) { sign = -1; l = l.slice(1); } else if (l[0] === "+") l = l.slice(1);
        var p = l.split(/[:.]/);
        if (!p.every(function (x) { return /^\d+$/.test(x); }) || p.length > 3) { bad.push(i + 1); return; }
        var sec = p.length === 1 ? +p[0] * 60 : p.length === 2 ? +p[0] * 3600 + +p[1] * 60 : +p[0] * 3600 + +p[1] * 60 + +p[2];
        total += sign * sec;
      });
      var neg = total < 0, a = Math.abs(total), hh = Math.floor(a / 3600), mm = Math.floor((a % 3600) / 60), ss = a % 60;
      setText(r3, tpl(M.sumResult, { t: (neg ? "−" : "") + hh + ":" + pad(mm) + (ss ? ":" + pad(ss) : "") }) + " · " + tpl(M.decimal, { x: (neg ? "−" : "") + nf2.format(a / 3600) }));
      setText(n3, bad.length ? tpl(M.sumBad, { n: bad.join(", ") }) : "");
    }
    [s, e].forEach(function (el) { el.addEventListener("input", function () { used1(); run1(); }); });
    [t, op, h, mi].forEach(function (el) { el.addEventListener("input", function () { used1(); run2(); }); });
    sum.addEventListener("input", function () { used1(); run3(); });
    run1(); run2(); run3();
  }

  /* ---------- Kaç gün kaldı (#32) ---------- */
  function days() {
    var nf1 = numFmt({ maximumFractionDigits: 1 }), hol = {}, input = q("[data-du-d]");
    function leftText(n, many, one) { return n === 1 ? M[one] : tpl(M[many], { n: n }); }
    function work(from, to) { // (from, to] arası iş günü; tatil 0, yarım gün 0,5
      var n = 0;
      for (var d = from + 1; d <= to; d++) {
        var dow = new Date(d * 86400000).getUTCDay();
        if (dow === 0 || dow === 6) continue;
        var h = hol[isoOf(d)]; n += h === "holiday" ? 0 : h === "half" ? 0.5 : 1;
      }
      return n;
    }
    function run() {
      var v = input.value, lines = q("[data-du-lines]");
      if (!/^\d{4}-\d\d-\d\d$/.test(v)) { setText(q("[data-du-main]"), "–"); lines.hidden = true; return; }
      var t0 = dnum(todayIso()), t1 = dnum(v), n = t1 - t0, a = Math.abs(n);
      setText(q("[data-du-main]"), n === 0 ? M.today : n > 0 ? leftText(n, "left", "left1") : leftText(-n, "passed", "passed1"));
      lines.hidden = n === 0;
      setText(q("[data-du-weeks]"), tpl(M.weeks, { w: Math.floor(a / 7), d: a % 7 }));
      setText(q("[data-du-months]"), tpl(M.months, { n: nf1.format(a / 30.4375) }));
      var wEl = q("[data-du-work]"); wEl.hidden = n <= 0 || n > 3660;
      if (!wEl.hidden) setText(wEl, tpl(M.workdays, { n: nf1.format(work(t0, t1)) }));
      setText(q("[data-du-dow]"), tpl(M.weekday, { d: CAL.weekdays[(new Date(t1 * 86400000).getUTCDay() + 6) % 7] }));
    }
    function table() {
      var t0 = dnum(todayIso()), shown = 0;
      qa("[data-du-row]").forEach(function (tr) {
        var n = dnum(tr.getAttribute("data-du-row")) - t0;
        tr.hidden = n < 0 || shown >= 12;
        if (tr.hidden) return;
        shown++;
        setText(tr.querySelector("[data-du-left]"), n === 0 ? M.today : leftText(n, "daysUnit", "daysUnit1"));
      });
    }
    var ny = new Date(S.nowMs()).getFullYear() + 1;
    var qs = new URLSearchParams(location.search).get("d");
    input.value = qs && /^\d{4}-\d\d-\d\d$/.test(qs) ? qs : ny + "-01-01";
    input.addEventListener("input", function () { used1(); run(); });
    run(); table();
    if (S.cfg.eventsUrl) fetch(S.cfg.eventsUrl, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (list) {
      list.forEach(function (e) { if (e.t === "holiday" || e.t === "half") hol[e.d] = e.t; });
      run();
    }).catch(function () {});
  }

  /* ---------- Aralık zamanlayıcı (#36): uygulamalardaki HiitConfig ile aynı kural (son turdan sonra dinlenme yok) ---------- */
  function interval() {
    var cfgEls = qa("[data-iv-cfg]"), voiceCb = q("[data-iv-voice]");
    var st = load("sth-hiit", { warm: 10, work: 30, rest: 15, rounds: 8, voice: true });
    var segs = [], total = 0, run = false, startMono = 0, base = 0, lastIdx = -1, lastSec = -1, tick = 0;
    var disp = q("[data-iv-display]"), phaseEl = q("[data-iv-phase]"), roundEl = q("[data-iv-round]"), prog = q("[data-iv-progress]"), totEl = q("[data-iv-total]"), bStart = q("[data-iv-start]");
    function clamp(v, a, b, d) { v = Math.round(+v); return isNaN(v) ? d : Math.min(b, Math.max(a, v)); }
    function read() {
      cfgEls.forEach(function (el) { var k = el.getAttribute("data-iv-cfg"); st[k] = clamp(el.value, +el.min, +el.max, st[k]); });
      st.voice = voiceCb.checked; save("sth-hiit", st); plan();
    }
    function plan() {
      segs = []; total = 0;
      if (st.warm > 0) segs.push({ p: "warmup", r: 0, d: st.warm });
      for (var r = 1; r <= st.rounds; r++) {
        segs.push({ p: "work", r: r, d: st.work });
        if (r < st.rounds && st.rest > 0) segs.push({ p: "rest", r: r, d: st.rest });
      }
      segs.forEach(function (s) { s.at = total; total += s.d; });
    }
    function elapsed() { return base + (run ? (S.mono() - startMono) / 1000 : 0); }
    function mmss(sec) { sec = Math.max(0, Math.ceil(sec)); return pad(Math.floor(sec / 60)) + ":" + pad(sec % 60); }
    function phaseName(s) { return s.p === "work" && s.r === st.rounds && st.rounds > 1 ? M.lastRound : M.phases[s.p]; }
    function paint() {
      var e = elapsed();
      if (e >= total) {
        setText(disp, "00:00"); setText(phaseEl, M.phases.done); phaseEl.setAttribute("data-phase", "short");
        prog.value = 0; setText(totEl, tpl(M.total, { t: "00:00" }));
        if (run) { run = false; base = total; clearInterval(tick); keepAwake(false); tone(880, 0.3, 0, 0.3); tone(1108.7, 0.3, 0.3, 0.3); tone(1318.5, 0.6, 0.6, 0.3); if (voiceCb.checked) speak(M.phases.done); }
        bStart.textContent = M.start; lastIdx = -1; return;
      }
      var i = 0; while (i < segs.length - 1 && segs[i + 1].at <= e) i++;
      var s = segs[i], left = s.at + s.d - e;
      setText(disp, mmss(left));
      setText(phaseEl, phaseName(s)); phaseEl.setAttribute("data-phase", s.p === "work" ? "work" : s.p === "rest" ? "short" : "long");
      setText(roundEl, s.r ? tpl(M.roundOf, { n: s.r, total: st.rounds }) : " ");
      prog.value = (left / s.d) * 100;
      setText(totEl, tpl(M.total, { t: mmss(total - e) }));
      if (run) {
        if (i !== lastIdx) {
          if (lastIdx !== -1 || e < 0.5) { chime(); if (voiceCb.checked) speak(phaseName(s)); }
          lastIdx = i;
        }
        var sec = Math.ceil(left);
        if (sec !== lastSec && sec <= 3 && sec >= 1 && s.d > 3) tone(660, 0.12, 0, 0.25);
        lastSec = sec;
      }
    }
    function toggle() {
      ctx(); used1();
      if (run) { base = elapsed(); run = false; clearInterval(tick); keepAwake(false); bStart.textContent = M.resume; paint(); return; }
      if (base >= total) { base = 0; lastIdx = -1; }
      voiceReady(function () {});
      startMono = S.mono(); run = true; keepAwake(true); bStart.textContent = M.pause;
      clearInterval(tick); tick = setInterval(paint, 100); paint();
    }
    function reset() { run = false; base = 0; lastIdx = -1; clearInterval(tick); keepAwake(false); bStart.textContent = M.start; paint(); }
    cfgEls.forEach(function (el) { el.value = st[el.getAttribute("data-iv-cfg")]; el.addEventListener("change", function () { read(); if (!run) reset(); }); });
    voiceCb.checked = st.voice !== false; voiceCb.addEventListener("change", read);
    qa("[data-iv-preset]").forEach(function (b) {
      b.addEventListener("click", function () {
        var v = b.getAttribute("data-iv-preset").split(",").map(Number);
        st.warm = v[0]; st.work = v[1]; st.rest = v[2]; st.rounds = v[3];
        cfgEls.forEach(function (el) { el.value = st[el.getAttribute("data-iv-cfg")]; });
        read(); reset();
      });
    });
    bStart.addEventListener("click", toggle);
    q("[data-iv-reset]").addEventListener("click", reset);
    doc.addEventListener("keydown", function (ev) { if (ev.key === " " && !typing(ev) && ev.target.tagName !== "BUTTON") { ev.preventDefault(); toggle(); } });
    plan(); paint();
  }

  /* ---------- Satranç saati (#37): Fischer artışı, hamle sayısı ---------- */
  function chess() {
    var ctrl = q("[data-ch-control]"), minIn = q("[data-ch-min]"), incIn = q("[data-ch-inc]"), msg = q("[data-ch-msg]"), pauseBtn = q("[data-ch-pause]");
    var sides = { white: q("[data-ch-side=white]"), black: q("[data-ch-side=black]") };
    var time = {}, moves = {}, inc = 0, turn = null, running = false, flag = null, last = 0, tick = 0;
    function other(c) { return c === "white" ? "black" : "white"; }
    function fmt(ms) {
      ms = Math.max(0, ms);
      if (ms < 10000) return (Math.floor(ms / 100) / 10).toFixed(1);
      var s = Math.ceil(ms / 1000), h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60);
      return (h ? h + ":" + pad(m) : pad(m)) + ":" + pad(s % 60);
    }
    function setup() {
      var v = ctrl.value, custom = v === "custom";
      minIn.disabled = incIn.disabled = !custom;
      var mins = custom ? Math.min(180, Math.max(1, +minIn.value || 5)) : +v.split(",")[0];
      inc = (custom ? Math.min(60, Math.max(0, +incIn.value || 0)) : +v.split(",")[1]) * 1000;
      if (!custom) { minIn.value = mins; incIn.value = inc / 1000; }
      time = { white: mins * 60000, black: mins * 60000 }; moves = { white: 0, black: 0 };
      turn = null; running = false; flag = null; clearInterval(tick); keepAwake(false);
      msg.hidden = true; pauseBtn.textContent = M.pause; pauseBtn.disabled = true; paint();
    }
    function paint() {
      ["white", "black"].forEach(function (c) {
        var el = sides[c];
        setText(el.querySelector("[data-ch-time]"), fmt(time[c]));
        setText(el.querySelector("[data-ch-moves]"), moves[c] ? tpl(M.moves, { n: moves[c] }) : " ");
        el.classList.toggle("is-turn", turn === c && !flag);
        el.classList.toggle("is-flag", flag === c);
      });
    }
    function step() {
      if (!running || !turn) return;
      var now = S.mono(); time[turn] -= now - last; last = now;
      if (time[turn] <= 0) {
        time[turn] = 0; flag = turn; running = false; clearInterval(tick); keepAwake(false); pauseBtn.disabled = true;
        msg.textContent = tpl(M.flag, { side: M[turn] }); msg.hidden = false;
        tone(988, 0.25, 0, 0.35); tone(988, 0.25, 0.35, 0.35); tone(988, 0.5, 0.7, 0.35);
      }
      paint();
    }
    function start() { last = S.mono(); running = true; clearInterval(tick); tick = setInterval(step, 50); keepAwake(true); pauseBtn.disabled = false; pauseBtn.textContent = M.pause; }
    function press(c) {
      if (flag) return;
      ctx(); used1();
      if (!turn) { turn = other(c); start(); tone(1200, 0.05, 0, 0.2); paint(); return; }
      if (!running || turn !== c) return;
      step(); if (flag) return;
      time[c] += inc; moves[c]++; turn = other(c); last = S.mono();
      tone(1200, 0.05, 0, 0.2); paint();
    }
    ["white", "black"].forEach(function (c) { sides[c].addEventListener("click", function () { press(c); }); });
    pauseBtn.addEventListener("click", function () {
      if (!turn || flag) return;
      if (running) { step(); running = false; clearInterval(tick); keepAwake(false); pauseBtn.textContent = M.resume; }
      else start();
    });
    q("[data-ch-reset]").addEventListener("click", setup);
    [ctrl, minIn, incIn].forEach(function (el) { el.addEventListener("change", setup); });
    doc.addEventListener("keydown", function (ev) {
      if (ev.key !== " " || typing(ev)) return;
      ev.preventDefault();
      press(turn || "black");
    });
    setup();
  }

  /* ---------- Metronom (#38): Web Audio zamanlayıcısı (ileriye 0,1 sn planlar) ---------- */
  function metronome() {
    var st = load("sth-metro", { bpm: 100, beats: 4, sub: 1, accent: true, vol: 70 });
    var range = q("[data-mm-range]"), num = q("[data-mm-bpm]"), show = q("[data-mm-bpm-show]"), tempo = q("[data-mm-tempo]");
    var beatsSel = q("[data-mm-beats]"), subSel = q("[data-mm-sub]"), accentCb = q("[data-mm-accent]"), volIn = q("[data-mm-vol]"), dots = q("[data-mm-dots]"), bStart = q("[data-mm-start]");
    var playing = false, timer = 0, nextT = 0, idx = 0, taps = [];
    var NAMES = [[40, "Grave"], [60, "Largo"], [66, "Larghetto"], [76, "Adagio"], [108, "Andante"], [120, "Moderato"], [156, "Allegro"], [176, "Vivace"], [200, "Presto"], [999, "Prestissimo"]];
    function persist() { save("sth-metro", st); }
    function setBpm(v) {
      st.bpm = Math.min(250, Math.max(30, Math.round(+v) || 100));
      range.value = st.bpm; num.value = st.bpm; setText(show, String(st.bpm));
      for (var i = 0; i < NAMES.length; i++) if (st.bpm < NAMES[i][0]) { setText(tempo, NAMES[i][1]); break; }
      persist();
    }
    function drawDots() {
      dots.textContent = "";
      for (var i = 0; i < st.beats; i++) { var s = doc.createElement("span"); if (i === 0 && st.accent) s.className = "is-accent"; dots.appendChild(s); }
    }
    function flash(beat, when) {
      var c = ctx(); var delay = Math.max(0, (when - c.currentTime) * 1000);
      setTimeout(function () {
        if (!playing) return;
        Array.prototype.forEach.call(dots.children, function (d, i) { d.classList.toggle("is-on", i === beat); });
      }, delay);
    }
    function schedule() {
      var c = ctx(); if (!c) return;
      var per = 60 / st.bpm / st.sub, vol = (st.vol / 100) * 0.6;
      while (nextT < c.currentTime + 0.12) {
        var main = idx % st.sub === 0, beat = Math.floor(idx / st.sub) % st.beats;
        if (main) {
          var acc = beat === 0 && st.accent && st.beats > 1;
          tone(acc ? 1568 : 1046.5, 0.05, 0, acc ? vol : vol * 0.8, "square", nextT);
          flash(beat, nextT);
        } else tone(784, 0.03, 0, vol * 0.45, "square", nextT);
        nextT += per; idx++;
      }
    }
    function startStop() {
      var c = ctx(); if (!c) return;
      used1();
      if (playing) { playing = false; clearInterval(timer); bStart.textContent = M.start; Array.prototype.forEach.call(dots.children, function (d) { d.classList.remove("is-on"); }); return; }
      playing = true; idx = 0; nextT = c.currentTime + 0.06; bStart.textContent = M.stop;
      clearInterval(timer); timer = setInterval(schedule, 25); schedule();
    }
    range.addEventListener("input", function () { setBpm(range.value); });
    num.addEventListener("change", function () { setBpm(num.value); });
    qa("[data-mm-step]").forEach(function (b) { b.addEventListener("click", function () { setBpm(st.bpm + +b.getAttribute("data-mm-step")); }); });
    beatsSel.addEventListener("change", function () { st.beats = +beatsSel.value; idx = 0; drawDots(); persist(); });
    subSel.addEventListener("change", function () { st.sub = +subSel.value; idx = Math.ceil(idx / st.sub) * st.sub; persist(); });
    accentCb.addEventListener("change", function () { st.accent = accentCb.checked; drawDots(); persist(); });
    volIn.addEventListener("input", function () { st.vol = +volIn.value; persist(); });
    q("[data-mm-tap]").addEventListener("click", function () {
      var now = performance.now();
      if (taps.length && now - taps[taps.length - 1] > 2000) taps = [];
      taps.push(now); if (taps.length > 6) taps.shift();
      if (taps.length >= 2) setBpm(60000 / ((taps[taps.length - 1] - taps[0]) / (taps.length - 1)));
    });
    bStart.addEventListener("click", startStop);
    doc.addEventListener("keydown", function (ev) { if (ev.key === " " && !typing(ev) && ev.target.tagName !== "BUTTON") { ev.preventDefault(); startStop(); } });
    beatsSel.value = st.beats; subSel.value = st.sub; accentCb.checked = st.accent !== false; volIn.value = st.vol;
    setBpm(st.bpm); drawDots();
  }

  /* ---------- Konuşan saat (#39): saatin yazıyla söylenişi (client.words) cihazdaki sesle okunur ---------- */
  function talking() {
    var every = q("[data-tk-every]"), chimeCb = q("[data-tk-chime]"), noVoice = q("[data-tk-novoice]"), lastKey = "";
    function sayNow() {
      var text = S.wordsText(S.parts(S.nowDate(), S.localTz));
      if (chimeCb.checked) { chime(); setTimeout(function () { speak(text); }, 700); } else speak(text);
    }
    voiceReady(function (v) { noVoice.hidden = !!v; });
    q("[data-tk-say]").addEventListener("click", function () { ctx(); used1(); sayNow(); });
    setInterval(function () {
      var n = every.value; if (n === "off") return;
      var p = S.parts(S.nowDate(), S.localTz), key = p.hour + ":" + p.minute;
      if (p.second > 2 || key === lastKey) return;
      if (n === "60" ? p.minute === 0 : p.minute % +n === 0) { lastKey = key; sayNow(); }
    }, 500);
    every.addEventListener("change", function () { ctx(); used1(); });
  }

  /* ---------- Burç (#41): Güneş ve Ay'ın tutulum boylamı (tropikal zodyak) ---------- */
  function zodiac() {
    var N = window.sthNature, rad = Math.PI / 180;
    function sunLon(ms) { // düşük duyarlıklı (≈0,01°), prayer-calc.js sunPos ile aynı formül
      var D = ms / 86400000 + 2440587.5 - 2451545, g = (357.529 + 0.98560028 * D) * rad, qq = 280.459 + 0.98564736 * D;
      return (((qq + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g)) % 360) + 360) % 360;
    }
    function moonLon(ms) {
      var p = N.moonPos(ms), D = ms / 86400000 + 2440587.5 - 2451545, e = (23.439 - 0.00000036 * D) * rad, a = p.ra * rad, d = p.dec * rad;
      return ((Math.atan2(Math.sin(a) * Math.cos(e) + Math.tan(d) * Math.sin(e), Math.cos(a)) / rad) % 360 + 360) % 360;
    }
    function sign(lon) { return Math.floor(lon / 30) % 12; }
    function label(i) { return M.sym[i] + "︎ " + M.signs[i]; }
    var dIn = q("[data-zo-d]"), tIn = q("[data-zo-t]");
    function run() {
      var m = /^(\d{4})-(\d\d)-(\d\d)$/.exec(dIn.value);
      if (!m) { setText(q("[data-zo-sun]"), "–"); setText(q("[data-zo-moon]"), "–"); return; }
      var tm = /^(\d\d):(\d\d)/.exec(tIn.value || ""), hh = tm ? +tm[1] : 12, mm = tm ? +tm[2] : 0;
      var at = new Date(+m[1], +m[2] - 1, +m[3], hh, mm).getTime();
      var s0 = sign(sunLon(new Date(+m[1], +m[2] - 1, +m[3], 0, 0).getTime())), s1 = sign(sunLon(new Date(+m[1], +m[2] - 1, +m[3], 23, 59).getTime()));
      setText(q("[data-zo-sun]"), label(sign(sunLon(at))));
      setText(q("[data-zo-moon]"), N && N.moonPos ? label(sign(moonLon(at))) : "–");
      var notes = [];
      if (s0 !== s1) notes.push(M.cusp);
      if (!tm) notes.push(M.noTime);
      setText(q("[data-zo-note]"), notes.join(" ") || " ");
    }
    [dIn, tIn].forEach(function (el) { el.addEventListener("input", function () { used1(); run(); }); });
    run();
  }

  ({ hijri: hijri, timecalc: timecalc, days: days, interval: interval, chess: chess, metronome: metronome, talking: talking, zodiac: zodiac }[kind] || function () {})();
})();
