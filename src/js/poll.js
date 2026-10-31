/* Toplantı oylaması (31 Ekim 2026, sahibin kararı: sunucusuz, bağlantıyla). Düzenleyici aday saatleri seçer ve oylama
   bağlantısını paylaşır; katılımcı uygun saatleri işaretleyip bir yanıt bağlantısı geri gönderir; düzenleyici bu bağlantıyı
   açınca ya da yapıştırınca yanıt sonuçlara eklenir. Her şey adresin # kısmında taşınır (tarayıcılar bu kısmı sunucuya
   göndermez); düzenleyicinin gördüğü oylamalar ve yanıtlar yalnız bu tarayıcıda, sth-polls anahtarında durur.
   Biçim: #p=<oylama> [&r=<yanıt>] [&a=<yanıt>~<yanıt>…]; oylama = base64url(JSON {v,i,t,o,d,z,s}), s dakika (Unix/60);
   yanıt = base64url(ad) + "." + her saat için 2 (uygun), 1 (olabilir), 0 (uygun değil). */
(function () {
  "use strict";
  var doc = document, S = window.sth;
  var root = doc.querySelector("[data-poll]");
  if (!root || !S) return;
  var P = (S.cfg.t && S.cfg.t.poll) || {};
  var PL = (S.cfg.t && S.cfg.t.plan) || {};
  var q = function (s) { return root.querySelector(s); };
  var MAX_SLOTS = 12, MAX_POLLS = 20, MAX_PEOPLE = 50, KEY = "sth-polls";
  var DURS = [15, 30, 45, 60, 90, 120, 180];
  var tz = (function () { try { return Intl.DateTimeFormat().resolvedOptions().timeZone || ""; } catch (e) { return ""; } })();

  /* ---------- kodlama ---------- */
  function b64e(s) {
    var b = new TextEncoder().encode(s), x = "";
    for (var i = 0; i < b.length; i++) x += String.fromCharCode(b[i]);
    return btoa(x).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }
  function b64d(s) {
    if (!/^[A-Za-z0-9_-]*$/.test(s)) throw new Error("b64");
    s = s.replace(/-/g, "+").replace(/_/g, "/");
    while (s.length % 4) s += "=";
    var x = atob(s), b = new Uint8Array(x.length);
    for (var i = 0; i < x.length; i++) b[i] = x.charCodeAt(i);
    return new TextDecoder("utf-8", { fatal: true }).decode(b);
  }
  function clean(v, max) { return String(v == null ? "" : v).replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim().slice(0, max); }
  function validPoll(o) {
    if (!o || typeof o !== "object" || o.v !== 1) return null;
    if (typeof o.i !== "string" || !/^[a-z0-9]{6,12}$/.test(o.i)) return null;
    if (!Array.isArray(o.s) || !o.s.length || o.s.length > MAX_SLOTS) return null;
    var s = [];
    for (var k = 0; k < o.s.length; k++) {
      var m = o.s[k];
      if (typeof m !== "number" || m !== Math.floor(m) || m < 27000000 || m > 40000000) return null; // ~2021–2046
      if (s.indexOf(m) < 0) s.push(m);
    }
    var d = +o.d;
    return { v: 1, i: o.i, t: clean(o.t, 80), o: clean(o.o, 40), d: d >= 5 && d <= 600 ? Math.round(d) : 60, z: clean(o.z, 48), s: s };
  }
  function encPoll(p) { return b64e(JSON.stringify({ v: 1, i: p.i, t: p.t, o: p.o, d: p.d, z: p.z, s: p.s })); }
  function decPoll(s) { try { return validPoll(JSON.parse(b64d(s))); } catch (e) { return null; } }
  function encResp(name, digits) { return b64e(name) + "." + digits; }
  function decResp(s, n) {
    var m = /^([A-Za-z0-9_-]{1,80})\.([0-2]{1,12})$/.exec(s || "");
    if (!m || m[2].length !== n) return null;
    var name; try { name = clean(b64d(m[1]), 40); } catch (e) { return null; }
    return name ? { n: name, a: m[2] } : null;
  }
  function parseHash(h) {
    var o = {};
    String(h || "").replace(/^#/, "").split("&").forEach(function (kv) {
      var i = kv.indexOf("="); if (i > 0) o[kv.slice(0, i)] = kv.slice(i + 1);
    });
    return o;
  }
  function pageUrl() { return location.origin + location.pathname; }
  function pollUrl(p) { return pageUrl() + "#p=" + encPoll(p); }

  /* ---------- kayıt (yalnız bu tarayıcı) ---------- */
  function loadAll() { try { var o = JSON.parse(S.store(KEY) || "{}"); return o && typeof o === "object" && !Array.isArray(o) ? o : {}; } catch (e) { return {}; } }
  function saveAll(all) {
    var ids = Object.keys(all);
    if (ids.length > MAX_POLLS) ids.sort(function (a, b) { return (all[a].at || 0) - (all[b].at || 0); }).slice(0, ids.length - MAX_POLLS).forEach(function (k) { delete all[k]; });
    try { S.store(KEY, JSON.stringify(all)); } catch (e) {}
  }
  function record(p, own) {
    var all = loadAll(), r = all[p.i];
    if (!r) r = all[p.i] = { p: p, r: {}, own: !!own };
    r.p = p; if (own) r.own = true; r.at = Date.now();
    saveAll(all); return r;
  }
  function addAnswer(p, ans) {
    var all = loadAll(), r = all[p.i] || (all[p.i] = { p: p, r: {}, own: false });
    var names = Object.keys(r.r);
    if (names.length >= MAX_PEOPLE && !(ans.n in r.r)) return false;
    r.r[ans.n] = ans.a; r.at = Date.now();
    saveAll(all); return true;
  }

  /* ---------- biçim ---------- */
  var h12 = !!(S.prefs && S.prefs.h12);
  function slotFmt(zone) {
    var o = { weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit", hourCycle: h12 ? "h12" : "h23" };
    if (zone) o.timeZone = zone;
    try { return new Intl.DateTimeFormat(S.locale, o); } catch (e) { delete o.timeZone; return new Intl.DateTimeFormat(S.locale, o); }
  }
  var fLocal = slotFmt("");
  function slotText(m) { return fLocal.format(new Date(m * 60000)); }
  function orgText(p, m) {
    if (!p.z || p.z === tz) return "";
    try { return S.tpl(P.orgTime, { time: slotFmt(p.z).format(new Date(m * 60000)), tz: p.z }); } catch (e) { return ""; }
  }
  function durText(d) { return S.durLabel ? S.durLabel(d) : d + " " + S.T.minuteShort; }
  function el(tag, cls, text) { var e = doc.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  function status(node, msg) { if (node) { node.textContent = msg || ""; } }
  function copy(text, node) {
    var done = function () { status(node, P.copied); setTimeout(function () { status(node, ""); }, 2500); };
    if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(text).then(done, function () { status(node, text); });
    else status(node, text);
  }
  function linkBox(box, url, hint) {
    box.textContent = "";
    if (hint) box.appendChild(el("p", "meta", hint));
    var row = el("div", "share-row");
    var inp = el("input", "poll-url"); inp.type = "url"; inp.readOnly = true; inp.value = url; inp.setAttribute("aria-label", P.linkLabel);
    inp.addEventListener("focus", function () { inp.select(); });
    var st = el("span", "meta"); st.setAttribute("role", "status"); st.setAttribute("aria-live", "polite");
    var c = el("button", "toggle", P.copy); c.type = "button"; c.addEventListener("click", function () { copy(url, st); });
    row.appendChild(inp); row.appendChild(c);
    if (navigator.share) {
      var sh = el("button", "toggle", P.share); sh.type = "button";
      sh.addEventListener("click", function () { navigator.share({ url: url }).catch(function () {}); });
      row.appendChild(sh);
    }
    row.appendChild(st);
    box.appendChild(row); box.hidden = false;
  }

  /* ---------- oluşturma ---------- */
  var createBox = q("[data-poll-create]"), linkOut = q("[data-poll-link]"), view = q("[data-poll-view]"), mine = q("[data-poll-mine]");
  var slots = [];
  var iTitle = q("[data-po-title]"), iOrg = q("[data-po-org]"), iDur = q("[data-po-dur]"), iDate = q("[data-po-date]"), iTime = q("[data-po-time]");
  var slotList = q("[data-po-slots]"), msg = q("[data-po-msg]");
  if (q("[data-po-tz]") && tz) q("[data-po-tz]").textContent = S.tpl(P.tzNote, { tz: tz });
  (function initDate() {
    var d = new Date(S.nowMs() + 86400000), pad = function (n) { return String(n).padStart(2, "0"); };
    if (iDate && !iDate.value) iDate.value = d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate());
    if (iTime && !iTime.value) iTime.value = "10:00";
  })();
  function drawSlots() {
    slotList.textContent = "";
    slots.sort(function (a, b) { return a - b; });
    slots.forEach(function (m) {
      var li = el("li", "chip-item"), t = el("span", null, slotText(m));
      var x = el("button", "toggle poll-x", "✕"); x.type = "button"; x.setAttribute("aria-label", P.removeSlot + ": " + slotText(m));
      x.addEventListener("click", function () { slots = slots.filter(function (v) { return v !== m; }); drawSlots(); });
      li.appendChild(t); li.appendChild(x); slotList.appendChild(li);
    });
  }
  var addBtn = q("[data-po-add]");
  if (addBtn) addBtn.addEventListener("click", function () {
    var dv = iDate.value, tv = iTime.value;
    if (!/^\d{4}-\d\d-\d\d$/.test(dv) || !/^\d\d:\d\d/.test(tv)) return;
    var d = new Date(+dv.slice(0, 4), +dv.slice(5, 7) - 1, +dv.slice(8, 10), +tv.slice(0, 2), +tv.slice(3, 5));
    var m = Math.round(d.getTime() / 60000);
    if (slots.length >= MAX_SLOTS) { status(msg, S.tpl(P.maxSlots, { n: MAX_SLOTS })); return; }
    if (slots.indexOf(m) < 0) slots.push(m);
    status(msg, ""); drawSlots();
  });
  var createBtn = q("[data-po-create]");
  if (createBtn) createBtn.addEventListener("click", function () {
    if (!slots.length) { status(msg, P.needSlot); return; }
    var id = ""; while (id.length < 8) id += Math.random().toString(36).slice(2);
    var p = validPoll({ v: 1, i: id.slice(0, 8), t: clean(iTitle.value, 80) || PL.meeting || "", o: clean(iOrg.value, 40), d: +iDur.value, z: tz, s: slots.slice() });
    if (!p) return;
    record(p, true);
    S.track("arac", { arac: "oylama" });
    history.replaceState(null, "", "#p=" + encPoll(p));
    route();
  });

  /* ---------- oylama, yanıt ve sonuçlar ---------- */
  function showPoll(p, extra) {
    createBox.hidden = true; linkOut.hidden = true; view.hidden = false;
    var rec = loadAll()[p.i];
    var own = !!(rec && rec.own);
    view.textContent = "";
    view.appendChild(el("h3", "poll-title", p.t || PL.meeting || ""));
    var meta = [durText(p.d)];
    if (p.o) meta.unshift(S.tpl(P.byLine, { name: p.o }));
    view.appendChild(el("p", "meta", meta.join(" · ")));
    if (tz) view.appendChild(el("p", "meta", S.tpl(P.tzNote, { tz: tz })));
    if (extra) view.appendChild(el("p", "poll-note", extra));

    // Düzenleyici: paylaşılacak oylama bağlantısı
    if (own) {
      var lb = el("div", "poll-linkbox"); view.appendChild(lb);
      lb.appendChild(el("h4", null, P.linkHeading));
      var inner = el("div"); lb.appendChild(inner); linkBox(inner, pollUrl(p), P.linkHint);
    }

    // Oy verme
    var form = el("form", "poll-vote"); form.noValidate = true;
    form.appendChild(el("h4", null, P.voteHeading));
    var nf = el("div", "field"), nl = el("label", null, P.yourName), ni = el("input");
    ni.id = "pv-name"; ni.type = "text"; nl.setAttribute("for", "pv-name"); ni.maxLength = 40; ni.autocomplete = "name";
    try { ni.value = S.store("sth-poll-name") || (own ? p.o : "") || ""; } catch (e) {}
    nf.appendChild(nl); nf.appendChild(ni); form.appendChild(nf);
    var choices = p.s.map(function () { return 2; });
    p.s.forEach(function (m, idx) {
      var fs = el("fieldset", "poll-slot"), lg = el("legend", null, slotText(m));
      var ot = orgText(p, m); if (ot) lg.appendChild(el("span", "meta poll-org", ot));
      fs.appendChild(lg);
      [[2, P.yes, "is-yes"], [1, P.maybe, "is-maybe"], [0, P.no, "is-no"]].forEach(function (c) {
        var lab = el("label", "poll-opt " + c[2]), r = el("input");
        r.type = "radio"; r.name = "pv-" + idx; r.value = String(c[0]); r.checked = c[0] === 2;
        r.addEventListener("change", function () { if (r.checked) choices[idx] = c[0]; });
        lab.appendChild(r); lab.appendChild(doc.createTextNode(" " + c[1])); fs.appendChild(lab);
      });
      form.appendChild(fs);
    });
    var act = el("div", "tool-actions"), rb = el("button", "btn", P.respond); rb.type = "submit"; act.appendChild(rb); form.appendChild(act);
    var vmsg = el("p", "meta"); vmsg.setAttribute("role", "status"); form.appendChild(vmsg);
    var rbox = el("div", "poll-linkbox"); rbox.hidden = true; form.appendChild(rbox);
    form.addEventListener("submit", function (ev) {
      ev.preventDefault();
      var name = clean(ni.value, 40);
      if (!name) { status(vmsg, P.needName); ni.focus(); return; }
      try { S.store("sth-poll-name", name); } catch (e) {}
      var ans = { n: name, a: choices.join("") };
      addAnswer(p, ans); drawResults();
      status(vmsg, "");
      linkBox(rbox, pollUrl(p) + "&r=" + encResp(ans.n, ans.a), own ? P.ownSaved : P.responseHint);
      S.track("arac", { arac: "oylama-yanit" });
    });
    view.appendChild(form);

    // Sonuçlar
    var res = el("section", "poll-results"); res.setAttribute("aria-labelledby", "pv-res-h");
    var rh = el("h4", null, P.resultsHeading); rh.id = "pv-res-h"; res.appendChild(rh);
    var resBody = el("div"); res.appendChild(resBody);
    // Gelen yanıt bağlantısını yapıştırma
    var pf = el("form", "poll-paste"); pf.noValidate = true;
    var pfl = el("label", null, P.pasteLabel), pfi = el("input"); pfi.id = "pv-paste"; pfl.setAttribute("for", "pv-paste"); pfi.type = "url"; pfi.autocomplete = "off";
    var pfb = el("button", "toggle", P.pasteBtn); pfb.type = "submit";
    var pfm = el("p", "meta"); pfm.setAttribute("role", "status");
    var prow = el("div", "share-row"); prow.appendChild(pfi); prow.appendChild(pfb);
    pf.appendChild(pfl); pf.appendChild(prow); pf.appendChild(pfm);
    pf.addEventListener("submit", function (ev) {
      ev.preventDefault();
      var v = pfi.value, hi = v.indexOf("#"), h = parseHash(hi >= 0 ? v.slice(hi) : v);
      var pp = h.p ? decPoll(h.p) : null, added = [];
      if (!pp || pp.i !== p.i) { status(pfm, P.pasteBad); return; }
      (h.r ? [h.r] : []).concat(h.a ? h.a.split("~") : []).forEach(function (s) {
        var a = decResp(s, p.s.length); if (a && addAnswer(p, a)) added.push(a.n);
      });
      status(pfm, added.length ? S.tpl(P.added, { names: added.join(", ") }) : P.pasteBad);
      if (added.length) { pfi.value = ""; drawResults(); }
    });
    res.appendChild(pf);
    view.appendChild(res);

    function drawResults() {
      var r = (loadAll()[p.i] || {}).r || {};
      var names = Object.keys(r);
      resBody.textContent = "";
      if (!names.length) { resBody.appendChild(el("p", "meta", P.noAnswers)); return; }
      var yes = p.s.map(function () { return 0; }), maybe = p.s.map(function () { return 0; });
      names.forEach(function (n) { for (var k = 0; k < p.s.length; k++) { var c = +r[n].charAt(k); if (c === 2) yes[k]++; else if (c === 1) maybe[k]++; } });
      var best = 0;
      for (var k = 1; k < p.s.length; k++) if (yes[k] > yes[best] || (yes[k] === yes[best] && maybe[k] > maybe[best])) best = k;
      var wrap = el("div", "table-wrap"), tb = el("table", "poll-table");
      var cap = el("caption", "sr-only", P.resultsHeading); tb.appendChild(cap);
      var th = el("thead"), tr = el("tr"); tr.appendChild(el("th", null, P.participant));
      p.s.forEach(function (m, k) { var c = el("th", k === best ? "is-best" : null, slotText(m)); c.scope = "col"; tr.appendChild(c); });
      th.appendChild(tr); tb.appendChild(th);
      var tbd = el("tbody");
      names.sort(function (a, b) { return a.localeCompare(b, S.lang); }).forEach(function (n) {
        var row = el("tr"), nh = el("th", null, n); nh.scope = "row"; row.appendChild(nh);
        for (var k = 0; k < p.s.length; k++) {
          var c = +r[n].charAt(k), td = el("td", ["is-no", "is-maybe", "is-yes"][c] + (k === best ? " is-best" : ""));
          td.appendChild(el("span", null, ["✗", "?", "✓"][c]));
          td.appendChild(el("span", "sr-only", " " + [P.no, P.maybe, P.yes][c]));
          row.appendChild(td);
        }
        {
          var dc = el("td", "poll-del"), db = el("button", "toggle poll-x", "✕"); db.type = "button"; db.setAttribute("aria-label", P.removeAnswer + ": " + n);
          db.addEventListener("click", function () { var all = loadAll(); if (all[p.i]) { delete all[p.i].r[n]; saveAll(all); drawResults(); } });
          dc.appendChild(db); row.appendChild(dc);
        }
        tbd.appendChild(row);
      });
      tb.appendChild(tbd);
      var tf = el("tfoot"), fr = el("tr"); fr.appendChild(el("th", null, P.total));
      for (var j = 0; j < p.s.length; j++) fr.appendChild(el("td", j === best ? "is-best" : null, "✓ " + yes[j] + " · ? " + maybe[j]));
      tf.appendChild(fr); tb.appendChild(tf);
      wrap.appendChild(tb); resBody.appendChild(wrap);
      resBody.appendChild(el("p", "poll-best", S.tpl(P.best, { slot: slotText(p.s[best]), n: yes[best], total: names.length })));
      // Takvime ekle (en uygun saat) ve sonuç bağlantısı
      var ms = p.s[best] * 60000, end = ms + p.d * 60000, title = p.t || PL.meeting || "";
      var icsD = function (x) { var d = new Date(x), pd = function (v) { return String(v).padStart(2, "0"); }; return d.getUTCFullYear() + pd(d.getUTCMonth() + 1) + pd(d.getUTCDate()) + "T" + pd(d.getUTCHours()) + pd(d.getUTCMinutes()) + "00Z"; };
      var esc = function (v) { return String(v).replace(/\\/g, "\\\\").replace(/[,;]/g, "\\$&").replace(/\r?\n/g, "\\n"); };
      var file = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Saat Tarih//poll//EN", "BEGIN:VEVENT", "UID:" + p.i + "-" + p.s[best] + "@time.bumba.tr",
        "DTSTAMP:" + icsD(Date.now()), "DTSTART:" + icsD(ms), "DTEND:" + icsD(end), "SUMMARY:" + esc(title), "DESCRIPTION:" + esc(pageUrl() + "#p=" + encPoll(p)), "END:VEVENT", "END:VCALENDAR"].join("\r\n");
      var row2 = el("div", "share-row");
      var a1 = el("a", "toggle", PL.addIcs); a1.href = "data:text/calendar;charset=utf-8," + encodeURIComponent(file); a1.download = "saat-tarih.ics";
      var a2 = el("a", "toggle", PL.addGoogle); a2.target = "_blank"; a2.rel = "noopener";
      a2.href = "https://calendar.google.com/calendar/render?action=TEMPLATE&text=" + encodeURIComponent(title) + "&dates=" + icsD(ms) + "/" + icsD(end) + "&details=" + encodeURIComponent(pageUrl() + "#p=" + encPoll(p));
      var all = names.map(function (n) { return encResp(n, r[n]); }).join("~");
      var st = el("span", "meta"); st.setAttribute("role", "status"); st.setAttribute("aria-live", "polite");
      var a3 = el("button", "toggle", P.resultsLink); a3.type = "button";
      a3.addEventListener("click", function () { copy(pollUrl(p) + "&a=" + all, st); });
      row2.appendChild(a1); row2.appendChild(a2); row2.appendChild(a3); row2.appendChild(st);
      resBody.appendChild(row2);
    }
    drawResults();
    var nb = el("p", "poll-new"), na = el("a", null, P.newPoll); na.href = pageUrl();
    na.addEventListener("click", function (ev) { ev.preventDefault(); history.replaceState(null, "", pageUrl()); route(); });
    nb.appendChild(na); view.appendChild(nb);
  }

  function drawMine() {
    var all = loadAll(), ids = Object.keys(all).sort(function (a, b) { return (all[b].at || 0) - (all[a].at || 0); });
    mine.textContent = "";
    if (!ids.length) { mine.hidden = true; return; }
    mine.hidden = false;
    mine.appendChild(el("h3", null, P.myPolls));
    var ul = el("ul", "poll-mine");
    ids.forEach(function (id) {
      var r = all[id], p = validPoll(r && r.p); if (!p) return;
      var li = el("li"), a = el("a", null, p.t || PL.meeting || ""); a.href = pollUrl(p);
      a.addEventListener("click", function (ev) { ev.preventDefault(); history.replaceState(null, "", "#p=" + encPoll(p)); route(); });
      li.appendChild(a);
      li.appendChild(el("span", "meta", " · " + S.tpl(P.answers, { n: Object.keys(r.r || {}).length }) + " · " + slotText(p.s[0])));
      var x = el("button", "toggle poll-x", "✕"); x.type = "button"; x.setAttribute("aria-label", P.deletePoll + ": " + (p.t || ""));
      x.addEventListener("click", function () { var al = loadAll(); delete al[id]; saveAll(al); drawMine(); });
      li.appendChild(x); ul.appendChild(li);
    });
    mine.appendChild(ul);
  }

  function route() {
    var h = parseHash(location.hash);
    if (h.p) {
      var p = decPoll(h.p);
      if (!p) { createBox.hidden = false; view.hidden = true; status(msg, P.invalid); drawMine(); return; }
      var rec = loadAll()[p.i];
      record(p, rec && rec.own);
      var added = [];
      (h.r ? [h.r] : []).concat(h.a ? h.a.split("~") : []).forEach(function (s) {
        var a = decResp(s, p.s.length); if (a && addAnswer(p, a)) added.push(a.n);
      });
      showPoll(p, added.length ? S.tpl(P.added, { names: added.join(", ") }) : "");
      mine.hidden = true;
      // Yanıt/sonuç bağlantısı bir kez işlendi: adreste yalnız oylama kalsın (yenilemede yeniden eklenmesin diye değil, temiz paylaşım için)
      if (h.r || h.a) history.replaceState(null, "", "#p=" + h.p);
    } else {
      createBox.hidden = false; view.hidden = true; linkOut.hidden = true;
      drawSlots(); drawMine();
    }
  }
  window.addEventListener("hashchange", route);
  route();
})();
