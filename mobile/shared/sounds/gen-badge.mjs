// Rozet kazanınca çalan kısa başarım sesi (sahibin isteği, 15 Kasım 2026): yükselen üç nota + parıltı, 1,1 sn,
// telifsiz sentez. Kullanım (ffmpeg gerekir): node mobile/shared/sounds/gen-badge.mjs
//   → mobile/ios/App/Resources/badge-win.caf (IMA4) ve mobile/android/app/src/main/res/raw/badge_win.ogg (Vorbis)
import { writeFileSync, mkdtempSync, rmSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join, dirname } from "node:path";
import { tmpdir } from "node:os";

const RATE = 22050, SECONDS = 1.1, N = Math.round(RATE * SECONDS), TAU = 2 * Math.PI;
const ROOT = join(dirname(new URL(import.meta.url).pathname), "../../");
const note = (m) => 440 * 2 ** ((m - 69) / 12);
const buf = new Float64Array(N);
function add(start, len, fn) {
  const a = Math.floor(start * RATE), b = Math.min(N, Math.floor((start + len) * RATE));
  for (let i = a; i < b; i++) { const x = (i - a) / RATE; buf[i] += Math.min(1, x / 0.004) * fn(x); }
}
// Do majör üç sesli yükseliş (C6 E6 G6) ve sonda C7; marimba benzeri kısmi tonlar
[[0, 84], [0.09, 88], [0.18, 91], [0.3, 96]].forEach(([t, m], k) => {
  const f = note(m), amp = k === 3 ? 0.45 : 0.32, dec = k === 3 ? 0.45 : 0.2;
  add(t, SECONDS - t, (x) => amp * Math.exp(-x / dec) * (Math.sin(TAU * f * x) + 0.3 * Math.sin(TAU * f * 3 * x) * Math.exp(-x / 0.08)));
});
// Parıltı: çok yüksek, kısa çan sesleri
[[0.34, 103], [0.42, 100], [0.5, 105]].forEach(([t, m]) => {
  const f = note(m);
  add(t, 0.5, (x) => 0.1 * Math.exp(-x / 0.12) * Math.sin(TAU * f * x));
});
let peak = 0;
for (let i = 0; i < N; i++) { buf[i] *= Math.min(1, (N - i) / (RATE * 0.15)); peak = Math.max(peak, Math.abs(buf[i])); }
const pcm = Int16Array.from(buf, (v) => Math.round((v * 0.85) / (peak || 1) * 32767));
const h = Buffer.alloc(44);
h.write("RIFF", 0); h.writeUInt32LE(36 + pcm.length * 2, 4); h.write("WAVE", 8);
h.write("fmt ", 12); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
h.writeUInt32LE(RATE, 24); h.writeUInt32LE(RATE * 2, 28); h.writeUInt16LE(2, 32); h.writeUInt16LE(16, 34);
h.write("data", 36); h.writeUInt32LE(pcm.length * 2, 40);
const tmp = mkdtempSync(join(tmpdir(), "badge-"));
try {
  const w = join(tmp, "badge.wav");
  writeFileSync(w, Buffer.concat([h, Buffer.from(pcm.buffer)]));
  execFileSync("ffmpeg", ["-v", "error", "-y", "-i", w, "-c:a", "adpcm_ima_qt", "-f", "caf", join(ROOT, "ios/App/Resources/badge-win.caf")]);
  execFileSync("ffmpeg", ["-v", "error", "-y", "-i", w, "-c:a", "libvorbis", "-q:a", "3", join(ROOT, "android/app/src/main/res/raw/badge_win.ogg")]);
  console.log("ses: badge-win");
} finally {
  rmSync(tmp, { recursive: true, force: true });
}
