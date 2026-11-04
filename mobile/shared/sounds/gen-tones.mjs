// Alarm ses kitaplığı (2. analiz #3; sahibin ekran görüntüleri): uygulamanın kendi, telifsiz (sentez) alarm sesleri.
// Üç kategori: yumuşak (rüzgâr çanı, marimba, arp, kuş cıvıltısı, kalimba), klasik (eski çalar saat, dijital bip,
// vızıltı), güçlü (siren, ritim). Her ses 29 sn (iOS'ta alarm/bildirim sesi en çok 30 sn), 22.050 Hz mono.
// Kullanım (ffmpeg gerekir): node mobile/shared/sounds/gen-tones.mjs
//   → mobile/ios/App/Resources/tone-<ad>.caf (IMA4) ve mobile/android/app/src/main/res/raw/tone_<ad>.ogg (Vorbis)
// Liste uygulamalarda: iOS AlarmTones.swift, Android AlarmTones.kt (aynı adlar ve sıra).
import { writeFileSync, mkdtempSync, rmSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join, dirname } from "node:path";
import { tmpdir } from "node:os";

const RATE = 22050, SECONDS = 29, N = RATE * SECONDS;
const ROOT = join(dirname(new URL(import.meta.url).pathname), "../../");
const TAU = 2 * Math.PI;

// Tekrarlanabilir sözde rastgele (xorshift)
function rng(seed) {
  let s = seed >>> 0 || 1;
  return () => { s ^= s << 13; s >>>= 0; s ^= s >>> 17; s ^= s << 5; s >>>= 0; return s / 4294967296; };
}
const note = (m) => 440 * 2 ** ((m - 69) / 12);
function add(buf, start, len, fn) {
  const a = Math.max(0, Math.floor(start * RATE)), b = Math.min(N, Math.floor((start + len) * RATE));
  for (let i = a; i < b; i++) buf[i] += fn((i - a) / RATE);
}
const attack = (t, a = 0.004) => Math.min(1, t / a);

const TONES = {
  // Rüzgâr çanı: pentatonik yüksek notalar, rastgele aralıklarla, çan benzeri kısmi tonlar
  chime(buf) {
    const r = rng(7), scale = [81, 83, 86, 88, 90, 93, 95, 98];
    for (let t = 0; t < SECONDS - 3; t += 0.18 + r() * 0.5) {
      const f = note(scale[Math.floor(r() * scale.length)]), amp = 0.25 + r() * 0.25;
      add(buf, t, 3.5, (x) => attack(x) * amp * Math.exp(-x / 1.1) *
        (Math.sin(TAU * f * x) + 0.4 * Math.sin(TAU * f * 2.76 * x) * Math.exp(-x / 0.4) + 0.2 * Math.sin(TAU * f * 5.4 * x) * Math.exp(-x / 0.15)));
    }
  },
  // Marimba: yuvarlanan altı notalık motif
  marimba(buf) {
    const motif = [72, 76, 79, 81, 79, 76], step = 0.22, bar = step * motif.length + 0.7;
    for (let t = 0; t < SECONDS - 2; t += bar) motif.forEach((m, k) => {
      const f = note(m);
      add(buf, t + k * step, 1.4, (x) => attack(x) * Math.exp(-x / 0.32) * (Math.sin(TAU * f * x) + 0.35 * Math.sin(TAU * f * 4 * x) * Math.exp(-x / 0.05)));
    });
  },
  // Arp: iki oktav yukarı süzülen arpej (Karplus-Strong telli çalgı)
  harp(buf) {
    const notes = [60, 64, 67, 72, 76, 79, 84, 88];
    for (let t = 0; t < SECONDS - 4; t += 3.2) notes.forEach((m, k) => {
      const f = note(m), period = Math.round(RATE / f), r = rng(m * 31 + Math.floor(t));
      const line = Float64Array.from({ length: period }, () => r() * 2 - 1);
      const len = Math.floor(2.6 * RATE), start = Math.floor((t + k * 0.09) * RATE);
      for (let i = 0, p = 0; i < len && start + i < N; i++, p = (p + 1) % period) {
        const next = (p + 1) % period;
        const v = line[p];
        line[p] = 0.996 * 0.5 * (line[p] + line[next]);
        buf[start + i] += v * 0.35;
      }
    });
  },
  // Kuş cıvıltısı: hızlı frekans süpürmeli kısa ötüşler, kümeler hâlinde
  birds(buf) {
    const r = rng(42);
    for (let t = 0.2; t < SECONDS - 2; t += 0.8 + r() * 1.6) {
      const count = 2 + Math.floor(r() * 5), base = 2200 + r() * 1800, span = 600 + r() * 1800, dur = 0.06 + r() * 0.08;
      for (let k = 0; k < count; k++) {
        let ph = 0;
        add(buf, t + k * (dur + 0.03), dur, (x) => {
          const u = x / dur, f = base + span * Math.sin(Math.PI * u);
          ph += TAU * f / RATE;
          return Math.sin(Math.PI * u) ** 2 * 0.45 * Math.sin(ph) * (0.7 + 0.3 * Math.sin(TAU * 60 * x));
        });
      }
    }
  },
  // Kalimba: inharmonik kısmi tonlu, yumuşak ezgi
  kalimba(buf) {
    const melody = [67, 71, 74, 79, 76, 74, 71, 74, 72, 76, 79, 83];
    for (let t = 0, k = 0; t < SECONDS - 2; t += 0.38, k++) {
      if (k % 12 === 11) t += 0.8;
      const f = note(melody[k % melody.length]);
      add(buf, t, 1.8, (x) => attack(x, 0.002) * Math.exp(-x / 0.5) * (Math.sin(TAU * f * x) + 0.25 * Math.sin(TAU * f * 5.43 * x) * Math.exp(-x / 0.08)));
    }
  },
  // Eski çalar saat: iki zile hızla vuran tokmak (1,5 sn çalar, 0,6 sn susar)
  bell(buf) {
    for (let t = 0; t < SECONDS - 1.6; t += 2.1) for (let h = 0; h < 30; h++) {
      const f = h % 2 ? 1180 : 1320;
      add(buf, t + h * 0.05, 0.5, (x) => attack(x, 0.001) * 0.35 * Math.exp(-x / 0.12) *
        (Math.sin(TAU * f * x) + 0.5 * Math.sin(TAU * f * 2.4 * x) + 0.3 * Math.sin(TAU * f * 3.9 * x)));
    }
  },
  // Dijital bip: dört kısa bip, kısa ara
  digital(buf) {
    for (let t = 0; t < SECONDS - 1; t += 1.2) for (let k = 0; k < 4; k++)
      add(buf, t + k * 0.13, 0.08, (x) => attack(x, 0.002) * 0.4 * Math.sign(Math.sin(TAU * 2048 * x)) * Math.min(1, (0.08 - x) / 0.004));
  },
  // Vızıltı: testere dişi dalga darbeleri
  buzzer(buf) {
    for (let t = 0; t < SECONDS - 0.6; t += 0.9)
      add(buf, t, 0.5, (x) => attack(x, 0.005) * Math.min(1, (0.5 - x) / 0.01) * 0.35 *
        (((x * 220) % 1) * 2 - 1 + 0.5 * (((x * 441) % 1) * 2 - 1)));
  },
  // Siren: 600–1400 Hz arası inip çıkan ton
  siren(buf) {
    let ph = 0;
    for (let i = 0; i < N; i++) {
      const t = i / RATE, f = 1000 + 400 * Math.sin(TAU * t / 2.4);
      ph += TAU * f / RATE;
      buf[i] += 0.4 * (Math.sin(ph) + 0.2 * Math.sin(2 * ph));
    }
  },
  // Ritim: 120 vuruş/dk davul, zil ve bas arpeji
  pulse(buf) {
    const r = rng(9), beat = 0.5, bass = [45, 45, 52, 50];
    for (let t = 0, k = 0; t < SECONDS - 1; t += beat, k++) {
      add(buf, t, 0.35, (x) => { const f = 50 + 120 * Math.exp(-x / 0.04); return 0.7 * Math.exp(-x / 0.12) * Math.sin(TAU * f * x); });
      add(buf, t + beat / 2, 0.06, (x) => 0.18 * Math.exp(-x / 0.02) * (r() * 2 - 1));
      const f = note(bass[Math.floor(k / 2) % bass.length]);
      add(buf, t, beat * 0.9, (x) => 0.25 * Math.exp(-x / 0.25) * (((x * f) % 1) < 0.5 ? 1 : -1));
      if (k % 4 === 2) add(buf, t, 0.12, (x) => 0.25 * Math.exp(-x / 0.06) * (r() * 2 - 1));
    }
  },
};

