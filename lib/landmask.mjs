// Kara maskesi: Natural Earth 1:110m kara halkalarını (src/data/land-110m.json) eşit dikdörtgen
// ızgaraya tarar (çift-tek kuralı, satır taraması). Çıktı satır başına çalışma uzunlukları:
// her satır su ile başlar, sonra kara/su sırayla. Küre betiği (globe.js) bunu açar.
export function landMask(rings, w = 720, h = 360) {
  // 180. meridyeni aşan halkalarda boylamı sürekli yap (Çukotka, Fiji); -90° kenarlı halka (Antarktika) olduğu gibi kalır
  const unwrapped = rings.map((ring) => {
    if (ring.some((p) => p[1] <= -89.9)) return ring;
    const out = [ring[0].slice()];
    for (let i = 1; i < ring.length; i++) {
      let x = ring[i][0];
      const prev = out[i - 1][0];
      while (x - prev > 180) x -= 360;
      while (prev - x > 180) x += 360;
      out.push([x, ring[i][1]]);
    }
    return out;
  });
  const rows = [];
  for (let r = 0; r < h; r++) {
    const lat = 90 - ((r + 0.5) * 180) / h;
    const line = new Uint8Array(w);
    for (const ring of unwrapped) {
      const xs = [];
      for (let i = 0, n = ring.length; i < n; i++) {
        const [x1, y1] = ring[i], [x2, y2] = ring[(i + 1) % n];
        if ((y1 <= lat && y2 > lat) || (y2 <= lat && y1 > lat)) xs.push(x1 + ((lat - y1) * (x2 - x1)) / (y2 - y1));
      }
      xs.sort((a, b) => a - b);
      // Her halka kendi aralıklarını XOR'lar: çift-tek kuralı halkalar arası da geçerli olur (göller)
      for (let k = 0; k + 1 < xs.length; k += 2) {
        const a = Math.ceil(((xs[k] + 180) * w) / 360 - 0.5);
        const b = Math.floor(((xs[k + 1] + 180) * w) / 360 - 0.5);
        for (let c = a; c <= b; c++) line[((c % w) + w) % w] ^= 1;
      }
    }
    const runs = [];
    let cur = 0, len = 0;
    for (let c = 0; c < w; c++) {
      if (line[c] === cur) len++;
      else { runs.push(len); cur = line[c]; len = 1; }
    }
    runs.push(len);
    rows.push(runs);
  }
  return { w, h, rows };
}

// Test ve önizleme için: maskeyi açar (1 = kara)
export function unpackMask(m) {
  const out = new Uint8Array(m.w * m.h);
  m.rows.forEach((runs, r) => {
    let c = 0, v = 0;
    for (const n of runs) { if (v) out.fill(1, r * m.w + c, r * m.w + c + n); c += n; v ^= 1; }
  });
  return out;
}
