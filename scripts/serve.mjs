// Yerel önizleme: `npm run serve` → http://localhost:8080 (dist/ klasörünü sunar)
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const DIST = path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "dist");
const PORT = Number(process.env.PORT || 8080);
const TYPES = {
  ".html": "text/html; charset=utf-8", ".css": "text/css; charset=utf-8", ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8", ".svg": "image/svg+xml", ".png": "image/png", ".ico": "image/x-icon",
  ".txt": "text/plain; charset=utf-8", ".xml": "application/xml; charset=utf-8", ".webmanifest": "application/manifest+json",
};

http
  .createServer((req, res) => {
    const url = new URL(req.url, "http://x");
    let rel = decodeURIComponent(url.pathname);
    let file = path.join(DIST, rel);
    if (!file.startsWith(DIST)) { res.writeHead(403).end(); return; }
    if (fs.existsSync(file) && fs.statSync(file).isDirectory()) {
      if (!rel.endsWith("/")) { res.writeHead(301, { Location: rel + "/" + url.search }).end(); return; }
      file = path.join(file, "index.html");
    }
    if (!fs.existsSync(file)) {
      res.writeHead(404, { "Content-Type": TYPES[".html"] }).end(fs.readFileSync(path.join(DIST, "404.html")));
      return;
    }
    res.writeHead(200, { "Content-Type": TYPES[path.extname(file)] || "application/octet-stream" });
    fs.createReadStream(file).pipe(res);
  })
  .listen(PORT, () => console.log(`http://localhost:${PORT}`));
