/* Saat araçları: kronometre, geri sayım, alarm, pomodoro. main.js'in sunduğu window.sth ile çalışır.
   Süre ölçümü performance.now() (cihaz saatinden bağımsız, tek yönlü) ile yapılır; sayfa yenilenince
   sürdürmek için duvar saati yalnız saklama içindir. Tarihe geri sayım ve alarm sunucu-senkron saati kullanır. */
(function () {
  "use strict";
  var S = window.sth;
  var root = document.querySelector("[data-tool]");
  if (!S || !root) return;
  var doc = document;
  var TT = (S.T && S.T.tools) || {};
  var CM = TT.common || {};
  var kind = root.getAttribute("data-tool");
  var tpl = S.tpl, store = S.store;
  var baseTitle = doc.title;

  function q(sel) { return root.querySelector(sel); }
  function qa(sel) { return Array.prototype.slice.call(root.querySelectorAll(sel)); }
  function pad(n, w) { return String(Math.floor(n)).padStart(w || 2, "0"); }
  function load(key, fallback) { try { return Object.assign({}, fallback, JSON.parse(store(key) || "{}")); } catch (e) { return fallback; } }
  function save(key, v) { store(key, JSON.stringify(v)); }
  function hms(ms, showHours) {
    var t = Math.max(0, Math.ceil(ms / 1000)), h = Math.floor(t / 3600), m = Math.floor((t % 3600) / 60), s = t % 60;
    return (showHours || h ? pad(h) + ":" : "") + pad(m) + ":" + pad(s);
  }
  var used = false;
  function used1() { if (!used) { used = true; S.track("arac", { arac: kind }); } }
  function setTitle(t) { doc.title = t ? t + " · " + baseTitle : baseTitle; }

  /* ---------- Ses, bildirim, ortak "çalıyor" afişi ---------- */
  var audio = null;
  function ctx() {
    try {
      if (!audio) audio = new (window.AudioContext || window.webkitAudioContext)();
      if (audio.state === "suspended") audio.resume();
    } catch (e) {}
    return audio;
  }
  function beep(freq, dur, when) {
    var c = ctx(); if (!c) return;
    var o = c.createOscillator(), g = c.createGain(), t = c.currentTime + when;
    o.type = "sine"; o.frequency.value = freq;
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(0.35, t + 0.02);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(c.destination); o.start(t); o.stop(t + dur + 0.05);
  }
  function soundOn() { var cb = q("[data-sound]"); return !cb || cb.checked; }
  function chime() { if (!soundOn()) return; beep(880, 0.18, 0); beep(880, 0.18, 0.26); beep(1175, 0.32, 0.52); }
  var testBtn = q("[data-test-sound]");
  if (testBtn) testBtn.addEventListener("click", function () { ctx(); chime(); });

  var notifyBtn = q("[data-notify]");
  function notifyLabel() {
    if (!notifyBtn) return;
    if (!("Notification" in window)) { notifyBtn.hidden = true; return; }
    if (Notification.permission === "granted") { notifyBtn.textContent = CM.notifyOn; notifyBtn.disabled = true; }
    else if (Notification.permission === "denied") { notifyBtn.textContent = CM.notifyDenied; notifyBtn.disabled = true; }
  }
  if (notifyBtn) {
    notifyLabel();
    notifyBtn.addEventListener("click", function () {
      try { Notification.requestPermission().then(notifyLabel); } catch (e) { notifyLabel(); }
    });
  }
  function notify(title, body) {
    try {
      if ("Notification" in window && Notification.permission === "granted") new Notification(title, { body: body || "", icon: "/icon-192.png", tag: "sth-" + kind });
    } catch (e) {}
  }

  var banner = q("[data-ring-banner]");
  var ringTimer = null, flashTimer = null, ringOnStop = null, ringUntil = 0;
  function ring(title, onStop) {
    ringOnStop = onStop || null;
    ringUntil = performance.now() + 5 * 60 * 1000;
    if (banner) { q("[data-ring-title]").textContent = title; banner.hidden = false; var b = banner.querySelector("[data-ring-stop]"); if (b) b.focus(); }
    notify(title, baseTitle);
    clearInterval(ringTimer); clearInterval(flashTimer);
    chime();
    ringTimer = setInterval(function () { if (performance.now() > ringUntil) stopRing(); else chime(); }, 2200);
    var on = false;
    flashTimer = setInterval(function () { on = !on; doc.title = on ? "🔔 " + title : baseTitle; }, 800);
  }
  function stopRing() {
    clearInterval(ringTimer); clearInterval(flashTimer); ringTimer = flashTimer = null;
    doc.title = baseTitle;
    if (banner) banner.hidden = true;
    var cb = ringOnStop; ringOnStop = null; if (cb) cb();
  }
  if (banner) {
    var stopBtn = banner.querySelector("[data-ring-stop]");
    if (stopBtn) stopBtn.addEventListener("click", stopRing);
  }

  S.makeFullscreen(root, q("[data-fs]"));
  function typing(ev) { var t = ev.target; return t && (/^(INPUT|TEXTAREA|SELECT)$/.test(t.tagName) || t.isContentEditable); }
  function onButton(ev) { return ev.target && ev.target.tagName === "BUTTON"; }

  /* ---------- Kronometre ---------- */
  function stopwatch() {
    var P = TT.stopwatch || {};
    var disp = q("[data-sw-display]"), bStart = q("[data-sw-start]"), bLap = q("[data-sw-lap]"), bReset = q("[data-sw-reset]");
    var lapsBox = q("[data-sw-laps]"), body = q("[data-sw-body]"), empty = q("[data-sw-empty]"), ann = q("[data-sw-announce]");
    var st = load("sth-sw", { run: false, wall: 0, base: 0, laps: [] });
    var t0 = 0, raf = 0, titleTick = 0;
    if (st.run) { st.base += Math.max(0, Date.now() - st.wall); }
    function elapsed() { return st.run ? st.base + (performance.now() - t0) : st.base; }
    function fmt(ms) {
      var c = Math.floor(ms / 10) % 100, s = Math.floor(ms / 1000) % 60, m = Math.floor(ms / 60000) % 60, h = Math.floor(ms / 3600000);
      return pad(h) + ":" + pad(m) + ":" + pad(s) + "." + pad(c);
    }
    function persist() { save("sth-sw", { run: st.run, wall: Date.now(), base: st.run ? st.base + (performance.now() - t0) : st.base, laps: st.laps }); }
    function paint() {
      disp.textContent = fmt(elapsed());
      if (st.run) raf = requestAnimationFrame(paint);
    }
    function lapTimes() { return st.laps.map(function (tot, i) { return tot - (i ? st.laps[i - 1] : 0); }); }
    function renderLaps() {
      var lt = lapTimes(), min = Math.min.apply(null, lt), max = Math.max.apply(null, lt);
      body.textContent = "";
      for (var i = lt.length - 1; i >= 0; i--) {
        var tr = doc.createElement("tr"), th = doc.createElement("th"), a = doc.createElement("td"), b = doc.createElement("td");
        th.scope = "row"; th.textContent = String(i + 1);
        a.textContent = fmt(lt[i]); b.textContent = fmt(st.laps[i]);
        if (lt.length > 1 && lt[i] === min) { tr.className = "lap-best"; a.textContent += " · " + P.best; }
        else if (lt.length > 1 && lt[i] === max) { tr.className = "lap-worst"; a.textContent += " · " + P.worst; }
        tr.appendChild(th); tr.appendChild(a); tr.appendChild(b); body.appendChild(tr);
      }
      lapsBox.hidden = !lt.length; empty.hidden = !!lt.length;
    }
    function buttons() {
      var idle = !st.run && st.base === 0;
      bStart.textContent = st.run ? P.stop : idle ? P.start : P.resume;
      bLap.disabled = !st.run; bReset.disabled = idle;
    }
    function start() {
      ctx(); used1();
      if (st.run) { st.base = elapsed(); st.run = false; cancelAnimationFrame(raf); clearInterval(titleTick); setTitle(""); }
      else { t0 = performance.now(); st.run = true; raf = requestAnimationFrame(paint); titleTick = setInterval(function () { setTitle(fmt(elapsed()).slice(0, 8)); }, 1000); }
      persist(); buttons(); paint();
    }
    function lap() {
      if (!st.run) return;
      st.laps.push(elapsed()); persist(); renderLaps();
      var lt = lapTimes(); ann.textContent = tpl(P.lapAdded, { n: lt.length, t: fmt(lt[lt.length - 1]) });
    }
    function reset() {
      st = { run: false, wall: 0, base: 0, laps: [] }; cancelAnimationFrame(raf); clearInterval(titleTick); setTitle("");
      persist(); buttons(); renderLaps(); paint();
    }
    bStart.addEventListener("click", start); bLap.addEventListener("click", lap); bReset.addEventListener("click", reset);
    doc.addEventListener("keydown", function (ev) {
      if (ev.ctrlKey || ev.metaKey || ev.altKey || typing(ev)) return;
      if (ev.key === " ") { if (onButton(ev)) return; ev.preventDefault(); start(); }
      else if (ev.key === "l" || ev.key === "L") lap();
      else if (ev.key === "r" || ev.key === "R") reset();
    });
    if (st.run) { t0 = performance.now(); raf = requestAnimationFrame(paint); titleTick = setInterval(function () { setTitle(fmt(elapsed()).slice(0, 8)); }, 1000); }
    buttons(); renderLaps(); paint();
  }

  /* ---------- Geri sayım ---------- */
  function countdown() {
    var P = TT.countdown || {};
    var tabs = qa("[data-cd-tab]"), panels = qa("[data-cd-panel]");
    var iH = q("[data-cd-h]"), iM = q("[data-cd-m]"), iS = q("[data-cd-s]"), iT = q("[data-cd-target]");
    var disp = q("[data-cd-display]"), prog = q("[data-cd-progress]"), msg = q("[data-cd-message]");
    var bStart = q("[data-cd-start]"), bReset = q("[data-cd-reset]"), quick = q("[data-cd-quick]");
    var st = load("sth-cd", { mode: "timer", run: false, dur: 300000, rem: 300000, wallEnd: 0, target: "" });
    var endMono = 0, tick = 0;
    function mode() { return st.mode; }
    function setMode(m) {
      st.mode = m;
      tabs.forEach(function (t) { var on = t.getAttribute("data-cd-tab") === m; t.setAttribute("aria-selected", String(on)); t.tabIndex = on ? 0 : -1; });
      panels.forEach(function (p) { p.hidden = p.getAttribute("data-cd-panel") !== m; });
      prog.hidden = m !== "timer";
    }
    function inputDur() { return ((+iH.value || 0) * 3600 + (+iM.value || 0) * 60 + (+iS.value || 0)) * 1000; }
    function setInputs(ms) { var t = Math.round(ms / 1000); iH.value = Math.floor(t / 3600); iM.value = Math.floor((t % 3600) / 60); iS.value = t % 60; }
    function targetMs() { var d = iT.value ? new Date(iT.value) : null; return d && !isNaN(d) ? d.getTime() : 0; }
    function persist() { save("sth-cd", st); }
    function show(text) { if (disp.textContent !== text) disp.textContent = text; }
    function text(ms) {
      if (mode() === "timer") return hms(ms, true);
      var t = Math.max(0, Math.ceil(ms / 1000)), d = Math.floor(t / 86400);
      return (d ? d + " " + P.days + " " : "") + hms((t % 86400) * 1000, true);
    }
    function remaining() {
      if (mode() === "timer") return st.run ? endMono - S.mono() : st.rem;
      return targetMs() - S.nowMs();
    }
    function msgShow(t) { msg.textContent = t || ""; msg.hidden = !t; }
    function buttons() {
      bStart.textContent = st.run ? P.pause : (mode() === "timer" && st.rem < st.dur && st.rem > 0 ? P.resume : (mode() === "date" ? P.targetSet : P.start));
    }
    function finish() {
      st.run = false; st.rem = 0; persist(); clearInterval(tick); tick = 0; show(text(0)); prog.value = 0; buttons();
      msgShow(mode() === "timer" ? P.finished : P.finishedDate);
      ring(mode() === "timer" ? P.finished : P.finishedDate, function () { msgShow(""); });
    }
    function loop() {
      var r = remaining();
      show(text(Math.max(r, 0)));
      if (mode() === "timer") prog.value = st.dur ? Math.max(0, (r / st.dur) * 100) : 0;
      setTitle(st.run ? text(r) : "");
      if (st.run && r <= 0) finish();
    }
    function run() { clearInterval(tick); tick = setInterval(loop, 250); loop(); }
    function start() {
      ctx(); used1(); msgShow("");
      if (st.run) { st.rem = Math.max(0, endMono - S.mono()); st.run = false; clearInterval(tick); tick = 0; setTitle(""); persist(); buttons(); loop(); return; }
      if (mode() === "timer") {
        var d = st.rem > 0 && st.rem < st.dur ? st.rem : inputDur();
        if (d <= 0) { msgShow(P.invalid); return; }
        if (!(st.rem > 0 && st.rem < st.dur)) st.dur = d;
        st.rem = d; endMono = S.mono() + d; st.wallEnd = Date.now() + d; st.run = true;
      } else {
        var tg = targetMs();
        if (!tg || tg <= S.nowMs()) { msgShow(P.invalid); return; }
        st.target = iT.value; st.run = true;
        st.wallEnd = tg;
      }
      persist(); buttons(); run();
    }
    function reset() {
      st.run = false; clearInterval(tick); tick = 0; setTitle(""); msgShow("");
      if (mode() === "timer") { st.dur = inputDur() || 300000; st.rem = st.dur; } else { st.target = ""; iT.value = ""; }
      persist(); buttons(); stopRing(); loop();
    }
    tabs.forEach(function (t) {
      t.addEventListener("click", function () {
        if (st.run) return;
        setMode(t.getAttribute("data-cd-tab")); persist(); buttons(); loop();
      });
      t.addEventListener("keydown", function (ev) {
        if (ev.key !== "ArrowRight" && ev.key !== "ArrowLeft") return;
        var other = tabs.filter(function (x) { return x !== t; })[0]; other.focus(); other.click();
      });
    });
    [iH, iM, iS].forEach(function (i) { i.addEventListener("input", function () { if (!st.run && mode() === "timer") { st.dur = inputDur(); st.rem = st.dur; buttons(); loop(); } }); });
    iT.addEventListener("input", function () { if (!st.run) { st.target = iT.value; persist(); loop(); } });
    qa("[data-cd-preset]").forEach(function (b) {
      b.addEventListener("click", function () {
        if (st.run) { st.run = false; clearInterval(tick); }
        setInputs(+b.getAttribute("data-cd-preset") * 60000); st.dur = inputDur(); st.rem = st.dur; start();
      });
    });
    function setTarget(d) {
      var v = d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate()) + "T" + pad(d.getHours()) + ":" + pad(d.getMinutes());
      if (st.run) { st.run = false; clearInterval(tick); }
      iT.value = v; st.target = v; start();
    }
    var ny = q("[data-cd-quick-newyear]");
    if (ny) ny.addEventListener("click", function () { setTarget(new Date(S.nowDate().getFullYear() + 1, 0, 1, 0, 0)); });
    // Yaklaşan bayram ve özel günler (events JSON)
    if (quick && S.cfg.eventsUrl) {
      fetch(S.cfg.eventsUrl, { credentials: "omit" }).then(function (r) { return r.json(); }).then(function (list) {
        var now = S.nowMs(), seen = {};
        list.forEach(function (e) {
          if (seen[e.k] || ["rb1", "kb1", "oct29", "ramadanStart", "newYear", "may19", "aug30"].indexOf(e.k) < 0 || e.k === "newYear") return;
          var p = e.d.split("-"), dt = new Date(+p[0], +p[1] - 1, +p[2], 0, 0);
          if (dt.getTime() <= now) return;
          seen[e.k] = 1;
          var b = doc.createElement("button");
          b.type = "button"; b.className = "chip"; b.textContent = e.n;
          b.addEventListener("click", function () { setTarget(dt); });
          quick.appendChild(b);
        });
      }).catch(function () {});
    }
    bStart.addEventListener("click", start); bReset.addEventListener("click", reset);

    // Kayıtlı durumu geri yükle
    setMode(st.mode);
    if (st.target) iT.value = st.target;
    setInputs(st.dur);
    if (st.run) {
      if (mode() === "timer") {
        var left = st.wallEnd - Date.now();
        if (left <= 0) { st.run = false; st.rem = 0; persist(); } else { st.rem = left; endMono = S.mono() + left; }
      } else if (targetMs() <= S.nowMs()) { st.run = false; persist(); }
    }
    buttons(); loop(); if (st.run) run();
  }

  /* ---------- Alarm ---------- */
  function alarm() {
    var P = TT.alarm || {};
    var form = q("[data-alarm-form]"), list = q("[data-al-list]"), empty = q("[data-al-empty]"), nextEl = q("[data-al-next]");
    var wake = q("[data-wake]");
    var alarms = (function () { try { var a = JSON.parse(store("sth-alarms") || "[]"); return Array.isArray(a) ? a : []; } catch (e) { return []; } })();
    var snoozeUntil = 0, ringing = null, lock = null;
    function persist() { save("sth-alarms", alarms); }
    function label(a) { return a.label || P.defaultLabel; }
    function daysText(a) {
      if (!a.days.length) return P.once;
      if (a.days.length === 7) return P.every;
      return a.days.slice().sort().map(function (d) { return P.dayNames[d]; }).join(", ");
    }
    function dowOf(d) { return (d.getDay() + 6) % 7; }
    function nextFire(a, from) {
      for (var i = 0; i < 8; i++) {
        var d = new Date(from.getFullYear(), from.getMonth(), from.getDate() + i, +a.time.slice(0, 2), +a.time.slice(3, 5), 0);
        if (d <= from) continue;
        if (!a.days.length || a.days.indexOf(dowOf(d)) > -1) return d;
      }
      return null;
    }
    function render() {
      list.textContent = "";
      alarms.sort(function (a, b) { return a.time < b.time ? -1 : 1; });
      alarms.forEach(function (a) {
        var li = doc.createElement("li"); li.className = "alarm-item";
        var cb = doc.createElement("input"); cb.type = "checkbox"; cb.checked = a.on; cb.id = "al-" + a.id;
        cb.setAttribute("aria-label", P.enable + ": " + a.time + " " + label(a));
        cb.addEventListener("change", function () { a.on = cb.checked; if (a.on) ctx(); persist(); renderNext(); lockSync(); });
        var info = doc.createElement("label"); info.setAttribute("for", cb.id); info.className = "al-info";
        var tm = doc.createElement("strong"); tm.textContent = a.time;
        var tx = doc.createElement("span"); tx.textContent = label(a) + " · " + daysText(a);
        info.appendChild(tm); info.appendChild(tx);
        var del = doc.createElement("button"); del.type = "button"; del.className = "toggle"; del.textContent = P["delete"];
        del.setAttribute("aria-label", P["delete"] + ": " + a.time + " " + label(a));
        del.addEventListener("click", function () { alarms = alarms.filter(function (x) { return x !== a; }); persist(); render(); lockSync(); });
        li.appendChild(cb); li.appendChild(info); li.appendChild(del); list.appendChild(li);
      });
      empty.hidden = alarms.length > 0;
      renderNext();
    }
    function renderNext() {
      var now = S.nowDate(), best = null;
      alarms.forEach(function (a) { if (!a.on) return; var d = nextFire(a, now); if (d && (!best || d < best.d)) best = { d: d, a: a }; });
      if (!best) { nextEl.textContent = P.nextNone; return; }
      var mins = Math.round((best.d - now) / 60000), h = Math.floor(mins / 60), m = mins % 60;
      nextEl.textContent = P.next + ": " + best.a.time + " (" + (h ? h + " " + S.T.hourShort + " " : "") + m + " " + S.T.minuteShort + ")";
    }
    function lockSync() {
      if (!wake || !navigator.wakeLock) { if (wake) wake.parentNode.hidden = true; return; }
      var need = wake.checked && alarms.some(function (a) { return a.on; });
      if (need && !lock) { navigator.wakeLock.request("screen").then(function (l) { lock = l; l.addEventListener("release", function () { lock = null; }); }).catch(function () {}); }
      else if (!need && lock) { lock.release().catch(function () {}); lock = null; }
    }
    if (wake) {
      wake.checked = store("sth-wake") === "1";
      wake.addEventListener("change", function () { store("sth-wake", wake.checked ? "1" : "0"); lockSync(); });
      doc.addEventListener("visibilitychange", function () { if (!doc.hidden) lockSync(); });
    }
    qa("[data-al-preset]").forEach(function (b) {
      b.addEventListener("click", function () {
        var p = b.getAttribute("data-al-preset");
        qa("[data-al-day]").forEach(function (c) { var d = +c.value; c.checked = p === "all" || (p === "weekdays" && d < 5); });
      });
    });
    form.addEventListener("submit", function (ev) {
      ev.preventDefault(); ctx(); used1();
      var time = q("[data-al-time]").value;
      if (!/^\d\d:\d\d$/.test(time)) return;
      alarms.push({ id: Date.now().toString(36) + Math.random().toString(36).slice(2, 5), time: time, label: q("[data-al-label]").value.trim().slice(0, 40), days: qa("[data-al-day]").filter(function (c) { return c.checked; }).map(function (c) { return +c.value; }), on: true, last: "" });
      q("[data-al-label]").value = "";
      persist(); render(); lockSync();
    });
    function fire(a, key) {
      ringing = a; a.last = key; if (!a.days.length) a.on = false; persist(); render();
      ring(label(a) + " · " + a.time, function () { ringing = null; });
    }
    function check() {
      var now = S.nowDate(), key = now.getFullYear() + "-" + now.getMonth() + "-" + now.getDate(), mod = now.getHours() * 60 + now.getMinutes();
      if (snoozeUntil && S.nowMs() >= snoozeUntil && !ringing) { snoozeUntil = 0; ring(P.ringing, null); return; }
      alarms.forEach(function (a) {
        if (!a.on || ringing) return;
        var am = +a.time.slice(0, 2) * 60 + +a.time.slice(3, 5), diff = (mod - am + 1440) % 1440;
        if (diff > 1 || a.last === key + a.time) return;
        if (a.days.length && a.days.indexOf(dowOf(now)) < 0) return;
        fire(a, key + a.time);
      });
    }
    var sn = banner && banner.querySelector("[data-ring-snooze]");
    if (sn) sn.addEventListener("click", function () { snoozeUntil = S.nowMs() + 5 * 60000; stopRing(); });
    setInterval(function () { check(); }, 1000);
    setInterval(renderNext, 15000);
    render(); lockSync();
  }

  /* ---------- Pomodoro ---------- */
  function pomodoro() {
    var P = TT.pomodoro || {};
    var disp = q("[data-pomo-display]"), prog = q("[data-pomo-progress]"), phaseEl = q("[data-pomo-phase]"), roundEl = q("[data-pomo-round]"), countEl = q("[data-pomo-count]");
    var bStart = q("[data-pomo-start]"), bSkip = q("[data-pomo-skip]"), bReset = q("[data-pomo-reset]");
    var iW = q("[data-pomo-w]"), iS = q("[data-pomo-s]"), iL = q("[data-pomo-l]"), iR = q("[data-pomo-r]");
    var st = load("sth-pomo", { w: 25, s: 5, l: 15, r: 4, phase: "work", round: 1, done: 0, run: false, rem: 0, wallEnd: 0 });
    var endMono = 0, tick = 0;
    function clamp(v, a, b, d) { v = Math.round(+v); return isNaN(v) ? d : Math.min(b, Math.max(a, v)); }
    function readSettings() { st.w = clamp(iW.value, 1, 180, 25); st.s = clamp(iS.value, 1, 60, 5); st.l = clamp(iL.value, 1, 120, 15); st.r = clamp(iR.value, 2, 12, 4); }
    function phaseMs(p) { return (p === "work" ? st.w : p === "short" ? st.s : st.l) * 60000; }
    function persist() { save("sth-pomo", st); }
    function remaining() { return st.run ? endMono - S.mono() : st.rem; }
    function paint() {
      var r = Math.max(0, remaining()), total = phaseMs(st.phase);
      disp.textContent = hms(r, false);
      prog.value = total ? (r / total) * 100 : 0;
      phaseEl.textContent = P[st.phase];
      phaseEl.setAttribute("data-phase", st.phase);
      roundEl.textContent = tpl(P.round, { n: st.round, total: st.r });
      countEl.textContent = tpl(P.completed, { n: st.done });
      bStart.textContent = st.run ? P.pause : (st.rem > 0 && st.rem < phaseMs(st.phase) ? P.resume : P.start);
      setTitle(st.run ? hms(r, false) + " " + P[st.phase] : "");
    }
    function begin(phase, autostart) {
      st.phase = phase; st.rem = phaseMs(phase);
      if (autostart) { st.run = true; endMono = S.mono() + st.rem; st.wallEnd = Date.now() + st.rem; } else st.run = false;
      persist(); paint();
    }
    function next(auto) {
      var prev = st.phase;
      if (prev === "work") { st.done++; if (st.round >= st.r) begin("long", auto); else begin("short", auto); }
      else if (prev === "short") { st.round++; begin("work", auto); }
      else { st.round = 1; begin("work", auto); }
    }
    function loop() {
      paint();
      if (st.run && remaining() <= 0) {
        var name = P[st.phase];
        chime(); notify(tpl(P.phaseDone, { phase: name }), baseTitle);
        next(true);
      }
    }
    function toggle() {
      ctx(); used1(); readSettings();
      if (st.run) { st.rem = Math.max(0, endMono - S.mono()); st.run = false; }
      else {
        if (!(st.rem > 0)) st.rem = phaseMs(st.phase);
        endMono = S.mono() + st.rem; st.wallEnd = Date.now() + st.rem; st.run = true;
      }
      persist(); paint();
    }
    function reset() { readSettings(); st.round = 1; st.done = 0; begin("work", false); }
    bStart.addEventListener("click", toggle);
    bSkip.addEventListener("click", function () { ctx(); var was = st.run; next(was); });
    bReset.addEventListener("click", reset);
    [iW, iS, iL, iR].forEach(function (i) { i.addEventListener("change", function () { readSettings(); if (!st.run) { st.rem = phaseMs(st.phase); } persist(); paint(); }); });
    iW.value = st.w; iS.value = st.s; iL.value = st.l; iR.value = st.r;
    if (st.run) {
      var left = st.wallEnd - Date.now();
      if (left > 0) { st.rem = left; endMono = S.mono() + left; } else { st.run = false; st.rem = 0; }
    }
    if (!(st.rem > 0) && !st.run) st.rem = phaseMs(st.phase);
    paint();
    setInterval(loop, 250);
  }

  ({ stopwatch: stopwatch, countdown: countdown, alarm: alarm, pomodoro: pomodoro }[kind] || function () {})();
})();
