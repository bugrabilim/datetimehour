// Sakin ön alarm sesi (2. analiz #42; yalnız iPhone, bildirim sesi en çok 30 sn): yumuşak yağmur ve arada bir kısık
// rüzgâr çanı; ilk 6 sn'de sessizden yükselir, son 4 sn'de söner, en yüksek düzey ~−10 dBFS. Telifsiz (sentez).
// Android'de dosya yok: ön alarm uygulamanın kendi yağmur sesini (NoiseGen) kısık çalar.
// Kullanım: node mobile/shared/sounds/gen-prealarm.mjs out.wav
//           ffmpeg -i out.wav -c:a adpcm_ima_qt -f caf mobile/ios/App/Resources/prealarm.caf
import { writeFileSync } from "node:fs";

const RATE = 22050, SECONDS = 28;
const n = RATE * SECONDS;
const pcm = new Int16Array(n);
let seed = 0x5a17, lp = 0, hp = 0, prev = 0;
const chimes = [[4.0, 1046.5], [9.5, 1318.5], [15.0, 1174.7], [21.0, 1568.0]]; // C6 E6 D6 G6
for (let i = 0; i < n; i++) {
  const t = i / RATE;
  seed = (seed * 1664525 + 1013904223) >>> 0;
  const w = seed / 4294967296 * 2 - 1;
  // Yağmur: beyaz gürültü, alçak ve yüksek geçiren basit süzgeçlerle yumuşatılır; hafif dalgalanma
  lp += 0.35 * (w - lp);
  hp = 0.97 * (hp + lp - prev); prev = lp;
  const rain = hp * (0.8 + 0.2 * Math.sin(2 * Math.PI * 0.13 * t));
  let chime = 0;
  for (const [s, f] of chimes) {
    const dt = t - s;
    if (dt < 0 || dt > 4) continue;
    chime += Math.exp(-dt / 1.1) * Math.min(1, dt / 0.01) * (Math.sin(2 * Math.PI * f * dt) + 0.3 * Math.sin(2 * Math.PI * f * 2.76 * dt) * Math.exp(-dt / 0.4));
  }
  const env = Math.min(1, t / 6) * Math.min(1, (SECONDS - t) / 4);
  pcm[i] = Math.round(Math.max(-1, Math.min(1, (rain * 0.55 + chime * 0.12) * env)) * 32767);
}
const header = Buffer.alloc(44);
header.write("RIFF", 0); header.writeUInt32LE(36 + n * 2, 4); header.write("WAVE", 8);
header.write("fmt ", 12); header.writeUInt32LE(16, 16); header.writeUInt16LE(1, 20); header.writeUInt16LE(1, 22);
header.writeUInt32LE(RATE, 24); header.writeUInt32LE(RATE * 2, 28); header.writeUInt16LE(2, 32); header.writeUInt16LE(16, 34);
header.write("data", 36); header.writeUInt32LE(n * 2, 40);
writeFileSync(process.argv[2] || "prealarm.wav", Buffer.concat([header, Buffer.from(pcm.buffer)]));
