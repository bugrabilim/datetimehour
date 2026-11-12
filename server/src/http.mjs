/* Küçük HTTP yardımcıları (çerçevesiz node:http) */
async function readBody(req, limit) {
  let size = 0;
  const chunks = [];
  for await (const c of req) {
    size += c.length;
    if (size > limit) throw Object.assign(new Error("too_large"), { status: 413 });
    chunks.push(c);
  }
  return Buffer.concat(chunks).toString("utf8");
}

export async function readForm(req, limit = 16 * 1024) {
  return new URLSearchParams(await readBody(req, limit));
}

export async function readJson(req, limit = 64 * 1024) {
  const text = await readBody(req, limit);
  if (!text) return {};
  try {
    const v = JSON.parse(text);
    return v && typeof v === "object" ? v : {};
  } catch {
    throw Object.assign(new Error("bad_json"), { status: 400 });
  }
}

export function send(res, status, body, type = "text/plain; charset=utf-8", extra = {}) {
  res.writeHead(status, { "Content-Type": type, "Cache-Control": "no-store", ...extra });
  res.end(body);
}
export const json = (res, status, obj, extra = {}) => send(res, status, JSON.stringify(obj), "application/json; charset=utf-8", extra);
export const redirect = (res, to, extra = {}) => { res.writeHead(303, { Location: to, "Cache-Control": "no-store", ...extra }); res.end(); };
