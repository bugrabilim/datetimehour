// Kademeli alarm melodisi (iOS AlarmKit kendi sesini yükseltemediği için uygulamayla gelir): marimba benzeri
// dört notalık motif, 29 sn; ses ilk 20 sn'de kısıktan tam sese çıkar, sonra tam seste sürer. Telifsiz (sentez).
// Kullanım: node mobile/shared/sounds/gen-alarm-rise.mjs out.wav
//           ffmpeg -i out.wav -c:a adpcm_ima_qt -f caf mobile/ios/App/Resources/alarm-rise.caf
import { writeFileSync } from "node:fs";

const RATE = 22050, SECONDS = 29, RISE = 20;
const notes = [659.25, 783.99, 1046.5, 783.99]; // E5 G5 C6 G5
const STEP = 0.3, PATTERN = 1.8;
const n = RATE * SECONDS;
const pcm = new Int16Array(n);
for (let i = 0; i < n; i++) {
  const t = i / RATE;
  const gain = 0.06 + 0.94 * Math.min(1, t / RISE) ** 2;
  const p = t % PATTERN;
  let v = 0;
  for (let k = 0; k < notes.length; k++) {
    const dt = p - k * STEP;
    if (dt < 0 || dt > 1.2) continue;
    const f = notes[k];
    const env = Math.exp(-dt / 0.28) * Math.min(1, dt / 0.004);
    v += env * (Math.sin(2 * Math.PI * f * dt) + 0.35 * Math.sin(2 * Math.PI * f * 4 * dt) * Math.exp(-dt / 0.06));
  }
  pcm[i] = Math.round(Math.max(-1, Math.min(1, v * 0.55 * gain)) * 32767);
}
const header = Buffer.alloc(44);
header.write("RIFF", 0); header.writeUInt32LE(36 + n * 2, 4); header.write("WAVE", 8);
header.write("fmt ", 12); header.writeUInt32LE(16, 16); header.writeUInt16LE(1, 20); header.writeUInt16LE(1, 22);
header.writeUInt32LE(RATE, 24); header.writeUInt32LE(RATE * 2, 28); header.writeUInt16LE(2, 32); header.writeUInt16LE(16, 34);
header.write("data", 36); header.writeUInt32LE(n * 2, 40);
writeFileSync(process.argv[2] || "alarm-rise.wav", Buffer.concat([header, Buffer.from(pcm.buffer)]));