function wav(samples) {
  const pcm = Int16Array.from(samples, (v) => Math.round(Math.max(-1, Math.min(1, v)) * 32767));
  const h = Buffer.alloc(44);
  h.write("RIFF", 0); h.writeUInt32LE(36 + pcm.length * 2, 4); h.write("WAVE", 8);
  h.write("fmt ", 12); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
  h.writeUInt32LE(RATE, 24); h.writeUInt32LE(RATE * 2, 28); h.writeUInt16LE(2, 32); h.writeUInt16LE(16, 34);
  h.write("data", 36); h.writeUInt32LE(pcm.length * 2, 40);
  return Buffer.concat([h, Buffer.from(pcm.buffer)]);
}

const tmp = mkdtempSync(join(tmpdir(), "tones-"));
try {
  for (const [id, fn] of Object.entries(TONES)) {
    const buf = new Float64Array(N);
    fn(buf);
    // Baş ve sonda yumuşatma, tepe 0,9'a ölçekleme
    let peak = 0;
    for (let i = 0; i < N; i++) {
      const fade = Math.min(1, i / (RATE * 0.02), (N - i) / (RATE * 1.2));
      buf[i] *= fade;
      peak = Math.max(peak, Math.abs(buf[i]));
    }
    for (let i = 0; i < N; i++) buf[i] *= 0.9 / (peak || 1);
    const w = join(tmp, `${id}.wav`);
    writeFileSync(w, wav(buf));
    execFileSync("ffmpeg", ["-v", "error", "-y", "-i", w, "-c:a", "adpcm_ima_qt", "-f", "caf", join(ROOT, `ios/App/Resources/tone-${id}.caf`)]);
    execFileSync("ffmpeg", ["-v", "error", "-y", "-i", w, "-c:a", "libvorbis", "-q:a", "2", join(ROOT, `android/app/src/main/res/raw/tone_${id}.ogg`)]);
    console.log("ses:", id);
  }
} finally {
  rmSync(tmp, { recursive: true, force: true });
}
